package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.location.AutoTripConfig
import kotlin.math.max

data class TripStats(
    val distanceMeters: Double,
    val durationMs: Long,
    val averageSpeedMps: Double,
    val maxSpeedMps: Double?,
)

/** Trip statistics as defined in PRD §16. Points are expected in recordedAt order. */
object TripStatisticsCalculator {

    fun calculate(points: List<LocationPoint>, startedAt: Long, endedAt: Long): TripStats {
        val distance = anchoredDistance(points)
        val durationMs = (endedAt - startedAt).coerceAtLeast(0)
        val average = if (durationMs > 0) distance / (durationMs / 1000.0) else 0.0
        val max = points.mapNotNull { it.speedMps }.maxOrNull()?.toDouble()
        return TripStats(distance, durationMs, average, max)
    }

    /**
     * Distance with jitter damping (PRD §38 Fase 5, v2.5): a point only adds distance when it is
     * farther from the current anchor than either point's accuracy, and then becomes the new anchor.
     * A point whose GPS speed is known and below [AutoTripConfig.STATIONARY_SPEED_MPS] is skipped.
     * A GPS gap stays a straight line between the anchor and the next point (§31).
     */
    fun anchoredDistance(points: List<LocationPoint>): Double {
        var anchor = points.firstOrNull() ?: return 0.0
        var total = 0.0
        for (point in points.drop(1)) {
            val speed = point.speedMps
            if (speed != null && speed < AutoTripConfig.STATIONARY_SPEED_MPS) continue
            val meters = GeoDistance.meters(anchor.latitude, anchor.longitude, point.latitude, point.longitude)
            if (meters > max(anchor.accuracyMeters, point.accuracyMeters)) {
                total += meters
                anchor = point
            }
        }
        return total
    }
}
