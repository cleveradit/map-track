package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint

/** Degrees of latitude per meter on the haversine sphere. */
const val DEG_PER_METER = 1.0 / 111_195.08

fun testPoint(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    recordedAt: Long = 0L,
    accuracy: Float = 5f,
    speed: Float? = null,
) = LocationPoint(
    tripId = "trip",
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
    speedMps = speed,
    bearingDegrees = null,
    altitudeMeters = null,
    recordedAt = recordedAt,
)
