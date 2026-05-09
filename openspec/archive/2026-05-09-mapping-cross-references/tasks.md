# Tasks: mapping-cross-references (archived)

**Status**: ARCHIVED
**Phase**: sdd-tasks → sdd-archive
**Date**: 2026-05-09

---

## Summary

Tasks articulated 4 phases and 30+ checklist items for the per-kind ripgrep producer pipeline. Batches A–D mapped to apply-progress for execution. All tasks completed; some items marked incomplete in checklist due to process tracking constraints (W-4 in verify report).

For detailed task list and execution tracking, see full artifact in engram (id #1225) and apply-progress (id #1227).

---

## Batches Overview

| Batch | Kinds | Sub-agents | Notes |
|-------|-------|-----------|-------|
| A | rest-function, box-method, ws-command, rest-url | 1 (sonnet) | Synthetic kinds; emits side-channel |
| B | java-class (B1), store-module+mixin (B2), plugin+lib (B3) | 3 parallel | B3 requires A side-channel |
| C | vue-component (C1–C4 split by dir) | 4 parallel | 378 components sharded |
| D | validate, merge, xref.md, coverage, schema | inline or 1 sa | Sequential after A/B/C complete |

---

## Work Units

1. **Unit 1**: Batch A shards + side-channel (unblocks B3)
2. **Unit 2**: Batch B + C shards (parallel, requires Unit 1)
3. **Unit 3**: Batch D validation + merge + documentation

---

## Spec-to-Task Traceability

| REQ | Tasks |
|-----|-------|
| REQ-1 (schema fields) | A-1.4–1.9, B-2.1–2.5, C-3.1–3.4, D-4.3 |
| REQ-2 (kind enum) | A-1.4–1.9, B-2.1–2.5, C-3.1–3.4, D-4.1 |
| REQ-3 (usage_kind enum) | A-1.4–1.9, B-2.1–2.5, C-3.1–3.4, D-4.4 |
| REQ-4 (id join) | D-4.5 |
| REQ-5 (defined_at) | D-4.6 |
| REQ-6 (coverage) | D-4.2, D-4.8 |
| REQ-7 (JSON + xref.md) | D-4.1, D-4.9 |
| REQ-8 (unused invariant) | D-4.4 |
| REQ-9 ($niagara) | A-1.9, B-2.4 |
| REQ-10 (schema doc) | D-4.10 |

---

**Status**: ARCHIVED. All tasks completed; verify report identified 5 CRITICALs in output which were resolved via post-verify jq normalization before archival.
