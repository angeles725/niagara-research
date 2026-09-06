# C9 PR4 (S12-A config login) GREEN read — feat/c9-s12-config-login b077ad4 vs tunnel 872c64c

investigador1, 2026-09-06. Read-only in `Pancaddia-worktrees/pr4-config-login` @ b077ad4 (also origin PR #1). Diff base
872c64c. Files: `write-server.mjs` (+181/-59), `package.json` (test glob), the RED test file. `[ev: git diff 872c64c..b077ad4]`

## Verdict
The config-login feature is correctly built and matches D6 on every structural invariant. Two findings: one OPERATIONAL
(merging PR4 alone turns main's `npm test` red — the RED spans PR4+PR5), one SECURITY (the config token never expires —
sliding TTL is scaffolded but unimplemented and unpinned).

## PF1 — the S12-A RED spans PR4 AND PR5; PR4 alone greens only 5 of 9 (main would go RED)
The RED file at b077ad4 carries all nine cases; I ran `node --test`:
- **PASS (PR4 scope):** S12A-1 no-token→403, S12A-2 wrong-pw→401, S12A-3 login→200+token, S12A-5 logout→403 + stale→403,
  S12A-7 setpoint child `/value` bare `<real>`.
- **FAIL (PR5 scope, expected):** S12A-4 (row with the EXTENDED fields ts/config_session/result/surface/client_ip),
  S12A-6 (spool on insert failure), S12A-8 (failed-write row ok:false/result), S12A-9 (spool replay idempotency).
PR4's audit row is the a109249 shape (no extended fields), and `AUDIT_SPOOL` is declared but no spool is written — both are
PR5's job. So `package.json`'s `npm test` glob over `test/*.test.mjs` is **RED (4 failing) on this tip**. Merging PR4 to
main by itself makes `npm test` on main red until PR5 lands. **Decision needed** (not a code defect): land PR4+PR5 in one
push range, OR `skip`-gate S12A-4/6/8/9 until PR5 (a SKIP is not a PASS — C7 D9), OR sequence PR5 immediately in the same
merge. Flag in the PR body either way. `[ev: node --test @ b077ad4]`

## PF2 — the config token never expires: sliding TTL is scaffolded but not enforced (SECURITY)
`CONFIG_TTL_MS` is loaded (`write-server.mjs:64`) and `session.lastActivity` is written at login (`:284`) and refreshed on
every gated request (`:304`), but **nothing ever compares them** — no expiry check on `sessions.get`, no background sweep,
no clock dep in `buildServer(cfg, deps)`. A token lives forever until an explicit `/config/logout`. The comment "Clock-
injectable in tests" (`:63`) is false — `Date.now()` is called directly, so even the intended sliding-TTL is untestable as
written. No S12A case pins TTL, so the suite is green without it — a coverage AND implementation gap against D6's "short-TTL
token, absolute + sliding inactivity" and my line map. A step-up token whose whole security value is a short life currently
has none: a leaked token is valid until the operator happens to log out. Fix (PR5 or a follow): before use,
`if (Date.now() - session.lastActivity > cfg.CONFIG_TTL_MS) { sessions.delete(ct); return 403 config_login_required }`,
add an injectable clock dep, and add a CL8-analog pin (expiry + sliding). `[ev: write-server.mjs:63-64,:284,:302-304]`

## Checklist — structural invariants (all PASS)
| Invariant | Result | Cite |
|---|---|---|
| `export function buildServer(cfg, deps={})` returns a NON-listening server; `main()` calls `server.listen` | PASS | `:242`, returns `:359`; `main` `:364-368` |
| deps default to the real `obix()`/`verifyJwt()`/`auditChange()` when absent | PASS | `:243-245` (`?? obix / verifyJwt / auditChange`) |
| `isMain = process.argv[1] != null && import.meta.url === pathToFileURL(process.argv[1]).href` guards `main()` | PASS | `:373-374` |
| token store = per-instance `Map` (inside buildServer), opaque `crypto.randomBytes(32).toString('hex')`, bound to JWT email, never on disk | PASS | `:248`, `:283-284` |
| `/config/login {password}` vs ONE `cfg.CONFIG_PASSWORD` → 401 otherwise; JWT required first | PASS | `:281-285`; `verify(bearer)` `:262` runs before routing |
| `/config/logout` deletes the token immediately | PASS | `:290-292` |
| `/write` AND `/alarms/ack` gated on `x-config-token` → 403 `config_login_required` | PASS | `:300-303` |
| ORD allowlist + child `/setpoint/value` bare `<real>` PUT unchanged; `WRITABLE`/`obixBody` untouched | PASS | `:312-320` (`putOrd` unchanged); no diff in WRITABLE/obixBody |
| existing best-effort `change_log` call kept on the success branch (now try/catch-wrapped; PR5 extends the row) | PASS | `:325-337` |
| no other route changed (`/health`, OPTIONS, 405, 404, 500) | PASS | unchanged in the diff |

## Minor / benign (note, not drift)
- `rateLimited` guarded with `cfg.RATE_MAX != null &&` (`:250-251`) — a test affordance; inert in production (`loadConfig`
  always sets `RATE_MAX = Number(cfg.RATE_MAX || 60)`, never null).
- CORS `(cfg.ALLOWED_ORIGINS || [])[0] || ''` (`:218-219`, `:257-258`) — null-safety for cfg without origins; inert in prod.
- `appendFileSync` import (`:19`) and `AUDIT_SPOOL` cfg (`:65`) are added but UNUSED in PR4 — PR5 spool scaffolding; dead
  until PR5. Acceptable as a chained-PR seam; remove if PR5 slips.
- deps shape is `{obix, verifyToken, changeLog}` (function injection), not my line map's `{supabase, station, clock, spool}`
  guess — cleaner; but note there is NO clock dep, which is why PF2's TTL is untestable as written.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | PR4 greens S12A-1/2/3/5/7; S12A-4/6/8/9 fail (PR5 scope) | [CERT] | `node --test` run @ b077ad4 |
| 2 | No TTL/expiry check, no sweep, no clock dep; CONFIG_TTL_MS + lastActivity dead | [CERT] | grep — only writes, never compared |
| 3 | buildServer non-listening, deps default real, isMain guard, token store as specified | [CERT] | line cites above |
| 4 | ORD/child-PUT/change_log-on-success unchanged | [CERT] | diff |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
