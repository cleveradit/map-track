package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.usecase.testPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedSeriesTest {

    @Test
    fun skipsPointsWithoutSpeedAndConvertsToKmh() {
        val points = listOf(
            testPoint(recordedAt = 1_000, speed = 10f),
            testPoint(recordedAt = 6_000, speed = null),
            testPoint(recordedAt = 11_000, speed = 12.5f),
        )

        val samples = speedSeries(points, startedAt = 0)

        assertEquals(listOf(1_000L, 11_000L), samples.map { it.offsetMs })
        assertEquals(36.0, samples[0].kmh, 0.001)
        assertEquals(45.0, samples[1].kmh, 0.001)
    }

    @Test
    fun longTripIsThinnedKeepingEnds() {
        val points = List(2_000) { testPoint(recordedAt = it * 5_000L, speed = it.toFloat()) }

        val samples = speedSeries(points, startedAt = 0, maxSamples = 500)

        assertTrue(samples.size <= 500)
        assertEquals(0L, samples.first().offsetMs)
        assertEquals(1_999 * 5_000L, samples.last().offsetMs)
    }

    @Test
    fun yAxisRoundsUpToTen() {
        assertEquals(80.0, chartMaxKmh(listOf(SpeedSample(0, 73.0))), 0.0)
        assertEquals(10.0, chartMaxKmh(emptyList()), 0.0)
    }
}
