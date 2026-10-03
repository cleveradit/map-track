package com.radityodwiki.maptrack.domain.model

import com.radityodwiki.maptrack.domain.usecase.AccelerationPhase
import com.radityodwiki.maptrack.domain.usecase.AccelerationState

/** A saved acceleration test (PRD §38 Fase 5, v2.6). Times are ms from the start; null = not reached. */
data class AccelerationRun(
    val id: Long,
    val startedAt: Long,
    val distanceTimesMs: Map<Int, Long>,
    val distanceSpeedsMps: Map<Int, Double>,
    val time0To100KmhMs: Long?,
    val maxSpeedMps: Double,
)

/** A finished run with at least one target reached; null otherwise (nothing worth saving). */
fun AccelerationState.toRun(): AccelerationRun? {
    val start = startedAt ?: return null
    if (phase != AccelerationPhase.FINISHED) return null
    if (distanceTimesMs.isEmpty() && time0To100KmhMs == null) return null
    return AccelerationRun(0, start, distanceTimesMs, distanceSpeedsMps, time0To100KmhMs, maxSpeedMps)
}
