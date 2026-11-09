package io.github.cfsima.modernsafe

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.StrictMode
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.service.AutoLockService

/**
 * Custom [Application] class for ModernSafe.
 *
 * Responsibilities:
 * - App-level startup and lifecycle management.
 * - Eager initialization/warming of core repositories (e.g., [SettingsRepository]).
 * - Debugging policies such as [StrictMode] for leak and thread violation detection in debug builds.
 * - Centralized application dependency container facilitating future DI integration (Hilt/Koin).
 */
class SafeApplication : Application() {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository.getInstance(this)
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                val settings = settingsRepository.userSettings.value
                if (settings.lockOnScreenLock) {
                    if (AuthManager.isSignedIn()) {
                        AuthManager.setSignedOut()
                        stopService(Intent(context, AutoLockService::class.java))
                        sendBroadcast(Intent(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT))
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }

        // Initialize settings repository to trigger eager migration/load
        settingsRepository

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        ContextCompat.registerReceiver(this, screenOffReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /**
     * Configures StrictMode policies in debug builds to detect thread violations and resource leaks.
     */
    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build(),
        )
    }
}

