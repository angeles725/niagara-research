# B854 — HARBOR HM_BMS Supervisor: control-folder and device-naming architecture

**Focus:** harbor-greenmax-lighting
**Sources:** `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/config.bog` (full station bog, sha256 `4515de9498e7…`) · `clients/distech-merida-harbor/HM_Central_backup_07_09_2026/config.bog` (JACE-8000, sha256 `04906eed6eb3…`) · `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/shared/px/GreenMAX*.px`
**Scope:** structural and naming facts about the HM_BMS Supervisor (N4 Workstation, EC-Net4) that governs all 16 GreenMAX lighting panels via a NiagaraNetwork proxy to HM_Central (Distech JACE-8000, EC-Net 4.3). Focuses on device-folder naming, per-circuit schedule placement, tablero Px architecture, file-name anomalies, and relay counts. Evidence from bog-nav read-only inspection only.
**Date:** 2026-09-11
**Verified against:** HM_BMS station backup 2026-09-08 (root `config.bog`), HM_Central station backup 2026-09-07, `shared/px/*.px` files, `tools/greenmax-tablero-gen.py`.

---

## 1. Two config.bog files in the HM_BMS backup

The backup at `HM_BMS_backup_08_09_2026/` contains **two distinct** `.bog` files:

| Path | sha256 (first 16 hex) | Lines (bog-nav grep) | Role |
|---|---|---|---|
| `HM_BMS_backup_08_09_2026/config.bog` | `4515de9498e7cec9…` | 82,519 | **Full station bog** — all per-circuit BooleanSchedules + proxy driver + Principal; registered in SOURCES.md |
| `HM_BMS_backup_08_09_2026/HM_BMS/config.bog` | `3aaeccdb00145ef6…` | 17,245 | Partial extraction (Services + NiagaraNetwork driver + Principal only; no schedules) |

All claim verifications in this block used the **full station bog** (`HM_BMS_backup_08_09_2026/config.bog`). [CERT-live] markers reference that file unless stated otherwise.

---

## 2. Station root structure

The HM_BMS Supervisor has three top-level sections [CERT-live: `bog-nav … tree`]:

- **Services** — Alarm, Category, Role, User, Auth, Box, Fox, History, etc.
- **Drivers/NiagaraNetwork** — two connections:
  - `HM_Central` (NiagaraStation) — the JACE-8000; all GM device folders live here
  - `HM_BMS` (ProviderStation) — self-reference for HM_Central's back-proxy
- **Principal** (Folder, h:c92) — navigation PxViews: `Iluminacion`, `Iluminacion_1`, `SCAH`, `uma19`, `Equipos`

There is **no** `Config/Iluminacion`, `Iluminacion/GM{n}`, `SelR`, `SchedMux`, or `NameMux` component in either bog as of the 2026-09-08 snapshot. Those are the B851/B852 redesign components not yet committed to station.

---

## 3. Device-folder naming in HM_BMS (NiagaraPointFolders)

All GreenMAX ilum panels are proxied in the Supervisor at `Drivers/NiagaraNetwork/HM_Central/points/<name>` as `nd:NiagaraPointFolder`. [CERT-live: `bog-nav … find --type NiagaraPointFolder`]

| Handle | Path (relative to station root) | Panel | Extra leading zero? |
|---|---|---|---|
| h:1327 | `…/points/GM01ilum` | GM01 | no |
| h:136c | `…/points/GM02ilum` | GM02 | no |
| h:13b1 | `…/points/GM03ilum` | GM03 | no |
| h:1268 | `…/points/GM04ilum` | GM04 | no |
| h:1410 | `…/points/GM05ilum` | GM05 | no |
| h:1445 | `…/points/GM06ilum` | GM06 | no |
| h:4b9f | `…/points/GM07ilum` | GM07 | no |
| h:9e8d | `…/points/GM08ilum` | GM08 | no |
| h:1494 | `…/points/GM09ilum` | GM09 | no |
| h:3b88 | `…/points/GM010ilum` | GM10 | **YES — 010** |
| h:3bb1 | `…/points/GM011ilum` | GM11 | **YES — 011** |
| h:4933 | `…/points/GM012ilum` | GM12 | **YES — 012** |
| h:cede | `…/points/GM13ilum` | GM13 | no |
| h:9d9e | `…/points/GM14ilum` | GM14 | no |
| h:9dfd | `…/points/GM15ilum` | GM15 | no |
| h:cf1f | `…/points/GM16ilum` | GM16 | no |

**Pattern:** panels 01–09 and 13–16 use the expected `GM0{X}ilum`/`GM{XX}ilum` format. Only panels **10, 11, and 12** carry an extra leading zero (`GM010`, `GM011`, `GM012`). This appears to be a Workbench configuration error when those three panels were added. Panels 13–16, added later, reverted to the plain format.

**HM_Central inconsistency (cross-station):** in HM_Central's BACnet driver the device names are *not* consistent with HM_BMS. HM_Central has `GM10ilum` (no leading zero), `GM011ilum` (with), and `GM12ilum` (no). [CERT-live: `bog-nav … find --type BacnetDevice` on HM_Central bog] The HM_BMS proxy folder names were created independently in Workbench and differ from the underlying BACnet device names.

---

## 4. Per-circuit BooleanSchedules: current (pre-redesign) architecture

The existing control path (before the B851 redesign) stores one `sch:BooleanSchedule` per circuit inside each device folder, named `R{k}` [CERT-live: `bog-nav HM_BMS_backup_08_09_2026/config.bog find --type BooleanSchedule`]:

```
Drivers/NiagaraNetwork/HM_Central/points/GM010ilum/R1   <sch:BooleanSchedule>
Drivers/NiagaraNetwork/HM_Central/points/GM010ilum/R2   …
…
Drivers/NiagaraNetwork/HM_Central/points/GM010ilum/R32  <sch:BooleanSchedule>
```

These are the `SchdlGMnn Rk` writables. Each schedule outputs to `HorR{k}.in10` in HM_Central via NiagaraNetwork. The redesign (B851) keeps these intact and layers a new mux on top via `SelR{k}/SchedMux{k}`.

---

## 5. Per-panel relay counts

Verified by counting `sch:BooleanSchedule` children per device folder in the HM_BMS full bog AND by counting `c:BooleanWritable` per BACnet device in HM_Central AND by inspecting `.px` file `max_relay`. All three sources agree. [CERT-live for HM_BMS: `bog-nav … find --type BooleanSchedule --json`; CERT-live for HM_Central: `bog-nav … grep "Relay"`; CERT for .px: `grep -oE "Relay\$5b[0-9]+\$5d" shared/px/GreenMAX*.px`]

| Panel (HM_BMS folder name) | Relay count | .px max relay |
|---|---|---|
| GM02ilum | **20** | 20 |
| GM04ilum | **20** | 20 |
| GM05ilum | **15** | 15 |
| GM06ilum | **20** | 20 |
| GM07ilum | **23** | 23 |
| GM08ilum | **32** | 32 |
| GM010ilum | **32** | 32 |
| GM011ilum | **26** | 26 |
| GM012ilum | **12** | 12 |
| GM13ilum | **30** | 30 |
| GM14ilum | **32** | 32 |
| GM15ilum | **32** | 32 |
| GM16ilum | **13** | 13 |

GM01, GM03, GM09 appear in the bog with fewer circuits (partial proxy or non-operative). They are not in the 13 operative tableros targeted by the redesign.

---

## 6. Tablero .px files and relative-ord binding

The current tablero views are stored as **external .px files** in `HM_BMS_backup_08_09_2026/shared/px/`, not as `BPxView` components inside device folders in the bog. `bog-nav … find --type PxView` returns zero results against the full station bog. [CERT-live]

All GreenMAX tablero files use **RELATIVE `slot:` ords** for pills and LEDs [CERT: content of `shared/px/GreenMAX02 TabAL6.px`, `GreenMAX07 TabAL7.px`, `GreenMAX07 TabAL8.px`, etc.]:

```xml
<!-- pill / LED — URL-encoded form of slot:Relay[k].BO -->
ord="slot:Relay$5b1$5d$2eBO"
ord="slot:Relay$5b1$5d$2eBO/active"
```

These resolve correctly when a `.px` file is opened **in the context** of a device folder (the standard Niagara `|view:ViewName` nav-tree mechanism). The redesign (B852/B853, `tools/greenmax-tablero-gen.py`) preserves this relative-ord pattern and is designed to produce a view "ready to paste into the PxView inside `GM<X>ilum`" [CERT: tool docstring]. Until the redesigned tablero is committed to station, no BPxView component exists inside any GM device folder.

---

## 7. File-name anomaly: GreenMAX07 TabAL8.px is GM08's view

Two files share the `GreenMAX07` prefix in `shared/px/`:

| File | Max relay (`Relay$5b{n}$5d` highest n) | Actual panel |
|---|---|---|
| `GreenMAX07 TabAL7.px` | 23 | **GM07** (23 relays ✓) |
| `GreenMAX07 TabAL8.px` | **32** | **GM08** (32 relays) — mislabeled |

[CERT: `grep -oE "Relay\$5b[0-9]+\$5d" …` on both files]

There is no file named `GreenMAX08 TabAL*.px`. `GreenMAX07 TabAL8.px` is GM08's 32-relay tablero, mistakenly prefixed as GreenMAX07. File names are misleading; relay count and handle (inside the .px) are authoritative.

Additional naming inconsistencies observed: `GreenMAX02 TabAL6.px` and `GreenMAX06 TabAL6.px` both carry "TabAL6"; `GreenMAX09 TabAL9.px`, `GreenMAX10 TabAL9.px`, and `GreenMAX11 TabAL9.px` all carry "TabAL9" (the third one has no relay content — appears to be an empty stub). The `TabAL{n}` number does not reliably encode the panel number.

---

## 8. Control folder: redesign target location

The B851–B853 redesign places new components (`SelR{k}`, `SchedMux{k}`, `NameMux{k}`) in a new folder at `Iluminacion/GM{n}` in the Supervisor. The `greenmax-tablero-gen.py` tool emits absolute ords of the form `station:|slot:/Iluminacion/{gm}/…` [CERT: tool docstring in `tools/README.md`].

The control folder uses **plain numbers without any leading zero** even when the device folder carries one. Example: device folder `GM010ilum` → control folder `Iluminacion/GM10`; device folder `GM02ilum` → control folder `Iluminacion/GM2`. [CERT: tool docstring — "control folder = plain number, no leading zero even when the device is `GM0Xilum`"]

This folder does **not yet exist** in the 2026-09-08 backup. The claim "Config/Iluminacion/GM{n}" (with a `Config/` prefix) is inconsistent with the tool, which uses just `Iluminacion/{gm}/`. The actual binding path is `/Iluminacion/{gm}/…`. [INFER on exact path until the redesign is committed and re-backed-up]

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1a | Pills/LEDs bind RELATIVE `slot:Relay[k].BO` in current tablero .px files | [CERT] | `GreenMAX02 TabAL6.px`, `GreenMAX07 TabAL7.px` etc.: `slot:Relay$5b{k}$5d$2eBO` pattern; `grep -oE "slot:Relay\$5b[0-9]+\$5d" …` |
| 1b | PxView "lives INSIDE" the device folder as a bog component | [INFER] | `bog-nav … find --type PxView` → 0 results in the full station bog; .px files are in `shared/px/`; redesign *intends* to put it inside the folder |
| 2a | SelR/SchedMux/NameMux components exist in `Config/Iluminacion/GM{n}` | [INFER] | Not found in 2026-09-08 HM_BMS bog; redesign target not yet committed |
| 2b | Control folder path uses plain number (no leading zero) | [CERT] | `tools/greenmax-tablero-gen.py` docstring in `tools/README.md`: "control folder = plain number, no leading zero even when the device is `GM0Xilum`"; tool emits `station:|slot:/Iluminacion/{gm}/…` |
| 2c | "Config/" prefix in the claim path | [INFER — DISCREPANCY] | Tool uses `/Iluminacion/{gm}/`, not `/Config/Iluminacion/{gm}/` |
| 3 | Device folders GM010/GM011/GM012ilum carry extra leading zero in HM_BMS | [CERT-live] | `bog-nav HM_BMS_backup_08_09_2026/config.bog find --type NiagaraPointFolder` → handles h:3b88/h:3bb1/h:4933 |
| 3b | GM13–16 device folders do NOT carry leading zero | [CERT-live] | Same command: GM13ilum/GM14ilum/GM15ilum/GM16ilum confirmed |
| 3c | HM_Central BACnet names differ (GM10ilum, GM011ilum, GM12ilum — inconsistent) | [CERT-live] | `bog-nav HM_Central_backup_07_09_2026/config.bog find --type BacnetDevice` |
| 4 | `GreenMAX07 TabAL8.px` is GM08's 32-relay view | [CERT] | `grep -oE "Relay\$5b[0-9]+\$5d"` on the file → max 32; `GreenMAX07 TabAL7.px` → max 23 |
| 5 | Per-panel relay counts GM02=20, GM04=20, GM05=15, GM06=20, GM07=23, GM08=32 | [CERT-live] | `bog-nav … find --type BooleanSchedule --json` + HM_Central BooleanWritable count + .px max relay |
| 5b | GM10=32, GM11=26, GM12=12, GM13=30, GM14=32, GM15=32, GM16=13 | [CERT-live] | Same command; folder names GM010/GM011/GM012 in HM_BMS; GM10/GM011/GM12 in HM_Central |

**Tally:** 6 [CERT-live], 3 [CERT], 4 [INFER] (including 1 claim-level discrepancy flagged). 0 unresolved tool failures.

---

## Connections

- [Block B841] — station map + current per-circuit schedule→writable→BACnet chain (the `Drivers/NiagaraNetwork/HM_Central/points/GM0Xilum/R{k}` architecture is a direct continuation of B841's §3 thesis)
- [Block B846] — matrix extraction tool (`greenmax-matrix.py`): 349 circuits, two generations. Relay counts confirmed here match B846's matrix output.
- [Block B851] — LIVE Route B pilot on GM02 circuit 1; adds SelR/SchedMux to the Supervisor. B854 shows these components are absent from the 2026-09-08 backup; the pilot was done live but not re-backed-up before the existing backup was taken.
- [Block B852] — PxView tablero redesign spec. Claim 1b (PxView inside device folder) and claim 2a (Config/Iluminacion/GM{n}) originate from the B852 design intent; B854 establishes they are [INFER] pending commissioning.
- [Block B853] — Hx browser deployment. The `shared/px/` location for .px files and the `|view:` nav mechanism are the current deployment pattern. B854 confirms no BPxView is in the bog.

---

## Open gaps

- **B854-G1** (investigable) — Confirm where the `SelR/SchedMux/NameMux` pilot components (GM02 c1) actually landed in the live station. The 2026-09-08 backup predates or excludes them. A fresh station export after the full GM02 redesign would resolve the exact folder path and whether `Config/` prefix was used.
- **B854-G2** (investigable) — The `GreenMAX11 TabAL9.px` file has zero relay references (empty stub). Determine whether it is an orphaned placeholder, a failed view, or belongs to an unreachable panel.
- **B854-G3** (investigable) — Reconcile the HM_BMS device-folder naming (`GM010/GM011/GM012ilum`) with the HM_Central BACnet device names (`GM10ilum`/`GM011ilum`/`GM12ilum`). The inconsistency suggests the HM_BMS proxy was manually named without matching the JACE-side names.
- **B854-G4** (investigable) — Verify that `tools/greenmax-tablero-gen.py` uses `GM{n}` (with leading zero stripped) vs `GM0{n}` for the control folder ords for single-digit panels (e.g., device `GM02ilum` → control folder `Iluminacion/GM2` or `Iluminacion/GM02`). The docstring says "plain number" but it is worth confirming with a dry-run.
