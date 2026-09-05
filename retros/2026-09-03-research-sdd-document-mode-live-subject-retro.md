<!-- review-status: applied 2026-09-05 · kit 7349004 · shipped: #1 (§20), #2 (§7), PR #434 272e1ad; #3 (DYNAMIC-SETUP headless Chromium, PR #446 7349004) -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective (3/3)

> THIRD research-sdd retro of the day. #1 `2026-09-03-research-sdd-obix-quick-mode-retro.md` (quick-mode),
> #2 `2026-09-03-research-sdd-cross-session-verify-retro.md` (cross-session verify). This one covers a
> **document-mode (§20)** run: capturing an already-built product — the "Panccadia 3D external viewer"
> (standalone HTML dashboard, Three.js nave + ported DashboardPan 2D, Supabase/oBIX data, mobile UX) — as
> target #30 `panccadia-3d-viewer`, 5 blocks + RUNBOOK. This is the CENTRAL kit-transfer retro that enters
> `sweep-retros.sh`; the per-run §18 retro lives in the corpus itself
> (`~/panccadia-3d-viewer/corpus/retros/2026-09-03-panccadia-3d-viewer-document-run.md`, commit `4547fe5`)
> — the deltas below consolidate its transferable findings into the niagara-research pipeline (the corpus is
> a SEPARATE repo the niagara-research sweep does not see). READ-ONLY on the kit — PROPOSES only (§18).

## Context
`/research-sdd` was invoked in document mode to formally document a product built the same session. The
subject `index.html` was under continuous LIVE edit while the corpus was authored (the operator kept asking
for changes: responsive fixes, title, favicon, cache-busting). The document-cycle ran clean (5 blocks pass
`verify-block`, archive gates ok), but three transferable gaps surfaced. Verification of the product (a
WebGL page) also produced a reusable headless-QA recipe.

## Proposed kit deltas

> Only genuinely NEW / transferable items. Anything already encoded is under "Already covered".

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Document-mode on a LOCAL, unversioned, working-tree file under live edit MUST first freeze a byte-stable snapshot (copy + record its md5/sha) and anchor every `file:line` cite to the snapshot — a git sha cannot name uncommitted working-tree bytes, and the live file moves under you. | `METHODOLOGY.md §20` (document mode), cross-ref `§5` (sources: beautified-temp / tag-pinning analogs) | This run: subject md5 drifted `1335aad0`→`dbd52496`→`433630f3` while authoring; cites anchored to the frozen `sources/subject-snapshot/index.html` (`dbd52496`) stayed reproducible — live-file cites would ALL be broken now (line numbers shifted ~+1 below line 561). | new | HIGH |
| 2 | Engram-mirror fallback for an UNREGISTERED target: `mem_save project:"<target>"` fails because auto-detect resolves to the ambient project; codify — save under the nearest same-client project with `research/<target>/*` topic keys AND declare the placement in RESEARCH-STATE. | `METHODOLOGY.md §7` (state/memory) | `panccadia-3d-viewer` is unregistered → auto-detect resolved to ambient `niagara-research`; mirrors saved under `pancaddia-leon-tunnel` with `research/panccadia-3d-viewer/*` keys and declared in RESEARCH-STATE §Notes. | refinement | MEDIUM |
| 3 | (toolchain · absorb) Headless-Chromium WebGL QA recipe: launch `--use-angle=swiftshader`; screenshots of a live WebGL page TIME OUT under software GL → capture via CDP `Page.captureScreenshot` (no stability wait) and/or switch off the WebGL tab first; relaunch the browser per viewport (context reuse throws "Failed to open a new tab"); serve the file on a fresh port and confirm what it serves (a stale server on a reused port caused a wrong-file false positive). | `toolbelt/DYNAMIC-SETUP.md` (+ register in `toolbelt/tool-registry.md`) | Verifying the 3D viewer: model-page `page.screenshot` timed out repeatedly; CDP + tab-switch + relaunch-per-viewport + fresh port (8799, not the stale 8791 serving the old 2D module) is what produced reliable evidence. | absorb | MEDIUM |

## Already covered (do NOT re-add)
- **Freeze / pin-source** has analogs — §5 beautified-temp copies, §5 go-src tag-pinning — but NONE covers a local working-tree file under live edit; #1 is that new case, not a re-statement.
- **`[INFER]` honesty** — the run correctly floored deploy-target, pipeline live-status, and the "3D v17" filename at `[INFER]` (B5 §5.6 even names why it is not `[CERT-doc]`). That is §3 working as written ("no citation ⇒ INFER") — no delta.
- **Verify-before-assert on cross-session claims** and the **giant-single-line grep filter** (`awk 'length<300'` before classify/locate) — already captured in `2026-09-03-research-sdd-cross-session-verify-retro.md`; this run reused both (verified peer claims; the demo-string audit filtered base64 noise).
- **One-block-per-commit + commit convention** — followed throughout the document cycle.

## Not a kit delta (attributed elsewhere, on purpose)
- The **Bash auto-approval classifier outage** (`claude-sonnet-4-6[1m] temporarily unavailable` → Bash blocked mid-session) is a Claude Code HARNESS/infra condition, not a research-sdd kit gap. Worked around by the operator switching permission mode / allowing `Bash(python3:*)`. Recorded here as environment context only — no kit change proposed.

## Meta
- Corpus: `~/panccadia-3d-viewer/corpus/` — 5 blocks + RUNBOOK, archive closed (CATALOG/INDEX regenerated), TARGETS.md #30 refreshed (5 md / 1 retro / document-cycle COMPLETE).
- Product shipped in parallel to `https://panccadia.angeles-group.org` (Cloudflare Pages, build 2026-09-03-5), snapshot mode until the mini-PC pipeline reconnects.
