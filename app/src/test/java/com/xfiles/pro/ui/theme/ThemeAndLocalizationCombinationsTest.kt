package com.xfiles.pro.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeAndLocalizationCombinationsTest {

    private fun getLocalizedContext(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.code)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    @Test
    fun `test all 16 combinations of 4 languages and 4 themes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val languages = AppLanguage.entries
        val themes = ThemeMode.entries

        var combinationsTested = 0

        for (language in languages) {
            val localizedContext = getLocalizedContext(context, language)

            // Verify RTL / LTR layout direction
            if (language.isRtl) {
                assertTrue(
                    "Language ${language.code} must be RTL",
                    language == AppLanguage.ARABIC || language == AppLanguage.PERSIAN
                )
            } else {
                assertFalse(
                    "Language ${language.code} must be LTR",
                    language == AppLanguage.ARABIC || language == AppLanguage.PERSIAN
                )
            }

            for (theme in themes) {
                // Verify String resolution in active language for theme title and description
                val title = localizedContext.getString(theme.titleRes)
                val description = localizedContext.getString(theme.descriptionRes)

                assertNotNull(title)
                assertFalse("Theme title must not be empty for ${language.code} + ${theme.id}", title.isBlank())
                assertNotNull(description)
                assertFalse("Theme desc must not be empty for ${language.code} + ${theme.id}", description.isBlank())

                // Verify theme dialog strings
                val dialogTitle = localizedContext.getString(R.string.dialog_theme_title)
                val dialogSubtitle = localizedContext.getString(R.string.dialog_theme_subtitle)
                val btnChangeTheme = localizedContext.getString(R.string.btn_change_theme)

                assertFalse(dialogTitle.isBlank())
                assertFalse(dialogSubtitle.isBlank())
                assertFalse(btnChangeTheme.isBlank())

                // Verify corresponding theme tokens exist and have valid colors
                val tokens = when (theme) {
                    ThemeMode.CLASSIC -> ClassicTokens
                    ThemeMode.MIDNIGHT -> MidnightTokens
                    ThemeMode.ROYAL -> RoyalTokens
                    ThemeMode.SYSTEM -> createTokensFromColorScheme(ClassicColorScheme, isDark = false)
                }
                assertNotNull(tokens)
                assertNotNull(tokens.background)
                assertNotNull(tokens.primary)

                combinationsTested++
            }
        }

        assertEquals("Exactly 16 combinations must be verified", 16, combinationsTested)
    }

    @Test
    fun `arabic strings for themes match official translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val arabicContext = getLocalizedContext(context, AppLanguage.ARABIC)

        assertTrue(arabicContext.getString(R.string.theme_classic).contains("كلاسيكي"))
        assertTrue(arabicContext.getString(R.string.theme_midnight).contains("منتصف الليل"))
        assertTrue(arabicContext.getString(R.string.theme_royal).contains("ملكي"))
        assertTrue(arabicContext.getString(R.string.theme_system).contains("النظام"))
    }

    @Test
    fun `spanish strings for themes match official translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val spanishContext = getLocalizedContext(context, AppLanguage.SPANISH)

        assertEquals("Clásico", spanishContext.getString(R.string.theme_classic))
        assertEquals("Medianoche", spanishContext.getString(R.string.theme_midnight))
        assertEquals("Real", spanishContext.getString(R.string.theme_royal))
        assertEquals("Sistema", spanishContext.getString(R.string.theme_system))
    }

    @Test
    fun `persian strings for themes match official translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val persianContext = getLocalizedContext(context, AppLanguage.PERSIAN)

        assertEquals("کلاسیک", persianContext.getString(R.string.theme_classic))
        assertEquals("نیمه‌شب", persianContext.getString(R.string.theme_midnight))
        assertEquals("شاهانه", persianContext.getString(R.string.theme_royal))
        assertEquals("سیستم", persianContext.getString(R.string.theme_system))
    }
}
