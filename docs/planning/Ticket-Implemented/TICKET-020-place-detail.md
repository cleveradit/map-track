# Implementation Plan: TICKET-020 (Detail Tempat & Hapus Tempat)

**Ticket:** `TICKET-020`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-019`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 3 — Fitur 4 dan 5 |
| Route | `place/{placeId}`; dibuka dari item Tab Tempat |
| Data | `combine(observePlace(id), observePlaces(), observeAllVisits())`; visit milik tempat ini = `PlaceMatcher.group(visits, places)[id]` sehingga visit di area tumpang tindih hanya dihitung untuk tempat dengan pusat terdekat (sama dengan Tab Tempat dan Trip Detail) |
| Isi | Peta statis: lingkaran radius + titik pusat, kamera `placeZoom(radius)`; ringkasan Radius, Total kunjungan, Total durasi (`formatDuration` jumlah durasi visit); daftar kunjungan terbaru dulu: tanggal, jam datang–pergi, durasi; tap → Trip Detail |
| Kosong | `Belum ada kunjungan` |
| Aksi top bar | Ubah (`Icons.Filled.Edit`) → `Routes.editPlace(id)`; Hapus (`Icons.Filled.Delete`) → dialog konfirmasi → `deletePlace` → kembali |
| Hapus | Hanya baris `places`; trip, titik, dan visit tidak tersentuh (visit kehilangan nama) |
| Tempat tidak ada | `NotFound` (mis. dibuka setelah dihapus) |
| Lapisan lingkaran | `addPlaceCircleLayers`/`placeCircle` dipindah ke `ui/places/PlaceCircle.kt` agar dipakai pemilih dan detail |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna melihat seberapa sering dan berapa lama ia berada di sebuah tempat, membuka trip terkait, serta mengubah atau menghapus tempat tanpa kehilangan data trip.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceCircle.kt` — `addPlaceCircleLayers`, `placeCircle`, `PLACE_CIRCLE_COLOR` (dipindah dari `PlacePickerMap.kt`).
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceDetailViewModel.kt`
   - `sealed interface PlaceDetailUiState { Loading; NotFound; Loaded(name, radius: String, latitude, longitude, radiusMeters, visitCount: Int, totalDuration: String, visits: List<PlaceVisitItem>) }`
   - `data class PlaceVisitItem(val tripId: String, val date: String, val timeRange: String, val duration: String)`
   - `fun delete()`; `val deleted: StateFlow<Boolean>`; `ARG_PLACE_ID`, `Factory`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceDetailScreen.kt`
   - `fun PlaceDetailScreen(onBack: () -> Unit, onEdit: (String) -> Unit, onTripClick: (String) -> Unit, modifier: Modifier = Modifier, viewModel: PlaceDetailViewModel = ...)`
4. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt` — `Routes.PLACE_DETAIL`, `Routes.placeDetail(id)`; Tab Tempat `onPlaceClick`.
5. File: `app/src/main/res/values/strings.xml` — teks detail & dialog hapus.
6. Test: `ui/places/PlaceDetailViewModelTest.kt`.

---

## 4. Scope of Changes

### A. UI

1. ViewModel, layar, peta statis, dialog hapus, route.

### B. Test

1. Total, tumpang tindih, reaktif, hapus.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Total | 2 trip dengan visit 10 menit dan 20 menit di tempat | `visitCount = 2`, `totalDuration = "30 menit"`, daftar terbaru dulu dengan `tripId` | `[x]` |
| Boundary: tanpa kunjungan | Tempat tanpa visit | `visitCount = 0`, `totalDuration = "0 menit"`, daftar kosong | `[x]` |
| Tumpang tindih | Visit lebih dekat ke tempat lain yang juga mencakupnya | Tidak dihitung di tempat ini | `[x]` |
| Reaktif | Radius diperkecil sehingga visit keluar | Loaded berikutnya `visitCount = 0` | `[x]` |
| Failure: tempat tidak ada | id tidak dikenal | `NotFound` | `[x]` |
| Hapus | `delete()` | `deleted = true`; tempat hilang; trip, titik, visit utuh (failure case PRD: tidak ada data hilang) | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 151 test, 0 gagal (`PlaceDetailViewModelTest` 6). Tampilan peta detail dan dialog hapus diuji manual di checklist TICKET-022.

---

## 7. Out of Scope

1. Nama di Trip Detail dan "Simpan sebagai tempat" (TICKET-021).
2. Statistik lintas tempat, grafik kunjungan.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
