# B744 · Anatomy of a Niagara RT "block" (BComponent) — the consolidated pattern: what it is made of, its format and rules, what it shows, does, and can explore/discover

> **Scope**: the ONE consolidated reference for "what is an RT block in Niagara" — a `BComponent`'s parts,
> format, rules, what it shows, what it does and how, and what it can do / explore / discover. This is a
> SYNTHESIS/INDEX over the rt campaign (B4, B729-B743) — each section cites the deep block; nothing is
> re-derived. Foco: module-best-practices.
>
> **Sources**: FUENTE 1 — B4 (object model), B729 (lifecycle), B730 (idioms), B731 (audit), B732 (alarms),
> B733 (0-10V/PID/math), B734 (points), B735 (slots/facets/links), B736 (BStatus), B737 (engine/watchdog/
> composition), B738 (proxyExt/facets/propagateFlags/icons), B739/B740 (schema/enum), B741/B743 (QA/tests),
> module-anatomy B629-636 (the MODULE that packages the block).

---

## 744.1 — What an RT block IS
A `BComponent` subtype: a live node in the station component tree, registered as a Niagara **type**
(`@NiagaraType` → `module-include.xml` `<type>` → registry `NTypeInfo`, loaded once via `ModuleClassLoader`,
B631). It carries typed STATE, invocable BEHAVIOR, and can broadcast EVENTS; it runs on the engine thread;
it can be linked, nested, navigated, alarmed, trended. A "block" on the wire sheet = one such component.

## 744.2 — What it is made of (parts)
- **Slots** — the members (B4 §4.1): **Property** (holds a typed `BValue`, persistable, get/set),
  **Action** (invocable, 0-1 param, opt. returns, handler `do<Name>()`), **Topic** (event, `fire<Name>()`).
- **Generated region** — `@NiagaraProperty/@NiagaraAction/@NiagaraTopic` → Slotomatic writes
  `newProperty/getX/setX`, `newAction/doX`, `newTopic/fireX` + the type hash. Authoritative — edit annotations
  then regen (B735 §735.1, module-anatomy).
- **Frozen vs dynamic slots** — frozen = declared in the type; dynamic = `add()`ed at runtime (B4 §4.3.3).
- **Children (composition)** — a slot whose type is a BComponent = a child; components nest into TREES
  (Station→Network→Device→Point→Extension). Group concerns into child components instead of a flat slot wall
  (B737 §B — the fix for our 25-slot sprawl).
- **If it's a control POINT** (`BControlPoint`): additionally an `out` (`BStatusValue`), `facets`, a
  `proxyExt` (field binding) and an ordered **extension** pipeline (proxy/alarm/history/control) — B734.
  A plain BComponent has none of that; it's just slots + children.

## 744.3 — Format & rules
- **Java 8**, `@NiagaraType`, Slotomatic-generated AUTO region (all our modules; B741/build).
- **Engine-thread discipline** (B6/B730): one engine thread runs all callbacks/timers/actions; NEVER block
  or throw on it (`catch(Throwable)`+log), hop back with `post()`/`postAsync()` from other threads, heavy IO
  off-thread via BWorker. A slow callback freezes the whole station (B737 §A).
- **Lifecycle hooks** (B729): `started()` (running→true, incl. late mount), `descendantsStarted()`,
  `stationStarted()`, `atSteadyState()` (bootstrap only), `stopped()` (teardown), `changed(p,cx)`,
  `clockChanged(shift)`. Self-armed timers MUST arm in `started()`+`atSteadyState()` (never atSteadyState-only).
- **Slot flags** (B735 §735.2 / B730 §730.5): `TRANSIENT` runtime state · `READONLY` outputs · `SUMMARY`
  wire-sheet pins · `OPERATOR` tunables · `HIDDEN` internals · `DEFAULT_ON_CLONE` calc-state · `ASYNC` timer
  actions · `FAN_IN` multi-link.
- **Facets** (B735/B738): `units`, `precision`, `min/max`, `range`, `trueText/falseText`, editor overrides;
  `getSlotFacets()` for dynamic projection.
- **Status** (B736): every `BStatusValue` carries the 8-bit `BStatus` (fault/down/stale/overridden/null/…);
  gate inputs with `isValid()`, set NULL/OVERRIDDEN honestly, propagate.
- **Schema evolution** (B739): ADD slots freely; NEVER retype an existing slot with saved data (breaks the
  `.bog` → no boot). Enums (B740): intra-module only; cross-module links use a plain double, never a shared enum.

## 744.4 — What it SHOWS
- **Property sheet** — non-hidden Property slots (values, editable per flags).
- **Wire sheet** — the component as a block with `SUMMARY` slots as pins; `getIcon()` gives its glyph
  (PNG/SVG module resource, B738).
- **Facets** render values with units/precision; **children** show as a collapsible tree.
- HIDDEN slots and actions don't show; the Link picker is filtered (B735 §735.4).

## 744.5 — What it DOES / HOW
- **Reacts**: `changed(p,cx)` on slot writes (guarded `isRunning()`, dispatch by slot, deadband before
  writing back — B730 §730.2); points run `execute()`/`onExecute` and commit `out` only on change (B730 §730.1).
- **Times**: `Clock.schedule`/`schedulePeriodically` → `HIDDEN|ASYNC` action callbacks, cancel-before-reschedule,
  teardown in `stopped()` (B729/B730 §730.4).
- **Connects**: BLinks propagate Property→Property (dataLink) or Topic→Action (event); knobs mirror the link.
- **Computes**: pure decision logic ideally extracted to a Baja-free class + adapter (template-method,
  B730 §730.7; testable, B741/B743).

## 744.6 — What it CAN DO / EXPLORE / DISCOVER
- **Be linked** to other components (`checkLink`→`LinkCheck`; excludable via `BIUnlinkable*`, B735 §735.4).
- **Be composed** into equipment trees; **navigate** parent/children (`getParent()`, `getChildren(type)`),
  by ORD (`station:|slot:/…`, B5) and by **BQL/NEQL** queries (B5) — e.g. the dashboard servlet queries the
  alarm space by BQL (B732).
- **Carry extensions** (only points): alarm (`BAlarmSourceExt`+algorithm, B732), history/trend
  (`BHistoryExt`, B733), proxy (field IO, B734).
- **Modulate** outputs via `BNumericWritable`→AO and run **PID** via `kitControl.BLoopPoint` (B733).
- **Raise alarms**, **be trended**, **be tagged/related** (hierarchy/tags, B5/B730-G1), **be overridden** via
  a writable priority array or HOA (B731/B734), **be discovered** in the palette/registry (module-anatomy).
- **Be tested**: pure logic in JUnit (WSL); lifecycle/arming only via `BTestNgStation` (needs a station) or
  live smoke (B743).

## 744.7 — Application: our blocks vs the pattern
Our `BColdRoom`/`BEvaporatorUnit`/`BCompressorControl`/`BDefrostController` are plain `BComponents` (not
points) with `BStatus*` properties, boolean outputs BLinked to driver proxy points. They follow the pattern
on lifecycle/engine-safety/degradation, but deviate on: flat slots (compose into children, B737),
atSteadyState-only timers (add started(), B729), no facets units (B735/B738), no DEFAULT_ON_CLONE (B731),
alarms/history not modeled (feature, B732/B733), defrost logic untested (B741). The consolidated backlog is
B742.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | An RT block = a registered BComponent subtype (slots + children), runs on the engine thread | [CERT] | B4; B631; B6/B737 |
| 2 | Slots = Property/Action/Topic; generated by Slotomatic from @Niagara* annotations; frozen vs dynamic | [CERT] | B4 §4.1; B735 §735.1 |
| 3 | Composition into child trees (Network→Device→Point→Extension); points add out/facets/proxyExt/extensions | [CERT] | B737; B734 |
| 4 | Rules: Java8/Slotomatic, engine-thread discipline, lifecycle hooks, flags, facets, BStatus, add-don't-retype, intra-module enums | [CERT] | B729/B730/B735/B736/B739/B740 |
| 5 | Shows via property sheet/wire sheet(SUMMARY)/icon/facets/tree; HIDDEN + link filters curate it | [CERT] | B735 §735.4; B738 |
| 6 | Can link/compose/navigate(ORD/BQL)/carry extensions/modulate/PID/alarm/trend/override/test | [CERT] | B5/B732/B733/B734/B735/B743 |

**Tally**: 6 [CERT]. No unmarked claims. This is an index/synthesis; depth lives in the cited blocks.

## Connections
- The whole rt campaign B4 + **B729-B743**; module-anatomy B629-636 (the packaging); kit `types/logic.md`.

## Open gaps
- **B744-G1**: none new — this consolidates; per-topic gaps live in their blocks (e.g. B743-G1 the Sched seam).
