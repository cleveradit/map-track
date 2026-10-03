package com.radityodwiki.maptrack.ui.tripdetail

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = viewModel(factory = TripDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // System bar insets are already applied by the outer Scaffold in MapTrackNavHost.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_detail_title)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                TripDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                TripDetailUiState.NotFound -> Text(stringResource(R.string.trip_not_found), Modifier.align(Alignment.Center))
                is TripDetailUiState.Loaded -> TripDetailContent(current)
            }
        }
    }
}

@Composable
private fun TripDetailContent(state: TripDetailUiState.Loaded) {
    val summary = state.summary
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(summary.date, style = MaterialTheme.typography.headlineSmall)
        if (summary.isActive) {
            Text(
                stringResource(R.string.history_active),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        SummaryRow(stringResource(R.string.label_start), summary.startTime, stringResource(R.string.label_end), summary.endTime)
        SummaryRow(stringResource(R.string.label_duration), summary.duration, stringResource(R.string.label_distance), summary.distance)
        SummaryRow(stringResource(R.string.label_avg_speed), summary.averageSpeed, stringResource(R.string.label_max_speed), summary.maxSpeed)
        SummaryRow(stringResource(R.string.label_point_count), state.pointCount.toString(), null, null)

        TripRouteMap(
            route = state.route,
            showFinish = !summary.isActive,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        if (state.route.size < 2) {
            Text(stringResource(R.string.route_not_enough_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(stringResource(R.string.speed_history_title), style = MaterialTheme.typography.titleMedium)
        if (state.samples.size >= 2) {
            SpeedChart(state.samples, Modifier.fillMaxWidth())
        } else {
            Text(stringResource(R.string.speed_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryRow(label1: String, value1: String, label2: String?, value2: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        SummaryCell(label1, value1, Modifier.weight(1f))
        if (label2 != null && value2 != null) SummaryCell(label2, value2, Modifier.weight(1f))
        else Box(Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
