# Implementation Plan: TICKET-001 (Setup Project Android)

**Ticket:** `TICKET-001`  
**Status:** `DONE`  
**Target Audience:** AI Developer Agents  
**Depends On:** `None`

---

## 1. Business Decision Snapshot (Approved)

| Item | Approved Value |
|---|---|
| applicationId / namespace | `com.radityodwiki.maptrack` |
| minSdk | `29` (Android 10, mendukung `foregroundServiceType` tanpa percabangan versi lama) |
| compileSdk / targetSdk | `37` / `36` (library AndroidX terbaru mewajibkan compileSdk ≥ 37) |
| Build system | Gradle wrapper 9.8.0, AGP 9.4.1 (built-in Kotlin), Kotlin 2.4.x, version catalog `gradle/libs.versions.toml` |
| JDK | 17 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Dependency injection | Manual (`AppContainer` di `MapTrackApplication`), tanpa Hilt — simple-first, build lebih cepat |
| Navigasi awal | Bottom navigation `Home` dan `History`, keduanya placeholder |
| Persetujuan | Disetujui user lewat instruksi loop Fase 1 (tiket langsung `READY`) |

---

## 2. Objective

Membuat project Android single-module yang dapat di-build dan dijalankan, dengan struktur package sesuai PRD §42, navigasi Home/History, dan konfigurasi tracking terpusat. Tiket ini menjadi fondasi semua tiket Fase 1 berikutnya.

---

## 3. Non-Negotiable Technical Contract

1. File: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradle/wrapper/*`
   - Change: project Gradle single-module `:app` dengan version catalog.
2. File: `app/build.gradle.kts`
   - Change: plugin `com.android.application` + `org.jetbrains.kotlin.plugin.compose`; `namespace`/`applicationId` = `com.radityodwiki.maptrack`; minSdk 29; compileSdk 37, targetSdk 36; JUnit 4 untuk unit test.
3. File: `app/src/main/java/com/radityodwiki/maptrack/MapTrackApplication.kt`
   - Class: `class MapTrackApplication : Application()` dengan properti `lateinit var container: AppContainer`.
4. File: `app/src/main/java/com/radityodwiki/maptrack/AppContainer.kt`
   - Class: `class AppContainer(context: Context)` — kosong untuk sekarang, tempat wiring repository di tiket berikutnya.
5. File: `app/src/main/java/com/radityodwiki/maptrack/location/TrackingConfig.kt`
   - Object: `object TrackingConfig` dengan konstanta `INTERVAL_MS = 5_000L`, `MIN_UPDATE_INTERVAL_MS = 5_000L`, `MAX_ACCURACY_METERS = 50f`, `MAX_JUMP_SPEED_MPS = 70.0`, `STALE_FIX_MS = 15_000L`, `STATIONARY_SPEED_KMH = 1.0`.
6. File: `app/src/main/java/com/radityodwiki/maptrack/MainActivity.kt`
   - Change: `ComponentActivity` dengan `enableEdgeToEdge()` dan `setContent { MapTrackTheme { MapTrackNavHost() } }`.
7. File: `app/src/main/java/com/radityodwiki/maptrack/navigation/MapTrackNavHost.kt`
   - Composable: `@Composable fun MapTrackNavHost()` — `Scaffold` + `NavigationBar` dua tab, route `home` dan `history`.
8. File: `app/src/main/java/com/radityodwiki/maptrack/ui/home/HomeScreen.kt`, `ui/history/HistoryScreen.kt`
   - Composable placeholder `HomeScreen()` dan `HistoryScreen()`.
9. File: `app/src/main/java/com/radityodwiki/maptrack/ui/theme/Theme.kt`
   - Composable: `MapTrackTheme(content)` — Material 3, dynamic color pada Android 12+.
10. File: `docs/ai-context.md`
    - Change: versi pasti di §1 dan isi §4 Local Development Environment.

---

## 4. Scope of Changes

### A. Gradle

1. Buat wrapper Gradle 9.8.0.
2. Version catalog berisi AGP, Kotlin, Compose BOM, activity-compose, navigation-compose, lifecycle, core-ktx, JUnit.
3. `local.properties` (sudah di-gitignore) menunjuk `sdk.dir`.

### B. Kode aplikasi

1. Application class + AppContainer.
2. Theme, MainActivity, NavHost dengan dua tab.
3. `TrackingConfig`.

### C. Dokumentasi

1. `ai-context.md` §1 (versi) dan §4 (cara build).
2. `architecture.md` diisi sesuai kode yang ada.

---

## 5. Acceptance Test Matrix

| Case | Input | Expected Result | Status |
|---|---|---|---|
| Build debug | `./gradlew assembleDebug` | BUILD SUCCESSFUL, APK di `app/build/outputs/apk/debug/` | `[x]` |
| Unit test | `./gradlew testDebugUnitTest` | BUILD SUCCESSFUL | `[x]` |
| Lint | `./gradlew lintDebug` | Tidak ada error | `[x]` 0 error, 3 warning (ikon belum ada, targetSdk 36 < 37, data extraction rules) |
| Navigasi (manual, HP) | Tap tab History lalu Home | Layar berganti, tab aktif ter-highlight | `[ ]` manual |
| Boundary: rotasi (manual, HP) | Putar layar di tab History | Tetap di tab History | `[ ]` manual |
| Failure | N/A — belum ada input atau data yang bisa gagal | — | N/A |

---

## 6. Verification Commands

1. `./gradlew assembleDebug`
2. `./gradlew testDebugUnitTest`
3. `./gradlew lintDebug`

Expected:

1. Ketiganya BUILD SUCCESSFUL.

---

## 7. Out of Scope

1. Room, lokasi, permission, service (tiket berikutnya).
2. MapLibre.
3. Ikon aplikasi kustom.

---

## 8. Completion Checklist

- [x] Section 1 fully filled and status set correctly.
- [x] All Non-Negotiable Technical Contract items implemented.
- [x] Acceptance Test Matrix completed (kecuali uji manual di HP).
- [x] Verification commands executed successfully.
- [x] No out-of-scope changes introduced.
