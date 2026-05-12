# Proposal: mx60-history-time-range

**Change**: `mx60-history-time-range`
**Fecha propuesta**: 2026-05-11
**Source investigación**: Bloque #73 (engram observation #1265 + `niagara-mental-model-bloque73.md` §73.0–§73.10)
**TIER**: 1 sprint-1 OBLIGATORIO (4 acciones)
**Implementation repo**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/` — fuera de este workspace de investigación
**TDD policy**: Strict TDD Mode ACTIVE — la fase `sdd-apply` DEBE seguir `~/.claude/skills/_shared/strict-tdd.md` con test runner Niagara module Gradle. Cada cambio de comportamiento entra red→green→refactor. No fallback a Standard Mode.

---

## Intent

El "límite de 1 hora" que el usuario percibe en los charts de MX60 NO es una limitación arquitectónica de Chart.js, NO es retention de la BD, y NO es el cap fundamental del backend. Es un **bug silencioso de vocabulary mismatch** entre frontend y backend: `_fetchSlotHistory` envía `range=1h|8h|24h|7d` (`UpDetail.js:171-176`), pero `ChiHistoryHelper.computeRange` (`ChiHistoryHelper.java:257-318`) sólo reconoce `lastHour|last8Hours|today|last24Hours|yesterday|last7Days|last30Days|monthToDate`. Ningún tab del UI matchea, todos caen al `else` final (L312-313 `cal.add(Calendar.HOUR_OF_DAY, -1)`) y devuelven la misma hora. El bug es invisible porque no hay log, no hay error HTTP, sólo data que parece "correcta" pero corresponde al rango incorrecto.

Esta propuesta desbloquea sprint-1 atacando la causa raíz (vocabulary mapping) y las 3 causas secundarias que emergen tan pronto se arregla la primera: backend `maxPoints=2000` HARDCODED que trunca (descarta) records en lapsos largos (`ChiHistoryHelper.java:194`), feature gap del frontend que sólo expone 4 de 8 rangos que el backend ya soporta, y `HISTORY_MINUTES=24*60` fijo en `UpDetail.js:396` que dispararía un loop O(N) de `Array.shift()` en `_appendLiveSample` ni bien el usuario abra `7d`. **Éxito**: los 4 tabs `1h/8h/24h/7d` devuelven data del rango nombrado real, el tab `7d` retorna serie completa con stride aplicado (no truncada), aparecen 4 rangos nuevos (`today`, `yesterday`, `last30Days`, `monthToDate`) usables desde la UI, y no hay regresión en ninguno de los patterns superiores de MX60 (Chart.js v4, IIFE cache, `_appendLiveSample`, RAF stagger).

---

## Scope

### In — sprint-1 OBLIGATORIO (4 acciones del bloque #73 §73.7)

| Acción | Descripción | Files | LOC estimadas |
|--------|-------------|-------|----------------|
| **R1.1** | `RANGE_TO_BACKEND` mapping const + uso en `_fetchSlotHistory` | `UpDetail.js:171-176` (alta const adyacente a `RANGES`) + `UpDetail.js:~552` (usar mapping antes de construir URL) | 1 const (~10 LOC) + ~2 LOC change |
| **R1.2** | Backend cap dinámico por rango + stride downsampling (NOT truncate) | `ChiHistoryHelper.java:194` (eliminar HARDCODED 2000) + `ChiHistoryHelper.java:132-229` (`queryHistoryData`) + `ChiHistoryHelper.java:257-318` (`computeRange` retornar también `step/maxPoints` por rango, o helper hermano) | ~40-60 LOC backend |
| **R1.3** | Extender `RANGES` frontend con 4 backend ranges ya soportados | `UpDetail.js:171-176` (agregar `today`, `yesterday`, `last30Days`, `monthToDate` al array `RANGES` con sus `hours/points/step`) + UI rendering de tabs (loop sobre `RANGES`) | ~10 LOC + posibles CSS de tabs adicionales |
| **HISTORY_MINUTES dinámico** | Cap dinámico por rango activo (no `24*60` fijo) | `UpDetail.js:396` → derivar de `RANGES.find(r => r.id === activeRange).hours * 60` con fallback | ~5-10 LOC |

**Patterns MX60 que se DEBEN PRESERVAR explícitamente** (auditados en bloque #73, marcados superiores vs Reflow — cualquier cambio que los rompa es regresión):

1. **Chart.js v4 line chart** (`UpDetail.js:3273`) — no migrar a D3.
2. **IIFE caches**: `MX60.HistoryIndex` + `MX60.HistoryListCache` (auto-resolution session-scoped).
3. **Client-side stride downsampling** en `filterHistoryByRange` (`UpDetail.js:894-907`) — `stride = max(1, floor(filtered.length / r.points))`. SUPERIOR al Reflow no-sampling.
4. **`_appendLiveSample` O(1)** per notify (`UpDetail.js:3329-3377`) — append directo a Chart.js dataset sin rebuild.
5. **RAF stagger rebuild** (`UpDetail.js:3692-3709`) — elimina "Forced reflow took 300-500ms" violations.
6. **Canvas pre-cache + `requestIdleCallback`** initial defer — UX superior.
7. **`comfortBandPlugin` + `htmlLegendPlugin`** propios — overlays HVAC-específicos.
8. **`detectEquipmentHistories`** auto-resolution via link graph (`ChiHistoryHelper.java:338-428`) — INVENTADO en MX60, sin contraparte Reflow.

### Out — defer explícito a SDDs futuros

**TIER-2 sprint-2** (4 candidatos, NO en esta propuesta):
- **R2.1 multi-history endpoint** comma-separated (heredar de Reflow `historyName.split(",")`) — defer porque sprint-1 cierra la queja del usuario sin esto; impacto secundario de payload reduction; requiere coordinación con `_loadRealHistory` per slot.
- **R2.2 boxcs subscription tail** + `bajaHeartbeat.start` — defer porque la solución actual de polling REST 5s + EquipmentSnapshotStore RAF 500ms (bloque #71) ya es viable; pasar a boxcs es optimización, no fix.
- **R2.3 Compare mode** (port de `CompareRangeCalculator.java` Reflow 316 LOC) — defer porque es feature nueva, no fix; el usuario no la pidió para sprint-1.
- **R2.4 Export CSV/PNG** — defer por la misma razón: feature nueva, no fix.

**TIER-3 DEFER indefinido**:
- **Migración a D3chart** — REJECTED en bloque #73 §73.0 veredicto (C). D3chart Reflow es 3114 LOC over-engineered para HVAC sub-day. NO se considera.
- **NDJSON streaming** estilo `WebChartQueryServlet` nativo (bloque #45) — defer; requiere reescribir el endpoint REST completo. Considerar sólo si payload size pasa a ser bottleneck real.
- **`BHistoryRollup` server-side** — defer; alternativa nativa Niagara al stride client-side. No es necesaria mientras stride client funcione.
- **Disk cache GZIP** estilo Reflow `HistoryIO` — defer; el usuario no reportó latencia de fetch como dolor.

**Bugs de Reflow NO incluidos** (son del repo Reflow, no MX60):
- #263 `last24Hours` operator precedence (`historyCache.js:57` — `24*DAY/24*24` = 24 días).
- #264 skip-cache `state.groups` undefined NEVER fires (`historyCache.js:489`).
- #256 `SimpleDateFormat`+`DecimalFormat` thread-safety en `HistoryDataResponse.java:39-40` (Reflow).

**Aclaración thread-safety MX60**: el `extractValue` inline en `ChiHistoryHelper.java` (L235-251) usa `String.format` por llamada — no replica el patrón estático bug-prone de Reflow. **No hay replica del #256 en MX60** para este scope. El bug `#256` REPLICA mencionado en bloque #70 vive en archivos de Reflow no en MX60.

### Out — explícitamente no tocado

- `chihuahua-rt` srcTest tests — los tests del helper viven en `chihuahua-ux/srcTest/test/.../ChiHistoryHelperTest.java`. Strict TDD obliga a actualizarlos junto con R1.2; no se elimina ninguna assertion existente sin justificación documentada.
- Otros dominios (alarms — bloque #72, equipment — bloque #71, navigation, login). Sprint-1 es scope-locked a History+Data.

---

## Approach

El plan respeta la cadena de causalidad descubierta en bloque #73: arreglar el vocabulary mismatch sin las otras 3 acciones expone bugs secundarios; las 4 acciones forman una unidad mínima coherente.

**Capa 1 — Frontend mapping (R1.1)**. Se introduce una sola fuente de verdad para el vocabulary: un `const RANGE_TO_BACKEND = { '1h': 'lastHour', '8h': 'last8Hours', '24h': 'last24Hours', '7d': 'last7Days', ... }` (incluye también los 4 IDs nuevos de R1.3 desde el día uno). En `_fetchSlotHistory` (`UpDetail.js:~552`), antes de construir la URL, se transforma `range` con este mapping y se envía la key backend correcta. **Por qué un const y no inline**: el mismo vocabulary se va a referenciar desde tests, posibles futuros endpoints (R2.1 multi-history), y desde la lógica de cap dinámico (`HISTORY_MINUTES`). Una sola fuente evita la divergencia que en Reflow ya produjo 3 vocabularies (#302 backend 15 vs ChartToolBar 13 vs TimeRangePicker fallback 15 con 6 keys ficticios). Tests TDD: red contra `_fetchSlotHistory` debe verificar que para `range='7d'` la URL contiene `range=last7Days`; los 4 IDs nuevos del R1.3 también deben mappear.

**Capa 2 — Backend dispatch (parte fácil de R1.2)**. La función `computeRange` (`ChiHistoryHelper.java:257-318`) ya soporta las 8 keys correctamente. Una vez que R1.1 entrega las keys correctas, esta función deja de caer al `else` `lastHour`. **No requiere cambio funcional**, pero sí se refactoriza el bloque `else` final para que loguee un WARNING cuando reciba una key desconocida (defensive logging — sin esto, futuros bugs de vocabulary volverían a ser silenciosos). Tests TDD: verificar que `computeRange("last7Days")` retorna `[start, stop]` con delta de 7 días exactos; verificar que `computeRange("foo")` cae a `lastHour` Y emite WARNING.

**Capa 3 — Backend cap dinámico + stride (parte central de R1.2)**. El `maxPoints = 2000` HARDCODED de `ChiHistoryHelper.java:194` es el segundo problema serio: en `last7Days` con BD a 1pt/min (10080 records) el backend descarta 8080 records porque hace `cursor.next() && count < 2000` (L196) — toma los primeros 2000 que la BD entrega (orden del cursor), no aplica stride. El resultado: chart con head completo y gap final. Fix: derivar `targetPoints` del rango — usar el mismo `RANGES[].points` del frontend (60/96/96/168 + nuevos) como contrato compartido, traducido al backend en un helper paralelo a `computeRange` (`computeTargetPoints(String rangeName)` o un struct `RangeSpec`). Aplicar **stride downsampling** equivalente al cliente: `stride = max(1, totalRecords / targetPoints)`, iterar `cursor` saltando, y siempre incluir el último record para que la línea termine en "now" (mismo trick que `filterHistoryByRange` L902-905). **Por qué stride y no LTTB**: LTTB requiere doble pass o ventana — el cursor de BHistoryDatabase es single-pass forward; stride es O(N) sin doble pass y preserva la forma suficiente para HVAC dashboards. **Trade-off documentado**: stride puede ocultar spikes de duración < step. Mitigación: el cap de `maxStride` (por ejemplo 600 = stride máximo aceptable) más una flag opcional `?fullResolution=true` para debugging. La preservación del trabajo client-side stride se mantiene (es defensa en profundidad: si backend devuelve más records que `r.points`, frontend re-aplica stride; si backend ya devolvió exactamente `r.points`, frontend no hace nada). Tests TDD: con cursor mock de 10080 records y rango `last7Days` (targetPoints=168), el response JSON debe tener ≤ 168 + 1 puntos, primer punto cerca de `start`, último punto cerca de `stop`, monotonic timestamps.

**Capa 4 — Frontend exposición de los 4 rangos nuevos (R1.3)**. El backend ya soporta `today, yesterday, last30Days, monthToDate` — el frontend simplemente no expone tabs. Agregar 4 entradas al array `RANGES` con sus correspondientes `hours/points/step` (`today`: hasta 24h variable según hora actual — usar 24 como upper bound; `yesterday`: 24h fijas; `last30Days`: 720h; `monthToDate`: hasta 744h variable). El UI rendering ya itera sobre `RANGES` (`UpDetail.js:327` button rendered from `RANGES`), por lo que basta extender el array — la lógica de tabs es genérica. **Por qué incluir esto en sprint-1 y no diferir**: el bloque #73 implication #312 lo identifica como feature gap obvio (4 de 8 backend ranges escondidos) y el costo marginal es ~10 LOC sobre el R1.1 ya hecho. Tests TDD: por cada nuevo `RANGES[i]`, verificar que click en el tab dispara fetch con la key backend correcta y que `filterHistoryByRange` filtra usando `hours` correcto.

**Capa 5 — `HISTORY_MINUTES` cap dinámico**. Hoy `UpDetail.js:396` es `const HISTORY_MINUTES = 24 * 60` (1440 entries cap, comentario L390-395 explica el trade-off original). Tan pronto el usuario abra `7d` con R1.1+R1.2 funcionando, el buffer cliente acumulará 10080 entries (asumiendo 1pt/min) y `_appendLiveSample` empezará a hacer `Array.shift()` en cada notify — O(N) por shift, RAF 500ms = 2x/segundo, 10080 elementos = ~5040 movimientos por shift. Fix: derivar `HISTORY_MINUTES` del rango activo: `const minutes = (RANGES.find(r => r.id === activeRange) || RANGES[0]).hours * 60`. Esto permite que el cap crezca cuando el usuario está en `7d` (10080) y se reduzca cuando vuelve a `1h` (60). El `Array.shift()` sigue siendo O(N) por elemento descartado pero ahora sólo se ejecuta cuando el buffer realmente sobrepasa el rango activo, no porque el cap global sea pequeño. Tests TDD: simular cambio de rango `1h → 7d`, verificar que `HISTORY_MINUTES` o su equivalente se actualiza; con 12000 entries acumulados y rango `1h`, verificar que el buffer se trimma a 60 entries.

**Orden de implementación recomendado** (strict TDD red-green-refactor cada paso): (1) R1.1 mapping const + use en `_fetchSlotHistory`. (2) Backend dispatch logging defensivo en `computeRange` else branch. (3) R1.2 cap dinámico + stride en `queryHistoryData`. (4) R1.3 nuevos rangos en `RANGES`. (5) `HISTORY_MINUTES` dinámico. Cada paso es PR-able independiente; los 5 forman un único change set coherente para sprint-1.

---

## Why now

Sin estas 4 acciones, el sprint-1 history MX60 no se destraba. Específicamente:

- **Sin R1.1**: los 4 tabs son fake. El usuario hace click en `7d` y ve la misma data de `1h`. Es la queja literal que disparó el bloque #73. Coste de no hacerlo: la feature actual está visualmente rota.
- **Sin R1.2**: tan pronto R1.1 entregue keys correctas, el tab `7d` empieza a llegar al backend bien, pero el cap 2000 HARDCODED le entrega al usuario un chart con 2000 puntos del head (las primeras horas del rango) y nada de las últimas. Coste: chart aparece con "gap final" inexplicable.
- **Sin R1.3**: el backend desperdicia 4 rangos que ya implementó. `today`, `yesterday`, `last30Days`, `monthToDate` están en `computeRange` (L268, L279, L298, L302) pero ningún botón del UI puede llamarlos. Coste: feature gap visible al primer code review interno.
- **Sin `HISTORY_MINUTES` dinámico**: post-R1.2 el `7d` trae 10080 records al cliente, `_appendLiveSample` arranca `Array.shift()` cada 500ms con array de 1440 capped, descartando samples vivos y degradando el live update. Coste: live update entra en performance degradation invisible (no crash, sólo lag).

Las 4 acciones forman una **unidad mínima coherente**. Hacer 3 de las 4 deja al usuario con un dolor secundario diferente del original pero igual de visible. Por eso entran al mismo SDD change y no se separan.

---

## Risks

1. **Vocabulary mapping divergente futuro** — alguien más adelante puede agregar un rango al backend (ej. `last12Hours` del vocabulary Reflow) sin actualizar `RANGE_TO_BACKEND` en frontend, regenerando el bug original. **Mitigación**: el const `RANGE_TO_BACKEND` debe ser la **única** fuente de verdad de keys backend; el test TDD del helper backend debe enumerar las keys aceptadas y el test TDD del frontend debe verificar que cada `RANGES[i].id` tiene entrada en el mapping. La regla de pattern `single source of truth for vocabulary` queda explícita en la spec.

2. **Stride downsampling oculta spikes** — para HVAC dashboards, un spike de duración menor al `step` del rango puede no aparecer en el chart si el stride se lo come. Mitigación documentada en spec: stride preserva la primera muestra del bucket (no min/max ni avg), y se documenta el trade-off; ofrecer flag opcional `?fullResolution=true` para debugging diagnóstico (no expuesto al UI usuario regular).

3. **Backend `queryHistoryData` ya tiene tests existentes** — la refactor para introducir cap dinámico puede romper tests existentes en `ChiHistoryHelperTest.java`. **Mitigación**: la fase `sdd-spec` debe inventariar los tests vivos del helper antes de la fase `sdd-apply`; strict-tdd exige red→green→refactor con tests existentes preservados o explícitamente actualizados con justificación.

4. **Nuevos rangos `today` / `yesterday` / `monthToDate`** tienen rango variable (no fijo en horas). El `RANGES[].hours` se usa hoy para 2 cosas en frontend: `filterHistoryByRange` cutoff (L896) y derivación de `HISTORY_MINUTES` propuesta arriba. Para `today` a las 10:00 AM, `hours` debería ser 10, no 24. **Mitigación**: en lugar de `hours` estático para los 4 rangos nuevos, calcular `hours` dinámicamente desde `computeRange` boundaries o tener un campo `dynamic: true` que dispare el cálculo de cutoff por reloj. La spec debe definir el shape exacto del RANGES schema extendido — ESTA ES UNA DECISIÓN PENDIENTE PARA LA FASE `sdd-design`.

5. **Polling guard ≥30s** del bloque #69 #245 (HistoryChart.vue:304-309) es de Reflow, no MX60 — MX60 usa EquipmentSnapshotStore RAF 500ms. Cambiar el cap `HISTORY_MINUTES` no debería afectar el polling, pero hay que verificar que `_appendLiveSample` no asume un cap fijo en ningún otro lugar del archivo. **Mitigación**: `sdd-spec` enumerará todos los call sites de `HISTORY_MINUTES` para confirmar que un único punto de uso (o pocos) absorbe el cambio.

6. **`detectEquipmentHistories` auto-resolution** (`ChiHistoryHelper.java:338-428`) NO se toca en esta propuesta, pero el endpoint `equipment-histories` corre antes de cualquier `historyData` request. Hay que verificar que el cache `MX60.HistoryIndex` no caché-ee per-range — debe ser session-scoped a histories disponibles, no a rangos. **Mitigación**: verificación en `sdd-design`.

7. **Strict TDD en repo separado** — la implementación es en `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`, no en este workspace. El test runner es Niagara module Gradle. La fase `sdd-apply` debe arrancar declarando dónde corren los tests y verificando que el comando Gradle pasa antes de cada commit. **Mitigación**: spec inicial debe documentar el comando exacto Gradle (algo como `gradle :chihuahua-ux:test --tests ChiHistoryHelperTest` y equivalente JS si lo hay).

---

## Success criteria

1. **Los 4 tabs originales** (`1h`, `8h`, `24h`, `7d`) retornan data del rango nombrado real — no la hora silenciosa actual. Verificable por timestamp del primer y último punto del JSON de `/mx60/api/historyData?id=...&range=last7Days` (delta ≈ 7 × 86400000 ms).
2. **El tab `7d`** retorna serie con stride aplicado, no truncada a primeros 2000 puntos. Verificable: con BD de 10080 records, el response trae ≤ 168+1 puntos, último punto dentro de los últimos `step` (3600000 ms) desde `now`.
3. **Los 4 rangos nuevos** (`today`, `yesterday`, `last30Days`, `monthToDate`) visibles y funcionales en la UI; cada uno dispara fetch con la key backend correcta.
4. **Live update sin degradación**: cambiar `1h → 7d` no induce lag perceptible en `_appendLiveSample`; el buffer cliente se ajusta al cap dinámico.
5. **Patterns preservados — verificación específica**: `Chart.js v4` sigue siendo el engine (no D3), `MX60.HistoryIndex` y `MX60.HistoryListCache` siguen funcionando session-scoped, `filterHistoryByRange` sigue aplicando stride client-side (defensa en profundidad), `RAF stagger rebuild` sigue evitando reflow violations en DevTools Performance tab, `comfortBandPlugin` + `htmlLegendPlugin` siguen renderizando bands y legend correctamente.
6. **Tests strict-TDD pasan**: `ChiHistoryHelperTest` actualizado con nuevos asserts por rango; tests JS (o equivalente) cubren `RANGE_TO_BACKEND` y la lógica de `HISTORY_MINUTES` dinámico.
7. **Defensive logging** activo: `computeRange` con key desconocida emite WARNING — el bug original ya no puede ser silencioso.

---

## Cross-refs

- **Engram #1265** — `bloque/73/history-data-tier1-triple-source` (3 veredictos arquitectónicos A/B/C, 4 bugs latentes, 14 implications #299..#312, decisiones HEREDAR/DESCARTAR/PRESERVAR/INVENTAR, 4 sprint-1 OBLIGATORIAS).
- **Mental model** — `/home/cristian/niagara-research/niagara-mental-model-bloque73.md` (~545 LOC, §73.0–§73.10) — fuente primaria de evidencia empírica.
- **INDEX** — `/home/cristian/niagara-research/INDEX.md` Capa 19 entry #73 (Transplante operacional, triple-source TIER-1 History+Data).
- **Implications nuevas** — #299 vocabulary mismatch (root cause), #300 maxPoints=2000 HARDCODED truncate, #305 client-side stride SUPERIOR preservar, #306 EquipmentSnapshotStore RAF 500ms SUPERIOR, #307 RAF stagger + canvas pre-cache, #310 requestIdleCallback initial defer, #311 HISTORY_MINUTES cap dinámico sprint-1, #312 frontend solo expone 4 de 8 backend ranges feature gap.
- **Bloques relacionados** — #45 (WebChart nativo, inspiración cap backend), #62 §62.9.3 (polling componentes paralelo conceptual), #68 §68.1 (transplante backend blueprint), #69 #245 (polling guard ≥30s) + #249 (bajaHeartbeat DORMANT sprint-1), #70 #254/#256/#262 (HistoryData audit reconfirmado), #71 #279 (HISTORY_MINUTES cap UpDetail.js:396).
- **MX60 source READ-ONLY** — `ChiHistoryHelper.java` (619 LOC, audited L194 y L257-318), `UpDetail.js` (3841 LOC, audited L171-176 / L390-396 / L545-580 / L894-907).
- **Strict TDD** — `~/.claude/skills/_shared/strict-tdd.md` (NON-NEGOTIABLE para fase `sdd-apply`).
- **Next phases** — `sdd-spec` (contratos formales por capa: mapping, dispatch, cap dinámico, RANGES extendido, HISTORY_MINUTES) y `sdd-design` (esquema final de `RANGES` para rangos dinámicos `today/monthToDate`, contrato del helper backend `computeTargetPoints` o `RangeSpec`, decisiones de logging defensivo). Pueden correr en paralelo.
