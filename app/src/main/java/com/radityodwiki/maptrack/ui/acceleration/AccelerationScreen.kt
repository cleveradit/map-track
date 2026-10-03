package com.radityodwiki.maptrack.ui.acceleration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.model.AccelerationRun
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.usecase.AccelerationConfig
import com.radityodwiki.maptrack.domain.usecase.AccelerationPhase
import com.radityodwiki.maptrack.domain.usecase.AccelerationState
import com.radityodwiki.maptrack.ui.format.formatDate
import com.radityodwiki.maptrack.ui.format.formatSeconds
import com.radityodwiki.maptrack.ui.format.formatSpeed
import com.radityodwiki.maptrack.ui.format.formatTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccelerationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccelerationViewModel = viewModel(factory = AccelerationViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The screen stays on while measuring a run.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // System bar insets are already applied by the outer Scaffold in MapTrackNavHost.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acceleration_title)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.acceleration_safety), color = MaterialTheme.colorScheme.error)
            when {
                !state.permissionOk -> Text(stringResource(R.string.acceleration_need_permission))
                !state.locationEnabled -> Text(stringResource(R.string.location_disabled))
                else -> Measurement(state.measurement, state.distanceUnit, viewModel::stop, viewModel::restart)
            }
            if (state.runs.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.acceleration_history), style = MaterialTheme.typography.titleMedium)
                state.runs.forEach { run -> RunRow(run, state.distanceUnit) { viewModel.deleteRun(run.id) } }
            }
        }
    }
}

@Composable
private fun Measurement(state: AccelerationState, unit: DistanceUnit, onStop: () -> Unit, onRestart: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(state.phase.labelRes()), style = MaterialTheme.typography.titleMedium)
            Text(formatSpeed(state.speedMps, unit), style = MaterialTheme.typography.displayMedium)
            Row(Modifier.fillMaxWidth()) {
                Text(formatSeconds(state.elapsedMs), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Text("${state.distanceMeters.roundToInt()} m", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AccelerationConfig.DISTANCE_TARGETS_METERS.forEach { meters ->
            ResultRow(
                label = stringResource(R.string.acceleration_distance_target, meters),
                time = formatSeconds(state.distanceTimesMs[meters]),
                detail = state.distanceSpeedsMps[meters]?.let { formatSpeed(it, unit) },
            )
        }
        ResultRow(stringResource(R.string.acceleration_speed_target), formatSeconds(state.time0To100KmhMs), null)
    }

    when (state.phase) {
        AccelerationPhase.RUNNING -> Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.acceleration_stop))
        }
        AccelerationPhase.FINISHED, AccelerationPhase.INVALID ->
            OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acceleration_restart))
            }
        else -> Unit
    }
}

@Composable
private fun ResultRow(label: String, time: String, detail: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, Modifier.padding(end = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(time, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RunRow(run: AccelerationRun, unit: DistanceUnit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${formatDate(run.startedAt)} · ${formatTime(run.startedAt)}", style = MaterialTheme.typography.titleSmall)
            val parts = AccelerationConfig.DISTANCE_TARGETS_METERS.mapNotNull { m ->
                run.distanceTimesMs[m]?.let { "$m m ${formatSeconds(it)}" }
            } + listOfNotNull(run.time0To100KmhMs?.let { "0–100 km/jam ${formatSeconds(it)}" })
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.acceleration_max_speed, formatSpeed(run.maxSpeedMps, unit)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

private fun AccelerationPhase.labelRes(): Int = when (this) {
    AccelerationPhase.WAITING_GPS -> R.string.acceleration_waiting_gps
    AccelerationPhase.WAITING_STILL -> R.string.acceleration_waiting_still
    AccelerationPhase.READY -> R.string.acceleration_ready
    AccelerationPhase.RUNNING -> R.string.acceleration_running
    AccelerationPhase.FINISHED -> R.string.acceleration_finished
    AccelerationPhase.INVALID -> R.string.acceleration_invalid
}
