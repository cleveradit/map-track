package com.radityodwiki.maptrack.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.usecase.PlaceDetectionConfig
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

    private val minute = 60_000L

    /** One point per minute at the same place, from [fromMs] to [toMs] inclusive. */
    private suspend fun storeStay(tripId: String, fromMs: Long, toMs: Long) {
        for (t in fromMs..toMs step minute) repository.addPoint(point(tripId, t))
    }

    private suspend fun versionOf(tripId: String) = database.tripDao().getById(tripId)!!.visitDetectionVersion

    @Test
    fun finishTrip_storesVisitsWithStatistics() = runTest {
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * minute)
        clock = 20 * minute

        val finished = repository.finishTrip(trip.id, 11 * minute)

        assertEquals(TripStatus.COMPLETED, finished.status)
        assertEquals(20 * minute, finished.updatedAt)
        val visits = repository.observeVisits(trip.id).first()
        assertEquals(1, visits.size)
        assertEquals(11, visits[0].pointCount)
        assertEquals(PlaceDetectionConfig.DETECTION_VERSION, versionOf(trip.id))
        assertEquals(20 * minute, repository.getTrip(trip.id)!!.updatedAt)
    }

    @Test
    fun finishTrip_twiceDoesNotDuplicateVisits() = runTest {
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * minute)

        repository.finishTrip(trip.id, 11 * minute)
        repository.finishTrip(trip.id, 12 * minute)

        assertEquals(1, repository.observeVisits(trip.id).first().size)
    }

    @Test
    fun finishTrip_withFewerThanTwoPoints_hasNoVisits() = runTest {
        val empty = repository.startTrip().getOrThrow()
        repository.finishTrip(empty.id, 2_000)
        val single = repository.startTrip().getOrThrow()
        repository.addPoint(point(single.id, 1_500))
        repository.finishTrip(single.id, 2_000)

        for (id in listOf(empty.id, single.id)) {
            assertEquals(TripStatus.COMPLETED, repository.getTrip(id)!!.status)
            assertTrue(repository.observeVisits(id).first().isEmpty())
            assertEquals(PlaceDetectionConfig.DETECTION_VERSION, versionOf(id))
        }
    }

    @Test
    fun observeVisits_orderedByArrival() = runTest {
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * minute)
        // Second stop 1 km away, after a move longer than the merge gap.
        for (t in 20 * minute..30 * minute step minute) {
            repository.addPoint(point(trip.id, t).copy(latitude = -7.79))
        }

        repository.finishTrip(trip.id, 31 * minute)

        assertEquals(listOf(0L, 20 * minute), repository.observeVisits(trip.id).first().map { it.arrivedAt })
    }

    @Test
    fun deleteTrip_cascadesToVisits() = runTest {
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * minute)
        repository.finishTrip(trip.id, 11 * minute)

        repository.deleteTrip(trip.id).getOrThrow()

        assertTrue(database.visitDao().getForTrip(trip.id).isEmpty())
    }

    @Test
    fun recomputeVisits_onlyForCompletedTripsWithOldVersion() = runTest {
        // completeTrip leaves visits uncomputed, like a trip completed before Fase 2.
        val old = repository.startTrip().getOrThrow()
        storeStay(old.id, 0, 10 * minute)
        repository.completeTrip(old.id, 11 * minute, 0.0, 0.0, 10.0)
        val updatedAt = repository.getTrip(old.id)!!.updatedAt
        clock = 99 * minute
        val running = repository.startTrip().getOrThrow()
        storeStay(running.id, 100 * minute, 110 * minute)

        assertEquals(listOf(old.id), repository.tripIdsNeedingVisits())
        assertTrue(repository.recomputeVisits(old.id))

        assertEquals(1, repository.observeVisits(old.id).first().size)
        assertEquals(updatedAt, repository.getTrip(old.id)!!.updatedAt)
        assertEquals(11, repository.countPoints(old.id))
        assertFalse(repository.recomputeVisits(old.id)) // already up to date
        assertFalse(repository.recomputeVisits(running.id))
        assertFalse(repository.recomputeVisits("missing"))
        assertTrue(repository.observeVisits(running.id).first().isEmpty())
        assertEquals(emptyList<String>(), repository.tripIdsNeedingVisits())
    }

    @Test
    fun startTrip_recordsSource() = runTest {
        val manual = repository.startTrip().getOrThrow()
        repository.completeTrip(manual.id, 2_000, 0.0, 0.0, null)
        val auto = repository.startTrip(TripSource.AUTO).getOrThrow()

        assertEquals(TripSource.MANUAL, repository.getTrip(manual.id)!!.source)
        assertEquals(TripSource.AUTO, repository.getTrip(auto.id)!!.source)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownSource_isRejected() {
        TripSource.fromDbValue("x")
    }
}
