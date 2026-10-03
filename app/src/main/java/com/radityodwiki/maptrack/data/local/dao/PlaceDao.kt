package com.radityodwiki.maptrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.radityodwiki.maptrack.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Insert
    suspend fun insert(place: PlaceEntity)

    @Update
    suspend fun update(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getById(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE id = :id")
    fun observeById(id: String): Flow<PlaceEntity?>

    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<PlaceEntity>>
}
