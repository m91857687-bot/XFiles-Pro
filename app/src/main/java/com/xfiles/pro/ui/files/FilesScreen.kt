package com.xfiles.pro.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import com.xfiles.pro.R
import com.xfiles.pro.XFilesApplication
import com.xfiles.pro.core.util.ByteFormatter
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.ui.archive.ArchiveViewerScreen
import com.xfiles.pro.ui.archive.ArchiveViewModel
import com.xfiles.pro.ui.archive.components.ArchiveProgressDialog
import com.xfiles.pro.ui.archive.components.CreateArchiveDialog
import com.xfiles.pro.ui.archive.components.ExtractArchiveDialog
import com.xfiles.pro.ui.files.components.AdvancedSearchSheet
import com.xfiles.pro.ui.files.components.DualPaneFilesView
import com.xfiles.pro.ui.files.components.FileTabBar
import com.xfiles.pro.ui.files.components.SearchHistoryView
import java.io.File
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
            (LocalContext.current.applicationContext as XFilesApplication).container.fileIndexingRepository,
            (LocalContext.current.applicationContext as XFilesApplication).container.tabRepository,
            (LocalContext.current.applicationContext as XFilesApplication).container.searchHistoryRepository,
            (LocalContext.current.applicationContext as XFilesApplication).container.archiveRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as XFilesApplication).container

    if (uiState.archiveFileToView != null) {
        val archiveFile = File(uiState.archiveFileToView!!.path)
        val archiveViewModel: ArchiveViewModel = viewModel(
            key = "archive_viewer_${archiveFile.absolutePath}",
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ArchiveViewModel(appContainer.archiveRepository) as T
                }
            }
        )
        LaunchedEffect(archiveFile) {
            archiveViewModel.loadArchive(archiveFile)
        }
        ArchiveViewerScreen(
            viewModel = archiveViewModel,
            onBack = { viewModel.viewArchive(null) }
        )
        return
    }

    // Handle back button inside directory hierarchy or search mode
    BackHandler(enabled = uiState.isSearching || uiState.breadcrumbs.size > 1) {
        if (uiState.isSearching) {
            viewModel.clearSearch()
        } else {
            viewModel.navigateUp()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("files_screen_container")) {
        val isWideScreen = maxWidth >= 600.dp
        val showDualPane = isWideScreen || uiState.isDualPaneMode

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Search Bar & Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchCriteria.query,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("files_search_input"),
                    placeholder = {
                        Text(
                            text = if (uiState.totalIndexedCount > 0) {
                                "Search ${uiState.totalIndexedCount} files..."
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
                        if (uiState.searchCriteria.query.isNotEmpty()) {
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

                // Filter Button with Badge if filters are active
                BadgedBox(
                    badge = {
                        if (uiState.searchCriteria.isFilteringActive) {
                            Badge { Text("!") }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { viewModel.toggleFilterSheet(true) },
                        modifier = Modifier.testTag("files_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_title),
                            tint = if (uiState.searchCriteria.isFilteringActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Dual Pane Toggle Button
                IconButton(
                    onClick = { viewModel.toggleDualPaneMode() },
                    modifier = Modifier.testTag("files_dual_pane_toggle")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                        contentDescription = stringResource(R.string.dual_pane_toggle),
                        tint = if (showDualPane) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Manual Index Refresh Button
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

            // 2. Multi-Tab Bar
            FileTabBar(
                tabs = uiState.tabs,
                activeTabIndex = uiState.activeTabIndex,
                onTabSelected = { viewModel.switchTab(it) },
                onTabClosed = { viewModel.closeTab(it) },
                onCloseOthers = { viewModel.closeOtherTabs() },
                onNewTabClick = { viewModel.openNewTab() }
            )

            // 3. Progress Indicators (Indexing & Transfer)
            if (uiState.indexingState.isIndexing) {
                LinearProgressIndicator(
                    progress = { uiState.indexingState.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
            }
            if (uiState.isTransferring) {
                LinearProgressIndicator(
                    progress = { uiState.transferProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // 4. Status / Operation Banner
            uiState.operationSuccessMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        IconButton(onClick = { viewModel.clearOperationMessage() }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // 5. Main Area: Search Results OR Recent History OR Directory Browsing
            if (uiState.isSearching) {
                SearchResultsView(
                    query = uiState.searchCriteria.query,
                    results = uiState.searchResults,
                    onFileClick = { item -> viewModel.navigateInto(item) },
                    onOpenParentFolder = { item -> viewModel.navigateToParentFolder(item) },
                    onOpenInNewTab = { item -> viewModel.openSearchResultInNewTab(item) },
                    onCopyToOtherPane = { item -> viewModel.copyFileToOtherPane(item) },
                    onClearSearch = { viewModel.clearSearch() }
                )
            } else {
                // If not searching, show recent search history when query field is touched, or file browsing
                if (uiState.searchCriteria.query.isEmpty() && uiState.searchHistory.isNotEmpty() && uiState.isSearchHistoryVisible) {
                    SearchHistoryView(
                        history = uiState.searchHistory,
                        onQueryClick = { viewModel.rerunRecentSearch(it) },
                        onRemoveQuery = { viewModel.removeSearchFromHistory(it) },
                        onClearAll = { viewModel.clearSearchHistory() }
                    )
                }

                // File Browsing Area: Dual Pane vs Single Pane
                if (showDualPane) {
                    DualPaneFilesView(
                        primaryPane = uiState.primaryPane,
                        secondaryPane = uiState.secondaryPane,
                        activePaneId = uiState.activePaneId,
                        onSetActivePane = { viewModel.setActivePane(it) },
                        onNavigateInto = { item, pane -> viewModel.navigateInto(item, pane) },
                        onBreadcrumbClick = { path, pane -> viewModel.navigateToBreadcrumb(path, pane) },
                        onRenameClick = { item, pane -> viewModel.openRenameDialog(item) },
                        onDeleteClick = { item, pane -> viewModel.openDeleteDialog(item) },
                        onDetailsClick = { item, pane -> viewModel.selectItemForDetails(item, pane) },
                        onCopyToOther = { item -> viewModel.copyFileToOtherPane(item) },
                        onMoveToOther = { item -> viewModel.moveFileToOtherPane(item) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    SinglePaneSection(
                        uiState = uiState,
                        isGridView = isGridView,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // FAB for Creating Folder / File
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
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.archive_create_title)) },
                    onClick = {
                        showFabMenu = false
                        viewModel.showCreateArchiveDialog(true)
                    },
                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    modifier = Modifier.testTag("fab_create_archive_item")
                )
            }
        }

        // Advanced Search Filter Bottom Sheet
        if (uiState.showFilterSheet) {
            AdvancedSearchSheet(
                criteria = uiState.searchCriteria,
                currentFolderPath = uiState.activePaneState.currentPath,
                onCategorySelected = { viewModel.setFileTypeFilter(it) },
                onSizeSelected = { viewModel.setSizeFilter(it) },
                onDateSelected = { viewModel.setDateFilter(it) },
                onTargetSelected = { viewModel.setSearchTarget(it) },
                onScopeSelected = { viewModel.setSearchScope(it) },
                onClearFilters = { viewModel.clearFilters() },
                onDismiss = { viewModel.toggleFilterSheet(false) }
            )
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

        if (uiState.showCreateArchiveDialog) {
            val currentDir = File(uiState.activePaneState.currentPath)
            val selectedFiles: List<File> = uiState.activePaneState.selectedItemForAction?.let { listOf(File(it.path)) }
                ?: listOf(currentDir)
            CreateArchiveDialog(
                selectedFiles = selectedFiles,
                onDismiss = { viewModel.showCreateArchiveDialog(false) },
                onConfirm = { name, format, level, password ->
                    viewModel.createArchive(name, format, level, password)
                }
            )
        }

        uiState.archiveFileToExtract?.let { item ->
            val archiveFile = File(item.path)
            val destDir = archiveFile.parentFile ?: File(uiState.activePaneState.currentPath)
            ExtractArchiveDialog(
                archiveFile = archiveFile,
                defaultDestinationDir = File(destDir, archiveFile.nameWithoutExtension),
                hasSelectedEntries = false,
                onDismiss = { viewModel.showExtractArchiveDialog(null) },
                onConfirm = { dest, conflict, selectedOnly ->
                    viewModel.extractArchive(dest, conflict, selectedOnly)
                }
            )
        }

        if (uiState.isArchiveOperationInProgress && uiState.archiveProgress != null) {
            ArchiveProgressDialog(
                title = stringResource(R.string.archive_operation_in_progress),
                progress = uiState.archiveProgress!!,
                onCancel = { viewModel.cancelArchiveOperation() }
            )
        }
    }
}

@Composable
private fun SinglePaneSection(
    uiState: FilesUiState,
    isGridView: Boolean,
    viewModel: FilesViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        BreadcrumbBar(
            breadcrumbs = uiState.breadcrumbs,
            onBreadcrumbClick = { path -> viewModel.navigateToBreadcrumb(path) }
        )

        val displayFiles = uiState.files

        if (uiState.isLoading) {
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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "This folder is empty",
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
                modifier = Modifier.fillMaxSize().testTag("files_grid_view")
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
                modifier = Modifier.fillMaxSize().testTag("files_list_view")
            ) {
                items(displayFiles, key = { it.path }) { item ->
                    FileListRow(
                        item = item,
                        onClick = { viewModel.navigateInto(item) },
                        onRenameClick = { viewModel.openRenameDialog(item) },
                        onDeleteClick = { viewModel.openDeleteDialog(item) },
                        onDetailsClick = { viewModel.selectItemForDetails(item) },
                        onExtractClick = { viewModel.showExtractArchiveDialog(item) },
                        onCompressClick = { viewModel.showCreateArchiveDialog(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    results: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onOpenParentFolder: (FileItem) -> Unit,
    onOpenInNewTab: (FileItem) -> Unit,
    onCopyToOtherPane: (FileItem) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results (${results.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onClearSearch) {
                Text("Exit Search", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files found matching criteria",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().testTag("files_search_results_view")
            ) {
                items(results, key = { it.path }) { item ->
                    SearchResultRow(
                        item = item,
                        onClick = { onFileClick(item) },
                        onOpenParentFolder = { onOpenParentFolder(item) },
                        onOpenInNewTab = { onOpenInNewTab(item) },
                        onCopyToOtherPane = { onCopyToOtherPane(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: FileItem,
    onClick: () -> Unit,
    onOpenParentFolder: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onCopyToOtherPane: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("search_result_item_${item.name}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Folder,
                contentDescription = null,
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_open_parent)) },
                        onClick = {
                            showMenu = false
                            onOpenParentFolder()
                        },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_open_new_tab)) },
                        onClick = {
                            showMenu = false
                            onOpenInNewTab()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dual_pane_copy_to_other)) },
                        onClick = {
                            showMenu = false
                            onCopyToOtherPane()
                        }
                    )
                }
            }
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
    onDetailsClick: () -> Unit,
    onExtractClick: () -> Unit = {},
    onCompressClick: () -> Unit = {}
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.fileType == FileType.ARCHIVE -> Icons.Default.Archive
                    else -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = when {
                    item.isDirectory -> MaterialTheme.colorScheme.primary
                    item.fileType == FileType.ARCHIVE -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Folder" else item.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    if (item.fileType == FileType.ARCHIVE) {
                        DropdownMenuItem(
                            text = { Text("Open Archive") },
                            onClick = {
                                expandedMenu = false
                                onClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Extract Here") },
                            onClick = {
                                expandedMenu = false
                                onExtractClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Compress") },
                        onClick = {
                            expandedMenu = false
                            onCompressClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            expandedMenu = false
                            onDetailsClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            expandedMenu = false
                            onRenameClick()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expandedMenu = false
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
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Folder,
                contentDescription = null,
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (item.isDirectory) "Folder" else item.formattedSize,
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
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("dialog_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
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
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.isDirectory) "Folder Details" else "File Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name: ${item.name}", fontWeight = FontWeight.Bold)
                Text("Path: ${item.path}")
                Text("Size: ${item.formattedSize} (${item.size} bytes)")
                Text("Modified: ${dateFormat.format(Date(item.lastModified))}")
                Text("Type: ${item.fileType.name}")
                Text("Writable: ${if (item.canWrite) "Yes" else "No"}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
