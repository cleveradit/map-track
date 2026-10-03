package com.radityodwiki.maptrack.ui.places

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.PlaceInput
import com.radityodwiki.maptrack.domain.usecase.DEG_PER_METER
import com.radityodwiki.maptrack.domain.usecase.testPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlacesViewModelTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var trips: TripRepository
    private lateinit var places: PlaceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        trips = TripRepository(database)
        places = PlaceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** A completed trip with one 10-minute visit at (0, 0). */
    private suspend fun tripWithVisit() {
        val trip = trips.startTrip().getOrThrow()
        for (t in 0L..10 * 60_000L step 60_000) trips.addPoint(testPoint(recordedAt = t).copy(tripId = trip.id))
        trips.finishTrip(trip.id, 11 * 60_000L)
    }

    @Test
    fun emptyWithoutPlaces() = runTest {
        assertEquals(emptyList<PlaceListItem>(), PlacesViewModel(places, trips).items.first { it != null })
    }

    @Test
    fun countsFollowRadiusChanges() = runTest {
        tripWithVisit()
        // Center 80 m north of the visit.
        val input = PlaceInput("Rumah", 80 * DEG_PER_METER, 0.0, 100.0)
        val place = places.createPlace(input).getOrThrow()
        val viewModel = PlacesViewModel(places, trips)

        assertEquals(1, viewModel.items.first { it != null }!!.single().visitCount)

        places.updatePlace(place.id, input.copy(radiusMeters = 50.0)).getOrThrow()

        assertEquals(0, viewModel.items.first { it?.single()?.visitCount == 0 }!!.single().visitCount)
    }
}
