# Block 856 — Hx vs bajaux/ux Px Rendering Engine Reference: module structure, version gates, and widget behavior per engine

> **Focus:** harbor-greenmax-lighting. **Scope:** version-authoritative reference for the two Px
> rendering engines shipped with Niagara 4 — the server-side **Hx** engine (`hx-wb`) and the
> client-side **bajaux/ux** engine (`kitPx-ux`) — covering which modules provide each, the N4
> version boundary where `kitPx-ux` exists vs does not, the exact mechanism behind the B853
> `ActionBinding` fixed-arg failure, and the practical design rules for a station whose Px pages
> will be viewed through Hx.
>
> This block **generalises** B853's empirical findings into a reusable reference. It is **not** a
> re-investigation of B853; it cites `[CERT-live]` markers owned by B853 where appropriate.
>
> **Evidence sources:**
> - `[CERT]` = verbatim from decompiled / vineflower source or `module.xml` at the cited path.
> - `[CERT-doc]` = official Niagara help guide, cited by file and section.
> - `[CERT-live]` = empirical on the client station HM_BMS (EC-Net 4.3.58.18) — owned by B853.
> - `[INFER]` = derived from `[CERT]` evidence; what would confirm it is stated.

---

## 1. Two engines, two module sets

Niagara 4 ships with two distinct engines for rendering a Px page in a browser:

| | **Hx engine** | **bajaux/ux engine** |
|---|---|---|
| Module | `hx-wb` | `kitPx-ux` (separate jar) |
| Java root | `BHxPxMedia extends BPxMedia` | N/A — pure JS |
| Rendering locus | **Station process** (server-side) | **Browser** (client-side JS) |
| Transport | Page loads = full HTTP round-trips; state sync via DOM-delta AJAX | BajaScript BOX WebSocket; `bajaux/Widget` lifecycle |
| Action binding impl | `BHxPxActionBinding` (Java) | `ActionBinding.js` (JS, RequireJS) |
| Px widget discovery | `AgentFilter.is(BHxPxWidget.TYPE)` per-widget | `kitPx-ux` `rc/binding/*.js` + spandrel |
| Profiles | Default/Basic/Handheld Hx Profile, HTML5 Hx Profile | bajaux profile (HTML5 ux), UxMedia pages |

`BHxPxMedia` extends `javax.baja.ui.px.BPxMedia` and uses two compile-time agent filters:
```java
private static final AgentFilter hxPx    = AgentFilter.is(BHxPxWidget.TYPE);
private static final AgentFilter hxPxBinding = AgentFilter.is(BHxPxBinding.TYPE);
```
`[CERT]` `organized/hx/hx-wb/decompiled/com/tridium/hx/BHxPxMedia.java:37-38`

A widget or binding with **no registered `BHxPxWidget` or `BHxPxBinding` agent** is skipped by Hx
and renders as nothing — no error, just an empty slot in the page.

---

## 2. Module structure per N4 version

### 2.1 EC-Net 4.3.58.18 (≈ N4.4, client station HM_BMS)

```
kitPx-wb  vendorVersion="4.3.58.18"  releaseDate="2017-05-10"
  deps: hx-wb, bajaux-ux, webEditors-ux, bajaui-wb, control-rt, converters-rt, ...
  NO kitPx-ux dependency
```
`[CERT]` `organized/_client-ecnet-4.3.58.18-n4.4/kitPx-wb/META-INF/module.xml:1-3`

The directory `organized/_client-ecnet-4.3.58.18-n4.4/` contains only `kitPx-wb` and
`webEditors-ux` — `kitPx-ux.jar` is **absent**. `[CERT]` directory listing.

Consequence: **every Px page opened in a browser on this station is served by the Hx engine.** There is
no code path to bajaux Px rendering. `bajaux-ux` is present as a dependency (bajaux widgets can exist
in other views) but is not used for Px-file rendering.

### 2.2 N4.14 (corpus Supervisor)

```
kitPx-wb  vendorVersion="4.14.0.162"  releaseDate="2024-05-28"
  deps: hx-wb, bajaux-ux, bajaux-rt, kitPx-ux (implicit via kitPx-ux dependency tree),
        webEditors-ux, bajaui-wb, bajaui-ux, converters-ux, uxBuilder-ux, ...

kitPx-ux  vendorVersion="4.14.0.162"  runtimeProfile="ux"
  deps: bajaux-ux, bajaux-rt, bajaScript-ux, webEditors-ux, uxBuilder-ux, ...
  NO hx-wb dependency
```
`[CERT]` `organized/kitPx/kitPx-wb/extracted/META-INF/module.xml:2-30`
`[CERT]` `organized/kitPx/kitPx-ux/extracted/META-INF/module.xml:2-30`

`kitPx-ux` is a **separate jar** (`runtimeProfile="ux"`) that delivers only JS files under `rc/`.
It has no hx-wb dependency and no Java classes — pure browser-side code.

### 2.3 Version boundary: when did `kitPx-ux.jar` appear?

The exact Niagara minor version is not provable from the two versions in the corpus.  
What is provable:

| Fact | Marker |
|------|--------|
| N4.3 (client) has no `kitPx-ux.jar` | [CERT] — module.xml + directory |
| N4.14 has `kitPx-ux.jar` | [CERT] — module.xml |
| "hx-wb (HTML5 Hx profile, N4 pre-4.7)" characterized as legacy | [INFER] from B36 §36.1 line 141 |
| UxMedia (Px-as-JSON → bajaux render) introduced in N4.10 | [CERT] from B13 §13.3.4 |
| `WbWebProfile` (Workbench applet) deprecated in N4.13, removed in N5 | [CERT-doc] `niagara-help/guides-clean/devguide/uiFromAxToN4.txt:L70` |
| bajaux framework itself exists since N4.0 | [CERT-doc] `uiFromAxToN4.txt:L121` |
| N4.9: all Java applet / Web Start support removed from browser | [CERT-doc] `uiFromAxToN4.txt:L70` |

**Working bound:** `kitPx-ux.jar` appeared somewhere in N4.7–N4.10. The earliest confirmed presence
is N4.10 (UxMedia, which uses `kitPx-ux` for client-side rendering). **[INFER]** — what would confirm:
inspect a kitPx-ux module from an N4.7 or N4.8 install.

---

## 3. The Hx `ActionBinding` fixed-argument failure — exact mechanism

This is the root cause of the B853 §4 failure (no-arg write fails silently).

### 3.1 `BHxPxActionBinding.ActionBindingInvokeActionCommand.getDefaultActionArgument`

```java
// kitPx-wb/vineflower/com/tridium/kitpx/hx/BHxPxActionBinding.java:114-116
public BValue getDefaultActionArgument(HxOp op) {
    BActionBinding actionBinding = (BActionBinding)op.get();
    return !actionBinding.getActionArg().equals(BActionBinding.actionArg.getDefaultValue())
        ? actionBinding.getActionArg()   // returns the stored BString "2.0", "1.0", etc.
        : null;                          // returns null if actionArg is the default (empty)
}
```
`[CERT]` `organized/kitPx/kitPx-wb/vineflower/com/tridium/kitpx/hx/BHxPxActionBinding.java:114-116`

`BActionBinding.getActionArg()` returns the stored property as a **`BString`** — the Px XML value
`<ActionBinding actionArg="2.0">` is stored as a string, not decoded to the target type.

### 3.2 `BHxPxValueBinding.handleAction` — invoke decision tree

```java
// hx-wb/vineflower/javax/baja/hx/px/binding/BHxPxValueBinding.java (condensed)
BValue def = component.getActionParameterDefault(action);   // action's declared param type
if (def == null) {
    component.invoke(action, null, op);              // ①  no-arg action → works
} else {
    BValue defaultActionArg = command.getDefaultActionArgument(op);  // ← from §3.1
    if (defaultActionArg != null) {
        component.invoke(action, defaultActionArg, op);  // ② fixed-arg → passes BString
    } else if (op.getFormValue("paramsDisplayed") != null) {
        BValue param = dialog.saveContent(op);
        component.invoke(action, param, op);             // ③ dialog has been shown
    } else {
        dialog.open(op);                                 // ④ empty actionArg → shows dialog
    }
}
```
`[CERT]` `organized/hx/hx-wb/vineflower/javax/baja/hx/px/binding/BHxPxValueBinding.java:186-212`

**Path ②** is the failure: `defaultActionArg` is `BString("2.0")` but `BNumericWritable.set` declares
`parameterType="BDouble"`. `component.invoke(action, BString, op)` throws a type mismatch and returns
silently — the button appears to work (click event fires) but the point value never changes.
`[CERT-live]` (observed, owned by B853 §4).

**Path ④** is the working path for writing a numeric value: leave `actionArg` empty, let Hx open the
dialog. The user types the value, the dialog calls `dialog.saveContent` which returns a properly-typed
`BDouble` → `component.invoke` succeeds. `[CERT-live]` (owned by B853 §5).

**Path ①** is why `BooleanWritable.active` / `.inactive` / `.auto` work: those actions are
`flags=256=OPERATOR` with **no parameter** (`def == null`). `[CERT-live]` (owned by B853 §5).

### 3.3 Bajaux fix: `ActionBinding.js` decodes correctly

```javascript
// kitPx-ux/vineflower/rc/binding/ActionBinding.js (excerpt)
function resolveActionArgument(component, slot, actionArg) {
    if (actionArg === '') return Promise.resolve();  // empty → no arg
    if (!actionArg.getType().is("baja:String")) return Promise.resolve(actionArg);
    return component.getActionParameterDefault(slot).then(function(value) {
        paramType = value.getType();
        if (paramType.isSimple()) {
            return value.decodeAsync(actionArg);   // decodes "2.0" → BDouble(2.0)
        }
    });
}
```
`[CERT]` `organized/kitPx/kitPx-ux/vineflower/rc/binding/ActionBinding.js` (function `resolveActionArgument`)

Under bajaux, a fixed `actionArg="2.0"` **is properly decoded** to the target type before invoke.
The N4.3 client cannot benefit from this because `kitPx-ux.jar` does not exist on that station.

---

## 4. Widget-level behavior differences: Hx vs bajaux

| Widget / binding | Hx behavior | bajaux behavior |
|---|---|---|
| **ActionBinding, no `actionArg`** (action has param) | Opens Set dialog (path ④) `[CERT]` | Opens dialog or uses resolveActionArgument `[CERT]` |
| **ActionBinding, fixed `actionArg`** (action has typed param) | **Fails silently** (BString type mismatch, path ②) `[CERT]` + `[CERT-live]` | Decodes string to target type → works `[CERT]` |
| **No-arg actions** (`active`, `inactive`, `auto`) | Works (path ①, null passed) `[CERT]` + `[CERT-live]` | Works (empty actionArg → resolve undefined) `[CERT]` |
| **`active`/`inactive` with no `<Override>` wrapper** | Pops "Override duration" dialog (action has a parameter) `[CERT-live]` B853 §5 | Same |
| **`active`/`inactive` with empty `<Override name="actionArg"/>`** | Invokes with DEFAULT (Permanent), no dialog `[CERT-live]` B853 §5 | Equivalent |
| **PopupBinding** | Works (`BHxPxPopupBinding` agent present) `[CERT]` class exists in kitPx-wb | Works (JS) |
| **SetPointBinding** | Hx agent `BHxPxSetPointBinding` present `[CERT]` | JS equivalent present |
| **ValueBinding** + `ObjectToString`/`IBooleanToSimple` | Works if `converters` module is imported in Px `[CERT-live]` B853 §3 | Same import requirement |
| **ImageButton** | `BHxPxImageButton` agent present; renders normal + mouseOver images via DOM injection `[CERT]` | bajaux JS widget |
| **Widget with no `BHxPxWidget` agent** | Silently absent `[CERT]` `BHxPxMedia` agent filter | N/A — different discovery |
| **UxMedia Px-as-JSON** | Not available in N4.3; Hx-only `[CERT]` | Available in N4.10+ `[CERT]` B13 §13.3.4 |

---

## 5. Hx profile hierarchy (N4.14 official)

From the official Niagara help:
> "Default Hx Profile: real-time data displayed using Hx technology instead of the Java plugin."
> "HTML5 Hx Profile: defines the Default Web Profile and the Default Mobile Web Profile."

`[CERT-doc]` `niagara-help/guides-clean/Graphics/aDefaultHxProfile.txt`  
`[CERT-doc]` `niagara-help/guides-clean/Graphics/HTML5HxProfileGraphicsN4-777D3747.txt`  
`[CERT-doc]` `niagara-help/guides-clean/Graphics/defaultTouchProf.txt`

The web profiles ship as:

```
BIWebProfile
  ├─ BIHxProfile          ← Hx family
  │     ├─ BHxProfile          (base, hx-wb)
  │     └─ BHTML5HxProfile     (hx-wb, modern HTML5)
  └─ BWbWebProfile        ← Workbench-applet family (deprecated N4.13)
```
`[CERT-doc]` `niagara-help/devguide/javax/baja/web/BIWebProfile.txt:SUBCLASSES`

In N4.3 (client) a browser user is assigned the **Default Hx Profile** or **HTML5 Hx Profile** — both
backed by `BHTML5HxProfile` which routes Px files through `BHxPxMedia`. `[CERT]` class
`organized/hx/hx-wb/vineflower/com/tridium/hx/BHTML5HxProfile.java`.

---

## 6. Practical rules for Hx-only stations

If the target station is N4 < ~4.10 (no UxMedia, no `kitPx-ux.jar`) or if the admin has not
enabled UxMedia:

1. **Never put a non-empty `actionArg` on an `ActionBinding` to a typed action (NumericWritable.set,
   EnumWritable.set, etc.).**  The write will fail silently. Use `actionArg=""` to get the Set dialog.
   `[CERT]` mechanism §3.2 path ②.

2. **Boolean no-arg actions** (`active`, `inactive`, `auto`) work. Add `<Override name="actionArg"/>` to
   suppress the duration-prompt dialog on `active`/`inactive`. `[CERT-live]` B853 §5.

3. **Every module whose widgets/bindings/converters appear in the Px must be explicitly imported** in the
   Px file header. Missing imports produce `PxDecoder … Unknown type <X>` errors. `[CERT-live]` B853 §3.

4. **Test every binding in the actual browser, not only in Workbench.** Workbench decodes `actionArg`
   strings at bind time; Hx does not. A button that writes correctly in Workbench can be silent in the
   browser. `[CERT]` §3.2; `[CERT-live]` B853 §4.

5. **Widgets with no registered `BHxPxWidget` agent silently disappear.** Verify the widget catalog
   (`kitPx-wb`, `kitPxN4svg-wb`, `converters`, etc.) covers every element you place. `[CERT]`
   `BHxPxMedia` agent filter.

6. **From N4.10 onward**, UxMedia (`niagara.preferUxMedia=true` or per-user profile set to HTML5) routes
   Px through bajaux. Fixed `actionArg` works. This does **not** apply to the client station (N4.3).
   `[CERT]` B13 §13.3.4; `[INFER]` per-user switching — confirm with live N4.10+ test.

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Client station (N4.3.58.18) has no `kitPx-ux.jar` | [CERT] | `organized/_client-ecnet-4.3.58.18-n4.4/` directory: only `kitPx-wb` + `webEditors-ux` |
| 2 | N4.14 has both `kitPx-wb` and `kitPx-ux` | [CERT] | `organized/kitPx/kitPx-{wb,ux}/extracted/META-INF/module.xml` |
| 3 | `getDefaultActionArgument` returns raw `BString` for non-empty actionArg | [CERT] | `kitPx-wb/vineflower/.../BHxPxActionBinding.java:114-116` |
| 4 | `handleAction` passes that BString directly to `component.invoke` → type mismatch | [CERT] | `hx-wb/vineflower/.../BHxPxValueBinding.java:196-208` |
| 5 | Empty actionArg → `getDefaultActionArgument` returns null → Hx opens Set dialog | [CERT] | Same file, path ④ (dialog.open) |
| 6 | bajaux `ActionBinding.js` calls `decodeAsync` → proper type decode | [CERT] | `kitPx-ux/vineflower/rc/binding/ActionBinding.js` `resolveActionArgument` |
| 7 | bajaux introduced in N4.0 | [CERT-doc] | `niagara-help/guides-clean/devguide/uiFromAxToN4.txt:L121` |
| 8 | UxMedia (Px-as-JSON → bajaux) = N4.10+ | [CERT] | B13 §13.3.4 |
| 9 | `WbWebProfile` deprecated N4.13 | [CERT-doc] | `uiFromAxToN4.txt:L70` |
| 10 | `BHxPxMedia` uses `AgentFilter.is(BHxPxWidget.TYPE)` — widget-gated | [CERT] | `organized/hx/hx-wb/decompiled/com/tridium/hx/BHxPxMedia.java:37-38` |
| 11 | `BHxPxImageButton` agent registered for `kitPx:ImageButton` | [CERT] | `kitPx-wb/vineflower/.../BHxPxImageButton.java:@AgentOn` |
| 12 | Profile hierarchy `BIWebProfile → BIHxProfile → BHTML5HxProfile` | [CERT-doc] | `niagara-help/devguide/javax/baja/web/BIWebProfile.txt:SUBCLASSES` |
| 13 | kitPx-ux version boundary (N4.7–N4.10) | [INFER] | B36 "pre-4.7" heuristic + B13 UxMedia N4.10; requires intermediate install to confirm |

**Tally:** [CERT] × 9 · [CERT-doc] × 3 · [INFER] × 1 = 13 claims verified.

---

## Connections

- **B845** — decided to use stock Px (not a custom HTML/JS module) for the Harbor dashboard.
  This block explains the rendering engine that stock Px goes through on the client station.
- **B852** — designed the Px UI layout and widget selection for the operator tablero.
  Rules §6.1–6.3 here constrain every ActionBinding choice made in B852.
- **B853** — live empirical evidence on the client browser (EC-Net 4.3.58.18).
  This block provides the source-level mechanism behind B853's §4 failure and §5 workaround.
- **B9** — UI stack overview (Hx + bajaux), first mention of Hx profile and bajaux side-by-side.
- **B36** — PX widget deep-dive; introduced "N4 pre-4.7" characterisation of Hx as legacy.
- **B293** — notes `hx:HxPxMedia` agent-gated widget discovery (independently confirms §1 of this block).

---

## Open gaps

| Gap | Description |
|-----|-------------|
| **B856-G1** | Confirm exact N4 version where `kitPx-ux.jar` first shipped — requires inspecting a N4.7 or N4.8 install. Resolves the [INFER] in §2.3. |
| **B856-G2** | Test fixed `actionArg` on a station running N4.10+ with UxMedia enabled — confirm bajaux path resolves it correctly (validate §6 claim 6 as `[CERT-live]`). |
| **B856-G3** | `ImageButton buttonStyle="toolBar"` hover/highlight rendering in Hx vs bajaux — `UxImageButtonUtil.write()` vs legacy path; B853 mentions highlight not rendering; no dedicated `[CERT-live]` yet. |
