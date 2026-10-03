# Implementation Plan: TICKET-011 (Penutup Fase 1: Audit & Optimasi Dasar)

**Ticket:** `TICKET-011`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-010`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Audit | Setiap butir PRD §40 dan alur §41 dipetakan ke kode/tiket; celah yang ditemukan diperbaiki di tiket ini |
| Celah privasi | Android 12+ mengabaikan `allowBackup=false` untuk device-to-device transfer → tambah `dataExtractionRules` yang mengecualikan semua domain untuk cloud backup dan device transfer (PRD §35) |
| Ikon aplikasi | Adaptive icon vektor sederhana (pin lokasi) — menghilangkan ikon default dan lint `MissingApplicationIcon` |
| Optimasi baterai/GPS dasar (PRD §43 no. 16) | Audit langkah yang sudah ada; tidak menambah perilaku baru (peredam jitter dan optimasi lanjutan di Fase 5) |
| Permission library | Merged manifest diverifikasi tidak mengandung `ACCESS_BACKGROUND_LOCATION` |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Memastikan seluruh acceptance criteria Fase 1 terpenuhi di kode, menutup celah privasi backup/transfer, dan menyerahkan satu checklist uji manual lengkap kepada user.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/res/xml/data_extraction_rules.xml`
   - `<cloud-backup>` dan `<device-transfer>` mengecualikan domain `root`, `file`, `database`, `sharedpref`, `external`.
2. File: `app/src/main/AndroidManifest.xml`
   - `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:icon`, `android:roundIcon`.
   - Tambahan saat implementasi: `android:fullBackupContent="@xml/backup_rules"` (`res/xml/backup_rules.xml`) untuk Android 10–11, diminta lint.
3. File: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml`, `drawable/ic_launcher_foreground.xml`, `values/colors.xml`
   - Adaptive icon.
4. File: `docs/index.md`
   - Status Fase 1.

---

## 4. Audit PRD §40

| Kriteria | Implementasi | Tiket |
|---|---|---|
| Start Tracking, trip baru dibuat | `TrackingController.start()` → `TripRepository.startTrip()` | 005 |
| Lokasi direkam & tersimpan ke Room | `LocationTrackingService` → `TripRecorder.record()` | 005 |
| Tracking jalan di background | Foreground service `location`, `START_NOT_STICKY` | 005 |
| Stop tracking | Home & notification → `ACTION_STOP` | 005 |
| Titik: lat, lon, `recorded_at`, accuracy; speed/bearing/altitude bila ada | `LocationPointEntity`, `GpsFix` | 002, 004 |
| Titik yang tidak lolos filter tidak disimpan | `LocationFilter` | 003, 005 |
| Trip: start, end, durasi, jarak, avg, max | `TripRepository.finishTrip()` + `TripStatisticsCalculator` | 003, 005 |
| History: daftar, terbaru dulu, buka, hapus | `HistoryScreen`, `TripDetailScreen` | 006, 007 |
| Map: lokasi terkini, rute, start/finish berbeda | `HomeMap`, `TripRouteMap` | 008, 009 |
| Speed: terkini, max, avg, grafik | Home, `TripSummary`, `SpeedChart` | 004, 007 |
| Recovery trip terputus | Dialog Home + `finishInterrupted` | 010 |
| Offline: tracking, Room, history, statistik tetap jalan | Satu-satunya jaringan adalah tile peta | 008 |

Langkah baterai/GPS yang sudah ada: request lokasi Home berhenti 5 detik setelah Home tidak terlihat; selama tracking Home memakai fix dari service (tidak ada request kedua); `PRIORITY_HIGH_ACCURACY` hanya saat tracking atau Home terlihat; satu insert per fix yang lolos; marker Home memperbarui satu titik GeoJSON, tidak membaca database.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Rules ekstraksi data | `lintDebug` | Tidak ada `DataExtractionRules` | `[x]` |
| Ikon | `lintDebug` | Tidak ada `MissingApplicationIcon` | `[x]` |
| Permission | Merged manifest | Tanpa `ACCESS_BACKGROUND_LOCATION` | `[x]` |
| Regresi | `testDebugUnitTest` | Semua lulus | `[x]` |
| Boundary / failure | N/A — tiket konfigurasi dan audit; kasus batas dan gagal fitur sudah dites di tiket 002–010 | — | N/A |
| Manual: ikon | Launcher HP | Ikon pin biru tampil | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 73/73 lulus, `assembleDebug` sukses, lint 0 error dan 1 warning (`OldTargetApi`, disengaja). Merged manifest tanpa `ACCESS_BACKGROUND_LOCATION` (`ACCESS_NETWORK_STATE` dan `ACCESS_WIFI_STATE` berasal dari library peta).


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 6a. Checklist Uji Manual Fase 1 (gabungan, untuk user)

Instal: `./gradlew installDebug` dengan HP terhubung (USB debugging aktif).

| # | Langkah | Hasil yang diharapkan | Tiket |
|---|---|---|---|
| 1 | Buka app pertama kali | Ikon pin biru di launcher; kartu penjelasan izin, dialog izin baru muncul setelah tombol | 004, 011 |
| 2 | Tolak izin / pilih "Perkiraan" | Pesan sesuai; tab History tetap bisa dibuka | 004 |
| 3 | Izinkan "Lokasi akurat", di luar ruangan | Peta, titik biru, status GPS Aktif, kecepatan & akurasi terisi ±30 detik | 004, 008 |
| 4 | Geser/zoom peta, lalu "Ikuti posisi" | Gesture peta normal (tidak terbawa scroll); kamera kembali ke posisi | 008 |
| 5 | Pindah ke History > 5 detik | Ikon lokasi status bar hilang (bila tidak tracking) | 004 |
| 6 | Start Tracking (izinkan notifikasi) | Notification "Tracking aktif" dengan kecepatan & durasi | 005 |
| 7 | Minimize, buka app lain, kunci layar 5+ menit sambil bergerak | Durasi terus bertambah; tracking tidak berhenti | 005 |
| 8 | Tap body notification | App terbuka di Home, status Aktif | 005 |
| 9 | Stop dari notification | Notification hilang; Home kembali ke Start | 005 |
| 10 | Buka History | Trip terbaru di atas dengan jarak & durasi | 006 |
| 11 | Buka trip | Ringkasan, rute dengan titik hijau/merah, grafik kecepatan | 007, 009 |
| 12 | Hapus trip: Batal lalu Hapus | Batal tetap ada; Hapus hilang | 006 |
| 13 | Start, lalu force stop app, buka lagi | Dialog "Perjalanan sebelumnya tidak selesai…"; Akhiri Trip → trip selesai di History | 010 |
| 14 | Mode pesawat (GPS tetap aktif), Start, bergerak, Stop | Peta boleh kosong; trip tersimpan dengan statistik | 008 |
| 15 | Matikan Location saat tracking | Notification "Location service tidak aktif"; trip tetap aktif | 005 |
| 16 | Alur PRD §41 lengkap, tutup & buka ulang app | Trip sebelumnya masih ada | semua |

Catatan: beberapa vendor HP (mis. Xiaomi, Oppo, Vivo) mematikan foreground service secara agresif. Bila langkah 7 gagal, nonaktifkan optimasi baterai untuk Map Track lalu ulangi, dan catat merek/versi HP di backlog.

## 7. Out of Scope

1. Peredam jitter dan optimasi lanjutan (Fase 5).
2. targetSdk 37 (lint `OldTargetApi` dibiarkan; perlu uji perilaku runtime baru).
3. Konfigurasi rilis (signing, R8).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
