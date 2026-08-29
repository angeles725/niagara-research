# Niagara N4 — chihuahua-source (CS1): the build files vs the reference template — and a §14 correction, the "over-permissioning" is the UNTOUCHED Tridium scaffold (empty permission groups, no actual grants), not real over-privilege

**Focus**: chihuahua-source · **Gap**: CS1 (manifest/build vs [B647] template) · **Session**: 2026-08-29 · **Block**: B649
**Sources** (`[CERT]` REAL source): `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/` — `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `chihuahua/chihuahua-{rt,ux,wb}/{module-permissions.xml, module-include.xml, *.gradle.kts, src}`.

**Scope**: grade chihuahua's SOURCE build files against the reference template [B647]; test the [B636] #1 / [B640] P1 "over-permissioning" premise at the source. Build mechanics = [B637]-[B639] (REMIT).

---

## 649.1 §14 CORRECTION — the "over-permissioning" is empty scaffold, not real over-privilege

[B636] #1 and [B640] P1 flagged chihuahua (and the fleet) as over-permissioned because the manifest declares `<niagara-permission-groups>` with `type="all"`. Reading the SOURCE refutes the severity `[CERT]` `chihuahua-rt/module-permissions.xml` (identical in `-ux`):
```xml
<permissions>
  <niagara-permission-groups type="all">      <!-- Insert any global permissions here. --></niagara-permission-groups>
  <niagara-permission-groups type="workbench"><!-- Insert any workbench specific permissions here. --></niagara-permission-groups>
  <niagara-permission-groups type="station">  <!--<req-permission><name>NETWORK_COMMUNICATION</name>…</req-permission>--></niagara-permission-groups>
</permissions>
```
The three groups are **EMPTY** — only the New-Module-Wizard placeholder comments, and the sole `<req-permission>` (a NETWORK_COMMUNICATION example) is **commented out**. There is **no `<java-permissions>` block at all**. Per [B635]: `<niagara-permission-groups>` → `NiagaraPermissionGroupFactory.parse` → `requestedPermissions`; with no `<req-permission>` children, that set is EMPTY, so the module requests **nothing beyond the always-on base grant** (its own `niagara.chihuahua.*` props + keyring). `checkTpk` stays false (no `<java-permissions>`).

**Correction**: chihuahua is NOT actually over-permissioned — it requests the minimal base grant. B636 #1 / B640 P1 counted the empty group DECLARATIONS (the untouched scaffold `type="all"` wrapper) as if they granted broad permissions; they do not. The real finding is cosmetic: **the module ships the untouched permission scaffold** (noise, not risk). This applies fleet-wide ([B640] P1 counted `permGroups=3` the same way) — the shop's modules are minimally-permissioned in effect, just carrying the scaffold's empty group tags. (Back-pointers added to B640 and B636.)

Recommendation downgrades accordingly: optionally delete the empty `<permissions>` for cleanliness; it is not a privilege fix. This is the value of source-level over jar-level auditing — the jar shows the tags, the source shows they're empty scaffold.

---

## 649.2 Build files vs the [B647] template

`[CERT]` — chihuahua's build matches the reference template well:
- **Root `build.gradle.kts`**: `vendor { defaultVendor("ANGELES"); defaultModuleVersion("1.3") }` — real vendor + a REAL version history (1.0→1.1 RBAC/audit→1.2 export links→1.3 wb part) — chihuahua is the ONE module that versions properly ([B640] P2 target met here, unlike the frozen-1.0 fleet). Applies `niagara`, `vendor`, `niagara-signing` plugins.
- **`settings.gradle.kts`**: `gradlePluginVersion="7.3.40"` (the 4.13.2 SDK plugin), 4-level `niagara_home` resolution ([B638]).
- **`gradle.properties`**: `niagara_home=…iC-Niagara-4.13.2.18` (compile against 4.13 SDK), Java-8 Zulu, auto-detect off ([B638]).
- **Part `chihuahua-rt.gradle.kts`**: `id("com.tridium.niagara-module")` + `niagara-signing` + `bajadoc` + `niagara-jacoco` + annotation-processors; `moduleManifest { runtimeProfile … }`. Standard multi-part layout ([B12]).
- **Slotomatic**: `[CERT]` 8 source files carry `BAJA AUTO GENERATED` markers — the slot codegen ([B631]/[B637]) is applied in the rt sources.
- **`module-include.xml`** present per part (the `<types>` registry, [B631]).

Deviations vs [B647] (real ones, from the jar audit [B636], confirmed at source):
1. **No `module.palette`** ([B636] #4) — chihuahua exports reusable components but ships no palette. Real, low.
2. **`bajadoc` + `niagara-jacoco` applied but tests are dead** ([B637]: the 7.6.17 `niagaraTest` bug) — jacoco produces no coverage; the plugins are configured but inert. Cleanup: keep `run-tests-wsl.sh` (pure JUnit), drop the dead jacoco/niagaraTest wiring.
3. The "over-permission" ([B636] #1) is DOWNGRADED to cosmetic scaffold (§649.1).

---

## 649.3 Grade

chihuahua's build is the BEST-versioned of the fleet (real 1.0→1.3 history), correctly templated (plugins, profiles, slotomatic, version-targeting), and — corrected here — minimally-permissioned in effect. Its only real build-side gaps are the missing palette and the dead test/jacoco wiring. As the sole production module, its build hygiene is actually good; the earlier "over-permissioned" flag was a jar-level artifact the source disproves.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | module-permissions.xml groups are EMPTY (placeholder comments; req-permission commented out); no <java-permissions> | [CERT] | chihuahua-{rt,ux}/module-permissions.xml | ✅ read verbatim |
| 2 | ∴ requests only base grant (empty groups ⇒ empty requestedPermissions; checkTpk false) — NOT over-permissioned (§14 refines B636#1/B640 P1) | [CERT]/[INFER] | §1 + [B635] | ✅ read+derive |
| 3 | root build: vendor ANGELES, real version 1.0→1.3 history (best-versioned of fleet) | [CERT] | build.gradle.kts | ✅ read |
| 4 | plugin 7.3.40 (4.13.2 SDK), niagara_home→iSMA 4.13.2, Java-8 | [CERT] | settings.gradle.kts + gradle.properties | ✅ read ([B638]) |
| 5 | 8 rt sources carry BAJA AUTO GENERATED (slotomatic applied); module-include.xml per part | [CERT] | rg + find | ✅ grep |
| 6 | real deviations: no module.palette; bajadoc/jacoco configured but tests dead (7.6.17) | [CERT] | [B636] #4 + chihuahua-rt.gradle.kts + [B637] | ✅ read |

**Tally**: [CERT] ×5 · [INFER] ×1 · real-source block. Permission scaffold + version + plugins token-checked verbatim. §14 refinement issued to B640 P1 + B636 #1 (back-pointers added).

## Connections

- **§14 REFINES [B640]** P1 + **[B636]** #1 — empty scaffold groups ≠ over-privilege. Back-pointers added. **[B635]** — empty niagara-permission-groups ⇒ base grant only. **[B647]/[B637]-[B639]** — the template graded against. **[B631]** — slotomatic/module-include.
- Forward: CS2 (rt control), CS6 (audit reconcile), CS8 (verdict: build hygiene good).

## Gaps uncovered

- None new. The dead jacoco/niagaraTest wiring is a cleanup ([B637] already named the 7.6.17 cause), not a new gap.
