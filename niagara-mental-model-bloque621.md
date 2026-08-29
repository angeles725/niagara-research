# Block 621 — ports (PO-G2): SNMP agent :161/:162 — off by default, but community `public` on both read AND write when enabled

> **What**: The auth-gate and reachability of the SNMP agent ports (UDP :161 requests, :162 traps). Answer:
> the agent is DISABLED by default (`snmpReceiveRequests=false`); when enabled it listens on UDP :161 and,
> for SNMP v1/v2c, is gated ONLY by a community string whose default is `public` for BOTH read-only AND
> read-write — and by default there is no source-IP filter. v3 adds USM auth/priv + VACM view-access. A SET
> reaches the engineer-mapped MIB exports with no Niagara principal/RBAC.
> **Scope**: `nSnmp` agent-side (`BSnmpNetwork` config + `BSnmpAgent` exports). The full nSnmp module
> (SNMPv3 USM internals, typed proxy exts, trap reception) is REMITTANCE to [B476]; this block is the
> per-port auth-gate + reachability the corpus never consolidated (the planned B476-G2). **Block type**:
> EVIDENCE (config) + reachability analysis.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/nSnmp/nSnmp-rt/vineflower/com/tridium/nSnmp/BSnmpNetwork.java`
> - `organized/nSnmp/nSnmp-rt/vineflower/com/tridium/nSnmp/BSnmpAgent.java`
> **Method**: vineflower; property defaults read from the `newProperty(...)` initializers (intact literals).
> Markers: `[CERT]` `file:line`; `[INFER]` = the security characterization.

---

## 621.1 — Ports + default posture: OFF by default `[CERT]`

`BSnmpNetwork extends BNNetwork implements BIService` — the SNMP agent/network. Listening is DISABLED by
default: `snmpReceiveRequests = false` `[CERT]` (`BSnmpNetwork.java:147`) and `snmpReceiveTraps = false`
(`:157`). When enabled, `receiveConfig = BSnmpUdpCommConfig(initSnmpPort("req"))` (UDP :161) and
`trapConfig = ...("traps")` (UDP :162) `[CERT]` (`:150,158`). The exposed MIB lives on `localDevice =
BSnmpAgent` (`:164`). Default SNMP version for the network-manager relationship is v1 (`:161`).

## 621.2 — Auth gate: community `public` on read AND write; no source filter by default `[CERT]`

For SNMP v1/v2c the gate is the COMMUNITY STRING, and the defaults are permissive `[CERT]`:
- `readOnlyCommunity = "public"` (`BSnmpNetwork.java:155`)
- `readWriteCommunity = "public"` (`:156`) — the WRITE community also defaults to `public`.
- `checkCommunityOnRequests = true` (`:154`) — the community IS checked, but against the default value
  `public`, a universally-known string. So an enabled agent left at defaults accepts GET and **SET** from
  any client that sends `public`.
- `ignoreRequestsFromUnrecognizedSources = false` (`:152`) with `recognizedSources = new
  BNetworkManagerList()` empty (`:153`) — by default NO source-IP allowlist is enforced.

For SNMP v3 the gate is USM (auth/priv) + VACM view-based access — `BSnmpAgent` imports
`version3.vacm.BVacmContextTable` `[CERT]` (`BSnmpAgent.java:10`); the v3 USM/DES/AES internals are
REMITTANCE to [B476]. v3 is the only mode with a real per-principal auth gate; v1/v2c has only the shared
community secret.

## 621.3 — Reachability: the export table, read and (SET) write, no Niagara RBAC `[CERT]`/`[INFER]`

The agent exposes `exports = BSnmpExportTable` `[CERT]` (`BSnmpAgent.java:39-41,56,62`) — the engineer-mapped
set of MIB objects bound to Niagara points/values. A GET reads them; a SET (allowed when the request's
community matches `readWriteCommunity`) writes the writable exports. As with Modbus ([B620]), an SNMP request
carries **no `BUser`** — v1/v2c authorization is the community string, v3 is USM/VACM, and neither maps to a
Niagara user/permission. So a SET is gated by (community | USM+VACM) and the export's own writable mapping,
NOT by a Niagara RBAC bit `[INFER]`. Reach is bounded by the export table (what was mapped), not the whole
station.

## 621.4 — Security characterization (the per-port answer) `[INFER]`

| Dimension | SNMP agent :161 (UDP req) / :162 (UDP traps) |
|---|---|
| What it is | SNMP agent — exposes station values as MIB objects to SNMP managers; also receives traps → alarms |
| Configured | `BSnmpNetwork` (`Config/Drivers/SnmpNetwork`); `snmpReceiveRequests`/`snmpReceiveTraps`; ports via `BSnmpUdpCommConfig` |
| Default posture | **DISABLED** (`snmpReceiveRequests=false`) — safer default than Modbus |
| Auth gate | v1/v2c: community string, default `public` for READ and WRITE (checked, but the value is public); no source-IP filter by default. v3: USM auth/priv + VACM ([B476]) |
| Reachability | The `BSnmpExportTable` mapped objects (read; SET-write where writable). No Niagara RBAC/principal |
| Mitigations | keep disabled; change both community strings off `public`; enable `ignoreRequestsFromUnrecognizedSources` + populate `recognizedSources` (source allowlist); use v3 (USM+VACM); network segmentation |

Operator guidance `[INFER]`: if the agent must be enabled, treat v1/v2c-with-`public` as no authentication —
change the read-write community at minimum, add a source allowlist, and prefer v3 with authPriv. Unlike
Modbus :502 ([B620]), SNMP DOES offer in-protocol hardening (communities, source allowlist, v3 USM/VACM);
the risk is the permissive DEFAULTS, not a missing mechanism.

## 621.5 — Connections

- **[B476]** — the nSnmp module (SNMPv3 USM internals, typed proxy exts, trap reception). B621 closes the
  planned B476-G2 (the agent-side per-port auth/reach angle) with the community/source defaults.
- **[B620]** — Modbus TCP Slave :502: the sibling auth-free-by-default export port; contrast — Modbus has NO
  in-protocol hardening, SNMP has communities/allowlist/v3.
- **[B398]** — the live-port security posture (B621 adds the SNMP default-community risk to that thread).
- Forward: **PO-G8** synthesis (master per-port table).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Agent disabled by default (`snmpReceiveRequests=false`, `snmpReceiveTraps=false`) | `[CERT]` | BSnmpNetwork.java:147,157 | ✓ read |
| 2 | Ports :161 (req) / :162 (traps) via `BSnmpUdpCommConfig` | `[CERT]` | BSnmpNetwork.java:150,158 | ✓ read |
| 3 | `readOnlyCommunity` AND `readWriteCommunity` default `"public"` | `[CERT]` | BSnmpNetwork.java:155,156 | ✓ read |
| 4 | `checkCommunityOnRequests=true` but against the `public` default | `[CERT]` | BSnmpNetwork.java:154 | ✓ read |
| 5 | No source filter by default (`ignoreRequestsFromUnrecognizedSources=false`, empty list) | `[CERT]` | BSnmpNetwork.java:152,153 | ✓ read |
| 6 | v3 = USM + VACM view-access (`BVacmContextTable`) | `[CERT]` | BSnmpAgent.java:10 | ✓ read |
| 7 | Reach = `BSnmpExportTable` mapped objects; SET writes writable exports | `[CERT]` | BSnmpAgent.java:39-41,56 | ✓ read |
| 8 | No Niagara `BUser`/RBAC on an SNMP SET | `[INFER]` | absence of user-Context + SNMP has no principal | ✓ reasoned |

**Tally**: `[CERT]` = 7 · `[INFER]` = 1. **Ratio** ≈ 0.14 — block type = EVIDENCE (config-cited). PO-G2 closed
(also closes B476-G2's agent-side angle).
**Tokens checked**: all 7 `[CERT]` read against the cited `newProperty`/import lines. No scrubbed literals in
the cited config region.
