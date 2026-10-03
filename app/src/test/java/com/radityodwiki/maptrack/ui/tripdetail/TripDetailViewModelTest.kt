package com.radityodwiki.maptrack.ui.tripdetail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
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
}
