# Block 444 — BACnet/IP device addressing and dynamic address binding: the MAC is `IP:0xBAC0`, the stable key is the Device Instance, and Niagara re-resolves the MAC by Who-Is/I-Am

> **Mode:** DOCUMENT/CAPTURE (METHODOLOGY §20) — captures a client-facing finding produced in-session, not a
> gap-discovery pass. **Question the operator asked:** when adding a thermostat under `BacnetNetwork` the MAC
> is entered as `<device-ip>:0xBAC0` (e.g. `192.168.0.50:0xBAC0`); can the device's *hardware* MAC be entered
> instead so Niagara keeps finding it if the IP changes? And can a device be added with only its Device
> Instance and NO MAC, relying on Who-Is/I-Am?
>
> **Scope:** the `bacnet` driver's client-side device addressing and re-binding path only. Does NOT re-derive
> the BACnet object/property/COV model [B23], the APDU/NPDU/BVLC wire codec [B133], discovery→point typing
> [B271], or the N4.15 BBMD/routing additions [B391] — those are REMITTANCE and cited where they bound this
> answer.
>
> **Sources (Tridium original, non-decompiled — `docSource/extracted`, javadoc intact):**
> `organized/docSource/docSource-doc/extracted/bacnet-rt/javax/baja/bacnet/datatypes/BBacnetAddress.java`;
> `.../javax/baja/bacnet/BBacnetDevice.java`; `.../javax/baja/bacnet/BBacnetNetwork.java`. Decompiled
> (`com.tridium`, vineflower): `organized/bacnet/bacnet-rt/vineflower/com/tridium/bacnet/stack/BBacnetStack.java`.
> Markers: `[CERT]` verbatim in local source (file:line), `[INFER]` bounded synthesis, `[CERT-doc]` Tridium
> javadoc. **Type:** MIXED source-evidence / operator-facing synthesis.

---

## 444.1 — In BACnet/IP the "MAC address" field IS `IP:port`; it is not the Ethernet hardware MAC `[CERT]`

`BBacnetAddress` is a `BStruct` of two properties: a fixed 16-bit `networkNumber` and a variable-length
`macAddress` of type `BBacnetOctetString`
(`BBacnetAddress.java:38-52`). The javadoc states the identity contract verbatim: *"containing a fixed 16-bit
network number and a variable length MAC address. A Bacnet device is uniquely identified by the combination
of network number and MAC address."* (`BBacnetAddress.java:21-23`) `[CERT-doc]`

The *meaning* of the MAC octets is set by an `addressType` discriminator with named constants — the relevant
one is `MAC_TYPE_IP = 2` (`BBacnetAddress.java:534`). For a BACnet/IP device the MAC is **6 octets = 4 IP +
2 UDP port**. This is proven by both the render and the parse paths:

- `toString` for `MAC_TYPE_IP` prints `a.b.c.d` from `mac[0..3]`, then, if `mac.length > 5`, appends `:0x` +
  the hex of `(mac[4]<<8)|mac[5]` (`BBacnetAddress.java:415-427`). So `192.168.0.50:0xBAC0` is exactly IP
  `192.168.0.50` + port `0xBAC0` = 47808, the ASHRAE-registered default BACnet/IP UDP port. `[CERT]`
- `parse` for `MAC_TYPE_IP` tokenizes on `".: "`; the 5-token form reads 4 IP octets then one port token,
  packing the port into `b[4],b[5]` for a 6-byte MAC (`BBacnetAddress.java:472-494`). `[CERT]`

Therefore the operator is NOT entering "IP instead of MAC" — in BACnet/IP the IP+port **is** the MAC. There
is no separate slot for the NIC/Ethernet hardware address of the field device. The Ethernet 6-byte hardware
MAC is only a BACnet MAC under `MAC_TYPE_ETHERNET = 1` (a BACnet-over-802.3 data link, distinct from IP and
rarely deployed); under MS/TP the MAC is a single station byte (`MAC_TYPE_MSTP`), and BACnet/SC uses a
6-octet VMAC (`MAC_TYPE_SC`, [B280]). Entering an Ethernet hardware MAC into an IP device's address does not
address anything — BACnet/IP routes over UDP/IP, not over Ethernet MACs. `[CERT]`/`[INFER]`

## 444.2 — The stable identity is the Device Instance; the MAC is a cache Niagara rewrites `[CERT]`

The operator's real goal — survive an IP change — is served not by the MAC but by the **Device Instance**
(the Device Object Identifier, the "device id" entered first). The re-binding chain, all verbatim:

1. `checkAddress()` issues a Who-Is **filtered by this device's own instance number** as both low and high
   limit, sent to the global broadcast address:
   `client().whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, instanceNumber, instanceNumber)`
   (`BBacnetDevice.java:1044-1055`; `GLOBAL_BROADCAST_ADDRESS` has network `BROADCAST_NETWORK = 0xFFFF` and a
   null MAC, `BBacnetAddress.java:529,539`). `[CERT]`
2. The target answers **I-Am**, carrying its *current* address. `BBacnetStack.receiveIAm` forwards it:
   `this.bacnet().updateDeviceInfo(iAm.getObjectId(), addr, ...)` (`BBacnetStack.java:146-147`). `[CERT]`
3. `BBacnetNetwork.updateDeviceInfo` looks the device up **by Object Identifier, not by MAC**:
   `BBacnetDevice device = doLookupDeviceById(objectId)` and then calls `device.updateDeviceInfo(...)`
   (`BBacnetNetwork.java:971-989`). `[CERT]`
4. `BBacnetDevice.updateDeviceInfo` stamps the address type from the port's link layer
   (`BBacnetIpLinkLayer → MAC_TYPE_IP`, `BBacnetDevice.java:1493-1505`) and — only when the incoming MAC
   differs from the stored one — **overwrites the address property in place**:
   `if (!getAddress().equals(newAddress.getNetworkNumber(), newAddress.getMacAddress().getBytes())) set(BBacnetDevice.address, newAddress, noWrite);`
   (`BBacnetDevice.java:1508-1513`). `[CERT]`

So when a device's IP changes and it emits an (unsolicited) I-Am, Niagara matches it by instance and rewrites
the stored `IP:0xBAC0` for the operator. The `192.168.0.50:0xBAC0` in the database is a **cached binding**,
not a fixed key. This is BACnet dynamic address binding; the instance is the anchor. `[CERT]`/`[INFER]`

## 444.3 — A device CAN be added with only the Device Instance and a null MAC `[CERT]`

`isAddressValid()` is `addr != null && !addr.getMacAddress().isNull()` (`BBacnetDevice.java:1038-1041`). A
device whose MAC octet-string is empty is therefore "address-invalid" — but that is a resolvable state, not a
configuration error:

- On start/config, an address-invalid device is set `stale` and immediately calls `checkAddress()`
  (`BBacnetDevice.java:1108-1112`). `[CERT]`
- `doPing()` branches on validity. If the address is invalid it takes the explicit else branch whose own
  comment documents the intent — *"If/When the 'i-am' comes in from the device it will have its system status
  read by the updateDeviceInfo method"* — and calls `checkAddress()` (`BBacnetDevice.java:1607-1611`). The
  returning I-Am then runs §444.2's chain and populates the MAC. `[CERT]`

Answer to the operator: **yes** — enter the Device Instance and leave the MAC blank; Niagara marks the device
stale, broadcasts a Who-Is scoped to that instance, and fills the MAC from the I-Am automatically. In practice
Workbench's **Discover** does the same up front: a global Who-Is collects I-Am replies carrying both instance
AND address, so a discovered-and-added device arrives with its MAC already learned — manual MAC entry is
convenience/pinning, not a requirement. `[CERT]`/`[INFER]`

## 444.4 — Two caveats that decide whether auto-rebinding actually works `[CERT]`

1. **Reachability of the Who-Is — and why an ICMP ping across subnets proves nothing here.** There are TWO
   different transports in play. Every *confirmed* service (the poll, COV, reads) is sent **unicast to the
   device's stored address**: `client().readProperty(getAddress(), ...)`, `readPropertyMultiple(getAddress(),
   ...)`, `subscribeCov(getAddress(), ...)` (`BBacnetDevice.java:1621,1971,2270`). Unicast UDP is routed like
   any IP packet, so it crosses a router exactly as an `ICMP ping 192.168.0.50` does. **This is why a
   cross-subnet device with a pinned `IP:0xBAC0` polls fine today — not because Who-Is/I-Am crosses the
   router.** `checkAddress()`, by contrast, uses the *global broadcast* (`0xFFFF`, null MAC,
   `BBacnetDevice.java:1051`, `BBacnetAddress.java:539`), and a BACnet broadcast does **not** cross an IP
   router. So a successful ICMP ping (or working polling) demonstrates unicast reachability only; it does NOT
   demonstrate that a Who-Is or an unsolicited I-Am will traverse the subnet boundary — the device's I-Am is
   itself a broadcast on its own subnet. Cross-subnet Who-Is/I-Am works ONLY when a **BBMD** relays broadcasts
   (or the JACE is a registered **Foreign Device**) — the BBMD/routing surface in [B391]. Without that, the
   moment the IP changes, re-binding silently fails and `doMacAddressFailed()` reports
   `pingFail("Cannot resolve MAC address")` (`BBacnetDevice.java:2686-2698`), even though the old IP still
   pinged. To actually test it: run a Workbench **Discover** on the `BacnetNetwork` — if the other-subnet
   devices appear WITHOUT their MAC pre-entered, a BBMD is relaying broadcasts; if only local-subnet devices
   appear, the cross-subnet ones live purely on their pinned unicast address. `[CERT]`/`[INFER]`
2. **When the re-Who-Is fires for an already-bound device.** Re-resolution on ping failure is throttled: the
   ping worker only calls `checkAddress()` once every `CHECK_ADDRESS_AFTER_PING_FAILS` failed pings
   (`BBacnetDevice.java:1584`), and that constant defaults to **100**
   (`niagara.bacnet.checkAddressAfterFailedPings`, `BBacnetDevice.java:2947-2948`). So a device that already
   had a valid MAC and then vanished is not re-broadcast immediately — recovery is fastest when the device
   itself emits an unsolicited I-Am after its IP change (handled instantly in §444.2). `[CERT]`

Practical verdict: for IP-change resilience prefer a **DHCP reservation or static IP** per field device; if
you rely on instance-based rebinding, keep the Device Instance globally unique and ensure Who-Is/I-Am can
reach the device (same subnet, or a BBMD across subnets). The MAC field is managed by Niagara either way.
`[INFER]`

## 444.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BACnet address = networkNumber(16-bit) + variable macAddress octet-string; identity = (net, MAC) | `[CERT-doc]` | `BBacnetAddress.java:21-23,38-52` |
| 2 | `MAC_TYPE_IP = 2`; IP MAC renders as `a.b.c.d:0xPORT`, port = `(mac[4]<<8)|mac[5]` | `[CERT]` | `BBacnetAddress.java:415-427,534` |
| 3 | `0xBAC0` = 47808 = default BACnet/IP UDP port (the value the operator types) | `[CERT]`/`[INFER]` | `:420-425` + registered default |
| 4 | Ethernet hardware MAC is a different address type (`MAC_TYPE_ETHERNET=1`), not the IP MAC | `[CERT]` | `BBacnetAddress.java:407-413` |
| 5 | `checkAddress()` sends Who-Is scoped to the device's own instance, global broadcast | `[CERT]` | `BBacnetDevice.java:1044-1055`; `BBacnetAddress.java:529,539` |
| 6 | I-Am → `updateDeviceInfo` → device looked up **by objectId**, not by MAC | `[CERT]` | `BBacnetStack.java:146-147`; `BBacnetNetwork.java:971-989` |
| 7 | Stored address overwritten in place when incoming MAC differs | `[CERT]` | `BBacnetDevice.java:1508-1513` |
| 8 | Null-MAC device is valid: set stale + `checkAddress()`; `doPing` else-branch resolves via I-Am | `[CERT]` | `BBacnetDevice.java:1038-1041,1108-1112,1607-1611` |
| 9 | Global-broadcast Who-Is needs BBMD/Foreign-Device across subnets; else `pingFail("Cannot resolve MAC address")` | `[CERT]`/`[INFER]` | `BBacnetDevice.java:2686-2698`; [B391] |
| 10 | Ping-failure re-Who-Is throttled to every 100 failed pings (tunable) | `[CERT]` | `BBacnetDevice.java:1584,2947-2948` |
| 11 | Confirmed services (poll/COV/reads) are unicast to `getAddress()` — cross a router like ICMP; Who-Is is broadcast and does not | `[CERT]`/`[INFER]` | `BBacnetDevice.java:1621,1971,2270` vs `:1051` |

- Load-bearing tokens checked: **11/11** (address struct, MAC_TYPE_IP render+parse, Who-Is-by-instance,
  lookup-by-objectId, in-place address rewrite, null-MAC ping path, broadcast/BBMD caveat, throttle constant,
  unicast-poll vs broadcast-Who-Is).
- Scope check: conclusion restricted to the client-side addressing/re-binding path; object model, wire codec,
  discovery typing, and full BBMD/routing mechanics are cited, not re-derived.
- No `[INFER]` presented as `[CERT]`; every code claim carries file:line.
- Secrets check: N/A — no live install, no credentials/keys/host IDs; the sample IP `192.168.0.50` is the
  operator's illustrative value, not captured infrastructure. MCP/web sources: none.

## 444.x — Connections

- **Uses [B23]** (BACnet deep: objects/properties/networking stack) — this block opens the specific device
  addressing/re-binding path that B23 mapped at the stack level, without repeating the object model.
- **Uses [B133]** (APDU/NPDU/BVLC codec) — Who-Is/I-Am are the unconfirmed services carried by that transport;
  their encoding is REMITTANCE.
- **Uses [B271]** (discovery→point) — Workbench Discover is the Who-Is/I-Am sweep referenced in §444.3; point
  typing after discovery is out of scope here.
- **Uses [B391]** (N4.15 BBMD/routing additions) — the cross-subnet reachability caveat in §444.4 depends on
  that BBMD/Foreign-Device surface.
- **Relates [B280]** (BACnet/SC VMAC) — one more `addressType` where the "MAC" is not IP:port.
- Document-mode capture: no discovery gap opened. Two typed follow-ups remain live-only (`requires-execution`):
  observing an actual I-Am rebind after an IP change, and BBMD relay behavior on a live multi-subnet station.
