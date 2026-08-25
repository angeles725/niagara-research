# Block 488 — The license limit-enforcement map: two tiers (global-capacity `Metrics` vs per-network driver Feature), what each `*.limit` does on exceed (System.exit(-3) / hard block / silent block-add / component fatalFault), and a §14 correction of B14's counting API

> **Focus:** `licensing`. **Question (operator):** qué toma en cuenta / cómo se aplica cada límite. Completes
> [B14]/[B387] in breadth. READ-ONLY, decompiled vineflower; no binary run. Markers §3.
>
> **Sources:** `organized/baja/…/com/tridium/sys/metrics/{Metrics,GlobalGroup,SubGroup,Group}.java`,
> `…/resource/ResourceManager.java`, `…/station/Station.java`, `javax/baja/sys/BLink.java`,
> `driver/driver-rt/…/{BDeviceNetwork,BDevice,point/BProxyExt}.java`, `schedule-rt/…/BCompositeSchedule.java`,
> `history-rt/…/db/{LocalDbConnection,ConfigIndex}.java`, `platform-rt/…/daemon/PlatformStationManager.java`,
> `analytics-rt/…/{BAnalyticService,license/NAFFeatureUtil}.java`, `maxpro-rt/…/BMaxproCamera.java`.

## §488.1 — Two enforcement tiers `[CERT]`

- **Tier 1 — global capacity (station-wide):** `com.tridium.sys.metrics.Metrics` holds static counters;
  `GlobalGroup` reads feature `tridium:globalCapacity` at construction. Counter methods return a non-null
  "badGroups" String (network/device/point) or `false` (link/history/schedule) on exceed.
- **Tier 2 — per-network driver Feature:** `BDeviceNetwork.checkLicenseLimit()` reads every `*.limit` from the
  driver's OWN Feature, tracked per-network (`fw(501,…)`).

## §488.2 — Enforcement table `[CERT]`

| Limit | Reader (file:line) | Scope | Exceed behavior |
|---|---|---|---|
| `point.limit` (global) | `GlobalGroup:30`→`Metrics.incrementPoint:118-143`; `BProxyExt.fwStarted:560`→`checkFatalFault:285` | station | component fatalFault "Exceeded point limit…" (station keeps running) |
| `point.limit` (per-net) | `BDeviceNetwork.checkLicenseLimit:366-381`; `BProxyExt:327` `fw(501,"point.limit")` | per-network | component fatalFault |
| `device.limit` | `GlobalGroup:29`→`incrementDevice:89-108`; `BDevice.fwStarted:331`; per-net `BDevice:261` | station / per-net | component fatalFault |
| `network.limit` | `GlobalGroup:28`→`incrementNetwork:54-73`; `BDeviceNetwork.checkLicense:306-313` | station | network fatalFault + faultCause |
| `link.limit` | `GlobalGroup:31`→`incrementLink:153-168`; `BLink.activate:169` | station | `fatalFault=true` + log.severe; `BNotification` "Exceeded Link Limit" at limit+1; link NOT blocked |
| `history.limit` | `GlobalGroup:32`→`incrementHistory:178-193`; `LocalDbConnection.doCreateHistory:168`, `ConfigIndex:204` | station | **silent block-create** at runtime (no exception); on reload `ConfigIndex:205` logs severe + `it.remove()`; excludes Log/Audit history |
| `schedule.limit` | `GlobalGroup:33`→`incrementSchedule:203-220`; `BCompositeSchedule.fwStarted:169` | station | fatalFault + faultCause "Exceeded schedule limit…" |
| `heap.limit` | `ResourceManager.checkLicense:86-103` (globalCapacity, MB vs JVM maxMemory) | station | **`System.exit(-3)`** "STATION IS UNLICENSED!!!" |
| `resource.limit` | `ResourceManager.checkLicense:109-114` (only when NOT capacity-licensing) | station | `ResourceReport.platStationFault`→`Station.setStationFault` |
| station feature (`station`/`stationAzul`) | `Station.checkLicense:214-230` | station | **`System.exit(-3)`** "FATAL: Not licensed to run a station" |
| station COUNT (maxStations) | `PlatformStationManager.createStation:51-52` vs daemon `getMaxStations()` | platform/daemon | **`BajaRuntimeException("Maximum number of stations exceeded")`** — blocks creation ([B478] resolves here) |
| `algorithm.limit` / `alert.limit` / `proxyext.limit` | `BAnalyticService:1689-1716` via `NAFFeatureUtil.getLimit` | analytics service | `configFail` + `steady=false` + log.severe |
| `camera.limit` | `BMaxproCamera.videoCameraStarted:310` `fw(501,"camera.limit")` | per-network | `configFatal` |
| `zone.limit` | `EasyHealthyBuildingLicenseUtil:332` (Honeywell Alerton) | service | trial default "5"; enforced in that module |
| `driverCapacity*` sub-limits | `SubGroup:19-21` + `Metrics.findSubGroup:286`,`loadSubGroups:298-311` | per module-group | same fatalFault path |

**[CERT negative] — present in license data but NO numeric enforcement site:** `foxStream` (Fox has only
`preAuthFrameSizeLimit` config + `fox.maxServerSessions` warning at `Tuner:349`, neither a licensed capacity).
Any `*.limit` not in globalCapacity/driverCapacity/analytics/camera/zone is read generically only if it ends in
`.limit` inside a driver Feature (`BDeviceNetwork:334`), else unread.

## §488.3 — Fatal vs soft `[CERT]`

- **`System.exit(-3)`**: `heap.limit` exceeded; station feature not licensed. (`System.exit(-5)` on station-load
  failure, `Station:210`.)
- **Hard block (exception)**: station COUNT (`BajaRuntimeException`, daemon).
- **Silent block-add**: history create at runtime.
- **Component fatalFault (station survives)**: point, device, network, link, schedule, camera, analytics dims.
- **Uncapped if absent**: `globalCapacity` feature absent → `Group.parseLimit` null→`MAX_VALUE` → all uncapped
  ([B387 §387.4]).

## §488.4 — Counting model — §14 CORRECTION of [B14] `[CERT]`

- **[B14]'s `NLicenseManager.getPointCount/getDeviceCount/getHistoryCount/getScheduleCount` API is GONE in this
  build.** Counters live in `Metrics` as static `getGlobalPointsUsed/DevicesUsed/NetworksUsed/LinksUsed/
  HistoriesUsed/SchedulesUsed` (`Metrics.java:75-237`). (B14 gets a pointer to here.)
- **Trigger = increment-on-start** (component lifecycle `fwStarted()` for network/device/point/schedule,
  `BLink.activate()`, history `doCreateHistory()`); **NO decrement-on-remove.**
- **Periodic full recount:** `Metrics.Recount` thread (`:418-609`) walks the whole station tree every
  **300 000 ms**, zeroes buckets, re-counts (histories via BQL `select count(*)`), atomically swaps — this is
  how removals reconcile.

## §488.5 — Two divergent `parseLimit` semantics (load-bearing) `[CERT]`

- `Group.parseLimit` (`Group.java:16-24`, GlobalGroup/SubGroup): `null`→`MAX_VALUE`, `"none"`→`MAX_VALUE`, else
  int. **Absent key = uncapped.**
- `LicenseUtil.parseLimit` (`LicenseUtil.java:590-602`, systemDb/index): `null`→**`0` (blocks!)**, `"none"`→
  `MAX_VALUE`.
- `NAFFeatureUtil.getLimit` (analytics, `:22-32`): `null`/empty/`*`/`none`→`MAX_VALUE`.
The same missing attribute therefore means "unlimited" in capacity metrics but "zero/blocked" in the systemDb
path — a subtle, deliberate divergence.

## §488.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Two tiers: Metrics/GlobalGroup (globalCapacity) + per-net BDeviceNetwork.checkLicenseLimit | `[CERT]` | `GlobalGroup:28-33`; `BDeviceNetwork.java:366-381` | PASS |
| 2 | heap.limit + station feature → System.exit(-3); station count → BajaRuntimeException | `[CERT]` | `ResourceManager.java:86-103`; `Station.java:214-230`; `PlatformStationManager.java:51-52` | PASS |
| 3 | history over-limit = silent block-create; point/device/network/link/schedule/camera = component fatalFault | `[CERT]` | `LocalDbConnection.java:168`; `BProxyExt.java:285`; `BLink.java:169` | PASS |
| 4 | foxStream has NO license enforcement site | `[CERT negative]` | grep; `Tuner.java:349` (config only) | PASS |
| 5 | §14: NLicenseManager count API gone; counters in Metrics; increment-on-start, no decrement, 300s Recount | `[CERT]` | `Metrics.java:75-237,418-609` | PASS (corrects B14) |
| 6 | Two parseLimit semantics: Group null→MAX (uncapped) vs LicenseUtil null→0 (blocks) | `[CERT]` | `Group.java:16-24`; `LicenseUtil.java:590-602` | PASS |

**Tally:** 6 claims, all `[CERT]`/`[CERT negative]`, 0 unmarked. One §14 correction (B14 counting API).

## §488.7 — Connections

- Completes the limit picture over [B14] (counting — corrected here) and [B387 §387.4] (uncapped-if-absent).
- Feeds `docs/niagara-licensing.md` §7. B14 edited with a pointer to §488.4.
