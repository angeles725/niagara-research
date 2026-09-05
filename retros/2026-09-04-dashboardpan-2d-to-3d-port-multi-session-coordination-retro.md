<!-- review-status: applied 2026-09-05 · kit f73f5d6 · PARTIAL — shipped: #3 (§16 peer-owned dirty tree, PR #427); #4 in PR #434 (§3 teammate claim); DEFERRED: #1, #2, #5 (§11/§5) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · consulting · 2026-09-04 · DashboardPan 2D → 3D viewer port, multi-session coordination

> Run reviewed: a pure COORDINATION/CONSULTING session. This session ("Pan") held the DashboardPan 2D
> source-of-truth and fed a live port to two peer sessions: "3D" (the public Three.js viewer,
> panccadia.angeles-group.org) and "Codig" (CompPan rt control). No corpus block, no gap closed, no build.
> Work was: reconstruct the 2D→viewer data delta from git, map it to the oBIX facade, and hand it across
> session boundaries verbatim. READ-ONLY on the kit — PROPOSES only (§18).

## Context

The operator drove it prompt-by-prompt. Four coordination rounds, each answered from evidence (git +
grep + source read), never from memory:

1. **The "8 ords since deploy" delta.** @3D asked what changed in DashboardPan 2D since the viewer's
   build -7 deploy. Every module commit AND the deploy were dated 2026-09-03 — dates could not
   discriminate. The discriminator was empirical: `grep -c <ord>` against the viewer's own `index.html`
   (0 hits = not consumed = delta). Found 8 new ords from commit `7f2df6d` (process/timer keys +
   intercambiador); `fanState/comp*State` already present (2 hits) so NOT delta.
2. **Slot vs derived.** Of the 8, only 3 are real oBIX slots (`defrostActive`, `intercambiadorState`,
   `intercambiadorMode`); the 5 `*ElapsedMs`/`*RemainingMs` are computed by `DashboardReader.java` in the
   servlet and do NOT exist in the oBIX facade. A pipeline polling the facade by Batch would never find
   them — it must read the ANCHOR slots (`coolingSince`, `defrostStart`, `defrostDuration`,
   `nextDefrostTime`) and compute the ms itself. Only reading the reader revealed this.
3. **Condensadoras = uncommitted WIP.** @3D saw a "Condensadoras" panel not in my delta. `git status` +
   `git grep HEAD` showed it was working-tree-only (frontend + a `preview-mock.json`), no reader, no
   facade node. What the operator saw at :8092 was the preview server + mock, not the station.
4. **HOA already existed.** Operator said "add the per-compressor HOA mode." Before writing anything,
   `git diff` on CompPan showed Codig had ALREADY implemented it uncommitted (`condenser1/2/3Mode`,
   0/1/2). Asked Codig for the contract instead of duplicating/clobbering. Codig surfaced a SAFETY caveat
   (ON bypasses the discharge-high cutout) and a SENSOR caveat (`condenserNRunning` is amps-derived,
   unreliable → use command slot `condenserN`). The exact CompPan ORD was in no export; the operator gave
   it: `Programacion/CompresorControl`.

## Proposed kit deltas

> Only genuinely NEW items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | To prove a "what changed since X" DELTA when timestamps don't discriminate (same-day commits), check the CONSUMER for ABSENCE, not the producer's commit boundary. `grep -c <symbol>` against the artifact that would consume it: 0 hits = genuine delta; present = already had it. Empirical and cheap; beats trying to pin a commit/deploy boundary by date. | `METHODOLOGY.md §11` (self-verify) | 8 module commits + the viewer deploy all dated 2026-09-03; dates useless. grep=0 in `panccadia-3d-viewer/index.html` cleanly separated the 8 real-delta ords from `fanState/comp*State` (2 hits, already present). | new | HIGH |
| 2 | When handing a data contract across a servlet/facade boundary, distinguish a REAL slot from a reader-DERIVED value BEFORE naming the ords. Read the reader (e.g. `DashboardReader.java`): a value the servlet computes at read-time (`now − anchor`) is NOT in the oBIX facade and a facade poller will never find it — it must read the anchor slots and recompute. | `METHODOLOGY.md §5` (sources) + `§11` | 5 of 8 ords (`*ElapsedMs`/`*RemainingMs`) were servlet-computed from BAbsTime anchors; only 3 were real slots. A facade-Batch pipeline needed the anchors (`coolingSince`/`defrostStart`/`defrostDuration`/`nextDefrostTime`) + the recompute, not the derived keys. | new | HIGH |
| 3 | Before editing a file in a repo shared by live peer sessions, run `git status`/`git diff` on the target FIRST. A dirty working tree is a collision risk AND may already contain the requested change. Coordinate with the owning session rather than duplicating or clobbering uncommitted work. | `METHODOLOGY.md §16` (multi-focus) or a new multi-session §; `build-n4-module METHODOLOGY` | Operator said "add the HOA mode"; `git diff` showed Codig had already implemented `condenser1/2/3Mode` uncommitted (~115+107 lines). Asking Codig for the contract avoided a clobber and surfaced two caveats I would have missed. | new | HIGH |
| 4 | A component's station MOUNT/ORD (`Programacion/CompresorControl`) is integrator-placed config, NOT derivable from module source — a `BComponent` does not live under `/Services` by default. Do not fabricate a plausible path; state it is unknown and get it from a live oBIX nav or the operator. | `METHODOLOGY.md §3` (markers: no cite → [INFER]) | The CompPan ORD was in NO tunnel export (`points.json` had only `CuartoN/...` DashboardService keys). Refused to invent `/Services/...`; operator supplied `Programacion/CompresorControl`. | reinforce | MEDIUM |
| 5 | A control-WRITE contract is incomplete without its interlock/safety semantics. When relaying a write ord (HOA mode) across sessions, verify and surface what it bypasses. | `METHODOLOGY.md §5`; `build-n4-module` (control review) | Codig's HOA `ON` bypasses `dischargeHigh`/`suctionLow`/`stageDelay`, respecting only min-off. Surfaced to @3D (UI warning) and to the operator (pending RT decision) instead of relaying "0/1/2" alone. | new | MEDIUM |

## Already covered (do NOT re-add)

- **Read the real source, never memory** — every delta came from git diff + reading `DashboardReader.java`
  / `BRoomPanel.java` / `BCompressorControl.java`, not from recall. Textbook.
- **Cross-session lossless relay** — the orchestrator contract already governs handing choice envelopes /
  contracts across sessions verbatim; this session applied it (delta rows add only the *how-to-derive-the-
  content* wrinkles, not the relay discipline).
- **Engram proactive save** — mapping + delta saved to file-memory and engram as work progressed.

## Honest scope note

No discovery loop, no block, no CATALOG row, no gap closed, no build (the HOA was already Codig's). The
value delivered was COORDINATION: a verified, boundary-aware data contract handed to two peer sessions with
its caveats intact (derived-vs-slot, unreliable amps sensor, HOA safety bypass, uncommitted/no-deploy
state). Deltas #1–#3 are the reusable methodology wins; #4–#5 reinforce evidence and control-review
discipline. Everything stays MOCK/pending until CompPan is committed and the station is redeployed —
explicitly not done this session, per the operator.
