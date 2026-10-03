package com.radityodwiki.maptrack.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.model.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TripRecorderTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: TripRepository
    private lateinit var recorder: TripRecorder
    private var clock = 0L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TripRepository(database) { clock }
        recorder = TripRecorder(repository) { clock }
    }

    @After
    fun tearDown() = database.close()

    private fun fix(meters: Double, time: Long, accuracy: Float = 5f, speed: Float? = 2f) =
        GpsFix(meters * DEG_PER_METER, 0.0, accuracy, speed, null, null, time)

    @Test
    fun validFixIsStored() = runTest {
        val trip = repository.startTrip().getOrThrow()

        assertNull(recorder.record(trip.id, fix(0.0, 1_000)))
        assertEquals(1, repository.countPoints(trip.id))
    }

    @Test
    fun poorAccuracyIsNotStored() = runTest {
        val trip = repository.startTrip().getOrThrow()

        assertEquals(RejectReason.POOR_ACCURACY, recorder.record(trip.id, fix(0.0, 1_000, accuracy = 80f)))
        assertEquals(0, repository.countPoints(trip.id))
    }

    @Test
    fun rejectedJumpDoesNotBecomeReference() = runTest {
        val trip = repository.startTrip().getOrThrow()
        recorder.record(trip.id, fix(0.0, 0))

        assertEquals(RejectReason.GPS_JUMP, recorder.record(trip.id, fix(2_000.0, 5_000)))
        // Compared with the first stored point: 50 m in 10 s is plausible.
        assertNull(recorder.record(trip.id, fix(50.0, 10_000)))
        assertEquals(listOf(0L, 10_000L), repository.getPoints(trip.id).map { it.recordedAt })
    }

    @Test
    fun recorderUsesStoredPointAfterRestart() = runTest {
        val trip = repository.startTrip().getOrThrow()
        recorder.record(trip.id, fix(0.0, 5_000))

        val freshRecorder = TripRecorder(repository) { clock }

        assertEquals(RejectReason.OUT_OF_ORDER, freshRecorder.record(trip.id, fix(10.0, 5_000)))
    }

    @Test
    fun finishComputesStatistics() = runTest {
        val trip = repository.startTrip().getOrThrow()
        recorder.record(trip.id, fix(0.0, 10_000, speed = 1f))
        recorder.record(trip.id, fix(100.0, 50_000, speed = 4f))
        recorder.record(trip.id, fix(200.0, 90_000, speed = 2f))
        clock = 100_000

        val finished = recorder.finish(trip.id)

        assertEquals(TripStatus.COMPLETED, finished.status)
        assertEquals(100_000L, finished.endedAt)
        assertEquals(200.0, finished.distanceMeters!!, 0.01)
        assertEquals(2.0, finished.averageSpeedMps!!, 0.001)
        assertEquals(4.0, finished.maxSpeedMps!!, 0.001)
    }

    @Test
    fun finishWithoutPoints() = runTest {
        val trip = repository.startTrip().getOrThrow()
        clock = 60_000

        val finished = recorder.finish(trip.id)

        assertEquals(0.0, finished.distanceMeters!!, 0.0)
        assertEquals(0.0, finished.averageSpeedMps!!, 0.0)
        assertNull(finished.maxSpeedMps)
    }

    @Test
    fun finishIsIdempotent() = runTest {
        val trip = repository.startTrip().getOrThrow()
        clock = 60_000
        val first = recorder.finish(trip.id)
        clock = 120_000

        val second = recorder.finish(trip.id)

        assertEquals(first, second)
    }

    @Test
    fun finishInterruptedEndsAtLastPoint() = runTest {
        val trip = repository.startTrip().getOrThrow()
        recorder.record(trip.id, fix(0.0, 10_000))
        recorder.record(trip.id, fix(100.0, 60_000))
        clock = 999_000

        val finished = recorder.finishInterrupted(trip.id)

        assertEquals(TripStatus.COMPLETED, finished.status)
        assertEquals(60_000L, finished.endedAt)
        assertEquals(100.0, finished.distanceMeters!!, 0.01)
    }

    @Test
    fun finishInterruptedWithoutPointsEndsAtStart() = runTest {
        clock = 5_000
        val trip = repository.startTrip().getOrThrow()
        clock = 999_000

        val finished = recorder.finishInterrupted(trip.id)

        assertEquals(5_000L, finished.endedAt)
        assertEquals(0.0, finished.distanceMeters!!, 0.0)
    }

    /** Stores a point every minute at the same place from [fromMs] to [toMs], bypassing the filter. */
    private suspend fun storeStay(tripId: String, fromMs: Long, toMs: Long) {
        for (t in fromMs..toMs step 60_000) repository.addPoint(testPoint(recordedAt = t).copy(tripId = tripId))
    }

    @Test
    fun finishStoresVisits() = runTest {
        // Stop in the app and Stop in the notification both end in the service calling finish().
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * 60_000L)
        clock = 11 * 60_000L

        recorder.finish(trip.id)

        val visits = repository.observeVisits(trip.id).first()
        assertEquals(1, visits.size)
        assertEquals(0L, visits[0].arrivedAt)
        assertEquals(10 * 60_000L, visits[0].departedAt)
        assertEquals(11, visits[0].pointCount)
    }

    @Test
    fun finishInterruptedStoresVisits() = runTest {
        val trip = repository.startTrip().getOrThrow()
        storeStay(trip.id, 0, 10 * 60_000L)

        recorder.finishInterrupted(trip.id)

        assertEquals(1, repository.observeVisits(trip.id).first().size)
        assertEquals(PlaceDetectionConfig.DETECTION_VERSION, database.tripDao().getById(trip.id)!!.visitDetectionVersion)
    }

    @Test
    fun tripAccuracyThresholdIsApplied() = runTest {
        val trip = repository.startTrip().getOrThrow()

        assertEquals(RejectReason.POOR_ACCURACY, recorder.record(trip.id, fix(0.0, 1_000, accuracy = 30f), maxAccuracyMeters = 20f))
        assertNull(recorder.record(trip.id, fix(0.0, 2_000, accuracy = 30f), maxAccuracyMeters = 100f))
    }

    /** A finished trip of [meters] going north over [durationMs], started at 0. */
    private suspend fun finishedTrip(source: TripSource, meters: Double, durationMs: Long): String {
        clock = 0
        val trip = repository.startTrip(source).getOrThrow()
        repository.addPoint(testPoint(recordedAt = 0, speed = 5f).copy(tripId = trip.id))
        repository.addPoint(testPoint(latitude = meters * DEG_PER_METER, recordedAt = durationMs, speed = 5f).copy(tripId = trip.id))
        clock = durationMs
        recorder.finish(trip.id)
        return trip.id
    }

    @Test
    fun shortAutoTripsAreDeleted() = runTest {
        val tooShortDistance = finishedTrip(TripSource.AUTO, meters = 250.0, durationMs = 10 * 60_000L)
        val tooShortTime = finishedTrip(TripSource.AUTO, meters = 1_000.0, durationMs = 90_000L)
        val kept = finishedTrip(TripSource.AUTO, meters = 300.5, durationMs = 2 * 60_000L)

        assertNull(repository.getTrip(tooShortDistance))
        assertNull(repository.getTrip(tooShortTime))
        assertEquals(TripStatus.COMPLETED, repository.getTrip(kept)!!.status)
    }

    @Test
    fun shortManualTripIsKept() = runTest {
        val manual = finishedTrip(TripSource.MANUAL, meters = 50.0, durationMs = 30_000L)

        assertEquals(TripStatus.COMPLETED, repository.getTrip(manual)!!.status)
    }

    @Test
    fun interruptedShortAutoTripIsDeletedToo() = runTest {
        val trip = repository.startTrip(TripSource.AUTO).getOrThrow()
        repository.addPoint(testPoint(recordedAt = 10_000).copy(tripId = trip.id))

        recorder.finishInterrupted(trip.id)

        assertNull(repository.getTrip(trip.id))
    }
}
