package com.radityodwiki.maptrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.radityodwiki.maptrack.data.local.entity.AccelerationRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccelerationRunDao {

    @Insert
    suspend fun insert(run: AccelerationRunEntity): Long

    @Query("SELECT * FROM acceleration_runs ORDER BY started_at DESC")
    fun observeAll(): Flow<List<AccelerationRunEntity>>

    @Query("DELETE FROM acceleration_runs WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
