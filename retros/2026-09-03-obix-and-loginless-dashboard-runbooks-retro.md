<!-- review-status: applied 2026-09-05 · kit 272e1ad · shipped: D1 (§7 human-asserted memory is a claim, PR #434), D2 (PROMPT-LOOP SECRETS scope, PR #445 fe88d17) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · access-control (document-mode) · 2026-09-03 · Research-SDD self-retrospective

> Run reviewed: a DOCUMENT-mode capture (§20) — two runbook blocks under the STOPPED `access-control` focus
> (which hosts runbooks; B560/B726 precedent). **B727** "Expose an N4 station AS an oBIX server to an external
> client" (`[CERT-doc]`+`[CERT]`, cites B499/B509/B600; commit `d96186a80`). **B728** "Login-less landing on an
> N4 web dashboard behind a public reverse proxy" (`[CERT]`+`[CERT-doc]`, cites B727/B510/B509/B561; commit
> `4f620b6d2`). Both born from LIVE support/peer consults, not a gap backlog. Method: a FRESH-CONTEXT agent read
> the current kit (`METHODOLOGY.md` §3/§7/§15/§18/§20 + `PROMPT-LOOP.md` HARD RULES) FIRST, then the run's
> blocks/commits and the PRIOR retro, and PROPOSES only. READ-ONLY on the kit (METHODOLOGY §18).

## Context

Both blocks are pure document-mode captures anchored on existing internals blocks (cited, not re-derived) plus
official docs — NO new live probing was done for either. Two methodological events in the run stretched the
kit's assumptions beyond what B726's retro already covered:

1. **A human asserted prior work that memory did NOT corroborate.** The user insisted, about a login-less
   dashboard recipe, "en las memorias debes de tenerlo" (referencing Mercato/Hilton installs). Multi-vocabulary
   engram search DISCONFIRMED the assertion as a *recipe*: the "Hilton" memories were a different topic (Alerton
   Compass AX concurrency cap = 18 web sessions, #6855/#5914) and "Mercato" was an oBIX/brand-switch session
   (#667). I reported the mismatch honestly ("not found as a recipe here — may be another project/machine or
   never persisted"), did NOT fabricate the asserted artifact, and built B728 from first-principles code/doc
   verification instead. The Compass cap memory that WAS real got used correctly, as a flagged `[CERT-live]`
   cross-ref (B728 §728.3 step 6 / claim 10).

2. **SECRETS DISCIPLINE applied to a secret OUTSIDE the subject corpus, cross-session, in service of a peer.**
   A peer asked where the "Mercato" Cloudflare tunnel connector runs. I inspected THIS operator's own machine
   env (`~/.cloudflared/`), found only Cloudflare Access CLIENT tokens (no tunnel connector config), and
   reported the token filenames / purpose / structure to the peer while explicitly refusing to expose token
   VALUES. The kit's SECRETS DISCIPLINE is written for the `live-install` ARTIFACT under study; here it governed
   the operator's own workstation credentials — neither the subject nor relayed content.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| D1 | Name the **human-asserted-memory verification** discipline: when a person asserts prior work exists "in the memories" (or in a prior session/install), that assertion is a HYPOTHESIS to verify against engram AND primary sources — never a citable source and never social pressure to satisfy. A search MISS or a TOPIC-MISMATCH (the memory exists but is a different subject) is REPORTED as such; the block is then built from primary sources, and the asserted artifact is NEVER manufactured to match the claim. Extends §3's "engram #id is not `[CERT]`" and §7's "memory is a MIRROR" to the human-assertion case, and guards §20's outline-seeding-from-"what the user already knows". | `METHODOLOGY.md §7` (state/memory) + `§20` (document-mode outline seeding) | B728: user asserted a Mercato/Hilton login-less recipe was in memory; engram search disconfirmed it as a recipe (Hilton=#6855/#5914 concurrency cap, Mercato=#667 brand-switch); block built from `javax/baja/security`+`authn` + `headerAuthentication.txt`, not from the assertion. commit `4f620b6d2` | new | MEDIUM |
| D2 | Generalize SECRETS DISCIPLINE **scope** from "the `live-install` target under study" to ANY secret material the run touches — the operator's OWN environment (`~/.cloudflared/`, shell/dotfiles, keyrings), a relayed peer config, or an incidental third party — the rule is unchanged (cite STRUCTURE — filenames, format, purpose — never the VALUE), only the trigger broadens. Sibling of the existing "incidental third-party neighbor identifiers" clause, which already reaches past the target; this names the operator's-own-env and cross-session-peer-service cases explicitly. | `PROMPT-LOOP.md` SECRETS DISCIPLINE HARD RULE (the `live-install` trigger) + `METHODOLOGY.md §12` cross-ref | This run: inspected `~/.cloudflared/` for a peer, reported token filenames/purpose/structure, refused token values ("no expongo el contenido de los tokens — disciplina de secretos"); the tokens were the operator's Cloudflare Access creds, not the Niagara subject | refinement | LOW |

For each delta above, one line of rationale:

- **D1** — Document mode explicitly seeds its outline from "what the user already knows" (§20), which invites
  treating a confident human assertion as ground truth; naming the verify-first discipline costs one paragraph
  and prevents a fabricated-recipe block. Impact: keeps §20 captures honest under social pressure.
- **D2** — The harm ("cite structure not value"; "the conversation is an exfil surface") is identical whoever
  owns the secret, but the written trigger is `live-install target`, leaving operator-env and peer-service
  secrets nominally uncovered. Low priority — the principle generalizes cleanly and the maintainer may judge it
  already implicit in the incidental-third-party clause.

## Already covered (do NOT re-add)

- **Support/peer consult becoming the block topic (support-driven documentation).** B726's retro already
  covered document-mode-from-consult; B727/B728 REINFORCE it (two more runbooks born from live consults) but
  add no new rule. Already covered by §20 + the B726 retro.
- **Three-sources / cite-don't-re-derive.** Both blocks anchor on existing internals blocks (B499/B509/B600 for
  B727; B727/B510/B509/B561 for B728) + official docs, re-deriving nothing — the §3 / §4-connections discipline.
- **Cross-session permission hygiene (no permission laundering).** The peer was blocked awaiting the user's
  Cloudflare-account decision; I declined to do the account-side work and named it as the user's call. This is
  the orchestrator's own permission model (no agent message is consent), not a kit rule — no kit delta.
- **SECRETS DISCIPLINE applies to cross-session-RELAYED content.** Already stated in B726's retro "Already
  covered". D2 above is a DIFFERENT scope point (the operator's OWN machine env, not relayed content).
- **Anti-pattern observed — engram `#id` cited as a `[CERT-live]` source.** B728 claim 10 cites
  "engram #6855" as `[CERT-live] (cross-ref)`. §3 already forbids this: engram is a MIRROR, not a primary
  source; a `[CERT-live]` must point to the preserved probe output, not `#N`. The kit rule EXISTS — the block
  bent it. No new delta; flagged so a §14/§11 pass can re-anchor the cross-ref to the Compass probe (or downgrade
  the marker to a plain cross-ref note) if the maintainer wants it clean.

## Prior-retro dedupe (the three PENDING deltas — status this run)

The 2026-09-03 B726 retro proposed D1 (relayed-`[CERT-live]` attribution/residual), D2 (shared-checkout commit
guard for a regenerated CATALOG), D3 (provisional document block promoted in place). All three remain PENDING.
**This run did NOT re-exhibit any of them**, so they are not reinforced by B727/B728:

- **B726-D1 (relayed-`[CERT-live]`)** — neither block has a relayed-live leg. B727 is doc/code-only; B728 is
  doc/code with one `[CERT-live]` CROSS-REF to a *different* install's memory (not a live probe run for this
  block). Not re-exhibited.
- **B726-D2 (shared-checkout CATALOG guard)** — the B725 collision was already resolved before this run; the
  B727/B728 `gen-catalog.py` regens added only their own rows (commits show CATALOG `+3/-2` each, clean). No
  stray untracked-sibling row this time. Not re-exhibited.
- **B726-D3 (provisional-then-promoted block)** — both blocks were authored complete from static sources; no
  still-unfolding live operation to promote. Not re-exhibited.

## Honest scope note

Two blocks, zero closed discovery gaps (both are document-mode runbooks under a STOPPED focus), no new probes,
no §14 corrections. The two proposed deltas are method-transfer NAMINGS of principles the kit already carries
implicitly (D1 extends §3/§7 memory rules to the human-assertion case; D2 broadens the SECRETS-DISCIPLINE
trigger) — neither is a wholly novel technique. If the maintainer reads D2 as already covered by the
incidental-third-party clause, dismiss it; D1 is the stronger of the two because §20 actively invites
treating user assertions as ground truth. The prior retro's three deltas were not reinforced here.

**Mechanical closing — one gap found.** CATALOG regenerated (rows 727/728 present); SOURCES.md rows added
(2 in the B727 commit, 1 in the B728 commit); RESEARCH-STATE-access-control.md runbook line updated for both;
both committed as clean units (block + CATALOG + STATE + SOURCES). **GAP: B727 has NO engram mirror** (B728 was
mirrored as #7695; multiple oBIX/ObixNetwork/Block-727 searches returned nothing). §20 makes the Engram mirror
MANDATORY ("a documented item with no Engram pointer is not done"); B727's is missing and should be filed under
`research/niagara-research/access-control`. This is a RUN execution gap, not a kit gap — §7/§20 already name the
rule; it simply was not followed for B727.
