# Product Requirements Document — Map Track

**Versi:** 2.1 (2026-10-03). Scope project adalah produk penuh yang dikerjakan dalam 9 fase (§6). Daftar perubahan ada di [§45 Riwayat Revisi](#45-riwayat-revisi).

## 1. Ringkasan Produk

Map Track adalah aplikasi Android untuk mencatat lokasi, perjalanan, dan kecepatan pengguna.

Aplikasi mengambil data lokasi perangkat secara berkala selama tracking aktif, kemudian menyimpannya ke database lokal. Data tersebut digunakan untuk menampilkan posisi pengguna, riwayat perjalanan, jalur perjalanan pada peta, serta statistik seperti jarak, durasi, kecepatan rata-rata, dan kecepatan maksimum.

Di atas fondasi tersebut, aplikasi berkembang bertahap menjadi: deteksi tempat singgah, tempat tersimpan, peta offline, deteksi perjalanan otomatis, akun, backup dan sync ke cloud, grup, serta berbagi lokasi secara realtime antar-anggota grup.

Aplikasi dirancang dengan pendekatan **offline-first** dan **local-first**. Fitur inti (tracking, histori, statistik, tempat) tetap dapat digunakan tanpa koneksi internet dan tanpa akun di semua fase. Fitur jaringan bersifat opsional dan harus diaktifkan pengguna secara eksplisit.

---

# 2. Tujuan Produk

Tujuan aplikasi adalah memungkinkan pengguna:

1. Melihat posisi dirinya pada peta.
2. Melihat kecepatan saat ini.
3. Memulai dan menghentikan pencatatan perjalanan.
4. Tetap merekam lokasi ketika aplikasi berjalan di background.
5. Menyimpan seluruh data perjalanan secara lokal.
6. Melihat histori perjalanan.
7. Melihat jalur perjalanan pada peta.
8. Melihat histori kecepatan selama perjalanan.
9. Melihat statistik perjalanan.
10. Menggunakan fungsi utama tanpa koneksi internet.
11. Mengetahui tempat-tempat yang disinggahi dan memberi nama tempat penting.
12. Memakai peta tanpa internet di wilayah yang sudah diunduh.
13. Mencatat perjalanan secara otomatis tanpa menekan Start.
14. Membackup data perjalanan ke cloud dan memakainya di perangkat lain.
15. Membuat grup dan berbagi lokasi secara realtime dengan anggota grup.

---

# 3. Prinsip Produk

Produk dikembangkan berdasarkan prinsip berikut. Prinsip ini berlaku di semua fase.

### Offline-first

Fungsi inti tidak boleh bergantung pada backend atau koneksi internet.

Data perjalanan disimpan terlebih dahulu di perangkat. Fitur jaringan (sync, grup, live sharing) harus menangani kondisi offline dengan wajar: data diantre lalu dikirim ketika koneksi tersedia, tanpa mengganggu tracking.

### Local-first

Room di perangkat adalah sumber data utama. Backend hanya berfungsi sebagai salinan untuk backup, sync, dan sharing, bukan pengganti database lokal.

Aplikasi tetap dapat dipakai penuh tanpa akun.

### Privacy-first

Karena data lokasi merupakan data sensitif, data lokasi tidak boleh keluar dari perangkat kecuali:

1. pengguna sudah login; **dan**
2. pengguna mengaktifkan fitur terkait secara eksplisit (Cloud Backup atau Live Sharing).

Kedua fitur tersebut default nonaktif dan dapat dimatikan kapan saja. Tidak ada telemetry atau analytics lokasi di fase mana pun. Rincian per fase ada di §35.

### Battery-aware

Tracking dan live sharing harus cukup akurat tanpa membuat penggunaan baterai berlebihan.

Frekuensi pengambilan lokasi dapat diatur pengguna mulai Fase 4.

### Bertahap

Setiap fase fokus pada scope-nya dan harus stabil sebelum fase berikutnya dimulai. Fitur kompleks (cloud, grup, live sharing) tidak dikerjakan sebelum fondasi tracking lokal stabil.

Keputusan teknis pada fase awal tetap memperhitungkan kebutuhan fase akhir agar tidak memerlukan perombakan besar. Contohnya, ID trip berupa UUID sejak Fase 1 agar siap untuk sync (§24).

---

# 4. Target Platform

## Client

**Android**

Teknologi utama:

- Kotlin
- Jetpack Compose
- Android ViewModel
- Kotlin Coroutines
- Kotlin Flow
- Room Database
- Fused Location Provider
- Android Foreground Service
- MapLibre
- DataStore (mulai Fase 4, §26)

## Backend (mulai Fase 6)

- Laravel
- REST API untuk akun, sync, dan grup
- Kanal realtime (WebSocket) untuk live sharing

Detail backend (versi, database server, layanan realtime, hosting, metode autentikasi) ditetapkan di tiket awal Fase 6.

Tidak tersedia client:

- iOS
- Web
- Desktop

---

# 5. Target Pengguna

Semua pengguna adalah orang yang sama dengan tingkat fitur berbeda. Tidak ada role admin aplikasi.

| Tipe | Tersedia sejak | Kebutuhan |
|---|---|---|
| Personal User (tanpa akun) | Fase 1 | Mencatat perjalanan dan aktivitas lokasinya sendiri |
| Account User | Fase 6 | Membackup data dan memakainya di perangkat lain |
| Group Member | Fase 8 | Berbagi lokasi dan melihat lokasi anggota grup |

Contoh kebutuhan Personal User:

- Melihat perjalanan hari ini.
- Melihat jalur yang telah dilalui.
- Melihat kecepatan selama perjalanan.
- Mengetahui jarak perjalanan.
- Mengetahui berapa lama perjalanan berlangsung.
- Melihat riwayat perjalanan beberapa hari sebelumnya.
- Mengetahui berapa lama berada di suatu tempat.

Dalam grup terdapat dua peran: **owner** (pembuat grup) dan **member**.

---

# 6. Ruang Lingkup dan Fase Pengembangan

Project dikerjakan sampai produk penuh dalam 9 fase berurutan.

| Fase | Nama | Isi utama | Butuh backend |
|---|---|---|---|
| 1 | Core Tracking Lokal | Tracking, peta, trip, histori, statistik, recovery | Tidak |
| 2 | Place Detection | Deteksi tempat singgah (visit) | Tidak |
| 3 | Saved Places | Tempat bernama milik pengguna | Tidak |
| 4 | Offline Map & Settings | Unduh wilayah peta, halaman Settings | Tidak |
| 5 | Automatic Trip & Tracking Improvements | Trip otomatis, lanjutkan trip terputus, peredam jitter | Tidak |
| 6 | Account | Backend, register/login, device management | Ya |
| 7 | Cloud Backup & Sync | Backup, restore, sync antarperangkat | Ya |
| 8 | Group | Grup, undangan, keanggotaan | Ya |
| 9 | Live Sharing | Lokasi realtime ke anggota grup | Ya |

Account (Fase 6) dikerjakan sebelum Cloud Backup (Fase 7) karena backup ke server membutuhkan identitas pemilik data.

Bagian §7–§36 dan §39–§43 menjelaskan Fase 1 secara rinci. Fase 2–9 dijelaskan di §38. Detail rinci sebuah fase wajib dilengkapi di PRD ini sebelum fase tersebut mulai dikerjakan.

---

# 7. Fitur Utama

## 7.1 Home

Home merupakan halaman utama aplikasi.

Home harus menampilkan:

- peta;
- posisi pengguna saat ini;
- status GPS;
- status tracking;
- kecepatan saat ini;
- akurasi lokasi;
- tombol Start Tracking atau Stop Tracking.

Aturan:

- Posisi, kecepatan, dan akurasi di Home berasal dari GPS. Saat tracking aktif, data diambil dari service tracking. Saat tidak tracking, Home meminta update lokasi sendiri **hanya selama Home terlihat di layar**, lalu berhenti ketika Home ditinggalkan (battery-aware).
- Saat aplikasi dibuka kembali ketika tracking masih berjalan, Home langsung menampilkan status tracking aktif. Status ini dibaca dari trip berstatus `active` di database dan service yang sedang berjalan.

Contoh:

```text
My Location

┌───────────────────────────┐
│                           │
│            MAP            │
│                           │
│             ●             │
│                           │
└───────────────────────────┘

Kecepatan
42 km/h

Akurasi
± 6 meter

Tracking
● Aktif

[ Stop Tracking ]
```

---

# 8. Tracking Lokasi

## 8.1 Start Tracking

Pengguna dapat memulai tracking secara manual.

Ketika pengguna menekan:

```text
Start Tracking
```

sistem harus:

1. Memeriksa permission lokasi presisi (lihat §28).
2. Meminta permission notifikasi pada Android 13+ (boleh ditolak; tracking tetap berjalan).
3. Memeriksa apakah layanan lokasi perangkat aktif.
4. Memastikan tidak ada trip lain berstatus `active`. Hanya boleh ada **satu** trip aktif dalam satu waktu.
5. Membuat Trip baru dengan status `active`.
6. Menjalankan Foreground Location Service.
7. Memulai pengambilan lokasi.
8. Menyimpan titik lokasi yang lolos filter (§12) ke database.
9. Menampilkan status tracking aktif.

---

## 8.2 Stop Tracking

Pengguna dapat menghentikan tracking dari Home atau dari tombol **Stop** di foreground notification. Keduanya menjalankan logika yang sama, sehingga logika stop tidak boleh berada di UI.

Saat tombol:

```text
Stop Tracking
```

ditekan, sistem harus:

1. Menghentikan pengambilan lokasi.
2. Menghentikan foreground service.
3. Menetapkan `ended_at` = waktu saat Stop ditekan.
4. Menghitung statistik perjalanan (§16).
5. Menyimpan hasil statistik.
6. Mengubah status trip menjadi `completed`.

Langkah 3–6 dijalankan dalam satu transaksi database.

---

# 9. Background Tracking

Tracking harus tetap berjalan ketika:

- aplikasi diminimize;
- pengguna membuka aplikasi lain;
- layar perangkat dikunci.

Android Foreground Service digunakan selama tracking aktif.

Foreground notification wajib muncul.

Contoh:

```text
Location Tracker

Tracking aktif

Kecepatan: 43 km/h
Durasi: 18 menit

[ Stop ]
```

Menekan notification membuka kembali aplikasi.

Isi notification diperbarui setiap ada titik lokasi baru. Jika GPS sementara tidak tersedia, notification menampilkan `Menunggu sinyal GPS…` (lihat §31).

Aplikasi **tidak** meminta permission background location (`ACCESS_BACKGROUND_LOCATION`). Service selalu dimulai dari aksi pengguna saat aplikasi berada di foreground, sehingga permission lokasi foreground ditambah foreground service bertipe `location` sudah cukup. Ini sejalan dengan prinsip privacy-first. Keputusan ini ditinjau ulang di Fase 5, khusus untuk deteksi trip otomatis.

---

# 10. Data Lokasi

Setiap titik lokasi menyimpan:

```text
trip_id
latitude
longitude
accuracy
speed      (opsional)
bearing    (opsional)
altitude   (opsional)
recorded_at
```

`recorded_at` adalah waktu fix GPS dari Android (`Location.time`), bukan waktu penyimpanan ke database. Field opsional disimpan sebagai `NULL` apabila Android tidak menyediakannya. Skema lengkap ada di §25.

Contoh:

```text
Latitude:
-7.781245

Longitude:
110.367231

Accuracy:
5.4 meter

Speed:
12.5 m/s

Bearing:
118°

Recorded at:
2026-09-29 07:35:21
```

---

# 11. Interval Tracking

Pada Fase 1, aplikasi menggunakan interval tracking default.

Rekomendasi awal:

```text
Target interval:
5 detik
```

Namun aplikasi tidak harus menjamin lokasi baru tersedia tepat setiap 5 detik karena bergantung pada GPS dan sistem Android.

Konfigurasi request lokasi selama tracking:

| Parameter | Nilai default |
|---|---|
| Priority | `PRIORITY_HIGH_ACCURACY` (dibutuhkan untuk speed yang layak) |
| Interval | 5 detik |
| Min update interval | 5 detik |

Prioritas akurasi tinggi hanya dipakai selama tracking aktif dan selama Home terlihat (§7.1). Di luar itu tidak ada request lokasi sama sekali.

Semua parameter tracking dan filtering (§11, §12, §14) dikumpulkan sebagai konstanta di satu objek konfigurasi di source code (misalnya `TrackingConfig`) agar mudah diubah.

Mulai Fase 4, interval dapat diubah pengguna melalui halaman Settings.

---

# 12. Filtering Data GPS

Tidak semua hasil GPS harus langsung disimpan.

Sistem harus dapat menolak data yang jelas tidak valid.

Aturan dasar. Titik yang ditolak **tidak disimpan** ke database.

| Aturan | Tolak apabila | Default |
|---|---|---|
| Koordinat tidak valid | latitude di luar −90..90 atau longitude di luar −180..180 | — |
| Accuracy buruk | `accuracy` > ambang | 50 meter |
| Urutan waktu | `recorded_at` ≤ `recorded_at` titik tersimpan sebelumnya | — |
| GPS jump | jarak ke titik tersimpan sebelumnya ÷ selisih waktu > batas kecepatan | 70 m/s (252 km/h) |

Karena titik yang buruk sudah ditolak saat disimpan, seluruh perhitungan statistik (§16, §17) cukup memakai semua titik tersimpan tanpa filter tambahan.

Nilai `accuracy` tetap disimpan agar filtering dapat dikembangkan kemudian. Contohnya: meredam jitter GPS saat pengguna diam, yang pada Fase 1 dapat sedikit menambah jarak dan diredam di Fase 5.

---

# 13. Kecepatan

Kecepatan menggunakan data lokasi Android apabila tersedia.

Database menyimpan kecepatan dalam satuan:

```text
meter per second
```

Tampilan pengguna menggunakan:

```text
km/h
```

Konversi:

```text
km/h = m/s × 3.6
```

Contoh:

```text
12.5 m/s

=

45 km/h
```

---

# 14. Current Speed

Saat tracking aktif Home menampilkan:

```text
Kecepatan saat ini
```

Contoh:

```text
63 km/h
```

Aturan tampilan:

| Kondisi | Tampilan |
|---|---|
| Speed tersedia | nilai dibulatkan, misalnya `63 km/h` |
| Speed tersedia dan < 1 km/h (diam) | `0 km/h` |
| Fix terakhir tidak punya speed | `— km/h` |
| Fix terakhir lebih tua dari 15 detik (data basi) | `— km/h` |

---

# 15. Trip

Trip merupakan satu sesi tracking.

Trip dimulai ketika pengguna menekan:

```text
Start Tracking
```

dan berakhir ketika pengguna menekan:

```text
Stop Tracking
```

Trip ditampilkan dengan tanggal dan jam mulainya. ID trip berupa UUID (§24) dan tidak ditampilkan. Status trip:

| Status | Arti |
|---|---|
| `active` | Trip sedang direkam, atau terputus secara tidak normal (§32) |
| `completed` | Trip sudah diakhiri dan statistiknya sudah dihitung |

Status `interrupted` tidak disimpan sebagai nilai tersendiri. Trip yang terputus dikenali saat runtime sebagai trip `active` yang service-nya tidak berjalan (§33).

Contoh:

```text
Perjalanan 29 September 2026

Start:
07:32

End:
08:18

Duration:
46 menit
```

---

# 16. Trip Statistics

Setelah trip selesai, aplikasi menghitung minimal:

- waktu mulai;
- waktu selesai;
- durasi;
- total jarak;
- kecepatan rata-rata;
- kecepatan maksimum;
- jumlah location point.

Definisi:

| Statistik | Rumus | Disimpan? |
|---|---|---|
| Durasi | `ended_at − started_at` (termasuk waktu berhenti) | Tidak, dihitung dari kolom |
| Jarak | Jumlah jarak antar-titik berurutan (§17) | Ya, `distance_meters` |
| Kecepatan rata-rata | `distance_meters ÷ durasi` | Ya, `average_speed` (m/s) |
| Kecepatan maksimum | Nilai `speed` terbesar dari titik tersimpan | Ya, `max_speed` (m/s), `NULL` jika tidak ada titik dengan speed |
| Jumlah titik | `COUNT` location point milik trip | Tidak, di-query saat dibutuhkan |

Contoh di bawah konsisten dengan rumus ini: 21.7 km ÷ 46 menit ≈ 28 km/h.

Trip dengan kurang dari 2 titik tetap disimpan dengan `distance_meters = 0`. Halaman detailnya menampilkan keterangan bahwa data lokasi tidak cukup untuk menggambar rute.

Contoh:

```text
Trip Summary

29 September 2026

07:32 - 08:18

Durasi
46 menit

Jarak
21.7 km

Kecepatan rata-rata
28 km/h

Kecepatan maksimum
73 km/h
```

---

# 17. Perhitungan Jarak

Jarak dihitung berdasarkan posisi antar-location point secara berurutan.

Contoh:

```text
Point A
↓
120 meter
↓
Point B
↓
78 meter
↓
Point C
```

Total:

```text
198 meter
```

Jarak antar-titik dihitung dengan rumus haversine (jarak lingkaran besar, radius bumi rata-rata 6 371 008,8 m). Selisihnya terhadap jarak elipsoid WGS84 di bawah 0,5%, dan rumus ini dapat dites tanpa perangkat Android. Titik dengan GPS jump atau accuracy buruk sudah ditolak oleh filter §12, sehingga tidak ikut dihitung.

---

# 18. Trip History

Aplikasi memiliki halaman:

```text
History
```

yang menampilkan seluruh trip.

Trip diurutkan dari yang terbaru.

Contoh:

```text
29 September 2026

07:32 - 08:18
21.7 km
46 menit

────────────

28 September 2026

16:13 - 16:47
8.2 km
34 menit
```

Setiap item dapat ditekan untuk membuka detail perjalanan.

Trip yang sedang aktif ditampilkan paling atas dengan label `Sedang berjalan`, tanpa statistik akhir.

---

# 19. Trip Detail

Halaman detail trip menampilkan:

### Summary

- tanggal;
- start time;
- end time;
- duration;
- distance;
- average speed;
- max speed.

### Map

Menampilkan seluruh rute perjalanan.

### Speed History

Menampilkan grafik perubahan kecepatan.

---

# 20. Trip Route

Location point dari sebuah trip digambar sebagai polyline pada peta.

Contoh:

```text
START ●──────────╮
                 │
         ╭───────╯
         │
         │
         ╰──────────● FINISH
```

Peta menampilkan:

- marker start;
- marker finish;
- route/polyline.

Marker tempat singgah ditambahkan di Fase 2 (Place Detection).

---

# 21. Speed History

Trip Detail menampilkan grafik kecepatan berdasarkan waktu.

Contoh:

```text
80 │
70 │             ╭──╮
60 │          ╭──╯  ╰─╮
50 │       ╭──╯       │
40 │    ╭──╯          ╰─
30 │────╯
   └────────────────────
   07:32            08:18
```

Data berasal dari:

```text
location_points.speed
```

Tidak diperlukan tabel khusus untuk speed history.

Titik dengan `speed = NULL` dilewati. Grafik digambar dengan Compose Canvas, tanpa library chart tambahan (simple-first).

---

# 22. Map

Aplikasi menggunakan MapLibre.

Map digunakan untuk:

- posisi saat ini;
- marker pengguna;
- route;
- start marker;
- finish marker.

Peta tidak menjadi sumber data lokasi.

GPS tetap menjadi sumber utama koordinat.

**Sumber tile:** aplikasi memakai OpenFreeMap (style `liberty`), tile vektor online publik yang tidak membutuhkan akun atau API key. Atribusi OpenFreeMap, OpenMapTiles, dan OpenStreetMap ditampilkan lewat tombol atribusi peta.

**Catatan privasi:** saat mengunduh tile, server tile dapat mengetahui area peta yang sedang dilihat. Namun aplikasi tidak pernah mengirim koordinat GPS, titik lokasi, atau data trip ke server mana pun. Untuk wilayah yang sudah diunduh (Fase 4), peta tidak lagi memerlukan request tile.

---

# 23. Offline Map

Peta offline dikerjakan dalam dua tahap.

## Tahap pertama (Fase 1)

Tracking lokasi dan penyimpanan histori harus tetap bekerja tanpa internet.

Apabila tile peta tidak tersedia:

```text
Tracking tetap berjalan.
```

Map dapat tampil kosong atau hanya menampilkan resource yang sudah tersedia.

## Tahap kedua (Fase 4)

Pengguna dapat mengunduh wilayah peta untuk penggunaan offline.

Contoh:

```text
Offline Maps

Yogyakarta
Downloaded
184 MB

Surabaya
Not Downloaded

[ Download ]
```

---

# 24. Database

Aplikasi menggunakan Room Database.

Database minimal memiliki tabel berikut.

---

## 24.1 Trips

```text
trips
```

Konvensi untuk semua tabel:

- Waktu disimpan sebagai epoch milliseconds UTC (`Long`) dan ditampilkan dalam zona waktu perangkat.
- Kecepatan disimpan dalam m/s, jarak dalam meter.

| Field | Tipe | Aturan |
|---|---|---|
| `id` | String | Primary key, UUID v4 yang dibuat di perangkat |
| `started_at` | Long | Wajib |
| `ended_at` | Long? | `NULL` selama `active` |
| `distance_meters` | Double? | `NULL` selama `active` |
| `average_speed` | Double? | m/s, `NULL` selama `active` |
| `max_speed` | Double? | m/s, `NULL` selama `active` atau bila tidak ada data speed |
| `status` | String | `active` / `completed` |
| `updated_at` | Long | Wajib, diperbarui setiap baris berubah |

Kolom `created_at` dihapus karena nilainya sama dengan `started_at`.

ID berupa UUID dan kolom `updated_at` disiapkan sejak Fase 1 agar sync antarperangkat (Fase 7) tidak memerlukan perubahan primary key. Kolom sync lain, misalnya penanda penghapusan, ditambahkan lewat migrasi Room di Fase 7.

Index: `status` (untuk mencari trip aktif) dan `started_at` (untuk urutan History).

---

# 25. Location Points

Table:

```text
location_points
```

| Field | Tipe | Aturan |
|---|---|---|
| `id` | Long | Primary key, auto-increment |
| `trip_id` | String | Wajib, foreign key ke `trips.id`, `ON DELETE CASCADE` |
| `latitude` | Double | Wajib |
| `longitude` | Double | Wajib |
| `accuracy` | Float | Wajib, meter |
| `speed` | Float? | m/s |
| `bearing` | Float? | derajat |
| `altitude` | Double? | meter |
| `recorded_at` | Long | Wajib, waktu fix GPS |

Kolom `created_at` dihapus karena tidak dipakai oleh fitur mana pun.

Location point tidak pernah diubah setelah disimpan (append-only), sehingga tidak memerlukan `updated_at`. `id` hanya berlaku lokal. Identitas titik saat sync adalah pasangan (`trip_id`, `recorded_at`).

Index unik: (`trip_id`, `recorded_at`). Index ini dipakai untuk mengambil titik sebuah trip secara berurutan dan mencegah titik ganda.

Relasi:

```text
Trip
1
│
│
∞
LocationPoint
```

Apabila Trip dihapus, seluruh LocationPoint milik trip tersebut harus ikut dihapus.

---

# 26. DataStore

Fase 1–3 tidak memiliki halaman Settings, sehingga DataStore belum dipakai. Seluruh nilai default disimpan sebagai konstanta di source code (§11).

DataStore ditambahkan bersama halaman Settings pada Fase 4. Contoh pengaturan yang disimpan di sana:

```text
tracking_interval
accuracy_threshold
distance_unit
map_preferences
```

---

# 27. Navigation

Navigation utama:

```text
Home
History
```

Kemungkinan struktur:

```text
Home
│
├── Map
├── Current Speed
└── Tracking Control


History
│
├── Trip List
│
└── Trip Detail
    ├── Summary
    ├── Map
    └── Speed History
```

---

# 28. Permission

Aplikasi membutuhkan permission lokasi.

Permission harus dijelaskan secara transparan kepada pengguna.

Aplikasi harus meminta permission sesuai kebutuhan Android.

| Permission | Kapan | Wajib untuk tracking? |
|---|---|---|
| `ACCESS_FINE_LOCATION` (+ `ACCESS_COARSE_LOCATION`) | Saat pertama kali menekan Start Tracking atau membuka Home | Ya. Lokasi **presisi** wajib karena lokasi perkiraan tidak cukup untuk speed dan rute. |
| `POST_NOTIFICATIONS` (Android 13+) | Saat Start Tracking | Tidak. Jika ditolak, tracking tetap berjalan tetapi notification mungkin tidak terlihat. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | Dideklarasikan di manifest, tidak perlu diminta | — |
| `ACCESS_BACKGROUND_LOCATION` | **Tidak diminta** (lihat §9) | — |

Jika pengguna hanya memberi lokasi perkiraan, perlakukan sama seperti permission ditolak (§30).

Pengguna tidak boleh dipaksa memberikan permission sebelum mengetahui alasan penggunaannya.

Contoh informasi:

```text
Aplikasi membutuhkan akses lokasi untuk
merekam perjalanan dan kecepatan Anda.

Data lokasi hanya disimpan di perangkat.
```

---

# 29. GPS Tidak Aktif

Apabila GPS/location service perangkat mati ketika pengguna menekan Start Tracking:

Sistem harus menampilkan informasi:

```text
Location service tidak aktif.

Aktifkan Location agar tracking dapat dimulai.
```

Tracking tidak dimulai sampai location tersedia.

Apabila location service dimatikan **saat tracking sedang berjalan**, trip tetap `active` dan diperlakukan sama seperti kehilangan sinyal GPS (§31). Notification menampilkan `Location service tidak aktif`.

---

# 30. Permission Ditolak

Jika permission lokasi ditolak:

```text
Tracking tidak dapat dimulai karena
izin lokasi belum diberikan.
```

Aplikasi tetap dapat digunakan untuk melihat histori lama.

---

# 31. Kehilangan Sinyal GPS

Tracking tidak boleh langsung berhenti ketika GPS sementara tidak tersedia.

Contoh:

```text
Masuk basement
↓
GPS hilang
↓
tracking tetap ACTIVE
↓
GPS kembali
↓
tracking dilanjutkan
```

Sistem hanya berhenti apabila pengguna menghentikan tracking.

---

# 32. App Ditutup

Selama foreground service berjalan, tracking harus tetap berjalan walaupun Activity aplikasi tidak sedang terbuka.

Jika Android menghentikan proses karena kondisi tertentu, aplikasi harus menghindari menghasilkan trip yang corrupt.

Trip yang belum selesai tetap tersimpan dengan status `active`. Pada startup berikutnya, trip tersebut dikenali sebagai **terputus** apabila service tracking tidak sedang berjalan (§33).

Setiap titik lokasi langsung disimpan ke database saat diterima, sehingga data yang sudah terekam tidak hilang ketika proses dihentikan.

Service tidak di-restart otomatis oleh sistem (`START_NOT_STICKY`). Android modern membatasi foreground service lokasi yang dimulai dari background, sehingga pemulihan diserahkan kepada pengguna melalui §33.

---

# 33. Recovery Trip

Saat aplikasi dibuka, sistem memeriksa apakah ada trip dengan:

```text
status = active
```

Jika ada namun tracking service sudah tidak berjalan, sistem dapat menampilkan:

```text
Perjalanan sebelumnya tidak selesai dengan normal.

Mulai:
07:32

Data terakhir:
08:01

[ Akhiri Trip ]
```

Menekan **Akhiri Trip** menyelesaikan trip dengan aturan berikut:

- `ended_at` = `recorded_at` titik terakhir, atau `started_at` jika tidak ada titik;
- statistik dihitung seperti biasa (§16);
- status menjadi `completed`.

Selama masih ada trip terputus, Start Tracking tidak dapat dipakai (§8.1 langkah 4) sampai trip tersebut diakhiri.

Melanjutkan trip yang terputus dikerjakan di Fase 5.

---

# 34. Delete Trip

Pengguna harus dapat menghapus histori perjalanan.

Ketika trip dihapus:

```text
Trip
+
semua location point
```

harus ikut dihapus.

Trip berstatus `active` tidak dapat dihapus. Trip tersebut harus dihentikan atau diakhiri terlebih dahulu.

Sebelum menghapus tampilkan konfirmasi.

```text
Hapus perjalanan ini?

Data perjalanan dan lokasi akan dihapus
secara permanen dari perangkat.

[ Batal ]
[ Hapus ]
```

---

# 35. Privacy

Data yang keluar dari perangkat per fase:

| Fase | Data yang keluar perangkat |
|---|---|
| 1–3 | Tidak ada, kecuali request tile peta (§22) |
| 4 | Request tile dan unduhan wilayah peta |
| 5 | Sama dengan Fase 4 |
| 6 | Data akun (email, nama, info perangkat). Tidak ada data lokasi. |
| 7 | Trip dan location point, **hanya** jika pengguna mengaktifkan Cloud Backup |
| 8 | Data grup dan keanggotaan. Tidak ada data lokasi. |
| 9 | Lokasi terkini, **hanya** ke anggota grup dan **hanya** selama pengguna mengaktifkan Live Sharing |

Request tile dan unduhan wilayah peta tidak membawa koordinat GPS maupun data trip.

Aturan umum:

- tanpa login, aplikasi tidak mengirim data lokasi ke mana pun;
- Cloud Backup dan Live Sharing default nonaktif (opt-in);
- pengguna dapat menghapus akun beserta seluruh datanya di server;
- tidak ada telemetry atau analytics lokasi;
- seluruh komunikasi dengan backend wajib memakai HTTPS/TLS.

---

# 36. Non-Functional Requirements

## Performance

Home harus tetap responsif saat tracking berjalan.

Map tidak boleh membaca seluruh database location point setiap kali posisi berubah.

## Battery

Pengambilan GPS harus memperhatikan konsumsi baterai.

Interval default tidak boleh terlalu agresif tanpa kebutuhan. Hal yang sama berlaku untuk frekuensi pengiriman Live Sharing.

## Reliability

Tracking tidak boleh berhenti hanya karena pengguna meninggalkan Activity aplikasi.

Kegagalan jaringan atau backend tidak boleh menghentikan maupun memperlambat tracking.

## Data Integrity

LocationPoint harus memiliki Trip yang valid.

Sync tidak boleh menghasilkan trip atau titik ganda.

## Privacy

Tidak ada upload data lokasi tanpa fitur eksplisit dan persetujuan pengguna (§35).

## Security (mulai Fase 6)

Token autentikasi disimpan dengan aman di perangkat. Setiap pengguna hanya dapat mengakses datanya sendiri, dan lokasi anggota grup hanya dapat diakses oleh grup tersebut.

---

# 37. Out of Scope

Fitur berikut **tidak dikembangkan** di fase mana pun. Menambahkannya berarti merevisi PRD ini terlebih dahulu.

- client iOS, Web, dan Desktop;
- chat;
- SOS;
- crash detection;
- driving detection sebagai fitur tersendiri (Activity Recognition boleh dipakai secara internal untuk Fase 5);
- status baterai perangkat lain;
- geofence alert (notifikasi masuk/keluar area);
- deteksi Home/Work otomatis (Saved Places diisi manual oleh pengguna, Fase 3);
- reverse geocoding online;
- push notification (foreground notification untuk tracking tetap ada, §9; undangan grup memakai kode atau tautan, Fase 8);
- fitur sosial (feed, like, komentar);
- fitur "family" yang terpisah dari Group.

---

# 38. Fase Pengembangan 2–9

Bagian ini menjelaskan scope tiap fase lanjutan. Sebelum fase dimulai, requirement rinci, skema data, dan acceptance criteria fase tersebut harus dilengkapi di sini.

## Fase 2 — Place Detection

Mendeteksi saat pengguna berhenti cukup lama di satu area dan mencatatnya sebagai **visit**.

Contoh:

```text
08:15 - 16:30
LHI School
```

Scope:

- visit dideteksi dari location point yang terekam selama tracking, yaitu titik-titik yang berada dalam radius tertentu selama durasi minimum tertentu (nilai default ditetapkan di tiket);
- tabel baru untuk visit (waktu datang, waktu pergi, titik pusat);
- visit ditampilkan di Trip Detail sebagai marker tempat singgah dan sebagai daftar.

Catatan: pada fase ini visit hanya terdeteksi selama tracking aktif. Cakupannya menjadi lebih lengkap setelah trip otomatis tersedia di Fase 5.

---

## Fase 3 — Saved Places

Pengguna dapat membuat tempat bernama, misalnya:

```text
Rumah
Kantor
Sekolah
Gym
```

Scope:

- membuat, mengubah, dan menghapus tempat dengan memilih titik di peta dan menentukan radius;
- visit yang berada dalam radius sebuah tempat otomatis diberi nama tempat tersebut;
- tanpa reverse geocoding online; nama tempat selalu diisi pengguna.

---

## Fase 4 — Offline Map & Settings

Scope:

- pengguna memilih area di peta lalu mengunduhnya untuk penggunaan offline;
- daftar wilayah terunduh beserta ukurannya, dan opsi menghapus wilayah;
- wilayah yang sudah diunduh tampil tanpa internet;
- halaman Settings + DataStore: interval tracking, ambang accuracy, satuan jarak, preferensi peta.

Contoh:

```text
Offline Maps

Yogyakarta
Downloaded
184 MB

Surabaya
Not Downloaded

[ Download ]
```

Catatan: sumber tile harus mengizinkan unduhan offline. Lisensinya diperiksa di tiket fase ini.

---

## Fase 5 — Automatic Trip & Tracking Improvements

Scope:

- trip dimulai dan diakhiri otomatis berdasarkan pergerakan pengguna (misalnya dengan Activity Recognition Transition API);
- fitur trip otomatis dapat dinyalakan dan dimatikan, sedangkan Start/Stop manual tetap tersedia;
- melanjutkan trip yang terputus (§33);
- peredam jitter GPS saat pengguna diam (§12);
- optimasi baterai dan GPS.

Catatan permission: trip otomatis kemungkinan memerlukan `ACTIVITY_RECOGNITION` dan `ACCESS_BACKGROUND_LOCATION`. Kedua permission ini hanya diminta ketika pengguna mengaktifkan trip otomatis, dan keputusan di §9 ditinjau ulang di fase ini.

---

## Fase 6 — Account

Scope:

- backend Laravel + REST API;
- register, login, logout;
- hapus akun beserta seluruh data di server;
- device management: daftar perangkat yang sedang login dan logout perangkat dari jarak jauh;
- login bersifat opsional; tanpa login aplikasi tetap berfungsi seperti Fase 1–5.

Metode autentikasi ditetapkan di tiket awal fase.

---

## Fase 7 — Cloud Backup & Sync

Arsitektur:

```text
Room
 ↓
Sync
 ↓
Backend
```

Room tetap menjadi sumber data lokal.

Scope:

- opt-in dan hanya untuk pengguna yang login;
- trip yang sudah selesai beserta location point-nya diunggah ke server;
- trip yang dihapus di perangkat juga dihapus di server;
- restore data di perangkat baru setelah login;
- sync antarperangkat milik akun yang sama tanpa duplikasi (UUID trip, §24);
- sync berjalan di background hanya ketika ada jaringan dan tidak mengganggu tracking;
- saat Cloud Backup dimatikan, pengguna memilih apakah data di server ikut dihapus.

---

## Fase 8 — Group

Scope:

- membuat grup;
- mengundang anggota melalui kode atau tautan undangan;
- menerima undangan, keluar dari grup, dan mengeluarkan anggota (khusus owner);
- bergabung dengan grup **tidak** otomatis membagikan lokasi.

---

## Fase 9 — Live Sharing

Lokasi terkini pengguna dikirim ke backend lalu diteruskan ke anggota grup.

Arsitektur:

```text
GPS
 ↓
Room
 ↓
Sync
 ↓
Laravel
 ↓
Realtime
 ↓
Device lain
```

Scope:

- pengguna mengaktifkan Live Sharing per grup, dengan batas waktu opsional;
- anggota grup melihat posisi terkini dan waktu update terakhir dari anggota yang sedang berbagi;
- yang dibagikan hanya lokasi terkini, bukan riwayat trip;
- indikator yang jelas (foreground notification) selama Live Sharing aktif;
- frekuensi pengiriman memperhatikan baterai, dengan nilai default yang ditetapkan di tiket;
- saat offline, hanya lokasi terbaru yang dikirim ketika koneksi kembali; lokasi lama tidak dikirim ulang.

---

# 39. User Flow Utama (Fase 1)

## Memulai perjalanan

```text
Buka aplikasi
     ↓
Home
     ↓
Start Tracking
     ↓
Permission check
     ↓
Create Trip
     ↓
Start Foreground Service
     ↓
GPS tracking
     ↓
Save LocationPoint
```

---

## Mengakhiri perjalanan

```text
Tracking aktif
     ↓
Stop Tracking
     ↓
Stop Location Updates
     ↓
Calculate Statistics
     ↓
Complete Trip
     ↓
Trip Summary
```

---

## Melihat histori

```text
History
   ↓
Select Trip
   ↓
Trip Detail
   ├── Statistics
   ├── Route
   └── Speed Chart
```

---

# 40. Acceptance Criteria Fase 1

Fase 1 dianggap selesai apabila seluruh kondisi berikut terpenuhi. Acceptance criteria Fase 2–9 ditulis di §38 saat detail fase dilengkapi.

### Tracking

- Pengguna dapat menekan Start Tracking.
- Trip baru dibuat.
- Lokasi dapat direkam.
- Lokasi tersimpan ke Room.
- Tracking tetap berjalan saat aplikasi berada di background.
- Pengguna dapat menghentikan tracking.

### Location

Setiap titik minimal memiliki:

- latitude;
- longitude;
- recorded_at;
- accuracy.

Speed, bearing, dan altitude disimpan ketika tersedia.

Titik yang tidak lolos filter §12 tidak tersimpan.

### Trip

Setiap trip memiliki:

- start time;
- end time;
- duration;
- distance;
- average speed;
- max speed.

### History

- Pengguna dapat melihat daftar trip.
- Trip terbaru ditampilkan terlebih dahulu.
- Trip dapat dibuka.
- Trip yang sudah selesai dapat dihapus beserta seluruh location point-nya.

### Recovery

- Trip yang terputus karena proses dihentikan terdeteksi saat aplikasi dibuka.
- Trip yang terputus dapat diakhiri, dan statistiknya terhitung.

### Map

- Current location dapat ditampilkan.
- Trip route dapat digambar.
- Start dan finish dapat dibedakan.

### Speed

- Current speed dapat ditampilkan.
- Max speed dihitung.
- Average speed dihitung.
- Speed history dapat divisualisasikan.

### Offline

Tanpa koneksi internet:

- tracking tetap bekerja;
- GPS tetap dapat dicatat;
- Room tetap menyimpan data;
- history tetap dapat dibuka;
- statistik tetap tersedia;
- trip tetap dapat dibuat dan diselesaikan.

Peta offline belum termasuk Fase 1 (lihat Fase 4).

---

# 41. Definition of Done Fase 1

Fase 1 dianggap siap digunakan apabila pengguna dapat melakukan alur berikut dari awal sampai akhir:

```text
Buka aplikasi
↓
Melihat posisi
↓
Start Tracking
↓
Berkendara / berjalan
↓
Lock screen
↓
Tracking tetap berjalan
↓
Buka aplikasi
↓
Stop Tracking
↓
Melihat ringkasan
↓
Melihat rute
↓
Melihat grafik kecepatan
↓
Menutup aplikasi
↓
Membuka kembali aplikasi
↓
Trip sebelumnya masih tersedia
```

Semua proses tersebut harus dapat bekerja tanpa backend server.

---

# 42. Struktur Implementasi Awal

Struktur package yang disarankan:

```text
app/

├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/
│   │   └── entity/
│   │
│   └── repository/
│
├── domain/
│   ├── model/
│   └── usecase/
│
├── location/
│   ├── LocationTracker
│   └── LocationTrackingService
│
├── ui/
│   ├── home/
│   ├── history/
│   └── tripdetail/
│
├── navigation/
│
└── MainActivity.kt
```

Struktur backend ditetapkan pada Fase 6.

Arsitektur:

```text
Compose UI
     ↓
ViewModel
     ↓
Use Case / Repository
     ↓
┌───────────────┐
│               │
Room       Location Provider
```

---

# 43. Prioritas Implementasi Fase 1

Urutan fase mengikuti §6. Di dalam Fase 1, urutan implementasi yang disarankan:

```text
1. Project Android + Compose
2. Permission lokasi
3. Mendapatkan current location
4. Menampilkan current location
5. Room Database
6. Trip model
7. Start / Stop Tracking
8. Foreground Service
9. Penyimpanan location point
10. History
11. Trip statistics
12. Trip detail + speed history chart
13. MapLibre (peta Home + current location)
14. Route / Polyline di Trip detail
15. Recovery trip terputus
16. Battery dan GPS optimization dasar
```

Fokus pertama adalah memastikan pipeline:

```text
GPS
↓
Foreground Service
↓
Room
↓
History
```

stabil.

Map dan visualisasi dapat dikembangkan setelah data tracking bekerja dengan benar.

---

# 44. Keputusan Teknis

Keputusan awal proyek:

```text
Platform:
Android only

Language:
Kotlin

UI:
Jetpack Compose

Architecture:
MVVM / Repository

Location:
Fused Location Provider

Background:
Foreground Service

Database:
Room / SQLite

Map:
MapLibre

State:
ViewModel + Flow

Settings:
Konstanta di source code (Fase 1–3), DataStore (Fase 4+)

Timestamp:
Epoch millis UTC (Long)

ID trip:
UUID v4 dibuat di perangkat

Permission lokasi:
Foreground + presisi, tanpa background location (ditinjau di Fase 5)

Backend:
Laravel + REST API + realtime (Fase 6+)

Account:
Opsional (Fase 6+)

Cloud:
Backup dan sync opt-in (Fase 7+)

Multi-user:
Group + Live Sharing (Fase 8+)

Primary Design:
Offline-first
Local-first
Privacy-first
```

PRD ini menjadi dasar implementasi seluruh fase. Sebelum sebuah fase dimulai, detail requirement fase tersebut di §38 harus dilengkapi dan direview. Perubahan requirement harus dicatat di §45 dan dinilai dampaknya terhadap kestabilan tracking, penggunaan baterai, privasi, dan kompleksitas aplikasi.

---

# 45. Riwayat Revisi

## v2.1 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §17 | Rumus jarak memakai haversine, bukan `Location.distanceBetween` | Logika statistik dapat dites di JVM; selisih < 0,5% (TICKET-003) |
| §22 | Sumber tile ditetapkan: OpenFreeMap `liberty` | Tanpa akun/API key (TICKET-008) |

## v2.0 — 2026-10-03

Konsep MVP dihapus. Scope project adalah produk penuh dalam 9 fase.

| Bagian | Perubahan | Alasan |
|---|---|---|
| §1–§6 | Ringkasan, tujuan, prinsip, platform, dan pengguna mencakup seluruh fase; §6 berisi tabel fase | Scope menjadi produk penuh |
| §3, §35 | Privacy-first dijabarkan per fase; upload lokasi hanya untuk pengguna login + opt-in | Fitur cloud dan sharing masuk scope |
| §6, §38 | Account (Fase 6) dipindah sebelum Cloud Backup (Fase 7) | Backup ke server butuh identitas pemilik data |
| §15, §24, §25 | ID trip menjadi UUID; `updated_at` dikembalikan; index unik (`trip_id`, `recorded_at`) | Persiapan sync sejak awal, tanpa migrasi primary key |
| §26, §38 | Settings + DataStore masuk Fase 4 | Sebelumnya di luar scope |
| §33, §38 | Melanjutkan trip terputus dan peredam jitter masuk Fase 5 | Sebelumnya di luar scope |
| §36 | Requirement network, integritas sync, dan security ditambahkan | Fitur backend masuk scope |
| §37 | Hanya berisi fitur yang tidak dikerjakan di fase mana pun | Fitur lain sudah masuk fase |
| §39–§43 | Diberi label Fase 1 | Konsistensi |

## v1.1 — 2026-10-02

Penyelarasan detail dengan §1–§5. Tujuan dan scope tidak berubah.

| Bagian | Perubahan | Alasan |
|---|---|---|
| §4, §26, §44 | DataStore ditunda, default dijadikan konstanta di source code | Simple-first: belum ada halaman Settings |
| §7.1, §11 | Request lokasi akurasi tinggi hanya saat tracking atau Home terlihat | Battery-aware |
| §8.1 | Hanya satu trip aktif; urutan pengecekan permission diperjelas | Integritas data |
| §8.2, §9 | Stop dari notification memakai logika yang sama dengan Home | Reliability |
| §9, §28 | Tidak meminta background location; lokasi presisi wajib | Privacy-first, simple-first |
| §10, §25, §40 | Nama field waktu diseragamkan menjadi `recorded_at` (waktu fix GPS) | Konsistensi |
| §12 | Ambang filter konkret; titik yang ditolak tidak disimpan | Statistik sederhana dan konsisten |
| §14 | Aturan tampilan speed kosong, diam, dan basi | Kejelasan UI |
| §15, §24, §32, §33 | Status hanya `active`/`completed`; terputus = `active` tanpa service | Simple-first |
| §16, §17 | Rumus statistik ditetapkan; rata-rata = jarak ÷ durasi | Konsisten dengan contoh di §16 |
| §21 | Grafik speed dengan Compose Canvas | Simple-first, tanpa dependensi tambahan |
| §22, §35 | Sumber tile online tanpa akun + catatan privasi tile | Privacy-first, transparansi |
| §24, §25 | Tipe data, nullability, dan index ditetapkan; `created_at`/`updated_at` dihapus | Simple-first, tidak ada sync |
| §34 | Trip aktif tidak dapat dihapus | Integritas data |
| §43 | MapLibre dipindah sebelum Route; Offline map ke V0.4 | Route butuh MapLibre |
