package io.github.cfsima.modernsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

class ChangePass : AppCompatActivity() {

    private val viewModel: ChangePassViewModel by viewModels()

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                // If we get logged out, we should probably just finish,
                // but the original code did not seem to do much other than maybe finish or restart?
                // The original code:
                // if (action.equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                //      if (debug) Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                //      finish();
                // }
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        Toast.makeText(this@ChangePass, R.string.password_changed, Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                }

                ChangePassScreen(
                    uiState = uiState,
                    onChangePassword = { old, new, confirm ->
                        viewModel.changePassword(old, new, confirm)
                    },
                    onBack = { finish() },
                    onClearErrors = { viewModel.clearErrors() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!AuthManager.isSignedIn()) {
            val frontDoorIntent = Intent(this, FrontDoor::class.java)
            frontDoorIntent.action = CryptoIntents.ACTION_AUTOLOCK
            startActivity(frontDoorIntent)
            finish()
            return
        }

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        Passwords.Initialize(this)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            val restartTimerIntent = Intent(CryptoIntents.ACTION_RESTART_TIMER)
            sendBroadcast(restartTimerIntent)
        }
    }
}
