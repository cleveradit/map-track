package com.radityodwiki.maptrack.data.repository

import androidx.room.withTransaction
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.TripEntity
import com.radityodwiki.maptrack.data.local.entity.toDomain
import com.radityodwiki.maptrack.data.local.entity.toEntity
import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.Trip
import com.radityodwiki.maptrack.domain.model.TripSource
import com.radityodwiki.maptrack.domain.model.TripStatus
import com.radityodwiki.maptrack.domain.model.Visit
import com.radityodwiki.maptrack.domain.usecase.PlaceDetectionConfig
import com.radityodwiki.maptrack.domain.usecase.TripStatisticsCalculator
import com.radityodwiki.maptrack.domain.usecase.VisitDetector
import com.radityodwiki.maptrack.domain.usecase.VisitRecomputation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ActiveTripExistsException(val activeTripId: String) :
    IllegalStateException("Trip $activeTripId is still active")

class ActiveTripDeletionException(val tripId: String) :
    IllegalStateException("Active trip $tripId cannot be deleted")

class TripRepository(
    private val database: MapTrackDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : VisitRecomputation {
    private val tripDao = database.tripDao()
    private val pointDao = database.locationPointDao()
    private val visitDao = database.visitDao()

    /** Creates a new active trip. Fails if another trip is still active (PRD §8.1). */
    suspend fun startTrip(source: TripSource = TripSource.MANUAL): Result<Trip> = database.withTransaction {
        val active = tripDao.getActive()
        if (active != null) {
            Result.failure(ActiveTripExistsException(active.id))
        } else {
            val time = now()
            val entity = TripEntity(
                id = UUID.randomUUID().toString(),
                startedAt = time,
                endedAt = null,
                distanceMeters = null,
                averageSpeed = null,
                maxSpeed = null,
                status = TripStatus.ACTIVE.dbValue,
                updatedAt = time,
                source = source.dbValue,
            )
            tripDao.insert(entity)
            Result.success(entity.toDomain())
        }
    }

    /** Stores a point. Returns false when a point with the same trip and fix time already exists. */
    suspend fun addPoint(point: LocationPoint): Boolean =
        pointDao.insert(point.toEntity()) != -1L

    suspend fun completeTrip(
        tripId: String,
        endedAt: Long,
        distanceMeters: Double,
        averageSpeedMps: Double,
        maxSpeedMps: Double?,
    ): Trip = database.withTransaction {
        val current = tripDao.getById(tripId) ?: throw NoSuchElementException("Trip $tripId not found")
        val completed = current.copy(
            endedAt = endedAt,
            distanceMeters = distanceMeters,
            averageSpeed = averageSpeedMps,
            maxSpeed = maxSpeedMps,
            status = TripStatus.COMPLETED.dbValue,
            updatedAt = now(),
        )
        tripDao.update(completed)
        completed.toDomain()
    }

    /**
     * Computes statistics and visits from the stored points and marks the trip completed, in one
     * transaction (PRD §8.2, §38 Fase 2). Calling it again on a completed trip returns it unchanged.
     */
    suspend fun finishTrip(tripId: String, endedAt: Long): Trip = database.withTransaction {
        val trip = tripDao.getById(tripId) ?: throw NoSuchElementException("Trip $tripId not found")
        if (trip.status == TripStatus.COMPLETED.dbValue) return@withTransaction trip.toDomain()

        val end = endedAt.coerceAtLeast(trip.startedAt)
        val points = pointDao.getForTrip(tripId).map { it.toDomain() }
        val stats = TripStatisticsCalculator.calculate(points, trip.startedAt, end)
        val completed = trip.copy(
            endedAt = end,
            distanceMeters = stats.distanceMeters,
            averageSpeed = stats.averageSpeedMps,
            maxSpeed = stats.maxSpeedMps,
            status = TripStatus.COMPLETED.dbValue,
            updatedAt = now(),
        )
        tripDao.update(completed)
        replaceVisits(tripId, points)
        completed.toDomain()
    }

    /**
     * Recomputes the visits of a completed trip with an outdated detection version, in one transaction.
     * Skips deleted, active, and up-to-date trips. Leaves updated_at unchanged.
     */
    override suspend fun recomputeVisits(tripId: String): Boolean = database.withTransaction {
        val trip = tripDao.getById(tripId)
        if (trip == null ||
            trip.status != TripStatus.COMPLETED.dbValue ||
            trip.visitDetectionVersion >= PlaceDetectionConfig.DETECTION_VERSION
        ) {
            return@withTransaction false
        }
        replaceVisits(tripId, pointDao.getForTrip(tripId).map { it.toDomain() })
        true
    }

    override suspend fun tripIdsNeedingVisits(): List<String> =
        tripDao.getCompletedIdsWithVisitVersionBelow(PlaceDetectionConfig.DETECTION_VERSION)

    /** Must run inside a transaction: visits are derived data, replaced as a whole. */
    private suspend fun replaceVisits(tripId: String, points: List<LocationPoint>) {
        visitDao.deleteForTrip(tripId)
        visitDao.insertAll(VisitDetector.detect(points).map { it.toEntity() })
        tripDao.setVisitDetectionVersion(tripId, PlaceDetectionConfig.DETECTION_VERSION)
    }

    /** Deletes a completed trip and, via cascade, all its points and visits (PRD §34). */
    suspend fun deleteTrip(tripId: String): Result<Unit> = database.withTransaction {
        val trip = tripDao.getById(tripId)
        when {
            trip == null -> Result.success(Unit)
            trip.status == TripStatus.ACTIVE.dbValue -> Result.failure(ActiveTripDeletionException(tripId))
            else -> {
                tripDao.deleteById(tripId)
                Result.success(Unit)
            }
        }
    }

    fun observeTrips(): Flow<List<Trip>> = tripDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeTrip(id: String): Flow<Trip?> = tripDao.observeById(id).map { it?.toDomain() }

    fun observeActiveTrip(): Flow<Trip?> = tripDao.observeActive().map { it?.toDomain() }

    suspend fun getTrip(id: String): Trip? = tripDao.getById(id)?.toDomain()

    suspend fun getActiveTrip(): Trip? = tripDao.getActive()?.toDomain()

    fun observeAllVisits(): Flow<List<Visit>> = visitDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeVisits(tripId: String): Flow<List<Visit>> =
        visitDao.observeForTrip(tripId).map { list -> list.map { it.toDomain() } }

    fun observePoints(tripId: String): Flow<List<LocationPoint>> =
        pointDao.observeForTrip(tripId).map { list -> list.map { it.toDomain() } }

    suspend fun getPoints(tripId: String): List<LocationPoint> = pointDao.getForTrip(tripId).map { it.toDomain() }

    /** Points recorded at or after [since]; a small window for automatic-trip stop checks. */
    suspend fun getPointsSince(tripId: String, since: Long): List<LocationPoint> =
        pointDao.getForTripSince(tripId, since).map { it.toDomain() }

    suspend fun getLastPoint(tripId: String): LocationPoint? = pointDao.getLast(tripId)?.toDomain()

    suspend fun countPoints(tripId: String): Int = pointDao.countForTrip(tripId)
}
