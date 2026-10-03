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

## Bug: renderer Vulkan MapLibre crash dan ANR saat berpindah layar

**Status:** `OPEN`

**Summary:** Uji emulator 2026-10-03 (Android 16, `sdk_gphone64_x86_64`, GPU host): (1) crash native `SIGSEGV` (null pointer) di `mln::android::MapRenderer::render` pada `MapLibreVulkanSurfaceView`, terjadi saat aplikasi di background ±8 menit lalu trip dihentikan dari notification; (2) ANR saat meninggalkan Trip Detail: main thread menunggu di `MapLibreSurfaceView.surfaceDestroyed`, render thread terkunci di `vkDestroyFramebuffer` driver Vulkan emulator. Artifact `org.maplibre.gl:android-sdk` 13.6.1 memakai backend Vulkan; MapLibre juga menerbitkan `android-sdk-opengl` 13.6.1 dengan API yang sama. Laporan serupa: maplibre-native #3092 ("Vulkan is extremely unstable"), #2579, #4331. Emulator sendiri sempat crash (gfxstream "null ctx"), jadi sebagian penyebab mungkin khusus emulator.

**Open questions (resolve during planning):**
1. Ganti ke `android-sdk-opengl` 13.6.1 (rekomendasi) lalu ulangi skenario crash/ANR di emulator; apakah perlu juga diuji di HP nyata sebelum diputuskan?
2. Perlukah membatasi satu `MapView` aktif sekaligus (hindari dua renderer saat transisi navigasi)?

## Bug: penghemat baterai berhenti merekam saat pengguna mulai bergerak lagi

**Status:** `OPEN`

**Summary:** Uji emulator 2026-10-03: setelah diam 2 menit, request turun ke 30 s `PRIORITY_BALANCED_POWER_ACCURACY` (PRD §38 Fase 5). Sesudahnya hanya satu fix datang, dan selama 70 detik berkendara 10 m/s tidak ada satu pun titik tersimpan (jarak 0 m), karena `StationaryDetector` butuh fix untuk tahu pengguna bergerak. Di emulator mode balanced tidak memberi fix GPS sama sekali; di HP nyata mode ini memakai Wi-Fi/seluler yang di jalan sering ber-accuracy > 100 m, yang diabaikan detektor dan ditolak filter, sehingga perjalanan setelah singgah bisa tidak terekam. Prioritas tinggi: kehilangan data trip.

**Open questions (resolve during planning):**
1. Mode diam tetap `HIGH_ACCURACY` dan hanya interval diperpanjang ke 30 s (revisi PRD §38 Fase 5)? Atau tambah `setMinUpdateDistanceMeters`?
2. Perlukah Activity Recognition (transisi gerak) sebagai pemicu keluar mode diam untuk trip manual?

## Bug: peredam jitter dan penghemat baterai peka terhadap kecepatan GPS yang berisik

**Status:** `OPEN`

**Summary:** Di emulator, Fused Location menghitung kecepatan dari selisih posisi sehingga saat diam dengan simpangan ±5 m kecepatannya 0,6–2,2 m/s. Akibatnya `StationaryDetector` tidak pernah masuk mode diam (satu fix ≥ 0,5 m/s mereset) dan `anchoredDistance` menambah ±200 m selama 6 menit diam (accuracy dilaporkan 5 m, simpangan lebih besar). HP nyata memakai kecepatan Doppler yang biasanya mendekati 0 saat diam, tetapi sifat aturan ini sama dengan temuan simulasi DEC-008.

**Open questions (resolve during planning):**
1. Butuh rekaman nyata 10 menit diam di HP sebelum mengubah aturan?
2. Opsi: keluar mode diam hanya bila ≥ 2 fix berturut-turut bergerak; syarat jarak memakai jumlah accuracy; atau median kecepatan beberapa fix. Masing-masing perlu revisi PRD §38 Fase 5.

## Bug: menekan tab yang sedang aktif tidak kembali ke layar akarnya

**Status:** `OPEN`

**Summary:** Di Settings (dibuka dari Home) atau Trip Detail, menekan tab yang sama di navigasi bawah tidak kembali ke Home/History. Penyebab: `MapTrackNavHost` memakai `popUpTo(start) { saveState = true }` + `restoreState = true` untuk semua tab, sehingga back stack tab aktif disimpan lalu langsung dipulihkan.

**Open questions (resolve during planning):**
1. Perilaku yang diinginkan: tab aktif ditekan → kembali ke akar tab (pola umum Material)?

## Bug: teks tombol dialog izin aktivitas fisik dan tata letak form tempat

**Status:** `OPEN`

**Summary:** (1) Dialog penjelasan izin aktivitas fisik (trip otomatis) memakai tombol "Izinkan lokasi" (string `permission_allow`); seharusnya "Izinkan". (2) Form tempat: saat keyboard terbuka seluruh layar terdorong ke atas sehingga top bar dan tombol Simpan tidak terlihat. (3) Slider radius menampilkan 94 titik langkah yang terlihat padat.

**Open questions (resolve during planning):**
1. Form tempat memakai `imePadding`/`windowSoftInputMode=adjustResize` agar top bar tetap terlihat?
2. Slider tanpa titik langkah (`steps = 0` + pembulatan 10 m di ViewModel)?


## Fase 6 — Account

**Status:** `BLOCKED` *(pilihan backend sedang didiskusikan dengan user: Laravel + VPS di PRD saat ini vs. Firebase Spark gratis; PRD wajib direvisi sebelum tiket dibuat)*

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
