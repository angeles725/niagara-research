# Block 170 — chihuahua MX60 (`-ux`): topología de subscripción frontend (window.MX60, BajaScript + fallback REST, throttling)

> **WHAT:** topología de datos LIVE del frontend del módulo **chihuahua** MX60 (`chihuahua-ux`, capa `rc/js/`):
> el namespace `window.MX60` y su patrón IIFE ES5, cómo fluye el dato en vivo (subscripción BajaScript vs
> fallback REST 5s), el single-source-of-truth `EquipmentData`, el coalescing/throttling de snapshots
> (`EquipmentSnapshotStore`), el ring buffer histórico y la invalidación de cache. Cierra el gap **C8** del focus.
>
> Focus: **chihuahua** (arquitectura del módulo MX60, FUENTE PRIMARIA — no decompilado). Corpus language: Spanish.
>
> Sources (fuente primaria, alias `JS/` = `…/chihuahua/chihuahua/chihuahua-ux/src/rc/js/`):
> - `JS/app/SubscriptionPool.js` · `JS/app/EquipmentData.js` · `JS/app/EquipmentSnapshotStore.js` · `JS/app/LiveHistoryBuffer.js`
> - doc de diseño: `FRONTEND_ARCHITECTURE.md` (base `…/Cliente/Honeywell/MX60/chihuahua/`)
>
> **Sensibilidad:** despliegue de cliente real → `.env.local` (IP JACE / credenciales) **NO se leyó ni se cita**
> (SECRETS DISCIPLINE); el código JS es citable.
>
> Markers: `[CERT]` = leído en la fuente primaria (`file:line`) · `[INFER]` = deducción. Marker FUERA de la cita,
> estilo corpus. Capa 26 (frontend del dashboard OEM de autoría propia, contraparte del SPA Vue de nmodsreflow).
>
> Continúa [Block 163] (esqueleto del focus chihuahua).

---

## 170.1 — El namespace `window.MX60` y el patrón IIFE ES5

Todo el frontend MX60 vive bajo un único objeto global `window.MX60`, poblado por módulos IIFE ES5-estricto
que se sirven **directamente al browser sin transpiler** `[CERT]` `FRONTEND_ARCHITECTURE.md:10,258`. Cada `.js`
sigue el mismo esqueleto: `(function(window){ 'use strict'; var MX60 = window.MX60 || {}; … MX60.<Modulo> = {…};
window.MX60 = MX60; })(window)` `[CERT]` `JS/app/SubscriptionPool.js:28-31,570,585-587`. El patrón es
merge-idempotente (`window.MX60 || {}`) para que el orden de carga de los `<script>` no destruya lo ya
registrado.

- **State en closure, API pública mínima.** El state interno (caches, maps, timers) vive en variables
  closure-scoped; la superficie pública es sólo lo asignado a `MX60.<Modulo>` `[CERT]` `JS/app/EquipmentData.js:33-49,393-402`.
  Regla documentada: **nunca** accedas a `MX60.<store>._foo` desde consola/diags — devuelve `undefined` (closure)
  y produce falsos negativos `[CERT]` `FRONTEND_ARCHITECTURE.md:31-33,53-58`.
- **ES5 estricto obligatorio** en estos 4 módulos: sin `let`/`const`, sin arrow functions, sin template literals
  `[CERT]` `JS/app/SubscriptionPool.js:26`, `JS/app/EquipmentSnapshotStore.js:44`, `JS/app/LiveHistoryBuffer.js:16`.
  `[INFER]` Es la restricción de compatibilidad con browsers iSMA/Honeywell embebidos y con baja+bluebird.
- **Módulos de esta capa** (subset de §1): `MX60.SubscriptionPool`, `MX60.EquipmentData`,
  `MX60.EquipmentSnapshotStore`, `MX60.LiveHistoryBuffer` `[CERT]` `FRONTEND_ARCHITECTURE.md:19,20,26`.

---

## 170.2 — Dos rutas de dato: subscripción BajaScript (push live) vs fallback REST (poll 5s)

El frontend obtiene telemetría por **dos rutas mutuamente excluyentes en régimen** `[CERT]` `FRONTEND_ARCHITECTURE.md:133-141`:

| Ruta | Mecanismo | Latencia | Cuándo corre |
|---|---|---|---|
| **Baja subscription (push)** | `SubscriptionPool` → `new baja.Subscriber` + `baja.Ord.make(ord).get({subscriber})` | ~80ms–<1s | happy path (baja cargado) |
| **REST fetch snapshot** | `EquipmentData._restFetch()` → `GET /api/equipment` JSON completo | on-demand | first-paint inicial + `refresh()` manual |
| **REST fallback polling** | `_startRestPolling()` `setInterval` 5s | 5s | **sólo si baja falla** |

- **Carga de BajaScript vía requirejs, con timeout de 5s.** `_initBaja` exige `requirejs`; si no existe marca
  `_bajaFailed` y va a REST-only. Hace `requirejs(['baja!'], okCb, errCb)` y arma un `setTimeout(…, 5000)` que,
  si baja no cargó, setea `_bajaFailed` `[CERT]` `JS/app/SubscriptionPool.js:108-152`. Callbacks encolados en
  `_pendingCallbacks` se drenan al resolver `baja!` `[CERT]` `JS/app/SubscriptionPool.js:133-139`.
- **La subscripción real** arma `var sub = new _baja.Subscriber()`, engancha `sub.attach('changed', cb)`, y
  resuelve el ORD con `_baja.Ord.make(ord).get({ subscriber: sub })` — el `.then(comp)` entrega el componente
  resuelto y cada `'changed'` re-dispara el callback `[CERT]` `JS/app/SubscriptionPool.js:226-234`.
- **Elección de ruta en `EquipmentData.load`:** `_tryBaja` espera hasta **3s** haciendo poll de
  `SubscriptionPool.isReady()`/`isFailed()` cada 100ms `[CERT]` `JS/app/EquipmentData.js:233-262`. Si OK →
  `_subscribeAll()` + **una** REST fetch para first-paint (el polling NO arranca). Si falla → REST fetch +
  `_startRestPolling()` `[CERT]` `JS/app/EquipmentData.js:343-359`.
- **El fallback avisa una sola vez** con un Toast "Modo limitado: actualización cada 5s"; el intervalo sale de
  `cfg.pollMs.restFallbackMs` (default 5000) `[CERT]` `JS/app/EquipmentData.js:209-220`.

---

## 170.3 — Subscripción por-hijo estilo Reflow y el transform de slotName (GOTCHA G6)

`subscribeEquipment` no subscribe el monitor como un blob: resuelve el monitor ORD, itera sus hijos (los
componentes de equipo) y **subscribe cada hijo individualmente**, leyendo valores directo del objeto baja sin
roundtrip REST `[CERT]` `JS/app/SubscriptionPool.js:263-306,308-356`.

- **`monitorOrds` desde config**, no hardcode. `_readConfig` cae en `window.MX60_CONFIG ||
  MX60.ConfigManager.getConfig()` — el fallback a `ConfigManager` es crítico porque `MX60_CONFIG` nunca se
  asigna; sin él, `subscribeEquipment` sería no-op y el dashboard se congelaría tras el first-paint
  `[CERT]` `JS/app/SubscriptionPool.js:250-261,266-269`.
- **Lectura tipada por tipo de equipo.** `_readEquipment(comp, type)` recorre arrays de props numéricas/booleanas/
  string derivadas de las `@NiagaraProperty` de `BChiUp`/`BChiCarcamo`/`BChiDatalogger`, y para cada una hace
  `comp.get(prop).getValue()` con default seguro `[CERT]` `JS/app/SubscriptionPool.js:59-89,160-209`.
- **GOTCHA G6 — routing por slotName.** `_readEquipment` inyecta `data._slotName = comp.getName().toLowerCase()
  .replace(/_/g,'-')`, replicando el transform `slotNameToId` del backend, para que `EquipmentData` matchee el
  update contra su `_byId` `[CERT]` `JS/app/SubscriptionPool.js:197-206`, `JS/app/EquipmentData.js:312-318`.
- **Compat multi-versión de la API baja.** `_getMonitorSlotNames` prueba `getSlots().$keys` (iSMA 4.13.2),
  luego `getSlots().each(...)`, luego `getProperties()` (Honeywell 4.14) `[CERT]` `JS/app/SubscriptionPool.js:358-417`.
- **Debounce por monitor** vía `cfg.bajaDebounceMs` (default 200ms) para los eventos coarse de monitor
  `[CERT]` `JS/app/SubscriptionPool.js:284,289-293,466-472`.

---

## 170.4 — Watchdog de reconexión (silencio → re-subscribe)

`SubscriptionPool` corre un watchdog que detecta caída de la sesión Fox (Wi-Fi blip, idle timeout, restart de
estación): si NINGÚN update llega en `WATCHDOG_SILENCE_THRESHOLD_MS = 90000`ms mientras baja está `ready`, asume
drop y reconecta `[CERT]` `JS/app/SubscriptionPool.js:42-50,540-562`.

- `_lastUpdateMs` se refresca en cada `'changed'` y en cada resolve de hijo `[CERT]` `JS/app/SubscriptionPool.js:335,341-343`.
- El tick corre cada 30s (`WATCHDOG_TICK_MS`); al detectar silencio muestra Toast "Reconectando con la
  estación…", hace `cleanupAll()`, resetea flags y re-ejecuta `_watchdogResubFn` `[CERT]` `JS/app/SubscriptionPool.js:49,540-568`.
- La resub function la registra `EquipmentData._subscribeAll` vía `startWatchdog`, y re-corre el path
  `_tryBaja → _subscribeAll` `[CERT]` `JS/app/EquipmentData.js:271-282`.
- **Leak fix AP7:** en `pagehide` real (no bfcache) se limpia además el `_watchdogTimer`, que `cleanupAll()` NO
  toca a propósito `[CERT]` `JS/app/SubscriptionPool.js:519-530`.

---

## 170.5 — `EquipmentData`: single source of truth origin-agnóstico

`EquipmentData` es la ÚNICA fuente de verdad de la lista de equipos; su state (`_equipment`, `_byId`, `_byType`,
`_raw`) es **agnóstico al origen** — los consumers UI no saben si el dato vino de baja o de REST
`[CERT]` `JS/app/EquipmentData.js:1-5,25-27,35-43,74-76`.

- **API pública estable** (idéntica a la versión REST-only v3): `load/refresh/addListener/removeListener/
  getById/getByType/getAll/isLoaded` `[CERT]` `JS/app/EquipmentData.js:393-402`.
- **Merge dual root+summary (bug live-update).** El backend serializa telemetría anidada bajo `summary.*` y TODOS
  los consumers leen de ahí, pero `SubscriptionPool` entrega objeto plano. `_applySubData` escribe cada key en
  `target[k]` **y** `target.summary[k]`, y recomputa `status` client-side con la regla
  `fanOn||compressor1On||compressor2On` `[CERT]` `JS/app/EquipmentData.js:285-310`.
- **CRIT-2 — buffer de updates tempranos.** Updates de subscripción que llegan antes de que la primera REST fetch
  poble `_byId` se encolan en `_pendingSubUpdates` y se replayean tras `_index()` en `onload`
  `[CERT]` `JS/app/EquipmentData.js:51-55,164-175,320-329`.
- **Hidratación de stores derivadas** en cada REST fetch: `_seedThresholdStores` (Up/Carcamo/Datalogger) +
  `AlarmLatchStore.seedFromEquipment` `[CERT]` `JS/app/EquipmentData.js:107-117,155-163`.
- **URL del endpoint** vía `ConfigManager` con default `/mx60/api/equipment` `[CERT]` `JS/app/EquipmentData.js:123-127`.

---

## 170.6 — `EquipmentSnapshotStore`: coalescing RAF + cap 500ms (separa data cycle de UI cycle)

Este store desacopla el ciclo de dato (push baja 1-2 Hz) del ciclo de UI: `EquipmentData._notify()` empuja
snapshots, el store los coalesce, y un scheduler único despacha **un solo flush por ventana** a los subscribers
UI — evitando layout thrashing `[CERT]` `JS/app/EquipmentSnapshotStore.js:8-26`.

- **Único listener de `EquipmentData`.** `init()` engancha `_onEquipmentData` como listener; por cada equip en
  `state.byId` guarda `_latestById`, hace `_ringPush` (ring de 120) y marca `_pendingIds`
  `[CERT]` `JS/app/EquipmentSnapshotStore.js:97-110,189-210`.
- **Scheduler dual RAF + setTimeout cap.** `_scheduleFlush` arma `requestAnimationFrame(_flushIfReady)` para
  alinear al frame natural **y** un `setTimeout(…, _flushIntervalMs=500)` como cap duro para cuando el tab está
  hidden (RAF no dispara); pushes que coinciden con un flush ya programado incrementan `_coalesceCount`
  `[CERT]` `JS/app/EquipmentSnapshotStore.js:52-53,112-133`.
- **Payload del flush por id:** `{ latest, ringBuffer (slice del ring, cap 120), deltaMs }`
  `[CERT]` `JS/app/EquipmentSnapshotStore.js:135-166`.
- **Dos modos de subscriber:** `subscribe(equipId, cb)` (por equipo) y `subscribeAll(cb)` (batch global — recibe
  `{ids, latestById, flushTs}` una vez por flush) `[CERT]` `JS/app/EquipmentSnapshotStore.js:212-236,308-323`.
- **`flushNow(equipId)`** entrega sync para first-paint sin esperar los 500ms; intervalo y ring son tunables
  (`setFlushInterval`/`setRingSize`) `[CERT]` `JS/app/EquipmentSnapshotStore.js:238-261,273-287`.

---

## 170.7 — `LiveHistoryBuffer`: ring in-memory y el fix de hot-path T2.7

Ring buffer per-equipId (`MAX_POINTS = 120`) que acumula snapshots live para graficar cuando un slot no tiene
`BHistoryExt` configurado `[CERT]` `JS/app/LiveHistoryBuffer.js:1-16,27`.

- **`addDataPoint`** copia todos los slots no-metadata (lista `SKIP`) a una entrada `{t, values}` timestamped y
  hace shift al superar 120; también aplana `summary.*` `[CERT]` `JS/app/LiveHistoryBuffer.js:30-34,50-75`.
- **Fix de performance T2.7.** Antes `EquipmentData._notify()` llamaba `addDataPoint` por cada uno de los 68
  equipos en cada notify (~16/seg) = **1.088 calls/seg** (7,6% CPU baseline). Ahora `LiveHistoryBuffer` es
  subscriber global del `EquipmentSnapshotStore` vía `subscribeAll(_consumeFlush)`, que flushea throttled cada
  ~500ms = ~136 calls/seg (8× menos) `[CERT]` `JS/app/LiveHistoryBuffer.js:168-194`, `JS/app/EquipmentData.js:95-100`.
- **API de lectura:** `getSeries` (filtra sólo numéricos finitos para Chart.js), `getSlots`, `filterByRange`,
  `getCount`, `clear` `[CERT]` `JS/app/LiveHistoryBuffer.js:86-99,109-121,131-165`.

**Topología completa del hot-path (post-T2.7):**
`SubscriptionPool` (baja push) → `EquipmentData._onSubUpdate/_applySubData` → `_notify()` →
`EquipmentSnapshotStore._onEquipmentData` (coalesce RAF+500ms) → `_flush()` →
{ subscribers UI por-id · `subscribeAll` → `LiveHistoryBuffer._consumeFlush` } `[INFER]` (deducido del wiring
de §170.5–170.7).

---

## 170.8 — Invalidación de cache y límites de la topología de subscripción

- **Server-initiated changes NO propagan.** Slots NO subscritos via baja (`alarmLatches`, `protXActive`,
  `fanCmd`/`compCmd`, `alarmCounts`) sólo se ven tras `EquipmentData.refresh()` o F5 manual. Ej: un
  `doResetAlarmas()` invocado desde Workbench muta el slot server-side pero el dashboard sigue mostrando latches
  viejos `[CERT]` `FRONTEND_ARCHITECTURE.md:152-172`.
- **Patrón de invalidación post-action.** Tras invocar una action baja-native, en el `.then()` success hay que
  invalidar explícitamente **ambas** caches downstream: `EquipmentData.refresh()` (device state/latches) y
  `AlarmsManager.loadAll()` (lista de alarm records) — si invalidás sólo una, la otra queda stale hasta su propio
  ciclo `[CERT]` `FRONTEND_ARCHITECTURE.md:176-214`.
- **`refresh()` es sólo REST**, no altera el origin (fuerza `_restFetch`) `[CERT]` `JS/app/EquipmentData.js:365-367`.
- **Manejo de sesión expirada:** REST 401/403 dispara Toast "Sesión expirada" y detiene el polling
  `[CERT]` `JS/app/EquipmentData.js:181-184`.

---

## 170.x — Connections

- **[Block 163]** (esqueleto del focus chihuahua): este bloque profundiza la parte `chihuahua-ux` `rc/js/` que
  163 nombró como gap. La estructura tri-parte `-rt/-ux/-wb` y el alias `JS/` provienen de ahí.
- **[Block 165]** (endpoints `/api` del backend `-ux`): la ruta fallback poll aquí documentada consume
  `GET /mx60/api/equipment` `[CERT]` `JS/app/EquipmentData.js:123-127`; el `refresh()` manual y la hidratación de
  stores golpean el mismo endpoint servido por `BChiServlet`. Ver 165 para el lado servidor de ese contrato JSON
  (shape `{equipment:[…summary:{}]}`).
- **[Block 153] / [Block 152]** (nmodsreflow: SPA Vue + baja): **contraparte de comparación arquitectónica.**
  Ambos módulos usan BajaScript para live push, pero difieren en la capa de app:
  - **chihuahua** = ES5 IIFE sobre `window.MX60`, sin build step, sin framework; state en closures; throttling
    manual (RAF+500ms) hecho a mano en `EquipmentSnapshotStore` `[CERT]` `JS/app/EquipmentSnapshotStore.js:8-26`.
  - **Reflow** = Vue SPA (reactividad del framework maneja el fan-out a la UI; ver [Block 152]/[Block 153]).
  `[INFER]` La equivalencia funcional: `EquipmentSnapshotStore` + listeners cumple el rol que en Reflow cumple el
  sistema reactivo de Vue; chihuahua lo reimplementa a mano por la restricción no-transpiler/ES5.
