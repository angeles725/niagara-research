# Block 220 — Reflow dashboard-builder (V): cómo el usuario agrega sus PROPIAS fotos (upload)

> **Qué documenta.** Cómo entran al dashboard las imágenes PROPIAS del usuario (fotos de plantas, diagramas):
> por dónde se suben, dónde se guardan, en qué formato, y cómo el picker las ofrece. Gap BG8 del focus
> `nmodsreflow-builder`. Completa la respuesta a "agregar fotos/diseños dentro del módulo" (los assets embebidos
> son B219).
>
> **Alcance.** El path de las fotos propias (no las embebidas de B219). Responde el veredicto: ¿hay upload in-app
> o llegan por fuera?
>
> **Fuentes (primarias).**
> - Java RT: `com/niagaramods/nmodsreflow/http/BaseServlet.java` (tabla de rutas doPost),
>   `http/responses/{ImageListResponse,EquipmentNoteUpdateResponse}.java`.
> - SPA beautificada (1:1 `app.4509efb4.js` sha256 `81b82b83…`): `scratchpad/reflow-app.beauty.js`, citada `BF:`.
> - Evidencia de disco `[CERT]`: `…/HoneywellMX605132026/shared/Imagenes/*.jpg|.jpeg|.png` (fotos reales del
>   usuario) + refs `file:^Imagenes/…` en el `config.json` real (B218).
> - Barrido delegado (sonnet, mismo sweep que B219); tokens re-verificados por el driver.
>
> **Método / markers.** `[CERT]` = fuente primaria (`BF:` bundle 1:1 · Java `file:line` · archivo en disco).
> `[INFER]` = deducción.

---

## 220.1 — Veredicto: Reflow NO tiene upload de imágenes in-app `[CERT]`

No existe endpoint de subida de archivos en Reflow. Evidencia doble (grep negativo):

- **Servidor**: `BaseServlet.java` `doPost` tiene EXACTAMENTE 4 rutas (`:273-285`): `/config_update`,
  `/config_delta`, `/station/equipment-notes-update`, `/station/alarms/query`. **No hay ruta multipart / de
  upload**; no se llama `getParts()` `[CERT]` (grep negativo). Todo lo demás cae en 404.
- **Cliente**: el bundle SPA (123 740 líneas) tiene **0 hits** de `FileReader` / `multipart/form` / `dataTransfer`
  / `new FormData` `[CERT]` (grep negativo). No hay `<input type="file">` de subida (los 2 `type:"file"` del
  bundle son un prop de `OrdTree` para navegar CSVs por ORD, no un widget de upload).

Conclusión: **las fotos propias NO se suben desde la app de Reflow**.

## 220.2 — Cómo llegan realmente las fotos: out-of-band al file space de la station `[CERT]`/`[INFER]`

Las imágenes del usuario se colocan en el **file space de la station** (bajo `^` = station home, típicamente
`^Imagenes/` o `shared/Imagenes/`) por un mecanismo FUERA de Reflow: la **UI de file-space de Workbench** o la
transferencia de archivos de plataforma Niagara `[INFER]` (Reflow no provee la vía; es la de la plataforma).
Evidencia de disco `[CERT]`: la station real `HoneywellMX605132026/shared/Imagenes/` contiene las fotos del
cliente (`*.jpg/.jpeg/.png`), referenciadas en su `config.json` como `file:^Imagenes/<archivo>` (B218). Es decir:
el usuario sube la foto a la carpeta de la station con Workbench, y Reflow la **referencia**, no la recibe.

## 220.3 — El picker de "My Images": navegación del file space por nav-RPC `[CERT]`

Para elegir una foto propia, el cliente NO usa el REST `ImageListResponse` (`GET /station/images`,
`BaseServlet.java:190`) — que escanea `BFileSystem.INSTANCE.findFile("^")` (`ImageListResponse.java:31`) — porque
ese endpoint tiene **0 referencias en el bundle** (vestigial, como la image-library REST de B219 §219.1). En su
lugar, el componente `ImageBrowser`/`OrdTree` enumera el file space en vivo por **nav-RPC nativo de Niagara**
(`getNavChildren`) desde un nodo raíz "Station File System" (`ord:"station:|file:^"`, `BF:38839`). El usuario
navega el árbol y elige un archivo; `ImageBrowser.show()` acepta un ORD que debe empezar con `"file:^"` o
`"module://"` (`BF:40684`), y `okay()` emite `{ord, width, height}` (`BF:40689`) hacia `ImageSelect`, que llama
`$ord.image()` (B219 §219.4) para previsualizar/persistir la referencia.

**Producto**: "agregar mi foto" = (1) subir el archivo a la carpeta de la station con Workbench [fuera de Reflow],
(2) en el editor, abrir el picker, navegar el file space y seleccionar `file:^Imagenes/mi-foto.jpg`, (3) la card
(p. ej. `hyperlink.backgroundImage`) guarda ese ORD; se renderiza vía `/ord/` (B219).

## 220.4 — Formatos aceptados y la única escritura de bytes por HTTP `[CERT]`

- **Formatos** que el picker/listador reconoce: `jpg, jpeg, png, svg, gif` (`ImageListResponse.java:49`,
  filtro de extensión) `[CERT]`.
- **Única escritura de bytes vía POST**: `EquipmentNoteUpdateResponse.java` hace byte-passthrough del body a
  `^reflow/notes/<Equipment-Id>.json` (`:20-27`, temp-then-verify por `Content-Length`) — pero está scopeado a
  **NOTAS JSON de equipo**, NO a imágenes (target `^reflow/notes/`, no `^Imagenes/`) `[CERT]`. No es una vía de
  upload de fotos. (Queda como `[INFER]`/follow-up si una imagen podría viajar base64 dentro de `config_update`
  —p. ej. un logo de theme— pero no se observó.)

## 220.5 — Implicación para portar a chihuahua (BG11) `[INFER]`

El "upload" de Reflow es en realidad **cero código de upload**: se apoya 100% en el file space + Workbench +
servlets `/ord/` de la plataforma Niagara. Un módulo propio (chihuahua) que quiera "agregar fotos" tiene dos
caminos: (a) replicar el patrón Reflow (referenciar `file:^…` + picker de file space + servir por `/ord/`), lo
más barato; o (b) implementar un upload real (multipart) si se quiere subida in-app — algo que Reflow deliberada o
casualmente NO hace. Se retoma en BG11.

## 220.6 — Conexiones

- **[Block 219]** §219.4 — el ORD `file:^Imagenes/…` que se elige aquí se renderiza por `$ord.image()`→`/ord/`.
- **[Block 218]** §218.3 — `hyperlink.backgroundImage` (y `floorplans` photo, `theme.background`) son los
  consumidores de la foto elegida.
- **[Block 149]** — documentó `EquipmentNote` write-por-header como sink (seguridad); §220.4 lo reencuadra como la
  única escritura de bytes y aclara que NO es upload de imágenes.
- **Hacia adelante**: BG9 (Mapbox), BG3/BG4 (motor patch + editor), BG11 (chihuahua: replicar el patrón de assets).
