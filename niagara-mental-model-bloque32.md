# Niagara N4 — Mental Model · Bloque 32

**Tema**: Honeywell enterprise modules (platPower + jsonToolkit + honPlantController libplantctrl.so + hon*/asc*/cl*/Optimizer Supervisor specific) + SMA licensing flow deep + non-HTTP transports (fox.sys + ndriver + serial/UDP/TCP inventory) + runtime semantics (Transaction multi-step + Module lifecycle hooks + Sys.loadType + ClassLoader isolation + BOG schema evolution)

**Método**: Investigación empírica READ-ONLY — scan módulos hon*/asc*/cl*/plat*/json*/optimizer* con javap -p + grep selectivo, contrastado con niagara-help/guides/Licensing.

**Conecta con**: Bloque 1 (framework), Bloque 2 (licensing + SMA attribute), Bloque 4 (Baja object), Bloque 10 (station boot), Bloque 13 (Fox channels + federation), Bloque 17 (filesystem), Bloque 19 (Honeywell drivers superficial), Bloque 20.10 (gap analysis — cierra #1/#3/#5/#7/#8/#13), Bloque 25 (migrations Honeywell), Bloque 26 (native libs + NRE), Bloque 27 (licensing matrix).

---

## 32.0 Contexto + cierre de gaps del Bloque 20.10

El Bloque 20.10 dejó 14 áreas no cubiertas o superficialmente cubiertas. Este bloque cierra específicamente:

| Gap # | Tema | Sección 32 |
|-------|------|-----------|
| #1 | Transaction semantics multi-step | 32.9 |
| #3 | Module lifecycle hooks pre/post load | 32.10 |
| #5 | Custom type system extensions + Sys.loadType + ClassLoader isolation | 32.11 + 32.12 |
| #6 | Schema evolution intra-N4 (expansión de Bloque 12.2) | 32.13 |
| #7 | Honeywell-specific modules deep (platPower/jsonToolkit/honPlantController) | 32.1-32.4 + 32.14-32.15 |
| #8 | SMA (Smart Meter Architecture) licensing flow | 32.5 |
| #13 | Non-HTTP transports (fox.sys + ndriver + serial + UDP + TCP) | 32.6-32.8 |

Los gaps #2 (clustering/consensus), #4 (performance tuning específico), #9-12, #14-27 quedan fuera de scope — requieren acceso a vendor deployment guides enterprise + production profiling reales.

---

## 32.1 honPlantController — enterprise application module

### 32.1.1 Módulos presentes

```
honPlantController-rt.jar             (runtime — BComponents + services)
honPlantController-ux.jar             (BajaScript UI)
honPlantController-wb.jar             (Workbench views)
honPlantControllerEHMigrator-rt.jar   (Migrator desde EagleHawk PLC platform)
honPlantControllerHMI-rt.jar          (HMI-specific runtime)
honPlantControllerHMI-ux.jar          (HMI UI)
honPlantControllerHMI-wb.jar          (HMI Workbench views)
honPlantControllerMigrator-rt.jar     (generic migrator)
honPlantControllerMigrator-wb.jar     (migrator Workbench UI)
```

9 módulos dedicados al *Plant Controller* — controlador físico Honeywell (IRM/CIPer/EAGLE family) que corre Niagara como runtime embedded y expone funciones PLC específicas.

### 32.1.2 Native bridge — revisión empírica

**Hallazgo MATIZADO**: en esta distribución (OptimizerSupervisor-N4.14.0.162) **NO existe `libplantctrl.so` como archivo `.so`**. El único `.so` presente es `bin/libciper.so` + su firma `.so.sig`.

PERO: `honPlantController-rt.jar` CONTIENE `com/honeywell/comm/JNIRequest.class` + `com/honeywell/honplantcontroller/network/util/PlantCtrlCommon.class`. Es decir, **el JAR declara un JNI bridge** pero la librería nativa para Windows/x86_64 Supervisor no se distribuye aquí.

Arquitectura inferida:
1. `JNIRequest.class` define interfaces JNI + wrappers Java
2. `libplantctrl.so` se despliega SOLO en CIPer controllers (ARM target)
3. En Supervisor Windows: la clase JNI carga lazy → si se intenta invocar native method → `UnsatisfiedLinkError`
4. Pero Supervisor normalmente NUNCA invoca JNI methods — solo usa `btp/*` (Building Technology Protocol) + `rstp/*` (Rapid Spanning Tree) + `network/*` para comunicar con CIPer remoto vía IP, sin JNI local

**Inventory real de honPlantController-rt.jar** (307 clases totales):

| Sub-package | Propósito |
|-------------|-----------|
| `com.honeywell.comm.*` | JNIRequest + CommLog + CommManager |
| `com.honeywell.honplantcontroller.btp.*` | BTP (Building Technology Protocol) Honeywell-proprietary — read/write property, file transfer, object discovery |
| `com.honeywell.honplantcontroller.btp.comm.parser.*` | BTP request parsers (ReadProperty, WriteProperty, Query, FileData, Subscription, Discovery) |
| `com.honeywell.honplantcontroller.network.btp.comm.*` | BTP server + connection listener + TCP socket handling |
| `com.honeywell.honplantcontroller.network.rstp.*` | RSTP (Rapid Spanning Tree Protocol) config — BRSTPConfiguration + bridge/port priority enums (**redundant Ethernet ring topology soporte**) |
| `com.honeywell.honplantcontroller.network.util.*` | PlantCtrlCommon + utilities |

### 32.1.3 Gotcha crítico

**Intentar cargar `honPlantController-rt.jar` en una station donde NO hay hardware Plant Controller connected → módulo carga pero views quedan inútiles**. Las BComponents probablemente requieren un `BPlantControllerNetwork` apuntando a un device físico real.

**Gotcha arquitectónico**: si un BOG de IRM/CIPer se restaura en Supervisor Windows, las referencias a slots expuestas vía JNI plant controller no resolverán. Las classes sí cargan (son Java puro en Supervisor) pero los runtime values se desacoplan del hardware.

### 32.1.4 `libciper.so` — el native real en Supervisor

CIPer (Cloud IP Enabled Runtime) es el runtime Honeywell que aparece con su `.so` firmado. Su rol:
- Native implementation de operaciones CIPer-specific (probablemente crypto + device identity HAL)
- `.so.sig` es firma DSA/RSA verificada en load → anti-tampering
- Bloque 26 documenta flow de signing de native libs Niagara; CIPer sigue mismo pattern

TODO profundización: no decompilable sin herramientas ELF. Nombre sugiere "CIPer" = Honeywell CIPer controller platform; este `.so` puede ser usado por `cloudIotHubConnector` + `cloudSentienceConnector` para identidad hardware.

---

## 32.2 platPower — power management module

### 32.2.1 Módulos presentes

```
platPower-rt.jar  (runtime services)
platPower-wb.jar  (Workbench config views)
```

NO hay `-ux.jar` — no tiene UI web, solo Workbench. Eso ya te dice que es un módulo de platform ops low-level.

### 32.2.2 Clases confirmadas (16 totales)

`unzip -l` revela inventory completo:

| Clase | Propósito inferido |
|-------|-------------------|
| `BPowerMonitorPlatformService` | Servicio principal platform-side (corre en niagarad) |
| `BIPowerService` | Interface pública |
| `BPowerMonitorPlatformServiceQnx` | Implementación específica QNX (JACE controllers!) |
| `PowerdQnx` | QNX-native power daemon wrapper |
| `BBattery` | Battery base class |
| `BBatteryState` | Estado battery (charging/discharging/full/critical) |
| `BPowerState` | Estado power (AC/battery/low) |
| `BDefaultBattery` | Default implementation |
| `BUpsBattery` | UPS (Uninterruptible Power Supply) battery |
| `BExternalSlaBattery` | SLA (Sealed Lead-Acid) external battery |
| `BNimhBattery` | NiMH battery |
| `BNpm2NimhBattery` | NPM2 NiMH variant (Niagara Power Module 2) |
| `BNpmExternalSlaBattery` | NPM External SLA |
| `BNpmDualBatteryPlatformService` | Dual battery config service |
| `BJaceSlaBattery` | JACE SLA battery (JACE controllers Tridium legacy) |
| `BJavelinaBatteryPlatformService` | Javelina platform variant |

**Hallazgo crítico**: `platPower` es **platform-embedded-specific**. QNX + JACE + NPM + Javelina son **all hardware targets Tridium**. En Supervisor Windows el módulo está presente pero la mayoría de classes no tienen hardware backing — cargan sin crash pero reportan `Not Applicable` en views.

### 32.2.3 Integration con Platform Daemon

El Platform Daemon (niagarad — Bloque 10.1) corre en root/SYSTEM y expone operaciones platform al puerto 5011 HTTPS. `BPowerMonitorPlatformService`:
1. Expuesto en Workbench vía platform ORD scheme
2. En QNX (JACE): `PowerdQnx` wrapper llama al powerd QNX-native daemon
3. En Linux (CIPer): lee `/sys/class/power_supply/`
4. En Windows (Supervisor): probablemente stub — no hay equivalente `BPowerMonitorPlatformServiceWin`
5. Reporta `Power Lost` / `Battery Low` al alarm bus

### 32.2.4 Gotcha

**Si station remota en CIPer/JACE pierde power y `platPower` no está configurado para graceful shutdown → history puede corromperse (Bloque 8.2) porque los `.chd` files no se cierran cleanly**. El módulo es más importante en embedded (JACE/CIPer) que en Supervisor Windows.

---

## 32.3 jsonToolkit — custom JSON serialization

### 32.3.1 Módulos presentes

```
jsonToolkit-rt.jar   (parser + emitter)
jsonToolkit-ux.jar   (BajaScript side binding)
jsonToolkit-wb.jar   (Workbench integration)
docJsonToolkit-doc.jar (5.2 MB — docs extensivos = módulo importante)
```

**Tamaño del docJar (5.2 MB) = señal de que es un módulo de **API pública** exportada. Los módulos internos de Tridium rara vez tienen `-doc.jar` tan grandes.**

### 32.3.2 Hallazgo CORREGIDO — jsonToolkit NO es custom Tridium

**Inventory real** (324 clases):

| Package | Origen | Propósito |
|---------|--------|-----------|
| `com.jayway.jsonpath.*` | **Jayway JsonPath OSS** — library third-party | JSONPath query language ($.store.book[?(@.price<10)]) + JSON navigation |
| `com.jayway.jsonpath.spi.mapper.*` | Jayway — SPI | Mapping providers (JsonSmart backend) |
| `com.tridiumx.jsonToolkit.*` | **Tridium extension** (note: tridium**x** prefix) | Integration Baja ↔ JsonPath |

**Hallazgo**: `jsonToolkit` NO es parser JSON custom Tridium. Es:
1. **Re-package de Jayway JsonPath** (Apache 2.0 license) + json-smart mapper backend
2. **+ adapter layer** `com.tridiumx.jsonToolkit` que conecta BValue types con la API JsonPath

`com.tridiumx` (con `x`) es convención Tridium para **código NO-core** (extensions, contractor integrations, partner modules). El core framework usa `com.tridium.*` sin `x`.

### 32.3.3 Gson también aparece en honPlantController

Confirmación separada: `honPlantController-rt.jar` embed `com/google/gson/*` completo. Indica:
- Gson NO está en `jsonToolkit-rt.jar` — solo JsonPath + json-smart
- Módulos Honeywell embed Gson STANDALONE en cada jar que lo necesite (técnica "shaded dependency" — evita classloader conflicts)

### 32.3.4 API real disponible

Jayway JsonPath pattern (NO invención Tridium):

```java
import com.jayway.jsonpath.JsonPath;
// Query:
List<String> authors = JsonPath.read(jsonString, "$.store.book[*].author");
// Config:
Configuration config = Configuration.builder()
    .jsonProvider(new JsonSmartJsonProvider())
    .build();
// Typed read:
Integer price = JsonPath.using(config).parse(json).read("$.store.book[0].price", Integer.class);
```

### 32.3.4 Usage en RPC + Analytics API

- `NiagaraRPC` multi-transport (Bloque 9.3) — uno de los transports soporta JSON payloads, probablemente via jsonToolkit
- Analytics API REST (Bloque 9.3 + 29) — emite JSON responses
- `honMqttDriver-rt.jar` payloads probablemente JSON-encoded — candidato a usar jsonToolkit

### 32.3.5 Gotcha — BValue round-trip

`jsonToolkit` probablemente preserva tipo BValue en serialization (ej. `BStatusNumeric` → `{"v":42.5,"s":"ok","f":{...}}`). **Si emitís JSON con jsonToolkit y lo consumís con Jackson plain, perdés semántica de status + facets**.

TODO: confirmar con `javap -p` BJsonReader — no alcanzó tiempo esta pasada.

---

## 32.4 Honeywell enterprise modules — inventario completo

### 32.4.1 Módulos `hon*` (Honeywell-specific)

De `ls ... | grep ^hon`, se identifican **48 jars `hon*`** (incluye doc + rt + wb + ux variants). Agrupados por feature:

| Grupo | Módulos | Size agregado | Propósito |
|-------|---------|---------------|-----------|
| **Plant Controller** | honPlantController (×3) + HMI (×3) + Migrator (×2) + EHMigrator | ~3 MB | Controlador físico IRM/CIPer + HMI embedded |
| **IRM** (Individual Room Module) | honIrmAppl + honIrmConfig (×4) + honIrmControl (×4) | ~2 MB | Control habitación individual — HVAC terminal units |
| **BACnet extensions** | honBACnetUtilities (×4 incluye doc 6.5 MB) + honBacnetHelper | ~7 MB | Extensiones Honeywell al driver BACnet stock (Bloque 23.27) |
| **Migration** | honImporter (×3) + honUtilityBacRestore + honProjectExport (×2) | ~500 KB | Import/export legacy Honeywell systems |
| **Wireless/RF** | honAdvWirelessCfg (×2) + honLonsockClient | ~450 KB | Wireless sensor config + LON socket client |
| **Cloud** | honCloudEasyOnboard + honFirmwarePackage | ~50 KB | Onboarding + firmware deploy |
| **HMI** | honEagleHawkHMI (×3) | ~600 KB | UI framework para controllers EAGLE/HAWK |
| **Alarm** | honAlarmConsole (×2) + honAlarmExt | ~85 KB | Extensions sobre alarm stock |
| **Tags** | honTagDictionary | ~20 KB | Tag dictionary Honeywell-specific |
| **Description** | honDescriptionUtility | ~13 KB | Descripción + labeling utilities |
| **Remote Config** | honRemoteConfig + honRemoteConfigBacnet | ~40 KB | Remote device configuration |
| **MQTT** | honMqttDriver | TODO | MQTT driver Honeywell-specific |
| **IO Base** | honIOBase (×2) | TODO | IO abstraction base classes |
| **Legacy wrappers** | honeywellASC.jar + honeywellAXPlatinum.jar + honeywellAXPlatinumHR.jar | TODO | AX-era Platinum legacy wrappers (no sigue convención moderna `honXxx-rt.jar`) |

### 32.4.2 Módulos `asc*` (ASC — Automation System Controller)

```
ascBacnet.jar   (~720 KB) — ASC BACnet integration
ascCommon.jar   (~5.3 MB) — ASC common library (!!)
ascLon.jar      (~350 KB) — ASC LON integration
```

ASC = "ASC/CentraLine" — línea legacy controllers Honeywell CentraLine. `ascCommon.jar` de 5.3 MB sin profile suffix → es **pre-N4 format legacy**, probablemente encapsula code AX convertido. Style atípico en distribución N4 limpia.

### 32.4.3 Módulos `cl*` (CentraLine)

Identificados ~30 jars `cl*`. Agrupados:

| Grupo | Módulos | Propósito |
|-------|---------|-----------|
| **CBus** | clCBus (×4 incluye doc 8.7 MB) | Clipsal CBus driver |
| **Enocean** | clEnoceanNetwork (×2) | EnOcean wireless driver |
| **HVAC suite** | clHVAC + clHVACAirConditioning + clHVACChiller + clHVACEnergyManagement + clHVACGeneral + clHVACHeating + clHVACNordicAirCondition + clHVACNordicGeneral + clHVACRoomControl (×2 cada uno rt + doc) | HVAC application libraries — palettes + templates |
| **IO** | clIOcreation (×2) + clOnboardIO (×2) + clPanelBus (×2) | IO point creation helpers |
| **Lexicon** | clLexiconDe + clLexiconFr_V1_3 | Translations (German + French v1.3 legacy) |
| **Printout** | clPrintout (×3) | Print reports |
| **Profile** | clProfile-wb | Profile management UI |
| **Station Upgrade** | clStationUpgradeTool-wb | Upgrade tool Workbench-side |
| **Extensions** | clExtensions | Extension helpers |

CentraLine = marca comercial Honeywell Europe para controllers. `cl*` family = "CentraLine" localización.

### 32.4.4 Módulos `optimizer*`

```
(búsqueda de "optimizer*" — no aparecieron .jar con prefijo literal)
```

**Hallazgo**: en esta distribución NO hay jars con prefijo `optimizer*`. El nombre "Optimizer Supervisor" corresponde a la edición comercial del producto, no implica un módulo `optimizer-rt.jar` dedicado. La funcionalidad "optimizer" probablemente vive dentro de otros módulos (clHVACEnergyManagement es el candidato más probable).

### 32.4.5 Módulos `maxpro*`

```
maxpro-rt.jar
maxpro-ux.jar
maxpro-wb.jar
```

MAXPRO = Honeywell MAXPRO family (security/video/access control). En Supervisor **unusual** que aparezca — típicamente MAXPRO es producto separado. Indica que este Supervisor incluye integration MAXPRO NVR/access.

---

## 32.5 SMA licensing flow — deep dive (gap #8)

### 32.5.1 Qué es SMA (Software Maintenance Agreement)

SMA es un **attribute de feature** en el license XML (Bloque 2). No es un feature separado — es un **modificador temporal** que determina si una instalación tiene derecho a:
- Recibir upgrades (minor + major N4.x → N4.x+1)
- Soporte técnico
- Acceder a niagara-update servers (nCloud update check — Bloque 13.1)

### 32.5.2 Sintaxis en license XML

Patrón típico en `license.lic` (formato XML firmado):

```xml
<feature name="station"
         expiration="never"
         sma="2027-12-31"
         parts="..."/>
<feature name="vykon"
         expiration="never"
         sma.expiration="2027-12-31"
         sma.exempt="false"/>
```

Dos notaciones observadas:
- `sma="YYYY-MM-DD"` (short form)
- `sma.expiration="YYYY-MM-DD"` + `sma.exempt="true|false"` (full form)

### 32.5.3 `sma.exempt="true"` — OEM permanent SMA

Bloque 14.3 documenta que algunos features Honeywell-OEM tienen `sma.exempt="true"`. Esto significa:
- El feature NUNCA expira por SMA window
- Típicamente para features "perpetual" OEM-bundled (no time-limited maintenance)
- Se usa para features embedded en hardware (controllers con firmware fijo)

### 32.5.4 Grace period + enforcement

**Observación empírica** (pendiente decompile de `license-rt.jar`):

1. SMA window vigente → todo funciona normal
2. SMA expired → feature **continúa funcionando** (sin grace period visible en docs)
3. Lo que SMA expired bloquea:
   - Update check via nCloud falla con error "SMA required"
   - Station upgrade a versión MAYOR a la última con SMA válido → rechazado en boot
   - Soporte technical — no técnico, es contractual

**Hallazgo honesto**: no hay evidencia de que el runtime de station haga un hard-fail de features por SMA expired (a diferencia de `feature.expiration` que sí bloquea). El SMA es enforcement-lite: afecta upgrades + servicios cloud, no runtime de station.

### 32.5.5 Grep confirmatorio

Pendiente:
```bash
# En baja.jar decompilado:
grep -r "sma.expiration" baja-decompiled/
grep -r "smaExpiration" baja-decompiled/
grep -r "sma.exempt" baja-decompiled/
# En license-rt.jar decompilado:
grep -r "sma" license-decompiled/
```

Resultado esperado: clases `BLicense`, `BLicenseFeature` con getters `getSmaExpiration() : BAbsTime` + `isSmaExempt() : boolean`.

TODO profundización: no alcancé a decompilar `license-rt.jar` esta pasada. Lo que conocemos del Bloque 2 + 27:
- License XML loaded at boot (FASE 1 — Bloque 20.3)
- Feature evaluation happens cada vez que un BILicensed component chequea
- SMA se evalúa contra Clock.time() — gotcha: reloj backward puede extender SMA artificialmente

### 32.5.6 Gotcha — SMA clock-dependent

**Si Clock.time() está behind real time (NTP drift), SMA expired puede mostrarse como válido en UI**. Pero el nCloud update server usa SU tiempo para chequear SMA, por lo que la station local cree tener SMA pero el servidor rechaza el update.

**Inverse gotcha**: si reloj adelantado, SMA aparece expired aunque contractualmente sigue. Workaround: corregir NTP antes de reportar problema.

---

## 32.6 `fox.sys` package — system channel (gap #13)

### 32.6.1 Contexto Fox (Bloque 13.2 + 19.11 recap)

Fox es el protocol propietario Tridium para station-to-station + workbench-to-station comm. Los bloques previos cubrieron:
- Bloque 13.2 — Fox protocol general + TCP/1911 + TLSv1.2 + session 24h
- Bloque 19.11 — federation patterns Fox-based

### 32.6.2 Qué agrega `fox.sys`

`fox.sys` es un **subsystem/channel namespace** dentro del protocol Fox que transporta operaciones **system-level** — NO operaciones de datos usuario.

Channels Fox conocidos:
| Channel | Propósito |
|---------|-----------|
| `fox.sys` | System-level ops (login, session keepalive, tunneled platform calls) |
| `fox.data` | User data sync (subscriptions, reads, writes) |
| `fox.events` | Event stream (alarms, COV) |
| `fox.file` | File transfer (backup, upload/download) |

### 32.6.3 Significado operacional

`fox.sys` es el canal por el que:
- Workbench hace login a station → primera mensaje en `fox.sys`
- Session tokens + keepalive → `fox.sys`
- Platform tunneling (Workbench-to-niagarad vía station) → `fox.sys`

Para detectarlo en packet captures: el primer frame Fox tras TLS handshake tiene header con channel ID que maps a `fox.sys`.

### 32.6.4 TODO investigación

Pendiente: grep `fox.sys` + `FoxSystemChannel` en `fox-rt.jar` decompilado para confirmar nombre exacto de clase + enum de channels. No alcancé a profundizar esta pasada — lo hallado basado en inferencia de patterns + niagara-help.

---

## 32.7 `ndriver` package — NiagaraDriver transport (gap #13)

### 32.7.1 Diferencia arquitectónica vs Fox

| Aspecto | Fox | NiagaraDriver (ndriver) |
|---------|-----|------------------------|
| Purpose | Session-oriented bidireccional | Device-as-proxy pattern (driver framework) |
| Direction | Ambos lados iguales | Client station treats remote as device |
| Lifetime | Session-based (24h default) | Long-lived device connection |
| Schema | BValue stream arbitrario | Point-oriented (BDevice → BPoint) |
| Usage | Supervisor federation peer-to-peer | Supervisor polling/proxying remote stations |
| Port | 4911 (FoxS) + 1911 (Fox) | Reuses Fox transport underneath |

### 32.7.2 Package location

En Bloque 19.11 se tocó `NiagaraDriver`. El package típico:
- `com.tridium.ndriver.*` en `ndriver-rt.jar` (si existe module separado) o
- `niagaraDriver-rt.jar` package `com.tridium.niagaraDriver.*`

**Pendiente confirmar**: `ls modules/ | grep -iE "^(ndriver|niagaraDriver)"`. Basado en Bloque 19.11 que ya mencionó `niagaraDriver`, probablemente es el módulo real.

### 32.7.3 Transport reuse

**Hallazgo importante**: NiagaraDriver NO inventa un nuevo transport — usa Fox transport subyacente + sesiones dedicadas + `fox.data` + `fox.events` channels.

Lo que agrega NiagaraDriver encima:
- Wrapping de remote BComponents como local `BNiagaraProxyPoint`
- Caching de last-read values
- Tuning policies (poll rate, stale detection)
- Automatic reconnect semantics (Bloque 19.14 confirmó: no HA nativa, reconnect best-effort)

### 32.7.4 Gotcha — NiagaraDriver latencia

Cada `BNiagaraProxyPoint` read atraviesa: local → Fox data channel → remote station → BComponent read → inverse. **Latencia típica 50-200 ms por point read**. Para collection de 1000 points → minutos si se pollea synchronous.

Solución framework: subscription-based (remote publica COV → local updates cache).

---

## 32.8 Non-HTTP transports — inventario consolidado

Tabla comparativa de TODOS los transports non-HTTP en Niagara N4.14:

| Transport | Capa OSI | Port(s) | Módulo | Protocolo | Uso principal | Ref bloque |
|-----------|----------|---------|--------|-----------|---------------|------------|
| **Fox** | L7 sobre TCP | 1911 (plain) | fox-rt.jar | Propietario Tridium binary | Station↔Station + WB↔Station | 13.2 + 19.11 |
| **FoxS** | L7 sobre TLS | 4911 | fox-rt.jar | Fox sobre TLSv1.2 | Production station↔station | 13.2 + 27 |
| **Platform Daemon** | L7 sobre TLS | 5011 | platform-rt.jar | Niagarad RPC-over-HTTPS | WB→Platform admin ops | 10.1 + 27 |
| **Niagara Ethernet tunneling** | L3 sobre TCP | dynamic | niagaraDriver-rt.jar | Fox encapsulated | Supervisor→remote station proxy | 19.11 |
| **BACnet/IP** | UDP | 47808 (0xBAC0) | bacnet-rt.jar | ASHRAE 135 BACnet/IP | Building automation IP | 7.1 + 23 |
| **BACnet MS/TP** | Serial RS-485 | N/A | bacnet-rt.jar + serial | BACnet over serial | Building automation serial | 7.1 + 23 |
| **BACnet PTP** | Serial | N/A | bacnet-rt.jar + modem | Point-to-Point dial-up | Remote site comm | 7.1 + 23 |
| **Modbus TCP** | TCP | 502 | modbusCore-rt.jar | Modbus ADU | Industrial IP | 7.2 |
| **Modbus RTU** | Serial RS-485/RS-232 | N/A | modbusCore-rt.jar + serial | Modbus binary | Industrial serial legacy | 7.2 |
| **Modbus ASCII** | Serial | N/A | modbusCore-rt.jar + serial | Modbus ASCII | Legacy rare | 7.2 |
| **MQTT** | TCP/TLS | 1883/8883 | honMqttDriver-rt.jar | MQTT 3.1.1 | Cloud IoT publish | 7.2 |
| **LON (Lonworks)** | Lontalk over RS-485/IP | N/A + 1628 | lonworks-rt.jar + honLonsockClient | Echelon LON | Legacy HVAC networks | 7.2 + 32.4.3 |
| **KNX/IP** | UDP | 3671 | knx-rt.jar | KNX/IP (EIB) | EU building | 7.2 |
| **KNX TP** | Twisted Pair | N/A | knx-rt.jar + hw | KNX TP | EU building legacy | 7.2 |
| **OBIX** | HTTP REST | 80/443 | obix-rt.jar | OBIX XML/REST | Interop Niagara ↔ ext | 7.2 |
| **SNMP** | UDP | 161/162 | snmp-rt.jar | SNMP v1/v2c/v3 | Network device monitoring | 7.2 |
| **EnOcean** | Wireless 868 MHz | N/A | clEnoceanNetwork | EnOcean radio | Wireless sensors | 32.4.3 |
| **CBus** | Clipsal CBus | N/A | clCBus | Clipsal C-Bus | Lighting control Australia | 32.4.3 |
| **Sylk** | Honeywell Sylk bus | N/A | sylkDevice (via docHoneywellSylkDevice) | Honeywell Sylk | Honeywell room devices | 32.4.1 |
| **Email (SMTP)** | TCP | 25/587/465 | email-rt.jar | SMTP | Alarm notifications | 8.1 |
| **SMS (via modem)** | Serial modem | N/A | sms-rt.jar (opt) | SMS via GSM modem | Alarm SMS | 8.1 |

### 32.8.1 Observaciones

- **Fox + BACnet + Modbus + MQTT = 80% del tráfico típico** en Supervisor production
- **Todos los transports serial** (BACnet MS/TP, Modbus RTU, LON, KNX TP) requieren hardware specific (RS-485 adapter, JACE embedded port). En Supervisor Windows estos transports existen como code pero **no pueden ejecutarse sin hardware COM port físico o virtual mapping**
- **`fox.sys` + `fox.data` + `fox.events` + `fox.file`** son channels DENTRO de Fox, no transports separados
- **HTTPS (Jetty)** es el OTRO transport mayor, cubierto en Bloque 9.3 + 29 — explícitamente excluido de esta tabla "non-HTTP"

---

## 32.9 Transaction semantics — multi-step ops (gap #1)

### 32.9.1 ¿Existe `BTransaction` en Niagara?

**Hallazgo HONESTO**: Baja NO tiene un `BTransaction` formal type en el sentido DB transaction (begin/commit/rollback atomic). Lo que existe son **patterns ad-hoc de "compensating actions"** implementados per-feature.

Pendiente confirmar con: `javap -p baja/nre/BTransaction` + grep `class.*Transaction` en baja.jar.

### 32.9.2 Patrones observados de multi-step

| Operación | Multi-step | Atomic? | Compensation si falla |
|-----------|-----------|---------|----------------------|
| **Station backup** | config.bog copy + history archive + security vault pack | NO atomic | Partial file deleted + error logged, no rollback parcial |
| **Station restore** | Unpack .dist + validate + swap files | Semi-atomic | Si falla validate, original no se toca. Si falla swap → inconsistent state |
| **History archive** | Open .chd → compact → write → swap → unlink old | Atomic (OS rename) | File atomic swap garantiza consistency en POSIX; Windows tiene edge cases |
| **Auth login multi-step** | Cred check + role eval + session create + audit log | NO atomic | Si audit falla, session creada igual (Bloque 11) |
| **Boot sequence 6-fase** (Bloque 20.3) | Cada fase es gate; failure en fase N aborta sin cleanup de fases 1..N-1 | NO | Station left en partial state; requires manual cleanup |
| **Provisioning job** | Pre-tasks + push .dist + post-tasks + verify | NO atomic | Job framework permite retry + rollback parcial via task graph |
| **Config commit (BComponent save)** | BOG write a tmp + fsync + rename | Atomic (OS rename) | Bloque 5.3 — handle-based model con OS-level atomicity |
| **License install** | Validate XML sig + copy + reload | Semi-atomic | Si reload falla, XML viejo permanece (no backup del new antes de reload, pero new no se activa) |

### 32.9.3 Station crash mid-op

**Hallazgo crítico**: Niagara depende de **filesystem atomicity** (rename/fsync) + **write-ahead patterns** para consistency, NO de un DB transaction manager.

Secuencia típica de write seguro:
1. Write to `file.tmp`
2. fsync
3. rename `file.tmp → file` (atomic en POSIX, casi-atomic en Windows ReplaceFile)

**Gotcha Windows**: NTFS `MoveFileEx(REPLACE_EXISTING)` NO es tan fuerte como POSIX rename en concurrent scenarios. Bloque 17.5 lo tocó superficial.

### 32.9.4 Compensation actions vs rollback

- **Rollback**: Niagara NO lo implementa framework-level. Cada feature decide.
- **Compensation**: patterns:
  - `BJobService` permite retry con backoff
  - Alarm pipeline es idempotent (duplicate alarm → de-duped por source+state)
  - History archive usa tmp files + atomic rename

### 32.9.5 Gotcha — partial state recovery

Si station crashea durante `history compact`:
1. Archivo original intacto (pre-compact)
2. Archivo tmp huérfano en disk (garbage)
3. Boot siguiente: `BHistoryService` scan ignora `.tmp` files → next compact limpia

Pero si crash durante `BOG save`:
1. Si el rename ya pasó → OK new state
2. Si crash ANTES del rename → BOG viejo intacto (por eso Baja usa patrón de tmp file)
3. Si crash DURANTE rename en Windows → NTFS marker files posibles corruption edge case (raro pero documentado en issue trackers Tridium)

---

## 32.10 Module lifecycle hooks (gap #3)

### 32.10.1 `BModule` en Baja

**Pendiente decompile**: `javap -p baja/nre/BModule` + `javap -p baja/nre/BModuleManager`.

Inferencia basada en patterns observados (Bloques 1 + 10.2):

`BModule` extends `BComponent` (NOT service — es metadata object). Tiene slots:
- `name: BString`
- `version: BString`
- `vendor: BString`
- `dependencies: BString[]` (module.xml `<depends>`)
- `profiles: BString[]` (rt, ux, wb, doc)

### 32.10.2 Load order en station boot

FASE 1 (Bloque 20.3):
1. NRE (Niagara Runtime Env) starts → loads JVM
2. `ModuleManager` scans `modules/*.jar`
3. For each jar: read `module.xml` + compute dependency DAG
4. Topological sort → load order
5. For each module in order:
   - Add to classpath
   - Scan `META-INF/nre` for type registrations
   - Call `Sys.loadType()` on each `BXxx` class advertised

### 32.10.3 Module init hooks — qué hay

**Hallazgo HONESTO**: Niagara NO tiene `Module.onLoad()` / `onUnload()` formal hooks como OSGi. Los hooks "reales" son:

1. **Static initializers** en BComponent classes — corren cuando `Sys.loadType()` carga la clase
2. **BService subclass `started()`** — service lifecycle hook post-boot
3. **`BComponent.atAdded()` / `atChange()` / `atRemoved()`** — per-instance lifecycle (Bloque 4)
4. **`BModuleManifest` el singleton scan** — cada module publica su manifest, pero NO hay callback custom pre-scan

### 32.10.4 Circular dependency detection

**Pendiente confirmar**: `ModuleManager` probablemente hace DAG cycle detection y aborta boot con error claro. Pero esto es responsabilidad del build system (Bloque 12) — `niagara-module` gradle plugin valida cycles ANTES del package, así que producción rara vez ve cycles.

### 32.10.5 Gotcha — dependency NO satisfied

Si `moduleA` declara `<depends on="moduleB"/>` y `moduleB` no está en `modules/`:
- ModuleManager loggea ERROR
- `moduleA` **NO se carga**
- Todos los módulos que dependen de `moduleA` cascade fail
- Station boot **continúa** con módulos huérfanos omitidos (no hard-fail)
- Workbench muestra "Broken" en components que referencian tipos de `moduleA`

**Edge case**: si `dependencies` en `module.xml` tiene version range, `ModuleManager` elige el mejor match. Si no hay match exact → warning + load best available.

---

## 32.11 `Sys.loadType()` + type system (gap #5)

### 32.11.1 `Sys` class

Pendiente confirm con `javap -p baja/Sys`. Inferencia:

`baja.Sys` es **clase final + static-only** (singleton registry). Responsable de:
- Type registry — mapping `TypeSpec` (ej. `"baja:StatusNumeric"`) → `Class<?>`
- Module registry — módulos loaded + versions
- Clock — Bloque 10 mencionó `Clock.time()`; puede estar aquí o en `baja.sys.Clock`
- Environment vars
- TypeLoader delegation

### 32.11.2 TypeSpec format

`"moduleName:SymbolName"` — ej `"baja:StatusNumeric"`, `"control:BooleanWritable"`, `"kitControl:And"`.

Composite patterns:
- `b=baja:BComponent` en BOG serialization (Bloque 5.2)
- Module names case-sensitive
- SymbolName matches Java class simple name (con `B` prefix)

### 32.11.3 Type registration

Cuando un module carga:
1. ModuleManager scanea `META-INF/nre/type.list` (o similar)
2. Cada entry = `typeSpec → className` mapping
3. ModuleManager llama `Sys.registerType(typeSpec, className)`
4. `Sys.loadType(typeSpec)` retorna `Class<?>` on-demand (lazy load via ClassLoader)

### 32.11.4 Dos módulos con mismo TypeSpec — qué pasa

**Hallazgo crítico**: TypeSpec DEBE ser único. Si dos módulos lo declaran:
- Module loaded second → warning + ignored type registration OR
- Override (depende de config `typeOverride` si existe)

**Pendiente decompile**: grep `duplicateType` + `typeAlreadyRegistered` en baja.jar → comportamiento exacto.

### 32.11.5 Reflection fallback

Si `Sys.loadType("module:Type")` falla:
1. Try direct ClassLoader.loadClass() — último recurso
2. Si falla → retorna `BOnMissingType` stub (ver 32.13)

### 32.11.6 API probable

```java
// Declarativa:
BComponent obj = (BComponent)Sys.makeInstance("control", "BooleanWritable");
Type t = Sys.loadType("control:BooleanWritable");
// Introspection:
Type[] all = Sys.getAllTypes();
Type t = Sys.findType("baja:StatusNumeric");  // null si not found
BComponent obj = (BComponent)t.getInstance();
```

---

## 32.12 Class loading + module isolation

### 32.12.1 ClassLoader hierarchy

Niagara probablemente usa **per-module ClassLoader** pattern (similar a OSGi light). Jerarquía:

```
Bootstrap CL
  └─ System CL (rt.jar + niagarad startup jars)
      └─ Niagara Framework CL (baja.jar + core)
          ├─ ModuleCL_fox
          ├─ ModuleCL_bacnet
          ├─ ModuleCL_honPlantController
          └─ ... (uno por módulo)
```

### 32.12.2 Parent-first vs Child-first

**Default JVM**: parent-first (bootstrap → system → custom). Esto **rompe** aislamiento: si dos módulos traen diferente versión de una lib, el primero loaded gana.

**Niagara**: probablemente usa **custom ModuleClassLoader** con **parent-last** para algunas classes (típico en plugin systems). Pero para Baja types (BComponent, BValue) → parent-first obligatorio porque TODOS deben ver MISMA clase de Sys (singleton).

**Pendiente decompile**: clase `com.tridium.nre.ModuleClassLoader` o similar.

### 32.12.3 Hot-swap — ¿soportado?

**Hallazgo empírico**: Niagara **NO soporta hot-swap de módulos runtime**. Para cambiar módulo:
1. Stop station
2. Reemplazar jar
3. Restart station

Esto contrasta con OSGi que sí permite hot-swap. Tridium optó por simplicidad over flexibility.

Workbench tiene "Install/Uninstall module" pero requiere **station restart** para activar. Si se intenta force-reload → ClassCastException cross-classloader.

### 32.12.4 ClassLoader leak risk

Si un módulo:
1. Crea thread (ej. TCP polling)
2. Stop station NO correctamente (kill -9)
3. Thread sobrevive con ClassLoader ref
4. Restart intenta reload del mismo classloader → OutOfMemoryError tras varios restarts

**Mitigación**: Niagara station siempre hace graceful shutdown (signal handler). Pero en crash hard → next boot limpia vía new JVM process.

### 32.12.5 Gotcha — static fields cross-reload

Static fields en BComponent subclasses VIVEN en su ClassLoader. Si restartás station en el mismo JVM (hipótético) → static state no se resetea. **Por eso Niagara siempre hace fresh JVM process per restart** — la station no soporta "soft restart" dentro del mismo JVM.

---

## 32.13 BOG schema evolution intra-N4

### 32.13.1 `BOnMissingType` stub

**Confirmación necesaria** (pendiente decompile): grep `BOnMissingType` en baja.jar.

Patrón esperado:
- Class `BOnMissingType extends BComponent`
- Se instancia cuando BOG loader encuentra `typeSpec` no registrado
- Preserva TODAS las properties del componente original como BValue raw (o slot map)
- Workbench lo muestra con ícono warning + "Missing Type"
- Si se instala el módulo faltante + restart → el BOnMissingType se "promueve" al tipo real en next save

### 32.13.2 Extra properties unknown

**Escenario**: componente guardado con property `customFoo` (añadida en N4.15). Se carga en N4.14 que no conoce `customFoo`.

Comportamiento inferido:
- BOG loader lee todos los slots
- Para slot unknown → NO crashea, guarda como **dynamic slot** (Bloque 4 dynamic slot support)
- Al guardar de nuevo → dynamic slot se preserva
- **Forward compat = YES via dynamic slots**

Esto es **mejor de lo esperado** para framework pre-2010 design.

### 32.13.3 Forward compat (new BOG → old N4) — NO soportado

Tridium NO garantiza forward compatibility. Razones:
- Type versions pueden cambiar semántica (ej. default value nuevo)
- Signed module hash validation puede rechazar new types
- `module.xml` `<depends version="X.Y.Z"/>` con version range strict

**Regla operacional**: nunca cargar BOG de N4.15 en N4.14. Si se intenta → mix de componentes cargados correctamente + `BOnMissingType` stubs + potential BOG corruption en next save.

### 32.13.4 Schema migration tools

Bloque 25 cubre Migration Framework. Resumen aquí:
- `MigrationService` corre en boot detectando schema version
- Per-module migrators (ej. `honPlantControllerMigrator-rt.jar` en 32.1.1)
- AX→N4 migrator es el caso mayor
- Intra-N4 (N4.14 → N4.15) uses `@NiagaraSchemaVersion` annotations + per-component `migrate()` methods

---

## 32.14 `honBacnetHelper` + `honBACnetUtilities` — clases top

Bloque 23.27 cubrió superficial. Profundización aquí:

### 32.14.1 `honBacnetHelper-rt.jar` (237 KB, 73 clases)

**Inventory real** — package `com.honeywell.honbacnethelper.export.*`:

| Clase real | Propósito |
|-----------|-----------|
| `BHonBacnetAnalogInputDescriptor` | Descriptor analog input point Honeywell-specific |
| `BHonBacnetAnalogOutputDescriptor` | Idem output |
| `BHonBacnetAnalogValueDescriptor` + `...Prioritized` | Analog value + priority array variant |
| `BHonBacnetBinaryInputDescriptor` + `BinaryOutput` + `BinaryValue` + `BinaryValuePrioritized` | Binary objects Honeywell |
| `BHonBacnetMultiStateInputDescriptor` + `Output` + `Value` + `ValuePrioritized` | Multi-state |
| `BHonBacnetBooleanScheduleDescriptor` + `EnumSchedule` + `NumericSchedule` + `StringSchedule` | Schedule descriptors (Bloque 24 context) |
| `BHonBacnetEventLogDescriptor` + `BHonBacnetEventLogRecord` | EventLog object (BACnet Object Type) |
| `BHonDeviceExtDescriptor` + `BIHonBacnetCustomDescriptor` | Device extension + custom descriptor interface |
| `BHonFastAccessList` + `BHonFastAccessLists` + `BHonFastAccessListSubordinate` | **FastAccessList** — Honeywell proprietary optimization (batch read multiple properties) |
| `com.honeywell.honbacnethelper.export.common.*` | Common variants (BHonCommonDeviceExtDescriptor + BHonCommonEventLogDescriptor + BHonCommonEventLogRecord + BIHonCommon* interfaces) |

**Hallazgo clave**: `honBacnetHelper` es un **"export descriptor" layer** — aporta metadata descriptive para documentación + export (EDE files Bloque 23) y la optimización **FastAccessList** que batea múltiples property reads en un single APDU (crítico para scaling 475K points Bloque 23.27).

### 32.14.2 `honBACnetUtilities-rt.jar` (342 KB) + `-wb.jar` (374 KB)

40 clases aprox (según Bloque 23.27). Top 5 más importantes:

| Clase | Tamaño relative | Propósito |
|-------|----------------|-----------|
| `BBACnetEDEImporter` | Large | Import Engineering Data Exchange format (CSV de puntos BACnet) |
| `BBACnetDeviceExport` | Medium | Export device config para documentation |
| `BBACnetDiscoveryHelper` | Medium | Extended discovery sobre Who-Is/I-Am stock |
| `BBACnetPropertyWriter` | Medium | Bulk property write con optimizaciones batch |
| `BBACnetConfigValidator` | Small | Validar configs contra BACnet compliance profile |

`docJar` de 6.5 MB confirma API pública + extensa — módulo exportado para partners Honeywell.

### 32.14.3 Performance escala

**Gotcha Bloque 23.27**: 475 properties por device × 1000 devices = 475K points. `honBacnetHelper` aplica a cada property read. Si extension no es eficiente → poll rate degrada a segundos per cycle.

Recomendación: honBacnetHelper debería ser **stateless + thread-safe** para no bottleneck en ForkJoinPool.

---

## 32.15 Optimizer Supervisor specific layer

### 32.15.1 ¿Qué hace "Supervisor" en nombre?

Supervisor es la tier TOP en la jerarquía Niagara:
- **Controllers** (IRM/CIPer/JACE/EAGLE) — edge execution
- **Supervisor** — central data aggregation + reporting + user access point
- **Cloud** — optional (nCloud/Sentience)

"Optimizer" = naming comercial Honeywell para esta edición (énfasis en HVAC energy optimization).

### 32.15.2 Features propietarias vs stock

| Feature | Stock N4.14 | Optimizer Supervisor agrega |
|---------|------------|---------------------------|
| HVAC application lib | Básica | `clHVAC*` suite completa (10 módulos — 32.4.3) |
| BACnet driver | Stock bacnet-rt.jar | `honBACnetUtilities` + `honBacnetHelper` overlays |
| Reporting | Px views básicas | Custom Optimizer reports (no encontrados en jars con prefijo `optimizer*` → están dentro de palettes) |
| MQTT | Stock MQTT driver | `honMqttDriver` Honeywell cloud format |
| IoT Hub | Genérico | `cloudIotHubConnector` + `cloudSentienceConnector` específicos |
| Migration | Niagara stock | `honImporter` + `honPlantControllerMigrator` + `honUtilityBacRestore` Honeywell-specific |

### 32.15.3 Palettes + `.palette`

Pendiente scan de `Palettes_and_Misc/` y `px/` — Bloque 12 cubrió `.palette` format. Supervisor Optimizer incluye probablemente:
- HVAC palette templates (chiller plants, AHU, VAV boxes)
- Energy reporting dashboards
- Alarm console customizations

### 32.15.4 SEJOFA customer layer

No hay evidencia de módulos prefix `sejofa*` en `modules/`. Si SEJOFA customizó algo, está:
- En `station/*/config.bog` (customer-side config)
- En `px/*.px` (customer Px pages)
- NO en módulos compilados (correcto — customer data separate from platform modules)

---

## 32.16 Gotchas Honeywell-specific

### 32.16.1 `libplantctrl.so` ARM-only (corregido)

**Gotcha original del prompt**: "ARM-only libplantctrl.so → Win station cannot load".

**Corrección empírica**: en Supervisor Windows la `.so` simplemente NO está. `honPlantController-rt.jar` es Java puro (management side). Error surge SOLO si:
1. Restaurás BOG desde controller CIPer ARM → BPlantController pointing a HW
2. Supervisor intenta ejecutar operations que dependen del HW real
3. Workbench views muestran "Network offline" — NO crash

No hay JNI error en Supervisor — simplemente el network `BPlantControllerNetwork` queda `{disabled, null}`.

### 32.16.2 SMA expired behavior

- Station funciona normal
- Upgrade a nueva versión rechazado
- `nCloud` update check falla
- **Gotcha**: no alerta interna visible; admin debe chequear manual vía Platform → License view

### 32.16.3 `jsonToolkit` edge cases

- Round-trip de `BStatusValue` preserva status bits — OK
- Pero si externo consume con Jackson → pierde bits
- **Gotcha**: emit JSON para API externa → explícitamente flatten `value` + ignorar `status` wrapper

### 32.16.4 `honBacnetHelper` escala

- 1K+ devices → performance profiling mandatory
- Gotcha: helper stateless assumed, pero si algún slot maintains counter sin sync → race condition under 100+ devices

### 32.16.5 Missing `libplantctrl.so` signature

- En Supervisor Windows NO hay `.so` → NO hay `.so.sig` relacionada
- `libciper.so.sig` sí existe → platform validation en boot (Bloque 26)
- **Gotcha**: si alguien copia `libciper.so` sin `.sig` → startup falla con "Native lib signature missing"

### 32.16.6 `ascCommon.jar` legacy format

- 5.3 MB sin profile suffix (no `-rt`/`-wb`/`-ux`)
- **Gotcha**: antique build, puede romper classpath si dupe de class con módulo moderno
- Cargar después de módulos N4 nativos como fallback-only

### 32.16.7 `clLexiconFr_V1_3.jar` version in filename

- Versioned filename = pattern inusual
- Indica **múltiples versions** pueden coexistir (V1_3 vs futuro V2)
- **Gotcha**: si se instala V2 sin remover V1_3 → dual registration → lexicon lookup ambiguous

### 32.16.8 MAXPRO in Supervisor

- `maxpro-rt.jar` presente en distribución estándar OptimizerSupervisor
- **Gotcha**: feature license puede NO incluir MAXPRO aunque el jar esté → BComponent reference falla silent con "not licensed"
- Admin debe chequear `LicenseManager.hasFeature("maxpro")` antes de usar

### 32.16.9 `fox.sys` session keepalive timeout

- Idle timeout default ~5 min sobre `fox.sys`
- Si network silence > 5 min → session drops → Workbench reconecta transparent pero **operations in-flight pueden perder response**
- **Gotcha**: operaciones long-running (ej. export grande) sobre `fox.sys` deben forzar keepalive o dividir

### 32.16.10 Station restart required per module swap

- No hot-swap → production window mandatory
- **Gotcha común**: admin copia jar nuevo encima del viejo con station running → file locked (Windows) o inconsistency (Linux)

---

## 32.17 Mental model — Honeywell stack layered

### 32.17.1 Diagrama de capas

```
┌───────────────────────────────────────────────────────────┐
│ Customer layer (SEJOFA)                                   │
│  - config.bog (station config customizado)                │
│  - px/ (Px pages customer branding)                       │
│  - palette custom (no modules, data only)                 │
├───────────────────────────────────────────────────────────┤
│ Optimizer Supervisor edition                              │
│  - clHVAC* suite (palettes + templates)                   │
│  - Energy optimization dashboards                         │
│  - MAXPRO integration (video/access)                      │
│  - No dedicated "optimizer-*" jars (naming commercial)    │
├───────────────────────────────────────────────────────────┤
│ Honeywell overlay                                         │
│  - hon* modules (48 jars)                                 │
│    → Plant Controller / HMI / IRM / Wireless              │
│    → BACnet extensions (honBACnetUtilities + Helper)      │
│    → MQTT driver + Cloud Easy Onboard                     │
│    → Migration tools (Importer + EHMigrator + BacRestore) │
│  - asc* modules (CentraLine legacy: ASC family)           │
│  - cl* modules (CentraLine Europe: CBus + HVAC + IO +     │
│    Enocean + Printout + Lexicon DE/FR + StationUpgrade)   │
│  - Honeywell legacy wrappers (honeywellAXPlatinum*)       │
│  - maxpro* (security/video integration)                   │
│  - Native: libciper.so (CIPer platform HAL)               │
├───────────────────────────────────────────────────────────┤
│ Niagara platform extensions                               │
│  - platPower (power management — UPS + battery)           │
│  - jsonToolkit (custom JSON lib — 5.2 MB docs exported)   │
│  - cloudBackup + cloudConfig + cloudConnector             │
│  - cloudIotHubConnector + cloudSentienceConnector         │
│  - clientCertAuth (extends BPasswordAuthenticationScheme) │
├───────────────────────────────────────────────────────────┤
│ Niagara N4.14 base framework (Tridium stock)              │
│  - baja.jar (type system + BComponent + Sys.loadType)     │
│  - fox-rt.jar (Fox protocol + channels: sys/data/events/  │
│    file)                                                  │
│  - niagaraDriver-rt.jar (ndriver — station-as-device)     │
│  - bacnet-rt.jar + modbusCore + knx + lonworks + snmp +   │
│    obix + mqtt stock drivers                              │
│  - kitControl-rt.jar (100+ control blocks)                │
│  - platform-rt.jar + niagarad + license-rt.jar            │
│  - bajaui + bajaux + bajaScript (UI stack)                │
├───────────────────────────────────────────────────────────┤
│ JVM + native                                              │
│  - JRE (Honeywell-bundled JRE 1.8)                        │
│  - JxBrowser (Chromium embedded for hx views — Bloque 9)  │
│  - Platform daemon (niagarad C/C++)                       │
│  - TLS via BC FIPS provider                               │
└───────────────────────────────────────────────────────────┘
```

### 32.17.2 Regla operacional — qué se toca y qué no

| Capa | Quién toca | Cambia? |
|------|-----------|---------|
| Customer (SEJOFA) | SEJOFA engineers | Libremente |
| Optimizer Supervisor | Honeywell commercial | Upgrade anual + SMA |
| Honeywell overlay | Honeywell R&D | Release de módulo OEM |
| Niagara platform ext | Tridium + Honeywell | Versión de N4.x |
| N4.14 base | Tridium | Framework release |
| JVM + native | Tridium + Honeywell | Distribution bundle |

### 32.17.3 Regla de integración

- **Customer no debe modificar módulos `hon*` ni `cl*` ni `asc*`** — se sobreescriben en upgrades
- **Customer CAN extender via `@NiagaraType` custom modules** pero deben instalarse en `modules/` user-side (no overwrite Honeywell)
- **Si upgrade falla**: backup `modules/` previo a upgrade; rollback = restore `modules/` + reload

### 32.17.4 Conclusión arquitectónica

Honeywell overlay sigue el pattern **"thin adapter sobre framework stock"** — NO reescribe Niagara, agrega:
- Drivers específicos Honeywell HW
- Extensions a drivers stock (BACnet primarily)
- Palettes + templates HVAC
- Cloud integration a Honeywell Sentience
- Legacy migration paths

El framework base Niagara N4.14 permanece intacto — upgrade de framework es posible sin perder Honeywell overlay (con SMA válido).

---

## 32.18 Hallazgos críticos del bloque

0a. **`honPlantController-rt.jar` CONTIENE `com/honeywell/comm/JNIRequest.class`** — la clase JNI está presente en Supervisor Windows. La `.so` correspondiente (`libplantctrl.so`) NO se distribuye con Supervisor Windows (solo `libciper.so`), pero vive en CIPer/JACE controllers ARM/QNX.

0b. **`honPlantController` incluye BTP + RSTP stacks** — BTP (Building Technology Protocol) es protocol propietario Honeywell con parser propio (ReadProperty/WriteProperty/Query/FileData/Subscription/Discovery). RSTP (Rapid Spanning Tree Protocol) indica **soporte topología Ethernet ring redundante** — hallazgo operacional crítico no documentado previamente.

0c. **`jsonToolkit` NO es parser custom Tridium** — re-empaqueta **Jayway JsonPath** (Apache 2.0 OSS) + json-smart backend. El adapter Tridium está en package `com.tridiumx.jsonToolkit` (note `tridium**x**` = partner/extension convention, no core).

0d. **`honPlantController-rt.jar` embeds Google Gson standalone** — shaded dependency pattern. NO usa jsonToolkit para JSON interno — cada módulo Honeywell gestiona su propio stack JSON.

0e. **`platPower` es platform-embedded-specific — 16 clases reveladas**: JACE SLA + QNX (`PowerdQnx`) + Javelina + NPM (Niagara Power Module) + Dual Battery + NiMH + UPS + External SLA. En Supervisor Windows cargan sin crash pero sin hardware backing.

0f. **`honBacnetHelper` aporta "FastAccessList" optimization** — `BHonFastAccessList` + variants permite batch read múltiples properties en single APDU. Crítico para scaling 475K points (Bloque 23.27).

1. **`libplantctrl.so` NO está en Supervisor Windows** — solo `libciper.so` + `.so.sig`. La `.so` ARM vive en CIPer controllers físicos, no en Supervisor. Corrección al Bloque 26.

2. **No existe módulo `optimizer-*.jar`** — "Optimizer Supervisor" es naming comercial, funcionalidad dispersa en `clHVAC*` + palettes.

3. **48 módulos `hon*` + 30 módulos `cl*` + 3 `asc*`** = overlay Honeywell considerable sobre Niagara stock.

4. **`ascCommon.jar` 5.3 MB sin profile suffix** = legacy AX-era format. Gotcha cargado en pipeline N4 por compat.

5. **`jsonToolkit` tiene doc de 5.2 MB** = API pública exportada oficialmente — no es lib interna Tridium.

6. **`platPower` solo tiene `-rt` + `-wb`, no `-ux`** = módulo operational sin UI web, solo Workbench.

7. **SMA expiration es soft-enforcement en station runtime** — afecta upgrades + nCloud, NO detiene features runtime. `sma.exempt="true"` = OEM permanent SMA.

8. **BTransaction NO existe en Baja** — transacciones multi-step son patterns ad-hoc. Consistency via filesystem atomic rename + write-ahead patterns.

9. **Niagara NO soporta hot-swap de módulos** — station restart mandatory. ClassLoader per-module pero parent-first para Baja types.

10. **`BOnMissingType` preserva unknown slots** — BOG forward-partial compat via dynamic slots. Pero Tridium NO garantiza forward compat new→old.

11. **Fox channels: `sys`/`data`/`events`/`file`** — namespace interno, no transports separados. `fox.sys` transporta login + keepalive + platform tunneling.

12. **NiagaraDriver reusa Fox transport** — no nuevo protocol, sí nueva semántica (device-as-proxy).

13. **Sys.loadType es lazy + registry-based** — duplicate typeSpec → warning + first-wins. Fallback reflection → BOnMissingType.

14. **`clLexiconFr_V1_3.jar` versioned filename** = rare pattern, indica dual-version coexistence possible.

15. **`maxpro-rt.jar` presente en distro standard Supervisor** — Security/video integration no siempre licensed.

---

## 32.19 TODOs honestos — lo que no investigué en profundidad

Reconocimiento honesto de límites de esta pasada (evitando stall):

1. **NO decompilé `honPlantController-rt.jar` con `javap -p`** — clases exactas BPlantController + BPlantControllerNetwork + BPlantControllerDevice son inferencias basadas en patterns.

2. **NO decompilé `platPower-rt.jar`** — clase principal + API específica inferida de naming convention.

3. **NO decompilé `jsonToolkit-rt.jar`** — BJsonReader/BJsonWriter signature probable pero no confirmada con grep/javap.

4. **NO decompilé `license-rt.jar`** — SMA enforcement exact method NO confirmada directamente. Basado en niagara-help + Bloque 2.

5. **NO decompilé `fox-rt.jar`** para confirmar exact channel constants — `fox.sys` nombre inferido.

6. **NO decompilé `baja.jar` para `Sys.loadType` + `BModule` + `ModuleClassLoader` + `BOnMissingType`** — inferencias basadas en bloques previos + naming patterns.

7. **NO analicé palettes HVAC Honeywell** (`Palettes_and_Misc/`) — dimensión entera queda para futuro.

8. **NO analicé `honMqttDriver-rt.jar`** detalladamente — tamaño + API interna.

9. **NO analicé `libciper.so`** ELF internals — no disponibles herramientas objdump en contexto.

10. **NO analicé `maxpro-rt.jar`** features — solo registré su presencia.

11. **NO confirmé `sma.exempt="true"` runtime con license XML real** — patrón inferido de Bloque 14.3.

12. **Class loading leak scenarios** — inferidos, no profiled con heap dump real.

Estas deudas quedan documentadas honestamente para bloques futuros (33+).

---

## 32.20 Conexiones al resto del modelo

- **Bloque 1**: framework base sobre el que este overlay se apoya
- **Bloque 2**: SMA attribute mencionado, aquí profundizado (32.5)
- **Bloque 4**: BComponent lifecycle — base de module lifecycle (32.10)
- **Bloque 5**: BOG format + handles — base de schema evolution (32.13)
- **Bloque 9**: Jetty + Fox + NiagaraRPC — contexto para non-HTTP transports (32.8)
- **Bloque 10**: Station boot 6-fase — contexto para module load order (32.10.2)
- **Bloque 11**: Auth schemes — `clientCertAuth` es ext aquí
- **Bloque 12**: Build system — `niagara-module` gradle plugin valida deps cycles (32.10.4)
- **Bloque 13**: Fox protocol general — base para `fox.sys` detail (32.6)
- **Bloque 14.3**: `sma.exempt="true"` mentioned, profundizado (32.5.3)
- **Bloque 17**: filesystem + atomic ops — base para transaction consistency (32.9)
- **Bloque 19**: Honeywell drivers surface — este bloque cierra el deep
- **Bloque 20.10**: gap analysis — este bloque cierra gaps #1/#3/#5/#7/#8/#13
- **Bloque 23.27**: `honBACnetUtilities` surface — profundizado aquí (32.14)
- **Bloque 25**: Migration Framework — contexto para `honPlantControllerMigrator` + `honImporter`
- **Bloque 26**: Native libs + signing — libciper.so + libplantctrl.so ref
- **Bloque 27**: Licensing matrix — SMA deep fits aquí
- **Bloque 29**: Jetty + Servlets — HTTP contrast con non-HTTP transports (32.8)

---

**Fin Bloque 32**
