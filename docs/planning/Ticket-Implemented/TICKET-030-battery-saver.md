# Implementation Plan: TICKET-030 (Penghemat Baterai saat Diam)

**Ticket:** `TICKET-030`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-029`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Penghemat baterai, `AutoTripConfig` |
| Masuk mode diam | Fix berturut-turut tetap dalam `STATIONARY_RADIUS_METERS` (100 m) dari fix pertama periode diam, tanpa kecepatan ≥ `STATIONARY_SPEED_MPS` (0,5 m/s), selama ≥ `STATIONARY_DETECT_MS` (2 menit, inklusif) |
| Mode diam | Request lokasi `LocationRequestSpec(STATIONARY_INTERVAL_MS = 30 s, highAccuracy = false)` → `PRIORITY_BALANCED_POWER_ACCURACY` |
| Keluar mode diam | Fix keluar radius dari titik awal periode diam, atau kecepatan ≥ 0,5 m/s → kembali ke interval trip (snapshot Settings) dan `HIGH_ACCURACY` |
| Fix kurang akurat | Fix dengan accuracy > radius tidak dipakai detektor (tidak dapat membedakan diam/gerak); tetap diteruskan ke filter dan penyimpanan seperti biasa |
| Kecepatan kosong | Hanya aturan radius yang berlaku |
| Implementasi | `StationaryDetector` murni (JVM) di paket `location`; service memakai `MutableStateFlow<LocationRequestSpec>` + `flatMapLatest` sehingga request diganti tanpa menghentikan trip |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Saat pengguna diam lama selama tracking (misalnya singgah di kantor), GPS diminta lebih jarang dengan prioritas hemat daya, lalu kembali normal begitu pengguna bergerak.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/location/StationaryDetector.kt`
   - `class StationaryDetector { val isStationary: Boolean; fun onFix(fix: GpsFix): Boolean }`
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt`
   - Request lokasi dinamis berdasarkan `StationaryDetector`.
3. Test: `location/StationaryDetectorTest.kt` (baru).

---

## 4. Scope of Changes

### A. Detektor & service

1. Detektor murni, request dinamis.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Masuk mode diam | Fix tiap 5 s di titik sama, kecepatan 0,1 | Setelah 115 s belum; pada 120 s `isStationary = true` (boundary) | `[x]` |
| Keluar karena posisi | Mode diam, fix 150 m dari titik awal | `isStationary = false` | `[x]` |
| Keluar karena kecepatan | Mode diam, fix di tempat dengan kecepatan 0,5 | `isStationary = false` | `[x]` |
| Bergerak tidak masuk | Fix tiap 5 s maju 20 m, kecepatan 4 m/s | Tidak pernah diam | `[x]` |
| Fix tidak akurat | Fix accuracy 150 m di tengah periode diam | Diabaikan; periode diam tetap berjalan | `[x]` |
| Kecepatan kosong | Fix diam tanpa kecepatan selama 2 menit | Masuk mode diam (aturan radius) | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 194 test, 0 gagal (`StationaryDetectorTest` 6). Pergantian request di service diuji manual (checklist TICKET-032).

---

## 7. Out of Scope

1. Penghenti otomatis trip otomatis (TICKET-031).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
