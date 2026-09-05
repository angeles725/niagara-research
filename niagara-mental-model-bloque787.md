# B787 · Conformance audit — timer/watchdog: BEvaporatorUnit lacks a `stopped()` ticket-cancel (OMV2)

> **Scope**: CONFORMANCE AUDIT of our three modules' timer usage against the idiom (B775: keep the cancel handle,
> cancel on `stopped()`, re-arm on `changed()`; B729: arm in BOTH `started()` and `atSteadyState()`). ONE real
> finding (`BEvaporatorUnit`), the rest conform. Focus: `own-modules-vs-exemplars` (OMV2). Routes to a client
> punch-list item + a candidate BITING CHECK for `verify-module.sh`.
>
> **Sources**: FUENTE 3 our real source (verified this session): ColdRoomPan-rt (`BDefrostController`,
> `BEvaporatorUnit`, `BColdRoom`), CompPan-rt (`BCompressorControl`), DashboardPan-rt; reference chihuahua-rt
> (`BChiDashboardService`). FUENTE 1: B775 (timer idiom), B729/B730 (arm lifecycle), B741/B743 (defrost timer QA),
> B760 (punch-list). READ-ONLY. English (post-B115).

---

## 787.1 — The idiom under test `[CERT]`
B775: a scheduled timer's cancel HANDLE must be kept in a field, cancelled on `stopped()`, and re-armed on
`changed()` when its interval is configurable. B729: arm in BOTH `started()` (guarded by `Sys.atSteadyState()`) and
`atSteadyState()` so a late commissioning mount still fires.

## 787.2 — Conformant baseline (our own modules) `[CERT]`
- **`BDefrostController`** (ColdRoomPan): 4 `Clock.Ticket` fields; every `Clock.schedule` return is assigned; `stopped()`
  → `cancelAll()` (`BDefrostController.java:503-505`); `changed()` re-arms on `mode`/`interval` edit; armed in BOTH
  `atSteadyState()` and `started()` (late-mount fix present, commented `[ev: B729]`). Fully conformant.
- **`BCompressorControl`** (CompPan): 2 ticket fields; `stopped()` cancels tick + `powerOnTicket`
  (`BCompressorControl.java:1799-1802`); tick period is a fixed 5s constant (nothing to re-arm), all configurable
  timing read each `execute()` cycle and `changed()` re-runs `execute()`; armed in both hooks. Fully conformant.

## 787.3 — THE FINDING: `BEvaporatorUnit` has no `stopped()` ticket-cancel `[CERT]`
`BEvaporatorUnit` (ColdRoomPan) keeps 4 delay tickets in fields (`startDelayTicket`/`stopDelayTicket`/
`defrostEntryTicket`/`powerOnTicket`) and cancels them on RE-ARM via `cancelTicket()` (called at
`BEvaporatorUnit.java:821,895,908,1079`), with every `Clock.schedule` assigned to a field (`:825,:900,:913`). But it
has **NO `stopped()` override** — `grep -c 'public void stopped('` = 0; only `atSteadyState()` (:810), `started()`
(:836), `changed()` (:849) exist. So the 4 pending delay tickets are cancelled on re-arm but NEVER on component
stop/disable. The expiry callbacks (`doStartDelayExpired`/`doStopDelayExpired`/`doPowerOnExpired`/
`doDefrostFanOffExpired`) guard on `inDefrost`/`runCmd` but NOT on `isRunning()`, so a ticket in flight when the unit
is stopped can still fire `setBool(...)` on a stopped unit.
**Severity: LOW–MEDIUM** — the delays are short (seconds/minutes) so the window is small, but it violates the
"cancel on `stopped()`" half of the idiom that BOTH sibling timer-owners (BDefrostController, BCompressorControl)
honor. This is the exact inconsistency an audit exists to catch. `[INFER]` on the actuation-after-stop consequence;
`[CERT]` that `stopped()` is absent.
(NOTE: the CONFIGURABLE delay durations are read at arm time and not re-applied by `changed()` — this is ACCEPTABLE
for edge-triggered one-shot `BBooleanDelay`-style delays, NOT a periodic-interval no-re-arm defect.)

## 787.4 — Clean (no block owed elsewhere) `[CERT]`
`BColdRoom` (only `Clock.time()` stamps, no scheduled timer), `DashboardPan-rt` (no `Clock`/`schedule`/`Ticket` —
presentation only), and NO `BAbstractMonitor`/threshold watchdog in any of the three modules (a valid find-zero;
chihuahua's `*Monitor` classes are `BComponent` seed factories, not timer watchdogs).

## 787.5 — chihuahua reference: same idiom, different handle type `[CERT]`
`BChiDashboardService` uses a `java.util.concurrent.ScheduledExecutorService` (for cross-version portability): the
future is kept in a field `_tickHandle` (`ScheduledFuture`), armed in `started()` via `scheduleAtFixedRate`, and
cancelled in `stopped()` (`_tickHandle.cancel(false)` + `_tickScheduler.shutdown()`). It arms in `started()` only —
acceptable because a `BAbstractService.started()` always runs and the executor is independent of steady-state (the
B729 atSteadyState-only late-mount trap does not apply to a service-plus-executor). Same "keep the handle, cancel on
stop" idiom our `Clock.Ticket` owners follow — and the one BEvaporatorUnit half-follows.

## 787.6 — Two routings `[INFER, grounded]`
1. **Client punch-list (module change, out of kit scope)**: add to `BEvaporatorUnit` a `public void stopped() throws
   Exception { cancelTicket(); super.stopped(); }` (mirroring BDefrostController/BCompressorControl); optionally guard
   the four expiry callbacks with `isRunning()`. Ties B760 punch-list.
2. **Kit implication — candidate BITING CHECK for `verify-module.sh` (QA RED-first)**:
   - Cheap grep guard: `Clock.schedule*(...)` whose return value is NOT assigned (fire-and-forget ticket). Deterministic;
     ZERO hits on our corpus today (good regression guard, low finder-value now); FP risk LOW (a multi-line
     `field =\n Clock.schedule(...)` split evades the single-line regex — a known false-negative).
   - STRONGER structural check (the one that catches THIS defect): a class that owns a `Clock.Ticket` field (or calls
     `Clock.schedule`) but has NO `stopped()` method that cancels it. Not a one-line grep (needs per-class correlation
     of the ticket-field presence vs a `stopped()` calling `.cancel()`), so it is an AST/structured check — but it is
     the real biting rule, and it fires on BEvaporatorUnit today and passes BDefrostController/BCompressorControl.
   Recommend: ship the cheap grep as a regression guard AND propose the structural "ticket-owner-without-stopped-cancel"
   check as the biting rule QA writes RED first (fixture = a class with a Clock.Ticket field + no stopped-cancel → FAIL).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BDefrostController conforms: tickets kept, `stopped()`→`cancelAll`, re-arm on changed, both arm hooks | [CERT] | BDefrostController.java:503-505 (+ armed atSteadyState/started) |
| 2 | BCompressorControl conforms: `stopped()` cancels tick + powerOnTicket | [CERT] | BCompressorControl.java:1799-1802 |
| 3 | FINDING: BEvaporatorUnit keeps 4 tickets + cancels on re-arm but has NO `stopped()` override | [CERT] | BEvaporatorUnit.java:821,895,908 (cancelTicket on re-arm); grep `public void stopped(` = 0 |
| 4 | Expiry callbacks guard on inDefrost/runCmd, not isRunning() → a stopped-unit actuation window | [CERT/INFER] | [CERT] callbacks exist; [INFER] on the actuation consequence |
| 5 | BColdRoom/DashboardPan-rt have no timers; no BAbstractMonitor watchdog in any module | [CERT] | grep = 0 |
| 6 | chihuahua baseline = ScheduledExecutorService future kept + cancelled in stopped() (same idiom) | [CERT] | BChiDashboardService (_tickHandle) |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. Finding + baselines grep-verified inline this session.

## Connections
- **B775** (timer idiom — the conformance standard), **B729/B730** (arm lifecycle — BEvaporatorUnit's arm hooks
  conform; only the stop-cancel is missing), **B741/B743** (defrost timer QA), **B760** (punch-list — this adds one
  item). **B778** (the "keep the handle" discipline generalizes across service/timer resources).

## Open gaps
- **OMV2-G1** — whether a `Clock.Ticket` firing on a stopped `BEvaporatorUnit` actually actuates a relay (vs a no-op)
  needs a live/station test (requires-execution); the source shows the callbacks lack an `isRunning()` guard, but the
  real-world effect is unproven read-only.

## Kit implication (→ a `verify-module.sh` biting check + a client punch-list item)
Propose the "timer-ticket owner without a `stopped()`-cancel" structural check (fires on BEvaporatorUnit, passes the
two conformant siblings) as the biting rule QA RED-firsts, plus the cheap "`Clock.schedule` return value discarded"
grep as a regression guard. Client punch-list: add `stopped(){ cancelTicket(); super.stopped(); }` to BEvaporatorUnit.
