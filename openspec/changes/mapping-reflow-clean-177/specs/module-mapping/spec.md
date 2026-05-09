# module-mapping Specification

**Change**: mapping-reflow-clean-177
**Capability**: module-mapping (NEW)
**Schema version**: v1
**Date**: 2026-05-09

---

## Purpose

Define the complete behavioral contract for the `module-mapping` capability: a versioned,
queryable artifact that maps every non-excluded file in a Niagara/Reflow module to a
machine-readable and human-readable record. This spec is the authoritative contract for
both Tier 1 (index) and Tier 2 (domain deep dives) deliverables.

---

## Requirements

---

### Requirement: REQ-1 — Core schema fields mandatory for every mapped entry

Every entry in the mapping index MUST have the following fields populated with a non-null,
non-empty value: `path`, `kind`, `domain`, `purpose`, `loc`, `status`. The field
`dependencies` MUST be present in every entry and MAY be an empty array. Any entry
missing a required core field SHALL cause the overall mapping to be considered incomplete
and MUST NOT be committed.

`kind` MUST be one of the following enum values:
`java-class`, `vue-component`, `js-module`, `js-plugin`, `js-mixin`, `js-store`,
`js-api`, `js-lib`, `js-router`, `config`, `resource`.

`status` MUST be one of: `source`, `compiled`, `bundle`.

#### Scenario: Happy path — well-formed entry

- GIVEN a mapped file entry for any file in the repository
- WHEN the entry is validated against the core schema
- THEN `path`, `kind`, `domain`, `purpose`, `loc`, `status` are all present and non-empty
- AND `dependencies` is present (may be `[]`)
- AND `kind` value matches one of the allowed enum values
- AND `status` value matches one of `source`, `compiled`, `bundle`

#### Scenario: Edge case — binary asset entry

- GIVEN a JPG file from `nmodsreflow-rt/src/image-library/`
- WHEN it is included in the mapping
- THEN `kind` is `resource`, `status` is `resource`, `loc` is `0`, `purpose` describes the asset type
- AND `dependencies` is `[]`

#### Scenario: Error — missing required field

- GIVEN a mapping entry where `purpose` is null or absent
- WHEN the index is validated
- THEN validation reports the entry as malformed
- AND the entry is flagged before the index is committed

---

### Requirement: REQ-2 — Backend extension block mandatory for Java entries

Every entry with `kind: java-class` MUST also carry a backend extension block containing:
`profile` (enum: `rt` | `ux`), `package` (string), `bcomponent_type` (string or null),
`slots` (integer or null), `actions` (array, MAY be empty), `decompiled` (boolean).

Additionally:
- Entries whose `path` contains `http/responses/` MUST include `rest_endpoints` (string array).
- Entries whose `path` contains `commands/` MUST include `box_methods` (string array).

#### Scenario: Happy path — BComponent service class

- GIVEN the entry for `BReflowService.java`
- WHEN the backend extension block is validated
- THEN `profile` is `rt`, `package` is `com.niagaramods.nmodsreflow`
- AND `bcomponent_type` is `BComponent`, `slots` is `26`
- AND `actions` is a non-empty array of declared `@NiagaraAction` names
- AND `decompiled` is `false`

#### Scenario: Happy path — response handler class

- GIVEN an entry for a class under `http/responses/` (e.g., a history response handler)
- WHEN the backend extension block is validated
- THEN `rest_endpoints` is present and contains at least one endpoint string
- AND all other backend extension fields are present

#### Scenario: Happy path — BOX command class

- GIVEN an entry for a class under `commands/` (e.g., `BReflowAlarmCommands.java`)
- WHEN the backend extension block is validated
- THEN `box_methods` is present and contains at least one method name
- AND `profile` is `rt`

#### Scenario: Happy path — decompiled UX class

- GIVEN the entry for `BReflowScheme.java` or any `nmodsreflow-ux` class
- WHEN the backend extension block is validated
- THEN `decompiled` is `true`
- AND `profile` is `ux` for `nmodsreflow-ux` classes

#### Scenario: Error — java-class entry missing backend block

- GIVEN an entry with `kind: java-class` that has no backend extension block
- WHEN the index is validated
- THEN validation reports a schema violation for that entry

---

### Requirement: REQ-3 — Frontend extension block mandatory for Vue/JS entries

Every entry with `kind: vue-component` MUST include `component_dir` (string),
`store_modules` (array, MAY be empty), `plugins_used` (array, MAY be empty).

Additionally:
- Entries for view files (files under `views/`) MUST include `route_name` (string).
- Entries with `kind: js-store` MUST include `persistent` (boolean).

#### Scenario: Happy path — standard Vue component

- GIVEN the entry for a component under `components/equipment/`
- WHEN the frontend extension block is validated
- THEN `component_dir` is `equipment`
- AND `store_modules` and `plugins_used` are present (may be `[]`)

#### Scenario: Happy path — view file

- GIVEN the entry for a file under `views/` (e.g., `views/Home.vue`)
- WHEN the frontend extension block is validated
- THEN `route_name` is present and matches a named route in `router/index.js`

#### Scenario: Happy path — Vuex store module

- GIVEN the entry for a file with `kind: js-store`
- WHEN the frontend extension block is validated
- THEN `persistent` is `true` for modules included in `config.json` serialization
- AND `persistent` is `false` for transient modules (15 of 29 total)

#### Scenario: Error — vue-component missing frontend block

- GIVEN an entry with `kind: vue-component` that has no frontend extension fields
- WHEN the index is validated
- THEN validation reports a schema violation for that entry

---

### Requirement: REQ-4 — Dual-form index: human-readable MD and machine-readable JSON

The mapping MUST exist in BOTH of the following forms, co-located in
`docs/mappings/reflow-clean-177/`:

1. `index.md` — human-readable flat table, sorted ascending by `path`, one row per entry.
2. `index.json` — valid JSON array, parseable by `jq` v1.6+, one object per entry.

`index.json` MUST contain all core fields for every entry. Extension blocks MAY be
present inline or referenced via domain files; the JSON MUST be self-consistent.
`index.md` MUST render correctly in standard Markdown viewers (no broken table syntax).

#### Scenario: Happy path — jq query on history domain

- GIVEN `index.json` at `docs/mappings/reflow-clean-177/index.json`
- WHEN the query `jq '.[] | select(.domain=="history")' index.json` is executed
- THEN it returns without parse error
- AND the result contains ≥12 entries with `kind: java-class`
- AND the result contains ≥22 entries with `kind: vue-component`

#### Scenario: Happy path — index.md table integrity

- GIVEN `index.md` in the same directory
- WHEN a Markdown table parser processes the file
- THEN every row has the same number of columns as the header
- AND rows are sorted by the `path` column in ascending order

#### Scenario: Edge case — empty domain query

- GIVEN a valid `jq` query on a domain with only compiled/bundle entries
- WHEN executed against `index.json`
- THEN it returns an empty array `[]` without error (not a parse failure)

---

### Requirement: REQ-5 — Schema is documented and versioned in schema.md

`docs/mappings/reflow-clean-177/schema.md` MUST define the full `mapping-schema v1`
contract. It MUST contain:

- All core field names, their types, and allowed enum values for `kind` and `status`.
- A clear separation between **core fields** (universal, apply to every module) and
  **extension blocks** (per-codebase-type: `backend`, `frontend_vue`).
- A concrete reference example showing how an Analytics or MX60 module would ADD a new
  extension block (e.g., `analytics-extension` with fields `algorithm_block_type`,
  `dag_role`, `aon_encoded`) WITHOUT modifying the core schema.
- A `schema_version: v1` identifier at the top of the document.
- The list of allowed `kind` enum values and the list of allowed `status` enum values.

#### Scenario: Happy path — schema document present and complete

- GIVEN `schema.md` in `docs/mappings/reflow-clean-177/`
- WHEN a reviewer reads it
- THEN `schema_version: v1` appears at the top
- AND every core field from REQ-1 is defined with name, type, and constraints
- AND the backend extension block and frontend_vue extension block are fully listed
- AND an example analytics extension block is present

#### Scenario: Happy path — new module reuses schema

- GIVEN an engineer starting a mapping for the Analytics module
- WHEN they read `schema.md`
- THEN they can define a new extension block without touching core field definitions
- AND the example demonstrates the exact structure to follow

---

### Requirement: REQ-6 — Domain documents follow a fixed 5-section template

Every file matching `docs/mappings/reflow-clean-177/domains/<name>.md` MUST contain
exactly the following 5 sections in this order:

1. **Overview** — exactly 1 paragraph describing the domain.
2. **Entry points** — a Markdown table listing files that bootstrap or anchor the domain.
3. **Components / classes** — a full file list with extension fields populated.
4. **Cross-references** — links or references to files in other domains that this domain
   depends on or is depended upon by.
5. **Notes & gotchas** — known issues, fidelity ratings, decompile caveats, or any
   gap analysis flags relevant to the domain.

A domain doc that is missing any of these 5 sections SHALL be considered incomplete.
Section order MUST NOT be changed.

#### Scenario: Happy path — backend domain doc

- GIVEN `domains/backend.md`
- WHEN its structure is validated
- THEN it contains Overview, Entry points, Components / classes, Cross-references,
  Notes & gotchas — in that order
- AND "Entry points" table includes `BReflowService.java` and `BaseServlet.java`
- AND "Components / classes" lists entries for all 77 Java files

#### Scenario: Happy path — cross-stack domain (e.g., alarms)

- GIVEN `domains/alarms.md`
- WHEN its structure is validated
- THEN "Cross-references" lists both backend Java classes and frontend Vue components
  that participate in the alarms domain
- AND "Notes & gotchas" references GAP-ANALYSIS fidelity rating for the alarms area

#### Scenario: Error — domain doc missing a required section

- GIVEN a domain doc that has Overview, Entry points, and Components but is missing
  Cross-references and Notes & gotchas
- WHEN the doc is validated
- THEN it is flagged as non-conformant and MUST NOT be merged until corrected

---

### Requirement: REQ-7 — Coverage ≥95% of in-scope source files; spot-check fidelity ≥90%

The mapping MUST cover ≥95% of all files in the source repository, excluding only:
- `reflow-frontend/node_modules/` (third-party dependencies)
- `nmodsreflow-rt/src/rc/` compiled bundle output
- `nmodsreflow-ux/build/` build artifacts
- Binary image assets: `nmodsreflow-rt/src/image-library/` (22 JPG) and
  `nmodsreflow-rt/src/icons/` (6 PNG) — these MUST be catalogued with `status: resource`
  but MAY have minimal metadata (`purpose` = asset description, `loc` = 0)

A spot-check of ≥40 entries (5 per domain × 8 domains) MUST achieve ≥90% accuracy on the
`purpose` field when compared against the actual file source content. Accuracy is measured
as: the `purpose` string correctly describes the file's primary responsibility without
contradiction.

#### Scenario: Happy path — full coverage check

- GIVEN the completed `index.json`
- WHEN file count from `fd --exclude node_modules --exclude src/rc` is compared to entry count
- THEN the ratio (entries / total files) is ≥0.95
- AND every excluded path is explicitly documented (see REQ-10)

#### Scenario: Happy path — spot-check passes

- GIVEN a random sample of 5 entries from each of the 8 domains (40 total)
- WHEN each entry's `purpose` field is verified against the actual source file
- THEN ≥36 of 40 entries have a purpose description that matches the file's primary role
- AND any entry scoring POOR fidelity is flagged in the domain's Notes & gotchas section

#### Scenario: Edge case — spot-check failure

- GIVEN a domain where fewer than 90% of spot-checked entries pass fidelity
- WHEN the failure is detected during verification
- THEN the domain MUST be re-processed before the index is committed
- AND the verification report records which domain failed and how many entries were wrong

---

### Requirement: REQ-8 — Source-doc cross-references for synthesized entries

Every mapping entry whose `purpose`, extension fields, or metadata was synthesized from
`REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md`, or `NIAGARA-INTEGRATION.md` MUST
include a `source_doc` field referencing the document name and the heading/section used.

Spot-checked entries (per REQ-7 sample) MUST additionally include a `verified_at` field
containing an ISO-8601 timestamp recording when the entry was verified against source.

#### Scenario: Happy path — synthesized backend entry

- GIVEN the entry for `BReflowService.java`
- WHEN its fields are inspected
- THEN `source_doc` contains `"REFLOW-ARCHITECTURE-ANALYSIS.md#BReflowService"` or equivalent
- AND `slots: 26` is traceable to that document

#### Scenario: Happy path — spot-checked entry has verified_at

- GIVEN any of the 40 spot-check entries from REQ-7
- WHEN the entry is read from `index.json`
- THEN `verified_at` is present and contains a valid ISO-8601 datetime string

#### Scenario: Edge case — entry derived from direct file read (not synthesis)

- GIVEN an entry whose data was read directly from source (not from the 3 docs)
- WHEN it is inspected
- THEN `source_doc` MAY be absent or set to `"direct"` — both are acceptable
- AND `verified_at` MUST still be present if the entry was part of the spot-check sample

---

### Requirement: REQ-9 — README contains usage examples and extension instructions

`docs/mappings/reflow-clean-177/README.md` MUST contain:

- At least 3 example `rg` queries demonstrating how to search the mapping files
  (e.g., find all REST endpoints, find all decompiled classes, find files in a domain).
- At least 3 example `jq` queries against `index.json`
  (e.g., filter by domain, count by kind, list all persistent store modules).
- Step-by-step instructions for extending the schema for a new module
  (add extension block, register in schema.md, add entries).
- A pointer to `schema.md` for the full schema reference.

#### Scenario: Happy path — README is self-contained

- GIVEN a developer encountering the mapping for the first time
- WHEN they read `README.md`
- THEN they can execute at least one `rg` and one `jq` query against real files
  without reading any other document
- AND they can follow the extension instructions to add a new module mapping

#### Scenario: Happy path — jq query examples are syntactically valid

- GIVEN the 3+ `jq` examples in README.md
- WHEN each is executed against the real `index.json`
- THEN each returns a result without a jq parse error

#### Scenario: Edge case — README references schema.md

- GIVEN the README
- WHEN it is parsed
- THEN it contains at least one link or reference to `schema.md` in the same directory

---

### Requirement: REQ-10 — Excluded paths are explicitly listed with reasons

`schema.md` OR `index.md` MUST contain a dedicated section titled **Excluded Paths**
listing every directory/pattern that was excluded from the mapping, with a one-line
reason for each exclusion.

Required exclusions that MUST appear:

| Path | Reason |
|------|--------|
| `reflow-frontend/node_modules/` | Third-party dependencies; not project source |
| `nmodsreflow-rt/src/rc/` | Compiled webpack bundle output; not editable source |
| `nmodsreflow-ux/build/` | Build artifacts (.jar, .class, bajadoc); not editable source |
| `nmodsreflow-rt/src/image-library/` | Binary JPG assets; catalogued with status: resource |
| `nmodsreflow-rt/src/icons/` | Binary PNG assets; catalogued with status: resource |

Additional exclusions discovered during mapping MUST be appended to this list.

#### Scenario: Happy path — exclusion list is present and complete

- GIVEN the completed `schema.md` or `index.md`
- WHEN the Excluded Paths section is read
- THEN all 5 required entries from the table above are present with reasons
- AND any additional exclusion found during mapping is also listed

#### Scenario: Edge case — a new exclusion is discovered during Tier 2 deep dives

- GIVEN a sub-agent encounters an unexpected build artifact or binary directory
- WHEN it adds the path to its domain doc's Notes section
- THEN the path MUST also be appended to the central Excluded Paths list in schema.md or index.md
  before the mapping is considered complete
