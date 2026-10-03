package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.data.repository.ActiveTripExistsException
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.TrackingParams
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.usecase.TripRecorder

enum class StartTrackingError { PERMISSION_MISSING, LOCATION_DISABLED, TRIP_ALREADY_ACTIVE, SERVICE_START_FAILED, RESUME_EXPIRED }

/** Starts and stops [LocationTrackingService]; abstracted for tests. */
interface TrackingServiceLauncher {
    /** [params] are the trip's tracking parameters, read once at Start. */
    fun start(tripId: String, params: TrackingParams)
    fun stop()
}

/** Entry point for Start/Stop from the UI (PRD §8). */
class TrackingController(
    private val locationSource: LocationSource,
    private val repository: TripRepository,
    private val recorder: TripRecorder,
    private val stateHolder: TrackingStateHolder,
    private val launcher: TrackingServiceLauncher,
    private val trackingParams: suspend () -> TrackingParams = { TrackingParams.DEFAULT },
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** @return null when tracking started, otherwise why it could not start. */
    suspend fun start(source: TripSource = TripSource.MANUAL): StartTrackingError? {
        if (locationSource.permissionState() != LocationPermission.GRANTED) return StartTrackingError.PERMISSION_MISSING
        if (!locationSource.isLocationEnabled()) return StartTrackingError.LOCATION_DISABLED

        // Snapshot before the trip exists: later Settings changes apply from the next trip (PRD §38 Fase 4).
        val params = trackingParams()
        val trip = repository.startTrip(source).getOrElse { error ->
            if (error is ActiveTripExistsException) return StartTrackingError.TRIP_ALREADY_ACTIVE
            throw error
        }
        return try {
            launcher.start(trip.id, params)
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

    /**
     * Restarts tracking for an interrupted trip (PRD §38 Fase 5). The gap is not filled: distance
     * from the last point to the first new one is a straight line.
     */
    suspend fun resume(tripId: String): StartTrackingError? {
        if (locationSource.permissionState() != LocationPermission.GRANTED) return StartTrackingError.PERMISSION_MISSING
        if (!locationSource.isLocationEnabled()) return StartTrackingError.LOCATION_DISABLED
        val trip = repository.getTrip(tripId)
        if (trip == null || trip.status != TripStatus.ACTIVE) return StartTrackingError.RESUME_EXPIRED
        val lastDataAt = repository.getLastPoint(tripId)?.recordedAt ?: trip.startedAt
        if (!canResumeTrip(lastDataAt, clock())) return StartTrackingError.RESUME_EXPIRED

        val params = trackingParams()
        return try {
            // DEC-001: Active before the service runs, so the trip is not seen as interrupted meanwhile.
            stateHolder.state.value = TrackingState.Active(trip.id, trip.startedAt, lastFix = null)
            launcher.start(trip.id, params)
            null
        } catch (e: Exception) {
            stateHolder.state.value = TrackingState.Idle
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
