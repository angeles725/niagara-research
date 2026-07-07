<!-- review-status: applied 2026-07-07 · kit dee4b26 (PR #13) -->
# Retro — niagara-research · px-editor-deep · 2026-07-06 · Research-SDD self-retrospective

> Run reviewed: focus `px-editor-deep`, B198-B208 + synthesis B209 (11/11 gaps, STOP §8 exhausted).
> Trigger: focus completion (also corpus checkpoint — 4th focus of the px subsystem to close).
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then the
> run's blocks/commits, and proposes kit deltas. READ-ONLY on the kit — this report only PROPOSES; kit
> changes are human-reviewed and human-committed (METHODOLOGY §18).

## Proposed kit deltas

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Backlog sizing must come from an ACTUAL count (`find <dir> -name "*.java" \| wc -l`), not example-based estimation — and must disambiguate nested dirs sharing a name. Two of five "D" gaps were size/attribution errors, not just off-by-a-bit: D2 `studio/` was planned as "6" classes, real was 61 (10x undercount, B205 says so explicitly: "El backlog subestimó el tamaño (decía 6; son 61)"); D4 `commands/` was planned as "36" with examples `MoveWidget`/`MorphWidget`/`Align`/`Reorg` — but those classes actually live in the NESTED `studio/commands/` (covered by D2/B205), while the real top-level `commands/` (D4/B206) has a DIFFERENT, smaller set of 14 classes (Insert/Delete/Rename/…). The plan conflated two directories of the same name at different nesting depths. | `METHODOLOGY.md §13` (audit-first backlog) + `PROMPT-LOOP.md` BOOTSTRAP step e2 | B205 header (self-reported miscount) · B206 header ("14 clases… FUERA de `studio/`") · commit `000229b` (original plan: D2="6", D4="36" w/ studio-commands examples) vs commits `c2247cb` (B205, 61) / `89ad213` (B206, 14) | new | HIGH |
| 2 | When counting classes-in-module for backlog sizing, exclude/collapse DUPLICATE decompiler-pipeline output trees (e.g. procyon + vineflower parallel copies of the same sources) — count distinct FQCNs, not raw `.java` files across all pipelines. X6 `easyBinding` was pre-counted at 119 classes; the real count was ~62 (rt=11 + wb≈46 + ux=5), the gap being duplicated procyon+vineflower trees. | `METHODOLOGY.md §6` (research tools / decompile) or `§13` (same section as #1) | B207 header: "clases reales rt=11/wb≈46/ux=5 (el conteo previo de 119 incluía duplicados de pipelines procyon+vineflower)" · commit `4871af2` | new | MEDIUM |
| 3 | Broaden §5's "obfuscated string ⇒ `[INFER]`, structure ⇒ `[CERT]`" rule beyond its current APK/DEX-only framing. B207 (easyBinding) applied the exact same discipline correctly to a plain DECOMPILED JAR (Vineflower, not APK/DEX) that uses a custom runtime `z[]` XOR/array string-obfuscation scheme (unrelated to ProGuard/R8/DexGuard) — proving the principle generalizes to any artifact with runtime-decoded string literals, not only mobile bytecode obfuscators. | `METHODOLOGY.md §5` ("Obfuscated bytecode (APK/DEX, .NET, etc...)" heading/scope) | B207 §207.4: "los VALORES de strings obfuscados... son `[INFER]`" for `z[]`-decoded brand names/tags, `[CERT]`/`[CERT-estructura]` for the mechanism (`EncryptDecrypt.java:44`, AES via `SecretKeySpec`) · commit `4871af2` | refinement | LOW |
| 4 | Name the "hybrid closure" case explicitly: a gap can close by blending REMITTANCE (for the part already answered by prior blocks) and NEW investigation (for a genuinely novel part) WITHIN THE SAME block — distinct from §8's current framing of new/absence/remittance as three mutually-exclusive per-gap outcomes. D4 (`commands/`, B206) did exactly this: cited B198/B201/B205 by remittance for the `Command`/`Artifact` pattern, while contributing new substance (Insert family, AddResponsive/AddBorder wrappers) for the rest of the SAME gap — the block said so explicitly but the kit has no vocabulary for it. | `METHODOLOGY.md §8` (closure categories) | B206 header: "el patrón... ya está establecido... → se cierra por REMISIÓN; este bloque documenta la SUSTANCIA NUEVA" · commit `89ad213` | refinement | LOW |

For each delta above, one line of rationale (WHY it matters, what it costs, expected impact):

- **#1** — A backlog that mis-sizes or misattributes a gap by 10x (or points at the wrong directory) burns iteration budget guessing scope before the first block; a cheap `find`+`wc -l` per candidate directory at bootstrap/audit time (plus explicitly listing sibling dirs with the same basename) would have caught both errors for near-zero cost. Impact: more accurate priority ordering and iteration-count expectations at focus start.
- **#2** — A 2x inflated class count for a module (119 vs 62) skews priority/effort estimates and can make a well-scoped gap look artificially large. Cheap fix: when the toolbelt's decompile wrapper produces multiple pipeline outputs for the same jar (procyon+vineflower), the counting step should note that duplication once, so every subsequent size estimate for that module isn't off by ~2x.
- **#3** — Costs nothing (it is a scope/title clarification, not new logic) and prevents a future operator from thinking the obfuscated-string discipline only applies to APK/DEX and skipping it for a "regular" decompiled jar that happens to obfuscate its own strings.
- **#4** — Low cost (vocabulary only); makes it explicit that "closed by remittance" and "closed by new investigation" are not mutually exclusive at the block level, so future retros don't flag a well-executed hybrid closure (like B206) as a rule violation for want of a name.

## Already covered (dedupe — proof the retro read the kit first)

- Delegating heavy sweeps to `sonnet` for structural comprehension (used on 8 of 11 gaps) and doing light gaps inline (X3, D4, B209 synthesis) → already covered by `PROMPT-LOOP.md` step 3 (DELEGATE) + MODEL TIER rule; the run's `delegado?·tier` column in RESEARCH-STATE persisted this correctly per gap.
- Discovering X6 (`easyBinding`) mid-investigation of X2 (`template`) and registering it as a new gap on the fly → already covered by Golden Rule 7 (§9) and PROMPT-LOOP step 6 ("REGISTER the NEW gaps uncovered").
- Writing a focus-closing SYNTHESIS block (B209) after the terminal STOP fired at B208, before any corpus-level handoff → already covered by `METHODOLOGY.md §8` "Closed loop while working, open loop when done" (focus-level exhaustion → optional synthesis block).
- B201's "two parallel chart systems" (classic `javax.baja.chart` vs `webChart`) framed correctly as a CONTRAST/coexistence, not a refutation of B199 → already covered by `METHODOLOGY.md §14` REFUTE-vs-CLARIFY-SCOPE distinction (correctly NOT invoked as a correction, since no prior claim was wrong).
- Verify-block.sh's `extern` marking of decompiled/beautified citations, still requiring a manual token-check → already explicitly covered by `METHODOLOGY.md §11` ("a citation to a beautified-temp / decompiled / snapshot path shows as `extern`... the agent still confirms those... by reading the cited source"). No delta needed.
- AUDIT-FIRST BACKLOG seeding for the new focus (coverage-audit at prior session close, backlog committed in `000229b` before the focus started) → already covered by `PROMPT-LOOP.md` BOOTSTRAP step e / `METHODOLOGY.md §13` "Audit-first as a backlog seed."
- Multi-focus bookkeeping (own `RESEARCH-STATE-px-editor-deep.md`, `FOCUSES.md` entry, focus-aware block naming) → already covered by `METHODOLOGY.md §16`.

## Anti-patterns observed

- The original backlog (commit `000229b`) sized D2/D4 wrong and misattributed D4's examples to a sibling directory → the delta that would prevent it: #1.
- The easyBinding pre-count folded duplicate decompiler-pipeline trees into one class count → the delta that would prevent it: #2.

## Metrics

- **Blocks reviewed**: 11 (B198-B208) + 1 synthesis (B209) = 12.
- **§14 cross-block corrections in this run**: 0 (one CONTRAST was correctly framed as non-refuting, B201 vs B199).
- **Rules skipped in practice**: 0 confirmed (no evidence of a skipped gate; verify-block.sh usage could not be confirmed or denied from committed artifacts alone, since its output is a self-report, not a committed file — not counted as a skip without evidence).
- **Deltas proposed (new)**: 2 (#1, #2). **Refinements**: 2 (#3, #4). **Already-covered lessons**: 7.

## Honest verdict

This run is well-covered by the existing kit for its PROCESS (delegation, tiering, synthesis-at-focus-end,
closure vocabulary, obfuscation-string discipline in principle). The genuinely new material is narrower and
sits at the BOOTSTRAP/backlog-SIZING step, not the iteration loop itself: two of five "D" gaps in the
pre-flight plan (`000229b`) were wrong by an order of magnitude or pointed at the wrong directory (studio 6→61,
commands 36→14-plus-misattribution), and one gap's pre-count folded duplicate decompile-pipeline output
(easyBinding 119→62). The kit's current pre-flight gate (§b2/e2) checks that a gap's source EXISTS and is
REACHABLE, but not that its declared SIZE is measured rather than guessed from partial exploration — that is
the one real gap. The other two proposed items (#3, #4) are honest but low-stakes scope/vocabulary
refinements to sections that already encode the right instinct.
