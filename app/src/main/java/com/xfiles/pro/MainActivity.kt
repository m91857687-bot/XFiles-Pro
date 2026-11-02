package com.xfiles.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.ui.language.LanguagePickerDialog
import com.xfiles.pro.ui.language.LanguageSelectionScreen
import com.xfiles.pro.ui.language.LanguageViewModel
import com.xfiles.pro.ui.language.LocalizedApp
import com.xfiles.pro.ui.theme.XFilesTheme

class MainActivity : ComponentActivity() {

    private val languageViewModel: LanguageViewModel by viewModels {
        val app = application as XFilesApplication
        LanguageViewModel.provideFactory(app.container.languageRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by languageViewModel.uiState.collectAsStateWithLifecycle()

            LocalizedApp(language = uiState.currentLanguage) {
                XFilesTheme {
                    when {
                        uiState.isLoading -> {
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

                        !uiState.isFirstRunCompleted -> {
                            LanguageSelectionScreen(
                                initialLanguage = uiState.currentLanguage,
                                onConfirm = { selected ->
                                    languageViewModel.onConfirmFirstRun(selected)
                                }
                            )
                        }

                        else -> {
                            MainContentScreen(
                                currentLanguage = uiState.currentLanguage,
                                onChangeLanguageClick = {
                                    languageViewModel.onOpenLanguageDialog()
                                }
                            )

                            if (uiState.isLanguageDialogVisible) {
                                LanguagePickerDialog(
                                    selectedLanguage = uiState.selectedPendingLanguage,
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContentScreen(
    currentLanguage: AppLanguage,
    onChangeLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("home_header_icon"),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.home_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("app_title_text")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.home_status_ready),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("home_status_text")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.home_status_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Information Card: Language & Direction Details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_info_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    InfoRow(
                        label = stringResource(id = R.string.label_current_language),
                        value = "${currentLanguage.nativeName} (${currentLanguage.englishName})",
                        tag = "info_language_value"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val directionText = if (currentLanguage.isRtl) {
                        stringResource(id = R.string.direction_rtl)
                    } else {
                        stringResource(id = R.string.direction_ltr)
                    }
                    InfoRow(
                        label = stringResource(id = R.string.label_layout_direction),
                        value = directionText,
                        tag = "info_direction_value"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = stringResource(id = R.string.label_first_run_status),
                        value = stringResource(id = R.string.status_completed),
                        tag = "info_status_value"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onChangeLanguageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_change_language"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.btn_change_language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(tag)
        )
    }
}
