# B731 · ColdRoomPan / CompPan / DashboardPan — rt hardening & feature backlog, audited against the corpus

> **Scope**: an APPLICATION audit — not new framework research. It takes the mature Niagara rt corpus
> (lifecycle, idioms, alarms, writables, history) and asks, for the PANCCADIA León custom modules, "what do
> we do, what is the Tridium way, what should we consider." Every framework claim is a citation to an
> existing block; the NEW content is the per-module verdict + severity. Focus: `own-modules-audit`.
>
> **Sources**: our source tree `~/modulos_niagara_n4/Cliente/Leon-Guanjuato/{Paccadia/ColdRoomPan,Compresores/CompPan,Dashboard/DashboardPan}` (read this session); corpus B729 (timer lifecycle), B730 (rt idioms), B4 (slot system), B44/B244 (alarm pipeline/OEM ext), B104/B137 (writable/priority-array + I/O points), B278/B355 (history/trend), plus the ColdRoomPan hardening bitácoras and B729 §729.6 audit.

---

## 731.1 — Lifecycle (vs B729) `[CERT]`
- **Finding**: `BDefrostController` armed its interval only in `atSteadyState()` → dead on late-mount
  (commissioning). `BCompressorControl` arms its control `tick` only in `atSteadyState():1659` (changed()
  re-runs execute, NOT armTick) → late-mount freezes lead/lag hour-rotation + start-prove. `BEvaporatorUnit`
  only loses the soft-start stagger (LOW).
- **Tridium way**: arm in `started()` (guarded `Sys.atSteadyState()`) + `atSteadyState()` + optional
  `clockChanged()`; anti-pattern "atSteadyState-only" has 0 hits in Tridium (B729 §729.7).
- **Status**: fix (started+clockChanged) compiled for `BDefrostController` (with nextDefrostTime +
  applyRunCmd guard), pending deploy + cold-boot test. **`BCompressorControl` still needs the same
  `started()`** — MED, not yet in a build.
- **Verdict**: DefrostController = fixed pending verify; **CompPan = open (MED)**; EvaporatorUnit = LOW.

## 731.2 — Idioms & flags (vs B730) `[CERT]`
- **isValid()/getStatus() degradation**: PRESENT in all four B* components (10-12 hits) → honest degradation
  on bad probe. ✓ consistent with B730 §730.3.
- **catch(Throwable)+log on engine thread**: PRESENT (all handlers wrap → logError). ✓ B730 §730.10.
- **`DEFAULT_ON_CLONE`**: **0 hits** anywhere. HOA mode slots (`valveMode/fanMode/resistanceMode`,
  `SUMMARY|OPERATOR|TRANSIENT`) reset to Auto on RESTART but NOT on CLONE (Baja copies transient on
  newCopy; DEFAULT_ON_CLONE/REMOVE_ON_CLONE are the clone-scoped flags — B4 flag table). **Risk (MED,
  commissioning): cloning an evaporator/room that is in HAND/OFF carries the override into the clone until a
  restart.** Fix: `Flags.DEFAULT_ON_CLONE` on the HOA mode slots (Tridium uses it for calc-state,
  BOptimizedStartStop:112,466 — B730 §730.5).
- **commit-on-change (`out.equivalent`)**: 0 hits — we `setValue` unconditionally. LOW value here (outputs
  change infrequently); optional.
- **Verdict**: degradation + engine-safety GOOD; **DEFAULT_ON_CLONE on HOA modes = MED hardening**;
  commit-on-change = optional.

## 731.3 — Alarms (vs B44/B244; alarm-rt `BAlarmSourceExt`) `[CERT]`
- **Finding**: our control components have **NO alarm source** — no `BAlarmSourceExt`, no offnormal/fault
  extension (grep = 0). The alarm-limit slots (`roomHighAlarmLimit`, `evapHighAlarmLimit`,
  `evapLowAlarmLimit` doubles; `dischargeHighAlarm`/`stuckAlarm`/etc. status booleans) are **config values /
  live flags the control does not turn into `BAlarmRecord`s**. `DashboardPan`'s servlet READS the station
  alarm space via BQL (`sourceState='offnormal' or 'fault'`, `BDashboardServlet.java:485`) — so it shows
  alarms that OTHER sources (e.g. BACnet points) produce, but **our rooms/evaporators/compressors do not
  enter the Alarm Console** (not ack-able, not logged, not escalated).
- **Design note (correct)**: alarms must NOTIFY, never STOP control — the control path deliberately does not
  read these limits (ColdRoomPan hardening §4C, B729 links). That is right.
- **Tridium way**: to make a limit an actual console alarm, attach a proper alarm extension (alarm-rt:
  `BAlarmSourceExt` firing `toOffnormal/toFault/toNormal` `BAlarmRecord`s; or the OEM `honAlarmExt`
  delay/suppression layer, B244) — either on a proxy/control point or via a dedicated source component.
- **Verdict**: **FEATURE DECISION, not a bug.** If the operator expects room/coil/discharge highs to appear
  in the Alarm Console (ack/log/escalate), we must model real alarm sources. Today they are dashboard-only
  live indicators. Ties into the oBIX/TC500 orphan-alarm hygiene (proper alarm modeling + cleanup of stale
  source ORDs).

## 731.4 — Overrides: our HOA double vs writable priority arrays (vs B104/B137) `[CERT]`
- **Finding**: HOA is a per-output `TRANSIENT` `double` 0/1/2 (auto/hand/off), priority structural
  (defrost > HOA > auto), re-applied on mode change (bitácora 5cuartos §5).
- **Tridium way**: the canonical override is a **writable point with a priority array** (`BNumericWritable`/
  `BBooleanWritable`, `in1..in16`, emergency/manual/auto levels — B104/B137).
- **Trade-off (already reasoned)**: we chose a plain `double` on purpose — a shared frozen-enum link across
  two custom modules forces a cross-module `-rt` dependency (non-trivial DSL) and a leftover enum ref once
  surfaced live as `Missing class ColdRoomPan:HoaMode` (types/logic.md, bitácora 5cuartos §5). For a single
  HOA source per output, the double is adequate and dependency-free; we forgo multi-source priority
  arbitration and emergency levels, which we do not need.
- **Verdict**: ADEQUATE by design; only fix = §731.2 DEFAULT_ON_CLONE. Revisit only if we ever need multiple
  competing override sources per output.

## 731.5 — History / trend (vs B278/B355) `[CERT]`
- **Finding**: **no `BHistoryExt`/trend anywhere** (grep = 0). Room/coil/resistance temps are live-only; not
  logged for trending/analysis.
- **Tridium way**: attach a `BHistoryExt` (interval or COV) to the value to log to the history database
  (B278/B355).
- **Verdict**: **FEATURE DECISION.** If the operator wants temperature/defrost trends (very common for
  refrigeration diagnostics — verifying defrost effectiveness, pull-down curves), add history extensions on
  the key numerics. Not present today.

## 731.6 — Prioritized backlog

| # | Item | Severity | Type | Status |
|---|---|---|---|---|
| 1 | `started()`+`clockChanged()` on `BDefrostController` | HIGH | bug | compiled, pending deploy+cold-boot |
| 2 | `started()` on `BCompressorControl` (tick: lead/lag + start-prove) | MED | bug | open |
| 3 | `DEFAULT_ON_CLONE` on HOA mode slots (clone inherits HAND/OFF) | MED | hardening | proposed (2nd build) |
| 4 | anti-anchor-stale guard in `armTrigger` (`abs(elapsed)>3·interval`) | LOW | hardening | proposed (B729-G2) |
| 5 | Real alarm sources for room/coil/discharge limits | — | FEATURE (operator decision) | not started |
| 6 | History extensions for temps (trending) | — | FEATURE (operator decision) | not started |
| 7 | commit-on-change (`equivalent`) before setValue | LOW | polish | optional |

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Our control components have NO alarm source ext; alarm-limit slots don't produce BAlarmRecords | [CERT] | grep BAlarmSourceExt/offnormal = 0 in rt src; slots roomHighAlarmLimit/evapHighAlarmLimit/evapLowAlarmLimit/dischargeHighAlarm |
| 2 | DashboardPan reads alarms via BQL but doesn't create them | [CERT] | BDashboardServlet.java:485 (sourceState offnormal/fault) |
| 3 | No BHistoryExt/trend anywhere in our modules | [CERT] | grep BHistoryExt/trend = 0 |
| 4 | DEFAULT_ON_CLONE absent; HOA modes TRANSIENT (reset on restart, copied on clone) | [CERT] | grep=0; BEvaporatorUnit HOA slot flags SUMMARY\|OPERATOR\|TRANSIENT |
| 5 | isValid()/getStatus() + catch(Throwable) present across the 4 components | [CERT] | grep 10-12 hits; handlers wrap→logError |
| 6 | HOA-as-double was a deliberate choice to avoid cross-module enum dependency | [CERT] | types/logic.md; bitácora 5cuartos §5 (Missing class ColdRoomPan:HoaMode) |

**Tally**: 6 [CERT]. No unmarked claims. FEATURE items (alarms, history) are operator decisions, flagged as
such, not defects.

## Connections
- **B729** (timer lifecycle) · **B730** (rt idioms) — the rules this audit applies.
- **B4** (slot flags), **B44/B244** (alarm pipeline / OEM alarm ext), **B104/B137** (writable/priority-array),
  **B278/B355** (history/trend) — the Tridium-way references.
- **own-modules-audit focus** (B637-639) — sibling audits of our modules.
- Kit `build-n4-module-kit/types/logic.md` (applied rt checklist).

## Open gaps
- **B731-G1**: whether the operator WANTS console alarms (§731.3) and trends (§731.5) — a requirements
  question, not a code question; decide before building either feature.
- **B731-G2**: if console alarms are wanted, the exact authoring pattern (BAlarmSourceExt on a control
  component vs a dedicated source vs honAlarmExt) — a focused authoring investigation, deferred until the
  requirement is confirmed.
