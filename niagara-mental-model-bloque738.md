# B738 · Practical authoring how-to — adding a proxy ext, facets, `propagateFlags`, and an icon/SVG to a block

> **Scope**: concrete "how do I add X" recipes for a custom rt component: a proxy extension, facets
> (units/precision), a `propagateFlags` status mask, and a block icon (PNG or SVG). Applies the mechanics of
> B735 (facets/slots), B734 (points/proxyExt), B736 (BStatus). Answers direct authoring questions.
>
> **Sources**: FUENTE 3 docSource `extracted/baja/javax/baja/sys/BIcon.java`,
> `extracted/kitControl-rt/com/tridium/kitControl/BKitNumeric.java`,
> `extracted/control-rt/javax/baja/control/{BControlPoint,ext/BAbstractProxyExt}.java`,
> exemplars `BSequence`/`BTimeTrigger` (`getIcon`) — read this session. FUENTE 1: B730/B734/B735/B736.

---

## 738.1 — Adding a proxy ext (field binding) `[CERT]`
`proxyExt` is a frozen child slot on a `BControlPoint`, typed `BAbstractProxyExt`, default `BNullProxyExt`
(`BControlPoint.java:100-102`). To bind a point to a field you set `proxyExt` to a DRIVER's concrete proxy
ext (BACnet/Modbus/NRIO — each ships a `B<Proto>ProxyExt extends BAbstractProxyExt` with
`readSubscribed/readUnsubscribed/write`, async IO — B730 §730.8). You don't write your own unless you author
a driver.
- **Practically**: in Workbench you drag a proxy point from the driver palette (it comes with its proxyExt),
  or add the ext under a point. In code, a driver point declares `proxyExt` typed to its proxy ext.
- **For OUR modules**: we are NOT points, so we don't hold a proxyExt — we BLink our boolean/numeric slots to
  the driver's proxy points, which own the binding (B734 §734.5). If we ever needed a component to talk to a
  field directly, we'd model it as a driver point with a proxy ext, not bolt IO onto a control component.

## 738.2 — Adding facets (units / precision) `[CERT, recap B735]`
Three ways:
1. **At declaration**: `@NiagaraProperty(name="coilTemp", type="BStatusNumeric",
   facets=@Facet("BFacets.make(BFacets.UNITS, BUnit.getUnit(\"celsius\"), BFacets.PRECISION, BInteger.make(1))"))`
   — or the helpers `BFacets.makeNumeric(units, min, max, precision)`, `makeInt(range,min,max)`.
2. **On the slot in the AUTO region**: `newProperty(flags, default, BFacets.make(...))`.
3. **Dynamically**: override `getSlotFacets(Slot)` (`BComplex.java:451`) to project one `facets` config slot
   onto many outputs (B730 §730.6, `BSequence.getSlotFacets:894`).
Keys (B735 §735.2): `UNITS`, `PRECISION`, `MIN`/`MAX`, `RANGE`, `TRUE_TEXT`/`FALSE_TEXT`, `ALLOW_NULL`,
`FIELD_EDITOR`/`UX_FIELD_EDITOR`, `MAX_OVERRIDE_DURATION`. **For us**: put `UNITS`+`PRECISION` on temp/
pressure/percent slots (today bare doubles) so the HMI and links carry engineering units.

## 738.3 — `propagateFlags` (status propagation mask) `[CERT]`
`BKitNumeric` (kitControl base for numeric blocks) declares
`@NiagaraProperty(name="propagateFlags", type="BStatus", defaultValue="BStatus.ok")` (`BKitNumeric.java:35`).
It is a **`BStatus` MASK** the operator sets to choose WHICH input status bits (fault/down/stale/null…, B736)
are carried through to the output. The block, in its execute/`changed`, ANDs the incoming status against
`propagateFlags` before `out.setStatus(...)` — so a site can decide, without code, e.g. "propagate fault but
not stale." **To add it to our components**: declare a `propagateFlags` `BStatus` slot (SUMMARY|OPERATOR),
and when we OR/propagate input status to an output (B736 §736.3), mask it by `getPropagateFlags()`. Gives
operators the same control kitControl blocks have.

## 738.4 — Adding an icon / SVG to a block `[CERT]`
Override `getIcon()` to return a cached `BIcon` (exemplars: `BSequence`, `BTimeTrigger`:
`public BIcon getIcon(){ return icon; } private static final BIcon icon = BIcon.std("control/control.png");`).
- **`BIcon.std(fileName)`** resolves to the ord **`"module://icons/x16/" + fileName`** (`BIcon.java:69-71`) —
  i.e. the image is a **MODULE RESOURCE** under `icons/x16/…` (ship `x16` and `x32` sizes for raster). So:
  1. put `myicon.png` in your module's `icons/x16/` (and `icons/x32/`) resource dir (bundled into the jar);
  2. `private static final BIcon icon = BIcon.std("<myModule>/myicon.png");` (or just the filename if at the
     icons/x16 root); 3. `public BIcon getIcon(){ return icon; }`.
- **SVG (vector, one scalable file)**: reference it by a full ord with `BIcon.make(BOrd)` /
  `BIcon.make(String ordList)` (`BIcon.java:40-56`) — e.g. `BIcon.make("module://<myModule>/icons/myicon.svg")`.
  N4 renders SVG icons; SVG avoids maintaining x16/x32 rasters. `[CERT for BIcon.make; INFER that SVG is the
  preferred vector path — confirm the exact ord form against a shipping SVG icon]`.
- `BIcon.make(BOrdList)` also composes LAYERED icons (a base + an overlay badge), used for state glyphs.
- Cache the BIcon in a `static final` field (all exemplars do) — never build it per call.

**For us**: give `BColdRoom`/`BEvaporatorUnit`/`BCompressorControl`/`BDefrostController` distinct icons so the
tree/wire sheet is readable — a module resource + a 3-line `getIcon()` each.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | proxyExt is a frozen BAbstractProxyExt child on a BControlPoint; concrete exts come from drivers | [CERT] | BControlPoint.java:100-102; B734 |
| 2 | Facets added via @Facet/BFacets.make at declaration, or getSlotFacets override | [CERT] | B735 §735.2; BComplex.java:451 |
| 3 | propagateFlags is a BStatus mask slot (BKitNumeric) selecting which status bits propagate to the output | [CERT] | BKitNumeric.java:35,82 |
| 4 | getIcon() returns a cached BIcon; BIcon.std(name) → ord "module://icons/x16/"+name (module resource) | [CERT] | BIcon.java:69-71; BSequence/BTimeTrigger getIcon |
| 5 | BIcon.make(ord/ordList) references an arbitrary image (SVG) and can layer icons | [CERT] | BIcon.java:40-56 |
| 6 | Recommendation: units/precision facets + distinct icons + propagateFlags on our components | [INFER] | applies §738.2-4 to our modules |

**Tally**: 5 [CERT], 1 [INFER]. No unmarked claims.

## Connections
- **B734** (proxyExt/points), **B735** (facets/slots/links), **B736** (BStatus — what propagateFlags masks),
  **B730** §730.6 (getSlotFacets), module-anatomy (module resources / jar packaging).

## Open gaps
- **B738-G1**: the exact SVG ord form + N4 SVG-icon rendering support level — stated by intent; confirm
  against a shipping SVG icon resource.
- **B738-G2**: authoring a custom proxy ext (a real driver) — out of scope; only relevant if we build a driver.
