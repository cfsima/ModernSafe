package io.github.cfsima.modernsafe

import android.util.Log
import io.github.cfsima.modernsafe.password.Master

object AuthManager {
    private const val TAG = "AuthManager"

    @JvmField
    var lastUsedPassword: String? = null

    @JvmStatic
    fun isSignedIn(): Boolean {
        if ((Master.getSalt() != null) && (Master.getMasterKey() != null)) {
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
        Master.setMasterKey(null)
    }
}
