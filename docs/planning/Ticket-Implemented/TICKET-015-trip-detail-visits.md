# Implementation Plan: TICKET-015 (Visit di Trip Detail)

**Ticket:** `TICKET-015`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-014`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 2 — Tampilan |
| Data UI | `TripDetailUiState.Loaded.visits: List<VisitItem>`; `VisitItem.timeRange` = `"HH:mm - HH:mm"` (`formatTime`, zona perangkat), `VisitItem.duration` = `formatDuration(durationMs)` |
| Label item | String resource `"Tempat singgah · %1$s"` dengan durasi; nama tempat baru di Fase 3 |
| Trip aktif | `visits` selalu kosong (PRD: visit pada trip aktif tidak ditampilkan) |
| Posisi daftar | Judul "Tempat singgah" + daftar, tepat di bawah ringkasan dan di atas peta; seluruh bagian disembunyikan bila tidak ada visit |
| Marker peta | Dua `CircleLayer` dari satu source: halo ungu `#9334E6` transparan (radius 14, opacity 0.25) dan titik ungu `#9334E6` (radius 6, stroke putih 2); ditambahkan setelah garis rute dan sebelum layer start/finish sehingga tergambar di bawahnya. Beda bentuk (halo) dan warna dari start (hijau) / finish (merah) |
| Tap item | Kamera `animateCamera` ke pusat visit dengan zoom `MapConfig.FOLLOW_ZOOM`; peta dibawa ke viewport dengan `BringIntoViewRequester`. Setiap tap membuat `VisitFocusRequest` baru (bukan data class) sehingga tap berulang pada item yang sama tetap memusatkan ulang |
| History | Tidak berubah |
| Persetujuan | Instruksi Fase 2 dari user di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna melihat di mana dan berapa lama ia singgah selama sebuah trip, sebagai marker di peta dan sebagai daftar di Trip Detail, dan dapat menekan item untuk melihat lokasinya di peta.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/VisitItem.kt` (baru)
   - `data class VisitItem(val timeRange: String, val duration: String, val center: RoutePoint)`
   - `fun Visit.toVisitItem(zone: ZoneId = ZoneId.systemDefault()): VisitItem`
   - `class VisitFocusRequest(val center: RoutePoint)` — identitas per tap.
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt`
   - `Loaded` + `val visits: List<VisitItem>`; `combine` dengan `repository.observeVisits(tripId)`; kosong bila trip aktif.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripRouteMap.kt`
   - `fun TripRouteMap(route: List<RoutePoint>, visits: List<RoutePoint>, showFinish: Boolean, focus: VisitFocusRequest?, modifier: Modifier = Modifier)`
   - Source `route-visits`, layer `route-visit-halo` dan `route-visit-dot`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailScreen.kt`
   - Bagian "Tempat singgah", state `VisitFocusRequest?`, `BringIntoViewRequester` pada peta.
5. File: `app/src/main/res/values/strings.xml`
   - `visits_title` = "Tempat singgah", `visit_label` = "Tempat singgah · %1$s".
6. Test: `ui/tripdetail/VisitItemTest.kt` (baru), `ui/tripdetail/TripDetailViewModelTest.kt`.

---

## 4. Scope of Changes

### A. State

1. `VisitItem` + mapper, `Loaded.visits`.

### B. UI

1. Daftar "Tempat singgah" dengan titik ungu sebagai legenda marker.
2. Marker visit di `TripRouteMap`, fokus kamera, bring-into-view.

### C. Test

1. Format item, visit di state Loaded, trip aktif tanpa visit.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Format item | Visit 08:15–16:30 UTC, zona UTC | `timeRange = "08:15 - 16:30"`, `duration = "8 jam 15 menit"`, `center` = pusat visit | `[x]` |
| Boundary: visit tepat 5 menit | Visit 5 menit | `duration = "5 menit"` | `[x]` |
| Trip completed dengan visit | Trip diselesaikan dengan titik diam 10 menit | `Loaded.visits` berisi 1 item | `[x]` |
| Trip tanpa visit | Trip completed tanpa singgahan | `Loaded.visits` kosong → bagian daftar tidak tampil | `[x]` |
| Failure/boundary: trip aktif | Trip aktif yang punya baris visit (disisipkan langsung) | `Loaded.visits` kosong | `[x]` |
| Marker & tap item (manual di HP) | Buka trip dengan visit, tekan item dua kali | Marker ungu berhalo di bawah start/finish; peta terlihat dan kamera memusat setiap tap | `[ ]` — diuji user lewat checklist manual TICKET-016 setelah semua fase selesai |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Expected:

1. Semua test lulus.

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 109 test, 0 gagal (`VisitItemTest` 2, `TripDetailViewModelTest` 5).

---

## 7. Out of Scope

1. Nama tempat dan aksi "Simpan sebagai tempat" (Fase 3).
2. Perubahan History.
3. Highlight marker terpilih.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (kasus marker & tap diuji user lewat checklist TICKET-016 setelah semua fase selesai).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
