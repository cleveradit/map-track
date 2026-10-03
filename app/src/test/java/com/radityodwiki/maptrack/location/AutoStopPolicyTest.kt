package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.usecase.DEG_PER_METER
import com.radityodwiki.maptrack.domain.usecase.testPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoStopPolicyTest {

    private val minute = 60_000L
    private val now = 20 * minute

    /** One point every 30 s over the last five minutes, [lastNorthMeters] north for the final point. */
    private fun lastFiveMinutes(lastNorthMeters: Double = 0.0) = (0..10).map { i ->
        val north = if (i == 10) lastNorthMeters else 0.0
        testPoint(latitude = north * DEG_PER_METER, recordedAt = now - 5 * minute + i * 30_000L)
    }

    @Test
    fun stillPointsForFiveMinutesStop() {
        assertTrue(AutoStopPolicy.shouldStop(lastFiveMinutes(), tripStartedAt = 10 * minute, now = now, stillSince = null))
    }

    @Test
    fun tripShorterThanFiveMinutesKeepsGoing() {
        assertFalse(AutoStopPolicy.shouldStop(lastFiveMinutes(), tripStartedAt = now - 4 * minute, now = now, stillSince = null))
    }

    @Test
    fun pointOutsideRadiusKeepsGoing() {
        assertFalse(AutoStopPolicy.shouldStop(lastFiveMinutes(lastNorthMeters = 150.0), 10 * minute, now, null))
        assertTrue(AutoStopPolicy.shouldStop(lastFiveMinutes(lastNorthMeters = 99.0), 10 * minute, now, null))
    }

    @Test
    fun noRecentPointsWithoutStillKeepsGoing() {
        assertFalse(AutoStopPolicy.shouldStop(emptyList(), 10 * minute, now, null))
    }

    @Test
    fun activityStillForFiveMinutesStopsEvenWithoutPoints() {
        assertTrue(AutoStopPolicy.shouldStop(emptyList(), 10 * minute, now, stillSince = now - 5 * minute))
        assertFalse(AutoStopPolicy.shouldStop(emptyList(), 10 * minute, now, stillSince = now - 4 * minute))
    }
}
