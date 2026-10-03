package com.radityodwiki.maptrack

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.usecase.TripRecorder
import com.radityodwiki.maptrack.domain.usecase.VisitBackfill
import com.radityodwiki.maptrack.location.AndroidTrackingServiceLauncher
import com.radityodwiki.maptrack.location.LocationTracker
import com.radityodwiki.maptrack.location.TrackingController
import com.radityodwiki.maptrack.location.TrackingStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container. Repositories and data sources are wired here
 * and exposed to ViewModels and the tracking service.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    /** Lives as long as the process; for one-off background work such as the visit backfill. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    val visitBackfill: VisitBackfill by lazy { VisitBackfill(tripRepository, ::logBackfillFailure) }

    /** Computes visits for trips completed before Fase 2 (PRD §38 Fase 2). WorkManager only arrives in Fase 7. */
    fun startVisitBackfill(): Job = applicationScope.launch { visitBackfill.run() }

    // Timber only arrives transitively via MapLibre; the app does not use it.
    @SuppressLint("LogNotTimber")
    private fun logBackfillFailure(tripId: String, error: Exception) {
        Log.w(TAG, "Visit backfill failed for trip $tripId", error)
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
