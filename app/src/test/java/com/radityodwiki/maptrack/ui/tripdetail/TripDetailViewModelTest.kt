package com.radityodwiki.maptrack.ui.tripdetail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.VisitEntity
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.domain.model.PlaceInput
import kotlinx.coroutines.flow.flowOf
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
class TripDetailViewModelTest {

    private lateinit var database: MapTrackDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun unknownTripIsNotFound() = runTest {
        val viewModel = TripDetailViewModel("missing", TripRepository(database), PlaceRepository(database))

        val state = viewModel.uiState.first { it != TripDetailUiState.Loading }

        assertEquals(TripDetailUiState.NotFound, state)
    }

    @Test
    fun routeContainsStoredPointsInOrder() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        listOf(3_000L, 1_000L, 2_000L).forEachIndexed { i, time ->
            repository.addPoint(testPoint(latitude = i.toDouble(), recordedAt = time).copy(tripId = trip.id))
        }
        val viewModel = TripDetailViewModel(trip.id, repository, PlaceRepository(database))

        val state = viewModel.uiState.first { it is TripDetailUiState.Loaded } as TripDetailUiState.Loaded

        assertEquals(listOf(1.0, 2.0, 0.0), state.route.map { it.latitude })
        assertEquals(3, state.pointCount)
    }

    private suspend fun TripRepository.storeStay(tripId: String) {
        for (t in 0L..10 * 60_000L step 60_000) addPoint(testPoint(recordedAt = t).copy(tripId = tripId))
    }

    private suspend fun TripDetailViewModel.loaded() =
        uiState.first { it is TripDetailUiState.Loaded } as TripDetailUiState.Loaded

    @Test
    fun completedTripShowsVisits() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        repository.storeStay(trip.id)
        repository.finishTrip(trip.id, 11 * 60_000L)

        val state = TripDetailViewModel(trip.id, repository, PlaceRepository(database)).loaded()

        assertEquals(1, state.visits.size)
        assertEquals("10 menit", state.visits[0].duration)
    }

    @Test
    fun completedTripWithoutStopsHasNoVisits() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        repository.addPoint(testPoint(recordedAt = 1_000).copy(tripId = trip.id))
        repository.finishTrip(trip.id, 2_000)

        assertEquals(emptyList<VisitItem>(), TripDetailViewModel(trip.id, repository, PlaceRepository(database)).loaded().visits)
    }

    @Test
    fun activeTripHidesVisits() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        database.visitDao().insertAll(listOf(VisitEntity(0, trip.id, 0, 600_000, 0.0, 0.0, 5)))

        assertEquals(emptyList<VisitItem>(), TripDetailViewModel(trip.id, repository, PlaceRepository(database)).loaded().visits)
    }

    /** A completed trip with a 10-minute visit at (0, 0). */
    private suspend fun completedTripWithVisit(repository: TripRepository): String {
        val trip = repository.startTrip().getOrThrow()
        repository.storeStay(trip.id)
        repository.finishTrip(trip.id, 11 * 60_000L)
        return trip.id
    }

    @Test
    fun visitNamedByMatchingPlace() = runTest {
        val repository = TripRepository(database)
        val places = PlaceRepository(database)
        val tripId = completedTripWithVisit(repository)
        places.createPlace(PlaceInput("Kampus", 0.0, 0.0, 1_000.0)).getOrThrow()
        places.createPlace(PlaceInput("Rumah", 50 * DEG_PER_METER, 0.0, 100.0)).getOrThrow()
        places.createPlace(PlaceInput("Jauh", 150 * DEG_PER_METER, 0.0, 100.0)).getOrThrow()

        // Kampus is the nearest center (0 m); Rumah also covers the visit; Jauh does not.
        assertEquals("Kampus", TripDetailViewModel(tripId, repository, places).loaded().visits.single().placeName)
    }

    @Test
    fun visitOutsideRadiusIsUnnamed() = runTest {
        val repository = TripRepository(database)
        val places = PlaceRepository(database)
        val tripId = completedTripWithVisit(repository)
        places.createPlace(PlaceInput("Jauh", 150 * DEG_PER_METER, 0.0, 100.0)).getOrThrow()

        assertEquals(null, TripDetailViewModel(tripId, repository, places).loaded().visits.single().placeName)
    }

    @Test
    fun deletingPlaceRemovesNameButKeepsVisit() = runTest {
        val repository = TripRepository(database)
        val places = PlaceRepository(database)
        val tripId = completedTripWithVisit(repository)
        val place = places.createPlace(PlaceInput("Rumah", 0.0, 0.0, 100.0)).getOrThrow()
        val viewModel = TripDetailViewModel(tripId, repository, places)
        assertEquals("Rumah", viewModel.loaded().visits.single().placeName)

        places.deletePlace(place.id)

        val state = viewModel.uiState.first {
            (it as? TripDetailUiState.Loaded)?.visits?.singleOrNull()?.placeName == null
        } as TripDetailUiState.Loaded
        assertEquals(1, state.visits.size)
    }

    @Test
    fun imperialSettingsChangeDisplayOnly() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        repository.completeTrip(trip.id, trip.startedAt + 600_000, 1_000.0, 1.0, 10.0)
        val imperial = flowOf(AppSettings.DEFAULT.copy(distanceUnit = DistanceUnit.IMPERIAL))

        val state = TripDetailViewModel(trip.id, repository, PlaceRepository(database), imperial).loaded()

        assertEquals("0.6 mi", state.summary.distance)
        assertEquals("mph", state.speedUnit)
        assertEquals(1_000.0, repository.getTrip(trip.id)!!.distanceMeters!!, 0.0)
    }
}
