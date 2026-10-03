package com.radityodwiki.maptrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.radityodwiki.maptrack.domain.model.Visit

/** Derived from the trip's location points; recomputed by deleting and inserting (PRD §38 Fase 2). */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["trip_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trip_id"), Index(value = ["trip_id", "arrived_at"], unique = true)],
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trip_id") val tripId: String,
    @ColumnInfo(name = "arrived_at") val arrivedAt: Long,
    @ColumnInfo(name = "departed_at") val departedAt: Long,
    @ColumnInfo(name = "center_latitude") val centerLatitude: Double,
    @ColumnInfo(name = "center_longitude") val centerLongitude: Double,
    @ColumnInfo(name = "point_count") val pointCount: Int,
)

fun VisitEntity.toDomain() = Visit(
    tripId = tripId,
    arrivedAt = arrivedAt,
    departedAt = departedAt,
    centerLatitude = centerLatitude,
    centerLongitude = centerLongitude,
    pointCount = pointCount,
)

fun Visit.toEntity() = VisitEntity(
    tripId = tripId,
    arrivedAt = arrivedAt,
    departedAt = departedAt,
    centerLatitude = centerLatitude,
    centerLongitude = centerLongitude,
    pointCount = pointCount,
)
