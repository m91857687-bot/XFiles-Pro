package com.xfiles.pro.domain.model

import androidx.annotation.StringRes
import com.xfiles.pro.R

/**
 * Supported visual themes in XFiles Pro.
 * 1. CLASSIC: High-contrast black and white (Default).
 * 2. MIDNIGHT: OLED deep dark surface with luminescent accents.
 * 3. ROYAL: Obsidian dark background with imperial gold touches.
 * 4. SYSTEM: Dynamic follow of Android device theme (Light/Dark + Dynamic color).
 */
enum class ThemeMode(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int
) {
    CLASSIC(
        id = "classic",
        titleRes = R.string.theme_classic,
        descriptionRes = R.string.theme_classic_desc
    ),
    MIDNIGHT(
        id = "midnight",
        titleRes = R.string.theme_midnight,
        descriptionRes = R.string.theme_midnight_desc
    ),
    ROYAL(
        id = "royal",
        titleRes = R.string.theme_royal,
        descriptionRes = R.string.theme_royal_desc
    ),
    SYSTEM(
        id = "system",
        titleRes = R.string.theme_system,
        descriptionRes = R.string.theme_system_desc
    );

    companion object {
        val DEFAULT = CLASSIC

        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

typealias AppTheme = ThemeMode
