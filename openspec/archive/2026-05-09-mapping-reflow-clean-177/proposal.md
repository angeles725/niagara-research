# Proposal: mapping-reflow-clean-177

**Phase**: sdd-propose
**Date**: 2026-05-09
**Source (READ-ONLY)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Output (WRITE)**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`

[Content abbreviated for space — full content persisted in Engram #1211]

---

## Intent

Los bloques 63-67 consumían Reflow-Clean-177 de forma ad-hoc. Necesitamos un mapping queryable que materialice los análisis existentes en una estructura uniforme, sea grep/jq-able, y defina un schema reusable para Analytics/MX60.

## Scope (In Scope)

- `docs/mappings/reflow-clean-177/index.md` — índice maestro humano-legible, 547 entradas
- `docs/mappings/reflow-clean-177/index.json` — sidecar `jq`-queryable
- `docs/mappings/reflow-clean-177/schema.md` — definición canónica del schema core + extension (reusable para Analytics/MX60)
- `docs/mappings/reflow-clean-177/domains/` — 8 domain deep dives (backend, frontend overview, equipment, floorplans, alarms, history, buildings-config)
- `docs/mappings/reflow-clean-177/README.md` — guía de uso

## Capabilities

**New**: `module-mapping` — capacidad de producir mapping versionado (índice + schema + domain deep dives) para cualquier módulo Niagara/Reflow.

---

**Archived to**: openspec/archive/2026-05-09-mapping-reflow-clean-177/
**Phase Complete**: sdd-propose
**See Engram**: #1211 (full proposal)
