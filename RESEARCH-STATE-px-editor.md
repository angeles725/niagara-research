# RESEARCH-STATE — Focus `px-editor` (el PX Editor completo, más allá del menú)

> Focus multi-eje (METHODOLOGY §16) del target `niagara-research`. Ámbito: documentar el **PX Editor de
> Niagara N4 en amplitud** — la herramienta misma, el catálogo completo de widgets/bindings, media/perfiles,
> theming y animación. Continúa donde `px-menu` (B179-B190, CERRADO) paró: aquel cubrió la rebanada vertical
> del menú; éste cubre el resto de la superficie del editor.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), desde B191. Corpus en Español (técnico EN).
> Engram topic key: `research/niagara/px-editor/{gaps,progress}`.

## Ángulo declarado (§b2)

Reconstruir el PX Editor como SUBSISTEMA: (a) el módulo `pxEditor-wb` (la herramienta que edita `.px`),
(b) el catálogo completo de widgets `bajaui` y bindings `kitPx` que un `.px` puede contener, (c) cómo el mismo
`.px` se renderiza en distintos perfiles (media), (d) theming y animación. Complementa `px-menu` (mecánica del
formato) con la superficie de la herramienta y su paleta.

## Cobertura

**2 / 6 gaps** cerrados (33%). Bootstrap 2026-07-06.

## Gap backlog (priorizado)

| Gap | Descripción | Estado | Bloque | Fuente confirmada |
|---|---|---|---|---|
| E1 | El editor como herramienta: `BPxEditor` extends BWbPxView (load/save round-trip por PxEncoder/Decoder); `BPxEditorPane` (canvas BEdgePane+BStudio); paleta→canvas (WidgetFactory→BMakeWidget wizard→BMwFromPalette); clone por encode→decode + placeholder ord; `BPxEditorOptions` (per-usuario); undo delegado al Workbench. | **cerrado** | B191 | `sources/decompiled/pxEditor-wb/` (7 clases preservadas) |
| E2 | Catálogo widgets bajaui: jerarquía botones (BAbstractButton→BToggleButton→BCheckBox/Radio); inputs BTextField/BSlider; contenedores BTabbedPane/BSplitPane/BScrollPane; datos BTable/BTree/BList (por MODELO, no props .px). Boundary: charts en kitPx/webChart. | **cerrado** | B192 | `sources/decompiled/bajaui-wb-widgets/` (6 clases) |
| E3 | **Los 9 bindings `kitPx` restantes**: Action, BoundLabel, ButtonGroup, IncrementSetPoint, MomentaryToggle, MouseOver, SetPoint, Spectrum, SpectrumSetpoint — mecánica y uso de cada uno. | investigable `[CERT]` | — | `organized/kitPx/kitPx-wb/vineflower/com/tridium/kitpx/*Binding.java` |
| E4 | **Media / perfiles**: `BPxMedia`/`BWbPxMedia`/HxPxMedia/UxPxMedia — cómo el mismo `.px` se resuelve y renderiza en Workbench vs Hx vs bajaux. | investigable `[CERT]` | — | `bajaui-wb/.../BPxMedia.java` + `workbench-wb/.../BWbPxMedia.java` |
| E5 | **Theming / CSS**: tema Palladium, `bajaui.css`, `theme.css` (JavaFX) — cómo se estilan los widgets, clases CSS, override de tema. | investigable `[CERT]` | — | `bajaui-ux/extracted/rc/bajaui.css`, `bajaui-wb/extracted/rc/fx/theme.css` |
| E6 | **Animación**: la feature "Animate" (docGraphics) + el motor gx/batik — cómo una propiedad bound anima. | investigable `[CERT]`/`[CERT-doc]` | — | `gx-wb` batik + `sources/text-extracts/docGraphics-px-editor.md` (Animate) |

## Clasificación del backlog (§8)

- **read-only-investigable**: 4 (E3-E6) — todas con fuente confirmada. E1/E2 cerrados (B191-B192).
- **requires-execution**: 0. **blocked**: 0.
- **Orden de ataque**: E1 (editor tool) → E2 (widgets) → E3 (bindings) → E4 (media) → E5 (theming) → E6 (animación).

## Historial de iteraciones

| Iter | Gap | Bloque | Delegado? · tier | Resultado |
|---|---|---|---|---|
| 1 | E1 | B191 | yes · sonnet | pxEditor-wb tool: BPxEditor extends BWbPxView, load/save+clone round-trip PxEncoder/Decoder, árbol BWidget en BRootContainer (no BComponent). BPxEditorPane canvas (BEdgePane+BStudio facade+SelectedWidgets, edit vs readonly). Paleta→canvas: WidgetFactory chain→NavNodeFactory→BMakeWidget wizard→BMwFromPalette (BPaletteSideBar embebido, placeholder ord `<ord>`). BPxEditorOptions per-usuario (grid/snap/animateBindings/preserveIdentities). Undo delegado al Workbench Command/CommandArtifact. 7 clases preservadas. |
| 2 | E2 | B192 | yes · sonnet | Catálogo widgets: BAbstractButton(ext BLabel)→BToggleButton(selected)→BCheckBox/BRadio(halign). BTextField(visibleColumns, ext BTextEditor). BSlider(min/max/increment/value, ext BWidget). Contenedores BTabbedPane(tabPlacement,addPane)/BSplitPane(widget1/2,dividerPosition)/BScrollPane(content,h/vpolicy). Datos BTable/BTree/BList (ext BTransferWidget, por MODELO no props). §14: BZoomPane/DashboardPane NO en bajaui core. Charts en kitPx/webChart. 6 clases preservadas. |

## Notas

- Continúa el trabajo de `px-menu` (CERRADO 12/12). Reutiliza fuentes ya preservadas (PxDecoder/Encoder,
  panes, gx, converters) donde aplique; remisión a B179-B190 para lo ya documentado.
- El usuario pidió (2026-07-06) seguir documentando el PX editor en amplitud tras cerrar el menú.
