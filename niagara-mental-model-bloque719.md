# B719 — Station organization, navigation & grouping (SO4): tag once, navigate many ways — hierarchies + tags instead of duplicating the physical tree

> Focus: **station-organization** · Gap **SO4** (navigation & grouping). Block TYPE = **DESIGN/HOW-TO**. Feeds
> `docs/station-organization.md` §4. Marker `[CERT]` where re-citing verified blocks; `[INFER]` for framing.

## 719.1 — The problem: one physical tree, many desired views

[INFER] The physical component tree (`/Drivers/...` for IO, `/Config` or `/Services/...` for logic) is organized
for ENGINEERING, not for operators. An operator wants to browse **by equipment** ("show me all rooftop units") or
**by location** ("everything in Building A, Floor 2"). The wrong fix is to DUPLICATE the tree into a parallel
folder structure — that creates drift and doubles the maintenance. The right fix: **tag the components once, then
build alternate navigation views by tag/relation** without moving anything.

## 719.2 — Tags: the semantic layer (`tags` focus)

[CERT] The tag subsystem ([Block 260]–[Block 270]) attaches semantic markers/values to components independent of
their tree position:
- **Marker tags** — `equip` (this is a piece of equipment, [Block 264]), `point`, `hvac`, etc.
- **Value tags** — `geoCity`, `geoFloor`, `dis`, a custom `siteId`, etc.
- **Relations** — `equipRef`/`childPoint`/`parentDevice` connect a point to its equipment and vice versa
  ([Block 264] §264.5).
- The Niagara/Haystack **dictionaries** define the standard tag vocabulary; `neqlize` normalizes queries
  ([Block 690] on the JACE's stock dictionary).

**Tag your equipment component `equip`**, tag points with their role + geo/location values, and the semantic layer
is in place — queries and views resolve by tag, not by path.

## 719.3 — Hierarchies: alternate navigation trees (`hierarchy` focus)

[CERT] The `hierarchy` subsystem ([Block 584]–[Block 590]) builds ALTERNATE navigation trees from a sequence of
**level definitions** (Group / List / Query / Relation) over the SAME components — WITHOUT moving them:
- A hierarchy is defined by an ordered list of level-defs; each level groups/queries the components by a tag or a
  relation ([Block 584]).
- It is **on-demand and stateless** — the tree is computed per navigation from `contextParams`, not persisted, so
  it never drifts from the underlying components ([Block 588]/[Block 589]).
- Example: a "by location → by equipment-type" hierarchy = level 1 group by `geoBuilding`, level 2 group by
  `equipType`, leaf = the `equip` components. The same points appear under a "by network" hierarchy too — one set
  of components, several navigable trees.

Permissions on the hierarchy tree are the component categories (orthogonal to the hierarchy shape, [Block 589]) —
so a navigation view never bypasses RBAC.

## 719.4 — The pattern: tag once, navigate many ways

[INFER] The maintainable structure:
1. **Physical tree** stays engineering-shaped: points under devices (SO1), logic per equipment near its points
   (SO2).
2. **Tag** each equipment `equip` + geo/role/value tags; wire `equipRef` relations point→equipment.
3. **Hierarchies** provide the operator-facing views (by equipment, by location, by system) computed from those
   tags — no duplicate folders.
4. **Nav files / PX views** bind to the hierarchy or to tag queries, so a dashboard shows "this equipment's
   points" resolved by relation, not by a hardcoded ORD (which is also what keeps links stable, SO3).

This is why the point stays under its device and the logic stays per-equipment: the OPERATOR view is a
projection (hierarchy) over the tags, not a third physical copy.

## 719.5 — Applied to the operator's case

[INFER] For the TC500 + IO-R-34:
- Tag the TC500 zone equipment `equip` + `geoFloor`/`geoRoom`; tag the IO-R-34-driven equipment likewise.
- The points keep `equipRef` back to their equipment (learn/discovery can auto-tag, or tag in bulk).
- Build one hierarchy "by floor → by equipment" — the operator browses zones without you moving a single point
  or logic component out of its engineering location.

## Connections

- Tag subsystem → focus `tags` [Block 260]–[Block 270]/[Block 264]; JACE stock dictionary → [Block 690]. Hierarchy
  engine → focus `hierarchy` [Block 584]–[Block 590]. equip boundary → [Block 264]. Points/logic/links → [Block 716]/
  [Block 717]/[Block 718] (SO1-3). Deliverable: `docs/station-organization.md` §4.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | tags attach semantics independent of tree position; equip/equipRef | [CERT] | [Block 264] | cited |
| 2 | hierarchy = alternate nav trees by level-defs, on-demand/stateless | [CERT] | [Block 584]/[Block 588] | cited |
| 3 | hierarchy permissions = component categories (RBAC preserved) | [CERT] | [Block 589] | cited |
| 4 | tag-once-navigate-many pattern (no duplicate tree) | [INFER] | 719.4 | reasoned |
| 5 | applied to TC500/IO-R-34 | [INFER] | 719.5 | reasoned |

**Tally:** [CERT] ×3 · [INFER] ×2. Block TYPE = **DESIGN/HOW-TO** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

SO4 CLOSED. Next: **SO5** (reuse — equipment templates + provisioning — + the focus SYNTHESIS and deliverable
finalization) — focus-closing block.
