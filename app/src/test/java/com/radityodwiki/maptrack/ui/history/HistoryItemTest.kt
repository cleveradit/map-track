package com.radityodwiki.maptrack.ui.history

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryItemTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, jakarta).toInstant().toEpochMilli()

    private fun trip(start: Long, end: Long?, distance: Double?, status: TripStatus) =
        Trip("id", start, end, distance, null, null, status, start)

    @Test
    fun completedTrip() {
        val item = trip(at(29, 7, 32), at(29, 8, 18), 21_700.0, TripStatus.COMPLETED).toHistoryItem(jakarta)

        assertEquals("29 September 2026", item.date)
        assertEquals("07:32 - 08:18", item.timeRange)
        assertEquals("21.7 km", item.distance)
        assertEquals("46 menit", item.duration)
        assertFalse(item.isActive)
    }

    @Test
    fun activeTripHasNoStatistics() {
        val item = trip(at(29, 7, 32), null, null, TripStatus.ACTIVE).toHistoryItem(jakarta)

        assertEquals("07:32 - …", item.timeRange)
        assertNull(item.distance)
        assertNull(item.duration)
        assertTrue(item.isActive)
    }

    @Test
    fun tripOverMidnightUsesStartDate() {
        val item = trip(at(28, 23, 50), at(29, 0, 20), 5_000.0, TripStatus.COMPLETED).toHistoryItem(jakarta)

        assertEquals("28 September 2026", item.date)
        assertEquals("23:50 - 00:20", item.timeRange)
        assertEquals("30 menit", item.duration)
    }
}
