# Cross-Reference Schema — `xref.json`

**Schema version**: `1.0`
**Sibling artifact**: `index.json` (the mapping). The xref artifact answers "WHO USES this symbol?" while the mapping answers "WHAT is this file?".

---

## Envelope

```json
{
  "schema_version": "1.0",
  "xref_for": "reflow-clean-177",
  "sibling_index": "index.json",
  "generated_at": "ISO 8601 datetime",
  "total_entries": <integer>,
  "entries": [ ... ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `schema_version` | string | Semver-like. `1.0` initial. |
| `xref_for` | string | Module name. Pairs with `index.json`'s `module` field. |
| `sibling_index` | string | Path or filename of the companion mapping. |
| `generated_at` | ISO datetime | Build timestamp. |
| `total_entries` | integer | Count of entries[] for jq sanity. |
| `entries` | array | Per-symbol records. |

---

## Per-Entry Core Fields

```json
{
  "id": "...",
  "symbol": "...",
  "kind": "...",
  "defined_at": "...",
  "used_at": [
    { "path": "relative/path", "usage_kind": "..." }
  ],
  "usage_count": 0,
  "unused": true
}
```

| Field | Type | Mandatory | Description |
|-------|------|:---------:|-------------|
| `id` | string | Yes | Stable identifier. For non-synthetic kinds, MUST match an `id` in `index.json`. For synthetic kinds (rest-function, box-method, ws-command, rest-url) uses form `<source-file>#<name>`. |
| `symbol` | string | Yes | Human-readable name (class name, function name, plugin variable, URL path). |
| `kind` | enum | Yes | One of the 10 closed values below. |
| `defined_at` | string | Yes | Path relative to `Reflow-Clean-177/` repo root. |
| `used_at` | array | Yes (may be empty) | Each element: `{path, usage_kind}`. |
| `usage_count` | integer | Yes | `used_at \| length`. Computed at build. |
| `unused` | boolean | Yes | `usage_count == 0`. Computed at build. |

### `kind` Enum (closed set, REQ-2)

| Value | Defined In | Coverage Source |
|-------|-----------|-----------------|
| `java-class` | `nmodsreflow/.../*.java` | index.json `kind == "java-class"` |
| `vue-component` | `reflow-frontend/src/{components,views}/.../*.vue` | index.json `kind == "vue-component"` |
| `store-module` | `reflow-frontend/src/store/{index,modules}/*.js` | index.json `kind == "js-store"` |
| `mixin` | `reflow-frontend/src/mixins/*.js` (+ co-located) | index.json `kind == "js-mixin"` |
| `plugin` | `reflow-frontend/src/plugins/*.js` | index.json `kind == "js-plugin"` |
| `lib-utility` | `reflow-frontend/src/lib/*.js` | index.json `kind == "js-lib"` |
| `rest-function` | `reflow-frontend/src/api/rest.js` (named exports) | Synthetic — derived from api file |
| `box-method` | `reflow-frontend/src/api/box.js` (named exports) | Synthetic |
| `ws-command` | `reflow-frontend/src/api/websocket.js` (named exports) | Synthetic |
| `rest-url` | URL string literals in `api/rest.js` | Synthetic |

### `usage_kind` Enum (closed set, REQ-3)

| Value | Meaning |
|-------|---------|
| `import` | Static `import` statement of the module/symbol. |
| `extends` | Java `extends ClassName`. |
| `implements` | Java `implements InterfaceName`. |
| `invoke` | Method/function invocation (`Foo.bar()` or plain `bar()`). |
| `inject` | Vue plugin injection via `Vue.prototype.$plugin`. |
| `map-state` | Vuex `mapState('module', ...)`. |
| `map-getter` | Vuex `mapGetters('module', ...)`. |
| `map-action` | Vuex `mapActions('module', ...)`. |
| `map-mutation` | Vuex `mapMutations('module', ...)`. |
| `dispatch` | Vuex `store.dispatch('module/action')`. |
| `commit` | Vuex `store.commit('module/mutation')`. |
| `template` | Vue SFC template tag `<ComponentName>`. |
| `mixin-ref` | Mixin listed in `mixins: [ ... ]` array. |
| `rest-call` | `$niagara.<rest>()` or direct call to a `rest.js` export. |
| `box-call` | `$niagara.<box>()` or direct call to a `box.js` export. |
| `ws-call` | `websocket.<command>()` or `$niagara.ws.<command>()`. |
| `import-renamed` | Aliased import `{ X as Y }` — file captured, export-level approximate. |
| `dynamic-bind` | Vue `<component :is="dynamicName">` — symbolically inferred. |

---

## Coverage Thresholds (REQ-6)

| Kind | Floor | Source |
|------|------:|--------|
| `java-class` | 100% (77/77) | index.json |
| `store-module` | 100% (29-30) | index.json |
| `mixin` | 100% (18) | index.json |
| `plugin` | 100% (13) | index.json |
| `lib-utility` | 100% (10) | index.json |
| `rest-function` | 100% (synthetic, from api/rest.js) | api file |
| `box-method` | 100% (synthetic, from api/box.js) | api file |
| `ws-command` | 100% (synthetic, from api/websocket.js) | api file |
| `rest-url` | 100% (synthetic, URL literals) | api file |
| `vue-component` | ≥95% (≥360/378) | index.json |

---

## Reusability — Analytics / MX60 Extension Prototype

The schema is designed `core + extension`. For Analytics, add an extension block without touching core fields. Example for `algorithm-block` kind:

```json
{
  "id": "analytics/blocks/PIDController",
  "symbol": "PIDController",
  "kind": "algorithm-block",
  "defined_at": "analytics/src/blocks/PIDController.java",
  "used_at": [
    { "path": "analytics/dags/EnergyOptimizer.aon", "usage_kind": "dag-node" },
    { "path": "analytics/tests/PIDControllerTest.java", "usage_kind": "import" }
  ],
  "usage_count": 2,
  "unused": false,
  "analytics": {
    "block_type": "feedback-control",
    "aon_encoded": true,
    "io_pins": { "input": ["setpoint", "process"], "output": ["control"] }
  }
}
```

Analytics-specific extensions (NEW kinds):
- `algorithm-block` (Analytics DAG nodes — 64 in nominal install)
- `bql-query` (named BQL query strings)
- `aon-encoding` (binary encoding references)

Analytics-specific extension to `usage_kind`:
- `dag-node` (used as a node in an .aon graph)
- `io-bind` (input/output pin binding to another block)

The CORE shape (id, symbol, kind, defined_at, used_at, usage_count, unused) remains identical.

---

## Forward Compatibility Rules

- Adding a new `kind` value: minor bump (1.0 → 1.x). Consumers MUST treat unknown kinds as "ignore" rather than error.
- Adding a new `usage_kind` value: minor bump.
- Adding a top-level field to entries (e.g., extension blocks): minor bump.
- Removing or renaming any core field: major bump (1.x → 2.0).
- Changing the meaning of an existing enum value: major bump.

---

## Consumer Contract

A consumer using `jq` or another JSON tool against `xref.json` should:

1. Validate `schema_version` major matches its expectation.
2. Treat any unknown `kind` or `usage_kind` as opaque (filter out, don't error).
3. Use `id` to JOIN against `index.json` for the `kind` set covered by the mapping.
4. Treat `unused == true` as informational only — some symbols are reachable via dynamic patterns the static scan misses.

---

## Known Static-Analysis Limitations (Documented Non-Goals)

- **Dynamic component bindings** (`<component :is="...">`) — not statically resolvable; estimated <5% miss.
- **Aliased imports** (`{ X as Y }`) — file captured, symbol-level lost. Marked `import-renamed`.
- **Reflective Java loading** (Niagara framework loads via `module.xml`/`module-include.xml`) — Java classes referenced by Niagara reflection appear `unused` in xref. This is correct per scope (intra-Java edges only).
- **Transitive usage** — only direct usage is recorded. If A → B → C, A is not listed as a user of C.
