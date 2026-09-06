# Client issue draft — dashboard panel writes with no consumer at PANCCADIA (dead writes)

Author: companero (Fable), 2026-09-06. Drafted for the lead to surface to Cristian and file on `niagara-panccadia-leon`
when he says so. Evidence = a REAL read of the deployed station graph (`/mnt/c/Users/equipo/Niagara4.14/OptimizerSupervisor/
stations/PANCCADIA/config.bog`, 35094 bytes, 2026-09-04 16:03) with `tools/bog-nav.py`, plus the client source at `a109249`.
This CORRECTS the PR11 rows package (`…-c9-pr11-write-path-matrix-rows.md`, class B): the finding is per room, not blanket.

---
## Title
`HMI writes that reach no logic: Cuarto3.intercambiadorMode and coolOnSensorFault (all rooms) have no outgoing link`

## Summary
Two OPERATOR slots on `BRoomPanel` are rendered as controls in the HMI, accept the write (200, value stored on the
facade), and change NOTHING in the station logic because no link leaves the facade slot:
- `Cuarto3.intercambiadorMode` — the HMI shows an "Intercambiador" HOA row for Cuarto 3 (`rc/index.html:1445`); the bog has
  NO link from `Services/DashboardService/Cuarto3.intercambiadorMode`.
- `coolOnSensorFault` — on every room's config card (`rc/index.html:1740`, `SP_GENERAL`), exported by `DashboardReader`
  (`BOOL_CONFIG_SLOTS` :105), and the source comment promises "Link a BColdRoom.coolOnSensorFault" (`BRoomPanel.java:148`);
  the bog has NO link from `CuartoN.coolOnSensorFault` in ANY of the five rooms.
The three other slots I first flagged (`comp1Mode`, `comp2Mode`, `fanMode`) are NOT dead: they are Cuarto5-only by design
(`DashboardReader.java:128-130`, HMI rows only for Cuarto5 at `index.html:1411-1413`) and ARE linked there. On Cuarto1-4
they exist as facade slots but the HMI never renders them, so no operator can write them; only Workbench/oBIX could.

## Evidence (bog-nav; `--slot` matched EITHER link end at the time, so `--slot fanMode` also listed links whose TARGET is `…/EvaporatorUnit_N.fanMode` — the `fanMode` rows were therefore checked in `--csv` by the exact `Room.slot,` key; bog-nav is now endpoint-aware)
```
$ python3 tools/bog-nav.py "$BOG" find --type dashboardPan:RoomPanel --csv
44d51,Services/DashboardService/Cuarto1,DPCD:RoomPanel   … Cuarto2 44d60 · Cuarto3 44d6f · Cuarto4 44d7e · Cuarto5 44d8d
$ python3 tools/bog-nav.py "$BOG" links --from Cuarto1 --slot setpoint          # positive control: the tool resolves links
Services/DashboardService/Cuarto1.setpoint  -->  Programacion/ColdRoom_1.setpoint
$ for r in 1 2 3 4 5; do links --from Cuarto$r --slot intercambiadorMode; done
Cuarto1..Cuarto5.intercambiadorMode: (no matching links)                        # Cuarto3 is the one with an HMI control
$ for r in 1 2 3 4 5; do links --from Cuarto$r --slot coolOnSensorFault; done
Cuarto1..Cuarto5.coolOnSensorFault: (no matching links)                          # all five rooms
$ links --from Cuarto5 --csv | grep -E 'Cuarto5\.(fanMode|comp1Mode|comp2Mode),'  # the Cuarto5 trio IS wired
Link,Services/DashboardService/Cuarto5.fanMode,Programacion/ColdRoom_5/EvaporatorUnit.fanMode,True,False
Services/DashboardService/Cuarto5.comp1Mode  -->  Programacion/ColdRoom_5/EvaporatorUnit.valveMode
Services/DashboardService/Cuarto5.comp2Mode  -->  Programacion/ColdRoom_5/EvaporatorUnit2.valveMode
$ links --from Cuarto{1,2,3,4} --csv | grep -c 'Cuarto[1-4]\.(fanMode|comp1Mode|comp2Mode),'   → 0 (never rendered on 1-4)
$ links --to Cuarto1 --slot <each of the five>                                    → (no matching links)  # nothing feeds them either
```
Source facts (`a109249`): `BRoomPanel.java:257-262` `fanMode/comp1Mode/comp2Mode/intercambiadorMode` = `double`,
`SUMMARY|OPERATOR|TRANSIENT` (TRANSIENT: not persisted across a station restart — a dead write is also a LOST write);
`:148-154` `coolOnSensorFault` = `boolean`, `SUMMARY|OPERATOR` (persisted, so the stale value survives restarts while
doing nothing). Consumers: `DashboardReader.java:105,130,133` (export only, no logic); no Java reader outside the reader.

## Impact
- An operator toggling "Intercambiador" on Cuarto 3 or "Enfriar si falla sensor" on any room sees the control accept the
  value and NOTHING happens in the room. Silent — the exact failure class the C9 silent-protection work exists to remove.
- `coolOnSensorFault` is a protection policy (cool on sensor fault to protect product). The logic module has its own
  `coolOnSensorFault` slot (ColdRoomPan-rt) which is what actually runs; today it can only be changed from Workbench.

## Both surfaces write them dead (second read by investigador1, verified)
The tunnel write-server (`instalacion/pipeline/write-server.mjs` @ 9acb47c) lists BOTH slots in `WRITABLE`
(`intercambiadorMode: AUTOOFF` `:93`, `coolOnSensorFault: BOOL` `:94`) and PUTs them to `${FACADE_PATH}/CuartoN/<slot>` (`:269`).
So a viewer write is accepted, audited to `change_log`, and — with no link on the facade — changes nothing; for
`intercambiadorMode` (TRANSIENT) it is also lost on restart. Same dead-write class from surface A.

## The intercambiador has NO station-side control to link to (closed by investigador1 from the same bog)
`Programacion/ColdRoom_3` (`CRP:ColdRoom`) contains only `EvaporatorUnit_1`, `EvaporatorUnit_2`, `DefrostController`;
a whole-bog grep for `intercamb|exchanger` finds nothing but the unrelated `hx` web module; the client source at a109249
(ColdRoomPan-rt, CompPan-rt) has 0 files mentioning it. The facade javadoc says the link was meant to be made by the
integrator ("El integrador linkea este mode al control del intercambiador en la station", `BRoomPanel.java:261`) to a
control that was never created; `intercambiadorState` (`:280`, `BStatusBoolean`) is unlinked too, so the HMI LED
(`index.html:1445-1446`) always shows the default.

## Exits
**`coolOnSensorFault` (all five rooms)** — the logic slot exists (`Programacion/ColdRoom_N.coolOnSensorFault`); the link was
always intended. Exit: **wire it station-side** (Workbench, no code): `CuartoN.coolOnSensorFault → Programacion/ColdRoom_N.coolOnSensorFault`,
N = 1..5; goes on the station-change list with the defrost trial. Structural pin afterwards: `bog-nav links --from CuartoN
--slot coolOnSensorFault` resolves for every room (PR11 row → class A).
**`Cuarto3.intercambiadorMode`** — a "wire it" exit does not exist by itself: there is nothing to link to. The real exits:
1a. **Create the station-side control first** (a writable relay/proxy point for Cuarto 3's intercambiador, like the 22
    relays — a STATION + WIRING question), then link `Cuarto3.intercambiadorMode` (and `intercambiadorState` back) to it.
    Precondition, for Cristian: **is the intercambiador physically on any Niagara output at PANCCADIA?**
2.  **Display-only / drop the control**: remove the HMI HOA row (`index.html:1445`) and the `WRITABLE` entry (`write-server.mjs:93`),
    or drop `OPERATOR` from the facade slot (schema-risk SAFE flag change, B795). Keep `intercambiadorState` only if a
    real state source will feed it; otherwise remove the LED too (today it lies by showing the default).
Recommendation: ask Cristian the physical question before choosing; if the answer is "no output", exit 2.

## Acceptance
- bog-nav: `links --from CuartoN --slot coolOnSensorFault` resolves for N=1..5.
- intercambiador: `links --from Cuarto3 --slot intercambiadorMode` resolves to a real control (exit 1a) — or the HMI row, the write-server `WRITABLE` entry and the OPERATOR flag are gone and `intercambiadorState` has a source or is removed (exit 2).
- `bog-audit.sh` CHECK7 (dangling) stays 0; write-path matrix rows for both slots updated from ❌ to the chosen exit.

## Not in scope
`comp1Mode`/`comp2Mode`/`fanMode` on Cuarto1-4: unreachable from the HMI, harmless; note only. The PR11 package's class B
list is corrected by this file (8 B rows → 2 dead + 3 Cuarto5-wired + 3 local limits).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | no link from Cuarto3.intercambiadorMode / CuartoN.coolOnSensorFault (N=1..5) | [CERT] | bog-nav links on the 2026-09-04 config.bog, positive control passed |
| 2 | Cuarto5 fan/comp1/comp2 linked; Cuarto1-4 never render them | [CERT] | bog-nav --csv exact keys; index.html:1411-1413; DashboardReader.java:128-130 |
| 3 | HMI renders intercambiador (Cuarto3) and coolOnSensorFault (all rooms) | [CERT] | index.html:1445, :1740 |
| 4 | slot flags / TRANSIENT | [CERT] | BRoomPanel.java:148-154, :257-262 @ a109249 |
| 5 | bog-nav `--slot` matched either link end (NOT substring — my first diagnosis was wrong) | [CERT, tool caveat] | bog-nav.py:447 compared names exactly; the hits were target-end `…fanMode`; fixed: endpoint-aware `--slot` + `--slot-any` |
| 6 | no intercambiador control exists in the station or the logic source | [CERT] | investigador1 second read: bog tree of ColdRoom_3, whole-bog grep, client grep @ a109249; BRoomPanel.java:261 javadoc |
| 7 | both slots are in the write-server WRITABLE map | [CERT] | write-server.mjs:93-94, :269 @ 9acb47c (coolOnSensorFault included — corrects the second read's "HMI-only") |
