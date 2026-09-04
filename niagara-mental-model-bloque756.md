# B756 · The module build toolchain — vendorVersion, version-targeting to an SDK, the gradle-niagara plugin family, .jar vs .dist, and the signing chain (authoring recipe, code-grounded)

> **Scope**: the concrete authoring toolchain for building/versioning/signing a module — grounded in OUR real
> build files (Leon-Guanjuato) + the kit + the devguide. Complements the runtime versioning story (B754) with
> the BUILD side. Foco: **module-authoring** (MA3).
>
> **Sources**: FUENTE 3 — `/home/cristian/modulos_niagara_n4/Cliente/Leon-Guanjuato/Paccadia/` (`build.gradle.kts`,
> `settings.gradle.kts`, `gradle.properties`, `*/niagara-module.xml`, `*/module-include.xml`, generated
> `build/manifest/writeModuleXml/module.xml`), the kit `build-n4-module-kit/toolbelt/{build.sh,verify-module.sh,
> stored-repack.sh}` + `build-verify.md`. FUENTE 2 — devguide `build.txt`/`modules.txt`/`distribution.txt`/
> `upgradingBuild.txt`/`codeSigning.txt`. FUENTE 1 — B12 (build overview), module-anatomy B629-636, signing-pki
> B725. Plugin BYTECODE is not decompiled; behavior is from our build files + the authoritative devguide.

---

## 756.1 — vendorVersion: one source, stamped everywhere `[CERT]`
- Single source of truth = the vendor extension in the ROOT `build.gradle.kts`:
  `vendor { defaultVendor("Angeles"); defaultModuleVersion("2.0.3") }` (`Paccadia/build.gradle.kts:27-34`). The
  `com.tridium.vendor` plugin stamps `vendor` + `vendorVersion` onto every module's `module.xml` — verbatim in
  the generated `ColdRoomPan-rt/build/manifest/writeModuleXml/module.xml:2` (`vendor="Angeles"
  vendorVersion="2.0.3"`). Bumping a version = edit `defaultModuleVersion(...)`.
- **Dependencies are MINIMUMS, and you leave the version OFF in gradle**: write bare `api(":baja")`
  (`ColdRoomPan-rt.gradle.kts:47-50`), NOT `api("Tridium:baja:4.13")`. The manifest `<dependency
  vendorVersion="4.14">` is derived from the build's `niagara_home`, and the runtime uses whatever is in
  `!modules` ≥ that minimum (B754 §754.2; devguide `upgradingBuild.txt`).

## 756.2 — Version-TARGETING: build against the LOWEST station you must support `[CERT]`
- The target = the `niagara_home` you build against (`build.sh:39-61`, `-Pniagara_home=…`); its `baja` version
  is what gets stamped into the manifest's `baja` dependency.
- **The rule**: a 4.14 station REJECTS a jar whose manifest stamps `baja 4.15`. So target the lowest.
  Field-confirmed: ColdRoomPan stamps `baja … 4.14` even though `gradle.properties` points at a 4.15 SDK —
  `gradle.properties` "can lie"; the true target is intersection(plugin-in-m2, station-accepts-baja)
  (`build-verify.md:44-46`). The gate enforces it: `verify-module.sh:71-79` FAILs if `stamped baja > --target-version`.
- **Java-8 / bytecode major-52** ties to the runtime PROFILE, not the SDK version (every N4 profile is Java 8;
  rt/ux = Compact 3, wb/se = SE). `build.sh` forces JDK 8; the gate rejects newer bytecode.

## 756.3 — The gradle-niagara plugin family `[CERT]`
Not one plugin — a family, ONE version per install (`settings.gradle.kts:86`, overridable via
`-PniagaraPluginVersion`): 4.13.2→7.3.40, 4.14→7.6.17, 4.15.3→7.6.22 (`build-verify.md:49-53`). Applied:
`com.tridium.niagara`, `com.tridium.vendor`, `com.tridium.niagara-module` (configures `moduleManifest` + the
`jar`/`moduleTestJar` tasks), `com.tridium.niagara-signing` (must also be on the root), `bajadoc`,
`niagara-jacoco`, `niagara-annotation-processors` (adds `:nre` to `annotationProcessor`),
`convention.niagara-home-repositories` (`!bin/ext` + `!modules` as flat-file Maven repos). The `slotomatic`
task regenerates the AUTO region from `@NiagaraProperty` — a plain `gradle :jar` SKIPS it, so the kit runs
`:clean :slotomatic :jar` explicitly (`build.sh:59`). `moduleManifest{ moduleName.set(...); runtimeProfile.set(rt) }`
writes `module.xml`; the module-level `niagara-module.xml` (`preferredSymbol`, `runtimeProfiles`) replaced the
old `moduleParts{}` block.

## 756.4 — .jar vs .dist, and module-include.xml `[CERT]`
- A **module `.jar`** = a PKZIP jar with ONE runtime profile, `META-INF/module.xml` (manifest), the signed
  `META-INF/NIAGARA4.SF`/`.RSA`, and one `<type class=…>` per exported BComponent. The `<types>` list must
  MATCH `module-include.xml` (the authoring source: "one `<type>` per exported BComponent; a class not listed
  is dead bytecode"); the gate's `typecount` check enforces the match.
- A station-level **`.dist`** is a DIFFERENT thing: a platform-specific PKZIP whose entry paths mirror the
  target filesystem, packaging files + deploy instructions + `platform.bog` edits + file-replacement rules; it
  can EMBED installable modules (`<installable type="module">`). Our field deploy is at .jar granularity
  (`ng-deploy.sh`: backup → build → copy to `<niagara_home>/modules/*.jar` → type-count verify), NOT `.dist`.

## 756.5 — The signing chain `[CERT / CERT-live]`
- **Auto-signing is ON by default** (since 4.6): the `com.tridium.niagara-signing` plugin hooks the `jar` task
  and signs with a generic self-signed cert (alias `Niagara4Modules`, keystore
  `~/.tridium/security/niagara.signing.jks`). There is NO separate signing invocation — `:jar` signs. The gate
  requires it: `verify-module.sh` FAILs unless `META-INF/NIAGARA4.SF` is present, and BOTH profile jars must be
  signed.
- **Project-CA convention (`angelessigner`)** `[CERT-live 2026-09-01]`: a Honeywell SUPERVISOR accepts gradle's
  per-machine DEV cert — no re-sign (DashboardPan ships DEV-signed as-is); a JACE ENFORCES the project CA →
  JACE-bound modules (ColdRoomPan) are Workbench-re-signed under `angelessigner`. The custom CA is set with
  `niagaraSigning{ aliases.set(listOf("…")); signingProfileFile.set(…); allowDefaultProfile.set(false) }` +
  `createProfile`/`generateCertificate`/`importCertificate`; trust = import the CA PEM into each install's user
  trust store.
- **Workbench re-sign needs a STORED repack** `[CERT-live]`: a WSL-OpenJDK-8-vs-Windows-Zulu-8 deflater
  mismatch makes Workbench's `JarFileSigner` throw "invalid entry compressed size"; a clean rebuild does NOT
  fix it and local `jarsigner` gives a false negative. Fix = `toolbelt/stored-repack.sh` repackages every entry
  STORED (`-0`, MANIFEST.MF first, then `.SF`/`.RSA`, then the rest) — identical content bytes keep the DEV
  signature valid AND let Workbench re-sign. `--stored` gate check enforces zero deflated entries on that path.
- HSM signing (4.14+): `JarSignerSigningProfile` wraps `jarsigner` with PKCS11; `REFLECTION`/`HSM_SIGNING`
  permissions require a validly-signed+trusted module or the station halts on load.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | vendorVersion from `vendor{defaultModuleVersion}`; com.tridium.vendor stamps every module.xml; deps are bare minimums | [CERT] | build.gradle.kts:27-34; generated module.xml:2; ColdRoomPan-rt.gradle.kts:47-50 |
| 2 | Target = niagara_home; build against the lowest (a 4.14 station rejects a 4.15-stamped jar); gate --target-version | [CERT/CERT-live] | build-verify.md:44-46; verify-module.sh:71-79 |
| 3 | Plugin family, one version per install (4.14→7.6.17…); slotomatic is a distinct task a plain :jar skips | [CERT] | settings.gradle.kts:86; build.sh:59; build-verify.md:49-53 |
| 4 | .jar (one profile + manifest + signed + <type>=module-include.xml) vs platform .dist; we deploy .jar | [CERT] | modules.txt; distribution.txt; module-include.xml; ng-deploy.sh |
| 5 | Auto-sign via niagara-signing on :jar (Niagara4Modules); supervisor accepts DEV, JACE enforces project CA | [CERT/CERT-live] | codeSigning.txt; build-verify.md:88 |
| 6 | Workbench re-sign requires stored-repack (WSL deflater mismatch) | [CERT-live] | build-verify.md:90-93; stored-repack.sh |

**Tally**: 6 [CERT] (3 with live confirmation). No unmarked claims.

## Connections
- **B12** (build overview), module-anatomy **B629-636** (manifest/jar/classloader), **B725**/signing-pki
  (identity), **B754** (the runtime versioning this build side feeds), **B739**/**B740** (why version bumps
  matter), and the build-n4-module kit (`build.sh`/`verify-module.sh`/`build-verify.md` are the operational
  form of this block).

## Open gaps
- **B756-G1**: the Tridium gradle plugin BYTECODE is not in the corpus (only New-Module-Wizard templates);
  behavior above is from our build files + the authoritative devguide.
