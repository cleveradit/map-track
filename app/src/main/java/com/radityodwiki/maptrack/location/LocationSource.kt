package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import kotlinx.coroutines.flow.Flow

/** Interval and priority of a location request; tracking uses the trip's snapshot (PRD §38 Fase 4). */
data class LocationRequestSpec(val intervalMs: Long, val highAccuracy: Boolean) {
    companion object {
        val DEFAULT = LocationRequestSpec(TrackingConfig.INTERVAL_MS, highAccuracy = true)
    }
}

/** Location access used by tracking and Home; implemented by [LocationTracker]. */
interface LocationSource {
    /** Current grant state. Never returns [LocationPermission.NOT_REQUESTED]. */
    fun permissionState(): LocationPermission

    fun isLocationEnabled(): Boolean

    fun fixes(request: LocationRequestSpec = LocationRequestSpec.DEFAULT): Flow<GpsFix>
}
