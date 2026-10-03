# Implementation Plan: TICKET-013 (Tabel `visits` dan Migrasi Room 1 → 2)

**Ticket:** `TICKET-013`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-012`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 2 — Skema data |
| Mekanisme migrasi | Room `AutoMigration(from = 1, to = 2)`: hanya menambah tabel dan kolom ber-default, sehingga SQL yang dihasilkan Room cocok persis dengan skema yang divalidasi saat database dibuka |
| Data lama | Tidak ada baris yang diubah/dihapus; trip lama mendapat `visit_detection_version = 0` sehingga di-backfill di TICKET-014 |
| `visit_detection_version` | Kolom `trips`, `INTEGER NOT NULL DEFAULT 0`; tidak dipetakan ke domain `Trip` (detail penyimpanan, bukan data milik pengguna) |
| Test migrasi | Tanpa `room-testing`/`MigrationTestHelper`: unit test Robolectric membangun DB v1 dari `app/schemas/.../1.json`, lalu membukanya dengan `MapTrackDatabase` v2 sehingga Room menjalankan AutoMigration produksi dan memvalidasi skema. Alasan: Robolectric hanya membaca asset `mergeDebugAssets`, sehingga skema yang dipasang sebagai asset test tidak terbaca |
| Constraint visit | Dites terpisah di `VisitDaoTest` (DB in-memory v2) |
| Persetujuan | Instruksi loop Fase 2 dari user (tiket langsung `READY`) |

---

## 2. Objective

Menyediakan penyimpanan visit di Room sebagai data turunan per trip, dengan migrasi yang mempertahankan seluruh data Fase 1 di HP pengguna.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/VisitEntity.kt`
   - Tabel `visits`: `id` (Long, PK auto-increment), `trip_id` (FK → `trips.id`, `ON DELETE CASCADE`), `arrived_at`, `departed_at`, `center_latitude`, `center_longitude`, `point_count`.
   - Index `trip_id`; index unik (`trip_id`, `arrived_at`).
   - Mapper `VisitEntity.toDomain(): Visit`, `Visit.toEntity(): VisitEntity`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/TripEntity.kt`
   - `@ColumnInfo(name = "visit_detection_version", defaultValue = "0") val visitDetectionVersion: Int = 0`
3. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/VisitDao.kt`
   - `@Insert suspend fun insertAll(visits: List<VisitEntity>)`
   - `@Query suspend fun deleteForTrip(tripId: String): Int`
   - `@Query suspend fun getForTrip(tripId: String): List<VisitEntity>` (urut `arrived_at`)
   - `@Query fun observeForTrip(tripId: String): Flow<List<VisitEntity>>` (urut `arrived_at`)
4. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/TripDao.kt`
   - `@Query("UPDATE trips SET visit_detection_version = :version WHERE id = :id") suspend fun setVisitDetectionVersion(id: String, version: Int)`
   - `@Query suspend fun getCompletedIdsWithVisitVersionBelow(version: Int): List<String>`
5. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt`
   - `version = 2`, `entities` + `VisitEntity`, `autoMigrations = [AutoMigration(from = 1, to = 2)]`, `abstract fun visitDao(): VisitDao`.
6. File: `app/schemas/com.radityodwiki.maptrack.data.local.database.MapTrackDatabase/2.json`
   - Dihasilkan Room saat build.
7. File: `gradle/libs.versions.toml`, `app/build.gradle.kts`
   - Tidak berubah (tanpa dependency `room-testing`).
8. File: `app/src/test/java/com/radityodwiki/maptrack/data/local/database/MigrationTest.kt`
   - Test migrasi 1 → 2: DB v1 dibangun dari `createSql` di `1.json` (relatif terhadap working dir modul `app`), diisi data Fase 1, lalu dibuka dengan Room v2.
9. File: `app/src/test/java/com/radityodwiki/maptrack/data/local/dao/VisitDaoTest.kt`
   - Cascade, FK, constraint unik, query backfill, dan `setVisitDetectionVersion` tidak mengubah `updated_at`.

---

## 4. Scope of Changes

### A. Skema

1. Entity, DAO, database version 2, auto-migration.

### B. Test

1. Migrasi dengan data Fase 1, cascade visit, constraint unik.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Migrasi mempertahankan data | DB v1 berisi 1 trip `completed` + 2 titik + 1 trip `active` | Setelah migrasi ke v2: trip & titik utuh, `visit_detection_version = 0`, tabel `visits` kosong; skema tervalidasi Room (`MigrationTest`) | `[x]` |
| Trip lama terdeteksi perlu backfill | DB hasil migrasi | `getCompletedIdsWithVisitVersionBelow(1)` mengembalikan trip tersebut (`MigrationTest`) | `[x]` |
| Boundary: trip aktif / sudah terbaru | Trip `active` versi 0, trip `completed` versi 1 | Tidak dikembalikan oleh query backfill; `setVisitDetectionVersion` tidak mengubah `updated_at` (`VisitDaoTest`, `MigrationTest`) | `[x]` |
| Cascade | Hapus trip yang punya visit | Visit ikut terhapus (`VisitDaoTest`) | `[x]` |
| Failure: visit tanpa trip | Insert visit dengan `trip_id` tidak dikenal | `SQLiteConstraintException` (`VisitDaoTest`) | `[x]` |
| Failure: visit ganda | Dua visit dengan `trip_id` + `arrived_at` sama | `SQLiteConstraintException` (`VisitDaoTest`) | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug`

Expected:

1. Semua test lulus; `2.json` terbentuk.

Hasil (2026-10-03, di `master`):

1. `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 91 test, 0 gagal (`MigrationTest` 1, `VisitDaoTest` 4, `VisitDetectorTest` 13). Run pertama tanpa `--no-build-cache` gagal karena entri cache lokal `kspDebugKotlin` rusak, bukan karena kode.
2. `app/schemas/.../2.json` terbentuk.

---

## 7. Out of Scope

1. Menghitung dan menyimpan visit saat trip selesai / backfill (TICKET-014).
2. UI (TICKET-015).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
