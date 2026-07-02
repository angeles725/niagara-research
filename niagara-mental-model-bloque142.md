# Block 142 — nmodsreflow.77 (`-rt`): subsistema alarms (query read-only, doPrivileged ancho, BQL injection vía uuid)

> Research de **NiagaraMods Reflow v1.7.7 (build .75), paquete `alarms/` del runtime `-rt`**: cómo Reflow
> consulta y exporta alarmas de Niagara. Cubre `ReflowAlarmSource`, `AlarmData` (motor real), `AlarmSourceCollection`,
> `AlarmUuidArgs`, y la capa HTTP `AlarmQueryResponse` (POST) / `AlarmCSVResponse` (GET/CSV), más el
> agente `BReflowAlarmCommands`. NO cubre el `QueryFilter`/`Query` (taint source de HTTP, `http/util/` —
> queda como sub-gap) ni el contrato JSON frontend completo (R12).
>
> Focus: **nmodsreflow** (arquitectura backend `-rt`). Cierra el gap **R6**. Corpus language: Spanish
> (technical EN).
>
> Sources (primarias, JAR embarcado build .75, decompile Vineflower):
> `RT/` = `/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/nmodsreflow77-rt/vineflower/com/niagaramods/nmodsreflow`
> `ALM/` = `RT/alarms`. Capa HTTP: `RT/http/responses/`. Comandos: `RT/commands`.
>
> Método: decompile Vineflower del JAR embarcado + lectura directa + grep de tokens/callers. Markers:
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción anclada a líneas `[CERT]`.
> Nota de decompilado: Vineflower dejó ofuscados algunos nombres (`method_291`=`get(String)` en Jackson,
> `method_363`=parse de query en `http/util/Query`, `field_282`=`end` en `AlarmUuidArgs`); se citan tal cual.
>
> Capa 26 (OEM tercero NiagaraMods). Connects [Block 138] (service central + espina HTTP), [Block 140]
> (mismo patrón `doPrivileged` en el dispatch WS), [Block 141] (mismo patrón `doPrivileged` + BQL-por-concat
> en history; refuerza la nota de seguridad cross-focus), [Block 75]/[Block 113] (skipModuleValidation /
> code-signing), [Block 139] (licensing bypass — parte de la superficie agregada).

---

## 142.1 — Mapa del subsistema y hallazgo central `[CERT]`

El subsistema alarms de Reflow es **read/report-only**: no genera alarmas, no acknowledgea, no muta estado.
No hay ninguna llamada `BAlarmRecord.ackAlarm` ni transición de estado en `ALM/`.

| Clase | Rol | Estado | Cita |
|---|---|---|---|
| `ReflowAlarmSource` | POJO contador (tallies + `lastRecord`) — **NO** es BComponent/BAlarmSource/recipient | plain class | `ALM/ReflowAlarmSource.java:5,8` |
| `AlarmData` | Motor real: BQL query, doPrivileged, CSV stream, filtros | static-util | `ALM/AlarmData.java:82,122,142` |
| `AlarmSourceCollection` | Agregación en memoria por source (HashMap) — **sin DB propia** | clase con estado | `ALM/AlarmSourceCollection.java:13,15` |
| `AlarmUuidArgs` | Holder de ventana+filtro (start/end/range/sources) — **NO** BSimple/BStruct | plain class | `ALM/AlarmUuidArgs.java:11,17` |
| `AlarmQueryResponse` | Endpoint **POST** (parse JSON body → QueryFilter → query) | Response | `RT/http/responses/AlarmQueryResponse.java:31,35,38` |
| `AlarmCSVResponse` | Export CSV (GET, streaming) | Response | `RT/http/responses/AlarmCSVResponse.java:20-22` |

**Hallazgo central `[CERT]` `ALM/AlarmData.java:82`:** `getAlarmByUuid` construye BQL por concatenación
**sin escapar** la comilla simple:

```
BOrd.make("station:|alarm:|bql:select * where uuid = '" + uuid + "'").get(null)
```

El `uuid` llega crudo del arg del comando (`BReflowAlarmCommands.getAlarmByUuid`, `:46-49`), y ese comando
requiere **sólo permiso de lectura** (§142.5). `[INFER]` Una comilla simple en `uuid` rompe/inyecta la
cláusula BQL → **BQL injection alcanzable a read-permission**. Es el hallazgo de mayor señal del subsistema.

## 142.2 — AlarmData: motor de query bajo doPrivileged ancho `[CERT]`

`AlarmData.query()` envuelve **todo** el task de query en `AccessController.doPrivileged`, spawneando un
`Thread` crudo que `start`+`join` `[CERT]` `AlarmData.java:122-126`:

```
AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(new PrivilegedExceptionAction() {
   public run() {
      Thread thread = new Thread(task, "BReflowAlarmData.QueryCommand.Task");
      thread.start(); thread.join();
```

`[INFER]` **Mismo patrón ancho que B140 (dispatch WS) y B141 (history):** el bloque privilegiado corre BQL
construido desde el `QueryFilter` HTTP (input atacante-influenciado) con privilegio elevado. Sólo `query()`
está envuelto; `querySources`/`streamAlarmsCSV`/`streamSourcesCSV`/`getUuidsForSource` corren sin
`doPrivileged` `[CERT]` (grep negativo). Thread crudo sin pool, uno descartable por request `[CERT]` `:124`.

**Paginación** page-based aguas abajo: `limit=15`, `skip=(page-1)*15`, gated por `countOnly` `[CERT]`
`AlarmData.java:487`. **Shape JSON** `{ "total": N, "records": [...] }` (records omitido si `countOnly`)
`[CERT]` `AlarmData.java:202-203`. **Falla silenciosa:** el `catch` de `query()` devuelve `{}` vacío ante
CUALQUIER excepción — errores invisibles al caller `[CERT]` `AlarmData.java:132`.

**BQL adicional por concat:** `getAlarmsSinceTime` concatena `timestamp` (long, riesgo bajo) `[CERT]` `:70`;
`buildBQLQuery` concatena `getMillis()` (numérico) `[CERT]` `:376`. Sólo el `uuid` (§142.1) es string
atacante sin escapar.

## 142.3 — La única ruta que usa la API real de alarm DB `[CERT]`

`getUuidsForSource` es el **único** método que toca `AlarmDbConnection`: resuelve `BAlarmService`, abre
`getAlarmDb().getDbConnection(null)` y corre `timeQuery(start, end)` con cursor y cierre try-with-resources
`[CERT]` `AlarmData.java:247,250,254`. Todas las demás rutas de lectura van por `BOrd.make("station:|alarm:|bql:...")`
en vez de la conexión tipada `[CERT]` `:70,82,376`. `[INFER]` No se usa `AlarmRecordFilter`/`QueryFilter` de
Baja: el filtrado se hace en Java post-cursor (helpers `testXxx`), no se empuja a la DB — más datos cruzan a
memoria de los necesarios.

## 142.4 — AlarmSourceCollection, AlarmUuidArgs, ReflowAlarmSource `[CERT]`

`AlarmSourceCollection` bucketea `BAlarmRecord` por `alarm.getSource().encodeToString()` en un `HashMap`
`[CERT]` `AlarmSourceCollection.java:15,27`; NO abre conexión ni query — recibe records ya traídos por el
cursor BQL de `AlarmData`. Ordena por timestamp DESC vía comparator `[CERT]` `:50-51`. `[INFER]` NPE latente:
el comparator y `toString()` (`:79`) dereferencian `lastRecord.getTimestamp()`/`lastRecord.toString()` sin
guard (en la práctica siempre seteado en el primer record por source).

`ReflowAlarmSource` es un POJO contador: sólo tallies + un `BAlarmRecord lastRecord` copiado defensivamente
vía `newCopy()` `[CERT]` `ReflowAlarmSource.java:5,8,15`.

`AlarmUuidArgs` es un holder plano (no BSimple/BStruct) construido desde un `BComponent args`; lleva la
ventana de query + filtro de sources (split por coma), no los UUIDs `[CERT]` `AlarmUuidArgs.java:11,17`.
`[INFER]` Debug leftover: `System.out.println` de tiempos parseados (`:43-44`).

## 142.5 — Capa HTTP y autorización `[CERT]`

`AlarmQueryResponse` (POST) lee el body crudo, lo parsea con Jackson `ObjectMapper.readTree`, extrae el nodo
`"query"`, lo pasa a `Query.method_363(...)` → `QueryFilter.make(query)` y llama `AlarmData.query(...)`
`[CERT]` `AlarmQueryResponse.java:31,34-35,38`. `[INFER]` NPE: `jsonNode.method_291("query").toString()`
(`:34`) se dereferencia sin verificar que el nodo `query` exista.

`AlarmCSVResponse` (GET) parsea `req.getQueryString()` por el mismo `Query.method_363`→`QueryFilter.make` y
ramifica por `query.get("type")` == `"source"` `[CERT]` `AlarmCSVResponse.java:20-23`. `[INFER]` NPE si
`type` está ausente (`get` devuelve null y se llama `.equalsIgnoreCase`).

**Autorización — sin enforcement en este subsistema `[CERT]`:** `BReflowAlarmCommands` declara
`requiredPermissions = "r"` `[CERT]` `BReflowAlarmCommands.java:24` — toda la superficie (incluida la BQL con
`uuid` concatenado y la query bajo `doPrivileged`) es alcanzable con permiso de lectura simple.
`canAcknowledgeAlarms` sólo **reporta** un booleano (`cx.getUser().getPermissionsFor(alarmService).hasOperatorWrite()`),
no es un gate `[CERT]` `BReflowAlarmCommands.java:101`. El ack real ocurre en la consola de alarmas estándar
de Niagara, fuera de `nmodsreflow`.

## 142.6 — Bugs de correctitud `[CERT]`

- **String `==`/`!=` por referencia** en el filtro `active` de `AlarmData`: `state != "normal"` / `state == "normal"`
  `[CERT]` `AlarmData.java:340,343`; y `recordType == "alarm"` en CSV `[CERT]` `AlarmCSVResponse.java:28`.
  `[INFER]` Funciona sólo porque los literales están interned; bug latente si el string proviene de otra vía.
- **NPE en `getAlarmByUuid`**: el retorno de `cursor.next()` se ignora; si no hay fila, `cursor.get()` da null
  y `getAlarmRecord` NPEa `[CERT]` `AlarmData.java:82` (patrón confirmado en el método).
- **Falla silenciosa** (`return new JSONObject()` en catch) oculta errores `[CERT]` `AlarmData.java:132`.

## 142.7 — Cross-cutting vs. history (B141) `[CERT]`

| Patrón | history (B141) | alarms (B142) | Cita |
|---|---|---|---|
| `doPrivileged` ancho sobre input HTTP | sí | **sí** | `AlarmData.java:122` |
| BQL por concat | sí (`config.getId()`) | **sí, peor: `uuid` string sin escapar** | `AlarmData.java:82` |
| Thread crudo sin pool | sí | sí | `AlarmData.java:124` |
| Cache gzip en disco | sí (HistoryIO) | **ausente** — re-corre BQL en vivo | grep negativo |
| `SimpleDateFormat` estático no-safe | sí | **ausente** (usa `BAbsTime`/`BFormat`) | `AlarmData.java:293,432` |

## 142.8 — Connections

- **[Block 138]** — `AlarmData`/las Response cuelgan de la espina HTTP y del service central de Reflow.
- **[Block 140]** — mismo `AccessController.doPrivileged` ancho que el dispatch del canal WS. Ya son **tres**
  subsistemas (WS dispatch, history, alarms) elevando privilegio alrededor de trabajo alimentado por el cliente.
- **[Block 141]** — history: idéntico patrón doPrivileged + BQL-por-concat; alarms lo **agrava** con una
  inyección BQL concreta (`uuid` string) alcanzable a read-permission.
- **[Block 75]/[Block 113]** — `skipModuleValidation` / code-signing.
- **[Block 139]** — licensing bypass.

**Nota de seguridad cross-focus (REFORZADA desde B141 §141.9):** R6 aporta superficie privilegiada nueva y
la agrava. El cuadro agregado es ahora: (1) **tres** subsistemas de Reflow corren bloques `doPrivileged`
anchos sobre input del cliente (B140/B141/B142); (2) B142 añade una **BQL injection concreta** (`uuid` sin
escapar, `AlarmData.java:82`) alcanzable con **sólo permiso de lectura** (`requiredPermissions="r"`); (3) ese
privilegio elevado descansa en que el JAR OEM está firmado/validado por la plataforma, pero B75/B113 mostraron
que la validación de módulo puede desactivarse vía **`skipModuleValidation`**, y B139 documentó un **bypass**
en el licensing RSA. `[INFER]` La combinación —módulo con múltiples bloques privilegiados anchos + inyección
BQL a read-level + plataforma donde la validación de módulo puede apagarse + licensing con bypass— justifica
un **bloque de síntesis cross-focus** (nmodsreflow × platform-security) que evalúe la superficie de ataque
agregada. Queda anotada; no se resuelve en R6 (read-only, cruza focuses). Sub-gap adicional descubierto:
`http/util/Query` + `QueryFilter.make` (taint source de todo el filtrado HTTP) merecen su propio barrido.
