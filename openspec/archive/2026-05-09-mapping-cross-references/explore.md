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

## Ready for Proposal

Yes — all scoping, schema, ripgrep strategies, volume estimates, and edge cases are defined.
