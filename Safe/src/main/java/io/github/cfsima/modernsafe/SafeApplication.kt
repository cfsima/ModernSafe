package io.github.cfsima.modernsafe

import android.app.Application
import android.os.StrictMode

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

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }

        // Initialize settings repository to trigger eager migration/load
        settingsRepository
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

