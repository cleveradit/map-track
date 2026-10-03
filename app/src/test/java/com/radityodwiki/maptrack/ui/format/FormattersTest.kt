package com.radityodwiki.maptrack.ui.format

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
}
