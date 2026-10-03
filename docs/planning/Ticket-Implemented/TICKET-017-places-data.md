# Implementation Plan: TICKET-017 (Tabel `places`, Migrasi 2 → 3, Pencocokan Visit)

**Ticket:** `TICKET-017`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-016`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 3 — Keputusan, Nilai default, Skema data |
| Migrasi | Room `AutoMigration(from = 2, to = 3)`: hanya menambah tabel `places`; tidak ada FK dari `visits` ke `places` |
| ID & `updated_at` | UUID v4 dibuat di perangkat; `created_at` = `updated_at` saat dibuat; `updated_at` diperbarui setiap ubah (sync-ready, Rule 9) |
| Validasi | Nama di-trim, 1–50 karakter (dihitung per code point); radius 50–1 000 m inklusif; latitude −90..90, longitude −180..180. Ditegakkan di `PlaceRepository` (bukan hanya UI) |
| Pencocokan visit | Di Kotlin (`PlaceMatcher`, JVM murni), saat ditampilkan; tidak disimpan. Cocok bila jarak haversine pusat visit ↔ pusat tempat ≤ `radius_meters` (inklusif). Lebih dari satu cocok → pusat terdekat; jarak sama → `id` terkecil (deterministik) |
| Alasan pencocokan di Kotlin | Jumlah visit kecil (beberapa per trip) sehingga `observeAllVisits` + `observePlaces` cukup; haversine tidak tersedia di SQLite tanpa fungsi kustom |
| Urutan tempat | `ORDER BY name COLLATE NOCASE, id` |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat, mengikuti alur Fase 2 (tiket langsung `READY`) |

---

## 2. Objective

Menyediakan penyimpanan tempat bernama milik pengguna dan aturan pencocokan visit ↔ tempat yang menjadi dasar seluruh UI Fase 3, dengan migrasi yang mempertahankan data Fase 1–2.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/Place.kt`
   - `data class Place(id: String, name: String, latitude: Double, longitude: Double, radiusMeters: Double, createdAt: Long, updatedAt: Long)`
   - `data class PlaceInput(name: String, latitude: Double, longitude: Double, radiusMeters: Double)`
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceConfig.kt`
   - `DEFAULT_RADIUS_METERS = 100.0`, `MIN_RADIUS_METERS = 50.0`, `MAX_RADIUS_METERS = 1_000.0`, `NAME_MAX_LENGTH = 50`
3. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceValidator.kt`
   - `enum class PlaceNameError { EMPTY, TOO_LONG }`
   - `object PlaceValidator { fun nameError(name: String): PlaceNameError?; fun isValidRadius(meters: Double): Boolean; fun isValidCoordinate(latitude: Double, longitude: Double): Boolean }`
4. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceMatcher.kt`
   - `object PlaceMatcher { fun match(latitude: Double, longitude: Double, places: List<Place>): Place? }`
5. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/PlaceEntity.kt`
   - Tabel `places`: `id` TEXT PK, `name`, `latitude`, `longitude`, `radius_meters`, `created_at`, `updated_at`; mapper.
6. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/PlaceDao.kt`
   - `insert`, `update`, `deleteById(id): Int`, `getById`, `observeById`, `observeAll` (urut nama).
7. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/VisitDao.kt`
   - `fun observeAll(): Flow<List<VisitEntity>>` (urut `arrived_at` DESC).
8. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt`
   - `version = 3`, `PlaceEntity`, `AutoMigration(from = 2, to = 3)`, `placeDao()`.
9. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/PlaceRepository.kt`
   - `class InvalidPlaceException(val nameError: PlaceNameError?, val invalidRadius: Boolean, val invalidCoordinate: Boolean) : IllegalArgumentException`
   - `suspend fun createPlace(input: PlaceInput): Result<Place>`
   - `suspend fun updatePlace(id: String, input: PlaceInput): Result<Place>` (gagal `NoSuchElementException` bila tidak ada)
   - `suspend fun deletePlace(id: String)`
   - `fun observePlaces(): Flow<List<Place>>`, `fun observePlace(id: String): Flow<Place?>`, `suspend fun getPlace(id: String): Place?`
10. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt`
    - `fun observeAllVisits(): Flow<List<Visit>>`
11. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`
    - `val placeRepository: PlaceRepository by lazy { PlaceRepository(database) }`
12. File: `app/schemas/.../3.json` (dihasilkan Room).
13. Test: `PlaceValidatorTest`, `PlaceMatcherTest`, `PlaceRepositoryTest` (baru), `MigrationTest` (+ 2 → 3).

---

## 4. Scope of Changes

### A. Domain

1. Model, config, validator, matcher.

### B. Data

1. Entity, DAO, database v3, repository, wiring.

### C. Test

1. Validasi, pencocokan, CRUD, migrasi.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Boundary nama | `"  "`, 1 karakter, 50 karakter, 51 karakter, 50 emoji | `EMPTY`, valid, valid, `TOO_LONG`, valid | `[x]` |
| Boundary radius | 49.9, 50, 1 000, 1 000.1 | invalid, valid, valid, invalid | `[x]` |
| Cocok di dalam radius | Visit 99 m dari pusat, radius 100 | Tempat tersebut | `[x]` |
| Boundary tepat di radius / di luar | Visit tepat 100 m (± presisi) / 101 m | Cocok / `null` | `[x]` |
| Tumpang tindih | Dua tempat mencakup visit | Tempat dengan pusat terdekat | `[x]` |
| Buat tempat | Input valid dengan nama ber-spasi | Tersimpan, nama di-trim, UUID, `created_at = updated_at` | `[x]` |
| Failure: input tidak valid | Nama kosong / radius 20 | `InvalidPlaceException`, tidak ada baris | `[x]` |
| Ubah tempat | Ubah radius | `updated_at` baru, `created_at` tetap | `[x]` |
| Failure: ubah tempat yang tidak ada | id tidak dikenal | `NoSuchElementException` | `[x]` |
| Hapus tempat | Tempat + trip dengan visit | Tempat hilang; trip & visit utuh | `[x]` |
| Urutan | "beta", "Alpha", "gamma" | Alpha, beta, gamma | `[x]` |
| Migrasi 2 → 3 | DB v2 berisi trip + titik + visit | Data utuh, tabel `places` kosong, skema tervalidasi | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 127 test, 0 gagal (`PlaceValidatorTest` 5, `PlaceMatcherTest` 6, `PlaceRepositoryTest` 6, `MigrationTest` 2). `3.json` terbentuk.

---

## 7. Out of Scope

1. UI (TICKET-018–021).
2. Kolom sync (`deleted_at`, `synced_at`) — Fase 7.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
