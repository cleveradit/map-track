package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint

data class TripStats(
    val distanceMeters: Double,
    val durationMs: Long,
    val averageSpeedMps: Double,
    val maxSpeedMps: Double?,
)

/** Trip statistics as defined in PRD §16. Points are expected in recordedAt order. */
object TripStatisticsCalculator {

    fun calculate(points: List<LocationPoint>, startedAt: Long, endedAt: Long): TripStats {
        val distance = points.zipWithNext { a, b ->
            GeoDistance.meters(a.latitude, a.longitude, b.latitude, b.longitude)
        }.sum()
        val durationMs = (endedAt - startedAt).coerceAtLeast(0)
        val average = if (durationMs > 0) distance / (durationMs / 1000.0) else 0.0
        val max = points.mapNotNull { it.speedMps }.maxOrNull()?.toDouble()
        return TripStats(distance, durationMs, average, max)
    }
}
