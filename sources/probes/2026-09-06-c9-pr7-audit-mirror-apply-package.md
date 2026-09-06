# C9 PR7 / R7 — AuditHistory → `change_log` MIRROR: apply package (tunnel + kit doc)

Author: companero (Fable), 2026-09-06. Contract VERBATIM from QA's RED `qa/c9-s12-audit-mirror` **`0a14df8`** (parent tunnel
main `9acb47c`; `instalacion/pipeline/test/audit-mirror.test.mjs`, MIR1–MIR5, `node --test`, no npm deps) + design D7a.
The real-record mapping is grounded in B829 §(AuditRecord fields). `[ev: tunnel 0a14df8 audit-mirror.test.mjs]` `[ev: design D7a]`
`[ev: corpus B829 :68-70 (BAuditRecord fields)]` `[ev: corpus B830 §830.4 (AuditEvent has no session field)]`

## 0. Settled / dependencies
- **Flag OFF by default** and "off" means `readAuditHistory` is **never called** — not merely zero rows (MIR1; the mirror must
  not touch the station on every tick when disabled). Enabling it live is the **B829-live gate**, never a PR gate.
- **Surface literal `'servlet'`** (MIR4; D7 :39/:64). **`config_session` is NULL, never fabricated** (MIR5) — the framework's
  `AuditEvent` carries no session field (`ComplexSlotMap.java:1687`), so there is nothing to put there (B830 §830.4).
  R14 later changes what the `user` column carries (the second operator), NOT this column.
- **Dedupe key = the FULL 5-tuple `(ts, user, target, old, new)`** — never `ts` alone or `(ts, user)`: two legitimate changes
  in the same clock tick must stay distinct (MIR3; the fixture has two records sharing a `ts`).
- Depends on PR5's schema (`surface`, `config_session` columns exist) — sequence PR7 after PR5 (or PR5+PR7 together).

## 1. The contract (verbatim from the RED)
| Piece | Pin | Exact expectation |
|---|---|---|
| Module | header `:4-5` | NEW `instalacion/pipeline/audit-mirror.mjs` exporting `async function runMirror(cfg, deps) → { read, inserted, skipped }` |
| Flag | MIR1 `:47-54` | `cfg.MIRROR_ENABLED` absent/false → `deps.readAuditHistory` call count **0**, `changeLog.rows.length` 0, `r.inserted` 0 |
| Source | `:7-8` | `deps.readAuditHistory()` → `Array<{ ts, user, target, old, new }>` (a RECORDED `/PANCCADIA/AuditHistory` export in tests; the real job maps Niagara records to this shape, §2) |
| Sink | `:9-10` | `deps.changeLog = { rows, insert(row), has(key) }`; `has` = dedupe lookup on the 5-tuple key |
| Key | `:34` | `[ts, user, target, old_value ?? old, new_value ?? new].join('\|')` — the test's own key function; a record and its inserted row hash to the SAME key |
| Idempotent replay | MIR2 `:56-64` | first pass inserts every DISTINCT record (`after1 === FIXTURE.length`); second pass adds **0** rows |
| Full-tuple dedupe | MIR3 `:66-76` | second run → `inserted === 0`, `skipped === FIXTURE.length`; inserted keys are all distinct AND `keys.length === FIXTURE.length` (the two same-`ts` records both survive) |
| Row | MIR4 `:78-84`, MIR5 `:86-90` | every inserted row `{ ts, user, target, old_value, new_value, surface: 'servlet', config_session: null }` — `'config_session' in row` AND `=== null` |
| Named mutations (K13) | `:16-21` | ignore the flag → rows with the flag off (MIR1); drop the `has` check → replay doubles (MIR2); key on `ts` only → the two same-`ts` records collapse (MIR3); mislabel surface (MIR4); derive `config_session` from `user` → non-null (MIR5) |

## 2. The REAL export — mapping `/PANCCADIA/AuditHistory` records to `{ts, user, target, old, new}`
`history:AuditRecord` fields (B829 :68-70, `BAuditRecord.fromEvent` copies field-by-field, null→""):
| AuditRecord field | → mirror field | Note |
|---|---|---|
| `timestamp` (`Clock.time()`, ms) | `ts` | keep epoch ms (the fixture uses ms integers) |
| `userName` (`user.getUsername()`) | `user` | present for oBIX/fox/Workbench writes; for the servlet path it exists ONLY after R14 (before R14 the servlet write is not audited at all — B829) |
| `target` (component slot-path body) + `/` + `slotName` | `target` | the fixture's `Programacion/ColdRoom_1/setpoint` = target body + slot |
| `oldValue` | `old` | string as recorded |
| `value` | `new` | string as recorded |
| `operation` | (filter) | mirror ONLY `Changed` (slot writes); skip `Added/Removed/Invoked/…` — or carry it in a future column, additive |
**Reading the history:** the mini-PC already speaks oBIX to the JACE (`poller.mjs` `obixRequest(cfg, method, absPath, body)`
`:56`). The AuditHistory is a station history → `POST /obix/histories/PANCCADIA/AuditHistory/~historyQuery` with an
`obix:HistoryFilter` (`<obj is="obix:HistoryFilter"><int name="limit" val="500"/><abstime name="start" val="<last ts>"/></obj>`)
returning `obix:HistoryQueryOut` records whose child names are the record's slot names above — **confirm the exact column
names on ONE live query before wiring the mapper (B829-live / B830-G1 gate)**; keep the mapper a pure function
`mapRecord(obixRecord) → {ts,user,target,old,new}` with its own fixture. `[ev: tunnel poller.mjs:56 @ 9acb47c]` `[ev: oBIX 1.1 §history (CERT-doc: HistoryFilter/HistoryQueryOut)]` `[INFER: exact column names until the live query]`

## 3. File-level diff plan (`instalacion/pipeline/`)
| # | File | Edit |
|---|---|---|
| F1 | NEW `audit-mirror.mjs` | `export async function runMirror(cfg, deps = {})`: `if (!cfg.MIRROR_ENABLED) return { read: 0, inserted: 0, skipped: 0 };` (before ANY read) → `const recs = await deps.readAuditHistory();` → for each: `key = [r.ts, r.user, r.target, r.old, r.new].join('\|')`; `if (await deps.changeLog.has(key)) skipped++; else { await deps.changeLog.insert({ ts: r.ts, user: r.user, target: r.target, old_value: r.old, new_value: r.new, surface: 'servlet', config_session: null }); inserted++; }` → return `{ read: recs.length, inserted, skipped }`. Pure over its deps; no `loadConfig()` inside. |
| F2 | `audit-mirror.mjs` — the DEFAULT deps (only used when `deps.x` is absent) | `readAuditHistory` = oBIX history query (§2) with a persisted high-water `ts` (a small state file, e.g. `mirror-state.json`, so each run reads only new records); `changeLog.has(key)` = a Supabase query on the 5 columns (`ts=eq.&user=eq.&target=eq.&old_value=eq.&new_value=eq.`, `select=id`, `limit=1`) — or a unique index (§4); `changeLog.insert` = the same `/rest/v1/change_log` POST as `auditChange` (service key). |
| F3 | `write-server.mjs` (or a `mirror-cron.mjs`) | CLI entry `node audit-mirror.mjs` under an `import.meta` guard, run by cron on the mini-PC every N min with `MIRROR_ENABLED` from `config.env` (default absent = OFF). |
| F4 | `sql/2026-09-0X-change-log-mirror-index.sql` (ADDITIVE) | `create unique index if not exists change_log_dedupe_idx on public.change_log (ts, user_email, target, old_value, new_value) where surface = 'servlet';` — makes the 5-tuple dedupe a DB guarantee (the `has` check stays as the fast path; an insert conflict is then `skipped`, not an error). NOTE the sink column is `user_email` today; the mirror's `user` is a station username, not an email — write it to `user_email` (the identity column) and document, or add `user_name text` additively — **decision for the lead**: the RED's row uses the key `user`; the DB column name is the sink adapter's mapping. |
| F5 | `package.json` | `"test": "node --test test/"` already covers `audit-mirror.test.mjs` (5 more pins → 14/14 with S12-A's 9). |
| F6 | kit doc (the "+ kit doc" half of PR7) | one line in `types/dashboard.md` (or BUILD-LOOP §6): "the DashboardPan write trail is unified in `public.change_log`: write-server rows `surface='write-server'` with `config_session`; servlet rows mirrored from AuditHistory `surface='servlet'`, `config_session` NULL, deduped on `(ts,user,target,old,new)`; mirror flag-gated OFF; audit-append never fails the write" `[ev: proposal §5] [ev: corpus B829]` — see the PR12 doctrine draft for the exact line. |

## 4. Column mapping caveat (flag for the lead, small)
`change_log.user_email` is `text not null` (9acb47c schema); the mirror's `user` is a station username (`userName`), not an
email. Options: (a) write it into `user_email` as the identity column (simplest; rename the column semantics in docs);
(b) add `user_name text` additively and leave `user_email` NULL-able for servlet rows (needs `alter column drop not null`
— NOT additive-only). Recommend **(a)** for C9 (no schema change) with the doc line stating "identity column, email OR
station username by surface". The RED is agnostic (it checks the row key `user` handed to `insert`, not the DB column).

## 5. Mutation proof to record (K13)
(1) run with the flag off → MIR1 reads>0 flips; (2) delete the `has` check → MIR2 second pass inserts 3 (flips); (3) key
on `ts` only → MIR3 `keys.length` drops to 2 (flips); (4) `surface: 'write-server'` → MIR4 flips; (5) `config_session:
user` → MIR5 flips.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | contract rows in §1 | [CERT] | test file `:4-21, :34, :47-90` @ 0a14df8 |
| 2 | flag-off = never read; 5-tuple key; `'servlet'`; `config_session` null | [CERT] | MIR1/MIR3/MIR4/MIR5 + D7a table |
| 3 | AuditRecord field names | [CERT] | B829 :68-70 (`BAuditRecord.fromEvent`) |
| 4 | oBIX historyQuery shape / exact column names | [CERT-doc / INFER] | oBIX spec; confirm on one live query |
| 5 | `user_email` vs station username | [CERT, decision pending] | sql/2026-09-06-change-log-audit.sql @ 9acb47c |
