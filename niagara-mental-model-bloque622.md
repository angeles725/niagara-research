# Block 622 — ports (PO-G4): BACnet/SC hub `/hub` shares :443 but is gated by a dedicated Niagara auth scheme, not TLS-cert bypass

> **What**: The listening surface and admission model of the BACnet/SC HubFunction. Answer: the hub is NOT a
> separate port — it mounts as a WebSocket servlet at `/hub` on the `BWebService` HTTPS port (:443). Its
> admission is NOT a TLS-cert-only bypass of the Niagara session (the sweep's open hypothesis): the servlet
> is a `SecurityCheckServlet` that REQUIRES an authenticated Niagara `BUser` whose authenticator is a
> `BBacnetScAuthenticator` bound to this specific SC link layer — otherwise `401`. It is an SC-peer IDENTITY
> gate (a dedicated `BBacnetScAuthenticationScheme`), not a Niagara RBAC-role gate, and its reach is the
> BACnet/SC virtual network, not the station component tree.
> **Scope**: `bacnet.stack.link.sc.BHubFunction` (mount) + `...connection.jetty.BJettyScWebSocketAcceptor`
> (admission). The BACnet/SC wire codec is REMITTANCE to [B280]; `BAuthenticationScheme` SPI to [B510]; the
> `BWebService` HTTPS port to [B29]. **Block type**: EVIDENCE (code seam) + correction of an
> audit-sweep hypothesis.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/bacnet/bacnet-rt/vineflower/com/tridium/bacnet/stack/link/sc/BHubFunction.java`
> - `organized/bacnet/bacnet-rt/vineflower/com/tridium/bacnet/stack/link/sc/connection/jetty/BJettyScWebSocketAcceptor.java`
> **Method**: vineflower, driver-read. Markers: `[CERT]` `file:line`; `[INFER]` = characterization.

---

## 622.1 — Mount: a `/hub` WebSocket servlet on the BWebService HTTPS port `[CERT]`

`BHubFunction` holds `webSocketAcceptor = BJettyScWebSocketAcceptor.make(DEFAULT_SERVLET_NAME)` with
`DEFAULT_SERVLET_NAME = "hub"` `[CERT]` (`BHubFunction.java:58-60,85,88`). `BJettyScWebSocketAcceptor` wraps an
inner `JettyScWebSocketServlet extends WebSocketServlet` `[CERT]`
(`BJettyScWebSocketAcceptor.java:67`) — so the hub is served as a servlet on the station's web server, at
path `/hub` on the HTTPS port. It is NOT a distinct TCP port: it shares `:443` with every other web servlet,
so it is reachable exactly when `BWebService` HTTPS is enabled (`httpsEnabled`, default false — [B29]) and
inherits the same TLS endpoint.

## 622.2 — Admission: authenticated `BUser` + SC authenticator bound to this link layer `[CERT]`

The servlet is a `SecurityCheckServlet`, and its `service()` enforces, in order `[CERT]`
(`BJettyScWebSocketAcceptor.java:77-91`):
1. `if (!connectionAcceptor.canAcceptConnections()) → sendError(410)` (hub not accepting).
2. `BUser user = BUser.getCurrentAuthenticatedUser(); if (user == null) → sendError(401)` — an AUTHENTICATED
   Niagara user is required before the WebSocket upgrade.
3. `BAbstractAuthenticator authenticator = user.getAuthenticator();` the user is admitted ONLY if
   `authenticator instanceof BBacnetScAuthenticator && ((BBacnetScAuthenticator)authenticator)
   .isAssociatedWithLinkLayer(scLinkLayer)` — else `sendError(401)`.
4. Only then `super.service(...)` performs the WebSocket upgrade.

The auth scheme is discovered from the station's `BAuthenticationService`:
`getConfiguredAuthenticationScheme()` returns the first `BBacnetScAuthenticationScheme` child, logging a
warning if none exists `[CERT]` (`:96-111`). So the `/hub` endpoint is wired into the standard
`BAuthenticationScheme` SPI ([B510]) via a BACnet/SC-specific scheme + authenticator.

## 622.3 — Correction of the audit-sweep hypothesis + characterization `[CERT]`/`[INFER]`

The AUDIT-FIRST sweep raised the open question "does SC use only TLS-cert admission at the Jetty layer,
bypassing the Niagara session gate?". **Refuted** `[CERT]` (`:81-91`): the upgrade is gated by
`BUser.getCurrentAuthenticatedUser()` and a scheme-specific authenticator check — it does NOT bypass Niagara
authentication. What it is NOT, though, is a Niagara RBAC-ROLE gate: the check is IDENTITY (is this an
SC-authenticated peer bound to this link layer), not `hasOperatorRead()`/category permissions `[INFER]`.

| Dimension | BACnet/SC hub `/hub` |
|---|---|
| What it is | BACnet/SC routing hub — a WebSocket endpoint for SC nodes to join the virtual SC network |
| Port | NOT separate — servlet at `/hub` on `BWebService` HTTPS (:443); needs `httpsEnabled` |
| Auth gate | Authenticated Niagara `BUser` whose authenticator is a `BBacnetScAuthenticator` for THIS link layer (`BBacnetScAuthenticationScheme` in `BAuthenticationService`); no user → 401; wrong authenticator → 401; hub full/off → 410 |
| Reachability | BACnet/SC virtual-network participation (BVLC-SC forwarding between SC nodes) — the SC data plane, NOT the Niagara station component tree ([B280]) |
| Mitigations | `httpsEnabled` required; per-peer SC authenticator identity; hub `canAcceptConnections` cap |

Operator guidance `[INFER]`: the SC hub is authenticated (unlike Modbus/SNMP-public), but its admission is an
SC-peer identity, not a Niagara role — an admitted peer speaks BACnet/SC on the virtual network, it does not
gain station RBAC. Since it rides `:443`, any firewall rule for the web port also governs the hub; there is no
separate port to filter (the surprise: enabling HTTPS for the web UI also exposes `/hub` if the hub is
configured).

## 622.4 — Connections

- **[B280]** — the BACnet/SC BVLC-SC codec + WebSocket transport (what flows once admitted); B622 adds the
  admission/auth model and the `:443/hub` mount fact B280 left as `[INFER]` ("port from the wss:// URI").
- **[B510]** — the `BAuthenticationScheme` SPI the SC scheme plugs into; **[B29]** — the `BWebService` HTTPS
  port the hub servlet mounts on.
- **[B620]/[B621]** — the auth-FREE ports (Modbus/SNMP-public); the SC hub is the CONTRAST (authenticated).
- Forward: **PO-G8** synthesis.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Hub mounts as `/hub` WebSocket servlet (`DEFAULT_SERVLET_NAME="hub"`) on web HTTPS | `[CERT]` | BHubFunction.java:58-60,85,88 | ✓ read |
| 2 | Servlet is a `SecurityCheckServlet extends WebSocketServlet` | `[CERT]` | BJettyScWebSocketAcceptor.java:67 | ✓ read |
| 3 | Admission requires `BUser.getCurrentAuthenticatedUser() != null` else 401 | `[CERT]` | BJettyScWebSocketAcceptor.java:81-83 | ✓ read |
| 4 | User's authenticator must be `BBacnetScAuthenticator` bound to this link layer else 401 | `[CERT]` | BJettyScWebSocketAcceptor.java:85-91 | ✓ read |
| 5 | Auth scheme = `BBacnetScAuthenticationScheme` from `BAuthenticationService` | `[CERT]` | BJettyScWebSocketAcceptor.java:96-105 | ✓ read |
| 6 | Refutes "TLS-cert-only bypass" hypothesis; it IS Niagara-authenticated | `[CERT]` | #3/#4 | ✓ read |
| 7 | Gate is SC-peer IDENTITY, not a Niagara RBAC-role/permission check | `[INFER]` | no hasOperatorRead in the path | ✓ reasoned |
| 8 | Reach = BACnet/SC virtual network, not the station component tree | `[INFER]` | [B280] + the SC role | ✓ reasoned |

**Tally**: `[CERT]` = 6 · `[INFER]` = 2. **Ratio** ≈ 0.33. Block type = EVIDENCE (admission path code-cited;
the role-vs-identity + reach are reasoned). PO-G4 closed.
**Tokens checked**: the servlet `service()` admission chain (:77-91) and `getConfiguredAuthenticationScheme`
(:96-105) read directly; the mount name/acceptor read in BHubFunction. Refutation is a direct code read, not
inference.
