# Bloque 251 — Chart clásico (I): `BChart`, la jerarquía de tipos y el modelo de datos

> **Qué documenta**: el NÚCLEO del sistema de charting **clásico** de Niagara N4 — la clase base `BChart`,
> los 7 tipos concretos de gráfico, y el modelo de datos (`ChartModel` / `Series` / `TableSeries` /
> `JoinTable` / `ChartSpec` / `TrendFlags` / `ChartModelEvent`). Cierra el gap **H1** del focus
> `px-chart-classic`.
>
> **Alcance**: el paquete público `javax.baja.chart` (31 clases en `-wb` + 3 en `-rt`). NO cubre los ejes ni
> el render de ejes (gap H2), ni el binding a histories (gap H3, paquete `javax.baja.chart.binding`), ni la
> impl privada `com.tridium.chart` (gap H5). El sistema de charting **moderno** (`webChart`, bajaux/D3) está
> en el [Bloque 199] y es un sistema DISTINTO — ver §251.10.
>
> **Fuentes** (decompilado vineflower, pipeline canónico; los pipelines `decompiled/` y `pipeline/procyon/`
> son duplicados de las mismas clases y NO se leyeron):
> - `$WB` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-wb/vineflower/javax/baja/chart/`
> - `$RT` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-rt/vineflower/javax/baja/chart/`
> - API pública contrastada con `niagara_help package javax.baja.chart` (35 clases) — fuente Tridium oficial.
> - **Nota de fuentes**: `docSource` (fuente original de Tridium con javadoc) **NO cubre** `javax.baja.chart`
>   — verificado: 0 archivos bajo `docSource-doc/extracted/**/javax/baja/chart/`. Para este módulo NO existe
>   el atajo de la fuente original; toda la evidencia `[CERT]` de este bloque es decompilado + API pública.
> - El jar NO está ofuscado (`module_nav module chart-wb` → `ZKM: no`, bytecode v52 / Java 8), así que el
>   decompilado es legible entero — a diferencia de `easyBinding` ([Bloque 207], parcialmente ofuscado).
>
> **Método**: barrido delegado (tier `sonnet`) sobre las ~34 clases del paquete, devolviendo hallazgos
> citados; el driver re-verificó **13 tokens load-bearing** leyendo la fuente citada, y **corrigió 2
> afirmaciones del barrido** que no resistieron la verificación (ver §251.9). Marcadores: `[CERT]` = leído en
> la fuente primaria (`file:line`); `[CERT-doc]` = API pública oficial de niagara-help; `[INFER]` = deducción.

---

## 251.1 — `BChart`: qué es y qué NO es `[CERT]`

`BChart` es **un widget, no un componente de datos**:

```java
public abstract class BChart extends BWidget {
```
`$WB/BChart.java:19` `[CERT]`

Tres propiedades definen su contrato:

| Rasgo | Evidencia | Consecuencia |
|---|---|---|
| **Cero slots propios** | No declara `@NiagaraProperty`/`@NiagaraAction`/`@NiagaraTopic`; solo `@NiagaraType` | La configuración vive en `BChartPane` y en los subtipos concretos, no en la base |
| **`paint()` es `final`** | `public final void paint(Graphics g)` — `$WB/BChart.java:143` | Los subtipos NO pueden interceptar el pintado; solo pueden implementar el hook `doPaint()` |
| **Exige un padre `BChartPane`** | `paint()` comprueba `parent != null && parent instanceof BChartPane` (`$WB/BChart.java:144-145`); `getPane()` castea el padre dentro de un `try` y convierte el fallo en `IllegalStateException("ChartPane parent required.")` — `$WB/BChart.java:39-45` | Un `BChart` fuera de un `BChartPane` no es un widget degradado: es un error duro |

**El reparto de responsabilidades** `[INFER — deducido del reparto de métodos verificado]`: `BChart` sostiene el
modelo, reacciona a sus eventos y expone `doPaint()`; `BChartPane` es el que manda (layout, ejes, zoom, pan,
fondo, asignación de colores); `BChartCanvas` posee el búfer de píxeles y recorre los `BChart` hijos llamando
a `paint(cg)` sobre cada uno (`$WB/BChartCanvas.java:263`).

### `BChart.ChartSupport` — el puente modelo↔chart `[CERT]`

```java
   public abstract static class ChartSupport {
      public BChart chart;
```
`$WB/BChart.java:219-220` `[CERT]`

Clase interna abstracta que **`ChartModel` extiende** (§251.3). Su función es dar a todo modelo un
back-pointer a su chart, puesto y quitado por el ciclo de instalación. `[INFER]` Es el mecanismo que impide
que un mismo modelo quede instalado en dos charts a la vez — el back-pointer es único por instancia.

## 251.2 — Los 7 tipos concretos: ninguno es un subtipo delgado `[CERT]`

Los siete extienden `BChart` **directamente** (jerarquía plana, un solo nivel) y **todos llevan lógica real**.
Esto contrasta con el patrón dominante en el resto del corpus OEM, donde abundan los thin-subclass (p. ej. los
descriptores BACnet de [Bloque 246]).

| Tipo | Slots propios | Qué sobreescribe | Sustancia propia |
|---|---|---|---|
| `BLineChart` | ninguno | `doPaint()` (`synchronized`) | Proyecta `x,y` vía `xaxis.toDisplaySpace()` y traza `strokeLine()`. **Deduplicación de puntos colineales** con tolerancia de 2 px NO configurable (`$WB/BLineChart.java:86-117`). Cola interpolada punteada hasta `BAbsTime.now()` si `interpolateTail && isTime` |
| `BAreaChart` | ninguno | `doPaint()` | Construye listas de puntos e inserta anclas inferiores en las transiciones `isHidden`/`isStart`; rellena un `BPolygonGeom` con `ChartUtil.makeGradient(series.getBrush())` |
| `BBarChart` | `pen`, `stroke` | `doLayout()` + `doPaint()` | Calcula `clusterWidth`/`barWidth`/`overlapWidth` desde el espaciado de ticks del eje. Usa `JoinTable` para alinear multi-serie. **`overlap = 0.2` hardcodeado**, no expuesto como slot |
| `BStackedBarChart` | — | `updateAxes()` + `doLayout()` + `doPaint()` | **El único que sobreescribe `updateAxes()`**: auto-escala el eje Y con `JoinTable.getMinRow()/getMaxRow()` (totales por columna sumados). Apila rastreando `barTop` por clúster |
| `BPieChart` | `font`, `showSliceLabels`, `pen`, `stroke` | `usesXAxis()`/`usesYAxis()` → `false` (`$WB/BPieChart.java:141,146`) + render | **El más complejo**: grid de tortas con conteo de columnas por raíz cuadrada, arcos `BPathGeom` por porción, posicionado de etiquetas en la circunferencia. Clases internas `SingleValueSeries`, `Pie`, `Slice`, `LabelLocation`. Constructor alternativo `BPieChart(String[], double[])` |
| `BDiscreteLineChart` | — | `doPaint()` | Step-lines (pares `strokeLine` horizontal→vertical). `setStartIndex(int,int)` lo llama `BChartCanvas.doLayout()`, que enumera los hijos `BDiscreteLineChart` y les asigna índices secuenciales para apilarlos con offset vertical |
| `BDiscreteAreaChart` | — | `doPaint()` | Relleno escalonado; inserta segmentos horizontal→vertical explícitos por transición. Tiene los campos `startIndex`/`discreteCount` pero **no los aplica** en la construcción del polígono `[CERT]` |

**El punto arquitectónico** `[INFER]`: el tipo de gráfico NO es una propiedad de configuración ni una
estrategia inyectable — es una **subclase Java distinta**. Cambiar de línea a barras significa cambiar de
clase, no cambiar un enum. Es la diferencia estructural más grande contra `webChart` ([Bloque 199]), donde el
tipo de serie sí se elige por factory JS (`seriesFactory`, 4 tipos).

## 251.3 — El modelo de datos: tres modelos, una abstracción `[CERT]`

```java
public abstract class ChartModel extends BChart.ChartSupport {
```
`$WB/ChartModel.java:19` `[CERT]`

`ChartModel` es **clase abstracta, no interfaz** — dato relevante porque hereda de `ChartSupport` (§251.1) y
por lo tanto todo modelo ES un objeto de soporte con back-pointer al chart. Contrato abstracto mínimo:
`getSpecCount()` y `getSpec(int)`. Provee los disparadores `fireSpecModified` / `fireSpecAdded` /
`fireSpecRemoved` / `fireModelModified`, y un `export()` que fusiona todas las series en un `BDataTable`
pivotado por columna clave.

Tres implementaciones, tres propósitos:

| Modelo | Ubicación | Rol |
|---|---|---|
| `SimpleChartModel` | `$WB/SimpleChartModel.java` | Modelo utilitario programático: `ArrayList<ChartSpec>` (capacidad inicial 3). `add(ChartSpec)` llama a `spec.setModel(this)`. **No dispara eventos** en alta/baja |
| `BoundChartModel` | `$WB/binding/BoundChartModel.java` — paquete `binding`, **no** el paquete raíz | El modelo con datos VIVOS. `doSyncBindings()` recorre los slots `BChartBinding` del chart y crea un `BoundChartSpec` por binding atado. Un `BChartBindingCollection` puede producir varias `Series` por binding. `getSpecCount()`/`getSpec()` son `synchronized`. **Reutiliza ejes**: `findAxis()` busca en el pane un eje compatible antes de crear otro |
| (`BoundChartSpec`) | `$WB/com/tridium/chart/BoundChartSpec.java` | La subclase concreta de spec que instancia `BoundChartModel` — vive en el paquete PRIVADO de Tridium, no en la API pública |

`BoundChartModel` es la bisagra hacia el gap **H3** y se documenta allí; aquí queda registrado que es el único
de los tres que conecta con datos reales.

### `Series` y `TableSeries` — de dónde salen los números `[CERT]`

`Series` es abstracta: **columna 0 = X, columna 1 = Y**. Métodos clave: `getValue(int row, int col)` →
`Object`, `getTrendFlags(int row)` (0 por defecto), `getMin(int col)`/`getMax(int col)`,
`getBrush()`/`setPen()`.

`TableSeries` es la implementación respaldada por una tabla Baja — y trae **la decisión de diseño más cara del
módulo**:

```java
      this.table = Tables.slurp(backingTable);
```
`$WB/TableSeries.java:43` `[CERT]`

`slurp` = **materialización ansiosa completa** del `BITable` en memoria al construir la serie. No hay
streaming, no hay paginación, no hay ventana. `[INFER]` Una consulta de trend grande entra íntegra al heap del
Workbench antes de dibujar el primer píxel — límite práctico de escala del chart clásico, y explicación
plausible de por qué `analytics-rt` necesita una clase llamada `BChartRenderLimitConfiguration` (a confirmar
en H4).

Otros rasgos verificados de `TableSeries`: resuelve una columna `trendFlags` del backing table eligiendo entre
tres estrategias — `NumberToInt`, `DataValueToInt`, `StatusToInt` (`$WB/TableSeries.java:58-84`); convierte
unidades en el momento del acceso (`:446-449`); la clase interna `RowIndexColumn` mapea la columna 0 al índice
de fila secuencial (`BDouble`); y `getMax()` sobre columnas `BAbsTime` devuelve `BAbsTime.now()` cuando
`interpolateTail` está activo (`:162`).

## 251.4 — `JoinTable`: el pivote multi-serie `[CERT]`

`JoinTable` **no es un `BITable`** — es una estructura de pivote construida desde `Series[]`
(`$WB/JoinTable.java:15`). `make(Series[], keyColumn)` une todas las claves de todas las series, las ordena si
son `Comparable`, y arma una matriz `Object[seriesCount][keyCount]` con `null` en los huecos.

Lo consumen los tres tipos que necesitan alinear categorías entre series: `BBarChart`, `BStackedBarChart` y
`BPieChart`. `getMinRow()`/`getMaxRow()` suman los valores numéricos de todas las columnas por fila — que es
exactamente lo que `BStackedBarChart.updateAxes()` necesita para auto-escalar la pila.

**Gotcha verificado**: si el mínimo y el máximo dan ambos 0, el máximo se fuerza a 10:

```java
            max = 0.0;
         } else {
            max = 10.0;
```
`$WB/JoinTable.java:137-139` `[CERT]`

`[INFER]` Un dataset íntegramente en cero no rinde un eje degenerado 0–0: rinde un eje 0–10 inventado. Es una
decisión defendible para no dividir por cero, pero silenciosa — el gráfico miente sobre su escala sin avisar.

## 251.5 — `ChartSpec`: el 4-tuple que ata todo `[CERT]`

Clase Java plana, **sin anotaciones Niagara** (`$WB/ChartSpec.java`). Cuatro campos: `ChartModel model`,
`Series series`, `BAxis xaxis`, `BAxis yaxis`. Dos constructores: `ChartSpec(Series)` y
`ChartSpec(Series, BAxis, BAxis)`. El único método con efecto lateral es `setModel(ChartModel)`, que además
cablea `series.model = model`.

`[INFER]` `ChartSpec` es la unidad de composición del sistema: **una serie + sus dos ejes**. Un chart no
dibuja "datos", dibuja una lista de specs. No lleva orden ni metadatos de presentación — el color no vive
aquí, lo asigna el pane (§251.8).

Quién los construye: `SimpleChartModel.add()`, `BPieChart.setModel(String[], double[])` (un spec por par
etiqueta/valor) y `BoundChartModel.addSpec()`.

## 251.6 — `TrendFlags` y la fuga de `BStatus` hacia el render `[CERT]`

`TrendFlags` es una utilidad estática de bitmask que vive en el **runtime** (`$RT/TrendFlags.java`) — una de
las únicas 3 clases del paquete que la station necesita (ver H8). Bits verificados: `START = 1` (`:4`),
`HIDDEN = 4` (`:6`), `INTERPOLATED = 16` (`:8`), más `OUT_OF_ORDER = 2`, `MODIFIED = 8` y tres `RESERVED_*`.

El hallazgo interesante es el puente: la estrategia `StatusToInt` de `TableSeries` mapea `BStatus.getBits()`
**directamente** al int de trend flags, y las estrategias `DataValueToInt`/`NumberToInt` hacen OR con
`HIDDEN(4)` cuando el flag 16 del status está presente (`$WB/TableSeries.java:257-259, 274-276`).

`[INFER]` Consecuencia: los bits de estado de un punto (stale/disabled/alarm) aterrizan en el espacio de bits
de trend flags, y un status interpolado se traduce en **ocultar el punto** en el render. El estado de calidad
del dato y la decisión de dibujarlo comparten la misma palabra de bits — acoplamiento que un consumidor no
espera al leer solo la API pública.

## 251.7 — Eventos: la política rebuild-vs-refresh `[CERT]`

`ChartModelEvent` define 4 IDs: `MODEL_MODIFIED(0)`, `SPEC_ADDED(1)`, `SPEC_REMOVED(2)`, `SPEC_MODIFIED(3)`.

`BChart.modelModified(ChartModelEvent evt)` (`$WB/BChart.java:58`) discrimina uno solo:

- `SPEC_MODIFIED(3)` → `pane.refresh()` — recalcula ejes y repinta.
- **todos los demás** → `rebuild()` — reconstrucción completa de ejes y leyenda, diferida un frame.

`[INFER]` Es una optimización de un solo caso: cambiar los datos de una serie existente es barato; cualquier
cambio estructural (alta, baja, o modificación del modelo entero) paga la reconstrucción completa. Comparar
con el `PxEvent` bus de [Bloque 210], que clasifica 9 categorías con reclasificación fina — el chart clásico
resuelve lo suyo con un `if`.

## 251.8 — Hallazgos load-bearing (los que un integrador necesita saber) `[CERT]`

| # | Hallazgo | Evidencia | Por qué importa |
|---|---|---|---|
| 1 | **Tope duro de 12 colores con caída silenciosa a negro** | `DEFAULT_COLORS` tiene exactamente 12 entradas (`$WB/BChartPane.java:125-138`); si todas están en uso, `if (dup) { color = BColor.black; }` (`$WB/BChartPane.java:786-788`) | La serie 13 y siguientes se dibujan **todas negras**, sin warning y sin ciclado. Un dashboard con 15 puntos es ilegible por diseño |
| 2 | **`Tables.slurp()` materializa todo** | `$WB/TableSeries.java:43` | Techo de escala del chart clásico (§251.3) |
| 3 | **`ChartModel.export()` pisa el nivel del logger** | `log.setLevel(Level.INFO)` incondicional dentro de `export()` — `$WB/ChartModel.java:158` | Cada exportación sobreescribe la configuración externa del logger `chart.ChartModel`. Efecto lateral global desde un método de datos |
| 4 | **`BDiscreteLineChart` no es reentrante** | `private double lastX; private double lastY;` como campos de instancia (`$WB/BDiscreteLineChart.java:12-13`), y su `doPaint()` **no** es `synchronized` — a diferencia del de `BLineChart` | Un repintado concurrente o reentrante corrompe el estado de render |
| 5 | **Cast sin guarda en `BStackedBarChart`** | `BContinuousAxis yaxis = (BContinuousAxis)model.getSpec(0).getYAxis();` — `$WB/BStackedBarChart.java:109` | Un eje Y discreto lanza `ClassCastException` **en tiempo de pintado**, no de configuración |
| 6 | **Excepción tragada en `ChartController`** | `catch (Exception var3) { }` vacío tras `BExportDialog.hasExporters(...)` — `$WB/ChartController.java:229-230` | Cualquier fallo al enumerar exportadores deja el comando deshabilitado como si no existieran; el usuario ve "no se puede exportar" sin causa |
| 7 | **Auto-escala inventada 0–10** | `$WB/JoinTable.java:139` | §251.4 |
| 8 | **Sin gate de licencia ni de capacidad** | Barrido negativo sobre el paquete buscando `license`, `limit`, `cap`, `MAX_`, `@Deprecated`: 0 coincidencias (único `MAX_VALUE` es `Double.MAX_VALUE` como centinela geométrico) | **Proven-absence**: el chart clásico NO está licenciado ni capado por feature — contraste fuerte con la capa OEM Honeywell, donde casi todo módulo tiene su `checkFeature` ([Bloques 242], [244], [246]) |

## 251.9 — Nota de método: 2 afirmaciones del barrido corregidas por el token-check `[CERT]`

Se registran porque la disciplina de verificación solo vale si sus capturas quedan escritas:

1. **Ruta equivocada de `BoundChartModel`.** El barrido lo citó en `javax/baja/chart/`. Verificado: el archivo
   **no existe** ahí; vive en `javax/baja/chart/binding/BoundChartModel.java`. La clase es real, la ruta no.
   Corregido en §251.3. `[CERT]` — comprobado con `fd` sobre el árbol vineflower completo.
2. **Falso "sin guarda de padre nulo".** El barrido afirmó que `modelModified()` llama a `getPane()` *sin* la
   guarda de nulo que sí usa `paint()`. Verificado en `$WB/BChart.java:58-60`: `modelModified` hace
   `BChartPane pane = this.getPane(); if (pane != null) {...}` — **sí tiene la guarda**. Lo que es cierto es
   algo distinto y más fino: `getPane()` nunca devuelve null por padre-de-tipo-equivocado (eso lanza
   `IllegalStateException`); solo devuelve null cuando el padre ES null, porque el cast de null es válido
   (`$WB/BChart.java:39-45`). La guarda cubre el caso sin padre, no el caso mal-padre. `[CERT]`

Tokens load-bearing re-verificados por el driver leyendo la fuente: **13**.

## 251.10 — Conexiones

- **[Bloque 199]** (`webChart`, focus px-editor-deep) — **el otro sistema de charting**. Aquel es bajaux/D3,
  servido al browser, con tipos de serie elegidos por factory JS; éste es Swing/Workbench con un tipo por
  subclase Java. Coexisten en N4.14 sin capa común: no comparten modelo, ni ejes, ni bindings. El gap **H4**
  de este focus resuelve cuándo se usa cada uno.
- **[Bloque 201]** (`make/` wizard) — `BMwChart`/`BMwTimePlot` construyen widgets sobre el chart **clásico**
  documentado aquí; aquel bloque ya lo había señalado con un §14 contra B199. Este bloque abre la caja que
  B201 solo nombró.
- **[Bloque 210]** (PxEvent bus) — contraste de diseño de eventos: 9 categorías con reclasificación fina allá,
  un `if` de un caso acá (§251.7).
- **[Bloque 246]** (utilidades Honeywell) y **[Bloque 242]/[244]** — contraste de licenciamiento: la capa OEM
  gatea casi todo por `checkFeature`; el chart clásico **no tiene gate alguno** (§251.8 #8).
- **[Bloque 207]** (`easyBinding`) — contraste de legibilidad de fuente: aquel módulo está parcialmente
  ofuscado; `chart-wb` no tiene ZKM, el decompilado es íntegro.
- **Gaps que este bloque deja servidos**: H2 (ejes y render — `updateAxes()`, `toDisplaySpace()`, los
  contenedores `BChartPane`/`BChartCanvas` aparecen aquí sin abrirse), H3 (`BoundChartModel` +
  `javax.baja.chart.binding`), H5 (`com.tridium.chart.BoundChartSpec`), H8 (`TrendFlags` como una de las 3
  clases que viven en el runtime).
