<!-- review-status: applied 2026-07-07 · kit dee4b26 (PR #13) -->
# Retro — niagara-research · px-editor-core · 2026-07-06 · Research-SDD self-retrospective

> Run reviewed: focus `px-editor-core`, B210-B214 + synthesis B215 (5/5 gaps C1-C5, STOP §8 exhausted). The
> focus was PLANNED-but-not-started at session open (RESEARCH-STATE + backlog + source pre-flight already
> committed in a prior session, `ca8cd93`); this run executed the plan. Trigger: focus completion (5th focus
> of the PX subsystem to close; also closes the subsystem end-to-end per FOCUSES.md).
> Method: a FRESH-CONTEXT agent read the current kit (`PROMPT-LOOP.md` + `METHODOLOGY.md`) FIRST, then the
> 6 blocks in full, `RESEARCH-STATE-px-editor-core.md`, `FOCUSES.md`, `git log --oneline -8`, and the two most
> recent prior retros (`2026-07-06-px-editor-deep.md`, `2026-07-06-px-editor.md`) to dedupe. READ-ONLY on the
> kit — this report only PROPOSES; kit changes are human-reviewed and human-committed (METHODOLOGY §18).

## Proposed kit deltas

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | `verify-block.sh`'s raw marker tally (`grep -oE '\[INFER\]'` over the WHOLE block text) can be artificially inflated when a §14 cross-block correction QUOTES the prior block's marker literally (e.g. "El `[INFER]` de B211 §211.4 queda acotado") — that occurrence is a META-REFERENCE to a marker being corrected, not a new deduction, yet the script counts it the same as a genuine `[INFER]`. B213 measured this precisely: raw tally 6 `[INFER]`/8 `[CERT]` = 0.75 (would read as "evidence nearly exhausted" per §11's >~0.5 heuristic), but the block's own self-report in RESEARCH-STATE says the real new-deduction count is ≈1 — 5 of the 6 are the correction narrative naming B211's marker. Propose: add to §11's marker-tally guidance (and/or `verify-block.sh`'s ratio explanation) that a block containing a §14 correction must manually subtract meta-references to another block's marker from the raw count and report BOTH numbers (raw + adjusted), instead of leaving that subtraction as an ad hoc rescue the writer happened to do by hand this once. | `METHODOLOGY.md §11` (marker tally / ratio) — possibly also a comment in `toolbelt/verify-block.sh`'s ratio-output section | `RESEARCH-STATE-px-editor-core.md` iter-4 row ("6/8=0.75 **inflado** (5 de 6 [INFER] son meta-refs al marker de B211 en la corrección §14; deducción nueva real ≈1)") · block `niagara-mental-model-bloque213.md` §213.1 (the correction prose itself, which is where the literal `[INFER]` token reappears) · commit `2b8ec28` | new | HIGH |
| 2 | Two of five gaps in this focus corrected a WRONG naming-convention assumption baked into how the class was approached: `Handle` (B213 §213.5) was approached expecting direction/role CONSTANTS (enum-like) and turned out to be a plain 4-field POJO with no constants at all ("No hay constantes NW/N/NE/…"); `BIEnumToSimpleFE`/`BINumericToSimpleFE`/`BIStatusToSimpleFE` (B214, whole block header: "Corrige la suposición de nomenclatura") were approached expecting the `BI`-prefix to mark a Niagara `BInterface` (as it does elsewhere in this same corpus) but are concrete `BWbFieldEditor` classes — the `I` echoes the converter-interface they edit, not the class's own kind. Same failure mode twice in one 5-block focus: inferring a class's KIND (enum / interface / abstract) from its NAME pattern instead of its actual `class`/`interface`/`extends` declaration. Propose: add a line to PROMPT-LOOP step 3 (INVESTIGATE) or METHODOLOGY §9 (golden rules) — a sweep must not assert a class's kind from naming convention; state any name-implied kind as a hypothesis to confirm against the declaration line, not a given premise for the sweep prompt. | `PROMPT-LOOP.md` step 3 (INVESTIGATE) or `METHODOLOGY.md §9` | `niagara-mental-model-bloque213.md` §213.5 ("Handle es un POJO fino de 4 campos, NO un enum de posiciones") · `niagara-mental-model-bloque214.md` header + §214.1 ("Pese al patrón de nombre BI...FE ... los 3 son clases CONCRETAS que extienden BWbFieldEditor") · commits `2b8ec28`, `d4a8023` | new | MEDIUM |
| 3 | §16's focus-index vocabulary (`active`/`paused`/`stopped`) has no name for a focus that is PLANNED (its `RESEARCH-STATE-<focus>.md` + full gap-backlog + source pre-flight already committed) but has ZERO blocks written yet — exactly this focus's state between the prior session (commit `ca8cd93`, "registrar focus PLANIFICADO ... para próxima sesión") and this run's first block (B210). `FOCUSES.md` had no row at all for `px-editor-core` during that dormant window (it only gained a row once the run finished, jumping straight to `stopped`) — the "planned" state was tracked only informally in the commit message and the RESEARCH-STATE header's ad hoc launch note ("Arranque: `/research-sdd niagara-research px-editor-core new` (o `continue` una vez bootstrapeado)"), which had to spell out that `new` here means "start the already-planned focus," not a clean §-BOOTSTRAP. Propose: add `planned` as a 4th focus-index status in §16's table, and one sentence in PROMPT-LOOP's BOOTSTRAP guard clarifying that a focus with an existing `RESEARCH-STATE-<focus>.md`/backlog but 0 committed blocks is `planned`, not `active`/needs-bootstrap, and that its `new` launch arg means resume-the-plan. | `METHODOLOGY.md §16` (focus-index vocabulary table) + `PROMPT-LOOP.md` BOOTSTRAP guard (the "only if the target has NO INDEX.md/RESEARCH-STATE.md" line) | commit `ca8cd93` ("registrar focus PLANIFICADO — infra pxEditor-wb nombrada-no-abierta ... ~39 clases para próxima sesión") · `RESEARCH-STATE-px-editor-core.md` header launch note · `FOCUSES.md` line 21 (only reflects the focus post-completion, `**stopped**`) | refinement | LOW |

For each delta above, one line of rationale (WHY it matters, what it costs, expected impact):

- **#1** — The `[INFER]`/`[CERT]` ratio is the kit's own signal for "is this gap's evidence exhausted" (§11); a mechanical counting artifact that inflates it 6x above the real deduction count (0.75 raw vs ≈0.13 adjusted, roughly 1/8) undermines that signal exactly in the case — a §14 correction block — where a careful reader most needs it to be trustworthy. Cost is one added sentence to the self-report contract (report raw AND adjusted); this run already did the adjustment by hand in RESEARCH-STATE, so codifying it costs nothing new, only makes it mandatory instead of lucky.
- **#2** — Two corrected assumptions in one 5-block focus is a real recurrence, not noise; both wasted a read-then-correct cycle that a one-line caution in the sweep prompt would have prevented for near-zero cost (the sweep would read the declaration first and report the kind as found, rather than assuming it from `BI`/naming pattern and having to walk it back).
- **#3** — Low cost (a vocabulary + one clarifying sentence); prevents the launcher-arg ambiguity this run's own driver had to resolve by judgment alone (the RESEARCH-STATE header's ad hoc launch note is itself evidence the kit doesn't yet name this case), and keeps `FOCUSES.md` from silently omitting a focus that has real committed planning artifacts.

## Already covered (dedupe — proof the retro read the kit first)

- Every heavy sweep (5/5 gaps) delegated at `sonnet` tier for structural comprehension, synthesis (B215) done inline — already covered by `PROMPT-LOOP.md` step 3 DELEGATE + MODEL TIER rule; `RESEARCH-STATE-px-editor-core.md`'s iteration table persists `sí · sonnet` / `no · inline` per row exactly as required.
- Cross-block correction habit (B213 §14-corrects B211 §211.4, keeping the original text and adding "Corregido en [Block 213] §213.1 (§14)") → already covered by `METHODOLOGY.md §14`.
- Closure by remittance for `EventUtil` (cited to B210 §210.6 in both B213 §213.7 and noted in B211/B214) with the explicit "sin sustancia nueva" language → already covered by `METHODOLOGY.md §8` remittance closure category.
- Focus-closing SYNTHESIS block (B215) written immediately after the terminal STOP (5/5) fired, cross-referencing threads across the whole PX subsystem → already covered by `METHODOLOGY.md §8`/`§18` (focus-level exhaustion → optional synthesis block).
- Multi-focus bookkeeping (own `RESEARCH-STATE-px-editor-core.md`, block prefix continuing the GLOBAL numbering from B210, engram topic key `research/niagara/px-editor-core/...`) → already covered by `METHODOLOGY.md §16`.
- Self-verify contract: every block reports a marker tally + ratio + declares block TYPE (evidence vs synthesis) + a token-check count, 0 hallucinations reported across all 6 → already covered by `METHODOLOGY.md §11`. (The one nuance NOT covered — meta-reference inflation of the ratio — is delta #1, not a re-proposal of §11 itself.)
- Audit-first / pre-flight source confirmation for the C1-C5 backlog → already covered by `METHODOLOGY.md §13` + PROMPT-LOOP BOOTSTRAP step e2; this run inherited a backlog where that was already done in the prior session, not repeated here.

## Anti-patterns observed

- A §14-correction block inflating its own `[INFER]`/`[CERT]` ratio by quoting the corrected marker literally → the delta that would prevent/flag it: #1.
- Sweep prompts twice carrying a wrong kind-from-naming-convention assumption (Handle as enum, `BI*FE` as interfaces) that had to be corrected in-block → the delta that would prevent it: #2.

## Metrics

- **Blocks reviewed**: 6 (B210, B211, B212, B213, B214, B215 — all read in full).
- **§14 cross-block corrections in this run**: 1 (B213 → B211 §211.4).
- **Rules skipped in practice**: 0 confirmed (model tier declared every iteration; verify-block.sh + token-check reported every block; artifacts — CATALOG/INDEX/RESEARCH-STATE — updated per the state file).
- **Deltas proposed (new)**: 2 (#1 HIGH, #2 MEDIUM). **Refinements**: 1 (#3 LOW). **Already-covered lessons**: 6.

## Honest verdict

This run executed cleanly against the kit's process machinery (delegation, tiering, §14 correction discipline,
remittance closure, focus-closing synthesis, multi-focus bookkeeping) — nothing new there. Two things earned
genuine kit attention: the ratio-inflation mechanism in #1 is a real, precisely-quantified counting artifact
(0.75 raw vs ≈1-real-deduction) that the run itself caught and corrected by hand in its self-report, which is
exactly the kind of improvised-but-uncodified fix §18 exists to surface. The naming-convention assumption in
#2 recurred twice in a 5-block focus, which is enough to call it a pattern rather than a one-off. #3 is a
real but narrow bookkeeping gap (a focus state the kit's own launch note had to explain ad hoc) — worth fixing
cheaply, not urgent. No item here is invented to look productive; a run this clean does not need to manufacture
deltas, and none of the three above restate something the kit already says.
