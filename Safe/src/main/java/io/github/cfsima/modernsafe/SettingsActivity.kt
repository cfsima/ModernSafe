package io.github.cfsima.modernsafe

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private val backupPathLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Settings.setBackupDocumentAndMethod(this, uri.toString())
                viewModel.refreshPaths()
            }
        }
    }

    private val exportPathLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Settings.setExportDocumentAndMethod(this, uri.toString())
                viewModel.refreshPaths()
            }
        }
    }

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                startFrontDoor()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Secure flag
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            OISafeTheme {
                val externalAccess by viewModel.externalAccess.collectAsState()
                val lockTimeout by viewModel.lockTimeout.collectAsState()
                val lockOnScreenLock by viewModel.lockOnScreenLock.collectAsState()
                val keypad by viewModel.keypad.collectAsState()
                val keypadMute by viewModel.keypadMute.collectAsState()
                val autoBackup by viewModel.autoBackup.collectAsState()
                val autoBackupDays by viewModel.autoBackupDays.collectAsState()
                val backupPath by viewModel.backupPath.collectAsState()
                val exportPath by viewModel.exportPath.collectAsState()

                SettingsScreen(
                    externalAccess = externalAccess,
                    lockTimeout = lockTimeout,
                    lockOnScreenLock = lockOnScreenLock,
                    keypad = keypad,
                    keypadMute = keypadMute,
                    autoBackup = autoBackup,
                    autoBackupDays = autoBackupDays,
                    backupPath = backupPath,
                    exportPath = exportPath,
                    onExternalAccessChange = viewModel::setExternalAccess,
                    onLockTimeoutChange = viewModel::setLockTimeout,
                    onLockOnScreenLockChange = viewModel::setLockOnScreenLock,
                    onKeypadChange = viewModel::setKeypad,
                    onKeypadMuteChange = viewModel::setKeypadMute,
                    onAutoBackupChange = viewModel::setAutoBackup,
                    onAutoBackupDaysChange = viewModel::setAutoBackupDays,
                    onBackupPathClick = {
                        val intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_BACKUP, Settings.OISAFE_XML)
                        try {
                            backupPathLauncher.launch(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(this, R.string.restore_error, Toast.LENGTH_LONG).show()
                        }
                    },
                    onExportPathClick = {
                        val intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_EXPORT, Settings.OISAFE_CSV)
                        try {
                            exportPathLauncher.launch(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(this, R.string.restore_error, Toast.LENGTH_LONG).show()
                        }
                    },
                    onBack = { finish() }
                )
            }
        }

        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        if (!checkSignedIn()) {
            return
        }
        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Restart timer
        sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered", e)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
             sendBroadcast(Intent(CryptoIntents.ACTION_RESTART_TIMER))
        }
    }

    private fun checkSignedIn(): Boolean {
        if (!AuthManager.isSignedIn()) {
            startFrontDoor()
            return false
        }
        return true
    }

    private fun startFrontDoor() {
        val intent = Intent(this, FrontDoor::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}
