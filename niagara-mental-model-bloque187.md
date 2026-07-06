# Bloque 187 — Ord schemes en bindings/hyperlinks: cómo se escriben los targets del menú

> Research del focus **`px-menu`** (gap G10): cómo se escriben y resuelven las **ords** que un binding o
> hyperlink usa como target — la sintaxis `scheme:body|scheme:body` que va en `ord="..."`/`hyperlink="..."`.
> Foco en los schemes relevantes al menú (`slot:`, `station:`, `file:`, `history:`, `view:`, `module:`).
> Cierra parcialmente por REMISIÓN a [Block 35] (lista completa de schemes) + detalle propio. NO re-documenta
> el nav tree.
>
> Sources: `niagara-mental-model-bloque35.md:3` (lista de 15 ord schemes) · `sources/decompiled/baja-naming/BOrd.java`
> (make/normalize) · `.px` reales del corpus (evidencia empírica de `hyperlink=`/`ord=`).
> Method: remisión + lectura decompilado + corpus empírico. Markers (§3): `[CERT]` `file:line` · `[INFER]`.
>
> Capa PX (naming). Connects [Block 35] (ord schemes registry), [Block 185] (PopupBinding normaliza la ord), [Block 186] (hyperlink).

---

## 187.1 — Una ord es una cadena de queries `scheme:body` separadas por `|` `[CERT]`

`BOrd` (`javax.baja.naming`) modela la ord como una cadena de queries. `make(String)` la parsea
(`BOrd.java:97`), `make(BOrd base, BOrd child)` / `make(BOrd base, String)` la componen relativa a una base
(`BOrd.java:108,124`), y `normalize()` la resuelve/canoniza (`BOrd.java:564`). `[CERT]`

Evidencia empírica en `.px` reales: `hyperlink="history:|view:history:HistoryChartBuilder"`,
`hyperlink="slot:points|view:wiresheet:WireSheet"`, `ord="history:^usedHeapMemory"`,
`ord="local:|module://ascCommon/doc/...html"`. Cada `|` encadena un query que refina el anterior. `[CERT]`

## 187.2 — Los 15 schemes (remisión a Block 35) `[CERT]`

La lista COMPLETA está documentada en `niagara-mental-model-bloque35.md:3`
(remisión — no hay substancia nueva que agregar): `workbench:`, `tool:`, `widget:`, `nav:`, `slot:`,
`station:`, `module:`, `local:`, `file:`, `bog:`, `zip:`, `virtual:`, `spy:`, `http(s):`. `[CERT]` (Block 35)

## 187.3 — Los schemes relevantes al menú `[CERT]`

| Scheme | Qué apunta | Forma en el menú | Evidencia |
|---|---|---|---|
| `slot:` | componente en el BOG (BObject graph) de la station | `slot:/Path/Abs` (absoluto desde root), `slot:rel`, `slot:..` (sube) | `.px`: `slot:points`, `slot:..|view:lonworks:LonDeviceManager` |
| `station:` | raíz de la station | `station:|slot:/...` | `.px` reales |
| `file:` | archivo (la `.px` del menú) | `file:^px/menu.px` (`^` = relativo al dir actual) | `PopupBinding.ord` (B185) |
| `history:` | history space | `history:|view:history:HistoryChartBuilder` | `.px` real |
| `view:` | QUÉ vista abrir en el target | sufijo `|view:module:ViewName` | `.px` reales |
| `module:` | recurso de un módulo (`module://mod/...`) | íconos/docs | `.px`: `module://ascCommon/doc/...` |

El sufijo `|view:...` es clave para el menú: `slot:/Foo|view:px:View` navega a `Foo` Y abre su vista Px. `[CERT]` `[INFER]`

## 187.4 — Relativo vs absoluto: portabilidad `[CERT]`

- **Absoluto**: `slot:/Building/AHU1` (desde el root de la station) — se rompe si el `.px` se reusa en otra
  station con otra estructura.
- **Relativo**: `slot:..` (sube al parent), `slot:points` (baja), `file:^menu.px` (`^` = dir del `.px` actual).
  Resuelve contra la base vía `BOrd.make(base, rel).normalize()` (`BOrd.java:108,564`) — exactamente lo que
  hace `PopupBinding.popup()` con `BOrd.make(shell.getActiveOrd(), getOrd()).normalize()` (B185 §185.4). `[CERT]`

**Regla de portabilidad** (confirmada desde la doc en B180 §180.7): preferir ords RELATIVOS en los
`hyperlink` del menú para que el `.px` sea reutilizable entre stations. `[CERT]` (remisión Block 180)

## 187.5 — Implicación para el `menu.px` `[INFER]`

Cada ítem del menú (`<Label>` con `ValueBinding hyperlink="..."`) debe apuntar con la ord al destino + `|view:`.
Para un menú reutilizable: ords relativas (`slot:..|view:...`) donde la estructura lo permita; absolutas
(`station:|slot:/Services/AlarmService|view:alarm:AlarmDbView`) para targets fijos de la station (servicios,
que están siempre en la misma ruta). El `PopupBinding` en cambio usa `file:^px/menu.px` (relativo al gráfico). `[INFER]`

## 187.x — Connections

- **[Block 35]** — registry de los 15 ord schemes (fuente de §187.2, cerrado por remisión).
- **[Block 185]** — `PopupBinding` normaliza su ord relativa a la vista activa (§187.4).
- **[Block 186]** — `hyperlink` de `BValueBinding` = donde van estas ords en los ítems del menú.
- **[Block 180]** — regla de portabilidad (ords relativos) desde la doc oficial.
- **B-síntesis** (G4) — el `menu.px` usará estos ords en cada ítem.
