# Implementation Plan: TICKET-033 (Mesin Ukur & Penyimpanan Uji Akselerasi)

**Ticket:** `TICKET-033`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-032`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Uji akselerasi (v2.6) |
| Target | Waktu dari diam ke 100, 200, 300, 400, 500 m dan ke 100 km/jam (27,7778 m/s) |
| Mesin | `AccelerationMeter` murni (JVM) yang menerima fix (waktu, kecepatan, accuracy) dan mengeluarkan state; tanpa Android API |
| Fase | `WAITING_GPS` → `WAITING_STILL` → `READY` → `RUNNING` → `FINISHED` / `INVALID` |
| Siap | Accuracy ≤ 20 m dan kecepatan < 0,5 m/s terus-menerus ≥ 2 detik (inklusif). Fix dengan accuracy > 20 m di luar `RUNNING` → kembali `WAITING_GPS` |
| Mulai | Dari `READY`, fix pertama dengan kecepatan ≥ 1,0 m/s → `RUNNING`; titik nol = waktu fix diam terakhir, jarak 0 |
| Jarak | Integrasi trapesium kecepatan antar-fix |
| Waktu target | Asumsi percepatan konstan antar-fix: jarak = solusi kuadrat `d = v₀t + ½at²`; kecepatan = `t = (V − v₀)/a` |
| Selesai | Semua target tercapai, kecepatan < 1,0 m/s selama ≥ 2 detik, `stop()` oleh pengguna, atau 60 detik sejak titik nol |
| Gagal | Jeda antar-fix > 3 detik saat `RUNNING`, atau fix tanpa kecepatan saat `RUNNING` dianggap jeda (dilewati) |
| Simpan | Hanya `FINISHED` dengan minimal satu target tercapai, sekali per run |
| Tabel | `acceleration_runs`: `id` INTEGER PK auto, `started_at` INTEGER, `time_100m_ms` … `time_500m_ms` INTEGER?, `speed_at_100m_mps` … `speed_at_500m_mps` REAL?, `time_0_100_kmh_ms` INTEGER?, `max_speed_mps` REAL. Migrasi Room `AutoMigration(4, 5)`. Data lokal, tidak disinkronkan |
| Persetujuan | Instruksi user di chat: "tambahkan sekarang, buat tiket langsung status ready" |

---

## 2. Objective

Menyediakan perhitungan waktu akselerasi yang akurat dari GPS 1 Hz dan menyimpan hasilnya secara lokal sebagai dasar halaman Uji Akselerasi.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/AccelerationConfig.kt` — konstanta PRD.
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/AccelerationMeter.kt`
   - `data class SpeedFix(val timeMs: Long, val speedMps: Float?, val accuracyMeters: Float)`
   - `enum class AccelerationPhase { WAITING_GPS, WAITING_STILL, READY, RUNNING, FINISHED, INVALID }`
   - `data class AccelerationState(phase, elapsedMs: Long, distanceMeters: Double, speedMps: Double, distanceTimesMs: Map<Int, Long>, distanceSpeedsMps: Map<Int, Double>, time0To100KmhMs: Long?, maxSpeedMps: Double, startedAt: Long?)`
   - `class AccelerationMeter { val state: AccelerationState; fun onFix(fix: SpeedFix): AccelerationState; fun stop(): AccelerationState; fun reset() }`
3. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/AccelerationRun.kt` + `fun AccelerationState.toRun(): AccelerationRun?`
4. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/entity/AccelerationRunEntity.kt`, `data/local/dao/AccelerationRunDao.kt`
5. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/database/MapTrackDatabase.kt` — `version = 5`, `AutoMigration(4, 5)`, `accelerationRunDao()`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/AccelerationRepository.kt` — `save(run)`, `observeRuns(): Flow<List<AccelerationRun>>` (terbaru dulu), `delete(id)`.
7. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt` — `accelerationRepository`.
8. Test: `AccelerationMeterTest`, `AccelerationRepositoryTest`, `MigrationTest` (+ 4 → 5).

---

## 4. Scope of Changes

### A. Domain

1. Config, mesin ukur, model run.

### B. Data

1. Tabel, DAO, migrasi, repository.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Akselerasi konstan | 4 m/s², fix tiap 1 s, mulai dari diam | 0–100 km/jam 6,94 s ±0,1; 0–100 m 7,07 s ±0,1; 0–500 m 15,81 s ±0,1; `FINISHED` setelah semua target | `[x]` |
| Boundary siap | Diam 1,9 s / 2,0 s | `WAITING_STILL` / `READY` | `[x]` |
| Accuracy | Fix accuracy 25 m saat menunggu | `WAITING_GPS`; tidak bisa `READY` | `[x]` |
| Bergerak sebelum siap | Kecepatan 3 m/s saat `WAITING_STILL` | Tidak mulai | `[x]` |
| Failure: GPS terputus | Jeda 4 s saat `RUNNING` | `INVALID`; `toRun()` null | `[x]` |
| Berhenti di tengah | Pelan sampai 150 m lalu diam 2 s | `FINISHED`; 100 m tercapai, 200–500 m dan 0–100 km/jam null | `[x]` |
| `stop()` | Saat `RUNNING` setelah 120 m | `FINISHED` parsial | `[x]` |
| Batas waktu | Pelan 2 m/s selama 61 s | `FINISHED` pada 60 s | `[x]` |
| Tidak ada target | `stop()` sebelum 100 m dan < 100 km/jam | `toRun()` null | `[x]` |
| Simpan/hapus | `save` dua run, `delete` satu | Terbaru dulu; sisa satu | `[x]` |
| Migrasi 4 → 5 | DB v4 dengan trip | Data utuh, tabel `acceleration_runs` kosong | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 229 test, 0 gagal (`AccelerationMeterTest` 10, `AccelerationRepositoryTest` 1, `MigrationTest` 4). `5.json` terbentuk.

---

## 7. Out of Scope

1. Halaman dan navigasi (TICKET-034).
2. Target lain (mis. 0–60 mph, ¼ mil) — dapat ditambahkan lewat konstanta di tiket terpisah.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
