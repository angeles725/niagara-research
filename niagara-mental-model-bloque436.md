# Block 436 — The platform admin UI: platform-wb connects, platDaemon-wb is the client of the plat.exe daemon over the 3011/5011 wire

> Research of **`platform-wb` + `platDaemon-wb`** (focus `workbench`, gap WB10, LOW) — the Workbench-side
> platform administration UI that talks to a JACE/host platform daemon. Scope: the tool catalog, the
> connection/session model, the platform-vs-station credential split, TLS settings, and the commissioning
> wizard. Concise LOW block. Does NOT re-derive the daemon internals ([Block 381] plat.exe) or the wire
> ([Block 129] 3011/5011).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `platform-wb.jar`
> sha256 `2c83f51391738cb38345ffb91497a0bb3b3e67a86d17640058f9e3db439c3790` · `platDaemon-wb.jar`
> sha256 `7a12d5c04c7532659e48d7571aebef4f6725e844e100f7e87bb1fd3403f943d5`.
>
> Sources: Vineflower impl (`sources/decompiled/{platform-wb,platDaemon-wb}/`) + `platDaemon-wb` `module.xml`.
> Method: module.xml for the tool catalog, Vineflower for the connect/credential/TLS classes. CAVEAT:
> `BDaemonCnxHandler`/`BPlatformConnectionOptions` decompile MANGLED (port constant → token `n`); those claims
> are cited by REMITTANCE ([Block 129]/[Block 381]) or flagged as unverified. Markers: `[CERT]` (`file:line`)
> · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 381] (plat.exe — the daemon this UI is the client of), [Block 129]
> (3011 plain / 5011 TLS wire), [Block 392]-[Block 395] (platform crypto — `platCrypto` CertManager).

---

## 436.1 — Two modules: connect layer vs tool UI `[CERT]`

`platform-wb` is the CONNECTION layer (the `platform:` ORD scheme, the session/connect handler
`BDaemonCnxHandler`, ORD field editors, service-property editors). `platDaemon-wb` is the TOOL UI layer that
runs once a session is established. The tool catalog (from `platDaemon-wb` `module.xml` type registrations):
`[CERT]`

`PlatformAdministration` · `ApplicationDirector` · `SoftwareManager` · `StationCopier` · `LicenseManager` ·
`LexiconInstaller` · `FileTransferClient` · `TcpIpConfiguration` · `DistInstaller` (+ `platCrypto:CertManagerView`,
`platWifi:WifiConfiguration`, `platIEEE8021X` from sibling modules). `[CERT]` `[INFER]` this is the "Platform"
nav node's child set — every JACE-commissioning screen a Workbench engineer uses.

## 436.2 — The Workbench is a CLIENT of the plat.exe daemon `[CERT]`/`[INFER]`

`platDaemon-wb` sends daemon-protocol messages (`InitializeSessionMessage`, `AccountManagementMessage`,
`SystemPasswordMessage`, `FileTransferMessage`, `ReloadLicenseMessage`, `OSUpdateMessage`, …) over the platform
socket. `[CERT]`/`[INFER]` These are exactly the wire messages `plat.exe` ([Block 381]) implements on the daemon
side — so this block's UI and B381's native daemon are the two ends of the SAME protocol. The connection uses
the `platform:` ORD scheme through `BDaemonCnxHandler` (plain) and `BDaemonSecureCnxHandler` (TLS, in
`platCrypto`); the ports are **3011 (plain) / 5011 (TLS)** — REMITTANCE to [Block 129]/[Block 381] (the
`BDaemonCnxHandler` decompile is mangled and cannot cite the constant, but the wire is already `[CERT]` in the
corpus). `[CERT-remit]`

## 436.3 — Platform credentials are OS/file-domain, distinct from station users `[CERT]`

A platform login is NOT a Niagara station user — it is an OS/file-domain credential authenticated against the
daemon (`BUsernameAndPassword`). `[CERT]` `DaemonCredentialsManager.setCredentials` saves the credential to TWO
realms — the daemon session realm and the connection-handler realm — so Workbench can cache and reconnect
(`sources/decompiled/platform-wb/com/tridium/platform/ui/DaemonCredentialsManager.java:12`,`:13`,`:16` via
`AuthUtil.saveCredentials`). `[CERT]` `[INFER]` this dual-realm cache is why Workbench re-offers stored platform
credentials — and a place platform creds persist client-side, separate from the station's user service
([Block 435]).

## 436.4 — TLS settings and commissioning are version- and policy-gated `[CERT]`

`BDaemonSSLSettingsView` fetches the daemon's key aliases (`crypto?action=sendServerAliases`) and exposes
SSL-disabled / SSL+plain / SSL-only, port, key alias, and — gated on daemon version — a key passphrase (≥ 4.13,
`KEY_PASSWORD_SUPPORT_VERSION`) and TLS 1.2/1.3 + extended-master-secret (`MIN_MASTER_SECRET_NIAGARA_VERSION`)
(`sources/decompiled/platDaemon-wb/com/tridium/platDaemon/ui/BDaemonSSLSettingsView.java:116`,`:123`). `[CERT]`
`[INFER]` FIPS mode restricts the choice to TLS 1.2/1.3.

The **commissioning wizard** drives a fresh JACE through ~10 steps; `AuthStep extends CommissioningWizardStep`
(`.../ui/AuthStep.java:52`) negotiates the auth method (SCRAM-SHA512/native, SCRAM-bcrypt, digest/file, native
OS accounts) and creates/removes OS-level accounts. `[CERT]` `[INFER]` the sweep reports two default-`true`
hardening flags in `BPlatformConnectionOptions` — `requireDefaultAccountRemoval` and
`requireDefaultSystemPassphraseChange` (commissioning blocks unless the default OS account is removed and the
system passphrase changed). That class was NOT locatable in this decompilation, so the flags are recorded as an
**unverified sweep finding**, not `[CERT]`. `[INFER]`

## 436.5 — No Workbench license gate; the gate is the daemon credential `[CERT]`/`[INFER]`

No per-feature license gate guards platform UI availability in either module. `[CERT]` (The deprecated
`BPlatManagementProfile` whitelists view types only for the standalone Platform Administration Tool launch mode;
normal Workbench shows all platform views.) The only real gate is successful DAEMON authentication — anyone who
can authenticate to the daemon with OS/file-domain credentials gets the full platform toolset. `[INFER]` this
matters for a threat model: platform access is bounded by the OS-level daemon credential, not by a Niagara
station role.

## 436.6 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | platform-wb = connect layer; platDaemon-wb = tool UI (Software Manager, Station Copier, App Director, …) | `[CERT]` | `platDaemon-wb module.xml` |
| 2 | The UI sends daemon-protocol messages = the client end of plat.exe's wire; ports 3011/5011 | `[CERT]`/`[CERT-remit]` | §436.2; [Block 129]/[Block 381] |
| 3 | Platform creds = OS/file-domain `BUsernameAndPassword`, saved to TWO realms for reconnect | `[CERT]` | `DaemonCredentialsManager.java:12`,`:16` |
| 4 | TLS settings version-gated (key passphrase ≥4.13, TLS 1.2/1.3 + EMS) | `[CERT]` | `BDaemonSSLSettingsView.java:116`,`:123` |
| 5 | Commissioning `AuthStep` negotiates SCRAM/native/file auth + OS account create/remove | `[CERT]` | `AuthStep.java:52` |
| 6 | Two default-true hardening flags (default-account removal, passphrase change) | `[INFER]` | unverified sweep finding (class not located) |
| 7 | No Workbench license gate; the gate is daemon authentication | `[CERT]`/`[INFER]` | §436.5 |

**Marker tally**: `[CERT]` ≈ 14 · `[INFER]` 7 ([INFER]/[CERT] ≈ 0.50). Type: **EVIDENCE block** (LOW survey) —
ratio at the top of healthy because two load-bearing claims (port constant, hardening flags) hit MANGLED /
absent decompile and were honestly downgraded (REMITTANCE / unverified) rather than asserted. Tokens confirmed:
the module.xml tool names, `DaemonCredentialsManager.setCredentials`, `getDaemonVersion`/`createKeyPassword`,
`AuthStep extends CommissioningWizardStep`.

## 436.7 — Connections

- **[Block 381]** — plat.exe is the daemon; this block is its Workbench client. The `AccountManagement`/
  `SystemPassword` messages here are what B381's `setsystempw`/account natives serve.
- **[Block 129]** — the 3011/5011 platform wire; `BDaemonCnxHandler`/`BDaemonSecureCnxHandler` are its two
  client connectors.
- **[Block 392]-[Block 395]** — `platCrypto:CertManagerView` (in the tool catalog) is the UI over the platform
  cert/crypto this focus documented.
- **[Block 435]** — platform credentials cached by `DaemonCredentialsManager` are separate from the station
  users `BUserManager` administers.

<!-- research-block: focus workbench, gap WB10 (platform-wb + platDaemon-wb) — CLOSED at body grade (LOW); port/hardening claims honestly downgraded on mangled/absent decompile -->
