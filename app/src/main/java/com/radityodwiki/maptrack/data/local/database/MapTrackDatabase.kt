package com.radityodwiki.maptrack.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.radityodwiki.maptrack.data.local.dao.LocationPointDao
import com.radityodwiki.maptrack.data.local.dao.TripDao
import com.radityodwiki.maptrack.data.local.entity.LocationPointEntity
import com.radityodwiki.maptrack.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MapTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationPointDao(): LocationPointDao

    companion object {
        private const val FILE_NAME = "map_track.db"

        fun create(context: Context): MapTrackDatabase =
            Room.databaseBuilder(context.applicationContext, MapTrackDatabase::class.java, FILE_NAME)
                .build()
    }
}
