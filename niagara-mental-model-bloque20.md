# Niagara N4 — Bloque 20: BApp + net module + BAbstractService + Monitors + JobService + gap analysis

Parte del mental model. Ver [INDEX.md](INDEX.md). Relacionado con Bloques 4 (BComponent), 8 (services), 10 (station boot), 11 (auth/sessions), 14 (BBatchJobService), 16 (BAbstractService usage), 17 (system.properties), 18 (permissions).

Este bloque final cubre **residuales misceláneos** no profundizados en bloques previos: BApp/BWebApp application layer, net module, BAbstractService internals + lifecycle hooks + service ordering, Station/Engine/Lease/Resource Managers, BJobService base + BJob lifecycle + consumers, persistent policies per subsystem. Cierra con **gap analysis honesto** de lo que todavía queda sin cubrir después de 20 bloques.

---

## 20.1 BApp / BWebApp

### 20.1.1 Jerarquía — NO existe BAbstractApp

Hallazgo empírico: **BAbstractApp no existe**. Solo `javax.baja.app.BApp` como base abstract.

Jerarquía:
```
BObject → BValue → BComplex → BComponent → BApp
```

Implements: `BIService`, `BIStatus`, `BIAppComponent`, `BILicensed`.

Módulo: `app-rt.jar`.

Subclases concretas verificadas:
- `BWebApp` (`javax.baja.web.app`) — web application requires BWebService
- `BBajaScriptWebApp` — BajaScript-based web application

### 20.1.2 Lifecycle callbacks

4 callbacks protected no-op default:
- `enabled()` — fired al transicionar enabled property false→true
- `disabled()` — fired al transicionar true→false
- `appOk()` — status fault→operational
- `appFail()` — status operational→fault

Properties:
- `status` (BStatus, readonly) — combined state: disabled bit + fault bit
- `faultCause` (String, readonly) — razón fault (fatal/services/config)
- `enabled` (bool, rw) — user-controlled flag
- `version` (String, readonly) — module vendor version at startup

Métodos clave:
- `doUpdate()` — internal status recalculation
- `getRequiredServices()` — Type[] de services requeridos
- `isDisabled()` / `isFault()` / `isOperational()`
- `configOk()` / `configFail(String)` / `configFatal(String)` — fault management

### 20.1.3 App vs Service vs Component distinction

| Concept | Naturaleza | Lifecycle | Persistencia |
|---------|-----------|-----------|--------------|
| BComponent | Data model tree node | No startup/shutdown más allá de framework init | BOG |
| BAbstractService | Singleton feature manager | `serviceStarted()/serviceStopped()` | BOG + cross-service deps |
| BApp | Application container | `enabled()/disabled()/appOk()/appFail()` | BOG dentro BAppFolder |

**Apps son "user-startable application contexts"** dentro del tree; services son "singleton feature managers"; components son "data model nodes".

### 20.1.4 BStation ≠ BApp

BStation (Bloque 10.1, runtime host) **NO extiende BApp**. BStation es process host platform-level; BApp es application container dentro del tree de esa station. Apps se instancian en BAppFolder children del station tree.

---

## 20.2 net module

Módulo `net-rt.jar`, vendor Tridium, v4.14.0.162. Deps: baja-rt only.

### 20.2.1 Clases principales

**`BInternetAddress`** — composite value para `host[:port]`:
- IPv4 y IPv6 con bracket notation `[::1]:port`
- `getHost()`, `getPort()`, `getAddress()` (resuelve java.net.InetAddress)
- `equivalent(BInternetAddress)` — DNS-aware equality

**`HttpConnection`** — HTTP 1.1 client low-level:
- Static helpers: `post(host, port, uri, contentType, buffer)`, `get(host, port, uri)`
- Request/response header management
- Chunked transfer encoding
- Timeout configuration

**`HttpsConnection`** — SSL/TLS variant, wraps `javax.net.ssl.SSLSocket`. Integra con BC FIPS (Bloque 17.5.7) cuando `licensingFIPS=true`.

**`Http`** — utility constants: METHOD_GET, METHOD_POST, SC_200, SC_404, etc.

### 20.2.2 `BHttpProxyService`

Extends `BAbstractService`. Configura outbound proxy HTTP/HTTPS:

Properties:
- `enabled`
- `server`, `port`
- `exclusions` — CIDR list (default 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
- `authenticationScheme` — `BProxyAuthenticationType` (none/basic/digest)
- `user`, `password` (BPassword)

Integra `java.net.ProxySelector` + `java.net.Authenticator`. Reconfigurable runtime sin JVM restart.

### 20.2.3 NiagaraSocketPermission scoping (Bloque 18.4)

net-rt declara socket permissions diferenciados por contexto:

| Context | Actions | Target |
|---------|---------|--------|
| station | accept, connect, listen, resolve | `*:1-100000` |
| workbench | connect, resolve only | `*:1-100000` |

Esto previene drivers de acceso arbitrario fuera declared port ranges.

### 20.2.4 Consumidores típicos

- **Drivers outbound** (BACnet IP, Modbus TCP, MQTT) usan `HttpConnection` + `BInternetAddress` indirectamente vía protocol-specific wrappers
- **Fox client connections** (`BFoxClientConnection` Bloque 19.11) usan `javax.net.ssl.SSLSocket` via HttpsConnection plumbing + BHttpProxyService
- **BWebService/Jetty** (Bloque 9.3) — servidor usa Jetty separate, outbound calls (webhooks, remote APIs) via net module con BHttpProxyService global

### 20.2.5 No `platNet-rt` separado

Todo networking centralizado en `net-rt`. Drivers platform-specific (`platBacnet-rt`, `platPanelbus-rt`, `platEdgeIo-rt`) usan net module abstractions.

Diferencia con java.net stdlib:
- `BInternetAddress` maneja IPv6 bracket notation automático
- `HttpConnection` integra proxy config runtime
- `BHttpProxyService` permite reconfig sin restart
- Socket access gated por `NiagaraSocketPermission` fine-grained

---

## 20.3 BAbstractService internals

Clase base para TODOS los servicios Niagara (BAlarmService, BHistoryService, BUserService, BAnalyticService, BProvisioningService, etc.).

Jerarquía: `BComponent → BAbstractService`. Implements `BIService` + `BIStatus` + `BILicensed`.

Ubicación source: `niagara-help/source/baja/javax/baja/sys/BAbstractService.java`.

### 20.3.1 Lifecycle callbacks

2 callbacks primary + async futures N4+:

**Sync**:
- `serviceStarted()` — invocado durante bootstrap ANTES de general components start. Otros services ya registrados, componentes no running. Permite cross-service dependency init.
- `serviceStopped()` — invocado en unmount. Cleanup resources.

**Async N4+**:
- `completesStarted()` / `completesStopped()` — completable futures
- `whenServiceStarted()` / `whenServiceStopped()` — chainable callbacks

### 20.3.2 Station bootstrap phases (cross-ref Bloque 10.2.2)

Confirmed 6-phase sequence:
1. **Load** — deserialize config.bog en BStation, mount `local:|station:`
2. **Service Registration** — framework registra todos BIService. `Sys.getService()` available.
3. **Service Initialization** — cada service → `serviceStarted()` callback
4. **Component Start** — entire tree → `start()` → `started()` + `descendantsStarted()`
5. **Station Started** — todos componentes → `stationStarted()`
6. **Steady State** — timer built-in → `atSteadyState()` (configurable via `nre.steadystate` property system)

Services declaran via `getServiceTypes()` returns `Type[]`. Framework enforza dependency order.

### 20.3.3 Fault state management

3 fault states:
- **fatalFault** — permanent, recoverable solo via station restart. Trumps configFault.
- **configFault** — transient, cleared via `configOk()`
- **enabled** — user-controlled disable flag. Triggers `enabled()/disabled()` callbacks.

Métodos: `isFault()`, `isFatalFault()`, `isOperational()`, `isDisabled()`.

Licensing: override `getLicenseFeature()` para vendor-specific license checks per-service + global limits tracking.

---

## 20.4 System Monitor (`systemMonitor-rt.jar`)

**Hallazgo empírico**: módulo real es `systemMonitor-rt.jar`, no "stationMonitor" como especulado originalmente.

`BSystemMonitorService` extends `BAbstractService`. Coordina health monitoring platform-level.

### 20.4.1 Monitor classes hierarchy

Base: `BAbstractMonitor`. Subclases verificadas:

| Monitor | Métrica |
|---------|---------|
| `BPlatformMonitor` | Host OS general |
| `BAbstractCPUMonitor` | Base CPU |
| `BIdleCPUMonitor` | Idle CPU % |
| `BUsedCPUMonitor` | Used CPU % |
| `BHeapMemoryMonitor` | Heap used/max |
| `BMetaSpaceMemoryMonitor` | Metaspace (JDK 8+) |
| `BCodeCacheMemoryMonitor` | JIT code cache |
| `BLoadedClassesMonitor` | Loaded class count |
| `BSocketStateMonitor` | Active socket connections |
| `BRamDiskMonitor` | RAM disk usage |

Monitors run en background threads polling JMX + OS APIs.

### 20.4.2 Thresholds típicos + alarm escalation

Defaults configurables:
- Heap > 85% → warning
- Heap > 95% → critical
- FDs > 80% → warning
- Disk free < 10% → warning
- GC pause > 1 sec → warning

Alarms integradas con BAlarmService (Bloque 8.1) → recipients (email/SNMP/dashboard/external).

---

## 20.5 EngineManager + LeaseManager + ResourceManager

Package `com.tridium.sys.engine` + `com.tridium.sys.resource` (en `baja.jar`).

### 20.5.1 EngineManager

Singleton coordinador del engine thread event-driven (Bloque 6.1, NO scan cycle).

Componentes internos:
- `EngineCycleQueue` — pending callbacks (timer events, property changes, RPC responses, link updates)
- `EngineThread` — single thread que dequeues + executes secuencial
- Thread priority configurable

Stats expuestos `/spy/sysManagers/engineManager`:
- `totalCycles` — cumulative desde boot
- `avgCycleTime` — ms/cycle
- `maxCycleTime` — worst case
- `currentQueueDepth` — pending
- `activeThreads`
- `callbacksProcessed`

Spy sub-pages:
- `EngineManager$SummaryPage`
- `EngineManager$HogsPage` — top callback consumers (critical para diagnosticar lentitud)
- `EngineManager$TicketQueuePage` — async jobs
- `EngineManager$ResetPeakScanTime` — metrics reset

### 20.5.2 LeaseManager

Gestiona "leases" — time-bound resource reservations.

Uso:
- **Resource expiration** — cached objects con TTL, lease holds reference, expiry triggers cleanup
- **Lock leases** — distributed lock simulator (acquires lease, holds until released)
- **Connection pool leases** — DB/network connections, expiry returns to pool
- **Worker thread leases** — async job execution, lease representa pending job

Classes:
- `LeaseManager$LeaseContext` — lease state transitions
- `LeaseManager$LeaseThread` — background reaper
- `LeaseManager$SummaryPage` — spy page active leases + timeline

Config: lease timeout + max concurrent leases per resource type.

### 20.5.3 ResourceManager

JVM resource utilization. Expone `/spy/sysManagers/resourceManager`:

- **Heap** — used bytes, max, percent
- **GC** — pause frequency, cumulative time, last GC timestamp
- **Memory Pools** — young/old gen, metaspace, code cache breakdown
- **Threads** — active, peak, daemon
- **File Descriptors** — open count, soft/hard limits (Unix only)
- **Disk** — free per mount, used %

Classes: `ResourceManager$Page` (tabular spy) + `ResourceReport` (structured export).

---

## 20.6 JFR + JMX + performance profiling

### 20.6.1 JFR (Java Flight Recorder)

Bloque 17.5.2 confirmó `jfr.exe` + `jfr.jar` presentes en JRE Azul Zulu 1.8.0_412. Niagara soporta JFR vía JVM flags:
```
-XX:StartFlightRecording=duration=60s,filename=recording.jfr
```

On-demand recordings capturan CPU, I/O, memory allocation, lock contention, method profiling. Accesible desde spy pages o external JDK Mission Control.

### 20.6.2 JMX exposure

Niagara expone MBeans para engine metrics, resource pools, service status.

Ports típicos:
- 9010 — unsecured (raro en producción)
- 9011 — SSL default

Requires `MBEAN_PERMISSION` (Bloque 18.4, de los 3 permissions que SIEMPRE requieren firma).

MBean naming estándar:
- `com.tridium.sys.engine:type=EngineManager`
- `com.tridium.sys.resource:type=ResourceManager`
- `com.tridium.sys.license:type=LicenseManager`

Remote clients: JConsole, VisualVM, custom tooling.

---

## 20.7 `BJobService` base (distinto de `BBatchJobService`)

Clase: `javax.baja.job.BJobService` extends `BComponent` implements `BIService`. Ubicación `/Services/JobService`.

### 20.7.1 Diferencias con BBatchJobService (Bloque 14.11)

| Aspecto | BJobService | BBatchJobService |
|---------|-------------|-------------------|
| License gate | Sin restricción | Feature `provisioning` |
| Persistence | TRANSIENT (jobs discarded post-disposal) | Summaries persist con retention |
| Concurrency | ForkJoinPool (parallel) | Serialized batch ops |
| Storage | Dynamic children `type?` (no archival) | Persistent prototypes + history |
| Use case | System jobs, discovery, save, archive | Batch editor + provisioning |

### 20.7.2 BJob lifecycle

Clase abstract: `BJob` extends `BComponent`. Override `doRun(Context cx)` + `doCancel(Context cx)`.

State machine:
- `jobState`: `unknown` → `running` → `success` / `canceled` / `failed` (terminal)
- `progress`: `-1` (unknown) → `0-100%`
- Timestamps: `startTime`, `endTime`, `heartbeatTime` (BAbsTime con `Clock.time()` updates)

Lifecycle callbacks:
- `doSubmit()` — reset log, jobState=running, call doRun() en background thread
- `progress(int percent)` — update progress + heartbeatTime atomic
- `heartbeat()` — update heartbeatTime solo (monitor alive sin % tracking)
- `success()` / `canceled()` / `failed(Throwable)` — terminal transition + `complete(BJobState)`
- `doDispose()` — remove job from parent service + clean log (throws if running)

`complete(state)` sets endTime + valida final state terminal.

### 20.7.3 JobLog + recovery

JobLog = circular buffer con JobLogItem entries. LogSequence permite incremental reads via `readLogFrom(BLong sequenceNum)` para remote monitoring.

Log behavior:
- Cleared en `doSubmit()`
- Appended durante execution
- NOT persisted BOG por default (flags TRANSIENT)

**Crash recovery**: jobs in-flight **NO recovered**. JobService restart recreates fresh executor + monitorWorker. Excepción: `BStationSaveJob` synchronized con BOG persistence (Bloque 10.1).

### 20.7.4 MonitorWorker hung job detection

`MonitorWorker` polls cada 2 seg (`DEFAULT_THREAD_MONITOR_INTERVAL_MS = 2000`). Detecta hung jobs comparando `heartbeatTime` vs `Clock.time()`.

`UncaughtJobExceptionHandler` captura uncaught Throwables prevents executor thread pool starvation.

### 20.7.5 Consumidores BJobService

- `BStationSaveJob` (system) — save config.bog en configuration changes
- `BBackupService` jobs — backup creation/restore
- `BHistoryService` jobs — archive rotation + purge
- `BACnet/Modbus/OPC discovery jobs` — `BAbstractPollService` subclasses
- `BLonLearnJob` — supervised learning startup, cancellable UI

Submit pattern: `job.submit(cx)` → `BJobService.getService().submit(job, cx)` → `doSubmitAction()` adds transient child + `job.doSubmit()`.

---

## 20.8 Persistent policies per subsystem

### 20.8.1 History retention + archive (Bloque 8.2 complemento)

`BHistoryConfig`:
- `capacity` — BCapacity (record count o bytes). Default 500 records.
- `fullPolicy` — BFullPolicy (roll / stop / archive). Default **roll** (overwrite oldest on full).
- `interval` — BCollectionInterval (IRREGULAR / regular time steps)
- `storageType` — BStorageType.file (HDB file en `~/station/history/`)

Archive mechanism (BHistoryService):
- Rotation scheduled via BScheduleService (Bloque 8.3) típicamente midnight
- Old histories → `file:^^history/archive/` con timestamp suffix
- Archive jobs usan BBackupService file ops para compression/cleanup
- Retention days configurable antes de delete permanente

Default: 500-record capacity, rollover, sin explicit archive age limit.

### 20.8.2 Alarm retention (Bloque 8.1 complemento)

`BAlarmService`:
- `alarmDbConfig` — BAlarmDbConfig (file-based ADB) specifies storage path + max records
- `defaultAlarmClass` — routing rules + recipient list
- `coalesceAlarms` (default true) — merges duplicate source alarms
- `escalationTimeTrigger` — BTimeTrigger (default 1 minute)

Retention:
- DB file auto-compacts en startup (fragmentation cleanup)
- Completed alarms retained per `acknowledgeTime` + configurable TTL
- **No auto-ack timeout directo** en BAlarmService — apps implementan via BAlarmClass rules
- Archive opcional a external DB via jobs

### 20.8.3 Audit log rotation (Bloque 18.8 complemento)

Delegado a Auditor interface + AuditEvent records:
- `BAuditEvent` — timestamp, user, action, target, result
- Storage: file o remote (configurable)
- Rotation size-based (`audit.log.0 → audit.log.1`) + gzip compression
- Retention filesystem-level (admin-configurable)

**No built-in auto-delete** — confía en external cleanup jobs o OS logrotate.

### 20.8.4 Backup retention (Bloque 10.3 complemento)

`BBackupService`:
- `excludeFiles` — semicolon-separated patterns default `*.hdb;*.adb;*.lock;*backup*`
- `excludeDirectories` — BOrdList (history, alarm, webFileCache)
- `offlineExcludeFiles` / `offlineExcludeDirectories` — separate offline policies

Backup jobs (BBackupJob subclass):
- ZIP archive en `file:^^backup/` con timestamp
- **No built-in retention days** — admin delete manual o scheduled job
- Restore valida manifest + decrypt PBEEncryptingInputStream si password-protected

### 20.8.5 Session + login policies (Bloque 11 complemento)

`BUserService` confirmed defaults:

| Property | Default | Range |
|----------|---------|-------|
| `lockOutEnabled` | true | bool |
| `lockOutPeriod` | 10 seg | BRelTime |
| `maxBadLoginsBeforeLockOut` | 5 | 1-10 |
| `lockOutWindow` | 30 seg | BRelTime |
| `defaultAutoLogoffPeriod` | 15 min | 2 min - 4 hrs |
| `secureOnlyPasswordSet` | true | requires HTTPS for password change |

Comportamiento default: 5 failures en 30 seg → 10-seg lockout, session timeout 15 min.

`SessionManager` (com.tridium.session):
- Persistent login cookies (encrypted, short-lived default)
- Failed login tracking per user + IP (brute force mitigation)
- Auto-logout por inactividad
- Concurrent session limits (implementation-specific per transport)

---

## 20.9 Misc services remaining

### 20.9.1 Logging — no dedicated service

Hallazgo: **No existe `BLoggingService` dedicado**. Logging via `java.util.logging.Logger` throughout:
- Logger per module: `Logger.getLogger("module.path")`
- Levels SEVERE/WARNING/INFO/FINE/FINER/FINEST (mapea ERROR/WARN/INFO/DEBUG/TRACE)
- Handlers: FileHandler (rotation), ConsoleHandler, custom SyslogHandler
- Config: `logging.properties` en NRE home
- Per-module overrides: `setLevel()` dinámico

### 20.9.2 `BSpyService` (complementa Bloque 10.2.4)

Framework para `/spy/` servlet path:
- `BSpySpace` registration API — subclasses provide `spy(SpyWriter out)` method
- `SpyWriter` genera HTML tables/pre-blocks
- `ObjectSpy` traversa component tree
- Gated: `BIRestrictedComponent` requires `spy:read` permission

`BSpy` class — static helpers para spy page formatting.

### 20.9.3 `BHelpService`

Context-sensitive help runtime:
- Mapea component types → help URLs
- Resolve ords a help docs (HTML, PDF, external links)
- Per-type docstrings via `@NiagaraType` javadoc annotations
- No explicit help DB — sourced from HTML docs o external wiki

### 20.9.4 `BDebugService` (no dedicated class)

Debug toggles per módulo implementados como:
- Static flags (ej. `driver.bacnet.debug` property)
- Dump triggers memory/thread dumps on-demand (JMX integrado)
- Conditional logging: `if (debugEnabled) expensiveLog(...)` pattern

### 20.9.5 `BLexiconService` runtime (Bloque 12.2 complemento)

**No standalone service**. Integrado via:
- BajaScript i18n: `$L("module:key")` lookup runtime
- Message keys: `module-name.properties` files (ej. `baja.properties`)
- Locale resolution: `Sys.getLocale()` returns BLocale activa (set via `NRE_LOCALE` env var)
- `BTranslatable` interface para component display names + descriptions
- Fallback: English keys si translation missing

Ejemplo: `BJob.toString()` → `getType().getDisplayName(locale)` localized.

---

## 20.10 Final gap analysis — qué queda sin cubrir

Después de 20 bloques, reconocimiento honesto de áreas NO cubiertas o cubiertas superficialmente:

### 20.10.1 Arquitectura + patrones no profundizados

1. **Transaction semantics** — cómo operaciones multi-step (backup, history archive, auth) handle failures mid-operation. Rollback/compensation patterns no documentados.

2. **Clustering + distributed topology** — más allá de Supervisor/Subordinate (Bloques 13.1 + 19.11): consensus, failover automático, station-to-station sync. Bloque 19.14 confirmó que **no HA nativa** en NiagaraDriver — gap operacional conocido.

3. **Module lifecycle hooks pre/post load** — startup ordering más allá de service dependency resolution (Bloque 10.2.2). Module initialization hooks.

4. **Performance tuning specifics** — thread pool sizing, GC tuning, I/O buffering strategies. Solo mencionado parcialmente (Bloque 17.5.5 JVM flags defaults).

5. **Custom type system extensions** — cómo third-party types register con `Sys.loadType()`. Class loading + module isolation.

6. **Migration patterns** — schema evolution en `config.bog`. Handling old component versions post-upgrades. Bloque 12.2 cubrió AX→N4 pero no intra-N4 migrations.

### 20.10.2 Integraciones vendor/enterprise

7. **Honeywell-specific modules** deep — `platPower`, `jsonToolkit`, `honPlantController` (libplantctrl.so native) mencionados sin profundizar.

8. **SMA (Smart Meter Architecture) licensing** — feature mencionado en Bloque 2, no profundizado en flow.

9. **Remote diagnostics channels** — Niagara tiene capacidad de diagnóstico remoto vendor-specific, no investigado.

10. **FIPS compliance modes** — BC FIPS provider documentado (Bloque 17.5.7) pero no workflow operacional completo FIPS enforcement.

### 20.10.3 Third-party integration

11. **LDAP/SAML/OAuth auth** — `BPasswordAuthenticationScheme` cubierto (Bloque 11), pero external federation providers deep no.

12. **Custom datasources** — Oracle/SQL Server/timeseries DBs externos. Solo file/SQLite/OrientDB embedded mencionados implícitos.

13. **Non-HTTP transports** — serial deep, Modbus TCP deep. `fox.sys` y `ndriver` packages existen sin analyzar.

14. **Skyspark integration** — confirmado ausente en Bloque 16, pero no alternativas third-party analytics.

### 20.10.4 Security model details

15. **Key rotation** — TLS certs (Bloque 17.3.2) documentados, master.jceks rotation workflow no.

16. **Token expiry** — Fox session 24h (Bloque 13.2), BEARER authToken BOX (Bloque 18.6) — lifecycle detallado cross-token no.

17. **RBAC enforcement en method invocation level** — Bloque 11 cubrió role/category, pero Auditor integration en invocation no.

### 20.10.5 Gotchas de producción probables no documentados

18. **TimeZone handling en multi-zone archives** — `BHistoryConfig.timeZone` set per history, no enforced globally. Gotcha cross-zone aggregation.

19. **Clock.time() drift en RTC sync events** — potential duplicate history records cuando reloj adjusted backward mid-day.

20. **ForkJoinPool parallelism vs blocking I/O** — BJobService usa ForkJoinPool pero blocking I/O interactions no tuning guidance.

21. **Audit event loss si BAuditService unavailable** — fire-and-forget vs queued semantics no especificados en docs.

22. **Job exception handling** — framework assumes subclass implements exception logging. No framework-level persistence de failed job details.

23. **Large backup restore timeout** — sin chunking/resume mencionado. High-latency network restores pueden fallar.

24. **History archive blocks UI durante DB compaction** — synchronous cleanup. Gotcha latency operacional.

25. **Session timeout clock skew** — multiple servers sin NTP sync causa session drops inconsistentes.

26. **Lockout window edge case** — si clock adjusted backward, re-triggers failures (Bloque 20.8.5).

27. **Audit retention** — sin auto-delete built-in, confía en OS logrotate externo. Risk de disk fill en deployments que olvidan configurar.

### 20.10.6 Recomendación para cerrar gaps

Para completar estas áreas se requiere:
- Vendor documentation enterprise modules Honeywell/Tridium
- Production deployment guides (no disponibles en install)
- Security audit reports de deployments reales
- Performance profiling high-throughput multi-station federation
- Testing cross-timezone + clock adjustment scenarios
- Vendor roadmaps para HA + clustering futuros (N4.15+)

---

## 20.11 Hallazgos críticos del bloque

1. **NO existe BAbstractApp** — solo BApp como base abstract. BApp extends BComponent (no BFrame). Implements BIService + BIStatus + BIAppComponent + BILicensed.

2. **BStation NO extiende BApp** — process host ≠ application container. Apps se instancian en BAppFolder children.

3. **Subclase concreta principal de BApp**: `BWebApp` (requires BWebService) + `BBajaScriptWebApp`.

4. **net-rt.jar centralizado** — no `platNet-rt` separado. Contiene `BInternetAddress`, `HttpConnection/HttpsConnection`, `BHttpProxyService`, `Http` utility.

5. **`BHttpProxyService`** — proxy config reconfigurable runtime sin JVM restart. CIDR exclusions default (10/8, 172.16/12, 192.168/16).

6. **BAbstractService lifecycle N4+**: 2 callbacks sync (`serviceStarted/Stopped`) + async futures completables (`completesStarted`, `whenServiceStarted`).

7. **6-phase boot**: Load → Service Registration → serviceStarted → Component Start → stationStarted → atSteadyState (controlado por `nre.steadystate` property).

8. **Módulo real `systemMonitor-rt.jar`** (no "stationMonitor"). 10+ monitor classes extends `BAbstractMonitor` cubriendo CPU/memory/classes/sockets/disk.

9. **EngineManager spy sub-pages** — `$SummaryPage`, `$HogsPage` (top consumers crítico para debugging), `$TicketQueuePage`, `$ResetPeakScanTime`.

10. **LeaseManager abstrae** 4 tipos: resource expiration, lock leases, connection pool, worker thread. `$LeaseContext` + `$LeaseThread` + `$SummaryPage`.

11. **BJobService** (sin license gate) vs **BBatchJobService** (license `provisioning`) — diferencia clara en persistence + concurrency + use case.

12. **BJob jobs NOT persistidos BOG default** (flags TRANSIENT). Crash → jobs in-flight perdidos. Excepción: BStationSaveJob sincronizado con BOG.

13. **MonitorWorker 2-seg polling** para hung job detection. `UncaughtJobExceptionHandler` previene thread pool starvation.

14. **Persistent policies**: History default 500 records roll, no age limit; Alarm file ADB no auto-ack; Audit sin auto-delete (requires OS logrotate); Backup sin retention days built-in; Session default lockout 5 failures/30s → 10s + 15 min auto-logoff.

15. **NO BLoggingService dedicada** — usa java.util.logging.Logger throughout con per-module config.

16. **BLexiconService NO standalone** — integrado en BajaScript `$L()` + `.properties` files + `Sys.getLocale()` + `BTranslatable` interface.

17. **JMX Niagara**: ports 9010 (unsecured, raro) y 9011 (SSL default). MBean naming `com.tridium.sys.{engine,resource,license}:type=...`.

18. **JFR soporte** via JVM flags (`-XX:StartFlightRecording=...`) — JDK Mission Control attachable.

19. **27+ gaps reconocidos** tras 20 bloques — coverage honesto pero gaps operacionales + enterprise + vendor-specific + edge-case time handling + integrations externas requieren investigación adicional.

---

## 20.12 Conexiones con otros bloques

- **Bloque 3 (Security)**: MBEAN_PERMISSION requerido para JMX exposure.
- **Bloque 4 (Baja)**: BApp extends BComponent — hereda lifecycle + slots + facets.
- **Bloque 6.1 (Control engine)**: EngineManager implementation del event-driven single-thread model.
- **Bloque 8 (Services)**: BAbstractService es base de BAlarmService, BHistoryService, BScheduleService.
- **Bloque 9.3 (Web)**: BWebApp require BWebService; net module soporte outbound HTTP con proxy.
- **Bloque 10.2.2 (Boot sequence)**: 6-phase confirmado con callbacks específicos por phase.
- **Bloque 10.2.4 (Spy pages)**: `/spy/sysManagers/*` enumerados — engineManager, leaseManager, resourceManager, licenseManager.
- **Bloque 11.3 (Session)**: BUserService defaults lockout 5/30s → 10s + 15 min auto-logoff confirmados.
- **Bloque 14.11 (Batch Editor)**: BJobService vs BBatchJobService comparative clarificado.
- **Bloque 16 (Analytics + Provisioning)**: BAnalyticService + BProvisioningService extienden BAbstractService con lifecycle estándar.
- **Bloque 17.5.2 (JRE)**: JFR jfr.exe/jfr.jar confirmed + JVM flags defaults.
- **Bloque 18.4 (NiagaraSocketPermission)**: net-rt declara scope diferenciado station vs workbench.
- **Bloque 19.11 (NiagaraDriver)**: BFoxClientConnection usa HttpsConnection + BHttpProxyService globalmente.

---

## Engram topic keys

- `niagara/misc/bapp-webapp-net-module-httpproxy`
- `niagara/misc/babstractservice-lifecycle-monitors-engine-lease-resource`
- `niagara/misc/bjobservice-persistent-policies-gap-analysis`
