# B717 — Station organization, the equipment/logic layer (SO2): where control logic lives, the three authoring methods, and the official "keep blocks near their points" guidance

> Focus: **station-organization** · Gap **SO2** (the equipment/application layer). Block TYPE = **DESIGN/HOW-TO**
> (synthesized from verified blocks; high [INFER] ratio expected). Feeds `docs/station-organization.md` §2.
> Marker `[CERT]`/`[CERT-doc]` where re-citing verified code/official docs; `[INFER]` for framing.

## 717.1 — Logic lives in the station component space, NOT under /Drivers

[CERT] Control logic lives in the application component space, separate from the driver point tree:

| Space | Root | Contents |
|---|---|---|
| Driver | `slot:/Drivers/…` | raw device points + ProxyExt (SO1) |
| Application | `slot:/Config/…` or `slot:/Services/…` | equipment components, control logic, schedules |

The chihuahua production module makes this explicit: its equipment hierarchy root is `/Services/ChiDashboardService`
— "su raíz NO es el árbol de driver sino el servicio" ([Block 169] §169.5). An earlier v1 had everything under
`/Drivers/MX60/Chihuahua`; **v2 deliberately moved the logic out to `/Services`** ([Block 165]) — a real project
that proved the separation pays off.

## 717.2 — The three ways to author logic (and when to use each)

[CERT+CERT-doc]

1. **kitControl function blocks on a Wire Sheet** (graphical — the everyday default). ~100+ prebuilt
   `BComponent` blocks (`BLoopPoint`, `BAnd`, `BAdd`, …) wired with BLinks; `executeOnChange` fires the
   downstream block. No Java ([Block 6] §6.3.3; kitControl focus [Block 537]/[Block 538]/[Block 545]). *Use for*
   standard HVAC/control (PID, setpoint, scheduling, alarm triggers). For a simple computed value, an `Expr`
   (BQL expression) block is even lighter ([Block 538] BP6).
2. **BProgram** (freeform, Java-like escape hatch). `BProgram extends BComponent`, persisted in `.bog` (source +
   bytecode, no disk `.class`); override `onStart`/`onExecute`/`onStop`; slot-sheet properties + named actions.
   Sandboxed (file access limited to `station_home`, `Runtime.exec` blocked, edit superUser-only). Freeform
   (long-lived) vs Robot (one-shot, not persisted) ([Block 541]). *Use when* logic exceeds the blocks
   (multi-step sequencing, custom math/strings).
3. **Custom module** (compiled `BComponent`). Typed `@NiagaraProperty`/`@NiagaraAction` slots + `changed()`/
   execute logic in Java; full JVM, no sandbox; deployed as a signed `-rt` jar. E.g. `BChiUp` (49 slots, a 10 s
   `controlTick` running `applyProtections()`), `BEquipment` (3-input state machine, [Block 422]). *Use for*
   reusable equipment TYPES instantiated across many units, or OEM domain models (the module focuses A/B cover
   HOW to build these — [Block 705]–[Block 715]).

## 717.3 — The equipment-grouping model

[CERT] The canonical structure is **one component = one physical equipment**, holding that unit's logic, state,
and links to its device points. chihuahua's verified tree ([Block 169] §169.1):
```
/Services/ChiDashboardService            station service
  └─ Planta-N (BPlanta)                  site/zone container
       └─ {UpMonitor|CarcamoMonitor|…}   per equipment-type monitor
            └─ {BChiUp|BChiCarcamo|…}     ONE component per physical unit (77 BChiUp instances)
```
The **`equip` marker** (Haystack/Niagara tag) declares the equipment boundary: `BEquipRelation`/
`BContainmentRelation` walk the ancestor tree to the component carrying `n:equip`/`hs:equip` — THAT component is
the equipment ([Block 264] §264.5b, [Block 21] §21.2). Tag your equipment-folder component `equip` so semantic
queries and `equipRef` resolution find it.

## 717.4 — The official placement guidance (REFINES "a separate central folder")

[CERT-doc] Tridium's guidance on WHERE the kitControl blocks go is **Philosophy B: co-locate the control blocks
NEAR the points they serve** (distributed, under/beside the device's equipment area) — NOT in a distant central
`Logic` folder. A central folder (Philosophy A) "breeds off-view knobs and harder-to-follow logic" because links
span remote folders ([Block 538] BP5, `WhereToLocateKitControlComponents…txt:18`).

**Refinement of the SO1 framing:** "keep the raw PROXY POINTS points-only under the device" (SO1) still holds —
but the CONTROL LOGIC is not banished to a far central folder; it is grouped **per equipment, close to its
points**. So the two-layer model is: (1) proxy points = the device's raw IO; (2) equipment logic = a per-equipment
component/area co-located with those points (not a monolithic central Logic tree). The equipment component links
to the proxy points; it does not live INSIDE the raw `points/` container, but near it, per equipment.

## 717.5 — Why separate logic from the raw points

[CERT+INFER]
1. **Portability / re-addressing** — a device address change touches only the ProxyExt, not the control wiring
   ([Block 24]/[Block 538] BP5); chihuahua v2 moved logic /Drivers→/Services without changing the logic ([Block 165]).
2. **Legibility** — distributed-near-points keeps the wire readable ([Block 538] BP5).
3. **RBAC** — application-layer components are permission-gated independently (`OPERATOR_WRITE` at the app layer,
   [Block 648]); a read-only integrator sees points without control-write.
4. **Reuse** — one compiled equipment TYPE instantiated N times (77 `BChiUp`, [Block 169]); templates do the same
   graphically (SO5).

## Connections

- Equipment model → focus `chihuahua` [Block 163]–[Block 177]/[Block 648]–[Block 655]; kitControl → focus
  `kitControl` [Block 537]/[Block 538]/[Block 545]; BProgram → [Block 541]; control model → [Block 6]; equip tag →
  focus `tags` [Block 21]/[Block 264]; BEquipment → [Block 422]. Building custom modules → focuses A/B
  [Block 705]–[Block 715]. Driver/points → [Block 716] (SO1). Deliverable: `docs/station-organization.md` §2.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | logic in app space (/Config or /Services), not /Drivers; chihuahua v2 moved it | [CERT] | [Block 169]/[Block 165] | cited |
| 2 | 3 authoring methods (kitControl / BProgram / custom module) + when | [CERT] | [Block 6]/[Block 541]/[Block 422] | cited |
| 3 | equipment-grouping: one component per equipment; equip tag = boundary | [CERT] | [Block 169]/[Block 264] | cited |
| 4 | official Philosophy B: blocks NEAR points, not a central Logic folder | [CERT-doc] | [Block 538] BP5 | cited (refines SO1) |
| 5 | separation rationale (portability/legibility/RBAC/reuse) | [CERT]+[INFER] | [Block 538]/[Block 648]/[Block 169] | reasoned |

**Tally:** [CERT/CERT-doc] ×4 · [INFER] ×1. Block TYPE = **DESIGN/HOW-TO** — ratio healthy. Re-cites verified
blocks. NOTE: corrects this focus's bootstrap remittance — `kitControl` is DONE ([Block 537]/[Block 538]/
[Block 545]), not planned.

## Open gaps (this focus)

SO2 CLOSED. Next: **SO3** (linking points ↔ logic — Niagara links, priority array, decoupling logic from device
addressing). Then SO4 (nav: hierarchy+tags), SO5 (reuse + synthesis).
