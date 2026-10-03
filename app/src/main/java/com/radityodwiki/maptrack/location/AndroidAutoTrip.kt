package com.radityodwiki.maptrack.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Task
import com.radityodwiki.maptrack.MapTrackApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidAutoTripPermissions(context: Context) : AutoTripPermissions {
    private val appContext = context.applicationContext

    override fun hasActivityRecognition() = isGranted(Manifest.permission.ACTIVITY_RECOGNITION)

    override fun hasBackgroundLocation() = isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}

/** Activity Recognition Transition API: event based, no polling (PRD §38 Fase 5). */
class GmsActivityTransitions(context: Context) : ActivityTransitions {
    private val appContext = context.applicationContext
    private val client = ActivityRecognition.getClient(appContext)

    // Mutable: Play services fills in the transition result.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, ActivityTransitionReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    // AutoTripController checks ACTIVITY_RECOGNITION before subscribing.
    @SuppressLint("MissingPermission")
    override suspend fun subscribe(includeWalking: Boolean): Result<Unit> = runCatching {
        val moving = buildList {
            add(DetectedActivity.IN_VEHICLE)
            add(DetectedActivity.ON_BICYCLE)
            if (includeWalking) {
                add(DetectedActivity.WALKING)
                add(DetectedActivity.RUNNING)
            }
        }
        val transitions = moving.map { transition(it, ActivityTransition.ACTIVITY_TRANSITION_ENTER) } +
            transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER) +
            transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        client.requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent()).await()
    }

    @SuppressLint("MissingPermission")
    override suspend fun unsubscribe() {
        runCatching { client.removeActivityTransitionUpdates(pendingIntent()).await() }
    }

    private fun transition(activity: Int, type: Int) =
        ActivityTransition.Builder().setActivityType(activity).setActivityTransition(type).build()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}

private fun ActivityTransitionEvent.toEvent(): TransitionEvent? {
    val activity = when (activityType) {
        DetectedActivity.IN_VEHICLE -> MotionActivity.IN_VEHICLE
        DetectedActivity.ON_BICYCLE -> MotionActivity.ON_BICYCLE
        DetectedActivity.WALKING -> MotionActivity.WALKING
        DetectedActivity.RUNNING -> MotionActivity.RUNNING
        DetectedActivity.STILL -> MotionActivity.STILL
        else -> return null
    }
    return TransitionEvent(activity, transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER)
}

/** Receives activity transitions; starting the location service from here is allowed from the background. */
class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val events = ActivityTransitionResult.extractResult(intent)?.transitionEvents.orEmpty().mapNotNull { it.toEvent() }
        val container = (context.applicationContext as MapTrackApplication).container
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                container.autoTripController.onTransitions(events)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Transition subscriptions do not survive a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = (context.applicationContext as MapTrackApplication).container
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                container.autoTripController.reconcile()
            } finally {
                pending.finish()
            }
        }
    }
}
