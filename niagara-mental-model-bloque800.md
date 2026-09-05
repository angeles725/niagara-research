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

## 800.8 — Addendum: a live schema-risk OUTAGE + REFLOW cert-chain (third attribution channel) `[CERT-live]`

**(1) PANCCADIA station FAILED TO BOOT after a ColdRoomPan-rt reload** — `console_backup_260903_1704.txt`, all
at `17:04:32 03-Sep-26`, right after the `17:04:09 Out-of-date: Module changed "ColdRoomPan-rt"` reload:
- `WARNING [sys.xml] Cannot set property RoomPanel.setpoint: java.lang.ClassCastException: javax.baja.status.BStatusNumeric cannot be cast to javax.baja.sys.BDouble [943:40]`
- `WARNING [sys.xml] Missing frozen property: differentialUp [944:35]` · `zoneHighLimit [945:35]` · `zoneLowLimit [946:34]` · `evapLowLimit [947:34]`
- `WARNING [sys.xml] Missing slot StatusNumeric.startDelay [948:47]`
- `SEVERE [sys] Cannot load station  java.lang.ClassCastException: javax.baja.sys.BRelTime cannot be cast to javax.baja.sys.BComplex`

This is the **B795 schema-risk OUTAGE class observed LIVE** `[CERT-live]`: the reloaded module's slots no longer
match the persisted `.bog` (a retype `BStatusNumeric`↔`BDouble` + a `BRelTime`↔`BComplex` mismatch + removed/renamed
frozen props) → the station will not boot. **Closure evidence for B795-G1 (issue #50, station-required gap)** — the
exact `retype`/`remove_slot`/schema-mismatch verdict [Block 799] fixtures model, now confirmed against a live station.

**Third attribution channel (updates §800.5):** NONE of these lines carry a `com.angeles` frame OR our logger tag —
they are `SEVERE [sys] Cannot load station` + `[sys.xml]` warnings that NAME our types/slots (`RoomPanel.setpoint`,
`StatusNumeric.startDelay`, `differentialUp`…). So `triage-console.sh` needs a **THIRD attribution match**: a
`Cannot load station` SEVERE and `[sys.xml]` `Cannot set property`/`Missing frozen property`/`Missing slot` warnings
whose named type/slot belongs to one of our modules. Frame-only OR tag-only misses a total station outage.

**(2) REFLOW (Spanish locale, Jun 6–Jul 2) — cert-chain trust gap** `[CERT-live]`:
- `modifyThread` unschedule-controlTick ×**12** here (×**21** total with MX60's 9) — same chihuahua JDK-executor defect (§800.3).
- `ADVERTENCIA [chihuahua] BChiDashboardService: cannot force-load ChiAlarmHelper …` ×**9** — a lazy class-load deferral.
- `ADVERTENCIA [loader] Could not validate certificate path for entry com/angeles/chihuahua/components/BChiDashboardService.class in module chihuahua-rt: Could not validate cert chain` ×**7** for that exact class (25 `Could not validate cert` lines across chihuahua-rt entries in REFLOW). The chihuahua-rt jar is SIGNED but its signing cert is **not trusted by that station → it loads as UNSIGNED**.
  - **KIT GAP (doctrine/tool candidate):** `verify-module.sh` checks `META-INF/NIAGARA4.SF` **presence** only — NOT
    chain TRUST against the target station's trust store. A jar can pass the gate and still fail cert-chain
    validation on the deploy target. Candidate: a `--target-trust <station>` check, or document that signature
    presence ≠ trust. (Note: my verified count for the exact `BChiDashboardService.class` entry is 7, not the 11
    quoted in the request — reporting what the grep returns.)
