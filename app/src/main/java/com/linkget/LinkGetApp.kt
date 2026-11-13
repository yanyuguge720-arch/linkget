package com.linkget

import android.app.Application
import com.linkget.di.AppContainer

class LinkGetApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
