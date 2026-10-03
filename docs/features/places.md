# Saved Places

**Status:** Live

## Summary

Pengguna membuat tempat bernama (titik + radius) di tab **Tempat**. Visit diberi nama tempat saat ditampilkan: pusat visit berada dalam radius tempat, dan bila beberapa tempat cocok dipilih pusat terdekat. Nama tidak disimpan di tabel `visits`, sehingga mengubah atau menghapus tempat langsung berlaku ke semua visit lama. Tab Tempat menampilkan daftar, form buat/ubah, dan detail tempat dengan total kunjungan.

## Quick Reference

| Parameter `PlaceConfig` | Nilai |
|---|---|
| `DEFAULT_RADIUS_METERS` | 100.0 |
| `MIN_RADIUS_METERS` / `MAX_RADIUS_METERS` | 50.0 / 1 000.0 (inklusif) |
| `RADIUS_STEP_METERS` | 10.0 (langkah slider) |
| `NAME_MAX_LENGTH` | 50, dihitung per code point setelah trim |
| `CURRENT_LOCATION_TIMEOUT_MS` | 30 000 |

| Route | Layar | Argumen |
|---|---|---|
| `places` | `PlacesScreen` (tab) | — |
| `place/{placeId}` | `PlaceDetailScreen` | `placeId` |
| `place-editor?placeId=&lat=&lng=` | `PlaceEditorScreen` | Semua opsional, `String`: baru / dari visit (`lat`, `lng`) / ubah (`placeId`) |

| Aturan | Kode | Perilaku |
|---|---|---|
| Validasi | `PlaceValidator`, ditegakkan di `PlaceRepository` | Nama trim 1–50; radius 50–1 000; latitude −90..90, longitude −180..180; gagal → `InvalidPlaceException` |
| Cocok | `PlaceMatcher.match` | Haversine pusat visit ↔ pusat tempat ≤ `radius_meters`; terdekat menang; jarak sama → `id` terkecil |
| Kelompok | `PlaceMatcher.group` | Tiap visit hanya masuk ke satu tempat (yang menang `match`); dipakai Tab Tempat dan Detail Tempat |
| Hapus | `PlaceRepository.deletePlace` | Hanya baris `places`; trip, titik, visit tidak tersentuh |

| Layar | Isi |
|---|---|
| Tab Tempat | Nama, `Radius <jarak>`, `<n> kunjungan · terakhir <tanggal>` atau `Belum ada kunjungan`; urut nama tanpa membedakan huruf besar/kecil; tombol + |
| Form | Peta dengan pin tetap di tengah dan lingkaran radius; "Pakai lokasi saat ini"; slider radius; nama dengan penghitung `n/50`; Simpan di top bar |
| Detail | Peta statis (lingkaran + titik pusat), Radius, Total kunjungan, Total durasi, daftar kunjungan terbaru dulu (tap → Trip Detail); ikon Ubah dan Hapus (dengan konfirmasi) |

## Gotchas

- Titik form hanya diambil dari gerakan kamera yang dimulai gesture (`REASON_API_GESTURE`). Kamera default (Indonesia) dan kamera yang dipindah oleh kode tidak pernah mengisi titik; titik dari kode (tempat lama, visit, lokasi saat ini) diset di ViewModel lalu dikirim ke peta sebagai `CameraRequest` (bukan data class, agar permintaan sama tetap dijalankan). Lihat [DEC-005](../decision-log.md).
- Tempat baru tanpa titik tidak bisa disimpan; petunjuk "Geser peta…" tampil. Form dari visit atau tempat lama bisa disimpan walaupun peta tidak termuat (offline).
- "Pakai lokasi saat ini": izin belum ada → dialog penjelasan dulu (Rule 7), lalu dialog sistem. Fix pertama dengan accuracy ≤ `TrackingConfig.MAX_ACCURACY_METERS` dipakai; tanpa fix dalam 30 detik → pesan.
- Lingkaran digambar dengan `circleRing` pada sphere yang sama dengan `GeoDistance` (`EARTH_RADIUS_METERS`), sehingga lingkaran di peta sama dengan radius pencocokan.
- Visit di area tumpang tindih hanya dihitung untuk tempat dengan pusat terdekat, termasuk total di Detail Tempat dan jumlah di Tab Tempat. Lihat [DEC-006](../decision-log.md).
- Pencocokan berjalan di Kotlin setiap kali tempat atau visit berubah: semua visit (`observeAllVisits`) × semua tempat.
- Koordinat di route dikirim sebagai `String` agar presisi `Double` tidak hilang (`NavType.FloatType` hanya 32-bit).

## Related

- [place-detection.md](place-detection.md)
- [trip-detail.md](trip-detail.md)
- [data-model.md](../data-model.md)
- [PRD §38 Fase 3](../initiate-file/prd-map-track.md)
