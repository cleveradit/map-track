package com.radityodwiki.maptrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripStatisticsCalculatorTest {

    @Test
    fun distanceAverageAndMax() {
        val points = listOf(
            testPoint(latitude = 0.0, recordedAt = 0, speed = 1f),
            testPoint(latitude = 100 * DEG_PER_METER, recordedAt = 50_000, speed = 3.5f),
            testPoint(latitude = 200 * DEG_PER_METER, recordedAt = 100_000, speed = 2f),
        )

        val stats = TripStatisticsCalculator.calculate(points, startedAt = 0, endedAt = 100_000)

        assertEquals(200.0, stats.distanceMeters, 0.01)
        assertEquals(100_000L, stats.durationMs)
        assertEquals(2.0, stats.averageSpeedMps, 0.001)
        assertEquals(3.5, stats.maxSpeedMps!!, 0.0001)
    }

    @Test
    fun noPointsGiveZeroDistanceAndAverage() {
        val stats = TripStatisticsCalculator.calculate(emptyList(), startedAt = 0, endedAt = 60_000)

        assertEquals(0.0, stats.distanceMeters, 0.0)
        assertEquals(0.0, stats.averageSpeedMps, 0.0)
        assertNull(stats.maxSpeedMps)
    }

    @Test
    fun singlePointGivesZeroDistance() {
        val stats = TripStatisticsCalculator.calculate(listOf(testPoint(speed = 4f)), startedAt = 0, endedAt = 60_000)

        assertEquals(0.0, stats.distanceMeters, 0.0)
        assertEquals(4.0, stats.maxSpeedMps!!, 0.0)
    }

    @Test
    fun zeroDurationGivesZeroAverage() {
        val stats = TripStatisticsCalculator.calculate(listOf(testPoint()), startedAt = 5_000, endedAt = 5_000)

        assertEquals(0L, stats.durationMs)
        assertEquals(0.0, stats.averageSpeedMps, 0.0)
    }

    @Test
    fun missingSpeedGivesNullMax() {
        val points = listOf(testPoint(recordedAt = 0), testPoint(latitude = 10 * DEG_PER_METER, recordedAt = 5_000))

        assertNull(TripStatisticsCalculator.calculate(points, 0, 5_000).maxSpeedMps)
    }
}
