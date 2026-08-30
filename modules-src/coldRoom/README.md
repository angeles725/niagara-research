# coldRoom — N4 cold-room control module (`-rt`)

Compiled, signed **`coldRoom-rt`** custom module implementing the design in
[`docs/cold-room-module-design.md`](../../docs/cold-room-module-design.md). It defines
three reusable component TYPEs (one `BColdRoom` instantiated per physical room) plus two
frozen enums.

> **Placeholder to rename:** package `com.oem.coldRoom` and `vendor = "OEM"` are
> placeholders. Rename to the real OEM vendor package/identity before release, and align
> the signing alias with that vendor (`module-best-practices.md` §5.3).

## Status: SKELETON

Configurable slots, defaults/facets, and the slot-only control logic are implemented.
Anything touching physical I/O is marked `// TODO: link to proxy point (BLink, priority
in8)`; N4-API points not 100% verifiable in the corpus are marked `// TODO [INFER]:`.
This is **source authoring only** — not built or signed here (no toolchain).

## File → design-section map

| File | Implements |
|---|---|
| `coldRoom-rt/src/com/oem/coldRoom/BColdRoom.java` | §3.1 equip container; §4.1 hysteresis (BTstat deadband); §4.2 staging map (unit2 = call1 OR call2) |
| `coldRoom-rt/src/com/oem/coldRoom/BEvaporatorUnit.java` | §3.2 per-evaporator actuation, valve-first then evaporator-after-`startDelay` (BBooleanDelay mirror); defrost output hooks |
| `coldRoom-rt/src/com/oem/coldRoom/BDefrostController.java` | §5 defrost sequence (close valve→stop evap→energize resistance; terminate by duration OR resistance temp) + §5.4 interlock state machine (one unit at a time, `staggerDelay` default 4 min) |
| `coldRoom-rt/src/com/oem/coldRoom/enums/BStagingMode.java` | §3.1 `stagingMode` {single, staged} |
| `coldRoom-rt/src/com/oem/coldRoom/enums/BDefrostMode.java` | §5.1 `mode` {interval, schedule} |
| `coldRoom-rt/src/module-include.xml` | Slot-o-Matic `<type>` registry (one per `@NiagaraType`) |
| `build.gradle.kts`, `settings.gradle.kts`, `coldRoom-rt/build.gradle.kts` | gradle-niagara build (`moduleName = coldRoom`, `runtimeProfile = rt`, vendor `OEM`) |

### Conventions copied from the corpus
- **Annotations + generated-slot region + `TYPE = Sys.loadType(...)`**: kitControl
  `BBooleanDelay` (docSource original) and enum `BLoopAction`.
- **Hysteresis algorithm**: `com.tridium.kitControl.hvac.BTstat.calculate()`.
- **Timers**: `Clock.schedule(this, delay, <action>, null)` + hidden `@NiagaraAction`,
  mirroring `BBooleanDelay`.
- **gradle-niagara layout**: devkit `.gradle.kts` templates + `module-include.xml`.
- **Rules applied**: `changed()` filters by slot and never throws; computed outs are
  `TRANSIENT | READONLY`; faulted/null sensor holds state (fail-safe).

## Per-room instantiation (design §1, §4.2)

| Room | `stagingMode` | zone sensors | `BEvaporatorUnit` children | `BDefrostController` |
|---|---|---|---|---|
| 1 | `staged` | zone1 + zone2 | 3 (unit2 runs on OR) | — |
| 2 | `single` | zone1 | 1 | — |
| 3 | `single` | zone1 | 2 (`hasDefrost = true`) | yes |
| 4 | `single` | zone1 | 1 | — |

Units are dynamic children (add per room); parent maps calls by **child order**
(index 0 = unit 1). See the `// TODO [INFER]` on identity-by-order in `BColdRoom`.

## Build → sign → deploy

Follow the runbook [`docs/module-dev-workflow.md`](../../docs/module-dev-workflow.md)
(don't duplicate it). Summary:

1. **Edit** `.java` outside the `BEGIN BAJA AUTO GENERATED` markers; add any NEW type to
   `module-include.xml` in the same edit.
2. **Slot-o-Matic** (only when a `@Niagara*` annotation changed):
   `./gradlew :coldRoom-rt:slotomatic` — regenerates the AUTO region (never hand-edit it;
   the region here is a representative placeholder pending the first real run).
3. **Build + auto-sign**: `./gradlew :coldRoom-rt:build` (niagara-module + niagara-signing;
   keystore at `niagara_user_home/security/keystore.jceks`, alias = vendor).
4. **Deploy + verify**: `./scripts/ng-deploy.sh` (backup → gradlew → copy to
   `STATION_MODULES_DIR` → verify emitted types); restart the station so the registry
   rebuilds.
5. **Commission in the station**: instantiate `BColdRoom` ×4 under `/Config/ColdRooms`,
   add the `equip` tag, configure slots per room, add child `BEvaporatorUnit`s (and
   Room 3's `BDefrostController`), then **BLink** each `valveOut`/`evapOut`/`resistanceOut`
   to its `BBooleanWritable` (priority `in8`) and each `zoneN`/`coilTemp`/`resistanceTemp`
   from its proxy point. Attach history + alarm extensions and confirm the
   `AuditHistoryService` (design §7–§9).

## Not in this skeleton (station-side wiring, per design)
- BLinks to driver proxy points (§6) — done at commissioning, not in module source.
- History extensions (§7), `BAlarmSourceExt` visual alarms (§9), `AuditHistoryService`
  confirmation (§8) — attached to points/station, not slots of these types.
- `module.palette` (drag-and-drop) — recommended for a component module
  (`module-best-practices.md` §1.1.8); add before release.
