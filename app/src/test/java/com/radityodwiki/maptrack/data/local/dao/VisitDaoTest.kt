package com.radityodwiki.maptrack.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.TripEntity
import com.radityodwiki.maptrack.data.local.entity.VisitEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisitDaoTest {

    private lateinit var database: MapTrackDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun trip(id: String, status: String = "completed", version: Int = 0) = TripEntity(
        id = id,
        startedAt = 1_000,
        endedAt = 2_000,
        distanceMeters = 0.0,
        averageSpeed = 0.0,
        maxSpeed = null,
        status = status,
        updatedAt = 2_000,
        visitDetectionVersion = version,
    )

    private fun visit(tripId: String, arrivedAt: Long) = VisitEntity(
        tripId = tripId,
        arrivedAt = arrivedAt,
        departedAt = arrivedAt + 600_000,
        centerLatitude = -7.78,
        centerLongitude = 110.36,
        pointCount = 3,
    )

    @Test
    fun deletingTrip_cascadesToVisits() = runTest {
        database.tripDao().insert(trip("t"))
        database.visitDao().insertAll(listOf(visit("t", 1_000), visit("t", 5_000)))

        database.tripDao().deleteById("t")

        assertEquals(0, database.visitDao().getForTrip("t").size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun visitForUnknownTrip_isRejected() = runTest {
        database.visitDao().insertAll(listOf(visit("missing", 1_000)))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateArrival_isRejected() = runTest {
        database.tripDao().insert(trip("t"))
        database.visitDao().insertAll(listOf(visit("t", 1_000), visit("t", 1_000)))
    }

    @Test
    fun backfillQuery_skipsActiveAndUpToDateTrips() = runTest {
        database.tripDao().insert(trip("old"))
        database.tripDao().insert(trip("current", version = 1))
        database.tripDao().insert(trip("running", status = "active"))

        assertEquals(listOf("old"), database.tripDao().getCompletedIdsWithVisitVersionBelow(1))

        database.tripDao().setVisitDetectionVersion("old", 1)

        assertEquals(emptyList<String>(), database.tripDao().getCompletedIdsWithVisitVersionBelow(1))
        assertEquals(2_000L, database.tripDao().getById("old")!!.updatedAt)
    }
}
