package com.radityodwiki.maptrack.data.repository

import androidx.room.withTransaction
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.toDomain
import com.radityodwiki.maptrack.data.local.entity.toEntity
import com.radityodwiki.maptrack.domain.model.Place
import com.radityodwiki.maptrack.domain.model.PlaceInput
import com.radityodwiki.maptrack.domain.usecase.PlaceNameError
import com.radityodwiki.maptrack.domain.usecase.PlaceValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class InvalidPlaceException(
    val nameError: PlaceNameError?,
    val invalidRadius: Boolean,
    val invalidCoordinate: Boolean,
) : IllegalArgumentException("Invalid place: name=$nameError radius=$invalidRadius coordinate=$invalidCoordinate")

/** Saved places (PRD §38 Fase 3). Deleting a place never touches trips or visits. */
class PlaceRepository(
    private val database: MapTrackDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val placeDao = database.placeDao()

    suspend fun createPlace(input: PlaceInput): Result<Place> {
        validate(input)?.let { return Result.failure(it) }
        val time = now()
        val place = Place(
            id = UUID.randomUUID().toString(),
            name = input.name.trim(),
            latitude = input.latitude,
            longitude = input.longitude,
            radiusMeters = input.radiusMeters,
            createdAt = time,
            updatedAt = time,
        )
        placeDao.insert(place.toEntity())
        return Result.success(place)
    }

    suspend fun updatePlace(id: String, input: PlaceInput): Result<Place> {
        validate(input)?.let { return Result.failure(it) }
        return database.withTransaction {
            val current = placeDao.getById(id)
                ?: return@withTransaction Result.failure(NoSuchElementException("Place $id not found"))
            val updated = current.copy(
                name = input.name.trim(),
                latitude = input.latitude,
                longitude = input.longitude,
                radiusMeters = input.radiusMeters,
                updatedAt = now(),
            )
            placeDao.update(updated)
            Result.success(updated.toDomain())
        }
    }

    suspend fun deletePlace(id: String) {
        placeDao.deleteById(id)
    }

    fun observePlaces(): Flow<List<Place>> = placeDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePlace(id: String): Flow<Place?> = placeDao.observeById(id).map { it?.toDomain() }

    suspend fun getPlace(id: String): Place? = placeDao.getById(id)?.toDomain()

    private fun validate(input: PlaceInput): InvalidPlaceException? {
        val nameError = PlaceValidator.nameError(input.name)
        val invalidRadius = !PlaceValidator.isValidRadius(input.radiusMeters)
        val invalidCoordinate = !PlaceValidator.isValidCoordinate(input.latitude, input.longitude)
        return if (nameError != null || invalidRadius || invalidCoordinate) {
            InvalidPlaceException(nameError, invalidRadius, invalidCoordinate)
        } else {
            null
        }
    }
}
