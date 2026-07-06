# Bloque 185 — kitPx:PopupBinding: mecánica del hyperlink-popup (patrón A del menú)

> Research del focus **`px-menu`** (gap G2): la mecánica REAL de `kitPx:BPopupBinding` — el binding que
> emula un "dropdown" abriendo otra `.px` en una ventana al clic. Documenta el trigger exacto, la ventana que
> abre, y sus propiedades. Es el **patrón A** (recomendado para Workbench) del focus. NO cubre `BValueBinding`
> (patrón B, G3) ni la variante Hx (`BHxPxPopupBinding`).
>
> Sources (preservado §5): `sources/decompiled/kitPx-wb/BPopupBinding.java` (sha256 `82e3656b…`) — source
> original Tridium (`kitPx-wb`, `com.tridium.kitpx`). Leído íntegro (single-file, sin delegación).
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (bindings). Connects [Block 179] (patrón A del framing), [Block 180] (Popup Binding desde la doc), [Block 183] (position/size).

---

## 185.1 — Identidad: `extends BBinding`, `@AgentOn bajaui:Widget` `[CERT]`

`BPopupBinding extends BBinding`, declarado `@NiagaraType agent=@AgentOn(types="bajaui:Widget")`
(`BPopupBinding.java:45-49,91-93`) → **se puede pegar a CUALQUIER widget**, no solo botones. `[CERT]`

Hereda `ord` de `BBinding` (`getOrd()`, usado en `BPopupBinding.java:297,307,325`) — esa ord es la `.px` que
el popup abre. `[CERT]`

## 185.2 — Propiedades `[CERT]`

| Propiedad | Tipo | Default | Cita |
|---|---|---|---|
| `title` | String | `"Pop up"` (localizable con `%lexicon(...)%`) | `BPopupBinding.java:54-58,111` |
| `position` | `BPoint` | `BPoint.make(100, 100)` | `BPopupBinding.java:62-66,139` |
| `size` | `BSize` | `BSize.make(800, 600)` | `BPopupBinding.java:70-74,165` |
| `modal` | boolean | `false` | `BPopupBinding.java:78-82,191` |
| `ord` (heredada) | `BOrd` | — | `BBinding` |

Los defaults `position=100,100` y `size=800×600` explican por qué el popup NO aparece anclado al botón:
hay que ajustar `position`/`size` a mano (B180 §180.6 lo documenta desde la doc). `[INFER]`

## 185.3 — Trigger: `MOUSE_RELEASED` con botón izquierdo — es CLIC, no hover `[CERT]`

En `started()`, el binding se conecta a los eventos de mouse del widget:
`BPopupBinding.java:242-250` — `linkTo(getWidget(), BWidget.mouseEvent, mouseEvent)` (recibe eventos "regardless
of whether other bindings have consumed them", Issue 15791). `[CERT]`

`doMouseEvent()` (`BPopupBinding.java:256-267`) despacha por id: `MOUSE_ENTERED`→`entered`,
`MOUSE_EXITED`→`exited`, `MOUSE_RELEASED`→`released`. `[CERT]`

- `entered()` (`BPopupBinding.java:269-281`): SOLO cambia el cursor a `MouseCursor.hand` (si la ord no es
  null y hay mouse) y muestra el status. **No abre nada.** `[CERT]`
- `released()` (`BPopupBinding.java:295-299`): `if (isOver && !getOrd().isNull() && event.isButton1Down())
  popup();`. `[CERT]`

**Conclusión `[CERT]`**: el popup dispara en **MOUSE_RELEASED con botón 1 (izquierdo)** — es un CLIC. El hover
solo cambia el cursor. (Corrige la imprecisión "hover/click" del catálogo en bloque 36.)

## 185.4 — Qué abre: una ventana `BNiagaraWbDialog` (contexto Workbench) `[CERT]`

`popup()` (`BPopupBinding.java:319-334`):
```java
BWbShell shell = BWbShell.getWbShell(getWidget());
BOrd o = BOrd.make(shell.getActiveOrd(), getOrd()).normalize();
String titleFormat = FormatUtil.formatForStringProperty(getTitle(), getTarget());
BNiagaraWbDialog dlg = new BNiagaraWbDialog(BPopupProfile.TYPE, shell, o, titleFormat, getPosition(), getSize(), getModal());
dlg.open();
```
Es decir: `[CERT]`
1. Resuelve el shell Workbench del widget (`BWbShell.getWbShell`) → **contexto puro Workbench** (por eso este
   binding es el correcto para perfil Workbench; Hx usa `BHxPxPopupBinding`).
2. Normaliza la ord del popup RELATIVA a la vista activa (`shell.getActiveOrd()`) — de ahí que un `file:^px/menu.px` resuelva.
3. Abre un `BNiagaraWbDialog` (una VENTANA de diálogo) con `position`/`size`/`modal` — no un panel anclado.

Limitación estructural `[INFER]`: al ser `BNiagaraWbDialog` con `position` absoluta, el menú aparece como
ventana en coordenadas fijas del shell, NO bajo el botón. Es la diferencia dura vs un dropdown SLDS (el patrón
B/in-place, G3, resuelve el anclaje a costa de un punto en la station).

## 185.5 — Errores `[CERT]`

Si la ord no resuelve, loguea `SEVERE` y no crashea: `BPopupBinding.java:330-333` —
`catch(Exception e) { BBinding.LOGGER.log(Level.SEVERE, "Could not resolve pop up binding ORD", e); }`. `[CERT]`

## 185.x — Connections

- **[Block 179]** — framing: este es el "patrón A" (PopupBinding) que §179.3 anunció.
- **[Block 180]** — la doc oficial describe Popup Binding como hyperlink desde el usuario; este bloque da la mecánica `file:line`.
- **[Block 183]** — `position`/`size` usan `BPoint "x,y"`/`BSize "w,h"` (grammar de B183).
- **[Block 36]** — catálogo kitPx: corrige su "hover/click" → es clic (MOUSE_RELEASED button1).
- **B186** (G3) — `BValueBinding`: el patrón B (in-place) que resuelve el anclaje que §185.4 no da.
- **B-síntesis** (G4) — el `menu.px` usará este binding para el patrón A.
