# MX60 Chihuahua — Module Mapping

**Module**: `mx60-chihuahua`  
**Schema version**: 1.0  
**Location**: `docs/mappings/mx60-chihuahua/`  
**Primary artifact**: `index.json` (100 entries, 20 exclusions)  

---

## Module Overview

`chihuahua` is a Niagara 4 BMS plugin for Honeywell MX60 HVAC management.
The module consists of two Gradle sub-projects:

| Sub-project | Profile | Language | Role |
|-------------|---------|----------|------|
| `chihuahua-rt` | `rt` | Java / BajaScript | Real-time BComponents (BChiUp, BChiCarcamo, BChiDatalogger, monitors, service) |
| `chihuahua-ux` | `ux` | Java + IIFE JS | HTTP servlet (BChiServlet), helper classes, frontend IIFE modules |

### Architecture characteristics

- **Zero static imports on the frontend** — all 40+ JS files communicate via
  the global `MX60` namespace using IIFE patterns. There are no bundler, no
  `import`/`require` statements (except 3 ES module files loaded via
  `<script type="importmap">`).
- **IIFE-window dominant** — `(function(window) { ... })(window)` is the
  primary pattern. Some files use `(function() { ... })()` (iife-self) or
  named-param variants (iife-named). ES module files (`UpDetail.js`,
  `CarcamoDetail.js`, `SharedEnv.js`) load via importmap.
- **3 equipment types**: UP (Unidad Paquete, 37-slot HVAC), Cárcamo (sump pit,
  8-slot), Datalogger (pressure sensor, 9-slot).
- **BajaScript subscription primary, REST polling fallback** — `SubscriptionPool`
  manages live updates; REST is a fallback and initial-load path.

---

## Schema decisions

See `schema.md` for the full schema definition. Key extensions vs reflow-clean-177:

| Decision | MX60 value | Reflow value |
|----------|-----------|--------------|
| New `kind` values | 5 IIFE kinds (iife-app, iife-store, iife-lib, iife-util, iife-entry) | None (js-lib, js-store, vue-component, etc.) |
| `frontend_iife` block | Required for all iife-* entries | Not present |
| `frontend_vue` block | FORBIDDEN (zero .vue files) | Present for Vue components |
| `iife_pattern` enum | iife-window / iife-self / iife-named / iife-other | N/A |
| `subscriber_role` enum | producer / consumer / none | N/A |

### Kind family mapping (MX60 → reflow analog)

| MX60 kind | Reflow analog | Notes |
|-----------|---------------|-------|
| `iife-app` | `js-store` / `vue-component` | Page orchestrators, registers with DashboardApp |
| `iife-store` | `js-store` | In-memory state stores, no BajaScript subscription |
| `iife-lib` | `js-lib` | Reusable utilities and BajaScript integration |
| `iife-util` | `js-lib` | Pure utility, no MX60-specific business logic |
| `iife-entry` | `js-lib` | Bootstrap entry (index.html script block) |
| `java-class` | `java-class` | Same — BComponent or plain class |
| `module-descriptor` | `module-descriptor` | Same |
| `config` | `config` | Same |
| `resource` | `resource` | Same |
| `resource-image` | `resource-image` | Same |

---

## Query examples

All examples run from repo root. Requires `jq >= 1.6`.

### 1. List all IIFE store entries

```bash
jq '[.entries[] | select(.kind == "iife-store") | {id, domain, purpose}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 2. Find all entries where SubscriptionPool is a dependency (globals_read)

```bash
jq '[.entries[]
  | select(.frontend_iife != null)
  | select(.frontend_iife.globals_read | map(select(test("SubscriptionPool"))) | length > 0)
  | {id, globals_read: .frontend_iife.globals_read}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 3. Filter by subscriber_role = producer (BajaScript data producers)

```bash
jq '[.entries[]
  | select(.frontend_iife != null)
  | select(.frontend_iife.subscriber_role == "producer")
  | {id, domain}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 4. List all entries in the baja-integration domain

```bash
jq '[.entries[] | select(.domain == "baja-integration") | {id, kind, purpose}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 5. Show kind + domain for all iife-app entries

```bash
jq '[.entries[] | select(.kind == "iife-app") | {id: .id, domain: .domain, load_order_hint: .frontend_iife.load_order_hint}]
  | sort_by(.load_order_hint)' \
  docs/mappings/mx60-chihuahua/index.json
```

### 6. Find entries with inferred-from-mapping annotations

```bash
jq '[.entries[] | select(.purpose | test("inferred from mapping")) | {id, purpose}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 7. List all java-class entries with REST endpoints (backend http-rest domain)

```bash
jq '[.entries[]
  | select(.kind == "java-class")
  | select(.backend.rest_endpoints | length > 0)
  | {id, endpoint_count: (.backend.rest_endpoints | length)}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 8. Count entries by iife_pattern variant

```bash
jq '[.entries[]
  | select(.frontend_iife != null)
  | .frontend_iife.iife_pattern]
  | group_by(.) | map({pattern: .[0], count: length})' \
  docs/mappings/mx60-chihuahua/index.json
```

### 9. Find all iife-store entries and their globals_written

```bash
jq '[.entries[]
  | select(.kind == "iife-store")
  | {id, globals_written: .frontend_iife.globals_written}]' \
  docs/mappings/mx60-chihuahua/index.json
```

### 10. Get domain distribution summary

```bash
jq '[.entries[].domain] | group_by(.) | map({domain: .[0], count: length}) | sort_by(-.count)' \
  docs/mappings/mx60-chihuahua/index.json
```

### Note on xref.json

Cross-reference queries (e.g., "all callers of SubscriptionPool") require
`xref.json`, which will be generated in PR-3 (`scripts/build-xref.sh`).
After PR-3 merges, use:

```bash
# All dependencies of UpDetail.js (outgoing edges)
jq '[.[] | select(.from_id | test("UpDetail"))]' \
  docs/mappings/mx60-chihuahua/xref.json

# All modules that read MX60.AlarmsManager (incoming edges)
jq '[.[] | select(.to_id | test("AlarmsManager"))]' \
  docs/mappings/mx60-chihuahua/xref.json
```

---

## File structure

```
docs/mappings/mx60-chihuahua/
├── README.md               ← this file
├── schema.md               ← schema definition (kind enum, extension blocks)
├── index.json              ← merged machine-readable catalog (100 entries)
├── index.md                ← human-readable Markdown table
├── excluded.md             ← excluded paths with reasons
├── delta.json              ← delta vs reflow-clean-177 (PR-3)
├── delta-vs-reflow.md      ← human-readable delta (PR-3)
├── xref.json               ← cross-reference edges (PR-3)
├── xref.md                 ← human-readable xref (PR-3)
├── _validation.md          ← validation report (PR-3)
├── domains/                ← 17 domain deep-dives (PR-3)
│   ├── service-container.md
│   ├── equipment-backend.md
│   ├── ... (15 more)
│   └── threshold-stores.md
├── shards/                 ← per-shard source JSONs (inputs to merge)
│   ├── s1-backend-rt.json
│   ├── s2-backend-ux.json
│   ├── s3-frontend-core.json
│   ├── s4-frontend-equipment.json
│   ├── s5-frontend-alarms-schedules.json
│   └── s6-resources-config.json
└── scripts/
    ├── merge-shards.sh     ← merges s1..s6 → index.json
    ├── build-index-md.sh   ← generates index.md from index.json
    ├── build-delta.sh      ← generates delta.json (PR-3)
    ├── build-xref.sh       ← generates xref.json (PR-3)
    └── validate-shard.jq   ← per-shard schema validator (PR-3)
```
