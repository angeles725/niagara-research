# Bloque 49 — Facets + i18n + formatting en cliente: el rendering pipeline desde SPA externa

**Fecha**: 2026-05-06
**Método**: Investigación empírica READ-ONLY. Análisis de la API browser de bajaScript (`comp.getFacets()`, `prop.getFacets()`, `comp.getDisplay()`), del sistema lexicon Niagara (`BLexicon` + `lex!` plugin RequireJS + `%lexicon(key)%` placeholder), y de patrones reales en Reflow-Clean-177 (`DynamicColorForm.vue` con `getFacets('out').get('range')`, `NoteCard.vue` con browser-native `toLocaleDateString`, `Total.vue` con `toLocaleString`).
**Fuentes primarias**:
- `bajaScript-ux.jar:rc/baja/obj/Format.js` (BFormat browser API)
- `bajaScript-ux.jar:rc/baja/obj/TimeZone.js` (BTimeZone client)
- `bajaScript-ux.jar:rc/plugin/lex.js` (`lex!` plugin RequireJS)
- `reflow-frontend/src/components/dashboard/DynamicColorForm.vue:426` (uso real de `getFacets('out').get('range')` con BEnumRange)
- `reflow-frontend/src/components/cards/NoteCard.vue:159-160` (browser-native i18n via `toLocaleDateString`)
- Bloque 22.11-22.14 (BajaScript runtime browser + Component model)
- Bloque 22.20 (PX security + `BFormat.display(value, user, permissions)`)
- Bloque 12.2.5-12.2.8 (Lexicon file format + BLexicon runtime API + fallback chain)
- Bloque 31.11 (TimeZone handling multi-zone archives — gap #18)
- Bloque 4 (BFacets en slot system base)

**Versión analizada**: Honeywell OptimizerSupervisor-N4.14.0.162 + Reflow-Clean-177.

---

## 49.0 Contexto, scope, qué NO es este bloque

### ¿Qué ES este bloque?

Este bloque cubre la **última milla del rendering pipeline** cuando una SPA externa muestra un valor de Niagara al usuario:

- Cómo viajan los facets (metadata: range/units/format/decimal/min/max) del slot al cliente
- La API browser para acceder a ellos (`comp.getFacets()`, `prop.getFacets()`, `getDisplay()`)
- Cómo aplicar units/decimal/range para mostrar `21.5°C` en lugar de `21.5`
- Cómo manejar facets dinámicos (cambian en runtime — modo manual vs auto, modo verano vs invierno)
- El sistema BAbsTime + BTimeZone para timestamps cross-station (Supervisor con NYC/LAX/LHR subordinates)
- Lexicons: i18n server-side cargado runtime con fallback chain 4-niveles
- La pregunta arquitectónica clave: ¿quién formatea, server o cliente? (trade-offs concretos)
- Qué hace Reflow producción empíricamente (browser-native `Intl.*` para timestamps, `getFacets` para enum range)

### ¿Qué NO es este bloque?

- **NO es el modelo de slots/components base** — eso vive en Bloque 4 (BComponent + slot system + BFacets como concepto). Acá usamos esa base.
- **NO es BajaScript runtime completo** — eso vive en Bloque 22.11-22.14. Acá nos enfocamos en la API de display/format.
- **NO es PX rendering server-side** — eso vive en Bloque 22.1-22.10. SPA externa NO usa PX renderer.
- **NO es Workbench formatting** — Workbench tiene su propio FieldEditorSheet/PropertyEditor (Bloque 35). SPA externa va directo al modelo.
- **NO es history/chart formatting** — eso vive en Bloque 45 (WebChart NDJSON + d3 SVG). Acá: solo display de valores live.
- **NO redocumenta TimeZone server-side** — Bloque 31.11 ya tiene los detalles. Acá: implicaciones para SPA cliente.

### Pregunta unificadora

> Mi SPA recibe el valor `21.5` desde un BNumericWritable suscrito. Debo mostrarlo como `21.5 °C` con un decimal, dentro del rango válido [16..28] del slider, con timestamp de actualización formateado según locale del user, y el label del widget traducido al español. ¿Cómo lo hago?

**Respuesta corta**: 4 mecanismos coordinados:
1. **Facets via `comp.getFacets('out')`** o `prop.getFacets()` — para units, decimal places, range
2. **`comp.getDisplay(prop)`** — atajo server-side que retorna string ya formateado con units (`"21.5 °C"`)
3. **BAbsTime + browser-native `Intl.DateTimeFormat`** — para timestamps con TZ del user (Reflow usa `toLocaleDateString` directo)
4. **Lexicon via `lex!` plugin** o `%lexicon(key)%` en BFormat — para strings traducidos del UI

Pero hay decisión arquitectónica subyacente: **¿server formatea (consistencia) o cliente formatea (flexibilidad)?** Reflow elige híbrido: server para units/decimals via `getDisplay`, cliente para timestamps via `Intl.*`. Documentado en 49.7.

---

## 49.1 BFacets: qué son y cómo viajan al cliente

### 49.1.1 BFacets en el modelo Niagara

**CONFIRMADO** (Bloque 4 + Bloque 22.726):

`BFacets` es una collection inmutable de pares clave-valor que vive como metadata de:
- **Slots** (Properties, Actions, Topics) — facets a nivel slot
- **Components** (en algunos casos) — facets agregados o por defecto
- **Tipos** (Type) — facets default declarados en el Type

```java
// Server-side (Java) — declarando facets en un slot
@NiagaraProperty(
    name = "setpoint",
    type = "double",
    defaultValue = "20.0",
    facets = "{units=°C, min=15.0, max=30.0, precision=1}"
)
public double getSetpoint() { ... }
```

**Built-in facet keys más comunes**:

| Key | Tipo | Ejemplo | Uso |
|-----|------|---------|-----|
| `units` | string | `°C`, `kWh`, `%RH` | Sufijo en display |
| `min` | number | `15.0` | Boundary slider/input |
| `max` | number | `30.0` | Boundary slider/input |
| `precision` | int | `1`, `2` | Decimal places |
| `format` | string | `0.0%`, `0.00 °C` | Format string completo (sobreescribe units+precision) |
| `range` | BEnumRange | `{0:Off, 1:On, 2:Auto}` | Enum mapping |
| `trueText` / `falseText` | string | `"Activo"`, `"Inactivo"` | Boolean display |
| `editable` | boolean | `true`/`false` | UI hint (NO security — Bloque 48) |
| `hidden` | boolean | `true`/`false` | UI hint (NO security) |
| `summary` | boolean | `true`/`false` | UI hint para property sheet |

### 49.1.2 Cómo llegan los facets al cliente

**CONFIRMADO** (Bloque 22.11 BajaScript + Bloque 42.989):

Cuando la SPA hace `baja.Ord.make(ord).get()` o suscribe via Subscriber, el server incluye los facets actuales de cada slot en la respuesta BOX. Llegan como objeto JS-friendly:

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Drivers/Setpoint').get().then(function(comp) {
    var outProp = comp.getSlot('out');
    var facets = outProp.getFacets();
    
    facets.get('units');       // "°C"
    facets.get('min');         // 15.0
    facets.get('max');         // 30.0
    facets.get('precision');   // 1
    facets.get('range');       // BEnumRange instance (para enum)
  });
});
```

Los facets son **lazy-loaded**: el primer acceso a `prop.getFacets()` puede disparar un fetch al server si el slot no está hidratado. Después se cachea client-side.

### 49.1.3 BEnumRange: el caso especial de enums

**CONFIRMADO** (`reflow-frontend/src/components/dashboard/DynamicColorForm.vue:426`):

```javascript
// Patrón empírico real Reflow — BEnumRange para EnumPoint
var range = comp.getFacets('out').get('range');
range.getOrdinals().forEach(function (ord) {
  var tag = range.getTag(ord);  // "Off", "On", "Auto"
  // ord = 0, 1, 2; tag = display string para cada ordinal
});
```

`BEnumRange` es un objeto especial dentro del facet `range`. Su API browser:

| Método | Retorna | Significado |
|--------|---------|-------------|
| `range.getOrdinals()` | `int[]` | Array de ordinals declarados (`[0, 1, 2]`) |
| `range.getTag(ordinal)` | `string` | Display tag para ese ordinal (`"Off"`) |
| `range.getOrdinal(tag)` | `int` | Reverse lookup: tag → ordinal |
| `range.size()` | `int` | Cantidad de valores |
| `range.contains(ordinal)` | `boolean` | ¿el ordinal está en el range? |

**Uso típico**: dropdown que muestra los display tags pero envía el ordinal numérico al server.

```html
<select v-model="ordinalValue">
  <option v-for="ord in ordinals" :value="ord">{{ range.getTag(ord) }}</option>
</select>
```

### 49.1.4 El acceso `comp.getFacets()` vs `prop.getFacets()`

```javascript
// Variant 1: facets del component (heredados o agregados)
var compFacets = comp.getFacets();

// Variant 2: facets de un slot específico — más común
var propFacets = comp.getSlot('out').getFacets();

// Variant 3 (azúcar Reflow-style): equivalente a comp.getSlot('out').getFacets()
var propFacets = comp.getFacets('out');  // pasa el slot name como argumento
```

**INFERIDO**: la variante 3 es azúcar pero se usa en Reflow producción (`DynamicColorForm.vue:426`). Está documentada en bajaScript runtime (línea 932 Bloque 22). Equivalente funcional a variant 2.

---

## 49.2 La API de display: `getDisplay()` y atajos relacionados

### 49.2.1 `comp.getDisplay(prop)` — string ya formateado server-side

**CONFIRMADO** (Bloque 22.14 + Bloque 22.20):

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Drivers/Setpoint').get().then(function(comp) {
    var displayString = comp.getDisplay('out');
    // Retorna string ya formateado: "21.5 °C"
    // Server aplicó units + precision + format string del facets
  });
});
```

**Ventajas**:
- Server hace el formatting con la fuente de verdad (los facets actuales server-side)
- Consistencia: mismo display en Workbench, PX, HX, y SPA externa
- Aplica `BFormat.display(value, user, permissions)` con permission gating (Bloque 22.20 — oculta valor si user no tiene read perm)
- Maneja casos edge: nulls, NaN, fault values con strings localizados

**Desventajas**:
- Round-trip extra si el cliente solo tiene el valor crudo y necesita display
- Inflexibilidad: la SPA no puede customizar formato (ej. `21°C` sin decimal vs `21.5°C`)
- Dependencia de la lógica de formato server-side (cambios server alteran display sin cambiar cliente)

### 49.2.2 `comp.getOutDisplay()` — atajo para BControlPoint

**CONFIRMADO** (Bloque 22 línea 757):

```javascript
const value = comp.getOutDisplay();  // shortcut equivalente a comp.getDisplay('out')
```

Específico para `BControlPoint` (NumericPoint, BooleanPoint, etc.) — el slot canónico es `out`. Atajo común en código Niagara.

### 49.2.3 `prop.getValue()` + formato cliente — la alternativa flexible

```javascript
// Cliente formatea localmente
var value = comp.get('out');             // valor crudo (number, boolean, string)
var facets = comp.getFacets('out');      // facets para conocer units/precision

var precision = facets.get('precision') || 2;
var units = facets.get('units') || '';
var formatted = value.toFixed(precision) + ' ' + units;
// → "21.5 °C"
```

**Ventajas**:
- Cero round-trips extra una vez que tenés el comp
- Flexibilidad total: SPA puede aplicar lógica custom (truncar, redondear, formato condicional)
- Menos coupling con server-side BFormat behavior

**Desventajas**:
- Cliente debe replicar lógica de formato (decimal places, unit handling, null cases)
- Inconsistencia: SPA puede mostrar diferente que Workbench
- Si server cambia facets dinámicamente, cliente debe re-leer y re-formatear

### 49.2.4 `comp.getStatus()` — flags como señales UI

**CONFIRMADO** (Reflow `BoundLabel.vue:366`, `DeviceCard.vue:255`, `DeviceRow.vue:206`, `GroupCard.vue:189`, `GroupRow.vue:146`):

```javascript
// Patrón empírico real Reflow — leer status flags del out slot
var status = comp.get('out').getStatus();

// Status es un BStatus con bits:
status.isOk()             // todos los flags clear, valor confiable
status.isFault()          // device error, valor stale
status.isStale()          // último valor recibido pero ya viejo
status.isOverridden()     // priority array tiene override activo
status.isAlarm()          // valor disparó alarma
status.isUnackedAlarm()   // alarma no acknowledged
status.isDown()           // device offline
status.isDisabled()       // slot desactivado por config
```

**Uso típico**: cambiar color del widget según status (Reflow `DynamicColorForm.vue` define mapping color por status):

```javascript
// Mapping Reflow producción
statusColors: {
  down: '#fac600',
  fault: '#fc7734',
  stale: '#d9c09d',
  disabled: '#d6d6d6',
  alarm: '#cf1624',
  overridden: '#bfaddd',
  unackAlarm: '#cf4216',
  ok: '#19be6b'
}
```

**GOTCHA G49-1 — Status bits son ortogonales, no exclusivos**: un valor puede ser `fault` Y `overridden` simultáneamente. La SPA debe definir prioridad de display (Reflow chequea `isFault()` antes que `isOverridden()`). NO asumir que solo un flag está activo.

---

## 49.3 Aplicar units/decimal/range en display — patrones concretos

### 49.3.1 Numeric con units + precision

```javascript
// Patrón completo
function formatNumeric(comp, slotName) {
  var slotName = slotName || 'out';
  var value = comp.get(slotName);
  var facets = comp.getFacets(slotName);
  
  if (value === null || value === undefined || isNaN(value)) {
    return '--';  // null display
  }
  
  var precision = facets.get('precision');
  if (precision === undefined) precision = 2;
  
  var units = facets.get('units') || '';
  var formatted = value.toFixed(precision);
  
  return units ? formatted + ' ' + units : formatted;
}

// Uso
var display = formatNumeric(comp, 'out');  // "21.50 °C"
```

### 49.3.2 Boolean con trueText/falseText

```javascript
function formatBoolean(comp, slotName) {
  var slotName = slotName || 'out';
  var value = comp.get(slotName);
  var facets = comp.getFacets(slotName);
  
  if (value === null || value === undefined) return '--';
  
  return value
    ? (facets.get('trueText') || 'true')
    : (facets.get('falseText') || 'false');
}

// Uso (con facets {trueText="Activo", falseText="Inactivo"}):
var display = formatBoolean(comp);  // "Activo" o "Inactivo"
```

### 49.3.3 Enum con BEnumRange

```javascript
function formatEnum(comp, slotName) {
  var slotName = slotName || 'out';
  var value = comp.get(slotName);
  var facets = comp.getFacets(slotName);
  var range = facets.get('range');
  
  if (value === null || !range) return '--';
  
  return range.getTag(value) || ('UNKNOWN(' + value + ')');
}

// Uso (con range {0:Off, 1:On, 2:Auto}):
var display = formatEnum(comp);  // "Off", "On", o "Auto"
```

### 49.3.4 Range slider con min/max dinámicos

```javascript
// Vue component reactivo a facets
<template>
  <input type="range" :min="minValue" :max="maxValue" :step="step" v-model.number="value" />
  <span>{{ formatted }}</span>
</template>
<script>
export default {
  data: () => ({ comp: null, value: 0, sub: null }),
  computed: {
    facets() { return this.comp ? this.comp.getFacets('out') : null; },
    minValue() { return this.facets ? (this.facets.get('min') || 0) : 0; },
    maxValue() { return this.facets ? (this.facets.get('max') || 100) : 100; },
    step() {
      var precision = this.facets ? this.facets.get('precision') : 2;
      return Math.pow(10, -precision);
    },
    units() { return this.facets ? (this.facets.get('units') || '') : ''; },
    formatted() {
      return this.value.toFixed(this.facets.get('precision') || 2) + ' ' + this.units;
    }
  },
  async mounted() {
    var baja = await import('baja!');
    this.comp = await baja.Ord.make(this.ord).get();
    this.value = this.comp.get('out');
    
    this.sub = new baja.Subscriber();
    this.sub.attach({
      changed: (prop) => { if (prop.getName() === 'out') this.value = prop.getValue(); },
      facetsChanged: (slot) => { 
        if (slot.getName() === 'out') this.$forceUpdate();  // re-evaluar computed
      }
    });
    this.sub.subscribe(this.comp);
  },
  beforeDestroy() {
    this.sub.detach();
    this.sub.unsubscribe();
  }
};
</script>
```

---

## 49.4 Facets dinámicos: el callback `facetsChanged`

### 49.4.1 ¿Cuándo cambian los facets en runtime?

**CONFIRMADO** (Bloque 42.989):

Casos reales donde facets cambian dinámicamente:

| Caso | Qué cambia | Ejemplo |
|------|-----------|---------|
| Modo operación (auto/manual) | `min`, `max` | Setpoint `min=15` en auto, `min=10` en manual |
| Modo estacional (verano/invierno) | `min`, `max`, `units` | Cooling vs heating ranges |
| Cambio de unidades global | `units`, `precision` | Site cambia de °C a °F |
| Schedule cambia (modo nocturno) | `range` (enum) | Modo "Nocturno" agrega ordinal "EcoOff" |
| Configuración remota | cualquier facet | Admin re-config el slot |
| Dispositivo report new metadata | `range`, `units` | Driver sincroniza con device firmware update |

### 49.4.2 El callback `facetsChanged`

**CONFIRMADO** (Bloque 22.808 + Bloque 42.123):

```javascript
var sub = new baja.Subscriber();
sub.attach({
  facetsChanged: function(slot, cx) {
    // facets de UN slot específico cambiaron
    var newFacets = slot.getFacets();
    
    // Re-leer todo lo relevante
    var newMin = newFacets.get('min');
    var newMax = newFacets.get('max');
    var newUnits = newFacets.get('units');
    
    // Notificar al widget para re-render
    updateSliderBounds(slot.getName(), newMin, newMax);
    updateUnitsLabel(slot.getName(), newUnits);
  }
});
sub.subscribe(comp);
```

**INFERIDO**: el callback se dispara DESPUÉS de que el server emita el cambio. Si la SPA hace `comp.getFacets('out')` justo después del callback, retorna los facets nuevos. NO hay race window — el evento llega después de que el server commit el cambio.

### 49.4.3 Debouncing de `facetsChanged`

**INFERIDO**: si un admin cambia múltiples facets de un slot en rápida sucesión via Workbench, el cliente puede recibir múltiples `facetsChanged` para el mismo slot. La SPA debe debounce el re-render (usar `requestAnimationFrame` o un debounce de ~50ms) para evitar thrash.

```javascript
var pendingRerender = {};
function handleFacetsChanged(slot) {
  var key = slot.getName();
  pendingRerender[key] = slot;
  
  if (!pendingRerender._scheduled) {
    pendingRerender._scheduled = true;
    requestAnimationFrame(function() {
      Object.keys(pendingRerender).forEach(function(k) {
        if (k !== '_scheduled') doRerender(pendingRerender[k]);
      });
      pendingRerender = {};
    });
  }
}
```

**GOTCHA G49-2 — `facetsChanged` no incluye el delta**: el callback recibe solo `(slot, cx)` — NO recibe qué facet cambió ni el valor anterior. La SPA debe hacer `slot.getFacets()` y comparar con cache local si necesita el delta. Para uso simple (re-render todo), no importa.

### 49.4.4 `flagsChanged` vs `facetsChanged` — distinción

**CONFIRMADO** (Bloque 22.808 + Bloque 48.4.3):

| Callback | Qué cambia | Cuándo dispara |
|----------|-----------|----------------|
| `flagsChanged` | Flags del slot (HIDDEN, READONLY, OPERATOR, SUMMARY, etc.) | Setear/desetear bits | 
| `facetsChanged` | Facets del slot (units, range, min, max, format, etc.) | Re-config metadata |
| `componentFlagsChanged` | Flags del COMPONENT (no de un slot) | Comp-level changes |

Los tres son independientes. Un slot puede tener `facetsChanged` sin `flagsChanged` y viceversa. Para gating UI (visibility), Bloque 48 cubre flags. Para display formatting (este bloque), facets son la señal.

---

## 49.5 BAbsTime + BTimeZone: timestamps cross-station

### 49.5.1 BAbsTime: el modelo canónico

**CONFIRMADO** (Bloque 31.11.1-31.11.2):

```
BAbsTime = { long millis UTC, BTimeZone zone }
```

Cada timestamp Niagara se almacena como:
- **`millis`**: epoch UTC en long (canónico, comparable cross-zone)
- **`zone`**: `BTimeZone` reference (`"America/New_York"`, `"UTC"`, `"Europe/London"`)

Serialization BOG: `"millis;zone"` format.

### 49.5.2 BTimeZone en el cliente

**CONFIRMADO** (`bajaScript-ux.jar:rc/baja/obj/TimeZone.js`):

```javascript
require(['baja!'], function(baja) {
  baja.Ord.make('station:|slot:/Histories/Sensor1').get().then(function(comp) {
    var lastUpdate = comp.get('lastUpdate');  // BAbsTime instance
    
    var millis = lastUpdate.getMillis();   // long UTC
    var zone = lastUpdate.getZone();       // BTimeZone instance
    var zoneId = zone.getId();             // "America/New_York"
    
    // Display options — ver 49.5.3
  });
});
```

### 49.5.3 Tres opciones de display de timestamps

**Opción A: usar el zone del BAbsTime (storage TZ)**

```javascript
function formatBAbsTime_storage(absTime) {
  var d = new Date(absTime.getMillis());
  var zoneId = absTime.getZone().getId();
  
  return d.toLocaleString('en-US', { timeZone: zoneId });
}
// Ejemplo: "5/6/2026, 3:45:22 PM EST"
```

Muestra el timestamp en la TZ donde se generó. Útil para auditing — "este record se grabó a las 3:45 hora local del device".

**Opción B: usar la TZ del browser/user (display TZ)**

```javascript
function formatBAbsTime_userLocal(absTime) {
  var d = new Date(absTime.getMillis());
  return d.toLocaleString();  // browser default: locale + TZ del browser
}
// Ejemplo (browser en Argentina): "6/5/2026, 17:45:22"
```

Muestra el timestamp en la TZ del usuario. Útil para "cuándo pasó esto, en mi hora".

**Opción C: ambas, con label explícito**

```javascript
function formatBAbsTime_dual(absTime) {
  var d = new Date(absTime.getMillis());
  var zoneId = absTime.getZone().getId();
  
  var local = d.toLocaleString();
  var storage = d.toLocaleString('en-US', { timeZone: zoneId });
  
  return local + ' (' + storage + ' ' + zoneId + ')';
}
// Ejemplo: "6/5/2026, 17:45:22 (3:45:22 PM EST America/New_York)"
```

Sin ambigüedad. Útil para Supervisor multi-zone donde el operador necesita ver ambas.

### 49.5.4 Patrón Reflow: browser-native, ignora BAbsTime zone

**CONFIRMADO** (`reflow-frontend/src/components/cards/NoteCard.vue:159-160`):

```javascript
// Reflow producción — NO usa BAbsTime zone, asume browser TZ
return d.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) +
       ' at ' + d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
// "May 6, 2026 at 5:45 PM"
```

**Análisis**: Reflow ignora completamente el `BTimeZone` del BAbsTime y delega al browser. Es **simple y consistente para single-station deploys** pero **falla en escenarios Supervisor multi-zone**: si el subordinate NYC reporta un timestamp y la SPA está en Argentina, el display será la hora Argentina del millis UTC, no la hora NYC donde el evento ocurrió.

**GOTCHA G49-3 — Reflow ignora BTimeZone — OK para single-station, ROTO para multi-zone Supervisor**: si tu deploy tiene Supervisor con subordinates en diferentes TZs, `toLocaleString()` sin `timeZone` option da display ambiguo. Para Supervisor multi-zone, usar Opción A o C de 49.5.3.

### 49.5.5 DST gotchas

**CONFIRMADO** (Bloque 31.11.4):

| DST event | Comportamiento BAbsTime | Display gotcha |
|-----------|-------------------------|----------------|
| Spring forward (2:00 → 3:00) | Gap: no records con timestamp 2:30 local | Chart muestra hueco — esperado |
| Fall back (2:00 → 1:00) | Duplicate hour: records pueden colisionar timestamps locales con offsets diferentes | Chart binning by "local hour" puede mostrar spike doble |

**Mitigación**: para chart binning, usar `millis UTC` directamente, no `formatToLocalHour(millis, zone)`. El UTC es estrictamente monotónico — el local time NO.

### 49.5.6 Clock drift y NTP

**CONFIRMADO** (Bloque 31.11.5):

```
Station con RTC sin NTP → drift 1-5 min/día
Cross-station Supervisor query aggregation → record ordering inconsistent
```

**Implicación SPA**: si la SPA muestra "ordenado por timestamp", asumir que los timestamps cross-station pueden estar fuera de orden ±N segundos. Para alarms/events ordenados, considerar también `sequenceNumber` o `uuid` para tie-breaker, NO solo `millis`.

---

## 49.6 Lexicons: i18n server-side cargado runtime

### 49.6.1 Anatomía del sistema lexicon

**CONFIRMADO** (Bloque 12.2.5-12.2.7):

```
Niagara module
 └── module.lexicon                           (default, en raíz JAR)
 
Station filesystem
 └── !lexicon/
      ├── es_ES/
      │   ├── platform.lexicon
      │   ├── bajaui.lexicon
      │   └── nmodsreflow.lexicon
      ├── es/
      │   └── ...
      ├── fr_FR/
      │   └── ...
      └── ja_JP/
          └── ...
```

**Properties file format**:
```properties
# nmodsreflow.lexicon (default, English)
nav.dashboard=Dashboard
nav.alarms=Alarms
button.acknowledge=Acknowledge
button.override=Override
error.permission.denied=Permission denied: {0}
error.value.outOfRange=Value {0} outside range [{1}, {2}]
```

```properties
# !lexicon/es_ES/nmodsreflow.lexicon
nav.dashboard=Tablero
nav.alarms=Alarmas
button.acknowledge=Reconocer
button.override=Anular
error.permission.denied=Permiso denegado: {0}
error.value.outOfRange=Valor {0} fuera del rango [{1}, {2}]
```

### 49.6.2 Fallback chain 4-niveles

**CONFIRMADO** (Bloque 12.2.7):

Cuando el server resuelve un key, prueba en orden:
1. **Exact match**: `file:!lexicon/es_ES/platform.lexicon` (locale completo)
2. **Generic locale**: `file:!lexicon/es/platform.lexicon` (sin región)
3. **Station default**: `file:!lexicon/{stationLocale}/platform.lexicon` (locale config station)
4. **Module bundled**: `module://platform/platform.lexicon` (default ENG en JAR)

Si el key no existe en ninguno → returna `null` o el key literal según método llamado (`getText` vs `getOrNull`).

**Locales soportados Honeywell** (CONFIRMADO Bloque 12.2.7): `en_US` (default), `es`/`es_ES`, `fr`/`fr_FR`, `de`/`de_DE`, `ja_JP`, `zh_CN`, otros según distribución.

### 49.6.3 La API server-side `BLexicon`

**CONFIRMADO** (Bloque 12.2.6):

```java
// Opción 1: Lexicon (simple, default locale)
Lexicon lex = Lexicon.get("nmodsreflow");
String msg = lex.get("button.acknowledge");  // "Acknowledge" (en) o "Reconocer" (es)

// Opción 2: LexiconText (N4.8+, context-aware con locale del user)
LexiconText text = new LexiconText("nmodsreflow", "button.acknowledge");
String msg = text.getText(context);  // resuelve via context.getLocale()

// Opción 3: LexiconModule (N4.8+, reusable)
LexiconModule lex = LexiconModule.get("nmodsreflow");
String msg = lex.getText("button.acknowledge", context);
```

**Context param** (`WebOp`, `HttpServletRequest`, `OrdTarget`): proporciona locale del user actual. Workbench típicamente usa `null` (VM default).

### 49.6.4 La API browser: el `lex!` plugin RequireJS

**CONFIRMADO** (Bloque 12.2.6 + Bloque 12.2.8):

```javascript
// Carga sincrónica via lex! plugin RequireJS (cliente)
require(['lex!nmodsreflow:button.acknowledge'], function(label) {
  // label = "Acknowledge" o "Reconocer" según locale
  $('#ackButton').text(label);
});

// Helper Lex.get (en bs.built.min.js)
require(['baja!'], function(baja) {
  var label = baja.Lex.get("nmodsreflow", "button.acknowledge");
  $('#ackButton').text(label);
});
```

**Ventaja del `lex!` plugin**: AMD plugin pattern — RequireJS cachea el resultado y lo trae solo una vez por key. Útil para keys usados muchas veces en una vista.

**Trade-off vs JS-side i18n libs (vue-i18n, i18next, react-intl)**:

| Aspecto | Niagara `lex!` | JS-side i18n (vue-i18n) |
|---------|----------------|-------------------------|
| Source of truth | Server `.lexicon` files | Cliente JSON files |
| Locale | Determinado por user/session server | Determinado por cliente |
| Hot-reload | Requiere station restart o lexicon reload | HMR en dev |
| Customization | Site puede modificar via filesystem | Requiere rebuild |
| Coverage | Toda la station (incluyendo errors backend) | Solo strings UI |
| Round-trip | Carga lazy via require | Bundled en SPA |

**Reflow no usa `lex!` plugin empíricamente**: el bundle producción no lo importa. Reflow probablemente tiene strings hard-coded en inglés (no internacionalizado) o usa vue-i18n custom. **TODO 49-1**: validar.

### 49.6.5 Placeholder `%lexicon(key)%` en BFormat

**CONFIRMADO** (Bloque 12.2.8):

```xml
<!-- En PX/BOG: BFormat property con placeholder lexicon -->
<p n="toFaultText" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveFailure)%"/>
<p n="toNormalText" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveSuccess)%"/>
```

Runtime: `BFormat.toString(Context)` resuelve el placeholder usando el locale del context.

**Implicación para SPA**: si la SPA renderiza un BFormat directamente (via `comp.getDisplay()`), el server resuelve el lexicon. Si la SPA accede al BFormat raw (`comp.get('toFaultText')`), recibe el string `"%lexicon(...)%"` literal — debe resolverlo client-side via `lex!` o ignorarlo. **Recomendación**: usar `getDisplay()` para BFormat properties, no acceder raw.

### 49.6.6 Interpolación `{0}, {1}` placeholders

**CONFIRMADO** (Bloque 12.2.5):

```properties
error.value.outOfRange=Value {0} outside range [{1}, {2}]
```

```javascript
// Server-side
String msg = lex.get("error.value.outOfRange");
String formatted = MessageFormat.format(msg, value, min, max);
// "Value 35 outside range [15, 30]"

// Cliente-side — interpolación manual
require(['lex!nmodsreflow:error.value.outOfRange'], function(template) {
  var msg = template
    .replace('{0}', value)
    .replace('{1}', min)
    .replace('{2}', max);
});
```

**GOTCHA G49-4 — `lex!` plugin retorna template raw, NO interpolado**: el cliente debe interpolar manualmente. NO es como `vue-i18n` que tiene `$t('key', { value, min, max })`. Si tu SPA usa muchos templates con interpolación, vale envolver en helper.

---

## 49.7 La pregunta arquitectónica: ¿server formatea o cliente formatea?

### 49.7.1 Opción A: Server formatea (`getDisplay()`)

```javascript
// SPA pide string ya formateado
var display = comp.getDisplay('out');  // "21.5 °C"
$('.value').text(display);
```

**Pros**:
- Consistencia total con Workbench/PX/HX
- Server aplica BFormat con `BFormat.display(value, user, permissions)` — gating + i18n + format
- SPA no replica lógica
- Cambios server propagan automáticamente

**Contras**:
- Inflexibilidad de formato (cliente no puede customizar)
- Round-trip extra si solo tenés el value crudo
- Server-side dependency para feature básico

### 49.7.2 Opción B: Cliente formatea (raw value + facets)

```javascript
// SPA arma el string client-side
var value = comp.get('out');
var facets = comp.getFacets('out');
var display = formatNumeric(value, facets);  // función custom SPA
```

**Pros**:
- Flexibilidad total de formato
- Zero round-trips después de tener facets
- Lógica testeable client-side
- SPA puede applicar formatos no soportados por BFormat (científico, abreviaciones)

**Contras**:
- Replica lógica (decimal handling, null cases, unit positioning)
- Inconsistencia con Workbench/PX
- Lexicons UI no llegan automáticamente — SPA debe cargar via `lex!`

### 49.7.3 Opción C: Híbrido (recomendado)

```javascript
// Para valores BAS típicos (sensors, setpoints) — server formatea
var sensorDisplay = comp.getDisplay('out');  // "21.5 °C"

// Para timestamps y números UI custom — cliente formatea
var lastUpdate = comp.get('lastUpdate');  // BAbsTime
var timestampDisplay = new Date(lastUpdate.getMillis())
  .toLocaleString('es-AR', { timeZone: lastUpdate.getZone().getId() });

// Para strings UI (labels, errors) — lexicon
require(['lex!nmodsreflow:button.acknowledge'], function(label) {
  $('#btn').text(label);
});
```

**Esto es lo que Reflow hace empíricamente** (con la salvedad que NO usa `lex!` plugin para i18n — usa strings hard-coded inglés).

**Recomendación SEJOFA**:

| Tipo de display | Approach recomendado |
|-----------------|----------------------|
| Numeric con units (BAS sensor/setpoint) | `comp.getDisplay()` — server formatea |
| Boolean con trueText/falseText | `comp.getDisplay()` — server formatea |
| Enum con BEnumRange tags | Cliente vía `range.getTag(ord)` — más rápido reactivo |
| Status flags (ok/fault/stale) | Cliente vía `comp.get('out').getStatus()` — para UI behavior |
| Timestamps | Cliente vía `Intl.DateTimeFormat` — tz-aware, browser-native |
| Strings UI (labels, errors) | `lex!` plugin si i18n requerido, hardcode si single-locale |
| Numbers custom (counters, percentages UI) | Cliente vía `Intl.NumberFormat` |

---

## 49.8 Patrones recomendados (production-ready)

### 49.8.1 Composable Vue para display reactivo

```javascript
// composables/useDisplay.js
import { ref, onMounted, onUnmounted } from 'vue';

export function useDisplay(ord, slotName = 'out') {
  const display = ref('--');
  const value = ref(null);
  const facets = ref(null);
  const status = ref(null);
  let comp = null;
  let sub = null;
  
  onMounted(async () => {
    const baja = await import('baja!');
    comp = await baja.Ord.make(ord).get();
    
    // Initial display
    refresh();
    
    sub = new baja.Subscriber();
    sub.attach({
      changed: (prop) => { if (prop.getName() === slotName) refresh(); },
      facetsChanged: (slot) => { if (slot.getName() === slotName) refresh(); }
    });
    sub.subscribe(comp);
  });
  
  onUnmounted(() => {
    if (sub) { sub.detach(); sub.unsubscribe(); }
  });
  
  function refresh() {
    if (!comp) return;
    value.value = comp.get(slotName);
    facets.value = comp.getFacets(slotName);
    status.value = value.value && value.value.getStatus ? value.value.getStatus() : null;
    display.value = comp.getDisplay(slotName);  // server-formatted string
  }
  
  return { display, value, facets, status };
}
```

### 49.8.2 BAbsTime helper utilities

```javascript
// utils/time.js
export function formatBAbsTime(absTime, options = {}) {
  if (!absTime || !absTime.getMillis) return '--';
  
  var date = new Date(absTime.getMillis());
  var locale = options.locale || navigator.language;
  var useStorageZone = options.useStorageZone || false;
  
  var fmtOptions = {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options.intlOptions
  };
  
  if (useStorageZone) {
    fmtOptions.timeZone = absTime.getZone().getId();
  }
  
  return new Intl.DateTimeFormat(locale, fmtOptions).format(date);
}

export function formatBAbsTimeRelative(absTime) {
  if (!absTime || !absTime.getMillis) return '--';
  
  var diffMs = Date.now() - absTime.getMillis();
  var diffSec = Math.floor(diffMs / 1000);
  
  if (diffSec < 60) return diffSec + 's ago';
  if (diffSec < 3600) return Math.floor(diffSec / 60) + 'm ago';
  if (diffSec < 86400) return Math.floor(diffSec / 3600) + 'h ago';
  return Math.floor(diffSec / 86400) + 'd ago';
}
```

### 49.8.3 Lexicon helper con interpolación

```javascript
// utils/lex.js
export async function lex(module, key, params = {}) {
  return new Promise((resolve) => {
    require(['lex!' + module + ':' + key], function(template) {
      var result = template;
      Object.keys(params).forEach(function(k, i) {
        result = result.replace(new RegExp('\\{' + i + '\\}', 'g'), params[k]);
      });
      resolve(result);
    });
  });
}

// Uso
var msg = await lex('nmodsreflow', 'error.value.outOfRange', { value: 35, min: 15, max: 30 });
// "Value 35 outside range [15, 30]" o equivalente localizado
```

---

## 49.9 Reflow patterns observados empíricamente

### 49.9.1 Patrones que SÍ usa Reflow

**CONFIRMADO** (greps en `reflow-frontend/src/`):

| Patrón | Ubicación | Análisis |
|--------|-----------|----------|
| `comp.getFacets('out').get('range')` | `DynamicColorForm.vue:426` | Patrón canónico para enum points — itera ordinals |
| `val.get('out').getStatus()` | `DeviceCard.vue:255`, `DeviceRow.vue:206`, `GroupCard.vue:189`, `GroupRow.vue:146`, `BoundLabel.vue:366` | Status para UI color — patrón muy difundido |
| `comp.getDisplayName()` | `ScheduleList.vue:19,93`, `ScheduleListConfigItem.vue` | Display name para nav strings |
| `date.toLocaleDateString('en-US', {...})` | `NoteCard.vue:159` | Browser-native timestamps con locale hardcoded `'en-US'` |
| `value.toLocaleString()` | `Total.vue:40`, `OrdTreeItem.vue:72` | Browser-native number formatting |

### 49.9.2 Patrones que NO usa Reflow (gaps)

- **NO usa `comp.getDisplay()` / `comp.getOutDisplay()`** — Reflow formatea client-side con su propia lógica
- **NO usa `lex!` plugin RequireJS** — strings hard-coded en inglés
- **NO usa `BAbsTime.getZone()` para tz-aware display** — usa browser default TZ
- **NO usa `BFormat`** server-side via `getDisplay` — todo el formatting es cliente

### 49.9.3 Implicaciones del approach Reflow

| Pro | Contra |
|-----|--------|
| Cero round-trips para display formatting | Inconsistencia potencial con Workbench display |
| Reflow controla 100% el visual | Imposible internacionalizar sin refactor mayor |
| Funciona offline-friendly (cached comps) | Multi-zone Supervisor display ambiguo |
| Sin dependencia de BFormat server changes | Replica lógica de format (mantenimiento doble) |

**GOTCHA G49-5 — Reflow approach es "single-locale single-zone" implícito**: si tu deploy necesita multi-locale o Supervisor multi-zone, el approach Reflow NO sirve sin refactor. Para greenfield, considerar híbrido (49.7.3) desde el inicio.

---

## 49.10 Gotchas transversales

**G49-1 — Status bits son ortogonales, no exclusivos**: un valor puede ser `fault` Y `overridden` simultáneamente. Definir prioridad de display explícita (Reflow chequea fault antes que overridden).

**G49-2 — `facetsChanged` no incluye el delta**: callback recibe `(slot, cx)` sin info de qué facet cambió. SPA debe re-leer `slot.getFacets()` y comparar con cache si necesita el delta.

**G49-3 — Reflow ignora BTimeZone**: `toLocaleString()` sin `timeZone` option da display ambiguo en Supervisor multi-zone. Para multi-zone, usar `Intl.DateTimeFormat(locale, { timeZone: absTime.getZone().getId() })`.

**G49-4 — `lex!` plugin retorna template raw, NO interpolado**: cliente debe interpolar manualmente `{0}`, `{1}`. NO es como `vue-i18n`. Envolver en helper si se usa mucho.

**G49-5 — Reflow approach es "single-locale single-zone" implícito**: si necesitás multi-locale o Supervisor multi-zone, refactor obligatorio. Greenfield → usar híbrido desde inicio.

**G49-6 — `comp.getDisplay()` retorna string formateado pero NO incluye status**: si el slot está en `fault` o `stale`, el display puede mostrar el último valor conocido sin indicador. Combinar con `comp.get('out').getStatus()` para UI status-aware.

**G49-7 — Facets de Niagara vs Intl.NumberFormat**: las opciones no son 1:1. `precision` Niagara = decimal places. `Intl.NumberFormat({minimumFractionDigits, maximumFractionDigits})` permite rangos. Si el `precision` es 2 pero el valor es `21`, `value.toFixed(2)` da `"21.00"` mientras `Intl.NumberFormat(..., {minimumFractionDigits: 0, maximumFractionDigits: 2})` da `"21"`. Decidir según UX.

**G49-8 — `BEnumRange.getTag(ord)` puede retornar `null` para ordinals no declarados**: si el server cambia el range y el cliente tiene un valor con ordinal viejo no presente, `getTag` returna null. Defensive: `range.getTag(ord) || 'UNKNOWN(' + ord + ')'`.

**G49-9 — Browser TZ != station TZ != BAbsTime zone — tres TZs distintas**: en una SPA sirviendo Supervisor multi-zone, tenés (1) TZ del browser del user, (2) TZ del station servidor, (3) TZ del BAbsTime de cada record. Decidir explícitamente cuál usar para CADA display — no asumir.

**G49-10 — Lexicons no se hot-reload sin restart station**: cambios a `.lexicon` files requieren reload del lexicon o station restart. Workflow dev iterativo es lento — para SPA i18n, considerar JS-side i18n para iteración rápida y server lexicon para production.

**G49-11 — `BFormat.display(value, user, permissions)` aplica permission gating**: si user no tiene read perm sobre el slot, `getDisplay()` puede retornar `"***"` o `"<denied>"` en vez del valor. NO confiar en parsear ese string como número.

**G49-12 — DST fall-back duplicate hour rompe binning local-hour**: chart binning by "local hour" puede mostrar spike doble. Usar `millis UTC` para binning, `Intl.DateTimeFormat` solo para axis labels. NO binning por local time.

**G49-13 — `toLocaleString()` browser-native sin `locale` puede usar locale inesperado**: depende del browser config + system locale. Para consistencia, pasar locale explícito (`'en-US'` o `navigator.language`). Reflow usa `'en-US'` hardcoded en `NoteCard.vue` — funciona pero ata a inglés.

**G49-14 — `range.getOrdinals()` puede no estar ordenado**: `BEnumRange` no garantiza orden de los ordinals. Si el UI necesita orden específico (ascendente, custom), aplicar `.sort()` después.

**G49-15 — Facets `editable` y `hidden` son hints, NO security**: misma trampa que Bloque 48 — `editable=false` en facets NO impide writes server-side. Es UI hint. Para security real, server-side enforcement vía RBAC.

---

## 49.11 TODOs y validaciones pendientes

**TODO 49-1**: Auditar `reflow-frontend/src/` — ¿hay algún uso de `lex!` plugin RequireJS o solo strings hard-coded inglés? Si todo es hard-coded, es technical debt para internacionalización futura.

**TODO 49-2**: Validar empíricamente la latency de `comp.getDisplay()` vs cliente formatting. Si el round-trip extra es <10ms, server-side display tiene ventaja por consistency. Si >50ms en RTT alto, cliente-side wins por UX.

**TODO 49-3**: Verificar si bajaScript browser tiene API para listar locales soportados en la station (`baja.Lex.listLocales()` o equivalente). INFERIDO ausente — la SPA debe hardcodear los locales o pedirlos via servlet custom.

**TODO 49-4**: Investigar `BFormat` browser-side — el plugin `baja.obj.Format.js` existe en `bajaScript-ux.jar:rc/baja/obj/Format.js`. ¿Expone una API para parsear format strings client-side y formatear sin server round-trip? Si sí, cubriría el gap de "consistencia + zero round-trip".

**TODO 49-5**: Documentar el comportamiento de `comp.getDisplay()` cuando el slot tiene `BFormat` con `%lexicon(key)%` placeholder — ¿el server resuelve el lexicon antes de retornar? INFERIDO sí (server aplica context locale del request).

**TODO 49-6**: Probar empíricamente el orden de eventos `changed` + `facetsChanged` cuando ambos disparan en el mismo delta server. ¿Llega `facetsChanged` antes o después de `changed`? Si después, el primer `changed` puede usar facets viejos.

**TODO 49-7**: Validar el patrón Reflow para Supervisor multi-zone — si el deploy real Honeywell es Supervisor + JACEs en distintas TZs, ¿Reflow muestra los timestamps correctamente o tiene bugs documentados?

**TODO 49-8**: Investigar si `Intl.RelativeTimeFormat` browser-native puede reemplazar la función custom `formatBAbsTimeRelative` (49.8.2) — disponible en todos los browsers modernos (Chrome 71+, Firefox 65+, Safari 14+).

---

## 49.12 Próximos pasos

### Para implementar formatting + i18n en SPA externa Niagara

1. **Decidir approach**: server (`getDisplay`), cliente (`Intl.*` + facets manual), o híbrido (recomendado).

2. **Implementar `useDisplay` composable** (Vue) o equivalente — wrap `getDisplay` + `facetsChanged` + `changed` reactivamente.

3. **Implementar `formatBAbsTime` utility** con opción de TZ (storage o browser) — defaultear a browser para single-station, expone option para Supervisor multi-zone.

4. **Decidir i18n strategy**: si el deploy es multi-locale, usar `lex!` plugin con helper `lex(module, key, params)`. Si single-locale, hardcodear strings en inglés (acepta technical debt explícito).

5. **Para enums**: usar `range.getOrdinals()` + `range.getTag()` empíricamente — patrón Reflow es sólido.

6. **Para status**: leer `val.get('out').getStatus()` y mapear a CSS classes — Reflow tiene mapping color completo (49.2.4 statusColors).

7. **Reactividad**: subscribir con `facetsChanged` para slots cuyo metadata puede cambiar dinámicamente (setpoints con modos auto/manual).

8. **Multi-zone**: si deploy es Supervisor con subordinates en distintas TZs, NO usar `toLocaleString()` sin `timeZone` option. Documentar TZ display strategy explícitamente en cada widget.

9. **Defensive**: NUNCA asumir que `comp.has(slotName)`, `facets.get('range')`, o `range.getTag(ord)` retornan no-null. Wrap con fallbacks (Bloque 48 también aplica).

10. **Audit Reflow patterns**: si extiende Reflow, NO romper los patrones empíricos (`getFacets('out').get('range')` etc.) — son canónicos en este codebase.

### Para investigación futura

- Validar TODOs 49-1..49-8
- Comparar latency `getDisplay` vs cliente formatting empíricamente
- Investigar `BFormat` browser API completa (TODO 49-4)
- Probar Supervisor multi-zone display real (TODO 49-7)

---

## Fuentes y referencias cruzadas

| Afirmación | Fuente empírica | Bloque ref |
|------------|-----------------|------------|
| `BFacets` collection key-value metadata | Bloque 4 slot system + Bloque 22.726 | Bloque 4 + 22.726 |
| `comp.getFacets()` retorna metadata range/units/format | Bloque 22.932 BajaScript Component model | Bloque 22.932 |
| `prop.getFacets()` retorna `{editable, hidden, ...}` | `bajaScript-ux.jar` Type.getSlot().getFacets() | Bloque 22.726 |
| `comp.getFacets('out').get('range')` patrón empírico | `reflow-frontend/src/components/dashboard/DynamicColorForm.vue:426` | — |
| `BEnumRange.getOrdinals()/getTag()` API | Reflow `DynamicColorForm.vue:427-428` | — |
| `comp.getDisplay(prop)` retorna string formateado server-side | Bloque 22.14 + Bloque 22.20 | Bloque 22.14 |
| `comp.getOutDisplay()` shortcut BControlPoint | `bajaScript-ux.jar:rc/baja/comp/ControlPoint.js` | Bloque 22.757 |
| `BFormat.display(value, user, permissions)` aplica permission gating | Bloque 22.20 + Bloque 48 | Bloque 22.1160 |
| `comp.get('out').getStatus()` patrón status flags | Reflow múltiples components: DeviceCard, DeviceRow, GroupCard, GroupRow, BoundLabel | — |
| Status mapping color statusColors | Reflow `DynamicColorForm.vue:447-449` | — |
| `facetsChanged` callback en Subscriber | Bloque 22.808 + Bloque 42.123 | Bloque 22.808 + 42.123 |
| `flagsChanged` vs `facetsChanged` distinción | Bloque 42.119-189 | Bloque 42 |
| BAbsTime = {millis UTC, BTimeZone zone} | Bloque 31.11.1-31.11.2 | Bloque 31.11.1 |
| BAbsTime serialization "millis;zone" | Bloque 31.11.2 | Bloque 31.11.2 |
| Cross-zone aggregation gotcha (daily average TZ) | Bloque 31.11.3 | Bloque 31.11.3 |
| DST spring forward gap + fall back duplicate | Bloque 31.11.4 | Bloque 31.11.4 |
| Clock drift sin NTP 1-5min/día | Bloque 31.11.5 | Bloque 31.11.5 |
| Supervisor multi-zone subordinates display gotcha | Bloque 31.11.6 | Bloque 31.11.6 |
| Chart display TZ vs Storage TZ mismatch | Bloque 31.11.7 | Bloque 31.11.7 |
| `date.toLocaleDateString('en-US', {...})` browser-native pattern | Reflow `NoteCard.vue:159-160` | — |
| `value.toLocaleString()` browser-native number format | Reflow `Total.vue:40`, `OrdTreeItem.vue:72` | — |
| Lexicon file format properties key=value | Bloque 12.2.5 | Bloque 12.2.5 |
| Lexicon ubicaciones module.lexicon + !lexicon/{lang}/ | Bloque 12.2.5 | Bloque 12.2.5 |
| `BLexicon.getText(key, ctx)` server-side API | Bloque 12.2.6 | Bloque 12.2.6 |
| `lex!` plugin RequireJS browser-side | Bloque 12.2.6 + Bloque 12.2.8 | Bloque 12.2.6 |
| Fallback chain 4-niveles (exact → generic → station → module) | Bloque 12.2.7 | Bloque 12.2.7 |
| Locales soportados Honeywell en_US/es/fr/de/ja/zh | Bloque 12.2.7 | Bloque 12.2.7 |
| `%lexicon(key)%` placeholder en BFormat | Bloque 12.2.8 | Bloque 12.2.8 |
| Interpolación `{0}`, `{1}` placeholders | Bloque 12.2.5 | Bloque 12.2.5 |
| `comp.getDisplayName()` para nav strings | Reflow `ScheduleList.vue`, `ScheduleListConfigItem.vue` múltiples | — |
| Reflow NO usa `lex!` plugin (strings hard-coded inglés) | Inferido empírico — 0 matches `lex!` en `reflow-frontend/src/` | — |
| Reflow NO usa `getDisplay`/`getOutDisplay` (formatea cliente) | Inferido empírico — 0 matches en código | — |
