# Block 481 — Who watches a license change/tamper, and how licensing gates security posture: node-locked has NO runtime watcher (tamper-evident at load only), subscription has a live entitlement watchdog + operator alarm, there is NO tamper-evident audit trail, and licensing gates FIPS/developer/802.1X

> **Focus:** `licensing` (block B481). **Question (operator):** ¿hay un tercero que vigile un cambio de
> licencia, o que detecte si alguien la cambia/modifica? + cómo la licencia cambia la postura de seguridad.
> Complements [B477]/[B478]/[B479]/[B480].
>
> **Sources:** decompiled corpus (`organized/baja/…/com/tridium/sys/license/`,
> `organized/platform/platform-rt/…/license/BLicensePlatformService.java`, `sources/decompiled/niagarad-ext/`).
> READ-ONLY: no binary executed. `[CERT negative]` = grep ran, zero hits. Markers per §3.

## §481.1 — Node-locked (perpetual): NO runtime watcher — tamper-evident at load only `[CERT]`

- Grep `WatchService|FileWatcher|BFileWatcher|DirectoryWatch|ENTRY_MODIFY` across `sys/license` + `platform/license`
  → **empty** `[CERT negative]`. There is no filesystem watcher on `security/licenses`.
- Read path is boot-only: `NLicenseManager.postInit()`→`load()`→`NodeLockedLicenseManager.loadLicenses()` does a
  one-shot `dir.listFiles()` (`NodeLockedLicenseManager.java:14-29`). `reload()` (`NLicenseManager.java:188`)
  has **no node-locked caller** — its only caller is `SubscriptionLicenseManager.java:310`. Daemon side loads
  once in its `LicenseManager` singleton (`niagarad-ext/…/license/LicenseManager.java:24-34`).
- **So a mid-run on-disk change to a `.license` is NOT detected until restart.**
- **At the next load of a MODIFIED `.license`** (`LicenseFile.load`): sig Base64-decoded (`:83`), stripped,
  XML re-encoded, `LicenseUtil.verify(xml,sig,publicKey[,alg])` (`:170-181`). Tamper → false →
  `error="Invalid signature"` (`:175`) / `"Invalid <alg> signature"` (`:179`) / `"Missing signature element"`
  (`:202`) → `isValid()==false` (`:238-240`) → SEVERE `"License file not loaded - …"` (`:71-73`). Features are
  added only AFTER the sig passes (`:174-183`) → tampered file yields **zero features**. No private key on the
  box → cannot re-sign. **Detection is passive/tamper-evident, not monitored live.**

## §481.2 — Subscription: a live entitlement watchdog `[CERT]`

`SubscriptionLicenseManager` `ScheduledExecutorService` (`:55`), started in `postInit()` (`:141-142`):
- **EntitlementCheck** every `validCheckFreq` (default **6 h**) + 30 min + `random(0..900 s)`
  (`:194-200,68,77,182-184`) → `checkEntitlementPeriodically` (`:256-259`).
- **Clone (.cloned):** on `isInvalidRefreshToken()` → `createClonedFile()` + `deleteLicensesAndCertificatesFile()`
  + `Nre.licenseFailure()`, SEVERE "This instance has been cloned…" (`:291-303,601-621`).
- **Failure limit** `periodicFailureLimit` (default **3**) → `Nre.licenseFailure()` (`:284-286`).
- **KeyRotationCheck** daily poll, rotate at **90 d** (`:170,63-64,217-220`).
- **Listener fan-out:** `updateEntitlementStatus()` calls each `EntitlementStatusListener.entitlementCheckin(status)`
  (`:323-329`).

## §481.3 — Operator-visible alarm (subscription only) `[CERT]`

`BLicensePlatformService implements EntitlementStatusListener` (`BLicensePlatformService.java:54`), registers as
a listener **only on subscription platforms** (`:129-136`). `entitlementCheckin(status)` (`:180-188`): failure →
`makeNewOffnormalAlarm(facets failureCause=status.message)` + `firePlatformServiceAlarmEvent`; success →
`makeToNormal()`. `alarmType="license"` (`:196`); text from lexicon `platform:entitlementCheckinFailure/Success`;
`ackAlarm` action (`:58,87`). **Node-locked/perpetual raises NO entitlement alarm** — the listener is never added.
- **SMA lapse** = a *reminder*, not an alarm: `BSMANotificationSettings` (settings: `enabled`, `expirationReminder`
  default **45 days**, min 30/max 365) (`BSMANotificationSettings.java:44-49`); backed by
  `getLicenseMaintenanceExpiration` + `checkModuleReleaseDate` ("Module … not under active maintenance",
  `NLicenseManager.java:129-150`). A core `BSMAExpirationMonitor` does NOT exist — it lives only in 3rd-party
  modules (httpClient, jsonToolkit) `[CERT negative]`.

## §481.4 — Kill-switch `fatalLicenseFault` `[CERT]`

Field `NLicenseManager.java:32`, checked first in `getFeature`/`checkFeature`/`getFeatures` (`:45-46,60-61,76-77`)
— once set, ALL feature queries throw `LicenseDatabaseException` and `dump()` suppresses features (`:266`). **Only
trigger:** a second `tridium:brand` license → `fatalLicenseFault="Cannot have multiple branded licenses"`
(`addFeature`, `:224-226`). The public `setFatalLicenseFault()` (`:105-107`) has **no caller** `[CERT negative]`.
Distinct, harsher path: `Nre.licenseFailure()` → **`System.exit(-3)`** (`Nre.java:1147-1167`), invoked by the
subscription failures (`SubscriptionLicenseManager.java:269,286,290,300,306`). So: perpetual duplicate-brand →
poisoned manager; subscription failure → process exit.

## §481.5 — NO tamper-evident audit trail `[CERT negative]`

Grep `audit|AuditRecord|AuditHistory|BAuditRecord|logAudit` across `sys/license` + `platform/license` →
**no hits**. There is **no signed/tamper-evident audit event** for a license install or change. Only ordinary
unsigned logging: `"License file not loaded - …"` (`LicenseFile.java:72`), daemon load messages
(`niagarad …/license/LicenseManager.java:61,63,78,130`), subscription INFO (`SubscriptionLicenseManager.java:313,365`).
Ties to [B393]: Niagara signs the license *file* (integrity/authenticity via `LicenseUtil.verify`) but produces
**no record of who changed it or when**. A local operator swapping a validly-signed license leaves only ordinary
log lines. (Consistent with the transversal thesis: Niagara protects "who may run what," not "what happened and
cannot be denied.")

## §481.6 — Security-feature gates (posture) `[CERT]`

- **`fips140-2`** — `Nre.verifyFipsLicense()` (`Nre.java:918-931`, invoked `:750`): `checkFeature("tridium",
  "fips140-2")`; licensed-but-runtime-off → SEVERE banner (`:929`); FIPS running but unlicensed → `fatal()`
  (`:924-927`). Selects the **BouncyCastle-FIPS** provider (`!bin/ext/bcfips`, `SystemFilePaths.java:35,216`);
  `isFips()` steers the security stack. License and runtime must agree.
- **`developer` + `skipModuleValidation`** — `ModuleClassLoader.loadSkipModuleValidation()` (`:543-570`): needs
  sysprop `niagara.classLoader.skipModuleValidation` AND `checkFeature("tridium","developer")` attr
  `skipModuleValidation=true`; active → banner "**Module validation has been DISABLED**" (`:564-566`) — disables
  module JAR integrity validation. `moduleDev` attr → `NModuleDevFilePermission` recursive dev-home read
  (`:28-56`).
- **`smDeveloperMode`** — `Nre.checkSecurityManagerDisable()` (`:966-984`): sysprop
  `niagara.security.manager.disable` + `checkFeature("tridium","smDeveloperMode")` → relaxes/disables the Java
  SecurityManager (exceptions logged, not denied). `developer` also gates NRE watch mode (`:1188-1191`).
- **`ieee8021x`** — `BIEEE8021XPlatformService.serviceStarted()` `getFeature("tridium","ieee8021x").check()`
  (`platIEEE8021X-rt …:141-155`); unlicensed → service does not start. Licenses the 802.1X supplicant.
- **`syslog`** — grep `checkFeature/getFeature("…","syslog")` → **no hits** `[CERT negative]`. Syslog is NOT
  license-gated in this build.

## §481.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Node-locked has NO filesystem watcher; read boot/reload only; reload has no node-locked caller | `[CERT]`/`[CERT neg]` | `NodeLockedLicenseManager.java:14-29`; `NLicenseManager.java:188`; grep empty | PASS |
| 2 | Modified `.license` → verify fails → "Invalid signature" → isValid()==false → zero features | `[CERT]` | `LicenseFile.java:170-202,238-240,71-73` | PASS |
| 3 | Subscription EntitlementCheck 6h + clone `.cloned` + KeyRotation 90d + listener fan-out | `[CERT]` | `SubscriptionLicenseManager.java:141-200,291-329` | PASS |
| 4 | Subscription-only operator alarm `alarmType="license"`; node-locked none | `[CERT]` | `BLicensePlatformService.java:129-196` | PASS |
| 5 | SMA = reminder (45d), not alarm; no core BSMAExpirationMonitor | `[CERT]`/`[CERT neg]` | `BSMANotificationSettings.java:44-49` | PASS |
| 6 | `fatalLicenseFault` only from duplicate brand; poisons all feature checks; setter uncalled | `[CERT]`/`[CERT neg]` | `NLicenseManager.java:32,45-77,224-226,105-107` | PASS |
| 7 | NO tamper-evident audit trail for license change | `[CERT negative]` | grep `audit\|AuditRecord` empty | PASS |
| 8 | fips140-2→BCFIPS; developer/skipModuleValidation/smDeveloperMode relax module+SM enforcement; ieee8021x gates 802.1X; syslog NOT gated | `[CERT]`/`[CERT neg]` | `Nre.java:918-984,1188-1191`; `ModuleClassLoader.java:543-570`; `BIEEE8021XPlatformService.java:141-155` | PASS |

**Tally:** 8 claims, all `[CERT]`/`[CERT negative]`, 0 unmarked.

## §481.8 — Connections & bottom line

- **Answer:** node-locked has **no third-party runtime watcher** — a change is caught only at next load by
  signature verification (tamper-evident, zero features), with **no audit** of who/when. Subscription DOES have a
  live watcher (entitlement scheduler + clone detection) that raises an **operator alarm** and can `System.exit(-3)`.
- **Security posture:** licensing gates FIPS provider, developer/SM relaxations, and 802.1X; `syslog` is not gated.
- Builds on [B477] (station read/verify), [B478] (enforce), [B480] (subscription watchers), [B393] (signs code not
  data → no audit).

## §481.9 — Open gaps

- **B481-G1** the native `SecurityInitializer`/`njre` FIPS provider `addProvider` (outside the vineflower slice) —
  native, deferred (overlaps Segundo's native pass).
