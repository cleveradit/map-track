package com.radityodwiki.maptrack.ui.tripdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.usecase.PlaceMatcher
import com.radityodwiki.maptrack.ui.format.speedUnitLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface TripDetailUiState {
    data object Loading : TripDetailUiState
    data object NotFound : TripDetailUiState
    data class Loaded(
        val summary: TripSummary,
        val pointCount: Int,
        val samples: List<SpeedSample>,
        val route: List<RoutePoint>,
        /** Always empty while the trip is active (PRD §38 Fase 2). */
        val visits: List<VisitItem>,
        /** Unit of the speed chart, "km/h" or "mph". */
        val speedUnit: String = "km/h",
    ) : TripDetailUiState
}

class TripDetailViewModel(
    tripId: String,
    repository: TripRepository,
    placeRepository: PlaceRepository,
    settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT),
) : ViewModel() {

    /** Live while the trip is active: new points update the count and the chart. */
    val uiState: StateFlow<TripDetailUiState> = combine(
        repository.observeTrip(tripId),
        repository.observePoints(tripId),
        repository.observeVisits(tripId),
        placeRepository.observePlaces(),
        settings,
    ) { trip, points, visits, places, settings ->
        val unit = settings.distanceUnit
        if (trip == null) {
            TripDetailUiState.NotFound
        } else {
            val summary = trip.toSummary(unit)
            TripDetailUiState.Loaded(
                summary = summary,
                pointCount = points.size,
                samples = speedSeries(points, trip.startedAt, unit),
                route = points.map { it.toRoutePoint() },
                visits = if (summary.isActive) {
                    emptyList()
                } else {
                    visits.map { it.toVisitItem(PlaceMatcher.match(it.centerLatitude, it.centerLongitude, places)?.name) }
                },
                speedUnit = speedUnitLabel(unit),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState.Loading)

    companion object {
        const val ARG_TRIP_ID = "tripId"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                val tripId = createSavedStateHandle().get<String>(ARG_TRIP_ID).orEmpty()
                TripDetailViewModel(tripId, container.tripRepository, container.placeRepository, container.settingsRepository.settings)
            }
        }
    }
}

