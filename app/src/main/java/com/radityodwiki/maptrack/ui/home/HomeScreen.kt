package com.radityodwiki.maptrack.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.location.StartTrackingError
import com.radityodwiki.maptrack.ui.format.formatAccuracy
import com.radityodwiki.maptrack.ui.format.formatCurrentSpeed
import com.radityodwiki.maptrack.ui.format.formatDuration
import com.radityodwiki.maptrack.ui.format.formatTime
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermission() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onPermissionResult() }
    val requestPermission = {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }
    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }
    val openLocationSettings = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }

    // Notification permission is asked on Start (Android 13+); tracking starts whatever the answer.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.startTracking() }
    val onStart = {
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startTracking()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
        HomeMap(
            fix = state.fix,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        when (state.permission) {
            LocationPermission.NOT_REQUESTED -> MessageCard(
                message = stringResource(R.string.permission_rationale),
                primaryLabel = stringResource(R.string.permission_allow),
                onPrimary = requestPermission,
            )
            LocationPermission.DENIED -> MessageCard(
                message = stringResource(R.string.permission_denied),
                primaryLabel = stringResource(R.string.permission_retry),
                onPrimary = requestPermission,
                secondaryLabel = stringResource(R.string.open_app_settings),
                onSecondary = openAppSettings,
            )
            LocationPermission.APPROXIMATE_ONLY -> MessageCard(
                message = stringResource(R.string.permission_approximate),
                primaryLabel = stringResource(R.string.permission_retry),
                onPrimary = requestPermission,
                secondaryLabel = stringResource(R.string.open_app_settings),
                onSecondary = openAppSettings,
            )
            LocationPermission.GRANTED -> if (!state.locationEnabled) {
                MessageCard(
                    message = stringResource(R.string.location_disabled),
                    primaryLabel = stringResource(R.string.open_location_settings),
                    onPrimary = openLocationSettings,
                )
            }
        }

        StatRow(stringResource(R.string.label_speed), formatCurrentSpeed(state.fix?.speedMps, state.fix?.time, state.now))
        StatRow(stringResource(R.string.label_accuracy), formatAccuracy(state.fix?.accuracyMeters))
        StatRow(stringResource(R.string.label_gps), stringResource(state.gpsStatus.labelRes()))
        StatRow(
            stringResource(R.string.label_position),
            state.fix?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) } ?: "—",
        )

        TrackingSection(
            state = state,
            onStart = onStart,
            onStop = viewModel::stopTracking,
            onEndInterrupted = viewModel::endInterruptedTrip,
        )

        state.interruptedTrip?.let { interrupted ->
            InterruptedTripDialog(interrupted, enabled = !state.busy, onEnd = viewModel::endInterruptedTrip)
        }

        state.startError?.let { error ->
            MessageCard(
                message = stringResource(error.messageRes()),
                primaryLabel = stringResource(R.string.dismiss),
                onPrimary = viewModel::dismissError,
            )
        }
    }
}

@Composable
private fun TrackingSection(state: HomeUiState, onStart: () -> Unit, onStop: () -> Unit, onEndInterrupted: () -> Unit) {
    val trip = state.activeTrip
    if (state.interruptedTrip != null) {
        StatRow(stringResource(R.string.label_tracking), stringResource(R.string.tracking_interrupted))
        Button(onClick = onEndInterrupted, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.end_trip))
        }
        return
    }
    StatRow(
        stringResource(R.string.label_tracking),
        stringResource(if (trip != null) R.string.tracking_active else R.string.tracking_inactive),
    )
    if (trip != null) {
        StatRow(stringResource(R.string.label_duration), formatDuration(state.now - trip.startedAt))
        Button(onClick = onStop, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tracking_stop_button))
        }
    } else {
        Button(onClick = onStart, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tracking_start))
        }
    }
}

@Composable
private fun InterruptedTripDialog(trip: InterruptedTrip, enabled: Boolean, onEnd: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.interrupted_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow(stringResource(R.string.label_start), formatTime(trip.startedAt))
                StatRow(stringResource(R.string.label_last_data), trip.lastPointAt?.let { formatTime(it) } ?: "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onEnd, enabled = enabled) { Text(stringResource(R.string.end_trip)) }
        },
    )
}

private fun StartTrackingError.messageRes(): Int = when (this) {
    StartTrackingError.PERMISSION_MISSING -> R.string.permission_denied
    StartTrackingError.LOCATION_DISABLED -> R.string.location_disabled
    StartTrackingError.TRIP_ALREADY_ACTIVE -> R.string.error_trip_already_active
    StartTrackingError.SERVICE_START_FAILED -> R.string.error_service_start_failed
}

private fun GpsStatus.labelRes(): Int = when (this) {
    GpsStatus.NO_PERMISSION -> R.string.gps_no_permission
    GpsStatus.LOCATION_DISABLED -> R.string.gps_disabled
    GpsStatus.SEARCHING -> R.string.gps_searching
    GpsStatus.ACTIVE -> R.string.gps_active
}

@Composable
private fun StatRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun MessageCard(
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary) { Text(primaryLabel) }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
                }
            }
        }
    }
}
