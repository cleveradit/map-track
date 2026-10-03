# Implementation Plan: TICKET-008 (MapLibre di Home)

**Ticket:** `TICKET-008`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-007`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Library | `org.maplibre.gl:android-sdk` 13.6.1 |
| Sumber tile (PRD §22) | OpenFreeMap style `https://tiles.openfreemap.org/styles/liberty` — tanpa akun/API key; atribusi OpenFreeMap, OpenMapTiles, OpenStreetMap ditampilkan oleh tombol atribusi MapLibre |
| Marker posisi | `GeoJsonSource` 1 titik + `CircleLayer` dari `GpsFix` Home; **bukan** `LocationComponent` (GPS tetap satu-satunya sumber koordinat, PRD §22) |
| Kamera | Sebelum fix pertama: Indonesia (-2.5, 118, zoom 3.5). Fix pertama: zoom 16. Selanjutnya mengikuti posisi sampai pengguna menggeser peta; tombol "Ikuti posisi" mengaktifkan lagi |
| Tanpa internet | Peta boleh kosong; Home dan tracking tetap berfungsi (PRD §23 tahap 1) |
| Lifecycle | `MapView` dibungkus `AndroidView`; event lifecycle diteruskan; `MapLibre.getInstance` dipanggil di composable, bukan di `Application` |
| Komponen reusable | `MapLibreMap` composable dipakai lagi di TICKET-009 |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Home menampilkan peta MapLibre dengan marker posisi pengguna dari GPS, mengikuti pergerakan, tanpa mengganggu tracking bila tile tidak tersedia.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/AndroidManifest.xml`
   - Permission `INTERNET` (hanya untuk tile peta).
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapConfig.kt`
   - `object MapConfig { STYLE_URL, DEFAULT_LATITUDE = -2.5, DEFAULT_LONGITUDE = 118.0, DEFAULT_ZOOM = 3.5, FOLLOW_ZOOM = 16.0 }`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapLibreMap.kt`
   - `@Composable fun MapLibreMap(modifier: Modifier, onStyleLoaded: (MapLibreMap, Style) -> Unit)` — membuat `MapView`, meneruskan lifecycle, memuat `MapConfig.STYLE_URL`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/CameraFollow.kt`
   - `fun cameraActionFor(following: Boolean, hasCenteredOnce: Boolean): CameraAction` (nama final) dengan `enum class CameraAction { NONE, ZOOM_TO_FIX, FOLLOW }` — logika murni kamera.
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeMap.kt`
   - `@Composable fun HomeMap(fix: GpsFix?, modifier: Modifier)` — marker + kamera + tombol ikuti posisi.
6. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt`
   - Integration point: `MapPlaceholder()` diganti `HomeMap(state.fix)`.

---

## 4. Scope of Changes

### A. Build

1. Dependency MapLibre, permission `INTERNET`.

### B. Peta

1. `MapConfig`, `MapLibreMap`, `CameraFollow`, `HomeMap`.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Kamera fix pertama | `following = true`, belum pernah center | `ZOOM_TO_FIX` | `[x]` |
| Kamera mengikuti | `following = true`, sudah center | `FOLLOW` | `[x]` |
| Boundary: pengguna geser peta | `following = false` | `NONE` | `[x]` |
| Build dengan MapLibre | `assembleDebug` | Sukses | `[x]` |
| Manual: peta tampil | Online, izin lokasi ada | Peta OSM, titik biru di posisi, zoom ke jalan | `[ ]` manual |
| Manual: ikuti posisi | Geser peta, lalu tekan "Ikuti posisi" | Setelah geser kamera diam; tombol mengembalikan ke posisi | `[ ]` manual |
| Failure manual: offline | Mode pesawat (GPS tetap aktif), buka Home, Start Tracking | Peta kosong/abu, kecepatan & tracking tetap jalan | `[ ]` manual |
| Manual: atribusi | Tekan ikon (i) di pojok peta | Atribusi OpenFreeMap / OpenMapTiles / OpenStreetMap | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 63/63 lulus (baru: `CameraFollowTest` 3), `assembleDebug` sukses (APK debug ±85 MB karena library native MapLibre untuk 4 ABI), lint 0 error. Tambahan saat implementasi: `requestDisallowInterceptTouchEvent` agar gesture peta tidak diambil scroll Home. Baris `manual` menunggu uji user di HP, termasuk geser/zoom peta di dalam Home yang bisa di-scroll.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Rute/polyline di Trip Detail (TICKET-009).
2. Peta offline (Fase 4).
3. Rotasi marker sesuai bearing.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
