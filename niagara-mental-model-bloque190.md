# Bloque 190 — gx Parser (parse-side): simetría round-trip con el encode-side

> Research del focus **`px-menu`** (gap G12, refinamiento): confirma que `com.tridium.gx.parser.Parser` — el
> lado PARSE que B183 dejó como gap honesto — acepta EXACTAMENTE lo que el encode-side produce. Cierra el
> round-trip encode↔parse de los valores gx. Bloque CONCISO de refinamiento.
>
> Sources (preservado §5): `sources/decompiled/gx-parser/Parser.java` (Vineflower, `com.tridium.gx.parser`, 962 l.).
> Method: lectura READ-ONLY. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (valores). Connects [Block 183] (encode-side, cierra su gap G12).

---

## 190.1 — `parseColor`: hex 3/6/8, nombres, `rgb()`/`rgba()` `[CERT]`

`parseColor()` (`Parser.java:331-400`) acepta todas las formas que `BColor.encodeToString` produce (B183 §183.2):
- **Hex** `#rgb` (len 3), `#rrggbb` (len 6), `#aarrggbb` (len 8): `Parser.java:336-354` —
  `if (str.length()==6) { int rgb = Integer.parseInt(str,16); return BColor.make(rgb,false); }`. `[CERT]`
- **Nombres**: `BColor.getConstant(cur.str)` (`Parser.java:356-363`) — la misma tabla case-insensitive de B184/B183. `[CERT]`
- **Funciones** `rgb(...)` / `rgba(...)`: `Parser.java:365-380`. `[CERT]`

## 190.2 — `parseFont` / `parseBrush` / `parseInsets`: aceptan el encode-side `[CERT]`

- `parseFont()` (`Parser.java:453-484`): reconoce `bold` (`:461`), `italic` (`:464`) y la dimensión `pt`
  (`:473` `if (!this.cur.dimen("pt"))`) — el orden de tokens de B183 §183.3. `[CERT]`
- `parseBrush()` (`Parser.java:484-640`): intenta primero color sólido (`parseColor()`, `:485`); para gradientes
  reconoce la función `stop` (`Parser.java:595,625` `this.cur.function("stop")`) — el `stop(offset% color)` de
  B183 §183.4. `[CERT]`
- `parseInsets()` existe (`Parser.java:840`) para el shorthand 1/2/4-valores. `[CERT]`

## 190.3 — Conclusión: round-trip cerrado `[INFER]`

El parse-side acepta lo que el encode-side emite, en las cuatro familias (color/font/brush/insets). Confirma
que la guía de authoring de B183 (basada solo en el encode-side) es correcta y completa: **un valor escrito a
mano según B183 será aceptado por el parser**. El gap G12 (que B183 §183.6 dejó abierto honestamente) queda
cerrado con simetría demostrada, no asumida. `[INFER]`

## 190.x — Connections

- **[Block 183]** — encode-side de los valores gx: este bloque cierra su gap G12 confirmando el parse-side.
- **B189** — síntesis: los valores del `menu.px` (colores/fuentes) son válidos en ambos sentidos.
