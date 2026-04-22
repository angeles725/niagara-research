# BLOQUE 2 — Licensing Niagara N4

Fecha: 2026-04-20
Fuente primaria: `niagara-help/devguide-clean/licensing.txt`
Fuente empírica: `security/licenses/*.license`, `security/certificates/*.certificate`, LicenseUtil.java decompilado
Validado contra: corpus Honeywell OptimizerSupervisor N4.14.0.162 (hostId `Win-6E6E-10AC-D1DD-8276`)

---

## 2.1 Los 5 elementos del modelo de licensing

La documentación oficial define el modelo en 5 piezas:

| Elemento | Qué es | Dónde vive |
|----------|--------|------------|
| **HostId** | String corto que identifica UNA máquina física (JACE, workstation, server) | `nre -version` |
| **Certificate** | Archivo `.certificate` que mapea un vendor id a una **public key**. Firmado digitalmente por Tridium | `{home}/security/certificates/` |
| **License File** | Archivo `.license` XML que habilita features para un `hostId` específico, firmado digitalmente por un vendor | `{home}/security/licenses/` |
| **Feature** | Ítem único en la license database, keyed por `vendor + featureName`. Ej: `Tridium:station`, `Honeywell:redLink` | Dentro de cada `.license` |
| **API** | `javax.baja.license.*` — `Sys.getLicenseManager().checkFeature(vendor, name)` | Runtime |

### HostId en el corpus
Todos los 3 archivos de licencia apuntan a `Win-6E6E-10AC-D1DD-8276`. Ese es un identificador opaco derivado de atributos de la máquina (MAC address, VM UUID, etc.). **Si movés los `.license` a otra máquina, se invalidan automáticamente** — el `hostId` no matcheará.

---

## 2.2 Formato del archivo `.license` (schema XML oficial)

### Template oficial del devguide
```xml
<license
    version="1.0"
    vendor="Acme"
    generated="2002-06-01"
    expiration="never"
    hostId="Win-0000-1111-2222-3333">
  <feature name="alpha"/>
  <feature name="beta" expiration="2003-01-15"/>
  <feature name="gamma" count="10"/>
  <signature>MC0CFACwUvUwA+mNXMfogNb6PVURneerAhUAgZnTYb6kBCsvsmC2by1tUe/5k/4=</signature>
</license>
```

### Atributos root
- **vendor** (required): quién firma el archivo. Debe haber un `.certificate` con el mismo nombre en `certificates/`.
- **hostId** (required): máquina específica. Invalidación automática si no matchea.
- **expiration** (required): YYYY-MM-DD o `never`. Master expiration — aplica a todo el archivo.
- **generated** (required): YYYY-MM-DD. Si `now` < generated → invalid (anti-clock-skew).
- **version** (required): versión del schema de la licencia (en los 3 archivos del corpus: `version="4.15"`).

### Features
Cada `<feature>` tiene:
- **name** (required)
- **expiration** (optional, default `never`): override del master expiration si es más restrictivo
- **property attributes arbitrarios**: `count="10"`, `device.limit="none"`, `point.limit="500"`, `sma.exempt="true"`, `brandId="Webs"`, etc. Cada módulo que usa el feature lee las properties que le importan.

### Signature
El `<signature>` es una firma digital (ECDSA probablemente, base64) del contenido del archivo, creada con la **private key** del vendor. El framework la verifica contra la **public key** del `.certificate` correspondiente.

### Validation pipeline en boot
1. Match hostId → si distinto, rechaza.
2. Check generated date ≤ now → si not, rechaza.
3. Check master expiration > now → si expirado, rechaza.
4. Resolve vendor → busca `{vendor}.certificate` en certificates/.
5. Verify signature con la public key del certificate → si inválida, rechaza.
6. Merge features al license database global.

**Hallazgo crítico**: la validación es "por archivo completo". Si 1 sola de las 5 falla, el archivo entero se descarta — NO se cargan features parcialmente.

---

## 2.3 Los 3 archivos de licencia de la distribución Honeywell OptimizerSupervisor

### `Webs.license` — Tridium firma el framework core (153 features)
```xml
<license vendor="Tridium" expiration="2027-03-31" hostId="Win-6E6E-10AC-D1DD-8276"
         version="4.15" generated="2026-04-02">
  <feature name="brand" accept.station.out="*" accept.wb.in="*"
                        accept.wb.out="*" brandId="Webs" accept.station.in="*"/>
  <feature name="station" expiration="2027-03-31" station.limit="128" ... />
  <feature name="workbench" expiration="2027-03-31" admin="true"/>
  <feature name="developer" expiration="2027-03-31" moduleDev="true" skipModuleValidation="true"/>
  ...
```

- **Firma**: Tridium (por eso el nombre Webs es solo el archivo y el brand, NO el vendor)
- **Features core del framework**: `nre`, `station`, `workbench`, `crypto`, `tls`, `http`, `bacnet`, `modbus`, `fox-tunneling`, etc.
- **Feature `brand`**: es lo que transforma la UI/behavior de Niagara en "Webs". brandId="Webs" — todas las branding hooks leen esta property.
- **Feature `developer`**: tiene los attributes `moduleDev="true"` y `skipModuleValidation="true"` — ESTO ES EL BYPASS DE VALIDACIÓN que discutimos la sesión previa. En una distribución de production NO debería estar, pero en esta distribución de demo/dev SÍ está. Con este feature activo, el framework SALTEA la validación de signatures de módulos — efectivamente desarma el lockdown OEM.
- **Feature `http`** (usado por httpClient): tiene `sma.exempt="true"` — el httpClient NO requiere SMA activa.

### `Honeywell.license` — Honeywell firma features específicas (27 features)
```xml
<license vendor="Honeywell" expiration="2027-03-31" hostId="Win-6E6E-10AC-D1DD-8276"
         version="4.15" generated="2026-04-02">
  <feature name="honEdgeDriver" expiration="2027-03-31" history.limit="none" ... />
  <feature name="redLink" ... />
  <feature name="spyderProgrammable" ... />
  <feature name="honNiagaraApi" ... />
  <feature name="HBDashboard" zone.limit="none" ... />
  ...
```

- **Firma**: Honeywell (Honeywell.certificate)
- **Features**: productos Honeywell-specific (Spyder, RedLink, Edge, Dashboard, etc.)
- **Todos expiran 2027-03-31** — un único ciclo de renovación contractual.

### `HoneywellCentraLine.license` — subbrand con 1 sola feature
```xml
<license vendor="HoneywellCentraLine" ...>
  <feature name="clCbus" ... />
</license>
```

CentraLine es una marca europea de Honeywell. El único feature `clCbus` es un driver legacy. Existe como archivo separado porque está firmado por un cert distinto.

### Reflejo exacto en `certificates/`
Por cada vendor que firma, hay un `.certificate`:
```
Tridium.certificate          ← valida Webs.license
Honeywell.certificate        ← valida Honeywell.license
HoneywellCentraLine.certificate ← valida HoneywellCentraLine.license
```

Los certificates son granted por Tridium — es decir, Tridium firma los certificates de Honeywell y HoneywellCentraLine con su propia root key. Es una **chain of trust**: Tridium root → vendor cert → license signature.

---

## 2.4 SMA — Software Maintenance Agreement

### Qué es (conceptual)
SMA es el contrato de maintenance de un módulo. Conceptualmente: "pagaste soporte/updates hasta el día X". Niagara expone esto como metadata del license file.

### Cómo se codifica (empírico)

**1. SMA master date por vendor** — el LicenseManager expone:
```java
Optional<Long> getLicenseMaintenanceExpiration(String vendor)
```
Devuelve el millis timestamp de la SMA expiration del `.license` del vendor.

**2. SMA exempt a nivel feature** — attribute `sma.exempt="true"` en el `<feature>`:
```xml
<feature name="http"         sma.exempt="true" />  <!-- httpClient NO requiere SMA -->
<feature name="cloudLink"    sma.exempt="false"/>  <!-- cloudLink SÍ requiere SMA -->
<feature name="jsonToolkit"  sma.exempt="true" />  <!-- NO requiere SMA -->
```

**3. Pattern de check en código** (ejemplo de `LicenseUtil.java` de httpClient):
```java
public static final String LICENSE_FEATURE_NAME = "http";
public static final String SMA_EXEMPT_ATTRIBUTE = "sma.exempt";

public static Feature getLicense() {
    return Sys.getLicenseManager().getFeature("tridium", "http");
}

public static boolean isSmaExempt() {
    return getLicense().getb("sma.exempt", false);
}

public static Optional<Long> getSmaExpiration() {
    NLicenseManager lm = (NLicenseManager) Sys.getLicenseManager();
    return lm.getLicenseMaintenanceExpiration(Sys.getBajaVendor());
}

private static void checkSma() {
    if (!isSmaExempt()) {
        Optional<Long> sma = getSmaExpiration();
        if (sma.isPresent() && sma.get() < System.currentTimeMillis()) {
            throw new FeatureNotLicensedException(NO_SMA_MSG);
        }
    }
}
```

### Cómo funciona el BSMAExpirationMonitor de httpClient
Es un BComponent con Property `mode` (disabled/warning/fault) y `warnBelow` (days threshold).

Ciclo:
1. En `started()`: `setExempt(LicenseUtil.isSmaExempt())`. Si exempt → report ok, nada que monitorear.
2. Si no exempt: extrae `getSmaExpiration()`.
3. Si SMA no encontrada → report "no SMA".
4. Si `now >= expiration` → `reportExpired()` → raise fault alarm.
5. Si `now + warnBelow*day >= expiration` → `reportWarning()` → raise offNormal alarm.
6. Caso contrario → `reportOk()` + schedule next check.

### Hallazgo crítico sobre SMA
SMA es **soft enforcement**: levanta alarmas pero **NO bloquea la operación del módulo**. Esto es distinto de la validación de features expiradas o permissions — esas SÍ bloquean. SMA es un contract reminder, no un kill switch (al menos en httpClient — otros módulos pueden ser más estrictos).

---

## 2.5 Niagara vs módulo license — la distinción

| Nivel | Qué licencia | Quién emite | Dónde |
|-------|--------------|-------------|-------|
| **Niagara core** | Framework runtime (station, workbench, nre, web, fox, crypto, tls) | Tridium | `Webs.license` feature `station`, `workbench`, `nre` |
| **Tridium modules** | bacnet, modbus, http, lonworks, etc. | Tridium | `Webs.license` feature `bacnet`, `modbus`, etc. |
| **OEM vendor modules** | Productos Honeywell (redLink, honEdgeDriver) | Honeywell | `Honeywell.license` |
| **3rd party modules** | cualquier vendor registered en Tridium | Ese vendor | `{vendor}.license` |

**Resumen**: no hay "una licencia de Niagara" vs "una licencia por módulo". Hay **UN license database global** que mergea todas las features de todos los `.license` validados. Cada módulo que arranca hace `checkFeature(vendor, name)` contra ese database.

---

## 2.6 Feature flags y attributes — cómo un módulo lee properties

Cada feature puede llevar attributes arbitrarios. Ejemplos reales:
- `bacnet export="true"` → habilita BACnet export capability
- `rdbOracle history.limit="10" historyImport="true"` → Oracle driver con 10 max histories
- `secPhotoId device.limit="1" asureId.limit="1"` → 1 device, 1 AssureID reader
- `maxproVideo camera.limit="16" foxStream.limit="none"` → 16 cameras, unlimited fox streams
- `mobile schedule="true" propsheet="true" alarm="true" px="true" history="true" session.limit="none"` — feature flags granulares dentro del mobile feature

### API para leer attributes
```java
Feature f = Sys.getLicenseManager().getFeature("tridium", "bacnet");
String exportStr = f.get("export");          // "true" (String)
boolean export = f.getb("export", false);    // true
int deviceLimit = f.geti("device.limit", 0); // int
```

**"none" como valor**: convención para "unlimited". Las implementaciones chequean `"none".equals(v)` y skipean el check de límite.

---

## 2.7 Honeywell OEM overlay — qué hace distinto

Respecto a una instalación "Tridium puro" hipotética, Honeywell OptimizerSupervisor:

1. **Brand rewrite**: `<feature name="brand" brandId="Webs"/>` cambia el branding de Niagara a "Webs" (producto Honeywell). Todo el UI y el default config se adapta al brand.
2. **OEM cert + license separada**: en vez de solo `Tridium.certificate`, hay 3. Honeywell firma su propio set de features.
3. **Policy lockdown**: (cubierto en Bloque 3) — el OEM instala un `bin/policy/signing.properties` que HARDCODEA un cert específico como trust anchor para módulos con `<permissions>`. Un módulo custom firmado por un cert distinto es rechazado — aunque los permissions sean idénticos a un módulo Tridium firmado por Tridium.
4. **Module restrictions**: algunos módulos Tridium que existen en un Workbench pack puro no están en esta distribución (si no fueron empaquetados por Honeywell en el OEM build).
5. **SMA managed centralmente**: la SMA master date viene de la licencia del vendor — Honeywell maneja su propia SMA aparte de Tridium.

### El caso `developer` feature
Un descubrimiento que quedó pendiente de la sesión httpapi: la feature `developer` de la `Webs.license` tiene **`skipModuleValidation="true"`** — esto bypasea el lockdown de módulos. Esta distribución TIENE esa feature activa (visible en la license line 40).

**Implicación práctica**: con la license actual, un módulo custom con `<permissions>` podría cargar si el framework chequea esa feature ANTES de la validation. No confirmé si el check está habilitado en esta build específica — es un TODO de Bloque 3 (grep por `skipModuleValidation` en el código).

---

## 2.8 API runtime del licensing

Entry point: `javax.baja.sys.Sys.getLicenseManager()` → `LicenseManager`.

| Método | Para qué |
|--------|----------|
| `getFeature(vendor, name)` | `Feature` wrapper, throws `FeatureNotLicensedException` si no existe |
| `checkFeature(vendor, name)` | lanza `FeatureNotLicensedException` si no licenciado |
| `getFeatures()` | iterar todas las features del DB |
| `Feature.check()` | valida que existe + no expirado |
| `Feature.get(attr)` / `getb` / `geti` | leer properties del feature |
| `NLicenseManager.getLicenseMaintenanceExpiration(vendor)` | SMA date por vendor |
| `Sys.getBajaVendor()` | vendor del binary base (en esta dist: probablemente "Tridium" dado que nre y station vienen de ahí) |

Debug commands:
- `nre -licenses` (console): lista todas las features cargadas
- Workbench → spy pages: tree de la license DB
- Logs: buscar `license` logger / `.baja.license` package

---

## 2.9 Aggregate empírico de feature checks en el corpus

El module-navigator escaneó **95 `checkFeature()` callsites** en los 926 JARs. Los top (con cantidad de hits / classes / modules):

| Feature | Category | Hits | Classes | Modules |
|---------|----------|------|---------|---------|
| reflow | NiagaraMods | 12 | 2 | 2 |
| fips140-2 | tridium | 6 | 5 | 4 |
| developer | tridium | 5 | 4 | 1 |
| workbenchAzul | tridium | 5 | 1 | 1 |
| brand | Tridium | 4 | 4 | 4 |
| videoDriver | tridium | 4 | 4 | 2 |
| nre | tridium | 3 | 2 | 2 |
| workbench | tridium | 2 | 1 | 1 |

**Observaciones**:
- 37 de 95 calls tienen el feature name unresolvable (string var dinámico) — posiblemente leídas de config o calculadas.
- `developer` con 5 hits indica que el bypass `skipModuleValidation` está checked en varios lugares. Vale la pena trazar esto en Bloque 3.
- `brand` con 4 hits en 4 módulos distintos confirma que el branding tiene hooks en múltiples subsistemas.
- SMA: NO hay hits de `checkFeature("tridium", "sma")` — SMA NO es un feature, es metadata por vendor (confirmado por LicenseUtil.java).

---

## 2.10 Consecuencias prácticas (Bloque 2)

1. **License database es global y mergeado**. No hay concepto de "este módulo tiene SU licencia" — todos comparten el mismo DB loaded en boot.
2. **hostId + vendor + signature son triple check**. Copiar un `.license` a otra máquina no sirve.
3. **Feature attributes son el mecanismo de tuning**. Un feature sin restrictions tiene `*.limit="none"`; un feature con limits strictos tiene valores específicos.
4. **SMA es soft enforcement**, **feature expiration es hard enforcement**. SMA levanta alarm; feature expiration tira `FeatureNotLicensedException`.
5. **La feature `developer` con `skipModuleValidation="true"` existe en esta distribución** — pendiente de confirmar si está efectivamente honrada por el security manager. Si lo está, abre una puerta que la sesión httpapi no exploró.
6. **Tridium firma el framework, OEMs firman sus features, y los certs OEM son signed por Tridium root**. Chain of trust de 2 niveles. Romper la chain = módulo no válido.
7. **No hay ofuscación de la lista de features**. El `.license` es XML en claro. Leer Webs.license te dice exactamente qué funcionalidades tiene la distribución.

---

## Topic keys engram (referencias cruzadas)

- `niagara/licensing/model` — este bloque completo
- `niagara/estructura/framework` (Bloque 1) — cómo un módulo declara dependencias
- `niagara/honeywell-oem-signing-lockdown` (httpapi #302) — el cert lockdown del Bloque 3
- Pendiente Bloque 3: `niagara/security/skip-module-validation` — confirmar si la feature `developer` hace efectivo bypass

## Archivos leídos
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide-clean/licensing.txt`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/Webs.license`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/Honeywell.license`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/HoneywellCentraLine.license`
- `/home/cristian/modules/Prototipos/modulos/organized/httpClient/httpClient-rt/vineflower/com/tridium/httpClient/util/LicenseUtil.java`
- `/home/cristian/modules/Prototipos/modulos/organized/httpClient/httpClient-rt/vineflower/com/tridium/httpClient/util/BSMAExpirationMonitor.java` (partial)
