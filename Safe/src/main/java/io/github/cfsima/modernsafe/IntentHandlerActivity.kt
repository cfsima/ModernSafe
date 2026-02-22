/*
 * Copyright (C) 2011 OpenIntents.org
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.CategoryEntry
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.password.Master
import java.util.Arrays

class IntentHandlerActivity : AppCompatActivity() {

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                if (debug) Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT")
                finish()
            }
        }
    }

    private val ch: CryptoHelper?
        get() = AuthManager.cryptoHelper

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (debug) Log.d(TAG, "onCreate()")

        actionDispatch()
    }

    private fun actionDispatch() {
        val thisIntent = intent
        val action = thisIntent.action

        // If not logged in, show FrontDoor to log in
        if (!AuthManager.isSignedIn()) {
            val frontDoor = Intent(this, FrontDoor::class.java)
            frontDoor.action = action
            if (thisIntent.extras != null) {
                frontDoor.putExtras(thisIntent.extras!!)
            }
            frontDoor.data = thisIntent.data
            startActivity(frontDoor)
            finish()
            return
        }

        // Initialize Passwords DB (required for GET_PASSWORD/SET_PASSWORD)
        Passwords.Initialize(this)

        if (!checkExternalAccess(action)) {
            showDialogAllowExternalAccess()
            return
        }

        var callbackIntent = Intent(thisIntent)
        var callbackResult = RESULT_CANCELED

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
        finish()
    }

    private fun checkExternalAccess(action: String?): Boolean {
        val isLocalAction = action == null || action == Intent.ACTION_MAIN || action == CryptoIntents.ACTION_AUTOLOCK
        var isLocalPackage = false
        if (callingPackage == packageName) {
            isLocalPackage = true
        }

        if (debug) Log.d(TAG, "checkExternalAccess: callingPackage=$callingPackage, isLocalAction=$isLocalAction, isLocalPackage=$isLocalPackage")

        if (isLocalAction || isLocalPackage) {
            return true
        }

        // If external, check preference
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        return sp.getBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false)
    }

    private fun showDialogAllowExternalAccess() {
        if (dialog != null && dialog!!.isShowing) {
             return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_allow_access, null)
        val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val externalAccess = sp.getBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false)
        checkbox.isChecked = externalAccess

        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_external_access)
            .setView(view)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newAccess = checkbox.isChecked
                sp.edit().putBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, newAccess).apply()
                // Retry dispatch
                actionDispatch()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                setResult(RESULT_CANCELED)
                finish()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (debug) Log.d(TAG, "onResume")

        if (!AuthManager.isSignedIn()) {
            if (debug) Log.d(TAG, "not signed in")
            // onCreate already handles redirection to FrontDoor
            return
        }

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        if (debug) Log.d(TAG, "onPause")
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // ignore
        }
        if (isFinishing && dialog != null) {
            dialog?.dismiss()
            dialog = null
        }
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

        val callingPackage = callingPackage ?: throw Exception("Unknown calling package")

        if (passExists) {
            val packageAccess = Passwords.getPackageAccess(row!!.id)
            if (packageAccess == null || !PassEntry.checkPackageAccess(packageAccess!!, callingPackage)) {
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
            val packageAccess = Passwords.getPackageAccess(row!!.id)
            if (packageAccess == null || !PassEntry.checkPackageAccess(packageAccess!!, callingPackage)) {
                Passwords.addPackageAccess(row!!.id, callingPackage)
            }
        }
        return callbackIntent
    }

    companion object {
        private const val TAG = "IntentHandlerActivity"
        private const val debug = false
    }
}
