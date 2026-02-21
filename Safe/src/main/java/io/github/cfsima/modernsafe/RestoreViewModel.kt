package io.github.cfsima.modernsafe

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.model.RestoreDataSet
import io.github.cfsima.modernsafe.password.Master
import io.github.cfsima.modernsafe.model.Passwords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xml.sax.InputSource
import java.io.FileInputStream
import javax.xml.parsers.SAXParserFactory

data class RestoreUiState(
    val status: RestoreStatus = RestoreStatus.Idle,
    val error: String? = null,
    val restoreDataSet: RestoreDataSet? = null,
    val masterKey: String? = null
)

sealed class RestoreStatus {
    object Idle : RestoreStatus()
    object Parsing : RestoreStatus()
    object PasswordRequired : RestoreStatus()
    object Decrypting : RestoreStatus()
    object Confirming : RestoreStatus()
    object Restoring : RestoreStatus()
    object Success : RestoreStatus()
    object Error : RestoreStatus()
}

class RestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    private val context = application

    fun loadFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = RestoreUiState(status = RestoreStatus.Parsing)
            try {
                FileInputStream(path).use { inputStream ->
                    parse(InputSource(inputStream))
                }
            } catch (e: Exception) {
                _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = e.localizedMessage)
            }
        }
    }

    fun loadUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = RestoreUiState(status = RestoreStatus.Parsing)
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parse(InputSource(inputStream))
                } ?: run {
                    _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Could not open URI")
                }
            } catch (e: Exception) {
                _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = e.localizedMessage)
            }
        }
    }

    private fun parse(inputSource: InputSource) {
        try {
            val spf = SAXParserFactory.newInstance()
            val sp = spf.newSAXParser()
            val xr = sp.xmlReader
            val handler = RestoreHandler()
            xr.contentHandler = handler
            xr.parse(inputSource)

            val data = handler.parsedData
            if (data.version != Backup.CURRENT_VERSION) {
                _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Backup version mismatch: ${data.version}")
                return
            }

            _uiState.value = RestoreUiState(status = RestoreStatus.PasswordRequired, restoreDataSet = data)

        } catch (e: Exception) {
            Log.e("RestoreViewModel", "Error parsing", e)
            _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = e.localizedMessage)
        }
    }

    fun verifyPasswordAndDecrypt(password: String) {
        val data = _uiState.value.restoreDataSet ?: return
        viewModelScope.launch(Dispatchers.Default) {
             _uiState.value = _uiState.value.copy(status = RestoreStatus.Decrypting)

             val salt = data.salt
             val masterKeyEncrypted = data.masterKeyEncrypted

             if (salt.isNullOrEmpty() || masterKeyEncrypted.isNullOrEmpty()) {
                  _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Invalid backup data (missing salt or master key)")
                  return@launch
             }

             var masterKey: String? = null
             try {
                 val ch = CryptoHelper()
                 ch.init(CryptoHelper.EncryptionStrong, salt)
                 ch.setPassword(password)
                 masterKey = ch.decrypt(masterKeyEncrypted)

                 if (!ch.status) {
                     _uiState.value = _uiState.value.copy(status = RestoreStatus.PasswordRequired, error = "Incorrect password (decrypt failed)")
                     return@launch
                 }

                 // Verify by decrypting first category
                 if (data.categories.isNotEmpty()) {
                     val firstCat = data.categories[0]
                     val ch2 = CryptoHelper()
                     ch2.init(CryptoHelper.EncryptionMedium, salt)
                     ch2.setPassword(masterKey)
                     ch2.decrypt(firstCat.name)
                     if (!ch2.status) {
                          _uiState.value = _uiState.value.copy(status = RestoreStatus.PasswordRequired, error = "Incorrect password (category decrypt failed)")
                          return@launch
                     }
                 }

                 // Success! Ask for confirmation
                 _uiState.value = _uiState.value.copy(status = RestoreStatus.Confirming, masterKey = masterKey)

             } catch (e: Exception) {
                 _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = e.localizedMessage)
             }
        }
    }

    fun performRestore() {
        val data = _uiState.value.restoreDataSet ?: return
        val masterKey = _uiState.value.masterKey ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(status = RestoreStatus.Restoring)

            val dbHelper = DBHelper(context)
            try {
                if (!dbHelper.beginTransaction()) {
                     _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Database transaction failed")
                     return@launch
                }

                dbHelper.deleteDatabase()

                dbHelper.storeSalt(data.salt)
                dbHelper.storeMasterKey(data.masterKeyEncrypted)
                Master.salt = data.salt
                Master.masterKey = masterKey

                for (category in data.categories) {
                    dbHelper.addCategory(category)
                }

                for (password in data.pass) {
                    val rowid = dbHelper.addPassword(password)
                    password.packageAccess?.forEach { packageName ->
                        dbHelper.addPackageAccess(rowid, packageName)
                    }
                }

                dbHelper.commit()
                Passwords.Reset()

                _uiState.value = _uiState.value.copy(status = RestoreStatus.Success)
            } catch (e: Exception) {
                dbHelper.rollback()
                _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Database error: ${e.localizedMessage}")
            } finally {
                dbHelper.close()
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        if (_uiState.value.status == RestoreStatus.Error) {
             _uiState.value = _uiState.value.copy(status = RestoreStatus.Idle)
        }
    }
}
