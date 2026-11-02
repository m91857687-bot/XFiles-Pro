package com.xfiles.pro.data

import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LanguageRepositoryTest {

    private class FakeLanguageRepository : LanguageRepository {
        private val _selectedLanguage = MutableStateFlow(AppLanguage.ARABIC)
        private val _isFirstRunCompleted = MutableStateFlow(false)

        override val selectedLanguage: Flow<AppLanguage> = _selectedLanguage
        override val isFirstRunCompleted: Flow<Boolean> = _isFirstRunCompleted

        override suspend fun setLanguage(language: AppLanguage) {
            _selectedLanguage.value = language
        }

        override suspend fun completeFirstRun(language: AppLanguage) {
            _selectedLanguage.value = language
            _isFirstRunCompleted.value = true
        }
    }

    private lateinit var repository: FakeLanguageRepository

    @Before
    fun setup() {
        repository = FakeLanguageRepository()
    }

    @Test
    fun `initial state has first run not completed`() = runTest {
        assertFalse(repository.isFirstRunCompleted.first())
        assertEquals(AppLanguage.ARABIC, repository.selectedLanguage.first())
    }

    @Test
    fun `completeFirstRun updates language and marks first run completed`() = runTest {
        repository.completeFirstRun(AppLanguage.SPANISH)
        assertTrue(repository.isFirstRunCompleted.first())
        assertEquals(AppLanguage.SPANISH, repository.selectedLanguage.first())
    }

    @Test
    fun `setLanguage updates language dynamically without resetting first run`() = runTest {
        repository.completeFirstRun(AppLanguage.ENGLISH)
        assertTrue(repository.isFirstRunCompleted.first())

        repository.setLanguage(AppLanguage.PERSIAN)
        assertEquals(AppLanguage.PERSIAN, repository.selectedLanguage.first())
        assertTrue(repository.isFirstRunCompleted.first())
    }

    @Test
    fun `language can be switched to all 4 languages sequentially`() = runTest {
        repository.setLanguage(AppLanguage.ARABIC)
        assertEquals(AppLanguage.ARABIC, repository.selectedLanguage.first())

        repository.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, repository.selectedLanguage.first())

        repository.setLanguage(AppLanguage.SPANISH)
        assertEquals(AppLanguage.SPANISH, repository.selectedLanguage.first())

        repository.setLanguage(AppLanguage.PERSIAN)
        assertEquals(AppLanguage.PERSIAN, repository.selectedLanguage.first())
    }
}
