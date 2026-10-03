# Decision Log

Recording criteria: see [ai-context.md §5 Step 4](ai-context.md#5-documentation-workflow).

Entry format (4 fields):

```markdown
## DEC-XXX — Short title

**Decision:** What was decided.

**Why:** Why — the constraint, bug, or wrong assumption.

**Impact:** Concrete consequences that are not obvious from the code.

**Tickets:** TICKET-XXX (optional)
```

---

<!-- ADD DECISION LOG ENTRIES BELOW -->

## DEC-001 — Trip terputus dideteksi dari DB + state in-process, state di-set sebelum service jalan

**Decision:** Trip dianggap terputus bila berstatus `active` di DB tetapi `TrackingStateHolder` tidak `Active` untuk trip itu. `TrackingController.start()` men-set `TrackingState.Active` segera setelah `startForegroundService` dipanggil, tidak menunggu service.

**Why:** Service baru men-set state setelah coroutine-nya membaca trip dari DB. Di celah itu trip sudah `active` sementara state masih `Idle`, sehingga Home akan menampilkan dialog "perjalanan tidak selesai" untuk trip yang baru dimulai.

**Impact:** Kode baru yang memulai service atau mengubah `TrackingStateHolder` harus menjaga invarian ini: state `Active` selalu ditetapkan sebelum atau bersamaan dengan trip menjadi `active`, dan baru kembali `Idle` setelah trip `completed` (service memanggil `finish` dulu, lalu set `Idle`). Trip terputus diakhiri dengan `ended_at` = titik terakhir, bukan waktu sekarang.

**Tickets:** TICKET-010

