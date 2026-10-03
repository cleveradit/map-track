package com.radityodwiki.maptrack.ui.format

import com.radityodwiki.maptrack.domain.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun currentSpeed() {
        assertEquals("45 km/h", formatCurrentSpeed(12.5f, fixTime = 1_000, now = 2_000))
        assertEquals("0 km/h", formatCurrentSpeed(0.2f, fixTime = 1_000, now = 2_000))
        assertEquals("— km/h", formatCurrentSpeed(null, fixTime = 1_000, now = 2_000))
        assertEquals("— km/h", formatCurrentSpeed(12.5f, fixTime = null, now = 2_000))
    }

    @Test
    fun staleFixBoundary() {
        assertEquals("45 km/h", formatCurrentSpeed(12.5f, fixTime = 0, now = 15_000))
        assertEquals("— km/h", formatCurrentSpeed(12.5f, fixTime = 0, now = 15_001))
    }

    @Test
    fun distance() {
        assertEquals("850 m", formatDistance(850.0))
        assertEquals("1.0 km", formatDistance(1_000.0))
        assertEquals("21.7 km", formatDistance(21_700.0))
    }

    @Test
    fun duration() {
        assertEquals("0 menit", formatDuration(59_000))
        assertEquals("46 menit", formatDuration(46 * 60_000L))
        assertEquals("1 jam 5 menit", formatDuration(65 * 60_000L))
    }

    @Test
    fun accuracy() {
        assertEquals("± 6 meter", formatAccuracy(5.6f))
        assertEquals("—", formatAccuracy(null))
    }

    @Test
    fun imperialSpeed() {
        assertEquals("28 mph", formatCurrentSpeed(12.5f, fixTime = 1_000, now = 2_000, unit = DistanceUnit.IMPERIAL))
        assertEquals("0 mph", formatSpeed(0.2, DistanceUnit.IMPERIAL))
        assertEquals("— mph", formatSpeed(null, DistanceUnit.IMPERIAL))
        assertEquals("45 km/h", formatSpeed(12.5))
    }

    @Test
    fun imperialDistance() {
        assertEquals("328 ft", formatDistance(100.0, DistanceUnit.IMPERIAL))
        assertEquals("525 ft", formatDistance(160.0, DistanceUnit.IMPERIAL))
        assertEquals("0.1 mi", formatDistance(170.0, DistanceUnit.IMPERIAL))
        assertEquals("13.5 mi", formatDistance(21_700.0, DistanceUnit.IMPERIAL))
    }

    @Test
    fun imperialAccuracy() {
        assertEquals("± 20 ft", formatAccuracy(6f, DistanceUnit.IMPERIAL))
    }

    @Test
    fun seconds() {
        assertEquals("6.94 s", formatSeconds(6_944))
        assertEquals("—", formatSeconds(null))
    }
}
