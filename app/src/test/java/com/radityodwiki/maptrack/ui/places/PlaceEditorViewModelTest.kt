package com.radityodwiki.maptrack.ui.places

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.repository.PlaceRepository
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import com.radityodwiki.maptrack.domain.model.PlaceInput
import com.radityodwiki.maptrack.domain.usecase.PlaceNameError
import com.radityodwiki.maptrack.location.LocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaceEditorViewModelTest {

    private class FakeLocationSource : LocationSource {
        var permission = LocationPermission.GRANTED
        var enabled = true
        var fixes: Flow<GpsFix> = flow { awaitCancellation() }
        override fun permissionState() = permission
        override fun isLocationEnabled() = enabled
        override fun fixes() = fixes
    }

    private lateinit var database: MapTrackDatabase
    private lateinit var places: PlaceRepository
    private val location = FakeLocationSource()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        places = PlaceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun editor(placeId: String? = null, lat: Double? = null, lng: Double? = null) =
        PlaceEditorViewModel(placeId, lat, lng, places, location)

    private fun fix(lat: Double, accuracy: Float) = GpsFix(lat, 110.0, accuracy, null, null, null, 0)

    @Test
    fun newPlaceWithoutPoint_cannotSave() {
        val vm = editor()
        vm.onNameChange("Rumah")

        assertFalse(vm.uiState.value.canSave)
        assertEquals(null, vm.uiState.value.cameraRequest)
    }

    @Test
    fun pointFromMap_savesWithDefaultRadius() = runTest {
        val vm = editor()
        vm.onMapCenterPicked(-7.78, 110.36)
        vm.onNameChange(" Rumah ")

        vm.save()

        assertTrue(vm.uiState.first { it.saved }.saved)
        val saved = places.observePlaces().first().single()
        assertEquals("Rumah", saved.name)
        assertEquals(-7.78, saved.latitude, 0.0)
        assertEquals(100.0, saved.radiusMeters, 0.0)
    }

    @Test
    fun pointFromVisit_savesWithoutMap() = runTest {
        val vm = editor(lat = -7.123456789, lng = 110.987654321)
        vm.onNameChange("Kantor")

        assertNotNull(vm.uiState.value.cameraRequest)
        vm.save()

        vm.uiState.first { it.saved }
        assertEquals(-7.123456789, places.observePlaces().first().single().latitude, 0.0)
    }

    @Test
    fun blankName_isNotSaved() = runTest {
        val vm = editor(lat = 0.0, lng = 0.0)
        vm.onNameChange("  ")

        vm.save()

        assertFalse(vm.uiState.value.saved)
        assertEquals(PlaceNameError.EMPTY, vm.uiState.value.nameError)
        assertTrue(vm.uiState.value.showNameError)
        assertTrue(places.observePlaces().first().isEmpty())
    }

    @Test
    fun nameAndRadiusBoundaries() {
        val vm = editor()
        assertFalse(vm.uiState.value.showNameError)

        vm.onNameChange("a".repeat(51))
        assertEquals(PlaceNameError.TOO_LONG, vm.uiState.value.nameError)
        vm.onRadiusChange(20.0)
        assertEquals(50.0, vm.uiState.value.radiusMeters, 0.0)
        vm.onRadiusChange(1_234.0)
        assertEquals(1_000.0, vm.uiState.value.radiusMeters, 0.0)
        vm.onRadiusChange(104.0)
        assertEquals(100.0, vm.uiState.value.radiusMeters, 0.0)
    }

    @Test
    fun editingLoadsAndUpdatesSamePlace() = runTest {
        val place = places.createPlace(PlaceInput("Rumah", -7.0, 110.0, 200.0)).getOrThrow()
        val vm = editor(placeId = place.id)

        val loaded = vm.uiState.first { !it.isLoading }
        assertTrue(loaded.isEditing)
        assertEquals("Rumah", loaded.name)
        assertEquals(200.0, loaded.radiusMeters, 0.0)
        assertNotNull(loaded.cameraRequest)

        vm.onNameChange("Rumah Baru")
        vm.save()

        vm.uiState.first { it.saved }
        assertEquals(listOf("Rumah Baru"), places.observePlaces().first().map { it.name })
        assertEquals(place.id, places.observePlaces().first().single().id)
    }

    @Test
    fun editingUnknownPlace_isNotFound() = runTest {
        val vm = editor(placeId = "missing")

        assertTrue(vm.uiState.first { !it.isLoading }.notFound)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun currentLocation_asksPermissionFirst() {
        location.permission = LocationPermission.DENIED
        val vm = editor()

        vm.useCurrentLocation()
        assertEquals(EditorMessage.PERMISSION_RATIONALE, vm.uiState.value.message)

        vm.onPermissionResult(false)
        assertEquals(EditorMessage.PERMISSION_DENIED, vm.uiState.value.message)
    }

    @Test
    fun currentLocation_locationDisabled() {
        location.enabled = false
        val vm = editor()

        vm.useCurrentLocation()

        assertEquals(EditorMessage.LOCATION_DISABLED, vm.uiState.value.message)
    }

    @Test
    fun currentLocation_usesFirstAccurateFix() = runTest {
        location.fixes = flowOf(fix(1.0, 80f), fix(2.0, 10f))
        val vm = editor(lat = 0.0, lng = 0.0)
        val before = vm.uiState.value.cameraRequest

        vm.useCurrentLocation()

        val state = vm.uiState.first { it.latitude == 2.0 }
        assertFalse(state.isLocating)
        assertNotSame(before, state.cameraRequest)
        assertEquals(2.0, state.cameraRequest!!.latitude, 0.0)
    }

    @Test
    fun currentLocation_timesOut() = runTest {
        val vm = editor()

        vm.useCurrentLocation()

        val state = vm.uiState.first { it.message == EditorMessage.LOCATION_TIMEOUT }
        assertFalse(state.isLocating)
        assertFalse(state.hasPoint)
    }
}
