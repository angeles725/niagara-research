# Design: mapping-reflow-clean-177

**Phase**: sdd-design
**Date**: 2026-05-09
**Project**: niagara-research
**Schema status**: LOCKED v1.0 — reusable for Analytics/MX60.

---

## Architecture approach (one paragraph)

Documento mapping = artefacto estático versionado en git. Forma: 1 `index.json` (queryable vía `jq`) + 1 `index.md` (humano) + `schema.md` (contrato versionado) + N `domains/<x>.md` deep dives. El schema es **core + extension**: bloque core universal (10 campos), bloques extension (backend, frontend_vue, frontend_js, futuro analytics) viven en namespaces propios y son aditivos. Generación = pipeline determinístico de 4 batches (skeleton → backend → frontend paralelo → validation), aprovechando los 3 docs existentes como fuente primaria — cero re-exploración del código en Tier 1.

---

## Key Decisions (Schema v1.0 LOCKED)

| # | Decision | Rationale | Impact |
|---|----------|-----------|--------|
| D1 | Schema `core + extension`, top-level objeto | Reusabilidad Analytics/MX60; metadata futura sin breaking | Aplica a todos los SDD futuros que reutilicen schema |
| D2 | JSON sidecar + Markdown sibling | jq estándar; humanos leen md | Dual-form: máquina + humano |
| D3 | Synthesis from 3 docs + spot-check 40 archivos | 90% de info ya existe; AST parsing costo >2x | Eficiencia: 1-2 sesiones vs 4+ |
| D4 | path-based id (no hash) | legible, debuggeable, ya único | jq queries fáciles |
| D5 | 7 domain docs (backend + frontend + 5 warrants) | Balance entre granularidad y acoplamientos cross-domain | Legibilidad vs detalle |
| D6 | Pipeline 4 batches con C en paralelo (5 sub-agents) | Speedup ~5x sobre sequential; prefixes disjoint | Wall-clock: ~4 unidades |
| D7 | index.json enrichment via deferred merge en Batch D | Cero race conditions; trivialmente correcto | Batch D más pesado pero correcto |
| D8 | Forward-compat: adición de campos/bloques = no-breaking | Schema vivirá ≥3 changes (Analytics + MX60 + futuro) | schema_version v1.0 → v2.0 solo si core cambia |

---

## File Layout (LOCKED)

```
docs/mappings/reflow-clean-177/
├── README.md                    ← guía de uso + cómo extender + queries de ejemplo
├── schema.md                    ← contrato versionado v1.0
├── index.md                     ← humano: tabla paginada por dominio (~547 filas)
├── index.json                   ← jq-queryable: objeto top-level con entries[] + exclusions[]
├── excluded.md                  ← lista paths excluidos + razón
└── domains/
    ├── backend.md               ← TODOS los 77 Java (1 archivo)
    ├── frontend.md              ← overview + 17 dominios chicos
    ├── equipment.md             ← 41 components (warrant: domain crítico)
    ├── floorplans.md            ← 47 components (warrant: SVG canvas)
    ├── alarms.md                ← cross-stack: 5 Java + 22 Vue
    ├── history.md               ← cross-stack: 12 Java + 22 Vue
    └── buildings-config.md      ← buildings (27) + config (22)
```

---

## Pipeline (4 Batches, hybrid parallel/sequential)

```
Batch A (sequential, 1 sub-agent) → Skeleton: 5 files (index.md, index.json, schema.md, README.md, excluded.md)
   └── Batch B (sequential, 1 sub-agent after A) → Backend deep dive: domains/backend.md + patch
   └── Batch C (PARALLEL, 5 sub-agents after A) → 5 domain docs (frontend, equipment, floorplans, alarms, history/buildings)
       └── Batch D (sequential, 1 sub-agent after B + C) → Validation + merge + coverage report
```

**Wall-clock**: ~4 unidades en lugar de ~9 si todo fuera secuencial.

---

## Schema Core + Extensions (v1.0 LOCKED)

**Core** (10 campos universales, reusable):
- id, path, kind, domain, purpose, dependencies, loc, status, source_doc?, verified_at?

**Extensions** (aditivos, no breaking):
- backend (profile, package, bcomponent_type, slots, actions, rest_endpoints, box_methods, decompiled)
- frontend_vue (component_dir, store_modules, emits, props, mixins, plugins_used, route_name, fidelity)
- frontend_js (module_type, persistent, exports)
- **analytics** (prototipado para reuse: algorithm_type, dag_role, aon_encoded, execution_order, verdict)

Forward-compat rules:
1. Adición de campos = no-breaking
2. Adición de bloques de extensión = no-breaking
3. Cambio en core = MAJOR bump (v1.0 → v2.0)
4. Cambio de enum (kind, domain, status) = MINOR bump (v1.0 → v1.1)

---

## Validation Strategy (sdd-verify)

- [ ] JSON parse + top-level shape check
- [ ] Entry count ≥530
- [ ] Mandatory core fields present + non-null
- [ ] Backend ext for all java-class entries
- [ ] No node_modules entries
- [ ] Domain doc 5-section template compliance (5 per arquivo)
- [ ] Spot-check 40 entries (5 × 8 domains): ≥90% fidelity on purpose field
- [ ] Coverage ≥95% of in-scope files

---

## Trade-offs (Consolidated)

| Choice | Pros | Cons | Why chosen |
|--------|------|------|-----------|
| JSON vs YAML | jq standard, rigid schema | Less human-readable | jq universality > YAML readability |
| Flat index vs per-file | Queryable, O(1) lookup | Less modular | Index is source of truth |
| Synthesis vs AST parsing | Fast (1-2 sessions), uses existing docs | ~10% inference, depends on doc freshness | 90% info already exists, spot-check validates |
| path-based id vs hash | Legible, debuggeable, unique by construction | Breaks if file renaming (acceptable) | Developers understand paths |
| Deferred merge vs concurrent edits | Zero race conditions | Batch D heavier | Correctness > performance |

---

**Full Design**: see Engram #1213
**Status**: LOCKED, ready for sdd-tasks
