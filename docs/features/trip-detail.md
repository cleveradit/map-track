# Trip Detail

**Status:** Live

## Summary

Halaman detail dibuka dari item History lewat route `trip/{tripId}`. Isinya ringkasan statistik trip, jumlah titik, daftar tempat singgah (visit), peta rute dengan marker start, finish, dan visit, serta grafik kecepatan terhadap waktu yang digambar dengan Compose Canvas. Untuk trip yang masih aktif, jumlah titik dan grafik diperbarui live, sedangkan statistik akhir tampil `—` dan visit tidak ditampilkan.

## Quick Reference

| Field `TripSummary` | Sumber | Trip aktif |
|---|---|---|
| `date`, `startTime` | `startedAt` | tampil |
| `endTime`, `duration` | `endedAt` | `—` |
| `distance` | `distanceMeters` | `—` |
| `averageSpeed`, `maxSpeed` | `averageSpeedMps`, `maxSpeedMps` (`formatSpeedKmh`) | `—`; `maxSpeed` juga `—` bila null |

| Grafik | Aturan |
|---|---|
| Data | `speedSeries()`: `location_points.speed` × 3.6, titik tanpa speed dilewati |
| Sumbu X | Waktu sejak `startedAt`; label `0 menit` dan durasi sampel terakhir |
| Sumbu Y | 0 sampai `chartMaxKmh()` (kelipatan 10 di atas maksimum, minimal 10) |
| Batas sampel | 500, diambil merata dengan sampel pertama dan terakhir dipertahankan |
| < 2 sampel | Teks `Belum ada data kecepatan.` |

| Tempat singgah (`VisitItem`) | Aturan |
|---|---|
| Sumber | `TripRepository.observeVisits(tripId)` (lihat [place-detection.md](place-detection.md)); kosong bila trip aktif |
| `timeRange` | `"HH:mm - HH:mm"` dari `arrivedAt`/`departedAt`, zona perangkat |
| `placeName` | `PlaceMatcher.match(pusat visit, observePlaces())?.name` (lihat [places.md](places.md)); berubah langsung saat tempat diubah/dihapus |
| Label | `<placeName atau "Tempat singgah"> · <formatDuration(durationMs)>` (string `visit_label`) |
| Simpan sebagai tempat | `TextButton` hanya pada visit tanpa nama → `Routes.newPlaceAt(pusat visit)` |
| Posisi | Di bawah ringkasan, di atas peta; seluruh bagian disembunyikan bila tidak ada visit |
| Tap item | Peta dibawa ke viewport (`BringIntoViewRequester`) dan kamera beranimasi ke visit |

| `TripDetailUiState` | Tampilan |
|---|---|
| `Loading` | Indikator loading |
| `NotFound` | `Perjalanan tidak ditemukan.` |
| `Loaded` | Ringkasan (label "Otomatis" untuk trip `auto`), tempat singgah (`visits`), peta rute, grafik |

| Peta rute (`TripRouteMap`) | Aturan |
|---|---|
| Garis | `LineLayer` dari semua titik tersimpan, urut `recorded_at`; hanya bila ≥ 2 titik |
| Start | Lingkaran hijau `#1E8E3E` di titik pertama |
| Finish | Lingkaran merah `#D93025` di titik terakhir; hanya trip `completed` |
| Visit | Source `route-visits`: `route-visit-halo` (ungu `#9334E6`, radius 14, opacity 0.25) + `route-visit-dot` (ungu, radius 6, stroke putih 2); ditambahkan sebelum layer start/finish sehingga tergambar di bawahnya |
| Kamera | `routeCamera()`: bounding box + padding 48 px; sebaran < 0.0005° → center zoom 16; diatur sekali |
| Fokus visit | Setiap `VisitFocusRequest` baru → `animateCamera` ke pusat visit, zoom `MapConfig.FOLLOW_ZOOM` |
| < 2 titik | Teks `Data lokasi tidak cukup untuk menggambar rute.` |

## Gotchas

- `TripDetailScreen` memakai `Scaffold` sendiri di dalam `Scaffold` utama; keduanya `WindowInsets(0)` agar inset status bar tidak dihitung dua kali.
- Grafik membaca seluruh titik trip lewat `observePoints` setiap ada titik baru; batas 500 sampel hanya berlaku untuk penggambaran, bukan untuk query.

- Kamera rute hanya diatur sekali saat rute pertama tersedia, sehingga trip aktif yang rutenya bertambah tidak menarik kamera kembali ketika pengguna menggeser peta.
- Sumber GeoJSON dikosongkan dengan `FeatureCollection` kosong, bukan `null`.
- `VisitFocusRequest` sengaja bukan data class: tap berulang pada item yang sama membuat objek baru sehingga `LaunchedEffect` jalan lagi dan kamera memusat ulang. Fokus yang dipilih sebelum style termuat dijalankan setelah kamera awal, dan menandai kamera awal sudah diatur.

## Related

- [history.md](history.md)
- [place-detection.md](place-detection.md)
- [places.md](places.md)
- [PRD §19, §21, §38 Fase 2](../initiate-file/prd-map-track.md)
