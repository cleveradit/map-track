package com.radityodwiki.maptrack.data.repository

import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.data.local.entity.toDomain
import com.radityodwiki.maptrack.data.local.entity.toEntity
import com.radityodwiki.maptrack.domain.model.AccelerationRun
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saved acceleration tests; local only (PRD §38 Fase 5, v2.6). */
class AccelerationRepository(database: MapTrackDatabase) {
    private val dao = database.accelerationRunDao()

    /** @return the new run id. */
    suspend fun save(run: AccelerationRun): Long = dao.insert(run.toEntity().copy(id = 0))

    /** Newest first. */
    fun observeRuns(): Flow<List<AccelerationRun>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
