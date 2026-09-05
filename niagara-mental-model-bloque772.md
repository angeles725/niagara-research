# Block 772 — Authoring a Niagara point extension: the `BPointExtension` base-class contract (D6 / MAE1)

> Research of **how an author writes a NEW point extension in Niagara N4** — the abstract base-class
> authoring contract (which methods to override, how the extension observes the parent point's value,
> and multi-extension execution order). Scope: the base class and its override contract; NOT a catalog
> of concrete extensions (that is B6/B536/B552).
>
> Subject version: **N4.14.0.162** — control-rt / history-rt / alarm-rt as shipped in the corpus install.
>
> Sources: Tridium first-party source under `organized/docSource/docSource-doc/vineflower/{control-rt,history-rt,alarm-rt}/…`
> (line numbers verified in-file by the driver); corpus blocks B6/B536/B552/B734/B730/B738 (REMITTANCE);
> official docs via `niagara-help` (`class`/`devguide-search`/`source-grep`).
> Method: three-source sweep (corpus prior-coverage → niagara-help → code), driver-verified citations.
> Markers (canonical list: METHODOLOGY §3): `[CERT]` local primary source (`file:line`) · `[CERT-doc]`
> official doc · `[INFER]` deduction.
>
> **Type:** `standard`. Layer: module authoring / control (D6 extensions). Connects [Block 6] (extension
> taxonomy), [Block 536] (execution pipeline + `onExecute`), [Block 552] (alarm/history ext exemplars),
> [Block 734] (slot-order execution), [Block 730] (`changed()` discipline), [Block 738] (adding a proxyExt).
>
> **Premise correction:** gap MAE1 was seeded as "how to build a new `BAbstractPointExt`". **`BAbstractPointExt`
> does not exist** in the framework (§772.1) — the real base class is **`BPointExtension`**. The gap is renamed
> accordingly. No §14 back-pointer is owed: no prior corpus block asserted `BAbstractPointExt` (B6 already
> names `BPointExtension` correctly).

---

## 772.1 — Premise correction: the base class is `BPointExtension`, not `BAbstractPointExt` `[CERT]`

The authoring base for every point extension is **`BPointExtension`**, package `javax.baja.control`, extending
`BComponent`. It is the class an author subclasses; there is no `BAbstractPointExt` and no
`onExtended`/`onRetracted` lifecycle.

| Claim | Evidence | Citation |
|---|---|---|
| Base class = `BPointExtension` (`javax.baja.control`, extends `BComponent`) | class declaration | `organized/docSource/docSource-doc/vineflower/control-rt/javax/baja/control/BPointExtension.java` |
| `BAbstractPointExt` **does not exist** (proven absence) | `fd BAbstractPointExt` over `organized/` = 0 files; `niagara-help source-grep "BAbstractPointExt"` = 0/2603 files; `corpus-nav grep "BAbstractPointExt"` = 0 hits (two distinct terms: `BAbstractPointExt`, `AbstractPointExt`) | — (absence) |
| `onExtended` / `onRetracted` **are not part of the contract** (proven absence) | `niagara-help source-grep "onExtended\|onRetracted"` = 0/2603; `rg` over `…/control/` package = 0 hits | — (absence) |

## 772.2 — The base-class authoring contract `[CERT]`

One method is `abstract` (mandatory); everything else has a working default and is overridden only as needed.
All citations resolve in
`organized/docSource/docSource-doc/vineflower/control-rt/javax/baja/control/BPointExtension.java`.

| Method | Mandatory / Optional | What it does | Citation (`BPointExtension.java`) |
|---|---|---|---|
| `onExecute(BStatusValue out, Context cx)` | **MANDATORY** — the only `abstract` method | Runs every point cycle; author reads/mutates `out` (the point's working value) to apply the extension's effect | `:143` (`public abstract void onExecute(BStatusValue out, Context cx);`) |
| `requiresPointSubscription()` | Optional — default `false` | Return `true` to keep the parent point permanently subscribed (needed to see every value change, e.g. history when enabled / alarm when armed) | `:121` |
| `isParentLegal(BComponent parent)` | Optional — default requires the parent be a `BControlPoint` | Restrict which points may host this extension | `:161` |
| `isSiblingLegal(BComponent sibling)` | Optional — default rejects a same-type sibling (one instance per type) | Override to allow coexistence (history overrides it to `true`) | `:173` |
| `getParentPoint()` | **`final` — not overridable** | Returns the containing `BControlPoint` (null if the parent is not a control point); call it inside any override to reach the point | `:56` (`public final BControlPoint getParentPoint()`) |
| `started()` / `stopped()` (from `BComponent`) | Optional lifecycle | Set up / tear down subscriptions, timers, alarm support | see §772.5 |
| `changed(Property, Context)` (from `BComponent`) | Optional | Fires on the extension's **own** property changes — NOT the parent point's value (that arrives via `onExecute`). Guard with `Context.decoding` + `isRunning()` per [Block 730] | see §772.5 |

## 772.3 — How an extension observes the parent point's value `[CERT]`

The value is delivered as the `out` argument of `onExecute` — there is no separate "observe parent" callback.
`BControlPoint.doExecute()` drives it each cycle:

| Step | Mechanism | Citation (`BControlPoint.java`) |
|---|---|---|
| Point cycle routes to arbitration then extensions | `doExecute()` → `onExecute()` then `executeExtensions(working, null)` | `:285`, `:302` |
| Each extension is handed the current working value | `executeExtensions(BStatusValue out, Context cx)` iterates slots; for each `child instanceof BPointExtension` calls `ext.onExecute(out, cx)` | `:385`, `:391` |

`[INFER]` So an extension "observes" the parent by reading `out` inside `onExecute`; to observe it **off-cycle**
(e.g. on a timer) it calls `getParentPoint().getStatusValue()` directly, the pattern `BIntervalHistoryExt`
uses ([Block 552] §552.3).

## 772.4 — Multi-extension execution order `[CERT]`

Extensions execute in **slot-declaration order** (the order in `getProperties()` iteration = the Property Sheet
order), with the proxy extension always first. There is no priority field and no chain-of-responsibility; all
extensions run sequentially over the shared `out` working value.

| Claim | Citation |
|---|---|
| Extensions iterate in slot order and run unconditionally | `BControlPoint.java:385` (`executeExtensions`, `instanceof BPointExtension` loop) |
| The proxy extension is always first | `organized/docSource/docSource-doc/vineflower/control-rt/javax/baja/control/ext/BAbstractProxyExt.java:56` (verbatim: "The ProxyExt is always the first extension executed.") |
| Order is user-reorderable on the Property Sheet (proxyExt frozen first) | REMITTANCE [Block 734] §734.1 (slot-order execution) |

## 772.5 — Concrete exemplar confirmation `[CERT]`

Two shipped abstract subclasses show the contract in practice (only `onExecute` is mandatory; the rest are
selective overrides):

| Exemplar | Overrides used | Citation |
|---|---|---|
| `BHistoryExt` (history base) | `onExecute` (→ `pointChanged`), `started`, `stopped`, `changed`, `requiresPointSubscription`, `isSiblingLegal`→`true` (allow interval + COV on one point) | `organized/docSource/docSource-doc/vineflower/history-rt/javax/baja/history/ext/BHistoryExt.java` (`onExecute` region ~`:789`; `isSiblingLegal` ~`:607`) |
| `BAlarmSourceExt` | `onExecute` (→ `checkAlarms`, does NOT mutate `out` — notification only), `started`, `requiresPointSubscription` (returns alarm-enable state), `changed`, `isParentLegal` (adds numeric/discrete checks) | `organized/docSource/docSource-doc/vineflower/alarm-rt/javax/baja/alarm/ext/BAlarmSourceExt.java` (`onExecute` region ~`:1221`; `isParentLegal` ~`:1073`) |

`[INFER]` The exemplars confirm the authoring recipe: extend `BPointExtension`, implement `onExecute`, add
`requiresPointSubscription()`→`true` if you must see every change, and use `started`/`stopped` for
timers/subscriptions — mirroring [Block 552].

## 772.6 — What the official docs do NOT resolve `[CERT-doc]`

`niagara-help` documents extension **use**, not the authoring contract: `devguide-search "point extension"`
returns only `alarm.txt:18` (use `BAlarmSourceExt`) and `history.txt:44` (add `BHistoryExt`); the `class`
entry for `BPointExtension` confirms package `javax.baja.control`, `extends BComponent`, and `onExecute` as the
sole abstract method, but states no override-contract narrative. `devguide-search "BAbstractPointExt"` /
`"extension execution order"` = 0 hits. The authoring contract in §772.2–772.4 is reconstructed from source.

## 772.7 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Base class is `BPointExtension` (`javax.baja.control`, extends `BComponent`); `BAbstractPointExt` absent | `[CERT]` | `BPointExtension.java`; `fd`/`source-grep`/`corpus-nav` = 0 | Y — driver `fd` + `rg` |
| 2 | `onExecute` is the only abstract (mandatory) method | `[CERT]` | `BPointExtension.java:143` | Y — token verbatim |
| 3 | `requiresPointSubscription()` default `false` | `[CERT]` | `BPointExtension.java:121` | Y — token |
| 4 | `getParentPoint()` is `final` | `[CERT]` | `BPointExtension.java:56` | Y — token verbatim |
| 5 | `isParentLegal`/`isSiblingLegal` optional overrides | `[CERT]` | `BPointExtension.java:161,173` | Y — token |
| 6 | Parent value observed via `out` in `onExecute`, driven by `executeExtensions` | `[CERT]` | `BControlPoint.java:302,385,391` | Y — token |
| 7 | proxyExt always first; order = slot order | `[CERT]` | `BAbstractProxyExt.java:56`; `BControlPoint.java:385` | Y — verbatim string |
| 8 | `onExtended`/`onRetracted` are not in the contract | `[CERT]` | `source-grep` 0/2603; `rg` control pkg 0 | Y — driver `rg` |
| 9 | History/alarm exemplars follow the recipe | `[CERT]` | `BHistoryExt.java`, `BAlarmSourceExt.java` | Y — sub-agent, driver spot-checked base |
| 10 | Off-cycle observation via `getParentPoint().getStatusValue()` | `[INFER]` | from [Block 552] §552.3 | deduction |

**Tally:** `[CERT]` ×8 · `[CERT-doc]` ×1 · `[INFER]` ×2. `[INFER]`/`[CERT]` ratio ≈ 0.25 — EVIDENCE block, low
ratio, the base contract is fully sourced; residue is only the write-once execution PoC (see gaps).

## 772.8 — Kit implication → `build-n4-module-kit` `types/logic.md`

Add a **"Authoring a point extension"** recipe to `types/logic.md` (the kit currently documents control logic
but not the extension SPI):

1. **Extend `javax.baja.control.BPointExtension`** (extends `BComponent`). There is NO `BAbstractPointExt`;
   do NOT write `onExtended`/`onRetracted` (they don't exist).
2. **Implement the single abstract method** `onExecute(BStatusValue out, Context cx)` — read/mutate `out` (the
   point's working value). An alarm-style ext leaves `out` unchanged (notification only); a control-style ext
   mutates it.
3. **Override `requiresPointSubscription()` → `true`** only if the extension must see every value change.
4. **Reach the point** with the `final` `getParentPoint()`; for off-cycle work read
   `getParentPoint().getStatusValue()`.
5. **Restrict hosting** with `isParentLegal` / allow siblings with `isSiblingLegal` (default = one per type).
6. **Lifecycle**: use `started()`/`stopped()` for timers/subscriptions; `changed()` is for the ext's OWN
   properties (guard with `Context.decoding` + `isRunning()`), not the parent value.
7. **Execution order** is slot-declaration order; the proxyExt is always first — never assume your ext runs
   before proxy resolution.

## 772.9 — Connections & open gaps

- REMITTANCE (cited, not re-derived): [Block 6] taxonomy · [Block 536] execution pipeline/`onExecute` ·
  [Block 552] history/alarm ext exemplars · [Block 734] slot-order execution · [Block 730] `changed()`
  discipline · [Block 738] adding a proxyExt.
- Open child gap **MAE1-G1** (requires-execution): build and station-test a minimal `BPointExtension` subclass
  to confirm the authoring recipe end-to-end (registration `@NiagaraType`, palette entry, `onExecute` firing
  order against a live point). Deferred — needs a build+deploy, not read-only source.
