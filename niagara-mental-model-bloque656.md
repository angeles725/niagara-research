# Niagara N4 — own-modules-audit (MCP-G2, §12 LIVE): the `mcpbridge` MCP servlet IS mounted at `/mcp` on the live supervisor and dispatches its mutating tool surface (`create_component`, `set_property`, …) to any authenticated user — the B643 authorization-bypass is live-reachable, not latent

**Focus**: own-modules-audit · **Gap**: MCP-G2 (was requires-execution) · **Session**: 2026-08-29 · **Block**: B656 · **Phase**: §12 dynamic (live-install → SECRETS DISCIPLINE).
**Sources** (`[CERT-live]` live station 127.0.0.1, authorized, READ-ONLY probe): SCRAM-SHA-256 login as service account API2 via `sources/probes/B457-n4-login/niagara-n4-client.py`. Static basis = [B643] (BMcpServlet code).

**Scope**: resolve the one deferred requires-execution gap of the mcpbridge finding — is a `BMcpServlet` instance actually MOUNTED and reachable in the running station? SECRETS DISCIPLINE: HTTP codes + tool names cited; no secret values, no mutating tool invoked.

---

## 656.1 The probe (read-only)

`[CERT-live]` — authenticated to `https://127.0.0.1` as API2 (SCRAM-SHA-256, HTTP 200 on `/obix/about`), then probed candidate MCP servlet mounts:
```
GET /mcp          → HTTP 405   (Method Not Allowed — servlet MOUNTED, rejects GET)
GET /mcp/tools    → HTTP 405
GET /McpServlet   → HTTP 404   (not this name)
GET /mcpbridge    → HTTP 404
GET /api/mcp      → HTTP 404
```
A **405 (not 404)** at `/mcp` proves a servlet is mounted there ([B643]: `BMcpServlet.doGet` rejects; MCP is POST/JSON-RPC). Confirmation via a **read-only** JSON-RPC call:
```
POST /mcp  {"jsonrpc":"2.0","method":"tools/list","id":1}   (as API2)
→ HTTP 200
→ {"jsonrpc":"2.0","id":1,"result":{"tools":[
     {"name":"create_component","description":"Creates a new component in Niagara station",
        inputSchema: {parent_ord, type, name}},
     {"name":"set_property","description":"Sets a property value on a Niagara component", …}, …]}}
POST /mcp  {"jsonrpc":"2.0","method":"initialize","id":0}   → HTTP 200
```
The response is unambiguously mcpbridge (`create_component`/`set_property` with Niagara ORD schemas = the `com.sejofa.mcpbridge` `CreateComponentHandler`/`SetPropertyHandler`, [B643]). Only `tools/list`/`initialize` were sent — **no mutating tool was invoked**.

---

## 656.2 What this confirms — the bypass is live-reachable

Combining §656.1 with the static analysis ([B643]):
- The servlet **is mounted** at `/mcp` on the live supervisor (operator believed it was not — it IS).
- It **dispatches to `ToolDispatcher` for an authenticated user** (API2 got HTTP 200 + the tool list). API2 is a service account; the servlet requires only `getRemoteUser() != null` ([B643] §643.2), so ANY authenticated station user reaches the tool surface.
- The exposed tools include the **mutating** `create_component` and `set_property` — and [B643] §643.2b proved `ToolDispatcher.dispatch` is static/userless and `SetPropertyHandler` writes with no `canWrite`/RBAC. So an authenticated `set_property`/`create_component` call would execute with **no per-user authorization** — the RBAC bypass is not theoretical: the endpoint is live and the tools are advertised.

**Severity update to [B643]**: from "latent + dev-only (jar installed, believed unmounted)" → **the servlet is live-reachable on the dev supervisor (127.0.0.1)**. It is still not on the maquila production station (only `chihuahua` is, [B643]), so this is a DEV-SUPERVISOR exposure, not a client-production one — but any account that can authenticate to the dev supervisor (incl. the exposed `API2` service account, whose credentials should be rotated per prior sessions) can read/write/create across that station via `/mcp` with no role check.

---

## 656.3 Recommendation (raised priority)

1. **Immediate**: if the MCP bridge is not actively needed on the supervisor, **remove the `BMcpServlet` instance** (unmount `/mcp`) or disable the module — it is a full read/write/create control plane behind authentication-only.
2. **Before any use**: implement per-user RBAC in `ToolDispatcher`/handlers — pass the authenticated `BUser` and run tool ops as that user (`runAsUser`) so `set_property`/`create_component` are subject to `OPERATOR_WRITE`/category checks (adopt chihuahua's `ChiRbacHelper` pattern, [B648]).
3. **Rotate** the `API2` service-account credentials (exposed in prior sessions) — it currently has full MCP tool access.
4. Restrict `/mcp` to a dedicated least-privilege role and audit-log every tool call.

This closes MCP-G2. The overload-protection fault fix ([B650]/[B655] #1) remains requires-execution — validating it needs a controlled sensor-fault injection (not safe to perform on the live station), so it stays deferred to a test rig.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | authenticated to 127.0.0.1 as API2 (SCRAM-SHA-256), /obix/about HTTP 200 | [CERT-live] | live probe this session | ✅ observed |
| 2 | GET /mcp → 405 (mounted); /McpServlet,/mcpbridge,/api/mcp → 404 | [CERT-live] | live probe | ✅ observed |
| 3 | POST /mcp tools/list (authed) → HTTP 200 + tools[create_component,set_property,…] | [CERT-live] | live probe (read-only) | ✅ observed |
| 4 | tool set = com.sejofa.mcpbridge handlers → unambiguously mcpbridge | [CERT-live]+[CERT] | §1 + [B643] | ✅ cross-ref |
| 5 | dispatch requires only getRemoteUser≠null; no per-tool RBAC → any authed user = full write/create | [CERT] | [B643] §643.2/643.2b | ✅ cross-ref (static, verified B643) |
| 6 | live-reachable on DEV supervisor; not on maquila prod (only chihuahua there) | [CERT-live]/[CERT] | §1 + [B643] operator scope | ✅ |

**Tally**: [CERT-live] ×4 · [CERT] ×2 · §12 live block. All observations are read-only (no mutating tool invoked; SECRETS DISCIPLINE — codes + tool names only). MCP-G2 CLOSED.

## Connections

- **[B643]** — the static authz-bypass; this §12 block upgrades it latent→live-reachable and closes MCP-G2. **[B648]** — chihuahua's correct RBAC (the pattern to adopt). **[B457]** — the SCRAM login tool used. **[B398]** — API2 exposure / rotation theme.
- **own-modules-audit requires-execution now 0** (MCP-G2 closed); the only remaining requires-execution corpus-wide from this session is the chihuahua overload-fault live validation ([B655], needs a test rig).

## Gaps uncovered

- None new. The overload-protection fault fix ([B655] #1) stays requires-execution (needs controlled sensor-fault injection, unsafe on the live station). No further read-only or safe-live work remains.
