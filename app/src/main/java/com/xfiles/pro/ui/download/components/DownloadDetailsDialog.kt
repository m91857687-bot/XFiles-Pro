package com.xfiles.pro.ui.download.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.core.util.ByteFormatter
import com.xfiles.pro.domain.model.DownloadItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadDetailsDialog(
    item: DownloadItem,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.download_details_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                DetailItem(label = stringResource(id = R.string.download_details_filename), value = item.fileName)
                
                // URL with copy action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.download_details_url),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(item.url)) },
                        modifier = Modifier.testTag("details_copy_url_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(id = R.string.copy_link))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailItem(label = stringResource(id = R.string.download_details_path), value = item.destinationFilePath)
                DetailItem(
                    label = stringResource(id = R.string.download_details_size),
                    value = "${ByteFormatter.format(item.downloadedBytes)} / " +
                        if (item.totalBytes > 0) ByteFormatter.format(item.totalBytes) else stringResource(id = R.string.unknown)
                )
                DetailItem(label = stringResource(id = R.string.download_details_status), value = item.status.name)
                DetailItem(
                    label = stringResource(id = R.string.download_details_resumable),
                    value = if (item.isResumable) stringResource(id = R.string.yes) else stringResource(id = R.string.no)
                )

                if (!item.mimeType.isNullOrBlank()) {
                    DetailItem(label = stringResource(id = R.string.download_details_mime_type), value = item.mimeType)
                }

                if (!item.eTag.isNullOrBlank()) {
                    DetailItem(label = "ETag", value = item.eTag)
                }

                if (!item.lastModified.isNullOrBlank()) {
                    DetailItem(label = "Last-Modified", value = item.lastModified)
                }

                DetailItem(label = stringResource(id = R.string.download_details_created_at), value = dateFormat.format(Date(item.createdAt)))
                DetailItem(label = stringResource(id = R.string.download_details_updated_at), value = dateFormat.format(Date(item.updatedAt)))

                if (!item.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.download_details_error),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("details_dialog_close_button")
            ) {
                Text(stringResource(id = R.string.close))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
