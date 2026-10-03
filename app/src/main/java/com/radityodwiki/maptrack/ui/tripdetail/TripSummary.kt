package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.ui.format.formatDate
import com.radityodwiki.maptrack.ui.format.formatDistance
import com.radityodwiki.maptrack.ui.format.formatDuration
import com.radityodwiki.maptrack.ui.format.formatSpeedKmh
import com.radityodwiki.maptrack.ui.format.formatTime
import java.time.ZoneId

private const val NONE = "—"

/** Display values for the Trip Detail summary (PRD §19). Final statistics are "—" while active. */
data class TripSummary(
    val date: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val distance: String,
    val averageSpeed: String,
    val maxSpeed: String,
    val isActive: Boolean,
)

fun Trip.toSummary(zone: ZoneId = ZoneId.systemDefault()): TripSummary {
    val end = endedAt.takeIf { status == TripStatus.COMPLETED }
    return TripSummary(
        date = formatDate(startedAt, zone),
        startTime = formatTime(startedAt, zone),
        endTime = end?.let { formatTime(it, zone) } ?: NONE,
        duration = end?.let { formatDuration(it - startedAt) } ?: NONE,
        distance = distanceMeters?.takeIf { end != null }?.let(::formatDistance) ?: NONE,
        averageSpeed = averageSpeedMps?.takeIf { end != null }?.let(::formatSpeedKmh) ?: NONE,
        maxSpeed = maxSpeedMps?.takeIf { end != null }?.let(::formatSpeedKmh) ?: NONE,
        isActive = status == TripStatus.ACTIVE,
    )
}
