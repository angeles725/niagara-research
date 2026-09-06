# C9 R14 — the "second login inside DashboardPan before any write" UX, as a runnable mock (see it before code)

Author: companero (Fable), 2026-09-06. Product decision from Cristian (via the lead): the HMI panel keeps ONE shared
kiosk login, but a SECOND login INSIDE DashboardPan is required before any write — SPA modal → `/dashboardpan/api/config/login`
→ servlet re-auth against the STATION user DB → server-held config session with TTL + `/config/logout` → writes run
with that user's Context so Niagara AuditHistory attributes the real operator; NO credential storage in the module
(R14 / design D8b). investigador1 is researching the legal Niagara API (B830); the R14 apply-package follows B830.
This mock exists so the UX can be judged NOW, on the real dashboard, with zero module edits. `[ev: lead relay of Cristian's decision, 2026-09-06]` `[ev: corpus B829 (null-Context write is audit-suppressed → the real-Context write is the fix)]`

> **rev 2 (2026-09-06):** endpoints re-pathed to `/api/config/login|logout` to match QA's RED `qa/c9-s12-config-login` cc1c948 (CLW1) — under the existing `/api/*` dispatch branch, XHR guard for free. `/api/config/session` stays mock-only (the RED pins no session GET; the real chip derives its countdown from the login `ttl`).

## 1. Run it (30 seconds)
```
# the real SPA, read at the client tip (never the stale local checkout)
git -C ~/modulos_niagara_n4/Cliente/Leon-Guanjuato show a109249:Dashboard/DashboardPan/DashboardPan-ux/src/rc/index.html > /tmp/rc/index.html
python3 tools/dashboard-preview.py --rc /tmp/rc --prefix /dashboardpan --config-login --config-ttl 120
# open http://localhost:8080/dashboardpan/      (or /hmi for the 1280x800 WEB-HMI10 bezel)
```
Demo credentials: ANY username + password `1234` (`--config-password` to change). The real R14 servlet will check the
station user DB instead — the mock deliberately accepts one demo password so the 401 path can be shown.

## 2. What Cristian sees — the three-step flow
1. **Write without a session** — change a setpoint (or an HOA mode) on the dashboard as today. The SPA's own
   `POST /dashboardpan/api/setpoint` is intercepted: the **modal opens** ("Confirmar identidad para escribir" — usuario de
   la estación + contraseña; the write is HELD, not lost). Wrong password → inline "Credenciales inválidas", stays open.
   Cancelar → the held write returns a synthetic 403 and nothing is written.
2. **Login** — correct password → the modal closes, a **session chip** appears top-right ("🔒 cristian · 1:59" counting
   down), and the held write is **re-issued automatically** and lands (200). A **"Registro de cambios"** strip (bottom-
   right) shows the `change_log` row the R7 mirror would write: `hh:mm:ss  cristian  Cuarto1/setpoint  3.0 → 2.5  B`.
   Further writes go straight through while the session lives; each write extends it (sliding TTL).
3. **Logout** — "Salir" on the chip → session ends at once; the chip disappears; the NEXT write re-opens the modal.
   Letting the TTL run out does the same.
Kiosk fit: all targets ≥44px, the modal uses the SPA's own palette vars (`--surface/--line/--ink/--sage/--alarm`) and its
existing `.card/.ch/.acts` dialog geometry, so it reads as native, not bolted on.

## 3. What is MOCKED vs what the real R14 does (the contract the mock demonstrates)
| Piece | Mock (`--config-login`) | Real R14 (D8b) |
|---|---|---|
| `POST <prefix>/api/config/login {user,pass}` | any user + demo password → `200 {ok,user,ttl}` + cookie `dp_config_session`; else `401 {error:"auth"}` | servlet re-auths against the station user DB (B830 names the legal API); mints a server-held config session bound to user+purpose, short TTL; opaque handle only |
| `POST <prefix>/api/config/logout` | clears the one session, expires the cookie, `200` | revokes the session immediately |
| `GET <prefix>/api/config/session` (mock-only; the RED pins no session GET) | `{active,user,remaining,ttl}` | same shape (drives the chip) |
| `POST <prefix>/api/setpoint` (any `/api/*` POST) | no live session → `403 {error:"config_login_required"}`; with one → `200`, TTL extended, a `change_log` row appended | `DashboardWriteGuards` guard6 (config session required) → the write runs `parent.set(prop, toSet, cx)` with the SESSION USER's Context → AuditHistory attributes the real operator (B829-G2); the R7 mirror writes the same row to `public.change_log` with `surface='B'`, `config_session` = station username/session |
| `GET <prefix>/__mock/change_log` | the rows `{ts,user_email,config_session,room,slot,old_value,new_value,area,surface:"B",result,ok}` | the S12 canonical `change_log` schema (9acb47c + R5 additive columns) |
| XHR guard | kept: any `/api/*` or `/config/*` call without `X-Requested-With` → 302 (as the real dispatch) | unchanged |
| Credential storage | none (the mock keeps only the session token in process memory) | none in the module — R14 hard rule |
One shared panel → ONE config session at a time in the mock (a second login replaces the first) — the real design can
decide per-browser vs per-panel; the mock makes the simplest choice visible.

## 4. Proof — the HTTP flow, curl transcript (2026-09-06, port 8765, TTL 90 s, real `index.html` @ a109249)
```
1)  GET /                              → HTML carries the injected modal (2 marks)
2)  POST /api/setpoint  (no session)   → 403 {"ok": false, "error": "config_login_required"}
3)  POST /api/config/login  (wrong pass)   → 401
4)  POST /api/config/login  (pass 1234)    → 200 {"ok": true, "user": "cristian", "ttl": 90} + Set-Cookie dp_config_session
5)  POST /api/setpoint  (with cookie)  → 200 {"ok": true}
6)  GET  /__mock/change_log            → [{"ts":"04:07:40","user_email":"cristian","config_session":"a05c482c","room":"Cuarto1",
                                          "slot":"setpoint","old_value":null,"new_value":2.5,"area":"config","surface":"B","result":200,"ok":true}]
7)  GET  /api/config/session           → {"active": true, "user": "cristian", "remaining": 89, "ttl": 90}
8)  POST /api/config/logout            → 200 {"ok": true}
9)  POST /api/setpoint  (after logout) → 403
10) POST /api/setpoint  (no XHR header)→ 302   (the dispatch guard still bites)
```
10/10 as designed. `old_value` is `null` here because no `--mock` data file was given; with one, the mock reads the
prior value from it (mirroring the real pre-write GET).

## 5. What this does NOT decide (for B830 / the R14 apply-package)
- WHICH Niagara API the servlet uses to re-auth a station user and obtain a Context for `set()` (B830).
- Session store placement (servlet-local map vs a station-side service), TTL value, per-panel vs per-browser scope.
- Whether the modal also gates `/alarms/ack` (S12 says yes for the write-server surface A; mirror it on B).
- The exact `change_log` `config_session` value for surface B (station username vs a session id) — R7 decides.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | flow works end-to-end on the real index.html with zero module edits | [CERT] | §4 transcript; `tools/dashboard-preview.py --config-login` |
| 2 | the intercepted call sites are the SPA's real writes | [CERT] | `index.html:1335`, `:1929` (`fetch("/dashboardpan/api/setpoint")` with `X-Requested-With`) @ a109249 |
| 3 | modal geometry/palette mirror the SPA's `.card/.ch/.acts` + `:root` vars | [CERT] | `index.html:388-401`, `:11-23` @ a109249 |
| 4 | the real R14 contract column | [INFER, design] | lead relay / D8b; B830 pending |
