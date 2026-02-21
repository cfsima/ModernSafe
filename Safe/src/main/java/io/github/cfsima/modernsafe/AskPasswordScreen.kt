package io.github.cfsima.modernsafe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier
) {
    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(effects) {
        effects.collectLatest { effect ->
            if (effect is AskPasswordEffect.ShakeError) {
                // Shake Animation
                for (i in 0..3) {
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
                                contentDescription = stringResource(R.string.switch_mode)
                            )
                        }
                    }
                    if (uiState.isKeypadMode) {
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (uiState.isMuted) stringResource(R.string.sounds) else stringResource(R.string.mute)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val shakeModifier = Modifier.graphicsLayer { translationX = shakeAnim.value }

            if (uiState.isFirstTime) {
                FirstTimeContent(
                    onSetup = onFirstTimeSetup,
                    onRestore = onRestoreClicked,
                    modifier = shakeModifier
                )
            } else if (uiState.isKeypadMode) {
                KeypadContent(
                    input = uiState.keypadInput,
                    onDigit = onKeypadInput,
                    onDelete = onKeypadDelete,
                    onEnter = onKeypadEnter,
                    modifier = shakeModifier
                )
            } else {
                LoginContent(
                    onSubmit = onPasswordSubmit,
                    modifier = shakeModifier
                )
            }
        }
    }
}

@Composable
fun FirstTimeContent(
    onSetup: (String, String) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.widthIn(max = 400.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.first_time),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Master Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onSetup(password, confirm)
            }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { onSetup(password, confirm) },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotEmpty() && confirm.isNotEmpty()
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
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.widthIn(max = 400.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onSubmit(password)
            }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { onSubmit(password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotEmpty()
        ) {
            Text(stringResource(R.string.continue_text))
        }
    }
}

@Composable
fun KeypadContent(
    input: String,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.widthIn(max = 400.dp)
    ) {
        Text(
            text = if (input.isEmpty()) "Enter Password" else "•".repeat(input.length),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            textAlign = TextAlign.Center
        )

        val keys = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#')

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(keys) { key ->
                KeypadButton(
                    text = key.toString(),
                    onClick = { onDigit(key) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Delete")
            }

            Button(
                onClick = onEnter,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, contentDescription = "Enter")
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}
