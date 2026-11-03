package com.xfiles.pro.ui.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.core.util.ByteFormatter
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.ui.archive.components.ArchiveProgressDialog
import com.xfiles.pro.ui.archive.components.ExtractArchiveDialog
import com.xfiles.pro.ui.archive.components.PasswordPromptDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    viewModel: ArchiveViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by remember { mutableStateOf(false) }

    // Intercept back button to navigate up virtual hierarchy
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        } else if (!viewModel.navigateUp()) {
            onBack()
        }
    }

    LaunchedEffect(uiState.operationSuccessMessage) {
        uiState.operationSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("archive_viewer_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_in_archive_hint)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (uiState.searchQuery.isEmpty()) {
                                        isSearchActive = false
                                    } else {
                                        viewModel.setSearchQuery("")
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("archive_search_input")
                        )
                    } else {
                        Column {
                            Text(
                                text = uiState.archiveFile?.name ?: stringResource(R.string.archive_viewer_title),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${uiState.allEntries.size} items • ${ByteFormatter.format(uiState.totalUncompressedSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                viewModel.setSearchQuery("")
                            } else if (!viewModel.navigateUp()) {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("archive_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("archive_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_hint))
                        }

                        if (uiState.selectedEntryPaths.isNotEmpty()) {
                            if (uiState.format?.supportsInPlaceModification == true) {
                                IconButton(
                                    onClick = { viewModel.deleteSelectedEntries() },
                                    modifier = Modifier.testTag("archive_delete_selected_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.showExtractDialog(true) },
                            modifier = Modifier.testTag("archive_extract_action_button")
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = stringResource(R.string.archive_extract_title))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Virtual Breadcrumbs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.virtualBreadcrumbs.forEachIndexed { index, (label, path) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (index == uiState.virtualBreadcrumbs.lastIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable { viewModel.navigateToBreadcrumb(path) }
                            .padding(4.dp)
                    )
                    if (index < uiState.virtualBreadcrumbs.lastIndex) {
                        Text(
                            text = " / ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Selection status header
            if (uiState.selectedEntryPaths.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.selectedEntryPaths.size} selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text(stringResource(R.string.select_all))
                            }
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text(stringResource(R.string.clear_selection))
                            }
                        }
                    }
                }
            }

            // Entries List or Loading / Empty state
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("archive_loading_indicator")
                    )
                } else if (uiState.visibleEntries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) {
                                stringResource(R.string.search_no_results)
                            } else {
                                stringResource(R.string.archive_empty)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("archive_entries_list")
                    ) {
                        items(uiState.visibleEntries, key = { it.path }) { entry ->
                            ArchiveEntryRow(
                                entry = entry,
                                isSelected = uiState.selectedEntryPaths.contains(entry.path),
                                onToggleSelect = { viewModel.toggleSelection(entry.path) },
                                onClick = {
                                    if (entry.isDirectory) {
                                        viewModel.enterVirtualFolder(entry.path)
                                    } else {
                                        viewModel.toggleSelection(entry.path)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showExtractDialog && uiState.archiveFile != null) {
        val destDir = uiState.archiveFile!!.parentFile ?: File("/storage/emulated/0")
        ExtractArchiveDialog(
            archiveFile = uiState.archiveFile!!,
            defaultDestinationDir = File(destDir, uiState.archiveFile!!.nameWithoutExtension),
            hasSelectedEntries = uiState.selectedEntryPaths.isNotEmpty(),
            onDismiss = { viewModel.showExtractDialog(false) },
            onConfirm = { dest, conflict, selectedOnly ->
                viewModel.extract(dest, conflict, selectedOnly)
            }
        )
    }

    if (uiState.showPasswordPrompt && uiState.archiveFile != null) {
        PasswordPromptDialog(
            archiveName = uiState.archiveFile!!.name,
            errorMessage = uiState.errorMessage,
            onDismiss = {
                viewModel.showPasswordPrompt(false)
                onBack()
            },
            onConfirm = { password ->
                viewModel.loadArchive(uiState.archiveFile!!, password)
            }
        )
    }

    if (uiState.isOperationInProgress && uiState.progress != null) {
        ArchiveProgressDialog(
            title = stringResource(R.string.archive_operation_in_progress),
            progress = uiState.progress!!,
            onCancel = { viewModel.cancelOperation() }
        )
    }
}

@Composable
fun ArchiveEntryRow(
    entry: ArchiveEntryItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            modifier = Modifier.testTag("entry_checkbox_${entry.name}")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = when {
                entry.isDirectory -> Icons.Default.Folder
                entry.isEncrypted -> Icons.Default.Lock
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!entry.isDirectory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ByteFormatter.format(entry.uncompressedSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.lastModified > 0) {
                        val dateStr = remember(entry.lastModified) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.lastModified))
                        }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
