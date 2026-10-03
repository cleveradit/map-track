package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.Place
import com.radityodwiki.maptrack.domain.model.Visit

/** Names visits by saved places on display (PRD §38 Fase 3). */
object PlaceMatcher {

    /**
     * The place whose radius contains the point (inclusive). With overlapping places the nearest
     * center wins; an exact tie goes to the smallest id so the result is deterministic.
     */
    fun match(latitude: Double, longitude: Double, places: List<Place>): Place? =
        places
            .map { it to GeoDistance.meters(latitude, longitude, it.latitude, it.longitude) }
            .filter { (place, distance) -> distance <= place.radiusMeters }
            .minWithOrNull(compareBy<Pair<Place, Double>> { it.second }.thenBy { it.first.id })
            ?.first

    /** Visits per place id, each visit under its single best place; visit order is kept. */
    fun group(visits: List<Visit>, places: List<Place>): Map<String, List<Visit>> =
        visits.mapNotNull { visit -> match(visit.centerLatitude, visit.centerLongitude, places)?.let { it.id to visit } }
            .groupBy({ it.first }, { it.second })
}
