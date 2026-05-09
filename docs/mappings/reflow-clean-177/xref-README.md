# Cross-Reference Index — Usage Guide

The `xref.json` file is a queryable usage graph for `Reflow-Clean-177`. It complements the mapping (`index.json`) by answering "WHO uses this symbol?" instead of "WHAT is this file?".

For schema details, see `xref-schema.md`.

---

## What's Inside

- `xref.json` — canonical machine-readable graph (615 entries, ~390 KB).
- `xref.md` — human-readable summary with top consumers, unused symbols, top-20 most-used.
- `xref-schema.md` — schema reference.
- `xref-README.md` — this file.

---

## Quick `jq` Recipes

### 1. Find unused symbols (dead-code candidates)
```bash
jq -r '.entries[] | select(.unused == true) | "\(.kind)\t\(.symbol)\t\(.defined_at)"' xref.json | sort
```

### 2. Find all callers of a specific symbol
```bash
jq '.entries[] | select(.symbol == "BReflowService") | .used_at' xref.json
```

### 3. Top-20 most-used symbols
```bash
jq '.entries | sort_by(.usage_count) | reverse | .[0:20] | map({symbol, kind, usage_count})' xref.json
```

### 4. All Vue components consumed by a specific file
```bash
jq --arg p "reflow-frontend/src/views/EquipmentHome.vue" \
  '.entries | map(select(.kind == "vue-component" and (.used_at[]?.path == $p)))' xref.json
```

### 5. Filter by domain via index.json join
```bash
# Find all xref entries whose mapping says they're in the "floorplans" domain
ids=$(jq -r '.entries[] | select(.domain == "floorplans") | .id' index.json)
for id in $ids; do
  jq --arg id "$id" '.entries[] | select(.id == $id) | {symbol, usage_count}' xref.json
done
```

### 6. Plugin call density (top consumers of `$niagara`)
```bash
jq '.entries[] | select(.symbol == "$niagara") | .used_at | group_by(.path) | map({path: .[0].path, calls: length}) | sort_by(.calls) | reverse | .[0:10]' xref.json
```

---

## Quick `rg` Recipes

### 1. Find every file that imports a specific symbol
```bash
rg "from ['\"][^'\"]+/<SymbolName>" /home/cristian/modules/Prototipos/Reflow-Clean-177/reflow-frontend/src/
```

### 2. Find every file that uses a Vue component as template tag
```bash
rg "<DeviceCard[\s/>]" /home/cristian/modules/Prototipos/Reflow-Clean-177/reflow-frontend/src/
```

### 3. Find every store dispatch to a module
```bash
rg "dispatch\(['\"]alarms/" /home/cristian/modules/Prototipos/Reflow-Clean-177/reflow-frontend/src/
```

### 4. Find every Java class import (intra-module)
```bash
rg "import com\.niagaramods\.nmodsreflow\.[^;]*<ClassName>" /home/cristian/modules/Prototipos/Reflow-Clean-177/nmodsreflow/
```

---

## Extending for Another Module (Analytics, MX60)

The schema is `core + extension`. To produce an `xref.json` for another module:

1. **Build a sibling `index.json`** for the new module using the mapping schema (see `schema.md`).
2. **Run the same per-kind sub-agent pipeline** as `mapping-cross-references` SDD:
   - Batch A — synthetic kinds (REST/BOX/WS/URL functions, if applicable to the new module)
   - Batch B — Java + small kinds (java-class, store-module, mixin, plugin, lib-utility)
   - Batch C — Vue components (split by domain into ≤80-entry shards)
   - Batch D — validate + merge with jq
3. **Add an extension block** for codebase-specific kinds (e.g., `analytics` block with `algorithm-block` and `bql-query` kinds).
4. **Update `xref-schema.md`** for the new module to declare extension fields. Core schema STAYS identical.
5. **Reuse this README** with paths swapped — query patterns transfer 1-to-1 since core fields are the same.

---

## Companion: `index.json`

Every entry in `xref.json` whose `kind` is non-synthetic (`java-class`, `vue-component`, `store-module`, `mixin`, `plugin`, `lib-utility`) has its `id` matching an `id` in `index.json`. Synthetic kinds (`rest-function`, `box-method`, `ws-command`, `rest-url`) use `id` of form `<source-file>#<name>`.

To resolve a full picture for a symbol:
```bash
jq --arg sym "BReflowService" '
  .entries[] | select(.symbol == $sym)
' index.json   # WHAT is this file?

jq --arg sym "BReflowService" '
  .entries[] | select(.symbol == $sym)
' xref.json   # WHO uses it?
```

---

## Known Limitations (Quick Reminder)

See `xref-schema.md` for full detail. TL;DR:

- Dynamic `<component :is>` not captured.
- `import { X as Y }` file-level captured, symbol-level approximate.
- Niagara reflection-loaded Java classes appear `unused` (correct per scope).
- Transitive usage NOT recorded — only direct.
