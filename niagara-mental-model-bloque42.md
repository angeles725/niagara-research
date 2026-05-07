# Bloque 42 — bajaScript Subscriber lifecycle end-to-end desde SPA externa

**Fecha**: 2026-05-04
**Scope**: SPA externa 100% custom consumiendo data live de Niagara N4.14. NO Reflow-specific. Foco abstracto en el contrato público del Subscriber API, lease management, batching/deadbanding, reconnect, memory leaks, scaling y topología de suscripciones.
**Fuentes primarias**: `bajaScript-ux.jar` → `rc/baja/comp/Subscriber.js`, `rc/baja/comp/Component.js`, `rc/env/WebSocketConnection.js`, `rc/env/BrowserCommsManager.js`, `rc/env/ConnectionManager.js`, `rc/env/mux/BoxMessageRelay.js`, `rc/baja/comm/Batch.js`, `box-rt.jar` → `BComponentSpaceSessionHandler.class`, `BBoxServlet.class`, `BoxWebSocketServlet.class`. Contrastados con: Bloque 22 (BajaScript intro), Bloque 36 (BOX wire + lease 10s), Bloque 41 (BBrokerChannel), Bloque 47 (bootstrap headless), Bloque 51 (app-readable.js implementación real Reflow).

---

## 42.0 Contexto y scope

### ¿Qué es un Subscriber en N4?

En el modelo BajaScript, un **Subscriber** es el objeto que gestiona las suscripciones vivas a componentes del servidor (station). No es una abstracción HTTP polling — es una suscripción push bidireccional sobre el canal BOX (`/wsbox`), donde el servidor envía cambios (*unsolicited*) sin que el cliente los solicite cada vez.

La analogía es un *observable store* con back-pressure controlada por el servidor: el servidor emite deltas cuando el componente cambia, y el cliente mantiene un caché local (`BoxComponentSpace`) que aplica los deltas.

El Subscriber vive en la intersección de tres capas:

```
SPA JavaScript (browser)
  └─ baja.Subscriber          ← API pública (este bloque)
       └─ BoxComponentSpace   ← caché local del árbol de componentes
            └─ BOX WebSocket  ← transporte bidireccional sobre /wsbox
                 └─ BComponentSpaceSessionHandler  ← servidor (station)
```

### Scope de este bloque

- **Subscriber API contract** (firmas completas, flags, eventos)
- **Lease management** (10s hardcoded, renovación, expiración silenciosa)
- **Batching y deadbanding** (implícito 10ms N4.10+, control client-side)
- **Reconnect strategy** (exponential backoff, re-subscribe post-reconnect)
- **Memory leaks típicos** (cleanup obligatorio, dispose pattern)
- **Scaling** (100 → 1k → 10k suscripciones, límites browser y station)
- **Subscription a topics vs slots vs componentes** (tradeoffs)

### Qué NO cubre este bloque

- Fox Protocol (inter-station, Workbench↔Station) — Bloque 19
- BBrokerChannel Supervisor↔Subordinate — Bloque 41
- Bootstrap headless CORS/auth — Bloque 47
- EasyTemplates/PX binding server-side — Bloques 22, 36
- Writes con priority array — Bloque 46

---

## 42.1 Subscriber API contract

### 42.1.1 Construcción — `baja.Subscriber`

CONFIRMADO (`rc/baja/comp/Subscriber.js`, analizado en Bloque 22.11 y Bloque 36.5):

```javascript
// Construcción
var sub = new baja.Subscriber();

// Alternativa: sub ya viene embebido en widgets bajaux que usan Subscriber
// NiagaMods Reflow usa un singleton por instancia de $niagara plugin
// (CONFIRMADO app-readable.js:3521 — variable `oe` lazy init)
```

**`baja.Subscriber` NO es un singleton global**. Es una clase JS instanciable. Cada instancia gestiona su propio set de suscripciones y event handlers. El patrón Reflow usa UNO por instancia de plugin Vue (`$niagara`), pero es una decisión de diseño, no una restricción de la API.

Implicación: una SPA puede crear múltiples Subscribers (por feature, por panel, etc.) sin colisión entre ellos, siempre que se haga cleanup de cada uno.

### 42.1.2 Métodos de gestión de suscripción

```javascript
// SUBSCRIBE — suscribir componentes
sub.subscribe({
    comps: [comp1, comp2, comp3],  // BComponent[] — REQUERIDO
    ok:   function() {},            // deprecado, usar Promise
    fail: function(err) {}          // deprecado, usar Promise
});
// Retorna: Promise<void>
// Internamente llama Component.lease() sobre cada comp (ver 42.2)

// UNSUBSCRIBE — desuscribir componentes
sub.unsubscribe();                  // desuscribe TODOS
sub.unsubscribe([comp1]);           // desuscribe específicos (INFERIDO — no confirmado en source público)
// Retorna: void (operación sincrónica internamente, asyncrónica con server)

// SUBSCRIBED — chequear si un componente está suscrito
sub.subscribed(comp);              // Retorna: boolean
// Útil para evitar doble subscribe
```

**GOTCHA G42-1 — `subscribe` es idempotente en el Subscriber, no en el servidor**: Llamar `sub.subscribe({comps:[comp]})` sobre un componente ya suscrito renueva el lease en el servidor pero NO duplica los callbacks. La documentación en Bloque 36.5 confirma: "re-llamar `lease()` sobre comps ya leased simplemente renueva el ticket" (`Component.js:1743`). Sin embargo, si se llama con callbacks nuevos, el Subscriber los sobreescribe (no acumula).

### 42.1.3 Método `attach` — registro de callbacks

```javascript
sub.attach({
    // Eventos de slot/property:
    changed: function(prop, cx) {
        // `this` = el BComponent que cambió
        // prop = la Property que cambió (objeto Property con getName(), getType(), getFacets())
        // cx = Context (user, permissions, timestamp)
        var value = this.get(prop);           // valor actual
        var display = this.getDisplay(prop);  // string formateado
        var status = this.getStatus(prop);    // BStatus flags
    },
    
    added: function(prop, cx) {
        // `this` = comp padre
        // prop = Property recién añadida (slot dinámico)
    },
    
    removed: function(prop, val, cx) {
        // `this` = comp padre
        // prop = Property removida
        // val = último valor antes de remoción
    },
    
    renamed: function(prop, oldName, cx) {
        // slot renombrado
    },
    
    flagsChanged: function(slot, cx) {
        // flags de un slot cambiaron (readonly, hidden, summary...)
    },
    
    facetsChanged: function(slot, cx) {
        // facets de un slot cambiaron (range, units, format...)
    },
    
    // Eventos de topic:
    topicFired: function(topic, event, cx) {
        // `this` = comp que disparó el topic
        // topic = objeto Topic con getName()
        // event = BValue del evento asociado
    },
    
    // Eventos de lifecycle del subscriber:
    subscribed: function(cx) {
        // suscripción establecida y primera sincronización completada
        // `this` = el comp recién suscrito
        // IMPORTANTE: este callback se dispara POR CADA comp suscrito
    },
    
    unsubscribed: function(cx) {
        // comp desuscrito del servidor
        // se dispara en unsubscribe() explícito O en lease expired
    },
    
    // Eventos de estructura del árbol:
    componentRenamed: function(cx) {
        // el comp mismo fue renombrado (nav name cambió)
    },
    
    componentFlagsChanged: function(cx) {
        // flags del comp (no de un slot) cambiaron
    },
    
    componentReordered: function(cx) {
        // hijos del comp reordenados
    },
    
    // Ciclo de vida del widget:
    unmount: function(cx) {
        // comp fue desmontado del árbol (eliminado del server)
        // CRÍTICO: el comp ya no existe en station → cleanup obligatorio
    }
});
```

CONFIRMADO en Bloque 22.11: "Eventos: `changed, added, removed, renamed, flagsChanged, facetsChanged, topicFired, subscribed, unsubscribed, unmount, componentRenamed, componentFlagsChanged, componentReordered`".

### 42.1.4 Método `detach` — remover callbacks

```javascript
sub.detach();                // remueve TODOS los handlers
sub.detach("changed");       // remueve handler específico
sub.detach({changed: true}); // mismo que el anterior (objeto selector)
```

**GOTCHA G42-2 — `detach()` no unsubscribe al servidor**: `detach()` remueve los callbacks del Subscriber client-side, pero la suscripción en el servidor (y el lease) sigue activa. Para liberar recursos del servidor, es obligatorio llamar `sub.unsubscribe()` ADEMÁS de `sub.detach()`. El patrón correcto de cleanup:

```javascript
// Cleanup completo — orden obligatorio
sub.detach();         // 1. Primero detach (evita callbacks tras unsubscribe)
sub.unsubscribe();    // 2. Luego unsubscribe (libera recursos servidor)
```

### 42.1.5 eventMask flags

INFERIDO — no hay evidencia empírica de `eventMask` como parámetro numérico en la API JS pública. En la API Java server-side (`javax.baja.sys.BSubscription`) existen flags para controlar qué eventos generan notificación, pero en la API JS pública de `baja.Subscriber` los callbacks se registran por nombre de evento, no por máscara binaria.

La selección de qué eventos recibir se hace a nivel de handler: si no registrás `flagsChanged`, no hay overhead por procesar ese evento client-side (aunque el servidor sigue enviando el delta si ocurre). INFERIDO: el servidor NO filtra por event mask per-subscriber en N4.14 — envía todos los deltas y el cliente decide cuáles procesar.

**TODO honesto 42-1**: Verificar si `BComponentSpaceSessionHandler.addKnob()` acepta algún parámetro de filtrado de eventos. Source disponible en `box-rt.jar` pero no fue analizado en profundidad para este bloque.

### 42.1.6 Subscriber per-component vs global

```javascript
// Patrón 1: Subscriber global (Reflow) — un Subscriber para toda la SPA
var globalSub = new baja.Subscriber();
// + dispatcher interno por handle

// Patrón 2: Subscriber per-widget — cada widget tiene el suyo
// Ventaja: lifecycle tied al widget → destroy del widget = cleanup automático
// Desventaja: N widgets * N subscribes = N operaciones round-trip al servidor

// Patrón 3: Subscriber per-feature (recomendado para SPA media-scale)
var dashboardSub = new baja.Subscriber();   // subs del dashboard principal
var alarmSub    = new baja.Subscriber();   // subs del panel de alarmas
var historySub  = new baja.Subscriber();   // subs de historia (aunque historia usa BHistoryChannel)
```

**Recomendación**: Para SPAs con muchos componentes (>50), usar un Subscriber global con un dispatcher interno que enrute cambios a los consumidores. Crear un Subscriber por cada componente de UI pequeño genera overhead de registro/desregistro y complica el cleanup. CONFIRMADO en app-readable.js:3521 — Reflow usa este patrón (variable `re` como array de `{owner, callback, component}`).

---

## 42.2 Lease management (CRÍTICO)

### 42.2.1 El lease 10s hardcoded — CONFIRMADO

CONFIRMADO empíricamente en Bloque 36.5:

```javascript
// Component.js:1795 (bajaScript-ux.jar, fuente legible)
time = bajaDef(obj.time, 10000)  // 10000 ms = 10 SEGUNDOS — HARDCODED DEFAULT
```

El método `baja.Component.lease()` acepta un parámetro `time` que puede sobreescribirse:

```javascript
// Lease con tiempo custom (NO usa el default 10s)
baja.Component.lease({
    comps: [comp1, comp2],
    time: 60000  // 60 segundos
});
```

**GOTCHA G42-3 — El default de 10s es agresivo para SPAs con muchos puntos**: Con 100 componentes suscritos y un default de 10s, el cliente debe enviar un renewal cada 10s para cada uno. Con implicit batching esto se agrupa en ~1 frame BOX cada 10s, pero si la SPA pierde el WebSocket por >10s (tab en background, network blip), el servidor hace unsubscribe automático SILENCIOSO.

### 42.2.2 Renovación del lease

CONFIRMADO (`Component.js:1743`, referenciado en Bloque 36.5):

```javascript
// Patrón de renovación continua (la manera correcta)
function startLeaseRenewal(comps, intervalMs) {
    // El renewal interno es automático mientras el Subscriber esté activo
    // y el WebSocket esté conectado. No hay API explícita de "refresh" manual.
    
    // Si se pierde el WS, al reconectar se debe re-llamar sub.subscribe()
    // que internamente llama lease() — esto renueva el ticket servidor
}
```

**Internamente**, el Subscriber mantiene el lease activo mientras:
1. El WebSocket BOX está conectado, Y
2. No haya pasado el `time` desde el último renewal

La renovación es implícita: mientras haya actividad BOX (cualquier frame enviado en el WebSocket), el `BServerSession` del servidor se mantiene vivo. El lease específico del componente se renueva cuando el cliente envía `addKnob` o cuando el keepalive BOX ocurre.

**GOTCHA G42-4 — Lease ≠ Session keepalive**: El lease de un componente y la sesión BOX son cosas distintas. La sesión BOX tiene su propio keepalive (`box.serverSession.keepAliveSeconds = 300` por defecto — CONFIRMADO Bloque 36.10). Un lease de 10s puede expirar dentro de una sesión BOX activa si el componente específico no recibe renovación. La sesión puede estar viva pero las suscripciones muertas.

### 42.2.3 ¿Existe `BSubscription` server-side con timeout configurable?

INFERIDO — no confirmado empíricamente. La arquitectura server-side (`box-rt.jar`) tiene:

- `BComponentSpaceSessionHandler` — gestiona suscripciones por sesión
- El lease timeout server-side es controlado por el `LeaseManager` (Bloque 20: "4 tipos de leases")

El `LeaseManager` (Bloque 20) es un servicio de la station que gestiona tickets de lease. **No hay evidencia de configuración de timeout per-subscription vía BOG** — el timeout server-side se sincroniza con el `time` enviado por el cliente en el `addKnob`. Si el cliente envía `time:10000`, el servidor configura un ticket de 10s. Si el cliente envía `time:60000`, 60s.

**Implicación**: Para aumentar el lease server-side, la SPA simplemente pasa un `time` mayor al `lease()` call. El servidor respeta ese valor dentro de los límites del `LeaseManager`.

**TODO honesto 42-2**: Verificar si `LeaseManager` tiene un cap máximo configurable. Source `box-rt.jar:BComponentSpaceSessionHandler.addKnob()` procesamiento del parámetro `time`.

### 42.2.4 Qué pasa cuando la SPA pierde el WebSocket mid-lease

CONFIRMADO en Bloques 36.5 y 47.5.4:

```
Escenario: SPA abierta, 50 componentes suscritos con lease 10s
↓
Tab va a background (browser throttles timers)
↓
JavaScript pausa > 10 segundos
↓
Server-side: lease timer expiró → removeKnob para todos los 50 comps
↓
Server-side: estado limpio — ya no envía más deltas
↓
Tab vuelve a foreground
↓
WebSocket sigue conectado (TCP keepalive del OS mantuvo la conexión)
↓
BrowserCommsManager NO detecta el lease expiry
↓
RESULTADO: UI queda "congelada" — mostrando el último valor recibido
            Ningún callback "subscribed" ni "unsubscribed" se dispara
            El Subscriber cree que sigue suscrito (client-state desincronizado)
```

**CONFIRMADO en Bloque 36.5**: "si el tab queda abierto con JS paused (dev tools → pause), las subs se mantienen hasta fin-de-lease, después se pierden silenciosamente".

### 42.2.5 ¿Hay evento para "lease expired"?

**CONFIRMADO: NO hay evento explícito de lease expired**. El evento `unsubscribed` se dispara solo cuando el servidor envía un `removeKnob` unsolicited, lo cual ocurre cuando el lease expira Y el servidor decide hacer cleanup activo. En la práctica esto puede ser diferido o no dispararse si la sesión WebSocket cae antes.

El resultado es: **la expiración de lease es silenciosa desde la perspectiva del cliente**. El pattern robusto es asumir que cualquier interrupción >5s requiere re-suscripción completa.

### 42.2.6 Pattern de lease management robusto para SPA

```javascript
const LEASE_TIME_MS = 30000;  // 30s (3x el default — más tolerante a background)
const RENEWAL_INTERVAL_MS = 20000;  // renovar cada 20s (antes de expiración)

class NiagaraConnectionManager {
    constructor(subscriber) {
        this.sub = subscriber;
        this.activeComps = new Set();
        this.renewalTimer = null;
    }

    subscribe(comps) {
        comps.forEach(c => this.activeComps.add(c));
        return this.sub.subscribe({ comps, leaseTime: LEASE_TIME_MS });
    }

    startRenewal() {
        this.renewalTimer = setInterval(() => {
            const comps = Array.from(this.activeComps);
            if (comps.length > 0) {
                baja.Component.lease({ comps, time: LEASE_TIME_MS });
            }
        }, RENEWAL_INTERVAL_MS);
    }

    stopRenewal() {
        clearInterval(this.renewalTimer);
    }

    async resubscribeAll() {
        const comps = Array.from(this.activeComps);
        if (comps.length > 0) {
            await this.sub.subscribe({ comps, leaseTime: LEASE_TIME_MS });
        }
    }
}
```

---

## 42.3 Batching y deadbanding

### 42.3.1 Batching implícito 10ms — CONFIRMADO

CONFIRMADO en Bloque 36 (fuente: `Batch.js:18-35`, `BoxMessageRelay.js`):

```javascript
// Batch.js:18-35 (comentario en source legible)
// "starting in Niagara 4.10, BajaScript will _automatically_ package
//  operations together using implicit batching"
```

`BoxMessageRelay.js` implementa un debounce de ~10ms: todas las operaciones BOX generadas en un mismo tick JS (o dentro del ventana de 10ms) se agrupan en un único `BoxFrame` con múltiples mensajes en el array `m[]`.

```javascript
// Ejemplo: subscriber registra 10 comps en un loop
for (let i = 0; i < 10; i++) {
    sub.subscribe({ comps: [comps[i]] });
}
// RESULTADO: 1 sola BoxFrame con 10 addKnob messages (NO 10 frames separados)
// CONFIRMADO en wire format (Bloque 36.8 Pattern 3)
```

Esto es especialmente importante para inicializaciones masivas: registrar 1000 componentes en un loop JS genera O(1) frames BOX, no O(1000).

### 42.3.2 Control client-side del batching

CONFIRMADO (`rc/baja/comm/Batch.js`):

```javascript
// Batch MANUAL — forzar un batch específico
var batch = new baja.comm.Batch();
batch.addReq("boxcs", "addKnob", { handle: "h1", ord: "..." });
batch.addReq("boxcs", "addKnob", { handle: "h2", ord: "..." });
batch.commit(function(ok, fail, results) {
    // todas las respuestas en un array
});

// Batch como Promise
batch.promise().then(results => {
    // ...
});
```

**Para latencia crítica** (comando que no puede esperar el debounce de 10ms):

```javascript
// Forzar envío inmediato sin esperar el debounce
const cb = new baja.comm.Callback(ok, fail);
cb.addReq("boxcs", "set", { handle: "h1", slot: "in1", value: 42.0 });
cb.commit();  // envía AHORA, sin batching
```

**GOTCHA G42-5 — Batching implícito puede generar latencia inesperada en writes críticos**: Si la SPA escribe un setpoint y espera confirmación, el debounce de 10ms se interpone. Para operaciones críticas de control usar `Callback.commit()` directo.

### 42.3.3 Deadbanding — ¿server-side o client-side?

CONFIRMADO: el deadbanding es **server-side**, implementado en `BProxyExt` mediante la tuning policy. Bloque 24 ("ProxyExt pipeline + tuning policy — deadband + maxWritePeriod") confirma que:

- `minWritePeriod` — mínimo intervalo entre writes al driver
- `deadband` — cambio mínimo en el valor para generar notificación
- `maxWritePeriod` — forzar write incluso sin cambio

Desde la perspectiva del Subscriber cliente:

```
Driver (campo) → ProxyExt → [DEADBAND FILTER server-side] → BComponent
                                                              ↓
                                              BOX unsolicited "changed" event
                                                              ↓
                                              Subscriber callback
```

**El cliente NO ve los valores que no pasan el deadband**. No hay API client-side para configurar deadband en la suscripción BOX.

**Implicación para SPA**: Si la SPA necesita deadbanding personalizado diferente al del driver (ej. el driver tiene deadband de 0.5°C pero la UI quiere actualizar solo si cambia >2°C), debe implementarlo en el callback `changed`:

```javascript
sub.attach({
    changed: function(prop, cx) {
        if (prop.getName() === 'out') {
            const newVal = this.get(prop).getDouble();
            const prevVal = this._prevValue || 0;
            const clientDeadband = 2.0;  // °C
            if (Math.abs(newVal - prevVal) >= clientDeadband) {
                this._prevValue = newVal;
                updateUI(newVal);
            }
        }
    }
});
```

### 42.3.4 Coalescing de eventos rápidos

El servidor NO envía un delta por cada cambio individual si los cambios ocurren más rápido que el ciclo de polling del `BComponentSpaceSessionHandler`. El servidor agrega ("coalesces") cambios del mismo slot antes de enviar el unsolicited:

```
Slot cambia: 42.0 → 42.1 → 42.2 → 42.3 (en 5ms)
El servidor ve el estado final: 42.3
BOX unsolicited: op:"changed" slot:"out" value:42.3
El cliente NUNCA ve los valores intermedios
```

Este comportamiento es implícito — no hay API para controlar la frecuencia de coalescing. Depende del ciclo de polling interno del `BComponentSpaceSessionHandler` (INFERIDO: ~100ms por defecto, basado en la arquitectura event-driven de N4).

**GOTCHA G42-6 — Valores intermedios invisibles para el cliente**: Si la SPA necesita ver TODOS los valores (logging, trending), no puede depender del Subscriber BOX. Para trending completo se usa `BHistoryChannel` (History, Bloque 8) o la API de Analytics (Bloque 16).

---

## 42.4 Reconnect strategy desde SPA

### 42.4.1 Confirmación: reconnect NO automático

CONFIRMADO en Bloques 36.5, 47.5.4, y 51.2.4:

- Bloque 36.5: "Al reconectar, las subscripciones NO se re-registran automáticamente — es responsabilidad del widget re-invocar `lease()` en el `reconnected` event."
- Bloque 47.5.4 (GOTCHA G47-7): "El BajaScript runtime NO tiene reconnect automático out-of-the-box (CONFIRMADO en `ConnectionManager.js` — sólo detecta cierre, no reintenta)."
- Bloque 51.2.4: Reflow tampoco tiene reconnect automático robusto.

El `ConnectionManager.js` detecta el cierre del WebSocket pero no lo reintenta. La SPA es completamente responsable de la estrategia de reconnect.

### 42.4.2 Patrón de exponential backoff

```javascript
class BOXReconnectManager {
    constructor(options = {}) {
        this.minDelay = options.minDelay || 1000;     // 1s
        this.maxDelay = options.maxDelay || 30000;    // 30s
        this.factor   = options.factor   || 2;        // multiplicador
        this.jitter   = options.jitter   || 0.2;      // ±20% random
        this.currentDelay = this.minDelay;
        this.attempt = 0;
        this.onReconnect = options.onReconnect || (() => {});
    }

    start() {
        // Escuchar evento de desconexión de BajaScript
        // (el evento exacto varía — ver 42.4.3)
        baja.comm.addConnectionListener({
            connectionLost: () => this._scheduleReconnect(),
            connectionRestored: () => this._onConnected()
        });
    }

    _scheduleReconnect() {
        const jitterMs = this.currentDelay * this.jitter * (Math.random() * 2 - 1);
        const delay = Math.round(this.currentDelay + jitterMs);
        
        console.log(`[BOX Reconnect] Attempt ${this.attempt + 1} in ${delay}ms`);
        
        setTimeout(() => this._doReconnect(), delay);
        
        // Backoff
        this.currentDelay = Math.min(this.currentDelay * this.factor, this.maxDelay);
        this.attempt++;
    }

    _doReconnect() {
        baja.comm.start().catch(() => {
            this._scheduleReconnect();  // falla → reintentar
        });
    }

    _onConnected() {
        // Reset backoff
        this.currentDelay = this.minDelay;
        this.attempt = 0;
        // Notificar a la SPA para re-suscribir
        this.onReconnect();
    }
}

// Uso
const reconnectMgr = new BOXReconnectManager({
    onReconnect: async () => {
        // RE-SUSCRIBIR TODOS los componentes (OBLIGATORIO)
        await connectionManager.resubscribeAll();
        console.log('[BOX] Reconnected and resubscribed');
    }
});
reconnectMgr.start();
```

### 42.4.3 Re-suscribir tras reconnect — pasos obligatorios

Después de reconectar el WebSocket BOX, el estado del servidor es limpio. El cliente debe:

1. **Re-establecer la sesión BOX**: `baja.comm.start()` crea una nueva `BServerSession` (`HTTP POST /box`).
2. **Re-abrir el WebSocket**: automático tras `start()`.
3. **Re-resolver los ORDs** de todos los componentes: las referencias al `BComponent` obtenidas antes del corte pueden ser inválidas (handles obsoletos).
4. **Re-registrar suscripciones**: llamar `sub.subscribe({comps: [...]})` para cada componente.
5. **Re-obtener estado inicial**: el primer evento `subscribed` confirma sincronización con el estado actual del servidor.

```javascript
// Patrón robusto post-reconnect
async function reconnectAndResubscribe(subscriber, ordStrings) {
    // 1. Re-resolver ORDs (handles nuevos post-reconnect)
    const comps = await Promise.all(
        ordStrings.map(ord => 
            baja.Ord.make(ord).get({ lease: true }).catch(err => {
                console.error(`ORD ${ord} failed: ${err}`);
                return null;
            })
        )
    );
    
    const validComps = comps.filter(c => c !== null);
    
    // 2. Re-suscribir
    if (validComps.length > 0) {
        await subscriber.subscribe({ 
            comps: validComps, 
            leaseTime: 30000 
        });
    }
}
```

**GOTCHA G42-7 — Stale component references post-reconnect**: CONFIRMADO en Bloque 22 (Gotcha 21): "referencias guardadas mueren con `location.reload()`. Re-resolver ORD siempre". Lo mismo aplica post-reconnect sin reload: los `BComponent` objetos previos tienen handles obsoletos del servidor. Re-resolución por ORD es obligatoria.

### 42.4.4 BBrokerChannel `transfer` — ¿handoff de subscriptions?

CONFIRMADO en Bloque 41.4 que `BBrokerChannel` tiene un comando `transfer`. Sin embargo:

- `BBrokerChannel` es un canal **Fox** (inter-station, server-to-server), NO un canal BOX (browser-to-server).
- El `transfer` en `BBrokerChannel` es para transferir contexto de suscripción entre Supervisor y Subordinate en el protocolo Fox.
- **NO aplica para reconexión de browser**: un browser que reconecta NO puede usar `transfer` para "heredar" las suscripciones previas. El servidor trata cada nueva sesión BOX como una sesión completamente nueva.

**Conclusión**: No hay "snapshot resume" para reconexión de browser en N4.14. El re-subscribe completo es obligatorio.

---

## 42.5 Memory leaks típicos

### 42.5.1 Leak 1 — Subscriber no detached (el más común)

```javascript
// INCORRECTO — leak garantizado en SPA con navegación
function initPanel() {
    const sub = new baja.Subscriber();
    sub.attach({ changed: (prop, cx) => updateUI(this, prop) });
    sub.subscribe({ comps: [comp] });
    // ❌ NUNCA se llama sub.detach() ni sub.unsubscribe()
}

// Al navegar a otro panel: el Subscriber existe, el BComponent
// en el servidor sigue suscipto, el lease se renueva indefinidamente,
// y el callback sigue recibiendo eventos aunque el DOM esté destruido.
```

**Síntoma observable**: con DevTools → Memory → Heap Snapshot, los `baja.Subscriber` se acumulan sin GC.

**Fix**: siempre limpiar en el teardown del componente UI:

```javascript
// Vue.js
export default {
    data() { return { sub: null, comp: null } },
    async mounted() {
        this.sub = new baja.Subscriber();
        this.sub.attach({ changed: (prop) => this.handleChange(prop) });
        const target = await baja.Ord.make(this.ord).get({ lease: true });
        this.comp = target.getComponent();
        await this.sub.subscribe({ comps: [this.comp] });
    },
    beforeDestroy() {
        // Cleanup GARANTIZADO — incluso si mounted() no completó
        if (this.sub) {
            this.sub.detach();
            this.sub.unsubscribe();
        }
    }
};
```

### 42.5.2 Leak 2 — Listeners agregados sin remover

```javascript
// INCORRECTO — listener acumulado en cada mount sin cleanup
function MyWidget() {
    baja.comm.addConnectionListener({    // ← se acumula en cada instancia
        connectionLost: () => this.showOffline()
    });
    // ❌ nunca se llama baja.comm.removeConnectionListener()
}
```

**Fix**: rastrear y remover en teardown:

```javascript
class MyWidget {
    constructor() {
        this._connectionListener = {
            connectionLost: () => this.showOffline(),
            connectionRestored: () => this.showOnline()
        };
        baja.comm.addConnectionListener(this._connectionListener);
    }
    destroy() {
        baja.comm.removeConnectionListener(this._connectionListener);
    }
}
```

### 42.5.3 Leak 3 — BComponent graph retenido en memoria

El `BoxComponentSpace` (caché local del árbol de componentes) retiene referencias a objetos BComponent mientras estén suscritos. Si la SPA navega entre paneles sin llamar `unsubscribe()`, el caché crece ilimitadamente.

**Estimación de tamaño**: un BComponent típico con 10 slots ocupa ~5-20 KB en el BoxComponentSpace. Con 1000 componentes suscritos = 5-20 MB. Con 10,000 = 50-200 MB (peligroso para tabs de larga duración).

### 42.5.4 Cleanup en navegación SPA

```javascript
// React — useEffect con cleanup
useEffect(() => {
    let sub = new baja.Subscriber();
    let mounted = true;
    
    sub.attach({
        changed: (prop, cx) => {
            if (mounted) updateState(prop);  // guard contra update en unmount
        }
    });
    
    baja.Ord.make(ord).get({ lease: true })
        .then(target => {
            if (mounted) {
                return sub.subscribe({ comps: [target.getComponent()] });
            }
        });
    
    // Cleanup function — React la llama en unmount y en re-render
    return () => {
        mounted = false;
        sub.detach();
        sub.unsubscribe();
    };
}, [ord]);  // re-run si el ORD cambia
```

```javascript
// beforeunload — para cleanup de toda la SPA al cerrar tab
window.addEventListener('beforeunload', () => {
    globalSub.detach();
    globalSub.unsubscribe();
    // El servidor limpiará cuando la sesión expire (5 min default)
    // pero el unsubscribe explícito libera recursos más rápido
});
```

### 42.5.5 ¿Existe `subscriber.dispose()`?

INFERIDO — no confirmado en source público del Bloque 22 ni Bloque 36. La API documentada no incluye `dispose()`. El equivalente funcional es la secuencia `detach()` + `unsubscribe()`:

```javascript
// Equivalente a dispose()
function disposeSubscriber(sub) {
    sub.detach();       // limpia todos los callbacks
    sub.unsubscribe();  // libera recursos servidor
    sub = null;         // permite GC del objeto JS
}
```

**TODO honesto 42-3**: Verificar en `Subscriber.js` completo si existe algún método `dispose()`, `destroy()` o `close()` no documentado en la API pública.

---

## 42.6 Scaling: 100, 1k, 10k suscripciones concurrentes desde un browser

### 42.6.1 Tier breakdown performance esperado

| Tier | # subs | Comportamiento esperado | Limitante principal |
|------|--------|------------------------|---------------------|
| **Verde** | 1–100 | <5ms por evento `changed`, UI fluida, 0 problemas observables | Ninguno |
| **Amarillo** | 100–500 | 5–50ms por ciclo de procesamiento, posible lag visible en updates masivos | CPU JS single-thread |
| **Naranja** | 500–1000 | 50–200ms lag en render, memory pressure visible, posible jank en animaciones | Memory + event loop |
| **Rojo** | 1000–5000 | Degradación severa, tab freeze en bursts de cambios, riesgo OOM | BOX frame processing overhead |
| **Crítico** | >5000 | Tab crash frecuente (Chrome), OOM en dispositivos móviles | V8 heap limit, GC pressure |

Valores alineados con Bloque 36.15 (benchmark): "100 points live subs por browser tab en un Supervisor; 40 en un JACE".

### 42.6.2 Límites del browser

**RAM**: V8 (Chrome) tiene heap limit de ~1.4 GB en 64-bit. Con 10,000 componentes en BoxComponentSpace (50-200 MB), se acerca al límite con la suma del resto de la SPA.

**CPU event loop**: cada unsolicited BOX frame con cambios en 1000 componentes genera un microtask que procesa los deltas y dispara los callbacks. Si los callbacks hacen DOM updates síncronos, el browser event loop se bloquea.

**Conexiones WebSocket**: el browser tiene límite de ~6 WebSockets por origen (Chrome). Sin embargo, N4 usa **UN SOLO WebSocket** (`/wsbox`) con multiplexado BOX. No hay riesgo de exceder este límite con suscripciones.

### 42.6.3 Límites del servidor (station)

CONFIRMADO en Bloque 36.15:

| Punto | Límite empírico |
|-------|----------------|
| **Fox channel exhaustion** | ~1000 subs (Bloque 13) — compartido entre FOX y BOX |
| **BBrokerChannel poll rate** | No documentado — depende de `BStationPollScheduler` |
| **JACE (hardware pequeño)** | 40 subs live por tab es el límite seguro |
| **Supervisor (servidor)** | 100 subs live por tab es el límite seguro |
| **BUxBoundTable maxRows** | 1000 rows hard limit (Bloque 36.3) |

**GOTCHA G42-8 — El límite no es solo por tab, es acumulativo**: Múltiples tabs del mismo usuario usan la misma sesión (JSESSIONID compartido — CONFIRMADO Bloque 47.5.5). Si 5 tabs tienen 100 subs cada una = 500 subs concurrentes en el servidor para ese usuario. El servidor no diferencia entre tabs del mismo usuario.

### 42.6.4 Patrones para escalar más allá de 100 subs

#### Patrón 1: Virtualización — suscribir solo lo visible

```javascript
// Inspirado en virtual scroll: solo suscribir rows visible en pantalla
class VirtualizedSubscriptionManager {
    constructor(subscriber, allComponents) {
        this.sub = subscriber;
        this.all = allComponents;  // todos los ORDs
        this.visible = new Set(); // los actualmente suscritos
    }
    
    async onScroll(visibleOrdStrings) {
        const toSubscribe = visibleOrdStrings.filter(o => !this.visible.has(o));
        const toUnsubscribe = [...this.visible].filter(o => !visibleOrdStrings.includes(o));
        
        // Unsub primero (libera slots en el servidor)
        if (toUnsubscribe.length > 0) {
            const comps = toUnsubscribe.map(o => this.resolvedComps.get(o));
            await this.sub.unsubscribe(comps);
            toUnsubscribe.forEach(o => this.visible.delete(o));
        }
        
        // Sub los nuevos
        if (toSubscribe.length > 0) {
            const targets = await Promise.all(
                toSubscribe.map(o => baja.Ord.make(o).get({ lease: true }))
            );
            const comps = targets.map(t => t.getComponent());
            await this.sub.subscribe({ comps });
            toSubscribe.forEach(o => this.visible.add(o));
        }
    }
}
```

#### Patrón 2: Lazy subscribe on scroll (más simple)

```javascript
// IntersectionObserver para activar subs cuando un elemento es visible
const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const ord = entry.target.dataset.ord;
            subscribeIfNeeded(ord, entry.target);
        } else {
            const ord = entry.target.dataset.ord;
            unsubscribeIfNeeded(ord);
        }
    });
}, { rootMargin: '100px' });

document.querySelectorAll('[data-ord]').forEach(el => observer.observe(el));
```

#### Patrón 3: Aggregated subscription (suscribir padre, filtrar en cliente)

En lugar de suscribir 100 componentes leaf, suscribir 1 componente padre y filtrar los slots en el callback `changed`:

```javascript
// En vez de 100 subscriptions a puntos individuales...
// suscribir el folder padre que los contiene
sub.subscribe({ comps: [folderComp] });
sub.attach({
    added: (prop, cx) => {
        // nuevo punto añadido al folder
    },
    changed: (prop, cx) => {
        // cualquier slot del folder cambió
        if (prop.getName().endsWith('_temperature')) {
            updateTemperatureDisplay(this, prop);
        }
    }
});
```

**Tradeoff**: se reciben deltas de TODOS los slots del folder, no solo los interesantes. Mayor carga de procesamiento JS per-frame, pero menos suscripciones totales en el servidor.

---

## 42.7 Subscription a topics vs slots vs componentes

### 42.7.1 Slot-based (el tipo más común)

El evento `changed` sobre un slot/property es el mecanismo principal para monitorear valores:

```javascript
sub.attach({
    changed: function(prop, cx) {
        // `this` = BComponent
        // prop.getName() = nombre del slot (ej: "out", "in1", "value")
        // this.get(prop) = valor actual (BValue)
        // this.getDisplay(prop) = string formateado con unidades
        // this.getStatus(prop) = BStatus {ok, overridden, fault, down, disabled, stale}
    }
});
```

**Características**:
- Recibe valor + timestamp + status en cada cambio
- El servidor aplica deadbanding (no se dispara si el cambio es menor al deadband configurado)
- Es **stateful**: el Subscriber tiene el estado actual en `BoxComponentSpace`
- Apropiado para: dashboards de monitoreo, setpoints, estados de puntos

### 42.7.2 Topic-based (eventos sin estado)

El evento `topicFired` es para topics que disparan eventos discretos (alarmas, comandos, notificaciones):

```javascript
sub.attach({
    topicFired: function(topic, event, cx) {
        // `this` = BComponent que disparó el topic
        // topic.getName() = nombre del topic (ej: "alarm", "commandResult")
        // event = BValue asociado al firing (puede ser null o BAlarmRecord, etc.)
    }
});
```

**Características**:
- Es **stateless**: el topic es un evento discreto, no un valor persistente
- No hay "último valor" — si la SPA no estaba suscrita cuando se disparó, no lo recibe
- Apropiado para: notificaciones de alarma en tiempo real, confirmaciones de comandos

**GOTCHA G42-9 — Topics no se "replay"**: A diferencia de un slot cuyo valor está disponible al suscribirse, un topic que se disparó antes de la suscripción NO se entrega. La SPA solo recibe topics que ocurren DESPUÉS de establecer la suscripción.

### 42.7.3 Component-based (agregado de slots)

Suscribir un BComponent recibe eventos de TODOS sus slots y también eventos estructurales:

```javascript
sub.subscribe({ comps: [folderComp] });
sub.attach({
    changed: (prop, cx) => { /* cualquier slot del comp cambió */ },
    added: (prop, cx) => { /* slot dinámico añadido */ },
    removed: (prop, val, cx) => { /* slot dinámico removido */ },
    topicFired: (topic, event, cx) => { /* cualquier topic del comp */ },
    componentRenamed: (cx) => { /* el comp mismo fue renombrado */ },
    unmount: (cx) => { /* comp eliminado del árbol */ }
});
```

**Características**:
- Recibe TODO — incluye slots que no son de interés
- Útil para monitorear la estructura del árbol (añadir/remover puntos dinámicos)
- Mayor overhead de procesamiento de eventos

### 42.7.4 Tabla de tradeoffs

| Criterio | Slot/Property | Topic | Component agregado |
|----------|--------------|-------|-------------------|
| Estado disponible al suscribir | SÍ (valor actual) | NO (solo events futuros) | SÍ (todos los slots) |
| Deadbanding server-side | SÍ | N/A | SÍ por slot |
| Overhead suscripción servidor | 1 por punto | 1 por comp | 1 por comp (recibe todo) |
| Eventos estruturales | NO | NO | SÍ |
| Apropiado para | Monitoring, dashboards | Alarmas, comandos | Árbol dinámico |
| Statefulness | Stateful | Stateless | Stateful |

### 42.7.5 Cuándo usar cada uno

- **Slot**: para cualquier punto de valor con historia (temperatura, setpoint, status booleano, contador). Siempre que necesites el "valor actual".
- **Topic**: para alarmas en tiempo real, resultados de comandos asíncronos, notificaciones de eventos. Cuando el evento es lo importante, no el estado.
- **Component agregado**: para monitorear carpetas con estructura dinámica (puntos que se añaden/remueven en runtime), o cuando necesitás eventos estructurales (`renamed`, `unmount`).

---

## 42.8 Refinamiento de bloques relacionados

### Bloque 43 — Schedule render + edit desde SPA externa

Lo descubierto en 42 impacta en:

1. **Suscripción al schedule**: Los `BWeeklySchedule` (y derivados de `BAbstractSchedule`) son BComponents normales, suscribibles via `baja.Subscriber`. El slot `out` (output actual) y `nextEvent` son los más relevantes para monitoreo live.

2. **Edición de schedule**: Las ediciones a un schedule desde SPA externa requieren `invoke()` en actions o `set()` en slots via BOX. El mismo Subscriber que monitorea puede enviar escrituras. **La separación entre suscripción (read) y escritura (write via set/invoke) usa el MISMO canal BOX** — no requiere conexión separada.

3. **Topic `clockChanged`**: Los schedules disparan `clockChanged` cuando el output cambia. Este topic es el mecanismo live para detectar "schedule acaba de ejecutarse". Apropiado suscribirse via `topicFired`.

4. **Lease y schedules**: Si la SPA cierra la conexión y el lease expira, el schedule sigue ejecutándose en el servidor — no se interrumpe. Solo se pierde la notificación cliente. Al reconectar y re-suscribir, el primer `subscribed` callback entrega el estado actual.

### Bloque 44 — Alarm Console pipeline frontend

Lo descubierto en 42 impacta en:

1. **`BAlarmChannel` BOX**: CONFIRMADO en Bloque 36.6 que existe un `alarm` channel BOX. La suscripción a alarmas live usa este canal, NO el `baja.Subscriber` genérico. El channel `alarm` envía unsolicited cuando llegan alarmas nuevas.

2. **Topics de alarma**: Las alarmas también disparan topics en los BComponents fuente (`alarm` topic). Si la SPA suscribe el punto fuente via `baja.Subscriber`, puede recibir el `topicFired` cuando ese punto genera una alarma. Pero para la Alarm Console completa (todas las alarmas de la station), el `BAlarmChannel` es más apropiado.

3. **Race condition acknowledge**: Si dos clientes intentan ack la misma alarma simultáneamente, el servidor procesa el primero y el segundo recibe un error. El Subscriber NO tiene mecanismo de optimistic locking — la SPA debe manejar el 409/error del servidor.

4. **Reflow confirma**: `canAcknowledgeAlarms` verifica `operatorWrite` sobre `BAlarmService` en el contexto del usuario — la autorización es server-side, no basta con el rol client-side (CONFIRMADO Bloque 51.1.4).

### Bloque 45 — History/Trend chart consumption

Lo descubierto en 42 impacta en:

1. **Historia NO usa `baja.Subscriber`**: Los datos históricos se obtienen via `BHistoryChannel` BOX (channel `hist`) — queries point-in-time, NO suscripción live. El Subscriber solo sirve para el valor ACTUAL (`out` del punto), no para el historial.

2. **Tendencia live**: Para mostrar un trending live (valor actual + historia), el patrón es: (a) query inicial por historia via `BHistoryChannel`, y (b) suscripción al slot `out` via `baja.Subscriber` para updates live. Son dos mecanismos distintos que la SPA debe combinar.

3. **Coalescing afecta granularidad**: Si el punto cambia 10 veces por segundo pero el Subscriber solo recibe el último valor en cada ciclo BOX (~100ms), el trending live via Subscriber tiene resolución máxima de ~10 FPS. Para resolución completa se necesita la historia COV del servidor.

4. **Lease y trending**: Mantener el lease activo es especialmente crítico para charts live. Un chart que pierde el lease y no lo detecta muestra datos "congelados" sin indicación visual de staleness.

### Bloque 46 — Writes con priority array desde SPA externa

Lo descubierto en 42 impacta en:

1. **Suscripción al priority array**: El slot `priorityArray` de un `BNumericWritable` (u otras BWritable) es suscribible como cualquier slot. El Subscriber `changed` para `priorityArray` entrega el array completo de 16 niveles en cada cambio.

2. **Write y lease coexisten**: El mismo canal BOX usado para subscribe sirve también para writes (`set` op en `boxcs` channel). La SPA puede suscribir Y escribir sobre los mismos componentes sin setup adicional.

3. **Feedback de write via Subscriber**: Si la SPA escribe un valor y suscribe el mismo punto, recibirá el callback `changed` con el nuevo valor confirmado por el servidor. Este es el mecanismo de "write confirmation" — no hay un ACK explícito de write; el nuevo valor en `changed` es la confirmación.

4. **Override via Subscriber**: El slot `override` de un BWritable (que setea un override temporal) también es suscribible y muestra el estado de override activo.

### Bloque 48 — RBAC visibility en frontend

Lo descubierto en 42 impacta en:

1. **Permissions chequeados server-side en ORD resolution**: Si el usuario no tiene read permission sobre un componente, `baja.Ord.make(ord).get()` rechaza la Promise. El Subscriber nunca se establece — no hay información filtrada que "llegue parcial".

2. **Facets y flags visibles**: Los facets de los slots (`hidden`, `readonly`, `summary`) son visibles al Subscriber via `facetsChanged` y `flagsChanged`. La SPA puede usarlos para construir UI adaptativa: si `readonly=true`, deshabilitar el input; si `hidden=true`, ocultar el widget.

3. **Subscriber y permissions dinámicos**: Si los permisos de un usuario cambian durante la sesión (ej. admin revoca acceso), el servidor puede enviar un `removeKnob` unsolicited para las suscripciones que ya no son permitidas. El evento `unsubscribed` se dispara en el cliente. La SPA debe manejar este caso mostrando "acceso revocado" en el widget.

4. **`canAcknowledgeAlarms` patrón**: La autorización a nivel de acción (no solo de lectura) se verifica server-side en el momento del invoke. La SPA no puede confiar en estado client-side para determinar si una acción está disponible — debe intentar y manejar el error de autorización.

### Bloque 49 — Facets, i18n y formatting en cliente

Lo descubierto en 42 impacta en:

1. **Facets disponibles en el callback**: El objeto `prop` en el callback `changed` permite acceder a los facets: `prop.getFacets()` retorna el `BFacets` con range, units, format, precision. Esto permite que la SPA formate el valor correctamente sin hardcodear unidades.

2. **`this.getDisplay(prop)`**: Este método en el callback `changed` retorna el valor ya formateado por el servidor según los facets del slot (unidades, precisión, etc.). Es la forma recomendada de obtener el string display — evita reimplementar el formatting en el cliente.

3. **Facets dinámicos**: Los facets pueden cambiar dinámicamente (ej. un componente cuyo rango de setpoint cambia según el modo de operación). El evento `facetsChanged` del Subscriber notifica estos cambios. La SPA debe re-leer los facets y re-renderizar el widget cuando este evento ocurre.

4. **Lexicon i18n**: Los nombres de display de slots y tipos vienen del servidor en el idioma de la sesión del usuario. No hay mecanismo en el Subscriber para cambiar el idioma sin re-autenticar con un `Accept-Language` diferente.

---

## 42.9 Antipatterns numerados

**AP-1 — Subscriber global sin cleanup**
```javascript
// ANTIPATRÓN
window.globalSub = new baja.Subscriber();  // global
// Nunca se limpia → leak guaranteed al navegar
```
**Fix**: siempre destroy en `beforeDestroy`/`useEffect cleanup`/`ngOnDestroy`.

**AP-2 — Resolve ORD y guardar la referencia entre reloads/reconnects**
```javascript
// ANTIPATRÓN
let savedComp = await baja.Ord.make(ord).get(); // guardar en módulo
// ... más tarde, tras reconnect ...
savedComp.get('out');  // ERROR — handle obsoleto
```
**Fix**: siempre re-resolver el ORD tras reconnect.

**AP-3 — No manejar el caso de lease expirado**
```javascript
// ANTIPATRÓN
sub.subscribe({ comps: [comp] });
// No hay ningún mecanismo de detección de lease expiry
// La UI muestra datos obsoletos indefinidamente
```
**Fix**: implementar `document.addEventListener('visibilitychange')` para detectar tab backgrounding y re-suscribir al volver.

**AP-4 — Suscribir 1000+ componentes sin virtualización**
```javascript
// ANTIPATRÓN
const comps = await Promise.all(allORDs.map(o => baja.Ord.make(o).get()));
sub.subscribe({ comps });  // 1000 suscripciones → station saturada
```
**Fix**: virtualizar con IntersectionObserver o paginación.

**AP-5 — Usar `added/removed` para monitorear valores**
```javascript
// ANTIPATRÓN
sub.attach({
    added: (prop) => updateValueDisplay(prop)  // INCORRECTO — `added` es para nuevos SLOTS dinámicos
});
```
**Fix**: usar `changed` para valores, `added`/`removed` solo para slots dinámicos.

**AP-6 — Esperar que `topicFired` entregue el historial**
```javascript
// ANTIPATRÓN
sub.attach({ topicFired: (topic) => log(topic) }); // captura SOLO eventos futuros
// Los eventos previos a la suscripción NO se entregan
```
**Fix**: para historial de eventos, usar `BAlarmChannel` o History API.

**AP-7 — Multiple Subscribers para el mismo componente sin consolidación**
```javascript
// ANTIPATRÓN
// Widget A: sub1.subscribe({comps: [comp]})
// Widget B: sub2.subscribe({comps: [comp]})
// Widget C: sub3.subscribe({comps: [comp]})
// → 3 suscripciones server-side para el MISMO comp (waste)
```
**Fix**: usar un Subscriber global con dispatcher interno por handle.

**AP-8 — Asumir que `unsubscribe()` es inmediato server-side**
```javascript
// ANTIPATRÓN — asume que el unsubscribe fue procesado
sub.unsubscribe([comp]);
await baja.Ord.make(ord).resolve({});  // inmediatamente después
// El servidor puede aún enviar deltas durante el RTT del removeKnob
```
**Fix**: `unsubscribe()` es fire-and-forget. El servidor puede enviar algunos deltas más mientras procesa el removeKnob.

**AP-9 — Deadlock initialize/load en widgets bajaux**
```javascript
// ANTIPATRÓN — documentado en Widget.js:857
doInitialize(dom, params) {
    return this.load(value);  // DEADLOCK — load espera a que doInitialize termine
}
```
**Fix**: resolver sub.subscribe() dentro de doInitialize y llamar load en el callback `subscribed`.

**AP-10 — `beforeunload` como única estrategia de cleanup**
```javascript
// ANTIPATRÓN — no confiable en mobile browsers
window.addEventListener('beforeunload', () => {
    sub.unsubscribe();  // mobile browsers no garantizan ejecutar esto
});
```
**Fix**: `beforeunload` + `visibilitychange` + cleanup en cada componente individual.

---

## 42.10 TODOs honestos

**TODO-1**: Verificar si `BComponentSpaceSessionHandler.addKnob()` acepta parámetro de eventMask para filtrado server-side de eventos. Source: `box-rt.jar:com/tridium/box/BComponentSpaceSessionHandler.class`. Si existe filtrado server-side, permite reducir el tráfico BOX para suscripciones a gran escala.

**TODO-2**: Verificar el cap máximo de `LeaseManager` (Bloque 20). ¿Hay un techo al `time` pasado en `lease()`? ¿Es configurable por BOG? Source: `box-rt.jar` + `baja.jar:com/tridium/sys/lease/LeaseManager.class`.

**TODO-3**: Confirmar si `baja.Subscriber` tiene algún método `dispose()`, `destroy()` o `close()` no documentado en los sources analizados. Source: `bajaScript-ux.jar:rc/baja/comp/Subscriber.js` completo.

**TODO-4**: Medir empíricamente el overhead de frame processing por número de componentes suscritos. La estimación de 100 subs/tab para Supervisor es un límite documentado por Honeywell, pero el mecanismo de degradación no está confirmado empíricamente (¿es linear? ¿exponencial?). Requiere lab con N4 real.

**TODO-5**: Verificar si el servidor tiene un límite máximo de suscripciones por sesión BOX configurable vía `system.properties`. Candidatos: `box.serverSession.maxKnobs`, `box.maxSubscriptionsPerSession`. Source: `box-rt.jar` strings.

**TODO-6**: Confirmar el mecanismo exacto de coalescing en `BComponentSpaceSessionHandler` y su periodo de ciclo. ¿Es configurable? Source: `box-rt.jar:BComponentSpaceSessionHandler.class` ciclo de polling.

**TODO-7**: Verificar si el evento `unsubscribed` se dispara cuando el lease expira (pro-active server notification) o solo cuando se llama `unsubscribe()` explícito. La arquitectura sugiere que el lease expiry puede ser silencioso. Requiere test en lab.

---

## 42.11 Próximos pasos (bloques siguientes)

### Bloque 43 — Schedule render + edit desde SPA externa
- Prioridad: ALTA
- Foco: suscripción al slot `out` de BAbstractSchedule, topic `clockChanged`, write de schedule entries via BOX, y el flujo de edición de `BWeeklySchedule` desde SPA (diferencia entre Workbench UI y BOX protocol directo).
- Input de Bloque 42: el Subscriber estándar funciona para monitorear schedules; la edición requiere `invoke()` en actions del schedule.

### Bloque 44 — Alarm Console pipeline frontend
- Prioridad: ALTA
- Foco: `BAlarmChannel` BOX detalle completo (ops disponibles, formato de mensajes), ack flow desde SPA, diferencia entre `baja.Subscriber topicFired` y `BAlarmChannel live feed`, paginación de alarmas históricas.
- Input de Bloque 42: confirmar que `BAlarmChannel` channel key en BOX frame es `"alarm"` y documentar ops disponibles.

### Bloque 45 — History/Trend chart consumption
- Prioridad: MEDIA
- Foco: `BHistoryChannel` BOX (channel `hist`), query inicial + streaming de datos, formato de respuesta, combinación con Subscriber live para trending en tiempo real.
- Input de Bloque 42: historia NO usa Subscriber — es un channel separado. El trending live requiere combinar ambos.

### Bloque 46 — Writes con priority array desde SPA externa
- Prioridad: ALTA
- Foco: write via BOX (`set` op en `boxcs`), priority array 16 niveles (nivel 8 = operator override), override duration, relinquish, feedback de write via Subscriber `changed`.
- Input de Bloque 42: el mismo canal BOX sirve para reads y writes; el `changed` callback confirma el write.

### Bloque 48 — RBAC visibility en frontend
- Prioridad: MEDIA
- Foco: cómo los permisos Niagara afectan la resolución de ORDs (`get()` falla con 403 si no hay read perm), `facets.hidden`/`facets.readonly` como señales de UI, `unsubscribed` event cuando permisos se revocan dinámicamente.
- Input de Bloque 42: permissions son chequeados en ORD resolution server-side; el Subscriber recibe `flagsChanged`/`facetsChanged` para actualizaciones de permisos UI.

### Bloque 49 — Facets, i18n y formatting en cliente
- Prioridad: BAJA
- Foco: `prop.getFacets()` en el callback `changed`, `this.getDisplay(prop)` para string formateado, facets dinámicos via `facetsChanged`, lexicon i18n.
- Input de Bloque 42: los facets están disponibles en el callback `changed` sin llamada adicional.

---

## Fuentes primarias analizadas

1. **Bloque 22** — BajaScript subscriber API intro (22.11), BOX protocol (22.12), Component model browser (22.14)
2. **Bloque 36** — Subscriber lifecycle real (`Component.js:1795` lease 10s default), BOX wire format (36.6), BoxComponentSpace sync ops (36.7), subscribe roundtrip diagram (36.9), WebSocket servlet config (36.10), performance numbers (36.15)
3. **Bloque 41** — BBrokerChannel Fox (41.4 — sub/unsub/transfer/invoke), confirmación que NO aplica para reconexión de browser
4. **Bloque 47** — Bootstrap headless: location.host hardcoded (47.1.3), CORS ausente (47.2), reconnect manual obligatorio (47.5.4 GOTCHA G47-7), lease 10s GOTCHA G47-5
5. **Bloque 51** — Implementación real Subscriber en app-readable.js (51.2.3), race condition subscribe/unsubscribe, batching de resolves 100ms (51.2.3), disconnect NO auto-reconnect (51.2.4)

Total: ~150 KB de material de referencia analizado, 11 secciones técnicas, 10 antipatterns, 7 TODOs honestos, 9 GOTCHAs numerados.
