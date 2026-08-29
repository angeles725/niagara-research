# Block 613 — graphql-admin (GQL-G2): the `@NiagaraRpc` Context-injection contract — using RPC methods as a GraphQL mutation back-end

> **What**: How `NiagaraRpcServlet` injects the authenticated session `Context` into a dispatched
> `@NiagaraRpc` method, and the exact method-signature + permission contract a resolver must follow to use
> `@NiagaraRpc` methods as its mutation back-end. This is the second Context-provenance model of the focus:
> B611 (GQL-G1) showed a raw `BWebServlet` where the `WebOp` IS the Context; here the RPC layer wraps a
> FRESH `SecurableContext` around the request context + the thread-local authenticated user and appends it
> as the method's last argument.
> **Scope**: `javax.baja.web.servlets.NiagaraRpcServlet` (dispatch) → `com.tridium.util.NiagaraRpcUtil.rpc()`
> (injection + gates) → `javax.baja.rpc.NiagaraRpc` (annotation). The `/rpc` URL envelope and the 4-layer
> auth summary are REMITTANCE to [B507]; per-field `canRead/canWrite` once an ORD is resolved is [B612]
> (GQL-G3); the session-Context source (`niagara.context`) is [B611] (GQL-G1).
> **Block type**: EVIDENCE (code seam) + DESIGN mapping (`[INFER]` rows).
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/web/web-rt/vineflower/javax/baja/web/servlets/NiagaraRpcServlet.java` (decompiled; readable)
> - `organized/baja/baja/vineflower/com/tridium/util/NiagaraRpcUtil.java` (decompiled — **class-name token
>   mangled** to `l`; cited by FILE PATH + method structure per §5, never by the garbled type token; method
>   bodies + string literals in the cited lines are intact)
> - `organized/baja/baja/vineflower/javax/baja/rpc/NiagaraRpc.java` (decompiled; annotation, clean)
> - `organized/web/web-rt/vineflower/com/tridium/web/rpc/BFileRpc.java` (decompiled; real example signatures)
> **Method**: vineflower; every load-bearing `file:line` re-grepped against the real file (this block's
> findings were gathered by a delegated `sonnet` sweep and then driver-VERIFIED token-by-token per §11 —
> the sweep's line numbers matched). Markers: `[CERT]` = verbatim `file:line`; `[INFER]` = design deduction.

---

## 613.1 — Dispatch: `/rpc/{method}/{ord}` → `NiagaraRpcUtil.rpc(..., cx)` `[CERT]`

`NiagaraRpcServlet.doPost` reads the SAME session context attribute as a plain servlet ([B611]):
`Context cx = (Context)req.getAttribute("niagara.context")` `[CERT]`
(`NiagaraRpcServlet.java:32`). It regex-splits the path into `methodName` + ORD, parses the request body as
a `JSONArray` of arguments, and calls
`NiagaraRpcUtil.rpc(TransportType.web, req.isSecure(), remoteAddr, ord, methodName, arguments, cx)` `[CERT]`
(`:55`), wrapping the result as `{"value": retVal}` `[CERT]` (`:57`). A batch form (empty path) loops the
same call over an array of `{ord, methodName, args}` and returns a bare `JSONArray` `[CERT]` (`:82`).
The `web` transport is HARD-CODED at `:55/:82` — a `box`/`fox`-only method is unreachable here (§613.4).

## 613.2 — Context injection: a fresh `SecurableContext` appended as the last argument `[CERT]`

Inside `NiagaraRpcUtil.rpc(...)` (`NiagaraRpcUtil.java`, class-name mangled — cited by path+structure):
1. The ORD is resolved AS the caller: `OrdTarget target = ord.relativizeToSession().resolve(BLocalHost.INSTANCE, cx)` `[CERT]` (`:57`).
2. The user is taken from a thread-local: `final BUser user = BUser.getCurrentAuthenticatedUser()` `[CERT]` (`:86`).
3. A fresh anonymous `SecurableContext` is built and APPENDED to the argument list `[CERT]` (`:90-93`),
   carrying facets `{isSecure, remoteAddr, transportType}` `[CERT]` (`:88`); its `getUser()` returns the
   thread-local `user`, its base wraps `cx`.
4. The target method is looked up by name + the runtime arg classes:
   `cls.getMethod(methodName, args.stream().map(NiagaraRpcUtil::convertToArgClass)...)` `[CERT]` (`:125`),
   where `convertToArgClass` maps ANY `Context` instance → `Context.class` `[CERT]` (`:208`). (A legacy
   whitelisted form uses `getMethod(methodName, Object.class, Context.class)` `[CERT]` (`:123`).)
5. Invoke: `method.invoke(isStatic ? null : object, args.toArray())` `[CERT]` (`:183`).

**The precise answer (GQL-G2)**: the method does NOT receive the raw `WebOp`. It receives a purpose-built
`SecurableContext` whose `getUser()` is `BUser.getCurrentAuthenticatedUser()` (the thread-local auth user the
filter chain set) and whose facets are `{isSecure, remoteAddr, transportType}` — NOT the `WebOp`'s facets.
That injected context is what a method body threads into `component.set(val, cx)` / `ord.resolve(host, cx)`
for permission attribution. For the `web` transport the thread-local user equals the `WebOp` user, so the
effective principal is the same as [B611]; the difference is the facets and the object identity.

## 613.3 — The method-signature contract `[CERT]`

The annotation (`NiagaraRpc.java`, verbatim) `[CERT]`:
```java
public @interface NiagaraRpc {
  Transport[]  transports();                 // REQUIRED — transport eligibility
  String       permissions()      default "I";   // "I" = operator-invoke; "unrestricted" = skip object gate
  Protected[]  protectedTargets() default {};    // extra ORD-based permission checks
  boolean      isSecure()         default false; // require a secure transport
}
```

Method rules (from `NiagaraRpcUtil` `:123-125,:183,:206-208` + real examples):
- **`public`** and reflectively resolvable by name.
- **The LAST parameter must be declared `Context`** (exactly — not `SecurableContext`, not `WebOp`): the
  lookup maps the injected arg to `Context.class` `[CERT]` (`:208`); declaring `SecurableContext` yields
  `NoSuchMethodException`.
- Other params are marshalled from JSON: string→`String`, number→`double`, boolean→`boolean`, array→`List`,
  object→`Map`.
- **`static`** when the ORD resolves to a type/`BTypeSpec`; instance methods require a resolved component.
- Return value serialized: `Map`→`JSONObject`, `Collection`→`JSONArray`, primitives/`String` as-is,
  `void`/`null`→null.

Real signatures (verbatim, `BFileRpc.java`) `[CERT]`:
```java
@NiagaraRpc(permissions="unrestricted", transports={@Transport(type=TransportType.box)})
public static String readTextFile(String ord, Context cx) throws Exception          // :52
public static void  writeTextFile(String ord, String contents, Context cx) …        // :109
```
`Context cx` is always the last parameter.

## 613.4 — Permission gates run BEFORE invoke `[CERT]`

`NiagaraRpcUtil.rpc` enforces, in order, before `method.invoke`:
1. **Transport match** — `hasMatchingTransport(rpc, transportType)` iterates `rpc.transports()`; no match ⇒
   `NoSuchMethodException` `[CERT]` (`:131,:196-197`).
2. **TLS** — `if (rpc.isSecure() && !isSecure) throw PermissionException` `[CERT]` (`:135`).
3. **Object permission** — unless `permissions` is `"unrestricted"` (normalized + skipped, `:141`), the
   target's permissions are checked: for a component instance `((BIProtected)object).getPermissions(cx)`
   `[CERT]` (`:147`); else `securityTarget.getPermissions(cx)` (and if `securityTarget == null` it throws)
   `[CERT]` (`:158`). Evaluated against `rpc.permissions()` (default `"I"` = operator-invoke).
4. **`protectedTargets` loop** — each `@Protected(ord=…, permissions=…)` is resolved
   `BOrd.make(...).resolve(BLocalHost.INSTANCE, cx)` and its `getSecurityTarget().getPermissions(cx)` checked
   `[CERT]` (`:165-172`).

These `getPermissions(cx)` calls are the SAME `BIProtected` contract [B612] documents — evaluated for the
injected context's user. So an `@NiagaraRpc` method inherits Niagara RBAC without hand-writing the gate,
EXCEPT when it declares `permissions="unrestricted"` (as `BFileRpc`/`BPasswordRpc` do), which skips gates 3
and 4 and shifts the entire authz burden into the method body's own `securityCheck(cx)`.

## 613.5 — Design map (GQL-G2) `[INFER]`

Two mutation-implementation paths for the GraphQL admin layer, now both fully specified:
- **(A) Dispatch GraphQL mutations to `@NiagaraRpc` methods.** A resolver receives the GraphQL mutation JSON,
  maps it to `(methodName, ord, args)`, and calls the RPC layer. Benefits: the session `Context` is injected,
  the 4 gates run, and the SAME method is reachable over `web`/`box`/`fox` for free ([B507] transport
  REMITTANCE). Constraints: the method must declare `Context` last; `web` transport must be listed; and a
  method marked `permissions="unrestricted"` MUST self-check — a GraphQL bridge that blindly forwards to
  `unrestricted` RPCs inherits their self-check assumption and can bypass RBAC.
- **(B) Hand-roll `doPost(WebOp)` handlers** ([B611]). Full control of the Context (the raw `WebOp`), but the
  resolver owns every gate (`canWrite` per [B612]) and gets only the `web` transport.

Recommended: use (A) for admin actions that already have `@NiagaraRpc` methods (point writes, password/user
ops, file ops), and (B) for bespoke query/mutation shapes. In BOTH, per-field authorization is [B612]'s
`OrdTarget.canRead/canWrite` on the resolved ORD; the RPC gates are target-level, not field-level.

**Security caveat** `[INFER]`: the injected context's principal is `BUser.getCurrentAuthenticatedUser()`
(thread-local), not the ORD-resolved context's — correct for `web`, but a resolver that re-enters the RPC
layer from a background thread (e.g. a `BJob`, [B511]) would see a DIFFERENT or null thread-local user. Admin
mutations dispatched off the request thread must pass an explicit context, not rely on the thread-local.

## 613.6 — Connections

- **[B611] (GQL-G1)** — the other Context-provenance model (raw `WebOp`). B613 is the RPC counterpart:
  same `req.getAttribute("niagara.context")` entry, different delivery (wrapped `SecurableContext`).
- **[B612] (GQL-G3)** — the per-field `canRead/canWrite` gate; B613's gates 3-4 call the same
  `BIProtected.getPermissions(cx)` at target granularity.
- **[B507]** — the `@NiagaraRpc` transport/envelope/4-layer-auth REMITTANCE; B613 is the method-author's
  Context+signature contract under it.
- **[B511]** — `BJob` async: the thread-local caveat in §613.5 points here.
- Forward: **GQL-G4** (the concrete BQL/`BComponent` call-site + JSON, which a `doPost` handler or an
  `@NiagaraRpc` method body both need).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `NiagaraRpcServlet` reads session ctx `req.getAttribute("niagara.context")` | `[CERT]` | NiagaraRpcServlet.java:32 | ✓ grep |
| 2 | Dispatch calls `NiagaraRpcUtil.rpc(TransportType.web,…,cx)`; result `{"value":…}` | `[CERT]` | NiagaraRpcServlet.java:55,57,82 | ✓ grep |
| 3 | ORD resolved as caller: `ord.relativizeToSession().resolve(BLocalHost.INSTANCE, cx)` | `[CERT]` | NiagaraRpcUtil.java:57 | ✓ grep |
| 4 | User = `BUser.getCurrentAuthenticatedUser()` (thread-local) | `[CERT]` | NiagaraRpcUtil.java:86 | ✓ grep |
| 5 | Fresh `SecurableContext` appended as last arg; facets {isSecure,remoteAddr,transportType} | `[CERT]` | NiagaraRpcUtil.java:88,90-93 | ✓ grep |
| 6 | Method looked up by name + `convertToArgClass`; any Context→`Context.class` | `[CERT]` | NiagaraRpcUtil.java:125,208 | ✓ grep |
| 7 | Invoke `method.invoke(isStatic?null:object, args.toArray())` | `[CERT]` | NiagaraRpcUtil.java:183 | ✓ grep |
| 8 | Annotation elements: transports (req), permissions default "I", protectedTargets, isSecure | `[CERT]` | NiagaraRpc.java (whole) | ✓ read |
| 9 | Real method signature `readTextFile(String ord, Context cx)` — Context last | `[CERT]` | BFileRpc.java:52,109 | ✓ read |
| 10 | Gates before invoke: transport match, TLS, object `getPermissions(cx)`, protectedTargets | `[CERT]` | NiagaraRpcUtil.java:131,135,147,158,165-172,196 | ✓ grep |
| 11 | `"unrestricted"` skips the object/protectedTargets gates | `[CERT]` | NiagaraRpcUtil.java:141 | ✓ grep |
| 12 | Path (A): dispatch GraphQL mutations to @NiagaraRpc; (B) hand-roll doPost | `[INFER]` | design map from #1-#11 + [B611] | ✓ reasoned |
| 13 | Thread-local principal caveat for off-request-thread dispatch | `[INFER]` | deduction from #4 + [B511] | ✓ reasoned |

**Tally**: `[CERT]` = 11 · `[INFER]` = 2 · others = 0. **[INFER]/[CERT] ratio** ≈ 0.18 — LOW; block type =
EVIDENCE. G2 closed.
**Framework-semantic check (§11)**: the delegated sweep's permission claims (gates 3/4, "unrestricted" skip)
were driver-re-read against `NiagaraRpcUtil.java:135-183` — CONFIRMED, no de-escalation needed; the
class-name mangling was noted and citations anchored to path+line, never the garbled token.
**Tokens checked**: 11 `[CERT]` grep/read-confirmed against the cited lines across 4 files.
