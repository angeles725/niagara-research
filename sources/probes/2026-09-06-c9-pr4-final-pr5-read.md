# C9 PR4 final (a55153b) + PR5 (08e3ed6) reads — S12-A config-login + change_log extension

investigador1, 2026-09-06. Read-only in `Pancaddia-worktrees/{pr4-config-login @ a55153b, pr5-change-log @ 08e3ed6}` (PR5
stacked on a55153b). Suites run with `node --test`. `[ev: git diff + node --test]`

## PR4 final a55153b — PF2 (token TTL) is CLOSED
My earlier PF2 (config token never expires) is fixed:
- `buildServer(cfg, deps)` now takes `deps.now ?? (() => Date.now())` (`write-server.mjs:248`); comment `:241,:243` documents it.
- login seeds `issuedAt/lastActivity` from `now()` (`:287`); the gate compares `now() - session.lastActivity > cfg.CONFIG_TTL_MS`,
  deletes the token and returns 403 `config_login_required` on expiry (`:307-310`), and slides on every gated request (`:312`).
- No direct `Date.now()` remains on the token path (the surviving `Date.now()` are the rate window `:73`, JWKS cache `:188/:191`,
  and JWT `exp` `:206` — none touch the config token). `CONFIG_TTL_MS` default documented in config.example.env per the lead.
Verdict: PF2 resolved.

**PF1 still true on the ISOLATED PR4 tip (by design):** `node --test` at a55153b = S12A-1/2/3/5/7 PASS, S12A-4/6/8/9 FAIL
(PR5's extended-row + spool cases). So a55153b alone is 5/9 and merging PR4 by itself would make main's `npm test` red. This
is resolved by the stack (below) — see the merge-ordering note.

## PR5 08e3ed6 (stacked on a55153b) — 10/10 GREEN, no drift
`node --test test/write-server.config-login.test.mjs` = **tests 10 / pass 10 / fail 0**. Every pre-written invariant holds:
| Invariant | Result | Cite (write-server.mjs / sql) |
|---|---|---|
| Row on EVERY attempt (success + failure), one sink call per write | PASS | row `:338-351`, single `sink(row)` `:354-356` before the response branch `:362-365` |
| `result` = the write-server answer status (200 / 403 / **502 for oBIX 5xx** / 400) | PASS | `answerStatus :330-333`; `result: answerStatus :347` |
| `config_session` = the step-up token id, non-empty on surface A | PASS | login now stores `{ id: token, … }` `:286`; `config_session: session.id :349` |
| `surface: 'write-server'` (S12A-4 literal) | PASS | `:348` |
| `client_ip` from the existing `ip` (cf-connecting-ip ‖ socket.remoteAddress) | PASS | `client_ip: ip || null :350` |
| `old_value` from the pre-write GET only, never the body | PASS | `oldVal` from GET `:325-327`; `old_value: oldVal :343` |
| spool ONLY on insert failure — one JSON-lines row incl. the error; never blocks the write | PASS | try `sink` / catch → `appendFileSync(AUDIT_SPOOL, {…row, error})` `:354-359` |
| `auditChange` REJECTS on failure so the caller's catch fires | PASS | `if (!res.ok) throw` `:174` |
| `replaySpool` drains exactly once + idempotent + truncates the file | PASS | `:407-427`: reads lines, strips the spool-only `error` before re-insert, keeps only re-failed, `writeFileSync(path, kept.length ? … : '')` `:425` |
| migration additive-only / idempotent / commented rollback / NOT executed | PASS | `sql/2026-09-06-change-log-extend.sql`: `add column if not exists ×4` `:7-15`, `create index if not exists :18`, `-- ROLLBACK :23`, "Run in Supabase SQL Editor" `:3` (not run) |

Notes: (a) `surface text not null default 'write-server'` (`:13`) is additive-safe — Postgres 11+ adds a defaulted column
as metadata-only, existing rows take the default. (b) PR5 adds `id: token` to the session object created in PR4's login
handler (`:286`) — the only edit to PR4 code; needed so `config_session` is the token id. (c) the PR7 mirror dedupe index
(`change_log_dedupe_idx`, companero's PR7 F4) is separate from PR5's `change_log_session_idx` (`:18`) — no collision.

## Merge-ordering (the one carry-over — decision, not a defect)
PR4 and PR5 must land as ONE unit (merge the stack at PR5's tip, which contains PR4) so main never sees the 5/9
intermediate. If PR4 is merged to main as a separate commit first, `npm test` on main is red until PR5. Merging the
stack keeps main green throughout. Flag in the PR body.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | PR4 a55153b: TTL enforced via deps.now, swept on expiry, slid on activity; no Date.now on token path | [CERT] | `:248,:287,:307-312` + grep |
| 2 | PR4 isolated = 5/9 (S12A-4/6/8/9 fail); PR5 stack = 10/10 | [CERT] | node --test both tips |
| 3 | PR5 row fields result/config_session/surface/client_ip/old_value + spool-on-failure + auditChange-throws | [CERT] | line cites |
| 4 | replaySpool drains once + truncates; migration additive/idempotent/rollback/not-run | [CERT] | `:407-427`, sql file |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
