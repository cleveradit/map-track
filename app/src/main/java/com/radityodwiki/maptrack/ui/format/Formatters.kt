package com.radityodwiki.maptrack.ui.format

import com.radityodwiki.maptrack.location.TrackingConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val MPS_TO_KMH = 3.6
private const val UNKNOWN_SPEED = "— km/h"

/** Current speed on Home and in the notification (PRD §14). */
fun formatCurrentSpeed(speedMps: Float?, fixTime: Long?, now: Long): String {
    if (fixTime == null || now - fixTime > TrackingConfig.STALE_FIX_MS) return UNKNOWN_SPEED
    return formatSpeedKmh(speedMps?.toDouble())
}

fun formatSpeedKmh(speedMps: Double?): String {
    if (speedMps == null) return UNKNOWN_SPEED
    val kmh = speedMps * MPS_TO_KMH
    if (kmh < TrackingConfig.STATIONARY_SPEED_KMH) return "0 km/h"
    return "${kmh.roundToInt()} km/h"
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) {
        "${meters.roundToInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1000)
    }

fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"
}

/** Location accuracy on Home, e.g. "± 6 meter" (PRD §7.1). */
fun formatAccuracy(accuracyMeters: Float?): String =
    if (accuracyMeters == null) "—" else "± ${accuracyMeters.roundToInt()} meter"

private val INDONESIAN: Locale = Locale.forLanguageTag("id-ID")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", INDONESIAN)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", INDONESIAN)

/** e.g. "29 September 2026", in the device time zone. */
fun formatDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** e.g. "07:32", 24-hour clock, in the device time zone. */
fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
