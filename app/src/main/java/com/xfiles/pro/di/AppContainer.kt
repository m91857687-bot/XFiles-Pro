package com.xfiles.pro.di

import android.content.Context
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.database.XFilesDatabase
import com.xfiles.pro.data.local.datastore.LanguagePreferences
import com.xfiles.pro.data.repository.LanguageRepositoryImpl
import com.xfiles.pro.data.repository.StorageRepositoryImpl
import com.xfiles.pro.domain.repository.LanguageRepository
import com.xfiles.pro.domain.repository.StorageRepository

/**
 * Dependency injection container for application-level singletons.
 */
interface AppContainer {
    val dispatchersProvider: DispatchersProvider
    val database: XFilesDatabase
    val storageRepository: StorageRepository
    val languagePreferences: LanguagePreferences
    val languageRepository: LanguageRepository
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

    override val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(context, dispatchersProvider)
    }

    override val languagePreferences: LanguagePreferences by lazy {
        LanguagePreferences(context)
    }

    override val languageRepository: LanguageRepository by lazy {
        LanguageRepositoryImpl(languagePreferences, dispatchersProvider)
    }
}
