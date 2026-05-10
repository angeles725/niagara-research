# SDD Spec — mapping-mx60

**Date**: 2026-05-09
**Phase**: sdd-spec
**Reads**: proposal.md (engram #1248), explore.md (engram #1247), reflow-spec (engram #1212), schema.md (reflow-clean-177)

---

## Coverage matrix

| Area | REQ IDs |
|------|---------|
| Schema core fields | REQ-1 |
| Schema envelope | REQ-2 |
| Backend extension block | REQ-3 |
| IIFE `frontend_iife` extension block | REQ-4 |
| `kind` enum MX60 extension | REQ-5 |
| Dual-form index (MD + JSON) | REQ-6 |
| Domain deep-dives (17 domains) | REQ-7 |
| Coverage and fidelity thresholds | REQ-8 |
| Spot-check and `verified_at` | REQ-9 |
| Excluded paths documented | REQ-10 |
| Delta-vs-reflow dual-form deliverable | REQ-11 |
| Xref layer completeness | REQ-12 |
| Multi-shard hard cap (≤75 entries/shard) | REQ-13 |
| Empirical-vs-inferred distinction | REQ-14 |

---

## Requirements

---

### REQ-1: Core schema fields mandatory for every mapped entry

**Statement**: Every entry in `index.json` MUST contain non-null, non-empty values for the 9 mandatory core fields: `id`, `path`, `kind`, `domain`, `purpose`, `loc`, `status`, `dependencies` (may be `[]`), and must have `source_doc` and `verified_at` keys present (values may be `null` for non-spot-checked entries).

**Rationale**: A single missing mandatory field silently corrupts jq queries that select on that field, making the entire catalog unreliable.

**Acceptance scenarios**:
- GIVEN a freshly committed `index.json`
  WHEN `jq '[.entries[] | select(.id == null or .path == null or .kind == null or .domain == null or .purpose == null or .loc == null or .status == null)] | length' index.json`
  THEN result is `0`
- GIVEN any single entry in `index.json`
  WHEN the entry is read
  THEN `dependencies` key is present (value `[]` is acceptable)
  AND `source_doc` key is present (value `null` is acceptable)
  AND `verified_at` key is present (value `null` is acceptable for non-spot-checked entries)
- GIVEN a binary static asset (e.g., a JPEG image)
  WHEN its entry is inspected
  THEN `loc` is `0`, `status` is `resource`, `dependencies` is `[]`

**Verification method**: `jq '[.entries[] | select(.id == null or .path == null or .kind == null)] | length' index.json` returns `0`; spot-check 3 entries manually.

---

### REQ-2: Top-level JSON envelope fields are complete and valid

**Statement**: `index.json` MUST contain a top-level envelope with `schema_version`, `module`, `source_repo`, `generated_at`, `entries` (array), and `exclusions` (array); `schema_version` MUST be `"1.0"`, `module` MUST be `"mx60-chihuahua"`.

**Rationale**: The envelope makes the artifact self-describing and verifiable without reading any companion doc; schema_version enables forward-compatibility guards.

**Acceptance scenarios**:
- GIVEN `index.json` at `docs/mappings/mx60-chihuahua/index.json`
  WHEN `jq '{schema_version, module, source_repo, generated_at}' index.json`
  THEN `schema_version` is `"1.0"`, `module` is `"mx60-chihuahua"`, `source_repo` is a non-empty string, `generated_at` matches ISO 8601 format
- GIVEN the same file
  WHEN `jq '.entries | type' index.json`
  THEN result is `"array"`
  AND `jq '.exclusions | type' index.json` result is `"array"`

**Verification method**: `jq '{schema_version, module, source_repo, generated_at, entries_count: (.entries|length), exclusions_count: (.exclusions|length)}' index.json` — inspect all fields in one pass.

---

### REQ-3: Backend extension block mandatory for every java-class entry

**Statement**: Every entry with `kind: "java-class"` MUST include a `backend` extension block containing: `profile` (enum `rt`|`ux`), `package` (string), `bcomponent_type` (string or null), `slots` (integer or null), `actions` (array, may be `[]`), `rest_endpoints` (array, may be `[]`), `box_methods` (array, may be `[]`), `decompiled` (boolean).

**Rationale**: Backend extension is the primary queryable signal for transplant planning — filtering by `profile`, `slots`, or `rest_endpoints` drives all bloque #68 §68.1–§68.2 analysis.

**Acceptance scenarios**:
- GIVEN the entry for `BChiDashboardService.java`
  WHEN its `backend` block is inspected
  THEN `profile` is `"rt"`, `package` is `"com.angeles.chihuahua.components"`, `bcomponent_type` is `"BComponent"`, `slots` is a positive integer, `decompiled` is `false`
- GIVEN the entry for `BChiServlet.java`
  WHEN its `backend` block is inspected
  THEN `rest_endpoints` is a non-empty array containing at least the entries for `/api/equipment` and `/api/alarms`
- GIVEN `jq '[.entries[] | select(.kind == "java-class") | select(.backend == null)] | length' index.json`
  THEN result is `0` (no java-class entry lacks a backend block)
- GIVEN the entry for `ChiEquipmentReader.java`
  WHEN its `backend` block is inspected
  THEN `bcomponent_type` is `null`, `slots` is `null` (it is not a BComponent), `decompiled` is `false`

**Verification method**: `jq '[.entries[] | select(.kind == "java-class") | select(.backend == null)] | length' index.json` must return `0`.

---

### REQ-4: `frontend_iife` extension block mandatory for every IIFE JS entry

**Statement**: Every entry with `kind` in `["iife-app", "iife-store", "iife-lib", "iife-util", "iife-entry"]` MUST include a `frontend_iife` extension block with fields: `namespace` (string), `globals_written` (string array, may be `[]`), `globals_read` (string array, may be `[]`), `iife_pattern` (enum: `"wrapped-window"` | `"wrapped-bare"` | `"iife-no-args"` | `"not-iife"`), `load_order_hint` (integer or null), `subscriber_role` (enum: `"consumer"` | `"producer"` | `"none"`).

**Rationale**: MX60 has no static `import` statements — the `frontend_iife` block is the ONLY machine-queryable representation of the namespace dependency graph across 40 IIFE JS files; it replaces `import` graphs for transplant planning (bloque #68 §68.5).

**Acceptance scenarios**:
- GIVEN the entry for `AlarmsManager.js`
  WHEN its `frontend_iife` block is inspected
  THEN `namespace` is `"MX60"`, `globals_written` contains `"MX60.AlarmsManager"`, `iife_pattern` is `"wrapped-window"`, `subscriber_role` is `"consumer"` or `"producer"`
- GIVEN the entry for `SubscriptionPool.js`
  WHEN its `frontend_iife` block is inspected
  THEN `globals_written` contains `"MX60.SubscriptionPool"`, `globals_read` contains at least one dependency, `subscriber_role` is `"producer"`
- GIVEN `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' index.json`
  THEN result is `0` (no IIFE entry lacks the block)
- GIVEN all 40 IIFE JS source files are indexed
  WHEN queried with `jq '[.entries[] | select(.frontend_iife != null)] | length' index.json`
  THEN result is ≥40

**Verification method**: `jq '[.entries[] | select(.kind | test("^iife-")) | select(.frontend_iife == null)] | length' index.json` must return `0`; manually verify `load_order_hint` for 3 entries (DashboardApp, SubscriptionPool, Router).

---

### REQ-5: `kind` enum extended with 5 MX60-specific IIFE values

**Statement**: `schema.md` for mx60-chihuahua MUST declare 5 new `kind` enum values — `iife-app`, `iife-store`, `iife-lib`, `iife-util`, `iife-entry` — in addition to inheriting all v1.0 core values; `index.json` MUST NOT contain any `kind` value outside the combined allowed set.

**Rationale**: Using the reflow kind enum unchanged would misclassify 40 IIFE JS files as generic `js-lib` or similar, breaking domain-specific queries and transplant analysis; IIFE kinds are architecturally distinct from Vue/Vuex kinds (no bundler, no static imports, global namespace).

**Acceptance scenarios**:
- GIVEN the MX60 `schema.md`
  WHEN the `kind` enum section is read
  THEN all 5 IIFE kind values appear with their definitions
  AND reflow v1.0 core kinds (`java-class`, `config`, `resource-image`, `module-descriptor`, etc.) are also listed as inherited
- GIVEN `index.json`
  WHEN `jq '[.entries[].kind] | unique' index.json`
  THEN every value in the output appears in the combined schema kind enum (no undocumented values)
- GIVEN all JS source files under `chihuahua-ux/src/rc/js/app/` and `js/lib/` and `js/util/`
  WHEN their entries are inspected
  THEN each has a kind in `["iife-app", "iife-store", "iife-lib", "iife-util", "iife-entry"]` — NOT `"js-lib"` or `"js-store"`

**Verification method**: `jq '[.entries[].kind] | unique' index.json` — cross-check each value against the declared enum in `schema.md`; zero unknown values allowed.

---

### REQ-6: Dual-form index — human-readable MD and machine-readable JSON

**Statement**: The mapping MUST exist in both `index.json` (valid JSON, parseable by jq v1.6+) and `index.md` (Markdown table, one row per entry, sorted ascending by `path`), co-located at `docs/mappings/mx60-chihuahua/`.

**Rationale**: JSON alone fails review workflows; MD alone fails programmatic transplant analysis; both forms must be consistent (same entry count, same paths).

**Acceptance scenarios**:
- GIVEN both `index.json` and `index.md` exist at `docs/mappings/mx60-chihuahua/`
  WHEN `jq '.entries | length' index.json` is compared to the row count of `index.md` (excluding header and separator)
  THEN the counts match (±0 tolerance)
- GIVEN `index.json`
  WHEN `jq '.' index.json > /dev/null`
  THEN exits with code 0 (valid JSON)
- GIVEN `index.md`
  WHEN the file is rendered in a Markdown viewer
  THEN every row has the same number of pipe-delimited columns as the header row
  AND rows are sorted ascending by the `path` column

**Verification method**: `jq '.entries | length' index.json` vs `rg '^\|' index.md | wc -l` (subtract 2 for header+separator); `jq '.' index.json > /dev/null` exit code check.

---

### REQ-7: Seventeen domain deep-dive documents follow a fixed 5-section template

**Statement**: Every file matching `docs/mappings/mx60-chihuahua/domains/<name>.md` MUST contain exactly these 5 sections in order: **Overview**, **Entry points**, **Components / classes**, **Cross-references**, **Notes & gotchas**; exactly 17 domain documents MUST exist (service-container, equipment-backend, http-rest, equipment-reader, alarms-backend, history-backend, schedules-backend, util-backend, app-shell, equipment-frontend, equipment-detail, alarms-frontend, schedules-frontend, history-frontend, baja-integration, ui-lib, threshold-stores).

**Rationale**: Consistent domain structure enables reviewers to navigate any domain without re-learning the layout; the 5-section template is the contract that reflow established and MX60 inherits.

**Acceptance scenarios**:
- GIVEN the `domains/` directory
  WHEN `fd '\.md$' docs/mappings/mx60-chihuahua/domains/ | wc -l`
  THEN result is `17`
- GIVEN any domain file (e.g., `domains/baja-integration.md`)
  WHEN its headings are extracted with `rg '^## ' domains/baja-integration.md`
  THEN the output contains exactly `Overview`, `Entry points`, `Components / classes`, `Cross-references`, `Notes & gotchas` — in that order
- GIVEN `domains/equipment-detail.md`
  WHEN the **Entry points** section is read
  THEN it references `UpDetail.js` as the primary entry point for the merged equipment-detail domain

**Verification method**: `fd '\.md$' docs/mappings/mx60-chihuahua/domains/` must list exactly 17 files; `rg '^## ' domains/*.md` must show the 5-section pattern for each.

---

### REQ-8: Coverage ≥95% of in-scope source files; spot-check fidelity ≥90%

**Statement**: The mapping MUST cover ≥95% of all non-excluded source files under `chihuahua/chihuahua/` (effective source root); the 16 test Java files, binary assets, `.idea/`, `.gradle/`, and `build/` directories are the ONLY permitted exclusions; a spot-check of ≥40 entries (≥2 per domain × 17 domains + extras) MUST achieve ≥90% `purpose` fidelity when verified against actual source files.

**Rationale**: Coverage and fidelity thresholds are the primary quality gates — a mapping that excludes too much or mislabels too many entries cannot serve as transplant ground-truth.

**Acceptance scenarios**:
- GIVEN the completed `index.json`
  WHEN the count of source entries (`jq '[.entries[] | select(.status == "source")] | length' index.json`) is compared to the count of non-excluded source files in the effective source root
  THEN the ratio is ≥0.95
- GIVEN a spot-check sample of ≥40 entries (drawn from all 17 domains)
  WHEN each entry's `purpose` is verified against the actual source file content
  THEN ≥36 of 40 entries (≥90%) have a `purpose` that correctly describes the file's primary responsibility without contradiction
- GIVEN a domain where spot-check fidelity falls below 90%
  WHEN detected during verification
  THEN the domain MUST be re-processed before `index.json` is committed

**Verification method**: `jq '[.entries[] | select(.status == "source")] | length' index.json` divided by `fd '\.java$|\.js$' <source-root> --exclude srcTest | wc -l`; spot-check documented in `_validation.md`.

---

### REQ-9: Spot-checked entries carry `verified_at`; synthesized entries carry `source_doc`

**Statement**: Every entry in the ≥40 spot-check sample MUST have `verified_at` set to a valid ISO 8601 datetime string; every entry whose `purpose` or extension fields were derived from a synthesis document (`HANDOFF.md`, `PORT-CHECKLIST.md`, openspec artifacts) MUST have `source_doc` set to a non-null object with `file` and `section` keys.

**Rationale**: Traceability from catalog entry back to source document is required for audit and for resolving discrepancies during transplant; null `verified_at` on spot-checked entries (the critical failure in the reflow mapping, per CRITICAL-1 in #1219) must not repeat.

**Acceptance scenarios**:
- GIVEN all entries with `verified_at != null`
  WHEN `jq '[.entries[] | select(.verified_at != null) | .verified_at] | length' index.json`
  THEN result is ≥40
- GIVEN any of those entries
  WHEN its `verified_at` value is parsed as ISO 8601
  THEN parsing succeeds (format: `"YYYY-MM-DDTHH:MM:SSZ"` or equivalent)
- GIVEN `jq '[.entries[] | select(.source_doc != null) | select(.source_doc.file == null or .source_doc.section == null)] | length' index.json`
  THEN result is `0` (no entry has a malformed source_doc object)

**Verification method**: `jq '[.entries[] | select(.verified_at != null)] | length' index.json` ≥ 40; `jq '[.entries[] | select(.source_doc != null) | .source_doc] | map(select(.file == null))' index.json` = `[]`.

---

### REQ-10: Excluded paths documented in `excluded.md` and `exclusions[]` array

**Statement**: Every path excluded from the mapping MUST appear in both `excluded.md` (human-readable, with reason) and the `exclusions[]` array in `index.json` (machine-readable); required exclusions are: 16 test Java files (chihuahua-rt/srcTest + chihuahua-ux/srcTest), `.idea/`, `.gradle/`, `build/` directories, outer Gradle wrapper files.

**Rationale**: Explicit exclusion documentation prevents coverage inflation and makes audit straightforward — reviewers must be able to determine why a file is absent from the catalog without reading source.

**Acceptance scenarios**:
- GIVEN `excluded.md`
  WHEN the file is read
  THEN it lists each of the 16 test Java files by name with reason `"Test files excluded per mapping convention"`
  AND `.idea/`, `.gradle/`, `build/` directories appear with their reasons
- GIVEN `index.json`
  WHEN `jq '.exclusions | length' index.json`
  THEN result is ≥3 (test files group + .idea + build, at minimum)
- GIVEN a test file path (e.g., `chihuahua-rt/srcTest/com/angeles/chihuahua/BChiDashboardServiceTest.java`)
  WHEN searched in `index.json` entries
  THEN it is NOT present in `.entries[]` — only in `.exclusions[]`

**Verification method**: `jq '.exclusions | length' index.json` ≥ 3; `rg 'srcTest' excluded.md` returns non-empty; spot-check that no srcTest path appears in `jq '.entries[].path' index.json`.

---

### REQ-11: Delta-vs-reflow deliverable in dual form (MD + JSON) with required columns

**Statement**: `docs/mappings/mx60-chihuahua/delta-vs-reflow.md` and `delta.json` MUST both exist; `delta.json` MUST use the locked schema with fields `mx60_id`, `reflow_id`, `status`, `loc_mx60`, `loc_reflow`, `loc_delta_pct`, `evidence`, `bloque68_section`, `notes`; `status` MUST be one of `HEREDADO`, `REESCRITO`, `FALTA`, `NUEVO`, `ANÁLOGO`; every bloque #68 §68.1–§68.5 prescription MUST be referenced by ≥1 delta row via `bloque68_section`; `evidence` MUST be a non-empty string (file:line citation).

**Rationale**: The delta is the differentiating deliverable converting bloque #68 theoretical prescriptions into empirical ground-truth; a delta row without evidence or bloque section reference cannot validate any transplant decision.

**Acceptance scenarios**:
- GIVEN `delta.json`
  WHEN `jq '{schema_version, module, compared_against}' delta.json`
  THEN `schema_version` is `"1.0"`, `module` is `"mx60-chihuahua"`, `compared_against` is `"reflow-clean-177"`
- GIVEN `jq '[.deltas[] | select(.evidence == null or .evidence == "")] | length' delta.json`
  THEN result is `0` (no row lacks evidence)
- GIVEN `jq '[.deltas[] | select(.status | IN("HEREDADO","REESCRITO","FALTA","NUEVO","ANÁLOGO") | not)] | length' delta.json`
  THEN result is `0` (no row has an invalid status)
- GIVEN `jq '[.deltas[] | select(.bloque68_section != null)] | map(.bloque68_section) | unique' delta.json`
  THEN the output contains at least `"§68.1"`, `"§68.2"`, `"§68.3"`, `"§68.4"`, `"§68.5"`
- GIVEN `delta-vs-reflow.md`
  WHEN the file is rendered
  THEN it contains a Markdown table with columns for MX60 component, Reflow component, Status, LOC values, Evidence, and Bloque #68 section

**Verification method**: All four jq assertions above; `rg '§68\.[12345]' delta.json` returns ≥5 matches; manual spot-check of 5 REESCRITO rows for evidence validity.

---

### REQ-12: Xref layer present with ≥80 entries and valid schema

**Statement**: `xref.json` MUST exist with ≥80 xref edges; each edge MUST have `from_id`, `to_id`, `usage_kind`, and `evidence` fields; `from_id` and `to_id` MUST match paths present in `index.json` entries; `xref.md` MUST be a human-readable companion listing the same edges.

**Rationale**: MX60 uses a global IIFE namespace with no static imports — xref is the ONLY machine-queryable dependency graph across the 40 IIFE files; without it, transplant task generation (which files must be migrated together) cannot be automated.

**Acceptance scenarios**:
- GIVEN `xref.json`
  WHEN `jq '.entries | length' xref.json` (or equivalent top-level array length)
  THEN result is ≥80
- GIVEN any xref edge
  WHEN `from_id` and `to_id` are extracted
  THEN both values appear in `jq '[.entries[].id]' index.json` (all xref endpoints are mapped entries)
- GIVEN `jq '[.[] | select(.from_id == null or .to_id == null or .usage_kind == null or .evidence == null)] | length' xref.json`
  THEN result is `0`
- GIVEN the entry for `UpDetail.js`
  WHEN all xref edges where `from_id` matches `UpDetail.js` path are counted
  THEN ≥10 outgoing edges exist (reflecting the 37-slot panel's high cardinality — per R2 mitigation)

**Verification method**: `jq '. | length' xref.json` ≥ 80 (adjust for top-level structure); `jq '[.[] | select(.from_id == null)] | length'` = 0; spot-check 5 edges for `to_id` validity against index.

---

### REQ-13: No shard produces more than 75 index entries

**Statement**: Each named shard (S1–S8) MUST contribute ≤75 entries to the final `index.json`; the combined total MUST fall in the 100–130 source entry range declared in the proposal; verification MUST be possible by grouping entries by domain and confirming shard domain assignments.

**Rationale**: The 75-entry hard cap (from #1231) is a token-budget discipline rule — exceeding it causes sub-agent context overflow and produces lower-fidelity entries; this cap is testable post-merge.

**Acceptance scenarios**:
- GIVEN the completed `index.json`
  WHEN entries are grouped by the domains assigned to each shard (S1: service-container + equipment-backend; S2: http-rest + equipment-reader + alarms-backend + history-backend + schedules-backend + util-backend; etc.)
  THEN no shard group exceeds 75 entries
- GIVEN `jq '.entries | length' index.json`
  THEN result is between 100 and 140 (source entries + resource/config entries)
- GIVEN the xref shard (S8)
  WHEN `jq '. | length' xref.json`
  THEN result is ≤100 (within the 80–100 estimate; hard cap does not formally apply to xref but estimate holds)

**Verification method**: `jq '[.entries[] | select(.domain | IN("service-container","equipment-backend"))] | length' index.json` ≤ 75 (repeat for each shard grouping); `jq '.entries | length' index.json` in [100, 140].

---

### REQ-14: Runtime behavior claims marked as inferred from mapping

**Statement**: Any `purpose`, extension field value, or `notes` entry in domain docs that describes runtime behavior not directly verifiable from static source reading MUST include the annotation `"**inferred from mapping, not verified empirically**"` (or equivalent marked phrase); approximately 15–20 entries are expected to carry this annotation (SubscriptionPool BajaScript lifecycle, controlTick timing, WritePoint dual-path fallback behavior).

**Rationale**: Per #1238 (clean-room-disconnected-asymmetry principle), runtime behavior claims in a static mapping artifact are not authoritative; annotating them prevents transplant decisions from being based on unverified behavioral assumptions.

**Acceptance scenarios**:
- GIVEN the entry for `SubscriptionPool.js`
  WHEN its `purpose` or the `Notes & gotchas` section of `domains/baja-integration.md` is read
  THEN the text contains `"inferred from mapping"` or equivalent disclaimer for any claim about BajaScript subscriber lifecycle timing
- GIVEN the entry for `BChiDashboardService.java`
  WHEN its `purpose` or `domains/service-container.md` Notes section is read
  THEN any claim about `controlTick` 10-second interval behavioral outcome contains an inferred annotation
- GIVEN `rg 'inferred from mapping' docs/mappings/mx60-chihuahua/ -r --include='*.md' --include='*.json' | wc -l`
  THEN result is ≥10 (conservative floor; expected 15–20)

**Verification method**: `rg 'inferred from mapping' docs/mappings/mx60-chihuahua/ -r | wc -l` ≥ 10; manual review of SubscriptionPool, WritePoint, BChiDashboardService (controlTick) entries.

---

## Out-of-scope

Per proposal §2 — explicit restatement:

- **Empirical runtime validation**: BajaScript latency, `controlTick` behavioral timing, `_bajaSetBroken` flag outcomes. These remain annotated as `**inferred from mapping**` (REQ-14 covers the annotation requirement; actual empirical testing is out of scope for this change).
- **Transplant execution**: Implementation SDDs `mx60-transplant-historydata` and `mx60-transplant-iife-to-pinia` are separate changes. This mapping is input to those, not a substitute.
- **Test files mapping**: 16 test `.java` files are excluded per reflow convention; Niagara plugin 7.3.40 test discovery is broken (per HANDOFF.md).
- **Re-mapping reflow**: `reflow-clean-177` mapping is frozen at v1.0. Any reflow schema corrections are separate.
- **UpDetail.js sub-section split**: Single entry, `purpose` ≤150 chars; `defined_at: {start, end}` extension deferred to `mx60-transplant-updetail`.
- **Binary asset machine-readable entries**: W-2 pattern from reflow — JPEG/PNG/font files catalogued in `excluded.md` but NOT required as machine-readable entries in `index.json` for this change (deferred to follow-up `mapping-binary-assets-mx60`).
- **Cross-references with AP-1..96 implementation patterns**: Separate SDD.
- **`frontend_vue` extension block**: Does not apply to MX60 (zero `.vue` files confirmed empirically).

---

## Acceptance verdict mapping

| Condition | Verdict |
|-----------|---------|
| All 14 REQs PASS | **ARCHIVE-READY** |
| REQ-11 or REQ-12 FAIL (delta or xref missing/malformed) | **MUST FIX** — these are the differentiating deliverables |
| REQ-1, REQ-3, or REQ-4 FAIL (core or extension fields missing) | **MUST FIX** — schema contract broken |
| REQ-8 coverage < 90% (hard floor) | **MUST FIX** |
| REQ-8 coverage 90–95% (below target but above floor) | **PASS-WITH-WARNING** — orchestrator decides |
| REQ-9 verified_at count < 40 | **MUST FIX** — repeat of CRITICAL-1 from reflow |
| REQ-13 shard cap exceeded | **PASS-WITH-WARNING** — log which shard, orchestrator decides |
| REQ-14 inferred annotations < 10 | **PASS-WITH-WARNING** — verify key entries manually |
| Any single REQ PASS-WITH-WARNING | Orchestrator reviews severity before archive |

---

## skill_resolution
- injected
