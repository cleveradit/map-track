# Place Detection

**Status:** Live

## Summary

Saat trip diselesaikan, location point trip itu dikelompokkan menjadi **visit**: tempat pengguna diam minimal 5 menit dalam radius 100 m. Visit adalah data turunan di tabel `visits`, dihitung dalam transaksi yang sama dengan statistik trip, dan dapat dihitung ulang kapan saja dari location point. Trip yang selesai sebelum Fase 2 (atau dengan versi algoritma lama) dihitung ulang di background saat aplikasi dibuka. Visit tampil di Trip Detail; lihat [trip-detail.md](trip-detail.md).

## Quick Reference

| Parameter `PlaceDetectionConfig` | Nilai | Arti |
|---|---|---|
| `VISIT_RADIUS_METERS` | 100.0 | Titik masuk cluster bila jaraknya ke pusat (rata-rata berjalan) ≤ nilai ini |
| `MIN_VISIT_DURATION_MS` | 300 000 (5 menit) | Cluster menjadi visit bila `recorded_at` terakhir − pertama ≥ nilai ini (inklusif) |
| `MERGE_GAP_MS` | 300 000 (5 menit) | Visit berurutan digabung bila jedanya ≤ nilai ini... |
| `MERGE_DISTANCE_METERS` | 100.0 | ...dan jarak antar-pusatnya ≤ nilai ini |
| `DETECTION_VERSION` | 1 | Dinaikkan bila algoritma berubah; trip `completed` dengan versi lebih rendah dihitung ulang |

| Langkah `VisitDetector.detect(points)` | Aturan |
|---|---|
| Input | Semua titik tersimpan satu trip, urut `recorded_at`; < 2 titik → list kosong |
| Cluster | Titik berikutnya ditambahkan selama dalam radius dari pusat cluster; titik yang keluar radius memulai cluster baru |
| Filter durasi | Cluster < `MIN_VISIT_DURATION_MS` dibuang |
| Gabung | Visit berurutan yang memenuhi `MERGE_GAP_MS` dan `MERGE_DISTANCE_METERS` digabung; pusat = rata-rata seluruh titik keduanya; berantai |
| Hasil | `arrived_at` = titik pertama, `departed_at` = titik terakhir, pusat, `point_count` |

| Kapan dihitung | Kode | Catatan |
|---|---|---|
| Trip selesai (Stop di Home, Stop dari notification, Akhiri Trip terputus, gagal start service) | `TripRepository.finishTrip()` → `replaceVisits()` | Transaksi yang sama dengan statistik; titik yang sama |
| Backfill | `MapTrackApplication.onCreate` → `AppContainer.startVisitBackfill()` → `VisitBackfill.run()` → `TripRepository.recomputeVisits()` | Coroutine di `AppContainer.applicationScope` (`SupervisorJob() + Dispatchers.Default`) |

| API `TripRepository` | Perilaku |
|---|---|
| `recomputeVisits(tripId): Boolean` | Transaksi; `false` bila trip tidak ada, belum `completed`, atau `visit_detection_version` ≥ `DETECTION_VERSION` |
| `tripIdsNeedingVisits(): List<String>` | Trip `completed` dengan versi < `DETECTION_VERSION`, terbaru dulu |
| `observeVisits(tripId): Flow<List<Visit>>` | Urut `arrived_at` |

## Gotchas

- `replaceVisits` = hapus semua visit trip → insert hasil baru → `setVisitDetectionVersion`. Penulisan versi tidak mengubah `trips.updated_at` (visit bukan data milik pengguna). `finishTrip` tetap mengisi `updated_at` karena penyelesaian trip itu sendiri.
- Trip < 2 titik tetap ditulis versinya, sehingga tidak dicoba ulang setiap aplikasi dibuka.
- Penggabungan berjalan setelah filter durasi. Satu titik melenceng di tengah singgahan panjang tidak memecah visit, tetapi titik melenceng < 5 menit setelah tiba (atau sebelum pergi) membuat potongan pendek itu terbuang: visit tetap satu, dengan `arrived_at` lebih lambat (atau `departed_at` lebih awal). Lihat [DEC-002](../decision-log.md).
- Backfill: gagal di satu trip dicatat `Log.w` (tag `AppContainer`) dan trip berikutnya tetap diproses; trip yang gagal tetap versi lama dan dicoba lagi saat aplikasi dibuka berikutnya. `CancellationException` dilempar ulang.
- Backfill juga berjalan bila proses dimulai oleh service. Balapan dengan `finishTrip` aman: keduanya transaksi Room, dan `recomputeVisits` memeriksa versi di dalam transaksi.
- `TripRepository.completeTrip` (hanya dipakai test Fase 1) tidak menghitung visit.
- Visit pada trip aktif tidak dihitung dan tidak ditampilkan.
- Nama visit (Fase 3) tidak disimpan di `visits`; dicocokkan dengan tempat saat ditampilkan. Lihat [places.md](places.md).

## Related

- [trip-detail.md](trip-detail.md)
- [tracking.md](tracking.md)
- [data-model.md](../data-model.md)
- [PRD §38 Fase 2](../initiate-file/prd-map-track.md)
