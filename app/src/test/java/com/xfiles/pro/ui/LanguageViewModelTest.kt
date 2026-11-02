package com.xfiles.pro.ui

import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.repository.LanguageRepository
import com.xfiles.pro.ui.language.LanguageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class LanguageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeLanguageRepository : LanguageRepository {
        val selectedLanguageFlow = MutableStateFlow(AppLanguage.ARABIC)
        val isFirstRunFlow = MutableStateFlow(false)

        override val selectedLanguage: Flow<AppLanguage> = selectedLanguageFlow
        override val isFirstRunCompleted: Flow<Boolean> = isFirstRunFlow

        override suspend fun setLanguage(language: AppLanguage) {
            selectedLanguageFlow.value = language
        }

        override suspend fun completeFirstRun(language: AppLanguage) {
            selectedLanguageFlow.value = language
            isFirstRunFlow.value = true
        }
    }

    private lateinit var repository: FakeLanguageRepository
    private lateinit var viewModel: LanguageViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLanguageRepository()
        viewModel = LanguageViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState reflects repository state`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isFirstRunCompleted)
        assertEquals(AppLanguage.ARABIC, state.currentLanguage)
        assertFalse(state.isLanguageDialogVisible)
    }

    @Test
    fun `confirm first run marks state completed and sets language`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onConfirmFirstRun(AppLanguage.ENGLISH)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFirstRunCompleted)
        assertEquals(AppLanguage.ENGLISH, state.currentLanguage)
    }

    @Test
    fun `dialog open, select, apply updates language dynamically`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onConfirmFirstRun(AppLanguage.ENGLISH)
        testScheduler.advanceUntilIdle()

        viewModel.onOpenLanguageDialog()
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLanguageDialogVisible)

        viewModel.onSelectLanguage(AppLanguage.PERSIAN)
        testScheduler.advanceUntilIdle()
        assertEquals(AppLanguage.PERSIAN, viewModel.uiState.value.selectedPendingLanguage)

        viewModel.onApplyDialogLanguage()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLanguageDialogVisible)
        assertEquals(AppLanguage.PERSIAN, viewModel.uiState.value.currentLanguage)
    }

    @Test
    fun `dialog dismiss leaves current language unchanged`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.advanceUntilIdle()

        viewModel.onConfirmFirstRun(AppLanguage.SPANISH)
        testScheduler.advanceUntilIdle()

        viewModel.onOpenLanguageDialog()
        testScheduler.advanceUntilIdle()
        viewModel.onSelectLanguage(AppLanguage.ARABIC)
        testScheduler.advanceUntilIdle()
        viewModel.onCloseLanguageDialog()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLanguageDialogVisible)
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.currentLanguage)
    }
}
