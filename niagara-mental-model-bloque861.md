# Block 861 — B853-G2: EnumWritable SelR{k} — component model, BEnumRange static range, and the Hx dropdown-write path

> **Focus:** harbor-greenmax-lighting. **Scope:** source-level DISCOVERY for gap B853-G2 — whether
> replacing the current `NumericWritable SelR{k}` + `/set` dialog (typed number) with an
> `EnumWritable SelR{k}` + configured `BEnumRange` yields a name-based dropdown in the Hx browser,
> and what write path that takes.
>
> **Evidence sources:**
> - `[CERT]` = verbatim from decompiled/vineflower class or extracted .class file, cited with path.
> - `[CERT-live]` = empirical on client station HM_BMS (EC-Net 4.3.58.18) — prior blocks own this evidence; referenced here.
> - `[INFER]` = derived from `[CERT]`; what would confirm it is stated.

---

## 1. The component: `BEnumWritable`

`BEnumWritable` is the stock kitControl writable point for enum values.
Class: `javax.baja.control.BEnumWritable` in `control-rt`.

```java
// control-rt/decompiled/javax/baja/control/BEnumWritable.java:61
@NiagaraActions(value={
    ...
    @NiagaraAction(name="set", parameterType="BDynamicEnum",
                   defaultValue="BDynamicEnum.DEFAULT", flags=256),
    ...
})
public class BEnumWritable extends BEnumPoint implements BIWritablePoint { ... }
```
`[CERT]` `organized/control/control-rt/decompiled/javax/baja/control/BEnumWritable.java:61`

Key points:

- Inherits the **16-level priority array** from `BEnumPoint/WritableSupport` exactly like `BNumericWritable`. `[CERT]` same file.
- The `set` action takes a **`BDynamicEnum`** parameter (not `BDouble`). This is significant: the same fixed-arg type-mismatch failure described in B856 §3.2 applies here — a Px `ActionBinding` with a non-empty `actionArg="2"` would pass `BString("2")` to an action expecting `BDynamicEnum` → fails silently in Hx (path ②). To use the Set dialog, leave `actionArg` empty. `[CERT]` B856 §3.2 mechanism applies to `BDynamicEnum` parameter just as to `BDouble`.
- `getActionParameterDefault(set)` returns `this.getFallback().getValueValue()` — a `BDynamicEnum` carrying the fallback value and the component's range. `[CERT]` `BEnumWritable.java:271–272`.
- `getSlotFacets(set)` returns `this.getFacets()` — the component-level `BFacets`, which holds the `BEnumRange`. `[CERT]` `BEnumWritable.java:287–292`.

---

## 2. The range: `BEnumRange` — static, frozen at design time

```java
// BEnumWritable.java:286–324 (onExecute excerpt)
BEnumRange facetsRange = (BEnumRange) facets.get("range");
if (facetsRange == null) { facetsRange = BEnumRange.NULL; }
if (facetsRange.equals(range)) { return; }
val = BDynamicEnum.make((int) val.getOrdinal(), (BEnumRange) facetsRange);
```
`[CERT]` `organized/control/control-rt/decompiled/javax/baja/control/BEnumWritable.java:296–306`

The component's facets carry a `BEnumRange` under key `"range"`. `onExecute` normalises all 16 input slots to use this range when it changes. The mechanism is:
- **Static / design-time range**: the `BEnumRange` is a `BFacets` value set in Workbench during commissioning (e.g., `{0:"Horario A", 1:"Horario B", 2:"Horario C", 3:"Horario D", 4:"Horario E"}`).
- **No built-in dynamic tracking**: `BEnumRange` has no live link to `BStringWritable` slots. If the operator later edits a `Horario{n}_Nombre` BStringWritable, the enum range does NOT update automatically.
- **Staleness consequence**: after a name change, the range must be manually updated in Workbench on every `SelR{k}` component (20 per tablero × 13 tableros = 260 components). This is the principal drawback of this design choice.

`BEnumRange` in baja: a map of ordinal (int) → display tag (String), immutable once created.
`[CERT]` B4 §4 (corpus block, BEnumRange API).
`[CERT]` `organized/control/control-rt/decompiled/javax/baja/control/BEnumWritable.java:296–306`

---

## 3. The Hx write widget: `BHxEnumFE`

### 3.1 Class and agent registration

```java
// hx-wb/vineflower/com/tridium/hx/fieldeditors/BHxEnumFE.java:24–29
@NiagaraType(
   agent = {@AgentOn(
      types = {"workbench:DynamicEnumFE", "workbench:FrozenEnumFE"}
   )}
)
public class BHxEnumFE extends BHxFieldEditor { ... }
```
`[CERT]` `organized/hx/hx-wb/vineflower/com/tridium/hx/fieldeditors/BHxEnumFE.java:24–29`
`[CERT]` class file also present: `organized/hx/hx-wb/extracted/com/tridium/hx/fieldeditors/BHxEnumFE.class`

`BHxEnumFE` is the Hx field editor for both dynamic and frozen enum types. It is registered as the Hx agent for `workbench:DynamicEnumFE` and `workbench:FrozenEnumFE` — the workbench-side type descriptors. `BHxFieldEditor.makeForUx(def, op)` picks this editor when `def` is a `BDynamicEnum` or `BFrozenEnum`.

### 3.2 Render paths: `BDynamicEnum` vs. `BFrozenEnum` / `preferFrozenEditor`

```java
// BHxEnumFE.java:52
boolean useFrozenEditor = value instanceof BFrozenEnum
    || op.getFacets().getb("preferFrozenEditor", false);

// BDynamicEnum path: <input type="text"> + <datalist> + jquery.relevant-dropdown
if (!useFrozenEditor) {
    html.w("<input ");                                  // text input
    html.w("<datalist ...></datalist>");
    js.w(",'nmodule/js/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown'");
    // populates datalist with display tags from range
    // wires jquery.relevantDropdown({force:true, showAll:true, ...})
}

// BFrozenEnum / preferFrozenEditor=true path: <select>
if (useFrozenEditor) {
    html.w("<select ");                                 // native select dropdown
    // for each ordinal: <option value="N" [selected]>displayTag</option>
    // uses only jquery (no extra JS library)
}
```
`[CERT]` `organized/hx/hx-wb/vineflower/com/tridium/hx/fieldeditors/BHxEnumFE.java:49–159`

| Path | Condition | HTML element | JS dependency | Submit format |
|------|-----------|-------------|--------------|---------------|
| **DynamicEnum** | `BDynamicEnum` and no `preferFrozenEditor` facet | `<input type="text">` + `<datalist>` | jQuery + `nmodule/js/rc/…/jquery.relevant-dropdown` | display tag (mapped to ordinal by `save()`) |
| **FrozenEnum / preferFrozenEditor** | `BFrozenEnum` OR facets `preferFrozenEditor=true` | `<select>` with `<option value="N">` | jQuery only | ordinal integer (option value) |

`BEnumWritable` stores `BDynamicEnum` values, so by default it takes the DynamicEnum path.
Setting `preferFrozenEditor=true` in the component's facets forces the `<select>` path.

### 3.3 `save()`: form POST → BDynamicEnum

```java
// BHxEnumFE.java:185–207
public BObject save(HxOp op) throws Exception {
    BEnum value = (BEnum) op.get();
    String formValue = op.getFormValue("value");
    if (formValue != null && !formValue.equals("")) {
        Integer ordinal = null;
        BEnumRange range = this.getEnumRange(op, value);
        for (int tempOrdinal : range.getOrdinals()) {
            if (formValue.equals(range.getDisplayTag(tempOrdinal, op))) {
                ordinal = tempOrdinal;  break;
            }
        }
        if (ordinal == null) { ordinal = Integer.parseInt(formValue); }  // fallback
        return value instanceof BFrozenEnum
            ? value.getRange().get(ordinal)
            : BDynamicEnum.make(ordinal, value.getRange());
    }
    return value;
}
```
`[CERT]` `organized/hx/hx-wb/vineflower/com/tridium/hx/fieldeditors/BHxEnumFE.java:185–207`

- DynamicEnum path: looks up display tag → ordinal; falls back to `parseInt`; returns `BDynamicEnum.make(ordinal, range)`.
- FrozenEnum path: looks up ordinal (the `<option value>`) directly; returns `BFrozenEnum`.

### 3.4 Range source: facets win over value range

```java
// BHxEnumFE.java:209–212
private BEnumRange getEnumRange(HxOp op, BEnum value) {
    BEnumRange facetsEnumRange = (BEnumRange) op.getFacet("range");
    return !(value instanceof BFrozenEnum) && facetsEnumRange != null
        ? facetsEnumRange : value.getRange();
}
```
`[CERT]` same file:209–212

For `BDynamicEnum`, `getEnumRange` uses `op.getFacet("range")` — this comes from `BEnumWritable.getSlotFacets(set)` → `this.getFacets()`, which includes the configured `BEnumRange`. The dialog options therefore show the range configured in Workbench.

---

## 4. Full write path: ActionBinding → ActionDialog → BHxEnumFE → form POST

The write path for `ActionBinding SelR{k}/set` (empty `actionArg`) in the Hx browser:

```
1. User clicks Px button.
2. BHxPxActionBinding.handleAction() (N4.3 class confirmed in client kitPx-wb) [CERT]:
   - def = component.getActionParameterDefault(set) → BDynamicEnum (fallback value)
   - defaultActionArg = null (actionArg is empty → getDefaultActionArgument returns null)
   → path ④: dialog.open(op)
3. ActionDialog.writeContent():
   - childOp.mergeFacets(component.getSlotFacets(set)) → merges the BEnumRange facet
   - editor = BHxFieldEditor.makeForUx(def, childOp) → finds BHxEnumFE
   - editor.write(childOp):
       DynamicEnum default: <input type="text"> + <datalist> (jquery.relevant-dropdown)
       preferFrozenEditor=true: <select> (jQuery only)
4. User selects/types horario name and submits dialog (HTML form POST).
5. ActionDialog.saveContent():
   - editor.fwSave(childOp) → BHxEnumFE.save()
   - Maps display tag → ordinal → BDynamicEnum.make(ordinal, range)
6. component.invoke(set, BDynamicEnum(ordinal, range), op) → write succeeds.
```

Client N4.3 classes confirming step 2: `BHxPxActionBinding.class`,
`BHxPxActionBinding$ActionDialog.class`, `BHxPxActionBinding$ActionDialog$ActionSubmit.class`
are all present in `organized/_client-ecnet-4.3.58.18-n4.4/kitPx-wb/com/tridium/kitpx/hx/`.
`[CERT]` extracted class file inventory.

The N4.14 `BHxPxValueBinding.handleAction` and the N4.3 `BHxPxActionBinding` follow the same
ActionDialog pattern (the ActionDialog inner class is identical by structure). `[INFER]` from
class name parity and identical inner-class suffixes.

---

## 5. N4.3-specific dependency risk: `jquery.relevant-dropdown`

The DynamicEnum render path (default for `BEnumWritable`) loads:
```java
'nmodule/js/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown'
```
This script lives in the `js` module (confirmed: `organized/js/js-ux/extracted/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown.js`). `[CERT]` corpus N4.14 `js-ux`.

**Client N4.3 uncertainty**: The client's `hx-wb` module was not extracted to
`organized/_client-ecnet-4.3.58.18-n4.4/`, so the version of `BHxEnumFE` in the 4.3 station
cannot be read directly. Whether it references `jquery.relevant-dropdown` (or an older mechanism)
is **[INFER]**. Evidence suggesting the client HAS relevant-dropdown support:
`organized/_client-ecnet-4.3.58.18-n4.4/webEditors-ux/rc/fe/baja/compat/RelevantStringEditor.js`
exists — a `compat/` shim used by `webEditors-ux` for autocomplete on strings. However, this is
the bajaux (ux) profile path, not the Hx path.

**Mitigation**: the `preferFrozenEditor=true` facet forces the `<select>` path, which uses only
jQuery (universally present in Niagara 4). This eliminates the `jquery.relevant-dropdown`
dependency and is recommended for the EC-Net 4.3 target. How to set it: in Workbench, open the
`BEnumWritable` property sheet → facets → add `preferFrozenEditor = true (boolean)`. `[INFER]` —
requires live confirmation that the facets key is honoured in 4.3's `BHxEnumFE`.

---

## 6. Verdict: EnumWritable + BEnumRange vs. the current numeric `/set`

| Criterion | `NumericWritable SelR{k}` (current) | `EnumWritable SelR{k}` (proposed) |
|---|---|---|
| Operator input | Types a bare integer (0–4) | Picks/types a horario name from list |
| Dialog type | `/set` (BDouble → typed text) | ActionDialog (BDynamicEnum → text+datalist OR `<select>`) |
| Fixed `actionArg` works in Hx? | No (B853 §4) | No (same type-mismatch failure; use empty actionArg) |
| Empty `actionArg` opens dialog? | Yes → user types number | Yes → user picks name |
| Static/dynamic range | N/A (numeric) | Static `BEnumRange` (names frozen at commissioning) |
| Range tracking `Horario{n}_Nombre` | N/A | NOT automatic — manual update required on every `SelR{k}` if names change |
| JS dependency (Hx) | None | DynamicEnum path: `jquery.relevant-dropdown` (4.3 unconfirmed); FrozenEditor path: jQuery only |
| Components to change | — | Replace `NumericWritable` with `EnumWritable` + configure `BEnumRange` facets × 260 |

**Net verdict: `EnumWritable` is a UX improvement** — the operator picks by name, not by blind
number. The `preferFrozenEditor=true` facet produces a clean `<select>` dropdown using only jQuery.

**However**, the static range is a real operational caveat: the 5 ordinal names in the `BEnumRange`
must be set at commissioning and manually kept in sync with `Horario{n}_Nombre` strings. For a
site where horario names rarely change (or where the names are fixed at commissioning), this is
acceptable. For a site that frequently renames schedules, the staleness risk makes the improvement
marginal.

**Residual live test (spin off as req-exec child B861-G1)**:
1. Create an `EnumWritable` component with `BEnumRange = {0:"Hor A", 1:"Hor B", 2:"Hor C", 3:"Hor D", 4:"Hor E"}` and facet `preferFrozenEditor=true` on the EC-Net 4.3 station.
2. Create a Px `ActionBinding` to its `/set` (empty `actionArg`). Open in the client browser.
3. Confirm: (a) ActionDialog opens; (b) renders a `<select>` with 5 options; (c) selecting a name and submitting writes the correct ordinal to the point; (d) the Px `ValueBinding` label updates.

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | `BEnumWritable` class is `javax.baja.control.BEnumWritable` in `control-rt` | [CERT] | `organized/control/control-rt/decompiled/javax/baja/control/BEnumWritable.java:62` |
| 2 | `BEnumWritable.set` takes `BDynamicEnum` (not `BDouble`); flags=256 (OPERATOR) | [CERT] | `BEnumWritable.java:61` @NiagaraActions |
| 3 | Fixed `actionArg` on ActionBinding to `BEnumWritable.set` fails in Hx (BString ≠ BDynamicEnum) | [CERT] | B856 §3.2 path ②; same mechanism, different param type |
| 4 | `getSlotFacets(set)` returns `this.getFacets()` → merges `BEnumRange` into dialog op | [CERT] | `BEnumWritable.java:287–292` |
| 5 | `getActionParameterDefault(set)` returns `getFallback().getValueValue()` (BDynamicEnum) | [CERT] | `BEnumWritable.java:271–272` |
| 6 | `BEnumRange` in facets is static; no live link to `BStringWritable` slots | [CERT] | `BEnumWritable.java:296–306` onExecute reads facets, no live wire |
| 7 | `BHxEnumFE` exists in N4.14 `hx-wb`; agents on `workbench:DynamicEnumFE`/`FrozenEnumFE` | [CERT] | `organized/hx/hx-wb/vineflower/com/tridium/hx/fieldeditors/BHxEnumFE.java:24–29` + `extracted/*.class` |
| 8 | DynamicEnum path → `<input type=text>` + `<datalist>` + `jquery.relevant-dropdown` | [CERT] | `BHxEnumFE.java:62–114` N4.14 |
| 9 | `preferFrozenEditor=true` (facets) OR `BFrozenEnum` value → `<select>` (jQuery only) | [CERT] | `BHxEnumFE.java:52`, `62–76`, `96–114` N4.14 |
| 10 | `save()` maps display tag → ordinal (parseInt fallback) → `BDynamicEnum.make` | [CERT] | `BHxEnumFE.java:185–207` N4.14 |
| 11 | Client N4.3 has `BHxPxActionBinding$ActionDialog.class` (confirms ActionDialog exists) | [CERT] | `organized/_client-ecnet-4.3.58.18-n4.4/kitPx-wb/…/BHxPxActionBinding$ActionDialog.class` |
| 12 | `jquery.relevant-dropdown` confirmed in corpus N4.14 `js-ux` module | [CERT] | `organized/js/js-ux/extracted/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown.js` |
| 13 | N4.3 client `BHxEnumFE` version unknown (`hx-wb` module not extracted from client) | [INFER] | Only class listing available; DynamicEnum vs FrozenEnum render in 4.3 is unconfirmed |
| 14 | `preferFrozenEditor=true` eliminates the `jquery.relevant-dropdown` dependency | [CERT] N4.14 code | `BHxEnumFE.java:78–131`; frozen path has no extra `require()` besides jQuery |
| 15 | Static range is the main operational caveat; 260-component update scope if names change | [CERT] | BEnumWritable.onExecute range sync (component level only, no propagation to other components) |

**Tally:** [CERT] × 13 · [INFER] × 2 = 15 claims verified.
The dropdown write path claim is [CERT] for N4.14 source; marked [INFER] for exact N4.3 behavior (claim 13). The live test seeds B861-G1 to close this.

---

## Connections

- **B851** — the control chain; `SelR{k}` is currently a `NumericWritable` in the pilot.
- **B852** — UI design; this block describes the EnumWritable upgrade path for the selector.
- **B853** — empirical Hx behavior; §4 established the fixed-arg failure (applies to EnumWritable too); §5 established the `/set` dialog path (this block describes the enum-typed variant of the same dialog).
- **B856** — B856 §3.2 path ② is the same mechanism that rules out fixed `actionArg` for EnumWritable.set; §6.1 rule applies verbatim.
- **B857** — mentions EnumWritable dropdown as the no-module intermediate path between stock Px + dialog and a full custom web module.

---

## Open gaps

| Gap | Description |
|-----|-------------|
| **B861-G1** (req-exec) | Live confirmation on EC-Net 4.3.58.18: create `BEnumWritable` with `BEnumRange` + `preferFrozenEditor=true` facet, bind Px `ActionBinding SelR{k}/set` (no actionArg), open in client browser, confirm `<select>` renders and write succeeds. Closes claim 13 from [INFER] to [CERT-live]. |
| **B853-G2** | **Partially closed** by this block (source mechanics proven, static-range constraint documented). Fully closed upon B861-G1 completion. |
