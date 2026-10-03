# Product Requirements Document — Map Track

**Versi:** 2.7 (2026-10-03). Scope project adalah produk penuh yang dikerjakan dalam 9 fase (§6). Daftar perubahan ada di [§45 Riwayat Revisi](#45-riwayat-revisi).

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
12. Memakai peta tanpa internet di area yang sudah pernah dibuka (cache peta).
13. Mencatat perjalanan secara otomatis tanpa menekan Start.
14. Membackup data perjalanan ke cloud dan memakainya di perangkat lain.
15. Membuat grup dan berbagi lokasi secara realtime dengan anggota grup.
16. Mengukur waktu akselerasi kendaraan (0–100/200/300/400/500 m dan 0–100 km/jam).

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

Setiap fase fokus pada scope-nya dan harus selesai (seluruh tiket selesai, unit test, build, dan lint lulus) sebelum fase berikutnya dimulai. Uji manual di HP dilakukan sekali setelah seluruh fase selesai, mengikuti checklist uji manual setiap fase. Fitur kompleks (cloud, grup, live sharing) tidak dikerjakan sebelum fondasi tracking lokal stabil.

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
- Activity Recognition Transition API (mulai Fase 5, §38)
- Retrofit + OkHttp + kotlinx.serialization (mulai Fase 6, §38)
- WorkManager untuk sync background (mulai Fase 7, §38)

## Backend (mulai Fase 6)

- Laravel
- REST API untuk akun, sync, dan grup (`/api/v1`)
- PostgreSQL, Redis
- Kanal realtime: Laravel Reverb (WebSocket) untuk live sharing
- Autentikasi: email + password, token Laravel Sanctum per perangkat

Rincian dan alasannya ada di §38 Fase 6. Versi persis dan penyedia VPS ditetapkan di tiket awal Fase 6.

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
| 4 | Offline Map & Settings | Cache peta offline, halaman Settings | Tidak |
| 5 | Automatic Trip & Tracking Improvements | Trip otomatis, lanjutkan trip terputus, peredam jitter, uji akselerasi | Tidak |
| 6 | Account | Backend, register/login, device management | Ya |
| 7 | Cloud Backup & Sync | Backup, restore, sync antarperangkat | Ya |
| 8 | Group | Grup, undangan, keanggotaan | Ya |
| 9 | Live Sharing | Lokasi realtime ke anggota grup | Ya |

Account (Fase 6) dikerjakan sebelum Cloud Backup (Fase 7) karena backup ke server membutuhkan identitas pemilik data.

Bagian §7–§36 dan §39–§43 menjelaskan Fase 1 secara rinci. Fase 2–9 dijelaskan rinci di §38, termasuk skema data, nilai default, dan acceptance criteria masing-masing. Perubahan detail sebuah fase dicatat di §45 sebelum fase tersebut dikerjakan.

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

Aplikasi **tidak** meminta permission background location (`ACCESS_BACKGROUND_LOCATION`). Service selalu dimulai dari aksi pengguna saat aplikasi berada di foreground, sehingga permission lokasi foreground ditambah foreground service bertipe `location` sudah cukup. Ini sejalan dengan prinsip privacy-first. Pengecualiannya hanya trip otomatis di Fase 5: `ACCESS_BACKGROUND_LOCATION` diminta hanya saat pengguna mengaktifkan fitur tersebut (§38 Fase 5). Tracking manual dan Live Sharing tetap tanpa permission ini.

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

**Catatan privasi:** saat mengunduh tile, server tile dapat mengetahui area peta yang sedang dilihat. Namun aplikasi tidak pernah mengirim koordinat GPS, titik lokasi, atau data trip ke server mana pun. Untuk area yang tile-nya sudah ada di cache (Fase 4), peta dapat tampil tanpa request tile.

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

Tile yang pernah ditampilkan disimpan di cache peta sehingga area yang sudah pernah dibuka tetap tampil tanpa internet. Tidak ada unduhan wilayah (lisensi OpenFreeMap, §38 Fase 4).

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

DataStore ditambahkan bersama halaman Settings pada Fase 4. Daftar key, pilihan, dan default ada di §38 Fase 4:

```text
tracking_interval_ms
accuracy_threshold_m
distance_unit
map_follow_location
```

---

# 27. Navigation

Navigation utama Fase 1–2:

```text
Home
History
```

Tab **Tempat** ditambahkan di Fase 3 dan tab **Grup** di Fase 8. Settings dibuka dari top app bar Home mulai Fase 4 (§38).

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
| 4 | Request tile peta (sama seperti Fase 1–3; tile yang sudah ada di cache tidak diminta ulang) |
| 5 | Sama dengan Fase 4 |
| 6 | Data akun (email, nama, info perangkat). Tidak ada data lokasi. |
| 7 | Trip dan location point, **hanya** jika pengguna mengaktifkan Cloud Backup |
| 8 | Data grup dan keanggotaan. Tidak ada data lokasi. |
| 9 | Lokasi terkini (latitude, longitude, accuracy, waktu fix), **hanya** ke anggota grup dan **hanya** selama pengguna mengaktifkan Live Sharing. Server hanya menyimpan satu lokasi terakhir dan menghapusnya saat sharing berhenti |

Request tile peta tidak membawa koordinat GPS maupun data trip.

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

Bagian ini berisi requirement rinci, skema data, nilai default, dan acceptance criteria tiap fase lanjutan. Semua keputusan di sini tunduk pada §1–§3: offline-first, local-first, privacy-first, battery-aware, dan bertahap.

Konvensi yang berlaku di semua fase (sama dengan Fase 1, §24):

- Waktu dalam epoch millis UTC (`Long`), kecepatan dalam m/s, jarak dalam meter.
- Nilai default berupa konstanta di satu objek konfigurasi per fitur (mengikuti pola `TrackingConfig`), bukan angka tersebar.
- Perubahan skema Room selalu lewat migrasi berversi dan skema terekspor. Data lama tidak boleh hilang.
- Tiket boleh menyesuaikan nilai default setelah pengujian di perangkat. Perubahan tersebut dicatat di §45.

---

## Fase 2 — Place Detection

### Tujuan

Mendeteksi saat pengguna berhenti cukup lama di satu area dan mencatatnya sebagai **visit**. Ini memenuhi tujuan §2 poin 11 (bagian deteksi) dan kebutuhan §5 "mengetahui berapa lama berada di suatu tempat".

Contoh tampilan (nama tempat baru tersedia di Fase 3):

```text
08:15 - 16:30
Tempat singgah · 8 jam 15 menit
```

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Kapan deteksi berjalan | Sekali saat trip diselesaikan (Stop, Stop dari notification, atau Akhiri Trip terputus), di transaksi yang sama dengan statistik (§8.2) | Battery-aware: tidak ada kerja tambahan selama tracking; logika dapat dites di JVM |
| Visit pada trip aktif | Tidak ditampilkan | Konsisten dengan statistik yang juga baru ada setelah trip selesai |
| Sifat data visit | **Data turunan** dari location point; dapat dihitung ulang kapan saja | Location point tetap sumber kebenaran (local-first); algoritma dapat diperbaiki tanpa kehilangan data |
| Sinkronisasi visit (Fase 7) | Tidak disinkronkan; setiap perangkat menghitung ulang dari location point | Mengurangi kompleksitas sync |
| Sumber titik | Semua location point tersimpan (sudah lolos filter §12) | Tidak perlu filter tambahan |

### Nilai default (`PlaceDetectionConfig`)

| Parameter | Default | Arti |
|---|---|---|
| `VISIT_RADIUS_METERS` | 100 m | Titik masih dianggap di tempat yang sama bila berjarak ≤ radius ini dari pusat cluster |
| `MIN_VISIT_DURATION_MS` | 5 menit | Durasi minimum agar cluster menjadi visit |
| `MERGE_GAP_MS` | 5 menit | Dua visit berurutan digabung bila jeda antar-keduanya ≤ nilai ini |
| `MERGE_DISTANCE_METERS` | 100 m | ... dan jarak antar-pusatnya ≤ nilai ini |
| `DETECTION_VERSION` | 1 | Versi algoritma; dinaikkan bila algoritma berubah agar trip lama dihitung ulang |

### Algoritma

Input: location point satu trip, urut `recorded_at`. Jarak memakai haversine (§17).

1. Mulai cluster dari titik pertama yang belum diproses. Pusat cluster = rata-rata latitude/longitude anggotanya.
2. Tambahkan titik berikutnya ke cluster selama jaraknya ke pusat cluster ≤ `VISIT_RADIUS_METERS`. Pusat dihitung ulang setiap ada anggota baru.
3. Saat titik berikutnya keluar radius (atau titik habis), cluster ditutup. Bila `recorded_at` anggota terakhir − `recorded_at` anggota pertama ≥ `MIN_VISIT_DURATION_MS`, cluster menjadi visit. Proses diulang dari titik yang keluar radius.
4. Visit berurutan yang memenuhi `MERGE_GAP_MS` dan `MERGE_DISTANCE_METERS` digabung. Ini meredam satu-dua titik melenceng di tengah singgahan.

Hasil per visit: `arrived_at` = titik pertama, `departed_at` = titik terakhir, pusat = rata-rata seluruh titik visit, `point_count`.

Aturan kasus tepi:

| Kasus | Perilaku |
|---|---|
| GPS hilang selama singgah (misalnya di dalam gedung), lalu titik berikutnya masih dalam radius | Visit berlanjut; jeda waktu tanpa titik ikut dihitung sebagai durasi |
| Trip diawali dengan diam | Visit tercatat dengan `arrived_at` = titik pertama trip (waktu datang sebenarnya tidak diketahui) |
| Trip diakhiri dengan diam | Visit tercatat dengan `departed_at` = titik terakhir trip |
| Trip < 2 titik | Tidak ada visit |
| Trip lama (dibuat sebelum Fase 2) | Dihitung otomatis di background sekali saat aplikasi dibuka, untuk trip `completed` dengan `visit_detection_version` < `DETECTION_VERSION` |

### Skema data

Migrasi Room versi 1 → 2.

Tabel baru `visits`:

| Field | Tipe | Aturan |
|---|---|---|
| `id` | Long | Primary key, auto-increment (lokal; data turunan) |
| `trip_id` | String | Wajib, FK → `trips.id`, `ON DELETE CASCADE` |
| `arrived_at` | Long | Wajib |
| `departed_at` | Long | Wajib, ≥ `arrived_at` |
| `center_latitude` | Double | Wajib |
| `center_longitude` | Double | Wajib |
| `point_count` | Int | Wajib, ≥ 1 |

Index: `trip_id`, dan unik (`trip_id`, `arrived_at`).

Kolom baru di `trips`: `visit_detection_version` (Int, wajib, default `0`). Diisi `DETECTION_VERSION` setelah visit trip tersebut dihitung. Kolom ini tidak mengubah `updated_at` karena bukan perubahan data milik pengguna.

Menghitung ulang visit sebuah trip = hapus semua visit trip tersebut lalu insert hasil baru, dalam satu transaksi.

### Tampilan

- **Trip Detail — peta:** marker tempat singgah, berbeda bentuk/warna dari marker start dan finish (§20).
- **Trip Detail — daftar "Tempat singgah":** di bawah ringkasan. Tiap item menampilkan jam datang–pergi dan durasi. Menekan item memusatkan peta ke marker tersebut.
- Trip tanpa visit tidak menampilkan bagian daftar.
- History tidak berubah.

### Acceptance Criteria Fase 2

- Trip yang diselesaikan lewat Stop, Stop dari notification, maupun Akhiri Trip menghasilkan visit sesuai algoritma, dalam transaksi yang sama dengan statistik.
- Diam ≥ 5 menit dalam radius 100 m menghasilkan tepat satu visit; diam < 5 menit tidak menghasilkan visit (boundary).
- Satu titik melenceng di tengah singgahan tidak memecah visit menjadi dua.
- Trip dengan < 2 titik tidak menghasilkan visit dan tidak menyebabkan error (failure case).
- Menghapus trip ikut menghapus visit-nya.
- Trip yang dibuat sebelum Fase 2 mendapat visit setelah aplikasi dibuka, tanpa menghapus data apa pun; migrasi 1 → 2 dites.
- Visit tampil di Trip Detail sebagai marker dan sebagai daftar.
- Semua berjalan tanpa internet.

### Tidak termasuk Fase 2

Nama tempat (Fase 3), visit di luar tracking (Fase 5), visit pada trip aktif, statistik visit lintas trip (Fase 3).

---

## Fase 3 — Saved Places

### Tujuan

Pengguna membuat tempat bernama (misalnya Rumah, Kantor, Sekolah, Gym). Visit yang berada di dalam radius tempat tersebut otomatis diberi namanya. Ini memenuhi tujuan §2 poin 11 (bagian memberi nama).

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Cara visit diberi nama | Dicocokkan saat ditampilkan (bukan disimpan sebagai kolom di `visits`) | Mengubah atau menghapus tempat langsung berlaku ke semua visit lama; visit tetap data turunan |
| Aturan cocok | Pusat visit berada dalam radius tempat (haversine ≤ `radius_meters`). Bila lebih dari satu tempat cocok, dipilih tempat dengan pusat terdekat | Deterministik dan sederhana |
| Tempat saling tumpang tindih | Diizinkan | Pengguna bebas; aturan "terdekat" menyelesaikan konflik |
| Nama tempat | Selalu diisi pengguna; tanpa reverse geocoding online (§37) | Privacy-first |
| ID tempat | UUID v4 dibuat di perangkat, `updated_at` dipelihara | Tempat adalah data milik pengguna yang disinkronkan di Fase 7 (Rule sync-ready seperti trip) |

### Nilai default (`PlaceConfig`)

| Parameter | Default |
|---|---|
| `DEFAULT_RADIUS_METERS` | 100 m |
| `MIN_RADIUS_METERS` / `MAX_RADIUS_METERS` | 50 m / 1 000 m |
| `NAME_MAX_LENGTH` | 50 karakter |

### Skema data

Migrasi Room versi 2 → 3. Tabel baru `places`:

| Field | Tipe | Aturan |
|---|---|---|
| `id` | String | Primary key, UUID v4 |
| `name` | String | Wajib, di-trim, 1–50 karakter, tidak harus unik |
| `latitude` | Double | Wajib |
| `longitude` | Double | Wajib |
| `radius_meters` | Double | Wajib, 50–1 000 |
| `created_at` | Long | Wajib |
| `updated_at` | Long | Wajib, diperbarui setiap baris berubah |

Tidak ada relasi FK dari `visits` ke `places`.

### Fitur

1. **Daftar tempat:** tab navigasi baru **Tempat** (navigasi utama menjadi Home, History, Tempat). Menampilkan nama, radius, jumlah kunjungan, dan kunjungan terakhir. Diurutkan berdasarkan nama.
2. **Buat tempat:** pilih titik dengan menggeser peta (pin tetap di tengah layar) atau tombol "Pakai lokasi saat ini"; atur radius dengan slider; lingkaran radius tampil di peta; isi nama. Juga dapat dibuat dari item visit di Trip Detail lewat aksi **Simpan sebagai tempat** (titik = pusat visit).
3. **Ubah tempat:** nama, titik, dan radius.
4. **Hapus tempat:** dengan konfirmasi. Visit tidak ikut terhapus, hanya kehilangan nama.
5. **Detail tempat:** peta dengan lingkaran radius, total kunjungan, total durasi, dan daftar kunjungan (tanggal, jam datang–pergi, durasi; menekan item membuka Trip Detail terkait). Data diambil dari visit semua trip yang cocok dengan tempat ini.
6. **Trip Detail:** visit yang cocok menampilkan nama tempat; yang tidak cocok tetap "Tempat singgah".

Membuat tempat tidak membutuhkan permission lokasi kecuali saat memakai "Pakai lokasi saat ini". Peta yang tidak termuat (offline) tidak menghalangi pembuatan tempat dari visit.

### Acceptance Criteria Fase 3

- Pengguna dapat membuat, mengubah, dan menghapus tempat tanpa internet.
- Nama kosong atau > 50 karakter ditolak; radius di luar 50–1 000 m tidak dapat dipilih (boundary).
- Visit yang pusatnya tepat di dalam radius diberi nama tempat; yang di luar radius tidak.
- Dua tempat yang tumpang tindih: visit diberi nama tempat dengan pusat terdekat.
- Mengubah radius atau menghapus tempat langsung memperbarui nama visit di Trip Detail dan Detail Tempat.
- Menghapus tempat tidak menghapus trip maupun visit (failure case: tidak ada data hilang).
- Detail tempat menampilkan total kunjungan dan total durasi yang sesuai dengan visit.
- Migrasi 2 → 3 dites.

### Tidak termasuk Fase 3

Deteksi Home/Work otomatis, reverse geocoding, geofence alert (§37), kategori/ikon tempat, impor tempat.

---

## Fase 4 — Offline Map & Settings

### Tujuan

Peta tetap tampil tanpa internet untuk area yang sudah pernah dibuka (tujuan §2 poin 12), dan pengguna dapat mengatur parameter utama lewat halaman Settings.

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Mekanisme offline | Ambient cache MapLibre (tile yang pernah ditampilkan), batas `MapConfig.MAP_CACHE_MAX_BYTES` = 200 MB | Gratis, tanpa dependensi baru, dan sesuai lisensi |
| Unduhan wilayah | **Tidak ada.** Terms of Service OpenFreeMap melarang pengambilan data otomatis tanpa izin, dan unduhan wilayah meminta ribuan tile sekaligus. Sumber tile lain yang gratis tanpa API key dan jelas mengizinkan unduhan offline tidak ditemukan (verifikasi TICKET-023) | Kepatuhan lisensi; prinsip gratis |
| Settings | DataStore Preferences (§26) | Sesuai keputusan v1.1 |
| Perubahan setting tracking saat trip aktif | Berlaku mulai trip berikutnya; Settings menampilkan keterangan ini | Satu trip memakai parameter yang konsisten |

### Cache peta offline

1. Setiap tile yang pernah ditampilkan disimpan otomatis di cache MapLibre (ambient cache) hingga `MapConfig.MAP_CACHE_MAX_BYTES` (200 MB). Bila penuh, tile yang paling lama tidak dipakai dibuang lebih dulu.
2. Area yang sudah pernah dibuka di peta (Home, Trip Detail, Tempat) tetap tampil tanpa internet selama tile-nya masih ada di cache. Area yang belum pernah dibuka tampil kosong saat offline.
3. Settings menyediakan aksi **Hapus cache peta** dengan konfirmasi; tile diunduh ulang saat peta dibuka dengan internet.
4. Tidak ada halaman unduh wilayah: unduhan massal dari server publik OpenFreeMap tanpa izin bertentangan dengan Terms of Service-nya (larangan mengambil data secara otomatis tanpa izin). Fitur unduh wilayah dapat ditambahkan bila OpenFreeMap memberi izin tertulis.
5. Cache peta tidak memengaruhi tracking; peta kosong tetap tidak menghentikan tracking.

### Settings

| Key DataStore | Pilihan | Default |
|---|---|---|
| `tracking_interval_ms` | 3 s, 5 s, 10 s, 30 s | 5 s |
| `accuracy_threshold_m` | 20 m, 30 m, 50 m, 100 m | 50 m |
| `distance_unit` | Metrik (km, km/h) / Imperial (mi, mph) | Metrik |
| `map_follow_location` | Kamera Home mengikuti posisi: aktif/nonaktif | Aktif |

Aturan:

- Database tetap menyimpan meter dan m/s (§13). Satuan hanya memengaruhi tampilan.
- Batas GPS jump dan stale timeout tetap konstanta di `TrackingConfig`.
- Parameter tracking dibaca saat Start dan dipakai sampai trip selesai.
- Settings juga berisi bagian **Tentang**: versi aplikasi, atribusi peta, dan pernyataan privasi singkat ("Data lokasi hanya disimpan di perangkat").
- Settings dibuka dari ikon di top app bar Home.

### Acceptance Criteria Fase 4

- Cache peta dikonfigurasi 200 MB saat peta pertama dibuat (boundary: tidak melebihi batas ini).
- Dengan mode pesawat, area yang sebelumnya dibuka dengan internet tetap tampil; area lain kosong tetapi tracking tetap berjalan (failure case).
- Hapus cache peta mengosongkan cache; aplikasi dan tracking tetap berjalan normal.
- Mengubah interval saat trip aktif tidak mengubah trip yang sedang berjalan; trip berikutnya memakai interval baru.
- Ambang accuracy baru dipakai filter §12 pada trip berikutnya.
- Satuan imperial mengubah semua tampilan jarak dan kecepatan tanpa mengubah data tersimpan.
- Setting bertahan setelah aplikasi ditutup dan dibuka kembali.

### Tidak termasuk Fase 4

Pilihan gaya peta lain, sinkronisasi setting antarperangkat, unduhan wilayah (manual maupun otomatis).

---

## Fase 5 — Automatic Trip & Tracking Improvements

### Tujuan

Mencatat perjalanan tanpa menekan Start (tujuan §2 poin 13), melanjutkan trip terputus, meredam jitter GPS, dan menghemat baterai.

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Pemicu start otomatis | Activity Recognition Transition API: `ENTER` aktivitas `IN_VEHICLE` atau `ON_BICYCLE`. `WALKING`/`RUNNING` hanya bila pengguna mengaktifkan opsi "Termasuk berjalan kaki" (default nonaktif) | Berjalan di dalam rumah terlalu sering memicu trip palsu |
| Default trip otomatis | **Nonaktif** (opt-in) | Privacy-first, battery-aware |
| Permission | `ACTIVITY_RECOGNITION` dan `ACCESS_BACKGROUND_LOCATION` diminta **hanya** saat pengguna mengaktifkan trip otomatis, masing-masing dengan penjelasan terlebih dahulu. Jika salah satu ditolak, trip otomatis tetap nonaktif | Revisi §9: tracking manual tetap tanpa background location |
| Dasar hukum start dari background | Android mengizinkan foreground service dimulai dari background ketika aplikasi menerima event activity recognition transition; tipe `location` dari background membutuhkan background location | Persyaratan platform |
| Asal trip | Kolom baru `trips.source`: `manual` / `auto` | History menandai trip otomatis |
| Start otomatis saat ada trip `active` | Diabaikan | Satu trip aktif (§8.1) |
| Trip manual | Tidak pernah dihentikan otomatis | Kendali tetap di pengguna |

### Nilai default (`AutoTripConfig`)

| Parameter | Default | Arti |
|---|---|---|
| `AUTO_STOP_STILL_MS` | 5 menit | Trip otomatis berhenti bila diam selama ini |
| `AUTO_STOP_RADIUS_METERS` | 100 m | "Diam" = semua titik dalam rentang `AUTO_STOP_STILL_MS` terakhir berada dalam radius ini, atau Activity Recognition melaporkan `STILL` tanpa transisi gerak sejak itu |
| `MIN_AUTO_TRIP_DISTANCE_METERS` | 300 m | Trip otomatis yang lebih pendek dihapus otomatis setelah berhenti |
| `MIN_AUTO_TRIP_DURATION_MS` | 2 menit | ... atau yang lebih singkat dari ini |
| `RESUME_MAX_GAP_MS` | 60 menit | Batas jeda agar trip terputus masih dapat dilanjutkan |
| `STATIONARY_INTERVAL_MS` | 30 detik | Interval GPS saat terdeteksi diam selama tracking |
| `STATIONARY_SPEED_MPS` | 0,5 m/s | Ambang kecepatan "diam" untuk penghemat baterai |

### Trip otomatis

1. Toggle **Trip otomatis** di Settings, beserta opsi "Termasuk berjalan kaki".
2. Saat transisi gerak diterima dan tidak ada trip aktif, aplikasi memulai trip `source = auto` dan foreground service seperti §8.1 langkah 5–9.
3. Notification menampilkan "Perjalanan otomatis" dan tombol Stop. Stop manual berlaku normal.
4. Saat kondisi diam terpenuhi, trip otomatis diakhiri seperti Stop (§8.2) dengan `ended_at` = `recorded_at` titik terakhir. Durasi trip ikut mencakup waktu diam tersebut, dan titik diam tetap dipakai deteksi visit (singgahan di tujuan tercatat).
5. Trip otomatis di bawah batas minimum jarak atau durasi dihapus beserta titiknya, tanpa notifikasi.
6. History dan Trip Detail menampilkan label "Otomatis" untuk trip `source = auto`.
7. Mematikan toggle menghentikan langganan transisi; trip otomatis yang sedang berjalan tetap berjalan sampai dihentikan.
8. Bila permission dicabut dari pengaturan sistem, toggle otomatis menjadi nonaktif saat aplikasi dibuka berikutnya, disertai keterangan.

### Melanjutkan trip terputus

Dialog §33 mendapat tombol **Lanjutkan** di samping **Akhiri Trip**:

- Hanya tersedia bila `recorded_at` titik terakhir (atau `started_at` bila tidak ada titik) ≤ `RESUME_MAX_GAP_MS` dari sekarang. Di luar batas itu hanya Akhiri Trip yang tersedia.
- Lanjutkan menjalankan foreground service untuk trip yang sama, dengan pemeriksaan permission dan layanan lokasi seperti §8.1.
- Jeda tanpa titik diperlakukan sama seperti kehilangan sinyal GPS (§31): jarak antara titik terakhir dan titik pertama setelah lanjut dihitung garis lurus.
- Invarian DEC-001 tetap berlaku: state `Active` ditetapkan sebelum service berjalan.

### Peredam jitter

- Titik tetap disimpan semua (deteksi visit membutuhkan titik saat diam).
- Perhitungan jarak (§17) memakai **titik jangkar**: jarak hanya ditambahkan bila jarak dari titik jangkar ke titik baru > `max(accuracy jangkar, accuracy titik baru)`; titik baru itu lalu menjadi jangkar. Titik pertama adalah jangkar awal.
- Titik yang kecepatan GPS-nya diketahui dan < `STATIONARY_SPEED_MPS` tidak menambah jarak dan tidak menjadi jangkar. Tanpa syarat ini, simulasi diam 10 menit dengan accuracy 10–30 m tetap menambah ratusan meter (DEC-008).
- Hanya berlaku untuk trip yang diselesaikan setelah Fase 5. Statistik trip lama tidak dihitung ulang agar histori tetap stabil.

### Penghemat baterai

- Selama tracking, bila kecepatan < `STATIONARY_SPEED_MPS` dan titik tetap dalam radius 100 m selama 2 menit, interval request lokasi diperpanjang ke `STATIONARY_INTERVAL_MS`. Prioritas **tetap** `HIGH_ACCURACY`: dengan `BALANCED_POWER_ACCURACY` fix datang jarang atau sangat kasar sehingga gerakan tidak terdeteksi dan sisa perjalanan tidak terekam (uji emulator, DEC-010).
- Kembali ke interval normal begitu titik keluar radius atau kecepatan ≥ `STATIONARY_SPEED_MPS`.
- Langganan Activity Recognition memakai Transition API (berbasis event), bukan polling.

### Skema data

Migrasi Room 3 → 4: kolom `trips.source` (String, wajib, default `manual` untuk data lama).

### Acceptance Criteria Fase 5

- Dengan trip otomatis aktif, mulai berkendara memulai trip tanpa membuka aplikasi; berhenti ≥ 5 menit mengakhirinya.
- Trip otomatis 250 m atau 90 detik dihapus otomatis; 300 m dan 2 menit dipertahankan (boundary).
- Menolak `ACCESS_BACKGROUND_LOCATION` membuat trip otomatis tetap nonaktif, sementara tracking manual tetap berfungsi (failure case).
- Trip manual tidak pernah dihentikan otomatis.
- Trip terputus 30 menit lalu dapat dilanjutkan; 90 menit lalu hanya dapat diakhiri.
- Rekaman uji diam 10 menit dengan jitter GPS (accuracy 10–30 m) menambah jarak < 50 m.
- Interval GPS turun saat diam dan kembali normal saat bergerak.
- Migrasi 3 → 4 dites; trip lama berlabel manual.

### Uji akselerasi (tambahan v2.6)

Pengguna mengukur berapa detik yang dibutuhkan dari diam untuk menempuh 100, 200, 300, 400, dan 500 m, serta untuk mencapai 100 km/jam (tujuan §2 poin 16).

| Keputusan | Nilai | Alasan |
|---|---|---|
| Letak | Halaman **Uji Akselerasi** dibuka dari Home; terpisah dari trip (tidak membuat trip, tidak menyimpan location point) | Mode ukur singkat dengan kebutuhan GPS berbeda |
| GPS | 1 fix per detik, `HIGH_ACCURACY`, hanya selama halaman terbuka (tanpa foreground service); layar dijaga tetap menyala | Resolusi waktu cukup; battery-aware karena hanya sesaat |
| Sumber hitungan | Kecepatan GPS (Doppler) tiap fix. Jarak = integral kecepatan terhadap waktu (trapesium), bukan selisih posisi | Posisi GPS berisik; kecepatan Doppler lebih stabil |
| Titik nol | Fix diam terakhir sebelum kecepatan ≥ ambang mulai | Sederhana; galat ≤ 1 detik interval GPS |
| Waktu di tiap target | Interpolasi dengan asumsi percepatan konstan di antara dua fix | Hasil per 0,01 detik walau GPS 1 Hz |
| Satuan target | Selalu meter dan km/jam seperti yang diminta; kecepatan langsung mengikuti setting satuan | Standar uji yang dikenal pengguna |
| Penyimpanan | Tabel lokal `acceleration_runs` (migrasi Room 4 → 5); hanya run yang selesai dengan minimal satu target tercapai | Riwayat hasil; tetap offline |
| Keselamatan | Halaman menampilkan peringatan untuk hanya menguji di tempat aman dan legal, tanpa mengoperasikan HP saat berkendara | Tanggung jawab produk |

Nilai default (`AccelerationConfig`):

| Parameter | Default | Arti |
|---|---|---|
| `GPS_INTERVAL_MS` | 1 000 | Interval GPS selama halaman terbuka |
| `MAX_ACCURACY_METERS` | 20 m | Siap mengukur hanya bila accuracy fix ≤ ini |
| `STILL_SPEED_MPS` | 0,5 m/s | Di bawah ini dianggap diam |
| `READY_STILL_MS` | 2 detik | Lama diam sebelum status Siap |
| `START_SPEED_MPS` | 1,0 m/s | Pengukuran dimulai saat kecepatan mencapai ini |
| `MAX_FIX_GAP_MS` | 3 detik | Jeda fix lebih lama → run gagal ("GPS terputus") |
| `STOP_BELOW_START_MS` | 2 detik | Kecepatan di bawah ambang mulai selama ini → run selesai |
| `MAX_RUN_MS` | 60 detik | Batas lama satu run |
| Target jarak | 100, 200, 300, 400, 500 m | |
| Target kecepatan | 100 km/jam | |

Alur: Menunggu GPS akurat → Berhenti total dulu → **Siap** → Mengukur (kecepatan, waktu, jarak langsung) → Selesai (tabel waktu per target; target yang tidak tercapai "—") atau Gagal. Run selesai saat semua target tercapai, kendaraan berhenti, pengguna menekan Berhenti, atau batas waktu habis.

Acceptance criteria:

- Akselerasi konstan 4 m/s² tersimulasi menghasilkan 0–100 km/jam ±0,1 detik dari 6,94 detik dan 0–100 m ±0,1 detik dari 7,07 detik.
- Pengukuran tidak dimulai sebelum diam ≥ 2 detik dengan accuracy ≤ 20 m (boundary).
- Jeda GPS > 3 detik saat mengukur membuat run gagal dan tidak disimpan (failure case).
- Target yang tidak tercapai ditampilkan "—"; hasil tersimpan dan dapat dihapus.
- Migrasi 4 → 5 dites.
- Tanpa izin lokasi presisi atau dengan Location mati, halaman menampilkan pesan dan tidak mengukur.

### Tidak termasuk Fase 5

Driving detection sebagai fitur tersendiri (§37), deteksi moda transportasi untuk ditampilkan, start otomatis tanpa izin pengguna.

---

## Fase 6 — Account

### Tujuan

Menyediakan identitas pemilik data sebagai dasar Cloud Backup, Group, dan Live Sharing. Login tetap opsional: tanpa akun aplikasi berfungsi persis seperti Fase 1–5.

### Keputusan teknis backend

| Area | Keputusan | Alasan |
|---|---|---|
| Framework | Laravel versi stabil terbaru saat tiket awal Fase 6 dibuat | §4 |
| Database server | PostgreSQL | Andal untuk data relasional dan volume titik lokasi |
| Cache/queue | Redis | Antrean email dan rate limiting |
| Realtime (dipakai Fase 9) | Laravel Reverb (WebSocket, protokol Pusher) | First-party Laravel, dapat di-host sendiri tanpa layanan pihak ketiga |
| Autentikasi | Email + password, token Laravel Sanctum per perangkat | Tanpa ketergantungan layanan login pihak ketiga |
| Verifikasi email | Wajib sebelum Cloud Backup dan Group dapat dipakai | Mencegah data tersimpan di akun dengan email salah ketik |
| Hosting | Satu VPS (region terdekat dengan pengguna), Docker Compose, HTTPS (TLS) wajib, enkripsi disk | Sederhana dan murah; data tidak diserahkan ke layanan pihak ketiga selain penyedia VPS |
| Versi API | Prefix `/api/v1` | Kompatibilitas aplikasi lama |
| Client HTTP | Retrofit + OkHttp + kotlinx.serialization | Standar Android, mudah dites |
| Penyimpanan token di perangkat | DataStore terenkripsi dengan kunci AES-GCM di Android Keystore | §36 Security |

### Fitur

1. **Register:** nama tampilan (1–50 karakter), email, password (min. 8 karakter). Email verifikasi dikirim.
2. **Login / logout.** Login membuat token baru untuk perangkat ini dan mendaftarkan perangkat.
3. **Logout:** menghapus token di server dan di perangkat. Data lokal **tidak** dihapus (local-first). Dialog logout menyediakan opsi "Hapus juga data di perangkat ini" (default tidak dicentang).
4. **Lupa password:** tautan reset via email.
5. **Device management:** daftar perangkat yang sedang login (nama model, versi aplikasi, terakhir aktif, penanda "perangkat ini"). Pengguna dapat me-logout perangkat lain. Perangkat yang di-logout jarak jauh mendeteksi `401` pada request berikutnya lalu kembali ke mode tanpa akun; data lokalnya tetap ada.
6. **Hapus akun:** konfirmasi dengan password. Server menghapus akun, perangkat, token, dan seluruh data (backup, keanggotaan grup, lokasi live) secara permanen dan segera. Grup milik pengguna dialihkan ke anggota terlama; grup tanpa anggota lain dihapus. Data lokal di perangkat tidak dihapus.
7. **Halaman Akun** dibuka dari Settings. Tanpa login, halaman ini menawarkan login/register beserta penjelasan bahwa akun bersifat opsional.

### Skema server

| Tabel | Field utama |
|---|---|
| `users` | `id`, `name`, `email` (unik), `email_verified_at`, `password`, `cloud_backup_enabled` (Fase 7), `created_at`, `updated_at` |
| `devices` | `id`, `user_id`, `name`, `platform`, `app_version`, `last_seen_at`, `created_at` |
| `personal_access_tokens` | Tabel Sanctum, satu token per `device_id` |

### Endpoint

| Method | Path | Fungsi |
|---|---|---|
| POST | `/auth/register` | Daftar |
| POST | `/auth/login` | Login, mengembalikan token + device id |
| POST | `/auth/logout` | Revoke token perangkat ini |
| POST | `/auth/forgot-password`, `/auth/email/resend` | Reset password, kirim ulang verifikasi |
| GET | `/me` | Profil dan status verifikasi |
| GET | `/devices` | Daftar perangkat |
| DELETE | `/devices/{id}` | Logout perangkat lain |
| DELETE | `/me` | Hapus akun (body: password) |

### Aturan

- Tidak ada data lokasi yang dikirim di fase ini (§35).
- Login dibatasi 5 percobaan per menit per email + IP.
- Request jaringan tidak pernah dijalankan di thread atau coroutine milik tracking. Kegagalan jaringan hanya memengaruhi halaman Akun.

### Acceptance Criteria Fase 6

- Tanpa login, seluruh fitur Fase 1–5 bekerja seperti sebelumnya.
- Register, verifikasi email, login, dan logout berfungsi.
- Password 7 karakter ditolak; 8 karakter diterima (boundary).
- Login dengan password salah menampilkan pesan error; percobaan ke-6 dalam satu menit ditolak (failure case).
- Logout tidak menghapus trip lokal kecuali opsi hapus dicentang.
- Logout jarak jauh membuat perangkat lain kembali ke mode tanpa akun pada request berikutnya.
- Hapus akun menghapus seluruh data server pengguna tersebut.
- Token tidak tersimpan dalam bentuk teks biasa di perangkat.
- Semua komunikasi memakai HTTPS.

### Tidak termasuk Fase 6

Login pihak ketiga (Google dan lain-lain), two-factor authentication, mengganti email, panel admin.

---

## Fase 7 — Cloud Backup & Sync

### Tujuan

Membackup data ke cloud dan memakainya di perangkat lain (tujuan §2 poin 14). Room tetap sumber data utama; server hanya salinan (local-first).

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Syarat | Login, email terverifikasi, dan Cloud Backup diaktifkan secara eksplisit. Default nonaktif | Privacy-first (§3) |
| Cakupan status Cloud Backup | Per akun (`users.cloud_backup_enabled`), bukan per perangkat | Semua perangkat akun yang sama berperilaku konsisten |
| Data yang disinkronkan | `trips` (hanya `completed`) beserta `location_points`, dan `places` | Data milik pengguna |
| Tidak disinkronkan | Trip `active`, `visits` (dihitung ulang dari titik), Settings, wilayah offline | Data turunan atau khusus perangkat |
| Mekanisme background | WorkManager dengan constraint jaringan tersedia | Retry dan backoff bawaan; tidak mengganggu tracking |
| Pemicu sync | Trip selesai, perubahan/hapus tempat, aplikasi dibuka, periodik setiap 6 jam | Data cepat tercadang tanpa polling agresif |
| Identitas data | Trip dan tempat: UUID; titik: (`trip_id`, `recorded_at`) | Upload idempoten, tanpa duplikasi (§36) |
| Konflik | Trip selesai bersifat immutable, sehingga satu-satunya perubahan adalah hapus (hapus selalu menang). Tempat: last-write-wins berdasarkan `updated_at`, termasuk tombstone hapus | Sederhana dan dapat diprediksi |
| Enkripsi | TLS saat transit dan enkripsi disk server. Tanpa end-to-end encryption | Kompleksitas E2E tidak sebanding untuk fase ini |

### Perubahan skema lokal

Migrasi Room 4 → 5:

| Tabel | Kolom baru | Arti |
|---|---|---|
| `trips` | `deleted_at` (Long?) | Tombstone: trip disembunyikan dari UI dan titiknya sudah dihapus, menunggu konfirmasi hapus dari server |
| `trips` | `synced_at` (Long?) | Waktu terakhir server mengonfirmasi trip ini; `NULL` = belum terupload |
| `places` | `deleted_at` (Long?), `synced_at` (Long?) | Sama seperti di atas |

Aturan hapus:

- Cloud Backup **nonaktif**: hapus permanen seperti Fase 1.
- Cloud Backup **aktif**: baris diberi `deleted_at`, titik/visit dihapus, lalu baris dihapus permanen setelah server mengonfirmasi.

### Alur sync

1. **Push:** trip `completed` dengan `synced_at IS NULL` diupload bersama titiknya (titik dikirim dalam chunk 1 000 lalu trip difinalisasi). Tempat dengan `updated_at > synced_at` atau belum pernah sync diupload. Tombstone dikirim sebagai hapus.
2. **Pull:** `GET /sync/changes?cursor=…` mengembalikan trip, tempat, dan penghapusan sejak cursor terakhir (cursor = versi monotonic yang diberikan server). Trip baru diunduh lengkap dengan titiknya, lalu visit dihitung ulang secara lokal.
3. Cursor disimpan di DataStore setelah satu batch pull berhasil diterapkan dalam transaksi.
4. Server melakukan upsert berdasarkan ID, sehingga mengirim ulang data yang sama tidak menghasilkan duplikat.

### Fitur

1. **Aktifkan Cloud Backup** (halaman Akun): dialog menjelaskan data apa yang diupload dan ke mana. Setelah aktif, seluruh trip `completed` dan tempat yang sudah ada diupload.
2. **Restore di perangkat baru:** setelah login, bila Cloud Backup akun aktif, aplikasi bertanya "Pulihkan data dari cloud?". Data diunduh dan digabung dengan data lokal (tidak menimpa).
3. **Status sync:** halaman Akun menampilkan waktu sync terakhir, jumlah data menunggu upload, dan error terakhir. Tersedia tombol "Sync sekarang".
4. **Matikan Cloud Backup:** pengguna memilih "Simpan data di server" atau "Hapus data di server". Data lokal tidak terpengaruh.
5. **Login ke akun berbeda** di perangkat yang datanya pernah dicadangkan ke akun lain: aplikasi menampilkan peringatan sebelum menggabungkan data lokal ke akun baru.

### Skema server

| Tabel | Field utama |
|---|---|
| `trips` | `id` (UUID), `user_id`, kolom statistik sama dengan §24, `source`, `version`, `deleted_at`, `updated_at` |
| `location_points` | `trip_id`, `latitude`, `longitude`, `accuracy`, `speed`, `bearing`, `altitude`, `recorded_at`; unik (`trip_id`, `recorded_at`) |
| `places` | `id` (UUID), `user_id`, `name`, `latitude`, `longitude`, `radius_meters`, `created_at`, `updated_at`, `version`, `deleted_at` |

Setiap query dibatasi `user_id` milik token (§36).

### Acceptance Criteria Fase 7

- Tanpa mengaktifkan Cloud Backup, tidak ada data lokasi yang terkirim (diverifikasi dengan inspeksi trafik).
- Trip selesai terupload saat ada jaringan; saat offline, trip diantre lalu terupload ketika koneksi kembali.
- Tracking tidak tersendat selama sync berjalan.
- Perangkat baru yang login dan memilih Pulihkan mendapatkan trip, rute, statistik, visit, dan tempat yang sama.
- Upload ulang trip yang sama (misalnya akibat retry) tidak menghasilkan trip atau titik ganda (boundary).
- Trip yang dihapus di perangkat A hilang dari perangkat B setelah sync.
- Edit tempat di dua perangkat: hasil akhir = edit dengan `updated_at` terbaru.
- Server tidak tersedia (5xx/timeout): data tetap di antrean, error tampil di status sync, tracking normal (failure case).
- Mematikan Cloud Backup dengan "Hapus data di server" menghapus seluruh data pengguna di server.
- Migrasi 4 → 5 dites.

### Tidak termasuk Fase 7

End-to-end encryption, sync Settings, berbagi trip ke pengguna lain, ekspor GPX/CSV.

---

## Fase 8 — Group

### Tujuan

Membuat grup dan mengelola keanggotaan sebagai dasar Live Sharing. Fase ini tidak mengirim data lokasi sama sekali.

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Syarat | Login dan email terverifikasi | Identitas anggota jelas |
| Batas anggota | Maksimal 20 anggota per grup; maksimal 10 grup per pengguna | Grup untuk keluarga/teman dekat, beban server dan baterai terkendali |
| Undangan | Kode 8 karakter (huruf besar + angka tanpa `0`, `O`, `1`, `I`), berlaku 7 hari, dapat dipakai berkali-kali sampai kedaluwarsa atau dicabut. Tautan `https://<domain>/join/<kode>` membuka aplikasi via Android App Links; bila aplikasi belum terpasang, halaman web menampilkan kode | Tanpa push notification (§37) |
| Bergabung | Pengguna memasukkan kode atau membuka tautan, melihat nama grup dan jumlah anggota, lalu menekan Gabung | Persetujuan eksplisit |
| Lokasi | Bergabung **tidak** membagikan lokasi | Privacy-first |
| Data offline | Daftar grup dan anggota disimpan sebagai cache read-only di Room; aksi yang butuh server dinonaktifkan saat offline | Fitur jaringan menangani offline dengan wajar (§3) |

### Peran dan aksi

| Aksi | Owner | Member |
|---|---|---|
| Lihat daftar anggota | Ya | Ya |
| Ganti nama grup | Ya | Tidak |
| Buat ulang / cabut kode undangan | Ya | Tidak |
| Keluarkan anggota | Ya | Tidak |
| Alihkan kepemilikan | Ya | Tidak |
| Hapus grup | Ya | Tidak |
| Keluar dari grup | Ya, setelah mengalihkan kepemilikan; bila satu-satunya anggota, keluar = hapus grup | Ya |

### Fitur

1. Tab navigasi **Grup** (navigasi utama menjadi Home, History, Tempat, Grup). Tanpa login, tab ini menampilkan penjelasan dan tombol login.
2. Buat grup: nama 1–50 karakter.
3. Detail grup: nama, daftar anggota (nama tampilan, peran, tanggal bergabung), kode undangan dan tombol bagikan (share sheet Android) untuk owner.
4. Gabung via kode atau tautan.
5. Konfirmasi untuk keluar, mengeluarkan anggota, dan hapus grup.

### Skema

Server:

| Tabel | Field utama |
|---|---|
| `groups` | `id` (UUID), `name`, `owner_id`, `created_at`, `updated_at` |
| `group_members` | `group_id`, `user_id`, `role` (`owner`/`member`), `joined_at`; unik (`group_id`, `user_id`) |
| `group_invites` | `group_id`, `code` (unik), `expires_at`, `revoked_at` |

Lokal (cache, migrasi Room 5 → 6): `groups` (`id`, `name`, `role`, `fetched_at`) dan `group_members` (`group_id`, `user_id`, `name`, `role`, `joined_at`). Cache dihapus saat logout.

### Acceptance Criteria Fase 8

- Pengguna dapat membuat grup, membagikan kode, dan pengguna lain dapat bergabung dengan kode atau tautan.
- Kode yang kedaluwarsa atau dicabut ditolak dengan pesan jelas (failure case).
- Anggota ke-21 ditolak; anggota ke-20 diterima (boundary).
- Hanya owner yang dapat mengeluarkan anggota, mengganti nama, dan menghapus grup (diuji juga di API, bukan hanya UI).
- Pengguna yang bukan anggota tidak dapat membaca data grup lewat API.
- Bergabung ke grup tidak mengirim data lokasi apa pun.
- Saat offline, daftar grup tetap tampil dari cache dan aksi server nonaktif.
- Migrasi 5 → 6 dites.

### Tidak termasuk Fase 8

Chat, feed, push notification, peran selain owner/member, grup publik.

---

## Fase 9 — Live Sharing

### Tujuan

Anggota grup dapat melihat lokasi terkini anggota lain yang sedang berbagi (tujuan §2 poin 15), dengan kendali penuh di tangan pengirim.

### Keputusan

| Keputusan | Nilai | Alasan |
|---|---|---|
| Aktivasi | Per grup, eksplisit, default nonaktif | Privacy-first |
| Durasi | Pilihan 15 menit, 1 jam (default), 8 jam, atau "Sampai saya matikan" | Mencegah berbagi lokasi tanpa sadar terlalu lama |
| Data yang dikirim | Hanya `latitude`, `longitude`, `accuracy`, `recorded_at`. Tanpa speed, bearing, altitude, atau riwayat trip | Data minimum yang dibutuhkan |
| Penyimpanan di server | Hanya satu lokasi terakhir per pengguna (ditimpa setiap update), dihapus saat sharing berhenti atau kedaluwarsa. Tidak ada riwayat | Privacy-first |
| Penyimpanan lokal | Tabel `live_location_outbox` di Room berisi paling banyak satu baris (lokasi terbaru yang belum terkirim) | Sesuai arsitektur GPS → Room → Sync; saat offline hanya lokasi terbaru yang dikirim |
| Service | Satu foreground service tipe `location` yang sama dengan tracking. Jika tracking aktif, sharing memakai fix tracking; jika tidak, service berjalan dalam mode sharing saja | Satu notification, satu sumber GPS |
| Permission | Dimulai dari aksi pengguna di foreground, sehingga tidak butuh background location | Konsisten dengan §9 |
| Pengiriman | REST `POST /live/location`; server meneruskan ke anggota lewat Reverb private channel `group.{id}` | Upload sekali untuk semua grup aktif |
| Penerimaan | WebSocket hanya terhubung selama layar peta grup terlihat; saat dibuka, snapshot diambil via REST lalu subscribe | Battery-aware |

### Nilai default (`LiveSharingConfig`)

| Parameter | Default |
|---|---|
| Interval GPS mode sharing saja | 30 detik, prioritas `BALANCED_POWER_ACCURACY` |
| Kirim saat bergerak | Paling cepat setiap 30 detik |
| Kirim saat diam (perpindahan < 50 m dari lokasi terakhir terkirim) | Setiap 5 menit (heartbeat) |
| Batas update di server | Update lebih cepat dari 10 detik diabaikan |
| Lokasi dianggap lama | > 15 menit, ditampilkan pudar dengan "terakhir terlihat …" |

### Fitur

1. Detail grup: toggle **Bagikan lokasi saya** dengan pilihan durasi. Dialog menjelaskan bahwa lokasi terkini dikirim ke anggota grup ini selama durasi tersebut.
2. Foreground notification selama sharing aktif: "Berbagi lokasi dengan <nama grup> sampai <jam>", tombol **Berhenti berbagi**. Bila tracking juga aktif, informasi digabung dalam satu notification.
3. **Peta grup:** marker setiap anggota yang sedang berbagi (nama, waktu update terakhir, lingkaran accuracy). Anggota yang tidak berbagi tidak tampil di peta, tetapi tampil di daftar dengan status "Tidak berbagi".
4. Indikator di daftar grup untuk grup yang sedang menerima lokasi saya.

### Kondisi sharing berhenti

Sharing berhenti dan lokasi di server dihapus bila:

- pengguna mematikan toggle atau menekan Berhenti berbagi;
- durasi habis (ditegakkan oleh server melalui `expires_at` dan oleh perangkat);
- pengguna keluar atau dikeluarkan dari grup, atau grup dihapus;
- pengguna logout atau menghapus akun;
- permission lokasi dicabut.

Sharing **tidak** berlanjut setelah proses aplikasi dihentikan sistem (`START_NOT_STICKY`, sama dengan §32). Status di server kedaluwarsa sendiri dan anggota melihat lokasi tersebut sebagai lama.

### Skema

Server:

| Tabel | Field utama |
|---|---|
| `live_shares` | `user_id`, `group_id`, `started_at`, `expires_at` (nullable untuk "sampai dimatikan"); unik (`user_id`, `group_id`) |
| `live_locations` | `user_id` (PK), `latitude`, `longitude`, `accuracy`, `recorded_at`, `received_at` |

Otorisasi channel `group.{id}`: hanya anggota grup. Endpoint snapshot hanya mengembalikan lokasi anggota yang memiliki `live_shares` aktif untuk grup tersebut.

Lokal (migrasi Room 6 → 7): `live_location_outbox` (`id` = 1 tetap, `latitude`, `longitude`, `accuracy`, `recorded_at`).

### Acceptance Criteria Fase 9

- Tanpa mengaktifkan sharing, tidak ada lokasi yang terkirim meski sudah bergabung grup.
- Anggota lain melihat posisi pengirim diperbarui sekitar setiap 30 detik saat bergerak.
- Sharing 15 menit berhenti otomatis pada menit ke-15; lokasi hilang dari peta anggota (boundary).
- Saat offline, lokasi tidak menumpuk; setelah koneksi kembali hanya lokasi terbaru yang terkirim (failure case).
- Anggota yang dikeluarkan langsung kehilangan akses ke lokasi anggota grup dan berhenti membagikan lokasinya.
- Non-anggota tidak dapat subscribe channel maupun membaca snapshot (diuji di API).
- Notification sharing selalu tampil selama sharing aktif.
- Tracking dan sharing berjalan bersamaan dengan satu notification dan satu sumber GPS.
- Server tidak menyimpan riwayat lokasi (hanya satu baris per pengguna).
- Migrasi 6 → 7 dites.

### Tidak termasuk Fase 9

Riwayat lokasi anggota, berbagi trip, status baterai perangkat lain, geofence alert, SOS (§37), berbagi ke orang di luar grup.

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

Fase 1 dianggap selesai apabila seluruh kondisi berikut terpenuhi. Acceptance criteria Fase 2–9 ada di §38, di bagian masing-masing fase.

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
Foreground + presisi; background location hanya untuk trip otomatis opt-in (Fase 5)

Backend:
Laravel + PostgreSQL + REST API + Reverb realtime (Fase 6+)

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

PRD ini menjadi dasar implementasi seluruh fase. Sebelum sebuah fase dimulai, detail requirement fase tersebut di §38 harus direview. Perubahan requirement harus dicatat di §45 dan dinilai dampaknya terhadap kestabilan tracking, penggunaan baterai, privasi, dan kompleksitas aplikasi.

---

# 45. Riwayat Revisi

## v2.7 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §38 Fase 5 — Penghemat baterai | Mode diam tetap `HIGH_ACCURACY`; hanya interval yang diperpanjang ke 30 detik | Uji emulator: dengan `BALANCED_POWER_ACCURACY` perjalanan setelah singgah tidak terekam sama sekali (0 titik selama 70 detik bergerak) |

## v2.6 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §2, §6, §38 Fase 5 | Tujuan 16 dan fitur Uji Akselerasi (0–100/200/300/400/500 m, 0–100 km/jam), tabel `acceleration_runs` (migrasi 4 → 5) | Permintaan user |

## v2.5 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §38 Fase 5 — Peredam jitter | Titik dengan kecepatan GPS diketahui < `STATIONARY_SPEED_MPS` (0,5 m/s) dilewati oleh perhitungan jarak jangkar | Aturan accuracy saja tidak memenuhi kriteria "< 50 m untuk diam 10 menit" pada simulasi (median ±200 m dengan drift, ±1,5 km dengan jitter acak). Kriteria asli tetap; pembuktian akhir dengan rekaman nyata di uji manual |

## v2.4 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §2, §6, §22, §23, §26, §35, §38 Fase 4 | Unduhan wilayah offline diganti cache peta otomatis (ambient cache MapLibre 200 MB) + aksi Hapus cache peta; key `offline_download_wifi_only` dihapus | Terms of Service OpenFreeMap melarang pengambilan data otomatis tanpa izin; tidak ditemukan sumber tile gratis tanpa API key yang jelas mengizinkan unduhan offline. Keputusan user: opsi cache (gratis, sesuai lisensi) |

## v2.3 — 2026-10-03

| Bagian | Perubahan | Alasan |
|---|---|---|
| §3 Bertahap | Syarat lanjut ke fase berikutnya: fase sebelumnya selesai dengan unit test, build, dan lint lulus. Uji manual di HP dilakukan sekali setelah semua fase selesai, bukan di akhir setiap fase | Keputusan pemilik produk: pengujian manual dikumpulkan di akhir |

## v2.2 — 2026-10-03

Detail Fase 2–9 dilengkapi. Tujuan dan prinsip (§1–§3) tidak berubah.

| Bagian | Perubahan | Alasan |
|---|---|---|
| §38 Fase 2 | Algoritma visit (radius 100 m, min. 5 menit), dihitung saat trip selesai; tabel `visits` sebagai data turunan yang tidak disinkronkan | Battery-aware; location point tetap sumber kebenaran |
| §38 Fase 3 | Tabel `places` (UUID); nama visit dicocokkan saat ditampilkan; tab Tempat dan detail tempat | Perubahan tempat langsung berlaku ke visit lama; sync-ready |
| §38 Fase 4 | Offline region via MapLibre (zoom 10–14, maks. 20 000 tile, default Wi-Fi saja); daftar key DataStore; setting tracking berlaku di trip berikutnya | Hemat kuota; trip memakai parameter konsisten |
| §38 Fase 5 | Trip otomatis opt-in (kendaraan/sepeda), kolom `trips.source`, lanjutkan trip ≤ 60 menit, jarak dengan titik jangkar, interval turun saat diam | Revisi keputusan §9 hanya untuk trip otomatis |
| §38 Fase 6 | Backend: Laravel + PostgreSQL + Redis + Reverb, Sanctum, email + password, verifikasi email; logout tidak menghapus data lokal | Local-first, privacy-first |
| §38 Fase 7 | Cloud Backup per akun; sync trip selesai, titik, dan tempat via WorkManager; hapus menang, tempat last-write-wins; tombstone `deleted_at` | Sync idempoten tanpa duplikasi |
| §38 Fase 8 | Undangan kode 8 karakter berlaku 7 hari + App Links; maks. 20 anggota; cache grup read-only | Tanpa push notification; offline wajar |
| §38 Fase 9 | Sharing per grup dengan durasi; hanya lat/lng/accuracy/waktu; server menyimpan satu lokasi terakhir; satu foreground service bersama tracking | Data minimum, battery-aware |
| §4, §9, §26, §27, §35, §40, §44 | Diselaraskan dengan §38 | Konsistensi |

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
