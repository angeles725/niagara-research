# B750 · Applying the Honeywell organization taxonomy to OUR modules — a concrete re-organization of ColdRoomPan / CompPan / DashboardPan

> **Scope**: turn the 10 Honeywell organization patterns (B749) into a concrete, deploy-safe re-organization of
> our refrigeration modules. For each pattern: what Honeywell does, what WE do today, and the specific change.
> This is the deliverable of the operator's "cómo tener todo bien distribuido" ask. Foco:
> **interactive-composition** (IC10, closes the focus). DESIGN block — high [INFER] expected; every Honeywell
> FACT cites B749, every "our modules" fact cites the prior audit.
>
> **Sources**: FUENTE 1 — B749 (the taxonomy, P1-P10), B737/B744 (composition), B735/B745 (flags/facets/units),
> B746 (palette templates), B742 (our consolidated backlog), B731 (our module audit), B739/B740 (schema/enum
> safety), B729 (lifecycle/units resolution). Our module facts: the ColdRoomPan/CompPan/DashboardPan audit
> already in the corpus + engram commissioning map.

---

## 750.1 — Where we stand vs the Honeywell patterns `[CERT/INFER]`

| Pattern (B749) | Honeywell does | We do today | Gap |
|---|---|---|---|
| P1 reuse the framework spine | app = `BPointDeviceExt`, folder = `BPointFolder` | plain `BComponent`s BLinked to driver proxy points | OK — we correctly stay plain components; we are NOT a driver (B734) |
| **P2 domain device-extensions** | one child ext per domain (points/alarms/sched/config) | `BColdRoom` → `BEvaporatorUnit` (25 flat slots) → `BDefrostController` | ⚠️ evaporator mixes timing/outputs/hoa/freeze/alarms in one flat block |
| **P3 config vs state vs wire-map** | state on point · frozen `config` child · `BStruct` wire-map | config, state, and links all mixed as flat slots | ⚠️ no config/state separation |
| **P4 typed folders w/ isParentLegal** | `BParameterFolder` etc. self-validate parent/child | evaporators are frozen children, no folder; no legality guard | ⚠️ works but no self-validation |
| P5 frozen skeleton / dynamic population | frozen known HW, dynamic logic | frozen evaporators (known count) | OK — frozen fits our fixed rooms |
| **P6 reuse via palette templates** | Venom/IRM ship pre-wired palette apps | bare components in the palette (B746) | ⚠️ commissioning is drag-nest-flag-link |
| P7 management separate from containers | device managers ≠ containers | N/A (we have no management module) | N/A |
| P8 category = palette taxonomy | FB categories are packages, tree is flat | N/A | N/A |
| **P9 semantic tags/relations overlay** | honTagDictionary decorates the tree | no tags on our components | ⚠️ no discoverability by query |
| P10 shared base substrate | honIOBase = point/config bases | each module independent | minor — worth it only if we grow more equipment modules |

Five actionable gaps: **P2, P3, P4, P6, P9**. They line up with (and sharpen) the B748 playbook.

## 750.2 — The re-organization, pattern by pattern `[INFER, grounded in B749]`

### From P2+P4 — distribute the evaporator into domain child components (typed folder for the group)
```
BColdRoom
 ├─ evaporators : (typed folder, isChildLegal → BEvaporatorUnit)     ← P4
 │   ├─ BEvaporatorUnit_1
 │   └─ BEvaporatorUnit_2
 └─ BDefrostController        (already a de-facto "defrost domain ext" of the room)   ← P2
```
And inside each `BEvaporatorUnit`, one child per DOMAIN (exactly Honeywell's per-domain split):
```
BEvaporatorUnit
 ├─ (core)   runCmd, hasDefrost, coilTemp
 ├─ timing   : BComponent   startDelay, stopDelay, powerOnDelay        ← P2 domain child
 ├─ outputs  : BComponent   valveOut, evapOut, resistanceOut  (READONLY)
 ├─ hoa      : BComponent   valveMode, fanMode, resistanceMode (OPERATOR)
 └─ freeze   : BComponent   freezeSetpoint, freezeDiffStop, freezeDiffRestart
```
This is B737's fix, now confirmed as the SAME thing Honeywell does everywhere (P2). Deploy-safe: ADD the
child components, migrate links; never retype an existing slot (P-safety, B739).

### From P3 — separate config from live-state
Move the tunables (setpoints, delays, limits) into a frozen `config` child `BComponent`, keep the live values
+ `BStatus` on the unit. This is `honIOBase`'s point-vs-config duality (B749 P3). Benefit: the property sheet
shows "what it's doing" (state) apart from "how it's set" (config); the HMI binds each cleanly. We have NO
field-register map, so we skip the third `BStruct` wire-map plane — that plane only exists because Honeywell
addresses BACnet objects; our outputs are BLinks to driver points (B734 §734.5).

### From P6 — ship pre-wired palette assembly templates
Exactly Venom/IRM (B749 P6) and B746 §746.3: author `module.palette` items that are WHOLE pre-wired rooms
(`ColdRoom_2Evaps_Defrost` with `hasDefrost=true` baked in, the defrost controller already a child so
`units()` resolves — B729). Commissioning becomes drag-one-thing. Lowest risk (palette is a resource).

### From P9 — tag the components for query-based discoverability
Add semantic tags (`room`, `evaporator`, `compressor`, `defrost`) via a small tag dictionary (honTagDictionary
pattern, B749 P9). Then the dashboard and any operator view find equipment by BQL/NEQL query instead of
tree-walking — and it feeds a future interactive flow view (B748-G1). Overlay, not nesting: it does not touch
the containment tree.

### P10 — a shared base module: defer
Only worth it if we add more equipment types (a freezer module, an AHU module). For three refrigeration modules
it is premature; revisit if the equipment catalog grows.

## 750.3 — Sequenced, deploy-safe plan `[INFER]`
Slots into B742 as the "organization" batch, ordered by risk:
1. **Palette templates (P6)** — resource-only, zero code risk. Do first; immediate commissioning win.
2. **Tags (P9)** — additive overlay, no tree change.
3. **Config/state split (P3)** + **domain children (P2/P4)** — the structural change. ADD child components,
   migrate links in one careful pass per module (never retype — B739 was a real outage). Test lifecycle/units
   after (B729/B743). Do the evaporator first (biggest sprawl), then compressor.
The full [CERT] mechanics for each step already live in the cited blocks; this is organization, not new logic.

## 750.4 — The one-line rule to remember `[INFER]`
**Honeywell distributes by CONTAINMENT with fixed roles (device → per-domain extension → typed folder → leaf),
separates config/state/wire into distinct blocks, freezes the known skeleton and adds the variable part
dynamically, packages reuse as palette templates, and overlays meaning with tags.** Copy that shape and our
modules stop "desbordando".

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Our five actionable gaps vs Honeywell = P2, P3, P4, P6, P9 (table) | [CERT/INFER] | B749 patterns × our audit (B731/B742) |
| 2 | Distribute evaporator into domain child components + a typed folder for the group | [INFER] | B749 P2/P4; B737 §B.3 |
| 3 | Separate config (frozen child) from live-state; skip the wire-map plane (we have no field registers) | [INFER] | B749 P3; B734 §734.5 |
| 4 | Ship pre-wired palette templates (ColdRoom_2Evaps_Defrost, hasDefrost baked in) | [CERT/INFER] | B749 P6; B746 §746.3; B729 (units resolution) |
| 5 | Tag components for BQL discoverability as an overlay, not nesting | [INFER] | B749 P9; B5 (BQL/tags) |
| 6 | Sequenced deploy-safe: palette → tags → config/state+domain children; never retype (B739) | [INFER/CERT] | B742; B739 (retype outage [CERT]) |

**Tally**: 4 [INFER], 2 [CERT/INFER]. High [INFER] expected (applied design); every FACT cites a block. No
unmarked claims.

## Connections
- **B749** (the taxonomy this applies), **B737**/**B744** (composition), **B735**/**B745** (flags/facets/units),
  **B746** (palette templates = P6), **B742** (the backlog this batch joins), **B731** (our audit), **B739**/
  **B740** (schema/enum safety), **B729** (units resolution / lifecycle), **B748** (the interactivity playbook
  this reinforces), **B5** (tags/BQL for P9).

## Open gaps
- **B750-G1**: a worked slotomatic diff for the evaporator domain-child refactor (child @NiagaraProperty
  declarations + link migration) — an implementation task, not research.
- **B750-G2**: authoring the pre-wired palette templates + a tag dictionary in Workbench and reading back the
  serialized BOG (confirms B746-G1's override syntax) — requires-execution.
