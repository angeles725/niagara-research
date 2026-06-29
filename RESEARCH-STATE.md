# niagara-research — Research State (Honeywell Spyder ECOSYSTEM focus)

> Operational state consumed by the Research-SDD loop. Mirrored in engram
> (`research/niagara/spyder-gaps`, `research/niagara/spyder-progress`). Visible, versionable source.
>
> **Scope of this state file**: the **Honeywell Spyder ecosystem** focus area (user-chosen). The broader
> niagara corpus (B1–B114) is mature and tracked in `INDEX.md`/`CATALOG.md`; this file tracks ONLY the
> Spyder-ecosystem gap-backlog for the loop. Corpus language for NEW blocks = **English** (the legacy
> niagara blocks are Spanish; new Spyder-ecosystem blocks switch to English, noted at the top of each).
>
> **Bootstrap note (iteration it.1, 2026-06-28)**: the launch prompt assumed the next free block number
> was B110; that was stale — B110–B114 already exist (honRemoteConfig, honEagleHawkHMI, security
> detection, module-signing hardening, BOG encryption). The next free number is **B115**, which this
> iteration wrote.

## Coverage

- **Covered blocks (Spyder ecosystem)**: B77 (Spyder BACnet+LON drivers), B96 (Venom TAB),
  B100 (ipcMigrator Spyder→IPC), B101 (airFlowBalancer/kitCat), B106 (honeywellSpyderTool/XL10NextGen
  engine+compiler+simulator), B115 (spyderToIrmNxMigrator), B116 (docHoneywellSpyder — bundled
  SpyderTool help: official operator + per-block FB I/O reference), **B117 (XL10NextGen FB catalog
  CODE-SIDE — 43 blocks cross-verified descriptor+algorithm vs the B116 docs; closes B106 pending #1),
  B118 (honeywellSpyderTool `io/` detailed I/O layer — closes B106 #2), B119 (honeywellSpyderTool UI/wizard/
  application-generation layer — ~250 cls in 3 sub-systems; closes B106 #3; escalates B102→CERT + B106 TASO
  mapping→CERT), **B120 (Spyder driver download/upload WIRE protocol deep-dive vs B77 — BACnet AtomicWriteFile to
  File objects 301/302/303 + Program-object halt/run vs LON LonWorks file-transfer + NCIs + ShortStack; closes G6),
  B121 (Kingfisher / TR wall-module tool-side — closes G7: `BSylkTIDeviceModelEnum` 21-model registry [TR23/40/42/70/71/75 +SIO +actuator];
  Sylk/S-Bus tool-side framework [`BISylkDevice` compile/decompile + `SylkLink`/`SylkLinkTable` PVID binding + `BSylkDeviceStore` sectioned binary];
  compiler chain `BBasicKFCompiler→BZioEnhCompiler→BZioplusCompiler` PINNED to TR70→TR71→TR75 [TR4x = separate `BTr40/BTr42Compiler` lineage] via `BKFCompilerFactory` ordinal dispatch;
  `BKFStateMachine` 3066-line LCD/HMI simulation; CRC-16-only/no-signature confirmed at Kingfisher+Sylk path; escalates multiple B106 `[CERT-a]`→`[CERT]`),
  **B122 (bundled XML resource extract — closes the last 2 read-only micro-gaps G4b+G6b: sensor catalog `sensorType` ids 0-12 + spans from `IO_Common.xml`,
  per-model fixed-IO pins from `FixedIo_model*.xml`/`IO_Model*.xml`/`IO_Micro_Model*.xml`, and the PVL6436AS=pressure+actuator / PVL6438NS=pressure-only confirmation via `ModelSpecificIOXmlName`;
  per-model store offsets/capacities from `BacnetSpyder.xml`/`LonSpyder.xml` — **CORRECTS B120 §120.6**: Model-8 Micro capacities are IDENTICAL to full-size, the real diff is byte OFFSET/layout + physical wiring count (15-25 pins vs 42), not smaller stores;
  escalates B118 §118.5/§118.2 `[CERT-doc]`→`[CERT]` + B120 §120.6 `[INFER]`→`[CERT]` — NEW this iteration, STATIC loop CLOSED)**.
- **Coverage metric (this focus)**: **7 / 8** main Spyder-ecosystem gaps closed (G1-G7) **+ both XML micro-gaps (G4b, G6b) closed** = **read-only-investigable backlog EMPTY**.
- **Last iteration (it.8, FINAL)**: 2026-06-28 — closed **G4b + G6b** (bundled XML resource extract; thin data-pass, escalation not new code distillation). Read-only-investigable Spyder gaps now **= 0**. **Spyder STATIC loop DECLARED STOPPED** (see Stop control below).
- **Prior iteration (it.7)**: 2026-06-28 — closed **G7** (Kingfisher / TR wall-module tool-side deep-dive, code-side).
  Verified & escalated B106 §106.5's Kingfisher/Sylk `[CERT-a]`: (1) **device model** `BSylkTIDeviceModelEnum` 21 ordinals split into **TR4x** (TR40/TR42 ±H/CO₂) and **TR7x** (TR70/71/75 = "Zio"/"Zio+"); on-wire identity `DEVICE_TYPE_MAJOR=3` (TR7x) vs `14`/`15` (TR40/TR42). (2) **Sylk tool-side** = `BISylkDevice.compile/fullDecompile/quickDecompile` + `SylkLink` (spyderPVID↔sylkDevPVID, dir `>`/`<`/`<>`) + `SylkLinkTable`; binary = nested `BVector` `BSylkDeviceStore`→`FileSection`s. (3) **Compiler chain PINNED** to TR70→TR71→TR75 (only Zio+/TR75 adds Day/EventSchedule, matching `getSchedulePVIDsLimit()` 0→48); TR4x = separate flat `BTr40/BTr42Compiler extends BComponent`; dispatch `BKFCompilerFactory.getKFCompiler()` on model ordinal. (4) **LCD/HMI** `BKFStateMachine` (3066 lines, NOT ~1800 as B106 estimated) + `BSegmentEnums` + `BLcdUtils` (bitSetArray[168], 7/10-seg renderers) — firmware-HMI re-impl for PC preview, the HMI twin of B106's PID sim. (5) **Security**: CRC-16 per file section (`BChecksumGenerator`), zero crypto in `kingfisher/**`+`sylk/**`, 54 `System.out.println` debug leaks — extends B106's no-signature finding to the TR payload + S-Bus transport (B88). FB-engine tie: `BSpyderIICompilation.compileKingfisherInputs/Outputs` (:378/:388/:450/:581); `BSBusWallModule extends BAbstractSylkDeviceFB extends BHoneywellComponent`. Marker tally: 59 `[CERT]` / 3 `[INFER]` (ratio 0.05) / 35 distinct file:line citations all grep-confirmed. doc↔code delta: BKFStateMachine 1800→3066, `BKFLabel`→`B*AlphaNumericAlertLabel`/`BEnumerationLabel`, tr4x recursive 114 (not 101, UI included).
- **Prior iteration (it.6)**: 2026-06-28 — closed **G6** (Spyder driver download/upload WIRE protocol, code-side, both drivers).
  Upgraded B77 §77.4 `[INFER]`/`[CERT-a]` flow to `[CERT]`: **BACnet** moves the compiled binary via **AtomicWriteFile**
  (`atomicWriteFileStream`, APDU-chunked at `maxAPDU-30`, 5× retry + 1s sleep) into **BACnet File objects** (301 config / 302
  param / 303 proxy / 304 file-offset / 262 BOAC / 263 bindings), each section fenced by a **Program-object (type16) halt(prop90←5)
  → write → run(prop90←1)** handshake; metadata via properties (objectName 77, apduTimeout 11, retries 73, maxInfoFrames 63).
  **LON** moves it via **LonWorks file transfer** (`LonFile.createFile/open/write/close` to numbered files: relay 0/1/2/3, normal
  1/2/3) + **NCI** force-writes (`nciDeviceName`, `nciApplVerNew` GUID/rev/brand/ver) for identity, plus an **Echelon ShortStack**
  image (SI data + SNVT descriptors + program ID) generated at compile-time and embedded as stores 33/34. **Same for both**:
  per-store `getBinaryModified()` BitSet incremental download, model-offset store layout (`getStoreOffset`→`ModelInfo`), and **CRC-16
  ONLY (no signature)** integrity with a read-back-and-CRC-correct step on the proxy file. **[SECURITY]** confirms B106 at the wire:
  any party that speaks AtomicWriteFile to 301/302/303 + recomputes CRC injects arbitrary control logic. **Model 8 = Micro BACnet
  family** (PVB4024NS/PVB4022AS/PUB4024S/PUB1012S/PVB0000AS, `model_id="8"` in BacnetSpyder.xml) — grounds the B40.2.6 note; it is a
  capacity/offset distinction, not a numeric runtime branch. Version gating via `compVer.compareTo(Version)` (5.0.0/5.110/5.113/6.1.0/
  6.112 + LON NV gates) on restore; ToolVersion stamped on upload.
- **Prior iteration**: 2026-06-28 — closed G5 (`honeywellSpyderTool` full UI/wizard layer, code-side: ~250 UI classes
  across `functionalBlocks/ui`/`deviceModes/common/ui`/`xl10Controller/ui`/`kingfisher/ui`+`uiframework`). Found **3
  architecturally distinct sub-systems**: (A) FB wiresheet editor + device-mode UI = **pure Niagara** (`BPiranhaWireSheet
  extends BWireSheet` codename "Piranha", `BSpyderComponentView extends BWbComponentView`, `BModeUI`→Engineering/
  Simulation/OnlineDebugging = the 3 B116 modes); (B) **the application-generation backbone IS the model factory** —
  `FactoryManager` string-dispatches a model name to an `IModelFactory` loaded by reflection from a sibling module
  (lonSpyder/bacnetSpyder/tasowizSupport); `IModelFactory` (29 methods) supplies all managers → the JAR is a model-
  agnostic shell; (C) Kingfisher wall-module config wizard (`BWizardFrame extends BFrame`) + XML-driven UI-gen engine
  (`BAXViewKF`/`BAXContainerKF`/`BLayoutUtilKF`, widgets via `Class.forName(type)`). **ESCALATED B102 `[INFER]`→`[CERT]`**
  (183 files use genericUIFramework, concentrated in kingfisher) and **ESCALATED the TASO mapping in B106 `[CERT-a]`→
  `[CERT]`** (`BUtilityClass`: Model1=CV/AHU, 2/3=VAV, Bacnet1/2=VAV, Model4=LCBS). Honest gap G5b: `tasowizSupport`
  absent from corpus → B118.→B119.

## Gap-backlog (prioritized)

| Pri | ID | Gap | Artifact / source | Bucket | Status |
|---|---|---|---|---|---|
| — | G1 | `spyderToIrmNxMigrator` — Spyder XL10 → IRM Nx application transpiler | Java `organized/spyderToIrmNxMigrator/` | read-only | **COVERED → B115** |
| — | G2 | `docHoneywellSpyder` — bundled Spyder docs/help (operator + FB reference; yields `[CERT-doc]` for the FB catalog) | doc/HTML `organized/docHoneywellSpyder/` | read-only | **COVERED → B116** |
| — | G3 | XL10NextGen **complete FB catalog** — per-block I/O, params, semantics (B106 pending #1) — code-side cross-verification of all 43 FBs (descriptor pins/NBlockID + simulation algorithm) against B116's `[CERT-doc]` palette; authoritative FB type-ID registry derived | Java `organized/honeywellSpyderTool/.../functionalBlocks/blocks/` + `deviceModes/simulation/simulationblocks/` | read-only | **COVERED → B117** (39/43 MATCH, XOr semantic mismatch flagged, B106 #1 closed, B115 count corrected) |
| — | G4 | `honeywellSpyderTool` **detailed I/O layer** (`io/`, 87 cls: terminal assignment, linearization, sensor types) — B106 pending #2 | Java `organized/honeywellSpyderTool/.../io/` | read-only | **COVERED → B118** (XML-driven physical-points model; 2-pt + piecewise linearization; `byte[]` terminal map AI/DI/AO/DO+flags; model-gating mechanism CERT, per-model XML values stay CERT-doc from B116; B106 #2 closed) |
| — | G5 | `honeywellSpyderTool` **full UI/wizard layer** (`functionalBlocks/ui`, `kingfisher/ui`, wizardgen, ~250 cls) — B106 pending #3 | Java (wb UI) | read-only | **COVERED → B119** (3 sub-systems: (A) editor=Niagara puro, (B) model factory = app-generation backbone, (C) Kingfisher wizard + XML UI-gen; escalates B102→CERT + B106 TASO mapping→CERT; B106 #3 closed) |
| low | G5b | `tasowizSupport` module — the TASO wizard **factory/device/blocks** (`com.honeywell.tasowiz.*`: TASOWizModelFactory1-4, BTasoWizLonSpyder/BacnetSpyder, BLogicalInputSelect/OutputSelect, BZeleny) — what each CV-AHU/VAV/LCBS wizard emits internally | Java module **absent from corpus** | blocked-on-artifact (needs the `tasowizSupport` JAR decompiled) | pending |
| — | G6 | Spyder **driver deep-dive vs B77** — download/upload protocol wire detail, BOAC, file writers, retry/restore version gating (B77 left `[CERT-a]`/`[INFER]`) | Java `honeywellBacnetSpyder`/`honeywellLonSpyder` | read-only | **COVERED → B120** (BACnet AtomicWriteFile→File objects 301/302/303 + Program-object halt/run; LON LonWorks file-transfer + NCIs + ShortStack; getBinaryModified BitSet incremental; CRC-16-only integrity + read-back correct; Model8=Micro; version gating CERT; B77 §77.4 escalated `[INFER]`→`[CERT]`) |
| low | G6b | Per-model **store offsets/capacities** XML — extract `getFileVariables(name).getOffset()/getCapacity()` values from `BacnetSpyder.xml`/`LonSpyder.xml` to upgrade Model-8 (Micro) capacity claims from `[INFER]` to `[CERT]` | XML resource | read-only | **COVERED → B122** (Model-8 = ModelMicroBACnet1-5 PVB4024NS/PVB4022AS/PUB4024S/PUB1012S/PVB0000AS; **CORRECTION**: Micro store capacities IDENTICAL to full-size [NvConfig 500, ControlLoop 200], real diff = param-file byte OFFSET/layout reorder + DigitalInput/FloatingMotor record sizes + separate FileOffset store + physical wiring 15-25 pins vs 42; LON ShortStack stores 33/34 caps concrete; B120 §120.6 `[INFER]`→`[CERT]`) |
| — | G7 | **Kingfisher / TR wall-module tool-side** deep-dive (`kingfisher/tr4x`, `sylk/fw`, BKFStateMachine LCD sim) — B106 touched, not distilled | Java `honeywellSpyderTool/.../kingfisher`, `sylk/fw` | read-only | **COVERED → B121** (TR device model = `BSylkTIDeviceModelEnum` 21 ordinals TR4x/TR7x; Sylk tool-side `BISylkDevice`+`SylkLink`/`SylkLinkTable`+`BSylkDeviceStore` sectioned binary; compiler chain `BBasicKFCompiler→BZioEnhCompiler→BZioplusCompiler` PINNED to TR70→71→75, TR4x=separate `BTr40/BTr42Compiler`, dispatch `BKFCompilerFactory` by ordinal; `BKFStateMachine` 3066-line LCD/HMI sim + `BSegmentEnums`/`BLcdUtils`; CRC-16-only/no-signature; escalates B106 `[CERT-a]`→`[CERT]`) |
| low | G8 | Spyder→IRM migration **round-trip fidelity** — verify a migrated app behaves equivalently on a real IRM/BEATS (the lossy mappings of B115 §115.3/§115.7) | requires migrated app + IRM controller | requires-execution / blocked | deferred |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.1 | 2026-06-28 | G1 — spyderToIrmNxMigrator (Spyder→IRM Nx transpiler) | B115 | G8 (round-trip fidelity, requires-execution); sharpened G3/G4/G5 from B106 pendings |
| it.2 | 2026-06-28 | G2 — docHoneywellSpyder (bundled JavaHelp doc module; `[CERT-doc]` per-block FB I/O + controller I/O model) | B116 | none net-new; G3 advanced (per-block vendor I/O now documented, code-side algorithm verification remains); G4 fed (`[CERT-doc]` physical-points/model-gating); escalated B77 XIF `[INFER]`→`[CERT-doc]` |
| it.3 | 2026-06-28 | G3 — XL10NextGen complete FB catalog, CODE-SIDE (43 FBs descriptor+algorithm cross-verified vs B116; authoritative type-ID registry from FBSpyder1/2/RelayInfo) | B117 | none net-new; closed B106 pending #1; corrected B115 §115.3 45-vs-43 count `[INFER]`→`[CERT]`; surfaced XOr "exactly-one-true" semantic mismatch (migration correctness risk for G8) |
| it.4 | 2026-06-28 | G4 — `honeywellSpyderTool` detailed I/O layer, CODE-SIDE (87-class `io/`: physical-points model, sensor linearization, `byte[]` terminal assignment + model-gating; cross-verified vs B116 §116.4 — MATCH) | B118 | G4b (micro-gap, low): extract the bundled IO XML resources (`IO_Common.xml`, per-model `*IO.xml`, `lonXL10NextGenFixedIo` XML) to upgrade per-model fixed-point values from `[CERT-doc]` to `[CERT]`; closed B106 pending #2 |
| it.5 | 2026-06-28 | G5 — `honeywellSpyderTool` full UI/wizard layer, CODE-SIDE (~250 cls; 3 sub-systems: editor=Niagara puro / model-factory app-generation backbone / Kingfisher wizard + XML UI-gen). Escalated B102 §102.5 `[INFER]`→`[CERT]` (183 files use genericUIFramework) + B106 TASO model→HVAC mapping `[CERT-a]`→`[CERT]` (BUtilityClass). Closed B106 pending #3 | B119 | G5b (low, blocked-on-artifact): the `tasowizSupport` module (`com.honeywell.tasowiz.*` factories/devices/blocks) is absent from the corpus → what each TASO wizard generates internally stays uninvestigable until it is decompiled |
| it.6 | 2026-06-28 | G6 — Spyder driver download/upload WIRE protocol deep-dive vs B77, CODE-SIDE (both drivers): BACnet AtomicWriteFile→File objects 301/302/303/304/262/263 + Program-object (type16) halt(prop90←5)/run(prop90←1) handshake, APDU-chunked, 5× retry; LON LonWorks file-transfer (`LonFile`) to numbered files + NCI force-writes (`nciDeviceName`/`nciApplVerNew`) + Echelon ShortStack image (stores 33/34); `getBinaryModified()` BitSet incremental (both); CRC-16-only integrity + proxy read-back-and-correct; Model8=Micro BACnet family; version gating CERT. Escalated B77 §77.4 `[INFER]`/`[CERT-a]`→`[CERT]`. [SECURITY] confirms B106 no-signature finding at the wire | B120 | G6b (low, blocked-on-artifact): per-model store offsets/capacities live in `BacnetSpyder.xml`/`LonSpyder.xml` (not read) → Model-8 capacity specifics stay `[INFER]` until the XML is extracted |
| it.8 | 2026-06-28 | **G4b + G6b** — bundled XML resource extract (FINAL, thin data-pass). **G4b**: sensor catalog `sensorType` ids 0-12 + Input/Output/Sensor spans from `IO_Common.xml:71-83`; per-model fixed-IO pins from `FixedIo_model{1,2,3}.xml` + `IO_Model{1,2,3}.xml` + `IO_Micro_Model{1..5}.xml` (Micro relocates actuator 17/18→12/13); **PVL6436AS=pressure+actuator / PVL6438NS=pressure-only CONFIRMED verbatim** via `ModelSpecificIOXmlName` (PVL6436AS→IO_Model1, PVL6438NS→IO_Model2) — closes B118 §118.7 "UNVERIFIABLE code-side" ledger row, `[CERT-doc]`→`[CERT]`. **G6b**: Model-8 = ModelMicroBACnet1-5 from `BacnetSpyder.xml`; **CORRECTION of B120 §120.6** — Micro store CAPACITIES are IDENTICAL to full-size (NvConfig 500, ControlLoop 200), the real difference is param-file byte OFFSET/layout reorder (ApplVerNew@0 vs @2059) + a few record sizes (DigitalInput 20→22, FloatingMotor 14→18) + a separate FileOffset store + smaller PHYSICAL wiring (15-25 pins vs 42); LON ShortStack stores 33/34 caps concrete; `[INFER]`→`[CERT]` | B122 | **none** — read-only-investigable backlog now EMPTY; STATIC loop STOPPED |
| it.7 | 2026-06-28 | G7 — Kingfisher / TR wall-module tool-side, CODE-SIDE: device model `BSylkTIDeviceModelEnum` (21 ordinals TR23/40/42/70/71/75 +SIO +actuator, TR4x vs TR7x families, on-wire `DEVICE_TYPE_MAJOR` 3/14/15); Sylk tool-side framework (`BISylkDevice` compile/fullDecompile/quickDecompile + `SylkLink`/`SylkLinkTable` PVID binding `>`/`<`/`<>` + `BSylkDeviceStore` nested-`BVector` `FileSection` binary + `ICompilationConstants` sensor-PVID/file-offset/screen-type grammar); compiler chain `BBasicKFCompiler→BZioEnhCompiler→BZioplusCompiler` PINNED to TR70→TR71→TR75 (only Zio+/TR75 adds Day/EventSchedule, `getSchedulePVIDsLimit()` 0→48), TR4x = separate flat `BTr40/BTr42Compiler extends BComponent`, dispatch `BKFCompilerFactory.getKFCompiler()` by model ordinal; `BKFStateMachine` (3066 lines) LCD/HMI simulation + `BSegmentEnums`/`BLcdUtils`(bitSetArray[168], 7/10-seg renderers); CRC-16-only/no-crypto + 54 `System.out.println` leaks; FB-engine tie `BSpyderIICompilation.compileKingfisher*` + `BSBusWallModule→BAbstractSylkDeviceFB→BHoneywellComponent`. Verified & escalated multiple B106 §106.5 `[CERT-a]`→`[CERT]` | B121 | none net-new read-only (G7 was the last main read-only gap). Remaining read-only = only the 2 XML-extract micro-gaps G4b + G6b. doc↔code deltas logged honestly (BKFStateMachine 1800→3066; `BKFLabel`→`B*AlphaNumericAlertLabel`; tr4x 114 recursive not 101) |

## Blocked / non-read-only gaps (tagged with what they need)

- **G8** — needs: a migrated `.bog` loaded onto a **live IRM/BEATS controller** to confirm the lossy FB
  reconstructions (BAggregation, BPsychrometric, BIrmSubFolder decompositions) are behaviorally
  equivalent. → DYNAMIC phase (METHODOLOGY §12) when hardware appears. `requires-execution`.
- **G5b** — needs: the `tasowizSupport` module JAR (`com.honeywell.tasowiz.*`) added to the corpus and decompiled.
  honeywellSpyderTool only references it by string + reflection (`FactoryManager`/`BSpyderPaletteRoot`); the actual
  CV-AHU/VAV/LCBS application templates the TASO wizards emit live there. `blocked-on-artifact` (missing module).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8) — **SPYDER STATIC LOOP STOPPED**

- **Open gaps — read-only investigable**: **0** (G1-G7 all closed; G4b + G6b micro-gaps closed → B122). ← **STATIC loop investigable-exhaustion REACHED.**
- **Open gaps — requires-execution**: **1** (G8)
- **Open gaps — blocked (live system/hardware/artifact)**: **1** (G5b — needs `tasowizSupport` module added to corpus)
- Consecutive iterations with empty read-only backlog: N/A (primary criterion = investigable exhaustion, now met)
- Budget cap (default safety net): none
- **Total Spyder-ecosystem gaps remaining**: **2**, both **non-read-only** (1 requires-execution [G8] + 1 blocked-on-artifact [G5b]). Read-only-investigable = **0**.

### 🛑 STOP DECLARATION (Spyder ecosystem, it.8, 2026-06-28)

The **Honeywell Spyder ecosystem STATIC research loop is DECLARED STOPPED** per METHODOLOGY §8 (primary criterion: read-only-investigable exhaustion). Every read-only-investigable Spyder gap is closed.

- **Blocks written this focus session**: **B115–B122** (8 blocks, all English):
  - B115 spyderToIrmNxMigrator (G1) · B116 docHoneywellSpyder vendor help `[CERT-doc]` (G2) · B117 XL10NextGen FB catalog code-side (G3) · B118 `io/` detailed I/O layer (G4) · B119 UI/wizard/app-generation layer (G5) · B120 driver download/upload WIRE protocol (G6) · B121 Kingfisher/TR wall-module tool-side (G7) · **B122 bundled XML resource extract (G4b + G6b)**.
- **Coverage**: **7/7 main read-only gaps closed (100%)** + both XML micro-gaps (G4b, G6b) closed. Read-only-investigable backlog **= 0**.
- **2 remaining gaps — both BLOCKED on non-read-only access, tagged with what each needs**:
  1. **G8** (round-trip migration fidelity) — **requires-execution / hardware**. Needs a migrated `.bog` loaded onto a **live IRM/BEATS controller** to confirm B115's lossy FB reconstructions (BAggregation, BPsychrometric, BIrmSubFolder decompositions) behave equivalently. → DYNAMIC phase (METHODOLOGY §12) when hardware appears.
  2. **G5b** (`tasowizSupport` module internals) — **blocked-on-artifact**. Needs the `tasowizSupport` JAR (`com.honeywell.tasowiz.*` factories/devices/blocks) **added to the corpus and decompiled**. honeywellSpyderTool only references it by string + reflection (`FactoryManager`/`BSpyderPaletteRoot`); the CV-AHU/VAV/LCBS application templates the TASO wizards emit live there.

**Resume condition**: re-open the Spyder loop only if (a) IRM/BEATS hardware becomes available (→ G8, dynamic), or (b) the `tasowizSupport` module is added to `organized/` (→ G5b). No further read-only static work remains.
