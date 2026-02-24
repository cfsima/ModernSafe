package io.github.cfsima.modernsafe

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

class PassEdit : AppCompatActivity() {

    companion object {
        const val RESULT_DELETED = Activity.RESULT_FIRST_USER
        private const val TAG = "PassEdit"
    }

    private val viewModel: PassEditViewModel by viewModels()

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                startFrontDoor()
            }
        }
    }

    private val generatePassLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PassGen.CHANGE_ENTRY_RESULT) {
            val newPass = result.data?.getStringExtra(PassGen.NEW_PASS_KEY)
            if (newPass != null) {
                viewModel.updatePassword(newPass)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        supportActionBar?.hide()

        if (savedInstanceState == null) {
            val id = intent.getLongExtra(PassList.KEY_ID, -1)
            val categoryId = intent.getLongExtra(PassList.KEY_CATEGORY_ID, -1)
            viewModel.load(id, categoryId)
        }

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.isSaved) {
                    if (uiState.isSaved) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }

                LaunchedEffect(uiState.isDeleted) {
                    if (uiState.isDeleted) {
                        setResult(RESULT_DELETED)
                        finish()
                    }
                }

                PassEditScreen(
                    uiState = uiState,
                    onDescriptionChange = viewModel::updateDescription,
                    onWebsiteChange = viewModel::updateWebsite,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onNoteChange = viewModel::updateNote,
                    onSave = viewModel::save,
                    onDelete = viewModel::delete,
                    onGeneratePassword = {
                        val intent = Intent(this, PassGen::class.java)
                        generatePassLauncher.launch(intent)
                    },
                    onBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onConsumeSave = viewModel::consumeSaveEvent,
                    onConsumeDelete = viewModel::consumeDeleteEvent
                )
            }
        }

        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onResume() {
        super.onResume()
        if (!checkSignedIn()) {
            return
        }

        Passwords.Initialize(this)

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered", e)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun checkSignedIn(): Boolean {
        if (!AuthManager.isSignedIn()) {
            startFrontDoor()
            return false
        }
        return true
    }

    private fun startFrontDoor() {
        val intent = Intent(this, AskPassword::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        intent.putExtra(AskPassword.EXTRA_IS_LOCAL, true)
        startActivity(intent)
        finish()
    }
}
