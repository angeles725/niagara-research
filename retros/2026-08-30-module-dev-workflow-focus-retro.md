<!-- kit-retro -->
<!-- review-status: applied 2026-09-05 · kit d9be953 -->
<!--
  focus: module-dev-workflow
  blocks: B711–B715 (5 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: PROMPT-LOOP.md (two new deltas · step 1 + step 3)
-->

# §18 Retrospective — focus: module-dev-workflow (B711–B715)

**Run summary:** Second synthesis-guide focus in the same session, sibling to `module-best-practices`
(B705–B710). 5 gaps (WF1–WF5), all closed inline — no delegation. Deliverable:
`docs/module-dev-workflow.md` (a step-by-step runbook). Same [Block N] source material as the sibling
focus (module-anatomy B630–B636, own-modules-audit B637–B639, module-best-practices B705–B710), but
orthogonal deliverable shape: rules-guide (module-best-practices.md) vs. process-runbook
(module-dev-workflow.md). One mid-focus out-of-scope operator question answered inline and flagged.
No §14 corrections. Zero new primary evidence.

---

## Honest deduplication against this session's prior retros

This run is the SECOND pure synthesis-guide focus. The following module-best-practices retro deltas
(2026-08-30, same session) are directly validated by this run — they are NOT re-proposed:

- **MBP Delta 1 (SYNTHESIS-GUIDE FOCUS pattern):** Fully validated by a second independent instance.
  This focus exhibits every characteristic named in that delta: sources are corpus blocks only, every
  block is DESIGN/SYNTHESIS type, deliverable is a `docs/` artifact, STOP = all gaps closed + deliverable
  finalized. Two instances in one session give the pattern real weight.

- **MBP Delta 2 (verify-block WARN "ZERO file:line" expected):** The RESEARCH-STATE does not explicitly
  record WARN firings per-block. However, the fully-inline execution means some blocks were written with
  targeted re-reads of existing block text (no file:line citations to decompiled code), which is exactly
  the condition that fires the WARN. Directionally consistent; at minimum no new failure mode appeared.

- **MBP Delta 3 (synthesis-delegation heuristic: in-hand → inline):** The STRONGEST validation of this
  retro. All 5 gaps are recorded `no · inline`. The reason is precisely Delta 3's "in-hand" branch: the
  sibling focus (module-best-practices) had already swept the same source blocks in this session, making
  every gap's material available in driver context at zero additional delegation cost. This is a cleaner
  demonstration of the heuristic than the sibling focus itself produced (where 3/6 were delegated because
  they were first-time reads; 3/6 were inline because they became in-hand after the first three sweeps).
  Here, the first synthesis focus amortized the delegation cost for the second.

These three are validation evidence only. No amendment to those deltas is proposed here.

---

## Delta W1 — MED — SYNTHESIS-GUIDE FOCUS PAIR: rules-guide + process-runbook as two orthogonal deliverables over shared sources

**Target:** PROMPT-LOOP.md — step 1 (CHOOSE), immediately after the SYNTHESIS-GUIDE FOCUS named pattern
(MBP Delta 1), as a named sub-case.

**Evidence:** This focus (B711–B715, `docs/module-dev-workflow.md`) + sibling focus `module-best-practices`
(B705–B710, `docs/module-best-practices.md`). Both consumed the same source blocks (module-anatomy
B630–B636, own-modules-audit B637–B639). Their deliverables are orthogonal:

- `module-best-practices.md` — WHAT (rules, principles, anti-patterns, quality criteria)
- `module-dev-workflow.md` — HOW (the step-by-step process, tool mechanics, commands, error-fix table)

No block in either deliverable duplicates the other. The same body of knowledge yielded two genuinely
distinct documents because the DELIVERABLE SHAPE (rules vs. process) was declared at bootstrap time.

**What the kit already covers:**

- The SYNTHESIS-GUIDE FOCUS pattern (MBP Delta 1, proposed this session): names the whole-focus
  synthesis type but does NOT address the case where the SAME evidence base is synthesized into TWO
  distinct deliverables in two sequential focuses.
- METHODOLOGY §16 (multi-focus): governs parallel or sequential focuses with distinct evidence bases,
  each seeding its own angle. It does NOT describe two synthesis-guide focuses sharing the same source
  blocks but producing orthogonal documents.

**Gap:** Without a named pattern, a future agent facing "we have enough evidence; what deliverable shape
do we choose?" might default to one large guide covering everything, missing the option to split
rules-from-process into two maintainable, purpose-fit documents. Conversely, an agent starting a second
synthesis focus over the same blocks without a named justification might incorrectly mark all gaps as
REMITTANCE (already answered by the sibling focus) and close the focus prematurely, even though the
deliverable shape is genuinely different.

**Proposed rule (propose-never-apply):**

> **SYNTHESIS-GUIDE FOCUS PAIR (two orthogonal deliverables over shared evidence).** When a body of
> corpus evidence divides naturally along two orthogonal axes — most commonly WHAT (rules, principles,
> anti-patterns) vs. HOW (process, commands, tool mechanics, step sequences) — two sequential
> SYNTHESIS-GUIDE focuses may be run over the same source blocks, each producing a distinct `docs/`
> deliverable. This is not a duplicate or a split for its own sake; the deliverable shapes are
> orthogonal and non-substitutable.
>
> **Bootstrap note for the second focus:** pre-declare the sibling focus as REMITTANCE for any gap
> that the first focus FULLY COVERS as content. Gaps that address the SAME FACTS but in a DIFFERENT
> SHAPE (e.g. "the build loop" described as a rule in focus A but as a step-by-step command sequence
> in focus B) are NOT remittances — they are new gaps in the second focus. The test: "would a reader
> of focus A's deliverable find the identical content in focus B's deliverable?" If yes → remittance.
> If no (same evidence, different shape/purpose) → new gap.
>
> **Context amortization:** the second focus is almost always fully inline. The first focus loaded
> the shared source blocks into session context; the second inherits that context at zero delegation
> cost (MBP Delta 3's "in-hand → inline" branch). If the second focus is run in a NEW session (after
> compaction), re-apply the delegation heuristic from scratch (first-time reads → delegate).
>
> **Naming convention:** declare the pair relationship in each focus's RESEARCH-STATE header:
> "Companion to focus `<sibling>` (`docs/<sibling-guide>.md`) — orthogonal deliverable shape."
> This prevents a third focus from treating either as a full remittance of the other.

**Priority:** MED — appeared once as a deliberate design choice, strongly supported by the clean
outcome (two non-overlapping guides over identical evidence). The risk of missing this pattern is either
producing one bloated guide (rules and process interleaved, harder to use) or prematurely closing the
second focus as a duplicate. The "is this a remittance or a different shape?" disambiguation is the
key decision the pattern names.

---

## Delta W2 — LOW — Out-of-scope operator question mid-focus: answer inline, flag, do not contaminate

**Target:** PROMPT-LOOP.md — step 3 INVESTIGATE (or step 1 CHOOSE), as a brief operational note.

**Evidence:** During this focus (between WF2 and WF3), the operator asked a station-engineering
question: "where do device points vs. control logic go?" This question is OUT of scope for the
`module-dev-workflow` focus (which covers the dev-loop and tool mechanics, not the station-engineering
design patterns that belong to a future `station-engineering` focus). The agent answered inline
from prior corpus knowledge, explicitly labeled it "out of scope for this focus," and noted it as a
candidate new focus rather than opening a gap in the current RESEARCH-STATE.

**What the kit already covers:**

- §16 multi-focus describes opening a new focus and declaring an angle, but does NOT describe what
  to do when an operator question ARRIVES MID-ITERATION in a running focus and is clearly out of scope.
- The BOOTSTRAP b2 ANGLE rule says to declare an angle before the first gap and surface ambiguity
  early — but the trigger there is ambiguity at bootstrap, not an unexpected question mid-focus.
- The "REVERSE BACKLOG SWEEP" (PROMPT-LOOP step 6) covers re-scoping existing backlog gaps when a
  block refutes their premise. It does not cover an ad-hoc operator question that simply falls outside
  the declared angle.

**Gap:** Without a named convention, a future agent might: (a) open a new gap in the current focus
(contaminating the focus RESEARCH-STATE with an out-of-scope question), (b) defer the answer entirely
("I'll answer that after the focus closes"), or (c) answer inline but not flag it for follow-up.
None of (a) or (b) serve the operator well; (c) is better but leaves no breadcrumb for a future focus.

**Proposed rule (propose-never-apply):**

> **OUT-OF-SCOPE OPERATOR QUESTION MID-FOCUS (answer inline, flag, do not contaminate).** When the
> operator asks a question that falls outside the current focus's declared angle while a focus is
> running:
>
> 1. **Answer inline** from current corpus knowledge. Do NOT defer ("I'll cover that after the focus").
>    The operator asked now; answer now.
> 2. **Label it explicitly.** Mark the answer as "out of scope for focus `<current-focus>` — answered
>    from corpus knowledge, not opening a WF-N gap." This prevents the reader from expecting a block
>    for it.
> 3. **Do NOT add a gap to the current focus's RESEARCH-STATE.** The question belongs to a different
>    angle; adding it to the current backlog contaminates the focus and risks a premature STOP (now
>    one gap is blocked-on-different-focus).
> 4. **Offer a named future focus** as a one-line note: "This is a candidate for a `<focus-name>`
>    focus (angle: <one sentence>)." Do not open that focus mid-iteration. The breadcrumb makes it
>    discoverable in the session or in a future retro.
>
> Scope: applies only to clear, unambiguous out-of-scope questions where the current focus's declared
> angle excludes the question definitively (e.g. "station design patterns" asked during a "tool
> mechanics" focus). If the question is genuinely on the border of the current focus's angle, add it
> as a new gap and note the extension.

**Priority:** LOW — this run handled the case correctly by intuition; the pattern is simple enough
that a competent agent would likely handle it correctly without the rule. The rule's main value is
giving future agents language to use ("out of scope for focus X, answered inline, candidate for focus
Y") rather than prescribing a new behavior. Low priority because the failure mode (adding an
out-of-scope gap to the current RESEARCH-STATE) is detectable and correctable; it would not cause data
loss or a wrong block.

---

## Summary of new deltas proposed

| # | Priority | Gist | Absorb target |
|---|---|---|---|
| W1 | MED | SYNTHESIS-GUIDE FOCUS PAIR: two orthogonal deliverables (rules-guide + process-runbook) over shared evidence; amortized inline; "same evidence, different shape" is NOT a remittance | PROMPT-LOOP.md step 1, after MBP Delta 1 |
| W2 | LOW | Out-of-scope operator question mid-focus: answer inline, label explicitly, offer future focus name, do NOT add gap to current RESEARCH-STATE | PROMPT-LOOP.md step 3 or step 1 |

## Prior-retro deltas validated by this run (not re-proposed)

| Retro | Delta | Validation strength |
|---|---|---|
| module-best-practices (same session) | D1 SYNTHESIS-GUIDE FOCUS | STRONG — second independent full instance, every characteristic confirmed |
| module-best-practices (same session) | D3 in-hand → inline heuristic | STRONGEST possible — all 5/5 gaps inline because sibling focus pre-loaded the material |
| module-best-practices (same session) | D2 verify-block WARN expected | DIRECTIONAL — fully-inline run, consistent; no new failure mode |

---

## Run quality notes (not kit deltas — operational observations)

- All 5 self-verify sections passed; `verify-block.sh` exited 0 on all blocks.
- The rules-vs-process split was declared explicitly in the RESEARCH-STATE header at bootstrap
  ("distinct from `module-best-practices` (which gives RULES): this gives the PROCESS and the TOOL
  MECHANICS") — the correct declaration under MBP Delta 1's guidance.
- Zero delegation across 5 gaps — the first synthesis focus's sweeps fully amortized the context
  loading cost. Iteration-history records `no · inline (B631/B637-639 in-hand)` correctly.
- `docs/module-dev-workflow.md` finalized in B715; cites research blocks throughout ([B638], [B631],
  etc.) but is readable without the corpus.
- `verify-state.sh` and `research-sdd-archive.sh` both passed at focus close.
- `block_scope: shared-global` correctly declared and working.
- No §14 corrections — this focus added no new assertions about primary artifacts; all claims are
  process distillations of already-verified [CERT] block findings.
- The mid-focus out-of-scope operator question (station-engineering design patterns) was handled
  correctly: answered inline, labeled out-of-scope, candidate focus named, no gap added to WF backlog.
