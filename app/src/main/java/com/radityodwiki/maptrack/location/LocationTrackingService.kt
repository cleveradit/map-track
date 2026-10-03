package com.radityodwiki.maptrack.location

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.TrackingParams
import com.radityodwiki.maptrack.domain.model.TripSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NOTIFICATION_REFRESH_MS = 5_000L

/**
 * Foreground service that records the active trip (PRD §9). Keeps running when the Activity
 * is gone; not restarted by the system after process death (PRD §32).
 */
class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackingJob: Job? = null
    private var stopping = false

    /** Display unit follows Settings live; tracking parameters do not (they are the trip's snapshot). */
    private var unit = DistanceUnit.METRIC
    private var autoTrip = false

    private val container get() = (application as MapTrackApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TrackingNotification.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
                val params = TrackingParams(
                    intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, TrackingParams.DEFAULT.intervalMs),
                    maxAccuracyMeters = intent.getFloatExtra(EXTRA_MAX_ACCURACY_M, TrackingParams.DEFAULT.maxAccuracyMeters),
                )
                if (tripId == null) stopSelf() else startTracking(tripId, params)
            }
            ACTION_STOP -> stopTracking()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startTracking(tripId: String, params: TrackingParams) {
        // Must be called promptly after startForegroundService().
        ServiceCompat.startForeground(
            this,
            TRACKING_NOTIFICATION_ID,
            TrackingNotification.build(this, trackingNotificationText(null, System.currentTimeMillis(), System.currentTimeMillis(), true)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        if (trackingJob != null) return

        val stateHolder = container.trackingStateHolder
        trackingJob = scope.launch {
            val trip = container.tripRepository.getTrip(tripId)
            if (trip == null) {
                stopSelfCompletely()
                return@launch
            }
            stateHolder.state.value = TrackingState.Active(trip.id, trip.startedAt, lastFix = null)
            autoTrip = trip.source == TripSource.AUTO

            launch { container.settingsRepository.settings.collect { unit = it.distanceUnit } }
            // Only automatic trips stop by themselves; manual trips never do (PRD §38 Fase 5).
            if (autoTrip) {
                launch {
                    while (isActive) {
                        delay(AutoTripConfig.AUTO_STOP_CHECK_MS)
                        val now = System.currentTimeMillis()
                        val recent = container.tripRepository.getPointsSince(trip.id, now - AutoTripConfig.AUTO_STOP_STILL_MS)
                        if (AutoStopPolicy.shouldStop(recent, trip.startedAt, now, container.stillnessHolder.stillSince)) {
                            stopTracking(endAtLastPoint = true)
                        }
                    }
                }
            }
            launch {
                while (isActive) {
                    refreshNotification()
                    delay(NOTIFICATION_REFRESH_MS)
                }
            }
            // Battery saver (PRD §38 Fase 5): a slower, balanced request while the user is still.
            val normal = LocationRequestSpec(params.intervalMs, highAccuracy = true)
            val stationary = LocationRequestSpec(AutoTripConfig.STATIONARY_INTERVAL_MS, highAccuracy = false)
            val detector = StationaryDetector()
            val request = MutableStateFlow(normal)
            request.flatMapLatest { container.locationTracker.fixes(it) }.collect { fix ->
                stateHolder.state.value = TrackingState.Active(trip.id, trip.startedAt, fix)
                container.tripRecorder.record(trip.id, fix, params.maxAccuracyMeters)
                request.value = if (detector.onFix(fix)) stationary else normal
                refreshNotification()
            }
        }
    }

    /** @param endAtLastPoint true for an automatic stop: the trip ends at its last point, not now. */
    private fun stopTracking(endAtLastPoint: Boolean = false) {
        if (stopping) return
        stopping = true
        val active = container.trackingStateHolder.state.value as? TrackingState.Active
        scope.launch {
            trackingJob?.cancelAndJoin()
            val tripId = active?.tripId ?: container.tripRepository.getActiveTrip()?.id
            tripId?.let {
                if (endAtLastPoint) container.tripRecorder.finishInterrupted(it) else container.tripRecorder.finish(it)
            }
            stopSelfCompletely()
        }
    }

    private fun refreshNotification() {
        val active = container.trackingStateHolder.state.value as? TrackingState.Active ?: return
        val text = trackingNotificationText(
            lastFix = active.lastFix,
            startedAt = active.startedAt,
            now = System.currentTimeMillis(),
            locationEnabled = container.locationTracker.isLocationEnabled(),
            unit = unit,
        )
        getSystemService(NotificationManager::class.java)
            .notify(TRACKING_NOTIFICATION_ID, TrackingNotification.build(this, text, autoTrip))
    }

    private fun stopSelfCompletely() {
        container.trackingStateHolder.state.value = TrackingState.Idle
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        container.trackingStateHolder.state.value = TrackingState.Idle
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.radityodwiki.maptrack.action.START_TRACKING"
        const val ACTION_STOP = "com.radityodwiki.maptrack.action.STOP_TRACKING"
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val EXTRA_MAX_ACCURACY_M = "max_accuracy_m"

        fun startIntent(context: Context, tripId: String, params: TrackingParams): Intent =
            Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TRIP_ID, tripId)
                .putExtra(EXTRA_INTERVAL_MS, params.intervalMs)
                .putExtra(EXTRA_MAX_ACCURACY_M, params.maxAccuracyMeters)

        fun stopIntent(context: Context): Intent =
            Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP)
    }
}

class AndroidTrackingServiceLauncher(context: Context) : TrackingServiceLauncher {
    private val appContext = context.applicationContext

    override fun start(tripId: String, params: TrackingParams) {
        ContextCompat.startForegroundService(appContext, LocationTrackingService.startIntent(appContext, tripId, params))
    }

    override fun stop() {
        appContext.startService(LocationTrackingService.stopIntent(appContext))
    }
}
