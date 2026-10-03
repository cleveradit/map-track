package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.data.repository.ActiveTripExistsException
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.usecase.TripRecorder

enum class StartTrackingError { PERMISSION_MISSING, LOCATION_DISABLED, TRIP_ALREADY_ACTIVE, SERVICE_START_FAILED }

/** Starts and stops [LocationTrackingService]; abstracted for tests. */
interface TrackingServiceLauncher {
    fun start(tripId: String)
    fun stop()
}

/** Entry point for Start/Stop from the UI (PRD §8). */
class TrackingController(
    private val locationSource: LocationSource,
    private val repository: TripRepository,
    private val recorder: TripRecorder,
    private val stateHolder: TrackingStateHolder,
    private val launcher: TrackingServiceLauncher,
) {

    /** @return null when tracking started, otherwise why it could not start. */
    suspend fun start(): StartTrackingError? {
        if (locationSource.permissionState() != LocationPermission.GRANTED) return StartTrackingError.PERMISSION_MISSING
        if (!locationSource.isLocationEnabled()) return StartTrackingError.LOCATION_DISABLED

        val trip = repository.startTrip().getOrElse { error ->
            if (error is ActiveTripExistsException) return StartTrackingError.TRIP_ALREADY_ACTIVE
            throw error
        }
        return try {
            launcher.start(trip.id)
            // Publish immediately so the gap before the service runs is not seen as an interrupted trip.
            stateHolder.state.value = TrackingState.Active(trip.id, trip.startedAt, lastFix = null)
            null
        } catch (e: Exception) {
            // Do not leave an active trip without a service behind.
            stateHolder.state.value = TrackingState.Idle
            recorder.finish(trip.id)
            StartTrackingError.SERVICE_START_FAILED
        }
    }

    /** Stops the running service, or ends an interrupted trip when no service is running. */
    suspend fun stop() {
        if (stateHolder.state.value is TrackingState.Active) {
            launcher.stop()
        } else {
            repository.getActiveTrip()?.let { recorder.finishInterrupted(it.id) }
        }
    }

    /** Ends a trip left active by a killed process (PRD §33). */
    suspend fun endInterruptedTrip(tripId: String) {
        recorder.finishInterrupted(tripId)
    }
}
