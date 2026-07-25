# Bloque 254 — Chart clásico (IV): el mapa de consumidores y el veredicto clásico vs `webChart`

> **Qué documenta**: QUIÉN consume realmente el charting clásico (`javax.baja.chart`) en el universo de 926
> jars, qué construye cada consumidor, y **cuándo Niagara N4 usa el chart clásico y cuándo `webChart`**.
> Cierra el gap **H4** del focus `px-chart-classic` — el gap transversal que motivó el focus.
>
> **Contiene un matiz §14 a [Bloque 253] §253.5** (el registro de agentes para ejes existe, pero el consumidor
> más pesado NO lo usa) y **un matiz a [Bloque 253] §253.9** (los permisos sí se imponen, pero una capa más
> arriba). Ver §254.3 y §254.5.
>
> **Alcance**: relacional. Enumera consumidores y decide la pregunta clásico-vs-web; NO abre en profundidad los
> módulos consumidores (`analytics`, `history`, `seriesTransform` tienen sus propios subsistemas y quedan
> fuera de este focus).
>
> **Fuentes**:
> - **`module-navigator`** sobre 926 jars / ~51k clases — `grep 'import javax.baja.chart' -n 500` → **55
>   archivos en 8 módulos**.
> - Decompilado vineflower de los módulos consumidores, raíz `/home/cristian/modules/Prototipos/modulos/organized/`.
>
> **Método**: barrido delegado (tier `sonnet`, 80 llamadas de herramienta) + verificación inline del driver:
> **12 tokens load-bearing** re-verificados, incluidas **dos ausencias probadas** (`@AgentOn` en los ejes de
> analytics; `@Deprecated` en todo el módulo `chart`). Marcadores: `[CERT]` = fuente primaria;
> `[INFER]` = deducción.
>
> **TIPO DE BLOQUE — MIXTO: evidencia relacional + VEREDICTO** (§11). Declararlo importa porque el ratio
> `[INFER]/[CERT]` cerró en **0.59**, por encima del 0.5 que en un bloque de evidencia puro señalaría
> agotamiento. Acá la mitad relacional (§254.1, §254.2, §254.4-§254.7) es evidencia dura, mientras que el gap
> H4 pedía explícitamente un **veredicto** ("¿cuándo usa N4 cada sistema?") — y un veredicto se construye
> deduciendo sobre evidencia, no citándolo de una línea. La lectura honesta: el ratio alto viene de §254.3 y
> §254.8, no de falta de fuente. Aun así, la evidencia RELACIONAL de este gap **sí** quedó sustancialmente
> agotada: los 55 archivos consumidores están enumerados y no hay más que enumerar.

---

## 254.1 — El mapa completo: 8 módulos, 55 archivos `[CERT]`

`module_nav grep 'import javax.baja.chart' -n 500` `[CERT]`

| Módulo | Qué construye con el chart clásico |
|---|---|
| **`analytics-wb`** | El consumidor pesado: 14+ clases — `BAggregationChart`, `BAverageProfileChart(+Pane)`, `BEquipmentOperationChart`, `BLoadDurationChart`, `BRankingChart`, `BRelativeContributionChart`, `BSpectrumChart(+Canvas/Legend/Pane)`, ejes propios `BDaysAxis`/`BHoursAxis`/`BTimeOfDayAxis`, y `BAnalyticChartBinding` |
| `chart-wb` | El propio módulo (incluye `BHxPxChartPane`, `BPdfChartPane`, `BAxisContainer`, `BResourceManager` + clases de test) |
| **`history-wb`** | `BHistoryChart` (la vista de usuario), `BChartEditor` (el builder), `BHxHistoryChartBuilder`, `BLiveHistoryChartEditor`, `BLiveValueChartBinding`, `LiveChartSeries` |
| **`seriesTransform-wb`** | `BTransformChartBindingCollection`, `BChartDescriptor`, `BChartConfigPane`, `BDescriptorEditor`, `BMwTransformChart` |
| `pxEditor-wb` | `BMwChart`, `BMwTimePlot` — los widgets del wizard *Make* ([Bloque 201]) |
| **`honeywellSpyderTool`** | `BPieChartPane` — ver §254.7 |
| `history-rt` | `BHistoryPointListItem` (solo importa `BAxisBound`, no renderiza) |
| `docSource-doc` | artefacto de documentación del anterior |

`[INFER]` El patrón: **todos los consumidores reales son módulos `-wb`**. Los dos `-rt` de la lista solo tocan
`BAxisBound`, que es justamente una de las clases serializables que viven en el runtime ([Bloque 253] §253.5).
Confirma desde el lado del consumo lo que H8 va a mirar desde el lado del módulo.

## 254.2 — `analytics-wb`: el chart clásico es la base de los gráficos de Analytics `[CERT]`

```java
public class BAggregationChart extends BChart {
```
`analytics-wb/…/BAggregationChart.java:45` `[CERT]`

Los charts de Analytics **no envuelven** al chart clásico: lo **extienden**. `BAggregationChart` añade slots
propios (`displayValueLabels`, `barWidthPercent`, `pen`, `stroke`, `font`) y sobreescribe `doPaint()` — es
decir, usa exactamente el hook que [Bloque 251] §251.1 identificó como el único punto de entrada de render
disponible (porque `paint()` es `final`).

Otros dos verificados por el barrido:
- `BAverageProfileChart` — mapa de calor día/hora; bucketiza filas de history en un
  `HashMap<String, HashMap<BRelTime, BDouble>>` por serie, con un `BTimeOfDayAxis` propio en X.
- `BEquipmentOperationChart` — línea de tiempo de estados on/off como barras horizontales, con `BDiscreteAxis`
  en Y.

`[INFER]` Esto responde una pregunta que el corpus tenía abierta desde [Bloque 66]-[Bloque 68]: la capa de
Analytics de Niagara **no tiene motor gráfico propio en Workbench** — es una capa de semántica de negocio
(agregación, perfiles, ranking, espectro) montada sobre el mismo `BChart`/`BChartPane` de 2010 documentado en
B251/B252. Todos los gotchas de esos bloques (tope de 12 colores, `slurp` completo, doble búfer solo AWT) son
gotchas **de Analytics también**.

## 254.3 — §14 (matiz) a [Bloque 253] §253.5: el punto de extensión existe pero NADIE lo usa `[CERT]`

[Bloque 253] §253.5 estableció — y sigue siendo cierto — que `BAxisSpec.toAxis()` elige el tipo de eje
consultando el registro de agentes (`Sys.getRegistry().getAgents(...)` filtrado por `BAxis.TYPE`), de modo que
un módulo *puede* registrar su propio `BAxis` para un tipo de valor.

**La evidencia de este gap muestra que el consumidor más pesado NO ejerce ese mecanismo:**

```java
@NiagaraType
public class BDaysAxis extends BContinuousAxis {
```
`analytics-wb/…/ui/chart/BDaysAxis.java:12-13` `[CERT]`

**Ausencia probada**: ni `BDaysAxis` ni `BHoursAxis` declaran `agent = {@AgentOn(...)}` — solo `@NiagaraType`
(verificado por conteo de `AgentOn` en ambos archivos → **0**) `[CERT]`. Se instancian **a mano** dentro del
constructor del chart que los necesita (`BLoadDurationChart` crea su `BHoursAxis`; `BAverageProfileChart` crea
su `BTimeOfDayAxis`).

**Lectura corregida** `[INFER]`: la capacidad declarativa de B253 es real pero **queda como camino no
transitado**. Analytics extiende el chart clásico por **construcción programática**, no por registro. Sumado a
lo de [Bloque 252] §252.5 (renderers solo por setter), el cuadro honesto del módulo es:

| Capa | Mecanismo disponible | ¿Se usa en la práctica? |
|---|---|---|
| Bindings del chart | `@AgentOn(types={"chart:Chart"})` | **Sí** — los 2 del módulo + `seriesTransform` (§254.6) |
| Tipo de eje según tipo de dato | registro de agentes | **No** — el mayor consumidor construye sus ejes a mano |
| Renderers (ejes/swatches) | setters Java | n/a (no hay alternativa) |

Ninguna de las dos afirmaciones previas se retira; se acota su alcance con evidencia de uso real.

## 254.4 — `BChartRenderLimitConfiguration`: los límites existen, el enforcement NO se pudo verificar `[CERT]`

Vive en `analytics-rt` y se monta como propiedad **oculta** (flags=4) de `BAnalyticService`:
`chartRenderCapacity` (`analytics-rt/…/BAnalyticService.java:361`).

Límites por tipo de gráfico, en filas, con facets `min=1000` / `max=99999999` `[CERT]`
(`analytics-rt/…/BChartRenderLimitConfiguration.java:104-111`):

| Propiedad | Default | | Propiedad | Default |
|---|---|---|---|---|
| `aggregationChart` | **3.000** | | `spectrumChart` | 10.000 |
| `averageProfileChart` | **3.000** | | `rankingChart` | **250.000** |
| `analyticWebChart` | 15.000 | | `relativeContributionChart` | **250.000** |
| `loadDurationChart` | 15.000 | | `equipmentOperationChart` | **250.000** |

`[INFER]` La hipótesis con la que se abrió este gap — que estos topes son la mitigación del `Tables.slurp()`
de [Bloque 251] §251.3 — es **coherente** con los valores: los charts que agregan/perfilan (los que más
cómputo por fila hacen) tienen el tope más bajo, 3.000 filas.

**Pero el enforcement no está verificado, y hay que decirlo**: `rg 'getChartRenderCapacity|chartRenderCapacity'`
sobre todo el código decompilado disponible devuelve hits **solo dentro de `BAnalyticService`** — ningún
llamador visible. `[INFER]` O el control vive en el handler de queries de `analytics-rt` sin quedar visible en
el decompilado, o en código ofuscado. **Qué ocurre al superar el límite (truncado silencioso, error, o
degradado) queda como UNVERIFIED** — no hay evidencia para afirmarlo.

## 254.5 — `history-wb`: la vista real, y dónde SÍ está el control de acceso `[CERT]`

Dos clases que conviene no confundir:

- **`BHistoryChart`** — **la vista que ve el usuario**. `@AgentOn(types = {"history:IHistory",
  "history:HistoryExt"}, requiredPermissions = "r")` (`history-wb/…/BHistoryChart.java:99-101`) `[CERT]`.
  Construye un `BChartPane` directamente.
- **`BChartEditor`** — `extends BEdgePane`, **sin** `agent=`: no es una vista navegable. Es el panel
  constructor (árbol de histories a la izquierda, `BChartPane` a la derecha), instanciado por
  `BHxHistoryChartBuilder`.

**Matiz §14 a [Bloque 253] §253.9** `[CERT]`: allí registré que las 9 clases del paquete `binding` no tienen
**ningún** chequeo de permisos. Sigue siendo cierto — pero la conclusión implícita ("el chart no controla
acceso") es demasiado fuerte: el control está **una capa más arriba**, en el `@AgentOn` de la vista, vía
`requiredPermissions = "r"`. `[INFER]` El modelo es el estándar de Niagara: la vista exige permiso de lectura
sobre el history; el binding, ya dentro de una vista autorizada, no vuelve a chequear.

## 254.6 — `seriesTransform-wb`: el único uso real del multi-serie `[CERT]`

`BTransformChartBindingCollection` — el implementador único que [Bloque 253] §253.6 encontró — declara
**dos** agentes: `@AgentOn(types={"chart:Chart"})` y `@AgentOn(types={"seriesTransform:TransformGraph"})`, y
tres slots: `query` (`BTransformQuery`), `yAxesDescriptors` (`BDescriptors`) y `xColumn`
(`BColumnIdentifier`).

Cómo produce N series: itera la lista `BDescriptors`, y **cada `BChartDescriptor`** (un `BComponent` con
`brush`, `pen`, `seriesName`, `yColumn`) se mapea a un `TableSeries`. La columna X por defecto es
`COL_TIMESTAMP`. `BChartConfigPane` (`extends BLabelPane`) es la UI de configuración: editor de query BQL a la
izquierda, editor de lista de descriptores a la derecha.

`[INFER]` Es exactamente el caso de uso para el que existía el hook: **una query, N columnas, N series**.

## 254.7 — El chart clásico dentro de la herramienta OEM Honeywell `[CERT]`

```java
import javax.baja.chart.BChartPane;
import javax.baja.chart.BPieChart;
…
public class BPieChartPane extends BCanvasPane {
```
`honeywellSpyderTool/…/BPieChartPane.java:10-11,27` `[CERT]`

`honeywellSpyderTool` — la herramienta de comisionamiento Spyder documentada en el hilo OEM del corpus — usa
`BPieChart` y `BChartPane` directos. `[INFER]` Cierra un círculo: el mismo motor de charting de 2010 que
sostiene Analytics también dibuja en la herramienta de puesta en marcha de los controladores Spyder. Los
gotchas de B251/B252 alcanzan también a esa herramienta.

## 254.8 — VEREDICTO: qué decide entre clásico y `webChart` `[CERT]`

**Lo decide el PERFIL, no un switch de runtime.**

| | Chart clásico (`javax.baja.chart`) | `webChart` ([Bloque 199]) |
|---|---|---|
| Perfil | **Workbench / Swing** | **Browser / móvil (bajaux)** |
| Naturaleza | `BWidget`/`BPane` pintados por el UI manager de Swing | JS/D3 servido por servlet |
| Evidencia | Todos los consumidores de §254.1 son `-wb` y extienden `BWidget`/`BPane` | `BChartWidget` (`webChart-ux`) agenta sobre `history:HistorySpace`, `history:IHistory`, `webChart:ChartFile`, `control:NumericPoint`, `schedule:NumericSchedule` y sirve `module://webChart/rc/ChartWidget.js` |

**Los puentes** — módulos que sirven a ambos mundos con clases distintas:
- `analytics-wb`: `BAnalyticChartBinding` (clásico, Workbench) **y** `BAnalyticWebChartBinding` (agenta sobre
  `bajaui:Widget` + `baja:Component`, envuelve `BWebWidget` para browser). Dos tipos separados, un módulo.
- `history-wb`: `BHistoryChart` para Workbench; `BChartFile` (el `.chart` JSON) agenta a
  `webChart:ChartWidget` y a `mobile:MobileHistoryAppChartView` para browser/móvil.

`[INFER]` **No existe conversión ni ruteo automático entre los dos sistemas.** Un módulo que quiera gráficos
en ambos perfiles escribe dos implementaciones. Es el costo real de arrastrar dos motores.

### ¿Está deprecado el chart clásico en 4.14? NO `[CERT]`

**Ausencia probada**: `rg -c '@Deprecated|@deprecated'` sobre `chart-rt/vineflower` + `chart-wb/vineflower`
→ **0 archivos**. Tampoco hay comentarios de "use webChart instead" en ninguna clase decompilada del módulo, ni
en los consumidores de §254.1.

`[INFER]` El chart clásico está **plenamente vigente en N4.14**, sin señal de deprecación en el código. No es
legacy a punto de morir: es el motor de charting del perfil Workbench, y `webChart` es el del perfil web. Son
paralelos, no sucesivos — lo que corrige la lectura intuitiva (y la que yo mismo insinué en [Bloque 252] al
hablar de "generación anterior") de que uno reemplaza al otro.

## 254.9 — Conexiones

- **[Bloque 253]** — matizado dos veces: §254.3 (el registro de agentes para ejes no se usa en la práctica) y
  §254.5 (los permisos existen, en la vista, no en el binding).
- **[Bloque 252]** — cierra el hilo de su §252.5: el módulo no es "pre-agentes", y tampoco es legacy deprecado
  (§254.8).
- **[Bloque 251]** — los gotchas documentados allí (12 colores, `slurp`, búfer AWT) **se propagan a Analytics,
  a la vista de histories y a la herramienta Spyder**, por herencia directa de `BChart`.
- **[Bloque 199]** (`webChart`) — el veredicto §254.8 es la respuesta a la pregunta transversal que abrió este
  focus.
- **[Bloque 201]** (`make/` wizard) — `BMwChart`/`BMwTimePlot` confirmados como consumidores de `pxEditor-wb`.
- **[Bloque 66]-[Bloque 68]** (analytics) — §254.2 responde de qué está hecho el render de Analytics en
  Workbench.
- **Gaps abiertos**: H5 (`com.tridium.chart` — `BAxisContainer`, `BoundChartSpec`, `BoundTimeSeries`,
  `BPdfChartPane`, `BHxPxChartPane`), H6 (PDF + HX), H7 (tests), H8 (split rt/wb).
