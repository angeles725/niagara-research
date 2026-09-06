# Campaign 8 close — CHANGELOG.md draft for wave 2 (PR9–PR15) + wave 3 (PR16–PR20), apply-ready

Author: companero (Opus), 2026-09-06. Target: `niagara-tools/CHANGELOG.md` (repo root, NOT the kit subdir).
Style copied verbatim from the live `[v0.19.0]` wave-1 block: one `- **`script/doc` <short> (PRn):**` bullet per PR,
imperative one-line behaviour + key flags/exits/bats/smoke, a trailing `[ev: retro <slug>]` (or `[ev: corpus B<n>]`),
grouped under `### Added` / `### Changed`, closed by a `### References` block. Every fact below is drawn from the merged
retros / wave3 spec cited inline; anything not yet merged (PR16–PR20) is marked **PENDING**.

---

## Version decision — KEEP 0.19.0, NO bump (tag only)

**Proposal: the close PR does NOT bump to 0.19.1. Campaign 8 ships as a single `v0.19.0` minor release, tagged on the
final merged commit.** Reasoning:
- `tasks.md` C.3/C.5 are explicit: *"Confirm `VERSION` = 0.19.0"* and *"`git tag v0.19.0` on final merged commit; push
  tag"* — the whole campaign (all 15 PRs + close) was scoped as `v0.18.0 → v0.19.0` (tasks.md header).
- `VERSION` already reads `0.19.0`; PR8 (D12) prematurely renamed `## [Unreleased] → ## [v0.19.0]` after wave 1 only.
  So the CHANGELOG's `v0.19.0` section currently lists PR1–PR8 alone, while PR9–PR20 belong to the SAME release.
- **The close PR therefore EXTENDS the existing `## [v0.19.0]` section with the wave-2/3 entries below (not a new
  `0.19.1` section).** A 0.19.1 would wrongly imply wave 1 shipped and wave 2/3 are a patch on top; they are one campaign.
- SemVer: all deltas are additive tools + doc folds (no breaking change to an existing tool's contract) → a single minor
  is correct; the tag lands once, on the final merged commit.

**Apply mechanics:** move the two wave-headed `### Added`/`### Changed` blocks below INTO the existing `## [v0.19.0]`
section (append after the wave-1 `### Added` list, keeping one `### References` block that merges both lists), OR — if the
team prefers to stage them first — paste them under `## [Unreleased]` and fold into `v0.19.0` at tag time. Either way the
final tagged CHANGELOG has ONE `v0.19.0` covering PR1–PR20.

---

## Wave 2 (PR9–PR15) — apply-ready

### Added — Campaign 8 close Wave 2: station snapshot, bog audit, wb + servlet lints (PR9–PR12)

- **`station-snapshot.sh` (PR9):** `toolbelt/station-snapshot.sh <station-dir> <out-dir>` — pre/post-deploy audit-surface snapshot; copies `config.bog` + `console*.txt`, records history/alarm db pointers by path+size (never the db files), writes `manifest.json` with per-file sha256; source dir never opened for write; the NTFS/0777-mount guard strips `+x` from outputs and `cp -p` preserves the Windows mtimes (output mtime is not an ordering signal); exits 0 ok / 1 copy failure / 3 usage; SN1–SN5 bats; real smoke on PANCCADIA [ev: retro campaign8-station-snapshot].
- **`bog-audit.sh` (PR10):** `toolbelt/bog-audit.sh <config.bog|file.xml> --module <MOD>… [--source-dir <dir>] [--strict]` — station `config.bog` auditor, CHECK1–CHECK12 over an embedded python3 D10 BOG-XML grammar engine; runs from the bog alone for CHECK1/8/9/10/11/12, `--source-dir` adds the source-coupled CHECK2–CHECK7; CHECK11 proxy-link-safety is **FAIL** (own-module output linked to a Boolean/NumericWritable with no explicit fallback holds last state on stop/reload), CHECK12 dashboard-write-to-link-target is advisory WARN, and a frozen slot inherited from a framework superclass is CHECK5 WARN "possibly inherited" not a ghost FAIL; exits 0 clean / 1 any FAIL / 3 usage/python3-absent; real smoke on PANCCADIA (17 CHECK11 FAIL) [ev: retro campaign8-bog-audit].
- **`lint-wb-threading.sh` + wb conformance (PR11):** `toolbelt/lint-wb-threading.sh <wb-src-dir> [--strict]` — two heuristic WARNs over a `-wb` src tree: `ui-thread-traversal` (a `doInvoke` body calling `getNavChildren`/`getNavNodes`/`BqlQuery` without `invokeLater`/`BJobService`/`JobThread`) + an agent-breadth heuristic; WARN-only, exit 1 only under `--strict`. Ships with `slot-coverage.sh` WB-LEX1 (missing-lexicon exit-1 path), `verify-module.sh` WB-SCAFFOLD1 + WB-DEP1 (`check_wb_scaffold` + `check_phantom_dep`), and the `types/wb-widgets.md` DWB1 10-rule doctrine + chihuahua-wb exemplar; K19 routing [ev: retro campaign8-wb-audit].
- **`lint-servlet.sh` (PR12):** `toolbelt/lint-servlet.sh <ux-profile>/src` — BWebServlet security lint following callees ~depth-3; six checks: `auth-gate`, `input-400` (a numeric parse not inside a try/catch that returns 400), `unbounded-set`, `cache-nofinger`, `log-in-handler`, and `csrf-xrw-only` (an `X-Requested-With` guard with no `CsrfUtil`/`csrfToken` → WARN, B813); ships the `CsrfXrwOnly.java` fixture + LSV4 pin; exit 0 clean/WARN-only / 1 any FAIL / 3 usage; K19 routing [ev: retro campaign8-lint-servlet].

### Changed — Campaign 8 close Wave 2: build + deploy doctrine (PR13–PR15)

- **Post-deploy verification subsection (PR13):** `BUILD-LOOP.md §6.a` — an ordered post-deploy verification block (pre-snapshot → hot-reload watch → console triage → `schema-risk.sh`/`bog-audit.sh` re-run), gated on the CHECK11 proxy-link-safety result; the five step scripts are hard-pinned in `tests/kit-links.bats L7` [ev: retro campaign8-post-deploy-checklist].
- **Build-pipeline documentation (PR14):** `BUILD-LOOP.md §4.a` Gradle `niagara-module` task matrix (what each task does + the safe `clean slotomatic jar` combination) and `§4.b` `vendorVersion`/`bajaVersion` version-bump checklist + station-reload consequence; `tests/build-sh.bats` gains the `BS-lock` + `BS-lock-hint` exit-31 station-lock + `mirror-niagara-home.sh` regression pins [ev: corpus B807] [ev: corpus B795] [ev: retro campaign8-build-pipeline].
- **RT-control doctrine promotion (PR15):** the campaign-8 RT-control doctrine promoted into the `types/` core (doc-only): `types/logic.md` §RT-control-logic (PID anti-windup/NaN-to-fault/deadband, the §805.9 flowchart template — B805/B808), `types/logic-authoring.md` §history-ext (B804) and §"Slot types for externally written values" (the value-class→slot table: `double`/`BStatusNumeric` write shapes, the oBIX child-leaf bare `<real>` preferred form + silent-zero fallback, the frozen-enum-carries-range rule — B823/B822/B825/B826/B828/B816), and a `types/dashboard.md` pointer [ev: retro campaign8-rt-doctrine].

---

## Wave 3 (PR16–PR20) — PLACEHOLDERS (fill the smoke/bats counts + retro slugs when each merges)

> Exact tool/doc names from `openspec/changes/build-n4-module-campaign8/wave3.md`. Each entry is **PENDING** until its
> PR merges and its retro lands; the retro slug token is the apply-time fill-in (grep-before-fold to confirm 0→credited).

### Added — Campaign 8 close Wave 3 (PR16–PR20) — PENDING

- **`new-retro.sh` + `kit-ticket.sh` (PR16, retro-loop) — PENDING:** `toolbelt/new-retro.sh <module-or-kit> [--ticket]` creates `retros/<date>-<slug>.md` from a template (line 1 `<!-- review-status: pending -->`, 4 sections), appends one `retros/INDEX.md` row, and sets `retro_pending: true` in `BUILD-STATE.md` — all three atomically, idempotent on re-run; `toolbelt/kit-ticket.sh "<title>" --from <retro-file>` opens a kit-repo GitHub issue (`gh issue create`, labels `kit,from-run`), SKIP+exit-0 when `gh` is absent/unauth (never fails a run); `skill/SKILL.md` close-of-run step + `BUILD-LOOP.md §7` retro gate (CD1) [ev: retro campaign8-retro-loop — PENDING].
- **`lint-structure.sh` + `types/structure.md` (PR18, structure) — PENDING:** `toolbelt/lint-structure.sh <module-root>` implements the L1–L11 structure lint (L1 package naming, L2 one @NiagaraType/file, L3 pure-model package no-baja, L4 lexicon non-empty, L5 palette non-empty for rt, L6 module-include.xml consistent, L7 3-part dep floors, L9 no empty skeleton, L10 no absolute host paths in tracked gradle.properties, L11 mixed pure+Baja srcTest declares both `:test-wb` and junit; L8 signed-jar stays in `verify-module.sh`); row `FAIL|WARN  lint-structure  <path>  L<n>: <reason>`; exits 0/1/3; `scaffold-module.sh` output passes L1–L11; `types/structure.md` B817 layout doctrine (L1–L11 summary) [ev: retro campaign8-structure — PENDING].
- **`lint-write-path.sh` (PR19, write-path) — PENDING:** `toolbelt/lint-write-path.sh <module-root> [--bog <config.bog>]` — every `Flags.OPERATOR` `@NiagaraProperty` MUST have a row in `docs/write-path-matrix.md` (slot · writer · timing · existing `src/test` test name); a missing row FAILs naming the slot (client smoke: 13 covered → exit 0; 22 dashboard-writable with 13 in matrix → FAIL the 9 uncovered) [ev: retro campaign8-write-path — PENDING].
- **`bog-audit.sh` CHECK13–CHECK19 (PR20, station-logic) — PENDING:** extends `toolbelt/bog-audit.sh` with CHECK13 relay-double-source (FAIL: two sources into one output relay slot), CHECK14 own-output-unlinked (WARN, suppressed when `hasDefrost=false`), CHECK15 sensor-crossed-by-name (WARN: `C{n}` slot sourcing an `E{m}` unit mismatch), CHECK16 hasDefrost↔DefrostController sibling (FAIL), CHECK17 roomN-index-mismatch (FAIL: ColdRoom_1 links addressing `evap3*`), + CHECK18/CHECK19 [ev: retro campaign8-station-logic — PENDING].

### Changed — Campaign 8 close Wave 3 — PENDING

- **Orchestration doctrine (PR17, doc-only) — PENDING:** `build-n4-module-kit/ORCHESTRATION.md` created (8 sections: roles, model table, delegation triggers, escalation gate, artifact store, evidence discipline, retro/ticket loop, recovery); `skill/SKILL.md` gains steps 1b (Explore shard, audit-first >3 files, sonnet), 1c (opus design shard for schema/new-slot/facade), 5b (peer QA RED-by-branch-name session before every code PR); `kit-links.bats L7` extension verifies every ORCHESTRATION-named script exists [ev: retro campaign8-orchestration — PENDING].

---

## `### References` block (apply-ready — merge into the single v0.19.0 References)

```
### References

- SDD change: `build-n4-module-campaign8` (PR1 … PR20 + close; v0.18.0 → v0.19.0, single minor — tagged `v0.19.0` on the final merged commit per tasks.md C.3/C.5, no 0.19.1 bump).
- Engram topics `sdd/build-n4-module-campaign8/*`.
- Research blocks folded (niagara-research): B788/B795/B800/B801/B803/B804/B805/B807/B808/B809/B810/B813/B816/B817/B821/B822/B823/B825/B826/B828/B829.
- Retros (campaign-8): campaign8-lint-delays, -triage-console, -lint-timers-ext, -facets-lint, -slot-per-slot, -rc-scan, -doctrine-fold, -report-integration, -station-snapshot, -bog-audit, -wb-audit, -lint-servlet, -post-deploy-checklist, -build-pipeline, -rt-doctrine (+ wave-3: -retro-loop, -orchestration, -structure, -write-path, -station-logic when merged).
```

---

## Notes for the apply worker
- **Re-run at apply:** the wave-2 entries are settled (PR9–PR15 merged, main `cc428e5`); PR16–PR20 fill-ins wait on those
  merges — confirm each tool's final flags/exits against the shipped script before pasting (the wave3.md spec is the
  intent, the merged script is the truth — C7-close L1 / campaign8-facets-lint: real counts over design estimates).
- **One References block:** do not duplicate — merge the wave-1 References list with the block above so `v0.19.0` has one.
- **Version line:** leave `VERSION` at `0.19.0`; the close PR's only version action is `git tag v0.19.0` + push (C.5).
