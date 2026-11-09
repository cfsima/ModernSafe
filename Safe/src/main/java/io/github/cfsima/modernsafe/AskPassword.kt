package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.cfsima.modernsafe.navigation.AskPasswordRoute
import io.github.cfsima.modernsafe.navigation.ModernSafeNavHost

/**
 * Legacy activity replaced by [AskPasswordRoute] in [ModernSafeNavHost].
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class AskPassword : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_LOCAL = "io.github.cfsima.modernsafe.bundle.EXTRA_IS_REMOTE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forwardIntent = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            data = intent.data
            intent.extras?.let { putExtras(it) }
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        }
        startActivity(forwardIntent)
        finish()
    }
}
