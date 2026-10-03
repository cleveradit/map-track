package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.toRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccelerationMeterTest {

    private val meter = AccelerationMeter()

    private fun fix(seconds: Double, speed: Float?, accuracy: Float = 5f) =
        meter.onFix(SpeedFix((seconds * 1000).toLong(), speed, accuracy))

    /** Still fixes every second from 0 to 2 s: ready, zero point at 2 s. */
    private fun standStill() {
        for (s in 0..2) fix(s.toDouble(), 0f)
        assertEquals(AccelerationPhase.READY, meter.state.phase)
    }

    @Test
    fun constantAcceleration() {
        standStill()
        // 4 m/s² from the zero point at 2 s.
        var s = 3
        while (meter.state.phase != AccelerationPhase.FINISHED && s < 40) {
            fix(s.toDouble(), (4.0 * (s - 2)).toFloat())
            s++
        }

        val state = meter.state
        assertEquals(AccelerationPhase.FINISHED, state.phase)
        assertEquals(6_944.0, state.time0To100KmhMs!!.toDouble(), 100.0)
        assertEquals(7_071.0, state.distanceTimesMs.getValue(100).toDouble(), 100.0)
        assertEquals(15_811.0, state.distanceTimesMs.getValue(500).toDouble(), 100.0)
        assertEquals(28.28, state.distanceSpeedsMps.getValue(100), 0.1)
        assertEquals(2_000L, state.startedAt)
    }

    @Test
    fun readyAfterTwoSecondsStill() {
        fix(0.0, 0f)
        fix(1.0, 0f)
        fix(1.9, 0f)
        assertEquals(AccelerationPhase.WAITING_STILL, meter.state.phase)
        fix(2.0, 0f)
        assertEquals(AccelerationPhase.READY, meter.state.phase)
    }

    @Test
    fun inaccurateFixesKeepWaiting() {
        for (s in 0..5) fix(s.toDouble(), 0f, accuracy = 25f)

        assertEquals(AccelerationPhase.WAITING_GPS, meter.state.phase)
    }

    @Test
    fun movingBeforeReadyDoesNotStart() {
        fix(0.0, 0f)
        fix(1.0, 3f)
        fix(2.0, 5f)

        assertEquals(AccelerationPhase.WAITING_STILL, meter.state.phase)
    }

    @Test
    fun gpsGapInvalidatesRun() {
        standStill()
        fix(3.0, 4f)
        fix(7.0, 20f)

        assertEquals(AccelerationPhase.INVALID, meter.state.phase)
        assertNull(meter.state.toRun())
    }

    @Test
    fun stoppingMidwayKeepsReachedTargets() {
        standStill()
        // 10 m/s for 15 s (≈ 150 m), then still for 2 s.
        for (s in 3..17) fix(s.toDouble(), 10f)
        fix(18.0, 0f)
        fix(20.0, 0f)

        val state = meter.state
        assertEquals(AccelerationPhase.FINISHED, state.phase)
        assertEquals(setOf(100), state.distanceTimesMs.keys)
        assertNull(state.time0To100KmhMs)
        assertNotNull(state.toRun())
    }

    @Test
    fun userStopEndsRun() {
        standStill()
        for (s in 3..15) fix(s.toDouble(), 10f)

        assertEquals(AccelerationPhase.FINISHED, meter.stop().phase)
        assertEquals(setOf(100), meter.state.distanceTimesMs.keys)
    }

    @Test
    fun runEndsAtTimeLimit() {
        standStill()
        var s = 3
        while (meter.state.phase == AccelerationPhase.RUNNING || s == 3) {
            fix(s.toDouble(), 2f)
            s++
            if (s > 100) break
        }

        assertEquals(AccelerationPhase.FINISHED, meter.state.phase)
        assertEquals(60_000L, meter.state.elapsedMs)
    }

    @Test
    fun noTargetReachedIsNotSaved() {
        standStill()
        for (s in 3..6) fix(s.toDouble(), 5f)

        meter.stop()

        assertNull(meter.state.toRun())
    }

    @Test
    fun resetStartsOver() {
        standStill()
        fix(3.0, 5f)
        meter.reset()

        assertEquals(AccelerationState(), meter.state)
    }
}
