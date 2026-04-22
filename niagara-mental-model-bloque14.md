# Niagara N4 — Bloque 14: Point counting + License limits + Templates + Batch Editor + EasyTemplates

Parte del mental model. Ver [INDEX.md](INDEX.md) para el mapa completo. Relacionado con Bloques 2 (licensing alto nivel), 7 (drivers framework), 13.1 (federation), y referencia forward al Bloque 16 (Provisioning Service Supervisor-scale).

Este bloque cubre la **operación y escala de dev/ops**: reglas de conteo de puntos para licensing, limits por feature, templates en las 4 formas que coexisten en el framework (Niagara core, EasyTemplates Honeywell, palettes, station templates), Batch Editor Workbench, y el workflow Template/Match/Bind originado en LON pero generalizable a otros drivers.

---

## 14.1 Point counting fundamentals

### 14.1.1 Qué es un "punto contabilizable"

Un punto contabilizable para licensing es toda instancia de `BControlPoint` o sus subclases que:
1. Reside local en la station (no templates no instanciados)
2. Tiene bindings o extensions activos
3. Participa en data collection (history), monitoring (alarmas) o control (drivers)

Los 8 tipos de BControlPoint (Bloque 6.3) **NO se diferencian en el conteo**:
- `BBooleanPoint` / `BBooleanWritable`
- `BNumericPoint` / `BNumericWritable`
- `BEnumPoint` / `BEnumWritable`
- `BStringPoint` / `BStringWritable`

Cada instancia cuenta como 1 punto sin importar tipo, RW/RO, ni cantidad de extensions.

### 14.1.2 Qué cuenta y qué no

| Componente | Cuenta | Motivo |
|---|---|---|
| BControlPoint local instanciado | Sí | Entry físico en BD de la station |
| BControlPoint en template no instanciado | No | Templates no se expanden hasta `instantiate()` |
| Proxy point con BProxyExt bindeado a device | Sí | Punto virtual sobre remoto — cuenta local |
| BHistoryExt en un punto | No separado | Extension, integrada al punto base |
| BAlarmSourceExt en un punto | No separado | Extension, integrada al punto base |
| Virtual points / kitControl blocks con `out` | Sí (si implementan BIPointCountable) | Output slot bindeable = punto |
| BLink entre slots | No | Relación, no punto |
| BFolder, BService, BNetwork | No | No son BControlPoint |
| History records individuales | Separado | Cuenta en `history.limit` o `historyRecord.limit`, no en `point.limit` |
| Punto off-line (device desconectado) | Sí | Cuenta hasta el delete |

**Separación importante**: `history.limit` + `historyExt.limit` + `historyRecord.limit` son **dimensiones independientes** de `point.limit`. Un station puede tener 500 puntos con 1000 history extensions acumuladas si cada punto tiene varias.

### 14.1.3 Totalización en LicenseManager

`NLicenseManager` singleton agrega contadores. Expone:
- `getPointCount()` — total de BControlPoint locales
- `getDeviceCount()` — total de BDevice en todos los BDriverContainer
- `getHistoryCount()` — histories activas
- `getScheduleCount()` — BAbstractSchedule activas

Frecuencia: check on-demand durante `BComponent.add()`, `addChild()`. No poll periódico — es event-driven sobre el tree.

Caching: counters incrementados/decrementados en `added()` / `removed()` callbacks del lifecycle (Bloque 4.3).

Spy page: `/spy/sysManagers/licenseManager` expone features + properties + current counts (inferido del patrón spy Bloque 10.2.4).

---

## 14.2 License limits runtime enforcement

### 14.2.1 Tipos de limits

Los limits son atributos de feature en el XML `.license` (Bloque 2):

| Limit | Unidad | Semántica |
|-------|--------|-----------|
| `point.limit` | int o "none" | Max BControlPoint activos en station |
| `device.limit` | int o "none" | Max BDevice combinados en todos los drivers |
| `history.limit` | int o "none" | Max records históricos acumulados |
| `historyExt.limit` | int o "none" | Max BHistoryExt extensions activas |
| `historyRecord.limit` | int o "none" | Max history records total stored |
| `schedule.limit` | int o "none" | Max BAbstractSchedule instances |
| `camera.limit` | int o "none" | Max cámaras (video drivers) |
| `foxStream.limit` | int o "none" | Max FOX subscriptions concurrent |
| `zone.limit` | int o "none" | Max dashboard zones (Honeywell) |
| `proxyext.limit` | int o "none" | Max proxy extensions (analytics) |
| `algorithm.limit` | int o "none" | Max algorithms (analytics) |
| `alert.limit` | int o "none" | Max alertas |

`"none"` = unlimited. Default si atributo omitido = unlimited también (salvo features específicas).

### 14.2.2 Hard cap vs soft cap

**Hard cap** — default:
- Intentar crear BControlPoint con count == limit → lanza `LicenseLimitExceededException` en `BComponent.add()`
- Bloquea inmediato, sin grace
- Workbench UI muestra error dialog

**Soft cap** — ciertos features tienen behavior degraded:
- Alarm generado (`PointLimitExceeded`, `DeviceLimitExceeded`)
- Polling/updates deshabilitados sobre puntos excedentes
- Operación existente continúa, pero new adds bloqueados

**Grace periods** — no para counts, solo para **expiry de feature**:
- Feature license expira → grace 24-48h antes de deshabilitar módulos dependientes
- Subscription license off-line (Bloque 13.1.1) → cache 24-48h silent grace

### 14.2.3 Workflows al exceder

**Backup restore con más puntos que limit**:
```
Backup: 10,000 puntos | point.limit=5,000
1. RestoreService parsea XML
2. Itera creando BControlPoint...
3. En punto 5,001: LicenseLimitExceededException
4. Restore ABORTA (no partial restore)
5. Opciones: reducir backup (export subset) o upgradear license feature
```

**Hot-add en producción**:
```
Station: count=5,000 | limit=5,000
Operator: Workbench → "Add Point"
1. RPC create via Fox
2. NLicenseManager.checkBeforeAdd() → count==limit → REJECT
3. UI: "Point limit exceeded"
```

**License feature expiry mid-run**:
```
Feature.expiration < now:
1. NLicenseManager detecta via getExpiration()
2. checkFeature() → FeatureNotLicensedException
3. Módulos dependientes se deshabilitan gracefully
4. Current points NO cambian (limits siguen)
5. 24-48h grace: after eso, nuevas operaciones bloqueadas
```

**Federation Supervisor ↔ Subordinate**:
- Supervisor cuenta puntos **locales** (1000 puntos locales contra su `point.limit`)
- Subordinate cuenta puntos **locales** (500 puntos contra su `point.limit`)
- Puntos exportados del Sub al Supervisor vía Fox: cuentan **en origen** (Subordinate), NO en Supervisor (son referencias remotas)
- Cada station valida su propio limit independientemente

---

## 14.3 Ejemplos reales de features (Honeywell + Webs licenses)

**Honeywell.license** (27 features, vendor="Honeywell") — extraído del install:

```xml
<feature name="bport" expiration="2027-03-31"
  history.limit="none" point.limit="none" schedule.limit="none" device.limit="none"/>

<feature name="honEdgeDriver" expiration="2027-03-31"
  history.limit="none" point.limit="none" schedule.limit="none" device.limit="none"/>

<feature name="maxproVideo" expiration="2027-03-31"
  history.limit="none" point.limit="none" schedule.limit="none" device.limit="none"
  camera.limit="16" foxStream.limit="none"/>

<feature name="honEasyTemplate" expiration="2027-03-31"/>
<feature name="honEasyBinding" expiration="2027-03-31"/>
<feature name="honEasyDatabaseManager" expiration="2027-03-31"/>
```

**Webs.license** (150+ features, vendor="Tridium", brand="Webs") — extraído del install:

```xml
<feature name="nCloudDriver" expiration="2027-03-31"
  history.limit="500" point.limit="1000" device.limit="1"/>
<!-- Honeywell Cloud driver: muy restrictivo -->

<feature name="demoStation" expiration="2027-03-31"
  historyExt.limit="10000" historyRecord.limit="10000"/>
<!-- Demo: 10k extensions + 10k records SEPARADOS -->

<feature name="analytics" expiration="2027-03-31"
  alerts="none" algorithms="none" algorithm.limit="none" point.limit="none"
  proxyext.limit="none" device.limit="none" alert.limit="none"/>

<feature name="smartKey" expiration="2027-03-31"
  history.limit="none" point.limit="none" schedule.limit="none" device.limit="6"/>
<!-- SmartKey: solo 6 locks -->

<feature name="adr" expiration="2027-03-31"
  history.limit="none" point.limit="none" device.limit="1"/>
<!-- Demand Response OpenADR: 1 device -->

<feature name="eSignature" expiration="2027-03-31" point.limit="500"/>

<feature name="bacnet" expiration="2027-03-31"
  history.limit="none" point.limit="none" schedule.limit="none" device.limit="none"
  ports="none" export="true"/>
<!-- BACnet: unlimited + export=true permite exportar puntos vía Fox -->

<feature name="http" expiration="2027-03-31"
  history.limit="none" point.limit="none" sma.exempt="true"
  schedule.limit="none" device.limit="none"/>
<!-- HTTP: sma.exempt=true = no requiere SMA (Software Maintenance Agreement) -->

<feature name="developer" expiration="2027-03-31"
  moduleDev="true" skipModuleValidation="true"/>
<!-- Developer: habilita bypass del Bloque 18 -->
```

**Patrón observado**:
- Features de drivers open (bacnet, modbus, http, lon) → typically `point.limit="none"`
- Features premium vendor-specific (nCloudDriver, smartKey, maxproVideo) → limits granulares
- Features edge (eSignature, adr) → limits quirky
- Atributos especiales: `sma.exempt`, `export`, `moduleDev`, `skipModuleValidation`

---

## 14.4 Federation counting — Supervisor vs Subordinate

Regla crítica (clarificación del Bloque 13.1):

| Scope | Cuenta en `point.limit` |
|-------|-------------------------|
| Punto local en Supervisor | Supervisor |
| Punto local en Subordinate | Subordinate |
| Punto del Sub exportado a Super vía Fox | **Solo Subordinate** (origen) |
| Fox stream subscription Super→Sub | `foxStream.limit` del endpoint destino |
| History archiving Sub→Super | `history.limit` del origen (Sub) |

**Implicación operacional**: un Supervisor con 1000 puntos locales puede subscribirse a 10,000 puntos remotos de 10 Subordinates sin exceder su propio `point.limit`. Los 10,000 cuentan solo en sus respectivos orígenes.

---

## 14.5 Spy + alarms + audit licensing

**Spy pages**:
- `/spy/sysManagers/licenseManager` — features listed + expiration + validity + counts
- `/spy/licensing/*` — desglose granular por dimensión

**Alarms automáticos**:
- `PointLimitExceeded` (WARNING severity)
- `DeviceLimitExceeded`
- `HistoryLimitExceeded`
- `ScheduleLimitExceeded`
- `LicenseExpired` (por feature)
- `LicenseExpiresIn` (advance warning)

**Audit via BHistoryService**:
- Point count changes → trend history
- Query: `history:/station/PointCountMetric` → evolución temporal

**CLI**:
- `nre -licenses` → lista features + properties
- `plat.exe` → platform view con license summary

---

## 14.6 Niagara Templates Framework core

### 14.6.1 BComponentTemplate + BTemplateService

Framework Tridium centralizado. License gate: feature `"template"` Tridium.

**Clases principales** (módulo `template-rt.jar`):

- **`BTemplateConfig`** — componente que encapsula metadata + config:
  - `templateName` (identificador único)
  - `uID` (UUID inmutable para tracking global)
  - `version` (BVersion X.Y.Z)
  - `deployed` (flag compiled/ready)
  - `propagated` (estado de propagación a instances)

- **`BTemplateService`** — singleton orchestrador:
  - HashMap<Object, BTemplateConfig> interno
  - Registro centralizado: cada BTemplateConfig auto-registra en `register()`
  - Children: un `BTemplateInfo` por template registrado (property slot `t?`)
  - Fox channel "template" registrado en BFoxChannelRegistry para RPC remoto

- **`BTemplateInfo`** — descriptor expuesto:
  - `locationOrd` (BOrd al BTemplateConfig real)
  - `ntplSignature` (firma criptográfica del BOG deployado)
  - `localSignature` (hash del tree local actual)

- **`BConfigBinding`** — struct de parámetro instanciable:
  ```java
  BOrd targetOrd;       // destino del parámetro externo
  String sourceSlot;    // slot del template (ej. "device/address")
  String targetSlot;    // slot del target (ej. "address")
  String userTip;       // hint UI para el operador
  ```

### 14.6.2 Formato `.ntpl` y persistence

`.ntpl` file = ZIP comprimido con:
- `template.bog` — subtree BOG serializado
- `template-manifest.xml` — manifest con binding info + version + compat
- `shared/config/` — config compartida opcional

Workflow de creación:
1. Dev arma estructura en station (ej. BACnet device + 5 proxy points + alarm ext + history ext)
2. Envuelve en `BTemplateConfig` dentro de la station
3. Workbench invoca action `makeApplicationTemplate(useMinorVersion)` del servicio
4. Genera `.ntpl` file con snapshot + manifest
5. Distribuir `.ntpl` via import dialog a otras stations

### 14.6.3 Binding + versioning + upgrade

**Parametrización**: `BVector<BConfigBinding>` en property `pxEditBindings`. Cada binding dice: "al instanciar, permitir customizar `sourceSlot` desde `targetOrd.targetSlot`".

**Instantiation**:
```java
BTemplateService ts = (BTemplateService) BServiceManager.lookupService(BTemplateService.TYPE);
BComponentTemplate tmpl = ts.loadTemplate(ord("module:path/MyTemplate.ntpl"));
BComponent instance = tmpl.instantiate(parent, params);
```

**NO hay propagación automática**: cambios al template source NO afectan instances ya creadas. Upgrade explícito requerido:

`BUpgradeTemplateJob` — procesa BVector de templates:
- Compara `ntplSignature` vs `localSignature`
- Si difieren, ejecuta delta migrations
- Properties `deployed` / `propagated` marcan transición

Esto es intencional: templates son snapshots point-in-time, no links vivos.

---

## 14.7 EasyTemplates Honeywell (framework distinto)

### 14.7.1 Overview

**Framework DIFERENTE al core Niagara Templates**. Módulo `easyTemplating-rt.jar` (Honeywell vendor-specific, no Tridium). License gate: feature `honEasyTemplate` en Honeywell.license.

Directorio: `/home/cristian/Niagara4.14/OptimizerSupervisor/EasyTemplates/` (verificado empírico).

Layout:
```
EasyTemplates/
├── easytemplating.properties     # config: et.objectType=Simple Object, et.selectedIndex=0
├── SimpleObjects/
│   └── Default_lib/version.xml   # <library version="0.0" name="..."/>
└── ComplexObjects/
    └── Default_lib/              # (vacío inicial)
```

Dirs son esqueleto. Content real en `.jar` + resolución runtime via services.

### 14.7.2 SimpleObjects vs ComplexObjects

**SimpleObjects** (extensión `.etso`):
- Templates atómicos (single device simple, puntos agregadores basic)
- UI wizard simplificado
- Pocos parámetros

**ComplexObjects**:
- Templates multi-componente (device + puntos + extensions interconectados)
- Parametrización avanzada (direcciones BACnet, descripciones dinámicas, bindings cruzados)
- Resolución de dependencias entre sub-componentes

Ambos manejados por `BEasyTemplate` component:
```java
BString templateName;        // nombre ingresado por UI
BOrd templatePx;             // referencia a archivo template .px (ComplexObjects)
BOrd popupPx;                // referencia a popup.px (UI helper)
Action generateTemplate;     // invoca doGenerateTemplate()
```

### 14.7.3 Parametrization workflow

`generateTemplate()` flow:
1. UI selecciona template del picker Workbench
2. User ingresa nombre instancia
3. Invoca action `generateTemplate()`
4. `doGenerateTemplate()`:
   - Resuelve `templatePx` BOrd → BIFile
   - Extrae NEQL query descriptor del .px
   - `EasyTemplatingGenerationHelper.generateTemplate(component, name, neqlQuery, templatePx, popupPx)` ejecuta
   - Carga estructura del template .px
   - Resuelve parámetros via `EasyTemplatingUtils`
   - Genera componentes con names customizados
   - Crea property bindings + extensions automáticamente
5. Result: subtree completo bajo parent seleccionado

**NEQL query**: extracto semántico de jerarquía (ej. "CentraLine/Zone5/VAV1") que UI usa para prefill parameters.

### 14.7.4 Servicios + módulo

- `BEasyTemplatingService` — orquestador principal
- `BEasyTemplatingVirtualService` — soporte para virtual points (Bloque 13.2.3)
- Tipos registrados: `EasyTemplate`, `EasyTemplatingLinker`, `SimpleObjectFile`, `LoadVirtualSlotsJob`

**Dependencias del módulo**: `template-rt`, `control-rt`, `driver-rt`, `bacnet-rt`.

### 14.7.5 Distinción core vs EasyTemplates

| Aspecto | Niagara Templates (core) | EasyTemplates (Honeywell) |
|---------|--------------------------|----------------------------|
| License | `template` (Tridium) | `honEasyTemplate` (Honeywell) |
| Módulo | `template-rt.jar` | `easyTemplating-rt.jar` |
| Formato | `.ntpl` (ZIP: BOG + manifest) | Descriptor `.px` + `.etso` |
| Paradigma | Snapshot de subtree serializado | Generación dinámica on-demand |
| Parametrización | `BConfigBinding` explícito | NEQL query + utils automáticos |
| UI | Import dialog + params | Wizard integrado Workbench |
| Auto-binding | Manual | Automático via NEQL |

---

## 14.8 Directorios relacionados (applicationTemplates / stationTemplates / templates)

User Home verificado empírico:
- `/home/cristian/Niagara4.14/OptimizerSupervisor/applicationTemplates/` — vacío
- `/home/cristian/Niagara4.14/OptimizerSupervisor/stationTemplates/` — vacío
- `/home/cristian/Niagara4.14/OptimizerSupervisor/templates/` — vacío

Install defaults: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/defaults/workbench/`

Propósito:
- `applicationTemplates/` — pre-built configs importables como unidad (ej. HVAC Energy Dashboard)
- `stationTemplates/` — wizards para nueva station (ej. "Supervisor on Windows", "Lightweight Controller")
- `templates/` — genéricos override de defaults/workbench/

**Formato**: `.ntpl` (idéntico al core Niagara Templates).

---

## 14.9 Palettes vs templates — los 4 mecanismos coexistentes

Niagara soporta **4 mecanismos** complementarios para reutilización de configuración:

| Mecanismo | Formato | Ubicación source | Instanciación | Auto-binding | License |
|-----------|---------|------------------|---------------|--------------|---------|
| Niagara Template core | `.ntpl` (ZIP: BOG + manifest) | Tree de station o import | `makeApplicationTemplate` → instalable | Manual via BConfigBinding | Tridium `template` |
| EasyTemplate Honeywell | `.px` + `.etso` descriptores | `EasyTemplates/` user home | UI action `generateTemplate()` | Automático via NEQL | Honeywell `honEasyTemplate` |
| Palette | `.palette` (ZIP: `file.xml` BOG) | `Palettes_and_Misc/palettes/` | Drag-drop en wiresheet | Structural copy (sin link al source) | Ninguna |
| Station Template | `.ntpl` | `newStations/` dir | Wizard en Workbench | N/A (bootstrap) | Tridium `template` |

**`.palette` files** en este install:
- `HoneywellSubmeter.palette` (13 K)
- `hvfd.palette` (13 K)
- `XL15C.palette` (573 K)

ZIP interno con `file.xml` (BOG serializado en XML comprimido). Drag-drop crea copia local sin mantener referencia al source — equivalente a "clipboard snippet".

**Coexistencia típica**: un Workbench puede simultáneamente:
- Importar application templates (`.ntpl`) vía dialog
- Generar templates on-the-fly con EasyTemplates wizard
- Drag-drop palettes predefinidas
- Crear stations desde wizard

---

## 14.10 Device templates + driver integration

Pattern crítico: template que encapsula `BDevice` + subtree de proxy points + extensions pre-wired.

Ejemplo real: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/Palettes_and_Misc/Spyder Model 5/templates/Spyder Model 5 VAV Templates 108`

Contenido típico del template:
- Device BACnet (network, address, instance ID — parametrizados via BConfigBinding)
- 10-20 proxy points (Temperature, Setpoint, DamperPosition, Fan, etc.)
- AlarmSourceExt pre-wired a puntos críticos
- HistoryExt con interval configurado
- Relations / links internos entre componentes

Palettes_and_Misc con templates vendor-specific verificados:
```
Palettes_and_Misc/
├── Spyder Classic Files/
├── Spyder Model 5/
│   ├── firmware/
│   └── templates/                # .ntpl files
├── Spyder Model 7/
│   ├── firmware/
│   └── template/                 # Spyder 7 VAV application Rev 01_117
├── TC300/, TC500/, TR100/        # Honeywell thermostats
├── Optimizer Unitary/
├── CIPer Model 30/
├── BACnetFFT_N4_Reflash/
└── palettes/                     # .palette files
```

Workflow al instanciar:
1. Import `.ntpl` → wizard prompts (Device Network Address? Instance ID? Zone Name?)
2. Template instancia:
   - BACnet device con `network.address=<user>`, `instance=<user>`
   - N proxy points generados y bindeados
   - BLink auto-creados a props del device (scan on startup)
   - AlarmSourceExt activado (trigger on `Temperature > 30°C` example)
   - HistoryExt activado (log every 5 min)
3. Result: subsystem HVAC VAV funcional con config mínima manual

**License dependency**: template con BACnet device requiere license `bport` o `bacnet` activa.

---

## 14.11 Batch Editor (Workbench tool)

### 14.11.1 Overview

Tool integral de Workbench para seleccionar N componentes (ej. 100 NumericPoints) y aplicar edits simultáneos atómicamente. Crítico en comisionamiento a escala (100 VAV boxes, arrays de sensores).

**UI flow**:
1. Seleccionar múltiples en PointManager o wiresheet (Ctrl+click, Shift+click)
2. Right-click → Batch Edit → abre editor tabular
3. Tabla editable por columnas (cada columna = slot/prop/facet)
4. Modificar valores en una o varias columnas
5. Validate → preview de cambios
6. Commit → aplicar todos atómicamente

### 14.11.2 Arquitectura

- **`BBatchJobService`** — `BAbstractService` orquestador. License gate: feature `"provisioning"` (NO es gratis).
- **`com.tridium.batchJob.ui.BBatchJobList`** — UI list de jobs
- **`com.tridium.batchJob.ui.BJobStageBuilder`** — gestor de stages de edición
- **`BThreadPoolJobQueue`** — queue con `maxThreads` configurable (default 1 secuencial)

Diferencias con `BJobService` base:
- `BBatchJobService` persiste summaries hasta disposal explícito (BJobService descarta rápido)
- Puede generar alarmas al completar jobs
- License gate `provisioning`

Job structure: **Stages** (etapas) → **Steps** (pasos) dentro de cada stage. Un batch edit sobre 100 points = 1 job con 1 stage con 100 steps.

### 14.11.3 Atomicidad y rollback

- Cada stage commit atómico en BOG
- Si falla 1 de 100 steps: comportamiento depende de modo
  - Optimista: los 99 ya aplicados persisten; error reportado para el fallido
  - Pesimista: transacción revierte completa
- Evidencia de código (`BJobStage`, `BJobStep`) sugiere rollback **limitado** — favor observar behavior empírico

### 14.11.4 Limitaciones + ejemplos

Tipos permitidos:
- Cualquier subclase BControlPoint (Numeric, Boolean, Enum, String + Writable)
- Extensions (HistoryExt, AlarmSourceExt add/remove)
- Facets (si ya existen en el punto)

**NO soportado directamente**:
- Batch edit de BDevice, BNetwork level (requiere programación explícita)
- Pattern replacement style `${i}` (Visio-style) — NO built-in. Workaround: BajaScript o script externo.

Ejemplos típicos:
1. "Set Units=degF a 200 temperature points" — seleccionar, Batch Edit columna Units → degF → Commit
2. "Add HistoryExt con interval=15min a 50 flow points" — seleccionar, Add Extension → HistoryExt, interval=900s → Commit
3. "Increase alarmPriority de 100 a 255 en 50 critical points"
4. "Disable polling a puntos obsoletos" — set enabled=false bulk

---

## 14.12 LON Template/Match/Bind workflow

Profundiza Bloque 7.3.3. Pattern template-match-bind originado en LON pero generalizable.

### 14.12.1 LonMark profile + XIF files

LonMark = standard abierto de Echelon. Define profiles (perfiles) de devices que enumeran Network Variables (NVs), config properties, functional objects.

**XIF** (eXternal Interface File):
- Texto plano + XML
- NVs declaradas: `nvoTemp` (output SNVT_Temp), `nviCmd` (input SNVT_switch), etc.
- Config properties
- Functional objects ("Temperature sensor", "On/Off controller")
- Transformación Niagara: `com.tridium.lonworks.util.xif.XifToXDevice` parsea XIF → `com.tridium.lonworks.xml.XLonDevice`

### 14.12.2 Template registration via ProgramId

`DeviceDef` (utility en `lonworks-rt.jar`) mapea ProgramId del device LON (8 bytes hex) a:
- Clase Java directa: ej. `cl=lonHoneywell:Q7300`
- XML file (LNML): ej. `xml=lonSiebe/Mnlrv3.lnml`

Entries en `module-include.xml`:
```xml
<def name="lonworks.80 00 0c 50 3c 03 04 17" value="cl=lonHoneywell:Q7300" />
<def name="lonworks.80 00 16 50 0a 04 04 0a" value="xml=lonSiebe/Mnlrv3.lnml" />
```

**Wildcards**: `80 00 8e 10 0a 04 0* **` mapea rango de ProgramIds (un vendor + familia entera).

### 14.12.3 Match — discovery + profile identification

`BLonLearnJob` ejecuta "learn node" service sobre devices LON:
- Lee domain table + address table + NV config table
- Busca devices nuevos (no configurados) o con ZeroNeuronId

Matching flow:
1. Obtiene ProgramId del device
2. Consulta `DeviceDef` registry
3. Match exacto → instancia la clase template
4. Wildcard match → instancia template genérico
5. No match → crea `BDynamicDevice` + fallback a learning manual de NVs

`BLearnParameter` struct controla:
- `useLonObjects` — particionar NVs en LON objects específicos (ej. "Sensor Object 0", "Actuator Object 1") o aplanar en 1 contenedor

### 14.12.4 Bind — auto-create proxy + NV binding

Una vez matched:
1. **Proxy creation**: por cada NV en template → `BLonPoint` (proxy en Niagara)
2. **Extension binding**: `javax.baja.lonworks.proxy.BLonPointDeviceExt` vincula point ↔ NV remota
3. **Sync automático**: change en NV remota → proxy point refleja; change proxy → NV sincroniza
4. **learnNv action** en `BLonDevice` — re-learn NVs si se añaden post-discovery

Binding config:
- Tipo dato: SNVT_Temp vs custom types
- Refresh/polling interval
- Alarm mapping (qué NV → qué BAlarmRecord)

### 14.12.5 Bind Tool Workbench UI

Workbench expone:
- `BLonXmlEditor` + tools asociados
- Review device matches post-discovery
- Approve/reject individual binds antes de persist
- Ajustar parámetros de binding (units, scaling, polling)
- Crear/editar custom XIF para profiles non-standard
- `BLonXmlCreate` — compilar LNML desde XIF

---

## 14.13 Template/Match/Bind en otros drivers

Pattern agnóstico a driver, manifestación diferente:

| Driver | Discovery | Template | Match | Auto-binding |
|--------|-----------|----------|-------|--------------|
| **LON** | BLonLearnJob (domain/address/NV tables) | XIF + ProgramId registry + wildcards | ProgramId → Class/LNML | Fuerte — auto proxy + NV binding |
| **BACnet** | I-Am broadcast + device object list | Vendor-id + object-type → device definition (builtin, no `BBacnetTemplate` explícito) | Device identifier matching | Medio — descubre objects, manual binding a proxy |
| **Modbus** | Sin auto-discovery | Register map vendor-specific (ej. "Schneider Altivar 71" register map) | Manual vendor + model selection | Low — `BModbusPointManager` crea puntos desde register map, pero manual |
| **OPC UA** | Browse address space | Tag name/type patterns (regex) | Tag type matching | Medio — auto-subscribe a data changes |
| **OPC DA** | Similar browse | — | Manual | Low |
| **MQTT** | Sin protocol discovery | Topic regex patterns (`building/[floor]/[zone]/temperature`) | Manual regex definition | Low — subscribe + payload parsing |

**Fuerte auto-provisioning**: LON (XIF + ProgramId → full binding)
**Medio**: BACnet, OPC UA
**Low (manual)**: Modbus, MQTT

---

## 14.14 Workflow end-to-end (ejemplo 100 VAV boxes)

Scenario: comisionamiento 100 VAV boxes HVAC con Spyder Model 5 controllers, red BACnet IP.

1. **Identify**: 100 VAV, IP 192.168.1.100-199, BACnet
2. **Load palette**: Workbench → Palette sidebar → `Palettes_and_Misc/Spyder Model 5/templates/` → drag "Spyder Model 5 VAV Templates 108" → wiresheet
3. **Configure first**: `VAV_01` con IP 192.168.1.100, subnet=1, node=1
4. **Clone 99 veces**: Copy+paste → Niagara auto-increments display names (VAV_02...VAV_100). Properties numéricas (IP octets) NO auto-increment.
5. **Batch IP edit**: seleccionar 100 VAV → Batch Edit → columna `networkAddress`. Pattern replacement no nativo → usar BajaScript para calcular IPs incrementalmente:
   ```js
   var vavs = baja.Ord.make("station:|slot:/Drivers/BacnetNetwork").resolve().get().getChildren();
   for (var i = 0; i < 100; i++) {
     vavs[i].set("address", "192.168.1." + (100 + i));
   }
   ```
6. **Batch alarm config**: seleccionar 100 VAV → Batch Edit → `alarmPriority=128`, `alarmSeverity=2` → Commit
7. **Batch history**: subset 50 temp points → Batch Edit → Add HistoryExt, interval=900s → Commit
8. **Discovery + bind**: BACnet discovery job → scan red → match 100 devices → approve en Bind Tool → auto-create proxy points
9. **Deploy via Provisioning**: push config a subordinate stations via `BNiagaraNetworkJob` (ver 14.15)
10. **Commissioning**: verificar live data flow + alarmas + history records

**Limitación observada**: ausencia de pattern replacement `${i}` built-in en Batch Editor obliga a scripting para edits con patrones numéricos. Workbench tiene Copy+Paste funcional pero no macro-expansion.

---

## 14.15 Relación con Provisioning Service (forward al Bloque 16)

Batch Editor = station-scoped (100 puntos en 1 station). Provisioning Service = Supervisor-scoped (N stations, jobs en paralelo).

Clases clave (profundización en Bloque 16):
- `javax.baja.provisioningNiagara.BNiagaraNetworkJob` — batch job escalado
- `BNiagaraNetworkJobPrototype` — template para networked batch jobs
- `BForEachStationStage` — itera steps por cada station en network
- `BProvisioningBackupStep` — online/offline backup N stations paralelo
- `BUpdateLicensesJobStep` — sync licenses desde license server a N stations

**License gate**: `provisioning` feature + rol de Supervisor.

Mechanical similarity con Batch Editor: multi-target iteration, stage-based execution, atomicity per stage. Difiere en scope (network vs tree).

**Conexión típica**: Batch Editor prepara single-station config (100 VAV + history + alarms); Provisioning Service replica esa config a 50+ stations.

---

## 14.16 Hallazgos críticos del bloque

1. **nCloudDriver feature** tiene limits muy restrictivos (`point.limit=1000, device.limit=1, history.limit=500`) — Honeywell Cloud driver NO es "unlimited" aunque otros drivers Honeywell sí lo son.

2. **`demoStation`** tiene `historyExt.limit=10000` SEPARADO de `historyRecord.limit=10000` — son dimensiones independientes.

3. **`http` feature con `sma.exempt=true`** — no requiere Software Maintenance Agreement. Atributo raro útil para operational tiers sin SMA continuo.

4. **`bacnet` feature con `export=true`** — permite exportar puntos del station vía Fox (Bloque 13.2). No todos los drivers tienen este atributo — puede controlar visibilidad cross-station.

5. **Hard cap default + 24-48h grace solo para expiry** — point counting NO tiene grace. Excedencia lanza `LicenseLimitExceededException` inmediato.

6. **Federation counting en origen** — puntos de Subordinate exportados a Supervisor cuentan SOLO en el Subordinate. Un Supervisor puede agregar 10,000 puntos remotos sin exceder su local `point.limit`.

7. **Niagara Templates core NO auto-propaga** — instances no actualizan al cambiar template source. Upgrade requiere `BUpgradeTemplateJob` explícito.

8. **EasyTemplates es framework distinto** al core — módulo `easyTemplating-rt.jar` Honeywell, descriptor `.px + .etso` (no `.ntpl`), NEQL query para parametrización, license `honEasyTemplate`.

9. **4 mecanismos coexistentes de reusability**: Niagara Templates (`.ntpl`), EasyTemplates (`.px/.etso`), Palettes (`.palette`), Station Templates (`.ntpl` wizard). Usos complementarios.

10. **Palettes solo copy-estructural** — no mantienen link al source. Cambios en `.palette` no propagan a instancias existentes. Son "clipboard snippets" persistidos.

11. **Device templates parametrizan via BConfigBinding** — IP, BACnet instance ID, etc. Wizard en Workbench prompts al instanciar.

12. **Batch Editor requiere license `provisioning`** — feature gated. Sin license, tool grayed out en Workbench.

13. **Pattern replacement `${i}` NO built-in** — Batch Editor no tiene macro expansion Visio-style. Workaround: BajaScript o tool externo para edits numéricos incrementales.

14. **LON template registry usa ProgramId de 8 bytes con wildcards** — `80 00 8e 10 0a 04 0* **` permite match por vendor + familia completa.

15. **Template/Match/Bind strength por driver**: LON fuerte (auto-discovery + auto-binding), BACnet/OPC medio, Modbus/MQTT low (manual).

16. **Spyder templates reales** en `Palettes_and_Misc/Spyder Model 5|7/templates/` vienen con firmware/ + templates/ — package completo por modelo de controlador Honeywell.

---

## 14.17 Conexiones con otros bloques

- **Bloque 2 (Licensing)**: este bloque profundiza `point.limit`/`device.limit`/`history.limit` runtime enforcement mencionados a alto nivel.
- **Bloque 4 (Baja)**: templates usan BComponent lifecycle (`added()`/`removed()`) para count updates + template instantiation.
- **Bloque 5 (ORD + BOG)**: `.ntpl` es ZIP de BOG serializado; BOrd parametriza templates.
- **Bloque 6 (Control)**: BControlPoint subclases son lo que cuenta para `point.limit`. kitControl virtual points cuentan si implementan BIPointCountable.
- **Bloque 7 (Drivers)**: Template/Match/Bind profundiza 7.3.3 (LON) y extiende a BACnet/Modbus/OPC/MQTT.
- **Bloque 8 (History/Alarm)**: HistoryExt + AlarmSourceExt no cuentan separado en `point.limit` pero tienen sus propios limits (`history.limit`, `historyExt.limit`).
- **Bloque 10.3 (Backup)**: restore de backup con puntos > limit aborta con `LicenseLimitExceededException`.
- **Bloque 13.1 (Niagara Network)**: federation counting clarificado — puntos remotos cuentan en origen.
- **Bloque 16 (próximo — Analytics + Provisioning)**: `BNiagaraNetworkJob` + `BForEachStationStage` son el Supervisor-scale del Batch Editor.
- **Bloque 17 (Filesystem)**: `EasyTemplates/` en User Home, `Palettes_and_Misc/` en Install Home.

---

## Engram topic keys

- `niagara/licensing/point-counting-limits-runtime`
- `niagara/templates/niagara-core-vs-easytemplates-honeywell`
- `niagara/operations/batch-editor-lon-template-match-bind`
