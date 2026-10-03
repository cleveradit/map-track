package com.radityodwiki.maptrack.ui.map

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * A MapLibre [MapView] hosted in Compose with lifecycle forwarding. [onStyleLoaded] runs once the
 * style is ready; without network the style may never load and the map stays blank (PRD §23).
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapLibreMap(
    modifier: Modifier = Modifier,
    onStyleLoaded: (MapLibreMap, Style) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnStyleLoaded = rememberUpdatedState(onStyleLoaded)

    val mapView = remember {
        MapLibre.getInstance(context)
        MapCache.ensureConfigured(context)
        MapView(context).apply {
            onCreate(null)
            // Keep pan/zoom gestures on the map when it sits inside a scrollable parent.
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(MapConfig.DEFAULT_LATITUDE, MapConfig.DEFAULT_LONGITUDE))
                    .zoom(MapConfig.DEFAULT_ZOOM)
                    .build()
                map.setStyle(Style.Builder().fromUri(MapConfig.STYLE_URL)) { style ->
                    currentOnStyleLoaded.value(map, style)
                }
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
