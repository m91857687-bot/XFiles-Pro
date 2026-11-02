package com.xfiles.pro.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RobolectricLocalizationTest {

    private fun getLocalizedContext(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    @Test
    fun `verify english strings resolve correctly`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val localized = getLocalizedContext(baseContext, Locale.forLanguageTag("en"))

        assertEquals("XFiles Pro", localized.getString(R.string.app_name))
        assertEquals("Choose Language", localized.getString(R.string.language_select_title))
        assertEquals("Continue", localized.getString(R.string.btn_continue))
        assertEquals("Change Language", localized.getString(R.string.btn_change_language))
    }

    @Test
    fun `verify arabic strings resolve correctly with native text`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val localized = getLocalizedContext(baseContext, Locale.forLanguageTag("ar"))

        assertEquals("XFiles Pro", localized.getString(R.string.app_name))
        assertEquals("اختر لغة التطبيق", localized.getString(R.string.language_select_title))
        assertEquals("متابعة", localized.getString(R.string.btn_continue))
        assertEquals("تغيير اللغة", localized.getString(R.string.btn_change_language))
        assertEquals("العربية", localized.getString(R.string.language_ar))
    }

    @Test
    fun `verify spanish strings resolve correctly with native text`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val localized = getLocalizedContext(baseContext, Locale.forLanguageTag("es"))

        assertEquals("XFiles Pro", localized.getString(R.string.app_name))
        assertEquals("Elegir idioma", localized.getString(R.string.language_select_title))
        assertEquals("Continuar", localized.getString(R.string.btn_continue))
        assertEquals("Cambiar idioma", localized.getString(R.string.btn_change_language))
        assertEquals("Español", localized.getString(R.string.language_es))
    }

    @Test
    fun `verify persian strings resolve correctly with native text`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val localized = getLocalizedContext(baseContext, Locale.forLanguageTag("fa"))

        assertEquals("XFiles Pro", localized.getString(R.string.app_name))
        assertEquals("انتخاب زبان برنامه", localized.getString(R.string.language_select_title))
        assertEquals("ادامه", localized.getString(R.string.btn_continue))
        assertEquals("تغییر زبان", localized.getString(R.string.btn_change_language))
        assertEquals("فارسی", localized.getString(R.string.language_fa))
    }
}
