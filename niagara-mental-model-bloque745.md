# B745 · Niagara units — `BUnit` / `UnitDatabase`, the `units` facet, and how to put °C / kPa / % on a numeric slot (authoring doc)

> **Scope**: the Niagara unit system and the concrete recipe to give a numeric slot engineering units +
> precision — closing the recurring "our temps/pressures are bare doubles" recommendation (B731/B735/B738)
> as a documentation reference. Java-8, docSource-clean. No module changes — doc only. Foco: module-best-practices.
>
> **Sources**: FUENTE 3 docSource `extracted/baja/javax/baja/units/{BUnit,UnitDatabase}.java`,
> `extracted/baja/javax/baja/sys/BFacets.java` (read this session). FUENTE 1: B735 (facets), B4 (slots),
> units mentioned in B17/B36/B40/B118/B122/B271/B272/B389 (no authoring block until now).

---

## 745.1 — The model `[CERT]`
- **`BUnit`** (`javax.baja.units.BUnit`, `final`) is one unit: a name, a symbol, a `BDimension`, and a scale
  (`getUnitName()`, `getSymbol()`, `getDimension()`, `:245-280`). Look one up by name:
  **`BUnit.getUnit("celsius")`** (`:79`). Define a custom one with `BUnit.make(name, symbol, dimension, scale…)`
  (`:106-192`). `BUnit.NULL` = no unit. (Offset units like **celsius** are special-cased — the class
  comment calls them out, `:378-392` — because °C↔K is an OFFSET, not just a scale.)
- **`UnitDatabase`** (`javax.baja.units.UnitDatabase`) is the registry, loaded once via
  `UnitDatabase.getDefault()` from a units XML (`unitdb.elems("quantity")`, `:136`). Units are grouped by
  **Quantity** (temperature, pressure, dimensionless…): `getQuantities()` (`:78`), `getQuantity(BUnit)`
  (`:90`), and `getUnit(name)` → `BUnit.getUnit(name)` (`:38`). To discover exact unit-name strings on this
  install: `UnitDatabase.getDefault().getQuantities()` / dump, or read the units XML resource.

## 745.2 — The `units` facet `[CERT]`
Units reach a slot through facets (`BFacets.java`): **`UNITS`** = `"units"` (a `BUnit`, `:85`),
**`PRECISION`** (decimals), **`UNIT_CONVERSION`** (`:159`), **`SHOW_UNITS`** (`:166`). The canonical builder
is **`BFacets.makeNumeric(BUnit unit, BInteger precision, BNumber min, BNumber max)`** (`:262`) — it packs
UNITS+PRECISION+MIN+MAX; `unit==null → BUnit.NULL`.

## 745.3 — Recipe: put °C / kPa / % on a slot `[CERT/INFER]`
Declaration (Java 8), on a numeric property:
```java
@NiagaraProperty(
  name = "coilTemp", type = "BStatusNumeric",
  defaultValue = "new BStatusNumeric()",
  facets = @Facet("BFacets.make(BFacets.UNITS, BUnit.getUnit(\"celsius\"), BFacets.PRECISION, BInteger.make(1))"))
```
or, for a plain numeric, `newProperty(flags, default, BFacets.makeNumeric(BUnit.getUnit("celsius"), BInteger.make(1), null, null))`.
Dynamic projection onto many outputs: override `getSlotFacets(Slot)` (B735 §735.2 / B730 §730.6).

Likely unit-name strings for us (`"celsius"` is `[CERT]` from the BUnit comment; the rest are `[INFER]` —
confirm against `UnitDatabase.getQuantities()` / the units XML on the install):
- **Temperature** → `"celsius"`.
- **Pressure** → a pressure-quantity unit such as `"kilopascal"` / `"pascal"` / `"bar"` / `"pounds_per_square_inch"`.
- **Percent / dimensionless** → `"percent"`.

Notes: for °C/°F, the OFFSET handling means don't hand-roll conversions — let the facet/`UNIT_CONVERSION`
and the field editor do it. `SHOW_UNITS` toggles whether the symbol renders next to the value.

## 745.4 — Application to our modules `[CERT/INFER]`
Our numeric slots (room/coil/resistance temps, suction/discharge pressures, demand %, setpoints) are largely
**bare doubles / BStatusNumeric without a `units` facet** (B731/B735) → the HMI, the property sheet, and links
show a naked number. Adding `units`+`precision` facets (this recipe) makes every surface render engineering
units and gives operators correct editors — a low-risk, high-legibility change. It is **additive** (a facet
on an existing slot is not a type change) so it is schema-safe (B739): no retype, no `.bog` risk. Bundle into
the Batch-3 readability refactor (B742). Doc-only for now, as requested.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BUnit is a final unit (name/symbol/dimension/scale); BUnit.getUnit("celsius") looks up by name; BUnit.NULL = none | [CERT] | BUnit.java:64,79,106-192,245-280 |
| 2 | UnitDatabase.getDefault() loads units (from XML) grouped by Quantity; getUnit/getQuantities | [CERT] | UnitDatabase.java:38,52,78,90,136 |
| 3 | Units set via the `units` facet (a BUnit) + precision; BFacets.makeNumeric(unit,precision,min,max) | [CERT] | BFacets.java:85,159,166,262 |
| 4 | Offset units (celsius) are special-cased — don't hand-roll °C conversions | [CERT] | BUnit.java:378-392 comment |
| 5 | "celsius" is a valid unit name; pressure/percent names to confirm against the install's UnitDatabase | [CERT/INFER] | celsius [CERT]; others [INFER] |
| 6 | Adding a units facet is additive/schema-safe (not a type change) | [CERT] | B739 (add-don't-retype); facet ≠ type |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims.

## Connections
- **B735** (facets/slots), **B738** (getSlotFacets/how-to), **B736** (BStatusNumeric carries the value),
  **B731/B742** (our bare-double gap + the refactor batch), **B739** (facet change is schema-safe). Units in B4/B36/B40/B122.

## Open gaps
- **B745-G1**: the exact pressure/percent unit-name strings + the quantity list on this install — a one-shot
  `UnitDatabase.getDefault().getQuantities()` dump (or read the units XML) would make them [CERT]; deferred
  (doc-only pass).
