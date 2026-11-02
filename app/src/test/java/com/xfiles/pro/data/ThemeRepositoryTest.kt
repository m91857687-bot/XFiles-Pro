package com.xfiles.pro.data

import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemeRepositoryTest {

    private class FakeThemeRepository : ThemeRepository {
        private val _selectedTheme = MutableStateFlow(ThemeMode.CLASSIC)

        override val selectedTheme: Flow<ThemeMode> = _selectedTheme

        override suspend fun setTheme(theme: ThemeMode) {
            _selectedTheme.value = theme
        }
    }

    private lateinit var repository: FakeThemeRepository

    @Before
    fun setup() {
        repository = FakeThemeRepository()
    }

    @Test
    fun `initial state defaults to CLASSIC theme`() = runTest {
        assertEquals(ThemeMode.CLASSIC, repository.selectedTheme.first())
    }

    @Test
    fun `setTheme updates selected theme to MIDNIGHT`() = runTest {
        repository.setTheme(ThemeMode.MIDNIGHT)
        assertEquals(ThemeMode.MIDNIGHT, repository.selectedTheme.first())
    }

    @Test
    fun `theme can be switched across all 4 themes sequentially`() = runTest {
        repository.setTheme(ThemeMode.CLASSIC)
        assertEquals(ThemeMode.CLASSIC, repository.selectedTheme.first())

        repository.setTheme(ThemeMode.MIDNIGHT)
        assertEquals(ThemeMode.MIDNIGHT, repository.selectedTheme.first())

        repository.setTheme(ThemeMode.ROYAL)
        assertEquals(ThemeMode.ROYAL, repository.selectedTheme.first())

        repository.setTheme(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repository.selectedTheme.first())
    }
}
