package io.github.cfsima.modernsafe

import android.content.Context
import java.io.File

@Suppress("unused")
object Settings {

    const val IO_METHOD_DOCUMENT_PROVIDER = "document_provider"
    const val IO_METHOD_FILE = "file"
    const val PREFERENCE_ALLOW_EXTERNAL_ACCESS = "external_access"
    const val PREFERENCE_LOCK_TIMEOUT = "lock_timeout"
    const val PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE = "5"
    const val PREFERENCE_LOCK_ON_SCREEN_LOCK = "lock_on_screen_lock"
    const val PREFERENCE_FIRST_TIME_WARNING = "first_time_warning"
    const val PREFERENCE_KEYPAD = "keypad"
    const val PREFERENCE_KEYPAD_MUTE = "keypad_mute"
    const val PREFERENCE_LAST_BACKUP_JULIAN = "last_backup_julian"
    const val PREFERENCE_LAST_AUTOBACKUP_CHECK = "last_autobackup_check"
    const val PREFERENCE_AUTOBACKUP = "autobackup"
    const val PREFERENCE_AUTOBACKUP_DAYS = "autobackup_days"
    const val PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE = "7"
    const val PREFERENCE_BACKUP_PATH = "backup_path"
    const val PREFERENCE_BACKUP_DOCUMENT = "backup_document"
    const val PREFERENCE_BACKUP_METHOD = "backup_method"
    const val OISAFE_XML = "oisafe.xml"
    const val PREFERENCE_EXPORT_PATH = "export_path"
    const val PREFERENCE_EXPORT_DOCUMENT = "export_document"
    const val PREFERENCE_EXPORT_METHOD = "export_method"
    const val OISAFE_CSV = "oisafe.csv"

    // Request Codes
    const val REQUEST_BACKUP_FILENAME = 0
    const val REQUEST_BACKUP_DOCUMENT = 1
    const val REQUEST_EXPORT_FILENAME = 2
    const val REQUEST_EXPORT_DOCUMENT = 3

    @JvmStatic
    fun getDefaultBackupPath(context: Context): String {
        val dir = context.getExternalFilesDir(null)
        return if (dir != null) {
            File(dir, OISAFE_XML).absolutePath
        } else {
            File(context.filesDir, OISAFE_XML).absolutePath
        }
    }

    @JvmStatic
    fun getDefaultExportPath(context: Context): String {
        val dir = context.getExternalFilesDir(null)
        return if (dir != null) {
            File(dir, OISAFE_CSV).absolutePath
        } else {
            File(context.filesDir, OISAFE_CSV).absolutePath
        }
    }

    @JvmStatic
    fun getBackupPath(context: Context): String {
        return SettingsRepository.getInstance(context).getBackupPath()
    }

    @JvmStatic
    fun setBackupPathAndMethod(context: Context, path: String) {
        SettingsRepository.getInstance(context).updateBackupPathAndMethodAsync(path)
    }

    @JvmStatic
    fun getBackupDocument(context: Context): String? {
        return SettingsRepository.getInstance(context).getBackupDocument()
    }

    @JvmStatic
    fun setBackupDocumentAndMethod(context: Context, uriString: String) {
        SettingsRepository.getInstance(context).updateBackupDocumentAndMethodAsync(uriString)
    }

    @JvmStatic
    fun getExportPath(context: Context): String {
        return SettingsRepository.getInstance(context).getExportPath()
    }

    @JvmStatic
    fun setExportPathAndMethod(context: Context, path: String) {
        SettingsRepository.getInstance(context).updateExportPathAndMethodAsync(path)
    }

    @JvmStatic
    fun getExportDocument(context: Context): String? {
        return SettingsRepository.getInstance(context).getExportDocument()
    }

    @JvmStatic
    fun setExportDocumentAndMethod(context: Context, uriString: String) {
        SettingsRepository.getInstance(context).updateExportDocumentAndMethodAsync(uriString)
    }

    @JvmStatic
    fun resetToDefaults(context: Context) {
        SettingsRepository.getInstance(context).resetToDefaultsAsync()
    }
}
