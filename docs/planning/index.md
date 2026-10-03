# Implementation Plan Index

Use this file to orchestrate active ticket plans with clear order and dependency.

Status meanings (`DRAFT` / `REVIEW` / `READY` / `DONE`): see [ai-context.md §5 Step 2](../ai-context.md#5-documentation-workflow).

## Execution Order

— (Tidak ada tiket aktif. Fase 2 selesai: TICKET-012–016 di `Ticket-Implemented/`. Fase 3 `OPEN` di [backlog](../backlog.md), siap dijadikan tiket mulai TICKET-017.)

## Global Agent Rules

1. Only execute a ticket the user explicitly asks for, and only if its status is `READY`.
2. Read each ticket's `Business Decision Snapshot` before any code change.
3. Follow each ticket's `Non-Negotiable Technical Contract` exactly.
4. Stop and report if a dependency ticket is incomplete or inconsistent.
