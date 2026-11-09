package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.cfsima.modernsafe.intents.CryptoIntents

/**
 * The main activity entry point.
 * Replaces the legacy FrontDoor and Safe activities.
 * This class name must be preserved for launcher shortcuts.
 */
class FrontDoor : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val action = intent.action

        if ((action == null) || (action == Intent.ACTION_MAIN) || (action == CryptoIntents.ACTION_AUTOLOCK)) {
            val i = Intent(this, MainActivity::class.java).apply {
                action?.let { this.action = it }
                data = intent.data
                intent.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            }
            startActivity(i)
        }

        finish()
    }
}
