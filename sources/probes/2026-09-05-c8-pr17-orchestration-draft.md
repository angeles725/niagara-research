# C8 PR17 — `build-n4-module-kit/ORCHESTRATION.md` apply-ready CONTENT draft

> For the PR17 apply worker: this is the doc-only body of a NEW kit file `ORCHESTRATION.md` (wave3.md D14;
> doc-only, L7 resolved after wave-3 merges). Place it VERBATIM. Every process claim is cited to a retro slug,
> a corpus block, a kit file/§, or the CLAUDE.md model table — no invented process. Two source notes:
> - `wave3.md`/D14 is the PR spec (the lead's scratchpad); the CONTENT below is grounded in citable artifacts.
> - There is NO dedicated "orchestration" retro; the orchestration doctrine cites the multi-session-coordination
>   retro (folded at `METHODOLOGY.md` §Multi-session coordination) + the research-sdd retro-automation retro.

---
=== BEGIN ORCHESTRATION.md ===

# Orchestration — research-sdd · gentle SDD · BUILD-LOOP, and how they hand off

This kit is one of THREE tools a run may use. They are not alternatives to pick between once; a mature change
flows THROUGH all three. This file says WHEN each applies, WHICH model runs each phase, and HOW they hand off.

## The three tools — when each applies

| Tool | Use it to | Produces | Marker of "done" |
|---|---|---|---|
| **research-sdd** | answer an open technical question against the framework (three sources: corpus + niagara-help + decompiled code) | a numbered `[CERT]`/`[INFER]` block ending in a **Kit implication** | a self-verify table with every claim marked; a named gap if not closed `[ev: research-sdd METHODOLOGY §3/§8/§11]` |
| **gentle SDD** | turn a decided change into a durable, reviewable contract | `proposal → spec → design → tasks`, ledgered attempts | spec requirements have scenarios; tasks map to spec; verify passes `[ev: CLAUDE.md SDD workflow]` |
| **BUILD-LOOP** (this kit) | build / verify / deploy an actual N4 module | a signed Java-8 jar past the verify gate + a per-module `BUILD-STATE` envelope | `verify-module.sh` passes; the HARD close gate (§7) is satisfied `[ev: BUILD-LOOP.md §5/§7]` |

Rule of thumb: **research-sdd finds the WHY, gentle SDD fixes the WHAT/contract, BUILD-LOOP produces the
artifact.** A one-line mechanical edit skips straight to BUILD-LOOP; a novel framework behavior starts in
research-sdd; a multi-file change with real ambiguity earns a gentle-SDD proposal first.

## Which model runs each phase

Gentle-SDD phase models are fixed by the CLAUDE.md **Model Assignments** table `[ev: CLAUDE.md Model Assignments]`:

| Phase | Model | Why |
|---|---|---|
| sdd-explore | sonnet | structural reads, not architectural |
| sdd-research | sonnet | collects source-backed evidence |
| sdd-propose | **opus** | architectural decisions |
| sdd-spec | sonnet | structured writing |
| sdd-design | **opus** | architecture decisions |
| sdd-tasks | sonnet | mechanical breakdown |
| sdd-apply | sonnet | implementation |
| sdd-verify | sonnet | validation against spec |
| sdd-archive | haiku | copy + close |
| jd-judge-a / jd-judge-b / jd-fix-agent | sonnet | adversarial review + surgical fixes |
| default (generic delegation) | sonnet | fallback |

**research-sdd investigation lanes** (the corpus-block authoring that feeds the pipeline) run on **opus** — that
is a heavier reasoning task than gentle-SDD's `sdd-research` PHASE (sonnet), which only collects external
evidence. Do not conflate the two: `sdd-research` = a sonnet sub-agent phase; a research-sdd lane = an opus
authoring session. `[ev: CLAUDE.md Model Assignments; team practice campaign-8]`

## How the three hand off — one delta, end to end

The campaign-8 pipeline, each arrow a real artifact:

```
research-sdd [CERT] block  →  gentle-SDD spec requirement  →  QA RED test  →  sdd-apply (→GREEN)  →  retro  →  fold
     (Kit implication)          (proposal/spec/design)         (a biting,        (implementation)     (new-retro   (promotion into
                                                                mutation-proven                        .sh stub)    the kit core)
                                                                failing test)
```

1. **Research block → spec requirement.** A `[CERT]` block's **Kit implication** names the target kit file/§ and
   an `[ev: corpus B<n>]` token; that becomes a gentle-SDD spec requirement (with a scenario). `[ev: corpus B801/B806/B815 Kit-implication sections]`
2. **Spec → RED.** QA authors a test that FAILS on the current tree and BITES only on the real defect
   (mutation-proven), pinned to a named branch, not a stale hash. `[ev: METHODOLOGY.md K2, K13]`
3. **RED → apply (GREEN).** `sdd-apply` (sonnet) implements to green; the automated half of the gate is
   `verify-module.sh` — a jar that has not passed it does not go to a station. `[ev: BUILD-LOOP.md §5]`
4. **Apply → retro.** The run writes its retro via `new-retro.sh` (the per-run precondition, below), capturing the
   proposed kit delta as `propose-never-apply`. `[ev: retro research-sdd-retro-automation; PR16 draft]`
5. **Retro → fold.** A PROMOTION PR folds the proposed delta into the kit core under the §7 close gate exit (c),
   and the folded doc line carries `[ev: retro <slug>]` — which `sweep-fold-audit.sh` harvests to justify flipping
   the retro's INDEX row to `folded`. `[ev: BUILD-LOOP.md §7; toolbelt/sweep-fold-audit.sh]`

Boundaries between concurrent lanes are enforced by the multi-session rule: **check the tree before editing a
shared file — a dirty working tree is a peer's live work, off-limits.** `[ev: retro dashboardpan-2d-to-3d-port · METHODOLOGY.md §Multi-session coordination]`

## The closing step — a per-run retro/ticket loop (every run, not at STOP)

Every run ENDS by writing its retro; the retro is a precondition for "done", not an at-STOP afterthought.

- `toolbelt/new-retro.sh <module|kit> <slug>` emits the retro stub (What happened / Evidence / Proposed kit deltas
  table / Lessons) and appends its `retros/INDEX.md` row (`pending`). `[ev: PR16 draft §1/§2]`
- A defect in a KIT CHECK or DOCTRINE (a lint that misses/over-fires, a stale rule) additionally opens
  `toolbelt/kit-ticket.sh "<one line>"` (labels `kit`/`from-run`/`campaign-9`). `[ev: PR16 draft §3]`
- `toolbelt/sweep-build-state.sh --age` at orient (BUILD-LOOP §0.a) surfaces the accrued retro DEBT so it cannot
  be skipped across a continuous chain. `[ev: PR16 draft §4]`

WHY this is a hard loop and not a manual habit: §-close retros fire only at STOP / focus-close, but a continuous
lead-delegated chain (one unit → next task → next unit) NEVER reaches a STOP, so the trigger never arms — observed
live at ~8:1 (units landed : retros written) until the operator asked why. The debt counter makes the retro
un-skippable, same shape as the verify gate. `[ev: retro research-sdd-retro-automation §A]`

=== END ORCHESTRATION.md ===

---
## Apply-worker notes
- Place the block between the BEGIN/END markers as `build-n4-module-kit/ORCHESTRATION.md`; drop the markers.
- doc-only (D14); no toolbelt change here — the `new-retro.sh`/`kit-ticket.sh`/`--age` references are satisfied by
  PR16 (land PR16 first, or this doc's tool references dangle until it does).
- Route it: name `ORCHESTRATION.md` in `skill/SKILL.md` § References so `kit-links.bats` sees it (K19).
- Close: PR17 is a kit change → its own `new-retro.sh kit c8-pr17-orchestration` + `retro_pending` in the kit
  self-envelope, same push range (envelope-pairing, BUILD-LOOP §7).
- Fidelity note for investigador1: the ONLY place I departed from the lead's brief is the model table — the lead's
  note said "research lanes opus"; the CLAUDE.md table says `sdd-research`=sonnet. I kept BOTH, scoped: gentle-SDD
  `sdd-research` phase = sonnet (table), research-sdd corpus lane = opus (practice). Confirm this split is intended.
