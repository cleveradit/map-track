package com.radityodwiki.maptrack.ui.format

import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.location.TrackingConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val MPS_TO_KMH = 3.6
private const val MPS_TO_MPH = 2.236936
private const val METERS_TO_FEET = 3.28084
private const val METERS_PER_MILE = 1_609.344

/** Below this many miles, imperial distances are shown in feet. */
private const val MIN_MILES = 0.1

fun speedUnitLabel(unit: DistanceUnit): String = if (unit == DistanceUnit.METRIC) "km/h" else "mph"

/** Converts a stored m/s speed to the display unit (Rule 2: storage never changes). */
fun Double.toDisplaySpeed(unit: DistanceUnit): Double =
    this * if (unit == DistanceUnit.METRIC) MPS_TO_KMH else MPS_TO_MPH

/** Current speed on Home and in the notification (PRD §14). */
fun formatCurrentSpeed(speedMps: Float?, fixTime: Long?, now: Long, unit: DistanceUnit = DistanceUnit.METRIC): String {
    if (fixTime == null || now - fixTime > TrackingConfig.STALE_FIX_MS) return "— ${speedUnitLabel(unit)}"
    return formatSpeed(speedMps?.toDouble(), unit)
}

/** Speeds below [TrackingConfig.STATIONARY_SPEED_KMH] show as 0 in either unit. */
fun formatSpeed(speedMps: Double?, unit: DistanceUnit = DistanceUnit.METRIC): String {
    val label = speedUnitLabel(unit)
    if (speedMps == null) return "— $label"
    if (speedMps * MPS_TO_KMH < TrackingConfig.STATIONARY_SPEED_KMH) return "0 $label"
    return "${speedMps.toDisplaySpeed(unit).roundToInt()} $label"
}

fun formatDistance(meters: Double, unit: DistanceUnit = DistanceUnit.METRIC): String {
    if (unit == DistanceUnit.IMPERIAL) {
        val miles = meters / METERS_PER_MILE
        return if (miles < MIN_MILES) {
            "${(meters * METERS_TO_FEET).roundToInt()} ft"
        } else {
            String.format(Locale.US, "%.1f mi", miles)
        }
    }
    return if (meters < 1000) {
        "${meters.roundToInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1000)
    }
}

fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours jam $minutes menit" else "$minutes menit"
}

/** Acceleration test times, e.g. "6.94 s"; "—" when the target was not reached. */
fun formatSeconds(ms: Long?): String =
    if (ms == null) "—" else String.format(Locale.US, "%.2f s", ms / 1000.0)

/** Location accuracy on Home, e.g. "± 6 meter" or "± 20 ft" (PRD §7.1). */
fun formatAccuracy(accuracyMeters: Float?, unit: DistanceUnit = DistanceUnit.METRIC): String = when {
    accuracyMeters == null -> "—"
    unit == DistanceUnit.IMPERIAL -> "± ${(accuracyMeters * METERS_TO_FEET).roundToInt()} ft"
    else -> "± ${accuracyMeters.roundToInt()} meter"
}

private val INDONESIAN: Locale = Locale.forLanguageTag("id-ID")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", INDONESIAN)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", INDONESIAN)

/** e.g. "29 September 2026", in the device time zone. */
fun formatDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** e.g. "07:32", 24-hour clock, in the device time zone. */
fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
