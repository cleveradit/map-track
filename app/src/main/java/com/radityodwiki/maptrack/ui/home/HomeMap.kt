package com.radityodwiki.maptrack.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.ui.map.CameraAction
import com.radityodwiki.maptrack.ui.map.MapConfig
import com.radityodwiki.maptrack.ui.map.MapLibreMap
import com.radityodwiki.maptrack.ui.map.cameraActionFor
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

private const val USER_SOURCE_ID = "user-position"
private const val USER_LAYER_ID = "user-position-dot"

/** Home map: a dot at the latest GPS fix and a camera that follows it (PRD §7.1, §22). */
@Composable
fun HomeMap(fix: GpsFix?, modifier: Modifier = Modifier) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var userSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var following by remember { mutableStateOf(true) }
    var hasCenteredOnce by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        MapLibreMap { loadedMap, style ->
            val source = GeoJsonSource(USER_SOURCE_ID)
            style.addSource(source)
            style.addLayer(
                CircleLayer(USER_LAYER_ID, USER_SOURCE_ID).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor("#1A73E8"),
                    PropertyFactory.circleStrokeWidth(3f),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                ),
            )
            loadedMap.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) following = false
            }
            userSource = source
            map = loadedMap
        }

        if (!following && fix != null) {
            SmallFloatingActionButton(
                onClick = { following = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) {
                Text(stringResource(R.string.map_follow), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }

    LaunchedEffect(fix, map, userSource, following) {
        val currentMap = map ?: return@LaunchedEffect
        val source = userSource ?: return@LaunchedEffect
        if (fix == null) return@LaunchedEffect
        source.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
        val target = LatLng(fix.latitude, fix.longitude)
        when (cameraActionFor(following, hasCenteredOnce)) {
            CameraAction.ZOOM_TO_FIX -> {
                currentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, MapConfig.FOLLOW_ZOOM))
                hasCenteredOnce = true
            }
            CameraAction.FOLLOW -> currentMap.easeCamera(CameraUpdateFactory.newLatLng(target))
            CameraAction.NONE -> Unit
        }
    }
}
