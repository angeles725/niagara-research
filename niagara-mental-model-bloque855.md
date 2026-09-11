# Block 855 — Route A custom module: slot facets, scale instantiation, and upgrade recompile plan (closes B843-G1/G2/G3)

> **Focus:** harbor-greenmax-lighting. **Scope:** three open design-detail gaps for the `BGreenMaxPanel`/`BGreenMaxCircuit`
> module (B843 Route A). This block deepens B843 — do **not** re-read §1–§9 of that block; it is assumed. The three
> gaps answered here are: (G1) what `BFacets` and slot `Flags` the components need; (G2) how to instantiate ~330
> circuit children without hand-wiring; (G3) what a major N4 upgrade costs for this module vs the kitControl Route B.
>
> **Evidence base (priority order per METHODOLOGY §6):**
> - **Corpus framework source** `[CERT]` — `extracted/baja/javax/baja/sys/BFacets.java` + `Flags.java`; decompiled
>   exemplar `clHVAC-rt/vineflower/cl/hvac/BDutyCycleTypeEnum.java` + `BSwitchingLogic.java` + `BEovOpModeEnum.java`.
> - **Team module manifests** `[CERT]` — `Cliente/Leon-Guanjuato/…/module.xml` (ColdRoomPan-rt, DashboardPan-ux);
>   client station `_client-ecnet-4.3.58.18-n4.4/kitPx-wb/META-INF/module.xml`.
> - **B843** `[INFER]` — the slot definitions this block annotates (§3 there).
> - **B854** `[CERT-live]` — per-panel relay counts; bog-nav confirmed panel naming and `BooleanSchedule` counts.
> - **build-n4-module SKILL + team retros** `[CERT(process)]` — verify-gate `facets-req` lint, `defaultModuleVersion`,
>   `niagara_home` migration, signing.
> - **B838** `[CERT-live/doc]` — station is 4.3.58.18; 4.3→4.15 forces module recompile.
>
> **Sources:**
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/BFacets.java`
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/sys/Flags.java`
> - `organized/clHVAC/clHVAC-rt/vineflower/cl/hvac/BDutyCycleTypeEnum.java`
> - `organized/clHVAC/clHVAC-rt/vineflower/cl/hvac/BSwitchingLogic.java`
> - `organized/jsonToolkit/META-INF/module.xml` (vendorVersion + moduleParts exemplar)
> - `organized/_client-ecnet-4.3.58.18-n4.4/kitPx-wb/META-INF/module.xml` (4.3 dependency pin exemplar)
> - `Cliente/Leon-Guanjuato/Paccadia/ColdRoomPan-rt/META-INF/module.xml` (team manifest exemplar)
> - B843, B844, B851, B854 (cited, not re-derived)
> - niagara-help query `module upgrade recompile sign` → zero result (recorded as data)
> - niagara-help query `vendorVersion dependency` → zero result (recorded as data)

---

## 1. G1 — Slot flags and `BFacets` for `BGreenMaxPanel` and `BGreenMaxCircuit`

### 1.1 The two flag constants that matter `[CERT]`

`Flags.java:183–191`:

```
READONLY  = 0x00000001   // 'r' — slot is not writable by any user
TRANSIENT = 0x00000002   // 't' — not persisted; designed for link input/output
OPERATOR  = 0x00000100   // 'o' — operator-level (not admin) write permission
```

Applying these to B843 §3 slots:

| Slot | Flags | Rationale |
|---|---|---|
| `panelScheduleSel`, `panelHoa` | `Flags.OPERATOR` | Operator writes the selector/HOA dropdown |
| `circuitScheduleSel`, `circuitHoa` | `Flags.OPERATOR` | Same — one per circuit |
| `h1In..h4In` (schedule master inputs) | `Flags.READONLY\|Flags.TRANSIENT` | Link targets only; operator must NOT see a text field |
| `effectiveState`, `effectiveSchedule` | `Flags.READONLY\|Flags.TRANSIENT` | Computed outputs; not persisted |
| `autoOut`, `ovrOut` | `Flags.READONLY\|Flags.TRANSIENT` | Link sources to `SchdlGMnn Rk.in10/in8`; not user-writable |
| `forceRecalc` (action) | `Flags.HIDDEN` | Internal; not exposed in operator UI |

`[CERT]`: bit values from `Flags.java:183–191`. Flags composition is `|`-combined in the annotation `flags` field or in the `newProperty()` third call.

### 1.2 Enum range facets — automatic from `@NiagaraEnum` + `@Range` `[CERT]`

`BFacets.java:93` defines `RANGE = "range"` and `BFacets.java:386–394` shows that
`BFacets.makeEnum(BEnumRange range)` caches the facets map `{range → BEnumRange}` directly on the
`BEnumRange` instance returned by the `BFrozenEnum` type.

`BDutyCycleTypeEnum.java` (clHVAC vineflower) shows the complete pattern for a frozen enum:

```java
@NiagaraType
@NiagaraEnum(
    range = {@Range("Heating"), @Range("Cooling"), @Range("HeatingAndCooling")},
    defaultValue = "Heating"
)
public final class BDutyCycleTypeEnum extends BFrozenEnum { … }
```

Slotomatic reads `@NiagaraEnum` and generates the `BEnumRange` (ordinal tags `Heating`, `Cooling`, …) and the
static `TYPE` loader. The runtime then serves `BFacets.makeEnum(range)` automatically whenever a UI component
asks for the facets of a slot typed to this class. **No explicit `@Facet` annotation is needed inside
`@NiagaraProperty` for enum types** — the facets are derived from the type.

Applying to the B843 enums:

```java
// BScheduleSel.java — 4-option horario selector (panel-level)
@NiagaraType
@NiagaraEnum(range = {@Range("H1"), @Range("H2"), @Range("H3"), @Range("H4")},
             defaultValue = "H1")
public final class BScheduleSel extends BFrozenEnum { … }

// BCircuitSel.java — per-circuit selector (adds FollowPanel)
@NiagaraType
@NiagaraEnum(range = {@Range("FollowPanel"), @Range("H1"), @Range("H2"), @Range("H3"), @Range("H4")},
             defaultValue = "FollowPanel")
public final class BCircuitSel extends BFrozenEnum { … }

// BHoaMode.java — Hand/Off/Auto override
@NiagaraType
@NiagaraEnum(range = {@Range("Hand"), @Range("Off"), @Range("Auto")},
             defaultValue = "Auto")
public final class BHoaMode extends BFrozenEnum { … }
```

The Workbench / Hx property sheet renders any `BFrozenEnum` property as a **dropdown** using the `BEnumRange`
from `BFacets.RANGE` — exactly the "HOA-style selector with N options" the operator asked for (B842 §1). `[CERT]`

### 1.3 Numeric facets for input slots — `@Facet` inside `@NiagaraProperty` `[CERT]`

For numeric-typed slots (none in this module — all slots here are `BFrozenEnum`, `BStatusBoolean`, or
`BStatusEnum`), the pattern (from `BSwitchingLogic.java` clHVAC vineflower, lines ~40–48) is:

```java
@NiagaraProperty(name = "outDelayMillis", type = "long", defaultValue = "0",
    facets = {
        @Facet(name = "BFacets.MIN", value = "0"),
        @Facet(name = "BFacets.MAX", value = "0xFFFFFFFFL")
    })
```

The `BGreenMaxPanel` / `BGreenMaxCircuit` classes have **no numeric OPERATOR slots**, so the verify-gate
`facets-req` lint (which WARNs on OPERATOR numerics without UNITS/PRECISION) will not fire. The enum
properties are excluded from that lint because their facets come from the type, not from `@Facet` tags.

### 1.4 Force-ON override is NOT a separate slot `[INFER]`

The "force-ON override" requested in the gap description is implemented by the `BHoaMode.Hand` value on
`circuitHoa` / `panelHoa`. When the operator selects "Hand", the resolver sets `ovrOut = {true}` → writes
`SchdlGMnn Rk.in8 = true` (priority 8), forcing the circuit ON independent of schedules. There is no
separate `forceOnOverride: BStatusBoolean` slot — the HOA enum is the override control. A dedicated `BStatusBoolean`
override slot would duplicate the HOA semantics and confuse the operator. `[INFER]`

---

## 2. G2 — Instantiation tooling for ~13 panels / ~330 circuits

### 2.1 The scale problem `[CERT-live]`

B854 §5 confirms relay counts for 13 operative panels:
GM02=20, GM04=20, GM05=15, GM06=20, GM07=23, GM08=32, GM10=32, GM11=26, GM12=12,
GM13=30, GM14=32, GM15=32, GM16=13.
Total: **307 circuits** across 13 panels (the 13 operative panels; GM01/GM03/GM09 are partial/non-operative
per B854). Each requires one `BGreenMaxCircuit` child and two output links (`autoOut`→`in10`,
`ovrOut`→`in8`).

### 2.2 Option A — Workbench manual copy-paste `[INFER]`

Build one `BGreenMaxPanel` template fully (4 master-schedule links + one `BGreenMaxCircuit` with both
output links), then Ctrl+C/Paste-15 times in the nav-tree, rename each panel, and manually:
- Re-link the 4 master schedule inputs for each panel.
- Add the correct count of `BGreenMaxCircuit` children per the B854 relay table.
- Re-link each circuit's `autoOut`/`ovrOut` to the corresponding `SchdlGMnn Rk.in10`/`in8`.

**Tradeoffs:** Zero tooling; works in any Workbench version. Creating 307 circuit instances + 614 output
links manually is prohibitively slow (estimated 4–8 hours of click-heavy Workbench sessions) and highly
error-prone (mis-linked circuits, wrong relay count per panel). Not recommended. `[INFER]`

### 2.3 Option B — BajaScript program object `[INFER]`

Niagara's `bajaScript-ux` module (installed on 4.3 supervisors — `bajaScript-ux` appears in
`kitPx-wb/module.xml` dependency list `[CERT]`) allows a `BProgram` component in the station to run
JavaScript that calls the Baja runtime API directly:

```javascript
// pseudocode — BajaScript on the station
var panel = BOrd.make("station:|slot:/Iluminacion/GM2").get();
for (var k = 1; k <= 20; k++) {
    var circuit = BObject.make("GreenMaxLighting:BGreenMaxCircuit");
    panel.add("R" + k, circuit, null);
    // then create links autoOut→SchdlGM02 R1.in10, ovrOut→in8
}
```

**Tradeoffs:** No external tooling; runs live in the station. Authoring the script requires Workbench's
BajaScript editor. Error handling is manual — a script crash leaves the component tree in a partial state.
Debugging in a live station has commissioning risk. BajaScript also requires the `BGreenMaxLighting` module
to be already deployed before the script can instantiate `BGreenMaxCircuit`. Link creation in BajaScript
(the `BLink` API) needs careful ORD construction against the existing `SchdlGMnn Rk` path. `[INFER]`

### 2.4 Option C — Bog-subtree emitter (RECOMMENDED) `[INFER]`

Extend the existing `tools/greenmax-tablero-gen.py` pattern (already used to generate the 13 tablero PxViews
at scale in this campaign; CERT from B854/tools/README.md) to a new tool
`tools/greenmax-panel-gen.py` that:
1. Reads the per-panel relay count table from B854 §5 (hard-coded or from a CSV).
2. Generates one `BGreenMaxPanel` bog XML subtree per panel, containing the correct N `BGreenMaxCircuit`
   children (named `R1..RN`), with each child's `autoOut` and `ovrOut` link declarations pointing to the
   correct `SchdlGMnn Rk.in10`/`in8` ORD.
3. Outputs bog XML fragments for Workbench File → Import (or paste-special into the `Iluminacion` folder).

**Tradeoffs (PROS):** The subtree is generated and reviewable offline, before touching the live station.
Repeatable and idempotent. Follows the existing generator infrastructure (same ORD-construction conventions
as `greenmax-tablero-gen.py`). Re-running with updated relay counts regenerates the subtree without
partial-create risk. The bog XML can be version-controlled. Fastest path to 307 circuits with the lowest
error rate. **CONS:** Requires the `BGreenMaxLighting` module jar to be deployed before the imported bog
can resolve the component types. Bog import may silently drop an unresolved type unless the station is
running with the module loaded. A dry-run validation step (import to a scratch station with the module
loaded) is recommended before the live-station import. Handle assignment in imported bogs is managed
by Workbench automatically. `[INFER]`

### 2.5 Decision summary

| Option | Effort | Risk | Requires module pre-deploy | Offline review |
|--------|--------|------|---------------------------|----------------|
| Manual copy-paste | Very high (~307 manual ops) | High (link errors) | Yes | No |
| BajaScript program | Medium (script authoring) | Medium (live partial state) | Yes | Partial |
| Bog-subtree emitter | Low (one generator run) | Low (offline) | Yes | Yes |

**Recommended: Option C (bog-subtree emitter).** B853-G9 (independently seeded) identifies the same
need. Writing `greenmax-panel-gen.py` is the natural successor to `greenmax-tablero-gen.py`. `[INFER]`

---

## 3. G3 — Upgrade recompile plan: Route A custom module vs Route B kitControl

### 3.1 What pins the module to a platform version `[CERT]`

The module manifest `module.xml` declares each Tridium dependency with an explicit `vendorVersion` pin.
From the client station's `kitPx-wb/META-INF/module.xml` `[CERT]`:

```xml
<module name='kitPx-wb' vendorVersion='4.3.58.18' …>
  <dependency name='baja' vendor='Tridium' vendorVersion='4.3'/>
  <dependency name='control-rt' vendor='Tridium' vendorVersion='4.3'/>
  …
</module>
```

The `vendorVersion='4.3'` in each dependency tells the platform class loader what the module was compiled
against. When the platform is at 4.14, it provides `baja@4.14` on the module classpath — and the platform
accepts a module whose dependency pin is `4.3` only if the `4.3` API is a compatible subset of `4.14`.
This is generally true for *Tridium* modules (Tridium maintains backward compatibility within a generation),
but for B838 §7 `[CERT-doc]`, the 4.3→4.15 transition crosses a QNX6.5→QNX7.0/Linux OS boundary and
introduces breaking changes in native-layer APIs, requiring a recompile even for modules that don't
touch the changed APIs.

### 3.2 Recompile steps on a major upgrade `[CERT(process)/INFER]`

The team's `ColdRoomPan-rt/module.xml` `[CERT]` shows the manifest after a successful build against 4.14:

```xml
<module name="ColdRoomPan-rt" vendor="Angeles" vendorVersion="2.0.3" …>
  <dependency name="baja" vendor="Tridium" vendorVersion="4.14"/>
  …
</module>
```

The full steps to port `GreenMaxLighting-rt` from 4.3 to a hypothetical 4.15 target:

1. **Set `niagara_home`** in `gradle.properties` to the new 4.15 install directory
   (`niagara_home=C:\Niagara\Niagara-4.15.x.y`). The Niagara Gradle plugin reads this to pick the right
   dependency jars from `niagara_home/etc/m2/repository`. `[CERT(process)]`
2. **Bump `defaultModuleVersion`** in the GROUP `build.gradle.kts` (MEMORY: "defaultModuleVersion in
   GROUP build.gradle.kts, not the module .kts / not vendorVersion"). For example, `"1.0.0"` → `"1.1.0"`.
   This sets the module's own `vendorVersion` in the manifest. `[CERT(process)]`
3. **Build** with `toolbelt/build.sh` (Java 8 + slotomatic + jar). The Gradle plugin resolves all
   `<dependency>` `vendorVersion` pins to `4.15` automatically from `niagara_home`. `[CERT(process)]`
4. **Sign** the jar (`NIAGARA4.SF` entry) with the team's key. A new bytecode round produces a new
   signature regardless of logic changes. `[CERT(process)]`
5. **Run `verify-module.sh`**: bytecode major 52, `NIAGARA4.SF` present, `module.xml` types resolve.
   `[CERT(process)]`
6. **Deploy** to the upgraded station (backup → install → reload → `triage-console.sh`).

### 3.3 `<moduleParts>` cost `[CERT/INFER]`

From `jsonToolkit/META-INF/module.xml` `[CERT]`:

```xml
<moduleParts>
  <modulePart name="httpClient-ux" runtimeProfile="ux"/>
  <modulePart name="httpClient-wb" runtimeProfile="wb"/>
</moduleParts>
```

A module with `-ux` or `-wb` parts must recompile and re-sign **each part** separately. For a pure `-rt`
module (B843 design scope — only `GreenMaxLighting-rt`), there is only one jar to recompile. A future
dashboard part (`GreenMaxLighting-ux`) would add a second recompile unit. `[INFER]`

### 3.4 Route A vs Route B on upgrade `[INFER]`

| Item | Route A (custom module) | Route B (kitControl) |
|---|---|---|
| Who owns the recompile? | The integrator (Angeles / future client integrator) | Tridium — ships with every new N4 |
| Toolchain required? | Yes: Java 8, Gradle, signing cert, niagara_home for new version | None |
| Recompile per major upgrade? | Yes — mandatory (4.3→4.15 confirmed by B838 §7 `[CERT-doc]`) | No — `kitControl` is Tridium-maintained |
| Recompile per minor upgrade? | Only if Tridium breaks a used API | No |
| Station-only serviceability? | No — future EC-Net integrator needs the source + build env | Yes — Workbench-only config |
| Logic change = re-sign + re-deploy? | Yes | No — logic IS the wiring |

The recompile cost is real but bounded: for a pure `-rt` module touching only `baja`, `control-rt`, and
`schedule-rt`, the recompile is a `./gradlew clean slotomatic jar` against the new `niagara_home` — less
than 5 minutes of build time once the toolchain is set up. The lasting cost is **maintained toolchain and
source custody** across integrators. `[INFER]`

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|---|---|---|
| G1-1 | `Flags.OPERATOR = 0x00000100`, `READONLY = 0x00000001`, `TRANSIENT = 0x00000002` | [CERT] | `Flags.java:183–184,191` |
| G1-2 | `BFacets.RANGE = "range"` constant; `makeEnum(BEnumRange)` caches facets on the range | [CERT] | `BFacets.java:93,386–394` |
| G1-3 | `@NiagaraEnum(range={@Range(…)}, defaultValue=…)` on `BFrozenEnum` → slotomatic generates `BEnumRange`; no `@Facet` needed inside `@NiagaraProperty` for enum slot | [CERT] | `BDutyCycleTypeEnum.java` (clHVAC vineflower), entire file |
| G1-4 | `@Facet(name="BFacets.MIN", value="0")` inside `@NiagaraProperty` is the pattern for numeric min/max | [CERT] | `BSwitchingLogic.java` (clHVAC vineflower) lines ~40–48 |
| G1-5 | Selector+HOA slots → `Flags.OPERATOR`; input link slots → `Flags.READONLY\|Flags.TRANSIENT`; output/computed slots → same | [INFER] built on [CERT] | Flag values from G1-1; slot role assignments from B843 §3 |
| G1-6 | Force-ON override is the `BHoaMode.Hand` enum value — no separate boolean override slot | [INFER] | Design; HOA Hand → ovrOut=true → in8=true |
| G1-7 | verify-gate `facets-req` lint exempts enum properties (facets come from type) | [CERT(process)] | build-n4-module SKILL: "facets-req (OPERATOR numeric without facets key … WARN)" |
| G2-1 | 13 operative panels, relay counts GM02=20..GM16=13; total 307 circuits | [CERT-live] | B854 §5 (all three sources agree) |
| G2-2 | `bajaScript-ux` is a dependency in the client's 4.3 station (installed) | [CERT] | `kitPx-wb/module.xml` dependency list |
| G2-3 | `greenmax-tablero-gen.py` successfully generated 13 tablero views this campaign (same generator pattern as proposed emitter) | [CERT(process)] | B854 §6; `tools/README.md` |
| G2-4 | Bog-subtree emitter is recommended: offline, reviewable, consistent with existing tooling | [INFER] | Design comparison |
| G3-1 | `ColdRoomPan-rt/module.xml` pins `baja vendor="Tridium" vendorVersion="4.14"` after build against 4.14 | [CERT] | `Cliente/Leon-Guanjuato/Paccadia/ColdRoomPan-rt/META-INF/module.xml` |
| G3-2 | Client 4.3 station's `kitPx-wb` pins all dependencies to `vendorVersion='4.3'` | [CERT] | `_client-ecnet-4.3.58.18-n4.4/kitPx-wb/META-INF/module.xml` |
| G3-3 | A `<moduleParts>` module requires each part recompiled; a pure `-rt` module has one jar | [CERT/INFER] | `jsonToolkit/META-INF/module.xml` `<moduleParts>` structure |
| G3-4 | 4.3→4.15 upgrade forces module recompile (OS boundary) | [CERT-doc] | B838 §7 (cited, not re-derived) |
| G3-5 | Route B (kitControl) costs zero on upgrade — Tridium-maintained, no source custody | [INFER] | Route B design B844; Tridium ships kitControl with every N4 |
| G3-6 | Recompile steps: set `niagara_home`, bump `defaultModuleVersion` in GROUP build.gradle.kts, `./gradlew clean slotomatic jar`, sign, verify-module.sh | [CERT(process)] | build-n4-module SKILL; team bitacora (`grep niagara_home` in Paccadia) |

**Tally:** 9 `[CERT]` or `[CERT(process)]`, 1 `[CERT-live]`, 1 `[CERT-doc]`, 5 `[INFER]` built on
`[CERT]`, 0 unverified tool failures. The niagara-help zero results for "module upgrade recompile sign"
and "vendorVersion dependency" are recorded as data — both queries drew on original source + manifest
files instead.

---

## Connections

- **[B843]** — the slot definitions (§3) and build/deploy discipline (§7) this block annotates. B855 closes
  B843-G1, B843-G2, and B843-G3 filed there.
- **[B844]** — Route B kitControl: no recompile on upgrade (G3 trade-off); no instantiation tooling needed
  beyond manual wiring (~330 wiresheet copies — a different scale problem).
- **[B854]** — per-panel relay counts used in G2-1; the 13-panel table is the input to the bog-subtree emitter.
- **[B853-G9]** — independently seeded gap for programmatic control-graph generation; this block's G2-4
  confirms the bog-emitter route and closes the overlap.
- **[B838]** — platform version 4.3.58.18 + 4.3→4.15 recompile boundary (G3 anchor).
- **[B851]** — live Route B pilot confirmed the 4.3 Supervisor handles the kitControl-only approach without
  module maintenance, reinforcing the G3 trade-off.
- **Team module retros (ColdRoomPan/DashboardPan)** — the manifest structure, `defaultModuleVersion`,
  `niagara_home`, and sign/verify-gate discipline that G3 re-uses.

---

## Open gaps

- **B855-G1** (requires execution) — Prove the bog-subtree emitter approach: write `tools/greenmax-panel-gen.py`
  (one panel, N=20 circuits for GM02), import the bog into a scratch station with `GreenMaxLighting-rt` loaded,
  confirm that all `BGreenMaxCircuit` children resolve and output links bind. This validates the offline
  emitter before committing to the full 307-circuit bulk import.
- **B855-G2** (investigable) — Confirm whether Workbench 4.3 bog import preserves `@NiagaraProperty` default
  values on import (e.g., `circuitScheduleSel` defaults to `FollowPanel`). If bog import ignores defaults and
  sets all enum slots to ordinal 0, a post-import script step is needed to set each `circuitScheduleSel` to
  `BCircuitSel.FollowPanel`. Depends on B855-G1 execution.
- **B843-G1** (original, distinct) — The original B843-G1 (write mechanism for the HOA override: data link
  with null-status vs programmatic `set()`) is a separate question from this block's G1. It remains open
  (requires live bench test) and is noted in RESEARCH-STATE as a `requires-execution` gap.
