# Viewer follow-up issue — config step-up token on every write (after PR4 ships)

Author: companero (Fable), 2026-09-06. For the lead to hand to the VIEWER session (the public dashboard on Supabase/Vercel),
to file when write-server PR4 (+PR5) ships. Contract is the write-server surface-A step-up (RED `qa/c9-s12-write-server`,
S12A-1/2/3/5). The UX is already mocked in `dashboard-preview.py --config-login`. `[ev: RED e7e6615 S12A-1/2/3/5]` `[ev: proposal D-1 settled]`

## Title
`Viewer: send x-config-token on every /write and /alarms/ack after the config step-up (PR4 write-server)`

## Contract (what the viewer must do once PR4 is live)
The write-server now GATES config/control writes behind a step-up token (a shared password bound to the viewer's JWT
identity — one password for C9; per-user re-auth is a C10 seed). The browser flow:
1. Before the first write in a session, `POST <write-server>/config/login` with `{ "password": "<the shared config password>" }`
   and the usual `Authorization: Bearer <supabase-jwt>`. → **200** `{ "token": "<opaque>" }` on the right password; **401** `{ ok:false, error:'auth' }`
   (no `token`) on a wrong one — show the modal, let the operator retry. The login body has NO ttl field.
2. On EVERY `POST /write` AND `POST /alarms/ack`, send the header `x-config-token: <token>` (plus the Bearer JWT as today).
   Missing/expired/stale token → **403** `{ "error": "config_login_required" }` — the viewer catches 403+that body and
   re-opens the login modal, then replays the write.
3. TTL is **10 min sliding**, ENFORCED server-side (idle > `CONFIG_TTL_MS=600000` → the next gated request is 403
   `config_login_required`; the window slides on each accepted write). The write-server does NOT expose the remaining TTL:
   there is **no `/config/session` route** (that GET lives only in the `dashboard-preview` MOCK). For the chip countdown,
   derive it client-side from the known `CONFIG_TTL_MS` reset on each successful write — OR, if the server's value is
   wanted, extend the login response to `{ token, ttlMs }` (a one-line write-server change). `POST /config/logout` (with the
   token) ends the session immediately; the next write with that token is 403.
4. The token is NOT a cookie — it is an app-held value sent as the `x-config-token` header (CSRF-safe with the existing
   `X-Requested-With` XHR guard).

## Endpoints
| Method · path | Body / header | Success | Failure |
|---|---|---|---|
| `POST /config/login` | `{password}` + Bearer | 200 `{ token }` (NO ttl in the body) | 401 `{ ok:false, error:'auth' }` |
| `POST /config/logout` | `x-config-token` + Bearer | 200 | — |
| `POST /write` | `x-config-token` + Bearer + `{ord,value}` | 200 `{ok:true}` + one change_log row | 403 `config_login_required` (no/stale token); 400 invalid value; 502 station unreachable |
| `POST /alarms/ack` | `x-config-token` + Bearer + `{alarm}` | 200 | 403 `config_login_required` |

## Error codes the viewer handles
- **403 `config_login_required`** → open the config-login modal, keep the pending write, replay after 200.
- **401** on `/config/login` → "contraseña incorrecta", stay in the modal.
- **502** on `/write` → "estación no responde", the write was audited as `ok:false` server-side; offer retry.
- **400** on `/write` → invalid value; the field validation should have caught it (defence-in-depth).

## Acceptance
- A write without a prior `/config/login` returns 403 `config_login_required` and the modal opens (not a silent failure).
- After `/config/login` 200, writes and `/alarms/ack` succeed with `x-config-token`; each authorized write yields one
  `change_log` row (`surface='write-server'`, the token's id as `config_session`).
- After 10 min idle OR `/config/logout`, the next write is 403 and the modal re-opens.
- The chip/countdown is derived client-side from `CONFIG_TTL_MS` (or a `{token, ttlMs}` login extension) — NOT from a
  `/config/session` GET, which exists only in the `--config-login` mock, not on the write-server.

## Not in scope
Per-user re-authentication (each operator with their own credentials) and a configurator role list are C10 seeds — C9 is
the single shared `CONFIG_PASSWORD`. `config_session` is the token id, opaque; identity is the JWT email in `user_email`.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | login {password}→token/401; x-config-token on write; 403 config_login_required; logout | [CERT] | RED e7e6615 S12A-1/2/3/5 |
| 2 | /alarms/ack gated | [CERT] | S12 plan Part 1 (guard order); proposal |
| 3 | TTL 10 min sliding ENFORCED; no /config/session route; login body {token} only | [CERT] | write-server.mjs @ 9d76b3c :281-289 (login {token}), :302-312 (403 config_login_required + sliding-TTL sweep) |
| 4 | 502 audited ok:false | [CERT] | S12A-8 (e7e6615) |
