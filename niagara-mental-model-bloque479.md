# Block 479 — Platform-side license delivery/install/sync: the Workbench License Manager pushes plain license files over the daemon TLS session, and the platform install path performs NO cryptographic verification — trust is deferred to the station (closes B479)

> **Focus:** `licensing` (gap B479). **Question:** who DELIVERS / INSTALLS / ADMINISTERS a license at the
> **platform** layer, and what does the platform validate on install? Complements [B477] (station reads
> licenses) and [B478] (`niagarad` enforces exit codes + platform-feature gate). Answers the operator question
> "cómo llega la licencia a la máquina y quién la administra".
>
> **Sources:** decompiled corpus already registered — `organized/platDaemon/platDaemon-wb/…/ui/licenseinstall/`,
> `organized/platform/platform-rt/…/license/`, `organized/baja/…/com/tridium/sys/license/dom/LicenseDatabase.java`,
> `sources/decompiled/niagarad-ext/…/{servlet,license}/`. READ-ONLY: no binary executed, no launcher/daemon run.
> No new artifacts (no SOURCES additions). Markers per §3.

## §479.1 — The administrator: `BLicenseManager` (WB Platform → License Manager) `[CERT]`

A **DaemonSession view**, not a station view: `@AgentOn(types={"platform:DaemonSession"})`, `extends
BDaemonSessionView`, holds a `BDaemonSession` (`BLicenseManager.java:213-218,344`). It runs against the
platform daemon connection. Operations: import license (`importFile()` :836), export `.lar`
(`ExportLicenseCommand`, default `"licenses.lar"` :1030-1036), delete license/cert (:238-320), change brand,
**register / regenerate Host ID** (`RegisterCommand` :1059, `RegenerateCommand` :1114), subscription
get-entitlement. Import/delete/brand disabled when `isLicenseReadonly()` (:374). It reads the Supervisor's
local DB via `LicenseDatabase.LOCAL_INSTANCE.getLicenses(hostId)` (:420,796).

## §479.2 — Install = a FILE TRANSFER over the daemon session (not a crypto op) `[CERT]`

`BLicenseManager.sync()` builds a `FileTransferMessage`; per add/update calls
`PortalLicenseUtil.addFile(message, fileInfo, isPerpetual, isNiagaraReadonly)`, per remove `message.addDelete`
(`BLicenseManager.java:446-491`), then `DaemonFileUtil.transfer(session, message, …)` (:504), then sends
`ReloadLicenseMessage` + `BRemoteDaemonPlatform.make(session,null).requestReload()` (:524-525). Target path =
`LicenseInfo.getInstalledFilePath` → `SystemFilePaths.getLicensesDirPath(...)` resolving to **`!security/licenses`**
(perpetual, writable home), **`~security/licenses`** (read-only home), or **`security/subscription/licenses`**
(subscription) (`LicenseInfo.java:213-279`; `SystemFilePaths.java:123-135`).

## §479.3 — The `.lar` license archive `[CERT]`

`BLicenseArchiveFile` = a `BDataFile` `@FileExt(name="lar")` — a **ZIP** whose entries are
`licenses/<hostId>/<name>.license` (each a `VendorLicense` XML), MIME `application/x-baja-license-archive`
(`BLicenseArchiveFile.java:18-91`). Imported by `LicenseDatabase.importFile()`: `.lar` → `importLicenses()`
(unzip, `add(VendorLicense.make("lar",zipIn,false))` per `*.license`); `.license` → `add(VendorLicense.make(file))`
(`LicenseDatabase.java:237-286`). `add()` writes to `<perpetualLicenseDir>/db/<hostId>/<name>.license`, skipping
wildcard `"*"` hosts and older-`generated` duplicates (:43-52,406-418).

## §479.4 — `inbox` auto-import + commissioning `[CERT]`

`LocalLicenseDatabase.init()` imports the license dir and **`security/licenses/inbox`** (moving files out),
then `exportNewLicenses()` copies host-matching licenses into the active license dir
(`LicenseDatabase.java:488-502,544-613`); `importDir` deletes `.lar` after import and deletes non-local-host
`.license` after adding (:544-570). Commissioning: `LicenseSelection.getLicenseSync()` +
`getSelectedLicenseMode()` (perpetual vs subscription) (`LicenseSelection.java:15-24`); `LicenseStep` applies
the sync through the same file-transfer path.

## §479.5 — `LicenseSync` + provisioning + portal `[CERT]`

- `LicenseSync<T>` = diff container `toAdd/toUpdate/toRemove` (`LicenseSync.java:8-17`), COMPUTED by
  `PortalLicenseUtil.syncLicenses(hostId, current[], toInstall[])` (dedup by vendor, keep newest `generated`,
  `shouldInstall` compares **signature strings** :314-366,451-479) and APPLIED by `PortalLicenseUtil.sync(...)`
  (:368-382) — file transfer, not crypto.
- **Provisioning (Supervisor→N stations):** `BUpdateLicensesJobStep`: per station reads its summary over
  `BDaemonSession`; subscription stations pull via `SubscriptionLicenseMessage`; perpetual/online only if
  `nwExt.getLicenses().getAllowLicenseServerAccess()` → `PortalLicenseUtil.getPortalUpdates`; **offline
  fallback** = serve newer licenses from `LicenseDatabase.LOCAL_INSTANCE.getLicenses(hostId,brand)`
  (`BUpdateLicensesJobStep.java:179-507`).
- **Portal (online):** `PortalLicenseUtil` reflects into `com.tridium.portal.api.PortalApi` (`PortalApi.java`):
  `DEFAULT_HOST="https://axlicensing.tridium.com"` (override via brand `license.server` / `-Dportal.host`),
  endpoints `POST /ws/license/api31/{getByHostId,getUpdates,getCertsByHostId,getCertByVendor}`,
  `GET …/ping`; plain `HttpURLConnection`, `text/xml`, 30 s (`PortalApi.java:25-124,194-200,279-332`). This is
  the **perpetual** activation portal (distinct from the subscription runtime `niagaracentralapis.honeywell.com`
  of [B477 §477.3]).

## §479.6 — TRUST AT INSTALL: the platform performs NO signature verification `[CERT]` (security-relevant)

The platform install path is a **trusted transport + hostId/date gate only**:
- WB sync / `PortalLicenseUtil` only compare `signature` strings for equality and `generated` timestamps to
  decide add-vs-skip — no RSA/hash verify against a Tridium key (`PortalLicenseUtil.java:98-122,451-479`).
- Supervisor DB `add()` rejects only `hostId=="*"` and older duplicates — no signature check
  (`LicenseDatabase.java:43-48`).
- `niagarad`'s own `com/tridium/niagarad/license/LicenseFile.load()` checks XML root, `vendor`, **hostId equals
  local `getHostId()`**, clock sanity, `generated`/`expiration` — and nothing else (`LicenseFile.java:33-99`);
  `grep signature|verify|publicKey|rsa` across `com/tridium/niagarad/license/` = **ZERO hits**; daemon
  `Feature.check()` is expiry-only (`Feature.java:37`).
- **The cryptographic trust decision (DSA signature over the embedded master key) is owned by the STATION JVM
  at load time** ([B477 §4], `com.tridium.sys.license.LicenseFile.load` → `LicenseUtil.verify`), NOT by the
  platform install path. `[INFER]` corroborated by the absence of any verify call platform/daemon-side +
  station-side logic in [B477].

## §479.7 — Daemon license endpoints (the TLS platform session) `[CERT]`

- **`FileServlet`** (`niagarad-ext/…/servlet/FileServlet.java`, registered `NiagaraDaemon.java:677`): receives
  the `FileTransferMessage`, `doPost`→`writeFile`, every path canonicalized and constrained to
  `canonicalRootDir` (= NIAGARA_USER_HOME), special keyring handling for `security/.kr/.km/.sp`; max 512 MB.
  This lands the `.license`/`.lar`/cert under `security/licenses[/certificates]`.
- **`UpdateDaemonServlet`**: the control endpoint — `reloadLicenses` → `LicenseManager.getInstance().reload()`
  (:353), `reloadLicenseMode`, `subscriptionMode`, `updateSubscriptionLicense`, `regenerateNreId`,
  `startAccessTokenPoll` (OAuth **device-code**: returns `user_code`+`verification_uri`), `register` →
  `getRegistrationApi().register(status, licenseKey)` (key format `XXXX-XXXX-XXXX-XXXX`).
- **Fox counterpart (station-side):** `BLicenseChannel` exposes `listlicenses/updateentitlements/…` and file
  `read/write/delete`; **write/delete require `adminInvoke` on `BLicensePlatformService`**
  (`BLicenseChannel.java:446-449,513-516`), path-restricted to the licenses/certificates dirs.

## §479.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | `BLicenseManager` is a `platform:DaemonSession` view that administers licenses | `[CERT]` | `BLicenseManager.java:213-218,344` | PASS |
| 2 | Install = `FileTransferMessage`→`DaemonFileUtil.transfer`→`FileServlet` + `ReloadLicenseMessage` | `[CERT]` | `BLicenseManager.java:446-525`; `FileServlet` @ `NiagaraDaemon.java:677` | PASS |
| 3 | `.lar` = ZIP of `licenses/<hostId>/<name>.license`, `@FileExt("lar")` | `[CERT]` | `BLicenseArchiveFile.java:18-91` | PASS |
| 4 | `.license`/`.lar` import → `db/<hostId>/`; inbox auto-import | `[CERT]` | `LicenseDatabase.java:237-286,488-613` | PASS |
| 5 | LicenseSync diff computed by `PortalLicenseUtil.syncLicenses` (signature-equality) | `[CERT]` | `PortalLicenseUtil.java:314-382,451-479` | PASS |
| 6 | Provisioning push per-station; offline fallback to Supervisor `db/<hostId>` | `[CERT]` | `BUpdateLicensesJobStep.java:179-507` | PASS |
| 7 | Portal = `axlicensing.tridium.com /ws/license/api31/*`, plain HTTP text/xml | `[CERT]` | `PortalApi.java:25-124` | PASS |
| 8 | **Platform install does NOT verify signature** — only hostId + dates; crypto deferred to station | `[CERT]`/`[INFER]` | `PortalLicenseUtil.java:451-479`; `niagarad LicenseFile.java:33-99` (grep verify=0); [B477 §4] | PASS |
| 9 | Fox `BLicenseChannel` write/delete require `adminInvoke` on `BLicensePlatformService` | `[CERT]` | `BLicenseChannel.java:446-449,513-516` | PASS |

**Tally:** 9 claims, 8 `[CERT]` + 1 `[CERT]`/`[INFER]` (claim 8's deferral is INFER, its "no verify call" half is CERT), 0 unmarked.

## §479.9 — Connections

- **Completes the license lifecycle triad:** [B479] delivery/install (platform, no crypto) → [B477] station
  reads + DSA-verifies → [B478] `niagarad` enforces (exit codes + platform-feature gate).
- **Security thread:** reinforces [B392]/[B477] — Niagara defers the license crypto trust to the station load;
  the platform layer is trusted transport + hostId/date gate. The install path itself does not stop a
  tampered/foreign license from being STORED — it just won't VALIDATE later (station `isValid()==false` →
  zero features). Consistent with [B387]/[B477].
- **Deliverable** `docs/niagara-licensing.md` §6.5 already covers activation/import at a high level; this block
  is the code-level mechanism.

## §479.10 — Open gaps

- **B478-G1** (still open, deferred) native `shmem`/`createWatchdog` watchdog contract — liveness, not licensing.
- **B479-G1** the `RegistrationApi`/`AccessTokenApi`/`DeviceCodeApi` device-code registration flow end-to-end
  (subscription onboarding) — requires reading `portalApi`/subscription client details or a live §12 run.
