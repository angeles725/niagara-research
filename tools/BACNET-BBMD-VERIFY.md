# Runbook — Verify BBMD / Who-Is reachability for a multi-subnet BACnet/IP deployment

Companion to `tools/bacnet-bbmd-verify.py`. Grounded in block **B444** (BACnet/IP device
addressing & dynamic binding). Wire constants and Niagara BBMD behavior are `[CERT]` from the
`bacnet-rt` bytecode corpus (see the script's docstring for file:line).

## The question this answers

You add thermostats under a `BacnetNetwork` with the MAC as `<ip>:0xBAC0`
(e.g. `192.168.0.50:0xBAC0`). If a device's IP changes, will Niagara re-bind it automatically by
its **Device Instance**? That works ONLY if a broadcast **Who-Is** (and the device's **I-Am**)
can cross between subnets — which requires a **BBMD** or a **Foreign-Device** registration.

**An ICMP `ping 192.168.0.50` succeeding does NOT prove this.** Two different transports:

| Traffic | Transport | Crosses a router? |
|---|---|---|
| Poll / COV / reads (`readProperty(getAddress(),…)`) | **unicast** to `IP:port` | yes — like ICMP |
| Who-Is (`checkAddress` → `whoIs(GLOBAL_BROADCAST,…)`) | **broadcast** `0xFFFF` | **no**, without a BBMD |

So a pinned `IP:0xBAC0` polls fine across subnets today by unicast; that says nothing about
rebind-on-IP-change. This runbook + tool separate the two.

## Your topology — fill in the blanks

```
[ Supervisor / JACE ]  IP: __________   subnet: __________   (run the tool here)
        |
     [ router ]
        |
[ Thermostats ]        subnet: 192.168.0.0/24   sample device: 192.168.0.50   port: 0x BAC0 (47808)

BBMD expected at: __________   (JACE IpPort? a device? a router? "none yet"?)
```

Run the tool from the **supervisor/JACE host** (or any host on the supervisor's subnet) so the
broadcast test reflects what Niagara's own Who-Is would reach.

## Step 1 — Run the wire test (read-only)

```bash
# If Niagara is NOT running on this host (port 47808 free):
python3 tools/bacnet-bbmd-verify.py \
    --device-ip 192.168.0.50 \
    --local-bcast <this-host-subnet-broadcast, e.g. 10.0.0.255> \
    --bbmd <candidate BBMD IP, e.g. the JACE IpPort IP>

# If Niagara HOLDS 47808 on this host, bind an ephemeral source port:
python3 tools/bacnet-bbmd-verify.py --device-ip 192.168.0.50 \
    --local-bcast <bcast> --bbmd <bbmd-ip> --src-port 0
```

The tool sends real BACnet frames: a directed **unicast** Who-Is to the device, a **broadcast**
Who-Is on the local wire, and a **Read-BDT/Read-FDT** to the candidate BBMD.

## Step 2 — Read the verdict

| Result | Meaning | Action |
|---|---|---|
| **REBIND OK** — a device from `192.168.0.x` answered the **broadcast**, and/or a BBMD returned a BDT | Who-Is crosses the router. Instance-based rebind works. | Nothing. Optionally leave the MAC blank and let Niagara learn it. |
| **REBIND WILL FAIL** — device answers **unicast** but NOT the broadcast, no BBMD replied | Cross-subnet devices work only via their pinned IP. An IP change breaks them. | Configure a BBMD (Step 4) **or** guarantee the IP (Step 5). |
| **INCONCLUSIVE** — no unicast answer either | Wrong IP/port, firewall, or non-routed host. | Fix reachability and re-run. |

If a BBMD answered, the printed **BDT** lists the peer subnets it distributes broadcasts to —
the authoritative proof of which subnets are bridged. The **FDT** lists registered foreign
devices with their TTL.

## Step 3 — Cross-check in Workbench (GUI)

1. **Discover test (fastest sanity check).** Open the `BacnetNetwork` → **Discover**. It fires a
   global Who-Is. If your `192.168.0.x` thermostats appear **without** having their MAC
   pre-entered, broadcasts are crossing (a BBMD is relaying). If only same-subnet devices appear,
   the cross-subnet ones live purely on their pinned unicast address — matching a REBIND-WILL-FAIL
   verdict.
2. **Inspect the IP port config.** `BacnetNetwork` → `Bacnet Comm` → `Network` → the **IP port**.
   Niagara's BBMD/Foreign-Device surface (component types confirmed in `bacnet-rt`:
   `BBroadcastDistributionTable`/`BBdtEntry`, `BForeignDeviceTable`/`BFdtEntry`,
   `BForeignDeviceRegistration`):
   - **BBMD mode**: a populated **Broadcast Distribution Table** (one entry per remote subnet's
     BBMD) means this JACE acts as a BBMD.
   - **Foreign-Device mode**: a **registration** pointing at a remote BBMD IP + TTL means this
     JACE registers itself as a foreign device to reach the other subnet's broadcasts.
   - Empty both → no cross-subnet broadcast path exists.

## Step 4 — If you need a BBMD (cross-subnet rebind required)

BACnet's answer to routers blocking broadcasts. One of:

- **A BBMD on each subnet that has devices**, with each BBMD's **BDT** listing all the other
  BBMDs. Broadcasts are then forwarded device-subnet ↔ supervisor-subnet. Many JACEs and field
  routers can be a BBMD (enable it on the IP port and populate the BDT with the peer BBMD IPs).
- **Foreign-Device registration**: if only the supervisor/JACE needs the other subnet's
  broadcasts and that subnet already has a BBMD, register the JACE IP port as a **foreign device**
  to that BBMD (IP + TTL). Niagara re-registers before the TTL expires
  (`RegisterForeignDevice`, BVLL function `0x05`).

After configuring, re-run Step 1 — a REBIND-OK verdict confirms it end to end.

## Step 5 — Or sidestep it (often simpler)

If cross-subnet broadcast is not worth a BBMD, make the IP stable instead:

- **DHCP reservation** (MAC-pinned lease) or a **static IP** per thermostat. The pinned
  `IP:0xBAC0` unicast path already works across the router (that is why polling works today), so a
  stable IP removes the need for rebind entirely.
- Keep every **Device Instance globally unique** regardless — it is the real identity key.

## Caveats

- The ping-failure re-Who-Is inside Niagara is throttled to every **100** failed pings
  (`niagara.bacnet.checkAddressAfterFailedPings`); fastest recovery is the device's own
  unsolicited I-Am — which is itself a broadcast, so it too needs the BBMD path across subnets.
- The tool is **read-only**: it never registers a foreign device or writes a BDT. Foreign-Device
  registration is a state change on the BBMD; do it in Workbench (Step 4), not from this tool.
- Authorized networks only. This sends BACnet management frames; run it against your own plant.
