package com.radityodwiki.maptrack.ui.places

import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.Place
import com.radityodwiki.maptrack.domain.model.Visit
import com.radityodwiki.maptrack.ui.format.formatDate
import com.radityodwiki.maptrack.ui.format.formatDistance
import java.time.ZoneId

/** One row of the Tempat list (PRD §38 Fase 3). */
data class PlaceListItem(
    val placeId: String,
    val name: String,
    val radius: String,
    val visitCount: Int,
    /** Arrival date of the latest matching visit; null without visits. */
    val lastVisitDate: String?,
)

/** [visits] are the visits matched to this place. */
fun Place.toListItem(
    visits: List<Visit>,
    unit: DistanceUnit = DistanceUnit.METRIC,
    zone: ZoneId = ZoneId.systemDefault(),
) = PlaceListItem(
    placeId = id,
    name = name,
    radius = formatDistance(radiusMeters, unit),
    visitCount = visits.size,
    lastVisitDate = visits.maxOfOrNull { it.arrivedAt }?.let { formatDate(it, zone) },
)
