# Block 565 — `BRoleHierarchies`: the mixin that tags every role with a set of custom-hierarchy names — the seam where RBAC roles scope which navigation hierarchies a user sees

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC7 — the `BRoleHierarchies` mixin; the RBAC↔hierarchy seam never documented in
any RBAC or hierarchy block)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the mixin class + a consumer sweep. Small, single-class gap.
**Primary sources** `[CERT]`:
- `organized/hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/BRoleHierarchies.java` (175 lines).
- consumers `organized/hierarchy/hierarchy-rt/.../BHierarchyService.java`, `HierarchyUtil.java`;
  ux `BRoleHierarchiesEditor`/`BRoleHierarchiesCompactEditor`; wb `BRoleHierarchiesMgrAgent`.

**Scope**: how a ROLE connects to the custom-hierarchy (navigation) subsystem. This is the one RBAC surface that
lives in `hierarchy-rt`, not `baja`. Does NOT open the hierarchy subsystem itself (BHierarchy/BHierarchyService
definition — hierarchy focus territory) — only the role seam.

---

## 565.1 A `BIMixIn` agent-attached to every role [CERT]

`public class BRoleHierarchies extends BComponent implements BIMixIn` `[CERT] :30`, declared
`@NiagaraType(agent = @AgentOn(types = {"baja:IRole"}))` `[CERT] :19-23`. Because it is a `BIMixIn` agent on
`baja:IRole`, the framework **auto-attaches one `BRoleHierarchies` to every role** in the station (the same
agent-mixin pattern seen elsewhere: the type declares it applies to `IRole`, and the role gains it without the
role class knowing). `getRole()` returns the host `BIRole` `[CERT] :75`.

## 565.2 It stores one thing: a delimited set of hierarchy names [CERT]

The mixin carries a single property `hierarchyNames` (`String`, `defaultValue = ""`, `flags = 5`,
`NAMES_DELIMITER = ","`) `[CERT] :24-34`. Access is set-oriented `[CERT] :36-83`:
- `getHierarchyNames()` / `setHierarchyNames(String)` — the raw comma-delimited string.
- `getHierarchySet()` → `splitHierarchies(...)` → a `Set<String>` `[CERT] :79-80`.
- `hasHierarchy(BHierarchy)` `[CERT] :83` — membership test.
- constructor `BRoleHierarchies(BHierarchy[])` `[CERT] :51-56` builds the name set from hierarchy objects.

So the entire payload is: *which named hierarchies does this role belong to.* `flags = 5` plus a dedicated
manager agent (`BRoleHierarchiesMgrAgent`, wb) `[INFER]` means it is edited through its own manager, not the
raw property sheet.

## 565.3 The seam: roles scope custom navigation [CERT]

Consumers `[CERT]` (sweep): `BHierarchyService` and `HierarchyUtil` (rt) read the role's hierarchy set; the ux/wb
editors (`BRoleHierarchiesEditor`, `BRoleHierarchiesCompactEditor`, the manager agent) let an engineer assign
hierarchies to a role. The mechanism: **custom hierarchies (the alternative navigation trees the `hierarchy`
module builds) are scoped by ROLE** — a user, through the roles they hold, is associated with a set of
hierarchy names, and the hierarchy service uses that to decide which custom nav trees that user participates in.
This is distinct from category-based *visibility* ([Block 561]): categories gate whether you can see a
component; role-hierarchies gate which *navigation structure* is presented. Together they are two orthogonal
RBAC-driven filters — one on component access, one on navigation shape.

## 565.4 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BRoleHierarchies is a BIMixIn @AgentOn baja:IRole → auto-attached to every role | [CERT] | BRoleHierarchies.java:19-30 | token-checked ✓ |
| 2 | Single property hierarchyNames (String, "," delimiter, flags=5); set-oriented API (getHierarchySet/hasHierarchy) | [CERT] | :24-34,79-83 | token-checked ✓ |
| 3 | getRole() returns host BIRole; constructor builds name set from BHierarchy[] | [CERT] | :51-56,75 | token-checked ✓ |
| 4 | Consumers BHierarchyService/HierarchyUtil (rt) + ux/wb editors + manager agent | [CERT] | consumer sweep (paths) | grep-confirmed ✓ |
| 5 | Seam = roles scope which custom nav hierarchies a user participates in (orthogonal to category visibility) | [CERT-synthesis] | rows 1-4 + [B561] | reasoned ✓ |
| 6 | flags=5 + dedicated manager ⇒ edited via manager, not raw sheet | [INFER] | :31 + BRoleHierarchiesMgrAgent | reasoned |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×1. Block TYPE = EVIDENCE (decompilation). 4 of 6
rows token-verified inline.

## Connections

- **[Block 11]** — the role model; this mixin extends a role with hierarchy membership.
- **[Block 561]** (AC3) — category-based visibility; the orthogonal RBAC filter (access vs navigation).
- **hierarchy subsystem** — `BHierarchy`/`BHierarchyService` (not opened here) is the custom-nav engine this seam
  feeds; a dedicated `hierarchy` focus would open it.

## Open gaps (this block)

- The hierarchy subsystem proper (`BHierarchy`, level definitions, NEQL-driven hierarchy generation) is NOT
  opened — it is its own subsystem (candidate future focus), out of the RBAC angle. Focus continues at AC8
  (UserMonitor + BUserEvent), the final access-control gap.
