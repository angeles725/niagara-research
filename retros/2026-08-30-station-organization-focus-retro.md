<!-- kit-retro -->
<!-- review-status: applied 2026-09-05 · kit f73f5d6 · shipped: SO1 (PROMPT-LOOP, PR #445 d9be953), SO2 (§16 focus status is a verifiable claim, PR #427) -->
<!--
  focus: station-organization
  blocks: B716–B720 (5 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: PROMPT-LOOP.md (two low-priority deltas — step 3 W2 companion + step e remittance note)
-->

# §18 Retrospective — focus: station-organization (B716–B720)

**Run summary:** Third SYNTHESIS-GUIDE focus in the same session, born from an out-of-scope operator
question (TC500/IO-R-34 — where does the programming go?) that arrived mid-`module-dev-workflow` focus.
The prior retro (module-dev-workflow, Delta W2) already proposed the handling protocol for out-of-scope
questions; this run is the concrete follow-through — the question was answered inline, flagged as a
candidate focus, and promoted to a full focus in the next turn. 5 gaps (SO1–SO5); 2 delegated (SO1/SO2
sonnet sweeps), 3 inline (SO3/SO4/SO5 in-hand after SO1/SO2). Deliverable: `docs/station-organization.md`.

Two notable events beyond ordinary synthesis-focus execution:
1. The SO2 sweep corrected the DRIVER'S OWN prior inline answer to the operator about where control logic
   goes — Philosophy B (blocks-near-points) refines the "logic in a separate area" framing.
2. The SO2 sweep found that `kitControl` is DONE (B537/538/545, 2026-08-28), while the bootstrap
   remittance note declared it "planned/not-done." This produced a §14 correction to the focus's OWN
   RESEARCH-STATE header — an unusual event (§14 normally corrects a prior-focus block, not the current
   focus's own bootstrap note).

---

## Honest deduplication — prior deltas validated by this run (not re-proposed)

| Retro | Delta | Validation strength |
|---|---|---|
| module-dev-workflow (same session) | **W2 — out-of-scope operator question mid-focus** | STRONGEST possible — this entire focus IS the follow-through. Question arrived mid-WF, answered inline, labeled out-of-scope, named as candidate focus. Promoted to a full focus in the next turn, exactly as W2 prescribes. W2 is now validated by a complete end-to-end instance (question → inline answer → candidate flag → full focus). |
| module-best-practices (same session) | **Delta 1 — SYNTHESIS-GUIDE FOCUS pattern** | STRONG — third independent instance. Every characteristic holds: corpus blocks as only sources, all blocks DESIGN/SYNTHESIS type, `docs/` deliverable per gap, STOP = all gaps closed + deliverable finalized. |
| module-best-practices (same session) | **Delta 3 — in-hand → inline heuristic** | STRONG — SO1/SO2 were the first reads (sonnet delegated); SO3/SO4/SO5 were inline because the prior sweeps loaded the relevant blocks (chihuahua/ChiLinkHelper/tags/hierarchy) into driver context. The split was clean: 2 delegated, 3 inline, zero ambiguity. |
| module-dev-workflow (same session) | **W1 — FOCUS PAIR (rules-guide + process-runbook)** | PARTIAL — validates that SYNTHESIS-GUIDE focuses can target different deliverable shapes over related evidence (station-engineering vs. module-dev), but does NOT validate the "same evidence base, orthogonal shape" core of W1 (this focus draws on different blocks than the module pair). No re-proposal; noted as partial. |

---

## Delta SO1 — LOW — Delegated sweep contradicts driver's own prior inline-to-user statement: acknowledge before writing the block

**Target:** PROMPT-LOOP.md — step 3 INVESTIGATE, VERIFY BEFORE ACTING paragraph (after the "scope of a
sub-agent's proven-absence is narrower" bullet), as a brief companion note.

**Evidence:** B717 / iteration 2 of this focus. During `module-dev-workflow`, the operator asked "where
does the programming go?" and the driver answered inline: logic in a separate area (Config or Services),
not under /Drivers. This is broadly correct. However, the SO2 sweep of the station-organization focus
surfaced Tridium's explicit "Philosophy B" from the kitControl docs ([Block 538] BP5): **co-locate
kitControl blocks NEAR the points they control** — not a central `Logic/` folder, but rather an
equipment-component block organizer that lives near (or at the same level as) the driven points.
B717 §717.4 honestly stated: "This refines the SO1/operator answer — 'not under /Drivers' remains
correct; 'in a separate folder' needs the Philosophy B nuance. Philosophy B is the better framing."

**What the kit already covers:**

- **§14 correction:** corrects errors in CORPUS BLOCKS. It does not cover the case where the error is
  in an INLINE statement made to the user (not committed to any block, no back-pointer required).
- **VERIFY BEFORE ACTING (step 3):** says to verify sub-agent reports before writing a block —
  especially absences. This covers the direction driver→block. It does NOT name the inverse event:
  sub-agent evidence contradicts a driver-to-user inline assertion.
- **SCOPING JUDGMENTS ARE HYPOTHESES (step 3):** says prior block scope-outs are testable hypotheses.
  Orthogonal — that rule re-tests a block's reasoning; this delta is about the driver's unrecorded
  inline assertion.

**Gap:** The kit has no named protocol for the case where a delegated sweep returns evidence that
contradicts something the DRIVER said inline to the operator (before any block was written). The
failure mode is: the driver writes the block incorporating the sweep's finding WITHOUT acknowledging
that it supersedes the earlier inline answer — leaving the operator with an uncorrected prior
statement in the conversation.

**Proposed note (propose-never-apply):**

> **INLINE-ANSWER CORRECTION BY SWEEP.** When a delegated sweep returns evidence that CONTRADICTS or
> materially refines an assertion the driver made INLINE to the operator (not a block — a conversational
> answer given before or during an earlier gap), acknowledge the refinement BEFORE or WHILE writing
> the block:
>
> 1. Name what the earlier inline answer said and where it was wrong or incomplete.
> 2. State the sweep's contradicting/refining finding with its citation.
> 3. Write the block using the refined framing.
>
> This is NOT a §14 correction (§14 targets corpus blocks). It is a conversational acknowledgment —
> there is no back-pointer to edit, but the operator must not be left with the uncorrected framing.
> Record the refinement in the block's self-verify or Connections section so it is auditable.
>
> Trigger: the sweep's finding changes the OPERATIONAL FRAMING of the inline answer in a way that
> would alter the operator's behavior (e.g. "not in /Drivers" stays correct; "in a central Logic
> folder" is refined by Philosophy B to "near the controlled equipment"). A minor nuance that does
> not change the operator's action is NOT a trigger — use judgment.

**Priority:** LOW — this was handled correctly by intuition in B717 (the block honestly named the
refinement). The risk of omission is not data loss or a wrong block — it is leaving the operator with
an uncorrected conversational statement. Low enough that the existing VERIFY BEFORE ACTING discipline
mostly covers it by implication: if you verify findings before writing the block, you naturally
discover any contradiction with your prior inline answer. The main value of this note is giving agents
language for the obligation ("conversational acknowledgment, not §14") and a criterion for when it
applies.

---

## Delta SO2 — LOW — Bootstrap remittance focus-status is a verifiable claim: confirm from FOCUSES.md before sealing "focus X is planned/not-done"

**Target:** PROMPT-LOOP.md — step e (POPULATE the scaffolded RESEARCH-STATE), PRE-DECLARE REMITTANCES
FIRST paragraph, as a one-sentence caution added after "read FOCUSES.md + INDEX.md."

**Evidence:** RESEARCH-STATE-station-organization.md bootstrap declared: "Control-logic library
INTERNALS (kitControl) are REMITTANCE-DEFERRED (focus `kitControl` is planned/not-done)." The SO2
sweep immediately found [Block 537]/[Block 538]/[Block 545] — kitControl IS done (2026-08-28). The
RESEARCH-STATE header was corrected in iteration 2 as a §14 note: "§14: kitControl is DONE not
planned." The Remittance section was also corrected to read "DONE 2026-08-28 — corrected from
bootstrap's 'planned'."

**What the kit already covers:**

- PRE-DECLARE REMITTANCES FIRST (PROMPT-LOOP step e): "read FOCUSES.md + INDEX.md for subjects an
  EXISTING block already answers, and PRE-DECLARE those as REMITTANCE gaps WITH their [Block N] §N.x
  citations BEFORE delegating the audit sweep." The instruction to read FOCUSES.md is already there.

**What happened:** The bootstrap declared kitControl as "planned/not-done" WITHOUT reading FOCUSES.md.
The focus-completion status was GUESSED from memory rather than derived from the index. Had FOCUSES.md
been read, B537/538/545 would have been found and the remittance would have been declared correctly.

**Why this is not simply "the agent should follow the existing rule":** The existing rule says READ
FOCUSES.md but does not call out that a "focus X is not done" claim is itself a VERIFIABLE CLAIM
that requires evidence from FOCUSES.md. The failure mode is subtle: the agent may believe it knows
the focus status from session context (cross-focus awareness) and skip the FOCUSES.md read for what
seems like an obvious "not done" case. Naming this as a verifiable claim — not a free assertion —
closes the loophole.

**Proposed note (propose-never-apply):**

> **FOCUS-STATUS IN A REMITTANCE IS A VERIFIABLE CLAIM.** When declaring a remittance as
> "deferred: focus `<X>` is planned/not-done," treat the focus-status as a verifiable claim, not a
> free assertion from session context or memory. Read `FOCUSES.md` (or `INDEX.md`) to confirm that
> the cited focus has NO closed blocks before sealing "not-done" into RESEARCH-STATE. A focus whose
> blocks appear in INDEX.md IS done; treat it as a standard remittance with [Block N] citations,
> not a deferred one.
>
> A "planned/not-done" remittance that is actually done produces a §14 correction to the focus's OWN
> bootstrap note — an unusual self-correction that is avoidable by a one-line FOCUSES.md read.

**Priority:** LOW — one instance; the failure was caught immediately by the first delegated sweep and
corrected cleanly. The correction cost was minimal (RESEARCH-STATE header edit + Remittance section
update). However, the event is distinctive enough to name: it is the only §14 in this session that
targeted the current focus's OWN RESEARCH-STATE header (all other §14 corrections targeted prior-focus
blocks), which suggests the failure mode is genuinely new and not covered by existing guidance.

---

## Deduplication — considered, not re-proposed

1. **MBP Delta 1 (SYNTHESIS-GUIDE FOCUS), W1 (FOCUS PAIR), W2 (out-of-scope question):** All three
   are validated above, not re-proposed. This run provides validation evidence, not new doctrine.

2. **§14 BACK-POINTER CHECK (step 5):** The §14 in this run targeted the RESEARCH-STATE bootstrap note
   (not a block), so the back-pointer check (edit the OLD BLOCK to add "corrected in BN") does not apply
   literally — there is no "old block" to back-pointer, only the RESEARCH-STATE header which was edited
   directly. No new delta here; the existing §14 back-pointer rule's scope (prior BLOCKS) was correctly
   understood and not applied to a state-file correction.

3. **GAP PREMISES ARE HYPOTHESES (BOOTSTRAP step e):** Already in the kit and covers this type of
   bootstrap-time wrong premise. Delta SO2 does not re-propose GAP PREMISES; it names a SPECIFIC
   INSTANCE of that pattern (focus-status claims in remittance notes) and adds the verification check
   that prevents the failure. The existing rule says "premises are hypotheses" — Delta SO2 says "this
   particular premise requires one specific verification action (read FOCUSES.md)." Complementary,
   not overlapping.

4. **VERIFY BEFORE ACTING (step 3):** Already covers validating sub-agent findings before writing a
   block. Delta SO1 extends this to cover the inverse direction (sub-agent finding contradicts a prior
   DRIVER statement). Complementary.

5. **Out-of-scope question delta W2 (prior retro):** W2 already proposes the full protocol for
   handling out-of-scope questions. This run's execution of that protocol is validation evidence, not
   a new delta.

---

## Summary of new deltas proposed

| # | Priority | Gist | Absorb target |
|---|---|---|---|
| SO1 | LOW | When a delegated sweep contradicts the driver's own prior inline-to-user statement: acknowledge the refinement conversationally before/while writing the block; this is NOT a §14 (no block back-pointer); trigger only when the correction would change operator behavior | PROMPT-LOOP.md step 3 VERIFY BEFORE ACTING paragraph |
| SO2 | LOW | Focus-status in a bootstrap remittance note ("focus X is planned/not-done") is a verifiable claim: confirm from FOCUSES.md before sealing; a wrong "not-done" claim produces a §14 to the focus's own RESEARCH-STATE (avoidable by one-line read) | PROMPT-LOOP.md step e PRE-DECLARE REMITTANCES FIRST paragraph |

## Prior-retro deltas validated by this run

| Retro | Delta | Validation strength |
|---|---|---|
| module-dev-workflow | W2 out-of-scope question protocol | STRONGEST — this focus is the complete follow-through; every step of W2 executed as prescribed |
| module-best-practices | Delta 1 SYNTHESIS-GUIDE FOCUS | STRONG — third independent full instance |
| module-best-practices | Delta 3 in-hand → inline heuristic | STRONG — clean 2-delegated/3-inline split, in-hand criterion applied correctly |
| module-dev-workflow | W1 FOCUS PAIR | PARTIAL — different evidence base; validates the focus-type pattern, not the "same evidence + orthogonal shape" core |

---

## Run quality notes (not kit deltas — operational observations)

- All 5 self-verify sections passed; verify-block exited 0 on all blocks.
- The Philosophy B correction (SO2 sweep) was honestly disclosed in B717 §717.4 before the block was
  finalized — the inline-answer correction protocol (Delta SO1) was followed correctly by intuition.
- The §14 correction to the bootstrap kitControl status was applied in the same commit as B717;
  both RESEARCH-STATE header and Remittance section were corrected. No orphaned correction.
- `block_scope: shared-global` correctly declared and working.
- `verify-state.sh` and `research-sdd-archive.sh` both passed at focus close.
- `docs/station-organization.md` finalized in B720; cites research blocks throughout and is readable
  without the corpus. The short answer (two layers) is stated at the top of the document.
- Zero delegation on SO3/SO4/SO5 — the SO1/SO2 sweeps loaded chihuahua/ChiLinkHelper/BLink/tags/
  hierarchy into driver context, making the remaining three gaps fully in-hand.
- This focus is notably THIN in new findings: its entire value is synthesis and organization of
  existing evidence. That is the correct characterization — no new primary evidence was needed, and
  the focus correctly declared DESIGN/SYNTHESIS type throughout.
