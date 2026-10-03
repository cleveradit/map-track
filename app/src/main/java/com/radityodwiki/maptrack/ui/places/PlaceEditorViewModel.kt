package com.radityodwiki.maptrack.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.PlaceInput
import com.radityodwiki.maptrack.domain.usecase.PlaceConfig
import com.radityodwiki.maptrack.domain.usecase.PlaceNameError
import com.radityodwiki.maptrack.domain.usecase.PlaceValidator
import com.radityodwiki.maptrack.location.LocationSource
import com.radityodwiki.maptrack.location.TrackingConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

enum class EditorMessage { PERMISSION_RATIONALE, PERMISSION_DENIED, LOCATION_DISABLED, LOCATION_TIMEOUT, SAVE_FAILED }

/** Moves the picker camera to a point set outside the map. Not a data class: each request is new. */
class CameraRequest(val latitude: Double, val longitude: Double)

data class PlaceEditorUiState(
    val isEditing: Boolean,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    /** Shown only once [showNameError] is true, i.e. after the name was edited or Save was pressed. */
    val showNameError: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double = PlaceConfig.DEFAULT_RADIUS_METERS,
    val isLocating: Boolean = false,
    val message: EditorMessage? = null,
    val cameraRequest: CameraRequest? = null,
    val saved: Boolean = false,
) {
    val nameError: PlaceNameError? get() = PlaceValidator.nameError(name)
    val nameLength: Int get() = name.trim().let { it.codePointCount(0, it.length) }
    val hasPoint: Boolean get() = latitude != null && longitude != null
    val canSave: Boolean get() = hasPoint && !isLoading && !isLocating && !notFound
}

/** Create or edit a saved place (PRD §38 Fase 3). */
class PlaceEditorViewModel(
    private val placeId: String?,
    initialLatitude: Double?,
    initialLongitude: Double?,
    private val placeRepository: PlaceRepository,
    private val locationSource: LocationSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceEditorUiState(isEditing = placeId != null))
    val uiState: StateFlow<PlaceEditorUiState> = _uiState.asStateFlow()

    private var locating: Job? = null

    init {
        if (placeId != null) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val place = placeRepository.getPlace(placeId)
                _uiState.update {
                    if (place == null) {
                        it.copy(isLoading = false, notFound = true)
                    } else {
                        it.copy(
                            isLoading = false,
                            name = place.name,
                            latitude = place.latitude,
                            longitude = place.longitude,
                            radiusMeters = place.radiusMeters,
                            cameraRequest = CameraRequest(place.latitude, place.longitude),
                        )
                    }
                }
            }
        } else if (initialLatitude != null && initialLongitude != null) {
            setPoint(initialLatitude, initialLongitude)
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, showNameError = true) }
    }

    /** Snaps to [PlaceConfig.RADIUS_STEP_METERS] within the allowed range. */
    fun onRadiusChange(meters: Double) {
        val step = PlaceConfig.RADIUS_STEP_METERS
        val snapped = ((meters / step).roundToInt() * step)
            .coerceIn(PlaceConfig.MIN_RADIUS_METERS, PlaceConfig.MAX_RADIUS_METERS)
        _uiState.update { it.copy(radiusMeters = snapped) }
    }

    /** The user moved the map; the camera is already there, so no camera request. */
    fun onMapCenterPicked(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    fun useCurrentLocation() {
        if (locating?.isActive == true) return
        when {
            locationSource.permissionState() != LocationPermission.GRANTED ->
                _uiState.update { it.copy(message = EditorMessage.PERMISSION_RATIONALE) }
            !locationSource.isLocationEnabled() ->
                _uiState.update { it.copy(message = EditorMessage.LOCATION_DISABLED) }
            else -> locating = viewModelScope.launch {
                _uiState.update { it.copy(isLocating = true, message = null) }
                val fix = withTimeoutOrNull(PlaceConfig.CURRENT_LOCATION_TIMEOUT_MS) {
                    locationSource.fixes().first { it.accuracyMeters <= TrackingConfig.MAX_ACCURACY_METERS }
                }
                _uiState.update { it.copy(isLocating = false) }
                if (fix == null) {
                    _uiState.update { it.copy(message = EditorMessage.LOCATION_TIMEOUT) }
                } else {
                    setPoint(fix.latitude, fix.longitude)
                }
            }
        }
    }

    /** Result of the system permission dialog shown after [EditorMessage.PERMISSION_RATIONALE]. */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(message = null) }
        if (granted && locationSource.permissionState() == LocationPermission.GRANTED) {
            useCurrentLocation()
        } else {
            _uiState.update { it.copy(message = EditorMessage.PERMISSION_DENIED) }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun save() {
        val state = _uiState.value
        val latitude = state.latitude
        val longitude = state.longitude
        if (!state.canSave || latitude == null || longitude == null) return
        if (state.nameError != null) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            val input = PlaceInput(state.name, latitude, longitude, state.radiusMeters)
            val result = if (placeId == null) {
                placeRepository.createPlace(input)
            } else {
                placeRepository.updatePlace(placeId, input)
            }
            _uiState.update {
                if (result.isSuccess) it.copy(saved = true) else it.copy(message = EditorMessage.SAVE_FAILED)
            }
        }
    }

    private fun setPoint(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(latitude = latitude, longitude = longitude, cameraRequest = CameraRequest(latitude, longitude))
        }
    }

    companion object {
        const val ARG_PLACE_ID = "placeId"
        const val ARG_LATITUDE = "lat"
        const val ARG_LONGITUDE = "lng"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                val handle = createSavedStateHandle()
                PlaceEditorViewModel(
                    placeId = handle.get<String>(ARG_PLACE_ID),
                    initialLatitude = handle.get<String>(ARG_LATITUDE)?.toDoubleOrNull(),
                    initialLongitude = handle.get<String>(ARG_LONGITUDE)?.toDoubleOrNull(),
                    placeRepository = container.placeRepository,
                    locationSource = container.locationTracker,
                )
            }
        }
    }
}
