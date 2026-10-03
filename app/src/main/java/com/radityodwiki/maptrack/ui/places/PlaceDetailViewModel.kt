package com.radityodwiki.maptrack.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.Visit
import com.radityodwiki.maptrack.domain.usecase.PlaceMatcher
import com.radityodwiki.maptrack.ui.format.formatDate
import com.radityodwiki.maptrack.ui.format.formatDistance
import com.radityodwiki.maptrack.ui.format.formatDuration
import com.radityodwiki.maptrack.ui.format.formatTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

/** One visit in the place detail list; tapping it opens [tripId]. */
data class PlaceVisitItem(val tripId: String, val date: String, val timeRange: String, val duration: String)

fun Visit.toPlaceVisitItem(zone: ZoneId = ZoneId.systemDefault()) = PlaceVisitItem(
    tripId = tripId,
    date = formatDate(arrivedAt, zone),
    timeRange = "${formatTime(arrivedAt, zone)} - ${formatTime(departedAt, zone)}",
    duration = formatDuration(durationMs),
)

sealed interface PlaceDetailUiState {
    data object Loading : PlaceDetailUiState
    data object NotFound : PlaceDetailUiState
    data class Loaded(
        val name: String,
        val radius: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val visitCount: Int,
        val totalDuration: String,
        /** Newest first. */
        val visits: List<PlaceVisitItem>,
    ) : PlaceDetailUiState
}

class PlaceDetailViewModel(
    val placeId: String,
    private val placeRepository: PlaceRepository,
    tripRepository: TripRepository,
) : ViewModel() {

    /** Visits are those whose nearest matching place is this one (PRD §38 Fase 3). */
    val uiState: StateFlow<PlaceDetailUiState> = combine(
        placeRepository.observePlace(placeId),
        placeRepository.observePlaces(),
        tripRepository.observeAllVisits(),
    ) { place, places, visits ->
        if (place == null) {
            PlaceDetailUiState.NotFound
        } else {
            val matched = PlaceMatcher.group(visits, places)[place.id].orEmpty()
            PlaceDetailUiState.Loaded(
                name = place.name,
                radius = formatDistance(place.radiusMeters),
                latitude = place.latitude,
                longitude = place.longitude,
                radiusMeters = place.radiusMeters,
                visitCount = matched.size,
                totalDuration = formatDuration(matched.sumOf { it.durationMs }),
                visits = matched.sortedByDescending { it.arrivedAt }.map { it.toPlaceVisitItem() },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceDetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Removes only the place; trips and visits stay and simply lose the name. */
    fun delete() {
        viewModelScope.launch {
            placeRepository.deletePlace(placeId)
            _deleted.value = true
        }
    }

    companion object {
        const val ARG_PLACE_ID = "placeId"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                val placeId = createSavedStateHandle().get<String>(ARG_PLACE_ID).orEmpty()
                PlaceDetailViewModel(placeId, container.placeRepository, container.tripRepository)
            }
        }
    }
}
