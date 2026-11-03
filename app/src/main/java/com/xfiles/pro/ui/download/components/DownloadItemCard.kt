package com.xfiles.pro.ui.download.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.xfiles.pro.R
import com.xfiles.pro.core.util.ByteFormatter
import com.xfiles.pro.domain.model.DownloadItem
import com.xfiles.pro.domain.model.DownloadStatus
import java.io.File

@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onRetryClick: (String) -> Unit,
    onDetailsClick: (DownloadItem) -> Unit,
    onRenameClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onNavigateToFolder: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Status Icon, File Name, and Context Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status / Type Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusContainerColor(item.status)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.status) {
                            DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                            DownloadStatus.FAILED -> Icons.Default.Error
                            DownloadStatus.DOWNLOADING -> Icons.Default.PlayArrow
                            DownloadStatus.PAUSED -> Icons.Default.Pause
                            DownloadStatus.QUEUED -> Icons.Default.Refresh
                            DownloadStatus.CANCELLED -> Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = statusContentColor(item.status),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("download_card_name_${item.id}")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusContainerColor(item.status)
                        ) {
                            Text(
                                text = statusText(item.status),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = statusContentColor(item.status),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!item.isResumable && item.status.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.download_not_resumable),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("download_card_menu_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.download_action_details)) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDetailsClick(item)
                            }
                        )

                        if (item.status == DownloadStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.download_action_open)) },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    openDownloadedFile(context, item.destinationFilePath, item.mimeType)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.download_action_go_to_folder)) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToFolder?.invoke(item.destinationDirectory)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.download_action_rename)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRenameClick(item)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.copy_link)) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(item.url))
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.download_action_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick(item)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            if (item.status.isActive) {
                if (item.isIndeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { item.progressFraction ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (item.status == DownloadStatus.PAUSED && item.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { item.progressFraction ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.outline,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats Row: Downloaded/Total, Percentage, Speed/ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val downloadedFormatted = ByteFormatter.format(item.downloadedBytes)
                val totalFormatted = if (item.totalBytes > 0) ByteFormatter.format(item.totalBytes) else "--"

                val sizeText = if (item.totalBytes > 0) {
                    "$downloadedFormatted / $totalFormatted (${item.progressPercentage}%)"
                } else {
                    downloadedFormatted
                }

                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Secondary stat: speed and ETA for active, or error message for failed
                if (item.status == DownloadStatus.DOWNLOADING) {
                    val speedText = if (item.downloadSpeed > 0) "${ByteFormatter.format(item.downloadSpeed)}/s" else ""
                    val etaText = if (item.etaSeconds > 0) formatEta(item.etaSeconds) else ""
                    val speedEta = listOf(speedText, etaText).filter { it.isNotBlank() }.joinToString(" • ")

                    if (speedEta.isNotBlank()) {
                        Text(
                            text = speedEta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(
                            onClick = { onPauseClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_pause_${item.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = stringResource(id = R.string.action_pause), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { onCancelClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_cancel_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(
                            onClick = { onResumeClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_resume_${item.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(id = R.string.action_resume), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { onCancelClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_cancel_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.QUEUED -> {
                        IconButton(
                            onClick = { onCancelClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_cancel_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.action_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        IconButton(
                            onClick = { onRetryClick(item.id) },
                            modifier = Modifier.size(36.dp).testTag("action_retry_${item.id}")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.action_retry), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { onDeleteClick(item) },
                            modifier = Modifier.size(36.dp).testTag("action_delete_${item.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(
                            onClick = { openDownloadedFile(context, item.destinationFilePath, item.mimeType) },
                            modifier = Modifier.size(36.dp).testTag("action_open_${item.id}")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = stringResource(id = R.string.download_action_open), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusContainerColor(status: DownloadStatus) = when (status) {
    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    DownloadStatus.QUEUED -> MaterialTheme.colorScheme.surfaceVariant
    DownloadStatus.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun statusContentColor(status: DownloadStatus) = when (status) {
    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondary
    DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
    DownloadStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun statusText(status: DownloadStatus) = when (status) {
    DownloadStatus.QUEUED -> stringResource(id = R.string.download_status_queued)
    DownloadStatus.DOWNLOADING -> stringResource(id = R.string.download_status_downloading)
    DownloadStatus.PAUSED -> stringResource(id = R.string.download_status_paused)
    DownloadStatus.COMPLETED -> stringResource(id = R.string.download_status_completed)
    DownloadStatus.FAILED -> stringResource(id = R.string.download_status_failed)
    DownloadStatus.CANCELLED -> stringResource(id = R.string.download_status_cancelled)
}

private fun formatEta(seconds: Long): String {
    return if (seconds < 60) {
        "${seconds}s"
    } else if (seconds < 3600) {
        val m = seconds / 60
        val s = seconds % 60
        "${m}m ${s}s"
    } else {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        "${h}h ${m}m"
    }
}

private fun openDownloadedFile(context: Context, filePath: String, mimeType: String?) {
    try {
        val file = File(filePath)
        if (!file.exists()) return

        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
