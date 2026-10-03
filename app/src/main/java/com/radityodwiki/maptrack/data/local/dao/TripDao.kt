package com.radityodwiki.maptrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.radityodwiki.maptrack.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips ORDER BY started_at DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE status = 'active' LIMIT 1")
    suspend fun getActive(): TripEntity?

    @Query("SELECT * FROM trips WHERE status = 'active' LIMIT 1")
    fun observeActive(): Flow<TripEntity?>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
