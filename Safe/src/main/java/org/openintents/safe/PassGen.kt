package org.openintents.safe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.openintents.intents.CryptoIntents
import org.openintents.safe.ui.theme.OISafeTheme

class PassGen : AppCompatActivity() {

    companion object {
        @JvmField
        val CHANGE_ENTRY_RESULT = 2
        @JvmField
        val NEW_PASS_KEY = "new_pass"
    }

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                val frontdoor = Intent(this@PassGen, Safe::class.java).apply {
                    action = CryptoIntents.ACTION_AUTOLOCK
                }
                startActivity(frontdoor)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Ensure user is signed in
        if (!CategoryList.isSignedIn()) {
            val frontdoor = Intent(this, Safe::class.java).apply {
                action = CryptoIntents.ACTION_AUTOLOCK
            }
            startActivity(frontdoor)
            finish()
            return
        }

        setContent {
            OISafeTheme {
                PassGenScreen(
                    onUsePassword = { password ->
                        val resultIntent = Intent().apply {
                            putExtra(NEW_PASS_KEY, password)
                        }
                        setResult(CHANGE_ENTRY_RESULT, resultIntent)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!CategoryList.isSignedIn()) {
             val frontdoor = Intent(this, Safe::class.java).apply {
                action = CryptoIntents.ACTION_AUTOLOCK
            }
            startActivity(frontdoor)
            finish()
            return
        }
        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignore if not registered
        }
    }
}
