# Block 625 — ports (PO-G6): `BServerPort` is the common listening-port type and it programs an on-device firewall (pf) — the central port plumbing

> **What**: The central port-configuration layer behind every listening service. Answer: `javax.baja.firewall
> .BServerPort` is the common type each service uses to declare a port (public port, local bind port,
> protocol, adapter, loopback); it programs a per-port `RedirectRule` into a pluggable firewall processor —
> `PfFirewallProcessor` (the pf packet filter, on embedded/QNX platforms) or `NullFirewallProcessor` (no
> OS-firewall management, e.g. a Windows/Linux supervisor). `BServerPort.getRuleList()` is the in-process
> enumeration of all mapped ports. It is a management/config layer, NOT a remote discovery surface.
> **Scope**: `javax.baja.firewall.BServerPort` + `com.tridium.firewall.{ConcurrentFirewallProcessor,
> FirewallRulesPage}` + the `com.tridium.nre.firewall` processor/rule types. The per-service ports that USE
> `BServerPort` are the other PO blocks ([B620] Modbus, [B623] Fox, [B29] Web). **Block type**: EVIDENCE
> (code) — the shared plumbing that explains the `publicServerPort`/`bindingPort`/loopback fields seen
> elsewhere.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/baja/baja/vineflower/javax/baja/firewall/BServerPort.java`
> - `organized/baja/baja/vineflower/com/tridium/firewall/ConcurrentFirewallProcessor.java`
> **Method**: vineflower, driver-read. Markers: `[CERT]` `file:line`; `[INFER]` = platform-selection reasoning.

---

## 625.1 — `BServerPort`: the common port declaration `[CERT]`

`BServerPort` frozen properties: `publicServerPort`, `localServerPort`, `ipProtocol` (default
`BIpProtocolEnum.tcp`), `adapter` (default `"any"`) `[CERT]` (`BServerPort.java:78-85`). Accessors:
`getPublicServerPort()` (the advertised port), `getBindingPort()` (the actual local bind), `getBindToLoopback()`
`[CERT]` (`:98,188,184`). Every listening service in the corpus declares its port as a `BServerPort` — Fox
(`BServerPort(1911, TCP)`, [B623]), Web (:80/:443, [B29]), Modbus slave (`BServerPort(502, TCP)`, [B620]) —
so this is the single common type behind all of them. The `public`-vs-`binding` split lets a service advertise
one port while binding another (a redirect).

## 625.2 — It programs an on-device firewall REDIRECT rule per port `[CERT]`

`updateFirewallRules()` builds a `RedirectRule` (public port → local port, protocol, adapter, loopback flag)
and installs it via `fw().addRule(rule)` `[CERT]` (`BServerPort.java:193-214`); `removeFirewallRules()`
(`:225`) tears it down. So opening a `BServerPort` (getBindingPort/setBindToLoopback etc.) triggers a
firewall-rule update `[CERT]` (`:156,162,176,189`). The static `BServerPort.getRuleList()` returns the
aggregate `FirewallRule[]` `[CERT]` (`:238`) — the in-process enumeration of every mapped/redirected port.

The processor is PLUGGABLE — `BServerPort` imports `PfFirewallProcessor` (the OpenBSD/QNX `pf` packet
filter), `NullFirewallProcessor` (a no-op), and `ConcurrentFirewallProcessor` (serializes rule application
via a `Timer`, `ConcurrentFirewallProcessor.java:10,22`) `[CERT]` (`BServerPort.java:3-11`). `[INFER]`: on an
embedded controller with `pf` (the QNX JACE, [B459]) the rules are pushed to the OS packet filter — N4
manages the device firewall per port; on a Windows/Linux supervisor ([B398]) the `NullFirewallProcessor`
applies — N4 does NOT manage the host firewall there (the OS/administrator does).

## 625.3 — Not a remote discovery surface `[CERT]`/`[INFER]`

The port/rule enumeration (`getRuleList()`) is an in-process Java API, and `FirewallRulesPage` is a Workbench
UI page (authenticated, behind the web/Fox auth like any WB view) `[CERT]` (imports; `BServerPort.java:4`).
There is NO unauthenticated remote endpoint that dumps the port table `[INFER]` — the central enumeration is
a management/config concern, reachable only through an authenticated station session. So PO-G6's "is it a
discovery surface?" resolves to NO: the discovery risk is the per-protocol multicast ([B623]), not this
firewall/port layer.

| Dimension | `BServerPort` / firewall layer |
|---|---|
| What it is | The common listening-port declaration + on-device firewall (pf) rule programmer |
| Central config | Yes — every service's port is a `BServerPort`; `getRuleList()` enumerates all mapped ports in-process |
| Firewall filter | pf on embedded/QNX (rules pushed to the OS packet filter); Null on Windows/Linux supervisor (N4 does not manage the host firewall) |
| Remote discovery | NO — enumeration is a Java API + authenticated Workbench `FirewallRulesPage`, not a remote endpoint |
| Relevance | Explains `publicServerPort` vs `bindingPort` + `bindToLoopback` (the loopback-bind hardening) seen per-port |

## 625.4 — Connections

- **[B620]/[B623]/[B29]** — the services that declare their ports as `BServerPort` (Modbus/Fox/Web); B625 is
  the shared type + firewall plumbing under them (e.g. the `getBindToLoopback()` [B620] cited is this class).
- **[B459]** — the QNX JACE (where `pf` is the real firewall N4 programs); **[B398]** — the Windows supervisor
  (where the Null processor applies).
- Forward: **PO-G8** synthesis (the master table; `bindToLoopback` is the cross-cutting hardening this layer
  provides).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BServerPort` props: publicServerPort/localServerPort/ipProtocol(tcp)/adapter(any) | `[CERT]` | BServerPort.java:78-85 | ✓ read |
| 2 | Accessors getPublicServerPort/getBindingPort/getBindToLoopback | `[CERT]` | BServerPort.java:98,188,184 | ✓ read |
| 3 | `updateFirewallRules()` installs a `RedirectRule` via `fw().addRule` | `[CERT]` | BServerPort.java:193-214 | ✓ read |
| 4 | `getRuleList()` returns the aggregate FirewallRule[] (central enumeration) | `[CERT]` | BServerPort.java:238 | ✓ read |
| 5 | Pluggable processor: Pf (QNX pf) / Null / Concurrent (Timer-serialized) | `[CERT]` | BServerPort.java:3-11; ConcurrentFirewallProcessor.java:10,22 | ✓ read |
| 6 | pf on embedded/QNX JACE; Null on Windows/Linux supervisor | `[INFER]` | processor set + [B459]/[B398] | ✓ reasoned |
| 7 | Not a remote discovery surface (Java API + authenticated WB page) | `[CERT]`/`[INFER]` | BServerPort.java:4 (FirewallRulesPage) | ✓ reasoned |

**Tally**: `[CERT]` = 5 · `[INFER]` = 2. **Ratio** ≈ 0.3. Block type = EVIDENCE. PO-G6 closed.
**Tokens checked**: the property set, accessors, `updateFirewallRules`/`RedirectRule`/`getRuleList`, and the
processor imports read directly; the platform-selection and no-remote-endpoint claims are reasoned from the
processor set + sibling blocks.
