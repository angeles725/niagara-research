# Niagara N4 — Mental Model · Bloque 10: Platform & Station Lifecycle

**Sesión**: 2026-04-22
**Fuentes**: devguide (station, distribution, files, spy, arch-stack, arch-remoteProgramming), source `com.tridium.sys.station.*`, `com.tridium.sys.Nre`, platform-rt, platDaemon-*, backup-rt.

---

## 10.1 Platform daemon (niagarad)

### 10.1.1 niagarad process (Windows/Linux)

**niagarad** = daemon nativo (C/C++, no-Java) responsable de bootstrapping, monitoreo y gestión de la plataforma.

**Encarnaciones**:
- **Windows**: Windows Service registrado como `NiagaraD` (ejecutable `niagarad.exe`). Auto-start boot.
- **Linux/QNX**: POSIX daemon por systemd o `/etc/rc.d/`.

**Puerto**: TCP **5011/HTTPS** — Niagarad protocol (análogo a FOX pero para platform commands, no data).

**Rol**: NO ejecuta lógica de negocio. Es "kernel del platform" — gestiona procesos, config, licences, TLS config, comunicación con Workbench. **Lanza y supervisa** JVMs de stations (no es JVM él mismo).

### 10.1.2 Platform vs Station

| Aspecto | Platform (daemon) | Station (NRE JVM) |
|---------|-------------------|-------------------|
| Gestiona | Procesos, IP, NTP, licenses, TLS | Componentes, links, drivers, alarmas |
| Puerto | 5011/HTTPS (Niagarad) | 1911 (FOX) o 4911 (FoxS) |
| Cliente | Workbench PlatformSession, `plat.exe` CLI | Workbench FOX session, remote stations |
| Lifecycle | Arranca con SO, sobrevive crash de station | Arrancado/parado por daemon |
| Persistencia | `daemon.properties`, TCP/IP config, licenses | `config.bog` |
| Auth | Credenciales platform (user/password sistema) | BUser + roles/permissions |
| Escalabilidad | 1 daemon / máquina | N stations (típicamente 1) |

### 10.1.3 Platform services catalogados

En `PlatformServiceContainer` (platform.bog):

1. **`BSystemPlatformService`** (plat:SystemPlatformService) — platforms Win32/Linux/QNX/Atlas/NPSDK. Reboot, shutdown, station save/restore, engine watchdog. Alarma `stationSaveAlarmSupport`.
2. **`BTcpIpPlatformService`** — interfaces ethernet, IP estática/DHCP, bonding, bridging. Lee/escribe `/etc/hosts`, iface configs.
3. **`BLicensePlatformService`** — archivos `.license`. Check-in/check-out de entitlements subscription/cloud. `BLicenseChannel` FOX canal para queries remotas.
4. **`BNtpPlatformService`** — NTP client/server. SO-específico (Win32/Linux).
5. **`BSyslogPlatformService`** — syslog remoto RFC 5424.
6. **Otros**: `BackupManager`, `PlatformBackupManager`, `StationCopierPlatformService`.

### 10.1.4 Platform connection protocol (5011)

**Niagarad protocol** (propietario, no publicado en docs pero observable en jars):
- XML-based messages sobre TLS 1.2+.
- Multiplexado: 1 TCP → N canales de comando (similar FOX).
- Auth: basic (user/password) → token session.
- `DaemonMessage` base + ~150 subclases especializadas.

**Mensajes clave**:
- `StartAppMessage`, `StopAppMessage`, `RestartAppMessage`.
- `GetFileMessage`, `SaveStationMessage`.
- `ReloadLicenseMessage`.
- `UpdateTcpIpHostMessage`.
- `UpdateSSLSettingsMessage`.

**Diferencia FOX vs Niagarad**: FOX es data flow tiempo real (values, alarms, links); Niagarad es admin commands.

### 10.1.5 Station process spawning

Al recibir `StartAppMessage` para station "MyStation":

1. **Resolve**: `/stations/MyStation/config.bog`.
2. **Spawn JVM**: `nre.exe -station:MyStation -Dniagara.home=... -Dniagara.user.home=... -Xms256m -Xmx1024m`.
3. **Working dir**: `${niagara.home}`.
4. **Env vars**: `NIAGARA_HOME`, `NIAGARA_USER_HOME`, `PATH` (con `${NIAGARA_HOME}/bin/` para nativas).
5. **Handshake**: station booted → registra con daemon via IPC local (socket/named pipe), bidirectional link para reporting.

### 10.1.6 Lifecycle

1. **Daemon startup** (SO boot): lee `daemon.properties` (puerto 5011, TLS keys, preload modules), carga `platform.bog`, escucha 5011.
2. **Station startup**: Workbench `StartAppMessage` → daemon valida → spawn `nre.exe` → NRE deserializa config.bog → service init → component tree start → atSteadyState callback → escucha FOX.
3. **Steady state**: ~3s default (config `nre.steadystate`). Control loops operativos.
4. **Station stop**: `StopAppMessage` → SIGTERM JVM → NRE save config.bog + stop components + stop services + exit.
5. **Restart**: stop → wait exit → start.
6. **Daemon stop**: SIGTERM → envía SIGTERM a todas las JVMs → espera 30s → SIGKILL si no gracefulness.

### 10.1.7 Crash recovery

Si station JVM crashea (segfault, OOM, deadlock):

1. **Watchdog**: daemon polls `/proc/PID` (Linux) o handle (Windows). Detecta exit inesperado.
2. **Alarm**: `EngineMonitor` (platform.bog) reporta "Station crashed".
3. **Auto-restart** (configurable via `BSystemPlatformService.autoRestartStation`):
   - Exponential backoff: 1s, 2s, 4s, …, max 60s.
   - Max intentos configurable (default 5).
   - Alcanzado max → "Failed", no reinicia más.
4. **Manual**: admin via Workbench `RestartAppMessage`.

**config.bog ACID**: último write confirmado sobrevive. Next boot carga correctamente.

### 10.1.8 Logs

**Daemon logs** (`${niagara.user.home}/daemon/`):
- `daemon.log` append, rotado diariamente.
- Content: startup, platform events, station start/stop, license reload, TLS cert changes, IPC errors.
- Level via `DaemonLogLevel` (INFO/WARN/ERROR/DEBUG).

**Station logs** (`${niagara.user.home}/stations/{name}/`):
- `console.log` (redirect stdout JVM).
- Content: boot phases, service init, component start, alarm events, FOX connections, module loads.
- Acceso via `GetAppOutputMessage` (daemon relay) o Workbench "Download Logs".

**Sincronización**: al `StopAppMessage`, NRE flushea alarms/logs a disk antes de exit. Daemon puede zipear logs en `backup.tro` comprimido.

---

## 10.2 Station startup + file system + spy

### 10.2.1 Boot sequence (8 fases)

Implementado en `Station.bootStation()`:

1. **Setup JVM**: instancia `Console`, adquiere file lock (`FileLock.lock()` del boot file), suspende `EngineManager`.
2. **Carga config.bog**: `Station.loadStation(bootFile)` via `ValueDocDecoder` con type resolver del registry. Monta `BStation` en namespace ORD como `local:|station:` via `BComponentSpace`. Tipicamente 200-500ms.
3. **Carga plataforma**: `Nre.loadPlatform()` inicializa `RuntimeProfile` (Supervisor, Jace, etc.), managers (EngineManager, ResourceManager, StdoutManager).
4. **Validación licencia**: `Station.checkLicense()` contra `tridium` feature. Demo mode → `FAULT_DEMO_ONLY`.
5. **Data Recovery init**: `initDataRecoveryService()` restaura snapshot previo si enabled.
6. **Service init loop**: `ServiceManager.startAllServices()` (ver 10.2.2). ~100-300ms.
7. **Component tree start**: `station.start()` → `BComponent.start()` cascada. Callbacks: `started()` → `descendantsStarted()`.
8. **Post-start**: `stationStarted()` (Bloque 6.1), timer steady state (default 10s, config `niagara.steadystate`), `atSteadyState()` callback — control algorithms inician.

**Total típico**: 2-5s desktop, comprimido a 500ms en embedded (Jace).

### 10.2.2 Service dependency resolution

`ServiceManager` two-pass implícito (no DAG explícito, categoría por tipo):

**Pass 1 — Data Recovery primero**:
```java
if (!dataRecoveryRunning) startDataRecoveryService();
```

**Pass 2 — ServiceContainers, luego resto**:
```java
BComponent[] serviceContainers = getServices("baja:ServiceContainer");
for (BComponent sc : serviceContainers) startService(sc);
for (BComponent service : getAllServices())
  if (!alreadyStarted(service)) startService(service);
```

**Hotplug**: post-startup `ServiceManager.register()` chequea `servicesRunning=true` → `startService()` inmediato.

**Orden empírico típico**:
1. DataRecoveryService
2. JobService
3. AlarmService
4. CommunicationService
5. Otros dependientes

Cada service bloquea thread en su `serviceStarted()` callback.

### 10.2.3 File system semantics

4 raíces ORD especiales en `BLocalHost`:

| Root | Type | Contenido | Montado en | Uso |
|------|------|-----------|-----------|-----|
| `!config` | BComponentSpace | config.bog (BStation) | `local:\|station:` | Estado de la station |
| `!sys` | BFileSpace | `$NIAGARAHOME/sys` | `local:\|sys:` | System files (modules, libs, docs) |
| `!fox` | BComponentSpace | Fox protocol tree | `local:\|fox:` | Remote station links |
| `!file` | BFileSystem | Local FS root | `local:\|file:` | `/home`, `/etc`, etc. |

**BFile resource model**:
- **Lazy-loaded**: `BIFile.getStore().read()` on-demand.
- **Typed**: `.bog` → `file:BogFile`, `.xml` → `file:XmlFile` mapeados en `module-include.xml`.
- **Config vs data**: config en `config.bog` (BComponent tree), data en `~stations/{name}/data/` (off-bog).

Ejemplo resolution:
```
file:/etc/config.bog → BFileSystem.INSTANCE.resolve("etc/config.bog")
                    → BFile con store=LocalFileStore("/.../etc/config.bog")
```

### 10.2.4 Spy pages

Diagnostics framework ortogonal al modelo de componentes. URL namespace `/spy/`.

**Arquitectura**:
- `Spy.ROOT` (`BSpySpace`) raíz global.
- `SpyDir` página con subpáginas (`SpyDir.add(name, subpage)`).
- Ruta típica: `/spy/sysManagers/{managerName}/{metricName}`.

**Subpáginas** (añadidas en `Nre.initSystemSpies()`):

| Subpágina | Source | Contenido |
|-----------|--------|-----------|
| `/spy/sysManagers/engineManager` | EngineManager | Thread count, lease count, queue depth, CPU time |
| `/spy/sysManagers/leaseManager` | LeaseManager | Active leases, task distribution, stale |
| `/spy/sysManagers/resourceManager` | ResourceManager | Heap, GC stats, file descriptors, disk |
| `/spy/console` | ConsoleSpyDir | Output buffer, command history, errors |
| `/spy/security` | SecuritySpyDir | Permissions, audit log, module signatures |

**Acceso HTTP**: `/station:/spy/sysManagers/engineManager` via `BSpyScheme`. Renderiza HTML table via `SpyWriter`. Authorization por default abierto (Workbench); filtros via `BSpySpace.allowRemoteAccess()`.

### 10.2.5 Component mounting runtime

Agregar componente dinámicamente:

1. **Add to tree**: `parent.addChild(newComponent, slotName)` — slot registrado, parent + ORD path asignados.
2. **Space integration**: `newComponent.fw(105, Boolean.TRUE, ...)` (framework code) → añade a `space.spaceObjects()` (index rápido).
3. **Start immediate** (si parent started):
   ```java
   if (parent.isStarted()) {
     newComponent.start();
     newComponent.descendantsStarted();
   }
   ```
4. **stationStarted callback** (si station en steady state):
   ```java
   newComponent.stationStarted();
   ```

**Timing**: mounting + start + callbacks < 50ms típico (sin I/O bloqueante). **Critical**: lookup de services en `started()` debe fallar gracefully si no existen aún.

---

## 10.3 Backup, distribution, disaster recovery

### 10.3.1 BBackupService

Servicio station para backups de configuración. Operaciones:

- **`restoreFiles(BIFile backupFile, Context cx)`**: restaura desde .dist. Valida dependencias plataforma/software, verifica permisos `adminWrite`, audita. Lanza `RestoreThread` → apaga station, restaura archivos, reinicia.
- **Exclusiones configurables**: `excludeFiles`, `excludeDirectories`, `offlineExcludeFiles`, `offlineExcludeDirectories`.

Registra canal FOX `BFoxBackupJob` para backups remotos. Requiere `NiagaraBasicPermission("RESTORE_BACKUP")`. Genera audit de seguridad.

### 10.3.2 Formato `.dist`

JAR (PKZIP compatible) con:
- **Manifest** `meta-inf/dist.xml`: versión, dependencias (OS, NRE, modules, hardware), exclusiones, modules a instalar.
- **Content**: config.bog, archivos estáticos (px, html, images), platform.bog, licenses, certificates.
- **NO incluye**: runtime databases (`.hdb` history, `.adb` alarm), webFileCache, datos activos.

**Manifest attrs**: `reboot` (t/f), `noRunningApp`, `absoluteElementPaths`, `hostId` (backups host-específicos), reglas de reemplazo (`always`, `never`, `crc`, `oscrc`, `nocopy`, `hostid`).

### 10.3.3 Backup automático

**Online** (station running): FOX BackupService crea `.dist`, excluye `*.hdb`, `*.adb`, `*.lock`, dirs `history/`, `alarm/`, `webFileCache/`. Accesible Workbench Platform Administration.

**Offline** (station stopped): daemon-side backup sin exclusiones. Más completo, requiere station down.

**microSD (HON-9000)**: backups automáticos daily 02:00 local, retiene últimas 3 versiones.

### 10.3.4 Restore flow

1. **Seleccionar .dist**: Distribution File Installer parsea, verifica compatibilidad, valida dependencias.
2. **Validación**: si protegido con passphrase, solicita. Verifica modules presentes en software registry.
3. **Detener station**: automáticamente o manual.
4. **Install**: copia archivos, actualiza config.bog, platform.bog. Opcional restore TCP/IP.
5. **Reboot**: sistema reinicia. Varios minutos.

Post-restore: station reinicia, carga config.bog restaurado. NO instala software, NO modifica content filter.

### 10.3.5 Station Copier

Vista en Platform Administration:
- **Dual pane**: izq = stations en Workbench PC (User Home), der = stations en daemon User Home del platform abierto.
- **Operaciones**: Copy (install en remota/local), Rename, Delete.
- **Dependencias**: verifica modules presentes, advertencias de signing de third-party.

Portabilidad sin `.dist`.

### 10.3.6 Upgrade flow (N4.14 → N4.15)

**Preserva**: config.bog, modules instalados, config data en platform.bog.
**Pierde**: runtime code (recompila contra nueva NRE), deprecated properties, cambios API incompatibles.

**Proceso**: Distribution File Installer descarga nueva NRE + modules + OS updates → aplica via `.dist` → requiere reboot.

Módulos custom: recompilar contra Gradle 7.6+ (Niagara 4.13+). API changes en release notes.

### 10.3.7 Commissioning (primer setup)

1. Conectar controller a Workbench (USB/Ethernet RNDIS).
2. Open Platform → Platform Administration.
3. Create New Station → config.bog en `~/stations/{name}/`.
4. Import licenses → License Manager.
5. Configurar TCP/IP → network, DNS, rutas.
6. Install drivers → modules opcionales BACnet/LON/SNMP.

Alternativo: **copy existing station** via Station Copier desde otro host con validación de dependencias.

### 10.3.8 DR (Disaster Recovery) practices

1. **Backups regulares**: automatizar via cron/scheduler. Guardar `.dist` en ubicación segura (USB, net share).
2. **Validar backups**: restore periódico en test host.
3. **Documentar dependencias**: NRE version, modules, licenses, TCP/IP settings en manifest.
4. **Factory reset**: botón SHUT DOWN en HON-9000 → credenciales default.
5. **Restore selectivo**: si `.dist` contiene modules no soportados en destino, Installer bloquea. Usar `.dist` compatible.
6. **Protección**: encriptar `.dist` con system passphrase → auth required al restore.

**Recuperación rápida**: Host ID en microSD es portable — clona stations entre controladores similares con mismo `.dist`.

---

## Síntesis del bloque

### Modelo mental

**Niagara es un sistema de dos niveles**:
1. **Platform daemon** (`niagarad`): proceso nativo C/C++, vida larga, gestiona múltiples stations. Puerto 5011/HTTPS, mensajes admin (Niagarad protocol). Services: System/TcpIp/License/Ntp/Syslog/Backup.
2. **Station** (JVM + NRE): una o más spawned por daemon. Puerto FOX 1911/4911. Boot en 8 fases secuenciales, 2-5s desktop, 500ms embebido.

**File system** es namespace ORD unificado: `!config` (config.bog), `!sys` (system files), `!fox` (remote stations), `!file` (local filesystem). Todo lazy-loaded, typed.

**Spy pages** son backdoor diagnóstico en `/spy/` — acceso directo a managers internos sin modelarlos como BComponents. Critical para debugging runtime.

**Backup/restore** a través de `.dist` (JAR con manifest, config.bog, modules). Online vs offline. Station Copier es la alternativa sin archivo (copia directa platform-to-platform).

### Conexiones

- **Bloque 1** (Estructura): boot carga modules del registry via `ClassScanner`. Profile determina runtime class.
- **Bloque 2** (Licensing): `BLicensePlatformService` gestiona `.license` files. Station valida feature `tridium` en fase 4 del boot.
- **Bloque 3** (Security): SecurityManager install en fase 1. Policy files signed verifican integridad. module validation durante class load.
- **Bloque 4** (Baja): `BStation` es BComponent. `BComponentSpace` es el contenedor raíz. Service lifecycle usa callbacks de BComponent.
- **Bloque 5** (BOG): `config.bog` deserializado por LoadOp en fase 2. `.dist` contiene config.bog + platform.bog.
- **Bloque 6** (Engine): fase 7 boot = `start()` cascada. fase 8 = `stationStarted()` + `atSteadyState()`. Engine thread arranca en fase 3.
- **Bloque 9** (UI): Workbench abre **dos tipos de conexiones** — PlatformSession (5011) y FOX Session (1911). Mismas credenciales pero passwords y scopes independientes.

### Gotchas críticos

1. **5011 vs 1911/4911** — puertos distintos, protocolos distintos, credenciales distintas. Confusión común en debugging.
2. **Platform vs Station credentials** — cuentas separadas. Platform auth = OS-level; station auth = BUser.
3. **Auto-restart crash loop** — si station crashea repetidamente, backoff exponencial hasta max intentos, luego "Failed". Monitorear daemon.log.
4. **Service init order no es DAG** — categoría por tipo. Services no deben depender del orden explícito; usar callbacks lifecycle.
5. **`started()` callback puede ejecutar antes de que otros services estén listos** — hacer lookups con fallback, o defer a `stationStarted()`.
6. **config.bog ACID pero .hdb/.adb no en backup online** — databases runtime requieren offline backup para integridad completa.
7. **`.dist` encriptado requiere passphrase en restore** — si se pierde, backup inutilizable.
8. **Station Copier valida dependencies pero no firmas** — third-party modules pueden pasar verificación y luego fallar al load (signing chain).
9. **Spy pages sin auth por default** — si platform expuesto a red no confiable, filtrar via `allowRemoteAccess()`.
10. **File lock durante boot** — dos stations con mismo nombre no pueden coexistir. Lock se libera en exit; crash puede dejar lock huérfano.

### Qué habilita

Con Bloques 1-10 podés:
- Debuggear por qué un station no arranca (fase exacta que falla via logs).
- Diseñar recovery procedure post-crash (auto-restart + DR plan).
- Migrar station entre hardware con Station Copier o `.dist`.
- Consultar spy pages para diagnósticos runtime sin rebootear.
- Entender el aislamiento platform/station y por qué son cuentas separadas.

**Próximo**: Bloque 11 — Auth / RBAC runtime (BUser, BRole, BCategory, schemes: Digest/SAML/OAuth/Kerberos/Google).

---

## Engram topic keys

- `niagara/platform/daemon-niagarad` — niagarad process, puerto 5011, platform services, lifecycle, crash recovery, logs.
- `niagara/platform/station-boot-filesystem` — boot 8 fases, service resolution, !config/!sys/!fox/!file, spy pages, mounting runtime.
- `niagara/platform/backup-dist-disaster-recovery` — BBackupService, formato .dist, online/offline backup, restore flow, Station Copier, upgrade, commissioning, DR.

---

**Sesión cerrada**: 2026-04-22 — Bloque 10 consolidado.
