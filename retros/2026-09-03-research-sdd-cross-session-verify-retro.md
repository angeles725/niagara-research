<!-- review-status: applied 2026-09-05 · kit e0b701a · shipped: #1 (§3 teammate claim carries no marker, PR #434), #2 (§6 giant single-line artifact recipe — merged with large-single-file #1) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective (2/2)

> SECOND research-sdd retro of the day. The FIRST (`2026-09-03-research-sdd-obix-quick-mode-retro.md`)
> covers the only discovery-shaped work (the oBIX quick-mode question). This one covers the LATER window:
> a live-incident build/fix (soft-start staggered startup, ColdRoomPan/CompPan v2.0.1→v2.0.3) plus
> cross-session coordination with a peer Claude session ("Panccadia"). No research loop ran here — so this
> captures only the two items that genuinely TRANSFER to the research-sdd loop; the build lessons live in
> the build-n4-module kit retro `2026-09-03-soft-start-staggered-startup.md`. READ-ONLY on the kit —
> PROPOSES only (METHODOLOGY §18).

## Context
A peer session asked for the DashboardPan source to make it responsive and asserted it was "Vue + D3".
I verified against the source before agreeing, corrected them (it is a vanilla single-file SPA with
hand-rolled SVG), and coordinated a non-conflicting branch. The verification, and a grep gotcha it exposed,
are the transferable bits.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence (block / commit / § / transcript ref) | Type | Priority |
|---|---|---|---|---|---|
| 1 | A TEAMMATE's (or any cross-session) technical claim carries no evidence marker until verified against the source — treat it exactly like an un-cited `[INFER]`, not as a given, before acting on it. Naming the source of a claim ("a peer says X") is provenance, not proof. | `METHODOLOGY.md §3` (markers) + §14 (cross-block consistency) | Peer asserted the DashboardPan SPA is "Vue + D3"; verifying against source (0 external `<script src>`, 0 `new Vue`/`d3.*` on real code lines, 100+ vanilla DOM calls) showed it is vanilla + hand-rolled SVG. Acting on the wrong claim would have derailed the peer's responsive work. | reinforce | MEDIUM |
| 2 | A substring grep for a keyword to CLASSIFY a file (framework detection, "does it use X") lies on a data-URI/base64-laden file, exactly as it does when LOCATING a symbol — apply the line-length filter (`awk 'length<300'` / a python `len(line)<N` guard) before grepping for the classification too. | `METHODOLOGY.md §6` (research tools) + the giant-single-line recipe from `retros/2026-09-01-large-single-file-navigation-retro.md` | This session: `grep -ci "vue|d3"` on `index.html` returned 800+ hits (all base64 noise); `awk 'length<300' \| grep` returned 0 real hits → the correct "not Vue/D3" verdict. | reinforce | LOW |

## Already covered (do NOT re-add)
- **Three-sources / verify-before-assert** — delta #1 is an APPLICATION of the existing §3/§5 discipline to
  a cross-session claim, not a new rule; it only asks to name that case explicitly.
- **Giant single-line artifact navigation** — delta #2 extends the existing 2026-09-01 lesson from
  "locating a symbol" to "classifying the file"; same technique, same target section.
- **Quick-mode terminal = engram finding + seed** — proposed in the first (oBIX) retro of the day; not repeated.

## Honest scope note
This window was NOT a research investigation: no block, no CATALOG row, no gap closed. Both deltas are
small reinforcements to the evidence-discipline sections, surfaced by a peer-coordination episode rather
than a discovery loop. The substantive research retro for 2026-09-03 is the oBIX one.
