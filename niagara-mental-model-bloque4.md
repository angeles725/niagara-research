# Niagara N4 — Mental Model · Bloque 4: Baja Object Model

**Sesión**: 2026-04-21
**Distribución**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY (3 sub-agents Explore en paralelo)
**Fuentes primarias**:
- `niagara-help/devguide-clean/` — docs oficiales (objectModel, componentModel, slot-o-matic-2000, buildingSimples, buildingComplexes, buildingEnums, execution, entityModel)
- `niagara-help/source/baja/javax/baja/sys/` — código fuente Java del módulo `baja`
- `modules/Prototipos/modulos/organized/baja/baja/vineflower/javax/baja/sys/` — decompilado Vineflower (cuando no hay `.java` source)

Este bloque cubre el **modelo de programación** de Niagara. Bloques 1-3 describen infraestructura (estructura de módulos, licencias, sandbox JVM). Este es cómo SE ESCRIBE un componente.

---

## Tabla de contenidos

1. [Slot System](#41-slot-system)
   - 4.1.1 Taxonomía — Slot, Property, Action, Topic
   - 4.1.2 Flags de slot
   - 4.1.3 Ordinals y orden de slots
   - 4.1.4 SlotCursor — API de navegación
   - 4.1.5 Annotations + slot-o-matic-2000 codegen
2. [Jerarquía de tipos Baja](#42-jerarquía-de-tipos-baja)
   - 4.2.1 BObject — raíz del universo
   - 4.2.2 BValue — capa de valores
   - 4.2.3 Las tres familias — BComplex, BSimple, BStruct
   - 4.2.4 BComponent — BComplex con ciclo de vida
   - 4.2.5 Tipos atómicos del framework
   - 4.2.6 Enums Baja (BFrozenEnum)
   - 4.2.7 Reglas de implementación para tipos nuevos
3. [Ciclo de vida, Facets y Slots dinámicos](#43-ciclo-de-vida-facets-y-slots-dinámicos)
   - 4.3.1 Ciclo de vida del BComponent
   - 4.3.2 Facets — metadata tipada
   - 4.3.3 Slots dinámicos vs frozen
4. [Síntesis del bloque](#síntesis-del-bloque)

---

## 4.1 Slot System

### 4.1.1 Taxonomía — Slot, Property, Action, Topic

El framework modela componentes como colecciones de **slots** — unidades de metadatos que encapsulan comportamiento y almacenamiento. Existen tres tipos semánticamente distintos:

| Tipo | Interfaz | Rol | Semántica | Ejemplo |
|------|----------|-----|-----------|---------|
| **Property** | `javax.baja.sys.Property` | Almacenamiento | Contiene un BValue tipado; estado persistible; gettable/settable en runtime. Tipo + default value. | `public static final Property temperature = newProperty(0, BDouble.make(20.0))` |
| **Action** | `javax.baja.sys.Action` | Comportamiento | Invocable; puede tomar 0-1 parámetro y opcionalmente devolver BValue. Requiere método `do<ActionName>()` implementador. | `public static final Action reset = newAction(0); public void doReset() { ... }` |
| **Topic** | `javax.baja.sys.Topic` | Evento | Event source placeholder; no almacena estado. Define tipo de evento que dispara. Se invoca con `fire<TopicName>(eventValue)`. | `public static final Topic onError = newTopic(0); public void fireOnError(BString msg) { fire(onError, msg, null); }` |

**Distinción conceptual**:
- **Properties** = preguntas (`get`/`set`), estado persistible.
- **Actions** = órdenes (`invoke`), ejecución.
- **Topics** = notificaciones (`fire`), broadcasting de eventos.

**Frozen vs dynamic**:
- **Frozen**: declarados en classfile en compile-time, inmutables (no se pueden remover en runtime).
- **Dynamic**: agregados/removidos en runtime vía `BComponent.add(String name, BValue value, int flags)`. Solo disponibles en BComponent (no en BStruct).

### 4.1.2 Flags de slot

El framework define **21 flags estándar** + 4 user-defined reservados. Se almacenan como bitmask (int 32-bit). Fuente: `javax.baja.sys.Flags` líneas 169-228.

Aplicabilidad: **P**=Property, **A**=Action, **T**=Topic.

| Flag | Hex | Char | Aplica | Semántica runtime |
|------|-----|------|--------|-------------------|
| `READONLY` | `0x00000001` | r | P | Slot no puede ser modificado por el usuario; writes rechazadas. |
| `TRANSIENT` | `0x00000002` | t | P | No persiste a disco cuando se guarda el objeto graph. Reset a default al recargar. |
| `HIDDEN` | `0x00000004` | h | P,A,T | UI no debe mostrar el slot. Queryable pero invisible en Workbench. |
| `SUMMARY` | `0x00000008` | s | P | Slot focal de UI (columna en tabla, glyph gráfico). |
| `ASYNC` | `0x00000010` | a | A | Action se ejecuta async coalesced en engine thread, no caller thread. |
| `NO_RUN` | `0x00000020` | n | P | Previene recursión start/stop sobre este slot. |
| `DEFAULT_ON_CLONE` | `0x00000040` | d | P | `newCopy()` revierte a default, no clona del source (salvo CopyHints). |
| `CONFIRM_REQUIRED` | `0x00000080` | c | A | Action requiere diálogo de confirmación antes de invocarse. |
| `OPERATOR` | `0x00000100` | o | P,A,T | Security level "operator" (default sin flag = admin). En BStruct aplica recursivo. |
| `EXECUTE_ON_CHANGE` | `0x00000200` | x | P | Cambio de property gatilla ejecución del componente (típico en programs). |
| `FAN_IN` | `0x00000400` | f | P | Múltiples links pueden apuntar a este slot (default: solo 1). |
| `NO_AUDIT` | `0x00000800` | A | P,A | Changes/invocations no se auditan (logging deshabilitado). |
| `COMPOSITE` | `0x00001000` | p | P,A,T | Framework-managed. Setear manualmente es rarísimo. |
| `REMOVE_ON_CLONE` | `0x00002000` | R | P,A,T | `newCopy()` remueve dynamic slots con este flag. Frozen: unaffected. |
| `METADATA` | `0x00004000` | m | P | Property es metadata del objeto (queryable vía `getMetadataProperties()`). |
| `LINK_TARGET` | `0x00008000` | L | P,A,T | Framework-set. Indica que el slot es targeted por active link(s). |
| `NON_CRITICAL` | `0x00010000` | N | P | No persistido en BIDataRecoveryService (battery-less JACE). |
| `USER_DEFINED_1..4` | `0x10000000..0x80000000` | 1..4 | P,A,T | Disponibles para lógica de negocio custom. |

**Combinaciones típicas observadas en el corpus**:
- `READONLY | TRANSIENT` = input linkable (no write user, no persist).
- `HIDDEN | TRANSIENT` = estado interno del componente.
- `SUMMARY | OPERATOR` = property clave, visible a operator.
- `ASYNC | CONFIRM_REQUIRED` = action larga con confirmación UX.

**Discrepancia doc vs código**: el devguide menciona ~15 flags con detalle. El código en `Flags.java` define 21 (agregados: `LINK_TARGET`, `COMPOSITE`, `METADATA`, `NON_CRITICAL`). La fuente de verdad es el código.

### 4.1.3 Ordinals y orden de slots

**Ordinals en enums** (vía `@Range`):
```java
@NiagaraEnum(
  range = {
    @Range("off"),                            // ordinal=0 (auto)
    @Range("on"),                             // ordinal=1
    @Range(value="custom", ordinal=5)         // explícito
  },
  defaultValue = "off"
)
public class BState extends BFrozenEnum { }
```

- Auto-numbering es conveniente.
- Explicit ordinals permiten serialización estable tras añadir ranges nuevos (no shift).

**Orden visual en Workbench** (wire sheet, properties panel):
- El orden de declaración en código NO fuerza directamente el orden en UI.
- **Frozen slots** preservan declaration order (reflection scans en sequence).
- **Dynamic slots** se reordenan explícitamente con `BComponent.reorder(Property[])`.
- **Display hints** (facets, annotations) sugieren orden pero no lo fuerzan.
- El archivo `module.palette` controla presentation hints per-módulo.

No existe un flag `flagsOrdinal` separado — el orden es metadata externa al slot.

### 4.1.4 SlotCursor — API de navegación

Interface: `javax.baja.sys.SlotCursor<S extends Slot>`. Iterador con type-aware convenience methods.

```java
// Iteración básica sobre todos los slots
SlotCursor cursor = myComponent.slotCursor(Slot.class);
while (cursor.next()) {
  Slot slot = cursor.slot();
  String name = slot.getName();
  int flags = slot.getDefaultFlags();
}

// Property-specific
PropertyCursor propCursor = myComponent.propertyAt();
while (propCursor.next()) {
  Property prop = propCursor.property();
  BValue value = propCursor.getBValue();
}

// Saltar por tipo (solo properties cuyo valor es BComponent)
while (propCursor.nextComponent()) {
  BComponent child = (BComponent) propCursor.getBValue();
}

// Filtrado por clase arbitraria
cursor.next(MyCustomClass.class);
```

**Métodos clave**:
- `target()` — componente parent que se itera.
- `slot()`, `property()`, `action()`, `topic()` — cast-safe accessors.
- `getTypeAccess()` — hint primitive vs BValue.
- `getBoolean()`, `getInt()`, `getDouble()`, `getString()`, `getBValue()` — getters tipados.
- `nextObject()`, `nextComponent()` — skip hasta el siguiente slot que matchea la categoría.

### 4.1.5 Annotations + slot-o-matic-2000 codegen

**Pipeline de compilación**:
1. Dev escribe anotaciones `@NiagaraType`, `@NiagaraProperty`, `@NiagaraAction`, `@NiagaraTopic`.
2. `@NiagaraType` la procesa un annotation processor estándar (actualiza `module-include.xml`).
3. `@NiagaraProperty|Action|Topic` las procesa **slot-o-matic** (post-compile tool). Genera métodos + static slot fields DENTRO del mismo `.java`, entre markers:
   ```
   /*+ BEGIN BAJA AUTO GENERATED CODE +*/
   ...
   /*+ END +*/
   ```
4. Gradle: `gradlew :module-rt:slotomatic` ejecuta la herramienta.

**`@NiagaraType`** (obligatorio, marker):
```java
@NiagaraType
@AgentOn(types = "baja:Component")
public class BMyComponent extends BComponent { }
```
Atributos: `agent` (array de `@AgentOn`), `adapter` (`@Adapter` from/to), `ext` (array `@FileExt`), `ordScheme` (String).

**`@NiagaraProperty`**:
```java
@NiagaraProperty(
  name = "temperature",
  type = "baja:AbsTemp",
  defaultValue = "BAbsTemp.make(20)",
  flags = Flags.SUMMARY | Flags.OPERATOR,
  facets = { @Facet(name = "BFacets.MIN", value = "BAbsTemp.make(-50)") }
)
```
Genera:
```java
public static final Property temperature = newProperty(
  Flags.SUMMARY | Flags.OPERATOR,
  BAbsTemp.make(20),
  null // facets
);
public BAbsTemp getTemperature() { return (BAbsTemp) get(temperature); }
public void setTemperature(BAbsTemp v) { set(temperature, v); }
```

**`@NiagaraAction`**:
```java
@NiagaraAction(
  name = "reset",
  parameterType = "baja:RelTime",
  defaultValue = "BRelTime.ZERO",
  returnType = "baja:Status",
  flags = Flags.ASYNC | Flags.CONFIRM_REQUIRED
)
```
Genera:
```java
public static final Action reset = newAction(Flags.ASYNC | Flags.CONFIRM_REQUIRED, BRelTime.ZERO);
public BStatus reset(BRelTime duration) { return (BStatus) invoke(reset, duration, null); }
public BStatus doReset(BRelTime duration) { /* impl */ }
```

**`@NiagaraTopic`**:
```java
@NiagaraTopic(
  name = "changed",
  eventType = "baja:ValueChangedEvent",
  flags = Flags.HIDDEN
)
```
Genera:
```java
public static final Topic changed = newTopic(Flags.HIDDEN, null);
public void fireChanged(BValueChangedEvent event) { fire(changed, event, null); }
```

**Atributos comunes**:
- `name` (required, String).
- `flags` (optional, int).
- `facets` (optional, array de `@Facet`).
- `override` (optional, boolean) — si `true`, no genera getter/setter (asume herencia).

**`@Facet`**:
```java
@Facet(name = "BFacets.MIN", value = "0")
@Facet(name = "BFacets.MAX", value = "100")
@Facet(name = "customKey", value = "\"my string literal\"")  // literal en quotes
```

**`@NiagaraEnum` + `@Range`**:
```java
@NiagaraType
@NiagaraEnum(
  range = {
    @Range("stopped"),
    @Range("running"),
    @Range(value = "fault", ordinal = 99)
  },
  defaultValue = "stopped"
)
public class BRunState extends BFrozenEnum { }
```

**Referencia**:
- `niagara-help/source/baja/javax/baja/sys/Flags.java` — 21 flags constants.
- `niagara-help/source/baja/javax/baja/sys/Slot.java` — interfaz base.
- `niagara-help/source/baja/javax/baja/sys/SlotCursor.java` — API cursor.
- `niagara-help/devguide-clean/slot-o-matic-2000.txt` líneas 200-584 — anotaciones detalladas.

---

## 4.2 Jerarquía de tipos Baja

### 4.2.1 BObject — raíz del universo

**Ubicación**: `javax.baja.sys.BObject` (abstract, `@NiagaraType`).

**Rol**: raíz absoluta. Toda clase que implemente un Type de Niagara debe ser subclase de BObject. Es el análogo Niagara a `java.lang.Object`.

**Métodos críticos**:
- `Type getType()` — devuelve el Type metadatos. Override obligatorio en cada subclase.
- `boolean equivalent(Object obj)` — comparación por valor (default: `equals`). Usado en bindings/sync.
- `String toString(Context context)` — serialización a texto con contexto (Lexicon, idioma).
- `String toDebugString()` — representación con info runtime (identidad, handle, etc.).
- `AgentList getAgents(Context cx)` — viewers/configs del registry para este tipo.
- `BIcon getIcon()` — ícono UI.

**Predicados de tipo** (finales, cachean instanceof):
```java
public final boolean isValue()       // instanceof BValue
public final boolean isSimple()      // instanceof BSimple
public final boolean isComplex()     // instanceof BComplex
public final boolean isStruct()      // instanceof BStruct
public final boolean isComponent()   // instanceof BComponent
```

**Conversiones seguras** (finales, cast sin unchecked):
```java
public final BValue asValue()
public final BSimple asSimple()
public final BComplex asComplex()
public final BStruct asStruct()
public final BComponent asComponent()
```

**Gotcha**: `equals()` compara identidad por defecto (`==`), no valor. Solo BValue y subclases sobrescriben para equality profunda. `equivalent()` es la interfaz estándar para comparación semántica.

### 4.2.2 BValue — capa de valores

**Ubicación**: `javax.baja.sys.BValue` (abstract extends BObject).

**Rol**: capa intermedia que distingue "valores" (inmutables o copiables, serializables) de BObject puro. Todo valor almacenado en un Property slot debe ser BValue (o primitivo: boolean, int, long, float, double, String — wrapeados).

**Métodos añadidos**:
- `BValue newCopy()` — copia profunda. BSimple devuelve `this` (immutable). BComplex/BComponent devuelven nueva instancia con slots copiados recursivamente.
- `BValue newCopy(boolean exact)` — si `exact=true`, preserva metadata exactamente; `false` simplifica.
- `BValue newCopy(CopyHints hints)` — copia controlada (omitir slots, redirigir a otro espacio).

**Por qué existe la capa**: separa "cualquier cosa con Type" (BObject) de "objetos que SON valores semánticos, copiables, serializables, storables en properties" (BValue).

### 4.2.3 Las tres familias — BComplex, BSimple, BStruct

#### BSimple — valor atómico e inmutable

**Ubicación**: `javax.baja.sys.BSimple` (abstract extends BValue implements BIEncodable).

Encapsula UNA unidad de dato indivisible. **Inmutable** — una vez construido, no cambia.

**Abstract methods que todo BSimple implementa**:
- `void encode(DataOutput out)` — serialización binaria.
- `BObject decode(DataInput in)` — deserialización binaria.
- `String encodeToString()` — serialización texto.
- `BObject decodeFromString(String s)` — deserialización texto.
- `boolean equals(Object obj)` — igualdad por valor. **Dos BSimple con mismo valor deben ser equal**.

**Comportamiento especial**:
- `newCopy()` devuelve `this` (no copia, immutable).
- `hashCode()` sin default — lanza `UnsupportedOperationException` si no está sobrescrito. Fuerza que cada BSimple implemente hash por valor.
- `equivalent(obj)` siempre usa `equals(obj)`.

**Convención**: constructores privados + factory methods `make()` (interning, caching).

#### BComplex — valor estructurado con slots (navegable)

**Ubicación**: `javax.baja.sys.BComplex` (abstract extends BValue).

Agrega múltiples valores (slots) en estructura. **Es navegable** — iterable con cursors, accesible por nombre de slot.

**Métodos centrales**:
- `String getName()` — nombre de la instancia.
- `BComplex getParent()` — parent si está embebido.
- `Property getPropertyInParent()` — property donde vive dentro del parent.
- `BValue get(Property prop)` / `set(Property prop, BValue val)` — acceso a slots.
- `ComplexSlotMap slotMap` — estructura interna que mapea slots (congelados + dinámicos).

#### BStruct — BComplex con restricciones

- Solo **frozen Properties** (sin Actions ni Topics).
- Sin slots dinámicos.
- Más eficiente en memoria que BComponent.
- Las Properties de un BStruct SÍ pueden ser linked (a diferencia de BComponent).

#### Tabla comparativa — cuándo usar cuál

| Aspecto | BSimple | BStruct | BComponent |
|---------|---------|---------|------------|
| Slots | 0 | Frozen Properties solo | Frozen/Dynamic Props/Actions/Topics |
| Mutable | No (immutable) | Sí | Sí |
| Memory | Muy bajo (cacheado) | Bajo | Alto (full object space) |
| Linkable | N/A | Sí (Properties del struct) | Las Properties del component NO, pero puede contener BStruct linked |
| Persistencia | Serializable atómico | Serializable binario/texto | Full WYSIWYG, mount en Station |
| Ejemplo | `BInteger`, `BString`, `BAbsTime` | `BFacets`, `BLocation`, `BDependency` | `BController`, `BSchedule`, `BService` |

**Regla de decisión para tipos nuevos**:
1. **Valor único indivisible** (número, texto, fecha, enum) → **BSimple**.
2. **Valor compuesto pero sin actions ni ciclo de vida** → **BStruct**.
3. **Componente vivo** (actions, children, lifecycle) → **BComponent**.

### 4.2.4 BComponent — BComplex con ciclo de vida

**Ubicación**: `javax.baja.sys.BComponent` (extends BComplex implements BISpaceNode, BIProtected, BICategorizable, BIEntity).

**Qué añade sobre BComplex**:

1. **ComponentSpace** — mundo donde vive (típicamente Station). `getComponentSpace()`.
2. **Parent/child hierarchy** — cada BComponent (excepto root) tiene un parent. Árbol jerárquico.
3. **Links** — Property values pueden ser `BOrd` a otros componentes (binding dinámico). Clases: `BLink`, `BConversionLink`.
4. **Persistencia** — serializado a BOG (XML). Sobrevive reboots.
5. **Subscriptions** — listeners vía `Subscriber` interface.
6. **Tags & Categories** — metadata para búsqueda, organización (vía `javax.baja.tag`).
7. **Security** — `BIProtected`: permisos granulares por action/property.

**Métodos clave**:
- `BComponentSpace getComponentSpace()`
- `BComponent getParent()`
- `BComponent[] getChildren(Class<T> type)` — hijos filtrados.
- `void add(String name, BComponent child)` — agrega hijo (maneja parent linkage).
- `void remove(BComponent child)` — quita hijo.
- `BOrd getOrd()` — path absoluto (ej. `station:|slot:/Drivers/bacnet/points/temp`).

**Gotcha importante**: un BComponent NO puede ser el valor directo de una Property linked. Para referenciar otro componente, usá `BOrd` (que SÍ es BSimple, linkable). El BOrd se resuelve en tiempo de lectura con `resolve()`.

### 4.2.5 Tipos atómicos del framework

#### Numéricos (BNumber)
| Tipo | Almacena | Factory |
|------|----------|---------|
| `BInteger` | int (32-bit) | `BInteger.make(int)` |
| `BLong` | long (64-bit) | `BLong.make(long)` |
| `BFloat` | float (32-bit IEEE) | `BFloat.make(float)` |
| `BDouble` | double (64-bit IEEE) | `BDouble.make(double)` |

#### Texto & data
| Tipo | Almacena | Factory |
|------|----------|---------|
| `BString` | String UTF-8 | `BString.make(String)` |
| `BBlob` | byte[] raw | `BBlob.make(byte[])` |
| `BOrd` | String (path qualified) | `BOrd.make(String)` |
| `BFacets` | Map<String,BValue> | `BFacets.make(key[], val[])` |

#### Booleano & enumeración
| Tipo | Almacena | Factory |
|------|----------|---------|
| `BBoolean` | boolean | `BBoolean.TRUE` / `FALSE` |
| `BEnum` (abstract) | int ordinal | — |
| `BFrozenEnum` (abstract) | int ordinal + class metadata | Subclases definen rango |
| `BDynamicEnum` | int ordinal + BEnumRange | `BDynamicEnum.make(ordinal, range)` |

#### Tiempo & duración
| Tipo | Almacena | Factory |
|------|----------|---------|
| `BAbsTime` | long ms + BTimeZone | `BAbsTime.now()`, `BAbsTime.make(y, m, d, ...)` |
| `BRelTime` | long ms (sin zona) | `BRelTime.DAY`, `BRelTime.makeHours(int)` |
| `BDate` | long ms (solo fecha) | `BDate.make(year, month, day)` |
| `BTime` | long ms (solo hora) | `BTime.make(hour, minute, second)` |
| `BMonth` (BFrozenEnum) | int (0-11) | `BMonth.april` |
| `BWeekday` (BFrozenEnum) | int (0-6) | `BWeekday.monday` |

#### Status (wrapper con bits de estado)
| Tipo | Almacena | Uso |
|------|----------|-----|
| `BStatusNumeric` | double value + int status_bits | Punto analógico con estado (ej. temp=23.5°C con ALARM bit) |
| `BStatusBoolean` | boolean value + int status_bits | Punto booleano con estado |
| `BStatusString` | String value + int status_bits | Punto string con estado |
| `BStatusEnum` | int ordinal + int status_bits | Enum con estado |
| `BStatus` (BBitString) | int bits (6 flags) | Standalone: ok, disabled, fault, down, alarm, stale, null |

#### Especiales
| Tipo | Almacena | Rol |
|------|----------|-----|
| `BBitString` | int (32-bit) | Máscara de bits genérica |
| `BMarker` | void | Singleton sin datos (ej. `BMarker.DEFAULT`) |
| `BIcon` | String path | Referencia a ícono |

### 4.2.6 Enums Baja (BFrozenEnum)

**Ubicación**: `javax.baja.sys.BFrozenEnum` (abstract extends BEnum extends BSimple).

**Concepto**: conjunto fijo de pares `(int ordinal, String tag)` definidos en compile-time.

**Estructura interna**:
- **Ordinal** (int): 0, 1, 2, …
- **Tag** (String): `"horizontal"`, `"vertical"`, …
- **Range** (BEnumRange): lista de todas las instancias válidas. Centralizada en el TypeInfo.

**Definición típica**:
```java
public final class BOrientation extends BFrozenEnum {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final BOrientation horizontal = new BOrientation(HORIZONTAL);
    public static final BOrientation vertical = new BOrientation(VERTICAL);

    public static BOrientation make(int ordinal) {
        return (BOrientation) horizontal.getRange().get(ordinal);
    }
    public static BOrientation make(String tag) {
        return (BOrientation) horizontal.getRange().get(tag);
    }

    private BOrientation(int ordinal) { super(ordinal); }

    public Type getType() { return TYPE; }
    public static final Type TYPE = Sys.loadType(BOrientation.class);
}
```

**Uso**:
```java
BOrientation o1 = BOrientation.horizontal;
BOrientation o2 = BOrientation.make(0);
BOrientation o3 = BOrientation.make("horizontal");

int ord = o1.getOrdinal();
String tag = o1.getTag();
BEnumRange range = o1.getRange();
```

**Diferencia con enum Java puro**:

| Aspecto | BFrozenEnum | `enum` Java |
|---------|-------------|-------------|
| Herencia | Extiende BFrozenEnum (BSimple) | Implícito `extends Enum` |
| Serialización | Binario/texto vía encode/decode. Compatible con FOX, XML, REST | Java Serialization |
| Interoperabilidad | BajaScript, XML, REST services | Solo Java |
| Rango dinámico | Range en Type metadata (introspectable) | Via reflection |
| `values()` | `getRange().values()` | `MyEnum.values()` |
| Comparación | `equals()` chequea clase + ordinal | Identidad (singleton) |

### 4.2.7 Reglas de implementación para tipos nuevos

**BSimple nuevo**:
1. Extend `BSimple`, declarar `final`.
2. Implement `encode(DataOutput)`, `decode(DataInput)`, `encodeToString()`, `decodeFromString(String)`.
3. Implement `equals(Object)` por valor + `hashCode()`.
4. Declarar `public static final DEFAULT = ...`.
5. Factory `make()` con constructor privado.
6. `static final Type TYPE = Sys.loadType(YourClass.class)` + override `getType()`.

**BStruct nuevo**:
1. Extend `BStruct`.
2. Declarar frozen Properties con `newProperty(flags, default)`.
3. Getter/setter por Property.
4. Constructor público sin-args.
5. Type boilerplate.

**BComponent nuevo**:
1. Extend `BComponent`.
2. Declarar frozen Properties, Actions (con `do*`), Topics.
3. Constructor público sin-args.
4. Type boilerplate.
5. Anotaciones `@NiagaraProperty`, `@NiagaraAction`, `@NiagaraTopic` para slot-o-matic.

**BFrozenEnum nuevo**:
1. Extend `BFrozenEnum`, declarar `final`.
2. Constantes `static final int ORD_NAME = ...`.
3. Instancias `static final YourEnum name = new YourEnum(ORD_NAME)`.
4. Constructor privado `super(ordinal)`.
5. Factory `make(int)` y `make(String)`.
6. Type boilerplate.

---

## 4.3 Ciclo de vida, Facets y Slots dinámicos

### 4.3.1 Ciclo de vida del BComponent

**Fases discernibles**:

1. **`loaded()`** — componente deserializado del BOG, aún NO integrado al árbol. Sin acceso a parent/space.
2. **`mounted()`** — entra al árbol del station. Parent y space establecidos. `isMounted()` ⇒ true.
3. **`started()`** — station completó arranque, servicios activos, timers pueden registrarse. Subcomponentes ya iniciados recursivamente (salvo flag `NO_RUN`).
4. **`descendantsStarted()`** — callback post-start cuando TODOS los descendientes ya ejecutaron `started()`.
5. **`stationStarted()`** — station completó toda la inicialización. Storage, licensing, networking operacionales.
6. **`atSteadyState()`** — NRE determinó que las transitoriedades se amortiguaron. Última oportunidad pre-operación normal.
7. **`stopped()`** / **`descendantsStopped()`** — shutdown simétrico. Limpiar timers, conexiones, leases.

**Thread safety**: los callbacks se invocan desde el **engine manager thread** (shared pool para async actions y timers). **No son thread-safe por default**. Si el componente expone mutaciones desde otro thread, el código debe sincronizar manualmente.

**Callbacks de eventos (context-driven)**:

| Callback | Invocado por | Rol |
|----------|--------------|-----|
| `changed(Property, Context)` | `set()` post-cambio | Notificar observadores. |
| `added(Property, Context)` | dynamic `add()` | Nuevo dynamic slot insertado. |
| `removed(Property, oldValue, Context)` | dynamic `remove()` | Slot dinámico borrado. |
| `renamed(Property, oldName, Context)` | dynamic `rename()` | Slot renombrado. |
| `reordered(Context)` | dynamic `reorder()` | Orden de dinámicos cambió. |
| `flagsChanged(Slot, Context)` | `setFlags()` | Flags modificados. |
| `facetsChanged(Slot, Context)` | `setFacets()` | Metadata tipada actualizada. |
| `childParented(Property, newChild, Context)` | set BComponent como padre | Nuevo child agregado. |
| `childUnparented(Property, oldChild, Context)` | remove BComponent | Child removido. |
| `knobAdded(Knob, Context)` | motor de relaciones | Binding creado. |
| `knobRemoved(Knob, Context)` | motor de relaciones | Binding destruido. |
| `clockChanged(BRelTime shift)` | reloj del sistema | Cambio horario detectado. |
| `subscribed()` / `unsubscribed()` | suscripción FOX o local | Observer conectó/desconectó. |

**Verificación empírica** (`BComponent.java` decompilado):
- L201: `public void started() throws Exception`
- L204: `public void descendantsStarted() throws Exception`
- L207: `public void stopped() throws Exception`
- L210: `public void descendantsStopped() throws Exception`
- L213: `public void stationStarted() throws Exception`
- L216: `public void atSteadyState() throws Exception`
- L219: `public void clockChanged(BRelTime shift) throws Exception`
- L633: `public void changed(Property, Context)`
- L636–L660: `added`, `removed`, `renamed`, `reordered`, `flagsChanged`, `facetsChanged`, `childParented`, `childUnparented`

### 4.3.2 Facets — metadata tipada

**`BFacets`**: clase final, inmutable. Pares clave-valor (`String` → `BIDataValue`) que describen un slot SIN modificar su tipo.

**Facets comunes**:

| Facet | Tipo | Rol | Ejemplo |
|-------|------|-----|---------|
| `units` | `BUnit` | Unidad de medida (numérico) | `BUnit.CELSIUS`, `BUnit.PERCENT` |
| `precision` | `BInteger` | Decimales significativos (1-7) | `BInteger.make(2)` |
| `min` / `max` | `BNumber` | Rango válido | `BDouble.make(0.0)`, `BDouble.make(100.0)` |
| `range` | `BEnumRange` | Valores enum permitidos | `MyEnum.range` |
| `trueText` / `falseText` | `BString` | Etiquetas boolean UI | `"Activo"` / `"Inactivo"` |
| `fieldWidth` | `BInteger` | Ancho caracteres (UI hint) | `BInteger.make(10)` |
| `multiLine` | `BBoolean` | Textarea para strings largos | `BBoolean.TRUE` |
| `showDate` / `showTime` / `showSeconds` | `BBoolean` | Format hints BAbsTime | `BBoolean.TRUE` |
| `showUnits` | `BBoolean` | Renderizar símbolo unidad | `BBoolean.TRUE` |
| `allowNull` | `BBoolean` | Permitir null en campo | `BBoolean.FALSE` |
| `targetType` | String-encoded | Tipo esperado en ORD fields | `"baja:Numeric"` |
| `realms` | `BString` | Authorization realms | `"admin,operator"` |

**Declaración**:
- **Frozen**: anotación en bytecode (`@NiagaraProperty(facets = ...)`).
- **Dynamic**: al agregar el slot:
  ```java
  component.add("temperature",
    BDouble.make(20.0),
    Flags.OPERATOR,
    BFacets.makeNumeric(BUnit.CELSIUS, 2, 0.0, 100.0),
    Context.commit);
  ```

**Lectura runtime**:
```java
BFacets facets = property.getFacets();
BUnit units = facets.getUnit();
BInteger precision = facets.getPrecision();
BNumber min = facets.getMin();
```

**Factory methods de BFacets**:
- `makeNumeric(BUnit, precision, min, max)` — numeric standard.
- `makeInt(unit, min, max, radix)` — integer con base.
- `makeBoolean(trueText, falseText)` — boolean con etiquetas.
- `makeEnum(BEnumRange)` — enum dropdown.
- `make(key, value)` — facet individual.
- `make(Map)` — múltiples facets.

**Rol en UI**: Workbench usa facets para renderizar slots — spinner numérico si hay min/max, combobox si hay range, textarea si multiLine=true, etc.

### 4.3.3 Slots dinámicos vs frozen

**Frozen**: declarados en código (`@NiagaraProperty`), presentes en TODAS las instancias. Definen la forma de la clase.

**Dynamic**: agregados/removidos en runtime. Varían per-instancia. Permiten extensibilidad sin recompilar.

**API BComponent**:
```java
// Agregar
Property prop = component.add("newSlot",
  BDouble.make(42.0),
  Flags.OPERATOR,
  BFacets.makeNumeric(2),
  Context.commit);

// Remover
component.remove("oldSlot", Context.commit);
component.remove(property, Context.commit);

// Renombrar
component.rename(property, "newName", Context.commit);

// Reordenar
Property[] props = { prop1, prop2 };
component.reorder(props, Context.commit);
```

**`REMOVE_ON_CLONE` específico**: cuando se clona un componente (deep copy para export/import), dynamic slots con este flag se descartan automáticamente. Útil para slots generados dinámicamente que no deben replicarse (result sets temporales, caches).

**Persistencia en BOG**: dynamic slots se serializan nativamente. `ValueDocEncoder` escribe nombre + valor + flags + facets. Al cargar, `LoadOp` recrea con `.add(name, value, flags, facets, Context.commit)`.

**Casos reales en el corpus**:

1. **`BSearchResultSet`** (search-rt): agrega resultados como dinámicos:
   ```java
   myItems.add("result0", copyTaskResult(result), Flags.TRANSIENT | Flags.OPERATOR);
   ```
   → cada resultado es dynamic, TRANSIENT (no persistir), OPERATOR-visible.

2. **`BWebService`** (web-rt): agrega config https conditionally:
   ```java
   Flags.add(this, httpsCert, null, Flags.READONLY | Flags.HIDDEN | Flags.USER_DEFINED_1);
   ```

3. **`LoadOp`** (baja sync): recrea dinámicos durante carga BOG:
   ```java
   Property prop = to.add(name, toValue, fromFlags, fromFacets, Context.commit);
   orderedDynamicProps.add(toSlot.asProperty());
   ```

**Componentes que usan dinámicos intensivamente**:
- `BFolder`, `BServiceContainer` — agregar folders/componentes vía UI.
- Workbench navigator, palettes — items, vistas, config.
- `BPointFolder`, `BDeviceFolder` — points/devices agregados runtime.
- `BControlPoint` — dynamic slots para status, history reference tras init.
- `BSearchResultSet` — cada resultado es dynamic property.

**Thread safety en dinámicos**: `.add()`, `.remove()`, `.rename()` son synchronized en BComponent. Los callbacks (`added`, `removed`, `renamed`) se invocan post-operación. **No re-entrar en `.add()` del mismo componente dentro de un callback** — riesgo de deadlock.

---

## Síntesis del bloque

### Modelo mental consolidado

Niagara es un **framework de componentes auto-descriptivos** construido sobre 3 pilares:

1. **Slots como unidad universal**: todo comportamiento persistible, invocable o notificable de un componente es un slot (Property, Action, Topic). Flags y facets añaden metadata rica SIN tocar el tipo.

2. **Jerarquía tipada estricta**: BObject → BValue → {BSimple, BComplex, BStruct} → BComponent. Cada capa añade capacidades (copia, navegación, ciclo de vida). La decisión "¿qué extiendo para mi tipo nuevo?" está dictada por 3 preguntas: ¿es atómico?, ¿tiene actions/lifecycle?, ¿puede tener children?

3. **Extensibilidad sin recompilar**: dynamic slots + facets permiten que Workbench agregue/edite properties en runtime. BOG los persiste tal cual. Esto es lo que hace que el modelo Niagara sea "WYSIWYG sobre Java" en vez de "Java con un editor".

### Conexiones con bloques 1-3

- **Con Bloque 1 (Estructura)**: el registry (`NRegistry`, `Builder`, `ClassScanner`) indexa los 10,797 `@NiagaraType` del corpus. Los 4,326 `@NiagaraProperty` y 1,499 `@NiagaraAction` que vimos ahí son exactamente las anotaciones documentadas acá.
- **Con Bloque 2 (Licensing)**: features se consumen vía `Sys.getLicenseManager().checkFeature()`. Esa API retorna `Feature`, que internamente es un BComplex con slots tipados (facets). El modelo Baja es la forma en que el licensing se expone al runtime.
- **Con Bloque 3 (Security)**: `BIProtected` en BComponent conecta con el sistema de permisos BAS (users/roles/categories). `@Facet(name="realms", value="admin")` a nivel slot permite restringir acceso granular sin tocar el sandbox JVM.

### Gotchas críticos para tener a mano

1. **BComponent no es linkable directamente** como valor. Para referenciar, usar `BOrd` (BSimple). Se resuelve en lectura con `resolve()`.
2. **`equals()` default en BObject = identidad**. Solo BValue y subclases comparan por valor. Usar `equivalent()` para semántica genérica.
3. **Callbacks lifecycle corren en engine thread compartido** — no son thread-safe si el componente muta estado desde otros threads.
4. **Re-entrar `.add()` dentro de `added()` callback** → deadlock.
5. **Discrepancia docs ↔ código**: la fuente de verdad es `Flags.java` (21 flags), no el devguide (menciona ~15).
6. **`@NiagaraType` vs `@NiagaraProperty|Action|Topic`** las procesan herramientas distintas — la primera en compile-time estándar, las otras por slot-o-matic post-compile.

### Qué habilita este bloque

Con Bloques 1-3 + 4 tenés el mental model suficiente para:
- Leer cualquier `.java` decompilado de un módulo y entender qué hace.
- Diseñar un tipo nuevo y decidir correctamente BSimple / BStruct / BComponent.
- Entender por qué Workbench renderiza un slot de una forma (facets).
- Saber qué callback implementar para reaccionar a un evento.

**Lo que todavía NO podés hacer con solo estos 4 bloques**:
- Navegar componentes por ORD (Bloque 5).
- Persistir en BOG correctamente (Bloque 5).
- Escribir queries BQL/NEQL (Bloque 5).
- Entender cómo se ejecuta un control loop (Bloque 6).
- Implementar un driver (Bloque 7).

**Próximo bloque recomendado**: Bloque 5 — ORD + BOG + BQL/NEQL.

---

## Engram topic keys generados por este bloque

- `niagara/baja/slot-system` — Taxonomía Slot/Property/Action/Topic + 21 flags + SlotCursor + annotations.
- `niagara/baja/type-hierarchy` — BObject → BValue → BSimple/BComplex/BStruct → BComponent + tipos atómicos + BFrozenEnum.
- `niagara/baja/lifecycle-facets-dynamic` — Callbacks lifecycle + BFacets + API dynamic slots + thread safety.

---

**Sesión cerrada**: 2026-04-21 — Bloque 4 consolidado y verificado empíricamente contra corpus decompilado + devguide oficial.
