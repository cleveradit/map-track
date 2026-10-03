package com.radityodwiki.maptrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationFilterTest {

    private val previous = testPoint(recordedAt = 10_000)

    @Test
    fun firstValidPointIsAccepted() {
        assertNull(LocationFilter.evaluate(testPoint(), previous = null))
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        assertEquals(RejectReason.INVALID_COORDINATES, LocationFilter.evaluate(testPoint(latitude = 91.0), null))
        assertEquals(RejectReason.INVALID_COORDINATES, LocationFilter.evaluate(testPoint(longitude = Double.NaN), null))
    }

    @Test
    fun accuracyBoundary() {
        assertNull(LocationFilter.evaluate(testPoint(accuracy = 50f), null))
        assertEquals(RejectReason.POOR_ACCURACY, LocationFilter.evaluate(testPoint(accuracy = 50.1f), null))
    }

    @Test
    fun sameOrOlderTimestampIsRejected() {
        assertEquals(RejectReason.OUT_OF_ORDER, LocationFilter.evaluate(testPoint(recordedAt = 10_000), previous))
        assertEquals(RejectReason.OUT_OF_ORDER, LocationFilter.evaluate(testPoint(recordedAt = 9_000), previous))
    }

    @Test
    fun jumpOfOneKilometerInFiveSecondsIsRejected() {
        val candidate = testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 15_000)
        assertEquals(RejectReason.GPS_JUMP, LocationFilter.evaluate(candidate, previous))
    }

    @Test
    fun oneKilometerAfterTenMinutesGpsLossIsAccepted() {
        val candidate = testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 10_000 + 600_000)
        assertNull(LocationFilter.evaluate(candidate, previous))
    }

    @Test
    fun normalDrivingSpeedIsAccepted() {
        // 150 m in 5 s = 30 m/s (108 km/h)
        val candidate = testPoint(latitude = 150 * DEG_PER_METER, recordedAt = 15_000)
        assertNull(LocationFilter.evaluate(candidate, previous))
    }

    @Test
    fun customAccuracyThresholdBoundary() {
        assertNull(LocationFilter.evaluate(testPoint(accuracy = 20.0f), null, maxAccuracyMeters = 20f))
        assertEquals(
            RejectReason.POOR_ACCURACY,
            LocationFilter.evaluate(testPoint(accuracy = 20.1f), null, maxAccuracyMeters = 20f),
        )
    }
}
