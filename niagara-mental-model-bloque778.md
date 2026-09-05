# B778 · Authoring a custom SERVICE, a new ORD SCHEME, and a SERVER-SIDE subscription — the three module-author SPIs (MAE7, D4)

> **Scope**: the AUTHOR side of three inter-module-communication SPIs, walked from real Tridium exemplars, for a
> module builder: (1) register a custom service, (2) implement a new ORD scheme, (3) subscribe to component change
> traps from SERVER code inside the station. Extends the runtime-model blocks (B20/B757 registration, B408 named
> BHandleScheme, B512/B553 the BOX *client* subscription) with the concrete author-side recipe. Focus:
> `module-authoring-exemplars` (MAE7 / dimension D4). Kit destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled — `systemMonitor-rt` (`BSystemMonitorService`), `baja` (`BAbstractService`,
> `BIService`, `BOrdScheme`, `BHandleScheme`, `Subscriber`, `UserMonitor`), verified this session at
> `organized/`. FUENTE 1: B20/B757, B408, B5/B758, B512/B553. READ-ONLY. English (post-B115).

---

## 778.1 — A custom SERVICE: `extends BAbstractService` + `getServiceTypes()` + registration-by-placement `[CERT]`
A service is a `BComponent` carrying the `BIService` marker via the abstract base:
`public abstract class BAbstractService extends BComponent implements BIService, BIStatus, BILicensed`
(`BAbstractService.java:51-54`). Author recipe, from the `systemMonitor` exemplar
(`systemMonitor/systemMonitor-rt/vineflower/com/tridium/sysmon/BSystemMonitorService.java`):
- **Declare**: `public final class BSystemMonitorService extends BAbstractService implements BIRestrictedComponent`
  (:63).
- **The decisive registration hook** — override `getServiceTypes()`: `public Type[] getServiceTypes() { return new
  Type[]{ TYPE }; }` (:134-136). Contract (`BIService.java:44-52`): *"Return the types to be registered under. The
  service will be automatically registered during station bootstrap, and unregistered if unmounted… result should
  be static."* → **registration-by-placement**: dropping the component under `/Services` auto-registers it under the
  returned Type(s); there is NO manual register call. (Confirms B20/B757.)
- **Lookup side**: `Sys.getService(Type)` → `Nre.getServiceManager().getService(...)` (`baja …/Sys.java:142-143`);
  `Sys.getServices(Type)` (:150-151).
- **Lifecycle**: `serviceStarted()` (base no-op `BAbstractService.java:147-151`; overridden at
  BSystemMonitorService.java:130 to wire links — may `Sys.getService()` peers, general components not yet started)
  → then the ordinary `BComponent` `started()` / `atSteadyState()`. Framework dispatch
  `Fw.SERVICE_STARTED → fwServiceStarted() → checkLicense()+updateStatus()` (`BAbstractService.java:505-523`).
- **Optional license gate**: override `getLicenseFeature()` (base returns null = free/unlicensed,
  `BAbstractService.java:326-330`).

## 778.2 — A new ORD SCHEME: `extends BOrdScheme` (a `BSingleton`) + `@NiagaraType(ordScheme=…)` `[CERT]` (walks B408)
The ORD scheme base is a singleton, not an interface — `public abstract class BOrdScheme extends BSingleton`
(`baja/javax/baja/naming/BOrdScheme.java:28-30`); there is NO `BIOrdScheme`. Author recipe, from `BHandleScheme`
(scheme id `"h"`, `docSource-doc/vineflower/baja/javax/baja/space/BHandleScheme.java`):
- **Declare**: `@NiagaraType(ordScheme = "h")` + `@NiagaraSingleton` (:21-23), `extends BOrdScheme` (:26), a
  singleton `INSTANCE`, private ctor `super("h")` (:55). The id is set via `protected BOrdScheme(String id)`
  (lower-cased, BOrdScheme.java:82-85).
- **`resolve(OrdTarget base, OrdQuery query)`** — the abstract String/handle → target method (BOrdScheme.java:118),
  implemented at BHandleScheme.java:66-86: read `query.getBody()`, find the `BComponentSpace`, return
  `new OrdTarget(base, space.resolveByHandle(handle))`. Optionally override `parse(String queryBody)` (default
  `new BasicQuery(id, queryBody)`, BOrdScheme.java:105-108).
- **Registration — two coupled mechanisms**: (a) Slot-o-Matic emits the annotation into `module.xml` as
  `<type class="…BHandleScheme" name="HandleScheme" ordScheme="h"/>` — the same shape verified for the built-ins
  `file`/`module`/`local`/`ip`/`zip` (`baja …/META-INF/module.xml:41,89,88,86,69`); (b) runtime resolution
  `BOrdScheme.lookup(schemeId)` → `Sys.getRegistry().getOrdScheme(id).getInstance()` (BOrdScheme.java:55-58).
This fully walks B408 (which only NAMED BHandleScheme).

## 778.3 — A SERVER-SIDE subscription: subclass `Subscriber`, override `event(BComponentEvent)` `[CERT]` (fills a 0-hit gap)
The server-side change-trap listener is `javax.baja.sys.Subscriber` (abstract): *"a listener for ComponentEvents on
zero or more BComponents… subclasses override the abstract `event()` method."* (`Subscriber.java:25-36`). Author
recipe, from `UserMonitor.UserSubscriber` (`baja/javax/baja/user/UserMonitor.java`):
- **Author the listener**: `private class UserSubscriber extends Subscriber` (:147) implementing
  `public void event(BComponentEvent event)` (:152) — dispatch on `event.getId()`, read `event.getSlot()` /
  `event.getSourceComponent()`.
- **Subscribe from server code**: `subscribe(BComponent c, int depth, Context cx)` + overloads
  (`Subscriber.java:194-220`); exemplar calls `subscriber.subscribe(user, 10)` (depth 10 = descend subtree,
  UserMonitor.java:81). ORD-batch form `subscribe(BComponentSpace space, BOrd[] ords, int depth, Context cx)`
  (Subscriber.java:276-283).
- **How the trap arrives**: `doSubscribe()` registers into the component slot map and notifies the space via
  `space.getSubscribeCallbacks().subscribe(...)` (Subscriber.java:297,467 — *"if the space is remote this results in
  a network call"*). Event filter via `BComponentEventMask` (default `PROPERTY_EVENTS`).
- **Cleanup**: `unsubscribe(...)`/`unsubscribeAll()` (Subscriber.java:371-405) + `gc()` for unmounted components;
  the exemplar unsubscribes on its stop path. A separate TYPE-level path exists:
  `BComponentSpace.subscribe(Type[], TypeSubscriber)`.
This is the SERVER side (inside the station) — complementary to the BOX/BajaScript CLIENT subscription (B512/B553).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | A service = `extends BAbstractService` (a BComponent + BIService); registered by overriding `getServiceTypes()` | [CERT] | BAbstractService.java:51-54; BSystemMonitorService.java:63,134-136 |
| 2 | Registration is by-placement (auto at bootstrap when under /Services); lookup via `Sys.getService(Type)` | [CERT] | BIService.java:44-52; Sys.java:142-143 |
| 3 | `serviceStarted()` is the service-specific hook (peers resolvable, general components not yet started) | [CERT] | BAbstractService.java:147-151; BSystemMonitorService.java:130; BIService.java:54-61 |
| 4 | A new ORD scheme = `extends BOrdScheme` (a BSingleton), `@NiagaraType(ordScheme="id")`, ctor `super("id")`, override `resolve()` | [CERT] | BOrdScheme.java:28-30,82-85,118; BHandleScheme.java:21-26,55,66-86 |
| 5 | Scheme registration = Slot-o-Matic `<type … ordScheme="id"/>` in module.xml + `BOrdScheme.lookup` at runtime | [CERT] | baja module.xml:41,86,88,89; BOrdScheme.java:55-58 |
| 6 | Server-side subscription = subclass `Subscriber`, override `event(BComponentEvent)`, call `subscribe(c,depth,cx)` | [CERT] | Subscriber.java:25-36,194-220; UserMonitor.java:147,152,81 |
| 7 | The trap plumbing notifies the space (`getSubscribeCallbacks().subscribe`), remote space = network call; cleanup via unsubscribeAll | [CERT] | Subscriber.java:297,453-467,371-405 |

**Tally**: 7 [CERT], 0 [INFER]. No unmarked claims. Load-bearing cites (getServiceTypes, BHandleScheme ordScheme/resolve, module.xml registration, Subscriber.event/subscribe) grep-verified inline this session at `organized/`.

## Connections
- **B20/B757** (Sys.getService + registration-by-placement) — §778.1 confirms + adds the verbatim `getServiceTypes`
  line + the serviceStarted/started split. **B408** — §778.2 walks the BHandleScheme it only named.
- **B5/B758** (ORD/BQL) — the ORD scheme parse/resolve contract. **B512/B553** (BOX/BajaScript CLIENT subscription)
  — §778.3 is the SERVER-side complement. **B762/B763** (our -ux write-surface) — a server-side Subscriber is how a
  dashboard service could react to live slot changes without polling (future).

## Open gaps
- **MAE7-G1** — the TYPE-level server subscription (`BComponentSpace.subscribe(Type[], TypeSubscriber)`) is named
  here but not walked; a bounded follow-up if a builder needs "notify me when any instance of Type X mounts."

## Kit implication (→ `types/logic.md`)
Add an "author-side SPIs" section: (1) **service** — `extends BAbstractService`, override
`getServiceTypes(){return new Type[]{TYPE};}`, drop under `/Services` (auto-registered; hook `serviceStarted()`),
look up via `Sys.getService(Type)`; (2) **new ORD scheme** — `extends BOrdScheme` (`BSingleton`) with
`@NiagaraType(ordScheme="<id>")` + `@NiagaraSingleton`, ctor `super("<id>")`, override `resolve()` (Slot-o-Matic
emits `<type … ordScheme="<id>"/>`, registry resolves via `BOrdScheme.lookup`); (3) **server-side subscription** —
subclass `javax.baja.sys.Subscriber`, override `event(BComponentEvent)`, call `subscribe(component, depth, cx)` and
`unsubscribeAll()` on stop. All three are `[CERT]`-exemplar-backed.
