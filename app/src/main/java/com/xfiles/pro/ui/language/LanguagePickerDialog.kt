package com.xfiles.pro.ui.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.AppLanguage

/**
 * Runtime Language Switcher Dialog.
 */
@Composable
fun LanguagePickerDialog(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("language_picker_dialog"),
        title = {
            Text(
                text = stringResource(id = R.string.dialog_language_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_language_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(lang) }
                            .padding(vertical = 10.dp)
                            .testTag("dialog_option_${lang.code}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (lang == selectedLanguage),
                            onClick = { onLanguageSelected(lang) },
                            modifier = Modifier.testTag("radio_${lang.code}")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            val subLabel = when (lang) {
                                AppLanguage.ARABIC -> stringResource(id = R.string.language_ar_sub)
                                AppLanguage.ENGLISH -> stringResource(id = R.string.language_en_sub)
                                AppLanguage.SPANISH -> stringResource(id = R.string.language_es_sub)
                                AppLanguage.PERSIAN -> stringResource(id = R.string.language_fa_sub)
                            }
                            Text(
                                text = subLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                modifier = Modifier.testTag("btn_apply_dialog")
            ) {
                Text(stringResource(id = R.string.btn_apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_dialog")
            ) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
