package io.github.cfsima.modernsafe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.cfsima.modernsafe.model.PassEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassListScreen(
    uiState: PassListUiState,
    onAddPassword: () -> Unit,
    onViewPassword: (PassEntry) -> Unit,
    onEditPassword: (PassEntry) -> Unit,
    onDeletePassword: (PassEntry) -> Unit,
    onMovePassword: (PassEntry) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPassword) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.password_add))
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_safe),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddPassword) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.password_add))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(paddingValues),
                contentPadding = paddingValues
            ) {
                items(uiState.entries) { entry ->
                    PassListItem(
                        entry = entry,
                        onView = { onViewPassword(entry) },
                        onEdit = { onEditPassword(entry) },
                        onDelete = { onDeletePassword(entry) },
                        onMove = { onMovePassword(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun PassListItem(
    entry: PassEntry,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(entry.plainDescription) },
        modifier = Modifier.clickable { onView() },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.password_view)) },
                        onClick = {
                            expanded = false
                            onView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.password_edit)) },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.password_delete)) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move)) },
                        onClick = {
                            expanded = false
                            onMove()
                        }
                    )
                }
            }
        }
    )
    HorizontalDivider()
}

@Composable
fun DeletePasswordDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_password_title)) },
        text = { Text(stringResource(R.string.dialog_delete_password_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        }
    )
}

@Composable
fun MovePasswordDialog(
    categories: Map<String, Long>,
    onCategorySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Sort categories by name
    val sortedCategories = categories.entries.sortedBy { it.key }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_select)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(sortedCategories) { (name, id) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        modifier = Modifier.clickable { onCategorySelected(id) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
