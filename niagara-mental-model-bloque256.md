# Bloque 256 — Chart clásico (VI): las salidas no-Swing — PDF y el perfil Hx

> **Qué documenta**: las dos rutas por las que un chart clásico sale del canvas Swing — `com.tridium.chart.pdf`
> (exportación a PDF, 2 clases) y `com.tridium.chart.hx` (el puente al perfil web **Hx** legacy, 1 clase).
> Cierra el gap **H6** del focus `px-chart-classic`.
>
> **Contiene un matiz §14 a [Bloque 254] §254.8**: el veredicto "Workbench = clásico / browser = webChart"
> tiene un **tercer caso** que no había visto. Ver §256.3.
>
> **Fuentes** (decompilado vineflower, **leídas íntegras inline por el driver** — las 3 clases suman 180
> líneas, por debajo del umbral de delegación del kit):
> - `$P` = `/home/cristian/modules/Prototipos/modulos/organized/chart/chart-wb/vineflower/com/tridium/chart/`
>
> **Método**: sin sub-agente. El driver leyó los 3 archivos completos y cita línea por línea. Marcadores:
> `[CERT]` = fuente primaria; `[INFER]` = deducción. Bloque de EVIDENCIA.

---

## 256.1 — `BPdfChartPane`: exportar a PDF MUTANDO el chart vivo `[CERT]`

```java
public class BPdfChartPane extends BChartPane implements BIPdfWidget {
```
`$P/pdf/BPdfChartPane.java:24` `[CERT]` — agente sobre `chart:ChartPane` (`:19-21`).

El método interesante es `fromWidget(BWidget widget, PdfOp op)` (`:32`), que **clona el pane para PDF**
copiando todas sus propiedades. Pero cuando la propiedad es un `BChart`, hace algo que no es una copia:

```java
            a.setModel(new BoundChartModel());
            b.setModel(m);
```
`$P/pdf/BPdfChartPane.java:49-50` `[CERT]`

`a` es el chart **original en pantalla**, `b` es la copia para PDF. La secuencia es: se guarda el modelo de
`a`, **se le instala a `a` un `BoundChartModel` vacío**, y se le pasa el modelo original a `b`.

`[INFER]` Es un **traspaso, no una copia**: para exportar, el chart en vivo se queda momentáneamente sin sus
datos. Es una elección deliberada — evita duplicar en memoria un modelo que puede tener un `TableSeries` con
toda una tabla dentro ([Bloque 251] §251.3) —, pero deja el widget de pantalla en un estado inconsistente
durante la exportación.

**La prueba de que Tridium sabía de este efecto** está en el llamador (§256.2).

Los bindings sí se copian, con una llamada de framework por opcode:
```java
               temp.fw(303, binds[i].getTarget(), null, null, null);
```
`$P/pdf/BPdfChartPane.java:55` `[CERT]` — `[INFER]` `fw(303, …)` es el mecanismo interno de re-target de un
binding clonado; el opcode numérico es propio del framework y no está documentado en la API pública.

Cierra con `prepare()`, `getController().setTraceOn(false)` (`:62`) y `build()`. `[INFER]` Desactivar la traza
tiene sentido: el cursor de traza es interacción de mouse ([Bloque 255] §255.5) y en un PDF no hay mouse.

## 256.2 — `BResourceManagerToPdf`: y el `refresh` que repara el destrozo `[CERT]`

```java
public class BResourceManagerToPdf extends BPdfExporter implements BIWbViewExporter {
```
`$P/pdf/BResourceManagerToPdf.java:22` `[CERT]` — agente sobre `chart:ResourceManager`.

`export(PdfOp op)` arma un `PdfDocument` con el stream y las dimensiones del `op` (`:30`), toma el chart de
sistema del Resource Manager ([Bloque 255] §255.8), lo pasa por `BPdfChartPane.fromWidget()` (`:35`), lo
envuelve en un `BPdfFlowPane` y lo renderiza con `BWidgetToPdf`.

Y la última línea del método:

```java
         manager.getWbShell().getRefreshCommand().doInvoke(new CommandEvent(null));
```
`$P/pdf/BResourceManagerToPdf.java:41` `[CERT]`

`[INFER]` **Esto confirma §256.1**: después de exportar, se fuerza un refresh de la shell de Workbench. Si
`fromWidget()` fuera una copia limpia, no haría falta refrescar nada. El refresh está ahí para reconstruir la
vista cuyo chart quedó con un `BoundChartModel` vacío. Es una reparación explícita de un efecto colateral
conocido — no un detalle cosmético.

## 256.3 — `BHxPxChartPane`: §14 — el chart clásico SÍ llega al browser, degradado `[CERT]`

```java
@NiagaraSingleton
public class BHxPxChartPane extends BHxPxGraphics {
```
`$P/hx/BHxPxChartPane.java:22-23` `[CERT]` — singleton, agente sobre `chart:ChartPane`.

Es el puente del chart clásico al perfil **Hx** (el perfil web legacy de Niagara, documentado en
[Bloque 194]). Su `update(width, height, loaded, HxOp op)` (`:38-56`): hace `prepare()`, apaga la traza
(`:41`), `build()`, **propaga los facets del `HxOp` a todos los ejes X e Y** (`:47`, `:51`) — que es como el
formato de fecha del cliente llega al eje temporal ([Bloque 252] §252.3) —, fija los bounds al tamaño pedido y
corre un **layout recursivo manual** sobre el árbol de widgets (`:59-66`).

Dos líneas definen la experiencia resultante:

```java
   public BWidget[] getChildWidgets(BWidget widget, Context cx) {
      return new BWidget[0];
```
`$P/hx/BHxPxChartPane.java:34-35` `[CERT]` — **cero widgets hijos expuestos**.

```java
   public MouseEventCommand getMouseEventHandler() {
      return null;
```
`$P/hx/BHxPxChartPane.java:68-69` `[CERT]` — **sin manejador de eventos de mouse**.

`[INFER]` La conclusión es directa: servido por Hx, el chart clásico es una **imagen monolítica y muerta**. Sin
hijos navegables, sin zoom, sin pan, sin cursor de traza — todo el aparato de interacción de
`BChartPane`/`ChartController`/`BPanControl` ([Bloque 252] §252.6, [Bloque 255] §255.5) queda inaccesible. Se
renderiza del lado del servidor y se manda el resultado.

### El matiz a [Bloque 254] §254.8

Allí concluí, con la evidencia que tenía, que el perfil decide: *Workbench → clásico, browser → `webChart`*.
Con `BHxPxChartPane` a la vista, el cuadro correcto tiene **tres** casos, no dos:

| Perfil | Motor | Interacción |
|---|---|---|
| Workbench (Swing) | chart clásico | completa (zoom, pan, traza, menú de exportación) |
| Browser moderno (bajaux) | `webChart` D3 ([Bloque 199]) | completa, del lado del cliente |
| **Browser Hx (legacy)** | **chart clásico renderizado en servidor** | **ninguna** |

`[INFER]` El veredicto de fondo de B254 no se cae — `webChart` sigue siendo el camino web *vivo*, y el clásico
sigue sin estar deprecado. Lo que se corrige es el "browser ⇒ webChart" como absoluto: existe una tercera ruta,
legacy y degradada, por la que el chart clásico llega igual al navegador. Encaja con lo que [Bloque 194]
documentó del perfil Hx como capa agent-gated de menor capacidad.

## 256.4 — Conexiones

- **[Bloque 254]** — **matizado por §256.3**: el veredicto clásico-vs-web tiene un tercer caso (Hx).
- **[Bloque 194]** (media/perfiles Wb/Hx/Mobile) — `BHxPxChartPane` es un caso concreto de lo que aquel bloque
  describió como el perfil Hx agent-gated.
- **[Bloque 255]** — `BResourceManagerToPdf` exporta justamente el `BResourceManager` documentado en §255.8;
  y el `setTraceOn(false)` de ambas salidas apaga la interacción descrita en §255.5.
- **[Bloque 251]** — el traspaso de modelo de §256.1 se entiende por el costo del `Tables.slurp()`.
- **[Bloque 252]** — la propagación de facets a los ejes (§256.3) es el override de formato temporal descrito
  en §252.3.
- **Gaps abiertos**: H7 (`test/`, 8 clases), H8 (split rt/wb, 5 clases).
