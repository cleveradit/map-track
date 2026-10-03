# Implementation Plan: TICKET-035 (Perbaikan Penghemat Baterai: Rekaman Berhenti Setelah Diam)

**Ticket:** `TICKET-035`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-030`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber | Backlog "Bug: penghemat baterai berhenti merekam saat pengguna mulai bergerak lagi" (uji emulator 2026-10-03) |
| Masalah | Mode diam memakai `PRIORITY_BALANCED_POWER_ACCURACY`. Di emulator tidak ada fix sama sekali; di HP nyata fix Wi-Fi/seluler sering ber-accuracy > 100 m (diabaikan `StationaryDetector`, ditolak filter). Detektor tidak pernah melihat gerakan → tetap di mode diam → perjalanan berikutnya tidak terekam |
| Perbaikan | Mode diam tetap `HIGH_ACCURACY` (GPS); hanya interval yang diperpanjang ke `max(STATIONARY_INTERVAL_MS, interval trip)`. PRD direvisi ke v2.7 |
| Dampak | Hemat baterai lebih kecil daripada desain awal, tetapi data tidak hilang. Jeda deteksi gerak ≤ 30 detik; jarak selama jeda itu dihitung garis lurus seperti jeda GPS |
| Fungsi murni | `trackingRequest(intervalMs: Long, stationary: Boolean): LocationRequestSpec` agar dapat dites |
| Persetujuan | Instruksi user di chat: "perbaiki bug penghemat baterai dulu, buat tiket langsung ready" |

---

## 2. Objective

Penghemat baterai tidak lagi menghentikan rekaman: setelah singgah, perjalanan berikutnya tetap tercatat dan interval kembali normal begitu pengguna bergerak.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/location/StationaryDetector.kt`
   - `fun trackingRequest(intervalMs: Long, stationary: Boolean): LocationRequestSpec`
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt` — memakai `trackingRequest`.
3. File: `docs/initiate-file/prd-map-track.md` — §38 Fase 5 Penghemat baterai, §45 v2.7.
4. Test: `StationaryDetectorTest`.

---

## 4. Scope of Changes

### A. Kode

1. Request mode diam tetap high accuracy.

### B. Dokumen

1. PRD v2.7, decision log, feature doc tracking, backlog.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Mode normal | `trackingRequest(5_000, false)` | `(5_000, highAccuracy = true)` | `[x]` |
| Mode diam | `trackingRequest(5_000, true)` | `(30_000, highAccuracy = true)` | `[x]` |
| Boundary: interval trip ≥ 30 s | `trackingRequest(30_000, true)` | `(30_000, true)` — tidak berubah dari mode normal | `[x]` |
| Emulator: diam lalu bergerak | Diam 2,5 menit lalu 10 m/s selama 70 detik | Interval 30 s saat diam; titik kembali tersimpan saat bergerak, interval kembali 5 s; jarak > 0 | `[x]` |
| Failure (sebelum perbaikan) | Skenario yang sama dengan build lama | 0 titik saat bergerak (direproduksi di uji emulator) | N/A — sudah tercatat di backlog; dipakai sebagai pembanding |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`
2. Skenario emulator diam → bergerak.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug lintDebug` → `BUILD SUCCESSFUL`; 235 test, 0 gagal (`StationaryDetectorTest` 7); lint 0 error, 1 warning (`OldTargetApi`). Emulator, skenario sama dengan temuan: diam → interval 30 s (jeda 123 → 157 → 187 s), lalu bergerak → titik kembali tiap 5 s, jarak trip 700 m sesuai rute (sebelum perbaikan: 0 titik, 0 m).

---

## 7. Out of Scope

1. Peka terhadap kecepatan GPS berisik (backlog terpisah).
2. Prioritas `BALANCED_POWER_ACCURACY` di Fase 9 (dicatat di DEC-010 untuk ditinjau saat Fase 9 direncanakan).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
