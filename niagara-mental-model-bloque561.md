# Block 561 — `BCategoryService` runtime: category enforcement is ORD-PREFIX inheritance recomputed on a 60 s periodic daemon over a fixed 256-slot space — not a live tree walk, and the cap is 256 (not 64)

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC3 — the CategoryService runtime mechanics [Block 11] described conceptually
but never opened: how an ORD resolves to a category mask, propagation, the recompute cadence, the real cap)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the category package; resolver + update-thread + buffer size
token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/category/{BCategoryService,BOrdToCategoryMap,BCategoryMask,
  BCategoryMode}.java`.

**Scope**: [Block 11] covers the category CONCEPT (categories partition the component space; a user's
`BCategoryMask` gates what they see). This block opens the RUNTIME: the ORD→mask resolver, how inheritance
actually works, the recompute cadence, and the true category ceiling. It corrects an audit assumption (64-limit)
with the measured value. Does NOT re-open the RBAC concept model or slot-level enforcement ([Block 11]/[Block
30]) — REMITTANCE.

---

## 561.1 The service and its immutable map [CERT]

`public final class BCategoryService extends BComponent implements BIService, BIRestrictedComponent` `[CERT] :66`.
Properties `[CERT] :53-68`: `ordMap` (a `BOrdToCategoryMap`), `updatePeriod` (`BRelTime`, **default 60 000 ms =
60 s**), and an `update` action. `BOrdToCategoryMap extends BSimple implements BIUnlinkable` `[CERT]
BOrdToCategoryMap.java:20` — an **immutable value** holding two parallel arrays (`BOrd[]` + `BCategoryMask[]`);
`setCategoryMask(ord, mask)` returns a NEW map `[CERT] :60`, and it persists via `encodeToString`/`decodeFromString`
`[CERT] :286-305`. `BIRestrictedComponent` `[CERT] :279-280` means editing the service is **super-user gated**
(`checkContextForSuperUser`) — a non-super-user cannot re-map categories.

## 561.2 The resolver: ORD-PREFIX inheritance, not a tree walk [CERT]

Two resolvers on the map, both wrapped by the service (returning `DEFAULT_MASK` on miss `[CERT] :154-173`):
- `getCategoryMask(BOrd)` — **exact** ORD match.
- `getAppliedCategoryMask(BOrd)` `[CERT] BOrdToCategoryMap.java:204-236` — the **enforcement** resolver. It
  relativizes the ORD to a string and, for each map entry, returns that entry's mask when the target string
  either equals the entry OR **`s.startsWith(this.ordSlashes[i])`** (a path-prefix hit). Case-sensitivity is
  decided per-ORD by `HistoryCategoryUtil.isCaseSensitiveInOrdToCategoryMap`, with a case-insensitive fallback
  match tracked separately.

So **category inheritance is STRING-PREFIX on the ORD**, not a component-tree ascent: a point at
`station:|slot:/Drivers/Bacnet/Dev/Pt` inherits the category assigned to `.../Drivers/Bacnet` because its ORD
string starts with that ancestor's slash-path. Assigning a category to a container implicitly categorizes its
subtree by prefix. `DEFAULT_MASK = BCategoryMask.make("1")` `[CERT] :73` — an unmapped ORD falls into
**category index 0** (the "1" bit), the default category, which is why stock components are visible to ordinary
users until explicitly categorized.

## 561.3 The recompute is PERIODIC and asynchronous [CERT]

Category membership is NOT recomputed on every change. An inner `UpdateThread extends Thread` `[CERT] :460-490`
— a **daemon at priority-1** — sleeps 5 s in a loop and calls `doUpdate()` only when `Clock.ticks() -
lastUpdateTicks >= updatePeriod` (period clamped to a 1 s floor; `period == 0` disables). `doUpdate()` `[CERT]
:326-343`:
```java
BCategoryMask[] working = new BCategoryMask[256];      // fixed 256-slot category space
... working[i] = BCategoryMask.NULL ...
ComponentSlotMap slotMap = (ComponentSlotMap) station.fw(1);
slotMap.updateDeepOr(working, 0);                      // deep-OR membership across the whole station
```
Operational consequence: after you change a component's category, enforcement can lag by up to `updatePeriod`
(default **60 s**) before the recomputed masks take effect — unless someone fires the `update` action / RPC.
There is an `updateRpc` `@NiagaraRpc(permissions = "r", transports = box/fox/web)` `[CERT] :344-358` so any
read-capable user can force an immediate rescan.

## 561.4 The cap is 256, not 64 [CERT] — corrects the audit assumption

The AUDIT-FIRST seed proposed a "64-category hard limit". **Measured value: the `doUpdate` working buffer is
`new BCategoryMask[256]` `[CERT] :328`**, and `updateDeepOr(working, 0)` fills membership across all 256 slots —
so the runtime category space is **256**, not 64. `BCategoryMask` itself is a variable-length hex bitset
(`make(int[] indices)` sets `1 << (index % 4)` into byte `index / 4` `[CERT] BCategoryMask.java:135-169`,
`WILDCARD = "*"` `[CERT] :18`), so the mask type does not impose 64; the 256 buffer is the effective ceiling.

## 561.5 How masks combine — `BCategoryMode` [CERT]

`BCategoryMask` supports `or`/`and` `[CERT] :23,78`. The combination policy is `BCategoryMode`: binary
`union` (0) / `intersection` (1), **`DEFAULT = union`** `[CERT] BCategoryMode.java:12-19`. Union is the
permissive default (a user in categories A and B sees A ∪ B) — consistent with the roles-union default seen in
[Block 559] §559.5: N4's identity-combination defaults lean permissive on what you can access.

## 561.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BCategoryService: BIService + BIRestrictedComponent (super-user gated); ordMap + updatePeriod default 60s | [CERT] | BCategoryService.java:66,68,279-280 | token-checked ✓ |
| 2 | BOrdToCategoryMap = immutable BSimple, parallel BOrd[]/BCategoryMask[] arrays, setCategoryMask returns new map, persisted via encodeToString | [CERT] | BOrdToCategoryMap.java:20,60,286-305 | token-checked ✓ |
| 3 | getAppliedCategoryMask = ORD-PREFIX inheritance (startsWith on slash-path), not tree walk; case-sensitivity via HistoryCategoryUtil | [CERT] | BOrdToCategoryMap.java:204-236 | token-checked ✓ |
| 4 | DEFAULT_MASK = make("1") = category 0; unmapped ORD → default category | [CERT] | BCategoryService.java:73,162 | token-checked ✓ |
| 5 | Recompute is periodic/async: daemon prio-1, 5s tick, doUpdate when updatePeriod elapsed (min 1s, 0=off) | [CERT] | :460-490 | token-checked ✓ |
| 6 | Category space cap = 256 (working buffer), NOT 64; deep-OR fill | [CERT] | :328,337 | token-checked ✓ |
| 7 | BCategoryMode union(0)/intersection(1), DEFAULT=union | [CERT] | BCategoryMode.java:12-19 | token-checked ✓ |
| 8 | updateRpc @NiagaraRpc permissions="r" over box/fox/web forces rescan | [CERT] | :344-358 | token-checked ✓ |

**Marker tally**: [CERT] ×8 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 8 of 8 rows token-verified
inline. Corrects one AUDIT-FIRST assumption (64→256).

## Connections

- **[Block 11]** — the category CONCEPT; this opens its runtime (resolver, cadence, cap).
- **[Block 30]** — slot-level enforcement (AccessSlotCursor consumes the category mask this service computes).
- **[Block 559]** (AC2) §559.5 — the same permissive-union default direction (roles union / category union).
- **[Block 558]** (AC1) — sibling RBAC runtime.

## Open gaps (this block)

- `ComponentSlotMap.updateDeepOr` internals (how each component contributes its category bit during the deep-OR)
  are named, not decompiled — low value; open on demand. `HistoryCategoryUtil` auto-categorization of history
  records is named ([Block 8] history territory). Focus continues at AC4 (password encoder chain).
