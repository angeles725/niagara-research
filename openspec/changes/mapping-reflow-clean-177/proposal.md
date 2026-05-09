# Proposal: mapping-reflow-clean-177

**Phase**: sdd-propose
**Date**: 2026-05-09
**Source (READ-ONLY)**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/`
**Output (WRITE)**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`

---

## Intent

Los bloques 63-67 vienen consumiendo `Reflow-Clean-177` de forma ad-hoc: cada sesión re-explora directorios, vuelve a contar archivos, re-deduce qué clase Java alimenta qué Vue store. Eso es deuda de navegación, no de conocimiento — el conocimiento ya existe en `REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md` y `NIAGARA-INTEGRATION.md`, sólo está disperso. Necesitamos un mapping queryable que (a) materialice esos tres docs en una estructura uniforme, (b) sea grep/jq-able sin abrir 535 archivos, y (c) defina un schema **reusable** para mapear Analytics y MX60 después con el mismo formato. Esto desbloquea trabajo derivado (referencias cruzadas para diseño MX60, validación de antipatterns, generación de tasks SDD apuntando a archivos exactos) sin volver a pagar el costo de exploración.

## Scope

### In Scope

- `docs/mappings/reflow-clean-177/index.md` — índice maestro humano-legible, una fila por archivo (535 entradas).
- `docs/mappings/reflow-clean-177/index.json` — sidecar `jq`-queryable con los mismos 535 registros, schema-conformante.
- `docs/mappings/reflow-clean-177/schema.md` — definición canónica del schema **core + extension** (reusable para Analytics/MX60).
- `docs/mappings/reflow-clean-177/domains/backend.md` — deep dive backend: `BReflowService` (26 slots), `BaseServlet` (24 endpoints), 7 BOX command classes (~25 RPC methods), History/Alarms/Sync/WebSocket pipelines.
- `docs/mappings/reflow-clean-177/domains/frontend.md` — deep dive frontend: bootstrap `main.js`, App.vue, router 37 rutas, Vuex 29 módulos, 13 plugins, 17 mixins, 10 lib, capa API 5 archivos.
- `docs/mappings/reflow-clean-177/domains/floorplans.md` — domain con 47 componentes; warrant propio por complejidad SVG canvas.
- `docs/mappings/reflow-clean-177/domains/equipment.md` — domain con 41 componentes; warrant propio por superficie de UI.
- `docs/mappings/reflow-clean-177/domains/alarms.md` — domain cross-stack (5 clases Java + 22 Vue + AlarmCache lib + websocket events).
- `docs/mappings/reflow-clean-177/domains/history.md` — domain cross-stack (12 clases Java + 22 Vue + 5 serializers Jackson).
- `docs/mappings/reflow-clean-177/domains/buildings-config.md` — combina buildings (27) + config (22) por acoplamiento `BReflowSyncService`/`config.json`.
- `docs/mappings/reflow-clean-177/README.md` — guía de uso: cómo se hace `rg`/`jq`, cómo extender el schema para otro módulo, cómo se mapea fidelity de GAP-ANALYSIS.

### Out of Scope

- NO es una herramienta de software ni una UI; es documentación versionada en este repo.
- NO incluye análisis de assets binarios (22 JPGs en `image-library/`, 6 PNGs en `icons/`, .jar/.class de `nmodsreflow-ux/build/`) más allá de catalogarlos como `status: resource | compiled`.
- NO contiene diff decompilado-vs-original ni intenta verificar correctitud del CFR 0.152 sobre las 4 clases marcadas como decompiled.
- NO mapea Analytics (`bloque 66-67`) ni MX60 — esos consumirán **el mismo schema** en otro change posterior.
- NO analiza runtime/comportamiento dinámico (subscripciones BajaScript en vivo, race conditions WebSocket, perf profiling).
- NO replica `node_modules/` ni `src/rc/` (compiled bundle); se marcan como excluidos.
- NO duplica el contenido íntegro de los 3 docs existentes; los **referencia y sintetiza**, no los copia.

## Capabilities

### New Capabilities

- `module-mapping`: capacidad de producir un mapping versionado (índice + schema + domain deep dives) para cualquier módulo Niagara/Reflow del catálogo MX60, partiendo de un schema `core + extension`. Esta capability define el contrato del artefacto, los campos obligatorios, y la convención de directorios `docs/mappings/<module>/`.

### Modified Capabilities

- None.

## Approach

**Estrategia híbrida B + C** (de la exploración):

1. **Skeleton del índice (Tier 1, Option B — synthesize)**: una pasada determinística sobre los 3 docs existentes (`REFLOW-ARCHITECTURE-ANALYSIS.md`, `GAP-ANALYSIS.md`, `NIAGARA-INTEGRATION.md`) más `fd` sobre el árbol del repo fuente para producir `index.json`. Cada archivo del repo fuente genera UNA entrada con campos `core` poblados; campos de extensión se rellenan desde los docs cuando el doc los provee, se dejan `null` cuando requieren inspección directa. **Cero re-lectura de los 535 archivos en este tier.**

2. **Domain deep dives (Tier 2, Option C — domain-by-domain)**: 8 domain docs producidos por sub-agents en paralelo; cada uno lee SU subset (~10-50 archivos) y pobla los campos extensión que faltaban, además de la prosa explicativa. El `index.json` se enriquece al final con los campos descubiertos.

3. **Spot-check de validación**: 5 archivos elegidos al azar por dominio se leen completos y se compara su entrada en `index.json` contra el archivo real. Si fidelity < 90%, el dominio se re-procesa.

4. **Schema lock (core + extension)**:
   - **Core (universal, mandatorio)**: `path`, `kind`, `domain`, `purpose`, `loc`, `status`. `dependencies` opcional pero recomendado.
   - **Backend extension (Java, opcional)**: `profile` (`rt`/`ux`), `package`, `bcomponent_type`, `slots`, `actions`, `rest_endpoints`, `box_methods`, `decompiled`.
   - **Frontend Vue extension (opcional)**: `component_dir`, `store_modules`, `emits`, `props`, `mixins`, `plugins_used`, `persistent`, `route_name`, `fidelity`.
   - Cualquier extensión futura (analytics, mx60-specific) se agrega como nuevo bloque sin tocar el `core`. Esa es la garantía de reuso.

**Effort estimate**: 1 sesión para skeleton + schema + README (sdd-apply batch 1). 1-2 sesiones para domain deep dives en paralelo (sdd-apply batch 2 con 4-6 sub-agents). 1 sesión corta para spot-check + verify. Total ~3-4 sesiones.

**Reusability commitment**: el `schema.md` se versiona explícitamente como `mapping-schema v1`. El próximo change (`mapping-analytics-niagara` o `mapping-mx60-stack`) **importa** este schema, agrega su propio bloque de extensión (ej. `analytics-extension` con `algorithm_block_type`, `dag_role`, `aon_encoded`), y reutiliza el mismo layout de directorios. README incluye un walkthrough explícito de "cómo extender para otro módulo".

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `docs/mappings/reflow-clean-177/` | New | 11 archivos nuevos: index.md, index.json, schema.md, README.md, 8 domain docs |
| `openspec/changes/mapping-reflow-clean-177/` | New | proposal.md (este), spec.md, design.md, tasks.md (siguientes fases) |
| `/home/cristian/modules/Prototipos/Reflow-Clean-177/` | Read-only | Cero escrituras; sólo se lee como fuente |
| Engram | New | 9 topic_keys nuevos bajo `sdd/mapping-reflow-clean-177/*` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Los 3 docs fuente están desactualizados respecto al código | Medium | Spot-check de 5 archivos por dominio; si gap > 10% se marca el dominio como `tier: needs-rescan` y se prioriza inspección directa |
| Schema v1 resulta insuficiente para Analytics/MX60 | Low-Medium | Diseño explícito `core + extension`; v2 agrega bloques nuevos sin romper v1; `core` se mantiene estable |
| Decompiled Java introduce ruido en backend.md | Low | Campo `decompiled: true` en las 4 clases afectadas (`BReflowScheme`, `BReflow`, `BReflowConfig`, `BReflowRedirect`); el doc lo señala explícitamente |
| `index.json` 535-entry crece a tamaño difícil de leer | Low | `index.md` es la vista humana paginada por dominio; `index.json` es para `jq` y herramientas; nunca se lee linear |
| Sub-agents en paralelo producen domain docs con estilo divergente | Medium | `schema.md` + plantilla en README definen layout obligatorio por domain doc (secciones fijas: Overview / File Inventory / Key Classes / Cross-stack Links / Known Issues) |
| Fidelity ratings de GAP-ANALYSIS quedan obsoletas si el upstream cambia | Low | Campo `fidelity` se marca con `as_of: 2026-04-06`; se trata como snapshot, no como verdad viva |

## Rollback Plan

Trivial: el change sólo agrega archivos a `docs/mappings/reflow-clean-177/` y entradas Engram bajo `sdd/mapping-reflow-clean-177/*`. Para revertir:

1. `git rm -r docs/mappings/reflow-clean-177/`
2. `git rm -r openspec/changes/mapping-reflow-clean-177/`
3. (Opcional) `mem_delete` sobre los 9 topic_keys en Engram (sino quedan como histórico inerte, sin daño).

Cero impacto en código de bloques 63-67 ya commiteados; cero impacto sobre el repo fuente (read-only).

## Dependencies

- Repo fuente disponible en `/home/cristian/modules/Prototipos/Reflow-Clean-177/` (verified).
- Tres docs de análisis presentes en `reflow-frontend/docs/` (verified en exploration).
- `fd`, `rg`, `bat` instalados (verified — toolchain estándar del workspace).
- Engram backend disponible para hybrid persistence (verified — exploración ya persistida).

## Success Criteria

- [ ] `docs/mappings/reflow-clean-177/index.json` contiene exactamente una entrada por cada archivo source/resource/compiled del repo fuente, excluyendo `node_modules/` y `src/rc/` minified.
- [ ] `jq '.[] | select(.domain == "history")' index.json` devuelve >= 12 entradas backend + >= 22 entradas frontend.
- [ ] `schema.md` documenta el modelo `core + extension` con ejemplo concreto de cómo extenderlo a un módulo distinto (Analytics).
- [ ] Los 8 domain docs siguen la plantilla fija definida en README (5 secciones obligatorias).
- [ ] Spot-check sample (5 archivos × 8 domains = 40 archivos) verifica fidelidad >= 90% entre entrada en `index.json` y archivo real.
- [ ] README contiene al menos 3 ejemplos de queries `jq` y 2 ejemplos de búsqueda `rg` cruzando dominios.
- [ ] Próximo change que mapee otro módulo (Analytics o MX60) reutiliza `schema.md` sin modificar el bloque `core`.
