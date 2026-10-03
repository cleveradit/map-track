package com.radityodwiki.maptrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.radityodwiki.maptrack.domain.model.LocationPoint

@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["trip_id", "recorded_at"], unique = true)],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float?,
    val bearing: Float?,
    val altitude: Double?,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
)

fun LocationPointEntity.toDomain() = LocationPoint(
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
    speedMps = speed,
    bearingDegrees = bearing,
    altitudeMeters = altitude,
    recordedAt = recordedAt,
)

fun LocationPoint.toEntity() = LocationPointEntity(
    tripId = tripId,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracyMeters,
    speed = speedMps,
    bearing = bearingDegrees,
    altitude = altitudeMeters,
    recordedAt = recordedAt,
)
