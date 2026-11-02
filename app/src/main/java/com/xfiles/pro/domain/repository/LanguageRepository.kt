package com.xfiles.pro.domain.repository

import com.xfiles.pro.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for language preferences and first-run completion tracking.
 */
interface LanguageRepository {
    val selectedLanguage: Flow<AppLanguage>
    val isFirstRunCompleted: Flow<Boolean>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun completeFirstRun(language: AppLanguage)
}
