package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.LocationPoint

private const val MIN_SPAN_DEGREES = 0.0005

data class RoutePoint(val latitude: Double, val longitude: Double)

fun LocationPoint.toRoutePoint() = RoutePoint(latitude, longitude)

/** Initial camera for a trip route. */
sealed interface RouteCamera {
    data object None : RouteCamera
    data class Center(val point: RoutePoint) : RouteCamera
    data class Bounds(val south: Double, val west: Double, val north: Double, val east: Double) : RouteCamera
}

/** Fits the whole route; a route that barely moves is centered instead of zoomed to the max. */
fun routeCamera(route: List<RoutePoint>): RouteCamera {
    if (route.isEmpty()) return RouteCamera.None
    val south = route.minOf { it.latitude }
    val north = route.maxOf { it.latitude }
    val west = route.minOf { it.longitude }
    val east = route.maxOf { it.longitude }
    if (north - south < MIN_SPAN_DEGREES && east - west < MIN_SPAN_DEGREES) {
        return RouteCamera.Center(RoutePoint((south + north) / 2, (west + east) / 2))
    }
    return RouteCamera.Bounds(south, west, north, east)
}
