package com.radityodwiki.maptrack

import android.app.Application

class MapTrackApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.startVisitBackfill()
        container.reconcileAutoTrip()
    }
}
