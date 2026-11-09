package io.github.cfsima.modernsafe

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private const val DATASTORE_NAME = "settings"
private const val TAG = "SettingsRepository"

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences"
            )
        )
    }
)

data class UserSettings(
    val externalAccess: Boolean = false,
    val lockTimeout: String = Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE,
    val lockOnScreenLock: Boolean = true,
    val firstTimeWarning: Boolean = false,
    val keypad: Boolean = false,
    val keypadMute: Boolean = false,
    val lastBackupJulian: Int = 0,
    val lastAutoBackupCheck: Int = 0,
    val autoBackup: Boolean = true,
    val autoBackupDays: String = Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE,
    val backupPath: String = "",
    val backupDocument: String? = null,
    val backupMethod: String = Settings.IO_METHOD_FILE,
    val exportPath: String = "",
    val exportDocument: String? = null,
    val exportMethod: String = Settings.IO_METHOD_FILE,
    val biometricEnabled: Boolean = false,
    val biometricPayload: String? = null
) {
    val displayBackupPath: String
        get() = if (backupMethod == Settings.IO_METHOD_DOCUMENT_PROVIDER) {
            backupDocument.orEmpty()
        } else {
            backupPath
        }

    val displayExportPath: String
        get() = if (exportMethod == Settings.IO_METHOD_DOCUMENT_PROVIDER) {
            exportDocument.orEmpty()
        } else {
            exportPath
        }

    companion object {
        fun createDefault(context: Context): UserSettings {
            return UserSettings(
                backupPath = Settings.getDefaultBackupPath(context),
                exportPath = Settings.getDefaultExportPath(context)
            )
        }
    }
}

class SettingsRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.applicationContext.settingsDataStore,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val appContext: Context = context.applicationContext

    object PreferencesKeys {
        val ALLOW_EXTERNAL_ACCESS = booleanPreferencesKey(Settings.PREFERENCE_ALLOW_EXTERNAL_ACCESS)
        val LOCK_TIMEOUT = stringPreferencesKey(Settings.PREFERENCE_LOCK_TIMEOUT)
        val LOCK_ON_SCREEN_LOCK = booleanPreferencesKey(Settings.PREFERENCE_LOCK_ON_SCREEN_LOCK)
        val FIRST_TIME_WARNING = booleanPreferencesKey(Settings.PREFERENCE_FIRST_TIME_WARNING)
        val KEYPAD = booleanPreferencesKey(Settings.PREFERENCE_KEYPAD)
        val KEYPAD_MUTE = booleanPreferencesKey(Settings.PREFERENCE_KEYPAD_MUTE)
        val LAST_BACKUP_JULIAN = intPreferencesKey(Settings.PREFERENCE_LAST_BACKUP_JULIAN)
        val LAST_AUTOBACKUP_CHECK = intPreferencesKey(Settings.PREFERENCE_LAST_AUTOBACKUP_CHECK)
        val AUTOBACKUP = booleanPreferencesKey(Settings.PREFERENCE_AUTOBACKUP)
        val AUTOBACKUP_DAYS = stringPreferencesKey(Settings.PREFERENCE_AUTOBACKUP_DAYS)
        val BACKUP_PATH = stringPreferencesKey(Settings.PREFERENCE_BACKUP_PATH)
        val BACKUP_DOCUMENT = stringPreferencesKey(Settings.PREFERENCE_BACKUP_DOCUMENT)
        val BACKUP_METHOD = stringPreferencesKey(Settings.PREFERENCE_BACKUP_METHOD)
        val EXPORT_PATH = stringPreferencesKey(Settings.PREFERENCE_EXPORT_PATH)
        val EXPORT_DOCUMENT = stringPreferencesKey(Settings.PREFERENCE_EXPORT_DOCUMENT)
        val EXPORT_METHOD = stringPreferencesKey(Settings.PREFERENCE_EXPORT_METHOD)
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val BIOMETRIC_PAYLOAD = stringPreferencesKey("biometric_payload")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences DataStore", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }
        .distinctUntilChanged()

    val userSettings: StateFlow<UserSettings> = settingsFlow
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSettings.createDefault(appContext)
        )

    val externalAccessFlow: Flow<Boolean> = settingsFlow.map { it.externalAccess }.distinctUntilChanged()
    val lockTimeoutFlow: Flow<String> = settingsFlow.map { it.lockTimeout }.distinctUntilChanged()
    val lockOnScreenLockFlow: Flow<Boolean> = settingsFlow.map { it.lockOnScreenLock }.distinctUntilChanged()
    val keypadFlow: Flow<Boolean> = settingsFlow.map { it.keypad }.distinctUntilChanged()
    val keypadMuteFlow: Flow<Boolean> = settingsFlow.map { it.keypadMute }.distinctUntilChanged()
    val autoBackupFlow: Flow<Boolean> = settingsFlow.map { it.autoBackup }.distinctUntilChanged()
    val autoBackupDaysFlow: Flow<String> = settingsFlow.map { it.autoBackupDays }.distinctUntilChanged()
    val displayBackupPathFlow: Flow<String> = settingsFlow.map { it.displayBackupPath }.distinctUntilChanged()
    val displayExportPathFlow: Flow<String> = settingsFlow.map { it.displayExportPath }.distinctUntilChanged()
    val biometricEnabledFlow: Flow<Boolean> = settingsFlow.map { it.biometricEnabled }.distinctUntilChanged()

    private fun mapPreferences(preferences: Preferences): UserSettings {
        val defaultBackup = Settings.getDefaultBackupPath(appContext)
        val defaultExport = Settings.getDefaultExportPath(appContext)

        return UserSettings(
            externalAccess = preferences[PreferencesKeys.ALLOW_EXTERNAL_ACCESS] ?: false,
            lockTimeout = preferences[PreferencesKeys.LOCK_TIMEOUT] ?: Settings.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE,
            lockOnScreenLock = preferences[PreferencesKeys.LOCK_ON_SCREEN_LOCK] ?: true,
            firstTimeWarning = preferences[PreferencesKeys.FIRST_TIME_WARNING] ?: false,
            keypad = preferences[PreferencesKeys.KEYPAD] ?: false,
            keypadMute = preferences[PreferencesKeys.KEYPAD_MUTE] ?: false,
            lastBackupJulian = preferences[PreferencesKeys.LAST_BACKUP_JULIAN] ?: 0,
            lastAutoBackupCheck = preferences[PreferencesKeys.LAST_AUTOBACKUP_CHECK] ?: 0,
            autoBackup = preferences[PreferencesKeys.AUTOBACKUP] ?: true,
            autoBackupDays = preferences[PreferencesKeys.AUTOBACKUP_DAYS] ?: Settings.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE,
            backupPath = preferences[PreferencesKeys.BACKUP_PATH] ?: defaultBackup,
            backupDocument = preferences[PreferencesKeys.BACKUP_DOCUMENT],
            backupMethod = preferences[PreferencesKeys.BACKUP_METHOD] ?: Settings.IO_METHOD_FILE,
            exportPath = preferences[PreferencesKeys.EXPORT_PATH] ?: defaultExport,
            exportDocument = preferences[PreferencesKeys.EXPORT_DOCUMENT],
            exportMethod = preferences[PreferencesKeys.EXPORT_METHOD] ?: Settings.IO_METHOD_FILE,
            biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
            biometricPayload = preferences[PreferencesKeys.BIOMETRIC_PAYLOAD]
        )
    }

    suspend fun setExternalAccess(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOW_EXTERNAL_ACCESS] = enabled
        }
    }

    suspend fun setLockTimeout(timeout: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_TIMEOUT] = timeout
        }
    }

    suspend fun setLockOnScreenLock(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_ON_SCREEN_LOCK] = enabled
        }
    }

    suspend fun setKeypad(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEYPAD] = enabled
        }
    }

    suspend fun setKeypadMute(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEYPAD_MUTE] = enabled
        }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOBACKUP] = enabled
        }
    }

    suspend fun setAutoBackupDays(days: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOBACKUP_DAYS] = days
        }
    }

    suspend fun setBackupPathAndMethod(path: String, method: String = Settings.IO_METHOD_FILE) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_PATH] = path
            preferences[PreferencesKeys.BACKUP_METHOD] = method
        }
    }

    suspend fun setBackupDocumentAndMethod(uriString: String, method: String = Settings.IO_METHOD_DOCUMENT_PROVIDER) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_DOCUMENT] = uriString
            preferences[PreferencesKeys.BACKUP_METHOD] = method
        }
    }

    suspend fun setExportPathAndMethod(path: String, method: String = Settings.IO_METHOD_FILE) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_PATH] = path
            preferences[PreferencesKeys.EXPORT_METHOD] = method
        }
    }

    suspend fun setExportDocumentAndMethod(uriString: String, method: String = Settings.IO_METHOD_DOCUMENT_PROVIDER) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPORT_DOCUMENT] = uriString
            preferences[PreferencesKeys.EXPORT_METHOD] = method
        }
    }

    suspend fun setLastBackupJulian(julianDay: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_JULIAN] = julianDay
        }
    }

    fun updateBackupPathAndMethodAsync(path: String) {
        externalScope.launch {
            setBackupPathAndMethod(path)
        }
    }

    fun updateBackupDocumentAndMethodAsync(uriString: String) {
        externalScope.launch {
            setBackupDocumentAndMethod(uriString)
        }
    }

    fun updateExportPathAndMethodAsync(path: String) {
        externalScope.launch {
            setExportPathAndMethod(path)
        }
    }

    fun updateExportDocumentAndMethodAsync(uriString: String) {
        externalScope.launch {
            setExportDocumentAndMethod(uriString)
        }
    }

    fun setLastBackupJulianAsync(julianDay: Int) {
        externalScope.launch {
            setLastBackupJulian(julianDay)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean, payload: String? = null) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
            if (payload != null) {
                preferences[PreferencesKeys.BIOMETRIC_PAYLOAD] = payload
            } else if (!enabled) {
                preferences.remove(PreferencesKeys.BIOMETRIC_PAYLOAD)
            }
        }
    }

    fun isBiometricEnabled(): Boolean = userSettings.value.biometricEnabled
    fun getBiometricPayload(): String? = userSettings.value.biometricPayload

    fun getBackupPath(): String = userSettings.value.backupPath
    fun getBackupDocument(): String? = userSettings.value.backupDocument
    fun getExportPath(): String = userSettings.value.exportPath
    fun getExportDocument(): String? = userSettings.value.exportDocument
    fun isExternalAccessAllowed(): Boolean = userSettings.value.externalAccess

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
