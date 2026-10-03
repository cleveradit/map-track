# History

**Status:** Live

## Summary

Tab History menampilkan semua trip, terbaru di atas, dengan tanggal, rentang jam, jarak, dan durasi. Trip yang masih aktif ditandai `Sedang berjalan` tanpa statistik. Tap item membuka [Trip Detail](trip-detail.md). Trip yang sudah selesai dapat dihapus setelah konfirmasi; titik lokasinya ikut terhapus lewat cascade.

## Quick Reference

| Field `HistoryItem` | Trip selesai | Trip aktif |
|---|---|---|
| `date` | `29 September 2026` (tanggal mulai) | sama |
| `timeRange` | `07:32 - 08:18` | `07:32 - …` |
| `distance` | `formatDistance(distanceMeters)` | `null` |
| `duration` | `formatDuration(endedAt - startedAt)` | `null` |
| Tombol hapus | Ada | Tidak ada |

| State daftar | Tampilan |
|---|---|
| `items == null` | Indikator loading |
| Kosong | `Belum ada perjalanan.` |
| Ada isi | `LazyColumn` dengan `key = tripId` |

## Gotchas

- Trip `source = auto` diberi label "Otomatis" di bawah tanggal ([auto-trip.md](auto-trip.md)).

- Tanggal dan jam memakai zona waktu perangkat saat ditampilkan; trip yang melewati tengah malam ditampilkan dengan tanggal mulai.
- `HistoryViewModel.deleteTrip` tidak menampilkan error; trip aktif ditolak oleh `TripRepository` dan tetap muncul di daftar.

## Related

- [tracking.md](tracking.md)
- [data-model.md](../data-model.md)
- [PRD §18, §34](../initiate-file/prd-map-track.md)
