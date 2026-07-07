# Bloque 209 — Síntesis `px-editor-deep`: 4 hilos transversales del subsistema PX profundo (B198-B208)

> **Bloque de SÍNTESIS de cierre del focus `px-editor-deep`** (§8 terminal trigger). Consolida los 11 bloques deep
> (B198-B208) en 4 hilos transversales que ningún bloque individual expone completo. Complementa B197 (síntesis
> cross-focus del subsistema PX en 7 capas, a alto nivel) con la profundidad de esta sesión. No agrega evidencia nueva:
> remite a los bloques citados (§8 — synthesis a nivel focus).
>
> Sources: remisión a B198-B208 (todos con sus fuentes preservadas en `sources/decompiled/`). Method: consolidación.
> Markers (§3): remisión `[Block N]`; `[INFER]` para las conclusiones transversales. Tipo: SYNTHESIS/DESIGN block
> (ratio [INFER] alto ESPERADO, §11 — no es exhaustion).
>
> Capa PX (síntesis). Connects TODOS los bloques del focus + [Block 197] (síntesis previa).

---

## 209.1 — Hilo 1: `bajaux` es la base unificadora del render web (patrón type + 2 agentes) `[INFER]`

El hallazgo más transversal del focus: **una sola arquitectura de puente rt→web se repite en TODO widget web de Niagara**.
Un **type Niagara** (ej. `webChart:ChartWidget`, `kitPx:GenericFieldEditor`) tiene DOS agentes de superficie:

- **Wb (Swing)**: una clase Java real que participa del framework Workbench.
- **Ux (web)**: un shim `BSingleton implements BIJavaScriptWidget` cuyo único trabajo es `@AgentOn` + `JsInfo`→módulo JS,
  y toda la lógica vive en un `bajaux/Widget` client-side.

Confirmado en 4 módulos independientes: webChart ([Block 199] `BChartWidget`), field editors ([Block 202] `BUx*`),
easyBinding ([Block 207] `BBaseWidget`/`BEasyBinding`), y el framework en sí ([Block 204] `bajaux`). Los JS builds de
todos (`BBajauxJsBuild` subclases) se concatenan en la MISMA receta de bundling ordenada
(`BWbOrdTargetResolver`, [Block 204] §204.5). **bajaux (con spandrel, su virtual-DOM propio) es el sustrato común**; los
demás módulos son consumidores. Y es CERO-coupled con PxMedia/Swing ([Block 204] §204.6, grep negativo) — dos superficies
de render paralelas, unidas solo en la capa de Type-registration/URL. Esto CONFIRMA y cierra la hipótesis de [Block 194]. `[INFER]`

## 209.2 — Hilo 2: dos sistemas de charts + dos sistemas de símbolos gráficos `[INFER]`

El focus reveló que "graficar en PX" tiene implementaciones PARALELAS, fácilmente confundibles:

- **Charts**: el chart **clásico** `javax.baja.chart.BChartPane`/`BLineChart` (un `BWidget` Swing dibujado en el canvas,
  producido por el wizard Make Chart, [Block 201] §201.2) vs **webChart** (bajaux/D3/HTML5, [Block 199]). Son módulos
  distintos; el wizard produce el clásico, no webChart ([Block 201] §14).
- **Símbolos gráficos**: packs raster (`kitPxGraphics`/`kitPxHvac`: `BoundLabel`+imagen switcheada) vs vector
  (`kitPxN4svg`: `ui:Picture`+SVG, [Block 203]) vs componentes tipados (`kitPxBuilding`). Y el SVG se rasteriza por
  Apache Batik empaquetado ([Block 208]).

Patrón general: Niagara acumula generaciones tecnológicas en paralelo (Swing clásico → bajaux moderno; raster → vector),
sin deprecar la anterior. El editor PX debe soportar TODAS. `[INFER]`

## 209.3 — Hilo 3: el undo/redo del Workbench (`javax.baja.ui.Command`) unifica TODA mutación `[CERT]`

Establecido en [Block 191], confirmado en CADA paquete de mutación del editor: **el PX editor NO tiene pila de undo
propia** — todo cambio es un `javax.baja.ui.Command` que devuelve un `CommandArtifact` (redo/undo):

- cell-sheet edits ([Block 198] §198.3: ChangeProperty/ChangeBinding/AddBinding),
- geometry drawing ([Block 205] §205.5: MoveWidget/MorphWidget/Align/Reorg),
- editor-level ([Block 206]: Insert/Delete/AddResponsive/AddBorder),
- ORD-rewriting ([Block 198] §198.6: ChangeOrds),
- template/layer/prop sheets ([Block 198] §198.5).

Excepciones consistentes (navegación pura, no-undoable): `Select` y Copy/Paste devuelven `null`. Es una decisión
arquitectural fuerte: reutilizar el contrato Command del Workbench en vez de un sistema de undo específico de PX. `[CERT]`

## 209.4 — Hilo 4: la reutilización sobre kitPx + el contexto OEM `[INFER]`

Los sistemas "de alto nivel" del focus NO reimplementan bindings — se construyen SOBRE kitPx ([Block 193]):

- **Templates** ([Block 200]): el Graphics tab de un template ES el `BPxEditorPane` embebido ([Block 200] §200.6); un
  template reutiliza el editor PX completo.
- **easyBinding** ([Block 207]): un módulo **OEM Honeywell** (`com.honeywell.easybinding`) que envuelve un `BBoundLabel`
  kitPx + auto-converter en un widget único; `BEasyBaseBinding extends BSecureBoundLabelBinding` (linaje kitPx). Es la
  única evidencia del focus de un módulo OEM de tercero, con seguridad propia: assets **cifrados AES por license-feature**
  (branding), license-gating. Relevante para despliegues Honeywell del cliente.
- **El cell-sheet como componente reutilizable** ([Block 198]): el wizard Make ([Block 201]) y el cell-sheet
  (`MakeWidgetContext implements CellSheetContext`) comparten el mismo `BPxCellSheet`; `BMwPropertyBatch` incluso embebe
  un `BPaletteSideBar` recursivamente.

Patrón: kitPx (bindings + widgets base) es el núcleo; templates/easyBinding/wizard son capas de authoring encima. `[INFER]`

## 209.5 — Cobertura final del focus + Connections

**px-editor-deep: 11/11 gaps, B198-B208.** Grupo D (interno de `pxEditor-wb`): D1 sidebars/cell-sheet ([Block 198]),
D2 studio/dibujo ([Block 205]), D3 make/wizard ([Block 201]), D4 commands ([Block 206]), D5 field-editors ([Block 202]).
Grupo X (módulos vecinos): X1 webChart ([Block 199]), X2 templates ([Block 200]), X3 packs gráficos ([Block 203]),
X4 svgBatik ([Block 208]), X5 bajaux ([Block 204]), X6 easyBinding ([Block 207]).

- **[Block 197]** (síntesis cross-focus 7 capas): B209 profundiza sus capas "herramienta/edición" y "render web" con la
  evidencia deep de esta sesión.
- **Feeds futuros identificados** (fuera de scope, para posibles focuses nuevos): `javax.baja.chart` (chart clásico
  completo), el detalle de `kitPxBuilding` (componentes de equipo tipados), Apache Batik interno.
- **Subsistema PX completo**: con px-menu (B179-B190) + px-editor (B191-B196) + px-editor-deep (B198-B208) + las síntesis
  (B197, B209), el subsistema PX de Niagara N4 queda reconstruido end-to-end.
