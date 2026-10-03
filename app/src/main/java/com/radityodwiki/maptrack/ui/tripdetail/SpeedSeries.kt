package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.ui.format.toDisplaySpeed
import kotlin.math.ceil

private const val Y_STEP = 10.0

/** One point of the speed chart: time since trip start and speed in the display unit (km/h or mph). */
data class SpeedSample(val offsetMs: Long, val speed: Double)

/**
 * Speed history from `location_points.speed` (PRD §21). Points without speed are skipped.
 * Long trips are thinned evenly to at most [maxSamples], keeping the first and last sample.
 */
fun speedSeries(
    points: List<LocationPoint>,
    startedAt: Long,
    unit: DistanceUnit = DistanceUnit.METRIC,
    maxSamples: Int = 500,
): List<SpeedSample> {
    val samples = points.mapNotNull { point ->
        point.speedMps?.let { SpeedSample(point.recordedAt - startedAt, it.toDouble().toDisplaySpeed(unit)) }
    }
    if (samples.size <= maxSamples || maxSamples < 2) return samples
    val step = (samples.size - 1).toDouble() / (maxSamples - 1)
    return List(maxSamples) { i -> samples[Math.round(i * step).toInt()] }
}

/** Top of the Y axis: the next multiple of 10 above the fastest sample (at least 10), in the display unit. */
fun chartMax(samples: List<SpeedSample>): Double {
    val max = samples.maxOfOrNull { it.speed } ?: 0.0
    return (ceil(max / Y_STEP) * Y_STEP).coerceAtLeast(Y_STEP)
}
