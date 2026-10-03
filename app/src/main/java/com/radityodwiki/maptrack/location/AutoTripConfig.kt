package com.radityodwiki.maptrack.location

/** Single source of Fase 5 parameters: automatic trips, resume, and battery saving (PRD §38 Fase 5). */
object AutoTripConfig {
    /** An automatic trip stops after being still this long. */
    const val AUTO_STOP_STILL_MS = 5 * 60_000L

    /** "Still" = every point of the last [AUTO_STOP_STILL_MS] lies within this radius. */
    const val AUTO_STOP_RADIUS_METERS = 100.0

    /** Automatic trips shorter than this distance or [MIN_AUTO_TRIP_DURATION_MS] are deleted after stopping. */
    const val MIN_AUTO_TRIP_DISTANCE_METERS = 300.0
    const val MIN_AUTO_TRIP_DURATION_MS = 2 * 60_000L

    /** How often the tracking service checks whether an automatic trip has gone still. */
    const val AUTO_STOP_CHECK_MS = 30_000L

    /** An interrupted trip can be resumed when its last data is at most this old. */
    const val RESUME_MAX_GAP_MS = 60 * 60_000L

    /** GPS interval while the battery saver detects the user is still (priority stays high accuracy). */
    const val STATIONARY_INTERVAL_MS = 30_000L

    /** Below this speed the user counts as still, for the battery saver and jitter damping. */
    const val STATIONARY_SPEED_MPS = 0.5f

    /** The battery saver kicks in after this long within [STATIONARY_RADIUS_METERS] below [STATIONARY_SPEED_MPS]. */
    const val STATIONARY_DETECT_MS = 2 * 60_000L
    const val STATIONARY_RADIUS_METERS = 100.0
}

/** An interrupted trip is resumable while its last data is at most [AutoTripConfig.RESUME_MAX_GAP_MS] old (inclusive). */
fun canResumeTrip(lastDataAt: Long, now: Long): Boolean = now - lastDataAt <= AutoTripConfig.RESUME_MAX_GAP_MS
