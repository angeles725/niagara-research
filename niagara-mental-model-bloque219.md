# Block 219 — Reflow dashboard-builder (IV): bibliotecas de assets embebidas y el mecanismo ORD→URL de imágenes

> **Qué documenta.** Las bibliotecas de assets que Reflow embarca DENTRO del módulo (arte de equipos HVAC,
> iconos, sonidos, matriz de puntos) y —lo central— **cómo un widget convierte una referencia ORD en una imagen
> visible**. Gap BG7 del focus `nmodsreflow-builder`. Responde "cómo se agregan diseños/iconos dentro del módulo".
>
> **Alcance.** Los assets embebidos y su vía de servido/referencia. El upload de fotos PROPIAS del usuario es
> BG8 (B220). El catálogo de widgets que las consume es B218.
>
> **Fuentes (primarias).**
> - Java RT: `com/niagaramods/nmodsreflow/http/responses/{ImageLibraryResponse,ImageListResponse,FileResponse}.java`
>   + `http/BaseServlet.java` (paths reales) + árbol de assets embebidos (`image-library/`, `sound-library/`,
>   `rc/*.json`).
> - SPA beautificada (1:1 con `app.4509efb4.js` sha256 `81b82b83…`, 1.7.7.75): `scratchpad/reflow-app.beauty.js`,
>   citada `BF:`.
> - Barrido delegado (sonnet); tokens load-bearing re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` bundle 1:1 · Java `file:line` · JSON de asset parseado).
> `[INFER]` = deducción.

---

## 219.1 — `image-library`: arte de equipos embebido (25 imágenes, 8 categorías) `[CERT]`

Reflow trae una biblioteca de arte de equipos HVAC dentro del JAR:
`nmodsreflow77-rt/vineflower/image-library/{RTUs,AHUs,FCUs,VAVs,Misc,Cooling-Towers,Chillers,Boilers}/*.jpg`
(25 archivos, 8 categorías). Se referencia por ORD `module://nmodsreflow/image-library/<cat>/<file>.jpg`.

**Cómo se navega la biblioteca** `[CERT]`: hay DOS superficies, y sólo una está viva:
- La clase Java `ImageLibraryResponse.java` (`GET /station/image-library`, ruteada en `BaseServlet.java:190-198`)
  resuelve `BOrd.make("module://nmodsreflow/image-library")` (`:31`) y camina recursivamente los `BINavNode`
  hijos, emitiendo `{ord, name, type:"directory"|"file", …}` (`:45,48,53,56`).
- **Pero el SPA NO llama ese endpoint** `[CERT]` (grep de `station/image-library` en el bundle = 0 hits). El picker
  real (`ImageBrowser`/`OrdTree`) usa el **nav-RPC nativo de Niagara** (`getNavChildren`) sobre un nodo raíz
  registrado en `BF:38851` (`ord:"module://nmodsreflow/image-library"`, label "Reflow Equipment Library"). Es
  navegación de árbol ORD por el canal nav estándar, no un fetch JSON propio.

Conclusión `[INFER]`: `ImageLibraryResponse`/`ImageListResponse` son una **superficie REST vestigial** (presente en
el servlet, no ejercida por el bundle Vue actual — posible legacy). El picker usa el nav-RPC de Niagara. Además hay
un mapa por defecto equipo→imagen de la biblioteca (`BF:5033`, `{ahu:{ord:"module://…/AHUs/AHU-1.jpg"}, boiler:…}`)
para sembrar thumbnails por tipo de equipo.

## 219.2 — Catálogo de iconos: FontAwesome (1853 iconos) `[CERT]`

El picker de iconos (para `hyperlink.iconName`, etc.) consume dos JSON estáticos del módulo:
`rc/icon-search.json` (**1853** nombres de icono FontAwesome, dict plano) y `rc/icon-categories.json` (**75**
categorías) — ambos verificados por parseo directo. El componente picker (`BF:61772`) hace
`get("/nmodsreflow/icon-categories.json")` + `get("/nmodsreflow/icon-search.json")`; `selectIcon` fija `iconName`;
`okay()` emite `{name, style}` donde `style` es la familia FontAwesome `"far"`/`"fas"` (`BF:61804`). Estos JSON los
sirve la rama fallback genérica del servlet (`FileResponse`, §219.4) — no necesitan clase dedicada.

## 219.3 — `point-matrix.json`: auto-binding de puntos (109 puntos + regex) `[CERT]`

`rc/point-matrix.json` = **109** nombres canónicos de punto HVAC, cada uno con tags de prioridad por tipo de equipo
(`critical/important/secondary/available/n-a`) y un campo `regex` de matcheo de nombre. Se fetchea en `BF:4919`
(`get("/nmodsreflow/point-matrix.json")`) y `pointMapData` (`BF:4939`) mapea los matches a
`{id, displayName, identifier:<regex>}`. Es la tabla que alimenta el **auto-mapeo de puntos** (`equipmentRemap`):
cuando el usuario agrega un equipo, Reflow reconoce sus puntos por regex y los liga a los slots canónicos. No es un
catálogo de iconos (corrige un supuesto previo).

## 219.4 — EL mecanismo ORD→URL: cómo una referencia se vuelve `<img>` `[CERT]`

Es la pieza central de "cómo aparecen las imágenes". El helper `$ord.image(t)` (`BF:3766-3773`) traduce una
referencia ORD a una URL HTTP servible:

```
module://nmodsreflow/image-library/AHUs/AHU-1.jpg   →  /module/nmodsreflow/image-library/AHUs/AHU-1.jpg
file:^Imagenes/x.jpg                                →  /ord/file:^Imagenes/x.jpg
```

- `module://…` → `/module/…` — lo sirve el **servlet de recursos de módulo NATIVO de Niagara** (`/module/...`), NO
  el `BaseServlet` custom de Reflow `[CERT]` (`BF:3770`).
- `file:^…` → `/ord/file:^…` — lo sirve el **resolver de ORD NATIVO de Niagara** (`/ord/...`), tampoco el servlet
  de Reflow `[CERT]` (`BF:3771`). (`^` = station home.)
- `$ord.sound(t)` reusa el MISMO resolver (`BF:3774`).

**Consumidores** (todo lo que muestra una imagen pasa por este helper) `[CERT]`: fondos de tarjeta
(`backgroundImage:"url('"+$ord.image(card.config.backgroundImage.ord)+"')"`, `BF:21233,22971,25498`), hero de
building/página (`BF:15350`), fondo del theme (`this.theme.background`, `BF:55568`), thumbnails y elemento de
equipo (`BF:54950,46074`). **Implicación de producto clave**: Reflow NO reimplementa el servido de imágenes —
delega a los servlets nativos de Niagara (`/module/` para embebidas, `/ord/` para las del file space); su único
trabajo es construir la URL correcta desde el ORD. Esto es directamente relevante para portar a chihuahua (BG11):
la referencia de imágenes es "gratis" vía la plataforma Niagara.

## 219.5 — `sound-library`: sonidos de alarma embebidos (11 MP3) `[CERT]`

`sound-library/*.mp3` = 11 archivos (Electronic Beep, High Low, Ding, Warning, Error…). Se navegan igual que la
image-library por nav-RPC (`BF:15138`, `getNavChildren("module://nmodsreflow/sound-library")`) y se referencian
por el mismo `$ord.sound()`→`$ord.image()` resolver (§219.4). Sirven para alarmas audibles.

## 219.6 — Conexiones

- **[Block 218]** §218.3 — `hyperlink.backgroundImage`/`iconName` y `circle`/`historyChart` son los widgets que
  consumen estos assets; §219.4 explica CÓMO la referencia se vuelve pixel.
- **[Block 216]** §216.1 — `com.tridium.json` es el JSON que arma `ImageLibraryResponse`/`ImageListResponse`
  (aunque el bundle no los use); §219.1 confirma que son vestigiales.
- **Hacia BG8 (B220)**: `file:^Imagenes/` es la referencia a fotos PROPIAS del usuario — B220 responde cómo LLEGAN
  esas fotos al file space (spoiler: no hay upload in-app; out-of-band vía Workbench).
- **Hacia BG11 (chihuahua)**: el patrón "referenciar por ORD + servir con servlets nativos Niagara" es replicable;
  chihuahua ya sirve assets propios (B172/B173) — comparar.
