# Bloque 186 — BValueBinding: el motor del patrón in-place (data→propiedad) + hyperlink

> Research del focus **`px-menu`** (gap G3): `BValueBinding` — el binding más versátil, y el motor del
> **patrón B (in-place)**: ata el `out` de un punto a una propiedad de widget (p.ej. `visible`) vía un slot
> converter dinámico, y da la navegación por `hyperlink`. Junto con B184 (converters) cierra la mecánica del
> menú anclado. Documenta `getOnWidget` + el manejo de mouse + `degradeBehavior` (heredado de `BBinding`).
> NO cubre el binding de tabla ni los ord schemes (G10).
>
> Sources (preservados §5): `sources/decompiled/bajaui-wb-px/BValueBinding.java` + `BBinding.java` (source
> original Tridium, `bajaui-wb`, `javax.baja.ui`). Mayormente leído esta sesión + re-lectura puntual.
> Method: lectura READ-ONLY del decompilado. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (bindings). Connects [Block 184] (converters que invoca), [Block 181] (slot dinámico), [Block 185] (PopupBinding, patrón A).

---

## 186.1 — Identidad + propiedades `[CERT]`

`BValueBinding extends BBinding`, `@AgentOn` sobre `bajaui:Widget` Y `baja:Value` (`BValueBinding.java:49-58`).
Propiedades propias:

| Propiedad | Tipo | Default | Rol | Cita |
|---|---|---|---|---|
| `hyperlink` | `BOrd` | `BOrd.NULL` | clic en el widget → navega a esa ord | `BValueBinding.java:63-67,109` |
| `summary` | `BFormat` | `"%displayName?...% = %.%"` | texto de status en hover | `BValueBinding.java:72-76` |
| `popupEnabled` | boolean | `true` | click-derecho → menú de acciones del componente | `BValueBinding.java:82-86,186` |

Hereda de `BBinding`: `ord` (`BOrd.NULL`, `BBinding.java:72`) y **`degradeBehavior`** (`BDegradeBehavior.none`,
`BBinding.java:44-46,100`) — el mismo `degradeBehavior="hide"` visto en los `.px` reales (B182). `[CERT]`

## 186.2 — `getOnWidget`: el slot converter dinámico (motor del `visible`) `[CERT]`

El corazón del patrón in-place. `getOnWidget(Property prop)` (`BValueBinding.java:227-266`):
```java
BValue override = get(prop.getName());          // busca un slot hijo con el nombre de la propiedad
if (override instanceof BConverter) {
   BConverter converter = (BConverter)override;
   BObject from = get();                          // el valor bound (out del punto)
   BObject to   = prop.getDefaultValue().newCopy();
   ...
   to = converter.convert(from, to, getConverterContext());   // convierte a la propiedad
   return (BValue)to;
}
return null;
```
Es decir: el binding busca un slot hijo cuyo NOMBRE coincida con una propiedad del widget; si ese slot es un
`BConverter`, convierte el valor bound (`from = get()` = `out` del punto) al tipo de la propiedad y lo escribe.
`[CERT]` Esto es EXACTAMENTE lo que hace `<ValueBinding ord="slot:menuOpen"><IBooleanToSimple name="visible"/></ValueBinding>`:
el slot `visible` (un `BIBooleanToSimple`, B184) convierte el boolean del punto a la propiedad `visible`. `[CERT]`

El contexto pasado al converter se puede augmentar con facets (`getConverterContext`, `BValueBinding.java:270-277`;
la doc cita a `kitPx:MouseOverBinding` como ejemplo de override). `[CERT]`

## 186.3 — Manejo de mouse: hyperlink (izq) vs menú de acciones (der) `[CERT]`

`firedOnWidget` despacha los `BMouseEvent` a `handleMouseEvent` (`BValueBinding.java:291-297`), que enruta
ENTERED/EXITED/PRESSED/RELEASED (`BValueBinding.java:321-331`). `[CERT]`

- **`entered`** (`BValueBinding.java:333-349`): cursor `MouseCursor.hand` SOLO si `hyperlink` no es null; agenda
  un update de status periódico (1 s). `[CERT]`
- **`released`** (`BValueBinding.java:376-396`): dos ramas — `[CERT]`
  1. Si es popup-trigger (click-derecho) → arma el menú de acciones (§186.4).
  2. Si no, y `isOver && !getHyperlink().isNull()` → `((BIHyperlinkShell)shell).hyperlink(new HyperlinkInfo(ord, event))`:
     **navega** a la ord del `hyperlink`. Este es el mecanismo que usan los ítems del `menu.px` (B180 §180.6).

## 186.4 — `popupEnabled`: menú de acciones por click-derecho (vía reflection) `[CERT]`

`getMenu()` (`BValueBinding.java:398-420`): si `popupEnabled` y está bound, usa reflection para invocar
`javax.baja.workbench.nav.menu.NavMenuUtil.makeActionsMenu(widget, component)` y devolver un `BMenu` con las
acciones del componente. `[CERT]`

Nota `[INFER]`: este `BMenu` (click-derecho, contextual) es un menú Swing del Workbench (B179 §179.2), NO un
dropdown de canvas — refuerza que el menú anclado del focus se emula con `visible` (§186.2), no con este.

## 186.5 — Cómo se combinan para el patrón in-place `[INFER]`

Bloque de síntesis. El patrón B (menú anclado) usa `BValueBinding` en DOS roles distintos:
1. **En el panel del menú**: `ValueBinding ord="slot:menuOpen"` + slot converter `<IBooleanToSimple name="visible"/>`
   → §186.2 ata la visibilidad del panel al punto `menuOpen` (B184 confirma que produce el boolean correcto).
2. **En cada ítem del menú**: `ValueBinding hyperlink="<ord destino>"` → §186.3 navega al clic.

Falta solo el trigger que togglea `menuOpen` (fricción documentada en B181/memoria: `BooleanWritable` sin
action toggle nativa). El `menu.px` de síntesis (G4) integra todo. `[INFER]`

## 186.x — Connections

- **[Block 184]** — converters: `getOnWidget` (§186.2) invoca `BConverter.convert(from,to,ctx)`; `IBooleanToSimple` es el slot.
- **[Block 181]** — gramática: el slot converter es el "sub-elemento con name que no matchea prop frozen" (§181.4).
- **[Block 185]** — `PopupBinding` (patrón A): `BValueBinding` es el patrón B, con anclaje pero requiere punto en station.
- **[Block 179]** — framing: cierra el "patrón B" que §179.3 anunció.
- **B-síntesis** (G4) — `menu.px` que integra ambos roles de §186.5.
