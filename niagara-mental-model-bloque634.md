# Niagara N4 — module-anatomy (MA6): the palette runtime reader — `module.palette` is discovered lazily from the module's `BZipSpace`, BOG-decoded into an ungated nav node (`BModulePaletteNode`), and cached per module

**Focus**: module-anatomy · **Gap**: MA6 (palette runtime reader) · **Session**: 2026-08-29 · **Block**: B634
**Sources** (`[CERT]` decompiled Java, vineflower `decompiled/` tree):
- `organized/baja/baja/decompiled/com/tridium/sys/module/BModulePaletteNode.java`
- `organized/baja/baja/decompiled/javax/baja/sys/BModule.java`

**Scope**: the LOAD side of `module.palette` (format = [B12] §12.3.2, ZIP-of-BOG; BOG decode = [B5], REMIT). Confirms the physical entry seen in [B632] (`control-rt.jar!module.palette`) becomes a browsable Workbench node.

---

## 634.1 What `BModulePaletteNode` is

`[CERT]` `BModulePaletteNode.java:40` — `public class BModulePaletteNode extends BComponentSpace` — the palette is exposed as a full **component SPACE** (a `BINavNode`), not a leaf, so its pre-built components are browsable/expandable in the nav tree exactly like a station's component space. Its display name is fixed `"module.palette"` (`:61,:122`).

`BModule` holds it as `Optional<BModulePaletteNode> paletteNode` `[CERT]` `BModule.java:80`, and a static `Map<String,BModulePaletteNode>` in the node's `make(...)` factory caches one instance per module name.

---

## 634.2 Discovery: scan the module's zip spaces for `module.palette`

`[CERT]` `BModule.java:66,259,458-472` — a `BModule` keeps `Map<RuntimeProfile,BZipSpace> zipSpaceByRuntimeProfile` (one zip space per profile part, populated at `addModulePart`, [B629] §629.5). `initNavChildren()` accumulates palette files across ALL profile parts:
```java
this.zipSpaceByRuntimeProfile.values().forEach(zipSpace -> {
    for (BIFile file : zipSpace.listFiles())
        if (file.getFileName().equals("module.palette")) accumPalettes.add(file);
});
```
So a module can contribute a palette from any of its profile jars; they are merged into one node.

---

## 634.3 Decode: BOG deserialization with a fault-tolerant type resolver

`[CERT]` `BModulePaletteNode.java:61-87` — the constructor decodes each palette file into the space's root folder:
```java
super("module.palette", null, ordInSession);
// BUnrestrictedFolder palette = root
try (ValueDocDecoder decoder = new ValueDocDecoder(partFile)) {
    decoder.setTypeResolver(new ValueDocDecoder.ITypeResolver(){ /* swallow unknown module/type */ });
    BValue document = decoder.decodeDocument();   // BOG deserialize (B5)
    // components merged into the palette folder
}
```
Two facts: (1) the palette is a standard BOG document read through `ValueDocDecoder` ([B5]); (2) a **custom `ITypeResolver` is installed to tolerate unknown modules/types** — a palette referencing a type whose module isn't installed does not blow up the whole palette load (it skips the missing type). This is why a palette from a partially-installed vendor kit still opens, showing only the resolvable components — the on-disk basis of [B12] §12.3.2's "palettes only validate at load" gotcha.

---

## 634.4 Lazy + cached + ungated

- **Lazy**: `paletteNode` starts `null` (`:80`, reset to `null` in `clearNavChildren()` `:453`); it is populated only on the first `initNavChildren()` (`:458`), which every nav accessor (`:381,:397,:403`) funnels through. `hasPalette()`-style checks return `paletteNode.isPresent()` (`:116`). BOG decode happens once, at first nav expansion — not at module load ([B630] boot does not read palettes).
- **Cached**: once the `Optional` is set it is not re-scanned; the node factory also caches by module name. A module with no `module.palette` gets `Optional.empty()` and adds no nav child.
- **Ungated**: grep of `initNavChildren`/`BModulePaletteNode` for `license`/`permission`/`checkLicensed` = ∅. `BModule.checkLicensed()` exists (`:148`) but is NOT on the palette path. **Palette visibility is neither licensed nor permission-gated** — any module that ships a `module.palette` exposes it to any Workbench user who can see the module. (Consistent with palettes being data, not a licensed feature.)

---

## 634.5 What this means for building/distributing a module

- **Shipping a palette is free and low-effort**: drop a `module.palette` BOG at the jar root ([B632]) and it appears as a browsable node — no registration, no license, no permission wiring. For a module that exports reusable components, a palette is the intended delivery of "starter" instances.
- **A palette tolerates missing types** (the fault-swallowing resolver), so a palette can reference optional/other-vendor types without breaking; but those entries silently vanish for users who lack the module — validate the palette against the minimal install.
- **Chihuahua check (MA8)**: `chihuahua-rt.jar` ships NO `module.palette` ([B632] taxonomy) despite exporting reusable components (`BPlanta`, `BChiCarcamo`, `BChiUp`, `BChiDatalogger`…). Adding one is a cheap usability improvement — the components would become drag-and-drop instead of hand-built.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | BModulePaletteNode extends BComponentSpace (a nav SPACE), name "module.palette" | [CERT] | BModulePaletteNode.java:40,61,122 | ✅ read verbatim |
| 2 | BModule holds Optional<BModulePaletteNode> paletteNode | [CERT] | BModule.java:80 | ✅ read verbatim |
| 3 | discovery scans zipSpaceByRuntimeProfile for files named "module.palette" | [CERT] | BModule.java:66,458-472 | ✅ read verbatim |
| 4 | decode via ValueDocDecoder.decodeDocument (BOG) with a custom fault-tolerant ITypeResolver | [CERT] | BModulePaletteNode.java:68-87 | ✅ read verbatim |
| 5 | lazy (null until first initNavChildren) + cached (Optional + static map); decode once | [CERT] | BModule.java:80,453,458 · :116,381,397,403 | ✅ read |
| 6 | no license/permission gate on palette visibility (checkLicensed not on this path) | [CERT] | rg BModulePaletteNode/initNavChildren license/permission = ∅ | ✅ grep |

**Tally**: [CERT] ×6 · [INFER] ×0 · ratio 0.0 (EVIDENCE block, small/narrow gap; investigable evidence exhausted for this reader). All citations token-checked verbatim.

## Connections

- **[B12]** §12.3.2 (palette format + "validate only at load") — the fault-tolerant resolver is why. **[B632]** — the physical `module.palette` entry this reads. **[B5]** — BOG/`ValueDocDecoder` (REMIT). **[B629]** — `BModule`/`BZipSpace` per-profile the scan walks.
- Forward: MA8 uses the "chihuahua ships no palette" observation as a usability improvement.

## Gaps uncovered

- None. MA6 answered read-only on disk; a LOW gap, now closed.
