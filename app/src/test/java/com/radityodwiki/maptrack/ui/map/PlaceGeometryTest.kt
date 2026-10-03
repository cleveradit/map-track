package com.radityodwiki.maptrack.ui.map

import com.radityodwiki.maptrack.domain.usecase.GeoDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceGeometryTest {

    @Test
    fun circleRing_isClosedAndAtRadius() {
        val ring = circleRing(-7.78, 110.36, 100.0)

        assertEquals(65, ring.size)
        assertEquals(ring.first(), ring.last())
        ring.forEach { (lng, lat) ->
            assertEquals(100.0, GeoDistance.meters(-7.78, 110.36, lat, lng), 0.5)
        }
    }

    @Test
    fun placeZoom_decreasesWithRadiusWithinBounds() {
        assertEquals(17.0, placeZoom(50.0), 1e-9)
        assertEquals(16.0, placeZoom(100.0), 1e-9)
        val far = placeZoom(1_000.0)
        assertTrue(far < 16.0 && far >= 12.0)
    }
}
