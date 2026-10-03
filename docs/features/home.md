# Home

**Status:** Live

## Summary

Home menampilkan peta MapLibre dengan titik posisi pengguna, status GPS, posisi, kecepatan saat ini, akurasi lokasi, status tracking, durasi trip aktif, serta tombol Start/Stop Tracking. Izin lokasi diminta hanya setelah pengguna membaca penjelasan dan menekan tombol. Update lokasi hanya berjalan selama Home terlihat di layar.

## Quick Reference

| Elemen | Sumber | Aturan |
|---|---|---|
| Kecepatan | `GpsFix.speedMps` | `formatCurrentSpeed`: `— km/h` bila tidak ada speed atau fix > 15 s; `0 km/h` bila < 1 km/h |
| Akurasi | `GpsFix.accuracyMeters` | `formatAccuracy`: `± N meter` atau `—` |
| GPS | `gpsStatusOf` | `NO_PERMISSION` → `LOCATION_DISABLED` → `SEARCHING` → `ACTIVE` |
| Posisi | `GpsFix.latitude/longitude` | 6 desimal |
| Peta | `HomeMap` → `MapLibreMap` | Style OpenFreeMap `liberty`; titik biru (`CircleLayer`) di fix terakhir |
| Kamera | `cameraActionFor` | Fix pertama: zoom 16; berikutnya mengikuti; berhenti mengikuti saat pengguna menggeser peta; tombol `Ikuti posisi` mengaktifkan lagi |
| Tracking | `HomeUiState.activeTrip` (trip `active` di DB) | `● Aktif` + durasi + Stop, atau `Tidak aktif` + Start |
| Trip terputus | `HomeUiState.interruptedTrip` | Dialog tak bisa ditutup (jam mulai, data terakhir, `Akhiri Trip`) + status `Terputus` |
| Pesan error Start | `HomeUiState.startError` | Lihat [tracking.md](tracking.md) |

| State permission | Tampilan |
|---|---|
| `NOT_REQUESTED` | Kartu penjelasan (PRD §28) + tombol "Izinkan lokasi" |
| `DENIED` | Pesan PRD §30 + "Coba lagi" dan "Buka Pengaturan" |
| `APPROXIMATE_ONLY` | Pesan meminta lokasi akurat + "Coba lagi" dan "Buka Pengaturan" |
| `GRANTED`, Location mati | Pesan PRD §29 + "Aktifkan Location" |

## Gotchas

- `NOT_REQUESTED` tidak dapat dibedakan dari `DENIED` oleh Android; `HomeViewModel` menandai `DENIED` hanya setelah dialog izin pernah ditampilkan dalam sesi ini. Setelah proses dimulai ulang, pengguna yang pernah menolak kembali melihat kartu penjelasan.
- Update lokasi berhenti 5 detik setelah Home tidak terlihat (`SharingStarted.WhileSubscribed(5_000)`), bukan seketika.
- Status tracking diambil dari database; trip `active` yang tidak dimiliki service ditampilkan sebagai terputus.
- Status Location service dibaca ulang setiap detik oleh ticker ViewModel, bukan lewat broadcast.

- Marker posisi digambar dari `GpsFix` Home lewat `GeoJsonSource`, bukan `LocationComponent` MapLibre, agar GPS tetap satu-satunya sumber koordinat dan tidak ada request lokasi tambahan.
- Tanpa internet style tidak termuat dan peta kosong; status, kecepatan, dan tracking tidak terpengaruh.
- `MapLibreMap` meminta parent tidak mencegat gesture (`requestDisallowInterceptTouchEvent`) karena Home berada di `verticalScroll`.

## Related

- [architecture.md](../architecture.md)
- [PRD §7.1, §14, §28–§30](../initiate-file/prd-map-track.md)
