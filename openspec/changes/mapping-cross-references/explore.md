# Exploration: mapping-cross-references

**Phase**: sdd-explore
**Date**: 2026-05-09
**Source (READ-ONLY)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Engram topic**: `sdd/mapping-cross-references/explore` (id #1221)
**Sibling artifact**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/index.json` (547 entries — the symbol catalog)

---

## Goal

Survey the cross-reference space for Reflow-Clean-177: for every symbol catalogued in the sibling mapping, determine WHO uses it — producing a complementary xref artifact indexed by `symbol → used_at[]`.

---

## Current State

The sibling change `mapping-reflow-clean-177` produced:
- **Index**: `docs/mappings/reflow-clean-177/index.json` — 547 entries
  - 77 Java classes (backend)
  - 378 Vue components
  - 30 Vuex store modules
  - 18 mixins
  - 13 plugins
  - 10 lib utilities
  - 6 api modules + 1 router
  - 11 build/config files (excluded from xref)

Source repo:
- `nmodsreflow/nmodsreflow-rt/src/` — 77 Java files
- `reflow-frontend/src/` — ~430+ JS/Vue files

---

## Symbol Kinds and Ripgrep Strategies

### 1. Java class (77 symbols)
- Intra-module imports + extends/implements within `nmodsreflow/`
- Pattern: `rg "import com\.niagaramods\.nmodsreflow\.<FQN_tail>"` + `rg "extends <ClassName>|implements <ClassName>"`
- Sample: `rg "import com\.niagaramods\.nmodsreflow\."` → **112 occurrences across 35 files**
- **Estimated edges**: ~110 java→java (frontend never imports Java directly — uses REST/BOX/WS)

### 2. REST function (28 named exports in `api/rest.js`)
- Functions exposed via `Vue.prototype.$niagara` plugin — NOT direct imports
- Pattern: `rg "niagara\.(getConfig|saveConfig|queryAlarms|...)"` + plugins/http.js direct uses
- Exception: `api/external.js` `checkVersion` IS imported directly in `store/modules/updates.js`
- **Estimated edges**: ~84 (28 × ~3 callers)

### 3. BOX method (21 named exports in `api/box.js`)
- Same indirection as REST — exposed via `$niagara` plugin
- Pattern: `rg "niagara\.(historyGetData|alarmGetClasses|getLicenseData|...)"`
- **Estimated edges**: ~63

### 4. WebSocket command (11 named exports in `api/websocket.js`)
- Pattern: `rg "websocket\.(join|route|configRoute|...)"` and `\$niagara\.ws\.`
- Sample: `websocket.|ws.|WS_` → 39 occurrences across 18 files
- **Estimated edges**: ~28

### 5. Vue component (378 symbols)
- Imports + template usage `<ComponentName` or `<component-name`
- Pattern: `rg "import.*ComponentName\.vue"` + `rg "<ComponentName[\s/>]"`
- Sample: `import.*from '@/components/'` → 53 across 20; many use relative `'./Component.vue'`
- **Estimated edges**: ~750 (highest count due to component reuse)

### 6. Vuex store module (30 modules)
- Map helpers + dispatch/commit
- Pattern: `rg "mapState\(['\"]<module>"` + `rg "dispatch\(['\"]<module>\/"`
- Sample: `mapState|mapGetters|mapActions|mapMutations` → **248 files**; `store.dispatch|store.commit` → **509 occurrences across 152 files**
- **Estimated edges**: ~240 (highest density per symbol)

### 7. Mixin (18 symbols)
- `mixins: [...]` array references
- Pattern: `rg "from ['\"]@/mixins/<name>|from ['\"]\./<name>"` + `rg "<name>" src/`
- Sample: `mixins: [` → **72 occurrences across 72 files**
- **Estimated edges**: ~72

### 8. Plugin (13 symbols, become `this.$pluginName`)
- Direct `this.$plugin` references
- Pattern: `rg "\$niagara\.|\$baja\.|\$utils\."`
- Sample: `$niagara.` → **268 occurrences in 54 files**
- **Estimated edges**: ~260 (dominated by `$niagara`)

### 9. Lib utility (10 symbols in `src/lib/`)
- Pattern: `rg "from ['\"]@/lib/<name>|from ['\"](\.\.\/)*lib/<name>"`
- Sample: `import.*from '@/lib/'` → 19 across 15
- **Estimated edges**: ~30

### 10. REST endpoint URL (28 URL paths in `api/rest.js`)
- Pattern: `rg "/nmodsreflow/<path>"`
- Sample: `nmodsreflow/` → 57 occurrences across 18 files
- **Estimated edges**: ~42 (low — most URLs only in `api/rest.js`)

---

## Volume Summary

| Symbol Kind   | Count | Avg Callers | Est. Edges |
|---------------|------:|------------:|-----------:|
| java-class    |    77 |         1.4 |       ~110 |
| rest-function |    28 |         3.0 |        ~84 |
| box-method    |    21 |         3.0 |        ~63 |
| ws-command    |    11 |         2.5 |        ~28 |
| vue-component |   378 |         2.0 |       ~750 |
| store-module  |    30 |         8.0 |       ~240 |
| mixin         |    18 |         4.0 |        ~72 |
| plugin        |    13 |        20.0 |       ~260 |
| lib-utility   |    10 |         3.0 |        ~30 |
| rest-url      |    28 |         1.5 |        ~42 |
| **TOTAL**     |   614 |             |    **~1679** |

**Realistic xref entries**: ~536 (configs excluded). **Realistic edges**: 1,400–1,800.

---

## Schema Design

```json
{
  "schema_version": "1.0",
  "xref_for": "reflow-clean-177",
  "generated_at": "ISO-8601",
  "total_entries": 536,
  "entries": [
    {
      "id": "...",
      "symbol": "...",
      "kind": "java-class|rest-function|box-method|ws-command|vue-component|store-module|mixin|plugin|lib-utility|rest-url",
      "defined_at": "...",
      "used_at": [
        { "path": "relative/path", "usage_kind": "import|extends|implements|invoke|inject|map-state|map-getter|map-action|dispatch|commit|template|mixin-ref|rest-call|box-call|ws-call" }
      ],
      "usage_count": 0,
      "unused": true
    }
  ]
}
```

---

## Approaches

| Approach | Pros | Cons | Effort |
|----------|------|------|--------|
| **A. Per-kind batched sub-agents → JSON shards merged via jq** | Proven (mapping SDD), parallelizable, independently verifiable shards | jq merge step | Medium |
| B. Single sub-agent over all 536 symbols | Simpler orchestration | Context window risk | High risk |
| C. Python/Node scanner script | Total coverage, alias-aware | Tooling not in repo | High |

**Recommended**: Approach A — same pipeline as `mapping-reflow-clean-177`. 10 batches, one per kind.

---

## Edge Cases

1. **Renamed imports** (`{ X as Y }`): file attributed correctly, export-level lost. Mark `usage_kind: import-renamed`.
2. **Same-name symbols**: pattern `<Symbol[\s/>]` is anchored enough.
3. **External/framework symbols**: out of scope — they live in `dependencies[]` of index.json.
4. **Decompiled stubs**: include as `used_at` sources, flag clearly.
5. **Plugin injection**: track via `$pluginName.` regex, not import patterns.
6. **API call indirection**: REST/BOX exposed through `$niagara` — search both plugin pattern AND store actions.

---

## Risks

1. False positives from template strings/comments → mitigated by `-t vue` and context filtering.
2. False negatives from aliased imports → file-level captured, export-level approximate.
3. Transitive usage not captured (by design — direct only).
4. Dynamic component bindings `<component :is>` → static-unresolvable, estimated <5% miss.
5. Scale: 536 symbols × 10 kinds → per-kind batching keeps each batch fast.

---

## Reusability for Analytics/MX60

**Core (any Java+Vue codebase)**: java-class, vue-component, store-module, mixin, plugin, lib-utility kinds + core schema fields.

**Extension per codebase**:
- `rest-function` + `rest-url` → any REST-API codebase
- `box-method` → Niagara BOX codebases (Reflow, Analytics)
- `ws-command` → WebSocket codebases
- `algorithm-block` → Analytics DAG specifically
- `bql-query` → Analytics

---

## Recommendation for Propose Phase

1. Build `xref.json` via per-kind batched sub-agents (Approach A).
2. Each batch outputs shard `xref-{kind}.json`.
3. Final merge → canonical `docs/mappings/reflow-clean-177/xref.json` (sibling to `index.json`).
4. Schema as designed above.
5. **Priority order for batches** (highest xref value first):
   - store-module (consumer density)
   - vue-component (largest set)
   - plugin (high structural signal)
   - mixin
   - rest-function
   - box-method
   - java-class
   - lib-utility
   - ws-command
   - rest-url

**Estimated output**: ~536 xref entries, 1,400–1,800 edges, `xref.json` ~300–400 KB.

---

## Ready for Proposal

Yes — all scoping, schema, ripgrep strategies, volume estimates, and edge cases are defined.
