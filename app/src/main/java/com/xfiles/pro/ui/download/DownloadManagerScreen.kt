package com.xfiles.pro.ui.download

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xfiles.pro.R
import com.xfiles.pro.XFilesApplication
import com.xfiles.pro.ui.download.components.DeleteDownloadDialog
import com.xfiles.pro.ui.download.components.DownloadDetailsDialog
import com.xfiles.pro.ui.download.components.DownloadItemCard
import com.xfiles.pro.ui.download.components.DownloadRenameDialog
import com.xfiles.pro.ui.download.components.NewDownloadDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    onBackClick: () -> Unit,
    onNavigateToFolder: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as XFilesApplication
    val viewModel: DownloadViewModel = viewModel(
        factory = DownloadViewModel.provideFactory(app.container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Android 13+ Notification permission request
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.download_manager_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("download_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.completedCount > 0) {
                        IconButton(
                            onClick = { viewModel.clearCompleted() },
                            modifier = Modifier.testTag("clear_completed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(id = R.string.download_action_clear_completed)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewDownloadDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("download_fab_new")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.download_action_add)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("download_manager_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(id = R.string.download_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("download_search_input")
            )

            // Filter Chips Row
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadFilter.entries.forEach { filter ->
                    val isSelected = uiState.filter == filter
                    val label = stringResource(id = filter.labelRes)
                    val count = when (filter) {
                        DownloadFilter.ALL -> uiState.items.size
                        DownloadFilter.ACTIVE -> uiState.activeCount
                        DownloadFilter.COMPLETED -> uiState.completedCount
                        DownloadFilter.PAUSED_FAILED -> uiState.items.size - uiState.activeCount - uiState.completedCount
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text("$label ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List or Empty State
            val filtered = uiState.filteredItems
            if (filtered.isEmpty()) {
                DownloadEmptyState(
                    filter = uiState.filter,
                    isSearching = uiState.searchQuery.isNotBlank()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloads_lazy_column"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filtered, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onPauseClick = { viewModel.pauseDownload(it) },
                            onResumeClick = { viewModel.resumeDownload(it) },
                            onCancelClick = { viewModel.cancelDownload(it) },
                            onRetryClick = { viewModel.retryDownload(it) },
                            onDetailsClick = { viewModel.openDetails(it) },
                            onRenameClick = { viewModel.openRename(it) },
                            onDeleteClick = { viewModel.openDeleteConfirm(it) },
                            onNavigateToFolder = onNavigateToFolder
                        )
                    }
                }
            }
        }
    }

    // New Download Dialog
    if (uiState.showNewDownloadDialog) {
        NewDownloadDialog(
            defaultDirectory = uiState.defaultDestinationDirectory,
            onDismiss = { viewModel.closeNewDownloadDialog() },
            onStartDownload = { url, fileName, destDir ->
                viewModel.startDownload(url, fileName, destDir)
            }
        )
    }

    // Details Dialog
    if (uiState.showDetailsDialog && uiState.selectedItem != null) {
        DownloadDetailsDialog(
            item = uiState.selectedItem!!,
            onDismiss = { viewModel.closeDetails() }
        )
    }

    // Rename Dialog
    if (uiState.showRenameDialog && uiState.selectedItem != null) {
        DownloadRenameDialog(
            item = uiState.selectedItem!!,
            onDismiss = { viewModel.closeRename() },
            onConfirmRename = { newName ->
                viewModel.renameItem(uiState.selectedItem!!.id, newName)
            }
        )
    }

    // Delete Dialog
    if (uiState.showDeleteConfirmDialog && uiState.selectedItem != null) {
        DeleteDownloadDialog(
            item = uiState.selectedItem!!,
            onDismiss = { viewModel.closeDeleteConfirm() },
            onConfirmDelete = { deleteFile ->
                viewModel.confirmDelete(uiState.selectedItem!!.id, deleteFile)
            }
        )
    }
}

@Composable
private fun DownloadEmptyState(
    filter: DownloadFilter,
    isSearching: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("downloads_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = if (isSearching) {
                stringResource(id = R.string.search_no_results)
            } else {
                when (filter) {
                    DownloadFilter.ALL -> stringResource(id = R.string.download_empty_all)
                    DownloadFilter.ACTIVE -> stringResource(id = R.string.download_empty_active)
                    DownloadFilter.COMPLETED -> stringResource(id = R.string.download_empty_completed)
                    DownloadFilter.PAUSED_FAILED -> stringResource(id = R.string.download_empty_paused)
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.download_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
