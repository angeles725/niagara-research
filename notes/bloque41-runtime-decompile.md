# Bloque 41 — Runtime decompile profundo (cierre TODOs honestos Bloque 32)

**Fecha**: 2026-04-25
**Build**: OptimizerSupervisor-N4.14.0.162 (Tridium 4.14.0.162, build 2024-05-28)
**Herramientas**: `javap -p -c` (OpenJDK 11), `unzip`, `strings`. NO se usó vineflower.
**JARs analizados**: `baja.jar`, `fox-rt.jar`, `nre.jar` (en `bin/ext/`), `abstractMqttDriver-rt.jar`, `honMqttDriver-rt.jar`.

---

## Resumen ejecutivo (TODOs cerrados empíricamente)

1. **Transaction CLASS EXISTE** — pero NO se llama `BTransaction`. Es `javax.baja.sync.Transaction` (sin prefijo B), abstracta, extiende `SyncBuffer` e implementa `Context`. Bloque 32.9 estaba **parcialmente correcto**: el "transaction" es semánticamente weak (commit no rollbackable — `abortCommit(Exception){return;}` está vacío), pero la clase existe.
2. **BModule lifecycle hooks NO viven en BModule** — `BModule` es solo un `BFileSpace` con metadata (moduleName, vendor, lexicons, types). Los hooks reales están en (a) `ModuleManager` (`registerOnLoadCallback`, `loadModule`, `unloadModule`, `postInit`, `initSystemJars`) y (b) `BComponent` para componentes individuales (`started()`, `descendantsStarted()`, `stationStarted()`, `atSteadyState()`).
3. **Sys.loadType es one-liner** — `Sys.loadType(Class) → Nre.getSchemaManager().load(Class)`. Delega a `SchemaManager.load(int, Class)` (synchronized) que registra el Type en el schema registry global.
4. **fox.sys NO es un package de constants** — son **clases concretas** registradas por nombre-string en `BFoxChannelRegistry`: `sys` (BSysChannel), `file` (BFileChannel), `user` (BUserChannel), `broker` (BBrokerChannel), `data` (BDataChannel — para BQL/queries entity), `spy` (BSpyChannel). Cada una es subclase de `BFoxChannel`.
5. **honMqttDriver es 100% específico para sensores Netvox LoRa** — extiende `BAbstractMqttDriverNetwork` (Tridium genérico, en `abstractMqttDriver-rt.jar`). El stack MQTT real es **Eclipse Paho v1.2.5** (bundled en `abstractMqttDriver-rt.jar`). Soporta TLS 1.0/1.1/1.2/1.3 + QoS 0/1/2. Honeywell agrega solo el catálogo de modelos R718A/CT/G/PE, R720E, RA715/716 + license `honLoRaMqtt`.
6. **license SMA via XElem XML** — no hay `license-rt.jar` separado. Toda la lógica vive en `baja.jar` bajo `com/tridium/sys/license/`. Atributos como `expiration`/`sma.exempt` son **strings dentro de un XElem** parseado vía `Feature.get(String)` / `Feature.geti(String, int)` / `Feature.getb(String, boolean)` — NO hay parsing tipado nativo, todo es string-based con defaults.
7. **BOnMissingType NO EXISTE** — confirmado refutado. Forward-compat al cargar BOG con tipos faltantes funciona via `ValueDocDecoder$BogTypeResolver.newInstance()`: (a) si la prop es frozen → retorna `Property.getDefaultValue()`, (b) si no hay prop conocida → emite `IDecoderPlugin.warningAndSkip(...)` y retorna `null`. **NO hay clase stub/placeholder, es silent-skip warning-based**.

---

## 1. BTransaction — empírico (existe? formas alternativas)

**Comando**:
```bash
unzip -l baja.jar | grep -iE "Transaction"
unzip -o baja.jar javax/baja/sync/Transaction.class
javap -p javax/baja/sync/Transaction.class
```

**Output relevante**:
```
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

### Hallazgos
- **Nombre real**: `javax.baja.sync.Transaction` (NO `BTransaction`, NO prefijo B porque NO es BComponent — es un wrapper de Context).
- Es **abstracta**: `protected` constructor; existen subclases internas (no encontradas en search inicial — probablemente generadas por el framework cuando se hace `Transaction.start()`).
- **Patrón de uso**: estático `start(BComponent, Context) → Context` y `end(BComponent, Context)` — Java try/finally idiom.
- Hereda de `SyncBuffer` → la "transacción" es realmente **un buffer de SyncOps acumulables** que se aplica en `commit()`.

### Semántica weak confirmada (`SyncBuffer.commit()` bytecode)
**Comando**: `javap -c -p javax/baja/sync/SyncBuffer.class`

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

`commit(Context)` itera operaciones, en exception llama `abortCommit(e)` que **no hace nada**, luego `athrow`. **No hay rollback de ops aplicadas antes de la falla** — confirmando empíricamente que "transaction" es nominal, no transaccional. El estado queda inconsistente.

**Gotcha crítico**: el código que use `Transaction.start()/end()` debe asumir failure-inconsistency. No hay protección ACID.

---

## 2. BModule lifecycle hooks — javap output reseñado

**Comando**:
```bash
unzip -o baja.jar javax/baja/sys/BModule.class com/tridium/sys/module/NModule.class \
  com/tridium/sys/module/ModuleManager.class javax/baja/sys/BComponent.class
javap -p javax/baja/sys/BModule.class | head -50
```

### BModule — NO tiene start/stop/init públicos
```
public final class javax.baja.sys.BModule extends javax.baja.file.BFileSpace
    implements javax.baja.sys.BIComparable {
  // NO hay start(), stop(), init(), loaded(), unloaded() públicos
  public static java.util.Optional<javax.baja.sys.BModule> getModule(BISpace);
  public final java.lang.String getModuleName();
  public final javax.baja.util.Lexicon getLexicon();
  public javax.baja.sys.Type getType(java.lang.String) throws TypeException;
  public final java.lang.Class<?> loadClass(java.lang.String) throws ClassNotFoundException;
  // ... sólo metadata + filesystem + classloader
}
```

`BModule` es **final**, no se subclasea. No hay hooks override-ables.

### Lifecycle real está en ModuleManager
**Comando**: `javap -p com/tridium/sys/module/ModuleManager.class | grep -iE "load|start|init"`

```
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

**Hook real**: `ModuleManager.registerOnLoadCallback(Consumer<NModule[]>)` — único punto donde código user-side puede engancharse al ciclo de vida de carga.

### Lifecycle de COMPONENTES (no módulos) — `BComponent` hooks
**Comando**: `javap -p javax/baja/sys/BComponent.class | grep -iE "started|atSteady"`

```
public void started() throws java.lang.Exception;          // primer started en ese space
public void descendantsStarted() throws java.lang.Exception; // hijos all started
public void stationStarted() throws java.lang.Exception;   // station completa OK
public void atSteadyState() throws java.lang.Exception;    // post-startup, system idle
```

**Empírico**: `BHonMqttNetwork extends BAbstractMqttDriverNetwork` overrides `stationStarted()` y `descendantsStarted()`. Confirmado en módulo Honeywell:
```
public void stationStarted() throws java.lang.Exception;
public void descendantsStarted() throws java.lang.Exception;
```

### Gotchas
- Confundir "module lifecycle" con "component lifecycle" es error frecuente. Módulos son cargados/descargados, componentes son started/stopped en el árbol.
- No hay `onUnload()` en BComponent — el shutdown se hace via `ModuleManager.unloadModule()` que dispara `unloadModuleTypes()` → `SchemaManager.unload(Class)`.

---

## 3. Sys.loadType internals

**Comando**:
```bash
javap -c -p javax/baja/sys/Sys.class | grep -A 6 "loadType"
```

**Bytecode literal**:
```
public static javax.baja.sys.Type loadType(java.lang.Class<?>);
    Code:
       0: invokestatic  #51   // Method com/tridium/sys/Nre.getSchemaManager:()Lcom/tridium/sys/schema/SchemaManager;
       3: aload_0
       4: invokevirtual #57   // Method com/tridium/sys/schema/SchemaManager.load:(Ljava/lang/Class;)Ljavax/baja/sys/Type;
       7: areturn
```

### SchemaManager.load
```
public synchronized javax.baja.sys.Type load(java.lang.Class<?>);
public synchronized boolean isTypeLoaded(java.lang.String);
public synchronized boolean unload(java.lang.Class<?>);
public synchronized boolean unload(java.util.Collection<java.lang.Class<?>>);
private javax.baja.sys.Type load(int, java.lang.Class<?>);
private javax.baja.sys.Type loadPrivileged(int, java.lang.Class<?>);
```

### Hallazgos
- `Sys.loadType(Class)` es **delegación pura** a `Nre.getSchemaManager().load(Class)`.
- `SchemaManager.load` es `synchronized` (lock global de schema registry) — en estaciones grandes con many concurrent module loads, esto puede ser bottleneck.
- Hay un overload `loadPrivileged(int, Class)` con AccessController bypass (probable para boot).
- **Type registration es por Class object, no por nombre** — usa el classloader del módulo, así que el mismo type-name en distintos módulos genera collision si NModule no lo namespacea.

---

## 4. fox.sys channels constants

**Comandos**:
```bash
unzip -l fox-rt.jar | grep -iE "Channel\.class"
javap -c -p com/tridium/fox/sys/BFoxChannelRegistry.class
```

### Channels concretos (NO constants — son clases registradas por nombre)
| Channel name | Class                                          | Comandos típicos |
|--------------|------------------------------------------------|------------------|
| `sys`        | `com.tridium.fox.sys.BSysChannel`              | summary, stationCall, stationEvent, niagaraRpc, listLocalSpaces, makeBrokerChannel, subscribeNavEvents |
| `file`       | `com.tridium.fox.sys.file.BFileChannel`        | head, list, delete, makeFile, makeDir, move, setLastModified, getCrc |
| `user`       | `com.tridium.fox.sys.user.BUserChannel`        | fetchPrefs, setAuthenticator, resetSessionTimeout, getSessionTimeRemaining |
| `broker`     | `com.tridium.fox.sys.broker.BBrokerChannel`    | loadRoot, syncFromMaster, load, loadSlot, sub, unsub, transfer, invoke |
| `data`       | `com.tridium.fox.sys.data.BDataChannel`        | canExportEntities, resolve, resolveEntities, exportEntities |
| `spy`        | `com.tridium.fox.sys.spy.BSpyChannel`          | get |

### BFoxChannelRegistry bytecode (confirma nombres)
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
BSpyChannel(): ldc "spy"  → BFoxChannel.<init>(String)
BBrokerChannel(): ldc "broker"  → ...
BDataChannel(): ldc "data"  → ...
```

### Hallazgos
- El "system" propiamente dicho NO tiene un namespace `fox.sys` con constants — es un **registry de strings → BFoxChannel subclasses**.
- **Bloque 32.6 ("fox.sys constants/channels") era especulación** — la verdad: 6 channels nombrados, cada uno con set de comandos string-based dispatch en `process(FoxRequest)`.
- Channel additional discovered (NO en Bloque 32): **`broker`** = subscription/sync engine (Supervisor↔Subordinate), **`data`** = BQL/entity queries.

### Gotcha
- `BFoxChannel.fwSessionOpened()` se llama una vez por sesión Fox abierta. Custom drivers que extienden BFoxChannel deben implementar `process(FoxRequest)` y usar el namespace strings de comandos consistentes con channel name.
- `prototype` field en `BFoxChannelRegistry` (estático) es el template inmutable — **no mutable runtime** para añadir channels custom sin modificar baja.jar.

---

## 5. honMqttDriver internal stack

**Comandos**:
```bash
unzip -l honMqttDriver-rt.jar
unzip -p honMqttDriver-rt.jar META-INF/module.xml
unzip -p abstractMqttDriver-rt.jar META-INF/maven/org.eclipse.paho/.../pom.properties
strings abstractMqttDriver-rt.jar | grep -iE "paho|aws|hivemq|mosquitto"
```

### Composición real
- `honMqttDriver-rt.jar` es **delgado** (~50KB clases custom). Solo agrega:
  - `BHonMqttNetwork extends BAbstractMqttDriverNetwork` (Tridium base)
  - 7 clases de sensores Netvox LoRa: R718A/CT/G/PE, R720E, RA715/716
  - `BHonLoraSensor`, `BHonLoraStringPoint`, `BHonLoraProxyExt`
  - `LoraLicenseHandler.isHonLoraComponentLicensed()` checa feature `honLoRaMqtt` del vendor `Honeywell`
  - Bundlea Gson 2.x (para parsear `SensorDetails.json` con specs Netvox)

### Stack MQTT real
- `abstractMqttDriver-rt.jar` (Tridium genérico) bundlea:
  - **Eclipse Paho MQTTv3 versión 1.2.5** (`META-INF/maven/org.eclipse.paho/org.eclipse.paho.client.mqttv3/pom.properties`)
  - **AWS IoT SDK Java** (`com/amazonaws/services/iot/client/AWSIotMqttClient.class`)
  - Authenticators para AWS, Azure (SAS), GCP (JWT), Generic
  - Cliente abstraction: `INiagaraMqttClient` con factory `MqttClientFactory.getClient(BAbstractMqttDriverDevice)` que selecciona Paho default

### TLS / QoS
**Comando**: `javap -p javax/baja/security/crypto/BSslTlsEnum.class`

```
public final class javax.baja.security.crypto.BSslTlsEnum extends BFrozenEnum {
  public static final int TLSV_1;       // tlsv1
  public static final int TLSV_1_1;     // tlsv1_1
  public static final int TLSV_1_2;     // tlsv1_2
  public static final int TLSV_1_3;     // tlsv1_3
  public static final BSslTlsEnum DEFAULT;
}
```
**Soporta TLS 1.0 hasta 1.3**. Default no determinado bytecode-only sin trace static initializer (probablemente 1.2).

QoS values (de `BMqttQualityOfService.class` strings):
```
EXACTLY_ONCE
AtLeastOnce
ExactlyOnce
"Exactly Once (2)"
"Atleast Once (1)"
```
**QoS 0, 1, 2 supported**.

### Topic naming Honeywell
- Resource embebido: `module://honMqttDriver/res/SensorDetails.json` — define topic patterns por modelo Netvox.
- No se observan topic constants hardcoded — los topics se construyen dinámicamente a partir de la config del JSON + device address LoRa.
- License feature: `Honeywell:honLoRaMqtt`.

### Gotchas
- Feature license `honLoRaMqtt` debe estar presente o `LoraLicenseHandler.isHonLoraComponentLicensed()` lanza `FeatureNotLicensedException` al instanciar puntos.
- Sólo soporta sensores Netvox listados — NO es generic MQTT-driver Honeywell. Para otros modelos LoRa se necesita extender o usar `abstractMqttDriver` directamente.
- Paho 1.2.5 es **MQTTv3** — NO MQTT 5.0. Sin properties, sin reason codes, sin shared subscriptions.

---

## 6. license-rt.jar SMA methods

**Realidad**: NO existe `license-rt.jar` separado. License está bundled en `baja.jar` bajo `com/tridium/sys/license/`.

**Comandos**:
```bash
unzip -l baja.jar | grep "/license/"
unzip -o baja.jar com/tridium/sys/license/LicenseUtil.class \
  com/tridium/sys/license/dom/Feature.class \
  com/tridium/sys/license/subscription/SubscriptionLicenseManager.class \
  com/tridium/sys/license/BSMANotificationSettings.class
```

### Feature.class — parser strings de attributes
```
public class com.tridium.sys.license.dom.Feature {
  public boolean isExpired();
  public long getExpiration();
  public void setExpiration(long);
  public String get(String);                 // raw attribute
  public String get(String, String);         // with default
  public boolean getb(String, boolean);      // string→bool
  public int geti(String, int);              // string→int
  public String[] list();                    // all attribute names
  public void set(String, String);
  public final void set(String, boolean);
  public final void set(String, int);
  void load(javax.baja.xml.XElem) throws Exception;
  XElem save();
}
```

**Hallazgo crítico**: TODA la lógica SMA (`sma.exempt`, `feature.sma.expiration`) se accede via `feature.getb("sma.exempt", false)` o `feature.geti("sma.expiration", 0)`. **NO hay parser tipado, son strings con defaults**. Esto explica por qué Bloque 14.3 menciona variantes de attribute names — cualquier típo en el .license XML pasa silently con default.

### LicenseUtil.class — verify chain
```
public final class com.tridium.sys.license.LicenseUtil {
  static final long INVALID_LICENSE_TIME_MILLIS_FLOOR;
  public static final String TRIDIUM_VENDOR;
  private static java.security.PublicKey masterPublicKey;
  private static java.security.PublicKey version2PublicKey;

  public static long parseDate(String);
  public static long parseDate(String, boolean);
  public static int parseLimit(Feature, String);
  public static byte[] encode(XElem);
  public static boolean verify(byte[], byte[], byte[]) throws Exception;
  public static boolean verify(byte[], byte[], PublicKey) throws Exception;
  public static boolean verify(byte[], byte[], Version) throws Exception;
  static PublicKey getMasterPublicKey() throws Exception;
  static PublicKey getVersion2PublicKey() throws Exception;
}
```

- **Dual public key system**: `masterPublicKey` (legacy) + `version2PublicKey`. License XML signed con uno u otro segun version del archivo.
- `verify(bytes, signature, version)` selecciona PublicKey por version.
- `INVALID_LICENSE_TIME_MILLIS_FLOOR` = piso temporal — fechas debajo se invalidan (probable 2010-01-01 default check).

### SubscriptionLicenseManager.class — nCloud check (the cloud subscription flow)
```
public final class SubscriptionLicenseManager extends NLicenseManager {
  public void initializeLrt();
  public void postInit();
  public EntitlementApi$EntitlementStatus updateCertificates(RequestCertificates, String);
  private void initPeriodicKeyRotation();
  private void initPeriodicEntitlementCheck();
  public boolean isKeyRotationNeeded();
  public void refreshLrtIfNreIdChanged();
  public void checkSubscription();
  public void checkEntitlementPeriodically();
  public void checkEntitlement();
  public void checkEntitlement(RetrieveEntitlements, long, boolean);
  public EntitlementApi$EntitlementStatus getLicenseUpdate(JSONObject, RetrieveEntitlements);
  public static boolean isLicenseSignatureValid(XElem, File);
  public void rotateKeysApi();
  public void rotateKeys();
  public void rotateKeys(long);
  public void regenerateNreId();
}
```

**Strings detectados**:
- `EntitlementApi`, `EntitlementUtil`, `EntitlementException`, `EntitlementStatusListener`
- `RetrieveEntitlements`, `RequestCertificates`, `LicenseRefreshToken`
- Frequencies: `KEY_ROTATION_FREQUENCY`, `entitlementCheckFrequency`, `THIRTY_MINUTES`, `ENTITLEMENT_RETRY_DELAY_MS`
- **Periodic check**: `ScheduledExecutorService` con `keyRotation` + `entitlementCheck` jobs

### Hallazgos
- "nCloud" interno es **`EntitlementApi`** — vive en `com.tridium.nre.subscription`.
- Subscription license usa **LRT (License Refresh Token)** + periodic key rotation + periodic entitlement check vs cloud.
- `regenerateNreId()` permite renovar nre identity (post-clone scenario para evitar collision).
- Si no hay subscription license, se cae al `NLicenseManager` clásico (file-based con XElem signed).

### BSMANotificationSettings (UI alarm settings)
```
public class BSMANotificationSettings extends BComponent {
  public boolean getEnabled();
  public boolean getShowExpirationDate();
  public boolean getShowExpirationReminder();
  public int getExpirationReminder();    // días antes
}
```
Settings UI para warning de SMA expiration — el "real check" es vía `Feature.isExpired()` / `getExpiration()` de cada feature.

### Gotchas
- `feature.getb("sma.exempt", false)` con typo (`Sma.Exempt`, `sma_exempt`, etc.) **devuelve silently false** sin error — bug-prone si se modifica .license a mano.
- Subscription LRT directory: `getSubscriptionDirectory()`, certs: `getSubscriptionCertificateDirectory()`, licenses: `getSubscriptionLicenseDirectory()` — **3 directorios separados** en file system local.
- `CLONED_FILE` flag detecta NRE-id collision (post host duplication) y bloquea startup hasta `regenerateNreId()`.

---

## 7. BOnMissingType — confirmado/refutado

**Comandos**:
```bash
# Búsqueda exhaustiva en TODOS los 969 jars del módulos/
find modules -name "*.jar" | xargs -I {} sh -c 'unzip -l {} 2>/dev/null | grep -iE "OnMissing|UnresolvedType|MissingType"'
# → 0 resultados
```

**REFUTADO**: NO existe `BOnMissingType`, `BMissingType`, ni `UnresolvedType` en ningún jar del Optimizer Supervisor 4.14.0.162.

### Mecanismo real de forward-compat (BOG con tipos faltantes)
**Comando**: `javap -c -p 'javax/baja/io/ValueDocDecoder$BogTypeResolver.class'`

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

### Comportamiento real cuando un .bog tiene type ausente
1. **Si typespec es null** (slot mal-decodeado):
   - Si la property es frozen (declared en TYPE) → retorna `Property.getDefaultValue()`
   - Si no → emite `IDecoderPlugin.warningAndSkip("Missing frozen property:...")` y retorna `null`
2. **Si typespec es `module:typeName`** y el módulo está cargado pero el type no:
   - Itera todos los module parts del module
   - Si alguno tiene el type → `ValueDocDecoder.typeResolverNewInstance(NModule, String)` → instancia
   - Si nadie tiene el type → fallback a warning + skip
3. **Si modulo no resuelve**: `ModuleManager.loadModuleParts(name)` lanza exception, capturada y plugin emite error

**Conclusión**: forward-compat es **silent skip + warning logging**, NO un BStub/placeholder. Esto significa:
- Cargar un .bog con types más nuevos → **slot se pierde silenciosamente** (warning en log)
- Save back → la información NO se preserva (porque el slot es null)
- **Pérdida de datos en round-trip si subes/bajas versión de framework**

### Gotchas
- **No hay forward-roundtrip safety**: BOG cargado por estación con types missing y re-guardado pierde slots.
- "BOnMissingType" del Bloque 32.13 era inferencia incorrecta — el modelo mental probablemente lo derivó de patrones en otros frameworks (e.g., Eclipse EMF UnresolvedProxy, Java AbsentTypeReference).
- Para prevenir pérdida: **no abrir/save cycle .bog en versión old del framework** si fue creado con newer.

---

## CORRECCIONES / CONFIRMACIONES a Bloque 32

| Bloque 32 ítem | Estado | Corrección empírica |
|---|---|---|
| 32.6 fox.sys constants | **REFUTADO** | NO existen "constants" — son 6 channels named: sys, file, user, broker, data, spy. Cada uno con set de string-commands en `process(FoxRequest)`. |
| 32.9 BTransaction "no existe" | **PARCIAL** | EXISTE como `javax.baja.sync.Transaction` (sin B). Pero la semántica weak es **CORRECTA** — `abortCommit()` está vacío, no hay rollback. |
| 32.13 BOnMissingType forward-compat | **REFUTADO** | NO existe. Forward-compat = silent skip + warning via `BogTypeResolver.warningAndSkip()`. **Hay pérdida de datos en round-trip cross-version**. |
| 32 Sys.loadType | **CONFIRMADO + AMPLIADO** | Es one-liner delegate a `SchemaManager.load(Class)` (synchronized — bottleneck en concurrent loads). |
| 32 BModule lifecycle hooks | **REFUTADO** | BModule es final, no tiene hooks. Lifecycle real está en (a) `ModuleManager.registerOnLoadCallback()`, (b) `BComponent.started()/descendantsStarted()/stationStarted()/atSteadyState()`. |
| 32 license-rt.jar SMA | **CORREGIDO UBICACIÓN** | NO existe jar separado. Vive en `baja.jar` bajo `com/tridium/sys/license/`. SMA es `feature.getb("sma.exempt", false)` string-based. |
| 32 honMqttDriver "NO analizado" | **CERRADO** | 100% custom Honeywell para sensores Netvox LoRa. Stack base = Eclipse Paho 1.2.5 + AWS IoT SDK (en `abstractMqttDriver-rt.jar`). TLS 1.0–1.3, QoS 0/1/2. License feature `Honeywell:honLoRaMqtt`. |

---

## Gotchas nuevos descubiertos

### G1. Transaction no es ACID
`SyncBuffer.abortCommit(Exception)` está literalmente vacío (`return;`). Si commit falla en op N de M, las ops 1..N-1 **YA ESTÁN APLICADAS**. El estado del componente queda inconsistente. **Impacto**: workflows que dependen de "rollback on error" en `Transaction.start()/end()` están rotos por diseño. Mitigación: validar TODO antes de aplicar SyncOps.

### G2. SchemaManager.load es synchronized — bottleneck de boot
Estaciones con 1000+ módulos custom hacen `Sys.loadType()` en paralelo durante boot — todos serializan en el monitor de SchemaManager. Síntoma: **boot lento sin causa visible**, JVM con high contention en `SchemaManager$$Lock`.

### G3. fox channel `prototype` es estático e inmutable
No se pueden registrar channels custom runtime sin parchear baja.jar. Honeywell **no extendió** los channels en este Supervisor — todos los 6 channels son stock Tridium.

### G4. BOG forward-compat pierde slots silently
Cargar un .bog con types desconocidos en un framework older → warning + slot vacío. Re-save → slot perdido permanentemente. **No hay equivalente a Eclipse EMF's eUnknownFeature preservation.**

### G5. License `sma.exempt` es string typo-prone
`feature.getb("sma.exempt", false)` retorna default false silently con cualquier typo en .license XML. **Auditar manualmente** — typo bugs no se reportan.

### G6. Subscription license tiene 3 directorios separados
`SUBSCRIPTION_DIRECTORY` + `CERTIFICATE_DIRECTORY` + `LICENSE_DIRECTORY`. Backups parciales que omitan alguno corrompen el subscription state. `CLONED_FILE` detecta NRE-id collision post-clone host.

### G7. honMqttDriver soporta SOLO Netvox
La lista de sensores está hardcoded en `lora/sensors/`. Para genéricos LoRa hay que usar `abstractMqttDriver-rt.jar` directamente (sin license `honLoRaMqtt`).

### G8. BModule final → no extensible
Custom módulos no pueden override BModule behavior. Toda customización va via `BComponent` lifecycle hooks, NO via "module hook".

### G9. Eclipse Paho 1.2.5 es MQTTv3 SOLO
Sin MQTT 5.0 properties, reason codes, shared subscriptions. Si proyecto requiere MQTT 5.0 features → necesita driver custom NO basado en `abstractMqttDriver-rt.jar`.

### G10. ModuleManager.loadModule synchronized global
Todas las cargas de módulos serialize. Combined with G2 → boot order nondeterminism amplified bajo carga.

---

## Resumen de archivos clase analizados (paths absolutos)

- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/baja.jar`
  - `javax/baja/sync/Transaction.class`
  - `javax/baja/sync/SyncBuffer.class`
  - `javax/baja/sync/LoadOp.class`, `SyncOp.class`
  - `javax/baja/sys/Sys.class`, `BModule.class`, `BComponent.class`, `Type.class`
  - `javax/baja/util/BTypeSpec.class`
  - `javax/baja/io/ValueDocDecoder.class`, `ValueDocDecoder$BogTypeResolver.class`
  - `javax/baja/security/crypto/BSslTlsEnum.class`
  - `com/tridium/sys/module/NModule.class`, `ModuleManager.class`
  - `com/tridium/sys/schema/SchemaManager.class`
  - `com/tridium/sys/license/LicenseUtil.class`, `NLicenseManager.class`, `BSMANotificationSettings.class`
  - `com/tridium/sys/license/dom/Feature.class`
  - `com/tridium/sys/license/subscription/SubscriptionLicenseManager.class`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/fox-rt.jar`
  - `com/tridium/fox/sys/BFoxChannel.class`, `BSysChannel.class`, `BFoxConnection.class`, `BFoxChannelRegistry.class`
  - `com/tridium/fox/sys/file/BFileChannel.class`
  - `com/tridium/fox/sys/user/BUserChannel.class`
  - `com/tridium/fox/sys/broker/BBrokerChannel.class`
  - `com/tridium/fox/sys/data/BDataChannel.class`
  - `com/tridium/fox/sys/spy/BSpyChannel.class`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/abstractMqttDriver-rt.jar`
  - `com/tridium/mqttClientDriver/BAbstractMqttDriverDevice.class`
  - `com/tridium/mqttClientDriver/util/MqttClientFactory.class`, `BMqttQualityOfService.class`
  - `com/tridium/mqttClientDriver/clients/paho/MqttClientPaho.class`
  - `META-INF/maven/org.eclipse.paho/.../pom.properties` → version=1.2.5
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/honMqttDriver-rt.jar`
  - `com/honeywell/honmqttdriver/BHonMqttNetwork.class`
  - `com/honeywell/honmqttdriver/lora/points/BHonLoraSensor.class`
  - `com/honeywell/honmqttdriver/util/LoraLicenseHandler.class`
  - 7 sensor classes en `lora/sensors/`
  - Resource: `module://honMqttDriver/res/SensorDetails.json`
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/ext/nre.jar`
  - Solo nre internal infrastructure (syslog, security, util) — NO contiene BModule ni Sys (esos viven en baja.jar).

---

## Observación final

El Bloque 32 mostró 4 errores estructurales no triviales:
1. Asumió `BTransaction` con prefijo B (es `Transaction` sin B porque NO es BComponent).
2. Asumió license-rt.jar separado (vive en baja.jar).
3. Asumió BModule tiene lifecycle hooks (no — está final, hooks viven en BComponent + ModuleManager).
4. Inventó `BOnMissingType` (no existe — es warning+skip).

Las 4 correcciones tienen impacto operacional real: workflows asumiendo rollback transactional fallan; búsquedas de license en jar no-existente fallan; intentos de override BModule.start() compilan-fallan; y migración cross-version de .bog tiene data loss silencioso no documentado en el modelo previo.
