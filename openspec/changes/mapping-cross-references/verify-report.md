# Verification Report: mapping-cross-references

**Change**: mapping-cross-references  
**Verified**: 2026-05-09  
**Verifier**: sdd-verify (Claude Sonnet 4.6)  
**Verdict**: FAIL

---

## Completeness

| Phase | Tasks | Complete (apply-progress) | Complete (tasks doc) |
|-------|-------|--------------------------|----------------------|
| Phase 1 (Batch A) | A-1.1..A-1.9 | YES | YES (marked [x]) |
| Phase 2 (Batch B) | B-2.1..B-2.5 | YES | NO (marked [ ]) |
| Phase 3 (Batch C) | C-3.1..C-3.4 | YES | NO (marked [ ]) |
| Phase 4 (Batch D) | D-4.1..D-4.12 | YES | NO (marked [ ]) |

**Note**: apply-progress (#1227) reports all tasks complete but tasks artifact (#1225) was never updated with [x] checkmarks for phases 2–4. Output files exist and are populated — this is a documentation gap, not a delivery gap.

---

## Build / Test Evidence

No automated test suite applicable (research/documentation repo). Verification executed via `jq` queries and structural inspection directly against output files.

- `jq -e . xref.json` → exit code 0 (PASS)
- File sizes: xref.json 389KB, xref.md 9KB, xref-schema.md present, xref-README.md present

---

## Spec Compliance Matrix

| REQ | Requirement | Result | Detail |
|-----|-------------|--------|--------|
| REQ-1 | Core schema fields on every entry | PARTIAL FAIL | See below |
| REQ-2 | Kind enum closed set (10 values) | PASS | Exactly 10 allowed values present, none extra |
| REQ-3 | usage_kind enum closed set (17 values) | FAIL | 4 values never used in data; not a closed-set violation but missing coverage |
| REQ-4 | id join with index.json for non-synthetic | FAIL | 217 of 525 non-synthetic ids not in index.json |
| REQ-5 | defined_at consistency (non-synthetic) | PARTIAL | java-class/store/mixin/plugin/lib-utility: 0 drift; vue-component: see REQ-1 |
| REQ-6 | Coverage thresholds | PARTIAL FAIL | See coverage table |
| REQ-7 | JSON valid + xref.md structure | PASS | jq -e exits 0; xref.md has summary table and Top Consumers |
| REQ-8 | unused flag invariant (100% of entries) | FAIL | 56 vue-component entries with unused==false when usage_count==0 |
| REQ-9 | $niagara disambiguation | PASS | $niagara: 53 used_at entries; box-method callers attributed (5/24); ws-command 0/14 (all stubs — expected per apply-progress) |
| REQ-10 | xref-schema.md complete | PASS WITH CAVEAT | schema_version 1.0, all 10 kinds, 17 usage_kinds, Analytics extension prototype (PIDController, not PsychrometricCalculator — spec said "e.g.") |

---

## REQ-1 Detail: Core Fields

All 615 entries have: `id`, `symbol`, `kind`, `used_at`, `usage_count`, `unused`. Envelope fields all present.

**Failures**:
- **220 vue-component entries lack `defined_at` key entirely** (36% of all entries). They use `path` (90 entries) or nothing (130 entries) instead of the spec-mandated `defined_at`.
- **305 `used_at[]` entries use `file` field instead of `path` field**. The spec (REQ-1) mandates `used_at[]` to have consistent fields with `path` as the location key. These 305 entries use `file` instead.
- **159 `used_at[]` entries have `path: null`**. All are vue-component entries from a shard batch that stored paths as null.
- **90 vue-component entries have a non-spec `callers[]` array** with `{caller, usage_kind}` structure that duplicates caller data in a different shape, while `used_at[]` remains empty for those same entries.
- **269 vue-component entries carry a non-spec `domain` field** (informational, not harmful but non-standard).

---

## REQ-3 Detail: usage_kind Coverage

13 of 17 spec values appear in the data. Missing 4 that were never emitted:
- `map-action` — Vuex mapActions calls (not found via rg during apply)
- `ws-call` — websocket commands (all 14 ws-commands are stubs with 0 callers; expected)
- `import-renamed` — aliased imports (documented as known limitation in schema)
- `dynamic-bind` — dynamic components (documented in schema)

**Assessment**: Not a closed-set violation (no extra values). Missing values are absent from data because callers don't exist or were not found. The enum is not "used" fully but its presence in the schema is correct.

---

## REQ-4 Detail: id Join

| Kind | xref count | matching index | NOT in index |
|------|-----------|----------------|--------------|
| java-class | 77 | 77 | **0 (PASS)** |
| store-module | 29 | 29 | **0 (PASS)** |
| mixin | 18 | 18 | 0 (PASS) |
| plugin | 13 | 13 | 0 (PASS) |
| lib-utility | 10 | 10 | 0 (PASS) |
| vue-component | 378 | 161 | **217 (FAIL)** |

The 217 vue-component failures break down into 3 inconsistent id formats:
1. `reflow-frontend/src/components/...vue` — 161 match index.json (CORRECT format)
2. `components/...vue` or `views/...vue` — 126 entries use stripped paths (missing `reflow-frontend/src/` prefix)
3. `vue-component/family/ComponentName` — 90 entries use semantic keys not present in index.json
4. 1 entry with other format

Synthetic kinds (rest-function, box-method, ws-command, rest-url): all 90 synthetic ids follow `{stem}#{symbol}` pattern, all unique. PASS.

---

## REQ-5 Detail: defined_at Consistency

For java-class, store-module, mixin, plugin, lib-utility: 0 drift cases (defined_at == index path for all 147 matching entries). **PASS.**

For vue-component: 220 entries lack `defined_at` key. Of the 158 that have it, no drift detected for the subset with matching index ids. The absence of `defined_at` in 220 entries is itself a REQ-1 + REQ-5 violation.

---

## REQ-6 Coverage Table

| Kind | Spec Target | Actual | Delta | Status |
|------|------------|--------|-------|--------|
| java-class | 77 | 77 | 0 | PASS |
| store-module | 30 | 29 | -1 | WARNING (missing: store/index.js — documented skip) |
| mixin | 18 | 18 | 0 | PASS |
| plugin | 13 | 13 | 0 | PASS |
| lib-utility | 10 | 10 | 0 | PASS |
| rest-function | 28 | 26 | -2 | WARNING (drift documented in apply-progress) |
| box-method | 21 (spec) / 24 (actual) | 24 | +3 | WARNING (index.json authoritative: apply found 24, not spec's 21) |
| ws-command | 11 (spec) / 14 (actual) | 14 | +3 | WARNING (3 infra exports included per apply-progress) |
| rest-url | 28 | 26 | -2 | WARNING (drift documented in apply-progress) |
| vue-component | ≥360 | 378 | +18 | PASS (count sufficient, id format issues separate) |

---

## REQ-8 Detail: unused Invariant

56 vue-component entries have `unused == false` when `usage_count == 0` and `used_at == []`. This is a direct invariant violation. The entries appear to be components where a `callers[]` array was populated with caller data but the `unused` flag was not recomputed after `used_at` was left empty (i.e., caller data went into a non-spec `callers[]` field instead of `used_at[]`, leaving the spec fields contradictory).

---

## REQ-9 Detail: $niagara Disambiguation

- `$niagara` plugin entry: `usage_count: 53`, `used_at` has 53 entries. PASS.
- `box-method`: 5 of 24 entries have callers (historyGetDevices, historyGetGroupTree, historyGetList, loadPointMap, refreshLicense). 19/24 unused — per apply-progress, api/box.js is mostly stubs. Acceptable.
- `rest-function`: 9 of 26 entries have callers. 17/26 unused.
- `ws-command`: 0 of 14 have callers. All stubs (Phase 5+, initSocket commented out). Per apply-progress: expected.
- Attribution logic adjusted: $niagara wraps native BAJA API (alarm/bql/nav/subscriber/history), NOT REST/BOX. REST calls go via `this.$api.rest.<name>`. This discovery changed attribution logic — documented correctly in apply-progress and tasks.

---

## Issues

### CRITICAL

**C-1 (REQ-1, REQ-5): 220 vue-component entries missing `defined_at` field**  
220 of 378 vue-component entries (58%) have no `defined_at` key. 90 use `path` instead, 130 use neither. REQ-1 mandates `defined_at` on every entry. REQ-5 mandates `defined_at == index.path`. Both are violated at scale.  
_Fix_: normalize all vue-component entries so `defined_at = path` (or `reflow-frontend/src/` + relative path where applicable).

**C-2 (REQ-4): 217 non-synthetic ids not joinable to index.json**  
217 vue-component entries use id formats that don't match index.json's `reflow-frontend/src/components/...vue` format. 126 use stripped paths, 90 use semantic `vue-component/family/Name` keys.  
_Fix_: rebuild vue-component ids from index.json entries, ensuring `id == index entry id`.

**C-3 (REQ-8): 56 vue-component entries with incorrect `unused` flag**  
`unused == false` when `usage_count == 0` and `used_at == []`. The invariant "unused iff usage_count == 0" fails for 56 entries (all vue-component from semantic-id shard).  
_Fix_: recompute `unused = (usage_count == 0)` after merging; OR migrate `callers[]` → `used_at[]` and recount.

**C-4 (REQ-1): 305 `used_at[]` entries use `file` field instead of `path`**  
used_at entries for mixin-ref callers use `{file, usage_kind}` instead of spec-mandated `{path, usage_kind}`. No caller location is addressable via the spec key.  
_Fix_: rename `file` → `path` in all used_at entries uniformly.

**C-5 (REQ-1): 159 `used_at[]` entries have `path: null`**  
All are vue-component entries from floorplans shard (reflow-frontend/src shard). Caller locations were not captured.  
_Fix_: re-run the rg sweep for floorplan components and capture caller file paths; or mark as unknown and exclude from usage_count.

### WARNING

**W-1 (REQ-6): store-module 29/30 — store/index.js not covered**  
Spec requires 30 (all). Only 29 present. The missing entry is `reflow-frontend/src/store/index.js`. Apply-progress documented this as an intentional skip (it's the root store aggregator, not a module per se). Needs explicit spec exception or addition.

**W-2 (REQ-6): rest-function 26/28, rest-url 26/28 — 2 drifts each**  
Apply-progress documents that actual file exports yielded 26, not spec's 28. The spec number was derived from an earlier count. Either spec requires update or 2 missing functions need to be added.

**W-3 (REQ-6): box-method 24, ws-command 14 — exceeds spec targets (21 and 11)**  
apply-progress explains +3 each from infra exports and additional discovered methods. index.json is considered authoritative (24 box, 14 ws). The spec numbers are outdated. This is a spec calibration issue, not an implementation error.

**W-4 (tasks): Phases 2–4 tasks never marked [x] in tasks artifact**  
All 615 entries exist and files are present, confirming phases B, C, D executed. But the tasks document (#1225) still shows [ ] for all B/C/D tasks. The apply-progress record (#1227) is the only completion evidence.

**W-5 (REQ-1): 90 vue-component entries have non-spec `callers[]` array**  
These entries carry caller data in a `callers[]{caller, usage_kind}` format (semantic-id shard) instead of `used_at[]{path, usage_kind}`. The data is present but in the wrong schema shape, causing `used_at` to be empty and `unused` to be wrong.

### SUGGESTION

**S-1 (REQ-3): usage_kind values `map-action`, `import-renamed`, `dynamic-bind` documented but never emitted**  
Three enum values are in the spec and schema but don't appear in the data. This is correct if no callers exist, but the apply phase should have verified whether mapActions patterns were searched. Recommend adding one explicit sweep for `mapActions` patterns to confirm zero vs. missed.

**S-2 (REQ-10): Analytics prototype uses `PIDController`, spec suggested `PsychrometricCalculator`**  
Spec said "e.g." so this is not a failure. But the prototype uses `dag-node` usage_kind (not `box-call` as spec suggested). Both are valid examples; `dag-node` is arguably more accurate for algorithm blocks.

**S-3: Non-spec fields (`domain`, `path`, `callers`) in vue-component entries**  
These are informational and don't break anything, but they make the schema contract harder to enforce downstream. Recommend stripping or moving to an `_metadata` extension field.

---

## Final Verdict: FAIL

5 CRITICAL issues, 5 WARNINGs, 3 SUGGESTIONs.

The implementation is substantially complete — all 615 entries exist, JSON is valid, coverage thresholds are met for most kinds, and non-vue kinds are correct. However, the vue-component shard (378 entries, 61% of all entries) has systematic schema normalization failures across 3 shards: wrong id format, missing `defined_at`, wrong field name for location in `used_at`, and incorrect `unused` flags. These are data-quality issues traceable to shard normalization bugs during apply.

**Not ready for archive.** Recommended action: re-run `sdd-apply` targeting the 5 CRITICAL issues only (scope: vue-component normalization pass).

