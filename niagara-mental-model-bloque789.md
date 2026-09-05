# B789 · Conformance audit — children-legality + poll-vs-subscribe (OMV3/OMV5), background-work clean (OMV6)

> **Scope**: CONFORMANCE AUDIT of our three modules across three dimensions at once — extensions/children (B772/B779),
> services/ORD/subscription (B778), background-work (B774). Two real findings (both LOW/LOW-MED, both self-documented
> as TODOs in our own source), one clean dimension. Combined into ONE block (no padding — each finding is compact).
> Focus: `own-modules-vs-exemplars` (OMV3+OMV5+OMV6). Routes to a client punch-list; biting-checks are advisory here
> (the defects need semantic judgment, not a static lint — an honest contrast to the OMV2/OMV4 hard checks).
>
> **Sources**: FUENTE 3 our rt source (verified this session): ColdRoomPan (`BColdRoom`, `BEvaporatorUnit`,
> `BDefrostController`), CompPan (`BCompressorControl`), DashboardPan (`BDashboardService`, `BRoomPanel`); reference
> chihuahua-rt. FUENTE 1: B772/B779 (extensions/children), B778 (service/subscription), B774 (jobs). READ-ONLY.

---

## 789.1 — OMV3 extensions/children `[CERT]`
- **No custom `BPointExtension`** in our modules or chihuahua (all `extends BComponent`/`BAbstractService`) — correct
  (our components are equipment/control objects, not point extensions).
- **Container-by-cardinality CONFORMS** (matches B779): `BColdRoom` uses DYNAMIC children (`getChildren(
  BEvaporatorUnit.class)`, `BColdRoom.java:526`) — right for a variable 1..3 unit count; CompPan's 3 compressors are
  FROZEN output slots (`condenser1/2/3`, fixed cardinality) and DashboardPan's `Cuarto1..Cuarto5` are FROZEN
  `@NiagaraProperty` (fixed 5) — both the right call for fixed cardinality.
- **FINDING OMV3-1 (LOW-MED)**: `BColdRoom` is an ORDER- and TYPE-sensitive container with NO `isChildLegal`/
  `isParentLegal` guard (grep of the whole module = 0). `execute()` maps staging onto units BY CHILD SLOT ORDER
  (index 0/1/2 → unit1/unit2/unit3, `BColdRoom.java:471-488`, self-flagged `TODO`), and `getUnits()` returns EVERY
  `BEvaporatorUnit` child (`:523-529`). So a 4th unit an integrator drops mounts silently and gets DRIVEN
  (`i>=2 → call2`); an unrelated component mounts silently too. The parent/child contract is real but unenforced
  (`BDefrostController`/`BEvaporatorUnit` both assume the parent is a `BColdRoom`). chihuahua also omits legality
  guards (house style), but `BColdRoom` is exactly the control-by-order container where one earns its keep.
- No slot RETYPE hazard (all types at slotomatic v1.0; B739/B754 clean). [INFER]

## 789.2 — OMV5 services/ORD/subscription `[CERT]`
- **Registration CLEAN**: `BDashboardService extends BAbstractService` + overrides `getServiceTypes()`→`{TYPE}`
  (`BDashboardService.java:74,236-237`) — proper service. `BColdRoom`/`BCompressorControl` are plain `BComponent`
  equipment/control objects (correct — not station-wide singletons); `BRoomPanel` is a plain facade. No mis-registration.
- **FINDING OMV5-1 (LOW)**: `BDefrostController` POLLS a sibling's slot where a subscription fits (the B778
  server-side-subscription idiom). During an active defrost it re-reads the unit's `resistanceTemp` every
  `POLL = BRelTime.make(5000)` (`BDefrostController.java:731`) via a self-rescheduling ticket (`doPollTerminate`
  :629, `pollTicket = Clock.schedule(this, POLL, pollTerminate, null)` :622,:641). The value already flows through
  `BEvaporatorUnit.changed()`; a `Subscriber.event(BComponentEvent)` on `resistanceTemp` would terminate on the
  crossing EDGE (not up to 5 s late) and drop the ticket churn. Our own source documents this as the intended
  cleanup (`:639-640` `TODO … Prefer subscribing to the unit's resistanceTemp change instead of polling`). Bounded to
  active-defrost windows, one slot, 5 s → LOW.
- **NOT a violation** (honesty): `BCompressorControl`'s 5 s `tick` is a legitimate HEARTBEAT (time integration for
  run-hours rotation + start-prove timeout — must advance even with no input change); inputs arrive event-driven via
  `changed()`. `BColdRoom`/`BRoomPanel` are purely event-driven. Do NOT flag these.

## 789.3 — OMV6 background-work: CLEAN (no block owed) `[CERT]`
No file I/O, network, `Thread.sleep`, big loop, or `BSimpleJob` in any of the three rt modules (grep = 0). Every
engine-thread entry point is short (pure-logic slot I/O delegating to plain-Java models, wrapped in
`try/catch…logError`). `BDashboardService.appendAudit`/`_trimAuditRing` is O(≤500) (MAX_AUDIT_ENTRIES) and runs on the
ux SERVLET thread, not the engine thread. Nothing warrants a `BSimpleJob`. CLEAN.

## 789.4 — Routings `[INFER, grounded]`
- **OMV3-1 client punch-list**: add `isChildLegal(BComponent c){ return c instanceof BEvaporatorUnit || c instanceof
  BDefrostController; }` to `BColdRoom` (and cap units at 3), so the control-by-order logic can't be broken by an
  integrator dropping an extra/foreign child. Kit biting-check: ADVISORY only — "an order-sensitive container without
  a legality guard" is not statically decidable (a lint can't know a container is order-sensitive); surface it as a
  review note, not a hard fail.
- **OMV5-1 client punch-list**: replace `BDefrostController`'s 5 s resistanceTemp poll with a `Subscriber` on the
  unit's `resistanceTemp` (edge-terminate, drop the self-rescheduling ticket). Kit biting-check: ADVISORY — "a
  self-rescheduling poll of a sibling slot that could be a subscription" needs semantic judgment (vs a legitimate
  heartbeat like CompPan's tick); a review note, not a lint.
- **OMV6**: nothing.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | No custom BPointExtension; container-by-cardinality conforms (BColdRoom dynamic, CompPan/DashboardPan frozen) | [CERT] | BColdRoom.java:526; BCompressorControl condenser1-3; BDashboardService Cuarto1-5 |
| 2 | OMV3-1: BColdRoom controls by child order + returns all children + NO isChildLegal/isParentLegal (module grep = 0) | [CERT] | BColdRoom.java:471-488,523-529; grep legality = 0 |
| 3 | Service registration clean: BDashboardService = BAbstractService+getServiceTypes; others correctly plain BComponent | [CERT] | BDashboardService.java:74,236-237 |
| 4 | OMV5-1: BDefrostController polls resistanceTemp every POLL=5000ms via self-rescheduling ticket; self-TODO prefers subscribing | [CERT] | BDefrostController.java:622,629,641,731,639-640 |
| 5 | CompPan tick is a legitimate heartbeat (time integration/start-prove), NOT poll-instead-of-subscribe | [CERT/INFER] | BCompressorControl.java tick/doTick; [INFER] on the "legitimate" judgment |
| 6 | OMV6 background-work CLEAN — no File/sleep/BSimpleJob; engine-thread work bounded | [CERT] | grep = 0 across 3 rt modules |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. Both findings + the clean dimension grep-verified inline this session.

## Connections
- **B772/B779** (extensions/children — OMV3-1 is the legality-veto B779 documents, unapplied), **B778** (server-side
  subscription — OMV5-1 is the poll-vs-subscribe idiom, unapplied; both self-flagged as TODOs in our source),
  **B774** (jobs — OMV6 confirms none warranted). **B787** (BEvaporatorUnit timer finding — same module family).
  **B760** (punch-list — adds two items).

## Open gaps
- **OMV3-1-G1 / OMV5-1-G1** — both are requires-execution to prove the real-world effect (a 4th unit actually driven;
  the 5 s termination latency) — read-only source shows the gap, a station test shows the impact.

## Kit implication (→ client punch-list; biting-checks are ADVISORY here)
Two client punch-list items (BColdRoom `isChildLegal`; BDefrostController subscribe-not-poll) — both already TODO-flagged
in our own source. HONEST kit conclusion: unlike OMV2 (ticket-without-stopped-cancel) and OMV4 (dup-lexicon-keys /
empty-palette), the OMV3/OMV5 defects are NOT statically lintable (order-sensitivity and poll-vs-subscribe need semantic
judgment) — propose them as ADVISORY review notes in the kit's checklist, not hard `verify-module.sh` fails. This
distinction (which conformance rules can bite as a lint vs which stay human-review) is itself a kit-methodology delta.
