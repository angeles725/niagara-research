# Block 589 — Permission enforcement in the nav tree: each entity's applied `BCategoryMask` is baked onto its `BLevelElem` and OR-propagated UP to group ancestors, then read-time `fw(1302)` filters children per-user by `hasOperatorRead` — category-visibility (AC3), orthogonal to the role-hierarchy scoping (B565)

**Session**: 2026-08-28
**Focus**: `hierarchy` (gap H6 — how RBAC is enforced within the custom tree)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `HierarchyCacheBuilder.setElemAndAncestorPermissions` +
`BLevelElem.fw(1302)`; the mask propagation and the per-user filter token-verified inline.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/com/tridium/hierarchy/HierarchyCacheBuilder.java:592-599`.
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/BLevelElem.java:379-381`.
- `[CERT-doc]` `niagara-help/guides-clean/Hierarchies/Permissions-2D02C1AF.txt`.

**Scope**: RBAC inside a hierarchy. Ties the category model ([Block 561] AC3) and the role seam ([Block 565] AC7)
to the nav tree. Does NOT re-open either — applies them.

---

## 589.1 Build time: bake the entity mask, propagate up [CERT]

During cache build ([Block 585]), `setElemAndAncestorPermissions(elem, entity)` `[CERT] :592-599` runs per
entity:
```java
BCategoryMask entityMask = BCategoryMask.NULL;
if (entity instanceof BComponent) entityMask = ((BComponent) entity).getAppliedCategoryMask();  // AC3 applied mask
elem.setCategoryMask(entityMask, null);
BINavNode parent = elem.getNavParent();
// ...walk up, OR-ing the mask onto each grouping ancestor...
```
Each leaf element gets the entity's **applied category mask** (the ORD-prefix-inherited category from [Block 561]
§561.2), and the mask is OR-propagated UP through the grouping ancestors. So a GROUP node's mask becomes the
UNION of its descendant entities' categories — **a group is potentially visible to a user if they can see ANY
entity within it**.

## 589.2 Read time: per-user filter by operator-read [CERT]

`BLevelElem.fw(1302)` (getServerCacheChildren, the cached-read path [Block 585]/[Block 588]) `[CERT] :379-381`:
```java
BUser user = HierarchyUtil.getUser();
if (user != null && !user.getPermissions().isSuperUser())
   return serverCacheChildren.stream()
       .filter(elem -> elem.getPermissions(user).hasOperatorRead())
       .toArray(BLevelElem[]::new);
```
So the cache stores the FULL tree once (with baked masks), and each read is FILTERED for the requesting user: a
non-super-user sees only the child nodes on which they have `hasOperatorRead` (evaluated from the element's
category mask against the user's category permissions, [Block 561]); a **super-user sees everything** (the filter
is skipped). This is the same category-visibility model as the component tree, applied to virtual nodes.

## 589.3 Two orthogonal RBAC filters [CERT-synthesis]

Hierarchy access is gated by TWO independent mechanisms:
1. **Role-hierarchy scoping** ([Block 565] AC7): `BRoleHierarchies` tags a role with WHICH hierarchies it may see
   at all — gate on the hierarchy as a whole.
2. **Category visibility** (this block, [Block 561] AC3): within a hierarchy the user is allowed to see, WHICH
   NODES appear is gated by category masks + `hasOperatorRead`.

They compose: a role decides you can open the "By Floor" hierarchy; categories decide you see Floor 3 and its
AHU but not the tenant's private meters. Neither subsumes the other.

## 589.4 A visibility subtlety [CERT]

Because the group mask is the UNION of descendants (§589.1), a group folder is visible if the user can see AT
LEAST ONE child. So a user with read on a single point on Floor 3 will see the **"Floor 3" group node** (its
name is a tag VALUE, e.g. the floor number) even though the other points under it are filtered out at read
(§589.2). The group STRUCTURE (the set of tag values that formed the levels) can therefore be partially exposed
as metadata, while the underlying point DATA stays category-protected. This is a deliberate consequence of
building navigation from tags — the tag values are the tree, and a visible leaf implies a visible path to it.
Operators who treat group names (floor numbers, equipment types, tenant names) as sensitive should category-gate
accordingly, because the nav tree surfaces them wherever any descendant is readable.

## 589.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | setElemAndAncestorPermissions sets elem mask = entity.getAppliedCategoryMask() (AC3) and OR-propagates up nav parents | [CERT] | HierarchyCacheBuilder.java:592-599 | token-checked ✓ |
| 2 | Group node mask = union of descendant entity categories (visible if any child is) | [CERT] | :592-599 (up-walk) | token-checked ✓ |
| 3 | fw(1302) read filters children per-user: non-super-user → only hasOperatorRead nodes; super-user sees all | [CERT] | BLevelElem.java:379-381 | token-checked ✓ |
| 4 | Two orthogonal filters: role-hierarchy scoping (B565) gates the hierarchy; category masks (B561) gate nodes | [CERT-synthesis] | rows 1-3 + [B565]/[B561] | reasoned ✓ |
| 5 | Group name (tag value) can be exposed as metadata when any descendant is readable | [CERT] | rows 1-3 | reasoned ✓ |

**Marker tally**: [CERT] ×3 · [CERT-doc] ×1 (Permissions guide) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation + doc). 3 of 5 rows token-verified inline.

## Connections

- **[Block 561]** (AC3) — the category model + `getAppliedCategoryMask` (ORD-prefix inheritance) baked here.
- **[Block 565]** (AC7) — the ROLE-hierarchy scoping; the orthogonal filter (which hierarchies vs which nodes).
- **[Block 585]** (H2) — the cache build where the mask is baked; **[Block 588]** (H5) — the fw(1302) read path
  this filters.
- **[Block 30]** — slot-level enforcement; the same `hasOperatorRead` gate, applied to virtual nodes.

## Open gaps (this block)

- The on-demand (non-cached) path's per-user filtering (does the re-query apply category filtering the same way?)
  is implied by the shared `getPermissions(user)` model but not separately traced — low value. Focus continues at
  H7 (transport: BOX + Fox channels), the final gap.
