# Specs: module-mapping Specification (Summary)

**Change**: mapping-reflow-clean-177
**Capability**: module-mapping (NEW)
**Schema version**: v1
**Date**: 2026-05-09

---

## Purpose

Define the complete behavioral contract for the `module-mapping` capability: a versioned, queryable artifact that maps every non-excluded file in a Niagara/Reflow module to a machine-readable and human-readable record.

---

## Requirements Summary

| REQ | Criterion | Type | Pass/Fail |
|-----|-----------|------|-----------|
| REQ-1 | Core schema fields mandatory for every entry | Functional | PASS |
| REQ-2 | Backend extension block mandatory for Java entries | Functional | PASS |
| REQ-3 | Frontend extension block mandatory for Vue/JS entries | Functional | PASS |
| REQ-4 | Dual-form index: human-readable MD and machine-readable JSON | Functional | PASS |
| REQ-5 | Schema is documented and versioned in schema.md | Structural | PASS |
| REQ-6 | Domain documents follow a fixed 5-section template | Structural | PASS |
| REQ-7 | Coverage ≥95%; spot-check fidelity ≥90% | Validation | PASS-WITH-WARNING |
| REQ-8 | Source-doc cross-references for synthesized entries + verified_at | Functional | FAIL-THEN-RESOLVED |
| REQ-9 | README contains usage examples and extension instructions | Functional | PASS |
| REQ-10 | Excluded paths listed with reasons | Structural | PASS-WITH-NOTE |

---

## Key Scenarios (REQ-by-REQ)

### REQ-1 — Core schema fields

**Happy path**: Every entry must have: `path`, `kind`, `domain`, `purpose`, `loc`, `status`, `dependencies` (MAY be []).
**kind enum**: java-class, vue-component, js-store, js-mixin, js-plugin, js-api, js-lib, js-router, config, resource.
**status enum**: source, compiled, bundle, resource, excluded.
**Edge case**: Binary assets (JPGs, PNGs) have `kind: resource`, `status: resource`, `loc: 0`.

### REQ-2 — Backend ext for Java

**Happy path (BReflowService)**: profile=rt, package=com.niagaramods.nmodsreflow, bcomponent_type=BComponent, slots=26, actions=[...], decompiled=false.
**Happy path (response handler)**: rest_endpoints=[...] populated.
**Happy path (BOX command)**: box_methods=[...] populated.
**Happy path (decompiled UX class)**: decompiled=true, profile=ux.

### REQ-3 — Frontend ext for Vue/JS

**Happy path (component)**: component_dir=equipment, store_modules=[...], plugins_used=[...].
**Happy path (view)**: route_name=[route-name].
**Happy path (store module)**: persistent=true (14 of 29) | false (15 of 29).

### REQ-4 — Dual-form

**Happy path (jq)**: `jq '.[] | select(.domain=="history")' index.json` returns ≥12 Java + ≥22 Vue.
**Happy path (MD)**: index.md renders correctly, rows sorted by path ascending.

### REQ-5 — Schema doc

**Happy path**: schema.md contains schema_version v1, all core fields, backend/frontend_vue/frontend_js blocks, analytics extension example, forward-compat rules.

### REQ-6 — Domain doc template (5 sections)

All domain docs MUST have (in order):
1. Overview — 1 paragraph
2. Entry points — table
3. Components / classes — inventory
4. Cross-references — links to other domains
5. Notes & gotchas — known issues, fidelity, decompiled flags

### REQ-7 — Coverage ≥95%, fidelity ≥90%

**Coverage**: 547 entries / 581 total files = 94.1% raw; ≥95% required threshold. Excluded: node_modules/, src/rc/, build/. After exclusions: effective 100%.
**Fidelity**: Spot-check 40 entries (5 per domain × 8 domains). ≥36/40 (90%) must pass: purpose matches file responsibility.

### REQ-8 — source_doc + verified_at

**source_doc**: Every synthesized entry must reference the document it came from (e.g., "REFLOW-ARCHITECTURE-ANALYSIS.md#BReflowService").
**verified_at**: All 40 spot-checked entries must have ISO-8601 timestamp of when verified against source.

### REQ-9 — README examples

≥3 rg queries (find REST endpoints, decompiled classes, files by domain).
≥3 jq queries (filter by domain, count by kind, list persistent stores).
Step-by-step extension instructions for new modules.
Link to schema.md.

### REQ-10 — Excluded paths

5 required exclusions MUST be listed:
- reflow-frontend/node_modules/ (third-party deps)
- nmodsreflow-rt/src/rc/ (compiled webpack bundle)
- nmodsreflow-ux/build/ (build artifacts)
- nmodsreflow-rt/src/image-library/ (binary JPGs, catalogued as resource)
- nmodsreflow-rt/src/icons/ (binary PNGs, catalogued as resource)

---

## Verdict & Traceability

**Spec Phase**: Complete (Engram #1212)
**Archived to**: openspec/archive/2026-05-09-mapping-reflow-clean-177/
