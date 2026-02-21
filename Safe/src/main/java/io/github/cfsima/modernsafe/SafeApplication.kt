package io.github.cfsima.modernsafe

import android.app.Application

class SafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PRNGFixes.apply()
    }
}
