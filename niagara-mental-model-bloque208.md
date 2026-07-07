# Bloque 208 — `svgBatik`: Apache Batik empaquetado + 5 clases Tridium que lo enchufan a gx:Image por ORD

> Research del focus **`px-editor-deep`** (gap X4, ÚLTIMO): el módulo `svgBatik` — el motor de render/rasterización SVG
> que Niagara usa en PX. Hallazgo central: `svgBatik` es **Apache Batik (+ Apache XML Graphics) repackaged** (~604
> clases `org.apache.batik.*`/`org.apache.xmlgraphics.*`) MÁS solo **5 clases propias Tridium** (`com.tridium.svg.batik`)
> que integran el esquema ORD de Niagara con el pipeline de imagen de Batik y enchufan Batik al sistema `gx:Image` de
> Niagara. Es el motor que rasteriza los SVG de N4svg (B203) y los SVG animados de B196. Cierra el focus `px-editor-deep`.
>
> Sources (preservados §5): `sources/decompiled/svgBatik/` — 5 `.java` Tridium (Vineflower). Las ~604 clases Apache Batik
> NO preservadas (librería externa conocida: Apache Batik SVG Toolkit). Barrido inline 2026-07-06; citas token-checked.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block.
> Citas relativas a `com/tridium/svg/batik/`.
>
> Capa PX (render SVG). Connects [Block 203] (N4svg SVG), [Block 196] (SVG animado), [Block 183] (gx:Image),
> [Block 197] (síntesis). ÚLTIMO bloque del focus px-editor-deep.

---

## 208.1 — `svgBatik` = Apache Batik repackaged + 5 clases Tridium `[CERT]`

El módulo `svgBatik-wb` contiene **858 clases**, de las cuales **~604 son Apache Batik + Apache XML Graphics**
(`org.apache.batik.*` = 286, `org.apache.xmlgraphics.*` = 318; + XML/SAX/transform de soporte). Apache Batik es el
toolkit SVG estándar de Java (parseo/DOM SVG, GVT = Graphics Vector Tree, rasterización a `BufferedImage`). Niagara NO
reimplementa SVG — **empaqueta Batik** y le agrega solo **5 clases propias** en `com.tridium.svg.batik`:
`BSvgDecoder`, `BatikOrdUtils`, `OrdParsedURLProtocolHandler`, `OrdParsedURLData`, `OrdRegistryEntry`. `[CERT]` (conteo find)

## 208.2 — `BSvgDecoder`: el puente a `gx:Image` `[CERT]`

`BSvgDecoder` (`BSvgDecoder.java:52`, `extends com.tridium.gx.awt.BImageDecoder implements BIAgent`) es el punto de
entrada: un **agente decoder** registrado en el sistema de imágenes gx de Niagara (B183). Cuando gx encuentra un
`gx:Image` que apunta a un `.svg`, este decoder lo maneja: usa el pipeline Batik —`createDocument` produce un
`SVGOMDocument`, `GVTBuilder` construye el `GraphicsNode` (el árbol vectorial), y se rasteriza a un
`java.awt.image.BufferedImage` (`imports` en `BSvgDecoder.java:3,11,31,38`)— devolviendo el bitmap que gx pinta. `[CERT]`

Es decir: los símbolos SVG de N4svg (B203) y cualquier `gx:Image` con `.svg` se renderizan rasterizando vía Batik a un
`BImage`/`BufferedImage` — no hay render SVG vectorial nativo en el canvas Swing; Batik rasteriza y gx pinta el resultado. `[INFER]`

## 208.3 — El puente ORD ↔ URL de Batik `[CERT]`

Batik resuelve recursos (el SVG mismo, o imágenes/refs DENTRO de un SVG) por su propio sistema de URLs (`ParsedURL`).
Las otras 4 clases Tridium enchufan el esquema **ORD de Niagara** a ese sistema, para que Batik pueda cargar
`module://`/`ord:` en vez de solo `http`/`file`:

- **`BatikOrdUtils`** (`BatikOrdUtils.java:8`): convierte entre ORD Niagara y URL Batik — `PREFIX = "ord://svgBatik/?ord="`
  (`:9`), `toBatikUrl(ParsedURL)`/`fromBatikUrl` mapean `ord:svgBatik` ↔ el ORD real. `[CERT]`
- **`OrdParsedURLProtocolHandler`** (`OrdParsedURLProtocolHandler.java:7`, `extends ParsedURLDefaultProtocolHandler`):
  registra el protocolo `ord:` en Batik; `parseURL` devuelve un `OrdParsedURLData(BOrd.make(fromBatikUrl(...)), ref)` (`:14-25`). `[CERT]`
- **`OrdParsedURLData`** (`OrdParsedURLData.java:12`, `extends ParsedURLData`, `protocol = "ord"`, `:20`): la data de una
  URL ORD parseada — abre el stream resolviendo el `BOrd` contra el sistema de archivos de Niagara. `[CERT]`
- **`OrdRegistryEntry`** (`OrdRegistryEntry.java:21`, `extends AbstractRegistryEntry implements URLRegistryEntry`):
  registra el handler en el **`ImageTagRegistry` de Batik** (`:16`) — así una `<image>` dentro de un SVG que referencia un
  `ord:`/`module://` la resuelve el ORD de Niagara. `[CERT]`

Resumen: Batik hace el trabajo SVG pesado; las 5 clases Tridium lo integran al modelo de recursos (ORD) y de imagen
(`gx:Image`) de Niagara. Es un patrón de **integración de librería externa**, no una reimplementación. `[INFER]`

## 208.4 — Connections + cierre del focus

- **[Block 203]** (N4svg): los 973 símbolos SVG de N4svg se rasterizan por ESTE motor — `gx:Image` `.svg` → `BSvgDecoder`
  → Batik GVT → `BufferedImage`. B203 mostró el QUÉ (símbolos SVG en palettes); B208 muestra el CÓMO (Batik los rasteriza).
- **[Block 196]** (SVG animado): un SVG animado (On.svg) se decodifica igual por Batik; la "animación" sigue siendo el
  switch de imagen por binding (B196), Batik solo rasteriza cada frame SVG.
- **[Block 183]** (gx:Image): `BSvgDecoder extends BImageDecoder` — se integra al sistema de decoders de imagen gx de B183;
  SVG es un formato de imagen más para gx, servido por Batik.
- **[Block 197]** (síntesis 7 capas): X4 cierra la sub-capa "render SVG" marcada como boundary.
- **CIERRE DEL FOCUS `px-editor-deep`**: con X4, los 11 gaps (10 originales + X6 descubierto) quedan cubiertos. Grupo D
  (D1-D5: sidebars/studio/make/commands/field-editors) + Grupo X (X1 webChart, X2 templates, X3 packs gráficos, X4 svgBatik,
  X5 bajaux, X6 easyBinding). El subsistema PX queda documentado end-to-end (B179-B208).
