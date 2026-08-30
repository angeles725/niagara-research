# B718 — Station organization, linking points ↔ logic (SO3): how the equipment layer connects to the device points, the priority array, and keeping the link stable across re-addressing

> Focus: **station-organization** · Gap **SO3** (linking points ↔ logic). Block TYPE = **DESIGN/HOW-TO**.
> Feeds `docs/station-organization.md` §3. Marker `[CERT]` where re-citing verified code; `[INFER]` for framing.

## 718.1 — The link is the bridge between the two layers

[CERT] A **`BLink`** (`javax.baja.sys.BLink`) connects a SOURCE slot to a TARGET slot: when the source changes,
its `changed()` callback propagates through the knobs to the links, firing the target ([Block 6] §6.1). This is
the mechanism that connects the equipment logic (SO2) to the raw device points (SO1) WITHOUT the logic living
inside the points. The logic reads an input point and writes an output point purely through links.

- **Reading an input:** link `Device/points/zoneTemp.out` → your logic block's input.
- **Writing an output:** link your logic's output → `Device/points/fanCmd` (a writable point).

[CERT] Two caveats from the engine model ([Block 6] §6.1.6): Niagara does **NOT** compute a topological sort of
links — direct links propagate FIFO/depth-first on the call stack; indirect (ORD) links load in BOG order. So a
fast link loop can storm; keep link chains acyclic and let writable points' priority array absorb multi-source
writes.

## 718.2 — The priority array (how multiple sources write one output safely)

[CERT] A writable point (`BNumericWritable`/`BBooleanWritable`) has a **16-level priority array** (`in1`..`in16`)
+ a `fallback` ([Block 6]/[Block 716]). The active level = the lowest-numbered NON-null input; the proxyExt writes
THAT to the device. This is how several logic sources can command one output without fighting:
- link your control logic to a mid priority (e.g. `in16` = default program level),
- leave higher levels (`in1`..`in8`) for overrides (manual, emergency, BACnet command priority),
- `fallback` is used when all levels are null.

For a BACnet writable object, the proxyExt maps the N4 level to the BACnet command priority directly
([Block 544]). **Rule:** write control logic at a documented priority level; don't hardcode a single source.

## 718.3 — Authoring the links: the two tools

[CERT] chihuahua ships BOTH sides of the link lifecycle ([Block 654]/[Block 650]):
- **`BBatchLinkEditor`** (wb, design-time): an engineer bulk-creates links in Workbench — accumulate From/To,
  dry-run `checkLink`, then `target.makeLink` + `target.add(slotName, link, tx)` with **one Transaction per
  component space** ([Block 654]). This is how you WIRE the equipment logic to the points at commissioning.
- **`ChiLinkHelper`** (rt, runtime): backs up / restores the module's links **by handle** so they survive a
  save/transfer/re-provision ([Block 650]). Kept SEPARATE from the wb tool (no shared code, different profiles).

## 718.4 — Decoupling: link so a re-address doesn't rewire logic

[CERT+INFER] The point of linking (vs embedding the address in the logic) is that a device re-address touches
only the **proxyExt** on the point, not the link or the logic ([Block 538] BP5, [Block 716]). Two robustness
patterns:
- **Link by a stable handle**, not a fragile display path — Niagara links carry a component handle; chihuahua's
  `ChiLinkHelper` backs up/restores by handle so a rename/move doesn't break the wire ([Block 650]).
- **Reference by tag/relation where possible** — resolve the target point via the `equip`/`equipRef` relation or
  a tag query rather than a hardcoded ORD (SO4), so the logic finds "this equipment's zone-temp point" without a
  literal path. chihuahua reconstructs point ORDs programmatically from the equipment position ([Block 169]).

## 718.5 — The end-to-end picture (SO1+SO2+SO3)

[INFER] Putting the three together for the operator's TC500/IO-R-34 case:
1. **Points** (SO1): `/Drivers/BacnetNetwork/TC500/points/…` and `/Drivers/NrioNetwork/io34/points/…` — raw IO,
   points-only, created by discovery/learn.
2. **Logic** (SO2): a per-equipment component/area (near the points, Philosophy B), tagged `equip`, holding the
   kitControl/Program/custom logic.
3. **Links** (SO3): BLinks connect the equipment logic to the points — reading inputs, writing outputs at a
   priority level. Wire them with the batch link editor; keep them handle/tag-stable so re-addressing the TC500
   or the IO-R-34 doesn't touch the logic.

## Connections

- Link/engine model → [Block 6]; priority array → [Block 6]/[Block 716]; BACnet command priority → [Block 544];
  link authoring/backup → [Block 654]/[Block 650]; ORD reconstruction from equipment position → [Block 169].
  Driver/points → [Block 716] (SO1); equipment layer → [Block 717] (SO2). Deliverable:
  `docs/station-organization.md` §3.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | BLink bridges logic↔points; no auto topo-sort (FIFO/depth-first) | [CERT] | [Block 6] §6.1 | cited |
| 2 | writable point 16-level priority array; active=lowest non-null | [CERT] | [Block 6]/[Block 716] | cited |
| 3 | BACnet level → command priority | [CERT] | [Block 544] | cited |
| 4 | batch link editor (wire) + ChiLinkHelper (backup/restore by handle) | [CERT] | [Block 654]/[Block 650] | cited |
| 5 | link by handle/tag → re-address touches only proxyExt | [CERT]+[INFER] | [Block 538]/[Block 650]/[Block 169] | reasoned |

**Tally:** [CERT] ×4 · [INFER] ×1. Block TYPE = **DESIGN/HOW-TO** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

SO3 CLOSED. Next: **SO4** (navigation & grouping — hierarchy + tags so operator views group by equipment/location
without duplicating the physical tree). Then SO5 (reuse + synthesis + deliverable finalization).
