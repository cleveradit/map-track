package com.radityodwiki.maptrack.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.radityodwiki.maptrack.domain.model.GpsFix
import com.radityodwiki.maptrack.domain.model.LocationPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Wraps the Fused Location Provider. GPS is the only coordinate source (PRD §22). */
class LocationTracker(context: Context) : LocationSource {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    /** Current grant state. Never returns [LocationPermission.NOT_REQUESTED]; callers track that. */
    override fun permissionState(): LocationPermission = when {
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationPermission.GRANTED
        isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationPermission.APPROXIMATE_ONLY
        else -> LocationPermission.DENIED
    }

    override fun isLocationEnabled(): Boolean {
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * High-accuracy fixes at [TrackingConfig.INTERVAL_MS]. Updates are removed when the collector
     * is cancelled. Completes immediately when precise location permission is missing.
     */
    @SuppressLint("MissingPermission")
    override fun fixes(): Flow<GpsFix> = callbackFlow {
        if (permissionState() != LocationPermission.GRANTED) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TrackingConfig.INTERVAL_MS)
            .setMinUpdateIntervalMillis(TrackingConfig.MIN_UPDATE_INTERVAL_MS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toGpsFix()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Location.toGpsFix() = GpsFix(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracy,
    speedMps = if (hasSpeed()) speed else null,
    bearingDegrees = if (hasBearing()) bearing else null,
    altitudeMeters = if (hasAltitude()) altitude else null,
    time = time,
)
