# Block 591 — The template binding editors: `BTemplateConfigEditor` and `BTemplateIOEditor` — the Workbench tables where an engineer AUTHORS what a template exposes, writing `BConfigBinding` rows keyed by component HANDLE (with a legacy composite-link migration)

**Session**: 2026-08-28
**Focus**: `template-wb` (gap TW1 — the two substantive binding editors; the core UI for parameterizing a template)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BTemplateConfigEditor` (1675 L) + `BTemplateIOEditor` (1853 L); the
binding model, the handle-keyed save, and the migration token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-wb/vineflower/com/tridium/template/ui/{BTemplateConfigEditor,BTemplateIOEditor}.java`.

**Scope**: the Workbench authoring UI for template bindings — the human side of the `<bindings>` manifest schema.
[B200 §200.6] gave the template-wb overview; TW1 opens the two editors it left unopened. Does NOT re-derive the
binding CONTRACT ([B200 §200.3]) or the manifest schema ([Block 580] T3) — this is the UI over them; the tag
chooser it uses is [B260–B270].

---

## 591.1 `BTemplateConfigEditor` — a table over `BConfigBinding`s [CERT]

`BTemplateConfigEditor extends BEdgePane` `[CERT] :90`, with a `BTable` driven by an inner `BindingModel`
`[CERT] :171`. It loads the template's config bindings — `BConfigBinding[] bindings =
templateConfig.getConfigBindings()` `[CERT] :185` — and renders one ROW per binding (an inner `BindingSlot`
carries the editable row state). Each row exposes a `BConfigBinding`'s fields, including its `userTip`
`[CERT] :206`. Add/remove/edit rows is the workflow; a `removed` list `[CERT] :142` tracks deletions until Save.

So the Config editor is where an engineer decides WHICH slots of the template's components become tunable
parameters — each row is one exposed config value ([Block 580] T3's `<settings>`/`<bindings>` `Value`).

## 591.2 Save writes bindings keyed by HANDLE [CERT]

On Save `[CERT] :357`:
```java
BConfigBinding binding = new BConfigBinding(targetComp.getHandleOrd(), bindingSlot.name,
                                            bindingSlot.slotOrAttribute, bindingSlot.userTip);
```
The binding is constructed from `targetComp.getHandleOrd()` — the target component's **handle ORD**, not its slot
path. This is the same durability choice as the upgrade transfer ([Block 579] §579.2): a binding addressed by
HANDLE survives the component-tree swap a template deploy/upgrade performs, so the exposed parameter keeps
pointing at the right slot after re-deployment. The editor rewrites the full `BConfigBinding[]` on Save.

## 591.3 A legacy migration is built in [CERT]

The editor carries a one-way migration `[CERT] :180`:
```java
BDialog.info(this.owner, "Converting Template Configuration property Composite Links to BConfigBindings. Must click Save to complete.");
```
So older templates whose config was expressed as **property composite links** are converted to `BConfigBinding`s
when opened in this editor (completed on Save). This is why the corpus's older template blocks and the current
binding model differ — the UI upgrades the representation on edit.

## 591.4 `BTemplateIOEditor` — I/O bindings plus tag panes [CERT]

`BTemplateIOEditor extends BEdgePane implements TemplateConst` `[CERT] :97` is the sibling editor for INPUT/OUTPUT
bindings. It runs THREE `BTable`s `[CERT] :124-126`: the main `table` (the I/O bindings) plus `sourceTagTable`
and `ioTagTable` — two tag panes driven by `TagSupport` `[CERT] :8` (the tag chooser, [B260–B270] REMITTANCE). So
the IO editor defines which inputs and outputs the template exposes AND lets the author attach source/IO tags to
them. Like the Config editor, it reads/writes `BConfigBinding` `[CERT] :5` (I/O bindings share the same binding
type, distinguished by the `in`/`out` `typ`, [Block 580] §580.2).

## 591.5 Thesis [CERT-synthesis]

The two editors are the human counterpart to the manifest schema ([Block 580]): the Config editor authors the
`cfg`/`str`/`num`/`bool` `Value` bindings (tunable config), the IO editor authors the `in`/`out` bindings (wiring
exposure) plus tags — both persisted as `BConfigBinding`s keyed by handle so they survive re-deployment. There is
no new engine logic here; the value of TW1 is confirming that (a) the authoring UI is a straightforward table
over the same `BConfigBinding` model the rt engine ([Block 577]/[Block 580]) consumes, (b) handle-addressing is
enforced at authoring time (not just at upgrade), and (c) the editor silently migrates legacy composite-link
templates forward.

## 591.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BTemplateConfigEditor (BEdgePane) = BTable/BindingModel over templateConfig.getConfigBindings(); rows carry userTip; removed list | [CERT] | BTemplateConfigEditor.java:90,142,171,185,206 | token-checked ✓ |
| 2 | Save constructs BConfigBinding(targetComp.getHandleOrd(), name, slotOrAttribute, userTip) — keyed by HANDLE | [CERT] | :357 | token-checked ✓ |
| 3 | Built-in migration: composite links → BConfigBindings on Save | [CERT] | :180 | token-checked ✓ |
| 4 | BTemplateIOEditor (BEdgePane, TemplateConst) = 3 BTables (I/O + source/io tag panes via TagSupport), reads/writes BConfigBinding | [CERT] | BTemplateIOEditor.java:97,124-126,8,5 | token-checked ✓ |
| 5 | Editors = UI over the same BConfigBinding model the manifest/rt engine uses; no new engine logic | [CERT-synthesis] | rows 1-4 + [B580] | reasoned ✓ |

**Marker tally**: [CERT] ×4 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 4 of 5
rows token-verified inline.

## Connections

- **[B200 §200.6]** — the template-wb overview; TW1 opens the Config/IO editors.
- **[B200 §200.3]/[Block 580]** (T3) — the binding contract + manifest `<bindings>` these editors author.
- **[Block 579]** (T5) — handle-keyed bindings survive the upgrade swap; the editor enforces handle-addressing at
  authoring time.
- **[B260–B270]** — the tag chooser (`TagSupport`) the IO editor embeds.

## Open gaps (this block)

- The optional-component node tree and double-click column dispatch (edit widgets per column) are UI detail,
  named not fully traced — low value. Focus continues at TW2 (the Excel IMPORT path).
