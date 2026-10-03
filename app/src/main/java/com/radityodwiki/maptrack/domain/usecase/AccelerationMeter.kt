package com.radityodwiki.maptrack.domain.usecase

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** One GPS fix as the acceleration test sees it: fix time (epoch ms), Doppler speed, accuracy. */
data class SpeedFix(val timeMs: Long, val speedMps: Float?, val accuracyMeters: Float)

enum class AccelerationPhase { WAITING_GPS, WAITING_STILL, READY, RUNNING, FINISHED, INVALID }

data class AccelerationState(
    val phase: AccelerationPhase = AccelerationPhase.WAITING_GPS,
    /** Time since the start (the last still fix), 0 before the run. */
    val elapsedMs: Long = 0,
    val distanceMeters: Double = 0.0,
    val speedMps: Double = 0.0,
    /** Target distance (m) → time from the start (ms), only for targets reached. */
    val distanceTimesMs: Map<Int, Long> = emptyMap(),
    /** Target distance (m) → speed when it was passed. */
    val distanceSpeedsMps: Map<Int, Double> = emptyMap(),
    val time0To100KmhMs: Long? = null,
    val maxSpeedMps: Double = 0.0,
    /** Epoch ms of the start, null before the run. */
    val startedAt: Long? = null,
)

/**
 * Acceleration test from 1 Hz GPS speeds (PRD §38 Fase 5, v2.6). Distance is the trapezoid
 * integral of speed; target times assume constant acceleration between two fixes. Not thread-safe.
 */
class AccelerationMeter {
    private data class Sample(val timeMs: Long, val speedMps: Double, val distanceMeters: Double)

    var state = AccelerationState()
        private set

    private var stillSince: Long? = null
    private var lastStill: SpeedFix? = null
    private var last: Sample? = null
    private var belowStartSince: Long? = null

    fun onFix(fix: SpeedFix): AccelerationState {
        when (state.phase) {
            AccelerationPhase.FINISHED, AccelerationPhase.INVALID -> Unit
            AccelerationPhase.RUNNING -> measure(fix)
            else -> waitForStart(fix)
        }
        return state
    }

    /** The user ends a run early; keeps the targets reached so far. */
    fun stop(): AccelerationState {
        if (state.phase == AccelerationPhase.RUNNING) state = state.copy(phase = AccelerationPhase.FINISHED)
        return state
    }

    fun reset() {
        state = AccelerationState()
        stillSince = null
        lastStill = null
        last = null
        belowStartSince = null
    }

    private fun waitForStart(fix: SpeedFix) {
        val speed = fix.speedMps?.toDouble()
        if (fix.accuracyMeters > AccelerationConfig.MAX_ACCURACY_METERS || speed == null) {
            stillSince = null
            state = state.copy(phase = AccelerationPhase.WAITING_GPS, speedMps = speed ?: 0.0)
            return
        }
        when {
            speed < AccelerationConfig.STILL_SPEED_MPS -> {
                val since = stillSince ?: fix.timeMs.also { stillSince = it }
                lastStill = fix
                val ready = fix.timeMs - since >= AccelerationConfig.READY_STILL_MS
                state = state.copy(
                    phase = if (ready) AccelerationPhase.READY else AccelerationPhase.WAITING_STILL,
                    speedMps = speed,
                )
            }
            state.phase == AccelerationPhase.READY && speed >= AccelerationConfig.START_SPEED_MPS -> {
                val zero = lastStill!!
                last = Sample(zero.timeMs, zero.speedMps?.toDouble() ?: 0.0, 0.0)
                state = state.copy(phase = AccelerationPhase.RUNNING, startedAt = zero.timeMs)
                measure(fix)
            }
            else -> {
                stillSince = null
                state = state.copy(phase = AccelerationPhase.WAITING_STILL, speedMps = speed)
            }
        }
    }

    private fun measure(fix: SpeedFix) {
        val previous = last ?: return
        val start = state.startedAt ?: return
        if (fix.timeMs - previous.timeMs > AccelerationConfig.MAX_FIX_GAP_MS) {
            state = state.copy(phase = AccelerationPhase.INVALID)
            return
        }
        // A fix without speed is skipped; the next one is checked against the gap limit.
        val speed = fix.speedMps?.toDouble() ?: return
        if (fix.timeMs <= previous.timeMs) return

        val dtSeconds = (fix.timeMs - previous.timeMs) / 1000.0
        val acceleration = (speed - previous.speedMps) / dtSeconds
        val distance = previous.distanceMeters + (previous.speedMps + speed) / 2 * dtSeconds

        val times = state.distanceTimesMs.toMutableMap()
        val speeds = state.distanceSpeedsMps.toMutableMap()
        for (target in AccelerationConfig.DISTANCE_TARGETS_METERS) {
            if (target in times || distance < target) continue
            val t = timeToDistance(previous.speedMps, acceleration, target - previous.distanceMeters, dtSeconds)
            times[target] = previous.timeMs + (t * 1000).roundToLong() - start
            speeds[target] = previous.speedMps + acceleration * t
        }
        var time0To100 = state.time0To100KmhMs
        if (time0To100 == null && speed >= AccelerationConfig.TARGET_SPEED_MPS) {
            val t = if (acceleration > 0) {
                ((AccelerationConfig.TARGET_SPEED_MPS - previous.speedMps) / acceleration).coerceIn(0.0, dtSeconds)
            } else {
                0.0
            }
            time0To100 = previous.timeMs + (t * 1000).roundToLong() - start
        }

        last = Sample(fix.timeMs, speed, distance)
        val elapsed = fix.timeMs - start
        val allReached = times.size == AccelerationConfig.DISTANCE_TARGETS_METERS.size && time0To100 != null
        if (speed < AccelerationConfig.START_SPEED_MPS) {
            if (belowStartSince == null) belowStartSince = fix.timeMs
        } else {
            belowStartSince = null
        }
        val stopped = belowStartSince?.let { fix.timeMs - it >= AccelerationConfig.STOP_BELOW_START_MS } == true
        val finished = allReached || stopped || elapsed >= AccelerationConfig.MAX_RUN_MS

        state = state.copy(
            phase = if (finished) AccelerationPhase.FINISHED else AccelerationPhase.RUNNING,
            elapsedMs = elapsed,
            distanceMeters = distance,
            speedMps = speed,
            distanceTimesMs = times,
            distanceSpeedsMps = speeds,
            time0To100KmhMs = time0To100,
            maxSpeedMps = max(state.maxSpeedMps, speed),
        )
    }

    /** Seconds into a segment until [meters] are covered, with constant acceleration within it. */
    private fun timeToDistance(v0: Double, acceleration: Double, meters: Double, segmentSeconds: Double): Double {
        val t = if (abs(acceleration) < 1e-9) {
            if (v0 > 0) meters / v0 else segmentSeconds
        } else {
            (-v0 + sqrt((v0 * v0 + 2 * acceleration * meters).coerceAtLeast(0.0))) / acceleration
        }
        return t.coerceIn(0.0, segmentSeconds)
    }
}
