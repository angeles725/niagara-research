# §18 meta-retro — the retro step is discipline-triggered, not gate-enforced (2026-08-25)

<!-- review-status: pending -->

> Operator-prompted meta-retrospective: "why did the retros only become visible when I reminded you — isn't
> the retro part supposed to be built into /research-sdd? Does something need improving so retros are followed
> to the letter?" This retro answers that and proposes the fix.

## What actually happened (the record, for honesty)

The two focus retros WERE created proactively at focus-close, self-initiated per the §18 TERMINAL TRIGGER —
NOT only after the operator asked. Git timestamps:
- `2f50849` `research(niagara/framework-drivers): §18 closure retro` — **12:02:54**
- `5e04e65` `research(niagara/apis): §18 closure retro` — **13:32:41**
- `2c22667` `research(niagara/apis): retro D5` — **13:59:03**

The operator's "do you have retros?" question came after all three. The project history holds ~29 §18 retros
(one per focus close) — the discipline IS being followed. The real defect was COMMUNICATION: the retros were
mentioned only in passing in the close reports, so they surfaced to the operator only on request.

## The genuine gap this exposes

The §18 self-retrospective is **trigger-by-discipline**, not **trigger-by-gate**. Contrast:
- `verify-block` / `verify-state` / `verify-sources` are GATES — they refuse to proceed / the archive driver
  REFUSES (exit 3) when they fail.
- The §18 retro has NO equivalent enforcement. It is (a) a checklist step in PROMPT-LOOP's TERMINAL TRIGGER,
  and (b) surfaced AFTER the fact by `sweep-retros.sh` (a supervision sweep, run separately by the human/cron).
  Nothing at close-time GATES on the retro existing.

Consequently a retro can be silently skipped by: context loss before close, an abrupt session end, or — as
happened THIS session — a **manual close that bypasses `research-sdd-archive.sh` entirely** (see [D5]). The
skip produces no immediate signal; it is only caught later by a human running the sweep. Relying on the driver
to remember is exactly the failure mode gates exist to remove.

## Proposed deltas

**D6 — HIGH — Gate the focus close on retro existence (make the retro a first-class gate, not a checklist item).**
- Kit target: `toolbelt/research-sdd-archive.sh` (the gated close driver) + `PROMPT-LOOP.md` TERMINAL TRIGGER.
- Proposed: `research-sdd-archive.sh` should, alongside `verify-state`/`verify-sources`/`scan-secrets`, GATE the
  close on the existence of a `retros/<date>-<focus>-*.md` with `review-status: pending` for the closing
  focus — REFUSE the close checklist until it exists (same exit-3 tier). PROMPT-LOOP should restate the §18
  retro as a HARD close-gate, not an optional/last step. This converts "trigger-by-discipline" into
  "trigger-by-gate", matching the other close artifacts.
- Coupling: this ONLY helps if the close actually runs the archive driver. Since this corpus's shared-global
  numbering makes the archive driver refuse (D5), and the manual close bypasses all gates, D6 depends on D5
  being fixed first (so the archive driver is usable) — otherwise the retro gate is bypassed with everything
  else. Fix D5, then D6 makes the retro non-skippable.

**D7 — LOW — Surface the retro as a first-class close deliverable in the RETURN CONTRACT.**
- Kit target: `PROMPT-LOOP.md` RETURN CONTRACT / close-report format.
- Evidence: this session's retros were created but mentioned only in passing, so the operator could not see
  them without asking. Proposed: the close report MUST print `retro: <path> (N deltas, review-status: pending)`
  as an explicit line, so the human always sees the retro was written and where — the same way block/commit are
  reported. A written-but-invisible deliverable reads as a missing one.

## Reinforced

- The §18 mechanism itself works and is followed (29 retros in history; both this session's focuses got one at
  close, self-initiated). The gap is ENFORCEMENT + VISIBILITY, not the existence of the step.
- Ties to [D5] (2026-08-25-apis-closure addendum): the manual-close path bypasses ALL gates including any future
  retro gate; both point at "the mechanized close must be usable and used."
