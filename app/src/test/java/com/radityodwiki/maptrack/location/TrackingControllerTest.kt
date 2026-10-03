package com.radityodwiki.maptrack.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.TrackingParams
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.usecase.PlaceDetectionConfig
import com.radityodwiki.maptrack.domain.usecase.DEG_PER_METER
import com.radityodwiki.maptrack.domain.usecase.TripRecorder
import com.radityodwiki.maptrack.domain.usecase.testPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrackingControllerTest {

    private class FakeLocationSource : LocationSource {
        var permission = LocationPermission.GRANTED
        var enabled = true
        override fun permissionState() = permission
        override fun isLocationEnabled() = enabled
        override fun fixes(request: LocationRequestSpec): Flow<GpsFix> = emptyFlow()
    }

    private class FakeLauncher : TrackingServiceLauncher {
        val started = mutableListOf<String>()
        val params = mutableListOf<TrackingParams>()
        var stopCalls = 0
        var failOnStart = false
        override fun start(tripId: String, params: TrackingParams) {
            if (failOnStart) throw IllegalStateException("not allowed")
            started += tripId
            this.params += params
        }
        override fun stop() {
            stopCalls++
        }
    }

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: TripRepository
    private val source = FakeLocationSource()
    private val launcher = FakeLauncher()
    private val stateHolder = TrackingStateHolder()
    private lateinit var controller: TrackingController

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TripRepository(database) { 1_000L }
        controller = TrackingController(source, repository, TripRecorder(repository) { 2_000L }, stateHolder, launcher)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun startCreatesTripAndLaunchesService() = runTest {
        assertNull(controller.start())

        val active = repository.getActiveTrip()!!
        assertEquals(listOf(active.id), launcher.started)
        assertEquals(TrackingState.Active(active.id, active.startedAt, null), stateHolder.state.value)
    }

    @Test
    fun startWithoutPreciseLocationFails() = runTest {
        source.permission = LocationPermission.APPROXIMATE_ONLY

        assertEquals(StartTrackingError.PERMISSION_MISSING, controller.start())
        assertNull(repository.getActiveTrip())
        assertTrue(launcher.started.isEmpty())
    }

    @Test
    fun startWithLocationDisabledFails() = runTest {
        source.enabled = false

        assertEquals(StartTrackingError.LOCATION_DISABLED, controller.start())
        assertNull(repository.getActiveTrip())
    }

    @Test
    fun secondStartFailsWhileTripActive() = runTest {
        controller.start()

        assertEquals(StartTrackingError.TRIP_ALREADY_ACTIVE, controller.start())
        assertEquals(1, launcher.started.size)
    }

    @Test
    fun serviceStartFailureFinishesTrip() = runTest {
        launcher.failOnStart = true

        assertEquals(StartTrackingError.SERVICE_START_FAILED, controller.start())
        assertNull(repository.getActiveTrip())
        assertEquals(TrackingState.Idle, stateHolder.state.value)
    }

    @Test
    fun stopWithRunningServiceDelegatesToService() = runTest {
        controller.start()
        val tripId = launcher.started.single()

        controller.stop()

        assertEquals(1, launcher.stopCalls)
        assertEquals(TripStatus.ACTIVE, repository.getTrip(tripId)!!.status)
    }

    @Test
    fun stopWithoutServiceEndsTripAtLastPoint() = runTest {
        controller.start()
        val tripId = launcher.started.single()
        repository.addPoint(testPoint(recordedAt = 1_500L).copy(tripId = tripId))
        stateHolder.state.value = TrackingState.Idle // process died and came back

        controller.stop()

        assertEquals(0, launcher.stopCalls)
        val trip = repository.getTrip(tripId)!!
        assertEquals(TripStatus.COMPLETED, trip.status)
        assertEquals(1_500L, trip.endedAt)
    }

    private suspend fun storeStay(tripId: String) {
        for (t in 0L..10 * 60_000L step 60_000) repository.addPoint(testPoint(recordedAt = t).copy(tripId = tripId))
    }

    @Test
    fun stopWithoutServiceStoresVisits() = runTest {
        controller.start()
        val tripId = launcher.started.single()
        storeStay(tripId)
        stateHolder.state.value = TrackingState.Idle

        controller.stop()

        assertEquals(1, repository.observeVisits(tripId).first().size)
    }

    @Test
    fun endInterruptedTripStoresVisits() = runTest {
        val tripId = repository.startTrip().getOrThrow().id
        storeStay(tripId)

        controller.endInterruptedTrip(tripId)

        assertEquals(1, repository.observeVisits(tripId).first().size)
        assertEquals(PlaceDetectionConfig.DETECTION_VERSION, database.tripDao().getById(tripId)!!.visitDetectionVersion)
    }

    @Test
    fun startPassesSettingsSnapshotToService() = runTest {
        var settings = TrackingParams(10_000, 30f)
        val controller = TrackingController(source, repository, TripRecorder(repository) { 2_000L }, stateHolder, launcher, trackingParams = { settings })

        controller.start()
        // A change while the trip runs only affects the next trip.
        settings = TrackingParams(30_000, 100f)
        controller.stop()
        stateHolder.state.value = TrackingState.Idle
        repository.getActiveTrip()?.let { TripRecorder(repository).finish(it.id) }
        controller.start()

        assertEquals(listOf(TrackingParams(10_000, 30f), TrackingParams(30_000, 100f)), launcher.params)
    }

    private val minute = 60_000L

    /** An active trip with one point at [lastPointAt], left without a service. */
    private suspend fun interruptedTrip(lastPointAt: Long): String {
        val tripId = repository.startTrip().getOrThrow().id
        repository.addPoint(testPoint(recordedAt = lastPointAt).copy(tripId = tripId))
        return tripId
    }

    private fun controllerAt(now: Long, params: TrackingParams = TrackingParams.DEFAULT) =
        TrackingController(source, repository, TripRecorder(repository) { now }, stateHolder, launcher, { params }, { now })

    @Test
    fun resumeRestartsServiceForSameTrip() = runTest {
        val tripId = interruptedTrip(lastPointAt = 0)
        var stateWhenLaunched: TrackingState? = null
        val recording = object : TrackingServiceLauncher {
            override fun start(tripId: String, params: TrackingParams) {
                stateWhenLaunched = stateHolder.state.value
                launcher.start(tripId, params)
            }
            override fun stop() = launcher.stop()
        }
        val controller = TrackingController(
            source, repository, TripRecorder(repository), stateHolder, recording, { TrackingParams(10_000, 30f) }, { 30 * minute },
        )

        assertNull(controller.resume(tripId))

        assertEquals(listOf(tripId), launcher.started)
        assertEquals(listOf(TrackingParams(10_000, 30f)), launcher.params)
        assertEquals(tripId, (stateWhenLaunched as TrackingState.Active).tripId)
    }

    @Test
    fun resumeAfterNinetyMinutesIsRefused() = runTest {
        val tripId = interruptedTrip(lastPointAt = 0)

        assertEquals(StartTrackingError.RESUME_EXPIRED, controllerAt(90 * minute).resume(tripId))
        assertTrue(launcher.started.isEmpty())
    }

    @Test
    fun resumeChecksPermissionAndLocation() = runTest {
        val tripId = interruptedTrip(lastPointAt = 0)
        source.permission = LocationPermission.APPROXIMATE_ONLY
        assertEquals(StartTrackingError.PERMISSION_MISSING, controllerAt(minute).resume(tripId))

        source.permission = LocationPermission.GRANTED
        source.enabled = false
        assertEquals(StartTrackingError.LOCATION_DISABLED, controllerAt(minute).resume(tripId))
    }

    @Test
    fun resumeServiceFailureKeepsTripInterrupted() = runTest {
        val tripId = interruptedTrip(lastPointAt = 0)
        launcher.failOnStart = true

        assertEquals(StartTrackingError.SERVICE_START_FAILED, controllerAt(minute).resume(tripId))
        assertEquals(TrackingState.Idle, stateHolder.state.value)
        assertEquals(TripStatus.ACTIVE, repository.getTrip(tripId)!!.status)
    }

    @Test
    fun distanceAfterResumeIncludesStraightGap() = runTest {
        val tripId = interruptedTrip(lastPointAt = 0)
        controllerAt(30 * minute).resume(tripId)
        repository.addPoint(testPoint(latitude = 1_000 * DEG_PER_METER, recordedAt = 30 * minute).copy(tripId = tripId))

        val finished = TripRecorder(repository) { 31 * minute }.finish(tripId)

        assertEquals(1_000.0, finished.distanceMeters!!, 0.01)
    }
}
