# Block 478 — Who enforces licensing at runtime: the `niagarad` platform daemon supervises stations, treats license-failure exit codes (-3/-6) as non-recoverable, runs its OWN platform-feature license manager — and the §14 reconciliation that `com.tridium.niagarad.license.*` DOES exist (closes B477-G1)

> **Focus:** `licensing` (gap **B477-G1**). **Question:** when a station calls `Nre.licenseFailure()`
> (`System.exit(-3)`) or fails a required module signature (`System.exit(-6)`), WHO acts on it, and what does
> the platform daemon read/enforce about licensing itself? This answers the operator question "qué protege /
> qué ejecuta / qué lee el licenciamiento" at the daemon layer.
>
> **Sources (new this pass):** first-hand Vineflower decompile of `bin/ext/niagarad.jar`
> (`sources/decompiled/niagarad-ext/`, sha256 `8d295b6db249f54e0bf61365f9e01df3fbb839c48f2a0da8a318999700afd716`,
> 558,055 B, 105 `.java`). READ-ONLY: no launcher/daemon/station was executed. Markers per §3.
>
> **⚠ This block RETRACTS a wrong §14 claim I introduced 2026-08-24** (that `com.tridium.niagarad.license.*`
> does not exist). See §478.5.

## §478.1 — Station supervision (how niagarad launches/monitors a station) `[CERT]`

- **Spawn:** `StationApp.launch()` builds `{ NIAGARA_HOME/bin/station, appName,
  "-daemonspawn:"+<AES-256 enc user/pw/addr/ports>, "-rp:"+<runtime profiles> }` and runs
  `new ProcessBuilder(commandString)` in `apps/<name>`, `redirectErrorStream(true)` — executable is `station`
  (not `station.exe`). `StationApp.java:586-597`.
- **Auto-start/restart:** parsed from `daemon.properties` keys `station.<name>.isautostart|isautorestart`
  (`App.updateAttribute() :154-159`; defaults `isAutoStart=false`, `isAutoRestart=true`, `App.java:63-64`).
  On `AppRegistry.start()` only auto-start apps launch (`AppRegistry.java:76-80`). A `WatchDirectoryThread`
  polls the apps dir every 5 s (`AppRegistry.java:490-492`).

## §478.2 — Reaction to a LICENSE-FAILURE exit code (the load-bearing answer) `[CERT]`

- Exit code read in `App.waitForAppExit()`, handled in `App.run()` (`App.java:484-576`); normalized
  `if (exitCode > 127) exitCode -= 256;` (`:490-492`) so raw 253 → −3.
- **Codes are classified** by `StationApp.isRecoverableError(int)` (`StationApp.java:622-634`):
  `-7,-6,-5,-4,-3,-2 → false` (NON-recoverable); default → true.
- **So `-3` (from `Nre.licenseFailure`) and `-6` (module-signature failure) are NON-recoverable:** the station
  is set `FAILED(4)`, logged `"<type> <name> failed, rc = -3"` (`App.java:522-529`); the restart gate
  (`App.java:538-558`) calls `isRecoverableError` → false → **does NOT restart**, logging `"preventing …
  restart, error is not recoverable"` (`:554-557`). **A license-failed station is left down/FAILED, not
  restarted.**
- Other codes: `0 → IDLE(0)` (or `HALTED(6)`); `-99 → "requested reboot on exit"` → `queueReboot()`
  (`App.java:501-520`). If a *recoverable* error occurs but `!allowRestart()` (daemon.properties
  `allowStationRestart`), it escalates to `NiagaraDaemon.failureReboot()` (`App.java:546-553`).
- App-status constants: `IDLE 0, STARTING 1, RUNNING 2, STOPPING 3, FAILED 4, UNKNOWN 5, HALTED 6`
  (`App.java:31-37`).

## §478.3 — The daemon's OWN platform-feature license enforcement `[CERT]`

niagarad reloads `LicenseManager` at startup (`NiagaraDaemon.java:797`) and gates station launch on
**platform-level** features (NOT per-module signatures — those are delegated to the station):

- **Station-count:** `station` feature `station.limit` → `PlatformInfo.maxRunningStations()`
  (`PlatformInfo.java:265-281`; missing/expired → 1; `"none"` → unlimited; else default 32). Enforced in
  `AppRegistry.canStartApp()` (`currentRunningApps < maxRunningApps`, `AppRegistry.java:317-326`) with message
  `"would exceed licensed running station count"` (`App.java:219-249`).
- **JRE / OS:** `isValidJre8License()`, `isValidQnx7License()` (`StationApp.java:120-173,321-325`).
- **Capacity/heap:** `globalCapacity` feature `heap.limit` → rewrites `-Xmx` in `nre.properties`
  (`StationApp.java:334-425`).
- **Subscription:** if `SubscriptionLicenseUtil.getLicenseMode()==SUBSCRIPTION` and hostId status ≠ "ok",
  refuses to launch ("license not yet registered") (`StationApp.java:310-318`).
- HostId published as HTTP header `Niagara-HostId` = `platformProvider.getHostId()` (`NiagaraDaemon.java:801-803`).

## §478.4 — The watchdog is liveness, NOT licensing `[CERT]`

`EngineWatchdog.run()` (`EngineWatchdog.java:81-160`) is a lockup detector: cadence = native
`shmem.timeout` (floored 1 s, `:98-101`); compares `engineCycles` between polls; unchanged → `"ENGINE LOCKUP
DETECTED"` (`:117-123`); then (unless policy 1) `Thread.sleep(15000)` and switch on `shmem.policy`
(`:137-156`): **1 = logging only · 2 (default) = KILL VM (`app.kill(0)`) · 3 = REBOOT PLATFORM
(`app.kill(2)` + `queueReboot()`)**. It does not act on license failures.
- **Give-up limit** lives in `NiagaraDaemon.failureReboot()` (`NiagaraDaemon.java:1148-1207`):
  `failureRebootLimit` default **3**, `failureRebootLimitPeriod` default **600000 ms**; counts reboots in the
  window and if `count >= limit` logs `"reboot limit reached"` and returns 0 (stops), else `queueReboot(true)`.

## §478.5 — §14 RECONCILIATION: `com.tridium.niagarad.license.*` EXISTS (retracts my earlier error) `[CERT]`

A 2026-08-24 note in [B477 §477.4] / a pointer added to [B442] claimed "no `com.tridium.niagarad.license.*`
package exists" and "corrected" B442. **That was WRONG** — an unverified negative from an agent that
decompiled only `nre.jar`. First-hand decompile of `niagarad.jar` shows the package EXISTS:
`sources/decompiled/niagarad-ext/com/tridium/niagarad/license/{Brand,Feature,LicenseFile,LicenseManager,
LicenseUtil}.java` (5 classes). It is a **platform-feature** manager with a fixed `FEATURE_WHITELIST`:
`jre8qnx, jre8Qnx7Zulu, jre8J8000Azul, qnx7, globalCapacity, fips140-2, station, stationAzul, brand,
smDeveloperMode, ieee8021x, syslog` (`LicenseManager.java:201-215`); loads `.license`/`.lar` from the
perpetual/subscription dir (`:37-136`); `Feature.isExpired()/check()` gate validity (`Feature.java:33-38`).

**So [B442 §442.3] was correct all along**; B442's pointer and B477 §477.4 are corrected/retracted to point
here. **Two DISTINCT license layers therefore coexist** (this is the reconciled model):
1. **Station JVM** — `baja.jar com.tridium.sys.license.*` (full: features/limits/host/dates, DSA-verified;
   NodeLocked vs Subscription; [B477]).
2. **Platform daemon** — `niagarad.jar com.tridium.niagarad.license.*` (a whitelisted **platform-feature**
   subset: JRE/QNX/station-count/heap/brand/fips/syslog/smDeveloperMode/ieee8021x) that gates whether the
   daemon will START a station at all (§478.3).
The one earlier sub-claim that survives: the daemon sets `System.setProperty("NiagaraDaemon","true")`
(`NiagaraDaemon.java:201`; consumed within niagarad only by `OutputSocket.java:46`), which the STATION-side
`nre.jar RetrieveEntitlements.isLicenseValid()` reads to skip in-process module-signature validation
(delegating it to `baja`).

## §478.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | niagarad spawns `bin/station` via ProcessBuilder with `-daemonspawn:`/`-rp:` | `[CERT]` | `StationApp.java:586-597` | PASS |
| 2 | Exit normalized `>127 -=256`; `isRecoverableError` returns false for −2..−7 | `[CERT]` | `App.java:490-492`; `StationApp.java:622-634` | PASS |
| 3 | −3/−6 → FAILED, NOT restarted ("error is not recoverable") | `[CERT]` | `App.java:522-557` | PASS |
| 4 | Station-count gate `station.limit`→maxRunningStations; "would exceed licensed running station count" | `[CERT]` | `PlatformInfo.java:265-281`; `AppRegistry.java:317-326`; `App.java:219-249` | PASS |
| 5 | heap.limit rewrites −Xmx; subscription hostid gate refuses launch | `[CERT]` | `StationApp.java:334-425,310-318` | PASS |
| 6 | EngineWatchdog = lockup only; policy 1 log / 2 kill VM / 3 reboot | `[CERT]` | `EngineWatchdog.java:81-160` | PASS |
| 7 | failureReboot limit 3 / 600000 ms → "reboot limit reached" gives up | `[CERT]` | `NiagaraDaemon.java:1148-1207` | PASS |
| 8 | `com.tridium.niagarad.license.*` EXISTS (5 classes) + FEATURE_WHITELIST | `[CERT]` | `sources/decompiled/niagarad-ext/com/tridium/niagarad/license/*`; `LicenseManager.java:201-215` | PASS (retracts my 2026-08-24 error; B442 §442.3 stands) |
| 9 | daemon sets `-DNiagaraDaemon=true`; station-side skip-in-process-verify | `[CERT]` | `NiagaraDaemon.java:201`; `nre-ext/…/RetrieveEntitlements.java:246-275` | PASS |

**Tally:** 9 claims, 9 `[CERT]`, 0 `[INFER]`, 0 unmarked. One self-correction (claim 8 reverses a wrong prior claim).

## §478.7 — Connections

- **Closes** B477-G1. **Retracts/corrects** [B477 §477.4] and the intermediate pointer in [B442]; **restores**
  [B442 §442.3] as correct.
- **Builds on** [B477] (station-side managers, `Nre.licenseFailure()`→`System.exit(-3)`), [B387] (feature
  gates), [B392]/[B319] (module signing / `System.exit(-6)`).
- **Deliverable** `docs/niagara-licensing.md` updated (§10) to the reconciled two-layer model.

## §478.8 — Open gaps

- **B478-G1** the native `platformProvider.createWatchdog` / `shmem` shared-memory contract (native side of the
  watchdog cadence/policy) — requires native RE of the platform provider DLL. Deferred.
