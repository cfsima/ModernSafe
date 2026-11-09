package io.github.cfsima.modernsafe

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassGenScreen(
    onUsePassword: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: PassGenViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val clipboardMessage = stringResource(R.string.copied_to_clipboard)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generate_password)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Generated Password Display
            OutlinedTextField(
                value = state.generatedPassword,
                onValueChange = { /* Read only */ },
                readOnly = true,
                label = { Text(stringResource(R.string.password)) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.generatePassword() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.generate_password))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Length
            OutlinedTextField(
                value = state.length,
                onValueChange = { viewModel.updateLength(it) },
                label = { Text(stringResource(R.string.pass_gen_length)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Options
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.includeUpper,
                        onCheckedChange = { viewModel.toggleUpper(it) }
                    )
                    Text(stringResource(R.string.pass_gen_uppercase))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.includeLower,
                        onCheckedChange = { viewModel.toggleLower(it) }
                    )
                    Text(stringResource(R.string.pass_gen_lowercase))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.includeDigits,
                        onCheckedChange = { viewModel.toggleDigits(it) }
                    )
                    Text(stringResource(R.string.pass_gen_numbers))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.includeSymbols,
                        onCheckedChange = { viewModel.toggleSymbols(it) }
                    )
                    Text(stringResource(R.string.pass_gen_symbols))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("password", state.generatedPassword).apply {
                            description.extras = PersistableBundle().apply {
                                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                            }
                        }
                        clipboard.setPrimaryClip(clipData)
                        scope.launch {
                            snackbarHostState.showSnackbar(clipboardMessage)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pass_gen_copy_to_clipboard))
                }
                Button(
                    onClick = { onUsePassword(state.generatedPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pass_gen_copy_to_current_entry))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
