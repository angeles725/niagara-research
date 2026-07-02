# Block 167 — chihuahua MX60 (`-ux/-rt`): audit trail (ring auditLog, fire-and-forget, merge con SecurityHistory)

> **WHAT** — Documenta el subsistema de **audit trail** del módulo Niagara **chihuahua** (SDD `mx60-rbac-and-audit-trail`, gap C5): la forma del registro de auditoría, la semántica del ring buffer persistente `auditLog` (retención ~500), el patrón *fire-and-forget* (un fallo de auditoría nunca tumba el write), la fusión de acciones propias del módulo con eventos de login nativos leídos de `SecurityHistory`, el `parseAndFilter` del endpoint `GET /api/audit`, y el estado del TODO de login-history.
> **Focus:** **chihuahua** (Cliente/Honeywell/MX60). Fuente de primera parte (código real del módulo).
> **Fuentes (aliases):**
> - `UX` = `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiAuditHelper.java`
> - `UX-SRV` = `chihuahua-ux/src/com/angeles/chihuahua/ux/BChiServlet.java`
> - `RT` = `chihuahua-rt/src/com/angeles/chihuahua/components/BChiDashboardService.java`
> **Marcadores:** `[CERT] file:line` = verificado en código de primera parte; `[INFER]` = inferencia razonada, no probada en estación.
> **.env.local NO leído** (fuera de alcance).
> **Capa 26.** Continúa [Block 163] / [Block 164].

---

## 167.1 — Qué es y por qué es un NET-ADD frente a Reflow

El audit trail es una capacidad que **Reflow (B138–B150) NO tiene**: Reflow no registra ninguna traza de auditoría de escrituras ni de accesos. chihuahua la agrega como parte del SDD `mx60-rbac-and-audit-trail`, con dos piezas de almacenamiento/lectura:

- **Almacén (RT):** un slot `String` persistente `auditLog` en `BChiDashboardService`, tratado como **ring buffer de líneas-JSON** con retención ~500 entradas. `[CERT] RT:68-76` (declaración `@NiagaraProperty auditLog`), `[CERT] RT:148-172` (región slot generada).
- **Lectura/fusión (UX):** `ChiAuditHelper` construye/parsea entradas propias del módulo y, en el endpoint `GET /api/audit`, las **fusiona** con eventos de login/logout nativos leídos de la historia `SecurityHistory`. `[CERT] UX:19-46` (Javadoc de rol), `[CERT] UX-SRV:1095-1099` (comentario del handler).

Es una **capability net-add**: no existe contraparte en Reflow (ver §167.x — Connections).

---

## 167.2 — Forma del registro de acción (record shape)

Cada acción del módulo se serializa como **una línea JSON compacta** (JSON-lines, sin saltos de línea internos). Orden fijo de campos: `ts, user, action, ord, oldValue, newValue`. `[CERT] UX:122-149` (`buildEntry`).

```
{"ts":<epochMs>,"user":"...","action":"...","ord":"...","oldValue":"...","newValue":"..."}
```

`[CERT] RT:71` y `[CERT] RT:152` (shape documentado en el slot).

Detalles certificados:
- `ts` es epoch-millis (número JSON crudo). `[CERT] UX:126`.
- `oldValue` / `newValue` son opcionales: si son `null` en Java se serializan como `null` JSON (no como `"null"`). `[CERT] UX:131-146`.
- Todos los valores string pasan por `ChiJsonUtil.escapeJson`, que cubre el set completo de control JSON (`" \ \n \r \t \b \f`, control chars, U+2028, U+2029). Esto previene **entry-split y forgery** (que un valor con `\n` inyecte una línea falsa en el ring). `[CERT] UX:110-113`, `[CERT] UX:127-129`.
- La cadena resultante **no contiene saltos de línea**, por lo que es segura para append al ring newline-delimited. `[CERT] UX:108-109`, `[CERT] UX:120`.

`action` es una etiqueta corta del tipo de operación auditada (p. ej. `"setpoint"`, `"alarmAckAll"`). `[CERT] UX:117`.

---

## 167.3 — Ring buffer: semántica de retención (~500) y persistencia

El almacén vive en `BChiDashboardService`:

- Slot `auditLog` de tipo `baja:String`, **PERSISTENTE** (sin `TRANSIENT`) → las entradas sobreviven el reinicio de la estación (van al `.bog`). `[CERT] RT:69-70`, `[CERT] RT:156`.
- Cap de retención: `MAX_AUDIT_ENTRIES = 500`. `[CERT] RT:641`.
- **`appendAudit(jsonLine)`** — añade una entrada al final, separada por `\n`; entradas null/vacías son no-op; tras el append recorta el ring a 500. `[CERT] RT:653-667`. Null/empty ignorado en `[CERT] RT:655`.
- **`getAuditRaw()`** — devuelve el string crudo newline-delimited (o `""` si nunca se escribió). `[CERT] RT:677-681`.
- **`_trimAuditRing(log, maxEntries)`** — cuenta líneas no-vacías; si excede el cap, **descarta desde la cabeza** (las entradas más viejas primero, FIFO). `[CERT] RT:693-725`; conteo y "drop from head" en `[CERT] RT:707-724`.

**Thread-safety:** `appendAudit` y `getAuditRaw` son `synchronized` sobre `this` (mismo monitor que `getThemeForUser`/`setThemeForUser`), para que escrituras concurrentes no intercalen datos parciales de línea en el slot. `[CERT] RT:653` (`synchronized void appendAudit`), `[CERT] RT:677` (`synchronized String getAuditRaw`), `[CERT] RT:632-634` (nota de contrato).

**Orden en el ring:** inserción = más-nuevo-al-final; `parseAndFilter` preserva ese orden (newest-last). `[CERT] UX:172`.

**Frontera de módulo (R-11 / ADR-D1):** `chihuahua-rt` NO importa `chihuahua-ux`. Los helpers del ring son puras operaciones String/slot — sin librería JSON, sin `ChiAuditHelper`. `[CERT] RT:636-637`.

---

## 167.4 — Fire-and-forget: un fallo de auditoría nunca tumba el write

Tras **cada escritura exitosa** con permiso concedido (gated write), el servlet añade un record de auditoría en un bloque try/catch aislado cuyo fallo se **loguea a WARNING y se ignora** — la respuesta del write ya se emitió y NO se ve afectada. Patrón en la ruta de setpoint:

- El write y la respuesta `{"ok":true}` se emiten primero. `[CERT] UX-SRV:792-799`.
- Luego, `T2.3`: se resuelve el `BChiDashboardService` y se llama `appendAudit(ChiAuditHelper.buildEntry(remoteUser, "setpoint", ord, oldValStr, value, now))`. `[CERT] UX-SRV:801-811`.
- Cualquier excepción de auditoría cae en `catch (Exception auditEx)` → `LOG.warning("... audit write failed (ignored): ...")`, sin re-lanzar. `[CERT] UX-SRV:802` (comentario: "failure to audit MUST NOT fail the write response"), `[CERT] UX-SRV:813-816`.

Este mismo patrón fire-and-forget se repite en **todas** las rutas de escritura gated del servlet (cada write auditado es también un write con RBAC — ver [Block 164]):
- `handleAlarmLatch` — `[CERT] UX-SRV:1371-1384`.
- `handleAlarmUnlatch` — `[CERT] UX-SRV:1458-1471`.
- `handleUpThresholdSet` — `[CERT] UX-SRV:1577-1590`.
- `handleCarcamoThresholdSet` — `[CERT] UX-SRV:1692-1705`.
- `handleDataloggerThresholdSet` — `[CERT] UX-SRV:1807-1820`.
- `handleAlarmNotesPost` (usa `action="alarmNotesPost"`, ord = uuid único o `"multi(N)"`) — `[CERT] UX-SRV:2035-2050`, `[CERT] UX-SRV:2041`.
- `handleAlarmAckAll` — `[CERT] UX-SRV:2107-2122`.

**Consecuencia de diseño [INFER]:** la auditoría es *best-effort*, no transaccional. Un write puede completarse y quedar sin registro si `appendAudit` falla (p. ej. servicio no resuelto), pero jamás se corrompe la operación de control por un fallo de la traza — la seguridad operativa prima sobre la completitud del log.

---

## 167.5 — `parseAndFilter`: lectura y filtrado de acciones propias

`ChiAuditHelper.parseAndFilter(raw, userFilter, rangeFilter, actionFilter)` transforma el string crudo del ring en una lista de mapas `String→String` filtrada. `[CERT] UX:174-235`.

- `raw` null/vacío → lista vacía. `[CERT] UX:180`.
- Parte por `\n`; cada línea no-vacía se pasa a `_parseEntry`; entradas malformadas se **saltan en silencio** (no abortan el barrido). `[CERT] UX:192-199`.
- **Filtro `user`:** substring **case-insensitive** sobre el campo `user`. `[CERT] UX:202-206`.
- **Filtro `action`:** substring case-insensitive sobre el campo `action`. `[CERT] UX:208-212`.
- **Filtro `range` (REQ-7):** convierte el vocabulario de rango a `[startMs, endMs]` una sola vez vía `_computeRangeMs`, luego descarta entradas fuera de ventana; un `ts` no parseable descarta la entrada por seguridad. `[CERT] UX:182-190`, `[CERT] UX:214-230`.
- Vocabulario de rango (`lastHour`, `last8Hours`, `last24Hours`/`lastDay`, `last7Days`, `last30Days`; desconocido → default 24 h). `[CERT] UX:675-708`.

`_parseEntry` es un escáner plano ligero (no un parser JSON completo): decodifica escapes JSON a los caracteres verdaderos, de modo que el mapa contiene los valores originales sin escapar. Una sola capa de escape: `buildEntry` escapa al escribir, `_parseEntry` desescapa al leer — sin doble-escape. `[CERT] UX:501-517`, `[CERT] UX:519-627`. La reserialización de salida (`_entryToJson`) vuelve a escapar exactamente una vez. `[CERT] UX:639-660`.

Métodos puros WSL-testables (`buildEntry`, `parseAndFilter`, `_parseEntry`, `_computeRangeMs`) sin estación Niagara. `[CERT] UX:35-36`.

---

## 167.6 — Fusión con eventos de login nativos (SecurityHistory)

`GET /api/audit` devuelve un objeto **fusionado** con dos arrays: acciones propias del módulo + logins nativos. `[CERT] UX:244-247`, `[CERT] UX:260-277` (`writeAuditJson`, forma `{"actions":[...],"logins":[...]}`).

`ChiAuditHelper.readLoginEvents(context, rangeStr)` lee los eventos de login/logout nativos de la estación usando **solo `javax.baja.history.*`** (mismo patrón cursor que `ChiHistoryHelper`, sin import directo de `com.tridium.*` — constraint R-11 / ADR-D1). `[CERT] UX:279-287`, `[CERT] UX:310-431`.

Flujo certificado:
- Abre el `BHistoryDatabase` vía el ORD canónico `history:`. `[CERT] UX:319`.
- Resuelve el `BHistoryId` como `(stationName, LOGIN_HISTORY_NAME)`, con `stationName` en runtime vía `Sys.getStation().getStationName()` (sin hardcode). `[CERT] UX:325-326`.
- Login/logout viven en `/<station>/SecurityHistory` (Niagara los desvía ahí vía `BSecurityAuditHistorySource`; **no** están en `AuditHistory`). `[CERT] UX:74-81`, `[CERT] UX:322-324`.
- Rango **abierto** (`null, null`) cuando no se selecciona rango → devuelve TODOS los logins retenidos (igual que el visor de historia de Workbench). Esto corrige un bug donde una ventana acotada devolvía cursor vacío entre logins esporádicos, dejando la pestaña ACCESOS intermitentemente vacía. `[CERT] UX:336-356`.
- Itera con `TableCursor`; de cada `BHistoryRecord` lee por nombre de slot `userName` / `operation` / `message` (helper `_recSlot` vía `BComplex.getProperty/getString`, sin `com.tridium.*`). `[CERT] UX:360-389`, `[CERT] UX:488-499`.
- `userName` llega SlotPath-escapado (p. ej. `Roberto$20Perez`) y se desescapa vía `ChiRbacHelper.unescapeUsername`. `[CERT] UX:381-384`.
- Entrada de login resultante: mapa con `ts`, `user`, `eventType`, `message` (esquema distinto al de acciones, que usan `action`). `[CERT] UX:398-402`, `[CERT] UX:441-444`.

**Exclusión VIEW-ONLY de cuentas de servicio:** los usernames en `HIDDEN_ACCESS_USERS` (actualmente `"api"`, case-insensitive + trim) se **saltan** para que no lleguen al dashboard ACCESOS; la `SecurityHistory` nativa queda intacta (solo se oculta ruido de una cuenta no-humana). `[CERT] UX:83-96`, `[CERT] UX:391-396`.

**Degradación graciosa:** si la historia no se encuentra o cualquier excepción ocurre durante la iteración, se devuelve **lista vacía sin lanzar** (WARNING logueado). `[CERT] UX:296-301`, `[CERT] UX:328-334`, `[CERT] UX:417-422`.

Filtro de usuario para logins: `filterLoginsByUser` (substring case-insensitive sobre `user`; el filtro `action` NO aplica a logins porque su esquema usa `eventType`). `[CERT] UX:437-444`, `[CERT] UX:455-475`.

---

## 167.7 — El handler `GET /api/audit` y su ruta

Dispatch de ruta: `RouteAction.Audit` → `handleAudit(user, range, action, req, resp)`. `[CERT] UX-SRV:344-349`.

`handleAudit` (`[CERT] UX-SRV:1122-1167`):
1. **Auth safety-net:** `req.getRemoteUser()` null/vacío → 401 (el contenedor Niagara normalmente ya autentica). `[CERT] UX-SRV:1130-1138`.
2. **(a) Acciones:** resuelve el service, `getAuditRaw()`, y `parseAndFilter(raw, user, range, action)`. `[CERT] UX-SRV:1140-1148`.
3. **(b) Logins:** `readLoginEvents(this, range)` y luego `filterLoginsByUser(..., user)`. `[CERT] UX-SRV:1150-1153`.
4. **Merge y emisión:** `writeAuditJson(actions, logins, out)`, status 200, log INFO con conteos. `[CERT] UX-SRV:1155-1158`.
5. Cualquier excepción → 500 con mensaje escapado. `[CERT] UX-SRV:1160-1165`.

Consistente con el resto del servlet: la lectura es de solo-lectura pero exige autenticación. Los filtros son todos opcionales (null = sin filtro).

---

## 167.8 — Estado del TODO de login-history (matiz importante)

El prompt lo describe como "TODO no resuelto", pero el código muestra que está **resuelto en parte**:

- El identificador ya está fijado: `LOGIN_HISTORY_NAME = "SecurityHistory"`, con Javadoc explicando que login/logout se desvían ahí vía `BSecurityAuditHistorySource` y que el device es el `stationName` en runtime. `[CERT] UX:74-81`.
- `readLoginEvents` está **completamente implementado** (no es un skeleton que devuelve lista vacía). `[CERT] UX:310-431`.

Lo que **queda pendiente** es una verificación de "station smoke": confirmar en la estación real (Workbench → Config > Platform Services > Logger Service, o `ChiHistoryHelper.listHistories()`) que el nombre de historia efectivo es el esperado; hasta confirmarlo, la ruta de login degrada a lista vacía sin fallar. Los comentarios TODO residuales que reflejan esa incertidumbre histórica siguen en `[CERT] UX:53-63` (bloque TODO con candidatos `AuditHistory`/`AuditTrail`/`LogHistory`) y `[CERT] UX:303` (Javadoc "TODO (station smoke): verify LOGIN_HISTORY_NAME"). El Javadoc de la clase también lo cataloga como "open question" de diseño D4. `[CERT] UX:38-43`.

**Resumen [INFER]:** el TODO ya no es un bloqueo funcional (el código lee `SecurityHistory` correctamente por diseño Niagara documentado en el propio Javadoc); es una tarea de verificación en sitio. Un lector que solo mire los comentarios TODO viejos (`AuditHistory`/`AuditTrail`/`LogHistory`) podría creer que el nombre sigue sin decidir — no es así: la constante ya apunta a `SecurityHistory`.

---

## 167.x — Connections

- **[Block 164] (RBAC / gated writes):** el audit trail es la contraparte de la capa RBAC. **Cada write gated que pasa el guardado RBAC es también auditado** con el mismo patrón fire-and-forget en el bloque `T2.3` posterior a la respuesta. Las 7 rutas de escritura citadas en §167.4 son exactamente las rutas con guard RBAC (`REQ-3` / ADR D2 en `[CERT] UX-SRV:678`, `1320`, `1408`, `1525`, `1640`, `1755`, `2012`, `2078`). El SDD que introduce ambas capas es `mx60-rbac-and-audit-trail` (RBAC = REQ-3; audit = REQ-6/REQ-7).
- **[Block 163]:** contexto del subsistema chihuahua MX60 (dashboard service, servlet, jerarquía Planta/Monitor/UP) sobre el que se apoyan el slot `auditLog` (en `BChiDashboardService`) y el endpoint (en `BChiServlet`).
- **Reflow (B138–B150) — sin equivalente:** Reflow **NO tiene** ningún audit trail (ni ring de acciones, ni merge con logins nativos, ni endpoint `/api/audit`). Este bloque documenta por tanto una **capability net-add** de chihuahua frente a Reflow; sirve como contraparte de comparación (chihuahua registra quién cambió qué setpoint/umbral/alarma y cuándo; Reflow no deja traza).
