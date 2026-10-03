# Implementation Plan: TICKET-029 (Lanjutkan Trip Terputus)

**Ticket:** `TICKET-029`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-028`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Melanjutkan trip terputus; §33; DEC-001 |
| Syarat | `now − (recorded_at titik terakhir ?: started_at) ≤ AutoTripConfig.RESUME_MAX_GAP_MS` (60 menit, inklusif). Di luar itu hanya Akhiri Trip |
| Tombol | Dialog trip terputus dan bagian Tracking Home menampilkan **Lanjutkan** di samping **Akhiri Trip** bila syarat terpenuhi; dievaluasi ulang tiap detik (ticker Home) |
| Alur | `TrackingController.resume(tripId)`: cek izin presisi dan Location seperti Start → cek trip masih `active` dan dalam batas → state `Active` diset **sebelum** service dijalankan (DEC-001) → `launcher.start(tripId, snapshot TrackingParams baru)`. Gagal start service → state kembali `Idle`, trip tetap terputus |
| Jeda | Tidak ada titik buatan; jarak titik terakhir → titik pertama setelah lanjut dihitung garis lurus oleh perhitungan jangkar (§31) |
| Error baru | `StartTrackingError.RESUME_EXPIRED` (pesan "Trip terputus lebih dari 60 menit dan hanya dapat diakhiri.") |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Trip yang terputus karena proses aplikasi mati dapat dilanjutkan sebagai trip yang sama bila jedanya belum lama, sehingga satu perjalanan tidak terpecah.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/InterruptedTrip.kt`
   - `fun InterruptedTrip.canResume(now: Long): Boolean`
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt`
   - `suspend fun resume(tripId: String): StartTrackingError?`; `StartTrackingError.RESUME_EXPIRED`; konstruktor + `clock: () -> Long = System::currentTimeMillis`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeViewModel.kt` — `resumeInterruptedTrip()`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt` — tombol Lanjutkan.
5. File: `app/src/main/res/values/strings.xml` — `resume_trip`, `error_resume_expired`.
6. Test: `InterruptedTripTest`, `TrackingControllerTest`.

---

## 4. Scope of Changes

### A. Logika

1. Syarat lanjut, `resume`.

### B. UI

1. Tombol dan pesan.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Boundary waktu | Titik terakhir 30 / 60 / 61 / 90 menit lalu | `canResume` true / true / false / false | `[x]` |
| Tanpa titik | `started_at` 30 menit lalu | `canResume = true` | `[x]` |
| Lanjutkan berhasil | Trip aktif terputus 30 menit, izin & Location OK | `null`; state `Active(tripId)` sebelum launcher; launcher menerima trip yang sama + snapshot settings | `[x]` |
| Failure: kedaluwarsa | Trip terputus 90 menit | `RESUME_EXPIRED`; launcher tidak dipanggil | `[x]` |
| Failure: izin / Location | Izin perkiraan / Location mati | `PERMISSION_MISSING` / `LOCATION_DISABLED` | `[x]` |
| Failure: service gagal | Launcher melempar exception | `SERVICE_START_FAILED`; state `Idle`; trip tetap `active` | `[x]` |
| Jarak setelah jeda | Titik sebelum dan sesudah lanjut berjarak 1 km | Jarak trip menyertakan 1 km garis lurus | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 188 test, 0 gagal (`TrackingControllerTest` 15, `InterruptedTripTest` 6). `canResumeTrip` ditempatkan di `location/AutoTripConfig.kt` (bukan `ui/home`) agar `TrackingController` tidak bergantung pada paket UI.

---

## 7. Out of Scope

1. Melanjutkan otomatis tanpa aksi pengguna.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
