# Map Track — Documentation Hub

Documentation hub for the Map Track project (offline-first Android location & trip tracker). Written to be easy to digest for both developers and AI agents.

**Stack:** Kotlin · Jetpack Compose · Room · Fused Location Provider · Foreground Service · MapLibre (Android) · Laravel backend from Phase 6

**Status:** Fase 1 (Core Tracking Lokal) selesai diimplementasikan (TICKET-001–011); menunggu uji manual di HP — checklist di [TICKET-011](planning/Ticket-Implemented/TICKET-011-phase1-closeout.md#6a-checklist-uji-manual-fase-1-gabungan-untuk-user). Detail Fase 2–9 sudah lengkap di PRD §38 (v2.2); item per fase ada di [backlog.md](backlog.md), Fase 2 (Place Detection) siap dijadikan tiket.

## Start here

Reading order to understand the project without reading code:

1. **[AI Context & Mandates](ai-context.md)** — absolute rules, architecture boundaries, dev environment commands. Read first.
2. **[Architecture Map](architecture.md)** — module map, public surface, and how they relate.
3. **[Data Model Reference](data-model.md)** — all tables/collections, columns/fields, relations, and core enums.
4. **[Feature Docs Index](features/index.md)** — per-feature details.

## Initial Documents

- **[PRD — Map Track](initiate-file/prd-map-track.md)** — product requirements for the full product in 9 phases (source of scope & acceptance criteria).

## Process & Planning

- **[Backlog](backlog.md)** — bugs/ideas not yet ticketed.
- **[Planning](planning/index.md)** — active implementation plans.
- **[Decision Log](decision-log.md)** — technical decisions & non-obvious trade-offs.

---

*Feature docs live in `docs/features/`. Unimplemented items live in `docs/backlog.md`; active plans in `docs/planning/`.*
