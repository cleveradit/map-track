package com.radityodwiki.maptrack.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    /** DataStore allows one instance per file per process, so each "app start" gets its own scope. */
    private fun dataStore(file: File) = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it },
        produceFile = { file },
    )

    private fun file() = File(folder.root, "settings.preferences_pb")

    @Test
    fun emptyStore_returnsDefaults() = runTest {
        assertEquals(AppSettings.DEFAULT, SettingsRepository(dataStore(file())).current())
    }

    @Test
    fun valuesSurviveRestart() = runTest {
        val file = file()
        val repository = SettingsRepository(dataStore(file))
        repository.setTrackingInterval(10_000)
        repository.setAccuracyThreshold(20f)
        repository.setDistanceUnit(DistanceUnit.IMPERIAL)
        repository.setMapFollowLocation(false)
        scopes.forEach { it.cancel() }

        val reopened = SettingsRepository(dataStore(file)).current()

        assertEquals(AppSettings(10_000, 20f, DistanceUnit.IMPERIAL, false), reopened)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedInterval_isRejected() = runTest {
        SettingsRepository(dataStore(file())).setTrackingInterval(7_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedAccuracy_isRejected() = runTest {
        SettingsRepository(dataStore(file())).setAccuracyThreshold(25f)
    }

    @Test
    fun unknownStoredValue_fallsBackToDefault() = runTest {
        val store = dataStore(file())
        store.edit { it[longPreferencesKey("tracking_interval_ms")] = 7_000 }

        assertEquals(AppSettings.DEFAULT.trackingIntervalMs, SettingsRepository(store).current().trackingIntervalMs)
    }

    @Test
    fun autoTripSettingsDefaultOffAndPersist() = runTest {
        val repository = SettingsRepository(dataStore(file()))
        val defaults = repository.current()
        assertEquals(false, defaults.autoTripEnabled)
        assertEquals(false, defaults.autoTripIncludeWalking)
        assertEquals(false, defaults.autoTripRevokedNotice)

        repository.setAutoTrip(true)
        repository.setAutoTripIncludeWalking(true)
        repository.setAutoTripRevokedNotice(true)

        val current = repository.current()
        assertEquals(true, current.autoTripEnabled)
        assertEquals(true, current.autoTripIncludeWalking)
        assertEquals(true, current.autoTripRevokedNotice)
    }
}
