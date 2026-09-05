# B806 · Resource budget for a JACE/station — what oversaturates it, and the viability of our module logic `[CERT]`

> What can OVERSATURATE a JACE-8000/9000 or a station, and how viable the operator's module logic is on one.
> A synthesis: cites the corpus where it already answers (REMITTANCE), and closes the gaps with new Tridium doc
> (`docPlatform.txt`) and code (`EngineManager`, fox `SessionCircuits`, the operator modules) cites. Deliverable
> for the `/build-n4-module` kit: the §806.8 **resource-budget table** and the §806.9 **per-module viability
> checklist**. `live-install` where operator numbers are used → SECRETS DISCIPLINE (counts/structure, no values).
>
> **Sources**: REMITTANCE B6/B337/B338/B708/B737 (engine thread), B729/B775/B801/B787 (timers), B800 (live
> console). NEW: `organized/baja/baja/vineflower/com/tridium/sys/engine/EngineManager.java`, fox
> `SessionCircuits.java`, `docPlatform.txt` (niagara-help), operator `BDashboardService`/`BChiDashboardService`.
> Every NEW number driver-verified (grep, bypassing the `organized/` gitignore). Markers: `[CERT]` code file:line ·
> `[CERT-doc]` `docPlatform.txt:line` · `[CERT-live]` console · `[INFER]`.
>
> **Type:** `mixed`. Connects [Block 737]/[Block 6]/[Block 338], [Block 775]/[Block 801]/[Block 787], [Block 800].

## 806.1 — Engine thread (what oversaturates it) `[CERT]`
- ONE daemon thread `"Nre:Engine"` runs EVERY component callback (`changed()`, `execute()`, Clock action);
  `EngineManager.java:558-578` loop = `Thread.sleep(engineSleepPeriod) → execute()`, `:40` `engineSleepPeriod = 20L` ms. `[CERT]`
- A slow callback FREEZES the whole station — watchdogs recover a DEAD/HUNG station, NOT a merely slow one
  (REMITTANCE [Block 737] §A). A `BITable` ORD (BQL/NEQL/history) runs on a pool but BLOCKS the engine thread up
  to **30 s** waiting ([Block 338]).
- **Engine watchdog timeout = 3 min** default (policy Terminate/Log/Reboot), daemon polls the engine cycle-count
  and dumps the stack on stop. `[CERT-doc]` `docPlatform.txt:2458` area / watchdog policy.
- Hog accounting is CUMULATIVE (`spy:/sys/engineManager/hogs`, top-100 by total engine time, `EngineManager.java:437-443`);
  there is **NO per-invocation "slow callback > N ms" threshold** in code (§806.11 OPEN GAP). `[CERT]`
- Engine-thread-never-throws: wrap `changed()/started()/stopped()` in `catch(Throwable){log}` ([Block 708]). `[CERT]`

## 806.2 — Timers `[CERT]`
- Clock timers are bucketed by period in `EngineManager.java:61-65`: `shortTimerThreshold = 2100L` ms ·
  `mediumTimerThreshold = 61000L` ms; three UNBOUNDED `TicketQueue`s scanned every 100-1000 / 1000-10000 /
  1000-30000 ms respectively (`TicketQueue(minScan,maxScan)` — a SCAN bound, not a count cap). So there is no
  hard ticket limit, but every periodic ticket costs a scan + a callback on the one engine thread. `[CERT]`
- Delay/period FLOOR: `> 0` ms; `Clock.schedule` with `≤ 0` throws `IllegalArgumentException: time <= 0`
  (`EngineManager.java:326-328`) — live on PANCCADIA ×5 from `BDefrostController.armTrigger` ([Block 801]/[Block 800]). `[CERT-live]`
- Operator poll costs: ColdRoomPan `BCompressorControl` tick **5 s** (`Clock.schedulePeriodically`, [Block 787]);
  chihuahua `controlTick` **10 s** on a JDK executor (§806.4); systemMonitor **15 min** default ([Block 775]).

## 806.3 — Memory / persistence `[CERT]`
- A large String slot rewritten+persisted on every write is the main hidden cost: both `BDashboardService.java:69`
  (`@NiagaraProperty name="auditLog" type="String"`, **no `Flags.TRANSIENT`**) and `BChiDashboardService.java`
  keep a **500-entry** (`MAX_AUDIT_ENTRIES=500`, `BDashboardService.java:256` / `BChiDashboardService.java:641`)
  newline-JSON audit ring — ~100 KB `[INFER, format-grounded]`. Every successful setpoint write →
  `appendAudit()` → `setAuditLog(full string)` → the slot is dirtied → the whole ~100 KB re-persists into
  `config.bog` at the next station save. `[CERT]`
- Station auto-save default 24 h on controllers `[CERT-doc]`; chihuahua correctly marks `controlLockContentionCount`
  `Flags.TRANSIENT` (`BChiDashboardService.java:54-59`) — the pattern `auditLog` should follow. `[CERT]`
- On a JACE (flash storage) high-frequency full-BOG writes wear flash `[INFER]`.

## 806.4 — Threads `[CERT]`
- Engine = **1** thread (all callbacks serialized). Fox session circuit pool = **2** threads
  (`SessionCircuits.java:24` `new SessionCircuits.ServiceThread[2]`). `[CERT]`
- **Doctrine — no JDK executors in a station component:** chihuahua's `BChiDashboardService.java:305,314`
  (`Executors.newSingleThreadScheduledExecutor` + `scheduleAtFixedRate`) is shut down at stop (`:455
  _tickScheduler.shutdown()`), which the Niagara SecurityManager DENIES (`RuntimePermission "modifyThread"`) —
  **21×** live across two stations ([Block 800] §800.3/§800.8). Use `Clock.schedulePeriodically`, never
  `java.util.concurrent`. `[CERT-live]`
- Jetty/web worker pool size not exposed in the code layer read (§806.11 OPEN GAP).

## 806.5 — Web / servlet load `[CERT]`
- DashboardPan SPA polls every **5 s** with NO backoff (`DashboardPan-ux/src/rc/index.html:704` `pollMs: 5000`),
  ≈ **24 HTTP req/min per browser tab** (equipment + alarms) — linear in operator count on a small JACE Jetty
  pool `[INFER]`. `[CERT]`
- The servlet write path mutates a `BComponent` SYNCHRONOUSLY from a Jetty worker thread —
  `BDashboardServlet.java:274` `parent.set(prop,…)` with no `post()` to the engine thread; it propagates via a
  `BLink` into `coldRoom.changed → armTrigger` (the [Block 800] §800.2 causal chain, and one trigger of the
  `time<=0` defect). Cross-thread mutation is a race risk `[INFER]`. `[CERT]`

## 806.6 — Histories / alarms / links + platform capacity `[CERT-doc]`
- Station resource limits are `resource.limit` / N4 `globalCapacity` (devices/points/histories/links/schedules).
  Boot semantics: exceed the limit → **warning on startup**; exceed by **110% → the station will NOT boot**
  (`docPlatform.txt:2458-2459`). `[CERT-doc]`
- A concrete JACE example: `device.limit="25" point.limit="500" link.limit="400" history.limit="125"`
  (`docPlatform.txt:5528,5564`). Every station's `AuditHistory` + `LogHistory` COUNT toward `history.limit`
  (`docPlatform.txt:5585`), and normal engineering "quickly exceeds its history limit" (`:5704`). `[CERT-doc]`
- The exact **kRU cap for the JACE-8000/9000 specifically is NOT stated** in corpus or niagara-help (§806.11 OPEN GAP).

## 806.7 — How to MEASURE on a live station `[CERT-doc/CERT]`
- **Resource Manager** view = `spy:/metrics` (globalCapacity counts/limits; superuser). Engine metrics at
  `spy:/sys/engineManager` (`engine.scan.lifetime/peak/usage`, queue sizes, `EngineManager.java:414-431`) and
  `spy:/sys/engineManager/hogs` (top-100 by cumulative time). `[CERT]`
- **Automatable from OUTSIDE** (for a future `station-load.sh`): the console `GET /station/output` (used in
  [Block 800]); Fox `station:|slot:/Services/PlatformServices` (`docPlatform.txt` PlatformServices over Fox);
  the spy ORD `ip:<IP>|fox:|spy:/sys/engineManager` and `spy:/metrics`; oBIX station ORDs if the oBIX server is
  licensed. So a probe could read resource counts + engine hogs + console over Fox/spy/HTTP without Workbench. `[CERT-doc]`

## 806.8 — RESOURCE-BUDGET TABLE (kit-citable) `[CERT unless noted]`
| Dimension | Budget / limit | Cite |
|---|---|---|
| Engine thread | 1 thread, 20 ms tick, all callbacks serialized | `EngineManager.java:40,558` |
| Engine watchdog timeout | 3 min default (recovers hung, not slow) | `docPlatform.txt` watchdog policy · [B737] |
| Per-callback slow threshold | none in code (hog = cumulative only) | `EngineManager.java:437-443` — OPEN GAP |
| Max engine-thread block (a query) | 30 s (then abort) | [B338] |
| Clock delay/period floor | > 0 ms (0 → IllegalArgumentException) | `EngineManager.java:326-328` · [B801] live |
| Timer buckets | short ≤2100 ms · medium ≤61000 ms · long > · queues unbounded | `EngineManager.java:61-65` |
| Fox session circuit pool | 2 threads | `SessionCircuits.java:24` |
| Jetty worker pool | not found | OPEN GAP |
| Persisted String slot cost | full slot re-persisted to config.bog per write | `BDashboardService.java:69,256` |
| Audit ring per module | 500 entries ≈ 100 KB, NOT TRANSIENT | `:256` / `BChiDashboardService.java:641` |
| Station auto-save | 24 h default (controllers) | `docPlatform.txt` |
| globalCapacity (typical JACE) | 25 dev · 500 pt · 400 link · 125 hist (incl Audit+Log) | `docPlatform.txt:5528,5585` |
| Exceed capacity | >100% warn on boot · >110% NO boot | `docPlatform.txt:2458-2459` |
| kRU cap JACE-8000/9000 | not stated | OPEN GAP |
| Browser poll | 5 s fixed, no backoff (24 req/min/tab) | `index.html:704` |
| JDK executor in a component | forbidden — SecurityManager denies modifyThread | `BChiDashboardService.java:455` · [B800] |

## 806.9 — VIABILITY CHECKLIST per rt module (count before deploying to a JACE) `[CERT-grounded]`
Before a module goes on a JACE, COUNT and check:
1. **Engine-thread cost** = Σ(periodic callbacks × frequency). Each `execute()`/tick must run ≪ 20 ms. ColdRoomPan
   `BCompressorControl` fires `execute()` 12×/min (5 s). `[CERT B787]`
2. **Clock tickets, and every one cancelled in `stopped()`.** `BEvaporatorUnit` keeps 4 tickets with NO `stopped()`
   cancel → 4 leaked timers ([Block 787]) — mirror `BDefrostController.stopped()→cancelAll()`. `[CERT]`
3. **Clock delays floored `> 0`.** `BDefrostController.armTrigger` passed `≤ 0` → `time<=0` ×5 live; floor at
   `max(1, interval-elapsed)`. `[CERT-live B800/B801]`
4. **No `java.util.concurrent` executors** — use `Clock`. (chihuahua's is the counter-example, 21× live.) `[CERT]`
5. **No large persisted String slot** rewritten per action — mark `Flags.TRANSIENT` or write a file, not a BOG slot.
   (`auditLog` 500-entry ~100 KB per DashboardPan + chihuahua.) `[CERT]`
6. **Servlet writes hop to the engine thread** (avoid raw cross-thread `BComponent.set`); poll with backoff. `[INFER]`
7. **globalCapacity budget**: proxy points < 500, histories < 125 (incl Audit+Log), links < 400, devices < 25;
   >110% = no boot. Count histories a dashboard adds per sensor. `[CERT-doc]`
8. **Guard writes with `isRunning()`** — `BEvaporatorUnit.applyRunCmd` threw `NotRunningException` ×6 live. `[CERT-live B800]`

## 806.10 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Engine = 1 thread, 20 ms tick; slow callback freezes station | `[CERT]` | `EngineManager.java:40,558`; [B737] | Y — grep |
| 2 | Timer buckets 2100/61000 ms, unbounded queues; delay floor >0 | `[CERT]` | `EngineManager.java:61-65,326` | Y — grep |
| 3 | Fox circuit pool = 2 threads | `[CERT]` | `SessionCircuits.java:24` | Y — grep |
| 4 | Persisted 500-entry ~100 KB audit ring re-saved per write; not TRANSIENT | `[CERT]` | `BDashboardService.java:69,256`; `BChiDashboardService.java:641` | Y — grep |
| 5 | globalCapacity: >110% → no boot; 25/500/400/125 JACE example | `[CERT-doc]` | `docPlatform.txt:2458-2459,5528,5585` | Y — grep |
| 6 | Poll 5 s no backoff; JDK-executor forbidden (modifyThread) | `[CERT]`/`[CERT-live]` | `index.html:704`; [B800] | Y |
| 7 | Exact JACE kRU cap + per-callback slow threshold + Jetty pool = OPEN | `[INFER]` | §806.11 | honest gaps |

**Tally:** `[CERT]` ×4 · `[CERT-doc]` ×1 · `[CERT-live]`/mixed ×1 · `[INFER]` ×1. No unmarked claims.

## 806.11 — Connections & open gaps
- REMITTANCE: [Block 737] (engine thread + watchdog), [Block 338] (30 s query block), [Block 708] (never-throw),
  [Block 775]/[Block 729]/[Block 801]/[Block 787] (timers), [Block 800] (live console).
- OPEN GAPS (blocked-on-source / requires-live): (1) exact kRU/globalCapacity DEFAULT cap for JACE-8000/9000 —
  not in corpus or niagara-help; (2) per-invocation slow-callback ms threshold — none in `EngineManager` (only
  cumulative hog); (3) Jetty worker-pool size on a JACE; (4) DashboardPan servlet cache-control headers.
- **B806-G1** (requires-execution): read `spy:/metrics` + `spy:/sys/engineManager/hogs` on a live JACE to put real
  counts against these limits — the input a future `station-load.sh` probe automates.
