# Bloque 422 — `kitPxBuilding`: BComponents como agregadores de estado multi-input (la excepción del pack)

> Research del focus **`px-tail`**, gap **P2** (MEDIUM). Documenta `kitPxBuilding` — el pack de widgets
> PX que [Bloque 203] señaló como "la excepción" frente a los otros tres packs (Graphics/Hvac/N4svg).
> Responde la pregunta guía: ¿por qué estos componentes necesitan código Java tipado cuando los demás
> packs son BOG puro? Scope: las 15 clases reales (13 `-rt` + 2 `-ux`; `-wb` = 0 Java). NO cubre la
> lógica JS del bundle ni las variantes de palette BOG del propio pack.
>
> **Versión del sujeto**: `kitPxBuilding-rt` v4.14.0.2 (build 2024-04-10, vendor: Tridium Europe,
> `META-INF/module.xml`). Dependencia declarada: `baja` Tridium ≥ 4.14.
>
> **Sources (READ-ONLY, decompilado Vineflower)**:
> `/home/cristian/modules/Prototipos/modulos/organized/kitPxBuilding/kitPxBuilding-rt/vineflower/`
> `/home/cristian/modules/Prototipos/modulos/organized/kitPxBuilding/kitPxBuilding-ux/vineflower/`
> (citas `file:line` relativas a su directorio `vineflower/` base).
> `module.xml` leído desde `kitPxBuilding-rt/extracted/META-INF/module.xml`.
>
> **Method**: lectura directa de decompilado Vineflower; 15 clases, gap acotado, lectura inline (sin
> delegación). Conteo de `-wb` Java: `find` = 0 clases Java; confirmado por `decompiled/summary.txt`
> (sólo `module.palette + res`, sin clases).
>
> **Markers** (METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> **Connections**: [Bloque 203] (marcó kitPxBuilding como excepción; los otros tres packs son BOG puro),
> [Bloque 421] (patrón `-ux` `BSingleton+BIJavaScript` vs `BBajaScriptTypeExt` aquí).

---

## 422.1 — Estructura tri-parte: 15 clases Java (0 en `-wb`) `[CERT]`

El módulo `kitPxBuilding` es tri-parte (`rt`/`ux`/`wb`) pero el código Java se concentra en dos:

| Perfil | Clases Java | Contenido |
|---|---|---|
| `-rt` | **13** | 7 BComponent/BEnum-state + 6 BFrozenEnum types |
| `-ux` | **2** | JsBuild + TypeExt para superficie web |
| `-wb` | **0** | Sólo `module.palette` + recursos (sin clases Java) |
| **Total** | **15** | — |

`[CERT]` conteos: `find kitPxBuilding-rt/vineflower -name "*.java"` = 13 hits;
`find kitPxBuilding-ux/vineflower -name "*.java"` = 2 hits;
`decompiled/summary.txt` de `-wb`: "META-INF, module.palette, res" (sin `.class`).

El `module.xml` del `-rt` registra 13 tipos en el NRE y declara a `-ux` y `-wb` como `modulePart`.
`[CERT]` `kitPxBuilding-rt/extracted/META-INF/module.xml` (bloque `<types>` + `<moduleParts>`).

---

## 422.2 — La clase base: `BKitPxBuildingBaseEnum extends BComponent implements BIEnum` `[CERT]`

La clave arquitectural del pack es su clase abstracta base:

```java
// BKitPxBuildingBaseEnum.java:12
@NiagaraType
public abstract class BKitPxBuildingBaseEnum extends BComponent implements BIEnum {
    public static final Type TYPE = Sys.loadType(BKitPxBuildingBaseEnum.class);

    public final BEnum getEnum()      { return this.getOut().getEnum(); }
    public final BFacets getEnumFacets() { return this.getOut().getEnum().getEnumFacets(); }

    public abstract BEnum getOut();
    public abstract void setOut(BEnum var1);
}
```

`[CERT]` `com/tridiumemea/extras/BKitPxBuildingBaseEnum.java:12-30`.

Dos decisiones de diseño clave, y por qué importan:

| Decisión | Consecuencia |
|---|---|
| `extends BComponent` | El componente existe en el árbol de componentes de la estación; tiene slots persistentes, participa en el wire-sheet, es addressable por ORD |
| `implements BIEnum` | Expone su `out` como un `BEnum`; lo hace compatible con los converters de enum estándar (ej. `FrozenEnumToSimple`) que una página PX puede usar directamente |

La interfaz `BIEnum` es el **puente**: hace que el componente "se comporte como un enum" para cualquier binding de bajaux o PX, sin que el binding necesite saber que hay lógica multi-input detrás.

---

## 422.3 — Los cinco componentes rt: máquinas de estado `changed()` `[CERT]`

Las 7 clases concretas del `-rt` (5 componentes de equipo + `BKnob` + `BPointer`) siguen el mismo patrón:
slots de entrada tipados (`BStatusBoolean`/`BStatusNumeric`) + un slot `out` (`BFrozenEnum`) + un método
`changed()` que implementa la lógica combinatoria.

### BEquipment — 3 entradas booleanas → 4 estados

```java
// BEquipment.java:14-35 (propiedades declaradas con @NiagaraProperties)
@NiagaraProperty(name = "out",       type = "BEnum",          flags = 9)  // read+write, export
@NiagaraProperty(name = "demand",    type = "BStatusBoolean", flags = 8)  // read+write
@NiagaraProperty(name = "isRunning", type = "BStatusBoolean", flags = 8)
@NiagaraProperty(name = "inAlarm",   type = "BStatusBoolean", flags = 8)
```

`[CERT]` `com/tridiumemea/extras/BEquipment.java:14-35`.

Lógica en `changed()`:
```java
// BEquipment.java:82-95
if (this.getInAlarm().getValue()) {
    this.setOut(BEquipmentState.alarm);     // PRIORIDAD MÁXIMA: alarma anula todo
} else {
    int enumIndex = this.getIsRunning().getValue() ? 2 : 0;
    enumIndex = this.getDemand().getValue() ? enumIndex + 1 : enumIndex;
    this.setOut(BEquipmentState.make(enumIndex));
    // 0→idle, 1→actuate, 2→running, 3→running+demand
}
```

`[CERT]` `com/tridiumemea/extras/BEquipment.java:82-95`.

### BDamperEquipment — 4 entradas → 5 estados (agrega `demandMidway`)

Extiende `BEquipment` en lógica: cuando `demand=true && isRunning=true && demandMidway=true` → estado
`midway` (posición parcialmente abierta), en lugar de `open`. `[CERT]`
`com/tridiumemea/extras/BDamperEquipment.java:96-114`.

Estados `BDamperState`: `closed` / `actuate` / `alarm` / `open` / `midway`.
`[CERT]` `com/tridiumemea/extras/enums/BDamperState.java:11-14`.

### BLoHiEquipment — 6 entradas booleanas → 7 estados

Modela equipos de dos velocidades (Lo y Hi). Las 6 entradas son `demandLo`, `isRunningLo`, `inAlarmLo`,
`demandHi`, `isRunningHi`, `inAlarmHi`. El estado codifica cuál speed está activa y en qué condición:

| Estado | Significado |
|---|---|
| `idle` | ninguna speed activa |
| `actuateLo` / `alarmLo` / `runningLo` | sólo Lo activa |
| `runningLoActuateHi` / `runningLoAlarmHi` / `runningLoHi` | Lo activa + Hi en distintos sub-estados |

`[CERT]` `com/tridiumemea/extras/enums/BLoHiStates.java:11-13` (range con 7 valores).
`[CERT]` `com/tridiumemea/extras/BLoHiEquipment.java:125-141` (lógica `changed()`).

### BDualSwitch — 2 entradas booleanas → 4 estados por bit-encoding

```java
// BDualSwitch.java:72-75
int enumIndex = (switchOne ? 1 : 0) + (switchTwo ? 2 : 0);
this.setOut(BDualSwitchState.make(enumIndex));
```

`[CERT]` `com/tridiumemea/extras/BDualSwitch.java:72-75`. Los 4 estados representan las combinaciones
de dos interruptores independientes (00/01/10/11).

### BKnob y BPointer — 1 entrada numérica → posición angular cuantizada

Ambos aceptan un `BStatusNumeric` y lo mapean a una posición visual cuantizada:

- **BKnob**: `knobPosition` (0-100%) → 8 posiciones en pasos de 45° (`position270`→`position225`).
  Umbral de cuantización: bandas de 12.5%.
  `[CERT]` `com/tridiumemea/extras/BKnob.java:54-82`.

- **BPointer**: `measuredInput` (0-100%) → 9 posiciones angulares de −90° a +90°.
  Bandas de 12.5%.
  `[CERT]` `com/tridiumemea/extras/BPointer.java:55-80`.
  (Nota: los nombres de constantes `pointer$2b90`, `pointer$2d90`, etc. son artifacts de Vineflower
  al decompilat identificadores con caracteres especiales como `+`/`−` en el bytecode.)

---

## 422.4 — Por qué código y no BOG: la respuesta `[CERT]` + `[INFER]`

Esta es la pregunta central que [Bloque 203] §203.4 dejó abierta.

**Los packs BOG puros (Graphics, Hvac, N4svg) resuelven un problema de 1-entrada → N-imágenes:**

| Pack | Entradas | Mecanismo | ¿Código? |
|---|---|---|---|
| kitPxGraphics | 1 boolean | `IBooleanToSimple` (On.gif/Off.png) | No — BOG puro |
| kitPxHvac | 1 boolean | `IBooleanToSimple` | No — BOG puro |
| kitPxN4svg | 1 numeric o boolean | `NumericToSimpleMap` / `IBooleanToSimple` | No — BOG puro |
| **kitPxBuilding** | **2-6 entradas tipadas** | `BComponent.changed()` | **Sí — código Java** |

`[CERT]` para los packs BOG: [Bloque 203] §203.2 (Graphics/Hvac, `IBooleanToSimple`), §203.3 (N4svg,
`NumericToSimpleMap`). `[CERT]` para kitPxBuilding: §422.3 (este bloque).

**La razón fundamental tiene dos partes:**

**Parte A — Multi-input sin BOG converter nativo.** Un `ValueBinding` BOG acepta exactamente UN valor
como entrada y aplica UN converter. No existe un converter estándar de Niagara que tome 3 booleanos
con lógica de prioridad (`inAlarm` anula siempre) y produzca un enum de 4 estados. `[INFER]`

**Parte B — El output sí usa la misma maquinaria.** Críticamente, el código NO agrega lógica de
rendering: el slot `out: BFrozenEnum` del BComponent es un dato enum estándar que CUALQUIER página PX
puede bindear usando los mismos converters (`FrozenEnumToSimple`, `NumericToSimpleMap`) ya conocidos
de los packs BOG. `[INFER]` — la separación es intencionada: el BComponent resuelve la agregación;
la palette BOG del `-wb` (`module.palette`) resuelve la visualización binding ese slot `out`.

**En resumen**: el código tipado existe porque `BComponent.changed()` es el único mecanismo de Niagara
para combinar múltiples slots de entrada con lógica condicional de prioridad y producir un único slot
de salida. Una vez que el `out` existe, la visualización es idéntica a los packs BOG puros. `[INFER]`

---

## 422.5 — La capa `-ux`: `BBajaScriptTypeExt` en lugar de `BSingleton+BIJavaScript` `[CERT]`

El perfil `-ux` tiene 2 clases y usa un patrón diferente al de [Bloque 421] (`webEditors-ux`):

**`BKitPxBuildingJsBuild`** — singleton, registra el bundle JS:

```java
// BKitPxBuildingJsBuild.java:13-15
public static final BKitPxBuildingJsBuild INSTANCE = new BKitPxBuildingJsBuild(
    "kitPxBuilding", new BOrd[]{BOrd.make("module://kitPxBuilding/rc/kitPxBuilding.built.min.js")}
);
```

`[CERT]` `com/tridiumemea/extras/ux/BKitPxBuildingJsBuild.java:13-15`.

A diferencia de `BWebEditorsJsBuild` (B421 §421.6), este `BJsBuild` no declara dependencias en otros
builds — es un bundle independiente.

**`BKitPxBuildingIEnumTypeExt`** — TypeExt agent sobre el tipo base:

```java
// BKitPxBuildingIEnumTypeExt.java:13-17
@NiagaraType(
   agent = {@AgentOn(types = {"kitPxBuilding:KitPxBuildingBaseEnum"})}
)
@NiagaraSingleton
public class BKitPxBuildingIEnumTypeExt extends BBajaScriptTypeExt {
```

`[CERT]` `com/tridiumemea/extras/ux/BKitPxBuildingIEnumTypeExt.java:13-22`.

La diferencia clave con B421: aquí no se usa `BIJavaScript` (interfaz de field editors) sino
`BBajaScriptTypeExt` (extensión de tipo en la capa BajaScript del cliente web). Un `TypeExt` agente
registrado sobre `KitPxBuildingBaseEnum` se aplica automáticamente a TODAS las subclases
(`BEquipment`, `BDamper`, `BKnob`, etc.) — un único agente cubre todo el árbol de tipos del pack.

El `JsInfo` apunta a `module://kitPxBuilding/rc/KitPxBuildingIEnum.js`. `[CERT]`
`com/tridiumemea/extras/ux/BKitPxBuildingIEnumTypeExt.java:22`.

| Dimensión | `webEditors-ux` (B421) | `kitPxBuilding-ux` (este bloque) |
|---|---|---|
| Patrón | `BSingleton + BIJavaScript` | `BSingleton + BBajaScriptTypeExt` |
| Registro | `@AgentOn` por tipo concreto | `@AgentOn` sobre tipo base (cubre subtipos) |
| Propósito | Field editor de property sheet | TypeExt de cliente web para enum |
| Un agente por | 1 tipo baja | Todo el árbol `KitPxBuildingBaseEnum` |

---

## 422.6 — Los 6 tipos enum: `BFrozenEnum` con `@NiagaraEnum` range `[CERT]`

Todos los enums de estado heredan de `BFrozenEnum` con `@NiagaraEnum(range={...})`. Tabla completa:

| Enum | Estados | Componente que lo usa |
|---|---|---|
| `BEquipmentState` | idle · actuate · alarm · running | `BEquipment` |
| `BDamperState` | closed · actuate · alarm · open · midway | `BDamperEquipment` |
| `BDualSwitchState` | (4 estados bit-encoded) | `BDualSwitch` |
| `BLoHiStates` | idle · actuateLo · alarmLo · runningLo · runningLoActuateHi · runningLoAlarmHi · runningLoHi | `BLoHiEquipment` |
| `BKnobPosition` | position270 · position315 · position0 · position45 · position90 · position135 · position180 · position225 | `BKnob` |
| `BPointerStates` | 9 posiciones angulares (−90° a +90°) | `BPointer` |

`[CERT]` registros NRE en `module.xml`: los 6 enums aparecen como `<type class="..."/>` con sus nombres
cortos (DamperState, EquipmentState, etc.). `[CERT]` `kitPxBuilding-rt/extracted/META-INF/module.xml`.

Cada enum es un `BFrozenEnum` inmutable de Niagara: las constantes son instancias `static final`
construidas con el ordinal y recuperadas con `make(int)` / `make(String)`.

---

## 422.x — Connections

- **[Bloque 203]** (la excepción): este bloque responde lo que B203 §203.4 dejó como hipótesis
  (`[INFER]` "widgets de equipo compuestos con ESTADO tipado... más cercano a los widgets kitPx reales").
  Confirmado: el código existe por la lógica multi-input en `changed()`, imposible en BOG puro.

- **[Bloque 421]** (webEditors-ux, `BSingleton+BIJavaScript`): contraste en el patrón `-ux`. Los field
  editors de `webEditors` usan un agente `BIJavaScript` por tipo baja; `kitPxBuilding-ux` usa
  `BBajaScriptTypeExt` — un solo agente sobre el tipo base abstracto que cubre todo el árbol. La
  diferencia refleja propósitos distintos: field editor vs TypeExt de rendering para un enum tipado.

- **[Bloque 192]** (widgets bajaui): los BComponent de kitPxBuilding no son widgets en sí mismos —
  son datos en el árbol de la estación. Los widgets PX que los visualizan (en `module.palette` del
  `-wb`) serán widgets bajaui/kitPx que bindean el slot `out` con los mismos converters de B192/B193.

- **[Bloque 193]** (ValueBinding + converters): la limitación de 1-input de `ValueBinding` es la razón
  directa por la que kitPxBuilding necesita código. Los packs BOG la evitan usando equipos de señal
  única; kitPxBuilding la supera con un `BComponent` de pre-agregación.
