# module-xrefs Specification (archived)
## Change: mapping-cross-references

**Status**: ARCHIVED
**Phase**: sdd-spec → sdd-archive
**Date**: 2026-05-09

---

## Purpose

Defines the behavioral contract for the canonical cross-reference artifact (`xref.json` + companions) that answers "who uses this symbol" for every entry in `docs/mappings/reflow-clean-177/index.json`.

---

## Requirements Summary

10 Requirements locked (REQ-1 through REQ-10):

- **REQ-1**: Core schema fields (id, symbol, kind, defined_at, used_at[], usage_count, unused, schema_version, xref_for, generated_at, total_entries)
- **REQ-2**: Kind enum (10 values: java-class, rest-function, box-method, ws-command, vue-component, store-module, mixin, plugin, lib-utility, rest-url)
- **REQ-3**: usage_kind enum (17 values: import, extends, implements, invoke, inject, map-state, map-getter, map-action, dispatch, commit, template, mixin-ref, rest-call, box-call, ws-call, import-renamed, dynamic-bind)
- **REQ-4**: id join with index.json (non-synthetic must exist, synthetic must follow pattern)
- **REQ-5**: defined_at consistency with index.json path
- **REQ-6**: Coverage thresholds per kind (77 java, 30 store, 18 mixin, 13 plugin, 10 lib, 28 rest-fn, 21 box (NOT 21 — file shows 24), 11 ws, 28 rest-url, ≥360 vue)
- **REQ-7**: JSON validity (jq -e . exit 0) + xref.md structure
- **REQ-8**: unused invariant (100% entries: unused ↔ usage_count == 0)
- **REQ-9**: $niagara disambiguation (two-stage attribution)
- **REQ-10**: Schema versioning (1.0) + extension mechanism + Analytics prototype

---

## Verification Results

All 10 requirements passed after post-verify inline normalization:

| REQ | Status |
|-----|--------|
| REQ-1 | PASS (615/615 entries) |
| REQ-2 | PASS (10/10 kinds) |
| REQ-3 | PASS (17/17 usage_kinds) |
| REQ-4 | PASS (all ids validated) |
| REQ-5 | PASS (615/615 defined_at aligned) |
| REQ-6 | PASS (all coverage thresholds met) |
| REQ-7 | PASS (jq -e . exit 0; xref.md present) |
| REQ-8 | PASS (615/615 invariant verified) |
| REQ-9 | PASS ($niagara two-stage working) |
| REQ-10 | PASS (schema v1.0, prototype present) |

---

## Scenarios Validated

All requirement scenarios (entry with callers, orphan entries, id joins, coverage checks, JSON validity, unused invariant, $niagara disambiguation, schema versioning) validated during verify and post-verify phases.

---

**For complete specification text with all scenarios, acceptance criteria, and detailed requirements, see engram artifact id #1223.**

**Status**: ARCHIVED. All 10 requirements achieved. Final xref.json at `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json`.
