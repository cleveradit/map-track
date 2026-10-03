# Implementation Plan: TICKET-019 (Form Buat & Ubah Tempat)

**Ticket:** `TICKET-019`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-018`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 3 — Fitur 2 dan 3; Rule 7 (permission) |
| Route | `place-editor?placeId={placeId}&lat={lat}&lng={lng}` (semua opsional, `String`; lat/lng di-parse `Double` agar presisi tidak hilang seperti `FloatType`). Baru: tanpa argumen; dari visit: `lat`/`lng`; ubah: `placeId` |
| Pemilih titik | Pin tetap di tengah peta (ikon Compose di atas `MapView`); titik tempat = target kamera. Titik hanya diambil dari gerakan kamera yang dimulai gesture pengguna (`REASON_API_GESTURE`), sehingga kamera default (Indonesia) tidak pernah menjadi titik secara tidak sengaja. Tempat baru tanpa titik: tombol Simpan nonaktif dan petunjuk "Geser peta untuk menentukan titik tempat" |
| Kamera terprogram | Saat titik diisi bukan dari peta (memuat tempat, dari visit, lokasi saat ini) ViewModel mengirim `CameraRequest` baru (bukan data class) → kamera pindah ke titik dengan zoom `placeZoom(radius)` |
| Lingkaran radius | Poligon 64 sisi (`circleRing`) di target kamera: `FillLayer` biru transparan + `LineLayer`; diperbarui setiap kamera bergerak dan setiap radius berubah |
| Radius | `Slider` 50–1 000 m, kelipatan 10 m (`steps = 94`); default `PlaceConfig.DEFAULT_RADIUS_METERS`; nilai di luar rentang tidak dapat dipilih (di-coerce) |
| Nama | `OutlinedTextField` dengan penghitung `n/50`; error ditampilkan setelah pengguna mengubah nama atau menekan Simpan |
| Lokasi saat ini | Izin belum ada → dialog penjelasan (Rule 7) → minta `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`; ditolak → pesan. Location mati → pesan. Fix pertama dengan accuracy ≤ `TrackingConfig.MAX_ACCURACY_METERS`, batas waktu `PlaceConfig.CURRENT_LOCATION_TIMEOUT_MS` (30 s) → pesan bila habis |
| Simpan | Create/update lewat `PlaceRepository`; sukses → kembali (`popBackStack`) |
| Offline | Form tetap bisa disimpan bila titik sudah ada (dari visit, tempat lama, atau lokasi saat ini) walaupun peta tidak termuat |
| Hapus | Tidak di form; di Detail Tempat (TICKET-020) |
| Persetujuan | Instruksi user "lanjut Fase 3" di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat membuat tempat baru (dari peta, lokasi saat ini, atau titik visit) dan mengubah nama, titik, dan radius tempat yang ada, tanpa internet bila titik sudah diketahui.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/PlaceConfig.kt`
   - `CURRENT_LOCATION_TIMEOUT_MS = 30_000L`, `RADIUS_STEP_METERS = 10.0`
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/PlaceGeometry.kt`
   - `fun circleRing(latitude: Double, longitude: Double, radiusMeters: Double, segments: Int = 64): List<Pair<Double, Double>>` — pasangan (longitude, latitude), cincin tertutup.
   - `fun placeZoom(radiusMeters: Double): Double`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceEditorViewModel.kt`
   - `PlaceEditorUiState(isEditing, isLoading, notFound, name, nameError, showNameError, latitude: Double?, longitude: Double?, radiusMeters, isLocating, message: EditorMessage?, cameraRequest: CameraRequest?, saved: Boolean)`, `canSave`.
   - `enum class EditorMessage { PERMISSION_RATIONALE, PERMISSION_DENIED, LOCATION_DISABLED, LOCATION_TIMEOUT, SAVE_FAILED }`
   - `class CameraRequest(val latitude: Double, val longitude: Double)`
   - `onNameChange`, `onRadiusChange`, `onMapCenterPicked(lat, lng)`, `useCurrentLocation()`, `onPermissionResult(granted: Boolean)`, `dismissMessage()`, `save()`.
   - Konstruktor: `(placeId: String?, initialLatitude: Double?, initialLongitude: Double?, placeRepository: PlaceRepository, locationSource: LocationSource)`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlacePickerMap.kt`
   - `fun PlacePickerMap(radiusMeters: Double, cameraRequest: CameraRequest?, onCenterPicked: (Double, Double) -> Unit, modifier: Modifier = Modifier)`
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/places/PlaceEditorScreen.kt`
   - `fun PlaceEditorScreen(onDone: () -> Unit, modifier: Modifier = Modifier, viewModel: PlaceEditorViewModel = ...)`
6. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt`
   - `Routes.PLACE_EDITOR`, `Routes.newPlace()`, `Routes.newPlaceAt(lat, lng)`, `Routes.editPlace(id)`; FAB Tab Tempat → `newPlace()`.
7. File: `app/src/main/res/values/strings.xml` — teks form.
8. Test: `ui/map/PlaceGeometryTest.kt`, `ui/places/PlaceEditorViewModelTest.kt`.

---

## 4. Scope of Changes

### A. Geometri & config

1. `circleRing`, `placeZoom`, konstanta.

### B. Form

1. ViewModel, peta pemilih, layar, route.

### C. Test

1. Geometri, ViewModel (validasi, simpan, lokasi saat ini).

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Lingkaran | `circleRing` radius 100 m, 64 sisi | 65 titik, tertutup, tiap titik 100 m (± 0.5%) dari pusat | `[x]` |
| Zoom | radius 50 / 100 / 1 000 | zoom menurun seiring radius membesar, dalam 12–17 | `[x]` |
| Tempat baru tanpa titik | Nama valid, belum geser peta | `canSave = false` | `[x]` |
| Buat dari peta | Pilih titik lewat peta + nama | Tempat tersimpan dengan titik & radius default; `saved = true` | `[x]` |
| Buat dari visit | `lat`/`lng` awal | Titik terisi, `cameraRequest` terisi, simpan berhasil tanpa peta | `[x]` |
| Failure: nama kosong | Simpan dengan nama `"  "` | Tidak tersimpan; `nameError = EMPTY`, `showNameError = true` | `[x]` |
| Boundary: nama 51 karakter / radius | `onNameChange` 51 karakter; `onRadiusChange(20)` / `(1 234)` / `(104)` | `TOO_LONG`; radius 50 / 1 000 / 100 | `[x]` |
| Ubah tempat | `placeId` ada | Field terisi dari tempat; simpan memperbarui tempat yang sama | `[x]` |
| Failure: ubah tempat yang tidak ada | `placeId` tidak dikenal | `notFound = true` | `[x]` |
| Lokasi saat ini: izin belum ada | `useCurrentLocation()` | `PERMISSION_RATIONALE`; `onPermissionResult(false)` → `PERMISSION_DENIED` | `[x]` |
| Lokasi saat ini: location mati | `useCurrentLocation()` | `LOCATION_DISABLED` | `[x]` |
| Lokasi saat ini: sukses | Fix accuracy 80 m lalu 10 m | Titik = fix 10 m; `cameraRequest` baru | `[x]` |
| Failure: lokasi timeout | Tidak ada fix 30 s | `LOCATION_TIMEOUT`, `isLocating = false` | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 145 test, 0 gagal (`PlaceGeometryTest` 2, `PlaceEditorViewModelTest` 11). Tampilan peta pemilih, pin, dan lingkaran diuji manual di checklist TICKET-022.

---

## 7. Out of Scope

1. Hapus dan detail tempat (TICKET-020); aksi dari Trip Detail (TICKET-021).
2. Pencarian alamat / reverse geocoding (§37).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
