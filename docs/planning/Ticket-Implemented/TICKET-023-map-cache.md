# Implementation Plan: TICKET-023 (Verifikasi Lisensi Tile & Cache Peta Offline)

**Ticket:** `TICKET-023`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-022`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 4 (v2.4) — Keputusan, Cache peta offline |
| Hasil verifikasi lisensi | [OpenFreeMap ToS](https://openfreemap.org/tos/) tidak menyebut offline, tetapi melarang *"collect data from the service in automated ways without permission"*. Unduhan wilayah lewat `OfflineManager` (ribuan tile otomatis) termasuk larangan itu tanpa izin. OpenFreeMap menyediakan planet MBTiles hanya untuk self-hosting. VersaTiles tidak memiliki ketentuan offline yang jelas; tile OSM resmi melarang prefetch; PMTiles tidak didukung offline pack MapLibre Android |
| Keputusan | Opsi B dari user: tanpa unduhan wilayah; offline lewat ambient cache MapLibre untuk tile yang pernah ditampilkan. PRD direvisi ke v2.4 |
| Batas cache | `MapConfig.MAP_CACHE_MAX_BYTES = 200 * 1024 * 1024` (default MapLibre 50 MB), diset sekali per proses saat peta pertama dibuat |
| Hapus cache | `MapCache.clear(context): Result<Unit>` (dipakai Settings di TICKET-025) |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" dan pilihan "gunakan opsi B" di chat (tiket langsung `READY`) |

---

## 2. Objective

Area yang pernah dibuka di peta tetap tampil tanpa internet, tanpa melanggar lisensi server tile gratis, dan pengguna dapat mengosongkan cache.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapConfig.kt` — `const val MAP_CACHE_MAX_BYTES = 200L * 1024 * 1024`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapCache.kt`
   - `object MapCache { fun ensureConfigured(context: Context); suspend fun clear(context: Context): Result<Unit> }`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/map/MapLibreMap.kt` — panggil `MapCache.ensureConfigured` setelah `MapLibre.getInstance`.
4. File: `docs/initiate-file/prd-map-track.md`, `docs/ai-context.md` — revisi v2.4 (sudah diterapkan di tiket ini).

---

## 4. Scope of Changes

### A. Dokumen

1. PRD v2.4, ai-context §1, backlog Fase 4 dihapus (jadi tiket).

### B. Kode

1. Konfigurasi dan pembersihan cache.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Unit test | N/A — `OfflineManager` memakai library native MapLibre yang tidak berjalan di JVM/Robolectric | — | N/A |
| Build | `assembleDebug` | Sukses | `[x]` |
| Manual: offline area yang pernah dibuka | Buka Home/Trip Detail online, lalu mode pesawat dan buka lagi | Tile tampil dari cache | `[ ]` manual (checklist TICKET-026) |
| Failure manual: area belum pernah dibuka | Mode pesawat, geser ke area baru | Peta kosong, tracking tetap berjalan | `[ ]` manual (checklist TICKET-026) |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 155 test, 0 gagal. Tambahan: `app/src/test/resources/robolectric.properties` (`application=android.app.Application`) agar unit test tidak menjalankan `MapTrackApplication` (database asli + backfill visit) yang memunculkan peringatan CloseGuard SQLite. Kasus manual masuk checklist TICKET-026.

---

## 7. Out of Scope

1. Halaman Settings dan aksi Hapus cache di UI (TICKET-025).
2. Unduhan wilayah (dihapus dari scope, PRD v2.4).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
