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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
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
                if (tripId == null) stopSelf() else startTracking(tripId)
            }
            ACTION_STOP -> stopTracking()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTracking(tripId: String) {
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

            launch {
                while (isActive) {
                    refreshNotification()
                    delay(NOTIFICATION_REFRESH_MS)
                }
            }
            container.locationTracker.fixes().collect { fix ->
                stateHolder.state.value = TrackingState.Active(trip.id, trip.startedAt, fix)
                container.tripRecorder.record(trip.id, fix)
                refreshNotification()
            }
        }
    }

    private fun stopTracking() {
        if (stopping) return
        stopping = true
        val active = container.trackingStateHolder.state.value as? TrackingState.Active
        scope.launch {
            trackingJob?.cancelAndJoin()
            val tripId = active?.tripId ?: container.tripRepository.getActiveTrip()?.id
            tripId?.let { container.tripRecorder.finish(it) }
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
        )
        getSystemService(NotificationManager::class.java)
            .notify(TRACKING_NOTIFICATION_ID, TrackingNotification.build(this, text))
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

        fun startIntent(context: Context, tripId: String): Intent =
            Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TRIP_ID, tripId)

        fun stopIntent(context: Context): Intent =
            Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP)
    }
}

class AndroidTrackingServiceLauncher(context: Context) : TrackingServiceLauncher {
    private val appContext = context.applicationContext

    override fun start(tripId: String) {
        ContextCompat.startForegroundService(appContext, LocationTrackingService.startIntent(appContext, tripId))
    }

    override fun stop() {
        appContext.startService(LocationTrackingService.stopIntent(appContext))
    }
}
