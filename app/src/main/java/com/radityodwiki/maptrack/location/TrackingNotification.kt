package com.radityodwiki.maptrack.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.radityodwiki.maptrack.MainActivity
import com.radityodwiki.maptrack.R
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.ui.format.formatCurrentSpeed
import com.radityodwiki.maptrack.ui.format.formatDuration

const val TRACKING_NOTIFICATION_ID = 1
private const val CHANNEL_ID = "tracking"

/** Body text of the tracking notification (PRD §9). */
fun trackingNotificationText(
    lastFix: GpsFix?,
    startedAt: Long,
    now: Long,
    locationEnabled: Boolean,
    unit: DistanceUnit = DistanceUnit.METRIC,
): String {
    val duration = "Durasi: ${formatDuration(now - startedAt)}"
    return when {
        !locationEnabled -> "Location service tidak aktif · $duration"
        lastFix == null || now - lastFix.time > TrackingConfig.STALE_FIX_MS -> "Menunggu sinyal GPS… · $duration"
        else -> "Kecepatan: ${formatCurrentSpeed(lastFix.speedMps, lastFix.time, now, unit)} · $duration"
    }
}

object TrackingNotification {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** [auto] marks an automatic trip: the title becomes "Perjalanan otomatis" (PRD §38 Fase 5). */
    fun build(context: Context, text: String, auto: Boolean = false): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            1,
            LocationTrackingService.stopIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tracking)
            .setContentTitle(context.getString(if (auto) R.string.auto_trip_notification_title else R.string.tracking_notification_title))
            .setContentText(text)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.tracking_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
