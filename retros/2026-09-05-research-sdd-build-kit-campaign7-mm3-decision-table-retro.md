<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — niagara-research · research-sdd · 2026-09-05 · Campaign-7 research (1/2): MM3 slot-change survival DECISION TABLE (B795) — kit delta for schema-risk.sh (#46)

> Run reviewed: campaign-7 research candidate 1 (ranked HIGH/HIGH). B795 mechanizes [B754]'s code-grounded
> saved-data survival matrix into a machine-readable classifier table so `schema-risk.sh` (niagara-tools issue
> #46, math-model MM3) has a single source of truth for SAFE/LOSSY/OUTAGE. READ-ONLY on the build kit — PROPOSES
> only (§18); I do NOT edit `$KIT`.

## Proposed kit deltas (for `/build-n4-module`)

| # | Proposed change | Target (file) | Evidence (block · key cite) | Priority |
|---|---|---|---|---|
| MM3Δ1 | Embed B795 §795.4 CSV VERBATIM as the schema-risk.sh classifier table (change_kind → verdict → evidence). The script parses a two-snapshot slot-diff into `change_kind` rows and returns the worst cell (OUTAGE>LOSSY>SAFE). | `schema-risk.sh` (new) / MM3 contract | B795 §795.4; B754 §754.6 | HIGH |
| MM3Δ2 | Encode the fail-safe as a hard contract: unresolved subtype → `*_unknown` row; unrecognized change_kind → `UNKNOWN` = OUTAGE. Never downgrade on uncertainty (a false SAFE ships the boot-loop). | `schema-risk.sh` / METHODOLOGY | B795 §795.2 (retro `slot-type-change-rompe-bog`) | HIGH |
| MM3Δ3 | Doc rule: the table is generated FROM B754/B795, never hand-edited in the script — a Niagara decode change updates B754 → B795 → the embedded CSV. | `METHODOLOGY.md` | B795 §795.5 | MED |

## The bite (why it is a real check, not decoration)
- The strongest cells are the two OUTAGE cases: `retype_simple` (unparseable saved `v=` propagates unwrapped —
  B739) and `remove_or_rename_enum_tag` (`InvalidEnumException`). Either one is a station that WILL NOT boot;
  the classifier catches both from a slot-diff, pre-deploy. RED→GREEN fixture: a before/after module pair with a
  persisted-slot retype must classify OUTAGE; an add-only pair must classify SAFE.
- The fail-safe is the point: `schema-risk.sh` that cannot resolve simple-vs-complex must return the WORSE cell,
  so the check never green-lights the ClassCastException boot-loop.

## Already covered (dedupe)
- The decode mechanism, warningAndSkip-vs-unwrapped-throw, and every matrix cell are [CERT] in [B754]; B795 adds
  only the machine-readable re-expression + the fail-safe collapse + the ext/package-move [INFER] rows (flagged).
- Version-floor half of deploy-safety = [B784] (separate; scaffold #45), not re-derived here.

## What went well (keep)
- Verified the three load-bearing cites (B754/B784/B791 H1 + B754 topic) BEFORE writing — B754 already owned the
  code grounding, so B795 is a faithful mechanization, not a re-derivation.
- Two OUTAGE cells that depend on an unresolvable subtype are collapsed to a documented fail-safe rather than
  guessed — honest about what a static slot-diff can and cannot prove (B795-G1/G2 named as requires-execution).
