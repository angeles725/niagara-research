# Niagara N4 Licensing & Module-Protection Subsystem — Consolidated Reference (document mode §20)

**Scope.** How Niagara N4 (build N4.14.0.162, Honeywell OEM `OptimizerSupervisor`) licenses features and
protects its modules: what a license *is*, where every input (Host ID, dates, signatures) comes from, the
end-to-end check process, the two license managers (local node-locked vs server subscription), and the three
orthogonal protections applied to modules (confidentiality / integrity / entitlement). This document
**consolidates** an existing, dispersed research thread (≈20 blocks) into one reference; it does not
re-derive closed findings — it cites them — and adds fresh reverse-engineering corroboration produced for
this pass.

**Evidence base.** Corpus blocks (cited as `[Bn]`); decompiled source under
`organized/baja/baja/vineflower/com/tridium/sys/license/` and the newly-decompiled
`sources/decompiled/nre-ext/` (`bin/ext/nre.jar`, sha256 `33aaaac5…`, 437 files, Vineflower); native
Ghidra/radare2 corroboration under `sources/native-corroboration/oem-honeywell-licensing-2026-08-24/`.
Markers per METHODOLOGY §3: `[CERT]` verbatim in local code (file:line), `[CERT-doc]` official Tridium doc,
`[CERT-live]`/`[CERT-hw]` empirical against a live system, `[INFER]` reasoned. **Secrets discipline:** Host
IDs are shown as format templates (`Win-XXXX-…`), never real values; `.license`/`.certificate` files carry
only public keys + signatures by design.

**One-paragraph mental model.** A Niagara license is a **DSA-signed XML record** that binds a **set of named
features (with numeric limits) + date windows** to **one Host ID**. At boot the station loads every
`.license` under `security/licenses/`, verifies each signature against a **hidden master public key baked
into `baja.jar`**, checks the Host ID matches this machine and the dates are in range, and materializes the
valid features into an in-memory map. Everything after that is a map lookup: each driver/service asks
"is my feature present / not expired?" **A license does not add or remove code — it gates code that is
already installed.** Module *code* protection is a **separate** RSA-2048 signing layer (integrity), and
*reading* the code is largely not prevented at all (Java bytecode is decompilable). Three different
mechanisms, three different keys, three different failure modes — the rest of this document separates them.

---

## 0. The three orthogonal protections (do not conflate them)

| Protection | Question it answers | Mechanism | Trust anchor | Algorithm | Failure |
|---|---|---|---|---|---|
| **Confidentiality** | Can the code be *read*? | **None** for core code (plain decompile). OEM widgets only: ZKM/XOR string-obf + AES/XOR-encrypted image *assets* | (asset AES key = license-feature name) | — | reading is not prevented |
| **Integrity / authenticity** | Was the code *recompiled / tampered*? | Detached `<name>.sig` (RSA-2048, 256 B) + standard X.509 JAR signing; `validateCertChain` at add-time & class-load | `security/truststore.jks` + baked-in TPK; build pin `bin/policy/signing.properties` | RSA-2048 / SHA-256 | chain-fail → mode-gated; **required-verify fail → `System.exit(-6)`** |
| **Entitlement** | Are you *licensed* to run it? | `NModule.checkLicensed()` → feature gate + SMA date gate; the license itself is DSA-signed | **hidden master DSA-1024** (+ v2 ECDSA P-256) embedded in `baja.jar` | SHA1withDSA / (SHA256withECDSA v2) | `FeatureNotLicensedException` / zero features; heap over-cap → `System.exit(-3)` |

The rest of the document: §1–§7 the licensing (entitlement) subsystem; §8 module protection (confidentiality
+ integrity); §9 security posture; §10 evidence index.

---

## 1. What a license IS

A `.license` is a signed XML record. `[CERT]` [B126 §126.6], structure verbatim:

```xml
<license vendor="Honeywell" hostId="Win-XXXX-XXXX-XXXX-XXXX"
         generated="2024-06-14" expiration="never" version="4.15"
         maintenanceExpiration="2025-06-14">
  <feature name="nre"      expiration="never"/>
  <feature name="station"  expiration="never"/>
  <feature name="modbusTcp" point.limit="none" device.limit="none"/>
  <feature name="developer" moduleDev="true" skipModuleValidation="true"/>
  ...
  <signature>BASE64( DER SEQ{ INT r(20B), INT s(20B) } )</signature>
</license>
```

- **Signature scheme:** `SHA-1 with DSA`, DSA-1024. The `<signature>` is base64 of DER `SEQ{INT r, INT s}`
  (each 20 bytes). `[CERT]` [B126 §126.1].
- **A license = features + limits + host binding + date windows.** Features are named `(vendor, name)`
  pairs; limits are attributes on each feature (`point.limit`, `device.limit`, `history.limit`,
  `schedule.limit`, `heap.limit`, `port.limit`, …); `"none"` means unlimited. `[CERT]` [B14 §14.2],
  [B387 §387.2].
- **Vendor certificates** (`security/certificates/<Vendor>.certificate`) are the same DSA-1024 XML form; they
  carry the vendor's public key and are themselves signed by the master root (see §4). `[CERT]` [B126 §126.6],
  [B395 §395.1].

---

## 2. The Host ID — where every input comes from

The Host ID is what a license is bound to. Its derivation is **non-cryptographic** and platform-specific.

### 2.1 Windows / supervisor host — `NreWin32::getHostId` (RE-confirmed this pass)

Fresh Ghidra/r2 corroboration of `njre.dll` (sha256 `7007ff82…`), function @ **`0x180004ec0`**
(refines [B424]'s `~0x180004a70`). `[CERT]` `sources/native-corroboration/…/njre.getHostId.ghidra.c`:

Four host inputs, each collected into a 256-byte labelled buffer:
1. **hidden local key file** — `getOrCreateHiddenKey(…,"key",0xff)`; stored at rest under Windows **DPAPI**
   (`CryptProtectData`/`CryptUnprotectData` are the *only* CRYPT32 imports — DPAPI-at-rest, **not** a digest).
2. **registry `RegisteredOwner`** — `getRegWinCurVerImpl("Windows NT","RegisteredOwner")` (falls back to `"Windows"`).
3. **cached product id** — `getOrCreateCachedProductIdKey(…,"product",0xff)` → reads `HKLM\SOFTWARE\Niagara4`.
4. **`C:` volume serial** — `getVolume(…"volume")` → `GetVolumeInformationA("c:\\", …, lpVolumeSerialNumber, …)`.

- **The fold:** each input byte is XOR'd into an 8-byte state with a `shr/xor` feedback (a home-grown
  rolling checksum). **Zero SHA/MD5/HMAC/BCrypt imports in the function** — confirmed against the full import
  table. `[CERT]`. This is the DLL's own exported non-crypto `NreWin32::hash(uchar*,int)`.
- **Render:** `"%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X"` → `Win-XXXX-XXXX-XXXX-XXXX`. `[CERT]`.
- **Vendor:** literal `"tridium"` hardcoded in `.rdata` — even on the Honeywell OEM build the native host
  vendor is `tridium`. `[CERT]` [B424 §424.4].
- **Failure:** every unrecoverable step (`Host Id cannot be found/generated`) → **`exit(0xf9)`** (=249),
  gated on platform property `disableHostIdGeneration != "false"`. `[CERT]`.
- **Security consequence:** the identity carries no integrity primitive of its own — two of the four inputs
  (hidden key, product id) are attacker-writable local files. The DSA signature wraps the Host ID but the
  fold itself is forgeable/collidable if all four inputs are controlled. `[CERT]/[INFER]` [B424 §424.6].

### 2.2 JACE / QNX controller — hardware-bound `Qnx-TITAN-…`

On a JACE-8000 the Host ID is `Qnx-TITAN-XXXX-XXXX-XXXX-XXXX` — **hardware-bound** (board/QNX identifiers),
stable per physical unit, not the Windows fold. `[CERT-live]` [B467], live-confirmed `Qnx-TITAN-44A2-****-****-363E`
[B473]. "N4.0 uses the same host ID as before … provided its operating system remains unchanged."
`[CERT-doc]` `AXtoN4Migration/pSecureNiagara4_LicenseFiles.txt:38-39` [B467 §467.1].

### 2.3 Subscription host — `Nre-…`

In subscription mode the Host ID is a minted `Nre-XXXX-…` UUID, regenerable via `regenerateNreId()` (see §6.3).
`[CERT]` `sources/decompiled/nre-ext/com/tridium/nre/subscription/SubscriptionLicenseUtil.java:178-186,304-379`.

### 2.4 The Java → native bridge

The running Host ID is fetched through:
```
Nre.getHostId()      Nre.java:1294  → NreLib.getHostId()
Sys.getHostId()      Sys.java:116   → NreLib.getHostId()
NreLib.getHostId()   NreLib.java:19-21 → IPlatformProvider.getHostId()   (AccessController.doPrivileged)
   → JNI getHostId0 → NreWin32::getHostId (§2.1)
```
`[CERT]` [manager-lifecycle trace]; JNI leg `[CERT-doc]` [B124/B125].

---

## 3. Dates, the clock, and the SHA — where time and hashing come from

### 3.1 The clock source — the plain OS wall clock, no trusted time

Every date gate uses the **unauthenticated OS clock**. `[CERT]`:
- `LicenseFile.load()` → `long now = System.currentTimeMillis();` (`LicenseFile.java:93`).
- Feature expiry → `javax.baja.sys.Clock.millis()`, a thin wrapper: `return System.currentTimeMillis();`
  (`Clock.java:26-27`).
- **There is no NTP/monotonic/attested time source anywhere in the license path.** Rolling the OS clock is
  the entire attack surface against the date gates; the only counters are the 2015 floor and the 36 h grace
  (both themselves clock-relative). `[CERT]` — stated as posture.

### 3.2 The SHA — where hashing enters

Two distinct places, both handled by the Mocana-backed provider `dsfspi.dll`
(`Tridium.Niagara.DsfSpiLib 4.14.0.22`, Mocana NanoCrypto statically linked — RE-confirmed this pass,
sha256 `82e8c7f0…`) `[CERT]` `sources/native-corroboration/…/dsfspi.native-static.v1.json`:
- **License/cert signature:** `SHA-1 with DSA` — the XML is canonicalized (`LicenseUtil.encode`) and the SHA-1
  digest is DSA-verified against the vendor public key. `[CERT]` [B126 §126.2], [B395 §395.1].
- **Module/file signature:** `SHA-with-RSA` — `DsfUtil::checkFileSignature` streams the file in **10 KiB
  (`0x2800`) chunks** through SHA, then RSA-verifies against the detached `"%s.sig"` sidecar. RE-confirmed:
  `fread` size `0x2800`, sidecar path `"%s.sig"`, log strings `verifying file signature` /
  `file signature verification succeeded|failed`. `[CERT]` [B126 §126.3] + this pass's r2 disasm.

### 3.3 Every date field and its check (ordered, inside `LicenseFile.load`)

`[CERT]` `com/tridium/sys/license/LicenseFile.java`; all dates parsed by `LicenseUtil.parseDate(str, startOfDay)`
(`yyyy-MM-dd`; `"never"` → `Long.MAX_VALUE`):

| Order | Field / check | Code | Message on failure |
|---|---|---|---|
| 1 | Host ID present | `:41` | `HostId not supported` |
| 2 | `generated` present & parses (start-of-day) | `:96-102` | `Missing/Invalid license generated date` |
| 3 | **Clock floor 2015-01-01** (`now < parseDate("2015-01-01")`) | `:106` | `Current system time appears invalid, date before 2015-01-01` |
| 4 | **36 h backdating grace** (`now < generated − 129600000L`) | `:110` (`MILLIS_IN_36_HOURS`) | `Current date is earlier than license generated date` |
| 5 | `expiration` present & parses (end-of-day 23:59:59) | `:116-122` | `Missing/Invalid license expiration date` |
| 6 | **License expiry** (`now > expiration`) | `:126` | `License file is expired` |
| 7 | `version` / `maintenance` (tridium) | `:139-168` | `License for older version…` / `…maintenance not active` |

- **`INVALID_LICENSE_TIME_MILLIS_FLOOR = 1420070400000L`** (2015-01-01) — the named constant
  (`LicenseUtil.java:27`); `load` recomputes it inline. `[CERT]`.
- **Feature-level expiry** is separate and **strict** (no grace): `NFeature.check()` →
  `if (expiration < Clock.millis()) throw FeatureLicenseExpiredException` (`NFeature.java:41-45`). `[CERT]`.
- **There is only ONE grace window: the 36 h backdating tolerance.** Expiry has no 24–48 h grace; the only
  "slack" is that `expiration` is parsed to end-of-day. `[CERT]` — absence confirmed across
  `LicenseFile`/`NFeature`/`NLicenseManager`.
- **SMA (`maintenanceExpiration`)** is a *module-build-date* gate, not a clock gate:
  `NLicenseManager.checkModuleReleaseDate()` throws `LicenseDatabaseException("Module … not under active
  maintenance")` when `module.getReleaseDate() > maintenanceExpiration` at class-load. `[CERT]` [B387 §387.5].

---

## 4. The trust anchor and signature verification (who signs, who verifies)

- **The real root is hidden inside `baja.jar`, not on disk.** `LicenseUtil` embeds `masterPublicKeyData` =
  a **444-byte DER DSA-1024** key (sha256 `aed58673…`, OID id-dsa `1.2.840.10040.4.1`, Tridium 2003 root
  family). The 4.14 build also embeds `version2PublicKeyData` = **ECDSA P-256** (91 B, `7d766e9c…`), selected
  only when a record declares `version="2.0"`; all shipped records are `version="1.0"` → DSA. `[CERT]`
  [B387 §387.6], [B395 §395.3]. (The OEM 4.10.9.14 build has DSA only — single-root delta, [B322 §322.2].)
- **The chain:** embedded master DSA-1024 (hidden in every N4) → signs each vendor `.certificate`
  (Tridium, Honeywell, HoneywellCentraLine) → each vendor key signs its `*.license`. The on-disk
  `Tridium.certificate` is a **signed leaf**, NOT the root (corrects [B392 §392.4]). `[CERT]` [B395 §395.2].
- **Who verifies (the Java authority):** `CertificateFile.load()` verifies each vendor cert against the
  embedded master key (`CertificateFile.java:74` → `LicenseUtil.getMasterPublicKey()`); `LicenseFile.load()`
  strips `<signature>`, canonicalizes (`LicenseUtil.encode`), and `LicenseUtil.verify(xml, sig, cert.publicKey)`
  (SHA1withDSA). Bad/missing signature → `"Invalid signature"` / `"Missing signature element"` →
  `isValid()==false` → **zero features loaded**. `[CERT]` [B387 §387.6], [B322 §322.1].
- **The native fast-path is NOT the security boundary.** `nre.dll LicenseUtil::isFeaturePresent`
  (@ `0x180001f90`, RE-confirmed this pass) lists `\security\licenses`, filters `.license`, and **text-matches**
  two needles (`<license vendor="%s"` + `<feature name="%s"`) with `strstr` — **no signature verification
  inside the function** (`checkFileSignature` exists in the DLL but is not called from here). It is a presence
  fast-path the native launcher trusts for gates like `-javaagent`/`fips140-2`; the Java `LicenseManager`
  remains the real authority. `[CERT]` [B126 §126.6], [B319], + this pass's Ghidra body.

---

## 5. The end-to-end license-check PROCESS (boot → runtime)

### 5.1 Boot

`[CERT]` `com/tridium/sys/Nre.java` + `NLicenseManager.java`:
```
Nre.boot()
  :716  licenseManager = NLicenseManager.make()      // pick manager (see §6)
  :740  licenseManager.postInit()                    // → load()
          NLicenseManager.load()  (:192-196)
            loadCertificates()    // security/certificates/*.certificate
            loadLicenses()        // security/licenses/*.license  (node-locked)
          registers the "licenseManager" Spy page
  :750  verifyFipsLicense()  → checkFeature("tridium","fips140-2")
```
The license is fully loaded **before** modules/engine/services init, so every downstream consumer can gate
on it. `[CERT]`.

### 5.2 Per-license validation (`LicenseFile.load`, all-or-nothing)

Ordered: Host ID present → root element → vendor certificate resolves & is valid → `<signature>` present →
**Host ID match** (§6) → date checks (§3.3) → version/maintenance → **signature verify** → register features.
First failure sets `this.error`, discards the whole file, logs SEVERE `License file not loaded - …{invalid: <error>}`.
`[CERT]` `LicenseFile.java:77-205`, [B443 stage-walk].

### 5.3 Runtime

Pure in-memory map lookups: `NLicenseManager.getFeature(vendor,name)` returns the `NFeature` (no expiry
enforce); `checkFeature(vendor,name)` additionally calls `feature.check()` and throws
`FeatureLicenseExpiredException` if expired. Both resolve through `Sys.getLicenseManager()`. `[CERT]`
`NLicenseManager.java:43-72`, `NFeature.java:41-45`.

---

## 6. Host ID ↔ license match, and LOCAL vs SERVER

### 6.1 The Host-ID match — exact string compare

`[CERT]` the comparison is a one-liner in the concrete subclass:
```java
// NodeLockedLicenseManager.java:54-56  (subscription twin: SubscriptionLicenseManager.java:667)
protected boolean isLicenseHostIdValid() { return this.hostId.equals(Nre.getHostId()); }
```
- **Exact, case-sensitive `String.equals`** — no normalization (contrast the 3rd-party Reflow reimpl, which
  upper-cases, §7). `this.hostId` = the license's `hostId` XML attribute (`LicenseFile.java:84`).
- **Mismatch string:** `"HostId does not match"` (`LicenseFile.java:87`) → file marked invalid, **no features**.
- **Foreign-host file relocation:** `LicenseDatabase` normalization copies a license whose `hostId ≠ Sys.getHostId()`
  into `security/licenses/db/<claimedHostId>/` and **deletes the original** from `licenses/`, logging `moved …`
  — so it is filed under the foreign host's directory and never loaded as an active feature. `[CERT]`
  `LicenseDatabase.java:544-570`, [B316 §89-101 live].

### 6.2 The fork — two managers, one switch

`[CERT]` `NLicenseManager.make()` (`NLicenseManager.java:37-41`):
```java
return SubscriptionLicenseUtil.getLicenseMode() == LicenseMode.SUBSCRIPTION
     ? new SubscriptionLicenseManager()   // SERVER
     : new NodeLockedLicenseManager();     // LOCAL (default)
```
- **The mode enum** `LicenseMode { PERPETUAL, SUBSCRIPTION }` (`nre-ext/…/util/LicenseMode.java:3-6`). `[CERT]`.
- **The decision** (`SubscriptionLicenseUtil.getLicenseMode()`, `nre-ext/…/subscription/SubscriptionLicenseUtil.java:84-96`):
  reads `license.properties` → property **`license.subscriptionMode`**, OR the platform hard-requirement
  `PlatformProvider.requireSubscription()` ("The platform is forcing subscription licensing…"); gated overall
  by system property **`niagara.license.subscriptionLicenseAllowed`**. `[CERT]`.

Class hierarchy: `javax.baja.license.LicenseManager` (interface) ← `NLicenseManager` (abstract, implements the
3-method contract `getFeature`/`checkFeature`/`getFeatures`) ← `{NodeLockedLicenseManager,
SubscriptionLicenseManager}`. The only truly abstract method is `loadLicenses()`. `[CERT]`
`NLicenseManager.java:26,218`, `NodeLockedLicenseManager.java:12`, `SubscriptionLicenseManager.java:54`.

### 6.3 LOCAL — `NodeLockedLicenseManager` (perpetual, offline, NO runtime watcher)

- **Comes from:** default branch of `make()`. **Source dir:** `security/licenses/` +
  `security/certificates/` (`getPerpetualLicensePath().getParent()`). `[CERT]` `NLicenseManager.java:198-200`.
- **Validation:** 100% offline (§5.2). **Zero network imports** in `NodeLockedLicenseManager`/`LicenseFile`/
  `LicenseUtil`. `[CERT]` (verified empty grep).
- **Who watches it:** **nobody.** The class is 58 lines — no scheduler, no thread, empty inherited `shutdown()`.
  The license is evaluated **only at boot** (or explicit `reload()`/`rebootLicenseManager()`); a re-check
  happens only on restart. `[CERT]` `NodeLockedLicenseManager.java`. This is the classic JACE / Supervisor /
  Workstation model.

### 6.4 SERVER — `SubscriptionLicenseManager` (phones home, fails closed)

- **Comes from:** `make()` when mode is SUBSCRIPTION. **Source dir:** a *separate* tree
  `NiagaraFiles.getSubscriptionPath()` + `/licenses`, `/certificates`, `/.cloned` — NOT `security/licenses/`.
  `[CERT]` `SubscriptionLicenseManager.java:58-61,456-459`.
- **The entitlement server (this OEM build):** `https://www.niagaracentralapis.honeywell.com` (`:443`),
  overridable via `license.entitlementUrl`. Endpoints (okhttp3 POST): `/ncents/entitlements` (check),
  `/ncents/certificates`, `/ncents/authn/api_key` (key rotation), `/ncents/register`. `[CERT]`
  `nre-ext/…/subscription/{EntitlementApi.java:32, EntitlementUtil.java:45-65, RetrieveEntitlements.java:51-53}`.
  NOT a `*.tridium.com` host in this build.
- **Auth:** signed **JWT ES256**, header `kid=K1`, claims `sub=hostId`, `aud=www.niagaracentralapis.honeywell.com`,
  10-minute expiry, private key from `JwtSignatureKeys`; device public key registered/rotated via
  `/ncents/authn/api_key`. `[CERT]` `EntitlementUtil.java:76-110`, `RotateKeys.java:22-24`.
- **Request payload** (`LicenseRefreshToken`): `{nreId=HostId, productId(station|station_<port>|workbench),
  refreshIncrement(monotonic), restoreId, nonce}`. `[CERT]` `RetrieveEntitlements.java:138-145`,
  `LicenseRefreshToken.java:4-38`.
- **Who watches it — two schedulers** (`ScheduledExecutorService(1)`), `[CERT]` `SubscriptionLicenseManager.java`:
  - **EntitlementCheck** — every `validCheckFreq` (default **6 h**, `Duration.ofHours(6)`) + 30 min +
    `random(0..900 s)`; initial delay `freq + 30 min + rand`. Inner per-call retry window 5 min with
    exponential backoff.
  - **Retry/failure:** `validCheckRetry.limit` (default **3**). On `periodicCheckFailureCount ≥ limit` →
    `Nre.licenseFailure()`.
  - **KeyRotationCheck** — daily poll (`Duration.ofDays(1)`), rotates when JWT keys reach **90-day** age
    (`Period.ofDays(90)`).
- **Clone detection:** server returns HTTP 409 `INVALID_REFRESH_TOKEN` (a stale/duplicate `refreshIncrement`
  from a copied station) → `createClonedFile()` writes `.cloned` (timestamp) + `deleteLicensesAndCertificatesFile()`
  (recursively wipes `licenses/` + `certificates/`) + `Nre.licenseFailure()`, log "This instance has been
  cloned. Please regenerate Host Id and register." A later *successful* check clears `.cloned`. Operator
  remediation = `regenerateNreId()` (new `Nre-XXXX-…` Host ID). `[CERT]` `SubscriptionLicenseManager.java:291-303,601-621`.
- **The failure action — `Nre.licenseFailure()`** → **`System.exit(-3)`** (hard JVM kill), `[CERT]`
  `Nre.java:1147-1167`. Special case: an interactive Workbench launch lacking the `workbench`/`nre` feature
  `return`s instead of exiting (`:1157-1160`); under unit test it is a no-op.

### 6.5 ACTIVATION vs RUNTIME (keep them separate)

- **A running node-locked station NEVER contacts a server** — runtime is always local-file. `[CERT]` §6.3.
- **A running subscription station DOES phone home** (~6 h, ≥daily per doc) and fails closed. `[CERT]`/`[CERT-doc]`
  §6.4; `Container/Licensing-3A8F48E0.html` ("reach out … at least once a day … to continue running").
- **Activation** (getting a license onto a box) is a distinct, one-time, provisioning-side online step:
  - Perpetual: Workbench "Request License" / Commissioning Wizard pulls from the portal
    `https://axlicensing.tridium.com` (`PortalApi`, Workbench-side) keyed by Host ID and **writes local
    `.license` + `.certificate`** into `security/`. `[CERT]` `LicenseDownload.java:30-70`, `[CERT-doc]` J9Startup.
    Offline alternative: drop a `.lar`/`.license` into `security/licenses/inbox/`. `[CERT-doc]`
    `ControllerLicensingWithoutAnInterne-3484104F.html`.
  - Subscription device registration: `https://www.niagara-community.com` (Salesforce OAuth device-code flow,
    hardcoded client id). `[CERT]` `EntitlementUtil.java:47-59`.

### 6.6 The read oracle & the on-disk layout

- **`nre -licenses`** = `NLicenseManager.dump()` — HostId + certs + `Licenses (perpetual|subscription)` +
  materialized Features. The **authoritative** read-only signal. `[CERT]` `NLicenseManager.java:235-293`,
  `[CERT-hw]` [B442 §442.1] (unlicensed → `Licenses (node-locked): none`, `Features: none`).
- **On disk:** `security/licenses/*.license` (active), `…/db/<HostId>/` (per-host DB, `add()` keeps newest
  `generated`; root copy is authoritative, `db/` is the host-keyed store), `…/inbox/` (import drop-zone),
  `security/certificates/*.certificate` (vendor chain), `security/truststore.jks` (module trust — a *separate*
  domain). `[CERT]` `LicenseDatabase.java`, [B386 §386.3].
- **Correction [B386 → B442]:** the presence of a `security/` tree does **NOT** prove licensing — an
  unlicensed 4.10 host retains a baseline `security/` with a valid `Tridium.certificate` and empty
  `licenses/{db,inbox}`. License state = **validated `.license` records + loaded features**, read via
  `nre -licenses`. `[CERT-hw]` [B442 §442.2].
- **Modules are NOT added/removed by licensing** — the module set is a vendor/version/user axis; a license is
  a runtime feature-gate over whatever is installed. `[CERT]` [B388 §388.3].

---

## 7. Feature gates, limits, and per-module consumers ("quién lo exige")

- **The idiom:** each driver/service declares `getLicenseFeature()` → `Sys.getLicenseManager().getFeature(vendor,name)`
  and faults itself if absent — **distributed per-module gates, no central switch.** `checkFeature`/
  `getLicenseFeature` appears in **577 files**. `[CERT]` [manager-lifecycle trace], [B387 §387.3].
- **Unlicensed = UNCAPPED, not disabled:** if the `globalCapacity` feature is absent, all limits → `Integer.MAX_VALUE`
  (`GlobalGroup.java:24-48`). Licensing *adds* caps. `[CERT]` [B387 §387.4].
- **Exceed behavior:** heap over-cap → `ResourceManager.checkLicense()` prints `STATION IS UNLICENSED!!! …` →
  **`System.exit(-3)`**; over a component cap → that component faults (`fatalFault`). `[CERT]` [B387 §387.4].
- **Three-layer gate** (generalized from jsonToolkit, [B335 §335.3]): (1) feature present → else service
  unreachable; (2) per-operation boolean attribute (`feature.getb("import"/"export", false)`); (3) SMA
  (`getLicenseMaintenanceExpiration(vendor)` in the past → `smaExpired`, unless `sma.exempt`). Most modules use
  the degenerate one-layer form.

**Concrete per-module feature strings (verbatim):**

| Module | Feature `(vendor, name)` | Citation |
|---|---|---|
| nre / station / workbench / developer / fips140-2 | `("tridium", …)` | `Nre.java:497,920,973,1189`; `Station.java:216` |
| email | `("tridium","email")` | `BEmailService.java:111-112` [B324] |
| jsonToolkit | `("tridium","jsonToolkit")` + `import`/`export`/`sma.exempt` | `LicenseLimit.java:14-46` [B335] |
| Modbus (×4) | `modbusAsync`, `modbusTcp`, `modbusSlave`, `modbusTcpSlave` | [B301] |
| electronicSignature | `tridium:eSignature` (+ `point.limit="500"`) | [B352]; [B387 §387.3] |
| bacnet / mstp / bacnetSc | `("tridium", …)` | `BBacnetNetwork.java:339-344` |
| globalCapacity (heap/point/device/…) | `("tridium","globalCapacity")` | `ResourceManager.java:82`, `GlobalGroup.java:24` |
| easyBinding (OEM) | `honEasyBinding` (Honeywell; multi-vendor fallback) | `EbLicenseUtil.java:22` [B207] |
| galileoKitPx (OEM) | asset XOR key from `"honEasyBinding"`; PIN bindings consult NO feature | [B423] |

**Third-party OEM as a product (Reflow, [B139]/[B232]):** a *dual* model — native Niagara license
(`checkFeature(vendor,"reflow")`) OR proprietary `^niagaramods.license` (RSA `SHA256withRSA`, public key
inside the module, host binding **upper-cased**, zero-touch fetch `GET http://api.niagaramodules.com/license/<hostId>`
over plain HTTP). Any validation failure **fail-opens** to a trial tier (not fail-closed). Tiers keyed by
station-type (jace/supervisor/site/enterprise). `[CERT]` [B139 §139.1-6], [B232].

---

## 8. Module protection — reading (confidentiality) and recompiling (integrity)

### 8.1 Confidentiality — core bytecode is NOT protected

Niagara module bytecode is **not encrypted at rest**; a `.jar` is a standard ZIP and the whole corpus is
built on ordinary decompilation. `[CERT]` [B392 §392.2]. The only confidentiality add-ons are narrow and
**OEM-widget-only, never over core code**:
- **String obfuscation** on specific Honeywell OEM `-wb` widgets: `galileoKitPx` uses **ZKM (Zelix
  KlassMaster)** (141 encrypted strings, [B423 §423.2]); `easyBinding` uses a **custom `z[]` XOR** decoded at
  runtime ([B207]).
- **Encrypted image ASSETS (not code):** `easyBinding` AES-encrypts widget images with a key derived from the
  license-feature name (`EncryptDecrypt.java:44`); `galileoKitPx` XOR-encrypts images keyed on the literal
  `"honEasyBinding"` (`KitpxUtils.java:37`). Assets decrypt only when the license is active. `[CERT]` [B207],
  [B423 §423.3].

**Bottom line:** you can read (decompile) essentially all Niagara code; confidentiality is effectively nil for
the platform, and only a few OEM widget modules hide strings + encrypt image assets.

### 8.2 Integrity / anti-recompile — detached RSA-2048 signatures verified at load

This is what stops a decompiled-and-recompiled module from loading.
- **Scheme:** module JARs carry a detached `<name>.sig` = **exactly 256 raw bytes** (RSA-2048, no
  PKCS#7/DER wrapper), produced only by Tridium's `niagara-signing-plugin` (standard `jarsigner` is
  incompatible). Additionally the class manifest is signed via standard `java.util.jar` X.509
  (`META-INF/NIAGARA4.{SF,RSA}`). `[CERT]` [B126 §126.1], [B18 §18.1.3], [B392 §392.2].
- **Trust anchor:** `security/truststore.jks` (JKS, integrity password = Java default `changeit`) + a baked-in
  TPK; build-side pin `bin/policy/signing.properties` (issuer/subject DN + serial matching the artifact
  PKCS#7 byte-for-byte). `[CERT]` [B392 §392.3].
- **The real chain of `baja.jar`:** `Niagara4Modules Code Signing` (RSA-2048) → `Honeywell CodeSign RSA CA`
  (RSA-4096) → `Honeywell Product PKI RSA` (self-signed root). In this OEM build **even core Tridium modules
  are re-signed by Honeywell's PKI.** `[CERT]` [B392 §392.2].
- **Who verifies, when:** add-time `ModuleManager.verifyModuleSignature` (`:330-404` →
  `validateCertChain`) and class-load `ModuleClassLoader.verifyJarEntrySignature` (`:374-528`, before
  `defineClass`); verifier classes `JarSignatureRegistry`, `CertificateChainValidator`, `CoreTrustStore` in
  `nre.jar`; native `dsfspi.dll DsfUtil::checkFileSignature` + `nverify.exe`. `[CERT]` [B392 §392.6], [B442 §442.3].
- **Failure when a module is recompiled/tampered:**
  1. **Invalid signature on a required-verification module → `System.exit(-6)`** (the whole station process
     dies, not just the class). `[CERT]` [B392 §392.6] `ModuleClassLoader.java:520-522`.
  2. **Cert-chain that doesn't reach a trusted anchor → `CERT_PATH_VALIDATION_FAILURE`**, then
     accept/reject decided by `moduleVerificationMode` (see §9). `INVALID_SIGNATURE` is rejected in **every**
     mode. `[CERT]` [B113 §113.2.3].
- **Conditional universality:** "any N4 accepts these modules" is true only because the signer chains to a
  root in *that install's* trust anchor. An OEM replaces the anchor with its own root and re-signs even core
  modules; a Honeywell-signed module would not validate on a stock-Tridium N4 (different root) and vice-versa.
  `[INFER]` [B392 §392.7] (corrects [B113 §113.5]).

### 8.3 The module load process — integrity and entitlement are SEPARATE steps

`[CERT]` ordered at class-load:
1. Open JAR (`ModuleFile extends JarFile`, `verify=true`).
2. `requiresSignature` = module requests any of ACCESS_CLASS / REFLECTION / MBEAN_PERMISSION →
   `validateCertChain = requiresSignature && !SKIP_MODULE_VALIDATION`.
3. **INTEGRITY:** `verifyJarEntrySignature` → `validateCertChain` (RSA); required-verify fail → `System.exit(-6)`.
4. **ENTITLEMENT:** `NModule.checkLicensed()` → per-module feature gate (`getFeature`) + SMA build-date gate
   (`checkModuleReleaseDate`).
5. License authenticity underpinning step 4 = the DSA path of §4.
6. Capacity enforcement (§7): unlicensed = uncapped; heap over-cap → `System.exit(-3)`.

They use **different algorithms** (RSA-2048 vs DSA-1024), **different anchors** (`truststore.jks`/TPK vs
embedded master DSA in `baja.jar`), and **fail differently** (`System.exit(-6)`/chain-fail vs
`FeatureNotLicensedException`/zero-features/`System.exit(-3)`).

---

## 9. Security posture (factual, with citations — not a how-to)

The off-switches and weaknesses the corpus recorded, stated as posture:

| Item | What it controls | Live-observed (B398) |
|---|---|---|
| `moduleVerificationMode` (`low`/`medium`/`high`) | accept/reject on signature status; `low` fail-opens unsigned/chain-fail/self-signed; `INVALID_SIGNATURE` rejected in all modes | **`low`** in `defaults/system.properties` (SEC-01 CRIT) `[CERT-live]` |
| `developer` feature + `skipModuleValidation="true"` (+ sysprop) | disables cert-chain validation for `requiresSignature` modules; loud banner `**** Module validation has been DISABLED ****` | `Webs.license` carries `developer{skipModuleValidation=true}` + `smDeveloperMode` `[CERT-live]` [B18] |
| `commandLinePropertyBlacklist` gap | `moduleVerificationMode`/`program.requireSigning` protected from CLI override, but `classLoader.skipModuleValidation` / `commissioning.ignoreVerificationMode` are NOT | skip levers absent from blacklist live (SEC-05) `[CERT-live]` |
| `truststore.jks` password | JKS integrity MAC | opens with default `changeit` `[CERT-live]` |
| OS clock | every license date gate (§3.1) | unauthenticated wall clock — clock-roll is the date attack surface `[CERT]` |
| Host ID fold | license binding input (§2.1) | non-crypto; 2 of 4 inputs are attacker-writable local files `[CERT]` |
| Native fast-path | `-javaagent`/`fips140-2` launcher gates | signature-less `strstr` text-match (§4) — evadable, Java layer independently rejects `[CERT]` [B319] |

**The transversal thesis of the signing/licensing thread:** Niagara cryptographically protects *"who may run
what"* (modules / dist / firmware / licenses, with roots hidden in `baja.jar`) far more strongly than
*"what happened and cannot be denied"* (audit/history/backup/`.bog` carry no signature). And the closer to
physical I/O, the weaker the integrity. `[CERT]` [B392], [B393], [B396]. `SignedDistFilter` verifies only
install-distribution parts, not station data. `[CERT]` [B393 §393.3].

---

## 10. Corrections this pass produced (§14) and evidence index

**Corrections:**
- **[B442] CONFIRMED (an earlier "correction" here was RETRACTED — see [B478]):** a first-hand decompile of
  `bin/ext/niagarad.jar` (`sources/decompiled/niagarad-ext/`, sha256 `8d295b6d…`) confirms
  `com.tridium.niagarad.license.*` **DOES exist** — 5 classes (`Brand, Feature, LicenseFile, LicenseManager,
  LicenseUtil`), a **platform-feature** license manager with a fixed `FEATURE_WHITELIST` (`jre8qnx, qnx7,
  globalCapacity, fips140-2, station, brand, syslog, smDeveloperMode, ieee8021x`; `LicenseManager.java:201-215`).
  So B442 §442.3 was right; my intermediate claim that the package didn't exist was a wrong unverified negative
  (it came from an agent that decompiled only `nre.jar`). The TRUE sub-fact: the daemon sets
  `-DNiagaraDaemon=true` (`NiagaraDaemon.java:201`), read station-side by `RetrieveEntitlements` to **skip
  in-process module-signature validation** and delegate that to `baja`'s `SubscriptionLicenseManager` `[CERT]`
  `nre-ext/…/subscription/RetrieveEntitlements.java:246-275`.
- **[B424] → refined:** `getHostId` body is @ `0x180004ec0` (not `~0x180004a70`); product-id cache reads
  `HKLM\SOFTWARE\Niagara4`; CRYPT32 usage is DPAPI-at-rest only. `[CERT]` this pass.
- **Entitlement host (OEM build):** `niagaracentralapis.honeywell.com` (runtime) + `niagara-community.com`
  (registration), NOT `axlicensing.tridium.com` (that is the Workbench portal). `[CERT]` this pass.

**Fresh artifacts (registered in `sources/SOURCES.md`):**
- `sources/native-corroboration/oem-honeywell-licensing-2026-08-24/` — `njre|nre|dsfspi.native-static.v1.json`
  (radare2), `njre.getHostId.ghidra.c`, `nre.isFeaturePresent.ghidra.c` (Ghidra bodies).
- `sources/decompiled/nre-ext/` — full Vineflower decompile of `bin/ext/nre.jar` (sha256 `33aaaac5…`, 437 files),
  first corpus decompile of the subscription/entitlement layer.

**Open follow-ups (child gaps, not opened here):**
- `niagarad.jar` `com.tridium.niagarad.app.{App,StationApp,EngineWatchdog}` — the daemon that acts on
  `Nre.licenseFailure()` and can reboot/kill the station.
- Full Ghidra C body of `DsfUtil::checkFileSignature` in `dsfspi.dll` (deferred; corroborated via r2 this pass).
- Live §12 validation of the local-vs-subscription oracle and the Workbench License Manager (operator can open
  a live station — see the runbook note below).

**Blocks consolidated:** B2, B14, B18, B75, B113, B126, B139, B207, B232, B301, B316, B319, B322, B324, B335,
B352, B379, B386, B387, B388, B389, B392, B393, B395, B396, B398, B423, B424, B442, B443, B467.
