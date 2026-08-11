# Block 443 — authorization differential: the first proven A/B divergence is license discovery, and B's station denial is the expected `tridium:nre` consumer result

> **Focus:** bounded `license-diff` corrective synthesis; the focus remains STOPPED 6/6. **Question:** where
> does authorization first diverge between a known-good licensed control A and the unlicensed laboratory
> install B, and which later differences are merely version/vendor confounders? This reconciles [B386]-[B392]
> and [B442]; it does not repeat their generic architecture maps.
>
> **Systems:** A = live local Honeywell OptimizerSupervisor **4.14.0.162**, licensed; B = remote iC-Niagara
> **4.10.9.14**, unlicensed. **Critical control:** these are not the same build or distribution, so no JAR,
> native-binary, or configuration hash is attributed to licensing. **Method:** read-only `nre -version` and
> parsed `nre -licenses` on A; read-only aggregate `nre -licenses` over the authorized B SSH channel; preserved
> decompiled 4.10 source; prior B consumer traces. No station/Workbench process was launched and no file was
> modified on either install.
>
> **Sources:** `sources/probes/B443-authorization-differential-2026-08-11/decision-evidence.txt`; 4.10
> `sources/probes/B317-pentest-2026-08-01/native/jars/vineflower/{NLicenseManager,NodeLockedLicenseManager,
> LicenseFile,CertificateFile}.java`; prior runtime [B316], licensed feature map [B387], signing map [B392],
> B inventory [B442]. HostId values are redacted. Markers: `[CERT-hw]` live diagnostic, `[CERT]` source or
> preserved corpus evidence, `[INFER]` bounded synthesis. **Type:** MIXED evidence/synthesis.

---

## 443.1 — A and B provenance is resolved, but A is not a same-build control `[CERT-hw]`

The control is the actual install at `C:\Honeywell\OptimizerSupervisor-N4.14.0.162`. Its own NRE reports
`niagara.home` at that path, vendor version **4.14.0.162**, host model Workstation, and a redacted HostId
(`decision-evidence.txt:5-14`). Its own license oracle currently reports **3 valid certificates, 3 valid
licenses, and 178 loaded features**, including `tridium:nre`, `tridium:station`, `tridium:workbench`, and
`tridium:developer` (`:15-23`). This establishes A as a known-good **licensed diagnostic control** without
assuming that directory presence means licensed. `[CERT-hw]`

B is the authorized laboratory endpoint at `C:\Niagara\iC-Niagara-4.10.9.14`. A fresh aggregate diagnostic
reports **1 valid certificate, 0 valid licenses, and 0 loaded features** (`:27-35`), independently confirming
[B442 §442.1]. Its state is genuinely unlicensed; no evidence shows B receiving a licensed feature despite
that absence. `[CERT-hw]`

The pair differs in host identity, release, distribution/OEM overlay, modules, and configuration
(`:42-43`). Therefore **no A/B binary hash is comparable for licensing attribution**. A is valid for the
license-state/control-flow question because each build supplies the same product oracle; it is not valid for
claiming that differing JAR bytes, trust files, or defaults are caused by licensing. `[CERT-hw]`/`[INFER]`

## 443.2 — Stage-by-stage decision map and the first divergence

| Stage and decision position | Component / probable purpose; caller → callee | Input → output; filesystem/config dependency | Expected A / observed B; execution status | Confidence; preserve; read-only confirmation / falsification |
|---|---|---|---|---|
| **1. Host identity** (before authorization) | Native NRE supplies `Nre.getHostId()`; `NodeLockedLicense.isLicenseHostIdValid()` compares it to parsed `hostId` (`NodeLockedLicenseManager.java:32-57`). | Machine identity + license `hostId` → boolean host binding. | Both emit a redacted `Win-...` identity. A's valid licenses prove its binding accepted; B has no license candidate, so **no B host-mismatch check executes**. Classification: **IDENTITY_DIFFERENCE**, not causal. | High for execution/binding semantics. Preserve only format/source. Harden: never use directory name or HostId alone as authorization proof. Confirm: valid license appears on its own host. Falsify causal attribution here: B remains zero-license before any candidate reaches comparison. |
| **2. License discovery** (before authorization; **first proven divergence**) | `NLicenseManager.postInit()` → `load()` → `loadCertificates()` + abstract `loadLicenses()` (`NLicenseManager.java:180-203,283-290`); node-locked implementation scans `.license` files (`NodeLockedLicenseManager.java:13-30`). | `niagara.home/security/{certificates,licenses}` plus database normalization from `inbox`/root into `db/<hostId>` [B316 §316.3.1] → arrays of certificate/license objects. | A discovers 3 valid licenses; B discovers 0 (`decision-evidence.txt:15-18,30-35`). **Output differs before license parsing can occur on B.** Classification: **INPUT_DIFFERENCE**, **VALIDATION_RESULT_DIFFERENCE**. | **Proven**, very high. Preserve aggregate counts and path, never payloads. Harden: monitor both stores and the product oracle; do not infer state from `security/` existence. Confirm: repeat aggregate `nre -licenses`. Falsify: B reports any valid node-locked license. |
| **3. Parse + host/date/version binding** (before authorization) | `LicenseFile.load()` parses XML then checks host, generated-date floor/skew, expiration, and Tridium version/SMA compatibility (`LicenseFile.java:38-75,77-170`). | Discovered license XML + current HostId/time + Baja module metadata → valid license or specific error. | A reaches and passes this stage for 3 files. B has **no input**, so parser/binding do not execute for a license; this is not a B parse failure. Classification: **CONTROL_FLOW_DIFFERENCE** caused by the stage-2 input difference. | High. Preserve only validity/error class. Harden: retain typed validation errors and trusted time telemetry without retaining payloads. Confirm: oracle shows `{valid}` or a typed rejection. Falsify: a B invalid-file reason would prove parsing executed and move the first divergence to that check. |
| **4. Certificate validation** (before authorization) | `CertificateFile.load()` parses vendor/public-key metadata and verifies certificate signature against the embedded master public key (`CertificateFile.java:23-90`); `LicenseFile` resolves the vendor cert before validating the license signature (`LicenseFile.java:77-88,172-198`). | `security/certificates/*.certificate` + embedded public root → valid vendor key; license canonical bytes/signature → accepted/rejected license. | A: 3 certs valid and 3 licenses accepted. B: Tridium cert **valid**, but no license signature is presented. Classification: **TRUST_STORE_DIFFERENCE** (3 vs 1 cert), but **not causal**; no B certificate or license-signature rejection occurred. | High. Preserve names/counts/status only. Harden: restrict and integrity-monitor certificate stores; keep licensing and module trust domains separate. Confirm: certificate remains valid while licenses remain zero. Falsify trust causality: a B `Invalid certificate`/`Invalid signature` result, absent here. |
| **5. Feature materialization / entitlement** (authorization input) | `LicenseFile.loadFeature()` constructs `NFeature` and calls `NLicenseManager.addFeature()` (`LicenseFile.java:202-229`); `getFeature`/`checkFeature` looks up `vendor:feature`, with `checkFeature` also enforcing expiry (`NLicenseManager.java:38-65`). | Valid license feature elements → in-memory feature map → feature object or `FeatureNotLicensedException`. | A materializes 178 features, including `tridium:nre`; B materializes 0. **This is the direct authorization-input difference.** Classification: **INPUT_DIFFERENCE**, **VALIDATION_RESULT_DIFFERENCE**. | **Proven**, very high. Preserve aggregate and named gate features. Harden: alert on required-gate absence and expiry before operational startup. Confirm: `nre -licenses` feature count/gate names. Falsify: B loads `tridium:nre` yet still throws FeatureNotLicensed for that key. |
| **6. SMA + module signature** (module-load checks, adjacent to but distinct from the `nre` entitlement decision) | SMA: `NLicenseManager.checkModuleReleaseDate()` compares module release date to a matching valid vendor license (`NLicenseManager.java:121-138`). Signature: standard JAR verification + `validateCertChain` at add/class-load [B392 §§392.2,392.6]. | Valid vendor license dates + module metadata; signed JAR + runtime trust anchor + verification mode/developer entitlement → module accepted/rejected. | A's `developer{skipModuleValidation=true}` is loaded [B387 §§387.1,387.6]; B has no developer feature. That is a **CONTROL_FLOW_DIFFERENCE**, but neither captured B startup nor fresh diagnostics report a module-signature failure; B reaches the later `tridium:nre` check. Version/vendor trust and JAR differences are **BINARY_DIFFERENCE**, **CONFIGURATION_DIFFERENCE**, **TRUST_STORE_DIFFERENCE** confounders, not the observed denial cause. | High for code/config, medium for untraced exact ordering. Preserve module-verification logs and exact build identities, not keys. Harden: enforce production module verification and avoid developer skip grants. Confirm: read-only startup log names signature/SMA result before entitlement. Falsify causal exclusion: a signature/SMA exception precedes `FeatureNotLicensedException`. |
| **7. Authorization decision** | Consumer calls `LicenseManager.checkFeature("Tridium","nre")`; absent key throws at `NLicenseManager.checkFeature` (`NLicenseManager.java:52-64`). [B316 §316.3.2] locates this in `Nre.runClass` for platform/station launch. | Feature map + requested key → return feature or exception. | A's current map contains `tridium:nre`; B's map does not, and B previously threw exactly `FeatureNotLicensedException: tridium:nre` for `plat.exe` and `station.exe` (`decision-evidence.txt:37-39`). Classification: **VALIDATION_RESULT_DIFFERENCE**, **CONTROL_FLOW_DIFFERENCE**. | **Proven root-cause edge**, very high. Preserve exception class/key/stack location. Harden: fail closed with a stable typed event suitable for monitoring. Confirm: same read-only launch trace on a licensed same-build control returns past this call. Falsify: B fails earlier or the exception names another key. |
| **8. Workbench / station / module startup** (after decision) | `Nre.runClass` dispatches the selected tool; station/platform consumers request `tridium:nre`, while the B Workbench launcher did not take that gate in the captured test [B316 §316.3.2]. Modules then enforce their own distributed feature gates [B387 §387.3]. | Authorization result + selected main class + module set/config → process continues, faults, or exits. | B station/platform stop at the missing-feature exception; B Workbench stayed running. This is expected **consumer-specific behavior**, not unauthorized station operation. A previously had a running PRUEBAS station, but current census has zero station processes and a lone lock is not treated as current proof (`decision-evidence.txt:23-24,37-40`). Classification: **RUNTIME_DIFFERENCE**, **CONTROL_FLOW_DIFFERENCE**, **VALIDATION_RESULT_DIFFERENCE**. | High for B and historical A; current A startup is untested by design. Preserve process/exit/stack evidence. Harden: test each consumer independently and label unlicensed Workbench state clearly. Confirm: same-build licensed A crosses the check and starts; falsify: B station runs with features still zero, which is not observed. |

## 443.3 — Differential findings and exact anomaly classifications `[CERT-hw]`/`[INFER]`

| Observed difference | Classification(s) | Causal status |
|---|---|---|
| Different redacted host identities | **IDENTITY_DIFFERENCE** | Real but not causal: A binds successfully; B has no candidate license to mismatch. |
| A 3 valid licenses / 178 features; B 0 / 0 | **INPUT_DIFFERENCE**, **VALIDATION_RESULT_DIFFERENCE** | **Proven first divergence and root cause input.** |
| A 3 vendor certs; B 1 valid Tridium cert | **TRUST_STORE_DIFFERENCE** | Non-causal in this observation: B's available cert validates and no license reaches it. |
| 4.14 Honeywell vs 4.10 iC JAR/native/defaults/module sets | **BINARY_DIFFERENCE**, **CONFIGURATION_DIFFERENCE** | Confounder. No binary hash is used as license evidence. |
| A loads `developer{skipModuleValidation=true}`; B does not | **INPUT_DIFFERENCE**, **CONTROL_FLOW_DIFFERENCE** | Real later-path difference; not the captured station denial, which names `tridium:nre`. |
| B station/platform denied; B Workbench runs | **CONTROL_FLOW_DIFFERENCE**, **VALIDATION_RESULT_DIFFERENCE**, **RUNTIME_DIFFERENCE** | Expected per-consumer behavior; no bypass or anomalous feature authorization observed. |

## 443.4 — Ranked hypotheses and falsifiable read-only experiments

1. **Proven root cause (0.98):** B discovers no valid license, materializes no `tridium:nre` entitlement, and
   the station/platform consumer throws for that exact missing key. Confirmation: current B `0/0` oracle plus
   preserved exception. Falsification: B loads `tridium:nre` while the same call still throws.
2. **Strong adjacent explanation, not a second root cause (0.85):** Workbench starts because its captured
   launcher path does not request the `tridium:nre` gate, while station/platform do. Confirmation: read-only
   same-build startup traces showing consumer call sites. Falsification: a Workbench trace throws the same key
   before becoming usable.
3. **Unresolved exact-order hypothesis (0.60):** module signature/SMA checks complete before the final
   station `tridium:nre` consumer check. Existing evidence shows the later exception and no signature/SMA error,
   but does not timestamp every internal check. Confirmation: diagnostic-level startup log on an unmodified
   same-build licensed/unlicensed pair. Falsification: a signature/SMA exception occurs first.
4. **Disfavored (0.10):** B's denial is caused by host mismatch, certificate trust, or OEM binary drift.
   Current evidence falsifies those as the observed edge: no B license exists to bind/verify, its certificate is
   valid, and the thrown result is missing `tridium:nre`, not a trust or linkage error.

## 443.5 — Root-cause conclusion, hardening, and exact remaining gap `[INFER]`

**Conclusion:** the first proven divergence is **license discovery**. A supplies three valid node-locked
licenses; B supplies none. Parsing, host binding, and license-signature validation therefore execute for A
but have no B license input. Entitlement evaluation yields `tridium:nre` for A and no features for B; the B
station/platform authorization consumer then denies startup exactly as designed. Workbench starting on B does
not establish a bypass because the station consumer remains denied and no protected feature was observed
authorized. Confidence: **very high (0.98)** for this causal edge; **medium** for total internal ordering before
the captured consumer stack.

Defensive remediation/regression:

- provision B only through the legitimate vendor/OEM license workflow and verify with redacted `nre -licenses`;
- alert on `licenses=0`, `features=0`, invalid vendor certificates, and `FeatureNotLicensedException`, while
  distinguishing a valid baseline certificate from an actual license;
- remove production dependence on `developer{skipModuleValidation=true}` through an appropriately issued
  production license/profile; keep module verification and trust-store monitoring separate from licensing;
- regression-test station, platform, Workbench, and representative module gates independently: a green
  Workbench process is not a station-authorization oracle;
- compare binaries/configuration only between identical product/vendor/build artifacts; use runtime license
  diagnostics for cross-build license-state comparisons.

**Exact remaining evidence gap:** no legitimate **licensed iC-Niagara 4.10.9.14** control and no paired
diagnostic-level startup trace exist. Therefore this iteration cannot prove byte-identical control-flow order,
compare meaningful A/B JAR hashes, or show a current A station crossing the same `tridium:nre` call. The next
read-only step is to acquire authorized access to that same-build licensed control, capture redacted
`nre -version/-licenses`, then capture station/platform/Workbench exit and diagnostic logs without changing
either install. Until then, build-level attribution remains explicitly unresolved.

## 443.6 — Self-verify

- Load-bearing tokens checked: **14/14** (both paths/versions, A 3/3/178, four A gate features, B 1/0/0,
  `loadLicenses`, HostId equality, certificate validation, `checkFeature`, B exception/Workbench outcome).
- Scope check: conclusion is restricted to the observed authorization edge; binary/config ordering is not
  generalized across 4.10 iC and 4.14 Honeywell.
- A/B hashes: **none compared** because artifacts are not same-build comparable.
- Secrets check: HostId values suppressed; no credentials, private keys, tokens, signatures, or license XML
  captured. MCP/web sources: N/A.

## 443.x — Connections

- **Reconciles [B386]-[B391]:** preserves their version/vendor caveat and uses their license API/signing map
  without treating cross-build hashes as license evidence.
- **Extends [B442]:** adds a current licensed A oracle and pins the first A/B divergence plus consumer result.
- **Uses [B316]:** B's observed station/platform denial and Workbench continuation.
- **Uses [B387]:** entitlement API and A's gate-bearing `developer` feature.
- **Uses [B392]:** module signature validation is a separate trust domain and not the captured denial.
- No new gap is opened in this stopped focus; the same-build control remains an explicitly typed evidence gap.
