# Data Model Reference

## Data storage

| Storage | Isi | Definisi |
|---|---|---|
| Room database `map_track.db` (versi 5) | Trip, location point, visit, tempat, dan hasil uji akselerasi | `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` |
| DataStore Preferences `settings` | Setting pengguna (Fase 4): `tracking_interval_ms`, `accuracy_threshold_m`, `distance_unit`, `map_follow_location`, `auto_trip_enabled`, `auto_trip_include_walking`, `auto_trip_revoked_notice`; lihat [settings.md](features/settings.md) | `app/src/main/java/com/radityodwiki/maptrack/data/settings/SettingsRepository.kt` |
| Cache tile MapLibre | Ambient cache tile peta, maks. 200 MB (`MapConfig.MAP_CACHE_MAX_BYTES`); dikelola MapLibre, bukan Room | `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapCache.kt` |
| Skema terekspor | Snapshot skema per versi (`1.json`–`5.json`) untuk migrasi | `app/schemas/com.radityodwiki.maptrack.data.local.database.MapTrackDatabase/` |

Konvensi: waktu dalam epoch millis UTC (`Long`), kecepatan dalam m/s, jarak dalam meter.

## Entities or tables

| Entity/table | Purpose | Definition source |
|---|---|---|
| `trips` | Satu sesi tracking beserta statistik akhirnya | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/TripEntity.kt` |
| `location_points` | Titik GPS yang lolos filter, milik satu trip | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/LocationPointEntity.kt` |
| `places` | Tempat bernama milik pengguna (sync-ready) | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/PlaceEntity.kt` |
| `acceleration_runs` | Hasil uji akselerasi; lokal, tidak disinkronkan | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/AccelerationRunEntity.kt` |
| `visits` | Tempat singgah satu trip; data turunan dari `location_points`, dapat dihitung ulang | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/VisitEntity.kt` |

## Key fields

| Entity/table | Field | Type | Rules or constraints |
|---|---|---|---|
| `trips` | `id` | TEXT | Primary key, UUID v4 dibuat di perangkat (`TripRepository.startTrip`) |
| `trips` | `started_at` | INTEGER | Wajib, di-index |
| `trips` | `ended_at` | INTEGER? | `NULL` selama `active` |
| `trips` | `distance_meters` | REAL? | `NULL` selama `active` |
| `trips` | `average_speed` | REAL? | m/s, `NULL` selama `active` |
| `trips` | `max_speed` | REAL? | m/s, `NULL` selama `active` atau bila tidak ada data speed |
| `trips` | `status` | TEXT | `active` / `completed`, di-index |
| `trips` | `updated_at` | INTEGER | Wajib, diisi saat insert dan saat trip diselesaikan (`finishTrip`/`completeTrip`); tidak berubah karena perhitungan visit |
| `trips` | `source` | TEXT | Wajib, `manual` / `auto` (default `manual`; trip sebelum Fase 5 = `manual`); enum `TripSource` |
| `trips` | `visit_detection_version` | INTEGER | Wajib, default `0` (belum dihitung); diisi `PlaceDetectionConfig.DETECTION_VERSION` setelah visit dihitung. Tidak dipetakan ke domain `Trip` |
| `location_points` | `id` | INTEGER | Primary key auto-increment, hanya berlaku lokal |
| `location_points` | `trip_id` | TEXT | Wajib, FK → `trips.id`, `ON DELETE CASCADE` |
| `location_points` | `latitude`, `longitude` | REAL | Wajib |
| `location_points` | `accuracy` | REAL | Wajib, meter |
| `location_points` | `speed` | REAL? | m/s |
| `location_points` | `bearing` | REAL? | derajat |
| `location_points` | `altitude` | REAL? | meter |
| `location_points` | `recorded_at` | INTEGER | Wajib, waktu fix GPS; unik bersama `trip_id` |
| `visits` | `id` | INTEGER | Primary key auto-increment, hanya berlaku lokal |
| `visits` | `trip_id` | TEXT | Wajib, FK → `trips.id`, `ON DELETE CASCADE`, di-index |
| `visits` | `arrived_at`, `departed_at` | INTEGER | Wajib; `recorded_at` titik pertama dan terakhir visit; (`trip_id`, `arrived_at`) unik |
| `visits` | `center_latitude`, `center_longitude` | REAL | Wajib, rata-rata titik visit |
| `visits` | `point_count` | INTEGER | Wajib, ≥ 1 |
| `places` | `id` | TEXT | Primary key, UUID v4 dibuat di perangkat (`PlaceRepository.createPlace`) |
| `places` | `name` | TEXT | Wajib, di-trim, 1–50 karakter (code point), tidak harus unik |
| `places` | `latitude`, `longitude` | REAL | Wajib, −90..90 / −180..180 |
| `places` | `radius_meters` | REAL | Wajib, 50–1 000 |
| `places` | `created_at` | INTEGER | Wajib |
| `places` | `updated_at` | INTEGER | Wajib, diperbarui setiap `updatePlace` |
| `acceleration_runs` | `id` | INTEGER | Primary key auto-increment |
| `acceleration_runs` | `started_at` | INTEGER | Wajib, epoch ms titik nol |
| `acceleration_runs` | `time_100m_ms` … `time_500m_ms` | INTEGER? | ms dari titik nol; `NULL` = target tidak tercapai |
| `acceleration_runs` | `speed_at_100m_mps` … `speed_at_500m_mps` | REAL? | Kecepatan saat lewat target |
| `acceleration_runs` | `time_0_100_kmh_ms` | INTEGER? | ms ke 100 km/jam |
| `acceleration_runs` | `max_speed_mps` | REAL | Wajib |

## Relations and enums

- `trips` 1 → ∞ `location_points` lewat `trip_id`. Menghapus trip ikut menghapus semua titiknya (cascade).
- `trips` 1 → ∞ `visits` lewat `trip_id` (cascade). Visit tidak pernah diubah per baris: dihitung ulang dengan menghapus semua visit trip lalu insert hasil baru dalam satu transaksi.
- Tidak ada relasi FK antara `visits` dan `places`: nama visit dicocokkan saat ditampilkan (`PlaceMatcher`), sehingga menghapus tempat tidak menyentuh visit.
- Aturan validasi tempat ditegakkan `PlaceRepository` (`InvalidPlaceException`), bukan skema.
- Index unik (`trip_id`, `recorded_at`): insert titik ganda diabaikan (`OnConflictStrategy.IGNORE`, `addPoint` mengembalikan `false`).
- Enum `TripStatus` (`domain/model/Trip.kt`): `ACTIVE` ↔ `active`, `COMPLETED` ↔ `completed`. Enum `TripSource`: `MANUAL` ↔ `manual`, `AUTO` ↔ `auto`.
- Trip `auto` yang terlalu pendek (< 300 m atau < 2 menit) dihapus beserta titik dan visit-nya setelah selesai (`TripRecorder`).
- Aturan yang ditegakkan `TripRepository`, bukan skema: maksimal satu trip `active` (`ActiveTripExistsException`), trip `active` tidak dapat dihapus (`ActiveTripDeletionException`).

## Migrations

| Dari → ke | Mekanisme | Perubahan | Test |
|---|---|---|---|
| 1 → 2 (Fase 2) | `AutoMigration(from = 1, to = 2)` | Tabel `visits`; kolom `trips.visit_detection_version INTEGER NOT NULL DEFAULT 0`. Tidak ada baris yang diubah | `MigrationTest` (lihat [DEC-004](decision-log.md)) |
| 2 → 3 (Fase 3) | `AutoMigration(from = 2, to = 3)` | Tabel `places`. Tidak ada baris yang diubah | `MigrationTest.migrate2To3_*` |
| 3 → 4 (Fase 5) | `AutoMigration(from = 3, to = 4)` | Kolom `trips.source TEXT NOT NULL DEFAULT 'manual'` | `MigrationTest.migrate3To4_*` |
| 4 → 5 (Fase 5, v2.6) | `AutoMigration(from = 4, to = 5)` | Tabel `acceleration_runs` | `MigrationTest.migrate4To5_*` |
