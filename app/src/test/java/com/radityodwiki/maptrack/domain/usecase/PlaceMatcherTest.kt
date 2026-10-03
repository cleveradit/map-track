package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.Place
import com.radityodwiki.maptrack.domain.model.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceMatcherTest {

    private fun place(id: String, northMeters: Double, radius: Double = 100.0) =
        Place(id, "Place $id", northMeters * DEG_PER_METER, 0.0, radius, 0, 0)

    private fun match(northMeters: Double, places: List<Place>) =
        PlaceMatcher.match(northMeters * DEG_PER_METER, 0.0, places)

    @Test
    fun insideRadius_matches() {
        assertEquals("home", match(99.0, listOf(place("home", 0.0)))?.id)
    }

    @Test
    fun exactlyOnRadius_matches() {
        val distance = GeoDistance.meters(0.0, 0.0, 100.0 * DEG_PER_METER, 0.0)
        val home = place("home", 0.0, radius = distance)

        assertEquals("home", match(100.0, listOf(home))?.id)
    }

    @Test
    fun outsideRadius_doesNotMatch() {
        assertNull(match(101.0, listOf(place("home", 0.0))))
    }

    @Test
    fun noPlaces_doesNotMatch() {
        assertNull(match(0.0, emptyList()))
    }

    @Test
    fun overlappingPlaces_nearestCenterWins() {
        val big = place("big", 0.0, radius = 1_000.0)
        val small = place("small", 300.0, radius = 100.0)

        assertEquals("small", match(250.0, listOf(big, small))?.id)
        assertEquals("big", match(100.0, listOf(big, small))?.id)
    }

    @Test
    fun equalDistance_smallestIdWins() {
        // Same center, different ids.
        val b = place("b", 0.0)
        val a = place("a", 0.0)

        assertEquals("a", match(50.0, listOf(b, a))?.id)
    }

    private fun visit(northMeters: Double, arrivedAt: Long) =
        Visit("trip", arrivedAt, arrivedAt + 600_000, northMeters * DEG_PER_METER, 0.0, 10)

    @Test
    fun group_putsEachVisitUnderItsNearestPlace() {
        val a = place("a", 0.0, radius = 500.0)
        val b = place("b", 400.0, radius = 100.0)
        val atA1 = visit(0.0, 1)
        val atA2 = visit(10.0, 2)
        val nearerB = visit(350.0, 3)
        val outside = visit(5_000.0, 4)

        val groups = PlaceMatcher.group(listOf(atA1, atA2, nearerB, outside), listOf(a, b))

        assertEquals(listOf(atA1, atA2), groups["a"])
        assertEquals(listOf(nearerB), groups["b"])
        assertEquals(setOf("a", "b"), groups.keys)
    }
}
