# Map Track — Documentation Hub

Documentation hub for the Map Track project (offline-first Android location & trip tracker). Written to be easy to digest for both developers and AI agents.

**Stack:** Kotlin · Jetpack Compose · Room · Fused Location Provider · Foreground Service · MapLibre (Android) · Laravel backend from Phase 6

**Status:** Fase 1 (Core Tracking Lokal, TICKET-001–011), Fase 2 (Place Detection, TICKET-012–016), dan Fase 3 (Saved Places, TICKET-017–022) selesai diimplementasikan. Uji manual di HP dilakukan sekali setelah semua fase selesai — checklist sejauh ini: [Fase 1](planning/Ticket-Implemented/TICKET-011-phase1-closeout.md#6a-checklist-uji-manual-fase-1-gabungan-untuk-user) dan [Fase 2](planning/Ticket-Implemented/TICKET-016-phase2-closeout.md#6a-checklist-uji-manual-fase-2-untuk-user), [Fase 3](planning/Ticket-Implemented/TICKET-022-phase3-closeout.md#6a-checklist-uji-manual-fase-3-untuk-user). Detail Fase 4–9 ada di PRD §38 dan [backlog.md](backlog.md); Fase 4 siap dijadikan tiket.

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
