# Bloque #74 — Mini-corrigendum a Bloque #72: ack alarmas MX60 frontend

**Fecha**: 2026-05-10
**Tipo**: Mini-audit empírico — corrige veredicto (A) de §72.4 ("Ack flow YA EXISTE funcional end-to-end")
**Trigger**: observación usuario 2026-05-10 — "el botón ack en frontend no hace nada", contradice análisis estático bloque #72
**Source READ-ONLY**: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/chihuahua-ux/`
**Methodology applied**: engram #1238 clean-room-disconnected-asymmetry + #1236 mapping-vs-empirical-audit (TIER-1 a NIVEL VEREDICTO de bloque previo)

---

## §74.0 — Punto exacto de ruptura

### Hallazgo central

El frontend **silencia universalmente toda falla del backend ack** porque trata `xhr.status === 200` como éxito **sin inspeccionar `ackedCount` ni `failedCount`** en el payload JSON. Tres XHR-paths independientes comparten el mismo bug de diseño.

Esto significa: incluso si el backend ejecuta `ChiAlarmHelper.ackAlarms` y retorna `{"ackedCount":0,"failedCount":N,"errors":["reflection probe failed"]}` con HTTP 200 OK, el frontend muestra "Reconocida" (Toast) o llama `onSuccess(result)` y refresca la lista. Visualmente el usuario percibe que "el botón no hace nada" — porque la lista se refresca pero las alarmas siguen `unacked`.

### Cita literal del bug — 3 paths simétricos

**Path 1 — `AlarmsManager.ackAlarms` (D-001/D-002, retry-aware, "modern path")**
`chihuahua-ux/src/rc/js/app/AlarmsManager.js:284-291`:
```js
xhr.onreadystatechange = function () {
  if (xhr.readyState !== 4) return;
  if (xhr.status === 200) {
    var result;
    try { result = JSON.parse(xhr.responseText); } catch (e) { result = {}; }
    if (typeof opts.onSuccess === 'function') opts.onSuccess(result);
    // Reload alarm list after successful ack.
    loadAll(null);
  } else if (attempt + 1 < MAX_ATTEMPTS) {
```
El comentario dice **"after successful ack"**, pero `result.ackedCount === 0` cuenta como "successful" porque el chequeo es solo `xhr.status === 200`. Sin inspección de `ackedCount`, sin error toast cuando `failedCount > 0`.

**Path 2 — `AlarmCards._doAck` (single-card modal)**
`chihuahua-ux/src/rc/js/app/AlarmCards.js:290-296`:
```js
xhr.onreadystatechange = function() {
  if (xhr.readyState !== 4) return;
  var ok = (xhr.status === 200 || xhr.status === 204);
  if (MX60.Toast) MX60.Toast.show(ok ? 'Reconocida' : 'Error al reconocer', ok ? 'ok' : 'error');
  if (ok && typeof opts.onAck === 'function') opts.onAck(uuids);
};
```
Toast literal `"Reconocida"` aunque `ackedCount=0`. **Falsa confirmación visual al usuario**. Esto solo es perceptible si el operador verifica que la alarma SIGUE en estado `unacked` después.

**Path 3 — `AlarmModalActions._postAck` (legacy duplicate, flagged en #72 #289)**
`chihuahua-ux/src/rc/js/app/AlarmModalActions.js:50-62`:
```js
function _postAck(uuids, callback) {
  var url = _ackUrl();
  var xhr = new XMLHttpRequest();
  xhr.open('POST', url, true);
  xhr.setRequestHeader('Content-Type', 'application/json');
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.onreadystatechange = function () {
    if (xhr.readyState !== 4) return;
    if (callback) callback(xhr.status === 200 || xhr.status === 204 ? null : ('HTTP ' + xhr.status));
  };
  xhr.onerror = function() { if (callback) callback('Error de red'); };
  xhr.send(JSON.stringify({ uuids: uuids }));
}
```
Mismo bug. Callback recibe `null` (= success) para cualquier `200`.

### Backend siempre devuelve 200

`chihuahua-ux/src/com/angeles/chihuahua/ux/BChiServlet.java:1187-1194` (handler):
```java
ChiAlarmHelper.AckResult result =
    ChiAlarmHelper.ackAlarms(uuids, remoteUser, this);

LOG.info("handleAlarmAck: acked=" + result.ackedCount
    + " failed=" + result.failedCount + " user=" + remoteUser);

resp.setStatus(HttpServletResponse.SC_OK);
out.print(result.toJson());
```
El servlet **siempre** retorna `SC_OK` (200) cuando el handler termina sin throw — **incluso si `ackedCount=0` y `failedCount=uuids.length`**. El status HTTP no refleja la semántica del resultado.

### Las 3 vías por las que `ackAlarms` puede retornar 0/N silenciosamente

`chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAlarmHelper.java`:

**(F1) Reflection probe falló al class-load** — L361-368:
```java
if (!hasAckSupport)
{
  String[] errors = new String[uuids.length];
  for (int i = 0; i < errors.length; i++)
    errors[i] = "reflection probe failed — BAlarmService.acknowledgeAlarm not available";
  LOG.warning("ChiAlarmHelper.ackAlarms: ack-not-supported (stub mode), "
      + uuids.length + " alarm(s) NOT acked for user=" + username);
  return new AckResult(0, uuids.length, errors);
}
```
4 strategies (L128-172). Si las 4 fallan en iSMA 4.13.2 deployment, `hasAckSupport=false` queda cacheado para toda la vida del classloader.

**(F2) `BAlarmService` no resoluble en `Sys.getService()`** — L394-398:
```java
if (alarmService == null)
{
  for (int i = 0; i < uuids.length; i++) errors.add("alarm-service-not-found");
  return new AckResult(0, uuids.length, errors.toArray(new String[0]));
}
```
Mismo patrón silencioso.

**(F3) BQL `select where uuid='...'` no devuelve `BAlarmRecord`** — L415-426:
```java
String bql = "station:|alarm:|bql:select * where uuid='" + uuid + "'";
BITable table = (BITable) BOrd.make(bql).get(context, null);
cursor = table.cursor();

BAlarmRecord rec = null;
if (cursor.next()) rec = (BAlarmRecord) cursor.get();

if (rec == null)
{
  failedCount++;
  errors.add("uuid-not-found: " + uuid);
  continue;
}
```
Si el uuid llegó al backend pero la BAlarmDb no lo encuentra (alarma ya purgada, encoding diferente, índice corrupto, etc.) → `failedCount=N` con errors `"uuid-not-found"`.

### Failure modes adicionales SIN llegar al backend

**(F4) Botón AlarmCards `disabled` por falta de `uuid`/`uuids` en payload del row** — `AlarmCards.js:135`:
```js
'<button type="button" class="mx60-btn mx60-btn--danger" ' +
  'data-alarmcard-action="ack"' + (function() {
    var hasAckable = !!(alarm.uuid || (alarm.uuids && alarm.uuids.length > 0));
    return hasAckable ? '' : ' disabled title="ACK no disponible (firmware)"';
  }()) + '>' +
'Reconocer' +
```
Si el row del backend llega sin `uuid`/`uuids` (ChiAlarmQueryHelper L94-115 los puebla, pero defensive: `try { ... } catch (Exception ignore) {}` swallows todo), botón queda `disabled`. Click **no dispara nada** y title oculto al usuario salvo hover.

**(F5) `AlarmsPage._handleBulkAck` con rows sin uuids** — `AlarmsPage.js:567-573`:
```js
console.warn('[AlarmsPage] _handleBulkAck: source rows have no uuid/uuids fields — backend issue?');
if (window.MX60 && MX60.Toast && typeof MX60.Toast.warn === 'function') {
  MX60.Toast.warn('No hay UUIDs disponibles para reconocer (revisar backend)');
}
return;
```
Aquí SÍ hay toast user-visible (único path que sí surface el problema). Si el operador usa el bulk bar y el backend stripped uuids, ve "No hay UUIDs disponibles". En cualquier OTRO path el silencio es total.

### Veredicto §74.0

**El "botón no hace nada" es consistente con TODOS los modos F1..F5**. Sin acceso a DevTools del operador no puedo distinguir cuál es la causa específica de la sesión observada. Pero los 5 son SÍNTOMAS del mismo **bug de diseño raíz**:

> **Frontend treats HTTP 200 as success without inspecting `ackedCount`/`failedCount`. Backend always returns 200 even when 0 alarms were acked.**

Este bug de diseño **transforma TODAS las failure modes backend en "el botón parece no hacer nada"** (o peor: false-success Toast en `AlarmCards`). El usuario tiene razón: empíricamente, el botón no acked alarmas. Aún si el backend "funciona" en strategy 1 o 2, cualquier transient failure (BQL miss, invocation exception, service unresolvable) queda silenciado.

### Plan de disambiguación empírica (lo que debería hacer el operador)

Para identificar cuál de F1..F5 está activo en deployment Honeywell:

1. **DevTools → Network**: click ack, ver request/response.
   - Si NO hay request → F4 (botón disabled) o handler nunca dispara.
   - Si hay 200 response con `{"ackedCount":0,"failedCount":N,"errors":[...]}` → leer `errors[0]`.
2. **Logs de la station** (jaceService o spy log):
   - `WARNING ChiAlarmHelper.ackAlarms: ack-not-supported (stub mode)` → F1.
   - `WARNING ChiAlarmHelper.ackAlarms: cannot resolve BAlarmService` → F2.
   - `WARNING ChiAlarmHelper.ackAlarms: failed for uuid=... (strategy=N)` → F3 (invocation exception).
   - `errors[]=["uuid-not-found: ..."]` → F3 (BQL miss).
3. **Console del browser**:
   - `[AlarmsPage] _handleBulkAck: source rows have no uuid/uuids fields` → F5 (uuid stripping).
   - `[AlarmsPage] MX60.Confirm not available!` → modal nunca se abre.

---

## §74.1 — Por qué el static analysis de #72 no lo detectó

Bloque #72 §72.4 veredicto (A) afirmó:

> "Ack flow YA EXISTE funcional end-to-end — NO es gap. Evidence: ChiAlarmHelper.ackAlarms L353 + BChiServlet handleAlarmAck L1158 + AlarmsManager.ackAlarms L261 (POST /mx60/api/alarms/ack + 3x retry exp backoff 100/200/400ms) + AlarmsPage._handleBulkAck L549-611 (MX60.Confirm + bulk ack)."

**Lo que verificó (correcto)**:
- ✅ El handler existe en el servlet (L1158).
- ✅ La ruta está registrada en `ChiServletDispatch.route` (L357-360).
- ✅ El XHR del frontend apunta a la URL correcta (L281).
- ✅ El header `X-Requested-With: XMLHttpRequest` está presente (L283), pasa el guard de L339-343.
- ✅ El helper invoca strategies via reflection (L429-447).

**Lo que NO verificó (gap metodológico)**:
- ❌ Si el frontend **inspecciona la semántica** de la response (`ackedCount` vs `failedCount`).
- ❌ Si el backend **propaga la falla en el HTTP status** o solo en el body.
- ❌ Si los failure modes intermedios (probe failed, BQL miss, service unresolved) son **user-visibles** o silenciosos.
- ❌ Si las DEFENSIVE swallow-all `catch (Exception ignore) {}` en ChiAlarmQueryHelper L95, L115 pueden producir rows sin uuid → triggering F4/F5.

### Lección metodológica — gap del audit estructural

El audit estructural responde **"¿está cableado?"** — y aquí la respuesta es SÍ. Pero "cableado" no implica "funcional para el usuario". Para refutar el framing "ack funciona", se necesita:

**Audit semántico** (lo que faltó):
1. Trazar el camino feliz **y los tres caminos tristes** desde el helper hasta el render del Toast.
2. Verificar que cada **falla intermedia** (probe fail, BQL miss, invocation exception, swallow catch) produce un mensaje **user-visible** distinto del path feliz.
3. Comprobar que el contrato HTTP-status / body-semantics es coherente — **status 200 ↔ ackedCount > 0** debe ser invariante, o el frontend debe inspeccionar el body.

Bloque #72 ejecutó audit estructural impecable. Pero la metodología engram #1238 (clean-room-disconnected-asymmetry) — pensada para detectar simulacros frontend desconectados del backend — **es más amplia**: también aplica a **simulacros de respuesta backend silenciados por el frontend**. Esa segunda dirección de la asimetría no estaba contemplada en el bloque #72.

### Implication nueva #299 (extiende #1238)

**#299 — clean-room-disconnected-asymmetry aplica en AMBOS sentidos**: (a) frontend simula respuesta sin backend conectado; (b) backend retorna failure detail pero frontend silencia/ignora. Auditoría completa requiere trazar **ambas direcciones del contrato**, no solo "¿dispara la cadena de funciones?". Cross-ref bloque #72 §72.4 veredicto (A) corregido por bloque #74. Bug de diseño identificable solo via análisis SEMÁNTICO del response handling, no via wire-trace.

### Re-confirmación de #1236

**#1236 (mapping-vs-empirical-audit)** queda re-confirmado a NIVEL **veredicto-de-bloque-previo**: incluso un bloque empírico TIER-1 puede afirmar "funcional end-to-end" cuando solo verificó wiring, no semántica. La observación del usuario "no hace nada" fue empírica TIER-0 (testing user-facing) y refutó un veredicto TIER-1 que se basó solo en code-trace estático.

---

## §74.2 — Decisión sobre SDD scope

### Estado previo (§72.7)

Bloque #72 recomendó escenario **(b)** para SDD pending `mx60-alarms-latch-mode-change`:
- Incluir T1.2 (endpoint `/api/alarms/uuids-by-source` para fix bulk ack >200 cap).
- Incluir T1.3 (consolidate `_postAck` → `AlarmsManager.ackAlarms`).
- Incluir T1.4 (`acknowledgmentRequiresNote` gate condicional Honeywell).
- Documentar Sistema 1 vs Sistema 2 en design.md.

### Nuevo hallazgo — qué cambia

T1.3 (consolidación de paths duplicados) **es ortogonal al bug de §74.0**. Los 3 paths comparten el mismo bug — consolidarlos NO arregla la silenciación. De hecho, deduplicar a un solo path con el bug **propaga** la falla a todos los call sites.

**Tarea NUEVA T1.5 — "Ack response semantics + failure surfacing"** (sprint-1, bloqueante a T1.3):

(1) **Backend**: `BChiServlet.handleAlarmAck` debe retornar status no-200 cuando `ackedCount === 0 && uuids.length > 0`. Propuesta: `502 Bad Gateway` si la causa es probe-fail (sistema-level), `404` si todos uuid-not-found, `500` si invocation exceptions. Mantener body JSON con `errors[]` para diagnóstico.

(2) **Frontend `AlarmsManager.ackAlarms`** (L284-301): inspeccionar `result.ackedCount` y `result.failedCount`. Si `ackedCount < uuids.length`, llamar `opts.onError` con detalle y mostrar Toast con texto del primer error (no genérico).

(3) **Frontend `AlarmCards._doAck`** (L290-296): chequear `result.ackedCount` antes del Toast "Reconocida". Si `< uuids.length`, Toast `error` con causa.

(4) **Frontend `AlarmModalActions._postAck`** (L50-62): mismo patrón antes de retornar `null` (success) al callback.

(5) **Logging diagnóstico**: agregar `LOG.warning` en cada return early de `ChiAlarmHelper.ackAlarms` con counter `ack_failure_mode={F1|F2|F3a|F3b}` para que sysadmin pueda triagear sin parsear body JSON.

### Recomendación de scope SDD

**Opción A** — extender el SDD pending: agregar T1.5 al scope de `mx60-alarms-latch-mode-change` con orden T1.5 → T1.3 → T1.2 → T1.4. Pro: una sola wave de spec/design/apply/verify. Con: el SDD ya tenía scope amplio (latch + ack-as-concept); agregar response-semantics lo carga aún más y mezcla 3 ejes de cambio (latch property, deduplication, response semantics).

**Opción B** — SDD separado `mx60-alarms-ack-response-semantics` ANTES del existente: nuevo SDD focused solo en T1.5 (5 archivos: BChiServlet + 3 JS + ChiAlarmHelper logging). 1-2 días de implementación. Pro: scope quirúrgico, fácil verificar, fix rápido del bug user-visible. Con: dos SDDs en sequence.

**Opción C** — fix sin SDD: el bug es localizado y mecánico (5 archivos, ~50 líneas modificadas). No requiere arquitectura nueva. Podría ir como hotfix directo con commit + nota en INDEX.md.

**Recomendación: Opción B**. Razones:

- T1.5 tiene un eje semántico **distinto** a latch-mode-change (que es Sistema 1 = protección). Mezclarlos viola la regla #287 ("dual sistema latch vs Niagara native NO MEZCLAR").
- El bug está confirmado user-facing — merece prioridad sobre el SDD existente (latch-mode-change es feature, no bugfix).
- Un SDD separado deja **trazabilidad** del bug + decisión arquitectónica response-semantics como referencia futura para otros endpoints (alarmLatch, alarmUnlatch, alarmNotes — todos pueden tener el mismo bug, ver §74.3).
- La Opción C (hotfix sin SDD) **omite** el audit del mismo patrón en otros endpoints — riesgo de fix-de-uno-no-de-los-otros.

### Pregunta para el usuario

Antes de avanzar:

> ¿Querés que cree el SDD nuevo `mx60-alarms-ack-response-semantics` (Opción B), o preferís extender el existente (Opción A) o ir directo con hotfix (Opción C)?

---

## §74.3 — Posible falla idéntica en endpoints hermanos (defer, audit P1)

`alarmLatch`, `alarmUnlatch`, `alarmNotesPost` siguen el **mismo patrón** en `BChiServlet.java`:
- L1011-1080 `handleAlarmLatch` — retorna 200 con body JSON.
- L1082-1146 `handleAlarmUnlatch` — retorna 200 con body JSON.
- L1668+ `handleAlarmNotesPost` — patrón similar.

Y los XHR frontend correspondientes (UpDetail.js latch/reset, AlarmCards _doSaveNote) probablemente comparten el mismo "200 = success without body inspection". **No verificado en este bloque** — defer a audit T1.6 follow-up.

Si el patrón se confirma, T1.5 debería expandirse a "ack + latch + unlatch + notes response semantics — uniform contract". Pero el scope actual del SDD es solo ack — el patrón hermano queda flagged y se atendería en SDD posterior.

---

## §74.4 — Tally global post-Bloque 74

- 96 antipatterns AP-1..96 (sin cambios).
- 42 reglas template MX60 (sin cambios).
- **299 implications #1..#299** (+1: #299 clean-room-disconnected-asymmetry bidirectional).
- Capa 19 EXTENDIDA con audit empírico TIER-0 user-facing aplicado a veredicto previo TIER-1 estructural — **invariante metodológica nueva**: TIER-0 user-observation puede refutar TIER-1 wiring-trace cuando el wiring es estructuralmente correcto pero semánticamente roto.

---

## Cross-refs

- **Refuta**: bloque #72 §72.4 veredicto (A) parcialmente; el flow está cableado pero NO funcional end-to-end para el usuario.
- **Confirma**: bloque #72 §72.5 implication #289 ("`_postAck` duplicates AlarmsManager BUG"), pero ELEVA su severidad — la deduplicación NO es suficiente sin T1.5.
- **Re-confirma**: engram #1236 (mapping-vs-empirical-audit) a NIVEL veredicto-de-bloque + engram #1238 (clean-room-disconnected-asymmetry) bidireccional via #299.
- **Cita**: bloques #44 (auth flow), #54 (CSRF probe pattern), #62 (alarms tier-2 Reflow).

## Files audited

- `chihuahua-ux/src/rc/js/app/AlarmsManager.js` (327 LOC, focus L261-309)
- `chihuahua-ux/src/rc/js/app/AlarmCards.js` (369 LOC, focus L120-150 + L281-297)
- `chihuahua-ux/src/rc/js/app/AlarmModalActions.js` (240 LOC, focus L39-62)
- `chihuahua-ux/src/rc/js/app/AlarmsPage.js` (824 LOC, focus L502-612)
- `chihuahua-ux/src/rc/js/app/AlarmDetailPage.js` (481 LOC, focus L185-285)
- `chihuahua-ux/src/com/angeles/chihuahua/ux/BChiServlet.java` (focus L172-237 dispatch + L1158-1203 handler)
- `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiServletDispatch.java` (focus L321-385 POST routes)
- `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAlarmHelper.java` (focus L60-175 probe + L341-484 ackAlarms + L702-734 AckResult)
- `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAlarmQueryHelper.java` (focus L94-115 uuid population, L234-241 JSON serialization)
