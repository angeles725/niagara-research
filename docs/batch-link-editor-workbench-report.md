# Implementation Report — Batch Link Editor (Niagara N4 Workbench View)

> Hand-off brief for implementing a custom Workbench view that lets a user create many
> wire-sheet links (Link From / Link To) against the slots of the current "equip"
> component in **batch**, picking the other endpoint from a navigable tree of the whole
> station, and applying everything at once with a single **Save** button.
>
> All API facts below were verified against decompiled source of **Niagara N4.14.0.162**
> (`niagara-help/source`). Each is tagged **[CERT]** (read from code, with `file:line`) or
> **[INFER]** (reasoned, must be confirmed at build time). Code identifiers are in English.

---

## 0. Target UX (what the user asked for)

- A window/view bound to the current **equip** `BComponent`.
- **Left panel**: every linkable slot of the equip (properties / actions / topics).
- **Right panel**: a tree of the **entire station** (Config, Files, Hierarchy, History, Alarm
  spaces) that lazily expands on click, **plus a search box** that filters components by name.
- **Flow**: select an equip slot → mark it as **From** or **To** → pick the remote point on the
  right → choose its slot → **Accept**/**Cancel** → repeat for as many links as needed → a final
  **Save** button commits all pending links at once and closes.

**Verdict: fully feasible as a Workbench Java view.** The right-hand tree already exists as a
reusable widget (`BNavTree`), and the link model supports build-in-memory + validate + batch-commit.

---

## 1. Platform decision

**Build it as a Workbench plugin view: a class extending `BWbComponentView`.** Rationale:

- The station tree the user described **is** the Workbench Nav tree — available as the
  `BNavTree` widget for free (lazy-expand, all spaces). A web dashboard would require
  reimplementing that tree via servlets/ORDs from scratch.
- The link API (`BLink`, `checkLink`, `makeLink`, `Transaction`) is plain Java, callable
  directly from the view — local station or remote station over Fox (proxy space) both work.

---

## 2. Link model — what the implementer MUST understand

### 2.1 What a `BLink` is  [CERT]
- `BLink extends BRelation extends BStruct extends BValue` — `baja/javax/baja/sys/BLink.java:114`.
- A link is a **dynamic child slot of the TARGET component**. Class comment: *"The target side is
  always the link's parent and source side is a component-slot pair which pushes events to the
  target."* (`BLink.java:28-29`).
- Key persistent properties (`BLink.java`): `sourceOrd:BOrd` (:163), `sourceSlotName:String`
  (:174), `targetSlotName:String` (:197), `enabled:boolean` (:220), `relationId` fixed to
  `"n:dataLink"` (:128, HIDDEN+READONLY).
- Constructors: `BLink(BOrd sourceOrd, String sourceSlot, String targetSlot, boolean enabled)`
  (:254, **persistent/indirect** — use this), `BLink(BComponent source, Slot src, Slot tgt)`
  (:265, transient/direct), `BLink()` (:280).

### 2.2 Direction semantics — who owns the link  [CERT, critical]
The owner (where the `BLink` is `add()`-ed) flips with direction. Define it explicitly in the model:

| User action on an equip slot `E` against a remote slot `R` | source | target (owner of BLink) |
|---|---|---|
| **Link FROM** (equip slot is fed *from* the remote) | remote `R` | equip `E` |
| **Link TO** (equip slot feeds *to* the remote) | equip `E` | remote `R` |

So a batch mixing From and To will `add()` links onto **different** components. That's fine —
the `Transaction` is created on the **space**, not on one component, so it can touch many.

### 2.3 Legal slot combinations  [CERT] (`LinkCheck.java`)
Allowed: property→property, property→action, action→action, action→topic, topic→action,
topic→topic. **Rejected**: action→property (:187), topic→property (:229), property→topic (:173).

Target-slot rules (prop→prop): not `READONLY` (:149), not `METADATA` (:72), not already linked
unless it has `FAN_IN` (:152), source must not be a `BComponent` unless `BVector` (:150).
Unlinkable endpoints: source implementing `BIUnlinkableSource` (:64) or target
`BIUnlinkableTarget` e.g. `BPassword` (:60). Type mismatch with no adapter → invalid (:299,
`"linkcheck.mismatchedTypes"`).

### 2.4 Dry-run validation (no side effects)  [CERT]
```java
// BComponent.java:1580
LinkCheck check = target.checkLink(source, sourceSlot, targetSlot, cx);
if (!check.isValid()) { String reason = check.getInvalidReason(); /* show, skip */ }
```
`checkLink` mutates nothing — call it on every pending link before applying anything.
There is **no `BLinkException`**; errors are reported via `LinkCheck.isValid()` /
`getInvalidReason()`.

### 2.5 Building the link  [CERT]
```java
// BComponent.java:1610 — returns a BLink, or a BConversionLink if types differ but an adapter exists
BLink link = target.makeLink(source, sourceSlot, targetSlot, cx);
```

---

## 3. Batch apply (the Save button)  [CERT]

Use one `Transaction` on the component space; `add` each link with the transaction as `Context`;
`commit` once.

```java
// space from any component in the station:
BComponentSpace space = equip.getComponentSpace();        // BComplex/BComponent
Transaction tx = space.newTransaction(cx);                // BComponentSpace.java:464

for (PendingLink p : pending) {                            // already validated in step 2.4
    BLink link = p.target.makeLink(p.source, p.sourceSlot, p.targetSlot, tx);
    String name = uniqueSlotName(p.target, "Link");        // see 3.1
    p.target.add(name, link, tx);                          // BComponent.java:899 (Property add(String,BValue,Context))
}
tx.commit();                                               // applies all ops; pattern doc Transaction.java:22
```

Facts the implementer must respect:
- **No real rollback.** `SyncBuffer.commit()` applies ops sequentially; if one throws on a local
  space the exception propagates but earlier ops are already applied. **Strategy: validate ALL
  with `checkLink` first, then apply.** (Residual risk: another client mutates the station between
  validate and commit — handle by surfacing the commit error.)
- **Nested transactions are illegal** — `Transaction.java:106` throws if the base context is
  already a `Transaction`. Build your own `cx`, don't nest.
- `add()` auto-activates the link (installs the `Knob` on the source) once the component is
  running — no explicit `activate()` needed. (`BLink.java:369`).
- After `commit`, links are live in memory. To persist to `config.bog`, mark the view modified
  (`setModified()`) and let the shell's Save run, or invoke `BWbShell.getSaveCommand()`. The added
  slots make their owners dirty, so a normal station save persists them. **[INFER]** — confirm the
  exact persistence trigger on your station setup.

### 3.1 Unique slot name  [CERT pattern]
```java
String uniqueSlotName(BComponent target, String base) {
    if (target.getSlot(base) == null) return base;        // BComplex.getSlot:516
    for (int i = 1; ; i++) { String n = base + i; if (target.getSlot(n) == null) return n; }
}
```
(Mirrors the native `LinkCommand` which generates `"Link"`, `"Link1"`, …)

---

## 4. The view shell

### 4.1 Class + registration  [CERT pattern]
```java
package com.example.linktool;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.*;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
  agent = @AgentOn(types = "baja:Component", requiredPermissions = "rwi") // narrow to your equip type if you have one
)
public final class BBatchLinkEditor extends BWbComponentView {
  @Override public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BBatchLinkEditor.class);

  @Override protected void doLoadValue(BObject value, Context cx) throws Exception {
    BComponent equip = (BComponent) value;
    buildUi(equip);            // left: equip slots, right: BNavTree + search, bottom: Save
  }
  @Override protected BObject doSaveValue(BObject value, Context cx) throws Exception {
    commitPending(cx);         // section 3
    return value;
  }
}
```
- `BWbComponentView` (`workbench-wb/javax/baja/workbench/view/BWbComponentView.java:30`) auto-registers
  for component events. Registration is by the `@AgentOn` annotation processed by Slot-o-Matic at
  build (no manual `module-include.xml` entry for the agent). **[INFER]** confirm your build runs
  the Niagara annotation/Slot-o-Matic step so `_BBatchLinkEditor` is generated.
- `requiredPermissions`: needs invoke/write to add links — use `"rwi"`.

### 4.2 Widgets  [CERT — all `bajaui-wb`, package `javax.baja.ui.*`]
| Need | Class | Where |
|---|---|---|
| L/R split | `javax.baja.ui.pane.BSplitPane` | `BSplitPane.java:103` (`BSplitPane(BOrientation.horizontal, 30)`) |
| Equip slot list | `javax.baja.ui.list.BList` | `BList.java:102` (`addItem`, `getSelectedItem`, topic `actionPerformed`) |
| Station tree | `javax.baja.workbench.nav.tree.BNavTree` | section 5 |
| Search box | `javax.baja.ui.BTextField` | `BTextField.java:62` |
| Buttons | `javax.baja.ui.BButton` + `Command` | `BButton.java:24` |
| Scroll | `javax.baja.ui.pane.BScrollPane` | — |
| Edge/grid layout | `javax.baja.ui.pane.BEdgePane` / `BGridPane` | — |
| Modal confirm | `javax.baja.ui.BDialog` | `BDialog.java:36` (`BDialog.open(owner,title,content,BDialog.OK_CANCEL,null)`) |

Set the root: `setContent(splitPane)` (`BWbPlugin.setContent`).

### 4.3 Buttons via Command  [CERT]
```java
Command save = new Command(this, lex, "save") {
  public CommandArtifact doInvoke() throws Exception { saveValue(null); return null; }
};
new BButton(save);
```

---

## 5. Right panel — station tree  [CERT]

```java
BNavTree tree = new BNavTree();          // BNavTree.java:104 — same root as the WB Nav sidebar
// or new BNavTree(navRoot) to scope to one host/station.
```
- `workbench-wb/javax/baja/workbench/nav/tree/BNavTree.java:48`, extends `BTree`.
- Lazy-expand is built in (`NavTreeNode.getChildren()` / `buildChildren()` :184-226).
- Selection → component:
```java
BObject sel = tree.getSelectedObject();              // BNavTree.java:116 (returns BObject)
if (sel instanceof BComponent) {                      // station nav nodes ARE BComponents (NavTreeNode.java:260)
  BComponent remote = (BComponent) sel;
}
```
- If a node is not a `BComponent` (e.g. a File space node), resolve via `((BINavNode)sel).getNavOrd()`
  then `.resolve(...).get()` — but for linking you only care about `BComponent` nodes; ignore others.

Spaces appear automatically under the local host: Config = `station:|slot:/`, Files =
`local:|file:/`, plus Hierarchy/History/Alarm. **[INFER]** exact space ORDs — the tree shows them
without you wiring ORDs manually.

---

## 6. Listing slots  [CERT — `baja/javax/baja/sys/BComplex.java`]

For the left panel (equip) and for the remote slot picker:
```java
for (SlotCursor<Property> c = comp.getProperties(); c.next(); ) { Property p = c.get(); ... }  // :624
for (SlotCursor<Action>   c = comp.getActions();    c.next(); ) { Action a = c.get(); ... }    // :640
for (SlotCursor<Topic>    c = comp.getTopics();     c.next(); ) { Topic t = c.get(); ... }     // :656
// or arrays: getPropertiesArray()/getActionsArray()/getTopicsArray()/getSlotsArray()
int flags = comp.getFlags(slot);
if ((flags & Flags.HIDDEN) != 0) continue;            // Flags.java:183-191 — skip hidden
Slot slot = comp.getSlot(name);                        // BComplex.java:516 — name -> Slot
```
Show slot kind from `slot.isProperty()/isAction()/isTopic()`. Optionally pre-filter the remote
picker with `target.checkLink(...)` per candidate so only valid pairs are selectable.

---

## 7. Search box  [CERT]

Primary: `BSearchService` (NEQL).
```java
BSearchService svc = BSearchService.getService();      // search-rt/.../BSearchService.java
// build BSearchParams with query "neql:displayName like '*<text>*'" scoped to station:, run executeSearch(params)
```
Simple alternative (no service): iterate `BINavNode.iterateNavDescendants()` from the station root
and filter `getNavDisplayName(cx)` by substring, then `expandToNavNode(node)` /
`expandToOrd(base, ord)` on the `BNavTree` to reveal the hit. **[INFER]** exact NEQL `like` syntax —
confirm against `neql-rt` (no source in this tree); `displayName=="..."` exact-match is safe.

---

## 8. Pending-link data model (in memory)

```java
final class PendingLink {
  BComponent source, target;     // resolved per direction (section 2.2)
  Slot sourceSlot, targetSlot;
  String label;                  // for the pending-list UI
  String validationReason;       // null if checkLink passed
}
List<PendingLink> pending = new ArrayList<>();
```
Build each `PendingLink` from (equip slot, direction From/To, remote component, remote slot),
run `checkLink` immediately to show valid/invalid, append. The final Save commits the whole list.

---

## 9. End-to-end flow

1. `doLoadValue` → render: left `BList` of equip slots (filter HIDDEN), right `BNavTree` + search
   `BTextField`, a pending-links `BList`, and a Save `BButton`.
2. User selects an equip slot, picks **From** or **To** (two small buttons / a toggle).
3. User selects a remote `BComponent` in the tree (or via search) → show its valid slots
   (filtered by `checkLink`) in a small picker → **Accept** (`BDialog.OK`) builds a `PendingLink`,
   validates, and appends to the pending list; **Cancel** discards.
4. Repeat. Allow removing rows from the pending list.
5. **Save**: validate all (`checkLink`), if any invalid show them and stop; else open one
   `Transaction`, `makeLink`+`add(tx)` each, `commit()`, mark the view modified / trigger station
   save, then close.

---

## 10. Gotchas / limits (call these out to the implementer)

- **Validate-all-then-apply** — no transactional rollback (section 3).
- **Direction changes the owner component** (section 2.2) — get this wrong and links land on the
  wrong side / fail validation.
- **FAN_IN**: a property target already linked rejects a second link unless it has `FAN_IN`.
- **Type conversion**: `makeLink` may return a `BConversionLink` (fine) or `checkLink` fails on
  mismatch with no adapter — surface the reason text.
- **Remote stations**: works over Fox proxy spaces, but every `checkLink`/`add` is a round-trip;
  for big batches keep the validation pass tight.
- **Slot-o-Matic / `@NiagaraType`** annotations are build-time — the project must run the Niagara
  Gradle build so the generated `_*` companion and the agent registration exist. **[INFER]**
- The native `BLinkPad` dialog only returns **one** slot-pair per open — do **not** try to reuse it
  for heterogeneous batches; build the picker yourself on top of `checkLink`/`makeLink`/`add`.

---

## 11. Optional reuse of the native machinery

If you ever want homogeneous batches (same slot-pair across many components) you can reuse the
native `LinkCommand` (`workbench-wb/javax/baja/workbench/commands/LinkCommand.java:79`) — pass
non-null slot names and it applies the cartesian product with undo/redo for free. For the
heterogeneous, accumulate-then-save flow the user wants, the **base API path
(`checkLink` → `makeLink` → `Transaction.add` → `commit`) is the right choice** — sections 2-3.

---

## 12. API quick reference (all verified `file:line`, N4.14.0.162)

| Symbol | Signature / note | Source |
|---|---|---|
| `BComponent.checkLink` | `LinkCheck checkLink(BComponent, Slot, Slot, Context)` | `baja/.../sys/BComponent.java:1580` |
| `BComponent.makeLink` | `BLink makeLink(BComponent, Slot, Slot, Context)` | `BComponent.java:1610` |
| `BComponent.add` | `Property add(String, BValue, Context)` | `BComponent.java:899` |
| `BComplex.getSlot` | `Slot getSlot(String)` | `BComplex.java:516` |
| `BComplex.getProperties/Actions/Topics` | `SlotCursor<…>` | `BComplex.java:624/640/656` |
| `BComponentSpace.newTransaction` | `Transaction newTransaction(Context)` | `space/BComponentSpace.java:464` |
| `Transaction` | implements `Context`; `.commit()`; no nesting (:106) | `sync/Transaction.java:40` |
| `BLink` | ctor `(BOrd,String,String,boolean)` (:254); link is child of target | `sys/BLink.java:114` |
| `LinkCheck` | `.isValid()`, `.getInvalidReason()` | `sys/LinkCheck.java` |
| `BWbComponentView` | base class for the view | `workbench-wb/.../view/BWbComponentView.java:30` |
| `BNavTree` | station tree widget; `getSelectedObject():BObject` | `workbench-wb/.../nav/tree/BNavTree.java:48/116` |
| `NavTreeNode` | nav nodes are `BComponent` | `…/nav/tree/NavTreeNode.java:260` |
| `BSplitPane` | `(BOrientation, double)` | `bajaui-wb/.../ui/pane/BSplitPane.java:103` |
| `BList` | `addItem`, `getSelectedItem`, topic `actionPerformed` | `bajaui-wb/.../ui/list/BList.java:102` |
| `BTextField` | search box | `bajaui-wb/.../ui/BTextField.java:62` |
| `BButton` / `Command` | button + action | `bajaui-wb/.../ui/BButton.java:24` |
| `BDialog` | `open(owner,title,content,OK_CANCEL,null)` | `bajaui-wb/.../ui/BDialog.java:36` |
| `Flags` | `READONLY/HIDDEN/SUMMARY/FAN_IN/METADATA` | `sys/Flags.java:183` |
| `BSearchService` | `getService()`, `executeSearch(BSearchParams)` | `search-rt/.../search/BSearchService.java` |
| `LinkCommand` (native, optional) | reuse for homogeneous batches | `workbench-wb/.../commands/LinkCommand.java:79` |

---

## 13. Suggested build order for the implementer

1. Skeleton `BBatchLinkEditor extends BWbComponentView` + `@AgentOn`; get it to open on a component.
2. Left `BList` of equip slots (filter HIDDEN) with From/To toggle.
3. Embed `BNavTree`; on selection resolve to `BComponent`; list its valid slots via `checkLink`.
4. Accept/Cancel `BDialog` → build+validate `PendingLink` → pending `BList`.
5. Save: validate-all → `Transaction` → `makeLink`+`add` → `commit` → mark modified → close.
6. Add the search box (`BSearchService` or nav iteration + `expandToNavNode`).
7. Polish: remove-from-pending, reverse direction, error surfacing, conversion-link badge.
