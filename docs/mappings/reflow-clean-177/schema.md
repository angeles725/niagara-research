# Module Mapping Schema — v1.0

**schema_version**: 1.0
**Module**: reflow-clean-177 (reference implementation)
**Design date**: 2026-05-09
**Status**: LOCKED — additive extensions only. Core field changes require MAJOR bump (v2.0).

---

## Overview

The module-mapping schema uses a **core + extension** model. Core fields are universal
and apply identically to every file in every Niagara/Reflow module. Extension blocks
are additive namespaces that carry codebase-type-specific metadata. Adding a new
extension block for a different module (e.g., Analytics) does NOT require modifying
the core schema — it is appended alongside existing extensions.

---

## Top-Level JSON Envelope

Every `index.json` produced by this schema MUST have this envelope:

```json
{
  "schema_version": "1.0",
  "module": "<module-slug>",
  "source_repo": "<absolute-path-or-url>",
  "generated_at": "<ISO 8601 datetime>",
  "generator": "<tool or agent that produced the file>",
  "entries": [ ... ],
  "exclusions": [ ... ]
}
```

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| `schema_version` | string | yes | Must be `"1.0"` for this version |
| `module` | string | yes | Slug identifying the module (e.g., `reflow-clean-177`) |
| `source_repo` | string | yes | Absolute path or remote URL of the source being mapped |
| `generated_at` | string | yes | ISO 8601 datetime when the file was generated |
| `generator` | string | no | Identifies the tool or agent. May be omitted. |
| `entries` | array | yes | Array of entry objects (see Core Fields below) |
| `exclusions` | array | yes | Array of exclusion-reason objects (see Exclusions below) |

---

## Core Fields (mandatory for every entry)

These 10 fields MUST be present with a non-null, non-empty value on every entry.
The `dependencies` field MUST be present but MAY be an empty array `[]`.

| Field | Type | Mandatory | Description |
|-------|------|-----------|-------------|
| `id` | string | yes | Normalised path relative to repo root (e.g., `reflow-frontend/src/main.js`). Doubles as the stable lookup key. |
| `path` | string | yes | Same value as `id`. Duplicated for ergonomic jq queries (`select(.path=="...")`) |
| `kind` | enum | yes | File classification. See allowed values below. |
| `domain` | string | yes | Logical domain this file belongs to. See domain taxonomy below. |
| `purpose` | string (≤150 chars) | yes | One-sentence description of what this file does. |
| `dependencies` | string[] | yes (may be `[]`) | Key imports or uses. Java: FQN class names. JS/Vue: `@/...` module paths or npm package names. |
| `loc` | integer | yes | Approximate lines of code. Binary and bundled assets use `0`. |
| `status` | enum | yes | Source provenance. See allowed values below. |
| `source_doc` | object or null | no | Reference to the synthesis document and section used to derive this entry's `purpose` and extension fields. Object form: `{"file": "FILENAME.md", "section": "Section Heading"}`. Use `null` if derived from direct file read. |
| `verified_at` | string or null | no | ISO 8601 datetime when this entry was spot-checked against source. Required for entries in the REQ-7 spot-check sample. |

### `kind` Enum Values

| Value | Applies to |
|-------|-----------|
| `java-class` | Java source files (`.java`) — application classes, interfaces |
| `vue-component` | Vue single-file components (`.vue`) under `components/` |
| `vue-view` | Vue SFC files under `views/` — routed top-level views |
| `js-store` | Vuex store module files |
| `js-mixin` | Vue mixins |
| `js-plugin` | Vue plugins installed on `Vue.prototype` |
| `js-api` | API layer modules (REST, WebSocket, BajaScript, BOX) |
| `js-lib` | Shared utility/helper libraries |
| `js-router` | Vue Router configuration |
| `js-util` | Small utility JS files co-located with a component/feature |
| `js-entry` | Application entry point (`main.js`) |
| `config` | Build configs, module descriptors, XML configs, palette files |
| `resource-image` | Binary image assets (JPG, PNG, SVG) |
| `resource-icon` | Binary icon assets (PNG, SVG) |
| `resource-template` | Handlebars (`.hbs`) or HTML template files |
| `compiled-class` | Compiled `.class` files (status must be `compiled`) |
| `compiled-jar` | Compiled `.jar` artifacts |
| `compiled-bundle` | Webpack/Vite production bundle files |
| `module-descriptor` | Niagara `niagara-module.xml` or `module.palette` |

### `status` Enum Values

| Value | Meaning |
|-------|---------|
| `source` | Editable source file (Java, Vue, JS) |
| `compiled` | Compiled artifact (.class, .jar) — not editable |
| `bundle` | Webpack/Vite bundle output — not editable source |
| `resource` | Binary or static asset (image, font, icon) |
| `excluded` | Explicitly excluded from the mapping (appears only in `exclusions[]`) |

### Domain Taxonomy

Domains are logical groupings that map to architectural responsibilities, NOT directory names.
The same domain may span both backend Java and frontend Vue files.

**Backend domains**: `service-container`, `ord-scheme`, `http-rest`, `http-websocket`, `bajascript-box`, `history-backend`, `alarms-backend`, `sync-config`, `backups`, `util-backend`, `ux-widgets`

**Frontend domains**: `app-shell`, `state`, `dashboard`, `buildings`, `equipment`, `alarms-frontend`, `histories-frontend`, `floorplans`, `config-ui`, `navigation`, `cards`, `charts`, `points`, `profiles-rbac`, `schedules-frontend`, `pages`, `weather`, `maps`, `settings`, `websocket-ui`, `api-layer`, `mixins`, `plugins`, `lib`

**Cross-cutting / infrastructure**: `module-descriptor`, `build-config`, `image-library`, `icons`, `bundle-output`

---

## Extension Blocks

Extension blocks are optional namespaced objects added alongside core fields. They are
additive: adding a new extension block for a new module type does not change the core
schema and does not require a version bump.

### `backend` Extension Block

Applies to: entries with `kind` in `[java-class]` and optionally `compiled-class`.

```json
"backend": {
  "profile": "rt",
  "package": "com.niagaramods.nmodsreflow",
  "bcomponent_type": "BComponent",
  "slots": 26,
  "actions": ["refreshLicense", "reloadLicenseFile", "clearCache"],
  "rest_endpoints": [],
  "box_methods": [],
  "decompiled": false
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `profile` | enum (`rt` \| `ux`) | yes | Niagara module profile |
| `package` | string | yes | Java package (e.g., `com.niagaramods.nmodsreflow.history`) |
| `bcomponent_type` | string or null | yes | BComponent subtype string, or `null` if not a BComponent |
| `slots` | integer or null | yes | Number of declared BComponent slots. `null` if not applicable. |
| `actions` | string[] | yes (may be `[]`) | Declared `@NiagaraAction` method names |
| `rest_endpoints` | string[] | yes (may be `[]`) | HTTP endpoints handled — required non-empty for files under `http/responses/` and `BaseServlet` |
| `box_methods` | string[] | yes (may be `[]`) | BajaScript RPC method names — required non-empty for files under `commands/` |
| `decompiled` | boolean | yes | `true` for CFR-decompiled classes (BReflowScheme, BReflow, BReflowConfig, BReflowRedirect) |

### `frontend_vue` Extension Block

Applies to: entries with `kind` in `[vue-component, vue-view]`.

```json
"frontend_vue": {
  "component_dir": "equipment",
  "store_modules": ["equipment", "buildings"],
  "emits": ["device-changed", "equipment-updated"],
  "props": ["deviceId", "buildingId", "viewMode"],
  "mixins": ["equipmentMixin"],
  "plugins_used": ["$niagara", "$http"],
  "route_name": null,
  "fidelity": "GOOD"
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `component_dir` | string | yes | Subdirectory within `components/` or `views/` |
| `store_modules` | string[] | yes (may be `[]`) | Vuex modules this component accesses |
| `emits` | string[] | no | Key `$emit` event names (top 3-5) |
| `props` | string[] | no | Key Vue props (top 3-5) |
| `mixins` | string[] | no | Mixin names applied |
| `plugins_used` | string[] | no | `Vue.prototype` plugin keys used |
| `route_name` | string or null | no | Router route name. Required non-null for `vue-view` entries. |
| `fidelity` | enum | no | Reconstruction fidelity from GAP-ANALYSIS. Values: `EXCELLENT`, `GOOD`, `FAIR`, `POOR`. Absent if not assessed. |

### `frontend_js` Extension Block

Applies to: entries with `kind` in `[js-store, js-mixin, js-plugin, js-api, js-lib, js-router, js-util, js-entry]`.

```json
"frontend_js": {
  "module_type": "store",
  "persistent": true,
  "exports": ["state", "mutations", "getters", "actions"]
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `module_type` | enum | yes | `store`, `mixin`, `plugin`, `api`, `lib`, `router`, `util`, `entry` |
| `persistent` | boolean | yes if `module_type == "store"` | `true` if included in `config.json` serialization (14 of 29 modules are persistent) |
| `exports` | string[] | no | Top-level named exports |

---

## Reference Examples

### Example 1 — `BReflowService.java` (backend BComponent entry point)

```json
{
  "id": "nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java",
  "path": "nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/BReflowService.java",
  "kind": "java-class",
  "domain": "service-container",
  "purpose": "Root BComponent service; 26 slots; implements BIService + BIRestrictedComponent; spawns sync, websocket, and channel sub-services.",
  "dependencies": ["BReflowSyncService", "BReflowWebSocketAcceptor", "BReflowChannelService", "BaseServlet"],
  "loc": 468,
  "status": "source",
  "source_doc": {"file": "REFLOW-ARCHITECTURE-ANALYSIS.md", "section": "BReflowService"},
  "verified_at": "2026-05-09T12:00:00Z",
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsreflow",
    "bcomponent_type": "BComponent",
    "slots": 26,
    "actions": ["refreshLicense", "reloadLicenseFile", "clearCache", "clearHistoryCache", "ticketExpired"],
    "rest_endpoints": [],
    "box_methods": [],
    "decompiled": false
  }
}
```

### Example 2 — `Equipment.vue` (frontend Vue component)

```json
{
  "id": "reflow-frontend/src/components/equipment/DeviceCard.vue",
  "path": "reflow-frontend/src/components/equipment/DeviceCard.vue",
  "kind": "vue-component",
  "domain": "equipment",
  "purpose": "Card view for a single equipment device; subscribes to live point data via BajaScript; renders name, type, status, and point badges.",
  "dependencies": ["@/mixins/equipmentMixin", "@/mixins/subscriberMixin", "@/store/modules/equipment"],
  "loc": 180,
  "status": "source",
  "source_doc": {"file": "GAP-ANALYSIS.md", "section": "Equipment"},
  "verified_at": "2026-05-09T12:00:00Z",
  "frontend_vue": {
    "component_dir": "equipment",
    "store_modules": ["equipment", "buildings"],
    "emits": ["device-changed"],
    "props": ["deviceId", "buildingId"],
    "mixins": ["equipmentMixin", "subscriberMixin"],
    "plugins_used": ["$niagara", "$http"],
    "route_name": null,
    "fidelity": "FAIR"
  }
}
```

### Example 3 — `$niagara` plugin

```json
{
  "id": "reflow-frontend/src/plugins/niagara.js",
  "path": "reflow-frontend/src/plugins/niagara.js",
  "kind": "js-plugin",
  "domain": "plugins",
  "purpose": "Vue plugin installing $niagara on Vue.prototype; provides alarm subscriber, BQL query, history RPC, and Niagara subscriber lifecycle helpers.",
  "dependencies": ["@/api/bajascript", "@/api/box"],
  "loc": 90,
  "status": "source",
  "source_doc": {"file": "REFLOW-ARCHITECTURE-ANALYSIS.md", "section": "Frontend"},
  "verified_at": null,
  "frontend_js": {
    "module_type": "plugin",
    "persistent": false,
    "exports": ["install"]
  }
}
```

### Example 4 — `profiles.js` Vuex store module (persistent)

```json
{
  "id": "reflow-frontend/src/store/modules/profiles.js",
  "path": "reflow-frontend/src/store/modules/profiles.js",
  "kind": "js-store",
  "domain": "profiles-rbac",
  "purpose": "Vuex store managing RBAC user profiles; implements 7-path authorizeLink engine, isPathAvailable, getAllRoutes (77L), getRouteTreeData (189L), and restrictNewContent dual-mode.",
  "dependencies": ["@/api/rest"],
  "loc": 420,
  "status": "source",
  "source_doc": {"file": "GAP-ANALYSIS.md", "section": "StoreModuleFidelity"},
  "verified_at": null,
  "frontend_js": {
    "module_type": "store",
    "persistent": true,
    "exports": ["state", "getters", "mutations", "actions"]
  }
}
```

### Example 5 — `BaseServlet.java` (HTTP front controller)

```json
{
  "id": "nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java",
  "path": "nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/http/BaseServlet.java",
  "kind": "java-class",
  "domain": "http-rest",
  "purpose": "HTTP front controller; routes 24 REST endpoints by URL path to dedicated response handler classes; handles auth, CSRF, and error wrapping.",
  "dependencies": ["ConfigResponse", "HistoryDataResponse", "AlarmQueryResponse", "CsrfGuard", "Query"],
  "loc": 300,
  "status": "source",
  "source_doc": {"file": "REFLOW-ARCHITECTURE-ANALYSIS.md", "section": "RESTAPIBaseServlet"},
  "verified_at": "2026-05-09T12:00:00Z",
  "backend": {
    "profile": "rt",
    "package": "com.niagaramods.nmodsreflow.http",
    "bcomponent_type": null,
    "slots": null,
    "actions": [],
    "rest_endpoints": [
      "GET /config", "GET /demos", "GET /station/equipment-notes",
      "GET /station/backups", "GET /station/histories", "GET /station/histories/:name",
      "GET /station/history-data", "GET /station/history-groups",
      "GET /station/schedules", "GET /station/images", "GET /station/files",
      "GET /station/image-library", "GET /station/alarms/csv", "GET /weather-map",
      "POST /config_update", "POST /config_delta", "POST /station/equipment-notes-update",
      "POST /station/alarms/query", "WS /ws"
    ],
    "box_methods": [],
    "decompiled": false
  }
}
```

---

## Versioning Rules (Forward-Compatibility)

| Change type | Version impact | Action required |
|-------------|----------------|-----------------|
| Add a new optional field to core | No change | Document in this file; note as `optional` |
| Add a new extension block | No change | Document new block here; existing consumers ignore unknown blocks |
| Add a new `kind` or `status` enum value | MINOR bump → v1.1 | Update this doc; parsers must tolerate unknown enums |
| Rename or remove a core field | MAJOR bump → v2.0 | Requires migration plan; old consumers will break |
| Change a core field type | MAJOR bump → v2.0 | Requires migration plan |

---

## Reusability: Analytics / MX60 Extension Block (prototype)

This section demonstrates how the Analytics module would reuse the same `schema_version: 1.0`
core fields by adding an `analytics` extension block — without touching any core definitions.

### `analytics` Extension Block (prototype — not yet implemented)

Applies to: entries from `analytics-module` with algorithm block files.

```json
"analytics": {
  "algorithm_type": "BAlgorithmBlock",
  "dag_role": "transform",
  "aon_encoded": true,
  "execution_order": 4,
  "verdict": "NO",
  "verdict_session": "bloque67"
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `algorithm_type` | string | yes | Niagara Analytics algorithm class name |
| `dag_role` | enum | yes | Role in the algorithm DAG: `source`, `transform`, `sink` |
| `aon_encoded` | boolean | yes | Whether the block uses AON (Algorithm Object Notation) encoding |
| `execution_order` | integer or null | no | Topological execution order in the DAG |
| `verdict` | enum (`SI` \| `NO` \| `PENDING`) | no | MX60 migration verdict from bloque67 analysis |
| `verdict_session` | string | no | Session ID where the verdict was recorded |

### How to add this block for a new module

1. Copy the `analytics` block prototype above.
2. Define it in a NEW schema doc: `docs/mappings/analytics-module/schema.md`.
3. Reference `schema_version: "1.0"` — no modification to this file.
4. Add `analytics` block entries to your `index.json` alongside core fields.
5. Existing Reflow tooling ignores unknown extension blocks gracefully.

---

## Excluded Paths Reference

See `excluded.md` in the same directory for the authoritative list of excluded paths
with reasons. The `exclusions[]` array in `index.json` mirrors that list in machine-readable form.

Each exclusion entry in `index.json` has the shape:

```json
{
  "path": "reflow-frontend/node_modules/",
  "reason": "Third-party npm dependencies; not project source."
}
```
