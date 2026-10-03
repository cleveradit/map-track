package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import kotlinx.coroutines.flow.Flow

/** Location access used by tracking and Home; implemented by [LocationTracker]. */
interface LocationSource {
    /** Current grant state. Never returns [LocationPermission.NOT_REQUESTED]. */
    fun permissionState(): LocationPermission

    fun isLocationEnabled(): Boolean

    fun fixes(): Flow<GpsFix>
}
