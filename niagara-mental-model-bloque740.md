# B740 · Frozen enums — how to author them, and why a shared enum linked across two custom modules breaks (`Missing class ColdRoomPan:HoaMode`, [CERT-live])

> **Scope**: how Tridium declares a frozen enum (`BFrozenEnum` + `@Range`), and the concrete pitfall we hit —
> linking an enum-typed value ACROSS two custom modules forces a cross-module `-rt` dependency, and a leftover
> reference to a deleted enum takes the station to a `Missing class` fault. Ends with the safe rule (plain
> double for cross-module links). Grounded in the PANCCADIA León `HoaMode` incident.
>
> **Sources**: FUENTE 3 docSource `extracted/baja/javax/baja/sys/{BFrozenEnum,BEnumRange}.java` (read this
> session). FUENTE (empirical [CERT-live]) `Missing class ColdRoomPan:HoaMode` on the live JACE; kit
> `types/logic.md` + bitácora 5cuartos §5. FUENTE 1: enum coverage B23/B33/B34/B100/B141/B242.

---

## 740.1 — Authoring a frozen enum `[CERT]`
A frozen enum is a `BFrozenEnum` subtype (`javax.baja.sys.BFrozenEnum`) — an immutable ordinal into a fixed
range. Pattern:
- `@NiagaraType` on the class; `@Range` lists the tags (e.g. `@Range({"auto","hand","off"})`).
- Slotomatic generates ordinal constants (`AUTO=0, HAND=1, OFF=2`), the `make(int)`/`make(String)` factories,
  and the singleton instances (`auto`, `hand`, `off`). The `range` (a `BEnumRange` from the `EnumType`) maps
  ordinal↔tag (`BFrozenEnum` reads `((EnumType)getType()).getTag(ordinal)`).
- Facet `range` (B735) on a slot restricts an enum property to that range; the field editor shows the tags.
Intra-module (declared and used inside ONE module) this is clean and idiomatic.

## 740.2 — The cross-module link pitfall `[CERT-live]`
A frozen enum is a TYPE. **A BLink carrying that enum requires the IDENTICAL type on BOTH ends** — so if
module B links an enum value from module A, **module-B-rt must depend on module-A-rt** to have that class.
And the custom-module dependency DSL is non-trivial: `compileOnly(files(...))` compiles but **does not reach
the plugin/runtime classpath**, and the niagara gradle plugin auto-includes only TRIDIUM modules, not your
sibling custom module. So the dependency is easy to get wrong, and a dangling enum reference does not fail at
build — it fails at STATION LOAD as a **`Missing class <module>:<EnumType>`** fault. We hit exactly this:
after removing a `BHoaMode` enum, a leftover reference surfaced live on the JACE as
**`Missing class ColdRoomPan:HoaMode`** (bitácora 5cuartos §5).

## 740.3 — The safe rule `[CERT-live]`
**For a value linked ACROSS custom modules, use a plain `double`/`int` (e.g. 0/1/2), NEVER a shared
frozen-enum type.** A `double` links with ZERO cross-module dependency; Workbench simply shows 0/1/2. This is
exactly why our HOA mode is a `double` (auto=0/hand=1/off=2) rather than a `BHoaMode` enum (B731 §731.4,
types/logic.md). Keep frozen enums for INTRA-module use (both link ends in the same module), where the type
is always present. Corollary: **never delete an enum type still referenced** by a slot default, a link, or a
`.bog` value — that is the `Missing class` trigger; remove all references first (and remember §B739: a
saved enum value can also complicate a schema change).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | A frozen enum = BFrozenEnum subtype + @Range; ordinal↔tag via EnumType/BEnumRange | [CERT] | BFrozenEnum.java; BEnumRange.java |
| 2 | An enum-typed link needs the identical type on both ends → forces module-B-rt to depend on module-A-rt | [CERT/INFER] | Baja link type-identity; §740.2 |
| 3 | Custom-module dep DSL is non-trivial (compileOnly(files) misses the plugin classpath; plugin auto-includes only Tridium) | [CERT-live] | bitácora 5cuartos §5 |
| 4 | A dangling/deleted enum ref fails at station LOAD as `Missing class <mod>:<Enum>`, not at build | [CERT-live] | live JACE `Missing class ColdRoomPan:HoaMode` |
| 5 | Safe rule: cross-module links use a plain double/int; keep enums intra-module; never delete a referenced enum | [CERT-live] | our HOA-as-double; the incident |

**Tally**: 4 [CERT/CERT-live], 1 [CERT/INFER]. No unmarked claims.

## Connections
- **B731** §731.4 (HOA double vs priority array), **B739** (schema/.bog — deleting/retyping references),
  **B735** (enum `range` facet), types/logic.md (the rule already captured); enum depth B23/B33/B34/B100/B141/B242.

## Open gaps
- **B740-G1**: the correct gradle DSL to declare a real cross-module `-rt` dependency (when an enum link is
  genuinely required intra-plant) — the plugin's supported form; not opened (we avoid it via the double rule).
