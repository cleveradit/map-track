# Trip Detail

**Status:** Live

## Summary

Halaman detail dibuka dari item History lewat route `trip/{tripId}`. Isinya ringkasan statistik trip, jumlah titik, peta rute dengan marker start dan finish, serta grafik kecepatan terhadap waktu yang digambar dengan Compose Canvas. Untuk trip yang masih aktif, jumlah titik dan grafik diperbarui live, sedangkan statistik akhir tampil `—`.

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

| `TripDetailUiState` | Tampilan |
|---|---|
| `Loading` | Indikator loading |
| `NotFound` | `Perjalanan tidak ditemukan.` |
| `Loaded` | Ringkasan, peta rute, grafik |

| Peta rute (`TripRouteMap`) | Aturan |
|---|---|
| Garis | `LineLayer` dari semua titik tersimpan, urut `recorded_at`; hanya bila ≥ 2 titik |
| Start | Lingkaran hijau `#1E8E3E` di titik pertama |
| Finish | Lingkaran merah `#D93025` di titik terakhir; hanya trip `completed` |
| Kamera | `routeCamera()`: bounding box + padding 48 px; sebaran < 0.0005° → center zoom 16; diatur sekali |
| < 2 titik | Teks `Data lokasi tidak cukup untuk menggambar rute.` |

## Gotchas

- `TripDetailScreen` memakai `Scaffold` sendiri di dalam `Scaffold` utama; keduanya `WindowInsets(0)` agar inset status bar tidak dihitung dua kali.
- Grafik membaca seluruh titik trip lewat `observePoints` setiap ada titik baru; batas 500 sampel hanya berlaku untuk penggambaran, bukan untuk query.

- Kamera rute hanya diatur sekali saat rute pertama tersedia, sehingga trip aktif yang rutenya bertambah tidak menarik kamera kembali ketika pengguna menggeser peta.
- Sumber GeoJSON dikosongkan dengan `FeatureCollection` kosong, bukan `null`.

## Related

- [history.md](history.md)
- [PRD §19, §21](../initiate-file/prd-map-track.md)
