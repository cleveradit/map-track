package com.radityodwiki.maptrack.domain.model

/** A raw fix from the location provider. [time] is the fix time in epoch millis UTC. */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearingDegrees: Float?,
    val altitudeMeters: Double?,
    val time: Long,
)

fun GpsFix.toLocationPoint(tripId: String) = LocationPoint(
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    speedMps = speedMps,
    bearingDegrees = bearingDegrees,
    altitudeMeters = altitudeMeters,
    recordedAt = time,
)
