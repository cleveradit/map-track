# Implementation Plan: TICKET-003 (Filter GPS, Statistik Trip, Formatter)

**Ticket:** `TICKET-003`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-002`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Aturan filter | PRD §12: koordinat tidak valid (termasuk NaN), accuracy > `TrackingConfig.MAX_ACCURACY_METERS`, `recordedAt` ≤ titik sebelumnya, kecepatan implisit > `TrackingConfig.MAX_JUMP_SPEED_MPS` |
| Pembanding filter | Titik **tersimpan** terakhir dari trip yang sama |
| Rumus jarak | Haversine murni Kotlin (radius bumi 6 371 008.8 m), bukan `Location.distanceBetween`, agar dapat dites di JVM. Selisih terhadap WGS84 < 0,5% — PRD §17 diperbarui |
| Statistik | PRD §16: durasi = `endedAt − startedAt`; jarak = jumlah haversine titik berurutan; rata-rata = jarak ÷ durasi (0 bila durasi 0); maks = speed terbesar, `null` bila tidak ada speed |
| Format kecepatan | PRD §14: `— km/h` bila tidak ada fix, speed null, atau fix lebih tua dari `STALE_FIX_MS`; `0 km/h` bila < `STATIONARY_SPEED_KMH`; selain itu dibulatkan |
| Format jarak | < 1000 m → `850 m`; ≥ 1000 m → `21.7 km` (1 desimal, titik desimal) |
| Format durasi | < 1 jam → `46 menit`; ≥ 1 jam → `1 jam 5 menit`; < 1 menit → `0 menit` |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Menyediakan logika murni untuk menentukan titik GPS mana yang disimpan, menghitung statistik trip saat selesai, dan memformat angka untuk UI, sehingga tiket service dan UI berikutnya tinggal memakainya.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/GeoDistance.kt`
   - `object GeoDistance { fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double }`
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/LocationFilter.kt`
   - `enum class RejectReason { INVALID_COORDINATES, POOR_ACCURACY, OUT_OF_ORDER, GPS_JUMP }`
   - `object LocationFilter { fun evaluate(candidate: LocationPoint, previous: LocationPoint?): RejectReason? }` — `null` berarti diterima.
3. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripStatisticsCalculator.kt`
   - `data class TripStats(distanceMeters: Double, durationMs: Long, averageSpeedMps: Double, maxSpeedMps: Double?)`
   - `object TripStatisticsCalculator { fun calculate(points: List<LocationPoint>, startedAt: Long, endedAt: Long): TripStats }`
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt`
   - `fun formatCurrentSpeed(speedMps: Float?, fixTime: Long?, now: Long): String`
   - `fun formatSpeedKmh(speedMps: Double?): String`
   - `fun formatDistance(meters: Double): String`
   - `fun formatDuration(durationMs: Long): String`
5. File: `docs/initiate-file/prd-map-track.md`
   - Change: §17 menyebut haversine; tambah baris riwayat revisi v2.1.

---

## 4. Scope of Changes

### A. Domain

1. `GeoDistance`, `LocationFilter`, `TripStatisticsCalculator`.

### B. UI format

1. `Formatters.kt`.

### C. Test

1. Unit test JVM murni untuk keempat file.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Jarak dikenal | 1° lintang di ekuator | ±111 195 m (toleransi 1 m) | `[x]` |
| Titik pertama valid | `previous = null`, accuracy 5 m | Diterima | `[x]` |
| Failure: koordinat | lat 91 / lon NaN | `INVALID_COORDINATES` | `[x]` |
| Boundary: accuracy | 50 m diterima, 50.1 m ditolak | Sesuai | `[x]` |
| Failure: urutan waktu | `recordedAt` sama dengan sebelumnya | `OUT_OF_ORDER` | `[x]` |
| Failure: GPS jump | 1 km dalam 5 detik (200 m/s) | `GPS_JUMP` | `[x]` |
| Boundary: jump setelah GPS hilang | 1 km dalam 10 menit | Diterima | `[x]` |
| Statistik | 3 titik 100 m + 100 m, 100 detik | jarak ≈ 200 m, rata-rata ≈ 2 m/s, maks = speed terbesar | `[x]` |
| Boundary: < 2 titik | 0 atau 1 titik | jarak 0, rata-rata 0 | `[x]` |
| Boundary: tanpa speed | semua speed null | `maxSpeedMps = null` | `[x]` |
| Format kecepatan | 12.5 m/s segar | `45 km/h` | `[x]` |
| Boundary: diam / basi / null | 0.2 m/s; fix 16 s lalu; speed null | `0 km/h`; `— km/h`; `— km/h` | `[x]` |
| Format jarak & durasi | 850 m, 21 700 m, 46 menit, 65 menit | `850 m`, `21.7 km`, `46 menit`, `1 jam 5 menit` | `[x]` |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 27/27 lulus (18 test baru: `GeoDistanceTest` 2, `LocationFilterTest` 7, `TripStatisticsCalculatorTest` 5, `FormattersTest` 4), `assembleDebug` sukses, lint 0 error.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Pemakaian filter dan kalkulator di service (TICKET-005).
2. Peredam jitter saat diam (Fase 5).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
