# Module Mapping Schema — mx60-chihuahua v1.0 (extension of reflow v1.0)

**schema_version**: 1.0
**extensions**: ["backend", "frontend_iife"]
**Module**: mx60-chihuahua
**Design date**: 2026-05-09
**Status**: LOCKED — additive extension of reflow-clean-177 schema v1.0. Core field changes require MAJOR bump (v2.0).

---

## Overview

The mx60-chihuahua mapping inherits the reflow-clean-177 v1.0 schema in full and adds two
extension blocks: `backend` (inherited verbatim from reflow) and `frontend_iife` (new, specific
to MX60's IIFE-based JavaScript architecture).

MX60 has zero `.vue` files. The `frontend_vue` extension block from reflow is **explicitly
forbidden** — the validator rejects any entry that declares `frontend_vue`. The `frontend_js`
extension block is **mutually exclusive** with any `iife-*` kind: an entry may not carry both.

---

## Top-Level JSON Envelope

```json
{
  "schema_version": "1.0",
  "module": "mx60-chihuahua",
  "extensions": ["backend", "frontend_iife"],
  "source_repo": "/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/",
  "generated_at": "<ISO 8601 datetime>",
  "generator": "sdd-apply mapping-mx60 v1",
  "entries": [ ... ],
  "exclusions": [ ... ]
}
```

The `extensions` field (new in mx60, absent in reflow) declares which extension blocks this
mapping uses. Consumers that don't know `frontend_iife` should ignore it gracefully.

---

## Core Fields (inherited from reflow v1.0 — mandatory for every entry)

These 10 fields are inherited unchanged from reflow-clean-177 schema v1.0.

| Field | Type | Mandatory | Description |
|-------|------|-----------|-------------|
| `id` | string | yes | Normalised path relative to repo root. Doubles as stable lookup key. |
| `path` | string | yes | Same value as `id`. Duplicated for ergonomic jq queries. |
| `kind` | enum | yes | File classification. See combined enum below (inherited + MX60 new). |
| `domain` | string | yes | Logical domain this file belongs to. |
| `purpose` | string (≤150 chars) | yes | One-sentence description of what this file does. Hard cap: 150 chars. |
| `loc` | integer | yes | Approximate lines of code. Binary assets use `0`. |
| `status` | enum | yes | Source provenance. See allowed values below. |
| `source_doc` | object or null | yes (key must be present) | Reference to synthesis doc. Object: `{"file": "...", "section": "..."}`. Null if direct file read. |
| `verified_at` | string or null | yes (key must be present) | ISO 8601 datetime of spot-check. Required non-null for ≥40 spot-checked entries. |
| `dependencies` | string[] | yes (may be `[]`) | Key imports or uses. Java: FQN class names. JS: globals read (MX60.X). |

---

## `kind` Enum Values

### Inherited from reflow v1.0

| Value | Applies to |
|-------|-----------|
| `java-class` | Java source files (`.java`) — application classes, interfaces |
| `config` | Build configs, module descriptors, XML configs, palette files |
| `resource-image` | Binary image assets (JPG, PNG, SVG) |
| `module-descriptor` | Niagara `niagara-module.xml`, `module.palette`, `module.lexicon`, `module-permissions.xml` |
| `compiled-bundle` | Third-party precompiled JS bundles (Chart.js, Three.js) |
| `resource` | Binary or static assets (fonts, CSS, other static files) |

Note: reflow kinds `vue-component`, `vue-view`, `js-store`, `js-mixin`, `js-plugin`, `js-api`,
`js-lib`, `js-router`, `js-util`, `js-entry`, `compiled-class`, `compiled-jar`, `resource-icon`,
`resource-template` are inherited as valid enum values but are not expected to appear in
mx60-chihuahua entries (zero Vue files, zero compiled-class entries).

### MX60-Specific IIFE Kind Values (5 new values)

These 5 values are unique to the MX60 IIFE JavaScript architecture. They replace the reflow
Vue-specific kinds and must NOT be confused with `js-lib`, `js-store`, etc.

| Value | Applies to | Description |
|-------|-----------|-------------|
| `iife-app` | Page/section orchestrators | Files that own a major UI surface area or route. Examples: `DashboardApp.js`, `AlarmsManager.js`, `AlarmsPage.js`, `EquipmentDetail.js`, `UpDetail.js`, `CarcamoDetail.js`, `DataloggerDetail.js`, `ScheduleView.js`, `HomeMap.js`, `Router.js`, `ConfigManager.js`, `SharedEnv.js`, `Configuracion.js`. |
| `iife-store` | In-memory state stores | Files ending in `Store.js`. Examples: `EquipmentSnapshotStore.js`, `UpThresholdStore.js`, `ModoOverrideStore.js`, `OutputOverrideStore.js`, `CarcamoThresholdStore.js`, `DataloggerThresholdStore.js`, `AlarmLatchStore.js`. |
| `iife-lib` | Reusable cross-cutting library modules | Files under `rc/js/lib/` OR reusable named modules: `SubscriptionPool.js`, `WritePoint.js`, `Toast.js`, `Confirm.js`, `StatusResolver.js`, `AlarmCards.js`, `AlarmDetailsTable.js`, `AlarmDetailPage.js`, `AlarmModalActions.js`, `AlarmNotesModal.js`, `EquipmentCard.js`, `EquipmentData.js`, `LiveHistoryBuffer.js`, `TimeRangePicker.js`, `BulkActionBar.js`, `ParticleAnimation.js`. |
| `iife-util` | Small utility modules | Files under `rc/js/util/`: `CsvExport.js`, `Dropdown.js`, `Popover.js`, `RelativeTime.js`. |
| `iife-entry` | Bootstrap entry point | The HTML file or inline `<script>` block that initialises the application (bootstraps `MX60.DashboardApp.init()`). |

### 9-Rule Kind Decision Tree (apply in order — first match wins)

```
1. Is the file a Java source (.java)?                              → java-class
2. Is the file niagara-module.xml, module.palette,
   module.lexicon, or module-permissions.xml?                     → module-descriptor
3. Is the file a Gradle build script (.gradle.kts)?               → config
4. Is the file index.html (SPA entry point with bootstrap IIFE)?  → iife-entry
5. Is the filename a direct bootstrap entry for the IIFE stack?   → iife-entry
6. Does the filename end in Store.js?                             → iife-store
7. Is the file under rc/js/util/?                                 → iife-util
8. Is the file under rc/js/lib/ OR on the iife-lib name list?    → iife-lib
9. Is the file an orchestrator / major UI section (iife-app list)?→ iife-app
   Fallback: check if it manages a sub-view or alarms/schedule
   section — if yes, iife-app; if no, iife-lib.
10. Is the file a binary image (JPG, PNG)?                         → resource-image
11. Is the file a binary font (.woff2)?                            → resource
12. Is the file CSS (.css)?                                        → resource
13. Is the file a precompiled third-party JS bundle?               → compiled-bundle
```

---

## `status` Enum Values (inherited from reflow v1.0)

| Value | Meaning |
|-------|---------|
| `source` | Editable source file (Java, JS, HTML, CSS) |
| `compiled` | Compiled artifact — not editable |
| `bundle` | Production bundle output — not editable source |
| `resource` | Binary or static asset (image, font) |
| `excluded` | Explicitly excluded (appears only in `exclusions[]`) |

---

## Extension Blocks

### `backend` Extension Block (inherited from reflow v1.0)

Mandatory for every entry with `kind: "java-class"`. Applies unchanged from reflow schema.

```json
"backend": {
  "profile": "rt",
  "package": "com.angeles.chihuahua.components",
  "bcomponent_type": "BComponent",
  "slots": 37,
  "actions": [],
  "rest_endpoints": [],
  "box_methods": [],
  "decompiled": false
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `profile` | enum (`rt` \| `ux`) | yes | Niagara module profile |
| `package` | string | yes | Java package FQN |
| `bcomponent_type` | string or null | yes | BComponent subtype, or `null` if not a BComponent |
| `slots` | integer or null | yes | Declared BComponent slots count. `null` if not a BComponent. |
| `actions` | string[] | yes (may be `[]`) | Declared `@NiagaraAction` method names |
| `rest_endpoints` | string[] | yes (may be `[]`) | HTTP endpoints handled. Non-empty for BChiServlet. |
| `box_methods` | string[] | yes (may be `[]`) | BajaScript RPC method names |
| `decompiled` | boolean | yes | Always `false` in mx60-chihuahua (all source available) |

### `frontend_iife` Extension Block (NEW — MX60-specific)

Mandatory for every entry with `kind` in `["iife-app", "iife-store", "iife-lib", "iife-util", "iife-entry"]`.
Must NOT appear on `java-class`, `config`, `resource-*`, `module-descriptor`, or `compiled-bundle` entries.

```json
"frontend_iife": {
  "namespace": "MX60",
  "globals_written": ["MX60.SubscriptionPool"],
  "globals_read": ["MX60.SharedEnv", "MX60.ConfigManager"],
  "iife_pattern": "iife-window",
  "load_order_hint": 5,
  "subscriber_role": "producer"
}
```

| Field | Type | Mandatory in block | Notes |
|-------|------|--------------------|-------|
| `namespace` | string | yes | Always `"MX60"` for mx60-chihuahua — fixed constant |
| `globals_written` | string[] | yes (may be `[]`) | Global symbols this file defines (e.g., `"MX60.SubscriptionPool"`) |
| `globals_read` | string[] | yes (may be `[]`) | Global symbols this file reads/depends on from other IIFE files |
| `iife_pattern` | enum | yes | See `iife_pattern` enum below |
| `load_order_hint` | integer or null | yes | 1-based load position from `index.html` `<script>` tag order. `null` if loaded dynamically or as ES module. |
| `subscriber_role` | enum | yes | See `subscriber_role` enum below |

#### `iife_pattern` Enum (4 values — LOCKED)

| Value | Meaning |
|-------|---------|
| `iife-window` | Standard `(function(win) { ... })(window)` or `(function() { window.MX60.X = ... })()` — assigns to `window.MX60` |
| `iife-self` | `(function(self) { ... })(this)` pattern |
| `iife-named` | Named IIFE with explicit namespace parameter: `var MX60 = MX60 \|\| {}; (function(ns) { ... })(MX60)` |
| `iife-other` | Any other IIFE structure not fitting the above 3, or ES module (UpDetail.js, CarcamoDetail.js use `type="module"`) |

#### `subscriber_role` Enum (3 values — LOCKED)

| Value | Meaning |
|-------|---------|
| `producer` | File creates or manages BajaScript subscriptions (calls `baja.subscribe`, manages subscriber objects, owns teardown) |
| `consumer` | File reads from subscriptions established elsewhere; triggers polling or uses subscription data |
| `none` | File has no BajaScript subscriber involvement (pure UI, pure state, pure utility) |

---

## Explicit Prohibitions

1. **`frontend_vue` is FORBIDDEN** in mx60-chihuahua. Zero `.vue` files exist. The validator rejects
   any entry with a `frontend_vue` block. `rg '"frontend_vue"' index.json` must return 0 matches.

2. **`frontend_js` is mutually exclusive with `iife-*` kinds.** An entry cannot carry both a
   `frontend_js` block and a kind in `["iife-app","iife-store","iife-lib","iife-util","iife-entry"]`.
   The validator enforces this. `js-lib`, `js-store`, etc. may appear in the enum technically but
   are not expected and must not coexist with `frontend_iife`.

3. **Prohibited field names in entries.** These field names must NOT appear in any entry object:
   `from`, `caller`, `file`, `callers`, `used_by`, `edges`, `source_path`, `name`.
   Use `id`, `path`, `source_doc`, `dependencies` respectively.

---

## Exclusions Array Format

Each exclusion entry in `index.json` uses the same format as reflow:

```json
{
  "path": "chihuahua-rt/srcTest/",
  "reason": "Test files excluded per mapping convention."
}
```

Mandatory exclusions for mx60-chihuahua:
- 16 individual test Java files across `chihuahua-rt/srcTest/` and `chihuahua-ux/srcTest/`
- `.idea/` directory (IDE metadata)
- `.gradle/` directory (Gradle cache)
- `build/` directories (compiled output)
- Outer Gradle wrapper files (if present at repo root)

---

## Versioning Rules (inherited from reflow v1.0)

| Change type | Version impact | Action required |
|-------------|----------------|-----------------|
| Add a new optional field to core | No change | Document here; note as `optional` |
| Add a new extension block | No change | Document new block; existing consumers ignore unknown blocks |
| Add a new `kind` or `status` enum value | MINOR bump → v1.1 | Update this doc; parsers must tolerate unknown enums |
| Rename or remove a core field | MAJOR bump → v2.0 | Migration plan required |
| Change `frontend_iife` field type | MINOR bump → v1.1 | Update this doc |

---

## Acceptance Criteria for This Schema File

- `rg 'iife-app|iife-store|iife-lib|iife-util|iife-entry' schema.md` returns 5 distinct kind values
- `rg 'frontend_iife' schema.md` returns ≥6 lines
- `rg 'frontend_vue.*forbidden|forbidden.*frontend_vue' schema.md -i` returns ≥1 match
- `rg '"iife-window"|"iife-self"|"iife-named"|"iife-other"' schema.md` returns 4 distinct values
- `rg '"producer"|"consumer"|"none"' schema.md` returns 3 distinct subscriber_role values
