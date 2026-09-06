# B821 · Protection anatomy of our RT modules — every protection trip as (WHAT fires · WHO fires · WHO watches), one-bit-traced at `fbe9009`; the RT control logic raises ZERO alarm-console events (every surface is a plain SUMMARY slot, never a `BAlarmRecord`); + the two author-built classes Tridium ships NO primitive for (set-dominant SR latch, independent heartbeat) `[CERT]`

> **Scope**: the user's RT deep-dive question — "protections: what fires them, who fires them, who watches; heartbeat; latch" — generalized into a first-principles **protection anatomy** and audited against our three client RT modules. AUDIT-FIRST: enumerates every protection/interlock/cutout/anti-cycle/fault-hold in ColdRoomPan-rt / CompPan-rt / DashboardPan and classifies each on three axes (fires · actor+thread · surface), then names the cross-cutting gap (no alarm-console path) and the two protection primitives Tridium does not ship. NOT a re-derivation of the mechanisms: REMITTANCE to [B805] (RT control + one-bit trace), [B808] (who-watches feedback surfaces), [B810] (actuator safe-state/fallback), [B812] (heartbeat/liveness), [B776] (action protection flags), [B819] (demand gate + NaN-never-demand).
>
> **Tree discipline** (the [B815] §815.10 lesson, applied to myself): the first enumeration pass read the client working tree at `81f542e` (v2.0.0); `origin/main` is **`fbe9009`** — the 8 merged PRs (ColdRoomPan v2.0.4–2.0.7, CompPan v2.0.3, DashboardPan v2.1.1), **1303 insertions** across the six protection files. All cites below are re-anchored at `fbe9009` and confirmed at the enclosing method; three PR-added surfaces the v2.0.0 read would have mis-reported as gaps (CR-1/CR-5/CR-9) are marked CLOSED.
>
> **Sources**: FUENTE 1 (own modules, `[CERT]` at `fbe9009`) — `ColdRoomPan-rt/{BEvaporatorUnit,BColdRoom,BDefrostController,ColdRoomControl}.java`, `CompPan-rt/{BCompressorControl,CompressorControl}.java`, `DashboardPan-ux/{BDashboardServlet,DashboardReader}.java`, `BRoomPanel.java`. REMITTANCE — [B805]/[B808]/[B810]/[B812]/[B776]/[B819]. PoCs (`[CERT]`, tests GREEN) — protection-latch seam **e31bd60a1**, heartbeat seam **fc9caa1ff**, HOA-precedence seam **5a9020fd6**.

---

## 821.1 — The three-axis anatomy `[INFER, framework grounded in 821.2-4]`
Every protection answers three separable questions. Confusing them is where control bugs hide:

1. **WHAT fires it** — the *trip source*: a boolean/numeric condition on a process variable, a mode, or a timer. Seven classes recur (§821.2): process-limit cutout · sensor-fault/NaN fault-hold · safety interlock · HOA-OFF lockout · anti-cycle timer · demand gate · proof-of-run/liveness.
2. **WHO fires it** — the *actor + thread*: the method that evaluates and acts, and whether it runs on the deterministic engine thread or an async `Clock.Ticket` callback, or is an operator/framework action (§821.3). A protection evaluated on two threads without per-slot serialization is where [B816] overlap lives.
3. **WHO watches it** — the *surface*: a fault-status slot, an alarm record, a dashboard tile, or nothing (§821.4). A protection that fires with no surface is a *silent runaway* — it defends the process but no one can see why.

A complete protection declares all three. Our modules are strong on (1)/(2) and weak on (3): **many trips are correct but silent**, and **not one raises an alarm-console event** (§821.4).

## 821.2 — WHAT FIRES: the trip-source taxonomy (our 22 protections at `fbe9009`) `[CERT]`
| ID | Protection | WHAT fires (condition · file:line) | Class |
|---|---|---|---|
| CR-1 | Zone-sensor fault-hold | `!sensorValid` → `coolOnFault?true:prev` `ColdRoomControl.java:39` (guard `BColdRoom.java:506`) | sensor-fault hold |
| CR-2 | Invalid-setpoint skip | `!spVal.getStatus().isValid()` → skip cycle `BColdRoom.java:443` | sensor-fault hold |
| CR-3 | Freeze-stat valve trip | `coil ≤ sp - diffStop` `ColdRoomControl.java:134` (band-latched :136; guard :133) | process-limit cutout |
| CR-4 | Resistance-HAND valve/fan interlock | `resistanceMode == HOA_HAND` → `valveInhibited`/`fanInhibited` true `BEvaporatorUnit.java:1105,1116` | safety interlock |
| CR-5 | HOA-OFF lockout (valve/fan/resist) | `mode == HOA_OFF` → false; `resistanceCommand` checks OFF **before** `inDefrost` `ColdRoomControl.java:75,101` | HOA-OFF lockout |
| CR-6 | HAND blocked by inhibit | `inhibit` overrides HAND `ColdRoomControl.java:74` | safety interlock |
| CR-7 | Defrost fan/valve HOA bypass | `inDefrost` → HOA not applied `BEvaporatorUnit.java:1232` | interlock (sequence-owns) |
| CR-8 | Defrost one-token serialization | one `defrostingUnit`; others FIFO `waitingQueue` `BDefrostController.java:762-770,947` | interlock |
| CR-9 | hasDefrost / lockout eligibility | `hasDefrost && !resistanceLockedOut` `ColdRoomControl.java:187` | interlock |
| CR-10 | Invalid-runCmd gate | `!getRunCmd().getStatus().isValid()` → hold `BEvaporatorUnit.java:924` | sensor-fault hold |
| CR-11 | Stale-ticket guards | Clock callbacks re-check validity/`inDefrost` `BEvaporatorUnit.java:984,994,1159` | liveness (self-guard) |
| CP-1 | Low-suction cutout | `suction < suctionLowLimit` (guarded `>0`, valid) → shed stage `CompressorControl.java:215`; sheds a running HAND unit :300-305 | process-limit cutout |
| CP-2 | High-discharge cutout | `discharge > dischargeHighLimit` → block stage-up `CompressorControl.java:140,213`; blocks HAND start :265 | process-limit cutout |
| CP-3 | Zero-demand gate | `demandCount <= 0` → target 0 `CompressorControl.java:201` (FASE1 `target=demandCount` :207) | demand gate ([B819]) |
| CP-4 | minOff anti-cycle | `(now-cmdSince[k]) < minOffMs` → skip start `CompressorControl.java:358`; OFF edge-stamp :260-262 | anti-cycle timer |
| CP-5 | minOn anti-cycle | `(now-cmdSince[k]) < minOnMs` → skip stop `CompressorControl.java:369` | anti-cycle timer |
| CP-6 | stageDelay | `(now-lastStageMs) < stageDelayMs` → no stage `CompressorControl.java:221`; proof timers reset per stage :232-233 | anti-cycle timer |
| CP-7 | Proof-of-run fault | `measured && cmd && !drawing && elapsed ≥ startProveMs` `CompressorControl.java:166` (LIVE, self-clears) | proof-of-run |
| CP-8 | Stuck-contactor | `measured && !cmd && drawing` `CompressorControl.java:161` | proof-of-run (inverse) |
| CP-9 | Suction-sensor mismatch | `v1 && v2 && |p1-p2| > tol` `BCompressorControl.java:2003` | sensor-integrity |
| CP-10 | Rack-unavailable | `available == 0` (all OFF) `CompressorControl.java:173` | demand/availability |
| CP-11 | Pressure-fallback | `!(suctionValid && suctionBand>0)` `CompressorControl.java:206` | mode indicator |

**The NaN discipline holds ([B819]):** every process-variable trip is guarded by `isValid()`/`!Double.isNaN` before it evaluates (CR-1/2/3/10, CP-1/2/3/7/8), and an invalid input **holds** rather than trips (equipment fail-safe) — a bad sensor never fabricates or suppresses a protection. This is the one axis our modules do rigorously.

## 821.3 — WHO FIRES: actor + thread `[CERT]`
- **Engine thread, synchronous** (`changed(prop)` / `execute()` / `atSteadyState()`): CR-1..CR-7, CR-10, and every CompPan trip when an input slot changes (`BCompressorControl.changed()` `:1825`). Deterministic; the servlet write and the engine `set()` serialize per-slot ([B816] §816.1), so the trip sees old-or-new, never torn.
- **Async `Clock.Ticket` expiry**: the defrost sequence (CR-7/CR-8 via `doIntervalExpired`/`doStaggerExpired`/`doDurationExpired`), the evaporator delays (CR-11), and the CompPan 5 s `doTick()` re-evaluation of ALL CompPan trips (`BCompressorControl.java` tick). **Consequence**: a CompPan protection fires on BOTH the engine thread (on change) AND the Clock thread (every 5 s) — the two must be idempotent on the shared `cmd[]`/`cmdSince[]` state; they are, because `step()` is a pure recompute over transient arrays, not an incremental mutation.
- **Operator action / HOA**: `forceDefrost` (`@NiagaraAction OPERATOR` `BDefrostController.java:153`), `faultReset` (`CP-7`), and the HOA mode writes (CR-4/5/6) — engine thread via the action/slot-write path, [B776] OPERATOR-gated.
- **Framework lifecycle**: HOA mode slots are `Flags.TRANSIENT` → the framework reverts them to AUTO on restart (an *implicit* safety: no output is left forced unattended). Silent (§821.6).

## 821.4 — WHO WATCHES: the surface taxonomy + the cross-cutting gap `[CERT]`
Four surface tiers, from strongest to none:
1. **Alarm-console event** (`BAlarmSourceExt` on the point, or a raised `BAlarmRecord`) — reaches the operator's alarm queue, ack/unack, history. **NONE of our RT protections use this.** A clean grep of `ColdRoomPan-rt/src` + `CompPan-rt/src` for `BAlarmSourceExt|BAlarmRecord|BAlarmService` returns **ZERO** (`fbe9009`). DashboardPan-ux *reads* the station alarm DB (`BDashboardServlet.java:511` `(BAlarmRecord)cursor.get()`) to DISPLAY existing alarms — but our modules never POPULATE it. **So no protection trip in our modules ever reaches the alarm console** — the dashboard's alarm table shows only alarms wired by someone else (e.g., a point extension), never a freeze trip, a stuck contactor, or a failed compressor start.
2. **Named status slot** (a `SUMMARY | READONLY` boolean the operator/dashboard can read): `dischargeHighAlarm` (CP-2 `:1533`), `stuckAlarm` (CP-8 `:1510`), `condenserNFault` (CP-7 `:1303`), `suctionMismatch` (CP-9), `rackAvailable` (CP-10), `pressureFallback` (CP-11), `defrostSkipped`+`lastSkipReason` (CR-9 `BDefrostController.java:429,452`). Visible in Workbench; linkable to the SPA via `DashboardReader`. This is the ceiling our modules reach.
3. **Inferred from an output/effect slot** (no named reason, but the *effect* is visible): CR-3/CR-4/CR-5/CR-6/CR-7 — the valve/fan/resist state LED (`evapNValveState`/`FanState` via `DashboardReader.STATE_SLOTS`) goes off; the operator sees the actuator off but must *infer* the cause.
4. **SILENT** — no slot, no alarm, no LED reason (§821.6).

**The cross-cutting finding**: our protection surfacing tops out at tier 2 (a plain SUMMARY slot). Tier 1 (the alarm console) is entirely unused — a design gap, because tier-2 slots are only seen by someone actively looking at Workbench or the SPA, whereas a `BAlarmRecord` pushes to the operator. This is the single highest-value protection improvement across all three modules and the concrete form of [B808]'s "a LOGIC fault must reach the operator."

## 821.5 — The two protection primitives Tridium ships NO stock class for `[CERT — PoCs]`
The user's "latch" and "heartbeat" are exactly the two protections the framework leaves to the author ([B805] §805.3 established kitControl ships NO SR latch; [B812] no independent liveness monitor). Both built + mutation-proven this campaign:
- **Set-dominant SR protection latch** — `ProtectionLatch.java` (PoC **e31bd60a1**, 8 tests GREEN). Contract: SET dominates, first-out captured once on the CLEAR→TRIPPED edge, explicit operator reset only while the trip condition is clear, no re-trip chatter. Mutation: dropping the first-out guard fails `firstOutIsNotOverwritten`. Baja wrapper: `step()` from `execute()`/`changed()`, a reset ACTION ([B803] step-up-gateable), first-out → a **`BAlarmSourceExt`** (the tier-1 surface §821.4 lacks). **This is the missing "who watches" for a latched safety** — none of our current trips latch a first-out cause.
- **Independent heartbeat/liveness monitor** — `LivenessDecision.step()` (PoC **fc9caa1ff**, 8 tests GREEN). Contract: STALLED when `age > factor×period` (strict, never at the boundary), RECOVERED is an edge, hysteresis holds through the band, a never-ticked producer ages into a stall, a backward clock jump is treated as a fresh tick (fail-safe alive — [B775] §775.6 / [B801]). Mutation: `age > threshold` → `>=` fails `boundaryIsNotStalled`. Baja wrapper: a `lastTick` TRANSIENT slot the producer stamps, a monitor on `schedulePeriodically` (period floored ≥1 s per [B801]) that raises a `BAlarmRecord` on STALLED and clears on RECOVERED. **This is the "who watches the watcher"** — our modules have CR-11 self-guards but no independent monitor that a whole control loop has stalled.

## 821.6 — The SILENT protections at `fbe9009` (the honest gap list) `[CERT]`
Post-PR (the v2.0.0 gaps CR-1 cooling-state, CR-5 OFF-in-defrost, CR-9 defrost-skip are **CLOSED** by PR#7/#4/#5), these remain with no operator-visible surface:

| Protection | Where the bit dies | Operator experience |
|---|---|---|
| CR-3 freeze-stat trip | `freezeTripped` is a **private field** `BEvaporatorUnit.java:1287`, no slot | valve closed while the room calls — cause unnamed |
| CR-10 invalid-runCmd | `applyRunCmd()` returns, no slot `:924` | actuator stops tracking demand, silently |
| CR-11 stale-ticket | Clock callback returns early, no slot | output simply doesn't change |
| CP-1 low-suction cutout | no named "LP trip" slot (contrast `dischargeHighAlarm` exists) | stages shed with no reason indicator |
| CP-4 minOff | `cmdSince[]` private array, no slot | compressor dark while demand present |
| CP-5 minOn | `cmdSince[]` private array, no slot | compressor runs past demand |
| CP-6 stageDelay | `lastStageMs` private long, no slot | staging lags demand up to `stageDelayMs` |
| (framework) HOA TRANSIENT restart-revert | framework clears the override, no notice | HAND silently lost on restart |

The asymmetry is the tell: CP-2 high-discharge has `dischargeHighAlarm` but CP-1 low-suction has **no** symmetric `suctionLowAlarm` — a low-suction (vacuum) trip, the more damaging of the pair, is the quieter one.

## 821.7 — One-bit end-to-end traces (the Excavador directive) `[CERT]`
Tracing a single bit from sensor to operator exposes exactly where a surface is missing:
- **Freeze-stat (visible EFFECT, SILENT reason)**: `coilTemp` sensor bit → `recomputeFreeze()` `:1092` → `freezeTrip(coil ≤ sp-diffStop)` `ColdRoomControl.java:134` → `freezeTripped=true` (**private, :1287 — the bit stops here for the operator**) → `valveInhibited()` true `:1106` → `setBool(valveOut)` → `applyHoa(inhibit=true)` → false → `valveOut=false` → BLink → `BRoomPanel.evap1ValveState` → `DashboardReader.STATE_SLOTS` → JSON `st` → SPA LED off. *The operator sees the valve off; the word "freeze" never leaves the module.*
- **HOA-OFF lockout (fully traceable, PR#4)**: operator `resistanceMode=OFF` → `changed(resistanceMode)` → `resistanceCommand(mode=OFF)` returns false at `ColdRoomControl.java:101` **before** the `inDefrost` check at `:102` → `resistanceOut=false` even mid-defrost, and `terminateCurrent()` ends a now-heatless cycle `BEvaporatorUnit.java:1209` → LED off. Surface = the operator's own OFF plus the state LED. OFF > defrost > HAND > AUTO, matching the HOA-precedence seam (5a9020fd6) and [B805] §805.11.
- **minOff (FULLY SILENT)**: amps drop → `cmd[k]` true→false edge → `cmdSince[k]=now` `:260` (**private array — bit stops here**) → next `step()` `pickLeastHoursOff` skips k while `(now-cmdSince[k]) < minOffMs` `:358` → compressor stays off despite demand → no slot, no LED reason. *The operator sees a dark compressor with the rack calling and has nothing to read.*

## 821.8 — Kit implication `[INFER, grounded]`
- **`types/logic.md` §"Protection anatomy" doctrine**: every protection declares all three axes (fires · actor+thread · surface); the NaN-guard-then-hold rule ([B819]); the surface tiers (§821.4) with the target = tier 1 (alarm) for any *safety* trip, tier 2 (named slot) minimum for any trip that changes an actuator. `[ev: corpus B821]`
- **A protection checklist** for `verify-module.sh`/review: for each trip — is the process variable `isValid()`-guarded? does it hold (not trip) on invalid? does it set a **named** status slot (not only a private field)? does a *safety* trip raise a `BAlarmSourceExt`/`BAlarmRecord`? is it idempotent across the engine and Clock threads?
- **Lint candidate (statically decidable → WARN)**: a method that drives an output slot false on a limit/interlock condition but writes **no** status slot and raises no alarm in the same method scope → WARN "silent-protection" (the §821.6 shape). Advisory boundary inherits [B819-G1]/[B820]: whether a present slot is really the *reason* is human-review. Extends the [B820] demand-in-scope family.
- **Feeds**: S13 (health surface = the tier-1/tier-2 fix: fault-status slot + `BAlarmSourceExt` + `lastTick`), S2 (protection-latch seam = the latched first-out surface §821.5), S3 (heartbeat seam = the independent monitor §821.5).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 22 protections enumerated across ColdRoomPan-rt/CompPan-rt at `fbe9009`; each classified fires/actor/watch | `[CERT]` | §821.2 table, file:line at fbe9009 (re-anchored from stale 81f542e) |
| 2 | The RT control modules raise ZERO alarm-console events — no `BAlarmSourceExt`/`BAlarmRecord` in ColdRoomPan-rt/CompPan-rt src (clean grep); DashboardPan only READS the alarm DB | `[CERT — verified absent]` | grep of the two -rt src trees = empty; `BDashboardServlet.java:13,511` reads `BAlarmRecord` |
| 3 | HOA-OFF dominates defrost: `resistanceCommand` checks `HOA_OFF` (→false) BEFORE `inDefrost` (→true) | `[CERT]` | `ColdRoomControl.java:101-102` (PR#4, v2.0.6); matches seam 5a9020fd6 + [B805] §805.11 |
| 4 | Every process-variable trip is `isValid()`/`!Double.isNaN`-guarded and HOLDS (not trips) on invalid | `[CERT]` | CR-1/2/3/10, CP-1/2/3/7 guards; [B819] NaN-never-demand |
| 5 | Genuine SILENT trips at `fbe9009`: CR-3/CR-10/CR-11, CP-1/CP-4/CP-5/CP-6 (freezeTripped/cmdSince/lastStageMs are private fields, no slot) | `[CERT]` | `BEvaporatorUnit.java:1287`; `CompressorControl.java:260,358,369,221` |
| 6 | The two author-built primitives (SR latch, heartbeat) are built + mutation-proven; Tridium ships neither | `[CERT — PoC]` | e31bd60a1 (8 GREEN), fc9caa1ff (8 GREEN); [B805] §805.3, [B812] |
| 7 | CompPan trips fire on BOTH the engine thread (changed) and the 5 s Clock tick; safe because `step()` is a pure recompute | `[CERT]` | `BCompressorControl.java:1825` changed + `doTick`; `CompressorControl.step()` over transient arrays |

**Tally**: 6 `[CERT]` (1 verified-absent) · 1 `[CERT-PoC]`. §821.1 anatomy + §821.8 kit are `[INFER]` grounded in the [CERT] audit. Tree stated: `fbe9009` (re-anchored from a stale `81f542e` first pass — the [B815] §815.10 discipline applied to my own work). Dedupe: the control/protection MECHANISMS are REMITTANCE ([B805]/[B808]/[B810]/[B812]/[B776]/[B819]); this block adds the three-axis ANATOMY, the [CERT] 22-protection audit, the no-alarm-console cross-cutting finding, the honest silent list, and the one-bit traces.

## Connections
- **[B805]** (RT control exemplars + one-bit trace — the mechanism this anatomizes; §805.11 HOA precedence = CR-5), **[B808]** (who-watches feedback — §821.4 tier-1 gap is its "fault must reach the operator" made concrete), **[B810]** (actuator safe-state/fallback — the idle output a trip drives), **[B812]** (heartbeat = §821.5 second primitive), **[B776]** (OPERATOR action flags — the HOA/reset actors), **[B819]** (demand gate CP-3 + NaN-never-demand = the guard discipline), **[B820]** (demand-in-scope lint = the sibling of the §821.8 silent-protection lint), **[B816]** (engine/Clock thread overlap — §821.3). Kit: `types/logic.md` §"Protection anatomy" + the checklist + the silent-protection lint; feeds C9 seeds S13/S2/S3.

## Open gaps
- **B821-G1** (bounded): the silent-protection lint's decidable shape — distinguishing a trip that legitimately relies on an *effect* slot (tier 3, e.g. the valve LED) from one that is truly silent (tier 4) needs the same data-flow rule as [B819-G1]/[B820]; starts advisory (WARN).
- **B821-G2** (requires-execution): confirm on a live station that a raised `BAlarmSourceExt` from a protection reaches the REFLOW/PANCCADIA operator alarm console (the tier-1 fix) — pairs with S13 and the [B808] surfacing chain.
