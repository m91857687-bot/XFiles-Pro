package com.xfiles.pro.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.ui.components.XFilesTopAppBar
import com.xfiles.pro.ui.download.DownloadManagerScreen
import com.xfiles.pro.ui.files.FilesScreen
import com.xfiles.pro.ui.home.HomeScreen
import com.xfiles.pro.ui.media.MediaScreen
import com.xfiles.pro.ui.settings.SettingsScreen
import com.xfiles.pro.ui.tools.ToolsScreen
import kotlinx.coroutines.launch

/**
 * Main application UI shell for XFiles Pro.
 * Houses the adaptive navigation (NavigationBar for mobile, NavigationRail for tablets/wide screens),
 * Top App Bar, and manages the backstack cleanly with BackHandler.
 */
@Composable
fun XFilesAppShell(
    currentLanguage: AppLanguage,
    currentTheme: ThemeMode,
    onChangeLanguageClick: () -> Unit,
    onChangeThemeClick: () -> Unit,
    initialDestination: NavDestination = NavDestination.HOME,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var showDownloadManager by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Android Back key handling: Return to Home if on sub-screen or close Download Manager
    BackHandler(enabled = showDownloadManager || currentDestination != NavDestination.HOME) {
        if (showDownloadManager) {
            showDownloadManager = false
        } else {
            currentDestination = NavDestination.HOME
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("app_shell_root")) {
        if (showDownloadManager) {
            DownloadManagerScreen(
                onBackClick = { showDownloadManager = false },
                onNavigateToFolder = {
                    showDownloadManager = false
                    currentDestination = NavDestination.FILES
                }
            )
        } else {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Adaptive Tablet / Desktop Layout: Navigation Rail on the side
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("app_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavDestination.entries.forEach { destination ->
                            val isSelected = destination == currentDestination
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = stringResource(id = destination.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = destination.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_rail_item_${destination.route}")
                            )
                        }
                    }

                    Scaffold(
                        topBar = {
                            XFilesTopAppBar(
                                currentDestination = currentDestination,
                                isGridView = isGridView,
                                showViewToggle = currentDestination == NavDestination.FILES,
                                onViewToggleClick = { isGridView = !isGridView },
                                onSearchClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Search engine scheduled in Phase 6")
                                    }
                                }
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.weight(1f)
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            ActiveScreenContent(
                                destination = currentDestination,
                                isGridView = isGridView,
                                currentLanguage = currentLanguage,
                                currentTheme = currentTheme,
                                onChangeLanguageClick = onChangeLanguageClick,
                                onChangeThemeClick = onChangeThemeClick,
                                onNavigate = { currentDestination = it },
                                onOpenDownloads = { showDownloadManager = true }
                            )
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout: Bottom Navigation Bar
                Scaffold(
                    topBar = {
                        XFilesTopAppBar(
                            currentDestination = currentDestination,
                            isGridView = isGridView,
                            showViewToggle = currentDestination == NavDestination.FILES,
                            onViewToggleClick = { isGridView = !isGridView },
                            onSearchClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Search engine scheduled in Phase 6")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 3.dp
                        ) {
                            NavDestination.entries.forEach { destination ->
                                val isSelected = destination == currentDestination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = stringResource(id = destination.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(id = destination.labelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_item_${destination.route}")
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        ActiveScreenContent(
                            destination = currentDestination,
                            isGridView = isGridView,
                            currentLanguage = currentLanguage,
                            currentTheme = currentTheme,
                            onChangeLanguageClick = onChangeLanguageClick,
                            onChangeThemeClick = onChangeThemeClick,
                            onNavigate = { currentDestination = it },
                            onOpenDownloads = { showDownloadManager = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveScreenContent(
    destination: NavDestination,
    isGridView: Boolean,
    currentLanguage: AppLanguage,
    currentTheme: ThemeMode,
    onChangeLanguageClick: () -> Unit,
    onChangeThemeClick: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onOpenDownloads: () -> Unit
) {
    when (destination) {
        NavDestination.HOME -> HomeScreen(onNavigate = onNavigate, onOpenDownloads = onOpenDownloads)
        NavDestination.FILES -> FilesScreen(isGridView = isGridView)
        NavDestination.MEDIA -> MediaScreen()
        NavDestination.TOOLS -> ToolsScreen(onOpenDownloads = onOpenDownloads)
        NavDestination.SETTINGS -> SettingsScreen(
            currentLanguage = currentLanguage,
            currentTheme = currentTheme,
            onChangeLanguageClick = onChangeLanguageClick,
            onChangeThemeClick = onChangeThemeClick
        )
    }
}
