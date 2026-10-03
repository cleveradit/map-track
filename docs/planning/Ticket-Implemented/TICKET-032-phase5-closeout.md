# Implementation Plan: TICKET-032 (Penutup Fase 5: Audit, Verifikasi, Dokumentasi)

**Ticket:** `TICKET-032`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-027`, `TICKET-028`, `TICKET-029`, `TICKET-030`, `TICKET-031`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Audit | Setiap acceptance criteria PRD §38 Fase 5 (v2.5) dipetakan ke kode dan test |
| Verifikasi | `./gradlew testDebugUnitTest assembleDebug lintDebug`; target lint 0 error, 1 warning `OldTargetApi` |
| Uji manual | Checklist §6a, dijalankan user sekali setelah semua fase selesai (ai-context Rule 8) |
| Dokumentasi | `auto-trip.md` (baru), `tracking.md`, `home.md`, `history.md`, `trip-detail.md`, `data-model.md`, `architecture.md`, `decision-log.md` (DEC-008, DEC-009), `docs/index.md`, backlog Fase 6 |
| Fase 6 | Tidak dimulai: user meminta pilihan backend (Laravel + VPS vs. Firebase) didiskusikan dan PRD direvisi dulu |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Memastikan acceptance criteria Fase 5 terpenuhi dan terverifikasi, dokumentasi mencerminkan kode, dan checklist uji manual Fase 5 tersedia.

---

## 3. Non-Negotiable Technical Contract

1. File: `docs/features/auto-trip.md` (baru), terdaftar di `docs/features/index.md`.
2. File: dokumen yang disebut di §1 baris Dokumentasi.

---

## 4. Audit PRD §38 Fase 5 (v2.5)

| Acceptance criteria | Implementasi | Test | Tiket |
|---|---|---|---|
| Trip otomatis mulai saat berkendara tanpa membuka aplikasi; berhenti setelah diam ≥ 5 menit | `ActivityTransitionReceiver` → `AutoTripController.onTransitions` → `TrackingController.start(AUTO)`; service + `AutoStopPolicy` | `AutoTripControllerTest.vehicleTransitionStartsAutoTrip`, `AutoStopPolicyTest`; perangkat: uji manual §6a no. 3–4 | 031 |
| Trip otomatis 250 m atau 90 detik dihapus; 300 m dan 2 menit dipertahankan (boundary) | `Trip.isTooShortAutoTrip` di `TripRecorder` | `TripRecorderTest.shortAutoTripsAreDeleted` | 031 |
| Menolak `ACCESS_BACKGROUND_LOCATION` → trip otomatis nonaktif, tracking manual tetap jalan (failure) | `AutoTripController.enable`, `SettingsViewModel` | `AutoTripControllerTest.enableWithoutBackgroundPermissionFails`, `SettingsViewModelTest.autoTripDeniedOrWithoutPreciseLocation` | 031 |
| Trip manual tidak pernah dihentikan otomatis | Loop henti hanya untuk `source = auto`; trip manual tidak dihapus | `TripRecorderTest.shortManualTripIsKept`; uji manual §6a no. 6 | 031 |
| Terputus 30 menit dapat dilanjutkan; 90 menit hanya dapat diakhiri | `canResumeTrip`, `TrackingController.resume` | `InterruptedTripTest.resumeWindowBoundary`, `TrackingControllerTest.resume*` | 029 |
| Diam 10 menit dengan jitter (accuracy 10–30 m) menambah jarak < 50 m | `anchoredDistance` + syarat kecepatan (PRD v2.5) | `TripStatisticsCalculatorTest.stationaryJitterAddsLessThanFiftyMeters`; rekaman nyata: uji manual §6a no. 8 | 028 |
| Interval GPS turun saat diam dan kembali saat bergerak | `StationaryDetector` + request dinamis di service | `StationaryDetectorTest`; uji manual §6a no. 9 | 030 |
| Migrasi 3 → 4 dites; trip lama berlabel manual | `AutoMigration(3, 4)` | `MigrationTest.migrate3To4_marksExistingTripsManual` | 027 |

Celah yang ditemukan: aturan jangkar PRD saja tidak memenuhi kriteria jitter pada simulasi → ditambah syarat kecepatan (PRD v2.5, DEC-008).

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Regresi + test Fase 5 | `testDebugUnitTest` | Semua lulus | `[x]` |
| Build | `assembleDebug` | Sukses | `[x]` |
| Lint | `lintDebug` | 0 error, 1 warning `OldTargetApi` | `[x]` |
| Boundary / failure | N/A — tiket audit dan dokumentasi; kasus batas dan gagal dites di TICKET-027–031 | — | N/A |
| Manual di HP | Checklist §6a | Semua langkah sesuai | `[ ]` manual (user, setelah semua fase selesai) |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`

---

## 6a. Checklist Uji Manual Fase 5 (untuk user)

Checklist ini dijalankan user sekali bersama checklist fase lain setelah semua fase selesai, bukan di akhir fase ini (ai-context Rule 8).

| # | Langkah | Hasil yang diharapkan | Tiket |
|---|---|---|---|
| 1 | Instal di atas build lama yang berisi trip | Trip lama tetap ada tanpa label "Otomatis" | 027 |
| 2 | Settings → nyalakan Trip otomatis; tolak izin aktivitas fisik (atau "lokasi sepanjang waktu") | Penjelasan muncul sebelum tiap dialog izin; setelah ditolak switch tetap mati dengan pesan; Start Tracking manual tetap berfungsi | 031 |
| 3 | Nyalakan lagi dan izinkan semuanya ("Izinkan sepanjang waktu"); tutup aplikasi; mulai berkendara/bersepeda beberapa menit | Notification "Perjalanan otomatis" muncul tanpa membuka aplikasi | 031 |
| 4 | Berhenti dan diam ≥ 5 menit | Notification hilang; trip di History berlabel "Otomatis", berakhir di titik terakhir, tempat singgah tujuan tercatat | 031 |
| 5 | Picu trip otomatis lalu berhenti setelah < 300 m | Trip itu tidak muncul di History | 031 |
| 6 | Start Tracking manual, diam 10 menit | Trip manual tidak berhenti sendiri | 031 |
| 7 | Start manual, force stop aplikasi; buka lagi dalam 30 menit → Lanjutkan; berjalan lalu Stop. Ulangi tetapi tunggu > 60 menit | Lanjutkan tersedia dan trip yang sama berlanjut (satu trip di History). Setelah > 60 menit hanya Akhiri Trip | 029 |
| 8 | Start manual, letakkan HP diam 10 menit (di dalam gedung juga), Stop | Jarak trip < 50 m | 028 |
| 9 | Selama tracking diam > 2 menit, lalu bergerak | Ikon lokasi status bar tetap ada; titik tersimpan lebih jarang saat diam (±30 s di Trip Detail) dan kembali sesuai interval saat bergerak | 030 |
| 10 | Matikan Trip otomatis saat trip otomatis berjalan | Trip tetap berjalan sampai diam/Stop; trip baru tidak dimulai otomatis lagi | 031 |
| 11 | Dengan Trip otomatis aktif, cabut izin aktivitas fisik dari pengaturan sistem, buka aplikasi → Settings | Switch mati dengan keterangan izin dicabut | 031 |
| 12 | Dengan Trip otomatis aktif, restart HP lalu berkendara | Trip otomatis tetap tercatat tanpa membuka aplikasi | 031 |

Catatan: beberapa vendor (Xiaomi, Oppo, Vivo) membatasi aktivitas background. Bila langkah 3 atau 12 gagal, nonaktifkan optimasi baterai untuk Map Track lalu ulangi, dan catat merek/versi HP di backlog.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` dan `lintDebug` → `BUILD SUCCESSFUL`; 217 test, 0 gagal; lint 0 error, 1 warning (`OldTargetApi`).

---

## 7. Out of Scope

1. Fase 6 (akun) — menunggu keputusan backend.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (uji manual di HP dijalankan user setelah semua fase selesai).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
