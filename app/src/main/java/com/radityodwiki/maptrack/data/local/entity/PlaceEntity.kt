package com.radityodwiki.maptrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.radityodwiki.maptrack.domain.model.Place

/** User-owned and sync-ready: client-generated UUID and maintained updated_at (Rule 9). */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun PlaceEntity.toDomain() = Place(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Place.toEntity() = PlaceEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
