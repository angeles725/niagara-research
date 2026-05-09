# Tasks: mapping-cross-references

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~600–900 (new files only, source read-only) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single deliverable — all new output files |
| Delivery strategy | auto-chain |
| Chain strategy | N/A — documentation/research repo, no PR review cycle |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

> Note: budget rule is inapplicable. Source repo is read-only. All output is
> new files under `docs/mappings/reflow-clean-177/`. No deletions, no source edits.

### Suggested Work Units

| Unit | Goal | Notes |
|------|------|-------|
| 1 | Batch A shards + side-channel | Unblocks B3; must finish before B |
| 2 | Batch B + C shards (parallel) | Requires Unit 1 side-channel |
| 3 | Batch D: validate + merge + docs | Requires all shards complete |

---

## Phase 1: Batch A — Synthetic kinds (sequential, unblocks everything)

- [ ] A-1.1 Read `/home/cristian/niagara-research/src/api/rest.js` — extract all exported function names (target: 28).
- [ ] A-1.2 Read `/home/cristian/niagara-research/src/api/box.js` — extract all exported method names (target: 24, index.json authoritative).
- [ ] A-1.3 Read `/home/cristian/niagara-research/src/api/websocket.js` — extract all exported command names (target: 14; 3 are infra utilities).
- [ ] A-1.4 Run rg pattern `\$niagara\.<NAME>\(` + `this._vm\.\$niagara\.<NAME>\(` across `src/` for each rest-function name → build `rest-function` entries with `usage_kind: rest-call`.
- [ ] A-1.5 Same rg sweep for each box-method name → build `box-method` entries with `usage_kind: box-call`.
- [ ] A-1.6 Run rg `websocket\.<NAME>\(` + `\$niagara\.ws\.<NAME>\(` for each ws-command name → build `ws-command` entries with `usage_kind: ws-call`; mark infra commands (initSocket/disconnectSocket/getSocket) as expected-sparse.
- [ ] A-1.7 Run rg `'/nmodsreflow/<URL>'` across `src/` → build `rest-url` entries (28, most expected unused); `id` pattern: `api/rest.js#<url-slug>`.
- [ ] A-1.8 Emit `/tmp/xref-shards/rest-function.json`, `box-method.json`, `ws-command.json`, `rest-url.json` — each a JSON array sorted by `symbol`.
- [ ] A-1.9 Emit side-channel `/tmp/xref-shards/_niagara-methods.json`: `{ rest: [...names], box: [...names], ws: [...names] }`. Mirror content to engram as `sdd/mapping-cross-references/niagara-methods-sidecar`.

---

## Phase 2: Batch B — Java + small kinds (B1, B2, B3 run in parallel after Phase 1)

- [ ] B-2.1 (B1) Run rg `import com\.niagaramods\.nmodsreflow\.<FQN>;` + `extends|implements <SHORT>` across `src/` Java scope — build 77 `java-class` entries; `id` and `defined_at` sourced from `index.json`; `usage_kind: import | extends | implements`. Emit `/tmp/xref-shards/java-class.json`.
- [ ] B-2.2 (B2) Run rg `mapState|mapGetters|mapActions|mapMutations` + `dispatch('NAME/'` + `commit('NAME/'` across `src/` for each of 30 store-modules — build entries with matching `usage_kind`. Emit `/tmp/xref-shards/store-module.json`.
- [ ] B-2.3 (B2) Run rg `from '@/mixins/NAME'` + relative import + `mixins:[NAME]` across `src/` for each of 18 mixins — `usage_kind: import | mixin-ref`. Emit `/tmp/xref-shards/mixin.json`.
- [ ] B-2.4 (B3) Consume `/tmp/xref-shards/_niagara-methods.json`. Run rg `\$NAME\.` for each of 13 plugins; for `$niagara`, apply two-stage attribution per ADR-03: emit plugin `invoke` edges AND add `rest-call`/`box-call`/`ws-call` edges to respective function-kind shards if the method resolves. Emit `/tmp/xref-shards/plugin.json`.
- [ ] B-2.5 (B3) Run absolute + relative import rg for each of 10 lib-utilities — `usage_kind: import`. Emit `/tmp/xref-shards/lib-utility.json`.

---

## Phase 3: Batch C — Vue components (C1–C4 run in parallel after Phase 1)

Each task: run 4 rg patterns per component (PascalCase template `<NAME[\s/>]`, kebab-case template, relative import, absolute import). `usage_kind: template | import | import-renamed`. `id` and `defined_at` from `index.json`. Emit one shard per sub-batch.

- [ ] C-3.1 (C1) Equipment + dashboard + buildings + config families (~100 components). Emit `/tmp/xref-shards/vue-c1.json`.
- [ ] C-3.2 (C2) Floorplans family (52 components). Emit `/tmp/xref-shards/vue-c2.json`.
- [ ] C-3.3 (C3) Alarms + history + cards + charts families (~73 components). Emit `/tmp/xref-shards/vue-c3.json`.
- [ ] C-3.4 (C4) Common + navigation + layout + map + maps + pages + points + profiles + schedules + settings + weather + websocket + wizard + browser + views families (~150 components). Emit `/tmp/xref-shards/vue-c4.json`.

---

## Phase 4: Batch D — Validate, merge, docs (sequential after Phases 2–3)

- [ ] D-4.1 Parse-validate every shard: `jq -e . /tmp/xref-shards/*.json` — all must exit 0. Report any that fail.
- [ ] D-4.2 Per-shard count check: verify entry count per kind matches REQ-6 targets (java-class 77, store-module 30, mixin 18, plugin 13, lib-utility 10, rest-function 28, box-method 24, ws-command 14, rest-url 28; vue total ≥ 360).
- [ ] D-4.3 `jq -s 'add'` all shards into merged array; wrap in envelope `{schema_version:"1.0", xref_for, generated_at, total_entries, entries: sort_by(.kind,.symbol)}`. Write to `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.json`.
- [ ] D-4.4 Validate merged `xref.json`: `jq -e .` exit 0; `total_entries == length(entries)`; all 7 entry fields present; `usage_count == length(used_at)` for every entry; `unused == (usage_count == 0)` for every entry (REQ-8, 100% of entries).
- [ ] D-4.5 Cross-validate REQ-4: for non-synthetic kinds, every `id` ∈ `index.json`. For synthetic kinds, every `id` matches `{source-file-stem}#{symbol-name}` and is unique.
- [ ] D-4.6 Cross-validate REQ-5: for non-synthetic kinds, `defined_at` equals `path` from matching `index.json` entry.
- [ ] D-4.7 Spot-check 30 entries (3 per kind × 10 kinds): for each, run the kind's rg pattern manually and confirm result count ≈ entry's `usage_count`. Document discrepancies.
- [ ] D-4.8 Coverage report (REQ-6): for each kind, verify every relevant `index.json` id has a corresponding xref entry; list any missing ids as CRITICAL gaps.
- [ ] D-4.9 Generate `xref.md` via bash + jq heredoc — sections: per-kind summary table (count/edges/unused), Top Consumers top-5 per kind, Most-used overall top-20. Write to `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref.md`.
- [ ] D-4.10 Generate `xref-schema.md` — document `schema_version:"1.0"`, all 7 entry fields with types and constraints, `usage_kind` enum (17 values), extension mechanism, Analytics extension prototype with `algorithm-block` kind and sample id `analytics/algorithm#PsychrometricCalculator`. Write to `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref-schema.md`.
- [ ] D-4.11 Generate `xref-README.md` — rg + jq usage examples (find all callers, list unused by kind, query by id). Write to `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/xref-README.md`.
- [ ] D-4.12 Save apply-progress to engram: topic `sdd/mapping-cross-references/apply-progress` with checklist status and output file paths.

---

## Dependency Map

```
Phase 1 (A)
    └─→ Phase 2 B1, B2 (parallel)
    └─→ Phase 2 B3 (needs A side-channel)
    └─→ Phase 3 C1, C2, C3, C4 (parallel)
              └─→ Phase 4 (D) — all shards complete
```

## Spec → Task Traceability

| REQ | Tasks |
|-----|-------|
| REQ-1 (schema fields) | A-1.4–A-1.9, B-2.1–B-2.5, C-3.1–C-3.4, D-4.3 |
| REQ-2 (kind enum) | A-1.4–A-1.9, B-2.1–B-2.5, C-3.1–C-3.4, D-4.1 |
| REQ-3 (usage_kind enum) | A-1.4–A-1.9, B-2.1–B-2.5, C-3.1–C-3.4, D-4.4 |
| REQ-4 (id join) | D-4.5 |
| REQ-5 (defined_at) | D-4.6 |
| REQ-6 (coverage) | D-4.2, D-4.8 |
| REQ-7 (JSON validity + xref.md) | D-4.1, D-4.9 |
| REQ-8 (unused invariant) | D-4.4 |
| REQ-9 ($niagara disambiguation) | A-1.9, B-2.4 |
| REQ-10 (schema doc) | D-4.10 |
