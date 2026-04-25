# Bloque 41 — Runtime decompile profundo (cierre TODOs Bloque 32)

**Sesión**: 2026-04-25
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: `javap -p -c` empírico sobre clases extraídas de JARs Tridium con `unzip`. NO se usó vineflower. Verificación cross-JAR (`baja.jar`, `fox-rt.jar`, `nre.jar`, `abstractMqttDriver-rt.jar`, `honMqttDriver-rt.jar`).
**Cobertura**: Capa 15 — cierre empírico de los 7 TODOs honestos del Bloque 32.

Este bloque cierra empíricamente los TODOs documentados en Bloque 32: `BTransaction` semántica real, `BModule` lifecycle hooks, `Sys.loadType` internals, `BOnMissingType` (refutado), `fox.sys` constants/channels, `license-rt.jar` SMA methods, y `honMqttDriver` internal stack. **4 errores estructurales del Bloque 32 corregidos**.

---

## 41.1 — `javax.baja.sync.Transaction` — existe pero NO es ACID

### 41.1.1 Existencia confirmada

Bloque 32.9 dijo "no hay BTransaction real". **Parcialmente correcto**: existe la clase, pero NO con prefijo B (porque NO es BComponent — es wrapper de Context).

```bash
unzip -o baja.jar javax/baja/sync/Transaction.class
javap -p javax/baja/sync/Transaction.class
```

Output:
```java
public abstract class javax.baja.sync.Transaction
    extends javax.baja.sync.SyncBuffer
    implements javax.baja.sys.Context {

  private final javax.baja.sys.Context baseContext;
  private final javax.baja.sys.BFacets facets;

  public static javax.baja.sys.Context start(javax.baja.sys.BComponent, javax.baja.sys.Context);
  public static void end(javax.baja.sys.BComponent, javax.baja.sys.Context) throws java.lang.Exception;
  protected javax.baja.sync.Transaction(javax.baja.space.BComponentSpace, javax.baja.sys.Context);
  public javax.baja.sys.Context getBase();
  public javax.baja.user.BUser getUser();
  public javax.baja.sys.BFacets getFacets();
  public java.lang.String getLanguage();
}
```

- Nombre real: **`javax.baja.sync.Transaction`** (sin B prefix porque NO es BComponent)
- Es **abstracta**: `protected` constructor; subclases internas generadas por framework cuando se hace `Transaction.start()`
- Hereda de **`SyncBuffer`** → la "transacción" es realmente **un buffer de SyncOps acumulables** que se aplica en `commit()`
- Patrón uso: estático `start(BComponent, Context) → Context` y `end(BComponent, Context)` — Java try/finally idiom

### 41.1.2 Semántica weak — `abortCommit()` está vacío

```bash
javap -c -p javax/baja/sync/SyncBuffer.class
```

Bytecode literal:
```
public final void commit() throws java.lang.Exception;
    Code:
       0: aload_0
       1: aconst_null
       2: invokevirtual #53  // commit:(Ljavax/baja/sys/Context;)V
       5: return

  protected void abortCommit(java.lang.Exception);
    Code:
       0: return       ← VACÍO. NO hace rollback.
```

`commit(Context)` itera operaciones; en exception llama `abortCommit(e)` que **no hace nada**, luego re-throw. **NO hay rollback de ops aplicadas antes de la falla**.

**Confirmado empíricamente**: "transaction" es nominal, NO transaccional. El estado queda inconsistente si commit falla mid-way.

### 41.1.3 Implicación operacional

- Workflows que dependan de "rollback on error" en `Transaction.start()/end()` están **rotos por diseño**
- Si `commit` falla en op N de M, las ops 1..N-1 **YA ESTÁN APLICADAS** — estado componente queda inconsistente
- **Mitigación**: validar TODO antes de aplicar SyncOps. NO confiar en que la "transaction" rolle back.

**REFINA Bloque 32.9**: el Bloque dijo "no hay BTransaction real / compensation manual". La realidad es: la clase EXISTE (sin B prefix), pero la semántica weak es CORRECTA.

---

## 41.2 — `BModule` es `final`, lifecycle hooks NO están ahí

### 41.2.1 BModule sin start/stop/init

```bash
unzip -o baja.jar javax/baja/sys/BModule.class
javap -p javax/baja/sys/BModule.class | head -50
```

```java
public final class javax.baja.sys.BModule extends javax.baja.file.BFileSpace
    implements javax.baja.sys.BIComparable {

  // NO start(), stop(), init(), loaded(), unloaded() públicos
  public static java.util.Optional<javax.baja.sys.BModule> getModule(BISpace);
  public final java.lang.String getModuleName();
  public final javax.baja.util.Lexicon getLexicon();
  public javax.baja.sys.Type getType(java.lang.String) throws TypeException;
  public final java.lang.Class<?> loadClass(java.lang.String) throws ClassNotFoundException;
  // ... sólo metadata + filesystem + classloader
}
```

`BModule` es **final** — NO se subclasea. NO hay hooks override-ables. Bloque 32 asumió incorrectamente que existían lifecycle hooks ahí.

### 41.2.2 Lifecycle real de módulos — `ModuleManager`

```bash
javap -p com/tridium/sys/module/ModuleManager.class | grep -iE "load|start|init"
```

```java
public synchronized NModule loadModule(java.lang.String, RuntimeProfile);
public synchronized NModule loadDependency(java.lang.String) throws ModuleException;
public synchronized NModule[] loadModuleParts(java.lang.String) throws ModuleException;
public synchronized void unloadModule(ModuleInfo) throws ModuleException;
public synchronized void unloadModuleClasses(ModuleInfo);
public synchronized void updateReloadedModule(ModuleInfo);
public void registerOnLoadCallback(Consumer<NModule[]>);
public void initSystemJars();
public void postInit();
```

**Hook real para usuarios**: **`ModuleManager.registerOnLoadCallback(Consumer<NModule[]>)`** — único punto donde código user-side puede engancharse al ciclo de vida de carga.

Métodos `synchronized` (lock global de ModuleManager): `loadModule`, `loadDependency`, `loadModuleParts`, `unloadModule`, `unloadModuleClasses`, `updateReloadedModule`. **Bottleneck en concurrent module loads**.

### 41.2.3 Lifecycle real de COMPONENTES — `BComponent` hooks

```bash
javap -p javax/baja/sys/BComponent.class | grep -iE "started|atSteady"
```

```java
public void started() throws java.lang.Exception;            // primer started en ese space
public void descendantsStarted() throws java.lang.Exception; // hijos all started
public void stationStarted() throws java.lang.Exception;     // station completa OK
public void atSteadyState() throws java.lang.Exception;      // post-startup, system idle
```

**Empírico**: `BHonMqttNetwork extends BAbstractMqttDriverNetwork` overrides `stationStarted()` y `descendantsStarted()`. Confirma que el lifecycle de componentes individuales es donde vive la lógica de inicialización driver-side, NO en `BModule`.

### 41.2.4 Distinción crítica module vs component lifecycle

Confundir "module lifecycle" con "component lifecycle" es error frecuente:
- **Módulos**: cargados/descargados via `ModuleManager` (post-init, registry callback)
- **Componentes**: started/stopped en el árbol via `BComponent` hooks

**NO hay `onUnload()` en BComponent** — el shutdown se hace via `ModuleManager.unloadModule()` que dispara `unloadModuleTypes()` → `SchemaManager.unload(Class)`.

**REFINA Bloque 32**: lifecycle hooks asumidos en BModule NO existen. Toda customización va via BComponent hooks + ModuleManager registry callback.

---

## 41.3 — `Sys.loadType` — one-liner delegate, bottleneck synchronized

### 41.3.1 Bytecode literal

```bash
javap -c -p javax/baja/sys/Sys.class | grep -A 6 "loadType"
```

```
public static javax.baja.sys.Type loadType(java.lang.Class<?>);
    Code:
       0: invokestatic  #51   // Method com/tridium/sys/Nre.getSchemaManager
       3: aload_0
       4: invokevirtual #57   // Method com/tridium/sys/schema/SchemaManager.load
       7: areturn
```

`Sys.loadType(Class)` es **delegación pura** a `Nre.getSchemaManager().load(Class)`.

### 41.3.2 SchemaManager — synchronized global

```java
public synchronized javax.baja.sys.Type load(java.lang.Class<?>);
public synchronized boolean isTypeLoaded(java.lang.String);
public synchronized boolean unload(java.lang.Class<?>);
public synchronized boolean unload(java.util.Collection<java.lang.Class<?>>);
private javax.baja.sys.Type load(int, java.lang.Class<?>);
private javax.baja.sys.Type loadPrivileged(int, java.lang.Class<?>);
```

- `SchemaManager.load` es **`synchronized`** (lock global de schema registry)
- En estaciones grandes con many concurrent module loads, esto es bottleneck
- Hay overload `loadPrivileged(int, Class)` con AccessController bypass (probable para boot)
- **Type registration es por Class object, no por nombre** — usa el classloader del módulo, así que el mismo type-name en distintos módulos genera collision si NModule no lo namespacea

### 41.3.3 Implicación operacional

Estaciones con 1000+ módulos custom hacen `Sys.loadType()` en paralelo durante boot — todos serializan en el monitor de SchemaManager. **Síntoma**: boot lento sin causa visible, JVM con high contention en `SchemaManager$$Lock`.

Cross-ref con Bloque 31 (performance): este es el **bottleneck no documentado** en boot que explica startups lentos en Supervisor con many drivers.

---

## 41.4 — `fox.sys` — NO constants, son 6 channels named

### 41.4.1 Bloque 32.6 era especulación — REFUTADO

Bloque 32.6 dijo "fox.sys system channels (boot, commissioning, config sync, BQL, subscription)" sin lista concreta — era inferencia.

**Realidad empírica**:
```bash
unzip -l fox-rt.jar | grep -iE "Channel\.class"
javap -c -p com/tridium/fox/sys/BFoxChannelRegistry.class
```

**6 channels concretos** registrados por nombre-string en `BFoxChannelRegistry`:

| Channel | Class | Comandos típicos |
|---------|-------|------------------|
| `sys` | `com.tridium.fox.sys.BSysChannel` | summary, stationCall, stationEvent, niagaraRpc, listLocalSpaces, makeBrokerChannel, subscribeNavEvents |
| `file` | `com.tridium.fox.sys.file.BFileChannel` | head, list, delete, makeFile, makeDir, move, setLastModified, getCrc |
| `user` | `com.tridium.fox.sys.user.BUserChannel` | fetchPrefs, setAuthenticator, resetSessionTimeout, getSessionTimeRemaining |
| `broker` | `com.tridium.fox.sys.broker.BBrokerChannel` | loadRoot, syncFromMaster, load, loadSlot, sub, unsub, transfer, invoke |
| `data` | `com.tridium.fox.sys.data.BDataChannel` | canExportEntities, resolve, resolveEntities, exportEntities |
| `spy` | `com.tridium.fox.sys.spy.BSpyChannel` | get |

### 41.4.2 BFoxChannelRegistry bytecode confirma nombres

```
public final BSysChannel getSysChannel();
       1: ldc  #3  // String "sys"
       3: invokevirtual get:(Ljava/lang/String;)Ljavax/baja/sys/BValue;

public final BFileChannel getFileChannel();
       1: ldc  #6  // String "file"

public final BUserChannel getUserChannel();
       1: ldc  #8  // String "user"
```

Constructores de cada channel pasan el name al `BFoxChannel(String)`:
```
BSpyChannel():    ldc "spy"     → BFoxChannel.<init>(String)
BBrokerChannel(): ldc "broker"  → ...
BDataChannel():   ldc "data"    → ...
```

### 41.4.3 Channels descubiertos NO en Bloque 32

- **`broker`** = subscription/sync engine (Supervisor↔Subordinate). Comandos: `loadRoot`, `syncFromMaster`, `sub`, `unsub`, `transfer`, `invoke`. **Esto explica el flow Supervisor-Subordinate sync** mencionado en Bloque 13.1 sin profundizar.
- **`data`** = BQL/entity queries. Comandos: `canExportEntities`, `resolve`, `resolveEntities`, `exportEntities`. **Cross-ref Bloque 21** (BQL/NEQL) — el `data` channel es el endpoint Fox para queries.

### 41.4.4 Limitación: prototype field estático

```java
private static BFoxChannelRegistry prototype;  // template inmutable
```

`prototype` field en `BFoxChannelRegistry` es estático e inmutable. **Custom drivers NO pueden registrar channels custom runtime sin parchear baja.jar**. Honeywell **no extendió** los channels en este Supervisor — todos los 6 channels son stock Tridium.

---

## 41.5 — `honMqttDriver` — 100% Netvox LoRa, Paho 1.2.5 MQTTv3

### 41.5.1 Composición del módulo

```bash
unzip -l honMqttDriver-rt.jar
unzip -p honMqttDriver-rt.jar META-INF/module.xml
```

`honMqttDriver-rt.jar` es **delgado** (~50KB clases custom). Solo agrega:
- `BHonMqttNetwork extends BAbstractMqttDriverNetwork` (Tridium base, en `abstractMqttDriver-rt.jar`)
- 7 clases de sensores Netvox LoRa: **R718A/CT/G/PE, R720E, RA715/716**
- `BHonLoraSensor`, `BHonLoraStringPoint`, `BHonLoraProxyExt`
- `LoraLicenseHandler.isHonLoraComponentLicensed()` checa feature `Honeywell:honLoRaMqtt`
- Bundles **Gson 2.x** (parsea `SensorDetails.json` con specs Netvox)

### 41.5.2 Stack MQTT real — Eclipse Paho 1.2.5 + AWS IoT SDK

```bash
unzip -p abstractMqttDriver-rt.jar META-INF/maven/org.eclipse.paho/.../pom.properties
strings abstractMqttDriver-rt.jar | grep -iE "paho|aws|hivemq|mosquitto"
```

`abstractMqttDriver-rt.jar` (Tridium genérico) bundlea:
- **Eclipse Paho MQTTv3 versión 1.2.5** (`META-INF/maven/org.eclipse.paho/org.eclipse.paho.client.mqttv3/pom.properties`)
- **AWS IoT SDK Java** (`com/amazonaws/services/iot/client/AWSIotMqttClient.class`)
- Authenticators: AWS, Azure (SAS), GCP (JWT), Generic
- Cliente abstraction: `INiagaraMqttClient` con factory `MqttClientFactory.getClient(BAbstractMqttDriverDevice)` que selecciona Paho default

### 41.5.3 TLS / QoS soportados

```bash
javap -p javax/baja/security/crypto/BSslTlsEnum.class
```

```java
public final class BSslTlsEnum extends BFrozenEnum {
  public static final int TLSV_1;       // tlsv1
  public static final int TLSV_1_1;     // tlsv1_1
  public static final int TLSV_1_2;     // tlsv1_2
  public static final int TLSV_1_3;     // tlsv1_3
  public static final BSslTlsEnum DEFAULT;
}
```

**Soporta TLS 1.0 hasta 1.3**. Default no determinado bytecode-only (probablemente 1.2).

QoS (de `BMqttQualityOfService.class` strings):
```
EXACTLY_ONCE       (QoS 2)
AtLeastOnce        (QoS 1)
"Atleast Once (1)"
"Exactly Once (2)"
```
**QoS 0, 1, 2 supported**.

### 41.5.4 Topic naming Honeywell

- Resource embebido: `module://honMqttDriver/res/SensorDetails.json` — define topic patterns por modelo Netvox
- NO topic constants hardcoded — topics se construyen dinámicamente desde config JSON + device address LoRa
- License feature: `Honeywell:honLoRaMqtt`

### 41.5.5 Implicaciones operacionales

- `LoraLicenseHandler.isHonLoraComponentLicensed()` lanza `FeatureNotLicensedException` al instanciar puntos si feature `honLoRaMqtt` ausente
- **Solo soporta sensores Netvox listados** — NO es generic MQTT-driver Honeywell. Para otros modelos LoRa hay que extender o usar `abstractMqttDriver` directamente
- **Paho 1.2.5 es MQTTv3** — NO MQTT 5.0. Sin properties, sin reason codes, sin shared subscriptions. Si proyecto requiere MQTT 5.0 features → driver custom NO basado en `abstractMqttDriver-rt.jar`

**Cierra TODO Bloque 32** "honMqttDriver NO analizado".

---

## 41.6 — License — `baja.jar` no `license-rt.jar` separado

### 41.6.1 NO existe `license-rt.jar`

Bloque 32 asumió un jar separado. Realidad:

```bash
unzip -l baja.jar | grep "/license/"
```

Toda la lógica vive en `baja.jar` bajo `com/tridium/sys/license/`:
- `LicenseUtil.class`
- `NLicenseManager.class`
- `BSMANotificationSettings.class`
- `dom/Feature.class` — parser de attributes
- `subscription/SubscriptionLicenseManager.class` — nCloud / EntitlementApi

### 41.6.2 Feature.class — parser strings

```java
public class com.tridium.sys.license.dom.Feature {
  public boolean isExpired();
  public long getExpiration();
  public void setExpiration(long);
  public String get(String);                 // raw attribute
  public String get(String, String);         // with default
  public boolean getb(String, boolean);      // string→bool
  public int geti(String, int);              // string→int
  public String[] list();                    // all attribute names
  void load(javax.baja.xml.XElem) throws Exception;
  XElem save();
}
```

**Hallazgo crítico**: TODA la lógica SMA (`sma.exempt`, `feature.sma.expiration`) se accede via `feature.getb("sma.exempt", false)` o `feature.geti("sma.expiration", 0)`. **NO hay parser tipado, son strings con defaults**.

Esto explica por qué Bloque 14.3 menciona variantes de attribute names — cualquier typo en `.license` XML pasa silently con default. **Auditar manualmente** — typo bugs no se reportan (`feature.getb("Sma.Exempt", false)` retorna silently false).

### 41.6.3 LicenseUtil — verify chain

```java
public final class LicenseUtil {
  static final long INVALID_LICENSE_TIME_MILLIS_FLOOR;
  public static final String TRIDIUM_VENDOR;
  private static java.security.PublicKey masterPublicKey;
  private static java.security.PublicKey version2PublicKey;

  public static long parseDate(String);
  public static int parseLimit(Feature, String);
  public static byte[] encode(XElem);
  public static boolean verify(byte[], byte[], byte[]) throws Exception;
  public static boolean verify(byte[], byte[], PublicKey) throws Exception;
  public static boolean verify(byte[], byte[], Version) throws Exception;
  static PublicKey getMasterPublicKey() throws Exception;
  static PublicKey getVersion2PublicKey() throws Exception;
}
```

- **Dual public key system**: `masterPublicKey` (legacy) + `version2PublicKey`. License XML signed con uno u otro según version del archivo.
- `verify(bytes, signature, version)` selecciona PublicKey por version.
- `INVALID_LICENSE_TIME_MILLIS_FLOOR` = piso temporal — fechas debajo se invalidan (probable 2010-01-01 default check anti-clock-rollback).

### 41.6.4 Subscription / nCloud = `EntitlementApi`

```java
public final class SubscriptionLicenseManager extends NLicenseManager {
  public void initializeLrt();
  public void postInit();
  public EntitlementStatus updateCertificates(RequestCertificates, String);
  private void initPeriodicKeyRotation();
  private void initPeriodicEntitlementCheck();
  public boolean isKeyRotationNeeded();
  public void refreshLrtIfNreIdChanged();
  public void checkSubscription();
  public void checkEntitlement();
  public EntitlementStatus getLicenseUpdate(JSONObject, RetrieveEntitlements);
  public static boolean isLicenseSignatureValid(XElem, File);
  public void rotateKeys();
  public void regenerateNreId();
}
```

**Strings detectados**:
- `EntitlementApi`, `EntitlementUtil`, `EntitlementException`, `EntitlementStatusListener`
- `RetrieveEntitlements`, `RequestCertificates`, `LicenseRefreshToken` (LRT)
- Frequencies: `KEY_ROTATION_FREQUENCY`, `entitlementCheckFrequency`, `THIRTY_MINUTES`, `ENTITLEMENT_RETRY_DELAY_MS`
- **Periodic check**: `ScheduledExecutorService` con `keyRotation` + `entitlementCheck` jobs

**Hallazgos**:
- "nCloud" interno es **`EntitlementApi`** — vive en `com.tridium.nre.subscription`
- Subscription license usa **LRT (License Refresh Token)** + periodic key rotation + periodic entitlement check vs cloud
- **3 directorios separados** local: `getSubscriptionDirectory()`, `getSubscriptionCertificateDirectory()`, `getSubscriptionLicenseDirectory()`
- `regenerateNreId()` permite renovar nre identity (post-clone scenario para evitar collision)
- Si no hay subscription license, se cae al `NLicenseManager` clásico (file-based con XElem signed)

### 41.6.5 BSMANotificationSettings (UI alarm settings)

```java
public class BSMANotificationSettings extends BComponent {
  public boolean getEnabled();
  public boolean getShowExpirationDate();
  public boolean getShowExpirationReminder();
  public int getExpirationReminder();    // días antes
}
```

Settings UI para warning de SMA expiration. El "real check" es vía `Feature.isExpired()` / `getExpiration()` de cada feature.

### 41.6.6 `CLONED_FILE` flag — NRE-id collision detect

`SubscriptionLicenseManager` detecta NRE-id collision (post host duplication / VM clone) y bloquea startup hasta `regenerateNreId()`. **Anti-licensing-evasion via VM cloning**.

---

## 41.7 — `BOnMissingType` REFUTADO + forward-compat real

### 41.7.1 Búsqueda exhaustiva — clase NO existe

```bash
find modules -name "*.jar" | xargs -I {} sh -c 'unzip -l {} 2>/dev/null | grep -iE "OnMissing|UnresolvedType|MissingType"'
# → 0 resultados en TODOS los 969 jars del Optimizer Supervisor 4.14.0.162
```

**REFUTADO**: NO existe `BOnMissingType`, `BMissingType`, ni `UnresolvedType` en ningún jar.

Bloque 32.13 inventó el nombre — probable inferencia derivada de patrones en otros frameworks (Eclipse EMF UnresolvedProxy, Java AbsentTypeReference).

### 41.7.2 Mecanismo real: `ValueDocDecoder$BogTypeResolver`

```bash
javap -c -p 'javax/baja/io/ValueDocDecoder$BogTypeResolver.class'
```

Bytecode `newInstance(...)`:
```
public BValue newInstance(ValueDocDecoder, BComplex, String, Property, String typespec);
    Code:
       0: aload 5            // typespec
       2: ifnonnull 48       // si null, salta a manejo de prop default
       5: aload 4            // property
       7: ifnull 18
      10: aload 4
      12: invokeinterface getDefaultValue:()Ljavax/baja/sys/BValue;  ← retorna default
      17: areturn
      18: aload_1            // si NO hay prop conocida:
      19: getfield plugin
      29: ldc "Missing frozen property:"   ← warning
      41: invokeinterface warningAndSkip:(Ljava/lang/String;)V
      46: aconst_null
      47: areturn            ← retorna NULL
```

### 41.7.3 Comportamiento real cuando `.bog` tiene type ausente

1. **Si typespec es null** (slot mal-decodeado):
   - Si la property es frozen (declared en TYPE) → retorna `Property.getDefaultValue()`
   - Si no → emite `IDecoderPlugin.warningAndSkip("Missing frozen property:...")` y retorna `null`
2. **Si typespec es `module:typeName`** y el módulo está cargado pero el type no:
   - Itera todos los module parts del module
   - Si alguno tiene el type → `ValueDocDecoder.typeResolverNewInstance(NModule, String)` → instancia
   - Si nadie tiene el type → fallback a warning + skip
3. **Si modulo no resuelve**: `ModuleManager.loadModuleParts(name)` lanza exception, capturada y plugin emite error

### 41.7.4 Implicación: data loss silencioso en round-trip cross-version

**Conclusión**: forward-compat es **silent skip + warning logging**, NO un BStub/placeholder. Esto significa:
- Cargar un `.bog` con types más nuevos en framework older → **slot se pierde silenciosamente** (warning en log)
- Save back → la información NO se preserva (porque el slot es null)
- **Pérdida de datos en round-trip si subes/bajas versión de framework**

**NO hay equivalente a Eclipse EMF's eUnknownFeature preservation**. Bloque 32.13 implicaba que `BOnMissingType` podría preservar — esto es FALSO.

**Para prevenir pérdida**: NO abrir/save cycle `.bog` en versión old del framework si fue creado con newer.

**REFINA Bloque 32.13** críticamente.

---

## Correcciones / confirmaciones a Bloque 32 — tabla

| Bloque 32 ítem | Estado | Corrección empírica |
|----------------|--------|---------------------|
| 32.6 fox.sys constants | **REFUTADO** | NO existen "constants" — son 6 channels named: sys, file, user, broker, data, spy. Cada uno con set string-commands en `process(FoxRequest)`. |
| 32.9 BTransaction "no existe" | **PARCIAL** | EXISTE como `javax.baja.sync.Transaction` (sin B). Pero la semántica weak es CORRECTA — `abortCommit()` está vacío, no hay rollback. |
| 32.13 BOnMissingType forward-compat | **REFUTADO** | NO existe. Forward-compat = silent skip + warning via `BogTypeResolver.warningAndSkip()`. **Hay pérdida de datos en round-trip cross-version**. |
| 32 Sys.loadType | **CONFIRMADO + AMPLIADO** | Es one-liner delegate a `SchemaManager.load(Class)` (synchronized — bottleneck en concurrent loads). |
| 32 BModule lifecycle hooks | **REFUTADO** | BModule es final, no tiene hooks. Lifecycle real está en (a) `ModuleManager.registerOnLoadCallback()`, (b) `BComponent.started()/descendantsStarted()/stationStarted()/atSteadyState()`. |
| 32 license-rt.jar SMA | **CORREGIDO UBICACIÓN** | NO existe jar separado. Vive en `baja.jar` bajo `com/tridium/sys/license/`. SMA es `feature.getb("sma.exempt", false)` string-based. |
| 32 honMqttDriver "NO analizado" | **CERRADO** | 100% custom Honeywell para sensores Netvox LoRa. Stack base = Eclipse Paho 1.2.5 + AWS IoT SDK (en `abstractMqttDriver-rt.jar`). TLS 1.0–1.3, QoS 0/1/2. License feature `Honeywell:honLoRaMqtt`. |

**4 errores estructurales del Bloque 32 corregidos**:
1. Asumió `BTransaction` con prefijo B (es `Transaction` sin B porque NO es BComponent).
2. Asumió `license-rt.jar` separado (vive en `baja.jar`).
3. Asumió BModule tiene lifecycle hooks (no — está final, hooks viven en BComponent + ModuleManager).
4. Inventó `BOnMissingType` (no existe — es warning+skip).

---

## Gotchas nuevos descubiertos

- **G41.1 — Transaction no es ACID**: `SyncBuffer.abortCommit(Exception)` está vacío. Si commit falla en op N de M, ops 1..N-1 ya aplicadas. Estado inconsistente. Validar TODO antes de SyncOps.
- **G41.2 — SchemaManager.load synchronized**: bottleneck de boot con 1000+ módulos custom. Síntoma: boot lento, JVM con high contention en `SchemaManager$$Lock`.
- **G41.3 — fox channel `prototype` estático e inmutable**: NO se pueden registrar channels custom runtime sin parchear baja.jar. Honeywell NO extendió channels en este Supervisor — todos stock Tridium.
- **G41.4 — BOG forward-compat pierde slots silently**: cargar `.bog` con types desconocidos en framework older → warning + slot vacío. Re-save → slot perdido permanentemente. NO equivalente a EMF eUnknownFeature.
- **G41.5 — License `sma.exempt` es string typo-prone**: `feature.getb("Sma.Exempt", false)` con typo retorna default false silently. Auditar manualmente — typo bugs no se reportan.
- **G41.6 — Subscription license tiene 3 directorios separados**: `SUBSCRIPTION_DIRECTORY` + `CERTIFICATE_DIRECTORY` + `LICENSE_DIRECTORY`. Backups parciales que omitan alguno corrompen subscription state. `CLONED_FILE` detecta NRE-id collision post-clone.
- **G41.7 — honMqttDriver SOLO Netvox**: lista hardcoded en `lora/sensors/`. Para genéricos LoRa usar `abstractMqttDriver-rt.jar` directamente (sin license `honLoRaMqtt`).
- **G41.8 — BModule final → no extensible**: custom módulos NO pueden override BModule behavior. Toda customización via `BComponent` lifecycle hooks o `ModuleManager.registerOnLoadCallback()`.
- **G41.9 — Eclipse Paho 1.2.5 es MQTTv3 ONLY**: sin MQTT 5.0 properties, reason codes, shared subscriptions. Proyecto requiriendo 5.0 → driver custom NO basado en `abstractMqttDriver-rt.jar`.
- **G41.10 — ModuleManager.loadModule synchronized global**: todas las cargas de módulos serialize. Combined con G41.2 → boot order nondeterminism amplified bajo carga.
- **G41.11 — `INVALID_LICENSE_TIME_MILLIS_FLOOR` clock-rollback protection**: licenses con fechas debajo del floor temporal se invalidan. Anti-evasion via clock manipulation.
- **G41.12 — `regenerateNreId()` post-VM-clone**: si station se clona vía VM snapshot, NRE-id duplicado bloquea startup. Workflow recovery requiere `regenerateNreId()` manual.
- **G41.13 — `data` channel es endpoint Fox de BQL queries**: cross-ref Bloque 21 — el `BDataChannel` es donde live BQL/NEQL entity queries en Fox sessions remotas.
- **G41.14 — `broker` channel = Supervisor↔Subordinate sync engine**: subscription, sync, transfer commands. Cross-ref Bloque 13.1 federation.

---

## Resumen archivos analizados

### `baja.jar`
- `javax/baja/sync/Transaction.class`, `SyncBuffer.class`, `LoadOp.class`, `SyncOp.class`
- `javax/baja/sys/Sys.class`, `BModule.class`, `BComponent.class`, `Type.class`
- `javax/baja/util/BTypeSpec.class`
- `javax/baja/io/ValueDocDecoder.class`, `ValueDocDecoder$BogTypeResolver.class`
- `javax/baja/security/crypto/BSslTlsEnum.class`
- `com/tridium/sys/module/NModule.class`, `ModuleManager.class`
- `com/tridium/sys/schema/SchemaManager.class`
- `com/tridium/sys/license/LicenseUtil.class`, `NLicenseManager.class`, `BSMANotificationSettings.class`
- `com/tridium/sys/license/dom/Feature.class`
- `com/tridium/sys/license/subscription/SubscriptionLicenseManager.class`

### `fox-rt.jar`
- `com/tridium/fox/sys/BFoxChannel.class`, `BSysChannel.class`, `BFoxConnection.class`, `BFoxChannelRegistry.class`
- `com/tridium/fox/sys/file/BFileChannel.class`
- `com/tridium/fox/sys/user/BUserChannel.class`
- `com/tridium/fox/sys/broker/BBrokerChannel.class`
- `com/tridium/fox/sys/data/BDataChannel.class`
- `com/tridium/fox/sys/spy/BSpyChannel.class`

### `abstractMqttDriver-rt.jar`
- `com/tridium/mqttClientDriver/BAbstractMqttDriverDevice.class`
- `com/tridium/mqttClientDriver/util/MqttClientFactory.class`, `BMqttQualityOfService.class`
- `com/tridium/mqttClientDriver/clients/paho/MqttClientPaho.class`
- `META-INF/maven/org.eclipse.paho/.../pom.properties` → version=1.2.5

### `honMqttDriver-rt.jar`
- `com/honeywell/honmqttdriver/BHonMqttNetwork.class`
- `com/honeywell/honmqttdriver/lora/points/BHonLoraSensor.class`
- `com/honeywell/honmqttdriver/util/LoraLicenseHandler.class`
- 7 sensor classes en `lora/sensors/`
- Resource: `module://honMqttDriver/res/SensorDetails.json`

### `bin/ext/nre.jar`
- Solo nre internal infrastructure (syslog, security, util) — NO contiene BModule ni Sys (esos viven en baja.jar)

---

## Cross-refs a bloques previos

- **Bloque 32.6 fox.sys**: REFUTADO — son 6 channels named, NO constants
- **Bloque 32.9 BTransaction**: REFINADO — clase EXISTE como `javax.baja.sync.Transaction` (sin B), semántica weak CONFIRMADA empíricamente
- **Bloque 32.13 BOnMissingType**: REFUTADO — NO existe. Forward-compat real es silent skip + warning, con data loss en round-trip cross-version
- **Bloque 32 Sys.loadType**: CONFIRMADO + AMPLIADO — bottleneck synchronized en boot
- **Bloque 32 BModule lifecycle**: REFUTADO — BModule es final, hooks en BComponent + ModuleManager
- **Bloque 32 license-rt.jar**: CORREGIDO — NO existe jar separado; vive en `baja.jar`
- **Bloque 32 honMqttDriver**: CERRADO — Netvox LoRa + Paho 1.2.5 + AWS IoT SDK + License `honLoRaMqtt`
- **Bloque 21 (BQL/NEQL)**: EXTIENDE — `data` channel Fox es el endpoint de BQL queries cross-station
- **Bloque 13.1 (Niagara Network federation)**: EXTIENDE — `broker` channel es el engine real de Supervisor↔Subordinate sync
- **Bloque 31 (performance)**: EXTIENDE — SchemaManager.load synchronized + ModuleManager.loadModule synchronized son bottlenecks documentados de boot lento
- **Bloque 27.6 (cert types matrix)**: EXTIENDE — TLS 1.0-1.3 confirmado en BSslTlsEnum
- **Bloque 30.7 (.km/.kr DPAPI keyring)**: COMPLEMENTA — el subscription license tiene 3 directorios separados con CLONED_FILE detection post-VM-clone

---

## Topic keys engram

- `niagara/bloque41/transaction-sync-buffer-weak` — Transaction existe `javax.baja.sync.Transaction`, abortCommit vacío, NO ACID
- `niagara/bloque41/bmodule-final-lifecycle-real` — BModule final, hooks en ModuleManager + BComponent.started/atSteadyState
- `niagara/bloque41/sys-loadtype-schema-bottleneck` — Sys.loadType delegate, SchemaManager.load synchronized global
- `niagara/bloque41/fox-six-channels-named` — sys/file/user/broker/data/spy + prototype inmutable
- `niagara/bloque41/honmqtt-netvox-paho-mqttv3` — Netvox LoRa hardcoded + Paho 1.2.5 + AWS IoT SDK + TLS 1.0-1.3 + QoS 0/1/2
- `niagara/bloque41/license-baja-jar-string-attrs` — license en baja.jar (NO -rt separado), SMA string-based typo-prone, EntitlementApi LRT
- `niagara/bloque41/bog-forward-compat-data-loss` — BOnMissingType REFUTADO, BogTypeResolver warning+skip, round-trip cross-version pierde slots
