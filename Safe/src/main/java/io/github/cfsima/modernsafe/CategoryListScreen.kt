package io.github.cfsima.modernsafe

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.cfsima.modernsafe.model.CategoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    uiState: CategoryListUiState,
    onCategoryClick: (Long) -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (Long, String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onSearch: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onChangePassword: () -> Unit,
    onPreferences: () -> Unit,
    onAbout: () -> Unit,
    onUserMessageDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(value = false) }
    var showAddDialog by remember { mutableStateOf(value = false) }
    var newCategoryName by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Edit Dialog State
    var categoryToEdit by remember { mutableStateOf<CategoryEntry?>(null) }
    var editCategoryName by remember { mutableStateOf("") }

    // Delete Dialog State
    var categoryToDelete by remember { mutableStateOf<CategoryEntry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onUserMessageDismiss()
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.password_add)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddCategory(newCategoryName)
                        showAddDialog = false
                        newCategoryName = ""
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                ) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    // Edit Dialog
    if (categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(stringResource(R.string.edit_entry)) },
            text = {
                OutlinedTextField(
                    value = editCategoryName,
                    onValueChange = { editCategoryName = it },
                    label = { Text(stringResource(R.string.category)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editCategoryName.trim().isEmpty()) {
                            Toast.makeText(context, R.string.notify_blank_name, Toast.LENGTH_SHORT).show()
                        } else {
                            categoryToEdit?.let {
                                onEditCategory(it.id, editCategoryName)
                            }
                            categoryToEdit = null
                            editCategoryName = ""
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToEdit = null },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Delete Dialog
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.password_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_category, categoryToDelete?.plainName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let {
                            onDeleteCategory(it.id)
                        }
                        categoryToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                ) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.backup)) },
                            onClick = {
                                showMenu = false
                                onBackup()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.restore)) },
                            onClick = {
                                showMenu = false
                                onRestore()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_database)) },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_database)) },
                            onClick = {
                                showMenu = false
                                onImport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_password)) },
                            onClick = {
                                showMenu = false
                                onChangePassword()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.preferences)) },
                            onClick = {
                                showMenu = false
                                onPreferences()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = {
                                showMenu = false
                                onAbout()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.password_add))
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_category),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
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
                contentPadding = paddingValues,
            ) {
                items(uiState.categories) { category ->
                    CategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.id) },
                        onEdit = {
                            editCategoryName = category.plainName ?: ""
                            categoryToEdit = category
                        },
                        onDelete = {
                            categoryToDelete = category
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryEntry,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showItemMenu by remember { mutableStateOf(value = false) }

    ListItem(
        headlineContent = { Text(category.plainName ?: "") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.count.toString(), modifier = Modifier.padding(end = 8.dp))
                Box {
                    IconButton(
                        onClick = { showItemMenu = true },
                        modifier = Modifier.testTag("menu_" + (category.plainName ?: "")),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showItemMenu,
                        onDismissRequest = { showItemMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_entry)) },
                            onClick = {
                                showItemMenu = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.password_delete)) },
                            onClick = {
                                showItemMenu = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}
