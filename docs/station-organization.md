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

## 2. The equipment / logic layer — where control logic goes

**Logic lives in the station component space (`/Config` or `/Services`), NOT under `/Drivers`.** chihuahua puts
its equipment tree under `/Services/ChiDashboardService`; v2 deliberately moved it out of `/Drivers`. [B169, B165]

### 2.1 Three ways to author logic

| Method | What | Use when |
|---|---|---|
| **kitControl on a Wire Sheet** | ~100+ prebuilt blocks wired with BLinks, no Java (or an `Expr` block for a simple value) | standard HVAC/control — the everyday default [B6, B538] |
| **BProgram** | freeform Java-like, persisted in `.bog`, sandboxed | logic beyond the blocks (sequencing, custom math) [B541] |
| **Custom module** | compiled `BComponent` with typed slots, deployed as a signed `-rt` jar | reusable equipment TYPES / OEM models (see the module guides) [B705-B715] |

### 2.2 Group by equipment
One component = one physical equipment, holding its logic + links to its points. Tag the equipment-folder
component `equip` (Haystack/Niagara) so it becomes the boundary for semantic queries / `equipRef`. [B169, B264]

### 2.3 Official placement: keep blocks NEAR their points (Philosophy B)
Tridium recommends co-locating kitControl blocks **near the points they serve** (per equipment), NOT in a distant
central `Logic` folder — a central folder "breeds off-view knobs and harder-to-follow logic." [B538 BP5]

So the two layers are: (1) proxy points = the device's raw IO (points-only); (2) equipment logic = a
per-equipment component/area **co-located with those points**, linking to them — not a monolithic central Logic
tree, and not inside the raw `points/` container. [B538, B716]

### 2.4 Why separate
Portability (re-address touches only the ProxyExt), legibility, RBAC (app layer gated independently), reuse
(one type × N instances — 77 BChiUp). [B538, B648, B169]

---

## 3. Linking points ↔ logic

The **BLink** is the bridge: it connects a source slot to a target slot; a change on the source fires the target.
The equipment logic reads input points and writes output points purely through links — the logic never lives
inside the points. [B6]

### 3.1 The priority array (multi-source writes)
A writable point has 16 priority levels (`in1..in16`) + `fallback`; the active = lowest non-null. Link your
control logic at a documented level (e.g. program default), leave higher levels for overrides. BACnet maps the
level to the object's command priority. [B6, B544, B716]

### 3.2 Wiring the links
- **BBatchLinkEditor** (Workbench, design-time): bulk-create links — dry-run `checkLink`, one Transaction per
  space. Use this to wire equipment logic to points at commissioning. [B654]
- **ChiLinkHelper** (runtime): backup/restore links **by handle** so they survive save/transfer/re-provision. [B650]

### 3.3 Keep links stable across re-addressing
Link by a stable handle (not a fragile path), and reference the target via `equip`/`equipRef` relations or a tag
query where possible — so re-addressing the TC500 or IO-R-34 touches only the point's proxyExt, not the logic. [B538, B650, B169]

### 3.4 End-to-end (TC500 / IO-R-34)
1. **Points**: `/Drivers/BacnetNetwork/TC500/points/…`, `/Drivers/NrioNetwork/io34/points/…` (raw IO, discovery).
2. **Logic**: a per-equipment component near the points, tagged `equip`.
3. **Links**: BLinks connect logic ↔ points (read inputs, write outputs at a priority level), wired with the
   batch editor, kept handle/tag-stable.

---

## 4. Navigation & grouping — tag once, navigate many ways

The physical tree is engineering-shaped. Operators want views by equipment/location. **Don't duplicate the tree
into parallel folders** — tag once, build alternate views.

### 4.1 Tags (semantic layer)
Attach markers (`equip`, `point`, `hvac`) + value tags (`geoFloor`, `siteId`) + relations (`equipRef`,
`childPoint`) to components, independent of tree position. [B260-B270, B264]

### 4.2 Hierarchies (alternate nav trees)
The `hierarchy` subsystem builds navigation trees from ordered level-defs (Group/Query/Relation) over the SAME
components, WITHOUT moving them — on-demand, stateless, so it never drifts. E.g. "by building → by equipment
type → equip leaves." Permissions stay = component categories (RBAC preserved). [B584-B590]

### 4.3 The pattern
1. Physical tree stays engineering-shaped (points under devices, logic per equipment).
2. Tag each equipment `equip` + geo/role tags; wire `equipRef` point→equipment.
3. Hierarchies give the operator views (by equipment/location/system) — no duplicate folders.
4. PX/nav views bind to hierarchies or tag queries, not hardcoded ORDs (also keeps links stable, §3).

### 4.4 Your case
Tag the TC500 zone + IO-R-34 equipment `equip` + `geoFloor`; keep `equipRef` on their points; build one "by floor
→ by equipment" hierarchy. Operators browse zones without moving any point or logic component.

---

## 5. Reuse

- **Equipment templates** (`.ntpl`): model one equipment well (points+logic+links+tags), capture it as a template
  with typed parameters, deploy N instances each re-bound to a different device's points. [B577-B583, B591]
- **Provisioning**: push the structure/updates to a fleet of JACEs via `BBatchJob` over the `niagaraProv` channel
  (template deploy is a provisioning step). [B567-B576]

---

## Summary — the recommended structure

| Layer | Where | Rule |
|---|---|---|
| IO / points | `/Drivers/<Net>/<Device>/points/` | points-only, one per physical signal, from discovery/learn |
| Equipment / logic | `/Config` or `/Services`, per-equipment, near its points | one component = one equipment, tagged `equip`, Philosophy B |
| Links | between the two | BLink + priority array, handle/tag-stable |
| Navigation | hierarchies over tags | tag once, navigate many; never duplicate the tree |
| Reuse | templates + provisioning | build once, stamp/push to the fleet |

**Do NOT:** logic in `/Drivers` or inside `points/`; a monolithic central `Logic` folder far from the points;
duplicating the tree for an operator view; hardcoded ORDs that break on re-address.

*Every rule traces to a [Block N] in the corpus.*
