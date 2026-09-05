# B802 · Inter-module communication is module-AGNOSTIC within a station — links, service discovery, and subscription never check the module; the only real boundaries are the compile-time Type dependency and the fox: remote hop `[CERT]`

> **Scope**: how a component in module A talks to a component in module B. The answer (verified in code): within one
> running station, NONE of the three runtime mechanisms — `BLink`, service discovery (`Sys.getService`), or
> `Subscriber` — consults the module. The module boundary is a BUILD-TIME / classloading concept (where a type is
> defined), not a runtime routing concept. The only two true "inter" boundaries are (a) the compile-time dependency
> needed to reference another module's service `Type`, and (b) the `fox:` ORD hop to a SEPARATE station (a real JVM
> boundary). Extends [B778] (same-space services + `Subscriber.event`) with the cross-module + distributed picture.
>
> **Sources (read-only, FUENTE 3, file:line [CERT], 4.14.0.162 decompile `organized/…/vineflower`)**: `javax.baja.sys`
> `BLink`, `BRelation`, `Sys`, `Subscriber`; `com.tridium.sys.service.ServiceManager`; `com.tridium.fox.sys`
> `BFoxScheme`, `broker/BFoxComponentSpace`. FUENTE 1: [B778] (author-side services/subscription — REMITTANCE for the
> registration mechanism), [B5]/[B20]/[B573] (ORD + resolution), [B507]-[B516] (fox/box transport). All load-bearing
> cites grep-verified this session (a delegated map's file:line is a hypothesis until the method is read).

---

## 802.1 — Links: a cross-module link IS a same-module link `[CERT]`
`BLink extends BRelation`. The source is either a live reference (`this.direct`) or, across component-space distance,
the `sourceOrd` property (a `BOrd`, `BRelation.java:50`, `getSourceOrd() BRelation.java:81`); the target is always the
link's owner (`getParentComponent()`); both slot endpoints are NAME strings. `BLink.resolve()` is pure ORD traversal —
`getSourceOrd().resolve(getParentComponent()).get()` (`BLink.java:303,307`) — and `activate()` picks
`isDirect() ? this.direct : this.resolve()` then installs a `Knob` by slot name (`BLink.java:168,179`). **No step in
the chain inspects which module defined either endpoint's type.** A→B link = A→A link.

## 802.2 — Service discovery: by type-spec STRING, guarded only by station space `[CERT]`
`Sys.getService(Type)` delegates to `Nre.getServiceManager().getService(type.getTypeSpec().toString(null))`
(`Sys.java:142-143`); `findService(Type)`/`getServices` likewise (`Sys.java:146,151`). `ServiceManager` keeps
`Map<String, BComponent[]> byKey = new ConcurrentHashMap<>()` (`ServiceManager.java:40`) and answers by string key
(`byKey.get(typeSpec)` `:59`). Registration's ONLY guard is station membership, not module:
`register(S service){ if (service.getComponentSpace() == Station.space){ Type[] types = service.getServiceTypes(); …`
(`ServiceManager.java:91-93`). **So any module can obtain any other module's service by passing its `Type`.** The one
coupling is compile-time: to name `BXyzService.TYPE` you need a build `<dependency>` on that module (see [B784]) — the
runtime registry itself is module-blind. (Service TYPES are declared as a plain `<type>` in `module.xml` + a
`getServiceTypes()` returning `new Type[]{TYPE}`; the SPI-registration mechanism is [B778], REMITTANCE.)

## 802.3 — Subscription: partitioned by `BComponentSpace`, never by module `[CERT]`
`Subscriber.doSubscribe(BComponent c,…)` records the component and calls
`((ComponentSlotMap)c.getSlotMap()).subscribe(this)` (`Subscriber.java:169-172`) — it operates on the target's slot
map directly, no module check. `updateSpaceSubscription(…)` groups the batch into
`HashMap<BComponentSpace, List<BComponent>> bySpace` keyed by `comp.getComponentSpace()`
(`Subscriber.java:250-256,266`); there is even a `subscribe(BComponentSpace space, BOrd[] ords,…)` overload
(`:151`). **The partitioning unit is the station database (`BComponentSpace`), not the module.** A `Subscriber` in
module A receives `BComponentEvent`s from a component whose type lives in module B, same as B778's same-module case.

## 802.4 — The one real boundary in a station: the fox: remote hop `[CERT]`
Reaching ANOTHER station is the only place a boundary bites. A `fox://host:1911|station:|slot/path` ORD is resolved by
`BFoxScheme` (`@NiagaraType(ordScheme="fox")` `BFoxScheme.java:20`): `resolve(base, query, client)` builds
`BFoxSession.make(null, host, port, false)` then `BFoxSession.connect(session)` (a TCP connect + authentication) and
returns `new OrdTarget(base, session)` (`BFoxScheme.java:45,56,60,61`). The remote tree is then materialized as LOCAL
proxies: `BFoxComponentSpace extends BProxyComponentSpace` (`BFoxComponentSpace.java:48`) with a
`FoxSubscribeCallbacks` (`:64,345`) that tunnels subscribe/link/traverse operations over the fox wire channel. **Once
the session is up, links, subscription and ORD traversal work IDENTICALLY on the proxy `BComponent`s** — the only
explicit steps are the `fox:` ORD and the connect-time auth. This is why cross-station comms is "just" an ORD scheme.

## 802.5 — Kit implication `[INFER, grounded in §802.1-4]`
For `/build-n4-module`: to make one module cooperate with another, **do NOT invent a message bus or a registry** —
the framework already routes by ORD / Type / component:
- **Link** a source slot to a target by `BOrd` (`sourceOrd`) — cross-module is free.
- **Discover** another module's service with `Sys.getService(TheirService.TYPE)` and add the `<dependency>` in
  `module.xml` (the ONLY coupling); never scan or hardcode a lookup.
- **Subscribe** to another module's component with a `Subscriber` (edge-driven, [B801]/[B775] timer rules apply if you
  poll instead) — module-agnostic within the station.
- **Cross-station**: address the remote by a `fox:` ORD; treat the proxy as local, but budget for connect-time auth +
  latency + the session's liveness. → PROPOSED `types/logic.md` §"talking to another module": teach the module-agnostic
  rule + the two real boundaries; the anti-pattern is a bespoke inter-module channel.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BLink` source = `sourceOrd` BOrd or `direct`; target = parent; resolve() is pure ORD traversal; activate() installs a Knob by slot name — no module check | [CERT] | BRelation.java:50,81; BLink.java:168,179,303,307 |
| 2 | `Sys.getService(Type)` → `ServiceManager.byKey` (ConcurrentHashMap<String,BComponent[]>) lookup by type-spec string | [CERT] | Sys.java:142-143,146,151; ServiceManager.java:40,59 |
| 3 | `ServiceManager.register` guard is `getComponentSpace() == Station.space`, NOT module identity | [CERT] | ServiceManager.java:91-93 |
| 4 | `Subscriber.doSubscribe` calls `ComponentSlotMap.subscribe(this)`; batches grouped by `BComponentSpace`, never module | [CERT] | Subscriber.java:169-172,250-256,266 |
| 5 | Cross-station = `fox:` ORD → `BFoxScheme.resolve` → `BFoxSession.connect` → `BFoxComponentSpace` proxy (extends BProxyComponentSpace) | [CERT] | BFoxScheme.java:20,45,56,60,61; BFoxComponentSpace.java:48,64,345 |
| 6 | Only real couplings: compile-time `Type` dependency (module.xml <dependency>) + the fox: remote hop | [CERT]+[INFER] | §802.2 (Type dep) + §802.4 (fox); B784 (deps) |

**Tally**: 5 [CERT] · 1 [CERT]+[INFER]. All file:line grep-verified this session (10-cite map from a delegated sweep,
each confirmed at the enclosing method; register guard corrected to :92-93, doSubscribe to :169-172). §802.5 is [INFER].

## Connections
- **B778** (author-side service SPI + same-space `Subscriber.event` — this is its cross-module/distributed extension),
  **B5**/**B20**/**B573** (ORD + resolution — the substrate links/discovery ride on), **B507**-**B516** (fox/box
  transport + apis), **B784** (module.xml `<dependency>` — the one compile-time coupling), **B801**/**B775** (if a
  module polls another instead of subscribing, the timer rules bite). Kit: `types/logic.md` §"talking to another module".

## Open gaps
- **B802-G1** (requires-execution): the fox connect-time AUTH + session-liveness behavior (reconnect, dead-session
  detection) is named, not traced — a distributed lane ([B507]-[B516] adjacent). Bounded follow-up if a builder needs a
  cross-station module; the in-station module-agnostic contract (§802.1-3) is fully [CERT].
