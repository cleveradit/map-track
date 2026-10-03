# Implementation Plan: TICKET-024 (Data Settings & Parameter Tracking per Trip)

**Ticket:** `TICKET-024`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-023`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §26, §38 Fase 4 — Settings (v2.4) |
| Penyimpanan | DataStore Preferences `androidx.datastore:datastore-preferences:1.2.1` (stabil terbaru), file `settings` |
| Key & default | `tracking_interval_ms` (3/5/10/30 s, default 5 s), `accuracy_threshold_m` (20/30/50/100 m, default 50 m), `distance_unit` (`metric`/`imperial`, default metrik), `map_follow_location` (default aktif). Nilai tersimpan di luar pilihan → default |
| Sumber pilihan & default | `TrackingConfig` (Rule 4): `INTERVAL_OPTIONS_MS`, `ACCURACY_OPTIONS_METERS`; `INTERVAL_MS` dan `MAX_ACCURACY_METERS` tetap menjadi default |
| Snapshot per trip | `TrackingParams(intervalMs, maxAccuracyMeters)` dibaca `TrackingController.start()` dari settings, dikirim ke service lewat extra intent, dan dipakai sampai trip selesai. Mengubah setting saat trip aktif tidak memengaruhi trip itu |
| Request lokasi | `LocationSource.fixes(request: LocationRequestSpec = LocationRequestSpec.DEFAULT)`; `LocationRequestSpec(intervalMs, highAccuracy)` juga dipakai penghemat baterai Fase 5. Interval minimum update = interval yang diminta |
| Filter | `LocationFilter.evaluate(candidate, previous, maxAccuracyMeters = TrackingConfig.MAX_ACCURACY_METERS)`; `TripRecorder.record(tripId, fix, maxAccuracyMeters)` |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Setting tersimpan permanen di perangkat, dan interval serta ambang accuracy yang dipilih pengguna dipakai mulai trip berikutnya tanpa mengubah trip yang sedang berjalan.

---

## 3. Non-Negotiable Technical Contract

1. File: `gradle/libs.versions.toml`, `app/build.gradle.kts` — `androidx-datastore-preferences` 1.2.1.
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingConfig.kt`
   - `val INTERVAL_OPTIONS_MS = listOf(3_000L, 5_000L, 10_000L, 30_000L)`, `val ACCURACY_OPTIONS_METERS = listOf(20f, 30f, 50f, 100f)`
3. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/AppSettings.kt`
   - `enum class DistanceUnit { METRIC, IMPERIAL }`
   - `data class AppSettings(trackingIntervalMs: Long, accuracyThresholdMeters: Float, distanceUnit: DistanceUnit, mapFollowLocation: Boolean)` + `DEFAULT`
   - `data class TrackingParams(intervalMs: Long, maxAccuracyMeters: Float)` + `DEFAULT`; `fun AppSettings.trackingParams()`
4. File: `app/src/main/java/com/radityodwiki/maptrack/data/settings/SettingsRepository.kt`
   - `class SettingsRepository(dataStore: DataStore<Preferences>)`: `val settings: Flow<AppSettings>`, `suspend fun current(): AppSettings`, `setTrackingInterval(ms)`, `setAccuracyThreshold(m)`, `setDistanceUnit(unit)`, `setMapFollowLocation(enabled)` (nilai di luar pilihan → `IllegalArgumentException`)
5. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationSource.kt`, `LocationTracker.kt`
   - `data class LocationRequestSpec(val intervalMs: Long, val highAccuracy: Boolean)` + `DEFAULT`; `fun fixes(request: LocationRequestSpec = LocationRequestSpec.DEFAULT): Flow<GpsFix>`
6. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt`
   - Konstruktor + `trackingParams: suspend () -> TrackingParams = { TrackingParams.DEFAULT }`; `TrackingServiceLauncher.start(tripId: String, params: TrackingParams)`
7. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt`
   - Extra `interval_ms`, `max_accuracy_m`; dipakai untuk `fixes()` dan `record()`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/LocationFilter.kt`, `TripRecorder.kt` — parameter accuracy.
9. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt` — `settingsRepository`; controller membaca `settingsRepository.current().trackingParams()`.
10. Test: `data/settings/SettingsRepositoryTest.kt` (baru), `LocationFilterTest`, `TripRecorderTest`, `TrackingControllerTest`.

---

## 4. Scope of Changes

### A. Settings

1. Dependency, model, repository, wiring.

### B. Tracking

1. Spesifikasi request, snapshot parameter, filter.

### C. Test

1. Repository, filter, controller.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Default | DataStore kosong | `AppSettings.DEFAULT` (5 s, 50 m, metrik, ikuti posisi) | `[x]` |
| Simpan & baca ulang | Set 10 s, 20 m, imperial, follow off; buat repository baru di file yang sama | Nilai bertahan | `[x]` |
| Failure: nilai tidak valid | `setTrackingInterval(7_000)` / nilai tersimpan 7 000 | `IllegalArgumentException` / dibaca sebagai default | `[x]` |
| Boundary filter | accuracy 20.0 dan 20.1 dengan ambang 20 | Diterima / `POOR_ACCURACY` | `[x]` |
| Snapshot saat Start | Settings 10 s / 30 m saat Start | Launcher menerima `TrackingParams(10_000, 30f)` | `[x]` |
| Ubah setting saat trip aktif | Start dengan 5 s, ubah ke 30 s | Trip berjalan tetap memakai 5 s (snapshot); Start berikutnya 30 s | `[x]` |
| Recorder | `record(..., maxAccuracyMeters = 20f)` fix 30 m | Ditolak `POOR_ACCURACY` | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 163 test, 0 gagal (`SettingsRepositoryTest` 5, `LocationFilterTest` 8, `TripRecorderTest` 12, `TrackingControllerTest` 10). `TrackingConfig.MIN_UPDATE_INTERVAL_MS` dihapus: interval minimum update kini sama dengan interval yang diminta.

---

## 7. Out of Scope

1. Halaman Settings, satuan imperial di tampilan, kamera ikuti posisi (TICKET-025).
2. Penghemat baterai (Fase 5).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
