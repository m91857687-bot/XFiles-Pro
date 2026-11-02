package com.xfiles.pro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.xfiles.pro.domain.model.ThemeMode

/**
 * Main theme provider for XFiles Pro.
 * Dynamically switches ColorScheme and XFilesColorTokens based on ThemeMode without restarting the Activity.
 */
@Composable
fun XFilesTheme(
    themeMode: ThemeMode = ThemeMode.CLASSIC,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val (colorScheme: ColorScheme, tokens: XFilesColorTokens) = when (themeMode) {
        ThemeMode.CLASSIC -> {
            ClassicColorScheme to ClassicTokens
        }
        ThemeMode.MIDNIGHT -> {
            MidnightColorScheme to MidnightTokens
        }
        ThemeMode.ROYAL -> {
            RoyalColorScheme to RoyalTokens
        }
        ThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scheme = if (systemInDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
                val dynamicTokens = createTokensFromColorScheme(scheme, systemInDark)
                scheme to dynamicTokens
            } else {
                if (systemInDark) {
                    MidnightColorScheme to MidnightTokens
                } else {
                    ClassicColorScheme to ClassicTokens
                }
            }
        }
    }

    CompositionLocalProvider(LocalXFilesTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
