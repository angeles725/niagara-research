# Niagara N4 — module-anatomy (MA4): the physical module-jar skeleton — `META-INF/{MANIFEST.MF, NIAGARA4.SF, NIAGARA4.RSA, module.xml}` + classes-by-package + profile-specific payload (`module.palette`/`.lexicon` for rt, `rc/` icons+css for wb, `rc/` JS web-assets for ux)

**Focus**: module-anatomy · **Gap**: MA4 (physical JAR layout) · **Session**: 2026-08-29 · **Block**: B632
**Sources** (`[CERT]` direct artifact — real signed module jars from the live install; structure is not secret):
- `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/control-rt.jar` (rt exemplar)
- `.../modules/bajaui-wb.jar` (wb exemplar) · `.../modules/ace-ux.jar` (ux exemplar) · `.../modules/bacnet-rt.jar` (driver)
Inspected read-only with `unzip -l`/`unzip -p`. Signing MECHANISM = [B489]/[B492] (REMIT); this block records only the physical LAYOUT.

**Scope**: assemble, once, the complete entry map every N4 module jar follows and how it varies by profile — the reference "skeleton" a builder targets. Manifest schema = [B12]/[B76] (REMIT); this block shows the real bytes.

---

## 632.1 The invariant core — `META-INF/` (every module jar)

`[CERT]` `control-rt.jar` (identical shape in wb/ux/driver jars): the first four entries are always:
```
META-INF/MANIFEST.MF     ← JAR manifest + per-entry SHA digests (signing base)
META-INF/NIAGARA4.SF     ← signature file (digest of MANIFEST.MF)
META-INF/NIAGARA4.RSA    ← PKCS#7 signature block
META-INF/module.xml      ← the Niagara module manifest ([B629] reads this)
```
The signer alias is uniformly **`NIAGARA4`** across control-rt, bajaui-wb, ace-ux, bacnet-rt `[CERT]` (each has exactly `META-INF/NIAGARA4.SF` + `.RSA`, no other signer). So the signing artifacts are a fixed pair keyed to the Honeywell/Tridium code-signing identity, not per-module. The chain behind `NIAGARA4.RSA` = [B392]/[B489]/[B492] (Honeywell Product PKI RSA-2048) — REMIT. Layout fact: **a module jar is a signed jar** — strip/alter any entry and MANIFEST.MF's digest fails at load (`m.signed = entry.getCertificates() != null`, [B630] makeModule).

The real manifest `[CERT]` `control-rt.jar!META-INF/module.xml`:
```xml
<module name="control-rt" bajaVersion="0" vendor="Tridium" vendorVersion="4.14.0.162"
        description="Niagara Control Module" preferredSymbol="c" nre="true" autoload="true"
        installable="true" buildMillis="1718362677807" buildHost="ee033fd13409"
        moduleName="control" runtimeProfile="rt" releaseDate="2024-05-28">
  <dependencies><dependency name="baja" vendor="Tridium" vendorVersion="4.14.0"/></dependencies>
  <dirs/> <installation/>
  <types><type name="ControlPoint" class="javax.baja.control.BControlPoint"/> ...</types>
</module>
```
Confirms every attribute [B629]/[B12] named, live: `moduleName` (logical) vs `name` (part), `runtimeProfile`, `autoload`, `installable`, `nre`, `buildMillis`/`buildHost` (reproducibility metadata), `releaseDate`.

---

## 632.2 The class payload — dual namespace by package

`[CERT]` `control-rt.jar` — `.class` files laid out by package path, in TWO namespaces side by side:
- `javax/baja/control/...` — the PUBLIC API types (`BControlPoint`, `BNumericWritable`, `WritableSupport`, …). Registered in `<types>`.
- `com/tridium/control/...` — the IMPLEMENTATION/internal classes (`converters/BRelTimeToTriggerMode`). Mostly NOT in `<types>`.

This is the on-disk form of the framework convention: `javax.baja.*` = contract, `com.tridium.*` = impl ([corpus-wide]). Not every `.class` is a Baja Type — 39 classes, ~24 `<type>` entries; the rest are support/inner classes (`BNumericWritable$NumericWritableSupport`) and non-Type helpers. Only classes listed in `<types>` are registered ([B631] MA3).

---

## 632.3 Profile-specific payload — the skeleton varies by `runtimeProfile`

| Profile | Class count (exemplar) | Distinctive payload | Meaning |
|---|---|---|---|
| **rt** (`control-rt`) | 39 | `module.palette` (root), `control-rt.lexicon` (root) | station runtime: types + a drag-drop palette + default strings |
| **wb** (`bajaui-wb`) | 749 | `rc/fx/theme.css`, 13× `*.png` (icons), `bajaui-wb.lexicon` | Workbench Swing UI: heavy class payload + icon/css resources |
| **ux** (`ace-ux`) | 4 | `rc/*.js` (`ace.built.min.js`, `AcePointManager.js`, …) web assets | browser UI: thin Java agents + JS delivered as `rc/` resources |

Three facts pinned:
1. **`module.palette`** sits at the JAR ROOT (rt jars that ship pre-built components) — a ZIP-of-BOG ([B12] §12.3.2 format; MA6 opens its reader). `control-rt.jar` has one; not every module does.
2. **`<modulePart>.lexicon` at the jar ROOT** (`control-rt.lexicon`, `bajaui-wb.lexicon`) — the DEFAULT-locale strings. Locale-specific lexicons are NOT in the jar; they live in the station file space `file:!lexicon/{lang}/…` ([B12] §12.2.5). So a jar carries only its default lexicon; translations are deployed separately.
3. **`rc/`** is the web-servable/resource root. For **wb** it holds Swing resources (`rc/fx/theme.css`, `*.png` icons); for **ux** it holds the JavaScript UI (`rc/ace.built.min.js` etc.). This is the physical basis of the profile split: ux "code" is largely JS shipped as `rc/` resources with a few Java agent classes, while rt/wb "code" is `.class` bytecode.

---

## 632.4 Dependency weight scales with profile position

`[CERT]` real manifests: `control-rt` declares ONE dependency (`baja`); `ace-ux` declares **30+** (`alarm-rt`, `bajaScript-ux`, `bajaui-ux`, `bajaux-rt/ux`, `box-rt`, `bql-rt/ux`, `chart-rt`, `control-rt`, `driver-rt/ux`, …). This mirrors [B12] §12.1.6's profile permission matrix in the wild: an rt part depends only on other rt (here just `baja`), while a ux part legitimately depends on rt AND ux siblings across many modules. The `<dependency vendorVersion="4.14.0">` is checked at resolve ([B630] §630.4 `checkBajaVersion`/`checkVendor`).

---

## 632.5 The reference skeleton (assembled)

```
<modulePart>.jar
├── META-INF/
│   ├── MANIFEST.MF          # jar manifest + per-entry SHA digests
│   ├── NIAGARA4.SF          # signature file  ┐ signing = B489/B492 (REMIT)
│   ├── NIAGARA4.RSA         # PKCS#7 block     ┘ (Honeywell Product PKI RSA-2048)
│   └── module.xml           # THE manifest: attrs + <dependencies> + <types> (+ <defs>/<permissions>)
├── javax/baja/<pkg>/*.class # PUBLIC API types (registered in <types>)
├── com/tridium/<pkg>/*.class# implementation/internal classes
├── module.palette           # (rt, optional) ZIP-of-BOG drag-drop template  → MA6
├── <modulePart>.lexicon     # default-locale strings (locales → station file space)
└── rc/                      # resources: wb→icons/*.png + fx/theme.css ; ux→*.js web assets
```

**For building/distributing:**
- A conformant jar MUST be SIGNED (META-INF signature pair) and carry a `module.xml` with `runtimeProfile` ([B630]: unsigned/AX manifests silently fail). Your gradle `sign`/`signMods` task ([B12] §12.1.2) produces the `NIAGARA4.*` pair — but with YOUR developer cert, whose acceptance depends on the station's trust anchor + `moduleVerificationMode` ([B392]/[B519], REMIT).
- Ship UI as the RIGHT profile: browser UI → `rc/*.js` in a `-ux` jar; Swing → `.class` + `rc/` in a `-wb` jar. Putting web assets in `-rt` or Swing classes needed by a headless station in `-wb` is the anti-pattern MA8 checks chihuahua for.
- Default lexicon rides in the jar; translations deploy to the station file space separately — a distribution step easy to forget.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | Every jar begins META-INF/{MANIFEST.MF, NIAGARA4.SF, NIAGARA4.RSA, module.xml} | [CERT] | control-rt.jar unzip -l (+ wb/ux/bacnet same) | ✅ unzip |
| 2 | Signer alias uniformly NIAGARA4 across rt/wb/ux/driver jars | [CERT] | unzip -l grep SF/RSA across 4 jars | ✅ unzip |
| 3 | Real module.xml carries all [B629] attrs incl. moduleName/runtimeProfile/autoload/buildMillis | [CERT] | control-rt.jar!META-INF/module.xml | ✅ unzip -p |
| 4 | Classes in dual namespace javax.baja.* (API, in <types>) + com.tridium.* (impl) | [CERT] | control-rt.jar entry list | ✅ unzip |
| 5 | rt exemplar carries module.palette + control-rt.lexicon at root | [CERT] | control-rt.jar entry list | ✅ unzip |
| 6 | No locale-lexicon subdirs in jar (only <part>.lexicon root); locales → station file space | [CERT] | bajaui-wb.jar grep lexicon = only bajaui-wb.lexicon | ✅ unzip |
| 7 | wb = icons(*.png)+rc/fx/theme.css; ux = rc/*.js web assets + thin classes | [CERT] | bajaui-wb.jar rc/ + ace-ux.jar rc/*.js | ✅ unzip |
| 8 | ux part declares 30+ deps vs rt's 1 (baja) — profile matrix live | [CERT] | ace-ux.jar!module.xml vs control-rt.jar!module.xml | ✅ unzip -p |

**Tally**: [CERT] ×8 · [INFER] ×0 · ratio 0.0 (DIRECT-ARTIFACT reference block — all claims are real-jar reads; low ratio EXPECTED for a layout skeleton). Every claim token-checked against the actual jar this iteration.

## Connections

- **[B629]** — `module.xml` is the entry `ModuleManifest`/`NModule` read. **[B630]** — the signed jar + `runtimeProfile` gate at load. **[B631]** — `<types>` → registry.
- **[B12]** §12.2.5 (lexicon locations) §12.3.2 (.palette) — confirmed against real bytes. **[B489]/[B492]** — the `NIAGARA4.SF/.RSA` chain (REMIT).
- Forward: MA6 opens `module.palette`'s reader (`BModulePaletteNode`); MA5 opens how this jar is written into a station's `modules/` dir.

## Gaps uncovered

- None new. MA4 is the direct-artifact reference; it confirms rather than opens. `rc/` servlet-serving path (how `rc/*.js` reaches a browser) is bajaux/web-tier ([B508]/[B615]) — REMIT, not a new row.
