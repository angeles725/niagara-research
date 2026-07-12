# Block 217 — Reflow dashboard-builder (II): el modelo de dashboard editable, su persistencia y el update en vivo

> **Qué documenta.** Cómo Reflow REPRESENTA un dashboard como dato, cómo lo PERSISTE, y cómo un cambio se
> PROPAGA en vivo a otros navegadores. Es el núcleo del "creá un dashboard, modificalo y que se actualice"
> del pedido. Gap BG2 del focus `nmodsreflow-builder`.
>
> **Alcance.** El modelo de datos (pool de tarjetas + forma de una tarjeta), la jerarquía de superficies
> (landing/pages/buildings), la serialización a `config.json`, las dos vías de guardado (full-write vs
> JSON-Patch delta) y el push por WebSocket. NO disecciona el motor zjsonpatch en sí (BG3) ni el editor
> visual/paleta (BG4) ni el catálogo de tipos de tarjeta (BG5).
>
> **Fuentes (primarias).**
> - SPA beautificada (1:1 con el minificado; identidad anclada al original
>   `app.4509efb4.js` sha256 `81b82b83…`, 2 631 974 B): temp `scratchpad/reflow-app.beauty.js` (123 740 líneas).
>   Se cita `BF:<línea>` de ese temp.
> - Java `-rt`: `com/niagaramods/nmodsreflow/http/responses/ConfigUpdateResponse.java`,
>   `.../ConfigDeltaResponse.java`, `sync/BReflowSyncService.java` (paths reales, `file:line`).
> - Barrido delegado (sonnet) sobre el beautified temp; tokens load-bearing re-verificados por grep por el
>   driver antes de escribir.
>
> **Método / markers.** `[CERT]` = leído en la fuente primaria (`BF:` beautified 1:1 · Java `file:line`).
> `[CERT-live]` = observado contra la station N4 VIVA (GET read-only `https://localhost/nmodsreflow/config`,
> usuario `API`/HTTPBasicScheme, HTTP 200; probe sanitizado en `sources/probes/B217-live-config-structure-20260712.txt`).
> `[INFER]` = deducción. **DIVERGENCIA DE VERSIÓN (honesta):** el corpus static (este bloque + B216) reconstruye
> el JAR **Reflow 1.7.7.75**; la station viva corre **Reflow 1.7.5-43** (`reflowVersion`, re-medido live §12). El
> modelo de datos validado abajo (§217.8) es común a ambas versiones (mismo `config.json` schema v14); donde una
> afirmación sea específica de 1.7.7 se marca. SECRETS DISCIPLINE: se cita ESTRUCTURA del config vivo, nunca los
> valores del sitio del cliente (nombres de edificios/equipos, coords, branding, profiles).

---

## 217.1 — El dashboard es un POOL PLANO de tarjetas (Vuex `dashboardCards`) `[CERT]`

El estado del dashboard vive en el store Vuex, módulo `dashboardCards` (registrado junto a ~17 módulos:
`alarms, colors, equipment, floorplans, buildings, histories, landing, navigation, pages, dashboardCards,
schedules, theme, weather, license, updates, notify, menu…`, `BF:13952`). Su estado es **un array plano y
canónico de tarjetas** (`state.cards[]`, `BF:8380`), no un árbol por página.

Forma de UNA tarjeta (reconstruida del estado semilla + el mixin editor `mutateConfig`/`mutateCard`, `BF:8380`,
`BF:92458-92525`):

```jsonc
{
  "id": "uuid",                 // vt.generate() guid si falta (mutación ADD_CARD)
  "type": "alarm|weather-forecast|table|…",  // 1 tipo por componente Vue (catálogo → BG5)
  "enabled": true,
  "config": { /* sub-objeto por tipo: title, columns[], rows[], console, building, priorities[]… */ },
  "width":  "single|double|full",                       // BF:92460 (default "single")
  "height": "auto|quarter|half|single|double|exact|min",// BF:92487 (quarter/half gated hasPercentageHeights)
  "heightValue": 0,             // [INFER] nombre inferido de un único read-site BF:22681 (sólo si height="exact")
  "name": "opcional"
}
```

Enum `width`/`height` grep-confirmado (`single/double/full/quarter/half`, `BF:92460-92525`) `[CERT]`. Mutaciones
del módulo (`BF:8399-8431`): `ADD_CARD` (genera `id` si falta), `REMOVE_CARD`, `DUPLICATE_CARD` (deep-clone vía
lodash `cloneDeep`), `UPDATE_CARD` (**reemplazo del objeto completo por id — NO es un patch parcial**),
`REMOVE_BLANK_CARDS`, `RESET_ALARM_CARD_CONSOLES`.

**No hay mutación `REORDER_CARD`** en el módulo `[CERT]` (grep negativo). El orden NO es una propiedad de la
tarjeta: vive en los contenedores (§217.2) como arrays ordenados de IDs; reordenar = recommitear el array de IDs
completo (`SET_CARDS`, `BF:7809`) `[INFER]` (por ausencia + forma de `SET_CARDS`, no por leer el handler de drag).

## 217.2 — Jerarquía de superficies: contenedores que REFERENCIAN el pool por id `[CERT]`

Las tarjetas NO se guardan por página. Cada "superficie de dashboard" sólo tiene un array ordenado de IDs que
apuntan al pool único `dashboardCards.cards[]`:

| Superficie | Estado | Evidencia |
|---|---|---|
| **landing** (dashboard raíz/home) | `cards: [id, id, …]`, `maximumDashboardColumns: 4` | `BF:7772-7786` |
| **pages[]** (páginas de nav; `type:"reflow"` = página-dashboard) | cada item lleva su propio `cards: [ids]` + `groupId`/`subnavId` | `REMOVE_ITEM`: `"reflow"===a.type && a.cards.forEach(REMOVE_CARD)` `BF:8315` |
| **buildings** (dashboard por edificio) | cada building lleva `cards: [ids]` | acción `removeCard` recorre `landing.cards`, `pages.items[].cards` y `buildings/getBuildings[].cards` `BF:8497-8524` |

`DUPLICATE_ITEM` (`BF:8292`) al duplicar una página duplica cada tarjeta referenciada generando IDs nuevos →
cada página posee su propia LISTA de tarjetas (no comparte instancias). La jerarquía de producto: **buildings →
(floors/equipment) + landing (raíz) + pages[] (lista plana, `type:"reflow"`) + cards por building**, todos
tirando instancias de widget del mismo pool por id `[CERT]`. (El anidado floors/equipment no se recorrió — fuera
de BG2, `[INFER]`.)

## 217.3 — Serialización: TODO el estado Vuex menos lo efímero → `config.json` (un blob opaco) `[CERT]`

El payload de guardado se arma con `Na(state)` (`BF:13938`): hace `cloneDeep` del estado raíz completo, **borra
~38 claves efímeras/de sesión** (`alarmData, demo, license, menu, notify, socket*, migration*, user, …`, lista
`Ma[]` `BF:13938`), y pasa el resto por `removeUndefined`. El resultado es `config.json` verbatim. Es decir: el
`config.json` = TODOS los módulos persistentes (`dashboardCards, landing, pages, buildings, equipment, colors,
theme, weather, schedules, navigation, alarms, floorplans, histories…`) serializados juntos. Esto **confirma
desde el cliente la tesis del servidor delgado** (B216 §216.1): el backend nunca modela el dashboard; recibe,
guarda y sirve un JSON opaco. `getters.stateJson` (`BF:14100`) es la misma función envuelta en `JSON.stringify`
(export/diagnóstico, no la vía de guardado).

## 217.4 — Dos vías de guardado: full-write vs JSON-Patch delta `[CERT]`

La acción `save` (`BF:14114`) se **dispara sola**: un subscriber de mutaciones Vuex (`BF:118263`) tiene una
whitelist de ~30 mutaciones efímeras a IGNORAR; **cualquier otra mutación** (tocar `dashboardCards`, `landing`,
`pages`, `buildings`…) cae en `else → dispatch("save")`. No hace falta botón "Guardar": editar una tarjeta
auto-dispara el pipeline (debounce `saveWaitTime` ≈ 3000 ms). Luego bifurca por MODO:

**(a) Single-user / con control → full write** (`saveState`, `BF:14144`):
`post("/nmodsreflow/config_update", Na(state), headers:{Client-Id, Client-Username, Client-Migration})`; exige el
header de respuesta `config-timestamp` o lanza error. Servidor `ConfigUpdateResponse.java`: escribe el body a un
temp `^reflow/cache/temp/<ts>`, verifica byte-count vs `Content-Length`, copia temp→`^reflow/config.json`
(`:33,88` — write-to-temp-then-copy, no rename atómico), setea header `Config-Timestamp` (`:91`), y **broadcast**
`{"type":"config-reload","author":{…}}` en canal `"reflow"` (`:96,101`) `[CERT]`.

**(b) Multi-user → delta JSON-Patch** (`saveDelta`, `BF:14184`): toma un snapshot fresco `Na(state)`, lo DIFF-ea
contra el último snapshot sincronizado (lib fast-json-patch, export `compare` → array RFC-6902); si vacío, aborta.
Hace ping WS `{command:"sync-delta", action:"ping"}`; el server responde `{sendFullState: config==null}`. Si
`sendFullState` → cae al MISMO full-write. Si no → `post("/nmodsreflow/config_delta", {delta})`. Servidor
`ConfigDeltaResponse.java` → `sync.applyConfig(...)` → hilo `ConfigSyncTask` que aplica
`JsonPatch.apply(delta, service.config)` (`BReflowSyncService.java:420`, `com.flipkart.zjsonpatch`), setea el
nuevo config y **broadcast** `{"type":"delta","delta":<patch>,"author":{…}}` en `"reflow"` (`:438,441,446`)
`[CERT]`.

**La clave de producto**: el modo determina la vía; NO hay heurística "edición chica→delta, grande→full". El
switch es puramente `state.isMultiUser` `[CERT]` (`BF:14114`).

## 217.5 — Push en vivo: reload forzado (full) vs merge en caliente (delta) `[CERT]`

El cliente escucha el canal `"reflow"` (`messageSubscriber`, `BF:14460`) y trata cada tipo distinto:

- **`config-reload`** (emitido por cada full-write, §217.4a) → si `isMultiUser` y el autor no es uno mismo →
  `SET_REQUIRES_RELOAD(true)`, una bandera suave. Se observa en `BF:118191`: dispara un modal BLOQUEANTE
  **"Reload Required — Another user has replaced the Reflow configuration file… You must reload"** con botón que
  hace `location.reload(true)`. Es decir: **un full-write obliga a los demás viewers a un reload manual de página**
  `[CERT]` (`BF:118191`, "Reload Required" grep-confirmado).
- **`delta`** (emitido por cada delta-save) → `subscribeToDeltas` (`BF:14508`) aplica el MISMO patch a su árbol
  Vuex reactivo local (`applyPatch(state, patch)` vía mutación `STATE_DELTA`, `BF:14029`) y emite `delta-sync`.
  **Otros navegadores aplican el JSON-Patch directo sobre su estado reactivo — sin reload** `[CERT]`
  (`delta-sync` grep-confirmado `BF:14029`).

Resumen del "y se actualiza": en estación **multi-usuario** la edición se propaga como **merge en caliente**
(JSON-Patch aplicado sobre el Vuex de cada viewer, la UI reacciona sola); un **full-write** (single-user, o
fallback) fuerza **reload duro** en los demás. Esta es la razón de producto por la que el delta existe: edición
colaborativa sin recargar.

## 217.6 — Nota de librerías: dos implementaciones de JSON-Patch en los extremos `[CERT]`/`[INFER]`

El cliente diffea/aplica con **fast-json-patch** (exports `compare`/`applyPatch`, `BF:14029,14184`); el servidor
aplica con **flipkart-zjsonpatch** (`BReflowSyncService.java:7,420`). Son dos libs RFC-6902 DISTINTAS, acopladas
sólo por producir/consumir el mismo formato de operaciones `[CERT]`. El wrapper HTTP del cliente (`p.a`, módulo
`0c7c`) imita la forma pública de axios pero **no es axios** (B216 §216.4: `axios`=0 hits); su cuerpo vive en
`chunk-vendors.js` (no abierto — `[INFER]`/thin). Detalle del motor server zjsonpatch → BG3.

## 217.8 — Validación contra la station VIVA `[CERT-live]`

GET read-only `https://localhost/nmodsreflow/config` (usuario `API`/HTTPBasicScheme, HTTP 200, 60 154 B; dashboard
**default de fábrica**, confirmado por el usuario). Estructura observada (probe sanitizado
`sources/probes/B217-live-config-structure-20260712.txt`):

| Afirmación static (§) | Observado en vivo | Verdict |
|---|---|---|
| §217.3 `config.json` = todo el estado Vuex persistente | 18 keys top-level: `alarms, buildings, colors, dashboardCards, equipment, floorplans, histories, landing, navigation, pages, profiles, reflowVersion, savePaused, schedules, stateLoaded, theme, version, weather` | **CONFIRMED** |
| §217.1 pool plano `dashboardCards.cards[]` | `cards` length 2, cada card = `{id, type, enabled, config}` | **CONFIRMED** |
| §217.1 `width` opcional (default `single`) | ninguna card trae `width`/`height` → defaults aplican | **CONFIRMED** |
| §217.1 `config` = sub-objeto por tipo | card `alarm`: `config={display, displayType, title}`; card `weather-forecast`: `config={}` | **CONFIRMED** (coincide con el estado semilla del bundle `BF:8381`) |
| §217.2 contenedores referencian IDs del pool | `landing.cards` = array de 2 ids, y `landing.cards == dashboardCards.cards[].id` (`match=true`); `pages.items`=0 | **CONFIRMED** |

Identidad live re-medida (§12, no heredada): `reflowVersion=1.7.5-43`, `config version=14`, `stateLoaded=true`,
`savePaused=false`. El `config.json` vivo tiene el mismo esquema que la reconstrucción 1.7.7 → el modelo de datos
del dashboard es **estable entre 1.7.5 y 1.7.7** `[CERT-live]`. El backup del estado default (sha256 `bf70f28f…`,
sólo en scratchpad) queda como target de revert para el experimento de escritura (bloque dinámico siguiente).

## 217.7 — Conexiones

- **[Block 216]** — §216.1 identificó `flipkart-zjsonpatch` como "motor editá-y-se-actualiza"; §217.4b/217.5
  lo ubican en el flujo real (delta multi-usuario) y revelan el segundo actor (fast-json-patch en cliente).
- **[Block 143]/[Block 145]** — documentaron `applyConfig`/`ConfigDeltaResponse`/`ConfigUpdateResponse` como
  superficie de escritura sin auth (seguridad); §217 las reencuadra como la MAQUINARIA DE GUARDADO del producto
  (temp-then-copy, headers `Client-*`, broadcast). Coherente con ambos ángulos.
- **[Block 154]** — mapeó endpoint→método; §217 agrega el DISPARADOR (auto-save por mutación) y la semántica
  full-vs-delta.
- **VALIDACIÓN LIVE (§217.8, REALIZADA)**: la forma de `dashboardCards.cards[]`, la serialización a `config.json`
  y la referencia landing→pool-ids quedan CONFIRMED contra la station viva (`[CERT-live]`, dashboard default).
  Pendiente de la fase dinámica de ESCRITURA (bloque siguiente): observar el broadcast `delta`/`config-reload` en
  el WS y un ciclo edit→save→propagación end-to-end creando un dashboard más completo (backup `bf70f28f…` listo
  como revert).
- **Hacia adelante**: BG3 (zjsonpatch en detalle), BG4 (cómo el editor produce estas mutaciones), BG5 (los tipos
  de tarjeta del pool), BG11 (chihuahua persiste distinto — comparar este modelo con el de `com.angeles.chihuahua`).
