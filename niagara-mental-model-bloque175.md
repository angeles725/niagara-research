# Block 175 — chihuahua MX60 (`-ux`): subsistema schedule (NumericSchedule BQL, filtro BChiUp-parent, WebScheduler iframe)

> **WHAT** — Documenta el subsistema de horarios (schedules) del módulo Niagara **chihuahua** MX60: cómo el backend `-ux` descubre los `BNumericSchedule` de la estación vía BQL, por qué filtra sólo los que cuelgan de un `BChiUp`, la forma del JSON que devuelve `/api/schedules`, y cómo el frontend delega toda la EDICIÓN al editor nativo de Niagara (WebScheduler) embebido en un `<iframe>` modal — es decir, listado read-only del lado servidor + UI de edición nativa.
> **Foco:** **chihuahua** (Cliente/Honeywell/MX60).
> **Idioma:** Español.
> **Sources (aliases):**
> - `UX/` = `chihuahua-ux/src/com/angeles/chihuahua/ux/`
> - `JS/` = `chihuahua-ux/src/rc/js/`
> **Markers legend:** `[CERT]` = verificado contra file:line real. `[INFER]` = inferencia razonada a partir del código, no impresa literalmente.
> `.env.local` **NO leído** (fuera de alcance).
> **Capa 26.**
> Continúa [Block 163].

---

## 175.1 — Panorama: listado read-only server-side + edición nativa client-side

El subsistema schedule de chihuahua se apoya en una división de responsabilidades limpia:

- **El backend `-ux` sólo LISTA.** No crea, no edita, no persiste horarios. Su único trabajo es descubrir los `BNumericSchedule` de la estación y serializarlos a JSON para que el dashboard pinte una tarjeta por cada horario `[CERT]` `UX/ChiScheduleHelper.java:16-27`.
- **La EDICIÓN la hace el editor nativo de Niagara** (WebScheduler / WebCalendarScheduler / WebTriggerScheduler), que el frontend embebe en un `<iframe>` modal apuntando a la ORD del schedule con su vista `[CERT]` `JS/app/ScheduleView.js:5-7`.

Esto significa que chihuahua NO reimplementa la lógica de horarios semanales, calendarios ni excepciones: reutiliza el framework HX de Niagara para la parte compleja y sólo aporta el descubrimiento + el chrome del modal `[INFER]`. El helper documenta su origen: "Adapted from SnlsScheduleHelper (SanLuis reference module)" `[CERT]` `UX/ChiScheduleHelper.java:29`, y el frontend "Ported verbatim from SanLuis ScheduleView.js" `[CERT]` `JS/app/ScheduleView.js:16`.

---

## 175.2 — Descubrimiento por BQL: `select * from schedule:NumericSchedule`

El descubrimiento server-side vive en `ChiScheduleHelper.writeSchedules(PrintWriter, BComponent)`. Construye una ORD BQL contra la raíz de la estación y la resuelve a una `BITable`:

```
station:|slot:/|bql:select * from schedule:NumericSchedule
```

Esto está en `[CERT]` `UX/ChiScheduleHelper.java:81-83`. El resultado se recorre con un `TableCursor` `[CERT]` `UX/ChiScheduleHelper.java:85-90`.

Punto clave documentado en el propio código: el select apunta al **tipo base** `schedule:NumericSchedule`, y BQL en Niagara es **polimórfico** — devuelve también todos los subtipos. En N4.14 ese tipo base es el padre de Weekly, Calendar y Trigger, por lo que un único select cubre las tres variantes (patrón que el código llama "UNION-3") `[CERT]` `UX/ChiScheduleHelper.java:62-83`. El import correspondiente es `javax.baja.schedule.BNumericSchedule` `[CERT]` `UX/ChiScheduleHelper.java:7`.

**Degradación graciosa:** cualquier excepción durante la ejecución del BQL o la iteración del cursor se loguea como WARNING y se escribe un array vacío `[]` en vez de propagar el error `[CERT]` `UX/ChiScheduleHelper.java:107-111`. El cursor se cierra en un `finally` defensivo `[CERT]` `UX/ChiScheduleHelper.java:112-118`.

---

## 175.3 — El filtro BChiUp-parent: por qué sólo horarios de las UP

No todos los `BNumericSchedule` de la estación llegan al dashboard. Dentro del bucle del cursor, cada schedule se filtra por su **componente padre**: sólo se emite si `schedule.getParent()` es una instancia de `BChiUp` `[CERT]` `UX/ChiScheduleHelper.java:94-97`. Si el padre es de cualquier otro tipo, se hace `continue` y el horario se descarta.

El motivo es de diseño (per-UP), explicitado en el javadoc del helper: "only per-UP schedules appear; carcamos and dataloggers are excluded by design" `[CERT]` `UX/ChiScheduleHelper.java:20-22`. Es decir, un horario que cuelgue de un cárcamo, de un datalogger o de cualquier otro equipo NO aparece en la página de Schedules — sólo interesan los horarios asociados a una Unidad Paquete (UP), que es el equipo cuyo funcionamiento se programa `[INFER]`.

El `BChiUp` recuperado (`up`) se pasa junto al schedule al serializador para adjuntar metadata del equipo `[CERT]` `UX/ChiScheduleHelper.java:97-102`. El import de `BChiUp` viene de `com.angeles.chihuahua.components.BChiUp` `[CERT]` `UX/ChiScheduleHelper.java:3` — ver [Block 168] para la definición de ese componente.

---

## 175.4 — Forma del JSON: `serializeSchedule`

Cada horario se serializa a mano (concatenación de strings, sin librería JSON) en `serializeSchedule(BNumericSchedule, BChiUp, PrintWriter)` `[CERT]` `UX/ChiScheduleHelper.java:136-209`. Los campos emitidos son:

- `name` — nombre de slot del schedule `[CERT]` `UX/ChiScheduleHelper.java:138,152`.
- `displayName` — en realidad se rellena con el **label de la UP** (`equipLabel`), no con el nombre del schedule `[CERT]` `UX/ChiScheduleHelper.java:153`.
- `slotPath`, `navOrd`, `type` — ruta de slot, ORD de navegación y tipo Niagara del schedule `[CERT]` `UX/ChiScheduleHelper.java:139-141,154-156`. `navOrd` es el campo que luego usa el frontend para construir la URL del iframe.
- `viewType` — vista WebScheduler derivada del tipo (ver §175.6) `[CERT]` `UX/ChiScheduleHelper.java:142-143,157`.
- `equipId` — id de frontend derivado del slot de la UP, en minúsculas con `_`→`-` (p.ej. `up01-p1`) `[CERT]` `UX/ChiScheduleHelper.java:145-146,158`.
- `equipLabel` — label operable de la UP (lee el slot `label` del `BChiUp`, con fallback al nombre de slot) `[CERT]` `UX/ChiScheduleHelper.java:147,159,215-232`.
- `equipOrd` — slotPath completo del `BChiUp` `[CERT]` `UX/ChiScheduleHelper.java:148,160`.
- `planta` — índice de planta leído del slot `planta` del `BChiUp` (numérico, default 0) `[CERT]` `UX/ChiScheduleHelper.java:149,161,234-247`.
- `out` — valor de salida actual del schedule (`schedule.get("out").toString()`) `[CERT]` `UX/ChiScheduleHelper.java:164-167`.
- `outValue` / `outStatus` — si el slot `out` es un `BStatusValue`, se desglosan valor interno y status `[CERT]` `UX/ChiScheduleHelper.java:168-173`.
- `parent` — objeto anidado `{ name, displayName, slotPath }` describiendo el `BChiUp` `[CERT]` `UX/ChiScheduleHelper.java:176-181`.
- `facets` — objeto `{ key:value, ... }` si el slot `facets` existe y es `BFacets`; envuelto en try/catch que ignora fallos `[CERT]` `UX/ChiScheduleHelper.java:183-206`.

El escapado de strings delega en `ChiJsonUtil.escapeJson` vía el helper local `escJson` `[CERT]` `UX/ChiScheduleHelper.java:249-253`.

---

## 175.5 — El endpoint `/api/schedules` en el servlet

El listado se expone como `GET /mx60/api/schedules`. En `BChiServlet`, el dispatcher enruta la acción `ChiServletDispatch.RouteAction.SchedulesList` a `handleSchedules(resp)` `[CERT]` `UX/BChiServlet.java:285-287`.

`handleSchedules` `[CERT]` `UX/BChiServlet.java:635-651`:

1. Fija los headers de API (`Content-Type: application/json; charset=UTF-8` + `Cache-Control: no-cache, no-store, must-revalidate`) `[CERT]` `UX/BChiServlet.java:637,1272-1276`.
2. Pone status `200 OK` `[CERT]` `UX/BChiServlet.java:641`.
3. Delega TODO el trabajo de serialización a `ChiScheduleHelper.writeSchedules(out, this)` — pasándose a sí mismo (`this`, el servlet-BComponent) como contexto para la resolución de la ORD BQL `[CERT]` `UX/BChiServlet.java:642`.
4. Red de seguridad secundaria: si el helper lanzara algo pese a su propio try/catch, el servlet loguea WARNING, pone `500` y escribe `[]` `[CERT]` `UX/BChiServlet.java:644-649`.
5. Siempre hace `out.flush()` `[CERT]` `UX/BChiServlet.java:650`.

El schedule también está declarado en la config que el servlet inyecta al frontend: la clave `"schedules":"/mx60/api/schedules"` `[CERT]` `UX/BChiServlet.java:101` y el ítem de menú `{"id":"schedules","label":"SCHEDULES"}` `[CERT]` `UX/BChiServlet.java:89`. Ver [Block 165] para el mapa completo de endpoints del servlet.

> Nota histórica en comentarios: el endpoint fue portado de `BSnlsServlet.handleSchedules` (SanLuis) y un fix D13 actualizó el comentario `BWeeklySchedule`→`BNumericSchedule` `[CERT]` `UX/BChiServlet.java:631-633`.

---

## 175.6 — Selección de vista WebScheduler por tipo (paridad Java/JS)

El tipo Niagara del schedule determina qué editor nativo abrir. Hay **dos implementaciones espejo** de este mapeo, una en cada lado:

- **Java:** `getViewTypeForScheduleType(String)` `[CERT]` `UX/ChiScheduleHelper.java:50-56` — `CalendarSchedule`→`view:schedule:WebCalendarScheduler`, `TriggerSchedule`→`view:schedule:WebTriggerScheduler`, default→`view:schedule:WebScheduler`. El resultado se emite en el campo `viewType` del JSON.
- **JS:** `_getScheduleView(type)` `[CERT]` `JS/app/ScheduleView.js:108-113` — misma tabla de decisión exacta (mismos tres casos, mismo default).

Es una duplicación deliberada `[INFER]`: el JS usa `_getScheduleView` en `openEditor` para no depender de que el backend haya rellenado `viewType`, mientras que el Java lo precalcula por si el frontend prefiere leerlo directo. Un comentario de código marca este tipo de paridad Java↔JS como contrato explícito (labels de planta) `[CERT]` `JS/app/ScheduleView.js:204-206`.

---

## 175.7 — Frontend: descubrimiento y tarjetas (read-only)

`ScheduleView.js` es un módulo IIFE ES5-estricto (sin `let`/`const`, sin arrow functions, sin template literals) `[CERT]` `JS/app/ScheduleView.js:19-22`, registrado como página `'schedules'` del `DashboardApp` `[CERT]` `JS/app/ScheduleView.js:527-529`.

Flujo de carga:

- `_fetchSchedules` hace un `XMLHttpRequest` GET al endpoint (leído de `MX60.ConfigManager.getConfig().api.schedules`, con fallback hard-coded a `/mx60/api/schedules`) `[CERT]` `JS/app/ScheduleView.js:42-52`. Cabecera `X-Requested-With: XMLHttpRequest` `[CERT]` `JS/app/ScheduleView.js:53`.
- Parsea la respuesta y sólo acepta arrays; cualquier otra cosa o JSON inválido cae a `_data = []` con `_loadError` `[CERT]` `JS/app/ScheduleView.js:60-78`. Un flag `_loadAttempted` evita el bucle infinito de XHR cuando el fetch falla (fix MED-1) `[CERT]` `JS/app/ScheduleView.js:57-59,245-259`.
- `_render` pinta una **tarjeta por horario** (`_buildScheduleCard`), mostrando valor de salida, tipo corto y un hint "Editar horario"; toda la tarjeta lleva `data-action="open"` `[CERT]` `JS/app/ScheduleView.js:138-188`.
- Filtro por planta (tabs `all`/1..6) y paginación de 12 por página (3×4) sobre el set filtrado `[CERT]` `JS/app/ScheduleView.js:30-31,263-295`.
- Refresco en vivo: re-fetch cada 30 s mientras la página está montada, **pausado si el modal del iframe está abierto** (para no competir con el bootstrap HX del iframe) `[CERT]` `JS/app/ScheduleView.js:481-490`.

Nada de esto edita el schedule: es puramente lectura + navegación `[INFER]`.

---

## 175.8 — Edición nativa: el `<iframe>` WebScheduler modal

El corazón de la estrategia "edición nativa" está en `openEditor` → `openEditorByOrd` `[CERT]` `JS/app/ScheduleView.js:304-386`:

- Al hacer click en una tarjeta, se toma el `navOrd` del item y su `viewType`, y se construye la URL del iframe:
  ```
  /ord/<encodeURI(navOrd + '|' + view)>?fullScreen=true
  ```
  `[CERT]` `JS/app/ScheduleView.js:316-319`. Es decir, se navega a la ORD nativa del schedule con la vista WebScheduler correspondiente, pidiendo pantalla completa.
- Ese iframe se inserta en un modal overlay creado dinámicamente (`schedule-modal-root`) con header, título y botón de cerrar `[CERT]` `JS/app/ScheduleView.js:322-355`.
- **Sin atributo `sandbox`** en el iframe — decisión explícita comentada: el WebScheduler necesita contexto de navegador completo (cookies, fetch, `top.location` para guardar) y un sandbox rompería el bootstrap del framework HX de Niagara `[CERT]` `JS/app/ScheduleView.js:344-348`.
- **Sin timeout** de carga: sólo `onerror` dispara el fallback (abrir en pestaña nueva vía `window.open`), porque el WebScheduler puede tardar >5 s en renderizar su layout HX completo (theme.css, schedule.css, app.js) y un timeout prematuro abría el editor en pestaña nueva en vez de en el modal `[CERT]` `JS/app/ScheduleView.js:365-375`.
- Cierre: click en overlay/botón, o tecla ESC (`_escapeHandler` sobre `keydown`); `closeEditor` remueve el modal y restaura el scroll del body `[CERT]` `JS/app/ScheduleView.js:358-403`.

Consecuencia arquitectónica: **toda la persistencia del horario ocurre dentro del iframe**, gestionada por el propio Niagara WebScheduler; chihuahua nunca ve ni intermedia el guardado `[INFER]`. `openEditorByOrd` está además expuesto en la API pública del módulo para que otros callers (p.ej. UpDetail) abran el editor conociendo ya la ORD `[CERT]` `JS/app/ScheduleView.js:315-316,511-520`.

---

## 175.x — Connections

- **[Block 165]** — mapa de endpoints del `BChiServlet`: aquí se documenta específicamente el endpoint `/api/schedules` (routing `SchedulesList`→`handleSchedules`, headers de API, config inyectada `api.schedules`).
- **[Block 168]** — `BChiUp`: el componente que es el **padre** por el que se filtra (§175.3). Sólo los `BNumericSchedule` cuyo `getParent()` es un `BChiUp` llegan al dashboard; este bloque documenta el listado, [Block 168] documenta el equipo padre y sus slots `label`/`planta` que el serializador lee.
- **Contrapartida de comparación — manejo de schedules en Reflow:** el otro módulo research-SDD (Reflow/nmodsreflow) tiene su propio tratamiento de horarios; conviene contrastar su enfoque de discovery/edición contra el patrón chihuahua (BQL polimórfico `NumericSchedule` + filtro por parent tipado + iframe WebScheduler nativo sin sandbox) para identificar divergencias de diseño entre ambos linajes.
- **Continúa [Block 163].**
