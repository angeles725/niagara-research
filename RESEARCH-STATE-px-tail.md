# RESEARCH-STATE — focus: px-tail (PLANNED)

> Focus **PLANIFICADO** el 2026-07-24. **0 bloques escritos.** §16: focus *planned* — **el loop NO debe
> re-BOOTSTRAPEARLO**; toma este estado y escribe su primer bloque.
>
> **Origen**: los cinco focuses PX del corpus están CERRADOS (px-menu B179-B190 · px-editor B191-B196 ·
> px-editor-deep B198-B209 · px-editor-core B210-B215 · px-chart-classic B251-B259), pero el universo PX
> **no** está agotado: quedan tres módulos con **cero entradas en `CATALOG.md`**.
>
> Corpus en **Español (técnico EN)**. Numeración global; próximo libre al planificar: **B271**.
> Engram topic key: `research/niagara/px-tail/{gaps,progress}`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 419
gaps_closed: 2
known_gaps: 3
investigable_open: 1
requires_execution_open: 0
blocked_open: 0
block_scope: shared-global
<!-- /research-state.v1 -->

focus: px-tail
status: active
planned_on: 2026-07-24
started_on: 2026-08-09

## Ángulo declarado (§b2)

Cerrar la **cola** del subsistema PX: los tres módulos que los cinco focuses previos nombraron o rozaron sin
abrir nunca. No es profundizar lo ya documentado — es cubrir lo que quedó fuera.

## Pre-flight e2 — tamaño MEDIDO (pipelines colapsados)

Conteo sobre `vineflower/` únicamente. **Advertencia de método**: contar `.java` sin restringir a `vineflower`
suma los pipelines duplicados (`decompiled` + `procyon` + CFR) e infla 2-3×. En este mismo corpus se cometió
el error **dos veces** (chart: 134 vs 67 real; estos tres módulos: 190/57/30 vs 95/19/15 reales).

| Módulo | Clases distintas | Entradas en CATALOG |
|---|---|---|
| `webEditors` | **95** (`-ux`) | **0** |
| `galileoKitPx` | **19** (`-wb`) | **0** |
| `kitPxBuilding` | **15** (`-rt` + `-ux` + `-wb`) | **0** |

## Gap-backlog (priorizado)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | P1 webEditors la capa ux de field editors web | decompiled-java | closed (B421) |
| medium | P2 kitPxBuilding componentes de equipo tipados | decompiled-java | closed (B422) |
| low | P3 galileoKitPx el kitPx de otro OEM | decompiled-java | pending (NEXT) |

### Detalle por gap

- **P1 (HIGH, 95 clases)** — el más grande y el más citado sin abrir: aparece en **8 bloques** (7 menciones
  solo en [Bloque 199], que documentó que los *settings* de `webChart` se montan sobre `webEditors`) y **nunca
  fue sujeto**. Es la capa `-ux` de field editors web. Cruce obligado con [Bloque 204] (bajaux: `Widget`
  lifecycle + spandrel), [Bloque 202] (field editors PX Wb↔Ux) y [Bloque 214] (los FE por `@AgentOn`).
  Pregunta guía: ¿es el equivalente web de `BWbFieldEditor`, y cómo se emparejan?
- **P2 (MED, 15 clases)** — [Bloque 203] lo marcó como **"la excepción"**: mientras `kitPxGraphics`/`Hvac`/
  `N4svg` son paletas BOG sin código, `kitPxBuilding` trae **componentes Java tipados** (`BEquipment`,
  `BDamper`, `BKnob`). Tri-parte `rt`/`ux`/`wb`. Pregunta guía: ¿por qué estos necesitaron código y los otros
  no?
- **P3 (LOW, 19 clases)** — un kitPx de **otro OEM** (Galileo), `-wb`. Valor comparativo: contrastar con
  `easyBinding` ([Bloque 207], OEM Honeywell, license-gated y parcialmente ofuscado) para ver si el patrón OEM
  se repite.

## Blocked gaps

- none

## Clasificación (§8)

- **read-only-investigable**: **1** (P3). **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: **2 / 3** (2 bloques escritos).
- **Próximo gap**: **P3**.

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| 1 | 2026-08-09 | P1 webEditors | B421 | Patrón BSingleton+BIJavaScript+@AgentOn+JsInfo; 95 clases, 49 FE-baja, 66 tipos; BPropertySheet=vista principal; Wb↔Web: BWbFieldEditor vs BIJavaScript | No · sonnet |
| 2 | 2026-08-09 | P2 kitPxBuilding | B422 | BComponent changed() como agregador multi-input; BKitPxBuildingBaseEnum extends BComponent+BIEnum; 5 state machines + 6 enums en -rt; BBajaScriptTypeExt (no BIJavaScript) en -ux; -wb = 0 Java; BOG puro imposible por multi-input con prioridad | No · sonnet |

**Resume condition**: 2/3 gaps closed. Tomar P3 y correr el NORMAL CYCLE.
