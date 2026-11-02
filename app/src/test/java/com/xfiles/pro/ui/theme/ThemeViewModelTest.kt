package com.xfiles.pro.ui.theme

import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeThemeRepository
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeThemeRepository(ThemeMode.CLASSIC)
        viewModel = ThemeViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState has CLASSIC theme and dialog is hidden`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.CLASSIC, state.currentTheme)
        assertEquals(ThemeMode.CLASSIC, state.pendingSelectedTheme)
        assertFalse(state.isThemeDialogVisible)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onOpenThemeDialog reveals dialog with current theme as pending`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onOpenThemeDialog()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isThemeDialogVisible)
        assertEquals(ThemeMode.CLASSIC, state.pendingSelectedTheme)
    }

    @Test
    fun `onSelectPendingTheme updates pending selection without modifying current theme`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onOpenThemeDialog()
        testScheduler.advanceUntilIdle()

        viewModel.onSelectPendingTheme(ThemeMode.ROYAL)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isThemeDialogVisible)
        assertEquals(ThemeMode.ROYAL, state.pendingSelectedTheme)
        assertEquals(ThemeMode.CLASSIC, state.currentTheme)
    }

    @Test
    fun `onApplyTheme persists pending theme and dismisses dialog`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onOpenThemeDialog()
        testScheduler.advanceUntilIdle()

        viewModel.onSelectPendingTheme(ThemeMode.MIDNIGHT)
        testScheduler.advanceUntilIdle()

        viewModel.onApplyTheme()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isThemeDialogVisible)
        assertEquals(ThemeMode.MIDNIGHT, state.currentTheme)
        assertEquals(ThemeMode.MIDNIGHT, state.pendingSelectedTheme)
    }

    @Test
    fun `onCloseThemeDialog dismisses dialog without persisting pending theme`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onOpenThemeDialog()
        testScheduler.advanceUntilIdle()

        viewModel.onSelectPendingTheme(ThemeMode.ROYAL)
        testScheduler.advanceUntilIdle()

        viewModel.onCloseThemeDialog()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isThemeDialogVisible)
        assertEquals(ThemeMode.CLASSIC, state.currentTheme)
    }

    @Test
    fun `onSetThemeDirectly immediately switches theme`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onSetThemeDirectly(ThemeMode.SYSTEM)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.currentTheme)
    }

    private class FakeThemeRepository(initial: ThemeMode) : ThemeRepository {
        private val _flow = MutableStateFlow(initial)
        override val selectedTheme: Flow<ThemeMode> = _flow.asStateFlow()

        override suspend fun setTheme(theme: ThemeMode) {
            _flow.value = theme
        }
    }
}
