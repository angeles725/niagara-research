# B812 · The heartbeat/liveness watchdog pattern — an author-built independent monitor that detects a STALLED producer (not a bad value) `[INFER, primitive-grounded]`

> The pattern Tridium does NOT ship: an independent monitor that detects a component whose periodic callback has
> STOPPED FIRING (a leaked/never-armed/dead Clock ticket) — as opposed to a value that is merely out of range.
> Composed from first-party primitives (B775/B801/B805) into a copy-ready shape for the operator's rt components,
> completing the B808 health surface. A DESIGN/APPLIED block — the code is a synthesis, so its `[INFER]` ratio is
> EXPECTED and healthy (§11), each element grounded in a cited primitive.
>
> **Sources**: REMITTANCE [Block 775] §775.6 (`BTimeTrigger` self-heal) + §775 (no shipped independent monitor =
> the gap), [Block 801] (Clock delay/period floor `> 0`), [Block 805] §805.4-5 (fault BIT → `BAlarmRecord` →
> `BAlarmService`), [Block 808] (health surface), [Block 787] (the leaked-timer failure this catches),
> [Block 552]/[Block 772] (alarm/point-ext authoring). Markers: `[CERT]` cited primitive · `[INFER]` design synthesis.
>
> **Type:** `design/applied`. Connects [Block 808] (its health-slot #3 = this heartbeat), [Block 775] (fills the
> MAE4-G1 residue), [Block 801]/[Block 805]/[Block 787].

## 812.1 — The gap: what value-alarms and self-heal CANNOT catch `[CERT]`
- A **value alarm** (an alarm ext / `BOutOfRangeAlgorithm`, [Block 805] §805.4-5, [Block 552]) watches the VALUE —
  out of range, or a `BStatus` fault bit set by the producer. If the producer's callback STOPS FIRING, the value
  simply goes STALE at its last-good reading — the alarm never trips. `[CERT via B805]`
- `BTimeTrigger` ([Block 775] §775.6, `javax.baja.control.trigger.BTimeTrigger:238-249`) self-heals its OWN timer
  (one idempotent `init()` from every hook + `clockChanged`→init), but a component can only self-heal a timer it
  KNOWS is broken — a leaked ticket ([Block 787] `BEvaporatorUnit` has 4 tickets, no `stopped()` cancel) or a
  never-armed one is invisible to itself. `[CERT via B775/B787]`
- **Tridium ships NO independent periodic dead-ticket/heartbeat monitor** — [Block 775] "no shipped
  `BAbstractMonitor` watches a job heartbeat" (MAE4-G1, a find-zero). That residue is exactly this pattern. `[CERT via B775]`

## 812.2 — The pattern (copy-ready shape) `[INFER, primitive-grounded]`
Two collaborating pieces — the PRODUCER emits a heartbeat, an INDEPENDENT monitor watches its age:

**(A) Producer side (each timer-driven component):**
- `@NiagaraProperty(name="lastTick", type="baja:AbsTime", flags=Flags.SUMMARY|Flags.TRANSIENT|Flags.READONLY)` —
  updated to `Clock.time()` at the TOP of every periodic callback (`execute()`/tick), BEFORE any work that could
  throw. TRANSIENT so it never persists to `config.bog` ([Block 806] persistence cost). `[INFER]`

**(B) Monitor side (one independent component, not the producer):**
- Periodic check via `Clock.schedulePeriodically(this, getCheckInterval(), doCheck, null)`, re-armed through ONE
  idempotent `init()` from `started()`/`atSteadyState()`/`changed(interval)`/`clockChanged` — the `BTimeTrigger`
  self-heal shape ([Block 775] §775.6). The re-arm delay is floored `max(1, …)` ms — a `≤ 0` throws
  `IllegalArgumentException: time <= 0` ([Block 801], proven live 5× on PANCCADIA). `[CERT-grounded]`
- On each check: `age = Clock.time().delta(getLastTick())`; if `age > staleThreshold` (recommend **3×** the
  producer's period — tolerates one missed tick + jitter) → the producer has STALLED. `[INFER]`
- On stall: set the producer's/monitor's `health` `BStatus` bit to FAULT (the [Block 808] health slot) AND raise
  a `BAlarmRecord` through `BAlarmService` — the SAME path a point-ext protection uses ([Block 805] §805.4-5:
  `BStatus fault → BAlarmRecord → BAlarmService → console → ack`). On recovery (a fresh tick), clear the bit +
  return-to-normal the alarm. `[INFER, grounded in B805]`

## 812.3 — Why it completes the B808 health surface `[INFER]`
[Block 808] §808.4 listed a "heartbeat/last-tick slot" as health-surface item #3 but left the WATCHER unbuilt.
This is that watcher. Together: `lastTick` (producer) + `health` fault bit + the alarm = a dashboard tile can show
green/amber/**fault** and the alarm console records the stall — so the leaked-timer / `time<=0` / stopped-callback
class ([Block 787]/[Block 800]) is caught the moment the producer goes quiet, NOT hours later when an operator
notices the room warming. The monitor is INDEPENDENT (its own timer), so a producer whose timer died cannot
suppress its own alarm — the key property a self-heal alone lacks. `[INFER]`

## 812.4 — Kit implication → `types/logic.md` "liveness watchdog" `[INFER, grounded]`
Add a copy-ready "liveness watchdog" recipe: the `lastTick` producer slot (TRANSIENT, set at callback top) + the
independent monitor (idempotent-`init()` `schedulePeriodically`, floor `max(1,…)`ms, `staleThreshold` = 3× period,
→ `BStatus` fault + `BAlarmRecord`/`BAlarmService`). Pairs with the [Block 808] health-surface checklist (item #3
now has its watcher) and the [Block 787] timer-lint (a leaked ticket + a missing heartbeat is the double-fault a
JACE hides). Guard: the monitor itself is a timer-driven component → it obeys the [Block 787] rule (cancel in
`stopped()`) and the [Block 801] floor.

## 812.5 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | A value-alarm can't detect a stalled producer (value goes stale, no trip) | `[CERT]` | [B805] §805.4-5; [B552] | via cited blocks |
| 2 | `BTimeTrigger` self-heals its own timer only; no shipped independent monitor | `[CERT]` | [B775] §775.6 (`BTimeTrigger.java:238-249`) + §775 find-zero | via cited blocks |
| 3 | Pattern = producer `lastTick` (TRANSIENT) + independent periodic monitor, stall at age > 3× period | `[INFER]` | §812.2, composed | design |
| 4 | Re-arm floored `max(1,…)`ms (else `time<=0`); raise via `BStatus`→`BAlarmRecord`→`BAlarmService` | `[CERT-grounded]` | [B801]; [B805] §805.4-5 | via cited blocks |
| 5 | Completes [B808] health-surface item #3 (heartbeat slot's missing watcher) | `[INFER]` | [B808] §808.4 | grounded |

**Tally:** `[CERT]`/grounded ×3 · `[INFER]` ×2. DESIGN/APPLIED block — high `[INFER]` is expected, not an
exhaustion signal (§11); every element cites a first-party primitive.

## 812.6 — Connections & open gaps
- [Block 808] (health surface — this is its item-#3 watcher), [Block 775] §775.6 (self-heal / the MAE4-G1 gap this
  fills), [Block 801] (delay floor), [Block 805] §805.4-5 (alarm-raise path), [Block 787] (leaked-timer failure),
  [Block 552]/[Block 772] (alarm/point-ext authoring).
- **B812-G1** (build/PoC, requires-execution): build the `lastTick` + independent monitor on a ColdRoomPan
  component and confirm on a live station that killing the producer's tick raises the alarm + flips the dashboard
  tile within `staleThreshold` — the pattern proven live (pairs with [Block 808] B808-G1).
