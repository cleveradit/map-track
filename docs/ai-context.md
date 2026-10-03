# AI Context & Development Mandates

All AI agents MUST read and follow these rules before proposing or implementing any changes.

> **Orientation (understand the project without reading code):** read in order — this document (mandates) → [architecture.md](architecture.md) (module map) → [data-model.md](data-model.md) (data structures) → [features/index.md](features/index.md) (per-feature details).

**Documentation language:** Indonesian (technical terms stay in English).

> **Product source of truth:** [initiate-file/prd-map-track.md](initiate-file/prd-map-track.md) — full product scope in 9 phases (§6), Phase 1 detail (§7–§43), Phase 2–9 scope (§38), and out-of-scope list (§37). Any scope change must be weighed against tracking stability, battery, privacy, and complexity before it is planned.

---

## 1. Tech Stack (STRICT — no deviations)

| Area | Choice |
|---|---|
| Platform | Android client only (no iOS, Web, Desktop client) |
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository (optional Use Case layer) |
| State | Android ViewModel + Kotlin Coroutines + Kotlin Flow |
| Location | Fused Location Provider |
| Background | Android Foreground Service (tracking, live sharing); WorkManager for sync from Phase 7 |
| Database | Room (SQLite) |
| Settings | Constants in source code (Phase 1–3); DataStore from Phase 4 |
| Map | MapLibre (offline via MapLibre ambient tile cache from Phase 4; no region downloads — PRD v2.4) |
| Activity detection (Phase 5) | Activity Recognition Transition API, only for opt-in automatic trips |
| Networking (Phase 6+) | Retrofit + OkHttp + kotlinx.serialization |
| Backend (Phase 6+) | Laravel + PostgreSQL + Redis + Laravel Reverb (realtime), Sanctum token auth (PRD §38 Fase 6); exact versions and VPS provider decided in the first Phase 6 ticket |

**Pinned versions** (source of truth: `gradle/libs.versions.toml`, `app/build.gradle.kts`):

| Item | Version |
|---|---|
| JDK | 17 |
| Gradle wrapper | 9.8.0 |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android`) |
| Kotlin / Compose compiler plugin | 2.4.20 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 29 |
| applicationId / namespace | `com.radityodwiki.maptrack` |
| Dependency injection | Manual `AppContainer` (no Hilt) |

---

## 2. Architecture Mandates

1. **Layering:** Compose UI → ViewModel → Use Case / Repository → (Room | Location Provider). UI never talks to Room or the location provider directly.
2. **Package layout** (per PRD §42): `data/local/{dao,database,entity}`, `data/repository`, `domain/{model,usecase}`, `location/` (`LocationTracker`, `LocationTrackingService`), `ui/{home,history,tripdetail}`, `navigation/`, `MainActivity.kt`.
3. **Offline-first / local-first:** all core features (tracking, history, statistics) must work without internet. Room is the single source of truth for trip and location data.
4. **GPS is the coordinate source**, not the map. MapLibre is display-only; missing map tiles must never stop tracking.
5. **Tracking lives in the Foreground Service**, not in an Activity/ViewModel, so it survives minimize, app switching, and screen lock.

---

## 3. Rules

**Rule 1 — Privacy:** Location data leaves the device only for a logged-in user who explicitly enabled Cloud Backup (Phase 7) or Live Sharing (Phase 9); both are opt-in, default off. No location telemetry/analytics in any phase. Per-phase rules: PRD §35.

**Rule 2 — Units:** Speed is stored in m/s; displayed in km/h (`km/h = m/s × 3.6`). Distance is stored in meters. Timestamps are epoch millis UTC (`Long`), displayed in device time zone.

**Rule 3 — Data integrity:** Every `LocationPoint` must reference a valid `Trip` (foreign key with `ON DELETE CASCADE`). Always persist `accuracy` so GPS filtering can evolve later.

**Rule 4 — Configurable tracking parameters:** Tracking interval (5 s), accuracy threshold (50 m), GPS-jump limit (70 m/s), and stale-speed timeout (15 s) live in one config object (e.g. `TrackingConfig`), never as magic numbers scattered across the code.

**Rule 5 — Resilience:** Temporary GPS loss must not stop tracking; only the user stops a trip. Trip status is only `active`/`completed`; an `active` trip without a running service is an interrupted trip (PRD §32–33). At most one `active` trip exists at a time.

**Rule 6 — Performance:** Do not re-read all location points from the database on every position update; observe incrementally via Flow.

**Rule 7 — Permissions:** Explain why location access is needed before requesting it; precise location is required, `ACCESS_BACKGROUND_LOCATION` is requested only when the user enables opt-in automatic trips (Phase 5) — manual tracking and Live Sharing never need it; history must remain usable when permission is denied.

**Rule 8 — Scope:** Work phase by phase (PRD §6); do not start a phase before its detail in PRD §38 is completed and the previous phase is complete: all its tickets `DONE` with `testDebugUnitTest`, `assembleDebug`, and `lintDebug` passing. Manual testing on the phone is NOT a gate between phases: the user tests manually only once, after all phases (1–9) are finished. Each phase closeout ticket still writes its manual checklist so the final test can follow them in order. Never implement anything in PRD §37 (Out of Scope) without revising the PRD first.

**Rule 9 — Sync-ready IDs:** Trip IDs are client-generated UUID strings and `trips.updated_at` is maintained from Phase 1, so Phase 7 sync needs no primary-key migration. Location points are append-only and unique on (`trip_id`, `recorded_at`).

---

## 4. Local Development Environment

**Prerequisites**

- JDK 17 (`jdk17-openjdk` on Arch/CachyOS).
- Android SDK at `~/Android/Sdk` with `platform-tools`, `platforms;android-37.0`, `build-tools;36.1.0` (install via `~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager`).
- `local.properties` (gitignored) containing `sdk.dir=/home/<user>/Android/Sdk`.

**Commands** (run from the repo root)

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` → `app/build/reports/lint-results-debug.txt` (expected: 0 errors, 1 warning `OldTargetApi`) |
| Install on connected phone | `./gradlew installDebug` (USB debugging on) |
| Clear build cache | `./gradlew clean` |

**Notes**

- Device testing is done by the user on a physical phone, once after all phases are finished (Rule 8); no emulator is set up on the dev machine.
- The first build downloads dependencies into `~/.gradle` and takes several minutes.

---

## 5. Documentation Workflow

**Rule 0 — All documentation/context MUST live in `docs/` (versioned in git), NOT in the AI's local memory.**
Developers switch between machines, and the AI's local memory is not committed, so it is unavailable on other machines. Therefore: decisions, ticket status, analysis results, and any context that must survive across sessions/machines must be written to the appropriate `docs/` file (planning, feature, decision-log, or backlog) — not stored as local memory. AI agents must not rely on local memory as the source of truth for this project.

All documentation is written in the **Documentation language** set at the top of this file.

Never write code without going through this flow first. Do not create `implementation_plan.md` as a standalone artifact — use the official planning files below.

**Step 1 — Backlog (`docs/backlog.md`)**
New bugs/ideas go here without ticket numbers. Status: `OPEN` (ready to be planned) or `BLOCKED`. Remove the item once it becomes a planning ticket.

**Step 2 — Planning (`docs/planning/TICKET-NUMBER-NAME.md`)**
Create the plan before writing any code. Use `docs/planning/_template-implementation-plan.md`. Register in `docs/planning/index.md`. Get explicit user approval before executing.

Status lifecycle: `DRAFT` → `REVIEW` → `READY` → `DONE`.
- `DRAFT`: decisions not locked, execution blocked.
- `REVIEW`: decisions locked, waiting for user manual approval — do NOT execute.
- `READY`: user gave explicit written approval in chat — execution allowed.
- `DONE`: implementation and verification complete.

Each plan must include: Business Decision Snapshot, Non-Negotiable Technical Contract (target files, method signatures, integration points, return shapes), Acceptance Test Matrix (min. 1 boundary + 1 failure case, or `N/A — <reason>` if not relevant), and Out of Scope section.

When a ticket is DONE: move its file to `docs/planning/Ticket-Implemented/`, remove its entry from `docs/planning/index.md` (do not delete the index file itself). Check both locations when numbering new tickets to avoid duplicates.

`docs/planning/current-session.md` is updated only when the user explicitly asks. Do not create or modify it on your own initiative.

**Step 3 — Feature Docs (`docs/features/`)**
Every shipped feature needs a doc here, registered in [`docs/features/index.md`](features/index.md). Use the lean template: `# Title` → `**Status:** Live` → `## Summary` (2–4 sentences) → relevant sections (Quick Reference / catalogs as tables) → `## Gotchas` (non-obvious things only) → `## Related` (links to related docs).

Style rules (mandatory): no decorative emoji in headers; no commit hash/branch/date in Status; no motivational/marketing prose; method/field/column catalogs are always tables; every claim is verified against the actual code (anything unverified is not written); cross-links use standard Markdown links (`[name](name.md)`). Structural docs (module map, data reference) live one level up: `docs/architecture.md` and `docs/data-model.md`.

**Step 4 — Decision Log (`docs/decision-log.md`)**
Record a decision only if it meets at least one criterion:
- A technical gotcha/trap that cannot be derived from reading the code
- A business trade-off with non-obvious consequences
- A correction of a previously wrong assumption or mandate

Do not record: code cleanup, minor UI changes, or anything already documented in `ai-context.md` itself. SUPERSEDED entries must be deleted, not kept. Use the entry format in [`docs/decision-log.md`](decision-log.md).
