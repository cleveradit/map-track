# Implementation Plan: TICKET-006 (History: Daftar & Hapus Trip)

**Ticket:** `TICKET-006`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-005`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Urutan | Terbaru di atas (`started_at DESC`, sudah dari `TripDao.observeAll`) |
| Isi item selesai | Tanggal (`29 September 2026`), rentang jam (`07:32 - 08:18`), jarak, durasi (PRD §18) |
| Item trip aktif | Tanggal, jam mulai, label `Sedang berjalan`, tanpa statistik dan tanpa tombol hapus |
| Format tanggal/jam | `java.time`, zona waktu perangkat, locale `id-ID`, jam 24 jam |
| Hapus | Ikon hapus di item selesai → dialog konfirmasi teks PRD §34 → `TripRepository.deleteTrip` |
| Tap item | Callback `onTripClick(tripId)` disiapkan; navigasi ke detail dikerjakan di TICKET-007 |
| Daftar kosong | Teks `Belum ada perjalanan.` |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat melihat seluruh trip yang tersimpan, terbaru di atas, beserta ringkasan jarak dan durasi, serta menghapus trip yang sudah selesai setelah konfirmasi.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/ui/format/Formatters.kt`
   - `fun formatDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String`
   - `fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String`
2. File: `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryItem.kt`
   - `data class HistoryItem(tripId: String, date: String, timeRange: String, distance: String?, duration: String?, isActive: Boolean)`
   - `fun Trip.toHistoryItem(zone: ZoneId = ZoneId.systemDefault()): HistoryItem`
3. File: `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryViewModel.kt`
   - `val items: StateFlow<List<HistoryItem>?>` (`null` = memuat), `fun deleteTrip(tripId: String)`, `Factory`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryScreen.kt`
   - `@Composable fun HistoryScreen(onTripClick: (String) -> Unit, modifier, viewModel)` — `LazyColumn`, dialog konfirmasi hapus.

---

## 4. Scope of Changes

### A. Format

1. `formatDate`, `formatTime`.

### B. History

1. `HistoryItem`, `HistoryViewModel`, `HistoryScreen`, string resource.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Format tanggal & jam | 2026-09-29 07:32 WIB | `29 September 2026`, `07:32` | `[x]` |
| Item selesai | Trip 07:32–08:18, 21 700 m | `07:32 - 08:18`, `21.7 km`, `46 menit`, tidak aktif | `[x]` |
| Boundary: item aktif | Trip `active` | `07:32 - …`, jarak & durasi `null`, `isActive = true` | `[x]` |
| Boundary: lewat tengah malam | 23:50–00:20 | Tanggal = tanggal mulai, `23:50 - 00:20` | `[x]` |
| Failure: hapus trip aktif | `deleteTrip(activeId)` di ViewModel | Trip tetap ada (repository menolak), tidak crash | `[x]` |
| Manual: urutan | 2+ trip | Terbaru di atas | `[ ]` manual |
| Manual: hapus | Ikon hapus → Batal; lalu → Hapus | Batal: tetap ada. Hapus: hilang dari daftar | `[ ]` manual |
| Manual: kosong | Belum ada trip | `Belum ada perjalanan.` | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 53/53 lulus (baru: `HistoryItemTest` 3, `HistoryViewModelTest` 1), `assembleDebug` sukses, lint 0 error. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Halaman Trip Detail (TICKET-007).
2. Hapus dari halaman detail.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
