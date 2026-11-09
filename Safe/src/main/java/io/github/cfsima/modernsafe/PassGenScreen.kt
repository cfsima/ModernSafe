package io.github.cfsima.modernsafe

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    val strengthColor = when {
        state.strengthScore <= 0.25f -> MaterialTheme.colorScheme.error
        state.strengthScore <= 0.50f -> Color(0xFFF57C00) // Orange
        state.strengthScore <= 0.75f -> Color(0xFF388E3C) // Green
        else -> Color(0xFF1B5E20) // Deep Green
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generate_password)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
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
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                trailingIcon = {
                    IconButton(onClick = { viewModel.generatePassword() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.generate_password))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Password Strength Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Strength: ${state.strengthLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = strengthColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.strengthScore },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = strengthColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Length Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pass_gen_length),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${state.length}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = state.length.toFloat(),
                    onValueChange = { viewModel.updateLength(it.roundToInt()) },
                    valueRange = 8f..64f,
                    steps = 55,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val activeCount = viewModel.activeCategoryCount()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.includeUpper,
                            enabled = !state.includeUpper || activeCount > 1,
                            onCheckedChange = { viewModel.toggleUpper(it) }
                        )
                        Text(stringResource(R.string.pass_gen_uppercase))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.includeLower,
                            enabled = !state.includeLower || activeCount > 1,
                            onCheckedChange = { viewModel.toggleLower(it) }
                        )
                        Text(stringResource(R.string.pass_gen_lowercase))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.includeDigits,
                            enabled = !state.includeDigits || activeCount > 1,
                            onCheckedChange = { viewModel.toggleDigits(it) }
                        )
                        Text(stringResource(R.string.pass_gen_numbers))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.includeSymbols,
                            enabled = !state.includeSymbols || activeCount > 1,
                            onCheckedChange = { viewModel.toggleSymbols(it) }
                        )
                        Text(stringResource(R.string.pass_gen_symbols))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
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
