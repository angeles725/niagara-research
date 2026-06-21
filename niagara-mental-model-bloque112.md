# Bloque 112 — Detección y forense post-incidente del vector del [Bloque 75]: **nss SecurityDashboard** + **daemon `console.log`** + **PolicySpy** — la contracara DEFENSIVA

> Investigación de **respuesta a incidente DEFENSIVA**. El [Bloque 75] reconstruyó la **cadena de ataque** (módulo no firmado declara `NETWORK_COMMUNICATION`, abre 443 vía `NiagaraSocketPermission`, y borra el audit trail con `Sys.setAuditor(null)`), y propuso un plan de **PREVENCIÓN** P0/P1/P2. Este bloque es su **contracara**: las capacidades nativas de **DETECCIÓN** (¿cómo veo el módulo malicioso, antes o después?) y **FORENSE** (¿qué evidencia del deploy sobrevive al borrado?), las tres ya presentes en el corpus N4.14 y casi siempre **apagadas o no consultadas** por default.
>
> **Qué es** `[CERT]`: tres herramientas defensivas reales, una por eje. (1) **nss Security Dashboard** (`com.tridium.nss.dashboard.*`) — el panel que **lista cada módulo por nivel de riesgo de sus permission groups y por estado de firma**, cruzando *declara permiso peligroso* × *está firmado*. (2) El **log del platform daemon** (`{niagara_user_home}/logging/console.*`) — evidencia del deploy que vive a **nivel OS, fuera de la station**, que un atacante que solo compromete la station **no borra**. (3) **PolicySpy** (`com.tridium.security.PolicySpy`) — la spy page que renderiza en runtime, módulo por módulo, **qué permission groups tiene y con qué parámetros** (`hosts=* ports=443`).
>
> **El hallazgo de anclaje** `[CERT]`: el vector del B75 **es detectable con herramientas nativas que ya están en la station**. El módulo malicioso que el B75 reconstruyó —no firmado + `NETWORK_COMMUNICATION ports=443`— aparece como **WARNING en DOS subsecciones del Security Dashboard** (permisos + firmas), aparece en la **tabla de PolicySpy con `ports=443` legible**, y su deploy deja huella en el **`console.log` del daemon** que el `Sys.setAuditor(null)` **no toca**. La detección es **independiente del gate de prevención**: aunque `moduleVerificationMode=low` deje **cargar** el módulo (fail-open, B75 §75.3), el dashboard **igual lo marca**. La defensa no falló por falta de herramientas; falló porque estaban **off / no monitoreadas**.
>
> Fuentes READ-ONLY (vineflower): `nss/nss-rt/.../com/tridium/nss/dashboard/{BSecurityDashboardModulePermissions,BSecurityDashboardModuleSignatures,SecurityDashboardServlet}.java` + `nss/.../BSecurityService.java`; `baja/baja/.../com/tridium/security/{PolicySpy,SecuritySpyDir,NetworkCommunicationPermissionGroup}.java` + `.../sys/module/ModuleClassLoader.java` + `.../sys/Nre.java`; `platform/platform-rt/.../install/{ModuleSignatureStatusEnum,InstallScenario}.java` + `.../daemon/{BDaemonSession,DaemonFileUtil}.java` + `.../BBackupService.java`; `platDaemon/platDaemon-rt/.../command/BModuleInstallCommand.java`; `program/program-rt/.../program/BCode.java`.
> Método: **verificación directa file:line** del flujo de cada dashboard, de la spy page, y del logging del install/daemon. `[CERT]` = leído verbatim; `[INFER]` = deducción marcada. Conteo honesto: **~18 clases vineflower** citadas, cross-módulo (no hay módulo único que contar; sin inflado por triple-decompilado — solo el path `vineflower/`).
>
> Capa 21 (Test infrastructure + Security incident), **junto al [Bloque 75]**: este bloque es la **capa de DETECCIÓN** que complementa la **capa de PREVENCIÓN** del B75. Cross-ref fuerte: [Bloque 75] (ancla — vector + hardening), [Bloque 18] (module signing + permissions), [Bloque 27] (network surface), [Bloque 30/31] (audit + key rotation).

---

## 112.0 — El reframe: el B75 previene, este detecta y reconstruye `[CERT]`

El B75 cerró con un veredicto incómodo: con la config de fábrica, el incidente es **el comportamiento esperado**, no un exploit. Su plan ataca la **prevención** (firma obligatoria, daemon TLS-only, blindar `skipModuleValidation`). Pero el cliente preguntó dos cosas que la prevención no responde:

1. **«¿Cómo detecto si HOY tengo un módulo así cargado?»** → eje 1 (nss Dashboard) + eje 3 (PolicySpy).
2. **«El malware se autolimpió. ¿Quedó ALGUNA evidencia del deploy?»** → eje 2 (daemon `console.log`).

La tesis de este bloque `[CERT]`: **las tres capacidades existen, son nativas, y el vector del B75 es visible en las tres.** El problema operativo no es de herramientas sino de **postura**: el dashboard no se consulta, el syslog está off (B75 §75.4 P2), y el `console.log` del daemon nadie lo mira post-mortem.

---

## 112.1 — Eje 1: el **nss Security Dashboard** detecta el módulo malicioso `[CERT]`

El servicio `nss:SecurityService` (`BSecurityService extends BAbstractService`, `:102`) agrega **provider agents** que cada uno aporta una subsección al dashboard. Dos de ellos son exactamente lo que un defensor necesita contra el vector del B75.

### 112.1.1 — `BSecurityDashboardModulePermissions`: riesgo de permisos × firma `[CERT]`

`extends BObject implements BISecurityDashboardProviderAgent`, `@AgentOn(types={"nss:SecurityService"})` (`:34-39`). Su `getSecurityDashboardItems()` (`:79`) hace lo siguiente `[CERT]`:

1. Construye el set de **módulos confiables** (`trustedModules`) = los que están en estado de firma `OK` + `SIGNER_SELF_SIGNED` + `TIMESTAMP_SELF_SIGNED`, **quitando** los `CERT_PATH_VALIDATION_FAILURE` (`:91-96`). O sea, *confiable = firmado y con cadena válida*.
2. Recorre **todos** los módulos (`DefaultModulesFileManager.init`, `:199`); para cada uno que `hasNiagaraPermissions()` (`:214`), resuelve sus permission groups vía `NiagaraPolicyUtil.getAllPermissionGroups(moduleUrl)` bajo `doPrivileged` (`:232`).
3. **Clasifica cada módulo por el `getRiskLevel()` de sus grupos × si está en `trustedModules`** (`:233-254`):

| `RiskLevel` del grupo | Módulo NO confiable (no firmado) | Módulo confiable (firmado) |
|---|---|---|
| `SEVERE` | `severePermissionGroupModulesSet` → **`makeWarning`** | `okSevere…` → `makeOk` |
| `MODERATE` | `moderatePermissionGroupModulesSet` → **`makeWarning`** | `okModerate…` → `makeOk` |
| `MILD` | `mildPermissionGroupModulesSet` → **`makeWarning`** | `okMild…` → `makeOk` |

**El anclaje exacto al B75** `[CERT]`: `NetworkCommunicationPermissionGroup` es **`RiskLevel.MODERATE`** (`NetworkCommunicationPermissionGroup.java:43`, `super(RiskLevel.MODERATE, …)`). Por lo tanto, el módulo malicioso del B75 —no firmado, declara `NETWORK_COMMUNICATION`— cae en `moderatePermissionGroupModulesSet` y se emite como **`makeWarning`** (`:117-124`), con el **nombre del módulo en el summary** (`severePermissionGroupModulesSet.toString()`). Un defensor que abre el dashboard ve: *«módulos no confiables con permisos de comunicación de red: [nombre_del_modulo_malicioso]»*.

> **La señal clave** `[CERT]`: el dashboard **cruza riesgo × firma**. Un módulo Honeywell legítimo firmado que abre BACnet aparece como **OK** (`okModerate`); el módulo no firmado que abre 443 aparece como **WARNING** en la misma subsección. Es exactamente el discriminante que el B75 identificó como ausente en el gate de carga (`NETWORK_COMMUNICATION.requiresSignature()=false`): aquí la firma **sí** discrimina, no para *bloquear* sino para *señalar*.

### 112.1.2 — `BSecurityDashboardModuleSignatures`: el modo de verificación + el bucket `UNSIGNED` `[CERT]`

`extends BObject implements BISecurityDashboardProviderAgent` (`:32`). Aporta **dos cosas** que pegan directo al B75:

**(a) Flag del `moduleVerificationMode`** (`addVerificationModeItem`, `:75`) `[CERT]`:
```java
if (this.verificationMode.compareTo(ModuleVerificationMode.DEFAULT) < 0) status = securityStatusWarning;
else if (this.verificationMode == ModuleVerificationMode.high) status = securityStatusOK;
else status = securityStatusInfo;
```
Solo **`high`** se premia con OK; cualquier modo por debajo del DEFAULT se marca **WARNING**. Es decir: el dashboard **señala directamente el `moduleVerificationMode=low`** que el B75 §75.4 P0.1 identificó como el eslabón a corregir. La debilidad de configuración del B75 **es un ítem de warning explícito** en este panel `[CERT]`. *(El orden exacto `low` vs `DEFAULT` vive en el enum `com.tridium.nre.security.ModuleVerificationMode`, fuera del corpus vineflower; que solo `high`→OK es `[CERT]`.)*

**(b) Lista por estado de firma** (`addSignatureStatusItems`, `:94`) `[CERT]`: agrupa los módulos por `ModuleSignatureStatusEnum` (`OK, NOT_TIMESTAMPED, UNKNOWN, SIGNER_SELF_SIGNED, TIMESTAMP_SELF_SIGNED, CERT_PATH_VALIDATION_FAILURE, CERT_PATH_VALIDATION_WARNING, UNSIGNED, INVALID_SIGNATURE`, `ModuleSignatureStatusEnum.java:16-24`). El módulo malicioso del B75 cae en **`UNSIGNED`**, y el nivel del ítem se decide así (`:107-112`):
```java
else if (signatureStatus.isAcceptable(this.verificationMode)) itemStatus = securityStatusWarning;  // amarillo
else                                                          itemStatus = securityStatusAlert;    // rojo
```
La sutileza forense `[CERT]`: con `verificationMode=low`, `UNSIGNED.isAcceptable(low)` cae al `default: return true` del switch (`ModuleSignatureStatusEnum.java:36-41` — `low` solo rechaza `INVALID_SIGNATURE`) → el módulo es *aceptable* (carga, fail-open del B75) **pero el ítem sale igual como WARNING**. Con `medium`/`high`, `UNSIGNED` no es aceptable (`:33-34`) → sale como **ALERT (rojo)**. **En cualquier modo, el módulo no firmado figura en el bucket `UNSIGNED` y queda flagueado.** Detección desacoplada de prevención.

### 112.1.3 — `BSecurityService`: el dashboard es **consultable por programa y por toda la red**, no solo UI `[CERT]`

Crítico para incident response a escala: el dashboard **no es solo una pantalla**.

- **JSON programático** `[CERT]`: `getStationDashboardData(Context cx)` (`:398`) devuelve un `JSONObject` `{version, stationName, timestamp, sections[]→subsections[]→items[]}` recorriendo **todos** los provider agents (`:407-417`). Cada ítem se serializa con `item.toJSON()` (`:470`). Gateado por `getPermissions(cx).hasAdminRead()` (`:401`, lanza `PermissionException` si no).
- **Auto-descubrimiento de providers** `[CERT]`: `:505` itera `Sys.getRegistry().getConcreteTypes(BISecurityDashboardProviderAgent.TYPE…)` — los dos agentes de 112.1.1/112.1.2 se recogen solos.
- **Network-wide vía Fox** `[CERT]`: `getSystemDashboardData(cx)` (`:553`) agrega la postura de **todas las stations alcanzables** de la Niagara Network; el canal se registra como `BFoxSecurityDashboardChannel` (`:193`, `registry.add("securityDashboard", …)`). `getSystemDashboardDataForStation(stationName, …)` (`:666`) consulta una station remota puntual. Hay versionado de protocolo `VERSION_4_8`/`VERSION_4_14` (`:631`).
- **Persistible** `[CERT]`: propiedad `saveDashboardDataToBog` (`:85`) y acción `refreshSystemDashboardData` (`:97-112`).

> **Implicación de respuesta a incidente** `[CERT]`: desde un **Supervisor** un defensor puede pedir por Fox el `getSystemDashboardData` y obtener, en **JSON**, la lista de módulos no firmados con permisos de red de **cada station de la flota** — el barrido exacto para encontrar el módulo del B75 replicado. La detección es **automatizable y federada**, no una inspección manual estación por estación.

**Cómo se accede (UI)** `[CERT]`: `SecurityDashboardServlet extends HttpServlet` (`:21`, logger `"securityDashboard"`) sirve el panel; en Workbench es la vista del `SecurityService`. Logger transversal de todo el eje: **`"securityDashboard"`** (`BSecurityService:117`, ambos dashboards, el servlet).

---

## 112.2 — Eje 2: forense vía el **`console.log` del daemon** + IOCs de warnings `[CERT]`

El B75 §75.2 H1 explica el «no dejó rastro»: `Sys.setAuditor(null)` apaga el audit **interno** de la station sin permiso. Pero hay un log que **vive fuera** de la station y que ese borrado no alcanza.

### 112.2.1 — Dónde vive el log del daemon (ruta en disco) `[CERT]`

La pieza que lo fija `[CERT]`: `BBackupService.java:165`:
```java
COMMON_PLATFORM_BACKUP = { BOrd.make("file:~daemon"), BOrd.make("file:~etc"), BOrd.make("file:~logging") };
```
`~logging` resuelve a `{niagara_user_home}/logging/`. Y la prueba del **nombre del fichero** `[CERT]`: `BBackupService.java:149`, el default de `excludeFiles` incluye **`console.*`** entre los patrones excluidos del backup → los logs del daemon se llaman **`console.log`, `console.log.0`, `console.log.1`…** y viven en `~logging`. Config asociada en `{niagara_user_home}/daemon/daemon.properties` (`BSystemPlatformServiceNpsdk:206`). Path concreto `[INFER]` por convención de ORD (no literal en el Java): Linux `…/niagara_user_home/logging/console.log`, Windows `C:\ProgramData\Tridium\niagara<ver>\logging\console.log`.

**Quién lo escribe y por qué el atacante no lo borra** `[CERT/INFER]`: el daemon server (`niagarad`) es **binario nativo** — no hay Java decompilado de su loop. El directorio `~logging` lo **posee el proceso niagarad a nivel OS**, no la JVM de la station. Un atacante que compromete la *station* (la JVM) **no tiene el handle del proceso daemon** para reescribir `console.log` `[INFER, fuerte]`: **no existe en el corpus Java ninguna API que borre el log del daemon desde la capa Baja** —contraste directo con `Sys.setAuditor(null)` (`Sys.java:178`) que sí apaga el audit interno—. Esa asimetría **es** la oportunidad forense.

**Cómo se lee post-incidente** `[CERT]`: el daemon expone su log por dos servlets HTTP — `/systemlog` (`GetSystemLogMessage.java:16`, opcional `?log=<nombre>`) y `/getdaemonoutput` (`GetDaemonOutputMessage.java:7`, el buffer de consola en vivo, con `follow`/`updatesonly`). Existe además un directorio dedicado `~audits` (`SystemFilePaths.java:32`, `PLATFORM_AUDIT_DIR_PATH`) para artefactos de audit de plataforma.

### 112.2.2 — Qué registra el deploy (y qué NO) `[CERT]`

Honestidad forense: el logging **client-side** (la JVM que *inicia* el deploy) es **pobre**. Lo que sí queda en la capa Java `[CERT]`:

| Fuente | Logger | Qué registra |
|---|---|---|
| `BModuleInstallCommand.java:27` (CLI `plat moduleinstall`) | `"moduleinstall"` | `"Stopping stations on " + host` (`:128`), `"Installation complete."` (`:131`), `"Started " + appSurrogate` (`:136`), `"cannot commit installation"` severe (`:125`) — **host name + éxito/fallo** |
| `InstallScenario.java:113` | `"platform.install"` | a nivel `FINE`: *«Unable to install module %s. Signature status %s is not acceptable for verification mode %s»* (`:314-323`) |
| `DaemonFileUtil.java:535` | `"platform.daemonSession.transfer"` | *«%,d bytes transferred, … Speed …»* — **bytes + duración, NO el filename, NO el usuario** |
| `BDaemonSession.acquireCredentials()` `:337-352` | — | **CERO logging de auth** (no usuario, no IP, no éxito/fallo en la capa Java) |

**Conclusión `[CERT]`**: el **usuario, la IP origen, el nombre exacto del JAR y el timestamp** del deploy **no** aparecen en ningún `Logger` Java del flujo de install. Esos campos —los que el cliente del B75 necesitaba para atribución— los captura el **daemon nativo** y terminan en `console.log`. Por eso el `console.log` del daemon **es la fuente forense primaria** del deploy: es donde el `niagarad` server registra la conexión autenticada y la operación de install, fuera del alcance del borrado interno.

### 112.2.3 — Corroboración del ancla: la firma se valida **client-side**, el daemon no `[CERT]`

Hallazgo que **refina** el B75 §75.1 paso 3. El chequeo de firma del install vive en `InstallScenario.solve()` —**en la JVM del cliente/Workbench**, antes del `commit`— (`InstallScenario.java:302-326`: `getSignatureStatus(certValidator)` → `if (!status.isAcceptable(verificationMode)) signatureError=true`). El **daemon server (`niagarad`) no tiene chequeo Java equivalente**: acepta y commitea lo que llegue autenticado por el file-transfer. Dos bypass visibles en el propio `InstallScenario` `[CERT]`:

- **`niagara.commissioning.ignoreVerificationMode=true`** (`InstallScenario.java:291`) → fuerza `verificationMode = low` en el gate de install, **aunque la station esté en `medium`/`high`**. Es un sysprop de comisionamiento que neutraliza el gate del lado cliente.
- **`PlatformFileManager.put()` directo** → entrega el JAR al daemon **salteando `InstallScenario` por completo** `[INFER, fuerte]`: si el deploy no pasa por `solve()`, no hay chequeo client-side, y el server no valida.

> **Refinamiento al [Bloque 75]** `[CERT]`: hay **DOS gates de firma**, no uno. (a) **Gate de carga** server-side — `ModuleManager.verifyModuleSignature` gobernado por `moduleVerificationMode` (B75 §75.3, el que el B75 analizó). (b) **Gate de install** client-side — `InstallScenario.solve`, **bypassable** con `niagara.commissioning.ignoreVerificationMode=true`. El B75 P0.1 (`moduleVerificationMode=high`) cierra (a) —correcto y suficiente para *bloquear la carga*— pero `ignoreVerificationMode` debería sumarse al `commandLineBlacklist` junto a `skipModuleValidation` (B75 §75.4 P0.3): es otra palanca CLI que debilita la verificación.

### 112.2.4 — Los IOCs de warnings: el bytecode no firmado deja rastro `[CERT]`

Cuando el módulo no firmado **carga** (con `low`), el classloader **loguea** —esos warnings son IOCs persistentes en el log de la station (no en el audit que se borró)— `[CERT]`:

- `ModuleClassLoader.java` (logger **`"loader"`**, `:600`): constante `UNSIGNED = "No code signers for entry %s in module %s. Signed modules will be required in a future release."` (`:73`), emitida en `:397-407`. Matiz `[CERT]`: el nivel es `WARNING` **solo la primera vez** (`:395` `Level level = !this.loggedWarning && canCheckTpk ? Level.WARNING : Level.FINEST`); luego baja a `FINEST` y setea `loggedWarning=true` (`:407`). Es decir, **un warning one-shot por instancia de classloader** — fácil de perder si el log rota o si nadie filtra a tiempo.
- También `SIGNER_SELF_SIGNED` (`:74`, emitido `:432`) — si el atacante usó un cert self-signed en vez de no firmar.
- `BCode.java:214` (program objects): `log.warning(lex.getText("program.notSigned"))` — para la **ruta alternativa** del B75 §75.1 (BProgram sin firma ejecutando bytecode). También `program.notTimestamped` (`:208`).

---

## 112.3 — Eje 3: **PolicySpy**, la inspección de la policy activa en runtime `[CERT]`

`PolicySpy extends javax.baja.spy.Spy` (`PolicySpy.java:19`). Es la página que un investigador abre **en runtime** para ver **qué permisos tiene realmente cada módulo cargado**, leyendo la **policy activa de la JVM** (no el `module.xml` en disco — la policy efectiva).

**Qué muestra** `[CERT]`: `write(SpyWriter)` (`:38`) recorre `DefaultModulesFileManager.get().init(false)` y, por cada módulo con `hasNiagaraPermissions()`, hace `getAllPermissionGroups(moduleUrl)` (`:89-91`) y emite una **tabla HTML** `módulo → [permission groups]`. Por cada grupo, `writeGroupDescription` (`:112`) renderiza cuatro campos `[CERT]`:

| Campo | Fuente | Para el vector del B75 muestra |
|---|---|---|
| `type` | `group.getType()` | `NETWORK_COMMUNICATION` |
| `purpose` | `group.getPurpose()` | el `purposeKey` declarado |
| **`params`** | **`group.getParameters()`** | **`hosts=* ports=443`** ← el smoking gun |
| `riskLevel` | `group.getRiskLevel()` + icono | **MODERATE** (círculo **dorado**, `circleGold.png`, `:138`) |

**El smoking gun `[CERT]`**: el `NetworkCommunicationPermissionGroup` guarda en su mapa `parameters` las claves `hosts` y `ports` tal como vinieron del `module.xml` (`NetworkCommunicationPermissionGroup.java:25-26, 49-60`). PolicySpy llama `group.getParameters()` (`PolicySpy:119`) y **lo renderiza literalmente**. Es decir, la spy page muestra, en texto plano, **el módulo malicioso con `ports=443` y `hosts=*`** — la prueba directa, en runtime, de que ESE módulo tiene permiso para abrir 443. Más aún: el `type` del grupo (`server`/`all`) revela si pidió `accept, listen` (server socket — exactamente `new SSLServerSocket(443)` del B75), porque las actions se derivan de ahí (`:69-81`).

**Cómo se accede** `[CERT]`: PolicySpy se registra dentro de `SecuritySpyDir` (`SecuritySpyDir.java:9`, `add("Policy Information", policySpy)`), montado en `Spy.ROOT` bajo `"securityInfo"` (`Nre.java:731`, `Spy.ROOT.add("securityInfo", new SecuritySpyDir())`). Ruta de spy: **`spy:/securityInfo/Policy Information`** — accesible desde Workbench (vista *Spy*) o por URL de spy de la station (`http://<host>/ord/spy:/securityInfo/...`). Junto a PolicySpy hay un `HsmSpy` si hay engine HSM (`:12-14`).

**Control de acceso** `[CERT]`: el constructor exige `NiagaraBasicPermission("VIEW_NIAGARA_POLICY")` (`PolicySpy.java:26-28`) — ver la policy es en sí un permiso. Bien para un defensor con admin; nota de hardening: ese mismo permiso le da a un atacante autenticado el mapa completo de qué módulo puede qué.

> **PolicySpy vs Dashboard** `[CERT]`: el **Dashboard** (eje 1) es *agregado y federable* (¿hay algún no-firmado con red? lista por flota, JSON) — bueno para **barrido**. **PolicySpy** es *crudo y exacto* (este módulo tiene EXACTAMENTE estos grupos con ESTOS params) — bueno para **confirmación forense** del hallazgo. Se complementan: el dashboard te dice *dónde mirar*, PolicySpy te muestra *el `ports=443`*.

---

## 112.4 — IOCs concretos (qué buscar) `[CERT]`

| # | IOC | Dónde | Evidencia file:line |
|---|---|---|---|
| IOC-1 | Módulo en **WARNING** bajo *«non-trusted modules with MODERATE permission groups»* | nss Dashboard → Module Permissions | `BSecurityDashboardModulePermissions:117-124` + `NetworkCommunicationPermissionGroup:43` |
| IOC-2 | Módulo en el bucket **`UNSIGNED`** (amarillo en `low`, rojo en `medium`/`high`) | nss Dashboard → Module Signatures | `BSecurityDashboardModuleSignatures:94-115` + `ModuleSignatureStatusEnum:23` |
| IOC-3 | Ítem **WARNING** *«verification mode below default»* | nss Dashboard → Module Signatures | `BSecurityDashboardModuleSignatures:77-78` |
| IOC-4 | Fila en PolicySpy con `type=NETWORK_COMMUNICATION`, `params: hosts=* ports=443`, riesgo MODERATE (dorado) | `spy:/securityInfo/Policy Information` | `PolicySpy:112-121` + `NetworkCommunicationPermissionGroup:49-60` |
| IOC-5 | Log line `"No code signers for entry … in module <X>"` (logger `loader`, WARNING one-shot) | log de la station (`console.log` / JUL) | `ModuleClassLoader:73,395-407` |
| IOC-6 | Log line `program.notSigned` (logger de program) | log de la station | `BCode:214` |
| IOC-7 | En `console.log` del daemon: conexión autenticada al daemon + operación de install (usuario/IP/timestamp del deploy) | `{niagara_user_home}/logging/console.*` (vía `/systemlog`, `/getdaemonoutput`) | `BBackupService:149,165`; `GetSystemLogMessage:16`; `GetDaemonOutputMessage:7` |
| IOC-8 | Sysprop sospechoso `niagara.commissioning.ignoreVerificationMode=true` o `skipModuleValidation` activos | line args / `system.properties` | `InstallScenario:291`; (B75 §75.4 P0.3) |
| IOC-9 | `Sys.setAuditor(null)` ⇒ **ausencia abrupta** de entradas de audit tras un timestamp (el «hueco» es la señal) | AuditHistory de la station | `Sys.java:178` (B75 H1) |

---

## 112.5 — Matriz: señal de compromiso → dónde detectarla → qué la borra / qué no `[CERT]`

| Señal de compromiso | Dónde se detecta (file:line) | ¿La borra el atacante station-only? |
|---|---|---|
| Módulo no firmado declara `NETWORK_COMMUNICATION ports=443` | nss Dashboard (IOC-1/2) `BSecurityDashboardModulePermissions:117`; PolicySpy (IOC-4) `PolicySpy:119` | **NO** — se computa **en vivo** desde la policy/los módulos en disco; no es un log que se borre. Solo desaparece si **se quita el módulo** (lo cual ya es remediación). |
| `moduleVerificationMode` debilitado (`low`) | nss Dashboard (IOC-3) `BSecurityDashboardModuleSignatures:77` | **NO** — refleja config viva (`Nre.getModuleVerificationMode()`). |
| Carga del módulo no firmado | warning `"No code signers"` (IOC-5) `ModuleClassLoader:397` | **PARCIAL** — vive en el log de la station; **borrable** si el atacante silencia JUL (`LoggingPermission("control")`, B75 §75.2) o rota el log. **One-shot** ⇒ frágil. |
| El **deploy** del JAR (usuario, IP, timestamp, operación) | `console.log` del daemon (IOC-7) `BBackupService:165` + `/systemlog` | **NO** — vive a nivel OS, lo escribe el **niagarad nativo**; sin API Baja para borrarlo desde la station. **Evidencia forense primaria.** |
| Borrado del audit interno | hueco temporal en AuditHistory (IOC-9) `Sys.java:178` | **El borrado ES la acción**; la *ausencia* (el hueco) es la señal. La pre-imagen solo sobrevive si hubo **syslog offload** (B75 §75.4 P2). |
| Reinicio de la station para cargar el módulo | `console.log` daemon: `"Stopping stations on …"` / restart | **NO** — server-side daemon, `BModuleInstallCommand:128`. |

> **La lectura defensiva** `[CERT]`: las señales de **estado** (módulo presente, modo de verificación, permisos efectivos) son las **más robustas** porque se computan en vivo y no son borrables sin deshacer el ataque — y son justo lo que el **Dashboard + PolicySpy** exponen. Las señales de **evento** (logs) son borrables **dentro** de la station (audit, JUL) pero **no** en el daemon (`console.log`). Conclusión operativa: para *detectar presencia* usá Dashboard/PolicySpy; para *reconstruir el deploy* usá el `console.log` del daemon.

---

## 112.6 — Cómo encaja con el hardening del [Bloque 75]: la capa de DETECCIÓN `[CERT]`

El B75 entregó **prevención** (P0/P1) y un esbozo de **detección/evidencia** (P2). Este bloque **instrumenta P2** y agrega la **detección de estado** que P2 no cubría:

| Capa | B75 (prevención) | B112 (detección/forense) |
|---|---|---|
| **Antes** del incidente | P0.1 `moduleVerificationMode=high` bloquea la carga | nss Dashboard como **monitoreo continuo**: IOC-1/2/3 alertan del módulo no firmado con red y del modo débil. Barrido por flota vía Fox (`getSystemDashboardData`). |
| **Durante** | P1 daemon TLS-only + firewall reduce entrada | `console.log` del daemon captura la conexión autenticada y el deploy (IOC-7). |
| **Después** | P2 syslog offload preserva el audit antes del borrado | Dashboard/PolicySpy confirman *presencia* del módulo (IOC-4); `console.log` reconstruye *quién/cuándo/desde dónde* (IOC-7); IOCs de warnings (IOC-5/6) dan *cuándo cargó*. |

**Recomendaciones operativas que añade este bloque** (complementan, no reemplazan, el P0/P1/P2 del B75):

1. **Monitorear el nss Security Dashboard** (idealmente por Fox desde el Supervisor) y **alertar** ante cualquier ítem WARNING/ALERT en *Module Permissions* o *Module Signatures*. Es detección **continua y gratuita** — ya está en la station.
2. **Cosechar `~logging/console.*` del daemon a un store externo** (junto con el syslog del B75 P2). Es la evidencia que sobrevive al borrado interno.
3. **Sumar `niagara.commissioning.ignoreVerificationMode` al `commandLineBlacklist`** junto a `skipModuleValidation` (extiende B75 P0.3): cierra el bypass del gate de install client-side (112.2.3).
4. **Incluir PolicySpy en el runbook de IR**: ante sospecha, `spy:/securityInfo/Policy Information` confirma en runtime el `ports=443` del módulo (IOC-4) — prueba directa para el informe.

---

## 112.7 — Hallazgos CERT, corrigenda y cierre

**Hallazgos CERT (uno por eje)**:
1. **Detección (nss Dashboard)**: el módulo del B75 —no firmado + `NETWORK_COMMUNICATION` (RiskLevel.MODERATE)— sale como **WARNING en dos subsecciones** (permisos: `BSecurityDashboardModulePermissions:117`; firmas/bucket UNSIGNED: `BSecurityDashboardModuleSignatures:94`), y el modo `low` se flaguea solo (`:77`). Es **consultable por JSON y federable por Fox** (`BSecurityService:398,553`), con `hasAdminRead`. **Detección desacoplada de la prevención**: aunque `low` deje cargar, el dashboard marca igual.
2. **Forense (daemon `console.log`)**: el log del daemon vive en **`{niagara_user_home}/logging/console.*`** (`BBackupService:149,165`), lo escribe el **niagarad nativo a nivel OS**, se lee por `/systemlog` + `/getdaemonoutput`, y **no hay API Baja para borrarlo desde la station** — sobrevive al `Sys.setAuditor(null)`. Es la fuente que conserva usuario/IP/timestamp del deploy, ausentes del logging Java client-side.
3. **Inspección (PolicySpy)**: `spy:/securityInfo/Policy Information` (`Nre.java:731`) renderiza módulo→grupos con **`params: hosts=* ports=443`** literal (`PolicySpy:119` + `NetworkCommunicationPermissionGroup:49`) — la prueba runtime directa. Gateada por `VIEW_NIAGARA_POLICY`.

**Corrigendum / refinamiento al [Bloque 75]** `[CERT]`:
- **Dos gates de firma, no uno** (112.2.3): además del **gate de carga** server-side (`ModuleManager.verifyModuleSignature` / `moduleVerificationMode`, que el B75 analizó), existe un **gate de install** client-side (`InstallScenario.solve`, `:302`), **bypassable** con `niagara.commissioning.ignoreVerificationMode=true` (`:291`). Recomendación nueva: blacklistear ese sysprop (extiende P0.3). El B75 P0.1 sigue siendo correcto y suficiente para *bloquear la carga*; este refinamiento añade que el chequeo de *install* es independiente y tiene su propia palanca de bypass.
- **Refuerzo de P2**: el B75 §75.4 P2 mencionó el daemon log «a nivel OS» de forma genérica; este bloque lo **fija a `~logging/console.*`** y a los servlets `/systemlog`/`/getdaemonoutput`, y documenta **por qué** el atacante station-only no lo borra (no hay API Baja equivalente a `Sys.setAuditor(null)` para el daemon).

Sin corrección a otros bloques. La premisa del B75 (firma ≠ red; audit borrable sin permiso) queda intacta; este bloque **no** la contradice — muestra que, pese a esas debilidades de *prevención*, la *detección* del vector estaba disponible y desaprovechada.

**Pendiente conocido**: el interior del `niagarad` nativo (el formato exacto de las líneas de `console.log` para un install, los campos literales) está fuera del corpus Java — se infiere de los servlets y del logging client-side, y se confirmaría leyendo un `console.log` real de la station comprometida (recomendado para el informe al cliente). El enum `ModuleVerificationMode` (orden `low`/`medium`/`high`/`DEFAULT`) vive en `com.tridium.nre.security`, no decompilado aquí; solo se verificó verbatim que el dashboard premia únicamente `high` con OK.

---

**Bloque cerrado**: 2026-06-21. Investigación READ-ONLY sobre source decompilado (vineflower) — ~18 clases cross-módulo (nss-rt, baja, program-rt, platform-rt, platDaemon-rt). Contracara DEFENSIVA (detección + forense) del [Bloque 75] (prevención). Capa 21. Engram: `niagara/security/b112-detection-forensics`.
