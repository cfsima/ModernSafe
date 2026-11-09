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
    val masterKey: String? = null,
    val userPassword: String? = null
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

             if (salt.isEmpty() || masterKeyEncrypted.isEmpty()) {
                  _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Invalid backup data (missing salt or master key)")
                  return@launch
             }

             var masterKey: String?
             try {
                 val ch = CryptoHelper()
                 val isV5 = masterKeyEncrypted.startsWith(CryptoHelper.V5_PREFIX)
                 var unlockSuccess: Boolean
                 if (isV5) {
                     ch.initV5(salt)
                     ch.setPassword(password)
                     masterKey = ch.decrypt(masterKeyEncrypted)
                     unlockSuccess = ch.status && masterKey.isNotEmpty()
                 } else {
                     ch.init(CryptoHelper.EncryptionStrong, salt)
                     ch.setPassword(password)
                     masterKey = ch.decrypt(masterKeyEncrypted)
                     unlockSuccess = ch.status && masterKey.isNotEmpty()

                     if (!unlockSuccess) {
                         val chMed = CryptoHelper()
                         chMed.init(CryptoHelper.EncryptionMedium, salt)
                         chMed.setPassword(password)
                         val medDecrypted = try { chMed.decrypt(masterKeyEncrypted) } catch (_: Exception) { null }
                         if (chMed.status && !medDecrypted.isNullOrEmpty()) {
                             masterKey = medDecrypted
                             unlockSuccess = true
                         }
                     }
                 }

                 if (!unlockSuccess || masterKey.isEmpty()) {
                     _uiState.value = _uiState.value.copy(status = RestoreStatus.PasswordRequired, error = "Incorrect password (decrypt failed)")
                     return@launch
                 }

                 // Verify by decrypting first category
                 if (data.categories.isNotEmpty()) {
                     val firstCat = data.categories[0]
                     val ch2 = CryptoHelper()
                     if (firstCat.name.startsWith(CryptoHelper.V5_PREFIX)) {
                         ch2.initV5WithMasterKey(masterKey)
                     } else {
                         ch2.init(CryptoHelper.EncryptionMedium, salt)
                         ch2.setPassword(masterKey)
                     }
                     ch2.decrypt(firstCat.name)
                     if (!ch2.status) {
                          _uiState.value = _uiState.value.copy(status = RestoreStatus.PasswordRequired, error = "Incorrect password (category decrypt failed)")
                          return@launch
                     }
                 }

                 // Success! Ask for confirmation
                 _uiState.value = _uiState.value.copy(
                     status = RestoreStatus.Confirming,
                     masterKey = masterKey,
                     userPassword = password
                 )

             } catch (e: Exception) {
                 _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = e.localizedMessage)
             }
        }
    }

    fun performRestore() {
        val data = _uiState.value.restoreDataSet ?: return
        val masterKey = _uiState.value.masterKey ?: return
        val userPassword = _uiState.value.userPassword

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(status = RestoreStatus.Restoring)

            val dbHelper = DBHelper(context)
            try {
                if (!dbHelper.beginTransaction()) {
                     _uiState.value = RestoreUiState(status = RestoreStatus.Error, error = "Database transaction failed")
                     return@launch
                }

                dbHelper.deleteDatabase()

                val isV5Backup = data.masterKeyEncrypted.startsWith(CryptoHelper.V5_PREFIX)

                if (isV5Backup || userPassword == null) {
                    // Backup is already V5 (or no password provided): write directly
                    dbHelper.storeSalt(data.salt)
                    dbHelper.storeMasterKey(data.masterKeyEncrypted)
                    val version = if (isV5Backup) 5 else 4
                    dbHelper.updateVersion(version)
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
                    val strength = if (isV5Backup) CryptoHelper.EncryptionV5 else CryptoHelper.EncryptionMedium
                    Passwords.InitCrypto(strength, data.salt, masterKey)
                    Passwords.Reset()
                } else {
                    // Upgrade legacy backup transparently to V5 (AES-256-GCM + PBKDF2)
                    val legacyDekCrypto = CryptoHelper()
                    legacyDekCrypto.init(CryptoHelper.EncryptionMedium, data.salt)
                    legacyDekCrypto.setPassword(masterKey)

                    val newSalt = CryptoHelper.generateSalt(CryptoHelper.SALT_LENGTH)
                    val v5KekCrypto = CryptoHelper()
                    v5KekCrypto.initV5(newSalt)
                    v5KekCrypto.setPassword(userPassword)
                    val newEncryptedMasterKey = v5KekCrypto.encrypt(masterKey)

                    val v5DekCrypto = CryptoHelper()
                    v5DekCrypto.initV5WithMasterKey(masterKey)

                    dbHelper.storeSalt(newSalt)
                    dbHelper.storeMasterKey(newEncryptedMasterKey)
                    dbHelper.updateVersion(5)
                    Master.salt = newSalt
                    Master.masterKey = masterKey

                    for (category in data.categories) {
                        val plainName = try { legacyDekCrypto.decrypt(category.name) } catch (_: Exception) { category.name }
                        category.name = v5DekCrypto.encrypt(plainName)
                        dbHelper.addCategory(category)
                    }

                    for (passRow in data.pass) {
                        val plainDesc = try { legacyDekCrypto.decrypt(passRow.description) } catch (_: Exception) { passRow.description }
                        val plainPass = try { legacyDekCrypto.decrypt(passRow.password) } catch (_: Exception) { passRow.password }
                        val web = passRow.website
                        val plainWeb = if (!web.isNullOrEmpty()) {
                            try { legacyDekCrypto.decrypt(web) } catch (_: Exception) { web }
                        } else ""
                        val user = passRow.username
                        val plainUser = if (!user.isNullOrEmpty()) {
                            try { legacyDekCrypto.decrypt(user) } catch (_: Exception) { user }
                        } else ""
                        val note = passRow.note
                        val plainNote = if (!note.isNullOrEmpty()) {
                            try { legacyDekCrypto.decrypt(note) } catch (_: Exception) { note }
                        } else ""
                        val unique = passRow.uniqueName
                        val plainUnique = if (!unique.isNullOrEmpty()) {
                            try { legacyDekCrypto.decrypt(unique) } catch (_: Exception) { unique }
                        } else ""

                        passRow.description = v5DekCrypto.encrypt(plainDesc)
                        passRow.password = v5DekCrypto.encrypt(plainPass)
                        passRow.website = if (plainWeb.isNotEmpty()) v5DekCrypto.encrypt(plainWeb) else ""
                        passRow.username = if (plainUser.isNotEmpty()) v5DekCrypto.encrypt(plainUser) else ""
                        passRow.note = if (plainNote.isNotEmpty()) v5DekCrypto.encrypt(plainNote) else ""
                        passRow.uniqueName = if (plainUnique.isNotEmpty()) v5DekCrypto.encrypt(plainUnique) else ""

                        val rowid = dbHelper.addPassword(passRow)
                        passRow.packageAccess?.forEach { packageName ->
                            dbHelper.addPackageAccess(rowid, packageName)
                        }
                    }

                    dbHelper.commit()
                    Passwords.InitCrypto(CryptoHelper.EncryptionV5, newSalt, masterKey)
                    Passwords.Reset()
                }

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
