# RESEARCH-STATE — focus: px-chart-classic (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-07-24** sobre el módulo `chart`
> (`javax.baja.chart` — el sistema de charting **CLÁSICO** Swing/Workbench de Niagara N4), el feed que los
> focuses PX previos declararon explícitamente fuera de su alcance:
> - `RESEARCH-STATE-px-editor-core.md` §Notas: *"Feeds separados (NO parte de este focus, serían otro):
>   `javax.baja.chart` (chart clásico completo, B201 lo rozó)"*.
> - B201 (`make/` wizard) documentó `BMwChart`/`BMwTimePlot` usando **chart clásico**, y marcó el §14 contra
>   B199 (`webChart` = el sistema **moderno** bajaux/D3). Los dos sistemas coexisten; solo uno está documentado.
>
> **Los 4 focuses PX previos están CERRADOS** (px-menu B179-B190 12/12 · px-editor B191-B196 6/6 ·
> px-editor-deep B198-B209 11/11 · px-editor-core B210-B215 5/5), todos con `investigable_open: 0`.
> Este NO es una reapertura de ninguno: es un focus nuevo sobre un módulo distinto (`chart`, no `pxEditor-wb`).
>
> Corpus en **Español (técnico EN)** por continuidad (TARGETS.md fila 1). Numeración global de bloques
> (`niagara-mental-model-bloqueN.md`); el máximo en disco al bootstrap es **B250** → este focus arranca en **B251**.
> Engram topic key: `research/niagara/px-chart-classic/{gaps,progress}`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 247
gaps_closed: 1
known_gaps: 8
investigable_open: 7
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: px-chart-classic
status: active
bootstrapped_on: 2026-07-24
block_prefix: niagara-mental-model-bloqueN.md (numeración global; siguiente libre derivado en vivo)

## Ángulo declarado (§b2)

Reconstruir el **sistema de charting clásico** de Niagara N4 (`javax.baja.chart`): su modelo de datos, su
jerarquía de tipos de gráfico, su sistema de ejes/render Swing, y **cómo se bindea a datos reales**
(histories/puntos). Objetivo transversal: entender por qué N4 arrastra **DOS sistemas de charting**
(clásico Swing-Workbench vs `webChart` bajaux/D3 de B199) y quién consume cada uno.

## Pre-flight e2 — existencia + tamaño MEDIDO (no estimado)

Fuente: `/home/cristian/modules/Prototipos/modulos/organized/chart/`. Conteo sobre el pipeline **vineflower**
(canónico), colapsando los pipelines duplicados `decompiled/` + `pipeline/procyon/` + CFR — que triplicaban el
raw `.java` a ~134 y habrían mis-dimensionado el backlog (METHODOLOGY §13).

| Artefacto | Clases distintas | Desglose por paquete |
|---|---|---|
| `chart-rt` | **5** | `javax/baja/chart` 3 · `javax/baja/chart/binding` 2 |
| `chart-wb` | **62** | `javax/baja/chart` 31 · `com/tridium/chart` 11 · `com/tridium/chart/test` 8 · `javax/baja/chart/binding` 7 · `com/tridium/chart/wb` 2 · `com/tridium/chart/pdf` 2 · `com/tridium/chart/hx` 1 |
| **Total** | **67** | (100 `.class` en `chart-wb/extracted` = 62 top-level + inner/anónimas) |

Metadatos del jar (`module_nav module chart-wb`): bytecode v52 (Java 8), **ZKM: no** (sin ofuscación),
vineflower + CFR disponibles.

**API pública** (`niagara_help package`, fuente Tridium oficial): `javax.baja.chart` = **35 clases**;
`javax.baja.chart.binding` = **9 clases**. Nota: `docSource` NO cubre `javax.baja.chart` (verificado: 0
archivos bajo `docSource-doc/extracted/**/javax/baja/chart/`) → no hay fuente original con javadoc para este
módulo; la evidencia primaria es el **decompilado vineflower** `[CERT]` + la **API pública de niagara-help**.

**Señal de consumidores** (`module_nav grep 'import javax\.baja\.chart'` — 30+ matches / 8 archivos):
`analytics-wb` (`BAggregationChart`, `BAverageProfileChart`, `BAverageProfileChartPane`,
`BEquipmentOperationChart`, `BDaysAxis`, `BHoursAxis`, `BAnalyticChartBinding`). `module_nav search 'BChart*'`
suma consumidores en otros módulos: `BChartEditor` (history-wb), `BChartConfigPane`/`BChartDescriptor`
(seriesTransform-wb), `BChartFile` (webChart-rt), `BChartRenderLimitConfiguration` (analytics-rt).

## Gap-backlog (priorizado)

Formato canónico de 4 columnas exigido por `research-sdd-status.sh` (prioridad `high/medium/low`, estado con
token líder `pending`); el detalle de fuentes y clases por gap va en la lista bajo la tabla.

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | H1 modelo de datos + jerarquía de charts | decompiled-java | closed (B251) |
| high | H2 ejes + render Swing | decompiled-java | pending (NEXT) |
| high | H3 binding a datos reales (histories/puntos) | decompiled-java | pending |
| high | H4 consumidores reales + §14 vs B199/B201 | relational | pending |
| medium | H5 implementación interna com.tridium.chart | decompiled-java | pending |
| medium | H6 salidas no-Swing PDF + HX | decompiled-java | pending |
| medium | H7 los tests como especificación | decompiled-java | pending |
| low | H8 el split rt/wb | decompiled-java | pending |

### Detalle por gap (fuentes medidas)

- **H1** — `BChart` y sus subtipos (`BLineChart`, `BBarChart`, `BAreaChart`, `BPieChart`, `BStackedBarChart`,
  `BDiscreteLineChart`, `BDiscreteAreaChart`) + `ChartModel`/`SimpleChartModel`/`Series`/`TableSeries`/
  `ChartSpec`/`JoinTable`/`TrendFlags`/`ChartModelEvent`/`ChartController`.
  Fuente: `chart-wb/vineflower/javax/baja/chart/` (31) + `chart-rt` (3).
- **H2** — `BAxis`/`BContinuousAxis`/`BDiscreteAxis`/`BNumericAxis`/`BAbsTimeAxis`/`BAxisDimension`/
  `BAxisLocation` + `AxisRenderer`/`DefaultAxisRenderer`/`SwatchRenderer`/`DefaultSwatchRenderer` +
  contenedores `BChartPane` (884 líneas) / `BChartCanvas` (529) / `BChartHeader` / `BChartLegend` /
  `BDefaultChartLegend` / `BNullChartLegend`. Fuente: mismo paquete que H1.
- **H3** — `javax.baja.chart.binding` (9): `BChartBinding`, `BTableChartBinding`, `BValueChartBinding`,
  `BChartBindingCollection`, `BoundChartModel`, `BAxisSpec`, `BDiscreteAxisSpec`, `BAxisBound`,
  `BColumnIdentifier`. Cómo un chart se ata a un history/punto. Cruce con B193 (los 9 bindings kitPx) y
  B198 (binding/ ORD-rewriting). Fuente: `chart-{rt,wb}/vineflower/javax/baja/chart/binding/` (2+7).
- **H4** — quién usa el chart CLÁSICO en el universo de 926 jars: `analytics-wb` (6+ clases), `history-wb`
  `BChartEditor`, `seriesTransform-wb`. Responde la pregunta transversal: ¿cuándo N4 usa clásico y cuándo
  `webChart`? Refina/confirma B199 §X1 y B201 (`BMwChart`). Fuente: `module_nav` (xref/grep/type-consumers).
- **H5** — qué agrega la impl privada sobre la API pública (11 clases) + `com.tridium.chart.wb` (2).
  Fuente: `chart-wb/vineflower/com/tridium/chart/`.
- **H6** — `com.tridium.chart.pdf` (2) = export a PDF; `com.tridium.chart.hx` (1) = puente al perfil web HX
  legacy. Cruce con B194 (media/perfiles Wb/Hx/Mobile). Fuente: `com/tridium/chart/{pdf,hx}/`.
- **H7** — `com.tridium.chart.test` (8 clases): qué contratos ejercitan y qué revelan del uso previsto
  (evidencia de uso real, no doc). Fuente: `com/tridium/chart/test/`.
- **H8** — por qué el runtime tiene solo 5 clases y todo el motor vive en `-wb`: qué es lo mínimo que una
  station necesita saber de un chart sin poder dibujarlo. Fuente: `chart-rt/vineflower/` (5).

## Blocked gaps

- none

## Clasificación (§8)

- **read-only-investigable**: **7** (H2-H8) → focus ACTIVO.
- **requires-execution**: 0. **blocked**: 0.
- **Coverage metric**: **1 / 8** gaps cerrados (B251).
- **Próximo gap**: **H2** (ejes + render Swing — B251 los nombró sin abrirlos).

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| it.1 | 2026-07-24 | **H1** — modelo + jerarquía | **B251** | `BChart extends BWidget` con CERO slots propios y `paint()` **final** (solo hook `doPaint()`); exige padre `BChartPane` o `IllegalStateException`. Los 7 tipos concretos extienden `BChart` directo y **ninguno es thin** — el tipo de gráfico es una SUBCLASE JAVA, no un enum (contraste estructural con el `seriesFactory` JS de B199). `ChartModel` = clase abstracta que hereda de `BChart.ChartSupport` (todo modelo lleva back-pointer a su chart). `TableSeries` hace `Tables.slurp()` = **materialización ansiosa completa** del BITable (techo de escala). `JoinTable` = pivote multi-serie (no es BITable) con auto-escala inventada 0–10 si min=max=0. `TrendFlags` (en `-rt`) recibe los bits de `BStatus` directo → el estado de calidad y la decisión de dibujar comparten palabra de bits. Eventos: solo `SPEC_MODIFIED(3)` hace `refresh()`, todo lo demás `rebuild()`. **8 hallazgos load-bearing** incl. tope duro de 12 colores con caída silenciosa a negro, logger pisado en `export()`, `BDiscreteLineChart` no reentrante, cast sin guarda en `BStackedBarChart`, excepción tragada en `ChartController`. **Proven-absence**: CERO gate de licencia/capacidad (contraste con la capa OEM de B242/B244/B246). §251.9: 2 afirmaciones del barrido CORREGIDAS por el token-check (ruta de `BoundChartModel`; falso "sin guarda de padre nulo"). 13 tokens re-verificados. verify-block exit 0, ratio 7/19 = 0.37 (evidencia, sano) | sí · **sonnet** (barrido 34 clases) + verificación inline |

**Resume condition**: focus ACTIVO recién bootstrapeado. NO re-bootstrapear; tomar H1 del backlog de arriba y
correr el NORMAL CYCLE. Fuentes ya confirmadas y medidas (e2 arriba) — no re-medir, no re-decompilar.
