package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.location.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private val minute = 60_000L

    @Test
    fun resumeWindowBoundary() {
        val now = 1_000 * minute
        fun lastPointAgo(minutes: Long) = InterruptedTrip("t", startedAt = 0, lastPointAt = now - minutes * minute)

        assertTrue(lastPointAgo(30).canResume(now))
        assertTrue(lastPointAgo(60).canResume(now))
        assertFalse(lastPointAgo(61).canResume(now))
        assertFalse(lastPointAgo(90).canResume(now))
    }

    @Test
    fun withoutPointsStartTimeCounts() {
        val now = 1_000 * minute

        assertTrue(InterruptedTrip("t", startedAt = now - 30 * minute, lastPointAt = null).canResume(now))
        assertFalse(InterruptedTrip("t", startedAt = now - 90 * minute, lastPointAt = null).canResume(now))
    }
}
