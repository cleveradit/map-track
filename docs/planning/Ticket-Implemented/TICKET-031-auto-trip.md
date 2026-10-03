# Implementation Plan: TICKET-031 (Trip Otomatis)

**Ticket:** `TICKET-031`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-030`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Sumber spesifikasi | PRD §38 Fase 5 — Trip otomatis, Keputusan, `AutoTripConfig`; Rule 7 |
| Setting | DataStore `auto_trip_enabled` (default `false`), `auto_trip_include_walking` (default `false`), `auto_trip_revoked_notice` (default `false`) |
| Mengaktifkan | Switch di Settings → syarat izin lokasi presisi sudah ada (bila belum: pesan untuk mengizinkan dari Home) → dialog penjelasan + izin `ACTIVITY_RECOGNITION` → dialog penjelasan + izin `ACCESS_BACKGROUND_LOCATION` ("Izinkan sepanjang waktu") → berlangganan transisi. Salah satu ditolak atau berlangganan gagal → tetap nonaktif dengan pesan |
| Langganan | Activity Recognition Transition API: `ENTER` `IN_VEHICLE`, `ON_BICYCLE` (+ `WALKING`, `RUNNING` bila "Termasuk berjalan kaki"), serta `STILL` `ENTER`/`EXIT`. `PendingIntent` broadcast mutable ke `ActivityTransitionReceiver` (tidak di-export) |
| Mulai otomatis | Transisi gerak yang diizinkan + toggle aktif + izin lengkap + Location aktif + tidak ada trip `active` → `TrackingController.start(TripSource.AUTO)` (snapshot Settings, DEC-001). Selain itu diabaikan tanpa notifikasi |
| Notification | Judul "Perjalanan otomatis" untuk trip `auto`; tombol Stop sama |
| Henti otomatis | Hanya trip `auto`: dicek tiap 30 s di service. Diam = (trip sudah berjalan ≥ 5 menit dan semua titik 5 menit terakhir, minimal satu, dalam 100 m dari titik pertama jendela itu) **atau** `STILL ENTER` diterima ≥ 5 menit lalu tanpa transisi gerak/`STILL EXIT` sesudahnya. Diakhiri dengan `ended_at` = titik terakhir (seperti `finishInterrupted`) |
| Trip terlalu pendek | Trip `auto` dengan jarak < 300 m **atau** durasi < 2 menit dihapus setelah diakhiri, lewat jalur mana pun (henti otomatis, Stop manual, Akhiri Trip), tanpa notifikasi. Trip manual tidak pernah dihapus |
| Trip manual | Tidak pernah dihentikan otomatis (loop henti otomatis hanya berjalan untuk `auto`) |
| Mematikan | Berhenti berlangganan; trip otomatis yang berjalan tetap berjalan sampai dihentikan |
| Izin dicabut | Setiap proses dimulai (`MapTrackApplication.onCreate`), `AutoTripController.reconcile()`: toggle aktif tetapi izin tidak lengkap → toggle nonaktif + `auto_trip_revoked_notice = true`; Settings menampilkan keterangan sampai trip otomatis diaktifkan lagi. Bila izin lengkap → berlangganan ulang |
| Reboot / update | `BootReceiver` (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) memanggil `reconcile()` karena langganan transisi hilang setelah reboot |
| Abstraksi untuk test | `AutoTripPermissions` dan `ActivityTransitions` (interface) dengan implementasi Android; `AutoTripController` dan `AutoStopPolicy` murni dapat dites |
| Persetujuan | Instruksi user "lanjut fase 4 dan 5" di chat (tiket langsung `READY`) |

---

## 2. Objective

Pengguna yang mengaktifkan trip otomatis mendapatkan trip tercatat saat mulai berkendara atau bersepeda tanpa membuka aplikasi, dan trip itu berhenti sendiri setelah diam beberapa menit; trip palsu yang terlalu pendek tidak mengotori History.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/AndroidManifest.xml` — izin `ACTIVITY_RECOGNITION`, `ACCESS_BACKGROUND_LOCATION`, `RECEIVE_BOOT_COMPLETED`; receiver `.location.ActivityTransitionReceiver`, `.location.BootReceiver`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/domain/model/AppSettings.kt` — `autoTripEnabled`, `autoTripIncludeWalking`, `autoTripRevokedNotice` (default `false`).
3. File: `app/src/main/java/com/radityodwiki/maptrack/data/settings/SettingsRepository.kt` — key + `setAutoTrip(enabled: Boolean)`, `setAutoTripIncludeWalking(enabled: Boolean)`, `setAutoTripRevokedNotice(shown: Boolean)`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/location/AutoTrip.kt`
   - `enum class MotionActivity { IN_VEHICLE, ON_BICYCLE, WALKING, RUNNING, STILL }`, `data class TransitionEvent(val activity: MotionActivity, val enter: Boolean)`
   - `interface AutoTripPermissions { fun hasActivityRecognition(): Boolean; fun hasBackgroundLocation(): Boolean }`
   - `interface ActivityTransitions { suspend fun subscribe(includeWalking: Boolean): Result<Unit>; suspend fun unsubscribe() }`
   - `class StillnessHolder { var stillSince: Long? }`
   - `object AutoStopPolicy { fun shouldStop(recentPoints: List<LocationPoint>, tripStartedAt: Long, now: Long, stillSince: Long?): Boolean }`
   - `fun Trip.isTooShortAutoTrip(): Boolean`
5. File: `app/src/main/java/com/radityodwiki/maptrack/location/AutoTripController.kt`
   - `suspend fun enable(): Boolean`, `suspend fun disable()`, `suspend fun setIncludeWalking(enabled: Boolean)`, `suspend fun reconcile()`, `suspend fun onTransitions(events: List<TransitionEvent>)`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/location/AndroidAutoTrip.kt` — `AndroidAutoTripPermissions`, `GmsActivityTransitions`, `ActivityTransitionReceiver`, `BootReceiver`.
7. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt` — `start(source: TripSource = TripSource.MANUAL)`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripRecorder.kt` — hapus trip `auto` yang terlalu pendek setelah `finish`/`finishInterrupted`.
9. File: `app/src/main/java/com/radityodwiki/maptrack/data/local/dao/LocationPointDao.kt`, `TripRepository.kt` — `getPointsSince(tripId, since)`.
10. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt`, `TrackingNotification.kt` — henti otomatis, judul notification.
11. File: `app/src/main/java/com/radityodwiki/maptrack/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt` — bagian Trip otomatis dan alur izin.
12. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`, `MapTrackApplication.kt` — wiring + `reconcile()` saat start.
13. Test: `location/AutoStopPolicyTest.kt`, `location/AutoTripControllerTest.kt` (baru), `TripRecorderTest`, `SettingsRepositoryTest`, `SettingsViewModelTest`.

---

## 4. Scope of Changes

### A. Domain & data

1. Setting, kebijakan henti, trip terlalu pendek, query titik terbaru.

### B. Platform

1. Manifest, receiver, langganan transisi, izin, service, notification.

### C. UI

1. Bagian Trip otomatis di Settings.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Mulai otomatis | Toggle aktif, izin lengkap, `IN_VEHICLE ENTER`, tanpa trip aktif | Trip `auto` dibuat, service dijalankan | `[x]` |
| Diabaikan | Toggle nonaktif / trip aktif ada / `WALKING` tanpa opsi / izin background hilang / Location mati | Tidak ada trip baru | `[x]` |
| Berjalan kaki | Opsi "Termasuk berjalan kaki" aktif, `WALKING ENTER` | Trip `auto` dibuat | `[x]` |
| Henti karena titik | Trip auto 10 menit, titik 5 menit terakhir dalam 100 m | `shouldStop = true` | `[x]` |
| Boundary henti | Trip baru berjalan 4 menit diam / titik terakhir 150 m | `false` / `false` | `[x]` |
| Henti karena STILL | `stillSince` 5 menit lalu (tanpa titik) / 4 menit lalu | `true` / `false` | `[x]` |
| Boundary trip pendek | Auto 250 m/10 menit; 1 km/90 s; 300 m/2 menit | Dihapus; dihapus; dipertahankan | `[x]` |
| Trip manual pendek | Manual 50 m/30 s | Tetap ada | `[x]` |
| Failure: izin background ditolak | `enable()` tanpa izin background | `false`, toggle tetap nonaktif, tidak berlangganan; tracking manual tidak terpengaruh | `[x]` |
| Failure: berlangganan gagal | `subscribe` gagal | `false`, toggle nonaktif | `[x]` |
| Izin dicabut | Toggle aktif, `reconcile()` tanpa izin AR | Toggle nonaktif, `auto_trip_revoked_notice = true`, berhenti berlangganan | `[x]` |
| Matikan toggle | `disable()` saat trip auto berjalan | Berhenti berlangganan; trip tetap `active` | `[x]` |
| STILL dilacak | `STILL ENTER` lalu `IN_VEHICLE ENTER` | `stillSince` diisi lalu dikosongkan | `[x]` |

---

## 6. Verification Commands

1. `./gradlew testDebugUnitTest assembleDebug`

Hasil (2026-10-03): `./gradlew --no-build-cache testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`; 217 test, 0 gagal (`AutoStopPolicyTest` 5, `AutoTripControllerTest` 11, `TripRecorderTest` 15, `SettingsRepositoryTest` 6, `SettingsViewModelTest` 6). Test menemukan bug: di `requestAutoTrip` status `FAILED` tertimpa `null` karena coroutine berjalan langsung (dispatcher immediate); diperbaiki. Langganan transisi, receiver, mulai service dari background, dan henti otomatis di service diuji manual (checklist TICKET-032).

---

## 7. Out of Scope

1. Deteksi moda transportasi untuk ditampilkan, driving detection (§37).
2. Start otomatis tanpa izin pengguna.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
