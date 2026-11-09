package io.github.cfsima.modernsafe

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.navigation.AskPasswordRoute
import io.github.cfsima.modernsafe.navigation.CategoryListRoute
import io.github.cfsima.modernsafe.navigation.LogOffRoute
import io.github.cfsima.modernsafe.navigation.ModernSafeNavHost
import io.github.cfsima.modernsafe.service.AutoLockService
import io.github.cfsima.modernsafe.ui.theme.ModernSafeTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

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
            val settingsRepository = remember { SettingsRepository.getInstance(applicationContext) }
            val userSettings by settingsRepository.userSettings.collectAsState()

            ModernSafeTheme(
                themePalette = userSettings.themePalette,
                darkMode = userSettings.darkMode
            ) {
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

        val settings = (application as SafeApplication).settingsRepository.userSettings.value
        val timeoutMinutes = settings.lockTimeout.toIntOrNull() ?: 5
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager

        val isKeyguardLocked = keyguardManager?.isKeyguardLocked == true
        if (settings.lockOnScreenLock && isKeyguardLocked) {
            AuthManager.setSignedOut()
            stopService(Intent(applicationContext, AutoLockService::class.java))
        } else if (AuthManager.isSessionTimedOut(timeoutMinutes)) {
            AuthManager.setSignedOut()
            stopService(Intent(applicationContext, AutoLockService::class.java))
        }

        if (AuthManager.isSignedIn()) {
            AuthManager.updateLastActiveTime()
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        } else {
            onLoggedOut()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            val isScreenOff = powerManager?.isInteractive == false
            val isKeyguardLocked = keyguardManager?.isKeyguardLocked == true

            val settings = (application as SafeApplication).settingsRepository.userSettings.value
            if (settings.lockOnScreenLock && (isScreenOff || isKeyguardLocked)) {
                if (AuthManager.isSignedIn()) {
                    AuthManager.setSignedOut()
                    stopService(Intent(applicationContext, AutoLockService::class.java))
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            AuthManager.updateLastActiveTime()
            sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun onLoggedOut() {
        val currentDest = navController?.currentDestination
        if (currentDest == null || currentDest.hasRoute<AskPasswordRoute>()) {
            return
        }
        navController?.navigate(AskPasswordRoute(isLocal = true)) {
            popUpTo(0) { inclusive = true }
        }
    }
}
