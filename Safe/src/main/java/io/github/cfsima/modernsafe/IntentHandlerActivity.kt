/*
 * Copyright 2007-2008 Steven Osborn
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

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import io.github.cfsima.modernsafe.dialog.DialogHostingActivity
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.CategoryEntry
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.password.Master
import java.util.Arrays

/**
 * FrontDoor Activity
 * <p/>
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
class IntentHandlerActivity : AppCompatActivity() {

    private val debug = true
    private val TAG = "IntentHandlerActivity"

    private var ch: CryptoHelper? = null
    private lateinit var mPreferences: SharedPreferences

    // Activity Result Launchers
    private lateinit var askPasswordLauncher: ActivityResultLauncher<Intent>
    private lateinit var allowExternalAccessLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (debug) Log.d(TAG, "onCreate($savedInstanceState)")

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Initialize Passwords (database helper etc.)
        if (!Passwords.Initialize(this)) {
            finish()
            return
        }

        // Initialize ActivityResultLaunchers
        askPasswordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (debug) Log.d(TAG, "RESULT_OK")
                actionDispatch()
            } else {
                if (debug) Log.d(TAG, "RESULT_CANCELED")
                moveTaskToBack(true)
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        allowExternalAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Check again, regardless whether user pressed "OK" or "Cancel".
            // DialogHostingActivity never returns a resultCode different than RESULT_CANCELED.
            if (debug) Log.i(TAG, "actionDispatch called right now")
            actionDispatch()
        }
    }

    override fun onResume() {
        super.onResume()
        if (debug) Log.d(TAG, "onResume()")
        startUp()
    }

    override fun onPause() {
        super.onPause()
        if (debug) Log.d(TAG, "onPause()")
    }

    private fun startUp() {
        val askPassIsLocal = isIntentLocal()

        if (Master.masterKey == null) {
            val promptForPassword = getExtraBoolean(intent, CryptoIntents.EXTRA_PROMPT, CryptoIntents.EXTRA_PROMPT_MODERN, true)
            if (debug) Log.d(TAG, "Prompt for password: $promptForPassword")

            if (promptForPassword) {
                if (debug) Log.d(TAG, "ask for password")
                val askPass = Intent(applicationContext, AskPassword::class.java)
                val inputBody = getExtraString(intent, CryptoIntents.EXTRA_TEXT, CryptoIntents.EXTRA_TEXT_MODERN)
                askPass.putExtra(CryptoIntents.EXTRA_TEXT, inputBody)
                askPass.putExtra(CryptoIntents.EXTRA_TEXT_MODERN, inputBody)
                askPass.putExtra(AskPassword.EXTRA_IS_LOCAL, askPassIsLocal)
                askPasswordLauncher.launch(askPass)
            } else {
                if (debug) Log.d(TAG, "ask for password (cancelled)")
                setResult(RESULT_CANCELED)
                finish()
            }
        } else {
            val externalAccess = mPreferences.getBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false)
            if (askPassIsLocal || externalAccess) {
                if (debug) Log.d(TAG, "starting actionDispatch")
                actionDispatch()
            } else {
                if (debug) Log.d(TAG, "start showDialogAllowExternalAccess()")
                showDialogAllowExternalAccess()
            }
        }
    }

    private fun isIntentLocal(): Boolean {
        val action = intent.action
        val isLocal = action == null || action == Intent.ACTION_MAIN || action == CryptoIntents.ACTION_AUTOLOCK
        if (debug) Log.d(TAG, "isLocal=$isLocal, action=$action")
        return isLocal
    }

    private fun showDialogAllowExternalAccess() {
        val i = Intent(this, DialogHostingActivity::class.java)
        i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_ALLOW_EXTERNAL_ACCESS)
        allowExternalAccessLauncher.launch(i)
    }

    private fun actionDispatch() {
        val thisIntent = intent
        val action = thisIntent.action
        // Create a copy of the intent to return as callback, ensuring immutability of the original
        var callbackIntent = Intent(intent)
        var callbackResult = RESULT_CANCELED

        if (debug) Log.d(TAG, "actionDispatch()")

        if (Master.salt.isNullOrEmpty()) {
            return
        }

        if (ch == null) {
            ch = CryptoHelper()
        }

        try {
            ch?.init(CryptoHelper.EncryptionMedium, Master.salt)
            ch?.setPassword(Master.masterKey)
        } catch (e1: CryptoHelperException) {
            e1.printStackTrace()
            Toast.makeText(this, getString(R.string.crypto_error) + e1.message, Toast.LENGTH_SHORT).show()
            return
        }

        val externalAccess = mPreferences.getBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false)

        if (action == null || action == Intent.ACTION_MAIN) {
            val i = Intent(applicationContext, CategoryList::class.java)
            startActivity(i)
        } else if (action == CryptoIntents.ACTION_AUTOLOCK) {
            if (debug) Log.d(TAG, "autolock")
            finish()
        } else if (externalAccess) {
            if (action == CryptoIntents.ACTION_ENCRYPT || action == CryptoIntents.ACTION_ENCRYPT_MODERN) {
                callbackResult = encryptIntent(thisIntent, callbackIntent)
            } else if (action == CryptoIntents.ACTION_DECRYPT || action == CryptoIntents.ACTION_DECRYPT_MODERN) {
                callbackResult = decryptIntent(thisIntent, callbackIntent)
            } else if (action == CryptoIntents.ACTION_GET_PASSWORD || action == CryptoIntents.ACTION_GET_PASSWORD_MODERN ||
                action == CryptoIntents.ACTION_SET_PASSWORD || action == CryptoIntents.ACTION_SET_PASSWORD_MODERN
            ) {
                try {
                    callbackIntent = getSetPassword(thisIntent, callbackIntent)
                    callbackResult = RESULT_OK
                } catch (e: CryptoHelperException) {
                    Log.e(TAG, e.toString(), e)
                    Toast.makeText(
                        this@IntentHandlerActivity,
                        "There was a crypto error while retrieving the requested password: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString(), e)
                    Toast.makeText(
                        this@IntentHandlerActivity,
                        "There was an error in retrieving the requested password: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            setResult(callbackResult, callbackIntent)
        }
        finish()
    }

    // Helper functions
    private fun getExtraString(intent: Intent, legacyKey: String, modernKey: String): String? {
        return if (intent.hasExtra(modernKey)) {
            intent.getStringExtra(modernKey)
        } else {
            intent.getStringExtra(legacyKey)
        }
    }

    private fun getExtraStringArray(intent: Intent, legacyKey: String, modernKey: String): Array<String>? {
        return if (intent.hasExtra(modernKey)) {
            intent.getStringArrayExtra(modernKey)
        } else {
            intent.getStringArrayExtra(legacyKey)
        }
    }

    private fun getExtraBoolean(intent: Intent, legacyKey: String, modernKey: String, defaultValue: Boolean): Boolean {
        return if (intent.hasExtra(modernKey)) {
            intent.getBooleanExtra(modernKey, defaultValue)
        } else {
            intent.getBooleanExtra(legacyKey, defaultValue)
        }
    }

    private fun encryptIntent(thisIntent: Intent, callbackIntent: Intent): Int {
        if (debug) Log.d(TAG, "encryptIntent()")
        var callbackResult = RESULT_CANCELED

        try {
            val inputBody = getExtraString(thisIntent, CryptoIntents.EXTRA_TEXT, CryptoIntents.EXTRA_TEXT_MODERN)
            if (inputBody != null) {
                if (debug) Log.d(TAG, "inputBody=$inputBody")
                val outputBody = ch?.encryptWithSessionKey(inputBody)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_MODERN, outputBody)
            }

            val inputArr = getExtraStringArray(thisIntent, CryptoIntents.EXTRA_TEXT_ARRAY, CryptoIntents.EXTRA_TEXT_ARRAY_MODERN)
            if (inputArr != null) {
                if (debug) Log.d(TAG, "in=${Arrays.toString(inputArr)}")
                val outputArr = arrayOfNulls<String>(inputArr.size)
                for (i in inputArr.indices) {
                    outputArr[i] = ch?.encryptWithSessionKey(inputArr[i])
                }
                if (debug) Log.d(TAG, "out=${Arrays.toString(outputArr)}")
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, outputArr)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY_MODERN, outputArr)
            }

            thisIntent.data?.let { fileUri ->
                val newFileUri = ch?.encryptFileWithSessionKey(this, fileUri)
                callbackIntent.data = newFileUri
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY) || thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY_MODERN)) {
                val sessionKey = ch?.currentSessionKey
                if (sessionKey == null) {
                    return RESULT_CANCELED
                }
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY, sessionKey)
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY_MODERN, sessionKey)
                callbackIntent.data = CryptoContentProvider.CONTENT_URI
            }
            callbackResult = RESULT_OK
        } catch (e: CryptoHelperException) {
            Log.e(TAG, e.toString())
        }
        return callbackResult
    }

    private fun decryptIntent(thisIntent: Intent, callbackIntent: Intent): Int {
        var callbackResult = RESULT_CANCELED
        try {
            val inputBody = getExtraString(thisIntent, CryptoIntents.EXTRA_TEXT, CryptoIntents.EXTRA_TEXT_MODERN)
            if (inputBody != null) {
                val outputBody = ch?.decryptWithSessionKey(inputBody)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_MODERN, outputBody)
            }

            val inputArr = getExtraStringArray(thisIntent, CryptoIntents.EXTRA_TEXT_ARRAY, CryptoIntents.EXTRA_TEXT_ARRAY_MODERN)
            if (inputArr != null) {
                val outputArr = arrayOfNulls<String>(inputArr.size)
                for (i in inputArr.indices) {
                    outputArr[i] = ch?.decryptWithSessionKey(inputArr[i])
                }
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, outputArr)
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY_MODERN, outputArr)
            }

            thisIntent.data?.let { fileUri ->
                val newFileUri = ch?.decryptFileWithSessionKey(this, fileUri)
                callbackIntent.data = newFileUri
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY) || thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY_MODERN)) {
                val sessionKey = ch?.currentSessionKey
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY, sessionKey)
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY_MODERN, sessionKey)
                callbackIntent.data = CryptoContentProvider.CONTENT_URI
            }
            callbackResult = RESULT_OK

        } catch (e: CryptoHelperException) {
            Log.e(TAG, e.toString())
        }
        return callbackResult
    }

    @Throws(Exception::class)
    private fun getSetPassword(thisIntent: Intent, callbackIntent: Intent): Intent {
        val action = thisIntent.action
        if (debug) Log.d(TAG, "GET_or_SET_PASSWORD")

        var username: String? = null
        var password: String? = null

        val clearUniqueName = getExtraString(thisIntent, CryptoIntents.EXTRA_UNIQUE_NAME, CryptoIntents.EXTRA_UNIQUE_NAME_MODERN)
            ?: throw Exception("EXTRA_UNIQUE_NAME not set.")

        var row: PassEntry? = Passwords.findPassWithUniqueName(clearUniqueName)
        val passExists = (row != null)

        val callingPackage = callingPackage // Activity method

        if (passExists) {
            val packageAccess = Passwords.getPackageAccess(row?.id)
            if (packageAccess == null || !PassEntry.checkPackageAccess(packageAccess, callingPackage)) {
                throw Exception("It is currently not permissible for this application to request this password.")
            }
        } else {
            row = PassEntry()
        }

        if (action == CryptoIntents.ACTION_GET_PASSWORD || action == CryptoIntents.ACTION_GET_PASSWORD_MODERN) {
            if (passExists) {
                username = row?.plainUsername
                password = row?.plainPassword
            } else {
                throw Exception("Could not find password with the unique name: $clearUniqueName")
            }
            callbackIntent.putExtra(CryptoIntents.EXTRA_USERNAME, username)
            callbackIntent.putExtra(CryptoIntents.EXTRA_USERNAME_MODERN, username)
            callbackIntent.putExtra(CryptoIntents.EXTRA_PASSWORD, password)
            callbackIntent.putExtra(CryptoIntents.EXTRA_PASSWORD_MODERN, password)

        } else if (action == CryptoIntents.ACTION_SET_PASSWORD || action == CryptoIntents.ACTION_SET_PASSWORD_MODERN) {
            val clearUsername = getExtraString(thisIntent, CryptoIntents.EXTRA_USERNAME, CryptoIntents.EXTRA_USERNAME_MODERN)
            val clearPassword = getExtraString(thisIntent, CryptoIntents.EXTRA_PASSWORD, CryptoIntents.EXTRA_PASSWORD_MODERN)
                ?: throw Exception("PASSWORD extra must be set.")

            row!!.plainUsername = clearUsername ?: ""
            row.plainPassword = clearPassword

            if (passExists) {
                if (clearUsername.isNullOrEmpty() && clearPassword.isEmpty()) {
                    Passwords.deletePassEntry(row.id)
                } else {
                    Passwords.putPassEntry(row)
                }
            } else {
                row.plainUniqueName = clearUniqueName
                row.plainDescription = clearUniqueName
                row.plainWebsite = ""
                row.plainNote = ""

                val category = "Application Data"
                var c: CategoryEntry? = Passwords.getCategoryEntryByName(category)
                if (c == null) {
                    c = CategoryEntry()
                    c.plainName = "Application Data"
                    c.id = Passwords.putCategoryEntry(c)
                }
                row.category = c!!.id
                row.id = 0
                row.id = Passwords.putPassEntry(row)
            }
            val packageAccess = Passwords.getPackageAccess(row.id)
            if (packageAccess == null || !PassEntry.checkPackageAccess(packageAccess, callingPackage)) {
                Passwords.addPackageAccess(row.id, callingPackage)
            }
        }
        return callbackIntent
    }
}
