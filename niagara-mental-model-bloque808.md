# B808 · Who watches the logic — feedback surfaces, traced from the ColdRoomPan BEvaporatorUnit fault to the operator `[CERT]`

> How a component knows (and SHOWS) it is working: the two feedback channels a station has, WHO observes each,
> and — traced end-to-end — where the operator's ColdRoomPan runtime fault actually surfaces (spoiler: the console
> only). Deliverable for the kit: a "health/feedback surface" checklist for rt components.
>
> **Sources**: operator `ColdRoomPan-rt` (`BEvaporatorUnit`/`BColdRoom`) + `DashboardPan-rt` (`BRoomPanel`) source
> (driver-verified by grep); REMITTANCE B775 (BAbstractMonitor/watchdog), B552 (alarm ext feedback), B734 (status),
> B806 (Resource Manager/hog), B800 (the live faults), B787 (the timer leak). Markers: `[CERT]` source `file:line`
> · `[INFER]`.
>
> **Type:** `mixed`. Connects [Block 800] (the console faults), [Block 787] (BEvaporatorUnit timer leak), [Block 775]
> (monitor), [Block 552] (alarm ext), [Block 806] (measurement surface).

## 808.1 — The two feedback channels `[CERT]`
A Niagara station carries a component's state two independent ways:
- **(A) VALUE status** — every point value is a `BStatus*` (fault/down/stale/overridden bits ride ON the value).
  It propagates through `BLink`s, so a downstream reader is "fault-aware". ColdRoomPan uses this: `BEvaporatorUnit`
  outputs are `BStatusBoolean`/`BStatusNumeric` (`:247,362,385,408,431,454`), and DashboardPan `BRoomPanel` does a
  "fault-aware read" of the linked `BStatus` (`BRoomPanel.java:39`; unlinked → null status). `[CERT]`
- **(B) COMPONENT / LOGIC health** — whether the component's own code is running correctly (timer armed, callback
  not throwing). This is NOT carried by a value's status bits; a station exposes it via **status FLAGS on the
  component, an ALARM extension, or a MONITOR** — see §808.3. ColdRoomPan exposes NONE of these (§808.2).

## 808.2 — The trace: a BEvaporatorUnit runtime fault → the operator `[CERT]`
Follow the real B787/B800 fault (leaked timer / `time<=0` / `NotRunningException`):
1. The fault happens inside a callback. `BEvaporatorUnit` wraps every callback in
   `catch (Throwable t) { logError("atSteadyState"/"started", t); }` (`BEvaporatorUnit.java:832,845`) — it
   **LOGS and swallows**; it does NOT set any fault status or slot. `[CERT]`
2. That log line is the ONLY artifact — an `[engine]`/`[coldRoomPan]` `WARNING` in the console
   (the [Block 800] census: `time<=0` ×5, `NotRunningException` ×6). `[CERT-live via B800]`
3. **No operator-visible surface exists:** ColdRoomPan-rt has **no `BAlarmSourceExt`, no alarm ext, no fault-status
   OUTPUT slot** (grep for `BAlarmSourceExt|setFault|faultCause|raiseAlarm|offnormal` over `ColdRoomPan-rt/*.java`
   returns ZERO). `[CERT — proven absence]`
4. The one "alarm" it has — `roomHighAlarmLimit` — is a threshold on the temperature VALUE, "**feeds the room
   high-temp VISUAL alarm only (does NOT affect control)**" (`BColdRoom.java:67`), and the source itself notes
   "**pair with a sensor-fault alarm for visibility**" (`:75`) — a self-documented missing feedback path. `[CERT]`

**Conclusion `[INFER, grounded]`:** for a LOGIC fault, *nobody watches at the operator level* — it reaches only
the engine console (which needs `triage-console.sh`, [Block 800], to be read at all). VALUE faults reach the
dashboard; LOGIC faults do not.

## 808.3 — Who Tridium makes watch (REMITTANCE) `[CERT via cited blocks]`
Tridium's own modules expose component health through observers ColdRoomPan omits:
- **`BAbstractMonitor` / watchdogs** — `systemMonitor` polls health on a timer and raises status; the engine
  watchdog (3 min) catches a HUNG station, not a slow/faulted component. [Block 775] / [Block 806].
- **`BAlarmSourceExt` (alarm ext)** — the intended feedback path: a component routes offnormal/fault to the
  Alarm Service → alarm console + history → operator. [Block 552]/[Block 787].
- **Status FLAGS (fault/down/stale/disabled)** on the component/value, observed by fault-aware readers and the
  nav-tree colouring. [Block 734].
- **Resource Manager + engine hog logs** (`spy:/sys/engineManager/hogs`, `spy:/metrics`) — the platform-level
  view of "is a component eating the engine thread". [Block 806].
- Observers = the **Alarm Service** (routes/records), **History** (persists), and the **operator UI**.

## 808.4 — Kit doctrine: the health/feedback surface checklist for an rt component `[INFER, grounded]`
A control module should let a dashboard/alarm console tell it is HEALTHY without reading the console. Expose:
1. **A fault-status OUTPUT slot** (`BStatusBoolean`/enum) set in the `catch` block — not just `logError`. A swallowed
   `Throwable` must flip a visible health slot. (ColdRoomPan `BEvaporatorUnit.java:832,845` logs only — the fix.)
2. **A `BAlarmSourceExt`** (or an explicit route to the Alarm Service) on the component's own offnormal/fault, so
   the fault reaches the alarm console + history, not just the log. [Block 552]
3. **A heartbeat / last-tick timestamp slot** the dashboard reads to confirm the periodic callback is alive
   (a leaked/never-armed timer, [Block 787]/[Block 801], is invisible without it).
4. **Guard callbacks with `isRunning()`** and set the fault slot on the transient (the `NotRunningException` ×6,
   [Block 800]) instead of throwing into the log.
5. **A value-threshold visual alarm is NOT a health surface** — `roomHighAlarmLimit` watches the temperature, not
   the logic; pair it with a component-fault alarm (the source's own note, `BColdRoom.java:75`).
So a dashboard's per-equipment tile can show green/amber/fault from (1)+(3), and the alarm console from (2).

## 808.5 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Two channels: value-status (propagates to dashboard) vs component-health | `[CERT]` | `BEvaporatorUnit` BStatus outputs; `BRoomPanel.java:39` fault-aware read | Y — grep |
| 2 | BEvaporatorUnit swallows faults to the log (catch→logError), sets no fault status | `[CERT]` | `BEvaporatorUnit.java:832,845` | Y — grep |
| 3 | ColdRoomPan-rt has NO alarm ext / fault-status output (proven absence) | `[CERT]` | grep `BAlarmSourceExt|setFault|raiseAlarm` = 0 | Y — grep |
| 4 | roomHighAlarmLimit = value-threshold visual only; source flags the missing sensor-fault alarm | `[CERT]` | `BColdRoom.java:67,75` | Y |
| 5 | Health surface = fault-status slot + alarm ext + heartbeat + isRunning-guard | `[INFER]` | §808.4, grounded in B552/B775/B787/B800 | grounded |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1. No unmarked claims.

## 808.6 — Connections & open gaps
- [Block 800] (the console faults this traces), [Block 787] (timer leak), [Block 552] (alarm ext), [Block 775]
  (monitor/watchdog), [Block 734] (status flags), [Block 806] (Resource Manager/hog).
- **B808-G1** (build/PoC, requires-execution): add a `health` `BStatusBoolean` + a `BAlarmSourceExt` to a ColdRoomPan
  component and confirm a leaked-timer/`time<=0` fault flips the dashboard tile + raises an alarm — the doctrine proven live.
