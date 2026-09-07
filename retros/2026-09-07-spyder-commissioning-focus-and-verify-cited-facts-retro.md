# Retro — spyder-commissioning focus (B835-837) + the verify-cited-facts discipline

Date: 2026-09-07
Author: Opus 4.8 (research session, with Cristian)
Scope: new focus `spyder-commissioning` — how to commission a Honeywell Spyder on a JACE-8000
(electrical + Niagara N4). Three blocks: B835 (BACnet MS/TP path + field wiring), B836 (Spyder
Tool function-block reference), B837 (LON/Classic path). All three sources consulted; every
load-bearing electrical fact read verbatim from the official Honeywell docs under
`niagara-help/docs-text/`.

## Why this focus exists

Operator how-to ("cómo dar de alta un Spyder con un JACE-8000 + conexión eléctrica"). FUENTE 1
had only fragments (B14 Spyder templates, B23 MS/TP internals, B25 the module set) and **nothing
on the field electrical connection** — that lived only in FUENTE 2 (Honeywell install/wiring
guides). So the corpus answer was "partial", and the electrical part had to be sourced fresh and
then folded back so next time it is FUENTE 1.

## What got produced (all cited)

- **B835** — the MS/TP recipe: JACE power (24 Vac Class-2), COM1=A/COM2=B, bias BIA/END(562 Ω+term),
  Spyder power on bornes 3/4, MS/TP port `C1+/C1-/GND` (RS5N 40/41/42, RL6N 62/63/64), the full
  terminal maps of both models, cable (STP TIA/EIA-485 ~22 AWG, Belden 9842), 120 Ω termination at
  both ends, 32 unit loads (→128 devices), auto-MAC (no address switches, MAC 0 reserved,
  maxMaster 127), and the Workbench flow (BacnetNetwork→port/baud→BACnetSpyder→Controller Summary
  →Engineering Mode→Discover→Match→Full Download→license).
- **B836** — the shared Spyder Tool function blocks (categories `fbs.*`; PID/AIA/FlowControl/Stager/
  StageDriver/SetTemperatureMode/OccupancyArbitrator; the "Stop/Start Sequenced Control Engine"
  gotcha; `spyderApps Ver28` macro layer).
- **B837** — the LON sibling (NPB-8000-LON part 12978, FTT-10A, polarity-insensitive, 78.125 kbaud
  fixed, Neuron ID + service-pin + Commission; station as LonWorks network manager) + a LON-vs-MS/TP
  table. Key finding: **the wiresheet + function blocks are identical** for LON and BACnet; only the
  network layer differs.

## The lesson: a delegated gather's citations are not evidence until verified

The heavy reads were delegated to sub-agents (one per gap). The reports came back richly cited —
but this is **electrical wiring an operator will physically connect**, so the citations had to be
checked, not trusted.

- I first ran `ls niagara-help/docs-text/<file>` from inside `docs-text/` with the full relative
  path again → "No such file" for every file, and briefly concluded the sub-agent had hallucinated
  its sources. **That was my own `cwd` bug**, not a hallucination: the files existed; `find` from the
  repo root found them all. → *A "file not found" after a `cd` is a path bug to rule out before it
  becomes a hallucination verdict.*
- Verifying anyway paid off as **confirmation**: every load-bearing fact (24 Vac, C1+/C1-/GND on
  40/41/42 & 62/63/64, bias END=562 Ω, 120 Ω both-ends termination, MAC 0 reserved, FTT-10 polarity-
  insensitive, part 12978) was found verbatim in the real doc. The gathers were accurate — but the
  discipline is what let me hand over wiring numbers as `[CERT-doc]` rather than `[INFER]`.
- Both agents marked genuinely-absent facts honestly (FTT-10 termination / max-node rules live in an
  Echelon guide outside our three sources → `[INFER]`, flagged, gap B837-G1). Good behavior to keep.

## What stayed open (and why)

- **B835-G4** — a live probe of the operator's JACE for `[CERT-live]` values. **Cannot be done from
  the research host** — it needs the hardware connected and Cristian's authorization. Correctly left
  open rather than faked.
- Residuals B836-G1 (per-block property tables) and B837-G1 (Echelon FTT-10 termination rules).

## Proposed deltas (for human review — not applied to the kit here)

1. **Reinforce in the protocol/PROMPT-LOOP:** when a delegated gather returns citations for facts a
   human will act on physically (wiring, part numbers, safety), the orchestrator VERIFIES a sample
   of the load-bearing citations against the real file before relaying — `[CERT-doc]` requires the
   orchestrator to have seen the line, not just the sub-agent.
2. **Guard note:** rule out a `cwd`/relative-path bug before declaring a cited file missing (verify
   with `find <root> -name` from the repo root).
