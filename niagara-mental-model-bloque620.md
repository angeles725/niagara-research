# Block 620 — ports (PO-G3): Modbus TCP Slave :502 — an unauthenticated read/write surface bounded only by the register map

> **What**: The auth-gate and reachability of the Modbus TCP Slave listening port (:502). Answer: `502/tcp`
> is served by a plain `ServerSocket.accept()` loop with **ZERO authentication and no Niagara principal** —
> any TCP client that reaches it reads and (where mapped writable) writes the points an engineer exported
> into the server register map. The only controls are network-level (loopback bind), a connection cap, and
> the license gate — none of them an auth gate.
> **Scope**: `modbusTcpSlave` (`BModbusTcpSlaveNetwork` + `ModbusTcpServer` + session) and the
> `modbusCore.server` point model (register map). The Modbus CLIENT/poll side is REMITTANCE to [B295]; the
> `BServerPort` firewall type is PO-G6. **Block type**: EVIDENCE (code) + reachability analysis.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/modbusTcpSlave/modbusTcpSlave-rt/vineflower/com/tridium/modbusTcpSlave/BModbusTcpSlaveNetwork.java`
> - `organized/modbusTcpSlave/modbusTcpSlave-rt/vineflower/com/tridium/modbusTcpSlave/comm/ModbusTcpServer.java`
> - `organized/modbusCore/modbusCore-rt/vineflower/com/tridium/modbusCore/server/` (point model + `messages/`)
> **Method**: vineflower. **String-scrubbed caveat (§5)**: `modbusCore.server` classes render the `Context`
> parameter type as the scrubber token `n` and some var names as `ln`; cited by class + structure, never by
> the garbled token. Config/port literals in `modbusTcpSlave` are intact. Markers: `[CERT]` `file:line`;
> `[INFER]` = the security characterization.

---

## 620.1 — Port + config: `:502`, license-gated, no auth property `[CERT]`

`BModbusTcpSlaveNetwork extends BModbusServerNetwork`, `port = new BServerPort(502, IpProtocol.TCP)`
(default), `socketTimeoutInMillis = 30000`, `maximumConnections = 5` (facet 1–100) `[CERT]`
(`BModbusTcpSlaveNetwork.java:22-47`). License gate: `Sys.getLicenseManager().getFeature("tridium",
"modbusTcpSlave")` `[CERT]` (`:96-98`) — the port only opens on a licensed station. There is **no
authentication, user, or credential property anywhere on the service** — the property set is port, timeout,
max-connections, current-connections `[CERT]` (`:23-47`).

## 620.2 — The listener: `accept()` → session, no credential exchange `[CERT]`

`ModbusTcpServer.run()` → `openPort()` + `acceptSessions()`. `openPort()` binds a `ServerSocket` on
`port.getBindingPort()`; if `port.getBindToLoopback()` is set it binds to loopback only, else all interfaces
`[CERT]` (`ModbusTcpServer.java:58-65`). `acceptSessions()` loops `serverSocket.accept()` and, while under
`maximumConnections`, wraps each socket in a `ModbusTcpSlaveSession` and starts it `[CERT]` (`:82-96`).
**No credential, handshake, or auth step exists between `accept()` and an active session** — the TCP
connection IS the authorization. The only admission controls are:
- `getBindToLoopback()` — bind to `127.0.0.1` only (a network-level restriction, not auth) `[CERT]` (`:61`).
- `maximumConnections` (default 5) — a connection cap; excess connections wait, not rejected on identity
  `[CERT]` (`:85,98`; `BModbusTcpSlaveNetwork.changed` trims sessions on change, `:183-192`).

## 620.3 — Reachability: the register map, read and write `[CERT]`/`[INFER]`

The accepted function set is the server message family in `modbusCore.server.messages` `[CERT]` (class
listing): `ModbusServerReadRequest`, `ModbusServerWriteRequest`, `ModbusServerWriteReadRequest`,
`ModbusServerReadFileRequest`, `ModbusServerWriteFileRequest` (+ responses). So the port answers register
READS, register WRITES, combined write-read, and file-record read/write.

What those reach is the **server register map**, not the whole station `[CERT]` (class listing):
`BModbusRegisterRangeTable` / `BModbusRegisterRangeEntry` define the register ranges; each mapped point is a
server proxy extension — `BModbusServerNumericProxyExt`, `BModbusServerBooleanProxyExt`,
`BModbusServerRegisterBitProxyExt`, `BModbusServerStringRecord` — under `BModbusServerPointDeviceExt` /
`BModbusServerPointFolder`. So the exposure is exactly the set of points an engineer EXPORTED into the
register table (each as a server proxyExt), and a write reaches the mapped point through its proxyExt.

**No Niagara RBAC applies to a Modbus write** `[INFER]`: a Modbus session carries no `BUser` (the protocol
has no principal), and the server write path takes a framework `Context` with no authenticated user (the
`modbusCore.server` write handlers render `Context` as the scrubbed `n` token, with no user attribution).
The write is therefore gated ONLY by whether the point was mapped as a WRITABLE server proxyExt, never by a
permission bit or category. This is unlike the web/Fox surfaces ([B611]/[B612]) where every operation runs
as a session user under RBAC. (The exact writable-vs-read-only proxyExt distinction is a natural child gap —
see below.)

## 620.4 — Security characterization (the per-port answer) `[INFER]`

| Dimension | Modbus TCP Slave :502 |
|---|---|
| What it is | Modbus/TCP SERVER — exposes station points as Modbus registers to external Modbus masters |
| Configured | `BModbusTcpSlaveNetwork.port` = `BServerPort(502, TCP)`; license `tridium/modbusTcpSlave` |
| Auth gate | **NONE** — no credential, no Niagara user, no RBAC. TCP reachability = full authorization |
| Reachability | The exported register map (read; write where mapped writable). NOT the whole station — bounded by what an engineer mapped as server proxyExts |
| Mitigations | `getBindToLoopback()` (loopback-only bind), `maximumConnections` cap (DoS bound), license gate, and NETWORK controls (firewall/VLAN/air-gap) — the only real defenses |

Operator guidance `[INFER]`: treat :502 as an OT-network trust boundary — a plaintext, unauthenticated
read/write channel into the mapped points. Only expose it on a trusted, segmented network; prefer
loopback-bind or a firewall rule when the master is co-located; never expose it to a routable/enterprise
network. This is the same posture as BACnet/IP :47808 and KNXnet/IP :3671 (also auth-free by protocol
design), and the inverse of Fox/Web (SCRAM + RBAC).

## 620.5 — Connections

- **[B295]** — the Modbus driver's CLIENT/poll side (the station AS a Modbus master). B620 is the SLAVE/server
  side (the station AS a Modbus device exposing a port) — the inverse role, a different listening surface.
- **[B398]** — SEC-14 flagged plaintext :1911/:80; B620 adds :502 to the auth-free-listener set with the
  reachability detail. **[B133]/[B503]** — BACnet/IP :47808 and KNXnet/IP :3671, the sibling auth-free
  protocol ports.
- **[B611]/[B612]** — the CONTRAST: web/Fox operations run as a session user under RBAC; a Modbus write does
  not (no principal).
- Forward: **PO-G8** synthesis (the master per-port table); a child gap on the writable-vs-read-only server
  proxyExt distinction (what fraction of the map is writable) could deepen this.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Port default `BServerPort(502, TCP)`; props = port/timeout/maxConn/currentConn only | `[CERT]` | BModbusTcpSlaveNetwork.java:22-47 | ✓ read |
| 2 | License-gated `tridium/modbusTcpSlave` | `[CERT]` | BModbusTcpSlaveNetwork.java:96-98 | ✓ read |
| 3 | Listener is `ServerSocket.accept()` → session with NO credential step | `[CERT]` | ModbusTcpServer.java:82-96 | ✓ read |
| 4 | Only network-level controls: loopback bind + maximumConnections cap | `[CERT]` | ModbusTcpServer.java:61-65,85; BModbusTcpSlaveNetwork.java:183-192 | ✓ read |
| 5 | Function set = ReadRequest/WriteRequest/WriteReadRequest/Read+WriteFileRequest | `[CERT]` | modbusCore/server/messages/ (class listing) | ✓ listed |
| 6 | Exposure = the register map (RangeTable + server proxyExts), engineer-defined | `[CERT]` | modbusCore/server/ (class listing) | ✓ listed |
| 7 | No Niagara RBAC/principal on a Modbus write (no BUser in session) | `[INFER]` | absence of user-Context in server write path + Modbus has no auth | ✓ reasoned |
| 8 | Per-port verdict: auth-free read/write, bounded by the map; net controls only | `[INFER]` | synthesis of #1-#7 | ✓ reasoned |

**Tally**: `[CERT]` = 6 · `[INFER]` = 2. **Ratio** ≈ 0.33. Block type = EVIDENCE (the auth-free listener and
map model are code-cited; the RBAC-absence + verdict are reasoned). PO-G3 closed.
**Tokens checked**: port/license/props read in `BModbusTcpSlaveNetwork`; the `accept()` loop + loopback/cap
read in `ModbusTcpServer`; the function-code + point-model class sets enumerated by directory listing. The
`modbusCore.server` scrubbed `Context`→`n` token was NOT cited as identity (§5).
