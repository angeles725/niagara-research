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
| `file:` | archivo (la `.px` del menú) | `file:^px/menu.px` (`^` = **station home**, ver §187.4) | `PopupBinding.ord` (B185) |
| `history:` | history space | `history:|view:history:HistoryChartBuilder` | `.px` real |
| `view:` | QUÉ vista abrir en el target | sufijo `|view:module:ViewName` | `.px` reales |
| `module:` | recurso de un módulo (`module://mod/...`) | íconos/docs | `.px`: `module://ascCommon/doc/...` |

El sufijo `|view:...` es clave para el menú: `slot:/Foo|view:px:View` navega a `Foo` Y abre su vista Px. `[CERT]` `[INFER]`

## 187.4 — Relativo vs absoluto: portabilidad `[CERT]`

- **Absoluto**: `slot:/Building/AHU1` (desde el root de la station) — se rompe si el `.px` se reusa en otra
  station con otra estructura.
- **Relativo**: `slot:..` (sube al parent), `slot:points` (baja).
  Resuelve contra la base vía `BOrd.make(base, rel).normalize()` (`BOrd.java:108,564`) — exactamente lo que
  hace `PopupBinding.popup()` con `BOrd.make(shell.getActiveOrd(), getOrd()).normalize()` (B185 §185.4). `[CERT]`

> **CORRECCIÓN (2026-07-26)** — una versión previa de este bloque afirmaba en §187.3 y acá que en `file:^...`
> el `^` era "relativo al dir actual" / "el dir del `.px` actual". **Es FALSO.** `^` es un ROOT ESPECIAL del
> file system que apunta al **station home**, registrado explícitamente al construir el `BFileSystem`: `[CERT]`
>
> ```java
> File stationHomeFile = Sys.getStationHome();
> BLocalFileStore stationHomeStore = new BLocalFileStore(this, new FilePath("^"), stationHomeFile);
> this.stationHome = new BDirectory(stationHomeStore, LexiconText.make("baja", "nav.stationHome"));
> specials.put("^", stationHome);
> ```
> `docSource/docSource-doc/extracted/baja/javax/baja/file/BFileSystem.java:144-151` (fuente ORIGINAL de
> Tridium con javadoc, no decompilada — prioridad (a) del protocolo).
>
> El mismo constructor registra `~` = user home (`baseOrdStationToUserHome = "local:|file:~" + homeDiff`,
> `:157`). Es decir: `^` y `~` son ANCLAS ABSOLUTAS, no relativas: `file:^px/menu.px` resuelve al mismo
> archivo sin importar dónde esté el `.px` que lo referencia.
>
> **A qué directorio apunta `^` en la práctica** `[CERT-live]` — verificado contra la station PRUEBAS viva
> (N4.14 OptimizerSupervisor, 2026-07-26): la raíz del file space NO es la carpeta de la station sino su
> subcarpeta **`shared/`**. Evidencia convergente:
> - El árbol `Files` de Workbench muestra exactamente el contenido de
>   `<station>/shared/` (`domo`, `Imagenes`, `images`, `irmRepository`, `px`, `reflow`, `sdash`) y NO muestra
>   `config.bog`, `console.txt`, `alarm/`, `history/`, que sí están en `<station>/`.
> - Los `.px` reales del proyecto se referencian con `file:^px/Header.px` (`Floorplan.px:17`) y
>   `file:^images/Floorplan3D.png` (`:19`), y esos archivos viven en `<station>/shared/px/` y
>   `<station>/shared/images/`.
>
> Es decir: `file:^px/menu.px` → `<station>/shared/px/menu.px`. Un `.px` puesto en `<station>/px/` NO es
> visible desde Workbench ni resoluble por la ord. (Se perdió una vuelta por asumir la carpeta equivocada.)
>
> Consecuencia práctica: `PopupBinding ord="file:^px/menu.px"` NO depende de la ubicación del gráfico que lo
> hospeda — la nota de §187.5 que lo llamaba "relativo al gráfico" queda igualmente corregida. Lo que sí es
> relativo es la normalización `BOrd.make(base, rel)` cuando la ord NO empieza con un ancla. `[CERT]`
>
> Verificado empíricamente en la station PRUEBAS: los `.px` colocados en `<station-home>/px/` son los que
> resuelve `file:^px/...`. Ver `docs/CONNECTING.md`.

**Regla de portabilidad** (confirmada desde la doc en B180 §180.7): preferir ords RELATIVOS en los
`hyperlink` del menú para que el `.px` sea reutilizable entre stations. `[CERT]` (remisión Block 180)

## 187.5 — Implicación para el `menu.px` `[INFER]`

Cada ítem del menú (`<Label>` con `ValueBinding hyperlink="..."`) debe apuntar con la ord al destino + `|view:`.
Para un menú reutilizable: ords relativas (`slot:..|view:...`) donde la estructura lo permita; absolutas
(`station:|slot:/Services/AlarmService|view:alarm:AlarmDbView`) para targets fijos de la station (servicios,
que están siempre en la misma ruta). El `PopupBinding` usa `file:^px/menu.px`, que NO es relativo al gráfico
sino anclado al station home — ver la CORRECCIÓN en §187.4. `[CERT]`

## 187.x — Connections

- **[Block 35]** — registry de los 15 ord schemes (fuente de §187.2, cerrado por remisión).
- **[Block 185]** — `PopupBinding` normaliza su ord relativa a la vista activa (§187.4).
- **[Block 186]** — `hyperlink` de `BValueBinding` = donde van estas ords en los ítems del menú.
- **[Block 180]** — regla de portabilidad (ords relativos) desde la doc oficial.
- **B-síntesis** (G4) — el `menu.px` usará estos ords en cada ítem.
