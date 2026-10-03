package com.radityodwiki.maptrack.ui.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radityodwiki.maptrack.ui.map.MapLibreMap
import com.radityodwiki.maptrack.ui.map.placeZoom
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource

private const val CIRCLE_SOURCE = "place-circle"

/**
 * Map with a pin fixed at its center (PRD §38 Fase 3). Only camera moves started by a user gesture
 * report a picked point, so the default camera never becomes a place by accident.
 */
@Composable
fun PlacePickerMap(
    radiusMeters: Double,
    cameraRequest: CameraRequest?,
    onCenterPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var circle by remember { mutableStateOf<GeoJsonSource?>(null) }
    val currentRadius by rememberUpdatedState(radiusMeters)
    val currentOnCenterPicked by rememberUpdatedState(onCenterPicked)

    fun drawCircle(loadedMap: MapLibreMap, source: GeoJsonSource) {
        val target = loadedMap.cameraPosition.target ?: return
        source.setGeoJson(placeCircle(target.latitude, target.longitude, currentRadius))
    }

    Box(modifier = modifier) {
        MapLibreMap(modifier = Modifier.fillMaxSize()) { loadedMap, style ->
            val source = GeoJsonSource(CIRCLE_SOURCE)
            addPlaceCircleLayers(style, source)
            var gestureMove = false
            loadedMap.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) gestureMove = true
            }
            loadedMap.addOnCameraMoveListener { drawCircle(loadedMap, source) }
            loadedMap.addOnCameraIdleListener {
                drawCircle(loadedMap, source)
                val target = loadedMap.cameraPosition.target
                if (gestureMove && target != null) currentOnCenterPicked(target.latitude, target.longitude)
                gestureMove = false
            }
            drawCircle(loadedMap, source)
            circle = source
            map = loadedMap
        }
        // The pin's tip marks the map center.
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Center).size(40.dp).offset(y = (-20).dp),
        )
    }

    LaunchedEffect(radiusMeters, map, circle) {
        val loadedMap = map ?: return@LaunchedEffect
        val source = circle ?: return@LaunchedEffect
        drawCircle(loadedMap, source)
    }

    LaunchedEffect(cameraRequest, map) {
        val loadedMap = map ?: return@LaunchedEffect
        val request = cameraRequest ?: return@LaunchedEffect
        loadedMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(request.latitude, request.longitude), placeZoom(currentRadius)),
        )
    }
}
