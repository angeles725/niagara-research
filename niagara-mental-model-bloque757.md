# B757 · Station integration — authoring a `BAbstractService` and controlling the nav tree (BINavNode), code-grounded

> **Scope**: how a module plugs into a running station beyond a plain component — the Service contract and the
> nav-tree contract. Grounded in our own `BDashboardService`. Foco: **module-authoring** (MA4).
>
> **Sources**: FUENTE 3 — `organized/baja/baja/decompiled/javax/baja/sys/{BIService,BAbstractService,Sys,BStation}.java`,
> `.../util/BServiceContainer.java`, `.../nav/BINavNode.java`, `.../sys/BComponent.java`,
> `com/tridium/sys/service/ServiceManager.java`, `com/tridium/sys/module/BModulePaletteNode.java`; our
> `BDashboardService.java`. FUENTE 1 — B20 (services/app overview), B634 (palette nav reader), B751/B752 (agents/views).

---

## 757.1 — Authoring a `BAbstractService` `[CERT]`
`BAbstractService extends BComponent implements BIService, BIStatus, BILicensed` and adds frozen `status`,
`faultCause`, `enabled` (`BAbstractService.java:35-42`). To author one:
1. `class BFoo extends BAbstractService` + `@NiagaraType`.
2. **Implement `getServiceTypes(){ return new Type[]{ TYPE }; }`** — it stays abstract from `BIService`
   (`BIService.java:22`), so it is MANDATORY. It is the set of type keys the service registers under.
3. Optionally override `serviceStarted()` (a no-op in the base, `:79-85`, called by `ServiceManager` after
   `fw(15)` which runs `checkLicense`+`updateStatus`) for init work. Fault helpers: `configOk/configFail/
   configFatal`. License-gate by overriding `getLicenseFeature()` (default null).

**Registration is by PLACEMENT, not annotation** `[CERT]`: `ServiceManager.register` only registers a component
whose `getComponentSpace() == Station.space` (`ServiceManager.java:100`) — i.e. it must be DROPPED into the
running station, conventionally under the frozen `/Services` node (`BStation.Services`, a `BServiceContainer`,
`BStation.java:39`). `BServiceContainer` is itself restricted to live directly under the station
(`isParentAncestryLegal:77-85`). Then it is indexed by each `type.toString()` in the `byKey` map.

## 757.2 — Service lookup & the duplicate rule `[CERT]`
Resolve a service anywhere via `Sys.getService(BFoo.TYPE)` (`Sys.java:160`) →
`ServiceManager.getService(typeSpec)` returns the **FIRST-registered** match (`:64-71`) or throws
`ServiceNotFoundException`. `register` does NOT reject duplicates — it grows the array, but `getService` only
ever returns index `[0]` (use `getServices` for all). A `BIRestrictedComponent` singleton service enforces
uniqueness: a duplicate is logged and REMOVED at start (`startService:281-297`). So: for a station-wide
singleton resolvable by type, author a Service; for a per-location thing resolved by ORD/slot path, a plain
`BComponent` is right (our `BRoomPanel` is a plain child, not a service).

## 757.3 — The nav tree is FREE for any component `[CERT]`
`BComponent` already implements the whole `BINavNode` contract (`BComponent.java:386-480`), so ANY component
appears in the Workbench nav with no extra code:
- `getNavName()` → slot name; `getNavDisplayName(cx)` → display name; `getNavIcon()` → `getIcon()`;
  `getNavOrd()` builds the ORD from the slot path automatically (`:461-475`).
- `getNavChildren()` (`:436-449`) is a FILTERED view of the property children: a child shows iff it is NOT
  `Flags.HIDDEN` **and** `kid.isNavChild()` is true (`isNavChild()` defaults true, `:451-453`).
- **The two levers**: hide a child from nav while keeping the slot → override `isNavChild()→false` (or set
  HIDDEN to remove it from ALL UI); reorder nav → reorder/rename the slots (nav order follows slot order).

Our `BDashboardService`/`BRoomPanel` override NONE of these — they appear purely via the defaults (the service
because it is a child under `/Services`; the `CuartoN` because they are non-hidden component slots). **Minimal
recipe: be a properly-named, non-hidden slot.**

## 757.4 — Injecting a VIRTUAL nav node (no backing slot) `[CERT]`
When you need a nav node that is NOT a real slot, the recipe (canonical example `BModulePaletteNode`, B634):
implement `BINodeNode`/subclass a space, hardcode `getNavName`/`getNavDisplayName`/`getNavIcon`, point
`getNavParent` at the host node, supply a stable `getNavOrd`, and have the parent's `getNavChildren` return
your synthetic instance (`BModulePaletteNode.java:121-158` splices "module.palette" under the module node).

## 757.5 — The default view + right-click menu come from the AGENT registry `[CERT]`
A nav node's default view and menu are NOT from the nav methods — they come from
`Registry.getAgents(TypeInfo)` keyed on the node's TYPE (`Registry.java:48`), populated by `@AgentOn`
declarations (B751/B752). To give a component a bespoke default view, author a view class
`@AgentOn(types="YourModule:YourType")`. Our components declare none → they get the framework defaults
(property sheet, slot sheet) + standard actions.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BAbstractService extends BComponent impl BIService/BIStatus/BILicensed; must implement getServiceTypes() | [CERT] | BAbstractService.java:35-42; BIService.java:22 |
| 2 | Registration by placement under /Services (Station.space); indexed by type; not by annotation | [CERT] | ServiceManager.java:100; BStation.java:39; BServiceContainer:77-85 |
| 3 | Sys.getService returns first-registered; duplicates grow array; restricted singleton removes dup | [CERT] | Sys.java:160; ServiceManager:64-71,281-297 |
| 4 | Any BComponent is a BINavNode; getNavChildren filters by !hidden && isNavChild; override isNavChild to hide | [CERT] | BComponent.java:386-480 |
| 5 | Virtual nav node recipe = BModulePaletteNode (hardcode nav methods, parent's getNavChildren returns it) | [CERT] | BModulePaletteNode.java:121-158 |
| 6 | Default view/menu from Registry.getAgents keyed on Type (@AgentOn); ours = framework defaults | [CERT] | Registry.java:48; our modules (zero @AgentOn on rt types) |

**Tally**: 6 [CERT]. No unmarked claims.

## Connections
- **B20** (services/app), **B634** (palette nav), **B751**/**B752** (agents/views), **B5** (ORD/nav ords),
  **B759** (our-modules audit — our service/nav is already minimal-correct).

## Open gaps
- **B757-G1**: `completesStarted()`/`whenServiceStarted()` deferred-start hooks — named, not exercised (we
  don't need async service start).
