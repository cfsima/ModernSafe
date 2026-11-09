@file:Suppress("DestructuringDeclaration")

package io.github.cfsima.modernsafe.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.AuthManager
import io.github.cfsima.modernsafe.SettingsRepository
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.password.Master
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AutoLockService : Service() {

    private var t: CountDownTimer? = null
    private lateinit var mIntentReceiver: BroadcastReceiver
    private lateinit var serviceNotification: ServiceNotification

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settingsRepository by lazy { SettingsRepository.getInstance(applicationContext) }

    @Volatile
    private var lockOnScreenLock: Boolean = true
    @Volatile
    private var timeoutMinutes: Int = 5

    override fun onCreate() {
        if (DEBUG) {
            Log.d(TAG, "onCreate")
        }
        mIntentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    if (DEBUG) {
                        Log.d(TAG, "caught ACTION_SCREEN_OFF")
                    }
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

        serviceNotification = ServiceNotification(this)

        serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                lockOnScreenLock = settings.lockOnScreenLock
                timeoutMinutes = try {
                    settings.lockTimeout.toInt()
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "why is lock_timeout busted?", e)
                    5
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startid: Int) {
        if (DEBUG) {
            Log.d(TAG, "onStart")
        }
        startTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) {
            Log.d(TAG, "Received start id : : ")
        }
        startTimer()
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY
    }

    override fun onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "onDestroy")
        }
        try {
            unregisterReceiver(mIntentReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered", e)
        }

        if (Master.masterKey != null) {
            lockOut()
        }
        serviceNotification.clearNotification()
        t?.cancel()
        serviceScope.cancel()
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
        stopSelf()
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
        AuthManager.updateLastActiveTime()
        serviceNotification.setNotification(this@AutoLockService)
        val timeoutUntilStop = (timeoutMinutes * 60000).toLong()

        if (DEBUG) {
            Log.d(TAG, "startTimer with timeoutUntilStop=")
        }

        t?.cancel() // cancel previous if any

        t = object : CountDownTimer(timeoutUntilStop, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // doing nothing.
                if (DEBUG) {
                    Log.d(TAG, "tick:  this=")
                }
                AuthManager.timeRemaining = millisUntilFinished
                if (Master.masterKey == null) {
                    if (DEBUG) {
                        Log.d(TAG, "detected masterKey=null")
                    }
                    lockOut()
                } else {
                    // Update notification progress periodically (every 15s) to avoid NotificationManager spam
                    if ((millisUntilFinished % 15000) < 1000) {
                        serviceNotification.updateProgress(
                            timeoutUntilStop.toInt(),
                            AuthManager.timeRemaining.toInt(),
                        )
                    }
                }
            }

            override fun onFinish() {
                if (DEBUG) {
                    Log.d(TAG, "onFinish()")
                }
                AuthManager.timeRemaining = 0
                lockOut()
            }
        }
        t?.start()
    }

    /**
     * Restart the timer.
     *
     *
     * Cancel any existing timers, and call [.startTimer].
     */
    private fun restartTimer() {
        if (DEBUG) {
            Log.d(TAG, "restartTimer")
        }
        t?.cancel()
        AuthManager.updateLastActiveTime()
        startTimer()
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "AutoLockService"
    }
}
