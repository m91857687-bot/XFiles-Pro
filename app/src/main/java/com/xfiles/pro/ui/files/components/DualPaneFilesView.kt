package com.xfiles.pro.ui.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.ui.files.PaneState

@Composable
fun DualPaneFilesView(
    primaryPane: PaneState,
    secondaryPane: PaneState,
    activePaneId: PaneId,
    onSetActivePane: (PaneId) -> Unit,
    onNavigateInto: (FileItem, PaneId) -> Unit,
    onBreadcrumbClick: (String, PaneId) -> Unit,
    onRenameClick: (FileItem, PaneId) -> Unit,
    onDeleteClick: (FileItem, PaneId) -> Unit,
    onDetailsClick: (FileItem, PaneId) -> Unit,
    onCopyToOther: (FileItem) -> Unit,
    onMoveToOther: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .testTag("dual_pane_container")
    ) {
        // Primary Pane (Left)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
        ) {
            IndividualPane(
                paneState = primaryPane,
                paneId = PaneId.PRIMARY,
                isActive = activePaneId == PaneId.PRIMARY,
                label = stringResource(R.string.dual_pane_primary),
                onActivate = { onSetActivePane(PaneId.PRIMARY) },
                onNavigateInto = { onNavigateInto(it, PaneId.PRIMARY) },
                onBreadcrumbClick = { onBreadcrumbClick(it, PaneId.PRIMARY) },
                onRenameClick = { onRenameClick(it, PaneId.PRIMARY) },
                onDeleteClick = { onDeleteClick(it, PaneId.PRIMARY) },
                onDetailsClick = { onDetailsClick(it, PaneId.PRIMARY) },
                onCopyToOther = onCopyToOther,
                onMoveToOther = onMoveToOther
            )
        }

        // Secondary Pane (Right)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
        ) {
            IndividualPane(
                paneState = secondaryPane,
                paneId = PaneId.SECONDARY,
                isActive = activePaneId == PaneId.SECONDARY,
                label = stringResource(R.string.dual_pane_secondary),
                onActivate = { onSetActivePane(PaneId.SECONDARY) },
                onNavigateInto = { onNavigateInto(it, PaneId.SECONDARY) },
                onBreadcrumbClick = { onBreadcrumbClick(it, PaneId.SECONDARY) },
                onRenameClick = { onRenameClick(it, PaneId.SECONDARY) },
                onDeleteClick = { onDeleteClick(it, PaneId.SECONDARY) },
                onDetailsClick = { onDetailsClick(it, PaneId.SECONDARY) },
                onCopyToOther = onCopyToOther,
                onMoveToOther = onMoveToOther
            )
        }
    }
}

@Composable
private fun IndividualPane(
    paneState: PaneState,
    paneId: PaneId,
    isActive: Boolean,
    label: String,
    onActivate: () -> Unit,
    onNavigateInto: (FileItem) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onRenameClick: (FileItem) -> Unit,
    onDeleteClick: (FileItem) -> Unit,
    onDetailsClick: (FileItem) -> Unit,
    onCopyToOther: (FileItem) -> Unit,
    onMoveToOther: (FileItem) -> Unit
) {
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val headerBg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onActivate() }
            .testTag(if (paneId == PaneId.PRIMARY) "pane_primary" else "pane_secondary"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pane Header
            Surface(
                color = headerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Path Breadcrumb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = paneState.currentPath.substringAfterLast('/', "Storage"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Files Content
            if (paneState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (paneState.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Folder is empty",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(paneState.files, key = { it.path }) { item ->
                        PaneFileRow(
                            item = item,
                            isActivePane = isActive,
                            onClick = { onNavigateInto(item) },
                            onCopyToOther = { onCopyToOther(item) },
                            onMoveToOther = { onMoveToOther(item) },
                            onDetailsClick = { onDetailsClick(item) },
                            onDeleteClick = { onDeleteClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneFileRow(
    item: FileItem,
    isActivePane: Boolean,
    onClick: () -> Unit,
    onCopyToOther: () -> Unit,
    onMoveToOther: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Folder,
                contentDescription = null,
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.isDirectory) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cross-pane transfer action buttons
            if (isActivePane) {
                IconButton(
                    onClick = onCopyToOther,
                    modifier = Modifier.size(28.dp).testTag("dual_pane_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.dual_pane_copy_to_other),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveToOther,
                    modifier = Modifier.size(28.dp).testTag("dual_pane_move_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = stringResource(R.string.dual_pane_move_to_other),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
