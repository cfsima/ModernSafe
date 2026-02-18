package io.github.cfsima.modernsafe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
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
