/*
 * Copyright 2012 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cfsima.modernsafe.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.AuthManager
import io.github.cfsima.modernsafe.Settings
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.password.Master

class AutoLockService : Service() {

    private var t: CountDownTimer? = null
    private lateinit var mIntentReceiver: BroadcastReceiver
    private lateinit var mPreferences: SharedPreferences
    private lateinit var serviceNotification: ServiceNotification

    override fun onCreate() {
        if (debug) {
            Log.d(TAG, "onCreate")
        }
        mIntentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    if (debug) {
                        Log.d(TAG, "caught ACTION_SCREEN_OFF")
                    }
                    val lockOnScreenLock = mPreferences.getBoolean(
                        Settings.PREFERENCE_LOCK_ON_SCREEN_LOCK, true
                    )
                    if (lockOnScreenLock) {
                        lockOut()
                    }
                } else if (intent.action == CryptoIntents.ACTION_RESTART_TIMER) {
                    restartTimer()
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(CryptoIntents.ACTION_RESTART_TIMER)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        ContextCompat.registerReceiver(this, mIntentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        serviceNotification = ServiceNotification(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startid: Int) {
        if (debug) {
            Log.d(TAG, "onStart")
        }
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (debug) {
            Log.d(TAG, "Received start id : : ")
        }
        startTimer()
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY
    }

    override fun onDestroy() {
        if (debug) {
            Log.d(TAG, "onDestroy")
        }
        try {
            unregisterReceiver(mIntentReceiver)
        } catch (e: IllegalArgumentException) {
            // ignore if not registered
        }

        if (Master.masterKey != null) {
            lockOut()
        }
        serviceNotification.clearNotification()
        t?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Clear the masterKey, notification, and broadcast
     * CryptoIntents.ACTION_CRYPTO_LOGGED_OUT
     */
    private fun lockOut() {
        AuthManager.setSignedOut() // This handles Master.masterKey = null and clearing CryptoHelper
        serviceNotification.clearNotification()
        t?.cancel()

        val intent = Intent(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        sendBroadcast(intent)
    }

    /**
     * Start a CountDownTimer() that will cause a lockOut()
     *
     * @see .lockOut
     */
    private fun startTimer() {
        if (Master.masterKey == null) {
            serviceNotification.clearNotification()
            t?.cancel()
            return
        }
        serviceNotification.setNotification(this@AutoLockService)
        val timeout = mPreferences.getString(
            Settings.PREFERENCE_LOCK_TIMEOUT,
            Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE
        )
        var timeoutMinutes = 5 // default to 5
        try {
            timeoutMinutes = timeout?.toInt() ?: 5
        } catch (e: NumberFormatException) {
            Log.d(TAG, "why is lock_timeout busted?")
        }
        val timeoutUntilStop = (timeoutMinutes * 60000).toLong()

        if (debug) {
            Log.d(TAG, "startTimer with timeoutUntilStop=")
        }

        t?.cancel() // cancel previous if any

        t = object : CountDownTimer(timeoutUntilStop, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // doing nothing.
                if (debug) {
                    Log.d(TAG, "tick:  this=")
                }
                AuthManager.timeRemaining = millisUntilFinished
                if (Master.masterKey == null) {
                    if (debug) {
                        Log.d(TAG, "detected masterKey=null")
                    }
                    lockOut()
                } else {
                    serviceNotification.updateProgress(
                        timeoutUntilStop.toInt(),
                        AuthManager.timeRemaining.toInt()
                    )
                }
            }

            override fun onFinish() {
                if (debug) {
                    Log.d(TAG, "onFinish()")
                }
                lockOut()
                AuthManager.timeRemaining = 0
            }
        }.start()

        AuthManager.timeRemaining = timeoutUntilStop
        if (debug) {
            Log.d(TAG, "Timer started with: ")
        }
    }

    /**
     * Restart the CountDownTimer()
     */
    private fun restartTimer() {
        // must be started with startTimer first.
        if (debug) {
            Log.d(TAG, "timer restarted")
        }
        if (t != null) {
            t?.cancel()
            t?.start()
        }
    }

    companion object {
        private const val debug = false
        private const val TAG = "AutoLockService"
    }
}
