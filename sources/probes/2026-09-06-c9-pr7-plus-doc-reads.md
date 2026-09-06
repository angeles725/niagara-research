# C9 reads — PR7 mirror (9d76b3c) + companero's four docs (99e267752 / 354723d8a / 7b4e385ee)

investigador1, 2026-09-06. `[ev: node --test + grep this session]`

## PR7 — audit-mirror 9d76b3c (stacked on 08e3ed6): CLEAN
`node --test test/audit-mirror.test.mjs` = **5/5**; full glob `test/*.test.mjs` = **15/15** (the PR4+PR5+PR7 stack keeps
main green). Every invariant holds:
| Invariant | Result | Cite (audit-mirror.mjs) |
|---|---|---|
| flag OFF → `readAuditHistory` NEVER called (not "zero rows") | PASS | `if (!cfg.MIRROR_ENABLED) return` `:48` BEFORE `await deps.readAuditHistory()` `:52` |
| 5-tuple key `(ts,user,target,old,new)` with `??` fallback old_value/new_value vs old/new | PASS | `makeKey` `:34` `[r.ts,r.user,r.target,r.old_value ?? r.old,r.new_value ?? r.new]` |
| rows surface 'servlet', config_session null, station username → identity (`user_email`) | PASS | `:67-68`; identity note `:13-14`; `user: r.user` |
| `mapRecord` / `isMirrorable` pure | PASS | `:88` (returns null if !mirrorable), `:107` |
| real deps: oBIX `~historyQuery` via helper (`AUDIT_PATH :131`, HistoryFilter start/limit `:249`) + Supabase REST, `has` = select on the 5-tuple (`:278`), insert POST (`:293`) | PASS | `buildDeps` |
| `isMain` argv-safe | PASS | `:306` (same pattern as write-server) |
| `npm run mirror` | PASS | package.json `:12` |
| optional partial unique index SQL NOT applied | PASS | `sql/2026-09-06-change-log-mirror-index.sql`: "OPTIONAL … Apply in the Supabase SQL Editor when stable" `:9-10`, IF NOT EXISTS `:13`, rollback `:17-18` |
| MIRROR_STATE high-water cursor documented | PASS | `:128-130`, readState/writeState `:220-223`, advanced `:254-259` |
Minor (note, not a defect): with the optional index UNapplied, `has()` is a select→insert (TOCTOU), so two concurrent
mirror runs could double-insert. Acceptable — the mirror is single-instance and flag-gated; the DB index is the durable
backstop when enabled. `[ev: audit-mirror.mjs]`

## PR11 paste-ready 99e267752: CLEAN
62 = CompPan-rt 15 · ColdRoomPan-rt 6 · DashboardPan-rt 41 at PR1 tip `e5bee1c`; rotation rows already in the matrix
(`docs/write-path-matrix.md:95-96`), correctly NOT re-added (mentioned twice as "do not re-add", zero duplicate rows).
Cross-check: module-find OPERATOR-slot totals (9/19/45 = 73) exceed the uncovered 62 by 11 — consistent with the ~11
already-covered W1-W13 slots; I did not re-run `lint-write-path` (the R11 measurement is the [CERT] source). **Forward
note**: PR1 was reworked after my F1-F4 read (tip moved past 57a15d2); before pasting, confirm the two rotation rows are
STILL at `write-path-matrix.md:95-96` on the FINAL PR1 tip — a rework could shift those line anchors. `[ev: file 99e267752]`

## Viewer config-token issue 7b4e385ee: correct EXCEPT one drift
The flow (login→token/401, `x-config-token` on every /write + /alarms/ack, 403 `config_login_required` → modal+replay,
logout, 502 audited ok:false) matches PR4 exactly. **DRIFT — `/config/session` does not exist on the real write-server.**
The Acceptance line "the chip/countdown … reflects the real `/config/session` remaining TTL" and the endpoint table's
`/config/session` reference a route the write-server never implements: PR4's routes are `/config/login`, `/config/logout`,
`/write`, `/alarms/ack`, `/health` only, and `/config/login` returns `{ token }` with **no ttl** (`write-server.mjs:288`).
`/config/session` exists ONLY in the `dashboard-preview.py --config-login` MOCK. So the viewer cannot read remaining TTL
from the server. Fix: either derive the countdown client-side from the known `CONFIG_TTL_MS` (10 min), reset on each
accepted write, OR extend the login response to `{ token, ttlMs }` (a small PR4/PR5 add) — do not imply a real
`/config/session`. `[ev: write-server.mjs:288 (login returns {token}); no /config/session route]`

## PR12/PR13 refresh 354723d8a: one dangling-token drift
The REFRESH section is right (campaign9-demand-scope + silent-protection retros already on main, tokens resolve; land PR12
AFTER the other campaign9-* retros exist or `sweep-fold-audit --strict` fails). **DRIFT — Fold 4 carries a retro token
that no PR creates.** Fold 4's line ends `[ev: corpus B829][ev: corpus B830 §830.4][ev: retro campaign9-s12-write-audit]`
(`pr12-doctrine-fold-drafts.md:70`), but the close-apply-package retro set is campaign9-{demand-scope, silent-protection,
ext-writable-shape, doctrine-fold, wave-lessons, close-process-meta-lessons} — **no `campaign9-s12-write-audit`**. That
slug will DANGLE at `sweep-fold-audit --strict` (PR13 close gate), because the S12 write-audit work (PR4/5/7) is TUNNEL,
not a kit retro. Fix: drop `[ev: retro campaign9-s12-write-audit]` from Fold 4 (the two corpus tokens already carry it),
OR add a `campaign9-s12-write-audit` retro to the close set. Otherwise the close PR fails its own strict audit. `[ev: pr12-doctrine-fold-drafts.md:70]` `[ev: close-apply-package retro table]`

## Deploy runbook delta 7b4e385ee: CLEAN
Version map (Compresores 2.0.3→2.1.0→2.2.0, Paccadia 2.0.7→2.1.0, Dashboard 2.1.1→2.2.0), ordered deploy (pending PR#9
jars FIRST, then C9 in dependency order), and config-only rollback levers (rotationInterval=0, MIRROR_ENABLED=false,
CONFIG token TTL — never a jar downgrade because additive slots make a downgrade a schema-risk OUTAGE) are all correct
and consistent with B795/B828. Note (not drift): the "current" column is the a109249 REPO version; the DEPLOYED station is
older (CompPan 2.0.1 per RAR a34f9bdd2), which the runbook handles by requiring the pending PR#9 deploy first (`:19`).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | PR7 5/5 + glob 15/15; MIR1 gate before read; 5-tuple ?? key; surface/config_session; oBIX+Supabase deps; cursor | [CERT] | node --test + audit-mirror.mjs cites |
| 2 | PR11 62=15/6/41, rotation not duplicated | [CERT] | file 99e267752; OPERATOR totals 9/19/45 consistent |
| 3 | /config/session absent on write-server; login returns {token} no ttl | [CERT] | write-server.mjs:288; route list |
| 4 | Fold 4 `campaign9-s12-write-audit` token not in the close retro set → dangles | [CERT] | pr12:70 vs close-package retro table |
| 5 | deploy runbook config-only rollback correct; deployed<repo handled by pending-first | [CERT] | runbook + RAR a34f9bdd2 |
Tally: 5 [CERT] · 0 [INFER] · 0 unmarked.
