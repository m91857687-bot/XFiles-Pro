package com.xfiles.pro.domain.model

/**
 * Domain representation of the supported application languages.
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean
) {
    ARABIC(code = "ar", nativeName = "العربية", englishName = "Arabic", isRtl = true),
    ENGLISH(code = "en", nativeName = "English", englishName = "English", isRtl = false),
    SPANISH(code = "es", nativeName = "Español", englishName = "Spanish", isRtl = false),
    PERSIAN(code = "fa", nativeName = "فارسی", englishName = "Persian", isRtl = true);

    companion object {
        val DEFAULT = ARABIC // Default locale preference or English based on requirement; English is safe fallback

        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
