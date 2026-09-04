# B754 · Module versioning + upgrade safety — how a module carries a version, how the station enforces it at boot, and the exact saved-data survival matrix across a version bump (code-grounded)

> **Scope**: what happens to a station when a module's version changes. Where the version lives, how boot-time
> dependency resolution enforces it, why there is NO per-module migration hook, and — the payoff — the exact
> SAFE / SAFE-but-lossy / OUTAGE matrix for every kind of schema change over an existing `.bog`. This
> generalizes and mechanizes B739 (retype outage) and B740 (Missing class) into the full rule. Foco:
> **module-authoring** (MA1 + MA2). The build/signing/version-targeting toolchain is B755.
>
> **Sources**: FUENTE 3 — `organized/baja/baja/vineflower/` (`NModule`, `BModule`, `ModuleManager`,
> `ValueDocDecoder`/`Encoder`, `BFrozenEnum`/`BEnumRange`, `Station`), `platform-rt` (`BVersion`),
> `migration-rt` (`javax.baja.migration`), all cited file:line. FUENTE 1 — B25 (Migration Framework overview),
> B12 (build), module-anatomy B629-636 (classloader/manifest), B739 (retype), B740 (enum/classloader). FUENTE
> 2 — devguide `modules.txt`/`build.txt`/`upgradingBuild.txt`. Every mechanism cites code; the matrix is [CERT].

---

## 754.1 — Where the version lives, and the two version layers `[CERT]`
- The manifest `meta-inf/module.xml` carries `bajaVersion`, `vendor`, `vendorVersion`; the class that READS it
  is `com.tridium.sys.module.NModule` (one per runtime profile), parsing each into a `javax.baja.util.Version`
  (`NModule.readAttributes:384-387`). `BModule` only DELEGATES to its `NModule` parts
  (`BModule.getVendorVersion(profile):180-183`).
- `javax.baja.util.Version` is dewey-decimal: parse splits on `.`, compare is digit-by-digit, and **on a tie
  the LONGER version wins** (`1.0.1 > 1.0`, `Version.compareTo:141-159`). Format = `major.minor.iteration.build`
  (devguide `modules.txt:94`).
- A SECOND, distinct version type exists for the installer/platform layer: `com.tridium.install.BVersion` (a
  `BStruct`) with a bit-flag comparison model (`MEETS_MINIMUM=115`, `meetsVersionRequirement:135-142`). It
  decides which JARs to install BEFORE the station runs. Keep the two apart: runtime `Version` vs install
  `BVersion`.

## 754.2 — Boot-time enforcement: a bad version = a hard boot failure `[CERT]`
At station boot the runtime classloader resolves each module's dependencies:
`ModuleManager.resolve:490-516` → for each `<dependency>` calls `checkBajaVersion` + `checkVendor` on the
depended-upon `NModule`; `NModule.checkBajaVersion:157-160` and `checkVendor:163-165` throw
`ModuleIncompatibleException` unless **required ≤ installed** (and vendor name matches). Any failure →
`ModuleException` → the station does not boot. So `<dependency vendorVersion="4.14">` means "installed must be
**≥ 4.14**" — a minimum, never an exact pin. (This is why you write bare `api(":baja")` and let the build stamp
the version — B755.)

## 754.3 — There is NO per-module migration hook `[CERT — key negative]`
`BModule` is `final` and has no `upgrade`/`migrate`/`moduleStarted` method; grep of `com/tridium/sys/module/`
and `javax/baja/sys/` found none. N4 has **no** "your saved data was written by an older version → run my
migrator" callback. Two consequences:
- **Whole-station** version conversion is a SEPARATE, OFFLINE tool: `javax.baja.migration` (module
  `migration-rt`) — a registry of `BIFileMigrator` + converter SPIs (`BIBogElementConverter`,
  `BIPxElementConverter`, `IOrdConverter`) surfaced as Workbench "Station Upgrade" tools (e.g. the Cl Station
  Upgrade Tool). There is **no `BMigrationService`** running in the station.
- **A component that must survive its OWN schema change must fix itself** in `started()`/`atSteadyState()` —
  detect the stranded/dynamic/missing slots and rewrite them; the decoder will not (§754.5, §754.6).

## 754.4 — The `.bog` binding is by-NAME, resolved live, ungated `[CERT]`
The encoder writes each slot as `<p n=name h=handle v=value t=type m=module f=flags x=facets>` and records the
module by NAME ONLY — no version, no hash (`ValueDocEncoder.encodeType:1081-1088`). The Slot-o-Matic type hash
(`/*@ …(2979906276)… @*/`) is a BUILD-time staleness marker, never read at load. The `.bog` itself only
versions the graph SCHEMA (`bajaObjectGraph version="1.0"|"4.0"`), not the module. So an existing `.bog` is
reconciled against WHATEVER module version is installed now, purely by slot name — nothing detects that the
layout changed. Whether a change is safe is decided entirely by the name-based reconciliation below.

## 754.5 — The one mechanism: `warningAndSkip` (survive) vs unwrapped throw (outage) `[CERT]`
`Station` loads `config.bog` via `ValueDocDecoder.decodeDocument` (`Station.java:171-174`). The ROOT object is
parsed failFast=true (a broken root is fatal); every CHILD slot is parsed failFast=false, so "missing
module/type/slot" is downgraded to a WARNING and the station still boots (`Station.java:180-186` just prints
the warning count). **The entire matrix reduces to one binary**: does the change route the slot to
`warningAndSkip` (non-fatal — that slot's data is dropped or shunted, boot continues) or to an UNWRAPPED throw
that propagates past failFast to `Station.java:174` (fatal — no boot, the B739 outage)?

## 754.6 — The saved-data survival matrix `[CERT]`

| Change to a frozen slot/type | Decode behavior | Verdict |
|---|---|---|
| **ADD** a frozen prop/action/topic | old `.bog` has no entry → new slot takes its default | **SAFE** |
| **REORDER** frozen slots | resolution is `byName.get(name)`, index-independent | **SAFE** |
| change **DEFAULT / FLAGS / FACETS** | only non-default values are saved → unsaved slots adopt the new default; saved values still apply | **SAFE** (semantics may shift) |
| **ADD** an enum tag | `.bog` stores the tag STRING; old tags still resolve | **SAFE** |
| **RENUMBER** enum ordinals (tags unchanged) | `.bog` is tag-based | **SAFE for `.bog`** (⚠ UNSAFE for Fox/binary sync, which encodes the ordinal) |
| **REMOVE** a frozen COMPLEX property (has `t=`) | name-miss → recreated as a DYNAMIC slot, value preserved | **SAFE-ish** (data shunts to a dynamic slot) |
| **REMOVE** a frozen SIMPLE property (`v=` only) | `warningAndSkip("Missing frozen property")` | **LOSSY-SAFE** (value dropped, boots) |
| **REMOVE** a frozen action/topic | `warningAndSkip("Missing frozen action/topic")` | **LOSSY-SAFE** |
| **RENAME** a frozen slot | old name → dynamic-slot resurrection (complex) or warn-skip; new name gets default | **SAFE-but-orphaned** (not auto-migrated) |
| **RETYPE** frozen COMPLEX↔complex/simple | `set()` throws → caught → `warningAndSkip("Cannot set property")` | **LOSSY-SAFE** (reverts to default) |
| **RETYPE** frozen SIMPLE so saved `v=` can't parse | `decodePrimitive` (`:503-529`) throws UNWRAPPED → propagates | **UNSAFE — OUTAGE (B739)** |
| **REMOVE/RENAME** an enum tag a `.bog` still stores | `getRange().get(tag)` → `InvalidEnumException` via `decodeSimple` (`:447,543-548`), unwrapped | **UNSAFE — OUTAGE** |
| whole TYPE removed/renamed, referenced by a `.bog` child | decode-time `TypeNotFoundException` → `warningAndSkip("Type not found")`; a `typeSwapMap` can remap a rename | **LOSSY-SAFE** (subtree skipped) |
| a shared cross-module frozen enum/type CLASS missing | CLASSLOADER/Introspector fails to load the OWNING type (B740/B631) — a DIFFERENT, earlier layer | **UNSAFE — fatal for every instance** (`Missing class …`) |

**Dividing line (memorize this)**: *wrapped-in-`warningAndSkip` = survivable; unwrapped throw = outage.* Dynamic
slots are strictly safer — they are self-describing (`t=`/`f=`/`x=`), recreated independent of the frozen
schema, with no fatal path.

## 754.7 — The upgrade-safety rules that fall out `[INFER, grounded in §754.6]`
1. **ADD, never retype or remove** a frozen slot with saved data (B739 generalized) — add-only is always safe.
2. **Never remove/rename an enum tag** that any station saved; only ADD tags. (Renumbering ordinals is fine for
   `.bog` but breaks Fox/binary sync, so avoid it too if the value crosses Fox.)
3. **Bump `vendorVersion` on every schema change** so the manifest/backup records it — even though nothing
   gates on it at decode, it is the human audit trail and the installer's minimum.
4. **If you MUST change a slot's shape**, do it as ADD-new + migrate-in-`started()` + leave-old-deprecated,
   never in-place retype. The decoder gives you no convert hook — you write the migration in the lifecycle.
5. **Back up the `.bog` before deploying a schema change** — the survivable cases still DROP data silently
   (only a printed warning count), so "it booted" is not "the data survived".

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Version in module.xml, parsed by NModule into javax.baja.util.Version (dewey, longer-wins-tie); BModule delegates | [CERT] | NModule.readAttributes:384-387; Version.compareTo:141-159; BModule:180-183 |
| 2 | Boot resolves deps: required ≤ installed else ModuleException (hard fail); vendorVersion = minimum | [CERT] | ModuleManager.resolve:490-516; NModule.checkBajaVersion:157-165 |
| 3 | No per-module migration hook; whole-station conversion is offline migration-rt (BIFileMigrator); no BMigrationService | [CERT] | BModule final (no upgrade); migration-rt registry |
| 4 | .bog binds by NAME, no version/hash; type-hash is build-time only; graph schema versioned 1.0/4.0 | [CERT] | ValueDocEncoder.encodeType:1081-1088; ValueDocDecoder header |
| 5 | Matrix governed by warningAndSkip (survive) vs unwrapped throw (outage); root failFast, children not | [CERT] | Station.java:171-186; ValueDocDecoder.parseSlot:273,375,389,394,467-473,503-529 |
| 6 | SAFE: add/reorder/default/flags/add-tag; LOSSY-SAFE: remove/rename/complex-retype; OUTAGE: simple-retype, remove/rename enum tag | [CERT] | the §754.6 cites |
| 7 | Dynamic slots strictly safer (self-describing, no fatal path) | [CERT] | ValueDocDecoder:472-473,1097-1098 |

**Tally**: 7 [CERT]. No unmarked claims. §754.7 rules are [INFER] grounded in the [CERT] matrix.

## Connections
- **B739** (the simple-retype outage — now one cell of the matrix), **B740**/**B631** (the classloader
  Missing-class layer, distinct from decode), **B25** (Migration Framework), **B12** (build), module-anatomy
  **B629-636** (manifest/classloader), **B729** (started()/atSteadyState() = the migration seam). Forward:
  **B755** (build/version-targeting/signing), **B759** (our-modules audit).

## Open gaps
- **B754-G1**: the `typeSwapMap` seeding mechanism (how a vendor registers a rename remap) — named, not fully
  traced.
- **B754-G2**: a worked `started()`-based self-migration example for one of our slots — an implementation task.
