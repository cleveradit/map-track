# Implementation Plan: TICKET-009 (Rute Trip di Peta)

**Ticket:** `TICKET-009`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-008`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Rute (PRD §20) | Semua titik tersimpan trip, urut `recorded_at`, digambar sebagai `LineLayer` dari `LineString` |
| Marker start | Lingkaran hijau `#1E8E3E` di titik pertama |
| Marker finish | Lingkaran merah `#D93025` di titik terakhir, **hanya** untuk trip `completed` (trip aktif belum punya finish) |
| Kamera | Diatur sekali saat rute pertama kali tersedia: bounding box rute + padding 48 px; bila sebaran < 0.0005° → center zoom 16; tanpa titik → kamera default |
| < 2 titik | Teks `Data lokasi tidak cukup untuk menggambar rute.` di bawah peta (PRD §16) |
| Trip aktif | Rute diperbarui live; kamera tidak di-reset agar pengguna bisa menggeser peta |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Trip Detail menampilkan jalur perjalanan di peta dengan penanda start dan finish yang dapat dibedakan, menggantikan placeholder peta.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/RouteCamera.kt`
   - `data class RoutePoint(latitude: Double, longitude: Double)`
   - `sealed interface RouteCamera { None; Center(point: RoutePoint); Bounds(south, west, north, east: Double) }`
   - `fun routeCamera(route: List<RoutePoint>): RouteCamera`
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt`
   - `TripDetailUiState.Loaded` ditambah `route: List<RoutePoint>`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripRouteMap.kt`
   - `@Composable fun TripRouteMap(route: List<RoutePoint>, showFinish: Boolean, modifier: Modifier)`
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailScreen.kt`
   - Integration point: placeholder peta diganti `TripRouteMap`.

---

## 4. Scope of Changes

### A. Trip detail

1. Data rute di state, logika kamera, komponen peta rute.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Kamera tanpa titik | `[]` | `None` | `[x]` |
| Boundary: satu titik / titik berdekatan | 1 titik; 2 titik selisih 0.0001° | `Center` | `[x]` |
| Kamera bounding box | Titik (-7.80,110.36) & (-7.75,110.40) | `Bounds(-7.80, 110.36, -7.75, 110.40)` | `[x]` |
| State rute | 3 titik tersimpan | `route` berisi 3 titik urut waktu | `[x]` |
| Manual: rute selesai | Buka detail trip selesai | Garis rute, titik hijau start, titik merah finish, seluruh rute terlihat | `[ ]` manual |
| Manual: trip aktif | Buka detail saat tracking | Rute bertambah, tanpa titik merah | `[ ]` manual |
| Failure manual: data kurang | Trip < 2 titik | Teks data lokasi tidak cukup | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 67/67 lulus (baru: `RouteCameraTest` 3, `TripDetailViewModelTest.routeContainsStoredPointsInOrder`), `assembleDebug` sukses, lint 0 error. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Marker tempat berhenti (Fase 2).
2. Pewarnaan rute berdasarkan kecepatan.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
