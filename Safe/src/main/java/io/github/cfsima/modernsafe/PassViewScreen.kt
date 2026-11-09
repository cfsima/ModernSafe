package io.github.cfsima.modernsafe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cfsima.modernsafe.model.PassEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassViewScreen(
    passEntry: PassEntry?,
    packageAccess: List<String>,
    onBack: () -> Unit,
    onEdit: (PassEntry?) -> Unit,
    onDelete: (PassEntry?) -> Unit,
    onCopy: (String, String) -> Unit,
    onLaunchUrl: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    hasNext: Boolean,
    hasPrev: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name) + " - " + stringResource(id = R.string.view_entry)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(passEntry) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.password_edit))
                    }
                    IconButton(onClick = { onDelete(passEntry) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.password_delete))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (passEntry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Description
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.description),
                        modifier = Modifier.padding(end = 8.dp).width(100.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = passEntry.plainDescription ?: "",
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                }

                // Website
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.website),
                        modifier = Modifier.padding(end = 8.dp).width(100.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = passEntry.plainWebsite ?: "",
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onLaunchUrl(passEntry.plainWebsite ?: "") }) {
                        Text(text = stringResource(id = R.string.go))
                    }
                }

                // Username
                val usernameLabel = stringResource(id = R.string.username)
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = usernameLabel,
                        modifier = Modifier.padding(end = 8.dp).width(100.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = passEntry.plainUsername ?: "",
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                            .clickable { onCopy(usernameLabel, passEntry.plainUsername ?: "") },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                }

                // Password
                val passwordLabel = stringResource(id = R.string.password)
                val rawPassword = passEntry.plainPassword ?: ""
                val displayedPassword = if (passwordVisible) rawPassword else "•".repeat(if (rawPassword.isEmpty()) 0 else rawPassword.length)
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = passwordLabel,
                        modifier = Modifier.padding(end = 8.dp).width(100.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displayedPassword,
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                            .clickable { onCopy(passwordLabel, rawPassword) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }

                // Notes
                Text(
                    text = stringResource(id = R.string.notes),
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = passEntry.plainNote ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp
                )

                // Last Edited
                Spacer(modifier = Modifier.height(16.dp))
                val lastEditedLabel = stringResource(id = R.string.last_edited)
                Text(
                    text = "$lastEditedLabel: ${passEntry.lastEdited}",
                    fontSize = 14.sp
                )

                // Unique Name
                if (!passEntry.plainUniqueName.isNullOrEmpty()) {
                    val uniqueNameLabel = stringResource(id = R.string.uniquename)
                    Text(
                        text = "$uniqueNameLabel: ${passEntry.plainUniqueName}",
                        fontSize = 14.sp
                    )
                }

                // Package Access
                if (packageAccess.isNotEmpty()) {
                    val packageText = packageAccess.joinToString(" ")
                    val packageAccessLabel = stringResource(id = R.string.package_access)
                    Text(
                        text = "$packageAccessLabel: $packageText",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Navigation
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onPrev,
                        enabled = hasPrev
                    ) {
                        Text(text = stringResource(id = R.string.previous))
                    }

                    Button(
                        onClick = onNext,
                        enabled = hasNext
                    ) {
                        Text(text = stringResource(id = R.string.next))
                    }
                }
            }
        }
    }
}
