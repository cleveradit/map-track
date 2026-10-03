package com.radityodwiki.maptrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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

    /** Ten minutes still, a fix every 5 s with accuracy 10–30 m scattered up to that accuracy. */
    private fun stationaryRecording(speed: (Random) -> Float?): List<com.radityodwiki.maptrack.domain.model.LocationPoint> {
        val random = Random(42)
        return (0..120).map { i ->
            val accuracy = 10f + random.nextFloat() * 20f
            val north = (random.nextDouble() * 2 - 1) * accuracy
            val east = (random.nextDouble() * 2 - 1) * accuracy
            testPoint(
                latitude = north * DEG_PER_METER,
                longitude = east * DEG_PER_METER,
                recordedAt = i * 5_000L,
                accuracy = accuracy,
                speed = speed(random),
            )
        }
    }

    @Test
    fun stationaryJitterAddsLessThanFiftyMeters() {
        val distance = TripStatisticsCalculator.anchoredDistance(stationaryRecording { it.nextFloat() * 0.45f })

        assertTrue("distance was $distance", distance < 50.0)
    }

    @Test
    fun withoutSpeedOnlyTheAccuracyRuleApplies() {
        // Documents the limit of the accuracy rule alone (DEC-008): scattered fixes without speed still add distance.
        val distance = TripStatisticsCalculator.anchoredDistance(stationaryRecording { null })

        assertTrue("distance was $distance", distance > 50.0)
    }

    @Test
    fun stepNotExceedingAccuracyIsNotCounted() {
        val step = GeoDistance.meters(0.0, 0.0, 10 * DEG_PER_METER, 0.0)
        // Smallest float accuracy that is not below the step: the step must be strictly larger to count.
        val accuracy = step.toFloat().let { if (it < step) Math.nextUp(it) else it }
        val points = listOf(
            testPoint(accuracy = accuracy),
            testPoint(latitude = 10 * DEG_PER_METER, recordedAt = 5_000, accuracy = accuracy),
        )

        assertEquals(0.0, TripStatisticsCalculator.anchoredDistance(points), 0.0)
        assertEquals(step, TripStatisticsCalculator.anchoredDistance(points.map { it.copy(accuracyMeters = 9f) }), 1e-9)
    }

    @Test
    fun slowMovementIsStillCounted() {
        val points = (0 until 100).map { i -> testPoint(latitude = i * 7 * DEG_PER_METER, recordedAt = i * 5_000L, accuracy = 10f, speed = 1.4f) }

        val distance = TripStatisticsCalculator.anchoredDistance(points)

        assertTrue("distance was $distance", distance >= 0.98 * (99 * 7 - 7))
    }

    @Test
    fun gpsGapIsStraightLine() {
        val points = listOf(testPoint(recordedAt = 0), testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 600_000))

        assertEquals(1_000.0, TripStatisticsCalculator.anchoredDistance(points), 0.01)
    }
}
