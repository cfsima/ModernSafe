package io.github.cfsima.modernsafe

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskPasswordScreen(
    uiState: AskPasswordUiState,
    effects: Flow<AskPasswordEffect>,
    onPasswordSubmit: (String) -> Unit,
    onFirstTimeSetup: (String, String) -> Unit,
    onToggleKeypad: () -> Unit,
    onToggleMute: () -> Unit,
    onKeypadInput: (Char) -> Unit,
    onKeypadDelete: () -> Unit,
    onKeypadEnter: () -> Unit,
    onRestoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometricUnlock: ((Activity) -> Unit)? = null,
) {
    val shakeAnim = remember { Animatable(0f) }
    val context = LocalContext.current
    val activity = context as? Activity
    var hasAutoPromptedBiometric by remember { mutableStateOf(value = false) }

    LaunchedEffect(uiState.isBiometricAvailable, activity) {
        if (uiState.isBiometricAvailable && (activity != null) && !hasAutoPromptedBiometric && !uiState.isFirstTime) {
            hasAutoPromptedBiometric = true
            onBiometricUnlock?.invoke(activity)
        }
    }

    LaunchedEffect(effects) {
        effects.collectLatest { effect ->
            if (effect is AskPasswordEffect.ShakeError) {
                // Shake Animation
                repeat(4) {
                    shakeAnim.animateTo(10f, animationSpec = tween(50))
                    shakeAnim.animateTo(-10f, animationSpec = tween(50))
                }
                shakeAnim.animateTo(0f, animationSpec = tween(50))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (!uiState.isFirstTime) {
                        IconButton(onClick = onToggleKeypad) {
                            Icon(
                                imageVector = if (uiState.isKeypadMode) Icons.Default.Close else Icons.Default.Apps,
                                contentDescription = stringResource(R.string.switch_mode),
                            )
                        }
                    }
                    if (uiState.isKeypadMode) {
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (uiState.isMuted) stringResource(R.string.sounds) else stringResource(R.string.mute),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val shakeModifier = Modifier.graphicsLayer { translationX = shakeAnim.value }

            if (uiState.isFirstTime) {
                FirstTimeContent(
                    onSetup = onFirstTimeSetup,
                    onRestore = onRestoreClicked,
                    modifier = shakeModifier,
                )
            } else if (uiState.isKeypadMode) {
                KeypadContent(
                    input = uiState.keypadInput,
                    onDigit = onKeypadInput,
                    onDelete = onKeypadDelete,
                    onEnter = onKeypadEnter,
                    modifier = shakeModifier,
                    onBiometricClick = if (uiState.isBiometricAvailable && (activity != null) && (onBiometricUnlock != null)) {
                        { onBiometricUnlock(activity) }
                    } else null,
                )
            } else {
                LoginContent(
                    onSubmit = onPasswordSubmit,
                    modifier = shakeModifier,
                    onBiometricClick = if (uiState.isBiometricAvailable && (activity != null) && (onBiometricUnlock != null)) {
                        { onBiometricUnlock(activity) }
                    } else null,
                )
            }
        }
    }
}

@Composable
fun FirstTimeContent(
    onSetup: (String, String) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .widthIn(max = 400.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.first_time),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.master_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSetup(password, confirm)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = { onSetup(password, confirm) },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotEmpty() && confirm.isNotEmpty(),
        ) {
            Text(stringResource(R.string.continue_text))
        }

        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.restore))
        }
    }
}

@Composable
fun LoginContent(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBiometricClick: (() -> Unit)? = null,
) {
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .widthIn(max = 400.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.welcome_back),
            style = MaterialTheme.typography.headlineMedium,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit(password)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = { onSubmit(password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotEmpty(),
        ) {
            Text(stringResource(R.string.continue_text))
        }

        onBiometricClick?.let { onClick ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.biometric_unlock),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.biometric_unlock))
            }
        }
    }
}

@Composable
fun KeypadContent(
    input: String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometricClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .widthIn(max = 400.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (input.isEmpty()) "Enter Password" else "•".repeat(input.length),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            textAlign = TextAlign.Center,
        )

        val keys = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf('*', '0', '#'),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowKeys.forEach { key ->
                        KeypadButton(
                            text = key.toString(),
                            onClick = { onDigit(key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.password_delete))
            }

            Button(
                onClick = onEnter,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm))
            }
        }

        onBiometricClick?.let { onClick ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.biometric_unlock),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.biometric_unlock))
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.5f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}
