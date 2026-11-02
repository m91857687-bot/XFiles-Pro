package com.xfiles.pro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Central design tokens holding semantic colors for XFiles Pro themes.
 * Covers: Background, Surface, Surface Variant, Primary, Secondary, Accent,
 * Text Primary, Text Secondary, Border, Divider, Error, Success, Warning.
 */
data class XFilesColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val divider: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color
)

val ClassicTokens = XFilesColorTokens(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    primary = Color(0xFF0F172A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    accent = Color(0xFF2563EB),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    border = Color(0xFFE2E8F0),
    divider = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    success = Color(0xFF16A34A),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFD97706),
    onWarning = Color(0xFFFFFFFF)
)

val MidnightTokens = XFilesColorTokens(
    background = Color(0xFF090D16),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF090D16),
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF111827),
    accent = Color(0xFF38BDF8),
    onAccent = Color(0xFF090D16),
    textPrimary = Color(0xFFF9FAFB),
    textSecondary = Color(0xFF9CA3AF),
    border = Color(0xFF374151),
    divider = Color(0xFF1F2937),
    error = Color(0xFFEF4444),
    onError = Color(0xFF111827),
    success = Color(0xFF22C55E),
    onSuccess = Color(0xFF111827),
    warning = Color(0xFFF59E0B),
    onWarning = Color(0xFF111827)
)

val RoyalTokens = XFilesColorTokens(
    background = Color(0xFF0C0B0E),
    surface = Color(0xFF17161B),
    surfaceVariant = Color(0xFF25232A),
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF121008),
    secondary = Color(0xFFC5A059),
    onSecondary = Color(0xFF121008),
    accent = Color(0xFFFFD700),
    onAccent = Color(0xFF121008),
    textPrimary = Color(0xFFFBF8F0),
    textSecondary = Color(0xFFC4B89A),
    border = Color(0xFF3D3624),
    divider = Color(0xFF2B261A),
    error = Color(0xFFE54D4D),
    onError = Color(0xFF121008),
    success = Color(0xFF48BB78),
    onSuccess = Color(0xFF121008),
    warning = Color(0xFFEAA023),
    onWarning = Color(0xFF121008)
)

fun createTokensFromColorScheme(colorScheme: ColorScheme, isDark: Boolean): XFilesColorTokens {
    return XFilesColorTokens(
        background = colorScheme.background,
        surface = colorScheme.surface,
        surfaceVariant = colorScheme.surfaceVariant,
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        accent = colorScheme.tertiary,
        onAccent = colorScheme.onTertiary,
        textPrimary = colorScheme.onSurface,
        textSecondary = colorScheme.onSurfaceVariant,
        border = colorScheme.outlineVariant,
        divider = colorScheme.outlineVariant,
        error = colorScheme.error,
        onError = colorScheme.onError,
        success = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A),
        onSuccess = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF),
        warning = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
        onWarning = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF)
    )
}

val LocalXFilesTokens = staticCompositionLocalOf { ClassicTokens }

val MaterialTheme.xFilesTokens: XFilesColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalXFilesTokens.current
