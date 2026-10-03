# Current Session

## Active Ticket

— (Fase 5 selesai: TICKET-027 s.d. TICKET-032 `DONE`, di `Ticket-Implemented/`. Fase 6 belum dimulai.)

## Progress

- Fase 1–3 sudah di-commit dan di-push sebelumnya.
- Fase 4 — Offline Map & Settings (TICKET-023–026):
  - Verifikasi lisensi: Terms of Service OpenFreeMap melarang pengambilan data otomatis tanpa izin, sehingga unduhan wilayah tidak dibuat. User memilih opsi B: cache tile MapLibre 200 MB + Hapus cache peta (PRD v2.4, [DEC-007](../decision-log.md)).
  - Settings (DataStore): interval & ambang akurasi sebagai snapshot per trip, satuan metrik/imperial di semua tampilan, kamera ikuti posisi, bagian Tentang.
- Fase 5 — Automatic Trip & Tracking Improvements (TICKET-027–032):
  - Room versi 4: kolom `trips.source` (`manual`/`auto`), label "Otomatis".
  - Peredam jitter titik jangkar + syarat kecepatan < 0,5 m/s (PRD v2.5, [DEC-008](../decision-log.md)).
  - Lanjutkan trip terputus ≤ 60 menit.
  - Penghemat baterai: request 30 s balanced setelah diam 2 menit.
  - Trip otomatis opt-in (Activity Recognition Transition API), henti setelah diam 5 menit, trip pendek dihapus lewat semua jalur selesai ([DEC-009](../decision-log.md)), reconcile saat start proses/boot.
- Verifikasi terakhir: `testDebugUnitTest` 217/217, `assembleDebug` sukses, lint 0 error / 1 warning (`OldTargetApi`).
- Diskusi backend Fase 6 dengan user: PRD saat ini (Laravel + PostgreSQL + Redis + Reverb di VPS) dibandingkan dengan Firebase Spark (gratis, tanpa kartu kredit, tanpa Cloud Functions). User mengutamakan layanan gratis dan aplikasi yang tetap berjalan offline.

## Pending / Blockers

- **Fase 6 menunggu keputusan backend dari user.** Bila Firebase: PRD v2.6 harus merevisi §4, ai-context §1, prinsip "Free-first", dan §38 Fase 6–9 (remote logout soft, rate limit login bawaan Firebase, sesi di penyimpanan privat aplikasi, logika di aplikasi + Security Rules, Realtime Database untuk live sharing). Proposal revisi direview user sebelum diterapkan.
- Pertanyaan Bertahap yang belum dijawab: perlukah uji manual fondasi lokal (Fase 1–5) di HP sebelum fitur jaringan, atau cukup test otomatis?
- Checklist uji manual terkumpul untuk pengujian akhir: [Fase 1](Ticket-Implemented/TICKET-011-phase1-closeout.md#6a-checklist-uji-manual-fase-1-gabungan-untuk-user), [Fase 2](Ticket-Implemented/TICKET-016-phase2-closeout.md#6a-checklist-uji-manual-fase-2-untuk-user), [Fase 3](Ticket-Implemented/TICKET-022-phase3-closeout.md#6a-checklist-uji-manual-fase-3-untuk-user), [Fase 4](Ticket-Implemented/TICKET-026-phase4-closeout.md#6a-checklist-uji-manual-fase-4-untuk-user), [Fase 5](Ticket-Implemented/TICKET-032-phase5-closeout.md#6a-checklist-uji-manual-fase-5-untuk-user).
- Build lokal memakai `--no-build-cache` karena entri cache Gradle `kspDebugKotlin` rusak; `./gradlew clean` atau hapus `~/.gradle/caches/build-cache-1` bila error muncul di run biasa.

## Next Steps

1. User memutuskan backend Fase 6 (Firebase Spark atau Laravel + VPS) dan menjawab pertanyaan Bertahap.
2. Tulis proposal revisi PRD (v2.6) untuk direview user; setelah disetujui, ubah backlog Fase 6 menjadi tiket mulai TICKET-033.
3. Setelah Fase 9 selesai: user menjalankan semua checklist uji manual di HP; masalah dicatat di backlog.
