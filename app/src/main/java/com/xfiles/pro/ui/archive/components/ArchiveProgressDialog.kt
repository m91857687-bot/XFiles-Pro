package com.xfiles.pro.ui.archive.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.ArchiveProgress

@Composable
fun ArchiveProgressDialog(
    title: String,
    progress: ArchiveProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Non-cancellable by touching outside
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (progress.currentItem.isNotEmpty()) {
                    Text(
                        text = progress.currentItem,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("archive_progress_current_item")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (progress.isIndeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive_progress_bar")
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive_progress_bar")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (progress.totalItems > 0) {
                        Text(
                            text = "${progress.completedItems} / ${progress.totalItems}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!progress.isIndeterminate) {
                        Text(
                            text = "${(progress.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_archive_progress_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
