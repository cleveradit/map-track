# Trip Otomatis

**Status:** Live

## Summary

Fitur opt-in (default nonaktif) di Settings. Setelah pengguna memberi izin aktivitas fisik dan lokasi sepanjang waktu, aplikasi berlangganan Activity Recognition Transition API. Saat transisi masuk ke berkendara atau bersepeda (opsional berjalan kaki/lari) diterima dan tidak ada trip aktif, trip `source = auto` dimulai tanpa membuka aplikasi. Trip otomatis berhenti sendiri setelah diam 5 menit, dan dihapus bila terlalu pendek.

## Quick Reference

| Parameter `AutoTripConfig` | Nilai | Arti |
|---|---|---|
| `AUTO_STOP_STILL_MS` | 5 menit | Lama diam sebelum trip otomatis berhenti |
| `AUTO_STOP_RADIUS_METERS` | 100 m | Semua titik 5 menit terakhir dalam radius ini dari titik pertama jendela = diam |
| `AUTO_STOP_CHECK_MS` | 30 s | Interval pengecekan henti di service |
| `MIN_AUTO_TRIP_DISTANCE_METERS` / `MIN_AUTO_TRIP_DURATION_MS` | 300 m / 2 menit | Di bawah salah satunya → trip otomatis dihapus setelah berhenti |

| Kejadian | Komponen | Perilaku |
|---|---|---|
| Aktifkan | `SettingsViewModel.requestAutoTrip` → `AutoTripController.enable` | Lokasi presisi wajib ada; dialog penjelasan → `ACTIVITY_RECOGNITION`; dialog penjelasan → `ACCESS_BACKGROUND_LOCATION`; berlangganan; gagal → tetap nonaktif |
| Transisi | `ActivityTransitionReceiver` → `onTransitions` | `IN_VEHICLE`/`ON_BICYCLE` (+ `WALKING`/`RUNNING` bila opsi) `ENTER` → `TrackingController.start(TripSource.AUTO)`; `STILL ENTER`/`EXIT` → `StillnessHolder.stillSince` |
| Henti | `LocationTrackingService` + `AutoStopPolicy` | Hanya trip `auto`; diam (titik atau `STILL` ≥ 5 menit) → `finishInterrupted` (`ended_at` = titik terakhir) |
| Trip pendek | `TripRecorder.finish`/`finishInterrupted` | `Trip.isTooShortAutoTrip()` → `deleteTrip`, tanpa notifikasi, lewat jalur selesai mana pun |
| Matikan | `AutoTripController.disable` | Berhenti berlangganan; trip otomatis yang berjalan tetap berjalan |
| Start proses / reboot / update | `MapTrackApplication.onCreate`, `BootReceiver` → `reconcile` | Izin lengkap → berlangganan ulang; izin dicabut → nonaktif + `auto_trip_revoked_notice` |

| Tampilan | Isi |
|---|---|
| Notification | Judul "Perjalanan otomatis" |
| History, Trip Detail | Label "Otomatis" |
| Settings | Switch Trip otomatis, switch Termasuk berjalan kaki, keterangan izin dicabut |

## Gotchas

- `ACCESS_BACKGROUND_LOCATION` hanya diminta saat trip otomatis diaktifkan (Rule 7); tracking manual dan Lanjutkan tidak membutuhkannya. Di Android 11+ permintaan ini membuka halaman izin sistem ("Izinkan sepanjang waktu").
- Memulai foreground service bertipe `location` dari background hanya diizinkan karena aplikasi menerima event activity transition dan memegang izin lokasi background; tanpa salah satunya, start diabaikan oleh `AutoTripController` sebelum mencoba.
- `PendingIntent` transisi harus `FLAG_MUTABLE` karena Play services mengisi hasilnya.
- Langganan transisi hilang setelah reboot dan update aplikasi; `BootReceiver` dan `MapTrackApplication.onCreate` memanggil `reconcile()` (idempoten).
- `StillnessHolder` hanya di memori proses: bila proses mati, henti otomatis mengandalkan titik GPS sampai `STILL ENTER` berikutnya.
- Trip pendek dihapus juga bila trip otomatis dihentikan manual atau diakhiri dari dialog trip terputus.
- Saat izin dicabut dari pengaturan sistem, Android menghentikan proses; keterangan baru muncul setelah aplikasi dibuka lagi.

## Related

- [tracking.md](tracking.md)
- [settings.md](settings.md)
- [PRD §38 Fase 5](../initiate-file/prd-map-track.md)
