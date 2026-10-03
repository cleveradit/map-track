# Implementation Plan: TICKET-010 (Recovery Trip Terputus)

**Ticket:** `TICKET-010`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-009`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Definisi terputus (PRD §32–§33) | Trip `active` di DB **dan** `TrackingStateHolder` bukan `Active` untuk trip yang sama |
| Celah saat Start | `TrackingController.start()` men-set `TrackingState.Active(tripId, startedAt, null)` segera setelah `launcher.start()` berhasil, sebelum service berjalan; di-reset ke `Idle` bila gagal |
| Dialog | Di Home, tidak bisa ditutup tanpa aksi: teks PRD §33, jam mulai, jam data terakhir (`—` bila tidak ada titik), tombol `Akhiri Trip` |
| Akhiri trip terputus | `ended_at` = `recorded_at` titik terakhir, atau `started_at` bila tidak ada titik; statistik dihitung; status `completed` |
| Stop tanpa service | `TrackingController.stop()` saat service tidak berjalan memakai aturan yang sama (bukan waktu sekarang) |
| Status di Home | Trip terputus ditampilkan `Terputus` dengan tombol `Akhiri Trip`, bukan `● Aktif` + durasi berjalan |
| Melanjutkan trip | Tidak (Fase 5) |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Bila proses aplikasi dihentikan saat tracking, trip yang belum selesai terdeteksi saat aplikasi dibuka kembali dan dapat diakhiri dengan statistik yang benar, tanpa menghasilkan trip yang corrupt atau waktu selesai yang terlalu panjang.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripRecorder.kt`
   - `suspend fun finishInterrupted(tripId: String): Trip`
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt`
   - `start()` men-set state `Active` setelah launcher sukses; `stop()` non-service memanggil `finishInterrupted`; tambah `suspend fun endInterruptedTrip(tripId: String)`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/InterruptedTrip.kt`
   - `data class InterruptedTrip(tripId: String, startedAt: Long, lastPointAt: Long?)`
   - `fun interruptedTripOf(activeTrip: Trip?, trackingState: TrackingState): Trip?`
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeViewModel.kt`, `HomeUiState.kt`
   - `HomeUiState.interruptedTrip: InterruptedTrip?`, `fun endInterruptedTrip()`.
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt`
   - Dialog recovery dan status `Terputus`.

---

## 4. Scope of Changes

### A. Domain & controller

1. `finishInterrupted`, perubahan `TrackingController`.

### B. Home

1. Deteksi, state, dialog, status.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Akhiri terputus dengan titik | Titik terakhir 08:01 | `endedAt` = 08:01, statistik terisi, `completed` | `[x]` |
| Boundary: tanpa titik | Trip tanpa titik | `endedAt = startedAt`, jarak 0 | `[x]` |
| Deteksi terputus | Trip aktif, state `Idle` | Trip dikembalikan | `[x]` |
| Boundary: service berjalan | Trip aktif, state `Active` trip sama | `null` | `[x]` |
| Boundary: state trip lain | State `Active` trip lain | Trip dikembalikan | `[x]` |
| Celah Start | `start()` sukses | State langsung `Active(tripId)` | `[x]` |
| Failure: service gagal | Launcher melempar exception | State `Idle`, trip diselesaikan | `[x]` |
| Stop tanpa service | Trip aktif + titik, state `Idle` | `endedAt` = titik terakhir | `[x]` |
| Manual: proses dibunuh | Start, rekam beberapa menit, `adb shell am kill com.radityodwiki.maptrack` (atau force stop), buka lagi | Dialog muncul dengan jam mulai & data terakhir; Akhiri Trip → trip selesai di History | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 73/73 lulus (baru: `InterruptedTripTest` 4, `TripRecorderTest.finishInterrupted*` 2; `TrackingControllerTest` diperbarui), `assembleDebug` sukses, lint 0 error. Gotcha dicatat sebagai DEC-001. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Melanjutkan trip terputus (Fase 5).
2. Penanda "terputus" di History / Trip Detail.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
