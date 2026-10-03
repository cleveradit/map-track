package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.data.settings.SettingsRepository
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.TripSource

/** What the Settings page needs to switch automatic trips; implemented by [AutoTripController]. */
interface AutoTripToggle {
    /** @return true when automatic trips are now on. */
    suspend fun enable(): Boolean
    suspend fun disable()
    suspend fun setIncludeWalking(enabled: Boolean)
}

/**
 * Opt-in automatic trips (PRD §38 Fase 5): keeps the transition subscription in line with the
 * setting and permissions, and starts an automatic trip on a movement transition.
 */
class AutoTripController(
    private val settings: SettingsRepository,
    private val permissions: AutoTripPermissions,
    private val transitions: ActivityTransitions,
    private val locationSource: LocationSource,
    private val tracking: TrackingController,
    private val stillness: StillnessHolder,
    private val clock: () -> Long = System::currentTimeMillis,
) : AutoTripToggle {

    private fun hasAllPermissions() =
        locationSource.permissionState() == LocationPermission.GRANTED &&
            permissions.hasActivityRecognition() &&
            permissions.hasBackgroundLocation()

    /** Call after the user granted the permissions. @return true when automatic trips are now on. */
    override suspend fun enable(): Boolean {
        if (!hasAllPermissions()) return false
        val includeWalking = settings.current().autoTripIncludeWalking
        if (transitions.subscribe(includeWalking).isFailure) return false
        settings.setAutoTrip(true)
        settings.setAutoTripRevokedNotice(false)
        return true
    }

    /** A running automatic trip keeps going until it stops. */
    override suspend fun disable() {
        settings.setAutoTrip(false)
        transitions.unsubscribe()
    }

    override suspend fun setIncludeWalking(enabled: Boolean) {
        settings.setAutoTripIncludeWalking(enabled)
        if (settings.current().autoTripEnabled) transitions.subscribe(enabled)
    }

    /** On every process start and after reboot: resubscribe, or switch off when a permission was revoked. */
    suspend fun reconcile() {
        val current = settings.current()
        if (!current.autoTripEnabled) return
        if (hasAllPermissions() && transitions.subscribe(current.autoTripIncludeWalking).isSuccess) return
        settings.setAutoTrip(false)
        settings.setAutoTripRevokedNotice(true)
        transitions.unsubscribe()
    }

    suspend fun onTransitions(events: List<TransitionEvent>) {
        val current = settings.current()
        if (!current.autoTripEnabled) return
        for (event in events) {
            when {
                event.activity == MotionActivity.STILL ->
                    stillness.stillSince = if (event.enter) clock() else null
                event.enter && isTrigger(event.activity, current.autoTripIncludeWalking) -> {
                    stillness.stillSince = null
                    startAutoTrip()
                }
            }
        }
    }

    private fun isTrigger(activity: MotionActivity, includeWalking: Boolean) = when (activity) {
        MotionActivity.IN_VEHICLE, MotionActivity.ON_BICYCLE -> true
        MotionActivity.WALKING, MotionActivity.RUNNING -> includeWalking
        MotionActivity.STILL -> false
    }

    /** Ignored silently when a trip is active, a permission is missing, or Location is off. */
    private suspend fun startAutoTrip() {
        if (!hasAllPermissions()) return
        tracking.start(TripSource.AUTO)
    }
}
