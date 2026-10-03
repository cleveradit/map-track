package com.radityodwiki.maptrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.location.LocationSource
import com.radityodwiki.maptrack.location.StartTrackingError
import com.radityodwiki.maptrack.location.TrackingController
import com.radityodwiki.maptrack.location.TrackingState
import com.radityodwiki.maptrack.location.TrackingStateHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TICK_MS = 1_000L

class HomeViewModel(
    private val locationSource: LocationSource,
    private val repository: TripRepository,
    private val stateHolder: TrackingStateHolder,
    private val controller: TrackingController,
    private val clock: () -> Long = System::currentTimeMillis,
    settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT),
) : ViewModel() {

    private var permissionRequested = false
    private val permission = MutableStateFlow(currentPermission())
    private val startError = MutableStateFlow<StartTrackingError?>(null)
    private val busy = MutableStateFlow(false)

    /**
     * While the service tracks, Home shows the service's fixes instead of opening a second
     * location request. Otherwise Home requests its own fixes while visible (PRD §7.1).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val fix = combine(permission, stateHolder.state.map { it is TrackingState.Active }.distinctUntilChanged()) { p, tracking ->
        p to tracking
    }.flatMapLatest { (permission, tracking) ->
        when {
            tracking -> stateHolder.state.map { (it as? TrackingState.Active)?.lastFix }
            permission == LocationPermission.GRANTED ->
                locationSource.fixes().map<GpsFix, GpsFix?> { it }.onStart { emit(null) }
            else -> flowOf(null)
        }
    }

    /** Re-evaluated every second so staleness, duration, and the location switch stay current. */
    private val ticks = flow {
        while (true) {
            emit(clock() to locationSource.isLocationEnabled())
            delay(TICK_MS)
        }
    }

    /** Active trip in the database that the tracking service does not own, with its last fix time. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val interruptedTrip = combine(repository.observeActiveTrip(), stateHolder.state) { trip, state ->
        interruptedTripOf(trip, state)
    }.distinctUntilChanged().mapLatest { trip ->
        trip?.let { InterruptedTrip(it.id, it.startedAt, repository.getLastPoint(it.id)?.recordedAt) }
    }

    private data class ActionState(
        val error: StartTrackingError?,
        val busy: Boolean,
        val interrupted: InterruptedTrip?,
        val settings: AppSettings,
    )

    private val actionState = combine(startError, busy, interruptedTrip, settings, ::ActionState)

    val uiState: StateFlow<HomeUiState> = combine(
        permission,
        fix,
        ticks,
        repository.observeActiveTrip(),
        actionState,
    ) { permission, fix, (now, enabled), activeTrip, action ->
        HomeUiState(
            permission = permission,
            locationEnabled = enabled,
            fix = fix,
            now = now,
            activeTrip = activeTrip,
            startError = action.error,
            busy = action.busy,
            interruptedTrip = action.interrupted,
            distanceUnit = action.settings.distanceUnit,
            mapFollowLocation = action.settings.mapFollowLocation,
        )
    }.stateIn(
        scope = viewModelScope,
        // Stop location updates shortly after Home leaves the screen (PRD §7.1).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(permission.value, locationSource.isLocationEnabled(), null, clock()),
    )

    fun refreshPermission() {
        permission.value = currentPermission()
    }

    fun onPermissionResult() {
        permissionRequested = true
        refreshPermission()
    }

    fun startTracking() = runAction {
        startError.value = controller.start()
    }

    fun stopTracking() = runAction {
        controller.stop()
    }

    fun resumeInterruptedTrip() {
        val tripId = uiState.value.interruptedTrip?.tripId ?: return
        runAction { startError.value = controller.resume(tripId) }
    }

    fun endInterruptedTrip() {
        val tripId = uiState.value.interruptedTrip?.tripId ?: return
        runAction { controller.endInterruptedTrip(tripId) }
    }

    fun dismissError() {
        startError.value = null
    }

    private fun runAction(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy.value = false
            }
        }
    }

    private fun currentPermission(): LocationPermission {
        val state = locationSource.permissionState()
        return if (state == LocationPermission.DENIED && !permissionRequested) LocationPermission.NOT_REQUESTED else state
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                HomeViewModel(
                    locationSource = container.locationTracker,
                    repository = container.tripRepository,
                    stateHolder = container.trackingStateHolder,
                    controller = container.trackingController,
                    settings = container.settingsRepository.settings,
                )
            }
        }
    }
}
