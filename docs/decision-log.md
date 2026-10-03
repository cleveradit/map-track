# Decision Log

Recording criteria: see [ai-context.md §5 Step 4](ai-context.md#5-documentation-workflow).

Entry format (4 fields):

```markdown
## DEC-XXX — Short title

**Decision:** What was decided.

**Why:** Why — the constraint, bug, or wrong assumption.

**Impact:** Concrete consequences that are not obvious from the code.

**Tickets:** TICKET-XXX (optional)
```

---

<!-- ADD DECISION LOG ENTRIES BELOW -->

## DEC-001 — Trip terputus dideteksi dari DB + state in-process, state di-set sebelum service jalan

**Decision:** Trip dianggap terputus bila berstatus `active` di DB tetapi `TrackingStateHolder` tidak `Active` untuk trip itu. `TrackingController.start()` men-set `TrackingState.Active` segera setelah `startForegroundService` dipanggil, tidak menunggu service.

**Why:** Service baru men-set state setelah coroutine-nya membaca trip dari DB. Di celah itu trip sudah `active` sementara state masih `Idle`, sehingga Home akan menampilkan dialog "perjalanan tidak selesai" untuk trip yang baru dimulai.

**Impact:** Kode baru yang memulai service atau mengubah `TrackingStateHolder` harus menjaga invarian ini: state `Active` selalu ditetapkan sebelum atau bersamaan dengan trip menjadi `active`, dan baru kembali `Idle` setelah trip `completed` (service memanggil `finish` dulu, lalu set `Idle`). Trip terputus diakhiri dengan `ended_at` = titik terakhir, bukan waktu sekarang.

**Tickets:** TICKET-010


## DEC-002 — Visit digabung setelah filter durasi, bukan sebelum

**Decision:** `VisitDetector` membuang cluster < `MIN_VISIT_DURATION_MS` dulu, baru menggabung visit berurutan yang memenuhi `MERGE_GAP_MS` dan `MERGE_DISTANCE_METERS` (urutan langkah PRD §38 Fase 2).

**Why:** Bila penggabungan dilakukan sebelum filter durasi, beberapa cluster pendek dari perjalanan pelan atau macet (masing-masing < 5 menit, berdekatan) dapat menyatu menjadi visit palsu.

**Impact:** Titik melenceng di tengah singgahan panjang tidak memecah visit. Namun bila titik melenceng terjadi < 5 menit setelah tiba atau sebelum pergi, potongan pendek itu terbuang: visit tetap satu, tetapi `arrived_at` lebih lambat atau `departed_at` lebih awal dari kenyataan. Mengubah urutan ini harus menaikkan `DETECTION_VERSION`.

**Tickets:** TICKET-012

## DEC-003 — Backfill visit memakai coroutine application scope, bukan WorkManager

**Decision:** `VisitBackfill` berjalan sekali per start proses dari `MapTrackApplication.onCreate`, di `AppContainer.applicationScope` (`SupervisorJob() + Dispatchers.Default`). Trip yang gagal tidak menghentikan trip lain dan tetap pada versi lama.

**Why:** WorkManager baru masuk tech stack di Fase 7 (ai-context §1). Pekerjaan backfill pendek, lokal, dan idempoten: bila proses mati di tengah jalan, trip yang belum diproses masih punya versi lama dan diproses lagi saat aplikasi dibuka berikutnya.

**Impact:** Tidak ada jaminan selesai saat proses dibunuh, tetapi tidak ada data yang hilang karena visit hanya data turunan. Backfill juga berjalan bila proses dimulai oleh service; keamanannya bergantung pada `recomputeVisits` yang memeriksa status dan versi di dalam transaksi. Saat Fase 7 menambah WorkManager, pemindahan backfill bersifat opsional.

**Tickets:** TICKET-014

## DEC-004 — Migration test tanpa room-testing / MigrationTestHelper

**Decision:** `MigrationTest` membangun database versi 1 langsung dari `createSql` di `app/schemas/.../1.json` (dibaca sebagai file, relatif terhadap direktori modul `app`), lalu membukanya dengan `MapTrackDatabase` versi terbaru. Room menjalankan migrasi produksi dan memvalidasi skema hasilnya terhadap entity. Tidak ada dependency `androidx.room:room-testing`.

**Why:** `MigrationTestHelper` membaca skema dari asset. Robolectric unit test hanya membaca asset hasil `mergeDebugAssets`, sehingga skema yang didaftarkan sebagai asset source set `test` tidak terbaca.

**Impact:** Migrasi berikutnya (2 → 3, dst.) dites dengan pola yang sama: `createDatabase(N)` dari `N.json` lalu buka dengan Room. Constraint tabel baru dites terpisah dengan database in-memory (contoh: `VisitDaoTest`). `1.json` dan seterusnya wajib tetap ada di git.

**Tickets:** TICKET-013

## DEC-005 — Titik tempat hanya diambil dari gesture peta

**Decision:** `PlacePickerMap` melaporkan target kamera sebagai titik tempat hanya bila gerakan kamera dimulai gesture pengguna (`REASON_API_GESTURE`). Titik dari kode (tempat lama, pusat visit, lokasi saat ini) diset di `PlaceEditorViewModel` lalu dikirim ke peta sebagai `CameraRequest`.

**Why:** Peta memulai di kamera default (Indonesia, zoom 3.5) dan dapat memicu event idle setelah style termuat. Bila setiap idle dianggap pilihan pengguna, titik dari visit/tempat lama bisa tertimpa titik default, atau tempat baru tersimpan di tengah Indonesia tanpa disadari.

**Impact:** Tempat baru tanpa gesture dan tanpa lokasi saat ini tidak bisa disimpan (Simpan nonaktif). Kode yang menambah cara lain menggerakkan kamera form harus lewat `CameraRequest`, bukan memanggil `onCenterPicked`.

**Tickets:** TICKET-019

## DEC-006 — Satu visit hanya dihitung untuk satu tempat

**Decision:** Di semua layar (Trip Detail, Tab Tempat, Detail Tempat) visit dimiliki tepat satu tempat: tempat yang cocok dengan pusat terdekat (`PlaceMatcher.group`). Pencocokan berjalan di Kotlin, bukan SQL.

**Why:** PRD hanya menetapkan aturan "pusat terdekat" untuk nama visit. Bila Detail Tempat menghitung semua visit di dalam radius, tempat besar yang tumpang tindih (mis. "Kampus" mencakup "Kantin") akan menampilkan kunjungan yang di Trip Detail bernama tempat lain. Haversine tidak tersedia di SQLite tanpa fungsi kustom, dan jumlah visit kecil.

**Impact:** Total kunjungan dan durasi tempat besar berkurang ketika tempat kecil di dalamnya dibuat. Pencocokan dihitung ulang (semua visit × semua tempat) setiap kali tempat atau visit berubah; bila data tumbuh besar, pertimbangkan cache, tetapi aturan kepemilikan harus tetap sama.

**Tickets:** TICKET-017, TICKET-018, TICKET-020

