# Bloque 195 — Theming/CSS: cómo se estilan los widgets (Palladium Swing vs .ux-* web)

> Research del focus **`px-editor`** (gap E5): cómo se ESTILAN los widgets de un `.px` — los DOS sistemas de
> theming según el media (B194): Java theme classes (Palladium) para Swing/Workbench, y CSS (`.ux-*`) para
> bajaux web. Aclara qué es tema vs qué es override de propiedad en el `.px`. NO cubre animación (E6).
>
> Sources (preservados §5): `sources/decompiled/px-theme/` — `bajaui.css` (bajaux, 696 l.), `theme.css`
> (JavaFX, 33 l.), `BStationTheme.java`. Method: lectura READ-ONLY de recursos + decompilado.
> Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (theming). Connects [Block 194] (media), [Block 192] (widgets estilados), [Block 22] (172 theme classes + Palladium).

---

## 195.1 — Dos sistemas de styling según el media `[CERT]`

El estilo NO vive en el `.px`; lo pone el TEMA, y el tema depende del media (B194): `[INFER]`
- **Swing/Workbench**: clases de tema **Java** (`com.tridium.ui.theme.*`) — **146 clases `*Theme`** en el
  paquete decompilado (`[CERT]`; [Block 22] cita 172 contando bytecode), que PINTAN cada widget. Más un
  `theme.css` chico para las partes JavaFX embebidas.
- **Web/bajaux**: **CSS** (`bajaui.css`) con clases `.ux-*` por widget. `[CERT]`

## 195.2 — Web (bajaux): `bajaui.css` con `.ux-<Widget>` + `-t-<Widget>-<part>` `[CERT]`

`bajaui.css` (696 l., 145 reglas `.ux-*`) estila **28 widgets** con la convención:
- `.ux-<Widget>` = la clase del componente (ej. `.ux-Button` `bajaui.css:57`, `.ux-Slider` `:5`, `.ux-BorderPane` `:29`). `[CERT]`
- `-t-<Widget>-<part>` = una PARTE temática interna (ej. `.-t-BorderPane-label`, `.-t-BorderPane-content`, `bajaui.css:33,48`). `[CERT]`

Los widgets estilados coinciden con el catálogo de [Block 192]: `.ux-Label`, `.ux-Button`, `.ux-ToggleButton`,
`.ux-RadioButton`, `.ux-Slider`, `.ux-TabbedPane`, `.ux-SplitPane`, `.ux-ScrollPane`, `.ux-HyperlinkLabel`,
`.ux-PxInclude`, `.ux-ValueBinding`, etc. `[CERT]` (grep sobre `bajaui.css`)

## 195.3 — Workbench (Swing): Java theme classes (Palladium default) + `theme.css` JavaFX `[CERT]`

- **Java theme**: cada widget tiene una interfaz/clase de tema (`ButtonTheme extends AbstractButtonTheme`;
  `CheckBoxTheme`, `GridPaneTheme`, `DropDownTheme`, `BorderPaneTheme`…). El tema ACTIVO lo gobierna
  `BStationTheme` (`BStationTheme.java:16`, implements `BIStationTheme`): por defecto **Palladium**
  (`BStationTheme.java:30` — *"Keeping default palladium theme for station theme"*), y un tema custom se
  instala por `system.property` (`:35`). `[CERT]`
- **`theme.css`** (33 l., JavaFX): estila las partes JavaFX embebidas del shell (menu bar, profile, quick
  search) con propiedades `-fx-*`. Notable: `.button:hover, .menu-button:hover { -fx-cursor: hand }`
  (`theme.css:1-8`) — el cursor-manita del hover (el mismo efecto que B185/B186 hacían por código en los bindings). `[CERT]`

## 195.4 — El `.px` no lleva estilo; lleva OVERRIDES de propiedad `[CERT]`

Consecuencia de B183/B181: un `.px` NO define CSS ni tema; solo setea propiedades de widget como atributos
(`foreground="#1b2733"`, `font="bold 12.0pt Arial"`, `background`…) que **overridean** el default del tema
por-widget. `[INFER]` El tema pinta el "base look"; el `.px` ajusta puntualmente. Por eso el `menu.px`
(B189) se ve consistente sin declarar tema: hereda Palladium (Workbench) y solo sobreescribe colores/fuentes
donde hace falta. `[CERT]` (remisión Block 183)

## 195.x — Connections

- **[Block 194]** — media: define QUÉ sistema de theming aplica (Swing→Java theme, web→CSS).
- **[Block 192]** — widgets: los `.ux-<Widget>` de `bajaui.css` mapean 1:1 al catálogo.
- **[Block 183]** — valores: `foreground`/`background`/`font` del `.px` son overrides sobre el tema (§195.4).
- **[Block 22]** — 172 theme classes + Palladium default: §195.3 confirma el mecanismo `file:line`.
- **E6** (próximo) — animación: cómo una propiedad temática/bound se anima en el tiempo.
