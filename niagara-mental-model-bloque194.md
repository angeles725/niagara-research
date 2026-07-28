# Bloque 194 — PX Media / perfiles: por qué el mismo .px se renderiza distinto (Wb/Hx/Mobile/bajaux)

> Research del focus **`px-editor`** (gap E4): el mecanismo de **media/perfiles** — cómo el atributo
> `media="..."` del `.px` selecciona el renderer, y por qué el MISMO archivo se comporta distinto en Workbench
> (Swing) vs Hx (HTML server-side) vs Mobile vs bajaux (web moderno). Responde la pregunta de por qué elegir
> el perfil importa (base de la decisión Workbench del focus px-menu). NO cubre theming (E5).
>
> Sources (preservados §5): `sources/decompiled/px-media/` — `BPxMedia.java` (docSource), `BHxPxMedia.java`,
> `BMobilePxMedia.java` (decompiled) + `sources/decompiled/bajaui-wb-px/PxDecoder.java` (resolución).
> Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (media). Connects [Block 181] (PxEncoder escribe `media=`), [Block 22] (WbPxMedia vs HxPxMedia), [Block 185] (BHxPxPopupBinding).

---

## 194.1 — `BPxMedia`: base abstracta, PERMISIVA por defecto `[CERT]`

`BPxMedia` (`BPxMedia.java:28`, extends `BSingleton`, `@NiagaraType`) es la abstracción de perfil. Su contrato
por defecto **acepta todo**: `isWidgetSupported`/`isBindingSupported` devuelven `true`
(`BPxMedia.java:57,66`), `validateWidget`/`validateBinding` devuelven `null` (sin warning, `:77`), y
`getPxFileOrd()` devuelve `DEFAULT_PX_FILE = BOrd.make("file:!defaults/workbench/newfiles/PxFile.px")`
(`BPxMedia.java:130-135`). Las subclases override-an para RESTRINGIR. `[CERT]`

## 194.2 — Cómo `media="..."` selecciona el renderer `[CERT]`

El atributo `media` del `.px` es un `BTypeSpec`-string. `PxDecoder.decodeHeader` lo lee y lo resuelve a un
`TypeInfo` vía el registry: `PxDecoder.java:202-212` — `String media = root.get("media", null); … this.media
= Sys.getRegistry().getType(media);`, expuesto por `getMedia()` (`:172-175`). El encoder lo escribe
(`PxEncoder.java:131-141`, `w(" media=\"").w(media).w("\"")` — el media-write de B181). `[CERT]`

Por eso el header de un `.px` de Workbench trae `media="workbench:WbPxMedia"` y uno de Hx
`media="hx:HxPxMedia"` (B22) — el mismo archivo, distinto type-spec → distinto renderer. `[INFER]`

## 194.3 — Los 4 media concretos, de más permisivo a más estricto `[CERT]`

> **↪ AMPLIADO por [Block 293] §293.14** con dos cosas que este bloque no cubrió: (a) `media` es un
> contrato de AUTORÍA, no un candado de runtime — `PxMediaValidationUtil` valida en el EDITOR, y se verificó
> en vivo que un `.px` con `WbPxMedia` renderiza sin problema bajo el perfil Hx del navegador; (b)
> `BPxMedia.getPxFileOrd()` hace que cada media pueda traer su PROPIA plantilla de archivo nuevo
> (`file:!defaults/workbench/newfiles/PxFile.px` por defecto, ancla `!` = Niagara home), que es el mecanismo
> detrás del `ScrollPane` raíz del que habla la guía de menús de navegación.

| Media | Restricción de widgets | Cita |
|---|---|---|
| **`BWbPxMedia`** (Workbench/Swing) | NINGUNA — hereda la base permisiva, override solo `getType()`. Renderiza CUALQUIER widget/binding | `BWbPxMedia` (workbench-wb) |
| **`BReportPxMedia`** | Ninguna en la clase; solo override `getPxFileOrd()` → `ReportPxFile.px` | `BReportPxMedia` (report) |
| **`BHxPxMedia`** (Hx/HTML) | **agent-gated**: un widget se soporta solo si existe un agente `BHxPxWidget` para su tipo (`AgentFilter.is(BHxPxWidget.TYPE)`); si no, NO soportado. `validateWidget` emite web-warnings | `BHxPxMedia.java:81,130` |
| **`BMobilePxMedia`** | **whitelist fija de 14 tipos** (`supportedTypes[]`): RootContainer, ScrollPane, CanvasPane, panes mobile, Button, Label, Slider, GenericFieldEditor, SetPointFieldEditor, Picture, BoundTable, Bargraph — la más estricta | `BMobilePxMedia.java:32,41` |

`[CERT]` La diferencia es CÓDIGO, no inferencia: Workbench acepta todo; Hx chequea existencia de agente por
widget; Mobile compara contra un array cerrado.

## 194.4 — Por qué esto importa para los bindings (Hx necesita `BHxPx*`) `[CERT]`

La regla de Hx (§194.3) explica por qué existen las variantes `BHxPx*Binding` que vimos en [Block 185]/[Block 193]:
en Hx, un `PopupBinding` sin su agente `BHxPxPopupBinding` NO se soporta. `[INFER]` Es la razón técnica detrás
de la recomendación del focus px-menu: **`BPopupBinding` para Workbench** (media permisiva), `BHxPxPopupBinding`
para Hx (necesita el agente). `[CERT]` (remisión Block 185)

## 194.5 — bajaux NO usa `BPxMedia` `[CERT]`

Verificado por ausencia: NO existe una subclase `BPxMedia` en `bajaux-rt`/`bajaui-ux`
(`grep -rl "PxMedia"` sobre ambos árboles = vacío). `[CERT]` Bajaux (el perfil web moderno) NO consume `.px`
por el mecanismo `BPxMedia` — es un pipeline JS/HTML5 de resolución de widgets APARTE, fuera del Px clásico
Swing-era. `[INFER]` Esto es coherente con [Block 22]/[Block 36] (bajaux = `BUx*` codegen, otra capa).

## 194.x — Connections

- **[Block 181]** — `PxEncoder`/`PxDecoder`: el `media=` que el encoder escribe y el decoder resuelve (§194.2).
- **[Block 22]** — `workbench:WbPxMedia` vs `hx:HxPxMedia` en el header: §194.3 da la mecánica de cada uno.
- **[Block 185]** — `PopupBinding` vs `BHxPxPopupBinding`: §194.4 explica POR QUÉ existe la variante Hx.
- **[Block 193]** — los `BHxPx*Binding`: agentes que Hx requiere.
- **E5** (próximo) — theming/CSS: cómo se estilan los widgets DENTRO de cada media (Palladium para Swing/bajaux).
