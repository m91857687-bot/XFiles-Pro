package com.xfiles.pro.ui.files.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.DateFilterRange
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.SearchFilterCriteria
import com.xfiles.pro.domain.model.SearchTarget
import com.xfiles.pro.domain.model.SizeFilterRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchSheet(
    criteria: SearchFilterCriteria,
    currentFolderPath: String,
    onCategorySelected: (FileType?) -> Unit,
    onSizeSelected: (SizeFilterRange) -> Unit,
    onDateSelected: (DateFilterRange) -> Unit,
    onTargetSelected: (SearchTarget) -> Unit,
    onScopeSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("advanced_search_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("filter_sheet_close")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Target Type (Both / Files Only / Folders Only)
            Text(
                text = stringResource(R.string.filter_target),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = criteria.searchTarget == SearchTarget.BOTH,
                    onClick = { onTargetSelected(SearchTarget.BOTH) },
                    label = { Text(stringResource(R.string.filter_target_both)) }
                )
                FilterChip(
                    selected = criteria.searchTarget == SearchTarget.FILES_ONLY,
                    onClick = { onTargetSelected(SearchTarget.FILES_ONLY) },
                    label = { Text(stringResource(R.string.filter_target_files)) }
                )
                FilterChip(
                    selected = criteria.searchTarget == SearchTarget.FOLDERS_ONLY,
                    onClick = { onTargetSelected(SearchTarget.FOLDERS_ONLY) },
                    label = { Text(stringResource(R.string.filter_target_folders)) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Search Scope (Current Folder vs All Storage)
            Text(
                text = "Search Scope",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = criteria.scopePath == null,
                    onClick = { onScopeSelected(null) },
                    label = { Text(stringResource(R.string.search_scope_all)) }
                )
                if (currentFolderPath.isNotBlank()) {
                    FilterChip(
                        selected = criteria.scopePath == currentFolderPath,
                        onClick = { onScopeSelected(currentFolderPath) },
                        label = { Text(stringResource(R.string.search_scope_current)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Category Filter
            Text(
                text = stringResource(R.string.filter_category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = criteria.fileType == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") }
                )
                listOf(
                    FileType.IMAGE to "Images",
                    FileType.VIDEO to "Videos",
                    FileType.AUDIO to "Audio",
                    FileType.DOCUMENT to "Documents",
                    FileType.ARCHIVE to "Archives",
                    FileType.APK to "APKs"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = criteria.fileType == type,
                        onClick = { onCategorySelected(if (criteria.fileType == type) null else type) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Size Filter Range
            Text(
                text = stringResource(R.string.filter_size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SizeFilterRange.ALL to stringResource(R.string.size_any),
                    SizeFilterRange.TINY_UNDER_1MB to stringResource(R.string.size_tiny),
                    SizeFilterRange.MEDIUM_1_50MB to stringResource(R.string.size_medium),
                    SizeFilterRange.LARGE_50_500MB to stringResource(R.string.size_large),
                    SizeFilterRange.HUGE_OVER_500MB to stringResource(R.string.size_huge)
                ).forEach { (range, label) ->
                    FilterChip(
                        selected = criteria.sizeRange == range,
                        onClick = { onSizeSelected(range) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Date Modified Filter Range
            Text(
                text = stringResource(R.string.filter_date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DateFilterRange.ALL to stringResource(R.string.date_any),
                    DateFilterRange.TODAY to stringResource(R.string.date_today),
                    DateFilterRange.THIS_WEEK to stringResource(R.string.date_week),
                    DateFilterRange.THIS_MONTH to stringResource(R.string.date_month),
                    DateFilterRange.THIS_YEAR to stringResource(R.string.date_year)
                ).forEach { (range, label) ->
                    FilterChip(
                        selected = criteria.dateRange == range,
                        onClick = { onDateSelected(range) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Reset / Apply)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearFilters,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_clear_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = null)
                    Text(text = stringResource(R.string.filter_clear_all), modifier = Modifier.padding(start = 6.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_apply_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = stringResource(R.string.filter_apply))
                }
            }
        }
    }
}
