package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.location.TrackingState

/** A trip left `active` while no tracking service runs for it (PRD §32–§33). */
data class InterruptedTrip(val tripId: String, val startedAt: Long, val lastPointAt: Long?)

/** Returns [activeTrip] when the in-process tracking state does not own it. */
fun interruptedTripOf(activeTrip: Trip?, trackingState: TrackingState): Trip? {
    if (activeTrip == null) return null
    val owned = trackingState is TrackingState.Active && trackingState.tripId == activeTrip.id
    return if (owned) null else activeTrip
}
