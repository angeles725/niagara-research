# Block 858 — Hx Image Rendering: `file:^` Asset Lifecycle, `buttonStyle="toolBar"` Highlight Mechanism, and the Selected-Mode Highlight Recipe

> **Focus:** harbor-greenmax-lighting. **Scope:** closes the cluster of Hx rendering gaps seeded by
> B853 and B856 that share the same phenomenon — a selected-mode highlight image that did not render in
> the client Hx browser. Four angles: WHY the image did not render (B853-G6), how `file:^` assets
> propagate to a running browser (B853-G8), the exact `buttonStyle="toolBar"` highlight mechanism in
> Hx (B856-G3), and the complete verified control+display recipe for selected-mode highlight (B853-G10).
>
> **Evidence:**
> - `[CERT]` = verbatim from decompiled/vineflower source at the cited path (N4.14 corpus).
> - `[CERT-live]` = empirical, owned by B853 (EC-Net 4.3.58.18 against HM_BMS).
> - `[INFER]` = derived from `[CERT]` evidence; what would confirm is stated.

---

## 1. B853-G6 — WHY the highlight image did not render in the Hx browser

### 1.1 Hypothesis (a): Hx caches `file:^` assets and a new file is not picked up — REFUTED

The station's `FileServlet` (the servlet that handles `file:^` and `file:` ords in the browser) sends
the following headers for **station-space files** (any file whose `BFileSpace` is not a `BModule` or
`BZipSpace`):

```
Cache-Control: private, must-revalidate, max-age=0
Last-Modified: <actual file modification timestamp>
```

The `max-age=0` means the browser MUST revalidate on every request. The `Last-Modified` header combined
with `If-Modified-Since` means: if the file timestamp has changed (e.g., a newly uploaded
`BotonVerde.png`), the server returns `200` with fresh content. If unchanged, it returns `304 Not
Modified`. A newly uploaded station file IS served to the browser without any station restart, nav
reload, or cache-busting action. No station restart is needed.

`[CERT]` `organized/web/web-rt/vineflower/com/tridium/web/servlets/FileServlet.java:57-66` (constants),
`:208-229` (`applyLastModified` + `getLastModifiedTimestamp`), `:240-248` (`getCacheControlHeader`
returning `max-age=0` for non-module files), `:270-272` (`isLongTermCacheable` — `BModule` and
`BZipSpace` only get `max-age=2592000`).

**Hypothesis (a) is REFUTED for the "asset not picked up" claim.** The caching mechanism
correctly serves fresh station files.

### 1.2 Hypothesis (b): `buttonStyle="toolBar"` paints over the Picture — BEST-SUPPORTED [INFER]

The Hx renderer for `kitPx:ImageButton` is `BHxPxImageButton`. Its `write()` method:

```java
// BHxPxImageButton.java:38-44
public void write(HxOp op) throws Exception {
    if (!UxLabelUtil.hasLegacyLabel(op)) {
        UxImageButtonUtil.write(op);   // ← modern path (no legacy label)
    } else {
        super.write(op);               // ← legacy path
    }
}
```

`UxImageButtonUtil.write()` emits this HTML structure:

```html
<span id="…button-outline" class="ux-Button-outline">
  <button id="…button" type="button">
    <div id="…imageButton-backgroundImage" class="-t-ImageButton-backgroundImage"></div>
    <span id="…labelRoot">…</span>
  </button>
</span>
```

`[CERT]` `organized/kitPx/kitPx-wb/vineflower/com/tridium/kitpx/hx/ux/UxImageButtonUtil.java:20-33`

The `<button>` element is a block-level HTML element. Browsers' UA (User-Agent) stylesheets render
`<button>` elements with a **system-default non-transparent background** (typically gray). The theme
CSS for `ux-Button-buttonStyle-toolBar` does NOT set `background: transparent` on the `<button>`
element:

```css
/* theme.css — all selectors for toolBar */
.ux-Button > .ux-Button-outline.ux-Button-buttonStyle-toolBar > button > .ux-Label > .ux-Label-content {
  margin: 3px;
}
.ux-Button:not(.bajaux-disabled) > .ux-Button-outline.ux-Button-buttonStyle-toolBar > button:hover {
  box-shadow: 0 0 0 1px #9c9e95;
  border: 1px solid #f7f7f7;          /* hover-only effect, not default state */
}
.ux-Button > .ux-Button-outline.ux-Button-buttonStyle-toolBar.ux-Button-buttonStyle-selected > button {
  background-color: #c1c1c1;          /* only when .selected class is also present */
}
```

`[CERT]` `organized/web/web-rt/extracted/rc/theme/theme.css:380-407`

**Consequence:** any `Picture` widget (rendered as an `<img>` or background-image element) placed BEHIND
the ImageButton in DOM/z-order is covered by the `<button>` element's UA-default opaque background.
The highlight image (`BotonVerde.png`) placed in a Picture widget behind the ImageButton simply does
not show through.

**This is `[INFER]`** from the CSS + HTML structure. What would confirm it: in a live Hx page, add
`style="background: transparent"` to the `<button>` element via browser DevTools and verify the Picture
becomes visible — spin off as **B858-G1** (requires execution).

### 1.3 Hypothesis (c): z-order/scale — subsumed

This is the same mechanism as (b) stated from the z-order angle. The `<button>` element's opaque UA
background is the z-order blocker. Scale does not play a role if the Picture and ImageButton have
matching geometry. Subsumed into (b).

### 1.4 Summary for G6

| Hypothesis | Verdict | Marker |
|---|---|---|
| (a) `file:^` caching blocks new asset | REFUTED — `max-age=0` + `Last-Modified` serves new files fresh | [CERT] FileServlet |
| (b) `buttonStyle="toolBar"` button opaque background covers Picture | BEST SUPPORTED — no `background: transparent` in CSS; `<button>` UA background is opaque | [INFER] |
| (c) z-order/scale | Subsumed by (b) | [INFER] |

**G6 PARTIAL — dominant hypothesis (b), requires live confirmation (B858-G1).**

---

## 2. B853-G8 — Hx asset lifecycle: how `file:^` files propagate to a running browser

### 2.1 Station-space files (`file:^px/…`)

Served by `FileServlet` with:

```
Cache-Control: private, must-revalidate, max-age=0
Last-Modified: <BIFile.getLastModified().getMillis(), truncated to seconds>
```

The `If-Modified-Since` / `Last-Modified` handshake is the ONLY cache mechanism. No ETag is used.

```java
// FileServlet.java:208-229
private static boolean applyLastModified(HttpServletRequest req, HttpServletResponse resp, BIFile file) {
    Long lastModified = getLastModifiedTimestamp(req, file);
    if (lastModified == null) {
        resp.setStatus(304);   // same or older timestamp → 304
        return false;
    } else {
        resp.setDateHeader("Last-Modified", lastModified);  // set header, return 200
        return true;
    }
}

private static Long getLastModifiedTimestamp(HttpServletRequest req, BIFile file) {
    long lastModified = file.getLastModified().getMillis();
    lastModified = lastModified / 1000L * 1000L;     // truncate to second
    if (lastModified > 0L) {
        long ifModified = req.getDateHeader("If-Modified-Since");
        if (ifModified > 0L && lastModified / 1000L <= ifModified / 1000L) {
            return null;   // cache hit → 304
        }
    }
    return lastModified;
}
```

`[CERT]` `organized/web/web-rt/vineflower/com/tridium/web/servlets/FileServlet.java:208-229`

**Lifecycle for a newly uploaded `file:^px/BotonVerde.png`:**
1. Upload occurs; `BIFile.getLastModified()` reflects the upload timestamp.
2. Next time the browser fetches the image URL: `If-Modified-Since` is absent (never fetched before) → 200 + fresh content.
3. On subsequent fetches: `If-Modified-Since` = previous `Last-Modified` timestamp → if unchanged → 304; if file modified → 200 + new content.
4. **No station restart, nav rebuild, or explicit cache-bust is needed.** The `max-age=0` directive forces the browser to check every time.

### 2.2 Module-based images (`module://kitPxN4svg/…`)

Module images live in `BModule` (a zip space):

```java
// FileServlet.java:270-272
private static boolean isLongTermCacheable(BIFile file) {
    BFileSpace space = file.getFileSpace();
    return space == null || space instanceof BZipSpace || space instanceof BModule;
    // ↑ module images land here → max-age=2592000 (30 days)
}
```

`[CERT]` `organized/web/web-rt/vineflower/com/tridium/web/servlets/FileServlet.java:270-272`

Module images are cached for 30 days. They don't change during a station run (modules are immutable), so this is safe. Station files are NOT long-term cacheable.

### 2.3 Why pill/LED images work but the `file:^` highlight image did not

The pill/LED images come from `module://kitPxN4svg/…` (long-term cached, always available from the
module at load time). The `file:^px/BotonVerde.png` highlight image requires:
1. The file to exist in station space (upload it first).
2. The next Hx update tick to call `px.updatePictureImage()` with the resolved URL.
3. The browser to fetch that URL → 200 on first fetch.

If the image file was NOT uploaded yet when the Px page was first opened, the `BHxPxPicture.update()`
call generates `BOrd.NULL` → no image src set. Even after uploading, the image won't render until
the next full Hx update cycle (typically triggered by a data change or page refresh). The module images
are always present at station startup.

`[CERT]` `organized/hx/hx-wb/decompiled/com/tridium/hx/px/BHxPxPicture.java:57-81` (update reads
`picture.getImage().getOrdList()` — if null, sets `display:none`).

**G8 CLOSED [CERT]** — `file:^` asset lifecycle is `max-age=0` + `Last-Modified`; no restart needed
for new files; update requires an Hx refresh cycle.

---

## 3. B856-G3 — `buttonStyle="toolBar"` highlight mechanism in Hx specifically

### 3.1 BButtonStyle enum

`buttonStyle` is a property of `BAbstractButton` (and therefore of `BImageButton` which extends
`BButton extends BAbstractButton`). The enum has four values:

```java
// BButtonStyle.java
@NiagaraEnum(range = {@Range("none"), @Range("normal"), @Range("toolBar"), @Range("hyperlink")})
public final class BButtonStyle extends BFrozenEnum {
    public static final BButtonStyle none     = new BButtonStyle(0);
    public static final BButtonStyle normal   = new BButtonStyle(1);
    public static final BButtonStyle toolBar  = new BButtonStyle(2);  // tag = "toolBar"
    public static final BButtonStyle hyperlink= new BButtonStyle(3);
}
```

`[CERT]` `organized/bajaui/bajaui-wb/vineflower/javax/baja/ui/enums/BButtonStyle.java:12-24`

### 3.2 Hx rendering path for `toolBar` style

`UxButtonUtil.update()` is called for ALL button types (via the shared path from `BHxPxImageButton →
UxImageButtonUtil → UxButtonUtil.update()`):

```java
// UxButtonUtil.java:33-43 (condensed)
BAbstractButton button = (BAbstractButton)op.get();
String buttonTag = button.getButtonStyle().getTag();       // → "toolBar"
outlineProperties.append("className", "ux-Button-buttonStyle-" + buttonTag);  // → "ux-Button-buttonStyle-toolBar"

if (button instanceof BToggleButton) {
    BToggleButton toggleButton = (BToggleButton)button;
    if (toggleButton.getSelected()) {
        properties.append("className", "ux-ToggleButton");
        outlineProperties.append("className", "ux-Button-buttonStyle-selected");  // ← ONLY for ToggleButton
    }
}
```

`[CERT]` `organized/hx/hx-wb/vineflower/com/tridium/hx/px/ux/UxButtonUtil.java:33-43`

### 3.3 What `ux-Button-buttonStyle-toolBar` actually does in CSS

From the canonical N4.14 theme:

| State | CSS rule | Visual effect |
|---|---|---|
| Default | No border or background set | Transparent-looking (but `<button>` UA background applies) |
| Hover | `:hover` → `box-shadow: 0 0 0 1px #9c9e95; border: 1px solid #f7f7f7` | A 1 px outlined border on mouse-over |
| Selected | `.ux-Button-buttonStyle-toolBar.ux-Button-buttonStyle-selected > button` → `background-color: #c1c1c1` | Gray fill — BUT requires `.ux-Button-buttonStyle-selected` class |

`[CERT]` `organized/web/web-rt/extracted/rc/theme/theme.css:380-407`

### 3.4 The critical constraint: `selected` class requires BToggleButton

The `ux-Button-buttonStyle-selected` class is appended in `UxButtonUtil.update()` ONLY when:
```java
button instanceof BToggleButton && toggleButton.getSelected()
```

`BImageButton` extends `BButton` — it is **NOT** a `BToggleButton`. Therefore an ImageButton with
`buttonStyle="toolBar"` **never** receives the `selected` CSS class, regardless of the bound value.
The gray persistent highlight does NOT appear. The only visual feedback from `toolBar` on an
ImageButton is the CSS `:hover` outline — purely mouse-driven, not value-driven.

`[CERT]` `UxButtonUtil.java:38-43` + `BImageButton.java` class declaration (extends BButton, not
BToggleButton) `organized/kitPx/kitPx-wb/decompiled/com/tridium/kitpx/BImageButton.java:33`

### 3.5 Workbench (AWT) comparison

In Workbench, `BAbstractButton.changed(buttonStyle)` adds the "toolbar" CSS class to the AWT widget
when `buttonStyle == BButtonStyle.toolBar`. Line 329 (BAbstractButton) additionally reads the
component selection command to paint a "selected" visual. This is AWT-paint based, not CSS. **The Hx
engine does NOT replicate this behavior** — it produces only the CSS `ux-Button-buttonStyle-toolBar`
class with hover-only effects.

`[CERT]` `organized/bajaui/bajaui-wb/vineflower/javax/baja/ui/BAbstractButton.java:222-230, :329`

**B856-G3 CLOSED [CERT]** — `buttonStyle="toolBar"` in Hx gives only a CSS hover outline; no
persistent selected-state for ImageButton. The Workbench selected-paint behavior does not translate to
the Hx renderer.

---

## 4. B853-G10 — The complete verified selected-mode-highlight recipe

### 4.1 Logic side (confirmed from B851/B853, `[CERT]`)

The control graph for detecting the current HOA mode of `SelR{k}` (the per-circuit horario selector):

```
SelR{k}.out (StatusNumeric) ──→ BStatusDemux.inNumeric
                                BStatusDemux.overridden  ── (BStatusBoolean, true when Manual)
                                BStatusDemux.inStatus   ← (can also bind inStatus directly)
```

`BStatusDemux.overridden` output:

```java
// BStatusDemux.java:226-234
int statusBits = this.getInNumeric().getStatus().getBits();
BStatus status = BStatus.make(statusBits);
this.getOverridden().setValue(status.isOverridden());  // → true when SelR{k} is manually overridden
```

`[CERT]` `organized/kitControl/kitControl-rt/vineflower/com/tridium/kitControl/util/BStatusDemux.java:226-234`

Derived mode detection:
- **isManual** = `BStatusDemux.overridden.value` is `true` (someone wrote to `SelR{k}`)
- **isAuto** = `IStatusToSimple` on `SelR{k}.out` → maps `ok` flag to display value (when no override active, `ok` bit = 1)
- **isOn (Manual-ON)** = `BAnd(isManual, SelR{k}.value > 0)` → implemented via `BAnd` + boolean comparison
- **isOff (Manual-OFF)** = `BAnd(isManual, NOT(SelR{k}.value > 0))` → `BAnd` + `BNot`

### 4.2 Rendering side: what works in Hx (from G6 + G8 + G3)

From the three preceding sections, the constraints on the Hx rendering are:

| Approach | Verdict in Hx |
|---|---|
| Picture (with file:^ image) BEHIND an ImageButton | FAILS — button's UA background covers it |
| ImageButton `buttonStyle="toolBar"` selected highlight | FAILS — no value-driven selected state for non-ToggleButton |
| LED/pill indicator beside the ImageButton (separate widget, same row) | WORKS — `IBooleanToSimple` on `BStatusDemux.overridden` renders correctly [CERT-live] B853 §6 |
| LED/pill bound to `IStatusToSimple` on `SelR{k}.out` | WORKS — same mechanism as the existing HorR{k} LED |

The CORRECT recipe for a selected-mode highlight indicator in Hx is:

```
BStatusDemux.overridden (BStatusBoolean)
    ──IBooleanToSimple──→ LED/pill widget
         trueValue  = [highlighted image, e.g. module://kitPxN4svg/…/indicator-green.svg]
         falseValue = [dim image, e.g. module://kitPxN4svg/…/indicator-grey.svg]
```

Place the LED NEXT TO the ImageButton (not behind it). The image paths should use `module://`
resources (long-term cached, always available) rather than `file:^` station assets.

If a `file:^` image is required, it must be uploaded to the station BEFORE the Px page is first
opened (or the page must be refreshed after upload). The `max-age=0` cache ensures the image is
fetched fresh on the next request once uploaded.

### 4.3 B853-G10 closure status

The logic side is **CLOSED [CERT]** — `BStatusDemux.overridden` is the correct flag and it resolves
from `SelR{k}.out`'s status bits. The rendering side is **CLOSED [CERT/INFER]** — use an LED/pill
indicator beside the button. The only remaining open item is confirming hypothesis (b) of G6 live
(B858-G1, requires execution).

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | FileServlet sends `Cache-Control: private, must-revalidate, max-age=0` for station `file:^` assets | [CERT] | FileServlet.java:65-66 (constant `cacheControlHeaderRevalidate`) + getCacheControlHeader logic |
| 2 | `Last-Modified` / `If-Modified-Since` is the only revalidation mechanism (no ETag) | [CERT] | FileServlet.java:208-229 |
| 3 | Module-based images (`BModule` / `BZipSpace`) get `max-age=2592000` (30 days) | [CERT] | FileServlet.java:270-272 + constants |
| 4 | No station restart needed for a newly uploaded `file:^` file to be served | [CERT] | `max-age=0` forces revalidation; new file has new `Last-Modified` → 200 response |
| 5 | `BHxPxPicture.update()` reads `picture.getImage().getOrdList().get(0)`; if null → `display:none` | [CERT] | BHxPxPicture.java:57-81 |
| 6 | `BHxPxImageButton.write()` calls `UxImageButtonUtil.write()` when no legacy label | [CERT] | BHxPxImageButton.java:38-44 |
| 7 | `UxImageButtonUtil.write()` emits `<button>` HTML element (opaque browser UA background) | [CERT] | UxImageButtonUtil.java:20-33 |
| 8 | Theme CSS for `toolBar` does NOT add `background: transparent` to the `<button>` element | [CERT] | web-rt/rc/theme/theme.css:380-407 |
| 9 | `buttonStyle="toolBar"` button covers a Picture placed behind it | [INFER] | Claims 7+8: button has opaque UA background; requires live confirmation (B858-G1) |
| 10 | `ux-Button-buttonStyle-selected` CSS class only added when `button instanceof BToggleButton && getSelected()` | [CERT] | UxButtonUtil.java:38-43 |
| 11 | `BImageButton` extends `BButton`, NOT `BToggleButton` → never gets `selected` class | [CERT] | BImageButton.java:33 class declaration |
| 12 | `buttonStyle="toolBar"` gives only hover CSS effect in Hx; no persistent value-driven highlight | [CERT] | Claims 10+11 |
| 13 | `BStatusDemux.overridden` = `true` when `SelR{k}` has its `overridden` status bit set | [CERT] | BStatusDemux.java:234 |
| 14 | LED/pill with `IBooleanToSimple` bound to `BStatusDemux.overridden` works in Hx browser | [CERT-live] | B853 §6 (same pattern works for pill/LED) |
| 15 | `file:^` images in IStatusToSimple/IBooleanToSimple need to exist at station before first page open | [CERT] | BHxPxPicture.java:57-81 (returns null → no display if OrdList empty) |

**Tally:** [CERT] × 12 · [CERT-live] × 1 (via B853) · [INFER] × 1 · [INFER] inheriting [CERT] × 1 = 15 claims.

---

## Connections

- **B851** — control chain; `BStatusDemux.overridden` + `BAnd`/`BNot` mode-detection logic first cited here as known.
- **B852** — Px UI design; this block constrains the highlight display approach for the tablero.
- **B853** — live empirical source of the G6/G8/G10 cluster; the failing `BotonVerde.png` scenario.
- **B856** — Hx vs bajaux engine reference; B856-G3 (ImageButton toolBar mechanism) is closed here.
- **B184** — converters block (`IBooleanToSimple`, `IStatusToSimple` internals) — confirms converter type
  check `v.getType() == to.getType()` constrains BImage-typed Picture.image slots.

---

## Open gaps

| Gap | Type | Description |
|---|---|---|
| **B858-G1** | requires-execution | Confirm hypothesis (b) from G6 live: open a tablero in Hx, inspect the ImageButton `<button>` element background in browser DevTools, toggle `background: transparent` and verify the Picture becomes visible. Closes G6 from [INFER] to [CERT-live]. |
| **B853-G6** (partial) | investigable | Partially closed — dominant hypothesis (b) identified as [INFER]; B858-G1 provides the live confirmation. |
