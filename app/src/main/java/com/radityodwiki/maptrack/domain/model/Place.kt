package com.radityodwiki.maptrack.domain.model

/**
 * A named place created by the user (PRD §38 Fase 3). Visits get its name when their center lies
 * within [radiusMeters]; the name is matched on display, never stored on the visit.
 */
data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val createdAt: Long,
    val updatedAt: Long,
)

/** User-editable fields of a [Place]; validated by the repository. */
data class PlaceInput(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)
