# Module Mapping — reflow-clean-177

**Schema**: v1.0 | **Generated**: 2026-05-09 | **Entries**: 535+ | **Coverage**: ≥95% of in-scope source

This directory contains the complete versioned mapping of the `reflow-clean-177` source tree.
The mapping exists in two co-located forms:

- `index.md` — human-readable flat table; one row per file; sortable in any Markdown viewer.
- `index.json` — machine-readable JSON array; queryable with `jq`; validated with `jq -e .`.

See `schema.md` for the full schema reference.

---

## Querying with `rg` (ripgrep)

All `rg` queries below run against the files in this directory. Run them from
`docs/mappings/reflow-clean-177/`.

### Find all REST endpoints

```bash
rg '"GET |"POST |"WS ' index.json
```

This returns every entry whose `backend.rest_endpoints` or `backend.box_methods`
contains an HTTP verb. To see just the endpoint strings:

```bash
rg '"(GET|POST|PUT|DELETE|WS) /' index.json -o | sort -u
```

### Find all Vue components in the floorplans domain

```bash
rg '"domain": "floorplans"' index.json -B5 -A5
```

Or search `index.md` for a quick visual list:

```bash
rg '^\| reflow-frontend/src/components/floorplans/' index.md
```

### Find all files with fidelity POOR or FAIR (from GAP-ANALYSIS)

```bash
rg '"fidelity": "(POOR|FAIR)"' index.json -B3
```

This shows which frontend Vue components have reconstruction quality concerns.
Useful when planning Tier 2 deep dives.

---

## Querying with `jq`

All `jq` queries run against `index.json`.

### Filter all entries in the floorplans domain

```bash
jq '[.entries[] | select(.domain=="floorplans")]' index.json
```

### Count entries by kind

```bash
jq '[.entries[] | .kind] | group_by(.) | map({kind: .[0], count: length})' index.json
```

Example output:
```json
[
  {"kind": "java-class", "count": 74},
  {"kind": "js-store", "count": 29},
  {"kind": "vue-component", "count": 366}
]
```

### List all persistent Vuex store modules

```bash
jq '[.entries[] | select(.kind=="js-store" and .frontend_js.persistent==true) | .path]' index.json
```

### Find all decompiled Java classes

```bash
jq '[.entries[] | select(.backend.decompiled==true) | {path, purpose}]' index.json
```

### Get all REST endpoints across all response handlers

```bash
jq '[.entries[] | select(.backend.rest_endpoints | length > 0) | {path, endpoints: .backend.rest_endpoints}]' index.json
```

### Find entries missing verified_at (not yet spot-checked)

```bash
jq '[.entries[] | select(.verified_at==null) | .path]' index.json | wc -l
```

---

## Extending the Schema for a New Module

Follow these steps to create a mapping for a new module (e.g., `analytics-module` or `mx60`).

### Step 1 — Create the directory

```bash
mkdir -p docs/mappings/<module-slug>/domains
```

### Step 2 — Choose your extension blocks

Read `schema.md` for existing extension blocks (`backend`, `frontend_vue`, `frontend_js`).
If your module has a different codebase type (e.g., Analytics DAG Java blocks), define
a new extension block following the prototype in `schema.md#ReusabilityAnalyticsMX60`.

Your extension block goes alongside core fields in each entry:

```json
{
  "id": "analytics-module/src/.../BAlgorithmBlock.java",
  "kind": "java-class",
  "domain": "algorithm-dag",
  "purpose": "Algorithm block for temperature delta computation.",
  ...core fields...,
  "analytics": {
    "algorithm_type": "BAlgorithmBlock",
    "dag_role": "transform",
    "aon_encoded": true
  }
}
```

### Step 3 — Create `schema.md` for the new module

Copy `docs/mappings/reflow-clean-177/schema.md` as a base. Set `schema_version: "1.0"` (no bump needed
unless you modify core fields). Document your new extension block in the **Extension Blocks** section.

### Step 4 — Create `index.json` and `index.md`

Use the same top-level envelope:

```json
{
  "schema_version": "1.0",
  "module": "<your-module-slug>",
  "source_repo": "<path>",
  "generated_at": "<ISO 8601>",
  "entries": [],
  "exclusions": []
}
```

Validate with:
```bash
jq -e . docs/mappings/<module-slug>/index.json
```

### Step 5 — Create domain docs

Follow the 5-section template from `domains/backend.md` or `domains/frontend.md`:
1. Overview
2. File inventory (table)
3. Architecture (ASCII diagram)
4. Cross-references
5. Known issues / Gotchas

### Step 6 — Register the mapping

Add a row to the master mappings index at `docs/mappings/README.md` (if it exists),
or create it with a summary table of all modules mapped.

---

## Files in This Directory

| File | Description |
|------|-------------|
| `index.md` | Human-readable flat table; 535+ entries; sortable |
| `index.json` | Machine-readable JSON; jq-queryable; validated |
| `schema.md` | Full schema reference (core fields + extension blocks) |
| `excluded.md` | Excluded paths with reasons |
| `README.md` | This file — usage guide |
| `domains/backend.md` | Deep dive: 74 Java rt + 3 Java ux classes (Batch B) |
| `domains/frontend.md` | Deep dive: app shell, state, 17 small domains (Batch C) |
| `domains/equipment.md` | Deep dive: 41 equipment components (Batch C) |
| `domains/floorplans.md` | Deep dive: 47 floorplan components (Batch C) |
| `domains/alarms.md` | Deep dive: 5 Java + 22 Vue alarms cross-stack (Batch C) |
| `domains/history.md` | Deep dive: 22 Vue histories (Batch C) |

Domain files under `domains/` are generated by Batch B and Batch C. They are
referenced by `index.json` entries via `source_doc` fields.

---

## Source Docs Used for Synthesis

| Document | Location | Used for |
|----------|----------|---------|
| `REFLOW-ARCHITECTURE-ANALYSIS.md` | `reflow-frontend/docs/` | Backend class map, LOC, slot table, REST endpoints, BOX methods |
| `GAP-ANALYSIS.md` | `reflow-frontend/docs/` | Per-domain fidelity ratings, store quality, missing features |
| `NIAGARA-INTEGRATION.md` | `reflow-frontend/docs/` | Build/deploy pipeline, servlet chain, SPA entry flow |

Source docs are READ-ONLY. The mapping synthesises from them; it does not modify them.
