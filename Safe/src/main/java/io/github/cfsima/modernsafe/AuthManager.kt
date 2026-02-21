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
    }
}
