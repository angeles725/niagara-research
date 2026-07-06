# Bloque 182 — Sistema de layout PX: CanvasPane (absoluto) vs GridPane/EdgePane/FlowPane (constraint)

> Research del focus **`px-menu`** (gap G7): cómo posiciona cada **pane** a sus widgets hijos, y el contraste
> clave entre layout ABSOLUTO (`CanvasPane`, `layout="x,y,w,h"`) y layout por CONSTRAINT / add-order
> (`GridPane`, `FlowPane`) o por slot nombrado (`EdgePane`). Decide qué pane usar para el menú vertical.
> Corrige una imprecisión de arrastre: `BBorderPane` NO es el pane de 5 regiones. NO cubre serialización de
> valores (B-G8) ni bindings.
>
> Sources (preservados §5): `sources/decompiled/bajaui-wb-layout/` — `BCanvasPane.java`, `BGridPane.java`,
> `BFlowPane.java`, `BEdgePane.java`, `BLayout.java`, `BWidget.java` (source original Tridium, `bajaui-wb`,
> `javax.baja.ui.pane` + base). Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (METHODOLOGY §3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (layout). Connects [Block 181] (gramática: `layout` es atributo del hijo), [Block 179] (framing), [Block 22].

---

## 182.1 — `layout` es una propiedad de `BWidget`, no del pane `[CERT]`

La info de posición por-hijo vive como una propiedad EN CADA widget:
`BWidget.java:204` — `public static final Property layout = newProperty(0, BLayout.DEFAULT, null);`. `[CERT]`

Por eso en el `.px` aparece como **atributo del elemento hijo** (`layout="x,y,w,h"`) — consistente con la
regla de B181 §181.3 (propiedad frozen-simple → atributo). `[CERT]`

`BLayout` define la gramática del string: *"BLayout stores an explicit layout as x, y, width, and height"*
(`BLayout.java:22-38`); cada `x`/`y` puede ser abs o percent, y `w`/`h` además aceptan `pref` (preferred). `[CERT]`

## 182.2 — `CanvasPane`: layout ABSOLUTO + viewSize/scale `[CERT]`

`CanvasPane` posiciona cada hijo por su `BLayout` absoluto. En `layoutKids`:
`BCanvasPane.java:364` — `BLayout layout = kid.getLayout();` … `BCanvasPane.java:403` — `kid.setBounds(cx, cy, cw, ch);`. `[CERT]`

El pane solo posee `viewSize` (*"the logical size of the pane's coordinate system"*) y `scale`
(`BScaleMode`: `fitWidth`/`fitRatio`/etc.) + `halign`/`valign`, que deciden cómo se escala/alinea ese view-box
en el tamaño real del pane (`BCanvasPane.java:46-93`), vía `ScaledLayout.scaleToSelf(this)` en `doLayout`
(`BCanvasPane.java:351`). `[CERT]`

Esto explica el `PxFile.px` default (`<CanvasPane viewSize="1000.0,800.0" scale="fitRatio">`) y por qué el
`menu.px` usa `layout="x,y,w,h"` por ítem. `[INFER]`

## 182.3 — `GridPane`: add-order row-major, sin constraint por hijo `[CERT]`

`columnCount` default = 2 (`BGridPane.java:61-66`, valor en 190). Los hijos fluyen **row-major por orden de
agregado** — no hay propiedad de fila/columna por hijo; la posición es implícita:
`BGridPane.java:610-636` (`int rows = kids.length/columns`, `for r… for c…`). `[CERT]`

Alineación y gaps: `halign`/`valign` (espacio sobrante del pane completo), `columnAlign` (default `left`),
`rowAlign` (default `center`, soporta `fill`) (`BGridPane.java:70-98`); `rowGap`/`columnGap` default 3
(`BGridPane.java:100-114`); `uniformRowHeight`/`uniformColumnWidth` default false, fuerzan celda al máximo
(`BGridPane.java:115-130`). `[CERT]`

## 182.4 — `EdgePane` es el pane de 5 regiones — NO `BBorderPane` `[CERT]`

**Corrección de arrastre** (`[CERT]`): el pane de 5 regiones (N/S/E/W/Center, estilo `java.awt.BorderLayout`)
es `BEdgePane`, no `BBorderPane`:
`BEdgePane.java:14-16` — *"It only supports five potential children in the frozen slots top, bottom, left,
right, and center."*. Cada región es un **slot nombrado dedicado** (`top`/`left`/`center`/`right`/`bottom`) —
la "constraint" es en qué slot lo ponés (`setTop`, `setLeft`…), no un objeto constraint. `doLayout` llena
top/bottom por alto preferido a lo ancho total, left/right por ancho preferido en la banda vertical restante,
y `center` toma lo que queda: `BEdgePane.java:343` — `c.setBounds(left, top, right-left, bottom-top);`. `[CERT]`

`BBorderPane`, en cambio, es un **decorador CSS-box**: `label` + un único slot `content`, con
`margin`/`padding`/`border`/`fill` (`BBorderPane.java:43-89`). No tiene regiones. `[CERT]` Por eso, para
"envolver" el menú con un borde, `BBorderPane` (un content + fill/border) es correcto; para regiones, `EdgePane`.

## 182.5 — `FlowPane`: solo horizontal con wrap `[CERT]`

`BFlowPane` fluye horizontalmente fila por fila (no existe propiedad de orientación vertical); envuelve a una
fila nueva cuando `moreWidth + rowWidth <= w` falla (`BFlowPane.java:394-413`). `align` (BHalign, default
`left`) alinea horizontalmente cada fila (`BFlowPane.java:40-44`); `rowAlign` (BValign, default `center`)
vertical dentro de la fila (`BFlowPane.java:449-465`); `hgap`/`vgap` default 4px (`BFlowPane.java:58-71`). `[CERT]`

## 182.6 — Qué pane conviene para el menú vertical `[INFER]`

Para un dropdown vertical (ítems apilados top→bottom, cada uno a lo ancho), lo más simple es **`GridPane`
con `columnCount=1`**: el flujo row-major apila naturalmente en vertical, sin calcular offsets. `CanvasPane`
obliga a computar la `y` de cada ítem a mano (lo que hace el `menu.px` actual de scratchpad) — funciona, pero
es overhead innecesario para un stack simple. `FlowPane` no sirve (solo horizontal). `[INFER]`

Recomendación revisada para el bloque de síntesis `menu.px` (gap G4, último del focus): ofrecer DOS variantes — `CanvasPane` (control pixel-
perfect, la que ya está) y `GridPane columnCount=1` (mantenible, se agrega/quita ítems sin recalcular). `[INFER]`

## 182.x — Connections

- **[Block 181]** — gramática: confirma que `layout` (prop de `BWidget`, §182.1) serializa como atributo del hijo.
- **[Block 179]** — framing del focus.
- **[Block 22]** — formato PX (jerarquía de panes mencionada): este bloque baja al detalle de cada pane.
- **B183** (síntesis `menu.px`) — usará §182.6 para ofrecer la variante `GridPane columnCount=1`.
- **G8** (próximo) — serialización de valores: cómo `BLayout`/`BPoint`/`BSize`/color/font se codifican a string.
