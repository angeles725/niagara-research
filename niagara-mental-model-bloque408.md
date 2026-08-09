# Block 408 — BComponentSpace Internal Lifecycle: LoadCallbacks, SubscribeCallbacks, TrapCallbacks, AuditableSpace Annotation, and BHandleScheme Resolution

> **Research focus:** `database` (gap **DB7**, medium-priority). Covers the internal lifecycle of
> `BComponentSpace` — the slot-tree container that backs every station/BOG space in Niagara N4. Specifically:
> how `LoadCallbacks`, `SubscribeCallbacks`, and `TrapCallbacks` fire during BOG deserialization and live
> operation; what `@AuditableSpace` adds (runtime audit trail gate); and when `BHandleScheme` is consulted
> for handle-based ORD resolution.
>
> **Not covered here (remittance):**
> - `BComponentSpace.modified()` no-op and the `BBogSpace` dirty-flag path → **[Block 402]** §402.1-402.2
> - Station `config.bog` save trigger (BStationSaveJob) → **[Block 402]** §402.3-402.5
> - ORD / space scheme enumeration (slot:/station:/h: overview) → **[Block 5]** §5.1
>
> Subject version: N4.14.0.162 — baja.jar docSource (original Tridium vendor source) +
> Vineflower decompiled baja.jar (complementary bodies).
>
> Sources:
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/BComponentSpace.java` (docSource — original vendor source)
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/LoadCallbacks.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/SubscribeCallbacks.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/TrapCallbacks.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/AuditableSpace.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/BHandleScheme.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/BSpace.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/Mark.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/BISpace.java`
> - `[CERT]` `docSource/docSource-doc/extracted/baja/javax/baja/space/BSpaceScheme.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/schema/ComponentSlotMap.java`
> - `[CERT]` `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/schema/ComplexSlotMap.java`
>
> All cited file:line verified against actual source. Prefix `docSource/` = `/home/cristian/modules/Prototipos/modulos/organized/docSource/docSource-doc/extracted/baja/javax/baja/space/`.
>
> Method: docSource original vendor source (primary) + Vineflower decompiled bodies (call-chain verification).
> Markers used:
> `[CERT]` local primary source (`file:line`) ·
> `[INFER]` researcher deduction.
>
> `database` focus. Connects [Block 402] (save trigger, `modified()` no-op, `BBogSpace` dirty flag),
> [Block 5] (ORD / space scheme enumeration), [Block 4] (BComponent slot lifecycle context).

---

## 408.1 — Class Hierarchy: BSpace → BComponentSpace `[CERT]`

`BComponentSpace` is the concrete slot-tree space used for the station (`config.bog`), platform.bog, and
any other space backed by a component tree.

| Class / Interface | Kind | Supertype | Implements | Key role |
|---|---|---|---|---|
| `BISpace` | interface | — | `BInterface` | Mounted-space contract: host/session/ORD queries |
| `BSpace` | abstract class | `BNavContainer` | `BIAgent`, `BISpace` | Navigation + host/session plumbing |
| `BComponentSpace` | concrete class | `BSpace` | `BIProtected`, `BIPropertySpace`, `BIDataRecoverySource`, `BIEntitySpace` | Handle-map + callback container |

`[CERT]` `BISpace.java:23-24` (interface declaration)
`[CERT]` `BSpace.java:27-30` (abstract class extends `BNavContainer`, implements `BIAgent`, `BISpace`)
`[CERT]` `BComponentSpace.java:81-84` (concrete class declaration)

`BComponentSpace` is annotated `@AuditableSpace` at its type declaration (see §408.5).
`[CERT]` `BComponentSpace.java:80`

### 408.1.1 — Core Fields

```java
// BComponentSpace.java:1742-1762
Map<Object, BComponent> map = new ConcurrentHashMap<>();  // handle → component lookup
long nextHandle = 1;

LoadCallbacks     loadCallbacks     = new LoadCallbacks();      // default: no-op
TrapCallbacks     trapCallbacks     = new TrapCallbacks();      // default: disabled
SubscribeCallbacks subscribeCallbacks = new SubscribeCallbacks(); // default: no-op
BComponent root;
BOrd ordInSession;
Type[] mixIns = new Type[0];
boolean holdMixInUpdates;
private BDataRecoveryComponentRecorder dataRecoveryRestorer = null;
```

`[CERT]` `BComponentSpace.java:1742-1762`

All three callback objects are initialized eagerly as default (no-op / disabled) instances.
Callers replace them with live implementations via the setters: `setLoadCallbacks()`,
`setTrapCallbacks()`, `setSubscribeCallbacks()`. `[CERT]` `BComponentSpace.java:400-435`

---

## 408.2 — Handle Lifecycle: mount() and BHandleScheme `[CERT]`

### 408.2.1 — Handle Assignment During Deserialization

During BOG deserialization, `ComponentSlotMap.mount(space, context, listener)` drives the mounting
of each component into its space. The handle assignment logic is in `BComponentSpace.mount()`:

```
BComponentSpace.mount(ComponentSlotMap support):
  1. solveForNextHandle(comp, nextHandle)    → advances nextHandle past any existing handle
  2. if handle == null OR starts with SWIZZLE_PREFIX:
       handle = generateHandle()             → Long.toHexString(nextHandle++)
  3. duplicate check (with data-recovery reassignment logic)
  4. map.put(handle, comp)                   → enters the handle→component lookup map
  5. updateMixIns(comp)                      → adds enabled MixIn slots if applicable
```

`[CERT]` `BComponentSpace.java:1365-1480` (`mount()` method body)
`[CERT]` `BComponentSpace.java:1517` (`generateHandle()` returns `Long.toHexString(nextHandle++)`)

Handles in a serialized BOG are hex strings (e.g., `"abc123"`). When a BOG is loaded, each
component's handle is already set in the BOG; `solveForNextHandle()` ensures `nextHandle` advances
past the maximum existing handle so new components get unique IDs.
`[CERT]` `BComponentSpace.java:1598-1620` (`solveForNextHandle()` body)

### 408.2.2 — BHandleScheme: "h:" ORD Scheme

`BHandleScheme` is the ORD scheme for handle-based lookup. It is registered with scheme id `"h"` via
the `@NiagaraType(ordScheme = "h")` annotation and is a `@NiagaraSingleton`.

```java
// BHandleScheme.java:21-23
@NiagaraType(ordScheme = "h")
@NiagaraSingleton
public class BHandleScheme extends BOrdScheme { ... }
```

`[CERT]` `BHandleScheme.java:21-33` (class declaration and INSTANCE field)

`BHandleScheme.resolve()` is consulted whenever an ORD containing a `h:` query is resolved
(e.g., `station:|h:abc123` as shown in [Block 5] §5.1.2):

```java
// BHandleScheme.java:66-87
public OrdTarget resolve(OrdTarget base, OrdQuery query) {
    BObject baseObject = base.get();
    BComponentSpace space = null;
    String handle = query.getBody();
    if (baseObject instanceof BComponentSpace)
        space = (BComponentSpace)baseObject;
    else if (baseObject instanceof BComponent)
        space = baseObject.asComponent().getComponentSpace();
    if (space == null)
        throw new InvalidOrdBaseException("Not based via ComponentSpace");
    return new OrdTarget(base, space.resolveByHandle(handle));  // → map.get(handle)
}
```

`[CERT]` `BHandleScheme.java:66-87`

**When consulted:** only at ORD resolution time (live operation) — NOT during BOG deserialization
itself. Handles are written into the BOG XML by the serializer and read back directly. The `h:` scheme
is the user-facing mechanism to navigate to a component whose slot path is unknown or may change.
`[INFER]` — BOG deserialization reads handle attributes directly; `BHandleScheme.resolve()` is the
runtime lookup path, not the load path.

### 408.2.3 — unmount()

`BComponentSpace.unmount()` removes the component from the map when a component is removed from the
space (e.g., component delete or station shutdown):

```java
// BComponentSpace.java:1482-1498
map.remove(c.getHandle());
// handle is intentionally left set on the ComponentSlotMap (for move operations)
```

`[CERT]` `BComponentSpace.java:1482-1498`

---

## 408.3 — LoadCallbacks: Lazy-Load Trigger During BOG Mounting `[CERT]`

`LoadCallbacks` provides hooks for **lazy loading** — deferring the population of dynamic slots
until they are first accessed, rather than loading them all at mount time.

### 408.3.1 — API Summary

| Method | Default | Called when |
|---|---|---|
| `isLazyLoad()` | `false` | Checked during every mount to decide whether to recurse into children |
| `loadSlots(BComponent c)` | no-op (routes to VirtualGateway if virtual) | First time `BComplex.loadSlots()` is called for a component |
| `loadSlot(BComponent c, String slotName)` | `null` (fall back to `loadSlots()`) | During slot path resolution in `BSlotScheme` |
| `newCopy(BValue[] values, CopyHints hints)` | `null` | `BValue.newCopy()` — deep-clone operation |

`[CERT]` `LoadCallbacks.java:19-98` (complete class body)

### 408.3.2 — BOG Mounting Lifecycle

`ComponentSlotMap.mount(space, context, listener)` is the entry point during BOG deserialization.
The lazy-load guard is at line 1587:

```java
// ComponentSlotMap.java:1587-1602
if (!space.getLoadCallbacks().isLazyLoad() || this.isBrokerPropsLoaded()) {
    SlotCursor<Property> c = this.getProperties();
    for (int i = 0; c.nextComponent(); i++) {
        getComponentSlotMap(c.get()).mount(space, context, listener);  // recurse
    }
}
```

`[CERT]` `ComponentSlotMap.java:1587-1602`

- If `isLazyLoad() == false` (default for standard `BComponentSpace`): children are mounted **eagerly**
  — the entire component tree is recursively mounted during BOG load.
- If `isLazyLoad() == true` (used by virtual/proxy spaces): children are **NOT** mounted at BOG load;
  they are deferred until the first slot access.

### 408.3.3 — First Slot Access Trigger

When any slot is accessed on a component in a lazy space (e.g., `contains()`, `getSlot()`),
`ComponentSlotMap.loadSlots()` fires:

```java
// ComponentSlotMap.java:484-491
public void loadSlots() {
    if (this.space != null) {
        if (!this.isBrokerPropsLoaded()) {
            this.space.getLoadCallbacks().loadSlots((BComponent)this.instance);
            this.setBrokerPropsLoaded(true);
        }
    }
}
```

`[CERT]` `ComponentSlotMap.java:484-491`

The `brokerPropsLoaded` flag prevents re-invocation: `loadSlots()` fires **exactly once** per
component instance. `[CERT]` `ComponentSlotMap.java:486-488`

For individual slot path resolution, `BSlotScheme` calls `loadSlot(c, slotName)` first; if it
returns `null`, it falls back to `loadSlots()`:
`[CERT]` `BSlotScheme.java:142-152` (vineflower)

### 408.3.4 — newCopy Trap

`ComponentSlotMap.newCopy()` (used for deep-clone operations during copy/paste) consults
`LoadCallbacks.newCopy()` to give the space a chance to implement its own cloning:

```java
// ComponentSlotMap.java:212
temp = this.space.getLoadCallbacks().newCopy(temp, hints);
```

`[CERT]` `ComponentSlotMap.java:212`

If `newCopy()` returns `null`, the standard Baja copy mechanism is used. This hook lets proxy
spaces substitute remote copies of values rather than locally deep-cloning them. `[INFER]`

---

## 408.4 — SubscribeCallbacks: Live Subscription Events `[CERT]`

`SubscribeCallbacks` hooks the transition of components into and out of the **subscribed** state
(when a remote client or internal consumer starts actively reading a component's values).

### 408.4.1 — API Summary

| Method | Default | Called when |
|---|---|---|
| `subscribe(BComponent[] c, int depth)` | no-op | Component enters subscribed state (subscriber count: 0 → 1) |
| `unsubscribe(BComponent[] c)` | no-op | Component leaves subscribed state (subscriber count: 1 → 0) |
| `update(BComponent c, int depth)` | no-op | One-time snapshot request (not ongoing subscription) |

`[CERT]` `SubscribeCallbacks.java:18-73` (complete class body)

`depth` semantics: 0 = component only; 1 = include children; 2 = grandchildren, etc. The space
is responsible for handling already-subscribed components in the subtree. `[CERT]` `SubscribeCallbacks.java:26-36`

### 408.4.2 — Dispatch Point (ComponentSlotMap)

`ComponentSlotMap.subscribe(boolean callSpace)` fires `SubscribeCallbacks.subscribe()` when a
component transitions from unsubscribed to subscribed:

```java
// ComponentSlotMap.java:657-673
public final void subscribe(boolean callSpace) {
    if (callSpace && this.space != null) {
        this.space.getSubscribeCallbacks()
            .subscribe(new BComponent[]{(BComponent)this.instance}, 0);
    }
    // then fires direct callback: component.subscribed()
    this.instance.fw(17, null, null, null, null);
    if (this.space == null || this.space.fireDirectCallbacks())
        ((BComponent)this.instance).subscribed();
}
```

`[CERT]` `ComponentSlotMap.java:657-673`

Similarly, `unsubscribe(boolean callSpace)` fires `SubscribeCallbacks.unsubscribe()`:
`[CERT]` `ComponentSlotMap.java:676-693`

**Order of events on subscribe:**
1. `SubscribeCallbacks.subscribe()` — space-level hook (COV registration, polling setup)
2. `fw(17, ...)` — internal framework event
3. `BComponent.subscribed()` — direct component callback (if `fireDirectCallbacks()` is true)

**What this hook is for:** demand-based COV registration and polling in driver spaces (e.g., a
BACnet driver space that subscribes/unsubscribes COV registrations with the real device based on
whether any Workbench client is watching the component). `[CERT]` `SubscribeCallbacks.java:26-36`

### 408.4.3 — update() Convenience

`BComponentSpace.update(BComponent c, int depth)` is a convenience wrapper:
```java
// BComponentSpace.java:445-448
public final void update(BComponent c, int depth) {
    getSubscribeCallbacks().update(c, depth);
}
```
`[CERT]` `BComponentSpace.java:445-448`

This is for one-time snapshots — an explicit refresh request that does not create an ongoing
subscription. `[CERT]` `SubscribeCallbacks.java:65-72`

---

## 408.5 — TrapCallbacks: Change Interception `[CERT]`

`TrapCallbacks` intercepts all component modifications before they reach the actual slot map storage.
Its primary use case is proxy component spaces that must redirect changes to a remote master space.

### 408.5.1 — API Summary

| Method | Default | When invoked |
|---|---|---|
| `isTrapEnabled()` | `false` | Gate: checked before every modification |
| `trapSets()` | `false` | Additional gate: whether `set()` is trapped (performance opt) |
| `set(c, propertyPath, value, context)` | throws UOE | Property value changed (`BComplex.set()`) |
| `add(c, name, value, flags, facets, context)` | throws UOE | Dynamic slot added |
| `remove(c, prop, context)` | throws UOE | Dynamic slot removed |
| `rename(c, prop, newName, context)` | throws UOE | Slot renamed |
| `reorder(c, order, context)` | throws UOE | Slots reordered |
| `setFlags(c, slot, flags, context)` | throws UOE | Slot flags changed |
| `setFacets(c, slot, facets, context)` | throws UOE | Slot facets changed |
| `setCategoryMask(c, mask, context)` | throws UOE | Category mask changed |
| `invoke(c, action, arg, context)` | throws UOE | Action invoked |

`[CERT]` `TrapCallbacks.java:26-130` (complete class body)

### 408.5.2 — Dispatch Pattern

Every component modification in `ComponentSlotMap` checks the same guard before proceeding:

```java
// ComponentSlotMap.java:421-424 (example: setFlags)
TrapCallbacks trap = this.getSpaceTrap();
if (trap != null && trap.isTrapEnabled() && context != Context.commit) {
    trap.setFlags(instance, slot, flags, context);
    // → trap handles the change, normal path skipped
} else {
    // normal slot modification proceeds
}
```

`[CERT]` `ComponentSlotMap.java:421-424` (setFlags dispatch)
`[CERT]` `ComponentSlotMap.java:1319` (add dispatch)
`[CERT]` `ComponentSlotMap.java:1715` (remove dispatch)
`[CERT]` `ComponentSlotMap.java:1826` (rename dispatch)
`[CERT]` `ComponentSlotMap.java:1927` (reorder dispatch)

`set()` trapping is controlled by the additional `trapSets()` gate (both `isTrapEnabled() && trapSets()` must be true):
`[CERT]` `ComplexSlotMap.java:504` (set dispatch with trapSets check)

### 408.5.3 — Bypass via Context.commit

The javadoc is explicit: `"To commit a change and bypass trap callbacks use Context.commit"`.
`[CERT]` `TrapCallbacks.java:18-20` (class javadoc)
`[CERT]` `ComponentSlotMap.java:422` (`context != Context.commit` exclusion in the guard)

This is the mechanism by which the space itself (or a trusted internal path) can write directly
to the slot map without triggering proxy redirection — e.g., when a proxy space receives an
update from the master and needs to commit it locally without re-sending it upstream. `[INFER]`

### 408.5.4 — Default: Everything Disabled

With `isTrapEnabled() == false` (the default), the entire trap mechanism is short-circuited.
All trap callback methods throw `UnsupportedOperationException` if reached without overriding
them — they are abstract in spirit even though the class is concrete. `[CERT]` `TrapCallbacks.java:36-39,62-64`

---

## 408.6 — @AuditableSpace: Runtime Annotation for audit() Gate `[CERT]`

`AuditableSpace` is a **Java runtime annotation** (`@interface`), NOT a class or interface.
It was introduced in Niagara 4.12.

```java
// AuditableSpace.java:1-29
@Retention(RetentionPolicy.RUNTIME)   // survives class loading; checked via reflection
@Target(ElementType.TYPE)             // applies to class/type declarations
public @interface AuditableSpace { }
```

`[CERT]` `AuditableSpace.java:25-29`

`BComponentSpace` itself carries this annotation at its type declaration:
```java
// BComponentSpace.java:79-84
@AuditableSpace
public class BComponentSpace extends BSpace implements BIProtected, ...
```
`[CERT]` `BComponentSpace.java:79-84`

### 408.6.1 — How ComplexSlotMap Uses the Annotation

`ComplexSlotMap.audit()` is called on every property change when a user (`BUser`) context is present.
The annotation check is the gate that decides whether the `Nre.auditor` is invoked:

```java
// ComplexSlotMap.java:1679-1695
public final void audit(OrdQuery targetPath, BUser user, String op, String slotName,
                         String oldValue, String value) {
    BComponentSpace componentSpace = this.getSpace();
    if (componentSpace != null) {
        if (componentSpace.getClass().isAnnotationPresent(AuditableSpace.class)) {
            Auditor auditor = Nre.auditor;
            if (auditor != null && targetPath != null) {
                auditor.audit(new AuditEvent(op, targetPath.getBody(),
                               slotName, oldValue, value, user.getUsername()));
            }
        } else if (log.isLoggable(Level.FINE)) {
            log.fine("Complex slot map auditing was disallowed for Space: " + ...);
        }
    }
}
```

`[CERT]` `ComplexSlotMap.java:1679-1695`

**Effect:** A `BComponentSpace` subclass that does NOT carry `@AuditableSpace` will have its
`audit()` calls silently suppressed (logged at FINE level only). Since `BComponentSpace` itself
carries the annotation, all its standard subclasses (including the station space) are auditable
by default. A custom space that extends a non-annotated superclass and forgets to add the
annotation will silently lose audit events — the FINE log is the only signal.
`[CERT]` `ComplexSlotMap.java:1695-1703` (else branch with FINE log)

### 408.6.2 — What "Auditing" Covers

The `AuditEvent` captures: `op` (e.g., "Changed"), `targetPath`, `slotName`, `oldValue`,
`newValue`, and `username`. `[CERT]` `ComplexSlotMap.java:1687` (AuditEvent constructor arguments)

Slots marked with `Flags.NO_AUDIT` are excluded from auditing even when the space is auditable:
`[CERT]` `ComplexSlotMap.java:1659` (`if (!Flags.isNoAudit(component, path[0]))`)

---

## 408.7 — BSpaceScheme and Mark (Supporting Types) `[CERT]`

### 408.7.1 — BSpaceScheme

`BSpaceScheme` (abstract) is the base for space ORD schemes that have different `BSpace`
implementations per session (e.g., `station:` scheme). Its resolution algorithm:

1. Maps the ORD base to a `BISession` via `toSession()`
2. Gets or creates a `BSpace` cached as a nav child on the session (keyed by scheme id)
3. Routes to the subclass `resolve(base, query, space)` hook

`[CERT]` `BSpaceScheme.java:83-111` (resolve() body)

`BHandleScheme` extends `BOrdScheme` directly (not `BSpaceScheme`) because it does not create
a space — it resolves within an existing space. `[CERT]` `BHandleScheme.java:25` (extends BOrdScheme)

### 408.7.2 — Mark (VM Clipboard)

`Mark` is the VM-wide clipboard (cut/copy source) for space operations (copy, move, delete).
There is exactly one current `Mark` at a time (`Mark.current` static field).

`[CERT]` `Mark.java:29-43` (class header and `getCurrent()`)

`BComponentSpace.fw()` routes `Fw.MAKE_DELETE_OP` and `Fw.DELETE_PROPS` via `Mark`:
`[CERT]` `BComponentSpace.java:1341-1344` (fw() switch cases)

`Mark` is NOT part of the callback lifecycle (LoadCallbacks/SubscribeCallbacks/TrapCallbacks);
it operates at a higher level (user-initiated cut/copy operations in Workbench). `[INFER]`

---

## 408.8 — Complete Lifecycle Summary `[CERT]` `[INFER]`

```
BOG DESERIALIZATION (config.bog → station space):
  Station.java:201 → new BComponentSpace("station", ...)   [plain BComponentSpace, no dirty flag]
  ComponentSlotMap.mount(space, context, listener):
    if (!isLazyLoad() || brokerPropsLoaded):                [default: eager]
      for each child component:
        child.ComponentSlotMap.mount(space, ...)            [recurse]
          → BComponentSpace.mount(childSlotMap):
              solveForNextHandle()                          [advance nextHandle]
              if handle == null → generateHandle()          [hex handle]
              map.put(handle, comp)                         [enter lookup map]
              updateMixIns(comp)                            [add MixIn slots]

  LoadCallbacks.loadSlots() fires: NEVER during eager mount [INFER]
    → only fires on FIRST SLOT ACCESS via ComponentSlotMap.loadSlots() [CERT :484-491]
    → guarded by brokerPropsLoaded flag (fires once per component)

LIVE OPERATION (after station started):
  Component modification (set/add/remove/rename/reorder/setFlags/invoke):
    TrapCallbacks guard: if (isTrapEnabled() && context != Context.commit):
      → trap.set() / trap.add() / ... [intercepts change]
    else: normal slot write proceeds
    [Flags.NO_AUDIT excluded]
    → ComplexSlotMap.audit() [if user context present]
        → AuditableSpace annotation check
        → Nre.auditor.audit(AuditEvent)

  Component enters subscribed state (subscriber count 0→1):
    ComponentSlotMap.subscribe(true):
      → SubscribeCallbacks.subscribe([component], 0)       [COV registration hook]
      → component.subscribed()                             [direct callback]

  Component leaves subscribed state (subscriber count 1→0):
    ComponentSlotMap.unsubscribe(true):
      → SubscribeCallbacks.unsubscribe([component])        [COV cleanup hook]
      → component.unsubscribed()                           [direct callback]

  One-time snapshot request:
    BComponentSpace.update(c, depth):
      → SubscribeCallbacks.update(c, depth)                [no ongoing subscription]

  Handle-based ORD resolution (e.g., station:|h:abc123):
    BHandleScheme.resolve():
      → space.resolveByHandle(handle)                      [map.get(handle)]
```

`[CERT]` `BComponentSpace.java:1365-1480,1517-1518` (mount / generateHandle)
`[CERT]` `ComponentSlotMap.java:484-491,657-693,1587-1602` (loadSlots / subscribe / mount)
`[CERT]` `BHandleScheme.java:66-87` (resolve)
`[INFER]` — LoadCallbacks.loadSlots() does not fire during eager mount; inferred from the guard at
ComponentSlotMap.java:1587 (eager path skips loadSlots()) and the once-per-component guard at :486.

---

## 408.9 — Connections

- **[Block 402]** — documents the `BBogSpace.modified()` dirty-flag path and the station
  `config.bog` save trigger. Block 402 §402.1 established that the station space is a plain
  `BComponentSpace` and that `modified()` is a no-op. Block 408 completes the picture by
  documenting what BComponentSpace DOES do during load/subscribe/trap — the lifecycle that
  B402 left as out of scope. Corrects no claim in B402; confirms `modified()` no-op at
  `BComponentSpace.java:489`.

- **[Block 5]** — §5.1.2 lists `h:` as one of the 29 ORD schemes. Block 408 documents the
  `BHandleScheme` implementation that backs that scheme: how handles are assigned during mount
  and how they are resolved at runtime via `map.get(handle)`.

- **[Block 4]** — documents the BComponent slot lifecycle (fw(), BSlot, property types,
  `@NiagaraProperty` annotation). Block 408 sits above that layer: BComponentSpace is the
  container that owns the handle map and callback dispatch, while Block 4's BComponent is the
  node within that container.

- **[Block 403]** — documents the RDBMS export pipeline. That pipeline reads from a
  `BComponentSpace`-backed history space via `BHistoryService.getDatabase().getConnection()`.
  Block 408 clarifies the space layer those calls operate on.
