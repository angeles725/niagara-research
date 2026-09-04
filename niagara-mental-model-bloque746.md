# B746 · The module palette — authoring format (BOG XML), what ours exposes, and pre-configured assembly templates for commissioning

> **Scope**: how a module's Workbench palette is AUTHORED (the `module.palette` BOG-XML format), what our
> palettes currently expose, and the reuse opportunity — pre-wired equipment templates so commissioning is
> drag-one-thing. Complements B634 (the palette runtime READER). Clean docSource + our own files. Foco:
> module-best-practices.
>
> **Sources**: FUENTE 3 our `ColdRoomPan-rt/module.palette` + Tridium `aaphp-rt/module.palette` (read this
> session). FUENTE 1: B634 (reader), B12 §12.3.2 (format ref), module-anatomy B629/B632 (jar packaging).

---

## 746.1 — Format: `module.palette` is a BOG XML `[CERT]`
It is a plain `bajaObjectGraph` XML file at the module root, bundled into the `-rt` jar (`module.palette`).
A root folder holds one `<p>` entry per palette item:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<bajaObjectGraph version="4.0" ...>
<p m="b=baja" t="b:Folder">
  <p n="ColdRoom"          m="CRP=ColdRoomPan" t="CRP:ColdRoom"/>
  <p n="EvaporatorUnit"    m="CRP=ColdRoomPan" t="CRP:EvaporatorUnit"/>
  <p n="DefrostController" m="CRP=ColdRoomPan" t="CRP:DefrostController"/>
</p>
</bajaObjectGraph>
```
- `n=` display name in the palette · `m=` a module alias binding (`ALIAS=ModuleName`) · `t=` the type
  (`alias:TypeName`). Each `<p>` is a **pre-built component instance** the engineer drags into a station.
- The reader (`BModule.initNavChildren` → `BModulePaletteNode`, B634) discovers `module.palette` across ALL
  profile jars of the module, merges them, and exposes the folder as a browsable component SPACE
  (`BUnrestrictedFolder` root) in the Workbench nav tree — expandable like a station's tree.
- Nuance: Tridium's convention uses `t="b:UnrestrictedFolder"` for the root (ungated); ours uses `b:Folder`
  (works — the reader wraps it — but `UnrestrictedFolder` is the idiomatic ungated palette root).

## 746.2 — What OUR palettes expose today `[CERT]`
`ColdRoomPan-rt/module.palette` lists three BARE, default-configured instances: `ColdRoom`,
`EvaporatorUnit`, `DefrostController`. (CompPan-rt and DashboardPan-{rt,ux,wb} also ship a `module.palette`.)
So an engineer can drag each component individually — but must then nest and wire them by hand (a ColdRoom,
then add N EvaporatorUnits under it, then a DefrostController, then set `hasDefrost`, then link outputs).

## 746.3 — The reuse win: pre-configured assembly templates `[CERT/INFER]`
A `<p>` entry can carry NESTED `<p>` children and property overrides — so a palette item can be a WHOLE
pre-wired assembly, not a bare component. Example (a ready Room-3-style unit):
```xml
<p n="ColdRoom_2Evaps_Defrost" m="CRP=ColdRoomPan" t="CRP:ColdRoom">
  <p n="EvaporatorUnit_1" t="CRP:EvaporatorUnit"><p n="hasDefrost" t="b:Boolean" v="true"/></p>
  <p n="EvaporatorUnit_2" t="CRP:EvaporatorUnit"><p n="hasDefrost" t="b:Boolean" v="true"/></p>
  <p n="DefrostController" t="CRP:DefrostController"/>
</p>
```
Dragging that drops a correctly-NESTED, correctly-flagged room in one action — the engineer just links field
points. Benefits: consistent structure, `hasDefrost` set right (avoids the "false → never defrosts" trap
B731), the DefrostController already a child of the room (so `units()` resolves, B729), fewer manual steps →
fewer commissioning errors. The easiest way to author these is in Workbench: build one correct assembly in a
scratch station, then copy it into the module palette view and Save (it serializes to this XML); or hand-edit
the BOG XML.

## 746.4 — Application `[INFER]`
Our palettes are functional but MINIMAL (bare components). Adding pre-wired assembly templates (a standard
1-evap room, a 2-evap room with defrost, a compressor rack with N stages + condensers) would make
commissioning drag-and-link instead of drag-nest-flag-link, and bake in the correct structure/flags. Low
risk (palette is a resource, not code; additive). Pairs with the composition refactor (B737) and the units
facets (B745) — a template palette is where "good defaults" live. Doc-only; a Batch-3 candidate (B742).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | module.palette is a BOG XML at module root, bundled in the jar; `<p n= m= t=>` per item | [CERT] | our ColdRoomPan-rt/module.palette; aaphp reference |
| 2 | Ours exposes bare ColdRoom/EvaporatorUnit/DefrostController; CompPan/DashboardPan also ship one | [CERT] | file listing + content |
| 3 | Reader merges palettes across profile jars into a browsable BUnrestrictedFolder space | [CERT] | B634 (BModulePaletteNode/BModule.initNavChildren) |
| 4 | A `<p>` can nest children + property overrides → a pre-wired assembly template | [CERT/INFER] | BOG nesting (B5); Tridium palettes nest; exact override syntax [INFER] |
| 5 | Assembly templates reduce commissioning errors (correct nesting/hasDefrost) | [INFER] | ties to B729/B731 traps |

**Tally**: 3 [CERT], 2 [CERT/INFER]. No unmarked claims.

## Connections
- **B634** (palette reader), **B12** §12.3.2 (format), **B632** (jar packaging), **B737** (composition —
  templates encode the right tree), **B731** (hasDefrost trap a template avoids), **B745** (units), **B742** (Batch-3).

## Open gaps
- **B746-G1**: exact BOG override syntax for property values inside a palette `<p>` (the `v=` / child-`<p>`
  form) — confirm by authoring one in Workbench and reading the serialized XML; sketched here.
