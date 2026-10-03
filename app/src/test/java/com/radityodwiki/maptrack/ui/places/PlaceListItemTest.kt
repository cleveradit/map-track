package com.radityodwiki.maptrack.ui.places

import com.radityodwiki.maptrack.domain.model.Place
import com.radityodwiki.maptrack.domain.model.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PlaceListItemTest {

    private val utc = ZoneOffset.UTC
    private val place = Place("p", "Rumah", 0.0, 0.0, 100.0, 0, 0)

    private fun visitOn(year: Int, month: Int, day: Int): Visit {
        val start = LocalDate.of(year, month, day).atStartOfDay(utc).toInstant().toEpochMilli()
        return Visit("trip", start, start + 600_000, 0.0, 0.0, 10)
    }

    @Test
    fun countsVisitsAndUsesLatestDate() {
        val item = place.toListItem(listOf(visitOn(2026, 10, 2), visitOn(2026, 9, 30)), utc)

        assertEquals(PlaceListItem("p", "Rumah", "100 m", 2, "2 Oktober 2026"), item)
    }

    @Test
    fun withoutVisits() {
        val item = place.copy(radiusMeters = 1_000.0).toListItem(emptyList(), utc)

        assertEquals(0, item.visitCount)
        assertNull(item.lastVisitDate)
        assertEquals("1.0 km", item.radius)
    }
}
