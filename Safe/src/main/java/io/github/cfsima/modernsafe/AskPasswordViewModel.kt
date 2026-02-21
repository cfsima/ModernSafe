package io.github.cfsima.modernsafe

import android.app.Application
import android.preference.PreferenceManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val showVersionError: Boolean = false
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

class AskPasswordViewModel(application: Application) : AndroidViewModel(application) {

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
    }

    fun checkSignedIn() {
        if (AuthManager.isSignedIn()) {
             viewModelScope.launch { _effects.emit(AskPasswordEffect.UnlockSuccess) }
        }
    }

    private fun initialize() {
        val context = getApplication<Application>()
        dbHelper = DBHelper(context)

        if (dbHelper?.isDatabaseOpen == false) {
            _uiState.update { it.copy(showDatabaseError = true) }
            return
        }

        if (dbHelper?.needsUpgrade() == true) {
            if (dbHelper?.fetchVersion() == 2) {
                 _uiState.update { it.copy(showVersionError = true) }
                 return
            }
        }

        dbSalt = dbHelper?.fetchSalt() ?: ""
        // Note: fetchMasterKey returns encrypted key from DB
        cachedEncryptedMasterKey = dbHelper?.fetchMasterKey() ?: ""

        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKeypad = sp.getBoolean(Settings.PREFERENCE_KEYPAD, false)
        val prefMute = sp.getBoolean(Settings.PREFERENCE_KEYPAD_MUTE, false)

        _uiState.update {
            it.copy(
                isFirstTime = cachedEncryptedMasterKey.isEmpty(),
                isKeypadMode = prefKeypad,
                isMuted = prefMute
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
            val salt = CryptoHelper.generateSalt()
            val masterKey = CryptoHelper.generateMasterKey()

            cryptoHelper.init(CryptoHelper.EncryptionStrong, salt)
            cryptoHelper.setPassword(password)
            val encryptedMasterKey = cryptoHelper.encrypt(masterKey)

            dbHelper?.storeSalt(salt)
            dbHelper?.storeMasterKey(encryptedMasterKey)

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
        if (dbHelper == null) return false

        // Always try to decrypt the cached ENCRYPTED key
        if (cachedEncryptedMasterKey.isEmpty()) {
            cachedEncryptedMasterKey = dbHelper?.fetchMasterKey() ?: return false
        }

        try {
            cryptoHelper.init(CryptoHelper.EncryptionStrong, dbSalt)
            cryptoHelper.setPassword(password)
            val decryptedMasterKey = cryptoHelper.decrypt(cachedEncryptedMasterKey)

            if (cryptoHelper.status) {
                sessionMasterKey = decryptedMasterKey // Store success result
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun onUnlockSuccess() {
        Master.setMasterKey(sessionMasterKey)
        Master.setSalt(dbSalt)
        CryptoContentProvider.ch = cryptoHelper
    }

    fun toggleKeypadMode() {
        val newMode = !_uiState.value.isKeypadMode
        _uiState.update { it.copy(isKeypadMode = newMode) }

        val sp = PreferenceManager.getDefaultSharedPreferences(getApplication())
        sp.edit().putBoolean(Settings.PREFERENCE_KEYPAD, newMode).apply()
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = newMute) }

        val sp = PreferenceManager.getDefaultSharedPreferences(getApplication())
        sp.edit().putBoolean(Settings.PREFERENCE_KEYPAD_MUTE, newMute).apply()
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

    fun onRestoreClicked() {
        viewModelScope.launch { _effects.emit(AskPasswordEffect.NavigateToRestoreFirstTime) }
    }

    override fun onCleared() {
        super.onCleared()
        dbHelper?.close()
    }
}
