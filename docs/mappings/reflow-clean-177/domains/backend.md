# Backend Domain — nmodsreflow (Java)

**Profile coverage**: rt (74) + ux (3) = 77 classes
**Module**: nmodsreflow
**Source**: `nmodsreflow/` (Java sources; BReflowScheme.java y las 3 clases ux decompiladas con CFR 0.152)

---

## 1. Overview

El backend de nmodsreflow es una capa de servicio Niagara N4 completamente contenida en el módulo `nmodsreflow`. Se monta en el stack Niagara como un único BComponent (`BReflowService`) que orquesta cuatro sub-sistemas: un front controller HTTP/REST (`BaseServlet`) que enruta 24 endpoints, un servidor WebSocket (`BReflowWebSocketAcceptor` + `BReflowChannelService`) para actualizaciones en tiempo real al browser, un motor de comandos BajaScript BOX para RPC tipado desde el frontend, y un servicio de sincronización de configuración (`BReflowSyncService`) con locking multi-usuario. La capa data-access vive en paquetes sueltos (`history/`, `alarms/`, `backups/`, `sync/`, `util/`) y es consumida exclusivamente por los handlers REST y los BOX commands — nunca expuesta directamente a Niagara Workbench. Las tres clases bajo `nmodsreflow-ux/` son widgets de Workbench (profile ux) que registran la vista Reflow y redirigen PX legacy al HTML5. El módulo no extiende ni sobrescribe servicios core de Niagara; opera como módulo de capa de aplicación sobre la plataforma Niagara 4.

---

## 2. Entry Points

Los archivos que bootstrapean o anclan el módulo en el runtime de Niagara:

| File | Role | LOC | Notes |
|------|------|-----|-------|
| `nmodsreflow/niagara-module.xml` | Descriptor del módulo — declara nombre, versión, dependencias Niagara y perfiles rt/ux | — | Raíz del módulo |
| `nmodsreflow/nmodsreflow-rt/module.palette` | Paleta de componentes rt — registra `BReflowService` como componente arrastrable en Workbench | — | Profile rt |
| `nmodsreflow/nmodsreflow-ux/module.palette` | Paleta ux — registra `BReflow`, `BReflowConfig`, `BReflowRedirect` | — | Profile ux |
| `nmodsreflow-rt/.../BReflowService.java` | BComponent raíz (26 slots) — bootstrapea todos los sub-servicios en `start()` | 468 | Punto de entrada rt |
| `nmodsreflow-rt/.../BReflowScheme.java` | BOrdScheme — registra el scheme `reflow:` en el ORD resolver de Niagara | 78 | Decompilado |
| `nmodsreflow-rt/.../http/BaseServlet.java` | Front controller HTTP — enruta 24 endpoints REST a handlers dedicados | 367 | HTTP anchor |
| `nmodsreflow-rt/.../http/sockets/SocketServlet.java` | Servlet WebSocket — gestiona el upgrade de HTTP a WebSocket y delega a `BReflowWebSocketAcceptor` | 54 | WS anchor |
| `nmodsreflow-ux/.../ux/BReflow.java` | BComponent UX — registra la vista Reflow en Niagara Workbench | 59 | Decompilado, profile ux |

---

## 3. Components / Classes

### 3.1 Service Container (1 class)

| Path (relativo a `nmodsreflow/nmodsreflow-rt/src/`) | bcomponent_type | slots | actions | LOC | decompiled |
|-----------------------------------------------------|-----------------|-------|---------|-----|------------|
| `com/niagaramods/nmodsreflow/BReflowService.java` | BComponent | 26 | refreshLicense, reloadLicenseFile, clearCache, clearHistoryCache, ticketExpired | 468 | false |

### 3.2 ORD Scheme (1 class)

| Path | bcomponent_type | slots | actions | LOC | decompiled |
|------|-----------------|-------|---------|-----|------------|
| `com/niagaramods/nmodsreflow/BReflowScheme.java` | BOrdScheme | — | — | 78 | true |

### 3.3 HTTP REST (28 classes)

Incluye el front controller, 18 response handlers, 3 utilidades HTTP y 5 clases en `http/sockets/` de WebSocket (agrupadas aquí por paquete; ver 3.4 para rol funcional).

#### 3.3.1 Front controller

| Path (relativo a `com/niagaramods/nmodsreflow/`) | bcomponent_type | rest_endpoints | LOC | decompiled |
|--------------------------------------------------|-----------------|---------------|-----|------------|
| `http/BaseServlet.java` | BComponent | GET /config, POST /config, GET /history/data, GET /history/list, GET /history/groups, GET /history/chart, GET /alarm/query, GET /alarm/csv, GET /schedule, GET /equipment/note, POST /equipment/note, GET /backup/list, POST /backup/create, POST /backup/apply, POST /backup/destroy, POST /backup/rename, POST /backup/reset, GET /file, GET /file/tree, GET /image/list, GET /image/library, GET /weather, GET /demo, GET /config/delta | 367 | false |

#### 3.3.2 Response handlers (18 classes)

Todos en `http/responses/`. Ninguno tiene `bcomponent_type`; son POJOs invocados por `BaseServlet`.

| Clase | rest_endpoint | LOC |
|-------|--------------|-----|
| `AlarmCSVResponse.java` | GET /nmodsreflow/alarm/csv | 38 |
| `AlarmQueryResponse.java` | GET /nmodsreflow/alarm/query | 44 |
| `BackupApplyResponse.java` | POST /nmodsreflow/backup/apply | 57 |
| `BackupCreateResponse.java` | POST /nmodsreflow/backup/create | 39 |
| `BackupDestroyResponse.java` | POST /nmodsreflow/backup/destroy | 38 |
| `BackupListResponse.java` | GET /nmodsreflow/backup/list | 43 |
| `BackupRenameResponse.java` | POST /nmodsreflow/backup/rename | 47 |
| `BackupResetResponse.java` | POST /nmodsreflow/backup/reset | 58 |
| `ConfigDeltaResponse.java` | GET /nmodsreflow/config/delta | 55 |
| `ConfigResponse.java` | GET /nmodsreflow/config | 118 |
| `ConfigUpdateResponse.java` | POST /nmodsreflow/config | 128 |
| `DemoResponse.java` | GET /nmodsreflow/demo | 48 |
| `EquipmentNoteResponse.java` | GET /nmodsreflow/equipment/note | 58 |
| `EquipmentNoteUpdateResponse.java` | POST /nmodsreflow/equipment/note | 87 |
| `FileResponse.java` | GET /nmodsreflow/file | 84 |
| `FileTreeResponse.java` | GET /nmodsreflow/file/tree | 66 |
| `HistoryChartDataResponse.java` | GET /nmodsreflow/history/chart | 74 |
| `HistoryDataResponse.java` | GET /nmodsreflow/history/data | 265 |
| `HistoryGroupsResponse.java` | GET /nmodsreflow/history/groups | 83 |
| `HistoryListResponse.java` | GET /nmodsreflow/history/list | 84 |
| `ImageLibraryResponse.java` | GET /nmodsreflow/image/library | 68 |
| `ImageListResponse.java` | GET /nmodsreflow/image/list | 64 |
| `SchedulesDataResponse.java` | GET /nmodsreflow/schedule | 33 |
| `WeatherMapResponse.java` | GET /nmodsreflow/weather | 125 |

#### 3.3.3 HTTP utilities (3 classes)

| Clase | Propósito | LOC |
|-------|-----------|-----|
| `http/util/CsrfGuard.java` | Validación de double-submit CSRF token en POST | 143 |
| `http/util/JsonBodies.java` | Lectura/escritura de cuerpos JSON en servlets | 86 |
| `http/util/Query.java` | Parseo de query params con conversión de tipos | 45 |

### 3.4 WebSocket (6 classes)

| Path (relativo a `com/niagaramods/nmodsreflow/http/sockets/`) | bcomponent_type | Propósito | LOC | decompiled |
|---------------------------------------------------------------|-----------------|-----------|-----|------------|
| `BReflowWebSocketAcceptor.java` | BComponent | Lifecycle manager WS — acepta conexiones, despacha IReflowCommand, sincroniza config | 505 | false |
| `BReflowChannelService.java` | BComponent | Pub/sub de canales — gestiona join/leave/broadcast para actualizaciones en tiempo real | 281 | false |
| `SocketServlet.java` | — | Servlet de upgrade HTTP→WS; delega al BReflowWebSocketAcceptor | 54 | false |
| `IReflowCommand.java` | — | Interfaz comando WS — define execute() para todos los processors de mensajes | 12 | false |
| `AsyncReflowCommand.java` | — | Wrapper async — difiere ejecución de IReflowCommand fuera del hilo WS | 34 | false |
| `ReflowWsHttpSessionListener.java` | — | HTTP session listener — limpia estado WS en expiración de sesión Niagara | 40 | false |

### 3.5 BajaScript BOX — comandos generales (5 classes)

Todos en `commands/`. Son BComponents registrados en la paleta; sus métodos son invocados desde el frontend via el plugin `api/box.js`.

| Clase | box_methods | LOC | decompiled |
|-------|------------|-----|------------|
| `BReflowBQLCommands.java` | query | 120 | false |
| `BReflowCSVCommands.java` | loadPointMap | 121 | false |
| `BReflowFileCommands.java` | listFiles | 149 | false |
| `BReflowNavCommands.java` | bformat, getNavChildren | 127 | false |
| `BReflowUserCommands.java` | getRoles, getAllRoles | 62 | false |

### 3.6 History (12 classes)

#### 3.6.1 BOX command

| Clase | box_methods | LOC |
|-------|------------|-----|
| `commands/BReflowHistoryCommands.java` | getList, getQuickList, getData, getGroupNames, getGroupTree, getDeviceTree, getDevices | 112 |

#### 3.6.2 Data layer

| Clase (bajo `history/`) | bcomponent_type | Propósito | LOC |
|-------------------------|-----------------|-----------|-----|
| `HistoryData.java` | — | Retrieval con Builder pattern y serialización Jackson para charts/tables | 663 |
| `HistoryGhostSubscriber.java` | — | Mantiene sesión de historia activa para evitar timeout en queries largas | 26 |
| `HistoryGroups.java` | — | Organización jerárquica de historias por folder/device | 112 |
| `HistoryIO.java` | — | I/O de records a/desde streams JSON | 103 |
| `HistoryList.java` | — | Enumeración y caching de historias disponibles en la station | 355 |

#### 3.6.3 JSON serializers (6 classes bajo `history/json/`)

| Clase | Propósito | LOC |
|-------|-----------|-----|
| `HistoryDeviceSerializer.java` | Serializa nodos device del árbol de historias | 65 |
| `HistoryFolderSerializer.java` | Serializa nodos folder del árbol de historias | 55 |
| `HistoryObjectMapper.java` | Configura Jackson ObjectMapper con todos los serializers de historia | 20 |
| `HistoryRecordOptions.java` | Value object: formato, timezone, precisión de serialización | 29 |
| `HistoryRecordSerializer.java` | Serializa BHistoryRecord a JSON con timestamp y valor | 122 |
| `IHistorySeralizer.java` | Interfaz contrato para todos los serializers de historia | 80 |

### 3.7 Alarms (6 classes)

#### 3.7.1 BOX command (en `commands/`, domain alarms)

| Clase | box_methods | LOC |
|-------|------------|-----|
| `commands/BReflowAlarmCommands.java` | getClasses, query, querySources, getActiveAlarmCounts, getUnackedAlarmCounts, getAlarmsSinceTime, canAcknowledgeAlarms | 113 |

#### 3.7.2 Data layer (en `alarms/`)

| Clase | bcomponent_type | Propósito | LOC |
|-------|-----------------|-----------|-----|
| `AlarmData.java` | — | Querying/filtering via BQL, export CSV y lookup por UUID | 439 |
| `AlarmSourceCollection.java` | — | Container de referencias a múltiples alarm sources para aggregation | 83 |
| `AlarmUuidArgs.java` | — | Value object: UUID arguments para ack/query BOX calls | 74 |
| `QueryFilter.java` | — | Encapsula parámetros de filtro (rango, severidad, source) para BQL queries | 158 |
| `ReflowAlarmSource.java` | — | Wrapper de ORD reference a un BIAlarmSource de Niagara | 25 |

### 3.8 Sync/Config (6 classes)

| Clase (bajo `sync/`) | bcomponent_type | Propósito | LOC |
|----------------------|-----------------|-----------|-----|
| `BReflowSyncService.java` | BComponent | Config sync service — locking multi-usuario, grantConfigControl, broadcast WS | 599 |
| `ConfigIO.java` | — | Read/write JSON de config con cache — persiste el JSON de 66KB en station filesystem | 252 |
| `ReflowSyncResponse.java` | — | Value object de respuesta sync — status, version token, secciones cambiadas | 26 |
| `ReflowSyncResponseSerializer.java` | — | Jackson serializer para ReflowSyncResponse para broadcast WS | 42 |
| `sync/commands/ReflowOrdTreeFavoritesRead.java` | — | Sync command: lee favoritos ORD-tree del config store | 71 |
| `sync/commands/ReflowOrdTreeFavoritesWrite.java` | — | Sync command: escribe favoritos ORD-tree en config store | 46 |

### 3.9 Backups (1 class)

| Clase | bcomponent_type | Propósito | LOC |
|-------|-----------------|-----------|-----|
| `backups/BackupManager.java` | — | CRUD de backups de station: create, list, apply, rename, destroy, reset | 248 |

### 3.10 Util (8 classes)

| Clase (bajo `util/`) | bcomponent_type | Propósito | LOC |
|----------------------|-----------------|-----------|-----|
| `BDateRangeEnum.java` | BComponent | Enum Niagara de presets de rango de fechas (Today, LastWeek, etc.) | 122 |
| `CommandHelpers.java` | — | Static helpers para BOX commands: parsing, auth checks, error formatting | 23 |
| `CompareRangeCalculator.java` | — | Computa rango periodo-anterior para charts de comparación | 316 |
| `Json.java` | — | Façade de Jackson ObjectMapper con métodos parse/stringify seguros | 97 |
| `NavNodeSerializer.java` | — | Serializa BNavNode de Niagara a JSON para BReflowNavCommands | 58 |
| `PointHelper.java` | — | Resuelve ORDs de puntos Niagara y lee valores para widgets | 50 |
| `RangeCalculator.java` | — | Traduce presets de rango a pares AbsTime start/end para queries de historia | 300 |
| `StringUtils.java` | — | Helpers de string null-safe, trimming y encoding | 15 |

### 3.11 UX Widgets (3 classes — profile ux)

Todas en `nmodsreflow-ux/src/com/niagaramods/nmodsreflow/ux/`. Todas decompiladas.

| Clase | bcomponent_type | Propósito | LOC | decompiled |
|-------|-----------------|-----------|-----|------------|
| `BReflow.java` | BComponent | Entry point UX — registra la vista Reflow en Niagara Workbench | 59 | true |
| `BReflowConfig.java` | BComponent | Panel de configuración en Workbench — expone propiedades editables a editores | 59 | true |
| `BReflowRedirect.java` | BComponent | Redirige PX views legacy al URL HTML5 de Reflow | 59 | true |

---

## 4. Cross-references

- **BReflowService** spawns `BReflowSyncService`, `BReflowChannelService` y `BReflowWebSocketAcceptor` en `start()`. Actúa como padre BComponent de todos los sub-servicios.
- **Alarms** — `BReflowAlarmCommands` (BOX) y `AlarmCSVResponse`/`AlarmQueryResponse` (REST) consumen `AlarmData` + `QueryFilter`. El frontend los invoca desde `reflow-frontend/src/api/alarms.js` y el plugin `api/box.js`.
- **History** — `BReflowHistoryCommands` (BOX) y los cuatro response handlers de historia consumen `HistoryData`, `HistoryList`, `HistoryGroups`. El frontend los invoca desde `reflow-frontend/src/api/history.js`.
- **BOX commands (generales)** — `BReflowBQLCommands`, `BReflowCSVCommands`, `BReflowFileCommands`, `BReflowNavCommands`, `BReflowUserCommands` son invocados exclusivamente desde el plugin frontend `reflow-frontend/src/plugins/$niagara.js` via el módulo `api/box.js`.
- **Config sync** — `BReflowSyncService.grantConfigControl()` es llamado desde el WebSocket acceptor cuando un cliente toma control de edición de config. El broadcast post-save llega al frontend via `BReflowChannelService` → WebSocket → Vuex store `state/configSync`.
- **BackupManager** — consumido por los 6 response handlers de backup en `http/responses/Backup*.java`. El frontend invoca estos endpoints desde `reflow-frontend/src/api/backups.js`.
- **Util** — `RangeCalculator` y `CompareRangeCalculator` son consumidos por `HistoryData` y los handlers de historia. `PointHelper` es consumido por handlers de equipment. `NavNodeSerializer` es exclusivo de `BReflowNavCommands`.
- **UX widgets** — `BReflow`, `BReflowConfig`, `BReflowRedirect` no dependen de clases rt en tiempo de compilación; la comunicación Workbench→station ocurre via ORD (scheme `reflow:`).
- **BReflowScheme** — resuelve el scheme `reflow:` en el ORD resolver de Niagara; usado cuando Workbench o una regla PRE intenta navegar a un recurso Reflow.

---

## 5. Notes & Gotchas

- **Clases decompiladas** — `BReflowScheme.java` (rt) y las 3 clases ux (`BReflow`, `BReflowConfig`, `BReflowRedirect`) fueron decompiladas con CFR 0.152. Las jerarquías de clase y los tipos de slots se infieren del bytecode; pueden diferir de la fuente original si el compilador introdujo synthetic fields o inner classes anónimas. Los LOC de estas 4 clases (78 + 59×3) son LOC del output de CFR, no del original.
- **javax.baja.* stubs ausentes** — el fuente descompilado referencia `javax.baja.alarm.BAlarmDatabase`, `javax.baja.control.BControlPoint`, `com.tridium.niagara.scheme.BOrdScheme` y otros tipos del SDK de Niagara N4, que no están en el repositorio. El análisis de dependencias se basa en imports del source; no se ejecutó compilación ni análisis estático completo.
- **BReflowLicenseCommands** — referenciado en `REFLOW-ARCHITECTURE-ANALYSIS.md` (sección "BOX Command Classes") pero no presente en el árbol de fuentes. Posiblemente fue eliminado antes del snapshot o forma parte de un módulo de licenciamiento separado. Su ausencia no afecta los 77 contados en este mapping.
- **BReflowAlarmCommands** vive en `commands/` pero su dominio en index.json es `alarms` (no `bajascript-box`). Esto es correcto por cohesión funcional; el sub-dominio 3.7 lo documenta en su contexto de alarms.
- **nmodsreflow-ux/build/** — artefactos de build (`.jar`, `.class`, bajadoc) excluidos del mapping per REQ-10. Sólo los 3 fuentes Java decompilados en `nmodsreflow-ux/src/` están incluidos.
- **HistoryData.java** (663 LOC) y `BReflowSyncService.java` (599 LOC) son las dos clases más grandes del módulo y concentran lógica de negocio crítica. Son candidatas prioritarias para refactor/test en el MX60.
- **WeatherMapResponse.java** (125 LOC) hace un proxy a una API externa de clima; introduce una dependencia de red externa sin circuit breaker documentado en el fuente analizado.
- **`IHistorySeralizer.java`** contiene un typo en el nombre (Serializer → Seralizer). Es el nombre real del archivo; no es un error de este mapping.
- **Dominio `websocket`** tiene 6 clases en `http/sockets/` pero el paquete Java las mezcla con las 3 utilidades HTTP en `http/util/` y el BaseServlet. El agrupamiento por dominio refleja función, no paquete.
