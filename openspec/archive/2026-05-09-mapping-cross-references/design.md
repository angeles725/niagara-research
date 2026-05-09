# Design — mapping-cross-references (archived)

**Status**: ARCHIVED
**Change**: mapping-cross-references
**Phase**: sdd-design → sdd-archive
**Date**: 2026-05-09

---

## Summary

The design locked the per-kind ripgrep producer pattern, jq merge contract, and validation strategy for the xref pipeline. All 10 symbol kinds defined per-kind patterns with expected coverage, risk mitigations, and ADR decisions. Pipeline proved reusable across Analytics/MX60 extension model.

Note: This archived copy contains the finalized design. For detailed implementation, refer to the full design artifact stored in engram (id #1224) and the apply/verify reports (ids #1227–#1228).

---

## Key Architecture Decisions (ADR-01 through ADR-07)

1. **ADR-01**: Sibling JSON file with id-join (xref.json next to index.json)
2. **ADR-02**: Per-kind ripgrep producers, no AST
3. **ADR-03**: $niagara two-stage attribution (plugin + method-level)
4. **ADR-04**: Vue-component split into 4–5 sub-shards by component_dir
5. **ADR-05**: Side-channel _niagara-methods.json between Batch A and B3
6. **ADR-06**: Deterministic sort: (kind, symbol) at top level
7. **ADR-07**: Shard format as JSON array; jq -s 'add' merge

---

## Per-Kind Patterns Summary

| Kind | Count | Search Root | Patterns | Notes |
|------|-------|-------------|----------|-------|
| java-class | 77 | nmodsreflow/ | import FQN; extends/implements SHORT | Intra-Java only |
| rest-function | 28 | reflow-frontend/src/ | $niagara.NAME, direct import | ~3 callers/function |
| box-method | 24 | reflow-frontend/src/ | $niagara.NAME | File authoritative, not proposal |
| ws-command | 14 | reflow-frontend/src/ | websocket.NAME, $niagara.ws.NAME | 3 infra utilities expected sparse |
| vue-component | 378 | reflow-frontend/src/ | 4 patterns per component (PascalCase/kebab/import) | Split into 4–5 sub-shards |
| store-module | 30 | reflow-frontend/src/ | mapState/Getters/Actions/Mutations, dispatch/commit | Highest consumer density |
| mixin | 18 | reflow-frontend/src/ | @/mixins import, mixins: [] | Includes profileMixin co-located |
| plugin | 13 | reflow-frontend/src/ | $NAME.method | Records method sub-field |
| lib-utility | 10 | reflow-frontend/src/ | @/lib import | Renamed imports marked import-renamed |
| rest-url | 28 | reflow-frontend/src/ | '/nmodsreflow/URL' patterns | Most expected unused |

---

## $niagara Disambiguation

Two-stage attribution: plugin shard records invoke + method field; rest-function/box-method/ws-command shards receive method-level entries via side-channel _niagara-methods.json. Avoids circular dependency, enables dual perspectives (plugin consumer vs. method caller).

---

## Validation & Risks

**Per-shard validation**: all 7 core fields present, usage_count matches length(used_at), unused invariant holds, usage_kind in enum.

**Cross-shard validation**: xref ids exist in index.json; coverage check that all relevant index.json ids have xref entries.

**Spot-check**: 30 random entries (3/kind); rg patterns re-run manually to confirm.

**Top 12 risks** tracked with mitigations (false positives, aliases, schema drift, self-references, shard JSON parse failure, side-channel loss, etc.).

---

## Reusability for MX60/Analytics

Stages 0/2/3/4 kind-agnostic. Batch A patterns reusable with path-root change. Reflow-specific kinds (rest/box/ws/url) become optional. Schema additions (algorithm-block, bql-query) forward-compatible via new kind enum values.

---

**For full implementation details and per-pattern specifications, see engram artifact id #1224.**
