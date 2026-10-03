package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.location.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterruptedTripTest {

    private val trip = Trip("a", 0, null, null, null, null, TripStatus.ACTIVE, 0)

    @Test
    fun activeTripWithoutServiceIsInterrupted() {
        assertEquals(trip, interruptedTripOf(trip, TrackingState.Idle))
    }

    @Test
    fun tripOwnedByServiceIsNotInterrupted() {
        assertNull(interruptedTripOf(trip, TrackingState.Active("a", 0, null)))
    }

    @Test
    fun serviceTrackingAnotherTripDoesNotOwnIt() {
        assertEquals(trip, interruptedTripOf(trip, TrackingState.Active("b", 0, null)))
    }

    @Test
    fun noActiveTrip() {
        assertNull(interruptedTripOf(null, TrackingState.Idle))
    }
}
