# Implementation Plan: TICKET-034 (Halaman Uji Akselerasi)

**Ticket:** `TICKET-034`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-033`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Uji akselerasi (v2.6) |
| Akses | Tombol "Uji akselerasi" di Home di bawah bagian Tracking → route `acceleration` |
| GPS | `LocationRequestSpec(AccelerationConfig.GPS_INTERVAL_MS, highAccuracy = true)` dikoleksi selama layar terlihat (`WhileSubscribed(5_000)`), tanpa service; layar dijaga menyala (`keepScreenOn`) |
| Prasyarat | Izin lokasi presisi dan Location aktif; bila tidak, pesan dan tidak mengukur |
| Tampilan | Peringatan keselamatan; status fase ("Menunggu sinyal GPS akurat…", "Berhenti total dulu…", "Siap — mulai berakselerasi", "Mengukur…", "Selesai", "Gagal: sinyal GPS terputus"); kecepatan besar (satuan Settings), waktu (detik, 2 desimal), jarak (m); tabel 0–100 m … 0–500 m (waktu + kecepatan saat lewat) dan 0–100 km/jam ("—" bila belum tercapai); tombol Berhenti (saat mengukur) dan Ulangi (setelah selesai/gagal) |
| Simpan | Saat fase menjadi `FINISHED` dan `toRun()` tidak null → `AccelerationRepository.save` sekali |
| Riwayat | Daftar hasil terbaru dulu di bawah tabel: tanggal & jam, ringkasan semua target, tombol hapus |
| Format waktu | `formatSeconds(ms)` → `"6.94 s"` (Locale.US, konsisten dengan formatter jarak) |
| Persetujuan | Instruksi user di chat: "tambahkan sekarang, buat tiket langsung status ready" |

---

## 2. Objective

Pengguna dapat mengukur waktu akselerasi dari layar khusus dengan umpan balik langsung, melihat hasil per target, dan menyimpan serta menghapus riwayat hasil.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/acceleration/AccelerationViewModel.kt`
   - `AccelerationUiState(permissionOk: Boolean, locationEnabled: Boolean, measurement: AccelerationState, distanceUnit: DistanceUnit, runs: List<AccelerationRun>)`
   - `fun stop()`, `fun restart()`, `fun deleteRun(id: Long)`; konstruktor `(locationSource, repository, settings: Flow<AppSettings> = flowOf(DEFAULT))`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/acceleration/AccelerationScreen.kt`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt` — `formatSeconds(ms: Long?): String`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt` — tombol; `MapTrackNavHost.kt` — `Routes.ACCELERATION`.
5. File: `app/src/main/res/values/strings.xml` — teks halaman.
6. Test: `ui/acceleration/AccelerationViewModelTest.kt`, `FormattersTest`.

---

## 4. Scope of Changes

### A. UI

1. ViewModel, layar, tombol Home, route.

### B. Dokumen

1. Feature doc `acceleration.md`, data-model (DB versi 5), architecture, index.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Run lengkap | Fake fix: diam 2 s lalu 4 m/s² | Fase `FINISHED`, 1 hasil tersimpan dengan 0–100 km/jam ±0,1 s dari 6,94 s | `[x]` |
| Failure: tanpa izin | Izin perkiraan | `permissionOk = false`, tidak ada fix diminta | `[x]` |
| Failure: GPS terputus | Jeda 4 s saat mengukur | `INVALID`, tidak tersimpan | `[x]` |
| Ulangi | `restart()` setelah selesai | Fase kembali `WAITING_GPS`; hasil lama tetap di riwayat | `[x]` |
| Hapus | `deleteRun(id)` | Riwayat berkurang satu | `[x]` |
| Boundary format | `formatSeconds(6944)` / `null` | `"6.94 s"` / `"—"` | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug lintDebug` → `BUILD SUCCESSFUL`; 234 test, 0 gagal (`AccelerationViewModelTest` 4, `FormattersTest` 9); lint 0 error, 1 warning (`OldTargetApi`). Uji emulator: alur Berhenti total → Siap → Mengukur → Selesai berjalan, semua target terisi dan tersimpan di riwayat. Nilai emulator lebih lambat dari ideal (0–100 km/jam 7,59 s vs 6,94 s) karena latensi `adb emu` membuat jeda fix ±1,3 s; ketepatan rumus dibuktikan unit test.

---

## 6a. Checklist Uji Manual Uji Akselerasi (untuk user)

Dijalankan bersama checklist fase lain setelah semua fase selesai (ai-context Rule 8). Lakukan hanya di tempat aman dan legal, HP terpasang di dudukan.

| # | Langkah | Hasil yang diharapkan |
|---|---|---|
| 1 | Home → Uji akselerasi, di luar ruangan, kendaraan diam | Status berubah ke "Berhenti total dulu…" lalu "Siap — mulai berakselerasi"; layar tidak mati |
| 2 | Akselerasi penuh sampai > 100 km/jam dan > 500 m | Waktu 0–100 … 0–500 m dan 0–100 km/jam terisi; bandingkan dengan spesifikasi kendaraan (selisih wajar ±0,5 detik) |
| 3 | Berhenti sebelum 300 m | Run selesai; target yang tidak tercapai "—"; hasil masuk Riwayat |
| 4 | Ulangi, lalu masuk terowongan/parkiran tertutup saat mengukur | "Gagal: sinyal GPS terputus"; tidak masuk Riwayat |
| 5 | Hapus satu hasil di Riwayat | Hasil hilang |
| 6 | Tinggalkan halaman | Ikon lokasi di status bar hilang ±5 detik kemudian (bila tidak tracking) |

## 7. Out of Scope

1. Target tambahan (0–60 mph, ¼ mil), grafik akselerasi, ekspor hasil.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
