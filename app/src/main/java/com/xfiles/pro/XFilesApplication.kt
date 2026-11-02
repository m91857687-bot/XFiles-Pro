package com.xfiles.pro

import android.app.Application
import com.xfiles.pro.di.AppContainer
import com.xfiles.pro.di.DefaultAppContainer

/**
 * Custom Application class maintaining the AppContainer instance.
 */
class XFilesApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
