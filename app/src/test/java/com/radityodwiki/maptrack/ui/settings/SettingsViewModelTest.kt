package com.radityodwiki.maptrack.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.radityodwiki.maptrack.data.settings.SettingsRepository
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.location.AutoTripPermissions
import com.radityodwiki.maptrack.location.AutoTripToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") },
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun settersPersist() = runTest {
        val vm = SettingsViewModel(repository) { Result.success(Unit) }

        vm.setTrackingInterval(30_000)
        vm.setAccuracyThreshold(100f)
        vm.setDistanceUnit(DistanceUnit.IMPERIAL)
        vm.setMapFollowLocation(false)

        val expected = AppSettings(30_000, 100f, DistanceUnit.IMPERIAL, false)
        assertEquals(expected, repository.settings.first { it == expected })
    }

    @Test
    fun unsupportedValuesAreIgnored() = runTest {
        val vm = SettingsViewModel(repository) { Result.success(Unit) }

        vm.setTrackingInterval(7_000)
        vm.setAccuracyThreshold(25f)

        assertEquals(AppSettings.DEFAULT, repository.current())
    }

    @Test
    fun clearCacheReportsResult() = runTest {
        var result = Result.success(Unit)
        val vm = SettingsViewModel(repository) { result }

        vm.clearMapCache()
        assertEquals(CacheMessage.CLEARED, vm.cacheMessage.first { it != null })
        vm.dismissCacheMessage()
        result = Result.failure(IllegalStateException("io"))
        vm.clearMapCache()
        assertEquals(CacheMessage.FAILED, vm.cacheMessage.first { it != null })
    }

    private class FakeAutoTrip : AutoTripToggle, AutoTripPermissions {
        var activity = false
        var background = false
        var enableResult = true
        var enabled = false
        override suspend fun enable() = enableResult.also { enabled = it }
        override suspend fun disable() {
            enabled = false
        }
        override suspend fun setIncludeWalking(enabled: Boolean) = Unit
        override fun hasActivityRecognition() = activity
        override fun hasBackgroundLocation() = background
    }

    @Test
    fun autoTripAsksEachPermissionAfterItsExplanation() = runTest {
        val auto = FakeAutoTrip()
        val vm = SettingsViewModel(repository, auto, auto, preciseLocation = { true }) { Result.success(Unit) }

        vm.requestAutoTrip()
        assertEquals(AutoTripStep.ACTIVITY_RATIONALE, vm.autoTripStep.value)

        auto.activity = true
        vm.onAutoTripPermissionResult(true)
        assertEquals(AutoTripStep.BACKGROUND_RATIONALE, vm.autoTripStep.value)

        auto.background = true
        vm.onAutoTripPermissionResult(true)
        assertNull(vm.autoTripStep.value)
        assertEquals(true, auto.enabled)
    }

    @Test
    fun autoTripDeniedOrWithoutPreciseLocation() = runTest {
        val auto = FakeAutoTrip()
        val noPrecise = SettingsViewModel(repository, auto, auto, preciseLocation = { false }) { Result.success(Unit) }
        noPrecise.requestAutoTrip()
        assertEquals(AutoTripStep.NEED_PRECISE_LOCATION, noPrecise.autoTripStep.value)

        val vm = SettingsViewModel(repository, auto, auto, preciseLocation = { true }) { Result.success(Unit) }
        vm.requestAutoTrip()
        vm.onAutoTripPermissionResult(false)
        assertEquals(AutoTripStep.DENIED, vm.autoTripStep.value)
        assertEquals(false, auto.enabled)
    }

    @Test
    fun autoTripEnableFailureIsReported() = runTest {
        val auto = FakeAutoTrip().apply {
            activity = true
            background = true
            enableResult = false
        }
        val vm = SettingsViewModel(repository, auto, auto, preciseLocation = { true }) { Result.success(Unit) }

        vm.requestAutoTrip()

        assertEquals(AutoTripStep.FAILED, vm.autoTripStep.first { it != null })
    }
}
