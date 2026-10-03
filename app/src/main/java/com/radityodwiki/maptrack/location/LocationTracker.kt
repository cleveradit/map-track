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
     * Fixes at the requested interval and priority. Updates are removed when the collector is
     * cancelled. Completes immediately when precise location permission is missing.
     */
    @SuppressLint("MissingPermission")
    override fun fixes(request: LocationRequestSpec): Flow<GpsFix> = callbackFlow {
        if (permissionState() != LocationPermission.GRANTED) {
            close()
            return@callbackFlow
        }
        val priority = if (request.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val locationRequest = LocationRequest.Builder(priority, request.intervalMs)
            .setMinUpdateIntervalMillis(request.intervalMs)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toGpsFix()) }
            }
        }
        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
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
