package com.radityodwiki.maptrack.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.usecase.PlaceMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class PlacesViewModel(
    placeRepository: PlaceRepository,
    tripRepository: TripRepository,
    settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT),
) : ViewModel() {

    /** Sorted by name; null while loading. Visit counts follow place and visit changes live. */
    val items: StateFlow<List<PlaceListItem>?> = combine(
        placeRepository.observePlaces(),
        tripRepository.observeAllVisits(),
        settings,
    ) { places, visits, settings ->
        val byPlace = PlaceMatcher.group(visits, places)
        places.map { it.toListItem(byPlace[it.id].orEmpty(), settings.distanceUnit) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                PlacesViewModel(container.placeRepository, container.tripRepository, container.settingsRepository.settings)
            }
        }
    }
}
