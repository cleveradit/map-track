package com.radityodwiki.maptrack.domain.model

/**
 * One accepted GPS fix of a trip. [recordedAt] is the fix time from Android
 * (`Location.time`), not the time it was stored.
 */
data class LocationPoint(
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearingDegrees: Float?,
    val altitudeMeters: Double?,
    val recordedAt: Long,
)
