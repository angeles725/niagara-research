# Design: mx60-history-time-range

**Change**: `mx60-history-time-range`
**Fase**: `sdd-design` (architectural HOW — NO requirements, NO step-by-step tasks)
**Fecha**: 2026-05-11
**Source**: proposal `sdd/mx60-history-time-range/proposal` (engram #1308) + bloque #73 (engram #1265) + read-only inspection MX60 source 2026-05-11
**TDD**: Strict TDD ACTIVE — toda decisión debe permitir red→green→refactor verificable contra `ChiHistoryHelperTest` y un harness JS (a definir, ver Decisión 7)

---

## Architecture Approach

El cambio se ejecuta sobre **dos capas que comparten un vocabulario canónico** (las keys camelCase del backend: `lastHour | last8Hours | last24Hours | last7Days | today | yesterday | last30Days | monthToDate`). La capa frontend (`UpDetail.js`) deja de ser libre de inventar IDs cortos y se vuelve un cliente disciplinado de ese vocabulario; la capa backend (`ChiHistoryHelper.java`) deja de tratar la lista de rangos como un `if/else` que opina sobre `start/stop` y se convierte en una **tabla de specs por rango** (`computeRange` + `computeTargetPoints` + opcionalmente un struct `RangeSpec` cohesivo) donde cada rango aporta sus 3 metadatos críticos: ventana temporal, points objetivo y step esperado. El `else` final del dispatch se mantiene como red de seguridad (`lastHour` fallback) pero **gana logging defensivo** — el fallback dejará de ser silencioso.

La filosofía es **defense in depth con un solo source of truth**: hay un único mapping cliente→backend (`RANGE_TO_BACKEND` en `UpDetail.js`), una única función de dispatch backend (`computeRange`), una única función de capacity-target backend (`computeTargetPoints` o método paralelo en `RangeSpec`), y un único derivador de cap dinámico en cliente (`_maxHistoryEntries()`). El client-side stride de `filterHistoryByRange` (UpDetail.js:894-907) se **PRESERVA** intacto — no porque sea redundante después del stride backend, sino porque ambos hacen jobs diferentes en momentos diferentes (backend reduce payload de red, frontend ajusta a chart canvas + recorta tras `_appendLiveSample`). Esta dualidad respeta el veredicto B de bloque #73 §73.7 ("PRESERVAR de MX60: client-side stride en filterHistoryByRange") sin sumarle complejidad nueva.

---

## Decision 1: Location of `RANGE_TO_BACKEND` const

**Opciones consideradas**:

- **A — Module-private const adyacente al `RANGES` array (UpDetail.js:171-176)**. Una sola declaración inmediatamente después de `RANGES`, dentro del mismo archivo `UpDetail.js`.
- **B — Exportado desde un módulo JS shared dentro de `chihuahua-ux`** (por ejemplo `rc/js/shared/historyVocabulary.js`).
- **C — Backend-driven**: el frontend hace un `GET /mx60/api/historyRanges` al boot y construye el mapping dinámicamente desde la respuesta.

**Elegida: A — module-private const adyacente al `RANGES` array**.

**Justificación**:

- **Blast radius mínimo**: `UpDetail.js` es el único call-site de `_fetchSlotHistory` en MX60 (confirmé via `_loadRealHistory` y rebuild flow L3423/L3783 — todos ruedan dentro del mismo archivo). El IIFE classic + ES module hybrid del archivo (engram #1257) hace que extraer a shared module obligue a tocar el bundle setup, sin beneficio porque no hay segundo consumidor.
- **Alineamiento con patrón MX60**: `RANGES`, `TEMP_GAUGES`, `UP_CHART_SLOTS`, `DEFAULT_RANGE` son todos `const` module-private en `UpDetail.js`. Agregar `RANGE_TO_BACKEND` en la misma vecindad respeta la convención del archivo y evita disonancia estilística.
- **Mantenimiento bajo**: cualquiera que toque `RANGES` para agregar un rango ve `RANGE_TO_BACKEND` literalmente 2 líneas abajo. El acople es visual y obliga a actualizar ambos en el mismo PR. Tests TDD aseguran el invariante (cada `RANGES[i].id` tiene entrada en el mapping).
- **Por qué NO B**: shared module exigiría justificar al menos un segundo consumidor — no existe hoy. YAGNI explícito. Si en sprint-2 R2.1 (multi-history endpoint) emerge un segundo consumidor real, **promover a shared module entonces** es trivial (cortar + pegar, sin cambio semántico).
- **Por qué NO C**: agrega un round-trip al boot (regresión perceptible en time-to-first-paint, que el `requestIdleCallback` initial defer #310 minimiza pero no elimina), agrega una nueva ruta REST que mantener, y resuelve un problema que no existe (las keys backend son estables; cambian en releases coordinados). Backend-driven sólo se justificaría si los rangos vinieran de configuración runtime — no es el caso.

**Consecuencia**: el const vive en `UpDetail.js` entre L176 y L177 (después de `RANGES`, antes de `DEFAULT_RANGE`). Forma estable:

```js
const RANGE_TO_BACKEND = {
  '1h': 'lastHour',
  '8h': 'last8Hours',
  '24h': 'last24Hours',
  '7d': 'last7Days',
  // R1.3 — 4 ranges previamente escondidos
  'today': 'today',
  'yesterday': 'yesterday',
  '30d': 'last30Days',
  'mtd': 'monthToDate'
};
```

(IDs cortos para los 4 originales y semantic-but-short para los 4 nuevos — coherente con la convención `1h/8h/24h/7d` ya establecida. Ver Decisión 5 sobre por qué el frontend keeps cortos en lugar de adoptar las keys backend directamente.)

---

## Decision 2: `maxPoints` per range — derivation strategy

**Opciones consideradas**:

- **A — Lookup table** explícita en backend, por key: `lastHour→60`, `last8Hours→96`, `last24Hours→96`, `last7Days→168`, `today→144`, `yesterday→96`, `last30Days→360`, `monthToDate→360`.
- **B — Derivada de density target** (1pt/30s para rangos ≤ 1h, 1pt/5min para ≤ 24h, 1pt/15min para ≤ 7d, 1pt/30min para 30d+).
- **C — User-tunable via property** (`@Property` del componente `ChiDashboardService` o param query `?targetPoints=N`).

**Elegida: A — lookup table explícita por key**.

**Justificación**:

- **Source of truth compartido frontend↔backend**: el frontend ya tiene `RANGES[].points` (60/96/96/168). El backend debe usar **exactamente** los mismos números por key, no calcularlos por densidad (que daría números diferentes y abriría la puerta a divergencia). Si el frontend renderiza 168 puntos para `last7Days`, el backend debe devolver ≈168 puntos para evitar trabajo redundante del client-side stride.
- **Precedente Niagara webChart nativo** (bloque #45): `autoSamplingSize=2500` y `maxSamplingSize=50000` son **explícitos por instancia**, no derivados. La práctica establecida en el ecosistema es lookup, no fórmula.
- **Bloque #73 §73.7 R1.2 verdict**: "raise backend cap a 50000 + agregar stride downsampling backend" — el cap **techo** (hard ceiling) y el cap **objetivo por rango** son dos cosas distintas. El techo previene OOM; el objetivo evita payload innecesario. La opción A respeta ambos sin colisionar.
- **Por qué NO B**: una fórmula por densidad pierde la habilidad de decidir, por ejemplo, "para `today` quiero más resolución que para `last24Hours` aunque ambos sean ≤24h" (today suele ser un foco de inspección manual). Hard-coding por key permite afinar caso por caso sin reescribir la fórmula.
- **Por qué NO C**: introduce surface area pública sin caso de uso concreto. Si emerge debug necesidad de full-resolution, se atiende con `?fullResolution=true` (ver Decisión 3) que es una flag binaria, no un tuning continuous.

**Valores exactos definidos** (los 4 originales replican `RANGES[].points`; los 4 nuevos derivan del trade-off resolución vs payload):

| Backend key | targetPoints | Densidad implícita |
|-------------|--------------|--------------------|
| `lastHour` | 60 | 1pt/min |
| `last8Hours` | 96 | 1pt/5min |
| `last24Hours` | 96 | 1pt/15min |
| `last7Days` | 168 | 1pt/hour |
| `today` | 144 | 1pt/10min (rango variable, hasta 24h) |
| `yesterday` | 96 | 1pt/15min (rango fijo 24h) |
| `last30Days` | 360 | 1pt/2h |
| `monthToDate` | 360 | 1pt/2h (rango variable, hasta ~31 días) |

**Hard ceiling** (techo de seguridad): `MAX_POINTS_HARD_CEILING = 5000`. Si por bug un rango pide más, se trunca a 5000. (Bajo el 50000 de Niagara webChart, pero alineado al tamaño realista del response MX60 — 50000 puntos × ~30 bytes = 1.5 MB por chart, fuera del régimen razonable para REST sin chunking.)

**Consecuencia**: nace `computeTargetPoints(String rangeName) → int` en `ChiHistoryHelper.java` (helper hermano de `computeRange`). Ver Decisión 5 para por qué se decide entre función paralela vs struct cohesivo.

---

## Decision 3: Stride algorithm — preserve first + last + spikes?

**Opciones consideradas**:

- **A — Pure uniform stride**: `stride = max(1, totalRecords / targetPoints)`, tomar cada N-ésimo record y siempre incluir el último.
- **B — Stride + always include first/last/min/max per window**: por cada bucket de `stride` records, tomar el primero + el min + el max + el último (4 muestras por bucket).
- **C — LTTB (Largest Triangle Three Buckets)**: algoritmo de downsampling visual-perceptual, dos passes (uno para precomputar bucket boundaries, otro para elegir el triángulo de área máxima en cada bucket).

**Elegida: A — pure uniform stride + always include first/last**.

**Justificación**:

- **Constraint single-pass**: el `cursor` de `BHistoryDatabase` es single-pass forward (`cursor.next()` en ChiHistoryHelper.java:196). LTTB necesita conocer `min/max` por bucket antes de seleccionar, lo cual obliga a doble pass o a bufferear el bucket entero en memoria. Para rangos largos (`last30Days` con 1pt/min = 43200 records) bufferear es costoso. **Stride uniforme es O(N) sin estado**.
- **Implementation complexity mínima**: 6 LOC backend. Stride uniforme es trivialmente bisectable, trivialmente testeable (predicado: "el response tiene ≤ targetPoints+1 puntos, primer y último point están presentes, gaps entre puntos consecutivos ≈ step expected").
- **Preserva el patrón del cliente**: `filterHistoryByRange` ya hace stride uniforme con `Math.floor(filtered.length / r.points)` + "always include last" (UpDetail.js:900-905). Replicar la misma forma en backend evita disonancia conceptual entre capas — los dos strides son el mismo algoritmo, sólo en momentos distintos.
- **Bloque #73 §73.4 verdict B**: MX60 client-side stride es superior a Reflow (que no tiene downsampling). Agregar backend stride NO cambia el algoritmo, sólo lo replica antes en el pipeline. **No reinventamos**, copiamos lo que ya funciona empíricamente.

**Spike trade-off explícito** (debe quedar documentado en la spec):

> Un spike (transient de duración menor a `step` del rango) puede quedar **invisible** si cae entre dos índices del stride. Para rangos cortos (`lastHour`, `last8Hours`) el `step` es pequeño (60s a 5min) y el riesgo es bajo. Para `last7Days` con `step=1h` cualquier spike de duración < 1h con muestreo más denso puede perderse. La mitigación NO es cambiar el algoritmo (eso es TIER-3 / debug-only) sino **exponer una flag opcional `?fullResolution=true`** que bypassea el stride backend y devuelve todos los records hasta `MAX_POINTS_HARD_CEILING`. Esta flag NO se expone en la UI regular del usuario — es para diagnóstico vía URL directa o herramienta de soporte. Si el dominio HVAC empieza a reportar spikes perdidos como falso negativo recurrente, ESCALAR a TIER-3 con propuesta de min/max preservation o LTTB.

**Consecuencia**: `queryHistoryData` reemplaza el actual `while (cursor.next() && count < maxPoints)` por:

1. Primera estimación de `totalRecords` (option a: contar primero — costoso; option b: estimar desde `windowMs / expectedStep` — barato pero impreciso). **Decisión interna**: usar `cursor.next()` con counter sin pre-conteo, aplicar stride **post-hoc** a un buffer transitorio de tamaño máximo `MAX_POINTS_HARD_CEILING`. Para rangos cortos esto es idéntico al actual. Para rangos largos donde el buffer se llenaría, aplicar stride **on-the-fly** descartando intermedios según un contador modulo. Esto preserva single-pass y bounded memory. **El algoritmo concreto + sus tests es input al sdd-tasks** — el design fija el contrato (≤ targetPoints+1 puntos, monotonic timestamps, primer y último presente) no el código.

---

## Decision 4: `HISTORY_MINUTES` cap — single source vs per-call

**Opciones consideradas**:

- **A — Computar al cambiar de rango**, guardar el valor en una variable de estado compartida (`_currentMaxHistoryEntries`), leerla en el call-site.
- **B — Función `_maxHistoryEntries()`** que lee el rango activo y deriva en cada llamada.

**Elegida: B — función `_maxHistoryEntries()`**.

**Justificación**:

- **Single call-site confirmado**: grep empírico (`HISTORY_MINUTES` aparece en UpDetail.js sólo en L396 declaración y L3535 uso `if (fullHistory.length > HISTORY_MINUTES + 600)`). El cap se consulta **una vez por live append**, no en hot loop crítico. El overhead de una función call + lookup en `RANGES` es nanosegundos — irrelevante.
- **Cero estado nuevo**: la opción A introduce una variable mutable que necesita ser actualizada **antes** del próximo `_appendLiveSample`. Si el código olvida actualizarla en algún path (ej. error en `_loadRealHistory`, rangos cambiados durante in-flight fetch), el cap queda stale y el bug es invisible. La función B lee `_currentHistoryRange` (variable que ya existe y ya se mantiene viva — verificable en UpDetail.js:3783) y deriva en el momento — imposible quedar desincronizada.
- **Bloque #73 §73.6 prescripción literal**: el snippet propuesto en §73.6 es exactamente una función (`function _maxHistoryEntries() { ... }`). Respeta el verdict directamente.
- **Por qué NO A**: el ahorro de performance es nulo (1 call site) y agrega un invariante implícito ("actualizar `_currentMaxHistoryEntries` cada vez que cambia el rango") que es bug-prone. Estado vs función pura — siempre función pura cuando el costo es despreciable.

**Consecuencia**: nace `_maxHistoryEntries()` en `UpDetail.js` adyacente a `HISTORY_MINUTES` (L396). El `const HISTORY_MINUTES = 24 * 60` se **mantiene** como **fallback constant** (referenciada por la función cuando `RANGES.find()` retorna `undefined`) — no se borra para preservar el comentario doc L388-395 que explica el trade-off histórico. Forma estable:

```js
const HISTORY_MINUTES_FALLBACK = 24 * 60;  // renombrado desde HISTORY_MINUTES
function _maxHistoryEntries() {
  const r = RANGES.find(function(x) { return x.id === _currentHistoryRange; });
  if (!r) return HISTORY_MINUTES_FALLBACK;
  // headroom 50% sobre points objetivo para baseline + live append buffer
  return Math.max(HISTORY_MINUTES_FALLBACK, Math.floor(r.points * 1.5));
}
```

L3535 cambia de `if (fullHistory.length > HISTORY_MINUTES + 600)` a `if (fullHistory.length > _maxHistoryEntries() + 600)`.

**Nota sobre `points` vs `hours`**: la función usa `r.points * 1.5` (no `r.hours * 60`) porque el cap representa **entries acumuladas**, no minutos. Para rangos densos (`lastHour` con 60 points) el cap se mantiene en el fallback 1440. Para `last7Days` con 168 points el cap también es 1440 (`168 × 1.5 = 252` → max con 1440 → 1440). El cap dinámico realmente sube sólo en rangos largos con densidad alta (`last30Days` con `targetPoints=360` → `540` → fallback). Para que el cap suba significativamente, los `points` del frontend tienen que crecer. Esto es **intencional**: el cap protege contra Array.shift() loop; mientras los `points` se mantengan en 60-360 el fallback 1440 absorbe sin degradación. Si en futuro se sube `last7Days.points` a 2000 (full hourly resolution con padding), el cap escala automáticamente.

---

## Decision 5: Exponer 4 nuevos rangos sin romper el shape de `RANGES`

**Opciones consideradas** (con shape actual confirmado en UpDetail.js:171-176: `{id, label, hours, points, step}`):

- **A — Extender el array `RANGES`** existente con 4 nuevas entradas que mantengan el mismo shape (`{id, label, hours, points, step}`).
- **B — Crear `RANGES_EXTENDED`** con shape extendido `{id, label, hours, points, step, dynamic?, computeHours?}` para soportar rangos variables (`today`, `monthToDate`) — backward compatible.
- **C — UI inline**: agregar los 4 rangos como elementos extra fuera del loop sobre `RANGES`.

**Elegida: B — extender el shape de `RANGES` con un campo opcional `dynamic`** (rechazando A puro porque rompe la semántica de `hours` para `today`/`monthToDate`, y rechazando C porque fragmenta el render loop).

**Justificación**:

- **`hours` deja de ser válido para `today` y `monthToDate`**: a las 10:00 AM, `today.hours` debería ser 10, no 24; el día 5 del mes `monthToDate.hours` debería ser 96, no 744. Si se mete un número fijo en el shape, `filterHistoryByRange` (que usa `r.hours` para `cutoff = Date.now() - r.hours * 3600000`) filtraría incorrectamente — el cutoff caería en el día anterior y `today` mostraría también yesterday.
- **Solución mínima**: agregar un campo opcional `dynamic: true` (y un helper `computeCutoff(r, now)` que sepa qué hacer con los 2 dynamic). Para los 6 rangos no-dinámicos (`1h, 8h, 24h, 7d, yesterday, 30d`) `r.hours` sigue siendo el valor estable. Para los 2 dinámicos, `computeCutoff` reescribe la lógica:
  - `today`: cutoff = inicio del día local (`new Date(); .setHours(0,0,0,0)`).
  - `monthToDate`: cutoff = primer día del mes local (`new Date(); .setDate(1); .setHours(0,0,0,0)`).
  - `yesterday`: aunque tiene rango "variable" en el sentido de no ser "últimas N horas", es **estable** (24h del día anterior). Se puede tratar como dinámico (cutoff = inicio de yesterday, ceiling = fin de yesterday) o ponerle `hours: 48` para que el cutoff alcance, pero entonces incluiría también `today` early — mejor `dynamic: true` también.

  Tres dinámicos: `today`, `yesterday`, `monthToDate`. `last30Days` es estático (720h fijos).

- **Por qué NO A puro**: rompe sutilmente `filterHistoryByRange`. El bug sería visualmente confuso (mezcla today+yesterday) — exactamente el tipo de bug silencioso que bloque #73 enseñó a evitar.
- **Por qué NO C**: el loop de tabs en `UpDetail.js` (referenciado en bloque #73 §73.6 implícito y verificable en código) itera sobre `RANGES` para renderizar el tab strip. Fragmentar a "los 4 originales por loop + 4 nuevos hardcoded" duplica markup y rompe la simetría — cada tab nuevo después requeriría 2 ediciones.

**UI rendering — tab strip vs dropdown**:

- 8 rangos visibles como tabs horizontales empiezan a saturar el header del `UpDetail`. Pero migrar a dropdown es cambio de UX que excede el scope de "fix backend desperdiciado" del proposal (R1.3 dice "expose 4 hidden ranges").
- **Decisión**: mantener tab strip horizontal para los 8. Si el ancho del header del UpDetail no alcanza visualmente, **escalar a sdd-design futuro** (UI redesign), no a este SDD. Esta decisión queda **abierta a confirmación del usuario** (ver `open_decisions` en el contrato de resultado).

**Localization de labels**:

- MX60 hoy NO tiene i18n para `'1h' / '8h' / '24h' / '7d'`. Los labels son inglés-only. Para `today / yesterday / 30d / mtd` el patrón inglés-only se mantiene en sprint-1 (consistencia con el resto del UI MX60). Si en sprint-N se introduce i18n, el shape de `RANGES` se extenderá con `i18nKey?: string` — no es scope de este change.

**Consecuencia**: forma estable del `RANGES` post-cambio:

```js
const RANGES = [
  { id: '1h',        label: '1h',    hours: 1,    points: 60,  step: 60 * 1000 },
  { id: '8h',        label: '8h',    hours: 8,    points: 96,  step: 5 * 60 * 1000 },
  { id: '24h',       label: '24h',   hours: 24,   points: 96,  step: 15 * 60 * 1000 },
  { id: '7d',        label: '7d',    hours: 168,  points: 168, step: 60 * 60 * 1000 },
  { id: 'today',     label: 'Today', dynamic: true,  points: 144, step: 10 * 60 * 1000 },
  { id: 'yesterday', label: 'Yest',  dynamic: true,  points: 96,  step: 15 * 60 * 1000 },
  { id: '30d',       label: '30d',   hours: 720,  points: 360, step: 2 * 60 * 60 * 1000 },
  { id: 'mtd',       label: 'MTD',   dynamic: true,  points: 360, step: 2 * 60 * 60 * 1000 }
];
```

`filterHistoryByRange` (UpDetail.js:894-907) se refactoriza para preguntar por `r.dynamic` y delegar a un helper `_computeRangeCutoff(r, Date.now())` que retorna `cutoff` apropiado.

---

## Decision 6: Preservar MX60 client-side stride O removerlo ahora que backend strides

**Opciones consideradas**:

- **A — Preservar ambos** (defense in depth): backend stride reduce payload a `targetPoints`, frontend `filterHistoryByRange` stride asegura chart-fit y filtra live-appended samples post-fetch.
- **B — Remover frontend stride** una vez backend handles canonical downsampling. Menos código, single point of truth.

**Elegida: A — Preservar ambos (defense in depth)**.

**Justificación**:

- **Bloque #73 §73.7 PRESERVAR list — explícito**: "Client-side stride downsampling en `filterHistoryByRange` (UpDetail.js:894-907) — `stride = max(1, floor(filtered.length / r.points))`. SUPERIOR al Reflow no-sampling". Y §73.5 verdict B: "preservar MX60 client-side stride + R1.2 raise backend cap a 50000 + agregar stride downsampling backend". El bloque empíricamente decretó que **ambos** son la postura correcta.
- **Los dos hacen jobs distintos**:
  - **Backend stride**: reduce payload de red. Aplica una vez por fetch, sobre data históricamente persistida.
  - **Frontend stride**: aplica sobre `fullHistory` que incluye **samples vivos** appended via `_appendLiveSample` (UpDetail.js:3329-3377). Después de N ticks de live append, `fullHistory.length` puede crecer arbitrariamente — el frontend stride es lo que mantiene el chart legible.

  Sin frontend stride, después de 30 minutos de live append en `1h` range, el chart tendría 60 históricos + 3600 live samples = 3660 puntos en un eje de 60. Chart.js render se degrada y la UX cae.

- **Chart-fit responsiveness**: el frontend stride se calcula sobre `filtered.length` (después del cutoff), así que se adapta dinámicamente al estado actual del `fullHistory`. Backend stride no puede hacer esto — sólo ve la BD histórica.
- **Backend trust**: si el backend devuelve `targetPoints+1` puntos, frontend stride es no-op (`Math.floor(60 / 60) = 1` → toma todos). El costo es 6 LOC ya escritas (UpDetail.js:894-907). Removerlo ahorra 6 LOC y pierde defense in depth + live-sample handling.
- **Por qué NO B**: la única ganancia es "menos código". El costo es perder un patrón superior que ya está en producción y que el bloque #73 marcó como PRESERVAR. Mal trade-off.

**Consecuencia**: `filterHistoryByRange` se mantiene intacta en su algoritmo central. **Lo único que cambia** es la lectura de `cutoff` cuando `r.dynamic === true` (delegando a `_computeRangeCutoff`, ver Decisión 5). El stride/include-last sigue exactamente igual.

---

## Decision 7: Test strategy (Strict TDD active)

**Implementation repo**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`.

### Backend (Java — JUnit)

- **Existing test file**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiHistoryHelperTest.java` (presence verified empíricamente 2026-05-11 via `fd`). La fase `sdd-spec` debe inventariar las assertions existentes — el design NO inventa lo que ya hay.
- **Nuevos asserts requeridos por este change**:
  1. `computeRange(name)` para cada una de las 8 keys retorna `[start, stop]` con delta coherente (delta ≈ 3600000 para `lastHour`, ≈ 7×86400000 para `last7Days`, etc.). Para `today`/`yesterday`/`monthToDate` verificar boundaries por reloj (start del día/mes).
  2. `computeRange("foo")` (key desconocida) cae a `lastHour` (delta ≈ 3600000) **Y** emite WARNING log capturable.
  3. `computeTargetPoints("last7Days")` retorna 168, etc. (la tabla completa de la Decisión 2).
  4. `computeTargetPoints("foo")` retorna fallback (60) **Y** emite WARNING.
  5. `queryHistoryData` con cursor mock de 10080 records y rango `last7Days` (targetPoints=168) devuelve response JSON con ≤ 169 puntos, primer point timestamp cerca de `start`, último point cerca de `stop`, timestamps monotonic ascending.
  6. `queryHistoryData` con `?fullResolution=true` y cursor mock de 3000 records bypassea stride, devuelve 3000 puntos (o hasta `MAX_POINTS_HARD_CEILING=5000` si más).
- **Gradle command**: convención Niagara module → `gradle :chihuahua-ux:test --tests com.angeles.chihuahua.ux.ChiHistoryHelperTest` (la fase `sdd-apply` debe verificar el comando exacto contra el `build.gradle` del módulo antes del primer commit — el design NO afirma sin verificar). Strict TDD obliga red→green→refactor cada test nuevo.

### Frontend (JS)

- **¿Hay vitest/jest configurado en MX60?**: NO empíricamente verificado en chihuahua-ux. La estructura del módulo es Niagara `chihuahua-ux` con `srcTest/` para JUnit Java; los `.js` viven en `src/rc/js/app/` sin acompañamiento de test runner JS visible.
- **Decisión interim**: la fase `sdd-spec` o `sdd-apply` debe **confirmar empíricamente** mediante `fd '(vitest|jest|karma|mocha)\.config' /home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60`. Tres caminos posibles según resultado:
  1. **Si existe runner JS**: agregar tests unitarios para `RANGE_TO_BACKEND` (cada `RANGES[i].id` tiene entry), `_maxHistoryEntries()` (variando `_currentHistoryRange`), `_computeRangeCutoff(r, fixedNow)` (con clock mockeado), `filterHistoryByRange` con `r.dynamic` (regression suite).
  2. **Si NO existe runner JS pero se acepta agregarlo**: standup mínimo de vitest (10-30 LOC config) sólo para los archivos tocados en este change — propuesta separada vía sdd-spec si el usuario lo aprueba.
  3. **Si NO existe runner JS y NO se agrega**: documentar un **manual regression checklist** ejecutable que la fase `sdd-verify` corra como QA manual:
     - Open UP detail page; click cada tab (8 ranges); verificar via DevTools Network que `range=` query param es la key backend correcta.
     - Click `7d`; verificar que el chart muestra serie completa hasta `now` (no gap final).
     - Click `30d`; verificar que el chart renderiza (botón visible).
     - Click `today` a diferentes horas del día; verificar que el cutoff es inicio del día (cutoff visual en eje X).
     - Switch `1h → 7d → 1h`; verificar via `console.log(fullHistory.length)` que el cap dinámico funciona (`< 252` post-switch a `1h`).

  **Pre-elección del design**: camino (3) — manual regression checklist — porque agregar un runner JS es scope creep respecto al proposal. El usuario puede sobrescribir esta decisión via `sdd-spec` si prefiere (1) o (2).

### Integration (Gradle station task)

- MX60 chihuahua tiene una station task convencional `gradle :chihuahua-rt:start` (verificar nombre exacto en sdd-apply). Para integration test:
  - Start station con BD de histories pre-poblada (fixture: 7 días @ 1pt/min para un slot `tempZona`).
  - Driver script (cURL): `GET /mx60/api/historyData?id=<slot>&range=last7Days` → verificar JSON response (≤169 puntos, last point timestamp ≈ now ±60s).
  - Repetir para los 8 rangos.
- **Fixture management**: la BD de fixtures NO existe hoy. **Decisión punteada**: sdd-spec inventaría qué station/baja file se puede usar como base; sdd-apply genera la fixture si no existe (script Niagara o programatic populate). NO es bloqueante para sprint-1 si los unit tests JUnit cubren el comportamiento; el integration test es **belt-and-suspenders**.

### Strict TDD applicability

Confirmado en el proposal y en `~/.claude/skills/_shared/strict-tdd.md`. **Cada cambio de comportamiento backend** (Decision 2/3) entra red→green→refactor:
- (R) escribir el assert que falla con código actual.
- (G) cambio mínimo para pasar.
- (Re) limpiar duplicación / nombres.

Para frontend, sin runner JS confirmado, "Strict TDD" se interpreta como **manual red→green ciclo**: ejecutar manual checklist antes del cambio (confirmar el bug), implementar, ejecutar de nuevo (confirmar fix). Esto NO sustituye unit tests pero es el mejor disponible si no hay runner.

---

## Implementation Sequence (input to sdd-tasks)

Orden de los work-unit commits, justificado por **safety + bisectabilidad + visibilidad de valor**:

1. **Add `RANGE_TO_BACKEND` const + extend `RANGES` shape** (frontend only, sin cambio de comportamiento todavía).
   - **Por qué primero**: cambio puramente declarativo, cero riesgo. Bisectable: si rompe algo, es trivial revertir.
   - **Valor visible**: ninguno todavía (intencional — el commit es plumbing).
   - **Tests**: assertion estática que `RANGE_TO_BACKEND` cubre todos los `RANGES[i].id`; smoke render check del tab strip (8 tabs visibles aunque 4 no funcionen).

2. **Wire `RANGE_TO_BACKEND` into `_fetchSlotHistory` + refactor `filterHistoryByRange` para `r.dynamic`**.
   - **Por qué segundo**: este commit **destraba los 4 tabs originales** (`1h`/`8h`/`24h`/`7d` empiezan a recibir data real del rango nombrado). El usuario ve la diferencia inmediatamente: clic en `7d` ahora muestra 7 días (truncado a primeros 2000 por backend cap, pero el dolor inmediato del proposal se resuelve aquí).
   - **Valor visible**: GIANT — primer fix del bug que disparó el bloque #73.
   - **Tests**: integration (manual o automated) — clic en `7d` debe traer JSON con `range=last7Days`.

3. **Backend: extract `computeTargetPoints` helper + WARNING logging defensivo en `computeRange` else branch** (sin tocar el stride todavía).
   - **Por qué tercero**: prepara el terreno para Step 4 sin cambiar comportamiento de truncate. Bisectable: si el helper tiene bug, no afecta runtime (no se llama todavía).
   - **Valor visible**: pequeño — futuros bugs vocabulary ya no son silenciosos.
   - **Tests**: unit tests para `computeTargetPoints` (los 8 valores de la tabla + fallback). Test del WARNING logging via captura de Logger.

4. **Backend: reemplazar `maxPoints=2000` HARDCODED por stride dinámico + integrar `computeTargetPoints` + `MAX_POINTS_HARD_CEILING`**.
   - **Por qué cuarto**: este commit resuelve el "gap final del chart en 7d" (causa secundaria del proposal). Aislado del Step 2: si introduce regresión, se revierte sin perder el fix de los 4 tabs.
   - **Valor visible**: GIANT — segundo fix grande del proposal.
   - **Tests**: unit `queryHistoryData` con cursor mock 10080 records → ≤169 puntos en response.

5. **Frontend: render UI tabs para los 4 nuevos rangos (today/yesterday/30d/mtd)**.
   - **Por qué quinto**: depende del shape `RANGES` (Step 1) y del backend wiring (Steps 2-4). Sin Steps 2-4 los nuevos tabs serían fake (caerían al `else lastHour`). Con Steps previos completos, exponerlos es seguro.
   - **Valor visible**: GIANT — feature gap eliminado, 4 rangos nuevos usables.
   - **Tests**: manual checklist por cada nuevo tab — clic, verificar JSON response con key backend correcta, verificar render del chart.

6. **`HISTORY_MINUTES` cap dinámico wiring** (introducir `_maxHistoryEntries()`, reemplazar L3535 referencia).
   - **Por qué sexto**: defensa contra Array.shift() loop post-7d. Sin Steps 2-4 este cambio es no-op (el array nunca crece más allá del cap viejo porque la data es truncada). Con Steps 2-4 sí podría crecer — pero un sólo call-site (L3535) hace que el cambio sea trivial y aislado.
   - **Valor visible**: nulo en UX directo, alto en performance latente.
   - **Tests**: simular `_currentHistoryRange = '7d'`, llenar `fullHistory` con > 1500 entries vía mock de `_loadRealHistory`, verificar que `_appendLiveSample` no trimma agresivamente.

7. **Regression smoke + manual checklist completo** (no es un commit de código sino un gate antes del PR final).
   - **Por qué último**: cierra el loop. Después de los 6 commits anteriores, ejecutar la checklist completa de Decisión 7 (rendering visual de los 8 tabs + cap dinámico + live append + comfortBand/htmlLegend siguen funcionando + RAF stagger sigue evitando reflow violations en DevTools Performance).
   - **Valor visible**: confirmación end-to-end de que ningún patrón preservado se rompió.

**Cada step es PR-able independiente** (suposición: el delivery strategy es `auto-chain` o `chained PRs recommended`). Si el delivery strategy fija `single-pr`, los 7 steps siguen siendo válidos como commits dentro del mismo PR — el orden y la bisectabilidad no cambian. La estimación de LOC del proposal (10+2+50+10+10 ≈ 80-100 LOC) está bajo el budget 400 — no se anticipa necesidad de chained PRs por tamaño.

---

## Risk Mitigation

Por decisión, qué puede salir mal y la guardia explícita:

| Decisión | Riesgo | Mitigación |
|----------|--------|------------|
| 1 (`RANGE_TO_BACKEND` location) | Alguien futuro agrega un rango a `RANGES` sin agregar entry al mapping → bug original regenerado. | Test TDD frontend: `RANGES.every(r => RANGE_TO_BACKEND[r.id])` debe ser `true`. Falla loud si falta. |
| 2 (lookup table targetPoints) | Frontend `RANGES[].points` y backend lookup divergen. | Test TDD backend: para los 4 originales, `computeTargetPoints(RANGE_TO_BACKEND[id])` debe igualar `RANGES[id].points`. Documentar en spec que ambos son source-of-truth duplicado intencional con guard. |
| 3 (stride uniforme) | Spike de duración < step se pierde. | Documentado explícitamente en spec. Flag `?fullResolution=true` para debug. Escalar a TIER-3 (min/max o LTTB) si el dominio reporta falsos negativos recurrentes. |
| 4 (`_maxHistoryEntries()` función) | `_currentHistoryRange` puede estar `undefined` durante boot transitorio. | Guard `RANGES.find()` retornando `undefined` → fallback a `HISTORY_MINUTES_FALLBACK`. Test: simular sin `_currentHistoryRange` set, verificar fallback. |
| 5 (`dynamic: true` en shape RANGES) | `filterHistoryByRange` viejo todavía hace `r.hours * 3600000` para los dynamics → cutoff wrong. | Refactor explícito en Step 2 (commit junto con `RANGE_TO_BACKEND` wiring). Test: para `today` a las 10:00 mockeado, cutoff = inicio del día (no `now - 24h`). |
| 6 (preservar client-side stride) | Defense in depth puede ocultar bug del backend stride (frontend lo "arregla" silenciosamente). | Test integration: response del backend para `last7Days` debe ya venir con ≤169 puntos antes de que frontend lo toque. Si el test pasa, frontend stride es no-op confirmable. |
| 7 (manual regression para frontend) | Manual checklist depende del rigor humano; se omite en releases apurados. | sdd-verify debe **ejecutar y reportar** la checklist como artifact persistido (engram `sdd/.../verify-report`). Sin reporte ejecutado, sdd-archive no debe cerrar el change. |

**Risks transversales** (del proposal, no resueltos por una sola decisión):

- **Vocabulary divergente futuro** — mitigado por Decisión 1 (single const en frontend) + Decisión 2 (lookup explícito backend) + tests TDD de cobertura.
- **Tests `ChiHistoryHelperTest` existentes pueden romper** — la fase sdd-spec debe inventariar y la fase sdd-apply debe preservar o documentar el cambio de assertion. **Decisión punteada** — no resuelta aquí, requiere lectura del test file en sdd-spec.
- **`detectEquipmentHistories` cache scope** — verificado read-only en ChiHistoryHelper.java:338-528 (referenciado por bloque #73). El cache `MX60.HistoryIndex` es session-scoped a histories disponibles (mapping `{equipId: {slot: histId}}`), NO contiene rangos. Cambiar rangos NO invalida el cache — confirmado por inspección.
- **Strict TDD en repo separado** — Decisión 7 explícita; sdd-apply documenta el comando Gradle antes del primer commit.

---

## Cross-refs

- **Proposal**: `/home/cristian/niagara-research/openspec/changes/mx60-history-time-range/proposal.md` (engram #1308).
- **Spec**: `/home/cristian/niagara-research/openspec/changes/mx60-history-time-range/spec.md` (sibling, written in parallel — esta design fija contratos arquitectónicos, la spec formaliza requirements por capa).
- **Bloque #73** (engram #1265 + `niagara-mental-model-bloque73.md` §73.0-§73.10): 3 veredictos arquitectónicos A/B/C + 4 decisiones HEREDAR/DESCARTAR/PRESERVAR/INVENTAR + 4 sprint-1 OBLIGATORIAS.
- **Implications nuevas referenciadas**: #299 (vocabulary mismatch ROOT CAUSE), #300 (maxPoints HARDCODED), #305 (client-side stride PRESERVAR), #311 (HISTORY_MINUTES dynamic cap), #312 (4 hidden ranges feature gap).
- **Mental model §73.6** — root cause con cita file:line + causa terciaria HISTORY_MINUTES (snippet `_maxHistoryEntries()` que Decisión 4 instancia literalmente).
- **Mental model §73.7** — decisiones HEREDAR/DESCARTAR/PRESERVAR/INVENTAR (fuente de Decisión 6 PRESERVE list).
- **Mental model §73.8** — tabla 14 implications #299..#312 (fuente empírica de cada decisión).
- **MX60 source READ-ONLY**:
  - `ChiHistoryHelper.java` L180-220 (queryHistoryData maxPoints), L257-318 (computeRange dispatch), L235-251 (extractValue inline String.format — patrón correcto a preservar), L338-528 (detectEquipmentHistories — read-only en este change).
  - `UpDetail.js` L165-177 (`TEMP_GAUGES`/`RANGES`/`DEFAULT_RANGE`), L388-396 (HISTORY_MINUTES con comment doc), L540-587 (`_fetchSlotHistory`), L880-907 (`filterHistoryByRange`), L3535 (único call-site de HISTORY_MINUTES).
  - `ChiHistoryHelperTest.java` (existe en `srcTest/test/com/angeles/chihuahua/ux/` — inventario por sdd-spec).
- **Strict TDD**: `~/.claude/skills/_shared/strict-tdd.md` (NON-NEGOTIABLE en sdd-apply).
- **Bloque #45**: precedente Niagara webChart `autoSamplingSize=2500 / maxSamplingSize=50000` — fuente del `MAX_POINTS_HARD_CEILING` y la práctica de "lookup explícito, no fórmula" (Decisión 2).
