# Uji Akselerasi

**Status:** Live

## Summary

Halaman Uji Akselerasi (tombol di Home, route `acceleration`) mengukur berapa detik dari diam untuk menempuh 100, 200, 300, 400, dan 500 m serta untuk mencapai 100 km/jam. GPS diminta 1 kali per detik hanya selama halaman terbuka, tanpa membuat trip. Hasil yang selesai disimpan lokal di tabel `acceleration_runs` dan dapat dihapus.

## Quick Reference

| Parameter `AccelerationConfig` | Nilai |
|---|---|
| `GPS_INTERVAL_MS` | 1 000 |
| `MAX_ACCURACY_METERS` | 20 m |
| `STILL_SPEED_MPS` / `READY_STILL_MS` | 0,5 m/s / 2 detik |
| `START_SPEED_MPS` | 1,0 m/s |
| `MAX_FIX_GAP_MS` | 3 detik |
| `STOP_BELOW_START_MS` | 2 detik |
| `MAX_RUN_MS` | 60 detik |
| Target | 100, 200, 300, 400, 500 m; 100 km/jam (`TARGET_SPEED_MPS`) |

| Fase `AccelerationPhase` | Tampilan | Pindah ke |
|---|---|---|
| `WAITING_GPS` | Menunggu sinyal GPS akurat… | `WAITING_STILL` saat accuracy ≤ 20 m |
| `WAITING_STILL` | Berhenti total dulu… | `READY` setelah < 0,5 m/s selama ≥ 2 detik |
| `READY` | Siap — mulai berakselerasi | `RUNNING` saat kecepatan ≥ 1,0 m/s |
| `RUNNING` | Mengukur… + tombol Berhenti | `FINISHED` (semua target, berhenti 2 detik, Berhenti, 60 detik) atau `INVALID` (jeda fix > 3 detik) |
| `FINISHED` / `INVALID` | Selesai / Gagal: sinyal GPS terputus + tombol Ulangi | `WAITING_GPS` |

| Perhitungan | Cara |
|---|---|
| Titik nol | Waktu fix diam terakhir sebelum mulai |
| Jarak | Integral trapesium kecepatan GPS (Doppler), bukan selisih posisi |
| Waktu target jarak | Solusi `d = v₀t + ½at²` dengan percepatan konstan di antara dua fix |
| Waktu 0–100 km/jam | `t = (27,78 − v₀) / a` di segmen yang melewati 100 km/jam |
| Simpan | Sekali per run, hanya `FINISHED` dengan minimal satu target (`AccelerationState.toRun()`) |

## Gotchas

- Target selalu meter dan km/jam; hanya kecepatan langsung dan kecepatan saat lewat target yang mengikuti setting satuan.
- Fix tanpa kecepatan saat mengukur dilewati; bila fix berkecepatan berikutnya datang > 3 detik setelah yang terakhir, run gagal dan tidak disimpan.
- Galat titik nol ≤ 1 interval GPS (1 detik) karena mulai dihitung dari fix diam terakhir.
- Layar dijaga menyala (`keepScreenOn`) selama halaman terbuka; GPS berhenti 5 detik setelah halaman ditinggalkan (`WhileSubscribed(5_000)`).
- Di emulator, `adb emu geo fix` menambah latensi sehingga hasil lebih lambat dari ideal; ketepatan rumus dibuktikan `AccelerationMeterTest`.

## Related

- [home.md](home.md)
- [data-model.md](../data-model.md)
- [PRD §38 Fase 5 — Uji akselerasi](../initiate-file/prd-map-track.md)
