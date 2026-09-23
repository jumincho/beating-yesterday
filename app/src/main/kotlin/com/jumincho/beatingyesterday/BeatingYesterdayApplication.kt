package com.jumincho.beatingyesterday

import android.app.Application
import com.jumincho.beatingyesterday.di.AppContainer

/** Owns the [AppContainer] for the lifetime of the process. */
class BeatingYesterdayApplication : Application() {

    /** App-wide dependencies. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
