# B817 · The module STRUCTURE STANDARD — how Tridium/Honeywell lay out an N4 module, and a conformance checklist our four modules can be linted against `[CERT]`

> **Scope**: the consolidation the operator asked for — "check whether our modules already have structure, algorithm,
> matrices, binaries, form, organization; what Niagara/Tridium/Honeywell do; principles for creation, improvement,
> compilation." AUDIT-FIRST: assembles the standard from the campaign-6 exemplar census + new `organized/` residue
> (on-disk layout, package naming, PURE-MODEL separation, resources, build/signing, Honeywell deltas, binaries), then
> audits OUR four modules against it. Deliverable: a new kit doc `types/structure.md` (standard layout + a
> "structure conformance" checklist with concrete lint candidates for `scaffold-module.sh` / `verify-module.sh`).
>
> **Sources**: FUENTE 1 REMITTANCE — [Block 784] (profile matrix + dep floor), [Block 790] (minimal skeleton +
> `scaffold-module.sh`), [Block 780] (palette/lexicon), [Block 779] (child containers), [Block 807] (build pipeline),
> [Block 805]/[Block 809]/[Block 813]/[Block 814] (rt/wb/ux/tags conventions), [Block 787]/[Block 788]/[Block 789]
> (conformance audits already run on our modules), [Block 127] (native libs). FUENTE 3 — `organized/` verified this
> session (kitControl/control/history/analytics/honeywell*/ffmpeg/xprotect/web). LIVE — our four client module trees
> (ColdRoomPan/CompPan/DashboardPan/chihuahua). Markers: `[CERT]` = verbatim `file:line`/path read this session ·
> `[INFER]` = derived recommendation.
>
> **Type:** `mixed` (consolidation + new residue + our-module audit). Focus: `module-authoring-exemplars`.

## 817.1 — On-disk layout: a module is one `moduleName` split into per-profile parts `[CERT]`
A logical module unpacks into per-`runtimeProfile` artifacts `<module>-{rt,ux,wb,se}` — `-rt` server runtime, `-ux`
browser (BajaScript/RequireJS), `-wb` Workbench (Swing), `-se` server-only. The `-rt` part enumerates its siblings in
`<moduleParts>`; `-doc` is a SEPARATE `runtimeProfile="doc"` module, never a part ([Block 784] §784.1). Verified:
`organized/kitControl/{kitControl-rt,kitControl-ux,kitControl-wb}/`; `kitControl-rt` module.xml `<moduleParts>` lists
`kitControl-ux`/`kitControl-wb` (`kitControl-rt/.../META-INF/module.xml`). `[CERT]`

The AUTHOR-side source tree (what you write, vs the decompiled jar) is: a profile gradle file `<MOD>-rt.gradle.kts`,
`module-include.xml` (the `<type>` list — the plugin GENERATES `META-INF/module.xml`), `module.lexicon` (renamed to
`<MOD>-rt.lexicon` in the jar), `module.palette`, and `src/com/<vendor>/<MOD>/…` ([Block 790] §14 corrections). `[CERT]`

## 817.2 — Package naming: `javax.baja.*` = public API, `com.tridium.*` = implementation `[CERT]`
The decisive convention, verified in real manifests:
- **PUBLIC framework types other modules depend on are declared under `javax.baja.<domain>`** — `control-rt`'s public
  points are `javax.baja.control.BControlPoint`/`BBooleanPoint`/`BEnumPoint`
  (`control-rt/vineflower/META-INF/module.xml`), while its INTERNAL converters are `com.tridium.control.converters.*`.
- **A leaf/implementation module keeps ALL types under `com.tridium.<module>`** — every `kitControl-rt` type is
  `com.tridium.kitControl.*` (`kitControl-rt/.../META-INF/module.xml`).
- **Extension frameworks get their own root** — analytics is `javax.bajax.analytics.*` (API) +
  `com.tridiumx.analytics.*` (impl) (`analytics-rt/vineflower/…`).
- **ONE public `@NiagaraType` per `.java` file, `BXxx` class naming** — verified across the kitControl `math/` package
  (16 files, one class each: `BAdd`, `BAbsValue`, `BMath`…). `[CERT]`
So a module author with no framework-API ambition uses `com.<vendor>.<Module>` throughout — which is exactly what our
modules do (§817.8). `[CERT]`

## 817.3 — PURE-MODEL SEPARATION: Tridium isolates plain-Java algorithm from the `BComponent` adapter `[CERT]`
The most important structural principle for testability. Tridium repeatedly puts the ALGORITHM in a plain-Java class
(zero `javax.baja` imports) and wraps it in a thin `BComponent` adapter:
- **kitControl** — `Psychrometric` (`kitControl-rt/vineflower/com/tridium/kitControl/hvac/Psychrometric.java`, 0 baja
  imports, a lookup-table enthalpy library) is called by the adapters `BOutsideAirOptimization` (`:4` imports it,
  `:323-324` `Psychrometric.enthalpy(...)`) and `BNightPurge`. `[CERT]`
- **history** — the record-store binary layer `Block`/`ReadBlock`/`WriteBlock`
  (`history-rt/vineflower/com/tridium/history/file/recstore/`, all 0 baja imports; `ReadBlock implements DataInput`)
  under the `@NiagaraType BRecordStoreHistoryTable` adapter. `[CERT]`
- **analytics** — `ThreadPool`/`StatusValue`/`StatusValues` (`analytics-rt/vineflower/com/tridiumx/analytics/…`, no
  baja) under `BAnalyticService` (which does `Analytics.setProvider(this)` to wire the pure facade). `[CERT]`

**Finding that reframes our audit:** Tridium ships NO in-jar test suites in these production modules — a clean grep of
`kitControl`/`history`/`analytics` for `extends BTest(Ng)`/`javax.baja.test` returns ZERO (the only `*Test*.java` is
`kitControl…util/BStringTest.java`, a production string-compare COMPONENT, not a test). Tridium's tests live in
separate test-only jars not shipped in the runtime. So the pure-model split is Tridium's testability seam even though
the tests themselves are out-of-band — and OUR modules, which DO carry `srcTest/` suites over their pure models
(§817.8), already EXCEED the shipped Tridium exemplars on this axis. `[CERT — verified absent]`

## 817.4 — Resources a module ships `[CERT]`
- **`module.lexicon`** — a `.properties` file (`key=value`, `#` comments) at jar root as `<MOD>-rt.lexicon`; localizes
  type + slot display names (`kitControl-rt/extracted/kitControl-rt.lexicon`, header `@author Brian Frank`). Non-empty
  or Workbench shows raw camelCase slot names ([Block 759]/[Block 780]). `[CERT]`
- **`module.palette`** — a `bajaObjectGraph` XML of draggable `<p>` instances (`n`=name, `t`=type, `m`=module alias),
  grouped under `b:UnrestrictedFolder`s (`kitControl-rt/extracted/module.palette`). Empty palette = the B5/[Block 788]
  footgun. `[CERT]`
- **`rc/`** — ux resources inside the `-ux` jar (`kitControl-ux/extracted/rc/…built.min.js`, `fe/…`, `baja/…`). `[CERT]`
- **Icons are CENTRALIZED** — Tridium standard modules do NOT bundle icons; they reference the shared `icons-ux`
  module (`icons-ux/extracted/x16/control/math/add.png`, referenced as `BIcon.std("control/math/add.png")` in
  `BAdd.java:12`). Third-party/Honeywell modules bundle their own under `resources/image/` or `img/`. `[CERT]`
- **Tag dictionary** — shipped as a `tagdictionary-rt` module of BComponent types + palette/lexicon, NOT as a bundled
  JSON/`.trio` data file (a `find` for standalone tagdictionary JSON returned zero — [Block 814] the author ships
  types, not data). `[CERT / CERT-absent]`

## 817.5 — Build, versioning, signing `[CERT]`
- **module.xml roster** — `vendor`, 4-part `vendorVersion` build stamp (`4.14.0.162`), `moduleName`, `runtimeProfile`,
  `preferredSymbol`, `buildMillis`, `releaseDate`, `<dependencies>`, `<types>`, `<moduleParts>`
  (`kitControl-rt/extracted/META-INF/module.xml`). `[CERT]`
- **Dependency = 3-part FLOOR, not the 4-part self-stamp** — `<dependency vendor="Tridium" vendorVersion="4.14.0"/>`
  ([Block 784] §784.2). `[CERT]`
- **Gradle** — the devkit ships Velocity templates (`devkit-wb/extracted/gradle/{build.gradle.kts.vm,
  module/module.gradle.kts.vm}`): root applies `com.tridium.niagara`/`vendor`/`niagara-signing`; the module part
  applies `niagara-module` + a `moduleManifest{ moduleName; runtimeProfile }` + a `Bajadoc` task. `[CERT]`
- **Signing is mandatory** — `META-INF/NIAGARA4.SF` + `NIAGARA4.RSA`, with a per-class `SHA-256-Digest` in
  `MANIFEST.MF` (`kitControl-rt/extracted/META-INF/`). Build pipeline + station-lock copy: [Block 807]. `[CERT]`

## 817.6 — Honeywell/OEM modules DIFFER from Tridium `[CERT]`
- **Namespace** `com.honeywell.*` (bacnetSpyder, honTagDictionary) / `com.honeywell.galileo.*` (Galileo). `[CERT]`
- **Different signing identity** — `META-INF/SERVER1.SF`+`SERVER1.RSA`, not `NIAGARA4.*`
  (`honeywellBacnetSpyder/…/META-INF/SERVER1.SF`). `[CERT]`
- **Own vendor + versioning** — `vendor='Honeywell'` `4.14.0.10.5.64`; Galileo `vendor='Honeywell.Galileo'`
  `1.4.2813.0` (its own 1.x scheme). Looser dep floors (`baja … vendorVersion='4.0'`). `[CERT]`
- **Obfuscated** — Honeywell modules are ZKM (Zelix KlassMaster) obfuscated: name-mangling + string encryption +
  irreducible control-flow (`honeywellBacnetSpyder/DEOBFUSCATION-NOTE.md`; `*.obfuscated-bak/` trees). Tridium modules
  are NOT. `[CERT]`
- **Branding bundled in-jar** (`galileoSupervisor-rt/extracted/resources/image/Alki_N4Logo.png`) vs Tridium's shared
  `icons-ux`. `[CERT]`
Implication: our OEM modules follow the Honeywell pattern (own vendor namespace `com.angeles.*`, own signing) — that is
legitimate; do NOT mimic `javax.baja.*` type declarations (reserved for the framework).

## 817.7 — Binaries a module MAY carry `[CERT]`
- **Native libs under `nativeLib/<arch>/`** inside the jar — `ffmpeg-wb/extracted/nativeLib/x86_64/avcodec-60.dll`,
  `xprotect-wb/extracted/nativeLib/VideoOS.Platform.dll` (+ a bundled `.exe` bridge). Corpus shows only Windows DLLs;
  no Linux `.so` present (a JACE-native `.so` would ship via the platform `nre` layer, [Block 127], not a module jar).
  `[CERT / CERT-absent for .so]`
- **Jar-in-jar** — `web-rt/extracted/wbapplet/wbapplet.jar` (legacy applet). `[CERT]`
- **Binary resources** — PNG/GIF (icons), minified JS bundles, policy XML (`baja/extracted/rc/niagara-policy.xml`). `[CERT]`
Our modules: chihuahua-ux bundles Chart.js/Three.js/woff2 fonts + ~37 JS modules under `rc/` — the legitimate
binary-resource form; none carry native libs (correct for pure-Java control logic).

## 817.8 — OUR FOUR MODULES vs the standard `[CERT]`
Structure inventory of the live client trees (all under `com.angeles.<Module>`, one `@NiagaraType` per file — both
conventions PASS across all four):

| Module | Parts | pure-model isolated? | model tests | lexicon | palette | notable gap |
|---|---|---|---|---|---|---|
| ColdRoomPan | rt | NO — `ColdRoomControl`/`CrLog` FLAT with BComponents (both 0 baja ✓) | `srcTest/…ColdRoomControlTest` 24 @Test (pure only) | 44 keys (BFanMode labels missing) | 3 | no BComponent tests; no `model/` pkg |
| CompPan | rt | NO — `CompressorControl`/`CpLog` flat (both 0 baja ✓) | `CompressorControlTest` (pure only) | 40+ keys | 1 (no folders) | no BComponent tests; palette flat |
| DashboardPan | rt,ux,wb | NO — pure `DashboardDispatch`/`RbacHelper`/`JsonUtil` mixed in `.ux` w/ `BDashboardServlet` + contaminated `DashboardReader` | ux `DashboardDispatchTest` only; rt/wb `srcTest` EMPTY | rt 26 · ux 1 · wb EMPTY | rt 2 · ux 1 · wb EMPTY | `-wb` is a 0-source skeleton (no jar, no JUnit dep); `DashboardReader` (14 baja) untested |
| chihuahua | rt,ux,wb | **YES** — `chihuahua-wb/…/wb/model/` 6 pure classes (0 baja ✓); `ChiJsonUtil` pure | rt 8 + ux 14 + wb 6 test files; wb/model fully tested | rt EMPTY · ux EMPTY | rt 11 · ux 3 · wb none | rt+ux lexicons EMPTY (10 types unlocalized); niagaraTest 0-discovery (plugin 7.6.17, [Block 807]) silently skips rt tests |

Verified myself this session: `chihuahua-wb/src/.../wb/model/` = 6 classes, `grep -rl 'import javax.baja'` → NONE;
chihuahua rt+ux `module.lexicon` → 0 `key=value` lines; `ColdRoomControl.java`/`CompressorControl.java` → 0 baja
imports; DashboardPan-wb `src` → 0 `.java`. Cross-refs: [Block 787] (ColdRoomPan/BEvaporatorUnit `stopped()`-cancel —
since fixed, [Block 815]), [Block 788] (DashboardPan-wb empty palette + partial lexicons + the corrected "CompPan
lexicon" claim). `[CERT]`

**Reading:** chihuahua is the structural EXEMPLAR among our four (isolated `wb/model/` + tests, correct palettes) but
has the worst lexicon state (both empty). ColdRoomPan/CompPan have clean pure models WITH tests (exceeding Tridium's
in-jar exemplars) but do not isolate them in a `model/` sub-package and never test the BComponents. DashboardPan-wb is
an empty skeleton that should be removed or filled.

## 817.9 — Kit implication → new kit doc `types/structure.md` `[INFER, grounded]`
1. **Standard layout** (copy-start, each element `[ev: corpus B<n>]`): the §817.1 tree + §817.2 naming + §817.5
   module.xml/dep-floor/signing, reusing the [Block 790] skeleton as the concrete file set.
2. **"Structure conformance" checklist — lint candidates** `scaffold-module.sh` emits-correct and `verify-module.sh`
   enforces:
   - **L1** every `.java` package is `com.<vendor>.<Module>[.<profile-sub>]`, consistent (grep the `package` line).
   - **L2** exactly one public `@NiagaraType` per `.java` file.
   - **L3** a pure-model package (`…/model/`) exists WITH a `srcTest` test per class AND `grep 'import javax.baja'`
     over it returns ZERO (the testability seam — §817.3). *Recommend `model/` isolation even though 2 of our modules
     keep it flat.*
   - **L4** `module.lexicon` present AND non-empty (≥1 `key=value`) — would fire on chihuahua rt+ux TODAY.
   - **L5** `module.palette` present AND non-empty — would fire on DashboardPan-wb TODAY ([Block 788]).
   - **L6** the source ships `module-include.xml` (NOT a hand-authored `META-INF/module.xml`) — [Block 790] §14.
   - **L7** `<dependency>` versions are 3-part floors, not the 4-part self-stamp ([Block 784]).
   - **L8** signed-jar present (`NIAGARA4`/vendor `.SF`+`.RSA`) — already in `verify-module.sh` ([Block 807]).
   - **L9** no empty skeleton part (a declared `-wb`/`-ux` with 0 `.java` AND empty palette/lexicon) — DashboardPan-wb.
   - **L10** no absolute HOST paths in a tracked `gradle.properties` — the client `ColdRoomPan`/`CompPan`
     `gradle.properties` hardcode `niagara_home=C:\…`, `niagara_user_home=C:\…`, `nodeHome=C:\…`, which break any
     non-Windows / different-host build (each surfaces as a `URISyntaxException: Illegal character … C:\…`); pass
     these via `-P`/env or a per-developer untracked file instead. Evidence: [Block 815] §815.12 (executed).
   - **L11** a module whose `srcTest` mixes pure-JUnit and Baja (`BTest`/`BTestNg`) tests declares BOTH
     `moduleTestImplementation(":test-wb")` AND `moduleTestImplementation("junit:junit:…")` (or splits the source
     sets) — else `moduleTestJar` cannot compile the tree. `ColdRoomPan-rt`/`CompPan-rt` declare only `:test-wb`.
     Evidence: [Block 815] §815.12 (executed).
3. **Ranked recommendations for our modules** (impact ÷ cost):
   R1 chihuahua — populate rt+ux `module.lexicon` (10 types unlocalized; highest operator-visible, cheap). 
   R2 DashboardPan-wb — delete the empty skeleton OR fill it + add its JUnit dep; add rt `BDashboardService` tests.
   R3 DashboardPan — test `DashboardReader` (14-baja, the sole data engine, untested) via a `BTestNg` station test
   ([Block 815] recipe). R4 ColdRoomPan/CompPan — isolate the pure model in a `model/` sub-package (align to chihuahua)
   + add BComponent lifecycle tests. R5 ColdRoomPan — add the missing `BFanMode`/`fanRunMode` lexicon keys.

## 817.10 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Module = per-profile parts rt/ux/wb/se; -doc is a separate module; -rt lists `<moduleParts>` | `[CERT]` | [B784]; `kitControl-rt/.../module.xml` | Y — REMITTANCE+read |
| 2 | `javax.baja.<domain>` = public API types; `com.tridium.<module>` = impl; one @NiagaraType/file, BXxx | `[CERT]` | `control-rt`+`kitControl-rt` module.xml; kitControl `math/` | Y — grep |
| 3 | Pure-model classes (0 baja) under thin BComponent adapters: Psychrometric, recstore Block, analytics ThreadPool | `[CERT]` | `Psychrometric.java`+`BOutsideAirOptimization.java:4,323`; `recstore/*.java`; `analytics/util,data` | Y — grep |
| 4 | Tridium ships NO in-jar test suites in kitControl/history/analytics (our modules exceed this) | `[CERT]` | clean grep `extends BTest(Ng)`/`javax.baja.test` → 0 | Y — verified absent |
| 5 | lexicon=.properties, palette=bajaObjectGraph XML, icons centralized in icons-ux, tags=types not JSON | `[CERT]` | `kitControl-rt/extracted/{*.lexicon,module.palette}`; `icons-ux/x16/…`; `BAdd.java:12` | Y — read |
| 6 | Signing NIAGARA4.SF/RSA + per-class SHA-256 manifest; dep = 3-part floor | `[CERT]` | `kitControl-rt/extracted/META-INF/`; [B784] | Y — read |
| 7 | Honeywell: com.honeywell.*, SERVER1.SF signing, own versioning, ZKM-obfuscated, in-jar branding | `[CERT]` | `honeywellBacnetSpyder/…/SERVER1.SF`+`DEOBFUSCATION-NOTE.md`; `galileoSupervisor-rt` | Y — read |
| 8 | Binaries: native libs `nativeLib/<arch>/` DLLs, jar-in-jar `wbapplet.jar`; no module `.so` in corpus | `[CERT]` | `ffmpeg-wb`/`xprotect-wb/nativeLib`; `web-rt/wbapplet/wbapplet.jar` | Y — ls |
| 9 | Our four: all `com.angeles.*` + one-type/file PASS; chihuahua isolates `wb/model` (6 pure); chihuahua rt+ux lexicons EMPTY; DashboardPan-wb 0 source | `[CERT]` | live trees, verified this session | Y — grep/ls |
| 10 | The structure.md checklist L1-L11 lint candidates + ranked recommendations | `[INFER]` | §817.9, composes 1-9 | recipe |

**Tally:** `[CERT]` ×9 · `[INFER]` ×1. Every load-bearing exemplar and every our-module claim grep/ls-verified this
session; the negative "no in-jar tests" finding confirmed with a clean grep, not a tool-failure zero (§ evidence
discipline).

## 817.11 — Connections & open gaps
- REMITTANCE: [Block 790] (minimal skeleton — the copy-start this standardizes), [Block 784] (profiles/deps),
  [Block 780] (palette/lexicon), [Block 779] (containers), [Block 807] (build/sign/version + plugin 7.6.17),
  [Block 805]/[Block 809]/[Block 813]/[Block 814] (rt/wb/ux/tags authoring), [Block 787]/[Block 788]/[Block 789]
  (the conformance audits folded here), [Block 815] (the lifecycle test that fills the "BComponent untested" gap),
  [Block 127] (native `nre` layer — where a real JACE `.so` lives, not a module jar).
- **B817-G1** (requires-execution): implement the L1-L11 checks in `verify-module.sh` + emit-correct in
  `scaffold-module.sh`, and run them RED→GREEN against our four trees (L4 must fire on chihuahua, L5+L9 on
  DashboardPan-wb) — the biting test the [Block 790] scaffold fixture needs.
- **B817-G2**: confirm where Tridium's OWN module tests live (the out-of-band test jars absent from this corpus) — to
  validate that the pure-model seam is genuinely how they test, not just how they COULD.
