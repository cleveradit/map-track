package com.radityodwiki.maptrack.domain.model

/**
 * A stop of at least a few minutes within one trip, derived from its location points
 * (PRD §38 Fase 2). It can be recomputed at any time; the points stay the source of truth.
 */
data class Visit(
    val tripId: String,
    val arrivedAt: Long,
    val departedAt: Long,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val pointCount: Int,
) {
    val durationMs: Long get() = departedAt - arrivedAt
}
