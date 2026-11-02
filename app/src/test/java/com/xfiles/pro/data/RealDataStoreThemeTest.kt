package com.xfiles.pro.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.datastore.LanguagePreferences
import com.xfiles.pro.data.local.datastore.ThemePreferences
import com.xfiles.pro.data.repository.LanguageRepositoryImpl
import com.xfiles.pro.data.repository.ThemeRepositoryImpl
import androidx.datastore.preferences.core.edit
import com.xfiles.pro.data.local.datastore.dataStore
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RealDataStoreThemeTest {

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences
    private lateinit var themeRepository: ThemeRepositoryImpl
    private lateinit var languagePreferences: LanguagePreferences
    private lateinit var languageRepository: LanguageRepositoryImpl

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        val dispatchers = DefaultDispatchersProvider()
        themePreferences = ThemePreferences(context)
        themeRepository = ThemeRepositoryImpl(themePreferences, dispatchers)
        languagePreferences = LanguagePreferences(context)
        languageRepository = LanguageRepositoryImpl(languagePreferences, dispatchers)

        // Clear preferences before each test to ensure determinism
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `default theme in real datastore is CLASSIC`() = runTest {
        val initial = themeRepository.selectedTheme.first()
        assertEquals(ThemeMode.CLASSIC, initial)
    }

    @Test
    fun `real datastore saves and retrieves theme across instances`() = runTest {
        themeRepository.setTheme(ThemeMode.ROYAL)
        assertEquals(ThemeMode.ROYAL, themeRepository.selectedTheme.first())

        // Recreate repository from the same context to simulate process relaunch
        val newPrefs = ThemePreferences(context)
        val newRepo = ThemeRepositoryImpl(newPrefs, DefaultDispatchersProvider())
        assertEquals(ThemeMode.ROYAL, newRepo.selectedTheme.first())
    }

    @Test
    fun `real datastore dynamic switching updates subscribers across all 4 themes`() = runTest {
        themeRepository.setTheme(ThemeMode.CLASSIC)
        assertEquals(ThemeMode.CLASSIC, themeRepository.selectedTheme.first())

        themeRepository.setTheme(ThemeMode.MIDNIGHT)
        assertEquals(ThemeMode.MIDNIGHT, themeRepository.selectedTheme.first())

        themeRepository.setTheme(ThemeMode.ROYAL)
        assertEquals(ThemeMode.ROYAL, themeRepository.selectedTheme.first())

        themeRepository.setTheme(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, themeRepository.selectedTheme.first())
    }

    @Test
    fun `theme and language preferences coexist in shared datastore without interference`() = runTest {
        // Set language to Persian and theme to Royal
        languageRepository.setLanguage(AppLanguage.PERSIAN)
        themeRepository.setTheme(ThemeMode.ROYAL)

        assertEquals(AppLanguage.PERSIAN, languageRepository.selectedLanguage.first())
        assertEquals(ThemeMode.ROYAL, themeRepository.selectedTheme.first())

        // Changing theme must not reset or affect language
        themeRepository.setTheme(ThemeMode.MIDNIGHT)
        assertEquals(ThemeMode.MIDNIGHT, themeRepository.selectedTheme.first())
        assertEquals(AppLanguage.PERSIAN, languageRepository.selectedLanguage.first())

        // Changing language must not reset or affect theme
        languageRepository.setLanguage(AppLanguage.SPANISH)
        assertEquals(AppLanguage.SPANISH, languageRepository.selectedLanguage.first())
        assertEquals(ThemeMode.MIDNIGHT, themeRepository.selectedTheme.first())
    }
}
