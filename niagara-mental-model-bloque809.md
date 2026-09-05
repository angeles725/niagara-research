# B809 · Tridium `-wb` authoring conventions — the 5 checks (off-UI-thread traversal, @AgentOn breadth, wb lexicon, load/save lifecycle, deps) + a "good -wb artifact" doctrine, Tridium exemplars vs chihuahua-wb `[CERT]`

> **Scope**: how Tridium's OWN `-wb` (Workbench view) modules are built, distilled into 5 named checks with a
> hard-rule-vs-advisory verdict and a lintable-vs-review split, each contrasted against a real anti-pattern in our
> `chihuahua-wb`. Evidence base for the kit's "good -wb artifact" doctrine (C8 PR11). Does NOT re-derive the WB
> framework (view/command/plugin mechanism = REMITTANCE); adds only the authoring CONVENTIONS + the checks.
>
> **Sources**: FUENTE 3 (read-only, file:line [CERT], 4.14.0.162) — `workbench-wb` (`BSimpleJob`, `BWbEditor`,
> `WbViewEventWorker`, `workbench-wb.lexicon`), `alarm-wb` (`BAlarmConsole`/`BAlarmDbView`), `kitControl-wb`
> (`module.xml`, no lexicon). FUENTE 1 (own module, [CERT]): `chihuahua-wb/BBatchLinkEditor`. REMITTANCE:
> [B780] (palette/@AgentOn dual-surface), [B791] (web tier), [B751]/[B752] (wb-ux-authoring ladder), [B774] (BJob).
> All load-bearing cites grep-verified at the enclosing line this session (a delegated map is a hypothesis until read).

---

## 809.1 — THREAD1: station traversal runs OFF the UI thread `[CERT]` — HARD RULE
A Workbench view/command must NEVER walk the station tree (`getChildren` DFS, `loadSlots`, ORD `.get()`, subscribe) on
the Swing/EDT thread. **WHY (physics of the UI)**: the EDT is single-threaded; a station traversal is a BLOCKING Fox
call over the network — while it runs, the EDT cannot paint or respond, so all of Workbench (including the progress bar)
FREEZES for the full network round-trip. **Tridium's primitive**: `BSimpleJob.doRun()` (`BSimpleJob.java:19`) starts an
inner `JobThread extends Thread` (`:36`, `thread.start() :21`) that runs the developer's `run(Context)` (`:47`) on a
real OS thread; UI updates AFTER the job marshal back via `WbViewEventWorker.invokeLater()`
(`WbViewEventWorker.java:35`). **chihuahua anti-pattern [CERT]**: `BBatchLinkEditor` does its "Load Remote Slots" station
work inside a `Command.doInvoke()` (`BBatchLinkEditor.java:314-315`, and 8 other `doInvoke` blocks) on the UI thread — a
Fox slot-load that freezes Workbench. FIX = move the traversal into a `BSimpleJob.run()`.

## 809.2 — AGENT1: @AgentOn on the NARROWEST type + requiredPermissions `[CERT]` — HARD (nav-exposed) / advisory (embedded)
**WHY**: `@AgentOn(types=…)` decides on WHICH components your view appears in the right-click menu; a broad type pollutes
every component's menu, and omitting `requiredPermissions` on a nav-reachable view bypasses the RBAC gate. **Tridium**:
`BAlarmConsole` = `@AgentOn(types={"alarm:ConsoleRecipient"}, requiredPermissions="r")` (`BAlarmConsole.java:121-123`) —
one narrow type + read perm; `BAlarmDbView` = a 3-type union of the same contract + `"r"` (`BAlarmDbView.java:73-75`);
a maintenance view uses `"w"`. Field editors on a leaf value type omit perms (the Property Sheet already guards write).
**chihuahua anti-pattern [CERT]**: `BBatchLinkEditor` = `@AgentOn(types="baja:Component", requiredPermissions="rwi")`
(`:67`) — the BROADEST possible type (appears on EVERY component) with broad read-write-invoke perms. FIX = target the
specific equipment type(s), not `baja:Component`.

## 809.3 — WB-LEX1: ship a `<mod>-wb.lexicon` with view display-name keys `[CERT]` — ADVISORY
**WHY**: view tab titles and menu labels are localized FROM the lexicon; no lexicon → raw class names / un-localizable
UI. **Tridium**: `workbench-wb.lexicon` carries `DirectoryList.displayName=Directory List`, `HexFileEditor.displayName=
Hex File Editor`, etc. (`workbench-wb.lexicon:22,24`); driver-wb/alarm-wb/history-wb all ship one. **Exception (why it's
advisory not hard)**: `kitControl-wb` (2 field editors only) ships NO `.lexicon` — a tiny FE-only module may omit it.
**chihuahua anti-pattern [CERT]**: `chihuahua-wb` ships a `BWbComponentView` (BBatchLinkEditor) but NO `.lexicon` — its
view name is not localizable. Rule: any `-wb` with a named view SHOULD carry a lexicon display-name key.

## 809.4 — SCAFFOLD1: the load/save/modified lifecycle + the legitimate saveValue bypass `[CERT]` — HARD invariant
`BWbEditor` (base of view + field editor) enforces: `loadValue()` (final, `:146`) → `doLoadValue()` hook (`:163`) →
**`modified = false`** (`:183`); `saveValue()` (final, `:194`) → **`if (!isModified()) return`** (`:196`) → else
`doSaveValue()` (`:202`) → `modified = false` (`:211`). **The framework invariant [HARD]**: a view is clean right after
load — so calling `setModified()` inside `doLoadValue()` is a BUG (marks dirty on load → spurious "save?" prompt).
**Legitimate saveValue bypass**: (a) a READ-ONLY view overrides only `doLoadValue`, never `setModified` → `!isModified`
skips `doSaveValue` forever, no dirty prompt (BAlarmConsole/BAlarmDbView); (b) a button that saves ITSELF calls
`doSaveValue()` directly (because the final `saveValue()` no-ops when `!modified`) and deliberately does not `setModified`
— our `chihuahua-wb/BBatchLinkEditor` does exactly this CONSCIOUSLY, documenting it: *"do NOT route through saveValue():
BWbEditor.saveValue() is final and would silently no-op … invoke doSaveValue() directly"* (`:367-374`) and *"Calling
setModified() here would … prompt 'save changes?' forever (the button bypasses saveValue())"* (`:1087-1088`). **The check
is not "never bypass" — it is: IF you bypass saveValue (a button-driven save), the write MUST actually happen in your own
call AND the modified flag must be handled deliberately (never-set or clearModified), with a smoke-test that the save
lands.** chihuahua's bypass is legitimate-but-owed-a-smoke-test (its own `[INFER]` note at `:910`).

## 809.5 — DEP1: declare companion-rt + workbench-wb + bajaui-wb + what you reference `[CERT]` — HARD (core) / advisory (exhaustive)
**WHY**: a `-wb` module resolves its `@AgentOn` targets and base classes at Workbench start; missing the companion `-rt`
→ `ClassNotFound` when the type system loads the viewed types. **Tridium**: `kitControl-wb/module.xml` declares
`kitControl-rt` (companion), `workbench-wb`, `bajaui-wb`, plus `alarm-rt`/`baja`/`bajaux-*` for referenced types
(`module.xml:4-9`); alarm-wb declares 35+, exhaustively (even `baja`, which is implicit) — Tridium favors completeness
over minimalism. **Minimal correct set**: `<mod>-rt` + `workbench-wb` + `bajaui-wb` (+ `gx-rt` if graphics) + every
module whose types appear in code. **chihuahua anti-pattern [per audit]**: phantom transitive deps (modules listed but
never referenced) — bloat, ADVISORY; the HARD failure is a MISSING companion-rt.

## 809.6 — The "good -wb artifact" doctrine (copy-ready, 10 lines) `[INFER, grounded]`
1. Put ALL station traversal in a `BSimpleJob.run()`; never `getChildren`/`loadSlots` on the EDT (THREAD1).
2. Marshal post-job UI updates back with `WbViewEventWorker.invokeLater()`.
3. `@AgentOn` the NARROWEST specific type(s), never `baja:Component` (AGENT1).
4. Set `requiredPermissions` on every nav-reachable view (`"r"` read, `"w"` write); FE on a leaf value may omit.
5. Ship a `<mod>-wb.lexicon` with a `displayName` key per named view (WB-LEX1).
6. Populate widgets in `doLoadValue()`; NEVER call `setModified()` there (SCAFFOLD1 invariant).
7. Write back in `doSaveValue()`; if a button bypasses the final `saveValue()`, do the write itself + handle `modified` deliberately + smoke-test it.
8. `module.xml` deps = `<mod>-rt` + `workbench-wb` + `bajaui-wb` (+ `gx-rt` if graphics) + every referenced module; no phantom deps, never miss companion-rt (DEP1).
9. Keep the testable logic in a Baja-free `wb/model/` package (the [B762] DWB1 seam) so it unit-tests off-station.
10. Prefer NOT building `-wb` at all unless a Manager/View/FieldEditor is genuinely needed ([B751] rung-0).

## 809.7 — Which checks BITE as a lint vs stay human-review `[INFER — extends the C6 lintable/advisory doctrine]`
- **STATICALLY LINTABLE (hard-fail-able)**: AGENT1 broad `@AgentOn(types="baja:Component")` or a nav view with no
  `requiredPermissions` (grep the annotation); DEP1 missing `<mod>-rt` in module.xml. → `verify-module.sh` / a wb lint.
- **STATICALLY LINTABLE (WARN)**: WB-LEX1 (module ships a `BWbComponentView` subclass but no `.lexicon`); SCAFFOLD1
  narrow case (`setModified()` textually inside `doLoadValue()`).
- **HUMAN-REVIEW (not statically decidable)**: THREAD1 (does this method run on the EDT AND traverse the station? needs
  flow analysis — like the C6 poll-vs-subscribe); the saveValue-bypass legitimacy (button-save vs write-loss).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Off-UI-thread traversal = `BSimpleJob.JobThread` runs `run(cx)` on an OS thread; UI marshalled via WbViewEventWorker | [CERT] | BSimpleJob.java:19,21,36,47; WbViewEventWorker.java:35 |
| 2 | @AgentOn convention = narrowest type + requiredPermissions (r/w); Tridium exemplars | [CERT] | BAlarmConsole.java:121-123; BAlarmDbView.java:73-75 |
| 3 | wb lexicon ships displayName keys; kitControl-wb (FE-only) omits it → advisory | [CERT] | workbench-wb.lexicon:22,24; kitControl-wb has no .lexicon |
| 4 | BWbEditor lifecycle: loadValue→doLoadValue→modified=false; saveValue guards `!isModified` then doSaveValue→modified=false | [CERT] | BWbEditor.java:146,163,183,194,196,202,211 |
| 5 | chihuahua-wb anti-patterns: @AgentOn(baja:Component,rwi), Command.doInvoke UI-thread slot load, no lexicon, conscious saveValue bypass | [CERT] | BBatchLinkEditor.java:67,314-315,367-374,1087-1088; no .lexicon |
| 6 | Correct -wb dep set = companion-rt + workbench-wb + bajaui-wb + referenced; Tridium declares exhaustively | [CERT] | kitControl-wb/module.xml:4-9 |

**Tally**: 6 [CERT]. All file:line grep-verified this session (Tridium exemplars + chihuahua contrasts both confirmed in
source). §809.6/§809.7 doctrine + the WHY reasoning are [INFER] grounded in the [CERT] mechanism. Dedupe: the view/command
/plugin/agent framework is REMITTANCE ([B780]/[B751]/[B752]/[B774]); this block adds the CONVENTIONS + 5 checks + the
hard/advisory + lintable/review split.

## Connections
- **[B780]** (@AgentOn dual-surface registration — this adds the BREADTH convention), **[B751]/[B752]** (the wb/ux
  authoring ladder — when to build -wb at all), **[B774]** (BJob/BSimpleJob — the off-thread primitive), **[B762]** (DWB1
  `wb/model/` pure seam — doctrine line 9), **[B791]** (web tier), **[B788]** (the C6 lintable-vs-advisory meta-delta this
  extends). Kit: `types/wb-widgets.md` §"good -wb artifact" + the 5 checks; own residue: chihuahua-wb punch-list
  (move traversal off-EDT, narrow @AgentOn, add lexicon).

## Open gaps
- **B809-G1** (bounded): the `BWbManager`/`BAbstractManager` table-model lifecycle (`WbViewEventWorker.invokeLater` after
  a manager job) — named (BAbstractManager:448), not fully traced; a follow-up if the kit needs a Manager recipe.
- **B809-G2** (requires-execution): confirm chihuahua-wb's `Command.doInvoke` slot-load actually freezes Workbench on a
  live station (the EDT-block is [INFER] from the structure) — a live smoke-test.
