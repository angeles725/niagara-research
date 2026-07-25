# Bloque 252 — Chart clásico (II): ejes, tick spacing y el pipeline de render Swing

> **Qué documenta**: la capa de EJES y RENDER del charting clásico de Niagara N4 — el contrato `BAxis`, la
> jerarquía de ejes concretos, los **dos algoritmos distintos de tick spacing** (numérico vs temporal), los
> renderers, y los contenedores `BChartPane` / `BChartCanvas` / las leyendas. Cierra el gap **H2** del focus
> `px-chart-classic`.
>
> **Alcance**: sigue en el paquete público `javax.baja.chart`. NO cubre el binding a datos (gap H3,
> `javax.baja.chart.binding`) ni la impl privada `com.tridium.chart` (gap H5) — `BAxisContainer` vive ahí y
> queda explícitamente fuera. El modelo de datos y la jerarquía de charts están en el [Bloque 251].
>
> **Fuentes** (decompilado vineflower, pipeline canónico):
> - `$WB` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-wb/vineflower/javax/baja/chart/`
> - `$RT` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-rt/vineflower/javax/baja/chart/`
>
> **Método**: barrido delegado (tier `sonnet`) sobre la capa de ejes/render; el driver re-verificó **14 tokens
> load-bearing** leyendo la fuente citada, **incluidos los dos bugs de §252.7 a y b**, que se confirman
> literalmente en el decompilado. Marcadores: `[CERT]` = leído en la fuente primaria (`file:line`);
> `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 252.1 — `BAxis`: un agente, no un componente `[CERT]`

```java
public abstract class BAxis extends BObject implements BIAgent {
```
`$WB/BAxis.java:14` `[CERT]`

Dos datos que definen su naturaleza:

- **Extiende `BObject`, no `BComponent`** — un eje NO es un componente montado en el árbol de la station.
- **Implementa `BIAgent`** — participa del sistema de agentes de Niagara.
- **Cero slots propios**: no declara `@NiagaraProperty` ni `@NiagaraAction` `[CERT]` (barrido negativo sobre
  las 390 líneas del archivo). Toda su configuración es estado Java, puesto programáticamente.

El contrato abstracto son **8 métodos** (`$WB/BAxis.java:188-268`): `getValueType()`, `getAxisMin()`/
`setAxisMin(Object)`/`updateAutoMin(Object)`, `getAxisMax()`/`setAxisMax(Object)`/`updateAutoMax(Object)`,
`getTickValues()`, `valueToString(Object)`, `toDisplaySpace(Object)` y `fromDisplaySpace(double)`.

### `toDisplaySpace()` — la proyección dato→píxel `[CERT]`

```java
   public double toDisplaySpace(Object value) {
      double len = this.getLength();
      double percent = this.toPercentOfRange(value);
      double loc = len * percent + this.getMinMargin();
      return this.getDimension() == BAxisDimension.x ? loc : this.getFullLength() - loc;
   }
```
`$WB/BContinuousAxis.java:23-28` `[CERT]`

Mapea un valor del dominio al rango de píxeles `[minMargin, fullLength - maxMargin]`. **La última línea es la
clave**: en el eje Y invierte el origen (`fullLength - loc`), porque en pantalla el píxel 0 es arriba y el
mínimo de datos va abajo. La inversión no es un detalle del renderer — está en la proyección misma.

El inverso `fromDisplaySpace(double)` es **abstracto explícito**, no derivado: cada eje concreto lo
implementa. Es lo que hace posible el zoom por arrastre y el cursor de traza.

Consumidores verificados de `toDisplaySpace()`: `DefaultAxisRenderer.paintLeft/Right/Top/Bottom` (posición de
cada etiqueta de tick), `BChartCanvas.paint()` para las líneas de grilla (`$WB/BChartCanvas.java:288,300`), y
`BChartCanvas.paintPointDetail()` para el cursor de traza (`:367-368`).

## 252.2 — La jerarquía de ejes: `BDiscreteAxis` es el hermano raro `[CERT]`

| Clase | Padre | Dominio | `toDisplaySpace` |
|---|---|---|---|
| `BAxis` | `BObject` | — (abstracto) | abstracto |
| `BContinuousAxis` | `BAxis` | continuo (aporta el contrato porcentaje-de-rango) | implementa vía `toPercentOfRange` |
| `BNumericAxis` | `BContinuousAxis` | `BINumeric` | hereda |
| `BAbsTimeAxis` | `BContinuousAxis` | `BAbsTime` (epoch ms) | hereda |
| `BDiscreteAxis` | **`BAxis` directo** | categórico | **implementación propia**: lookup de índice en `locs[]` |

`$WB/BAxis.java:14`, `BContinuousAxis.java:8`, `BNumericAxis.java:22`, `BAbsTimeAxis.java:22`,
`BDiscreteAxis.java:16` `[CERT]`

**`BDiscreteAxis` NO extiende `BContinuousAxis`** — cuelga de `BAxis` directo. `[INFER]` Tiene sentido: un eje
categórico no tiene "porcentaje de rango", tiene segmentos. Su `doLayout()` reparte anchos iguales con
`gapWidth = len / range.length / 5.0` (un 20% del segmento como hueco) y guarda los centros en píxeles en
`locs[]` (`$WB/BDiscreteAxis.java:71-90`). Coherentemente, **tiene el zoom deshabilitado** (`:146-148`) — no
se puede hacer zoom sobre categorías.

## 252.3 — Tick spacing: DOS algoritmos completamente distintos `[CERT]`

Este es el hallazgo estructural del bloque. El eje numérico y el temporal **no comparten estrategia**.

### Numérico — redondeo por orden de magnitud `[CERT]`

```java
      double log10 = Math.log(delta) * 0.4342944819018;
      this.tickIncrement = Math.max((float)Math.pow(10.0, Math.round(log10) - 2L), Float.MIN_VALUE);
      if (delta > 10.0 && this.tickIncrement < 5.0) {
         this.tickIncrement = 5.0;
```
`$WB/BNumericAxis.java:183-186` `[CERT]`

Secuencia completa (`doLayout`, `:181-239`): log10 del rango (con la constante `LOG10E` hardcodeada
`0.4342944819018`) → incremento inicial `10^(round(log10) - 2)` → **piso duro en 5.0 si el rango supera 10** →
normalización de precisión float dando una vuelta por `DecimalFormat` → piso por el facet de precisión →
**tope de 20 ticks** (si se pasa, duplica el incremento en bucle: ×10 cuando vale 1.0, ×2 en el resto) →
alineación del primer tick con `Math.round()`.

`[INFER]` Es redondeo por orden de magnitud, **no** una tabla de "nice numbers". Por eso puede producir
incrementos como 5.0 impuestos por el piso duro, en vez del 1/2/5/10 canónico que uno esperaría.

### Temporal — tabla fija de 10 tramos `[CERT]`

```java
   private long computeTickIncrement(long delta) {
      long tickIncrMillis = 1L;
      if (delta <= 1000L) {
         tickIncrMillis = 1L;
      } else if (delta < 60000L) {
         tickIncrMillis = 1000L;
      } else if (delta < 300000L) {
         tickIncrMillis = 15000L;
```
`$WB/BAbsTimeAxis.java:169-176` `[CERT]`

Cascada de 10 tramos sobre el span (`:170-193`): ≤1s→1ms · <1min→1s · <5min→15s · <30min→1min · <1h→5min ·
<6h→15min · ≤30h→1h · ≤36d→1día · <3años→30días · resto→1año. Si aun así salen más de **30** ticks, duplica el
incremento hasta bajar de 30 (`:144`).

El **formato** de la etiqueta lo elige `deltaToTimeFormat(delta)` (`:196-204`) en 4 tramos: `<10s` →
`D-MMM-YY h:mm:ss a z` + milisegundos; `<1h` → con segundos; `>30d` → `D-MMM-YY z`; el resto →
`D-MMM-YY h:mm a z`. **Un facet `timeFormat` presente en `timeFacets` pisa todo lo anterior** (`:148`) — ese es
el punto de extensión real para el formato de fecha.

## 252.4 — `BAxisDimension` y `BAxisLocation` son `BFrozenEnum` `[CERT]`

```java
public final class BAxisDimension extends BFrozenEnum {
```
`$RT/BAxisDimension.java:14` `[CERT]` (ambos viven en el **runtime**, junto con `TrendFlags` — ver H8)

- **`BAxisDimension`** `{x(0), y(1)}` — la dirección física. Es lo que consulta `toDisplaySpace()` para decidir
  si invierte la coordenada (§252.1).
- **`BAxisLocation`** `{top(0), bottom(1), left(2), right(3)}` — qué borde del canvas ocupa la tira del eje.
  Lo consume `DefaultAxisRenderer.paint()` para despachar a `paintTop/Bottom/Left/Right`.

Ninguno de los dos es un slot declarado: son estado en runtime que fija `BChartPane.build()`.

## 252.5 — Render: la extensión es 100% programática (el hallazgo de arquitectura) `[CERT]`

Dos contratos con formas distintas a propósito:

```java
public abstract class AxisRenderer {
   public abstract void paint(Graphics var1, BAxis var2);

   public abstract double getPreferredAxisWidth(BAxis var1);
}
```
`$WB/AxisRenderer.java:5-9` `[CERT]` — **clase abstracta**

```java
public interface SwatchRenderer {
   double getWidth();

   double getHeight();

   void paintSwatch(Series var1, Graphics var2, double var3, double var5);
}
```
`$WB/SwatchRenderer.java:5-11` `[CERT]` — **interfaz**

Implementaciones por defecto: `DefaultAxisRenderer extends AxisRenderer` (despacha por `BAxisLocation`, pinta
ticks, etiquetas, título y swatches de serie en línea) y `DefaultSwatchRenderer implements SwatchRenderer`
(un `fillRect` de 12×12 con el color de `series.getBrush()`, sin guarda contra brush nulo).

**Los puntos de extensión, verificados:**

| Qué | Cómo se cambia | Evidencia |
|---|---|---|
| Renderer de un eje | `BAxis.setRenderer(AxisRenderer)` — setter Java de instancia | `$WB/BAxis.java:282` |
| Renderer por defecto global | `BAxis.setDefaultRenderer(AxisRenderer)` — setter **estático**; nulo lanza NPE | `$WB/BAxis.java:274,276` |
| Swatch de la leyenda | `BDefaultChartLegend.setSwatchRenderer(SwatchRenderer)` — setter Java | `$WB/BDefaultChartLegend.java:157,159` |

**Ni un slot, ni un agente `@AgentOn`, ni una factory.** `[INFER]` Esto es una anomalía dentro del subsistema
PX tal como lo documentó el corpus: el editor PX extiende **todo** por agentes y registro declarativo — field
editors por `@AgentOn` ([Bloque 214]), perfiles OEM por `BIAgent`@`WbProfile` ([Bloque 211]), inserción de
widgets por factories registradas ([Bloque 212]). El chart clásico, en cambio, se extiende llamando setters
Java desde código. **Un OEM no puede cambiar el render de ejes de forma declarativa**: necesita código que
corra y llame al setter estático. Es un módulo de una generación anterior al mecanismo de agentes.

Detalle operativo de `DefaultAxisRenderer`: el título del eje y los swatches en línea solo se pintan si
`GxEnv.get().isRotationSupported()` es verdadero (`$WB/DefaultAxisRenderer.java:185,246`). En un entorno GX sin
rotación **desaparecen sin error** (ver §252.7 h).

## 252.6 — Los contenedores `[CERT]`

### `BChartPane` (885 líneas) — el que manda

Slots declarados (`$WB/BChartPane.java:42-113`): `border`, `background`, `header`, `canvas`, `legend`,
`zoomEnabled`, más los cuatro contenedores de ejes `leftAxes`/`rightAxes`/`topAxes`/`bottomAxes`
(`BAxisContainer`, con flags=6 = oculto + readonly). Acciones `showPanControl`/`hidePanControl` y topics
`showPanRequested`/`hidePanRequested`.

**Tres niveles de refresco, no dos** — matiz que refina lo dicho en [Bloque 251] §251.7:

| Método | Costo | Qué hace | Evidencia |
|---|---|---|---|
| `build()` | alto, `synchronized` | Teardown completo: limpia los 4 contenedores, llama `updateAxes()` en cada chart, **re-reparte los ejes**, y llama `canvas.build()`. Se dispara en `started()` y en `added()` | `:597-642` |
| `refresh()` | medio, `synchronized` | Incremental: resetea datos de eje **sin** limpiar contenedores, `updateAxes()` por chart, y relayout | `:562-577` |
| `rebuild()` | diferido | Solo marca `buildRequired=true` y `rebuildCountDown=1`; `animate()` decrementa y dispara el `build()` real dos frames después — **debounce** de ráfagas de datos | `:580-595` |

**La política de reparto de ejes está hardcodeada** `[CERT]` (`:597-642`): el primer eje X va a `bottom`, el
segundo a `top`, y todo excedente vuelve a `bottom`; el primer Y va a `left` y **todos los demás a `right`**.

Layout (`doLayout`, `:664-725`): el canvas central es el tamaño total menos las tiras de eje de los 4 lados y
la leyenda abajo, con un mínimo de 5 px para las tiras izquierda y derecha (`:686-687`). **Todos los `BChart`
hijos reciben exactamente los mismos bounds que el canvas** (`:722-724`) — se superponen; así es como se
dibujan varias series/tipos sobre el mismo área.

Zoom: `ArrayList<ZoomSpec> zoomStack` de **profundidad ilimitada** (`:118`), cada entrada guarda un snapshot
de min/max de los ejes. El pan mueve 1/10 del ancho del canvas por paso (`:439`).

`export()` **solo exporta el primer chart**: delega en `charts[0].getModel().export()` y devuelve null si no
hay charts (`:795-798`). `[INFER]` En un pane con varias series de charts distintos, la exportación pierde
todo menos el primero.

### `BChartCanvas` (530 líneas) — el búfer

Slots: `fill`, `borderStroke`, `borderPen`, `showHorizontalGridLines`, `showVerticalGridLines`, `gridStroke`,
`gridPen` (`$WB/BChartCanvas.java:34-77`).

```java
      if (GxEnv.get() instanceof AwtEnv) {
         if (this.chartBuffer == null) {
            this.chartBuffer = BImage.make(Math.max(w, 1.0), Math.max(h, 1.0));
```
`$WB/BChartCanvas.java:226-228` `[CERT]`

**El doble búfer existe solo bajo AWT.** Fuera de AWT, `noBuffer` queda en true (`:264`) y cada repintado
redibuja todo directo en `g`, sin caché. El overlay de selección se dibuja siempre sobre el `g` real después
del blit, nunca se cachea (`:323-326`).

Enumeración de hijos: `paint()` recorre un `SlotCursor<Property>` del pane filtrando por `BChart.class`
(`:306-309`); `doLayout()` hace un recorrido aparte filtrando **solo** `BDiscreteLineChart.class` para
asignarles los índices de apilado (`:244-260`) — el mecanismo que [Bloque 251] §252.2 describió desde el lado
del chart.

### Las leyendas y el patrón Null Object `[CERT]`

`BChartLegend` es un `BWidget` abstracto con slots `background`/`fill`/`font` y el contrato
`getPreferredHeight(double)`/`getPreferredWidth(double)`; sube por la cadena de padres hasta encontrar el
`BChartPane` (`$WB/BChartLegend.java:16-82`).

- `BDefaultChartLegend` — envuelve los ítems en filas; **se oculta sola si hay ≤1 serie** (devuelve altura 0,
  `:60`).
- `BNullChartLegend` — Null Object puro: `isNull()` → true y `computePreferredSize()` fija (0,0)
  (`$WB/BNullChartLegend.java:8-23`) `[CERT]`. `[INFER]` Existe para poder desactivar la leyenda **sin** que
  el layout tenga que chequear null en cada paso.

`BChartHeader` es un `BWidget` con 5 slots: `title`, `titleFont`, `subtitle`, `subtitleFont`, `brush`
(`$WB/BChartHeader.java:17-37`).

## 252.7 — Bugs y gotchas verificados `[CERT]`

Los dos primeros son **bugs reales en código de Tridium**, confirmados literalmente en el decompilado por el
driver (no solo reportados por el barrido).

**a) `BChartPane.assignColors()` — `return` donde iba `continue`** `[CERT]`

```java
      for (int s = 0; s < seriesList.length; s++) {
         Series series = seriesList[s];
         BBrush brush = series.getBrush();
         BColor color = null;
         if (brush != null && !brush.isNull()) {
            return;
         }
```
`$WB/BChartPane.java:757-762`

El `return` abandona **el método entero** en cuanto encuentra una serie que ya tiene brush, en vez de saltar
a la siguiente. `[INFER]` Consecuencia: si la primera serie ya viene con color asignado, **ninguna** de las
siguientes recibe color. Es el patrón clásico de bug de bucle, y explica escenarios de "algunas series salen
sin color" que un integrador no podría diagnosticar desde la UI.

**b) `BChartHeader` — el subtítulo se testea contra el título** `[CERT]`

```java
      String sub = this.getSubtitle();
      if (title.length() != 0) {
```
`$WB/BChartHeader.java:154-155`

Lee `sub` y acto seguido condiciona sobre `title`. `[INFER]` Con título vacío y subtítulo cargado, el
subtítulo **no se pinta nunca**; con título cargado y subtítulo vacío, entra a pintar una cadena vacía
(inocuo). El slot `subtitle` es inútil sin un `title` no vacío.

**c) Doble búfer solo AWT** (`:226`) — fuera de AWT no hay caché de pintado; cada frame redibuja todo (§252.6).

**d) `BAbsTimeAxis.reset()` ancla al reloj de pared** `[CERT]`:
`this.max = BAbsTime.now(); this.min = this.max.subtract(BRelTime.makeHours(1));`
(`$WB/BAbsTimeAxis.java:111-112`) — cada reset deja una ventana de **1 hora terminando ahora**, sin mirar el
rango de los datos.

**e) `BNumericAxis` expande ±10.0 cuando min == max** `[CERT]` (`:126-128`: `this.min = mid - 10.0`) —
constante hardcodeada, no configurable. Hermano del 0–10 inventado de `JoinTable` ([Bloque 251] §251.4).

**f) `BChartPane` fuerza un mínimo de 300×300** `[CERT]`:
`this.setPreferredSize(Math.max(300.0, header.getPreferredWidth()), 300.0);` (`:559`) — no es slot.

**g) `ParseException` tragada en `BNumericAxis.doLayout()`** (`:195-197`, `:218-219`): se captura, se loguea con
`log.severe`, y **el layout continúa con el `tickIncrement` previo al parseo** — ticks potencialmente mal
espaciados sin que nada falle visiblemente.

**h) Título y swatches de eje desaparecen sin rotación GX** (`DefaultAxisRenderer:185,246`) — §252.5.

**i) `BDiscreteAxis.fromDisplaySpace()` — guarda sospechosa de off-by-one** en `locs.length - 2` (`:126`).
Marcado `[INFER]` y **no confirmado como bug**: el driver verificó la línea pero no reprodujo el caso límite;
requiere ejecución (queda fuera del alcance read-only).

## 252.8 — Conexiones

- **[Bloque 251]** — la mitad complementaria: modelo de datos y jerarquía de charts. Este bloque refina su
  §251.7: el ciclo no es rebuild-vs-refresh sino **tres** niveles (`build` / `refresh` / `rebuild` diferido).
- **[Bloque 199]** (`webChart`) — el contraste sigue creciendo: allá las escalas Time/Value son D3 con
  sampling automático a 2500 puntos; acá son dos algoritmos Java distintos con topes de 20 y 30 ticks.
- **[Bloque 211]**, **[Bloque 212]**, **[Bloque 214]** — el contraste de EXTENSIBILIDAD (§252.5): el editor PX
  extiende por `@AgentOn`/agentes/factories registradas; el chart clásico solo por setters Java. Es el
  argumento más fuerte de que `chart` precede al mecanismo de agentes del framework.
- **[Bloque 183]** (valores gx) y **[Bloque 205]** (`studio/`, painters buffer-and-overlay) — el patrón de
  búfer + overlay de `BChartCanvas` (§252.6) es el mismo que usa el canvas del editor PX.
- **Gaps que siguen abiertos**: H3 (`javax.baja.chart.binding` — de dónde salen los datos), H4 (consumidores +
  §14 contra B199/B201), H5 (`com.tridium.chart`, donde vive `BAxisContainer`, nombrado aquí sin abrirse),
  H6, H7, H8.
