package com.radityodwiki.maptrack.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.radityodwiki.maptrack.domain.model.AccelerationRun

/** Local only, never synced (PRD §38 Fase 5, v2.6). One column pair per distance target. */
@Entity(tableName = "acceleration_runs")
data class AccelerationRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "time_100m_ms") val time100mMs: Long?,
    @ColumnInfo(name = "time_200m_ms") val time200mMs: Long?,
    @ColumnInfo(name = "time_300m_ms") val time300mMs: Long?,
    @ColumnInfo(name = "time_400m_ms") val time400mMs: Long?,
    @ColumnInfo(name = "time_500m_ms") val time500mMs: Long?,
    @ColumnInfo(name = "speed_at_100m_mps") val speedAt100mMps: Double?,
    @ColumnInfo(name = "speed_at_200m_mps") val speedAt200mMps: Double?,
    @ColumnInfo(name = "speed_at_300m_mps") val speedAt300mMps: Double?,
    @ColumnInfo(name = "speed_at_400m_mps") val speedAt400mMps: Double?,
    @ColumnInfo(name = "speed_at_500m_mps") val speedAt500mMps: Double?,
    @ColumnInfo(name = "time_0_100_kmh_ms") val time0To100KmhMs: Long?,
    @ColumnInfo(name = "max_speed_mps") val maxSpeedMps: Double,
)

fun AccelerationRunEntity.toDomain() = AccelerationRun(
    id = id,
    startedAt = startedAt,
    distanceTimesMs = listOf(100 to time100mMs, 200 to time200mMs, 300 to time300mMs, 400 to time400mMs, 500 to time500mMs)
        .mapNotNull { (m, t) -> t?.let { m to it } }.toMap(),
    distanceSpeedsMps = listOf(
        100 to speedAt100mMps,
        200 to speedAt200mMps,
        300 to speedAt300mMps,
        400 to speedAt400mMps,
        500 to speedAt500mMps,
    ).mapNotNull { (m, v) -> v?.let { m to it } }.toMap(),
    time0To100KmhMs = time0To100KmhMs,
    maxSpeedMps = maxSpeedMps,
)

fun AccelerationRun.toEntity() = AccelerationRunEntity(
    id = id,
    startedAt = startedAt,
    time100mMs = distanceTimesMs[100],
    time200mMs = distanceTimesMs[200],
    time300mMs = distanceTimesMs[300],
    time400mMs = distanceTimesMs[400],
    time500mMs = distanceTimesMs[500],
    speedAt100mMps = distanceSpeedsMps[100],
    speedAt200mMps = distanceSpeedsMps[200],
    speedAt300mMps = distanceSpeedsMps[300],
    speedAt400mMps = distanceSpeedsMps[400],
    speedAt500mMps = distanceSpeedsMps[500],
    time0To100KmhMs = time0To100KmhMs,
    maxSpeedMps = maxSpeedMps,
)
