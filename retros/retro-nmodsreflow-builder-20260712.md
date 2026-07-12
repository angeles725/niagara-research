<!-- review-status: pending -->
# Retro — niagara-research · nmodsreflow-builder · 2026-07-12 · Research-SDD self-retrospective

> Run reviewed: focus `nmodsreflow-builder`, B216-B227 (12 blocks, 12/12 gaps closed, FOCUS COMPLETE). Trigger:
> focus-completion (also corpus-level candidate — no other focus queued as of this run).
> Method: fresh-context read of `PROMPT-LOOP.md` (436 lines) + `METHODOLOGY.md` (914 lines, both full) FIRST,
> then `RESEARCH-STATE-nmodsreflow-builder.md`, blocks B216/B217/B224/B226 in full, and `git log --oneline -14`.
> READ-ONLY on the kit — this report only PROPOSES.

## Proposed kit deltas

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add the INVERSE case to "Bundle-evidence quality": a **negative** grep for a bare library name over a bundle where imports are referenced by **webpack module IDs** (not string names) does NOT prove absence — only an idiom/API/tag-level search does. Also nuance the "Beautified-temp citation" claim that minification preserves "string LITERALS 1:1" — that holds for the code's OWN literals, not for a THIRD-PARTY library reached only via an internal `require(<numeric-id>)` map, whose name never appears as a string at all. | `METHODOLOGY.md §5` ("Bundle-evidence quality" + "Beautified-temp citation") | B216 §216.4 claimed `d3`=0 hits (grep negative) → B224 §224.4 refuted it: d3 IS present, aliased under webpack module ids `cb29`/`898b`; the only literal `"d3"` was an unrelated query-param collision. §14 correction, commit `ac6712a`. | new | HIGH |
| 2 | Note that certainty RANK ≠ INFORMATIVENESS rank for a breadth/catalog question: when the research question is "what is the FULL SET of X" (a catalog, an enum), actively prefer a real, POPULATED artifact (even a lower-ranked `[CERT]` disk config) over a thin/default `[CERT-live]` instance — the live system's default state can under-report breadth even though it outranks static evidence for identity/protocol questions. | `METHODOLOGY.md §3` (certainty markers) or §8 (evidence discipline) | B217 §217.8 validated the SCHEMA against the live station's default dashboard (2 cards) `[CERT-live]`; B218 got the actual CATALOG BREADTH (20 types, 10 observed) from a real on-disk config `HoneywellMX605132026/shared/reflow/config.json` (26 cards) that the live default could never have shown. RESEARCH-STATE "Reordenamiento" note; commit `79f152a`. | new | MEDIUM |
| 3 | Name the "live backlog injection" pattern distinctly from §8's "Reopening a STOPPED loop": when the user adds new questions WHILE a focus is still ACTIVE (not stopped/exhausted), the loop simply appends/widens the current backlog (renumbering as needed) — no new bootstrap, no new authorization, no separate budget cap. This is lighter than reopening a stopped focus and isn't currently named. | `METHODOLOGY.md §8` (near "Reopening a STOPPED loop") | RESEARCH-STATE header note "backlog ampliado 2026-07-12: BG11 → chihuahua; +BG13 modernización"; iteration-history row 6: "BG13 modernizacion, pedido usuario; BG11 ampliado a chihuahua-builder" (commit `5ebb5ad`). | new | MEDIUM |

For each delta above, one line of rationale:

- **#1** — This is the cheapest possible bug to prevent (one grep) and the most expensive to leave uncaught (a false "absence" claim shipped as `[CERT]` until a later block happens to touch the same code). It also tightens a claim in §5 that is currently slightly wrong for webpack-bundled (not just minified) JS.
- **#2** — Saves a future run from trusting a live system's DEFAULT/empty state as if it were representative; a one-line caveat prevents under-cataloguing.
- **#3** — Gives the loop a name for something it already does correctly (this run handled it fine ad hoc) so future runs don't hesitate or wrongly treat it as a reopen requiring fresh authorization.

## Already covered (dedupe — proof the retro read the kit first)

- **Reusing one beautified temp across multiple/parallel sweeps** (BG9 sweep + BG4/BG6 sweeps all cited the same `scratchpad/reflow-app.beauty.js`, sha256-anchored) → already covered by `METHODOLOGY.md §5` "Beautified-temp citation" (a beautified temp is explicitly meant to be cited across MANY blocks as if it were the primary source; the file is read-only so concurrent sweeps don't conflict).
- **Validating a static block against a live station mid-focus, recording the version divergence (static 1.7.7.75 vs live 1.7.5-43)** → already covered in substance by `METHODOLOGY.md §3` ("the live system wins" / `[CERT-hw]`/`[CERT-live]` outranks `[CERT]`) + §12 "Re-measure ground-truth live, never inherit it" + §14 (REFUTE vs CLARIFY-SCOPE; a live finding can also just CONFIRM a static claim, which is what §217.8 did). Doing it as a subsection of the SAME block/iteration rather than a later separate block is a minor stylistic variant, not a rule gap — no delta proposed.
- **SECRETS DISCIPLINE on a real client production config** (B217's probe cites structure only — `cards[]` shape, key names, counts — and redacts building/equipment names, coordinates, branding) → already covered by `PROMPT-LOOP.md` "SECRETS DISCIPLINE (live-install targets)" + its REDACTION CHECKLIST, and `METHODOLOGY.md §12b` ("Data discipline extends to response bodies... cite STRUCTURE, never VALUES"). Textbook-correct application; nothing to add.
- **verify-state FAILs across the whole niagara-research corpus** (every `RESEARCH-STATE-*.md`, including this focus's, fails `no research-state.v1 envelope`) → checked directly by running `toolbelt/verify-state.sh` against the corpus: this is a **corpus-wide legacy-migration issue**, not specific to this run, and the tool's own error message already names the fix (`research-sdd-status.sh --sync-state`). This is an unrun migration, not a kit gap — no methodology delta proposed; flagging it here only so it isn't silently lost (an operational TODO for the corpus, not for the kit).

## Anti-patterns observed (optional)

- None rising to the level of a kit-rule violation. The one real miss (B216's false "d3 absent") was self-caught by the corpus's own §14 mechanism eight blocks later (B224), exactly as §11/§14 predict cross-block correction — not a per-block gate — is the real error-capture path. Delta #1 exists to shrink the WINDOW of that kind of miss, not to add a new gate.

## Metrics

- **Blocks reviewed**: 12 (B216-B227), read in full: B216, B217, B224, B226 · **§14 cross-block corrections in this run**: 2 (B224→B216 §216.4 d3; B224→B218 §218.3 circle/iView)
- **Rules skipped in practice**: 0 observed (model-tier declared on every delegated sweep; `no·inline` correctly recorded for the live probe; SECRETS DISCIPLINE followed; one block per commit; STOP correctly declared at read-only-investigable=0)
- **Deltas proposed (new)**: 3  ·  **Already-covered lessons**: 4

## Honest verdict

This run genuinely surfaces one HIGH-value new rule (#1: negative-grep-over-aliased-bundle) that the kit does not
currently state — it's the logical inverse of the existing "bare-name-hit proves nothing about USE" rule, but the
kit only has the positive-hit direction written down, and B216→B224 shows the negative direction bites just as
hard. #2 and #3 are real but smaller: useful naming/caveats that would have saved zero time this run (the operator
handled both correctly by judgment) but would help a less experienced future run. The rest of what looked
retro-worthy at first pass (temp reuse, live-validation-inline, secrets discipline) turned out, on reading the
actual kit text, to already be covered — this is not a run that discovered many gaps in the method itself; it is
mostly a run that exercised the method well and found one genuine hole.
