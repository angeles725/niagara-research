# RESEARCH-STATE — focus: spyder-commissioning

<!-- research-state.v1
blocks_in_focus: 7
first_block: 835
last_block: 865
open_gaps: 2
next_gap: B835-G4
coverage: BACnet MS/TP (B835) + function blocks (B836) + LON path (B837) + model-family→bus routing (B839, terminal label authoritative [CERT-hw]) + operator field-wiring runbook (B840) + web-evidence: Enhanced=LON-only + no-BACnet-Enhanced confirmed, decode supported (B864) + residuals: property tables + FTT-10 termination (B865) done; remaining = G4 live-probe (hardware) + B839-G1(partial/unavailable-wall); INVESTIGABLE-COMPLETE
-->

## Scope

How to commission a **BACnet MS/TP Spyder** (WEB-RS5N/RL6N, Model 5/7) on a **JACE-8000**: field
electrical wiring (JACE power + RS-485 ports/bias, Spyder power/terminals, MS/TP cable/termination),
addressing (auto-MAC, device instance, baud/max-master), and the Workbench/N4 workflow
(BacnetNetwork → BACnetSpyder → Controller Summary View → Engineering Mode → Discover → Match → Full
Download → license). Born from an operator how-to request; the corpus had only partial pieces
(B14 templates, B23 MS/TP internals) and NOTHING on the electrical connection until B835.

## Coverage

- **Done (B835–B837, B839–B840, B864, B865):** BACnet MS/TP path (B835), function-block reference (B836),
  LON/Classic path (B837), model-family decode (B839), field-wiring runbook (B840), Enhanced=LON-only
  web confirmation (B864), FB property tables + FTT-10 termination (B865).
- **Pending (req-exec / unavailable-wall only):** live JACE probe (B835-G4, hardware required); verbatim
  nomenclature key (B839-G1, TRADELINE catalog not public). Focus is **investigable-complete**.

## Open gaps

| Gap | Title | Status |
|-----|-------|--------|
| B835-G1 | Terminal-number map | **CLOSED** — both models (RS5N Table 2 + RL6N Table 6), termination 120 Ω, cable/length/loading, no-switch auto-MAC ([CERT-doc] B835 §3/§4/§5) |
| B835-G2 | LON/Classic Spyder path (NPB-8000-LON + honeywellLonSpyder) | **CLOSED → B837** (FTT-10 wiring, Neuron ID/service-pin/commission, LON vs MS/TP table) |
| B835-G3 | Spyder Tool function-block reference | **CLOSED → B836** (categories fbs.*, key blocks, gotchas, spyderApps Ver28) |
| **B835-G4** | Live commissioning probe on the operator's JACE (authorized) — real values [CERT-live] | **OPEN — needs hardware** (cannot do without the JACE + authorization) |
| B836-G1 | Per-block property tables for commissioning-relevant FBs (PID/Stager/StageDriver/OccupancyArbitrator/RateLimit/SetTemperatureMode + NVI/NVO/NCI) | **CLOSED (partial)** — property tables confirmed [CERT-doc] from individual guides (B865 §1); no discrete Alarm or Schedule FB in guide set (scoped gap noted B865 §1.7/§1.8) |
| B837-G1 | FTT-10 free-topology termination (Echelon guide) | **CLOSED (partial)** — 52.3 Ω×1 free / 105 Ω×2 bus [CERT-doc] from 3 Honeywell equipment docs (B865 §2); 320 m node-to-node / 500 m total wire [CERT-doc]; max-node-count (64?) still [INFER], Echelon 078-0156-01F not in source set |
| B839 | "Spyder Sylk Enhanced" = LON family (PVL/PUL…ES); model-number 3rd letter L=LON/B=BACnet routes JACE wiring B837 vs B835 | **DONE** — corrects a session MS/TP assumption ([CERT-doc] LonAlarmsView L174-181) |
| B839-G1 | Verbatim Honeywell datasheet model-number key (promote §2 decode [INFER]→[CERT-doc]) | **PARTIAL** — typed unavailable wall (B864 §1); TRADELINE catalog not publicly accessible; decode stays [INFER] but strongly supported by Table 1 cross-match across 4 official datasheets (29 models, 0 exceptions); digit decode verifiable from CERT-web; letter meanings still [INFER] |
| B839-G2 | Does "Spyder Enhanced" have both LON and BACnet SKUs? Live unit named "Sylk Enhanced" had BAC+/BAC-/SHIELD (MS/TP) — help lists Enhanced only under LON | **CLOSED → B864** — Enhanced is LON-only (PVL6436AES/PVL6438NES/PUL6438ES); no BACnet Enhanced SKU exists in any official Honeywell datasheet (63-1325/63-1328/63-2685/63-2689); live unit was a BACnet Classic (PVB/PUB) mis-identified by name; B839 §1 LON-only framing confirmed correct [CERT-web+CERT-doc] |

## Iteration history

- 2026-09-11 — B865 (residuals). Closed B836-G1 (partial): property tables for 6 key commissioning-relevant
  FBs (PID/Stager/StageDriver/OccupancyArbitrator/RateLimit/SetTemperatureMode) confirmed [CERT-doc] from
  individual guides; NVI/NVO/NCI covered as wiring primitives; no discrete Alarm/Schedule FB found. Closed
  B837-G1 (partial): FTT-10 termination values (52.3 Ω×1 free / 105 Ω×2 bus, 320 m node-to-node, 500 m
  total wire) confirmed [CERT-doc] from 3 Honeywell equipment docs (Eagle Controller EN2Z-1002GE51, CIPer
  Model 50 31-00233-03, EAGLEHAWK NX EN1Z-1039GE51); max-node-count (64) stays [INFER], Echelon
  078-0156-01F not in source set. Focus is now investigable-complete — remaining gaps are req-exec
  hardware (B835-G4) and unavailable-wall (B839-G1 partial).

- 2026-09-11 — B864 (web-evidence discovery). Closed B839-G2: four official Honeywell datasheets
  (63-2685/63-1325 LON; 63-1328/63-2689 BACnet) confirm that "Spyder Enhanced" (PVL/PUL…ES) is
  LON-only — no BACnet Enhanced SKU exists. B839 §1 LON-only framing is correct; live unit called
  "Sylk Enhanced" with BAC+ terminals was a BACnet Classic mis-identified by name. B839-G1 moved to
  partial: typed unavailable wall for verbatim nomenclature key (TRADELINE catalog not public); digit
  decode [UI][DI][AO][DO] verifiable from CERT-web Table 1; letter meanings remain [INFER].


- 2026-09-08 — B840 (document mode). Consolidated the session's operator field-wiring how-to into a
  panel-level runbook: terminal map (BAC+/BAC-/SHIELD → JACE −/+/S), shield-to-shield + earth-at-one-end,
  bias-switch decision (BIA 2.7kΩ/END 562Ω+150Ω/MID 47.5kΩ + which case), and the 2-node JACE↔Spyder case
  (JACE END + Spyder 120Ω). Also ENRICHED B835 §2 with the verbatim bias values + the `S`=shield rule (were
  incomplete). Cites B835/B839/JACE wiring guide; no re-derivation.

- 2026-09-08 — B839 + [CERT-hw] correction. Operator said the unit is a "Spyder Sylk Enhanced." Triaged the
  family name against niagara-help: `honeywellLonSpyder-LonAlarmsView` lists Spyder Enhanced under the LON
  driver, so B839 first concluded Enhanced→LON. THEN the operator read the physical terminals:
  BAC+(7)/BAC-(8)/SHIELD(9) = BACnet MS/TP (RS-485). Live evidence > doc: corrected B839 (§5 + [CERT-hw]
  claim 9 + banner) — the family NAME is not a reliable bus proxy; the terminal label decides (BAC+/BAC- or
  C1+/C1- ⇒ B835 MS/TP; 2-pin no-polarity FTT-10 ⇒ B837 LON). Opened B839-G2 (does Enhanced have both SKUs?).
  Net answer to operator: wire BAC+/BAC-/SHIELD as RS-485 MS/TP to the JACE (B835 path).
- 2026-09-06 — B835 bootstrap. Operator asked how to commission a Spyder on a JACE-8000 + electrical
  connection. Confirmed target = BACnet MS/TP Model 5/6/7. Delegated a 3-source gather; **verified every
  load-bearing electrical fact verbatim** against the real Honeywell docs (a first sub-agent report had
  plausible-but-unverified specifics; the docs existed and confirmed them — earlier "files missing" was a
  cwd path bug, not a hallucination). Wrote B835; seeded 4 gaps; registered 6 official docs in SOURCES.
