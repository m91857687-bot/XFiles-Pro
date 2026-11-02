package com.xfiles.pro.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xfiles.pro.R
import com.xfiles.pro.ui.navigation.NavDestination

/**
 * Top App Bar for XFiles Pro shell with branding, section indicator, search and view actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XFilesTopAppBar(
    currentDestination: NavDestination,
    isGridView: Boolean = false,
    showViewToggle: Boolean = false,
    onViewToggleClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("xfiles_top_app_bar"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.FolderSpecial,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("app_bar_brand_icon")
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("top_bar_title")
                    )
                    if (currentDestination != NavDestination.HOME) {
                        Text(
                            text = " • " + stringResource(id = currentDestination.labelRes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("top_bar_section_label")
                        )
                    }
                }
            }
        },
        actions = {
            // View Mode toggle (Active on Files destination)
            if (showViewToggle) {
                IconButton(
                    onClick = onViewToggleClick,
                    modifier = Modifier.testTag("btn_toggle_view_mode")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = stringResource(id = R.string.action_view_mode),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Search Action Hook
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("btn_top_search")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.action_search),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Overflow Options Action
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("btn_top_menu")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.action_more_options),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}
