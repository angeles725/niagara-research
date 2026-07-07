# Bloque 197 — Síntesis cross-focus: el subsistema PX de Niagara N4 end-to-end

> Research SÍNTESIS a nivel CORPUS (§8 terminal): consolida los dos focuses PX — `px-menu` (B179-B190) y
> `px-editor` (B191-B196) — en un mapa único del subsistema de presentación de Niagara N4, de archivo `.px` a
> pixel. NO agrega evidencia nueva: es integración con remisión a los 18 bloques que la sostienen.
>
> **Tipo de bloque: DESIGN/SYNTHESIS** — ratio `[INFER]/[CERT]` alto ESPERADO y sano (§11); las conexiones
> son inferencia respaldada por los `[CERT]`/`[CERT-doc]` de los bloques citados.
>
> Sources: [Block 179-196] (todo el corpus PX). Method: integración cross-focus. Markers (§3): `[CERT]`
> (remisión a bloque con `file:line`) · `[INFER]` (síntesis).
>
> Capa PX (síntesis). Connects TODOS los bloques PX (179-196).

---

## 197.1 — El subsistema PX en 7 capas `[INFER]`

Todo lo documentado se ordena en 7 capas, cada una con su(s) bloque(s):

| # | Capa | Qué es | Bloques |
|---|---|---|---|
| 1 | **Formato** | La gramática XML del `.px` (`PxDecoder`/`PxEncoder`): elemento=widget, atributo=prop, sub-elemento=binding, tag-en-1-línea | B181 · B22 |
| 2 | **Herramienta** | El editor (`pxEditor-wb`): `BStudio`, paleta→`BMakeWidget` wizard, clone por encode↔decode, opciones | B191 · B180 (workflow oficial) |
| 3 | **Catálogo** | Los widgets (bajaui: botones/inputs/contenedores/datos; kitPx: meters/bargraph) y su layout (panes) | B192 · B182 · B36 |
| 4 | **Binding** | Cómo el dato mueve al widget: `ValueBinding`+converter, los 11 bindings, ords | B184 · B185 · B186 · B193 · B187 |
| 5 | **Valores** | La serialización de cada propiedad (color/font/gradiente/point/size) | B183 · B190 |
| 6 | **Render** | El media/perfil que decide DÓNDE y con qué reglas se dibuja (Wb/Hx/Mobile/bajaux) | B194 |
| 7 | **Estilo + Animación** | Theming (Palladium/CSS) + animación=data-binding | B195 · B196 |

## 197.2 — El pipeline de un `.px`: de archivo a pixel `[INFER]`

1. Un `.px` es XML con `media="..."` que un **`BPxMedia`** resuelve (B194 §194.2). `[CERT]`
2. **`PxDecoder`** parsea: nombre de elemento → tipo vía `<import>`, atributos → props simples, sub-elementos →
   bindings/slots (B181 §181.2-4). `[CERT]`
3. Se arma un grafo de **`BWidget`** (no `BComponent`) rooteado en `BRootContainer` (B191 §191.1). `[CERT]`
4. El **layout** posiciona: `CanvasPane` absoluto o `GridPane`/`EdgePane` por constraint (B182). `[CERT]`
5. Los **bindings** se suscriben a sus ords y, al cambiar el dato, `getOnWidget`+converter escriben la
   propiedad del widget (B186 §186.2, B184) — esto ES la "animación" (B196). `[CERT]`
6. El **tema** (Palladium Java para Swing, `.ux-*` CSS para web) pinta el look base; el `.px` overridea
   colores/fuentes puntuales (B195, B183). `[CERT]`
7. El **media** filtra qué se soporta: Workbench todo, Hx solo con agente `BHxPxWidget`, Mobile whitelist
   (B194 §194.3). `[CERT]`

## 197.3 — Ejes transversales (lo que aparece en varias capas) `[INFER]`

- **El codec `PxEncoder`/`PxDecoder` es el pivote**: no solo carga/guarda (B181), también CLONA widgets en el
  editor (encode→decode, B191 §191.4) y resuelve el `media=` (B194). Documentarlo en px-menu se pagó en px-editor. `[CERT]`
- **El converter dinámico es el motor universal**: el mismo `getOnWidget` + slot-`BConverter` sirve para
  `visible` (menú in-place, B186/B184), color (`Spectrum`, B193), status (`BoundLabel`, B193) y toda
  "animación" (B196). `[CERT]`
- **El media determina TODO aguas abajo**: qué bindings existen (`BHxPx*` en Hx, B194 §194.4), qué theming
  aplica (Java vs CSS, B195), y por qué se eligió Workbench para el menú (B185/B194). `[CERT]`
- **PX no tiene estado ni scripting**: sin `BPxScript` (B22/B181), el estado vive en la station — la fricción
  del toggle del menú in-place (B189 §189.4). `[CERT]`

## 197.4 — El menú (la pregunta original) situado en el mapa `[INFER]`

La pregunta que abrió todo — "¿un Menu Button en el PX editor?" — toca las 7 capas: `[INFER]`
sin widget dropdown nativo (B179, capa 3) → se emula con `PopupBinding` (ventana) o `ValueBinding`+converter
sobre `visible` (in-place, capas 4-5) → escrito en `.px` con tag-1-línea (capa 1) → posicionado con
`GridPane columnCount=1` (capa 3) → navegando por ords `hyperlink` (capa 4) → estilado por Palladium con
overrides (capa 7) → válido en Workbench por su media permisiva (capa 6). El `menu.px` de B189 es la
integración de las 7 capas en un archivo. `[CERT]` (remisión Block 189)

## 197.5 — Mapa maestro de los 18 bloques `[CERT]`

| Bloque | Foco | Tema |
|---|---|---|
| B179 | px-menu | Framing: sin widget nativo, 2 patrones |
| B180 | px-menu | Workflow oficial del editor (`[CERT-doc]`) |
| B181 | px-menu | Gramática `PxDecoder`/`PxEncoder` |
| B182 | px-menu | Layout de panes (§14 BBorderPane) |
| B183 | px-menu | Serialización de valores gx |
| B184 | px-menu | Catálogo de converters (type-guard) |
| B185 | px-menu | `PopupBinding` (patrón A) |
| B186 | px-menu | `BValueBinding` (patrón B) |
| B187 | px-menu | Ord schemes |
| B188 | px-menu | `BPxInclude` |
| B189 | px-menu | Síntesis `menu.px` |
| B190 | px-menu | gx Parser round-trip |
| B191 | px-editor | La herramienta `pxEditor-wb` |
| B192 | px-editor | Catálogo de widgets |
| B193 | px-editor | Los 9 bindings kitPx |
| B194 | px-editor | Media/perfiles |
| B195 | px-editor | Theming |
| B196 | px-editor | Animación=data-binding |

`[CERT]` 18 bloques, ~35 clases del framework preservadas en `sources/decompiled/`, 3 capas de evidencia
(decompilado + doc oficial + `.px` reales).

## 197.x — Connections

- **[Block 179-190]** (px-menu) — capas 1,3,4,5 + los 2 patrones del menú.
- **[Block 191-196]** (px-editor) — capas 2,3,4,6,7 (la herramienta, el render, el estilo).
- **[Block 22]** — el bloque base que introdujo PX/bajaui a alto nivel; este corpus lo bajó a `file:line`.
- **[Block 36]** — catálogo kitPx: la fuente del "no hay dropdown nativo" (capa 3).
