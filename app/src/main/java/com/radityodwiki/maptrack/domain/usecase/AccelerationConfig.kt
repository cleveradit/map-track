package com.radityodwiki.maptrack.domain.usecase

/** Single source of acceleration test parameters (PRD §38 Fase 5, Uji akselerasi). */
object AccelerationConfig {
    /** GPS interval while the acceleration screen is open. */
    const val GPS_INTERVAL_MS = 1_000L

    /** Ready only with fixes at least this accurate. */
    const val MAX_ACCURACY_METERS = 20f

    /** Below this speed the vehicle counts as standing still. */
    const val STILL_SPEED_MPS = 0.5

    /** How long the vehicle must stand still before the test is ready (inclusive). */
    const val READY_STILL_MS = 2_000L

    /** The run starts once the speed reaches this. */
    const val START_SPEED_MPS = 1.0

    /** A longer gap between fixes while measuring makes the run invalid. */
    const val MAX_FIX_GAP_MS = 3_000L

    /** Below [START_SPEED_MPS] for this long ends the run. */
    const val STOP_BELOW_START_MS = 2_000L

    /** A run never lasts longer than this from its start. */
    const val MAX_RUN_MS = 60_000L

    val DISTANCE_TARGETS_METERS = listOf(100, 200, 300, 400, 500)

    /** 100 km/h. */
    const val TARGET_SPEED_MPS = 100 / 3.6
}
