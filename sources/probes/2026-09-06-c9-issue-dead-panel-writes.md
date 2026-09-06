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

## Two exits (pick one per slot; both are station-side or facade-side, no logic change)
1. **Wire the links station-side** (config.bog edit in Workbench, no code): `Cuarto3.intercambiadorMode → <the
   intercambiador's mode slot in Programacion/ColdRoom_3>` and `CuartoN.coolOnSensorFault → Programacion/ColdRoom_N.coolOnSensorFault`
   (N = 1..5). Then `bog-nav links --from CuartoN --slot coolOnSensorFault` must resolve for every room; add that as the
   PR11 structural pin (class A row). Requires: confirm the intercambiador mode slot exists on ColdRoom_3 (I did not find
   its target; it may be a relay/HOA on the IO tree — `bog-nav hoa --module` at apply).
2. **Demote the slots** (code, DashboardPan-rt + -ux): drop `OPERATOR` (or remove the HMI control) for
   `intercambiadorMode` and `coolOnSensorFault` on the facade so the HMI cannot offer a write that goes nowhere. This is a
   schema-risk SAFE flag change (B795) but removes a feature the HMI already shows — only acceptable if the feature is
   not wanted.
Recommendation: exit 1 for `coolOnSensorFault` (the link was always intended — the source comment says so); for
`intercambiadorMode`, ask Cristian whether Cuarto 3's intercambiador is meant to be HMI-controlled (exit 1) or
display-only (exit 2, keep `intercambiadorState`).

## Acceptance
- bog-nav: `links --from CuartoN --slot coolOnSensorFault` resolves for N=1..5; `links --from Cuarto3 --slot intercambiadorMode` resolves (exit 1) — or the HMI no longer renders the control and `module-find slots --flags o` no longer lists it (exit 2).
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
| 6 | intercambiador target slot on ColdRoom_3 | [UNVERIFIED] | find at apply |
