# C9 R5 — `change_log` additive migration + `buildServer(cfg, deps)` seam + failure spool: apply package (tunnel)

Author: companero (Fable), 2026-09-06. Contract VERBATIM from QA's RED `qa/c9-s12-write-server` **`55d6797`** (on tunnel
main **`9acb47c`**; `instalacion/pipeline/test/write-server.config-login.test.mjs`, S12A-1..S12A-7, `node --test`, no npm
deps). Baseline read at `9acb47c`: `write-server.mjs` (309 lines), `sql/2026-09-06-change-log-audit.sql`, `package.json`.
Clone: `/home/cristian/tunnel/clientes/Leon-Guanajuato/Pancaddia` (Guanajuato WITH the `a`). `[ev: tunnel 55d6797 test file]`
`[ev: tunnel write-server.mjs @ 9acb47c]` `[ev: proposal §5 / R5]` `[ev: S12 plan Part 1 (a6268a67b)]`

## 0. Scope, dependency, and one delta for the lead
- **R5 scope** (this package): the ADDITIVE `change_log` migration (`+config_session, +result, +surface, +client_ip`), the
  `buildServer(cfg, deps)` testability seam + guarded auto-run, the extended row per write, and the JSON-lines FAILURE
  SPOOL. The login/token pins (S12A-1/2/3/5) are **R4's contract** (RED 24adcba, base e4b42b0) and live in the SAME test
  file — the seam is shared, so whichever PR lands first carries `buildServer`; R5 assumes R4's login shape as pinned.
- **D-1 (flag, not decided here):** the RED's step-up is `POST /config/login {password}` checked against **one shared
  `cfg.CONFIG_PASSWORD`**, bound to the viewer's JWT `email` (`deps.verifyToken`). The S12 plan Part 1 / proposal :80 describe a
  **per-user Supabase re-auth (email+password)**. The RED wins for apply (K13); the plan wording should be reconciled by the lead
  (a single config password is not a "user+password store" — SC-4 still holds — but it is a weaker identity proof than re-auth).
- **D-2 (settled by the RED):** `surface` value for the write-server rows is **`'write-server'`** (S12A-4), not the proposal's
  `'A'`. Migration default = `'write-server'`; the surface-B mirror literal is **`'servlet'`** (design D7 :39/:64 — not `'dashboardpan'`).

## 1. The contract (verbatim from the RED)
| Piece | Pin | Exact expectation |
|---|---|---|
| Seam | harness `:52-75` | `export function buildServer(cfg, deps)` returns a **non-listening** `http.Server`; the harness calls `server.listen(0, '127.0.0.1')` itself. `deps = { obix(method, path, body) → {status, body}, verifyToken(bearer) → {email, sub}, changeLog(row) → {ok} \| throws }`. |
| Auto-run guard | header `:23-26` | `main()` must NOT run on import: guard with `import.meta.url === pathToFileURL(process.argv[1]).href`; `loadConfig()`'s `process.exit(2)` must not fire on import either (the harness sets dummy env only until the guard lands). |
| cfg fields the harness passes | `:66-70` | `OBIX_BASE/USER/PASS, SUPABASE_URL, FACADE_PATH, WRITE_PORT:0, CONFIG_PASSWORD, AUDIT_SPOOL, ALLOWED_ORIGINS` — `buildServer` must accept a cfg OBJECT (not re-run `loadConfig()`), and tolerate absent `SUPABASE_SERVICE_KEY`/`RATE_*`/`JWT_ISS`/`ALLOWED_EMAILS` (defaults). |
| Transport | `post()` `:77-83`, `login()` `:88-92` | every request: `POST`, `content-type: application/json`, `authorization: Bearer <viewer-jwt>` (validated by `deps.verifyToken`), JSON body. Config token = **header `x-config-token`** (S12A-4/5/6/7), NOT a cookie. |
| S12A-1 | `:94-100` | `/write` with a valid JWT but NO `x-config-token` → **403** |
| S12A-2 | `:102-109` | `/config/login {password:'wrong'}` → **401**, no `token` in the body |
| S12A-3 | `:111-119` | `/config/login {password: cfg.CONFIG_PASSWORD}` → **200** `{token: <non-empty string>}` |
| S12A-4 | `:121-136` | `/write {ord:'Cuarto1/setpoint', value:4.0}` + token → **200**; `deps.changeLog` called **exactly once** with a row containing ALL of `ts, config_session, result, surface, client_ip, user_email, room, slot, old_value, new_value`; `room==='Cuarto1'`, `slot==='setpoint'`, **`surface==='write-server'`**, `config_session` a non-empty string bound to the step-up token; **the spool file has 0 rows** |
| S12A-5 | `:138-146` | after `/config/logout` (+ token) the next `/write` with that token → **403**; a stale token → 403 |
| S12A-6 | `:148-160` | `deps.changeLog` THROWS → `/write` still **200**; the spool (`cfg.AUDIT_SPOOL`, JSON-lines) has **exactly one** row, with `room==='Cuarto1'` and (`'error' in row \|\| 'result' in row`) |
| S12A-7 | `:162-171` | the oBIX call for setpoint is a `PUT` whose path ends `/setpoint/value`, body matches `/^<real\b[^>]*\bval=/` and contains no `<obj` — **already true at 9acb47c** (`putOrd = \`${ord}/value\`` for setpoint; `obixBody(NUM)` → `<real val="…"/>`) |
| Named mutations (K13) | header `:30` | spool on success too → S12A-4's zero-spool flips; swallow the insert error without spooling → S12A-6 flips |

## 2. File-level diff plan (all under `instalacion/pipeline/`)

### F1 — `sql/2026-09-06-change-log-extended.sql` (NEW, ADDITIVE ONLY — no column drop/retype)
```sql
-- R5: extend public.change_log (created by 2026-09-06-change-log-audit.sql) — the ONE canonical audit sink.
alter table public.change_log
  add column if not exists config_session text,                                  -- step-up token id (write-server) / station username (surface B, R7)
  add column if not exists result         integer,                               -- station HTTP status of the write (200 on success; the failing status otherwise)
  add column if not exists surface        text not null default 'write-server',   -- 'write-server' | (R7 mirror literal)
  add column if not exists client_ip      inet;                                  -- cf-connecting-ip || socket.remoteAddress
create index if not exists change_log_session_idx on public.change_log (config_session);
-- RLS (read anon+authenticated, insert via service_role), ts index and the 90-day retention already cover the table.
```
`ts` already exists (`default now()`); `ok` already exists and stays the boolean the RED's `'result' in row` alternative
complements. Run in Supabase SQL Editor / Management API like the 9acb47c file. `[ev: sql/2026-09-06-change-log-audit.sql @ 9acb47c]`

### F2 — `write-server.mjs` — the seam (`buildServer`) + guarded auto-run (structure-preserving refactor)
| # | Anchor @ 9acb47c | Edit |
|---|---|---|
| F2.1 | `import { readFileSync } from 'node:fs'` (`:17`), `fileURLToPath` (`:18`) | add `appendFileSync` and `pathToFileURL` imports |
| F2.2 | `loadConfig()` (`:24-58`) | UNCHANGED, but it is only called from `main()` (never at module top level) — its `process.exit(2)` therefore never fires on import |
| F2.3 | `obix(cfg, …)` (`:132-152`), `verifyJwt(cfg, token)` (`:186-207`), `auditChange(cfg, row)` (`:154-168`) | keep as the DEFAULT implementations; `buildServer` takes `deps = { obix, verifyToken, changeLog }` and falls back to them: `const obixFn = deps.obix ?? ((m,p,b) => obix(cfg,m,p,b))`, `const verify = deps.verifyToken ?? ((t) => verifyJwt(cfg,t))`, `const sink = deps.changeLog ?? ((row) => auditChange(cfg,row))`. `auditChange` must now THROW (or return `{ok:false}`) on a non-2xx / network error instead of only `console.error` — the spool logic keys on that (F2.6). |
| F2.4 | `async function main()` (`:231-308`) | split: `export function buildServer(cfg, deps = {})` = everything from `http.createServer(...)` (`:233`) to the end of the handler (`:305`), **returning `server` without listening**; `main()` becomes `const cfg = loadConfig(); const server = buildServer(cfg, {}); server.listen(cfg.WRITE_PORT, '127.0.0.1', …)` (`:306-307`). |
| F2.5 | `main().catch(...)` (`:309`) | guard: `if (import.meta.url === pathToFileURL(process.argv[1]).href) main().catch((e) => { console.error('[FATAL]', e); process.exit(1); });` |
| F2.6 | inside the handler, `/write` success branch (`:270-283`) | build the EXTENDED row and route it through the sink with the spool fallback — see the block below |
| F2.7 | handler auth (`:246-251`) | `user = await verify(token)` (the injected verifier) — unchanged semantics |
| F2.8 | `/config/login`, `/config/logout`, the `x-config-token` gate on `/write` (and `/alarms/ack`) | R4's contract (RED 24adcba; S12A-1/2/3/5); the token store is a module-level `Map<token, {email, issuedAt, lastActivity}>` inside `buildServer`'s closure (per server instance, so tests are isolated); `config_session` = that token's id |

**F2.6 — the extended row + spool, verbatim shape (inside the `/write` success branch, replacing the `auditChange(cfg, {...})` call at `:272-282`):**
```js
const row = {
  ts: new Date().toISOString(),
  user_email: (user.email || '').toLowerCase() || null,
  user_id: user.sub || null,
  room: 'Cuarto' + m[1], slot: m[2],
  label: (typeof label === 'string' && label) ? label.slice(0, 120) : null,
  old_value: oldVal, new_value: obixReadVal(xml),
  area: /Mode$/.test(m[2]) ? 'control' : 'config',
  ok: true, result: r.status,                       // station HTTP status
  surface: 'write-server',                          // S12A-4 literal
  config_session: session.id,                       // the step-up token id (never the raw secret if you rotate it)
  client_ip: ip || null,                            // computed at :243 for rate limiting
};
try { const s = await sink(row); if (s && s.ok === false) throw new Error('change_log insert rejected'); }
catch (e) {                                         // audit NEVER blocks or fails the write (S12A-6)
  try { appendFileSync(cfg.AUDIT_SPOOL, JSON.stringify({ ...row, error: String(e.message || e) }) + '\n'); }
  catch (e2) { console.error('[warn] audit spool falló:', e2.message); }
}
return send(res, 200, { ok: true, ord, value, by: user.email || user.sub }, cfg);
```
Rules the pins fix: exactly ONE `sink(row)` call per successful write (S12A-4); NOTHING is spooled when the sink succeeds
(S12A-4 zero-spool); on sink failure exactly ONE spool line carrying the row + `error` (S12A-6); the spool file is
`cfg.AUDIT_SPOOL` (default `join(HERE, 'audit-spool.jsonl')` when absent). **Proposal intent beyond the RED (not yet pinned):**
also write a row on a FAILED station write (`ok:false, result: r.status`) — add it, and ask QA to pin it (S12A-8).

### F3 — `package.json` — add the runner
`"scripts": { …, "test": "node --test test/" }` (the RED header: `node --test`, no npm deps). No new dependencies.

### F4 — spool replay (small, additive; not pinned by 55d6797)
A `scripts/replay-audit-spool.mjs` (or a `--replay-spool` flag) that reads `cfg.AUDIT_SPOOL`, re-POSTs each line to
`change_log`, and truncates on success — so a Supabase outage never loses rows. Recommend as part of R5 with its own pin
(S12A-9: replay drains the spool exactly once, idempotent on re-run).

## 3. Skeleton of the refactor (what `buildServer` looks like)
```js
export function buildServer(cfg, deps = {}) {
  const obixFn = deps.obix        ?? ((m, p, b) => obix(cfg, m, p, b));
  const verify = deps.verifyToken ?? ((t) => verifyJwt(cfg, t));
  const sink   = deps.changeLog   ?? ((row) => auditChange(cfg, row));      // auditChange now rejects on failure
  const sessions = new Map();                                                // R4: x-config-token -> {email, issuedAt, lastActivity}
  return http.createServer(async (req, res) => { /* :234-305 unchanged except: verify(), obixFn(), the token gate, F2.6 */ });
}
async function main() { const cfg = loadConfig(); const server = buildServer(cfg); server.listen(cfg.WRITE_PORT, '127.0.0.1', () => …); }
if (import.meta.url === pathToFileURL(process.argv[1]).href) main().catch((e) => { console.error('[FATAL]', e); process.exit(1); });
```
`obix()` keeps `cfg.OBIX_BASE` + the JACE Basic auth; the harness's injected `obix` receives `(method, absPath, body)` —
so the handler must call `obixFn(method, path, body)` with the SAME three args the default takes after `cfg`.

## 4. Mutation proof to record (K13, in the R5 retro)
(a) spool on success too → S12A-4 `auditRows(spoolPath).length == 0` flips; (b) swallow the sink error without spooling →
S12A-6 `spooled.length == 1` flips; (c) return `<obj …/>` for setpoint → S12A-7 flips; (d) drop the `import.meta` guard →
every test fails on `process.exit(2)` from `loadConfig()` (the RED's own "for the right reason" note).

## 5. Gates / consequences
- `lint`: no kit lint applies to the tunnel (Node); run `node --test test/` (7/7 → 9/9 with S12A-8/9) + `node --check`.
- **change_log consumers:** the viewer's audit view reads the new columns (`surface`, `config_session`, `result`, `client_ip`);
  pre-R5 rows have them NULL/`'write-server'` (default) — never backfilled/faked. The R7 mirror writes the same columns
  with `surface: 'servlet'` and `config_session: null` (D7 MIR5 — the identity is the `user` column, never the session column).
- **Retention:** `pancaddia_retention()` already deletes `change_log` past 90 days — the spool is not retained (drained by F4).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | seam/harness/transport/pins as tabulated | [CERT] | test file `:23-26, :52-92, :94-171` @ 55d6797 |
| 2 | `surface === 'write-server'` (not `'A'`) | [CERT] | S12A-4 `:132` |
| 3 | baseline handler anchors (`:17-18, :24-58, :132-168, :186-207, :231-309`), `ip` at `:243`, child-`/value` + bare `<real>` already true | [CERT] | write-server.mjs @ 9acb47c |
| 4 | existing schema (11 cols) + RLS/retention cover the table | [CERT] | sql/2026-09-06-change-log-audit.sql @ 9acb47c |
| 5 | login = shared CONFIG_PASSWORD bound to JWT email (R4 shape) vs the plan's Supabase re-auth | [CERT for the RED; flagged D-1] | test `:65-70, :88-92` vs S12 plan Part 1 |
| 6 | failed-write row, spool replay | [INFER, proposal intent] | ask QA for S12A-8/9 |
