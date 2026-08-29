# Block 623 — ports (PO-G1): Fox multicast UDP :1911 — unauthenticated station discovery (host + platform disclosure), on by default

> **What**: The Fox multicast discovery surface. Answer: `BFoxService` joins multicast group
> `224.0.1.84:1911` (IPv4) / `[FF02::137]:1911` (IPv6) by default (`multicastEnabled=true`), and responds to
> an UNAUTHENTICATED "rollcall" discovery with an announcement disclosing its hostname, IPv4/IPv6 address,
> and Niagara platform type. A passive listener that joins the group can enumerate every Niagara host on the
> subnet/VLAN. It does NOT disclose the station name, version, or any credential (those require an
> authenticated Fox HELLO).
> **Scope**: `com.tridium.fox.session.{Fox, MulticastServer}` + the `BFoxService` announcement builder. The
> authenticated Fox protocol (SCRAM login, HELLO, the reach of a real Fox session) is REMITTANCE to
> [B134]/[B471]/[B515]. **Block type**: EVIDENCE (code) + reconnaissance-surface analysis.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/fox/fox-rt/vineflower/com/tridium/fox/session/Fox.java`
> - `organized/fox/fox-rt/vineflower/com/tridium/fox/session/MulticastServer.java`
> - `organized/fox/fox-rt/vineflower/com/tridium/fox/sys/BFoxService.java` (announcement builder — method name
>   scrubbed to `n`; cited by class + structure per §5)
> **Method**: vineflower. Markers: `[CERT]` `file:line`; `[INFER]` = the reconnaissance characterization.

---

## 623.1 — Multicast group + default-on `[CERT]`

`Fox` constants: `MULTICAST_ADDRESS = "224.0.1.84"`, `IPV6_MULTICAST_ADDRESS = "FF02::137"`,
`MULTICAST_PORT = 1911`, `multicastEnabled = true` (default), `multicastTimeToLive = 4` `[CERT]`
(`Fox.java:20-22,27,39`). These are overridable by system properties `niagara.fox.multicastEnabled` and
`niagara.fox.multicastTimeToLive` `[CERT]` (`Fox.java:188,195`). So by default a station participates in Fox
multicast discovery on UDP :1911, bounded to ~4 network hops by the TTL.

## 623.2 — The discovery exchange: unauthenticated rollcall → announcement `[CERT]`

`MulticastServer.rollcall(...)` is the discovery client: it sends discovery messages and collects
announcement `FoxMessage` responses into a vector `[CERT]` (`MulticastServer.java:129-153`, `sendRollcallMessage`
:159-177). The exchange carries timing params `responseDelay`/`responseInterval` to stagger responder
replies `[CERT]` (`:194-195`). There is NO authentication on this multicast exchange — it is plain UDP
discovery; any host that joins the group can send a rollcall and receive every station's announcement.

## 623.3 — What the announcement discloses (and what it does NOT) `[CERT]`

`updateAnnouncementAddress` / the `BFoxService` announcement builder populate the response `FoxMessage` with
`[CERT]` (`MulticastServer.java:207-217,242,245`; `BFoxService.java` announcement builder):
- `hostName` + `hostAddress` (IPv4 host name + address)
- `hostNameIPv6` + `hostAddressIPv6` (when a non-loopback IPv6 address exists)
- `niagaraPlatformType` (the platform class — e.g. JACE / Supervisor / Workstation)
- `unicast` flag (transport hint)

It does NOT carry the STATION NAME, software version, host id, or any credential — those are exchanged only
after an authenticated Fox connection's HELLO (`station.name` appears in `remoteHello`, on the TCP session,
REMITTANCE to [B134]/[B515]) `[CERT]` (`BFoxService.java` HELLO handling reads `remoteHello.getString(
"station.name", ...)`). So the multicast surface leaks HOST + PLATFORM reconnaissance, not station identity.

## 623.4 — Reconnaissance-surface characterization `[INFER]`

| Dimension | Fox multicast :1911 (UDP) |
|---|---|
| What it is | Fox station discovery — rollcall/announce so tools (Workbench) can find stations on a LAN |
| Configured | `Fox.multicastEnabled` (default true), TTL 4; group `224.0.1.84` / `FF02::137` :1911 |
| Auth gate | NONE — unauthenticated UDP multicast |
| Reachability (disclosure) | Passive enumeration of every participating Niagara host on the subnet/VLAN: hostname, IPv4+IPv6 address, platform type. NO station name/version/credential |
| Mitigations | `multicastEnabled=false` (system property) disables it; TTL 4 bounds hops; multicast does not cross an L3 boundary without a relay → network segmentation contains it |

Operator guidance `[INFER]`: this is a low-severity reconnaissance surface (host + platform, no secrets), but
in a hardened deployment where stations should not advertise themselves, set
`niagara.fox.multicastEnabled=false`. It is materially less sensitive than the unauthenticated CONTROL ports
(Modbus :502 [B620], SNMP-public [B621]) — it discloses, it does not command.

## 623.5 — Connections

- **[B134]/[B471]/[B515]** — the authenticated Fox protocol (SCRAM, HELLO with `station.name`, session reach)
  that the multicast discovery precedes; B623 is the pre-auth discovery surface, not the data plane.
- **[B398]** — SEC-14 (:1911 Fox plaintext) — B623 adds the multicast-discovery disclosure detail.
- **[B620]/[B621]/[B622]** — the other listening ports; B623 is the least sensitive (disclosure only).
- Forward: **PO-G8** synthesis.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Multicast `224.0.1.84`/`FF02::137` :1911, `multicastEnabled=true` default, TTL 4 | `[CERT]` | Fox.java:20-22,27,39 | ✓ read |
| 2 | Overridable via `niagara.fox.multicastEnabled` / `...multicastTimeToLive` | `[CERT]` | Fox.java:188,195 | ✓ read |
| 3 | Discovery = unauthenticated rollcall → announcement responses | `[CERT]` | MulticastServer.java:129-153,159 | ✓ read |
| 4 | Announcement discloses hostName, hostAddress, IPv6 host/addr, niagaraPlatformType | `[CERT]` | MulticastServer.java:207-217,242 | ✓ read |
| 5 | Station name/version/credential are NOT in the multicast (require Fox HELLO) | `[CERT]` | BFoxService.java (remoteHello `station.name`) | ✓ read |
| 6 | Passive subnet listener can enumerate all participating Niagara hosts | `[INFER]` | from #3/#4 (unauth multicast) | ✓ reasoned |
| 7 | Disclosure-only surface; disable via system property; TTL/segmentation contain it | `[INFER]` | from #1/#2/#4 | ✓ reasoned |

**Tally**: `[CERT]` = 5 · `[INFER]` = 2. **Ratio** ≈ 0.4. Block type = EVIDENCE (the group/exchange/fields are
code-cited; the recon characterization is reasoned). PO-G1 closed.
**Tokens checked**: multicast constants + system-prop overrides in `Fox.java`; rollcall + announcement-field
`add(...)` calls in `MulticastServer.java`; the HELLO `station.name` read in `BFoxService.java`. The scrubbed
announcement-builder method name (`n`) was not cited as identity (§5).
