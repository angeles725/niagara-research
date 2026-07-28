# Block 293 — Menu inheritance across views: the official `PxInclude` route vs the HTML shell, and the active-tab state the `.px` route still cannot express

> **DOCUMENT-MODE block (METHODOLOGY §20)** — CAPTURES an applied session against a live station,
> driven by a customer request, not by gap discovery.
>
> **The customer's ask** (site operator, over WhatsApp): after clicking an entry inside the dropdown, the
> parent tab must stay shaded — *"que se quede sombreado una vez pinchas"* → *"en ese caso se quedaría
> sombreada la pestaña Dropdown"*. Follow-up from the integrator: make the menu **inherited** by the
> content pages instead of repeated in each one.
>
> **Method note — the official docs paid, again.** This block was one step away from recommending the HTML
> shell as the *only* real route. A `guide-search "PxInclude"` returned a Tridium guide literally titled
> **"Create a global navigation menu using PxInclude"**. Skipping source 2 would have produced a false
> "there is no official pattern for this" — the exact failure the project protocol was written against.
>
> **Sources**: (a) Tridium original source — `javax.baja.ui.BBorder`, `javax.baja.ui.enums.BHalign`;
> (b) official Tridium documentation — `guides-clean/Graphics/docGraphics_CreatingANavigationMenu-1D868C52.txt`;
> (c) the LIVE station `PRUEBAS` (OptimizerSupervisor N4.14.0.162, Honeywell) — its own `.px` files as the
> imitation baseline, and the deployed artifacts of this session.
>
> Markers: `[CERT]` verbatim in local code · `[CERT-doc]` verbatim in official Tridium documentation ·
> `[CERT-live]` empirical against the running station · `[INFER]` derived. **Corpus language: ENGLISH.**
>
> **SECRETS DISCIPLINE (live-install)**: structure only, no credential values.
>
> PX layer (applied). Continues [Block 291] (the PX↔web bridge) and [Block 292] (the toggle).
> Connects [Block 188] (`BPxInclude`), [Block 189] (the applied menu), [Block 183] (gx serialization).

---

## 293.1 — Two routes to an inherited menu, and they are not interchangeable `[CERT-doc]` `[CERT-live]`

The question "can the menu be inherited?" has two answers in N4, and the choice is decided by **which views
the menu has to survive**, not by taste.

| | **Route A — HTML shell** | **Route B — `PxInclude`** |
|---|---|---|
| Menu defined in | `shell.html` (one file) | `navbar.px` (one file) |
| Inherited by | **every** view, including native ones | only `.px` files you author |
| Reloads on navigate | **no** — only the content iframe swaps | yes — the whole page re-renders |
| Anchored dropdown | yes (CSS) | needs the B292 toggle, one point per menu |
| Active-tab state | yes (CSS class) | **not expressible today** — see §293.5 |
| Official Tridium pattern | no | **yes** `[CERT-doc]` |
| Depends on | `BWebBrowser` / jxBrowser | pure PX |

The load-bearing asymmetry: **a native view cannot embed anything.** `PropertySheet`, `AlarmDbView` and the
chart builders are rendered by the framework, not authored, so there is nowhere to put a `PxInclude`. Any
navigation that reaches them loses the Route B menu at that moment. `[INFER]`

Route A does not have that failure mode because the menu never lives in the view — it lives one frame above
it, and the view is the *content*, not the host. `[INFER]`

## 293.2 — Route B is the documented Tridium pattern `[CERT-doc]`

The guide is explicit about both the intent and the maintenance property:

> "You can have a consistent navigation menu available on your home page or on all pages and you need only
> edit the navigation menu page any time a change is required. Since the navigation menu Px file is a
> PxInclude on the other Px views, they are automatically updated with any change that you make."

`guides-clean/Graphics/docGraphics_CreatingANavigationMenu-1D868C52.txt` `[CERT-doc]`

Its procedure, condensed: create a Px view for the menu → drag **Action Button** widgets from the `kitPx`
palette, one per destination → give each a binding whose `ord` is the destination `.px` → embed it in every
other page by dragging the **PxInclude** widget from the `bajaui` palette. `[CERT-doc]`

**The gotcha the guide states and nothing else does:** `[CERT-doc]`

> "NOTE: Remove the ScrollPane at the root in this view otherwise scroll bars display on the menu once it
> is embedded in other Px views."

A new Px view is created with a root `ScrollPane`, so the default output of the editor is *wrong* for this
use and fails visually only after embedding — i.e. one step after the author would look for the cause.
`[INFER]` The station's existing `navbar.px` has zero `ScrollPane` occurrences and therefore complies.
`[CERT-live]`

The site already runs this pattern elsewhere, which is independent confirmation of the shape:

```xml
<PxInclude layout="0.0,0.0,1000.0,130.0" ord="file:^px/Header.px"/>
```
`shared/px/Floorplan.px` `[CERT-live]` — a header, not a menu, but structurally the same inheritance.

## 293.3 — Route A: the shell, and why the frame is what makes inheritance free `[CERT-live]`

[Block 291] established that an `.html` in the file space is served with no build step, that `/file/...`
returns the raw document while `/ord/file:...` wraps it in the Hx profile, and that links back into Niagara
need `/ord/<encoded-ord>` with `target="_top"` to escape that wrapper. `[CERT-live]` (by reference)

The shell changes exactly one thing in that recipe, and it changes everything downstream: **`target` stops
being `_top` and becomes the name of a content iframe.**

```html
<div class="shell">
  <div class="navbar" id="navbar"> … <a href="/ord/file:%5Epx/Vistas/Equipos.px" target="content">Equipos</a> … </div>
  <iframe class="content" name="content" id="content" src="/ord/file:%5Epx/Vistas/Planta.px"></iframe>
</div>
```
`shared/px/webmenu/shell.html`, **first draft** `[CERT-live]`

> ⚠ **Do not copy the `href` above.** This literal form works in a browser and **fails inside
> `BWebBrowser`** — see §293.10. The shipped file stores the ord in `data-ord` and builds the `href` per
> host at load time. The `target="content"` attribute, which is the actual subject of this section, is
> unaffected.

Three consequences follow from that single attribute: `[INFER]`

1. **Inheritance is free.** The navbar is not copied into the views; the views are loaded *underneath* it.
   A page has to opt out of nothing and declare nothing.
2. **The active state stops needing persistence.** The navbar document is never re-parsed, so a CSS class
   set on click simply stays. `sessionStorage` degrades from mechanism to convenience — it now only covers a
   manual F5 of the shell itself.
3. **The dropdown stops being clipped.** In [Block 291]'s layout the `WebBrowser` was a 56 px strip
   (`layout="0.0,0.0,1920.0,56.0"`), so an absolutely-positioned `.dropdown` at `top: 56px` opened *outside*
   the widget's box. The shell is full-screen, so the panel has room to render inside the same document.

The host graphic collapses to one widget over the whole canvas: `[CERT-live]`

```xml
<CanvasPane name="content" viewSize="1920.0,1080.0" scale="fitRatio" minScaleFactor="0.5" maxScaleFactor="1.5" background="#3b6ea5">
  <WebBrowser layout="0.0,0.0,1920.0,1080.0" ord="file:^px/webmenu/shell.html" title="" showProgressIndicator="false" contextMenuEnabled="false"/>
</CanvasPane>
```
`shared/px/Shell.px` `[CERT-live]`

**Nesting cost, stated honestly:** a `.px` reached as `/ord/file:^…` inside the content frame is served by the
Hx wrapper, which carries its own `servletViewWidget` iframe ([Block 291] §291.3). The delivered result is
therefore shell-frame → wrapper → view-frame. It renders, but it is three documents deep, and
§291.4's base-URL trap applies to any *authored* page placed at that depth. `[INFER]`

## 293.4 — The active-tab state, and where the state actually has to live `[CERT-live]`

The customer's complaint had a cause that is invisible from the CSS: the deployed `menu-v2.html` styled only
`.open` — the shading *while* the panel is unfolded — and had no `.active` at all. `[CERT-live]` Adding a
class on click is not sufficient either, because with `target="_top"` the whole window navigates and the
menu document is rebuilt from scratch, discarding every in-memory flag. `[INFER]`

So the rule is: **the highlight must outlive whatever gets destroyed by navigation.**

| Layout | What navigation destroys | Where the active state must live |
|---|---|---|
| Bare navbar, `target="_top"` | the menu document itself | `sessionStorage`, re-applied on load |
| Shell + content iframe | only the content document | a DOM class on the navbar (storage optional) |
| Route B (`PxInclude`) | the whole page including the menu | a **station point** — see §293.5 |

The deployed fix covers the first two with the same code — it marks the class on click, remembers the `href`,
and on load re-applies it by walking up from the matching `<a>` to its `.item` parent: `[CERT-live]`

```js
function markActive(href) {
  …
  for (i = 0; i < links.length; i++) {
    if (links[i].getAttribute('href') === href) {
      addClass(links[i], 'active');
      parent = links[i];
      while (parent && !hasClass(parent, 'item')) { parent = parent.parentNode; }
      if (parent) { addClass(parent, 'active'); }
      return;
    }
  }
}
```

Walking to the `.item` parent is the part that answers the customer literally: the request was not to
highlight the clicked entry but **the tab that owns it**. `[INFER]` ES5 throughout, per the CSP and dialect
rules of [Block 291] §291.5. `[CERT]` (by reference)

## 293.5 — Why Route B cannot express the active tab, and the wrong reason it is tempting to give `[CERT]`

The conclusion holds, but the first reason this block reached for is NOT the binding one. Stating it
carefully, because the distinction decides whether the problem is solvable by finding a better widget.

**Mutual exclusivity is a solved problem — twice.** `BButtonGroupBinding` ([Block 292] §292.6) generates
radio-style buttons from a bound point's range. `[CERT]` (by reference) And the site's own OEM stack ships a
second, lighter mechanism that needs **no station point at all**:

```java
public class BRadioButtonGroupBinding extends BBinding {
   public static final Property groupName  = newProperty(0, "", null);
   public static final Property groupScope = newProperty(0, BToggleButtonGroupScopeEnum.universal, null);
```
`genericUIFramework/.../com/honeywell/baja/ui/creator/BRadioButtonGroupBinding.java:12-14` `[CERT]`

It is used exactly as a tab strip would want, grouping independent buttons by name:

```xml
<RadioButton text="rd1" selected="true">
  <WidgetIdBinding ord="wid:rd1"/>
  <RadioButtonGroupBinding groupName="g1"/>
</RadioButton>
```
`genericUIFramework/.../creator/demo/Include.px` `[CERT]` — note this is **OEM Honeywell**
(`com.honeywell.baja.ui.creator`), not framework base, so it is available on this station but is not a
portable N4 assumption. `[INFER]` The base framework's `BRadioButton` declares only `halign` of its own and
delegates exclusivity elsewhere — its javadoc says it "is used with groups of other BRadioButton's to
provide a choice which is exclusive of other options", without owning the grouping. `[CERT]`
(`javax/baja/ui/BRadioButton.java:14-19,48`)

**The real blocker is persistence, not exclusivity.** Under Route B every navigation re-renders the whole
page, and the embedded `PxInclude` menu is re-rendered with it — so `selected` returns to whatever the file
declares. A group binding makes the highlight *exclusive*; nothing makes it *survive the click that
navigates away*. `[INFER]` PX has no scripting ([Block 181]), so the only thing that outlives a re-render is
a value in the station — the same conclusion [Block 292] reached for the boolean toggle, now at
enumeration width. `[CERT]` (by reference)

Practical consequence for the site: choosing Route B gets an inherited menu, and with the OEM group binding
even a correct-looking tab strip *within* a page load — but the "shaded tab" the customer asked for needs a
station-side enum point written on each navigation. That is **B292-G1** territory and it is still open.
`[INFER]`

## 293.6 — Dialect corrections from imitating the station's real `.px` `[CERT]` `[CERT-live]`

Three deltas between what the corpus deliverables write and what the station's own PX Editor emits. All
three were found by diffing against real files before shipping, and two of them would have been invisible
until the editor complained.

**(a) The prolog.** Corpus deliverables open with `<px version='1.0'>`. Every graphic the editor writes on
this station opens with an XML declaration, a marker comment, and a `media` attribute: `[CERT-live]`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Niagara Presentation XML -->
<px version="1.0" media="workbench:WbPxMedia">
```

**(b) Quoting.** The editor emits double quotes; several corpus samples use single quotes. Both are
well-formed XML, so this is style, not correctness — but it is the difference between a file that reads as
hand-written and one that reads as editor-produced. `[INFER]`

**(c) `border` component order — a suspicion investigated and DISCHARGED.** The station writes
`border="1.0 solid black"` (width first); this session first wrote `border="solid 1 #d0d5dc"` (style first)
and it looked like a defect. The BNF says otherwise:

```
 all    := [style] || [width] || [brush]
```
`docSource/.../bajaui-wb/javax/baja/ui/BBorder.java:33` `[CERT]` — `||` is CSS notation for
*any order*, and the parser implements it as a three-pass loop that accepts whichever component matches:

```java
for (int i=0; i<3; i++)
{
  if (side.width == -1) side.width = parseWidth(p);
  if (side.style == -1) side.style = parseStyle(p);
  if (side.brush == null) side.brush = parseBrush(p);
}
if (side.width == -1) side.width = 1;
if (side.style == -1) side.style = SOLID;
if (side.brush == null) side.brush = BColor.black.toBrush();
```
`BBorder.java:262-272` `[CERT]`

So style-first parses. The deliverables were still switched to width-first, for a different reason: with
width first every component matches on pass 1, whereas style-first relies on `parseBrush` declining a
numeric token before the loop comes back around. Same result, fewer assumptions. `[INFER]` Documented values
per the javadoc: `width := double`, `style := "none" | "solid" | "dotted" | "dashed" | "groove" | "ridge" |
"inset" | "outset"`, plus per-side forms `top(…) right(…) bottom(…) left(…)`. `[CERT]`

Related check while writing the pages: `halign="left"` is valid — `BHalign` declares
`left | center | right | fill` and `DEFAULT = left`. `[CERT]`
(`javax/baja/ui/enums/BHalign.java:48-74`) It was dropped from the deliverables as redundant, not as wrong.

## 293.7 — What was deployed `[CERT-live]`

Under `<station>/shared/px/` on `PRUEBAS`:

| File | Route | Role |
|---|---|---|
| `webmenu/menu-v3.html` | — | The bare navbar with the fix: `.active` CSS + `sessionStorage`. Direct successor of `menu-v2.html`, which the customer already has. |
| `webmenu/shell.html` | A | Navbar + content iframe. The inheritance carrier. |
| `Shell.px` | A | Full-screen `WebBrowser` host. The only graphic that carries the menu. |
| `Vistas/Planta.px` · `Equipos.px` · `Historicos.px` | A | Content pages. Deliberately menu-free. |
| `Vistas/Planta-heredado.px` | B | The official `PxInclude` route against the station's existing `navbar.px`. |

`menu-v2.html` was left untouched so the link already sent to the customer keeps working. `[CERT-live]`
All four `.px` and the shell parse; the `.px` files were checked well-formed with an XML parser. `[CERT-live]`

**Not yet verified**: nothing here has been opened in a browser or in Workbench. The station answers `302`
to an unauthenticated request, so no live render was obtained this session. Every `[CERT-live]` above is
about **file content and site baseline**, never about rendered behaviour. `[CERT-live]`

## 293.8 — Where the `.px` templates actually are: ~250 shipped inside modules `[CERT]`

Asked directly whether template `.px` files exist in the modules or the documentation. They do — the docs
have none, the modules have hundreds. Measured over the extracted corpus, `.px` files per module
(deduplicated across decompiler variants, so treat these as relative weights):

```
honeywellVenomGraphics 60 · honeywellSpyderTool 48 · ascCommon 40 · honeywellAXPlatinumHR 20
honeywellAXPlatinum 20 · ascLon 12 · ascBacnet 10 · genericUIFramework 8 · tls 6
honeywellLonSpyder 6 · CentralineLONIOr5 6 · hx 4 · honeywellSylkDevice 4 · docDeveloper 4
axvelocity 4 · easyTemplating 2 · honeywellFunctionBlocks 2 · honBACnetUtilities 2 …
```
`[CERT]` The mass is OEM equipment graphics (`ascCommon/resources/graphics/VAVGraphics.px`,
`CVAHUGraphics.px`), not framework samples. `[CERT]`

**The find that matters: the editor's own base template.** `easyTemplating-wb` ships `res/PxFile.px`, and it
is the skeleton a new Px view starts from: `[CERT]`

```xml
<px version='1.0'>
<import>
  <module name='gx'/>
  <module name='bajaui'/>
</import>
<content>
  <ScrollPane>
    <CanvasPane name="content" viewSize="1000.0,800.0" scale="fitRatio" minScaleFactor="0.5" maxScaleFactor="1.0"/>
  </ScrollPane>
</content>
</px>
```
`easyTemplating/easyTemplating-wb/extracted/res/PxFile.px` `[CERT]`

Two things fall out of that file.

1. **It explains §293.2's gotcha at the source.** The guide's "Remove the ScrollPane at the root" is not
   advice about an unusual case — the root `ScrollPane` is in the *template*, so every new Px view has it
   by construction, and a menu authored the normal way is wrong by default until that node is deleted.
   `[INFER]`
2. **It qualifies §293.6(a).** The template uses `<px version='1.0'>` with single quotes and no `media`
   attribute — exactly the dialect the corpus deliverables were written in. The XML prolog, the
   `Niagara Presentation XML` comment and `media="workbench:WbPxMedia"` are added by the editor when it
   **saves**. So the corpus samples are not wrong; they are template-shaped rather than save-shaped.
   `[CERT]` `[INFER]`

`genericUIFramework/.../creator/demo/{Demo,Include}.px` is the other useful pair: a two-file
`PxInclude` demo, and the source of the `RadioButtonGroupBinding` idiom quoted in §293.5. `[CERT]`

Documentation side: three separate `guide-search` queries for templates returned nothing (§293.9 table).
The templates are a **module artifact, not a documented one** — which is why looking for them in the guides
first would have produced a false negative. `[INFER]`

## 293.9 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| Tridium documents a global nav menu via `PxInclude` | guide title + body quoted | `[CERT-doc]` |
| The guide's maintenance claim (edit once, all pages update) | quoted verbatim | `[CERT-doc]` |
| The guide requires removing the root `ScrollPane` | NOTE quoted verbatim | `[CERT-doc]` |
| `navbar.px` on the station has no `ScrollPane` | `rg -c ScrollPane navbar.px` → 0 | `[CERT-live]` |
| The site already uses `PxInclude` for a header | `Floorplan.px` line quoted | `[CERT-live]` |
| Native views cannot embed a `PxInclude` | they are framework-rendered, not authored | `[INFER]` |
| ⇒ Route B loses the menu on native views | derived from the above | `[INFER]` |
| The shell swaps `target="_top"` for a frame name | `shell.html` quoted | `[CERT-live]` |
| ⇒ the navbar document is never rebuilt | derived | `[INFER]` |
| ⇒ storage is optional under Route A | derived | `[INFER]` |
| B291's 56 px strip clipped the dropdown | B291 layout vs `top: 56px` rule | `[INFER]` |
| `/ord/file:` inside the frame adds the Hx wrapper | B291 §291.3 by reference | `[CERT-live]` |
| ⇒ the delivered result is three documents deep | derived | `[INFER]` |
| `menu-v2.html` styled only `.open`, never `.active` | deployed file inspected | `[CERT-live]` |
| The deployed fix walks up to the `.item` parent | `markActive` quoted | `[CERT-live]` |
| PX has no scripting, so the flag must be a station value | B181 by reference | `[CERT]` |
| `BButtonGroupBinding` generates buttons without hyperlinks | B292 §292.6 by reference | `[CERT]` |
| `BRadioButtonGroupBinding` has `groupName` + `groupScope`, needs no point | source quoted `:12-14` | `[CERT]` |
| …and it is OEM Honeywell, not framework base | package `com.honeywell.baja.ui.creator` | `[CERT]` |
| Base `BRadioButton` declares only `halign`, delegates grouping | javadoc `:14-19` + `:48` | `[CERT]` |
| ⇒ exclusivity is solved; **persistence** is the real blocker | derived | `[INFER]` |
| ⇒ Route B cannot show an active tab today (B292-G1) | derived | `[INFER]` |
| ~250 `.px` ship inside modules; docs ship none | per-module count | `[CERT]` |
| `easyTemplating/res/PxFile.px` is the new-view skeleton | file quoted in full | `[CERT]` |
| That template has a root `ScrollPane` | same file | `[CERT]` |
| ⇒ the guide's gotcha is a template default, not an edge case | derived | `[INFER]` |
| The template uses single quotes and no `media` attribute | same file | `[CERT]` |
| ⇒ corpus samples are template-shaped, not wrong | derived | `[INFER]` |
| The station's editor emits `<?xml?>` + `media="workbench:WbPxMedia"` | real `.px` files inspected | `[CERT-live]` |
| `BBorder` BNF is order-free (`||`) | `BBorder.java:33` quoted | `[CERT]` |
| The parser implements it as a 3-pass loop | `BBorder.java:262-272` quoted | `[CERT]` |
| ⇒ style-first was NOT a defect; suspicion discharged | derived from the two above | `[INFER]` |
| `BHalign` declares `left|center|right|fill`, default `left` | `BHalign.java:48-74` | `[CERT]` |
| Nothing was rendered live this session (302 unauthenticated) | probe | `[CERT-live]` |
| §293.10 `Href2Ord` wraps a non-scheme href and merges it onto the base ord | `Href2Ord.java:21-32` quoted | `[CERT]` |
| `baseOrd()` drops the filename for html/css/bajadoc | `Href2Ord.java:34-57` | `[CERT]` |
| ⇒ B291 §291.6's `/ord/` rule is browser-profile only | derived from the two above | `[INFER]` |
| §293.11 the click navigates the iframe, navbar unchanged | 4 captures | `[CERT-live]` |
| §293.11 the menu survives a native `AlarmDbView` | capture | `[CERT-live]` |
| §293.12 `fullScreenKey = "fullScreen"`, default `"false"` | `BHxProfile.java:173,290-292` quoted | `[CERT]` |
| §293.12 it is a **view** parameter, so it follows a view query | `getViewParameter` + the 403 URL shape | `[CERT]` `[CERT-live]` |
| §293.12 the chrome is gone after the fix | reload capture, `HistoryChartBuilder` | `[CERT-live]` |
| §293.12 a third native view inherits the menu | same capture | `[CERT-live]` |
| §293.12 the `\|view:`-already-present branch works | ord `history:\|view:history:…` rendered correctly | `[CERT-live]` |

Tally: **[CERT] 12 / [CERT-doc] 3 / [CERT-live] 9 / [INFER] 13.**

### Queries that returned a REAL zero (recorded so the next pass does not retry)

| Source | Literal query | Result |
|---|---|---|
| niagara-help | `guide-search "web browser widget px"` | `No guide text matches` |
| niagara-help | `guide-search "navigation bar graphic reuse"` | `No guide text matches` |
| niagara-help | `guide-search "Px template"` | `No guide text matches` |
| niagara-help | `guide-search "graphic template"` | `No guide text matches` |
| station `.px` | `rg -o '<PxInclude[^>]*>'` for a *menu* include | only header/image includes; no menu include existed before this session |

The `guide-search "PxInclude"` query, by contrast, returned **18 files** including the titled navigation-menu
guide of §293.2 — the same source that returned four zeros above. Calibration for the next pass: for PX,
niagara-help answers to the **widget/class name**, not to the described intent. `[INFER]`

`niagara_help.py freshness` reported all four corpora `OK` (bajadoc 3,589 · source 2,610 · guides 19,557 ·
devguide 7,328 files), so the two zeros are real zeros, not tool failures. `[CERT-live]`

---

## 293.10 — FIRST LIVE RENDER: `Href2Ord` rewrites links inside `BWebBrowser`, and it inverts B291 §291.6 `[CERT]` `[CERT-live]`

Added after §293.9: the operator opened `Shell.px` in **Workbench**. Two results, and the second closes most
of **B291-G1**.

**The navbar renders.** Logo, `Planta`, `Vistas ▾`, `Sistema ▾` all display inside the Px View. So
`BWebBrowser` hosting a `/file/`-authored page works under the Workbench profile — which is half of
**B291-G2**, previously unverified. `[CERT-live]`

**Every link 404s or 403s.** Two captured failures: `[CERT-live]`

```
404  /ord/file:%5Epx/webmenu/null
403  /ord/file:/ord/file:%255Epx/Dashboard/Bien2.px%7Cview:?fullScreen=true
```

The second is self-describing: `file:` + the *entire href* as a suffix, with `%5E` re-escaped to `%255E`.
The href was not followed — it was **merged into the widget's own ord**. The mechanism is in `baja`:

```java
public BOrd hrefToOrd(String href) {
   if (!this.isAlreadyOrd(href)) {
      href = new FilePath(this.fileScheme(), href).toString();
   }
   return BOrd.make(this.baseOrd(), href).normalize();
}

public boolean isAlreadyOrd(String href) {
   int colon = href.indexOf(58);
   return colon == -1 ? false : Sys.getRegistry().isOrdScheme(href.substring(0, colon));
}
```
`baja/com/tridium/util/Href2Ord.java:21-32` `[CERT]`, reached from
`BWebBrowserView.java:540` — `href.startsWith("#") ? BrowserUtil.mergeFilePathFragment(this.ord, href)
: new Href2Ord(this.ord).hrefToOrd(href)` `[CERT]`

**The trap, precisely.** `isAlreadyOrd` tests the substring **before the first colon**. For
`/ord/file:%5Epx/…` that substring is **`"/ord/file"`** — not a registered ord scheme — so the test fails,
the href is wrapped in `file:`, and `BOrd.make(baseOrd, …)` concatenates it. `[INFER]` And `baseOrd()`
(`:34-57`) deliberately drops the filename for `html`/`css`/`bajadoc`, resolving to the *parent folder* —
which is why the first failure landed in `…/webmenu/`, the shell's own directory. `[CERT]`

**This inverts [Block 291] §291.6.** That section states links "use `/ord/<url-encoded-ord>`, and must carry
`target="_top"`". That rule is correct **for the browser profile**, where the page is plain HTML served by
`/file/` and the browser resolves the URL itself. Inside `BWebBrowser` the requirement is the **opposite**:
pass the **raw ord**, because that is the only form `isAlreadyOrd` accepts. `[CERT]`

| Host | Link form that works | Why |
|---|---|---|
| Real browser (Hx profile) | `/ord/file:%5Epx/x.px` | plain URL, resolved by the browser |
| `BWebBrowser` (Workbench) | `file:^px/x.px` | `substring(0,4)` = `"file"` → a registered scheme |

`station:\|slot:…` and `history:\|view:…` pass the same test on `"station"` / `"history"`. `[INFER]`
B291 §291.6 has been edited in place with a pointer here.

**The fix deployed**: the ord became the single source of truth in `data-ord`, and the `href` is built at
load time for whichever host is rendering: `[CERT-live]`

```js
var isBrowser = /^https?:$/.test(window.location.protocol);
function ordToHref(ord) {
  if (!isBrowser) { return ord; }                 /* raw ord: passes isAlreadyOrd() */
  return '/ord/' + ord.replace(/\|/g, '%7C').replace(/\^/g, '%5E');
}
```

## 293.11 — SECOND LIVE RENDER: the shell works end-to-end in the browser profile `[CERT-live]`

`https://localhost/file/px/webmenu/shell.html`, four navigations captured. **Everything the customer asked
for works, and the central claim of §293.1 is confirmed by observation rather than by argument.**

| Observed | What it settles |
|---|---|
| `Planta` clicked → content swaps, tab underlined amber, navbar unchanged | frame isolation holds: the click navigates the **iframe**, not the window → **B293-G6 closed** |
| `Vistas ▸ Equipos` → content swaps, **`Vistas`** (the parent tab) stays underlined | the customer's literal request — the *owning tab* shades, not the entry → §293.4 confirmed |
| `Vistas ▸ Dashboard` → the site's real `Dashboard/Home.px` renders (ALSER BUILDING, live temps 18.45/23.33/21.72 °C, alarm counts 9/0/2, 3D model) with the navbar above it | an existing production graphic drops into the shell untouched — no edit to the page being inherited |
| `Sistema ▸ Alarmas` → **`AlarmService` Database View**, a NATIVE framework view, navbar intact, `Sistema` underlined | **the load-bearing claim**: the menu survives a view that cannot embed a `PxInclude`. Route A ≠ Route B, demonstrated. |

The status bar in the third capture reads `https://localhost/ord/file:%5Epx/Dashboard/Home.px` — the
browser branch of `ordToHref` (§293.10) emitting the encoded URL form, confirming the dual-host resolver
picks the right dialect from `location.protocol`. `[CERT-live]`

**Residue, visible in every capture:** the content frame carries Niagara's own Hx chrome — a `Px View` tab
strip, the view-selector dropdown, and the left icon rail. That is the wrapper of [Block 291] §291.3, i.e.
the nesting cost §293.3 predicted, now seen rather than inferred. `[CERT-live]` The 403 captured in §293.10
shows the framework's own URLs carry **`?fullScreen=true`**, which is the likely lever for suppressing it —
untested. → **B293-G2** narrowed. `[INFER]`

**Still unverified**: the same click test under **Workbench**, where `BWebBrowserView:540` intercepts at
widget level. The browser profile has no such interception, so this result does NOT generalise to Workbench.
`[INFER]`

## 293.12 — Suppressing the Hx chrome: `fullScreen` is a VIEW PARAMETER `[CERT]`

The chrome seen in §293.11 is drawn by the Hx profile, and it is switchable. The flag is not guesswork —
it is a named constant read from the view parameters:

```java
public static final String fullScreenKey = "fullScreen";
```
`hx-wb/javax/baja/hx/BHxProfile.java:173` `[CERT]`

```java
public boolean isFullScreen(BHxView view, HxOp op) {
    return "true".equals(op.getViewParameter(fullScreenKey, "false"));
}
```
`BHxProfile.java:290-292` `[CERT]` — note the default: **`"false"`**. The chrome is opt-out, which is why
every capture in §293.11 carried it without anyone asking for it. `[INFER]`

`BDefaultHxProfile` consumes the flag as a plain branch around the chrome —
`boolean fullScreen = this.isFullScreen(view, op); if (!fullScreen) { … }` — and, when set, emits
`hx.setFullScreen(true);` into the page and adds an `hx-outer-fullscreen` class.
`BDefaultHxProfile.java` / `BHxProfile.java:407-408` `[CERT]`

**It is a *view* parameter, not a free query argument**, so it has to follow a view query. That matches the
URL shape Niagara itself produced in the §293.10 403: `…Bien2.px%7Cview:?fullScreen=true` — an **empty**
`|view:` carrying the parameter. `[CERT-live]` An ord that already names a view (e.g.
`station:|slot:/Services/AlarmService|view:alarm:AlarmDbView`) must not get a second one, so the resolver
branches:

```js
function ordToHref(ord) {
  if (!isBrowser) { return ord; }
  var withView = ord.indexOf('|view:') !== -1 ? ord : ord + '|view:';
  return '/ord/' + withView.replace(/\|/g, '%7C').replace(/\^/g, '%5E') + '?fullScreen=true';
}
```
`shell.html` `[CERT-live]`

**Confirmed on reload — B293-G2 CLOSED.** `Sistema ▸ Históricos (chart)` now delivers
`HistoryChartBuilder` with **no `Px View` tab strip, no view selector, no icon rail**, content spanning the
full frame width, navbar above with `Sistema` underlined. `[CERT-live]`

Two things that capture settles beyond the chrome: `[CERT-live]`

1. **A third native view inherits the menu** — after `AlarmDbView` (§293.11), the chart builder. The
   pattern is not view-specific.
2. **The `|view:` branch is exercised, not just written.** That ord is
   `history:|view:history:HistoryChartBuilder`, which *already* names a view; the resolver correctly did
   NOT append a second one. The untested half of §293.12's conditional is now tested.

Route A is therefore complete for the browser profile: inherited menu, anchored dropdown, shaded parent tab,
native views included, no Niagara chrome. `[CERT-live]`

## 293.x — Connections and open gaps

- **[Block 291]** — the bridge this block builds on: `/file/` vs `/ord/file:`, the CSP rule sheet, the ES5
  dialect, and `target="_top"`. §293.3 is that recipe with one attribute changed.
- **[Block 292]** — the toggle, and the `BButtonGroupBinding` measurement that §293.5 turns into the reason
  Route B cannot express an active tab.
- **[Block 188]** — `BPxInclude` as a `BWidget`; the guide of §293.2 is the officially blessed use of it.
- **[Block 189]** — the applied menu; Route B here is its inheritance story.
- **[Block 183]** — gx serialization; §293.6 adds the `BBorder` parse-side ordering that B183 did not cover.

### Open gaps

| ID | Gap | Class |
|---|---|---|
| ~~**B293-G1**~~ | **CLOSED §293.10-§293.11.** Rendered under both profiles. Workbench: navbar displays, links needed the raw-ord fix. Browser: full end-to-end pass. No CSP/frame conflict observed. | **closed** |
| ~~**B293-G2**~~ | **CLOSED §293.12.** `fullScreen=true` as a view parameter removes the tab strip, view selector and icon rail. Verified on reload against `HistoryChartBuilder`. | **closed** |
| **B293-G3** | The guide names **Action Button** from `kitPx` for the menu entries, while the station's `navbar.px` uses `ImageButton` + `ActionBinding` + `MouseOverBinding`. Are these the same widget under two names, or did the site diverge from the documented recipe? Not resolved here. | STATIC |
| **B293-G4** | §293.8 counted ~250 module-shipped `.px` but opened only 3. The OEM graphics libraries (`honeywellVenomGraphics` 60, `honeywellSpyderTool` 48, `ascCommon` 40, `honeywellAXPlatinum*` 40) are an unread corpus of *working, shipped* PX authored by the vendor — the highest-fidelity style guide available for this site, and never surveyed. | STATIC |
| ~~**B293-G6**~~ | **CLOSED §293.11 for the browser profile**: the click navigates the iframe; frame isolation holds; the menu survives a native `AlarmDbView`. The Workbench half is re-opened as **B293-G7**. | **closed (browser)** |
| **B293-G7** | Same click test under **Workbench**, where `BWebBrowserView:540` intercepts links at widget level. Does the shell's frame isolation survive there, or is Route A browser-only? | DYNAMIC |
| **B293-G5** | `BRadioButtonGroupBinding.groupScope` defaults to `BToggleButtonGroupScopeEnum.universal` (§293.5). What are the other scope values, and does a non-universal scope confine grouping to a `PxInclude` subtree? Relevant because it decides whether one included navbar can be grouped independently per host page. | STATIC |
