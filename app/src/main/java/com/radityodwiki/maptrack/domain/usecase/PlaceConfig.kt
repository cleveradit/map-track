package com.radityodwiki.maptrack.domain.usecase

/** Single source of saved place limits (PRD §38 Fase 3). */
object PlaceConfig {
    const val DEFAULT_RADIUS_METERS = 100.0
    const val MIN_RADIUS_METERS = 50.0
    const val MAX_RADIUS_METERS = 1_000.0

    /** The radius slider moves in steps of this size. */
    const val RADIUS_STEP_METERS = 10.0

    /** Counted in code points after trimming, so an emoji is one character. */
    const val NAME_MAX_LENGTH = 50

    /** "Pakai lokasi saat ini" gives up when no accurate fix arrives within this time. */
    const val CURRENT_LOCATION_TIMEOUT_MS = 30_000L
}
