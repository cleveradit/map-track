package com.radityodwiki.maptrack.domain.model

import com.radityodwiki.maptrack.location.TrackingConfig

enum class DistanceUnit { METRIC, IMPERIAL }

/** User settings (PRD §38 Fase 4). Storage stays in meters and m/s; [distanceUnit] only affects display. */
data class AppSettings(
    val trackingIntervalMs: Long,
    val accuracyThresholdMeters: Float,
    val distanceUnit: DistanceUnit,
    val mapFollowLocation: Boolean,
    /** Opt-in automatic trips (PRD §38 Fase 5). */
    val autoTripEnabled: Boolean = false,
    val autoTripIncludeWalking: Boolean = false,
    /** Set when automatic trips were switched off because a permission was revoked. */
    val autoTripRevokedNotice: Boolean = false,
) {
    companion object {
        val DEFAULT = AppSettings(
            trackingIntervalMs = TrackingConfig.INTERVAL_MS,
            accuracyThresholdMeters = TrackingConfig.MAX_ACCURACY_METERS,
            distanceUnit = DistanceUnit.METRIC,
            mapFollowLocation = true,
        )
    }
}

/** Tracking parameters read once at Start and used until the trip ends. */
data class TrackingParams(val intervalMs: Long, val maxAccuracyMeters: Float) {
    companion object {
        val DEFAULT = TrackingParams(TrackingConfig.INTERVAL_MS, TrackingConfig.MAX_ACCURACY_METERS)
    }
}

fun AppSettings.trackingParams() = TrackingParams(trackingIntervalMs, accuracyThresholdMeters)
