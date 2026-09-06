# C9 — Air-defrost STATION trial for rooms 1/2/4: the exact per-unit link list (bog-nav, read-only)

Author: companero (Fable), 2026-09-06. Scope per C9 proposal §2.4: a **STATION-ONLY client trial** — the OR of
`evapOut`/`resistanceOut` into each unit's FAN relay via links, no module change (the `airDefrost` module flag stays a
deferred seed S16). Produced with `tools/bog-nav.py` on the PANCCADIA `config.bog` (read-only). **Nothing here is
applied — it waits for Cristian's green light**, then it is done in Workbench on the station after a `station-snapshot.sh`
pre-snapshot. `[ev: proposal §2.4]` `[ev: corpus S16]` `[ev: bog-nav PANCCADIA config.bog, read 2026-09-06]`

## 1. The reference pattern — Cuarto3 / `Programacion/ColdRoom_3` (electric defrost, the only room wired today)
- `hasDefrost = true` on BOTH units (`EvaporatorUnit_1`, `EvaporatorUnit_2`); a `CRP:DefrostController` sibling exists
  (`Programacion/ColdRoom_3/DefrostController`, **h:44b84**) — CHECK16 requires the pair in BOTH directions.
- Fan: `EvaporatorUnit_N.evapOut → Drivers/NrioNetwork/io34_1_6/points/ro1.in2` / `ro2.in2` (fan relays).
- Heat: `EvaporatorUnit_N.resistanceOut → …/io34_1_6/points/ro5.in2` / `ro6.in2` (SEPARATE resistance relays).
- So Cuarto3 does NOT OR the two into the fan — it has a physical heater. It is the reference for the *defrost wiring*
  (hasDefrost + DefrostController + the two outputs linked), not for the OR.
- `fanMode` (HOA, TRANSIENT `tsoL`) = auto; `fanRunMode = runOnDelay`. Fallback: `ro1` has one; `ro2..ro6` have NONE.
`[ev: bog-nav slot/links/relays/find on ColdRoom_3]` `[ev: retro campaign8-station-logic CHECK16]`

## 2. Rooms 1/2/4 today (why the trial is needed)
| Room | Units (physical) | `hasDefrost` | `DefrostController` | `resistanceOut` linked? | Fan relay (h) | fallback |
|---|---|---|---|---|---|---|
| ColdRoom_1 | EvaporatorUnit_1, _2, _3 | ABSENT in bog (= default false) | **none** | **no** | ro1 (h:44966), ro2 (h:44968), ro3 (h:4496a) | ro1 yes · ro2/ro3 **NO** |
| ColdRoom_2 | EvaporatorUnit | ABSENT | **none** | **no** | ro7 (h:44972) | **NO** |
| ColdRoom_4 | EvaporatorUnit | ABSENT | **none** | **no** | ro9 (h:44976) | **NO** |
All five units: `fanMode` = auto (≠ OFF, ✓), `fanRunMode = runOnDelay`. Every fan relay is `Drivers/NrioNetwork/io34_5_2/points/roN`,
fed today by ONE link `EvaporatorUnit.evapOut → roN.in2`. These are AIR-defrost rooms: no heater, no resistance relay — so
during a defrost phase the fan itself must keep running to melt frost with room air. `[ev: bog-nav rooms 1/2/4]`

## 3. Why a plain second link is NOT an OR (the one design fact that decides the shape)
A `BooleanWritable` priority array picks the HIGHEST-priority NON-NULL input (in1 … in16 + relinquish); it is not a
logical OR. With `evapOut → in2` and `resistanceOut → in3`, a defrost phase gives `evapOut=false` (non-null) at in2, which
WINS over `resistanceOut=true` at in3 → the fan would stop exactly when it must run. The OR therefore needs a
`kitControl:Or` block per unit. The bog has **no `kitControl:Or` today** (it does use `kitControl:And` in `Drivers/CODIGOS`,
so the module is installed). `[ev: corpus B805 §805.11 (priority array semantics)]` `[ev: bog-nav find --type kitControl:Or → none; grep kitControl:And → present]`

## 4. THE LINK LIST — per room, per unit (apply in this order, one unit at a time, verify after each)
Notation: `U` = the unit path, `RO` = its fan relay, `OR` = the new `kitControl:Or` placed beside the unit (name `FanOr_<unit>`).

### Per ROOM first (once per room, before the units)
| Room | Step |
|---|---|
| ColdRoom_1 / _2 / _4 | **R.1** Add a `CRP:DefrostController` sibling under `Programacion/ColdRoom_N` (drag from the ColdRoomPan palette), named `DefrostController`, and copy the CONFIG of `ColdRoom_3/DefrostController` (h:44b84: `mode`, `interval`, `duration`, `staggerDelay`, `terminateOnResistanceTemp=false` — no resistance sensor in an air room). Without it `hasDefrost=true` is a CHECK16 FAIL and no defrost cycle ever fires. |

### Per UNIT (5 units)
| # | `U` | `RO` (fan relay) | Links to ADD | Link to REMOVE | Slots to SET |
|---|---|---|---|---|---|
| 1 | `Programacion/ColdRoom_1/EvaporatorUnit_1` | `Drivers/NrioNetwork/io34_5_2/points/ro1` (h:44966) | `U.evapOut → OR.inA` · `U.resistanceOut → OR.inB` · `OR.out → RO.in2` | `U.evapOut → RO.in2` (replaced by `OR.out`) | `U.hasDefrost = true` · `RO.fallback = false` (exists: set its value false) · confirm `U.fanMode ≠ OFF` |
| 2 | `Programacion/ColdRoom_1/EvaporatorUnit_2` | `…/io34_5_2/points/ro2` (h:44968) | same three | `U.evapOut → RO.in2` | `hasDefrost = true` · **ADD `RO.fallback`** (none today) `= false` · `fanMode ≠ OFF` |
| 3 | `Programacion/ColdRoom_1/EvaporatorUnit_3` | `…/io34_5_2/points/ro3` (h:4496a) | same three | `U.evapOut → RO.in2` | `hasDefrost = true` · ADD `fallback = false` · `fanMode ≠ OFF` |
| 4 | `Programacion/ColdRoom_2/EvaporatorUnit` | `…/io34_5_2/points/ro7` (h:44972) | same three | `U.evapOut → RO.in2` | `hasDefrost = true` · ADD `fallback = false` · `fanMode ≠ OFF` |
| 5 | `Programacion/ColdRoom_4/EvaporatorUnit` | `…/io34_5_2/points/ro9` (h:44976) | same three | `U.evapOut → RO.in2` | `hasDefrost = true` · ADD `fallback = false` · `fanMode ≠ OFF` |

**Do NOT touch** the dashboard state links (`U.evapOut → Services/DashboardService/CuartoN.evapKFanState`) — they are
read-only mirrors. **Known separate issue, do not "fix" here:** on ColdRoom_1 the dashboard tiles are CROSSED (unit 1 ↔
tile evap3, unit 3 ↔ tile evap1, CHECK18) — the relay mapping above is by PHYSICAL unit (`EvaporatorUnit_1 → ro1`), which
is what the fan needs; the tile crossing is a DashboardPan link fix for another change. `[ev: bog-nav tiles]`

**Why `fallback = false`:** a writable fed by an own-module output with NO explicit fallback HOLDS its last state on a
station stop/reload — a fan (or resistance) left ON with no controller (B810, CHECK11: 17 such relays on PANCCADIA today).
`false` = de-energize when the source goes null. `[ev: corpus B810 §810.8]` `[ev: bog-nav relays — 17 NO fb]`

## 5. Verify after apply (read-only, bog-nav on a fresh snapshot)
```
bog-nav.py <config.bog> links --from ColdRoom_1 --slot resistanceOut      # 3 links → FanOr_*.inB
bog-nav.py <config.bog> links --to  io34_5_2/points/ro1 --slot in2         # ONE source: FanOr_EvaporatorUnit_1.out
bog-nav.py <config.bog> relays                                            # ro1/ro2/ro3/ro7/ro9 → "yes fb"
bog-nav.py <config.bog> find --type CRP:DefrostController                 # 4 (rooms 1,2,3,4)
bog-nav.py <config.bog> slot Programacion/ColdRoom_1/EvaporatorUnit_1 hasDefrost   # = true
bog-audit.sh <config.bog> --module ColdRoomPan                            # CHECK16 clean; CHECK14 no longer flags resistanceOut; CHECK13 no double-source
```
Expected bog-audit deltas: CHECK11 FAIL count drops by 4 (ro2/ro3/ro7/ro9 gain a fallback); CHECK14 own-output-unlinked
WARN drops by 5 (`resistanceOut` now linked); CHECK16 stays clean (pairs present); CHECK13 must NOT fire (the Or is ONE
source into `in2`). Rollback = delete the five `FanOr_*` blocks and re-link `U.evapOut → RO.in2`; `hasDefrost`/fallback are
harmless to leave. `[ev: retro campaign8-station-logic CHECK13/14/16]`

## 6. Green-light gate
Waits for Cristian. Then: `station-snapshot.sh` pre-snapshot → apply ONE room (ColdRoom_2, single unit, lowest blast radius)
→ verify §5 → observe one defrost cycle (fan stays ON while `resistanceOut` is true, `evapOut` false) → then rooms 1 and 4.

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Cuarto3 pattern: hasDefrost=true + DefrostController h:44b84 + evapOut/resistanceOut on separate relays | [CERT] | bog-nav slot/links/find |
| 2 | Rooms 1/2/4: no DefrostController, hasDefrost absent, resistanceOut unlinked, fan relays ro1/2/3/7/9 with handles | [CERT] | bog-nav tree/links/relays/slot |
| 3 | ro1 has a fallback; ro2/ro3/ro7/ro9 have none | [CERT] | bog-nav relays |
| 4 | priority array ≠ OR (highest non-null wins) → kitControl:Or needed; none in the bog | [CERT-doc / CERT] | B805 §805.11; bog-nav find |
| 5 | expected bog-audit deltas (CHECK11 −4, CHECK14 −5, CHECK16 clean, CHECK13 silent) | [INFER] | by construction; confirmed at verify |
