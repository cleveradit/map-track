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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaceDetailViewModelTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var trips: TripRepository
    private lateinit var places: PlaceRepository
    private var clock = 0L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        trips = TripRepository(database) { clock }
        places = PlaceRepository(database) { clock }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** A completed trip with one stay of [minutes] at [northMeters] north of (0, 0), starting at [startMs]. */
    private suspend fun tripWithStay(startMs: Long, minutes: Int, northMeters: Double = 0.0): String {
        clock = startMs
        val trip = trips.startTrip().getOrThrow()
        for (t in startMs..startMs + minutes * 60_000L step 60_000) {
            trips.addPoint(testPoint(latitude = northMeters * DEG_PER_METER, recordedAt = t).copy(tripId = trip.id))
        }
        trips.finishTrip(trip.id, startMs + minutes * 60_000L)
        return trip.id
    }

    private suspend fun PlaceDetailViewModel.loaded() =
        uiState.first { it is PlaceDetailUiState.Loaded } as PlaceDetailUiState.Loaded

    @Test
    fun totalsAndNewestFirst() = runTest {
        val older = tripWithStay(0, 10)
        val newer = tripWithStay(86_400_000, 20)
        val place = places.createPlace(PlaceInput("Rumah", 0.0, 0.0, 100.0)).getOrThrow()

        val state = PlaceDetailViewModel(place.id, places, trips).loaded()

        assertEquals(2, state.visitCount)
        assertEquals("30 menit", state.totalDuration)
        assertEquals(listOf(newer, older), state.visits.map { it.tripId })
        assertEquals("100 m", state.radius)
    }

    @Test
    fun withoutVisits() = runTest {
        val place = places.createPlace(PlaceInput("Kosong", 10.0, 10.0, 100.0)).getOrThrow()

        val state = PlaceDetailViewModel(place.id, places, trips).loaded()

        assertEquals(0, state.visitCount)
        assertEquals("0 menit", state.totalDuration)
        assertTrue(state.visits.isEmpty())
    }

    @Test
    fun overlappingVisitBelongsToNearestPlace() = runTest {
        tripWithStay(0, 10, northMeters = 300.0)
        val big = places.createPlace(PlaceInput("Kampus", 0.0, 0.0, 1_000.0)).getOrThrow()
        places.createPlace(PlaceInput("Kantin", 320 * DEG_PER_METER, 0.0, 100.0)).getOrThrow()

        assertEquals(0, PlaceDetailViewModel(big.id, places, trips).loaded().visitCount)
    }

    @Test
    fun followsRadiusChange() = runTest {
        tripWithStay(0, 10)
        val input = PlaceInput("Rumah", 80 * DEG_PER_METER, 0.0, 100.0)
        val place = places.createPlace(input).getOrThrow()
        val vm = PlaceDetailViewModel(place.id, places, trips)
        assertEquals(1, vm.loaded().visitCount)

        places.updatePlace(place.id, input.copy(radiusMeters = 50.0)).getOrThrow()

        assertEquals(0, (vm.uiState.first { (it as? PlaceDetailUiState.Loaded)?.visitCount == 0 } as PlaceDetailUiState.Loaded).visitCount)
    }

    @Test
    fun unknownPlace_isNotFound() = runTest {
        val state = PlaceDetailViewModel("missing", places, trips).uiState.first { it != PlaceDetailUiState.Loading }

        assertEquals(PlaceDetailUiState.NotFound, state)
    }

    @Test
    fun delete_keepsTripsAndVisits() = runTest {
        val tripId = tripWithStay(0, 10)
        val place = places.createPlace(PlaceInput("Rumah", 0.0, 0.0, 100.0)).getOrThrow()
        val vm = PlaceDetailViewModel(place.id, places, trips)

        vm.delete()

        assertTrue(vm.deleted.first { it })
        assertNull(places.getPlace(place.id))
        assertEquals(11, trips.countPoints(tripId))
        assertEquals(1, trips.observeVisits(tripId).first().size)
    }
}
