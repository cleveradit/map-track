package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.usecase.GeoDistance

/** Activities the app subscribes to with the Activity Recognition Transition API (PRD §38 Fase 5). */
enum class MotionActivity { IN_VEHICLE, ON_BICYCLE, WALKING, RUNNING, STILL }

data class TransitionEvent(val activity: MotionActivity, val enter: Boolean)

/** Runtime permissions automatic trips need on top of precise location; abstracted for tests. */
interface AutoTripPermissions {
    fun hasActivityRecognition(): Boolean
    fun hasBackgroundLocation(): Boolean
}

/** Subscription to activity transitions; abstracted for tests. */
interface ActivityTransitions {
    suspend fun subscribe(includeWalking: Boolean): Result<Unit>
    suspend fun unsubscribe()
}

/** When Activity Recognition last reported STILL without movement since; in-process only. */
class StillnessHolder {
    @Volatile
    var stillSince: Long? = null
}

/** Whether an automatic trip should end (PRD §38 Fase 5). Never used for manual trips. */
object AutoStopPolicy {

    /**
     * @param recentPoints stored points of the last [AutoTripConfig.AUTO_STOP_STILL_MS], in time order.
     * @param stillSince when STILL was entered without movement since, or null.
     */
    fun shouldStop(recentPoints: List<LocationPoint>, tripStartedAt: Long, now: Long, stillSince: Long?): Boolean {
        if (stillSince != null && now - stillSince >= AutoTripConfig.AUTO_STOP_STILL_MS) return true
        if (now - tripStartedAt < AutoTripConfig.AUTO_STOP_STILL_MS) return false
        val first = recentPoints.firstOrNull() ?: return false
        return recentPoints.all {
            GeoDistance.meters(first.latitude, first.longitude, it.latitude, it.longitude) <= AutoTripConfig.AUTO_STOP_RADIUS_METERS
        }
    }
}

/** Automatic trips below the minimum distance or duration are deleted after they end (PRD §38 Fase 5). */
fun Trip.isTooShortAutoTrip(): Boolean {
    if (source != TripSource.AUTO) return false
    val end = endedAt ?: return false
    return (distanceMeters ?: 0.0) < AutoTripConfig.MIN_AUTO_TRIP_DISTANCE_METERS ||
        end - startedAt < AutoTripConfig.MIN_AUTO_TRIP_DURATION_MS
}
