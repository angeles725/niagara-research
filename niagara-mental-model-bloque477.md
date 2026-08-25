# Block 477 — The subscription-licensing layer decompiled from `nre.jar`: two license managers, the Honeywell entitlement server, and the JWT/clone watchdog — plus native RE corroboration of the Host-ID/feature/signature primitives (licensing consolidation capstone)

> **Focus:** `licensing` (document-mode §20 consolidation; deliverable `docs/niagara-licensing.md`). **Question:**
> the licensing thread was dispersed across ~20 blocks with two avoidable `[INFER]` boundaries — the
> subscription/entitlement RUNTIME (living in `bin/ext/nre.jar`, never decompiled) and the native
> Host-ID/feature/signature bodies (decompiled once, never freshly corroborated for this pass). This block
> records the NEW primary findings that closed both; it does not re-derive the consolidated blocks (cited in
> `docs/niagara-licensing.md`).
>
> **Sources (new this pass, registered in `sources/SOURCES.md`):**
> - `sources/decompiled/nre-ext/` — full Vineflower decompile of `bin/ext/nre.jar` (sha256
>   `33aaaac5186e851d6d0773fb8df1ebe4970e18c34312b8e73ea697d15e560415`, 437 `.java`). First corpus decompile
>   of `com.tridium.nre.subscription.*`.
> - `sources/native-corroboration/oem-honeywell-licensing-2026-08-24/` — radare2 `native-static.v1` + Ghidra
>   `ExportDecompiledC` bodies for `njre.dll`/`nre.dll`/`dsfspi.dll`.
> - Java authority layer already in corpus: `organized/baja/baja/vineflower/com/tridium/sys/license/`.
>
> **Secrets discipline:** Host IDs shown as templates (`Win-XXXX-…`), never real values.

## §477.1 — The mode fork: two managers, one property switch `[CERT]`

`NLicenseManager.make()` (`NLicenseManager.java:37-41`) returns `SubscriptionLicenseManager` when
`SubscriptionLicenseUtil.getLicenseMode() == LicenseMode.SUBSCRIPTION`, else `NodeLockedLicenseManager`.

- `LicenseMode { PERPETUAL, SUBSCRIPTION }` — `[CERT]` `nre-ext/com/tridium/nre/util/LicenseMode.java:3-6`.
- The decision (`nre-ext/com/tridium/nre/subscription/SubscriptionLicenseUtil.java:84-96`): reads
  `license.properties` → property `license.subscriptionMode`, OR platform hard-requirement
  `PlatformProvider.requireSubscription()` ("The platform is forcing subscription licensing, overriding
  property settings."); the whole feature is gated by system property
  `niagara.license.subscriptionLicenseAllowed` (`:583-585,598`). `[CERT]`.
- Hierarchy: `javax.baja.license.LicenseManager` (interface) ← `NLicenseManager` (abstract, owns the
  `getFeature`/`checkFeature`/`getFeatures` contract) ← `{NodeLockedLicenseManager,
  SubscriptionLicenseManager}`; only `loadLicenses()` is abstract. `[CERT]` `NLicenseManager.java:26,218`.

## §477.2 — NodeLocked (LOCAL/perpetual): offline, no runtime watcher `[CERT]`

Reads `security/licenses/` + `security/certificates/` at boot (`getPerpetualLicensePath().getParent()`,
`NLicenseManager.java:198-200`), validates 100% offline (§477.5). The class is 58 lines — **no scheduler, no
thread, empty inherited `shutdown()`** — so the license is re-evaluated only on boot / `reload()` /
`rebootLicenseManager()`, never periodically. `[CERT]` `NodeLockedLicenseManager.java` (grep for
`ScheduledExecutorService`/`java.net` → empty).

## §477.3 — Subscription (SERVER): the Honeywell entitlement watchdog `[CERT]`

Separate tree `NiagaraFiles.getSubscriptionPath()` + `/licenses`,`/certificates`,`/.cloned`
(`SubscriptionLicenseManager.java:58-61`). Contacts an entitlement server at runtime and fails closed.

- **Server (this OEM build):** `https://www.niagaracentralapis.honeywell.com` (`:443`), overridable via
  `license.entitlementUrl`. Endpoints (okhttp3 POST): `/ncents/entitlements`, `/ncents/certificates`,
  `/ncents/authn/api_key`, `/ncents/register`. `[CERT]` `nre-ext/…/subscription/{EntitlementApi.java:32,
  EntitlementUtil.java:45-65, RetrieveEntitlements.java:51-53}`. This is a Honeywell host, NOT `*.tridium.com`.
- **Auth:** JWT `ES256`, header `kid=K1`, claims `sub=hostId`, `aud=www.niagaracentralapis.honeywell.com`,
  10-minute expiry; private key from `JwtSignatureKeys`; device public key rotated via `/ncents/authn/api_key`.
  `[CERT]` `EntitlementUtil.java:76-110`, `RotateKeys.java:22-24`.
- **Request token** (`LicenseRefreshToken`): `{nreId=HostId, productId(station|station_<port>|workbench),
  refreshIncrement(monotonic), restoreId, nonce}`. `[CERT]` `RetrieveEntitlements.java:138-145`.
- **Watchers** (`ScheduledExecutorService(1)`): **EntitlementCheck** every `validCheckFreq` (default 6 h) +
  30 min + `random(0..900 s)`; retry limit `validCheckRetry.limit` (default 3) → on
  `periodicCheckFailureCount ≥ limit` calls `Nre.licenseFailure()`. **KeyRotationCheck** daily, rotates JWT
  keys at 90-day age. `[CERT]` `SubscriptionLicenseManager.java:63-70,165-201,266-307`.
- **Clone detection:** server 409 `INVALID_REFRESH_TOKEN` → `createClonedFile()` writes `.cloned` +
  `deleteLicensesAndCertificatesFile()` (wipes `licenses/`+`certificates/`) + `Nre.licenseFailure()`; a later
  successful check clears `.cloned`; remediation `regenerateNreId()` mints a new `Nre-XXXX-…` Host ID.
  `[CERT]` `SubscriptionLicenseManager.java:291-303,601-621`, `SubscriptionLicenseUtil.java:304-379`.
- **Failure action:** `Nre.licenseFailure()` → **`System.exit(-3)`** (hard JVM kill); an interactive
  Workbench launch lacking the `workbench`/`nre` feature `return`s instead. `[CERT]` `Nre.java:1147-1167`.
- **Registration/activation portal** (distinct from runtime): `https://www.niagara-community.com` (Salesforce
  OAuth device-code, hardcoded client id). `[CERT]` `EntitlementUtil.java:47-59`. NOT `axlicensing.tridium.com`
  (that is the Workbench-side `PortalApi`, a different class not present in `nre.jar`).

## §477.4 — §14 correction of [B442]: no `niagarad.license` package exists `[CERT]`

[B442 §442.3] listed `bin/ext/niagarad.jar` as carrying `com.tridium.niagarad.license.{LicenseManager,
LicenseFile,LicenseUtil}`. The full `nre.jar` decompile + inspection of `niagarad.jar` show **no
`com.tridium.niagarad.license.*` package exists anywhere**. The license managers live solely in `baja.jar`
(`com.tridium.sys.license.*`). The daemon's tie to licensing is a **runtime switch, not a code copy**: it sets
`-DNiagaraDaemon=true`, which makes `RetrieveEntitlements.isLicenseValid()` skip in-process signature
validation and delegate to `baja`'s `SubscriptionLicenseManager.isLicenseSignatureValid(...)`. `[CERT]`
`nre-ext/com/tridium/nre/subscription/RetrieveEntitlements.java:246-275`. (The rest of §442.3 — nre.jar
`JarSignatureRegistry`/`CertificateChainValidator`/`CoreTrustStore`, plus baja/file/platform boundaries —
stands.) B442 edited with a pointer.

## §477.5 — Native RE corroboration (fresh Ghidra/r2 this pass) `[CERT]`

Manifests in `sources/native-corroboration/oem-honeywell-licensing-2026-08-24/`; each binary's sha256
verified by the wrapper.

- **`njre.dll NreWin32::getHostId` @ `0x180004ec0`** (refines [B424]'s `~0x180004a70`): 8-byte non-crypto
  XOR/shift fold over four inputs — hidden DPAPI key (`CryptProtectData` is the only CRYPT32 usage, at-rest,
  not a digest), `RegisteredOwner`, cached product-id in `HKLM\SOFTWARE\Niagara4`, and `C:` volume serial via
  `GetVolumeInformationA`. **Zero SHA/MD5/HMAC/BCrypt imports in the function.** Render
  `%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X` → `Win-XXXX-…`; vendor literal `"tridium"`; failure → `exit(0xf9)`.
- **`nre.dll LicenseUtil::isFeaturePresent` @ `0x180001f90`**: lists `\security\licenses`, filters `.license`,
  `strstr` two needles (`<license vendor="%s"`, `<feature name="%s"`) → **no signature-verify call inside the
  function** (`checkFileSignature` exists in the DLL but is not invoked here). Confirms the native fast-path is
  a presence check, not the security boundary — [B126 §126.6]/[B319].
- **`dsfspi.dll`** (`Tridium.Niagara.DsfSpiLib 4.14.0.22`): imports only KERNEL32 + VCRUNTIME (Mocana
  NanoCrypto statically linked); JCE SPI exports `DsfSha1WithDsaSignatureSpi` (license/cert) +
  `DsfSha256WithRsaSignatureSpi` (module). Full Ghidra C body of `DsfUtil::checkFileSignature` @ `0x18002bd40`
  (`dsfspi.checkFileSignature.ghidra.c`, all 5 points CONFIRMED): `nativeInitVerify(keyLen, key)` — the public
  key is passed IN by the caller (no cert/keystore lookup); `fread(buf,1,0x2800,f)` loop = 10 KiB chunks →
  `nativeUpdate`; sidecar `"%s.sig"` (`fopen`), then `DsfShaWithRsaSignature::nativeVerify` → `"file signature
  verification succeeded|failed"`; bounds pathLen 1..255, keyLen ≤500, sig <501 (0x1f5). Confirms
  [B126 §126.1-3].

## §477.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | `make()` picks Subscription vs NodeLocked from `getLicenseMode()`; `LicenseMode{PERPETUAL,SUBSCRIPTION}` | `[CERT]` | `NLicenseManager.java:37-41`; `nre-ext/…/util/LicenseMode.java:3-6` | PASS |
| 2 | Mode decided by `license.subscriptionMode` + `requireSubscription()`, gated by `niagara.license.subscriptionLicenseAllowed` | `[CERT]` | `SubscriptionLicenseUtil.java:84-96,583-598` | PASS |
| 3 | NodeLocked has no runtime scheduler (boot-only re-eval) | `[CERT]` | `NodeLockedLicenseManager.java` (empty grep) | PASS |
| 4 | Entitlement server `niagaracentralapis.honeywell.com` `/ncents/*`, okhttp3 POST | `[CERT]` | `EntitlementApi.java:32`, `EntitlementUtil.java:45-65`, `RetrieveEntitlements.java:51-53` | PASS |
| 5 | Auth = JWT ES256 kid=K1 sub=hostId 10-min | `[CERT]` | `EntitlementUtil.java:76-110` | PASS |
| 6 | EntitlementCheck 6h+30min+rand, retry 3 → `Nre.licenseFailure()`; KeyRotation daily/90d | `[CERT]` | `SubscriptionLicenseManager.java:63-70,165-201,266-307` | PASS |
| 7 | Clone: 409 → `.cloned` + wipe + licenseFailure; `Nre.licenseFailure()`→`System.exit(-3)` | `[CERT]` | `SubscriptionLicenseManager.java:291-303,601-621`; `Nre.java:1147-1167` | PASS |
| 8 | Registration portal `niagara-community.com` (OAuth), NOT `axlicensing.tridium.com` | `[CERT]` | `EntitlementUtil.java:47-59` | PASS |
| 9 | No `com.tridium.niagarad.license.*` package; daemon uses `-DNiagaraDaemon` to skip in-process verify | `[CERT]` | `RetrieveEntitlements.java:246-275` | PASS (corrects B442) |
| 10 | `getHostId` @0x180004ec0 = non-crypto XOR fold of 4 inputs, no SHA/MD5/HMAC | `[CERT]` | `sources/native-corroboration/…/njre.getHostId.ghidra.c` | PASS (refines B424) |
| 11 | `isFeaturePresent` @0x180001f90 = two-strstr text match, no sig verify | `[CERT]` | `sources/native-corroboration/…/nre.isFeaturePresent.ghidra.c` | PASS |
| 12 | `dsfspi` Mocana-static, checkFileSignature streams 10 KiB vs detached `.sig` | `[CERT]` | `sources/native-corroboration/…/dsfspi.native-static.v1.json` | PASS |

**Tally:** 12 claims, 12 `[CERT]`, 0 `[INFER]`, 0 unmarked. Two prior-block refinements (B424 offset, B442 package).

## §477.7 — Connections

- **Consolidated deliverable:** `docs/niagara-licensing.md` (the full end-to-end reference; §0 three
  protections, §1-7 licensing, §8 module protection, §9 posture).
- **Corrects/refines:** [B442] (§442.3 niagarad package — pointer added), [B424] (getHostId offset).
- **Builds on:** [B322]/[B387]/[B395] (Java authority + embedded DSA/ECDSA roots), [B126]/[B319] (native
  fast-path), [B316]/[B443] (live oracle + host-id match), [B392]/[B393] (module signing domains).

## §477.8 — Open gaps (child, not opened here)

- **B477-G1** `niagarad.jar` `com.tridium.niagarad.app.{App,StationApp,EngineWatchdog}` — the daemon that
  ACTS on `Nre.licenseFailure()` (reboot/kill the station). requires-code-read.
- **B477-G2** live §12 validation of the local-vs-subscription oracle (`nre -licenses`) and the Workbench
  License Manager on a live station. requires-execution (operator can open N4).
- **B477-G3** ~~full Ghidra C body of `DsfUtil::checkFileSignature`~~ **CLOSED** in this block (§477.5) —
  `dsfspi.checkFileSignature.ghidra.c` @ `0x18002bd40`, all 5 points CONFIRMED.
