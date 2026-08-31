# How to Build the ColdRoomPan Niagara N4 Module — End to End

> A complete walkthrough of how the `ColdRoomPan` module was built, from an empty
> project to a **signed, trusted, and running** module on a JACE. `ColdRoomPan` is
> the worked example throughout: a compiled `-rt` module that defines three reusable
> component **types** for four independent cold rooms.
>
> This guide is the "how we actually did it" narrative. It links to the reference
> docs rather than repeating them:
> - Project creation, click-by-click with screenshots → [`docs/manuals/new-module-creation/main.tex`](manuals/new-module-creation/main.tex) (PDF: `main.pdf`)
> - Codegen / tool mechanics → [`docs/module-dev-workflow.md`](module-dev-workflow.md)
> - The control design (requirements, logic, I/O) → [`docs/cold-room-module-design.md`](cold-room-module-design.md)
> - `module-permissions.xml` explained → [`niagara-mental-model-bloque721.md`](../niagara-mental-model-bloque721.md)
> - The WSL2 build loop → [`niagara-mental-model-bloque722.md`](../niagara-mental-model-bloque722.md)
> - Self-signing + JACE trust → [`niagara-mental-model-bloque723.md`](../niagara-mental-model-bloque723.md)

---

## 1. Overview

`ColdRoomPan` controls **four independent cold rooms** with a single reusable module.
Instead of one component per room, it defines **three component TYPES** that are
instantiated and nested as needed:

| Type (`B*` class) | Role | Design ref |
|---|---|---|
| `BColdRoom` | "equip" container — one per physical room. Holds setpoint/differential, zone sensors, and the control logic. | design §3.1 |
| `BEvaporatorUnit` | One evaporator (fan/compressor) + its solenoid valve (+ defrost resistance on Room 3). | design §3.2 |
| `BDefrostController` | Room 3 only. Coordinates defrost across that room's units with an interlock. | design §3.3 / §5 |

Two frozen enums back the configurable slots: `BStagingMode` (`single` / `staged`)
and `BDefrostMode` (`interval` / `schedule`).

**Toolchain**
- **Workbench** — creates the module skeleton, builds the certificate chain, signs, deploys.
- **IntelliJ IDEA** — edits Java, SDK 1.8, runs Gradle tasks.
- **WSL2 (Ubuntu, ext4)** — the actual build host (see §4).
- **Java 8** — Niagara builds require a full JDK 8.
- **gradle-niagara plugins 7.6.x** — `niagara-module`, `niagara-signing`, `bajadoc`, annotation processors.

**End state:** a `ColdRoomPan-rt.jar` signed with a code-signing certificate whose CA
the JACE trusts, installed and running in the station.

**Field values used for this module**

| Field | Value |
|---|---|
| Module name | `ColdRoomPan` |
| Preferred symbol | `CRP` |
| Vendor | `Angeles` |
| Java package | `com.angeles.ColdRoomPan` |
| Runtime profile | `rt` (runtime only) |
| Module version | `1.0` (`defaultModuleVersion` in root `build.gradle.kts`) |

---

## 2. Create the project

Use the Workbench wizard, then move to IntelliJ. Full click-by-click with screenshots
is in the manual — [`main.tex` Part A/B/C](manuals/new-module-creation/main.tex).

1. **Workbench → `Tools → New Module`.** Set module name `ColdRoomPan`, symbol `CRP`,
   vendor `Angeles`. Choose the **RUNTIME-only** profile (`rt`) and check **Create
   Palette** so the module ships a `module.palette`. (Manual: *Step 1 of 3 — module
   metadata and runtime profiles*. Note the gotcha: you **cannot go Back to Step 1**.)
2. **Open the generated folder in IntelliJ IDEA** and trust the project.
3. **`File → Project Structure`** → set **SDK to 1.8** and **language level to 8**.
4. **Edit `gradle.properties`** — point `niagara_home` at the SDK install and set the
   JDK-8 toolchain path. (Manual: *Part C — Configure the project*.)

The wizard produces a **nested** tree. The Gradle **root** (with `gradlew`,
`settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`) sits one level
above an inner `ColdRoomPan/` folder that holds the runtime **part** `ColdRoomPan-rt/`:

```
<root>/                         # rootProject.name = "ColdRoomPan"
├── gradlew  gradlew.bat
├── settings.gradle.kts         # plugin versions; findProjects()
├── build.gradle.kts            # vendor { Angeles, 1.0 }; signing plugin
├── gradle.properties           # niagara_home, JDK-8 path
└── ColdRoomPan/
    ├── niagara-module.xml       # moduleName + preferredSymbol="CRP"
    └── ColdRoomPan-rt/
        ├── ColdRoomPan-rt.gradle.kts   # moduleManifest { name, rt }, deps
        ├── module-include.xml          # <type> registry
        ├── module.palette              # palette contents
        ├── module.lexicon              # display names
        ├── module-permissions.xml      # security-manager requests (empty here)
        └── src/com/angeles/ColdRoomPan/*.java
```

`settings.gradle.kts` discovers parts via `findProjects()` — any `*-rt/*-rt.gradle.kts`
becomes a Gradle subproject. The root `build.gradle.kts` sets the vendor and version:

```kotlin
vendor {
  defaultVendor("Angeles")
  defaultModuleVersion("1.0")
}
```

---

## 3. Author the component code

This is the core of "how we made it." Every exported type is a `BComponent` subclass
that declares its slots with annotations; **Slot-o-Matic** then generates the
boilerplate, and hand-written control logic goes below the generated region.

### 3.1 The `@NiagaraType` pattern

Each class carries a **class-level** `@NiagaraType`, then one `@NiagaraProperty` per
slot and one `@NiagaraAction` per action. Real example from `BColdRoom.java`:

```java
@NiagaraType
@NiagaraProperty(
  name = "differentialUp",
  type = "double",
  defaultValue = "1d",
  facets = @Facet("BFacets.make(BFacets.MIN, BDouble.make(0d))")
)
@NiagaraProperty(
  name = "stagingMode",
  type = "BStagingMode",
  defaultValue = "BStagingMode.single"
)
@NiagaraProperty(
  name = "cooling",
  type = "BStatusBoolean",
  defaultValue = "new BStatusBoolean(false)",
  flags = Flags.TRANSIENT | Flags.SUMMARY | Flags.READONLY
)
public class BColdRoom extends BComponent { ... }
```

`flags` control slot behavior (`SUMMARY` for the property sheet, `READONLY`/`TRANSIENT`
for computed outputs, `HIDDEN` for internal timer actions). Actions are declared the
same way — `BEvaporatorUnit` has a hidden timer callback:

```java
@NiagaraAction(name = "startDelayExpired", flags = Flags.HIDDEN)
```

Frozen enums use `@NiagaraEnum` + `@Range` (from `BStagingMode.java`):

```java
@NiagaraType
@NiagaraEnum(range = { @Range("single"), @Range("staged") })
public final class BStagingMode extends BFrozenEnum { ... }
```

### 3.2 The Slot-o-Matic auto-generated region

When you build, Slot-o-Matic reads the annotations and writes the slot constants,
getters/setters, and action dispatch into a fenced region inside the same `.java` file:

```java
//region /*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
//@formatter:off
/*@ $com.angeles.ColdRoomPan.BColdRoom(3165439619)1.0$ @*/
/* Generated Sun Aug 30 19:14:18 CST 2026 by Slot-o-Matic (c) Tridium, Inc. 2012-2026 */
  public static final Property differentialUp = newProperty(0, 1d, BFacets.make(BFacets.MIN, BDouble.make(0d)));
  public double getDifferentialUp() { return getDouble(differentialUp); }
  public void setDifferentialUp(double v) { setDouble(differentialUp, v, null); }
  ...
  public static final Type TYPE = Sys.loadType(BColdRoom.class);
//@formatter:on
//endregion /*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/
```

- **Never hand-edit** inside the region — Slot-o-Matic overwrites it.
- The `/*@ $FQCN(hash)1.0$ @*/` line is a content hash; Slot-o-Matic re-runs codegen
  when the annotations change. See [`module-dev-workflow.md`](module-dev-workflow.md) §1.3.
- **All hand-written logic goes BELOW `//endregion`** — constructor, lifecycle
  overrides (`atSteadyState`, `changed`, `stopped`), and control methods.

### 3.3 The actual control logic

**`BColdRoom` — asymmetric hysteresis + staging map.** `computeCall()` starts cooling
above setpoint and stops below it, holding in the deadband (and holding on a faulted
sensor, fail-safe):

```java
double high = sp + dUp;   // start cooling above setpoint (differentialUp)
double low  = sp - dDown; // stop cooling below setpoint (differentialDown)
if (cv >= high) return true;
if (cv <= low)  return false;
return prev;              // deadband -> hold (hysteresis)
```

`execute()` then maps the zone calls onto the child units. In `single` mode all units
follow one call; in `staged` mode (Room 1) the mapping is by child order:

```java
if (!staged)          run = call1;            // Rooms 2,3,4
else if (i == 0)      run = call1;            // Room 1 unit 1
else if (i == 1)      run = call1 || call2;   // Room 1 unit 2 = OR of both zones
else                  run = call2;            // Room 1 unit 3
```

The room's `cooling` output = `call1 || call2`.

**`BEvaporatorUnit` — valve-first actuation.** On a rising `runCmd`: open `valveOut`
immediately, schedule `startDelayExpired` after `startDelay`, then set `evapOut`. On a
falling edge: stop the evaporator first, then close the valve (`applyRunCmd()` +
`doStartDelayExpired()`):

```java
if (cmd && !lastCmd) {                    // rising
  setBool(valveOut, true);
  startDelayTicket = Clock.schedule(this, getStartDelay(), startDelayExpired, null);
} else if (!cmd && lastCmd) {             // falling
  setBool(evapOut, false);
  setBool(valveOut, false);
}
```

Defrost hooks `enterDefrost()` / `exitDefrost()` let the controller take over the
outputs (close valve → stop evap → energize `resistanceOut`) only when `hasDefrost`.

**`BDefrostController` — trigger + sequence + interlock.** Trigger is `interval`
(free-running `Clock.schedule` timer, default 8 h) or `schedule` (rising edge on a
linked `scheduleInput`). `requestDefrostCycle()` queues every unit; the interlock
serializes them so **only one unit defrosts at a time**:

```java
if (defrostingUnit == -1) beginDefrost(i);   // token free -> start now
else if (waitingUnit == -1) waitingUnit = i; // busy -> wait our turn
```

A unit ends on `duration` elapsed **or** (`terminateOnResistanceTemp` and
`resistanceTemp >= resistanceTempThreshold`). When it ends, the waiting unit starts a
`staggerDelay` timer (default 4 min = `240000` ms) before taking the token.

### 3.4 The containment model — this is the crucial architectural point

`BEvaporatorUnit` and `BDefrostController` are **children of `BColdRoom`** and driven
**internally** — there are **no block-to-block wires**:

- `BColdRoom.getUnits()` returns `getChildren(BEvaporatorUnit.class)` in slot order and
  the room drives each unit by calling `u.getRunCmd().setValue(run); u.applyRunCmd();`.
- `BDefrostController.units()` reaches back to its parent: `((BColdRoom)getParent()).getUnits()`.

The only wiring at commissioning is to **physical points**: zone/coil/resistance
sensors are read *into* the input slots, and `valveOut` / `evapOut` / `resistanceOut`
are linked *out* to driver writables via **BLink at priority level `in8`**. (Design §6.)
The `setBool(...)` helper writes the slot; the BLink propagates that to the field
writable.

### 3.5 The descriptor files

Three descriptors sit next to the source. Keep them in sync with the code:

- **`module-include.xml`** — one `<type>` per exported class. A class not listed here
  is dead bytecode (no slots generated, `BTypeSpec.resolve()` never finds it). Includes
  the **two frozen enums**:

  ```xml
  <type class="com.angeles.ColdRoomPan.BColdRoom"          name="ColdRoom"/>
  <type class="com.angeles.ColdRoomPan.BEvaporatorUnit"    name="EvaporatorUnit"/>
  <type class="com.angeles.ColdRoomPan.BDefrostController" name="DefrostController"/>
  <type class="com.angeles.ColdRoomPan.BStagingMode"       name="StagingMode"/>
  <type class="com.angeles.ColdRoomPan.BDefrostMode"       name="DefrostMode"/>
  ```

- **`module.palette`** — lists the components so they appear in the Workbench palette
  (a `bajaObjectGraph` with one `<p>` per instantiable type). Only the three components
  are placed; the enums are slot types, not palette entries.

- **`module.lexicon`** — display names shown in Workbench (Spanish here). **Key = the
  slot name** (or type/enum tag). e.g. `ColdRoom=Cuarto frio`, `setpoint=Consigna`,
  `single=Simple`.

- **`module-permissions.xml`** — Java Security Manager request manifest. Empty for
  `ColdRoomPan` (no restricted host/JVM capabilities needed). See
  [`bloque721.md`](../niagara-mental-model-bloque721.md).

---

## 4. Build in WSL2

The module is built from a WSL2 (Ubuntu, ext4) shell. Copy the project to a WSL-native
path first, then make the wrapper executable:

```bash
chmod +x gradlew
```

`gradle.properties` stores Windows `C:\` paths that don't exist inside WSL, so **two
`-P` overrides are mandatory** on the command line. Run from the **project root** (where
`gradlew` lives), never from the inner part folder:

```bash
./gradlew :ColdRoomPan-rt:clean :ColdRoomPan-rt:slotomatic :ColdRoomPan-rt:jar \
  -Pniagara_home=<niagara-home> \
  -Porg.gradle.java.installations.paths=/usr/lib/jvm/java-8-openjdk-amd64
```

- **Always include `clean`** — otherwise a cached `META-INF/module.xml` with stale
  `<types>` gets packaged.
- **Run `slotomatic` only when a `@Niagara*` annotation changed** (added/removed slot or
  action). It regenerates the AUTO GENERATED region and updates the class hash. It runs
  fine in WSL — "slotomatic requires Windows" is a refuted myth.
- The built jar lands in `$niagara_home/modules/ColdRoomPan-rt.jar`; deploy = copy it to
  the target station's `modules/` dir.

**Version targeting** (build against the SDK you point `niagara_home` at):

| Target | Plugin version (`settings.gradle.kts`) | `niagara_home` |
|---|---|---|
| 4.15 (this project) | `7.6.22` | `C:\PowerB\PowerB-4.15.3.28` |
| 4.14 (e.g. chihuahua) | `7.6.17` | `.../OptimizerSupervisor-N4.14.0.162` (Honeywell) |

Full detail and the deploy/backup wrapper story: [`bloque722.md`](../niagara-mental-model-bloque722.md).

---

## 5. Sign the module

A host accepts a signed module **iff it trusts the CA** that signed the signing
certificate. Build the chain once in Workbench (`Tools → Certificate Management`):

1. **Create the CA** — `User Key Store → New`, Alias e.g. `luisCA`, **Certificate Usage
   = CA**, Key Size 4096, "Not After" far out. Appears yellow (untrusted).
2. **Trust the CA** — Export it (with private key), then `User Trust Store → Import` it
   back → turns green ✓.
3. **Create the code-signing cert** — `User Key Store → New`, Alias e.g. `luissigner`,
   **Certificate Usage = Code Signing**. Appears yellow.
4. **CSR** — select the signing cert → `Cert Request` → save the `.csr`.
5. **Sign the CSR** — `Tools → Certificate Signer Tool`, pick the `.csr`, enter the CA
   password, save.
6. **Import the signed cert back** — `User Key Store → Import` → turns green ✓. The
   signing identity is ready.

**Sign the jar** — two equivalent ways:
- **Workbench:** `Tools → Jar Signer Tool` → select `ColdRoomPan-rt.jar` → alias
  `luissigner` → password. (This is what was used.)
- **Gradle:** a `niagaraSigning { aliases / keystorePassword / keyPassword }` block in
  the root `build.gradle.kts` (secrets from a private property, never hard-coded).

If you sign nothing explicitly, the default Gradle build signs with an **auto-generated
DEV cert** (`CN=…(Niagara4Modules)`, OU "For Development Purposes Only", self-signed) —
enough for a dev station, not for a locked/production one. Full walkthrough:
[`bloque723.md`](../niagara-mental-model-bloque723.md).

---

## 6. Deploy + trust on the JACE

The JACE only ever receives the CA's **public** certificate — **the private key never
leaves the signing machine.**

1. **Export the CA's PUBLIC cert** — `User Trust Store → Export` (that store is
   public-only, so the key can't leak), producing a `.pem`.
2. **Connect to the JACE's Platform** (the remote host in Nav, e.g. `192.168.1.140`).
3. **`Certificate Management → User Trust Store → Import`** the CA `.pem` → the CA shows
   green ✓. The JACE now accepts any module signed by that CA's certs.
4. **Install the signed module** — via Software Manager, or copy `ColdRoomPan-rt.jar`
   into the platform `modules/` dir.
5. **Restart the station** to load the new module and its frozen slots. If a new frozen
   slot still doesn't appear, close and reopen Workbench (its client-type cache doesn't
   refresh on station restart).

See [`bloque723.md`](../niagara-mental-model-bloque723.md) §723.4.

---

## 7. Cross-version compatibility

A module's `module.xml` records its baja dependency at the **minor** version, not the
patch. `ColdRoomPan-rt`'s built manifest is:

```xml
<dependency name="baja" vendor="Tridium" vendorVersion="4.15"/>
```

That means "requires baja ≥ 4.15", so a module built against **4.15.3.28** runs on **any
4.15.x**. This was verified live: `ColdRoomPan` built against 4.15.3.28 loaded in a
Workbench install of **4.15.3.20** (palette shows its components) and targets a JACE of
**4.15.3.28** — the patch numbers are irrelevant to the dependency.

**Rule:** build against the **lowest** target minor version → it runs on that minor and
higher. Building against a higher minor and deploying to a lower one fails on the missing
baja version. Detail: [`bloque723.md`](../niagara-mental-model-bloque723.md) §723.5.
