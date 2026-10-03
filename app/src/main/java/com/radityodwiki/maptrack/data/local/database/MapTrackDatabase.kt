package com.radityodwiki.maptrack.data.local.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.radityodwiki.maptrack.data.local.dao.AccelerationRunDao
import com.radityodwiki.maptrack.data.local.dao.LocationPointDao
import com.radityodwiki.maptrack.data.local.dao.PlaceDao
import com.radityodwiki.maptrack.data.local.dao.TripDao
import com.radityodwiki.maptrack.data.local.dao.VisitDao
import com.radityodwiki.maptrack.data.local.entity.AccelerationRunEntity
import com.radityodwiki.maptrack.data.local.entity.LocationPointEntity
import com.radityodwiki.maptrack.data.local.entity.PlaceEntity
import com.radityodwiki.maptrack.data.local.entity.TripEntity
import com.radityodwiki.maptrack.data.local.entity.VisitEntity

@Database(
    entities = [
        TripEntity::class,
        LocationPointEntity::class,
        VisitEntity::class,
        PlaceEntity::class,
        AccelerationRunEntity::class,
    ],
    version = 5,
    exportSchema = true,
    // 1 → 2 (Fase 2): table visits + trips.visit_detection_version. 2 → 3 (Fase 3): table places.
    // 3 → 4 (Fase 5): trips.source, default 'manual'. 4 → 5 (Fase 5, v2.6): table acceleration_runs.
    // All purely additive.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class MapTrackDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun visitDao(): VisitDao
    abstract fun placeDao(): PlaceDao
    abstract fun accelerationRunDao(): AccelerationRunDao

    companion object {
        private const val FILE_NAME = "map_track.db"

        fun create(context: Context): MapTrackDatabase =
            Room.databaseBuilder(context.applicationContext, MapTrackDatabase::class.java, FILE_NAME)
                .build()
    }
}
