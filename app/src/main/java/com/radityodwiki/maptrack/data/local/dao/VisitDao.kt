package com.radityodwiki.maptrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.radityodwiki.maptrack.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Insert
    suspend fun insertAll(visits: List<VisitEntity>)

    @Query("DELETE FROM visits WHERE trip_id = :tripId")
    suspend fun deleteForTrip(tripId: String): Int

    @Query("SELECT * FROM visits WHERE trip_id = :tripId ORDER BY arrived_at ASC")
    suspend fun getForTrip(tripId: String): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE trip_id = :tripId ORDER BY arrived_at ASC")
    fun observeForTrip(tripId: String): Flow<List<VisitEntity>>
}
