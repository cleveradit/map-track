package com.radityodwiki.maptrack.ui.map

import com.radityodwiki.maptrack.domain.usecase.GeoDistance
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.sin

/**
 * A closed ring of (longitude, latitude) pairs approximating a circle on the same sphere as
 * [GeoDistance], so the drawn radius matches the radius used for visit matching.
 */
fun circleRing(latitude: Double, longitude: Double, radiusMeters: Double, segments: Int = 64): List<Pair<Double, Double>> {
    val angular = radiusMeters / GeoDistance.EARTH_RADIUS_METERS
    val lat1 = Math.toRadians(latitude)
    val lon1 = Math.toRadians(longitude)
    val ring = (0 until segments).map { i ->
        val bearing = 2 * Math.PI * i / segments
        val lat2 = asin(sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing))
        val lon2 = lon1 + atan2(sin(bearing) * sin(angular) * cos(lat1), cos(angular) - sin(lat1) * sin(lat2))
        Math.toDegrees(lon2) to Math.toDegrees(lat2)
    }
    return ring + ring.first()
}

/** Zoom that keeps a place circle comfortably on screen: 16 for 100 m, one level out per doubling. */
fun placeZoom(radiusMeters: Double): Double = (16.0 - log2(radiusMeters / 100.0)).coerceIn(12.0, 17.0)
