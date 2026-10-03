package com.radityodwiki.maptrack.location

/**
 * Single source of tracking and filtering parameters (PRD §11, §12, §14).
 */
object TrackingConfig {
    /** Requested location update interval while tracking. */
    const val INTERVAL_MS = 5_000L

    /** Fastest accepted location update interval. */
    const val MIN_UPDATE_INTERVAL_MS = 5_000L

    /** Fixes with worse accuracy than this are rejected. */
    const val MAX_ACCURACY_METERS = 50f

    /** Implied speed between consecutive fixes above this is treated as a GPS jump. */
    const val MAX_JUMP_SPEED_MPS = 70.0

    /** A fix older than this is stale; current speed is shown as unknown. */
    const val STALE_FIX_MS = 15_000L

    /** Speeds below this are displayed as 0 km/h. */
    const val STATIONARY_SPEED_KMH = 1.0
}
