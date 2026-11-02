package com.xfiles.pro.data.repository

import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.datastore.LanguagePreferences
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of LanguageRepository backed by DataStore.
 */
class LanguageRepositoryImpl(
    private val preferences: LanguagePreferences,
    private val dispatchers: DispatchersProvider
) : LanguageRepository {

    override val selectedLanguage: Flow<AppLanguage> =
        preferences.selectedLanguage.flowOn(dispatchers.io)

    override val isFirstRunCompleted: Flow<Boolean> =
        preferences.isFirstRunCompleted.flowOn(dispatchers.io)

    override suspend fun setLanguage(language: AppLanguage) = withContext(dispatchers.io) {
        preferences.saveLanguage(language)
    }

    override suspend fun completeFirstRun(language: AppLanguage) = withContext(dispatchers.io) {
        preferences.completeFirstRun(language)
    }
}
