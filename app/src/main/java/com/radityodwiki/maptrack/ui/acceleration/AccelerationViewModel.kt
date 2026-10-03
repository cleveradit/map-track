package com.radityodwiki.maptrack.ui.acceleration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.AccelerationRepository
import com.radityodwiki.maptrack.domain.model.AccelerationRun
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.toRun
import com.radityodwiki.maptrack.domain.usecase.AccelerationConfig
import com.radityodwiki.maptrack.domain.usecase.AccelerationMeter
import com.radityodwiki.maptrack.domain.usecase.AccelerationPhase
import com.radityodwiki.maptrack.domain.usecase.AccelerationState
import com.radityodwiki.maptrack.domain.usecase.SpeedFix
import com.radityodwiki.maptrack.location.LocationRequestSpec
import com.radityodwiki.maptrack.location.LocationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccelerationUiState(
    val permissionOk: Boolean,
    val locationEnabled: Boolean,
    val measurement: AccelerationState = AccelerationState(),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    /** Newest first. */
    val runs: List<AccelerationRun> = emptyList(),
)

/** Acceleration test screen (PRD §38 Fase 5, v2.6). GPS runs at 1 Hz only while the screen is visible. */
class AccelerationViewModel(
    private val locationSource: LocationSource,
    private val repository: AccelerationRepository,
    settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT),
) : ViewModel() {

    private val meter = AccelerationMeter()
    private val measurement = MutableStateFlow(meter.state)
    private var saved = false

    private val permissionOk get() = locationSource.permissionState() == LocationPermission.GRANTED

    /** Feeds the meter; emits once immediately so the screen renders before the first fix. */
    private val gps: Flow<Unit> =
        if (!permissionOk) {
            flowOf(Unit)
        } else {
            locationSource.fixes(LocationRequestSpec(AccelerationConfig.GPS_INTERVAL_MS, highAccuracy = true))
                .onEach(::onFix)
                .map { }
                .onStart { emit(Unit) }
        }

    val uiState: StateFlow<AccelerationUiState> = combine(
        measurement,
        settings,
        repository.observeRuns(),
        gps,
    ) { state, current, runs, _ ->
        AccelerationUiState(
            permissionOk = permissionOk,
            locationEnabled = locationSource.isLocationEnabled(),
            measurement = state,
            distanceUnit = current.distanceUnit,
            runs = runs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccelerationUiState(permissionOk, locationSource.isLocationEnabled()),
    )

    private fun onFix(fix: GpsFix) {
        meter.onFix(SpeedFix(fix.time, fix.speedMps, fix.accuracyMeters))
        publish()
    }

    fun stop() {
        meter.stop()
        publish()
    }

    fun restart() {
        meter.reset()
        saved = false
        publish()
    }

    fun deleteRun(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Saves a finished run once, when it reached at least one target. */
    private fun publish() {
        val state = meter.state
        measurement.value = state
        if (state.phase == AccelerationPhase.FINISHED && !saved) {
            val run = state.toRun() ?: return
            saved = true
            viewModelScope.launch { repository.save(run) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                AccelerationViewModel(container.locationTracker, container.accelerationRepository, container.settingsRepository.settings)
            }
        }
    }
}
