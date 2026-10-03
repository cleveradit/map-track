# Implementation Plan: TICKET-025 (Halaman Settings, Satuan Imperial, Kamera Ikuti Posisi)

**Ticket:** `TICKET-025`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-024`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §27, §38 Fase 4 — Settings (v2.4) |
| Akses | Ikon Settings (`Icons.Filled.Settings`) di samping judul Home → route `settings` |
| Isi halaman | **Tracking:** interval (3/5/10/30 detik) dan ambang akurasi (20/30/50/100 m, ditampilkan dalam satuan aktif) sebagai `FilterChip`, keterangan "Perubahan berlaku mulai trip berikutnya." **Tampilan:** satuan Metrik/Imperial, switch kamera Home mengikuti posisi. **Peta:** Hapus cache peta (dialog konfirmasi, pesan hasil). **Tentang:** versi aplikasi (`PackageManager`), atribusi peta, pernyataan privasi "Data lokasi hanya disimpan di perangkat." |
| Satuan imperial | Kecepatan mph (`m/s × 2.236936`); jarak < 0.1 mi → feet (`m × 3.28084`, dibulatkan), selainnya mi 1 desimal; akurasi `± n ft`. Ambang "diam" tetap `TrackingConfig.STATIONARY_SPEED_KMH` (dibandingkan dalam km/h sebelum konversi). Data tersimpan tidak berubah (Rule 2) |
| Cakupan satuan | Home (kecepatan, akurasi), notification tracking, History, Trip Detail (ringkasan, grafik kecepatan + label satuan), Tab Tempat, Detail Tempat, form tempat, pilihan akurasi di Settings |
| Penerusan satuan | ViewModel menerima `settings: Flow<AppSettings> = flowOf(AppSettings.DEFAULT)` (Factory memakai `settingsRepository.settings`), sehingga test lama tetap jalan. Notification membaca satuan terkini dari settings di service |
| Kamera ikuti posisi | `HomeMap(followByDefault)`: nilai awal `following` dari setting. Nonaktif → kamera tetap zoom ke fix pertama sekali, lalu tidak mengikuti; tombol "Ikuti posisi" tetap tersedia |
| Grafik | `SpeedSample.speed` dalam satuan tampilan; `chartMax()` kelipatan 10 satuan tampilan |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat mengatur interval, ambang akurasi, satuan, dan perilaku kamera dari halaman Settings, menghapus cache peta, dan melihat informasi aplikasi; satuan imperial berlaku di semua tampilan jarak dan kecepatan.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt`
   - `formatSpeed(speedMps: Double?, unit: DistanceUnit = METRIC)` (menggantikan `formatSpeedKmh`), `formatCurrentSpeed(..., unit = METRIC)`, `formatDistance(meters, unit = METRIC)`, `formatAccuracy(meters, unit = METRIC)`, `speedUnitLabel(unit)`, `fun Double.toDisplaySpeed(unit): Double` (dari m/s).
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/SpeedSeries.kt`
   - `SpeedSample(offsetMs, speed)`; `speedSeries(points, startedAt, unit = METRIC, maxSamples = 500)`; `chartMax(samples)`.
3. ViewModel + mapper: `TripDetailViewModel` (Loaded + `speedUnit: String`), `toSummary(unit)`, `HistoryViewModel`/`toHistoryItem(unit)`, `PlacesViewModel`/`toListItem(visits, unit)`, `PlaceDetailViewModel`, `PlaceEditorViewModel` (state `distanceUnit`), `HomeViewModel` (state `distanceUnit`, `mapFollowLocation`).
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt` (baru)
   - `SettingsViewModel(settingsRepository: SettingsRepository, clearMapCache: suspend () -> Result<Unit>)`: `settings: StateFlow<AppSettings?>`, `cacheMessage: StateFlow<CacheMessage?>`, setter, `clearMapCache()`, `dismissCacheMessage()`.
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt`, `HomeMap.kt` — ikon Settings, `followByDefault`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt`, `TrackingNotification.kt` — satuan di notification.
7. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt` — `Routes.SETTINGS`.
8. File: `app/src/main/res/values/strings.xml` — teks Settings.
9. Test: `FormattersTest`, `SpeedSeriesTest`, `TripSummaryTest`, `ui/settings/SettingsViewModelTest.kt` (baru), `TripDetailViewModelTest` (imperial).

---

## 4. Scope of Changes

### A. Satuan

1. Formatter, grafik, mapper, ViewModel, notification.

### B. Settings

1. ViewModel, layar, route, ikon Home, kamera ikuti posisi.

### C. Test

1. Formatter, grafik, ViewModel.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Kecepatan imperial | 12.5 m/s imperial / metrik | `28 mph` / `45 km/h` | `[x]` |
| Boundary diam | 0.2 m/s imperial | `0 mph` | `[x]` |
| Jarak imperial | 100 m / 160 m (0.0994 mi) / 170 m / 21 700 m | `328 ft` / `525 ft` / `0.1 mi` / `13.5 mi` | `[x]` |
| Akurasi imperial | 6 m | `± 20 ft` | `[x]` |
| Grafik imperial | 10 m/s | sampel `22.37`; `chartMax` 30 | `[x]` |
| Trip Detail imperial | Settings imperial, trip selesai 1 000 m | `distance = "0.6 mi"`, `speedUnit = "mph"` | `[x]` |
| Settings VM | Set interval 30 s, imperial, follow off | Nilai tersimpan dan terbaca kembali | `[x]` |
| Hapus cache | `clearMapCache` sukses / gagal | `CacheMessage.CLEARED` / `CacheMessage.FAILED` | `[x]` |
| Failure: nilai tidak valid | `setTrackingInterval(7_000)` lewat VM | Diabaikan, nilai tetap | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 171 test, 0 gagal (`FormattersTest` 8, `SpeedSeriesTest` 4, `SettingsViewModelTest` 3, `TripDetailViewModelTest` 9). Matrix jarak imperial dikoreksi: 160 m = 0.0994 mi sehingga tampil `525 ft`; contoh `0.1 mi` memakai 170 m.

---

## 7. Out of Scope

1. Setting trip otomatis (Fase 5).
2. Unduhan wilayah (dihapus, PRD v2.4).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
