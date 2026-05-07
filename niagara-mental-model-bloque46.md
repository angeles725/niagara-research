# Bloque 46 — Writes con priority array desde SPA externa

**Fecha**: 2026-05-04
**Método**: Síntesis multi-bloque READ-ONLY. Fuentes empíricas consolidadas de Bloques 24 (control framework + priority array), 23 (BACnet priority semantics), 29 (NiagaraRPC + web tier), 41 (Transaction semantics), 18/47 (CSRF + auth headless), 50/51 (Reflow como evidencia de referencia). Evidencia oBIX empírica directa de `obix-write-boolean.md` + `obix-write-protocol-empirico.md` (writes confirmados contra station real N4.14).
**Distribución analizada**: Honeywell OptimizerSupervisor-N4.14.0.162

---

## 46.0 Framing del problema — ¿qué escribe una SPA y cómo llega al device?

Una SPA externa que necesita escribir setpoints, overrides, o comandos discretos en una station Niagara N4 debe resolver cinco capas de complejidad:

1. **Autenticación headless**: SCRAM-SHA256 o Bearer token para obtener JSESSIONID
2. **CSRF**: Header `x-niagara-csrfToken` obligatorio en cualquier POST/PUT/DELETE
3. **Protocolo de write**: elegir entre oBIX REST, BOX BajaScript, o NiagaraRPC — cada uno con semántica distinta
4. **Priority array**: indicar el nivel correcto (1-16) según la intención del write
5. **Override lifecycle**: BOverride con duration, expiración automática, y relinquish correcto

La respuesta no es única: existen **dos paths viables y uno emergente** que deben elegirse según la arquitectura del deploy.

```
SPA externa
   │
   ├── Path A: oBIX REST (XML over HTTP) ──► ObixServlet ──► BWritablePoint ops
   │           /obix/config/<ord>/set/            confirmado empíricamente
   │           /obix/config/<ord>/override/
   │           /obix/config/<ord>/active|inactive/
   │
   ├── Path B: BOX BajaScript API ──────────► BBrokerChannel ──► BComponent.set()
   │           baja.Subscriber + invoke()         (requiere bs.built.min.js same-origin)
   │
   └── Path C: NiagaraRPC custom handler ──► BNiagaraRpcDispatcher ──► action invoke
               POST /rpc/<name>/<method>          (requiere módulo custom en station)
```

---

## 46.1 BWritablePoint — anatomía completa

### 46.1.1 Jerarquía Java (CONFIRMADO Bloque 24.15-16)

```
BControlPoint extends BComponent (abstract)
  ├── BNumericPoint implements BINumeric
  │     └── BNumericWritable extends BNumericPoint implements BIWritablePoint
  │
  ├── BBooleanPoint implements BIBoolean
  │     └── BBooleanWritable extends BBooleanPoint implements BIWritablePoint
  │
  ├── BEnumPoint
  │     └── BEnumWritable
  │
  └── BStringPoint
        └── BStringWritable
```

**kitControl adds**:
```
BKitNumericPoint extends BNumericPoint (propagateFlags)
BKitBooleanPoint extends BBooleanPoint
BKitEnumPoint   extends BEnumPoint
```

### 46.1.2 Slots del priority array (BNumericWritable como referencia)

```java
// Confirmado empíricamente vs station real (obix-write-protocol-empirico.md)
Property in1     BStatusNumeric  // LEVEL_1  — Manual Life Safety  (no visible vía oBIX write)
Property in2     BStatusNumeric  // LEVEL_2  — Automatic Life Safety
Property in3     BStatusNumeric  // LEVEL_3  — Available
// ...
Property in8     BStatusNumeric  // LEVEL_8  — Manual Operator (convención Operator Override)
// ...
Property in10    BStatusNumeric  // LEVEL_10 — también "Operator Override" en instalaciones comunes
// ...
Property in16    BStatusNumeric  // LEVEL_16 — Available default (el más bajo — "default")
Property fallback BStatusNumeric // post-LEVEL_16: valor si TODOS null
Property out     BStatusNumeric  // computed: el ganador del priority resolution (read-only)
Property overrideExpiration BAbsTime  // expiración auto del override (LEVEL_8)
```

**GOTCHA 46-1 — in* son read-only vía oBIX**: Los slots `inN` NO son escribibles directamente via PUT oBIX. Se confirma en `obix-write-protocol-empirico.md` — solo `facets` y `wsAnnotation` tienen `writable="true"`. Para escribir a un nivel específico se usa la op correspondiente (`set`, `override`, `emergencyOverride`).

### 46.1.3 Priority resolution (WritableSupport)

```java
// Confirmado Bloque 24.16 — WritableSupport.getActiveLevel()
for (level = LEVEL_1; level <= LEVEL_16; level++) {
    value = getInStatusValue(level);
    if (value != null && value.isValid()) return value;
}
return fallback;  // si TODOS los niveles son null o inválidos
```

- **Menor número = mayor prioridad** (LEVEL_1 > LEVEL_16)
- Fallback se activa SOLO si todos los `in*` son null o status inválido
- `out` = copia del ganador + status OVERRIDDEN si algún level manual activo

### 46.1.4 Mapeo de operaciones por tipo (oBIX empírico vs BajaScript)

| Tipo de punto | op oBIX → priority | BajaScript action | Nivel efectivo |
|---|---|---|---|
| BNumericWritable | `set` (body: `<real val="X"/>`) | `comp.set(x)` | LEVEL_16 (default) |
| BNumericWritable | `override` (body: `NumericOverride`) | `comp.override(ovr)` | LEVEL_8 |
| BNumericWritable | `emergencyOverride` (body: `<real val="X"/>`) | `comp.emergencyOverride(x)` | LEVEL_2 |
| BBooleanWritable | `set` (body: `<bool val="true"/>`) | `comp.set(true)` | LEVEL_16 |
| BBooleanWritable | `active` (body: Override) | `comp.active()` | LEVEL_8 |
| BBooleanWritable | `inactive` (body: Override) | `comp.inactive()` | LEVEL_8 |
| BBooleanWritable | `emergencyActive` (sin body) | `comp.emergencyActive()` | LEVEL_1 |
| BBooleanWritable | `emergencyInactive` (sin body) | `comp.emergencyInactive()` | LEVEL_1 |
| Any | `auto` (sin body) | `comp.auto()` | relinquish LEVEL_8 |

---

## 46.2 Path A — oBIX REST (CONFIRMADO EMPÍRICAMENTE)

### 46.2.1 Protocolo oBIX en Niagara N4

oBIX (Open Building Information Xchange) es el protocolo REST/XML más antiguo y estable para write de puntos en Niagara. El servlet (`/obix/*`) está registrado en `web-rt.jar` (Bloque 29.4.1 inventario de ~53 servlets) y expone el árbol de componentes como objetos oBIX.

**Endpoint base**: `POST https://<station>:<port>/obix/<scheme>/<path>/<op>/`

Donde `<scheme>` = `config` (el esquema de componentes) y `<path>` corresponde al slot path en el árbol de estación (el `|slot:...` sin el pipe, con espacios escapados como `$20`).

### 46.2.2 Write a BNumericWritable (CONFIRMADO vs station real)

**Write simple (priority 16)**:
```http
POST /obix/config/Drivers/CODIGOS/Amp$20Fan/set/ HTTP/1.1
Host: 172.19.160.1
Authorization: Basic <base64(user:pass)>
Content-Type: text/xml

<real val="42.0" xmlns="http://obix.org/ns/schema/1.0"/>
```

Response exitoso (HTTP 200):
```xml
<real val="42.0"
      is="/obix/def/control:NumericWritable /obix/def/control:NumericPoint obix:Point"
      display="42,0 {ok} @ def"
      .../>
```

`display="@ def"` confirma que se escribió en priority 16 (default level). Respuesta síncrona — el valor ya cambió.

**Override con duración (priority 8)**:
```http
POST /obix/config/Drivers/CODIGOS/Amp$20Fan/override/ HTTP/1.1
Content-Type: text/xml

<obj is="/obix/def/control:NumericOverride" xmlns="http://obix.org/ns/schema/1.0">
  <real name="value"    val="55.0"/>
  <reltime name="duration" val="PT5M"/>
</obj>
```

**Emergency override (priority 1 — highest)**:
```http
POST /obix/config/Drivers/CODIGOS/Amp$20Fan/emergencyOverride/ HTTP/1.1
Content-Type: text/xml

<real val="99.0" xmlns="http://obix.org/ns/schema/1.0"/>
```

**Relinquish override** (vuelve al nivel auto):
```http
POST /obix/config/Drivers/CODIGOS/Amp$20Fan/auto/ HTTP/1.1
Content-Type: text/xml
(sin body)
```

### 46.2.3 Write a BBooleanWritable (CONFIRMADO vs station real)

La diferencia clave respecto a Numeric: el valor target está en el **nombre de la operación**, no en el body del override.

```http
# Write TRUE (priority 16)
POST /obix/config/Drivers/CODIGOS/SW$20ALTA$201/set/ HTTP/1.1
Content-Type: text/xml
<bool val="true" xmlns="http://obix.org/ns/schema/1.0"/>

# Write FALSE (priority 16)
POST /obix/config/Drivers/CODIGOS/SW$20ALTA$201/set/ HTTP/1.1
Content-Type: text/xml
<bool val="false" xmlns="http://obix.org/ns/schema/1.0"/>

# Override a TRUE por 5min (priority 8)
POST /obix/config/Drivers/CODIGOS/SW$20ALTA$201/active/ HTTP/1.1
Content-Type: text/xml
<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
  <reltime name="duration" val="PT5M"/>
</obj>

# Override a FALSE por 5min (priority 8)
POST /obix/config/Drivers/CODIGOS/SW$20ALTA$201/inactive/ HTTP/1.1
Content-Type: text/xml
<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
  <reltime name="duration" val="PT5M"/>
</obj>
```

**GOTCHA 46-2 — Boolean vs Numeric override son APIs distintas**: Para Numeric, una sola op `override` recibe valor + duración. Para Boolean, son DOS ops separadas (`active`/`inactive`) que no llevan valor — el valor está implícito en el nombre. Un cliente genérico necesita lógica condicional según tipo.

### 46.2.4 Auth en oBIX

oBIX soporta `Authorization: Basic` (CONFIRMADO empíricamente). El usuario debe tener asignado `BLegacyBasicAuthenticationScheme` — NO el Digest default. Para producción con SCRAM:

```javascript
// Después del SCRAM handshake (Bloque 47.3.2), usar el JSESSIONID + CSRF:
const response = await fetch('/obix/config/Drivers/CODIGOS/Amp$20Fan/set/', {
    method: 'POST',
    credentials: 'include',  // envía JSESSIONID cookie
    headers: {
        'Content-Type': 'text/xml',
        'x-niagara-csrfToken': csrfToken  // OBLIGATORIO en POST (Bloque 29.3.3)
    },
    body: '<real val="42.0" xmlns="http://obix.org/ns/schema/1.0"/>'
});
```

**GOTCHA 46-3 — CSRF es obligatorio en oBIX POST**: El `CsrfProtectedFilter` aplica a TODOS los POST/PUT/DELETE, incluyendo `/obix/*`. La omisión del header retorna 403 con body `csrf.token.verify.error`. No hay excepción para oBIX.

### 46.2.5 Path encoding Niagara

Los espacios en los nombres de slots se codifican como `$20` (escape propio de Niagara, NO `%20` URL-encode estándar). En JavaScript:

```javascript
function niagara_encode_path(slotPath) {
    // Reemplaza espacios y caracteres especiales
    return slotPath
        .replace(/ /g, '$20')
        .replace(/\//g, '$2F')  // slash dentro de nombre (raro)
        .replace(/#/g, '$23');
}

const url = `/obix/config/${niagara_encode_path('Drivers/CODIGOS/Amp Fan')}/set/`;
```

### 46.2.6 Respuesta de error oBIX

oBIX retorna HTTP 200 pero con `<err>` en el body para errores de aplicación:

```xml
<!-- Error: slot no existe o no tiene ese op -->
<err href=".../fallback/"
     display="Cannot translate: <real val='42.0' .../>"
     xmlns="http://obix.org/ns/schema/1.0"/>
```

La SPA debe parsear el body oBIX y verificar que NO contenga `<err>`:

```javascript
async function obix_write_numeric(path, value) {
    const resp = await fetch(path + '/set/', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'text/xml',
            'x-niagara-csrfToken': csrfTokenStore.get()
        },
        body: `<real val="${value}" xmlns="http://obix.org/ns/schema/1.0"/>`
    });
    
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    
    const text = await resp.text();
    if (text.includes('<err')) {
        const display = text.match(/display="([^"]+)"/)?.[1];
        throw new Error(`oBIX error: ${display}`);
    }
    
    return text;  // XML response con el nuevo estado
}
```

---

## 46.3 Path B — BOX BajaScript API

### 46.3.1 Prerequisito: same-origin y bs.built.min.js

El path BajaScript requiere que la SPA corra en el mismo origen que la station (o detrás de un reverse proxy que los unifique), porque:
1. `WebSocketConnection.js` hardcodea `location.host` para el WebSocket BOX (CONFIRMADO Bloque 47.1.3)
2. `baja.comm.start()` asume co-localización con la station

Para SPAs en origen cruzado, este path es **inviable sin reverse proxy** (ver Bloque 47.2).

### 46.3.2 Resolver el componente antes de escribir

```javascript
// Prerequisito: runtime baja inicializado (require(["baja!"]) + baja.comm.start())
baja.require(['baja!'], function() {
    baja.comm.start().then(function() {
        // Resolver el ORD del punto
        const ord = baja.Ord.make('station:|slot:/Drivers/CODIGOS/Amp Fan');
        ord.get().then(function(comp) {
            // comp es el BNumericWritable como objeto BajaScript
            performWrite(comp);
        });
    });
});
```

### 46.3.3 API de write BajaScript (BBrokerChannel.invoke)

El write de un writable point vía BajaScript pasa por `BBrokerChannel` (CONFIRMADO Bloque 41.4 — uno de los 6 fox channels, con comando `invoke`). El path del usuario en JS es más alto nivel:

```javascript
// Write simple (priority 16 — level default)
comp.set(42.0);

// Write con override (priority 8, 1 hora, Numeric)
const ovr = baja.make('control:NumericOverride');
ovr.set('value', 45.0);
ovr.set('duration', baja.Duration.make(3600000));  // 1h en ms
comp.invoke('override', ovr);

// Auto — release override (relinquish)
comp.invoke('auto');

// Emergency override (priority 1 — LEVEL_1)
comp.invoke('emergencyOverride', baja.Double.make(99.0));

// Para BBooleanWritable
comp.invoke('active', overrideArg);    // TRUE priority 8
comp.invoke('inactive', overrideArg);  // FALSE priority 8
comp.invoke('set', baja.Boolean.make(true));  // TRUE priority 16
```

**GOTCHA 46-4 — comp.set() vs comp.invoke()**: `set()` es un shortcut para escribir en LEVEL_16 (default). `invoke()` permite llamar cualquier action del BComponent, incluyendo las de niveles de prioridad específicos. Para writes a prioridades distintas de 16, siempre usar `invoke('actionName', arg)`.

### 46.3.4 CSRF en BOX

**CSRF NO se requiere por frame BOX** — se valida una única vez en el POST inicial a `/box` (CONFIRMADO Bloque 29.10.4 + 47.5.1). Una vez establecido el WebSocket, las operaciones dentro del WebSocket no llevan CSRF adicional. El invariant es seguro: no hay forma cross-origin de abrir el WebSocket sin pasar primero por el POST de `/box` que exige CSRF.

### 46.3.5 Flags.ASYNC — obligatorio en writes desde BOX

Al invocar acciones desde el thread del BOX WebSocket, el write DEBE ser asíncrono para no bloquear el engine thread de Niagara (patrón documentado en KNX, Bloque 37, aplicable universalmente):

```javascript
// El framework BajaScript maneja esto internamente en invoke()
// El desarrollador NO necesita gestionar Flags.ASYNC explícitamente desde JS
// Pero en módulos Java custom que hacen writes desde handlers BOX:
// comp.set(value, Flags.ASYNC | ctx);  ← obligatorio en Java

// Desde JS, baja.Component.invoke() ya es inherentemente asíncrono
// (retorna Promise / sigue el callback pattern)
comp.invoke('set', baja.Double.make(42.0)).then(() => {
    console.log('Write acknowledged');
}).catch(err => {
    console.error('Write failed:', err);
});
```

---

## 46.4 Priority array — mapeo Niagara nativo vs BACnet

### 46.4.1 Los 16 niveles y su convención

Niagara hereda la semántica de 16 niveles de BACnet, pero los nombra mediante `BPriorityLevel` enum (CONFIRMADO Bloque 24.16):

| BPriorityLevel | Nivel # | Uso convencional | Quién escribe aquí (convención) |
|---|---|---|---|
| LEVEL_1 | 1 | Manual Life Safety | Sistema de safety (PLC, sensor emergencia) |
| LEVEL_2 | 2 | Automatic Life Safety | BMS emergency automation |
| LEVEL_3 | 3 | Available | — |
| LEVEL_4 | 4 | Available | — |
| LEVEL_5 | 5 | Available | — |
| LEVEL_6 | 6 | Available | — |
| LEVEL_7 | 7 | Available | Demanda/load shedding automático |
| LEVEL_8 | 8 | Manual Operator | **Override de operador — el más común desde SPA** |
| LEVEL_9 | 9 | Available | — |
| LEVEL_10 | 10 | Available | "Operator Override" en muchas instalaciones Honeywell |
| LEVEL_11 | 11 | Available | — |
| LEVEL_12 | 12 | Available | Aplicaciones soft (HVAC scheduling) |
| LEVEL_13 | 13 | Available | — |
| LEVEL_14 | 14 | Available | — |
| LEVEL_15 | 15 | Available | — |
| LEVEL_16 | 16 | Available default | Setpoint base, schedule, valor por defecto |
| FALLBACK | — | post-16 | Valor si todos los in* son null |

**GOTCHA 46-5 — convención LEVEL_8 vs LEVEL_10**: La convención oficial Niagara/BACnet usa LEVEL_8 para "Manual Operator Override". Sin embargo, instalaciones Honeywell pueden usar LEVEL_10 para el mismo propósito. El commissioning de cada proyecto DEBE documentar explícitamente qué nivel usa el "operator override" de la SPA. Consultar con el BAS engineer antes de hardcodear.

### 46.4.2 Mapeo oBIX → priority level

oBIX expone las operaciones por nombre — el mapeo a nivel de prioridad es:

| Op oBIX | Priority Niagara (inferido Bloque 24.16 + empírico) |
|---|---|
| `set` | LEVEL_16 (escribe en `in16`) |
| `override` / `active` / `inactive` | LEVEL_8 (escribe en `in8` con overrideExpiration) |
| `emergencyOverride` / `emergencyActive` / `emergencyInactive` | LEVEL_1 o LEVEL_2 |
| `auto` | clear LEVEL_8 (pone `in8` = null) |
| `emergencyAuto` | clear LEVEL_1/2 |

**GOTCHA 46-6 — No hay acceso a LEVEL_3..LEVEL_7 vía oBIX**: Las operaciones oBIX solo exponen tres bandas de prioridad (1, 8, 16). Para escribir en LEVEL_4 o LEVEL_12 desde una SPA, se necesita BajaScript (que hace setSlotValue directo) o un NiagaraRPC custom que reciba el nivel explícitamente. oBIX es conveniente pero inflexible en granularidad de prioridad.

### 46.4.3 BACnet priority array — diferencias con Niagara nativo

Para puntos que son proxies BACnet (Bloque 23):

```
BACnet PRIORITY_ARRAY (property 87) tiene 16 entradas.
Cuando Niagara escribe via BBacnetProxyExt:
  - priorityForWriting en BBacnetScheduleExport mapea nivel Niagara → BACnet
  - LEVEL_1 Niagara → BACnet priority 1 (Manual Life Safety)
  - LEVEL_16 Niagara → BACnet priority 16 (fallback del device)
```

La semántica es idéntica para BACnet-proxied points. La station N4 hace la traducción internamente. Desde la SPA, el write es igual.

**Diferencia clave**: Para BACnet Analog/Binary Output Objects, si Niagara escribe via `BACnet WriteProperty` con `priority=null` o sin el parámetro, el device BACnet puede rechazar el write (spec obliga priority en comandable objects). El ProxyExt en `bacnet-rt.jar` maneja esto automáticamente cuando `priorityForWriting` está configurado en el `BBacnetProxyExt`.

### 46.4.4 Relinquish default

Cuando todos los niveles `in1..in16` son null, el punto cae al `fallback`:

```
WritableSupport.getActiveLevel() → FALLBACK
out = fallback.value con status OK (sin OVERRIDDEN flag)
```

Llamar `auto()` en LEVEL_8 pone `in8 = null`. Si `in1..in7` y `in9..in16` también son null, el punto va a fallback. La SPA debe entender que "release" puede llevar el punto a un valor inesperado si otro nivel está activo.

---

## 46.5 BOverride — override temporal con auto-relinquish

### 46.5.1 Clase BOverride (CONFIRMADO Bloque 24.16)

```java
// En control-rt.jar
BOverride
  duration              BRelTime  // cuánto dura el override
  maxOverrideDuration   BRelTime  // cap máximo (configurable por site)

BNumericOverride extends BOverride
  value                 double    // el valor numérico

// Para Boolean: el valor está implícito en la acción (active/inactive)
// No existe BBooleanOverride con campo value — usa BOverride directamente
```

El override se registra en `in8` (LEVEL_8) con `overrideExpiration = now() + duration`. Un Clock scheduler (parte del framework BControlPoint) verifica la expiración y llama `auto()` automáticamente al vencer.

### 46.5.2 Invocar override temporal desde SPA vía oBIX (CONFIRMADO parcialmente)

Para `BNumericWritable`:
```http
POST /obix/config/path/to/point/override/ HTTP/1.1
Content-Type: text/xml
x-niagara-csrfToken: <token>

<obj is="/obix/def/control:NumericOverride" xmlns="http://obix.org/ns/schema/1.0">
  <real name="value"    val="55.0"/>
  <reltime name="duration" val="PT1H"/>
</obj>
```

El `reltime` usa formato ISO 8601 duration: `PT1H` = 1 hora, `PT30M` = 30 minutos, `P1D` = 1 día.

Para `BBooleanWritable` (override con duración — parcialmente confirmado):
```http
POST /obix/config/path/to/point/active/ HTTP/1.1
Content-Type: text/xml
x-niagara-csrfToken: <token>

<obj is="/obix/def/control:Override" xmlns="http://obix.org/ns/schema/1.0">
  <reltime name="duration" val="PT5M"/>
</obj>
```

**TODO-1 honesto**: El body del override booleano con duración via oBIX NO fue verificado empíricamente contra una station real (solo el `set/` sin duración fue confirmado). El esquema XML inferido de la estructura oBIX expuesta y del patrón `BOverride`. Requiere validación en lab.

### 46.5.3 maxOverrideDuration — cap de site

```java
// BOverride tiene maxOverrideDuration
// Si el cliente pide duration > maxOverrideDuration,
// el framework trunca al máximo permitido (comportamiento INFERIDO)
```

**TODO-2 honesto**: No se verificó empíricamente si Niagara rechaza o trunca overrides con duration mayor al máximo. El comportamiento esperado (por analogía con BACnet y diseño conservador de safety) es truncar silenciosamente. Requiere lab.

### 46.5.4 Verificar override activo desde SPA

```javascript
// Vía oBIX — leer el slot overrideExpiration
const resp = await fetch('/obix/config/.../overrideExpiration/', {
    credentials: 'include'
});
const xml = await resp.text();
// Si val no es vacío, el override está activo y expira en esa fecha

// Vía BajaScript (status flag)
baja.Subscriber.make({
    added: function(ev) {
        const comp = ev.get();
        const status = comp.get('out').getStatus();
        const isOverridden = status.has(baja.Status.OVERRIDDEN);
        // OVERRIDDEN flag en BStatus (0x0020) indica override activo
    }
});
```

---

## 46.6 Batch writes — el problema real

### 46.6.1 Caso de uso: "All ON" en dashboard — 50 setpoints simultáneos

En un dashboard de pisos de un edificio, el operador puede querer: "apagar todos los extractores de emergencia en el piso 3" — potencialmente 50 puntos simultáneos.

**Pregunta 1**: ¿`Transaction.start()/end()` en JS?
**Respuesta**: NO. `javax.baja.sync.Transaction` (Bloque 41.1) es una clase Java server-side que wrappea `SyncBuffer`. No está expuesta como API directa al cliente JS. No hay equivalente de "transaction block" disponible desde BajaScript ni desde oBIX REST.

**Pregunta 2**: ¿NiagaraRPC batch?
**Respuesta**: SÍ, JSON-RPC 2.0 soporta batch (CONFIRMADO Bloque 29.9.6):

```javascript
// JSON-RPC 2.0 batch — array de calls
const batchPayload = [
    { jsonrpc: "2.0", method: "writePoint", params: { ord: "slot:/path1", value: 1.0 }, id: 1 },
    { jsonrpc: "2.0", method: "writePoint", params: { ord: "slot:/path2", value: 1.0 }, id: 2 },
    // ... hasta N calls
];

const resp = await fetch('/rpc/myPoints/writePoint', {
    method: 'POST',
    credentials: 'include',
    headers: {
        'Content-Type': 'application/json',
        'x-niagara-csrfToken': csrfToken
    },
    body: JSON.stringify(batchPayload)  // array → batch RPC
});

// Response: array de responses en el mismo orden
const results = await resp.json();
// results[0]: { jsonrpc: "2.0", result: {...}, id: 1 }
// results[1]: { jsonrpc: "2.0", error: {...}, id: 2 }  // error parcial posible
```

**IMPORTANTE**: El batch NiagaraRPC requiere un handler custom registrado en un módulo Niagara. NO existe un endpoint `/rpc/points/write` out-of-the-box. La distribución Honeywell no expone un handler de write de puntos genérico vía RPC (Bloque 29.9.3 — solo hay handlers para password, file, lexicon, log, registry).

### 46.6.2 Atomicity — la realidad de Transaction

`Transaction.start()/end()` es un buffer de `SyncOps` que se aplica en `commit()`. El `abortCommit(Exception)` está VACÍO (CONFIRMADO Bloque 41.1.2). Si el commit falla en la operación N de M:

```
Op 1: write point_1 = 1.0    ← APLICADA
Op 2: write point_2 = 1.0    ← APLICADA  
Op 3: write point_3 = 1.0    ← falla (RBAC denied, timeout, etc.)
commit() → abortCommit(e) → return  // VACÍO — no rollback

Estado final: point_1 y point_2 escritos, point_3 no → INCONSISTENTE
```

**Conclusión sobre Transaction batch**: `Transaction` es viable para agrupar writes en un solo SyncBuffer, pero NO provee atomicidad. Si se requiere "all-or-nothing":
- La SPA debe validar TODOS los permisos antes de comenzar (pre-check)
- O usar compensación manual (re-write de los ya aplicados si alguno falla)
- O aceptar que el batch es best-effort y reportar el estado individual de cada write

### 46.6.3 Batch oBIX — NO soportado

oBIX REST no tiene semantica de batch nativa. Cada `POST /set/` es un request HTTP independiente. Para 50 puntos:

**Opción A — 50 requests paralelos**:
```javascript
const writeAll = points.map(p =>
    fetch(`/obix/${p.path}/set/`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'text/xml',
            'x-niagara-csrfToken': csrfToken
        },
        body: `<real val="${p.value}" xmlns="http://obix.org/ns/schema/1.0"/>`
    })
);
const results = await Promise.allSettled(writeAll);
// Analizar cuáles fallaron via results[i].status === 'rejected'
```

**Riesgo**: 50 requests paralelos pueden saturar el Jetty thread pool (default 200 threads, pero si la station tiene carga preexistente). Recomendado: hacer en lotes de 10 con control de concurrencia.

**Opción B — BOX BajaScript multiple invokes**:
```javascript
// BajaScript debouncea/batcha mensajes en el WebSocket automáticamente
// (BoxMessageRelay debounce 10ms — Bloque 22)
const writes = points.map(p => {
    const comp = resolvedComponents.get(p.path);
    return comp.invoke('set', baja.Double.make(p.value));
});
await Promise.allSettled(writes);
```

El debounce de BOX (10ms, confirmado Bloque 22) agrupa múltiples `invoke()` en el mismo BoxFrame, reduciendo overhead. Pero el server-side aplica cada write independientemente.

### 46.6.4 BBrokerChannel.invoke — batch implícito

`BBrokerChannel` (Bloque 41.4.2) tiene el comando `invoke` que puede ser llamado múltiples veces dentro del mismo BOX frame (batched por el debounce client-side). El server procesa cada invoke en el orden de llegada, sin garantía de atomicidad entre ellos.

**Respuesta de Transaction batch — ¿viable o no?**:

> **Transaction batch vía NiagaraRPC custom ES VIABLE en términos de plomería** (un handler Java puede abrir `Transaction.start()`, aplicar N SyncOps, y hacer `Transaction.end()`). **PERO la atomicidad es nominal** — `abortCommit()` vacío significa que errores mid-batch dejan estado inconsistente. Para casos donde la atomicidad importa (safety, contabilidad de energía), la SPA DEBE pre-validar + implementar compensación, o usar un handler server-side que valide todo antes de aplicar.

---

## 46.7 Conflict resolution

### 46.7.1 "Last write wins" — el default de Niagara

Cuando dos usuarios escriben el mismo punto en la misma prioridad simultáneamente:

```
User A: POST set/ val=42.0  → in16 = 42.0
User B: POST set/ val=55.0  → in16 = 55.0

Resultado: el que llega último al BComponent.set() gana.
No hay lock, no hay versioning.
```

Esta es la semántica de `WritableSupport.setInStatusValue()` — last-write-wins sin mecanismo de concurrencia optimista.

### 46.7.2 ¿Hay optimistic locking via version stamp?

**Niagara nativo**: NO. No existe mecanismo de versioning stamp para writes a `BWritablePoint`. El ORD system no tiene ETag para componentes. oBIX tampoco lo expone.

**Reflow (evidencia Bloque 51)**: Bloque 51 menciona `BReflowSyncService` y un race condition AP-17, pero ese es un sync service específico de Reflow para sincronizar configuración entre Supervisor y SPA, NO para writes de puntos. El RFC 6902 JSON Patch mencionado en Bloque 51 es una decisión de diseño de Reflow, NO Niagara nativo.

**Conclusión**: No hay optimistic locking disponible de forma nativa. Para casos donde sea crítico (dos operadores pueden competir en el mismo setpoint), la SPA debe implementar su propia lógica: último que actuó gana + notificación al otro (via BOX subscription — el update del punto llegará a todos los subscribers).

### 46.7.3 Detección de conflicto post-write

La mejor defensa contra conflictos es una suscripción activa al punto:

```javascript
baja.Subscriber.make({
    changed: function(ev) {
        const comp = ev.get();
        const newVal = comp.get('out').getNumeric();
        const activeLevel = comp.get('activeLevel');
        
        // Si el valor cambió y no fue nosotros quien escribió:
        if (newVal !== myLastWrittenValue) {
            notifyUser('El setpoint fue modificado por otro usuario: ' + newVal);
        }
    }
}).subscribe(myPoint);
```

Con BOX subscriptions activas, la SPA recibe inmediatamente el cambio producido por cualquier otro actor (otra SPA, Workbench, otro nivel de prioridad, expiración de override).

---

## 46.8 CSRF rotation per write — análisis de seguridad

### 46.8.1 El token es session-scoped, NO rota por request

**CONFIRMADO** Bloque 47.4.2: El CSRF token es session-scoped. Una sesión HTTP (JSESSIONID) tiene un único token CSRF que vive mientras la sesión esté activa.

Esto significa:
- Un atacante que capture el CSRF token puede usarlo para cualquier write durante el lifetime de la sesión
- NO hay protection adicional por request (no es Double Submit Cookie con rotación)
- El riesgo de leak vía XHR cross-origin está mitigado por CORS ausente en Niagara nativo (Bloque 47.2.1) — si la SPA está co-localizada, el mismo-origen evita el leak

### 46.8.2 Header name exacto

```
x-niagara-csrfToken
```

(CONFIRMADO en bytecode `CsrfProtectedFilter.class` — Bloque 29.3.3, 18.5.1, 47.4.4). Lowercase, sin mayúsculas. Algunos frameworks de HTTP client normalizan headers a Title-Case — verificar que el header llegue exactamente así.

### 46.8.3 ¿GETs destructivos sin CSRF?

`CsrfProtectedFilter` exime GET de CSRF (CONFIRMADO Bloque 47.4.3). Esto significa que un módulo custom que diseñe endpoints destructivos como GETs (pattern anti-security documentado en Bloque 51.3.3 como "AP-10") no tendrá protección CSRF. Para writes de puntos legítimos, SIEMPRE usar POST — que está cubierto.

### 46.8.4 Interceptor recomendado para SPA

```javascript
// Axios interceptor para writes (recomendado)
axios.interceptors.request.use(config => {
    if (['post', 'put', 'delete', 'patch'].includes(config.method?.toLowerCase())) {
        config.headers['x-niagara-csrfToken'] = csrfTokenStore.get();
    }
    return config;
});

// Recovery si el token expiró (nueva sesión)
axios.interceptors.response.use(
    res => res,
    async err => {
        if (err.response?.status === 403) {
            const body = await err.response.text?.() || '';
            if (body.includes('csrf')) {
                // Re-autenticar y obtener nuevo token
                const newToken = await reAuth();
                csrfTokenStore.set(newToken);
                // Reintentar con nuevo token
                err.config.headers['x-niagara-csrfToken'] = newToken;
                return axios.request(err.config);
            }
        }
        return Promise.reject(err);
    }
);
```

---

## 46.9 RBAC en writes — enforcement server-side

### 46.9.1 ¿Quién decide si el write es autorizado?

El enforcement de permisos en writes ocurre server-side en Niagara, nunca en el cliente. La SPA puede pre-verificar para UX, pero la decisión real la toma el servidor.

**Flujo de autorización para oBIX write** (INFERIDO de Bloque 29.7 + Bloque 11):

```
HTTP POST /obix/config/.../set/
 → Filter chain (TridiumSecurityFilter → AddSubjectFilter)
 → ObixServlet
   → Resolve BComponent from ORD
   → BPermissions check: user tiene "operatorWrite" sobre el component?
     │
     ├── YES → proceed → BActionInvocation → BNumericWritable.set(value)
     └── NO  → 403 Forbidden
```

### 46.9.2 Granularidad de permisos

**BPermissions** tiene 6 bits (Bloque 11): `r` (read), `w` (write), `i` (invoke), `R` (Admin read), `W` (Admin write), `I` (Admin invoke).

Para writes de puntos:
- La acción `set()` requiere **write** (`w`) sobre el componente objetivo
- La acción `emergencyOverride()` puede requerir permisos elevados (Bloque 24.16 menciona que LEVEL_1 debe ser restringido por RBAC)
- La acción `override()` requiere **invoke** (`i`) o **write** (`w`) — a confirmar en lab (TODO-3)

**GOTCHA 46-7 — LEVEL_1 y RBAC**: Por convención de safety en BAS, LEVEL_1 (Manual Life Safety) DEBERÍA estar restringido a roles de supervisor de seguridad. Niagara no enforza esto automáticamente — es responsabilidad del commissioning configurar los BPermissions correctamente. Si la SPA expone un botón "Emergency Override" accesible a todos los operadores, hay un riesgo de seguridad real.

### 46.9.3 Error responses RBAC

| Escenario | HTTP status | Mensaje |
|---|---|---|
| No autenticado | 401 | Challenge WWW-Authenticate: HELLO |
| Autenticado pero sin `w` sobre componente | 403 | "User has no permissions to access view" (OrdServlet) |
| Acción no encontrada (op incorrecta) | 200 + `<err>` | oBIX: "Cannot translate: ..." |
| Punto no existe | 404 | DefaultServlet fallback |
| CSRF faltante | 403 | "csrf.token.verify.error" |

### 46.9.4 Slot-level permissions (RBAC method-level)

Bloque 30 documenta RBAC method-level sobre slots. Para puntos writable:
- El slot `in8` puede tener categoria diferente al `in1` para separar quién puede hacer override vs emergency
- La categoría de permisos se hereda del container parent si no está explícitamente asignada

**Práctica recomendada**: asignar la categoría `operatorWrite` al container `Drivers` y `safetyWrite` a los puntos críticos que requieren LEVEL_1.

---

## 46.10 Antipatterns y gotchas consolidados

### AP-46-1 — Write a un nivel de prioridad sin entender el estado actual

Escribir en LEVEL_16 cuando LEVEL_8 está activo → el punto NO cambia (LEVEL_8 gana). La SPA debe leer el `activeLevel` antes de escribir para saber si su write tendrá efecto.

```javascript
// ANTES de escribir, verificar qué nivel está activo:
const activeLevel = comp.get('activeLevel');
if (activeLevel < 16) {
    // Hay un override activo. El write a LEVEL_16 no tendrá efecto visible.
    notifyUser('Override activo en nivel ' + activeLevel + '. El cambio no surtirá efecto.');
}
```

### AP-46-2 — Override sin duration → override permanente hasta auto() manual

`emergencyOverride` y `override` sin `duration` → el override persiste indefinidamente hasta que alguien llame `auto()`. En un dashboard con turnos de personal, esto puede dejar un punto bloqueado durante días si el operador no recuerda relinquish.

**Recomendación**: siempre incluir `duration` en overrides de SPA. Default razonable: 4-8 horas (duración de un turno).

### AP-46-3 — Batch oBIX con Promise.all() sin rate limiting

50 requests en paralelo puede:
1. Saturar el Jetty thread pool (default 200 threads pero compartidos con todas las requests)
2. Activar el DoSFilter si está configurado (Bloque 29.1.4 — off by default, pero sitios seguros lo activan)
3. Causar backpressure en el BACnet driver subyacente si los puntos son proxied BACnet

**Recomendación**: batch en grupos de 5-10 con throttling:
```javascript
async function batch_write(writes, concurrency = 5) {
    for (let i = 0; i < writes.length; i += concurrency) {
        const batch = writes.slice(i, i + concurrency);
        await Promise.allSettled(batch.map(w => w()));
    }
}
```

### AP-46-4 — Ignorar el status del out tras el write

El body de response de oBIX `set/` incluye el status flags del punto. Un status `{fault}` o `{stale}` indica que el write fue aceptado por Niagara pero el ProxyExt downstream tuvo problemas (device offline, Modbus timeout):

```javascript
const resp = await obix_write_numeric('/obix/config/.../set/', 42.0);
if (resp.includes('{fault}') || resp.includes('{stale}')) {
    notifyUser('Write aceptado pero el dispositivo no confirmó. Estado: fault/stale.');
}
```

### AP-46-5 — CSRF token en localStorage (vulnerabilidad XSS)

`localStorage` es accesible desde cualquier script en el mismo origen. Si la SPA tiene XSS, el atacante lee el token y puede hacer writes como el usuario. Almacenar el CSRF token en una variable de closure JS (módulo ES6 o IIFE) es más seguro.

### AP-46-6 — Escribir via PUT a slots del árbol oBIX

`PUT` a `/obix/config/.../out/` o a `/obix/config/.../in8/` retorna `200 OK` pero con `<err>Cannot translate`. El write no ocurre. Solo funciona `POST` a los `<op>` expuestos (Bloque 46.2.2). Error silencioso que confunde a desarrolladores nuevos.

### AP-46-7 — Asumir que Transaction batch es ACID

`Transaction.start()/end()` en Java server-side NO provee rollback. `abortCommit()` está vacío. Si el commit falla en la operación N, las operaciones 1..N-1 ya están aplicadas permanentemente. No confiar en que "si algo falla, todo se deshace" (CONFIRMADO Bloque 41.1.2).

### AP-46-8 — Boolean override sin diferenciar active/inactive

Para `BBooleanWritable`, un cliente genérico que quiere "escribir con override por 1 hora" necesita saber si el valor deseado es true (→ `active/`) o false (→ `inactive/`). No existe una op `override` con `value` en BBooleanWritable como sí existe en BNumericWritable. La lógica condicional es obligatoria.

### AP-46-9 — No leer la priority array antes de relinquish

Llamar `auto()` sin saber qué otros niveles están activos puede:
1. Relinquish LEVEL_8 → punto cae a LEVEL_4 (schedule automático activo) — resultado esperado pero no visualizado
2. O cae a LEVEL_16 → setpoint base que puede estar mal configurado
3. O cae a FALLBACK → valor estático que puede ser peligroso

Antes de relinquish, la SPA debe mostrar al operador "al liberar el override, el punto quedará controlado por <fuente_del_próximo_nivel>".

---

## 46.11 TODOs honestos

**TODO-1** — Verificar el body XML del override booleano con duration: El esquema `<obj is="control:Override"><reltime duration.../>` fue inferido del modelo oBIX y de la estructura BOverride. No fue confirmado empíricamente contra una station real. La diferencia con el Numeric (que usa `control:NumericOverride` con campo `value`) hace posible que el Boolean no acepte el mismo patrón.

**TODO-2** — Verificar comportamiento cuando duration > maxOverrideDuration: ¿Niagara rechaza el request (403/error oBIX) o trunca silenciosamente al máximo? El diseño conservador sugiere truncar, pero puede ser un error explícito.

**TODO-3** — Confirmar qué BPermissions bit requiere `override()` vs `set()`: La intuición es que `override()` necesita `invoke` (`i`) además de `write` (`w`), pero la action dispatcher puede solo chequear `w`. Requiere lab con usuario que tenga solo `r` o solo `w`.

**TODO-4** — Confirmar si `BBrokerChannel.invoke` soporta batch de N calls en un solo frame BOX: El frame BOX puede contener múltiples mensajes en el array `m[]` (Bloque 22), pero si `invoke` solo acepta uno por vez o si puede recibir un array de invocaciones no está verificado.

**TODO-5** — RBAC enforcement en LEVEL_1 (emergencyOverride): ¿El framework Niagara enforza automáticamente una permission extra para writes en `in1`, o es solo convención documentada? Requiere verificar `WritableSupport` en bytecode para ver si hay check de permiso diferenciado por nivel.

**TODO-6** — Path B (BajaScript `comp.set()`) — confirmar qué nivel de prioridad usa: Se asumió LEVEL_16 basado en la convención de `set()`, pero no se verificó qué slot concreto llena `comp.set()` en el bytecode de `WritableSupport`.

---

## 46.12 Próximos bloques — impacto y refinamiento

### Bloque 47 (Bootstrap headless) — refinamientos desde 46

- **CSRF en oBIX**: Confirmado que se necesita en POST oBIX. El patrón de Bloque 47.4.4 aplica directamente.
- **Auth**: oBIX acepta Basic Auth directamente sin SCRAM. Para producción usar SCRAM + session cookie.
- **Proxy**: El path oBIX (`/obix/*`) pasa por el mismo Jetty → mismo requisito de same-origin o reverse proxy.

### Bloque 48 (RBAC visibility en frontend)

- La SPA puede pre-verificar permisos antes de mostrar controles de write
- `BReflowUserCommands.getRoles()` en Reflow retorna los roles del usuario (Bloque 51)
- La acción `canAcknowledgeAlarms` en BReflowAlarmCommands usa `BBoolean` — patrón similar para `canWrite` (TODO: verificar si Niagara expone un endpoint equivalente para puntos)

### Bloque 49 (Facets, i18n y formatting)

- Los facets de un BNumericWritable incluyen `min`, `max`, `units`, `precision` — la SPA debe respetarlos en el UI input para evitar writes fuera de rango que la station rechaza silenciosamente
- `display` en la respuesta oBIX ya aplica facets (ej. `"42,0 {ok} @ def"` con coma decimal por locale) — parsearlo requiere entender el locale de la station

### Bloque 42 (Subscriber lifecycle)

- La subscripción BOX al punto escrito permite confirmar que el write llegó al device (status cambia de `{overridden}` a `{ok}` una vez el ProxyExt confirma)
- La SPA debe subscribirse ANTES de escribir para capturar el round-trip completo

---

## 46.13 Tabla resumen de paths de write — decision matrix

| Criterio | Path A: oBIX REST | Path B: BajaScript BOX | Path C: NiagaraRPC custom |
|---|---|---|---|
| Confirmación empírica | SÍ — confirmado vs station real | PARCIAL — API documentada, no probada end-to-end | NO — requiere módulo custom |
| Same-origin requerido | NO — funciona cross-origin con Basic Auth o SCRAM+CORS-proxy | SÍ — `location.host` hardcoded | NO |
| Priority levels accesibles | 3 bands (1, 8, 16) | Todos los 16 | Todos los 16 (si handler los expone) |
| Batch nativo | NO — N requests independientes | Implícito via BOX debounce | SÍ — JSON-RPC 2.0 batch |
| Atomicidad batch | N/A | N/A (best-effort) | NO — Transaction.abortCommit() vacío |
| Complejidad de setup | BAJA | ALTA (requiere bs.built.min.js, same-origin) | MUY ALTA (módulo custom, firma, deploy) |
| Override temporal | SÍ (body NumericOverride/Override) | SÍ (BNumericOverride/BRelTime) | Depende del handler |
| CSRF requerido | SÍ — en POST | NO — BOX valida en /box POST | SÍ — en POST /rpc/* |
| RBAC enforcement | SÍ — server-side automático | SÍ — server-side automático | Depende del handler |
| Mejor para | SPA cross-origin, integraciones simples | Dashboards same-origin con BajaScript | Writes atómicos, lógica de negocio server-side |

**Recomendación para SEJOFA**:
- MVP y primera iteración: **Path A (oBIX REST)** — confirmado empíricamente, minimal setup, funciona detrás del reverse proxy nginx documentado en Bloque 47.2.4
- Dashboards con BajaScript (Approach A' del Bloque 47): **Path B** cuando el runtime ya está cargado
- Writes complejos con validación de negocio: **Path C** con módulo custom solo si la complejidad lo justifica

---

## 46.14 ProxyExt write pipeline — lo que ocurre después del write

### 46.14.1 La cadena después de BWritablePoint.set()

Cuando la SPA escribe exitosamente (HTTP 200 en oBIX o invoke confirmado en BOX), el write en Niagara todavía debe propagarse al device físico. Este proceso es asíncrono en relación a la respuesta HTTP:

```
SPA → POST /obix/.../set/ → ObixServlet → BNumericWritable.set(42.0)
                                               ↓ (síncrono: actualiza in16, recalcula out)
                                           WritableSupport.getActiveLevel() = LEVEL_16
                                           out = 42.0, status = OK, flag OVERRIDDEN si aplica
                                               ↓ (asíncrono: ProxyExt pipeline)
                                           BAbstractProxyExt.onExecute(out, context)
                                               ↓ (tuning policy check)
                                           if (timeSince > minWritePeriod && delta > deadband):
                                               writeToDevice(42.0)
                                               ↓
                                           BACnet/Modbus/LON/etc → device físico
```

**Respuesta HTTP inmediata vs confirmación de device**: La respuesta HTTP 200 del oBIX `set/` confirma que Niagara aceptó el write. NO confirma que el device físico lo aplicó. El device puede estar offline, el bus saturado, o la tuning policy puede haber deferred el write.

### 46.14.2 Tuning policy — cuándo se retrasa el write al device

```java
// BAbstractProxyExt pseudo-lógica (Bloque 24.18)
if (newValue == lastWriteValue) return;  // dedup — NO write si mismo valor

timeSince = now() - lastWriteTime;
if (timeSince < minWritePeriod) {
    queue(newValue);  // write diferido
} else if (abs(newValue - lastWriteValue) < deadband) {
    return;  // delta demasiado pequeño — NO write
} else {
    writeToDevice(newValue);
    lastWriteTime = now();
}
```

Propiedades configurables en el ProxyExt:
- `minWritePeriod`: mínimo intervalo entre writes (ej. 1s para Modbus). Si la SPA escribe más rápido, los writes intermedios se descartan o encolan.
- `deadband`: cambio mínimo necesario para escribir al device (ej. 0.5°C). Writes dentro del deadband son silenciosamente ignorados al device.
- `maxWritePeriod`: máximo tiempo sin confirmar write (fuerza re-envío periódico).

**GOTCHA 46-11 — Write aceptado por Niagara, ignorado por ProxyExt**: Si la SPA escribe `42.0` y el punto ya tiene `42.0` en `out`, el ProxyExt hace dedup y NO escribe al device. El `out` refleja el valor "deseado" de Niagara, no necesariamente el valor confirmado por el device.

### 46.14.3 Verificar que el write llegó al device

Para confirmar el write al device, la SPA debe verificar el status del `out` después del write:

```javascript
// Vía BOX subscription (recomendado para UX reactivo)
const sub = baja.Subscriber.make({
    changed: function(ev) {
        const comp = ev.get();
        const out = comp.get('out');
        const status = out.getStatus();
        
        if (status.has(baja.Status.FAULT)) {
            notifyUser('Error al escribir en el dispositivo físico: fault');
        } else if (status.has(baja.Status.DOWN)) {
            notifyUser('Dispositivo offline — write en cola');
        } else if (status.has(baja.Status.STALE)) {
            notifyUser('Valor stale — dispositivo no confirmó');
        } else if (!status.hasAny()) {
            notifyUser('Write confirmado por el dispositivo');
        }
    }
});
sub.subscribe(writtenComponent);
```

Vía oBIX REST (polling):
```javascript
// Poll el out/ después de escribir (poco elegante vs BOX subscription)
await new Promise(r => setTimeout(r, 500));  // esperar 500ms
const check = await fetch('/obix/config/.../out/', { credentials: 'include' });
const xml = await check.text();
// Buscar display — "{ok}" vs "{fault}" vs "{down}" vs "{stale}"
const display = xml.match(/display="([^"]+)"/)?.[1];
```

### 46.14.4 Status flags en BStatus para writes

Los flags más relevantes post-write (Bloque 24.18):

| Status flag | Valor hex | Significado para write |
|---|---|---|
| OK (none) | 0x0000 | Write llegó al device sin problemas |
| FAULT | 0x0004 | Error en el device (Modbus exception, BACnet error) |
| DOWN | 0x0008 | Device offline (conexión perdida) |
| STALE | 0x0010 | Dato viejo — device no respondió en timeout |
| OVERRIDDEN | 0x0020 | Override activo en algún nivel (informativo) |

---

## 46.15 oBIX como protocolo de escritura — tradeoffs completos

### 46.15.1 Por qué oBIX es el path recomendado para SPA cross-origin

1. **Confirmación empírica directa**: El protocolo fue testeado en lab contra una station N4.14.0 real con `BNumericWritable` y `BBooleanWritable`. Los curl commands del cheat sheet funcionan con cero configuración adicional en la station (oBIX está habilitado por default).

2. **No requiere BajaScript**: No hay dependencia de `bs.built.min.js`, RequireJS, ni el bootstrap BOX. Un cliente HTTP simple (fetch, axios, curl) es suficiente.

3. **Cross-origin con auth standard**: Basic Auth funciona sin CORS (el servidor no bloquea OPTIONS). Con la SPA detrás de reverse proxy, las cookies de sesión SCRAM también funcionan.

4. **Respuesta síncrona**: El HTTP 200 llega después de que Niagara actualizó el `out`. No hay race condition en la lectura inmediata post-write.

### 46.15.2 Limitaciones conocidas de oBIX en este contexto

1. **Solo 3 niveles de prioridad**: oBIX no expone acceso a LEVEL_3..7 ni LEVEL_9..15.
2. **XML vs JSON**: La SPA debe parsear XML (DOMParser o regex). Más verboso que JSON.
3. **No hay batch nativo**: Cada write es un HTTP request independiente.
4. **Legado**: oBIX es un protocolo de 2006 (Open Building Information Xchange). Aunque funciona perfectamente en N4.14, no es el path "moderno" que Tridium está promoviendo (favorecen BOX/BajaScript).
5. **OBIX NO es oBIX**: Existe también un driver "obix" en Niagara para conectar a servidores oBIX externos. No confundir el servlet `/obix/` (que expone la station como servidor oBIX) con el driver `obix-rt.jar` (que hace la station actuar como cliente oBIX contra servidores externos).

### 46.15.3 Alternativa REST nativa Niagara — ORD resolution

El `OrdServlet` (`/ord/*`) también puede usarse para acceder a componentes, pero:
- No tiene endpoints predefinidos para invocar actions (`set`, `override`)
- Sirve vistas HTML por defecto, no JSON/XML estructurado
- Requiere un `BServletView` agent registrado para el componente específico

oBIX es el único protocolo REST nativo en Niagara que tiene una interfaz de write de puntos bien definida y documentada.

---

## Fuentes primarias y referencias cruzadas

| Afirmación | Fuente empírica | Bloque ref |
|---|---|---|
| `set()` escribe en LEVEL_16, respuesta `@ def` | `obix-write-protocol-empirico.md` — curl vs station real | — |
| BooleanWritable: `active/inactive` no llevan value | `obix-write-boolean.md` — inspección estructura XML | — |
| CSRF en POST oBIX | `CsrfProtectedFilter.class` — aplica a todos los métodos POST | Bloque 29.3.3 |
| `x-niagara-csrfToken` header exacto | bytecode CsrfProtectedFilter | Bloque 18.5.1 + 47.4.4 |
| `abortCommit()` vacío — no rollback | `SyncBuffer.class` bytecode via javap | Bloque 41.1.2 |
| NiagaraRPC batch support | `NiagaraRpcServlet` string "Error invoking multi RPC" | Bloque 29.9.6 |
| BBrokerChannel.invoke — fox channel | BFoxChannelRegistry bytecode | Bloque 41.4.2 |
| CSRF no requerido en frames BOX | `CsrfProtectedFilter` vs BoxWebSocketServlet flow | Bloque 29.10.4 + 47.5.1 |
| Priority 16 = `in16`, LEVEL_8 = `in8` | `BPriorityLevel` enum + `WritableSupport` | Bloque 24.16 |
| BOverride = value + duration + maxOverrideDuration | `BOverride.class` + `BNumericOverride.class` | Bloque 24.16 |
| `overrideExpiration` BAbsTime — Clock scheduler | `BNumericWritable` + `BControlPoint` clock subscription | Bloque 24.16 |
| Fallback activo cuando todos los in* = null | `WritableSupport.getActiveLevel()` lógica | Bloque 24.16 |
| BACnet priority 1..16 mapea directo a Niagara | BBacnetPriorityValue 13 choices, priority array property 87 | Bloque 23.2 |
| Session-scoped CSRF, no rota por request | `NiagaraWebSession` attribute storage | Bloque 47.4.2 |
| Boolean override con duración: body `control:Override` | Estructura oBIX `active` op + BOverride class | INFERIDO — TODO-1 |

---

## Gotchas numerados — resumen rápido

| # | Gotcha | Impacto |
|---|---|---|
| G46-1 | `in*` slots read-only vía oBIX PUT | Write silencioso falla (200 + `<err>`) |
| G46-2 | Boolean vs Numeric override: APIs distintas | Lógica condicional obligatoria por tipo |
| G46-3 | CSRF obligatorio en oBIX POST | 403 si se omite |
| G46-4 | `comp.set()` es LEVEL_16, no el nivel activo | Write no tiene efecto si hay override activo en nivel superior |
| G46-5 | Convención LEVEL_8 vs LEVEL_10 — depende de instalación | Hardcodear nivel sin consultar commissioning = riesgo |
| G46-6 | oBIX solo expone 3 bands de prioridad | No granularidad sobre todos los 16 niveles |
| G46-7 | LEVEL_1 debe restringirse por RBAC — Niagara no lo enforza automáticamente | Riesgo de safety si UI expone emergency a todos |
| G46-8 | Transaction.abortCommit() vacío — no ACID | Batch parcialmente aplicado en error |
| G46-9 | Override sin duration = permanente | Punto bloqueado indefinidamente |
| G46-10 | Override Boolean: `active/inactive`, no `override(value, duration)` | API distinta a Numeric |
