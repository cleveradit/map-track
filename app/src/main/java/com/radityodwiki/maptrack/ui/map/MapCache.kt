package com.radityodwiki.maptrack.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.offline.OfflineManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val TAG = "MapCache"

/** MapLibre's ambient tile cache: the offline map of PRD §38 Fase 4 (v2.4). */
object MapCache {
    private val configured = AtomicBoolean(false)

    /** Sets the cache limit once per process. Call after `MapLibre.getInstance`. */
    fun ensureConfigured(context: Context) {
        if (!configured.compareAndSet(false, true)) return
        OfflineManager.getInstance(context).setMaximumAmbientCacheSize(
            MapConfig.MAP_CACHE_MAX_BYTES,
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit
                override fun onError(message: String) = logError("Setting the map cache size failed: $message")
            },
        )
    }

    /** Removes every cached tile; they are fetched again when shown online. */
    suspend fun clear(context: Context): Result<Unit> = suspendCancellableCoroutine { continuation ->
        OfflineManager.getInstance(context).clearAmbientCache(
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }

                override fun onError(message: String) {
                    if (continuation.isActive) continuation.resume(Result.failure(IllegalStateException(message)))
                }
            },
        )
    }

    // Timber only arrives transitively via MapLibre; the app does not use it.
    @SuppressLint("LogNotTimber")
    private fun logError(message: String) {
        Log.w(TAG, message)
    }
}
