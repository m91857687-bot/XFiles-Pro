package com.xfiles.pro.ui.archive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.FileConflictResolution
import java.io.File

@Composable
fun ExtractArchiveDialog(
    archiveFile: File,
    defaultDestinationDir: File,
    hasSelectedEntries: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (destinationDir: File, conflictResolution: FileConflictResolution, selectedOnly: Boolean) -> Unit
) {
    var destinationPath by remember { mutableStateOf(defaultDestinationDir.absolutePath) }
    var conflictResolution by remember { mutableStateOf(FileConflictResolution.AUTO_RENAME) }
    var selectedOnly by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Unarchive, contentDescription = null) },
        title = { Text(stringResource(R.string.archive_extract_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${stringResource(R.string.archive_file_label)}: ${archiveFile.name}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = destinationPath,
                    onValueChange = { destinationPath = it },
                    label = { Text(stringResource(R.string.archive_extract_destination)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extract_destination_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.conflict_resolution_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = conflictResolution == FileConflictResolution.AUTO_RENAME,
                        onClick = { conflictResolution = FileConflictResolution.AUTO_RENAME },
                        label = { Text(stringResource(R.string.conflict_auto_rename)) },
                        modifier = Modifier.testTag("conflict_auto_rename_chip")
                    )
                    FilterChip(
                        selected = conflictResolution == FileConflictResolution.OVERWRITE,
                        onClick = { conflictResolution = FileConflictResolution.OVERWRITE },
                        label = { Text(stringResource(R.string.conflict_overwrite)) },
                        modifier = Modifier.testTag("conflict_overwrite_chip")
                    )
                    FilterChip(
                        selected = conflictResolution == FileConflictResolution.SKIP,
                        onClick = { conflictResolution = FileConflictResolution.SKIP },
                        label = { Text(stringResource(R.string.conflict_skip)) },
                        modifier = Modifier.testTag("conflict_skip_chip")
                    )
                }

                if (hasSelectedEntries) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !selectedOnly,
                            onClick = { selectedOnly = false },
                            label = { Text(stringResource(R.string.archive_extract_all)) },
                            modifier = Modifier.testTag("extract_all_chip")
                        )
                        FilterChip(
                            selected = selectedOnly,
                            onClick = { selectedOnly = true },
                            label = { Text(stringResource(R.string.archive_extract_selected)) },
                            modifier = Modifier.testTag("extract_selected_chip")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val destDir = File(destinationPath.trim().ifEmpty { defaultDestinationDir.absolutePath })
                    onConfirm(destDir, conflictResolution, selectedOnly)
                },
                modifier = Modifier.testTag("confirm_extract_button")
            ) {
                Text(stringResource(R.string.archive_extract_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_extract_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
