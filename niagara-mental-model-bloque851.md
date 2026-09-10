# Block 851 — HARBOR GreenMAX lighting: the VERIFIED control chain + live Route B pilot on GM02 (corrects B841/B846/B847)

> **Focus:** harbor-greenmax-lighting. **Scope:** close the requires-execution gaps of the Route B redesign
> by BUILDING and CUTTING OVER one live circuit, and record the control chain that the design blocks
> (B841/B846/B847) had only inferred. This block is the empirical floor the earlier design blocks lacked.
>
> **Evidence — three tiers, kept distinct:**
> - `[CERT-live]` = observed on the LIVE stations this session (2026-09-10) in Workbench: the supervisor
>   **HM_BMS** (`DESKTOP-A7JMVTF`) and the JACE **HM_Central** at **192.168.1.102**. Gate link acceptance,
>   HOA output values, the cutover with no relay flicker, `HorR1=On`, and the live Relation Sheet of `HorR1`.
> - `[CERT]` = verbatim from the two station `config.bog` BACKUPS (`clients/distech-merida-harbor/`,
>   HM_BMS 08-09-2026 + HM_Central 07-09-2026) read with `tools/bog-nav.py`, and from decompiled `kitControl`
>   / `control` source (`organized/`). Backups are disk snapshots, NOT the running system — structural, not runtime.
> - `[INFER]` = the campaign projection (per-circuit replication, time estimate).
>
> SECRETS DISCIPLINE: Host IDs / credentials cited as structure only. This block records a REAL live change:
> GM02 circuit 1 was cut over from its legacy schedule to the new scheme on the production station.

---

## 1. The real current control chain — per circuit, uniform GM02–GM16 `[CERT]`

Traced in both backups (bog-nav) and confirmed live (Relation Sheet). For circuit `k` of panel `GMxx`:

```
SUPERVISOR (HM_BMS):
   GMxxilum/Rk        (sch:BooleanSchedule, per-circuit)  .out
      → GMxxilum/HorRk (c:BooleanWritable) .in10
   (path: Drivers/NiagaraNetwork/HM_Central/points/GMxxilum/{Rk,HorRk})
        │  the JACE reads HorRk over Fox via its own proxy
JACE (HM_Central 192.168.1.102):
   HorGMxx/HorRk      (c:BooleanPoint + nd:NiagaraProxyExt, pointId → supervisor GMxxilum/HorRk) .out
      → BcpBacnetNetwork/GMxxilum/points/Relay[k].BO (c:BooleanWritable + bac:BacnetBooleanProxyExt) .in10
   (Relay[k].BO objectId = binaryOutput:k)
        │  BACnet write
   physical relay
```

- Handles (HM_BMS backup): `GM02ilum/HorR1` = `h:6a4d5`; its feeder `GM02ilum/R1` = `h:6a35f` (a
  `sch:BooleanSchedule`, NOT a plain writable — one dedicated schedule per circuit, ~349 total, exactly the
  premise B841 asserted).
- The feeder link is uniform: `GMxxilum/Rk.out → GMxxilum/HorRk.in10` for every panel GM02..GM16
  (verified by the global link dump).
- The insertion seam is **`HorRk.in10` on the SUPERVISOR**. Everything downstream (proxy → JACE → BACnet)
  is untouched — this CONFIRMS B841's thesis "the redesign lives on the Supervisor; JACE↔BACnet untouched".

## 2. Two-station topology + a methodology correction `[CERT]`/`[CERT-live]`

`HorGMxx/HorRk` on the JACE is a `NiagaraProxyExt` whose `pointId` is the supervisor path
`slot:/Drivers/NiagaraNetwork/HM_Central/points/GMxxilum/HorRk` — the JACE reads the supervisor's `HorRk`
and its `.out` links to the BACnet relay's `in10`. Live Relation Sheet of `HorGM02/HorR1` shows exactly one
link: `out → BcpBacnetNetwork/GM02ilum/points/Relay[1].BO.in10` `[CERT-live]`.

**Methodology note (recorded so a later pass does not repeat it):** `bog-nav links --to h:<x>` / `--from h:<x>`
returned `(no matching links)` for links that DO exist, briefly producing the false conclusion "the chain does
not exist". The `--slot-any` global dump found them. Root cause: `--to/--from` matched the needle only against
PATHS, never a handle. Fixed 2026-09-10 (§8). Live Workbench > a buggy tool read — the correct source order
caught the error.

## 3. Gate B847-G1 — CLOSED: Numeric→Enum links directly `[CERT-live]`

`kitControl:BBooleanSelect.select` is a `BStatusEnum`; `NumericConst/NumericWritable.out` is a
`BStatusNumeric`. In Workbench on this **4.3** station, linking `SelR1.out → SchedMux1.select` is **accepted
directly** — the `select` slot showed the numeric value with no conversion block requested. **No `Numeric→Enum`
converter is needed** for the whole campaign. This closes the single highest-risk unknown of Route B (B847-G1,
B844-G1).

## 4. The corrected control model — HOA per HORARIO, 5 horarios `[CERT-live]` (supersedes B847 §2)

Client requirement (2026-09-10): the **Auto/ON/OFF lives on each of the 5 master horarios**, NOT per circuit;
each circuit only **picks which horario it obeys**. This SUPERSEDES B847's per-circuit `HoaMux` and its
4-horario `numberValues=4`.

```
SHARED, once for the site — for each horario n = 1..5:
   Horarion (sch:BooleanSchedule) → Horn_HOA.inA        (1 = Auto: follow the program)
   ConstTRUE  (constants:BBooleanConst) → Horn_HOA.inB  (2 = ON : force on)
   ConstFALSE (constants:BBooleanConst) → Horn_HOA.inC  (3 = OFF: force off)
   Horn_Mode  (constants:BNumericConst) → Horn_HOA.select
   Horn_HOA (util:BBooleanSelect, numberValues=3) .out = "effective horario n"

PER CIRCUIT k (~one per interruptor):
   Hor1_HOA.out .. Hor5_HOA.out → SchedMuxk.inA..inE
   SelRk (constants:BNumericConst, 1..5) → SchedMuxk.select
   SchedMuxk (util:BBooleanSelect, numberValues=5) .out → HorRk.in10
```

Consequence for effort: the HOA is centralised to **5 instances** instead of one per circuit; each circuit is
now just **SchedMux + SelR** (~2 objects). The `select` link uses the verified direct Numeric→Enum (§3).
Every `SelRk` is independent → each interruptor chooses its horario without affecting the others.

## 5. Live pilot — GM02 circuit 1, built and cut over `[CERT-live]`

Built on the supervisor under `Config/Iluminacion/`: `Horarios/` (Horario1..5 + Hor1..5_HOA + Hor1..5_Mode +
ConstTRUE/ConstFALSE) and `GM02/` (SchedMux1 `numberValues=5`, SelR1). Observed:
- HOA: `Horn_Mode=2` → `Horn_HOA.out=true`; `=3` → `false`; `=1` → follows the schedule. `[CERT-live]`
- Selection: `SelR1` 1..5 makes `SchedMux1.out` follow the chosen effective horario. `[CERT-live]`
- **Cutover:** with `HorR1=On` and `SchedMux1.out=true` (values matched first), deleted
  `R1.out → HorR1.in10` and added `SchedMux1.out → HorR1.in10`. `HorR1` stayed On, the physical relay held,
  **no flicker**. `[CERT-live]`

So the full chain `SchedMuxk → HorRk.in10 → proxy → JACE → Relay[k].BO → relay` is proven end to end. The
legacy `R1` schedule is disconnected (not deleted); the other 19 GM02 circuits are unchanged.

## 6. Editable horario names via StringWritable `[CERT]`

The client wants to TYPE each horario's name (not "Horario 1..5"). Feasible: add one
`control:BStringWritable` per horario (`Horarion_Nombre`) — `BStringWritable extends BStringPoint` with
`WritableSupport` (a priority-array writable, `out` = `BStatusString`), so a Px text field can write it and
it persists. The COMPONENT keeps its fixed name (`Horarion`) so links/ORDs never break; the user-facing label
is the StringWritable value. The per-circuit selector still resolves by NUMBER (`SelRk` 1..5); the UI maps
number→name by reading the 5 `Horarion_Nombre` for display.

## 7. Campaign scope + what is still needed `[CERT]`/`[INFER]`

Circuit counts from `docs/matriz-puntos-DRAFT.csv` (349) and the client's down-panel report:
- **Excluded (down):** GM01 (19) + GM03 (7) + GM09 (16) = 42. GM01 was the only old-generation panel, so all
  13 remaining are the uniform new-gen `Rk→HorRk` pattern.
- **In scope:** 307 circuits / 13 panels.
- **Blocked on client cuadro de cargas:** GM05×13 + GM16×6 = 19 undescribed. **~288 convertible now.** `[INFER]`
- Still needed: (1) the weekly program CONTENT of the 5 masters; (2) the cuadro de cargas for the 19;
  (3) a time window for live cutovers; (4) Px edit access; (5) commercial confirmation (this is richer than
  the 2-state SEJOFA quote).
- Time `[INFER]`: per-circuit build is now ~2 objects + copy-paste + cutover; the estimate firms after timing
  GM02 circuits 2–20. Working assumption ~1 day/panel → ~2–3 weeks for the 13, plus the Px rework.

## 8. Tooling — bog-nav `--to/--from` accept a handle `[CERT]`

`tools/bog-nav.py`: added `_match_ref()` so `links --to h:xxxx` / `--from h:xxxx` match a link endpoint by its
component handle (not only by path). Prevents the §2 false negative. Regression case added to `selftest`
(`SELFTEST OK`); both previously-empty queries now resolve the `HorR1.out → Relay[1].BO.in10` link. Not yet
committed.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Chain per circuit: `Rk (BooleanSchedule) → HorRk.in10 → JACE proxy → Relay[k].BO.in10 → relay`, uniform GM02–16 | [CERT] | bog-nav on both backups; handles h:6a4d5/h:6a35f/h:5b006/h:1d204; global link dump |
| 2 | `Rk` is a per-circuit `sch:BooleanSchedule` (one schedule per circuit, ~349) | [CERT] | HM_BMS backup grep `GM02ilum/R1` = sch:BooleanSchedule |
| 3 | Insertion seam = `HorRk.in10` on the supervisor; JACE↔BACnet untouched (confirms B841) | [CERT] | link topology; Relay[k].BO objectId=binaryOutput:k |
| 4 | `HorGM02/HorR1.out → Relay[1].BO.in10` (live Relation Sheet) | [CERT-live] | Workbench Relation Sheet, JACE 192.168.1.102 |
| 5 | Numeric→Enum links directly on `BBooleanSelect.select` in 4.3 — no converter (closes B847-G1) | [CERT-live] | Workbench link accepted, select showed value |
| 6 | Corrected model: HOA per horario (5 shared Horn_HOA), circuit = SchedMux+SelR only; 5 horarios not 4 | [CERT-live] | pilot wire sheets; Horn_Mode 2→true/3→false/1→schedule |
| 7 | GM02 circuit 1 cut over live, HorR1 stayed On, relay held, no flicker | [CERT-live] | operator cutover on the production station |
| 8 | Editable horario names feasible via `control:BStringWritable` (writable point) | [CERT] | organized/control-rt BStringWritable extends BStringPoint + WritableSupport |
| 9 | Scope: 13 panels / 307 circuits / ~288 convertible (GM01/03/09 down; 19 blocked on cuadro de cargas) | [CERT]/[INFER] | matriz-puntos-DRAFT.csv counts; client down-panel report |
| 10 | bog-nav `--to/--from` now accept a handle; selftest green | [CERT] | tools/bog-nav.py `_match_ref`; selftest run |

**Tally:** 10 claims — 5 [CERT-live] (the live-station proofs), 4 [CERT] (backup-bog + code + tool), 1 mixed
[CERT]/[INFER] (scope + projection). The design is no longer inferred: the chain, the gate, the model, and the
end-to-end cutover are all empirical.

## Connections
- **B841** — station map + current chain; this block CONFIRMS its "control on the Supervisor" thesis with live evidence.
- **B846** — points matrix; CONFIRMS `Rk` is a real per-circuit BooleanSchedule (the matrix's "R1/Schedule1" was right).
- **B847** — Route B wiresheet; this block CORRECTS it: per-circuit HOA → per-horario HOA, `numberValues` 4 → 5, and CLOSES B847-G1 (gate).
- **B838 / backup-licensing-ops** — the same JACE HM_Central; backup discipline used before the live cutover.

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B851-G1** — replicate GM02 circuits 2–20 and TIME them to firm the per-panel / per-campaign estimate (requires-execution).
- **B851-G2** — the Px/UI rework: per-circuit horario selector bound to `SelRk`, per-horario Auto/ON/OFF + editable name bound to `Horn_Mode`/`Horarion_Nombre`, across 13 panel screens + the menu (requires-execution / needs client content).
- Inherited still-open: B846-G2 (cuadro de cargas for the 19), B842-G3 (calendar/holiday exceptions on the 5 masters).
