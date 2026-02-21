package io.github.cfsima.modernsafe

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _externalAccess = MutableStateFlow(false)
    val externalAccess: StateFlow<Boolean> = _externalAccess.asStateFlow()

    private val _lockTimeout = MutableStateFlow(Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE)
    val lockTimeout: StateFlow<String> = _lockTimeout.asStateFlow()

    private val _lockOnScreenLock = MutableStateFlow(true)
    val lockOnScreenLock: StateFlow<Boolean> = _lockOnScreenLock.asStateFlow()

    private val _keypad = MutableStateFlow(false)
    val keypad: StateFlow<Boolean> = _keypad.asStateFlow()

    private val _keypadMute = MutableStateFlow(false)
    val keypadMute: StateFlow<Boolean> = _keypadMute.asStateFlow()

    private val _autoBackup = MutableStateFlow(true)
    val autoBackup: StateFlow<Boolean> = _autoBackup.asStateFlow()

    private val _autoBackupDays = MutableStateFlow(Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE)
    val autoBackupDays: StateFlow<String> = _autoBackupDays.asStateFlow()

    private val _backupPath = MutableStateFlow("")
    val backupPath: StateFlow<String> = _backupPath.asStateFlow()

    private val _exportPath = MutableStateFlow("")
    val exportPath: StateFlow<String> = _exportPath.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _externalAccess.value = prefs.getBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false)
        _lockTimeout.value = prefs.getString(Settings.PREFERENCE_LOCK_TIMEOUT, Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE) ?: Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE
        _lockOnScreenLock.value = prefs.getBoolean(Settings.PREFERENCE_LOCK_ON_SCREEN_LOCK, true)
        _keypad.value = prefs.getBoolean(Settings.PREFERENCE_KEYPAD, false)
        _keypadMute.value = prefs.getBoolean(Settings.PREFERENCE_KEYPAD_MUTE, false)
        _autoBackup.value = prefs.getBoolean(Settings.PREFERENCE_AUTOBACKUP, true)
        _autoBackupDays.value = prefs.getString(Settings.PREFERENCE_AUTOBACKUP_DAYS, Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE) ?: Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE

        refreshPaths()
    }

    fun setExternalAccess(enabled: Boolean) {
        prefs.edit().putBoolean(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS, enabled).apply()
        _externalAccess.value = enabled
    }

    fun setLockTimeout(timeout: String) {
        prefs.edit().putString(Settings.PREFERENCE_LOCK_TIMEOUT, timeout).apply()
        _lockTimeout.value = timeout
    }

    fun setLockOnScreenLock(enabled: Boolean) {
        prefs.edit().putBoolean(Settings.PREFERENCE_LOCK_ON_SCREEN_LOCK, enabled).apply()
        _lockOnScreenLock.value = enabled
    }

    fun setKeypad(enabled: Boolean) {
        prefs.edit().putBoolean(Settings.PREFERENCE_KEYPAD, enabled).apply()
        _keypad.value = enabled
    }

    fun setKeypadMute(enabled: Boolean) {
        prefs.edit().putBoolean(Settings.PREFERENCE_KEYPAD_MUTE, enabled).apply()
        _keypadMute.value = enabled
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(Settings.PREFERENCE_AUTOBACKUP, enabled).apply()
        _autoBackup.value = enabled
    }

    fun setAutoBackupDays(days: String) {
        prefs.edit().putString(Settings.PREFERENCE_AUTOBACKUP_DAYS, days).apply()
        _autoBackupDays.value = days
    }

    fun refreshPaths() {
        _backupPath.value = getSmartBackupPath()
        _exportPath.value = getSmartExportPath()
    }

    private fun getSmartBackupPath(): String {
        val method = prefs.getString(Settings.PREFERENCE_BACKUP_METHOD, Settings.IO_METHOD_FILE)
        return if (method == Settings.IO_METHOD_DOCUMENT_PROVIDER) {
             Settings.getBackupDocument(getApplication()) ?: ""
        } else {
             Settings.getBackupPath(getApplication())
        }
    }

    private fun getSmartExportPath(): String {
        val method = prefs.getString(Settings.PREFERENCE_EXPORT_METHOD, Settings.IO_METHOD_FILE)
        return if (method == Settings.IO_METHOD_DOCUMENT_PROVIDER) {
             Settings.getExportDocument(getApplication()) ?: ""
        } else {
             Settings.getExportPath(getApplication())
        }
    }
}
