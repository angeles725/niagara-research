# Bloque 180 — PX Editor (Workbench): workflow oficial — crear vista, paleta, bindear, property sheet

> Research del focus **`px-menu`** (gap G5): el **workflow oficial del PX Editor** de Niagara N4 según la
> documentación de Tridium/Honeywell — cómo un ingeniero crea una Px view, arrastra widgets, los bindea a
> datos, agrega un binding y edita el property sheet. Complementa la mecánica decompilada (B179/B180+)
> con el "cómo se usa" documentado. NO cubre la gramática interna del `.px` (B182) ni Hx/bajaux.
>
> Sources: `sources/text-extracts/docGraphics-px-editor.md` — extractos preservados (§5) de la guía oficial
> `niagara-help/docs-text/docGraphics.txt` (N4.14.0.162, sha256 `c676821a…`); rangos de línea = archivo
> original. Barrido delegado (sonnet) 2026-07-06.
> Method: lectura de doc oficial. Markers (METHODOLOGY §3): `[CERT-doc]` documento oficial preservado
> (`sources/…docGraphics.txt:N`) · `[INFER]` deducción.
>
> Capa UI/PX (Workbench). Connects [Block 179] (framing del focus), [Block 36] (bindings kitPx), [Block 22] (formato PX).

---

## 180.1 — Qué es una Px view y un archivo `.px` `[CERT-doc]`

Una **Px view** es una relación entre un archivo `.px` y un componente: *"When you create a view, you are
creating a relationship between a Px file and a component."* (`docGraphics.txt:795`). El archivo `.px`
*"defines the content and presentation of a Px view."* (`docGraphics.txt:714-721`).

**Precaución de reutilización** `[CERT-doc]`: un mismo `.px` puede alimentar varias views —
*"Editing a Px file affects all views that use that particular Px file."* (`docGraphics.txt:753-755`). Los
tipos de **Target Media** (HxPx, Ux, Workbench, Report) se fijan al crear la view (`docGraphics.txt:552-593`).

## 180.2 — Crear una Px View: el wizard `[CERT-doc]`

Right-click sobre el componente → **Views → New View** abre el **New Px View wizard**: se nombra la view,
opcionalmente se renombra el archivo `.px`, se elige el Target Media, y el wizard crea el `.px` en la carpeta
`Files` de la station y lo abre en el editor (`docGraphics.txt:793-822`).

Regla `[CERT-doc]`: cambiar el Media en el Property Sheet del componente DESPUÉS de crear la view NO cambia el
`.px` existente (`docGraphics.txt:549-550`) — el media se decide en la creación.

## 180.3 — El PX Editor: modo edición vs vista, paleta, árbol de widgets `[CERT-doc]`

- **Toggle View/Edit Mode**: un icono alterna entre ver y editar el gráfico (`docGraphics.txt:942-970`, referido en 679/968/1039 del original).
- **Widget Tree side bar**: muestra la jerarquía de widgets de la view (`docGraphics.txt:737,777-778`).
- **Palette side bar / Make Widget wizard**: de dónde salen los widgets a colocar (`docGraphics.txt:768-769`).
- **Drop**: se arrastra un widget al canvas del editor (`docGraphics.txt:948`).

## 180.4 — Bindear un widget a datos (el workflow central) `[CERT-doc]`

El concepto núcleo: un **ord enlaza un binding a un widget**, y un binding es una relación única
widget↔objeto — *"An ord links a bindings to a widget. A single binding consists of a single widget–object
relationship."* (`docGraphics.txt:1548-1566`, cita en 1553-1554).

Atajo documentado: arrastrar un componente desde el **Nav tree** al canvas auto-abre el **Make Widget
wizard** con el ord pre-cargado y una opción de fuente **"Bound Label"** (`docGraphics.txt:942-970`). Ese es
el origen del `BoundLabel` que aparece en el catálogo kitPx [Block 36]. `[INFER]`

## 180.5 — Agregar un binding explícito + property sheet `[CERT-doc]`

- **Add Binding**: en el **Properties side bar** se hace clic en **Add Binding** y se elige el tipo de binding
  de una lista (`docGraphics.txt:1572-1587`). El binding queda en *"the binding area at the bottom of the
  widget property sheet"* — es decir, un **slot hijo** del widget (confirma la mecánica de B179/B182). `[CERT-doc]`
- **Tipos de binding oficiales** (`docGraphics.txt:1748-1780`): Action, Bound Label, Field Editor, **Popup**,
  Setpoint, Spectrum, Table, **Value** — la misma familia que el decompilado de kitPx/bajaui [Block 36]. `[CERT-doc]`
- **Property Sheet**: se edita en el Properties side bar o double-click al widget abre una Properties window
  (`docGraphics.txt:1572-1587`).
- **Animate**: para animar una propiedad con un binding, *el binding debe existir primero* — regla explícita
  en la doc `[CERT-doc]` (`docGraphics.txt:1572-1587`, nota "the binding must exist before the property can be animated").

## 180.6 — Hyperlink / navegación `[CERT-doc]`

Tres mecanismos documentados para "clic → navegar":
1. **Popup Binding**: actúa como hyperlink abriendo una Px view en una ventana popup
   (`docGraphics.txt:1087-1111`) — es el patrón del focus (B181, mecánica en el decompilado).
2. **Propiedad `hyperlink`** de un binding: *"This property provides a link to another object. When used, the
   hyperlink is active in the browser or in the Px viewer."* (`docGraphics.txt:1813-1816`).
3. **`HyperlinkLabel`**: *"the HyperlinkLabel causes a mouse cursor to change to a standard link cursor and
   the component performs a hyperlink when clicked."* (`docGraphics.txt:5237-5240`).

Los tres respaldan el `menu.px` del focus: cada opción del menú es un widget con la propiedad `hyperlink`
o un `HyperlinkLabel`. `[INFER]`

## 180.7 — Reglas documentadas (ords relativos, degradeBehavior) `[CERT-doc]`

- **Portabilidad de ords**: evitar ORDs absolutos en bindings/hyperlinks para reutilizar el `.px` entre
  stations; los ORDs relativos resuelven contra el parent actual (`docGraphics.txt:4252-4334`). Esto pesa en
  cómo escribir los `hyperlink` del `menu.px`. `[CERT-doc]`
- **`degradeBehavior`**: propiedad que gobierna la degradación grácil de la UI cuando un binding no se puede
  usar (`docGraphics.txt:1748-1780`) — el mismo `degradeBehavior="hide"` visto en los `.px` reales (B182). `[CERT-doc]`

## 180.x — Connections

- **[Block 179]** — framing del focus: este bloque aporta la capa "workflow oficial" que B179 anunció (G5).
- **[Block 36]** — catálogo kitPx: los "Types of data bindings" oficiales (§180.5) coinciden con los bindings decompilados.
- **[Block 22]** — formato PX: la doc describe el USO; B22/B182 describen la GRAMÁTICA del archivo.
- **B181** (próximo tras G6-G4) — `PopupBinding`: mecánica decompilada del hyperlink-popup que §180.6 documenta desde el usuario.
- **B182** — sintaxis `.px`: `degradeBehavior`/`hyperlink`/binding-como-slot-hijo confirmados aquí desde la doc.
