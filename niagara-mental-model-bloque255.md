# Bloque 255 — Chart clásico (V): la implementación privada `com.tridium.chart`

> **Qué documenta**: el paquete PRIVADO de Tridium que sostiene la API pública del charting clásico — las 11
> clases raíz de `com.tridium.chart` + las 2 de `wb/`: el contenedor de ejes, las specs y series ligadas, los
> controles de pan/zoom, los field editors del property sheet, y la glue de Workbench. Cierra el gap **H5**
> del focus `px-chart-classic`.
>
> **Resuelve un `UNVERIFIED` de [Bloque 252] §252.5** (quién llama al renderer de ejes) y **completa el cuadro
> de agentes de [Bloque 254] §254.3** con un matiz fino. Ver §255.2 y §255.6.
>
> **Alcance**: raíz de `com.tridium.chart` (11 clases) + `wb/` (2). Los subpaquetes `pdf/` (2), `hx/` (1) y
> `test/` (8) son los gaps H6 y H7 y NO se abrieron aquí.
>
> **Fuentes** (decompilado vineflower):
> - `$P` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-wb/vineflower/com/tridium/chart/`
>
> **Método**: barrido delegado (tier `sonnet`) sobre las 13 clases + verificación inline del driver: **11
> tokens load-bearing** re-verificados, incluidos los tres defectos de §255.7. Marcadores: `[CERT]` = fuente
> primaria (`file:line`); `[INFER]` = deducción. **Bloque de EVIDENCIA** — sin reclasificar.
>
> **SEÑAL DE AGOTAMIENTO (§11)**: este bloque cierra con ratio **0.56**, por encima del umbral 0.5, y es el
> **segundo consecutivo** en pasarlo (B254 dio 0.59). En un bloque de evidencia eso significa exactamente lo
> que dice el contrato: la evidencia investigable de este focus se está agotando. Y es consistente con lo que
> queda medido en el backlog — los tres gaps restantes (H6, H7, H8) suman **16 clases** entre los tres. Se
> registra acá, no se explica: alimenta la decisión §8 de STOP.

---

## 255.1 — Inventario: qué es realmente este paquete `[CERT]`

Las 13 clases se reparten en **cuatro grupos funcionales**, no en un "misceláneo":

| Grupo | Clases | Naturaleza |
|---|---|---|
| **Contenedor de ejes** | `BAxisContainer` | `BWidget` — la pieza que `BChartPane` declara en sus 4 slots |
| **Datos ligados** | `BoundChartSpec`, `BoundTimeSeries` | Java plano (extienden `ChartSpec` y `Series`) |
| **Controles de usuario** | `BAbstractPanControl`, `BDockedPanControl`, `BPanControl` | `BWidget`s de pan/zoom |
| **Field editors** | `BAxisBoundFE`, `BNumericAxisBoundFE`, `BAxisSpecFE`, `BColumnIdentifierFE` | `BWbFieldEditor` — el property sheet de Workbench |
| **Utilidad / glue** | `ChartUtil` (estática), `wb/BFlexPane`, `wb/BResourceManager` | — |

`[INFER]` El reparto dice algo del diseño: lo que Tridium mantuvo **privado** no es lógica de negocio del
chart, sino **la infraestructura de interacción y edición**. El modelo, los ejes y los bindings son públicos y
extensibles; el contenedor, los controles de pan y los editores de propiedades son suyos.

**Sin ofuscación**: las 13 clases decompilan limpio con vineflower, sin nombres de un carácter ni fallos
`[CERT]` — consistente con `ZKM: no` del jar ([Bloque 251] header).

## 255.2 — `BAxisContainer`: resuelto el `UNVERIFIED` de B252 `[CERT]`

[Bloque 252] §252.5 dejó abierto quién invoca al renderer de ejes, porque `BAxisContainer` estaba fuera de su
alcance. **Confirmado**:

```java
         try {
            axis.getRenderer().paint(g, axis);
         } finally {
            g.pop();
```
`$P/BAxisContainer.java:195-197` `[CERT]`

`BAxisContainer.paint()` (`:177-201`) recorre sus ejes y, por cada uno, hace `g.push()` →
`g.translate(axis.getX(), axis.getY())` → `axis.getRenderer().paint(g, axis)` → `g.pop()`. **El contenedor es
el que llama al renderer**, con el origen ya trasladado a la esquina del eje — por eso `DefaultAxisRenderer`
puede pintar en coordenadas locales. A partir del segundo eje dibuja además una línea negra separadora
(`:182-189`).

Lo demás de la clase:
- `extends BWidget` (`:29`), y guarda sus ejes en un **`ArrayList<BAxis>` Java plano** (`:33`), **no** en slots
  Niagara. `addAxis()`/`removeAxis()` (`:73-80`) propagan al eje la dimensión y la ubicación del contenedor —
  que es cómo `BAxisDimension`/`BAxisLocation` ([Bloque 252] §252.4) terminan seteados sin ser slots.
- `computePreferredSize()` (`:120-142`) suma en el eje de apilado y toma el máximo en el otro: para un
  contenedor X, ancho = máximo y alto = **suma** (los ejes se apilan verticalmente); para uno Y, al revés. Ese
  número es exactamente lo que consume el `doLayout()` de `BChartPane` ([Bloque 252] §252.6) para descontar las
  tiras laterales.

## 255.3 — `BoundChartSpec`: un back-pointer y nada más `[CERT]`

Añade **un solo campo** sobre el `ChartSpec` público: `private BChartBinding binding` con su getter
(`$P/BoundChartSpec.java:9,22`), y dos constructores espejo de los de `ChartSpec`.

**No tiene mecanismo de sincronización propio** `[CERT]` — es un contenedor pasivo. `[INFER]` Confirma desde
el lado de la implementación lo que [Bloque 253] §253.4 dedujo desde el binding: la sincronización la maneja
el ciclo de vida del binding (`bound()`/`unbound()` → `syncBindings()`) y el tick de 500 ms; el spec solo
recuerda de qué binding vino, para que el barrido de obsoletos de `doSyncBindings()` pueda compararlo.

## 255.4 — `BoundTimeSeries`: el búfer paginado del valor vivo `[CERT]`

```java
   public BoundTimeSeries(BValueChartBinding binding) {
      this.pageSize = 256;
```
`$P/BoundTimeSeries.java:34-35` `[CERT]` — el 256 se fija **en el constructor**: ni slot, ni parámetro, ni
facet. Cambiarlo exige recompilar.

**Estructura**: un array `Page[] pages` que crece por copy-on-grow (`addPage()` arma un array nuevo con
`System.arraycopy`, `:222-229`). Cada `Page` tiene `Sample[] samples` de tamaño 256 más dos cursores `first` y
`last` (ambos −1 si está vacía).

**Compresión por cambio**: `isChange()` (`:333-337`) compara distinto según el tipo — para un `BStatusValue`
dispara si cambió **el valor o el status**; para el resto, solo el valor. Cuando **no** hay cambio, en lugar de
agregar una muestra **actualiza el timestamp de la última en el lugar** (`:143-144`). `[INFER]` Es decir: una
señal plana no consume memoria, se representa como una sola muestra que se estira. Esto es lo que hace viable
un poll de 2 Hz sostenido.

**Ventana deslizante**: tras cada muestra, `trim(minTime)` avanza el cursor `first` de las páginas hasta la
primera muestra dentro de la ventana, y descarta páginas enteras reconstruyendo el array (`:238-273`). La
ventana tiene **dos modos** (`:152-158`): mientras el span acumulado es menor que `timeWindow`, ancla al primer
sample (`minTime = first`, `maxTime = first + window`); una vez superado, pasa a modo rodante
(`maxTime = now()`, `minTime = now() − window`).

## 255.5 — Los controles de pan/zoom `[CERT]`

`BAbstractPanControl` (`:26`) declara 6 `@NiagaraAction` (`panLeft/Right/Up/Down`, `zoomOut`, `noZoom`) y cada
`do*()` verifica `chart != null` y delega en el método homónimo de `BChartPane`. El campo `chart` es
`protected` — las subclases lo setean directo.

Dos implementaciones:
- **`BDockedPanControl`** — barra de herramientas de 9 comandos, con slot `zoomFactor` (default `0.15`). Su
  `ZoomHorizontalCommand` hace `clip = width * zoomFactor; chart.zoomIn(x, clip, width-clip)` (`:191-194`), y
  el vertical usa los argumentos **invertidos** (`chart.zoomIn(y, height-clip, clip)`, `:219-222`) — coherente
  con la inversión de origen del eje Y que documenta [Bloque 252] §252.1.
- **`BPanControl`** — overlay flotante de 6 botones con `computePreferredSize()` que devuelve **100×70 fijo**
  (`:73-74`) y botones en offsets de píxel cableados. No es redimensionable.

## 255.6 — Los field editors, y el matiz final sobre el registro `[CERT]`

Cuatro `BWbFieldEditor` que son la cara visible de los tipos serializables de [Bloque 253] §253.5:
`BAxisBoundFE` y `BNumericAxisBoundFE` (para `BAxisBound`), `BAxisSpecFE` (agente sobre `chart:AxisSpec`) y
`BColumnIdentifierFE` (agente sobre `chart:ColumnIdentifier`, con el selector null / rowIndex / tableColumn).

`BAxisBoundFE` cambia su sub-editor dinámicamente al cambiar el tipo del valor, reemplazando hijos con nombre
(`remove()` + `add()`, `:129-135`).

**El matiz que completa [Bloque 254] §254.3** `[CERT]`:

```java
      TypeInfo[] axisTypes = Sys.getRegistry().getTypes(BAxis.TYPE.getTypeInfo());
```
`$P/BAxisSpecFE.java:65`

El desplegable de tipo de eje del property sheet se puebla con **`getTypes()`** — *todos los subtipos
registrados de `BAxis`* — **no** con `getAgents()`. `[INFER]` Esto cierra el cuadro que venía armándose desde
B253/B254 y lo vuelve coherente:

| Vía | Mecanismo | Quién la usa |
|---|---|---|
| Resolución automática dato→eje | `getAgents()` filtrado por `BAxis.TYPE` (B253 §253.5) | **nadie** en la práctica (B254 §254.3) |
| **Elección manual del usuario** | **`getTypes()`** sobre `BAxis.TYPE` | **el property sheet de Workbench** |
| Construcción programática | `new BHoursAxis()` etc. | `analytics-wb` |

Es decir: los ejes custom de Analytics, aunque **no** sean agentes, **sí aparecen en el desplegable** del
usuario, porque basta con estar registrado como tipo. El punto de extensión funciona — por la vía manual, no
por la automática.

## 255.7 — Defectos verificados `[CERT]`

**a) Carrera de datos en `BoundTimeSeries` entre el tick y el pintado** `[CERT]`

La escritura está protegida — `sample()` toma `synchronized (this)` (`$P/BoundTimeSeries.java:133`), y
`addSample()`/`trim()` también. **La lectura no**:

```java
   @Override
   public Object getValue(int row, int col) {
      BoundTimeSeries.Sample s = this.getSample(row);
```
`$P/BoundTimeSeries.java:100-102` — sin `synchronized`, igual que `getSampleCount()` (`:189-198`).

`[INFER]` El hilo de pintado lee `pages[]` y `pages[0].first` mientras el hilo del `Clock` puede estar
reemplazando el array entero (`addPage`) o moviendo el cursor `first` (`trim`). Es una carrera real, no
teórica: el poll corre a 2 Hz de forma permanente mientras el chart esté ligado. El síntoma esperable es un
glitch de render o un `null`/índice viejo momentáneo, no corrupción persistente — pero está sin proteger.

**b) `ChartUtil.makeGradient()` pisa el color original** `[CERT]`

```java
         buf.append(" stop(0% ").append(ref).append(")");
         buf.append(" stop(0% ").append(start).append(")");
```
`$P/ChartUtil.java:28-29`

Dos paradas **en la misma posición 0%**: la segunda tapa a la primera. `[INFER]` El color `ref` — el color
original del brush de la serie — **nunca se rinde**; el gradiente va de `start` (brillo +0.1) a `stop`
(brillo −0.1). Afecta a todo `BAreaChart` ([Bloque 251] §251.2), que rellena sus polígonos con esta función.

**c) Misma carrera en `BResourceManager`** `[CERT]` — `CpuSeries.setData()` (`wb/BResourceManager.java:192`) y
`MemSeries.setData()` (`:268`) reemplazan el array `data` desde el `PollThread` (1000 ms) mientras el hilo de
pintado lo lee sin lock.

**d) `trim()` conserva una muestra fuera de ventana** — `pages[pIndex].first = sIndex - 1` (`:260`) deja una
muestra **anterior** al borde de la ventana. `[INFER]` Marcado como **probablemente intencional** (se necesita
el punto previo para dibujar el segmento que entra por el borde izquierdo), pero no se pudo confirmar la
intención desde el código: queda como UNVERIFIED, no como bug.

## 255.8 — Bonus: `wb/BResourceManager`, el chart clásico monitoreando la propia station `[CERT]`

Vista de Workbench agente sobre `fox:FoxSession`. Arma un `BChartPane` (con zoom deshabilitado) con dos
gráficos de línea: CPU (porcentajes) y memoria (KB → MB, con el máximo redondeado a múltiplo de 5 y 20% de
aire). Un `PollThread` llamado `"ui:ResourceManagerPoll"` llama cada 1000 ms a `resource.report` y
`resource.update` vía `BSysChannel.stationCall()` (`wb/BResourceManager.java:358-383`).

El eje X usa un `NullAxisRenderer` cuyo `paint()` es no-op y cuyo ancho preferido es 5.0 (`:344-356`)
`[INFER]`: el eje existe para reservar la tira de layout pero es invisible — el gráfico es por índice, no por
tiempo.

`[INFER]` Vale como ejemplo canónico de uso mínimo del chart clásico: dos `Series` a medida sobre arrays
`int[]`, un `BChartPane`, y un renderer nulo para el eje que no interesa. Es el patrón más corto para embeber
un chart en una vista de Workbench.

`wb/BFlexPane` es un splitter horizontal de dos slots con un `flex` 0-100 (`doLayout()`: `f = w * flex/100`,
`:89-97`), usado por el propio `BResourceManager`.

## 255.9 — Conexiones

- **[Bloque 252]** — **resuelto su `UNVERIFIED`**: `BAxisContainer.paint()` es quien invoca
  `axis.getRenderer().paint()` (§255.2). También explica cómo se setean `BAxisDimension`/`BAxisLocation` sin
  ser slots.
- **[Bloque 253]** — confirma desde la implementación que `BoundChartSpec` es pasivo (§255.3) y detalla el
  búfer que hay detrás del poll de 500 ms (§255.4).
- **[Bloque 254]** — **completado su §254.3**: el registro se usa por `getTypes()` en el property sheet aunque
  no por `getAgents()` en la resolución automática (§255.6).
- **[Bloque 251]** — el bug del gradiente (§255.7-b) afecta directamente a `BAreaChart`, documentado allí.
- **[Bloque 214]** (fieldeditors del editor PX) — mismo patrón `BWbFieldEditor` + registro por agente, aplicado
  acá a los tipos serializables del chart.
- **Gaps abiertos**: H6 (`pdf/` + `hx/`), H7 (`test/`), H8 (split rt/wb).
