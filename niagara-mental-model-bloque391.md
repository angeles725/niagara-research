# Block 391 — what N4.15 added to the BACnet driver: 73 new `bacnet-rt` classes = a jump to a newer ASHRAE 135 revision (BACnet/SC datatypes, Elevator/Lift objects, Timer + Value_Source, COV-Multiple, BBMD/routing), all additive

> **Version-axis deep-dive (surfaced by the license-diff japicmp thread [B390]).** [B390] found `bacnet-rt`
> was the one large ADDITIVE grower 4.14→4.15 (74 new classes, 0 removed). This block enumerates and
> categorizes those additions — what the 4.15 BACnet driver gained. It is a VERSION-axis / protocols-line
> finding, not a license one; filed here because it emerged from the install diff. READ-ONLY. Block type:
> EVIDENCE (tool). Extends the wire-level BACnet of [B131] and the `bacnetSc` license feature of [B387].
>
> Source: `japicmp 0.23.1` diff of `bacnet-rt.jar` A(4.14.0.162) → B(4.15.3.28.2), new-class list
> (`audits/B391-bacnet-new-classes.txt`, `audits/B391-bacnet415-summary.txt`).
> Markers: `[CERT]` observed in the japicmp new-class list (name cited) · `[INFER]` deduction (ASHRAE mapping).

---

## 391.1 — Five themes in the 73 new classes `[CERT]`

japicmp reports **73–74 new `bacnet-rt` classes, 89 modified, 0 removed** 4.14→4.15 ([B390]). Grouped: `[CERT]`

1. **BACnet/SC — Secure Connect (~10)** `[CERT]`: `BBacnetScConnectionState`, `BBacnetScDirectConnection`,
   `BBacnetScHubConnection`, `BBacnetScHubFunctionConnection`, `BBacnetScFailedConnectionRequest`,
   `BBacnetHostAddress`(+`Choice`), `BBacnetNetworkPortCommand`/`Descriptor`/`PendingChanges`. The datatype +
   Network-Port surface for BACnet/SC (the websocket/TLS hub-and-node transport). This is the on-the-wire
   substance behind the **`bacnetSc` license feature** [B387 §387.3] — the feature existed to gate it; 4.15
   ships its full datatype model. `[CERT]/[INFER]`
2. **Elevator / Lift objects (~17)** `[CERT]`: `BBacnetLiftCarMode`/`Direction`/`DoorCommand`/`DriveStatus`/
   `CallList`(+`Entry`), `BBacnetLiftFault`/`GroupMode`, `BBacnetEscalatorFault`/`Mode`/`OperationDirection`,
   `BBacnetLandingCallStatus`(+`CommandChoice`), `BBacnetLandingDoorStatus`(+`Entry`),
   `BBacnetAssignedLandingCalls`(+`Entry`) — plus new `enums.elevator` + `datatypes.elevator` packages. This
   is the ASHRAE 135 **Lift/Escalator object** family (Addendum), absent in 4.14. `[CERT]/[INFER]`
3. **COV-Multiple + event subscription (4)** `[CERT]`: `BBacnetCovMultipleSubscription`(+`CovReference`,
   +`Specification`), `BBacnetEventNotificationSubscription` — SubscribeCOVPropertyMultiple support.
4. **Timer object + Value_Source (~8)** `[CERT]`: `BBacnetTimerState`/`Transition`/`StateChangeValue`
   (+`Choice`) — the 135-2016 **Timer object**; `BBacnetValueSource`(+`Choice`) — the **Value_Source**
   property (command-source tracking, 135-2016). `[CERT]/[INFER]`
5. **Network layer / BBMD / routing + Optional wrappers (~34)** `[CERT]`: `BBacnetBdtEntry`/`FdtEntry`/
   `VmacEntry`/`RoutingTableEntry`, `BBacnetIpServerPort`/`BacnetIpInterface`/`BBacnetIpMode`/
   `BBacnetNetworkType`/`ProtocolLevel`/`RouterStatus`/`HostNPort` (BBMD/foreign-device/network-port
   modernization), and a large `BBacnetOptional*` value-wrapper set (`OptionalDate`/`Time`/`DateTime`/
   `Integer`/`Double`/`BitString`/`OctetString`/`DoorValue`/`BinaryLightingPv`/…) plus
   `BBacnetChangeOfValueCriteria`(+`Choice`), `BBacnetExtendedParameter`(+`Choice`), `BBacnetPortPermission`,
   `BBacnetSecurityDashboardProvider`. `[CERT]`

---

## 391.2 — Reading `[CERT]/[INFER]`

The shape is unambiguous: **4.15 pulls the Niagara BACnet stack up to a newer ASHRAE 135 revision** — Secure
Connect, Lift/Escalator objects, the Timer object, Value_Source, COV-multiple, and modernized BBMD/routing/
network-port datatypes — while **removing nothing** (0 classes deleted, the base API stays binary-compatible
per [B390]). The `datatypes` package dominates (38 of the new classes) because ASHRAE objects are modeled as
`BComponent` datatypes in Niagara ([B131] wire model). So an integrator on 4.14 gains these capabilities by
upgrading to 4.15 with no API breakage — the only upgrade gate is SMA/module-maintenance date ([B387 §387.5]),
not compatibility. `[CERT]/[INFER]`

Cross-line note: this is where the license and version axes touch — the **`bacnetSc` feature** the license
grants ([B387]) is the runtime gate; the **4.15 datatypes** here are the implementation it gates. A 4.14
station with a `bacnetSc` license could not use SC datatypes that did not yet exist; 4.15 ships them. `[INFER]`

---

## 391.3 — Self-verify

**Token re-checks** (`audits/B391-bacnet-new-classes.txt`):
1. 73 new-class FQNs captured; `javax.baja.bacnet.datatypes`=38, `enums.elevator`/`enums`=9+9, `datatypes.elevator`=8 — ✓ (japicmp package histogram).
2. SC set `BBacnetSc{ConnectionState,DirectConnection,HubConnection,HubFunctionConnection,FailedConnectionRequest}` present — ✓.
3. Lift/Escalator set (`BBacnetLiftCar*`, `BBacnetEscalator*`, `BBacnetLanding*`) present — ✓.
4. Timer (`BBacnetTimerState/Transition`) + `BBacnetValueSource` present — ✓.
5. 0 classes removed in bacnet-rt — ✓ ([B390] histogram).

**5/5 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 11 · `[INFER]` 4 (the ASHRAE-revision mapping, the feature↔datatype linkage).
Ratio ≈ 0.36 — a categorization block over a tool's class list; the [INFER]s are the ASHRAE-object naming
interpretation, each backed by the verbatim class names. EVIDENCE block.

---

## 391.x — Connections

- **[B390]** — this expands its "bacnet-rt +74, 0 removed" one-liner into the actual capability additions.
- **[B131]** — the BACnet wire/object model these new datatypes extend (SC transport, new ASHRAE objects).
- **[B387 §387.3]** — the `bacnetSc` license feature is the runtime gate; §391.1 theme-1 is the 4.15 datatype
  implementation it gates (the license/version axes meet here).
- **[B378]** — `bacnetAlarmRouter` (alarm routing) — a different bacnet add-on; unaffected by these datatypes.
- **Not license-axis**: this closes the version-axis curiosity the japicmp diff surfaced; the `license-diff`
  focus stays answered 6/6.
