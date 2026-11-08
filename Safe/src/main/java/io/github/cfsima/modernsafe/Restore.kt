package io.github.cfsima.modernsafe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

/**
 * Legacy activity replaced by RestoreRoute in ModernSafeNavHost.
 */
@Deprecated("Migrated to Compose Navigation within MainActivity. ModernSafeNavHost now manages this destination.")
class Restore : AppCompatActivity() {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FIRST_TIME = "first_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forwardIntent = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            data = intent.data
            intent.extras?.let { putExtras(it) }
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        }
        startActivity(forwardIntent)
        finish()
    }
}

@Composable
fun RestoreScreen(
    uiState: RestoreUiState,
    onPasswordEntered: (String) -> Unit,
    onConfirmRestore: () -> Unit,
    onSelectFile: () -> Unit,
    onCancel: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text("Error") },
            text = { Text(uiState.error) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) {
                    Text("OK")
                }
            }
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
            }
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
        verticalArrangement = Arrangement.Center
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
                Text("Parsing backup file...")
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
                    singleLine = true
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
                Text("Decrypting...")
            }
            RestoreStatus.Restoring -> {
                CircularProgressIndicator()
                Text("Restoring database...")
            }
            RestoreStatus.Success -> {
                Text("Restore Complete!")
            }
            RestoreStatus.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSelectFile) {
                    Text("Select Another File")
                }
            }
            else -> {}
        }
    }
}
