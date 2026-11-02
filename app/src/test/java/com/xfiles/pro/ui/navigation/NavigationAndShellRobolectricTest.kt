package com.xfiles.pro.ui.navigation

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.R
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.ui.theme.ClassicTokens
import com.xfiles.pro.ui.theme.MidnightTokens
import com.xfiles.pro.ui.theme.RoyalTokens
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
@Config(sdk = [36])
class NavigationAndShellRobolectricTest {

    private fun getLocalizedContext(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.code)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    @Test
    fun `navigation tab labels resolve properly in all 4 languages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        AppLanguage.entries.forEach { language ->
            val localizedContext = getLocalizedContext(context, language)

            NavDestination.entries.forEach { destination ->
                val label = localizedContext.getString(destination.labelRes)
                assertNotNull(label)
                assertFalse("Label must not be empty for ${language.code} on ${destination.route}", label.isBlank())
            }
        }
    }

    @Test
    fun `quick access items resolve properly in all 4 languages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quickAccessKeys = listOf(
            R.string.qa_internal_storage,
            R.string.qa_sd_card,
            R.string.qa_photos,
            R.string.qa_videos,
            R.string.qa_audio,
            R.string.qa_documents,
            R.string.qa_downloads,
            R.string.qa_apps
        )

        AppLanguage.entries.forEach { language ->
            val localizedContext = getLocalizedContext(context, language)

            quickAccessKeys.forEach { keyRes ->
                val text = localizedContext.getString(keyRes)
                assertNotNull(text)
                assertFalse("Quick access item must not be blank for ${language.code}", text.isBlank())
            }
        }
    }

    @Test
    fun `media and tools items resolve properly in all 4 languages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mediaKeys = listOf(
            R.string.media_tab_photos,
            R.string.media_tab_videos,
            R.string.media_tab_audio
        )
        val toolKeys = listOf(
            R.string.tool_storage_analyzer,
            R.string.tool_duplicate_cleaner,
            R.string.tool_recycle_bin,
            R.string.tool_wifi_sharing,
            R.string.tool_vault
        )

        AppLanguage.entries.forEach { language ->
            val localizedContext = getLocalizedContext(context, language)

            mediaKeys.forEach { key ->
                assertFalse(localizedContext.getString(key).isBlank())
            }
            toolKeys.forEach { key ->
                assertFalse(localizedContext.getString(key).isBlank())
            }
        }
    }

    @Test
    fun `arabic navigation labels match official Arabic translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val arabicContext = getLocalizedContext(context, AppLanguage.ARABIC)

        assertEquals("الرئيسية", arabicContext.getString(R.string.nav_home))
        assertEquals("الملفات", arabicContext.getString(R.string.nav_files))
        assertEquals("الوسائط", arabicContext.getString(R.string.nav_media))
        assertEquals("الأدوات", arabicContext.getString(R.string.nav_tools))
        assertEquals("الإعدادات", arabicContext.getString(R.string.nav_settings))
    }

    @Test
    fun `spanish navigation labels match official Spanish translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val spanishContext = getLocalizedContext(context, AppLanguage.SPANISH)

        assertEquals("Inicio", spanishContext.getString(R.string.nav_home))
        assertEquals("Archivos", spanishContext.getString(R.string.nav_files))
        assertEquals("Medios", spanishContext.getString(R.string.nav_media))
        assertEquals("Herramientas", spanishContext.getString(R.string.nav_tools))
        assertEquals("Ajustes", spanishContext.getString(R.string.nav_settings))
    }

    @Test
    fun `persian navigation labels match official Persian translations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val persianContext = getLocalizedContext(context, AppLanguage.PERSIAN)

        assertEquals("خانه", persianContext.getString(R.string.nav_home))
        assertEquals("فایل‌ها", persianContext.getString(R.string.nav_files))
        assertEquals("رسانه", persianContext.getString(R.string.nav_media))
        assertEquals("ابزارها", persianContext.getString(R.string.nav_tools))
        assertEquals("تنظیمات", persianContext.getString(R.string.nav_settings))
    }

    @Test
    fun `all 16 language and theme combinations are valid across navigation shell`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        var totalChecked = 0
        AppLanguage.entries.forEach { language ->
            val localizedContext = getLocalizedContext(context, language)

            ThemeMode.entries.forEach { theme ->
                // Verify tokens exist
                val tokens = when (theme) {
                    ThemeMode.CLASSIC -> ClassicTokens
                    ThemeMode.MIDNIGHT -> MidnightTokens
                    ThemeMode.ROYAL -> RoyalTokens
                    ThemeMode.SYSTEM -> ClassicTokens
                }
                assertNotNull(tokens.background)
                assertNotNull(tokens.primary)

                // Verify App Title and Nav Title
                val appTitle = localizedContext.getString(R.string.app_name)
                val homeTitle = localizedContext.getString(R.string.nav_home)
                assertFalse(appTitle.isBlank())
                assertFalse(homeTitle.isBlank())

                totalChecked++
            }
        }
        assertEquals(16, totalChecked)
    }
}
