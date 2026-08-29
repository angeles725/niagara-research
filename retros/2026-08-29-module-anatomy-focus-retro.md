<!-- kit-retro -->
# §18 Self-Retrospective — focus `module-anatomy` (B629–B636)

**Date**: 2026-08-29 · **Focus**: module-anatomy (niagara-research) · **Blocks**: B629–B636 (8) · **Mode**: heavy/auto, self-paced, Opus driver + sonnet sweeps · **Review-status**: pending
**Outcome**: 8/8 investigable closed (MA1–MA8), investigable=0. One §14 correction issued (B631 → B12). Focus = the code-side DEEPENING of B12/B25 (doc-side breadth).

## What the focus established (one line each)
- MA1/B629 — manifest parsed TWICE by independent readers (install `ModuleManifest`/`BModulePart` vs runtime `NModule`), no converter; `BModule`=per-profile `NModule` map.
- MA2/B630 — profile from manifest attr (not filename; AX modules silently skipped); registry is a PREBUILT binary; `ClassScanner` runs only at rebuild; recursive-DFS dep resolve; `-rp:` profile gate.
- MA3/B631 — type pipeline; **§14 corrects B12**: `module-include.xml` is READ by Slotomatic, no JSR-269 APT; Class loaded once via `ModuleClassLoader`.
- MA4/B632 — real signed-jar skeleton (`META-INF/{MANIFEST.MF,NIAGARA4.SF,.RSA,module.xml}` + dual-namespace classes + profile payload).
- MA5/B633 — install = signature-gated, stop-all-stations, overwrite-in-place stream to `modules/`, no backup/rollback.
- MA6/B634 — palette reader `BModulePaletteNode` lazy/BOG/ungated.
- MA7/B635 — `<permissions>` = base grant + 2 tracks; default `GrantAllPermissionGroupStore` + dev-mode SM softening make it soft in practice.
- MA8/B636 — reference skeleton + chihuahua case study (6 ranked deviations + portable checklist).

## What worked
- **AUDIT-FIRST + PRE-DECLARE REMITTANCES** kept the backlog to 8 genuinely-new gaps over a mature 628-block corpus; the sweep placed B12/B76/B617/signing-chain as remittances up front, so no block re-derived them.
- **GAP-PREMISE-IS-HYPOTHESIS paid off**: MA3's seeded premise ("`NiagaraTypeProcessor` writes module-include.xml") was refuted in-flight → §14 correction to B12 instead of propagating the error. The rule caught a real doc-side mistake.
- **Sonnet sweeps + Opus driver + inline verify** stayed lean across 8 blocks with no compaction; every sweep's load-bearing citations were re-grepped by the driver before authoring (two sweeps had low tool-counts that the verify pass would have caught had they inferred).

## Proposed kit deltas (PROPOSE, do not apply — human review)
1. **[MED] REAL-ARTIFACT-FIRST for packaging/layout/distribution gaps.** MA4 and the MA8 case study only had teeth because the driver ran `unzip -l`/`unzip -p` over the REAL signed jars (incl. the operator's own chihuahua jars + their `sw/` version history), not the decompiled `organized/` tree. Propose a NORMAL-CYCLE note: when a gap is about physical packaging/layout/on-disk artifact shape, inspect the real artifact directly before/alongside the decompiled sweep — decompiled source cannot show `META-INF` signing entries, jar entry taxonomy, or manifest bytes. (Sibling of DISK-FIRST; distinct because it targets the packaged artifact, not the source.)
2. **[LOW] `verify-block` should classify `jar!entry` and `[BNNN]` synthesis citations as a recognized non-resolvable class** (like `extern`), instead of emitting "N [CERT] markers but ZERO file:line resolved — checked nothing." Direct-artifact blocks (MA4/B632) and synthesis blocks (MA8/B636) legitimately cite `control-rt.jar!META-INF/module.xml` and `[B632]`; the current WARN reads as a defect when it is expected. (Scripts-lane; route to the verify-block owner.)
3. **[LOW] Name the COMBINED-SWEEP-FOR-INDEPENDENT-SMALL-GAPS pattern.** MA6+MA7 (two LOW gaps, different subsystems) were swept in ONE sonnet agent returning two labeled sections, then authored as two blocks/two commits. Cheaper than two sweeps, still one-block-one-commit. Propose documenting it under DELEGATE as a permitted batching when gaps are small, independent, and the sweep returns cleanly separated sections. (Not for EVIDENCE gaps that may uncover new gaps mid-sweep.)

## Non-kit execution note (not a delta)
- RESEARCH-STATE row edits repeatedly missed on exact-string anchors (phantom leading spaces, `.`/space differences in the source column). A `python3` replace on a short unique SUFFIX of the row was more reliable than the Edit tool on the full cell. Pure execution friction; no rule change.
