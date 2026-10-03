package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.usecase.DEG_PER_METER
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationaryDetectorTest {

    private fun fix(seconds: Long, northMeters: Double = 0.0, speed: Float? = 0.1f, accuracy: Float = 10f) =
        GpsFix(northMeters * DEG_PER_METER, 0.0, accuracy, speed, null, null, seconds * 1_000)

    /** Feeds a still fix every 5 s from 0 to [untilSeconds] inclusive. */
    private fun StationaryDetector.stayUntil(untilSeconds: Long, speed: Float? = 0.1f) {
        for (s in 0..untilSeconds step 5) onFix(fix(s, speed = speed))
    }

    @Test
    fun becomesStationaryAfterTwoMinutes() {
        val detector = StationaryDetector()

        detector.stayUntil(115)
        assertFalse(detector.isStationary)
        assertTrue(detector.onFix(fix(120)))
    }

    @Test
    fun leavingTheRadiusEndsStationary() {
        val detector = StationaryDetector()
        detector.stayUntil(120)

        assertFalse(detector.onFix(fix(125, northMeters = 150.0)))
    }

    @Test
    fun speedEndsStationary() {
        val detector = StationaryDetector()
        detector.stayUntil(120)

        assertFalse(detector.onFix(fix(125, speed = 0.5f)))
    }

    @Test
    fun movingNeverBecomesStationary() {
        val detector = StationaryDetector()

        for (s in 0L..600 step 5) assertFalse(detector.onFix(fix(s, northMeters = s * 4.0, speed = 4f)))
    }

    @Test
    fun inaccurateFixIsIgnored() {
        val detector = StationaryDetector()
        detector.stayUntil(60)
        detector.onFix(fix(65, northMeters = 300.0, accuracy = 150f))
        for (s in 70L..120 step 5) detector.onFix(fix(s))

        assertTrue(detector.isStationary)
    }

    @Test
    fun withoutSpeedRadiusAloneDecides() {
        val detector = StationaryDetector()

        detector.stayUntil(120, speed = null)

        assertTrue(detector.isStationary)
    }
}
