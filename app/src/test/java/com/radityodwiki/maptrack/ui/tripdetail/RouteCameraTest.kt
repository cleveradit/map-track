package com.radityodwiki.maptrack.ui.tripdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCameraTest {

    @Test
    fun emptyRouteKeepsDefaultCamera() {
        assertEquals(RouteCamera.None, routeCamera(emptyList()))
    }

    @Test
    fun singleOrNearbyPointsAreCentered() {
        assertTrue(routeCamera(listOf(RoutePoint(-7.78, 110.36))) is RouteCamera.Center)
        val nearby = routeCamera(listOf(RoutePoint(-7.78, 110.36), RoutePoint(-7.7801, 110.3601)))
        assertTrue(nearby is RouteCamera.Center)
    }

    @Test
    fun routeIsFittedToBounds() {
        val camera = routeCamera(listOf(RoutePoint(-7.80, 110.40), RoutePoint(-7.75, 110.36), RoutePoint(-7.77, 110.38)))

        assertEquals(RouteCamera.Bounds(south = -7.80, west = 110.36, north = -7.75, east = 110.40), camera)
    }
}
