package io.github.cfsima.modernsafe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.cfsima.modernsafe.ui.theme.OISafeTheme
import java.io.File

class Restore : AppCompatActivity() {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_FIRST_TIME = "first_time"
    }

    private val viewModel: RestoreViewModel by viewModels()

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.loadUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial load logic
        if (savedInstanceState == null) {
            val intentPath = intent.getStringExtra(KEY_FILE_PATH)
            val intentData = intent.data

            if (intentData != null) {
                viewModel.loadUri(intentData)
            } else if (intentPath != null) {
                viewModel.loadFile(intentPath)
            } else {
                // Try default path
                val defaultPath = Settings.getBackupPath(this)
                val file = File(defaultPath)
                if (file.exists()) {
                    viewModel.loadFile(defaultPath)
                }
                // Else remain Idle (user must pick file)
            }
        }

        setContent {
            OISafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val context = LocalContext.current

                    LaunchedEffect(uiState.status) {
                        if (uiState.status == RestoreStatus.Success) {
                            Toast.makeText(context, context.getString(R.string.restore_complete, "Unknown"), Toast.LENGTH_LONG).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }

                    RestoreScreen(
                        uiState = uiState,
                        onPasswordEntered = { viewModel.verifyPasswordAndDecrypt(it) },
                        onConfirmRestore = { viewModel.performRestore() },
                        onSelectFile = {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        },
                        onCancel = {
                             setResult(Activity.RESULT_CANCELED)
                             finish()
                        },
                        onErrorDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
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
