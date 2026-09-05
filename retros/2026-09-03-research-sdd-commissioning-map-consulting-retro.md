<!-- review-status: applied 2026-09-05 · kit 272e1ad · PARTIAL — shipped: #2, #3 (§20 quick-mode drift + client knowledge, PR #434); DEFERRED: #1 (§11 read the defining code before an enumeration claim) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective

> Run reviewed: a mixed session — ONE `/research-sdd` QUICK-mode hardware question (does an NRIO / IO-34-H
> analog output do 0-10 V, for a VFD at 40 m) answered from niagara-help docs-text, followed by a long
> PANCCADIA commissioning-map ANALYSIS (read the 3 control/UX modules + the client bitacora, delegated 2
> Explore agents, built a per-slot connection map, updated the CLIENT bitacora). No corpus block written.
> READ-ONLY on the kit — PROPOSES only (§18). Build-specific deltas live in the build-n4-module kit retro
> `2026-09-03-dashboard-servlet-write-surface-and-reader-authority.md`.

## Context

Two shapes in one session:

1. **QUICK/clarification** — a scoped [CERT-doc] hardware fact (IO-34-H = 16 UI / 10 relays / **8 AO
   0-10 Vdc**, the AO current-limited to 4 mA), answered directly from niagara-help
   `docs-text/IO-34-H_InputOutput_Module_Install_-_95-7753.txt` (FUENTE 2). Corpus/code not needed; the
   vendor install manual carried it. The question then DRIFTED into application-design advice (VFD input
   impedance, 40 m noise, then a PID→AO control loop) — still delivered as direct advice, no block.

2. **ANALYSIS/consulting** — "what connects to what" across ColdRoomPan / CompPan / DashboardPan.
   Delegated two parallel Explore agents (RT I/O map + UX controllable surface), each returning
   file:line-cited maps. I then made TWO wrong classifications in chat (anti-frost "not on the dashboard";
   `startDelay` as property-sheet-only) that the operator caught, before reading the ONE authoritative
   list (`DashboardReader.java:80-134`).

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | For an **ENUMERATION / set-membership** claim ("is X on the dashboard", "which slots does Y expose"), FIND and READ the code that DEFINES the set *before* answering. An enumerable, authoritative list beats any agent summary or partial read. Frame it as the sharp case of "read the real source": for set-membership, read the set's definition whole, once. | `METHODOLOGY.md §11` (self-verify) + `§5` (sources) | Classified `freeze*` and `startDelay` from partial reads / agent summaries → wrong twice, operator-caught. Authority was `DashboardReader.java:80-134` (7 named slot-group arrays, ~55 lines) — reading it first settles every "is it on the dashboard" question and would have prevented both errors. | new | HIGH |
| 2 | QUICK mode may DRIFT from a scoped factual question into APPLICATION-DESIGN advice (spec fact → "how do I use it"). It stays quick (no block), but the answer turns advisory/[INFER]-heavy, so MARK the shift: keep the cited [CERT-doc] fact separable from the design recommendation ([INFER] / install-practice). | `METHODOLOGY.md §20` (quick/document paths) + `§3` (markers) | NRI-O-34 0-10 V ([CERT-doc], install manual) → VFD-at-40 m wiring + PID-loop advice ([INFER], practice). Delivered together; marker discipline kept fact vs advice apart. | new | MEDIUM |
| 3 | CLIENT-specific commissioning/how-to knowledge routes to the **CLIENT repo bitacora**, NOT the niagara-research corpus or the kit. Document-mode captures REUSABLE toolchain/subject knowledge; a per-install wiring map is client project state. Name the routing test so document-mode isn't misapplied to client deliverables. | `METHODOLOGY.md §20` (document-mode write-routing) | The verified connection map went to `Cliente/Leon-Guanjuato/bitacora/2026-09-02-commissioning-3-modulos.md §8` — per-install, not corpus. Correct routing, but the rule is implicit. | reinforce/new | MEDIUM |

## Already covered (do NOT re-add)

- **Three-sources acumulative / "the zero is data"** — niagara-help (FUENTE 2) carried the hardware fact;
  corpus/code not consulted because the vendor manual answered. Textbook §5.
- **Delegation to compress broad reads** — 2 parallel Explore agents mapping RT + UX is the
  SOURCE/context-compression delegation already in PROMPT-LOOP.
- **Quick-mode for a scoped factual question against a registered target** — already retro'd
  (`2026-09-03-research-sdd-obix-quick-mode-retro.md`). Delta #2 adds only the *drift-into-design* wrinkle.

## Honest scope note

No discovery loop, no block, no CATALOG row, no gap closed. The research-sdd surface actually exercised was
QUICK mode (delta #2) plus delegated analysis (deltas #1, #3). **Delta #1 — read the set's definition
before an enumeration claim — is the real lesson of the session; it cost two operator-caught errors** that
a single 55-line read would have prevented.
