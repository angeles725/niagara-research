# ports — Research State

> Operational state consumed by the loop (Research-SDD). Mirrored in engram
> (`research/niagara/ports/gaps`, `.../progress`). Visible and versionable source.
>
> **Focus angle (§16 / §b2).** A per-port REFERENCE for a Niagara N4 station + platform install: for every
> network LISTENING port — (a) what it is / the service behind it, (b) where it is CONFIGURED (which
> `BService` + property/default), (c) the AUTH/PERMISSION gate on it, and (d) what is REACHABLE / the
> blast-radius through it. Consolidation + gap-fill over a mature corpus: protocol internals are REMITTANCE;
> the NEW work is the per-port auth-gate + reachability rows the corpus never consolidated, and the listening
> ports it never characterized. READ-ONLY over disk (DISK-FIRST; B398 already holds the live scan). If the
> live install is touched, SECRETS DISCIPLINE applies (cite structure, never secret values).
>
> Backlog from the AUDIT-FIRST coverage sweep 2026-08-29 (sonnet, 65 tool calls) with PRE-DECLARE
> REMITTANCES. Next block B620.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 618
gaps_closed: 3
known_gaps: 7
investigable_open: 4
requires_execution_open: 1
blocked_open: 1
deferred_open: 2
undocumented_findings: 0
block_scope: shared-global
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks**: 0 in this focus (corpus-wide count synced by the tool; global prefix `niagara-mental-model-bloque`)
- **Coverage metric**: 3 / 7 closed
- **Last iteration**: 2026-08-29 — PO-G4 closed (B622: BACnet/SC /hub on :443 is Niagara-authenticated, not TLS-cert bypass)

## Remittances (protocol internals already covered — cite, do NOT re-derive)

- Live port scan of the running supervisor — [B398] (:443/:5011 TLS cert ForRecoveryPurposes · :3011/:5011 platform daemon · :1911 Fox · :80 HTTP; SEC-04/09/14)
- Fox protocol / SCRAM — [B134] (wire) · [B457]/[B510] (SCRAM) · [B471]/[B606] (live foxs:4911 handshake) · [B513] (Fox client API) · [B515] (API synthesis)
- Web tier :80/:443 — [B29]/[B508] (servlets + routing) · [B507] (/rpc) · [B509] (/obix) · [B512] (/box)
- Platform daemon :3011/:5011 — [B129] (static: plat.exe/SCM, 18 commands, `secure=(port!=3011)`) · [B158] (HTTP-saved wire) · [B460] (live: 403 no-401, TLS1.3, two-credential model) · [B436]
- BACnet — [B133] (BACnet/IP APDU wire) · [B280] (BACnet/SC BVLC-SC codec + WebSocket) · [B275]–[B288] (export family = reach through :47808)
- Drivers with listening ports — [B476] (nSnmp: SNMPv3 USM; B476-G2 agent-side deferred) · [B496]/[B497]/[B498] (OPC-UA stack; :52520 auth+reach) · [B503] (KNXnet/IP :3671 multicast, no KNX Secure) · [B295] (Modbus poll/client)
- Outbound-only (not listeners) — [B504] (MQTT :1883 client) · [B396] (syslog offload)
- TLS/truststore posture — [B397]/[B156]

## Gap-backlog

<!-- Priority: high | medium | low | deferred. Status leading token: pending | requires-execution |
     blocked-on-<reason> | ✅ | ~~. Sources confirmed by the AUDIT-FIRST sweep (file:line). -->

| Priority | Gap | Artifact type / source | Status |
|---|---|---|---|
| high | PO-G3 — Modbus TCP Slave :502 auth-free reach: which function codes accepted, what points are exposed via the register map, and the unauthenticated WRITE blast-radius (`maximumConnections=5`, zero auth) | Java · organized/modbusTcpSlave/modbusTcpSlave-rt/.../BModbusTcpSlaveNetwork.java + comm/ModbusTcpServer.java | ✅ B620 — accept() loop, ZERO auth/no principal; func codes read/write/write-read/file; exposure=register map (server proxyExts), NOT whole station; no Niagara RBAC on write; controls=loopback-bind/maxConn/license/network only |
| high | PO-G2 — SNMP :161 agent auth-gate + reach: `readWriteCommunity="public"` default → unauthenticated write; the full set of exported MIB objects/points writable and any per-export ACL | Java · organized/nSnmp/nSnmp-rt/.../BSnmpNetwork.java + BSnmpAgent.java | ✅ B621 — agent OFF by default; v1/v2c gate=community (default public on read AND write), no source filter default; v3=USM+VACM (B476); reach=BSnmpExportTable, SET no Niagara RBAC; closes B476-G2 |
| high | PO-G4 — BACnet/SC HubFunction on `:443/hub`: admission model of the `/hub` WebSocket upgrade — Niagara session/RBAC vs TLS-cert-only at the Jetty layer (bypassing session); reach on the SC virtual network | Java · organized/bacnet/bacnet-rt/.../stack/link/sc/BHubFunction.java + BJettyScWebSocketAcceptor | ✅ B622 — /hub = WS servlet on :443 (needs httpsEnabled); SecurityCheckServlet requires authenticated BUser w/ BBacnetScAuthenticator bound to link layer (401 else); REFUTES TLS-cert-bypass; SC-peer IDENTITY gate not RBAC-role; reach=SC virtual net |
| medium | PO-G1 — Fox multicast UDP :1911: what each announcement discloses (station name/IP/port/version), whether a passive subnet listener can enumerate all stations, and whether disabling `multicastEnabled` is a hardening step | Java · organized/fox/fox-rt/.../session/Fox.java + MulticastServer.java + sys/BFoxService.java | pending |
| medium | PO-G5 — OPC-UA HTTPS endpoint :52443 (`BHttpsEndpoint`, enabled=true) auth model: does it share the :52520 user-auth (username/cert/anonymous) + Niagara RBAC, and how is it wired into `BOpcUaServer`? (B498 covered only :52520) | Java · organized/opcUaServer/opcUaServer-rt/.../BHttpsEndpoint.java + BOpcUaServer.java | pending |
| medium | PO-G6 — Central port config: `javax.baja.firewall.BServerPort` as the common listening-port type + the `com.tridium.firewall` layer (FirewallRulesPage/ConcurrentFirewallProcessor) — is there a station-wide port enumeration/filter, and is it a discovery surface? | Java · organized/baja/baja/.../javax/baja/firewall/BServerPort.java + com/tridium/firewall/{FirewallRulesPage,ConcurrentFirewallProcessor}.java | pending |
| medium | PO-G7 — Platform daemon :3011/:5011 auth MODEL (code): characterize the credential/admission model statically from `BDaemonSurrogate`/`BDaemonSSLStatus` (consolidating B129/B460); the live wire digest is the requires-execution child below | Java · organized/platDaemon/platDaemon-rt/.../BDaemonSurrogate.java + BDaemonSSLStatus.java | pending |
| deferred | PO-G7w — platform daemon on-the-wire auth handshake/digest (nonce-response? Fox-SCRAM reuse?) — the live frame B129 §129.6 deferred as "N6-wire" | live probe | requires-execution → §19 (not read-only; remittance [B129] N6-wire) |
| deferred | PO-G8 — SYNTHESIS: the master per-port reference table (all ports, covered+new: purpose · service+config · auth gate · reachability) — the focus deliverable, written at STOP | design synthesis over PO-G1..G7 + remittances | pending (parked; never NEXT — §8b) |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 0 | 2026-08-29 | (bootstrap) AUDIT-FIRST sweep + backlog seeded | — | yes · sonnet (coverage sweep) | 7 |
| 1 | 2026-08-29 | PO-G3 Modbus TCP Slave :502 auth-free reach | B620 | inline (constraint: 4-file server read) | 0 |
| 2 | 2026-08-29 | PO-G2 SNMP :161 agent auth-gate + reach | B621 | inline (constraint: 2-file config read) | 0 |
| 3 | 2026-08-29 | PO-G4 BACnet/SC /hub admission model | B622 | inline (constraint: 2-file security seam) | 0 |

## Blocked gaps (each tagged with what it needs)

- PO-G7w platform daemon wire digest — needs: a live platform-daemon session capture (requires-execution) · tried: static code (B129/B460 map the command set + confirm no 401 challenge, but the credential frame is only observable live) → read-only ceiling reached, deferred to §19

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 4   ← the STATIC loop STOPS when this hits 0
- **Open gaps — requires-execution**: 1 (PO-G7w)
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none (focus reuses the existing decompiled corpus; no new census — subject artifacts already extracted under `organized/`)
