package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.usecase.GeoDistance

/**
 * Battery saver state for one tracking session (PRD §38 Fase 5). The user counts as still after
 * [AutoTripConfig.STATIONARY_DETECT_MS] within [AutoTripConfig.STATIONARY_RADIUS_METERS] of the first
 * still fix without a speed of [AutoTripConfig.STATIONARY_SPEED_MPS] or more. Not thread-safe.
 */
class StationaryDetector {
    private var anchor: GpsFix? = null

    var isStationary = false
        private set

    /** @return whether the user is still after this fix. */
    fun onFix(fix: GpsFix): Boolean {
        // Too coarse to tell still from moving; balanced-power fixes are often like this.
        if (fix.accuracyMeters > AutoTripConfig.STATIONARY_RADIUS_METERS) return isStationary

        val start = anchor
        val moving = fix.speedMps?.let { it >= AutoTripConfig.STATIONARY_SPEED_MPS } == true
        if (start == null || moving || distance(start, fix) > AutoTripConfig.STATIONARY_RADIUS_METERS) {
            anchor = fix
            isStationary = false
        } else if (fix.time - start.time >= AutoTripConfig.STATIONARY_DETECT_MS) {
            isStationary = true
        }
        return isStationary
    }

    private fun distance(a: GpsFix, b: GpsFix) = GeoDistance.meters(a.latitude, a.longitude, b.latitude, b.longitude)
}
