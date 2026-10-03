# Architecture Map

## Summary

Map Track adalah aplikasi Android single-module (`:app`) berbasis Jetpack Compose. Saat ini aplikasi berisi Application class dengan dependency container manual, satu Activity, navigasi dua tab (Home, History), layer data Room untuk trip dan location point, serta logika domain murni untuk filter GPS dan statistik trip. Layer lokasi, service, dan UI fitur ditambahkan oleh tiket Fase 1 berikutnya sesuai [PRD §42](initiate-file/prd-map-track.md#42-struktur-implementasi-awal).

## Components

| Component | Responsibility | Code location | Dependencies |
|---|---|---|---|
| `MapTrackApplication` | Membuat `AppContainer` saat proses dimulai | `app/src/main/java/com/radityodwiki/maptrack/MapTrackApplication.kt` | `AppContainer` |
| `AppContainer` | Wiring dependency manual: `database`, `tripRepository`, `locationTracker`, `tripRecorder`, `trackingStateHolder`, `trackingController` (lazy) | `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt` | `MapTrackDatabase`, `TripRepository` |
| `MainActivity` | Entry point UI, memasang theme dan NavHost | `app/src/main/java/com/radityodwiki/maptrack/MainActivity.kt` | `MapTrackNavHost`, `MapTrackTheme` |
| `MapTrackNavHost` | Scaffold + bottom navigation, route `home`, `history`, dan `trip/{tripId}` | `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt` | `HomeScreen`, `HistoryScreen` |
| `HomeScreen` | Layar Home: alur izin lokasi, status GPS, kecepatan, akurasi, posisi | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt` | `HomeMap` | Peta Home: titik posisi dan kamera mengikuti | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeMap.kt` | `MapLibreMap`, `cameraActionFor` |
| `MapLibreMap` | `MapView` MapLibre di Compose dengan penerusan lifecycle; memuat `MapConfig.STYLE_URL` | `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapLibreMap.kt` | MapLibre Android SDK |
| `MapConfig`, `cameraActionFor` | URL style, kamera default, zoom ikuti; logika kamera murni | `app/src/main/java/com/radityodwiki/maptrack/ui/map/` | — |
| `HomeViewModel` |
| `HomeViewModel` | Menggabungkan izin, fix GPS, ticker 1 detik, trip aktif, dan state aksi menjadi `HomeUiState`; meneruskan Start/Stop ke `TrackingController` | `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeViewModel.kt` | `LocationSource`, `TripRepository`, `TrackingStateHolder`, `TrackingController` |
| `TrackingController` | Validasi dan urutan Start/Stop (PRD §8); mengakhiri trip terputus (PRD §33) | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt` | `LocationSource`, `TripRepository`, `TripRecorder`, `TrackingStateHolder`, `TrackingServiceLauncher` |
| `LocationTrackingService` | Foreground service `location`: koleksi fix, simpan titik, notification, Stop | `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt` | `AppContainer` |
| `TrackingStateHolder` | State in-process `Idle` / `Active(tripId, startedAt, lastFix)` | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingStateHolder.kt` | — |
| `TrackingNotification` | Channel, builder, dan teks notification tracking | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingNotification.kt` | Formatters |
| `TripRecorder` | Filter fix terhadap titik tersimpan terakhir lalu simpan; `finish` (waktu sekarang) dan `finishInterrupted` (titik terakhir) menyelesaikan trip | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripRecorder.kt` | `TripRepository`, `LocationFilter` |
| `LocationTracker` (`LocationSource`) | Fused Location Provider: `fixes()` sebagai Flow, cek izin dan status Location service | `app/src/main/java/com/radityodwiki/maptrack/location/LocationTracker.kt` | Play Services Location |
| `HistoryScreen` | Daftar trip dan dialog konfirmasi hapus | `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryScreen.kt` | `TripDetailScreen`, `SpeedChart`, `TripRouteMap` | Ringkasan trip, peta rute (polyline + marker start/finish), dan grafik kecepatan Canvas | `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/` | `TripDetailViewModel` |
| `TripDetailViewModel` | `observeTrip` + `observePoints` → `TripDetailUiState` (ringkasan, sampel kecepatan, titik rute); `tripId` dari `SavedStateHandle` | `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripDetailViewModel.kt` | `TripRepository` |
| `HistoryViewModel` |
| `HistoryViewModel` | `observeTrips()` → `List<HistoryItem>`; hapus trip | `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryViewModel.kt` | `TripRepository` |
| `MapTrackTheme` | Material 3 theme, dynamic color di Android 12+ | `app/src/main/java/com/radityodwiki/maptrack/ui/theme/Theme.kt` | — |
| `MapTrackDatabase` | Room database `map_track.db`, menyediakan `TripDao` dan `LocationPointDao` | `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` | Room |
| `TripDao`, `LocationPointDao` | Query trip dan location point (Flow untuk observasi) | `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/` | Room |
| `TripRepository` | API data trip untuk layer atas; menegakkan satu trip aktif, larangan hapus trip aktif, abaikan titik ganda; `finishTrip` menghitung statistik dalam transaksi | `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt` | `MapTrackDatabase` |
| Domain model `Trip`, `TripStatus`, `LocationPoint`, `GpsFix`, `LocationPermission` | Model yang dipakai di luar layer data | `app/src/main/java/com/radityodwiki/maptrack/domain/model/` | — |
| `LocationFilter` | Menentukan apakah fix GPS disimpan (PRD §12); mengembalikan `RejectReason` atau `null` | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/LocationFilter.kt` | `GeoDistance`, `TrackingConfig` |
| `TripStatisticsCalculator` | Menghitung jarak, durasi, rata-rata, dan kecepatan maksimum (PRD §16) | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripStatisticsCalculator.kt` | `GeoDistance` |
| `GeoDistance` | Jarak haversine antar-koordinat | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/GeoDistance.kt` | — |
| Formatters | Format kecepatan (termasuk aturan diam/basi), jarak, durasi, akurasi, tanggal, dan jam (locale `id-ID`) untuk UI | `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt` | `TrackingConfig` |
| `TrackingConfig` | Konstanta tracking dan filtering (interval, ambang accuracy, batas GPS jump, stale fix) | `app/src/main/java/com/radityodwiki/maptrack/location/TrackingConfig.kt` | — |

## Main flows

Startup: `MapTrackApplication.onCreate` membuat `AppContainer` → `MainActivity` memasang `MapTrackTheme` → `MapTrackNavHost` dengan start destination `home`.

Lokasi di Home: `HomeScreen` mengoleksi `HomeViewModel.uiState` dengan `collectAsStateWithLifecycle` → `uiState` (`WhileSubscribed(5_000)`) mengaktifkan `LocationTracker.fixes()` hanya bila izin `GRANTED` → `callbackFlow` mendaftarkan `LocationCallback` dan melepasnya saat tidak ada subscriber.

Start tracking: `HomeScreen` (minta izin notifikasi di Android 13+) → `HomeViewModel.startTracking()` → `TrackingController.start()` → `TripRepository.startTrip()` → `ContextCompat.startForegroundService(ACTION_START, tripId)` → `LocationTrackingService.startForeground()` → koleksi `LocationTracker.fixes()` → `TrackingStateHolder` diperbarui + `TripRecorder.record()` → Room.

Recovery: `HomeViewModel` menggabungkan `observeActiveTrip()` dengan `TrackingStateHolder.state` lewat `interruptedTripOf()` → dialog → `TrackingController.endInterruptedTrip()` → `TripRecorder.finishInterrupted()`.

Stop tracking: tombol Stop di Home atau notification → `ACTION_STOP` → service membatalkan koleksi → `TripRecorder.finish()` → `TripRepository.finishTrip()` → `stopForeground` + `stopSelf`. Tanpa service berjalan, `TrackingController.stop()` memanggil `TripRecorder.finishInterrupted()`.

## External integrations

| Sistem | Arah | Data |
|---|---|---|
| OpenFreeMap (`tiles.openfreemap.org`) | Unduh | Style, tile vektor, dan raster peta. Tidak ada koordinat GPS atau data trip yang dikirim (PRD §35). |
