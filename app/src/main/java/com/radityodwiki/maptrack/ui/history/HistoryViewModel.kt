package com.radityodwiki.maptrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: TripRepository,
    settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT),
) : ViewModel() {

    /** Newest first; null while the first query is loading. */
    val items: StateFlow<List<HistoryItem>?> = combine(repository.observeTrips(), settings) { trips, settings ->
        trips.map { it.toHistoryItem(settings.distanceUnit) }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Active trips are refused by the repository and stay in the list. */
    fun deleteTrip(tripId: String) {
        viewModelScope.launch { repository.deleteTrip(tripId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                HistoryViewModel(container.tripRepository, container.settingsRepository.settings)
            }
        }
    }
}
