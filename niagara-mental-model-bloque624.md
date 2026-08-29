# Block 624 — ports (PO-G5): the OPC-UA HTTPS endpoint :52443 is a defined-but-UNWIRED type — the server exposes only the TCP binary endpoint :52520

> **What**: Whether the OPC-UA server has a second listening port at :52443 (an HTTPS endpoint), as the
> AUDIT-FIRST sweep hypothesized. Answer: **no active :52443 on a default install.** `BHttpsEndpoint` (its own
> default `port=52443`, `enabled=true`) is a registered `BStruct` TYPE (`module.xml name="HttpsEndpoint"`) but
> it is NOT wired into `BOpcUaServer` — the server's frozen properties expose only `opcTcpEndpoint` (:52520)
> and register only that. The HTTPS endpoint type is vestigial/reserved, not a live second port.
> **Scope**: `BOpcUaServer` endpoint wiring + `BHttpsEndpoint`. The OPC-UA server auth model + :52520
> reach are REMITTANCE to [B498]. **Block type**: EVIDENCE — a proven-absence that corrects an audit-sweep
> hypothesis.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/opcUaServer/opcUaServer-rt/vineflower/com/tridium/opcUaServer/BOpcUaServer.java`
> - `organized/opcUaServer/opcUaServer-rt/vineflower/com/tridium/opcUaServer/BHttpsEndpoint.java` (class-name
>   token mangled to `n`; cited by file path + `module.xml` registered name per §5)
> - `organized/opcUaServer/opcUaServer-rt/vineflower/META-INF/module.xml`
> **Method**: vineflower + module.xml; the wiring absence was verified by a full grep for `BHttpsEndpoint`
> usage across `opcUaServer-rt` (§8 RE-MEASURE A DRAMATIC NEGATIVE — a second measurement before asserting
> absence). Markers: `[CERT]` `file:line`; `[INFER]` = characterization.

---

## 624.1 — The server wires ONE endpoint: `opcTcpEndpoint` :52520 `[CERT]`

`BOpcUaServer`'s frozen properties are `opcTcpEndpoint` (type `BOpcTcpEndpoint`, default `new
BOpcTcpEndpoint()`) and `userAuthenticationMethods` (`BOpcUserAuthenticationMethods.DEFAULT`) `[CERT]`
(`BOpcUaServer.java:107-113,179-180`). There is **no `httpsEndpoint` property** on the server. The server
registration path builds its endpoint URL from `getOpcTcpEndpoint().getPort()` / `getOpcTcpEndpoint()
.getEnabled()` only `[CERT]` (`BOpcUaServer.java:438`); no HTTPS bind appears in the registration.

## 624.2 — `BHttpsEndpoint` exists as a type but is referenced nowhere `[CERT]`

`BHttpsEndpoint extends BStruct` declares `enabled=true`, `port=52443`,
`opcHttpsSecurityPolicies=BOpcHttpsSecurityPolicies.DEFAULT` `[CERT]` (`BHttpsEndpoint.java:13-28`), and is
registered in the module type system as `HttpsEndpoint` `[CERT]` (`module.xml`,
`<type class="com.tridium.opcUaServer.n" name="HttpsEndpoint"/>`). But a full grep for `BHttpsEndpoint` across
`opcUaServer-rt/vineflower` finds it ONLY in its own class file and `module.xml` — **no property, no
`new BHttpsEndpoint()`, no registration, no `:52443` bind anywhere in `BOpcUaServer` or elsewhere in the
module** `[CERT]` (grep result: 0 usages outside the type declaration).

Therefore the type's own `enabled=true`/`port=52443` defaults are MOOT: nothing instantiates it as a server
child, so no HTTPS endpoint is created and **no :52443 socket listens** on a default (or ordinarily
configured) OPC-UA server. The server's only listening port is the TCP binary endpoint :52520.

## 624.3 — Correction + characterization `[CERT]`/`[INFER]`

The AUDIT-FIRST sweep listed :52443 as a second live OPC-UA endpoint ("OPC-UA server has TWO endpoints").
**Refuted by measurement** `[CERT]` (§624.2): the HTTPS endpoint TYPE exists but is unwired — it is not a
port. This is exactly the DISK-FIRST / RE-MEASURE discipline: a defined struct is not a live socket until it
is instantiated and bound.

| Dimension | OPC-UA HTTPS :52443 |
|---|---|
| Status | **Not a live port** — `BHttpsEndpoint` type is defined + registered but unwired into `BOpcUaServer` |
| Live OPC-UA port | Only `:52520` (TCP binary, `BOpcTcpEndpoint`) — auth (username/cert/anonymous) + reach = REMITTANCE [B498] |
| Implication | No separate :52443 to firewall or audit on a default install; the OPC-UA attack surface is :52520 alone |

`[INFER]`: the `BHttpsEndpoint` type is likely vestigial (a legacy/reserved HTTPS-transport option the shipped
`BOpcUaServer` does not expose) — a static-type presence, not a runtime surface. If a future/OEM build wired
it, its auth would presumably reuse the server-wide `userAuthenticationMethods` ([B498]); but that is
hypothetical, not the shipped behavior.

## 624.4 — Connections

- **[B498]** — the OPC-UA server (FD3): :52520 auth model (username→RBAC, anonymous unconditional, cert→no
  Niagara identity) + reach. B624 CORRECTS B498's port coverage by proving the second endpoint is not live
  (B498 characterized only :52520 and did not claim :52443; B624 closes that as proven-absent).
- **[B496]/[B497]** — the OPC-UA stack + client.
- Forward: **PO-G8** synthesis (the master table lists :52520 as the only live OPC-UA port).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BOpcUaServer` frozen endpoint props = `opcTcpEndpoint` + `userAuthenticationMethods` only | `[CERT]` | BOpcUaServer.java:107-113,179-180 | ✓ read |
| 2 | Server registration uses `getOpcTcpEndpoint()` only; no HTTPS bind | `[CERT]` | BOpcUaServer.java:438 | ✓ read |
| 3 | `BHttpsEndpoint` declares enabled=true/port=52443; registered as `HttpsEndpoint` type | `[CERT]` | BHttpsEndpoint.java:13-28; module.xml | ✓ read |
| 4 | `BHttpsEndpoint` has ZERO usages outside its own file + module.xml (unwired) | `[CERT]` | grep opcUaServer-rt/vineflower | ✓ measured |
| 5 | No :52443 listens on a default install (type moot without instantiation) | `[CERT]` | #1+#2+#4 | ✓ reasoned-from-measure |
| 6 | Refutes the sweep's "two endpoints"; only :52520 is live | `[CERT]` | #4 | ✓ measured |
| 7 | `BHttpsEndpoint` likely vestigial/reserved | `[INFER]` | type-present-not-wired | ✓ reasoned |

**Tally**: `[CERT]` = 6 · `[INFER]` = 1. **Ratio** ≈ 0.14. Block type = EVIDENCE (proven-absence via a
second measurement). PO-G5 closed.
**Tokens checked**: server frozen properties + registration line read; `BHttpsEndpoint` defaults + module.xml
type read; the zero-usage absence re-grepped across the whole `-rt` tree (RE-MEASURE A DRAMATIC NEGATIVE).
The scrubbed class-name token (`n`) was cited via the module.xml registered name, not the garbled token (§5).
