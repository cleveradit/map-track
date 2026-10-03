# Implementation Plan: TICKET-026 (Penutup Fase 4: Audit, Verifikasi, Dokumentasi)

**Ticket:** `TICKET-026`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-023`, `TICKET-024`, `TICKET-025`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Audit | Setiap acceptance criteria PRD §38 Fase 4 (v2.4) dipetakan ke kode dan test |
| Verifikasi | `./gradlew testDebugUnitTest assembleDebug lintDebug`; target lint 0 error, 1 warning `OldTargetApi` |
| Uji manual | Checklist §6a, dijalankan user sekali setelah semua fase selesai (ai-context Rule 8) |
| Dokumentasi | `settings.md` (baru), `home.md`, `tracking.md`, `data-model.md`, `architecture.md`, `decision-log.md` (DEC-007), `docs/index.md`, backlog (Fase 5 `OPEN`) |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Memastikan acceptance criteria Fase 4 terpenuhi dan terverifikasi, dokumentasi mencerminkan kode, dan checklist uji manual Fase 4 tersedia.

---

## 3. Non-Negotiable Technical Contract

1. File: `docs/features/settings.md` (baru), terdaftar di `docs/features/index.md`.
2. File: `docs/features/home.md`, `docs/features/tracking.md`, `docs/data-model.md`, `docs/architecture.md`, `docs/decision-log.md`, `docs/index.md`, `docs/backlog.md`.

---

## 4. Audit PRD §38 Fase 4 (v2.4)

| Acceptance criteria | Implementasi | Test | Tiket |
|---|---|---|---|
| Cache peta 200 MB dikonfigurasi saat peta pertama dibuat | `MapCache.ensureConfigured` di `MapLibreMap` | N/A (native MapLibre); uji manual §6a | 023 |
| Mode pesawat: area yang pernah dibuka tetap tampil; area lain kosong, tracking jalan | Ambient cache MapLibre; tracking tidak bergantung peta | Uji manual §6a no. 1–2 | 023 |
| Hapus cache peta | `SettingsViewModel.clearMapCache` → `MapCache.clear` | `SettingsViewModelTest.clearCacheReportsResult` | 023, 025 |
| Ubah interval saat trip aktif tidak mengubah trip berjalan | Snapshot `TrackingParams` di `TrackingController.start` → extra intent | `TrackingControllerTest.startPassesSettingsSnapshotToService` | 024 |
| Ambang accuracy baru dipakai filter pada trip berikutnya | `LocationFilter.evaluate(..., maxAccuracyMeters)` dari snapshot | `LocationFilterTest.customAccuracyThresholdBoundary`, `TripRecorderTest.tripAccuracyThresholdIsApplied` | 024 |
| Imperial mengubah semua tampilan tanpa mengubah data | Formatter + ViewModel menerima `distanceUnit` | `FormattersTest.imperial*`, `SpeedSeriesTest.imperialSamplesAndAxis`, `TripDetailViewModelTest.imperialSettingsChangeDisplayOnly` | 025 |
| Setting bertahan setelah aplikasi ditutup | DataStore Preferences | `SettingsRepositoryTest.valuesSurviveRestart` | 024 |

Celah yang ditemukan: tidak ada.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Regresi + test Fase 4 | `testDebugUnitTest` | Semua lulus | `[x]` |
| Build | `assembleDebug` | Sukses | `[x]` |
| Lint | `lintDebug` | 0 error, 1 warning `OldTargetApi` | `[x]` |
| Boundary / failure | N/A — tiket audit dan dokumentasi; kasus batas dan gagal dites di TICKET-023–025 | — | N/A |
| Manual di HP | Checklist §6a | Semua langkah sesuai | `[ ]` manual (user, setelah semua fase selesai) |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`

---

## 6a. Checklist Uji Manual Fase 4 (untuk user)

Checklist ini dijalankan user sekali bersama checklist fase lain setelah semua fase selesai, bukan di akhir fase ini (ai-context Rule 8, PRD v2.4).

| # | Langkah | Hasil yang diharapkan | Tiket |
|---|---|---|---|
| 1 | Online: buka Home dan satu Trip Detail, geser/zoom sekitar lokasi. Lalu mode pesawat (GPS tetap aktif) dan buka keduanya lagi | Peta area tadi tetap tampil dari cache | 023 |
| 2 | Masih mode pesawat, geser ke kota yang belum pernah dibuka; Start Tracking dan bergerak | Area baru kosong; tracking tetap berjalan dan tersimpan | 023 |
| 3 | Home → ikon Settings | Halaman Settings: Tracking, Tampilan, Peta, Tentang (versi, atribusi, privasi) | 025 |
| 4 | Settings → Hapus cache peta → Hapus; mode pesawat, buka Home | Pesan "Cache peta dihapus."; peta kosong sampai online lagi | 023, 025 |
| 5 | Start Tracking dengan interval 5 detik; selama trip ubah ke 30 detik; Stop. Start lagi | Trip pertama tetap ±5 detik antar-titik (Jumlah titik di Trip Detail); trip kedua ±30 detik | 024 |
| 6 | Pilih ambang akurasi 20 m, rekam di dalam gedung | Titik dengan akurasi > 20 m tidak tersimpan (lebih sedikit titik) | 024 |
| 7 | Pilih Imperial | Home (mph, ft), notification, History (mi/ft), Trip Detail (mi, mph, judul grafik mph), Tempat & form tempat (ft/mi) berubah; kembali ke Metrik mengembalikan semuanya | 025 |
| 8 | Matikan "Kamera Home mengikuti posisi", buka Home dan berjalan | Kamera zoom ke posisi sekali lalu diam; tombol "Ikuti posisi" tersedia | 025 |
| 9 | Ubah beberapa setting, tutup paksa aplikasi, buka lagi | Setting tetap sama | 024 |

Bila ada langkah yang gagal, catat di backlog dengan merek/versi HP.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug lintDebug` → `BUILD SUCCESSFUL`; 171 test, 0 gagal; lint 0 error, 1 warning (`OldTargetApi`). Lint pertama memunculkan `PluralsCandidate` pada `settings_interval_option`; penekanan dipindah ke root `strings.xml` (`tools:ignore="PluralsCandidate"`) karena bahasa Indonesia tidak berbentuk jamak.

---

## 7. Out of Scope

1. Fitur Fase 5.
2. Unduhan wilayah (dihapus, PRD v2.4).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (uji manual di HP dijalankan user setelah semua fase selesai).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
