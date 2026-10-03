# Implementation Plan: TICKET-027 (Kolom `trips.source`, Migrasi 3 → 4, Label "Otomatis")

**Ticket:** `TICKET-027`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-026`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Keputusan (Asal trip), Skema data, Trip otomatis no. 6 |
| Migrasi | Room `AutoMigration(from = 3, to = 4)`: kolom `trips.source TEXT NOT NULL DEFAULT 'manual'`; trip lama menjadi `manual` |
| Domain | `enum class TripSource(dbValue) { MANUAL("manual"), AUTO("auto") }`; `Trip.source` (default `MANUAL`); `TripRepository.startTrip(source: TripSource = TripSource.MANUAL)` |
| Label | History dan Trip Detail menampilkan "Otomatis" untuk `source = auto` |
| `updated_at` | Tidak terpengaruh migrasi |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Setiap trip mencatat asalnya (manual atau otomatis) sebagai dasar trip otomatis, dengan migrasi yang mempertahankan data lama.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/Trip.kt` — `TripSource`, `Trip.source`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/TripEntity.kt` — `@ColumnInfo(name = "source", defaultValue = "manual") val source: String = "manual"`; mapper.
3. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` — `version = 4`, `AutoMigration(from = 3, to = 4)`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt` — `startTrip(source)`.
5. File: `app/src/main/java/com/radityodwiki/maptrack/ui/history/HistoryItem.kt`, `HistoryScreen.kt` — `isAuto`, label.
6. File: `app/src/main/java/com/radityodwiki/maptrack/ui/tripdetail/TripSummary.kt`, `TripDetailScreen.kt` — `isAuto`, label.
7. File: `app/src/main/res/values/strings.xml` — `trip_source_auto` = "Otomatis".
8. File: `app/schemas/.../4.json` (dihasilkan Room).
9. Test: `MigrationTest` (+ 3 → 4), `TripRepositoryTest`, `HistoryItemTest`, `TripSummaryTest`.

---

## 4. Scope of Changes

### A. Data

1. Kolom, migrasi, domain, repository.

### B. UI

1. Label "Otomatis".

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Migrasi 3 → 4 | DB v3 berisi trip + titik + visit + tempat | Data utuh; `source = 'manual'`; skema tervalidasi | `[x]` |
| Default manual | `startTrip()` | `source = MANUAL` | `[x]` |
| Trip otomatis | `startTrip(TripSource.AUTO)` | `source = AUTO` tersimpan dan terbaca | `[x]` |
| Label | `toHistoryItem` / `toSummary` trip auto vs manual | `isAuto = true` / `false` | `[x]` |
| Failure: nilai tidak dikenal | `TripSource.fromDbValue("x")` | `IllegalArgumentException` | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 176 test, 0 gagal (`MigrationTest` 3, `TripRepositoryTest` 17, `HistoryItemTest` 4, `TripSummaryTest` 4). `4.json` terbentuk.

---

## 7. Out of Scope

1. Logika trip otomatis (TICKET-031).

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
