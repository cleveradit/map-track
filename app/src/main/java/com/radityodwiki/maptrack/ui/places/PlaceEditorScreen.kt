package com.radityodwiki.maptrack.ui.places

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.usecase.PlaceConfig
import com.radityodwiki.maptrack.domain.usecase.PlaceNameError
import com.radityodwiki.maptrack.ui.format.formatDistance

private val RADIUS_STEPS =
    ((PlaceConfig.MAX_RADIUS_METERS - PlaceConfig.MIN_RADIUS_METERS) / PlaceConfig.RADIUS_STEP_METERS).toInt() - 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceEditorViewModel = viewModel(factory = PlaceEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> viewModel.onPermissionResult(result[Manifest.permission.ACCESS_FINE_LOCATION] == true) }

    // System bar insets are already applied by the outer Scaffold in MapTrackNavHost.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.place_edit_title else R.string.place_add)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.notFound -> Text(stringResource(R.string.place_not_found), Modifier.align(Alignment.Center))
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> EditorContent(state, viewModel)
            }
        }
    }

    state.message?.let { message ->
        if (message == EditorMessage.PERMISSION_RATIONALE) {
            AlertDialog(
                onDismissRequest = viewModel::dismissMessage,
                text = { Text(stringResource(R.string.place_location_rationale)) },
                confirmButton = {
                    TextButton(onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    }) { Text(stringResource(R.string.permission_allow)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.cancel)) }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = viewModel::dismissMessage,
                text = { Text(stringResource(message.textRes())) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.dismiss)) }
                },
            )
        }
    }
}

@Composable
private fun EditorContent(state: PlaceEditorUiState, viewModel: PlaceEditorViewModel) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacePickerMap(
            radiusMeters = state.radiusMeters,
            cameraRequest = state.cameraRequest,
            onCenterPicked = viewModel::onMapCenterPicked,
            modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)),
        )
        if (!state.hasPoint) {
            Text(stringResource(R.string.place_pick_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = viewModel::useCurrentLocation,
            enabled = !state.isLocating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLocating) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.place_use_current_location))
            }
        }

        Text(
            stringResource(R.string.place_radius, formatDistance(state.radiusMeters)),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = state.radiusMeters.toFloat(),
            onValueChange = { viewModel.onRadiusChange(it.toDouble()) },
            valueRange = PlaceConfig.MIN_RADIUS_METERS.toFloat()..PlaceConfig.MAX_RADIUS_METERS.toFloat(),
            steps = RADIUS_STEPS,
        )

        val nameError = state.nameError.takeIf { state.showNameError }
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.place_name)) },
            singleLine = true,
            isError = nameError != null,
            supportingText = {
                Text(
                    when (nameError) {
                        PlaceNameError.EMPTY -> stringResource(R.string.place_name_empty)
                        PlaceNameError.TOO_LONG -> stringResource(R.string.place_name_too_long, PlaceConfig.NAME_MAX_LENGTH)
                        null -> "${state.nameLength}/${PlaceConfig.NAME_MAX_LENGTH}"
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun EditorMessage.textRes(): Int = when (this) {
    EditorMessage.PERMISSION_RATIONALE -> R.string.place_location_rationale
    EditorMessage.PERMISSION_DENIED -> R.string.place_location_denied
    EditorMessage.LOCATION_DISABLED -> R.string.place_location_disabled
    EditorMessage.LOCATION_TIMEOUT -> R.string.place_location_timeout
    EditorMessage.SAVE_FAILED -> R.string.place_save_failed
}
