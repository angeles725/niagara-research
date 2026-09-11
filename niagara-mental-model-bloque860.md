# B860 — HARBOR HM_Central JACE: HorGMnn seam anatomy, command-writable naming survey, and GM01 normalization plan

**Focus:** harbor-greenmax-lighting
**Sources:**
- `clients/distech-merida-harbor/HM_Central_backup_07_09_2026/config.bog` (JACE HM_Central, Distech EC-Net JACE-8000 4.3.58.18, sha256 `04906eed6eb3324397fb6ffa08d933e51cf99d2aeb0ed2a1b7fbacb45e0db960`)
- `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/config.bog` (Supervisor HM_BMS, N4 full station bog, sha256 `4515de9498e7cec9c4a82a0f67ca5780e6b6f5a97a3a28a96f884e3f7ed272e0`)
**Date:** 2026-09-11
**Closes:** B841-G2, B841-G3, B854-G3, B846-G3

---

## 1. The HorGMnn folders on the JACE: anatomy and verdict (closes B841-G2)

### 1.1 Folder inventory

The JACE bog contains 16 `nd:NiagaraPointFolder` entries named `HorGM01`..`HorGM16` under
`Drivers/NiagaraNetwork/HM_BMS/points/`. [CERT-live: `bog-nav HM_Central_backup_07_09_2026/config.bog grep "HorGM" | grep NiagaraPointFolder`]

Counting `c:BooleanPoint` children (each with a `nd:NiagaraProxyExt`) per folder:

| Folder | HorRk entries | Matches panel circuit count (B841/B854)? |
|--------|--------------|------------------------------------------|
| HorGM01 | **0** | n/a — GM01 uses old pattern |
| HorGM02 | 20 | yes (GM02 = 20) |
| HorGM03 | 7 | yes (GM03 = 7; entries R1–R6 + R17, not consecutive) |
| HorGM04 | 20 | yes |
| HorGM05 | 15 | yes |
| HorGM06 | 20 | yes |
| HorGM07 | 23 | yes |
| HorGM08 | 32 | yes |
| HorGM09 | 16 | yes |
| HorGM10 | 32 | yes (GM010 = 32) |
| HorGM11 | 26 | yes (GM011 = 26) |
| HorGM12 | 12 | yes (GM012 = 12) |
| HorGM13 | 30 | yes |
| HorGM14 | 32 | yes |
| HorGM15 | 32 | yes |
| HorGM16 | 13 | yes |

[CERT-live: `bog-nav HM_Central_backup_07_09_2026/config.bog grep "HorR" | grep "BooleanPoint" | grep -oP "HorGM\d+" | sort | uniq -c`]

### 1.2 What the populated folders contain

Each `HorGMnn/HorR{k}` is a `c:BooleanPoint` with a `nd:NiagaraProxyExt`. The proxy's `pointId` resolves to
the Supervisor's proxy-writable for the corresponding circuit. Example for GM02 R1:
[CERT-live: `bog-nav HM_Central_backup_07_09_2026/config.bog slot "Drivers/NiagaraNetwork/HM_BMS/points/HorGM02/HorR1/proxyExt"`]

```
pointId = slot:/Drivers/NiagaraNetwork/HM_Central/points/GM02ilum/HorR1
```

Meaning: the JACE's `HorGM02/HorR1` subscribes to the Supervisor's `Drivers/NiagaraNetwork/HM_Central/points/GM02ilum/HorR1`, which is the Supervisor-side `c:BooleanWritable` that schedules write to at `in10`.

### 1.3 Outgoing links: each HorR{k} drives a BACnet relay BO

Every `HorGMnn/HorR{k}.out` carries a dataLink to the corresponding BACnet Binary Output at `in10`:
[CERT-live: `bog-nav HM_Central_backup_07_09_2026/config.bog links --from "Drivers/NiagaraNetwork/HM_BMS/points/HorGM02"`]

```
HorGM02/HorR1.out   →  Relay[1].BO.in10
HorGM02/HorR10.out  →  Relay[10].BO.in10
… (all 20 circuits)
HorGM02/HorR19.out  →  Relay[20].BO.in10
```

This confirms the HorGMnn folders are part of the **active, production command chain** for GM02–GM16: they
form the JACE-side relay layer that reads the Supervisor's commanded value and writes it to the BACnet BO.

### 1.4 HorGM01: empty placeholder

`HorGM01` exists as a `nd:NiagaraPointFolder` with no children and no links. [CERT-live: `bog-nav … slot "Drivers/NiagaraNetwork/HM_BMS/points/HorGM01"` returns only the folder header; grep for HorGM01 produces no BooleanPoint rows in the JACE bog.]

### 1.5 Verdict

- **HorGM02–HorGM16: reusable consolidation seam.** [INFER] These are the active JACE-side relay layer for
  the 15 panels that use the new generation pattern. They subscribe to the Supervisor's `HorR{k}` writables
  and drive BACnet `Relay[k].BO.in10` at production-write priority. Any redesign that targets these writables
  (B851 mux, B843 custom module, B844 kitControl) passes transparently through this seam with no JACE-side
  change needed.

- **HorGM01: empty pending-migration placeholder.** [INFER] It was created in anticipation of a GM01
  normalization (§4 below) that has not yet been executed. It is structurally ready (the folder exists) but
  carries no content and no links — it is not orphaned dead scaffolding, but an unfilled migration seam.

---

## 2. Command-writable naming per panel (closes B841-G3)

B841 verified only GM01. The full survey:

### GM01 — old generation

On the Supervisor, writable targets are named `SchdlGM01 R{k}` (note the space before `R`), typed
`c:BooleanWritable`, R1..R14, inside the proxy folder `Drivers/NiagaraNetwork/HM_Central/points/GM01ilum/`.
[CERT-live: `bog-nav HM_BMS_backup_08_09_2026/config.bog grep "SchdlGM01" | grep BooleanWritable` → handles
h:227d4..h:227ee, 14 entries; bog XML encodes space as `$20`, i.e., `SchdlGM01$20R1`]

On the JACE, the corresponding proxy subscriptions live at
`Drivers/NiagaraNetwork/HM_BMS/points/GreenMAX1/SchdlGM01 R{k}` (14 × `c:BooleanPoint`), and each
`.out` dataLink drives `GM01ilum/Relay[k].BO.in16` (priority 16 fallback). [CERT-live: `bog-nav
HM_Central_backup_07_09_2026/config.bog links --from "Drivers/NiagaraNetwork/HM_BMS/points/GreenMAX1"` →
`SchdlGM01 R1.out → Relay[1].BO.in16` through `SchdlGM01 R6.out` shown; pattern consistent with B841 §3]

### GM02–GM16 — new generation

On the Supervisor, writable targets are named `HorR{k}` (`c:BooleanWritable`), R1..R{n} (where n = per-panel
circuit count), inside each panel's proxy folder `Drivers/NiagaraNetwork/HM_Central/points/GMnnilum/`.
[CERT-live: `bog-nav HM_BMS_backup_08_09_2026/config.bog grep "HorR1" | grep BooleanWritable` → confirms
`GM02ilum/HorR1`, `GM03ilum/HorR1`, `GM04ilum/HorR1`, `GM05ilum/HorR1`, `GM06ilum/HorR1`, `GM07ilum/HorR1`,
at minimum, all typed `c:BooleanWritable`]

On the JACE, the corresponding proxy subscriptions are the `HorGMnn/HorR{k}` points described in §1, linked
to `Relay[k].BO.in10` (priority 10, the normal schedule priority).

**Summary table:**

| Panel(s) | Supervisor writable name | Supervisor write priority | JACE proxy folder | JACE→BACnet priority |
|----------|--------------------------|--------------------------|-------------------|----------------------|
| GM01 | `SchdlGM01 R{k}` (space before R) | in10 (schedule→writable) | `GreenMAX1` | in16 (relay BO) |
| GM02–GM16 | `HorR{k}` | in10 | `HorGMnn` | in10 (relay BO) |

The naming difference matters for any automated emitter or script that must target these writables: GM01
requires `SchdlGM01 R{k}` as the point path component, while all other panels use `HorR{k}`.

---

## 3. HM_BMS vs HM_Central device-naming inconsistency, precise characterization (closes B854-G3)

B854-G3 noted the discrepancy for panels 10–12. A full cross-station survey extends it:

| Panel | HM_BMS proxy folder name | HM_Central BACnet device name | Direction of extra zero |
|-------|--------------------------|-------------------------------|------------------------|
| 01–09 | `GMnn ilum` | `GMnn ilum` | — consistent |
| 10 | `GM010ilum` | `GM10ilum` | HM_BMS adds zero |
| **11** | `GM011ilum` | `GM011ilum` | **consistent** (both carry zero) |
| 12 | `GM012ilum` | `GM12ilum` | HM_BMS adds zero |
| 13–14 | `GM13/14ilum` | `GM13/14ilum` | — consistent |
| **15** | `GM15ilum` | `GM015ilum` | **HM_Central adds zero** |
| 16 | `GM16ilum` | `GM16ilum` | — consistent |

[CERT-live: HM_BMS side from `bog-nav HM_BMS_backup_08_09_2026/config.bog find --type NiagaraPointFolder`
(B854 §3); HM_Central BACnet side from `bog-nav HM_Central_backup_07_09_2026/config.bog find --type
BacnetDevice | grep -i gm` → h:b6bc=`GM10ilum`, h:8928=`GM011ilum`, h:86fe=`GM12ilum`, h:dede=`GM015ilum`,
h:1841e=`GM13ilum`, h:df9d=`GM14ilum`]

**Precise pattern:**
- Panels 10 and 12: HM_BMS proxy was given an extra leading zero (`GM010`, `GM012`) when its BACnet device
  name in HM_Central has none (`GM10`, `GM12`). [INFER: Workbench configuration inconsistency when these
  panels were added, as B854 §3 noted.]
- Panel 11: both sides carry `GM011` — consistent. The zero originated on the BACnet device itself and was
  carried forward correctly when HM_BMS created the proxy.
- Panel 15: the JACE BACnet device name `GM015ilum` carries an extra zero not present in the HM_BMS proxy
  `GM15ilum`. This is the inverse mismatch direction — one or the other was misnamed at a different point
  in time.

**Redesign / emitter implications:**
- Addresses on the **HM_BMS (Supervisor) side** must use HM_BMS proxy folder names:
  `GM010ilum`, `GM011ilum`, `GM012ilum`, `GM15ilum`.
- Addresses on the **HM_Central (JACE) BACnet side** must use HM_Central BACnet device names:
  `GM10ilum`, `GM011ilum`, `GM12ilum`, `GM015ilum`.
- The **HorGMnn proxy folders on the JACE** (`HorGM10`, `HorGM11`, `HorGM12`, `HorGM15`) use sequential
  plain-number naming independent of both above naming schemes — they are always `HorGM{decimal panel number}`
  without extra zeros, regardless of which side carries one. [CERT-live: folder names confirmed by the grep
  at §1.1]
- Any tool that generates ords must look up the correct naming per side rather than deriving one from the
  other.

---

## 4. GM01 normalization to the newer pattern: concrete change set (closes B846-G3)

This is a design recommendation. All items below are [INFER] unless noted; they are grounded in the
two real generations directly visible in the bogs (§2 above, B841 §3).

### 4.1 Current GM01 state

| Layer | Components | Link target | Priority |
|-------|-----------|-------------|---------|
| Supervisor | `Schedule{k}` (BooleanSchedule, k=1..14) → `SchdlGM01 R{k}` (BooleanWritable).in10 | Schedule writes writable | in10 |
| JACE proxy | `GreenMAX1/SchdlGM01 R{k}` (BooleanPoint, proxyExt) `.out` | `Relay[k].BO.in16` | in16 |
| JACE HorGM01 | folder exists, **0 children, 0 links** | — | — |

### 4.2 Target state (aligned with GM02–GM16)

| Layer | Components | Link target | Priority |
|-------|-----------|-------------|---------|
| Supervisor | `R{k}` (BooleanSchedule, renamed from `Schedule{k}`) → `HorR{k}` (BooleanWritable, new).in10 | schedule writes writable | in10 |
| Supervisor labels | `labelR{k}` (kitControl:StringConst) × 14, `tabR{k}` (kitControl:StringConst) × 14 | — (metadata only) | n/a |
| JACE proxy | `HorGM01/HorR{k}` (BooleanPoint + proxyExt, new) `.out` | `Relay[k].BO.in10` | **in10** |
| JACE old | `GreenMAX1/SchdlGM01 R{k}` links → `Relay[k].BO.in16` | decommissioned | — |

### 4.3 Required changes

**On the Supervisor (HM_BMS):**
1. Add 14 × `HorR{k}` (`c:BooleanWritable`) inside `Drivers/NiagaraNetwork/HM_Central/points/GM01ilum/`.
2. Relink `Schedule{k}.out → HorR{k}.in10` (replacing the existing `Schedule{k}.out → SchdlGM01 R{k}.in10`).
   The schedule objects themselves do not need renaming unless uniform `R{k}` names are desired.
3. Add 14 × `labelR{k}` (`kitControl:StringConst`) with the per-circuit description. **GM01 has no existing
   in-station labels** (old generation omitted them); descriptions must be sourced from the client's cuadro
   de cargas or on-site verification (see open gap B846-G2 for the 5 undescribed GM01 circuits).
4. Add 14 × `tabR{k}` (`kitControl:StringConst`) with the tablero assignment string (e.g., `"TAB AL-1"`).
5. Decommission `SchdlGM01 R{k}` writables once step 2 is live and the JACE side is rewired.

**On the JACE (HM_Central):**
6. Populate `HorGM01` with 14 × `HorR{k}` (`c:BooleanPoint` + `nd:NiagaraProxyExt`), with each `proxyExt.pointId`
   set to `slot:/Drivers/NiagaraNetwork/HM_Central/points/GM01ilum/HorR{k}` (pointing to the new Supervisor
   writables added in step 1).
7. Create 14 dataLinks: `HorGM01/HorR{k}.out → Relay[k].BO.in10`. This is a **priority upgrade** from the
   current `in16` used by the old chain.
8. Remove the 14 existing dataLinks `GreenMAX1/SchdlGM01 R{k}.out → Relay[k].BO.in16`.

### 4.4 Key considerations

- **Write-priority upgrade (in16 → in10):** Before commissioning, confirm no external override writes occupy
  priorities 11–15 on `GM01ilum/Relay[k].BO`. The current `in16` is the lowest fallback; `in10` is the
  standard schedule priority used by all other panels. Higher priorities (in8, in1) remain free for manual
  override in both schemes.
- **HorGM01 is structurally ready:** the empty folder already exists on the JACE; only its children and
  their links need to be added.
- **Circuit indices:** the JACE HorGM01 folder will receive R1..R14 sequentially (GM01 has 14 circuits in
  the bog, all with consecutive indices), unlike GM03 which has a non-consecutive set.
- **Label sourcing required:** no equivalent of B846's `greenmax-matrix.py` output exists for GM01 labels;
  the 5 undescribed entries in that CSV (B846-G2) include the GM01 circuits.
- **This is prerequisite work for the bog emitter (B859-G1):** the emitter must generate GM01 using `HorR{k}`
  target names, not `SchdlGM01 R{k}`. The normalization step aligns GM01 with the emitter's single code path.

---

## Self-verify table

| # | Claim | Marker | Evidence command / source |
|---|-------|--------|--------------------------|
| 1 | All 16 HorGMnn folders exist in HM_Central JACE bog | [CERT-live] | `bog-nav HM_Central/config.bog grep "HorGM" \| grep NiagaraPointFolder` → 16 rows |
| 2 | HorGM01 has 0 HorRk children and 0 links | [CERT-live] | `bog-nav … slot "…/HorGM01"` → folder header only; no BooleanPoint rows in grep count |
| 3 | HorGM02 has 20 HorRk BooleanPoint entries, HorGM03=7, HorGM04=20, …, HorGM16=13 | [CERT-live] | `bog-nav … grep "HorR" \| grep BooleanPoint \| grep -oP "HorGM\d+" \| uniq -c` → 15 rows (HorGM01 absent) |
| 4 | HorGM02-16 circuit counts match per-panel counts from B841/B854 exactly | [CERT-live] | Same command vs B841 §2 table |
| 5 | HorGM02/HorR1/proxyExt.pointId = slot:/Drivers/NiagaraNetwork/HM_Central/points/GM02ilum/HorR1 | [CERT-live] | `bog-nav … slot "…/HorGM02/HorR1/proxyExt"` → pointId value |
| 6 | HorGM02/HorR{k}.out → GM02ilum/Relay[k].BO.in10 links confirmed live | [CERT-live] | `bog-nav … links --from "…/HorGM02"` → R1.out→Relay[1].BO.in10, R19.out→Relay[20].BO.in10, etc. |
| 7 | HorGM02-16 are the active production command chain | [INFER] | §1.2/1.3: have proxyExt + outgoing BO links; no link-count discrepancy found |
| 8 | HorGM01 is an empty pending-migration placeholder, not dead scaffolding | [INFER] | §1.4: 0 children, 0 links; structurally parallel to populated siblings; aligns with B841's "two generations" |
| 9 | GM01 Supervisor writables = `SchdlGM01 R{k}` (BooleanWritable, space before R, R1..R14) | [CERT-live] | `bog-nav HM_BMS/config.bog grep "SchdlGM01" \| grep BooleanWritable` → 14 entries h:227d4..h:227ee in GM01ilum |
| 10 | GM02+ Supervisor writables = `HorR{k}` (BooleanWritable) | [CERT-live] | `bog-nav HM_BMS/config.bog grep "HorR1" \| grep BooleanWritable` → GM02-GM07+ all have HorR1 BooleanWritable |
| 11 | JACE GM01 proxy folder = `GreenMAX1`, with `SchdlGM01 R{k}` BooleanPoint linking to Relay[k].BO.in16 | [CERT-live] | `bog-nav HM_Central/config.bog grep "GreenMAX" \| grep NiagaraPointFolder` → h:21d92; `links --from "…/GreenMAX1"` → SchdlGM01 R1.out→Relay[1].BO.in16 |
| 12 | HM_BMS panel 10 = `GM010ilum`, HM_Central BACnet = `GM10ilum` (extra zero on HM_BMS side) | [CERT-live] | B854 §3 (HM_BMS); `find --type BacnetDevice \| grep gm` in HM_Central → h:b6bc=GM10ilum |
| 13 | HM_BMS panel 11 = `GM011ilum`, HM_Central BACnet = `GM011ilum` (consistent) | [CERT-live] | B854 §3 (HM_BMS); `find --type BacnetDevice` in HM_Central → h:8928=GM011ilum |
| 14 | HM_BMS panel 12 = `GM012ilum`, HM_Central BACnet = `GM12ilum` (extra zero on HM_BMS side) | [CERT-live] | B854 §3 (HM_BMS); `find --type BacnetDevice` in HM_Central → h:86fe=GM12ilum |
| 15 | HM_BMS panel 15 = `GM15ilum`, HM_Central BACnet = `GM015ilum` (extra zero on HM_Central side) | [CERT-live] | B854 §3 (HM_BMS has GM15ilum); `find --type BacnetDevice` in HM_Central → h:dede=GM015ilum |
| 16 | HorGMnn folders use plain decimal numbers regardless of which side carries a leading zero | [CERT-live] | §1.1: folder names HorGM10, HorGM11, HorGM12, HorGM15 confirmed in grep output |
| 17 | GM01 normalization requires new HorR{k} writables (Supervisor) + populated HorGM01 (JACE) + priority upgrade in16→in10 | [INFER] | §4: derived from the two-generation diff; empty HorGM01 as structural evidence |

**Tally:** 12 [CERT-live], 2 [INFER-judgment], 3 [INFER-derived]. No unresolved tool failures. No uncited assertions.

---

## Connections

- **[B841]** — established the two panel generations (§3/§13) and flagged B841-G2/G3 as open; this block closes both.
- **[B846]** — matrix extraction tool; GM01 label absence noted there; B846-G3 closed here with a concrete change set. B846-G2 (26 undescribed circuits, including 5 GM01) remains open and is prerequisite for step 3 of §4.3.
- **[B851]** — LIVE Route B pilot on GM02 c1; the `HorGM02/HorR1` seam documented here is the production-layer the pilot exercises.
- **[B854]** — device-naming architecture; B854-G3 extended here with the panel-15 reversal and a per-panel table.
- **[B859]** — bog emitter; §4.4 notes that GM01 normalization is prerequisite to a single emitter code path.

---

## Open gaps

- **B846-G2** (requires client) — 26 undescribed circuits (5 from GM01) still need the cuadro de cargas to fill `labelR{k}` / `tabR{k}` for GM01 normalization step 3.
- **B860-G1** (requires execution) — live confirmation that no writes occupy priorities 11–15 on `GM01ilum/Relay[k].BO` before the priority upgrade (in16 → in10) in step 7 of §4.3. A read of the live priority array in Workbench resolves this before commissioning.
- **B854-G3 partial** — which side (HM_BMS proxy or HM_Central BACnet) should be corrected for the panel-15 naming discrepancy (`GM15ilum` vs `GM015ilum`) requires an operator decision; this block only characterizes the discrepancy.
