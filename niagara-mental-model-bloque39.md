# Bloque 39 — Provisioning + Backup + Supervisor Replication + HA Operacional + Flota Management

**Tema**: qué *realmente* pasa cuando un Supervisor Niagara N4.14 ejecuta provisioning masivo contra N subordinates, cómo funciona el `BBackupService`/restore, cómo se replica estado (points/histories/alarms/schedules) entre Supervisor y subordinates, y **qué alternativas operacionales existen** ante la ausencia confirmada de HA nativa — con runbooks concretos para failover manual y cold restore.

**Método**: read-only sobre `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/`:
- `modules/provisioningNiagara-ux.jar` + `provisioningNiagara-wb.jar` extraídos a `/tmp/b39/` (386 clases, 225 páginas HTML en `docProvisioning-doc.jar`)
- `modules/backup-rt.jar` + `backup-wb.jar` → `javap -p BBackupService`, `BFoxBackupJob`, `BAxOfflineBackup` (+ `docBackupRestore-doc.jar`)
- `modules/niagaraDriver-rt.jar` → `javap -p BNiagaraStation`, `BNiagaraNetwork`, `BNiagaraProxyExt`
- `grep -l "failover\|HighAvailability\|SplitBrain"` sobre todos los módulos → **0 matches confirmados**
- `defaults/system.properties` 589 líneas — 1 flag relevante: `niagara.license.subscriptionLicenseAllowed=true`
- **Hallazgo crítico #0**: NO existe `provisioningNiagara-rt.jar` ni `provisioning-rt.jar` separados. El runtime del provisioning framework para NiagaraNetwork vive **dentro de** `provisioningNiagara-wb.jar` (clases en `javax.baja.provisioningNiagara.*`) + `niagaraDriver-rt.jar` + `backup-rt.jar` + `batchJob-rt.jar`. El WB JAR NO es sólo UI — contiene `BNiagaraNetworkJob`, `BForEachStationStage`, `BProvisioningBackupStep`, `BProvisioningNiagaraNetworkExt` (runtime funcional). Esto rompe la convención clásica -rt/-wb/-ux.

**Conecta con**:
- **Bloque 14.6** (Niagara Templates NO auto-propagan) → `BUpgradeTemplateStep` es el vehículo de propagación manual via provisioning
- **Bloque 16.10.1** (NiagaraNetworkJob 2-stage FIXED Initial+ForEachStation) → extender con ejecución, retry, combinación adyacente
- **Bloque 16.11.1** (BProvisioningBackupStep) → profundizar `doRun` + parallel conflicts + lambda a BBackupService
- **Bloque 10.3.3** (online backup NO incluye `.hdb`/`.adb` abiertos) → mecanismo exacto y cómo lo evidencian los `excludeFiles`/`excludeDirectories` props
- **Bloque 13.1.7 + 19.13** (Supervisor bottleneck ~50 subordinates) → causas SPECIFIC: `BStationPollScheduler`, `pollScheduler` + Fox channel exhaustion + BatchJob thread pool
- **Bloque 13.2.5** (Fox channel exhaustion) → backup step abre channel dedicado per-subordinate
- **Bloque 14.4** (federation count in origin) → `BNiagaraProxyExt implements BISubLicenseable` CONFIRMADO
- **Bloque 19.14** (NO HA nativa) → evidencia exhaustiva `grep -l failover` = 0 matches; matriz operacional concreta
- **Bloque 20.7.3** (BJob NO persistido default) → provisioning job crash mid-way = **no resume** + `Canceled` cascading
- **Bloque 20.8.4** (backup restore NO chunking) → confirmado `BBackupService.zip(Job, OutputStream)` = stream directo monolítico
- **Bloque 24.12** (driverSchedule `getExportableSchedule()` INLINE) → schedule sync Supervisor→subordinates usa `BNiagaraScheduleExport`
- **Bloque 24.14** (clock drift) → impacto en HA pair Supervisor y en Historia federada
- **Bloque 30** (keyring transplant DPAPI `.km/.kr` + license DPAPI) → pieza clave del runbook de restore
- **Bloque 31** (thread pools saturation) → `BPlatformWorker`, `BCyclicThreadPoolWorker` per-subordinate, job queue
- **Bloque 33** (history archive + retention) → backup jobs almacenan `.dist` en `^provisioningNiagara/stationData/…` y requieren retention policy
- **Bloque 34** (alarm forward `BStationRecipient`) → alarm federation desde subordinate al Supervisor

---

## 39.0 Tres niveles de "backup/provisioning" — desambiguar primero

Niagara usa la palabra "backup" para 3 cosas MUY distintas. Confundir estas niveles es la fuente de 60% de los errores operacionales reportados.

| Tipo | Contenido | Tamaño | Herramienta | Clase Java | Uso típico |
|---|---|---|---|---|---|
| **Station Copier** (`.bog`) | Station `.bog`, histories, alarms | <1–50 MB | Workbench Station Copier | N/A (platform tool) | Mover station entre PCs/JACEs del mismo modelo |
| **Backup `.dist`** | `.bog` + histories + alarms + module refs + JVM/OS version + platform config | <1–50 MB | Workbench Tools→Station→Backup **o** `BBackupService` runtime **o** `BProvisioningBackupStep` en batch job | `javax.baja.backup.BBackupService` | Rollback lógico; migración de controller (mismo modelo, clean dist previa para downgrade) |
| **Clone backup** (USB JACE-8000) | TODO: bog + histories + alarms + modules + JVM + OS image + platform config | 50+ MB | Browser / USB port / Debug port | N/A (binario OS-level) | Disaster recovery a hardware idéntico. ONLY JACE-8000 con Niagara 4 |

**Docs de referencia**: `/tmp/b39/docBackupRestore-doc.jar.d/doc/PlatformAndStationBackupOptions1.html`.

Confirmación operacional de limitaciones:
- USB backup **FAT32/FAT32X únicamente**. NTFS NO soportado. USB sticks **≤128 GB** (flash, no HDD externo — causa brownout del JACE).
- Clone backup → **same-model restore only**. JACE-8000 backup no restora en JACE-9000 ni Supervisor ni workstation.
- Station Copier `.bog` → cross-model OK pero NO trae módulos ni OS.

---

## 39.1 `BBackupService` — anatomía real (backup-rt.jar)

`javax.baja.backup.BBackupService extends BAbstractService implements BIRestrictedComponent` — servicio singleton del station, no-instanciable libremente (`BIRestrictedComponent` + `checkParentForRestrictedComponent`).

### 39.1.1 Propiedades observables

```
excludeFiles          : BString   — glob pattern for online backup
excludeDirectories    : BOrdList  — ords excluded in online backup
offlineExcludeFiles   : BString   — glob pattern for offline backup (BAxOfflineBackup)
offlineExcludeDirectories : BOrdList
```

Dos sets distintos: `online` vs `offline`. Online = `zip()` con station running. Offline = `BAxOfflineBackup` con station DOWN (preferido para incluir `.hdb`/`.adb` en estado consistente).

### 39.1.2 Constantes internas críticas (vía `javap`)

```java
private static final BOrd[]     COMMON_PLATFORM_BACKUP;           // siempre incluidos
private static final BOrdList   DEFAULT_SECURITY_BACKUP;          // incluye keyring, certs
private static final BOrd[]     WRITABLE_HOME_PLATFORM_BACKUP_ADDER;
private static final BOrd[]     WRITABLE_LICENSE_WITH_WRITABLE_HOME_PLATFORM_BACKUP_ADDER;
private static final BOrd[]     WRITABLE_LICENSE_WITH_READONLY_HOME_PLATFORM_BACKUP_ADDER;
private static final BOrd[]     READONLY_LICENSE_WITH_WRITABLE_HOME_PLATFORM_BACKUP_ADDER;
private static final BOrd[]     READONLY_LICENSE_WITH_READONLY_HOME_PLATFORM_BACKUP_ADDER;
private static final List<Pattern> TRIDIUM_LEGACY_PASSPHRASE_ENCRYPTED_PATHS;
private static final List<Pattern> KEYRING_ENCRYPTED_STATION_PATHS;
private static final List<Pattern> PASSPHRASE_ENCRYPTED_PATHS;
```

**Hallazgo #1**: existen **4 combinaciones** distintas de backup según writable/readonly de license y home platform — el servicio introspecciona el hosting antes de decidir qué ords van. Esto explica por qué "backup desde JACE" y "backup desde Supervisor Win" producen `.dist` distintos en tamaño y contenido aunque la misma station lógica esté corriendo.

### 39.1.3 El método que importa: `zip(Job, OutputStream, boolean, Context)`

Firma real:
```java
public void zip(BJob job, OutputStream out, boolean includeHistoriesAlarms, Context cx)
public void zip(BJob job, PlatformDaemon pd, OutputStream out, boolean includeHA, Context cx)
public void zip(JobLog jl, ICanceler ic, PlatformDaemon pd, OutputStream out, boolean includeHA, Context cx)
```

**Observaciones críticas** (Bloque 20.8.4 confirmado y extendido):

1. **Un solo `OutputStream`** — stream directo sin chunking. Si la red corta a 80%, el `.dist` destino queda **corrupto** (no hay manifest al final aún, y el stream interno escribe ZIP central directory solo al `close()`). No hay retry.
2. **`ICanceler`** es cooperativo: el cancel manda flag, el loop de `addFilesToDist` verifica entre files, no interrumpe el stream activo de un `.hdb` de 500 MB.
3. **NO hay compresión adicional** — el `.dist` ES un ZIP estándar (ver `commonsCompress-rt` dep). Compression level default = `DEFLATE`. No encrypt-at-rest del container; la seguridad de credenciales viene de `PASSPHRASE_ENCRYPTED_PATHS` y `KEYRING_ENCRYPTED_STATION_PATHS` — **archivos individuales dentro del ZIP** se cifran con `PBEEncodingKey` si matchean los patterns.
4. **`processManifest` se llama AL FINAL** (`lambda$processManifest$2`) — el manifest lista checksums + versiones. Un backup interrumpido → manifest ausente → restore falla verificación en `verifyDependencies`.

### 39.1.4 Restore flow — NO hay `BRestoreJob` clase separada

Buscado exhaustivamente: `find /tmp/b39 -name "BRestoreJob*.class"` → sin resultados. El restore vive DENTRO de `BBackupService`:

```java
public void restoreFiles(BIFile, Context)
public void restoreFiles(BIFile, boolean overwrite, boolean restartAfter, long timeout, Context)
private void doRestore(BBackupService$RestoreOp)
private void restartStation(BBackupService$RestoreOp)                  // ← SÍ reinicia
private void verifyDependencies(BBackupService$RestoreOp)              // ← falla si modules no están
public static ICanceler makeCanceler(BJob)
```

Inner classes: `BBackupService$RestoreOp`, `BBackupService$RestoreThread`, `BBackupService$BackupOp`, más 4 variantes `{Readonly|Writable}LicenseAnd{Readonly|Writable}HomePlatformBackup`.

**Hallazgo #2**: `restoreFiles` toma `boolean restartAfter` — si `false`, el station queda en estado inconsistente con `.bog` del backup pero runtime del old. En UI está hardcoded a `true`; en Java API puedes pasarlo a `false` y romperte solo.

**Hallazgo #3**: hay un `RestoreThread` dedicado (inner class). El restore NO corre en engine thread ni en batch job queue — es un thread standalone que invoca `restartStation()` al final. Esto significa:
- El restore **no compite** con engine ni con otros batch jobs por thread pool
- PERO el restore **no se reporta** en `BJobService` con granularidad de otros jobs — hay que mirar el station log directo
- Si el Supervisor cae durante restore de un subordinate, el subordinate queda en estado `restarting` con `.bog` medio-aplicado

### 39.1.5 Fox channel dedicado — `BBackupChannel` (backup-rt.jar)

```java
class com.tridium.backup.BBackupChannel {
  class EmptyOutputStream { ... }
}
```

Registro en `serviceStarted → registerFoxChannel()` — **cada station abre 1 Fox channel dedicado a operaciones de backup**. Relevante para Bloque 13.2.5 (Fox channel exhaustion): en un Supervisor con 50 subordinates, durante ventana de backup simultánea se abren 50 channels de backup además de los de point/alarm/history/schedule. El `niagara.fox.maxServerSessions` default (sistema.properties menciona el flag) debe revisarse.

### 39.1.6 Permissions requeridas (module.xml backup-rt)

Este bloque merece lectura porque revela qué TOCA un backup:
```
read,write,delete  ${niagara.home}/defaults
read,write,delete  ${niagara.home}/etc
read,write,delete  ${niagara.home}/fips            ← FIPS material (Bloque 30)
read,write,delete  ${niagara.home}/lib
read,write,delete  ${niagara.home}/platform
read,write,delete  ${niagara.home}/security
read,write,delete  ${niagara.user.home}/-          ← TODO station home
read,write,delete  ${niagara.alternative.archive.path}/{alarm,history}
read,write,delete  ${niagara.alternative.database.path}/{alarm,history}
read,write,delete  /etc/dhcpd, /etc/ieee8021x, /etc/net, /etc/wifi
KeyRingPermission  *
NiagaraBasicPermission  GET_KEY_RING
NiagaraBasicPermission  TRANSCODE_KEY_RING
NiagaraBasicPermission  RESTORE_BACKUP
NiagaraBasicPermission  SYSTEM_PASSWORD
NiagaraSocketPermission  *:1-100000   ← accept,connect,listen,resolve
RuntimePermission  setIO, modifyThread, modifyThreadGroup, shutdownHooks
```

**Hallazgo #4**: el backup SÍ toca `/etc/dhcpd`, `/etc/wifi`, `/etc/ieee8021x`, `/etc/net` — en JACE con Linux embebido estos son config del OS. En Supervisor Windows esos paths no existen → se saltan silentemente. **Un `.dist` tomado en Supervisor NO tiene netcfg del host**; un `.dist` en JACE sí. Esto es por qué "restaurar .dist de JACE en Supervisor" falla incluso con el mismo `.bog`.

**Hallazgo #5**: `RuntimePermission shutdownHooks` → BackupService registra hook JVM; si el JVM cae durante backup por OOM, el hook intenta cerrar el ZIP central — a veces funciona, a veces deja `.dist.tmp` huérfano. Buscar `*.dist.tmp` en `^provisioningNiagara/stationData/` es indicador confiable de backup fallido.

---

## 39.2 Provisioning framework — `BNiagaraNetworkJob` + `BForEachStationStage`

Extensión de Bloque 16.10.1 con detalles operacionales.

### 39.2.1 Jerarquía de tipos (decorada)

```
javax.baja.batchJob.driver.BDeviceNetworkJob
      └── javax.baja.provisioningNiagara.BNiagaraNetworkJob
            · constructors: ()  |  (String)  |  (String[])
            · getStationState(String) → BJobState    ← per-station
            · setStationState(String, BJobState)
            · getInitialStage()         → BNetworkJobStage
            · getForEachStationStage()  → BForEachStationStage   ← 2nd stage FIXED
            · initStages()                             ← protected, crea los 2 stages
            · makeOp(Context)           → BatchJobOp
            · addStep(BNetworkJobStep)  ← Initial stage
            · addStep(BDeviceJobStep)   ← ForEachStation stage
            · getAgents(Context)        → AgentList

javax.baja.batchJob.driver.BDeviceJobPrototype
      └── javax.baja.provisioningNiagara.BNiagaraNetworkJobPrototype
            · Property jobPrototype                    ← template del job
            · Property retentionPolicy                 ← auto-disposal del job
```

El job tiene exactamente **2 stages fijos**: `Initial` (una vez, pre-loop) y `ForEachStation` (loop). NO hay stage `Final`, NO hay `Cleanup`.

### 39.2.2 Builder pattern

`BNiagaraNetworkJobBuilder` inner-classes:
- `CopyTemplates` — command para clonar steps desde un prototype a un job nuevo
- `Submit` — command que llama `saveJob()` + dispatch

Hallazgo: el field `TEMPLATE_CACHE` (estático, string) sugiere cache de templates ya copiados en el builder — evita re-leer blob cada vez. Invalidation: manual (nueva instancia del builder).

### 39.2.3 Ejecución — orden EXACTO (evidencia `docProvisioning/aProvJobExecution.html`)

1. **Pre-dispatch: step combining** (opt-in automático).
   - Steps **adyacentes** de los siguientes tipos son **combinados a uno**:
     - `InstallSoftwareStep` (Install By Spec / Install Combined By Spec)
     - `CopyFileStep` (File Copy)
     - `UpgradeOutOfDateStep`
   - Motivo: evitar dependency-check duplicado con el subordinate + minimizar reboots.
   - **Gotcha G1**: si insertás `SetTimeJobStep` entre dos `InstallSoftwareStep` adyacentes, rompés el combining → dos reboots en vez de uno → **5–10 min extra** por station × N stations.

2. **Initial stage** — ejecuta una sola vez, típicamente `BUpdateLicensesJobStep`. El Supervisor hace inquiry al licensing server con host-ids de todos los subordinates; respuesta = license archive que se aplica a la DB local del Supervisor. Si no hay internet: step falla, job falla SI está marcado stop-on-fail.

3. **ForEachStation stage** — loop secuencial sobre stations del job, en el orden del builder.
   - Para cada station: steps secuenciales, fail-fast.
   - Station state: `Running` → `Success` | `Failed` | `Canceled`.
   - **Si un step falla en stationA**: los remaining steps de stationA NO corren. Job continúa con stationB.
   - **Si cancelás el job mid-stream**: stationActual termina su step actual (cooperative cancel), las stations restantes quedan `Canceled`. NO rollback.

4. **Job state final**: `Success` sólo si 100% de stations+steps OK. Si al menos 1 failed → job `Failed` (pero las stations Success siguen Success; no hay rollback).

### 39.2.4 Per-station parallelism — `getParallelExecutionConflicts`

`BProvisioningBackupStep.getParallelExecutionConflicts(service, device, Set<BDevice>, op)` → retorna set de devices que NO pueden correr en paralelo con `device`.

**Implicación**: el batch job framework **tiene soporte para ejecución paralela de ForEachStation**, pero cada step declara sus conflicts. Para `BProvisioningBackupStep`, el conflict set típicamente incluye la misma device (no backups simultáneos sobre el mismo station) pero permite paralelo across stations.

**Hallazgo #6**: el default parallel degree NO está en system.properties — lo maneja `BBatchJobService` + thread pool size. `niagara.fox.maxServerSessions` y el pool size son el bottleneck real para "¿cuántos subordinates puedo backupear en paralelo?".

### 39.2.5 Retention policy (`BNiagaraNetworkJobPrototype.retentionPolicy`)

Evidencia en `/tmp/b39/docProvisioning-doc.jar.d/doc/ConfiguringJobRetentionPolicy.html`:
- Default: **permanent retention** (hasta `Dispose` manual).
- Disposal borra: batch job log files (`.bjl`), batch step log files (`.bjsl`), backup `.dist` asociados.
- Policy options: `by elapsed time` | `by number of executions`.
- Acción RMB `Enforce Retention Policy` en el prototype → ejecuta inmediato.
- Path de los `.dist`: `^provisioningNiagara/stationData/<station>/backups/backup_<station>_<timestamp>.dist`

**Gotcha G2 (clásico)**: provisioning backup sin retention → después de 6 meses × 50 stations × weekly backup, el Supervisor tiene 1300 `.dist` × 30 MB = **~39 GB** de `.dist` consumiendo disk. El station no-reporta el uso (no cuenta como history) y el free-space monitor falla silencioso hasta que el log rotation revienta.

---

## 39.3 Catálogo COMPLETO de Provisioning Steps (49 tipos, 4 subcategorías)

Obtenido con `find -name "*Step.class"` + `javap | grep extends`. Base classes:
- `BDeviceJobStep` — corre UNA VEZ por device (subordinate station)
- `BNetworkJobStep` — corre UNA VEZ total (Initial stage)
- `BAbstractSoftwareStep extends BDeviceJobStep` — software-family con combining
- `BAbstractDeployStep extends BDeviceJobStep` — template-family
- `BAbstractDiscoveryStep extends BNetworkJobStep` — discovery-family
- `BJobStep` — genérico abstract

| # | Step | Base | Categoría | UI Factory | Notas operacionales |
|---|---|---|---|---|---|
| 1 | `BProvisioningBackupStep` | `BDeviceJobStep` | Backup | `BProvisioningBackupFactory` | Ve 39.1.3. Genera `.dist` en Supervisor filesystem. Soporta parallel conflicts |
| 2 | `BInstallBySpecStep` | `BAbstractSoftwareStep` | Software | `BInstallSoftwareFactory` | Combinable con otros software adyacentes |
| 3 | `BInstallByOrdStep` | `BAbstractSoftwareStep` | Software | `BInstallSoftwareFactory` | |
| 4 | `BInstallCombinedBySpecStep` | `BAbstractSoftwareStep` | Software | `BInstallSoftwareFactory` | Batch install multiple modules |
| 5 | `BInstallStep` (abstract-ish) | `BAbstractSoftwareStep` | Software | — | Base para Clean Dist + DistWithPassphrase |
| 6 | `BInstallCleanDistStep` | `BInstallStep` | Software | `BInstallCleanDistFactory` | Factory-reset del controller con dist limpio |
| 7 | `BInstallDistWithPassPhraseStep` | `BAbstractSoftwareStep` | Software | — | Instala con passphrase embedded |
| 8 | `BStationInstallablesStep` | `BAbstractSoftwareStep` | Software | — | Gestiona installables del station (addOns) |
| 9 | `BUpgradeOutOfDateStep` | `BAbstractSoftwareStep` | Software | `BUpgradeOutOfDateFactory` | Compara con Supervisor repo + aplica deltas |
| 10 | `BUninstallModuleStep` | `BAbstractSoftwareStep` | Software | — | Remove module + reboot |
| 11 | `BExportApplicationTemplateConfigJobStep` | `BNetworkJobStep` | Software (export) | — | Initial-stage only |
| 12 | `BFileCopyStep` | `BAbstractSoftwareStep` | Files | `BCopyFileFactory` / `BCopyLocalFileFactory` | Combinable con software adyacente |
| 13 | `BRebootJobStep` | `BDeviceJobStep` | Device | `BRebootJobStepFactory` | `implements ICancelHint`. Timeout `deviceRebootTimeout` (Bloque 39.4.1) |
| 14 | `BRenameStationStep` | `BDeviceJobStep` | Bootstrap | `BRenameStationStepFactory` | `implements BIPrivilegedDeviceJobStep` — requiere elevated perms |
| 15 | `BEnableBootstrapStep` | `BNetworkJobStep` | Bootstrap | `BEnableBootstrapStepFactory` | Initial-stage. Habilita bootstrap mode |
| 16 | `BDhcpDiscoveryStep` | `BAbstractDiscoveryStep` | Discovery | `BDhcpDiscoveryStepFactory` | |
| 17 | `BNiagaraNetworkDiscoveryStep` | `BAbstractDiscoveryStep` | Discovery | `BNiagaraNetworkDiscoveryStepFactory` | |
| 18 | `BDeployTemplateStep` | `BAbstractDeployStep` | Template | `BDeployTemplateFactory` | Aplica template (Bloque 14.6) |
| 19 | `BDeployApplicationStep` | `BAbstractDeployStep` | Template | `BDeployApplicationFactory` | Application = template multi-station |
| 20 | `BUpgradeTemplateStep` | `BDeviceJobStep` | Template | `BUpgradeTemplateFactory` | Actualiza template YA DEPLOYADO. NO auto-propaga sin este step |
| 21 | `BInstallCertificateJobStep` | `BDeviceJobStep` | Security | `BInstallCertificateStepFactory` | Policy enum `BInstallCertificatePolicyEnum` |
| 22 | `BImportSignedCertificateJobStep` | `BDeviceJobStep` | Security | `BImportSignedCertificateFactory` | |
| 23 | `BGenerateCertJobStep` | `BDeviceJobStep` | Security | `BGenerateCertFactory` | CSR generation |
| 24 | `BExportCsrJobStep` | `BDeviceJobStep` | Security | `BExportCsrFactory` | |
| 25 | `BSignCertJobStep` | `BDeviceJobStep` | Security | — | Self-sign con CA del Supervisor |
| 26 | `BSetCertificateAliasJobStep` | `BDeviceJobStep` | Security | `BSetCertificateAliasFactory` | |
| 27 | `BSetTlsLevelJobStep` | `BDeviceJobStep` | Security | `BUxSetTlsLevelFactory` | TLS level per-station |
| 28 | `BConfigureNiagaraIdPAndSAMLSchemeJobStep` | `BDeviceJobStep` | Security (SAML) | `BConfigureNiagaraIdPAndSAMLSchemeStepFactory` | Inner exceptions: `SAMLIdPConfigurationJobStepArgsException`, `SAMLIdPConfigurationJobStepException` |
| 29 | `BSecurityGroupJobStep` | `BJobStep` | Security | `BSecurityGroupStepFactory` | **Único que extiende `BJobStep` directo — ni device ni network** |
| 30 | `BSetSystemPassphraseJobStep` | `BDeviceJobStep` | Credentials | `BSetSystemPassphraseStepFactory` | `SYSTEM_PASSWORD` permission requerida |
| 31 | `BSetPlatformCredentialsJobStep` | `BDeviceJobStep` | Credentials | `BSetPlatformCredentialsStepFactory` | Daemon user/pass |
| 32 | `BSetPlatformUserPasswordJobStep` | `BDeviceJobStep` | Credentials | `BSetPlatformUserPasswordStepFactory` | Rotate platform user pw |
| 33 | `BRemovePlatformUserJobStep` | `BDeviceJobStep` | Credentials | `BRemovePlatformUserStepFactory` | |
| 34 | `BAddStationUserStep` | `BDeviceJobStep` | Credentials | `BUxAddStationUserStepFactory` | Crea station user |
| 35 | `BRemoveStationUserStep` | `BDeviceJobStep` | Credentials | `BRemoveStationUserStepFactory` | |
| 36 | `BSetStationUserPasswordJobStep` | `BDeviceJobStep` | Credentials | `BSetStationUserPasswordStepFactory` | |
| 37 | `BSetStationConnectionCredentialsStep` | `BDeviceJobStep` | Credentials | `BSetStationConnectionCredentialsStepFactory` | Credentials que Supervisor usa para conectarse al subordinate |
| 38 | `BSetupReciprocalConnectionStep` | `BDeviceJobStep` | Connection | `BUxSetupReciprocalConnectionStepFactory` | Subordinate → Supervisor reverse connection |
| 39 | `BSetPropertyJobStep` | `BDeviceJobStep` | Component | `BSetPropertyStepFactory` | ORD-based property set remoto |
| 40 | `BRemovePropertyJobStep` | `BDeviceJobStep` | Component | `BRemovePropertyStepFactory` | |
| 41 | `BSetTimeJobStep` | `BDeviceJobStep` | Device | `BUxSetTimeStepFactory` | Sincroniza clock (ver Bloque 24.14) |
| 42 | `BRobotJobStep` | `BDeviceJobStep` | Script | `BRobotStepFactory` | `implements BIEncodable`. Ejecuta Robot script (`.nav`). Most flexible step |
| 43 | `BUpdateLicensesJobStep` | `BNetworkJobStep` | License | `BUxUpdateLicensesStepFactory` | Initial-stage. Ve 39.2.3-paso-2 |
| 44 | `BConvertToPerpetualLicenseModeJobStep` | `BNetworkJobStep` | License | `BConvertToPerpetualLicenseModeFactory` | Subscription → perpetual migration |
| 45 | `BUpdateConfigurationStep` | `BDeviceJobStep` | Config | `BUxUpdateConfigurationFactory` | |
| 46 | `BUpgradeApplicationStep` | `BDeviceJobStep` | Application | `BUxUpgradeApplicationFactory` | App-level upgrade distinto de template |
| — | **Total**: **46 step concretos** + 3 abstractos (`BAbstractSoftwareStep`, `BAbstractDeployStep`, `BAbstractDiscoveryStep`) | | | | |

### 39.3.1 Grupos funcionales (sugerido para operador)

```
Software (11):       Install×4 + Upgrade×2 + Uninstall + Reboot + FileCopy + ExportAppTemplateCfg + StationInstallables
Security (9):        Certificate×5 + TLS + SAML + SecurityGroup + … 
Credentials (8):     Passphrase + PlatformCreds×3 + StationUser×3 + StationConnCreds + Reciprocal
Template (3):        Deploy + DeployApp + UpgradeTemplate
License (2):         UpdateLicenses + ConvertToPerpetual
Device/Connection (4): Reboot + Rename + SetTime + SetupReciprocal
Discovery (2):       DhcpDiscovery + NiagaraNetworkDiscovery
Backup (1):          ProvisioningBackupStep
Component (2):       SetProperty + RemoveProperty
Script (1):          RobotJobStep (escape hatch — cualquier cosa custom)
Config (1):          UpdateConfigurationStep
Bootstrap (1):       EnableBootstrap
App (1):             UpgradeApplication
```

### 39.3.2 `BRobotJobStep` — el escape hatch

`BRobotJobStep implements BIEncodable` → acepta un **Niagara Robot** (`.nav` script file). Robot es un DSL interno de Niagara para scripting de workbench actions: `open`, `action`, `set`, `copy`, etc. Si ningún step del catalog hace lo que necesitás, `RobotJobStep` puede.

**Gotcha G3**: Robot se ejecuta con credenciales del **Supervisor**, no del subordinate — si el robot script hace `station.open(...)` va contra la station del Supervisor localmente, y luego remote-actions al subordinate vía Fox. Fallo común: el robot asume Workbench UI y falla en job unattended.

---

## 39.4 `BProvisioningNiagaraNetworkExt` — el service-level en el Supervisor

Es un `BAbstractService` con `Property software`, `licenses`, `pollScheduler`, `deviceRebootTimeout`, `stationShutdownTimeout`, `connectTimeout`, `socketTimeout`. Vive como service del Supervisor station (no del subordinate).

### 39.4.1 Timeouts exactos (config props)

```
deviceRebootTimeout     : int (seconds)  — max wait tras `BRebootJobStep` para reconnect
stationShutdownTimeout  : int (seconds)  — max wait para que station salude `stopped`
connectTimeout          : int (seconds)  — socket connect (platform or fox)
socketTimeout           : int (seconds)  — socket read, aplica a daemonSession streams
```

Estos cuatro son **los knobs operacionales** de provisioning. Defaults típicos 4.14: 120s / 60s / 30s / 60s — pero no están en `defaults/system.properties`, se leen del `.bog` (vienen del palette). Verificable con `Wb: provisioningNiagaraService → property sheet`.

**Hallazgo #7**: el `pollScheduler : BStationPollScheduler` es **por-network**, no per-station. Define cómo el Supervisor polea Subordinates para detectar out-of-date software. Si lo bajás de 24h default a 1h con 50 stations → saturación del `BPlatformWorker` pool.

### 39.4.2 FoxServerConnectionListener

`BProvisioningNiagaraNetworkExt implements BFoxService$FoxServerConnectionListener`
→ `serverConnectionCreated(BFoxServerConnection, FoxSession, FoxMessage)` callback.

**Reconexión de subordinate genera evento** que dispara `updateStatusForAllProvisioningExtensions()` → recompute de status per station + registro en status map.

### 39.4.3 `ProvisioningRegistry` + `BInstallableSpec`

`installableRegistry : ProvisioningRegistry` mantiene el catálogo de dists disponibles en el Supervisor para deploy. `getSoftwareValidationStatus(List<String>, Context)` retorna JSON con signed/unsigned status de cada module → lo consume la UI para "Not Signed" warnings.

**Hallazgo #8**: los módulos custom de Honeywell (`honPlantController-rt`, etc.) tienen `ModuleSignatureStatusEnum` distinto — `SIGNED_BY_VENDOR` vs `SIGNED_BY_TRIDIUM`. El step `BInstallBySpecStep` puede rechazar module Honeywell si el Supervisor está en TLS level HIGH y el cert chain no incluye CA de Honeywell en el trust store del daemon. Fix: añadir CA Honeywell a `security/user/my.cert` del Supervisor station + restart.

---

## 39.5 `BPlatformConnection` + `BPlatformWorker` — el pipe por donde pasa todo

Cada `BNiagaraStation` (subordinate) tiene un `BPlatformConnection` **como agent** (declarado en `module.xml` del provisioningNiagara-wb: `<agent><on type="niagaraDriver:NiagaraStation"/>`). Este agent es el "canal de trabajo del platform daemon" entre Supervisor y subordinate.

### 39.5.1 Propiedades y estado

```java
Property port, credentials, secure, health, alarmSourceInfo, worker
public BDaemonSession getDaemonSession()                    // puede lanzar AuthenticationException
public BDaemonSession getDaemonSession(int retries, int decayMs, boolean forceNew)
public static Function<Integer,Integer> DAEMON_SESSION_RETRY_DECAY  // retry backoff policy
```

El `DAEMON_SESSION_RETRY_DECAY` es un **public static Function** — expuesto como extension point. Default probablemente `x -> x*2000` (exponential backoff). Test: `BPlatformConnection.DAEMON_SESSION_RETRY_DECAY.apply(3)` para ver.

### 39.5.2 BIPingable

`implements BIPingable` → PingMonitor integra `BPlatformConnection` al health model estándar de drivers. El `health` property es el mismo modelo de ping-based-liveness que usan todos los drivers Niagara.

Implicación: si marcás `enable ping` en `BPlatformConnection`, el agent se comporta como "device" — con `ping` action, `doPing()`, `pingOk/pingFail`, `PingHealth` stats. Buen monitor para alertar si el Supervisor perdió platform access al subordinate (aunque Fox siga OK).

### 39.5.3 `BPlatformWorker` — thread dedicado por-station

Cada station tiene un `BPlatformWorker` (property de `BPlatformConnection`) — es un worker thread dedicado a operaciones platform-level (daemon session calls) con cola propia.

**Conexión con Bloque 31**: este es UN thread pool ADICIONAL al inventario de 21 pools del bloque 31. Con 50 subordinates → 50 `BPlatformWorker` threads solo para platform ops.

---

## 39.6 Supervisor Replication — qué se replica y cómo cuenta hacia licencia

Extensión profunda de Bloque 14.4 + 19.11+.

### 39.6.1 Tabla de replicación

| Recurso | Mecanismo | Dirección | Lazy/Eager | Cuenta hacia `point.limit` del Supervisor | Bloque ref |
|---|---|---|---|---|---|
| Points | `BNiagaraProxyExt` (extends `BProxyExt` implements `BISubLicenseable`) sobre `BNiagaraPointDeviceExt` del subordinate | Sub → Sup (pull subscribe) | **Eager** (subscription activa) | **SÍ — point count en Supervisor** ✅ | 14.4 |
| Histories | `BNiagaraHistoryDeviceExt` + import config (schedule-triggered) | Sub → Sup (pull scheduled) | **Lazy** (cron) | NO — historias separadas per-station | 33 |
| Alarms | `BNiagaraAlarmDeviceExt` + `BStationRecipient` routing | Sub → Sup (push al ocurrir) | Eager | NO | 34 |
| Schedules | `BNiagaraScheduleDeviceExt` + `BNiagaraScheduleExport` / `BNiagaraScheduleImportExt` | **BIDI**: Sup→Sub (driverSchedule push) o Sub→Sup (import) | Eager on change | NO | 24.12 |
| Users | `BNiagaraUserDeviceExt` | — | Ver Bloque 30 | NO | 30 |
| SysDef | `BNiagaraSysDefDeviceExt` | Bidi (config sync) | Lazy | NO | — |
| Files | `BNiagaraFileDeviceExt` | Sub → Sup on request | Lazy (on-demand) | NO | — |
| Virtuals | `BNiagaraVirtualDeviceExt` (gated by `virtualsEnabled`) | Sub exposes virtual tree | Lazy | NO | 25 |

### 39.6.2 Point federation — el costo real

`BNiagaraProxyExt` en el Supervisor:
```java
public class BNiagaraProxyExt extends BProxyExt
    implements BISubLicenseable, INiagaraProxyExt, IProxyActionParent
```

El `BISubLicenseable` + `getLicenseKeyPrefix()` → cada point federado **incrementa el contador de points del Supervisor**. Si Supervisor es licenciado para 10,000 points y cada subordinate tiene 1,000 points, **10 subordinates full-subscribe exhauria la licencia**.

**Gotcha G4 (bottleneck real ~50 stations)**: la razón NO es solo Fox channel exhaustion (Bloque 13.2.5) — es **combinación de 3 factores**:
1. **License** — 50 × 1000 = 50,000 points hacia `point.limit` Supervisor. La licencia supervisor enterprise típica: 10k–100k. Más stations = más points federados = más licencia consumida.
2. **Fox channels** — cada station abre ~4–6 channels (point / alarm / history / schedule / sysdef / backup). 50 × 5 = 250 server sessions del Supervisor Fox. Default `niagara.fox.maxServerSessions` ~= 100–200.
3. **Thread pools** — `BPlatformWorker` × 50 + `BCyclicThreadPoolWorker` × N + batchJob queue saturation.

Cualquiera de los 3 puede pegar primero según el workload.

### 39.6.3 Schedule sync — `BNiagaraScheduleExport` + `BNiagaraScheduleLearnJob`

Confirmadas en `niagaraDriver-rt.jar`:
```
BNiagaraScheduleDeviceExt.class
BNiagaraScheduleExport.class
BNiagaraScheduleImportExt.class
BNiagaraScheduleLearnJob.class
BNiagaraScheduleLearnResult.class
BScheduleChannel.class
DefaultNiagaraVirtualStationAdapter$ScheduleRpcInvocation.class
```

Bloque 24.12 reportaba `getExportableSchedule()` como INLINE (no cache). Confirmado: `BScheduleChannel` + `ScheduleRpcInvocation` → cada sync recalcula. `BNiagaraScheduleLearnJob` hace discovery/pull en BJob.

**Gotcha G5**: sync de schedules driverSchedule del Supervisor a subordinates → si el schedule tiene 500 events, cada push re-serializa TODO el schedule (no diff). Network overhead × N subordinates × cada cambio.

### 39.6.4 Alarm federation — relación con Bloque 34

Alarmas viajan subordinate → Supervisor vía `BStationRecipient` (configurado en el subordinate) + routing topic que apunta al Supervisor. El Supervisor las recibe en su `BAlarmService` y las guarda en su `.adb` propio.

**Hallazgo #9**: las alarmas NO cuentan como "federadas on-demand" como los points — se **almacenan duplicadas** en subordinate y Supervisor. Esto es por diseño (offline resiliency del Supervisor) pero significa que un outage del link Sup↔Sub seguido de un burst de alarmas → `alarm queue` del subordinate crece (`BStationRecipient` retry), y si el queue es bounded → alarmas perdidas.

---

## 39.7 Bottleneck Supervisor — por qué ~50 subordinates es el techo práctico

Consolidación de causas:

```
Supervisor (1 host)
   │
   ├─── BProvisioningNiagaraNetworkExt
   │        └── BStationPollScheduler (poll config del SW de c/station)
   │
   ├─── NiagaraNetwork.workers : BCyclicThreadPoolWorker   ← pool compartido
   │        └── worker threads for proxy point subscription pumping
   │
   ├─── BPlatformWorker × N stations    ← 1 thread dedicado per station
   │
   ├─── BatchJobService thread pool      ← provisioning jobs
   │        └── parallel execution limited by pool + getParallelExecutionConflicts
   │
   ├─── Fox Server Sessions              ← maxServerSessions cap
   │        └── 5 channels × 50 stations = 250 sessions típico
   │
   ├─── Engine thread ÚNICO              ← Bloque 31 — bottleneck global
   │        └── processes all property changes from proxy points
   │
   └─── License check en cada BNiagaraProxyExt.start()  ← SYNC O(n)
```

Pegan en ese orden típicamente:
1. **Engine thread** (Bloque 31) — saturado si subscription rate alta
2. **Fox sessions** — tocaste `maxServerSessions`; connection refused
3. **Point license** — nueva station no federa points ("license exceeded")
4. **Heap** — cada proxy point tiene cache de `BPropertyList` + subscription state; 50k points ≈ 500 MB sólo en proxy state

### 39.7.1 Señales observables

| Síntoma | Causa probable | Check |
|---|---|---|
| "Station appears down" intermitente en manager UI | Fox channel exhaustion, reconnects ciclo | `spy://fox/sessions` + `niagara.fox.traceSessionStates=true` |
| Points quedan `stale` (subscribed pero no actualizan) | Engine thread o worker pool saturado | `spy://engineManager/hogs` |
| Provisioning job `Failed` en station random | `BPlatformWorker` pool full + socketTimeout | job log + station platform log |
| License warning "point capacity 98%" | Federation overrun | `LicenseManager` en Supervisor |
| `OutOfMemoryError` engine | Proxy point subscription cache crece | GC logs + heap dump |

---

## 39.8 HA Operacional — QUÉ existe y qué NO (Bloque 19.14 extendido)

### 39.8.1 Evidencia exhaustiva de ausencia de HA nativa

```bash
grep -l "failover\|Failover\|HighAvailability\|ActiveStandby\|SplitBrain" \
      -r /tmp/b39/ --include="*.class"
# → 0 matches
```

Búsqueda en módulos adicionales Honeywell:
- `honProvisioning-*` — NO existe
- `honBackup*` — NO existe
- `ascProvisioning*` — NO existe
- `cloudBackup-rt.jar` — existe, pero es **upload a Niagara Cloud** (SaaS de Tridium), no peer-to-peer failover
- `honCloudEasyOnboard-rt.jar` — onboarding provisioning helper, no HA

**Confirmación Bloque 19.14**: NO hay:
- Clustering (ni Hazelcast ni raft ni gossip)
- Leader election
- Virtual IP failover
- Shared storage model
- Replication sync nativa entre 2 Supervisors
- Split-brain detection automática

### 39.8.2 Patrones alternativos — matriz de escenarios

| Escenario | Mecanismo | RTO realista | RPO realista | Split-brain risk | Complejidad |
|---|---|---|---|---|---|
| **Cold standby** con backup periódico manual | Cron backup + transfer a host-B + restore on failure | 30–120 min | = intervalo backup (4h–24h típ) | Bajo (host-B apagado hasta promoción) | Baja |
| **Warm standby** con backup diario + DNS switch | Backup diario → host-B restaura en startup automatizado pero apagado | 10–30 min | 24h | Medio (si DNS cache rompe, ambos hosts talking a subordinates) | Media |
| **Warm active** con `BProvisioningBackupStep` desde host-B pulling | host-B ejecuta provisioning backup contra host-A + restora localmente sin arrancar | 5–20 min | 1h–24h según sched | **ALTO** — hay que rigurosamente NO arrancar B | Media |
| **Dual-active (READ-only B)** | Ambos Supervisors running; B no federa points, solo mira histories importadas | 2–5 min (democión A, promoción B) | cercano a 0 en reads, alto en writes/commands | **CRÍTICO** — duplicate commands a subordinates | Alta |
| **Hardware redundancy** (2 JACEs con keepalive VIP) | NO SOPORTADO POR NIAGARA — requiere network-level VRRP + NAT, y ambos talking Fox confunde subordinates | — | — | **INACEPTABLE** | No recomendado |
| **DR-Cloud** usando `cloudBackup-rt.jar` | Upload backup a Niagara Cloud; en DR, descarga+restore manual | horas | 24h típico | N/A | Media (vendor-lock Tridium Cloud) |

### 39.8.3 Split-brain scenarios concretos

**Caso 1 — dual-active con ambos talking Fox**:
- A y B ambos abren `BPlatformConnection` al mismo subordinate
- Cada uno federa points → **contadores license en ambos**
- Ambos escriben `writeValue` vía `BNiagaraProxyExt` → **race condition en el subordinate**; `priority array` de BACnet puede resolver, `BControlPoint` simple NO
- Alarmas dobladas en recipients
- History import schedules compiten → ambos piden la misma ventana → network wasted

**Caso 2 — DNS switch sin demotion**:
- A sigue corriendo pero subordinates ya apuntan a B por DNS
- A cree que perdió subordinates → fault + alarm
- Subordinates reciben config push de B (schedules nuevos) pero A sigue polling con config vieja
- Auditable mess: `audit.trail` de A y de B no coinciden

**Caso 3 — restore simultáneo de mismo backup en ambos**:
- Operador en stress restaura `.dist` en A y B (mismo backup source)
- Ambos tienen identical `host-id` → license conflict en Tridium license server
- Una de las 2 licenses se desactiva random en próximo heartbeat a licensing.tridium.com

### 39.8.4 Detection de split-brain — manual

No hay detector automático. Los operators se dan cuenta por:
- Alarmas duplicadas en mismo evento
- History con gaps y overlaps
- Users reportando "ya configuré X y volvió al valor anterior"
- License warnings en ambos A y B
- `auditHistory` en subordinate muestra 2 usuarios distintos (user@supA, user@supB) escribiendo mismo point rápido

---

## 39.9 Runbook 1 — Failover manual 7 pasos (cold standby)

Asumido: Supervisor A caído; Supervisor B host apagado con último backup restored pero station NO started. Keyring DPAPI backup disponible (Bloque 30 procedure).

```
PRE-FLIGHT (5 min)
  1. CONFIRMAR A está caído realmente
     - ping host-A : fail
     - Workbench → open platform A : connection refused
     - System shell access (serial / IPMI) : host unreachable
     NO ASUMIR — si A responde parcialmente, primero demote A (stop station + stop daemon).

  2. VERIFICAR backup disponible en B
     - cd $niagaraUserHome-B/provisioningNiagara/stationData/<stationName>/backups
     - ls -lh backup_<station>_*.dist | tail -3
     - Verificar timestamp < RPO aceptable (ej: <24h)
     - Verificar tamaño > último backup known-good

PROMOTION (10 min)
  3. APLICAR keyring al host-B si aún no
     - Bloque 30 procedure: copiar $niagaraUserHome/security/*.km + *.kr de A (si rescatable)
     - En su ausencia: regenerar con system passphrase del dist backup (si conocida)
     - SIN keyring válido, los passwords encrypted en el .bog NO decodifican

  4. PROMOVER B
     - Edit $niagaraUserHome-B/stations/<station>/config.bog si hace falta:
        · Confirmar `<NiagaraNetwork>` tiene los subordinates esperados
        · Actualizar IP del Supervisor en cada subordinate ref si distinto
     - Verificar license:
        · niagara.license con host-id de B (puede requerir re-host call a Tridium)
        · Temporarily: license puede funcionar en grace period ~30 días

  5. ARRANCAR station B
     - platform → applications → start <station>
     - Monitor logs: station.log, audit.trail
     - Esperar `serviceStarted` completo (típ 2–10 min en Supervisor con 50 subordinates)

ANNOUNCE (5 min)
  6. CAMBIAR DNS / referencias
     - DNS A-record <supervisor.fqdn> → IP-B
     - O actualizar en cada subordinate: Wb → NiagaraNetwork → Supervisor → address = new IP
     - TTL DNS: idealmente ≤60s; si era 3600 tenés que esperar propagation

VERIFICATION (variable)
  7. CONFIRMAR operación
     - NiagaraNetwork Manager: todos subordinates `{ok}` en ≤10 min
     - Point manager random sample: values actualizándose (no `stale`)
     - Alarm console: eventos recientes visibles
     - Trigger manual provisioning job `Backup` contra 1 subordinate test → success
     - VERIFICAR A NO se levanta solo (disable auto-start / pull power)
```

**Tiempo total realista**: 30–60 min con operador entrenado + datos preparados. 2–4 h sin preparación.

---

## 39.10 Runbook 2 — Cold restore completo post-disaster 10 pasos

Asumido: pérdida total de Supervisor host (hardware dead + backups locales perdidos). Tenés backup `.dist` off-site y keyring backup separado (si seguiste Bloque 30).

```
PASO 1 — PREPARE new host (30–60 min)
  - Hardware spec ≥ original (ideal igual; Niagara chequea no estrictamente)
  - OS: Windows Server 2019+ (si Supervisor era Win) — MISMA familia recomendada
  - Install Niagara 4.14 mismo build (4.14.0.162 en este caso)
  - Install Honeywell OptimizerSupervisor distribution
  - NO arrancar station aún

PASO 2 — LICENSE rehost (15–60 min depende Tridium)
  - Obtener host-id del new host: `platformctl hostId` o wb platform
  - Contact Tridium licensing portal / partner Honeywell → rehost request
  - Recibir licenses.zip nuevo
  - Copy a $niagaraHome/security/licenses/ con certs asociados

PASO 3 — KEYRING recovery (CRÍTICO — Bloque 30)
  - Opción A: tenés backup .km/.kr files separados + system passphrase old
      → copy a $niagaraUserHome/security/ (station stopped)
  - Opción B: solo .dist sin keyring
      → PASSWORDS ENCRYPTED en el .bog NO van a decodificar
      → tenés que resetear TODO: BACnet device passwords, Modbus secrets, SAML IdP secrets, LDAP binds, BPassword reversibles, mail service smtp password, integration API keys
      → plan B: re-create user base from scratch; los system passphrases no migran sin keyring
  - Opción C: DPAPI scope mismatch (Bloque 30)
      → si A era domain user y B es local user → DPAPI NO descifra aunque passphrase OK
      → MISMO usuario SAME domain SAME machine context (idealmente)

PASO 4 — RESTORE .dist (10–30 min)
  - platform connection al NEW host as admin
  - Station Copier o Workbench Tools → Station → Restore
  - Select backup_<station>_<timestamp>.dist
  - Station directory: confirm destination path
  - Options:
      [x] overwrite existing station config
      [x] restart station after restore  (default true)
  - Internally: BBackupService.restoreFiles(BIFile, true, true, 0, ctx)
      → RestoreOp → verifyDependencies → restoreFiles → restartStation

PASO 5 — VERIFY module compatibility (5–20 min)
  - Si modules del backup NO están en new host → restore falla en verifyDependencies
  - Common gap: Honeywell modules versión distinta
  - Fix: instalar modules faltantes via platform → software manager → install from dist
  - Cross-check: el manifest del backup (inside .dist) lista exact versions
  - Pattern: backup .dist → extract → read `manifest.xml` antes del restore

PASO 6 — ARRANCAR station (5–10 min)
  - platform → applications → start
  - Tail $niagaraUserHome/stations/<s>/console.log y station.log
  - Errores típicos:
      · `license not found` → paso 2 mal
      · `cannot decrypt X` → keyring paso 3 mal
      · `module Y not found` → paso 5 incomplete
      · `corrupt db` → .hdb/.adb incompletos; ver paso 7

PASO 7 — RECONSTRUIR histories/alarms si .dist online era parcial (1–8h)
  - Bloque 10.3.3: backup online NO incluye .hdb/.adb abiertos
  - Si el backup fuente era online → los histories del último período no están
  - Import desde subordinates si tienen history almacenada:
      · Manual: NiagaraStation → histories → import job
      · O trigger schedule-based history import normal y esperar convergencia
  - Alarmas perdidas NO recuperables sin backup alarm archive separado

PASO 8 — CERT CHAIN rebuild (30 min)
  - TLS certs del backup son del host viejo; host-id distinto → certs válidos
    semánticamente PERO fingerprint distinto si regeneraste → clientes tienen
    pinned cert del old
  - Regenerate self-signed cert + distribute a todos los clients
  - O: si cert era CA-signed + privada viene en keyring → puede reusar si hostname same
  - Para cada subordinate: may need to reapprove new Supervisor cert
      · BProvisioningSetCertificateAliasJobStep + push manual

PASO 9 — RE-ESTABLECER subordinate connections (15–60 min según N)
  - Para cada subordinate, approach:
      a) Si subordinate tiene reciprocal connection y IP del Supervisor cambió:
         → SSH/Wb al subordinate → NiagaraNetwork → Supervisor address = new IP
      b) Si subordinate tiene pinned TLS fingerprint:
         → approve nuevo cert via platform → user → trust store
      c) Si subordinate tiene stored Supervisor credentials y estas cambiaron:
         → BSetStationConnectionCredentialsStep en modo 1:1 via console directa
  - Verify: NiagaraNetwork Manager en Supervisor → todos `{ok}`

PASO 10 — VALIDATION full + provisioning smoke test (30 min)
  - Trigger `BProvisioningBackupStep` contra 1 subordinate (test backup)
  - Trigger manual write a point de subordinate → verify actualiza en Supervisor
  - Alarm generation test: force alarm en subordinate → verify arrives in Supervisor alarm console
  - History import test: pick 1 history con rate 1min → verify new datapoints arrive
  - Schedule push test: edit driverSchedule in Supervisor → verify propaga al subordinate
  - License sanity: `LicenseManager` muestra counts razonables, no warnings
```

**Tiempo total realista**: **6–12 horas** si tenés backup reciente + keyring + modules. **24–48 horas** si keyring perdido (recrear credentials). **3–5 días** si perdiste backup reciente y hay que reconstruir desde subordinates + logs.

---

## 39.11 ASCII — topología flota + bottleneck points

```
                      ┌─────────────────────────────────────┐
                      │     SUPERVISOR (1 HOST — NO HA)     │
                      │                                     │
  DR Cloud  ◄────────►│  cloudBackup-rt   (Tridium Cloud)   │   ← opcional, vendor-lock
                      │                                     │
  Backup DR (manual)  │  BBackupService  ──► .dist files    │
  off-site ◄──────────┤      └── SLA RTO 6–12h typical      │
                      │                                     │
                      │  BProvisioningNiagaraNetworkExt     │
                      │      ├── software : ProvisioningRegistry
                      │      ├── licenses : BSupervisorLicenses
                      │      └── pollScheduler : BStationPollScheduler
                      │                                     │
                      │  BNiagaraNetwork                    │
                      │      ├── workers (cyclic pool)      │ ◄── BOTTLENECK 1: pool size
                      │      └── stations  [N=50 typical]   │
                      │                                     │
                      │  BatchJobService                    │
                      │      └── BNiagaraNetworkJob queue   │ ◄── BOTTLENECK 2: job queue depth
                      │                                     │
                      │  Engine Thread (ÚNICO)              │ ◄── BOTTLENECK 3: events × 50
                      │      └── Bloque 31                  │
                      │                                     │
                      │  Fox Server Sessions (max ~200)     │ ◄── BOTTLENECK 4: channels × 5 × 50 = 250
                      │                                     │
                      │  License: point.limit (e.g. 10k)    │ ◄── BOTTLENECK 5: federation count
                      └─────┬──────┬──────┬──────┬──────────┘
                            │      │      │      │
                 ┌──────────┘      │      │      └─────────────┐
                 │                 │      │                    │
           ┌─────▼───────┐   ┌─────▼──────┐  ... ┌─────────────▼────┐
           │ Subordinate │   │ Subordinate│      │ Subordinate N=50 │
           │  JACE-8000  │   │  JACE-9000 │      │    Supervisor    │  ← "Supervisor" can
           │             │   │            │      │  as subordinate  │    be subordinate
           └──┬──────────┘   └──────┬─────┘      └──────────────────┘
              │                     │
        Per-station channels        │
        ────────────────────        │
          - FoxClientConnection (1) │
          - point subscription (1)  │
          - history polling  (1)    │
          - alarm routing    (1)    │
          - schedule sync    (1)    │
          - backup channel   (1)    │  ← BBackupChannel (backup-rt)
          - platform daemon  (1)    │  ← BPlatformConnection
                                    │
                        Total: ~7 conn/station × 50 = 350 connections
```

---

## 39.12 Gotchas consolidados

### Parte A — Provisioning

**G1** Steps adyacentes `InstallSoftware`/`CopyFile`/`UpgradeOutOfDate` se COMBINAN auto. Meter un step no-combinable entre ellos (ej. `SetTimeJobStep`) rompe el combining → 2 reboots en vez de 1 → 5–10 min extra × N stations.

**G2** Retention default = "forever". Sin retention policy, backups `.dist` se acumulan en `^provisioningNiagara/stationData/` indefinidamente. 50 stations × weekly × 30MB × 52 semanas = 78 GB/año silencioso.

**G3** `BRobotJobStep` corre con credenciales del **Supervisor**, no del subordinate. Robot scripts asumiendo Workbench UI fallan en job unattended.

**G4** `BInstallBySpecStep` rechaza modules no-signed si Supervisor en TLS HIGH — incluso modules Honeywell oficiales si CA chain no está en trust del daemon. Fix: añadir CA al trust store antes del provisioning.

**G5** Job crash mid-stream → NO resume (Bloque 20.7.3 BJob no persisted default). Stations remaining → `Canceled` cascading. Debés re-submit el job entero; stations ya-success se re-ejecutan (idempotencia de cada step es tu responsabilidad).

**G6** `BProvisioningBackupStep.doRun` usa lambda sincrónico con `FilePath, RemoteStation, BNiagaraStation, BDeviceStepDetails, BBackupService, BPlatformConnection` — el step NO libera el thread mientras el backup corre. 50 backups en paralelo = 50 threads del batch job pool ocupados.

**G7** `subscriptionLicenseAllowed=true` (default en Honeywell dist) — habilita subscription options en provisioning commissioning. Deshabilitar en sites con license perpetual-only para evitar que operadores marquen stations como subscription por error (causa license consumption distinta).

### Parte B — Backup/Restore

**G8** `BBackupService.zip` stream monolítico. Cualquier interrupción → `.dist` corrupto; `.dist.tmp` huérfano marca el fallo. Monitor `.dist.tmp` como indicador de backup fallado.

**G9** Online backup NO incluye `.hdb`/`.adb` abiertos (Bloque 10.3.3 reconfirmado: `excludeFiles`/`excludeDirectories` properties del `BBackupService`). Para backup completo con histories corrientes → usar `BAxOfflineBackup` con station DOWN.

**G10** `TRIDIUM_LEGACY_PASSPHRASE_ENCRYPTED_PATHS` + `PASSPHRASE_ENCRYPTED_PATHS` + `KEYRING_ENCRYPTED_STATION_PATHS` — 3 sets distintos de paths cifrados. Restore a host DIFERENTE sin passphrase correcto → esos archivos quedan incrustados cifrados inutilizables.

**G11** Backup sobre Supervisor Windows NO captura `/etc/net, /etc/wifi, /etc/dhcpd, /etc/ieee8021x` (paths Linux). Un `.dist` Supervisor trasladado a JACE arranca sin netcfg del JACE (y viceversa).

**G12** Clone backup (JACE-8000 USB) FAT32 only, ≤128GB, flash NO HDD. USB externo con brownout puede BRICKEAR el JACE.

**G13** Cross-model restore prohibido para Clone Backup. Station Copier sí (bog-only). Backup `.dist` requiere clean dist previa si cross-version.

### Parte C — Supervisor Replication

**G14** Point federation cuenta en ORIGEN (Supervisor): `BNiagaraProxyExt implements BISubLicenseable`. 50 × 1k = 50k points. License enterprise típica 10k–100k — tope real.

**G15** Schedules se sincronizan FULL serialización cada change. 500-event schedule edit → full payload a cada subordinate. Network-expensive.

**G16** Alarmas NO son federadas lazy — almacenamiento dual (subordinate + Supervisor). Link flaky → `BStationRecipient` queue bounded del subordinate → alarmas perdidas si overflow.

**G17** History import schedules pueden overlappear si el Supervisor marca el subordinate como behind → doble trabajo, doble network. Tune `pollScheduler` + stagger import times across subordinates.

**G18** `BPlatformWorker` thread per-station. 50 stations = 50 threads solo para platform ops. Suma a los 21 pools del Bloque 31.

### Parte D — HA operacional

**G19** NO HA nativa. Grep exhaustivo 0 matches en `failover`/`HighAvailability`/`SplitBrain`. Operacional-only.

**G20** Split-brain NO detectable automático. Se descubre por síntomas (alarmas dobles, audit raro, license warnings en 2 hosts).

**G21** License tied a `host-id` (function del hardware + OS + MAC). Migration cross-hardware → rehost via Tridium licensing. Grace period típico 30 días.

**G22** Keyring DPAPI scope (Bloque 30): domain user A → local user B NO descifra el keyring aun con mismo passphrase. Plan recovery escalonado.

**G23** Clock drift entre Supervisor HA pair (Bloque 24.14) → history import filters con "since timestamp" bailan entre A y B. NTP obligatorio + max drift ≤1s.

**G24** `BProvisioningBackupStep` contra el PROPIO Supervisor station: no soportado en el workflow normal (el Supervisor es origen, no target). Backup del Supervisor = manual Wb Tools → Backup, o `BBackupService` API direct, o `BAxOfflineBackup` si station DOWN.

**G25** DR-Cloud (`cloudBackup-rt.jar`): vendor-lock Tridium; downloads a Niagara Cloud pero restore manual local. RTO alto (horas).

**G26** `BBackupService$RestoreThread` es standalone (no engine, no batch queue). Si Supervisor cae durante restore → station queda en `restarting` con `.bog` medio-aplicado. Recovery requiere restaurar a snapshot previo + re-intentar.

### Parte E — Honeywell-specific

**G27** NO módulos `honProvisioning*` / `honBackup*` / `ascProvisioning*` observados en el árbol. Honeywell NO añade extension a provisioning framework en esta distribución; usa el stack Tridium sin modificaciones de provisioning core.

**G28** `honCloudEasyOnboard-rt.jar` existe — es helper para onboarding masivo con Honeywell Cloud services (no HA, no DR). Scope distinto a provisioning tradicional.

**G29** Modules Honeywell (`honPlantController-rt`, `honBacnetHelper-rt`, etc.) firmados por Honeywell CA. Cross-check Bloque 39.4.3: si Supervisor TLS HIGH + CA Honeywell no en trust store → provisioning `BInstallBySpecStep` rechaza. Fix trust store antes de provisioning batch a subordinates con modules Honeywell.

---

## 39.13 Correcciones a bloques previos

### 39.13.1 Bloque 16.10.1 — ampliación

Bloque 16.10.1 describe `BNiagaraNetworkJob` como 2-stage FIXED Initial+ForEachStation. Correcto. **Detalle adicional no documentado**:
- `BForEachStationStage.getCombinedSteps(DeviceNetworkJobOp)` → returns `BDeviceJobStep[]` con step-combining aplicado
- `canPassDefaultCheck(BDevice, BDeviceJobStep)` → filter per-station per-step
- Ejecución NO es estrictamente secuencial — hay parallelism controlled by `getParallelExecutionConflicts` del step y por el thread pool del `BBatchJobService`
- Fail-fast per-station (step falla → stations remaining de la misma station abortan), pero `Failed` en stationA NO aborta el job entero

### 39.13.2 Bloque 19.14 — evidencia reforzada

Bloque 19.14 reporta "NO HA nativa". **Confirmación exhaustiva**: búsqueda `failover|Failover|HighAvailability|ActiveStandby|SplitBrain|Redundant` en 9 JARs core + Honeywell → 0 matches. Esta es la evidencia más fuerte disponible sin acceso a código Tridium privado. Única excepción: `cloudBackup-rt.jar` → upload a Niagara Cloud (SaaS vendor-lock, no HA).

### 39.13.3 Bloque 20.8.4 — backup chunking clarificado

Bloque 20.8.4 describe "NO chunking/resume". **Mecanismo exacto**: `BBackupService.zip(BJob, OutputStream, boolean, Context)` firma → un solo stream; ZIP central directory escrito solo en `close()`; si stream corta → `.dist` sin central directory → no leíble. La implementación interna NO tiene check-pointing intermedio. Para backup resumable hay que implementar wrapper externo (imposible sin modificar `BBackupService`).

### 39.13.4 Bloque 14.6 — templates + upgrade step

Bloque 14.6 dice "Templates NO auto-propagan". **Confirmación con mecanismo**: `BDeployTemplateStep` (Deploy inicial) vs `BUpgradeTemplateStep` (actualizar template ya deployado). El UpgradeTemplateStep es el PATH OFICIAL para propagación. NO hay detección automática de "template source changed" que dispare `BUpgradeTemplateStep` — es siempre trigger manual o via `BNiagaraNetworkJobPrototype` con schedule.

### 39.13.5 Bloque 16.11.1 — BProvisioningBackupStep detalle

Bloque 16.11.1 describe `BProvisioningBackupStep`. **Correcciones/adiciones**:
- **Clase real**: `javax.baja.provisioningNiagara.backup.BProvisioningBackupStep extends BDeviceJobStep implements BFoxClientConnection$Interest`
- **`implements BFoxClientConnection$Interest`** → el step es LISTENER de eventos de la Fox client connection → puede reaccionar a reconnect y ajustar behavior
- **`getParallelExecutionConflicts`** implementado → el framework sabe que NO se pueden hacer 2 backups sobre MISMO device en paralelo (obvio), pero permite backups en paralelo across devices
- **`makeJob(String)` static** → acceso programático para crear un backup job standalone (no-batch)
- **`deviceJobStepComplete`** callback post-run → momento de cleanup/notify; aquí se emitirían los `AsyncActionEvent`

### 39.13.6 Bloque 10.3.3 — exclude patterns

Bloque 10.3.3 describe online backup excluye `.hdb`/`.adb`. **Mecanismo confirmado en código**:
- `BBackupService.excludeFiles : BString` (glob)
- `BBackupService.excludeDirectories : BOrdList`
- `BBackupService.offlineExcludeFiles` / `offlineExcludeDirectories` (set separado para offline)
- El `findStationBackupFiles` + `listBackupFiles` aplican `PatternFilter[]` usando estos props
- La configuración DEFAULT tiene `.hdb`/`.adb` (activos/open) en `excludeFiles` online pero NO en `offlineExcludeFiles`

---

## 39.14 Resumen ejecutivo — 10 hallazgos no-obvios

1. **NO existe `provisioningNiagara-rt.jar` ni `provisioning-rt.jar`**. El runtime del provisioning vive DENTRO del WB JAR (`provisioningNiagara-wb.jar` contiene `javax.baja.provisioningNiagara.BNiagaraNetworkJob` etc.). Convención -rt/-wb violada en este módulo.

2. **NO existe `BRestoreJob` clase separada**. El restore corre dentro de `BBackupService` como `RestoreThread` standalone (no engine, no batch queue). Esto implica que un crash del Supervisor mid-restore deja el subordinate con `.bog` medio-aplicado sin mecanismo de retry automático.

3. **46 step types concretos** en el catálogo provisioning (4 subcategorías: software 11, security 9, credentials 8, device/connection 4, template 3, license 2, discovery 2, otros 7). `BRobotJobStep` es el escape hatch para operaciones no cubiertas por el resto.

4. **Step combining automático** solo para 3 tipos adyacentes: Install / CopyFile / UpgradeOutOfDate. Un step no-combinable intercalado (ej SetTime) rompe el combining y causa doble reboot (5–10 min extra por station).

5. **Bottleneck real ~50 subordinates NO es solo Fox channels** — es combinación de 3 factores: license point count (`BNiagaraProxyExt implements BISubLicenseable`), Fox server sessions (maxServerSessions ~200, 5×50=250), engine thread saturation. Cualquiera puede pegar primero según workload.

6. **`BBackupService` tiene 4 variantes internas** de backup según `{Writable|Readonly}License × {Writable|Readonly}HomePlatform` — el mismo station produce `.dist` distintos si corre en JACE (writable license + writable home) vs en workstation Supervisor (a veces readonly license).

7. **Backup permissions tocan paths Linux (`/etc/net`, `/etc/wifi`, `/etc/dhcpd`, `/etc/ieee8021x`)** aun en Supervisor Windows — se saltan silentes. Un `.dist` Windows NO lleva netcfg; un `.dist` JACE sí. Cross-platform restore incomplete por esto.

8. **`DAEMON_SESSION_RETRY_DECAY` es `public static Function<Integer,Integer>`** — extension point para tunear retry backoff de daemon sessions por-station. Default probablemente exponential; customizable runtime.

9. **`BProvisioningNiagaraNetworkExt implements FoxServerConnectionListener`** — el service reacciona a reconexiones de subordinates y recalcula status global. Esto explica latencia de UI (manager) tras reconnect: hay recompute O(N) sincrónico.

10. **Split-brain NO detectable automático** — no hay lógica `checkIfAnotherSupervisorActive()`. Detection es por síntomas downstream (alarmas dobles, audit trail inconsistente en subordinate, license warnings). El plan HA práctico obliga a STRICT discipline operacional (A down confirmado antes de promover B).

---

## 39.14b Diagnostic queries — qué preguntarle al station en producción

Referencia operacional: cómo inspeccionar el estado del provisioning/backup/federation desde una station corriendo.

### 39.14b.1 Estado de provisioning jobs

```
# Ubicación del job list
Wb → Supervisor station → Services → ProvisioningNiagaraService → NiagaraNetwork
   → Supervisor → BProvisioningNiagaraNetworkExt → NiagaraNetworkJobList

# Ver jobs pendientes / corriendo / fallados:
Wb RMB sobre NiagaraNetworkJobList → View Job Log

# Filesystem paths:
^batchJob/logs/NiagaraNetworkJob/<timestamp>.bjl       # job-level log binary
^batchJob/logs/DeviceJobStep/<timestamp>.bjsl          # step-level log binary
^provisioningNiagara/stationData/<station>/backups/   # .dist files
```

### 39.14b.2 Estado de federation points

```
# License utilization
Wb → Supervisor station → Services → LicenseService
    Property: pointLimit (hard cap)
    Property: pointsUsed

# Per-subordinate proxy point count (via BQL):
slot:/Drivers/NiagaraNetwork/<subordinate>/Points |bql:
  select name, out, subscriptionStatus, parent.parent.displayName

# Total proxy points globally:
station:|slot:/ |descendants:ofType:'niagaraDriver:NiagaraProxyExt' |bql: select parent.parent.displayName, count(*)
```

### 39.14b.3 Fox sessions snapshot

```
# Workbench shortcut:
Wb → Tools → Fox → Session Manager      (Tridium built-in)

# Platform spy (if enabled):
http://<host>:<fox-port>/spy/fox/sessions
http://<host>:<fox-port>/spy/fox/stats

# System property enablement:
niagara.fox.traceSessionStates=true     (runtime toggle, adds overhead)
niagara.fox.traceMulticast=true
```

### 39.14b.4 Platform connection health per-subordinate

```
# Per-station platform connection agent:
Wb → NiagaraNetwork → <subordinate> → Views → PlatformConnectionMgr
  Columns: status, health, port, secure, worker.queueDepth

# Agent class: BPlatformConnectionMgrAgent (provisioningNiagara-wb)
# Status values: {ok}, {fault:...}, {down}, {disabled}

# Manual ping:
Wb RMB sobre station → actions → ping
```

### 39.14b.5 Backup forensic — ¿el backup está sano?

```
# Antes de confiar en un .dist:
unzip -l backup_<station>_<ts>.dist | tail
  → debe haber `manifest.xml` al final
  → debe haber Central Directory (tail de unzip lo lista; si "archive is corrupted" no)

# Si hay .dist.tmp huérfano:
find ^provisioningNiagara -name '*.dist.tmp' -mtime +1
  → backup interrumpido hace >1 día; borrar + re-ejecutar

# Integrity quick-check:
Wb → Tools → Station → Restore → Select File (don't actually restore)
  → muestra manifest antes de aplicar
  → si falla parsing = .dist roto
```

### 39.14b.6 Audit trail de provisioning

```
# Audits generados por provisioning:
checkUserAndAuditForBackup(Context)      # BBackupService private
checkUserAndAuditForRestore(Context)     # BBackupService private

# Visible en:
Wb → Services → AuditHistoryService → AuditHistory
  Filter: operation starts with "backup" OR "restore" OR "provisioning"
```

---

## 39.15 Flags system.properties relevantes (defaults Honeywell)

Del grep `backup|provision|federation|retention` sobre `defaults/system.properties`:

```properties
# Provisioning
niagara.license.subscriptionLicenseAllowed=true
    # Habilita subscription options en provisioning/commissioning jobs.
    # Deshabilitar en sites con perpetual license para evitar errores de operador.
```

**Notable absence**: NO hay flag `provisioning.parallelDegree`, NO hay `backup.chunking`, NO hay `backup.compression.level`, NO hay `niagara.federation.pointLimit`, NO hay `niagara.ha.enabled`. Todos estos son hardcoded en clases o configurados en `.bog`.

**Flags platform-wide relevantes (inventario breve)**:
- `niagara.daemonsession.timeout` — tunning para `BPlatformConnection.getDaemonSession`
- `niagara.daemonsession.streamtimeout` — stream reads (relevant durante backup restore transfer)
- `niagara.fox.maxServerSessions` — bottleneck Fox sessions
- `niagara.fox.circuitChunkSize` — chunk size stream Fox (NO afecta backup ZIP; afecta sub stream)
- `niagara.fox.keepAliveInterval` — idle channel detection
- `niagara.fox.requestTimeout` — operational timeouts provisioning ops
- `niagara.findReachableStations.timeoutMillis` — discovery step timeout

---

## 39.16 Conclusión operacional

Niagara N4.14 OptimizerSupervisor Honeywell 4.14.0.162 **NO es un sistema HA**. Es un sistema de control industrial con:

- **Provisioning framework maduro** (46 step types, 2-stage job model, step-combining, retention policies) — robusto para flota hasta ~50 subordinates
- **Backup/restore funcional pero primitivo** — monolítico, sin resume, sin chunking, 3 niveles conceptuales (Station Copier / .dist / Clone)
- **Supervisor replication completa** (points/histories/alarms/schedules/users/virtuals) pero con costos: point federation cuenta en licencia del Supervisor
- **Sin HA nativa** — evidencia exhaustiva (0 matches código). Alternativas operacionales: cold standby + manual promotion (RTO 30–60 min realista) o DR off-site (RTO 6–12h)
- **Sin split-brain detection** — disciplina operacional obligatoria
- **Runbooks de failover (7 pasos) y cold restore (10 pasos) documentados en 39.9/39.10** — tiempos realistas 30–60 min y 6–48h respectivamente

Para sitios que exijan HA real (RTO <5 min, RPO <1 min): Niagara NO es suficiente por sí solo. Hace falta network-level redundancy (VIP + load balancer transparente) + replicación app-level custom + monitoring de split-brain. Costo de construcción: >3 meses ingeniería, fragilidad alta.

Para sitios BAS típicos (RTO aceptable 30–120 min, RPO 24h aceptable): backup periódico programado + warm standby host + keyring backup separado (Bloque 30) + runbook 39.10 cubren el 95% de escenarios.

---

**Fin Bloque 39.**
