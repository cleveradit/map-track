package com.radityodwiki.maptrack.location

/**
 * Single source of tracking and filtering parameters (PRD §11, §12, §14).
 */
object TrackingConfig {
    /** Default location update interval while tracking; the user can pick another from Fase 4. */
    const val INTERVAL_MS = 5_000L

    /** Interval choices in Settings (PRD §38 Fase 4). */
    val INTERVAL_OPTIONS_MS = listOf(3_000L, 5_000L, 10_000L, 30_000L)

    /** Default accuracy threshold: fixes with worse accuracy are rejected. */
    const val MAX_ACCURACY_METERS = 50f

    /** Accuracy threshold choices in Settings (PRD §38 Fase 4). */
    val ACCURACY_OPTIONS_METERS = listOf(20f, 30f, 50f, 100f)

    /** Implied speed between consecutive fixes above this is treated as a GPS jump. */
    const val MAX_JUMP_SPEED_MPS = 70.0

    /** A fix older than this is stale; current speed is shown as unknown. */
    const val STALE_FIX_MS = 15_000L

    /** Speeds below this are displayed as 0 km/h. */
    const val STATIONARY_SPEED_KMH = 1.0
}
