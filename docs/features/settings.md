# Settings

**Status:** Live

## Summary

Halaman Settings dibuka dari ikon di samping judul Home (route `settings`). Pengguna mengatur interval lokasi dan ambang akurasi tracking (berlaku mulai trip berikutnya), satuan metrik/imperial, dan apakah kamera Home mengikuti posisi; menghapus cache peta; serta melihat versi, atribusi peta, dan pernyataan privasi. Nilai disimpan di DataStore Preferences dan bertahan setelah aplikasi ditutup.

## Quick Reference

| Key DataStore | Pilihan | Default | Dipakai oleh |
|---|---|---|---|
| `tracking_interval_ms` | `TrackingConfig.INTERVAL_OPTIONS_MS`: 3 000, 5 000, 10 000, 30 000 | 5 000 | Snapshot `TrackingParams` saat Start → request lokasi service |
| `accuracy_threshold_m` | `TrackingConfig.ACCURACY_OPTIONS_METERS`: 20, 30, 50, 100 | 50 | Snapshot `TrackingParams` saat Start → `LocationFilter` |
| `distance_unit` | `metric` / `imperial` | `metric` | Semua tampilan jarak & kecepatan, notification |
| `map_follow_location` | `true` / `false` | `true` | `HomeMap(followByDefault)` |

| API | Perilaku |
|---|---|
| `SettingsRepository.settings: Flow<AppSettings>` | Nilai tersimpan di luar pilihan dibaca sebagai default |
| `setTrackingInterval` / `setAccuracyThreshold` | Nilai di luar pilihan → `IllegalArgumentException` (ViewModel mengabaikannya lebih dulu) |
| `TrackingController.start()` | Membaca `settingsRepository.current().trackingParams()` sebelum membuat trip; dikirim ke service sebagai extra `interval_ms`, `max_accuracy_m` |
| `MapCache.clear` | Hapus ambient cache MapLibre; hasil ditampilkan sebagai dialog |

| Satuan imperial | Aturan |
|---|---|
| Kecepatan | mph = m/s × 2.236936; < 1 km/h tetap `0 mph` |
| Jarak | < 0.1 mi → feet dibulatkan (`328 ft`); selain itu `x.x mi` |
| Akurasi | `± n ft` |
| Grafik kecepatan | Sampel dan sumbu Y dalam mph; judul `Histori kecepatan (mph)` |

## Gotchas

- Parameter tracking adalah snapshot per Start: service memakai extra intent, bukan membaca Settings. Mengubah interval saat trip aktif tidak memengaruhi trip itu. Satuan tampilan di notification justru mengikuti Settings secara langsung.
- Interval minimum update Fused Location sama dengan interval yang dipilih (`TrackingConfig.MIN_UPDATE_INTERVAL_MS` dihapus) agar pilihan 3 detik benar-benar berlaku.
- Request lokasi Home saat tidak tracking dan "Pakai lokasi saat ini" tetap memakai `LocationRequestSpec.DEFAULT` (5 s, high accuracy), bukan setting interval.
- Kamera ikuti nonaktif: kamera tetap zoom ke fix pertama sekali, lalu berhenti mengikuti; tombol "Ikuti posisi" tetap muncul.
- ViewModel menerima `settings: Flow<AppSettings>` dengan default `flowOf(AppSettings.DEFAULT)` sehingga test tidak memerlukan DataStore.
- Versi aplikasi dibaca dari `PackageManager` karena `buildConfig` tidak diaktifkan.

## Related

- [home.md](home.md)
- [tracking.md](tracking.md)
- [PRD §26, §38 Fase 4](../initiate-file/prd-map-track.md)
