# Bloque 199 — `webChart`: el charting es bajaux puro (D3/JS) sobre servlets rt; Java ux es solo el bridge

> Research del focus **`px-editor-deep`** (gap X1): el módulo `webChart` — los gráficos/charts de Niagara.
> Módulo MIXTO de 3 capas: una fina capa Java `rt` (servlets de datos + persistencia `.chart`), un bridge Java
> `ux` de ~4 clases (declara el bundle JS), y el motor REAL de charts en **JavaScript bajaux + D3**. Confirma la
> hipótesis de B194 (webChart NO usa PxMedia/Swing) y la nota §14 de B192 (charts fuera de bajaui core).
> NO cubre kitPxGraphics/Hvac (X3) ni bajaux genérico (X5).
>
> Sources (preservados §5): `sources/decompiled/webChart-rt/` (8 java), `sources/decompiled/webChart-ux/` (4 java
> + 60 `rc/*.js`), `sources/manuals/webChart-docs/` (7 HTML oficiales `[CERT-doc]`, registrados en SOURCES.md).
> Barrido delegado (sonnet) 2026-07-06; 9 citas load-bearing token-checked literal (java+js+doc).
> Method: lectura READ-ONLY del decompilado + doc oficial. Markers (§3): `[CERT]` `file:line` · `[CERT-doc]`
> `sources/manuals/...html` · `[INFER]`. Tipo: EVIDENCE block (decompilación + doc).
>
> Capa PX (charts / bajaux). Connects [Block 192] (widgets bajaui), [Block 193] (bindings), [Block 194] (bajaux
> sin PxMedia), [Block 197] (síntesis), [Block 198] (sidebars, hermano D1).

---

## 199.1 — La arquitectura: bajaux puro, el Java es solo plumbing `[CERT]`

`BChartWidget` **NO es una view PX/Swing** — es un `BSingleton` agente que solo apunta a un módulo JS:

```java
// sources/decompiled/webChart-ux/.../ux/BChartWidget.java:25-28
public final class BChartWidget extends BSingleton implements BIJavaScriptToPdf, BIFormFactorMax, BIChartableHistoryAgent, BICollectionSupport {
   public static final BChartWidget INSTANCE = new BChartWidget();
   private static final JsInfo jsInfo = JsInfo.make(BOrd.make("module://webChart/rc/ChartWidget.js"), BWebChartJsBuild.TYPE);
```

Se registra como **agente** (`@AgentOn`, `BChartWidget.java:19-22`) sobre `history:HistorySpace`, `control:NumericPoint`,
`webChart:ChartFile`, `schedule:NumericSchedule`, virtuales — y su ÚNICA tarea es `getJsInfo(Context)`: devolver el
puntero al módulo JS. `BWebChartJsBuild` (`BWebChartJsBuild.java:14-28`) declara el bundle minificado
(`webChart.built.min.js`) y sus dependencias JS: `webEditors` (field editors) + `history` + `BWebChartCssResource`
(4 CSS). Es el patrón bajaux agent-view: el browser resuelve el agente para el tipo dropeado, obtiene el `JsInfo`, y
carga el JS client-side. `[CERT]`

El doc oficial lo dice literal: *"this **bajaux HTML5-based WebWidget** component allows you to add a Chart widget to
a Px page or Hx page, or to a Dashboard pane"* (`sources/manuals/webChart-docs/webChart-ChartWidget.html`). `[CERT-doc]`

Y el JS confirma que es un `bajaux/Widget` plano que renderiza con **D3 sobre SVG** — sin canvas Swing, sin renderer PX:

```js
// sources/decompiled/webChart-ux/rc/ChartWidget.js:24
define(['baja!', ..., 'bajaux/Widget', 'd3', 'jquery', 'moment', ..., 'nmodule/webChart/rc/model/LineModel', ...],
  function (baja, ..., Widget, d3, $, moment, ...) { /* ChartWidget = function(params){ Widget.call(that, ...) } */ }
```

**Conclusión (confirma B194):** webChart es un subsistema bajaux/JS/HTML5 puro; el módulo Java `webChart-ux` es solo
un bridge fino (agente + declaración de bundle). El grueso de la lógica vive en `rc/*.js`. `[INFER]` (sobre las citas CERT)

## 199.2 — La capa servidor `rt`: servlets de datos + persistencia `.chart` `[CERT]`

Dos servlets (`web.xml:6-22`): `query/*` → `WebChartQueryServlet` (GET, datos); `file/*` → `WebChartFileServlet`
(POST, persistencia, CSRF-filtrado).

**`WebChartQueryServlet`** es un endpoint REST puro con media type `application/vnd.tridium.webChart-v1+json`, tres rutas
(`WebChartQueryServlet.java:47-49`):

| Ruta | Devuelve | file:line |
|---|---|---|
| `/data/<escaped-ord>` | JSON minificado newline-delimited de records de historial `{t,v,r,s}` (time/value/trendFlags/status) desde un cursor `BIHistory`/`BHistoryTimeQuery` | `WebChartQueryServlet.java:133-203` |
| `/schedule/<escaped-ord>` | transiciones `nextEvent()` de un `BControlSchedule` en un rango de días | `:246-263` |
| `/boxTable/<escaped-ord>` | streaming genérico de filas `BITable` | `:265-324` |

**Gotcha — ruta muerta**: `/boxTable/` no tiene NINGÚN caller en el JS `rc/` inspeccionado — presente para integraciones
externas/futuras, no la usa la UI actual. `[CERT]`

**`WebChartFileServlet`** persiste `.chart`/`.csv` solo por `doPost`, exige `hasOperatorWrite()` (`:37`), re-serializa el
JSON posteado vía `com.tridium.json.JSONObject` antes de escribir a `BFileSystem`, con guard anti-traversal
(regex `chartNameFilter` `[|]|([.][.])`, `:32`). `[CERT]`

**`BChartFile`** (`BChartFile.java`) es un `BDataFile`+`BITextFile` con `@FileExt(name="chart")` — un **archivo JSON de
texto plano** en el file space de la station, NO un `BComponent`. Hard-registra el orden de agentes:
`agents.toTop("mobile:MobileHistoryAppChartView"); agents.toTop("webChart:ChartWidget")` (`:44-46`) — confirma que
`BChartWidget` es el agente primario para abrir un `.chart`. `[CERT]`

**`BWebChartQueryRpc`** (`BComponent`) expone métodos `@NiagaraRpc` que el JS llama vía `baja.rpc()`: `getSourceList`,
`getInfo` (metadata bulk: capacities/facets/record-types/timezones), `getChartSettings`, `getPermissions`,
`getCurrentTime`… Todos `permissions="unrestricted"` en la capa RPC — los servlets y la resolución de `BOrd`
(`OrdTarget.canRead()`) hacen el chequeo real de permisos. `[CERT]`

**`BIChartFactory`** (`javax/baja/webChart/BIChartFactory.java:9`, marker `extends BIJavaScript`) es el **punto de
extensión**: módulos de terceros registran factories de series adicionales — el lado Java del tag JS
`webChart:IChartFactory` (§199.3). `BWebChartTimeRange`/`...Type` es el value type canónico de rango temporal
(enum de 12 períodos: `auto`/`today`/`last24Hours`/`weekToDate`/`lastMonth`/`yearToDate`… + `startFixed`/`endFixed`)
compartido por wire entre RPC Java y el modelo JS. `[CERT]`

## 199.3 — El motor JS: modelo de series y escalas `[CERT]`

El contrato wire servlet↔cliente está versionado en ambos lados: `WebChartQueryServlet.java:51` `Version("1")` ↔
`webChartUtil.js:323` `"application/vnd.tridium.webChart-v1+json"`. `[CERT]`

**Jerarquía del modelo**: `BaseModel` (abstracto) → `LineModel` (agrega `timeRange`/`timeScale`/`delta`/`live`,
`model/LineModel.js:20-35`). `ChartWidget` instancia `LineModel` directo. `[CERT]`

**Series (`BaseSeries` + subclases)** — una por tipo de fuente de datos:

| Series | Fuente | Mecanismo | file:line |
|---|---|---|---|
| `ServletSeries` | historial (default) | GET `/webChart/query/data/<ord>` vía `modelUtil.chunkData` | `model/ServletSeries.js:127` |
| `ScheduleSeries` | `BControlSchedule` | GET `/webChart/query/schedule/<ord>` | `model/ScheduleSeries.js:147` |
| `PointSeries` | punto LIVE (sin historial) | **suscripción BajaScript directa, sin llamar al servlet** | `model/PointSeries.js:242-280` |
| `ExternalSeries` | CSV import/drag-drop | array en memoria, parseado del CSV client-side | `model/ExternalSeries.js:98-193` |

**`seriesFactory.js`** es el dispatcher (chain-of-responsibility, `:351-756`) que elige la subclase para un ord/valor
dropeado: history-ord → punto-con-history-ext → virtual → non-point-con-hijos (búsqueda BQL) → `.chart` file → CSV →
schedule → OrdList → **registry factory** (último recurso, hook extensible):

```js
// model/seriesFactory.js:44, 729-755
chartFactoryTypeSpec = 'webChart:IChartFactory';
reg.resolveFirst(type, { tags: [chartFactoryTypeSpec] }).then(function (ChartFactoryConstructor) { ... });
```

Confirma el punto de extensión dual-sided end-to-end: Java `BIChartFactory` (§199.2) ↔ JS tag `webChart:IChartFactory`.
`BaseChartFactory.js` es la base que tales factories extienden. `[CERT]`

**Escalas**: `BaseScale` envuelve un `d3.scale.linear()` (`model/BaseScale.js:29`); `TimeScale` (X compartido, usa
`d3.time.scale()`) y `ValueScale` (Y) extienden. `BaseModel` soporta **múltiples value scales** (primaria/secundaria),
agrupadas por unidades coincidentes (`BaseModel.js:1003-1055`). `[CERT]`

**`samplingUtil.js`** — rollup de historial client-side (`rollup`, `:180-306`): agrupa puntos en time-slices de
`duration` ms, agrega por `samplingType` (`average`/`sum`/`max`/`min`), redondea valores discretos (enum/bool) al ordinal
más cercano, preserva semántica de gaps. `configureAutoSample` (`:157-168`) auto-togglea el sampling alrededor de un
umbral (`webChartUtil.getMaxSamplingSize()` = 2500, §199.5). `[CERT]`

## 199.4 — Settings y field editors: monta sobre `webEditors` `[CERT]`

**Gotcha — `ChartSettings` usa `baja.Component` como estructura de datos client-side, no como componente de station.**

```js
// sources/decompiled/webChart-ux/rc/ChartSettings.js:60-62
that.$seriesListSettings   = new baja.Component();
that.$valueScaleListSettings = new baja.Component();
that.$chartSettings        = new baja.Component();
```

`saveToJson`/`loadFromJson` (`:184-236`) BSON-encodean/decodean estos 4 componentes a las claves
`settings.{chart,layers,sampling,scales}` del `.chart`. El "modelo de settings" piggybackea en la maquinaria de slots
dinámicos de BajaScript para tener property-sheet gratis — nunca es un `Type` Niagara registrado. `[CERT]`

`SettingsCommand.js` abre un `TabbedEditor` de 4 tabs (Series/Axis/Layers/Sampling), cada uno con un
`SimplePropertySheet` bound a uno de esos `baja.Component`. `SimplePropertySheet` (`fe/SimplePropertySheet.js:10-28`) es
un `nmodule/webEditors/rc/wb/PropertySheet` recortado (`showHeader:false, nested:true`), y `SamplingPeriodEditor`
(`fe/SamplingPeriodEditor.js:10`) extiende `nmodule/webEditors/rc/fe/baja/OverrideRelTimeEditor` — **confirma que
webChart cabalga directo sobre el framework de field editors `webEditors`**, no rueda el suyo. `[CERT]`

`optionsManager.js` (`:17-112`) es aparte: un wrapper de `window.localStorage` para las "default chart options"
persistidas en el browser — distinto de los settings por-`.chart`. `[CERT]`

## 199.5 — Interacciones: zoom, time-range, export, sampling `[CERT-doc]` + `[CERT]`

- **Zoom** (`sources/manuals/webChart-docs/ZoomingOnAChart-95A25314.html` `[CERT-doc]`): dos modos en la Command Bar —
  *Home Zoom* (X al dataset primario) y *Time Zoom* (X al Time Range). Rueda sobre un eje → zoom de ese eje; `Alt`+rueda
  → ambos. Opción Axis "Data Zoom Scope" (`primary`/`all`). Implementado en `line/ZoomLayer.js:19-104` con
  `d3.behavior.zoom()`, decidiendo X-only/Y-only/XY por modificadores `altKey`/`shiftKey`; al detectar zoom manual llama
  `graph.widget().manualZoom()` que apaga los toggles de auto-zoom (`ChartWidget.js:1264-1277`). `[CERT]`
- **Time Range** (`ChangingChartTimeRange-95A066B6.html` `[CERT-doc]`): dropdown → diálogo Start (requerido)/End (opcional;
  en blanco = sigue live). Mapea a `BWebChartTimeRange.startFixed/endFixed` y al `$timeRangeTab`
  (`ChartWidget.js:301-349`). `[CERT]`
- **Export** (`ExportingAChart-95A0FF3B.html` `[CERT-doc]`): a `chart`(JSON)/`csv`/oBIX; ORD Absoluto vs **Relativo** desde
  4.6 (chart embebible en Px relativo). Matchea `ChartWidget.makeExport`/`$getSaveOptions` (`ChartWidget.js:628-679,1467-1476`)
  y `export/exportUtil.js`. `[CERT]`
- **Sampling** (`AboutSampling-E7DD848B.html` `[CERT-doc]`): el rollup se activa AUTO al superar **2500** puntos enfocados
  y se desactiva al bajar; bucket = duración/2500 redondeado al incremento "nice" (2.5h→3h, alineado a reloj). Coincide
  exacto con `samplingUtil` (§199.3) y `getMaxSamplingSize()` = 2500 (`ChartWidget.js:403,411`; `samplingUtil.js:161`). `[CERT]`
- **Save** (`SavingAChartFile-7EFB464D.html` `[CERT-doc]`): solo habilitado en un `.chart` ya exportado; sobrescribe
  in-place. Matchea `ChartWidget.doSave` POSTeando a `/webChart/file/save/<path>` (`ChartWidget.js:1195-1209`) →
  `WebChartFileServlet.doPost` (§199.2). `[CERT]`

## 199.6 — `BCircularGaugeWidget`: el gauge de valor único `[CERT]`

`BCircularGaugeWidget` (`.../ux/gauge/BCircularGaugeWidget.java:24-39`) sigue el MISMO patrón que `BChartWidget` — un
`BSingleton` agente (`@AgentOn` numeric/enum/bool/schedule) apuntando a `module://webChart/rc/gauge/CircularGaugeWidget.js`.
Pero implementa `BIFormFactorCompact`+`BIOffline` (no `BIFormFactorMax`/`BIChartableHistoryAgent`): es un tile chico de
dashboard, no una view de chart. El JS `gauge/CircularGaugeWidget.js:10,58-71` es un `bajaux/Widget` plano que se suscribe
a **UN punto live** (`subscriberMixIn`) y renderiza un arco SVG con D3 (`min`/`max`/`ticks`) — sin history query, sin time
range, sin sampling. Mucho más fino que el chart de serie temporal. `[CERT]`

## 199.7 — Connections

- **[Block 194]** (bajaux sin PxMedia): este bloque CONFIRMA su hipótesis — webChart renderiza con D3/SVG en
  `bajaux/Widget`, sin PxMedia/Swing; el `.px` solo hostea el agente `BChartWidget` que carga el JS.
- **[Block 192]** (§14: charts en kitPx, no bajaui core): correcto — el charting vive en su propio módulo `webChart`
  (bajaux), separado de bajaui/kitPx; monta sobre `webEditors` para los field editors (§199.4).
- **[Block 193]** (los 9 bindings kitPx): un chart en un `.px` es un widget dropeado con su ord bound; la data la sirve el
  servlet rt, no un ValueBinding kitPx — la data-binding del chart es interna al motor JS (series), distinta del binding PX.
- **[Block 198]** (sidebars, hermano D1): un `.chart` se abre por el agente `webChart:ChartWidget` (§199.2); en el editor
  PX, el `ChartWidget` es un widget más en el árbol/cell-sheet de B198.
- **[Block 197]** (síntesis 7 capas): X1 abre la sub-capa "charts", marcada como boundary en la síntesis.
- **Fuera de scope** (nombrados): `bajaux/Widget`, `nmodule/webEditors/rc/*` (field editors, base de settings),
  `nmodule/history/*` (BIHistory/queries que alimentan `ServletSeries`), `d3`/`jquery`/`moment` (libs JS) — feeds de X5 (bajaux).
