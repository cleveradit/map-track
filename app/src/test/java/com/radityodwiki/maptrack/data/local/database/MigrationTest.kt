package com.radityodwiki.maptrack.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Builds a version-1 database from the exported 1.json, then opens it with the current
 * [MapTrackDatabase]. Room runs the production migrations and validates the resulting schema
 * against the entities, throwing on any mismatch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"
    private val schemaDir = File("schemas/com.radityodwiki.maptrack.data.local.database.MapTrackDatabase")

    private fun createDatabase(version: Int, seed: SQLiteDatabase.() -> Unit) {
        context.deleteDatabase(dbName)
        val schema = JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database")
        context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
            db.seed()
        }
    }

    private fun openCurrent(): MapTrackDatabase =
        Room.databaseBuilder(context, MapTrackDatabase::class.java, dbName).allowMainThreadQueries().build()

    @Test
    fun migrate1To2_keepsPhase1DataAndMarksTripsForBackfill() = runTest {
        createDatabase(1) {
            execSQL(
                "INSERT INTO trips (id, started_at, ended_at, distance_meters, average_speed, max_speed, status, updated_at) " +
                    "VALUES ('done', 1000, 9000, 120.5, 2.0, 4.0, 'completed', 9000)",
            )
            execSQL(
                "INSERT INTO trips (id, started_at, ended_at, distance_meters, average_speed, max_speed, status, updated_at) " +
                    "VALUES ('running', 10000, NULL, NULL, NULL, NULL, 'active', 10000)",
            )
            execSQL(
                "INSERT INTO location_points (trip_id, latitude, longitude, accuracy, speed, bearing, altitude, recorded_at) " +
                    "VALUES ('done', -7.78, 110.36, 5.0, 1.0, NULL, NULL, 2000), ('done', -7.781, 110.36, 6.0, NULL, NULL, NULL, 3000)",
            )
        }

        val database = openCurrent()
        try {
            val trip = database.tripDao().getById("done")!!
            assertEquals(120.5, trip.distanceMeters!!, 0.0)
            assertEquals(9000L, trip.updatedAt)
            assertEquals(0, trip.visitDetectionVersion)
            assertEquals("active", database.tripDao().getById("running")!!.status)
            assertEquals(listOf(2000L, 3000L), database.locationPointDao().getForTrip("done").map { it.recordedAt })
            assertEquals(0, database.visitDao().getForTrip("done").size)
            assertEquals(listOf("done"), database.tripDao().getCompletedIdsWithVisitVersionBelow(1))
            assertEquals(2, database.openHelper.readableDatabase.version)
        } finally {
            database.close()
            context.deleteDatabase(dbName)
        }
    }
}
