# Implementation Plan: TICKET-022 (Penutup Fase 3: Audit, Verifikasi, Dokumentasi)

**Ticket:** `TICKET-022`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-017`, `TICKET-018`, `TICKET-019`, `TICKET-020`, `TICKET-021`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Audit | Setiap acceptance criteria PRD §38 Fase 3 dipetakan ke kode dan test; celah diperbaiki di tiket ini atau dicatat di backlog |
| Verifikasi | `./gradlew testDebugUnitTest assembleDebug lintDebug`; target lint 0 error, 1 warning `OldTargetApi` |
| Uji manual | Checklist Fase 3 di §6a, dijalankan user sekali setelah semua fase selesai (ai-context Rule 8); tidak memblokir Fase 4 |
| Dokumentasi | Feature doc `places.md` (baru), `trip-detail.md`, `place-detection.md`, `data-model.md`, `architecture.md`, `decision-log.md`, `docs/index.md`, backlog (Fase 4 `OPEN`) |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat (tiket langsung `READY`) |

---

## 2. Objective

Memastikan seluruh acceptance criteria Fase 3 terpenuhi dan terverifikasi, dokumentasi mencerminkan kode, dan checklist uji manual Fase 3 tersedia untuk pengujian akhir.

---

## 3. Non-Negotiable Technical Contract

1. File: `docs/features/places.md` (baru), terdaftar di `docs/features/index.md`.
2. File: `docs/features/trip-detail.md`, `docs/features/place-detection.md`, `docs/data-model.md`, `docs/architecture.md`, `docs/index.md`.
3. File: `docs/decision-log.md` — hanya entri yang memenuhi kriteria ai-context §5 Step 4.
4. File: `docs/backlog.md` — Fase 4 `OPEN`, Fase 5 menunggu Fase 4.

---

## 4. Audit PRD §38 Fase 3

| Acceptance criteria | Implementasi | Test | Tiket |
|---|---|---|---|
| Buat, ubah, hapus tempat tanpa internet | `PlaceRepository` (Room lokal); form dapat disimpan tanpa peta bila titik diketahui (visit, tempat lama, lokasi saat ini) | `PlaceRepositoryTest`, `PlaceEditorViewModelTest.pointFromVisit_savesWithoutMap`, `PlaceDetailViewModelTest.delete_keepsTripsAndVisits`; uji manual §6a no. 9 | 017, 019, 020 |
| Nama kosong / > 50 karakter ditolak; radius di luar 50–1 000 m tidak dapat dipilih (boundary) | `PlaceValidator` di repository; slider 50–1 000 kelipatan 10 + `onRadiusChange` coerce | `PlaceValidatorTest`, `PlaceRepositoryTest.createPlace_rejectsInvalidInput`, `PlaceEditorViewModelTest.nameAndRadiusBoundaries`, `blankName_isNotSaved` | 017, 019 |
| Visit tepat di dalam radius diberi nama; di luar tidak | `PlaceMatcher.match` (haversine ≤ radius, inklusif) | `PlaceMatcherTest`, `TripDetailViewModelTest.visitOutsideRadiusIsUnnamed` | 017, 021 |
| Tempat tumpang tindih → pusat terdekat | `PlaceMatcher.match` / `group` | `PlaceMatcherTest.overlappingPlaces_nearestCenterWins`, `TripDetailViewModelTest.visitNamedByMatchingPlace`, `PlaceDetailViewModelTest.overlappingVisitBelongsToNearestPlace` | 017, 018, 020, 021 |
| Ubah radius / hapus tempat langsung memperbarui nama visit di Trip Detail dan Detail Tempat | Nama dicocokkan saat ditampilkan dari Flow `observePlaces` + visit | `TripDetailViewModelTest.deletingPlaceRemovesNameButKeepsVisit`, `PlaceDetailViewModelTest.followsRadiusChange`, `PlacesViewModelTest.countsFollowRadiusChanges` | 018, 020, 021 |
| Hapus tempat tidak menghapus trip maupun visit (failure) | Tanpa FK `visits` → `places`; `deletePlace` hanya baris `places` | `PlaceRepositoryTest.deletePlace_keepsTripsAndVisits`, `PlaceDetailViewModelTest.delete_keepsTripsAndVisits` | 017, 020 |
| Detail tempat: total kunjungan & total durasi sesuai visit | `PlaceDetailViewModel` | `PlaceDetailViewModelTest.totalsAndNewestFirst`, `withoutVisits` | 020 |
| Migrasi 2 → 3 dites | `AutoMigration(2, 3)` | `MigrationTest.migrate2To3_keepsPhase2DataAndAddsEmptyPlaces` (dan 1 → 3) | 017 |

Celah yang ditemukan: tidak ada.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Regresi + test Fase 3 | `testDebugUnitTest` | Semua lulus | `[x]` |
| Build | `assembleDebug` | Sukses | `[x]` |
| Lint | `lintDebug` | 0 error, 1 warning `OldTargetApi` | `[x]` |
| Boundary / failure | N/A — tiket audit dan dokumentasi; kasus batas dan gagal sudah dites di TICKET-017–021 | — | N/A |
| Manual di HP | Checklist §6a | Semua langkah sesuai | `[ ]` manual (user, setelah semua fase selesai) |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`

---

## 6a. Checklist Uji Manual Fase 3 (untuk user)

Checklist ini dijalankan user sekali bersama checklist fase lain setelah semua fase selesai, bukan di akhir fase ini (ai-context Rule 8, PRD v2.3).

| # | Langkah | Hasil yang diharapkan | Tiket |
|---|---|---|---|
| 1 | Buka app | Navigasi bawah: Home, History, Tempat | 018 |
| 2 | Tab Tempat tanpa data | `Belum ada tempat.` dan tombol + | 018 |
| 3 | Tekan +; sebelum menggeser peta | Petunjuk "Geser peta untuk menentukan titik tempat…"; Simpan nonaktif | 019 |
| 4 | Geser peta, ubah slider radius dari ujung ke ujung | Pin tetap di tengah; lingkaran biru mengikuti peta dan radius; label 50 m – 1.0 km | 019 |
| 5 | Kosongkan nama lalu tekan Simpan; isi 51 karakter | Pesan "Nama tempat wajib diisi." / "maksimal 50 karakter"; tidak tersimpan | 019 |
| 6 | Isi nama "Rumah", Simpan | Kembali ke Tab Tempat; item Rumah dengan radius dan jumlah kunjungan | 018, 019 |
| 7 | Tambah tempat → "Pakai lokasi saat ini" (cabut izin lokasi dulu di Pengaturan) | Dialog penjelasan → dialog izin sistem; setelah diizinkan peta pindah ke posisi Anda | 019 |
| 8 | Buka trip dengan tempat singgah yang belum bernama → "Simpan sebagai tempat" | Form terbuka dengan pin di pusat visit; setelah disimpan, Trip Detail menampilkan `<nama> · <durasi>` dan aksi hilang | 019, 021 |
| 9 | Mode pesawat, ulangi no. 8 | Peta boleh kosong; tempat tetap tersimpan | 019 |
| 10 | Tab Tempat → buka tempat | Peta dengan lingkaran, Radius, Total kunjungan, Total durasi, daftar kunjungan; tap kunjungan membuka Trip Detail | 020 |
| 11 | Ubah tempat (ikon pensil): perkecil radius sampai visit di luar | Detail & Trip Detail: kunjungan hilang / nama kembali "Tempat singgah" | 019, 020, 021 |
| 12 | Hapus tempat: Batal lalu Hapus | Batal: tetap ada. Hapus: kembali ke daftar; trip tetap ada di History dengan tempat singgahnya | 020 |
| 13 | Buat dua tempat tumpang tindih di sekitar satu visit | Visit diberi nama tempat dengan pusat terdekat; hanya dihitung di tempat itu | 018, 020, 021 |
| 14 | Instal di atas build lama yang berisi data | Trip, titik, dan visit lama tetap ada (migrasi ke versi 3) | 017 |

Bila ada langkah yang gagal, catat di backlog dengan merek/versi HP.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug lintDebug` → `BUILD SUCCESSFUL`; 155 test, 0 gagal; lint 0 error, 1 warning (`OldTargetApi`). Run lint pertama memunculkan 2 warning `PluralsCandidate` pada `place_visit_summary` dan `place_name_too_long`; bahasa Indonesia tidak berbentuk jamak, sehingga diberi `tools:ignore="PluralsCandidate"`.

---

## 7. Out of Scope

1. Fitur Fase 4 (offline map, settings).
2. Perubahan kode di luar perbaikan celah audit.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (uji manual di HP dijalankan user setelah semua fase selesai).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
