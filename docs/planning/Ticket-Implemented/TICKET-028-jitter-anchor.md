# Implementation Plan: TICKET-028 (Peredam Jitter dengan Titik Jangkar)

**Ticket:** `TICKET-028`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-027`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Peredam jitter; §17 |
| Aturan | Titik pertama = jangkar. Untuk tiap titik berikutnya: `d = haversine(jangkar, titik)`; bila `d > max(accuracy jangkar, accuracy titik)` jarak ditambah `d` dan titik itu menjadi jangkar; selain itu dilewati (jangkar tetap) |
| Tambahan (PRD v2.5) | Titik yang kecepatan GPS-nya diketahui dan < `AutoTripConfig.STATIONARY_SPEED_MPS` (0,5 m/s) dilewati: tidak menambah jarak, tidak menjadi jangkar. Alasan: simulasi menunjukkan aturan accuracy saja menambah ±200 m (drift) sampai ±1,5 km (jitter acak) untuk diam 10 menit, jauh di atas kriteria 50 m (DEC-008) |
| Titik tersimpan | Semua tetap disimpan (deteksi visit butuh titik diam); peredam hanya di perhitungan jarak |
| Trip lama | Tidak dihitung ulang: statistik hanya dihitung di `finishTrip`, dan trip yang sudah `completed` tidak pernah diselesaikan ulang |
| Jeda GPS | Tetap garis lurus antara jangkar dan titik berikutnya (§31) |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Diam lama dengan GPS yang melompat-lompat tidak lagi menambah jarak palsu, sementara gerakan nyata tetap terhitung.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/location/AutoTripConfig.kt` — semua konstanta Fase 5 (PRD `AutoTripConfig`).
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripStatisticsCalculator.kt`
   - `fun anchoredDistance(points: List<LocationPoint>): Double` (internal ke object, dapat dites); `calculate` memakai `anchoredDistance`.
3. Test: `TripStatisticsCalculatorTest`.

---

## 4. Scope of Changes

### A. Domain

1. Perhitungan jarak dengan jangkar.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Diam dengan jitter | 10 menit, titik tiap 5 s, accuracy 10–30 m, simpangan acak ≤ accuracy, kecepatan 0–0,45 m/s | Jarak < 50 m | `[x]` |
| Batas aturan accuracy | Rekaman sama tanpa kecepatan | Jarak > 50 m (mendokumentasikan keterbatasan; rekaman nyata diuji manual) | `[x]` |
| Gerak normal | 3 titik berjarak 100 m, accuracy 5 m | 200 m (tidak berubah dari sebelumnya) | `[x]` |
| Boundary: tepat sama dengan accuracy | Langkah 10 m, accuracy 10 m | Tidak ditambah (harus lebih besar) | `[x]` |
| Gerak lambat | 100 titik garis lurus berjarak 7 m, accuracy 10 m, kecepatan 1,4 m/s | Jarak ≥ 98% panjang lintasan dikurangi satu langkah | `[x]` |
| Jeda GPS | Dua titik 1 km terpisah 10 menit | 1 000 m (garis lurus) | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 181 test, 0 gagal (`TripStatisticsCalculatorTest` 10). Aturan PRD saja gagal pada test diam (1 627 m); ditambah syarat kecepatan (PRD v2.5, DEC-008).

---

## 7. Out of Scope

1. Menghitung ulang statistik trip lama.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
