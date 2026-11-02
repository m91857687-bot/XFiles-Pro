package com.xfiles.pro.di

import android.content.Context
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.database.XFilesDatabase
import com.xfiles.pro.data.local.datastore.LanguagePreferences
import com.xfiles.pro.data.local.datastore.ThemePreferences
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.data.repository.LanguageRepositoryImpl
import com.xfiles.pro.data.repository.StorageRepositoryImpl
import com.xfiles.pro.data.repository.ThemeRepositoryImpl
import com.xfiles.pro.domain.repository.LanguageRepository
import com.xfiles.pro.data.repository.FileIndexingRepositoryImpl
import com.xfiles.pro.domain.repository.FileIndexingRepository
import com.xfiles.pro.domain.repository.StorageRepository
import com.xfiles.pro.domain.repository.ThemeRepository

/**
 * Dependency injection container for application-level singletons.
 */
interface AppContainer {
    val dispatchersProvider: DispatchersProvider
    val database: XFilesDatabase
    val storageRepository: StorageRepository
    val fileIndexingRepository: FileIndexingRepository
    val safManager: SafManager
    val mediaStoreManager: MediaStoreManager
    val languagePreferences: LanguagePreferences
    val languageRepository: LanguageRepository
    val themePreferences: ThemePreferences
    val themeRepository: ThemeRepository
    val tabPreferences: com.xfiles.pro.data.local.datastore.TabPreferences
    val tabRepository: com.xfiles.pro.domain.repository.TabRepository
    val searchPreferences: com.xfiles.pro.data.local.datastore.SearchPreferences
    val searchHistoryRepository: com.xfiles.pro.domain.repository.SearchHistoryRepository
    val archiveRepository: com.xfiles.pro.domain.repository.ArchiveRepository
}

/**
 * Default implementation of AppContainer.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val dispatchersProvider: DispatchersProvider by lazy {
        DefaultDispatchersProvider()
    }

    override val database: XFilesDatabase by lazy {
        XFilesDatabase.getInstance(context)
    }

    override val safManager: SafManager by lazy {
        SafManager(context)
    }

    override val mediaStoreManager: MediaStoreManager by lazy {
        MediaStoreManager(context)
    }

    override val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(context, dispatchersProvider, safManager, mediaStoreManager)
    }

    override val fileIndexingRepository: FileIndexingRepository by lazy {
        FileIndexingRepositoryImpl(
            context = context,
            dispatchers = dispatchersProvider,
            indexedFileDao = database.indexedFileDao(),
            storageRepository = storageRepository,
            mediaStoreManager = mediaStoreManager,
            safManager = safManager
        )
    }

    override val languagePreferences: LanguagePreferences by lazy {
        LanguagePreferences(context)
    }

    override val languageRepository: LanguageRepository by lazy {
        LanguageRepositoryImpl(languagePreferences, dispatchersProvider)
    }

    override val themePreferences: ThemePreferences by lazy {
        ThemePreferences(context)
    }

    override val themeRepository: ThemeRepository by lazy {
        ThemeRepositoryImpl(themePreferences, dispatchersProvider)
    }

    override val tabPreferences: com.xfiles.pro.data.local.datastore.TabPreferences by lazy {
        com.xfiles.pro.data.local.datastore.TabPreferences(context)
    }

    override val tabRepository: com.xfiles.pro.domain.repository.TabRepository by lazy {
        com.xfiles.pro.data.repository.TabRepositoryImpl(tabPreferences, dispatchersProvider)
    }

    override val searchPreferences: com.xfiles.pro.data.local.datastore.SearchPreferences by lazy {
        com.xfiles.pro.data.local.datastore.SearchPreferences(context)
    }

    override val searchHistoryRepository: com.xfiles.pro.domain.repository.SearchHistoryRepository by lazy {
        com.xfiles.pro.data.repository.SearchHistoryRepositoryImpl(searchPreferences, dispatchersProvider)
    }

    override val archiveRepository: com.xfiles.pro.domain.repository.ArchiveRepository by lazy {
        com.xfiles.pro.data.repository.ArchiveRepositoryImpl(dispatchersProvider)
    }
}
