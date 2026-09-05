# B779 · Child-tree construction primitives — the container choice (frozen vs dynamic vs typed BFolder), `reorder`, and legality vetoes (MAE8, D5)

> **Scope**: the DECISION a module author makes when building a child-tree — which container primitive per
> cardinality, how to order dynamic children, and how to restrict legal children. Corrects two audit hypotheses
> with code: `BComponentList` genuinely does NOT exist, and `reorder(Property[])` IS exercised by real modules.
> Complements interactive-composition P2–P4. Focus: `module-authoring-exemplars` (MAE8 / dimension D5). Kit
> destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled — `baja` (`BComponent`), `control-rt` (`BControlPoint`), `analytics-rt`,
> `tagdictionary-rt`, `bacnet-rt`, `bacnetAws-rt`, `clOnboardIO-rt`; verified this session at `organized/`.
> FUENTE 1: B4 (dynamic slots), B33 (add/remove batch), B749 P4 (typed folders/legality), B538 (naming).
> READ-ONLY. English (post-B115).

---

## 779.1 — There is NO `BComponentList` — children are SLOTS `[CERT]`
`module_nav class BComponentList` → *"Class 'BComponentList' not found"*; `corpus-nav find BComponentList` → no
matches (two independent tools agree — genuine absence, not a search miss). A module's children are held as **slots
in a `SlotMap`** on `BComplex`/`BComponent`, never in a "list" collection type. Ordered/repeated children are done
with dynamic slots + `reorder`, or a typed `BFolder` subclass — §779.2/779.3.

## 779.2 — The container choice: three primitives, three decision rules `[CERT]`
- **(a) FROZEN child** — a `@NiagaraProperty` typed as a BComponent. Use when the child is structurally guaranteed:
  always present, fixed name, known at compile time. Exemplar `BControlPoint` (control-rt) declares its `proxyExt`
  child as a frozen property: `@NiagaraProperty(name = "proxyExt", type = "BAbstractProxyExt", defaultValue = "new
  BNullProxyExt()")` (`control-rt/.../BControlPoint.java:34-39`) — every control point is BORN with a proxyExt.
  **Rule: fixed cardinality + fixed name → frozen property.**
- **(b) DYNAMIC child** — runtime `add(String name, BValue)`. Use when count/names are data-driven (unknown until
  runtime). Exemplar `analytics-rt` builds children in a loop: `this.aVal.add("value" + counter, …); this.aVal.add(
  "status" + counter, …)` (`analytics/.../AnalyticMultiTrendCursor.java:49-51`). **Rule: variable/looped cardinality,
  names computed at runtime → dynamic `add()`.**
- **(c) TYPED `BFolder` subclass** — use when children are a homogeneous, user-growable collection needing its own
  type/agents/naming. Exemplar `public class BAnalyticReportFolder extends BFolder` (`analytics-rt/.../report/
  BAnalyticReportFolder.java:49`). **Rule: homogeneous growable collection → a typed folder node.**

## 779.3 — `reorder(Property[])` — for DYNAMIC slots, and it IS used `[CERT]` (audit refuted)
The slot-reorder API on `BComponent`: `public final void reorder(Property[] dynamicProperties, Context context)`
(`baja/.../BComponent.java:1118`) and the no-Context overload (`:1131`), with a `reordered(Context)` callback. The
audit reported "0 exemplar hits" — FALSE: real call sites exist, e.g. `this.reorder(tvs, null)` in
`bacnet-rt/.../BBacnetDailySchedule.java:302` (plus `exportTags` BSupervisorJoinJob, `haystack` HsImportUtil,
`platform` BNtpServerVector per the sweep). **Rule**: `reorder` operates on `Property[]` DYNAMIC props — a builder
calls it to impose display/iteration order on a data-driven collection (import, join, vector); frozen children take
their order from declaration order and never call it. (The "0 callers" from a callgraph tool was a resolution miss,
not reality — grep found the sites; cf. the B-corpus heuristic "follow the call chain, don't trust a name-grep zero".)

## 779.4 — Typed-tree LEGALITY: `isChildLegal` / `isParentLegal`, default-permissive `[CERT]`
Both legality hooks default to `true` on `BComponent` — `public boolean isParentLegal(BComponent parent)` (`:1369`)
and `public boolean isChildLegal(BComponent child)` (`:1381`) — so a container accepts anything unless it OVERRIDES:
- **Container restricts its children** (`isChildLegal`): `tagdictionary-rt/.../BTagRuleScopeList.java:27` →
  `return child instanceof TagRuleScope;` (also `bacnetAws` BBacnetActionList → `child instanceof
  BBacnetActionCommand`).
- **Child restricts its parent** (`isParentLegal`): `clOnboardIO-rt/.../BOnboardIOPointFolder.java:36-44` rejects
  unless the parent is `BOnboardIOPointDeviceExt`/`BOnboardIOPointFolder` (else `false`); `bacnetAws`
  BBacnetAwsDeviceFolder similarly. **Rule: override `isChildLegal` on the container to reject foreign types; override
  `isParentLegal` on the movable node to restrict where it mounts — both are `instanceof` vetoes returning `false`.**

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | `BComponentList` does not exist; children are slots on BComplex/BComponent | [CERT] | module_nav (class not found) + corpus-nav (no matches) |
| 2 | Frozen child = `@NiagaraProperty` BComponent-typed (proxyExt on BControlPoint) | [CERT] | control-rt/BControlPoint.java:34-39 |
| 3 | Dynamic child = runtime `add(name, BValue)` in a loop (analytics) | [CERT] | analytics-rt/AnalyticMultiTrendCursor.java:49-51 |
| 4 | Typed growable collection = a `BFolder` subclass (BAnalyticReportFolder) | [CERT] | analytics-rt/BAnalyticReportFolder.java:49 |
| 5 | `reorder(Property[])` exists AND is called by real modules (audit's 0-hits refuted) | [CERT] | BComponent.java:1118,1131; BBacnetDailySchedule.java:302 |
| 6 | `isChildLegal`/`isParentLegal` default true; overridden with `instanceof` vetoes | [CERT] | BComponent.java:1369,1381; BTagRuleScopeList.java:27; BOnboardIOPointFolder.java:36-44 |

**Tally**: 6 [CERT], 0 [INFER on the claims]. No unmarked claims. Load-bearing cites grep-verified inline this
session; the delegated sweep's BComponent line numbers were corrected on verification (real: reorder :1118/:1131,
legality :1369/:1381).

## Connections
- **B4** (dynamic slots), **B33** (add/remove batch), **B749 P4** (typed folders + isParentLegal), **B538** (naming)
  — this block consolidates the CONTAINER-CHOICE decision across them. **B778** (author-side SPIs — a service is one
  such frozen-vs-dynamic decision at the /Services level).

## Open gaps
- **MAE8-G1** — `reorderToTop`/`reorderToBottom` (BComponent.java, seen near :1131) and the `reordered(Context)`
  callback are named but not walked; a bounded follow-up if a builder needs pinned-first/last children.

## Kit implication (→ `types/logic.md`)
Add a "child-tree containers — pick by cardinality" rule: **frozen `@NiagaraProperty`** (BComponent-typed) for
fixed/known children (born-with, like BControlPoint's proxyExt); **runtime `add(name, BValue)`** for data-driven
children, with `reorder(Property[])` to impose order; **a typed `BFolder` subclass** for a homogeneous, user-growable
collection. There is NO `BComponentList`. Enforce a typed tree by overriding `isChildLegal`/`isParentLegal` (both
default `true`) with `instanceof` vetoes returning `false`.
