package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface TrackingState {
    /** The tracking service is not running in this process. */
    data object Idle : TrackingState

    data class Active(val tripId: String, val startedAt: Long, val lastFix: GpsFix?) : TrackingState
}

/** In-process state published by [LocationTrackingService] and read by the UI. */
class TrackingStateHolder {
    val state = MutableStateFlow<TrackingState>(TrackingState.Idle)
}
