package com.radityodwiki.maptrack.domain.model

enum class TripStatus(val dbValue: String) {
    ACTIVE("active"),
    COMPLETED("completed");

    companion object {
        fun fromDbValue(value: String): TripStatus =
            entries.firstOrNull { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown trip status: $value")
    }
}

/** How a trip started (PRD §38 Fase 5). */
enum class TripSource(val dbValue: String) {
    MANUAL("manual"),
    AUTO("auto");

    companion object {
        fun fromDbValue(value: String): TripSource =
            entries.firstOrNull { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown trip source: $value")
    }
}

/**
 * One tracking session. Statistics are null while the trip is [TripStatus.ACTIVE].
 * Times are epoch millis UTC, speeds are m/s, distance is meters.
 */
data class Trip(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val distanceMeters: Double?,
    val averageSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val status: TripStatus,
    val updatedAt: Long,
    val source: TripSource = TripSource.MANUAL,
)
