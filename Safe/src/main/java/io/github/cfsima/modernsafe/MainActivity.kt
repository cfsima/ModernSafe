package io.github.cfsima.modernsafe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.navigation.AskPasswordRoute
import io.github.cfsima.modernsafe.navigation.CategoryListRoute
import io.github.cfsima.modernsafe.navigation.LogOffRoute
import io.github.cfsima.modernsafe.navigation.ModernSafeNavHost
import io.github.cfsima.modernsafe.ui.theme.ModernSafeTheme

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_LOG_OFF = "io.github.cfsima.modernsafe.action.LOG_OFF"
        const val EXTRA_DESTINATION = "extra_destination"
        const val DESTINATION_LOG_OFF = "destination_log_off"
    }

    private var navController: NavHostController? = null

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                onLoggedOut()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val isLocal = intent.getBooleanExtra(AskPassword.EXTRA_IS_LOCAL, true)
        val isLogOff = intent.action == ACTION_LOG_OFF || intent.getStringExtra(EXTRA_DESTINATION) == DESTINATION_LOG_OFF

        val startDestination: Any = when {
            isLogOff -> LogOffRoute
            AuthManager.isSignedIn() -> CategoryListRoute
            else -> AskPasswordRoute(isLocal = isLocal)
        }

        setContent {
            ModernSafeTheme {
                val hostNavController = rememberNavController()
                navController = hostNavController

                ModernSafeNavHost(
                    navController = hostNavController,
                    startDestination = startDestination,
                    onFinishActivity = { finish() },
                    onSetResultOkAndFinish = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isLogOff = intent.action == ACTION_LOG_OFF || intent.getStringExtra(EXTRA_DESTINATION) == DESTINATION_LOG_OFF
        if (isLogOff) {
            navController?.navigate(LogOffRoute)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        if (AuthManager.isSignedIn()) {
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
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
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun onLoggedOut() {
        navController?.navigate(AskPasswordRoute(isLocal = true)) {
            popUpTo(0) { inclusive = true }
        }
    }
}
