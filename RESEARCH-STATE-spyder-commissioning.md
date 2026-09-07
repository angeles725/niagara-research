# RESEARCH-STATE — focus: spyder-commissioning

<!-- research-state.v1
blocks_in_focus: 3
first_block: 835
last_block: 837
open_gaps: 3
next_gap: B835-G4
coverage: BACnet MS/TP (B835) + function blocks (B836) + LON path (B837) done; remaining = G4 live-probe (hardware) + child gaps B836-G1/B837-G1
-->

## Scope

How to commission a **BACnet MS/TP Spyder** (WEB-RS5N/RL6N, Model 5/7) on a **JACE-8000**: field
electrical wiring (JACE power + RS-485 ports/bias, Spyder power/terminals, MS/TP cable/termination),
addressing (auto-MAC, device instance, baud/max-master), and the Workbench/N4 workflow
(BacnetNetwork → BACnetSpyder → Controller Summary View → Engineering Mode → Discover → Match → Full
Download → license). Born from an operator how-to request; the corpus had only partial pieces
(B14 templates, B23 MS/TP internals) and NOTHING on the electrical connection until B835.

## Coverage

- **Done (B835):** the BACnet MS/TP path end-to-end — all electrical facts read verbatim from the Honeywell
  install/wiring docs (`[CERT-doc]`) + the Niagara-side config from B14/B23 and the honeywellBacnetSpyder
  guides + module.xml.
- **Pending:** exact per-terminal number map; the LON/Classic path; the Spyder Tool function-block
  reference; a live-JACE probe for `[CERT-live]` values.

## Open gaps

| Gap | Title | Status |
|-----|-------|--------|
| B835-G1 | Terminal-number map | **CLOSED** — both models (RS5N Table 2 + RL6N Table 6), termination 120 Ω, cable/length/loading, no-switch auto-MAC ([CERT-doc] B835 §3/§4/§5) |
| B835-G2 | LON/Classic Spyder path (NPB-8000-LON + honeywellLonSpyder) | **CLOSED → B837** (FTT-10 wiring, Neuron ID/service-pin/commission, LON vs MS/TP table) |
| B835-G3 | Spyder Tool function-block reference | **CLOSED → B836** (categories fbs.*, key blocks, gotchas, spyderApps Ver28) |
| **B835-G4** | Live commissioning probe on the operator's JACE (authorized) — real values [CERT-live] | **OPEN — needs hardware** (cannot do without the JACE + authorization) |
| B836-G1 / B837-G1 | Per-block property tables / FTT-10 termination (Echelon guide) | open (residual) |

## Iteration history

- 2026-09-06 — B835 bootstrap. Operator asked how to commission a Spyder on a JACE-8000 + electrical
  connection. Confirmed target = BACnet MS/TP Model 5/6/7. Delegated a 3-source gather; **verified every
  load-bearing electrical fact verbatim** against the real Honeywell docs (a first sub-agent report had
  plausible-but-unverified specifics; the docs existed and confirmed them — earlier "files missing" was a
  cwd path bug, not a hallucination). Wrote B835; seeded 4 gaps; registered 6 official docs in SOURCES.
