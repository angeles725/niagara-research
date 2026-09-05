<!-- review-status: applied 2026-09-05 · kit e0b701a · shipped: #1 (§6 giant single-line artifact recipe), #2 (§3 mtime stamp when no git, PR #434) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-01 · Research-SDD self-retrospective

> Run reviewed: a BUILD/commissioning session (DashboardPan `-ux` HMI UX + chart), not a discovery loop.
> No blocks were written. Trigger: operator asked for a retro. This captures only the lessons that
> genuinely TRANSFER to the research-sdd loop; build-specific lessons live in the build-n4-module kit
> retro `2026-09-01-dashboardpan-hmi-touch-ux.md`. READ-ONLY on the kit — PROPOSES only (METHODOLOGY §18).

## Context

The session edited a Niagara dashboard SPA (`src/rc/index.html`) whose source embeds room photos and
planos as base64 data URIs on single multi-MB lines. This is the same shape as many RESEARCH targets:
a minified JS bundle, a packed HTML artifact, a data-URI-laden SVG. Two loop-relevant frictions surfaced.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | Add a "giant single-line artifact" recipe to the research tools guidance: when a target file has multi-MB lines (base64/minified), the `Read` tool blows the token budget and a naive `grep -n` dumps mega-lines. Navigate with `sed -n 'A,Bp' \| cut -c1-160` for known ranges and search with a line-length filter (`awk 'length<300'` or a python `for i,l in enumerate(f): if len(l)<N and pat.search(l)`). Re-verify edits with a syntax check on the extracted script. | `METHODOLOGY.md §6` (research tools) + `toolbelt/tool-registry.md` (text-artifact wrapper note) | This session: DashboardPan `index.html` base64 lines → `Read` failed with token-limit error twice; `sed`+`awk 'length<300'`+python filter recovered navigation. | new | MEDIUM |
| 2 | Reinforce the evidence-marker rule for scope/attribution questions when the version-control source is unavailable: if `git` is not a repo, answer "did X change?" from file mtime + built-artifact inspection, and mark it inference-grade — never assert an un-cited "nothing changed". | `METHODOLOGY.md §3` (markers) | "¿ColdRoomPan no se modificó?" answered from `ls --time-style` mtimes + `unzip -p … \| grep -c` on the jar, after `git` returned "not a repository". | reinforce | LOW |

## Already covered (do NOT re-add)
- **Preview/verify-before-mutate discipline** — the session's "iterate on `/hmi`, operator approves, THEN compile" mirrors the loop's existing "read the REAL source, don't derive from memory" and the build kit's own preview gate. No new rule needed; it is the same principle in a build context.
- **Cross-source before concluding** — the §3/three-sources rule already covers "verify before asserting absence"; delta #2 only asks to name the mtime/artifact fallback explicitly for the no-git case.

## Honest scope note
This was not a research investigation, so there is no block, no CATALOG row, no gap closed. The two deltas
are tooling/methodology transfers only. If the maintainer judges delta #1 too build-specific, it can be
dismissed here since it is already captured in full in the build-n4-module kit retro.
