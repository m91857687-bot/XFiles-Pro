package com.xfiles.pro.ui.language

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.xfiles.pro.domain.model.AppLanguage
import java.util.Locale

/**
 * Higher-order Composable that injects localized Context, Configuration,
 * and LayoutDirection into the Compose tree dynamically.
 */
@Composable
fun LocalizedApp(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val systemConfig = LocalConfiguration.current

    val (localizedContext, localizedConfig) = remember(language, currentContext, systemConfig) {
        val targetLocale = Locale.forLanguageTag(language.code)
        Locale.setDefault(targetLocale)

        val newConfig = Configuration(systemConfig).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
        val wrappedContext = currentContext.createConfigurationContext(newConfig)
        Pair(wrappedContext, newConfig)
    }

    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
