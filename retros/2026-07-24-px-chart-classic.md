<!-- review-status: applied 2026-07-29 · kit cc5e13a -->
# Retro — niagara-research · px-chart-classic · 2026-07-24 · Research-SDD self-retrospective

> Run reviewed: focus `px-chart-classic`, B251-B259 (8 evidence blocks + B259 closing synthesis). Bootstrap
> → STOP in a single session, 2026-07-24. 8/8 gaps closed, coverage ratio 1.00. Trigger: focus-completion (§18).
> Method: fresh-context agent read the current kit (`PROMPT-LOOP.md` full · `METHODOLOGY.md` full) FIRST, then
> `RESEARCH-STATE-px-chart-classic.md` (full), blocks B251/B253/B254/B255/B259 (full), and
> `git log --oneline -12`. Deduped against all 7 existing retros under `niagara-research/retros/`.
> READ-ONLY on the kit — this report only PROPOSES; kit changes are human-reviewed and human-committed (§18).

## Run summary

Focus targets the classic Swing/Workbench charting subsystem (`javax.baja.chart`, 67 distinct classes in
`chart-rt` + `chart-wb`), declared out-of-scope by the four prior PX focuses. Pre-flight e2 correctly
measured 67 classes by collapsing the three decompiler-pipeline trees (raw count would have been ~134). Eight
gaps (H1-H8) closed sequentially; 5 of 8 iterations delegated to a `sonnet`-tier sub-agent. Notable outcomes:
4 Tridium bugs confirmed from decompiled source; 4 sub-agent errors caught by driver token-check (wrong package
path x2, a false "no null guard" claim, a false proven-absence); `BChartBindingCollection`'s sole implementor
found only after widening a module-tree search to the 926-jar universe. The run hit LOOP CONTINUATION issues
across all 8 evidence iterations (driver paused after each block awaiting a human "continuá"), and `--next`
returned STOP from a closed sibling focus rather than from the active one.

---

## Proposed kit deltas

> Only genuinely NEW items — anything the kit already encodes is listed under "Already covered", not here.

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add a `--focus <slug>` selector to `research-sdd-status.sh --next`. Currently line 26 resolves `$state` via `find … -name 'RESEARCH-STATE*.md' \| sort \| head -1`, which in a multi-focus corpus always reads the alphabetically-first focus. In this run, `chihuahua` (stopped, investigable=0) sorts before `px-chart-classic` (8 pending gaps), so `--next` returned `STOP \| read-only-investigable exhausted` while the active focus had work. `verify-state.sh` iterates all focuses correctly (its line ~19 scans all `RESEARCH-STATE*.md`); `--next` should do the same or accept a focus selector. Document the limitation in PROMPT-LOOP step 6: "For a multi-focus corpus, pass `--focus <slug>` to target the active focus's state file; without it, `--next` reads the alphabetically-first state file and may falsely declare STOP while the active focus still has pending gaps." | `toolbelt/research-sdd-status.sh` (line 26 fix) · `PROMPT-LOOP.md` step 6 (one-sentence caveat) | `research-sdd-status.sh` line 26 confirmed by code inspection: `sort \| head -1`; `verify-state.sh` confirmed to iterate all focuses. RESEARCH-STATE-px-chart-classic.md note (line 69): "Formato canónico de 4 columnas exigido por `research-sdd-status.sh`" — the driver was aware of the tool but had to bypass it because `--next` gave a false STOP. | new | HIGH |
| 2 | Add one sentence to PROMPT-LOOP BOOTSTRAP step e (where the backlog is authored) naming the parser's required row shape and its silent-failure risk. The template (`RESEARCH-STATE.template.md` lines 48-52) shows the correct shape, but step e only says "5-15 high-priority gaps" without referencing the format constraint. A 5-column Spanish table (`\| Pri \| ID \| Gap \| Fuente \| Estado \|` with priorities `ALTA` and status `investigable`) was written first; `research-sdd-status.sh` silently ignored all rows; only `verify-state.sh` surfaced the problem. Suggested text at the end of step e: "The parser requires exactly 4 columns (`\| Priority \| Gap \| … \| Status \|`); priority values must be `high`, `medium`, or `low` (not translated); Status must begin with token `pending` for a gap to be treated as investigable. Non-conforming rows are silently ignored — `verify-state.sh` will catch the mismatch, but only if run." | `PROMPT-LOOP.md` BOOTSTRAP step e (closing sentence) | RESEARCH-STATE-px-chart-classic.md line 69 note added by the driver after the format was corrected; template `RESEARCH-STATE.template.md` lines 48-52 shows the shape but is not cited in step e. The initial backlog required a full rewrite before `--next` could parse it. | new | MEDIUM |
| 3 | In PROMPT-LOOP "Two execution modes", add one sentence requiring that the chosen sub-mode be announced to the human at the start of iteration 1 of an orchestrated run. The kit defines supervised ("driver PAUSES after each block") and auto ("driver AUTO-CHAINS"), but neither bullet says to declare the mode. In this run the driver defaulted silently to supervised across 8 iterations; the user had to ask "you stopped the loop yourself, why?" to learn the mode. Suggested text: "At the start of iteration 1 of any orchestrated run, ANNOUNCE the sub-mode to the user: 'I am operating in supervised mode — prompt me after each block to continue' or 'I am operating in auto mode — I will chain until STOP fires.' Without this declaration the user has no basis to expect self-continuation or manual signaling, and may interpret supervised pauses as loop failures." | `PROMPT-LOOP.md` "Two execution modes" section, after the supervised/auto bullets | RESEARCH-STATE-px-chart-classic.md iteration history: all 8 evidence iterations closed with a manual "continuá" from the user. No iteration record shows the driver announcing a mode. PROMPT-LOOP defines both sub-modes but has no announcement obligation. | new | MEDIUM |
| 4 | Add a note to PROMPT-LOOP NORMAL CYCLE step 3 DELEGATE block (and cross-reference in METHODOLOGY §8 negative-closure paragraph): "A sub-agent's proven-absence inherits the sub-agent's search scope, which is narrower than the full corpus. Before promoting a sub-agent negative to a gap closure, the DRIVER must verify that the cited scope covers the relevant universe (e.g. all jars / all modules), not just the swept subtree. A module-scoped 'not found' is evidence for the module, not for the corpus." §8 already says to "cite what you searched and how" — this note is DISTINCT: it tells the DRIVER to WIDEN a sub-agent's scope before accepting it, not just to require citation from the sub-agent. | `PROMPT-LOOP.md` NORMAL CYCLE step 3 DELEGATE block (new bullet after "return ONLY the cited findings") · `METHODOLOGY.md §8` negative-closure paragraph (cross-reference) | B253 §253.8: "BChartBindingCollection — no concrete subclass — dead code. FALSO. El barrido buscó solo dentro del árbol de chart-wb. Ampliada la búsqueda al universo con module-navigator, aparece `BTransformChartBindingCollection` en seriesTransform-wb:67." The sub-agent correctly cited its scope (`chart-wb`); the driver's token-check then widened to 926 jars and found the implementor. | new | MEDIUM |
| 5 | In METHODOLOGY §11 (marker tally sub-section, after "Declare which type"), acknowledge MIXED as a valid third block type alongside evidence and design/applied: "A MIXED block (part evidence, part verdict or synthesis) may be declared as such. For a MIXED block, assess the evidence and synthesis portions separately for the ratio signal: if the evidence portion is exhausted, say so even if the high overall ratio is driven by the synthesis half." Without a named MIXED type, a driver facing a relational-evidence-plus-verdict block may force it into the binary and either misread the 0.5 threshold or suppress an honest exhaustion note. | `METHODOLOGY.md §11` (marker tally sub-section, after the evidence/design binary) | B254 header: "TIPO DE BLOQUE — MIXTO: evidencia relacional + VEREDICTO (§11). Declararlo importa porque el ratio 0.59 en un bloque de evidencia puro señalaría agotamiento [...]"; iteration history it.4: "bloque declarado MIXTO evidencia+veredicto; la evidencia RELACIONAL sí queda agotada." The driver improvised MIXED correctly; it is not named in the kit. | refinement | LOW |

For each delta above, one line of rationale:

- **#1** — A false STOP on a busy active focus is a run-killer: the driver gets a `STOP | read-only-investigable exhausted` from the alphabetically-first closed focus and has no automated path to the real work. `verify-state.sh` already iterates correctly; making `--next` focus-aware closes the gap.
- **#2** — A non-conforming backlog fails silently: `--next` returns BOOTSTRAP (no parseable gaps), with no error message, and the only recovery is `verify-state.sh` — which is edge-triggered on edits, not automatically run at bootstrap. One sentence in step e prevents the silent failure at the point where the author is writing the backlog.
- **#3** — Supervised mode is correct when a human is present; the problem is invisibility. Announcing the mode at iteration 1 costs nothing and removes the ambiguity that made 8 manual prompts look like loop failures.
- **#4** — §11's "trust the self-report" and §8's "cite your scope" are both correct, but together they allow a scope-limited sub-agent negative to be promoted to a corpus-level gap closure. The driver check proposed here is the only place in the workflow where the scope mismatch can be caught.
- **#5** — The §11 binary is a heuristic. MIXED is a common outcome for relational/comparative gaps that ask for a verdict; naming it prevents a future driver from forcing an honest MIXED block into the binary and either inflating an exhaustion signal or hiding one.

---

## Already covered (dedupe — proof the retro read the kit first)

- **Pipeline-inflated counts (raw `.java` files across duplicate decompiler pipelines)** → already covered by `METHODOLOGY.md §13` "Backlog SIZING" paragraph ("`easyBinding 119` was 62 distinct classes; count DISTINCT fully-qualified class names, not raw `.java` files") and `PROMPT-LOOP.md` e2 ("over DECOMPILED code collapse duplicate decompiler-pipeline trees first"). The prose is in the kit and was applied correctly at pre-flight (134→67). The two inflated counts that appeared inside sub-agent sweeps were caught by the existing token-check (§11). No additional prose warning changes the failure mode; token-check remains the correct catch mechanism.
- **Token-check hit rate (4 sub-agent errors caught across 5 sweeps)** → observational data. The kit already mandates the token-check at every iteration (PROMPT-LOOP step 5, METHODOLOGY §11). A numeric track record confirms the gate earns its cost but does not change the rule. No delta warranted.
- **§8 negative-closure evidence bar** → already covered: "cite what you searched and how (paths, counts, the grep/scan that came back empty), not a bare 'not found'." Delta #4 is DISTINCT from this — it adds a driver-level instruction to WIDEN a sub-agent's scope before accepting a negative, not just to require citation. The two operate at different levels.
- **Ratio >0.5 as exhaustion signal for evidence blocks** → already covered by §11: "for an EVIDENCE block (decompilation/reading), a high ratio (>~0.5) signals this gap's investigable evidence is nearly exhausted — say so." Delta #5 adds MIXED as a named type, not a change to the threshold or signal.
- **Focus-closing synthesis block (B259)** → already covered by METHODOLOGY §8: "optionally writing a focus-closing SYNTHESIS block FIRST: a terminal block that consolidates the just-finished focus, cross-referencing related blocks across other focuses."
- **One block per commit, commit message convention** → all 9 commits follow `research(niagara/px-chart-classic): B<n> <slug>`. Already covered by PROMPT-LOOP LOOP CONTINUATION ("ONE BLOCK PER COMMIT") and METHODOLOGY §15 ("Corpus commits use `research(<target>/<focus>): B<n> <slug>`").
- **Model-tier declared on every delegated sweep** → 5/8 iterations delegated to `sonnet`, 3 inline; all tiers recorded in iteration history. Already covered by PROMPT-LOOP MODEL TIER rule and RETURN CONTRACT.

## Anti-patterns observed (optional)

- `--next` returned `STOP` from the alphabetically-first closed focus (`chihuahua`) while the active focus (`px-chart-classic`) had 8 pending gaps → delta #1.
- Backlog authored with a 5-column Spanish table (`ALTA/investigable`); `research-sdd-status.sh` silently ignored it; `verify-state.sh` was the only surfacing path → delta #2.
- Orchestrated run defaulted silently to supervised mode: 8 manual "continuá" prompts from the user before the pattern was recognized → delta #3.
- Sub-agent H3 sweep declared "BChartBindingCollection has no concrete subclass — dead code" after searching only `chart-wb`; driver's token-check widened to the 926-jar universe and found `BTransformChartBindingCollection` in `seriesTransform-wb` → delta #4.

## Metrics

- **Blocks reviewed**: 9 (B251-B259, plus full RESEARCH-STATE iteration history) · **§14 cross-block corrections in this run**: 6 (it.1 §251.9 corrects 2 sub-agent claims; it.3 §253.7-8 corrects B252 §252.5 + sub-agent false proven-absence; it.4 §254.3 + §254.8 dual §14; it.5 corrects B254 §254.3; it.6 §14 to B254 §254.8)
- **Rules skipped in practice**: 1 (LOOP CONTINUATION — suspended per supervised mode, which is correct, but mode was never declared)
- **Deltas proposed (new)**: 4 new + 1 refinement · **Already-covered lessons**: 7

## Honest verdict

This run surfaces one genuine tool defect (#1: focus-blind `--next`) and one operational gap (#3: undeclared
execution mode) that the kit currently has no rule for. Both are mechanical: #1 has a one-line code fix; #3
has a one-sentence announcement rule. Deltas #2 and #4 are smaller but real: #2 prevents a silent bootstrap
failure that already cost one backlog rewrite, and #4 names the §8+§11 tension that a sub-agent's scope
creates for proven-absence. Delta #5 is the smallest — a named MIXED type that the driver improvised correctly
but the kit doesn't formalize. The seven already-covered items confirm the kit is sound on pipeline counts,
token-check mandate, exhaustion signals, synthesis blocks, and commit discipline — this run exercised all of
them correctly.
