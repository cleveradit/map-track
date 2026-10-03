package com.radityodwiki.maptrack.ui.tripdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.radityodwiki.maptrack.ui.map.MapConfig
import com.radityodwiki.maptrack.ui.map.MapLibreMap
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Geometry
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val ROUTE_SOURCE = "route"
private const val START_SOURCE = "route-start"
private const val FINISH_SOURCE = "route-finish"
private const val CAMERA_PADDING_PX = 48

private class RouteSources(val route: GeoJsonSource, val start: GeoJsonSource, val finish: GeoJsonSource)

/** Trip route as a polyline with start and finish markers (PRD §20). */
@Composable
fun TripRouteMap(route: List<RoutePoint>, showFinish: Boolean, modifier: Modifier = Modifier) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var sources by remember { mutableStateOf<RouteSources?>(null) }
    var cameraSet by remember { mutableStateOf(false) }

    MapLibreMap(modifier = modifier) { loadedMap, style ->
        val created = RouteSources(GeoJsonSource(ROUTE_SOURCE), GeoJsonSource(START_SOURCE), GeoJsonSource(FINISH_SOURCE))
        style.addSource(created.route)
        style.addSource(created.start)
        style.addSource(created.finish)
        style.addLayer(
            LineLayer("route-line", ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor("#1A73E8"),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            ),
        )
        style.addLayer(markerLayer("route-start-dot", START_SOURCE, "#1E8E3E"))
        style.addLayer(markerLayer("route-finish-dot", FINISH_SOURCE, "#D93025"))
        sources = created
        map = loadedMap
    }

    LaunchedEffect(route, showFinish, map, sources) {
        val currentMap = map ?: return@LaunchedEffect
        val current = sources ?: return@LaunchedEffect
        val points = route.map { Point.fromLngLat(it.longitude, it.latitude) }

        current.route.setGeoJson(featuresOf(if (points.size >= 2) LineString.fromLngLats(points) else null))
        current.start.setGeoJson(featuresOf(points.firstOrNull()))
        current.finish.setGeoJson(featuresOf(points.lastOrNull()?.takeIf { showFinish && points.size >= 2 }))

        // Set the camera once so a live route does not fight the user's panning.
        if (!cameraSet) {
            when (val camera = routeCamera(route)) {
                RouteCamera.None -> return@LaunchedEffect
                is RouteCamera.Center -> currentMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(camera.point.latitude, camera.point.longitude),
                        MapConfig.FOLLOW_ZOOM,
                    ),
                )
                is RouteCamera.Bounds -> currentMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds.from(camera.north, camera.east, camera.south, camera.west),
                        CAMERA_PADDING_PX,
                    ),
                )
            }
            cameraSet = true
        }
    }
}

private fun markerLayer(id: String, source: String, color: String) =
    CircleLayer(id, source).withProperties(
        PropertyFactory.circleRadius(8f),
        PropertyFactory.circleColor(color),
        PropertyFactory.circleStrokeWidth(3f),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
    )

/** Empty collection clears a source; otherwise a single feature. */
private fun featuresOf(geometry: Geometry?): FeatureCollection =
    FeatureCollection.fromFeatures(listOfNotNull(geometry?.let { Feature.fromGeometry(it) }))
