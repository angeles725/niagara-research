# Specs: module-mapping (Full Reference)

**Change**: mapping-reflow-clean-177
**Capability**: module-mapping (NEW)
**Schema version**: v1
**Date**: 2026-05-09

---

## Requirement Details

See `specs-module-mapping-spec.md` for the structured breakdown.

This file serves as a reference link to the full specification in Engram #1212.

---

## Key Scenarios by REQ

### REQ-1 — Core schema fields mandatory for every entry

```
Mandatory: path, kind, domain, purpose, loc, status
Optional: dependencies (default []), source_doc, verified_at
```

**kind enum**: java-class, vue-component, js-store, js-mixin, js-plugin, js-api, js-lib, js-router, config, resource
**status enum**: source, compiled, bundle, resource, excluded

### REQ-2 — Backend extension block mandatory for Java entries

For `kind: java-class`, MUST include:
- profile (rt | ux)
- package (string)
- bcomponent_type (string or null)
- slots (int or null)
- actions (string array, MAY be empty)
- decompiled (boolean)
- rest_endpoints (if under http/responses/)
- box_methods (if under commands/)

### REQ-3 — Frontend extension block mandatory for Vue/JS

For `kind: vue-component`:
- component_dir (string)
- store_modules (array)
- plugins_used (array)
- route_name (if view)

For `kind: js-store`:
- persistent (true | false)

### REQ-4 — Dual-form index: human-readable MD and machine-readable JSON

- index.md: flat table, sorted ascending by path, renders correctly in Markdown
- index.json: valid JSON, parseable by jq, core fields complete, ≥530 entries

### REQ-5 — Schema documented and versioned

schema.md MUST contain:
- schema_version: v1
- all core fields with name, type, allowed enums
- backend, frontend_vue, frontend_js extension blocks
- analytics extension example (forward-compat proof)
- forward-compat rules

### REQ-6 — Domain documents follow fixed 5-section template

EVERY domain doc MUST have (in order):
1. Overview (1 paragraph)
2. Entry points (table)
3. Components / classes (inventory)
4. Cross-references (links to other domains)
5. Notes & gotchas (known issues)

### REQ-7 — Coverage ≥95%; spot-check fidelity ≥90%

- Coverage: ≥95% of in-scope files (exclude node_modules/, src/rc/, build/)
- Fidelity: ≥40 entries (5 × 8 domains) spot-checked; ≥36/40 pass; purpose matches file responsibility

### REQ-8 — Source-doc cross-references for synthesized entries

- Every entry synthesized from existing docs MUST include `source_doc: {file, section}`
- Every spot-checked entry MUST include `verified_at: ISO-8601 datetime`

### REQ-9 — README contains usage examples

- ≥3 rg queries (find REST endpoints, decompiled classes, files by domain)
- ≥3 jq queries (filter by domain, count by kind, list persistent stores)
- Step-by-step extension instructions
- Link to schema.md

### REQ-10 — Excluded paths listed with reasons

5 REQUIRED exclusions:
- reflow-frontend/node_modules/ (third-party deps)
- nmodsreflow-rt/src/rc/ (compiled webpack bundle)
- nmodsreflow-ux/build/ (build artifacts)
- nmodsreflow-rt/src/image-library/ (binary JPGs, catalogued as resource)
- nmodsreflow-rt/src/icons/ (binary PNGs, catalogued as resource)

---

## Full Specification

**See Engram #1212** for the complete specification document with all scenarios, error paths, and edge cases.

**Archived to**: openspec/archive/2026-05-09-mapping-reflow-clean-177/
