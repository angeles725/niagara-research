# B761 · Connecting a Honeywell Spyder to a JACE-8000 and running discovery — the end-to-end field-integration workflow (BACnet MS/TP primary, LON legacy), a cross-cutting operational synthesis

> **Scope**: an operator-facing how-to that consolidates what the corpus already knows into one path: how a
> Honeywell **Spyder** controller physically connects to a **JACE-8000** and how it is **discovered** into a
> Niagara station. Two families are distinguished because the bus, driver, and discovery mechanism differ
> entirely: **Spyder BACnet MS/TP** (Model 5/7/II/Micro — current) and **Spyder Classic/LON** (legacy). Covers
> physical bus, station config, the 5-phase discovery lifecycle, and the Spyder-specific N4 engineering cycle
> (`honeywellBacnetSpyder`). This is a **document-mode capture** (METHODOLOGY §20) synthesizing prior research,
> not a new investigation. Cross-cutting — touches focuses **framework-drivers** (B496–B506), **jace8000**
> (B459–B475), and the discovery/operational blocks B28/B40/B14.
>
> **Sources**: FUENTE 1 (corpus) — B28 (discovery framework, decompiled `bacnet-rt`/`lonworks-rt`/`driver-*`),
> B40 (Spyder firmware/apps operational artifacts), B14 (mass VAV commissioning + templates), B459–B475
> (JACE-8000 architecture). FUENTE 2 (official Tridium/Honeywell docs) — `niagara-help/guides-clean/
> HoneywellSpyder/*` (Controller Summary, BACnet Link Manager, Spyder Device Manager / Batch Operations views).
> FUENTE 3 — B28 already cites the decompiled jobs; not re-derived here. **Honesty note**: the JACE-8000
> physical RS-485 port count/pinout is **NOT** present in this install's help set (searched — only CIPer,
> JACE-9000 `docJ9MtgWrg`, and power-board install sheets exist); that detail is marked `[INFER]` and must be
> confirmed against the JACE-8000 Install & Startup Guide.

---

## 761.1 — First decision: which Spyder? It determines everything `[CERT-doc]`

"Spyder" is not one controller. The connection path forks on the field bus:

| Family | Field bus | Niagara driver stack | Status |
|---|---|---|---|
| **Spyder BACnet** (Model 5 / 7 / II / Micro) | **BACnet MS/TP** (RS-485) or BACnet/IP | `bacnet` + `honeywellBacnetSpyder` | Current |
| **Spyder Classic / LON** | **LonWorks FT-10** | `lonworks` + `honeywellLonSpyder` | Legacy |

Corpus corroboration: `Spyder Model 5/7` ship `BEATS_MSTP_*` firmware = the **BACnet MS/TP** family (B40
§40.1); `spyderApps Ver28` is the **LON** app library (B40 §40.2). Valid BACnet models the N4 Controller
Summary lets you select include Spyder II `PVB6436AS / PVB6438NS / PUB6438S` and Micro `PVB4024NS…`
(`[CERT-doc]` `honeywellBacnetSpyder-BacnetControllerSummaryView`). This block treats the dominant case —
**Spyder BACnet MS/TP** — as the main path; §761.7 states what changes for LON.

## 761.2 — Physical connection, Spyder BACnet MS/TP `[INFER, MS/TP facts standard]`

MS/TP runs over **RS-485** (2-wire + reference):

- The JACE-8000 (TITAN) exposes **RS-485** ports on removable terminal blocks. `[INFER — NOT verified in this
  install's help; confirm port count/pinout against the JACE-8000 Install & Startup Guide]`.
- **Daisy-chain topology**, never a star. Each Spyder taps `+ / − / ref` in series.
- **Termination**: ~120 Ω at BOTH physical ends of the segment; **bias/pull-up** at a single point (the JACE
  may provide it via jumper/config). Missing/incorrect termination shows up as intermittent devices at
  discovery time.
- **Single token-passing master domain**: the JACE and the Spyders share the token on one pair; respect the
  per-segment node limit, add a repeater beyond it.

## 761.3 — Station configuration BEFORE discovery `[CERT + CERT-doc]`

Discovery in Niagara is **UI-driven, not server-driven** — there is no generic `BDiscoveryJob`; each driver
brings its own job and Learn table (`[CERT]` B28 §28.1, decompiled `driver-rt.jar`). Setup order:

1. **Palette → BACnet**: drop a `BacnetNetwork` under `Station > Config > Drivers`.
2. **Add the MS/TP Port** under the network, pointing at the JACE COM. Parameters that MUST match every node
   on the bus:
   - **Baud rate** (e.g. 38400 / 76800) — identical on all nodes.
   - **MAC address** of the JACE on that MS/TP (0–127 for masters).
   - **Max Master** ≥ the highest MAC on the bus — set too low, nodes above it are never seen (classic
     "won't discover" cause).
   - **Network Number** of the `BacnetNetwork` — unique across the whole BACnet system.
3. **Local Device object**: the JACE needs its own unique `Device_Instance`.
4. **Honeywell module**: install/verify `honeywellBacnetSpyder` on the JACE. Without it a Spyder appears as a
   generic BACnet device but you **lose** the Spyder views (Controller Summary, Link Manager, Batch
   Operations, program wiresheet) (`[CERT-doc]`).

> Hard rule: mismatched `baud`/`MAC` and a too-low `Max Master` are the #1 "no discovery" cause on MS/TP —
> suspect config before the cable.

## 761.4 — Discovery: the canonical 5-phase lifecycle `[CERT]`

Verified pattern (B28 §28.1.2), BACnet instantiation:

```
1. TRIGGER   BacnetNetwork Device Manager → "Discover" button
2. CONFIG    Device_Instance low/high range + timeout (default 30 s)
3. PROBE     JACE emits Who-Is (broadcast). On MS/TP it rides as a token+data frame on RS-485
4. COLLECT   each Spyder answers I-Am (device-id, vendor-id, max-APDU, segmentation); buffered for the timeout
5. PRESENT   LearnTable lists them → select rows → "Add" → BBacnetDevice created in the BOG
```

Field-relevant detail (B28 §28.2.2 / §28.2.3, decompiled `BBacnetDiscoverDevicesJob` / `…PointsJob`):

- After I-Am the JACE **mines** each device: `ReadProperty` of `Object_Name`, `Vendor_Name`, `Model_Name`,
  `Firmware_Revision`.
- **Duplicate Device Instance**: two Spyders answering with the same `Device_Instance` are flagged duplicate
  in the UI. Each Spyder must have a unique `Device_Instance` — set on the controller (Spyder tool / wall
  module), not on the JACE.
- **Discover Points** (second step, after the device is added): reads the Spyder's `Object_List` (76) —
  100–2000 objects in a VAV — pulling `Present_Value`, `Object_Name`, `Status_Flags`, `Units`, and
  `Priority_Array` for AO/BO/AV/MSO. Gotcha: if the device advertises `segmentation=NONE` but the list
  exceeds one APDU, it falls back to iterative per-index `ReadProperty` (slower, still works).

## 761.5 — What is Spyder-specific in N4 (not just "add device") `[CERT-doc]`

A Spyder is not a passive device: it runs a **program** (function blocks). `honeywellBacnetSpyder` adds its own
engineering cycle:

- **Controller Summary View**: set Device Name, Device Model, TimeZone/DST (feeds the device object's
  UTCOffset); toggle **Engineering Mode ↔ Normal Mode**.
- **Learn Logic / Sync**: "Sync From Field device to Wiresheet" pulls the controller's wiresheet; "Sync From
  Database" pushes yours.
- **BACnet Link Manager**: manages **bindings** between Spyders (source device/object/property → target).
  Link status `NewLink / Bound / Obsolete`; device status `Downloaded / To be downloaded / Offline`.
- **Spyder Batch Operations**: across many devices at once — Batch Download, Batch LearnLogic, Batch Sync
  (From Field / From Database), Batch Set Outputs to Auto (BacnetNetwork), Batch Set Mode to Auto (LonNetwork),
  Batch Compilation, Batch Validation. Serves the mass-VAV commissioning scenario (B14 documents 100 VAV with
  Spyder Model 5 templates).
- **Templates**: the firmware `.ntpl` files (`Palettes_and_Misc/Spyder Model 5|7/templates/`) import as
  Niagara Templates and instantiate under the device; with Template/Match/Bind, discovery can auto-map points
  by vendor+model (B14; B28 §28.2.2 step 10).

## 761.6 — "What to take into account" checklist

**Bus / physical** — daisy-chain; 120 Ω at both ends + bias at one point; baud/MAC/Max-Master aligned across
the whole segment.
**BACnet addressing** — unique `Device_Instance` per Spyder (set on the controller); unique Network Number;
JACE MAC free of collision.
**Modules / licensing** — `honeywellBacnetSpyder` present on the JACE; **point count** — each proxied point
consumes license, and a JACE-8000 has a point/device ceiling, so 100 VAV × N points can hit the license before
the hardware (B14; B28 §28.13 on countable virtual points).
**Firmware / model** — Model 5/7 use `BEATS_MSTP_*` (`.ufw`/`.bin`); Spyder Tool 3.7.x (ToolVersion
`3.7.44.5.206`) does NOT commission Model 8 (B40 G40.2.5) — match the tool to the model.
**Operation** — discover/edit in Engineering Mode, operate in Normal Mode; after point discovery, validate and
**Download** — links stay `To be downloaded` until pushed.

## 761.7 — If the Spyder is LON, not BACnet `[CERT]`

Whole driver changes (B28 §28.3):

- Physical: **LonWorks FT-10**, not RS-485/MS-TP.
- Driver `lonworks` + `honeywellLonSpyder` (LonNvManager, LonSpyderDeviceManager views).
- Discovery: two jobs — `BLonDiscoverJob` finds devices via **Query_Id 0x51** broadcast; `BLonLearnJob` learns
  NVs from the **XIF**. No Who-Is; bind is by Network Variables, not BACnet objects.
- Honeywell Spyder ProgramId prefix `90 00 0c` (Spyder-family variant) vs `80 00 0c` (standard LON Honeywell)
  (B40 G40.2.4).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Spyder forks into BACnet MS/TP (Model 5/7/II/Micro) vs Classic/LON; bus+driver differ | [CERT-doc] | HoneywellSpyder guides; B40 §40.1/§40.2 |
| 2 | Valid BACnet Spyder models: PVB6436AS/PVB6438NS/PUB6438S + Micro PVB4024NS… | [CERT-doc] | `BacnetControllerSummaryView` |
| 3 | Discovery is UI-driven; no generic BDiscoveryJob; per-driver job + Learn table | [CERT] | B28 §28.1 (decompiled driver-rt.jar) |
| 4 | 5-phase lifecycle: Trigger→Config→Probe(Who-Is)→Collect(I-Am)→Present/Add | [CERT] | B28 §28.1.2/§28.2.2 |
| 5 | Discover Points reads Object_List (76) + PV/Name/StatusFlags/Units/PriorityArray; NONE-segmentation fallback | [CERT] | B28 §28.2.3 |
| 6 | Spyder N4 views: Controller Summary, Link Manager (NewLink/Bound/Obsolete), Batch Operations | [CERT-doc] | HoneywellSpyder guides |
| 7 | MS/TP tuning: Max Master ≥ highest MAC, baud/MAC aligned; #1 no-discovery cause | [INFER] | standard MS/TP practice; not a code citation |
| 8 | JACE-8000 RS-485 port count/pinout | [INFER] | NOT in this install's help; needs Install & Startup Guide |
| 9 | LON path: FT-10, BLonDiscoverJob (Query_Id 0x51) + BLonLearnJob (XIF); ProgramId 90 00 0c | [CERT] | B28 §28.3; B40 G40.2.4 |
| 10 | Spyder Tool 3.7.x does not commission Model 8 | [CERT] | B40 G40.2.5 |

**Tally**: 6 [CERT/CERT-doc] with citation, 2 [INFER] explicitly flagged (MS/TP tuning heuristic, and the
unverified physical port detail). No unmarked claims. Every FACT cites a block or an official guide; the two
[INFER]s are named as general practice / not-verified, not disguised as facts.

## Connections
- **B28** — the discovery framework this block operationalizes (BACnet Who-Is/I-Am flow, LON Query_Id/XIF,
  5-phase lifecycle, point-count/virtual licensing).
- **B40** — Spyder firmware families (`BEATS_MSTP_*`), LON `spyderApps Ver28`, ProgramId prefixes, tool-version
  limits.
- **B14** — mass VAV commissioning with Spyder templates (the Batch Operations payoff).
- **B459–B475** (focus **jace8000**) — the JACE-8000 as the QNX host doing the discovering.
- **framework-drivers** (B496–B506) — the generic driver/ProxyExt substrate under the `bacnet`/`lonworks`
  drivers.

## Open gaps
- **B761-G1**: exact JACE-8000 RS-485 port count, pinout, and on-board termination/bias jumpers — needs the
  JACE-8000 Install & Startup Guide (absent from this help set) or a live-hardware read `[CERT-hw]`.
- **B761-G2**: the precise MS/TP Port slot defaults in the N4 `bacnet` module (default baud, Max Master,
  poll/token timing) — verifiable from `bacnet-rt`/`bacnet-wb` decompilation (framework-drivers scope), not
  covered here.
- **B761-G3**: Spyder II Model 8 commissioning path (which tool version replaces 3.7.x) — B40 G40.2.5 left it
  open.
