package com.radityodwiki.maptrack.ui.tripdetail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.VisitEntity
import com.radityodwiki.maptrack.data.repository.TripRepository
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
        val viewModel = TripDetailViewModel("missing", TripRepository(database))

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
        val viewModel = TripDetailViewModel(trip.id, repository)

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

        val state = TripDetailViewModel(trip.id, repository).loaded()

        assertEquals(1, state.visits.size)
        assertEquals("10 menit", state.visits[0].duration)
    }

    @Test
    fun completedTripWithoutStopsHasNoVisits() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        repository.addPoint(testPoint(recordedAt = 1_000).copy(tripId = trip.id))
        repository.finishTrip(trip.id, 2_000)

        assertEquals(emptyList<VisitItem>(), TripDetailViewModel(trip.id, repository).loaded().visits)
    }

    @Test
    fun activeTripHidesVisits() = runTest {
        val repository = TripRepository(database)
        val trip = repository.startTrip().getOrThrow()
        database.visitDao().insertAll(listOf(VisitEntity(0, trip.id, 0, 600_000, 0.0, 0.0, 5)))

        assertEquals(emptyList<VisitItem>(), TripDetailViewModel(trip.id, repository).loaded().visits)
    }
}
