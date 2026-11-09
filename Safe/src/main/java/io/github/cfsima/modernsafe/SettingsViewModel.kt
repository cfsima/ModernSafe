package io.github.cfsima.modernsafe

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.biometric.BiometricHelper
import io.github.cfsima.modernsafe.password.Master
import io.github.cfsima.modernsafe.util.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val externalAccess: Boolean = false,
    val lockTimeout: String = Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE,
    val lockOnScreenLock: Boolean = true,
    val keypad: Boolean = false,
    val keypadMute: Boolean = false,
    val autoBackup: Boolean = true,
    val autoBackupDays: String = Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE,
    val backupPath: String = "",
    val exportPath: String = "",
    val currentLanguage: String = "",
    val biometricEnabled: Boolean = false,
    val biometricStatus: BiometricHelper.BiometricStatus = BiometricHelper.BiometricStatus.UNAVAILABLE,
    val themePalette: String = "dynamic",
    val darkMode: String = "system"
)

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _currentLanguage = MutableStateFlow(LanguageManager.getCurrentLanguageTag())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _biometricStatus = MutableStateFlow(BiometricHelper.checkBiometricStatus(application))
    val biometricStatus: StateFlow<BiometricHelper.BiometricStatus> = _biometricStatus.asStateFlow()

    fun refreshBiometricStatus() {
        _biometricStatus.value = BiometricHelper.checkBiometricStatus(getApplication())
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _currentLanguage,
        _biometricStatus
    ) { settings, lang, bioStatus ->
        SettingsUiState(
            externalAccess = settings.externalAccess,
            lockTimeout = settings.lockTimeout,
            lockOnScreenLock = settings.lockOnScreenLock,
            keypad = settings.keypad,
            keypadMute = settings.keypadMute,
            autoBackup = settings.autoBackup,
            autoBackupDays = settings.autoBackupDays,
            backupPath = settings.displayBackupPath,
            exportPath = settings.displayExportPath,
            currentLanguage = lang,
            biometricEnabled = settings.biometricEnabled,
            biometricStatus = bioStatus,
            themePalette = settings.themePalette,
            darkMode = settings.darkMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    val externalAccess: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.externalAccess }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.externalAccess
        )

    val lockTimeout: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.lockTimeout }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.lockTimeout
        )

    val lockOnScreenLock: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.lockOnScreenLock }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.lockOnScreenLock
        )

    val keypad: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.keypad }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.keypad
        )

    val keypadMute: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.keypadMute }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.keypadMute
        )

    val autoBackup: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.autoBackup }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.autoBackup
        )

    val autoBackupDays: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.autoBackupDays }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.autoBackupDays
        )

    val backupPath: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.displayBackupPath }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.displayBackupPath
        )

    val exportPath: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.displayExportPath }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.displayExportPath
        )

    val biometricEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.biometricEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.userSettings.value.biometricEnabled
        )

    val isBiometricAvailable: Boolean
        get() = BiometricHelper.isBiometricAvailable(getApplication())

    fun setLanguage(tag: String) {
        _currentLanguage.value = tag
        LanguageManager.setLanguage(tag)
    }

    fun setExternalAccess(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExternalAccess(enabled)
        }
    }

    fun setLockTimeout(timeout: String) {
        viewModelScope.launch {
            settingsRepository.setLockTimeout(timeout)
        }
    }

    fun setLockOnScreenLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLockOnScreenLock(enabled)
        }
    }

    fun setKeypad(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeypad(enabled)
        }
    }

    fun setKeypadMute(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeypadMute(enabled)
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackup(enabled)
        }
    }

    fun setAutoBackupDays(days: String) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupDays(days)
        }
    }

    fun setBackupDocument(uriString: String) {
        viewModelScope.launch {
            settingsRepository.setBackupDocumentAndMethod(uriString)
        }
    }

    fun setBackupPath(uriString: String) {
        setBackupDocument(uriString)
    }

    fun setExportDocument(uriString: String) {
        viewModelScope.launch {
            settingsRepository.setExportDocumentAndMethod(uriString)
        }
    }

    fun setExportPath(uriString: String) {
        setExportDocument(uriString)
    }

    fun setThemePalette(palette: String) {
        viewModelScope.launch {
            settingsRepository.setThemePalette(palette)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(mode)
        }
    }

    fun onBiometricChange(activity: Activity, enabled: Boolean) {
        if (!enabled) {
            BiometricHelper.deleteKey()
            viewModelScope.launch {
                settingsRepository.setBiometricEnabled(false)
            }
            return
        }

        val masterKey = Master.masterKey ?: return
        val cipher = BiometricHelper.initEncryptCipher()
        if (cipher == null) {
            Log.e("SettingsViewModel", "Failed to init biometric encrypt cipher")
            return
        }

        BiometricHelper.showBiometricPrompt(
            activity = activity,
            cipher = cipher,
            title = activity.getString(R.string.biometric_prompt_enroll_title),
            subtitle = activity.getString(R.string.biometric_prompt_enroll_subtitle),
            onSuccess = { authCipher ->
                try {
                    val payload = BiometricHelper.encryptMasterKey(authCipher, masterKey)
                    viewModelScope.launch {
                        settingsRepository.setBiometricEnabled(true, payload)
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Biometric enrollment encryption failed", e)
                }
            },
            onError = { err ->
                Log.d("SettingsViewModel", "Biometric enroll error: $err")
            },
            onCancel = {}
        )
    }

    fun refreshPaths() {
        // No-op: paths are reactively updated via Flow
    }
}
