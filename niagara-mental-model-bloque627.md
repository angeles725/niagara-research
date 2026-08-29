# Block 627 — ports (SYNTHESIS / PO-G8): the master per-port reference for a Niagara N4 install — purpose, auth gate, reachability

> **What**: The focus-closing synthesis — one reference table for every network LISTENING port of a Niagara
> N4 station + platform install: what it is, where it is configured, the auth/permission gate, and what it
> reaches. This is the deliverable the focus was opened to produce. Every row remits to the block that
> established it.
> **Scope**: synthesis only — no new evidence; each fact is `[CERT]` in its cited block. **Block type**:
> SYNTHESIS (a high `[INFER]`/remittance ratio is correct here). **Subject version**: N4.14.0.162.
> **Sources**: the ports focus B620–B626 + the remitted framework/protocol blocks named inline.

---

## 627.1 — Master port table

| Port(s) | Proto | Service / where configured | Auth gate | Reachability (blast-radius) | Default | Block |
|---|---|---|---|---|---|---|
| **1911** | TCP | Fox — `BFoxService.foxPort` | **SCRAM-SHA-256 + RBAC** (session user) | Full station: component r/w, history, alarms, RPC, subscriptions | ON (plaintext) | [B134]/[B471]/[B515] |
| **4911** | TCP | Fox TLS — `BFoxService.foxsPort` | SCRAM + RBAC over TLS | Same as Fox, encrypted | **OFF** (`foxsEnabled=false`) | [B134]/[B457] |
| **1911** | UDP mcast | Fox discovery — `Fox.multicastEnabled` (224.0.1.84 / FF02::137) | **NONE** | Disclosure only: hostname, IPv4/IPv6, platform type. No station name/creds | ON (TTL 4) | [B623] |
| **80** | TCP | HTTP — `BWebService.httpPort` | Per-servlet: SCRAM session + CSRF; `/obix` RBAC-write | All servlets: `/ord` `/file` `/rpc` `/box` `/obix` `/wb` | ON | [B29]/[B508] |
| **443** | TCP | HTTPS — `BWebService.httpsPort` | Same, over TLS; `requireHttpsForPasswords=true` | All servlets + BACnet/SC `/hub` | **OFF** (`httpsEnabled=false`) | [B29]/[B508] |
| **443** `/hub` | WSS | BACnet/SC hub — `BHubFunction` (servlet on HTTPS, NOT a separate port) | **Niagara-authenticated** `BUser` w/ `BBacnetScAuthenticator` for this link layer (401 else) — identity, not RBAC role | BACnet/SC virtual network (BVLC-SC forwarding), NOT the station tree | needs HTTPS on | [B622]/[B280] |
| **3011** | TCP | Platform daemon (plain) — host daemon | **Platform username/password** (platform users ≠ station BUsers), up-front (no 401) | **Full host admin**: station lifecycle, files, install, backup, reboot, IP, license | ON unless `sslOnly` | [B626]/[B129]/[B460] |
| **5011** | TCP | Platform daemon (TLS) — `BDaemonSSLStatus` | Same, over TLS 1.3 | Same — full host admin | ON when SSL enabled | [B626]/[B460] |
| **47808** | UDP | BACnet/IP — `BBacnetIpLinkLayer.udpPort` (`0xBAC0`) | **NONE** (BACnet has no auth) | Exported BACnet objects: ReadProperty (all), WriteProperty (commandable) | ON if driver added | [B133]/[B275]–[B288] |
| **502** | TCP | Modbus TCP Slave — `BModbusTcpSlaveNetwork.port` | **NONE** (no principal) | The exported register map: read + write (where mapped writable). Not the whole station | ON if driver added; license-gated | [B620] |
| **161** / **162** | UDP | SNMP agent — `BSnmpNetwork` | v1/v2c: community (default **`public`** on read AND write, no source filter); v3: USM+VACM | The `BSnmpExportTable` mapped objects; SET writes writable exports; no Niagara RBAC | **OFF** (`snmpReceiveRequests=false`) | [B621]/[B476] |
| **52520** | TCP | OPC-UA server — `BOpcTcpEndpoint` | username→RBAC / cert→no-identity / **anonymous→unconditional** | Exported OPC-UA nodes (read; write to non-export nodes) | ON if server added | [B498] |
| **52443** | — | OPC-UA HTTPS — `BHttpsEndpoint` | **N/A — not a live port** (type defined but UNWIRED) | none | never listens (default) | [B624] |
| **3671** | UDP mcast | KNXnet/IP — `BKnxInstallation` (224.0.23.12) | **NONE** (no KNX Secure) | KNX group telegrams for mapped DPT datapoints; tunneling/routing | ON if driver added | [B503] |
| :1883 / syslog | TCP | MQTT / syslog — OUTBOUND CLIENTS (not listeners) | n/a (client) | n/a | — | [B504]/[B396] |

Cross-cutting plumbing: every listening port is a `BServerPort` (`publicServerPort`/`bindingPort`/
`bindToLoopback`/`adapter`) that programs an on-device firewall rule — `pf` on the QNX JACE, `Null` on a
Windows/Linux supervisor — enumerated in-process via `getRuleList()`; not a remote discovery surface ([B625]).

## 627.2 — Grouped by auth posture `[INFER]`

- **Authenticated + Niagara RBAC** (a station user + permission bits): **Fox :1911/:4911**, **Web servlets
  :80/:443** (`/ord` `/rpc` `/box` `/obix` `/wb`), **OPC-UA :52520** (username mode). These run operations as
  a session `BUser` under categories/permissions ([B611]/[B612]).
- **Authenticated by IDENTITY, not station RBAC role**: **BACnet/SC `/hub`** (an SC-peer identity) and the
  **platform daemon :3011/:5011** (a platform user = host-admin, a SEPARATE user space). A compromise of the
  platform daemon is a compromise of the whole controller, not one station role.
- **Unauthenticated by protocol design** (TCP/UDP reachability = authorization): **Modbus :502**,
  **BACnet/IP :47808**, **KNXnet/IP :3671**, **SNMP v1/v2c with `public`**, and **Fox multicast :1911**
  (disclosure only). Reach is the ENGINEER-MAPPED export set (register map / BACnet objects / MIB / group
  addresses), not the whole station — but with no credential.
- **Off by default** (a safer starting posture): SNMP agent, Fox TLS :4911, Web HTTPS :443. The
  unauthenticated DRIVER ports (:502, :47808, :3671) exist only when that driver/network is added.

## 627.3 — Operator hardening summary `[INFER]`

1. **Platform daemon** is the crown jewel — set `BDaemonSSLStatus=sslOnly` (refuse plaintext :3011), isolate
   it to a management network, strong platform passwords ([B626]).
2. **Enable HTTPS + `httpsOnly`/`foxsOnly`** and replace the default `ForRecoveryPurposes` TLS cert ([B398]
   SEC-04); this also gates BACnet/SC `/hub`.
3. **Unauthenticated driver ports** (:502 / :47808 / :3671) are OT trust boundaries — expose only on a
   segmented/air-gapped network, prefer `bindToLoopback` when the peer is co-located ([B620]/[B625]).
4. **SNMP**: keep disabled; if needed, change both community strings off `public`, add a source allowlist
   (`recognizedSources` + `ignoreRequestsFromUnrecognizedSources`), prefer v3 USM+VACM ([B621]).
5. **Fox multicast**: `niagara.fox.multicastEnabled=false` in a deployment that should not advertise ([B623]).
6. **OPC-UA**: disable anonymous; require username→RBAC or reject; :52443 is not a concern (unwired, [B624]).

## 627.4 — Connections

- Ports focus: [B620] Modbus · [B621] SNMP · [B622] BACnet/SC hub · [B623] Fox multicast · [B624] OPC-UA
  :52443 (unwired) · [B625] BServerPort/firewall · [B626] platform daemon.
- Remittances: [B134]/[B457]/[B471]/[B515] Fox · [B29]/[B508]/[B507]/[B509]/[B512] Web · [B129]/[B158]/[B460]
  platform daemon · [B133]/[B280]/[B275]–[B288] BACnet · [B476]/[B498]/[B503]/[B504]/[B396] drivers ·
  [B398]/[B397] posture · [B11]/[B611]/[B612] RBAC.
- Open (requires-execution): PO-G7w — the platform daemon on-the-wire credential digest ([B129] N6-wire).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Master table maps each listening port → service/auth/reach | `[INFER→CERT via B620-B626 + remittances]` | each row cited | ✓ remittance |
| 2 | Three auth postures: RBAC / identity-only / unauthenticated-by-design | `[INFER]` | grouping of the table | ✓ reasoned |
| 3 | Platform daemon = highest-value (host admin, separate user space) | `[CERT via B626/B129]` | [B626]/[B129] | ✓ remittance |
| 4 | Unauth driver ports reach the engineer-mapped export set, not whole station | `[CERT via B620/B133/B503]` | [B620]/[B133]/[B503] | ✓ remittance |
| 5 | :52443 OPC-UA HTTPS is not a live port | `[CERT via B624]` | [B624] | ✓ remittance |
| 6 | Hardening summary (6 items) | `[INFER]` | synthesis + [B398] | ✓ reasoned |

**Tally**: `[INFER]` = 2 · remittance-backed = 4. Block type = SYNTHESIS — high `[INFER]`/remittance ratio is
correct (every fact is `[CERT]` in its own block). Focus `ports` CLOSED: 7/7 investigable gaps (PO-G1..G7),
PO-G8 synthesis delivered, PO-G7w (live wire digest) remains requires-execution.
**No new primary source** — every row remits to a focus or framework block.
