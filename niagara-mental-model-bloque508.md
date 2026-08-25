# Block 508 — `apis` API2: the N4 web-tier routing + auth-negotiation surface — the servlet mount table, the ORD→view dispatch pipeline (`/ord/*` → parse → resolve → profile-filter → render), the `WWW-Authenticate: SCRAM` HTTP handshake (N4 "Digest" = SCRAM-SHA-256, not RFC 2617), and the Basic-is-scheme-gated refinement of [B290]

> **Focus:** `apis`, gap **API2** — how an HTTP request URL becomes an N4 component response, plus the HTTP
> auth negotiation. READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/web/web-rt/vineflower/{WEB-INF/web.xml, com/tridium/web/…, javax/baja/web/…}`.
> FUENTE 1 — [B507] (`/rpc` = API1, same table), [B58] (servlet/CSRF infra), [B290] (Basic ignored — REFINED
> here), [B134]/[B457] (SCRAM on Fox / login recipe — this is the HTTP `WWW-Authenticate` side), [B194]/[B433]
> (profiles/hx). Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §508.1 — The servlet mount table (`WEB-INF/web.xml`) `[CERT]`

The master N4 web routing table (verified verbatim):

| URL pattern | Servlet | Note |
|---|---|---|
| `/ord/*` | `OrdServlet` (`:269-273`) | the main ORD→view dispatcher (§508.2) |
| `/file/*` | `FileServlet` (`:94-103`) | station file access, prefix `file:^` |
| `/module/*` | `FileServlet` (`:121-125`) | module jar file access, prefix `module://` |
| `/vfile/*` | `FileServlet` | cached typed-resolver files (hx/bajaux) |
| `/view/all/ord/*` | `ViewAllOrdServlet` (`:189-193`) | all-views listing for an ORD |
| `/wb/*` | `WbServlet` (`:199-203`) | Workbench (px/wb) view service |
| `/rpc/*` | `NiagaraRpcServlet` (`:279`) | the RPC API — [B507] API1 |

Registered programmatically (Jetty, not web.xml): `/login`, `/prelogin`, `/logout` (`UnauthenticatedServlet`
subclasses asserting a `NiagaraBasicPermission("UNAUTHENTICATED_SERVLET")`). `[CERT negative]`: **no `/bql`,
`/obix`, or `/pr` mount in `web-rt`** — those live in other modules (BQL-over-HTTP = API8; oBIX server = API3).

## §508.2 — The ORD→HTTP dispatch pipeline `[CERT]`

`/ord/*` runs a filter pipeline then the servlet renders:
1. **Parse+resolve** — `OrdTargetFilter`: takes `req.getPathInfo()`, strips the leading `/`, appends the query
   string, `BOrd.make(...).normalize().resolve(BLocalHost.INSTANCE, cx)` → an `OrdTarget` stashed as
   `niagara.target`. The `sql:` scheme is hard-blocked. `cx` is the `niagara.context` (the authenticated `BUser`).
2. **WebOp** — `WebOpFilter` builds an `NWebOp(target, service, req, resp)`; empty path → redirect to the user's
   home page.
3. **View selection** — `ViewFilter`: `env.getViews(op)` → filter by permission + **profile** + `PxViewFilter`;
   an explicit `?viewid=` overrides, else `env.getDefaultView(op, views)`.
4. **Render** — `OrdServlet.doService`: a `BServletView` (hx/bajaux) → `view.service(op)`; a `BExporter` on GET →
   `exporter.export(op)`; a px/wb view → `WbServlet.serviceView(viewInfo, op)`; else 404.

`[INFER]`: the N4 web API is **ORD-addressed and view-negotiated** — the URL names a component (ORD), and the
response is whichever *view* of it the user's profile admits; there is no fixed per-resource REST schema.

## §508.3 — `/file`, `/module`, and the traversal/`.bog` guards `[CERT]`

`FileServlet` serves station/module files. `FileOrdTargetFilter` builds the ORD as `prefix + pathInfo` (`file:^`
or `module://`) and **guards path traversal**: `if (!pathInfo.contains("|") && !pathInfo.contains("../"))`
(`FileOrdTargetFilter.java:49`) — blocks ORD-pipe injection and `../`. `FileServlet.canRead` (`:425`,`:441-442`)
additionally refuses directories and any file ending `.bog`/`.bog.gz` — **the station database is never served
over `/file`**. `[CERT negative]`: there is no `/px` mount; px content is served through `/ord` → `WbServlet`.

## §508.4 — Auth negotiation: `WWW-Authenticate: SCRAM` (the "Digest"=SCRAM headline) `[CERT]`

N4's HTTP auth handler `BHttpDigestCallbackHandler` (`@AgentOn baja:DigestAuthenticationScheme`) proves the N4
"Digest" scheme is **SCRAM-SHA-256, not RFC 2617 MD5 Digest** — the Niagara scheme *label* is "Digest" but the
HTTP wire scheme string is `SCRAM`:

```
:37  public static final String SCHEME_NAME = "SCRAM";
:40  hashAlgorithm = NiagaraStationAlgorithmBundle...getMessageDigestAlgorithmName().toUpperCase();  // "SHA-256"
```

Three-leg SCRAM handshake over `Authorization`/`WWW-Authenticate` headers, each a custom `AuthMessage`
(`SCHEME key=value, …`, NOT RFC 2617), status 401 until complete:
- **hello** → `WWW-Authenticate: SCRAM hash=SHA-256` (`:91-93`);
- **client-first** → `new ScramServer(...).createServerFirstMessage(clientFirst)` → `WWW-Authenticate: SCRAM
  hash=SHA-256, data=<server_first>` (`:99-107`);
- **client-final** → `createServerFinalMessage(clientFinal)` → status 200 + `Authentication-Info` (`:117`).

`ScramServer` is `com.tridium.nre.auth.ScramServer`. `[INFER]`: this is the **HTTP-web-tier** face of the same
SCRAM primitive the corpus saw on Fox ([B134]) and in the login recipe ([B457]) — now the `WWW-Authenticate`
negotiation and the wire header format are documented; data is base64url-without-padding.

## §508.5 — Basic is present but scheme-gated (REFINES [B290]) `[CERT]`

`BWebHTTPBasicCallbackHandler` (`@AgentOn baja:HTTPBasicAuthenticationScheme`, `:18-19`) is a **real, functional**
RFC 7617 Basic handler — it decodes `Authorization: Basic` and is `readyForCallback` immediately. `[INFER]`
**§14 REFINE of [B290]:** Basic is NOT hard-disabled in code; whether it runs depends on whether
`baja:HTTPBasicAuthenticationScheme` is enabled in the station's Authentication Service. B290's "Basic ignored by
default" is therefore a **station-config default**, not an in-code rejection — if an operator enables the Basic
scheme, this handler processes Basic credentials. Back-pointer added to [B290].

## §508.6 — Content negotiation: per-account profile, not UA-sniffing `[CERT]`

The render profile is an account setting: `BWebProfileConfig` (`@AgentOn baja:User`) defaults to
`hx:HTML5HxProfile`. `ViewFilter.webOpFilter` filters the candidate views by `WebUtil.ProfileFilter(getWebProfile(op))`
— so the same ORD renders hx vs bajaux vs wb **by the user's configured profile**, not by `User-Agent` sniffing;
a `?viewid=` query overrides. Profile implementations live in the `hx`/`workbench` modules ([B433]/[B194]).

## §508.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | mount table: /ord→OrdServlet, /file+/module→FileServlet, /wb→WbServlet, /view/all/ord, /rpc→NiagaraRpcServlet | `[CERT]` | `web.xml:94,121,189,199,269,279` | PASS |
| 2 | ORD dispatch: OrdTargetFilter parse+resolve → WebOpFilter → ViewFilter (profile) → OrdServlet render; sql blocked | `[CERT]` | OrdTargetFilter/WebOpFilter/ViewFilter/OrdServlet | PASS |
| 3 | /file traversal guard (`|`,`../`) + canRead blocks dirs and .bog/.bog.gz; no /px mount | `[CERT]`/`[CERT neg]` | `FileOrdTargetFilter.java:49`; `FileServlet.java:425,441-442` | PASS |
| 4 | N4 "Digest"=SCRAM-SHA-256; WWW-Authenticate:SCRAM hash+data 3-leg handshake via ScramServer | `[CERT]` | `BHttpDigestCallbackHandler.java:37,40,91-93,99-107,117` | PASS |
| 5 | Basic handler present + functional, scheme-gated (not in-code disabled) — REFINES B290 | `[CERT]`+`[INFER]` | `BWebHTTPBasicCallbackHandler.java:18-19,26,34` | PASS |
| 6 | render profile = per-account BWebProfileConfig (default hx:HTML5HxProfile), viewid= override, no UA-sniff | `[CERT]` | `BWebProfileConfig`; `ViewFilter` | PASS |

**Tally:** 6 claims — all `[CERT]`/`[CERT negative]` load-bearing + 3 `[INFER]` (ORD-addressed API model,
SCRAM-is-same-primitive, B290 refine). Block TYPE = **EVIDENCE**; API2 CLOSED. §14 refine to [B290] with
back-pointer. All load-bearing tokens re-verified inline.

## §508.8 — Connections & focus status

- Same `web.xml` table hosts **API1 `/rpc`** ([B507]); this block is the routing+auth spine the whole web tier
  (RPC, oBIX-server API3, BQL-over-HTTP API8) hangs off.
- **SCRAM:** completes the HTTP `WWW-Authenticate` face of the SCRAM primitive ([B134] Fox / [B457] recipe) — the
  cross-finding that N4 "Digest"≠RFC-7616 (noted in the api-access/video work) now has its server-side handler.
- **§14 REFINE of [B290]:** Basic is config-gated, not in-code-disabled (back-pointer added).
- Profiles tie to [B194]/[B433]; `/file` `.bog`-block + traversal guard feed [B398]/[B490] SEC.
- **Focus status:** `apis` 2/8 (API1–API2 closed). NEXT = API3 (oBIX server-side `obix-rt`).
