# Block 430 — The property sheet and Swing field-editor dispatch: @AgentOn on the value's type, committed in a Transaction

> Research of the Workbench **property sheet + slot sheet + the Swing field-editor dispatch** (focus
> `workbench`, gap WB04) — how a component's slots become editable rows. Scope: `BPropertySheet` vs
> `BSlotSheet`, the `BWbFieldEditor` base, the TWO-TIER dispatch that picks an editor for a slot's type, a
> concrete example, and the commit path. Builds on [Block 428]'s `@AgentOn` view pipeline. Does NOT cover the
> WEB field editors ([Block 421], `webEditors`) or PX field editors ([Block 214]) — this is their Swing twin.
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `workbench-wb.jar`
> sha256 `17a84e2a26a6f6af0e1893738115ebb1ac7e002d3af8c11409fa4ee17f3d7c8c`.
>
> Sources: Tridium docSource (`sources/tridium-src/workbench-wb/.../fieldeditor/BWbFieldEditor.java`) +
> Vineflower impl (`sources/decompiled/workbench-wb/com/tridium/workbench/{propsheet,slotsheet,fieldeditors}/`,
> clean). Method: docSource for the FE contract, Vineflower for the dispatch, all lines re-verified live.
> Markers: `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 428] (the shell opens these as `@AgentOn` views on `baja:Component`),
> [Block 421] (web FE twin — same `@AgentOn`-on-value-type pattern), [Block 214] (PX FE twin), [Block 408]
> (BComponentSpace — the `Transaction` the commit uses).

---

## 430.1 — Property sheet vs slot sheet: curated values vs raw schema `[CERT]`

Both are `BWbComponentView`s `@AgentOn` on `baja:Component`/`baja:IPropertyContainer`, but differ by intent and
permission: `[CERT]`

| View | Perm | Enumerates | Shows | Citation |
|---|---|---|---|---|
| `BPropertySheet` | `r` (read) | `getPropertiesArray()` — only `Property` slots, minus `HIDDEN`/`BRelation`/`BAction`/`BTopic`/`BWsAnnotation` | editable VALUE rows (field editors) | `propsheet/BPropertySheet.java:20`–`:24` |
| `BSlotSheet` | `W` (write) | `getSlotsArray()` — ALL slots (props+actions+topics), no filtering | raw SCHEMA table (type/index/name/flags/facets), no editors | `slotsheet/BSlotSheet.java:40`–`:43`,`:154` |

`[INFER]` the property sheet is the everyday editor (curated, read-gated to open); the slot sheet is the
metadata inspector (raw, write-gated because renaming/reordering slots is a structural edit). Note `wsAnnotation`
([Block 429]) is explicitly filtered OUT of the property sheet — wire-sheet layout never shows as a property.

## 430.2 — The field-editor base contract `[CERT]`

`BWbFieldEditor extends BWbEditor` (`sources/tridium-src/workbench-wb/javax/baja/workbench/fieldeditor/BWbFieldEditor.java`).
Subclasses override `doLoadValue(BObject, Context)` / `doSaveValue(BObject, Context)`; the modified/commit
state (`isModified`/`setModified`/`clearModified`) lives in the `BWbEditor` superclass. `[CERT]` A static
`makeFor(BObject obj, Context cx, BWbShell shell)` (`BWbFieldEditor.java:77`) resolves an editor for a value —
the same logic the property sheet inlines (§430.3). `[CERT]`

## 430.3 — DISPATCH: two-tier, keyed on the VALUE's type `[CERT]`

This is the load-bearing mechanism (`propsheet/BComplexEntry.getEditorType`): `[CERT]`

```java
String explicit = facets.gets("fieldEditor", null);          // :308  TIER 1: facet override
if (explicit != null) return Sys.getRegistry().getType(explicit).getAgentInfo();
AgentList agents = kid.getAgents().filter(BWbFieldEditor.getAgentFilter(shell));  // :321  TIER 2
return agents.getDefault();                                   // :322  lowest-ordinal default
```

1. **TIER 1 — facet override**: if the slot's `BFacets` carries a `"fieldEditor"` type-spec string, that exact
   type is used, bypassing the registry. `[CERT]`
2. **TIER 2 — `@AgentOn` registry**: otherwise, `kid.getAgents()` is called on the slot's **runtime VALUE**
   (`kid`), filtered to agents that `is-a BWbFieldEditor`, and `getDefault()` (lowest ordinal) wins. `[CERT]`

This is the SAME `@AgentOn` agent dispatch the shell uses for views ([Block 428] §428.5) — keyed on the
object's TYPE. `[INFER]` one mechanism ("install a type `@AgentOn(types=...)`, the framework finds it")
selects views, field editors, sidebars, and PX/web editors alike. `makeEntry` then branches: a non-simple
value whose default editor is `BPropertySheetFE` becomes a collapsible `BComplexEntry`; everything else an
inline `BAtomicEntry` wrapping the concrete editor. `[CERT]`

## 430.4 — Concrete editors: type → FE by @AgentOn `[CERT]`

| Value type | Field editor | @AgentOn | Citation |
|---|---|---|---|
| `baja:Boolean` | `BBooleanFE` (2-item `BListDropDown`, `trueText`/`falseText` facets) | `@AgentOn(types={"baja:Boolean"})` | `fieldeditors/BBooleanFE.java:21`–`:25` |
| `baja:FrozenEnum` | `BFrozenEnumFE` (dropdown from `getRange().getOrdinals()`) | `@AgentOn(types={"baja:FrozenEnum"})` | `fieldeditors/BFrozenEnumFE.java` |
| `baja:Complex` / `IPropertyContainer` | `BPropertySheetFE` (recursive nested sheet — the expand trigger) | `@AgentOn(types={"baja:Complex","baja:IPropertyContainer"})` | `propsheet/BPropertySheetFE.java:14`–`:15` |

`[CERT]` So a boolean slot renders a true/false dropdown, an enum a range dropdown, a complex value a
collapsible sub-sheet — each chosen purely by the value's type via `@AgentOn`.

## 430.5 — Commit: dirty → shell Save → one Transaction `[CERT]`

Editing does not auto-save. `BAtomicEntry` links the editor's `pluginModified` topic to `setDirty()`, and its
`actionPerformed` (e.g. Enter) to the shell Save command. `[CERT]` `[INFER]` On Save, `BFieldEditorSheet.doSaveValue`
opens ONE `Transaction` over the mounted component, walks the entry tree calling `save` only on dirty entries,
and `BAtomicEntry.doSave` does `parent.target.asComplex().set(property, editor.saveValue(target, cx), cx)`
(`propsheet/BAtomicEntry.java:131`,`:137`). `[CERT]` So all edits in a property sheet commit as a single
transactional write-back to the component's slots. `[INFER]` this is why a property-sheet Save is atomic and
undoable at the component level — it rides the same `Transaction`/`BComponentSpace` machinery [Block 408]
documented.

## 430.6 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | `BPropertySheet` `@AgentOn(baja:Component,r)` = curated Property rows (filters hidden/action/topic/wsAnnotation) | `[CERT]` | `propsheet/BPropertySheet.java:20` |
| 2 | `BSlotSheet` `@AgentOn(...,W)` = raw `getSlotsArray()` schema table, no editors | `[CERT]` | `slotsheet/BSlotSheet.java:40`,`:154` |
| 3 | FE base = `BWbFieldEditor extends BWbEditor`; `doLoadValue`/`doSaveValue`; static `makeFor` | `[CERT]` | `BWbFieldEditor.java:77` |
| 4 | Dispatch = TIER1 facet `"fieldEditor"` override, else TIER2 `kid.getAgents().filter(FE).getDefault()` | `[CERT]` | `BComplexEntry.java:308`,`:321`,`:322` |
| 5 | Concrete: `baja:Boolean`→`BBooleanFE`, `baja:Complex`→`BPropertySheetFE` (via @AgentOn) | `[CERT]` | `BBooleanFE.java:21`; `BPropertySheetFE.java:14` |
| 6 | Commit = dirty→shell Save→one `Transaction`→`complex.set(prop, saveValue(...), tx)` | `[CERT]` | `BAtomicEntry.java:131`,`:137` |
| 7 | Same `@AgentOn`-on-value-type mechanism as views (B428), web FE (B421), PX FE (B214) | `[INFER]` | §430.3 |

**Marker tally**: `[CERT]` ≈ 22 · `[INFER]` 6 ([INFER]/[CERT] ≈ 0.27). Type: **EVIDENCE block** (model
overview) — ratio healthy. VERIFY-BEFORE-ACTING: every dispatch and commit line re-verified live; the concrete
FE `@AgentOn` annotations (not returned by the sweep's first grep) were confirmed by re-reading
`BBooleanFE.java:21` and `BPropertySheetFE.java:14` before citing them. No mangling. Tokens confirmed:
`getPropertiesArray`/`getSlotsArray`, `facets.gets("fieldEditor")`, `agents.getDefault()`, `@AgentOn baja:Boolean`,
`complex.set(prop,...,tx)`.

## 430.7 — Connections

- **[Block 428]** — the shell resolves `BPropertySheet`/`BSlotSheet` as views via the SAME `@AgentOn` pipeline;
  §430.3 shows field editors use the identical agent dispatch one level down.
- **[Block 421]** / **[Block 214]** — the web and PX field-editor stacks are the twins of this Swing one;
  all three key an editor to the value's type via `@AgentOn`. This block is the desktop member of the trio.
- **[Block 408]** — the commit `Transaction` is the `BComponentSpace` transaction machinery.
- **[Block 429]** — `wsAnnotation` is filtered out of the property sheet, confirming wire-sheet layout is
  intentionally invisible as a property.

<!-- research-block: focus workbench, gap WB04 (property sheet + slot sheet + Swing field-editor dispatch) — CLOSED at body grade -->
