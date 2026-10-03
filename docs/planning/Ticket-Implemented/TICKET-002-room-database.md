# Implementation Plan: TICKET-002 (Room Database: Trip & LocationPoint)

**Ticket:** `TICKET-002`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-001`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Library | Room 2.8.5 + KSP 2.3.12 + Room Gradle plugin (schema export ke `app/schemas/`) |
| Skema | Sesuai PRD §24–§25 v2.0: `trips.id` UUID String, `updated_at` wajib, `location_points` unik (`trip_id`, `recorded_at`), FK `ON DELETE CASCADE` |
| Status trip | Enum domain `TripStatus { ACTIVE, COMPLETED }`, disimpan sebagai string `active` / `completed` |
| Satu trip aktif | Ditegakkan di repository dalam transaksi: `startTrip` gagal jika sudah ada trip `active` |
| Hapus trip aktif | Ditolak di repository (PRD §34) |
| Titik ganda | Insert dengan `OnConflictStrategy.IGNORE` pada index unik |
| Waktu | Disuntikkan lewat `() -> Long` (default `System::currentTimeMillis`) agar dapat dites |
| Test | Robolectric 4.17 + Room in-memory di unit test JVM |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Menyediakan penyimpanan lokal trip dan location point yang menjadi sumber data utama aplikasi, beserta repository yang menegakkan aturan integritas (satu trip aktif, cascade delete, tidak ada titik ganda). Perhitungan statistik dan filtering GPS belum termasuk (TICKET-003).

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/Trip.kt`
   - `enum class TripStatus(val dbValue: String) { ACTIVE("active"), COMPLETED("completed") }`
   - `data class Trip(id: String, startedAt: Long, endedAt: Long?, distanceMeters: Double?, averageSpeedMps: Double?, maxSpeedMps: Double?, status: TripStatus, updatedAt: Long)`
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/LocationPoint.kt`
   - `data class LocationPoint(tripId: String, latitude: Double, longitude: Double, accuracyMeters: Float, speedMps: Float?, bearingDegrees: Float?, altitudeMeters: Double?, recordedAt: Long)`
3. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/TripEntity.kt`, `LocationPointEntity.kt`
   - Tabel `trips` dan `location_points`, nama kolom snake_case sesuai PRD.
4. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/TripDao.kt`
   - `insert(trip)`, `update(trip)`, `getById(id): TripEntity?`, `observeById(id): Flow<TripEntity?>`, `observeAll(): Flow<List<TripEntity>>` (urut `started_at DESC`), `getActive(): TripEntity?`, `observeActive(): Flow<TripEntity?>`, `deleteById(id): Int`.
5. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/LocationPointDao.kt`
   - `insert(point): Long` (IGNORE), `getForTrip(tripId): List<…>` dan `observeForTrip(tripId): Flow<List<…>>` (urut `recorded_at ASC`), `getLast(tripId): LocationPointEntity?`, `countForTrip(tripId): Int`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt`
   - `@Database(version = 1, exportSchema = true)`, `fun create(context): MapTrackDatabase` dengan nama file `map_track.db`.
7. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt`
   - `suspend fun startTrip(): Result<Trip>` — gagal dengan `ActiveTripExistsException` bila ada trip aktif.
   - `suspend fun addPoint(point: LocationPoint): Boolean` — `false` bila titik ganda diabaikan.
   - `suspend fun completeTrip(tripId: String, endedAt: Long, distanceMeters: Double, averageSpeedMps: Double, maxSpeedMps: Double?): Trip`
   - `suspend fun deleteTrip(tripId: String): Result<Unit>` — gagal dengan `ActiveTripDeletionException` bila trip aktif.
   - Tambahan saat implementasi: `suspend fun getTrip(id): Trip?`.
   - `fun observeTrips(): Flow<List<Trip>>`, `fun observeTrip(id): Flow<Trip?>`, `fun observeActiveTrip(): Flow<Trip?>`, `suspend fun getActiveTrip(): Trip?`, `fun observePoints(tripId): Flow<List<LocationPoint>>`, `suspend fun getPoints(tripId): List<LocationPoint>`, `suspend fun getLastPoint(tripId): LocationPoint?`, `suspend fun countPoints(tripId): Int`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`
   - Integration point: properti `database` dan `tripRepository`.

---

## 4. Scope of Changes

### A. Build

1. Tambah plugin KSP dan Room ke version catalog dan `app/build.gradle.kts`.
2. Dependency test: Robolectric, androidx.test core, kotlinx-coroutines-test.

### B. Data layer

1. Entity, DAO, database, mapper entity ↔ domain.
2. Repository dengan aturan integritas.

### C. Dokumentasi

1. `data-model.md` dan `architecture.md` diisi sesuai kode.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Start trip | `startTrip()` tanpa trip aktif | Trip `active`, `endedAt` null, id UUID | `[x]` |
| Failure: trip aktif ganda | `startTrip()` dua kali | Panggilan kedua `Result.failure(ActiveTripExistsException)` | `[x]` |
| Tambah titik | `addPoint` 3 titik berbeda | `getPoints` mengembalikan 3 titik urut `recordedAt` | `[x]` |
| Boundary: titik ganda | `addPoint` dengan (`tripId`, `recordedAt`) sama | Return `false`, jumlah titik tetap | `[x]` |
| Failure: FK tidak valid | `addPoint` dengan `tripId` yang tidak ada | Melempar `SQLiteConstraintException` | `[x]` |
| Complete trip | `completeTrip(...)` | Status `completed`, statistik tersimpan, `updatedAt` = waktu saat itu | `[x]` |
| Failure: hapus trip aktif | `deleteTrip(activeId)` | `Result.failure(ActiveTripDeletionException)`, data tetap | `[x]` |
| Cascade delete | `deleteTrip(completedId)` | Trip dan semua titiknya terhapus | `[x]` |
| Urutan history | 2 trip selesai | `observeTrips` urut `startedAt` terbaru dulu | `[x]` |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 9/9 lulus (`TripRepositoryTest`), `assembleDebug` sukses, lint 0 error (3 warning lama dari TICKET-001), skema `1.json` terbentuk.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

Expected:

1. Semua test lulus, build sukses, lint tanpa error.
2. File skema `app/schemas/com.radityodwiki.maptrack.data.local.database.MapTrackDatabase/1.json` terbentuk.

---

## 7. Out of Scope

1. Filtering GPS dan perhitungan statistik (TICKET-003).
2. UI yang memakai data ini.
3. Kolom atau tabel untuk Fase 2+.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
