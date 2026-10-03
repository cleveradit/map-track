package com.radityodwiki.maptrack.domain.usecase

import kotlin.coroutines.cancellation.CancellationException

/** What [VisitBackfill] needs from storage; implemented by TripRepository. */
interface VisitRecomputation {
    /** Completed trips whose visits were computed with an older algorithm, or never. */
    suspend fun tripIdsNeedingVisits(): List<String>

    /** @return true when the trip's visits were recomputed, false when it was skipped. */
    suspend fun recomputeVisits(tripId: String): Boolean
}

/**
 * Computes visits for trips completed before Fase 2 or before the latest [PlaceDetectionConfig.DETECTION_VERSION]
 * (PRD §38 Fase 2). A failing trip is reported and skipped; it keeps its old version and is retried on the next run.
 */
class VisitBackfill(
    private val store: VisitRecomputation,
    private val onFailure: (tripId: String, error: Exception) -> Unit = { _, _ -> },
) {

    /** @return the number of trips whose visits were recomputed. */
    suspend fun run(): Int {
        var recomputed = 0
        for (tripId in store.tripIdsNeedingVisits()) {
            try {
                if (store.recomputeVisits(tripId)) recomputed++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(tripId, e)
            }
        }
        return recomputed
    }
}
