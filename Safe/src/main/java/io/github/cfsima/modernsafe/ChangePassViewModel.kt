package io.github.cfsima.modernsafe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.model.Passwords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChangePassUiState(
    val oldPasswordError: Int? = null,
    val newPasswordError: Int? = null,
    val confirmPasswordError: Int? = null,
    val generalError: String? = null,
    val isSuccess: Boolean = false,
    val isLoading: Boolean = false
)

class ChangePassViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChangePassUiState())
    val uiState: StateFlow<ChangePassUiState> = _uiState.asStateFlow()

    fun clearErrors() {
        _uiState.update {
            it.copy(
                oldPasswordError = null,
                newPasswordError = null,
                confirmPasswordError = null,
                generalError = null
            )
        }
    }

    fun changePassword(oldPass: String, newPass: String, confirmPass: String) {
        clearErrors()

        if (newPass != confirmPass) {
            _uiState.update { it.copy(confirmPasswordError = R.string.new_verify_mismatch) }
            return
        }

        if (newPass.length < 4) {
            _uiState.update { it.copy(newPasswordError = R.string.notify_blank_pass) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) {
                performChangePass(oldPass, newPass)
            }
            _uiState.update { it.copy(isLoading = false) }

            if (result) {
                _uiState.update { it.copy(isSuccess = true) }
            }
        }
    }

    private fun performChangePass(oldPass: String, newPass: String): Boolean {
        val context = getApplication<Application>()
        val dbHelper = DBHelper(context)
        val ch = CryptoHelper()

        try {
            val encryptedMasterKey = dbHelper.fetchMasterKey()
            val salt = dbHelper.fetchSalt()

            ch.init(CryptoHelper.EncryptionStrong, salt)
            ch.setPassword(oldPass)

            val decryptedMasterKey = try {
                ch.decrypt(encryptedMasterKey)
            } catch (e: Exception) {
                null
            }

            if (decryptedMasterKey == null || !ch.status) {
                _uiState.update { it.copy(oldPasswordError = R.string.invalid_old_password) }
                return false
            }

            // Re-encrypt with new password
            ch.setPassword(newPass)
            val newEncryptedMasterKey = ch.encrypt(decryptedMasterKey)

            if (!ch.status) {
                _uiState.update { it.copy(generalError = context.getString(R.string.crypto_error)) }
                return false
            }

            dbHelper.storeMasterKey(newEncryptedMasterKey)

            // Re-initialize Passwords helper with the DEK (Decrypted Master Key)
            Passwords.InitCrypto(CryptoHelper.EncryptionMedium, salt, decryptedMasterKey)
            Passwords.Reset()

            return true

        } catch (e: Exception) {
            android.util.Log.e("ChangePassViewModel", "Failed to change password", e)
            _uiState.update { it.copy(generalError = context.getString(R.string.error_changing_password)) }
            return false
        } finally {
            dbHelper.close()
        }
    }
}
