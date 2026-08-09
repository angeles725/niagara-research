<!-- review-status: pending -->
# Retro — niagara-research · database · 2026-08-09 · Research-SDD self-retrospective

> Run reviewed: database focus B402–B413 (11 blocks + synthesis). Trigger: focus-completion (METHODOLOGY §18).
> Method: a fresh-context agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) first, then the
> run's blocks, commits, and §14 corrections, and proposes kit deltas. READ-ONLY on the kit — this report
> only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is listed under "Already covered", not here.
> Each delta: the concrete change · the target file/section · evidence · priority.

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add to SELF-VERIFY (step 5): when a §14 correction was issued this iteration, the gated checklist must include "confirm the OLD BLOCK was actually edited to add the back-pointer note ('corrected in BN') — not just documented in the new block's Connections". List it as a required artifact check alongside block file, CATALOG, INDEX, RESEARCH-STATE | `PROMPT-LOOP.md` — NORMAL CYCLE step 5 (SELF-VERIFY, Artifacts bullet) | commits 130abd2 (B5←B402), 3773531 (B33←B407), 5584d72 (B5←B411) were SEPARATE commits added by the driver AFTER the block committed; commit b68f30a (B410→B33) did it correctly in-iteration — 3/4 corrections omitted the old-block edit | new | HIGH |
| 2 | Add to UPDATE STATE (step 6): mandate `research-sdd-status.sh $TARGET --sync-state` to recompute and WRITE `covered_blocks` (and other envelope counts) before committing — never compute the count by hand or via `fd`/`find` which may use a different block-counting regex than `verify-state.sh` | `PROMPT-LOOP.md` — NORMAL CYCLE step 6 (UPDATE STATE, after "Update the coverage METRIC") | commits 5408a17 ("reconcile covered_blocks to 398"), 130abd2 ("B402 + reconcile block count 397"), d6b4ea0 ("enqueue DB11 + reconcile count 399") — 3 standalone reconcile commits in 11 blocks; root cause: `fd 'bloque[0-9]+\.md$'` = 408 while `verify-state.sh` counts 409 (includes `niagara-mental-model-bloque75-security-incident.md` via `-[[:alnum:]_-]+` suffix extension) | new | MEDIUM |
| 3 | Add to STOPPING §7 (synthesis/terminal trigger): gaps named in the synthesis block's report as child gaps MUST be registered in RESEARCH-STATE in the SAME commit as the synthesis block (or the immediately-following archive commit); a gap named only in the synthesis report but absent from RESEARCH-STATE is lost at session end per §8 ("a named child gap can be closed; 'pending' is lost") | `PROMPT-LOOP.md` — NORMAL CYCLE step 7 (STOPPING, synthesis option) | commit 5455067 (B413 synthesis) named 3 child gaps (DB-G1/G2/G3) only in the synthesis block; a separate driver commit a852383 was required to register them in RESEARCH-STATE — the synthesis agent applied step 6's "REGISTER the NEW gaps uncovered" to normal blocks but did not apply it at the terminal synthesis step | new | MEDIUM |
| 4 | Add `--state <RESEARCH-STATE-file>` (or `--focus <slug>`) argument to `verify-state.sh` so the EDGE-TRIGGERED lint can be scoped to the active focus's state file. Update PROMPT-LOOP step 6 to use it: `verify-state.sh $CORPUS --state $CORPUS/RESEARCH-STATE-<focus>.md`. Rationale: the current step 6 says "run `verify-state.sh $CORPUS` (cheap — one file)" but the implementation scans ALL RESEARCH-STATE*.md under the target; in niagara-research's 28-focus corpus every iteration generates FAIL noise from unrelated focuses (protocols bad-priority-value, modbus/nmodsreflow/live-station missing block_scope:shared-global, webChart/jsonToolkit stale covered_blocks) | `toolbelt/verify-state.sh` (new arg) + `PROMPT-LOOP.md` step 6 (scoped invocation) | running `verify-state.sh /home/cristian/niagara-research` during database-focus iterations produced 7+ FAIL lines from unrelated focuses; the driver instructed sub-agents to "set covered_blocks to whatever makes verify-state PASS" — a workaround that corrupted the intent of the linter; PROMPT-LOOP parenthetical "cheap — one file" documents single-file intent already | new | LOW |

For each delta above, one line of rationale (WHY it matters, what it costs, expected impact):

- **#1** — The old-block back-pointer is the §14 rule's visible audit trail; without a self-verify gate it is silently skipped 75 % of the time (3 of 4 corrections). Cost: one extra bullet in the step 5 checklist. Impact: corrections become single-commit, self-evident, and auditable by `git show`.
- **#2** — Using a different block-counting regex than verify-state.sh guarantees drift whenever a non-standard block name exists in the corpus. `--sync-state` uses the SAME logic as verify-state.sh and eliminates the discrepancy by construction. Cost: one mandatory command per iteration. Impact: zero standalone reconcile commits.
- **#3** — A synthesis agent that applies step 6 to normal blocks but not to the terminal step loses every child gap it named. The §8 rule ("named child gap can be closed; 'pending' is lost") already states the consequence; making the registration requirement explicit at the terminal step closes the gap. Cost: one sentence in step 7. Impact: child gaps registered consistently without driver intervention.
- **#4** — A linter that FAILs on pre-existing unrelated focuses is noise, not signal; agents working around it by tuning covered_blocks to satisfy the noise are inverting the tool's purpose. A `--state` argument keeps the edge-triggered lint cheap AND focused. Cost: ~10 lines in verify-state.sh. Impact: the parenthetical "cheap — one file" matches the actual behavior.

## Already covered (dedupe — proof the retro read the kit first)

> Lessons this run surfaced that the kit ALREADY encodes. Listing them proves the dedupe ran and prevents
> re-proposing baked-in rules.

- The §14 back-pointer RULE itself (adding "corrected in BN" to the old block) is already encoded — METHODOLOGY §14 states it explicitly ("keep the original text, add a note 'corrected in BN' + the new citation") and PROMPT-LOOP step 4 repeats it. Delta #1 proposes only the SELF-VERIFY gate that enforces it, which the kit does not yet have.
- The requirement to register new gaps every iteration is already in PROMPT-LOOP step 6 ("REGISTER the NEW gaps uncovered"). Delta #3 proposes only the explicit repetition of this requirement at the TERMINAL synthesis step, where it is currently implied but not stated.
- Using `verify-state.sh` for edge-triggered lint is already mandated (PROMPT-LOOP step 6 EDGE-TRIGGERED LINT). Delta #4 proposes only the scoping argument that the step's parenthetical already implies.
- The audit-first backlog pattern (BOOTSTRAP step e) was followed correctly — the bootstrap sub-agent produced a coverage matrix and seeded 10 well-shaped gaps before the first block.
- Model-tier discipline (haiku for mechanical enumeration, sonnet for structural comprehension) was applied correctly in all 11 iterations as recorded in the iteration history.
- Child-gap classification (blocked-on-source-missing, requires-execution) at STOP was performed correctly per §8 — the gaps were correctly typed; only their REGISTRATION path was incomplete (delta #3).

## Anti-patterns observed

- **covered_blocks by eye**: sub-agents incremented covered_blocks by +1 from the prior iteration instead of using the authoritative computation → delta #2 prevents it.
- **§14 old-block edit omitted**: sub-agents documented the correction in the new block's Connections but did not edit the old block → delta #1 gates it.
- **synthesis child-gap in report only**: the synthesis agent named child gaps in its prose report but did not write them to RESEARCH-STATE → delta #3 makes the requirement explicit at that step.
- **verify-state.sh noise accepted as normal**: the driver reconciled covered_blocks to satisfy unrelated-focus FAILs rather than questioning why the linter fires on other focuses → delta #4 removes the noise at source.

## Tools built, adapted, or outgrown

No tools were built, adapted, or outgrown in this focus. The run used only the standard decompilation pipeline (vineflower, decompile-java.sh) and the kit toolbelt. No `tools/` entries created.

| # | CREATED | ADAPTED | OUTGREW | ORACLE | VERDICT |
|---|---|---|---|---|---|
| T1 | — | — | — | — | `no` — no tool work in this focus |

## Metrics

- **Blocks reviewed**: 12 (B402–B413, including synthesis)  ·  **§14 cross-block corrections in this run**: 4 (B402→B5, B407→B33, B410→B33, B411→B5)  ·  **Rules skipped in practice**: 2 (old-block back-pointer edit × 3 iterations; child-gap RESEARCH-STATE registration × 1 synthesis)
- **Deltas proposed (new)**: 4  ·  **Already-covered lessons**: 6

## Honest verdict

The run was substantively clean — 11/11 gaps closed with good citation discipline, correct model-tier selection, and no [CERT] hallucinations. The four frictions are all procedural / tooling (not epistemic), are small in scope, and each has a targeted one-sentence or one-argument fix. Proposing all four is honest; none is invented to look productive.
