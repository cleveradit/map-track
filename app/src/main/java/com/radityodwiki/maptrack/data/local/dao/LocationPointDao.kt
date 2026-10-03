package com.radityodwiki.maptrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.radityodwiki.maptrack.data.local.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    /** Returns the new row id, or -1 when an identical (trip_id, recorded_at) point already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: LocationPointEntity): Long

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId ORDER BY recorded_at ASC")
    suspend fun getForTrip(tripId: String): List<LocationPointEntity>

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId ORDER BY recorded_at ASC")
    fun observeForTrip(tripId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId AND recorded_at >= :since ORDER BY recorded_at ASC")
    suspend fun getForTripSince(tripId: String, since: Long): List<LocationPointEntity>

    @Query("SELECT * FROM location_points WHERE trip_id = :tripId ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLast(tripId: String): LocationPointEntity?

    @Query("SELECT COUNT(*) FROM location_points WHERE trip_id = :tripId")
    suspend fun countForTrip(tripId: String): Int
}
