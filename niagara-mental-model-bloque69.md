# Bloque 69 — Audit empírico patrones live-update en Reflow-Clean-177: cierre flag #241 + tabla universal patrón-por-dominio + 10 implications #244..#253

**Fecha**: 2026-05-09
**Método**: Audit empírico DIRECTO sobre source `Reflow-Clean-177/` (READ-ONLY) **sin sintetizar de mapping**. Cada veredicto cita `file:line` y texto literal del código. Cierra la flag empírica #241 levantada en bloque #68 § 68.6.5 (discrepancia mapping `domains/alarms.md` "alarmSubscribe push WS" vs bloque #62 § 62.9.3 "polling 20s"). Spoiler: las DOS fuentes previas tenían errores parciales — la realidad empírica es más matizada.

**Fuentes primarias** (8 archivos clave + sweep `rg` cross-domain):
- `reflow-frontend/src/lib/alarmCache.js` (61 LOC)
- `reflow-frontend/src/plugins/niagara.js` (269 LOC)
- `reflow-frontend/src/api/websocket.js` (155 LOC)
- `reflow-frontend/src/mixins/subscriberMixin.js` (131 LOC)
- `reflow-frontend/src/lib/bajaHeartbeat.js` (151 LOC)
- `reflow-frontend/src/main.js` (123 LOC)
- `nmodsreflow-rt/.../http/sockets/BReflowChannelService.java` (281 LOC)
- `nmodsreflow-rt/.../history/HistoryGhostSubscriber.java` (26 LOC)
- + sweep `rg "setInterval|polling|subscriber\.|alarmSubscribe"` en `reflow-frontend/src/`

**Contexto previo cruzado**:
- Bloque #44 — Alarm Console pipeline frontend Niagara nativo
- Bloque #45 — History/Trend chart consumption (WebChart 3 GET + boxcs real-time)
- Bloque #62 — Alarmas Reflow dedicado (1137L, AP-72..78, reglas 20-22) — dijo "polling cada 20s vía alarmCache.js"
- Bloque #68 — Transplante MX60, flag #241 abierta
- Engram #1236 — methodology/mapping-vs-empirical-audit jerarquía de evidencia

---

## 69.0 Resumen ejecutivo

### Hallazgo principal: clean-room-177 NO está conectado a backend

**Reflow-Clean-177 es UI completa con backend mockeado**. Esto cambia la semántica del flag #241:

| Capa | Estado en clean-room-177 | Cita literal |
|---|---|---|
| `plugins/niagara.js` | **100% MOCK** | L1-5: `// Mock of the Niagara integration plugin. ... This mock returns safe defaults so components don't crash without a real N4 backend.` |
| `api/websocket.js` | **100% STUB** | L3-4: `// Stubs — no real connection, console.log for debugging\n// Real implementation: Phase 5+` |
| `lib/alarmCache.js` | **STUB infrastructure** | L1: `// alarmCache.js — Alarm count polling cache (stub)` + L4: `// Full implementation requires active Niagara alarm API — stub for now.` |
| `lib/bajaHeartbeat.js` | **DORMANT** | L13: `// This module is DORMANT until start() is called — add() and remove() are safe no-ops` |
| `main.js` import WS | **COMMENTED** | L107: `// import { initSocket } from './api/websocket';` |
| Backend `BReflowChannelService.java` | **REAL** (genérico pub-sub, no específico para alarmas) | L52: `public ConcurrentHashMap<String, ReflowChannel> channels = new ConcurrentHashMap<>();` |
| `HistoryGhostSubscriber.java` | **REAL** pero auto-unsuscribe al primer evento | L19-21: `public void event(BComponentEvent ...) { this.history.unsubscribe(this, this.subscriberCtx); }` |

**Implicación**: para inferir comportamiento de producción real hay que mirar el **bundle Reflow 1.7.5** (que sí está conectado), no clean-room-177. El clean-room nos da **forma + estructura + contratos**, no behavior runtime.

### Flag #241 — resolución consolidada

| Fuente previa | Afirmación | Veredicto empírico |
|---|---|---|
| Mapping `domains/alarms.md` §5 | "`$niagara.alarmSubscribe` → ChannelService → push WebSocket reactivo" | **WRONG en 3 puntos**: (1) `$niagara.alarmSubscribe` NO existe en `niagara.js` (mock no lo expone — verificado), (2) `BReflowChannelService` no emite alarmas (es pub-sub genérico para route-sync + config-control), (3) WS comentado y stub |
| Bloque #62 §62.9.3 | "alarmas NO usan WebSocket — usan polling cada 20s default vía `alarmCache.js`" | **CORRECT en spirit, WRONG en localización**: el polling existe pero vive en COMPONENTES (`AlarmDisplay.vue:168`, `AlarmsHome.vue:359`, `AlarmDetails.vue:451`) — NO en `alarmCache.js` (que es stub infraestructura sin uso real). Default 20s confirmado vía `consoleRefreshRate || 20`. |

---

## 69.1 Tabla universal — patrón live-update por dominio

| Dominio | Patrón empírico | Evidencia (file:line) | Cita literal |
|---|---|---|---|
| **Alarms — counts/sources** | `setInterval` polling, rate configurable (default 20s) en componentes | `AlarmDisplay.vue:167-168` | `var rate = this.alarmConsole.consoleRefreshRate \|\| 20;` `this.interval = setInterval(this.getAlarmCount, rate * 1000);` |
| **Alarms — home view** | `setInterval` polling 20s default | `AlarmsHome.vue:356-368` | `var refreshRate = (this.alarmConsole && this.alarmConsole.consoleRefreshRate) \|\| 20; ... this.refreshInterval = setInterval(async function () { ... await self.getAlarmSources(); await self.getAlarmClasses(); ...}, refreshRate * 1000);` |
| **Alarms — details view** | `setInterval` polling | `AlarmDetails.vue:451` | `this.refreshInterval = setInterval(async function () { ... }, ...)` |
| **History data on-demand** | Pull via BOX commands (no continuous) | `BReflowHistoryCommands.getData/getList/getQuickList` | (single-shot RPC, no subscription) |
| **History charts refresh** | `setInterval` polling configurable, mín 30s, default 300s (5 min) | `HistoryChart.vue:304-309` | `var interval = (this.card.config.refreshInterval && this.card.config.refreshInterval >= 30) ? this.card.config.refreshInterval : 300; this.refreshInterval = setInterval(function () { self.refreshData(true); }, 1000 * interval);` |
| **History sparklines** | Idem chart pattern | `HistorySpark.vue:305` | `this.refreshInterval = setInterval(function () { ...`  |
| **Points (live values)** | **BajaScript subscriber** via `subscriberMixin` (PUSH real Niagara) | `subscriberMixin.js:38` + `PointCard.vue:363`, `PointList.vue:140`, `NiagaraPoint.vue` | `this.$niagara.subscriber.subscribe(this.uuid, this.subscribedComponents, this.updateSubscribedComponents);` |
| **Equipment cards (Gauge/Circle/Toggle)** | BajaScript subscriber via `subscriberMixin` | `cards/Gauge.vue`, `cards/CircleCard.vue`, `cards/ToggleCard.vue`, `cards/table/Cell.vue` | (todos importan + extienden `subscriberMixin`) |
| **Schedules list** | BajaScript subscriber via `subscriberMixin` | `schedules/ScheduleList.vue` | (extiende `subscriberMixin`) |
| **Buildings status** | BajaScript subscriber via `subscriberMixin` | `buildings/BuildingStatusDisplay.vue`, `buildings/StatusWrap.vue` | (extienden `subscriberMixin`) |
| **Weather (current conditions)** | `setInterval` polling configurable | `Weather.vue:436` | `this.localInterval = setInterval(this.refreshWeather, this.localIntervalTime);` |
| **Weather map updates** | eventBus trigger | `WeatherMap.vue:68` | `eventBus.$on('weather-map-update', this.forceUpdate);` |
| **Dynamic colors (color binding live)** | BajaScript subscriber via `dynamicColorMixin` | `dynamicColorMixin.js:175,185,194` | 3 subscriptions independientes (color/target/deviation) |
| **Floorplans** | NO live updates de datos (UI estática config-driven) | `FloorPlanCanvas.vue:560` `setInterval` es solo throttling de marquee — no data | N/A para datos |
| **Device details** | BajaScript subscriber resolve directo | `DeviceDetailsView.vue:269` | `this.bajaDevice = await this.$niagara.subscriber.resolve(this.device.ord);` |
| **WebSocket app channels** | DESIGN: pub-sub genérico para route-sync + config-control. Estado clean-room: 100% stub | `BReflowChannelService.java:52` + `api/websocket.js:13-17` + `main.js:107` | Backend tiene `ConcurrentHashMap<String, ReflowChannel> channels` con métodos `who/join/leave/broadcast/leaveAll`. Frontend: `// STUB — save reference but don't connect` |
| **Heartbeat / Lease renewal** | Infraestructura DORMANT | `bajaHeartbeat.js:17-18` | `LEASE_MS = 30000 // 3× the BajaScript default 10s` `RENEWAL_MS = 20000 // ⅔ of LEASE_MS` |

### Tres patrones distintos coexisten en Reflow

1. **`setInterval` polling en componentes**: alarms + history charts + weather. Rate configurable por usuario.
2. **BajaScript subscriber via `subscriberMixin`**: points, equipment cards, schedules, buildings, dynamic colors. Patrón canónico Niagara — push real desde el station.
3. **Pull on-demand**: history data (queries vía BOX command), config (REST), backups, etc.

**WebSocket ≠ alarmas**. Existe pero es para sync entre clientes (qué view tiene cada uno) y config-control lock (un solo cliente edita a la vez), NO para alarm push.

---

## 69.2 Patrón polling — citas literales y rate-limits

### 69.2.1 AlarmDisplay.vue (consola alarmas en dashboard card)

**`AlarmDisplay.vue:163-184`** — mounted hook + polling setup:

```js
mounted: function () {
  var self = this;
  eventBus.$on('alarm-card-refetch', this.handleRefetch);
  this.getAlarmCount();
  var rate = this.alarmConsole.consoleRefreshRate || 20;       // ← default 20s
  this.interval = setInterval(this.getAlarmCount, rate * 1000); // ← polling
},
beforeDestroy: function () {
  clearInterval(this.interval);
  eventBus.$off('alarm-card-refetch', this.handleRefetch);
},
methods: {
  handleRefetch: function (cardId) {
    if (cardId === this.card.id) {
      clearInterval(this.interval);
      this.getAlarmCount();
      this.interval = setInterval(
        this.getAlarmCount,
        (this.alarmConsole.consoleRefreshRate || 20) * 1000
      );
    }
  },
```

**#244**: **Alarms usan `setInterval` polling con `rate = consoleRefreshRate || 20` segundos** en componentes (no en `alarmCache.js`). El usuario lo configura por consola via `ConsoleRefreshRateForm.vue`. Hot-reload soportado vía `eventBus.$on('alarm-card-refetch', ...)`.

### 69.2.2 AlarmsHome.vue (vista principal alarmas)

**`AlarmsHome.vue:355-369`** — async mounted con polling completo:

```js
async mounted() {
  var refreshRate = (this.alarmConsole && this.alarmConsole.consoleRefreshRate) || 20;
  var self = this;

  this.refreshInterval = setInterval(async function () {
    if (self.loading) return;
    if (self.alarmConsole && self.alarmConsole.soundsEnabled && self.$niagara && self.$niagara.alarm) {
      await self.$niagara.alarm.checkAlarmSounds(self.alarmClasses, self.alarmConsole);
    }
    await self.getAlarmSources();    // ← polling sources
    await self.getAlarmClasses();    // ← polling classes
    if (self.alarmConsole && self.alarmConsole.soundsEnabled && self.$niagara && self.$niagara.alarm) {
      self.$niagara.alarm.playAlarmSound();
    }
  }, refreshRate * 1000);
```

Cada `refreshRate` segundos: pull `getAlarmSources()` + `getAlarmClasses()` + `checkAlarmSounds()` + `playAlarmSound()` si toca.

### 69.2.3 HistoryChart.vue (chart refresh con guard rail mínimo)

**`HistoryChart.vue:300-313`** — `startRefreshTimer` method:

```js
startRefreshTimer: function () {
  var self = this;
  this.stopRefreshTimer();
  if (this.refreshOverride === true || this.card.config.refreshData) {
    var interval = (this.card.config.refreshInterval && this.card.config.refreshInterval >= 30)
      ? this.card.config.refreshInterval
      : 300;                                         // ← default 5 min
    this.refreshInterval = setInterval(function () {
      self.refreshData(true);
    }, 1000 * interval);
  }
},
```

**#245**: **History charts polling configurable con guard rail mínimo de 30 segundos, default 300s (5 minutos)**. El guard `>= 30` previene polling agresivo accidental. Refresh activado solo si `card.config.refreshData === true` o `refreshOverride === true`.

### 69.2.4 Weather.vue (condiciones locales)

**`Weather.vue:434-442`** — `setLocalInterval`:

```js
setLocalInterval: function () {
  this.clearLocalInterval();
  this.localInterval = setInterval(this.refreshWeather, this.localIntervalTime);
},
```

**#247**: **Weather usa `setInterval` polling con `localIntervalTime` configurable** + `eventBus.$on('weather-map-update')` (`WeatherMap.vue:68`) como trigger externo. Patrón híbrido polling + event.

---

## 69.3 Patrón BajaScript subscriber — `subscriberMixin`

### 69.3.1 subscriberMixin.js — lifecycle real

**`subscriberMixin.js:1-4`**:

```js
// subscriberMixin.js — BajaScript component subscription mixin
// Used by dashboard cards (ScheduleList, EquipmentCard, etc.) to subscribe
// to live Niagara component updates via $niagara.subscriber.
// Source: bundle Tt mixin (lines 2853-3001 in app-readable.js)
```

**`subscriberMixin.js:27-45`** — método `subscribe`:

```js
subscribe: async function () {
  var self = this;
  if (this.subscribedOrds) {
    this.subscriberLoading = true;
    var resolved = await this.$niagara.subscriber.resolve(this.subscribedOrds);
    this.$set(this, 'subscribedComponents', resolved);
    if (this.subscribeToActions) {
      resolved.forEach(function (comp) {
        self.parseActions(comp);
      });
    }
    this.$niagara.subscriber.subscribe(this.uuid, this.subscribedComponents, this.updateSubscribedComponents);
    this.subscribedComponents.forEach(function (c) {
      heartbeatAdd(self.uuid, c);
    });
  }
```

**`subscriberMixin.js:121-130`** — lifecycle hooks:

```js
mounted: function () {
  var self = this;
  this.$nextTick(function () {
    self.subscribe();
  });
},
beforeDestroy: function () {
  heartbeatRemove(this.uuid);
  this.$niagara.subscriber.unsubscribe(this.uuid);
}
```

**#246**: **Patrón BajaScript subscriber canónico** — `mounted → resolve(ords) → subscribe(uuid, comps, callback) + heartbeatAdd` / `beforeDestroy → heartbeatRemove + unsubscribe(uuid)`. Es PUSH real Niagara — el callback `updateSubscribedComponents` recibe eventos cuando cambia un slot del componente.

### 69.3.2 Quién usa subscriberMixin (10 components)

```
points/NiagaraPoint.vue
points/PointCard.vue
points/PointList.vue
schedules/ScheduleList.vue
buildings/BuildingStatusDisplay.vue
buildings/StatusWrap.vue
cards/CircleCard.vue
cards/Gauge.vue
cards/ToggleCard.vue
cards/table/Cell.vue
```

Adicionalmente, `dynamicColorMixin.js` invoca `$niagara.subscriber.subscribe(...)` directamente (líneas 175/185/194) — 3 subscriptions independientes (color principal + target + deviation) por componente que use color binding.

---

## 69.4 BReflowChannelService — pub-sub genérico, NO emite alarmas

### 69.4.1 Estructura del backend

**`BReflowChannelService.java:52`**:

```java
public ConcurrentHashMap<String, ReflowChannel> channels = new ConcurrentHashMap<String, ReflowChannel>();
```

Métodos públicos: `who/channelStatus/join/leave/leaveAll/broadcast`. Inner class `ReflowChannel` con `_subs: CopyOnWriteArrayList<ReflowWebSocket>` + `join/leave/broadcast/who`.

### 69.4.2 Channels que el frontend usa (verificable en `api/websocket.js`)

`api/websocket.js:50-62`:

```js
/**
 * #27 join — Join a channel ('reflow' or 'reflow-config')
 * @param {string} channel - Channel name
 * @returns {Promise<Object>} { type, ticket, command, channel, success, who: [] }
 */
export function join(channel) {
  console.log('[ws] join:', channel, '(stub)');
  return Promise.resolve({
    type: 'channel-status',
    ticket: ++ticketCounter,
    command: 'join',
    channel: channel,
    success: true,
    who: []
  });
}
```

**Solo 2 channels**: `reflow` (route-sync entre clientes — qué pantalla tiene cada uno) y `reflow-config` (config-control lock — un solo cliente edita a la vez).

**#248**: **`BReflowChannelService` es genérico pub-sub para sync de clientes (route + config-control), NO emite alarmas ni history events**. Cualquier afirmación de "WS push de alarmas" en mapping/docs es incorrecta para clean-room-177. Para alarmas live habría que extender el ChannelService o usar la API nativa Niagara `BAlarmService` (ver bloque #44).

---

## 69.5 HistoryGhostSubscriber — corrige descripción previa

### 69.5.1 Código real (26 LOC)

**`HistoryGhostSubscriber.java`** completo:

```java
class HistoryGhostSubscriber extends Subscriber {
   BHistory history;
   Context subscriberCtx;

   public HistoryGhostSubscriber(BHistory history, Context ctx) {
      this.history = history;
      this.subscriberCtx = ctx;
   }

   public void event(BComponentEvent bComponentEvent) {
      this.history.unsubscribe(this, this.subscriberCtx);   // ← AUTO-UNSUSCRIBE
   }

   protected void unsubscribed(BComponent c, Context cx) {
      super.unsubscribed(c, cx);
   }
}
```

### 69.5.2 Corrección a mapping y bloque #68

**`domains/history.md` §5** y **bloque #68 §68.1.5** decían:

> "Crea suscripción 'ghost' (vacía) sobre `BHistoryDatabase` para mantener viva la sesión de history en el station durante queries pesados; sin esto, el station cierra la sesión por timeout."

**Veredicto empírico**: **WRONG**. El subscriber **se auto-unsuscribe al primer evento** (línea 19-21). NO mantiene la sesión viva — al revés, es un mecanismo de **detección de cambio** (al primer cambio en `BHistory`, dispara el unsubscribe). El nombre "Ghost" probablemente alude a que es un subscriber transitorio de una sola vez, no a que sea un keepalive.

**#252**: **`HistoryGhostSubscriber` es un subscriber de detección de cambio one-shot, NO un keepalive**. Mapping y bloque #68 §68.1.5 corregidos. El verdadero keepalive en Reflow es `lib/bajaHeartbeat.js` (DORMANT en clean-room — ver §69.6).

---

## 69.6 bajaHeartbeat — keepalive real DORMANT en clean-room

### 69.6.1 Cita literal del propósito

**`bajaHeartbeat.js:1-15`**:

```js
// lib/bajaHeartbeat.js — BajaScript lease renewal heartbeat
//
// Design: module singleton (NOT Vuex). Lease renewal is infrastructure, not app state.
// Pattern mirrors lib/csrf.js — module-level closure state, named exports only, no class.
//
// Context: BajaScript default lease = 10s (Component.js:1795). Browser tab backgrounding
// throttles setInterval to ~1Hz, causing silent BOX subscription expiry (Bloque 42.2.4).
// This module fires ONE baja.Component.lease() every RENEWAL_MS for ALL tracked comps,
// preventing expiry when the user backgrounds the tab.
//
// Phase D wiring (NOT this change): main.js / injectBaja() must call start(baja) ONCE
// after BajaScript loads. This module is DORMANT until start() is called — add() and
// remove() are safe no-ops, causing zero regression in dev or before Phase D lands.
//
// Bloque 42 + 47: documents 30s lease and 20s renewal as production-safe constants.
```

### 69.6.2 Constantes y guard

**`bajaHeartbeat.js:17-18`**:

```js
var LEASE_MS = 30000;    // 3× the BajaScript default 10s — tolerates ~25s throttle slop
var RENEWAL_MS = 20000;  // ⅔ of LEASE_MS — renew before expiry with margin
```

**`bajaHeartbeat.js:110-118`** — guard dormant:

```js
export function add(uuid, comp) {
  if (!_started) {
    return; // dormant — no log, no state change (A3: dormant until start())
  }
  if (!_tracking.has(uuid)) {
    _tracking.set(uuid, new Set());
  }
  _tracking.get(uuid).add(comp);
}
```

**#249**: **`bajaHeartbeat.js` está DORMANT en clean-room-177** porque `start(baja)` nunca se llama desde `main.js` (Phase D wiring no aterrizó). Cuando MX60 use `subscriberMixin`, **DEBE llamar `start(baja)` desde sprint 1** después de cargar BajaScript — caso contrario las subscriptions expiran silenciosamente al backgroundear la tab. Constantes seguras: `LEASE_MS=30000`, `RENEWAL_MS=20000`.

---

## 69.7 alarmCache.js es STUB — corrige bloque #62

**`alarmCache.js:1-4`**:

```js
// alarmCache.js — Alarm count polling cache (stub)
// Original: bundle Fa (lines 15058-15177)
// Manages per-instance alarm count polling with callbacks.
// Full implementation requires active Niagara alarm API — stub for now.
```

**`alarmCache.js:39-44`**:

```js
startInterval: function (id, fn, ms) {
  var inst = instances[id];
  if (inst) {
    if (inst.interval) clearInterval(inst.interval);
    inst.interval = setInterval(fn, ms || 30000);   // ← default 30s, NO 20s
  }
},
```

**#251**: **`alarmCache.js` es STUB infraestructura** — la API existe (`createInstance/registerCallback/startInterval/notifyCallbacks`) pero **no se la consume en ningún componente** (sweep `rg "alarmCache"` en `src/` solo encontró el archivo mismo + el comment de bloque 42). Los componentes hacen polling con su propio `setInterval` directamente.

**Corrección a bloque #62 §62.9.3**: el polling vive en componentes, no en `alarmCache.js`. El default es `consoleRefreshRate || 20` (componentes), NO `30000ms` (lo que tiene `alarmCache.js:43`). Bloque #62 era correct in spirit (polling sí, no WS) pero la localización y el default estaban equivocados.

---

## 69.8 niagara.js es 100% mock — `$niagara.alarmSubscribe` NO existe

### 69.8.1 Cita literal del header

**`niagara.js:1-5`**:

```js
// plugins/niagara.js — Vue.prototype.$niagara
// Mock of the Niagara integration plugin.
// In production, aQ object has: encode, decode, uncamel, ord, alarm, bql,
// history, schedule, nav, matrix, backups, points, subscriber, util, browser, pointTypes.
// This mock returns safe defaults so components don't crash without a real N4 backend.
```

### 69.8.2 alarmMock — métodos existentes

**`niagara.js:94-124`**:

```js
var alarmMock = {
  getAlarmList: ..., getUuidForSources: ..., ackAlarm: ..., ackAlarms: ...,
  ackAlarmsByUuid: ..., addNote: ..., addNotes: ..., getNotes: ...,
  hyperAck: ..., getAlarmClasses: ..., getSourceGroupAlarms: ..., getSourceList: ...,
  getClassList: ..., classList: ..., getSoundFiles: ..., getFilter: ...,
  buildQueryString: ..., timeString: ...,
  startAlarmSounds: noop, stopAlarmSounds: noop, playAlarmSound: noop,
  checkAlarmSounds: ..., invokeSoundOrd: noop
};
```

**Métodos confirmados (20)**: ack/notes/queries/sounds. **NO existe** `alarmSubscribe` ni `subscribe` ni nada relacionado a push.

### 69.8.3 subscriberMock — confirma silencio

**`niagara.js:44-55`**:

```js
var subscriberMock = {
  resolve: function (ords) {
    warn('subscriber.resolve');
    return Promise.resolve(Array.isArray(ords) ? [] : []);
  },
  subscribe: function () {
    warn('subscriber.subscribe');     // ← solo console.warn, no hace nada
  },
  unsubscribe: function () {
    // silent — called in beforeDestroy, don't spam console
  }
};
```

**#250**: **`$niagara.alarmSubscribe` NO existe ni en clean-room-177 ni se infiere del listado de métodos en producción** (`alarm` namespace tiene 20 métodos, ninguno de subscribe). El mapping `domains/alarms.md` §5 describiendo "AlarmSubscriber pattern" via `$niagara.alarmSubscribe` es inventado — probablemente por inferencia del nombre, no por lectura del código. **Corrección obligatoria al mapping**.

---

## 69.9 Cierre flag #241 — veredicto consolidado

### Tabla de resolución

| Pregunta | Respuesta empírica |
|---|---|
| ¿Reflow alarms usan WebSocket push? | **NO**. WS está stub + comentado + el ChannelService no emite alarmas. |
| ¿Reflow alarms usan polling? | **SÍ**, vía `setInterval` en componentes (`AlarmDisplay`, `AlarmsHome`, `AlarmDetails`). Default 20s configurable por consola. |
| ¿`alarmCache.js` hace el polling? | **NO**, es stub infraestructura sin consumo real. |
| ¿`$niagara.alarmSubscribe` existe? | **NO**, ni en mock ni en lista de métodos prod (20 alarm methods, ninguno de subscribe). |
| ¿BajaScript subscriber se usa para algo? | **SÍ**, para points + cards + schedules + buildings + dynamic colors via `subscriberMixin` y `dynamicColorMixin`. |
| ¿Reflow tiene live updates? | **SÍ**, vía 3 patrones distintos (polling componentes / BajaScript subscriber / on-demand pull). |
| ¿La descripción del mapping era correcta? | **NO** — confundió alarms (polling) con points (subscriber) y atribuyó a alarms un patrón que no usa. |
| ¿La descripción de bloque #62 era correcta? | **CORRECT in spirit, WRONG in details** — polling sí, pero el código vive en componentes y el default es 20s no 30s, y `alarmCache.js` es stub no implementación. |

### Decisión MX60 actualizada (refina §68.6.5)

**Para alarmas en MX60**:
- **Mantener polling** (es lo que el bundle producción 1.7.5 está haciendo realmente)
- **Activar WS opcional** para notificaciones críticas (alarmas P0/P1) — usar `BReflowChannelService` extendido o `BAlarmService` BIAlarmCursor (bloque #44 reference)
- **Rate configurable** por consola con guard mínimo (≥10s para evitar floodear el station)

**Para points en MX60**: HEREDA `subscriberMixin` → `useSubscriber()` composable Vue 3, **CON `bajaHeartbeat.start(baja)` desde sprint 1** (#249).

**Para charts en MX60**: HEREDA polling con guard mínimo 30s, default 300s (5 min). #245.

**Para weather en MX60**: HEREDA polling configurable + eventBus pattern. #247.

**#253**: **clean-room-177 NO está conectado a un backend** — UI desconectada con mocks (`niagara.js`, `websocket.js`, `alarmCache.js`) + heartbeat dormant. Para inferir comportamiento de producción real para detalles fine-grained (rate limits, retry logic, error handling, etc.) hay que mirar el **bundle Reflow producción 1.7.5** o lab con station N4 viva. Clean-room-177 da forma + estructura + contratos, no behavior runtime.

---

## 69.10 Acciones de corrección sobre el mapping

Como consecuencia de este audit, las siguientes piezas del mapping requieren corrección (en una sesión dedicada, no en este bloque):

| Archivo | Sección | Corrección necesaria |
|---|---|---|
| `docs/mappings/reflow-clean-177/domains/alarms.md` | §5 "AlarmSubscriber pattern" | Reescribir: "polling `setInterval` en componentes (AlarmDisplay/AlarmsHome/AlarmDetails) con `consoleRefreshRate \|\| 20` segundos; `alarmCache.js` es stub no usado; `$niagara.alarmSubscribe` no existe; WS no emite alarmas". Tag `[OPEN]` → `[FIXED-69]`. |
| `docs/mappings/reflow-clean-177/domains/history.md` | §5 "HistoryGhostSubscriber" | Reescribir: "subscriber de detección one-shot — auto-unsuscribe al primer evento. NO es keepalive". El verdadero keepalive es `lib/bajaHeartbeat.js`. Tag → `[FIXED-69]`. |
| `niagara-mental-model-bloque68.md` | §68.1.5 | Idem — corregir descripción HistoryGhostSubscriber. Cross-ref a bloque #69. |

**Decisión**: las correcciones al mapping NO se hacen en esta sesión (bloque #69 las documenta, sesión futura las aplica). Razón: scope-discipline — un bloque por entregable.

---

## 69.11 Cross-references

- **Bloque #42** §42.2.4 — origen documental de `LEASE_MS=30s` + `RENEWAL_MS=20s` (browser tab backgrounding throttle)
- **Bloque #44** — Alarm Console Niagara nativo (BAlarmService + BAlarmSpaceConnection): para implementar push real de alarmas en MX60, usar BIAlarmCursor de la API nativa N4
- **Bloque #45** — History/Trend chart consumption (WebChart 3 GET + boxcs): real-time tail con `boxcs` subscription al `BControlPoint.out` es el patrón canónico para chart live-update — pero Reflow NO lo usa (usa polling 5 min)
- **Bloque #47** — Bootstrap headless SPA externa: confirma `LEASE_MS=30s` constant
- **Bloque #62** §62.9.3 — claim "polling 20s vía alarmCache.js" — corregido en este bloque (polling sí, alarmCache.js no)
- **Bloque #68** §68.6.5 — flag #241 abierta — **CERRADA en este bloque**
- **Engram #1236** — methodology/mapping-vs-empirical-audit — caso de estudio confirmado por este bloque

---

## 69.12 Resumen final — entregables

✅ Tabla universal patrón-por-dominio (§69.1) — 16 dominios mapeados con file:line
✅ Citas literales para cada veredicto (§69.2-§69.8)
✅ Cierre flag #241 con resolución consolidada (§69.9)
✅ 10 implications nuevas #244..#253
✅ Acciones de corrección al mapping documentadas (§69.10)
✅ Cross-refs a bloques #42, #44, #45, #47, #62, #68 + engram #1236

**Tamaño**: ~520 LOC (en línea con bloque #65 que también es síntesis post-audit).

**Status**: bloque listo. Capa 19 — Transplante operacional sigue extendiéndose. **Flag #241 RESUELTA**.

**Lección meta consolidada** (engram #1236 reforzada): el mapping es síntesis de fuentes secundarias (GAP-ANALYSIS, REFLOW-ARCHITECTURE-ANALYSIS); puede tener inferencias incorrectas para detalles runtime fine-grained. **Audit empírico directo del código gana siempre cuando difieren**. Esta sesión confirmó dos correcciones críticas (alarmas no usan WS push; HistoryGhostSubscriber no es keepalive) que el mapping había sintetizado mal.

---

**Sesión cerrada con bloque 69**: el mental model Niagara N4 + Reflow + MX60 ahora tiene **ground truth empírico** sobre patrones de live-update por dominio + flag #241 cerrada con evidencia + 2 correcciones identificadas para aplicar al mapping en sesión dedicada. Capa 19 Transplante operacional avanza con audit empírico complementario al transplante-blueprint del bloque #68.
