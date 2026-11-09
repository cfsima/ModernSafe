package io.github.cfsima.modernsafe

import android.util.Log
import io.github.cfsima.modernsafe.password.Master

/**
 * Singleton object to manage authentication and session state.
 *
 * NOTE: This singleton holds transient session state (like isSignedIn status and last used password).
 * This state is lost if the application process is killed. This is by design for security reasons
 * (we do not want to persist sensitive data like passwords or keys to disk).
 */
object AuthManager {
    private const val TAG = "AuthManager"

    /**
     * Stores the last used password to potentially clear it from clipboard on logoff.
     * This field is static (singleton) to be shared between PassViewViewModel and LogOffActivity.
     */
    @JvmField
    var lastUsedPassword: String? = null

    /**
     * The currently active CryptoHelper instance holding the session key.
     */
    var cryptoHelper: CryptoHelper? = null

    /**
     * Time remaining for auto-lock, managed by AutoLockService.
     */
    @Volatile
    var timeRemaining: Long = 0

    /**
     * Timestamp of last user interaction, using monotonic elapsedRealtime.
     */
    @Volatile
    var lastActiveTimestamp: Long = 0L

    var timeProvider: () -> Long = {
        try {
            android.os.SystemClock.elapsedRealtime()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    @JvmStatic
    fun updateLastActiveTime() {
        lastActiveTimestamp = timeProvider()
    }

    @JvmStatic
    fun isSessionTimedOut(timeoutMinutes: Int): Boolean {
        if (!isSignedIn()) return true
        if (lastActiveTimestamp == 0L) return false
        val timeoutMillis = timeoutMinutes * 60 * 1000L
        return (timeProvider() - lastActiveTimestamp) >= timeoutMillis
    }

    @JvmStatic
    fun getTimeRemainingMillis(timeoutMinutes: Int): Long {
        if (!isSignedIn()) return 0L
        if (lastActiveTimestamp == 0L) return timeoutMinutes * 60 * 1000L
        val timeoutMillis = timeoutMinutes * 60 * 1000L
        val elapsed = timeProvider() - lastActiveTimestamp
        return (timeoutMillis - elapsed).coerceAtLeast(0L)
    }

    @JvmStatic
    fun isSignedIn(): Boolean {
        if ((Master.salt != null) && (Master.masterKey != null)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "isSignedIn: true")
            }
            return true
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "isSignedIn: false")
        }
        return false
    }

    @JvmStatic
    fun setSignedOut() {
        Master.masterKey = null
        lastUsedPassword = null
        cryptoHelper = null
        timeRemaining = 0
        lastActiveTimestamp = 0L
        io.github.cfsima.modernsafe.model.Passwords.tearDown()
    }
}
