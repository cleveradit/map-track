# Implementation Plan: TICKET-014 (Integrasi Visit di finishTrip + Backfill)

**Ticket:** `TICKET-014`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-012`, `TICKET-013`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 2 — Keputusan, Aturan kasus tepi, Skema data |
| Kapan visit dihitung | Di `TripRepository.finishTrip`, dalam transaksi yang sama dengan statistik: hapus visit trip → insert hasil `VisitDetector` → `setVisitDetectionVersion(DETECTION_VERSION)`. Semua jalur selesai trip (Stop, Stop dari notification, Akhiri Trip terputus, gagal start service) melewati `finishTrip` |
| `updated_at` | Diisi `now()` oleh penyelesaian trip seperti Fase 1; penulisan versi visit dan hitung ulang tidak mengubahnya |
| Hitung ulang | `recomputeVisits(tripId)` dalam satu transaksi; dilewati (return `false`) bila trip sudah dihapus, belum `completed`, atau versinya sudah ≥ `DETECTION_VERSION` |
| Backfill | Use case `VisitBackfill`, berjalan sebagai coroutine di application scope milik `AppContainer` (`SupervisorJob() + Dispatchers.Default`), diluncurkan dari `MapTrackApplication.onCreate`. Bukan WorkManager: WorkManager baru masuk stack di Fase 7 |
| Kegagalan backfill | Gagal di satu trip dicatat (`Log.w`) dan tidak menghentikan trip lain; `CancellationException` tetap dilempar ulang. Trip yang gagal tetap versi lama sehingga dicoba lagi saat aplikasi dibuka berikutnya |
| Abstraksi untuk test | Interface `VisitRecomputation` (diimplementasikan `TripRepository`) agar kegagalan per trip dapat dites dengan fake, mengikuti pola `TrackingServiceLauncher` |
| Persetujuan | Instruksi Fase 2 dari user di chat (tiket langsung `READY`) |

---

## 2. Objective

Setiap trip yang selesai langsung memiliki visit tersimpan, dan trip lama dari Fase 1 mendapat visit otomatis saat aplikasi dibuka tanpa menghapus data apa pun. Visit tetap data turunan yang dapat dihitung ulang dari location point.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/VisitBackfill.kt`
   - `interface VisitRecomputation { suspend fun tripIdsNeedingVisits(): List<String>; suspend fun recomputeVisits(tripId: String): Boolean }`
   - `class VisitBackfill(store: VisitRecomputation, onFailure: (tripId: String, error: Exception) -> Unit = { _, _ -> })`
   - `suspend fun run(): Int` — jumlah trip yang visit-nya dihitung ulang.
2. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt`
   - `class TripRepository(...) : VisitRecomputation`
   - `finishTrip`: setelah `tripDao.update(completed)`, panggil helper privat `replaceVisits(tripId, points)` di transaksi yang sama.
   - `override suspend fun recomputeVisits(tripId: String): Boolean` (transaksi).
   - `override suspend fun tripIdsNeedingVisits(): List<String>` = `tripDao.getCompletedIdsWithVisitVersionBelow(DETECTION_VERSION)`.
   - `fun observeVisits(tripId: String): Flow<List<Visit>>`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`
   - `val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)`
   - `val visitBackfill: VisitBackfill by lazy { ... }` dengan `onFailure` → `Log.w`.
   - `fun startVisitBackfill(): Job`
4. File: `app/src/main/java/com/radityodwiki/maptrack/MapTrackApplication.kt`
   - `onCreate` memanggil `container.startVisitBackfill()`.
5. Test:
   - `app/src/test/java/com/radityodwiki/maptrack/data/repository/TripRepositoryTest.kt`
   - `app/src/test/java/com/radityodwiki/maptrack/domain/usecase/TripRecorderTest.kt`
   - `app/src/test/java/com/radityodwiki/maptrack/location/TrackingControllerTest.kt`
   - `app/src/test/java/com/radityodwiki/maptrack/domain/usecase/VisitBackfillTest.kt` (baru)

---

## 4. Scope of Changes

### A. Repository

1. `replaceVisits`: `visitDao.deleteForTrip` → `visitDao.insertAll(VisitDetector.detect(points))` → `tripDao.setVisitDetectionVersion`.
2. `finishTrip` memakai titik yang sama untuk statistik dan visit.
3. `recomputeVisits`, `tripIdsNeedingVisits`, `observeVisits`.

### B. Backfill

1. `VisitBackfill.run()` memproses trip satu per satu, menangkap `Exception` per trip kecuali `CancellationException`.
2. Wiring di `AppContainer` dan `MapTrackApplication`.

### C. Test

1. Jalur Stop (service → `TripRecorder.finish`; Stop dari notification memakai intent `ACTION_STOP` yang sama), Akhiri Trip terputus (`TrackingController.endInterruptedTrip` dan `stop()` tanpa service).
2. Kasus < 2 titik, cascade hapus, backfill.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Stop menghasilkan visit | Trip aktif dengan titik diam 10 menit, `TripRecorder.finish` (jalur Stop & Stop dari notification) | 1 visit; `visit_detection_version = DETECTION_VERSION`; `updated_at` = waktu selesai | `[x]` |
| Akhiri Trip terputus | Trip aktif tanpa service, `endInterruptedTrip` dan `stop()` | 1 visit, versi terbaru | `[x]` |
| `finishTrip` di repository | Titik diam ≥ 5 menit | Visit + statistik tersimpan; panggilan kedua tidak menggandakan visit | `[x]` |
| Boundary/failure: < 2 titik | Trip 0 dan 1 titik diselesaikan | Tanpa visit, tanpa error, versi tetap ditulis | `[x]` |
| Hapus trip | Trip completed dengan visit dihapus | Visit ikut terhapus | `[x]` |
| `recomputeVisits` | Trip completed versi 0 / aktif / tidak ada / versi terbaru | Hanya trip versi 0 dihitung (`true`); lainnya `false`; `updated_at` dan titik tidak berubah | `[x]` |
| Backfill trip lama | Trip completed versi 0 dengan titik diam | Visit terbentuk; titik dan statistik utuh; run kedua mengembalikan 0 | `[x]` |
| Failure: satu trip gagal | Fake store: trip kedua melempar exception | Trip lain tetap diproses; `onFailure` dipanggil sekali | `[x]` |
| Failure: dibatalkan | Fake store melempar `CancellationException` | Exception dilempar ulang, trip berikutnya tidak diproses | `[x]` |
| `observeVisits` | Trip selesai dengan visit | Flow mengirim visit urut `arrived_at` | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Expected:

1. Semua test lulus.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 104 test, 0 gagal (`TripRepositoryTest` 15, `TripRecorderTest` 11, `TrackingControllerTest` 9, `VisitBackfillTest` 3).

---

## 7. Out of Scope

1. UI Trip Detail (TICKET-015).
2. Visit pada trip aktif, nama tempat (Fase 3).
3. `completeTrip` (hanya dipakai test Fase 1) tidak menghitung visit; trip seperti itu tertangani backfill.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
