package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.toLocationPoint
import com.radityodwiki.maptrack.location.isTooShortAutoTrip
import com.radityodwiki.maptrack.location.TrackingConfig

/**
 * Filters and stores fixes for the active trip. Each fix is compared with the last *stored*
 * point, so a rejected fix never becomes the reference for the next one (PRD §12).
 * Not thread-safe: call [record] from a single coroutine.
 */
class TripRecorder(
    private val repository: TripRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastStored: LocationPoint? = null

    /** @return null when the fix was stored, otherwise why it was rejected. */
    suspend fun record(
        tripId: String,
        fix: GpsFix,
        maxAccuracyMeters: Float = TrackingConfig.MAX_ACCURACY_METERS,
    ): RejectReason? {
        val previous = lastStored?.takeIf { it.tripId == tripId } ?: repository.getLastPoint(tripId)
        val candidate = fix.toLocationPoint(tripId)
        val reason = LocationFilter.evaluate(candidate, previous, maxAccuracyMeters)
        if (reason == null) {
            repository.addPoint(candidate)
            lastStored = candidate
        } else {
            lastStored = previous
        }
        return reason
    }

    suspend fun finish(tripId: String): Trip {
        if (lastStored?.tripId == tripId) lastStored = null
        return discardIfTooShort(repository.finishTrip(tripId, now()))
    }

    /**
     * Ends a trip whose tracking process died (PRD §33), or an automatic trip that went still
     * (PRD §38 Fase 5): the end time is the last stored fix, or the start time when nothing was recorded.
     */
    suspend fun finishInterrupted(tripId: String): Trip {
        if (lastStored?.tripId == tripId) lastStored = null
        val trip = repository.getTrip(tripId) ?: throw NoSuchElementException("Trip $tripId not found")
        val endedAt = repository.getLastPoint(tripId)?.recordedAt ?: trip.startedAt
        return discardIfTooShort(repository.finishTrip(tripId, endedAt))
    }

    /** Short automatic trips are deleted silently, whichever way they ended (PRD §38 Fase 5). */
    private suspend fun discardIfTooShort(trip: Trip): Trip {
        if (trip.isTooShortAutoTrip()) repository.deleteTrip(trip.id)
        return trip
    }
}
