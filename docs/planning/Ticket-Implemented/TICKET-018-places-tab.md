# Implementation Plan: TICKET-018 (Tab Tempat — Daftar Tempat)

**Ticket:** `TICKET-018`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-017`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 3 — Fitur 1; PRD §27 (navigasi) |
| Navigasi | Tab ketiga **Tempat** (ikon `Icons.Filled.Place`, route `places`); urutan Home, History, Tempat |
| Isi item | Nama; `Radius <formatDistance>`; `<n> kunjungan · terakhir <formatDate(arrivedAt visit terbaru)>` atau `Belum ada kunjungan` |
| Hitung kunjungan | On-the-fly: `combine(observePlaces, observeAllVisits)` → `PlaceMatcher.group()`; tiap visit dihitung hanya untuk satu tempat (pusat terdekat), sama dengan aturan nama di Trip Detail |
| Urutan | Dari repository (nama, tanpa membedakan huruf besar/kecil) |
| Kosong | `Belum ada tempat.` |
| Tombol tambah & tap item | Parameter callback `onAddPlace` / `onPlaceClick` disiapkan; dihubungkan ke route di TICKET-019 (editor) dan TICKET-020 (detail) |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna melihat semua tempat yang ia simpan beserta berapa kali dan kapan terakhir ia mengunjunginya, dari tab navigasi baru.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceMatcher.kt`
   - `fun group(visits: List<Visit>, places: List<Place>): Map<String, List<Visit>>` — key `place.id`, urutan visit dipertahankan; visit tanpa tempat tidak masuk.
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceListItem.kt`
   - `data class PlaceListItem(val placeId: String, val name: String, val radius: String, val visitCount: Int, val lastVisitDate: String?)`
   - `fun Place.toListItem(visits: List<Visit>, zone: ZoneId = ZoneId.systemDefault()): PlaceListItem`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlacesViewModel.kt`
   - `val items: StateFlow<List<PlaceListItem>?>` (null saat loading); `Factory`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlacesScreen.kt`
   - `fun PlacesScreen(onPlaceClick: (String) -> Unit, onAddPlace: () -> Unit, modifier: Modifier = Modifier, viewModel: PlacesViewModel = ...)`
5. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt`
   - `Routes.PLACES = "places"`, `TopLevelTab.Places`.
6. File: `app/src/main/res/values/strings.xml` — `nav_places`, `places_empty`, `place_radius`, `place_visit_summary`, `place_no_visits`, `place_add`.
7. Test: `PlaceMatcherTest` (+group), `ui/places/PlaceListItemTest.kt`, `ui/places/PlacesViewModelTest.kt`.

---

## 4. Scope of Changes

### A. Domain

1. `PlaceMatcher.group`.

### B. UI

1. ViewModel, item mapper, layar, tab navigasi.

### C. Test

1. Pengelompokan, format item, ViewModel reaktif.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Kelompokkan visit | 3 visit: 2 di tempat A, 1 di luar semua tempat | A → 2 visit; visit luar tidak masuk | `[x]` |
| Tumpang tindih | Visit dalam radius A dan B, lebih dekat ke B | Hanya dihitung untuk B | `[x]` |
| Format item | Tempat radius 100, 2 visit (terbaru 2 Oktober 2026) | `Radius 100 m`, `visitCount = 2`, `lastVisitDate = "2 Oktober 2026"` | `[x]` |
| Boundary: tanpa kunjungan | Tempat tanpa visit cocok | `visitCount = 0`, `lastVisitDate = null` | `[x]` |
| Reaktif | Radius tempat diperkecil sehingga visit keluar | Item berikutnya `visitCount = 0` | `[x]` |
| Kosong | Tidak ada tempat | `items` = list kosong | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 132 test, 0 gagal (`PlaceMatcherTest` 7, `PlaceListItemTest` 2, `PlacesViewModelTest` 2). Ringkasan kunjungan memakai string biasa (bukan plurals) karena bahasa Indonesia tidak berbentuk jamak dan lint `MissingQuantity` akan menuntut `one`.

---

## 7. Out of Scope

1. Form buat/ubah (TICKET-019), detail & hapus (TICKET-020).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
