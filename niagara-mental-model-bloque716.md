# B716 — Station organization, the driver/points layer (SO1): where field points live — `/Drivers/<Network>/<Device>/points/`, and the points-only principle

> Focus: **station-organization** · Gap **SO1** (the driver/points layer). Block TYPE = **DESIGN/HOW-TO**
> (synthesized from verified driver blocks; high [INFER] ratio expected). Feeds `docs/station-organization.md` §1.
> Marker `[CERT]`/`[CERT-hw]` where re-citing verified code/disk; `[INFER]` for the how-to framing. Answers the
> operator's TC500-thermostat / IO-R-34 placement question.

## 716.1 — The canonical tree

[CERT] Every protocol connection is a three-tier tree under `/Drivers/`:
```
/Drivers/
  <Network>/     BDeviceNetwork (or BNNetwork / BSerialNetwork) — owns the transport (BACnet port, RS-485 bus,
                 TCP socket, Fox session), the license gate, worker threads, poll scheduler
    <Device>/    BDevice — one physical endpoint (a BACnet controller by instance, an IO module by bus address)
      points/    children = BControlPoint instances, each with a driver proxyExt
        <point>  BBooleanWritable / BNumericPoint / … + a proxy ext mapping it to a physical address
```
Examples: `BNiagaraNetwork extends BDeviceNetwork` ([Block 415]), `BOpcUaNetwork extends BNNetwork` ([Block 497]),
`BObixNetwork extends BDeviceNetwork` ([Block 499]); for the NRIO field bus, `nrio:NrioNetwork` on COM1 RS-485
([Block 687]).

## 716.2 — The JACE example (deployed evidence)

[CERT-hw] From the JACE-8000 config.bog ([Block 687]):
```
/Drivers/NrioNetwork              nrio:NrioNetwork, RS-485 COM1, ping 30 s
  io34_1_2/                       nrio:Nrio34Module, bus addr 1 (primary = outputs)
    io34_1_2/io34Sec/             nrio:Nrio34SecModule, bus addr 2 (secondary = inputs)
    points/
      ro1                         c:BooleanWritable + nrio:NrioRelayOutputProxyExt (fallback=false)
```
Full ORD: `/Drivers/NrioNetwork/io34_1_2/points/ro1`. The native bus shim under the Java driver is
`libplatnrio.so` (`discover0`/`enablePolling0`/…); BACnet MS/TP uses `libplatmstp.so` ([Block 680]).

## 716.3 — The proxy-point model

[CERT] A point is a `BControlPoint` with an `out` (BStatusValue) and a `proxyExt` ([Block 6]):
- `proxyExt = BNullProxyExt` → NOT a proxy (computed/hardcoded value).
- `proxyExt = a driver subclass` → a proxy of an external device; the driver supplies the subclass.
- `proxyExt.onExecute()` runs first each cycle: read points pull the physical value into `out`; writable points
  push the active priority-array level to the device.

| Point type | Mode | out | note |
|---|---|---|---|
| BNumericPoint / BBooleanPoint | readonly | BStatusNumeric/Boolean | device INPUT, no priority array |
| BNumericWritable / BBooleanWritable | writable | BStatusNumeric/Boolean | 16-slot priority array + fallback → device OUTPUT |

Driver-specific proxy exts (the address binding): `BBacnetProxyExt` (object instance + property, [Block 544]),
`NrioRelayOutputProxyExt` / `BNrioProxyExt` (bus address + channel, [Block 687]/[Block 19]), `BNiagaraProxyExt`
(remote ORD, [Block 415]), `BOpcUaClientProxyExt` (nodeId, [Block 497]), `BObixProxyExt` (href, [Block 499]),
`BMbusProxyExt` ([Block 500]), `BOpcProxyExt` (ItemID, [Block 502]).

## 716.4 — How points are created

[CERT+INFER] Two paths, both landing points under `<Device>/points/`:
1. **Discovery / Learn** (standard): open the driver's Point Manager in Workbench, select discovered
   devices/objects, run the learn `BJob` — it enumerates the address space and auto-creates the correct
   `BControlPoint` subclass with `proxyExt` filled in (e.g. M-Bus `BMbusLivePointSearchDiscoveryJob` [Block 500];
   Fox `discover`/`discoverSlots` on `BPointChannel` [Block 415]).
2. **Manual add**: add a point of the right type and fill the `proxyExt` address by hand (the JACE's single `ro1`
   relay was commissioned this way, [Block 687]).

## 716.5 — TC500 and IO-R-34 placement (the operator's case)

[CERT+INFER]
- **TC500 thermostat** (BACnet): `/Drivers/BacnetNetwork/<tc500>/points/<point>` — each BACnet object it exposes
  (zone temp AV, setpoint AV, mode MV, fan BO) becomes a control point with `BBacnetProxyExt` mapping the object
  instance + property. (A Honeywell-specific driver would create its own `<Network>` but the same three tiers.)
- **IO-R-34** (NRIO field module): `/Drivers/NrioNetwork/<module>/points/<point>` — relay outputs =
  `BBooleanWritable + NrioRelayOutputProxyExt`, digital/analog inputs = `BBooleanPoint`/`BNumericPoint +
  BNrioProxyExt`. Dual-address board (outputs on one address, inputs on the next), as the JACE io34 showed
  ([Block 687]).

## 716.6 — The points-only principle (sets up SO2/SO3)

[CERT+INFER] The `/Drivers/` subtree is the **raw IO mirror** — connectivity + physical address binding ONLY.
**Control logic does not belong here.** Every production example confirms it: chihuahua's command slots
(`fanCmd`/`setpoint`) are LINKED in Workbench to physical BACnet points; the module's logic (`BChiUp`,
`BChiDashboardService`) is entirely separate from the driver ([Block 650]); OpenADR writes a signal the integrator
LINKS to a downstream writable point ([Block 501]). **Rule:** keep `<Device>/points/` to one point per physical
signal, named by its physical role. Alarm/history extensions and control programs go in a separate
application/equipment layer LINKED to these points (SO2/SO3). Then a device re-address changes only the
`proxyExt`, not the logic.

## Connections

- Driver/device/points model → focus `framework-drivers` [Block 496]–[Block 506]; NRIO → [Block 680]/[Block 687];
  control point model → [Block 6]; NiagaraNetwork proxy → [Block 415]; BACnet proxy → [Block 544]. Points-only
  vs logic → [Block 650] (chihuahua)/[Block 501]. Deliverable: `docs/station-organization.md` §1.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 3-tier tree /Drivers/Network/Device/points | [CERT] | [Block 415]/[Block 497]/[Block 499] | cited |
| 2 | JACE NRIO example ro1 under io34/points | [CERT-hw] | [Block 687] | cited |
| 3 | proxy-point model (BControlPoint + proxyExt; read vs writable) | [CERT] | [Block 6]/[Block 544] | cited |
| 4 | discovery/learn BJob + manual add | [CERT] | [Block 500]/[Block 415]/[Block 687] | cited |
| 5 | TC500=BACnet tree, IO-R-34=NRIO tree | [CERT]+[INFER] | [Block 687]/[Block 680] | reasoned |
| 6 | points-only principle (logic linked, not inline) | [CERT] | [Block 650]/[Block 501] | cited |

**Tally:** [CERT/CERT-hw] ×5 · [INFER] ×1. Block TYPE = **DESIGN/HOW-TO** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

SO1 CLOSED. Next: **SO2** (the equipment/application layer — organizing control logic by equipment, kept SEPARATE
from the driver points). Then SO3 (linking), SO4 (nav: hierarchy+tags), SO5 (reuse + synthesis).
