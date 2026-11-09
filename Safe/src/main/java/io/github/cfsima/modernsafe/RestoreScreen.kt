package io.github.cfsima.modernsafe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RestoreScreen(
    uiState: RestoreUiState,
    onPasswordEntered: (String) -> Unit,
    onConfirmRestore: () -> Unit,
    onSelectFile: () -> Unit,
    onCancel: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text(stringResource(R.string.error)) },
            text = { Text(uiState.error) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (uiState.status == RestoreStatus.Confirming) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.dialog_restore_database_title)) },
            text = { Text(stringResource(R.string.dialog_restore_database_msg)) },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (uiState.status) {
            RestoreStatus.Idle -> {
                Text(text = stringResource(R.string.restore_no_file))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSelectFile) {
                    Text(stringResource(R.string.restore_select_file))
                }
            }
            RestoreStatus.Parsing -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.parsing_backup))
            }
            RestoreStatus.PasswordRequired -> {
                var password by remember { mutableStateOf("") }

                Text(text = stringResource(R.string.restore_set_password))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.master_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(onClick = { onPasswordEntered(password) }) {
                        Text(stringResource(R.string.restore))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onCancel) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
            RestoreStatus.Decrypting -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.decrypt_progress))
            }
            RestoreStatus.Restoring -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.restoring_database))
            }
            RestoreStatus.Success -> {
                Text(stringResource(R.string.restore_complete_status))
            }
            RestoreStatus.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSelectFile) {
                    Text(stringResource(R.string.select_another_file))
                }
            }
            else -> {}
        }
    }
}

@Composable
fun RestoreFirstTimeScreen(
    headerText: String,
    filePath: String,
    fileExists: Boolean,
    onRestore: () -> Unit,
    onChooseFile: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = headerText, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = stringResource(R.string.filename))
        Text(text = filePath)

        if (!fileExists) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.restore_no_file), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRestore, enabled = fileExists) {
            Text(stringResource(R.string.restore))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onChooseFile) {
            Text(stringResource(R.string.restore_select_file))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onCancel) {
            Text(stringResource(android.R.string.cancel))
        }
    }
}
