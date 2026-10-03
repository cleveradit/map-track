package com.radityodwiki.maptrack.location

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.TripRepository
import com.radityodwiki.maptrack.data.settings.SettingsRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.TrackingParams
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.usecase.TripRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutoTripControllerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class FakeSource : LocationSource {
        var permission = LocationPermission.GRANTED
        var enabled = true
        override fun permissionState() = permission
        override fun isLocationEnabled() = enabled
        override fun fixes(request: LocationRequestSpec): Flow<GpsFix> = emptyFlow()
    }

    private class FakePermissions : AutoTripPermissions {
        var activity = true
        var background = true
        override fun hasActivityRecognition() = activity
        override fun hasBackgroundLocation() = background
    }

    private class FakeTransitions : ActivityTransitions {
        var subscribed: Boolean? = null
        var includeWalking: Boolean? = null
        var fail = false
        override suspend fun subscribe(includeWalking: Boolean): Result<Unit> {
            if (fail) return Result.failure(IllegalStateException("no play services"))
            subscribed = true
            this.includeWalking = includeWalking
            return Result.success(Unit)
        }
        override suspend fun unsubscribe() {
            subscribed = false
        }
    }

    private class FakeLauncher : TrackingServiceLauncher {
        val started = mutableListOf<String>()
        override fun start(tripId: String, params: TrackingParams) {
            started += tripId
        }
        override fun stop() = Unit
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: MapTrackDatabase
    private lateinit var trips: TripRepository
    private lateinit var settings: SettingsRepository
    private val source = FakeSource()
    private val permissions = FakePermissions()
    private val transitions = FakeTransitions()
    private val launcher = FakeLauncher()
    private val stateHolder = TrackingStateHolder()
    private val stillness = StillnessHolder()
    private var clock = 1_000L
    private lateinit var controller: AutoTripController

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        trips = TripRepository(database) { clock }
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "s.preferences_pb") })
        val tracking = TrackingController(source, trips, TripRecorder(trips), stateHolder, launcher)
        controller = AutoTripController(settings, permissions, transitions, source, tracking, stillness) { clock }
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    private suspend fun enabled(includeWalking: Boolean = false) {
        settings.setAutoTripIncludeWalking(includeWalking)
        assertTrue(controller.enable())
    }

    @Test
    fun vehicleTransitionStartsAutoTrip() = runTest {
        enabled()

        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))

        val trip = trips.getActiveTrip()!!
        assertEquals(TripSource.AUTO, trip.source)
        assertEquals(listOf(trip.id), launcher.started)
    }

    @Test
    fun ignoredWhenDisabled() = runTest {
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))

        assertNull(trips.getActiveTrip())
    }

    @Test
    fun ignoredWhileATripIsActive() = runTest {
        enabled()
        val manual = trips.startTrip().getOrThrow()

        controller.onTransitions(listOf(TransitionEvent(MotionActivity.ON_BICYCLE, enter = true)))

        assertEquals(manual.id, trips.getActiveTrip()!!.id)
        assertTrue(launcher.started.isEmpty())
    }

    @Test
    fun walkingOnlyWithOption() = runTest {
        enabled(includeWalking = false)
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.WALKING, enter = true)))
        assertNull(trips.getActiveTrip())

        controller.setIncludeWalking(true)
        assertEquals(true, transitions.includeWalking)
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.WALKING, enter = true)))
        assertEquals(TripSource.AUTO, trips.getActiveTrip()!!.source)
    }

    @Test
    fun ignoredWithoutPermissionOrLocation() = runTest {
        enabled()
        permissions.background = false
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))
        assertNull(trips.getActiveTrip())

        permissions.background = true
        source.enabled = false
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))
        assertNull(trips.getActiveTrip())
    }

    @Test
    fun enableWithoutBackgroundPermissionFails() = runTest {
        permissions.background = false

        assertFalse(controller.enable())
        assertFalse(settings.current().autoTripEnabled)
        assertNull(transitions.subscribed)
    }

    @Test
    fun enableFailsWhenSubscriptionFails() = runTest {
        transitions.fail = true

        assertFalse(controller.enable())
        assertFalse(settings.current().autoTripEnabled)
    }

    @Test
    fun reconcileSwitchesOffWhenPermissionRevoked() = runTest {
        enabled()
        permissions.activity = false

        controller.reconcile()

        assertFalse(settings.current().autoTripEnabled)
        assertTrue(settings.current().autoTripRevokedNotice)
        assertEquals(false, transitions.subscribed)
    }

    @Test
    fun reconcileResubscribesAndEnableClearsNotice() = runTest {
        settings.setAutoTripRevokedNotice(true)
        enabled()
        assertFalse(settings.current().autoTripRevokedNotice)
        transitions.subscribed = null

        controller.reconcile()

        assertEquals(true, transitions.subscribed)
        assertTrue(settings.current().autoTripEnabled)
    }

    @Test
    fun disableKeepsRunningAutoTrip() = runTest {
        enabled()
        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))

        controller.disable()

        assertEquals(false, transitions.subscribed)
        assertEquals(TripStatus.ACTIVE, trips.getActiveTrip()!!.status)
    }

    @Test
    fun stillIsTrackedUntilMovement() = runTest {
        enabled()
        clock = 5_000L

        controller.onTransitions(listOf(TransitionEvent(MotionActivity.STILL, enter = true)))
        assertEquals(5_000L, stillness.stillSince)

        controller.onTransitions(listOf(TransitionEvent(MotionActivity.IN_VEHICLE, enter = true)))
        assertNull(stillness.stillSince)
    }
}
