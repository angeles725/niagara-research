# Block 291 — The PX↔web bridge without a module: `WebBrowser` vs `WebWidget`, the `/file/` servlet, and the `about:blank` iframe trap

> **DOCUMENT-MODE block (METHODOLOGY §20)** — CAPTURES a working method established against a live station.
> Answers a concrete question: **what JavaScript can be used with `.px` files alone, compiling nothing?**
>
> **Sources**: (a) Tridium code — `com.tridium.workbench.web.browser` (`BWebBrowser`, `BWebWidget`,
> `BrowserUtil`); (b) the LIVE station `PRUEBAS` (OptimizerSupervisor N4.14.0.162, Honeywell) over HTTPS;
> (c) the station's own shipped `.px` files. Probe:
> `sources/probes/live-20260727T012800Z-station-pruebas-filespace-and-obix.txt`.
>
> Markers: `[CERT]` verbatim in local code · `[CERT-live]` empirical against the running station ·
> `[INFER]` derived. **Corpus language: ENGLISH.**
>
> **SECRETS DISCIPLINE (live-install)**: structure only, no credential values.
>
> PX layer (web bridge). Connects [Block 204] (bajaux), [Block 194] (PX media/profiles),
> [Block 289] (the file space these files live in), [Block 181] (`.px` grammar).

---

## 291.1 — The premise: a `.px` has no scripting `[CERT]`

[Block 181] / [Block 22] establish that PX carries no scripting layer — there is no `<script>` in the PX
grammar and no expression language beyond bindings and converters. Everything in this block is therefore
about **hosting** web content from a `.px`, never about scripting the `.px` itself. `[INFER]`

## 291.2 — Two bridges, and only one works without a module `[CERT]`

Both live in `com.tridium.workbench.web.browser`, and the difference is in how each resolves its ord.

**`BWebWidget` — needs a module-registered view.** It reduces its `js` ord to a view id, discarding
everything that is not a `ViewQuery`:

```java
BOrd ord = this.getJs();
return ord.isNull()
   ? ""
   : Arrays.stream(ord.parse())
       .filter(q -> q instanceof ViewQuery)
       .map(q -> Objects.toString(((ViewQuery)q).getViewId(), ""))
       .findFirst().orElse("");
```
`workbench-wb/.../BWebWidget.java:568-571` `[CERT]`

A `file:` query produces no `ViewQuery`, so it is silently dropped. `BWebWidget` is therefore only usable
with a view a module registered — e.g. the station's own `js="view:schedule:WebScheduler"`. `[CERT-live]`

**`BWebBrowser` — takes a free ord, and is a PX widget.**

```java
public final class BWebBrowser extends BWidget {
   public static final Property title = newProperty(0, "", null);
   public static final Property ord = newProperty(0, BOrd.NULL, null);
   public static final Property location = newProperty(259, "", null);
   public static final Property progress = newProperty(259, 0, null);
   public static final Property progressRunning = newProperty(259, false, null);
   public static final Property showProgressIndicator = newProperty(0, false, null);
   public static final Property contextMenuEnabled = newProperty(0, true, null);
```
`workbench-wb/.../BWebBrowser.java:108-115` `[CERT]`

`extends BWidget` is the load-bearing part: it can be placed in a `.px` like any other widget, and its `ord`
is unfiltered. `[CERT]` Confirmed rendering a hand-written page in the live station. `[CERT-live]`

Its companion converts a file ord to a servable URL, and refuses to localize station-home paths:

```java
public static BHttpObject getOrdFilePathAsHttpObject(FilePath path) throws MalformedURLException {
   if (!path.isAuthorityAbsolute() && !path.isStationHomeAbsolute() && !path.isProtectedStationHomeAbsolute()) {
      File file = BFileSystem.INSTANCE.pathToLocalFile(path);
      return new BHttpObject(file.toURI().toURL().toString());
   } else {
      return null;
   }
}
```
`BrowserUtil.java:112-119` `[CERT]` — a `file:^...` path returns `null` here, i.e. it is NOT turned into a
local `file://`; it is served over HTTP by the station, which is the correct behaviour for a remote
station. `[INFER]` `mergeFilePathFragment` (`:121-130`) accepts the `file` and `module` schemes. `[CERT]`

Minimal working host, with the `jxBrowser` import the station's own graphics declare: `[CERT-live]`

```xml
<import>
  <module name="baja"/> <module name="bajaui"/> <module name="gx"/>
  <module name="jxBrowser"/> <module name="workbench"/>
</import>
...
<WebBrowser layout="0.0,0.0,1920.0,56.0" ord="file:^px/webmenu/menu.html" title="" showProgressIndicator="false" contextMenuEnabled="false"/>
```

## 291.3 — The station serves the file space over HTTP — two routes, one of them wrapped `[CERT-live]`

An `.html` placed in the file space is published with no build step at all. But WHICH url is used decides
whether the caller gets the file or a Niagara page around it:

| URL | Result |
|---|---|
| `/ord/file:%5Epx/webmenu/menu.html` | 200 — Niagara's **Hx wrapper** |
| `/file/px/webmenu/menu.html` | 200 — **the raw file** |

Counting the wrapper's own markers (`HTML Viewer`, `hxProfileOnload`, `servletViewWidget`) in each
response: **6 occurrences via `/ord/file:`, 0 via `/file/`.** `[CERT-live]`

The `/ord/file:` wrapper is a full Hx-profile page (`<body class="Zebra" onload='hxProfileOnload();'>`)
containing `<iframe name='servletViewWidget' id='servletViewWidget' src='about:blank' width='100%'
height='100%'>`. The "HTML Viewer" title a user sees comes from this wrapper, not from the authored
document. `[CERT-live]`

Note the path shapes differ: `/ord/file:%5E...` is `^`-anchored (station home, [Block 289] §289.3), while
the `/file/` servlet takes the path **relative to `shared/`** with no anchor — `/file/px/...`, and
`/file/%5Epx/...` returns 404. `[CERT-live]`

## 291.4 — The `about:blank` trap: absolute paths break inside the iframe `[CERT-live]`

The most expensive gotcha of the session, because the evidence is contradictory until the iframe is taken
into account.

A logo referenced as `src="/file/px/webmenu/logo.png"` renders as a broken image in the graphic — while
that exact URL returns `200 image/png` with real PNG bytes when fetched directly. All three of
`/file/px/...`, `/ord/file:%5E...` and `/ord/file:^...` serve the identical 22120-byte PNG. `[CERT-live]`

The resolution: the authored document is injected into an iframe whose document is `about:blank`
(§291.3), so a root-relative URL resolves against `about:blank`, not against the station. Inline CSS keeps
working because it never travels over the network — which is exactly why the page *looks* styled while its
images fail, and why the failure reads as a permissions problem when it is not. `[INFER]`

**The fix the station's own CSP hands you** — captured from the live response headers: `[CERT-live]`

```
default-src 'self' workbench
script-src  'self' workbench 'unsafe-inline' 'unsafe-eval'
style-src   'self' workbench 'unsafe-inline' cdnjs.cloudflare.com fonts.googleapis.com
font-src    'self' workbench fonts.gstatic.com
img-src     'self' workbench data: module: images.unsplash.com
connect-src 'self' workbench ws://localhost:443 wss://localhost:443 unsplash.niagaramodules.com
```

`img-src` allows `data:`, so embedding the image as a data URI removes the resolution problem entirely —
there is no path left to resolve. Measured on the real asset: 442×72 at 128 colours = 7292 B PNG =
9724 base64 chars; the host document went from 4 KB to 13.8 KB. `[CERT-live]`

## 291.5 — What that CSP permits, as a rule sheet `[CERT-live]`

| Capability | Allowed | Why |
|---|---|---|
| Inline `<script>` | **yes** | `script-src ... 'unsafe-inline'` |
| `eval()` | **yes** | `... 'unsafe-eval'` |
| JS from an external CDN | **no** | `script-src` is `'self' workbench` only |
| CSS from cdnjs / Google Fonts | **yes** | listed explicitly in `style-src` |
| Web fonts from gstatic | **yes** | `font-src` |
| Images as `data:` / `module:` | **yes** | `img-src` |
| WebSocket back to the station | **yes** | `connect-src ws(s)://localhost:443` |

The "no external JS" rule explains an artifact in the field: the site's own OEM module ships
`rc/ext/chart.umd.min.js` **inside the jar** rather than linking a CDN. `[CERT-live]` Under this CSP a
CDN `<script>` would simply be blocked. `[INFER]`

**Dialect: ES5.** [Block 204] quotes the framework's own literal warning — *"DO NOT convert to ES6 class -
this will break"* — and widget classes are addressed as RequireJS module-ids resolved lazily through AMD
`require` (B204 §204.5). `[CERT]` (by reference) Authored pages hosted this way should stay on ES5: `var`,
function expressions, no template literals. `[INFER]`

## 291.6 — Navigation back into Niagara from the hosted page `[CERT-live]`

> **⚠ CORRECTED by [Block 293] §293.10 — this rule is BROWSER-PROFILE ONLY.** Inside a `BWebBrowser`
> widget (Workbench) the requirement is the **opposite**: pass the **raw ord** (`file:^px/x.px`), never
> `/ord/<encoded>`. `Href2Ord.isAlreadyOrd` tests the substring before the first colon; for
> `/ord/file:%5E…` that substring is `"/ord/file"`, which is not a registered ord scheme, so the href is
> wrapped in `file:` and merged onto the widget's own ord — producing `file:/ord/file:%255E…` and a 403.
> Verified live. See B293 §293.10 for the mechanism and the dual-host fix.

Links use `/ord/<url-encoded-ord>`, and must carry `target="_top"` to escape the iframe of §291.3:

```html
<a href="/ord/station:%7Cslot:/Drivers/BacnetNetwork%7Cview:workbench:PropertySheet" target="_top">…</a>
<a href="/ord/file:%5Epx/Dashboard/Home.px" target="_top">…</a>
<a href="/ord/history:%7Cview:history:HistoryChartBuilder" target="_top">…</a>
```

`%7C` is `|` and `%5E` is `^`. The same convention appears in a third-party commercial menu module whose
documented config references icons as `/ord/file:%5EdropdownMenu/home.png` — independent corroboration of
the URL shape. `[CERT-doc]` (vendor documentation, wse-ltd.com)

## 291.7 — Scope limit: this is hosting, not protection `[INFER]`

Everything reached this way is plain text in `shared/` and plain text in the client's browser. Hiding the
"HTML Viewer" chrome (§291.3) is cosmetic; it does not restrict access. Anyone with Workbench `Files`
access reads the source, and devtools shows the delivered page regardless. The protective options in this
ecosystem are a signed compiled module, keeping valuable logic server-side in an `-rt` servlet so the
browser receives data rather than method, and licensing — not URL shape.

## 291.8 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| A `.px` has no scripting layer | B181/B22 by reference | `[CERT]` |
| `BWebWidget` keeps only `ViewQuery` from its ord | `BWebWidget.java:568-571` quoted | `[CERT]` |
| ⇒ a `file:` ord cannot drive `BWebWidget` | derived from the filter | `[INFER]` |
| `BWebBrowser extends BWidget` with 7 properties | `BWebBrowser.java:108-115` quoted | `[CERT]` |
| `getOrdFilePathAsHttpObject` returns null for `^` paths | `BrowserUtil.java:112-119` quoted | `[CERT]` |
| `mergeFilePathFragment` accepts `file` and `module` | `BrowserUtil.java:121-130` | `[CERT]` |
| `.html` in the file space is served with no build | `GET /file/px/... → 200 text/html` | `[CERT-live]` |
| `/ord/file:` wraps; `/file/` does not | 6 vs 0 wrapper markers counted | `[CERT-live]` |
| The wrapper uses an `about:blank` iframe | `servletViewWidget` tag quoted | `[CERT-live]` |
| `/file/%5Epx/...` is 404; `/file/px/...` is 200 | four-URL probe | `[CERT-live]` |
| Same PNG served by all three image URLs (22120 B) | probe with byte counts + PNG magic | `[CERT-live]` |
| ⇒ the broken image is iframe base resolution, not permissions | derived from the two above | `[INFER]` |
| The CSP allows inline script, eval, `data:` images | response headers quoted verbatim | `[CERT-live]` |
| The CSP forbids external-CDN JS | `script-src` has no external host | `[CERT-live]` |
| A local OEM module ships its chart lib inside the jar | `rc/ext/chart.umd.min.js` in the module | `[CERT-live]` |
| ⇒ that packaging is what the CSP forces | correlation, not a stated cause | `[INFER]` |
| Dialect is ES5 | B204 quoting the framework comment | `[CERT]` |
| `/ord/<encoded>` + `target="_top"` navigates back | working links in the deployed page | `[CERT-live]` |
| Vendor docs use the same `/ord/file:%5E...` shape | wse-ltd.com published config | `[CERT-doc]` |
| Hosting ≠ protection | reasoning about the medium | `[INFER]` |

Tally: **[CERT] 6 / [CERT-live] 10 / [CERT-doc] 1 / [INFER] 5.**

---

## 291.x — Connections and open gaps

- **[Block 289]** — the file space these documents live in; `^` = `<station>/shared/`. §291.3's `/file/`
  path shape is the servlet-side view of the same mapping.
- **[Block 204]** — bajaux, RequireJS/AMD and the ES5 constraint quoted in §291.5.
- **[Block 194]** — PX media/profiles; the wrapper of §291.3 is the Hx profile rendering a file view.
- **[Block 181]** — the grammar that has no scripting, which is why §291.2 exists at all.
- **[Block 199]** — `webChart` as bajaux over `-rt` servlets: the module-based counterpart to §291.7's
  "keep the logic server-side".

### Open gaps

| ID | Gap | Class |
|---|---|---|
| **B291-G1** | Does `BWebBrowser` accept an absolute HTTP ord (e.g. `ip:host\|http:/file/...`) so the wrapper can be bypassed from inside the widget? A variant was written but not confirmed; `BWebBrowser` delegates navigation to the JxBrowser impl and the decompiled body does not expose the ord→URL step. | DYNAMIC |
| **B291-G2** | Behaviour of a `WebBrowser`-hosting `.px` across profiles: verified rendering under `view:hx:HxPxView`, NOT verified under the Workbench Px View nor the mobile profile. `BWebBrowser` lives in `workbench-wb`, so the cross-profile story is unclear. | DYNAMIC |
| **B291-G3** | Whether an authored page hosted this way can use the bajaux API (subscriptions, `BajaScript`) rather than plain fetch/DOM — i.e. whether the RequireJS context of B204 is reachable from a `/file/`-served document. | STATIC |
