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
| 1 | oBIX PUT of `setpoint` | **NO** (escape hatch unverified) | JACE never advertises it writable; bare-value PUT rejected |
| 2 | oBIX/fox INVOKE of an existing action → `setSetpoint` | **NO** | no non-HIDDEN action reaches the setpoint |
| 3 | **DashboardPan-ux servlet `POST /dashboardpan/api/setpoint`** | **YES** | the HMI writes setpoints through it TODAY |
| 4 | fox / `station:\|slot:` set (Node or Java/nre client) | heavyweight, not clean | needs a full Baja fox client + login |
| 5 | other HTTP (BajaScript `/ord`, Haystack, BACnet/Modbus) | NO / config-only | Haystack absent; BACnet only if the bog exports it |
| 6 | write an upstream Writable point linked into `setpoint` | GAP | no bog available; design is direct-write, not link-in |
| 7 | schedule/ext-driven | GAP | only if already in the bog; none verified |

## 823.2 — Channel 1: oBIX PUT is effectively CLOSED `[CERT]` (mapper-forwarded, spot-verified)
The server write path: `BObixServer.service` POST/PUT switch → `ObixUtils.serviceWrite` (`ObixUtils.java:532-566`):
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
  `writable` is never advertised and `setpoint` is a plain (non-writable-point) property, this is NON-STANDARD and
  UNVERIFIED; only a controlled live PUT on a test room settles it. `[INFER, decode-grounded]`

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
- **`types/dashboard.md` doctrine:** a `BStatusNumeric` (or any `BComplex`) slot is NOT oBIX-writable — oBIX advertises
  `writable` only for a `BSimple` under `canWrite`; to write a complex value remotely, use the module's own
  servlet write path, an additive simple `*Cmd` slot, or a proper writable-point. Cross-ref [Block 822].
- **Recommendation (ranked):**

| Rank | Path | Code change | Audit | Risk |
|---|---|---|---|---|
| 1 | servlet `POST /api/setpoint` (channel 3) | NONE | `auditLog` (module ring) | low — already in daily use |
| 2 | additive `setpointCmd` slot ([Block 822]) | small, schema-SAFE | via write-server + AuditHistory | low — needs a build + schema-risk SAFE |
| 3 | oBIX `<obj>` escape-hatch PUT (channel 1) | NONE | none native | UNVERIFIED — live test only |
| 4 | fox/BajaScript client (channel 4/5) | none, but heavy infra | — | infra + session complexity |
| — | oBIX bare PUT / retype | — | — | RULED OUT (§823.2 / [B800] §800.8 OUTAGE) |

## 823.8 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | oBIX server write = serviceWrite; non-BIObixWritable → decode → parent.set | `[CERT]` | `ObixUtils.java:532-566,544,558` | Y — mapper+spot |
| 2 | BStatusNumeric not BIObixWritable (zero implementors); bare `<real>` → "Cannot translate" | `[CERT]` | `BIObixWritable.java:9-13`; `ObixDecoder.java:197,346` | Y |
| 3 | writable advertised only for BSimple under canWrite | `[CERT]` | `ObixUtils.java:241-243`; `BStatusValueAgent:51-53`; `BControlPointAgent:60` | Y |
| 4 | `<obj><real name="value">` reaches setFromVal→setValue (escape hatch, unverified server-accept) | `[INFER]` | `ObixDecoder.java:200-216,569,594` | decode-grounded |
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
- **B823-G1** (requires-execution, read-only): GET the PANCCADIA bog / Workbench-inspect whether `Cuarto*/setpoint` is
  a link target (channel 6) and capture the oBIX GET encoding of a `BStatusNumeric` (to settle the §823.2 escape hatch).
- **B823-G2** (requires-execution, authorized write on a TEST room only): confirm the servlet `POST` lands 200 + one
  `auditLog` line — the channel-3 proof — and, if pursued, the escape-hatch `<obj>` PUT verdict.
