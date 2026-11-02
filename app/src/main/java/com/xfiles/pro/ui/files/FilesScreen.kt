package com.xfiles.pro.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xfiles.pro.R
import com.xfiles.pro.XFilesApplication
import com.xfiles.pro.core.util.ByteFormatter
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(
    isGridView: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = viewModel(
        factory = FilesViewModel.provideFactory(
            (LocalContext.current.applicationContext as XFilesApplication).container.storageRepository,
            (LocalContext.current.applicationContext as XFilesApplication).container.fileIndexingRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle back button inside directory hierarchy or search mode
    BackHandler(enabled = uiState.isSearching || uiState.breadcrumbs.size > 1) {
        if (uiState.isSearching) {
            viewModel.clearSearch()
        } else {
            viewModel.navigateUp()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("files_screen_container")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Indexing Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("files_search_input"),
                    placeholder = {
                        Text(
                            text = if (uiState.totalIndexedCount > 0) {
                                "Search ${uiState.totalIndexedCount} indexed files..."
                            } else {
                                "Search files..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearSearch() },
                                modifier = Modifier.testTag("files_search_clear_button")
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { viewModel.triggerManualIndexing(forceRescan = false) },
                    modifier = Modifier.testTag("files_index_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Index",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Indexing progress indicator
            if (uiState.indexingState.isIndexing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.indexingState.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Indexing ${uiState.indexingState.currentVolume ?: ""}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { viewModel.cancelIndexing() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Display Breadcrumbs when browsing normally
            if (!uiState.isSearching) {
                BreadcrumbBar(
                    breadcrumbs = uiState.breadcrumbs,
                    onBreadcrumbClick = { path -> viewModel.navigateToBreadcrumb(path) }
                )
            } else {
                // Search Results Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results (${uiState.searchResults.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.clearSearch() }) {
                        Text("Exit Search", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // File Listing / Search Results / Loading State
            val displayFiles = if (uiState.isSearching) uiState.searchResults else uiState.files

            if (uiState.isLoading && !uiState.isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("files_loading_indicator"))
                }
            } else if (displayFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSearching) Icons.Default.Search else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (uiState.isSearching) {
                                "No files found matching '${uiState.searchQuery}'"
                            } else {
                                "This folder is empty"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView || uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("files_grid_view")
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        FileGridCard(
                            item = item,
                            onClick = { viewModel.navigateInto(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(if (uiState.isSearching) "files_search_results_view" else "files_list_view")
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        FileListRow(
                            item = item,
                            onClick = { viewModel.navigateInto(item) },
                            onRenameClick = { viewModel.openRenameDialog(item) },
                            onDeleteClick = { viewModel.openDeleteDialog(item) },
                            onDetailsClick = { viewModel.selectItemForDetails(item) }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Creating Folder/File
        var showFabMenu by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            FloatingActionButton(
                onClick = { showFabMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("files_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Item")
            }

            DropdownMenu(
                expanded = showFabMenu,
                onDismissRequest = { showFabMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("New Folder") },
                    onClick = {
                        showFabMenu = false
                        viewModel.openCreateFolderDialog()
                    },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("New File") },
                    onClick = {
                        showFabMenu = false
                        viewModel.openCreateFileDialog()
                    },
                    leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                )
            }
        }

        // Dialogs
        if (uiState.showCreateFolderDialog) {
            InputDialog(
                title = "Create New Folder",
                label = "Folder name",
                onConfirm = { name -> viewModel.createFolder(name) },
                onDismiss = { viewModel.closeCreateFolderDialog() }
            )
        }

        if (uiState.showCreateFileDialog) {
            InputDialog(
                title = "Create New File",
                label = "File name (e.g. note.txt)",
                onConfirm = { name -> viewModel.createFile(name) },
                onDismiss = { viewModel.closeCreateFileDialog() }
            )
        }

        uiState.itemToRename?.let { item ->
            InputDialog(
                title = "Rename ${if (item.isDirectory) "Folder" else "File"}",
                label = "New name",
                initialValue = item.name,
                onConfirm = { newName -> viewModel.renameFile(item, newName) },
                onDismiss = { viewModel.closeRenameDialog() }
            )
        }

        uiState.itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { viewModel.closeDeleteDialog() },
                title = { Text("Delete Confirmation") },
                text = { Text("Are you sure you want to delete '${item.name}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteFile(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        uiState.selectedItemForDetails?.let { item ->
            FileDetailsDialog(
                item = item,
                onDismiss = { viewModel.selectItemForDetails(null) }
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onBreadcrumbClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("breadcrumb_bar"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            breadcrumbs.forEachIndexed { index, (name, path) ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onBreadcrumbClick(path) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (index < breadcrumbs.lastIndex) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListRow(
    item: FileItem,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = remember(item.lastModified) {
        if (item.lastModified > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.lastModified))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("file_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.isDirectory) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(item),
                    contentDescription = null,
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (item.isDirectory) {
                    formattedDate
                } else {
                    "${ByteFormatter.format(item.size)} • $formattedDate"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            menuExpanded = false
                            onDetailsClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridCard(
    item: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("file_grid_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isDirectory) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(item),
                    contentDescription = null,
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (item.isDirectory) "Folder" else ByteFormatter.format(item.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isNotBlank()) {
                        onConfirm(textValue)
                    }
                },
                enabled = textValue.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(item.lastModified) {
        if (item.lastModified > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.lastModified))
        } else "Unknown"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Name", item.name)
                DetailRow("Type", item.fileType.name)
                if (!item.isDirectory) {
                    DetailRow("Size", "${ByteFormatter.format(item.size)} (${item.size} bytes)")
                }
                DetailRow("Path", item.path)
                DetailRow("Modified", formattedDate)
                DetailRow("Readable", if (item.canRead) "Yes" else "No")
                DetailRow("Writable", if (item.canWrite) "Yes" else "No")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getFileIcon(item: FileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.fileType) {
        FileType.IMAGE -> Icons.Default.Image
        FileType.VIDEO -> Icons.Default.VideoFile
        FileType.AUDIO -> Icons.Default.MusicNote
        FileType.DOCUMENT -> Icons.Default.Description
        else -> Icons.Default.Description
    }
}
