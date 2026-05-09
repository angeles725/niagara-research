# module-xrefs Specification
## Change: mapping-cross-references

## Purpose

Defines the behavioral contract for the canonical cross-reference artifact
(`xref.json` + companions) that answers "who uses this symbol" for every
entry in `docs/mappings/reflow-clean-177/index.json`.

---

## Requirements

### Requirement: REQ-1 — Core schema fields

Every entry in `xref.json` MUST contain: `id`, `symbol`, `kind`,
`defined_at`, `used_at` (array, MAY be empty), `usage_count` (integer ≥ 0),
and `unused` (boolean). The top-level envelope MUST contain:
`schema_version`, `xref_for`, `generated_at`, and `total_entries`.

#### Scenario: Entry with callers

- GIVEN a symbol that is imported by two files
- WHEN the entry is read from `xref.json`
- THEN `used_at` has exactly 2 elements, `usage_count == 2`, `unused == false`

#### Scenario: Orphan entry

- GIVEN a symbol with no known callers
- WHEN the entry is read from `xref.json`
- THEN `used_at` is `[]`, `usage_count == 0`, `unused == true`

#### Scenario: Missing envelope field

- GIVEN an `xref.json` that omits `schema_version`
- WHEN verification runs
- THEN the verify phase MUST report a CRITICAL failure

---

### Requirement: REQ-2 — Kind enum closed set

The `kind` field MUST be exactly one of the 10 values:
`java-class`, `rest-function`, `box-method`, `ws-command`, `vue-component`,
`store-module`, `mixin`, `plugin`, `lib-utility`, `rest-url`.
No other value is permitted.

#### Scenario: Valid kind

- GIVEN an entry with `kind: "store-module"`
- WHEN the file is validated
- THEN validation passes without error

#### Scenario: Invalid kind

- GIVEN an entry with `kind: "bql-query"` (Analytics extension, not in Reflow set)
- WHEN the file is validated
- THEN verification MUST report a CRITICAL failure naming the invalid entry

---

### Requirement: REQ-3 — usage_kind enum closed set

Each element of `used_at[]` MUST contain a `usage_kind` field that is exactly
one of the 17 values: `import`, `extends`, `implements`, `invoke`, `inject`,
`map-state`, `map-getter`, `map-action`, `dispatch`, `commit`, `template`,
`mixin-ref`, `rest-call`, `box-call`, `ws-call`, `import-renamed`,
`dynamic-bind`. No other value is permitted.

#### Scenario: Valid usage_kind

- GIVEN `used_at[0].usage_kind == "dispatch"`
- WHEN validation runs
- THEN no error is raised

#### Scenario: Invalid usage_kind

- GIVEN `used_at[0].usage_kind == "require"` (not in enum)
- WHEN validation runs
- THEN verification MUST report a CRITICAL failure naming the entry and offending value

---

### Requirement: REQ-4 — id join with index.json

For every entry whose `kind` is in
`{java-class, vue-component, store-module, mixin, plugin, lib-utility}`,
the `id` field MUST exactly match an `id` present in the sibling
`docs/mappings/reflow-clean-177/index.json`.

For entries whose `kind` is in
`{rest-function, box-method, ws-command, rest-url}` (synthetic kinds),
the `id` MUST be unique within `xref.json` and MUST follow the pattern
`{source-file-stem}#{symbol-name}` (e.g., `api/rest.js#getConfig`).

#### Scenario: Non-synthetic id matches index

- GIVEN a `vue-component` entry with `id: "Vue_DashboardCard"`
- WHEN `xref.json` is loaded alongside `index.json`
- THEN `jq` finds an entry in `index.json` where `.id == "Vue_DashboardCard"`

#### Scenario: Synthetic id is unique

- GIVEN two entries with `kind: "rest-function"` and `kind: "rest-url"`
  both derived from `api/rest.js`
- WHEN `xref.json` is loaded
- THEN each entry has a distinct `id`; no two entries share the same `id`

#### Scenario: Non-synthetic id absent from index

- GIVEN a `mixin` entry with an `id` not present in `index.json`
- WHEN verification runs
- THEN verification MUST report a CRITICAL failure

---

### Requirement: REQ-5 — defined_at consistency with index.json

For all non-synthetic kinds, the `defined_at` field MUST equal the `path`
value of the matching `index.json` entry for the same `id`.

#### Scenario: defined_at matches

- GIVEN `id: "Vuex_config"` maps to `path: "store/modules/config.js"` in index.json
- WHEN the xref entry is read
- THEN `defined_at == "store/modules/config.js"`

#### Scenario: defined_at drift

- GIVEN a `store-module` entry where `defined_at` differs from the index path
- WHEN verification runs
- THEN verification MUST report a CRITICAL failure with both expected and actual values

---

### Requirement: REQ-6 — Coverage thresholds

The following symbol counts MUST appear in `xref.json`
(whether used or unused):

| kind           | Required count |
|----------------|----------------|
| java-class     | 77 (all)       |
| store-module   | 30 (all)       |
| mixin          | 18 (all)       |
| plugin         | 13 (all)       |
| lib-utility    | 10 (all)       |
| rest-function  | 28 (all)       |
| box-method     | 21 (all)       |
| ws-command     | 11 (all)       |
| rest-url       | 28 (all)       |
| vue-component  | ≥ 360 (≥ 95% of 378) |

#### Scenario: Full java-class coverage

- GIVEN `index.json` contains 77 java-class entries
- WHEN `xref.json` is generated and filtered by `kind == "java-class"`
- THEN the result contains exactly 77 entries

#### Scenario: vue-component partial coverage

- GIVEN `index.json` contains 378 vue-component entries
- WHEN `xref.json` is generated and filtered by `kind == "vue-component"`
- THEN the result contains at least 360 entries

#### Scenario: Missing store-module

- GIVEN one store-module is absent from `xref.json`
- WHEN verification runs
- THEN verification MUST report a CRITICAL failure listing the missing id

---

### Requirement: REQ-7 — JSON validity and machine-queryability

`xref.json` MUST parse without error via `jq -e .`.
`xref.md` MUST be present and MUST contain:
a per-kind summary table (count, edge count, unused count)
and a "top consumers" section listing the top-5 used symbols per kind.

#### Scenario: Valid JSON

- GIVEN the generated `xref.json`
- WHEN `jq -e . xref.json` is executed
- THEN the command exits with code 0

#### Scenario: xref.md structure

- GIVEN the generated `xref.md`
- WHEN it is read
- THEN it contains at least one markdown table with columns for kind, count, and edges,
  AND a section titled "Top Consumers" with at least 5 entries per kind

---

### Requirement: REQ-8 — unused field correctness

`unused` MUST equal `true` if and only if `usage_count == 0`
AND `length(used_at) == 0`. The verify phase MUST check this invariant
for 100% of entries — no sampling.

#### Scenario: Invariant holds — zero callers

- GIVEN `usage_count == 0` and `used_at == []`
- WHEN verification evaluates the entry
- THEN `unused == true` is confirmed

#### Scenario: Invariant holds — non-zero callers

- GIVEN `usage_count == 3` and `used_at` has 3 elements
- WHEN verification evaluates the entry
- THEN `unused == false` is confirmed

#### Scenario: Invariant violated — count mismatch

- GIVEN `usage_count == 2` but `used_at` has 3 elements
- WHEN verification evaluates the entry
- THEN verification MUST report a CRITICAL failure naming the entry and the mismatch

#### Scenario: Invariant violated — flag inversion

- GIVEN `usage_count == 0` but `unused == false`
- WHEN verification evaluates the entry
- THEN verification MUST report a CRITICAL failure

---

### Requirement: REQ-9 — $niagara plugin disambiguation

The xref entry for plugin `$niagara` MAY include all occurrences in `used_at`
regardless of whether the call is REST, BOX, or WS.
The individual `rest-function`, `box-method`, and `ws-command` entries MUST
each list their direct callers, attributed by discriminating the method name
against the exports of `api/rest.js`, `api/box.js`, and `api/websocket.js`
respectively. The same file MAY appear in both the `$niagara` plugin entry
AND a specific `rest-function` / `box-method` / `ws-command` entry.

#### Scenario: REST call attribution

- GIVEN a Vue component calls `this.$niagara.getConfig()`
  AND `getConfig` is an export of `api/rest.js`
- WHEN xref is built
- THEN the component's path appears in the `rest-function#getConfig` entry's `used_at`
  with `usage_kind: "rest-call"`,
  AND it MAY also appear in the `$niagara` plugin entry's `used_at`

#### Scenario: BOX call attribution

- GIVEN a store module calls `this.$niagara.historyGetData()`
  AND `historyGetData` is an export of `api/box.js`
- WHEN xref is built
- THEN the module's path appears in the `box-method#historyGetData` entry with
  `usage_kind: "box-call"`, NOT as a `rest-call`

#### Scenario: Undiscriminated call

- GIVEN a call site pattern matches `$niagara.*` but the method name is not found
  in any of `rest.js`, `box.js`, or `websocket.js` exports
- WHEN xref is built
- THEN the occurrence is attributed to the `$niagara` plugin entry only,
  with `usage_kind: "invoke"`, and MUST NOT be silently dropped

---

### Requirement: REQ-10 — Schema versioning and reusability documentation

`xref-schema.md` MUST be present at
`docs/mappings/reflow-clean-177/xref-schema.md`.
It MUST declare `schema_version: "1.0"`, document all core fields and their
types, define the extension mechanism (additional `kind` and `usage_kind`
values per codebase), and include a concrete Analytics extension prototype
that introduces the `algorithm-block` kind.

#### Scenario: Analytics extension prototype readable

- GIVEN `xref-schema.md` is read
- WHEN the "Analytics Extension" section is located
- THEN it contains an `algorithm-block` kind definition with at least one
  `usage_kind` (e.g., `box-call`) and a sample `id` pattern
  (e.g., `analytics/algorithm#PsychrometricCalculator`)

#### Scenario: Schema version present

- GIVEN `xref-schema.md` is read
- WHEN the document is scanned for `schema_version`
- THEN the value `"1.0"` is found

#### Scenario: Core field table complete

- GIVEN `xref-schema.md` is read
- WHEN the core fields section is checked
- THEN all 7 entry-level fields (`id`, `symbol`, `kind`, `defined_at`,
  `used_at`, `usage_count`, `unused`) are documented with their types
  and constraints
