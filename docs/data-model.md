# Data Model Reference

## Data storage

| Storage | Isi | Definisi |
|---|---|---|
| Room database `map_track.db` (versi 1) | Trip dan location point | `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` |
| Skema terekspor | Snapshot skema per versi untuk migrasi | `app/schemas/com.radityodwiki.maptrack.data.local.database.MapTrackDatabase/` |

Konvensi: waktu dalam epoch millis UTC (`Long`), kecepatan dalam m/s, jarak dalam meter.

## Entities or tables

| Entity/table | Purpose | Definition source |
|---|---|---|
| `trips` | Satu sesi tracking beserta statistik akhirnya | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/TripEntity.kt` |
| `location_points` | Titik GPS yang lolos filter, milik satu trip | `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/LocationPointEntity.kt` |

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
| `trips` | `updated_at` | INTEGER | Wajib, diisi saat insert dan saat `completeTrip` |
| `location_points` | `id` | INTEGER | Primary key auto-increment, hanya berlaku lokal |
| `location_points` | `trip_id` | TEXT | Wajib, FK → `trips.id`, `ON DELETE CASCADE` |
| `location_points` | `latitude`, `longitude` | REAL | Wajib |
| `location_points` | `accuracy` | REAL | Wajib, meter |
| `location_points` | `speed` | REAL? | m/s |
| `location_points` | `bearing` | REAL? | derajat |
| `location_points` | `altitude` | REAL? | meter |
| `location_points` | `recorded_at` | INTEGER | Wajib, waktu fix GPS; unik bersama `trip_id` |

## Relations and enums

- `trips` 1 → ∞ `location_points` lewat `trip_id`. Menghapus trip ikut menghapus semua titiknya (cascade).
- Index unik (`trip_id`, `recorded_at`): insert titik ganda diabaikan (`OnConflictStrategy.IGNORE`, `addPoint` mengembalikan `false`).
- Enum `TripStatus` (`domain/model/Trip.kt`): `ACTIVE` ↔ `active`, `COMPLETED` ↔ `completed`.
- Aturan yang ditegakkan `TripRepository`, bukan skema: maksimal satu trip `active` (`ActiveTripExistsException`), trip `active` tidak dapat dihapus (`ActiveTripDeletionException`).
