# Implementation Plan: TICKET-016 (Penutup Fase 2: Audit, Verifikasi, Dokumentasi)

**Ticket:** `TICKET-016`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-012`, `TICKET-013`, `TICKET-014`, `TICKET-015`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Audit | Setiap acceptance criteria PRD §38 Fase 2 dipetakan ke kode dan test; celah yang ditemukan diperbaiki di tiket ini atau dicatat di backlog |
| Verifikasi | `./gradlew testDebugUnitTest assembleDebug lintDebug`; target lint 0 error, 1 warning `OldTargetApi` |
| Uji manual | Checklist Fase 2 untuk user di §6a, dijalankan sekali setelah semua fase selesai (ai-context Rule 8, PRD v2.3); tidak memblokir Fase 3 |
| Dokumentasi | Feature doc `place-detection.md` (baru), `trip-detail.md`, `tracking.md`, `data-model.md`, `architecture.md`, `decision-log.md`, `docs/index.md`, backlog Fase 3, `current-session.md` (diminta user secara eksplisit) |
| Persetujuan | Instruksi Fase 2 dari user di chat (tiket langsung `READY`) |

---

## 2. Objective

Memastikan seluruh acceptance criteria Fase 2 terpenuhi dan terverifikasi, dokumentasi mencerminkan kode, dan tersedia checklist uji manual Fase 2 untuk pengujian akhir di HP setelah semua fase selesai.

---

## 3. Non-Negotiable Technical Contract

1. File: `docs/features/place-detection.md` (baru), terdaftar di `docs/features/index.md`.
2. File: `docs/features/trip-detail.md`, `docs/features/tracking.md`, `docs/data-model.md`, `docs/architecture.md`, `docs/index.md`.
3. File: `docs/decision-log.md` — hanya entri yang memenuhi kriteria ai-context §5 Step 4.
4. File: `docs/backlog.md` — Fase 3 `OPEN` (uji manual tidak menjadi syarat antar-fase).
5. File: `docs/planning/current-session.md`.

---

## 4. Audit PRD §38 Fase 2

| Acceptance criteria | Implementasi | Test | Tiket |
|---|---|---|---|
| Stop, Stop dari notification, Akhiri Trip menghasilkan visit dalam transaksi yang sama dengan statistik | Semua jalur → `TripRepository.finishTrip()` → `replaceVisits()` di `withTransaction` yang sama | `TripRecorderTest.finishStoresVisits` (Stop & notification = `ACTION_STOP` → `finish`), `finishInterruptedStoresVisits`, `TrackingControllerTest.stopWithoutServiceStoresVisits`, `endInterruptedTripStoresVisits` | 014 |
| Diam ≥ 5 menit dalam 100 m → tepat satu visit; < 5 menit → tidak ada (boundary) | `VisitDetector` + `PlaceDetectionConfig` | `VisitDetectorTest` (tepat 5 menit, 5 menit − 1 detik) | 012 |
| Satu titik melenceng tidak memecah visit | Penggabungan `MERGE_GAP_MS` + `MERGE_DISTANCE_METERS` setelah filter durasi | `VisitDetectorTest` | 012 |
| Trip < 2 titik: tanpa visit, tanpa error (failure) | `VisitDetector.detect` mengembalikan list kosong; versi tetap ditulis | `TripRepositoryTest.finishTrip_withFewerThanTwoPoints_hasNoVisits` | 012, 014 |
| Hapus trip ikut menghapus visit | FK `ON DELETE CASCADE` | `VisitDaoTest.deletingTrip_cascadesToVisits`, `TripRepositoryTest.deleteTrip_cascadesToVisits` | 013, 014 |
| Trip lama mendapat visit setelah app dibuka, tanpa menghapus data; migrasi 1 → 2 dites | `AutoMigration(1, 2)`; `VisitBackfill` dari `MapTrackApplication.onCreate` | `MigrationTest`, `VisitBackfillTest`, `TripRepositoryTest.recomputeVisits_*` | 013, 014 |
| Visit tampil di Trip Detail sebagai marker dan daftar | `TripRouteMap` (layer `route-visit-halo`/`route-visit-dot`), `VisitList` | `TripDetailViewModelTest`, `VisitItemTest`; tampilan: uji manual §6a | 015 |
| Semua berjalan tanpa internet | Deteksi, penyimpanan, backfill, dan daftar sepenuhnya lokal; hanya tile peta yang butuh jaringan | Uji manual §6a no. 6 | 012–015 |

Celah yang ditemukan: tidak ada. Catatan: `TripRepository.completeTrip` (hanya dipakai test Fase 1) tidak menghitung visit; trip seperti itu tertangani backfill.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Regresi + test Fase 2 | `testDebugUnitTest` | Semua lulus | `[x]` |
| Build | `assembleDebug` | Sukses | `[x]` |
| Lint | `lintDebug` | 0 error, 1 warning `OldTargetApi` | `[x]` |
| Boundary / failure | N/A — tiket audit dan dokumentasi; kasus batas dan gagal sudah dites di TICKET-012–015 | — | N/A |
| Manual di HP | Checklist §6a | Semua langkah sesuai | `[ ]` manual (user, setelah semua fase selesai) |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`

Hasil (2026-10-03): `BUILD SUCCESSFUL` (dijalankan dengan `--no-build-cache` karena entri cache lokal `kspDebugKotlin` rusak); 109 test, 0 gagal; lint 0 error, 1 warning (`OldTargetApi`). Run lint pertama memunculkan warning kedua `LogNotTimber` (Timber hanya datang transitif dari MapLibre) pada log kegagalan backfill; diperbaiki dengan `@SuppressLint("LogNotTimber")` pada `AppContainer.logBackfillFailure`.

---

## 6a. Checklist Uji Manual Fase 2 (untuk user)

Checklist ini dijalankan user sekali bersama checklist fase lain setelah semua fase selesai, bukan di akhir fase ini (ai-context Rule 8, PRD v2.3).

Instal: `./gradlew installDebug` dengan HP terhubung (USB debugging aktif). Bila HP masih berisi build lama dengan data trip, instal di atasnya (jangan uninstal) agar migrasi Room teruji dengan data nyata.

| # | Langkah | Hasil yang diharapkan | Tiket |
|---|---|---|---|
| 1 | Sebelum instal, catat jumlah trip di History; instal build terbaru di atas build lama lalu buka app | Semua trip lama masih ada dengan statistik yang sama | 013 |
| 2 | Buka trip lama yang berisi singgahan ≥ 5 menit | Bagian "Tempat singgah" muncul dengan jam datang–pergi dan durasi (hasil backfill) | 014, 015 |
| 3 | Start Tracking, diam di satu tempat ≥ 6 menit, bergerak > 200 m, lalu Stop dari Home | Trip detail: 1 item "Tempat singgah · 6 menit" (kurang lebih), marker ungu berhalo di peta | 014, 015 |
| 4 | Ulangi no. 3 tetapi diam hanya ± 3 menit, Stop dari notification | Tidak ada bagian "Tempat singgah" | 014, 015 |
| 5 | Start, diam ≥ 6 menit, force stop app, buka lagi → Akhiri Trip | Trip selesai dan punya visit | 014 |
| 6 | Mode pesawat (GPS aktif), ulangi no. 3 | Visit tetap tercatat dan daftar tampil; peta boleh kosong | 012–015 |
| 7 | Di trip dengan visit: gulir ke atas sampai peta tidak terlihat, tekan item visit; geser peta, tekan item yang sama lagi | Layar bergulir hingga peta terlihat dan kamera beranimasi ke marker visit setiap kali ditekan | 015 |
| 8 | Bandingkan marker | Visit = ungu dengan halo, di bawah titik start (hijau) dan finish (merah) | 015 |
| 9 | Buka trip yang sedang aktif | Tidak ada bagian "Tempat singgah" | 015 |
| 10 | Hapus trip yang punya visit | Trip hilang dari History; tidak ada error | 014 |
| 11 | Buka History | Tampilan sama seperti Fase 1 | 015 |

Bila ada langkah yang gagal, catat di backlog dengan merek/versi HP.

---

## 7. Out of Scope

1. Fitur Fase 3 (nama tempat, tab Tempat).
2. Perubahan kode di luar perbaikan celah audit.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (uji manual di HP dijalankan user setelah semua fase selesai).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
