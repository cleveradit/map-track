# Implementation Plan: TICKET-007 (Trip Detail: Ringkasan & Grafik Kecepatan)

**Ticket:** `TICKET-007`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-006`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Navigasi | Route `trip/{tripId}` dari item History; tombol back di top bar |
| Ringkasan (PRD §19) | Tanggal, jam mulai, jam selesai, durasi, jarak, kecepatan rata-rata, kecepatan maksimum, jumlah titik |
| Trip aktif | Jam selesai dan statistik akhir tampil `—`, label `Sedang berjalan`; jumlah titik dan grafik tetap diperbarui live |
| Grafik kecepatan (PRD §21) | Compose Canvas, sumbu X = waktu sejak mulai, sumbu Y = km/h dari 0 sampai kelipatan 10 di atas maksimum; titik `speed = NULL` dilewati |
| Jumlah sampel grafik | Maksimal 500 sampel (diambil merata) agar ringan untuk trip panjang |
| Data kecepatan kurang | < 2 sampel → teks `Belum ada data kecepatan.` |
| Peta rute | Placeholder; route/polyline di TICKET-009 |
| Trip tidak ditemukan | Teks `Perjalanan tidak ditemukan.` |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat membuka sebuah trip dari History dan melihat ringkasan statistik serta grafik perubahan kecepatan selama perjalanan.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt`
   - Route `Routes.TRIP_DETAIL = "trip/{tripId}"`, `fun tripDetailRoute(tripId: String)`; History `onTripClick` menavigasi ke route ini.
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripSummary.kt`
   - `data class TripSummary(date, startTime, endTime, duration, distance, averageSpeed, maxSpeed: String, isActive: Boolean)`
   - `fun Trip.toSummary(zone: ZoneId = ZoneId.systemDefault()): TripSummary`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/SpeedSeries.kt`
   - `data class SpeedSample(offsetMs: Long, kmh: Double)`
   - `fun speedSeries(points: List<LocationPoint>, startedAt: Long, maxSamples: Int = 500): List<SpeedSample>`
   - `fun chartMaxKmh(samples: List<SpeedSample>): Double`
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt`
   - `val uiState: StateFlow<TripDetailUiState>` (`Loading`, `NotFound`, `Loaded(summary, pointCount, samples)`), `Factory` dengan `SavedStateHandle`.
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailScreen.kt`, `SpeedChart.kt`
   - Layar detail dan grafik Canvas.

---

## 4. Scope of Changes

### A. Navigasi

1. Route detail dan sambungan dari History.

### B. Trip detail

1. Ringkasan, grafik kecepatan, placeholder peta.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Ringkasan selesai | 07:32–08:18, 21 700 m, avg 7.86 m/s, max 20.3 m/s | `07:32`, `08:18`, `46 menit`, `21.7 km`, `28 km/h`, `73 km/h` | `[x]` |
| Boundary: ringkasan aktif | Trip `active` | Jam selesai, durasi, jarak, avg, max = `—`; `isActive` | `[x]` |
| Boundary: max null | `maxSpeedMps = null` | Max `—` | `[x]` |
| Seri kecepatan | 3 titik, 1 tanpa speed | 2 sampel, offset dari `startedAt`, km/h = m/s × 3.6 | `[x]` |
| Boundary: downsample | 2 000 titik, max 500 | ≤ 500 sampel, sampel pertama dan terakhir dipertahankan | `[x]` |
| Skala Y | max 73 km/h; tanpa sampel | 80; 10 | `[x]` |
| Failure: trip tidak ada | ID tidak dikenal | `NotFound` | `[x]` |
| Manual: buka detail | Tap item History | Ringkasan sesuai daftar, grafik tampil | `[ ]` manual |
| Manual: back | Tombol back | Kembali ke History | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 60/60 lulus (baru: `TripSummaryTest` 3, `SpeedSeriesTest` 3, `TripDetailViewModelTest` 1), `assembleDebug` sukses, lint 0 error. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Peta dan polyline rute (TICKET-008, 009).
2. Hapus trip dari halaman detail.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
