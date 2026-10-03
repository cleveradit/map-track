package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TripSummaryTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val start = ZonedDateTime.of(2026, 9, 29, 7, 32, 0, 0, jakarta).toInstant().toEpochMilli()
    private val end = start + 46 * 60_000L

    @Test
    fun completedTrip() {
        val summary = Trip("id", start, end, 21_700.0, 7.86, 20.3, TripStatus.COMPLETED, end).toSummary(jakarta)

        assertEquals("29 September 2026", summary.date)
        assertEquals("07:32", summary.startTime)
        assertEquals("08:18", summary.endTime)
        assertEquals("46 menit", summary.duration)
        assertEquals("21.7 km", summary.distance)
        assertEquals("28 km/h", summary.averageSpeed)
        assertEquals("73 km/h", summary.maxSpeed)
        assertFalse(summary.isActive)
    }

    @Test
    fun activeTripHidesFinalStatistics() {
        val summary = Trip("id", start, null, null, null, null, TripStatus.ACTIVE, start).toSummary(jakarta)

        assertEquals("07:32", summary.startTime)
        listOf(summary.endTime, summary.duration, summary.distance, summary.averageSpeed, summary.maxSpeed)
            .forEach { assertEquals("—", it) }
        assertTrue(summary.isActive)
    }

    @Test
    fun missingMaxSpeed() {
        val summary = Trip("id", start, end, 0.0, 0.0, null, TripStatus.COMPLETED, end).toSummary(jakarta)

        assertEquals("—", summary.maxSpeed)
    }
}
