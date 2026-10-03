package com.radityodwiki.maptrack.domain.usecase

/**
 * Single source of visit detection parameters (PRD §38 Fase 2).
 */
object PlaceDetectionConfig {
    /** A point still belongs to a cluster when it is at most this far from the cluster center. */
    const val VISIT_RADIUS_METERS = 100.0

    /** Minimum time between the first and last point of a cluster for it to become a visit. */
    const val MIN_VISIT_DURATION_MS = 5 * 60_000L

    /** Consecutive visits are merged when the gap between them is at most this... */
    const val MERGE_GAP_MS = 5 * 60_000L

    /** ...and their centers are at most this far apart. */
    const val MERGE_DISTANCE_METERS = 100.0

    /** Algorithm version; raising it makes completed trips recompute their visits. */
    const val DETECTION_VERSION = 1
}
