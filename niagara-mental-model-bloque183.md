# Bloque 183 — Serialización de valores de propiedad PX (gx): color, font, gradiente, point/size/insets

> Research del focus **`px-menu`** (gap G8): la **gramática de cada valor** que un autor escribe como atributo
> en un `.px` — colores, fuentes, gradientes, puntos, tamaños, insets. Documenta el lado ENCODE (autoritativo
> para hand-authoring: es lo que el encoder produce, por ende lo válido). El lado PARSE (`com.tridium.gx.parser.Parser`)
> se ubica pero no se profundiza (gap G12). NO cubre widgets ni bindings.
>
> Sources (preservados §5): `sources/decompiled/gx-rt/` — `BColor.java`, `BFont.java`, `BBrush.java`,
> `BPoint.java`, `BSize.java`, `BInsets.java` (source original Tridium, `gx-rt`, `javax.baja.gx`).
> Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado + contraste con `.px` reales. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (valores). Connects [Block 181] (atributo=valor encodeToString), [Block 182] (layout), [Block 22].

---

## 183.1 — Contrato general: `BSimple.encodeToString()` / `decodeFromString()` `[CERT]`

Los seis tipos son subclases de `BSimple` que implementan `encodeToString()`/`decodeFromString(String)`
(p.ej. `BColor.java:291-346`, `BPoint.java:115-152`). Este es el contrato que `PxEncoder` usa para emitir/
parsear el TEXTO del atributo en el `.px` (B181 §181.3). `[CERT]`

Distinción útil: `BPoint`/`BSize` decodifican INLINE (comma-split + `BDouble.decode`, sin parser externo);
`BColor`/`BFont`/`BBrush`/`BInsets` delegan el parse a `com.tridium.gx.parser.Parser`. `[CERT]`

## 183.2 — `BColor`: `#rrggbb` / `#aarrggbb` + nombres + `rgb()/rgba()` `[CERT]`

Formas aceptadas (javadoc `BColor.java:28-36`): `#rgb`, `#rrggbb`, `#aarrggbb`, `rgb(n,n,n)`,
`rgba(n,n,n,alpha)`, y keywords HTML4/CSS3/SVG. `[CERT]`

La tabla de nombres es un `Hashtable<String,BColor>` estático poblado con `constant(name, rgb)`
(`BColor.java:353-512`): `white=0xffffff` (`:509`), `red=0xff0000` (`:485`), `transparent=0x00ffffff` con
alpha (`:365`). Lookup case-insensitive: `constants.get(TextUtil.toLowerCase(name))` (`BColor.java:63-66`). `[CERT]`

`encodeToString()` escribe `#rrggbb` (6 nibbles) salvo que alpha≠255, donde escribe `#aarrggbb` (8 nibbles)
(`BColor.java:311-332`). Por eso los `.px` reales muestran `background="#f4f6f9"` o `foreground="white"` —
ambas formas válidas. `[CERT]`

## 183.3 — `BFont`: `[bold] [italic] [underline] <size>pt <name>` `[CERT]`

Gramática exacta (javadoc `BFont.java:26-27`): `"[bold] [italic] [underline] <size>pt <name>"`, ej.
`"bold italic 12pt Times New Roman"`. El orden lo confirma `make()` (`BFont.java:60-71`): flags bold→italic→
underline (cada uno palabra literal + espacio), luego `<size>pt ` (`BDouble.encode`), y el **nombre de familia
al final** (puede tener espacios, sin comillas, DEBE ser el último token). Default: `sans-serif`, 12pt, plain
(`BFont.java:448`). `[CERT]`

Esto valida el `font="bold 12.0pt Arial"` del `menu.px`. `[INFER]`

## 183.4 — `BBrush`: solid vs `linearGradient`/`radialGradient` + `stop(offset% color)` `[CERT]`

BNF completa en el javadoc (`BBrush.java:26-52`) — la referencia autoritativa:
```
brush          := color | inverse | linearGradient | radialGradient | image
linearGradient := linearGradient ( [spread] [angle] stop* )
radialGradient := radialGradient ( [spread] [center] [radius] [focal] stop* )
stop           := stop( offset color )
offset         := double %
```
Un brush **sólido** encodea como el color pelado: `Solid.toString() → color.encodeToString()`
(`BBrush.java:321-325`). Un **gradiente** lo arma `Gradient.toString()` (`BBrush.java:420-443`):
`"linearGradient"`/`"radialGradient"` + `(` + spread (`reflect`/`repeat` si no-default; `pad` se omite) +
campos no-default (`angle(...)` para linear, `:490`) + cada stop. `Stop.toString()` (`BBrush.java:648-652`):
`"stop(" + BDouble.encode(offset) + "% " + color + ")"`. `[CERT]`

Coincide EXACTAMENTE con el `background="linearGradient( stop(0.0% red) stop(100.0% #d1f2ff) )"` visto en
`.px` reales (hx `warmupInclude.px`). `[CERT]`

## 183.5 — `BPoint` / `BSize` / `BInsets` `[CERT]`

| Tipo | Grammar | Unidad | Default | Cita |
|---|---|---|---|---|
| `BPoint` | `"x,y"` | doubles crudos; `"null"` para NULL | `0,0` | `BPoint.java:133-152` |
| `BSize` | `"width,height"` | idem | `0,0` | `BSize.java:134-153` |
| `BInsets` | CSS-shorthand: 1 val→todos; 2→`"top right"`; else `"top right bottom left"` (space-separated, SIN comas) | doubles | `0 0 0 0` | `BInsets.java:21-30,176-192` |

`BPoint "x,y"` valida `position="100,100"` del `PopupBinding` (B180/B184) y `BSize "w,h"` valida `size="200,220"`. `[INFER]`

## 183.6 — Lado parse (`Parser.java`) — alcanzable, gap G12 `[CERT]`

`BColor`/`BFont`/`BBrush`/`BInsets` delegan el decode a `com.tridium.gx.parser.Parser`
(`parseColor`/`parseFont`/`parseBrush`/`parseInsets`; ej. `BColor.java:128-135`). Esa clase NO está en la
extracción docSource, pero SÍ en el árbol vineflower:
`organized/gx/gx-rt/vineflower/com/tridium/gx/parser/Parser.java` `[CERT]` (verificado 2026-07-06). Se registra
como gap **G12** (refinamiento: reconocimiento token del lado parse). Para hand-authoring NO es necesario — el
lado encode (§183.2-183.5) ya define los strings válidos, porque el encoder solo produce lo que el parser acepta. `[INFER]`

## 183.x — Connections

- **[Block 181]** — gramática: los valores de atributo que §181.3 emite vía `encodeToString` se detallan acá.
- **[Block 182]** — layout: `BPoint`/`BSize` (§183.5) son los tipos de `viewSize`/`position`/`size`.
- **[Block 22]** — formato PX.
- **B184+** (`PopupBinding`, síntesis `menu.px`) — usan estos grammars (`position="100,100"`, `size`, colores, fuentes).
- **G12** (nuevo) — `com.tridium.gx.parser.Parser`: reconocimiento del lado parse (baja prioridad; fuente confirmada).
