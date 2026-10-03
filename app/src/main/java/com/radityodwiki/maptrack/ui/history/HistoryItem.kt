package com.radityodwiki.maptrack.ui.history

import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.ui.format.formatDate
import com.radityodwiki.maptrack.ui.format.formatDistance
import com.radityodwiki.maptrack.ui.format.formatDuration
import com.radityodwiki.maptrack.ui.format.formatTime
import java.time.ZoneId

/** One row of the History list (PRD §18). Statistics are null while the trip is active. */
data class HistoryItem(
    val tripId: String,
    val date: String,
    val timeRange: String,
    val distance: String?,
    val duration: String?,
    val isActive: Boolean,
)

fun Trip.toHistoryItem(zone: ZoneId = ZoneId.systemDefault()): HistoryItem {
    val isActive = status == TripStatus.ACTIVE
    val end = endedAt
    return HistoryItem(
        tripId = id,
        date = formatDate(startedAt, zone),
        timeRange = "${formatTime(startedAt, zone)} - ${if (end != null) formatTime(end, zone) else "…"}",
        distance = distanceMeters?.takeUnless { isActive }?.let(::formatDistance),
        duration = end?.takeUnless { isActive }?.let { formatDuration(it - startedAt) },
        isActive = isActive,
    )
}
