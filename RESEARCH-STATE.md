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
  File objects 301/302/303 + Program-object halt/run vs LON LonWorks file-transfer + NCIs + ShortStack; closes G6 — NEW this iteration)**.
- **Coverage metric (this focus)**: **6 / 8** Spyder-ecosystem gaps closed this loop session (G1, G2, G3, G4, G5, G6).
- **Last iteration (it.6)**: 2026-06-28 — closed **G6** (Spyder driver download/upload WIRE protocol, code-side, both drivers).
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
| low | G6b | Per-model **store offsets/capacities** XML — extract `getFileVariables(name).getOffset()/getCapacity()` values from `BacnetSpyder.xml`/`LonSpyder.xml` to upgrade Model-8 (Micro) capacity claims from `[INFER]` to `[CERT]` | XML resource (not read) | blocked-on-artifact (XML extract) | pending |
| medium | G7 | **Kingfisher / TR wall-module tool-side** deep-dive (`kingfisher/tr4x` ~101 cls, `sylk/fw` ~39, BKFStateMachine LCD sim) — B106 touched, not distilled | Java `honeywellSpyderTool/.../kingfisher`, `sylk/fw` | read-only | pending |
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

## Blocked / non-read-only gaps (tagged with what they need)

- **G8** — needs: a migrated `.bog` loaded onto a **live IRM/BEATS controller** to confirm the lossy FB
  reconstructions (BAggregation, BPsychrometric, BIrmSubFolder decompositions) are behaviorally
  equivalent. → DYNAMIC phase (METHODOLOGY §12) when hardware appears. `requires-execution`.
- **G5b** — needs: the `tasowizSupport` module JAR (`com.honeywell.tasowiz.*`) added to the corpus and decompiled.
  honeywellSpyderTool only references it by string + reflection (`FactoryManager`/`BSpyderPaletteRoot`); the actual
  CV-AHU/VAV/LCBS application templates the TASO wizards emit live there. `blocked-on-artifact` (missing module).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: **1** (G7) + 2 micro-gaps (G4b, G6b — both XML-extract, low pri)  ← STATIC loop stops when this hits 0
- **Open gaps — requires-execution**: **1** (G8)
- **Open gaps — blocked (live system/hardware/artifact)**: **1** (G5b — needs `tasowizSupport` module added to corpus)
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap (default safety net): none
- **Total Spyder-ecosystem gaps remaining**: **3** main (1 read-only investigable [G7] + 1 requires-execution [G8] + 1 blocked-on-artifact [G5b]) + 2 micro-gaps (G4b, G6b)
- **Next recommended gap**: **G7** (Kingfisher/TR wall-module tool-side deep-dive, `kingfisher/tr4x` ~101 cls + `sylk/fw` ~39 +
  BKFStateMachine LCD sim — B106 touched, not distilled). After G7, the only remaining read-only-investigable work is the two
  XML-extract micro-gaps (G4b IO XML, G6b model offset/capacity XML). Blocked **G5b**: decompile `tasowizSupport` if it appears.
  **Read-only investigable Spyder gaps remaining after this iteration: 1 main (G7) + 2 micro-gaps.**
