# Backlog

This document lists all **unimplemented ideas, features, enhancements, and blocked items** for the project.

Before starting any item here, it **MUST** be converted into a detailed implementation plan in `docs/planning/` first!

---

## Format

Each backlog item follows this format:

```markdown
## [Feature/Idea Title]

**Status:** `OPEN` | `BLOCKED` *(blocker reason)*

**Summary:** 2-4 sentences describing what is wanted.

**Open questions (resolve during planning):**
1. [Question 1]
2. [Question 2]
3. [Question 3]
```

---

<!-- ADD BACKLOG ITEMS BELOW -->


## Fase 3 — Saved Places

**Status:** `OPEN`

**Summary:** Pengguna membuat, mengubah, dan menghapus tempat bernama (tabel `places` dengan UUID + `updated_at`, migrasi 2 → 3, `PlaceConfig`). Nama visit dicocokkan saat ditampilkan (pusat visit dalam radius; konflik → pusat terdekat), bukan disimpan. Tab navigasi baru **Tempat** dengan daftar, form buat/ubah (pin di tengah peta + slider radius), detail tempat, dan aksi "Simpan sebagai tempat" dari visit. Detail: PRD §38 Fase 3.

**Open questions (resolve during planning):**
1. Pencocokan visit ↔ tempat dilakukan di query SQL atau di Kotlin (use case)? Pertimbangkan performa Detail Tempat untuk banyak trip.
2. Komponen peta untuk pemilih titik + lingkaran radius: reuse komponen MapLibre Home/Trip Detail atau komponen baru?
3. Jumlah kunjungan & kunjungan terakhir di daftar tempat dihitung on-the-fly atau di-cache?

## Fase 4 — Offline Map & Settings

**Status:** `BLOCKED` *(menunggu Fase 3 selesai)*

**Summary:** Unduh wilayah peta via MapLibre `OfflineManager` (zoom 10–14, maks. 20 000 tile, default Wi-Fi saja) dengan halaman Offline Maps. Halaman Settings berbasis DataStore Preferences: interval tracking, ambang accuracy, satuan metrik/imperial, kamera mengikuti posisi, unduh hanya Wi-Fi, plus bagian Tentang. Setting tracking dibaca saat Start dan berlaku mulai trip berikutnya. Detail: PRD §38 Fase 4.

**Open questions (resolve during planning):**
1. Lisensi OpenFreeMap untuk unduhan offline — wajib diverifikasi di tiket pertama; bila tidak boleh, sumber tile pengganti apa (revisi §22)?
2. Cara `TrackingConfig` menerima nilai dari DataStore tanpa menjadikan interval/accuracy magic number (snapshot config per trip?).
3. Melanjutkan unduhan yang terputus saat halaman Offline Maps dibuka lagi: perilaku persis `OfflineRegion` MapLibre versi terpasang perlu diuji.

## Fase 5 — Automatic Trip & Tracking Improvements

**Status:** `BLOCKED` *(menunggu Fase 4 selesai)*

**Summary:** Trip otomatis opt-in via Activity Recognition Transition API (`IN_VEHICLE`/`ON_BICYCLE`, opsional berjalan kaki) dengan `AutoTripConfig`, kolom `trips.source` (migrasi 3 → 4), dan permission `ACTIVITY_RECOGNITION` + `ACCESS_BACKGROUND_LOCATION` hanya saat opsi diaktifkan. Termasuk tombol **Lanjutkan** untuk trip terputus ≤ 60 menit, peredam jitter dengan titik jangkar pada perhitungan jarak, dan penghemat baterai (interval 30 s saat diam). Detail: PRD §38 Fase 5.

**Open questions (resolve during planning):**
1. Uji start foreground service dari background lewat event activity transition di targetSdk 36 dan HP vendor agresif (Xiaomi/Oppo/Vivo).
2. Perubahan alur §8.1/DEC-001 untuk "Lanjutkan" agar invarian state `Active` sebelum service tetap terjaga.
3. Cara menguji peredam jitter (rekaman diam 10 menit) secara otomatis di JVM — perlu fixture data GPS?
4. Urutan tiket: improvement tracking (jitter, baterai, lanjutkan) sebelum trip otomatis?

## Fase 6 — Account

**Status:** `BLOCKED` *(menunggu Fase 5 selesai)*

**Summary:** Backend pertama: Laravel + PostgreSQL + Redis (+ Reverb untuk Fase 9) di satu VPS dengan Docker Compose dan HTTPS, API `/api/v1`, Sanctum token per perangkat. Client: register/login/logout, verifikasi email, lupa password, device management, hapus akun, halaman Akun dari Settings; token di DataStore terenkripsi Keystore. Login opsional — tanpa akun aplikasi tetap seperti Fase 1–5, dan tidak ada data lokasi dikirim. Detail: PRD §38 Fase 6.

**Open questions (resolve during planning):**
1. Versi Laravel/PostgreSQL/Redis dan penyedia VPS + region (diputuskan di tiket pertama Fase 6).
2. Struktur repo backend: repo terpisah atau folder di monorepo ini? Dokumentasi backend di `docs/` yang sama?
3. Domain dan penyedia email transaksional untuk verifikasi & reset password.
4. Konfigurasi rilis Android (signing, R8) yang tertunda dari TICKET-011 — perlu sebelum akun dipakai pengguna nyata?

## Fase 7 — Cloud Backup & Sync

**Status:** `BLOCKED` *(menunggu Fase 6 selesai)*

**Summary:** Backup opt-in per akun untuk trip `completed` + location point dan `places`, sync via WorkManager (push chunk 1 000 titik, pull berbasis cursor monotonic, upsert idempoten berdasarkan ID). Konflik: hapus selalu menang untuk trip, last-write-wins `updated_at` untuk tempat, tombstone `deleted_at` (migrasi 4 → 5). Fitur aktifkan/matikan backup, restore di perangkat baru (gabung, tidak menimpa), status sync, dan peringatan login ke akun berbeda. Visit tidak disinkronkan, dihitung ulang lokal. Detail: PRD §38 Fase 7.

**Open questions (resolve during planning):**
1. Constraint WorkManager (jaringan apa pun vs. unmetered) dan jadwal periodik vs. dipicu saat trip selesai.
2. Strategi menguji "tidak ada data lokasi terkirim tanpa backup" (inspeksi trafik) secara berulang.
3. Volume data per pengguna dan kebutuhan partisi/index tabel titik di PostgreSQL.

## Fase 8 — Group

**Status:** `BLOCKED` *(menunggu Fase 7 selesai)*

**Summary:** Grup dengan peran owner/anggota, maks. 20 anggota per grup dan 10 grup per pengguna. Undangan berupa kode 8 karakter berlaku 7 hari plus tautan `https://<domain>/join/<kode>` via Android App Links. Tab navigasi **Grup**; daftar grup & anggota di-cache read-only di Room (migrasi 5 → 6), aksi server nonaktif saat offline. Bergabung ke grup tidak mengirim data lokasi. Detail: PRD §38 Fase 8.

**Open questions (resolve during planning):**
1. Hosting `assetlinks.json` dan halaman web fallback untuk tautan undangan di domain backend.
2. Kebijakan refresh cache grup (saat tab dibuka, pull-to-refresh, atau keduanya).
3. Otorisasi API (Laravel Policy) dan cakupan test untuk aturan owner vs. anggota.

## Fase 9 — Live Sharing

**Status:** `BLOCKED` *(menunggu Fase 8 selesai)*

**Summary:** Berbagi lokasi realtime per grup dengan durasi (15 menit, 1 jam default, 8 jam, sampai dimatikan), hanya lat/lng/accuracy/waktu. Server menyimpan satu lokasi terakhir per pengguna tanpa riwayat dan menyiarkannya lewat Laravel Reverb; lokal memakai `live_location_outbox` (maks. satu baris, migrasi 6 → 7) dan `LiveSharingConfig`. Satu foreground service dan satu sumber GPS dipakai bersama tracking, dengan notification gabungan; peta grup menampilkan marker anggota yang berbagi. Detail: PRD §38 Fase 9.

**Open questions (resolve during planning):**
1. Refactor `LocationTrackingService` agar melayani tracking dan sharing sekaligus tanpa melanggar invarian Fase 1 (DEC-001, satu trip aktif).
2. Library klien WebSocket/Pusher di Android dan perilaku reconnect saat jaringan berganti.
3. Penegakan `expires_at` di server (scheduler/queue) dan di perangkat bila perangkat offline saat durasi habis.
