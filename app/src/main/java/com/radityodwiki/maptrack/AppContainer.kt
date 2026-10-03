package com.radityodwiki.maptrack

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.data.settings.SettingsRepository
import com.radityodwiki.maptrack.domain.model.trackingParams
import com.radityodwiki.maptrack.domain.usecase.TripRecorder
import com.radityodwiki.maptrack.domain.usecase.VisitBackfill
import com.radityodwiki.maptrack.location.AndroidAutoTripPermissions
import com.radityodwiki.maptrack.location.AndroidTrackingServiceLauncher
import com.radityodwiki.maptrack.location.AutoTripController
import com.radityodwiki.maptrack.location.GmsActivityTransitions
import com.radityodwiki.maptrack.location.LocationTracker
import com.radityodwiki.maptrack.location.StillnessHolder
import com.radityodwiki.maptrack.location.TrackingController
import com.radityodwiki.maptrack.location.TrackingStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

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

    val placeRepository: PlaceRepository by lazy { PlaceRepository(database) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

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
            trackingParams = { settingsRepository.current().trackingParams() },
        )
    }

    val stillnessHolder: StillnessHolder by lazy { StillnessHolder() }

    val autoTripPermissions: AndroidAutoTripPermissions by lazy { AndroidAutoTripPermissions(appContext) }

    val autoTripController: AutoTripController by lazy {
        AutoTripController(
            settings = settingsRepository,
            permissions = autoTripPermissions,
            transitions = GmsActivityTransitions(appContext),
            locationSource = locationTracker,
            tracking = trackingController,
            stillness = stillnessHolder,
        )
    }

    val visitBackfill: VisitBackfill by lazy { VisitBackfill(tripRepository, ::logBackfillFailure) }

    /** Computes visits for trips completed before Fase 2 (PRD §38 Fase 2). WorkManager only arrives in Fase 7. */
    fun startVisitBackfill(): Job = applicationScope.launch { visitBackfill.run() }

    /** Resubscribes automatic trips, or switches them off when a permission was revoked (PRD §38 Fase 5). */
    fun reconcileAutoTrip(): Job = applicationScope.launch { autoTripController.reconcile() }

    // Timber only arrives transitively via MapLibre; the app does not use it.
    @SuppressLint("LogNotTimber")
    private fun logBackfillFailure(tripId: String, error: Exception) {
        Log.w(TAG, "Visit backfill failed for trip $tripId", error)
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
