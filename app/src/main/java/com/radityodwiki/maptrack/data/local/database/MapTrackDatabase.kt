package com.radityodwiki.maptrack.data.local.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.radityodwiki.maptrack.data.local.dao.LocationPointDao
import com.radityodwiki.maptrack.data.local.dao.TripDao
import com.radityodwiki.maptrack.data.local.dao.VisitDao
import com.radityodwiki.maptrack.data.local.entity.LocationPointEntity
import com.radityodwiki.maptrack.data.local.entity.TripEntity
import com.radityodwiki.maptrack.data.local.entity.VisitEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class, VisitEntity::class],
    version = 2,
    exportSchema = true,
    // 1 → 2 (Fase 2): table visits + trips.visit_detection_version. Purely additive.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MapTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun visitDao(): VisitDao

    companion object {
        private const val FILE_NAME = "map_track.db"

        fun create(context: Context): MapTrackDatabase =
            Room.databaseBuilder(context.applicationContext, MapTrackDatabase::class.java, FILE_NAME)
                .build()
    }
}
