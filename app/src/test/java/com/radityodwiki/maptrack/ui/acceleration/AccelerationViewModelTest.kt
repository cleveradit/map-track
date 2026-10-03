package com.radityodwiki.maptrack.ui.acceleration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.AccelerationRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.usecase.AccelerationPhase
import com.radityodwiki.maptrack.location.LocationRequestSpec
import com.radityodwiki.maptrack.location.LocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccelerationViewModelTest {

    private class FakeSource : LocationSource {
        var permission = LocationPermission.GRANTED
        val emitted = MutableSharedFlow<GpsFix>(extraBufferCapacity = 100)
        val requests = mutableListOf<LocationRequestSpec>()
        override fun permissionState() = permission
        override fun isLocationEnabled() = true
        override fun fixes(request: LocationRequestSpec): Flow<GpsFix> {
            requests += request
            return emitted
        }
    }

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: AccelerationRepository
    private val source = FakeSource()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = AccelerationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** Starts collecting the screen state and waits until the fake GPS has a subscriber. */
    private suspend fun kotlinx.coroutines.test.TestScope.collect(vm: AccelerationViewModel) {
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        source.emitted.subscriptionCount.first { it > 0 }
    }

    private fun fix(seconds: Int, speed: Float) = GpsFix(0.0, 0.0, 5f, speed, null, null, seconds * 1_000L)

    @Test
    fun fullRunIsSavedOnce() = runTest {
        val vm = AccelerationViewModel(source, repository)
        collect(vm)

        for (s in 0..2) source.emitted.emit(fix(s, 0f))
        var s = 3
        while (vm.uiState.value.measurement.phase != AccelerationPhase.FINISHED && s < 40) {
            source.emitted.emit(fix(s, 4f * (s - 2)))
            s++
        }
        // Later fixes after finishing do not save again.
        source.emitted.emit(fix(s, 0f))

        val runs = repository.observeRuns().first { it.isNotEmpty() }
        assertEquals(1, runs.size)
        assertEquals(6_944.0, runs.single().time0To100KmhMs!!.toDouble(), 100.0)
        assertEquals(1_000L, source.requests.single().intervalMs)
    }

    @Test
    fun withoutPreciseLocationNoGpsIsRequested() = runTest {
        source.permission = LocationPermission.APPROXIMATE_ONLY
        val vm = AccelerationViewModel(source, repository)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        assertFalse(vm.uiState.value.permissionOk)
        assertTrue(source.requests.isEmpty())
    }

    @Test
    fun gpsGapIsNotSavedAndRestartResets() = runTest {
        val vm = AccelerationViewModel(source, repository)
        collect(vm)
        for (s in 0..2) source.emitted.emit(fix(s, 0f))
        source.emitted.emit(fix(3, 4f))
        source.emitted.emit(fix(8, 20f))

        assertEquals(AccelerationPhase.INVALID, vm.uiState.first { it.measurement.phase == AccelerationPhase.INVALID }.measurement.phase)
        assertTrue(repository.observeRuns().first().isEmpty())

        vm.restart()
        assertEquals(AccelerationPhase.WAITING_GPS, vm.uiState.value.measurement.phase)
    }

    @Test
    fun deleteRemovesRun() = runTest {
        val vm = AccelerationViewModel(source, repository)
        collect(vm)
        for (s in 0..2) source.emitted.emit(fix(s, 0f))
        for (s in 3..15) source.emitted.emit(fix(s, 10f))
        vm.stop()
        val run = repository.observeRuns().first { it.isNotEmpty() }.single()

        vm.deleteRun(run.id)

        assertTrue(repository.observeRuns().first().isEmpty())
    }
}
