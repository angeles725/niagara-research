<!-- review-status: pending -->
<!-- c9-plan -->
# C9 · S12 implementation plan — config-login step-up + audited setpoint write from the mini-PC (apply-ready)

Date: 2026-09-06 · Seed: C9 **S12** (DashboardPan servlet hardening + step-up auth) · Author: investigador1
Consolidates: [B803] (step-up), [B816] (write path), [B822] (additive-code), [B823] (no-code channels). Client cites
re-anchored at **`a109249` = current `origin/main`** of `angeles725/niagara-panccadia-leon` (PR #9 is docs-only on top
of `fbe9009`, so Java line numbers at `a109249` == `fbe9009`). `[CERT]` = verbatim file:line; `[INFER]` = design
decision not yet in code; **PENDING viewer** = a façade-export fact the viewer session is confirming.

> **Goal**: let the mini-PC write-server (Node, already holding the single station WRITE user) push a room setpoint
> **audited and behind a config-login step-up**, with NO Niagara module change in phase 1 (channel 3 = the module's own
> servlet, [B823]). The additive `applySetpoint` action ([B822]) is phase 2 only if the servlet path is rejected.

---

## Part 1 — Surface A: write-server (mini-PC) changes `[INFER — design; grounded in B803]`
The write-server gains a **config-session step-up** modeled on [B803] §803.6 (server-side allowlist + a fresh
short-TTL token bound to user+purpose, held server-side, never client-decided):
- **`POST /config/login`** — re-authenticate the operator against Supabase (email+password over TLS). On success mint a
  **config token**: random, server-held (in the write-server process / a store), TTL 2–5 min, bound to `(email + purpose="config-write")`.
  Return an opaque handle (httpOnly cookie or bearer). Base64/JSON is transport, not security — **TLS mandatory** ([B803] §803.2). `[INFER, B803 §803.6]`
- **`POST /config/logout`** — invalidate the config token immediately.
- **Inactivity expiry** — the token also expires after N min of no mutating call (sliding window). `[INFER]`
- **Token REQUIRED on the mutating endpoints** — `/write` (the setpoint path, Part 2) and `/alarms/ack`. A missing/expired
  token → 401 from the write-server, before it ever calls the station. Read endpoints (`/equipment`, `/alarms` GET) do NOT
  require it. `[INFER, mirrors B803 gate-0.5 "critical write adds step-up on top of the read path"]`
- **Server-side allowlist** — the write-server holds the set of ORDs it will write (the five `CuartoN/setpoint`); a write
  to any other ORD is rejected server-side (never client-decided). `[INFER, B803 §803.6 pt1]`
- **Audit — two rows per write**: (a) local **JSON-lines** file (append-only, the write-server's own trail); (b) a Supabase
  **`audit`** table row: `{ts, email, ord, old, new, result, ip, config_session}` where **`old` = a GET of the slot BEFORE
  the write** (read-modify-write, so the audit records the true prior value), `result` = the station's HTTP status, `email`
  = the Supabase config-session identity (the REAL operator — see the attribution caveat in Part 2). `[INFER — design]`

## Part 2 — The setpoint path (phase 1 = channel 3 servlet; phase 2 = additive action) `[CERT for the contract]`
**Phase 1 — write-server → the module's own servlet ([B823] channel 3, the one no-code path that works today):**
Exact request/response contract, re-anchored at `a109249`(=`fbe9009`):
```
POST /dashboardpan/api/setpoint HTTP/1.1        # servlet name "dashboardpan" (BDashboardServlet.java:81-84)
Authorization: Basic <base64(writeUser:pass)>   # or Cookie: JSESSIONID=<niagara web login>
X-Requested-With: XMLHttpRequest                # REQUIRED — else 302 Redirect (executable guard DashboardDispatch.java:121-126)
Content-Type: application/json

{"ord":"Cuarto1/setpoint","value":4.0}          # route: POST /api/setpoint -> SetpointWrite (DashboardDispatch.java:59-60)
```
- **Guards, in order (all [CERT] at `a109249`):**
  1. **X-Requested-With missing → 302** redirect home (NOT 4xx) — the executable `/api/` XHR guard → `Redirect(REDIRECT_HOME)` at `DashboardDispatch.java:121-126` (the `SetpointWrite` route is `:59-60`; earlier line refs were the class javadoc, not code).
  2. **No authenticated station user → 401** `SC_UNAUTHORIZED` — `DashboardRbacHelper.java:36,41` (`req.getRemoteUser()`).
  3. **User lacks `OPERATOR_WRITE` bit → 403** `SC_FORBIDDEN`, **FAIL-CLOSED** (any exception denies) — `DashboardRbacHelper.java:19,55-56`. Checks the permission BIT, not a role name; username is SlotPath-escaped (`Cristian Angeles`→`Cristian$20Angeles`, `:49`).
  4. **Invalid value → 400** `SC_BAD_REQUEST` — the PR#7 numeric-validation guard "reject missing/empty/NaN before coercion" at `BDashboardServlet.java:274-283` (plus the ORD-resolve/traversal 400s at `:216,225,234,256,265`).
- **The write:** `coerceValue(current, value)` (`BDashboardServlet.java:357`, `new BStatusNumeric(parseDouble)`) → `parent.set(prop, toSet, null)` (`:291`) — reaches the same slot as `setSetpoint`, via a **null-Context** servlet write. `[CERT]`
- **Response:** `200` `{"ok":true}` on success; the 3xx/4xx above otherwise. `[CERT]`
- **Audit lands in BOTH trails:** (a) the module's `auditLog` ring — `svc.appendAudit(JsonUtil.buildAuditEntry(...))` (`BDashboardServlet.java:312`; ring 500, `BDashboardService.java:68-72,256`), `{ts,user,action:"setpoint",ord,oldValue,newValue}`; **and** (b) the write-server's Supabase `audit` (Part 1). `[CERT for (a)]`
- **Single-station-user attribution caveat — now `[CERT]`, settled by CODE in B829 (`d26305d21`):** the servlet write uses `parent.set(prop, toSet, null)` (null Context). The framework builds a config `AuditEvent` ONLY inside `if (context != null && context.getUser() != null)` (`ComplexSlotMap.set:662`) and dispatches it only if `Nre.auditor != null` (`:1685`, set by `BAuditHistoryService`). So the servlet's null-Context write is **NOT audited at all — the event is SUPPRESSED, not merely unattributed**, and this holds *even with* an `AuditHistoryService` installed (it is not a station-config unknown any more). The oBIX PUT (Part 1) IS audited but to the SHARED oBIX login user (`ObixUtils:558`); only a fox/Workbench edit carries the real user. So no Niagara trail attributes the real remote operator — **the real operator identity lives ONLY in the write-server's Supabase `audit.email`** (the config-session), and Niagara AuditHistory is the out-of-band cross-check for direct Workbench edits. That is WHY Part 1's audit is authoritative, not the module ring. `[ev: corpus B829]`
  - **B829-G1 — CLOSED by a bog read `[CERT]`:** an `AuditHistoryService` IS installed on PANCCADIA — `tools/bog-nav.py <config.bog> find --type h:AuditHistoryService` → `Services/AuditHistoryService` (id `/PANCCADIA/AuditHistory`, recordType `history:AuditRecord`). So `Nre.auditor` is set; the servlet suppression is purely the null-Context gate, nothing missing. `[ev: corpus B829]`
  - **B829-G2 — the surface-B fix (small, schema-neutral):** pass a REAL user Context from the servlet — `parent.set(prop, toSet, cx)` with the authenticated request user instead of `null` — and the servlet write becomes Niagara-audited to that operator (a second, native trail alongside the Supabase one). Client work item for surface B. `[ev: corpus B829]`

**Phase 2 — the additive `applySetpoint(BDouble)` action ([B822]), ONLY if the servlet path is rejected** (e.g. they want
an oBIX-native write + a Niagara-side audit event): add `@NiagaraAction(flags=Flags.OPERATOR) applySetpoint(BDouble)` on
`BRoomPanel`, `doApplySetpoint`→`setSetpoint`. oBIX-native (`<op>`, POST→`BComponent.invoke` under `OPERATOR_INVOKE`,
`BDouble` arg from `<real>`; [B822] §822.4). Schema-**SAFE** (`add_slot(action)`, [B795]). Do NOT retype `setpoint`
(LOSSY→OUTAGE, [B800] §800.8). `[CERT — B822]`

## Part 3 — Surface B (the HMI servlet) step-up, later `[INFER, B803]`
The HMI's own critical writes (operator at the panel) get [B803] step-up in a later phase: a re-auth modal → the real
`x-niagara-csrfToken` double-submit ([B803] §803.5 — the servlet has **NO** CSRF token today, Part 6) + a server-side
step-up token for critical ORDs. Not in scope for the write-server work; recorded so the two surfaces converge. `[INFER]`

## Part 4 — RED shapes per component `[INFER — test design]`
- **write-server unit tests** (Node): `/config/login` mints a token; `/write` and `/alarms/ack` WITHOUT a token → 401;
  an EXPIRED token → 401; a write to a non-allowlisted ORD → 403; a successful write appends BOTH audit rows with
  `old` captured by a pre-write GET. RED = each assertion fails on the un-hardened server.
- **`DashboardDispatchTest`** (existing ux test, extend): assert the setpoint POST contract — XHR-missing→302, no-user→401,
  no-OPERATOR_WRITE→403, invalid-value→400 (the PR#7 guard), success→200+`auditLog` line. `[ev: corpus B823]`
- **schema-risk SAFE pin (phase 2 only)**: `schema-risk.sh` verdict = **SAFE** for the `applySetpoint` action add (add_slot,
  [B795]) — a RED that a retype would FAIL and the additive action PASSES. `[ev: corpus B795, B822]`

## Part 5 — Requires-execution (read-only FIRST; any write on a TEST room only, with Cristian's direct authorization) `[CERT — the gaps]`
- **B823-G1** (read-only): GET the PANCCADIA bog / Workbench-inspect whether `Services/DashboardService/Cuarto*/setpoint`
  is a link target (channel-6 question) AND capture the oBIX GET encoding of a `BStatusNumeric` (to settle the [B823]
  §823.2 escape-hatch). `[ev: corpus B823]`
- **B823-G2** (authorized, test room): the servlet `POST /dashboardpan/api/setpoint` lands 200 + one `auditLog` line — the
  channel-3 proof, the lowest-risk already-exercised path. `[ev: corpus B823]`
- **B822-G1** (authorized, test room): a `POST /obix/config/…/applySetpoint` with `<real val=".."/>` invokes AND the oBIX
  login user's `OPERATOR_INVOKE` gates it — the phase-2 live check. `[ev: corpus B822]`
- All three run in ONE read-only-first authorized live session (companero paired them, seed `dda592d49`).

## Part 6 — Security notes `[CERT / INFER]`
- **No CSRF token on the servlet today `[CERT]`** — a full grep of `DashboardPan-ux/src` for `csrfToken`/`x-niagara-csrf`
  returns 0. The servlet's defense is `X-Requested-With` (302) + authenticated session (401) + `OPERATOR_WRITE` (403),
  NOT a CSRF token ([B823] §823.4; contrast [B803] §803.5 which recommends the real token). The write-server sends
  `X-Requested-With` and authenticates as the write user; acceptable for a server-to-server call over TLS, but note the
  gap for the HMI browser path (Part 3). `[CERT]`
- **TLS mandatory** — the Supabase re-auth, the config token, and the station Basic/JSESSIONID all cross the wire; TLS is
  non-negotiable ([B803] §803.2). `[INFER, B803]`
- **Server-side allowlist** — the write-server writes only the five `CuartoN/setpoint` ORDs; never a client-supplied ORD
  ([B803] §803.6). `[INFER, B803]`
- **Overlap ([B816])** — `setpoint` is written by the servlet (this path) AND potentially by a link/HMI; the servlet
  `set()` serializes per-slot (last-writer-wins, no torn value, [B816] §816.1), but if a link also drives `setpoint` the
  write is EPHEMERAL (overwritten next propagation, [B816] §816.2). Confirm `setpoint` is a direct-write config slot (not
  link-driven) at B823-G1 before relying on the write sticking. `[CERT — B816]` **PENDING viewer** (façade link topology).

---
## Sources & tokens
[B803] step-up/CSRF · [B816] write-path overlap · [B822] additive action + schema-risk · [B823] no-code channels + the
servlet contract · [B795] add_slot=SAFE/retype=OUTAGE · [B800] §800.8 retype outage. Client cites re-anchored at
`a109249`(=`fbe9009`) this session (BDashboardServlet/DashboardDispatch/DashboardRbacHelper). Nothing invented; the
write-server/Supabase specifics are `[INFER]` design (the lead's spec), the servlet contract is `[CERT]`, the façade link
topology is **PENDING viewer**.
