package com.xfiles.pro.ui.archive.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.CompressionLevel
import java.io.File

@Composable
fun CreateArchiveDialog(
    selectedFiles: List<File>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, format: ArchiveFormat, level: CompressionLevel, password: String?) -> Unit
) {
    val defaultName = if (selectedFiles.size == 1) {
        selectedFiles.first().nameWithoutExtension
    } else {
        "Archive"
    }

    var archiveName by remember { mutableStateOf(defaultName) }
    var selectedFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.NORMAL) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val creatableFormats = ArchiveFormat.entries.filter { it.supportsCreation }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Archive, contentDescription = null) },
        title = { Text(stringResource(R.string.archive_create_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text(stringResource(R.string.archive_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archive_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Format selector
                Text(
                    text = stringResource(R.string.archive_format_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    creatableFormats.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.extension.uppercase()) },
                            modifier = Modifier.testTag("format_chip_${format.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compression Level selector
                Text(
                    text = stringResource(R.string.archive_compression_level),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompressionLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level.name) },
                            modifier = Modifier.testTag("level_chip_${level.name}")
                        )
                    }
                }

                if (selectedFormat.supportsPassword) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.archive_password_optional),
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.archive_password_label)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive_password_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = archiveName.trim().ifEmpty { defaultName }
                    onConfirm(
                        finalName,
                        selectedFormat,
                        selectedLevel,
                        password.trim().ifEmpty { null }
                    )
                },
                modifier = Modifier.testTag("confirm_create_archive_button")
            ) {
                Text(stringResource(R.string.archive_create_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_archive_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
