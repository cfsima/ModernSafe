package io.github.cfsima.modernsafe.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.cfsima.modernsafe.AskPasswordEffect
import io.github.cfsima.modernsafe.AskPasswordScreen
import io.github.cfsima.modernsafe.AskPasswordViewModel
import io.github.cfsima.modernsafe.AuthManager
import io.github.cfsima.modernsafe.CategoryListScreen
import io.github.cfsima.modernsafe.CategoryListViewModel
import io.github.cfsima.modernsafe.ChangePassScreen
import io.github.cfsima.modernsafe.ChangePassViewModel
import io.github.cfsima.modernsafe.DeletePasswordDialog
import io.github.cfsima.modernsafe.HelpScreen
import io.github.cfsima.modernsafe.Intents
import io.github.cfsima.modernsafe.LogOffScreenContent
import io.github.cfsima.modernsafe.MovePasswordDialog
import io.github.cfsima.modernsafe.PassEditScreen
import io.github.cfsima.modernsafe.PassEditViewModel
import io.github.cfsima.modernsafe.PassGenScreen
import io.github.cfsima.modernsafe.PassListScreen
import io.github.cfsima.modernsafe.PassListViewModel
import io.github.cfsima.modernsafe.PassViewScreen
import io.github.cfsima.modernsafe.PassViewViewModel
import io.github.cfsima.modernsafe.R
import io.github.cfsima.modernsafe.RestoreFirstTimeScreen
import io.github.cfsima.modernsafe.RestoreScreen
import io.github.cfsima.modernsafe.RestoreStatus
import io.github.cfsima.modernsafe.RestoreViewModel
import io.github.cfsima.modernsafe.SafeApplication
import io.github.cfsima.modernsafe.SearchScreen
import io.github.cfsima.modernsafe.SearchViewModel
import io.github.cfsima.modernsafe.Settings
import io.github.cfsima.modernsafe.SettingsScreen
import io.github.cfsima.modernsafe.SettingsViewModel
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import io.github.cfsima.modernsafe.service.AutoLockService
import io.github.cfsima.modernsafe.util.VersionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.IOException

private const val TAG = "ModernSafeDestinations"

fun NavGraphBuilder.askPasswordDestination(
    navController: NavHostController,
    onFinishActivity: () -> Unit,
    onSetResultOkAndFinish: () -> Unit
) {
    composable<AskPasswordRoute> { backStackEntry ->
        val context = LocalContext.current
        val route = backStackEntry.toRoute<AskPasswordRoute>()
        val viewModel: AskPasswordViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        var mpDigitBeep by remember { mutableStateOf<MediaPlayer?>(null) }
        var mpErrorBeep by remember { mutableStateOf<MediaPlayer?>(null) }
        var mpSuccessBeep by remember { mutableStateOf<MediaPlayer?>(null) }

        DisposableEffect(Unit) {
            try {
                mpDigitBeep = MediaPlayer.create(context, R.raw.dtmf2a)
                mpErrorBeep = MediaPlayer.create(context, R.raw.click6a)
                mpSuccessBeep = MediaPlayer.create(context, R.raw.dooropening1)
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer create failed", e)
            }
            onDispose {
                mpDigitBeep?.release()
                mpErrorBeep?.release()
                mpSuccessBeep?.release()
            }
        }

        fun tryPlay(mp: MediaPlayer?) {
            try {
                if (mp?.isPlaying == true) {
                    mp.seekTo(0)
                }
                mp?.start()
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer play failed", e)
            }
        }

        LaunchedEffect(Unit) {
            if (!route.isLocal) {
                Toast.makeText(context, R.string.external_request_password, Toast.LENGTH_LONG).show()
            }
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is AskPasswordEffect.UnlockSuccess -> {
                        viewModel.onUnlockSuccess()
                        context.startService(Intent(context.applicationContext, AutoLockService::class.java))
                        if (route.isLocal) {
                            navController.navigate(CategoryListRoute) {
                                popUpTo(AskPasswordRoute(isLocal = route.isLocal)) { inclusive = true }
                            }
                        } else {
                            onSetResultOkAndFinish()
                        }
                    }
                    is AskPasswordEffect.PlayDigitSound -> tryPlay(mpDigitBeep)
                    is AskPasswordEffect.PlayErrorSound -> tryPlay(mpErrorBeep)
                    is AskPasswordEffect.PlaySuccessSound -> tryPlay(mpSuccessBeep)
                    is AskPasswordEffect.ShakeError -> { /* Handled in Compose */ }
                    is AskPasswordEffect.ShowToast -> {
                        Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                    }
                    is AskPasswordEffect.NavigateToRestoreFirstTime -> {
                        navController.navigate(RestoreFirstTimeRoute)
                    }
                    is AskPasswordEffect.CloseApp -> onFinishActivity()
                }
            }
        }

        if (uiState.showDatabaseError) {
            AlertDialog(
                onDismissRequest = { onFinishActivity() },
                title = { Text(stringResource(R.string.database_error_title)) },
                text = { Text(stringResource(R.string.database_error_msg)) },
                confirmButton = {
                    TextButton(onClick = { onFinishActivity() }) { Text(stringResource(android.R.string.ok)) }
                }
            )
        }

        if (uiState.showVersionError) {
            AlertDialog(
                onDismissRequest = { onFinishActivity() },
                title = { Text(stringResource(R.string.database_version_error_title)) },
                text = { Text(stringResource(R.string.database_version_error_msg)) },
                confirmButton = {
                    TextButton(onClick = { onFinishActivity() }) { Text(stringResource(android.R.string.ok)) }
                }
            )
        }

        AskPasswordScreen(
            uiState = uiState,
            effects = viewModel.effects,
            onPasswordSubmit = { viewModel.onPasswordSubmit(it) },
            onFirstTimeSetup = { pass, confirm -> viewModel.onFirstTimeSetup(pass, confirm) },
            onToggleKeypad = { viewModel.toggleKeypadMode() },
            onToggleMute = { viewModel.toggleMute() },
            onKeypadInput = { viewModel.onKeypadInput(it) },
            onKeypadDelete = { viewModel.onKeypadDelete() },
            onKeypadEnter = { viewModel.onKeypadEnter() },
            onRestoreClicked = { viewModel.onRestoreClicked() },
            onBiometricUnlock = { activity -> viewModel.onBiometricUnlock(activity) }
        )
    }
}

fun NavGraphBuilder.categoryListDestination(navController: NavHostController) {
    composable<CategoryListRoute> {
        val context = LocalContext.current
        val viewModel: CategoryListViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            Passwords.Initialize(context)
            viewModel.loadCategories()
        }

        var showAboutDialog by remember { mutableStateOf(false) }
        var showExportConfirmDialog by remember { mutableStateOf(false) }
        var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
        var showImportDeleteConfirmDialog by remember { mutableStateOf(false) }

        val backupFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> viewModel.backupDatabase(uri) }
            }
        }

        val exportFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> viewModel.exportDatabase(uri) }
            }
        }

        val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    pendingImportUri = uri
                    showImportDeleteConfirmDialog = true
                }
            }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text(stringResource(R.string.app_name)) },
                text = { Text(stringResource(R.string.version_label) + VersionUtils.getVersionNumber(context)) },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) { Text(stringResource(android.R.string.ok)) }
                }
            )
        }

        if (showExportConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExportConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text(stringResource(R.string.export_database)) },
                text = { Text(stringResource(R.string.export_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        showExportConfirmDialog = false
                        val filename = Settings.getExportPath(context)
                        val intent = Intents.createCreateDocumentIntent(Intents.MIME_TYPE_EXPORT, Settings.OISAFE_CSV)
                        try {
                            exportFileLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            viewModel.exportDatabase(filename)
                        }
                    }) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExportConfirmDialog = false }) { Text(stringResource(R.string.no)) }
                }
            )
        }

        if (showImportDeleteConfirmDialog && pendingImportUri != null) {
            val uri = pendingImportUri!!
            AlertDialog(
                onDismissRequest = {
                    showImportDeleteConfirmDialog = false
                    pendingImportUri = null
                },
                title = { Text(stringResource(R.string.dialog_import_title)) },
                text = { Text(stringResource(R.string.dialog_import_msg, uri.toString())) },
                confirmButton = {
                    TextButton(onClick = {
                        showImportDeleteConfirmDialog = false
                        viewModel.importDatabase(uri, true)
                        pendingImportUri = null
                    }) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImportDeleteConfirmDialog = false
                        viewModel.importDatabase(uri, false)
                        pendingImportUri = null
                    }) { Text(stringResource(R.string.no)) }
                }
            )
        }

        if (uiState.importedFilePath != null) {
            val path = uiState.importedFilePath!!
            AlertDialog(
                onDismissRequest = { viewModel.onImportedFileConsumed() },
                title = { Text(stringResource(R.string.import_delete_csv)) },
                text = { Text(path) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteImportedFile(path)
                        viewModel.onImportedFileConsumed()
                    }) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onImportedFileConsumed() }) { Text(stringResource(R.string.no)) }
                }
            )
        }

        CategoryListScreen(
            uiState = uiState,
            onCategoryClick = { id -> navController.navigate(PassListRoute(categoryId = id)) },
            onAddCategory = { name -> viewModel.addCategory(name) },
            onEditCategory = { id, name -> viewModel.updateCategory(id, name) },
            onDeleteCategory = { id -> viewModel.deleteCategory(id) },
            onSearch = { navController.navigate(SearchRoute) },
            onBackup = {
                val intent = Intents.createCreateDocumentIntent(Intents.MIME_TYPE_BACKUP, Settings.OISAFE_XML)
                try {
                    backupFileLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    viewModel.backupDatabase(Settings.getBackupPath(context))
                }
            },
            onRestore = { navController.navigate(RestoreRoute()) },
            onExport = { showExportConfirmDialog = true },
            onImport = {
                val intent = Intents.createOpenDocumentIntents(Intents.MIME_TYPE_ANY_TEXT, Settings.getExportDocument(context) ?: "")
                try {
                    importFileLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show()
                }
            },
            onChangePassword = { navController.navigate(ChangePassRoute) },
            onPreferences = { navController.navigate(SettingsRoute) },
            onAbout = { showAboutDialog = true },
            onUserMessageDismiss = { viewModel.userMessageShown() }
        )
    }
}

fun NavGraphBuilder.passListDestination(navController: NavHostController) {
    composable<PassListRoute> { backStackEntry ->
        val context = LocalContext.current
        val route = backStackEntry.toRoute<PassListRoute>()
        val viewModel: PassListViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(route.categoryId) {
            viewModel.load(route.categoryId)
        }

        val reload = backStackEntry.savedStateHandle.getStateFlow("reload", false).collectAsState()
        LaunchedEffect(reload.value) {
            if (reload.value) {
                viewModel.reload()
                backStackEntry.savedStateHandle["reload"] = false
            }
        }

        var showDeleteDialog by remember { mutableStateOf<PassEntry?>(null) }
        var showMoveDialog by remember { mutableStateOf<PassEntry?>(null) }

        if (showDeleteDialog != null) {
            val entry = showDeleteDialog!!
            DeletePasswordDialog(
                onConfirm = {
                    viewModel.deleteEntry(entry)
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }

        if (showMoveDialog != null) {
            val entry = showMoveDialog!!
            val categories = remember(uiState.categoryName) { viewModel.getAvailableCategories() }
            MovePasswordDialog(
                categories = categories,
                onCategorySelected = { newCatId ->
                    val categoryName = categories.entries.find { it.value == newCatId }?.key
                    viewModel.moveEntry(entry, newCatId)
                    if (categoryName != null) {
                        Toast.makeText(context, context.applicationContext.getString(R.string.moved_to, categoryName), Toast.LENGTH_LONG).show()
                    }
                    showMoveDialog = null
                },
                onDismiss = { showMoveDialog = null }
            )
        }

        PassListScreen(
            uiState = uiState,
            onAddPassword = {
                navController.navigate(PassEditRoute(id = -1L, categoryId = route.categoryId))
            },
            onViewPassword = { entry ->
                val rowIds = uiState.entries.map { it.id }
                val position = uiState.entries.indexOfFirst { it.id == entry.id }
                navController.navigate(
                    PassViewRoute(
                        id = entry.id,
                        categoryId = route.categoryId,
                        rowIds = rowIds,
                        position = position
                    )
                )
            },
            onEditPassword = { entry ->
                navController.navigate(PassEditRoute(id = entry.id, categoryId = route.categoryId))
            },
            onDeletePassword = { entry -> showDeleteDialog = entry },
            onMovePassword = { entry -> showMoveDialog = entry },
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.passViewDestination(navController: NavHostController) {
    composable<PassViewRoute> { backStackEntry ->
        val context = LocalContext.current
        val route = backStackEntry.toRoute<PassViewRoute>()
        val viewModel: PassViewViewModel = viewModel()

        LaunchedEffect(route.id, route.position) {
            viewModel.initialize(
                rowId = route.id,
                categoryId = route.categoryId,
                rowIds = route.rowIds.toLongArray(),
                listPosition = route.position
            )
        }

        val entryEdited = backStackEntry.savedStateHandle.getStateFlow("entry_edited", false).collectAsState()
        LaunchedEffect(entryEdited.value) {
            if (entryEdited.value) {
                viewModel.reloadCurrent()
                navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                backStackEntry.savedStateHandle["entry_edited"] = false
            }
        }

        val entryDeleted = backStackEntry.savedStateHandle.getStateFlow("entry_deleted", false).collectAsState()
        LaunchedEffect(entryDeleted.value) {
            if (entryDeleted.value) {
                navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                backStackEntry.savedStateHandle["entry_deleted"] = false
                navController.popBackStack()
            }
        }

        val passEntry by viewModel.currentPassEntry.collectAsState()
        val packageAccess by viewModel.packageAccess.collectAsState()
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_password_title)) },
                text = { Text(stringResource(R.string.dialog_delete_password_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCurrentEntry()
                        navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    }) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) { Text(stringResource(R.string.no)) }
                }
            )
        }

        PassViewScreen(
            passEntry = passEntry,
            packageAccess = packageAccess,
            onBack = { navController.popBackStack() },
            onEdit = { entry ->
                entry?.let {
                    navController.navigate(PassEditRoute(id = it.id, categoryId = it.category))
                }
            },
            onDelete = { showDeleteConfirmDialog = true },
            onCopy = { label, text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText(label, text).apply {
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
                clipboard.setPrimaryClip(clipData)
                viewModel.updateLastUsedPassword(text)
                Toast.makeText(context, "$label ${context.getString(R.string.copied_to_clipboard)}", Toast.LENGTH_SHORT).show()
            },
            onLaunchUrl = { url ->
                var finalUrl = url
                if (finalUrl.isEmpty() || finalUrl == "http://") {
                    Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        try {
                            finalUrl = "https://$finalUrl"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                            context.startActivity(intent)
                        } catch (e2: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.invalid_website, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onNext = { viewModel.loadNext() },
            onPrev = { viewModel.loadPrevious() },
            hasNext = viewModel.hasNext(),
            hasPrev = viewModel.hasPrevious()
        )
    }
}

fun NavGraphBuilder.passEditDestination(navController: NavHostController) {
    composable<PassEditRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PassEditRoute>()
        val viewModel: PassEditViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(route.id, route.categoryId) {
            viewModel.load(route.id, route.categoryId)
        }

        val newPass = backStackEntry.savedStateHandle.getStateFlow<String?>("new_pass", null).collectAsState()
        LaunchedEffect(newPass.value) {
            newPass.value?.let { pass ->
                viewModel.onPasswordChange(pass)
                backStackEntry.savedStateHandle["new_pass"] = null
            }
        }

        PassEditScreen(
            uiState = uiState,
            onDescriptionChange = viewModel::onDescriptionChange,
            onWebsiteChange = viewModel::onWebsiteChange,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onNoteChange = viewModel::onNoteChange,
            onSave = {
                viewModel.save()
                navController.previousBackStackEntry?.savedStateHandle?.set("entry_edited", true)
            },
            onDelete = viewModel::delete,
            onGeneratePassword = { navController.navigate(PassGenRoute) },
            onBack = { navController.popBackStack() },
            onConsumeSave = viewModel::consumeSaveEvent,
            onConsumeDelete = viewModel::consumeDeleteEvent
        )
    }
}

fun NavGraphBuilder.passGenDestination(navController: NavHostController) {
    composable<PassGenRoute> {
        PassGenScreen(
            onUsePassword = { newPass ->
                navController.previousBackStackEntry?.savedStateHandle?.set("new_pass", newPass)
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.searchDestination(navController: NavHostController) {
    composable<SearchRoute> {
        val viewModel: SearchViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        SearchScreen(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            onResultClick = { _, result ->
                navController.navigate(
                    PassViewRoute(
                        id = result.id,
                        categoryId = result.categoryId,
                        rowIds = listOf(result.id),
                        position = 0
                    )
                )
            },
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.changePassDestination(navController: NavHostController) {
    composable<ChangePassRoute> {
        val viewModel: ChangePassViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState.isSuccess) {
            if (uiState.isSuccess) {
                navController.popBackStack()
            }
        }

        ChangePassScreen(
            uiState = uiState,
            onChangePassword = viewModel::changePassword,
            onBack = { navController.popBackStack() },
            onClearErrors = viewModel::clearErrors
        )
    }
}

fun NavGraphBuilder.settingsDestination(navController: NavHostController) {
    composable<SettingsRoute> {
        val context = LocalContext.current
        val viewModel: SettingsViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        val backupPathLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.setBackupPath(uri.toString())
                }
            }
        }

        val exportPathLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.setExportPath(uri.toString())
                }
            }
        }

        SettingsScreen(
            externalAccess = uiState.externalAccess,
            lockTimeout = uiState.lockTimeout,
            lockOnScreenLock = uiState.lockOnScreenLock,
            keypad = uiState.keypad,
            keypadMute = uiState.keypadMute,
            autoBackup = uiState.autoBackup,
            autoBackupDays = uiState.autoBackupDays,
            backupPath = uiState.backupPath,
            exportPath = uiState.exportPath,
            currentLanguage = uiState.currentLanguage,
            biometricEnabled = uiState.biometricEnabled,
            isBiometricAvailable = viewModel.isBiometricAvailable,
            onExternalAccessChange = viewModel::setExternalAccess,
            onLockTimeoutChange = viewModel::setLockTimeout,
            onLockOnScreenLockChange = viewModel::setLockOnScreenLock,
            onKeypadChange = viewModel::setKeypad,
            onKeypadMuteChange = viewModel::setKeypadMute,
            onAutoBackupChange = viewModel::setAutoBackup,
            onAutoBackupDaysChange = viewModel::setAutoBackupDays,
            onLanguageChange = viewModel::setLanguage,
            onBiometricChange = { enabled ->
                (context as? Activity)?.let { activity ->
                    viewModel.onBiometricChange(activity, enabled)
                }
            },
            onBackupPathClick = {
                val intent = Intents.createCreateDocumentIntent(Intents.MIME_TYPE_BACKUP, Settings.OISAFE_XML)
                try {
                    backupPathLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show()
                }
            },
            onExportPathClick = {
                val intent = Intents.createCreateDocumentIntent(Intents.MIME_TYPE_EXPORT, Settings.OISAFE_CSV)
                try {
                    exportPathLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.restore_error, Toast.LENGTH_LONG).show()
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.restoreDestination(navController: NavHostController) {
    composable<RestoreRoute> { backStackEntry ->
        val context = LocalContext.current
        val route = backStackEntry.toRoute<RestoreRoute>()
        val viewModel: RestoreViewModel = viewModel()

        val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { viewModel.loadUri(it) }
        }

        LaunchedEffect(route.filePath) {
            if (route.filePath != null) {
                val uri = Uri.parse(route.filePath)
                if (uri.scheme == "content" || uri.scheme == "file") {
                    viewModel.loadUri(uri)
                } else {
                    viewModel.loadFile(route.filePath)
                }
            } else {
                val defaultPath = Settings.getBackupPath(context)
                val file = File(defaultPath)
                if (file.exists()) {
                    viewModel.loadFile(defaultPath)
                }
            }
        }

        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState.status) {
            if (uiState.status == RestoreStatus.Success) {
                Toast.makeText(context, context.getString(R.string.restore_complete, "Unknown"), Toast.LENGTH_LONG).show()
                if (route.isFirstTime) {
                    navController.navigate(CategoryListRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    navController.previousBackStackEntry?.savedStateHandle?.set("reload", true)
                    navController.popBackStack()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RestoreScreen(
                uiState = uiState,
                onPasswordEntered = { viewModel.verifyPasswordAndDecrypt(it) },
                onConfirmRestore = { viewModel.performRestore() },
                onSelectFile = { openDocumentLauncher.launch(arrayOf("*/*")) },
                onCancel = { navController.popBackStack() },
                onErrorDismiss = { viewModel.clearError() }
            )
        }
    }

    composable<RestoreFirstTimeRoute> {
        val context = LocalContext.current
        val defaultPath = Settings.getBackupPath(context)
        val fileExists = File(defaultPath).exists()
        val appName = VersionUtils.getApplicationName(context)
        val version = VersionUtils.getVersionNumber(context)
        val headerText = "$appName $version"

        val chooseFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                navController.navigate(RestoreRoute(filePath = it.toString(), isFirstTime = true))
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            RestoreFirstTimeScreen(
                headerText = headerText,
                filePath = defaultPath,
                fileExists = fileExists,
                onRestore = {
                    navController.navigate(RestoreRoute(filePath = defaultPath, isFirstTime = true))
                },
                onChooseFile = { chooseFileLauncher.launch(arrayOf("*/*")) },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.helpDestination(navController: NavHostController) {
    composable<HelpRoute> {
        val context = LocalContext.current
        val title = "${stringResource(R.string.app_name)} - ${stringResource(R.string.help)}"
        var helpText by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            try {
                context.assets.open("help.html").use { stream ->
                    helpText = stream.bufferedReader().use { it.readText() }
                }
            } catch (e: IOException) {
                helpText = "Error loading help: ${e.message}"
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                HelpScreen(helpText)
            }
        }
    }
}

fun NavGraphBuilder.logOffDestination(navController: NavHostController) {
    composable<LogOffRoute> {
        val context = LocalContext.current
        val headerText = "${VersionUtils.getApplicationName(context)} ${VersionUtils.getVersionNumber(context)}"
        val settings = (context.applicationContext as SafeApplication).settingsRepository.userSettings.collectAsState().value
        val timeoutMinutes = settings.lockTimeout.toIntOrNull() ?: 5
        var timeRemaining by remember { mutableLongStateOf(AuthManager.getTimeRemainingMillis(timeoutMinutes)) }

        LaunchedEffect(timeoutMinutes) {
            while (true) {
                val remaining = AuthManager.getTimeRemainingMillis(timeoutMinutes)
                timeRemaining = remaining
                if (remaining <= 0L && AuthManager.isSignedIn()) {
                    AuthManager.setSignedOut()
                    navController.navigate(AskPasswordRoute()) {
                        popUpTo(0) { inclusive = true }
                    }
                    break
                }
                delay(1000)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LogOffScreenContent(
                headerText = headerText,
                timeRemaining = timeRemaining,
                onLogOff = {
                    context.stopService(Intent(context.applicationContext, AutoLockService::class.java))
                    try {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        if (cb.hasPrimaryClip()) {
                            val clip = cb.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString()
                                if (text == AuthManager.lastUsedPassword) {
                                    cb.clearPrimaryClip()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Clipboard access restricted or unavailable
                    }
                    AuthManager.setSignedOut()
                    navController.navigate(AskPasswordRoute()) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGoToPWS = {
                    if (AuthManager.isSignedIn()) {
                        navController.navigate(CategoryListRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(AskPasswordRoute()) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
