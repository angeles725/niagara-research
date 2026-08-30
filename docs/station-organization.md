# Niagara N4 Station Organization — How-To

> Where field points and control logic go in a station, and the recommended structure. Answers: adding points of
> a TC500 thermostat / IO-R-34 — where does the programming go? Every rule traces to a research block ([Block N]).
> Focus: `station-organization` (B716+).
>
> **The short answer:** two layers. (1) Field points live under their device in the driver tree, points-only.
> (2) Control logic lives in a SEPARATE equipment/application layer, linked to those points. Navigation groups by
> tags + hierarchy, not by duplicating the physical tree.

---

## 1. The driver / points layer — where field points live

### 1.1 The canonical tree
```
/Drivers/
  <Network>/     the transport (BACnet port, RS-485 bus, TCP, Fox) — license, workers, poll scheduler
    <Device>/    one physical endpoint (a controller by instance, an IO module by bus address)
      points/    BControlPoint instances, each with a driver proxyExt
        <point>  BBooleanWritable / BNumericPoint / … + proxyExt (physical address binding)
```
[B415, B497, B499, B687]

### 1.2 The point model
- A point = `BControlPoint` with `out` + `proxyExt`. `proxyExt=BNullProxyExt` → not a proxy (computed).
  A driver subclass → proxy of an external device. [B6]
- **Readonly** (`BNumericPoint`/`BBooleanPoint`) = device INPUT. **Writable** (`BNumericWritable`/
  `BBooleanWritable`) = device OUTPUT with a 16-slot priority array; the proxyExt writes the active level. [B6, B544]
- Address binding per driver: BACnet=`BBacnetProxyExt` (object instance+property), NRIO=`NrioRelayOutputProxyExt`/
  `BNrioProxyExt` (bus addr+channel), Niagara=`BNiagaraProxyExt` (remote ORD), OPC-UA/oBIX/M-Bus each their own. [B544, B687, B415]

### 1.3 How points are created
- **Discovery/Learn** (standard): the driver's Point Manager → learn `BJob` auto-creates the points with proxyExt
  filled in. [B500, B415]
- **Manual add**: add a point of the right type + fill the address by hand. [B687]

### 1.4 Your case
- **TC500 thermostat** (BACnet): `/Drivers/BacnetNetwork/<tc500>/points/<point>` — each BACnet object → a point
  with `BBacnetProxyExt`.
- **IO-R-34** (NRIO): `/Drivers/NrioNetwork/<module>/points/<point>` — relay outs = `BBooleanWritable +
  NrioRelayOutputProxyExt`, inputs = `BBooleanPoint`/`BNumericPoint + BNrioProxyExt`. Dual-address board. [B687, B680]

### 1.5 The points-only principle
`/Drivers/.../points/` is the RAW IO mirror — connectivity + address binding only. **No control logic here.** Keep
one point per physical signal, named by its physical role. Logic goes in a separate equipment layer LINKED to
these points (§2/§3). Then a device re-address changes only the proxyExt, not the logic. [B650, B501]

---

*(equipment/logic layer, linking, navigation, and reuse are added as SO2–SO5 close.)*
