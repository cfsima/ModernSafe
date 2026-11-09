package io.github.cfsima.modernsafe

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.biometric.BiometricHelper
import io.github.cfsima.modernsafe.password.Master
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskPasswordUiState(
    val isFirstTime: Boolean = false,
    val isKeypadMode: Boolean = false,
    val isMuted: Boolean = false,
    val keypadInput: String = "",
    val showDatabaseError: Boolean = false,
    val showVersionError: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

sealed class AskPasswordEffect {
    object UnlockSuccess : AskPasswordEffect()
    object PlayDigitSound : AskPasswordEffect()
    object PlayErrorSound : AskPasswordEffect()
    object PlaySuccessSound : AskPasswordEffect()
    object ShakeError : AskPasswordEffect()
    object NavigateToRestoreFirstTime : AskPasswordEffect()
    data class ShowToast(val messageResId: Int) : AskPasswordEffect()
    object CloseApp : AskPasswordEffect()
}

class AskPasswordViewModel @JvmOverloads constructor(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val TAG = "AskPasswordViewModel"

    private val _uiState = MutableStateFlow(AskPasswordUiState())
    val uiState: StateFlow<AskPasswordUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AskPasswordEffect>()
    val effects: SharedFlow<AskPasswordEffect> = _effects.asSharedFlow()

    private var dbHelper: DBHelper? = null
    private val cryptoHelper = CryptoHelper()
    private var dbSalt: String = ""

    // Stores the ENCRYPTED master key loaded from the database.
    // This is the "lock" we are trying to open.
    private var cachedEncryptedMasterKey: String = ""

    // Stores the DECRYPTED (plaintext) master key after successful unlock.
    // This is the "key" we hand over to the rest of the app.
    private var sessionMasterKey: String = ""

    init {
        initialize()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val canBiometric = BiometricHelper.isBiometricAvailable(getApplication()) &&
                        settings.biometricEnabled &&
                        !settings.biometricPayload.isNullOrEmpty()
                _uiState.update {
                    it.copy(
                        isKeypadMode = settings.keypad,
                        isMuted = settings.keypadMute,
                        isBiometricAvailable = canBiometric
                    )
                }
            }
        }
    }

    fun checkSignedIn() {
        if (AuthManager.isSignedIn()) {
             viewModelScope.launch { _effects.emit(AskPasswordEffect.UnlockSuccess) }
        }
    }

    private fun initialize() {
        val context = getApplication<Application>()
        dbHelper = DBHelper(context)

        if (dbHelper?.isDatabaseOpen() == false) {
            _uiState.update { it.copy(showDatabaseError = true) }
            return
        }

        if (dbHelper?.needsUpgrade == true) {
            if (dbHelper?.fetchVersion() == 2) {
                 _uiState.update { it.copy(showVersionError = true) }
                 return
            }
        }

        dbSalt = dbHelper?.fetchSalt() ?: ""
        // Note: fetchMasterKey returns encrypted key from DB
        cachedEncryptedMasterKey = dbHelper?.fetchMasterKey() ?: ""

        val userSettings = settingsRepository.userSettings.value
        val canBiometric = BiometricHelper.isBiometricAvailable(context) &&
                userSettings.biometricEnabled &&
                !userSettings.biometricPayload.isNullOrEmpty()
        _uiState.update {
            it.copy(
                isFirstTime = cachedEncryptedMasterKey.isEmpty(),
                isKeypadMode = userSettings.keypad,
                isMuted = userSettings.keypadMute,
                isBiometricAvailable = canBiometric
            )
        }
    }

    fun onPasswordSubmit(password: String) {
        if (password.length < 4) {
             viewModelScope.launch {
                 _effects.emit(AskPasswordEffect.ShowToast(R.string.notify_blank_pass))
                 _effects.emit(AskPasswordEffect.ShakeError)
             }
             return
        }

        if (checkUserPassword(password)) {
             viewModelScope.launch {
                 if (_uiState.value.isKeypadMode && !_uiState.value.isMuted) {
                      _effects.emit(AskPasswordEffect.PlaySuccessSound)
                 }
                 _effects.emit(AskPasswordEffect.UnlockSuccess)
             }
        } else {
             viewModelScope.launch {
                 _effects.emit(AskPasswordEffect.ShowToast(R.string.invalid_password))
                 if (_uiState.value.isKeypadMode && !_uiState.value.isMuted) {
                     _effects.emit(AskPasswordEffect.PlayErrorSound)
                 }
                 _effects.emit(AskPasswordEffect.ShakeError)
             }
        }
    }

    fun onFirstTimeSetup(password: String, confirm: String) {
        if (password.length < 4) {
             viewModelScope.launch {
                 _effects.emit(AskPasswordEffect.ShowToast(R.string.notify_blank_pass))
                 _effects.emit(AskPasswordEffect.ShakeError)
             }
             return
        }

        if (password != confirm) {
             viewModelScope.launch {
                 _effects.emit(AskPasswordEffect.ShowToast(R.string.confirm_pass_fail))
                 _effects.emit(AskPasswordEffect.ShakeError)
             }
             return
        }

        try {
            val salt = CryptoHelper.generateSalt(CryptoHelper.SALT_LENGTH)
            val masterKey = CryptoHelper.generateMasterKey()

            cryptoHelper.initV5(salt)
            cryptoHelper.setPassword(password)
            val encryptedMasterKey = cryptoHelper.encrypt(masterKey)

            dbHelper?.storeSalt(salt)
            dbHelper?.storeMasterKey(encryptedMasterKey)
            dbHelper?.updateVersion(5)

            // Set session vars
            sessionMasterKey = masterKey // Cleartext
            cachedEncryptedMasterKey = encryptedMasterKey // Update cache so we don't need re-init
            dbSalt = salt

            viewModelScope.launch {
                _effects.emit(AskPasswordEffect.UnlockSuccess)
            }
        } catch (e: Exception) {
             viewModelScope.launch {
                 _effects.emit(AskPasswordEffect.ShowToast(R.string.crypto_error))
             }
        }
    }

    private fun checkUserPassword(password: String): Boolean {
        val helper = dbHelper ?: return false

        // Always try to decrypt the cached ENCRYPTED key
        if (cachedEncryptedMasterKey.isEmpty()) {
            cachedEncryptedMasterKey = helper.fetchMasterKey()
            if (cachedEncryptedMasterKey.isEmpty()) return false
        }

        try {
            val isV5 = cachedEncryptedMasterKey.startsWith(CryptoHelper.V5_PREFIX)
            if (isV5) {
                cryptoHelper.initV5(dbSalt)
                cryptoHelper.setPassword(password)
                val decryptedMasterKey = cryptoHelper.decrypt(cachedEncryptedMasterKey)

                if (cryptoHelper.status && decryptedMasterKey.isNotEmpty()) {
                    sessionMasterKey = decryptedMasterKey
                    return true
                }
            } else {
                // Legacy V4 unlock attempt
                cryptoHelper.init(CryptoHelper.EncryptionStrong, dbSalt)
                cryptoHelper.setPassword(password)
                var decryptedMasterKey = cryptoHelper.decrypt(cachedEncryptedMasterKey)

                if (!cryptoHelper.status || decryptedMasterKey.isEmpty()) {
                    // Try medium encryption fallback
                    val chMed = CryptoHelper()
                    chMed.init(CryptoHelper.EncryptionMedium, dbSalt)
                    chMed.setPassword(password)
                    val medDecrypted = chMed.decrypt(cachedEncryptedMasterKey)
                    if (chMed.status && medDecrypted.isNotEmpty()) {
                        decryptedMasterKey = medDecrypted
                        cryptoHelper.status = true
                    }
                }

                if (cryptoHelper.status && decryptedMasterKey.isNotEmpty()) {
                    sessionMasterKey = decryptedMasterKey
                    // Transparently migrate database to V5
                    migrateToV5(password, decryptedMasterKey)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkUserPassword failed", e)
        }
        return false
    }

    private fun migrateToV5(password: String, masterKey: String) {
        val helper = dbHelper ?: return
        try {
            Log.i(TAG, "Starting vault migration from legacy to V5 (AES-256-GCM + PBKDF2)...")

            val legacyDekCrypto = CryptoHelper()
            legacyDekCrypto.init(CryptoHelper.EncryptionMedium, dbSalt)
            legacyDekCrypto.setPassword(masterKey)

            // 1. Decrypt existing categories
            val categories = helper.fetchAllCategoryRows()
            val decryptedCategories = categories.map { cat ->
                val plain = try { legacyDekCrypto.decrypt(cat.name) } catch (e: Exception) { cat.name }
                cat.id to plain
            }

            // 2. Decrypt existing passwords
            val passwords = helper.fetchAllRows(0)
            val decryptedPasswords = passwords.map { row ->
                val plainDesc = try { legacyDekCrypto.decrypt(row.description) } catch (e: Exception) { row.description }
                val plainWeb = if (!row.website.isNullOrEmpty()) try { legacyDekCrypto.decrypt(row.website) } catch (e: Exception) { row.website } else ""
                val plainUser = if (!row.username.isNullOrEmpty()) try { legacyDekCrypto.decrypt(row.username) } catch (e: Exception) { row.username } else ""
                val plainPass = try { legacyDekCrypto.decrypt(row.password) } catch (e: Exception) { row.password }
                val plainNote = if (!row.note.isNullOrEmpty()) try { legacyDekCrypto.decrypt(row.note) } catch (e: Exception) { row.note } else ""
                val plainUnique = if (!row.uniqueName.isNullOrEmpty()) try { legacyDekCrypto.decrypt(row.uniqueName) } catch (e: Exception) { row.uniqueName } else ""
                Triple(row, plainDesc to plainPass, listOf(plainWeb, plainUser, plainNote, plainUnique))
            }

            // 3. Initialize V5 crypto with 32-byte salt
            val newSalt = CryptoHelper.generateSalt(CryptoHelper.SALT_LENGTH)
            val v5KekCrypto = CryptoHelper()
            v5KekCrypto.initV5(newSalt)
            v5KekCrypto.setPassword(password)
            val newEncryptedMasterKey = v5KekCrypto.encrypt(masterKey)

            val v5DekCrypto = CryptoHelper()
            v5DekCrypto.initV5WithMasterKey(masterKey)

            // 4. Re-encrypt all records and commit atomically
            if (helper.beginTransaction()) {
                for ((id, plainName) in decryptedCategories) {
                    val entry = helper.fetchCategory(id)
                    entry.name = v5DekCrypto.encrypt(plainName)
                    helper.updateCategory(id, entry)
                }

                for ((row, descPass, others) in decryptedPasswords) {
                    row.description = v5DekCrypto.encrypt(descPass.first)
                    row.password = v5DekCrypto.encrypt(descPass.second)
                    row.website = if (others[0].isNotEmpty()) v5DekCrypto.encrypt(others[0]) else ""
                    row.username = if (others[1].isNotEmpty()) v5DekCrypto.encrypt(others[1]) else ""
                    row.note = if (others[2].isNotEmpty()) v5DekCrypto.encrypt(others[2]) else ""
                    row.uniqueName = if (others[3].isNotEmpty()) v5DekCrypto.encrypt(others[3]) else ""
                    helper.updatePassword(row.id, row)
                }

                helper.storeSalt(newSalt)
                helper.storeMasterKey(newEncryptedMasterKey)
                helper.updateVersion(5)
                helper.commit()

                dbSalt = newSalt
                cachedEncryptedMasterKey = newEncryptedMasterKey
                cryptoHelper.initV5(newSalt)
                cryptoHelper.setPassword(password)

                Log.i(TAG, "Vault migration to V5 complete!")
            }
        } catch (e: Exception) {
            helper.rollback()
            Log.e(TAG, "Failed to migrate vault to V5", e)
        }
    }

    fun onUnlockSuccess() {
        Master.masterKey = sessionMasterKey
        Master.salt = dbSalt
        AuthManager.cryptoHelper = cryptoHelper
        AuthManager.updateLastActiveTime()
    }

    fun toggleKeypadMode() {
        val newMode = !_uiState.value.isKeypadMode
        viewModelScope.launch {
            settingsRepository.setKeypad(newMode)
        }
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        viewModelScope.launch {
            settingsRepository.setKeypadMute(newMute)
        }
    }

    fun onKeypadInput(digit: Char) {
        val current = _uiState.value.keypadInput
        _uiState.update { it.copy(keypadInput = current + digit) }
        if (!_uiState.value.isMuted) {
             viewModelScope.launch { _effects.emit(AskPasswordEffect.PlayDigitSound) }
        }
    }

    fun onKeypadDelete() {
        val current = _uiState.value.keypadInput
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(keypadInput = current.dropLast(1)) }
        }
    }

    fun onKeypadClear() {
        _uiState.update { it.copy(keypadInput = "") }
    }

    fun onKeypadEnter() {
        val password = _uiState.value.keypadInput

        if (password.length < 4) {
             viewModelScope.launch {
                 if (!_uiState.value.isMuted) _effects.emit(AskPasswordEffect.PlayErrorSound)
                 _effects.emit(AskPasswordEffect.ShakeError)
                 _uiState.update { it.copy(keypadInput = "") }
             }
             return
        }

        if (checkUserPassword(password)) {
             viewModelScope.launch {
                 if (!_uiState.value.isMuted) _effects.emit(AskPasswordEffect.PlaySuccessSound)
                 _effects.emit(AskPasswordEffect.UnlockSuccess)
             }
        } else {
             viewModelScope.launch {
                 if (!_uiState.value.isMuted) _effects.emit(AskPasswordEffect.PlayErrorSound)
                 _effects.emit(AskPasswordEffect.ShakeError)
                 _uiState.update { it.copy(keypadInput = "") }
             }
        }
    }

    fun onBiometricUnlock(activity: Activity) {
        val payload = settingsRepository.getBiometricPayload() ?: return
        val cipher = BiometricHelper.initDecryptCipher(payload)
        if (cipher == null) {
            viewModelScope.launch {
                settingsRepository.setBiometricEnabled(false)
                _uiState.update { it.copy(isBiometricAvailable = false) }
                _effects.emit(AskPasswordEffect.ShowToast(R.string.invalid_password))
            }
            return
        }

        BiometricHelper.showBiometricPrompt(
            activity = activity,
            cipher = cipher,
            onSuccess = { authCipher ->
                try {
                    val decryptedMasterKey = BiometricHelper.decryptMasterKey(authCipher, payload)
                    if (decryptedMasterKey.isNotEmpty()) {
                        sessionMasterKey = decryptedMasterKey
                        onUnlockSuccess()
                        viewModelScope.launch {
                            _effects.emit(AskPasswordEffect.UnlockSuccess)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Biometric decryption failed", e)
                    viewModelScope.launch {
                        _effects.emit(AskPasswordEffect.ShakeError)
                    }
                }
            },
            onError = { err ->
                Log.d(TAG, "Biometric prompt error: $err")
            },
            onCancel = {}
        )
    }

    override fun onCleared() {
        super.onCleared()
        dbHelper?.close()
    }
}
