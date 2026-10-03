package com.radityodwiki.maptrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.settings.SettingsRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.location.AutoTripPermissions
import com.radityodwiki.maptrack.location.AutoTripToggle
import com.radityodwiki.maptrack.location.TrackingConfig
import com.radityodwiki.maptrack.ui.map.MapCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CacheMessage { CLEARED, FAILED }

/** Next step when the user switches automatic trips on (PRD §38 Fase 5, Rule 7). */
enum class AutoTripStep { NEED_PRECISE_LOCATION, ACTIVITY_RATIONALE, BACKGROUND_RATIONALE, DENIED, FAILED }

/** Without automatic-trip support wired in (tests that do not need it). */
private object NoAutoTrip : AutoTripToggle, AutoTripPermissions {
    override suspend fun enable() = false
    override suspend fun disable() = Unit
    override suspend fun setIncludeWalking(enabled: Boolean) = Unit
    override fun hasActivityRecognition() = false
    override fun hasBackgroundLocation() = false
}

/** Settings page (PRD §38 Fase 4). Tracking values apply from the next trip. */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val autoTrip: AutoTripToggle = NoAutoTrip,
    private val permissions: AutoTripPermissions = NoAutoTrip,
    private val preciseLocation: () -> Boolean = { true },
    private val clearCache: suspend () -> Result<Unit>,
) : ViewModel() {

    /** Null until the first read from DataStore. */
    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _cacheMessage = MutableStateFlow<CacheMessage?>(null)
    val cacheMessage: StateFlow<CacheMessage?> = _cacheMessage.asStateFlow()

    /** Values outside [TrackingConfig.INTERVAL_OPTIONS_MS] are ignored. */
    fun setTrackingInterval(intervalMs: Long) {
        if (intervalMs !in TrackingConfig.INTERVAL_OPTIONS_MS) return
        viewModelScope.launch { repository.setTrackingInterval(intervalMs) }
    }

    /** Values outside [TrackingConfig.ACCURACY_OPTIONS_METERS] are ignored. */
    fun setAccuracyThreshold(meters: Float) {
        if (meters !in TrackingConfig.ACCURACY_OPTIONS_METERS) return
        viewModelScope.launch { repository.setAccuracyThreshold(meters) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { repository.setDistanceUnit(unit) }
    }

    fun setMapFollowLocation(enabled: Boolean) {
        viewModelScope.launch { repository.setMapFollowLocation(enabled) }
    }

    private val _autoTripStep = MutableStateFlow<AutoTripStep?>(null)
    val autoTripStep: StateFlow<AutoTripStep?> = _autoTripStep.asStateFlow()

    /** Walks through the permissions one at a time, each after its explanation, then enables. */
    fun requestAutoTrip() {
        val step = when {
            !preciseLocation() -> AutoTripStep.NEED_PRECISE_LOCATION
            !permissions.hasActivityRecognition() -> AutoTripStep.ACTIVITY_RATIONALE
            !permissions.hasBackgroundLocation() -> AutoTripStep.BACKGROUND_RATIONALE
            else -> null
        }
        _autoTripStep.value = step
        // Set before enabling: on an immediate dispatcher the failure would otherwise be overwritten.
        if (step == null) viewModelScope.launch { if (!autoTrip.enable()) _autoTripStep.value = AutoTripStep.FAILED }
    }

    /** Result of a system permission dialog shown after a rationale step. */
    fun onAutoTripPermissionResult(granted: Boolean) {
        if (granted) requestAutoTrip() else _autoTripStep.value = AutoTripStep.DENIED
    }

    fun disableAutoTrip() {
        viewModelScope.launch { autoTrip.disable() }
    }

    fun setAutoTripIncludeWalking(enabled: Boolean) {
        viewModelScope.launch { autoTrip.setIncludeWalking(enabled) }
    }

    fun dismissAutoTripStep() {
        _autoTripStep.value = null
    }

    fun clearMapCache() {
        viewModelScope.launch {
            _cacheMessage.value = if (clearCache().isSuccess) CacheMessage.CLEARED else CacheMessage.FAILED
        }
    }

    fun dismissCacheMessage() {
        _cacheMessage.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MapTrackApplication
                val container = application.container
                SettingsViewModel(
                    repository = container.settingsRepository,
                    autoTrip = container.autoTripController,
                    permissions = container.autoTripPermissions,
                    preciseLocation = { container.locationTracker.permissionState() == LocationPermission.GRANTED },
                ) { MapCache.clear(application) }
            }
        }
    }
}
