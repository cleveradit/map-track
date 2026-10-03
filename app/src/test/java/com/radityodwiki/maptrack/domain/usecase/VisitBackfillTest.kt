package com.radityodwiki.maptrack.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisitBackfillTest {

    private class FakeStore(private val ids: List<String>, private val failing: Map<String, Exception>) : VisitRecomputation {
        val attempted = mutableListOf<String>()
        override suspend fun tripIdsNeedingVisits() = ids
        override suspend fun recomputeVisits(tripId: String): Boolean {
            attempted += tripId
            failing[tripId]?.let { throw it }
            return true
        }
    }

    @Test
    fun failingTripDoesNotStopOthers() = runTest {
        val store = FakeStore(listOf("a", "b", "c"), mapOf("b" to IllegalStateException("broken")))
        val failures = mutableListOf<String>()

        val recomputed = VisitBackfill(store) { tripId, _ -> failures += tripId }.run()

        assertEquals(2, recomputed)
        assertEquals(listOf("a", "b", "c"), store.attempted)
        assertEquals(listOf("b"), failures)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() = runTest {
        val store = FakeStore(listOf("a", "b"), mapOf("a" to CancellationException("cancelled")))

        try {
            VisitBackfill(store) { _, _ -> throw AssertionError("cancellation is not a failure") }.run()
        } finally {
            assertEquals(listOf("a"), store.attempted)
        }
    }

    @Test
    fun backfillsOldTripWithoutDeletingData() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val repository = TripRepository(database) { 1_000L }
            val trip = repository.startTrip().getOrThrow()
            for (t in 0L..10 * 60_000L step 60_000) repository.addPoint(testPoint(recordedAt = t).copy(tripId = trip.id))
            // Completed without visits, like a trip from Fase 1.
            val completed = repository.completeTrip(trip.id, 11 * 60_000L, 0.0, 0.0, null)

            assertEquals(1, VisitBackfill(repository).run())

            assertEquals(1, repository.observeVisits(trip.id).first().size)
            assertEquals(11, repository.countPoints(trip.id))
            assertEquals(completed, repository.getTrip(trip.id))
            assertEquals(0, VisitBackfill(repository).run())
        } finally {
            database.close()
        }
    }
}
