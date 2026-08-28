# Block 587 — The `hierarchy:` ORD scheme: a `BSpaceScheme` whose body is a slash-path (`/Hierarchy/seg/seg…`) resolved level-by-level — a grouping segment resolves to a `BLevelElem`, an escaped `station:|` leaf resolves to the REAL component — dispatched local or cross-station

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H4 — the `hierarchy:` ORD scheme resolution)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BHierarchyScheme` + `HierarchyQuery`; the parse/resolve dispatch
and the entity-vs-grouping leaf test token-verified inline.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/{BHierarchyScheme,HierarchyQuery}.java`.

**Scope**: how a `hierarchy:` ORD dereferences into the custom tree and ultimately to a real component. [B5
§5.1.2] noted the scheme exists in the ORD table; H4 opens the resolution. Uses the level model ([Block 584]) and
the scope dispatch ([Block 586]). Does NOT re-open ORD/SlotPath basics ([Block 5]).

---

## 587.1 `hierarchy:` is a SPACE scheme over a slash-path [CERT]

`BHierarchyScheme extends BSpaceScheme`, `ordScheme = "hierarchy"` `[CERT] :27-30`. It is a SPACE scheme (not a
bare ORD scheme) — it resolves into the hierarchy nav `BSpace`. `parse(queryBody)` `[CERT] :47-48` returns a
`HierarchyQuery`, and `HierarchyQuery extends SlotPath` `[CERT] :8` — so a hierarchy ORD body is a **slash-path
of names**, exactly like a component slot path:
```
hierarchy:/<HierarchyName>/<segment>/<segment>/…
```
`hierarchyNames[0]` is the hierarchy name; the following segments walk down the level tree.

## 587.2 Resolution walks the segments, dispatching local or remote [CERT]

`resolve(base, query, space)` `[CERT] :51-124` walks `hierarchyNames`. The first name selects the hierarchy
(unresolved → `UnresolvedException("Cannot resolve hierarchy elem " + hierarchyNames[0])` `[CERT] :71`). At each
step it branches on the space type — `hierarchySpace instanceof BFoxHierarchySpace` `[CERT] :62,76,97,116` —
so a hierarchy ORD can resolve against the LOCAL `BHierarchyService` OR a **remote station's** hierarchy space
(proxied through `BFoxHierarchySpace`, [Block 586] cross-station scopes, H7 transport). The same ORD form works
whether the hierarchy lives here or on a subordinate.

## 587.3 The leaf: grouping node vs REAL component [CERT]

The interesting decision is the LAST segment `[CERT] :86-106`:
- If `hierarchyNames[hierarchyNames.length - 1].startsWith("station$3a$7c")` `[CERT] :86` — the escaped form of
  `station:|` (`$3a`=`:`, `$7c`=`|`) — the leaf is an **ENTITY**: an actual station component ORD embedded in
  the hierarchy path. It resolves to the REAL component:
  `BOrd.make(SlotPath.unescape(lastName)).resolve(base.get(), user)` `[CERT] :97-102` (permission-checked via
  the `user` context).
- Otherwise the last segment is a **GROUPING** name → it resolves to a `BLevelElem` group node through the
  service.

So a hierarchy ORD is a **stable address into the custom tree whose leaf dereferences to the actual point/device**.
`hierarchy:/ByFloor/Floor1/AHU1/station:|slot:/Drivers/…/DischargeTemp` (escaped) resolves to the real
`DischargeTemp` point — but ADDRESSED by the custom "By Floor" tree, not the component tree. A grouping ORD
(`hierarchy:/ByFloor/Floor1`) resolves to the group folder element instead.

## 587.4 Why this matters [CERT-synthesis]

The scheme is what makes a hierarchy USABLE as an addressing space, not just a view. Because a `hierarchy:` ORD
parses as a SlotPath and its leaf dereferences to the real component (permission-checked, local or remote), any
Niagara construct that takes an ORD — a PX widget binding, a link, a nav bookmark, a report source — can point
INTO the custom tree. The tree from [Block 584] plus this scheme means an engineer can build a "By Floor / By
System" navigation and bind graphics to `hierarchy:` paths that survive component-tree reorganization, as long as
the tags/relations that define the levels still hold. The escaped-`station:|` leaf is the seam that ties the
virtual tree back to the concrete station.

## 587.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BHierarchyScheme extends BSpaceScheme, ordScheme="hierarchy"; parse → HierarchyQuery (extends SlotPath) | [CERT] | BHierarchyScheme.java:27-48; HierarchyQuery.java:8 | token-checked ✓ |
| 2 | resolve walks hierarchyNames; name[0] = hierarchy, unresolved → UnresolvedException | [CERT] | BHierarchyScheme.java:51-71 | token-checked ✓ |
| 3 | Dispatches local BHierarchyService vs remote BFoxHierarchySpace at each step | [CERT] | :62,76,97,116 | token-checked ✓ |
| 4 | Last segment starting "station$3a$7c" (escaped station:|) = ENTITY leaf → resolves to real component with user context | [CERT] | :86,97-102 | token-checked ✓ |
| 5 | Otherwise last segment = GROUPING name → resolves to a BLevelElem group node | [CERT] | :106-124 | token-checked ✓ |
| 6 | A hierarchy: ORD is a stable address whose leaf dereferences to the real component | [CERT-synthesis] | rows 1-5 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 584]** (H1) — the level tree these segments walk; **[Block 586]** (H3) — local vs cross-station scopes,
  mirrored by the local/Fox dispatch here.
- **[Block 5]** — ORD/SlotPath/space schemes (the foundation `hierarchy:` extends).
- **H7** (this focus) — `BFoxHierarchySpace`/`BFoxHierarchyChannel`, the remote-station transport this dispatches to.
- **[Block 22]/[Block 194]** — PX bindings that can target a `hierarchy:` ORD.

## Open gaps (this block)

- The exact per-segment element lookup (how an intermediate grouping segment maps to a cached/on-demand
  `BLevelElem`) is the fw(1302)/getChildElems path — H5/H6 territory. Focus continues at H5 (on-demand tree gen +
  contextParams).
