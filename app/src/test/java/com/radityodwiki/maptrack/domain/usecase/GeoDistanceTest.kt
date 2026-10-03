package com.radityodwiki.maptrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun oneDegreeLatitudeAtEquator() {
        assertEquals(111_195.0, GeoDistance.meters(0.0, 0.0, 1.0, 0.0), 1.0)
    }

    @Test
    fun samePointIsZero() {
        assertEquals(0.0, GeoDistance.meters(-7.78, 110.36, -7.78, 110.36), 0.0)
    }
}
