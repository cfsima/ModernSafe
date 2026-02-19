package io.github.cfsima.modernsafe

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.cfsima.modernsafe.model.CategoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable @Suppress("UNUSED_PARAMETER")
fun CategoryListScreen(
    uiState: CategoryListUiState,
    onCategoryClick: (Long) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (CategoryEntry) -> Unit,
    onDeleteCategory: (CategoryEntry) -> Unit,
    onSearch: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onChangePassword: () -> Unit,
    onPreferences: () -> Unit,
    onAbout: () -> Unit,
    onUserMessageDismiss: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    if (uiState.userMessage != null) {
        AlertDialog(
            onDismissRequest = onUserMessageDismiss,
            confirmButton = { TextButton(onClick = onUserMessageDismiss) { Text("OK") } },
            text = { Text(uiState.userMessage) }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.password_add)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddCategory(newCategoryName)
                        showAddDialog = false
                        newCategoryName = ""
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup)) },
                            onClick = { showMenu = false; onBackup() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.restore)) },
                            onClick = { showMenu = false; onRestore() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_database)) },
                            onClick = { showMenu = false; onExport() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_database)) },
                            onClick = { showMenu = false; onImport() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_password)) },
                            onClick = { showMenu = false; onChangePassword() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.preferences)) },
                            onClick = { showMenu = false; onPreferences() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = { showMenu = false; onAbout() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.password_add))
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(uiState.categories) { category ->
                    CategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
        }
    }
}

@Composable @Suppress("UNUSED_PARAMETER")
fun CategoryItem(
    category: CategoryEntry,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(category.plainName ?: "") },
        trailingContent = { Text(category.count.toString()) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
