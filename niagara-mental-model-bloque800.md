# B800 · Console-log census: exceptions through the operator's OWN modules (triage-console.sh contract input) `[CERT]`

> **§20 DOCUMENT-mode capture** (not gap-discovery). Censuses every exception whose stack passes through a
> `com.angeles.*` frame — OR is attributed by the module's logger TAG — across 14 station `console_backup_*.txt`
> from three live stations (PANCCADIA, REFLOW, HoneywellMX605132026). `live-install` source → **SECRETS
> DISCIPLINE**: structure/counts/frames only, no secret values. It is the contract input for the new kit tool
> `triage-console.sh` (§800.5 names the row shape and the two hard tool requirements).
>
> **Sources**: 14 preserved console backups (paths + sha256 in `sources/SOURCES.md`, this session). Cross-refs:
> [Block 787] (BEvaporatorUnit timer leak), [Block 801] (the `time<=0` root-cause, live-confirmed), chihuahua
> source `~/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`. Markers: `[CERT]` verbatim in a console file
> or in source (`file:line`) · `[INFER]` operational reading.
>
> **Type:** `capture`. Connects [Block 787], [Block 801], [Block 754]/[Block 739] (Clock decode), the chihuahua
> corpus.

## 800.1 — Method + the attribution rule `[CERT]`
A frame-only scan is NOT enough. Our modules surface in the console two ways, and a tool must key on BOTH:
- **(a) a `com.angeles.*` stack frame** — `at com.angeles.<pkg>.<Class>.<method>(<File>.java:<line>)`;
- **(b) the module LOGGER TAG** — `WARNING/ADVERTENCIA [<ts>][<tag>] …` where `<tag>` ∈ `{coldRoomPan,
  dashboardpan, chihuahua}` (and the Class name in the message). The 9 chihuahua `modifyThread` findings
  (§800.3) have NO `com.angeles` frame at all — a frame-only tool misses every one.

## 800.2 — Exception census (through `com.angeles` frames) `[CERT]`

All on **PANCCADIA** (REFLOW's `com.angeles` hits are `datacenter`/`interfaz1` INFO lines, not exception
frames; no exception traces pass through our ColdRoom/Dashboard/chihuahua code there).

| Exception : message | Own frame (`com.angeles…:line`) | Cnt | First → Last (CST) | Cause |
|---|---|---|---|---|
| `java.lang.IllegalArgumentException: time <= 0` | `ColdRoomPan.BDefrostController.armTrigger(BDefrostController.java:431)` (via `atSteadyState:378` / `changed:395`) | 4 | 02-Sep 16:20:10 → 22:28:21 | `armTrigger` calls `Clock.schedule` with delay ≤ 0 when the defrost interval is overdue — root-caused live in [Block 801] `[CERT]` |
| `java.lang.IllegalArgumentException: time <= 0` | `ColdRoomPan.BDefrostController.armTrigger(BDefrostController.java:495)` | 1 | 03-Sep 18:58:19 | same defect, second call site `[INFER]` |
| `javax.baja.sys.NotRunningException` | `ColdRoomPan.BEvaporatorUnit.applyRunCmd(BEvaporatorUnit.java:519)` | 3 | 02-Sep 00:15:16 → 16:19:57 | write to a slot/action while the component is not running (boot/reload transition) `[INFER]` |
| `javax.baja.sys.NotRunningException` | `ColdRoomPan.BEvaporatorUnit.applyRunCmd(BEvaporatorUnit.java:670)` | 2 | 02-Sep 16:44:07 → 19:43:13 | same, second call site `[INFER]` |
| `javax.baja.sys.NotRunningException` | `ColdRoomPan.BEvaporatorUnit.applyRunCmd(BEvaporatorUnit.java:683)` | 1 | 02-Sep 19:43:13 | same, third call site `[INFER]` |

**Causal enrichment `[CERT]`**: one `time<=0` at `console_backup_260903_0301.txt:250-269` is triggered by a
live operator DashboardPan write — `[22:28:08][dashboardpan] handleSetpointWrite: wrote defrostDuration=600000
… user=admin` (:249), which propagates via a `BLink` (`BDashboardServlet.handleSetpointWrite:274` → `doPost:147`
→ `BComplex.set` → `BLink.propagate` → `coldRoom.changed:350` → `armTrigger`). So the dashboard write is one
trigger of the re-arm bug.

## 800.3 — chihuahua doctrine finding: JDK executor vs Clock `[CERT]`
HoneywellMX605132026 (the chihuahua station, Spanish locale). Attributed by the `[chihuahua]` tag (no
`com.angeles` frame):

- **9×** `ADVERTENCIA [<ts>][chihuahua] BChiDashboardService: failed to unschedule controlTick: access denied
  ("java.lang.RuntimePermission" "modifyThread")`, at service **stop**, across 8 console files
  (first 07-Jun 00:23:52; recurs through Jun). `[CERT]`
- **Root cause in source `[CERT]`** — `chihuahua-rt/src/com/angeles/chihuahua/components/BChiDashboardService.java`:
  `controlTick` is scheduled on a **JDK `java.util.concurrent` scheduler**, not `Clock`:
  `:12` `import …ScheduledExecutorService` · `:305` `_tickScheduler = Executors.newSingleThreadScheduledExecutor(…new Thread(r,"chihuahua-controlTick")…)` ·
  `:314` `scheduleAtFixedRate(new Runnable(){ controlTick(); }, …)`. Unscheduling at stop calls
  `:455` `_tickScheduler.shutdown()`, which needs `RuntimePermission("modifyThread")` — the Niagara
  SecurityManager **denies** it, so the executor never shuts down cleanly.
- **DOCTRINE `[INFER, grounded]`**: a station component must schedule periodic work through **`javax.baja.sys.Clock`**
  (`schedule`/`schedulePeriodically`), NOT `java.util.concurrent` — the SecurityManager denies JDK thread
  management (`modifyThread`). Contrast: ColdRoomPan's `BDefrostController` DOES use `Clock` (right mechanism)
  but passes `time<=0` (§800.2) — two distinct defect classes, one per module.

## 800.4 — Boot + module-reload history `[CERT]`
- Boot markers (`[nre] Booting`): PANCCADIA **6**, REFLOW **7**, HoneywellMX **1** (per-file, one per backup).
- `Out-of-date: Module changed "<mod>"` (reload history, all stations): `interfaz1-rt` ×3, `datacenter-rt` ×3,
  `chihuahua-wb` ×2, `DashboardPan-rt` ×2, `ColdRoomPan-rt` ×2, `chihuahua-ux` ×1, `chihuahua-rt` ×1 — every
  reload immediately follows a `[nre] Booting`, i.e. hot module swaps during commissioning.

## 800.5 — Contract for `triage-console.sh` (the row shape + hard requirements) `[CERT-artifact]`
**Columns that matter** (one row per distinct finding):
`station · module · exception_class · message(normalized) · own_frame(file:line) · count · first_ts · last_ts · attribution(frame|tag) · cause_note`
Module verdict / severity: group by `(station, exception_class, normalized_message, own_frame)`; count +
first/last from the introducing log line's `[HH:MM:SS DD-Mmm-YY]`.

**Two HARD tool requirements this census proved:**
1. **Dual attribution** — match BOTH a `com.angeles.*` stack frame AND the module logger tag
   `[coldRoomPan|dashboardpan|chihuahua]`. A frame-only tool misses the 9 chihuahua `modifyThread` findings.
2. **Locale + encoding robustness** — parse English (`INFO|WARNING|SEVERE`) AND Spanish
   (`INFORMACIÓN|ADVERTENCIA|GRAVE`) level names; the HoneywellMX console is Spanish with accents as **mojibake
   (non-UTF-8 bytes)** — read latin-1/bytes, never assume UTF-8. (340 Spanish-level lines in one file alone.)
Also record `[nre] Booting` and `Out-of-date: Module changed "<mod>"` per file (§800.4).

## 800.6 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `time<=0` ×5 through `BDefrostController.armTrigger` (4@:431 + 1@:495), PANCCADIA | `[CERT]` | console traces; matches [Block 801] | Y — parsed + grep |
| 2 | `NotRunningException` ×6 through `BEvaporatorUnit.applyRunCmd` (:519/:670/:683) | `[CERT]` | console traces | Y |
| 3 | chihuahua `modifyThread` ×9 across 8 files; root = JDK `ScheduledExecutorService` shutdown | `[CERT]` | grep count 9; `BChiDashboardService.java:305,314,455` | Y — count + source read |
| 4 | Doctrine: station components must use `Clock`, not `java.util.concurrent` | `[INFER]` | SecurityManager denies `modifyThread` | grounded |
| 5 | Dual attribution (frame OR tag) + Spanish/mojibake are required tool behaviors | `[CERT]` | §800.1/§800.3 (frame-less chihuahua findings) | Y |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1 (+`[CERT-artifact]` row shape). Capture block — ratio not an exhaustion signal (§11).

## 800.7 — Connections & open gaps
- [Block 801] (time<=0 root-cause, live), [Block 787] (BEvaporatorUnit timer leak — same module), chihuahua corpus.
- **B800-G1** (doctrine PoC, requires-execution): confirm on a live station that replacing chihuahua's
  `ScheduledExecutorService` with `Clock.schedulePeriodically` removes the stop-time `modifyThread` denial.
- No numbering gap; `triage-console.sh` implements §800.5 (QA pre-staging its RED).
