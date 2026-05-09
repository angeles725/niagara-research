# Design — mapping-cross-references

**Status**: locked
**Companion**: spec.md (parallel artifact)
**Inputs**: proposal.md (LOCKED), explore.md
**Output target**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json` (sibling of `index.json`)

---

## 0. Architectural Overview

Per-kind ripgrep producers → JSON shards → `jq -s 'add'` merge → canonical `xref.json` →
`jq` projections → `xref.md`. The pipeline is the SAME shape used in
`mapping-reflow-clean-177` so it inherits its proven properties (parallel sub-agent
fan-out, idempotent merge, individual shard validation).

```
        [index.json] (READ)
              │
              ▼
   ┌──────────────────────────┐
   │ Stage 0 — preflight      │  inline orchestrator
   │  group entries by kind   │  (no sub-agent)
   └──────────────────────────┘
              │
              ▼
   ┌──────────────────────────┐
   │ Stage 1 — per-kind rg    │  Batch A (1 sa)
   │  produces JSON shards    │  Batch B (3 parallel)
   │  /tmp/xref-shards/*.json │  Batch C (5 parallel)
   └──────────────────────────┘
              │
              ▼
   ┌──────────────────────────┐
   │ Stage 2 — jq merge       │  Batch D
   │  validate + assemble     │  (inline or 1 sa)
   │  → xref.json             │
   └──────────────────────────┘
              │
              ▼
   ┌──────────────────────────┐
   │ Stage 3 — xref.md        │  jq + bash heredoc
   │  Stage 4 — schema/README │  manual prose
   └──────────────────────────┘
```

Source of truth: `index.json` provides `id`, `kind`, `defined_at`. The xref pipeline
NEVER re-derives these — only computes `used_at[]`, `usage_count`, `unused`.

---

## 1. Per-kind ripgrep patterns (LOCKED)

The source repo is **read-only**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
- Java search root: `nmodsreflow/`
- JS/Vue search root: `reflow-frontend/src/`

Common rg flags for all batches:
- `--json` for machine-readable output
- `-n` for line numbers (helps spot-check)
- `--no-messages` to silence missing-file noise
- File-type filters where applicable: `-t java`, `-t js`, plus a custom `--type-add 'vue:*.vue'`
  registered once by the sub-agent before the first scan.

All shards live in `/tmp/xref-shards/{kind}.json` and follow the array-of-entries shape
defined in §3.

### 1.1 java-class (77 symbols)

**Defined**: `nmodsreflow/nmodsreflow-rt/src/...`
**Searched**: same root (intra-Java only; frontend never imports Java).

For each class entry, the producer emits TWO patterns and unions the resulting paths:

```bash
SHORT=<simpleName>          # e.g. ReflowAlarmCommands
FQN=<fullyQualifiedName>    # e.g. com.niagaramods.nmodsreflow.alarm.ReflowAlarmCommands

# (a) import statements — usage_kind: "import"
rg --json -t java "^import\s+${FQN};" nmodsreflow/

# (b) inheritance — usage_kind: "extends" or "implements"
rg --json -t java "\bextends\s+${SHORT}\b|\bimplements\s+[^;{]*\b${SHORT}\b" nmodsreflow/
```

Iteration: the sub-agent reads the 77 java-class entries from index.json, builds a
shell loop, captures rg matches keyed by `id`, and writes one shard.

**Edge cases**:
- Self-references (a class file matching its own name) are filtered by comparing
  match path against the entry's `defined_at`.
- Inner classes (`Outer.Inner`) keep the outer class's id; the inner is not a
  separate index.json entry, so we ignore that nesting.

### 1.2 rest-function (28 symbols)

**Defined in**: `reflow-frontend/src/api/rest.js`
**Searched in**: `reflow-frontend/src/`

Function names are the 28 listed in §0 of this design (verified against actual file).

```bash
NAME=<funcName>             # e.g. getConfig

# (a) direct import (rare — only http.js/external.js do this) — "import"
rg --json -t js "from\s+['\"]@?/?api/rest['\"]" reflow-frontend/src/

# (b) plugin invocation — "rest-call" (also tracked at plugin level — see §2)
rg --json "\$niagara\.${NAME}\s*\(" reflow-frontend/src/
rg --json "this\._vm\.\$niagara\.${NAME}\s*\(" reflow-frontend/src/

# (c) destructured plugin access (defensive): const { getConfig } = this.$niagara
rg --json "\$niagara\b[^.]*\b${NAME}\s*\(" reflow-frontend/src/  # post-filter
```

Pattern (b) is the dominant path. Sub-agent prefers (b) and uses (c) only when (b)
returns zero matches for a function known to be wired in `plugins/niagara.js`.

### 1.3 box-method (24 symbols — NOT 21)

**Reconciliation note**: proposal listed 21; the actual `api/box.js` exports 24
(initial 3 + 3 nav + 1 file + 1 csv + 7 history + 8 alarm + 2 role + 2 license = 24).
The producer iterates entries from index.json, which is authoritative.

**Defined in**: `reflow-frontend/src/api/box.js`
**Searched in**: `reflow-frontend/src/`

```bash
NAME=<funcName>             # e.g. historyGetData

# (a) plugin invocation — "box-call"
rg --json "\$niagara\.${NAME}\s*\(" reflow-frontend/src/
rg --json "this\._vm\.\$niagara\.${NAME}\s*\(" reflow-frontend/src/

# (b) direct import — "import" (rare)
rg --json -t js "from\s+['\"]@?/?api/box['\"]" reflow-frontend/src/
```

### 1.4 ws-command (14 symbols, ~11 are commands)

**Defined in**: `reflow-frontend/src/api/websocket.js`
**Searched in**: `reflow-frontend/src/`

```bash
NAME=<funcName>             # e.g. join, configControlRequest

# (a) module-level invocation — "ws-call"
rg --json "\bwebsocket\.${NAME}\s*\(" reflow-frontend/src/

# (b) plugin-namespaced — "ws-call"
rg --json "\$niagara\.ws\.${NAME}\s*\(" reflow-frontend/src/

# (c) direct import — "import"
rg --json -t js "from\s+['\"]@?/?api/websocket['\"]" reflow-frontend/src/
```

`initSocket`, `disconnectSocket`, `getSocket` are infrastructure exports — they will
have very few callers (1–2 each) and may legitimately end up `unused: false`
because they are referenced from `plugins/niagara.js` only. That is correct
behaviour, not a bug.

### 1.5 vue-component (378 symbols)

**Defined in**: `reflow-frontend/src/{components,views}/.../*.vue`
**Searched in**: `reflow-frontend/src/`

For each component, FOUR patterns:

```bash
NAME=<ComponentName>        # PascalCase, e.g. DashboardCard
KEBAB=<kebab-case>          # e.g. dashboard-card

# (a) template usage PascalCase — "template"
rg --json --type-add 'vue:*.vue' -t vue "<${NAME}\b[\s/>]" reflow-frontend/src/

# (b) template usage kebab-case — "template"
rg --json --type-add 'vue:*.vue' -t vue "<${KEBAB}\b[\s/>]" reflow-frontend/src/

# (c) relative import — "import"
rg --json "from\s+['\"]\.{1,2}(/[^'\"]+)*/${NAME}(\.vue)?['\"]" reflow-frontend/src/

# (d) absolute import — "import"
rg --json "from\s+['\"]@/(components|views)/[^'\"]+/${NAME}(\.vue)?['\"]" reflow-frontend/src/
```

**Anchoring rule**: every template pattern uses `\b...[\s/>]` so closing tags
`</X>` and partial matches like `<ConfigCellText>` do NOT collide with `<ConfigCell>`.

**Sub-shard split** (5 parallel sub-agents, drawn from `component_dir` in index.json):

| Shard | Directories | Approx components |
|-------|-------------|-------------------|
| C1 | `equipment`, `dashboard`, `buildings`, `config` | ~100 |
| C2 | `floorplans` | ~52 |
| C3 | `alarms`, `history`, `cards`, `charts` | ~73 |
| C4 | `common`, `navigation`, `layout`, `map`, `maps`, `pages`, `points`, `profiles`, `schedules`, `settings`, `weather`, `websocket`, `wizard`, `browser`, `views` | ~150 |
| C5 | overflow (used only if any of C1–C4 exceeds 110 entries after the actual count) | — |

C5 is reserved capacity. If the empirical split is balanced, only C1–C4 run.

### 1.6 store-module (30 symbols)

**Defined in**: `reflow-frontend/src/store/modules/*.js`
**Searched in**: `reflow-frontend/src/`

```bash
NAME=<moduleName>           # e.g. config, alarms, equipment

# (a) map helpers — usage_kind matches the helper exactly
rg --json "mapState\(\s*['\"]${NAME}['\"]"      reflow-frontend/src/   # map-state
rg --json "mapGetters\(\s*['\"]${NAME}['\"]"    reflow-frontend/src/   # map-getter
rg --json "mapActions\(\s*['\"]${NAME}['\"]"    reflow-frontend/src/   # map-action
rg --json "mapMutations\(\s*['\"]${NAME}['\"]"  reflow-frontend/src/   # commit (mutation map)

# (b) namespaced dispatch/commit — "dispatch" / "commit"
rg --json "dispatch\(\s*['\"]${NAME}/" reflow-frontend/src/
rg --json "commit\(\s*['\"]${NAME}/"   reflow-frontend/src/

# (c) direct import of store module file — "import"
rg --json "from\s+['\"][^'\"]*store/modules/${NAME}['\"]" reflow-frontend/src/
```

Multi-pattern hits on the same `(path, module)` collapse into a single `used_at`
entry per `usage_kind`. A single file consuming a module via both `mapState` and
`mapActions` produces TWO `used_at` rows, one per `usage_kind`.

### 1.7 mixin (18 symbols)

**Defined in**: `reflow-frontend/src/mixins/*.js` (+ `profiles/profileMixin.js`)
**Searched in**: `reflow-frontend/src/`

```bash
NAME=<mixinName>            # e.g. resizableMixin

# (a) absolute import — "import"
rg --json "from\s+['\"]@/mixins/${NAME}['\"]" reflow-frontend/src/

# (b) relative import — "import"
rg --json "from\s+['\"](\.{1,2}/)+mixins/${NAME}['\"]" reflow-frontend/src/

# (c) co-located profileMixin — "import"
rg --json "from\s+['\"]\./profileMixin['\"]" reflow-frontend/src/profiles/

# (d) registration in component options — "mixin-ref"
rg --json "mixins\s*:\s*\[[^\]]*\b${NAME}\b" reflow-frontend/src/
```

Pattern (d) confirms actual usage (vs. dead imports). When (d) matches without (a)/(b)
we still record (d) — it means the mixin was destructured from a barrel.

### 1.8 plugin (13 symbols)

**Defined in**: `reflow-frontend/src/plugins/*.js`
**Searched in**: `reflow-frontend/src/`

Plugins manifest as `Vue.prototype.$NAME`. The xref tracks the plugin's METHOD-LEVEL
invocations:

```bash
NAME=<pluginName>           # e.g. niagara, baja, utils, time, ord, http, gbo,
                            #      workbench, cookies, configMode, labelForItem,
                            #      colorUtils, reflowLink

# (a) method invocation on plugin — "invoke"
rg --json "\bthis\.\$${NAME}\." reflow-frontend/src/
rg --json "\b\\\$${NAME}\."     reflow-frontend/src/   # template / setup() refs
```

Each match contributes `usage_kind: "invoke"` plus a `method` sub-property whose value
is captured from the regex's first group (the dot-suffixed identifier). See §2 for
how this folds back into rest-function / box-method / ws-command shards.

**Plugin file imports** themselves (`import niagara from '@/plugins/niagara'`) only
exist in `main.js`. They contribute one `usage_kind: "import"` per plugin.

### 1.9 lib-utility (10 symbols)

**Defined in**: `reflow-frontend/src/lib/*.js`
**Searched in**: `reflow-frontend/src/`

```bash
NAME=<libName>              # e.g. alarmCache, deepMerge, uuid

# (a) absolute — "import"
rg --json "from\s+['\"]@/lib/${NAME}['\"]" reflow-frontend/src/

# (b) relative — "import"
rg --json "from\s+['\"](\.{1,2}/)+lib/${NAME}['\"]" reflow-frontend/src/
```

Renamed imports (`import { uuid as genId }`) match the file but NOT the symbol.
Non-goal per proposal — accepted.

### 1.10 rest-url (28 symbols)

**Defined in**: `reflow-frontend/src/api/rest.js` (URL-side of each endpoint)
**Searched in**: `reflow-frontend/src/`

```bash
URL=<urlPath>               # e.g. station/alarms/query (NO leading slash)

rg --json "['\"]/nmodsreflow/${URL}['\"]" reflow-frontend/src/
rg --json "BASE\s*\+\s*['\"]/?${URL}['\"]" reflow-frontend/src/  # concatenated form
```

The vast majority of URLs are referenced ONLY in `api/rest.js` itself. The xref
shard reflects that — most rest-url entries will end up `unused: true` from the
consumer-side view because no Vue component hard-codes a URL string. That is the
intended diagnostic signal.

`api/rest.js` is the definition file — its own occurrences are filtered out.

---

## 2. `$niagara` Disambiguation (LOCKED)

The plugin `$niagara` aggregates REST, BOX, and WS calls. A single invocation
`this.$niagara.getConfig()` is meaningful at TWO levels.

### Stage 1 — plugin-level edge

The plugin shard records:

```json
{
  "id": "plugin/niagara",
  "symbol": "$niagara",
  "kind": "plugin",
  "defined_at": "src/plugins/niagara.js",
  "used_at": [
    {
      "path": "src/store/modules/config.js",
      "usage_kind": "invoke",
      "method": "getConfig"
    }
  ]
}
```

The optional `method` sub-field is part of the schema for `kind: plugin` ONLY.

### Stage 2 — method-level edge

The Stage 1 producer publishes a side-channel JSON (`/tmp/xref-shards/_niagara-methods.json`)
listing every `(path, method)` it captured. Stages for rest-function, box-method,
ws-command READ that side-channel and INSERT a corresponding `used_at` entry on the
matching method's xref shard:

| Method matches… | Inserts on shard | usage_kind |
|-----------------|------------------|------------|
| name in `api/rest.js` exports | rest-function | `rest-call` |
| name in `api/box.js` exports | box-method | `box-call` |
| name in `api/websocket.js` exports (or `ws.NAME`) | ws-command | `ws-call` |
| nothing (e.g. `$niagara.config` data) | — | dropped |

Crucially, if a method name appears in BOTH `rest.js` AND `box.js` (collision check
required), the producer writes to BOTH shards and logs a `WARN: ambiguous method NAME`.
Spot-check during Batch D verifies no real collisions exist (audited the three files
already — none today).

A single `$niagara.getConfig()` therefore produces:
- 1 edge on `plugin/niagara` (`invoke`, `method=getConfig`)
- 1 edge on `rest-function/getConfig` (`rest-call`)

That is intentional double-attribution: the plugin view answers "who pokes the
plugin?", the method view answers "who calls THIS function?". Both are needed.

### `websocket.NAME` vs `$niagara.ws.NAME`

The ws-command producer must NOT consume the `_niagara-methods.json` for entries
whose method does not start with a ws-command name. It consumes only entries
emitted under the `$niagara.ws.X` regex branch (Stage 1 keeps a `via_ws: true`
flag on those side-channel rows).

---

## 3. JSON Shard Contract

Shard path: `/tmp/xref-shards/{kind}.json` (one file per kind; vue-component splits
into `vue-component-c1.json` … `vue-component-c5.json`).

```json
[
  {
    "id": "string — same as index.json",
    "symbol": "string",
    "kind": "string — one of 10 enum values",
    "defined_at": "string — relative path",
    "used_at": [
      {
        "path": "string — relative path under reflow-frontend/src/ or nmodsreflow/",
        "usage_kind": "string — one of 17 enum values",
        "method": "string — OPTIONAL, only when kind=plugin"
      }
    ],
    "usage_count": 0,
    "unused": true
  }
]
```

**Sort order**: entries within a shard are sorted by `symbol` ASC. `used_at[]` is
sorted by `path` ASC, then `usage_kind` ASC. Stable sort guarantees diffability of
regenerations.

**Computed fields**:
- `usage_count = length(used_at)`
- `unused = (usage_count == 0)`

Producer must compute both before emit (validation later re-checks them).

**Path normalization**: every `path` is relative to the source repo root, using
forward slashes. Examples:
- `nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/alarm/ReflowAlarmCommands.java`
- `reflow-frontend/src/store/modules/config.js`

Self-references (path == defined_at) are filtered out.

---

## 4. `jq` Merge Contract

```bash
cd /tmp/xref-shards/

# 1. validate each shard parses
for f in *.json; do
  [ "$f" = "_merged.json" ] && continue
  [ "$f" = "_niagara-methods.json" ] && continue
  jq -e 'type=="array"' "$f" >/dev/null || { echo "INVALID: $f"; exit 1; }
done

# 2. merge
jq -s 'add' \
  java-class.json \
  rest-function.json \
  box-method.json \
  ws-command.json \
  vue-component-c*.json \
  store-module.json \
  mixin.json \
  plugin.json \
  lib-utility.json \
  rest-url.json \
  > _merged.json

TOTAL=$(jq 'length' _merged.json)
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# 3. assemble canonical shape
jq --arg now "$NOW" --argjson total "$TOTAL" '{
  schema_version: "1.0",
  xref_for: "reflow-clean-177",
  generated_at: $now,
  total_entries: $total,
  entries: (. | sort_by(.kind, .symbol))
}' _merged.json > /home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json

# 4. final validation
jq -e '
  .schema_version=="1.0"
  and .xref_for=="reflow-clean-177"
  and (.entries | length) == .total_entries
  and (.entries | all(.id and .symbol and .kind and .defined_at and (.used_at|type=="array")))
  and (.entries | all(.usage_count == (.used_at|length)))
  and (.entries | all(.unused == (.used_at|length == 0)))
' /home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json
```

Final entry count expected: ~536 (config files excluded as proposal §Volume).

---

## 5. Pipeline Batches

### Batch A — synthetic-kinds bundle (1 sub-agent, sonnet)

Reads `api/rest.js`, `api/box.js`, `api/websocket.js` ONCE; produces FOUR shards
plus the niagara-methods side-channel.

- Shards: rest-function (28), box-method (24), ws-command (14), rest-url (28)
- Side-channel: `_niagara-methods.json`
- Total entries emitted: ~94
- Wall-clock: ~10 min

### Batch B — Java + small kinds (3 parallel sub-agents)

| Sub-agent | Kinds | Entries |
|-----------|-------|---------|
| B1 | java-class | 77 |
| B2 | store-module + mixin | 30 + 18 = 48 |
| B3 | plugin + lib-utility | 13 + 10 = 23 |

B3 must run AFTER A so it can finalize the niagara plugin's `used_at` list using
the side-channel. Practical scheduling: A → (B1 ∥ B2 ∥ B3).

### Batch C — Vue components (4 parallel sub-agents, plus C5 as overflow)

C1, C2, C3, C4 as defined in §1.5. ~378 entries total. Wall-clock ~25 min for the
slowest shard (C4 has the most directories).

### Batch D — validation + merge (inline orchestrator OR 1 sub-agent)

1. Validate every shard parses (jq -e 'type=="array"').
2. Run merge (§4).
3. Run final validation (§4 step 4).
4. Generate `xref.md` (§6).
5. Spot-check 30 entries (3 per kind).
6. Compute coverage report:
   - Symbols missing from xref vs. index.json (relevant kinds only).
   - Edge count by kind.
   - $niagara double-attribution counter.
7. Save apply-progress to engram.

Inline is preferred unless the orchestrator's context is already heavy.

### Dependency graph

```
[index.json read by all batches]
        │
        ├──► A ──► (writes _niagara-methods.json)
        │      │
        │      ├──► B3 (plugin needs side-channel)
        │      │
        │      └──► (rest/box/ws shards already final from A)
        │
        ├──► B1 (independent)
        ├──► B2 (independent)
        ├──► C1, C2, C3, C4 (independent)
        │
        └──► D (waits on all of A, B1, B2, B3, C1, C2, C3, C4)
```

Total elapsed wall-clock with parallelism: ~45 min (was 1.5h sequential estimate).

---

## 6. `xref.md` Generation

Single bash + jq script, NO sub-agent:

```bash
XR=/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json
OUT=/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.md

TOTAL=$(jq '.total_entries' "$XR")
GENERATED=$(jq -r '.generated_at' "$XR")

cat > "$OUT" <<EOF
# Reflow-Clean-177 — Cross-Reference Index

**Schema version**: 1.0
**Total entries**: ${TOTAL}
**Generated at**: ${GENERATED}
**Sibling**: index.json (mapping)

## Top consumers by kind

| Kind | Symbol | usage_count | Top 3 user paths |
|------|--------|-------------|------------------|
EOF

jq -r '
  .entries
  | group_by(.kind)
  | map(sort_by(-.usage_count) | .[0:5])
  | flatten
  | .[]
  | "| \(.kind) | \(.symbol) | \(.usage_count) | \([.used_at[0:3][].path] | join(", ")) |"
' "$XR" >> "$OUT"

cat >> "$OUT" <<EOF

## Unused symbols

EOF

jq -r '
  .entries
  | map(select(.unused))
  | group_by(.kind)
  | map({kind: .[0].kind, count: length, symbols: map(.symbol)})
  | .[]
  | "### \(.kind) (\(.count) unused)\n\n" + (.symbols | map("- " + .) | join("\n")) + "\n"
' "$XR" >> "$OUT"

cat >> "$OUT" <<EOF

## Most-used symbols overall (top 20)

| Symbol | Kind | usage_count |
|--------|------|-------------|
EOF

jq -r '
  .entries
  | sort_by(-.usage_count)
  | .[0:20][]
  | "| \(.symbol) | \(.kind) | \(.usage_count) |"
' "$XR" >> "$OUT"
```

---

## 7. Trade-offs / Rejected Alternatives

### Rejected — AST-based scanner (Babel for JS/Vue, JavaParser for Java)

- **Pro**: Symbol-perfect, alias-aware, exposes type relationships for free.
- **Con**: Requires Node 18 + Babel + @vue/compiler-sfc + a Java JAR. New tooling
  surface, new failure modes. Slower (~3× rg). The proposal explicitly excludes
  AST-perfect parsing as a non-goal. Adopting it would also require schema
  re-design (per-export edges instead of per-file edges).
- **Verdict**: Reject. The 5–10% precision gain does not justify the tooling debt
  for a research mapping artifact.

### Rejected — Single-pass massive ripgrep with one mega-pattern

- **Pro**: One process, one parse. No shard merge.
- **Con**: 614+ alternatives in a single regex blows past rg's compiled-DFA
  budget. Even if compiled, the JSON output stream is 10× the size of the per-kind
  shards combined, and we lose `usage_kind` fidelity (cannot tell `extends` from
  `import` from a single union pattern). Producer also cannot parallelize.
- **Verdict**: Reject. We deliberately want per-kind isolation for correctness
  AND for shardable verification.

### Rejected — Per-symbol rg invocation (one process per of 614 symbols)

- **Pro**: Trivial code, smallest possible patterns.
- **Con**: 614 process spawns × ~80ms cold-start = ~50s of pure fork overhead per
  full regeneration. Worse, the orchestration logs balloon proportionally,
  drowning the agent's signal in `[rg] match…` lines. Caching nothing.
- **Verdict**: Reject. Per-kind batching with one rg per (kind, pattern) is the
  sweet spot.

### Rejected — Producing xref.md directly without intermediate xref.json

- **Pro**: Saves one file.
- **Con**: Loses jq queryability. Future MX60/Analytics adopters cannot reuse
  the data programmatically. Markdown cannot represent nested `used_at[]`
  cleanly. Schema evolution becomes a regex problem instead of a JSON problem.
- **Verdict**: Reject. The proposal locks JSON-first.

### Rejected — Storing xref.json INSIDE index.json (single file)

- **Pro**: One artifact, one read.
- **Con**: index.json grows from ~250 KB to ~600 KB. The mapping artifact
  changes meaning (was: "what is each file"; would become: "what is + who uses
  each file"). Breaks the contract already shipped to MX60/Analytics consumers.
- **Verdict**: Reject. Sibling files with id-join is the locked decision (proposal §Schema).

---

## 8. Validation Strategy

### 8.1 Per-shard validation (Stage 1.5, before merge)

For each shard:

```bash
jq -e '
  type=="array"
  and (all(.id and .symbol and .kind and .defined_at))
  and (all(.usage_count == (.used_at | length)))
  and (all(.unused == (.used_at | length == 0)))
  and (all(.used_at[].usage_kind | IN("import","import-renamed","extends","implements","invoke","inject","map-state","map-getter","map-action","dispatch","commit","template","mixin-ref","rest-call","box-call","ws-call","dynamic-bind")))
' "$shard"
```

Failure aborts the merge.

### 8.2 Cross-shard validation (Stage 2)

```bash
# every xref id exists in index.json
jq -r '.entries[].id' xref.json | sort -u > /tmp/xref-ids.txt
jq -r '.entries[].id'  index.json | sort -u > /tmp/index-ids.txt

comm -23 /tmp/xref-ids.txt /tmp/index-ids.txt   # MUST be empty
```

### 8.3 Coverage check

```bash
# every relevant index.json id has an xref entry
jq -r '
  .entries
  | map(select(.kind | IN("java-class","vue-component","store-module","mixin","plugin","lib-utility","rest-function","box-method","ws-command","rest-url")))
  | .[].id
' index.json | sort -u > /tmp/index-relevant-ids.txt

comm -23 /tmp/index-relevant-ids.txt /tmp/xref-ids.txt   # MUST be empty
```

### 8.4 Spot-check (30 entries, 3 per kind)

For each kind, pick 3 entries at random (one with high usage_count, one with
low, one `unused`). For each, run the canonical rg pattern manually and confirm
the path-set matches the xref's `used_at[].path`. Document discrepancies in the
verify report.

### 8.5 `$niagara` disambiguation check

```bash
# at least 5 rest-function entries should have a "rest-call" usage_kind
jq '[.entries[] | select(.kind=="rest-function") | select(.used_at[].usage_kind=="rest-call")] | length' xref.json
# expected: >= 5

# at least 3 box-method entries should have a "box-call" usage_kind
jq '[.entries[] | select(.kind=="box-method") | select(.used_at[].usage_kind=="box-call")] | length' xref.json
# expected: >= 3

# plugin "$niagara" should have method sub-field on every used_at row
jq '.entries[] | select(.id=="plugin/niagara") | .used_at | all(has("method"))' xref.json
# expected: true
```

### 8.6 Determinism check

Re-running the pipeline twice produces byte-identical xref.json (modulo
`generated_at`). Sort order in §3 plus stable rg output guarantees this.

```bash
diff <(jq 'del(.generated_at)' xref.json.run1) <(jq 'del(.generated_at)' xref.json.run2)
```

---

## 9. Risks + Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | False positives from HTML comments / string literals matching `<ComponentName` | M | Low | Anchor template patterns with `\b...[\s/>]`; spot-check during 8.4 |
| 2 | Renamed imports lose symbol-level fidelity (`import { uuid as gen }`) | H | Low | Documented as non-goal in proposal; mark `usage_kind: "import-renamed"` only when detected |
| 3 | `$niagara` method names colliding across REST/BOX/WS | L | M | Audit performed (§2): no collisions today. Producer logs `WARN` on any future collision so it surfaces in apply-progress |
| 4 | Vue dynamic binding `<component :is="...">` not captured | M | Low | Documented as non-goal; estimated <5% of usages; record `usage_kind: "dynamic-bind"` only for static aliases that DO match |
| 5 | Vue-component shard split drift (entries assigned to wrong shard) | L | M | Producer reads `component_dir` from index.json; common prompt template ensures identical patterns across C1–C4 |
| 6 | Shard JSON fails to parse, breaking the merge | M | H | §8.1 validates each shard before §8.2 merge. Failure aborts and reports the offending shard |
| 7 | Inner Java classes (`Outer.Inner`) treated as separate symbols | L | Low | Filter: producer uses simple class name regex with `\b` anchors; inner classes are not in index.json so won't get an xref entry anyway |
| 8 | rest-url false positive when string also appears as `bql:url` parameter | L | Low | Filter: pattern requires `'/nmodsreflow/...'` quoted form; bql arguments use a different syntax |
| 9 | Side-channel `_niagara-methods.json` lost between Batch A and B3 | L | H | Producer writes to `/tmp/xref-shards/_niagara-methods.json` AND mirrors to engram (`xref/intermediate/niagara-methods`); B3 falls back to engram if the file is missing |
| 10 | Self-references inflating usage_count | M | Low | Producer filters `path == defined_at`; validated in §8.4 spot-checks |
| 11 | Ambiguous "method" name reused across kinds (e.g. `getConfig` exists in REST AND in some local store) | L | M | Producer scopes each kind's regex to the appropriate file root; if collision detected during §8.5 it is logged and a follow-up SDD addresses it |
| 12 | Schema drift between Reflow xref and future MX60/Analytics adopters | L | M | `schema_version: "1.0"` is locked; any change → 1.1 with migration note in xref-schema.md (proposal §Reusability) |

---

## 10. ADR-style decisions

### ADR-01 — Sibling JSON file with id-join (vs. embedded in index.json)

**Status**: locked (proposal)
**Decision**: Produce `xref.json` next to `index.json`, joined by `id`.
**Rationale**: Keeps index.json contract stable for existing consumers; allows
xref to evolve independently; Markdown view (`xref.md`) is generated, not authored.
**Rejected alt**: embed `used_at[]` inside each index.json entry (§7).

### ADR-02 — Per-kind ripgrep producers, no AST

**Status**: locked
**Decision**: 10 ripgrep batches, each kind isolated, no Babel/JavaParser.
**Rationale**: Tooling minimalism, parallelism, proven by `mapping-reflow-clean-177`.
**Trade-off accepted**: ~5–10% precision loss on aliased imports.

### ADR-03 — `$niagara` two-stage attribution

**Status**: locked
**Decision**: Plugin shard records every `$niagara.X` invocation; method-level
shards (rest-function/box-method/ws-command) ALSO record the same invocation
through a side-channel.
**Rationale**: A single call has two valid interpretations ("plugin user" and
"REST function caller"). Both are needed for separate research questions.
**Cost**: ~+10% edge count, but precision is asymmetric — plugin view stays
coarse, method view stays precise.

### ADR-04 — Vue-component split into 4–5 sub-shards by `component_dir`

**Status**: locked
**Decision**: 4 fixed shards (C1–C4) by directory family, with C5 as overflow.
**Rationale**: 378 components × 4 patterns = 1512 rg invocations sequentially
≈ 2 min; sharded in 4 parallel sub-agents → ~30 s critical path.
**Trade-off**: producers read identical prompt template — prevents drift.

### ADR-05 — Side-channel `_niagara-methods.json` between Batch A and B3

**Status**: locked
**Decision**: Batch A emits a side-channel JSON with every `(path, method)`
captured; B3 (plugin shard) consumes it; rest/box/ws shards (also from A) already
encode their counterpart.
**Rationale**: Avoids a circular dependency where the plugin shard would need to
re-scan the same patterns. Eliminates re-work; keeps each shard producer focused
on one kind.
**Risk**: file loss between batches; mirrored to engram (`Risk #9`).

### ADR-06 — Sort order: by `(kind, symbol)` at top level, by `(path, usage_kind)` within `used_at[]`

**Status**: locked
**Decision**: Deterministic sort at every level.
**Rationale**: byte-identical regenerations; clean diffs in git when xref.json
is committed; predictable section order in xref.md.

### ADR-07 — Shard format identical to mapping shards (array of entries, no top-level wrapper)

**Status**: locked
**Decision**: `/tmp/xref-shards/{kind}.json` is a JSON array; `jq -s 'add'`
merges them; the top-level wrapper (`schema_version`, `total_entries`, `entries`)
is added once at merge time.
**Rationale**: simple `jq -s 'add'`; rejection of per-shard wrappers (which would
require `jq '[.[].entries] | add'` and lose the symmetry).

---

## 11. Reusability for MX60 / Analytics (commitment)

The pipeline architecture is the reusable surface. Concretely:

- `Stage 0/2/3/4` are kind-agnostic and reusable as-is.
- `Stage 1` patterns (§1.1, §1.5, §1.6, §1.7, §1.8, §1.9) are framework-level
  (Java + Vue) and apply to MX60 / Analytics with at most a path-root change.
- The Reflow-specific producers (§1.2, §1.3, §1.4, §1.10) become OPTIONAL
  extensions, declared in a per-codebase manifest (`xref-config.yaml`,
  not in scope here).
- Schema additions for Analytics (`algorithm-block`, `bql-query`) are
  forward-compatible: new `kind` enum values + same per-entry shape.

Concrete reuse path for Analytics:
1. Run Stages 0/2/3/4 unchanged.
2. Replace Batch A producers with `algorithm-block` + `bql-query` producers
   (different rg patterns, same shard contract).
3. Reuse Batches B1/B2 unchanged (same Java + Vuex/mixin patterns).
4. Reuse Batch C with a different component-dir map.

---

## 12. Open questions / assumptions requiring validation

1. **BOX export count**: proposal says 21, file shows 24. Locked to "use
   index.json as authoritative" — verify in apply that index.json has 24
   box-method entries (or 21, if grouping rule excluded utility methods).
2. **WS export count**: file shows 14, proposal says 11 commands. The 3
   utilities (init/disconnect/getSocket) are infrastructure; expected to
   remain `unused` or sparsely used in xref. Confirm during spot-check.
3. **Component count exactness**: proposal says 378; sub-agent must trust
   index.json's count for shard splits.
4. **`api/index.js` and `api/external.js`** are out of scope for this xref
   round (not on the kind list). If their consumers are needed later,
   add a `api-module` kind in v1.1.
