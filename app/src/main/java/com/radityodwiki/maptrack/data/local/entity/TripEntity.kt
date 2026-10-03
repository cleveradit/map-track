package com.radityodwiki.maptrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripStatus

@Entity(
    tableName = "trips",
    indices = [Index("status"), Index("started_at")],
)
data class TripEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double?,
    @ColumnInfo(name = "average_speed") val averageSpeed: Double?,
    @ColumnInfo(name = "max_speed") val maxSpeed: Double?,
    val status: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun TripEntity.toDomain() = Trip(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    distanceMeters = distanceMeters,
    averageSpeedMps = averageSpeed,
    maxSpeedMps = maxSpeed,
    status = TripStatus.fromDbValue(status),
    updatedAt = updatedAt,
)

fun Trip.toEntity() = TripEntity(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    distanceMeters = distanceMeters,
    averageSpeed = averageSpeedMps,
    maxSpeed = maxSpeedMps,
    status = status.dbValue,
    updatedAt = updatedAt,
)
