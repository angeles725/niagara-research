# B736 · The `BStatus` bit model — the 8 status flags, `isValid`/`isOk`, immutable `make*` factories, propagation, and how our modules should set/consume them

> **Scope**: the `BStatus` flag model that rides on every `BStatusValue` (BStatusNumeric/Boolean/Enum) — the
> 8 bits, the trust gate (`isValid`), the immutable builders, and how status aggregates/propagates — plus
> the APPLICATION note for our refrigeration modules (we handle degradation well already; this documents the
> full model and where OVERRIDDEN/NULL/STALE should be set). Foundational companion to B730 §730.3
> (degradation) and B732 (alarms). 86 blocks mention BStatus; none synthesizes the authoring model.
>
> **Sources**: FUENTE 3 docSource `extracted/baja/javax/baja/status/BStatus.java` (read this session).
> FUENTE 1: B4 (slots), B730 (degradation idiom), B732 (alarm), B734 (points/out).

---

## 736.1 — What `BStatus` is `[CERT]`
`BStatus` is *"a bit mask for various standardized status flags in the Baja control architecture, plus
arbitrary extensions using BFacets"* (`BStatus.java` class javadoc). It is a **`final`, immutable** value
carried by every `BStatusValue`'s `out`/property. You never mutate it; you build a new one with a factory.

## 736.2 — The 8 flags `[CERT]` (`BStatus.java:702-716`)

| Bit | Hex | Meaning |
|---|---|---|
| `DISABLED` | 0x01 | the point/object is disabled (out of service) |
| `FAULT` | 0x02 | a fault (bad config, driver fault, algorithm error) |
| `DOWN` | 0x04 | the device/network is down (comms lost) |
| `ALARM` | 0x08 | currently in an alarm (offnormal) condition |
| `STALE` | 0x10 | the value is old (not refreshed within its window) |
| `OVERRIDDEN` | 0x20 | value is being forced (a writable at manual/emergency, a HAND override) |
| `NULL` | 0x40 | no valid value present |
| `UNACKED_ALARM` | 0x80 | an alarm exists that has not been acknowledged |

`isOk()` = `bits == 0` (`:299`). Per-bit accessors `isDisabled/isFault/isDown/isAlarm/isStale/isOverridden/
isNull(...)` (`:305-334`). **`isValid()`** (`:287`) is the trust gate — "is this value safe to use in
control" (invalid when fault/down/null/stale/disabled) — the exact check to run before consuming any input
(B730 §730.3).

## 736.3 — Immutable builders + aggregation `[CERT]`
- `BStatus.make(int bits)` / `make(orig, bit, boolean)` / typed `makeFault/makeDown/makeStale/makeOverridden/
  makeNull/makeAlarm/makeUnackedAlarm/makeDisabled(orig, state)` (`:51-140`) — each returns a NEW BStatus
  with that bit set/cleared; the original is untouched.
- `make(orig, name, value)` (`:149-175`) — attach an arbitrary BFacets extension (a named tag) to the status.
- **Aggregate/propagate**: OR the input bits and run through `propagate(...)` so fault/down/stale/null flow to
  the output (B730 §730.3: `out.setStatus(propagate(BStatus.make(a|b|c|d)))`, BQuadMath). This is how a bad
  sensor upstream turns the whole computed chain invalid downstream, automatically.

## 736.4 — Application to our modules `[CERT/INFER]`
- **Consume: gate every input with `isValid()`** before using it — we already do (10-12 hits, B730/B731).
  Keep it. A faulted/null probe must not drive a control decision.
- **Produce: set the right bit, don't just null**:
  - No data / sensor absent → `makeNull` (NULL) on the output, so downstream/HMI shows "no value," not 0.
  - Value forced by HOA HAND / a writable override → **`makeOverridden`** (OVERRIDDEN) so the HMI and any
    subscriber SEE it's forced. Today our HOA is a `double` mode; the affected OUTPUT slot should also carry
    the OVERRIDDEN status bit so operators can tell a forced output from an auto one at a glance. `[INFER]`
  - Driver comms lost on a linked input → the proxy point already sets DOWN/STALE; propagate it so our
    computed outputs go invalid rather than freezing on a stale reading.
  - ALARM/UNACKED_ALARM belong to the alarm source ext (B732), not hand-set on our status.
- **Propagate**: when we compute an output from inputs, OR the inputs' status onto the output (a room whose
  zone probe is faulted should emit its cooling command with FAULT/NULL, not a confident value).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BStatus is a final immutable bitmask + BFacets extensions | [CERT] | BStatus.java class javadoc; `final class` |
| 2 | The 8 bits: DISABLED/FAULT/DOWN/ALARM/STALE/OVERRIDDEN/NULL/UNACKED_ALARM (0x01..0x80) | [CERT] | BStatus.java:702-716 |
| 3 | isOk()=bits==0; per-bit isXxx accessors; isValid() is the trust gate | [CERT] | BStatus.java:287,299,305-334 |
| 4 | Immutable typed builders makeFault/makeNull/makeOverridden/… (orig,state) | [CERT] | BStatus.java:51-140 |
| 5 | Status aggregates via OR + propagate() so invalidity flows downstream | [CERT] | B730 §730.3 (BQuadMath.propagate) |
| 6 | Our modules gate inputs with isValid() (good); should also set OVERRIDDEN on forced outputs and NULL on no-data | [CERT/INFER] | our source (isValid present); OVERRIDDEN/NULL recommendation [INFER] |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims.

## Connections
- **B730** §730.3 (degrade-on-bad-input idiom), **B732** (ALARM/UNACKED via alarm ext), **B734** (out=BStatusValue),
  **B731** §731.2 (our degradation is present). BStatus mentioned across 86 blocks; this is the authoring synthesis.

## Open gaps
- **B736-G1**: exact bit set that `isValid()` tests (read `:287` body) — stated by intent here, not verbatim.
- **B736-G2**: status precedence for DISPLAY (which bit's color/glyph wins when several are set) — a
  bajaui/render concern, not opened.
