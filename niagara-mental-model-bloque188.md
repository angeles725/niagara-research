# Bloque 188 — BPxInclude: embeber una .px en otra (el menú como componente reutilizable)

> Research del focus **`px-menu`** (gap G11): `BPxInclude` — el widget que embebe una `.px` dentro de otra.
> Relevante para el menú: permite incluir `menu.px` como un COMPONENTE reutilizable in-place (en vez de
> duplicar los ítems), y al ser un `BWidget` su propia `visible` se puede togglear (patrón in-place, B186).
> Cierra parcialmente por remisión a [Block 22] (que lo menciona a alto nivel). NO cubre `BNPxInclude`
> (variante Tridium colorize).
>
> Sources (preservado §5): `sources/decompiled/bajaui-wb-px/BPxInclude.java` (source original Tridium,
> `bajaui-wb`, `javax.baja.ui.px`). Method: lectura READ-ONLY + remisión. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (composición). Connects [Block 22] (PxInclude alto nivel), [Block 186] (visible togglable), [Block 187] (ord).

---

## 188.1 — `BPxInclude extends BWidget`; props `ord` + `variables` `[CERT]`

`BPxInclude` es un `BWidget` (`BPxInclude.java:55-56`), NO un binding — es un widget que se arrastra al canvas
y carga otra `.px`. Propiedades: `[CERT]`

| Propiedad | Tipo | Default | Rol | Cita |
|---|---|---|---|---|
| `ord` | `BOrd` | `BOrd.NULL` | la `.px` a incluir (p.ej. `file:^px/menu.px`) | `BPxInclude.java:42-45,71` |
| `variables` | (PxProperty[]) | — | parametrizan la `.px` incluida | `BPxInclude.java:50-51` |

Al ser `BWidget`, hereda `layout` y `visible` (B182 §182.1) — por eso su visibilidad es togglable como cualquier widget. `[CERT]`

## 188.2 — Carga asíncrona: `load()` / `isLoaded()` / `sync()` `[CERT]`

La `.px` incluida se carga en un thread de fondo. `[CERT]`
- `isLoaded()` (`BPxInclude.java:206-209`): true si ya está en memoria; false si aún carga async.
- `computePreferredSize()` / `doLayout()` (`BPxInclude.java:238-261`): disparan `load()` on-demand si no está
  cargada; una vez cargada, ponen el `root` a los bounds del include (`root.setBounds(0,0,getWidth(),getHeight())`).
- `getRoot()` (`BPxInclude.java:147-149`): el widget raíz cargado de la `.px` (null si no cargó o falló).
- Hooks `preLoad`/`loaded` (`BPxInclude.java:217-226`) para subclases.

`reload()` fuerza recarga (`BPxInclude.java:131-137`), y `changed()` la dispara al cambiar `ord` o `variables`:
`BPxInclude.java:232-236` — `if (prop == ord || prop == variables) reload();`. `[CERT]`

## 188.3 — `baseOrd`: resolución de la ord incluida `[CERT]`

`getBaseOrd()`/`setBaseOrd()` (`BPxInclude.java:159-167`) fijan la ord base contra la cual se resuelve la `ord`
de la `.px` incluida — coherente con la resolución relativa de ords (B187 §187.4). `[CERT]`

## 188.4 — Implicación para el menú `[INFER]`

`BPxInclude` habilita el menú como **componente reutilizable**: en vez de copiar los `<Label hyperlink=...>` en
cada gráfico, se ponen una vez en `menu.px` y cada gráfico embebe `<PxInclude ord="file:^px/menu.px"/>`.
Como `BPxInclude` ES un `BWidget`, para el patrón in-place (B186) se puede atar SU propia `visible` al punto
`menuOpen` con un `ValueBinding`+`IBooleanToSimple` — así el menú entero (incluido) aparece/desaparece sin
duplicar nada. `variables` permitiría, además, parametrizar el mismo `menu.px` por gráfico (p.ej. distinto
set de ítems). Es la vía más DRY para el menú anclado. `[INFER]`

## 188.x — Connections

- **[Block 22]** — menciona `BPxInclude async load` a alto nivel; este bloque da la mecánica `file:line`.
- **[Block 186]** — `visible` togglable: `BPxInclude` es `BWidget`, así que el in-place aplica a él directamente.
- **[Block 187]** — su `ord`/`baseOrd` usa los schemes/resolución documentados.
- **[Block 182]** — hereda `layout`/`visible` de `BWidget`.
- **B-síntesis** (G4) — el `menu.px` puede ofrecerse como include reutilizable.
