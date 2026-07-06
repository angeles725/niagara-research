# Bloque 189 — Síntesis aplicada: el menu.px completo (patrón A PopupBinding + patrón B in-place)

> Research del focus **`px-menu`** (gap G4 — bloque CULMINANTE / DESIGN-APPLIED): integra los 10 bloques del
> focus (B179-B188) en el `.px` concreto de un "Menu Button / Dropdown" para Workbench, en sus dos patrones,
> con cada decisión respaldada por su bloque de mecánica. Es el DELIVERABLE del focus.
>
> **Tipo de bloque: DESIGN/APPLIED** — un ratio `[INFER]/[CERT]` alto es ESPERADO y sano (§11): las decisiones
> de diseño son inferencia respaldada por los `[CERT]` de los bloques citados, no evidencia nueva. NO cierra
> por exhaustión de evidencia sino por integración completa.
>
> Sources: síntesis de [Block 179-188] + `.px` reales del corpus. Method: integración. Markers (§3):
> `[CERT]` (remisión a bloque con `file:line`) · `[INFER]` (decisión de diseño).
>
> Capa PX (aplicada). Connects [Block 179-188] (toda la mecánica del focus).

---

## 189.1 — Reglas de oro del `.px` a mano (de B181) `[CERT]`

Antes del XML, las reglas duras que evitan que el `XParser` rechace el archivo (B181 §181.5):
1. **El tag de apertura de un widget, con TODOS sus atributos, va en UNA sola línea** — partirlo lanza
   `XException: Expecting '='` (`sources/decompiled/bajaui-wb-px/PxEncoder.java:276-355`). `[CERT]`
2. Elemento = widget; atributo = propiedad simple no-default; sub-elemento = binding/slot hijo (B181 §181.3-4). `[CERT]`
3. `<import>` debe declarar cada módulo usado (`baja`, `gx`, `bajaui`, `converters` si hay converter). `[CERT]`

## 189.2 — Patrón A: `menu.px` autónomo + `PopupBinding` (recomendado para Workbench) `[INFER]`

El `menu.px` (una `.px` independiente que el `PopupBinding` abre en ventana, B185). Layout `GridPane
columnCount=1` (apila vertical sin calcular `y`, B182 §182.6) — CADA tag en 1 línea:

```xml
<px version='1.0'>

<import>
  <module name='baja'/>
  <module name='gx'/>
  <module name='bajaui'/>
</import>

<content>
  <GridPane columnCount="1" rowGap="2" background="#c8ccd4" halign="fill" columnAlign="fill">
    <Label text="Dashboard" font="bold 12.0pt Arial" foreground="#1b2733" background="#f4f6f9">
      <ValueBinding hyperlink="station:|slot:/Path/To/Dashboard|view:px:View"/>
    </Label>
    <Label text="Alarmas" font="bold 12.0pt Arial" foreground="#1b2733" background="#f4f6f9">
      <ValueBinding hyperlink="station:|slot:/Services/AlarmService|view:alarm:AlarmDbView"/>
    </Label>
    <Label text="Historial" font="bold 12.0pt Arial" foreground="#1b2733" background="#f4f6f9">
      <ValueBinding hyperlink="history:|view:history:HistoryChartBuilder"/>
    </Label>
  </GridPane>
</content>

</px>
```

Respaldo de cada decisión: `GridPane columnCount=1` (B182 §182.3/182.6); `Label`+`ValueBinding hyperlink`
para navegar al clic (B186 §186.3); ords con `|view:` (B187 §187.3); `font="bold 12.0pt Arial"` grammar
(B183 §183.3); colores `#hex` (B183 §183.2). `[CERT]` (por remisión)

En el gráfico principal, el botón trigger + el binding (B185): `[INFER]`
```xml
<Button text="Menu">
  <PopupBinding ord="file:^px/menu.px" size="200,220" position="8,40" modal="false"/>
</Button>
```
`ord=file:^px/menu.px` (relativo, B187 §187.4); `size`/`position` = `BSize`/`BPoint` `"w,h"`/`"x,y"` (B183 §183.5);
clic izquierdo dispara (B185 §185.3). Limitación: la ventana NO se ancla al botón (B185 §185.4). `[CERT]`

## 189.3 — Patrón B: menú in-place anclado (toggle `visible`) `[INFER]`

Panel embebido en el gráfico principal cuya `visible` sigue a un `BooleanWritable menuOpen` de la station:

```xml
<GridPane columnCount="1" rowGap="2" visible="false" background="#f4f6f9">
  <ValueBinding ord="station:|slot:/menuOpen" degradeBehavior="hide">
    <IBooleanToSimple name="visible"/>
  </ValueBinding>
  <Label text="Dashboard" font="bold 12.0pt Arial" foreground="#1b2733">
    <ValueBinding hyperlink="station:|slot:/Path/To/Dashboard|view:px:View"/>
  </Label>
</GridPane>
```
(Agregar `<module name='converters'/>` al `<import>`.)

Respaldo: `visible` es prop de `BWidget` togglable (B182 §182.1); `ValueBinding.getOnWidget` busca el slot
`visible` y, siendo `BConverter`, convierte el boolean del punto → la propiedad (B186 §186.2); `IBooleanToSimple`
con defaults `TRUE`/`FALSE` PASA el type-guard porque `visible` es `BBoolean` → produce boolean usable **sin
coerción** (B184 §184.4); `degradeBehavior="hide"` de `BBinding` (B186 §186.1). `[CERT]` (por remisión)

Como el panel es un `BWidget`, también se puede EMBEBER `menu.px` con `<PxInclude ord="file:^px/menu.px"/>` y
togglear la `visible` del `PxInclude` — la vía más DRY (B188 §188.4). `[INFER]`

## 189.4 — La fricción del toggle (sin resolver en PX puro) `[CERT]`

El punto débil real del patrón B: `PX no tiene scripting` (B181/B22), así que el estado vive en la station; y
`BooleanWritable` **no tiene una action `toggle` nativa** — `ActionBinding` invoca una action fija (B186 §186.3
usa `hyperlink`, no toggle). `[CERT]` (por remisión) Opciones: `[INFER]`
- **Abrir-con-botón / cerrar-con-selección**: el botón hace `set=true` en `menuOpen`; cada `Label` del menú,
  además de navegar, hace `set=false`. Simple; el clic-afuera no cierra.
- **Toggle real**: lógica en la station (un `Program` object o un link que invierta `menuOpen`). Más trabajo.

Y el gotcha del converter (B184 §184.5): `init()` reseed puede hacer que un binding recién agregado arranque
no-op hasta configurar `trueValue`/`falseValue` en el property sheet. `[CERT]` (por remisión)

## 189.5 — Veredicto y tabla de integración `[INFER]`

| | Patrón A (PopupBinding) | Patrón B (in-place) |
|---|---|---|
| Anclado al botón | No (ventana, B185) | **Sí** (B186/B182) |
| Requiere punto en station | No | **Sí** (`menuOpen`) |
| Toggle 1-botón | N/A | **Complejo** (B189 §189.4) |
| Reutilizable | `menu.px` autónomo | `PxInclude` (B188) |
| Esfuerzo | **Bajo** | Alto |

**Recomendación**: para Workbench, arrancar con **Patrón A** (bajo esfuerzo, sin tocar la station). El in-place
solo si el anclaje visual es requisito duro. `[INFER]`

Mapa de respaldo: gramática/tag-1-línea [Block 181] · layout GridPane [Block 182] · valores gx [Block 183] ·
converter type-guard [Block 184] · PopupBinding [Block 185] · ValueBinding/hyperlink [Block 186] · ords
[Block 187] · PxInclude [Block 188] · workflow editor [Block 180] · framing [Block 179].

## 189.x — Connections

- **[Block 179-188]** — TODA la mecánica del focus; este bloque es su integración aplicada.
- **[Block 179]** — cierra el mapa de 2 patrones que el framing anunció.
- Deliverable físico: `scratchpad/menu.px` (versión Patrón A, tag-1-línea) generado en la sesión de origen.
