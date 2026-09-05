<!-- review-status: applied 2026-09-05 · kit 272e1ad · shipped: #1 (§20 quick-mode terminal), #2 (§3 live cross-check), PR #434; #3 (PROMPT-LOOP quick-mode delegation, PR #445 fe88d17) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective

> Run reviewed: a BUILD/commissioning session (ColdRoomPan rt + DashboardPan freeze-stat/LEDs/Cuarto-5)
> that included ONE `/research-sdd` invocation — a scoped [CERT] factual question about oBIX write
> semantics, run as QUICK mode via a delegated subagent. No block was written (the operator chose to
> SEED it for a future session). This captures only the lessons that TRANSFER to the research-sdd loop;
> the build-specific lessons live in the build-n4-module kit retro
> `2026-09-03-coldroompan-dashboardpan-freeze-stat-leds.md`. READ-ONLY on the kit — PROPOSES only (§18).

## Context

Mid-build, the operator asked to verify a single architecture-blocking fact: *can an oBIX client (HTTP
Basic) WRITE a plain BComponent property, or only writable points?* This is the QUICK/clarification shape
(a scoped factual question against a registered corpus), not a discovery loop. It was answered by ONE
`general-purpose` subagent that swept all three sources and returned a cited verdict with file:line from
`organized/obixDriver` + `baja`. Delivery: direct answer + engram finding (#7991) + a seed memory (#7992)
pointing a future session to promote it into a numbered block. Corpus/niagara-help were ZERO on oBIX;
code (FUENTE 3) carried the answer.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Name the clean QUICK-mode terminal for a scoped [CERT] question that the operator does NOT want catalogued: **engram finding + a SEED memory** that points a future session at it (finding id, next-block hint, suggested focus, sources to register). This is distinct from "offer to promote" — it is a durable, recall-findable hand-off when promotion is deferred, not dropped. | `METHODOLOGY.md §20` (document/quick paths) + `PROMPT-LOOP.md` (quick path RETURN CONTRACT) | This session: oBIX verdict delivered as answer + engram #7991 (reference) + #7992 (seed); operator: "así se queda, deja el bloque como sugerencia para una futura sesión." | new | MEDIUM |
| 2 | For a code-derived [CERT] capability, REQUIRE a check for a related `[CERT-live]` block before calling the verdict complete — empirical/live behavior can NARROW a code-true capability and surface a practical gate the code path doesn't show. | `METHODOLOGY.md §14` (cross-block consistency) + §3 (markers) | Code proved "oBIX PUT writes a generic property via BComplex.set"; the practical caveat "HTTP Basic is 401-rejected under DigestScheme (SCRAM default)" came only from cross-referencing corpus B605 [CERT-live]. Without it the verdict would have over-promised. | reinforce | MEDIUM |
| 3 | Quick-mode may DELEGATE the three-source sweep to a single subagent when the answer needs deep decompiled-code reading — one bounded worker returns the cited verdict without inflating the parent. Name this as the quick-mode analogue of the loop's SOURCE-BEFORE-AGENT delegation. | `PROMPT-LOOP.md` (DELEGATION + quick path) | One `general-purpose` agent (28 tool calls) traced the full oBIX write path + permission chain and returned file:line citations; parent stayed thin. | new | LOW |

## Already covered (do NOT re-add)
- **"The zero is data" / three-sources acumulative** — corpus=ZERO + niagara-help=ZERO routed correctly
  to code (FUENTE 3), which delivered. Textbook §5/three-sources; delta #1-#3 don't touch it.
- **Read the REAL source, don't derive from memory** — the whole point of citing `organized/obixDriver`
  file:line; already the core rule.
- **Secrets discipline** — N/A this run (no live-install secret surfaced; the finding is code semantics).

## Honest scope note
This was NOT a discovery investigation: no block written, no CATALOG row, no gap closed — by the
operator's explicit choice to seed instead of catalogue. The three deltas are small loop refinements for
the QUICK/scoped-question path (the mode this run actually exercised), not the NORMAL CYCLE. The seeded
follow-up (promote oBIX to a numbered block) is recorded in engram #7992, not here.
