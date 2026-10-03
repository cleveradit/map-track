# Feature Docs — Index

Table of contents for all feature docs. Template and writing conventions: see [ai-context.md §5 Step 3](../ai-context.md#5-documentation-workflow).

For a full understanding, start with [architecture.md](../architecture.md) and [data-model.md](../data-model.md) first.

<!-- ADD ONE ROW PER FEATURE to the table below. Format:
| [file-name](file-name.md) | Short description (1 sentence) | `path/to/file` |
-->

| Doc | Description | Key files |
|---|---|---|
| [home](home.md) | Status GPS, posisi, kecepatan, akurasi, dan alur izin lokasi | `app/src/main/java/com/radityodwiki/maptrack/ui/home/` |
| [tracking](tracking.md) | Start/Stop trip, foreground service, filter dan penyimpanan titik, notification | `app/src/main/java/com/radityodwiki/maptrack/location/` |
| [history](history.md) | Daftar trip terbaru di atas dan hapus trip dengan konfirmasi | `app/src/main/java/com/radityodwiki/maptrack/ui/history/` |
| [trip-detail](trip-detail.md) | Ringkasan statistik trip, tempat singgah, peta rute, dan grafik kecepatan | `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/` |
| [acceleration](acceleration.md) | Uji akselerasi: waktu 0–100/200/300/400/500 m dan 0–100 km/jam dari GPS 1 Hz, riwayat hasil lokal | `app/src/main/java/com/radityodwiki/maptrack/ui/acceleration/` |
| [auto-trip](auto-trip.md) | Trip otomatis opt-in lewat Activity Recognition: mulai saat berkendara/bersepeda, henti setelah diam, hapus trip pendek | `app/src/main/java/com/radityodwiki/maptrack/location/AutoTripController.kt` |
| [settings](settings.md) | Halaman Settings (DataStore): interval & akurasi per trip, satuan, kamera ikuti, hapus cache peta, Tentang | `app/src/main/java/com/radityodwiki/maptrack/ui/settings/` |
| [places](places.md) | Tab Tempat: buat, ubah, hapus tempat bernama; detail kunjungan; nama visit dicocokkan saat ditampilkan | `app/src/main/java/com/radityodwiki/maptrack/ui/places/` |
| [place-detection](place-detection.md) | Deteksi visit (diam ≥ 5 menit dalam 100 m) saat trip selesai dan backfill trip lama | `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/VisitDetector.kt` |
