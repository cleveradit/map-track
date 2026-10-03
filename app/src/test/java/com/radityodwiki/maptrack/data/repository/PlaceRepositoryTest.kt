package com.radityodwiki.maptrack.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.domain.model.PlaceInput
import com.radityodwiki.maptrack.domain.usecase.PlaceNameError
import com.radityodwiki.maptrack.domain.usecase.testPoint
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
class PlaceRepositoryTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: PlaceRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PlaceRepository(database) { clock }
    }

    @After
    fun tearDown() = database.close()

    private fun input(name: String = "Rumah", radius: Double = 100.0) = PlaceInput(name, -7.78, 110.36, radius)

    @Test
    fun createPlace_trimsNameAndSetsTimestamps() = runTest {
        val place = repository.createPlace(input("  Rumah  ")).getOrThrow()

        assertEquals("Rumah", place.name)
        assertEquals(36, place.id.length)
        assertEquals(1_000L, place.createdAt)
        assertEquals(1_000L, place.updatedAt)
        assertEquals(place, repository.getPlace(place.id))
    }

    @Test
    fun createPlace_rejectsInvalidInput() = runTest {
        val blank = repository.createPlace(input(name = " ")).exceptionOrNull() as InvalidPlaceException
        val tooSmall = repository.createPlace(input(radius = 20.0)).exceptionOrNull() as InvalidPlaceException
        val badCoordinate = repository.createPlace(PlaceInput("X", 91.0, 0.0, 100.0)).exceptionOrNull() as InvalidPlaceException

        assertEquals(PlaceNameError.EMPTY, blank.nameError)
        assertFalse(blank.invalidRadius)
        assertTrue(tooSmall.invalidRadius)
        assertTrue(badCoordinate.invalidCoordinate)
        assertTrue(repository.observePlaces().first().isEmpty())
    }

    @Test
    fun updatePlace_refreshesUpdatedAtOnly() = runTest {
        val place = repository.createPlace(input()).getOrThrow()
        clock = 5_000L

        val updated = repository.updatePlace(place.id, input(name = "Kantor", radius = 250.0)).getOrThrow()

        assertEquals("Kantor", updated.name)
        assertEquals(250.0, updated.radiusMeters, 0.0)
        assertEquals(1_000L, updated.createdAt)
        assertEquals(5_000L, updated.updatedAt)
        assertEquals(updated, repository.observePlace(place.id).first())
    }

    @Test
    fun updatePlace_failsForUnknownOrInvalid() = runTest {
        val place = repository.createPlace(input()).getOrThrow()

        assertTrue(repository.updatePlace("missing", input()).exceptionOrNull() is NoSuchElementException)
        assertTrue(repository.updatePlace(place.id, input(radius = 1_001.0)).exceptionOrNull() is InvalidPlaceException)
        assertEquals(100.0, repository.getPlace(place.id)!!.radiusMeters, 0.0)
    }

    @Test
    fun deletePlace_keepsTripsAndVisits() = runTest {
        val trips = TripRepository(database) { clock }
        val trip = trips.startTrip().getOrThrow()
        for (t in 0L..10 * 60_000L step 60_000) trips.addPoint(testPoint(recordedAt = t).copy(tripId = trip.id))
        trips.finishTrip(trip.id, 11 * 60_000L)
        val place = repository.createPlace(PlaceInput("Rumah", 0.0, 0.0, 100.0)).getOrThrow()

        repository.deletePlace(place.id)

        assertNull(repository.getPlace(place.id))
        assertEquals(trip.id, trips.getTrip(trip.id)?.id)
        assertEquals(11, trips.countPoints(trip.id))
        assertEquals(1, trips.observeAllVisits().first().size)
    }

    @Test
    fun observePlaces_sortedByNameIgnoringCase() = runTest {
        listOf("beta", "Alpha", "gamma").forEach { repository.createPlace(input(name = it)) }

        assertEquals(listOf("Alpha", "beta", "gamma"), repository.observePlaces().first().map { it.name })
    }
}
