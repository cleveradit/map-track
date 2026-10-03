package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitDetectorTest {

    private val minute = 60_000L

    /** Points every [stepMs] from [fromMs] to [toMs] inclusive, alternating ±[jitterMeters] north of [northMeters]. */
    private fun stay(
        fromMs: Long,
        toMs: Long,
        northMeters: Double = 0.0,
        jitterMeters: Double = 0.0,
        stepMs: Long = 5_000,
    ): List<LocationPoint> = (fromMs..toMs step stepMs).mapIndexed { i, t ->
        val offset = if (i % 2 == 0) jitterMeters else -jitterMeters
        testPoint(latitude = (northMeters + offset) * DEG_PER_METER, recordedAt = t)
    }

    @Test
    fun stationaryFiveMinutes_isOneVisit() {
        val points = stay(0, 5 * minute, jitterMeters = 10.0)

        val visits = VisitDetector.detect(points)

        assertEquals(1, visits.size)
        assertEquals(61, visits[0].pointCount)
        assertEquals(0L, visits[0].arrivedAt)
        assertEquals(5 * minute, visits[0].departedAt)
        assertEquals(5 * minute, visits[0].durationMs)
        assertEquals("trip", visits[0].tripId)
    }

    @Test
    fun exactlyMinimumDuration_isVisit() {
        val visits = VisitDetector.detect(stay(0, PlaceDetectionConfig.MIN_VISIT_DURATION_MS, stepMs = 60_000))

        assertEquals(1, visits.size)
    }

    @Test
    fun justBelowMinimumDuration_isNoVisit() {
        val points = stay(0, 295_000) + testPoint(recordedAt = PlaceDetectionConfig.MIN_VISIT_DURATION_MS - 1_000)

        assertTrue(VisitDetector.detect(points).isEmpty())
    }

    @Test
    fun singleStrayPoint_doesNotSplitVisit() {
        val points = stay(0, 10 * minute) +
            testPoint(latitude = 300 * DEG_PER_METER, recordedAt = 10 * minute + 5_000) +
            stay(10 * minute + 10_000, 20 * minute + 10_000)

        val visits = VisitDetector.detect(points)

        assertEquals(1, visits.size)
        assertEquals(0L, visits[0].arrivedAt)
        assertEquals(20 * minute + 10_000, visits[0].departedAt)
        assertEquals(points.size - 1, visits[0].pointCount)
        assertEquals(0.0, visits[0].centerLatitude, 1e-12)
    }

    @Test
    fun movingThenStationary_arrivesAtFirstStationaryPoint() {
        val moving = (0 until 20).map { i -> testPoint(latitude = (i * 60.0) * DEG_PER_METER, recordedAt = i * 5_000L) }
        val stationary = stay(100_000, 100_000 + 6 * minute, northMeters = 1_340.0)

        val visits = VisitDetector.detect(moving + stationary)

        assertEquals(1, visits.size)
        assertEquals(100_000L, visits[0].arrivedAt)
        assertEquals(100_000 + 6 * minute, visits[0].departedAt)
    }

    @Test
    fun twoDifferentPlaces_areTwoVisits() {
        val points = stay(0, 6 * minute) + stay(7 * minute, 13 * minute, northMeters = 1_000.0)

        val visits = VisitDetector.detect(points)

        assertEquals(listOf(0L, 7 * minute), visits.map { it.arrivedAt })
        assertEquals(1_000.0, visits[1].centerLatitude / DEG_PER_METER, 1e-6)
    }

    @Test
    fun returningAfterLongGap_isNotMerged() {
        val points = stay(0, 6 * minute) +
            testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 8 * minute) +
            testPoint(latitude = 2_000 * DEG_PER_METER, recordedAt = 12 * minute) +
            stay(16 * minute + 1, 22 * minute + 1)

        val visits = VisitDetector.detect(points)

        assertEquals(2, visits.size)
    }

    @Test
    fun returningWithinMergeGap_isMerged() {
        val points = stay(0, 6 * minute) +
            testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 8 * minute) +
            stay(11 * minute, 17 * minute)

        val visits = VisitDetector.detect(points)

        assertEquals(1, visits.size)
        assertEquals(17 * minute, visits[0].departedAt)
    }

    @Test
    fun gpsLostDuringStay_countsTheGap() {
        val points = listOf(testPoint(recordedAt = 0), testPoint(latitude = 20 * DEG_PER_METER, recordedAt = 20 * minute))

        val visits = VisitDetector.detect(points)

        assertEquals(1, visits.size)
        assertEquals(20 * minute, visits[0].durationMs)
        assertEquals(2, visits[0].pointCount)
    }

    @Test
    fun tripStartingAndEndingStationary_usesFirstAndLastTripPoints() {
        val start = stay(0, 6 * minute)
        val moving = (1..20).map { i ->
            testPoint(latitude = (i * 50.0) * DEG_PER_METER, recordedAt = 6 * minute + i * 5_000L)
        }
        val end = stay(8 * minute, 14 * minute, northMeters = 1_050.0)

        val visits = VisitDetector.detect(start + moving + end)

        assertEquals(2, visits.size)
        assertEquals(0L, visits.first().arrivedAt)
        assertEquals(14 * minute, visits.last().departedAt)
    }

    @Test
    fun constantMovement_isNoVisit() {
        val points = (0 until 200).map { i -> testPoint(latitude = (i * 50.0) * DEG_PER_METER, recordedAt = i * 5_000L) }

        assertTrue(VisitDetector.detect(points).isEmpty())
    }

    @Test
    fun fewerThanTwoPoints_isNoVisit() {
        assertTrue(VisitDetector.detect(emptyList()).isEmpty())
        assertTrue(VisitDetector.detect(listOf(testPoint(recordedAt = 0))).isEmpty())
    }

    @Test
    fun center_isMeanOfPoints() {
        val points = listOf(
            testPoint(latitude = 20 * DEG_PER_METER, longitude = 0.0001, recordedAt = 0),
            testPoint(latitude = -20 * DEG_PER_METER, longitude = 0.0003, recordedAt = 6 * minute),
        )

        val visit = VisitDetector.detect(points).single()

        assertEquals(0.0, visit.centerLatitude, 1e-12)
        assertEquals(0.0002, visit.centerLongitude, 1e-12)
    }
}
