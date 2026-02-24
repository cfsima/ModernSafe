package io.github.cfsima.modernsafe

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import io.github.cfsima.modernsafe.intents.CryptoIntents
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import io.github.cfsima.modernsafe.util.VersionUtils
import java.io.File

class CategoryList : AppCompatActivity() {

    companion object {
        const val TAG = "CategoryList"
        const val KEY_ID = "id"

        // Constants used by other classes or legacy intents
        const val REQUEST_OPEN_CATEGORY = 1
        const val REQUEST_RESTORE = 2
        const val REQUEST_IMPORT_FILENAME = 3
        const val REQUEST_EXPORT_FILENAME = 4
        const val REQUEST_BACKUP_FILENAME = 5
        const val REQUEST_IMPORT_DOCUMENT = 6
        const val REQUEST_EXPORT_DOCUMENT = 7
        const val REQUEST_BACKUP_DOCUMENT = 8
        const val REQUEST_RESTORE_FILENAME = 9
        const val REQUEST_RESTORE_DOCUMENT = 10

        const val MIME_TYPE_BACKUP = "text/xml"
        const val MIME_TYPE_EXPORT = "text/csv"
        const val MIME_TYPE_ANY_TEXT = "text/*"

        const val MAX_CATEGORIES = 256
        const val PASSWORDSAFE_IMPORT_FILENAME = "/passwordsafe.csv"

        // Static compatibility methods/fields
        // isSignedIn() is now in AuthManager

        // This was a static flag in the old Activity. Ideally moved to ViewModel,
        // but for backward compat if any static access happens...
        // Actually, Import.java is being deleted, so we don't need this static field
        // to be accessible from there anymore. We can rely on ViewModel state.
        // However, if other classes access it? grep didn't show others.
        // So I'll remove the static importDeletedDatabase.
    }

    private val viewModel: CategoryListViewModel by viewModels()
    private var restartTimerIntent: Intent? = null

    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CryptoIntents.ACTION_CRYPTO_LOGGED_OUT) {
                startFrontDoor()
            }
        }
    }

    // --- Activity Result Launchers ---

    // BACKUP
    private val backupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // If it was a file picker (pre-KitKat or custom), it might return a file path.
                // But modern intent usually returns URI.
                // The legacy code used "getData().getPath()" for file picker.
                // Modern SAF returns a content URI.
                viewModel.backupDatabase(uri)
            }
        }
    }

    // RESTORE - Still uses legacy activity for now, so just need to reload if successful
    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadCategories()
        }
    }

    // EXPORT
    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.exportDatabase(uri)
            }
        }
    }

    // IMPORT
    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Prompt to delete existing?
                showImportDeleteConfirmDialog(uri)
            }
        }
    }

    private val openCategoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Reload in case passwords were added/moved/deleted
        viewModel.loadCategories()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        restartTimerIntent = Intent(CryptoIntents.ACTION_RESTART_TIMER)

        setContent {
            OISafeTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.importedFilePath) {
                    uiState.importedFilePath?.let { path ->
                         AlertDialog.Builder(this@CategoryList)
                            .setIcon(R.drawable.passicon)
                            .setTitle(R.string.import_delete_csv)
                            .setMessage(path)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                viewModel.deleteImportedFile(path)
                                viewModel.onImportedFileConsumed()
                            }
                            .setNegativeButton(R.string.no) { _, _ ->
                                viewModel.onImportedFileConsumed()
                            }
                            .show()
                    }
                }

                CategoryListScreen(
                    uiState = uiState,
                    onCategoryClick = { id ->
                        val intent = Intent(this, PassList::class.java)
                        intent.putExtra(KEY_ID, id)
                        openCategoryLauncher.launch(intent)
                    },
                    onAddCategory = { name -> viewModel.addCategory(name) },
                    onEditCategory = { id, name -> viewModel.updateCategory(id, name) },
                    onDeleteCategory = { id -> viewModel.deleteCategory(id) },
                    onSearch = { onSearchRequested() },
                    onBackup = { startBackup() },
                    onRestore = { startRestore() },
                    onExport = { startExport() },
                    onImport = { startImport() },
                    onChangePassword = { startActivity(Intent(this, ChangePass::class.java)) },
                    onPreferences = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onAbout = { showAboutDialog() },
                    onUserMessageDismiss = { viewModel.userMessageShown() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AuthManager.isSignedIn()) {
            startFrontDoor()
            return
        }

        Passwords.Initialize(this)
        viewModel.loadCategories()

        val filter = IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)
        ContextCompat.registerReceiver(this, logoutReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        maybRestartTimer()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(logoutReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered.", e)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (AuthManager.isSignedIn()) {
            maybRestartTimer()
        }
    }

    private fun maybRestartTimer() {
        restartTimerIntent?.let { sendBroadcast(it) }
    }

    private fun startFrontDoor() {
        val intent = Intent(this, AskPassword::class.java)
        intent.action = CryptoIntents.ACTION_AUTOLOCK
        intent.putExtra(AskPassword.EXTRA_IS_LOCAL, true)
        startActivity(intent)
        finish()
    }

    // --- Actions ---

    private fun startBackup() {
        maybRestartTimer()
        val filename = Settings.getBackupPath(this)

        val intent = Intents.createCreateDocumentIntent(MIME_TYPE_BACKUP, Settings.OISAFE_XML)
            try {
                backupFileLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                // Fallback or error
                viewModel.backupDatabase(filename)
            }
    }

    private fun startRestore() {
        val intent = Intent(this, Restore::class.java)
        restoreLauncher.launch(intent)
    }

    private fun startExport() {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.passicon)
            .setTitle(R.string.export_database)
            .setMessage(R.string.export_msg)
            .setPositiveButton(R.string.yes) { _, _ ->
                performExport()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun performExport() {
        maybRestartTimer()
        val filename = Settings.getExportPath(this)
        val intent = Intents.createCreateDocumentIntent(MIME_TYPE_EXPORT, Settings.OISAFE_CSV)
        try {
            exportFileLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback for when Storage Access Framework is not available
            viewModel.exportDatabase(filename)
        }
    }

    private fun startImport() {
        // Logic to determine default filename or use picker
        val filename = Settings.getExportPath(this) // Default suggestion

        val intent = Intents.createOpenDocumentIntents(MIME_TYPE_ANY_TEXT, Settings.getExportDocument(this) ?: "")
        try {
            importFileLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.restore_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun showImportDeleteConfirmDialog(uri: Uri) {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.passicon)
            .setTitle(R.string.dialog_import_title)
            .setMessage(getString(R.string.dialog_import_msg, uri.toString()))
            .setPositiveButton(R.string.yes) { _, _ ->
                showDeleteDatabaseConfirmDialog {
                     viewModel.importDatabase(uri, true)
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                viewModel.importDatabase(uri, false)
            }
            .show()
    }

    private fun showImportDeleteConfirmDialog(path: String) {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.passicon)
            .setTitle(R.string.dialog_import_title)
            .setMessage(getString(R.string.dialog_import_msg, path))
            .setPositiveButton(R.string.yes) { _, _ ->
                showDeleteDatabaseConfirmDialog {
                     viewModel.importDatabase(path, true)
                }
            }
            .setNegativeButton(R.string.no) { _, _ ->
                viewModel.importDatabase(path, false)
            }
            .show()
    }

    private fun showDeleteDatabaseConfirmDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.passicon)
            .setTitle(R.string.dialog_delete_database_title)
            .setMessage(R.string.dialog_delete_database_msg)
            .setPositiveButton(R.string.yes) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.version_label) + VersionUtils.getVersionNumber(this))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, Search::class.java))
        return true
    }
}
