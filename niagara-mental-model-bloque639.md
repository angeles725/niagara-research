# Niagara N4 — own-modules-audit (OMB3): signing is convention-driven (no `niagaraSigning{}` block) via the `angelessignerCA` alias in `niagara_user_home/security/keystore.jceks` — the active ANGELES chain replacing the legacy SEJOFA one — plus the `niagara-tools` deploy+KB repo

**Focus**: own-modules-audit · **Gap**: OMB3 (signing = ANGELES + niagara-tools) · **Session**: 2026-08-29 · **Block**: B639
**Sources** (`[CERT]` real operator source; SECRETS DISCIPLINE — alias/filenames/paths only, never key material):
- `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/build.gradle.kts` · `chihuahua/chihuahua-{rt,ux,wb}/*.gradle.kts`
- `.../chihuahua/openspec/changes/**/{proposal,design,archive-report}.md` (signing alias evidence)
- `/mnt/c/Users/equipo/Niagara4.13/iSMA CONTROLLI/security/` (keystore structure)
- `/home/cristian/modulos_niagara_n4/niagara-tools/` (VERSION, scripts, docs/knowledge-base)

**Scope**: the operator's statement "la firma vigente que solemos utilizar para firmar los módulos que se usan en stations es la de ANGELES." Signing CRYPTO/trust-anchor internals = [B392]/[B489]/[B492] (signing-pki, REMIT). Reference = [B636].

---

## 639.1 Signing is wired by convention, not by an explicit config block

`[CERT]` — grep of all `.kts`/`.gradle`/`.properties` in the repo finds **no `niagaraSigning { }` block**. Signing is applied purely by the plugin:
- `build.gradle.kts:18` — `id("com.tridium.niagara-signing")` on the root ("registers a factory only on the root project").
- `chihuahua-rt.gradle.kts:15` · `chihuahua-ux.gradle.kts:13` · `chihuahua-wb.gradle.kts:16` — the same plugin per part ("configures the correct signing of modules").

With no explicit block, the plugin uses the **Niagara user-home security store** of the `niagara_home`/`niagara_user_home` in effect ([B638]). The store `[CERT]` `…/Niagara4.13/iSMA CONTROLLI/security/`:
```
keystore.jceks      ← private signing keys (JCEKS keystore)
cacerts.jceks       ← trusted CA certs
untrusted.jceks
signing/            ← signing profile config + signers/
exemptions.tes
```
So signing = the `jar` task → niagara-signing plugin → key from `niagara_user_home/security/keystore.jceks`. This is why [B637] saw no explicit `sign`/`dist` task and [B632] saw every jar emerge already carrying `META-INF/*.SF/.RSA`.

---

## 639.2 The active identity: `angelessignerCA` (ANGELES), replacing the legacy SEJOFA chain

`[CERT]` `openspec/changes/chihuahua-reflow-sanluis-replica/proposal.md:302,319`:
```
| Vendor SEJOFA signing chain falla en CI | … | `angelessignerCA` ya está validado en C1+C2. Reusar mismo cert chain. |
- Plugin Tridium signing con `angelessignerCA`.
```
and `archive-report.md`: "All jars signed with `angelessignerCA` ✅". So the **active signing alias is `angelessignerCA`** — the ANGELES chain — and **SEJOFA is the legacy chain that fails in CI**, deliberately abandoned. This confirms the operator's statement with evidence, and resolves the open question in [B636] deviation #6:

| Chain | Where seen | Status |
|---|---|---|
| **ANGELES / `angelessignerCA`** | active signing alias; new modules | **CURRENT** (validated C1+C2) |
| **SEJOFA / `SEJOFA_C`** | `sdash-rt.jar` `META-INF/SEJOFA_C.SF` ([B632]); "falla en CI" | LEGACY (being replaced) |
| `NIAGARA4` (SF/RSA filename) | most jars' `META-INF/NIAGARA4.{SF,RSA}` ([B632]) | the signature-block NAME in the jar; its relation to the keystore alias/CN is a signing-pki detail — REMIT [B392] |

The vendor IDENTITY (manifest `vendor="ANGELES"`, [B638] `defaultVendor("ANGELES")`) and the signing CHAIN (`angelessignerCA`) are now aligned on ANGELES; the mixed `NIAGARA4`/`SEJOFA_C` signer-block names across older jars ([B632]) reflect the migration from the SEJOFA chain. Whether any of these certs is anchored to a trusted root or is a self-managed dev CA is [B392]'s question — and on the live station it loads regardless because `moduleVerificationMode=low` ([B398]/[B635]).

`[CERT]` `proposal.md:320` also records the build's WSL/NTFS bridge: "Robocopy WSL→Win→WSL para slotomatic + jar build" — signing/build runs on the Windows side of the security store, with WSL copying artifacts across.

---

## 639.3 `niagara-tools` — the shop's deploy + knowledge repo

`[CERT]` `niagara-tools/VERSION` = **0.3.0**. It is NOT a build system, signer, or scaffolder — it is a cross-project **deploy wrapper + knowledge base**:
- `scripts/ng-deploy.sh` — the canonical deploy ([B637] §637.4): backup → `./gradlew` (mode A/B/C) → copy jars to `STATION_MODULES_DIR` → verify types vs `EXPECTED_*_TYPES`; phase exit codes 10/20/30/40/50.
- `docs/knowledge-base/` — the shop's hard-won gotchas: `slotomatic.md` (when to run it, the "slotomatic myth"), `wsl-build-gotchas.md` (the `-P` overrides + gradlew path), `hot-reload-rules.md` (Java = station restart; JS/CSS = browser reload only), `bql-gotchas.md` (N4.14 BQL bugs + persistent-ack).
- `tests/ng-deploy.bats` (bats unit tests) + `tests/smoke-checklist.md` (manual A/B/C).
- `CONTRIBUTING.md` (SemVer bump policy) · `CHANGELOG.md` · `CLAUDE.md` (agent rules).

`niagara-tools` is the operator's answer to two reference-skeleton weaknesses: the vendor install path has no backup ([B633]) → ng-deploy takes one; and the build variant/slotomatic rule ([B637]) is tribal → the KB writes it down. It is a genuine strength of the shop's setup.

---

## 639.4 What this establishes + how to improve

- **Signing identity is now unambiguous**: active = `angelessignerCA` (ANGELES) from `niagara_user_home/security/keystore.jceks`; SEJOFA is legacy (CI-failing) and any `SEJOFA_C`-signed jar (e.g. `sdash`) should be re-signed with `angelessignerCA` for consistency (deviation to fix in OMA4).
- **The convention (no explicit block) is fine** but couples signing to whichever `niagara_user_home` is active — document that the security store must contain `angelessignerCA` before a build, or the jar signs with the wrong/no cert. A one-line preflight in `ng-deploy.sh` (assert the alias exists in the keystore) would catch a mis-provisioned dev machine early.
- **Trust-anchor posture is out of scope here** (REMIT [B392]): confirm whether `angelessignerCA` chains to a root the target stations trust, or whether loading relies on `moduleVerificationMode=low` ([B398]) — the latter is a hardening risk, not a build convenience.
- **niagara-tools is a strength** — keep it as the single source of build/deploy truth; fold the OMB1/OMB2 findings (variant rule, version/plugin coupling) into its KB.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | no `niagaraSigning{}` block; niagara-signing plugin applied root + per part | [CERT] | rg (∅) · build.gradle.kts:18 · part .kts:13/15/16 | ✅ grep+read |
| 2 | security store = niagara_user_home/security/{keystore.jceks,cacerts.jceks,signing/,…} (JCEKS) | [CERT] | ls …/iSMA CONTROLLI/security/ | ✅ read (names only) |
| 3 | active signing alias = angelessignerCA (ANGELES); jars signed with it (C1+C2, archive) | [CERT] | proposal.md:302,319 · archive-report.md | ✅ read verbatim |
| 4 | SEJOFA chain is legacy/CI-failing; SEJOFA_C on sdash jar ([B632]); NIAGARA4 = jar block name | [CERT] | proposal.md:302 · [B632] | ✅ read verbatim |
| 5 | build/sign runs Windows-side with Robocopy WSL↔Win bridge | [CERT] | proposal.md:320 | ✅ read verbatim |
| 6 | niagara-tools v0.3.0 = deploy wrapper (ng-deploy.sh) + KB (slotomatic/wsl/hot-reload/bql) | [CERT] | VERSION · scripts/ · docs/knowledge-base/ | ✅ read |

**Tally**: [CERT] ×6 · [INFER] ×0 · build-process block. SECRETS DISCIPLINE honored (alias names, keystore filenames/type, paths — no key material). Signing alias token-checked verbatim (proposal.md:302,319, read directly after a grep-render glitch).

## Connections

- **[B636]** dev#6 (signer identity) — resolved: active = angelessignerCA (ANGELES), SEJOFA = legacy. **[B632]** — the `NIAGARA4`/`SEJOFA_C` jar signer-block names. **[B392]/[B489]/[B492]** — the trust-anchor crypto (REMIT). **[B633]** — install has no backup; ng-deploy adds one. **[B637]/[B638]** — the build/version context this signing rides on.
- Forward: OMA1 (systemic patterns) now has the signer split explained; OMA4 (sdash SEJOFA_C) has its re-sign fix.

## Gaps uncovered

- None new for the backlog. Whether `angelessignerCA` chains to a station-trusted root vs self-managed CA is a signing-pki question (REMIT [B392]); confirming it needs cert-chain inspection, not blocking this focus.
