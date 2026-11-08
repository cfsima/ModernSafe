package io.github.cfsima.modernsafe

import android.app.Application

class SafeApplication : Application() {
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize settings repository to trigger eager migration/load
        settingsRepository
    }
}
