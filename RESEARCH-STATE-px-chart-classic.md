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
covered_blocks: 252
gaps_closed: 6
known_gaps: 8
investigable_open: 2
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
| high | H2 ejes + render Swing | decompiled-java | closed (B252) |
| high | H3 binding a datos reales (histories/puntos) | decompiled-java | closed (B253) |
| high | H4 consumidores reales + §14 vs B199/B201 | relational | closed (B254) |
| medium | H5 implementación interna com.tridium.chart | decompiled-java | closed (B255) |
| medium | H6 salidas no-Swing PDF + HX | decompiled-java | closed (B256) |
| medium | H7 los tests como especificación | decompiled-java | pending (NEXT) |
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

- **read-only-investigable**: **2** (H7, H8) → focus ACTIVO, cerca del agotamiento: suman **13 clases**
  (test 8 + rt 5). Se cierran INLINE (sin sub-agente), como H6.
- **requires-execution**: 0. **blocked**: 0. (Nota: el posible off-by-one de `BDiscreteAxis.fromDisplaySpace`,
  B252 §252.7-i, quedó marcado `[INFER]` NO confirmado — reproducirlo exige ejecución, fuera del alcance.)
- **Coverage metric**: **6 / 8** gaps cerrados (B251-B256). Quedan H7 (MED) + H8 (LOW).
- **Próximo gap**: **H7** (`com.tridium.chart.test` — los tests como especificación).
- **SEÑAL DE AGOTAMIENTO (§11)**: dos bloques CONSECUTIVOS de evidencia por encima del umbral 0.5 —
  B254 = 0.59, B255 = 0.56. La evidencia investigable del focus se está agotando; consistente con las 16
  clases que quedan. Alimenta la decisión §8 de STOP tras H8.
- **Señal de agotamiento (§11)**: B253 cerró con ratio 0.48, al filo del umbral 0.5 para un bloque de
  evidencia — parte por las inferencias meta de la corrección §14, pero la capa `binding` (9 clases chicas)
  queda sustancialmente agotada.

## Historia de iteración

| It | Fecha | Gap | Bloque | Hallazgo | Delegado? · tier |
|---|---|---|---|---|---|
| it.6 | 2026-07-24 | **H6** — salidas no-Swing | **B256** | `BPdfChartPane extends BChartPane implements BIPdfWidget`: para exportar **TRASPASA el modelo, no lo copia** — al chart EN VIVO le instala un `BoundChartModel` vacío y le pasa el modelo original a la copia PDF (evita duplicar un `TableSeries` con toda la tabla, pero deja el widget de pantalla inconsistente). **La prueba de que Tridium lo sabía**: `BResourceManagerToPdf.export()` termina forzando `getWbShell().getRefreshCommand().doInvoke()` — una reparación explícita del efecto colateral. Bindings clonados vía `temp.fw(303, target, ...)` (opcode interno no documentado). **§14 a B254 §254.8**: `BHxPxChartPane` (@NiagaraSingleton, agente sobre chart:ChartPane) demuestra que el chart clásico **SÍ llega al browser** por el perfil **Hx legacy** — pero DEGRADADO a imagen muerta: `getChildWidgets()` devuelve array VACÍO y `getMouseEventHandler()` devuelve NULL → sin zoom, sin pan, sin traza. El veredicto correcto tiene TRES casos, no dos: Workbench=clásico interactivo · browser moderno=webChart · **browser Hx=clásico renderizado en servidor sin interacción**. Propaga los facets del HxOp a todos los ejes (así llega el formato de fecha del cliente). | **no · inline** (3 clases, 180 líneas — bajo el umbral de delegación) |
| it.5 | 2026-07-24 | **H5** — impl privada | **B255** | Las 13 clases privadas NO son misceláneo: son **infraestructura de INTERACCIÓN y EDICIÓN** (contenedor de ejes, controles pan/zoom, 4 field editors del property sheet) — Tridium mantuvo público el modelo/ejes/bindings y privado el cómo se edita e interactúa. **RESUELVE el UNVERIFIED de B252 §252.5**: `BAxisContainer.paint()` es quien llama `axis.getRenderer().paint(g, axis)` con `g.push()/translate()/pop()` por eje (por eso el renderer pinta en coords locales); también explica cómo se setean `BAxisDimension`/`BAxisLocation` sin ser slots (los propaga `addAxis()`). `BoundChartSpec` = back-pointer al binding y NADA más (pasivo, confirma B253 §253.4). `BoundTimeSeries` = búfer paginado 256 con compresión por cambio (una señal plana NO consume memoria: estira el timestamp de la última muestra) y ventana en 2 modos (anclada hasta llenar `timeWindow`, luego rodante). **COMPLETA B254 §254.3**: el desplegable de tipo de eje del property sheet usa `getTypes()` (todos los subtipos registrados de BAxis), NO `getAgents()` → los ejes custom de analytics SÍ le aparecen al usuario aunque no sean agentes; el punto de extensión funciona por la vía MANUAL, no la automática. **3 defectos verificados**: (a) carrera de datos real — `sample()` es `synchronized` pero `getValue()`/`getSampleCount()` NO, y el poll de 2 Hz corre permanente mientras el paint thread lee; (b) `ChartUtil.makeGradient()` emite DOS stops en 0% → el color original del brush NUNCA se rinde (afecta a todo `BAreaChart`); (c) misma carrera en `BResourceManager`. Bonus: `BResourceManager` = el patrón mínimo para embeber un chart en una vista Workbench (2 Series sobre int[], BChartPane, NullAxisRenderer). Sin ZKM: las 13 decompilan limpio. 11 tokens re-verificados. verify-block exit 0, ratio 9/16 = 0.56 → **2do bloque consecutivo >0.5 = señal de agotamiento §11** | sí · **sonnet** (barrido 13 clases) + verificación inline |
| it.4 | 2026-07-24 | **H4** — consumidores + veredicto | **B254** | **8 módulos / 55 archivos** consumen el chart clásico (module-navigator sobre 926 jars). El pesado es `analytics-wb`: sus charts **EXTIENDEN `BChart`** (no lo envuelven) → **Analytics NO tiene motor gráfico propio en Workbench**, y hereda TODOS los gotchas de B251/B252 (12 colores, slurp, búfer AWT). También lo usa `honeywellSpyderTool` (`BPieChartPane`) → el mismo motor dibuja en la herramienta de comisionamiento Spyder. `BChartRenderLimitConfiguration` (analytics-rt, propiedad OCULTA de BAnalyticService): topes por tipo 3.000-250.000 filas, coherentes con mitigar el slurp — pero **enforcement UNVERIFIED**, cero llamadores visibles. **VEREDICTO clásico vs webChart: lo decide el PERFIL, no un switch** — Workbench/Swing = clásico, browser/móvil = webChart; los puentes (`analytics-wb`, `history-wb`) escriben DOS implementaciones separadas, no hay conversión. **Ausencia probada: CERO `@Deprecated` en todo el módulo** → el chart clásico está plenamente vigente en 4.14, es paralelo a webChart, no anterior. §14 x2: (a) matiza B253 §253.5 — el registro de agentes para ejes EXISTE pero `BDaysAxis`/`BHoursAxis` NO llevan `@AgentOn` (ausencia probada) y se construyen a mano → capacidad declarativa no transitada; (b) matiza B253 §253.9 — los permisos SÍ existen, una capa arriba: `BHistoryChart` usa `@AgentOn(requiredPermissions="r")`. 12 tokens re-verificados. verify-block exit 0, ratio 10/17 = 0.59 — bloque declarado **MIXTO evidencia+veredicto**; la evidencia RELACIONAL sí queda agotada (55 archivos enumerados) | sí · **sonnet** (80 tool-calls, module-navigator) + verificación inline |
| it.3 | 2026-07-24 | **H3** — binding a datos | **B253** | `BChartBinding extends BBinding` (javax.baja.ui) con **5 slots propios** — HERMANO de `BValueBinding`, no descendiente; declarable desde `.px`. Dos estrategias con naturaleza temporal OPUESTA: `BTableChartBinding` = **SNAPSHOT** (resuelve la ORD una vez, `Tables.slurp()`, sin COV ni scheduler → un chart de history NO se actualiza solo, solo al re-ligarse) vs `BValueChartBinding` = **POLL de 500 ms HARDCODEADO** (`Clock.schedulePeriodically`; el slot `timeWindow` de 5 min es el ANCHO de ventana, no la frecuencia) con filtro COV encima (`BoundTimeSeries.isChange`) y `pageSize=256` hardcodeado; al desbindear DESCARTA la historia en memoria. **Ausencia probada**: 0 referencias a `BHistoryConfig`/`BIHistory` en las 67 clases — el chart NO conoce histories, todo pasa por `BITable`. `doSyncBindings()` = reconciliación en 2 barridos disparada por `bound()`/`unbound()`, NO por llegada de datos; reutiliza ejes vía `findAxis()`+`isCompatible()` y FUSIONA ejes discretos. `BAxisSpec.toAxis()` elige el tipo de eje **por REGISTRO DE AGENTES** (`Sys.getRegistry().getAgents` filtrado por `BAxis.TYPE`). `BAxisBound`+`BColumnIdentifier` viven en `-rt` (lo serializable) con formato de cable `"fixed,<typespec>,<valor>"` / `"tableColumn:<nombre>"`. **§14 CORRIGE B252 §252.5**: mi tesis "módulo pre-agentes" era una generalización indebida desde la capa de render — el módulo SÍ usa agentes en la capa de datos; solo el RENDER quedó cableado a setters. §253.8: 2 afirmaciones del barrido corregidas, incl. "mecanismo multi-serie muerto" → FALSO, `BTransformChartBindingCollection` (seriesTransform-wb) es implementador único en 50.798 archivos. 12 tokens re-verificados. verify-block exit 0, ratio 11/23 = 0.48 | sí · **sonnet** (barrido binding) + module-navigator inline |
| it.2 | 2026-07-24 | **H2** — ejes + render | **B252** | `BAxis extends BObject implements BIAgent` (NO es BComponent, cero slots, 8 métodos abstractos); `toDisplaySpace()` invierte el origen en el eje Y dentro de la propia proyección. `BDiscreteAxis` cuelga de `BAxis` DIRECTO (no de `BContinuousAxis`) y tiene el zoom deshabilitado. **DOS algoritmos de tick spacing distintos**: numérico = redondeo por orden de magnitud (log10 + piso duro 5.0 + tope 20 ticks), temporal = tabla fija de 10 tramos (1ms..1año) + tope 30 ticks, con facet `timeFormat` como override real. `BAxisDimension`/`BAxisLocation` = `BFrozenEnum` en `-rt`. **HALLAZGO DE ARQUITECTURA**: la extensión del render es 100% PROGRAMÁTICA (setters Java, incl. uno estático) — ni slot, ni `@AgentOn`, ni factory; anomalía frente a B211/B212/B214 → `chart` precede al mecanismo de agentes. `BChartPane`: TRES niveles de refresco (build/refresh/rebuild diferido, refina B251 §251.7), reparto de ejes hardcodeado (1er X→bottom, 2do→top, 1er Y→left, resto→right), zoomStack ilimitado, `export()` solo exporta el PRIMER chart. Doble búfer de `BChartCanvas` **solo bajo AWT**. `BNullChartLegend` = Null Object. **2 BUGS REALES de Tridium confirmados literalmente**: (a) `assignColors()` usa `return` donde iba `continue` → si la 1ra serie ya tiene brush, NINGUNA de las siguientes recibe color; (b) `BChartHeader` testea `title.length()` para decidir si pinta el SUBTÍTULO → subtítulo inútil sin título. Más: reset del eje temporal ancla al reloj de pared (ventana 1h), ±10.0 hardcodeado si min==max, mínimo 300×300, `ParseException` tragada en el layout de ticks. 14 tokens re-verificados (incluidos los 2 bugs). verify-block exit 0, ratio 8/28 = 0.29 (evidencia, sano) | sí · **sonnet** (barrido ejes/render) + verificación inline |
| it.1 | 2026-07-24 | **H1** — modelo + jerarquía | **B251** | `BChart extends BWidget` con CERO slots propios y `paint()` **final** (solo hook `doPaint()`); exige padre `BChartPane` o `IllegalStateException`. Los 7 tipos concretos extienden `BChart` directo y **ninguno es thin** — el tipo de gráfico es una SUBCLASE JAVA, no un enum (contraste estructural con el `seriesFactory` JS de B199). `ChartModel` = clase abstracta que hereda de `BChart.ChartSupport` (todo modelo lleva back-pointer a su chart). `TableSeries` hace `Tables.slurp()` = **materialización ansiosa completa** del BITable (techo de escala). `JoinTable` = pivote multi-serie (no es BITable) con auto-escala inventada 0–10 si min=max=0. `TrendFlags` (en `-rt`) recibe los bits de `BStatus` directo → el estado de calidad y la decisión de dibujar comparten palabra de bits. Eventos: solo `SPEC_MODIFIED(3)` hace `refresh()`, todo lo demás `rebuild()`. **8 hallazgos load-bearing** incl. tope duro de 12 colores con caída silenciosa a negro, logger pisado en `export()`, `BDiscreteLineChart` no reentrante, cast sin guarda en `BStackedBarChart`, excepción tragada en `ChartController`. **Proven-absence**: CERO gate de licencia/capacidad (contraste con la capa OEM de B242/B244/B246). §251.9: 2 afirmaciones del barrido CORREGIDAS por el token-check (ruta de `BoundChartModel`; falso "sin guarda de padre nulo"). 13 tokens re-verificados. verify-block exit 0, ratio 7/19 = 0.37 (evidencia, sano) | sí · **sonnet** (barrido 34 clases) + verificación inline |

**Resume condition**: focus ACTIVO recién bootstrapeado. NO re-bootstrapear; tomar H1 del backlog de arriba y
correr el NORMAL CYCLE. Fuentes ya confirmadas y medidas (e2 arriba) — no re-medir, no re-decompilar.
