# Implementation Plan: TICKET-005 (Start/Stop Tracking & Foreground Service)

**Ticket:** `TICKET-005`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `TICKET-004`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| Urutan Start (PRD §8.1) | Cek izin presisi → cek Location service → `startTrip()` (gagal bila ada trip aktif) → `startForegroundService` dengan `tripId` |
| Izin notifikasi | Diminta saat Start ditekan di Android 13+; hasil apa pun tetap melanjutkan Start |
| Service | `LocationTrackingService`, `foregroundServiceType="location"`, `START_NOT_STICKY` (PRD §32) |
| Penyimpanan titik | Setiap fix dievaluasi `LocationFilter` terhadap titik **tersimpan** terakhir; yang lolos langsung di-insert (PRD §12, §32) |
| Stop | Dari Home atau tombol Stop di notification; keduanya lewat `ACTION_STOP` ke service. Bila service tidak berjalan tetapi ada trip aktif di DB, `TrackingController.stop()` menyelesaikan trip langsung |
| Penyelesaian trip | `TripRepository.finishTrip(tripId, endedAt)`: baca titik, hitung statistik, set `completed` dalam satu transaksi; idempoten |
| Gagal start service | Trip yang baru dibuat langsung diselesaikan (0 titik) dan error `SERVICE_START_FAILED` ditampilkan |
| Notification | Channel `tracking` (importance low), judul "Tracking aktif", teks "Kecepatan: X · Durasi: Y", atau "Menunggu sinyal GPS…" / "Location service tidak aktif"; tap membuka app; aksi Stop; diperbarui setiap fix dan setiap 5 detik |
| Home saat tracking | Fix diambil dari state service (tidak membuat request lokasi kedua); tampil status Tracking, durasi, tombol Stop |
| State bersama | `TrackingStateHolder` (`StateFlow<TrackingState>`) di `AppContainer`: `Idle` atau `Active(tripId, startedAt, lastFix)` |
| Persetujuan | Instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Pengguna dapat memulai dan menghentikan perjalanan. Selama tracking, lokasi direkam oleh foreground service yang tetap berjalan saat app di-minimize, berpindah app, atau layar terkunci. Titik yang lolos filter tersimpan ke Room, dan saat Stop statistik trip dihitung dan disimpan.

---

## 3. Non-Negotiable Technical Contract

1. File: `app/src/main/AndroidManifest.xml`
   - Permission `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`; `<service android:name=".location.LocationTrackingService" android:exported="false" android:foregroundServiceType="location" />`.
2. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationSource.kt`
   - `interface LocationSource { fun permissionState(): LocationPermission; fun isLocationEnabled(): Boolean; fun fixes(): Flow<GpsFix> }` — diimplementasikan `LocationTracker`.
3. File: `app/src/main/java/com/radityodwiki/maptrack/data/repository/TripRepository.kt`
   - `suspend fun finishTrip(tripId: String, endedAt: Long): Trip`
4. File: `app/src/main/java/com/radityodwiki/maptrack/domain/usecase/TripRecorder.kt`
   - `class TripRecorder(repository: TripRepository, now: () -> Long)`
   - `suspend fun record(tripId: String, fix: GpsFix): RejectReason?`
   - `suspend fun finish(tripId: String): Trip`
5. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingStateHolder.kt`
   - `sealed interface TrackingState { data object Idle; data class Active(tripId: String, startedAt: Long, lastFix: GpsFix?) }`, `class TrackingStateHolder { val state: MutableStateFlow<TrackingState> }`
6. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingController.kt`
   - `enum class StartTrackingError { PERMISSION_MISSING, LOCATION_DISABLED, TRIP_ALREADY_ACTIVE, SERVICE_START_FAILED }`
   - `interface TrackingServiceLauncher { fun start(tripId: String); fun stop() }`
   - `suspend fun start(): StartTrackingError?`, `suspend fun stop()`
7. File: `app/src/main/java/com/radityodwiki/maptrack/location/LocationTrackingService.kt`
   - `ACTION_START` (+ `EXTRA_TRIP_ID`), `ACTION_STOP`; `AndroidTrackingServiceLauncher(context)`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingNotification.kt`
   - `fun trackingNotificationText(lastFix: GpsFix?, startedAt: Long, now: Long, locationEnabled: Boolean): String` + builder notification dan channel.
9. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/*`
   - Bagian Tracking (status, durasi, tombol Start/Stop, pesan error) di Home.

---

## 4. Scope of Changes

### A. Domain & data

1. `finishTrip`, `TripRecorder`.

### B. Tracking

1. `LocationSource`, `TrackingStateHolder`, `TrackingController`, `LocationTrackingService`, notification.

### C. Home

1. ViewModel + UI untuk Start/Stop dan status tracking.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Rekam fix valid | Fix pertama accuracy 5 m | Tersimpan, return `null` | `[x]` |
| Failure: fix buruk | accuracy 80 m | Tidak tersimpan, `POOR_ACCURACY` | `[x]` |
| Boundary: jump dibanding titik tersimpan | Fix 2 ditolak (jump), fix 3 dekat fix 1 | Fix 3 diterima (pembanding fix 1) | `[x]` |
| Finish | 3 titik, Stop | Status `completed`, jarak dan statistik terisi, `endedAt` = waktu Stop | `[x]` |
| Boundary: finish tanpa titik | Stop tanpa fix | `completed`, jarak 0, `maxSpeed` null | `[x]` |
| Finish idempoten | `finishTrip` dua kali | Hasil sama, tidak error | `[x]` |
| Failure: start tanpa izin | Izin presisi tidak ada | `PERMISSION_MISSING`, tidak ada trip | `[x]` |
| Failure: Location mati | Location service off | `LOCATION_DISABLED`, tidak ada trip | `[x]` |
| Failure: trip aktif ada | Start dua kali | `TRIP_ALREADY_ACTIVE` | `[x]` |
| Failure: service gagal start | Launcher melempar exception | `SERVICE_START_FAILED`, trip diselesaikan | `[x]` |
| Stop tanpa service | Trip aktif di DB, state `Idle` | Trip diselesaikan langsung | `[x]` |
| Teks notifikasi | fix segar 12.5 m/s, 18 menit; fix basi; Location off | `Kecepatan: 45 km/h · Durasi: 18 menit`; `Menunggu sinyal GPS… · Durasi: …`; `Location service tidak aktif · Durasi: …` | `[x]` |
| Manual: tracking berjalan | Start, jalan/berkendara | Notification muncul dengan kecepatan dan durasi | `[ ]` manual |
| Manual: background | Minimize, buka app lain, kunci layar 5 menit | Tracking tetap jalan; durasi bertambah | `[ ]` manual |
| Manual: Stop dari notification | Tekan Stop di notification | Notification hilang, Home kembali ke Start | `[ ]` manual |
| Manual: tap notification | Tekan body notification | App terbuka di Home dengan status Aktif | `[ ]` manual |

---

## 6. Verification Commands

Hasil: `testDebugUnitTest` 49/49 lulus (baru: `TripRecorderTest` 7, `TrackingControllerTest` 7, `TrackingNotificationTextTest` 3), `assembleDebug` sukses, lint 0 error. Baris `manual` menunggu uji user di HP.


1. `./gradlew testDebugUnitTest`
2. `./gradlew assembleDebug lintDebug`

---

## 7. Out of Scope

1. Daftar History dan detail trip (TICKET-006, 007).
2. Dialog recovery trip terputus (TICKET-010) — di tiket ini trip aktif tanpa service hanya bisa dihentikan lewat tombol Stop.
3. Peta.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed.
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
