# Bloque 196 — Animación en PX: "animar" = data binding (+ SVG animado)

> Research del focus **`px-editor`** (gap E6, ÚLTIMO): qué significa "animar" un gráfico PX. Hallazgo central:
> en Niagara, **animación = data binding** — no es tweening temporal. Documenta los dos sentidos (binding de
> datos + imágenes SVG animadas) y el live-preview del editor. Bloque conciso de cierre del focus.
>
> Sources: `sources/text-extracts/docGraphics-px-editor.md` (rango 1522-1560, Cap.4) + `docGraphics.txt`
> original · `sources/decompiled/pxEditor-wb/BPxEditor.java` (PxEditorBinder). Barrido inline 2026-07-06.
> Method: doc oficial + decompilado. Markers (§3): `[CERT-doc]` doc preservado · `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (animación). Connects [Block 184]/[Block 186] (bindings), [Block 180] (workflow Animate), [Block 191] (animateBindings).

---

## 196.1 — "Animar" = bindear una propiedad a un dato `[CERT-doc]`

El Cap.4 de la guía oficial se titula literalmente *"Animating graphics (data binding)"*. La animación NO es
interpolación temporal — es que el widget CAMBIA con el dato bound: *"Widgets are animated by binding any
widget properties to a legitimate data source"* (`docGraphics.txt:1541-1545`), *"Animated graphics change, or
update, based on data values that come from … sources that are connected (or bound) to them"*
(`docGraphics.txt:1534-1537`). `[CERT-doc]`

Es decir: "animar una propiedad" = el mecanismo de converter dinámico que ya documentamos (B184/B186). Un
`visible` que sigue a `menuOpen`, un color que sigue a un `Spectrum`, un texto que sigue al `out` — todo eso
es "animación" en el vocabulario Niagara. `[INFER]` (remisión Block 184, Block 186)

## 196.2 — El live-preview del editor: `PxEditorBinder` + `animateBindings` `[CERT]`

En modo edición, el editor puede PREVISUALIZAR la animación en vivo. `BPxEditor.PxEditorBinder`
(`BPxEditor.java:348-363`, extends `Binder`) sobreescribe `updateBindings`: solo actualiza los bindings y
repinta **si la opción `animateBindings` está activa** — `if (editorPane != null &&
BPxEditorOptions.make().getAnimateBindings()) { … updateBinding(...); editorPane.repaint(); }`. `[CERT]`

Esto conecta el `animateBindings` de `BPxEditorOptions` (B191 §191.5): con la opción on, el diseñador ve el
gráfico moverse con datos reales mientras edita; off, ve solo el layout estático. `[CERT]` (remisión Block 191)

## 196.3 — El segundo sentido: imágenes SVG animadas `[CERT-doc]`

La doc lista un segundo camino: *"Animate using static SVG images"* (`docGraphics.txt:1530`) — visuales tipo
"rotating fan" (`docGraphics.txt:1538`) logrados con assets SVG (renderizados por el motor gx/batik). `[CERT-doc]`

Límite documentado `[CERT-doc]`: las features INTERACTIVAS del SVG (animaciones JavaScript embebidas) NO se
soportan (`docGraphics.txt:1134`). El SVG se usa como imagen (posiblemente multi-estado por binding), no como
animación JS autónoma. `[INFER]`

## 196.4 — Cierre: el focus px-editor completo `[INFER]`

Con E6, el PX editor queda documentado en amplitud: la herramienta (B191), los widgets (B192), los bindings
(B193 + B185/B186), el media/perfiles (B194), el theming (B195) y la animación (B196). Sumado al focus
px-menu (formato/gramática/menú, B179-B190), el subsistema PX está reconstruido end-to-end: **formato →
herramienta → catálogo → binding → render → estilo → animación**. `[INFER]`

## 196.x — Connections

- **[Block 184]/[Block 186]** — los converters/bindings que SON la "animación" de datos (§196.1).
- **[Block 180]** — workflow "Animate" (el binding debe existir primero): §196.1 da el significado real.
- **[Block 191]** — `animateBindings` de `BPxEditorOptions`: §196.2 muestra su efecto (live-preview vía PxEditorBinder).
- **[Block 179-195]** — este bloque cierra el focus px-editor; ver §196.4 para el mapa end-to-end.
