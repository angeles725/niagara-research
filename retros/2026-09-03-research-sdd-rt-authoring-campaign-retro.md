<!-- review-status: pending -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective — RT authoring campaign (B729-B746)

> FIFTH research-sdd retro of the day, distinct subject: a large RT-module authoring campaign — 18 corpus
> blocks (B729-B746) + a PoC + cross-session coordination with the "Codig" build session — that grew out of
> a live defrost-troubleshooting request. READ-ONLY on the kit; PROPOSES only (§18). Captures PROCESS lessons
> that transfer to the loop, not the block content.

## Context
Started as troubleshooting (Cuarto 3 defrost "never enters"), became a research campaign over a MATURE corpus
(720+ blocks). Most framework mechanics were already covered, so the loop's value was APPLICATION/synthesis
(audit our modules vs the corpus) + mining lived incidents. Ran alongside a parallel build session; the
research fed the build (the started() fix) and the build fed the research (a severity correction I got wrong
from a stale decompile).

## Proposed kit deltas

1. **Saturation → pivot to application, and the honest checkpoint is a valid loop output.** Against a mature
   corpus, new "angles" increasingly resolve to "already covered" (oBIX, lexicon). The right move is to
   detect that, CITE the existing block, and pivot the iteration to APPLICATION (our-modules audit vs corpus)
   or STOP with a map — not re-derive. I surfaced "this is covered; the value is now execution" several times;
   that honesty should be an explicit, encouraged loop outcome, not treated as failure to produce a block.
   → PROPOSED METHODOLOGY §8 (stopping) note: "on a mature corpus, saturation + an application pivot or an
   honest map is a legitimate terminal, superior to a thin re-derivation."

2. **`[CERT-a]` for delegated-agent citations — codify it.** Two broad Explore sweeps returned file:line
   citations I did not personally re-open; I marked those `[CERT-a]` (agent-gathered, corpus marker) vs
   `[CERT]` (personally read). This kept evidence honest at scale. → PROPOSED §3 markers: state explicitly
   that a delegated sweep's citations are `[CERT-a]` until spot-verified, and that a load-bearing claim should
   be re-opened to `[CERT]`.

3. **Decompiler-obfuscation is a WALL, not evidence.** docSource for some modules had method names mangled
   (`ln`/`n`: obixDriver, the BIntervalTriggerMode inner scheduler). I could not `[CERT]` internals from them;
   I switched to the vineflower tree for clean names, or marked `[INFER]`, or declined to write a thin block
   (oBIX). → PROPOSED §6/§21: "obfuscated docSource is a tool wall — prefer the vineflower/procyon tree for
   internals; never `[CERT]` a claim off an `ln`-mangled body."

4. **corpus-nav grep false zeros — a tool-reliability trap.** Several `corpus-nav grep`/`find` calls with
   escaped-alternation regex returned EMPTY where content plainly existed; a control query (a known term) and
   a ripgrep fallback exposed it. This is exactly the "falla ≠ cero" hazard. → PROPOSED tools/README + §
   heuristics: "always run a control query before trusting a corpus-nav zero; alternation/escaping breaks it —
   fall back to rg."

5. **Cross-session peer review strengthened evidence (verify-don't-agree in action).** The build session
   caught a real error of mine (BCompressorControl severity "MED-LOW / changed re-arms armTick" — from a
   stale decompile; the live source has armTick only in atSteadyState). I re-verified against the current
   source, confirmed the peer, and corrected the block. → PROPOSED §14 (consistency): when a parallel session
   or the operator disputes a claim, re-open the PRIMARY source (not the decompile that seeded the claim) and
   correct the block with a pointer; a peer catch is first-class evidence.

6. **PoC execution validated a documented claim (§19 in miniature).** Running the pure `DefrostControl` PoC
   (11 JUnit tests, Java-8 bytecode) turned the "extract + test the arming math" plan into executable proof
   of the anti-future-clock guard — in the scratchpad, without touching the module the build session owned.
   → PROPOSED §19: a scratchpad PoC that proves a control-logic claim (and does NOT mutate a shared subject)
   is a cheap, high-value evidence step; prefer it over asserting the logic is correct.

7. **Lived incidents are first-class [CERT-live] evidence.** The highest-value blocks (B739 retype outage,
   B740 `Missing class HoaMode`, B729 defrost late-mount) were anchored in real production incidents, not
   just code reading. → PROPOSED: the loop should actively mine the operator's incident history / bitácoras as
   a source tier, and mark those claims `[CERT-live]`.

## What went well (keep)
- Delegating the two broad organized/ sweeps to Explore kept the parent context clean and produced the
  canonical-idiom evidence (the "anti-pattern = 0 hits in Tridium" finding) that turned a rule from
  anecdotal into canonical.
- Per-block CATALOG regen + engram save after each block kept state consistent across a long run.
- Coordinating docs with the build session (division: research vs client bitácora/retros) avoided duplication.
