# Block 433 — The Hx framework: BHxView is a servlet view, not a Swing view — buffered HTML, header-keyed events, and poll-based live values

> Research of the **Hx render framework** (`hx-wb`, focus `workbench`, gap WB06) — Niagara's servlet-based HTML
> UI that renders BComponents to a browser without Swing. Scope: the `BHxView` base, the servlet dispatch, the
> HTML writer + page shell, the event/action model, the Hx-vs-Wb media separation, and the state/session model.
> The counterpart of the Swing framework ([Block 427]-[Block 432]). Does NOT cover PX-on-Hx rendering
> ([Block 194] context) — this is the Hx framework itself.
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `hx-wb.jar`
> sha256 `e86c48eeff3acd7f455dfd89d84c535e9f4a67c9de589cdf4f199dbf1b8b85ad`.
>
> Sources: Tridium docSource (`sources/tridium-src/hx-wb/javax/baja/hx/BHxView.java`) + Vineflower impl
> (`sources/decompiled/hx-wb/…/{BHxProfile,HxUtil,BDefaultHxProfile,BHTML5HxProfile}.java`, clean). Method:
> docSource for the view contract, Vineflower for the profile/servlet impl, all lines re-verified live.
> Markers: `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework (the WEB half). Connects [Block 428] (the Swing shell — Hx is its browser twin),
> [Block 194] (Hx as a PX render profile), [Block 432] (`Command` here is a DIFFERENT `Command` — a web event,
> not the Swing undo command).

---

## 433.1 — BHxView extends BServletView, not BWbView `[CERT]`

The load-bearing structural fact: an Hx view is a SERVLET view, not a Swing view. `[CERT]`

```java
public abstract class BHxView
  extends BServletView          // sources/tridium-src/hx-wb/javax/baja/hx/BHxView.java:42–43
```

It is abstract and NOT itself `@AgentOn` — concrete subclasses register their target types via `module.xml`
`<agent><on type="…"/>`. `[CERT]` The render hook is `write(HxOp op)` (`BHxView.java:256`, empty default —
subclasses override to emit HTML). `[CERT]` `[INFER]` so where the Swing framework paints a `BWidget` tree
([Block 427]), the Hx framework writes HTML from a `BServletView` — two entirely separate substrates behind the
same component model.

## 433.2 — The servlet dispatch: the view IS the handler `[CERT]`

There is NO separate `BHxServlet`. Because `BHxView` is a `BServletView`, the Niagara web stack (Jetty +
`WbServlet`) resolves an incoming `/ord?<target-ord>` URL to the target `BComponent`, finds its registered
`BServletView` agent (the concrete `BHxView`), and calls `doGet(WebOp)` (`BHxView.java:81`). `[CERT]` `doGet`
resolves the per-user `BHxProfile` from the session (with the `PREFER_HX_FACETS` facet), creates an `HxOp`, and
delegates to `profile.writeDocument(view, op)`. `[CERT]` `[INFER]` the ORD→view resolution is the SAME agent
registry as the Swing shell ([Block 428] §428.5) — only the base type (`BServletView` vs `BWbView`) and the
filter differ.

## 433.3 — HTML output: buffer first, then wrap the page shell `[CERT]`

Views write HTML fragments through `op.getHtmlWriter()` (a `javax.baja.io.HtmlWriter`) into an `HxDocument`
content buffer — NOT directly to the response. `[CERT]` Only after `view.write(op)` finishes does
`BHxProfile.writeDocument` (`.../BHxProfile.java:225`) wrap the buffer in the page shell: `<!DOCTYPE html>`
(`:252`), `<head>` (CSS/RequireJS/`hx.js`/`NiagaraEnv`/`hxProfileOnload`), and a single `<body>` `<form
method='post' action='/ord?…'>` around the buffered content. `[CERT]` A CSRF token is written as a hidden form
value on every page (`HxUtil.writeFormValue("csrfToken", …)`, `BHxProfile.java:230`) and verified on POST.
`[CERT]` The concrete profile's `doBody` injects the chrome (path bar, outer/inner content divs). `[INFER]`

## 433.4 — Events: server-side handlers, header-keyed dispatch `[CERT]`

Hx has no "op action type." A view registers server-side handlers at render time via `registerEvent(Event)`
(auto-id `eventN`). `[CERT]` The browser fires `hx.fireEvent(path, id)` (JS from `Event.getInvokeCode`) which
POSTs with headers `EVENT_PATH` + `EVENT_ID`. On the station, `BHxView.process(HxOp)` (`BHxView.java:277`)
reads those headers (`:286`), looks up the `Event` in a map (`events.get(eventId)`, `:298`/`:302`), decodes the
form values, and calls `event.handle(op)`. `[CERT]` `Command extends Event` adds a display name plus
`refresh()`/`redirect()` helpers — this is a WEB event, distinct from the Swing `Command`/undo model
([Block 432]). `[INFER]`

## 433.5 — Hx vs Wb media: an agent filter plus a translate() swap `[CERT]`

The same component gets an Hx or a Wb view through TWO mechanisms in `BHxProfile.HxWebEnv`: `[CERT]`
1. **Agent filter** — `HxFilter` keeps only agents whose type `is(hxView)` or `is(servlet)` (or
   `BIFormFactorMax+BIJavaScript`); Swing-only views are excluded from the Hx candidate list. `[CERT]`/`[INFER]`
2. **`translate()` swap** — when a selected agent is a `BWbView`/`BAbstractPxView`, the env looks up that
   type's registered `BHxView` peer in `Sys.getRegistry().getAgents(...)` and returns the Hx agent instead.
   `[CERT]` (e.g. `module.xml` registers `BHxPxWbView` `@on workbench:WbView`.) `[INFER]`

`BHxPxMedia extends BPxMedia` is the Hx counterpart to Wb's PX media, deciding at PX-edit time whether a widget
is "Hx-capable." `[CERT]`/`[INFER]` The profile itself is per-user (`BWebProfileConfig` mixin on `BUser`,
cached in the `HttpSession`). `[CERT]`

## 433.6 — State: stateless per request, live values by POLL `[CERT]`

An Hx view is effectively stateless per request. `[CERT]` Per-request document state (buffer, script queue)
lives in `HxOp`'s `HxDocument` and is discarded after the response; only error messages, theme, `profileConfig`,
and the CSRF token persist in the `HttpSession`. `[CERT]` **Live values are POLL-based, not push**: the client
runtime starts with `hx.started(op.isDynamic(), HxUtil.pollFreq)` (`BHxProfile.java:345`), and `pollFreq`
defaults to **5000 ms** (`Integer.getInteger("hx.poll.freq", 5000)`, `HxUtil.java`). `[CERT]` There is no
`HxSession` or server-push subscription — a view must call `setDynamic()` to opt into the poll cycle. `[INFER]`
This is the sharp contrast with the Swing side, where a `BWidget` is a live `BComponent` with real subscription.

## 433.7 — Legacy status `[CERT]`

Hx is the pre-HTML5 profile. Several `BHxProfile` methods are `@Deprecated` (e.g. web-start address/status bar,
"since 4.13 / removed in 5.0", `BHxProfile.java:210`,`:215`), a `DeprecatedFilter` pushes deprecated Hx views to
the bottom of the agent list (`:591`), and `com.tridium.hx.BHTML5HxProfile` exists as the N4-era successor
alongside the classic `BDefaultHxProfile`. `[CERT]` `[INFER]` Hx is maintained-but-legacy; new UI is HTML5/
bajaux, but the servlet+event+poll skeleton documented here still backs the classic browser views.

## 433.8 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `BHxView extends BServletView` (not BWbView), abstract, render hook `write(HxOp)` | `[CERT]` | `BHxView.java:42`,`:256` |
| 2 | No separate servlet — the view IS the `BServletView` handler; `doGet`→`profile.writeDocument` | `[CERT]` | `BHxView.java:81` |
| 3 | HTML buffered via `op.getHtmlWriter()`, then wrapped `<!DOCTYPE>`/`<head>`/`<form>` + CSRF | `[CERT]` | `BHxProfile.java:225`,`:230`,`:252` |
| 4 | Events = `registerEvent` + header-keyed `EVENT_PATH`/`EVENT_ID` → `process`→`event.handle` | `[CERT]` | `BHxView.java:277`,`:286`,`:298` |
| 5 | Hx-vs-Wb = `HxFilter` agent filter + `translate()` swap to the registered Hx peer | `[CERT]` | `BHxProfile.java` (HxWebEnv) |
| 6 | Stateless per request; live values POLL-based, `hx.poll.freq` default 5000 ms; no push/HxSession | `[CERT]` | `BHxProfile.java:345`; `HxUtil.java` |
| 7 | Hx is legacy: `@Deprecated` methods (4.13/5.0), `BHTML5HxProfile` successor | `[CERT]` | `BHxProfile.java:210`; `BHTML5HxProfile` exists |

**Marker tally**: `[CERT]` ≈ 22 · `[INFER]` 8 ([INFER]/[CERT] ≈ 0.36). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: every structural line re-verified live; no mangling in hx-wb.
Tokens confirmed: `extends BServletView`, `write(HxOp)`, `process`+`EVENT_PATH`, `csrfToken`, `<!DOCTYPE html>`,
`hx.poll.freq`/`5000`, `@Deprecated`, `BHTML5HxProfile`.

## 433.9 — Connections

- **[Block 428]** — the Swing shell's browser twin; both resolve ORD→view via the agent registry, but Hx uses
  `BServletView`+`HxFilter` and writes HTML instead of hosting a `BWbView`.
- **[Block 194]** — Hx as a PX render media; `BHxPxMedia`/`BHxPxWbView` (§433.5) are the bridge this block's
  framework provides.
- **[Block 432]** — the Hx `Command extends Event` is a WEB event handler, NOT the Swing undo `Command`; same
  name, different subsystem — a naming collision worth flagging.
- **[Block 421]** (webEditors) / bajaux — the modern HTML5 successor path; Hx (§433.7) is the legacy one it
  displaces.

<!-- research-block: focus workbench, gap WB06 (Hx render framework) — CLOSED at body grade -->
