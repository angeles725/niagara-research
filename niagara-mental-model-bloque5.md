# Niagara N4 — Mental Model · Bloque 5: ORD + BOG + Queries (BQL/NEQL) + Hierarchy + Tags

**Sesión**: 2026-04-22
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes primarias**:
- `niagara-help/devguide-clean/` — docs oficiales (naming, bog, bql, bqlExpressions, bqlExamples, neql, hierarchy, entityModel, xml, station)
- `niagara-help/source/baja/javax/baja/naming/` — BOrd, BOrdScheme, OrdTarget
- `niagara-help/source/baja/javax/baja/sync/` — LoadOp
- `platform.bog` real (defaults/platform.bog)
- Decompilado Vineflower cuando no hay source

Bloque 4 explicó CÓMO se declara un componente (slots, tipos, lifecycle). Este bloque explica **cómo se navegan, persisten y consultan** esos componentes.

---

## Tabla de contenidos

1. [ORD — Object Resolution Descriptor](#51-ord--object-resolution-descriptor)
2. [BOG — Baja Object Graph](#52-bog--baja-object-graph)
3. [Queries y modelos semánticos (BQL / NEQL / Hierarchy / Tags)](#53-queries-y-modelos-semánticos)
4. [Síntesis del bloque](#síntesis-del-bloque)

---

## 5.1 ORD — Object Resolution Descriptor

### 5.1.1 Concepto y rol

Un **ORD** es el sistema universal de nombrado de Niagara para identificar recursos de forma uniforme, agnóstico al almacenamiento (VM local, filesystem, station database, remote station via FOX, BQL query, módulo JAR).

**Gramática**:
```
ord   := query ("|" query)*
query := scheme ":" body
```

Cada query transforma el resultado anterior. Similar a una URI pero **composicional**.

**Ejemplos reales**:
- `local:|slot:/Drivers/bacnet/points/temp` — VM local + slot path.
- `ip:192.168.1.10|fox:1911|station:|h:abc123` — host + sesión FOX + station space + handle.
- `file:/etc/system.properties` — archivo filesystem.
- `module://bajaui-ux/icons/x16/cut.png` — recurso dentro de JAR.
- `bql:select * from control:NumericPoint where out.value>50` — query BQL.

**Por qué existe**:
- **Unificación**: una sola API (`BOrd.resolve()`) para cualquier recurso.
- **Composición**: host queries absolutas, session queries relativas al host, space queries relativas a session.
- **Persistencia**: `BOrd` es `BSimple` (inmutable, serializable, linkable en Properties).

### 5.1.2 Schemes — tabla exhaustiva

29 schemes registrados en el source oficial (más schemes custom per-módulo — BACnet, OBIX, reflow, etc.). Se accede por `Sys.getRegistry().getOrdScheme(schemeId)`.

| Scheme | Tipo | Body | Identifica | Ejemplo |
|--------|------|------|-----------|---------|
| `local` | Host+Session | (empty) | VM local (`BLocalHost`) | `local:` |
| `ip` | Host | IP/hostname | Máquina remota | `ip:192.168.1.10` |
| `fox` | Session | puerto opcional | Protocolo FOX TCP | `fox:` o `fox:4911` |
| `station` | Space | (empty) | `BComponentSpace` station | `station:` |
| `slot` | Path | `/path/to/slot` | Slot en `BComplex` | `slot:/Drivers/bacnet` |
| `h` | Path | handle ID | BComponent por handle único | `h:42abc` |
| `file` | Space | filepath | Sistema de archivos | `file:/etc/config.bog` |
| `module` | Space | `//modname/path` | Recurso en JAR módulo | `module://bajaui-ux/icons/cut.png` |
| `bql` | Query | SELECT…WHERE | Baja Query Language | `bql:select * where name='temp'` |
| `spy` | Space | `/path` | Diagnostic pages (`/spy/`) | `spy:/sysManagers/alarmManager` |
| `service` | Lookup | type spec | Service por type | `service:baja:AlarmManager` |
| `type` | Lookup | type spec | Type del registry | `type:baja:Numeric` |
| `resolve` | Transform | ORD relativo | Re-resolver ORD relativo | `resolve:slot:/config` |
| `view` | Transform | view config | Vistas customizadas (Workbench) | `view:properties` |
| `namespace` | Path | `/ns/path` | Namespace virtual (advanced) | `namespace:/devices/bacnet` |
| `hierarchy` | Path | `/hier/path` | Navegación jerárquica | `hierarchy:/devices` |
| `virtual` | Space | `/virtual` | Componentes virtuales (gateway) | `virtual:/gw/device1` |
| `zip` | Space | `/file.zip/entry` | ZIP archives | `zip:/backup.zip/file.txt` |
| `bog` | Space | BOG file (legacy) | Persist format | `bog:/station.bog` |
| `neql` | Query | NEQL syntax | NEQL queries | `neql:hs:equip` |
| `alarm` | Lookup | alarm path | Alarmas por path | — |
| `widget` / `widgetId` | Path | widget ref | Workbench widgets | — |
| `sql` / `rdbms` | Query | SQL | Enterprise DB | — |
| `mockhost` / `mocksession` | Test | (test) | Mocking para testing | — |
| `root` / `nav` | Nav | navegación CLI | Workbench navigator | — |

**Tipología**:
- **Host schemes** (`local`, `ip`): absolutas. Descartan queries anteriores en normalize.
- **Session schemes** (`fox`, `station`): cierran sesión dentro del host.
- **Space schemes** (`file`, `module`, `slot`, `virtual`, `zip`, `spy`): requieren session/space base.
- **Lookup schemes** (`service`, `type`, `resolve`): inspeccionan registry.
- **Query schemes** (`bql`, `neql`, `sql`): ejecutan contra un data source.
- **Custom**: módulos registran esquemas propios vía `@NiagaraType(ordScheme="...")`.

### 5.1.3 Composición y parsing

`BOrd` es `BSimple` (immutable):

```java
BOrd ord = BOrd.make("ip:somehost|fox:|station:|slot:/Drivers");
String s = ord.toString();  // "ip:somehost|fox:|station:|slot:/Drivers"
```

**Parsing** (`BOrd.parse()` → `OrdQuery[]`): tokeniza por `|`, luego cada query por `:`.

```java
OrdQuery[] queries = ord.parse();
// queries[0] = IpQuery("ip", "somehost")
// queries[1] = FoxQuery("fox", "")
// queries[2] = StationQuery("station", "")
// queries[3] = SlotPath("slot", "/Drivers")
```

**Interface `OrdQuery`**:
```java
public interface OrdQuery {
  String getScheme();
  String getBody();
  boolean isHost();                 // true si es absoluta
  boolean isSession();               // true si cierra sesión
  void normalize(OrdQueryList, int, Context);
}
```

**Subclases concretas**:

| Clase | Schemes | Métodos especiales |
|-------|---------|-------------------|
| `BasicQuery` | genéricos | `getScheme()`, `getBody()` |
| `LocalQuery` | `local` | `isHost=true`, `isSession=true` |
| `SlotPath` | `slot` | `depth()`, `nameAt(i)`, `getParentPath()` |
| `FilePath` | `file`, `module`, `zip` | `isAbsolute()`, `toFile()` |
| `BqlQuery` | `bql` | `getUnescaped()` |
| `ViewQuery` | `view` | view-specific |

**Construction programática**:
```java
// Via query array
OrdQuery[] queries = { hostQuery, sessionQuery, slotQuery };
BOrd ord = BOrd.make(queries);

// Via appending
BOrd base = BOrd.make("ip:host");
BOrd full = BOrd.make(base, "fox:|station:|slot:/path");
```

### 5.1.4 Resolution pipeline

Método principal: `BOrd.resolve(BObject base, Context cx) → OrdTarget`.

**Flow end-to-end**:

```
1. Parse ORD → OrdQuery[]
2. Normalize queries (mutate list in-place)
   └─ Host queries trim queries previas (absolutas)
3. Crear OrdTarget inicial (cx, ord, queries, base)
4. Iterar queries [i=0..length-1]
   ├─ scheme = BOrdScheme.lookup(q.getScheme())
   ├─ target = scheme.resolve(target, q)
   │   ├─ BLocalScheme: return BLocalHost.INSTANCE
   │   ├─ BSlotScheme: walk slot tree via SlotPath.depth()
   │   ├─ BFileScheme: resolve filesystem path
   │   ├─ BBqlScheme: execute BQL via BIBqlResolver agent
   │   └─ [custom schemes]: similar pattern
   └─ new OrdTarget(previousTarget, resolvedObject)
5. Return final OrdTarget → caller usa target.get() → BObject
```

**Ejemplo traced**:
```java
BOrd ord = BOrd.make("local:|slot:/Drivers/bacnet/points/temp");
OrdTarget target = ord.resolve(null, null);  // base=null → BLocalHost.INSTANCE

// PARSE:    [LocalQuery, SlotPath("/Drivers/bacnet/points/temp")]
// NORMALIZE: LocalQuery.isHost=true; nothing to trim.
// ITER 0:   BLocalScheme.resolve() → target.object = BLocalHost.INSTANCE
// ITER 1:   BSlotScheme.resolve() → walk .get("Drivers").get("bacnet").get("points").get("temp")
// RESULT:   target.get() = BNumeric (valor final)
```

**Errores**:
- `NullOrdException` — BOrd.NULL (empty).
- `UnknownSchemeException` — scheme no registrado.
- `SyntaxException` — query malformada.
- `UnresolvedException` — path/recurso no existe.
- `InvalidOrdBaseException` — base incompatible con scheme.

### 5.1.5 Relative vs absolute

**Absolute**: arranca con host scheme, resuelve sin base.
```java
BOrd abs = BOrd.make("local:|slot:/Drivers");
BObject r = abs.get();  // No base necesario
```

**Relative**: arranca con session/space scheme, requiere base.
```java
BOrd rel = BOrd.make("slot:/Drivers");
BComponent parent = /* ... */;
BObject r = rel.resolve(parent, null).get();
```

**Base resolution**:
- Si `base == null` → asume `BLocalHost.INSTANCE`.
- Primera query se resuelve usando `base` como OrdTarget inicial.

**Normalization** (`OrdQueryList.normalize()`):
- Query con `isHost()=true` **descarta queries anteriores** (trimming).
  - `slot:/a | ip:host | slot:/b` → `ip:host | slot:/b`.
- Query con `isSession()=true` permite relatividad dentro del host.

### 5.1.6 OrdTarget y handlers custom

**`OrdTarget`** (resultado de resolution; también implementa `Context`):
```java
public class OrdTarget implements Context {
  private BObject object;           // recurso resuelto hasta acá
  private OrdTarget base;           // OrdTarget previo (cadena)
  private BOrd ord;                 // BOrd original
  private OrdQuery[] queries;       // queries parseadas
  private BUser user;               // usuario del contexto
  private String lang;              // Lexicon idioma
  private BFacets facets;           // metadata acumulada
  private BPermissions permissions;
  private BComponent container;
}
```

**Navigation**:
```java
OrdTarget t = ord.resolve(base, cx);
BObject result = t.get();
BUser user = t.getUser();
OrdTarget baseTarget = t.getBaseOrdTarget();
```

**Registering custom scheme**:
```java
@NiagaraType(ordScheme = "myscheme")
@NiagaraSingleton
public final class BMyScheme extends BOrdScheme {
  public static final BMyScheme INSTANCE = new BMyScheme();
  private BMyScheme() { super("myscheme"); }
  @Override public OrdQuery parse(String body) { return new MyQuery(body); }
  @Override public OrdTarget resolve(OrdTarget base, OrdQuery q) {
    BObject resolved = /* ... */;
    return new OrdTarget(base, resolved);
  }
}
```

**Ejemplos en corpus**: `BBqlScheme` (bql-rt), `BAlarmScheme` (alarm-rt), `BObixOrdScheme` (obixDriver-rt), `BBacnetOrdScheme` (bacnet-rt).

### 5.1.7 BOrdList

Collections de ORDs para batch:
```java
BOrdList list = new BOrdList();
list.add(BOrd.make("local:|slot:/Drivers"));
list.add(BOrd.make("file:/etc/config.bog"));
for (int i = 0; i < list.size(); i++) list.get(i).get();
```

---

## 5.2 BOG — Baja Object Graph

### 5.2.1 Concepto y uso

BOG es el formato estándar para **serializar y persistir árboles de BValue a XML**. Es la fotografía del estado de componentes — toda la estructura de slots (properties, actions, topics) con valores, flags y facets se codifica en un XML compacto.

**Casos de uso**:
- `config.bog` — estado completo de la station.
- `.palette` — colecciones de paletas (templates) para Workbench.
- Export/import — trasladar componentes entre stations sin FOX.
- Snapshot de subtrees (backup antes de cambios).
- `.tro` — Tridium Restore Offline (BOG comprimido).

**Formato**: XML plano (`.bog`) o **zipeado** (único entry `file.xml` dentro del ZIP). El zipeado es estándar en station storage.

### 5.2.2 Formato XML — estructura

**Raíz**: todo BOG comienza con `<bajaObjectGraph>`.

```xml
<bajaObjectGraph
  version="4.0"
  reversibleEncodingKeySource="none|keyring|external"
  reversibleEncodingValidator="..."
  reversibleEncodingSalt="..."
  reversibleEncodingIterationCount="...">
```

- `version="4.0"` — schema N4 (AX usaba 1.0, incompatible).
- `reversibleEncodingKeySource` — dónde buscar clave para `BPassword`:
  - `"none"` — sin passwords reversibles. Máxima portabilidad.
  - `"keyring"` — cifrada con clave del station's key ring (no portable).
  - `"external"` — derivada de passphrase (portable, requiere passphrase al decodificar).

**3 tipos de elementos bajo el root**:

| Elemento | Rol |
|----------|-----|
| `<p>` | Property slot — el caso común |
| `<a>` | Action slot (frozen) — raro en BOG de estado |
| `<t>` | Topic slot (frozen) — raro en BOG de estado |

**Atributos universales**:

| Attr | Usa | Significado |
|------|-----|-------------|
| `n` | p,a,t | Nombre del slot (requerido para dinámicos) |
| `m` | p,a,t | Declaración de módulo: `symbol=name` (alias local al documento) |
| `t` | p,a,t | Type reference: `symbol:classname` |
| `f` | p,a,t | Flags encoded (ej. `"ors"` = READONLY+OPERATOR+SUMMARY; ver `Flags.encodeToString()`) |
| `h` | p (BComponent) | Handle opaco único para referencias/links |
| `x` | p | Facets encoded (ej. `"units=b:Celsius"`, `"min=b:0.0"`) |
| `v` | p (BSimple) | Valor serializado vía `BSimple.encodeToString()` |

**Nesting**: properties que contienen BComplex/BComponent se representan como `<p>` anidados.

### 5.2.3 Type references y resolution

**Forma**: `symbol:typename` donde `symbol` mapea a una declaración de módulo local al documento.

```xml
<p m="b=baja" t="b:String" v="hello"/>
<p m="kitControl=kitControl" t="kitControl:SineWave">
```

El `m="b=baja"` dice "la próxima vez que veas `b:Foo`, resolvé como `baja:Foo`". Es alias local.

**Resolution en decode**:
1. `ValueDocDecoder` lee `t="symbol:classname"`.
2. Busca `symbol` en la tabla de módulos del documento.
3. Llama `Sys.loadType(moduleName + ":" + classname)` via `ITypeResolver`.
4. Obtiene el `Type` metadatos.

**Fallback (versioning)**: si el tipo no existe (módulo removido), `ValueDocDecoder` cae a tipo fallback del metadata o lanza `UnknownTypeException`. Crítico para rolling upgrades.

### 5.2.4 Handle system

**Handle** (`h="..."`): string opaco, único per componente dentro del BOG. Propósito: **identificar componentes para referencias y links**.

Ejemplo con link:
```xml
<p n="SineWave" h="1" t="kitControl:SineWave">
  <p n="amplitude" v="35"/>
</p>
<p n="Add" h="3" t="kitControl:Add"/>
<p n="Link" t="b:Link">
  <p n="sourceOrd" v="h:1"/>          <!-- refiere al h="1" -->
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="inA"/>
</p>
```

**Mecánica**:
1. Cada `BComponent` serializado recibe handle único.
2. Property con `BOrd` se serializa como ORD; si apunta a componente dentro del BOG: `v="h:1"`.
3. Al deserializar, `LoadOp` crea `BOrd` con handle; `resolve()` lo mapea a instancia in-memory.

**Beneficio**: permite referencias circulares (A→B→A) sin punteros Java reales (que romperían la serialización).

### 5.2.5 Encoding de valores

**BSimple** → serializado en atributo `v`:
```xml
<p n="temperature" v="23.5"/>     <!-- BDouble -->
<p n="name" v="Pump-01"/>          <!-- BString -->
<p n="enabled" v="true"/>          <!-- BBoolean -->
```

Convenciones:
- Números: decimal literal.
- Booleanos: `"true"` / `"false"`.
- Strings: UTF-8; caracteres especiales escapados XML (`&#dd;`).
- Enums: ordinal o tag string (depende del tipo).
- Tiempos: ms desde epoch o ISO-8601.
- BOrd: cadena ORD qualified o `"h:N"` para referencias locales.

**BComplex / BStruct** → tree de `<p>` anidados:
```xml
<p n="myStruct" t="myModule:MyStructType">
  <p n="field1" v="value1"/>
  <p n="field2" v="value2"/>
</p>
```

**BComponent** → handle + type + children:
```xml
<p n="controller" h="c1" t="bacnet:BBacnetDevice">
  <p n="instanceNumber" v="100"/>
  <p n="networkNumber" v="1"/>
</p>
```

**BPassword**:
- **Non-reversible**: hash bcrypt/PBKDF2 (verificable pero no recuperable). Típico en userService.
- **Reversible**: cifrada con AES-256. Solo cuando debe enviarse a otro sistema.
- `BogPasswordObjectEncoder` en `ValueDocEncoder.java` maneja ambos.

### 5.2.6 Links en BOG

`BLink` es un BComponent especial que representa binding entre dos slots:

```xml
<p n="bindingName" h="link1" t="b:Link">
  <p n="sourceOrd" v="h:1"/>
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="inA"/>
  <p n="enabled" v="true"/>
</p>
```

**Semántica**:
- Source slot emite (notificación, cambio de valor).
- Target slot recibe.
- El link es **inbound** (apunta hacia adentro del parent que lo contiene).

**Tipos**:
- property → property (data binding, más común).
- property → action (trigger).
- action → action (event forwarding).
- topic → topic (event relay).
- `BConversionLink` — aplica función de conversión (ej. °C → °F).

**Resolución**: en `LoadOp.loadKnobs()` y `loadRelationKnobs()`, se crean `NKnob` (kernel knobs) que conectan source y target in-memory.

### 5.2.7 LoadOp pipeline (deserialización)

Ubicación: `javax.baja.sync.LoadOp`.

**Orquestación** (`commit()` L100-109):
```java
syncComponent(this.current, this.component, this.partialLoad);
if (!partialLoad) {
  loadKnobs(space);
  loadRelationKnobs(space);
}
```

**`syncComponent(from, to, partialLoad)` L111-232**:
- Sanity: mismo type, mismo handle.
- Itera slots frozen + dynamic en `from`.
- Para cada slot:
  - Si no existe en `to` → `.add(name, value, flags, facets, Context.commit)` (L179).
  - Si existe → actualiza flags, facets, valor.
  - Si removido en `from` → `.remove()` (L223).
- Dinámicos recursivamente: si value es `BComponent`, recursión `syncComponent()` (L184).
- Reorden: `.reorder(orderedDynamicProps, Context.commit)` (L230).

**`syncValue(from, fromProp, to, toProp, ...)` L234-300**:
- BSimple: reemplazo directo con `.set()`.
- BComponent: recursión.
- BStruct: sincronización in-place.

**`loadKnobs(space)`**: crea `NKnob` entre properties. Activa observers.

**`loadRelationKnobs(space)`**: específico para `BLink`/`BRelation`. Resuelve `sourceOrd` → handle → componente actual. Crea `NRelationKnob`. Activa si `enabled=true`.

**Thread context**: todo en engine thread compartido (asincrónico, no multithread interno).

### 5.2.8 Saving + atomic writes + backup

**Flush cycle** (típico ~5 seg):
1. Cambio en property marca componente dirty (`changed()` callback).
2. Station enumera dirty components.
3. `ValueDocEncoder.encodeDocument()` → `.bog.tmp`.
4. **Atomic rename**: `.bog` → `.bog.bak`, `.bog.tmp` → `.bog`.

> **[Corregido por B402 (§14)]** Los pasos 1-2 aplican a `BBogSpace` (platform.bog / palettes / BOG montado),
> NO a `config.bog`: la station corre sobre un `BComponentSpace` cuyo `modified()` es no-op — no hay dirty flag
> ni enumeración de dirty components. El guardado de `config.bog` lo dispara `StationManager` por TIEMPO
> (`stationAutoSaveFrequency`, default `BRelTime.HOUR` = 1 h), no por cambio. El temp file real es
> `.bog.working`, no `.bog.tmp`. Detalle completo de ambos paths en **[Block 402]**.
5. `.bog.bak` preserva versión anterior para recovery.

**Clases**:
- `com.tridium.sys.station.StationStorage` — orquesta flush.
- `ValueDocEncoder` (javax.baja.io) — codifica a XML.

**Password handling en save**:
```java
ValueDocEncoder encoder = new ValueDocEncoder(file, context);
encoder.setPassPhrase(Optional.of(phrase));  // solo si keySource="external"
encoder.setZipped(true);
encoder.encodeDocument(component);
```
Passwords cifran antes del write, nunca plaintext.

**Compresión**: `setZipped(true)` → ZIP con entry `file.xml`. Reduce 10-50% vs XML plano.

### 5.2.9 Ejemplo real — platform.bog comentado

Fragmento real de `defaults/platform.bog`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bajaObjectGraph version="4.0" reversibleEncodingKeySource="none" FIPSEnabled="false">
  <!-- Root: PlatformServiceContainer con handle "8f1df" -->
  <!-- m="plat=platform" declara alias "plat" para módulo platform -->
  <p h="8f1df" m="plat=platform" t="plat:PlatformServiceContainer">
    <!-- Child: SystemService (BComponent) con handle propio -->
    <p n="SystemService" h="8f1e0" t="plat:SystemPlatformServiceWin32">
      <!-- BString frozen: valor en atributo v= -->
      <p n="platformServiceDescription" v="System Settings"/>
      <!-- BComponent anidado con handle -->
      <p n="stationSaveAlarmSupport" h="8f1e2" t="plat:PlatformAlarmSupport">
        <!-- BStruct BFormat con valor especial lexicon lookup -->
        <p n="toFaultText" m="b=baja" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveFailure)%"/>
        <p n="toNormalText" t="b:Format" v="%lexicon(platform:SystemPlatformService.stationSaveSuccess)%"/>
        <!-- BFacets encoded: "alarmType=s:station$20save$20failure" ($20 = espacio escapado) -->
        <p n="metaData" t="b:Facets" v="alarmType=s:station$20save$20failure"/>
      </p>
    </p>
    <p n="TcpIpService" h="8f1e4" t="plat:TcpIpPlatformService">
      <p n="platformServiceDescription" v="TCP/IP Settings"/>
    </p>
    <!-- LicenseService con child oculto (f="h" = HIDDEN) -->
    <p n="LicenseService" h="8f1e9" t="plat:LicensePlatformService">
      <p n="platformServiceDescription" v="Licensing"/>
      <p n="entitlementAlarmSupport" f="h" h="8f1ea" t="plat:PlatformAlarmSupport">
        <p n="toOffnormalText" t="b:Format" v="%lexicon(platform:entitlementCheckinFailure)%"/>
        <p n="metaData" t="b:Facets" v="alarmType=s:license"/>
      </p>
    </p>
    <!-- BBlob WsAnnotation (coords UI) con flag READONLY (f="r") -->
    <p n="wsAnnotation" f="r" t="b:WsAnnotation" v="83,95,8"/>
  </p>
</bajaObjectGraph>
```

**Observaciones**:
1. Nesting completo via `<p>` anidados.
2. Handles opacos para links/referencias.
3. `m="alias=modulo"` para compacidad.
4. `f="h"` (HIDDEN), `f="r"` (READONLY) — flags en notación `encodeToString()`.
5. `v=` solo para BSimple.
6. `%lexicon(key)%` es placeholder resuelto runtime contra Lexicon.
7. En station real: este archivo está zipeado (`.bog` dentro de ZIP).

---

## 5.3 Queries y modelos semánticos

### 5.3.1 BQL — Baja Query Language

**Propósito**: SQL-like para identificar conjuntos de datos en el component tree y extensiones (history, alarms). Es **tree-aware** — opera sobre la jerarquía slot:/a/b/c.

**Sintaxis**:
```
select <projection> from <extent> where <predicate> [having <expr>] [order by <cols>]
```

| Parte | Rol | Ejemplo |
|-------|-----|---------|
| **Extent** | Tipo a buscar recursivamente | `control:ControlPoint` |
| **Base (ORD)** | Punto de partida | `slot:/a/b` (subtree) o `history:` (toda DB) |
| **Projection** | Columnas del resultado | `name, out.value as 'Value', status` |
| **Predicate** | Filtro booleano | `out.value > 50 and status.alarm` |
| **Path expression** | Navegación punto | `out.value`, `parent.name`, `facets.units.unitName` |

**Queryable types** (implementan `BIQueryable`): `control:ControlPoint`, `history:HistoryExt`, `alarm:AlarmSourceExt`, `schedule:AbstractSchedule`, `baja:Link`, etc.

**Path expressions** (relativos al extent):
- Properties directas: `out`, `in`, `out.value`, `out.status`.
- Getters unwrap: `getX()` → `x`.
- Nested structs: `facets.units.unitName`.
- Predicates: `status.alarm`, `slotExists('name')`, `propertyExists('out')`.

**Funciones escalares built-in**:
```
slotExists(name:BString)        → boolean
propertyExists(name:BString)    → boolean
substr(str, start, end)         → BString
```

**Agregadas** (GROUP BY implícito por columnas no-agregadas):
```
COUNT(*) | COUNT(expr)
MAX(expr) | MIN(expr) | SUM(expr) | AVG(expr)
```

**Ejecución**:
```java
BOrd ord = BOrd.make(
  "slot:/Services|bql:select slotPath, out from control:NumericPoint where out.value > 50"
);
BITable result = (BITable) ord.resolve(baseComponent).get();
TableCursor cursor = (TableCursor) result.cursor();
while (cursor.next()) {
  // cursor.get(colIndex)
}
```

**Operadores**:
- Lógicos: `and`, `or`, `not`.
- Comparación: `=`, `!=`, `<`, `<=`, `>`, `>=`, `like` (wildcards `%`, `*`).
- Aritméticos: `+`, `-`, `*`, `/`.

**Literales tipados**:
```
'string literal'           → BString
10, 3.14                   → numeric
true, false                → boolean
alarm:SourceState.normal   → enum
baja:RelTime '10000'       → BSimple con encoding string
```

**Ventaja BQL**: tree structure, dynamic method access, ORD-embeddable → reportes auto-refreshables.

### 5.3.2 NEQL — Niagara Entity Query Language

**Propósito**: queries sobre el **entity model** (tags + relations) en vez del slot tree. Introducido N4.x para separar semántica (Haystack-compatible) de la estructura física.

**Diferencias vs BQL**:
- Solo taggable entities (`BIEntity`).
- NO pathing (`parent.parent` prohibido).
- NO projection (retorna entities completas siempre).
- SÍ relaciones y traversal.
- SÍ markers, strings, números en tags.
- NO aggregate functions.

**Sintaxis**:
```
<predicate>                               // filter select (implicit)
select <tag-list> where <predicate>       // explicit pero projection ignorado
traverse <relation> (where <pred>)        // traversal relacional
```

**Ejemplos**:
```neql
hs:equip                                  // marker (solo presencia)
hs:area >= 150                            // tag value
hs:primaryFunction = "backup"
n:name like ".*Basement.*"                // regex
hs:yearBuilt > 2015 and hs:area <= 400    // lógica

traverse n:child-> where n:name = "OutsideTemp"
n:parent->n:parent->n:name like ".*Basement.*"

not n:point
!hs:equip
```

**Tags** (namespace:key):
```java
entity.tags().set(Id.newId("hs:floor"), BInteger.make(1));
// O via component model:
myComp.add(SlotPath.escape("b:floor"), BInteger.make(1), Flags.METADATA);

if (entity.tags().contains(Id.newId("n:point"))) { ... }
```

**Relaciones dirigidas**:
```java
entity.relations().add(Id.newId("hs:chilledWaterPlantRef"), otherEntity);
Collection<Relation> ins = entity.relations().get(relationId, Relations.IN);
```

**Ejecución**:
```java
BOrd q = BOrd.make("neql:hs:equip and hs:ahu");
BQueryResult r = (BQueryResult) q.resolve(base).get();
r.getResults().forEachRemaining(e -> System.out.println(((BComponent)e).getName()));
```

### 5.3.3 Hierarchy service

**Propósito**: árbol de navegación **lógica** alternativo al slot tree. Reorganiza componentes por criterio (ubicación, tipo, propiedad) sin mover nada en config.bog.

**Componentes**:
- `BHierarchyService` — singleton, gestiona definiciones.
- `BHierarchy` — una definición (contiene N `BLevelDef`).
- `BLevelDef` (4 tipos) — reglas para cada nivel.
- `BHierarchySpace` — contenedor virtual (client-side).
- `BLevelElem` (implementa `BINavNode`) — nodo jerárquico.

**4 tipos de `BLevelDef`**:

| Tipo | Rol | Ejemplo |
|------|-----|---------|
| `BGroupLevelDef` | Agrupa por valor único de tag | Puntos por piso (`b:floor`) |
| `BListLevelDef` | Grupos estáticos via `BNamedGroupDef[]` + NEQL filters | "HVAC Points" (`n:point and hs:hvac`), "Water Points" (`n:point and hs:water`) |
| `BQueryLevelDef` | Entities matching NEQL query | Todos los equipos: `hs:equip` |
| `BRelationLevelDef` | Entities alcanzables via relación | Hijos por `traverse n:childPoint->` |

**Ejemplo — jerarquía "Zona/Equipo/Punto"**:
```
BHierarchy {
  BQueryLevelDef(neql: "hs:zone")                    // Nivel 1: zonas
    BGroupLevelDef(groupBy: "b:location")            // Nivel 2: agrupa por ubicación
      BQueryLevelDef(neql: "hs:equip")               // Nivel 3: equipos
        BRelationLevelDef(relationId: "n:childPoint->", repeat: true)  // Nivel N
}
```

**Roles/Permisos**: `BRoleHierarchies` en cada `BRole` limita qué hierarchies son visibles.

**Navigation API**:
```java
BHierarchySpace hs = (BHierarchySpace) BOrd.make("hierarchy:").get(root);
BLevelElem elem = (BLevelElem) BOrd.make("hierarchy:SampleHierarchy").get(root);
for (BINavNode child : elem.getNavChildren()) {
  System.out.println(child.getNavDisplayName(null));
}
```

**Performance**: hierarchy ORD resolution es costoso (NEQL per nivel). Caché recomendado.

### 5.3.4 Tag Dictionary (Haystack semantic model)

**Propósito**: tabla central de tags (nombre, tipo, alias, metadata) para validar/descubrir. Soporta `SmartTagDictionary` (tags implícitos via reglas).

**Interfaz**:
```java
interface TagDictionary {
  boolean getEnabled();
  String getNamespace();
  Iterator<TagInfo> getTags();
  Iterator<TagGroupInfo> getTagGroups();
  Collection<TagInfo> getValidTags(Entity entity);
  Iterator<RelationInfo> getRelations();
}
```

**Namespaces built-in**:
- `n:` (Niagara) — `n:point`, `n:child`, `n:vendor`, `n:name`.
- `hs:` (Haystack) — `hs:site`, `hs:equip`, `hs:point`, `hs:sensor`, `hs:cmd`, `hs:chiller`, `hs:ahu`, `hs:zone`.
- `b:` (custom) — project-specific.

**Tags comunes**:

| Tag | Tipo | Semántica |
|-----|------|-----------|
| `n:point` | Marker | Componente es un punto |
| `hs:equip` | Marker | Componente es equipo HVAC |
| `hs:area` | Double | Área en m² |
| `hs:yearBuilt` | Integer | Año de construcción |
| `b:floor` | Integer | Piso (custom) |

**SmartTagDictionary** (implied tags): define reglas — ej. `BControlPoint` → implícitamente `n:point`; `hs:chiller` → implícitamente `hs:equip`. Sin tocar el componente original.

**Uso runtime**:
```java
Tag t = Tag.newTag("hs:floor", BInteger.make(3));
entity.tags().set(t);
if (entity.tags().contains(Id.newId("n:point"))) { ... }

TagDictionary dict = service.lookup("haystack");
Collection<TagInfo> valid = dict.getValidTags(entity);
```

### 5.3.5 Matriz de decisión — BQL vs NEQL vs Hierarchy

| Caso de uso | Herramienta | Razón |
|-------------|-------------|-------|
| Reportes dinámicos (valores, alarmas, history) | **BQL** | Tree, path expressions, aggregates, ORD-embeddable |
| Búsquedas semánticas (chillers, equipos piso 2) | **NEQL** | Tags directos, Haystack native |
| Navegación UI alternativa | **Hierarchy** | Reorganización lógica, scope-aware, roles |
| Validación de tags | **TagDictionary** | Metadata central, SmartTags |
| Traversal de relaciones | **NEQL traverse** | Dirección explícita |
| Combo estadístico + tag-filter | **BQL** con metadata extendida | BQL es el único con aggregates |

**Performance**:
- BQL: rápido (tree local, indexado).
- NEQL: moderado (scan de tags, permission checks inline).
- Hierarchy: lento (NEQL por nivel × N). Cachear.
- TagDictionary: rápido (lookup memoria).

**Gotcha**: NEQL **NO soporta COUNT/MAX/MIN/SUM/AVG**. Para estadísticas, BQL obligatorio.

---

## Síntesis del bloque

### Modelo mental consolidado

Bloque 5 explica **cómo se pregunta y cómo se persiste** el mundo de componentes que Bloque 4 definió. Tres mecanismos ortogonales:

1. **ORD** = pregunta "¿dónde está X?" — resolver en cualquier espacio (VM, filesystem, station, módulo, query).
2. **BOG** = respuesta "así queda X serializado" — XML minimalista con handles + tipos + flags + facets, atomic write, compresión zip.
3. **Queries** = pregunta "¿quiénes son los X que cumplen Y?" — BQL (tree-aware, aggregates) / NEQL (tag-aware, traversal) / Hierarchy (navegación lógica) / TagDictionary (validación).

### Conexiones con bloques anteriores

- **Bloque 1 (Estructura)**: módulos tienen ordScheme declarado via `@NiagaraType(ordScheme="...")`; el registry los indexa y los hace resolvables.
- **Bloque 2 (Licensing)**: feature queries (`Sys.getLicenseManager().checkFeature()`) retornan `Feature` = BComplex navegable con ORDs y slots tipados.
- **Bloque 3 (Security)**: permisos se verifican en OrdTarget al resolver — `BPermissions` viaja con el target, el `BSecurityManager` chequea antes de retornar el BObject.
- **Bloque 4 (Baja Object Model)**: todo lo de acá opera sobre los Slots del Bloque 4. BOrd es BSimple; BOG serializa tree de Property/Action/Topic; BQL queries devuelven cursors sobre componentes.

### Gotchas críticos

1. **ORDs con handle `h:` son path-independientes** — si movés un BComponent, su ORD por slot path se rompe, pero por handle NO. Útil para referencias estables.
2. **BOG zippeado vs plano** — misma sintaxis XML adentro, pero station usa zip. Si hacés copia manual, cuidá que el ZIP tenga entry `file.xml` (singular).
3. **Handles son locales al documento BOG** — no son globalmente únicos. Mismo handle en dos BOGs distintos son entidades distintas.
4. **Password keySource=none** = el BOG es portable pero SIN passwords reversibles (los setea a default). `keyring` = no portable. `external` = portable con passphrase.
5. **NEQL sin aggregates** — para estadísticas de tags, hay que combinar NEQL + BQL (o hacer agregación client-side).
6. **Hierarchy es caro** — cada nivel ejecuta NEQL. Para árboles grandes, cachear o usar vistas tabulares.
7. **LoadOp corre en engine thread** — heavy deserialización puede bloquear callbacks de otros componentes.
8. **Fallback types en BOG** — si un módulo desaparece, `ValueDocDecoder` puede caer a fallback o fallar. Validar con `gradlew :moduleSignCheck` antes de upgrades.

### Qué habilita

Con Bloques 1-5 podés:
- Navegar a cualquier componente por ORD, tanto desde código como desde queries.
- Entender qué pasa cuando se flushea `config.bog` y cómo se recupera tras crash.
- Escribir reportes BQL embebidos en ORDs (ej. como value de una Property).
- Construir vistas de navegación alternativas con Hierarchy + NEQL.
- Leer un `.bog` real y entender línea por línea.

**Lo que todavía no podés** con Bloques 1-5:
- Escribir un control loop kitControl (Bloque 6).
- Implementar un driver (BACnet, Modbus, etc.) (Bloque 7).
- Entender el pipeline de alarmas, histories y schedules (Bloque 8).

**Próximo**: Bloque 6 — Control Engine (kitControl + links + execution).

---

## Engram topic keys generados por este bloque

- `niagara/navigation/ord-system` — BOrd, schemes, parsing, resolution pipeline, OrdTarget.
- `niagara/persistence/bog-format` — estructura XML, handles, type resolution, LoadOp, flush cycles.
- `niagara/queries/bql-neql-hierarchy-tags` — BQL, NEQL, Hierarchy service, Tag Dictionary, matriz de decisión.

---

**Sesión cerrada**: 2026-04-22 — Bloque 5 consolidado y verificado empíricamente.
