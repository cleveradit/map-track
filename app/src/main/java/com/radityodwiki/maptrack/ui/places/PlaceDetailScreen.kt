package com.radityodwiki.maptrack.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.ui.map.MapLibreMap
import com.radityodwiki.maptrack.ui.map.placeZoom
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceDetailViewModel = viewModel(factory = PlaceDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    // System bar insets are already applied by the outer Scaffold in MapTrackNavHost.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text((state as? PlaceDetailUiState.Loaded)?.name ?: stringResource(R.string.place_detail_title)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state is PlaceDetailUiState.Loaded) {
                        IconButton(onClick = { onEdit(viewModel.placeId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.place_edit_title))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_place_title))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                PlaceDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                PlaceDetailUiState.NotFound -> Text(stringResource(R.string.place_not_found), Modifier.align(Alignment.Center))
                is PlaceDetailUiState.Loaded -> PlaceDetailContent(current, onTripClick)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_place_title)) },
            text = { Text(stringResource(R.string.delete_place_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PlaceDetailContent(state: PlaceDetailUiState.Loaded, onTripClick: (String) -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlaceMap(
            latitude = state.latitude,
            longitude = state.longitude,
            radiusMeters = state.radiusMeters,
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
        )
        Row(Modifier.fillMaxWidth()) {
            Stat(stringResource(R.string.label_radius), state.radius, Modifier.weight(1f))
            Stat(stringResource(R.string.label_total_visits), state.visitCount.toString(), Modifier.weight(1f))
            Stat(stringResource(R.string.label_total_duration), state.totalDuration, Modifier.weight(1f))
        }

        Text(stringResource(R.string.place_visits_title), style = MaterialTheme.typography.titleMedium)
        if (state.visits.isEmpty()) {
            Text(stringResource(R.string.place_no_visits), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            state.visits.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTripClick(item.tripId) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(item.date, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${item.timeRange} · ${item.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** Static map of a place: radius circle, center dot, camera fitted to the radius. */
@Composable
private fun PlaceMap(latitude: Double, longitude: Double, radiusMeters: Double, modifier: Modifier) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var circle by remember { mutableStateOf<GeoJsonSource?>(null) }
    var center by remember { mutableStateOf<GeoJsonSource?>(null) }

    MapLibreMap(modifier = modifier) { loadedMap, style ->
        val circleSource = GeoJsonSource("place-detail-circle")
        addPlaceCircleLayers(style, circleSource)
        val centerSource = GeoJsonSource("place-detail-center")
        style.addSource(centerSource)
        style.addLayer(
            CircleLayer("place-detail-center-dot", centerSource.id).withProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleColor(PLACE_CIRCLE_COLOR),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ),
        )
        circle = circleSource
        center = centerSource
        map = loadedMap
    }

    LaunchedEffect(latitude, longitude, radiusMeters, map, circle, center) {
        val loadedMap = map ?: return@LaunchedEffect
        circle?.setGeoJson(placeCircle(latitude, longitude, radiusMeters))
        center?.setGeoJson(Point.fromLngLat(longitude, latitude))
        loadedMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), placeZoom(radiusMeters)))
    }
}
