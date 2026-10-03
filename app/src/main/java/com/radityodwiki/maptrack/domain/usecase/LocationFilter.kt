package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.location.TrackingConfig

enum class RejectReason { INVALID_COORDINATES, POOR_ACCURACY, OUT_OF_ORDER, GPS_JUMP }

/** Decides whether a GPS fix is stored (PRD §12). Rejected fixes are never persisted. */
object LocationFilter {

    /**
     * @param previous the last point already stored for the same trip, or null for the first one.
     * @return null when [candidate] is accepted, otherwise the reason it is rejected.
     */
    fun evaluate(candidate: LocationPoint, previous: LocationPoint?): RejectReason? {
        if (!hasValidCoordinates(candidate)) return RejectReason.INVALID_COORDINATES
        if (candidate.accuracyMeters > TrackingConfig.MAX_ACCURACY_METERS) return RejectReason.POOR_ACCURACY
        if (previous == null) return null
        if (candidate.recordedAt <= previous.recordedAt) return RejectReason.OUT_OF_ORDER

        val meters = GeoDistance.meters(previous.latitude, previous.longitude, candidate.latitude, candidate.longitude)
        val seconds = (candidate.recordedAt - previous.recordedAt) / 1000.0
        if (meters / seconds > TrackingConfig.MAX_JUMP_SPEED_MPS) return RejectReason.GPS_JUMP
        return null
    }

    private fun hasValidCoordinates(point: LocationPoint): Boolean =
        point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
}
