# Block 536 — The `control` module writable-point model: `WritableSupport`, the 16-level priority-array arbitration, override actions, and the point-extension execution chain

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC1 — control-module internals) · first block of the focus
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY decompilation. Delegated sonnet sweep + inline token-verification against source.
**Primary sources** (fidelity order — original Tridium javadoc first):
- `organized/docSource/docSource-doc/vineflower/control-rt/javax/baja/control/` — ORIGINAL Tridium source
  with real javadoc: `WritableSupport.java`, `BControlPoint.java`, `BIWritablePoint.java`,
  `BNumericWritable.java`, `BAbstractProxyExt.java`, `BPointExtension.java`, `BNumericTotalizerExt.java`.
- `organized/control/control-rt/vineflower/javax/baja/control/enums/BPriorityLevel.java`,
  `.../util/BOverride.java`, `.../util/BNumericOverride.java` — classes ABSENT from docSource; decompiled.

**Scope**: this block distils *how a writable control point actually behaves* — the arbitration engine, the
priority levels and their persistence, the override/auto/set action surface, and the extension execution
order. It answers the operator's "how are control modules programmed / what are the rules" from the
runtime-contract side. It is the FIRST of the `kitControl` focus and the code-level deepening of the
concept table in **[Block 6] §6.3** (REMITTANCE: the taxonomy, proxyExt bridge, and facets are already
there — cited, not re-derived).

---

## 536.1 What [Block 6] already established (REMITTANCE — not re-derived here)

[Block 6] §6.3.1 gives the 8-type taxonomy (4 data types × {Readonly, Writable}), §6.3.2 the `proxyExt`
bridge to an external device, §6.3.5 facets. This block does NOT restate them; it opens the mechanics
those sections named but did not decompile: the arbitration algorithm, the level semantics, and the
action/extension execution contract.

MEASURED size of the package (sweep, verified): **31 distinct classes** in
`control-rt/vineflower/javax/baja/control/` across sub-packages `enums/` (3), `ext/` (4), `trigger/` (6),
`util/` (5), plus 13 in the package root. (The full `control` module = `-rt` + `-ux` + `-wb` ≈ 45 `.java`.)

## 536.2 The arbitration engine — `WritableSupport.onExecute` [CERT]

Every writable point (`BNumericWritable`, `BBooleanWritable`, `BEnumWritable`, `BStringWritable`) computes
its `out` through a shared inner helper, `WritableSupport`. The arbitration is a **1→16 first-valid-wins
scan**, verbatim [CERT] `docSource/.../WritableSupport.java:150`:

```java
void onExecute(BStatusValue out, Context cx) {
    BStatusValue active = null;
    int activeLevel = 17;
    for (int level = 1; level <= 16; ++level) {
      BStatusValue in = getLevel(level);
      if (in.getStatus().isValid()) { active = in; activeLevel = level; break; }   // :161
    }
    if (active == null) {                                                          // :168
      BStatusValue fallback = getFallback();
      out.copyFrom(fallback);
      out.setStatus(BStatus.ACTIVE_LEVEL, BDynamicEnum.make(BPriorityLevel.fallback));
    } else {
      setValue(active, out);
      int status = 0;
      if (activeLevel == 1 || activeLevel == 8) status |= BStatus.OVERRIDDEN;      // :177
      out.setStatus(status);
      out.setStatus(BStatus.ACTIVE_LEVEL, BDynamicEnum.make(BPriorityLevel.make(activeLevel)));
    }
}
```

The RULES that fall out of this, all [CERT]:
- **Priority order is fixed and numeric**: level 1 is highest, 16 lowest, `break` on the first valid input.
- **"Relinquished" = invalid status.** Each `inN` sits at `BStatus.nullStatus` when nobody commands it;
  `isValid()` is false for it, so the scan skips it. This is the BACnet-style *NULL relinquish* model.
- **`fallback` is the relinquish-default**, used ONLY when all 16 levels are null (`active == null`). It is
  NOT level 16 — it is stamped `BPriorityLevel.fallback` (ordinal 17) in the `ACTIVE_LEVEL` status facet.
- **`out` is never itself null**: with everything relinquished, `out` takes `fallback`. A writable point
  always resolves to a defined value.
- **Levels 1 and 8 alone raise the `OVERRIDDEN` status bit** (`:177`) — the UI "hand" indicator is a pure
  function of the *winning level*, hardcoded to {1, 8}, not of how the value was written.
- The winning level is published on `out` as the `ACTIVE_LEVEL` status facet, so downstream consumers can
  read which priority is currently in control.

## 536.3 The 16 input slots and their PERSISTENCE — why only levels 1 and 8 survive a reboot [CERT]

The 16 `inN` properties are declared on the writable class itself (not on `WritableSupport`, which reaches
them through abstract `in1()`…`in16()` getters — `WritableSupport.java:49-64`). Their **flags** encode the
persistence rule [CERT] `docSource/.../BNumericWritable.java`:

| Slot | Flags | Persisted in `.bog`? | Role |
|------|-------|----------------------|------|
| `in1` (level 1) | `READONLY` | **yes** (:258) | emergency override — survives restart |
| `in8` (level 8) | `READONLY` | **yes** (:443) | manual/operator override — survives restart |
| `in2..in7, in9, in11..in15` | `TRANSIENT` | no | ephemeral command levels (links, apps) |
| `in10, in16` | `SUMMARY | TRANSIENT` | no | ephemeral, surfaced in summary UI |
| `fallback` | (none) | **yes** (:679) | relinquish-default — survives restart |
| `overrideExpiration` | `OPERATOR | READONLY` | yes (:705) | timestamp a timed override auto-reverts at |

RULE [CERT]: a command written at an emergency (1) or manual (8) level **persists across a station restart**
because those slots are `READONLY`-persisted; every other level is `TRANSIENT` and is lost on reboot,
re-driven only when its source (a link, a driver, an app) executes again. This refines [Block 6] §6.3.6,
which asserted levels 1 and 8 persist "in BOG" — CONFIRMED, and now anchored to the exact flag.

## 536.4 `BPriorityLevel` — the level enum and where the *meaning* lives [CERT]

`enums/BPriorityLevel.java:17-53` [CERT] (path: `control-rt/vineflower/.../enums/`, ABSENT from docSource):
`NONE=0`, `LEVEL_1..LEVEL_16 = 1..16`, `FALLBACK=17`, `DEFAULT = none`. The `fallback` display string is
remapped to the lexicon key `"def"` so UIs render it as "Default".

RULE — **the enum carries NO semantic names**. There is no `EMERGENCY` or `MANUAL` constant; the meaning of
levels 1 and 8 lives in the *javadoc* of the writable inputs and in the `WritableSupport:177` hardcode, not
in the type. So "level 1 = emergency, level 8 = manual" is a **framework convention enforced by three
disjoint facts** (in1/in8 are the persisted slots §536.3, they alone raise OVERRIDDEN §536.2, and only they
have dedicated emergency/manual actions §536.5) — not a single declared enumeration. An integrator wiring a
link into `in4` gets a transient, non-overriding, mid-priority command with no special semantics.

## 536.5 The action surface — how a value is COMMANDED, per level [CERT]

Writables expose actions that delegate into `WritableSupport` [CERT] `docSource/.../WritableSupport.java`:

| Action | Writes | Status set | Timer | file:line |
|--------|--------|-----------|-------|-----------|
| `emergencyOverride(v)` | `in1` (level 1) | `BStatus.ok` | — | :187 |
| `emergencyAuto()` | `in1` → `nullStatus` | relinquish | — | :198 |
| `override(BOverride)` | `in8` (level 8) | `BStatus.ok` | schedules revert | :208 |
| `auto()` | `in8` → `nullStatus` | relinquish | cancels timer | :246 |
| `set(v)` | `fallback` | `BStatus.ok` | — | :261 |

**Timed override (TTL)** [CERT] `WritableSupport.java:235-243`: when `BOverride.duration > 0`,

```java
setOverrideExpiration(BAbsTime.make(Clock.millis() + duration.getMillis()));
overrideTimer = Clock.schedule(point, duration, point.getAction("auto"), null);
```

the override schedules the point's own `auto` action to fire at expiry — self-reverting level 8 back to
relinquished. A `maxOverrideDuration` cap is enforced in `override()` (`:226-233`), read from the point's
`facets` or from `station/sysInfo`. On restart, `WritableSupport.started()` (`:75`) re-reads
`overrideExpiration`: it reschedules the timer if still future, or calls `auto()` immediately if it has
already passed — so a timed manual override cannot get stuck "on" across a reboot.

RULE: `set()` writes the **fallback**, not a numbered level — the "default value" a point falls back to when
nothing else commands it is set through the ordinary `set` action, which is why a plain write to a writable
point looks like the lowest possible priority.

## 536.6 `BOverride` — the override command object [CERT]

`util/BOverride.java` [CERT]: `duration` = `BRelTime`, default `0` meaning **permanent** (:60);
`maxOverrideDuration` = `BRelTime`, `HIDDEN | TRANSIENT` framework-only cap (:87). The base `BOverride`
carries NO value; each typed subclass adds its own — `BNumericOverride.value` is a `double` (:46), with
`BBooleanOverride`/`BEnumOverride`/`BStringOverride` parallel. So an override is *(typed value + duration)*,
and `duration=0` is the permanent-hold sentinel.

## 536.7 `BIWritablePoint` — the writable contract [CERT]

`BIWritablePoint.java:43-57` [CERT] declares the three-method interface every writable implements:
`getActiveLevel()` (the winning `BPriorityLevel`), `getInStatusValue(BPriorityLevel)` (the value at a level),
`getInProperty(BPriorityLevel)` (the frozen `Property` slot for a level). This is the programmatic surface a
driver or app uses to read/introspect arbitration state without touching the 16 slots by name.

## 536.8 Extensions and execution order — `proxyExt` runs FIRST [CERT]

A control point's per-cycle work is `BControlPoint.doExecute()` [CERT] `docSource/.../BControlPoint.java:287`:
1. `onExecute(working, null)` — the point's own arbitration (for a writable, `WritableSupport.onExecute`).
2. `executeExtensions(working, null)` (`:385`) — iterates ALL properties via `SlotCursor<Property>` in
   **slot-declaration order**, calling `onExecute` on each `BPointExtension`.
3. `out.copyFrom(working, …)` — publishes `out` if it changed.

`BPointExtension.onExecute(BStatusValue, Context)` is **abstract** (`:143`) — every extension implements it.
Extensions declare `requiresPointSubscription()` (default `false`, `:121`); an extension that returns `true`
(e.g. `BNumericTotalizerExt`, `:331`) makes `BControlPoint.checkExtensionsRequireSubscription()` (`:375`)
mark the point permanently subscribed.

**`proxyExt` is ALWAYS the first extension executed** — CONFIRMED two ways [CERT]: the javadoc on
`BAbstractProxyExt.onExecute` states it verbatim (`BAbstractProxyExt.java:56`, *"The ProxyExt is always the
first extension executed"*), and `proxyExt` is the first frozen `BPointExtension` slot iterated in
`executeExtensions` declaration order. This CONFIRMS [Block 6] §6.3.2's claim from code. The working value it
returns is fed through the remaining extensions into `out` — so a proxy point's read from the device flows
after arbitration but ahead of alarm/history extensions.

**Extensions shipped in `control-rt` itself**: `ext/` holds only `BAbstractProxyExt`, `BNullProxyExt`,
`BNumericTotalizerExt`, `BDiscreteTotalizerExt`. Alarm and history extensions are `BPointExtension`
subclasses provided by OTHER modules (`alarm-rt`, `history-rt`), not this package [INFER — from the package
listing; the exact modules are named in [Block 6] §6.3.4 and the alarm/history focuses].

## 536.9 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Arbitration = 1→16 scan, first valid (non-null-status) wins, `break` | [CERT] | WritableSupport.java:150-166 | token-checked ✓ |
| 2 | `fallback` used only when all 16 null; stamped ordinal 17, not level 16 | [CERT] | WritableSupport.java:168-171 + BPriorityLevel.java:52 | ✓ |
| 3 | Only winning levels 1 and 8 raise `BStatus.OVERRIDDEN` | [CERT] | WritableSupport.java:177 | token-checked ✓ |
| 4 | in1/in8/fallback are READONLY-persisted; other inN TRANSIENT | [CERT] | BNumericWritable.java:258,443,679 | ✓ (sweep-cited) |
| 5 | BPriorityLevel: 0=none,1..16,17=fallback,DEFAULT=none | [CERT] | enums/BPriorityLevel.java:17-53 | token-checked ✓ |
| 6 | Enum has NO emergency/manual constant; semantics are convention | [CERT] | BPriorityLevel.java:14 (@Range) + WritableSupport:177 | ✓ |
| 7 | Actions: emergencyOverride→in1, override→in8+timer, set→fallback | [CERT] | WritableSupport.java:187,208,261 | token-checked ✓ |
| 8 | Timed override schedules `auto` via Clock.schedule; reschedules on restart | [CERT] | WritableSupport.java:235-243,75 | ✓ |
| 9 | BOverride = duration(0=permanent) + typed value on subclass | [CERT] | BOverride.java:60,87 + BNumericOverride.java:46 | ✓ (sweep-cited) |
| 10 | proxyExt is always first extension executed | [CERT] | BAbstractProxyExt.java:56 + BControlPoint.java:385 | token-checked ✓ |
| 11 | Alarm/history exts come from other modules, not control-rt | [INFER] | package listing | honest INFER |

**Marker tally**: [CERT] ×10 · [INFER] ×1. Ratio [INFER]/[CERT] = 0.10 — LOW. Block TYPE = EVIDENCE
(decompilation). A low ratio here means the gap's core is well-anchored; it does NOT signal exhaustion of
the focus (11 gaps remain).
**Tokens checked inline**: 6 load-bearing citations resolved directly against source (arbitration loop,
override/auto/set line offsets, BPriorityLevel ordinals) — all matched. Path correction applied:
`BPriorityLevel` was sweep-cited to docSource but lives only in the decompiled `enums/` tree.

> **Refined by [Block 538]** (KC3, official rules): on a **Boolean** writable, `In6` (level 6) is ALSO
> unlinkable — reserved for the built-in minimum on/off timers (`[CERT-doc] aPriorityLinkRules.txt:23`). This
> block was numeric-scoped and made no claim about Boolean level 6. B538 also adds the rule that status
> **never propagates into a control point** (the point boundary stops kitControl status-OR).

## Connections

- **[Block 6]** §6.3 — the taxonomy/proxyExt/facets this block deepens (REMITTANCE); §6.3.6 CONFIRMED +
  refined (level 1/8 persistence anchored to `READONLY` flags).
- **[Block 538]** (KC3) — the official linking RULES that reconcile with this block's arbitration mechanics;
  refines it with the Boolean level-6 reservation.
- **[Block 46]** — priority-array writes from an external SPA (the transport-specific write path; KC8 will
  consolidate the generic arbitration end-to-end).
- **[Block 37]** — kitControl↔virtual↔writeback via a KNX driver (proxyExt in a real driver chain).
- **[Block 276]** — the BACnet-writable slice of `BControlPoint` (prior partial coverage).
- **Forward**: KC2 (kitControl FB catalog — the blocks that link INTO these inputs), KC8 (priority-array
  arbitration end-to-end incl. driver proxy), KC4 (BLoopPoint, a writable-driven control primitive).

## Open gaps (this block)

- None new blocking. Item #11 (which modules provide alarm/history extensions) is answered by the existing
  alarm/history coverage; not a new gap.
- KC8 is the natural continuation (write path from a kitControl block → these 16 levels → driver).
