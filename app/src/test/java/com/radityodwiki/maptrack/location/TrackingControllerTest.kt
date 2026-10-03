package com.radityodwiki.maptrack.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.usecase.PlaceDetectionConfig
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
        override fun fixes(): Flow<GpsFix> = emptyFlow()
    }

    private class FakeLauncher : TrackingServiceLauncher {
        val started = mutableListOf<String>()
        var stopCalls = 0
        var failOnStart = false
        override fun start(tripId: String) {
            if (failOnStart) throw IllegalStateException("not allowed")
            started += tripId
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
}
