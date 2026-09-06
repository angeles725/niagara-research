# B823 · Can the EXISTING `BRoomPanel.setpoint` be written from the mini-PC with NO module change? — every no-code write channel enumerated, one-bit-traced `[CERT]`

> The NO-CODE half of the setpoint question (companion to [Block 822], which covers the ADDITIVE-code options —
> `setpointCmd`/`applySetpoint`/retype-schema-risk). Question, verbatim from the operator: with what exists today —
> `BRoomPanel.setpoint` = `BStatusNumeric`, `Flags.SUMMARY|OPERATOR`, exported at
> `/config/Services/DashboardService/CuartoN/setpoint` — can the write-server (Node on the mini-PC, already holding the
> single station WRITE user) push a new setpoint WITHOUT touching the module? Every candidate channel proven or ruled
> out, each `[CERT]` `file:line`, with a ranked recommendation and a read-only-first live plan.
>
> **Sources**: FUENTE 3 code — oBIX server path `organized/obixDriver/obixDriver-rt/vineflower/…` (mapper-forwarded
> by investigador1, spot-verified), client tree `Cliente/Leon-Guanjuato/…DashboardPan/{-rt,-ux}` @ `deed38c`,
> `write-server.mjs`/`poller.mjs` @ `8d738a2`. FUENTE 1 — [Block 822] (additive side), [Block 816] (write path),
> [Block 800] §800.8 (the retype OUTAGE), [Block 813]/[Block 763] (the servlet write gates), [Block 509] (oBIX/box).
> FUENTE 2 — niagara-help oBIX write semantics. Markers: `[CERT]` verbatim `file:line` · `[INFER]` · GAP (needs a
> live station read). Type: mixed (framework + live client). `[CERT]`

## 823.1 — The seven channels, at a glance `[CERT unless noted]`
| # | Channel | Works today, no module change? | Why |
|---|---|---|---|
| 1 | oBIX PUT of `setpoint` | **YES** with the exact wrapped body (LIVE-CONFIRMED) | bare/standard PUT rejected; `<obj is="…:StatusNumeric"><real name="value" val/></obj>` writes — but attr-only silently writes 0.0 |
| 2 | oBIX/fox INVOKE of an existing action → `setSetpoint` | **NO** | no non-HIDDEN action reaches the setpoint |
| 3 | **DashboardPan-ux servlet `POST /dashboardpan/api/setpoint`** | **YES** | the HMI writes setpoints through it TODAY |
| 4 | fox / `station:\|slot:` set (Node or Java/nre client) | heavyweight, not clean | needs a full Baja fox client + login |
| 5 | other HTTP (BajaScript `/ord`, Haystack, BACnet/Modbus) | NO / config-only | Haystack absent; BACnet only if the bog exports it |
| 6 | write an upstream Writable point linked into `setpoint` | GAP | no bog available; design is direct-write, not link-in |
| 7 | schedule/ext-driven | GAP | only if already in the bog; none verified |

## 823.2 — Channel 1: closed to STANDARD clients, but a WRAPPED PUT works — LIVE-CONFIRMED `[CERT-live]`
> **LIVE UPDATE (2026-09-06, Cristian-authorized read-only-first probe on Cuarto 1, viewer session) `[CERT-live]`**
> (full verbatim record: `sources/probes/2026-09-06-viewer-obix-setpoint-live-record.md`, SOURCES.md registered)**:**
> the escape hatch of §823.2 below is **CONFIRMED** — the exact working body is
> **`<obj is="/obix/def/baja:StatusNumeric"><real name="value" val="2.5"/></obj>` → `200 OK`, value `2.5 {ok}`,
> persisted** (not a display mirror). **Mechanism ([Block 825]):** the write is a TOP-SLOT REPLACEMENT (decode into a
> detached `newCopy` → `parent.set(slot, copy)`, `ObixUtils.java:543/:558`) — identical to the servlet — so the slot's
> outgoing link fires SYNCHRONOUSLY on the writing thread (`SlotKnobs.propagate:31-46`, <1 ms). **Measured (record §7):**
> PUT `132 ms` warm; control side reflected within `<1 s` (same engine cycle). The `~6 s` dashboard lag is the READER's
> poll cadence (the poller's 5 s cycle), NOT propagation. The GET is
> `<real val="3.0" is="/obix/def/baja:StatusNumeric" display="3.00 °C {ok}" unit="obix:units/celsius"/>` — **NO
> `writable="true"`, no `<op>`**, yet the wrapped PUT still writes. So **"writable attribute absent ≠ read-only"** — a
> hand-crafted wrapped PUT bypasses the never-advertised `writable` (this CORRECTS §823.2's "no client will attempt"
> to "no CONFORMANT client will; a hand-crafted one succeeds", and refines [Block 509]'s reading that absent-`writable`
> means unwritable). The six probed forms, verbatim:
> | PUT body | Result |
> |---|---|
> | bare `<real val="2.5"/>` (±`is`/`unit`) | `<err display="Cannot translate…">` |
> | `<obj is="…:StatusNumeric"><real val="2.5"/></obj>` (child has no `name`) | `Missing attr 'name'` |
> | `<obj is="…:StatusNumeric" val="2.5"/>` (attr on obj, NO value child) | **`200 OK` but SILENTLY WROTE `0.0`** ⚠ |
> | `<real name="value" val="2.5"/>` (name on a bare simple) | `Cannot translate` |
> | **`<obj is="/obix/def/baja:StatusNumeric"><real name="value" val="2.5"/></obj>`** | **`200 OK`, `2.5 {ok}`, holds** ✅ |
> **It PROPAGATES to control:** the wrapped PUT on `Cuarto1/setpoint` (the RoomPanel façade) followed through the live
> panel→control link to `Programacion/ColdRoom_1/setpoint` — both `2.5`, Supabase `latest` `2.5`. `[CERT-live, two
> probes]` **Read-timing caveat (READER discipline, NOT a propagation lag — [Block 825]):** the control slot updates
> synchronously in <1 ms, but a POLLED reader sees the old value until its next poll. A FIRST read taken moments after
> the PUT showed `3.0` because the reader had not re-polled — a false negative; re-reading after ~1 s (control) / ~6 s
> (dashboard/DB poller) shows the propagated `2.5`. So read-back after the READER's poll cadence — the propagation
> itself is already done.
> **Link source vs target ([Block 816] §816.2):** the probe wrote the SOURCE side (`Cuarto1/setpoint`, which DRIVES
> the link) → it propagates and STICKS. A write to the link-TARGET side (`ColdRoom_1/setpoint`, flags `sL`) would be
> EPHEMERAL — overwritten on the next source propagation. Write the façade SOURCE, not the control target.
> **The silent-zero HAZARD:** the attr-only `<obj … val="2.5"/>` returns `200` but writes `0.0` (the `value` child is
> missing so the property defaults) — a write that LOOKS successful and sets the setpoint to ZERO. The body must carry
> the `value` child exactly, and the client must read the control slot back after ~1 s. Channel 1 is a VIABLE no-code path but
> UNFORGIVING; see the trade-off in §823.7.
> **Note (plain doubles ARE directly writable):** on the same component, `differentialUp/Down`, `roomHighAlarmLimit`,
> `coolOnSensorFault` carry `writable="true"` in the GET (they are plain `double`/`boolean`, not complex) — so a bare
> `<real>`/`<bool>` PUT writes them normally. Only the complex `BStatusNumeric setpoint` needs the wrapped body. `[CERT-live]`

The code path below explains WHY (a decode-level trace, `[CERT]` static). The server write path:
`BObixServer.service` POST/PUT switch → `ObixUtils.serviceWrite` (`ObixUtils.java:532-566`):
`if (tgt instanceof BIObixWritable) tgt.write(dec)` ELSE `dec.decode(tgt.asValue().newCopy(true))` (`:544`) then
`parent.set(pary[idx], val, user)` (`:558`). Three independent blockers:
- **`BStatusNumeric` is NOT `BIObixWritable`** — a corpus grep finds ZERO implementors of the interface
  (`BIObixWritable.java:9-13`, declared, never implemented) → falls to the `decode` branch. `[CERT]`
- **A bare `<real val="21.5"/>` into a complex sink throws** — `BStatusNumeric` extends `BStatusValue`→`BStruct` =
  `BComplex`, not `BSimple`; `ObixDecoder`'s `else` branch throws `IllegalStateException("Cannot translate")`
  (`ObixDecoder.java:197` in `decode`, twin at `:346` in `decode2`) BEFORE `parent.set()`. This is the exact live
  error the write-server observed 2026-09-03. `[CERT]`
- **`writable` is NEVER advertised for a `BStatusNumeric`** — `ObixUtils.encode:241-243` sets `writable="true"` ONLY
  for a `BSimple` value under `cx.canWrite()`; `BStatusValueAgent.processAttr:51-53` and `BControlPointAgent:60`
  both `return null` for "writable". So a conformant oBIX client will not even attempt the PUT. `[CERT]`
- **Escape hatch (unverified)** — a hand-crafted `<obj is="baja:StatusNumeric"><real name="value" val="21.5"/></obj>`
  enters the `name=="obj"` branch (`ObixDecoder.java:200-216`), finds the `value` property, and `setFromVal` does
  `((BStatusNumeric)cpx).setValue(BDouble.decode(sval))` (`:569`, `null` flag `:594`) → reaches `parent.set()`.
  Whether the server ACCEPTS it is then a Baja slot-flag/permission question, not an oBIX-translate one — but since
  `writable` is never advertised and `setpoint` is a plain (non-writable-point) property, this was NON-STANDARD and
  once unverified — **now CONFIRMED live** (the exact wrapped body writes + propagates, §823.2 top). `[INFER→CERT-live]`

## 823.3 — Channel 2: no existing action reaches the setpoint `[CERT]`
An oBIX/fox INVOKE of a public action IS possible (`ObixEncoder.encodeOp:477`, `serviceInvoke:494` →
`BComponent.invoke` under `OPERATOR_INVOKE`, a `BDouble` arg from `<real>`; [Block 822]). But NO such action exists:
`BRoomPanel.java`, `BDashboardService.java`, `BColdRoom.java` declare ZERO `@NiagaraAction`; `BColdRoom.java:153`
`setSetpoint(BStatusNumeric)` is a plain property setter, not an action; `BEvaporatorUnit.java:200,205,210,216` has
four actions, all `Flags.HIDDEN` timer callbacks (HIDDEN suppresses oBIX/fox exposure) and none touch a setpoint.
So there is nothing to invoke. `[CERT]`

## 823.4 — Channel 3: the DashboardPan-ux servlet — the ONE channel that works today `[CERT]`
The HMI panel already writes setpoints through the module's own servlet; the write-server can call the same endpoint.
**Cites at client `fbe9009` — the DEPLOYED DashboardPan-ux 2.1.1 servlet the write-server actually hits** (main is
`a109249`, a doc-only commit on top; PR#7 shifted these lines +52/-3 vs the earlier `deed38c` tree):
- **Endpoint:** `POST /dashboardpan/api/setpoint` (servlet name `"dashboardpan"`, `BDashboardServlet.java:81-84`;
  `handleSetpointWrite`:195, called :147). Body `{"ord":"Cuarto1/setpoint","value":4.0}` parsed in
  `handleSetpointWrite` (~:195-215).
- **Guards, in order:** (1) `X-Requested-With: XMLHttpRequest` — missing → **302** redirect home, not 4xx
  (`DashboardDispatch.java:122-126`); (2) authenticated station session `req.getRemoteUser()` — missing → **401**
  (`DashboardRbacHelper.java:36`); (3) `OPERATOR_WRITE` permission bit from `BPermissions` (by bit, not role name),
  FAIL-CLOSED — lacking it → **403** (`DashboardRbacHelper.java:17-20,55`); (4) **invalid numeric/time value
  (missing/empty/NaN/Infinity/non-numeric) → 400** `SC_BAD_REQUEST` BEFORE coercion — the PR#7 guard
  (`BDashboardServlet.java:274-283`, `JsonUtil.parseFiniteDouble(value).isPresent()`; earlier 400s at
  :216/:225/:234/:256/:265) — so a NaN/empty value returns 400, never a silent `0` write; (5) path-traversal on `ord`
  → 400. **There is NO `x-niagara-csrfToken` check** anywhere in the -ux tree (grep → 0). `[CERT]`
- **The write:** `parent.get(prop)` → `coerceValue` (`new BStatusNumeric(parseDouble(rawValue))`,
  `BDashboardServlet.java:357`) → `parent.set(prop, toSet, null)` (`:291`). Reaches the same slot as `setSetpoint`,
  via the null-Context servlet write. `[CERT]`
- **Audit:** every success appends one JSON-lines record to `BDashboardService.auditLog`
  (`svc.appendAudit(JsonUtil.buildAuditEntry(…))`, `BDashboardServlet.java:312`; ring-buffered 500,
  `BDashboardService.java:68-72,256`) — `{ts,user,action:"setpoint",ord,oldValue,newValue}`. Almost certainly **NOT**
  written to Niagara's own AuditHistory: the module has ZERO `AuditHistory`/`BAuditHistoryService` wiring (grep of
  `DashboardPan-rt/src` → 0) and the `set(...)` carries a null Context (no user attribution) → no operator-attributed
  Baja audit event. Whether a STATION-level `AuditHistoryService` fires on that slot at all is a station-config question
  not settleable from the module source (B823-G1). So the module ring is the de-facto record. `[INFER — grounded]`
- **Exact request the write-server (Node) sends:**
  ```
  POST /dashboardpan/api/setpoint HTTP/1.1
  Authorization: Basic <base64(writeUser:pass)>   (or Cookie: JSESSIONID=<niagara web login>)
  X-Requested-With: XMLHttpRequest
  Content-Type: application/json

  {"ord":"Cuarto1/setpoint","value":4.0}
  ```
  The write user must have `OPERATOR_WRITE`. This is the SAME station write user the write-server already holds for
  oBIX, over the JACE HTTP(S) connector instead of oBIX. `[CERT]`

## 823.5 — Channels 4/5/7: possible but heavyweight, or config-dependent `[CERT / GAP]`
- **Channel 4 (fox):** a `station:|slot:/…/setpoint` set is exactly what Workbench does when the operator types the
  value (the interactive fox path — real, but not automatable from the write-server). From Node there is no fox
  client; from a small Java/`nre` client it needs a full Baja runtime + a fox login on the mini-PC. The install ships
  only `nre.exe`/`station.exe`/`wb.exe` (no `ord`/`bql` CLI). Heavyweight, not a clean win. `[CERT-grounded / INFER]`
- **Channel 5 (other HTTP):** BajaScript `/ord` (`baja.Ord.make(...).set(...)`, `bajaScript-ux`) rides the box/WebSocket
  channel and needs a station web session — a Node replica is heavyweight ([Block 509]). **Haystack: NOT installed**
  (no `nhaystack` in the install → NO). **BACnet:** `bacnet-*.jar` are in the install, but a BACnet write reaches
  `setpoint` ONLY if the station bog exports that slot as a BACnet object — none verified (GAP, and unlikely for a
  dashboard config slot). `[CERT for Haystack-absent; GAP for BACnet-export]`
- **Channel 7 (schedule/ext):** only if a `BSchedule`/ext already drives the slot in the bog — none verified. GAP.

## 823.6 — Channel 6 + the live-verification plan (read-only first) `[CERT / GAP]`
**Channel 6 (link target):** GAP — no station `.bog` is available on this machine (`find … -name '*.bog'` over the
client repo → 0; only unrelated Honeywell platform defaults). The façade design classifies `setpoint` as a
direct-WRITE config slot, not a link-IN display slot (`BRoomPanel.java:39-57`), so a feeding Writable point is
unlikely — but unproven. IF a `BNumericWritable` were linked into `setpoint`, an oBIX/fox write to THAT point's
`in1`–`in8` would drive it indirectly. `[CERT for the absence of a bog; GAP for the link]`

**Live plan (read-only FIRST, any write only on a TEST room and only with Cristian's direct authorization):**
1. GET the bog / Workbench-inspect `Services/DashboardService/Cuarto*/setpoint` — is it a link target? (channel 6)
2. GET that ORD over oBIX read-only from the mini-PC — capture the exact encoded element shape (to mirror an escape-
   hatch PUT). (channel 1)
3. ONLY on a test room + authorized: try the servlet `POST` (channel 3) — expect 200 + an `auditLog` line — before
   anything else. It is the lowest-risk, already-exercised path.

## 823.7 — Kit implication `[INFER, grounded]`
- **The answer for the kit + the client:** the pragmatic NO-CODE path is **channel 3 (the module's own servlet)** —
  the write-server calls `POST /dashboardpan/api/setpoint` with the station write user + `X-Requested-With`; it is
  audited to `auditLog` today. oBIX cannot write a `BStatusNumeric` config slot by any standard client (§823.2). The
  clean minimal-CODE alternative is [Block 822]'s additive `setpointCmd`.
- **`types/dashboard.md` doctrine:** a `BStatusNumeric` (or any `BComplex`) slot is not writable by a CONFORMANT oBIX
  client (`writable` advertised only for a `BSimple` under `canWrite`) — but a hand-crafted wrapped-`obj` PUT DOES
  write it (§823.2 live). To write a complex value remotely, prefer the module's own servlet path or an additive
  simple `*Cmd` slot / OPERATOR action; use the wrapped-`obj` oBIX PUT only with the exact `value`-child body (the
  attr-only form silently writes 0.0). Cross-ref [Block 822] and the slot-type doctrine (§"Slot types for externally
  written values").
- **Recommendation (ranked — the two viable no-/low-code paths; the user picks the trade-off):**

| Rank | Path | Code change | Audit | Body forgiving? | Risk |
|---|---|---|---|---|---|
| 1a | servlet `POST /api/setpoint` (channel 3) | NONE | `auditLog` (module ring) | yes (JSON `{ord,value}`; 400 on invalid) | low — already in daily use |
| 1c | oBIX child-leaf PUT — bare `<real val="N"/>` to `…/setpoint/value` (channel 1, LIVE-CONFIRMED end-to-end, propagates ~1.5 s) `[CERT-live]` B826-G2 | NONE | none native (write-server Supabase only) | yes — simple `BSimple` decode, NO silent-zero | low-med — PREFERRED oBIX form; served + `writable="true"` (B826-G1) |
| 1b | oBIX wrapped-`obj` PUT to the parent SLOT (channel 1, proven FALLBACK, propagates to control) | NONE | none native (write-server Supabase only) | **NO — attr-only silently writes 0.0; control-slot read-back after ~1 s (dashboard lags one poll ~6 s)** | med — unforgiving body + read-timing |
| 2 | additive `setpointCmd` slot ([Block 822]) | small, schema-SAFE | write-server + a Niagara-side event via an OPERATOR action | yes | low — needs a build + schema-risk SAFE |
| 3 | fox/BajaScript client (channel 4/5) | none, but heavy infra | — | — | infra + session complexity |
| — | oBIX BARE PUT / retype `setpoint` | — | — | — | RULED OUT (bare = "Cannot translate"; retype = [B800] §800.8 OUTAGE) |
Trade-off (all of 1a/1c/1b are viable no-code paths that reach control; the user picks, not picked here): among the oBIX
forms, **1c (child-leaf bare `<real>` to `…/setpoint/value`)** is now the PREFERRED transport — a simple `BSimple` write
with NO silent-zero hazard, served + `writable="true"` and proven to propagate ~1.5 s (B826-G1/G2, records §8/§9); **1b
(wrapped-`obj` to the parent slot)** stays the proven FALLBACK but its body is unforgiving (attr-only silently zeroes) and
needs a control-slot read-back after ~1 s. Both are UNIFORM with the write-server's oBIX transport and audited by its
Supabase trail. **1a (servlet)** rides the module's HTTP servlet, is body-forgiving with the PR#7 400 validation, and
writes the module's own `auditLog`. **2 (additive `applySetpoint` action, [Block 822])** stays the cleaner LONG-TERM
answer (an oBIX `<op>` with a native, attributed Niagara invoke event). `[CERT-live]`

## 823.8 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | oBIX server write = serviceWrite; non-BIObixWritable → decode → parent.set | `[CERT]` | `ObixUtils.java:532-566,544,558` | Y — mapper+spot |
| 2 | BStatusNumeric not BIObixWritable (zero implementors); bare `<real>` → "Cannot translate" | `[CERT]` | `BIObixWritable.java:9-13`; `ObixDecoder.java:197,346` | Y |
| 3 | writable advertised only for BSimple under canWrite | `[CERT]` | `ObixUtils.java:241-243`; `BStatusValueAgent:51-53`; `BControlPointAgent:60` | Y |
| 4 | `<obj is="…:StatusNumeric"><real name="value" val/></obj>` WRITES it live (200, 2.5{ok}, 80+s) + propagates to control within ~1 s (dashboard lags ~6 s); attr-only silently writes 0.0; writable-absent≠read-only | `[CERT-live]` | `sources/probes/2026-09-06-viewer-obix-setpoint-live-record.md`; `ObixDecoder.java:200-216,569,594` | Y — live |
| 5 | No non-HIDDEN action reaches setpoint | `[CERT]` | `BRoomPanel`/`BDashboardService`/`BColdRoom` (0 actions); `BEvaporatorUnit.java:200-216` HIDDEN | Y — sweep |
| 6 | Servlet `POST /dashboardpan/api/setpoint`, guards XHR(302)/auth(401)/OPERATOR_WRITE(403)/invalid-num(400), no CSRF | `[CERT]` | `fbe9009` `BDashboardServlet.java:81-84,195,274-283`; `DashboardDispatch.java:122-126`; `DashboardRbacHelper.java:17-20,36,55` | Y |
| 7 | Write = coerce→`parent.set(prop,new BStatusNumeric(v),null)`; audited to auditLog, not AuditHistory | `[CERT]` | `fbe9009` `BDashboardServlet.java:291,357,312`; `BDashboardService.java:68-72,256` | Y |
| 8 | Haystack absent; BACnet in install; no bog to confirm link/export | `[CERT/GAP]` | install `modules/`; `find … *.bog` → 0 | Y |

**Tally:** `[CERT]` ×7 · `[INFER]` ×1 (the escape hatch). Two GAPs (channel 6 link, BACnet export) need a live bog
read — named in §823.6. Nothing invented; every load-bearing cite spot-verified this session.

## 823.9 — Connections & open gaps
- REMITTANCE: [Block 822] (additive `setpointCmd`/`applySetpoint` + schema-risk — the code-change half; cross-cited),
  [Block 816] (the servlet write path / threading), [Block 813]/[Block 763] (DWS1 servlet gates), [Block 800] §800.8
  (the retype OUTAGE that rules out re-typing), [Block 509] (oBIX/box transport).
- **B823-G1** — the oBIX GET-encoding + escape-hatch half is **CLOSED live** (§823.2, 2026-09-06): the wrapped PUT
  writes; the child-leaf bare-`<real>` PUT is now the preferred form (§823.7 row 1c, B826-G1/G2). The channel-6
  link-target question is **CLOSED by a real bog read** `[CERT]` (`tools/bog-nav.py` on PANCCADIA `config.bog`,
  2026-09-05): `Services/DashboardService/Cuarto1/setpoint` is a link **SOURCE** — it feeds
  `Programacion/ColdRoom_1.setpoint` (`sourceSlotName=setpoint → targetSlotName=setpoint`) — and is **NOT** a link
  target itself (its `fed-by` set holds zoneTemp/evapTemp/coolingSince, never `setpoint`). So an external write to
  `Cuarto1/setpoint` STICKS and propagates DOWN into the logic; it is not overwritten by an inbound link ([Block 816]
  §816.2). The RoomPanel facade is the write point; the ColdRoom is downstream.
- **B823-G2** (requires-execution, authorized write on a TEST room only): confirm the servlet `POST` lands 200 + one
  `auditLog` line — the channel-3 proof — and, if pursued, the escape-hatch `<obj>` PUT verdict.
