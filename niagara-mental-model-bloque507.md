# Block 507 — `apis` API1: `@NiagaraRpc` — N4's annotation-driven server-side RPC API (`POST /rpc/{method}/{ord}`, JSON in/out, three transports web/box/fox, four-layer auth: session + CSRF + HTTPS + RBAC-default-`Invoke`)

> **Focus:** `apis` (bootstrapped this session; the "every N4 API" request), gap **API1** — the concrete
> "general N4 HTTP API" the AUDIT-FIRST sweep flagged as uncovered. READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/baja/baja/vineflower/javax/baja/rpc/`, `organized/web/web-rt/vineflower/`
> (`NiagaraRpcServlet`, `WEB-INF/web.xml`), `com/tridium/util/NiagaraRpcUtil.java`. FUENTE 1 — [B58] (servlet/
> CSRF infra), [B374] (webChart RPC vector), [B242]/[B493] (Honeywell UX RPC classes). Evidence delegated to a
> `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline against real line numbers.

## §507.1 — The model: a method annotation, not a service `[CERT]`

`javax.baja.rpc.NiagaraRpc` is a runtime-retained **method annotation** — a module marks a public method on a
`BObject` (or a static method on a singleton) as remotely callable; there is no interface to implement and no
naming convention (`NiagaraRpc.java:10`):

```
:11  Transport[] transports();               // required: which channels carry this method
:13  String permissions() default "I";        // RBAC code — default = Invoke
:15  Protected[] protectedTargets() default {}; // extra ORD-scoped permission targets
:17  boolean isSecure() default false;        // require HTTPS
```

Companion types: `Transport` (`{type, facets}`), `TransportType` enum = **`web` / `box` / `fox`**
(`TransportType.java:4-6`), `Protected` (`{ord, permissions}`). The invocation engine is
`com.tridium.util.NiagaraRpcUtil`; there is no `RpcHandler`/`RpcContext` class — dispatch is reflective off the
annotation.

## §507.2 — HTTP transport: `NiagaraRpcServlet` at `/rpc/*` `[CERT]`

`javax.baja.web.servlets.NiagaraRpcServlet`, **POST-only** (`NiagaraRpcServlet.java:31`), content-type
`application/json; charset=utf-8` (`:35`), `no-store`. Two call shapes:

- **Single** — `POST /rpc/{methodName}/{ord}`; path split by `Pattern "/([^/]+)/(.+)"` (`:29`); body = a JSON
  array of arguments (`:50`); dispatch `NiagaraRpcUtil.rpc(TransportType.web, req.isSecure(), remoteAddr, ord,
  methodName, arguments, cx)` (`:55`); response `{"value": <result | null>}` (`:57`).
- **Batch** — `POST /rpc` with body `[{ord, methodName, args}, …]` → JSON array of results in call order.

`[INFER]`: the RPC address is `{method, ORD}` — the ORD names the target BObject in the station tree, the method
name selects the annotated method on it. This is a **method-invocation** API, complementary to oBIX's
slot-value read/write.

## §507.3 — Marshalling: JSON + an implicit `Context` tail arg `[CERT]`

Pure `com.tridium.json` (not BOX, not Java serialization). Args map recursively `JSONArray→List`,
`JSONObject→Map`, primitives pass through; a `SecurableContext` is **appended as the final argument**, so every
`@NiagaraRpc` method must declare `Context` as its last parameter. Returns reverse-map to JSON, wrapped in
`{"value": …}`. A static **legacy whitelist** (`NiagaraRpcUtil`) grandfathers ~10 pre-annotation method pairs
that use the older Baja value-doc codec and are **`fox`-transport-only** — evidence that `@NiagaraRpc` is the
replacement path for a pre-existing RPC mechanism.

## §507.4 — Security: four layers, gated by default `[CERT]`

1. **Session** — the servlet reads the `niagara.context` request attribute set by the upstream web-session
   filter; no authenticated session ⇒ no context ⇒ the call cannot proceed.
2. **CSRF** — `web.xml:45-54` installs `csrfProtectedRpcFilter` (`javax.baja.web.filters.CsrfProtectedFilter`)
   on `url-pattern /rpc/*`; every POST passes the CSRF-token filter first ([B58] server-side guard).
3. **HTTPS** — `NiagaraRpcUtil.java:135-136`: if the method's `isSecure()` and the request is not secure →
   `PermissionException`.
4. **RBAC** — `:140` reads `rpc.permissions()` (default `"I"` = Invoke); `:141` `"unrestricted"` clears the
   requirement; otherwise the target's `BIProtected` security target is checked for the required permission,
   plus any `protectedTargets[]` ORD-scoped checks. `AccessControlException` → HTTP 403.

`[INFER]` **Default posture is CLOSED**: a method is reachable only by an authenticated, CSRF-valid session with
the `Invoke` right on the target; it is open only if it explicitly declares `permissions = "unrestricted"`.
This is a materially stronger default than the plaintext/Basic surfaces catalogued in the `framework-drivers`
focus — the RPC layer is the well-guarded one.

## §507.5 — Who uses it + relation to other APIs `[CERT]`

Real declarations (verified): `com.tridium.bacnetAws.BBacnetAwsNetwork.doSubmitDeviceManagerJob`
(`permissions="i"`, transports `box`+`web`, `BBacnetAwsNetwork.java:99-103`) and
`com.tridium.tagdictionary.neqlize.BNeqlizeRpc` (`permissions="unrestricted"`, `fox`-only,
`BNeqlizeRpc.java:72-74`). It is used **widely across OEM UX modules** — `honIrmConfig` `BPeripheralRPC`
([B493]), `SylkActuatorToolRPC` ([B245]), `honAlarmConsole` `BHonAlarmConsoleRpc` ([B244]),
`honeywellTCThermostatWizard`, `galileo` `BPointListViewRpc`, plus video drivers (maxpro/naxisVideo) — i.e.
`@NiagaraRpc` is the **server-side callable-method registry that bajaux UI views invoke over `/rpc`** (the RPCs
those OEM blocks kept naming). Distinct from: oBIX (slot data, [B499]/API3), BOX (subscription wire, API4),
plain servlets ([B58], the layer below it). `@AgentOn` is a *separate* px/bajaux view-binding annotation — not
this — though the two cooperate (an `@AgentOn` view calls `@NiagaraRpc` methods).

## §507.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | `@NiagaraRpc` = method annotation: transports/permissions(def "I")/protectedTargets/isSecure | `[CERT]` | `NiagaraRpc.java:10-17` | PASS |
| 2 | TransportType = web/box/fox | `[CERT]` | `TransportType.java:4-6` | PASS |
| 3 | NiagaraRpcServlet POST /rpc/{method}/{ord} (+ batch /rpc), JSON args, `{"value":…}` | `[CERT]` | `NiagaraRpcServlet.java:29,31,35,55,57` | PASS |
| 4 | marshalling = com.tridium.json; Context appended last; legacy whitelist fox-only | `[CERT]` | `NiagaraRpcUtil` (json import, legacy block) | PASS |
| 5 | 4-layer auth: session ctx + CSRF filter on /rpc/* + isSecure gate + RBAC default Invoke; unrestricted=open | `[CERT]` | `web.xml:45-54`; `NiagaraRpcUtil.java:135-136,140-141` | PASS |
| 6 | real usage: bacnetAws (web+box,"i"), neqlize (fox,unrestricted), many Honeywell UX RPC classes | `[CERT]` | `BBacnetAwsNetwork.java:99-103`; `BNeqlizeRpc.java:72-74` | PASS |

**Tally:** 6 claims — all `[CERT]` load-bearing + 2 `[INFER]` (address semantics, closed-default posture) on
cited code. Block TYPE = **EVIDENCE**; API1 CLOSED. All load-bearing tokens re-verified inline.

## §507.7 — Connections & focus status

- **This is the RPC surface behind the OEM UX "RPC" classes** the corpus kept naming without a framework block
  ([B245]/[B493]/[B244] etc.) — now the mechanism is documented once, centrally.
- Auth stack reuses [B58] (`CsrfProtectedFilter`, server-side CSRF) and the N4 RBAC permission model; the
  `box`/`fox` transports tie to API4 (BOX wire) and [B134] (Fox wire) respectively.
- Contrasts sharply with the `framework-drivers` security ladder ([B506]): `@NiagaraRpc` is **closed-by-default**
  (session+CSRF+RBAC), where several field-bus drivers were plaintext — the web RPC layer is the hardened one.
- **Focus status:** `apis` 1/8 (API1 closed). NEXT = API2 (ORD-over-HTTP web-tier routing).
