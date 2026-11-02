package com.xfiles.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xfiles.pro.ui.language.LanguagePickerDialog
import com.xfiles.pro.ui.language.LanguageSelectionScreen
import com.xfiles.pro.ui.language.LanguageViewModel
import com.xfiles.pro.ui.language.LocalizedApp
import com.xfiles.pro.ui.navigation.XFilesAppShell
import com.xfiles.pro.ui.theme.ThemePickerDialog
import com.xfiles.pro.ui.theme.ThemeViewModel
import com.xfiles.pro.ui.theme.XFilesTheme

class MainActivity : ComponentActivity() {

    private val languageViewModel: LanguageViewModel by viewModels {
        val app = application as XFilesApplication
        LanguageViewModel.provideFactory(app.container.languageRepository)
    }

    private val themeViewModel: ThemeViewModel by viewModels {
        val app = application as XFilesApplication
        ThemeViewModel.provideFactory(app.container.themeRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val languageState by languageViewModel.uiState.collectAsStateWithLifecycle()
            val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()

            LocalizedApp(language = languageState.currentLanguage) {
                XFilesTheme(themeMode = themeState.currentTheme) {
                    when {
                        languageState.isLoading || themeState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("loading_container"),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.testTag("loading_indicator")
                                )
                            }
                        }

                        !languageState.isFirstRunCompleted -> {
                            LanguageSelectionScreen(
                                initialLanguage = languageState.currentLanguage,
                                onConfirm = { selected ->
                                    languageViewModel.onConfirmFirstRun(selected)
                                }
                            )
                        }

                        else -> {
                            XFilesAppShell(
                                currentLanguage = languageState.currentLanguage,
                                currentTheme = themeState.currentTheme,
                                onChangeLanguageClick = {
                                    languageViewModel.onOpenLanguageDialog()
                                },
                                onChangeThemeClick = {
                                    themeViewModel.onOpenThemeDialog()
                                }
                            )

                            if (languageState.isLanguageDialogVisible) {
                                LanguagePickerDialog(
                                    selectedLanguage = languageState.selectedPendingLanguage,
                                    onLanguageSelected = { lang ->
                                        languageViewModel.onSelectLanguage(lang)
                                    },
                                    onApply = {
                                        languageViewModel.onApplyDialogLanguage()
                                    },
                                    onDismiss = {
                                        languageViewModel.onCloseLanguageDialog()
                                    }
                                )
                            }

                            if (themeState.isThemeDialogVisible) {
                                ThemePickerDialog(
                                    selectedTheme = themeState.pendingSelectedTheme,
                                    onThemeSelected = { theme ->
                                        themeViewModel.onSelectPendingTheme(theme)
                                    },
                                    onApply = {
                                        themeViewModel.onApplyTheme()
                                    },
                                    onDismiss = {
                                        themeViewModel.onCloseThemeDialog()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
