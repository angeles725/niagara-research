# Bloque 193 — Los 9 bindings kitPx restantes: mecánica de cada uno

> Research del focus **`px-editor`** (gap E3): la mecánica de los 9 bindings `kitPx` que `px-menu` NO detalló
> (PopupBinding [Block 185] y ValueBinding [Block 186] ya están). Cada uno con su trigger, qué hace, props
> clave y sobre qué widget aplica. Completa el catálogo de bindings del PX editor.
>
> Sources (preservados §5): `sources/decompiled/kitPx-wb-bindings/` — `BActionBinding`, `BSetPointBinding`,
> `BSpectrumBinding`, `BMomentaryToggleBinding`, `BBoundLabelBinding` (Vineflower, `com.tridium.kitpx`).
> Barrido delegado (sonnet) 2026-07-06.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (bindings). Connects [Block 36] (catálogo kitPx), [Block 185] (PopupBinding), [Block 186] (ValueBinding).

---

## 193.1 — El split de herencia: `BBinding` directo vs `BValueBinding` `[CERT]`

Los bindings se dividen por base — y eso determina si heredan el motor de slot-converter dinámico de
`BValueBinding` (B186 §186.2): `[CERT]`

- **`BBinding` directo** (SIN converter dinámico): `BActionBinding`, `BButtonGroupBinding`,
  `BMomentaryToggleBinding`, `BSpectrumBinding`.
- **`BValueBinding` subclase** (heredan `getOnWidget`/`convert`): `BBoundLabelBinding`, `BMouseOverBinding`,
  `BSetPointBinding` (+ su subclase `BIncrementSetPointBinding`), `BSpectrumSetpointBinding`.

## 193.2 — Bindings de ACCIÓN (clic → hace algo) `[CERT]`

| Binding | @AgentOn | Trigger | Qué hace | Props clave |
|---|---|---|---|---|
| `BActionBinding` | `bajaui:Widget` | evento cuyo nombre == `widgetEvent` (excluye MOUSE_EXITED, id 505) | resuelve el slot target como `Action` y la invoca (`InvokeActionCommand`), con guard anti-doble-fire | `widgetEvent` (`:29`), `actionArg` (`:32-35`) |
| `BMomentaryToggleBinding` | `bajaui:AbstractButton` | mouse PRESSED(501)/RELEASED(502) (`:37`) | press→`set(true)`, release→`set(false)` según `isPressed()`; debounce 100ms | (usa el estado del botón) |
| `BSetPointBinding` | `bajaui:Widget` | evento == `widgetEvent` (el doble-clic vive en `BSetPointFieldEditor`, no acá) | `saveSetPoint()` convierte y escribe (`saveProperty`/`saveAction`) con `min`/`max` (`verifyBounds`) | `widgetEvent` (`:49`), `widgetProperty` (`:52`) |
| `BIncrementSetPointBinding` | `bajaui:Button` | hereda de `BSetPointBinding` | override `saveWidgetProperty()`: devuelve `current + increment` (+/- buttons) | `increment` (`:20-23`) |

`[CERT]` `BActionBinding.java:29,32-35,78-80`; `BMomentaryToggleBinding.java:37`; `BSetPointBinding.java:49-56`. Detalle:
`BActionBinding` invoca la Action del target (p.ej. `override`/`auto` de un writable), con `runningAction`/
`actionMonitor` para evitar re-entrada (`BActionBinding.java:140-151`). `[CERT]`

**Paralelo con PopupBinding** `[INFER]`: `BMomentaryToggleBinding` usa el mismo patrón press/release de
`BPopupBinding` (B185 §185.3), pero keyea sobre `isPressed()` del botón, no `isButton1Down`.

## 193.3 — Bindings de DISPLAY (valor → look, sin trigger) `[CERT]`

| Binding | Base | Qué hace | Props clave |
|---|---|---|---|
| `BBoundLabelBinding` | `BValueBinding` (`@AgentOn kitPx:BoundLabel`) | override `getOnWidget`: deriva `blink`/`foreground`/`background`/`border` del `BStatus` del valor bound (unacked-alarm→blink, status colors) | `statusEffect` (`:28-32`) |
| `BSpectrumBinding` | `BBinding` (`@AgentOn bajaui:Widget`) | `getOnWidget` mapea un valor numérico a color vía `solveColor()`: interpola linear `lowColor`→`midColor`→`highColor` alrededor de `setpoint ± extent/2` | `setpoint`/`extent` (`:39-46`), `lowColor`/`midColor`/`highColor` (`:27-37`) |
| `BSpectrumSetpointBinding` | `BValueBinding` | combo trivial: `targetChanged()` empuja el valor numérico bound a cada `BSpectrumBinding` hermano del widget vía `setSetpoint()` | (sin props propias) |

`[CERT]` `BBoundLabelBinding.java:49-101`; `BSpectrumBinding.java:108-174`; `BSpectrumSetpointBinding.java:23-36`.
El `BBoundLabel` es lo que el "Make Widget wizard" ofrece como fuente por defecto (B180 §180.4, B191 §191.6). `[INFER]`

## 193.4 — Bindings de INTERACCIÓN (hover / grupo) `[CERT]`

- `BButtonGroupBinding` (`BBinding` directo): construye botones radio/toggle dinámicamente desde el range de
  un `control:BooleanWritable`/`EnumWritable`; el clic invoca la action `"set"` del target vía `ToggleCommand`
  (`BButtonGroupBinding.java:58-82,106-112`). Prop `style` (radio vs toggle, `:31-35`). `[CERT]`
- `BMouseOverBinding` (`BValueBinding`, `final`): en ENTERED(504)/EXITED(505) togglea un flag `active` y llama
  `targetChanged()`; expone `active` a los converters vía `BFacets` en `getConverterContext()`
  (`BMouseOverBinding.java:90-112`) → permite que las props del widget cambien de look en hover. Sobreescribe
  `ord`/`hyperlink`/`popupEnabled` para suprimir el clic/popup normal (`:27-57`). `[CERT]`

Nota `[INFER]`: `BMouseOverBinding` es el ejemplo canónico del "converter context override" que B186 §186.2
mencionó — el `active` de hover llega al converter como facet.

## 193.x — Connections

- **[Block 36]** — catálogo kitPx: este bloque da la mecánica `file:line` de los bindings que aquel listó.
- **[Block 185]** — `PopupBinding`: `BMomentaryToggleBinding` comparte su patrón press/release (§193.2).
- **[Block 186]** — `BValueBinding`: 4 de los 9 lo heredan (el converter dinámico); `BMouseOverBinding` es el ejemplo del context-override que B186 citó.
- **[Block 191]** — el Make Widget wizard: `BBoundLabel` es su fuente por defecto (§193.3).
- **E4** (próximo) — media/perfiles: cómo estos bindings se adaptan a Wb/Hx/Ux (los `BHxPx*Binding`).
