package io.github.cfsima.modernsafe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository.getInstance(application)
) : AndroidViewModel(application) {

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

    fun setExportDocument(uriString: String) {
        viewModelScope.launch {
            settingsRepository.setExportDocumentAndMethod(uriString)
        }
    }

    fun refreshPaths() {
        // No-op: paths are reactively updated via Flow
    }
}
