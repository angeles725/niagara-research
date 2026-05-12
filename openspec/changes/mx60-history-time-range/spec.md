# Delta Spec: mx60-history-time-range

**Change**: `mx60-history-time-range`
**Fecha spec**: 2026-05-11
**Phase**: TIER-1 sprint-1 OBLIGATORIO
**Source**: proposal.md + bloque #73 (engram #1265) + ChiHistoryHelper.java:194,257-318 + UpDetail.js:171-176,396,552,894-907,3329-3377,3531-3541
**Status**: FINAL

---

## Purpose

Esta spec define los contratos observables que DEBEN ser verdad después de aplicar
`mx60-history-time-range`. El cambio cierra el bug "todos los tabs de historia
devuelven 1 hora" corrigiendo el vocabulary mismatch frontend↔backend
(`UpDetail.js:552` envía `1h/8h/24h/7d`; `ChiHistoryHelper.java:257-318` solo
reconoce `lastHour/last8Hours/…`), introduce un backend cap dinámico con stride
downsampling en lugar del `maxPoints=2000` HARDCODED (`ChiHistoryHelper.java:194`),
expone los 4 rangos de backend ya implementados que el frontend nunca mostró, y
hace dinámico el buffer `HISTORY_MINUTES` para que `_appendLiveSample` no entre
en `Array.shift()` de 10 080 elementos post-fix. Los 8 patterns superiores de
Chart.js v4, IIFE caches, RAF stagger, `comfortBandPlugin`, etc. no deben
regresionar.

---

## Requirements

---

### REQ-1: Contrato de vocabulary (single source of truth frontend↔backend)

**Contexto de referencia**
- `UpDetail.js:171-176` — `RANGES` ids actuales: `'1h'`, `'8h'`, `'24h'`, `'7d'`
- `UpDetail.js:552` — URL construida: `'&range=' + encodeURIComponent(range)` — envía los ids sin transformar
- `ChiHistoryHelper.java:257-318` — `computeRange` reconoce: `last8Hours`, `today`, `last24Hours`, `yesterday`, `last7Days`, `last30Days`, `monthToDate`; else → `lastHour`

**Requirements**

WHEN un tab de rango es clickeado con id `X`
THEN `_fetchSlotHistory` SHALL transformar `X` usando `RANGE_TO_BACKEND` antes de construir la URL
AND la URL enviada al backend SHALL contener el canonical key del backend, no el id del tab

WHEN `RANGE_TO_BACKEND` es definido
THEN SHALL ser un `const` de módulo declarado adyacente a `RANGES` en `UpDetail.js`
AND SHALL incluir exactamente las siguientes 8 entradas y ninguna más salvo ampliación documentada:

| Frontend id (RANGES[].id) | Backend key (ChiHistoryHelper.computeRange) |
|---------------------------|---------------------------------------------|
| `'1h'`                    | `'lastHour'`                                |
| `'8h'`                    | `'last8Hours'`                              |
| `'24h'`                   | `'last24Hours'`                             |
| `'7d'`                    | `'last7Days'`                               |
| `'today'`                 | `'today'`                                   |
| `'yesterday'`             | `'yesterday'`                               |
| `'30d'`                   | `'last30Days'`                              |
| `'mtd'`                   | `'monthToDate'`                             |

WHEN se agrega un nuevo rango al array `RANGES`
THEN SHALL existir una entrada correspondiente en `RANGE_TO_BACKEND` antes del primer commit que use el nuevo rango
AND el test de cobertura de vocabulary SHALL fallar si `RANGES` tiene un `id` sin entrada en `RANGE_TO_BACKEND`

WHEN `_fetchSlotHistory` recibe un `range` no presente en `RANGE_TO_BACKEND`
THEN SHALL hacer `console.warn('[UpDetail] unknown range id: ' + range)` y enviar `'lastHour'` como fallback seguro
AND el fallback NO SHALL ser silencioso

---

### REQ-2: Backend cap dinámico + stride downsampling

**Contexto de referencia**
- `ChiHistoryHelper.java:194` — `int maxPoints = 2000;` HARDCODED
- `ChiHistoryHelper.java:196` — `while (cursor.next() && count < maxPoints)` — descarta records nuevos al superar el cap

**Requirements**

WHEN `queryHistoryData` recibe una request con `rangeName`
THEN `maxPoints` (o su sucesor `targetPoints`) SHALL ser derivado por rango, no fijo en 2000
AND los valores de `targetPoints` por rango SHALL ser:

| Backend key       | targetPoints | Racional                                |
|-------------------|--------------|-----------------------------------------|
| `lastHour`        | 60           | 1pt/min, RANGES.points=60               |
| `last8Hours`      | 96           | 1pt/5min, RANGES.points=96              |
| `last24Hours`     | 96           | 1pt/15min, RANGES.points=96             |
| `last7Days`       | 168          | 1pt/h, RANGES.points=168                |
| `today`           | 96           | 1pt/15min hasta 24h max, upper bound    |
| `yesterday`       | 96           | 1pt/15min, 24h fijas                    |
| `last30Days`      | 180          | 1pt/4h aprox, 30 días                   |
| `monthToDate`     | 180          | 1pt/4h aprox, hasta 744h upper bound    |

WHEN el total de records devueltos por el cursor es `N` y `N > targetPoints`
THEN el backend SHALL aplicar stride downsampling con `stride = max(1, floor(N / targetPoints))`
AND el backend SHALL incluir siempre el último record del cursor en la respuesta (evitar gap final en la línea)
AND el backend SHALL NOT truncar eliminando records del extremo más reciente (no truncate-head al revés: el comportamiento actual de cursor FIFO no invierte el orden, pero el result set truncado es el head, no el tail)

WHEN el total de records `N <= targetPoints`
THEN el backend SHALL devolver todos los records sin downsampling
AND el comportamiento SHALL ser idéntico al actual

WHEN `rangeName` es desconocido (no está en el if/else de `computeRange`)
THEN `computeRange` SHALL usar `lastHour` como fallback Y SHALL emitir `LOG.warning("computeRange unknown range: " + name)` (nivel WARNING, no SEVERE)
AND el WARNING SHALL incluir el valor recibido de `name` para facilitar debugging

WHEN el stride máximo es aplicado
THEN no SHALL existir un `maxStride` hardcoded en esta iteración; el design phase definirá si se introduce un cap de stride con flag `?fullResolution=true`

---

### REQ-3: Exposición de 4 rangos de backend en el frontend

**Contexto de referencia**
- `ChiHistoryHelper.java:268,279,298,302` — `today`, `yesterday`, `last30Days`, `monthToDate` ya implementados en `computeRange`
- `UpDetail.js:171-176` — array `RANGES` actual tiene solo 4 entradas

**Requirements**

WHEN la History UI renderiza los tabs de rango
THEN SHALL mostrar exactamente 8 tabs: `1h`, `8h`, `24h`, `7d`, `today`, `yesterday`, `30d`, `mtd`
AND los 4 tabs nuevos SHALL estar presentes en el array `RANGES` con el shape correcto

WHEN `RANGES` es extendido con los 4 rangos nuevos
THEN cada entrada nueva SHALL tener los campos `{ id, label, points, step }` donde:
- `id` es el frontend id (columna izquierda de la tabla REQ-1)
- `label` es la etiqueta visible en el tab
- `points` es el `targetPoints` de la tabla REQ-2 (contrato compartido frontend/backend)
- `step` es el intervalo representativo en ms (para `filterHistoryByRange` cutoff)

WHEN los rangos `today`, `yesterday`, `monthToDate` tienen duración variable según la hora del día
THEN `hours` en RANGES SHALL ser el upper bound estático (24, 24, 744 respectivamente)
AND la decisión de si `hours` debe ser dinámico (calculado desde `computeRange` boundaries) queda DELEGADA A `sdd-design`
AND hasta que `sdd-design` resuelva, `filterHistoryByRange` usará `hours` como upper bound sin efectos negativos observables

WHEN el usuario clickea cualquiera de los 4 nuevos tabs
THEN SHALL disparar `_fetchSlotHistory` vía el mismo path que los 4 tabs originales
AND el backend key enviado SHALL ser el valor de `RANGE_TO_BACKEND[id]`
AND el comportamiento SHALL ser indistinguible del flujo de los 4 tabs originales, salvo el rango efectivo de datos

---

### REQ-4: HISTORY_MINUTES cap dinámico por rango activo

**Contexto de referencia**
- `UpDetail.js:396` — `const HISTORY_MINUTES = 24 * 60;` (único punto de definición)
- `UpDetail.js:3535` — único call site: `if (fullHistory.length > HISTORY_MINUTES + 600)`
- Comportamiento actual: buffer cliente capped en 1440 entries para todos los rangos

**Requirements**

WHEN el usuario cambia el rango activo
THEN el cap del buffer `fullHistory` SHALL reflejar el rango activo, no un valor global fijo
AND el cap efectivo SHALL ser `activeRange.hours * 60` (minutos totales del rango activo)

WHEN el rango activo es `7d` (168 horas)
THEN el cap SHALL ser `168 * 60 = 10080` entries
AND no SHALL ocurrir `Array.shift()` / `splice` mientras `fullHistory.length <= 10080 + 600`

WHEN el rango activo es `1h` (1 hora)
THEN el cap SHALL ser `1 * 60 = 60` entries
AND el buffer SHALL trimarse agresivamente: entries más antiguas que 1h SHALL ser descartadas

WHEN `HISTORY_MINUTES` es redefinido como cap dinámico
THEN el call site en `UpDetail.js:3535` SHALL funcionar correctamente sin cambio de lógica (solo cambia el valor de referencia)
AND no SHALL existir otros call sites que asuman el valor fijo 1440 — confirmado: el único call site es L3535

WHEN el cap dinámico es derivado
THEN SHALL ser expresado como `(RANGES.find(r => r.id === activeRange) || RANGES[0]).hours * 60`
AND el fallback RANGES[0] (1h) SHALL prevenir errores si `activeRange` es undefined

---

### REQ-5: Preservación de patterns superiores MX60 (regression guard)

**Contexto de referencia**
- `UpDetail.js:3273` — Chart.js v4 line chart init
- `UpDetail.js:394-430` — IIFE cache `MX60.HistoryIndex`
- `UpDetail.js:894-907` — `filterHistoryByRange` con client-side stride
- `UpDetail.js:3329-3377` — `_appendLiveSample` O(1) per notify (4 SAFETY NETs)
- `UpDetail.js:3692-3709` — RAF stagger rebuild
- `UpDetail.js:3327` — `MAX_CHART_POINTS = 200`

**Requirements**

WHEN cualquiera de REQ-1 a REQ-4 es aplicado
THEN el engine Chart.js v4 SHALL seguir siendo el engine de rendering (no migración a D3)
AND `MX60.HistoryIndex` + `MX60.HistoryListCache` IIFE SHALL continuar funcionando con scope de sesión
AND `filterHistoryByRange` en `UpDetail.js:894-907` SHALL permanecer intacto como defensa en profundidad client-side
AND el cap `MAX_CHART_POINTS = 200` dentro de `_appendLiveSample` SHALL no cambiar
AND `_appendLiveSample` SHALL seguir siendo O(1) por notify (push + conditional splice — no rebuild)
AND RAF stagger rebuild en `UpDetail.js:3701-3709` SHALL no regresionar
AND `comfortBandPlugin` + `htmlLegendPlugin` SHALL seguir renderizando bands y legend correctamente
AND `requestIdleCallback` initial defer SHALL no regresionar
AND `detectEquipmentHistories` (`ChiHistoryHelper.java:338-428`) SHALL no ser modificado

WHEN `_appendLiveSample` ejecuta el trim del chart (L3361-3368)
THEN el trim SHALL comparar contra `MAX_CHART_POINTS` (200), no contra `HISTORY_MINUTES`
AND el trim de `fullHistory` (L3535) SHALL comparar contra el cap dinámico derivado en REQ-4
AND los dos caps SON DISTINTOS y NO deben conflacionarse

---

## Scenarios (test fixtures)

### Scenario S-1: Tab `1h` — regression guard (sin cambio de comportamiento)

```
GIVEN una sesión activa con historyId="abc123"
  AND el backend contiene 60 records en la última hora
WHEN el usuario clickea el tab "1h"
THEN _fetchSlotHistory SHALL construir URL con &range=lastHour (no &range=1h)
AND el response SHALL contener ≤ 60 records
AND el primer timestamp SHALL ser >= Date.now() - 3600000
AND el último timestamp SHALL ser <= Date.now()
AND el chart SHALL renderizar sin gaps
```

### Scenario S-2: Tab `7d` — devuelve serie con stride (hoy devuelve silenciosamente 1h)

```
GIVEN una BD con 10080 records en los últimos 7 días (1pt/min)
WHEN el usuario clickea el tab "7d"
THEN _fetchSlotHistory SHALL construir URL con &range=last7Days
AND el response SHALL contener ≤ 169 records (168 + 1 last-point guarantee)
AND el primer timestamp del response SHALL ser >= Date.now() - 7*24*3600000 - 3600000 (tolerancia ±1h)
AND el último timestamp SHALL ser <= Date.now() AND >= Date.now() - step(last7Days)
AND los timestamps SHALL ser monótonamente crecientes
AND no SHALL haber un gap entre el penúltimo y último punto > 2 * step(last7Days)
```

### Scenario S-3: Tab `today` — visible y funcional

```
GIVEN una sesión activa donde la UI ha renderizado los 8 tabs
THEN el tab con label "today" SHALL ser visible en el DOM
WHEN el usuario clickea el tab "today"
THEN la URL SHALL contener &range=today
AND el response SHALL contener solo records desde medianoche del día corriente
AND filterHistoryByRange del cliente SHALL aplicar cutoff con hours=24 (upper bound)
```

### Scenario S-4: HISTORY_MINUTES cap — trim correcto al cambiar rango

```
GIVEN fullHistory con 1500 entries acumulados
  AND rango activo es "1h"
WHEN onFlush recibe un nuevo sample
THEN fullHistory.length SHALL no superar (1 * 60) + 600 = 660 entries tras el trim
AND el trim SHALL usar splice(0, 100) (batch amortizado), no shift() por elemento

GIVEN fullHistory con 8000 entries acumulados
  AND rango activo es "7d"
WHEN onFlush recibe un nuevo sample
THEN fullHistory SHALL NOT ser trimado (8000 < 10080 + 600)
AND _appendLiveSample SHALL ejecutarse en O(1) sin degrade
```

### Scenario S-5: Defensive logging — range desconocido no es silencioso

```
GIVEN frontend envía range id "bogus" a _fetchSlotHistory
THEN console.warn SHALL emitir '[UpDetail] unknown range id: bogus'
AND la URL SHALL contener &range=lastHour (fallback seguro)

GIVEN ChiHistoryHelper.computeRange recibe rangeName="bogus"
THEN LOG.warning SHALL emitir 'computeRange unknown range: bogus'
AND el response SHALL ser equivalente a lastHour (1 hora de datos)
```

### Scenario S-6: Regression — Chart.js patterns intactos

```
GIVEN cualquiera de REQ-1..REQ-4 aplicado
WHEN el usuario abre un UP con 7 trend charts
THEN los 7 charts SHALL renderizarse con Chart.js v4 (no SVG d3 elements)
AND comfortBandPlugin SHALL dibujar la comfort band en el chart de temperatura
AND htmlLegendPlugin SHALL renderizar la leyenda HTML custom
AND RAF stagger SHALL distribuir los 7 rebuilds en 7+ animation frames
AND _appendLiveSample SHALL no invocar rebuildChart en condiciones normales (SAFETY NETs 1-4 cubren los edge cases)
```

---

## Acceptance Criteria

1. **Scenarios S-1 a S-6** passing en sus respectivos test runners (Gradle JUnit para S-2/S-5 backend; QUnit/Jasmine/Jest o equivalente JS para S-1/S-3/S-4/S-5 frontend).
2. **`ChiHistoryHelperTest.java`** actualizado: assertions existentes preservadas o actualizadas con justificación documentada; assertions nuevas para stride downsampling por rango (al menos `lastHour`, `last7Days`, `last30Days`).
3. **Cobertura de vocabulary**: test verifica que todo `RANGES[i].id` tiene entrada en `RANGE_TO_BACKEND` — falla si el array crece sin actualizar el mapping.
4. **Defensive logging activo**: test verifica que `computeRange` con key desconocida emite WARNING; test verifica que `_fetchSlotHistory` con range desconocido emite console.warn y usa fallback `lastHour`.
5. **Regression guard**: manual checklist (definido en fase design) verifica los 8 patterns superiores contra DevTools Performance tab y DOM inspection.
6. **Strict TDD**: cada item implementado en orden red → green → refactor. Test runner: Niagara module Gradle (backend) + equivalente JS según `sdd-design`. No fallback a Standard Mode.

---

## Out of Scope (defer a SDDs futuros)

- **TIER-2**: multi-history endpoint comma-separated (R2.1), boxcs subscription tail (R2.2), Compare mode (R2.3), Export CSV/PNG (R2.4)
- **TIER-3**: migración D3chart, NDJSON streaming, BHistoryRollup server-side, disk cache GZIP
- **`maxStride` hardcoded + flag `?fullResolution=true`**: postergado a sdd-design (riesgo #2 del proposal)
- **Rangos dinámicos `today`/`yesterday`/`monthToDate` con `hours` calculado por reloj**: postergado a sdd-design (riesgo #4 del proposal)
- **Thread-safety `extractValue` en `ChiHistoryHelper.java:235-251`**: ya usa `String.format` inline por llamada — no es bug, no requiere cambio en este scope
- **Bugs Reflow #263/#264/#256**: viven en repo Reflow, no MX60, fuera de scope

---

## Decisions pendientes para sdd-design (abiertas)

1. **Shape de `RANGES` para rangos dinámicos**: ¿`hours` estático upper bound (simple) o campo `dynamic: true` que dispara cálculo por reloj (correcto para `today` a las 10:00 AM)? — Impacta `filterHistoryByRange` cutoff exactitud.
2. **Helper backend `computeTargetPoints` vs struct `RangeSpec`**: ¿Agregar método hermano a `computeRange` o refactorizar para retornar struct con `[start, stop, targetPoints, step]`? — Impacta testabilidad y extensibilidad futura.
3. **`maxStride` cap + flag `?fullResolution=true`**: ¿Se introduce en sprint-1 o se defiere? — Impacta el trade-off de stride ocultando spikes.
4. **Test runner JS**: ¿QUnit (ya presente en chihuahua?), Jasmine standalone, o Jest? — Impacta comando Gradle para CI.
5. **`HISTORY_MINUTES` como variable mutable vs función derivada**: ¿`let historyMinutes` actualizada en `setRange`/`onRangeChange`, o `function getHistoryMinutes()` llamada en el punto de uso? — Impacta legibilidad y riesgo de stale value.

---

## Cross-refs

- `proposal.md` — `/home/cristian/niagara-research/openspec/changes/mx60-history-time-range/proposal.md`
- Bloque #73 §73.7 — engram #1265 (triple-source audit, 4 sprint-1 OBLIGATORIAS, 14 implications #299..#312)
- Implication #299 — vocabulary mismatch root cause
- Implication #300 — maxPoints=2000 HARDCODED truncate
- Implication #305 — client-side stride SUPERIOR, preservar
- Implication #311 — HISTORY_MINUTES cap dinámico sprint-1
- Implication #312 — frontend expone 4 de 8 backend ranges, feature gap
- `ChiHistoryHelper.java:194` — maxPoints HARDCODED (READ-ONLY ref)
- `ChiHistoryHelper.java:257-318` — computeRange dispatch chain (READ-ONLY ref)
- `UpDetail.js:171-176` — RANGES const actual (READ-ONLY ref)
- `UpDetail.js:552` — URL construction sin mapping (READ-ONLY ref)
- `UpDetail.js:396` — HISTORY_MINUTES fijo (READ-ONLY ref)
- `UpDetail.js:894-907` — filterHistoryByRange client-side stride (READ-ONLY ref)
- `UpDetail.js:3329-3377` — _appendLiveSample O(1) (READ-ONLY ref)
- `UpDetail.js:3531-3541` — fullHistory trim + _appendLiveSample call site (READ-ONLY ref)
- Strict TDD skill — `~/.claude/skills/_shared/strict-tdd.md`
- Next phases: `sdd-design` (decisiones #1-#5 arriba) + `sdd-tasks` (paralelo a design o posterior)
