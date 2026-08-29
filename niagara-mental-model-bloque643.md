# Niagara N4 — own-modules-audit (OMA5): `mcpbridge` is a Model Context Protocol server exposing the station to AI agents (list/read/set/create/link component tools) via `BMcpServlet` — auth-gated (401 if unauthenticated) but per-tool RBAC granularity is unverified; 95% of its bulk is bundled Gson

**Focus**: own-modules-audit · **Gap**: OMA5 (mcpbridge) · **Session**: 2026-08-29 · **Block**: B643
**Sources** (`[CERT]` direct artifact + decompile): `/mnt/c/…/modules/mcpbridge-rt.jar` (`META-INF/module.xml`, `BMcpServlet.class` via `javap -c`).

**Scope**: identify what `mcpbridge` is and grade it vs [B636], with a FALSIFY-BEFORE-REPORTING pass on its auth gate (it drives an operational security conclusion). Web-tier servlet/auth internals = [B508] (REMIT); reference = [B636].

> **Context (operator, 2026-08-29)**: `mcpbridge` is a purpose-built MCP the shop created (with AI assistance) and **never documented** — this block is its first documentation. The jar is PRESENT in the station's `modules/` (installed/available to load), but the operator believes it is **not currently mounted/active** in the running station. So the §643.2b access-control defect is a **LATENT code-level finding** — it becomes a live exposure only if/when a `BMcpServlet` instance is registered and reachable. Treat it as a "fix before you enable/deploy this" warning, not (necessarily) a currently-open hole. Whether an instance is mounted is a config.bog check (deferred, MCP-G2). **Production scope (operator, 2026-08-29)**: the ONLY operator module in production on a maquila/client station is `chihuahua` ([B636]); `mcpbridge` and every other module audited in this focus live on the DEV supervisor (`DESKTOP-4AAQ77H`) as development/demo/experimental. So this defect is dev-only + latent — its value is "fix before this pattern ever reaches a client station," not a production-incident report.

---

## 643.1 What it is: an MCP tool-server over a Niagara servlet

`[CERT]` `mcpbridge-rt.jar!META-INF/module.xml`:
```xml
<module … moduleName="mcpbridge" runtimeProfile="rt" description="MCP Bridge - Model Context Protocol for Niagara">
  <type class="com.sejofa.mcpbridge.BMcpServlet" name="McpServlet"/>
```
It is a single `BWebServlet` (`BMcpServlet extends javax.baja.web.BWebServlet` `[CERT]` `javap`) that implements the **Model Context Protocol** server side — exposing the live station as an MCP "tool server" so an AI agent can operate it over JSON-RPC/HTTP. The operator's own code is only **10 classes** `[CERT]`:
```
BMcpServlet · ToolDispatcher · util/{JsonParser, OrdResolver}
handlers/{CreateComponent, GetComponentInfo, LinkComponents, ListChildren, ReadProperty, SetProperty}Handler
```
The 6 handlers ARE the MCP tool surface — an agent can: enumerate the tree (`ListChildren`), inspect (`GetComponentInfo`), read (`ReadProperty`), **write (`SetProperty`), create components (`CreateComponent`), and wire them (`LinkComponents`)**. `OrdResolver` maps ORD paths for the MCP layer. This is a full read+WRITE+create+link control surface on the station — the most powerful capability of any module in the fleet.

`[CERT]` — rt-ONLY is correct (headless JSON-RPC service, no UI); no `rc/` assets. **204 of 206 classes are bundled Gson** (`com/google/gson/*`) — the JSON layer; own code is ~5%. Minimal and justified for the capability ([B640] Pattern 5: the size is Gson, not bloat). Signed `NIAGARA4` only (no SEJOFA_C), `vendorVersion 1.0`, `permGroups=3` — the systemic deviations ([B640]).

---

## 643.2 The auth gate — FALSIFIED the "wide open" hypothesis

The operational worry ("an AI-agent write/create surface — is it open?") is testable. `[CERT]` `javap -c BMcpServlet.doPost`:
```
getRemoteUser()  →  if null: setStatus 401 · "WWW-Authenticate" ·
  {"jsonrpc":"2.0","error":{"code":-32001,"message":"Authentication required"},"id":null}  · return
  else: "[MCP Bridge] Authenticated user: " <user>  →  dispatch to ToolDispatcher
```
So **the bridge requires an authenticated station user** — an unauthenticated MCP call gets HTTP 401 + a JSON-RPC "Authentication required" error, before any tool runs. `getRemoteUser()` is the servlet-container principal, which on N4 is the SCRAM/Basic-authenticated `BUser` ([B508]). The bridge is NOT anonymous-open. The "expose the station to AI agents" capability is real but gated behind station login.

## 643.2b MCP-G1 RESOLVED — authorization is BYPASSED (authentication ≠ authorization)

I closed the residual question by decompiling the dispatch path `[CERT]` `javap`:
- **`ToolDispatcher.dispatch(String toolName, JsonObject args)` is `static`** — it receives ONLY the tool name and JSON args. The authenticated user (`getRemoteUser`) checked in the servlet is **NOT passed down**: no `BUser`, no `WebOp`, no `Context`, no `Subject`, no `runAsUser`.
- **`SetPropertyHandler`** resolves the target via `OrdResolver.resolveComponent(String) → BComponent` and writes the property directly. There is **NO `canWrite`, NO `checkPermission`, NO `OrdTarget`, NO category/role check** on the path.

**Finding**: `mcpbridge` enforces AUTHENTICATION only (the 401 gate), NOT AUTHORIZATION. Once ANY authenticated station user passes the 401, the static, userless dispatch runs the tools with no RBAC — so **any authenticated user, regardless of role or category, gets full read + write + create + link over the ENTIRE station tree** via MCP. The N4 RBAC model ([B11]/[B558]: users→roles→permissions→categories, `OrdTarget.canWrite`) is completely bypassed by this module because the component ops never run through the `WebOp`/`runAsUser` context or check permissions. This is a **broken-access-control / privilege-escalation** defect: a read-only operator (or any low-privilege service account) can rewrite or restructure the station through the MCP endpoint. (Sample: `SetPropertyHandler` + `ToolDispatcher` read directly `[CERT]`; the sibling write handlers `CreateComponentHandler`/`LinkComponentsHandler` share the same userless static dispatch, so the same bypass applies `[INFER]`.)

---

## 643.3 Grade + recommendation

- **Capability vs risk**: `mcpbridge` is a legitimately novel, lean integration (10 own classes) — but it is a write/create control plane for the station. It is correctly authentication-gated (401), which refutes the worst case. The open question is authorization granularity (MCP-G1).
- **Recommendations**: (1) close MCP-G1 — confirm the handlers enforce per-user RBAC (run tools as the authenticated `BUser` so `SetProperty`/`CreateComponent` are subject to the same category/permission checks as any WebOp; if they run as a service super-user, ANY authenticated user gets full write — a privilege-escalation to fix). (2) Given `moduleVerificationMode=low` + the AI-write surface, restrict the servlet to a dedicated least-privilege MCP role and log every tool call. (3) Drop `permGroups=3` to the minimum ([B640] P1). (4) Gson is shared with `datacenter-ux` ([B645]) — extract to a `gson-rt` module to de-duplicate ([B640] cross-cutting).
- **Packaging**: otherwise conformant — single type, rt-only, real dependency on `web-rt`.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | description "MCP Bridge - Model Context Protocol for Niagara"; 1 type BMcpServlet extends BWebServlet | [CERT] | module.xml + javap | ✅ unzip+javap verbatim |
| 2 | 6 handlers = List/GetInfo/Read/Set/Create/Link component tools + ToolDispatcher + JsonParser/OrdResolver (10 own) | [CERT] | unzip -l grep sejofa | ✅ unzip |
| 3 | 204/206 classes = bundled Gson; rt-only, no rc/; NIAGARA4 signer, ver 1.0, permGroups 3 | [CERT] | unzip -l + scan | ✅ unzip |
| 4 | doPost requires getRemoteUser≠null else 401 + JSON-RPC "Authentication required"; then dispatches | [CERT] | javap -c BMcpServlet.doPost | ✅ javap verbatim |
| 5 | no per-tool RBAC/permission check visible in the servlet (authorization granularity unverified → MCP-G1) | [CERT]/[INFER] | javap (absence of checkPermission/canWrite) | ✅ javap + grep |

**Tally**: [CERT] ×4 · [INFER] ×1 · direct-artifact+decompile block. Auth gate token-checked in bytecode (401/getRemoteUser verbatim). New child gap MCP-G1 registered (requires handler decompile).

## Connections

- **[B640]** — the size anomaly (Gson) + systemic deviations. **[B508]** — `getRemoteUser`=SCRAM-authenticated BUser (the web-tier auth this rides on). **[B635]** — permGroups soft under GrantAll. **[B398]** — `moduleVerificationMode=low` context. **[B645]** — Gson duplicated with datacenter-ux.
- Forward: OMA8 (risk register: the MCP write surface + MCP-G1); the Gson-dedup recommendation.

## Gaps uncovered

- **MCP-G1** (requires-execution/decompile) — does `ToolDispatcher`/the handlers enforce per-user RBAC (run as authenticated `BUser`) or run with service privilege? Determines whether any authenticated user gets full station write/create. Registered in the backlog.
