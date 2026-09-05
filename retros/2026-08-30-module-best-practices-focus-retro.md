<!-- kit-retro -->
<!-- review-status: applied 2026-09-05 · kit d9be953 -->
<!--
  focus: module-best-practices
  blocks: B705–B710 (6 blocks)
  date: 2026-08-30
  review-status: pending
  propose-never-apply: true
  absorb-targets: PROMPT-LOOP.md (three deltas — steps 1 · 3 · 5)
-->

# §18 Retrospective — focus: module-best-practices (B705–B710)

**Run summary:** Pure SYNTHESIS/DESIGN focus — no new evidence extracted, no new jars or sources
inspected for the first time. Every block re-cited existing corpus blocks ([Block N]) and distilled
them into an actionable N4 module best-practices guide (`docs/module-best-practices.md`). Six gaps
(MBP1-6), each producing one guide section: rt/ux/wb/cross-cutting/build/exemplar-catalog. Three
gaps used delegated sonnet sweeps (MBP1-3, first-time multi-block synthesis); three were inline
(MBP4-6, material already in-hand from prior sweeps in the same session). Deliverable finalized in
B710. `verify-block.sh` exited 0 on all six but fired WARN "ZERO file:line citations resolved" on
the three delegated synthesis blocks — expected behavior, not an error.

---

## Delta 1 — HIGH — SYNTHESIS-GUIDE FOCUS: named pattern for a meta-focus that synthesizes N closed focuses into a guide

**Target:** PROMPT-LOOP.md — step 1 (CHOOSE), after the KNOWN-OUTLINE DESIGN CORPUS variant paragraph.

**Evidence:** RESEARCH-STATE-module-best-practices.md + B705–B710 + `docs/module-best-practices.md`.
The entire focus produced no primary evidence: every block's sources are existing corpus blocks
([Block 629]–[Block 655] + [Block 427]–[Block 432] etc.), not newly decompiled jars or new web
sources. The deliverable is a `docs/` guide that grew one section per iteration (MBP1 → §1 rt,
MBP2 → §2 ux, …, MBP6 → §6 exemplar catalog). The RESEARCH-STATE header correctly declared
"DESIGN/APPLIED corpus — synthesizes EXISTING evidence blocks into an actionable guide + improvement
recommendations; a high [INFER] ratio is EXPECTED."

**What the kit already covers:**

- **Per-block DESIGN/APPLIED type** (PROMPT-LOOP.md step 5): "For a DESIGN/APPLIED block (an
  integration plan, a PoC design, a synthesis), a high ratio is EXPECTED and healthy … Declare which
  type it is so the ratio is read right." This governs individual block self-verify. It does NOT
  describe a WHOLE FOCUS whose every gap is synthesis-only and whose terminal artifact is a `docs/`
  guide.
- **KNOWN-OUTLINE DESIGN CORPUS variant** (PROMPT-LOOP.md step 1): applies when the corpus subject
  is external tooling/specifications and the gap list is pre-fixed before any block is written. The
  batch-scout pattern targets EXTERNAL sources with a certifiability gate. This focus has no external
  sources to scout — all sources are already-verified corpus blocks.
- **DOCUMENT MODE (§20)**: for a focus that PROCESSES existing documents (manuals, PDFs) as sources
  to produce evidence blocks. The current focus does the inverse: it PRODUCES a new document by
  synthesizing prior evidence blocks.
- **SYNTHESIS-BLOCK REGISTRATION RULE** (PROMPT-LOOP.md step 6): governs the last block of an
  evidence focus (a focus-closing synthesis). This is not one closing block — the ENTIRE focus is
  synthesis-oriented from gap 1.

**Gap in the kit:** No named pattern exists for a focus whose PURPOSE is to consume N closed focuses
as input and produce a `docs/` guide or recommendations document as output, where every iteration
writes one guide section (not one evidence block). Without this pattern named, a future agent would
apply evidence-focus rules (scout gates, file-count delegation triggers, file:line token checks) to
a context where none of them apply.

**Proposed rule (propose-never-apply):**

> **SYNTHESIS-GUIDE FOCUS.** A focus whose entire purpose is to DISTILL N closed prior focuses into
> an actionable `docs/` guide or recommendations document — adding no new primary evidence — is a
> named focus type: SYNTHESIS-GUIDE. Characteristics that distinguish it from an evidence focus and
> from DOCUMENT MODE:
>
> - **Sources are corpus blocks, not files or external documents.** The investigation reads existing
>   [Block N] entries and optionally re-reads the real artifacts those blocks already cited. No new
>   source-preservation, no e3 scout gate, no file-type census for the guide's subject (the census
>   was already run by the prior focuses).
> - **Deliverable is a `docs/` artifact, not a block per se.** Each gap corresponds to one section
>   of the deliverable. The block for each gap exists as a corpus record, but its terminal value is
>   the contribution to `docs/<guide>.md`. Record the deliverable path in RESEARCH-STATE.
> - **Every block is DESIGN/SYNTHESIS type.** High [INFER] ratio is EXPECTED and HEALTHY at the
>   FOCUS level, not just per-block (declare this in the RESEARCH-STATE header at bootstrap so
>   `verify-state.sh` and reviewers read the ratio correctly across all blocks).
> - **verify-block WARN "ZERO file:line citations resolved" is EXPECTED** on any block whose
>   citations are exclusively [Block N] cross-references (see Delta 2 of this retro).
> - **Gap design:** each gap = one guide section; seed MBP-style gaps at bootstrap (pre-declared,
>   independent, one per layer or theme). The KNOWN-OUTLINE batch-scout variant does NOT apply —
>   there is nothing to scout; the sources are on disk.
> - **STOP criterion:** all gaps closed AND `docs/<guide>.md` finalized. A synthesis focus with no
>   `docs/` deliverable is an ordinary focus-closing synthesis block (PROMPT-LOOP step 7), not a
>   SYNTHESIS-GUIDE focus.
>
> Distinct from DOCUMENT MODE (§20), which produces evidence blocks FROM existing documents.
> Distinct from a focus-closing synthesis block (the terminal block of an evidence focus).
> Distinct from the KNOWN-OUTLINE DESIGN CORPUS variant (which targets external sources).

**Priority:** HIGH — the pattern appeared in full on this run (B705-B710 + `docs/module-best-
practices.md`) and is a reusable focus type for any mature multi-focus corpus that has accumulated
enough evidence to justify a distillation guide. Without naming it, the kit's evidence-focus rules
(scout gates, file-count delegation, file:line token checks, exhaustion signals) apply inappropriately
to a context where they cannot fire correctly.

---

## Delta 2 — MED — verify-block WARN "ZERO file:line citations resolved" is a false-positive for synthesis blocks

**Target:** PROMPT-LOOP.md — step 5 SELF-VERIFY, verify-block.sh paragraph (after "paste its
output").

**Evidence:** B705, B706, B707 (the three delegated synthesis blocks). `verify-block.sh` exited 0
(no hard failure) but printed WARN "ZERO file:line citations resolved" because those blocks cite
exclusively [Block N] cross-references — no `file:line` in the text. B708, B709, B710 (the three
inline blocks) included at least one targeted file read with a `file:line` citation each, so the
WARN did not fire for them. The driver's self-verify sections noted "WARN expected: synthesis block,
all citations are [Block N]" — the correct interpretation, but the kit gives no rule for it.

**What the kit already covers:** The verify-block step says: "it exits non-zero on a cited file:line
whose line is out of range. It is your own calculator, not an orchestrator gate." It also says:
"extern citations (beautified/decompiled/snapshot) are not script-verifiable — still token-check
those by reading." Neither passage addresses the distinct case where citations are EXCLUSIVELY [Block
N] cross-references. The DESIGN/APPLIED block type declaration (step 5 self-verify) explains the
high [INFER] ratio, but not the [Block N]-only citation profile and its effect on verify-block output.

**Gap in the kit:** A synthesis block whose [CERT] citations are exclusively [Block N] (not
file:line) will always produce "ZERO file:line citations resolved" from verify-block. A future agent
reading this WARN without context may incorrectly conclude that the block has uncited claims, or
may try to add spurious file:line citations to silence the WARN. Neither is correct.

**What "token-check" means for a synthesis block:** For an evidence block, token-check verifies a
literal string (method name, flag value, field name) is present in its cited `file:line`. For a
synthesis block, the analog is confirming that the FINDING attributed to [Block N] §N.x actually
appears in that block — a [Block N] cross-reference where that block says no such thing is a
fabricated citation, the synthesis equivalent of a wrong file:line.

**Proposed rule (propose-never-apply):**

> **SYNTHESIS BLOCK — verify-block WARN "ZERO file:line citations resolved" (expected, not an
> error).** When a block's TYPE is DESIGN/SYNTHESIS and its citations are exclusively [Block N]
> cross-references (no file:line), `verify-block.sh` will report "ZERO file:line citations
> resolved." This is EXPECTED and NOT a quality defect. Verify-block exits 0 in this case; the WARN
> is informational. Do not add spurious file:line citations to silence it.
>
> The TOKEN-CHECK discipline for synthesis blocks applies to the [Block N] citations INSTEAD of
> file:line: confirm that the finding or claim attributed to [Block N] §N.x actually appears in that
> block's cited section. A [Block N] that does not support the attributed claim is a fabricated
> synthesis citation — as invalid as a wrong file:line for an evidence block.
>
> Record in the self-verify section: "verify-block: exit 0, WARN 'ZERO file:line citations resolved'
> — EXPECTED (synthesis block; [Block N] token-check: N citations confirmed)."
>
> This WARN does NOT appear on synthesis blocks that include at least one targeted file read (e.g.
> a spot-check to confirm a code citation from a prior block). A delegated synthesis sweep that
> returns only [Block N] citations triggers it; an inline block that re-reads one file does not.

**Priority:** MED — the WARN fired on 3/6 blocks in this focus and was correctly explained each
time. Without this note, a future agent on a SYNTHESIS-GUIDE focus would have no rule to cite when
encountering it, and the temptation to "fix" it by adding spurious file:line citations would produce
a worse block (invented primary-source citations in a block that correctly should not have any).

---

## Delta 3 — LOW — Synthesis-delegation heuristic: new multi-block material → delegate; already in-hand → inline

**Target:** PROMPT-LOOP.md — step 3 INVESTIGATE, delegation paragraph (after the MODEL TIER rule).

**Evidence:** Iteration-history RESEARCH-STATE-module-best-practices.md. MBP1 (rt), MBP2 (ux),
MBP3 (wb): each synthesized 3-6 distinct prior focuses for the first time in this session;
delegated to sonnet. MBP4 (cross-cutting), MBP5 (build), MBP6 (exemplar + close): material was
already in-hand from the prior three sweeps' returns and at most one targeted block read; inline.
The decision point each time was: "is the material for this gap spread across multiple prior focuses
that have NOT yet been read in this session's context?"

**What the kit already covers:** The delegation trigger is "3-4 files or classes." For synthesis
blocks there are no binary files — the "files" are corpus blocks. The MODEL TIER rule says
"STRUCTURAL comprehension → sonnet," which correctly maps synthesis work. The delegation principle
"DELEGATE heavy sweeps to sub-agents — default, not optional" covers this in spirit. But the
synthesis-specific version of the question ("is this the first time I'm reading these blocks in this
context?") is not named. Without it, a future agent might delegate every synthesis gap (adding
sub-agent boundary cost when the driver already has the material), or delegate none of them (inflating
the driver context by reading all prior blocks inline).

**Proposed rule (propose-never-apply):**

> **SYNTHESIS-FOCUS DELEGATION HEURISTIC.** For a SYNTHESIS-GUIDE focus (Delta 1 of this retro),
> the file-count delegation trigger ("3-4 files") does not apply — the sources are corpus blocks,
> not binaries. Use this heuristic instead:
>
> - **Delegate (sonnet tier)** when the gap draws on 4+ prior blocks that have NOT yet been read in
>   the current session's context. The sub-agent reads, synthesizes, and returns findings cited by
>   [Block N]; the driver spot-checks key citations and writes the guide section.
> - **Inline** when the material for the gap was already returned by a prior sweep in the SAME
>   session (the driver context already holds the relevant block contents), or when the gap can be
>   closed by reading at most 1-2 targeted blocks directly.
>
> Record the decision in the iteration-history tier column: `yes · sonnet (synthesis — N blocks,
> first read)` or `no · inline (material in-hand from MBP1-3 sweeps)`. The in-hand criterion is
> session-scoped: a gap that would be "in-hand" in session N+1 after a compaction is not in-hand
> and should be delegated again.

**Priority:** LOW — the existing "cognitive demand → sonnet" tier rule is sufficient guidance in
most cases; this delta names the synthesis-specific trigger condition that maps "first-time read of
N prior blocks" to the delegation decision, making the in-hand heuristic explicit. Most relevant
for a SYNTHESIS-GUIDE focus with 6+ gaps where the early delegated sweeps can amortize over later
inline iterations.

---

## Deduplication — considered, not re-proposed

1. **DESIGN/APPLIED block type (step 5 self-verify).** Already in PROMPT-LOOP.md: "For a
   DESIGN/APPLIED block, a high ratio is EXPECTED and healthy, NOT an exhaustion signal — Declare
   which type it is." Delta 1 extends this to the FOCUS level (a whole focus, not just one block).
   Delta 2 extends it to the specific verify-block WARN that fires on [Block N]-only citations. Neither
   re-proposes the per-block DESIGN/APPLIED declaration rule.

2. **SYNTHESIS-BLOCK REGISTRATION RULE (step 6).** Governs the LAST BLOCK of an evidence focus
   (synthesis at close). This focus is ALL synthesis blocks, not just the last one. No overlap.

3. **DOCUMENT MODE (§20).** That mode processes existing documents as input and produces evidence
   blocks. This focus produces a NEW document FROM existing evidence blocks. The direction of
   information flow is reversed. Not a re-proposal.

4. **KNOWN-OUTLINE DESIGN CORPUS variant (step 1).** Governs a pre-fixed gap list over EXTERNAL
   sources with a certifiability gate and optional batch-scout round. This focus has no external
   sources to gate on — all sources are already-verified corpus blocks. The variant's constraints
   (concurrent scouts must not write shared corpus state; record scout verdicts on disk) do not apply.
   Not a re-proposal.

5. **MODEL TIER rule (delegation paragraph).** Already governs sub-agent model choice by cognitive
   demand. Delta 3 names the synthesis-specific input to that decision ("first-time read of N blocks
   vs. already in-hand"), not a new tier.

6. **Prior retro deltas (station-config D1/D2, data-at-rest A/B/C, history-audit R1/R2/R3).** None
   of those seven deltas touch synthesis-focus patterns, guide-deliverable artifacts, or verify-block
   behavior on [Block N]-only citation blocks. No overlap.

---

## Run quality notes (not kit deltas — operational observations)

- All 6 self-verify sections passed; verify-block exited 0 on all blocks; the "ZERO file:line"
  WARN on B705-B707 was explained in each self-verify section.
- The deliverable `docs/module-best-practices.md` was written incrementally (one section per gap)
  and finalized in B710. Each section cites research blocks; the guide is actionable without reading
  the corpus.
- No §14 corrections were needed — this focus added no new claims that could contradict prior blocks;
  all claims are distillations of already-verified [CERT] findings.
- The "new multi-block → delegate; in-hand → inline" split worked cleanly: MBP1-3 used sonnet
  sweeps (B705: spot-checked 2 code citations before writing), MBP4-6 were inline once the driver
  context held the synthesis material. No compaction occurred across 6 iterations.
- `verify-state.sh` and `verify-sources.sh` both passed at focus close; `research-sdd-archive.sh`
  ran cleanly. `block_scope: shared-global` was correctly declared and working.
- RESEARCH-STATE header declared "DESIGN/APPLIED corpus — high [INFER] ratio is EXPECTED" at
  bootstrap — the correct framing that kept the ratio from reading as an exhaustion signal.
