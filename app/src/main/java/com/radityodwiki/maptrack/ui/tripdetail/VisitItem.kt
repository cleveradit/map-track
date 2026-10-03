package com.radityodwiki.maptrack.ui.tripdetail

import com.radityodwiki.maptrack.domain.model.Visit
import com.radityodwiki.maptrack.ui.format.formatDuration
import com.radityodwiki.maptrack.ui.format.formatTime
import java.time.ZoneId

/**
 * One stop in the Trip Detail list, e.g. "08:15 - 16:30" and "8 jam 15 menit" (PRD §38 Fase 2).
 * [placeName] is the matching saved place, or null for a plain "Tempat singgah" (Fase 3).
 */
data class VisitItem(val timeRange: String, val duration: String, val center: RoutePoint, val placeName: String?)

fun Visit.toVisitItem(placeName: String?, zone: ZoneId = ZoneId.systemDefault()) = VisitItem(
    timeRange = "${formatTime(arrivedAt, zone)} - ${formatTime(departedAt, zone)}",
    duration = formatDuration(durationMs),
    center = RoutePoint(centerLatitude, centerLongitude),
    placeName = placeName,
)

/** A tap on a visit item. Deliberately not a data class: tapping the same item again re-centers the map. */
class VisitFocusRequest(val center: RoutePoint)
