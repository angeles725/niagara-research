# B734 · Niagara point types — the 4×2 taxonomy, point vs writable (priority array), proxy vs local, point extensions, and how our custom-component design relates

> **Scope**: the differences between the Niagara point types and the related distinctions (read-only point
> vs writable, proxy vs local, the point-extension machinery) — and, as an APPLICATION note, how our
> refrigeration modules (which use `BStatusNumeric`/`BStatusBoolean` PROPERTIES on custom `BComponents`,
> not points) relate to that model. Automatic campaign continuation. Mechanics cited to B14/B104/B116/B234
> etc.; NEW = the consolidated comparison + our-design placement.
>
> **Sources**: FUENTE 3 docSource `extracted/control-rt/javax/baja/control/` (`BControlPoint`,
> `B{Numeric,Boolean,Enum,String}{Point,Writable}`, `BIWritablePoint`, `WritableSupport`, `BPointExtension`)
> read this session. FUENTE 1: B14/B15/B104/B116/B118/B137/B148/B234/B248 (points); B4 (slots); B729-B733.

---

## 734.1 — What EVERY control point has (`BControlPoint`, abstract) `[CERT]`

`BControlPoint.java`:
- **an `out` property of type `BStatusValue`** (:43) — the point's current value+status. (Contrast our
  custom components: many `BStatusNumeric` properties, no single `out`.)
- **`facets`** (:90) applied to `out` — units, precision, range, enum range.
- **`proxyExt`** (`BAbstractProxyExt`, default `BNullProxyExt`, :100) — the field binding.
- **point extensions** run in slot-declaration order; each updates a working var via
  `PointExtension.onExecute`; the final working var sets `out` (:73-76). Extensions = proxy, **alarm**
  (`BAlarmSourceExt`, B732), **history** (`BHistoryExt`, B731/§733), control/interval.

So a control point is a value + facets + a binding + a pipeline of extensions. That machinery is exactly
what a plain `BStatusNumeric` property does NOT have.

## 734.2 — The 4×2 type matrix `[CERT]`

Concrete types = {Numeric, Boolean, Enum, String} × {Point, Writable}:

| Value | Read-only (monitor) | Writable (command) |
|---|---|---|
| Numeric | `BNumericPoint` | `BNumericWritable` |
| Boolean | `BBooleanPoint` | `BBooleanWritable` |
| Enum | `BEnumPoint` | `BEnumWritable` |
| String | `BStringPoint` | `BStringWritable` |

(`BDiscretePoint` also exists; writables share `BIWritablePoint` + `WritableSupport`.)

## 734.3 — Point (read-only) vs Writable — the key difference `[CERT]`

- **Point (read-only)**: `out` is a MONITORED/computed value — set by the proxy read, or by a single input
  link. No arbitration. Use for sensors and computed readings (a temperature, a status).
- **Writable**: a COMMANDED value with a **16-level priority array** (`in1..in16`) + `fallback` +
  override-expiration (`BNumericWritable.java:30,42-…`, `WritableSupport`). BACnet priority-array semantics:
  multiple sources write at different priorities; the highest active (non-null) level wins; `fallback` when
  all null. Use for OUTPUTS / setpoints commanded by several actors (operator override, schedule, program,
  emergency). This is the Tridium answer to "who wins when several things want to set this output" — which
  our HOA-double solves by hand for the single-source case (B731 §731.4).

## 734.4 — Proxy vs local `[CERT]`

- **Proxy point**: `proxyExt` ≠ `BNullProxyExt` → bound to a field device (BACnet/Modbus/NRIO). `out`
  reflects the field read; a writable proxy pushes the winning command to the field (async IO, B730 §730.8).
  This is where field alarms/history belong (B732).
- **Local point**: `proxyExt == BNullProxyExt` → logic-only soft value on the wire sheet, no field.

## 734.5 — How our modules relate `[CERT/INFER]`

Our rt components (`BColdRoom`, `BEvaporatorUnit`, `BCompressorControl`) are **custom `BComponents` with
`BStatusNumeric`/`BStatusBoolean` PROPERTIES**, NOT `BControlPoint`s `[CERT]`. Consequences:
- **No `out`/facets/proxyExt/extension pipeline** → cannot attach alarm/history/proxy extensions to our
  values directly (this is exactly why B732 puts alarms on the driver proxy points, and B731/§733 puts
  history there too).
- **No priority array** → our HOA is a hand-rolled `double` 0/1/2 instead of a writable's `in1..in16`
  (deliberate, to avoid cross-module enum deps — B731 §731.4).
- **We bind to the field indirectly**: our boolean output slots (`valveOut`/`evapOut`/`resistanceOut`) are
  BLinked to SEPARATE writable proxy points in the driver, which own the field binding — rather than our
  component BEING the point.

**Why this is a reasonable architecture** `[INFER]`: modeling a "cold room with N evaporators + a defrost
controller" as ONE structured `BComponent` tree captures domain relationships (interlock, staging, defrost
sequencing) that a flat bag of points cannot. The cost is forgoing the free point machinery (priority
array, extensions, facets) — which we re-acquire where it matters by LINKING to proper proxy points (field
IO, alarms, history live there). This is the standard "equipment component orchestrating linked points"
pattern (station-organization focus B716-720): logic in equipment components, field/alarm/history/trend on
the points.

**Practical rules of thumb for us**:
- A value the FIELD owns (sensor reading, a relay we command) → a proxy POINT in the driver (alarms/history
  attach there).
- A value MULTIPLE actors command (a setpoint an operator + schedule + program may all set) → a WRITABLE.
- A value OUR logic computes and owns (defrostActive, a staging decision) → a `BStatus*` property on our
  component (what we do today).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Every BControlPoint has an `out` (BStatusValue), `facets`, `proxyExt`, and an ordered extension pipeline | [CERT] | BControlPoint.java:43,90,100,73-76 |
| 2 | Point types = {Numeric,Boolean,Enum,String}×{Point,Writable} (+BDiscretePoint), writables share BIWritablePoint/WritableSupport | [CERT] | control/ file listing |
| 3 | Read-only point = single monitored/computed out; writable = 16-level priority array + fallback | [CERT] | BNumericWritable.java:30,42-…; WritableSupport |
| 4 | Proxy point (proxyExt≠NullProxyExt) is field-bound; local point (NullProxyExt) is logic-only | [CERT] | BControlPoint.java:96-102 |
| 5 | Our components use BStatus* properties, not BControlPoints → no out/extension/priority machinery; we link to proxy points for field/alarm/history | [CERT] | our rt source; B731/B732/§733 |
| 6 | The equipment-component-orchestrating-linked-points split is the reasonable pattern for us | [INFER] | station-organization B716-720; no counter-evidence |

**Tally**: 5 [CERT], 1 [INFER]. No unmarked claims.

## Connections
- **B4** (slot system), **B731** (HOA vs priority array), **B732** (alarms on points), **B733** (writable→AO),
  **station-organization B716-720** (equipment vs points), points depth B14/B104/B116/B234.

## Open gaps
- **B734-G1**: the exact priority-array resolution + relinquish-default semantics of `WritableSupport`
  (in1-in16 ordering, emergency levels) — covered in B137; not re-derived here.
- **B734-G2**: `BAbstractProxyExt` read/write/subscribe lifecycle detail — cite B730 §730.8 / driver blocks.
