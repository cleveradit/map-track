package com.radityodwiki.maptrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.radityodwiki.maptrack.MapTrackApplication
import com.radityodwiki.maptrack.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: TripRepository) : ViewModel() {

    /** Newest first; null while the first query is loading. */
    val items: StateFlow<List<HistoryItem>?> = repository.observeTrips()
        .map { trips -> trips.map { it.toHistoryItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Active trips are refused by the repository and stay in the list. */
    fun deleteTrip(tripId: String) {
        viewModelScope.launch { repository.deleteTrip(tripId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as MapTrackApplication).container
                HistoryViewModel(container.tripRepository)
            }
        }
    }
}
