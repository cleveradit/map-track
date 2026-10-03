package com.radityodwiki.maptrack.ui.home

import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.location.StartTrackingError
import com.radityodwiki.maptrack.location.TrackingConfig

enum class GpsStatus { NO_PERMISSION, LOCATION_DISABLED, SEARCHING, ACTIVE }

data class HomeUiState(
    val permission: LocationPermission,
    val locationEnabled: Boolean,
    val fix: GpsFix?,
    val now: Long,
    /** Trip with status `active` in the database, whether or not the service is running. */
    val activeTrip: Trip? = null,
    val startError: StartTrackingError? = null,
    val busy: Boolean = false,
    val interruptedTrip: InterruptedTrip? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val mapFollowLocation: Boolean = true,
) {
    val gpsStatus: GpsStatus get() = gpsStatusOf(permission, locationEnabled, fix, now)
    val isTracking: Boolean get() = activeTrip != null
}

fun gpsStatusOf(permission: LocationPermission, locationEnabled: Boolean, fix: GpsFix?, now: Long): GpsStatus = when {
    permission != LocationPermission.GRANTED -> GpsStatus.NO_PERMISSION
    !locationEnabled -> GpsStatus.LOCATION_DISABLED
    fix == null || now - fix.time > TrackingConfig.STALE_FIX_MS -> GpsStatus.SEARCHING
    else -> GpsStatus.ACTIVE
}
