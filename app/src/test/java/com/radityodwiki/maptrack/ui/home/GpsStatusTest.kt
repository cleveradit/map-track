package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsStatusTest {

    private val fix = GpsFix(-7.78, 110.36, 5f, 3f, null, null, time = 0)

    @Test
    fun missingPermissionWins() {
        listOf(LocationPermission.DENIED, LocationPermission.APPROXIMATE_ONLY, LocationPermission.NOT_REQUESTED).forEach {
            assertEquals(GpsStatus.NO_PERMISSION, gpsStatusOf(it, locationEnabled = true, fix = fix, now = 0))
        }
    }

    @Test
    fun locationServiceDisabled() {
        assertEquals(GpsStatus.LOCATION_DISABLED, gpsStatusOf(LocationPermission.GRANTED, false, fix, 0))
    }

    @Test
    fun noFixIsSearching() {
        assertEquals(GpsStatus.SEARCHING, gpsStatusOf(LocationPermission.GRANTED, true, null, 0))
    }

    @Test
    fun staleBoundary() {
        assertEquals(GpsStatus.ACTIVE, gpsStatusOf(LocationPermission.GRANTED, true, fix, now = 15_000))
        assertEquals(GpsStatus.SEARCHING, gpsStatusOf(LocationPermission.GRANTED, true, fix, now = 15_001))
    }
}
