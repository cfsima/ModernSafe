package io.github.cfsima.modernsafe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    externalAccess: Boolean,
    lockTimeout: String,
    lockOnScreenLock: Boolean,
    keypad: Boolean,
    keypadMute: Boolean,
    autoBackup: Boolean,
    autoBackupDays: String,
    backupPath: String,
    exportPath: String,
    onExternalAccessChange: (Boolean) -> Unit,
    onLockTimeoutChange: (String) -> Unit,
    onLockOnScreenLockChange: (Boolean) -> Unit,
    onKeypadChange: (Boolean) -> Unit,
    onKeypadMuteChange: (Boolean) -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onAutoBackupDaysChange: (String) -> Unit,
    onBackupPathClick: () -> Unit,
    onExportPathClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SwitchPreference(
                    title = stringResource(R.string.pref_title_external_access),
                    summary = stringResource(R.string.pref_summary_external_access),
                    checked = externalAccess,
                    onCheckedChange = onExternalAccessChange
                )
                HorizontalDivider()
            }

            item {
                ListPreference(
                    title = stringResource(R.string.pref_title_lock_timeout),
                    summary = stringResource(R.string.pref_summary_lock_timeout),
                    dialogTitle = stringResource(R.string.pref_dialog_title_lock_timeout),
                    entries = stringArrayResource(R.array.pref_entries_lock_timeout),
                    entryValues = stringArrayResource(R.array.pref_entryvalues_lock_timeout),
                    currentValue = lockTimeout,
                    onValueChange = onLockTimeoutChange
                )
                HorizontalDivider()
            }

            item {
                SwitchPreference(
                    title = stringResource(R.string.pref_title_lock_on_screen_lock),
                    summary = stringResource(R.string.pref_summary_lock_on_screen_lock),
                    checked = lockOnScreenLock,
                    onCheckedChange = onLockOnScreenLockChange
                )
                HorizontalDivider()
            }

            item {
                SwitchPreference(
                    title = stringResource(R.string.pref_title_keypad),
                    summary = stringResource(R.string.pref_summary_keypad),
                    checked = keypad,
                    onCheckedChange = onKeypadChange
                )
                HorizontalDivider()
            }

            item {
                SwitchPreference(
                    title = stringResource(R.string.pref_title_keypad_mute),
                    summary = stringResource(R.string.pref_summary_keypad_mute),
                    checked = keypadMute,
                    onCheckedChange = onKeypadMuteChange,
                    enabled = keypad
                )
                HorizontalDivider()
            }

            item {
                SwitchPreference(
                    title = stringResource(R.string.pref_title_autobackup),
                    summary = stringResource(R.string.pref_summary_autobackup),
                    checked = autoBackup,
                    onCheckedChange = onAutoBackupChange
                )
                HorizontalDivider()
            }

            item {
                ListPreference(
                    title = stringResource(R.string.pref_title_autobackup_days),
                    summary = stringResource(R.string.pref_summary_autobackup_days),
                    dialogTitle = stringResource(R.string.pref_dialog_title_autobackup_days),
                    entries = stringArrayResource(R.array.pref_entries_autobackup_days),
                    entryValues = stringArrayResource(R.array.pref_entryvalues_autobackup_days),
                    currentValue = autoBackupDays,
                    onValueChange = onAutoBackupDaysChange,
                    enabled = autoBackup
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pref_title_backup_path)) },
                    supportingContent = { Text(backupPath) },
                    modifier = Modifier.clickable(onClick = onBackupPathClick)
                )
                HorizontalDivider()
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pref_title_export_path)) },
                    supportingContent = { Text(exportPath) },
                    modifier = Modifier.clickable(onClick = onExportPathClick)
                )
            }
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title, style = if (enabled) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))) },
        supportingContent = { Text(summary, style = if (enabled) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

@Composable
fun ListPreference(
    title: String,
    summary: String,
    dialogTitle: String,
    entries: Array<String>,
    entryValues: Array<String>,
    currentValue: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title, style = if (enabled) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))) },
        supportingContent = { Text(summary, style = if (enabled) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))) },
        modifier = Modifier.clickable(enabled = enabled) { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = {
                Column(Modifier.selectableGroup()) {
                    entries.forEachIndexed { index, entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (entryValues[index] == currentValue),
                                    onClick = {
                                        onValueChange(entryValues[index])
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (entryValues[index] == currentValue),
                                onClick = null // null recommended for accessibility with selectable
                            )
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
