package com.xfiles.pro.data.repository

import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.datastore.ThemePreferences
import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of ThemeRepository backed by DataStore ThemePreferences.
 */
class ThemeRepositoryImpl(
    private val themePreferences: ThemePreferences,
    private val dispatchersProvider: DispatchersProvider = DefaultDispatchersProvider()
) : ThemeRepository {

    override val selectedTheme: Flow<ThemeMode> = themePreferences.selectedTheme
        .flowOn(dispatchersProvider.io)

    override suspend fun setTheme(theme: ThemeMode) {
        withContext(dispatchersProvider.io) {
            themePreferences.saveTheme(theme)
        }
    }
}
