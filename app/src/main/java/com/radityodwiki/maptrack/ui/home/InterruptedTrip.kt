package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.location.AutoTripConfig
import com.radityodwiki.maptrack.location.TrackingState
import com.radityodwiki.maptrack.location.canResumeTrip

/** A trip left `active` while no tracking service runs for it (PRD §32–§33). */
data class InterruptedTrip(val tripId: String, val startedAt: Long, val lastPointAt: Long?)

/** Resumable when its last data (or start, without points) is at most [AutoTripConfig.RESUME_MAX_GAP_MS] old. */
fun InterruptedTrip.canResume(now: Long): Boolean = canResumeTrip(lastPointAt ?: startedAt, now)

/** Returns [activeTrip] when the in-process tracking state does not own it. */
fun interruptedTripOf(activeTrip: Trip?, trackingState: TrackingState): Trip? {
    if (activeTrip == null) return null
    val owned = trackingState is TrackingState.Active && trackingState.tripId == activeTrip.id
    return if (owned) null else activeTrip
}
