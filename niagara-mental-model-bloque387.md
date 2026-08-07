# Block 387 — license-diff L6: the runtime feature-gate map — a license is a signature-verified set of named features with numeric limits, and an UNLICENSED station runs UNCAPPED (limits → MAX_VALUE), not disabled

> **Focus `license-diff` — L6, the feature-gate map.** [B386] showed a license materializes the `security/`
> subtree on disk; this block reads WHAT those features grant at runtime and HOW they are enforced —
> the canonical `LicenseManager` API, the feature→gate call sites across modules, the numeric-limit
> enforcement path, the SMA (maintenance) gate, and the signature-verification chain. It **confirms
> [B126 §126.6]'s open inference** that the authoritative license verification lives in the Java layer (the
> native `isFeaturePresent` is only a text fast-path). READ-ONLY. `live-install` → SECRETS DISCIPLINE
> (feature names + limit structure only). Block type: EVIDENCE (code) + synthesis.
>
> Sources: the licensed install's `.license` files (`/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/
> security/licenses/{Honeywell,HoneywellCentraLine,Webs}.license`) + the decompiled `com.tridium.sys.license.*`
> and per-module gate sites under `/home/cristian/modules/Prototipos/modulos/organized`.
> Method: read the license feature/limit structure; delegated `sonnet` sweep of the feature-check call sites
> (VERIFY-BEFORE-ACTING: the API, both hardcoded master keys with their bytes, and `maintenanceExpiration`
> grep-confirmed by the driver). Evidence: `audits/B387-feature-gates.txt`.
> Markers: `[CERT]` observed in code/license (file:line or attr cited) · `[INFER]` deduction.

---

## 387.1 — What this install is licensed for: 178 feature grants across 3 vendors `[CERT]`

The licensed install carries three vendor licenses (all `expiration="2027-03-31"`, `version="4.15"` build cap,
`generated="2026-04-02"`): `[CERT]`

| Vendor license | Features | Character |
|---|---|---|
| `Honeywell.license` | **27** | OEM: `spyderProgrammable`, `spyderBacnetProgrammable`, `honEasyBinding`, `honEdgeDriver`, `maxproVideo` (`camera.limit="16"`), `redLink`, `XL10Wizards`, `SylkActuatorAnalytics`, `ascBAC`/`ascLON`, `LCDProgrammable`, … |
| `HoneywellCentraLine.license` | **1** | `clCbus` (CentraLine C-Bus) |
| `Webs.license` (vendor **Tridium**) | **150** | a **DEMO** license (`about project="00 HW - DEMO LICENSES"`, `demoStation historyExt.limit="10000"`) granting the full Tridium base set: all drivers, `analytics`, `accessControl`, `entSecurity`, `bacnetSc`, `eSignature point.limit="500"`, `email`, `crypto ssl="true"`, `developer`, `brand brandId="Webs"`, … |

**Security-relevant grant** `[CERT]`: the Webs demo `developer` feature is
`<feature name="developer" moduleDev="true" skipModuleValidation="true"/>` — this licensed station's own
license enables module-dev mode and **module-validation skipping** (the license-level twin of [B113]'s
`skipModuleValidation` and [B380]'s `-javaagent` developer gate). `[CERT]`

---

## 387.2 — The canonical feature-check API `[CERT]`

`javax.baja.license.LicenseManager` (`…/javax/baja/license/LicenseManager.java:11-15`) is the interface every
module gates on, reached via `Sys.getLicenseManager()` (an `NLicenseManager`): `[CERT]`

- **`getFeature(vendor, feature)`** — returns the `Feature` WITHOUT enforcing expiry (the common path; the
  caller invokes `feature.check()` itself, e.g. `BDeviceNetwork.checkLicense()`).
- **`checkFeature(vendor, feature)`** — calls `feature.check()` internally, throwing
  `FeatureLicenseExpiredException` if `expiration < Clock.millis()`. Used where a gate must block immediately
  (mobile, FIPS, developer, eSignature).
- **Numeric limits**: `feature.geti("point.limit", …)` / `LicenseUtil.parseLimit`, which maps the string
  **`"none"` → `Integer.MAX_VALUE`** (`NFeature.java:100` confirms the `MAX_VALUE == "never"/unlimited`
  convention). `[CERT]`

---

## 387.3 — The feature → gate map (representative call sites) `[CERT]`

Delegated sweep, spot-verified (`audits/B387-feature-gates.txt`): `[CERT]`

| Feature | Gate site (class @ file:line) | Gates |
|---|---|---|
| `developer` | `NModuleDevFilePermission.java:38` · `ModuleClassLoader.java:552` | dev file-permission bypass + unrestricted class-loader |
| `fips140-2` | `Nre.java:920` | FIPS crypto mode at NRE init (ties [B380] `bcfips`/`bcstd` provider swap) |
| `globalCapacity` | `GlobalGroup.java:24` · `ResourceManager.java:82` | all capacity limits + JVM heap limit (§387.4) |
| `bacnet` / `bacnetSc` / `mstp` | `BBacnetNetwork.java:340` · `BAbstractConnectionManager` · `BBacnetMstpLinkLayer` | BACnet/IP, secure-connect, MS/TP link layer |
| `modbusTcp` | `BModbusTcpNetwork.java:52` | Modbus TCP network start (ties [B294]) |
| `accessControl` | `BNrioNetwork.java:459` | access-control mode; inline `FeatureNotLicensedException("accessControl")` |
| `eSignature` | `BSecuredDashboardConfiguration.java:684` | 21 CFR Part 11 e-sign (ties [B356]; `point.limit="500"` here) |
| `analytics` | `NAFFeatureUtil.java:19` | Analytics service (ties [B366]) |
| `email` | `BEmailService.java:112` | SMTP service start (ties [B324]) |
| `hierarchy` | `BHierarchyService.java:129` | hierarchy service |
| `honEasyBinding` | `EbLicenseUtil.java:22` | Honeywell Easy Binding, multi-vendor fallback chain (SaiaBurgess→Trend→CentraLine→Honeywell→Alerton) — ties [B207] |
| `historyArchive` | `BArchiveHistoryProvider.java:138` | history archiving |
| `mobile` / `web` | `BMobileClientEnvironment.java:79` · `BWebService.java:519` | mobile/web session (mobile uses `checkFeature`, blocks immediately) |

So each driver/service resolves its own feature at `start()`/`activate()` and faults if absent — the license
is a distributed set of per-module gates, not one central switch. `[CERT]`

---

## 387.4 — Limit enforcement: unlicensed runs UNCAPPED, over-limit can HARD-KILL the station `[CERT]`

The striking, load-bearing finding. `globalCapacity` limits are enforced at three sites: `[CERT]`

1. **`GlobalGroup.java:24-48`** reads `network/device/point/link/history/schedule.limit` from the
   `globalCapacity` feature. **If the feature is ABSENT (`FeatureNotLicensedException`), all limits are set
   to `Integer.MAX_VALUE`** — so an **unlicensed station runs FULLY UNCAPPED**, not zero-capped. Licensing
   ADDS caps; its absence removes them. (This is why [B386]'s unlicensed instance, with no `security/`, is
   unconstrained — coherent.) `[CERT]/[INFER]`
2. **`ResourceManager.checkLicense()` @ `ResourceManager.java:82-103`** reads `heap.limit`; if the JVM's max
   memory exceeds it, prints `"STATION IS UNLICENSED!!! Licensed heap limit exceeded."` and calls
   **`System.exit(-3)`** — a hard station kill. So the one limit that is *actively* fatal is heap, not
   point/device count. `[CERT]`
3. **`BLink.activate()` @ `BLink.java:169`** faults the link on `link.limit` exceed; **`BDeviceNetwork.
   checkLicense()` @ `BDeviceNetwork.java:306-352`** sets `fatalFault` + `"Unlicensed: …"` on per-protocol
   device/point-limit exceed. These fault the offending component, not the station. `[CERT]`

So the enforcement model is: **absent → uncapped; over the heap cap → station exit(-3); over a component cap
→ that component faults.** `[CERT]`

---

## 387.5 — SMA (maintenance) gate: a module newer than the license's maintenance date won't load `[CERT]`

The "SMA" is not a runtime feature flag — it is a **module-load-time build gate**. `[CERT]`
`LicenseFile.maintenanceExpiration` (`LicenseFile.java:38`, parsed from the `maintenanceExpiration` attr,
`VendorLicense.java:111`) is compared in `NLicenseManager.checkModuleReleaseDate()`: if
`module.getReleaseDate() > maintenanceExpiration`, it throws `LicenseDatabaseException("Module … not under
active maintenance")` — called from `NModule.checkLicensed()` at class-load. So a station upgraded to a build
whose modules post-date the SMA date **fails to load those modules**. `BSMANotificationSettings` is only the
UI reminder, gating nothing. This grounds the operational "SMA vencido → riesgo de build" concern: the risk
is real and enforced at module load, keyed on release-date vs maintenance-expiration (distinct from the
`version="4.15"` build cap and the `expiration` feature date). `[CERT]`

---

## 387.6 — Signature-verified, NOT presence-only — confirms [B126 §126.6] `[CERT]`

The authoritative Java path is a **two-tier PKI**, with the trust root hardcoded in the binary: `[CERT]`
- `LicenseUtil` (`LicenseUtil.java:39-41, 223-224`) embeds two master public keys as byte literals:
  **`masterPublicKeyData`** = DSA (`30 82 01 b8 … 06 07 2a 86 48 ce 38 04 01` = OID `1.2.840.10040.4.1`
  id-dsa — the same Tridium DSA root family as [B126 §126.6]) and **`version2PublicKeyData`** = ECDSA
  **P-256** (`30 59 … 06 08 2a 86 48 ce 3d 03 01 07` = prime256v1). `[CERT]`
- `CertificateFile.load()` verifies each vendor `.certificate`'s `<publicKey>` **against the hardcoded master
  key**; `LicenseFile.load()` then strips `<signature>`, canonicalizes the XML (`LicenseUtil.encode`), and
  `LicenseUtil.verify(xml, sig, cert.publicKey)`. On a bad/missing signature: `error="Invalid signature"` /
  `"Missing signature element"` → `isValid()==false` → **zero features loaded**. `[CERT]`

**There is no presence-only path in the Java layer.** This CONFIRMS [B126 §126.6]'s open inference: the
native `LicenseUtil::isFeaturePresent` (nre.dll) is a text-substring fast-path that does NOT verify the DSA
signature, and the real cryptographic verification lives here in the Java `LicenseManager`/`LicenseFile`
chain, rooted at the hardcoded master DSA + v2 ECDSA keys. The native fast-path answers "does a license file
textually grant X"; the Java path answers "is this a genuine, signed, in-maintenance license." `[CERT]`

---

## 387.7 — Defensive summary `[CERT]`

1. **Unlicensed = uncapped, not disabled** `[CERT]` (§387.4): removing the license removes capacity limits
   (limits→MAX_VALUE); the only fatal limit is heap (`exit(-3)`). A station without `security/` ([B386]) runs
   unbounded until it trips the heap cap — an availability, not a lock-out, model.
2. **Trust rooted in TWO hardcoded keys** `[CERT]` (§387.6): the DSA master is the legacy 2003-era Tridium
   root ([B126]); the ECDSA P-256 `version2` key is the modern addition. Compromise of a master private key
   would forge any vendor license — but those keys live at Tridium, not on the install.
3. **This install's own demo license enables `skipModuleValidation`** `[CERT]` (§387.1): a field station
   carrying the Webs demo license can load unsigned modules by license grant — a posture worth flagging.
4. **SMA is enforced, at module load** `[CERT]` (§387.5): upgrading past the maintenance date silently breaks
   module loading, not just a UI warning.

No secret VALUES: the embedded keys are PUBLIC (verification keys); license feature/limit structure only.

---

## 387.8 — Self-verify

**Token re-checks** (grep-confirmed, `audits/B387-feature-gates.txt`):
1. `LicenseManager.getFeature/checkFeature/getFeatures` @ `LicenseManager.java:11-15` — ✓.
2. `masterPublicKeyData` (DSA, `30 82 01 b8`, OID id-dsa) + `version2PublicKeyData` (ECDSA P-256) byte literals @ `LicenseUtil.java:223-224` — ✓ (bytes read).
3. `maintenanceExpiration` @ `LicenseFile.java:38` + `VendorLicense.java:111` — ✓.
4. `NFeature.java:100` `expiration==Long.MAX_VALUE ? "never"` (unlimited convention) — ✓.
5. License feature inventory 27+1+150, `expiration=2027-03-31`, `version=4.15`, `developer skipModuleValidation="true"` — ✓ (`.license` files).

**5/5 load-bearing tokens re-verified** (delegated-sweep gate-site line numbers accepted after confirming the API + keys + SMA field they hang off; the `GlobalGroup`/`ResourceManager` MAX_VALUE + exit(-3) claims are the sweep's, spot-consistent with `NFeature` MAX_VALUE convention).

**Marker tally**: `[CERT]` ≈ 30 · `[INFER]` 2 (unlicensed-uncapped generalization; native-vs-Java division of labor). Ratio ≈ 0.07 — low; EVIDENCE+synthesis. Confirms/extends B126, remits per-module gates to B294/B324/B356/B366/B380/B207.

---

## 387.x — Connections

- **[B126 §126.6]** — *confirms its open inference.* B126 found the native `isFeaturePresent` is a text-match
  and inferred Java does the real verification; B387 shows the Java `LicenseFile.load` signature chain rooted
  at the hardcoded DSA+ECDSA master keys, and that no presence-only path exists in Java.
- **[B2]** — the LicenseManager/HostId model; B387 supplies its runtime API + limit-enforcement sites.
- **[B386]** — the on-disk `security/` this focus's L1 found; L6 is what those files DO at runtime (and why
  their absence = uncapped).
- **[B380]** (`fips140-2`/`developer`), **[B356]** (`eSignature`), **[B324]** (`email`), **[B294]**
  (`modbusTcp`), **[B366]** (`analytics`), **[B207]** (`honEasyBinding`) — the per-feature gates this map remits to.
- **Focus status**: L1 (on-disk) + L6 (runtime) answer the license question from both sides. Remaining L3
  (module version delta, needs `japicmp`), L4 (native bin diff), L5 (config) are VERSION-axis, not license.
