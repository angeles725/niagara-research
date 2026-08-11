# Block 442 — license-diff corrective addendum: a present `security/` tree does not prove licensing; `nre -licenses` is the authoritative read-only oracle, and the 4.10 implementation spans seven runtime JAR boundaries plus the signing plugin

> **Focus:** `license-diff` corrective addendum; the focus remains STOPPED 6/6. **Question:** how can a
> defender distinguish an absent license from an invalid, foreign-host, or elsewhere-stored license without
> inferring state from one directory, and which shipped JARs own each boundary? This does not reopen L1-L6;
> it corrects [B386]'s version-independent wording and inventories the actual 4.10 implementation.
>
> **Subject version:** iC-Niagara **4.10.9.14**, live Windows mini-PC, observed 2026-08-11.
> **Sources:** redacted read-only SSH evidence
> `sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt`; decompiled 4.10
> `sources/probes/B317-pentest-2026-08-01/native/jars/LicenseFile.java`; prior coverage [B316]-[B318],
> [B386]-[B387], [B392]. **Method:** documented `nre -hostid/-version/-licenses`, bounded filesystem census,
> SHA-256 and ZIP central-directory entry inventory; no binaries copied, no station started, no write.
> HostId values are redacted. Markers: `[CERT-hw]` live host, `[CERT]` local decompiled source,
> `[INFER]` bounded architectural synthesis. **Block type:** EVIDENCE / corrective addendum.

---

## 442.1 — Diagnostic verdict: genuinely unlicensed, not merely an empty directory `[CERT-hw]`

The product's own documented diagnostic is decisive: `nre -licenses` exited 0 and reported one valid
`Tridium.certificate`, then **`Licenses (node-locked): none`**, **`Features: none`**, and an empty brand
(`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:18-24`). The HostId was independently emitted by both `nre -hostid` and `nre -version`,
but only its `Win-XXXX-XXXX-XXXX-XXXX` format is preserved (`:7-16`). This re-measures and confirms [B316]
without exposing the identifier. `[CERT-hw]`

The independent storage census rules out the two obvious alternate stores: there are **zero** `*.license`
files under the install and zero matching license/security artifacts under the actual `niagara.user.home`
tree; the usual ProgramData roots do not exist (`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:26-41`). Therefore the defensible state
for this host at this time is **license absent**, not "present but invalid" and not "stored in the user home".
Prior live tests in [B316] §316.3 show `nre -licenses` displaying each invalid file with its rejection reason;
the decompiled implementation retains the error and logs `License file not loaded`
(`sources/probes/B317-pentest-2026-08-01/native/jars/LicenseFile.java:38-74`), then distinguishes HostId
mismatch (`:77-88`) from invalid/missing signature (`:172-198`). `[CERT-hw]`/`[CERT]`

## 442.2 — Correction to [B386]: `security/` presence is version-dependent baseline `[CERT-hw]`

[B386] correctly measured that its unlicensed **4.13.2.18** comparison install had no `security/` directory,
but its headline generalized that observation into "a license materializes the entire security subtree".
The live **4.10.9.14** host falsifies that generalization: it is unlicensed by the product oracle, yet has:

- a valid `security/certificates/Tridium.certificate` (833 bytes, SHA-256 preserved),
- empty `security/licenses/{db,inbox}` directories,
- three policy files under `security/policy/`, and
- a 129,124-byte custom `security/signing/signers` store (`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:26-37`). `[CERT-hw]`

The corrected invariant is narrower and stronger: **license state is represented by validated `.license`
records and loaded features, not by existence of the parent `security/` tree.** Directory absence was a true
4.13 sample result; directory presence is not a positive license test across releases. `[INFER]`

## 442.3 — Seven principal JAR boundaries, not one "license JAR" `[CERT-hw]`

The installation contains 800 JARs; the bounded entry scan identifies seven load-bearing runtime boundaries
plus the build-side signing plugin (`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:42-63`):

| Boundary | Artifact identity | Relevant package/class ownership |
|---|---|---|
| Native-daemon license reader | `bin/ext/niagarad.jar` · 520,370 B · `7A3B467A…FB44` | `com.tridium.niagarad.license.{LicenseManager,LicenseFile,LicenseUtil}` |
| Runtime crypto/signature registry | `bin/ext/nre.jar` · 984,181 B · `8C868784…D3F` | `JarSignatureRegistry`, `CertificateChainValidator`, `ASN1HostId`, `CoreTrustStore` |
| Authoritative Baja license model | `modules/baja.jar` · 2,371,720 B · `8F8351B2…46B1` | `javax.baja.license.*`, `com.tridium.sys.license.*`, license DOM/database |
| File-type wrappers | `modules/file-rt.jar` · 117,561 B · `9F5C33F1…05B2` | `BLicenseFile`, `BCertificateFile`, `LicenseGenerator` |
| Platform synchronization/channel | `modules/platform-rt.jar` · 1,014,371 B · `3C8ABA5B…125` | `PlatformLicenseManager`, `LicenseSync`, `NiagaraLicenseManager`, `BLicenseChannel` |
| Commissioning UI | `modules/platDaemon-wb.jar` · 3,861,442 B · `2F80C1F4…F88AE` | `LicenseStep*`, platform `BLicenseManager*` |
| Platform trust-store service | `modules/platCrypto-rt.jar` · 131,297 B · `9E41F846…FE0B` | `NTrustStore`, `BPlatTrustStore`, certificate messages |
| Build-side signing | `niagara-signing-plugin-1.0.10.jar` · 143,688 B · `CB4F7675…9D2B5` | signing profiles and policy/third-party JAR signing tasks |

Every Niagara runtime/module artifact above reports implementation version 4.10.9.14 and vendor Tridium;
the plugin encodes its version in its repository path (`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:43-63`). `[CERT-hw]` This map
reconciles the existing architecture rather than replacing it: [B387] describes Baja feature validation,
[B129]/[B436] the platform channel/UI, and [B392] the distinct signing trust domains.

## 442.4 — Trust structure and evidence boundary `[CERT-hw]`

Two trust artifacts are directly established without key extraction: the licensing vendor certificate is
recognized as **valid** by `nre -licenses`, while `security/signing/signers` is a separate custom binary with
its own SHA-256 (`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:18-24,33-37`). Bundled `keytool` cannot parse `signers` (exit 1), so this
run does **not** promote or restate the prior `TridiumRoot`/DSA interpretation as fresh evidence
(`sources/probes/B442-license-inventory-2026-08-11/remote-evidence.txt:65-68`). That prior public-anchor finding remains prior corpus context; this block's new
claim is only the measured separation of certificate-based licensing from the custom signer registry.
`[CERT-hw]`

## 442.5 — Defensive decision rule `[INFER]`

Use this non-invasive order:

1. Run `nre -hostid` and redact the value; retain only source and format.
2. Run `nre -licenses`; classify **none**, **invalid with reason**, or **loaded features** from the product oracle.
3. Census both `niagara.home/security` and the `niagara.user.home` reported by `nre -version`; do not assume a root.
4. Hash/inventory public certificates and JARs, but do not read private-key/keyring material.

This order distinguishes empty, invalid, foreign-host, and alternate-location cases without modifying the
station or treating filesystem shape as license truth. `[INFER]`

## 442.6 — Self-verify

- Load-bearing live tokens checked: **8/8** (`4.10.9.14`, redacted HostId format, valid Tridium certificate,
  licenses none, features none, install license count 0, user-tree match count 0, seven runtime boundaries +
  signing plugin identities).
- Scope check: conclusions apply to this 4.10.9.14 host; only the correction "directory presence is not a
  cross-version license oracle" generalizes, because one counterexample is sufficient to refute that universal.
- Secrets check: no raw HostId, credentials, private keys, tokens, or license payloads preserved.
- MCP-doc snapshots: N/A (no MCP/web sources).

## 442.x — Connections

- **Corrects [B386] §§386.3/386.5:** its 4.13 absence remains true, but "license materializes the entire
  `security/` subtree" is not release-independent. Reciprocal pointer added there.
- **Confirms [B316] §316.1 by fresh read-only measurement:** the same host remains certificate-valid and
  unlicensed, without duplicating B316's invasive pentest work.
- **Extends [B387]:** maps its Baja validation logic to the concrete 4.10 JAR ownership boundaries.
- **Connects [B392]:** keeps licensing certificates and module/signing trust stores as distinct domains.
- No new gap: the narrow defensive question is closed; `license-diff` remains STOPPED 6/6.
