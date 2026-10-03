package com.radityodwiki.maptrack.domain.usecase

enum class PlaceNameError { EMPTY, TOO_LONG }

/** Rules for user-entered place fields (PRD §38 Fase 3). The name is checked after trimming. */
object PlaceValidator {

    fun nameError(name: String): PlaceNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> PlaceNameError.EMPTY
            trimmed.codePointCount(0, trimmed.length) > PlaceConfig.NAME_MAX_LENGTH -> PlaceNameError.TOO_LONG
            else -> null
        }
    }

    fun isValidRadius(meters: Double): Boolean =
        meters in PlaceConfig.MIN_RADIUS_METERS..PlaceConfig.MAX_RADIUS_METERS

    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}
