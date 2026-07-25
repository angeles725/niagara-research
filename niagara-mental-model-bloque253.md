# Bloque 253 — Chart clásico (III): el binding a datos reales, y §14 corrigiendo B252

> **Qué documenta**: cómo un chart clásico se ata a DATOS REALES — el paquete `javax.baja.chart.binding`
> (9 clases): la jerarquía `BChartBinding`, las dos estrategias concretas (tabla/history vs valor vivo), la
> sincronización `doSyncBindings()`, y las specs de eje/columna. Cierra el gap **H3** del focus
> `px-chart-classic`.
>
> **Contiene una corrección §14 a [Bloque 252] §252.5** — una generalización mía que la evidencia de este gap
> refuta. Ver §253.7.
>
> **Alcance**: el paquete `binding` (7 clases en `-wb` + 2 en `-rt`). `BoundTimeSeries` y `BoundChartSpec`
> viven en el paquete PRIVADO `com.tridium.chart` (gap H5) y se tocan aquí solo en lo que el binding exige.
>
> **Fuentes** (decompilado vineflower):
> - `$B` = `…/chart/chart-wb/vineflower/javax/baja/chart/binding/`
> - `$BR` = `…/chart/chart-rt/vineflower/javax/baja/chart/binding/`
> - `$P` = `…/chart/chart-wb/vineflower/com/tridium/chart/` (paquete privado, solo referencias)
> - Raíz común: `/home/cristian/modules/Prototipos/modulos/organized/`
> - **`module-navigator`** sobre los 926 jars / 50.798 archivos para la búsqueda de implementadores.
>
> **Método**: barrido delegado (tier `sonnet`) + verificación inline del driver: **12 tokens load-bearing**
> re-verificados, y **2 afirmaciones del barrido corregidas** (§253.8), una de ellas por ampliar la búsqueda
> del módulo al universo completo con module-navigator. Marcadores: `[CERT]` = fuente primaria (`file:line`);
> `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 253.1 — `BChartBinding`: es un `BBinding` de verdad `[CERT]`

```java
public abstract class BChartBinding extends BBinding {
   public static final Property seriesName = newProperty(0, "", null);
   public static final Property xAxis = newProperty(0, new BAxisSpec(), null);
   public static final Property yAxis = newProperty(0, new BAxisSpec(), null);
   public static final Property brush = newProperty(0, BBrush.NULL, null);
   public static final Property pen = newProperty(0, BPen.make(1.0), null);
```
`$B/BChartBinding.java:39-44` `[CERT]`

Esto cambia el encuadre respecto de los dos bloques anteriores. Mientras `BChart` y `BAxis` **no tienen slots
propios** ([Bloque 251] §251.1, [Bloque 252] §252.1), el binding **sí los tiene, y son 5**. Y extiende
`javax.baja.ui.BBinding` — el mismo tronco que `BValueBinding`, el binding de kitPx documentado en
[Bloque 193] y [Bloque 186].

`[INFER]` Consecuencia práctica: `BChartBinding` es **hermano** de `BValueBinding`, no descendiente. Hereda el
ciclo de vida completo de binding (`isBound()`, `get()`, `targetChanged()`, `bound()`/`unbound()`) y le suma
lo específico del chart: nombre de serie, spec de cada eje, brush y pen. Al ser un `BBinding` con slots, **es
declarable desde un archivo `.px`** como hijo del widget chart — a diferencia del renderer de ejes, que solo
se cambia por código (§253.7).

Único método abstracto: `Series createSeries()` (`$B/BChartBinding.java:97`).

## 253.2 — Las dos estrategias: snapshot vs poll `[CERT]`

Ambas concretas declaran `@AgentOn(types = {"chart:Chart"})` `[CERT]`
(`$B/BTableChartBinding.java:14-16`, `$B/BValueChartBinding.java:19-21`) — es decir, **se ofrecen como agentes
sobre el tipo chart**, que es el mecanismo por el que aparecen como opción en Workbench/PX.

| | `BTableChartBinding` | `BValueChartBinding` |
|---|---|---|
| Agente sobre | `chart:Chart` + `baja:ICollection` | `chart:Chart` + `baja:Value` |
| Target que resuelve | un `BITable` (tabla / resultado de history) | un `BValue` (punto vivo) |
| Slots extra | `xColumn`, `yColumn` (`BColumnIdentifier`) | `timeWindow` (`BRelTime`, default **5 minutos**) `[CERT]` `$B/BValueChartBinding.java:35` |
| Serie que produce | `TableSeries` | `BoundTimeSeries` |
| **Naturaleza temporal** | **SNAPSHOT** | **POLL periódico** |

### El binding de tabla es un snapshot, no una suscripción `[CERT]`

```java
      return new TableSeries(seriesName, (BITable<?>)this.get(), this.getXColumn(), this.getYColumn(), this.getBrush(), this.getPen());
```
`$B/BTableChartBinding.java:79`

`createSeries()` resuelve el target **una vez** y construye el `TableSeries`, que a su vez hace
`Tables.slurp()` de toda la tabla ([Bloque 251] §251.3). `[INFER]` No hay suscripción, ni COV, ni scheduler:
los datos quedan congelados en el instante de `doSyncBindings()`. Un chart de history no se "actualiza solo" —
se actualiza cuando algo fuerza una re-sincronización de bindings.

### El binding de valor es un poll de 500 ms hardcodeado `[CERT]`

```java
      this.ticket = Clock.schedulePeriodically(this, BRelTime.make(500L), sample, null);
```
`$B/BValueChartBinding.java:70`

`[CERT]` El intervalo **no es configurable**: es una constante en el código. El slot `timeWindow` controla el
ancho de la ventana deslizante (5 min por defecto), **no** la frecuencia de muestreo. Cada tick dispara la
acción `sample` → `doSample()` llama a `this.get()` y alimenta `BoundTimeSeries.sample()`. También muestrea
ante `valueChanged()`.

**Pero el poll no genera una muestra cada 500 ms**: `BoundTimeSeries` filtra por cambio —
`if (sample.isChange(value))` (`$P/BoundTimeSeries.java:139`, con el predicado en `:333`) `[CERT]`. Si el valor
no cambió, solo se corre el timestamp de la última muestra. `[INFER]` Es un **filtro COV montado sobre un
poll**: el costo de CPU es fijo (2 Hz por serie), el costo de memoria depende de la volatilidad del punto.

Al desbindear, `unbound()` cancela el ticket y anula la serie (`$B/BValueChartBinding.java:80-82`) — **la
historia en memoria se descarta**. Reconectar arranca de cero.

## 253.3 — El camino del history: todo pasa por `BITable`, nada por la API de history `[CERT]`

**Ausencia probada**: cero referencias a `BHistoryConfig` o `BIHistory` en las 67 clases del módulo `chart`
(`rg -l 'BHistoryConfig|BIHistory'` sobre `chart-rt/vineflower` + `chart-wb/vineflower` → **0 archivos**)
`[CERT]`.

El chart clásico **no sabe qué es un history**. La cadena real es:

```
ORD  →  BITable (lo que sea que la ORD resuelva)
     →  BTableChartBinding.get() casteado a BITable<?>
     →  TableSeries → Tables.slurp() → BIRandomAccessTable
     →  columnas por NOMBRE vía BColumnIdentifier
```

`[INFER]` El acoplamiento es con la abstracción de tabla, no con el subsistema de histories. Cualquier cosa
que implemente `BITable` sirve: un resultado de history, una consulta BQL, un componente propio. Es la razón
por la que este módulo pudo sobrevivir intacto a los cambios del subsistema de histories.

Lo que **sí** delata la intención de diseño son las convenciones de esquema que `TableSeries` reconoce por
nombre: una columna `trendFlags` y una columna `status` ([Bloque 251] §251.6), y los facets `startTime` /
`endTime` de la tabla para los mínimos/máximos del eje temporal — que es como una consulta de history
comunica su ventana.

## 253.4 — `doSyncBindings()`: el algoritmo de reconciliación `[CERT]`

`BoundChartModel.doSyncBindings()` (`$B/BoundChartModel.java:41-103`, `synchronized`) es una **reconciliación
en dos barridos**, no un rebuild:

1. Toma **todos** los `BBinding` hijos del chart (`chart.getBindings()`, `:44`).
2. Filtra a `instanceof BChartBinding && isBound()` (`:47-50`).
3. **Barrido de obsoletos** (`:57-73`): recorre los specs hacia atrás; si el binding de un spec ya no está en
   el conjunto ligado, lo elimina.
4. **Barrido de nuevos** (`:77-92`): recorre los bindings ligados hacia atrás; si ya existe spec para uno, lo
   saca de la lista de pendientes.
5. `addSpec()` para cada binding nuevo (`:96-99`), y `fireModelModified()` al final (`:101`).

**Cuándo se re-ejecuta**: `BChartBinding.bound()` y `unbound()` llaman a `model.syncBindings()`
(`$B/BChartBinding.java:131,141`). `[INFER]` Es decir, se dispara por eventos de ciclo de vida del binding —
**no** por llegada de datos. Esto confirma §253.2: un binding de tabla solo se refresca si se re-liga.

### La regla de reutilización de ejes `[CERT]`

`addSpec()` (`$B/BoundChartModel.java:105-159`) no crea un eje por serie: para cada spec pide
`getPane().findAxis(axis)`, que delega en `ChartModel.findAxis()` (`$WB/ChartModel.java:114-133`), el cual
recorre los specs existentes y, a igual dimensión, consulta `check.isCompatible(axis)` (`:127`).

Caso especial verificado (`:145-148`): si el eje Y encontrado es un `BDiscreteAxis`, **se fusiona** el nuevo
dentro del existente (`merge`) en vez de crear otro. `[INFER]` Es lo que hace que N series categóricas
compartan una única tira de categorías en lugar de apilar N ejes discretos.

*(La lógica de `BAxis.isCompatible()` no está en estas 9 clases — se llama desde `ChartModel`. Queda fuera del
alcance de este gap.)*

## 253.5 — Las specs: `BAxisSpec` resuelve el tipo de eje POR REGISTRO DE AGENTES `[CERT]`

```java
      AgentList agents = Sys.getRegistry().getAgents(valueTypeSpec.getTypeInfo());
      AgentList axes = agents.filter(AgentFilter.is(BAxis.TYPE));
```
`$B/BAxisSpec.java:118-119` `[CERT]`

Este es el hallazgo que dispara la corrección §14 (§253.7). `BAxisSpec` es un `BStruct`
(`$B/BAxisSpec.java:35`) con slots `valueType` (`BTypeSpec`), `min` y `max` (`BAxisBound`). Su `toAxis()`
**pregunta al registro de agentes de Niagara qué eje corresponde al tipo del dato**, filtrando por
`BAxis.TYPE`. Si no hay ningún eje registrado para ese tipo, cae a `new BDiscreteAxis()`.

`[INFER]` O sea: el tipo de eje NO está cableado. Un módulo puede registrar su propio `BAxis` como agente
sobre un tipo de valor propio, y el chart lo elegirá solo. Es extensibilidad declarativa real.

Las otras tres:

| Clase | Dónde | Qué es | Rol |
|---|---|---|---|
| `BDiscreteAxisSpec` | `-wb` | extiende `BAxisSpec` | Fuerza `BDiscreteAxis` **ignorando el registro** (`$B/BDiscreteAxisSpec.java:30`). Se usa cuando el tipo tiene eje numérico registrado pero querés render discreto |
| `BAxisBound` | **`-rt`** | `BSimple` final | Dos modos: `auto` (singleton) o `fixed` con valor tipado. Codificación de cable: `"auto"` o `"fixed,<typespec>,<valor>"` |
| `BColumnIdentifier` | **`-rt`** | `BSimple` final | Identifica una columna. **3 variantes**: `NULL_TYPE=1`, `ROW_INDEX_TYPE=2` (número de fila sintético como `BDouble`), `TABLE_COLUMN_TYPE=3` (por NOMBRE de columna). Cable: `"null"` / `"rowIndex"` / `"tableColumn:<nombre>"` |

`[INFER]` Que `BAxisBound` y `BColumnIdentifier` vivan en `-rt` (junto a `TrendFlags`, `BAxisDimension` y
`BAxisLocation`) es consistente con H8: al runtime le toca lo **serializable** — lo que viaja en un `.px` o un
`.bog` —, mientras el motor de dibujo se queda en `-wb`.

Para graficar un history: `xColumn = tableColumn:timestamp`, `yColumn = tableColumn:value`. El nombre debe
coincidir **exactamente** con el del esquema de la tabla.

## 253.6 — `BChartBindingCollection`: el punto de extensión multi-serie `[CERT]`

```java
   @Override
   public final Series createSeries() {
      return null;
   }

   public abstract Series[] createSeriesSet();

   public abstract BAxisSpec getYAxis(Series var1);
```
`$B/BChartBindingCollection.java:17-24` `[CERT]`

Subclase abstracta de `BChartBinding` que **anula** `createSeries()` a null (y lo marca `final`) y exige en su
lugar `createSeriesSet()` → `Series[]`, más un `getYAxis(Series)` por serie. `BoundChartModel.addSpec()`
ramifica por `instanceof` antes de llamar (`$B/BoundChartModel.java:110`), y le da a cada serie del array su
propio `BoundChartSpec` con su propio eje Y (`:134`).

**Quién lo implementa**: `BTransformChartBindingCollection`, en **`seriesTransform-wb`** `[CERT]` —
único implementador en los 50.798 archivos del universo indexado (`module_nav grep 'extends
BChartBindingCollection'` → 1 coincidencia). `[INFER]` El mecanismo multi-serie existe para que un módulo de
transformación de series produzca N series derivadas desde un solo binding; dentro del módulo `chart` no hay
ningún uso, es puramente un hook para terceros.

## 253.7 — §14 — CORRECCIÓN a [Bloque 252] §252.5 `[CERT]`

**Lo que afirmé en B252 §252.5**: que la extensión del chart clásico es *"100% programática — ni un slot, ni
un agente `@AgentOn`, ni una factory"*, y de ahí concluí que `chart` *"es un módulo de una generación anterior
al mecanismo de agentes del framework"*.

**Lo primero es correcto y se mantiene; la generalización es FALSA.** La evidencia de este gap la refuta:

| Capa | Mecanismo de extensión | Evidencia |
|---|---|---|
| Render de ejes / swatches | **Setters Java** (uno estático). Sin slot, sin agente | `$WB/BAxis.java:274,282` — **se mantiene, verificado** |
| Bindings del chart | **`@AgentOn(types={"chart:Chart"})`** | `$B/BTableChartBinding.java:14-16`, `$B/BValueChartBinding.java:19-21` |
| Elección del tipo de EJE según el tipo de dato | **Registro de agentes** `Sys.getRegistry().getAgents(...)` filtrado por `BAxis.TYPE` | `$B/BAxisSpec.java:118-119` |
| Multi-serie por binding | Subclase abstracta como hook (`BChartBindingCollection`) | §253.6 |

**Tesis corregida** `[INFER]`: el chart clásico **sí participa del mecanismo de agentes**, y en un punto nada
trivial — qué eje se instancia para qué tipo de valor se decide por registro, no por `if`. Lo que **no** pasa
por agentes es exclusivamente la capa de **render** (`AxisRenderer`, `SwatchRenderer`), que quedó cableada a
setters. La conclusión honesta no es "módulo pre-agentes", sino **"módulo con extensibilidad declarativa en la
capa de datos y cableada en la capa de dibujo"**.

El error de origen fue generalizar desde una capa (render) a un módulo entero sin haber abierto la capa de
binding. Queda como lección de método: no concluir sobre la arquitectura de un módulo con un solo paquete
leído.

## 253.8 — Nota de método: 2 afirmaciones del barrido corregidas `[CERT]`

1. **"No existe subclase concreta de `BChartBindingCollection` — mecanismo muerto".** FALSO. El barrido buscó
   solo dentro del árbol de `chart-wb`. Ampliada la búsqueda al universo con `module-navigator`, aparece
   `BTransformChartBindingCollection` en `seriesTransform-wb:67` `[CERT]`. Corregido en §253.6. **Lección**:
   una ausencia probada vale por el ámbito en que se probó — "no está en este módulo" ≠ "no existe".
2. **Ubicación de `BoundTimeSeries`.** El barrido lo citó sin calificar el paquete; verificado, vive en
   `com/tridium/chart/BoundTimeSeries.java` — paquete **privado** de Tridium (gap H5), no en la API pública.

Tokens load-bearing re-verificados por el driver: **12**.

## 253.9 — Gotchas de esta capa `[CERT]`

- **Poll de 500 ms no configurable** (`$B/BValueChartBinding.java:70`) — §253.2.
- **`pageSize = 256` hardcodeado** en `BoundTimeSeries` (`$P/BoundTimeSeries.java:31,35`) `[CERT]`, no
  configurable.
- **Cast sin guarda** a `BITable` (`$B/BTableChartBinding.java:79`): si la ORD resuelve a otra cosa, salta
  `ClassCastException`. Y `addSpec()`/`doSyncBindings()` **no tienen try-catch** (`$B/BoundChartModel.java:105-159`),
  así que la excepción aborta la sincronización entera dejando el chart en estado parcial.
- **`doSample()` no chequea null** antes de usar `this.get()` (`$B/BValueChartBinding.java:90`).
- **Cero chequeos de permisos** en las 9 clases del paquete: el control de acceso queda enteramente en lo que
  imponga `BBinding.get()` a nivel framework. `[INFER]` Contrasta con el write-gate explícito `checkCanWrite`
  de chihuahua ([Bloque 164]) — aunque aquí es camino de LECTURA, no de escritura.
- **`doSyncBindings()` es O(n²)** en cantidad de bindings (dos bucles anidados). Irrelevante con <10 series;
  anotado por corrección.

## 253.10 — Conexiones

- **[Bloque 252]** — **corregido por §253.7** (la tesis "módulo pre-agentes" era una generalización indebida).
- **[Bloque 251]** — cierra su hilo pendiente: `BoundChartModel`, nombrado allí, queda documentado; y se
  confirma que `TableSeries` + `Tables.slurp()` es el único camino de datos tabulares.
- **[Bloque 193]** y **[Bloque 186]** (los 9 bindings kitPx, `BValueBinding`) — `BChartBinding` es **hermano**
  de `BValueBinding` bajo `javax.baja.ui.BBinding`, no descendiente (§253.1).
- **[Bloque 199]** (`webChart`) — el contraste de frescura de datos: allá el motor JS consulta servlets de
  datos con sampling automático; acá el binding de tabla es un snapshot que solo se refresca al re-ligarse.
- **[Bloque 164]** (RBAC de chihuahua) — contraste de control de acceso (§253.9).
- **Gaps abiertos**: H4 (consumidores + §14 contra B199/B201 — ahora con `seriesTransform` confirmado como
  implementador), H5 (`com.tridium.chart`: `BoundChartSpec`, `BoundTimeSeries`, `BAxisContainer`), H6, H7, H8.
