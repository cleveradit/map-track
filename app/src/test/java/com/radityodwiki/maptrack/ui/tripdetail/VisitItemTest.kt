package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.Visit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class VisitItemTest {

    private val utc = ZoneOffset.UTC
    private val minute = 60_000L

    private fun visit(arrivedAt: Long, departedAt: Long) =
        Visit("trip", arrivedAt, departedAt, centerLatitude = -7.78, centerLongitude = 110.36, pointCount = 10)

    @Test
    fun formatsTimeRangeAndDuration() {
        val item = visit((8 * 60 + 15) * minute, (16 * 60 + 30) * minute).toVisitItem(null, utc)

        assertEquals("08:15 - 16:30", item.timeRange)
        assertEquals("8 jam 15 menit", item.duration)
        assertEquals(RoutePoint(-7.78, 110.36), item.center)
    }

    @Test
    fun minimumVisitDuration() {
        val item = visit(0, 5 * minute).toVisitItem(null, utc)

        assertEquals("00:00 - 00:05", item.timeRange)
        assertEquals("5 menit", item.duration)
    }

    @Test
    fun carriesPlaceName() {
        val plain = visit(0, 5 * minute).toVisitItem(null, utc)

        assertEquals(plain.copy(placeName = "Kantor"), visit(0, 5 * minute).toVisitItem("Kantor", utc))
        assertEquals(null, plain.placeName)
    }
}
