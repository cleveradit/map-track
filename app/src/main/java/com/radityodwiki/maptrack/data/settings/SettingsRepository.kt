package com.radityodwiki.maptrack.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.radityodwiki.maptrack.domain.model.AppSettings
import com.radityodwiki.maptrack.domain.model.DistanceUnit
import com.radityodwiki.maptrack.location.TrackingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Settings in DataStore Preferences (PRD §26, §38 Fase 4). Unknown stored values fall back to defaults. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTrackingInterval(intervalMs: Long) {
        require(intervalMs in TrackingConfig.INTERVAL_OPTIONS_MS) { "Unsupported interval $intervalMs" }
        dataStore.edit { it[TRACKING_INTERVAL_MS] = intervalMs }
    }

    suspend fun setAccuracyThreshold(meters: Float) {
        require(meters in TrackingConfig.ACCURACY_OPTIONS_METERS) { "Unsupported accuracy threshold $meters" }
        dataStore.edit { it[ACCURACY_THRESHOLD_M] = meters }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { it[DISTANCE_UNIT] = unit.storedValue() }
    }

    suspend fun setMapFollowLocation(enabled: Boolean) {
        dataStore.edit { it[MAP_FOLLOW_LOCATION] = enabled }
    }

    suspend fun setAutoTrip(enabled: Boolean) {
        dataStore.edit { it[AUTO_TRIP_ENABLED] = enabled }
    }

    suspend fun setAutoTripIncludeWalking(enabled: Boolean) {
        dataStore.edit { it[AUTO_TRIP_INCLUDE_WALKING] = enabled }
    }

    suspend fun setAutoTripRevokedNotice(shown: Boolean) {
        dataStore.edit { it[AUTO_TRIP_REVOKED_NOTICE] = shown }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings.DEFAULT
        return AppSettings(
            trackingIntervalMs = this[TRACKING_INTERVAL_MS]?.takeIf { it in TrackingConfig.INTERVAL_OPTIONS_MS }
                ?: defaults.trackingIntervalMs,
            accuracyThresholdMeters = this[ACCURACY_THRESHOLD_M]?.takeIf { it in TrackingConfig.ACCURACY_OPTIONS_METERS }
                ?: defaults.accuracyThresholdMeters,
            distanceUnit = DistanceUnit.entries.firstOrNull { it.storedValue() == this[DISTANCE_UNIT] }
                ?: defaults.distanceUnit,
            mapFollowLocation = this[MAP_FOLLOW_LOCATION] ?: defaults.mapFollowLocation,
            autoTripEnabled = this[AUTO_TRIP_ENABLED] ?: defaults.autoTripEnabled,
            autoTripIncludeWalking = this[AUTO_TRIP_INCLUDE_WALKING] ?: defaults.autoTripIncludeWalking,
            autoTripRevokedNotice = this[AUTO_TRIP_REVOKED_NOTICE] ?: defaults.autoTripRevokedNotice,
        )
    }

    private fun DistanceUnit.storedValue() = name.lowercase()

    private companion object {
        val TRACKING_INTERVAL_MS = longPreferencesKey("tracking_interval_ms")
        val ACCURACY_THRESHOLD_M = floatPreferencesKey("accuracy_threshold_m")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val MAP_FOLLOW_LOCATION = booleanPreferencesKey("map_follow_location")
        val AUTO_TRIP_ENABLED = booleanPreferencesKey("auto_trip_enabled")
        val AUTO_TRIP_INCLUDE_WALKING = booleanPreferencesKey("auto_trip_include_walking")
        val AUTO_TRIP_REVOKED_NOTICE = booleanPreferencesKey("auto_trip_revoked_notice")
    }
}
