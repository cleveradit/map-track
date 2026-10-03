package com.radityodwiki.maptrack.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.location.TrackingConfig
import com.radityodwiki.maptrack.ui.format.formatDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheMessage by viewModel.cacheMessage.collectAsStateWithLifecycle()
    val autoTripStep by viewModel.autoTripStep.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onAutoTripPermissionResult(granted)
    }

    // System bar insets are already applied by the outer Scaffold in MapTrackNavHost.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val current = settings
            if (current == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                SettingsContent(current, viewModel, onClearCache = { confirmClear = true })
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(stringResource(R.string.settings_clear_cache_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearMapCache()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    autoTripStep?.let { step ->
        val permission = when (step) {
            AutoTripStep.ACTIVITY_RATIONALE -> Manifest.permission.ACTIVITY_RECOGNITION
            AutoTripStep.BACKGROUND_RATIONALE -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> null
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissAutoTripStep,
            text = { Text(stringResource(step.textRes())) },
            confirmButton = {
                if (permission != null) {
                    TextButton(onClick = { permissionLauncher.launch(permission) }) {
                        Text(stringResource(R.string.permission_allow))
                    }
                } else {
                    TextButton(onClick = viewModel::dismissAutoTripStep) { Text(stringResource(R.string.dismiss)) }
                }
            },
            dismissButton = {
                if (permission != null) {
                    TextButton(onClick = viewModel::dismissAutoTripStep) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    cacheMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCacheMessage,
            text = {
                Text(
                    stringResource(
                        if (message == CacheMessage.CLEARED) R.string.settings_cache_cleared else R.string.settings_cache_failed,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCacheMessage) { Text(stringResource(R.string.dismiss)) }
            },
        )
    }
}

@Composable
private fun SettingsContent(settings: AppSettings, viewModel: SettingsViewModel, onClearCache: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.settings_tracking))
        Label(stringResource(R.string.settings_interval))
        ChoiceRow(
            options = TrackingConfig.INTERVAL_OPTIONS_MS,
            selected = settings.trackingIntervalMs,
            label = { stringResource(R.string.settings_interval_option, (it / 1000).toInt()) },
            onSelect = viewModel::setTrackingInterval,
        )
        Label(stringResource(R.string.settings_accuracy))
        ChoiceRow(
            options = TrackingConfig.ACCURACY_OPTIONS_METERS,
            selected = settings.accuracyThresholdMeters,
            label = { formatDistance(it.toDouble(), settings.distanceUnit) },
            onSelect = viewModel::setAccuracyThreshold,
        )
        Text(stringResource(R.string.settings_next_trip_note), color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()
        SectionTitle(stringResource(R.string.settings_display))
        Label(stringResource(R.string.settings_unit))
        ChoiceRow(
            options = DistanceUnit.entries,
            selected = settings.distanceUnit,
            label = {
                stringResource(if (it == DistanceUnit.METRIC) R.string.settings_unit_metric else R.string.settings_unit_imperial)
            },
            onSelect = viewModel::setDistanceUnit,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_follow), modifier = Modifier.weight(1f))
            Switch(checked = settings.mapFollowLocation, onCheckedChange = viewModel::setMapFollowLocation)
        }

        HorizontalDivider()
        SectionTitle(stringResource(R.string.settings_auto_trip))
        Text(stringResource(R.string.settings_auto_trip_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (settings.autoTripRevokedNotice && !settings.autoTripEnabled) {
            Text(stringResource(R.string.settings_auto_trip_revoked), color = MaterialTheme.colorScheme.error)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_auto_trip_toggle), modifier = Modifier.weight(1f))
            Switch(
                checked = settings.autoTripEnabled,
                onCheckedChange = { if (it) viewModel.requestAutoTrip() else viewModel.disableAutoTrip() },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_auto_trip_walking), modifier = Modifier.weight(1f))
            Switch(checked = settings.autoTripIncludeWalking, onCheckedChange = viewModel::setAutoTripIncludeWalking)
        }

        HorizontalDivider()
        SectionTitle(stringResource(R.string.settings_map))
        Text(stringResource(R.string.settings_map_cache_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_clear_cache))
        }

        HorizontalDivider()
        SectionTitle(stringResource(R.string.settings_about))
        Text(stringResource(R.string.settings_version, version))
        Text(stringResource(R.string.settings_attribution), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.settings_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

private fun AutoTripStep.textRes(): Int = when (this) {
    AutoTripStep.NEED_PRECISE_LOCATION -> R.string.auto_trip_need_precise
    AutoTripStep.ACTIVITY_RATIONALE -> R.string.auto_trip_activity_rationale
    AutoTripStep.BACKGROUND_RATIONALE -> R.string.auto_trip_background_rationale
    AutoTripStep.DENIED -> R.string.auto_trip_denied
    AutoTripStep.FAILED -> R.string.auto_trip_failed
}
