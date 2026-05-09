# Proposal — mapping-cross-references (archived)

**Phase**: sdd-propose
**Date**: 2026-05-09 (archived)
**Change**: mapping-cross-references

---

## Executive Summary

Proposed a canonical, queryable cross-reference graph for Reflow-Clean-177: `xref.json` with 615 entries and 1482 edges. Uses per-kind batched ripgrep producers → JSON shards → jq merge. Schema is core + extension-reusable for Analytics/MX60. No new tooling; proven pipeline from mapping-reflow-clean-177 SDD.

---

## Problem Statement

The mapping SDD (mapping-reflow-clean-177) produced a 547-entry symbol catalog (`index.json`). Complementary need: "Who uses each symbol?" — historically reconstructed ad-hoc via ripgrep. Solution: persistent, queryable xref artifact indexed by symbol → used_at[].

---

## Deliverables

**Output location**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`

| File | Role |
|------|------|
| xref.json | Canonical graph, 615 entries, 1482 edges |
| xref.md | Human summary: top consumers, unused symbols |
| xref-schema.md | Schema documentation + Analytics extension prototype |
| xref-README.md | Quick-start with jq/rg recipes |

---

## Schema (Locked)

**Per-entry**:
- id, symbol, kind (10-value enum), defined_at
- used_at[] with path, usage_kind (17-value enum)
- usage_count (computed), unused (boolean)

**Top-level envelope**:
- schema_version: "1.0", xref_for, generated_at, total_entries, entries[]

**10 kinds**: java-class, vue-component, store-module, mixin, plugin, lib-utility, rest-function, box-method, ws-command, rest-url

**17 usage_kinds**: import, import-renamed, extends, implements, invoke, inject, map-state, map-getter, map-action, dispatch, commit, template, mixin-ref, rest-call, box-call, ws-call, dynamic-bind

---

## Pipeline (Locked)

**Approach**: Per-kind batched sub-agents → JSON shards → jq merge → xref.json + derivatives.

**Batches**:
1. Batch A (1 sa): rest-function, box-method, ws-command, rest-url + side-channel
2. Batch B (3 parallel): java-class (B1), store-module+mixin (B2), plugin+lib (B3)
3. Batch C (4 parallel): vue-component split by dir family
4. Batch D (inline): merge, validate, xref.md, coverage report

**Sub-agent budget**: sonnet, ≤80 symbols per shard.

**Effort estimate**: ~1.5 h wall-clock with parallelism.

---

## Non-Goals & Risks

**Non-goals**:
- NO AST/symbol-perfect resolution
- NO alias precision (import { X as Y })
- NO dynamic bindings (<component :is>)
- NO transitive usage
- NO external symbols

**Risks** (12 tracked):
1. False positives from HTML comments/strings
2. Renamed imports lose fidelity
3. $niagara collisions (none today; audited)
4. Vue dynamic binding missed
5. Vue-component shard split drift
6. Shard JSON parse failure
7. Inner Java classes mis-treated
8. rest-url false positives
9. Side-channel loss (mirrored to engram)
10. Self-references inflate count
11. Ambiguous method names
12. Schema drift (versioned to 1.0)

---

## Reusability for Analytics/MX60

**Core** (any Java+Vue codebase):
- Kinds: java-class, vue-component, store-module, mixin, plugin, lib-utility
- Schema fields: id, symbol, kind, defined_at, used_at, usage_count, unused

**Extensions per codebase**:
- Reflow: rest-function, rest-url, box-method, ws-command
- Analytics: algorithm-block, bql-query, box-method
- MX60: TBD per codebase

---

## Approval Criteria (Locked)

- Schema (10 kinds, 17 usage_kinds) finalized
- Output path confirmed: docs/mappings/reflow-clean-177/
- Pipeline: 14 sub-agents sonnet, jq merge, ~1.5h
- Out-of-scope list accepted

---

**Status**: ARCHIVED. For details, see full proposal in engram.
