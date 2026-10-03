# Implementation Plan: TICKET-021 (Nama Tempat di Trip Detail & "Simpan sebagai tempat")

**Ticket:** `TICKET-021`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-020`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 3 — Fitur 2 (dari visit) dan 6 |
| Nama visit | `TripDetailViewModel` menggabungkan `observePlaces()`; `VisitItem.placeName` = `PlaceMatcher.match(pusat visit, places)?.name`. Label item: `<nama tempat> · <durasi>`, atau `Tempat singgah · <durasi>` bila tidak cocok. Perubahan/hapus tempat langsung memperbarui label (Flow) |
| Aksi "Simpan sebagai tempat" | `TextButton` di bawah item visit yang **belum** bernama; membuka form tempat baru dengan titik = pusat visit (`Routes.newPlaceAt`). Visit yang sudah bernama tidak menampilkan aksi agar daftar tidak penuh; tempat tumpang tindih tetap bisa dibuat dari Tab Tempat |
| Offline | Form dari visit sudah memiliki titik, sehingga bisa disimpan tanpa peta (TICKET-019) |
| Konstruktor ViewModel | `TripDetailViewModel(tripId, repository, placeRepository)`; test lama diperbarui |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat (tiket langsung `READY`) |

---

## 2. Objective

Visit di Trip Detail menampilkan nama tempat yang cocok, dan pengguna dapat menjadikan sebuah tempat singgah sebagai tempat bernama langsung dari Trip Detail.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/VisitItem.kt`
   - `data class VisitItem(val timeRange: String, val duration: String, val center: RoutePoint, val placeName: String?)`
   - `fun Visit.toVisitItem(placeName: String?, zone: ZoneId = ZoneId.systemDefault()): VisitItem`
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt`
   - Konstruktor + `placeRepository: PlaceRepository`; `combine` empat Flow.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailScreen.kt`
   - `fun TripDetailScreen(onBack: () -> Unit, onSaveVisitAsPlace: (latitude: Double, longitude: Double) -> Unit, modifier, viewModel)`
4. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt` — `onSaveVisitAsPlace` → `Routes.newPlaceAt`.
5. File: `app/src/main/res/values/strings.xml` — `visit_label` = `%1$s · %2$s`, `visit_save_as_place`.
6. Test: `VisitItemTest`, `TripDetailViewModelTest`.

---

## 4. Scope of Changes

### A. UI

1. Nama visit, aksi simpan, route.

### B. Test

1. Cocok, tidak cocok, reaktif terhadap hapus tempat.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Visit cocok | Tempat "Rumah" mencakup pusat visit | `placeName = "Rumah"` | `[x]` |
| Boundary: di luar radius | Pusat visit 150 m dari tempat radius 100 m | `placeName = null` | `[x]` |
| Tumpang tindih | Dua tempat mencakup visit | Nama tempat dengan pusat terdekat | `[x]` |
| Failure: tempat dihapus | Hapus tempat saat Trip Detail terbuka | Loaded berikutnya `placeName = null`; visit tetap ada | `[x]` |
| Mapper | `toVisitItem("Kantor")` | `placeName = "Kantor"`, field lain tidak berubah | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 155 test, 0 gagal (`TripDetailViewModelTest` 8, `VisitItemTest` 3). Tampilan aksi "Simpan sebagai tempat" diuji manual di checklist TICKET-022.

---

## 7. Out of Scope

1. Nama tempat di History (PRD: History tidak berubah).
2. Penutup fase (TICKET-022).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
