# RESEARCH-STATE — Focus `px-menu` (PX Menu-Button / Dropdown en Workbench)

> Focus multi-eje (METHODOLOGY §16) del target `niagara-research`. Ámbito: cómo construir un
> **"Menu Button / Dropdown Trigger"** (estilo SLDS) en el **PX Editor de Niagara N4, perfil Workbench**.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), mismo repo/hook. Corpus en Español
> (técnico EN), por continuidad con los 178 bloques previos.
> Engram topic key: `research/niagara/px-menu/{gaps,progress}`.

## Ángulo declarado (§b2)

Reconstruir el **patrón de UI** (no un subsistema del framework): qué widgets/bindings de `bajaui`/`kitPx`
permiten emular un botón que despliega un menú vertical de opciones en un gráfico PX, con qué mecánica real
(decompilado) y qué sintaxis `.px` concreta lo materializa. Perfil objetivo: **Workbench** (no Hx/bajaux).

## Cobertura

**7 / 12 gaps** cerrados (58%).

> Backlog EXPANDIDO 2026-07-06 (pedido del usuario: documentación exhaustiva de "cómo funciona, sus reglas,
> su sintaxis"). Tres capas de evidencia: gramática autoritativa (`PxDecoder/PxEncoder` decompilados,
> `[CERT]`), workflow oficial (`niagara-help` docs, `[CERT-doc]`), y validación empírica (cientos de `.px`
> reales). El focus deja de ser solo "el menú" y pasa a documentar el **PX Editor y su formato**.

## Gap backlog (priorizado)

| Gap | Descripción | Estado | Bloque | Fuente confirmada |
|---|---|---|---|---|
| G1 | Sin widget dropdown nativo; `BMenu*`=Swing WB; mapa 2 patrones. | **cerrado** | B179 | bloque35:80, bloque36, kitPx bindings |
| G5 | **PX Editor (herramienta) — workflow oficial**: crear Px View, paleta, bind widgets, add binding, property sheet. | **cerrado** | B180 | `sources/text-extracts/docGraphics-px-editor.md` (11 rangos preservados de docGraphics.txt) |
| G6 | **Gramática autoritativa del formato PX**: `PxDecoder`/`PxEncoder` — serialización, mapeo tipo→clase vía `<import>`, atributo=prop-simple vs sub-elemento=binding, tag en 1 línea. | **cerrado** | B181 | `sources/decompiled/bajaui-wb-px/{PxDecoder,PxEncoder}.java` (preservados) |
| G7 | **Sistema de layout**: `CanvasPane` (absoluto `layout="x,y,w,h"`) vs `GridPane`/`FlowPane` (add-order) vs `EdgePane` (5 slots nombrados). `layout`=prop de `BWidget`. (§14: `BBorderPane`≠5-regiones, es CSS-box.) | **cerrado** | B182 | `sources/decompiled/bajaui-wb-layout/` (6 clases preservadas) |
| G8 | **Serialización de valores**: `BColor` (#rrggbb/#aarrggbb/nombres/rgb()), `BFont` (`[bold][italic]<size>pt <name>`), `BBrush` (solid vs linear/radialGradient + `stop(offset% color)`), `BPoint`/`BSize` (`x,y`), `BInsets` (CSS-shorthand). Lado encode autoritativo. | **cerrado** | B183 | `sources/decompiled/gx-rt/` (6 clases preservadas) |
| G12 | **`com.tridium.gx.parser.Parser`** — reconocimiento del lado PARSE (color/font/brush/insets). Refinamiento; el lado encode (G8) ya basta para authoring. | investigable `[CERT]` (baja prioridad) | — | `organized/gx/gx-rt/vineflower/com/tridium/gx/parser/Parser.java` (confirmado) |
| G9 | **Catálogo de converters** (104 clases; familia `BI*ToSimple` widget-facing). `BIBooleanToSimple` (trueValue/falseValue + type-guard) SÍ produce boolean usable para `visible`, sin coerción. Gotcha init() reseed. | **cerrado** | B184 | `sources/decompiled/converters-rt/` (5 clases preservadas) |
| G2 | `kitPx:PopupBinding` — extends BBinding, @AgentOn bajaui:Widget; trigger MOUSE_RELEASED button1 (clic, no hover); abre BNiagaraWbDialog (Workbench) con position/size absolutos; props title/position/size/modal + ord heredada. | **cerrado** | B185 | `sources/decompiled/kitPx-wb/BPopupBinding.java` (preservado) |
| G3 | Patrón in-place (toggle visibility): `BValueBinding` + converter dinámico → `visible`; estado en station; fricción del toggle. | investigable `[CERT]` | — | `bajaui-wb/javax/baja/ui/BValueBinding.java` + `BWidget.java` |
| G10 | **Ord schemes en bindings** (`slot:`, `station:`, `file:`, `history:`, `view:`, `module:`…): cómo se escriben y resuelven los hyperlinks/targets. | investigable `[CERT]` | — | `BOrd` + bloque35 (lista de schemes) |
| G11 | **`BPxInclude`** — embeber una `.px` dentro de otra (relevante para el menú como componente reutilizable). | investigable `[CERT]` | — | `docSource/.../javax/baja/ui/px/BPxInclude.java`, bloque22 |
| G4 | **Síntesis aplicada**: sintaxis verificada del `menu.px` (estructura, `Label`+`hyperlink`, converter `visible`) + gotcha XParser. Bloque culminante que ata todo. | investigable `[CERT]` | — | `.px` reales (Venom Cvahu101, hx warmupInclude, PxFile.px) |

## Clasificación del backlog (§8)

- **read-only-investigable**: 5 (G3, G10, G11, G4, G12) — TODAS con fuente confirmada
  alcanzable (2026-07-06), `[CERT]`. G5-G9 + G2 cerrados (B180-B185). G12 = baja prioridad (refinamiento).
- **requires-execution**: 0. **blocked**: 0.
- **Orden de ataque**: G5 (editor oficial) → G6 (gramática/reglas) → G7 (layout) → G8 (valores) →
  G9 (converters) → G2 (PopupBinding) → G3 (in-place) → G10 (ords) → G11 (PxInclude) → G4 (síntesis `menu.px`).
- **Fuera de ámbito** (gaps futuros si se reabre): perfil **Hx** (`BHxPxPopupBinding`) y **bajaux** moderno.

## Fuentes cross-target (niagara-help = target #3)

G5 cita documentación oficial que vive en el install `niagara-help`, no en el repo del corpus. Al escribir
el bloque G5, **preservar** los extractos relevantes en `sources/` de este target y registrarlos en
`SOURCES.md` (§5), citando el `.pdf/.txt` preservado — no la ruta volátil del install.

## Historial de iteraciones

| Iter | Gap | Bloque | Delegado? · tier | Resultado |
|---|---|---|---|---|
| 1 | G1 | B179 | no · inline | Framing: sin widget nativo; `BMenu*` = Swing WB (bloque35:80); mapa 2 patrones. |
| 2 | G5 | B180 | yes · sonnet | Workflow oficial PX Editor (docGraphics.txt): New Px View wizard, paleta/árbol, bind por drag-ord, Add Binding=slot hijo, tipos de binding oficiales, hyperlink/HyperlinkLabel, reglas ords relativos + degradeBehavior. 11 rangos preservados en sources/. |
| 3 | G6 | B181 | yes · sonnet | Gramática PxDecoder/PxEncoder: raíz `px`/`version="1.0"`, secciones fijas, tipo resuelto vía `<import>` (no namespace), atributo=prop-frozen-simple no-default vs sub-elemento=binding/slot-hijo, tag en 1 línea (raíz del gotcha XParser), errores=XException. 2 fuentes preservadas. |
| 4 | G7 | B182 | yes · sonnet | Layout: `layout`=prop de `BWidget` (por eso atributo del hijo). CanvasPane=absoluto+viewSize/scale; GridPane=row-major add-order (columnCount def 2); FlowPane=horizontal+wrap; EdgePane=5 slots nombrados. §14: BBorderPane≠5-regiones (es CSS-box). Menú vertical → GridPane columnCount=1. 6 clases preservadas. |
| 5 | G8 | B183 | yes · sonnet | Serialización gx (lado encode): BColor #rrggbb/#aarrggbb+nombres+rgb(); BFont `[bold][italic][underline]<size>pt <name>`; BBrush BNF gradientes + `stop(offset% color)`; BPoint/BSize `x,y`; BInsets CSS-shorthand. Parser (parse-side) ubicado en vineflower → gap G12. 6 clases gx preservadas. |
| 6 | G9 | B184 | yes · sonnet | Converters (104 clases): familia BI*ToSimple=widget-facing. BConverter.convert(from,to,ctx) contrato. BIBooleanToSimple: trueValue/falseValue (default TRUE/FALSE) + type-guard getType()==to.getType() → SÍ produce boolean para `visible` sin coerción. Gotcha init() reseed ambos slots al valor actual. Hermanos BINumericToSimple (lookup-map), BIStatusToSimple (status bits). 5 clases preservadas. |
| 7 | G2 | B185 | no · inline | PopupBinding: extends BBinding @AgentOn Widget; started() linkTo mouseEvent; released() con isButton1Down()→popup() (clic, no hover; corrige bloque36); popup() abre BNiagaraWbDialog vía BWbShell (Workbench) con position/size absolutos → NO anclado. Props title/position(100,100)/size(800x600)/modal. 1 fuente preservada. |

## Notas

- Toda la evidencia primaria fue leída READ-ONLY en la sesión de origen (2026-07-06); las fuentes están
  confirmadas alcanzables antes de abrir cada iteración (SOURCE-BEFORE-AGENT).
- Perfil **Hx** (`BHxPxPopupBinding`) y **bajaux** quedan fuera de ámbito de este focus (el usuario fijó
  Workbench); se anotan como gaps futuros si se reabre.
