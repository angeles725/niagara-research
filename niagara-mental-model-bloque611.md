# Block 611 — graphql-admin (GQL-G1): the session-user `Context` seam of a custom `BWebServlet` — where a GraphQL resolver gets its RBAC identity

> **What**: The exact framework seam by which a custom `BWebServlet` (the host for a DIY GraphQL
> endpoint) obtains the *authenticated session user's* `Context`, so every component read/write a resolver
> performs runs AS that user and is RBAC-gated — not as the platform/superuser. This is GQL-G1, the
> load-bearing security invariant of the whole `graphql-admin` focus: get this wrong and the GraphQL layer
> silently bypasses Niagara's authorization model.
> **Scope**: The `javax.baja.web.BWebServlet` / `WebOp` request-handling contract only (mount → authenticate
> → dispatch → the `Context` handed to `service()`). The producer of the context attribute
> (`ContextFilter`, the Jetty filter chain) is REMITTANCE to [B29] §29.4 / [B508]; per-field authz beyond
> the baseline gate is GQL-G3 (`OrdTarget.canRead`); the concrete BQL/`BComponent` call-site that consumes
> this `Context` is GQL-G4. **Block type**: EVIDENCE (code seam) with a short DESIGN mapping — the [INFER]
> rows are the design guidance, expected and healthy for this focus.
> **Subject version**: Niagara N4.14.0.162 (OptimizerSupervisor install). `WebOp` `@since Baja 1.0`;
> `WebOp.isSecure()` `@since Niagara 4.14`.
> **Sources**:
> - `organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/BWebServlet.java` (Tridium original source, real javadoc + string literals — NOT decompiled)
> - `organized/docSource/docSource-doc/extracted/web-rt/javax/baja/web/WebOp.java` (Tridium original source)
> - `organized/baja/baja/vineflower/com/tridium/util/SecurableContext.java` (decompiled; 6-line interface, structure intact)
> - `organized/web/web-rt/vineflower/com/tridium/web/filters/ContextFilter.java` (decompiled — string-scrubbed; cited by class + call structure only, see METHOD)
> **Method**: docSource-first fidelity (§6). Markers: `[CERT]` = verbatim in the cited primary source
> (`file:line`); `[INFER]` = design deduction (not literal in any source). **String-scrubbed caveat (§5)**:
> `ContextFilter` (vineflower) renders its string literals as the scrubber token `n`/`ln`, so the *value*
> of the attribute it sets is NOT citable from that decompiled file. The attribute NAME `"niagara.context"`
> is cited from the CONSUMER side — `BWebServlet.java:174` in docSource, where the real literal survives —
> and `ContextFilter` is cited only for PRODUCER IDENTITY (which class sets a request attribute to the
> `Context`), by class + call structure, never by its scrubbed token.

---

## 611.1 — The mount: `servletName` → a per-URI `HttpServlet`, registered live `[CERT]`

`BWebServlet extends BAbstractService implements BINiagaraWebServlet` and declares one frozen property,
`servletName` (default `""`), whose javadoc states it "is used to register the servlet into the servers URI
namespace … set this property to `foo` to register the servlet to receive requests for anything starting
with `/foo`" `[CERT]` (`BWebServlet.java:37-49`, `getServletName` :82).

Registration is lifecycle-driven, not `web.xml`:
- `fw(int x, …)` dispatches `Fw.STARTED → register()`, `Fw.STOPPED → unregister()`, and `Fw.CHANGED` on the
  `servletName` property → `unregister(); register()` `[CERT]` (`:257-272`).
- `register()` = `((BWebService)Sys.getService(BWebService.TYPE)).getWebServer().register(this)` `[CERT]`
  (`:277-288`); `unregister()` is the mirror (`:293-304`). This corroborates the dynamic-Jetty mount
  mechanism documented in [B29] §29.2 (REMITTANCE — the `BWebServer.register` → `ServletContextHandler`
  side is there).

The HTTP entry point is an inner `private final class Servlet extends HttpServlet` whose `doGet`/`doPost`/
`doPut`/`doDelete` ALL funnel to the private `doService(req, resp)` `[CERT]` (`:323-352`); `getHttpServlet()`
exposes it (`:359-362`). So a subclass never touches the raw `HttpServlet` API — the framework owns it.

**Design map (GQL-G1)**: the GraphQL host is `class BGraphqlServlet extends BWebServlet` with
`servletName = "graphql-admin"`, mounting the resolver at `/graphql-admin/*`. Dropping the component into
the station's `WebService` (or the module's service list) mounts it; removing it unmounts it, live `[INFER]`.

## 611.2 — The authenticated `Context` arrives as a request attribute, pre-gated `[CERT]`

The private `doService()` is where authentication has already happened and the session user's identity is
handed to Niagara code `[CERT]` (`BWebServlet.java:163-198`):

```java
Context cx = (Context)req.getAttribute("niagara.context");        // :174
if (!getPermissions(cx).hasOperatorRead()) {                       // :175
  resp.sendError(HttpServletResponse.SC_FORBIDDEN); return;        // :177
}
OrdTarget target = getNavOrd().resolve(BLocalHost.INSTANCE, cx);   // :181
BWebService service = (BWebService)Sys.getService(BWebService.TYPE);
WebOp op = new NWebOp(target, service, req, resp);                 // :184
op.fw(this);
OrdTarget newTarget = resolve(op);                                 // :187  (default = op.getBaseOrdTarget())
…
req.setAttribute("niagara.op", op);                               // :197
service(op);                                                       // :198
```

Three load-bearing facts:
1. **The session user's `Context` is `req.getAttribute("niagara.context")`** `[CERT]` (`:174`). It is the
   real Tridium literal in docSource, so the attribute name is certain. The PRODUCER is the Jetty filter
   chain: `com.tridium.web.filters.ContextFilter` sets a request attribute to the resolved `Context` before
   the servlet runs `[CERT — producer identity]` (`ContextFilter.java`, the `req.setAttribute(<scrubbed>,
   context)` call; the literal VALUE is string-scrubbed in vineflower, so identity is cited, value is taken
   from the consumer at `:174`). Full filter-chain / SCRAM authentication is REMITTANCE to [B29] §29.4,
   [B508] §508.3, [B457].
2. **A baseline authorization gate fires before any dispatch**: `getPermissions(cx).hasOperatorRead()` —
   no operator-read ⇒ `403`, `service()` never runs `[CERT]` (`:175-179`). `getPermissions(Context)` is the
   per-servlet override point ([B29] §29.5.3 REMITTANCE) — a resolver can tighten the servlet-wide floor
   here, but it is only a FLOOR (operator-read by default), not per-field authz.
3. **The scope target is resolved AS the session user**: `getNavOrd().resolve(BLocalHost.INSTANCE, cx)`
   passes `cx` `[CERT]` (`:181`) — the servlet's own nav-ord resolves under the user's categories, not the
   platform's.

## 611.3 — `WebOp` IS a `Context`: the identity a resolver threads into every operation `[CERT]`

`service(WebOp op)` dispatches by HTTP method to `doGet/doPost/doPut/doDelete(WebOp)` `[CERT]`
(`BWebServlet.java:147-161`); the defaults reply `405`/`400` (`:209-248`), so a resolver overrides the verb
it needs (`doPost(WebOp)` for a GraphQL POST).

The `WebOp op` handed to those methods is itself the session `Context`:
- `public abstract class WebOp extends ExportOp implements SecurableContext` `[CERT]` (`WebOp.java:37-40`).
- `public interface SecurableContext extends javax.baja.sys.Context { boolean isSecure(); }` `[CERT]`
  (`SecurableContext.java`, whole file). Therefore `WebOp` is-a `Context`.
- The op carries the user: `WebOp.getProfileConfig()` calls `getWebEnv().getWebProfileConfig(getUser())`
  `[CERT]` (`WebOp.java:122`) — `getUser()` (inherited from the `Context`/`ExportOp` chain) returns the
  authenticated `BUser`.
- Helpers a handler needs: `getRequest()`/`getResponse()` (`:185-196`), `getPathInfo()` = the URI remainder
  after the servlet name (`:171-180`), `getWriter()`/`getOutputStream()`/`setContentType()` (`:211-281`).

**Design map (GQL-G1) — the invariant** `[INFER]`: inside `doPost(WebOp op)`, the resolver must thread `op`
(or equivalently `op.getRequest().getAttribute("niagara.context")`) as the `Context` argument of EVERY
data operation it performs — `ord.resolve(op)`, `component.set(value, op)`, `bqlOrd.resolve(op)`,
`action.invoke(arg, op)`. Doing so makes every GraphQL field resolve under the caller's roles/categories,
inheriting Niagara RBAC for free. The failure mode is specific and silent: if a resolver instead constructs
a fresh/empty `Context` (or resolves ORDs with no context, which some paths treat as an engine/superuser
context), the baseline `hasOperatorRead()` gate at `:175` still passes for any logged-in user, but the
per-field category/permission checks are then evaluated against the wrong principal — the whole GraphQL
surface silently escalates. Threading `op` is the one non-negotiable rule; the per-field check it enables is
GQL-G3 (`OrdTarget.canRead`).

## 611.4 — What this seam does NOT resolve (hand-off to sibling gaps)

- **Per-field authorization** beyond the servlet-wide `hasOperatorRead()` floor — `OrdTarget.canRead()` /
  `canWrite()` running as `op` → GQL-G3.
- **The concrete read/mutate call-site** (BQL query, slot set, action invoke) + JSON serialization with
  `com.tridium.json.JSONWriter`, all taking `op` as `Context` → GQL-G4.
- **Using `@NiagaraRpc` methods as the mutation back-end** instead of raw `doPost` handlers — does the RPC
  dispatcher inject the same session `Context`? → GQL-G2 ([B507] is the transport REMITTANCE).
- **CSRF** for the mutating POST (`x-niagara-csrfToken` vs the session attribute) — REMITTANCE to [B602],
  [B58] §58.2.

## 611.5 — Connections

- **[B29] §29.2 / §29.4 / §29.5.3** — the `BWebService`/Jetty mount mechanism, session/cookie lifecycle,
  and `getPermissions(Context)` override point. B611 is the *author-facing* consumer view of that machinery:
  where in the request the authenticated `Context` actually lands (`req.getAttribute("niagara.context")`).
- **[B163] §163.2-163.3** — chihuahua `BChiServlet extends BWebServlet` is the real-module precedent for
  everything here (servletName mount, per-handler RBAC gate). B611 supplies the framework `[CERT]` under
  B163's own-source recipe: chihuahua's `checkCanWrite` is the application layer over this `Context` seam.
- **[B508]** — the URL routing table and `/ord` pipeline; the auth negotiation that populates the filter
  chain feeding `ContextFilter`.
- **[B457] / [B510]** — SCRAM login and the `BAuthenticationScheme` SPI that produce the `BUser` principal
  `ContextFilter` wraps into the `Context`.
- **[B374]** — WebChart's rt "runs as the session user" claim (`OrdTarget.canRead()` gate) is the same
  invariant B611 formalizes; GQL-G3 will open `OrdTarget` directly.
- Forward: **GQL-G2** (@NiagaraRpc Context), **GQL-G3** (`OrdTarget.canRead`), **GQL-G4** (call-site + JSON).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `servletName` property → `/<name>` URI prefix; default `""` | `[CERT]` | BWebServlet.java:37-49,82 | ✓ token |
| 2 | Registration is lifecycle-driven (`Fw.STARTED/STOPPED/CHANGED` → register/unregister) | `[CERT]` | BWebServlet.java:257-272,277-304 | ✓ token |
| 3 | Inner `Servlet extends HttpServlet`; all verbs → `doService(req,resp)` | `[CERT]` | BWebServlet.java:323-352,359 | ✓ token |
| 4 | Session-user `Context` = `req.getAttribute("niagara.context")` | `[CERT]` | BWebServlet.java:174 | ✓ token (real literal, docSource) |
| 5 | `ContextFilter` is the producer that sets a request attr to the `Context` | `[CERT — producer identity]` | ContextFilter.java (setAttribute call; value string-scrubbed) | ✓ class+structure |
| 6 | Baseline gate `getPermissions(cx).hasOperatorRead()` → 403 before dispatch | `[CERT]` | BWebServlet.java:175-179 | ✓ token |
| 7 | Scope target resolved as the user: `getNavOrd().resolve(BLocalHost.INSTANCE, cx)` | `[CERT]` | BWebServlet.java:181 | ✓ token |
| 8 | `service(WebOp)` dispatches by HTTP method to `doGet/doPost/doPut/doDelete(WebOp)` | `[CERT]` | BWebServlet.java:147-161 | ✓ token |
| 9 | `WebOp extends ExportOp implements SecurableContext` | `[CERT]` | WebOp.java:37-40 | ✓ token |
| 10 | `SecurableContext extends javax.baja.sys.Context` | `[CERT]` | SecurableContext.java (whole file) | ✓ token |
| 11 | `WebOp.getUser()` yields the authenticated `BUser` (used for profile config) | `[CERT]` | WebOp.java:122 | ✓ token |
| 12 | Resolver must thread `op` as `Context` into every op, else silent RBAC escalation | `[INFER]` | design deduction from #4/#6/#9 | ✓ reasoned |
| 13 | GraphQL host = `BWebServlet` subclass, `servletName="graphql-admin"` at `/graphql-admin/*` | `[INFER]` | design map from #1 | ✓ reasoned |

**Tally**: `[CERT]` = 11 · `[CERT — producer identity]` = 1 (counted as CERT) · `[INFER]` = 2 · `[CERT-doc]`/`[CERT-web]`/`[CERT-a]` = 0.
**[INFER]/[CERT] ratio** ≈ 2/12 ≈ 0.17 — LOW. Block type = EVIDENCE (code seam); the two `[INFER]` rows are
the design mapping, not unresolved evidence. The seam itself is fully code-cited; G1 is closed.
**Tokens checked**: 11 `[CERT]` tokens grep/read-confirmed against the cited `file:line` (docSource literals
for the `"niagara.context"` attribute and the `hasOperatorRead` gate; `SecurableContext` whole-file). The one
string-scrubbed producer (`ContextFilter`) is cited by identity + structure per the §5 caveat, never by its
scrubbed literal.
