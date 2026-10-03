package com.radityodwiki.maptrack.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.radityodwiki.maptrack.data.local.database.MapTrackDatabase
import com.radityodwiki.maptrack.domain.model.AccelerationRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccelerationRepositoryTest {

    private lateinit var database: MapTrackDatabase
    private lateinit var repository: AccelerationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MapTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = AccelerationRepository(database)
    }

    @After
    fun tearDown() = database.close()

    private fun run(startedAt: Long) = AccelerationRun(
        id = 0,
        startedAt = startedAt,
        distanceTimesMs = mapOf(100 to 7_071L, 200 to 10_000L),
        distanceSpeedsMps = mapOf(100 to 28.3, 200 to 40.0),
        time0To100KmhMs = 6_944L,
        maxSpeedMps = 40.0,
    )

    @Test
    fun saveObserveAndDelete() = runTest {
        val olderId = repository.save(run(1_000))
        repository.save(run(2_000))

        val runs = repository.observeRuns().first()
        assertEquals(listOf(2_000L, 1_000L), runs.map { it.startedAt })
        assertEquals(run(2_000).copy(id = runs[0].id), runs[0])

        repository.delete(olderId)

        assertEquals(listOf(2_000L), repository.observeRuns().first().map { it.startedAt })
    }
}
