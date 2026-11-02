package com.xfiles.pro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Classic Theme Colors (Clean Light / Black & White)
val ClassicPrimary = Color(0xFF0F172A)
val ClassicOnPrimary = Color(0xFFFFFFFF)
val ClassicPrimaryContainer = Color(0xFFF1F5F9)
val ClassicOnPrimaryContainer = Color(0xFF0F172A)
val ClassicSecondary = Color(0xFF475569)
val ClassicOnSecondary = Color(0xFFFFFFFF)
val ClassicSecondaryContainer = Color(0xFFE2E8F0)
val ClassicOnSecondaryContainer = Color(0xFF0F172A)
val ClassicTertiary = Color(0xFF2563EB)
val ClassicOnTertiary = Color(0xFFFFFFFF)
val ClassicBackground = Color(0xFFFFFFFF)
val ClassicOnBackground = Color(0xFF0F172A)
val ClassicSurface = Color(0xFFFFFFFF)
val ClassicOnSurface = Color(0xFF0F172A)
val ClassicSurfaceVariant = Color(0xFFF1F5F9)
val ClassicOnSurfaceVariant = Color(0xFF475569)
val ClassicOutline = Color(0xFFCBD5E1)
val ClassicOutlineVariant = Color(0xFFE2E8F0)
val ClassicError = Color(0xFFDC2626)
val ClassicOnError = Color(0xFFFFFFFF)

// Midnight Theme Colors (Deep Dark / OLED Friendly)
val MidnightPrimary = Color(0xFF60A5FA)
val MidnightOnPrimary = Color(0xFF090D16)
val MidnightPrimaryContainer = Color(0xFF1E3A8A)
val MidnightOnPrimaryContainer = Color(0xFFDBEAFE)
val MidnightSecondary = Color(0xFF9CA3AF)
val MidnightOnSecondary = Color(0xFF111827)
val MidnightSecondaryContainer = Color(0xFF374151)
val MidnightOnSecondaryContainer = Color(0xFFF3F4F6)
val MidnightTertiary = Color(0xFF38BDF8)
val MidnightOnTertiary = Color(0xFF090D16)
val MidnightBackground = Color(0xFF090D16)
val MidnightOnBackground = Color(0xFFF9FAFB)
val MidnightSurface = Color(0xFF111827)
val MidnightOnSurface = Color(0xFFF9FAFB)
val MidnightSurfaceVariant = Color(0xFF1F2937)
val MidnightOnSurfaceVariant = Color(0xFF9CA3AF)
val MidnightOutline = Color(0xFF4B5563)
val MidnightOutlineVariant = Color(0xFF374151)
val MidnightError = Color(0xFFEF4444)
val MidnightOnError = Color(0xFF111827)

// Royal Theme Colors (Obsidian Velvet + Imperial Gold)
val RoyalPrimary = Color(0xFFD4AF37)
val RoyalOnPrimary = Color(0xFF121008)
val RoyalPrimaryContainer = Color(0xFF3A3010)
val RoyalOnPrimaryContainer = Color(0xFFFFECC0)
val RoyalSecondary = Color(0xFFC5A059)
val RoyalOnSecondary = Color(0xFF121008)
val RoyalSecondaryContainer = Color(0xFF2F291B)
val RoyalOnSecondaryContainer = Color(0xFFFBF8F0)
val RoyalTertiary = Color(0xFFFFD700)
val RoyalOnTertiary = Color(0xFF121008)
val RoyalBackground = Color(0xFF0C0B0E)
val RoyalOnBackground = Color(0xFFFBF8F0)
val RoyalSurface = Color(0xFF17161B)
val RoyalOnSurface = Color(0xFFFBF8F0)
val RoyalSurfaceVariant = Color(0xFF25232A)
val RoyalOnSurfaceVariant = Color(0xFFC4B89A)
val RoyalOutline = Color(0xFF4A412C)
val RoyalOutlineVariant = Color(0xFF3D3624)
val RoyalError = Color(0xFFE54D4D)
val RoyalOnError = Color(0xFF121008)

val ClassicColorScheme = lightColorScheme(
    primary = ClassicPrimary,
    onPrimary = ClassicOnPrimary,
    primaryContainer = ClassicPrimaryContainer,
    onPrimaryContainer = ClassicOnPrimaryContainer,
    secondary = ClassicSecondary,
    onSecondary = ClassicOnSecondary,
    secondaryContainer = ClassicSecondaryContainer,
    onSecondaryContainer = ClassicOnSecondaryContainer,
    tertiary = ClassicTertiary,
    onTertiary = ClassicOnTertiary,
    background = ClassicBackground,
    onBackground = ClassicOnBackground,
    surface = ClassicSurface,
    onSurface = ClassicOnSurface,
    surfaceVariant = ClassicSurfaceVariant,
    onSurfaceVariant = ClassicOnSurfaceVariant,
    outline = ClassicOutline,
    outlineVariant = ClassicOutlineVariant,
    error = ClassicError,
    onError = ClassicOnError
)

val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    onSecondary = MidnightOnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightTertiary,
    onTertiary = MidnightOnTertiary,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineVariant,
    error = MidnightError,
    onError = MidnightOnError
)

val RoyalColorScheme = darkColorScheme(
    primary = RoyalPrimary,
    onPrimary = RoyalOnPrimary,
    primaryContainer = RoyalPrimaryContainer,
    onPrimaryContainer = RoyalOnPrimaryContainer,
    secondary = RoyalSecondary,
    onSecondary = RoyalOnSecondary,
    secondaryContainer = RoyalSecondaryContainer,
    onSecondaryContainer = RoyalOnSecondaryContainer,
    tertiary = RoyalTertiary,
    onTertiary = RoyalOnTertiary,
    background = RoyalBackground,
    onBackground = RoyalOnBackground,
    surface = RoyalSurface,
    onSurface = RoyalOnSurface,
    surfaceVariant = RoyalSurfaceVariant,
    onSurfaceVariant = RoyalOnSurfaceVariant,
    outline = RoyalOutline,
    outlineVariant = RoyalOutlineVariant,
    error = RoyalError,
    onError = RoyalOnError
)
