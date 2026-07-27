# PX dropdown menu — working patterns

Applied deliverables of the `px-menu` focus (Blocks 179-190, corrected and extended by **B289-B292**).

There is **no native Menu Button / Dropdown widget** in the PX catalog (B179 §179.1), so it is emulated.
Three routes, ordered by what they actually deliver. Read the verdict before picking.

## Verdict first

| Route | Anchored dropdown? | Overlaps other widgets? | Needs a station point | Needs a compiled module |
|---|---|---|---|---|
| **A — `PopupBinding`** | **No** — opens a *window* | n/a | no | no |
| **B — in-place toggle** | **Yes** | no (PX canvas can't) | yes | no |
| **C — hosted HTML** | **Yes** | **yes** | no | no |

**Pattern A does not produce a dropdown.** `BPopupBinding` imports `BNiagaraWbDialog`, its javadoc says
"popping up new views *in a window*", and its `title` defaults to the literal `"Pop up"` — the caption
users see on the floating window, in Workbench *and* in the browser (B292 §292.5). It is the right tool
for opening a view in a window, and the wrong one for a menu.

## Files

| File | Route | Role |
|---|---|---|
| `menu.px` | A | The popup contents. Kept as the reference for the `GridPane` + `ValueBinding hyperlink` idiom. |
| `host-example.px` | A | Host graphic with the trigger `Button` + `PopupBinding`. |
| `dropdown-pattern-b.px` | B | **Recommended for an anchored dropdown.** `ToggleButton` + `SetPointBinding` + `visible` toggle. |

## Install

Copy the `.px` into the station's **`shared/px/`** folder:

```
<station-home>/shared/px/menu.px
```

**Not `<station-home>/px/`.** The file-space root that `^` resolves to is the station's `shared/`
subfolder, not the station folder itself (B289 §289.3). A file placed one level up is invisible to
Workbench and unresolvable by the ord.

To open it by ord, chain `file:` directly after `foxs:` — **never after `station:`**, which raises
`UnknownSchemeException` (B289-G1):

```
local:|foxs:|file:^px/menu.px
```

## Pattern B — the anchored dropdown (recommended)

Needs one `BooleanWritable` in the station; drag it from the **kitControl** palette ("Boolean Points").

```xml
<ToggleButton layout="16.0,52.0,140.0,26.0" text="Menu" selected="false">
  <SetPointBinding ord="station:|slot:/Drivers/PRUEBAS/MenuOpen" widgetEvent="actionPerformed" widgetProperty="selected"/>
</ToggleButton>

<GridPane layout="16.0,78.0,140.0,130.0" columnCount="1" rowGap="1" columnAlign="fill" uniformColumnWidth="true" visible="false">
  <ValueBinding ord="station:|slot:/Drivers/PRUEBAS/MenuOpen" degradeBehavior="hide">
    <IBooleanToSimple name="visible"/>
  </ValueBinding>
  <!-- one Label / ImageButton per entry -->
</GridPane>
```

Click flips `selected` → `SetPointBinding` writes it to the point → the panel's `ValueBinding` maps it onto
`visible`. Confirmed working on a live station (B292 §292.4).

Two things that fail silently:

- **Slot names are case-sensitive.** A point named `MenuOpen` bound as `menuOpen` gives a dead button with
  no error.
- A fresh `BooleanWritable` reads **null** until first written. `degradeBehavior="hide"` makes that mean
  "panel hidden", which is the right initial state — but set its `fallback` to `false` anyway.

## Pattern C — when the menu must overlap the content

The PX canvas cannot render a menu *over* other widgets. Neither can the commercial modules — their own
docs admit it. If overlapping submenus are a hard requirement, host an HTML/CSS/JS page from the file
space with `BWebBrowser` (B291): no module, no build.

```xml
<WebBrowser layout="0.0,0.0,1920.0,56.0" ord="file:^px/webmenu/menu.html" title="" showProgressIndicator="false" contextMenuEnabled="false"/>
```

Constraints that come with it (B291 §291.4-§291.6):

- The page is injected into an `about:blank` iframe, so **root-relative asset paths do not resolve**.
  Embed images as `data:` URIs — the station's CSP allows `img-src ... data:`.
- **No external-CDN JavaScript**: `script-src` is `'self' workbench 'unsafe-inline' 'unsafe-eval'`. Package
  libraries locally. External CSS from cdnjs and Google Fonts *is* allowed.
- **ES5 only** — the framework's own comment: *"DO NOT convert to ES6 class - this will break"* (B204).
- Navigate back with `/ord/<url-encoded-ord>` plus `target="_top"` to escape the iframe.

**This is hosting, not protection.** Everything served this way is readable from Workbench `Files` and in
the client's browser devtools. Hiding the "HTML Viewer" chrome is cosmetic. Real protection means a signed
compiled module, server-side logic in an `-rt` servlet, and licensing (B291 §291.7).

## Hard rules for hand-written `.px`

- **A widget's opening tag, with every attribute, goes on ONE line.** Splitting it raises
  `XException: Expecting '='` from `javax.baja.xml.XParser` (B181 §181.5). Child elements such as
  `<ValueBinding/>` do go on their own line — they are separate elements.
- `<import>` must declare every module used. `PopupBinding` and `SetPointBinding` need `kitPx`;
  `IBooleanToSimple` needs `converters`; `WebBrowser` needs `workbench` and `jxBrowser`.
- XML validity is **necessary but not sufficient**: a `.px` can parse as XML and still be un-openable if it
  uses non-PX widgets (B289 §289.6 documents a JavaFX-flavoured file that does exactly that).
