<!-- review-status: pending -->
# Retro — niagara-research · jsonToolkit · 2026-08-04 · Research-SDD self-retrospective

> Run reviewed: jsonToolkit focus, B335–B349 (14 evidence blocks + 1 synthesis). 14/14 gaps closed.
> Focus bootstrapped and completed 2026-08-04, immediately after the `email` focus. Second focus closed
> this session; first focus in this corpus to use `docJsonToolkit` (114 files, 33 cited) as `[CERT-doc]`.
> Outcome: `com.tridiumx.jsonToolkit` fully documented — bidirectional JSON marshaller, synchronous
> engine-thread generation, no autonomous transport, inbound trusts sender (export-reg NO ACL, ack
> attribution spoofable). Gate: 3-layer license. 2 child gaps G1/G2 deferred (requires-execution).
>
> Method: a FRESH-CONTEXT agent read `PROMPT-LOOP.md` + `METHODOLOGY.md` first, then the run's blocks,
> commits, and RESEARCH-STATE. READ-ONLY on the kit — proposes only; kit changes are human-reviewed and
> human-committed (METHODOLOGY §18).

---

## Proposed kit deltas

> Only genuinely NEW items; kit-existing rules appear under "Already covered / re-confirms".

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add a documented pattern — and optionally a companion `preserve-local-doc.sh` helper — for ALREADY-LOCAL official doc corpora (on disk, not fetched via URL). `fetch-doc.sh` covers URL fetch; nothing covers the 3-step manual sequence (cp to `sources/manuals/<focus>-docs/`, sha256sum, SOURCES.md row). Without it, every run hitting an on-disk doc corpus re-invents the workflow and risks inconsistency (missing sha256, blank Blocks column, wrong granularity). | `METHODOLOGY.md §5` (after the `fetch-doc.sh` paragraph) + `PROMPT-LOOP.md` BOOTSTRAP step e (doc-census note) | 33 files preserved across B336–B348 via manual 3-step; no kit recipe cited; each block's self-verify notes the cp+sha step as improvised; compare email focus where docEmail was NOT yet on disk so fetch-doc.sh was irrelevant | new workflow | HIGH |
| 2 | At the first `[CERT-doc]` claim in a block for a NEW local doc corpus, add an in-block PROMPT-LOOP reminder: "cite by full HTML basename from the SOURCES.md row — NOT a doc-title shorthand or a truncated form." §5 encodes the rule at the registry level; B336 initially violated it (early draft used a short title form, caught and fixed inline before commit). Without a per-block reminder at the cite-point, the rule remains a §5-level policy the sub-agent must remember, instead of a prompted gate. | `PROMPT-LOOP.md` WRITE ONE BLOCK step (cite-instruction note, adjacent to the FABRICATED-CITE warning) | B336 commit `e975837` — the block uses `JsonSchemaTypes-Json-70BA9870.html` (full name) in its final form; the correction happened within the iteration. Same rule is in §5 (line ~250-255 of METHODOLOGY.md) but not surfaced in the per-block iteration prompt | enforcement gap | MEDIUM |
| 3 | Name the DE-ESCALATION OUTCOME of the FRAMEWORK-SEMANTIC-CHECK as a first-class result. The kit has the check (driver re-verifies security claims after a sweep); it does not name what happens when the re-read DOWNGRADES a finding. B341 caught "setpoint bypass → authorized + sender-picks-priority" (one de-escalation, corrected in §341.8); B347 caught two more. B349 §349.5 frames it explicitly: "the most valuable act was subtracting a false finding, not adding one." Naming DE-ESCALATION as a distinct iteration-history outcome (not just "correction") lets retro reviewers recognize it as a quality signal and helps future runs surface the pattern rather than treat it as an ad-hoc fix note. | `PROMPT-LOOP.md` FRAMEWORK-SEMANTIC-CHECK paragraph; `METHODOLOGY.md §11` (self-verify types) | B341 commit `2b8ee80` iteration-history: "driver re-verify (downgraded overstated finding)"; B347 commit `3936806`: "2 de-escalations"; B349 §349.5 "subtracting a false finding"; no kit vocabulary for this outcome | new named pattern | MEDIUM |

---

## What held well (kit rules confirmed by this run)

- **Audit-first backlog** (`PROMPT-LOOP.md` BOOTSTRAP step e): 14 gaps seeded in one sonnet sweep before any block was written. Zero redundant gaps, zero prior-coverage misses (B20/B32/B76/B85 were pass-through mentions correctly identified as NOT CATALOG entries and not pre-declared REMITTANCE — the audit sweep correctly skipped them).

- **[CERT-doc] marker discipline** (METHODOLOGY §3): every block that cited `docJsonToolkit` used the `[CERT-doc]` marker with the full HTML basename. 33 of 114 files preserved and registered. B336 is declared "FIRST corpus block to cite docJsonToolkit" in its header — the corpus milestone was tracked per §3's intent.

- **FRAMEWORK-SEMANTIC-CHECK** (PROMPT-LOOP §INVESTIGATE): the driver's re-read of B341's security claims caught the sub-agent's "bypass" overstatement. The correction is cited at the block level (§341.2 says "the sweep initially called this a bypass; that is WRONG") and propagated to the synthesis (B349 §349.5). This confirms the check works and catches the most consequential category of error (overstated security findings).

- **Synthesis block self-verify "zero file:line WARN by design"** (METHODOLOGY §11): B349 §349.10 correctly declares TYPE=synthesis and explains the high `[INFER]` ratio (9.00) and zero `file:line` as expected and correct. Same pattern as B334 (email synthesis). The kit rule (declare block type so ratio is read correctly) is working and both synthesis blocks applied it.

- **requires_execution_open accounting** (METHODOLOGY §8 + RESEARCH-STATE schema): G1 and G2 are registered in BOTH the backlog table (explicit rows with `requires-execution → §12`) AND the iteration-history "New gaps uncovered" column of the synthesis row (B349). The counter `requires_execution_open: 2` matches. This AVOIDS the email-Delta-1 anti-pattern (email-G1 was in the iteration history but absent from the backlog table). See dedupe note below.

- **Focus commit message discipline**: all 15 commits use `research(niagara/jsonToolkit): B<n> <slug>` scope form. Git log is scannable for resume (as with the email focus).

---

## Dedupe notes (vs. email-focus retro 2026-08-04)

| # | Relation to email retro |
|---|---|
| Delta 1 (local-doc workflow) | NOT in email retro. Email focus did not use a local doc corpus (`docEmail` is not a recognized pre-extracted corpus in the organized/ tree). GENUINELY NEW. |
| Delta 2 (full-basename reminder in per-block prompt) | PARTIALLY overlaps email-Delta-2 (bare `:line` body pattern). Email Delta-2 is about bare `:line` single-source citation shorthand in the block body compensated by self-verify; this is about doc-title shorthand in `[CERT-doc]` citations. Same §5 rule, different surface. NEW enforcement angle. |
| Delta 3 (de-escalation named outcome) | NOT in email retro. Email B327 surfaced a spoofing finding but no overstated claim was caught by the driver. GENUINELY NEW. |
| requires_execution_open correct | RE-CONFIRMS email-Delta-1. This run got it right (both backlog table AND iteration-history register G1/G2). Email-Delta-1 proposes a kit rule to enforce this; this run provides evidence the rule is needed (the "right" outcome still required deliberate care). |

---

## Anti-patterns observed

- **Manual 3-step repeated 33 times** (Delta 1): each block that introduced a new `docJsonToolkit` file ran cp + sha256 + SOURCES.md row manually. By B345 the pattern was practiced but still undocumented. Risk: future runs with large on-disk doc corpora will repeat the same improvisation.

- **Full-basename violation caught inline, not by gate** (Delta 2): B336's early cite used a shorthand that would have failed `verify-sources.sh` LEVEL-4. The fix happened within the iteration before commit, but only because the agent noticed — the PROMPT-LOOP didn't prompt for it at cite-time.

---

> PROPOSAL ONLY — kit NOT edited. Kit changes require human review and a branch/PR per METHODOLOGY §18.
