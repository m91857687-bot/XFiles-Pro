package com.xfiles.pro.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.datastore.LanguagePreferences
import com.xfiles.pro.data.repository.LanguageRepositoryImpl
import com.xfiles.pro.domain.model.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RealDataStoreLanguageTest {

    private lateinit var context: Context
    private lateinit var preferences: LanguagePreferences
    private lateinit var repository: LanguageRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = LanguagePreferences(context)
        repository = LanguageRepositoryImpl(preferences, DefaultDispatchersProvider())
    }

    @Test
    fun `real datastore saves and retrieves language across instances`() = runTest {
        // Complete first run with Persian
        repository.completeFirstRun(AppLanguage.PERSIAN)
        assertTrue(repository.isFirstRunCompleted.first())
        assertEquals(AppLanguage.PERSIAN, repository.selectedLanguage.first())

        // Recreate repository instance from same context to simulate app relaunch
        val newPreferences = LanguagePreferences(context)
        val newRepository = LanguageRepositoryImpl(newPreferences, DefaultDispatchersProvider())

        assertTrue(newRepository.isFirstRunCompleted.first())
        assertEquals(AppLanguage.PERSIAN, newRepository.selectedLanguage.first())
    }

    @Test
    fun `real datastore dynamic switching updates all subscribers`() = runTest {
        repository.setLanguage(AppLanguage.SPANISH)
        assertEquals(AppLanguage.SPANISH, repository.selectedLanguage.first())

        repository.setLanguage(AppLanguage.ARABIC)
        assertEquals(AppLanguage.ARABIC, repository.selectedLanguage.first())

        repository.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, repository.selectedLanguage.first())
    }
}
