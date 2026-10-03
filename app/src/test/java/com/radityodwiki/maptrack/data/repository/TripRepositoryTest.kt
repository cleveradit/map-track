package com.radityodwiki.maptrack.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TripRepositoryTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: TripRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TripRepository(database) { clock }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun point(tripId: String, recordedAt: Long) = LocationPoint(
        tripId = tripId,
        latitude = -7.78,
        longitude = 110.36,
        accuracyMeters = 5f,
        speedMps = 10f,
        bearingDegrees = null,
        altitudeMeters = null,
        recordedAt = recordedAt,
    )

    @Test
    fun startTrip_createsActiveTripWithUuid() = runTest {
        val trip = repository.startTrip().getOrThrow()

        assertEquals(TripStatus.ACTIVE, trip.status)
        assertNull(trip.endedAt)
        assertEquals(1_000L, trip.startedAt)
        assertEquals(36, trip.id.length)
        assertEquals(trip, repository.getActiveTrip())
    }

    @Test
    fun startTrip_failsWhenTripAlreadyActive() = runTest {
        val first = repository.startTrip().getOrThrow()

        val second = repository.startTrip()

        assertTrue(second.exceptionOrNull() is ActiveTripExistsException)
        assertEquals(first.id, (second.exceptionOrNull() as ActiveTripExistsException).activeTripId)
    }

    @Test
    fun addPoint_returnsPointsOrderedByRecordedAt() = runTest {
        val trip = repository.startTrip().getOrThrow()

        repository.addPoint(point(trip.id, 3_000))
        repository.addPoint(point(trip.id, 1_000))
        repository.addPoint(point(trip.id, 2_000))

        assertEquals(listOf(1_000L, 2_000L, 3_000L), repository.getPoints(trip.id).map { it.recordedAt })
        assertEquals(3_000L, repository.getLastPoint(trip.id)?.recordedAt)
    }

    @Test
    fun addPoint_ignoresDuplicateRecordedAt() = runTest {
        val trip = repository.startTrip().getOrThrow()

        assertTrue(repository.addPoint(point(trip.id, 1_000)))
        assertFalse(repository.addPoint(point(trip.id, 1_000)))

        assertEquals(1, repository.countPoints(trip.id))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun addPoint_rejectsUnknownTrip() = runTest {
        repository.addPoint(point("missing-trip", 1_000))
    }

    @Test
    fun completeTrip_storesStatisticsAndStatus() = runTest {
        val trip = repository.startTrip().getOrThrow()
        clock = 5_000L

        val completed = repository.completeTrip(trip.id, 4_000L, 1234.5, 3.2, 12.0)

        assertEquals(TripStatus.COMPLETED, completed.status)
        assertEquals(4_000L, completed.endedAt)
        assertEquals(1234.5, completed.distanceMeters!!, 0.0)
        assertEquals(3.2, completed.averageSpeedMps!!, 0.0)
        assertEquals(12.0, completed.maxSpeedMps!!, 0.0)
        assertEquals(5_000L, completed.updatedAt)
        assertNull(repository.getActiveTrip())
    }

    @Test
    fun deleteTrip_rejectsActiveTrip() = runTest {
        val trip = repository.startTrip().getOrThrow()
        repository.addPoint(point(trip.id, 1_000))

        val result = repository.deleteTrip(trip.id)

        assertTrue(result.exceptionOrNull() is ActiveTripDeletionException)
        assertEquals(trip.id, repository.getTrip(trip.id)?.id)
        assertEquals(1, repository.countPoints(trip.id))
    }

    @Test
    fun deleteTrip_cascadesToPoints() = runTest {
        val trip = repository.startTrip().getOrThrow()
        repository.addPoint(point(trip.id, 1_000))
        repository.addPoint(point(trip.id, 2_000))
        repository.completeTrip(trip.id, 2_000, 10.0, 1.0, 10.0)

        repository.deleteTrip(trip.id).getOrThrow()

        assertNull(repository.getTrip(trip.id))
        assertEquals(0, repository.countPoints(trip.id))
    }

    @Test
    fun observeTrips_newestFirst() = runTest {
        val older = repository.startTrip().getOrThrow()
        repository.completeTrip(older.id, 2_000, 0.0, 0.0, null)
        clock = 10_000L
        val newer = repository.startTrip().getOrThrow()

        val trips = repository.observeTrips().first()

        assertEquals(listOf(newer.id, older.id), trips.map { it.id })
    }
}
