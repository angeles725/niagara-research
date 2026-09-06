# C8 PR16 — retro/ticket loop: apply-ready CONTENT draft

> For the PR16 apply worker: WIRE the scripts to emit exactly the templates below; do not invent shape. The
> content is modeled on the REAL campaign-8 kit retros (`build-n4-module-kit/retros/2026-09-05-campaign8-*.md`)
> and the gates that read them (`toolbelt/sweep-fold-audit.sh`, `toolbelt/sweep-build-state.sh`, `BUILD-LOOP.md`
> §7). Gate-critical, non-negotiable elements are flagged **[GATE]**. WHY this loop exists: Part 5.

---

## 1. `new-retro.sh` — the retro STUB it emits
`new-retro.sh <module|kit> <slug>` writes `retros/<date>-<slug>.md` (date = `date +%Y-%m-%d`) and appends the
INDEX row (Part 2). The emitted file:

```markdown
<!-- review-status: pending -->
# <date> · <module|kit> · <slug>

**Session**: <campaign/PR context — one line>
**Delta count**: <N>   <!-- [GATE] must equal the Proposed-kit-deltas table row count = the INDEX `deltas` column -->

## What happened
<one paragraph: the TRIGGER — what run/change/defect produced this retro>

## Evidence
- <console row / test name / commit / file:line>, each with a token: `[ev: corpus B<n>]` / `[ev: <console file>]` / `[ev: <commit sha>]`

## Proposed kit deltas (propose-never-apply)
| Δ | Delta | Target file / § | Token |
|---|---|---|---|
| Δ1 | <the change, one line> | `<kit file>` § `<exact heading>` | `[ev: corpus B<n>]` |
<!-- [GATE] when a Δ is later FOLDED, the kit doc line carries `[ev: retro <slug>]` — that token is what
     sweep-fold-audit.sh harvests to justify flipping this retro's INDEX row to `folded`. The <slug> here IS
     that token; keep it `[A-Za-z0-9][A-Za-z0-9._-]+`. -->

## Lessons
- <≤5 bullets; each a durable rule, not a narration>

---
**Status**: PENDING — INDEX row appended: `| <date>-<slug>.md | <module|kit> | <date> | pending | <N> |`
```

Notes for the apply worker:
- **[GATE]** first line is EXACTLY `<!-- review-status: pending -->` — the close gate + INDEX derive status from it.
- The stub is deliberately LIGHTER than a hand-written campaign-8 retro (which uses `## Context` + `## Proven
  Lessons` / `### Delta N`); it carries the same gate-critical parts (marker, delta table with tokens, INDEX row).
  A run may expand `## Lessons` into `## Proven Lessons` with `### Δn` blocks — same tokens, richer prose.
- `slug` is kebab-case, unique in `retros/`; `new-retro.sh` refuses to overwrite an existing file.

## 2. INDEX.md row + BUILD-STATE.md envelope
- **INDEX.md** — columns are FIXED (copy verbatim from `retros/INDEX.md`): `| Retro file | Module | Date |
  review-status | deltas |`. `new-retro.sh` appends, keeping the file date-sorted:
  ```
  | <date>-<slug>.md | <module|kit> | <date> | pending | <N> |
  ```
  **[GATE]** `review-status` ∈ {`pending`,`folded`}; `deltas` = the Δ-row count (0 = not enumerated). A `folded`
  row MUST have a matching `[ev: retro <slug>]` somewhere in the kit corpus or `sweep-fold-audit.sh` fails.
- **BUILD-STATE.md** — in the relevant `build-state.v1` envelope (the module's, or the `kit` self-section), set:
  ```
  retro_pending: true    # GATED — a retro is OWED for the last kit-affecting run; flips to false once it exists
  ```
  Lifecycle: a run that changes kit behavior sets `retro_pending: true`; `new-retro.sh` writing the stub (+ its
  INDEX row) is what lets the close gate flip it back to `false`. `retro_required: true` stays as the standing
  "this run could owe a retro" flag. **[GATE]** the pairing rule (`BUILD-LOOP.md` §7 envelope-pairing): the
  retro/INDEX anchor and the `BUILD-STATE.md` envelope change land in the SAME push range.

## 3. `kit-ticket.sh` — the kit-defect ticket body
`kit-ticket.sh "<one-line>"` opens a `kit` issue (or writes `retros/tickets/<date>-<slug>.md` if offline) with:

```markdown
[kit] <one-line title>

**Retro**: retros/<date>-<slug>.md
**What happened**: <the one-paragraph trigger, copied from the retro's "What happened">

**Proposed kit deltas**
| Δ | Delta | Target file / § | Token |
|---|---|---|---|
| Δ1 | <the change> | `<kit file>` § `<heading>` | `[ev: corpus B<n>]` |

Labels: `kit`, `from-run`, `campaign-9`
```
Use it when a run finds a defect in a KIT CHECK or DOCTRINE (a lint that misses/over-fires, a stale rule, a
missing gate) — distinct from a per-module retro. The ticket points back at the retro; it does not replace it.

## 4. BUILD-LOOP §7 + SKILL.md wording
- **`BUILD-LOOP.md` §7** (`## 7. Retro + close (HARD close gate — not optional)`) — add, near the top of the
  section (before the existing close-exit list, which stays intact):
  ```
  - **Every run ends by writing its retro** — run `toolbelt/new-retro.sh <module|kit> <slug>` and fill the stub
    (§1); a defect in a KIT CHECK or DOCTRINE additionally opens `toolbelt/kit-ticket.sh "<one line>"`. The retro
    is a PRECONDITION for "done", not an at-STOP afterthought — `toolbelt/sweep-build-state.sh --age` at orient
    (BUILD-LOOP §0.a) surfaces the accrued retro debt so it cannot be skipped across a continuous chain.
    [ev: retro research-sdd-retro-automation]
  ```
- **`skill/SKILL.md`** — Execution Steps, replace step 6 tail so the close step names the tool:
  ```
  6. Retro + close (HARD gate): run `toolbelt/new-retro.sh <module|kit> <slug>` and fill the stub (What happened /
     Evidence / Proposed kit deltas / Lessons); a kit-check/doctrine defect also opens `toolbelt/kit-ticket.sh`.
     Update `$KIT/BUILD-STATE.md` (envelope + `retro_pending`) — or declare `Retro: none (trivial: <reason>)`.
     `sweep-build-state.sh --age` at orient shows outstanding retro debt.
  ```

## 5. WHY — the doctrine this loop encodes (fold as the rationale)
Fold from the research-sdd retro `retros/2026-09-05-research-sdd-retro-automation-and-campaign8-backlog-retro.md`
(niagara-research) — **the defect it names is identical to the build-kit's**:
- **Defect**: §-close retros fire only at STOP / focus-close. In a continuous lead-delegated chain (one unit →
  next task → next unit) a STOP is NEVER reached, so the at-STOP trigger never arms and retros get skipped;
  observed live at ~8:1 (blocks landed : retros written) until the operator asked why. Reconstructing a retro from
  memory at the end is also when context is thinnest. `[ev: retro research-sdd-retro-automation §A]`
- **Fix (what PR16 implements)**: make the retro a per-run PRECONDITION with a visible DEBT counter — the
  `new-retro.sh` stub is the incremental sink (each run's "Kit implication" lands immediately, not re-gathered at
  STOP), and `sweep-build-state.sh --age` is the RETRO-DUE surface that makes the debt un-skippable. Same shape as
  the verify gate. `[ev: retro research-sdd-retro-automation §A.1 (retro-debt counter proposal)]`
- **Consequence traced**: without the sink, kit-deltas live only in commit messages + operator trust, not in a
  `retros/` file the kit sweep reads — exactly the state PR16 closes. `[ev: retro research-sdd-retro-automation]`

---
## Apply-worker checklist
- Emit the §1 stub VERBATIM (marker first line; delta-count = table rows = INDEX `deltas`).
- INDEX columns copied EXACTLY from `retros/INDEX.md`; `new-retro.sh` appends, never rewrites existing rows (K12).
- The scripts land named in BOTH `BUILD-LOOP.md` and `skill/SKILL.md` or `kit-links.bats` L4/L5 go RED (K19).
- PR16 is itself a kit change → its own close: `new-retro.sh kit c8-pr16-retro-loop` + `retro_pending` in the kit
  self-envelope, same push range (envelope-pairing).
