package com.radityodwiki.maptrack

import android.content.Context
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.usecase.TripRecorder
import com.radityodwiki.maptrack.location.AndroidTrackingServiceLauncher
import com.radityodwiki.maptrack.location.LocationTracker
import com.radityodwiki.maptrack.location.TrackingController
import com.radityodwiki.maptrack.location.TrackingStateHolder

/**
 * Manual dependency container. Repositories and data sources are wired here
 * and exposed to ViewModels and the tracking service.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val database: MapTrackDatabase by lazy { MapTrackDatabase.create(appContext) }

    val tripRepository: TripRepository by lazy { TripRepository(database) }

    val locationTracker: LocationTracker by lazy { LocationTracker(appContext) }

    val tripRecorder: TripRecorder by lazy { TripRecorder(tripRepository) }

    val trackingStateHolder: TrackingStateHolder by lazy { TrackingStateHolder() }

    val trackingController: TrackingController by lazy {
        TrackingController(
            locationSource = locationTracker,
            repository = tripRepository,
            recorder = tripRecorder,
            stateHolder = trackingStateHolder,
            launcher = AndroidTrackingServiceLauncher(appContext),
        )
    }
}
