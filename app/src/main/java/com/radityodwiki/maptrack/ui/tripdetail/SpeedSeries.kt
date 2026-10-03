package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.LocationPoint
import kotlin.math.ceil

private const val MPS_TO_KMH = 3.6
private const val Y_STEP_KMH = 10.0

/** One point of the speed chart: time since trip start and speed in km/h. */
data class SpeedSample(val offsetMs: Long, val kmh: Double)

/**
 * Speed history from `location_points.speed` (PRD §21). Points without speed are skipped.
 * Long trips are thinned evenly to at most [maxSamples], keeping the first and last sample.
 */
fun speedSeries(points: List<LocationPoint>, startedAt: Long, maxSamples: Int = 500): List<SpeedSample> {
    val samples = points.mapNotNull { point ->
        point.speedMps?.let { SpeedSample(point.recordedAt - startedAt, it * MPS_TO_KMH) }
    }
    if (samples.size <= maxSamples || maxSamples < 2) return samples
    val step = (samples.size - 1).toDouble() / (maxSamples - 1)
    return List(maxSamples) { i -> samples[Math.round(i * step).toInt()] }
}

/** Top of the Y axis: the next multiple of 10 km/h above the fastest sample (at least 10). */
fun chartMaxKmh(samples: List<SpeedSample>): Double {
    val max = samples.maxOfOrNull { it.kmh } ?: 0.0
    return (ceil(max / Y_STEP_KMH) * Y_STEP_KMH).coerceAtLeast(Y_STEP_KMH)
}
