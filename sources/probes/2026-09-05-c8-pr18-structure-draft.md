# C8 PR18 — `build-n4-module-kit/types/structure.md` apply-ready CONTENT draft

> For the PR18 apply worker: this is the doc-only body of the NEW kit file `types/structure.md` (wave3 R18.1),
> from B817 (+L10/L11). The L1–L11 table + tool contract MUST match the RED `qa/c8-structure` c32cb5a
> (`tests/lint-structure.bats`): tool `lint-structure.sh <module-root>`, row `FAIL|WARN  lint-structure  <path>
> L<n>: <reason>`, exit 0/1/3, and it PRUNES `.deploy-baseline/`. Every line `[ev: corpus B817]` (which itself
> carries the deeper B784/B790/B815 cites). Grep-before-fold: `types/structure.md` does not exist → net-new file.

---
=== BEGIN types/structure.md ===

# Type: module structure — the layout, naming, and a conformance lint (`lint-structure.sh`)

The shape a Niagara N4 module must have on disk, and the L1–L11 checklist `lint-structure.sh` enforces. Grounded
in how Tridium/Honeywell lay out their own modules and an audit of our four. `[ev: corpus B817]`

## Layout — one `moduleName`, per-profile parts `[ev: corpus B817]`
- A module splits into per-`runtimeProfile` artifacts `<module>-{rt,ux,wb,se}`: `-rt` server runtime, `-ux` browser
  (BajaScript), `-wb` Workbench (Swing), `-se` server-only. The `-rt` part lists its siblings in `<moduleParts>`.
- `-doc` is a SEPARATE `runtimeProfile="doc"` module (empty `<types/>`), NEVER a part of a code module.
- Author-side source tree: `<MOD>-rt.gradle.kts` + `module-include.xml` + `module.lexicon` + `module.palette` +
  `src/com/<vendor>/<MOD>/…` (+ `srcTest/` for tests, `rc/` for `-ux`). `[ev: corpus B817]`

## Naming — `javax.baja.*` = framework API, `com.<vendor>.*` = your code `[ev: corpus B817]`
- Tridium declares PUBLIC framework types under `javax.baja.<domain>` (e.g. `javax.baja.control.BControlPoint`) and
  keeps implementation under `com.tridium.<module>`. Honeywell/OEM modules use `com.<vendor>.<module>` throughout
  (ours: `com.angeles.<Module>`) — a leaf/OEM module NEVER declares `javax.baja.*` types.
- One public `@NiagaraType` per `.java` file; `BXxx` class naming. `[ev: corpus B817]`

## `module-include.xml` vs `META-INF/module.xml` `[ev: corpus B817]`
- You AUTHOR `module-include.xml` (the `<type>` list). The gradle plugin GENERATES `META-INF/module.xml` from it.
- A hand-authored `META-INF/module.xml` in source is WRONG (L6): it will be overwritten and drifts from the plugin
  output. Edit `module-include.xml`; never the generated manifest. `[ev: corpus B817]`

## The good-module checklist — L1–L11 (`lint-structure.sh <module-root>`) `[ev: corpus B817]`
Row format: `FAIL|WARN  lint-structure  <path>  L<n>: <reason>`. Exit **0** clean · **1** any FAIL · **3** usage.
`.deploy-baseline/` subtrees are PRUNED (a deploy snapshot is not source). `[ev: corpus B817]`

| L | Checks | Verdict | Real shape that proved it |
|---|---|---|---|
| L1 | every `.java` package is `com.<vendor>.<Module>[.<sub>]`, consistent | FAIL | all four modules PASS |
| L2 | exactly one public `@NiagaraType` per `.java` file | FAIL | all four PASS |
| L3 | a pure-model pkg (`…/model/`) with a `srcTest` test per class AND zero `import javax.baja` in it | WARN | chihuahua `wb/model/` isolated ✓; ColdRoomPan/CompPan keep it flat (advisory) |
| L4 | `module.lexicon` present AND non-empty (≥1 `key=value`) when ≥1 type is declared | FAIL | chihuahua-rt + -ux lexicons EMPTY → **L4 FAIL** |
| L5 | `module.palette` present AND non-empty on a component module | FAIL | empty palette = the B788 footgun |
| L6 | source ships `module-include.xml`, NOT a hand-authored `META-INF/module.xml` | FAIL | plugin generates the manifest |
| L7 | every `<dependency>` `vendorVersion` is a 3-part floor (`4.14.0`), not 2-part or the 4-part self-stamp | FAIL | mutation `:baja:4.14` (2-part) → **L7 FAIL** |
| L8 | signed jar present (`NIAGARA4`/vendor `.SF`+`.RSA`) | FAIL | shared with `verify-module.sh` |
| L9 | no empty skeleton part — a declared `-wb`/`-ux` with 0 `.java` AND empty palette/lexicon | FAIL | DashboardPan-wb (0 source, empty palette) → **L9 FAIL** |
| L10 | no absolute HOST paths in a tracked `gradle.properties` (`C:\…` `niagara_home`/`user_home`/`nodeHome`) | FAIL | client `ColdRoomPan`/`CompPan` `gradle.properties` hardcode `C:\Honeywell\…` → **L10 FAIL** (`.deploy-baseline/` pruned) |
| L11 | a `srcTest` mixing pure-JUnit + Baja (`BTest`/`BTestNg`) declares BOTH `moduleTestImplementation(":test-wb")` AND `…("junit:junit:…")` (or splits source sets) | FAIL | ColdRoomPan-rt/CompPan-rt declare only `:test-wb` → **L11 FAIL** |

`[ev: corpus B817]` for every row (B817 carries the deeper cites: L4/L5 ← B788, L6 ← B790 §14, L7 ← B784,
L8 ← B807, L10/L11 ← B815 §815.12).

## PASS state + scaffold `[ev: corpus B817]`
`scaffold-module.sh <MOD>` output passes L1–L11 at exit 0 — the skeleton is the GREEN fixture. A mutation that
empties the palette (L5/L9), a lexicon (L4), drops a 3-part floor (L7), hardcodes a `C:\` path (L10), or mixes
tests without both deps (L11) flips the corresponding row to FAIL. `[ev: corpus B817]`

## Recommendations for our modules (impact ÷ cost) `[ev: corpus B817]`
R1 chihuahua — populate rt+ux `module.lexicon` (10 types unlocalized; cheap, operator-visible). R2 DashboardPan-wb
— delete the empty skeleton OR fill it + add its JUnit dep. R3 DashboardPan — test `DashboardReader` (14-baja, the
sole data engine, untested) via a `BTestNg` station test (B815 recipe). R4 ColdRoomPan/CompPan — isolate the pure
model in a `model/` sub-package + add its junit moduleTest dep (L11). R5 ColdRoomPan — add the missing
`BFanMode`/`fanRunMode` lexicon keys. `[ev: corpus B817]`

=== END types/structure.md ===

---
## Apply-worker notes
- Place the body between BEGIN/END as `types/structure.md`; drop the markers.
- L-table verdicts (FAIL/WARN) + the row format + exit codes MUST match `qa/c8-structure` c32cb5a
  `tests/lint-structure.bats` (LS4/LS7/LS9/LS10/LS11 assert FAIL; LS-prune = `.deploy-baseline/` ignored;
  LS-usage = exit 3; LS-PASS = scaffold output exit 0). L3 is the only WARN.
- Route it: name `types/structure.md` in `skill/SKILL.md` § References + the Decision-Gates table, and
  `lint-structure.sh` in `BUILD-LOOP.md` §5 + `skill/SKILL.md` (K19: else `kit-links.bats` L4/L5 RED).
- Close: PR18 is a kit change → `new-retro.sh kit c8-pr18-structure` + `retro_pending` in the kit self-envelope,
  same push range (BUILD-LOOP §7 envelope-pairing).
