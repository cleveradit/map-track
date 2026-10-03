# Implementation Plan: TICKET-004 (Permission Lokasi & Posisi Terkini di Home)

**Ticket:** `TICKET-004`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-003`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber lokasi | Fused Location Provider (`play-services-location` 21.4.0), `PRIORITY_HIGH_ACCURACY`, interval dari `TrackingConfig` |
| Kapan Home meminta lokasi | Hanya selama Home terlihat: `collectAsStateWithLifecycle` + `stateIn(WhileSubscribed(5_000))` (PRD §7.1, battery-aware) |
| Permission | Minta `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` bersamaan, **setelah** kartu penjelasan PRD §28 ditekan. Tidak meminta saat app dibuka |
| Lokasi perkiraan saja | Diperlakukan seperti ditolak (PRD §28), dengan pesan meminta "Lokasi akurat" dan tombol ke pengaturan aplikasi |
| Ditolak | Pesan PRD §30 + tombol "Coba lagi" dan "Buka Pengaturan" |
| Location service mati | Pesan PRD §29 + tombol ke pengaturan Location |
| Status GPS | `NO_PERMISSION` → `LOCATION_DISABLED` → `SEARCHING` (belum ada fix atau fix basi > 15 s) → `ACTIVE` |
| Peta | Placeholder; MapLibre di TICKET-008. Koordinat ditampilkan sebagai teks sementara |
| Tracking control | Belum ada (TICKET-005) |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat memberi izin lokasi dengan penjelasan yang transparan, lalu melihat status GPS, posisi, kecepatan saat ini, dan akurasi di Home. Permintaan lokasi berhenti saat Home tidak terlihat.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/AndroidManifest.xml`
   - Change: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/GpsFix.kt`
   - `data class GpsFix(latitude: Double, longitude: Double, accuracyMeters: Float, speedMps: Float?, bearingDegrees: Float?, altitudeMeters: Double?, time: Long)` + `fun GpsFix.toLocationPoint(tripId: String): LocationPoint`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTracker.kt`
   - `class LocationTracker(context: Context)`
   - `fun permissionState(): LocationPermission` (tanpa memperhitungkan "pernah diminta")
   - `fun isLocationEnabled(): Boolean`
   - `fun fixes(): Flow<GpsFix>` — `callbackFlow`, berhenti (`removeLocationUpdates`) saat collector batal; selesai tanpa error bila izin presisi tidak ada.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeUiState.kt`
   - `enum class LocationPermission { GRANTED, APPROXIMATE_ONLY, DENIED, NOT_REQUESTED }` — saat implementasi dipindah ke `domain/model/LocationPermission.kt` agar `LocationTracker` tidak bergantung pada package UI.
   - `enum class GpsStatus { NO_PERMISSION, LOCATION_DISABLED, SEARCHING, ACTIVE }`
   - `data class HomeUiState(permission, locationEnabled: Boolean, fix: GpsFix?, now: Long)` dengan properti turunan `gpsStatus`.
   - `fun gpsStatusOf(permission, locationEnabled, fix, now): GpsStatus`
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeViewModel.kt`
   - `val uiState: StateFlow<HomeUiState>`, `fun refreshPermission()`, `fun onPermissionResult()`, `companion object { val Factory }`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt`
   - Kartu permission / location service, placeholder peta, baris Kecepatan, Akurasi, GPS, Posisi. Refresh permission pada `ON_RESUME`.
7. File: `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt`
   - `fun formatAccuracy(accuracyMeters: Float?): String` → `± 6 meter` / `—`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`
   - Integration point: properti `locationTracker`.

---

## 4. Scope of Changes

### A. Lokasi

1. Dependency `play-services-location`.
2. `GpsFix`, `LocationTracker`.

### B. Home

1. `HomeUiState`, `HomeViewModel`, `HomeScreen`, string resource.

### C. Test

1. Unit test `gpsStatusOf` dan `formatAccuracy`.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Status tanpa izin | permission `DENIED` / `APPROXIMATE_ONLY` / `NOT_REQUESTED` | `NO_PERMISSION` | `[x]` |
| Status location mati | izin ada, `locationEnabled = false` | `LOCATION_DISABLED` | `[x]` |
| Boundary: fix basi | fix 15 s lalu → `ACTIVE`; 15.001 s lalu → `SEARCHING` | Sesuai | `[x]` |
| Format akurasi | 5.6 m; null | `± 6 meter`; `—` | `[x]` |
| Manual: izin pertama | Buka app baru diinstal | Kartu penjelasan tampil, dialog izin baru muncul setelah tombol ditekan | `[ ]` manual |
| Manual: lokasi tampil | Izinkan "Lokasi akurat", di luar ruangan | Status GPS aktif, kecepatan, akurasi, koordinat terisi dalam ±30 detik | `[ ]` manual |
| Failure manual: izin ditolak | Tolak izin | Pesan PRD §30, tombol Coba lagi dan Buka Pengaturan; tab History tetap bisa dibuka | `[ ]` manual |
| Failure manual: lokasi perkiraan | Pilih "Perkiraan" | Pesan meminta lokasi akurat | `[ ]` manual |
| Manual: location mati | Matikan Location di quick settings lalu kembali ke app | Pesan PRD §29 | `[ ]` manual |
| Manual: hemat baterai | Pindah ke tab History atau app lain > 5 detik | Ikon lokasi di status bar hilang | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 32/32 lulus (baru: `GpsStatusTest` 4, `FormattersTest.accuracy`), `assembleDebug` sukses, lint 0 error. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Start/Stop tracking, foreground service, permission notifikasi (TICKET-005).
2. MapLibre (TICKET-008).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
