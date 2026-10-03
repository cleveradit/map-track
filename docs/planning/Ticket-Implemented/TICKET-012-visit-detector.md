# Implementation Plan: TICKET-012 (Algoritma Deteksi Visit)

**Ticket:** `TICKET-012`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-011`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 2 — Algoritma, Nilai default, Aturan kasus tepi |
| Bentuk implementasi | Fungsi murni di JVM (`object VisitDetector`), tanpa Android/Room, agar dapat dites unit |
| Parameter | `object PlaceDetectionConfig` (Rule 4): radius 100 m, min. 5 menit, merge gap 5 menit, merge distance 100 m, `DETECTION_VERSION = 1` |
| Urutan langkah | Cluster → filter durasi (≥ `MIN_VISIT_DURATION_MS`, inklusif) → gabung visit berurutan. Penggabungan **setelah** filter durasi, persis urutan PRD; dua cluster pendek (< 5 menit) tidak digabung menjadi visit agar perjalanan pelan tidak menjadi visit palsu |
| Pusat visit hasil gabung | Rata-rata seluruh titik kedua visit (bukan rata-rata kedua pusat); titik melenceng di antara keduanya tidak ikut |
| Gabung berantai | Diproses berurutan; hasil gabung dapat digabung lagi dengan visit berikutnya |
| Jarak | `GeoDistance.meters` (haversine, PRD §17) |
| Persetujuan | Instruksi loop Fase 2 dari user (tiket langsung `READY`) |

---

## 2. Objective

Menyediakan logika deteksi tempat singgah yang menerima location point satu trip dan menghasilkan daftar visit (datang, pergi, pusat, jumlah titik) sesuai PRD §38 Fase 2. Logika ini menjadi dasar penyimpanan visit (TICKET-013/014) dan tampilan Trip Detail (TICKET-015).

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceDetectionConfig.kt`
   - `object PlaceDetectionConfig { VISIT_RADIUS_METERS = 100.0; MIN_VISIT_DURATION_MS = 300_000L; MERGE_GAP_MS = 300_000L; MERGE_DISTANCE_METERS = 100.0; DETECTION_VERSION = 1 }`
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/Visit.kt`
   - `data class Visit(tripId: String, arrivedAt: Long, departedAt: Long, centerLatitude: Double, centerLongitude: Double, pointCount: Int)` dengan `val durationMs: Long get() = departedAt - arrivedAt`
3. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/VisitDetector.kt`
   - `object VisitDetector { fun detect(points: List<LocationPoint>): List<Visit> }`
   - Input urut `recordedAt` (sama dengan `TripStatisticsCalculator`); output urut `arrivedAt`; `tripId` diambil dari titik.
4. File: `app/src/test/java/com/radityodwiki/maptrack/domain/usecase/VisitDetectorTest.kt`
   - Test untuk seluruh baris Acceptance Test Matrix.

---

## 4. Scope of Changes

### A. Domain

1. `PlaceDetectionConfig`, `Visit`, `VisitDetector`.

### B. Test

1. `VisitDetectorTest` memakai `testPoint` + `DEG_PER_METER` dari `TestPoints.kt`.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Diam ≥ 5 menit | 61 titik tiap 5 s di satu lokasi (± 10 m) | Tepat 1 visit, `pointCount` 61, durasi 5 menit | `[x]` |
| Boundary: tepat 5 menit | Titik pertama 0, terakhir 300 000 ms | 1 visit (inklusif) | `[x]` |
| Boundary: < 5 menit | Titik terakhir 299 000 ms | Tidak ada visit | `[x]` |
| Satu titik melenceng | Diam 10 menit, 1 titik 300 m jauhnya, diam 10 menit di tempat yang sama | Tepat 1 visit dari awal sampai akhir, titik melenceng tidak dihitung | `[x]` |
| Bergerak lalu diam | Bergerak 1 km lalu diam 6 menit | 1 visit, `arrivedAt` = titik pertama di tempat diam | `[x]` |
| Dua tempat berbeda | Diam 6 menit di A, pindah 1 km, diam 6 menit di B | 2 visit urut waktu | `[x]` |
| Jeda gabung terlalu lama | Diam di A, pergi 10 menit, kembali ke A | 2 visit (jeda > `MERGE_GAP_MS`) | `[x]` |
| GPS hilang saat singgah | Titik 0 dan 20 menit di lokasi sama, tanpa titik di antaranya | 1 visit 20 menit | `[x]` |
| Trip diawali & diakhiri diam | Diam 6 menit, bergerak, diam 6 menit | `arrivedAt` = titik pertama trip, `departedAt` = titik terakhir trip | `[x]` |
| Bergerak terus | Titik tiap 5 s berjarak 50 m (36 km/h) | Tidak ada visit | `[x]` |
| Failure: < 2 titik | Daftar kosong dan 1 titik | Daftar kosong, tanpa exception | `[x]` |
| Pusat visit | Titik di dua posisi simetris | Pusat = rata-rata lat/lng | `[x]` |

---

## 6. Verification Commands

Hasil: `VisitDetectorTest` 13/13 lulus (termasuk kasus tambahan: kembali dalam `MERGE_GAP_MS` digabung, boundary inklusif).


1. `./gradlew testDebugUnitTest`

Expected:

1. Semua test lulus, termasuk `VisitDetectorTest`.

---

## 7. Out of Scope

1. Penyimpanan visit di Room (TICKET-013/014).
2. Tampilan visit (TICKET-015).
3. Nama tempat (Fase 3), visit di luar tracking (Fase 5).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
