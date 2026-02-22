/*
 * Copyright (C) 2009 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cfsima.modernsafe

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

class CryptoContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(uri: Uri, s: String?, as1: Array<String>?): Int {
        // not supported
        return 0
    }

    override fun getType(uri: Uri): String? {
        // return file extension (uri.lastIndexOf("."))
        return null //mMimeTypes.getMimeType(uri.toString());
    }

    override fun insert(uri: Uri, contentvalues: ContentValues?): Uri? {
        // not supported
        return null
    }

    override fun query(
        uri: Uri, as1: Array<String>?, s: String?, as2: Array<String>?,
        s1: String?
    ): Cursor? {
        /*
        if (uri.toString().startsWith(
				MIME_TYPE_PREFIX)) {
			MatrixCursor c = new MatrixCursor(new String[] { Images.Media.DATA,
					Images.Media.MIME_TYPE });
			// data = absolute path = uri - content://authority/mimetype
			String data = uri.toString().substring(20 + AUTHORITY.length());
			String mimeType = mMimeTypes.getMimeType(data);
			c.addRow(new String[] { data, mimeType });
			return c;
		} else {
			throw new RuntimeException("Unsupported uri");
		}
		*/
        return null
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (debug) {
            Log.d(TAG, "openFile(,)")
        }

        var pfd: ParcelFileDescriptor? = null
        try {
            val context = context ?: throw FileNotFoundException("Context is null")

            // get the /files/ directory for our application
            val filesDir = context.filesDir.toString()
            var path = filesDir
            val cryptSession: String?
            var sessionFile: String
            val modeBits: Int

            when (sUriMatcher.match(uri)) {
                ENCRYPT_ID -> {
                    if (debug) {
                        Log.d(TAG, "openFile: ENCRYPT")
                    }
                    modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY or
                            ParcelFileDescriptor.MODE_CREATE
                    cryptSession = uri.pathSegments[1]
                    sessionFile = "."
                    path += "/"
                }
                DECRYPT_ID -> {
                    if (debug) {
                        Log.d(TAG, "openFile: DECRYPT")
                    }
                    modeBits = ParcelFileDescriptor.MODE_READ_ONLY
                    cryptSession = uri.pathSegments[1]
                    sessionFile = "."
                    path += "/"
                }
                DECRYPT_FILE_ID -> {
                    if (debug) {
                        Log.d(TAG, "openFile: DECRYPT_FILE")
                    }
                    modeBits = ParcelFileDescriptor.MODE_READ_ONLY
                    val originalFile = "file://" + uri.getQueryParameter("file")
                    val sessionKey = uri.getQueryParameter("sessionkey")
                    // TODO: Check that sessionKey is valid.

                    // Decrypt file
                    val crypto = ch
                    if (crypto == null) {
                        if (debug) {
                            Log.d(TAG, "OI Safe currently logged out.")
                        }
                        return null
                    }

                    if (sessionKey != crypto.currentSessionKey) {
                        if (debug) {
                            Log.d(TAG, "Session keys do not match! " + sessionKey + " != " + crypto.currentSessionKey)
                        }
                        return null
                    }

                    if (debug) {
                        Log.d(TAG, "Original file path: ")
                    }
                    if (!AuthManager.isSignedIn()) {
                        val frontdoor = Intent(context, FrontDoor::class.java)
                        frontdoor.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(frontdoor)
                        throw CryptoHelperException("Not logged in.")
                    }

                    if (debug) {
                        Log.d(TAG, "Decrypt..")
                    }

                    val newUri = crypto.decryptFileWithSessionKeyThroughContentProvider(context, Uri.parse(originalFile))

                    // The returned URI from decryptFileWithSessionKeyThroughContentProvider might be null if decryption failed?
                    // CryptoHelper source shows it handles errors but returns a Uri constructed from CONTENT_URI.
                    // Wait, check CryptoHelper.kt decryptFileWithSessionKeyThroughContentProvider implementation.
                    // It returns a Uri?

                    if (newUri == null) {
                         // Should not happen if successful, but if failed inside CryptoHelper?
                         // CryptoHelper returns null on failure?
                         // Checked source: yes, it returns resultUri which is initialized to null, set if successful.
                         // So newUri can be null.
                         throw FileNotFoundException("Decryption failed")
                    }

                    cryptSession = newUri.pathSegments[1]
                    sessionFile = "."
                    path += "/"
                    if (debug) {
                        Log.d(TAG, "New path: ")
                    }
                }
                else -> throw IllegalArgumentException("Unknown URI ")
            }

            if (debug) {
                Log.d(TAG, "openFile: path=")
            }
            pfd = ParcelFileDescriptor.open(File(path), modeBits)

            // Delete the file immediately so it disappears when closed
            if (!context.deleteFile(sessionFile)) {
                if (debug) {
                    Log.e(TAG, "openFile: unable to delete: ")
                }
            }
        } catch (e: FileNotFoundException) {
            if (debug) {
                Log.d(TAG, "openFile: FileNotFound")
            }
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: CryptoHelperException) {
            if (debug) {
                Log.d(TAG, "openFile: CryptoHelperException:" + e.message)
            }
            pfd = null
            //throw e;
        }

        return pfd
    }

    companion object {
        private const val debug = false
        private const val TAG = "CryptoContentProvider"

        @JvmField
        var ch: CryptoHelper? = null

        const val AUTHORITY = "io.github.cfsima.modernsafe"
        @JvmField
        val CONTENT_URI: Uri = Uri.parse("content://")

        const val SESSION_FILE = "session"

        private const val ENCRYPT_ID = 2
        private const val DECRYPT_ID = 3
        private const val DECRYPT_FILE_ID = 4

        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            sUriMatcher.addURI(AUTHORITY, "encrypt/*", ENCRYPT_ID)
            sUriMatcher.addURI(AUTHORITY, "decrypt/*", DECRYPT_ID)
            sUriMatcher.addURI(AUTHORITY, "decryptfile", DECRYPT_FILE_ID)
        }
    }
}
