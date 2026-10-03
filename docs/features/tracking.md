# Tracking Perjalanan

**Status:** Live

## Summary

Pengguna memulai dan menghentikan trip dari Home. Selama trip aktif, `LocationTrackingService` (foreground service bertipe `location`) menerima fix GPS, menyaring dengan `LocationFilter`, dan langsung menyimpan titik yang lolos ke Room. Saat Stop, statistik dihitung dan trip ditandai `completed` dalam satu transaksi.

## Quick Reference

| Langkah | Komponen | Detail |
|---|---|---|
| Start | `TrackingController.start()` | Cek izin presisi → cek Location → `TripRepository.startTrip()` → `AndroidTrackingServiceLauncher.start(tripId)` |
| Rekam | `LocationTrackingService` → `TripRecorder.record()` | Pembanding filter = titik tersimpan terakhir (cache di memori, fallback ke DB) |
| State UI | `TrackingStateHolder` | `Idle` atau `Active(tripId, startedAt, lastFix)` |
| Notification | `TrackingNotification`, `trackingNotificationText()` | Diperbarui setiap fix dan setiap 5 detik; aksi Stop; tap membuka app |
| Stop | `TrackingController.stop()` | Service berjalan → `ACTION_STOP`; tidak berjalan → `TripRecorder.finishInterrupted()` |
| Recovery | `interruptedTripOf()`, dialog di Home | Trip `active` tanpa service → dialog PRD §33 → `endInterruptedTrip()`; `ended_at` = titik terakhir atau `started_at` |
| Selesai | `TripRepository.finishTrip()` | Statistik dari titik tersimpan, `ended_at` = waktu Stop, idempoten |

| `StartTrackingError` | Penyebab | Pesan di Home |
|---|---|---|
| `PERMISSION_MISSING` | Izin lokasi presisi tidak ada | PRD §30 |
| `LOCATION_DISABLED` | Location service mati | PRD §29 |
| `TRIP_ALREADY_ACTIVE` | Ada trip `active` di DB | Minta tekan Stop Tracking |
| `SERVICE_START_FAILED` | `startForegroundService` melempar exception | Trip baru langsung diselesaikan; minta coba lagi |

| Teks notification | Kondisi |
|---|---|
| `Kecepatan: 45 km/h · Durasi: 18 menit` | Fix ≤ 15 detik |
| `Menunggu sinyal GPS… · Durasi: …` | Belum ada fix atau fix basi |
| `Location service tidak aktif · Durasi: …` | Location dimatikan saat tracking |

## Gotchas

- Service memakai `START_NOT_STICKY`. Bila proses mati, trip tetap `active` di DB dan `TrackingStateHolder` kembali `Idle`; saat app dibuka, Home menampilkan dialog recovery dan status `Terputus`.
- `TrackingController.start()` men-set state `Active` sebelum service berjalan agar trip baru tidak terdeteksi terputus ([DEC-001](../decision-log.md)).
- Selama tracking, Home memakai `lastFix` dari `TrackingStateHolder` dan tidak membuka request lokasi kedua.
- Izin notifikasi diminta saat Start di Android 13+. Bila ditolak tracking tetap berjalan, tetapi notification bisa tidak terlihat.
- Fix yang ditolak tidak pernah menjadi pembanding fix berikutnya; setelah GPS jump, fix berikutnya dibandingkan dengan titik tersimpan sebelum jump.

## Related

- [home.md](home.md)
- [data-model.md](../data-model.md)
- [PRD §8, §9, §12, §16, §31–§32](../initiate-file/prd-map-track.md)
