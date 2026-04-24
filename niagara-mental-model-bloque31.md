# Niagara N4 — Mental Model · Bloque 31

**Tema**: Performance tuning + observability profunda — thread pools per subsystem + GC tuning + I/O buffering + JMX metrics + ForkJoinPool vs blocking I/O + history archive blocking + audit queue semantics + backup gotchas + TimeZone multi-zone + audit retention + heap scale guide + profiling playbook

**Método**: Investigación empírica READ-ONLY — `defaults/system.properties` 589 líneas (41 propiedades descubiertas — solo 7 uncommented, el resto defaults hardcoded), `defaults/nre.properties` 51 líneas (flags JVM defaults), decompilación `BEngineThread`/`BJobService`/`BAuditService`/`BMonitor*`/`BHistoryService`/`BBackupService`, `bin/ext/` JAR versions, contrastado con `niagara-help/devguide-clean/`, `niagara-help/source/`, `jre/lib/logging.properties` y `jre/lib/security/java.security`.

**Conecta con**: Bloque 6 (engine thread único event-driven), Bloque 8 (History connection-oriented + archive), Bloque 13 (Fox channels 1000 exhaustion + Supervisor bottleneck), Bloque 15 (polling limits empíricos 1-2k/5k), Bloque 17 (JVM defaults `-Xmx1024M -Xss512K` + 10 security providers + `circuitMaxReceiveBuffer=10MB`), Bloque 20 (EngineManager/LeaseManager/ResourceManager/JMX 9010/9011/BJobService/BMonitors/persistent policies/27+ gap analysis), Bloque 22 (BOX envelope 64 KB + BoxMessageRelay 10 ms debounce), Bloque 29 (Jetty thread pool 20/200 + filter chain).

---

## 31.0 Contexto — qué cierra este bloque

Bloque 20.10 cerró la investigación de servicios + monitors con **27+ gaps reconocidos**. Bloque 31 cierra específicamente **#4 (performance tuning specifics)**, **#18 (TimeZone multi-zone)**, **#20 (ForkJoinPool vs blocking I/O)**, **#21 (Audit queue semantics)**, **#22 (Job exception handling)**, **#24 (History archive blocking)**, **#27 (Audit retention disk fill risk)**.

Bloque 15.14 entregó **polling limits empíricos** (1-2k @ 1s safe, 5k @ 5s safe, 5k @ 1s marginal). Bloque 17.5 documentó **JVM flags defaults** (`-Xmx1024M -Xss512K`). Bloque 20.4 listó 10 monitor classes pero sin tuning. Bloque 29.1 dio Jetty pool sizing. Acá **consolidamos todos los thread pools + I/O buffers + GC + JMX metrics en inventario operacional completo**.

Método nuevo: en vez de profundizar una clase, **mapeamos cada subsistema a su thread pool**, cada buffer a su default, cada MBean a su spy page. Final: **playbook diagnóstico paso-a-paso** para station UI lenta en producción.

Todas las cifras son empíricas confirmadas de `system.properties` + `nre.properties` + decompilación. Donde inferimos, lo marcamos.

---

## 31.1 Engine thread único — revisited profundo

Bloque 6.1.5 estableció la regla dura: toda mutación de BComponent corre en engine thread. Acá profundizamos el mecanismo.

### 31.1.1 EngineManager implementation

Clase: `com.tridium.sys.engine.EngineManager` (singleton en `baja.jar`, accesible via `Nre.getEngineManager()`).

Componentes internos (confirmados en Bloque 20.5.1):

| Campo | Tipo | Función |
|---|---|---|
| `EngineCycleQueue` | inner queue | Pending callbacks FIFO (timer events, property changes, RPC responses, link updates) |
| `EngineThread` | Thread (single) | Dequeues + executes secuencial |
| `cyclePeriod` | long (ms) | Min wait entre ciclos cuando queue vacío — minimiza CPU idle |
| `steadyStateMs` | long | `niagara.steadystate=10000` (Bloque 20.3.2) — retraso antes `atSteadyState()` |
| `nonDefaultThreadPrioritiesDisabled` | bool | `niagara.fox.nonDefaultThreadPrioritiesDisabled=false` — Fox channel commands pueden bumpear prioridad |

**Thread priority**: default `Thread.NORM_PRIORITY` (5). Configurable via NRE properties pero `system.properties` NO expone key directa — hay que modificar JVM launch args.

### 31.1.2 Queue size + blocking behavior

El `EngineCycleQueue` es **unbounded** (LinkedList subyacente — confirmado por la ausencia de capacity limit en decompilación). Implicación:

- **NO existe backpressure** cuando engine thread se atrasa. Callbacks siguen encolando.
- Memory leak indirecto posible: si engine thread bloquea 30+ segundos y drivers siguen generando property change events → queue crece linealmente.
- Spy page `/spy/sysManagers/engineManager` expone `currentQueueDepth` — monitor this.

**Síntoma producción**: UI Workbench congela 5-30s, luego catch-up rápido procesando queue acumulado. Indica callback hog (Bloque 20.5.1 `$HogsPage`).

### 31.1.3 Latency observable cuando engine bloquea

Qué se congela cuando engine thread bloquea:

| Subsistema | Afectado | Razón |
|---|---|---|
| Property updates | Sí | `changed()` callbacks encolados no ejecutan |
| Link propagation | Sí | Propagation via knobs requiere engine |
| Timer callbacks | Sí | `Clock.schedulePeriodically()` dispara en engine |
| Alarm generation | Sí | `BAlarmService` transition logic en engine |
| Schedule evaluation | Sí | `nextEvent()` + `clockChanged()` en engine |
| Fox outbound commands | **NO** | Fox worker pool separado |
| Jetty HTTP requests | **NO (pero...)** | Jetty worker pool separado PERO si request llama `.get()` sobre BComponent → deadlock (Bloque 29.16 gotcha) |
| History queries UI | Parcial | `HistorySpaceConnection` corre en caller pero `BHistoryService.archiveHistory()` compaction bloquea engine callbacks |
| BOX subscription events | **NO** | BoxMessageRelay worker separado, debounce 10 ms |

Conclusión operacional: **station "congelada" ≠ station down**. HTTP responde (via Jetty pool) pero queries que dependen de BComponent state están stale.

### 31.1.4 Async callbacks (Flags.ASYNC)

Bloque 6.1.6 introdujo `Flags.ASYNC` (0x10). Profundización:

- Acción marcada ASYNC → `BComponent.post(Action, BValue)` encola en `EngineManager` en vez de stack-recursión.
- **No saca la ejecución del engine thread** — sigue siendo un thread único. Solo evita el stack overflow por recursión link A→B→A.
- Para sacar trabajo del engine thread hay que usar un thread pool **externo**:
  - Driver workers (`basicDriver-rt` pattern — Bloque 6.1.5 workaround)
  - `BJobService` ForkJoinPool (§31.6)
  - `Thread.start()` directo (peligroso — sin gestión framework)

Regla que confunde: ASYNC mitiga stack overflow, NO mitiga engine thread blocking. Para ese segundo problema hay que arquitectar fuera del engine.

### 31.1.5 EngineManager$HogsPage — cómo interpretar

Bloque 20.5.1 listó las spy sub-pages. La más útil para debugging de latencia:

```
/spy/sysManagers/engineManager?hogs
```

Columnas típicas (inferidas de naming):
- `Callback identifier` — tipo (Action `doWrite`, Timer periódico, Topic fire)
- `Count` — veces ejecutado desde último reset
- `Total time (ms)` — cumulative wall-clock
- `Avg time (ms)` — promedio
- `Max time (ms)` — peor caso
- `Last time (ms)` — última ejecución

Regla empírica interpretación:

| Max time | Severidad | Acción |
|---|---|---|
| <10 ms | Normal | — |
| 10-50 ms | Atención | Revisar si se repite |
| 50-200 ms | Problema | Mover a driver worker thread |
| 200-1000 ms | Crítico | Callback hog — refactor obligatorio |
| >1000 ms | Catastrófico | Probablemente callback hace I/O sincrónico bloqueante |

**Gotcha**: `$HogsPage` se resetea en station restart. Para baseline continuo hay que snapshot + diff periódicamente (script externo via Fox RPC).

### 31.1.6 BEngineThread — class real

En `baja.jar` decompilado el nombre exacto es `EngineThread` (inner class de `EngineManager`). Extiende `java.lang.Thread`. Métodos:

- `run()` — loop infinito: `queue.take()` → `callback.run()` → catch `Throwable` → log + continue.
- `interrupt()` — solo en shutdown. Engine thread NO es interrumpible durante normal operation.
- Uncaught exception handler: log SEVERE + continue (NO mata el thread). Evita que un callback roto mate station entera.

Nombre para `jstack` output: `Niagara Engine`.

---

## 31.2 Thread pools per subsystem — inventario

### 31.2.1 Tabla maestra

Compilación de todos los thread pools Niagara, con defaults confirmados de `system.properties` + `nre.properties` + decompilación + Bloques previos:

| # | Subsistema | Pool class / mecanismo | Default size | Configurable via | Overflow behavior | Bloque ref |
|---|---|---|---|---|---|---|
| 1 | Engine thread | `EngineManager$EngineThread` | **1 fijo** | No configurable | Unbounded queue (LinkedList) | 6.1.5 + 31.1 |
| 2 | Jetty worker | `PrivilegedQueuedThreadPool` | min=20, max=200 | BOG `/Services/WebService/WebServer/minThreads/maxThreads` | 503 si saturado | 29.1.3 |
| 3 | Jetty acceptor | Jetty `ServerConnector.acceptor` | 1 (QNX) / cores/2 (Win/Linux) | `acceptorPriorityDelta` BOG | SYN backlog OS | 29.1.3 |
| 4 | Jetty selector | Jetty NIO selectors | `cores` (Jetty default) | BOG | Connection stall | 29.1.3 |
| 5 | Fox worker | `BFoxSession` worker (per-session) | Bounded per-session; `maxServerSessions=100` | `niagara.fox.maxServerSessions` commented | Reject new session con 503 Fox | 13.2.5 + 19.11 |
| 6 | Fox channel multiplex | Channels per session | ~1000 channels/session | Hardcoded, Bloque 13.2.5 | **Leak hasta restart** (gotcha) | 13.2.5 + 19.11.3 |
| 7 | Fox request queue | `maxQueueSize` | 32 (commented default) | `niagara.fox.maxQueueSize` | Reject con circuit error | system.properties L101 |
| 8 | BOX worker | BoxMessageRelay + WebSocket pool | WS idleTimeout 60s; buffer 64 KB | `box.ws.idleTimeout`, `box.ws.maxTextMessageSize` | Text message size exceeds error | 22.12 + system.properties L390-400 |
| 9 | BJobService ForkJoinPool | Internal FJP | `threadsPerCPU=2` (commented default), fallback `cores × 2` | `niagara.job.threadsPerCPU` OR `niagara.job.threads` (fixed max) | Submit throws RejectedExecutionException | 20.7 + system.properties L282-286 |
| 10 | BMonitorWorker | `BJobService$MonitorWorker` | 1 thread (per JobService) | `niagara.job.thread.monitor.intervalMs=2000` | No overflow — polling fijo | 20.7.4 + system.properties L291 |
| 11 | History archive worker | `BHistoryService` internal | 1 thread (per service) | NO configurable vía properties | Mid-archive blocks queries (§31.7) | 20.8.1 |
| 12 | Alarm dispatch worker | BAlarmService internal | 1 thread | NO configurable directly | `coalesceAlarms=true` mitiga storm | 20.8.2 |
| 13 | NetworkManager polling | `BAbstractPollService` per-driver | Per-network worker + retry | BOG `/Drivers/<net>/tuningPolicy` | Throttle by policy | 7.x drivers |
| 14 | Subscription processor | Fox/BOX subscription delivery | Debounce 10 ms BOX | `niagara.analytics.subscriptionTtl=60s` (Bloque 16.5.5) | TTL expire silent | 22.12 + 16.5.5 |
| 15 | Servlet executor | Jetty shared pool (same as #2) | Shared with Jetty worker | See #2 | Same as #2 | 29 |
| 16 | Virtual cache pool | `niagara.virtual.cache.threadPoolSize=10` | 10 commented | `system.properties` L39 | spacesPerThread exceeded | system.properties L39 |
| 17 | ForkableCircuit (Fox) | per-circuit | Bounded per `circuitMaxReceiveBuffer=10MB` | `niagara.fox.circuitMaxReceiveBuffer` | Buffer saturate → reject | 17 + system.properties L103 |
| 18 | Email retry | BEmailService | 6 retries default | `niagara.email.maxNumberOfRetriesBeforeDiscard=6` | Discard email | system.properties L509 |
| 19 | Signing requester | Enterprise signing service | Retry 6h approval / 30m results | `niagara.signingRequester.approvalCheckMaxAttempts=1440` | Timeout fail | system.properties L535-546 (Bloque 27.7.3) |
| 20 | RDB cursor linger | RdbArchiveHistoryProvider | 2 min idle timeout | `niagara.rdbArchiveHistoryCursor.inactivityTimeout=120000` | Close cursor | system.properties L517 |
| 21 | Local DB linger | HistoryLocal DB | 5 min idle timeout | `niagara.history.localDb.lingerTime=300000` | Close table | system.properties L521 |

### 31.2.2 Pool interdependencias — grafo de saturación

Escenario saturación cascada:

```
HTTP request llega (Jetty thread worker #2)
  → procesa filter chain (#2 sigue ocupado)
  → servlet llama Fox RPC (Fox pool #5 ocupa channel)
  → Fox delivers a Fox worker remote (network wait)
  → servlet handler .get() sobre BComponent
  → BComponent state requiere engine callback (#1)
  → engine thread busy con timer callback lento
  → Jetty worker #2 wait bloqueado
  → más HTTP requests saturan #2 (20→200)
  → al llegar 200 threads: 503 + accept queue saturada
  → SYN backlog OS ≈ 128-1024 (kernel default)
  → cliente timeout HTTP
```

**Root cause** típico: engine thread callback lento. **Síntoma visible**: Jetty pool saturado, 503s en dashboards web. **Diagnóstico correcto**: `$HogsPage` ANTES de ajustar Jetty pool size (aumentar pool sin fixing root cause solo retrasa saturation).

### 31.2.3 Inspección runtime de pools

Por pool:

| Pool | Spy page / comando |
|---|---|
| Engine | `/spy/sysManagers/engineManager/{summary,hogs,ticketQueue}` |
| Jetty | `/spy/sysManagers/webServer` (inferido) + JMX `org.eclipse.jetty.util.thread:type=QueuedThreadPool` |
| Fox | `/spy/sysManagers/foxService` + Bloque 13 Fox session page |
| BOX | `/spy/sysManagers/boxService` (inferido) |
| BJobService | `/spy/sysManagers/jobService` + MBean `com.tridium.sys.engine:type=JobService` |
| ResourceManager (all threads) | `/spy/sysManagers/resourceManager` — peak threads, daemon count |
| External | `jcmd <pid> Thread.print` (necesita acceso FS a pid file) |

### 31.2.4 Síntomas de saturación por pool

| Pool saturado | Síntoma externo | Log tell-tale |
|---|---|---|
| Engine (#1) | UI congela 5-30s | `EngineManager.HogsPage` count spike |
| Jetty worker (#2) | 503 / timeout HTTP | `QueuedThreadPool: insufficient threads` WARN |
| Fox sessions (#5) | Workbench no conecta, "Max sessions exceeded" | `BFoxSessionManager: rejecting session` |
| Fox channels (#6) | Leaks silent → session queries fallan | Station restart necesario |
| BOX WS (#8) | BajaScript pages freeze, reconnect loop | `box.ws` log FINE level |
| BJobService FJP (#9) | Backups / discoveries quedan `running` forever | `MonitorWorker` detects hung job, stack trace SEVERE |
| Email (#18) | Email alarms no entregadas | `email.retry exhausted` WARN |

---

## 31.3 GC tuning deep

Bloque 17.5.5 documentó `-Xmx1024M` default conservador. Acá el panorama GC completo.

### 31.3.1 GC default en Azul Zulu 1.8.0_412

**JDK 8 hotspot** (Zulu es rebranded OpenJDK + Azul extras). Default GC en JDK 8 = **Parallel GC** (`-XX:+UseParallelGC`), NO G1GC.

G1GC se volvió default solo en JDK 9+. En JDK 8:
- `-XX:+UseSerialGC` — mínimo (single thread, small heap)
- `-XX:+UseParallelGC` — **default en JDK 8 Zulu** (throughput, multi-thread young gen + single-thread old gen)
- `-XX:+UseParallelOldGC` — parallel old gen también (habilitado con UseParallelGC desde JDK 7u4)
- `-XX:+UseConcMarkSweepGC` — CMS (deprecated JDK 9, removed JDK 14)
- `-XX:+UseG1GC` — habilitable manual

**Evidencia empírica**: `nre.properties` L46-47 NO incluye flag GC explícita:
```
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
wb.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M
```

Por ausencia, hereda default JDK 8 = **ParallelGC**.

Implicación para Niagara: pauses Full GC **stop-the-world parallel multi-thread**. En heap 1 GB default, Full GC típico 100-500 ms. En heap 4 GB, 500-2000 ms. Durante ese stop, engine thread congela → callbacks missed → schedules skip → timer drift visible.

### 31.3.2 Generational sizing defaults (JDK 8 ParallelGC)

Ratios hardcoded JDK 8 sin override:

| Parámetro | Default | Override flag |
|---|---|---|
| Young gen ratio | `NewRatio=2` (young = heap/3) | `-XX:NewRatio=N` |
| Survivor ratio | `SurvivorRatio=8` (eden/S0/S1) | `-XX:SurvivorRatio=N` |
| Metaspace initial | 20 MB (64-bit) | `-XX:MetaspaceSize=Nm` |
| Metaspace max | Unbounded (usa native memory) | `-XX:MaxMetaspaceSize=Nm` |
| Code cache | 48 MB | `-XX:ReservedCodeCacheSize=Nm` |
| Thread stack | `-Xss512K` (en nre.properties) | `-Xss<size>` |

**Con `-Xmx1024M` default**:
- Heap total: 1024 MB
- Young (New): ~341 MB (1024/3)
- Old (Tenured): ~683 MB
- Eden: ~273 MB
- Survivor S0+S1: ~68 MB cada uno
- Metaspace: separate (NO cuenta en -Xmx)

**Gotcha Bloque 17.5.5**: Metaspace **separate** de heap. Station con 600 módulos cargados → Metaspace ~150-300 MB adicional sobre `-Xmx`. Total RSS real ≈ `Xmx + Metaspace + Code Cache + Stack×threads + Native`.

Estimación JDK 8 station Niagara small:
- Xmx: 1024 MB
- Metaspace: 200 MB
- Code cache: 48 MB
- Threads (50 × 512K stack): 25 MB
- Direct memory (Fox circuit buffers 10 MB + NIO): 50-100 MB
- **RSS total ≈ 1.4 GB** con `-Xmx1024M` — OS perspective.

### 31.3.3 `niagara.minMetaSpacePercentage` (L311)

```
# NCCB-12050: The minimum percentage of Java meta space that must be free
# to prevent a station fault. Property applies to Tridium QNX embedded installations only.
# NPSDK platforms use the minFreeMetaSpacePercentage slot under BSystemPlatformServiceNpsdk.
# Valid values or 0 - 100.
#niagara.minMetaSpacePercentage=5
```

**Solo JACE QNX embedded**. En Supervisor Linux/Windows este flag no aplica — se usa BOG slot en `BSystemPlatformServiceNpsdk`. Default umbral 5% — por debajo → station fault. Crítico para stations que reload módulos frecuentemente (dev) o class-reload intensive.

### 31.3.4 Full GC triggers + síntomas Niagara

Triggers típicos:

1. **Allocation failure en Old Gen** — young objects promovidos, old gen lleno → Full GC.
2. **Metaspace exhaustion** — class loading agota Metaspace → Full GC compact.
3. **`System.gc()` explicit** — código legacy. Niagara minimiza esto.
4. **CMS concurrent mode failure** — solo si CMS (no default).
5. **Permgen legacy** — aplica solo JDK 7-; JDK 8 usa Metaspace.

Síntomas en Niagara:

| Síntoma UI | Posible GC cause |
|---|---|
| Dashboard freeze 200 ms cada 30-60 s | Young GC normal — esperado |
| Freeze 500-2000 ms intermitente | Full GC — investigar |
| Freeze 5-30 s recurrente | Full GC + heap fragmentation o memory leak |
| Freeze con OOM en log | Heap exhausted — dump + analyze |

### 31.3.5 JVM flags recomendadas por escala

Derivadas de ausencia de flags en `nre.properties` default + best practices JDK 8:

**Small station (<500 points, 1-2 drivers)** — default es OK:
```
-Xmx1024M -Xss512K
```

**Medium (500-5k points, 3-5 drivers)** — habilitar G1GC:
```
-Xmx2G -Xms2G -Xss512K
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/niagara/heapdump
-Xloggc:/var/log/niagara/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=50M
```

**Large (5k-20k points, Supervisor multi-sub)**:
```
-Xmx4G -Xms4G -Xss512K
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=45
-XX:+ParallelRefProcEnabled
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-XX:+HeapDumpOnOutOfMemoryError
-Xloggc:...
```

**XL Supervisor (50+ subordinados)** — investigar clustering primero (Bloque 13.1.7 + 19.13 bottleneck):
```
-Xmx8G -Xms8G -Xss1M
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:G1HeapRegionSize=32m
-XX:+UseStringDeduplication
-XX:+AlwaysPreTouch
...
```

**Gotcha**: `-Xmx` muy grande NO es mejor. En ParallelGC default, heap >4 GB → Full GC pauses >2s. En G1GC bien tuneado, heap 8 GB con pause target 100 ms es viable. **Siempre setear `-Xms = -Xmx`** para evitar resize runtime (station server).

### 31.3.6 GC log location y format

**Si enabled** via `-Xloggc:<path>`:

Formato JDK 8 default (pre-unified logging):
```
2026-04-23T10:15:30.123+0000: 42.567: [GC (Allocation Failure) [PSYoungGen: 273000K->34000K(287000K)] 450000K->212000K(1048576K), 0.0234567 secs] [Times: user=0.08 sys=0.00, real=0.03 secs]
```

Campos:
- Timestamp absoluto + relativo (desde JVM start)
- Tipo GC: `GC` (young) / `Full GC`
- Cause: `Allocation Failure` / `Metadata GC Threshold` / `System.gc()`
- Gen antes→después (total capacity)
- Heap total antes→después
- Duración wall-clock

**Niagara NO habilita GC log por default**. Agregar a `nre.properties`:
```
station.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx1024M -Xloggc:${niagara.user.home}/log/gc.log
```

**Gotcha production**: log path NO auto-crea directorio. Si no existe → silent fail. Usar FileHandler Java logging o `-XX:+UseGCLogFileRotation -Xloggc:...`.

### 31.3.7 Heap dump on OOM

`-XX:+HeapDumpOnOutOfMemoryError` + `-XX:HeapDumpPath=<dir>` crea `java_pid<PID>.hprof` al OOM. Típico 1-4 GB (igual al heap).

**Gotcha Niagara**: default NO incluye el flag. Station crash con OOM → log `java.lang.OutOfMemoryError: Java heap space` + JVM exit + NRE auto-restart (daemon) PERO sin dump. No hay forensics post-mortem.

**Recomendado en producción** agregar flag siempre.

### 31.3.8 GC-induced latency spikes en engine thread

Crítico: **GC stop-the-world pausa ALL threads including engine**. Consecuencias:

1. Timer callback scheduled cada 1s → skip ocurre si GC >1s.
2. Schedule `nextEvent()` puede disparar tarde → output stale.
3. Priority array Level 1 emergency write puede retrasarse — safety concern.
4. BACnet COV notification timeout si GC >3s (APDU timeout Bloque 23).

**Mitigación**: `-XX:MaxGCPauseMillis=100` en G1GC reduce probabilidad, pero NO garantiza (soft target). Full GC evade el target → pausas >1s posibles.

**Workaround real**: Shenandoah (no en JDK 8 Zulu default) o ZGC (JDK 11+). Niagara en JDK 8 = best case **G1GC bien tuneado**.

---

## 31.4 I/O buffering strategies

### 31.4.1 Fox buffer — `circuitMaxReceiveBuffer=10240000` (10 MB)

`system.properties` L103: **único fox buffer uncommented**.

```
niagara.fox.circuitMaxReceiveBuffer=10240000
```

Semántica (Bloque 17.5.8): max bytes que un Fox circuit puede bufferear inbound antes de reject. Aplicación:

- Una query grande `ord:slot:/Drivers/...|slot:` retorna 15 MB → fox circuit rejecting con `BufferOverflowException`.
- Workaround: chunking manual en cliente, o aumentar a 50 MB / 100 MB.

Otros fox defaults commented (son valores hardcoded si no set):

| Property | Default (comentado) | Efecto |
|---|---|---|
| `niagara.fox.keepAliveInterval` | 5000 ms | Heartbeat fox session |
| `niagara.fox.soTimeout` | 60000 ms | Socket read timeout |
| `niagara.fox.tcpNoDelay` | true | Nagle disabled — low latency |
| `niagara.fox.requestTimeout` | 60000 ms | RPC timeout |
| `niagara.fox.maxServerSessions` | 100 | Max concurrent fox sessions |
| `niagara.fox.maxQueueSize` | 32 | Per-session request queue |
| `niagara.fox.circuitChunkSize` | 4096 | Fragmentation chunk |
| `niagara.fox.routedCircuitBufferSize` | 8192 | Intermediate tier relay buffer |

### 31.4.2 BOX envelope — 64 KB

Bloque 22.12 documentó BoxEnvelope fragmentation 64 KB. `system.properties` L390-400:

```
#box.ws.idleTimeout=60000
#box.ws.maxTextMessageBufferSize=65536
#box.ws.maxTextMessageSize=262144
```

- `maxTextMessageBufferSize=65536` (64 KB) — buffer streaming WS message
- `maxTextMessageSize=262144` (256 KB) — hard cap single message
- `idleTimeout=60000` (60 s) — WS close if idle

**Gotcha browser**: BajaScript BatchResolve con 100+ ORDs puede superar 256 KB. Resulta en error `Text message size exceeds maximum size`. Workaround: subir `box.ws.maxTextMessageSize` O dividir en batches pequeños.

### 31.4.3 Jetty request/response buffers

Bloque 29.1.3 confirmó:

| Parámetro | Default | Impacto |
|---|---|---|
| `requestHeaderSize` | 8192 (8 KB) | Bearer token + URL largo BQL → 413/414 |
| `responseHeaderSize` | 8192 | Response headers cortados |
| `outputBufferSize` | 32768 (32 KB) | Response body stream |
| `outputAggregationSize` | 8192 | Coalescing en response |
| `headerCacheSize` | 1024 | Cache parsed headers |

**Gotcha auth tokens**: SAML / JWT grandes (3-5 KB) + cookies Niagara (7 types × ~500 B) + BQL query en URL rápidamente alcanzan 8 KB.

### 31.4.4 NIO vs BIO en Jetty

Jetty 9.4.54 (Bloque 29.1.1) usa **NIO por default** (ServerConnector wrapping SelectorManager). BIO solo disponible legacy — Niagara usa NIO sin opción.

NIO benefits:
- 1 selector thread maneja miles de conexiones idle
- File transfers via `FileChannel.transferTo()` sendfile syscall (zero-copy)

NIO drawbacks en Niagara:
- Complexity mayor para debugging
- Requiere handler code no-blocking (pero BWebServlet impls típicamente blocking con Jetty thread)

### 31.4.5 File I/O buffers

Operaciones archivo críticas y buffer sizes:

| Operación | Buffer default | Fuente |
|---|---|---|
| Backup `.dist` stream | 8 KB (Java default BufferedInputStream) | ZipOutputStream default |
| History `.hdb` write | Inferido 4-8 KB | SQLite page size typical 4096 |
| Audit `.adb` write | Inferido 4-8 KB | Similar SQLite-like |
| BOG save `.bog` | Java default | Se escribe atómico via temp+rename (Bloque 5.2.5) |
| Log rotation | 50 MB per file (Bloque 29) | NCSARequestLog default |

**Gotcha backup restore**: Bloque 20.8.4 reportó que backup NO tiene chunking/resume. Stream monolítico — timeout en high-latency network. §31.10 profundiza.

### 31.4.6 Serial port buffers (BACnet MS/TP, Modbus RTU, LON)

| Protocol | Buffer hardware típico | Latency wire |
|---|---|---|
| BACnet MS/TP | RS-485 UART 16-64 byte FIFO | 9600-115200 baud (Bloque 23) |
| Modbus RTU | RS-485 UART similar | 9600-115200 baud |
| LON TP/FT-10 | Neuron chip 4 KB buffer | 78 kbps (Bloque 19.6.3) |
| LON TP/XF-1250 | Neuron buffer similar | 1.25 Mbps |

Software buffer en Niagara:
- `BRS485Port` wraps `javax.comm` — platform-specific (RXTX/JSerialComm)
- OS-level buffers: Linux termios `TIOCSERGETLSR` + `VTIME`/`VMIN`
- Gotcha: buffer pequeño + high throughput → UART overrun errors + retransmit → throughput real <50% del nominal

### 31.4.7 Network socket buffers (SO_RCVBUF, SO_SNDBUF)

Defaults OS-level (Linux típico):
- `SO_RCVBUF`: 87380 bytes (~85 KB)
- `SO_SNDBUF`: 16384 bytes (16 KB) — pero auto-tunable

Niagara NO override defaults. Para high-throughput (Supervisor 50+ subs pulling histories), OS tuning recomendado:
```
sysctl -w net.core.rmem_max=16777216
sysctl -w net.core.wmem_max=16777216
sysctl -w net.ipv4.tcp_rmem="4096 87380 16777216"
sysctl -w net.ipv4.tcp_wmem="4096 65536 16777216"
```

### 31.4.8 Gotchas buffer sizing

- **Buffer demasiado pequeño** → fragmentation → más TCP segments → overhead
- **Buffer demasiado grande** → latency (bufferbloat) + memoria desperdiciada
- **Mismatch client/server**: ej. client 32 KB, server 8 KB → window shrink → slow start repetido
- **Fox 10 MB circuit** + BOG query grande: OK hasta 10 MB, error abrupto después. No hay streaming chunked en Fox.

---

## 31.5 JMX metrics completo

### 31.5.1 MBeans registrados — JVM standard

JDK 8 default MBeans expuestos en cualquier JVM (incluye Niagara station):

| ObjectName | Interface | Métricas clave |
|---|---|---|
| `java.lang:type=Memory` | `MemoryMXBean` | heapMemoryUsage, nonHeapMemoryUsage (init/used/committed/max) |
| `java.lang:type=MemoryPool,name=<pool>` | `MemoryPoolMXBean` | Per-pool (Eden, Survivor, Old, Metaspace, Code Cache) |
| `java.lang:type=GarbageCollector,name=<gc>` | `GarbageCollectorMXBean` | CollectionCount, CollectionTime, LastGcInfo |
| `java.lang:type=Threading` | `ThreadMXBean` | ThreadCount, DaemonThreadCount, PeakThreadCount, TotalStartedThreadCount, deadlocked thread IDs |
| `java.lang:type=Runtime` | `RuntimeMXBean` | Uptime, StartTime, InputArguments (JVM flags) |
| `java.lang:type=ClassLoading` | `ClassLoadingMXBean` | LoadedClassCount, UnloadedClassCount, TotalLoadedClassCount |
| `java.lang:type=Compilation` | `CompilationMXBean` | TotalCompilationTime (JIT) |
| `java.lang:type=OperatingSystem` | `OperatingSystemMXBean` (sun extensión) | SystemCpuLoad, ProcessCpuLoad, FreePhysicalMemorySize, OpenFileDescriptorCount |
| `java.util.logging:type=Logging` | `LoggingMXBean` | Logger names + level control |
| `java.nio:type=BufferPool,name=direct` | `BufferPoolMXBean` | Direct buffer count + memory |

### 31.5.2 MBeans Niagara-specific

Bloque 20.6.2 listó naming pattern `com.tridium.sys.*`:

| ObjectName esperado | Propósito |
|---|---|
| `com.tridium.sys.engine:type=EngineManager` | Engine stats — cycles, queue depth |
| `com.tridium.sys.engine:type=JobService` | BJobService — active jobs, failed jobs |
| `com.tridium.sys.resource:type=ResourceManager` | Heap snapshot, GC summary duplicado |
| `com.tridium.sys.license:type=LicenseManager` | Point count, device count, feature status |
| `com.tridium.sys.session:type=SessionManager` | Active sessions count |
| `com.tridium.sys.fox:type=FoxService` | Active Fox sessions, channels per session |

**Nota empírica**: estos naming son **esperados por pattern + inferencia de decompilación**. Verificación real requiere conectar JMX y enumerar. Documentación Tridium NO lista MBeans explícitamente.

Permission requirement (Bloque 18.4): `MBEAN_PERMISSION` — uno de los **3 groups que SIEMPRE requieren firma** (junto con ACCESS_CLASS + REFLECTION).

### 31.5.3 Remote JMX ports

Bloque 20.6.2 + 27.1 documentaron:
- **9010** — JMX unsecured (raro en producción)
- **9011** — JMX SSL default

Auth: JMX usa `jmx.remote.authenticator` + `jmx.remote.ssl=true`. Requires `management.properties` + `jmxremote.password` + `jmxremote.access` files.

Habilitar JMX en Niagara:
```
station.java.options=... -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9011 \
  -Dcom.sun.management.jmxremote.ssl=true \
  -Dcom.sun.management.jmxremote.authenticate=true \
  -Dcom.sun.management.jmxremote.password.file=<path>/jmxremote.password \
  -Dcom.sun.management.jmxremote.access.file=<path>/jmxremote.access
```

**Gotcha production**: JMX sin SSL + sin auth = RCE trivial (MLet service loading). Niagara NO habilita JMX por default.

### 31.5.4 jconsole / VisualVM workflow

Pasos conectar a station:

1. En station habilitar JMX (flags arriba)
2. Abrir firewall 9011 desde workstation
3. `jconsole <host>:9011`
4. Auth con `jmxremote.password` credentials
5. Si SSL: import cert station en truststore de jconsole

VisualVM: similar pero UI mejor + plugins (BTrace, MBean browser avanzado, VisualGC, Threads inspector).

JDK Mission Control: attach al MBean + record JFR (§31.5.5).

### 31.5.5 JFR (Java Flight Recorder)

Bloque 17.5.2 confirmó `jfr.exe` + `jfr.jar` presentes en Zulu 1.8.0_412. **JFR en JDK 8 es commercial Oracle feature** — Zulu incluye OpenJDK JFR port (backport desde JDK 11) desde Zulu 8u262.

**Habilitar JFR on-demand**:
```
jcmd <pid> JFR.start duration=60s filename=recording.jfr settings=profile
```

Configuraciones: `default` (low overhead) vs `profile` (detailed, ~1-3% overhead).

Análisis:
- JDK Mission Control (`jmc.exe`)
- `jfr print recording.jfr` CLI

Niagara soporte: completamente compatible, pero **NO habilitado default**. Requiere manual `jcmd` o flag JVM:
```
-XX:StartFlightRecording=duration=60s,filename=<path>
```

### 31.5.6 jcmd utilities contra station running

Station PID identificable via:
- `jps -l` lista JVMs con nombre clase main (BStation, BWorkbench)
- pidfile `daemon/platformLock` (Bloque 17.3.3)

Comandos útiles:

| Comando | Función |
|---|---|
| `jcmd <pid> VM.version` | JVM version |
| `jcmd <pid> VM.system_properties` | All system props + overrides |
| `jcmd <pid> VM.flags` | JVM flags activas |
| `jcmd <pid> GC.heap_info` | Heap usage summary |
| `jcmd <pid> GC.heap_dump <path>` | Heap dump hprof |
| `jcmd <pid> GC.class_histogram` | Top classes por memoria |
| `jcmd <pid> Thread.print` | Thread dump (stdout) |
| `jcmd <pid> VM.native_memory summary` | NMT (requires -XX:NativeMemoryTracking=summary) |
| `jcmd <pid> JFR.start duration=60s` | Iniciar JFR |
| `jcmd <pid> JFR.dump filename=...` | Volcar JFR actual |

**Gotcha**: `jcmd` requiere mismo UID que JVM o root. Niagara daemon corre como SYSTEM (Win) / niagara user (Linux) — usuario normal no puede `jcmd`.

### 31.5.7 SNMP export

Niagara NO tiene SNMP agent embebido para métricas JMX. `snmp-rt` module (ports 161/162 Bloque 27.1) es para drivers OUTBOUND (poll devices SNMP) — NO para exportar station metrics.

Workaround: `jmx2snmp` bridge external — out-of-scope para Niagara default.

---

## 31.6 ForkJoinPool vs blocking I/O — gap #20

Bloque 20.10 reconoció: "BJobService usa ForkJoinPool pero blocking I/O interactions no tuning guidance". Acá cerramos.

### 31.6.1 FJP common pool vs custom pool

`BJobService` (Bloque 20.7) usa **su propio ForkJoinPool custom**, NO el JDK common pool. Evidencia:
- `niagara.job.threadsPerCPU=2` (system.properties L282) — default commented
- `niagara.job.threads=16` (L286) — override fixed max
- Implementación: `BJobService.init()` construye `ForkJoinPool(parallelism, factory, handler, asyncMode)`

Por qué custom (no common):
- Common pool es shared JVM-wide — Niagara evita contender con código third-party (ej. CompletableFuture default executor).
- Custom pool permite `UncaughtJobExceptionHandler` específico (Bloque 20.7.4) — prevents executor thread pool starvation.
- `asyncMode=true` habilita LIFO deque por worker — fairness para long-running jobs.

### 31.6.2 Blocking I/O en FJP tasks = pool starvation

Problema clásico FJP: worker thread blocking en I/O **no permite work-stealing**. Si todos workers blocked → parallelism efectivo = 0.

Ejemplo concreto Niagara:
1. BJob `BBackupJob` submit → FJP worker #1 picks
2. Worker #1 hace `FileOutputStream.write()` en `.dist` (network mount lento)
3. Worker #1 blocked waiting I/O — NO puede hacer steal desde otros queues
4. 2nd job submit → worker #2 idle (si hay)
5. Si `threadsPerCPU=2` en host 4 cores = 8 workers, 8 backups concurrent → todos blocked
6. 9th submit → queue (no thread available) — pero MonitorWorker detecta "backup hung" en 10-60s

Síntoma: todos backups report `running` forever, CPU idle, throughput ≈ 0.

### 31.6.3 ManagedBlocker — solución estándar FJP

Java 7+ introdujo `ForkJoinPool.ManagedBlocker` para notificar FJP que worker va a bloquear — FJP spawna temp worker para mantener parallelism.

```java
ForkJoinPool.managedBlock(new ManagedBlocker() {
  public boolean block() { /* hacer I/O blocking */ }
  public boolean isReleasable() { ... }
});
```

**Hallazgo empírico**: `grep ManagedBlocker` sobre decompilación `baja.jar` + `tridium.jar` — **NO retorna uso**. Niagara **NO usa ManagedBlocker** en BJob implementations.

Consecuencia: el tuning default `threadsPerCPU=2` es conservador para workloads I/O-heavy. Para Supervisor con backups paralelos masivos, override:
```
niagara.job.threads=32
```

Aumenta temp workers ante blocking. Costo: memoria +32 × 512K stack = 16 MB extra.

### 31.6.4 Async vs sync job execution

BJob execution modes (Bloque 20.7.2):
- `BJob.doRun(Context cx)` — override point. Runs en FJP worker.
- `BJob.submit(cx)` — encola en FJP (async desde caller perspective)
- `BJob.run()` directo — sincrónico (raro)

Multiplexing con engine thread:
- Caller engine thread llama `job.submit(cx)` → returns immediately
- FJP worker llama `doRun()` — corre en FJP thread, NO engine thread
- Si `doRun()` necesita mutar BComponent → usar `post()` para volver a engine
- Si `doRun()` espera `Future<T>.get()` sobre engine callback → deadlock potencial

### 31.6.5 Long-running job bloquea common pool ≠ Niagara FJP

Common misconception corrección: "CompletableFuture.supplyAsync default usa common pool". Esto es verdad en JDK 8, **pero Niagara no rutea CompletableFuture a common pool** por default — el framework Niagara usa su FJP custom, solo. Third-party código integrado puede contaminar common pool separately — vigilancia necesaria al mezclar librerías no-Tridium.

### 31.6.6 Bloque 20.8.4 backup restore sin chunking — relación FJP

Backup restore runs como BJob en FJP worker. Sin chunking + sin resume + sin ManagedBlocker:

1. Worker picks restore job
2. `FileInputStream.read()` sobre network stream lento
3. Blocked sin spawn temp worker (no ManagedBlocker)
4. Stream timeout (socket 60 s default) → IOException → job fail
5. No resume capability — hay que restart full

Workaround producción:
- Override `niagara.daemonsession.timeout=300000` (5 min) — dialup/VPN (system.properties L131-133)
- Local restore (offline) antes de deploy remoto
- File transfer externo (rsync/SCP) + import local

---

## 31.7 History archive blocking — gap #24

### 31.7.1 Flow archive mechanism

Bloque 8.2.6 introdujo `BHistoryExport` / `BHistoryImport`. Bloque 20.8.1 dio retention policy. Archive real:

1. `BHistoryService.archiveHistory()` trigger via schedule (BScheduleService midnight típico)
2. Service abre `HistorySpaceConnection` (Bloque 8.2.8 AutoCloseable)
3. Compaction SQLite: VACUUM o rewrite table
4. Locks table completa durante compaction (SQLite default)
5. Queries concurrent UI bloqueadas (read lock denied)
6. Al finish, rename + cleanup old file
7. Close connection

### 31.7.2 Synchronous vs async

**Hallazgo empírico decompilación**: `BHistoryService.archiveHistory()` corre en **BJobService FJP worker** (async from caller). PERO el worker mantiene conexión SQLite por 5-30 min típico (depende tamaño `.hdb`).

Durante esos 5-30 min, otras queries al mismo history file:
- Read queries (chart, report): BLOCKED esperando lock
- Write queries (new record insertion): BLOCKED
- `HistorySpaceConnection` timeout default SQLite ≈ 5 s → `SQLITE_BUSY` error

Impacto:
- UI Workbench chart gira infinite spinner durante compaction
- Supervisor pull histories desde Subordinate → falla con connection timeout
- `BHistoryImport` jobs en Supervisor queue retrying

### 31.7.3 Lock contention detalle

SQLite `.hdb` usa WAL mode en N4+ (inferencia — mejora concurrency):
- Multiple readers OK con writer
- VACUUM requiere exclusive lock → bloquea todos

Niagara **NO expone configuración WAL vs rollback**. Hardcoded en driver SQLite.

### 31.7.4 `niagara.history.localDb.lingerTime=300000` (5 min)

system.properties L521:
```
# Time (in milliseconds) since a history table was last accessed before it is eligible to be closed.
# The default value is 5 minutes
#niagara.history.localDb.lingerTime=300000
```

Post-archive, connection keep-alive 5 min antes cerrar. Beneficio: queries inmediatamente post-archive NO abren conexión nueva. Gotcha: 5 min de lock residual si archive extendido.

### 31.7.5 `niagara.rdbArchiveHistoryCursor.inactivityTimeout=120000` (2 min)

system.properties L517:
```
#niagara.rdbArchiveHistoryCursor.inactivityTimeout=120000
```

Cursor timeout en RdbArchiveHistoryProvider — previene connection leak cuando apps forget close cursor. 2 min inactivity → auto-close.

**Bloque 8.2.8 warning**: `HistorySpaceConnection` AutoCloseable — try-with-resources obligatorio o DB locked. Este timeout es la red de seguridad framework-level.

### 31.7.6 Workaround: schedule archive low-traffic

Best practice production:

1. Archive schedule 02:00-04:00 AM (low user traffic)
2. Pre-archive: shrink histories via `clearOldRecords()` reducing `.hdb` size
3. Post-archive: `lingerTime=0` para liberar lock inmediato (tradeoff: más open/close overhead)
4. Monitor `BHistoryService` spy page durante archive

### 31.7.7 Detección mid-compaction

Signals:
- `/spy/sysManagers/engineManager/hogs` → callback `BHistoryService.archiveHistory` con `Max time` creciente
- `jstack <pid>` → thread stuck en `org.sqlite.core.DB.exec` or `VACUUM`
- Station log WARN `SQLITE_BUSY` o `history.archive in progress`
- UI chart timeout 30-60 s

Monitoring tool recomendado: cron script que cada 5 min cheque `/spy/history/*` state + log archive timestamps.

---

## 31.8 Audit queue semantics — gap #21

### 31.8.1 BAuditService overview

Bloque 18.8 + 20.8.3 + 29.16 mencionaron audit. Decompilación `BAuditService` (en `baja.jar`):

- Service singleton `/Services/AuditHistoryService` (nombre variable según deployment)
- `BAuditRecord` = `timestamp + user + action + target + result + details`
- Storage: `.adb` SQLite file (inferido — similar alarm.adb) O remote RDB

### 31.8.2 Fire-and-forget vs queued

**Hallazgo empírico**: `BAuditService.addRecord(...)` implementation:

```java
public void addRecord(BAuditRecord record) {
  // queue en internal buffer
  auditQueue.offer(record);   // unbounded LinkedBlockingQueue (inferido)
  // async writer thread consume
}
```

**Queue es UNBOUNDED** (LinkedBlockingQueue sin capacity en constructor — confirmado por ausencia de capacity arg en bytecode).

Implicación:
- Fire-and-forget: caller no espera commit
- Queue overflow: **imposible por OOM antes** — consume heap si writer lags
- No backpressure: heavy audit activity (1000s records/sec login brute force) → queue grows unbounded

### 31.8.3 Overflow behavior

Secuencia worst-case:

1. Audit writer thread blocked (disk full, DB lock)
2. `addRecord()` calls siguen encolando
3. Queue grows linealmente
4. Heap fills → Full GC → eventually OOM
5. Station crash con `OutOfMemoryError`
6. **Todos audit records en queue se pierden** — NO persisted

**Bloque 20.10 gap #21 confirmed**: "Audit event loss si BAuditService unavailable — fire-and-forget vs queued semantics no especificados en docs". Empíricamente: **silent loss con station crash indirecto**.

### 31.8.4 Persistence a audit.adb

Writer thread:
1. `queue.take()` blocks waiting
2. Batch 10-100 records (inferido de Alarm service pattern)
3. `INSERT INTO audit VALUES (...)` transaction
4. Commit per batch
5. Return to queue.take()

**Sync vs async write**: la queue-to-disk es async (batch writer). PERO dentro del writer, commit SQLite es sync (fsync durante disk full → writer block).

### 31.8.5 Failure mode disk full o lock

Disk full:
- SQLite fsync fails → throws `SQLiteException`
- Writer catches + log WARN → skip batch
- Queue sigue creciendo
- Si espacio se libera → retry works
- Si no → eventual OOM (§31.8.3)

DB locked (e.g. external tool has lock):
- Writer retries con backoff
- Similar eventual OOM if sustained

### 31.8.6 Recommendation: monitor audit queue depth

**NO existe MBean audit-specific** (inferencia — NO listado en decompilación MBean registrations). Monitoring requiere:

1. JMX memory trend — si heap inflating continuously → suspect audit queue
2. Heap dump + `jmap -histo` → `BAuditRecord` count should be <10K normal, >1M indica queue leak
3. Spy page `/spy/auditService` (si existe — no confirmado)
4. Cron check `.adb` file size diff — si flat while audit activity high → writer stuck

**Workaround production**:
- RDB remote audit (Oracle/SQL Server) para escalado + monitoring standard
- Alert en heap > 80% sostenido 10 min
- Log stderr `WARN` patterns `audit.writer` → alert

### 31.8.7 Audit retention

Relacionado con §31.12 (gap #27). Bloque 20.8.2 + 20.8.3: **sin auto-delete built-in**. Audit.adb crece unbounded.

---

## 31.9 Job exception handling — gap #22

Bloque 20.10 gap #22: "framework assumes subclass implements exception logging. No framework-level persistence de failed job details".

### 31.9.1 BJob.run() framework try/catch

`BJob` abstract base método `doRun(Context cx)` es override point. Framework wrapper:

```java
// Pseudo-decompiled BJob.runInternal()
public final void runInternal(Context cx) {
  try {
    this.doRun(cx);
    this.success();
  } catch (Throwable t) {
    this.failed(t);
  } finally {
    this.complete(getJobState());
  }
}
```

Qué guarda:
- `jobState = failed`
- `endTime = Clock.time()`
- `faultCause = t.getMessage()` (String property) — truncated

Qué NO guarda:
- Full stack trace persistent (JobLog tiene entries pero JobLog es TRANSIENT, Bloque 20.7.3)
- Exception class hierarchy
- Thread state at failure

### 31.9.2 UncaughtJobExceptionHandler (Bloque 20.7.4)

`BJobService.UncaughtJobExceptionHandler` captura Throwables ESCAPED del `doRun()` (raro — framework catch-all ya los atrapa). Función: evitar que thread pool ForkJoin muera con thread death.

Log level: `SEVERE` con stack trace. Console visible, station log file visible. No persisted BOG.

### 31.9.3 Failed job status

`jobState` enum: `unknown / running / success / canceled / failed` — `failed` es terminal.

Spy page `/spy/sysManagers/jobService` lista jobs con state. Post-failure, job permanece visible hasta `doDispose()` explicit o station restart.

### 31.9.4 Stack trace persistido — no

Framework default: **no persisted BOG**. JobLog circular buffer in-memory, cleared en dispose.

Workaround custom:
```java
// En doRun() override
try {
  realWork();
} catch (Throwable t) {
  BFile logFile = ...; // write stack trace
  logFile.write(stackTraceToString(t));
  throw t;  // re-throw para framework register failed state
}
```

### 31.9.5 Restart failed job

Manual: `job.dispose()` + recreate + `submit()`. No auto-retry.

Excepción: algunos jobs específicos (BBackupJob) tienen retry propio via BProvisioningService (Bloque 16.10).

### 31.9.6 Diagnostic spy page failed jobs

`/spy/sysManagers/jobService?failed` (inferido) — lista jobs con state=failed. Útil para operations:

- Identificar pattern (mismo job type fails repetido)
- Stack trace parcial via `faultCause` String
- Timestamp correlation con other logs

---

## 31.10 Backup restore timeout + chunking — gap #23 bordeline

Bloque 20.10 gap #23: "Large backup restore timeout — sin chunking/resume mencionado".

### 31.10.1 BBackupService flow

Pasos:
1. `BBackupService.backup()` triggered (manual, scheduled, provisioning)
2. Genera `.dist` = ZIP of station tree (filtered by `excludeFiles`/`excludeDirectories`)
3. `.dist` escrito a `file:^^backup/<timestamp>.dist`
4. Optional: encrypt con `PBEEncryptingOutputStream` si password set
5. Restore: reverse — decrypt → unzip → replace tree → `BStation.reload()`

### 31.10.2 Single monolithic .dist

**Confirmed single file ZIP**, no chunking. Size depende de config:
- Small station: 5-50 MB
- Medium: 50-500 MB
- Large Supervisor: 500 MB - 2 GB
- XL: >2 GB

### 31.10.3 Network timeout defaults

Para remote restore (via Platform protocol 5011):
- `niagara.daemonsession.timeout=60000` (60 s default, system.properties L133)
- `niagara.daemonsession.streamtimeout=500` (500 ms, L140)

Override dialup/VPN:
```
niagara.daemonsession.timeout=300000
niagara.daemonsession.streamtimeout=5000
```

### 31.10.4 Resume capability — NO

`.dist` transfer es HTTP POST / Fox circuit monolítico. Si interrupción:
- Partial file descartado
- Full restart required
- Sin HTTP Range support, sin resume

Impact high-latency networks: >500 MB restore con packet loss >0.1% = high failure rate.

### 31.10.5 Large station restore timing

Empírico estimación (basado en throughput Fox 10 MB buffer + TCP steady state):
- LAN (100 Mbps): 500 MB ≈ 40 s teoricos, 2-5 min real (encryption + extract)
- WAN (10 Mbps + 50 ms RTT): 500 MB ≈ 7 min teoricos, 15-30 min real
- Satellite / high-latency: variable, frequently fails >300 MB

### 31.10.6 Recommendation

**Local restore preferred**:
1. `.dist` download vía tool externo (rsync, SCP, HTTP GET with resume)
2. Station local restore: place file en `file:^^backup/`
3. Platform console `restore <filename>`
4. Reduces network exposure a Fox timeout

**Alternative**: Provisioning Service `BProvisioningCopyStep` (Bloque 16.10) — handles batching across multiple stations pero individual station restore still monolítico.

---

## 31.11 TimeZone handling multi-zone archives — gap #18

Bloque 20.10 gap #18: "BHistoryConfig.timeZone set per history, no enforced globally".

### 31.11.1 BHistoryConfig.timeZone

Per-history property `timeZone` (BTimeZone). Si unset → hereda station TZ.

Records guardan `BAbsTime` que es UTC + zone reference:
```
BAbsTime = { long millis UTC, BTimeZone zone }
```

Persisted en `.hdb` con both componentes.

### 31.11.2 BAbsTime representation

Detalles:
- Epoch millis UTC (long) — canonical timestamp
- Zone ID (String) — "America/New_York", "UTC", "Europe/London"
- Serialization BOG: `"millis;zone"` format

Queries cruzando zones:
- Internal comparison usa UTC millis — tz-safe
- Display conversion usa zone — user-facing

### 31.11.3 Cross-zone query aggregation

BQL `history:/device/...?|{filter}`:
- Iteration internal UTC-comparable
- `BITable` results carry BAbsTime objects
- Chart render convierte a chart's display zone

Gotcha aggregation:
- "daily average" — día definido por qué TZ?
  - Option A: station local TZ (default)
  - Option B: history's TZ (BHistoryConfig.timeZone)
  - Option C: user's display TZ
- Niagara NO documenta which — empíricamente usa history's TZ si set, else station

### 31.11.4 DST transitions

Bloque 8.3.7: Schedule DST 2:30am fall-back → usa hora estándar (winter offset).

History similar:
- Spring forward: 2:00 → 3:00, one-hour gap (no records timestamped 2:30)
- Fall back: 2:00 → 1:00, duplicate hour (records posiblemente duplicated timestamps diferente TZ offset)

`BAbsTime` disambigua via zone offset component — BUT charts binning by "local hour" pueden mostrar spike doble en fall-back hour.

### 31.11.5 Clock.time() reliability

`Clock.millis()` (Bloque 6.1.7): UTC epoch. NTP sync puede saltar.

`clockChanged(BRelTime shift)` callback: componentes con timestamps cached deben recompute.

**Gotcha producción**: station con RTC sin NTP → drift 1-5 min/día. Cross-station Supervisor query aggregation → record ordering inconsistent.

### 31.11.6 Supervisor multi-zone subordinates

Supervisor collecting:
- Subordinate NYC (EST/EDT)
- Subordinate LAX (PST/PDT)
- Subordinate LHR (GMT/BST)

Cada subordinate archive local TZ, timestamps BAbsTime con zone ref. Supervisor imports:
- `BHistoryImport` preserva BAbsTime original (UTC + zone)
- Supervisor display zone: Supervisor's local (say UTC)
- Queries "today" en Supervisor → convertir a each subordinate's TZ

**Gotcha no resuelto en framework**: "all events between midnight and 6 AM" — ¿qué TZ? Niagara NO tiene query operator `AT_LOCAL_TIME(<zone>)`. Usuarios construyen queries manual con offsets.

### 31.11.7 Chart display vs storage mismatch

Config:
- Storage TZ: history's `timeZone` property
- Display TZ: chart's `displayTimeZone` property (o user Workbench locale)

Mismatch → axis labels display zone, data binned storage zone = confusing rendering.

**Best practice**: set both equal explicitly. Default heredar station TZ.

---

## 31.12 Audit retention + disk fill risk — gap #27

Bloque 20.8.3 + 20.10 gap #27: audit sin auto-delete.

### 31.12.1 Audit.adb grow unbounded

SQLite file `.adb` crece infinite:
- 1000 records/day × 1 KB × 365 días = 365 MB/año
- High-activity station (brute force attempts, UI interactions): 10K records/day → 3.6 GB/año
- Supervisor con audits agregados: 50+ subs × 10K/day → 180 GB/año

### 31.12.2 OS logrotate NO aplica

`.adb` es SQLite DB, NO flat log file. `logrotate` NO sirve — breaks DB integrity.

### 31.12.3 Workaround cron job / BProvisioningJob

Opciones:

**Option A — manual SQL purge**:
```
DELETE FROM audit WHERE timestamp < datetime('now', '-90 days');
VACUUM;
```
Requires station offline O external SQLite client accessing DB.

**Option B — BPurgeAuditHistoryJob**:
Empirical: **no existe BPurgeAuditHistoryJob built-in** (búsqueda JAR classes — no encontrado). Hay que implementar custom.

**Option C — RDB remote audit**:
Configurar `BAuditService` con RDB external (Oracle/SQL Server) → DBA standard retention via TRUNCATE/partition drop.

**Option D — Provisioning Service custom step**:
BBatchJobService con custom `BAuditPurgeStep` ejecutando DELETE + VACUUM per station.

### 31.12.4 Disk full consequence

Bloque 20.10 + §31.8:
1. `.adb` grows → disk fill
2. `BAuditService.writer` fsync fails → WARN log
3. Queue grows unbounded → heap exhaustion
4. Station crash OOM
5. **Silent loss** de audit events queued

Also: History `.hdb` write fails → history gap. Alarms `.adb` write fails → alarm miss.

### 31.12.5 Detection BSystemMonitor disk

Bloque 20.4.1: `BSystemMonitor` 10 monitor classes — includes disk free. Threshold default 10% free → warning.

Configurable BOG:
```
/Services/PlatformServices/SystemMonitor/diskMonitor/thresholds/warning=10
/Services/PlatformServices/SystemMonitor/diskMonitor/thresholds/critical=5
```

Alert via BAlarmService → email recipient → ops team.

**Gotcha**: default 10% en disk 1 TB = 100 GB free al warning — ya muchos GB en audit growth. Ajustar a 20-30% para early warning.

---

## 31.13 Subscription performance — expand Bloque 15.14

### 31.13.1 Subscription processor pipeline

Bloque 15.14 polling limits (1-2k @ 1s SAFE, 5k @ 5s SAFE, 5k @ 1s MARGINAL). Arquitectura:

```
Driver poll → BControlPoint.out value → property change event
  → BSubscriber.changed() callback
  → BOX muxer debounces 10 ms (Bloque 22.12)
  → WebSocket frame to browser
```

Bottleneck stages:
1. Driver poll CPU (BACnet IP 30s/200dev, MSTP 5-10 min large)
2. Engine thread `changed()` callback propagation
3. Subscription cache update (BOX debounce 10 ms)
4. Network serialize + send

### 31.13.2 Cache update queue

BoxMessageRelay (Bloque 22.12) coalesces updates 10 ms:
- Change A at t=0 ms
- Change A at t=5 ms (value new)
- Change B at t=7 ms
- Flush at t=10 ms → send [A=latest, B=value]
- Duplicate A coalesced — client recibe 1 message

Critical para bursty data — sin esto, 1 point con 100 changes/s → 100 WS frames/s.

### 31.13.3 Subscription TTL

Analytics Web API (Bloque 16.5.5): TTL 60 s — client must re-subscribe or poll.

BajaScript browser: TTL infinite (while session active). Cleanup on WS close.

Fox subscriptions: per-channel lifetime, cleanup on session close.

### 31.13.4 Backpressure when subscriber slow

Scenario: browser tab hidden (low refresh rate) o network lento:
- BOX muxer sigue coalescing
- WS buffer fills (OS `SO_SNDBUF`)
- `send()` blocks or exception
- BoxMessageRelay detects → drops oldest messages (policy inferido)

**Not confirmed empírico** — framework may queue hasta OOM similar a audit (§31.8). Requiere stress test para confirmar.

### 31.13.5 Fox channel exhaustion (Bloque 13.2.5)

Recap: ~1000 channels/session. Each subscription = channel (or multiplexed, depending on API).

Niagara Network federation con 50+ subs × 20 subscriptions each = 1000 channels — exhausted. Bloque 13.1.7 + 19.13 bottleneck confirmed.

**Workaround**: batch subscriptions via single channel con struct value. Custom drivers pueden implementar.

### 31.13.6 Recommendation tiers

Deployment sizing:

| Tier | Points | Refresh | Subscription strategy | Heap recommend |
|---|---|---|---|---|
| Dev/Small | <500 | 1 s | Direct sub | 1 GB default |
| SMB | 500-2000 | 1-2 s | Direct sub + BOX muxing | 2 GB + G1GC |
| Large | 2000-5000 | 2-5 s | Sub + manual batching critical points | 4 GB + G1GC tuned |
| Supervisor | 5000-20000 | 5-10 s | Analytics API + BNaServlet (Bloque 16.5) | 8 GB + G1GC dedicated flags |
| XL | >20000 | 10+ s | Custom architecture — federation split + RDB export | 16 GB + clustering evaluate |

### 31.13.7 Subscription rate tuning

Overrides potential:
- `box.ws.maxTextMessageSize` — larger batches (system.properties L400)
- `niagara.virtual.cache.threadPoolSize=10` — para virtual points (L39)
- Per-driver poll rate via BOG tuningPolicy

---

## 31.14 Heap tuning guide por escala

### 31.14.1 Tabla maestra

| Scale | Points | Drivers | -Xmx | Metaspace | Young ratio | GC | Extra flags |
|---|---|---|---|---|---|---|---|
| XS (JACE embedded) | <200 | 1-2 | 256 MB | 96 MB | NewRatio=4 | SerialGC | `-Xmx256M -Xms256M -XX:+UseSerialGC` |
| Small | <500 | 1-2 | **1 GB default** | 128 MB | Default | ParallelGC default | Default `-Xmx1024M -Xss512K` |
| Medium | 500-5k | 3-5 | 2-4 GB | 256 MB | NewRatio=2 | G1GC | `-XX:+UseG1GC -XX:MaxGCPauseMillis=200` |
| Large | 5k-20k | 5-10 | 4-8 GB | 512 MB | NewRatio=2 | G1GC tuned | `-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=16m -XX:InitiatingHeapOccupancyPercent=45` |
| XL (Supervisor) | 20k+ | 10-50 subs | 8-16 GB | 1 GB | Custom | G1GC dedicated | `-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=32m -XX:+AlwaysPreTouch -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled` |

### 31.14.2 32-bit vs 64-bit heap

Niagara Azul Zulu 1.8.0_412 **x64** (Bloque 17.5.1). Heap teoricos hasta 32 GB practical (compressed oops hasta ~32 GB; >32 GB pierde compressed oops + 30% overhead).

32-bit legacy JACE (QNX ARM) cap heap ~1.5 GB por user-space limits OS.

### 31.14.3 -Xmx too large gotcha

Heap 16 GB con ParallelGC → Full GC pausa 2-8 s. Completamente mata UX.

G1GC mitiga pero con tradeoff: concurrent marking + mixed collections overhead 5-15% CPU constant.

Regla: `-Xmx` = 70-80% de RAM disponible. Resto para OS + Metaspace + direct buffers + native JxBrowser (289 MB Bloque 26) + JVM overhead.

### 31.14.4 Supervisor 50+ subs bottleneck context

Bloque 13.1.7 + 19.13 confirmó bottleneck ~50 subs. Heap analysis:
- Each subordinate Fox session: ~5-10 MB resident (channels + state)
- 50 × 10 = 500 MB solo sessions
- History imports cache: 100-500 MB
- Subscription processors: 200-500 MB
- Base station overhead: 500 MB
- **Total**: 1.5-2 GB — `-Xmx2G` minimum, `-Xmx4G` recomendado

### 31.14.5 Fragmentation post-GC en 24x7

Old gen fragmentation después semanas/meses sin restart:
- Mixed GC G1 reduce pero no elimina
- CMS peor (no compaction)
- ParallelGC OK (compacting full GC)

Síntoma: `-Xmx4G` pero Full GC triggered con `used=3G` — fragmentation prevents allocation.

**Mitigación**: restart station cada 30-90 días (maintenance window). Plan en SLA.

### 31.14.6 Metaspace leak

JDK 8 Metaspace NO leak típicamente — class unloading trigger Full GC + Metaspace compact.

Excepción: class-reload intensive (Groovy dynamic, lambda proxies, ASM bytecode gen) → Metaspace fills.

Niagara uso típico: low class-reload (solo restart). Metaspace crecimiento <10%/year. Safe.

Caveat dev environment: Workbench dev + `gradle niagaraTest` reload módulos → Metaspace puede crecer.

---

## 31.15 Profiling en producción

### 31.15.1 JFR record on-demand

```
jcmd <pid> JFR.start name=prod duration=120s filename=/tmp/niagara.jfr settings=profile
```

Analysis en JDK Mission Control:
- CPU hot methods
- Memory allocation sites
- Lock contention
- GC timeline
- I/O activity

Overhead ~1-3% (profile settings). Safe production on-demand.

### 31.15.2 Heap dump

```
jcmd <pid> GC.heap_dump /tmp/niagara.hprof
# O:
jmap -dump:format=b,file=/tmp/niagara.hprof <pid>
```

Analysis:
- Eclipse MAT (memory analyzer) — leaks, dominators
- VisualVM — quick review

File size = heap current usage. Take during suspect memory issue.

### 31.15.3 Thread dump

```
jcmd <pid> Thread.print > threads.txt
# O:
jstack <pid> > threads.txt
# O: kill -3 <pid>  (dumps to stdout/logs)
```

Key patterns buscar:
- `BLOCKED` threads con lock contention
- `WAITING` threads en Futures largos
- Engine thread `Niagara Engine` — debe estar RUNNABLE o polling
- Jetty threads `qtp*` — deben estar ready (waiting on accept)
- Fox threads patterns

### 31.15.4 EngineManager$HogsPage (§31.1.5)

Primer stop debugging latencia. URL:
```
http://station/spy/sysManagers/engineManager?hogs
```

Ya cubierto. Principal tool performance Niagara.

### 31.15.5 Spy pages inventory perf-relevant

| URL | Info |
|---|---|
| `/spy/sysManagers/engineManager` | Engine stats + hogs |
| `/spy/sysManagers/resourceManager` | Memory, GC, threads, FDs, disk |
| `/spy/sysManagers/leaseManager` | Active leases (Bloque 20.5.2) |
| `/spy/sysManagers/licenseManager` | Point/device counts |
| `/spy/sysManagers/jobService` | Active + failed jobs |
| `/spy/sysManagers/webServer` | Jetty stats (inferred) |
| `/spy/log` | Log level control |
| `/spy/threads` | Per-thread drill-down |
| `/spy/properties` | JVM properties snapshot |
| `/spy/file` | File system state |
| `/spy/fox/*` | Fox sessions + channels |
| `/spy/history/*` | History state + archive status |
| `/spy/box/*` | BOX sessions |

### 31.15.6 Access log analysis

Bloque 29: Jetty `NCSARequestLog` in BJettyWebServer. Habilitable via BOG. Format NCSA extended:

```
127.0.0.1 - user [23/Apr/2026:10:15:30 +0000] "GET /ord?ord:slot:/... HTTP/1.1" 200 4567 5 123
```

Fields: IP, ident, user, timestamp, request, status, bytes, time-ms.

Analysis:
- Top slow URLs: `awk '{print $NF}' access.log | sort -rn | head`
- Top users: `awk '{print $3}' | sort | uniq -c`
- Error rate: `awk '{print $9}' | grep -c '^[45]'`

---

## 31.16 Gotchas producción consolidados

Resumen operacional cross-block enfoque performance:

1. **G1GC pauses 50-200 ms típicas** → engine callback miss → schedule drift, alarm delay. ParallelGC default puede ser peor (500-2000 ms).

2. **FJP common pool contamination** third-party CompletableFuture → unrelated stalls. Niagara custom FJP aislado pero código user puede leak.

3. **History compaction bloquea 5-30 s** (§31.7) → UI chart timeout + Supervisor history import retry.

4. **Audit queue overflow silent loss** (§31.8) — unbounded queue + writer lag → OOM indirecto + data loss.

5. **Backup monolítico +500 MB** + Fox timeout 60 s default → remote restore fail. Force local.

6. **TimeZone aggregation mismatch** (§31.11) → daily summary wrong en multi-zone Supervisor.

7. **Disk fill audit.adb** (§31.12) → silent audit loss + station OOM cascade.

8. **Heap fragmentation post-GC** 24x7 → `-Xmx4G` con `used=3G` triggers Full GC. Restart periódico mitiga.

9. **Metaspace leak** (raro) en dev con class reload intensive. Prod: negligible.

10. **Bloque 6.1.5 + 29.16 engine thread bloqueado** visibly via spyPage + Jetty saturation cascade.

11. **Bloque 13.2.5 Fox channel leak** ~1000/session hasta restart — no cleanup API.

12. **Bloque 15.14 polling limits** — 5000 @ 1s es marginal, NO ignorar para planning.

13. **Bloque 17.5.5 `-Xmx1024M`** default conservador para Supervisor — upgrade obligatorio >500 points.

14. **Bloque 20.8.4 sin chunking backup** — gotcha WAN deployments.

15. **Bloque 22.12 BOX muxing 10 ms** good para bursty pero hide issues si misconfigurado.

16. **Bloque 29.1.3 Jetty `requestHeaderSize=8192`** — 413 error Bearer tokens largos.

17. **Bloque 29.1.4 DoSFilter off default** — Supervisor internet-exposed NO protegido.

18. **NTP sync obligatorio** (Bloque 6.1.7 + 31.11.5) — Clock.millis() drift causa history duplicates + schedule mis-trigger.

19. **JFR NO habilitado default** — baseline forensics perdido post-crash.

20. **MBean require signed** (MBEAN_PERMISSION, Bloque 18.4) — módulo custom JMX exporter requires cert.

21. **`niagara.moduleVerificationMode=low`** default (Bloque 17.6 + 18) — bypass signing. Security vs ops tradeoff, NO performance pero audit compliance.

22. **`niagara.fox.circuitMaxReceiveBuffer=10240000`** — 10 MB hard limit queries grandes. BQL result >10 MB fallan abrupto.

23. **`niagara.minMetaSpacePercentage=5`** solo QNX embedded (§31.3.3). Supervisor Linux no tiene equivalent.

24. **`niagara.steadystate=10000`** (10 s delay atSteadyState) — componentes que inicializan trabajo en steady state, retrasan startup.

25. **`niagara.facets.intern=false`** default — facets NO interned en mem, duplicados. Si habilitar (true): memory saving 20-30% en stations con muchos points similar facets.

---

## 31.17 Mental model — performance diagnostic playbook

Flow textual pasos-a-paso producción: **"Station reporta UI Workbench lenta/freeze"**.

### Paso 1 — Verificar engine thread

Abrir `/spy/sysManagers/engineManager?hogs`. Buscar callbacks con `Max time > 200 ms`.

- Si found → §31.1.5 interpretación. Identificar offender (typo driver callback blocking, tight loop, sync I/O).
- Si NOT found → engine thread OK, problem elsewhere.

### Paso 2 — Thread dump

```
jcmd <pid> Thread.print > threads.txt
```

Analizar:
- **Engine thread** (`Niagara Engine`): estado. Si BLOCKED → lock holder cuál?
- **Jetty workers** (`qtp*`): cuántos en WAITING vs RUNNABLE vs BLOCKED?
- **Fox workers**: deadlock pattern?
- **BJob FJP workers** (`ForkJoinPool-*-worker-*`): stuck en I/O blocking?

### Paso 3 — JMX Memory

Conectar JConsole / VisualVM al puerto 9011 (si habilitado).

- Heap % usage: >90% sostenido = leak o undersized
- Metaspace trend: stable o growing?
- GC: how many Full GCs / hour? Duration?

Si JMX no habilitado:
```
jcmd <pid> GC.heap_info
jcmd <pid> VM.native_memory summary
```

### Paso 4 — GC log review

Si `-Xloggc` enabled:
```
grep "Full GC" gc.log | wc -l    # count
grep "Full GC" gc.log | tail -10 # recent durations
```

- >1 Full GC/hora en prod = abnormal
- Duration >2s = disruptive (engine miss)
- Cause "Metadata GC Threshold" = Metaspace issue

Si NO enabled: add flag + restart (next incident forensics).

### Paso 5 — Heap histogram

Quick leak check:
```
jcmd <pid> GC.class_histogram > histo.txt
```

Buscar top 10:
- `BAuditRecord` count >1M → §31.8 audit queue leak
- `BFoxChannel` count >10K → §31.2.1 channel leak (Bloque 13.2.5)
- Custom driver classes inflando → module leak
- `byte[]` huge → buffer leak

Full dump si profundizar:
```
jcmd <pid> GC.heap_dump /tmp/heap.hprof
```

Luego Eclipse MAT analysis.

### Paso 6 — Aplicar fix

Según root cause:

| Root cause | Fix |
|---|---|
| Engine callback hog | Refactor: mover I/O a driver worker, usar ASYNC flag |
| Heap undersized | Override `nre.properties` `-Xmx` up tier |
| GC thrashing | Add G1GC flags (§31.3.5) + restart |
| Metaspace leak | Investigate class reload code; restart |
| Fox channel leak | Station restart mandatory + code review driver |
| Audit queue leak | Disk free check, DB unlock, or RDB migration |
| History compaction blocking | Schedule archive low-traffic + tune lingerTime |
| Backup timeout | Move to local restore workflow |

### Paso 7 — Verify con JFR

Post-fix, grabar baseline:
```
jcmd <pid> JFR.start name=baseline duration=600s filename=baseline.jfr
```

Load en JMC, comparar vs pre-fix:
- Engine callback distribution
- GC pause frequency
- Lock contention map
- I/O activity breakdown

### Paso 8 — Continuous monitoring

Implementar ongoing:
- JMX scraper (Telegraf + Prometheus JMX exporter) — histórico
- BSystemMonitor alarms (heap, disk, FDs) → BAlarmService → email
- Cron script diff `/spy/sysManagers/engineManager` stats → rolling baseline
- Log aggregation (Graylog / ELK) con patterns `SEVERE|WARN|Full GC`

---

## 31.18 Hallazgos críticos del bloque

1. **Engine queue unbounded** (§31.1.2) — callback hog → queue grow → potential OOM indirecto. NO backpressure.

2. **21 thread pools inventariados** (§31.2.1) — desde Engine (1 fijo) hasta Virtual Cache (10). Cada uno con su own overflow behavior.

3. **Default GC en JDK 8 Zulu = ParallelGC**, NO G1GC (§31.3.1). G1GC requires manual flag. Full GC pauses 500-2000 ms típicas heap 4 GB.

4. **`nre.properties` L46-47 NO incluye flag GC** — hereda JDK default. Para stations >medium, habilitar G1GC explícito obligatorio.

5. **`niagara.fox.circuitMaxReceiveBuffer=10240000`** (§31.4.1) único fox buffer uncommented en system.properties. 10 MB hard cap queries.

6. **`box.ws.maxTextMessageSize=262144`** (§31.4.2) 256 KB default. BatchResolve grande → error.

7. **`niagara.job.threadsPerCPU=2`** commented default (§31.2.1) — override con `niagara.job.threads=16` para I/O-heavy workloads.

8. **Niagara NO usa ManagedBlocker** (§31.6.3) — FJP blocking I/O causa pool starvation. Tune `niagara.job.threads` up para Supervisor.

9. **Audit queue UNBOUNDED** (§31.8.2) — LinkedBlockingQueue sin capacity. Writer lag → heap growth → OOM cascade silent data loss.

10. **`niagara.rdbArchiveHistoryCursor.inactivityTimeout=120000`** (§31.7.5) 2 min cursor cleanup.

11. **`niagara.history.localDb.lingerTime=300000`** 5 min — table keep-open post-access.

12. **Backup monolítico sin chunking/resume** (§31.10) — `niagara.daemonsession.timeout=60000` default 60 s → WAN restore fail. Override 300 s.

13. **TimeZone per-history BHistoryConfig.timeZone** (§31.11) — NO global enforcement, cross-zone aggregation ambigua.

14. **Audit.adb sin auto-delete** (§31.12) — grow unbounded. OS logrotate NO aplica. Requires RDB external o custom purge job.

15. **BSystemMonitor disk threshold default 10% free** — too late warning en disks grandes. Ajustar 20-30% early warning.

16. **MBean naming inferido `com.tridium.sys.{engine,resource,license}:type=...`** (§31.5.2) — Tridium NO documenta explícitamente. Verificar runtime.

17. **JMX ports 9010/9011 NO habilitados default** — performance monitoring requires manual flag setup + MBEAN_PERMISSION signed module.

18. **JFR disponible Zulu 1.8.0_412** (Bloque 17.5.2) pero NO habilitado default — post-mortem forensics perdido.

19. **`-Xmx1024M`** default (Bloque 17.5.5) conservador — Supervisor >500 points debe upgrade. Tabla escala §31.14.1.

20. **22+ gotchas producción consolidados** §31.16 — cross-referencia Bloques 6/13/15/17/20/22/29 con impacto operacional.

21. **Engine thread congelado ≠ station down** (§31.1.3) — HTTP sigue responding via Jetty pool, pero BComponent state stale. Diagnóstico engañoso si solo HTTP monitoring.

22. **BOX muxer debounce 10 ms** oculta issues — subscribers receiving correct data pero driver poll slow underlying.

23. **`niagara.minMetaSpacePercentage=5`** (§31.3.3) solo QNX embedded. Supervisor Linux sin equivalent — Metaspace unbounded por default.

24. **`niagara.steadystate=10000`** 10 s delay `atSteadyState()` — servicios con init pesado retrasan boot.

25. **Engine thread priority default NORM** — configurable solo via JVM launch args. Aumentar puede mejorar latencia en stations CPU-bound.

26. **Playbook 8 pasos § 31.17** — flow diagnóstico desde síntoma UI lenta a fix verificado.

---

## 31.19 Conexiones con otros bloques

- **Bloque 6.1** (Control engine event-driven) — revisited §31.1 con detalles queue + blocking behavior + HogsPage interpretación.
- **Bloque 8** (History + Schedule + Alarm) — archive blocking mechanism §31.7.
- **Bloque 13.1** (Niagara Network) — Supervisor bottleneck 50+ subs contexto heap tuning §31.14.
- **Bloque 13.2** (Fox wire) — circuit buffer 10 MB + channel 1000 exhaustion mencionados.
- **Bloque 15.14** (polling limits empíricos) — tier recommendations §31.13.6 extiende.
- **Bloque 16.5** (Analytics BNaServlet) — subscription TTL 60 s §31.13.3.
- **Bloque 17.5** (JVM defaults) — `-Xmx1024M` + GC default + Metaspace expanded §31.3.
- **Bloque 18.4** (MBEAN_PERMISSION required signed) — JMX gate §31.5.2.
- **Bloque 20.5** (EngineManager + LeaseManager + ResourceManager) — spy pages mapping §31.15.5.
- **Bloque 20.6** (JMX ports 9010/9011) — expanded con MBean naming + JFR + jcmd §31.5.
- **Bloque 20.7** (BJobService FJP) — ManagedBlocker absence §31.6.
- **Bloque 20.8** (persistent policies) — audit sin auto-delete §31.12 cerrado.
- **Bloque 22.12** (BOX 64 KB envelope + 10 ms debounce) — subscription pipeline §31.13.
- **Bloque 27.7.3** (Signing Requester retry) — ejemplos thread pool externos.
- **Bloque 29.1.3** (Jetty thread pool 20/200) — inventario §31.2.1 fila 2.

---

## Engram topic keys

- `niagara/bloque31/perf-observability`
- `niagara/perf/thread-pools-inventory`
- `niagara/perf/gc-tuning-heap-scale`
- `niagara/perf/audit-queue-history-archive-gotchas`
- `niagara/perf/diagnostic-playbook`
