# Architecture Map

## Summary

Map Track adalah aplikasi Android single-module (`:app`) berbasis Jetpack Compose. Aplikasi berisi Application class dengan dependency container manual, satu Activity, navigasi tiga tab (Home, History, Tempat) plus Trip Detail, Detail Tempat, dan form tempat, foreground service tracking, layer data Room untuk trip, location point, visit, dan tempat, serta logika domain murni untuk filter GPS, statistik trip, deteksi visit, dan pencocokan visit ↔ tempat. Struktur paket mengikuti [PRD §42](initiate-file/prd-map-track.md#42-struktur-implementasi-awal).

## Components

| Component | Responsibility | Code location | Dependencies |
|---|---|---|---|
| `MapTrackApplication` | Membuat `AppContainer` saat proses dimulai, lalu meluncurkan backfill visit dan `reconcileAutoTrip()` | `app/src/main/java/com/radityodwiki/maptrack/MapTrackApplication.kt` | `AppContainer` |
| `AppContainer` | Wiring dependency manual: `database`, `tripRepository`, `locationTracker`, `tripRecorder`, `trackingStateHolder`, `trackingController`, `visitBackfill`, `placeRepository`, `settingsRepository` (lazy, DataStore `settings`), `stillnessHolder`, `autoTripPermissions`, `autoTripController`; `reconcileAutoTrip()`; `applicationScope` (`SupervisorJob() + Dispatchers.Default`) dan `startVisitBackfill()` | `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt` | `MapTrackDatabase`, `TripRepository`, `VisitBackfill` |
| `MainActivity` | Entry point UI, memasang theme dan NavHost | `app/src/main/java/com/radityodwiki/maptrack/MainActivity.kt` | `MapTrackNavHost`, `MapTrackTheme` |
| `MapTrackNavHost` | Scaffold + bottom navigation, route `home`, `history`, `places`, `settings`, `trip/{tripId}`, `place/{placeId}`, `place-editor?placeId=&lat=&lng=` | `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt` | `HomeScreen`, `HistoryScreen` |
| `HomeScreen` | Layar Home: alur izin lokasi, status GPS, kecepatan, akurasi, posisi | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt` | `HomeViewModel`, `HomeMap` |
| `HomeMap` | Peta Home: titik posisi dan kamera mengikuti | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeMap.kt` | `MapLibreMap`, `cameraActionFor` |
| `MapLibreMap` | `MapView` MapLibre di Compose dengan penerusan lifecycle; memuat `MapConfig.STYLE_URL` | `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapLibreMap.kt` | MapLibre Android SDK |
| `MapConfig`, `cameraActionFor` | URL style, kamera default, zoom ikuti; logika kamera murni | `app/src/main/java/com/radityodwiki/maptrack/ui/map/` | — |
| `HomeViewModel` | Menggabungkan izin, fix GPS, ticker 1 detik, trip aktif, dan state aksi menjadi `HomeUiState`; meneruskan Start/Stop ke `TrackingController` | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeViewModel.kt` | `LocationSource`, `TripRepository`, `TrackingStateHolder`, `TrackingController` |
| `TrackingController` | Validasi dan urutan Start/Stop (PRD §8); mengakhiri atau melanjutkan trip terputus (PRD §33, §38 Fase 5) | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt` | `LocationSource`, `TripRepository`, `TripRecorder`, `TrackingStateHolder`, `TrackingServiceLauncher` |
| `LocationTrackingService` | Foreground service `location`: koleksi fix (request dinamis penghemat baterai), simpan titik, notification, Stop, henti otomatis trip `auto` | `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt` | `AppContainer` |
| `AutoTripController`, `AutoStopPolicy`, `AutoTripConfig` | Trip otomatis: langganan transisi sesuai setting/izin, mulai trip `auto`, kebijakan henti dan trip pendek, konstanta Fase 5 | `app/src/main/java/com/radityodwiki/maptrack/location/` | `SettingsRepository`, `TrackingController`, `ActivityTransitions`, `AutoTripPermissions` |
| `ActivityTransitionReceiver`, `BootReceiver`, `GmsActivityTransitions` | Receiver transisi aktivitas dan boot/update; langganan Activity Recognition Transition API | `app/src/main/java/com/radityodwiki/maptrack/location/AndroidAutoTrip.kt` | Play Services Location |
| `StationaryDetector` | Penghemat baterai: deteksi diam 2 menit untuk menurunkan request lokasi | `app/src/main/java/com/radityodwiki/maptrack/location/StationaryDetector.kt` | `GeoDistance` |
| `TrackingStateHolder` | State in-process `Idle` / `Active(tripId, startedAt, lastFix)` | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingStateHolder.kt` | — |
| `TrackingNotification` | Channel, builder, dan teks notification tracking | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingNotification.kt` | Formatters |
| `TripRecorder` | Filter fix terhadap titik tersimpan terakhir lalu simpan; `finish` (waktu sekarang) dan `finishInterrupted` (titik terakhir) menyelesaikan trip | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripRecorder.kt` | `TripRepository`, `LocationFilter` |
| `LocationTracker` (`LocationSource`) | Fused Location Provider: `fixes()` sebagai Flow, cek izin dan status Location service | `app/src/main/java/com/radityodwiki/maptrack/location/LocationTracker.kt` | Play Services Location |
| `HistoryScreen` | Daftar trip dan dialog konfirmasi hapus | `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryScreen.kt` | `HistoryViewModel` |
| `TripDetailScreen`, `SpeedChart`, `TripRouteMap` | Ringkasan trip, daftar tempat singgah, peta rute (polyline + marker start/finish/visit), dan grafik kecepatan Canvas | `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/` | `TripDetailViewModel` |
| `TripDetailViewModel` | `observeTrip` + `observePoints` + `observeVisits` + `observePlaces` → `TripDetailUiState` (ringkasan, sampel kecepatan, titik rute, visit bernama); `tripId` dari `SavedStateHandle` | `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt` | `TripRepository` |
| `PlacesScreen`, `PlacesViewModel` | Tab Tempat: daftar tempat dengan jumlah & kunjungan terakhir (`observePlaces` + `observeAllVisits` → `PlaceMatcher.group`) | `app/src/main/java/com/radityodwiki/maptrack/ui/places/` | `PlaceRepository`, `TripRepository` |
| `PlaceEditorScreen`, `PlaceEditorViewModel`, `PlacePickerMap` | Form buat/ubah tempat: pin tengah peta, lingkaran radius, slider, lokasi saat ini | `app/src/main/java/com/radityodwiki/maptrack/ui/places/` | `PlaceRepository`, `LocationSource` |
| `PlaceDetailScreen`, `PlaceDetailViewModel` | Detail tempat: peta statis, total kunjungan & durasi, daftar kunjungan, hapus | `app/src/main/java/com/radityodwiki/maptrack/ui/places/` | `PlaceRepository`, `TripRepository` |
| `circleRing`, `placeZoom` | Poligon lingkaran radius dan zoom kamera tempat (murni) | `app/src/main/java/com/radityodwiki/maptrack/ui/map/PlaceGeometry.kt` | `GeoDistance` |
| `HistoryViewModel` | `observeTrips()` → `List<HistoryItem>`; hapus trip | `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryViewModel.kt` | `TripRepository` |
| `SettingsScreen`, `SettingsViewModel` | Halaman Settings: interval, akurasi, satuan, kamera ikuti, hapus cache peta, Tentang | `app/src/main/java/com/radityodwiki/maptrack/ui/settings/` | `SettingsRepository`, `MapCache` |
| `SettingsRepository` | DataStore Preferences: `Flow<AppSettings>`, setter dengan validasi pilihan | `app/src/main/java/com/radityodwiki/maptrack/data/settings/SettingsRepository.kt` | DataStore |
| `MapCache` | Batas ambient cache tile MapLibre (200 MB) dan pembersihannya | `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapCache.kt` | MapLibre `OfflineManager` |
| `MapTrackTheme` | Material 3 theme, dynamic color di Android 12+ | `app/src/main/java/com/radityodwiki/maptrack/ui/theme/Theme.kt` | — |
| `MapTrackDatabase` | Room database `map_track.db` versi 3 (`AutoMigration` 1 → 2, 2 → 3), menyediakan `TripDao`, `LocationPointDao`, `VisitDao`, dan `PlaceDao` | `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` | Room |
| `TripDao`, `LocationPointDao`, `VisitDao`, `PlaceDao` | Query trip, location point, visit, dan tempat (Flow untuk observasi) | `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/` | Room |
| `TripRepository` | API data trip untuk layer atas; menegakkan satu trip aktif, larangan hapus trip aktif, abaikan titik ganda; `finishTrip` menghitung statistik dan visit dalam satu transaksi; `recomputeVisits` untuk backfill (implementasi `VisitRecomputation`) | `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt` | `MapTrackDatabase`, `TripStatisticsCalculator`, `VisitDetector` |
| `PlaceRepository` | CRUD tempat dengan validasi (`InvalidPlaceException`); hapus hanya baris `places` | `app/src/main/java/com/radityodwiki/maptrack/data/repository/PlaceRepository.kt` | `MapTrackDatabase`, `PlaceValidator` |
| Domain model `Trip`, `TripStatus`, `LocationPoint`, `Visit`, `Place`, `PlaceInput`, `GpsFix`, `LocationPermission` | Model yang dipakai di luar layer data | `app/src/main/java/com/radityodwiki/maptrack/domain/model/` | — |
| `LocationFilter` | Menentukan apakah fix GPS disimpan (PRD §12); mengembalikan `RejectReason` atau `null` | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/LocationFilter.kt` | `GeoDistance`, `TrackingConfig` |
| `TripStatisticsCalculator` | Menghitung jarak, durasi, rata-rata, dan kecepatan maksimum (PRD §16) | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripStatisticsCalculator.kt` | `GeoDistance` |
| `VisitDetector`, `PlaceDetectionConfig` | Deteksi visit dari titik satu trip dan parameternya (PRD §38 Fase 2) | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/` | `GeoDistance` |
| `VisitBackfill` (`VisitRecomputation`) | Menghitung ulang visit trip `completed` dengan versi deteksi lama; gagal per trip tidak menghentikan yang lain | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/VisitBackfill.kt` | `TripRepository` |
| `PlaceMatcher`, `PlaceValidator`, `PlaceConfig` | Pencocokan visit ↔ tempat (radius inklusif, pusat terdekat), validasi field tempat, dan batasnya (PRD §38 Fase 3) | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/` | `GeoDistance` |
| `GeoDistance` | Jarak haversine antar-koordinat | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/GeoDistance.kt` | — |
| Formatters | Format kecepatan (termasuk aturan diam/basi), jarak, durasi, akurasi, tanggal, dan jam (locale `id-ID`) untuk UI | `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt` | `TrackingConfig` |
| `TrackingConfig` | Konstanta tracking dan filtering (interval, ambang accuracy, batas GPS jump, stale fix) | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingConfig.kt` | — |

## Main flows

Startup: `MapTrackApplication.onCreate` membuat `AppContainer` → `startVisitBackfill()` (coroutine background) → `MainActivity` memasang `MapTrackTheme` → `MapTrackNavHost` dengan start destination `home`.

Lokasi di Home: `HomeScreen` mengoleksi `HomeViewModel.uiState` dengan `collectAsStateWithLifecycle` → `uiState` (`WhileSubscribed(5_000)`) mengaktifkan `LocationTracker.fixes()` hanya bila izin `GRANTED` → `callbackFlow` mendaftarkan `LocationCallback` dan melepasnya saat tidak ada subscriber.

Start tracking: `HomeScreen` (minta izin notifikasi di Android 13+) → `HomeViewModel.startTracking()` → `TrackingController.start()` (snapshot `TrackingParams` dari Settings) → `TripRepository.startTrip()` → `ContextCompat.startForegroundService(ACTION_START, tripId, interval_ms, max_accuracy_m)` → `LocationTrackingService.startForeground()` → koleksi `LocationTracker.fixes()` → `TrackingStateHolder` diperbarui + `TripRecorder.record()` → Room.

Recovery: `HomeViewModel` menggabungkan `observeActiveTrip()` dengan `TrackingStateHolder.state` lewat `interruptedTripOf()` → dialog → `TrackingController.endInterruptedTrip()` → `TripRecorder.finishInterrupted()`, atau Lanjutkan → `TrackingController.resume()` → state `Active` → service untuk trip yang sama.

Trip otomatis: Activity Recognition → `ActivityTransitionReceiver` → `AutoTripController.onTransitions()` → `TrackingController.start(TripSource.AUTO)` → service; service mengecek `AutoStopPolicy` tiap 30 s → `TripRecorder.finishInterrupted()` → trip pendek dihapus.

Stop tracking: tombol Stop di Home atau notification → `ACTION_STOP` → service membatalkan koleksi → `TripRecorder.finish()` → `TripRepository.finishTrip()` (statistik + visit, satu transaksi) → `stopForeground` + `stopSelf`. Tanpa service berjalan, `TrackingController.stop()` memanggil `TripRecorder.finishInterrupted()`.

Backfill visit: `AppContainer.startVisitBackfill()` → `VisitBackfill.run()` → `TripRepository.tripIdsNeedingVisits()` → per trip `recomputeVisits()` (transaksi: hapus visit → `VisitDetector.detect` → insert → `setVisitDetectionVersion`).

Trip Detail: `TripDetailViewModel` → `TripDetailScreen` (ringkasan → daftar "Tempat singgah" → `TripRouteMap` → `SpeedChart`); tap item visit → `VisitFocusRequest` → `animateCamera` + `bringIntoView`.

Tempat: Tab Tempat → `PlaceDetailScreen` (tap kunjungan → Trip Detail) atau `PlaceEditorScreen` (+ / ikon Ubah / "Simpan sebagai tempat" dari Trip Detail) → `PlaceRepository` → Room. Semua layar yang menampilkan nama visit menggabungkan `observePlaces()` dengan visit lalu memanggil `PlaceMatcher`, sehingga perubahan tempat langsung terlihat.

## External integrations

| Sistem | Arah | Data |
|---|---|---|
| OpenFreeMap (`tiles.openfreemap.org`) | Unduh | Style, tile vektor, dan raster peta, hanya untuk area yang ditampilkan; tile disimpan di ambient cache MapLibre (200 MB). Tidak ada unduhan wilayah massal ([DEC-007](decision-log.md)). Tidak ada koordinat GPS atau data trip yang dikirim (PRD §35). |
