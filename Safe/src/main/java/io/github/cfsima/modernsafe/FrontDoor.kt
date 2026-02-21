package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.cfsima.modernsafe.intents.CryptoIntents

/**
 * The main activity entry point.
 * Replaces the legacy FrontDoor and Safe activities.
 * This class name must be preserved for launcher shortcuts.
 */
class FrontDoor : AppCompatActivity() {

    private val TAG = "FrontDoor"
    private val debug = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (debug) Log.d(TAG, "onCreate()")

        val action = intent.action

        if (action == null || action == Intent.ACTION_MAIN || action == CryptoIntents.ACTION_AUTOLOCK) {
            // Forward to IntentHandlerActivity
            val i = Intent(applicationContext, IntentHandlerActivity::class.java)
            if (action != null) {
                i.action = action
            }
            startActivity(i)
        }
        // Do not start intents otherwise, those must be protected by permissions (handled elsewhere or ignored here)

        finish()
    }
}
