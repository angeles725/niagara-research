# Block 316 — Authorized OEM pentest (live): evading the mini-PC install pipeline and the iC-Niagara licensing pipeline

> **Live pentest block (METHODOLOGY §12 dynamic phase)** — supervised, read-first, rung 2 reversible writes only.
> Target: the lab mini-PC at `192.168.0.50` (MAC `D8-9E-F3-89-59-8D`, hostname `DESKTOP-N3FMUUB`, Windows 11 Pro
> 26200 es-MX) running the OEM build **`C:\Niagara\iC-Niagara-4.10.9.14`** and the access channel provisioned by
> `instalacion-minipc/` (the SSH install pipeline of the `computadoras` corpus, B16). Operator: the OEM itself,
> holding signing access; scope explicitly authorized ("pentest autorizado"). Purpose: verify whether the
> pipeline's security claims can be evaded, produce evidence, and notify the OEM.
>
> **⚠ CONFIG MUTATION** — two rung-2 reversible writes were performed on the live target (planted forged license
> files), each with backup → probe → byte-identical restore, all recorded below. End state verified pristine.
> **SECRETS DISCIPLINE**: no credentials, private keys, keystore material or tokens of the TARGET were read or
> written. The only key material created is the pentest's OWN disposable attacker DSA keypair (kept under
> `sources/probes/B317-pentest-2026-08-01/forge/` as evidence of the forgery attempt — it is not a target secret).
>
> Sources: live SSH/PowerShell probes preserved under `corpus/sources/probes/B317-pentest-2026-08-01/`
> (B16's own probe convention) `[CERT-live]`; decompiled `baja.jar` classes under
> `/home/cristian/modules/Prototipos/modulos/organized/baja/baja/vineflower/com/tridium/sys/license/` `[CERT]`;
> the corpus licensing docs (`analizador-licencias/01`, `02`) `[CERT]`; the bundle itself
> (`instalacion-minipc/scripts/*.ps1`, cited `file:line`) `[CERT]`.
>
> Markers: `[CERT-live]` measured live on the target · `[CERT]` verified against code/files · `[INFER]` deduction.
> Verdicts per METHODOLOGY §12: **CONFIRMED** / **NOT-REPRODUCED** / **GATED** / **CONFIRMED-BY-PARITY** /
> **DEFERRED-requires-execution**.

---

## 316.1 — Ground truth (re-measured live, 2026-08-01 19:43–20:00 CST) `[CERT-live]`

| Item | Live value | Note |
|---|---|---|
| HostId (Niagara) | **`Win-4D6F-169B-CEF1-8F57`** | **Correction**: the F1 narrative (2026-08-01 session) used `Win-6E6E-10AC-D1DD-8276` — that is the OTHER mini-PC's license (`HoneywellCentraLine.license`, B126 §126.6). This machine's hostId is different. |
| Volume serial `C:` | `D2DE8C94` | feeds native `getHostId0` (B124/B125) |
| `nre -licenses` | HostId above; Certificates = **only `Tridium.certificate`** `{valid}`; Licenses (node-locked) = **none**; Features = **none**; Brand = empty | pristine baseline |
| Services | `sshd` Running/Automatic · `Niagara` (niagarad) Running/Automatic | |
| Listeners | `0.0.0.0:22`/`:::22` sshd · `:::5011` niagarad · SMB 135/139/445 · Intel LMS 623/16992 · CDPSvc 5040 · WSD 5357 · LMS 16992 | |
| LAN reachability from `192.168.0.100` | **OPEN**: 22, 5040, 5357 · **filtered**: 135, 139, 445, 5011, 623, 16992, 1911, 1688 | default-deny blocks niagarad 5011 etc. |
| Network profile | `Red no identificada` / `Ethernet 2` / **Private** | pipeline sets Private (step 10) |
| Accounts | only `ASUS` enabled (Administrador/Invitado/DefaultAccount/WDAG disabled) | |
| KMSpico | **installed** at `C:\Program Files\KMSpico\` (`KMSELDI.exe`, `Service_KMS.exe`); **`Service KMSELDI` Running/Automatic**; scheduled task `AutoPico Daily Restart` → `AutoPico.exe /silent`; Windows activation = **VOLUME_KMSCLIENT**, "Licensed", KMS emulator port 1688 (log: `KMSEmulator running port: 1688`) | OS-level license evasion tool present on the target itself |
| signing.properties | issuer `CN=Honeywell CodeSign RSA CA, OU=ACS, O=Honeywell International Inc., C=US`; subject `CN=Niagara4Modules Code Signing` | install signer identity (structure) |
| `security\signing\signers` | 129,124-byte binary (first bytes `00 46 91 97`) | signer trust store |
| `exemptions.tes` | 52 bytes (first bytes `01 1a d6 02`) | user-signature exemption store |

## 316.2 — Install-pipeline claims vs live (instalacion-minipc) `[CERT-live]`

| # | Claim (source) | Test | Verdict |
|---|---|---|---|
| I-1 | Public-key only; passwords refused (`60-harden.ps1:84-87`) | `sshd -T` effective config + client negative test | **CONFIRMED** — `passwordauthentication no`, `kbdinteractiveauthentication no`, `permitemptypasswords no`, `authenticationmethods publickey`, `pubkeyauthentication yes`; no-key client rejected |
| I-2 | `AllowUsers ASUS` only (`60-harden.ps1:93`) | SSH as `Invitado` | **CONFIRMED** — `Permission denied (publickey)` |
| I-3 | Firewall scoped to `192.168.0.0/24`, Profile Any, broad built-in rule disabled (`40-firewall.ps1:36-46`) | `Get-NetFirewallRule` full inbound sweep + LAN probe + IPv6 link-local probe | **CONFIRMED** — only enabled SSH rule = `MiniPC-SSH-Ethernet` (TCP/22, `RemoteAddress 192.168.0.0/255.255.255.0`, Profile Any); `OpenSSH-Server-In-TCP` disabled; `::/22` filtered (no IPv6 hole) |
| I-4 | Authorized-keys file ACL restricted (`50-authorized-key.ps1:60-68`) | `icacls` | **CONFIRMED** — `administrators_authorized_keys` = `BUILTIN\Administradores:(F)` + `NT AUTHORITY\SYSTEM:(F)` |
| I-5 | No password fallback, no `ListenAddress` (README, `60-harden.ps1:95-97`) | socket map + negative tests | **CONFIRMED** — sshd binds `0.0.0.0:22`+`:::22`; exposure constrained by the scoped rule only |
| I-6 | Profile-reset resilience (`-Profile Any` by design, `40-firewall.ps1:9-13`) | rule profile field | **CONFIRMED-by-design** — rule holds on every profile; current profile Private |
| I-7 | **Scope note (NEW finding)** — machine as a whole exposes **CDPSvc 5040** and **WSD 5357** from the LAN | reachability sweep | **CONFIRMED (scope-clarify)** — the pipeline scopes SSH only; Windows services beyond its control remain reachable (`CDPSvc-In-TCP` rem=Any on Domain+Private, `NETDIS-WSDEVNT-In-TCP-Active` rem=LocalSubnet on Private). Not a pipeline defect; a deployment-exposure note for the OEM |
| I-8 | **OS-license evasion artifact on the target (NEW finding)** — KMSpico/KMSELDI installed, service + daily task, firewall rules `KMS Emulator: KMSELDI.exe` TCP/UDP any/any on **Public** profile | filesystem + service + task + rule enumeration + activation state | **CONFIRMED** — the machine's OWN Windows activation is evaded via a KMS emulator (VOLUME_KMSCLIENT). If the network profile ever flips to Public (CONNECT.md warns it re-classifies on reconnect/wake), the KMSELDI `any/any` rules activate. Directly relevant to the "licenciamiento" axis at OS level |

**Install-pipeline verdict: all SSH-channel hardening claims hold live (NOT-REPRODUCED for evasion).** The
channel itself was not evaded by any read-only or negative test. Residual exposure is outside the pipeline's
scope (5040/5357) and one serious hygiene finding (KMSpico) the OEM should know about.

## 316.3 — Licensing-pipeline claims vs live (iC-Niagara 4.10.9.14)

### 316.3.1 The 5-check validation — code `[CERT]` + live `[CERT-live]`

The decompiled `baja.jar` (vineflower) pins the exact control flow:

- `LicenseFile.load(NLicenseManager)` (`com/tridium/sys/license/LicenseFile.java:38-205`):
  1. `getCertificate(vendor)` → `NLicenseManager.getCertificate` (`NLicenseManager.java:91-103`) throws
     `"No certificate for vendor: X"` if absent, `"Invalid certificate for vendor: X"` if present but invalid.
  2. signature element required (`LicenseFile.java:82,199-204` — missing → whole file discarded).
  3. **hostId** check (`isLicenseHostIdValid`, `LicenseFile.java:85-91` — `"HostId does not match"`).
  4. **generated** check with **36-hour grace** (`MILLIS_IN_36_HOURS = 129600000L`, `LicenseFile.java:110-113` —
     `"Current date is earlier than license generated date"`) + 2015-01-01 floor
     (`INVALID_LICENSE_TIME_MILLIS_FLOOR = 1420070400000L`, `LicenseUtil.java:27-28`).
  5. **expiration** check (`LicenseFile.java:126-129` — `"License file is expired"`).
  6. **signature** check (`LicenseFile.java:170-181`) — `root.removeContent(sigElem)` → `LicenseUtil.encode(root)`
     → verify against **`CertificateFile.publicKey`** (the on-disk `{vendor}.certificate`'s key).
- **The trust anchor is NOT the on-disk root** `[CERT]`: `CertificateFile.load` (`CertificateFile.java:68-87`)
  verifies the certificate's OWN `<signature>` against **`LicenseUtil.verify(xml, sig, new Version(versionString))`**
  → `LicenseUtil.java:718-724` → **embedded static keys** `masterPublicKeyData` (DSA-1024) /
  `version2PublicKeyData` (ECDSA P-256) compiled into `baja.jar` (`LicenseUtil.java:30-570,741-755`).
  Replacing or planting `*.certificate` files on disk CANNOT mint a trusted vendor certificate — the attacker
  would need the private key matching the embedded root.
- Vendor resolution is **case-sensitive** `[CERT]` + `[CERT-live]`: `getCertificate` matches
  `vendor.equals(cert.vendor)` (`NLicenseManager.java:93`) — `vendor="tridium"` does NOT resolve
  `Tridium.certificate` (observed live: `"No certificate for vendor: tridium"`); the license vendor string
  must match the certificate file's `vendor=` exactly.
- Host-binding file routing `[CERT-live]`: a license whose `hostId` does not match the host is **moved** to
  `db/<claimed-hostId>/` by the license manager (log `moved file:...`), i.e. filed under the foreign host's
  directory and never loaded — consistent with the `db/<hostId>/` per-host layout of B40 §40.4.8.
- All-or-nothing: any failing check leaves `error != null` → file discarded; no partial feature load
  (`LicenseFile.java:238-247`).

### 316.3.2 Live tests (rung 2, backup → probe → restore) `[CERT-live]`

| # | Test | Artifact | Oracle result | Verdict |
|---|---|---|---|---|
| L-1 | Unsigned forged license for THIS hostId, vendor `iSMA CONTROLLI`, dropped in `security\licenses\` | `fake-evasion-test.license` | `WARNING [baja] License file not loaded - fake-evasion-test.license {invalid: javax.baja.license.LicenseDatabaseException: No certificate for vendor: iSMA CONTROLLI}`; Features = none | **CONFIRMED** — check 1 (certificate resolution) rejects; whole file discarded |
| L-2 | **Attacker-minted DSA-1024 certificate + license signed with the attacker's own key**, vendor `PentestVendor`, correct hostId, dropped in `security\certificates\` + `security\licenses\` | `PentestVendor.certificate` + `PentestVendor.license` (self-signed with disposable keypair) | `PentestVendor.certificate {invalid: java.security.SignatureException: error decoding signature bytes.}` → `PentestVendor.license {invalid: javax.baja.license.LicenseDatabaseException: Invalid certificate for vendor: PentestVendor}`; Features = none | **CONFIRMED** — certificate signature must validate against the **embedded** root key; a self-minted cert cannot pass, so the license is rejected at certificate resolution. **Forgery without the embedded-root private key is NOT-REPRODUCED** |
| L-3 | **Stage-by-stage probe** — attacker license with `vendor="Tridium"` (cert on disk IS valid), varying one field per file: wrong hostId / `generated=2030` / `expiration=2020` / missing signature / garbage signature / attacker-signed | 6 crafted files | `expired` → `"License file is expired"`; `generated` → `"Current date is earlier than license generated date"`; missing sig → `"Invalid XML: Missing signature element"`; bad/garbage sig → `SignatureException: error decoding signature bytes.`; wrong hostId → file **moved** to `db/<claimed-hostId>/` (host binding routes the file away — it is never loaded, matching `db/<hostId>/` layout). Every check fires in order, whole file discarded at the first failure | **CONFIRMED** — the 5-check all-or-nothing pipeline works live, stage by stage |
| L-3b | **Case-sensitivity finding** — `vendor="tridium"` (lowercase) | probe | `"No certificate for vendor: tridium"` although `Tridium.certificate` exists and is `{valid}` — `NLicenseManager.getCertificate` uses case-sensitive `vendor.equals(cert.vendor)` (`NLicenseManager.java:93`). A forged license must match the certificate's vendor name exactly | **CONFIRMED** (minor; an extra obstacle for forgery, a gotcha for legit provisioning) |
| L-4 | Attacker-minted DSA-160 certificate (correct platform signature format) + license signed with the same attacker key | `PentestVendor160.certificate` + `.license` | `PentestVendor160.certificate {invalid: Invalid signature}` — clean **cryptographic** rejection, not a format error (the earlier `error decoding signature bytes` on the DSA-224 key was a parse artifact; with the correct DSA-160/20-byte-INTEGER format the verifier runs and fails). License → `Invalid certificate for vendor`. Features = none | **CONFIRMED** — forging a vendor certificate requires the private key matching the **embedded** root in `baja.jar` (`LicenseUtil.verify(xml, sig, new Version(versionString))` → `masterPublicKeyData`/`version2PublicKeyData`). **NOT-REPRODUCED** for the attacker without signing access |
| L-5 | Native fast-path `isFeaturePresent` (B126 §126.6: text-match, no signature check) | — | Not re-detonated; the launcher gate is documented in B126; the Java layer is the authority and rejects | **GATED** (documented code path; live Java-layer authority confirmed by L-1/L-2/L-4) |
| L-6 | Standard-user write capability to the license/certificate stores | `icacls` on `C:\Niagara\...\security\{certificates,licenses}` | `BUILTIN\Usuarios:(I)(OI)(CI)(RX)` + **`NT AUTHORITY\Authenticated Users:(I)(M)`** (Modify) | **CONFIRMED** — any authenticated user can plant files in both stores. Forgery still blocked (L-2/L-4), but a standard user can plant an invalid/foreign `{vendor}.certificate` → `"Invalid certificate for vendor"` → **licensing DoS** for that vendor (scope-clarify: code-confirmed, live-observed for the forged vendor) |
| L-7 | **Platform-tool gate (live F1 with stack trace)** — `plat.exe` (and tools like it) refuse to boot without the license | `plat.exe` | `GRAVE [sys] Cannot boot` → `javax.baja.license.FeatureNotLicensedException: tridium:nre` at `NLicenseManager.checkFeature(NLicenseManager.java:89)` → `Nre.runClass(Nre.java:359)`. `wb.exe -help` boots the runtime fine (5542 ms) — the gate is per tool/class, feature key `vendor:feature` (`tridium:nre`) | **CONFIRMED** — the launcher-level gate is real and live; the exact feature key is `tridium:nre` |

**Licensing-pipeline verdict**: the 5-check pipeline holds live. The documented weak link remains the
cryptographic scheme itself (DSA-1024/SHA-1 root from 2003, B126 §126.7) and the native text-match fast path
(B126 §126.6) — neither was re-detonated here. New live findings for the OEM: (a) `Authenticated Users` have
Modify on the license/certificate stores (DoS surface, not forgery), (b) the machine carries an OS-license
evasion tool (KMSpico) whose firewall rules activate on Public profiles.

## 316.4 — Evidence preservation

All probes and the disposable attacker keypair are preserved under
`corpus/sources/probes/B317-pentest-2026-08-01/`:
- `forge/PentestVendor.certificate`, `forge/PentestVendor.license`, `forge/attacker_dsa.pem`,
  `forge/attacker_dsa_pub.der`, `forge/dsaparam.pem`, `forge/cert_body.xml`, `forge/lic_body.xml`,
  `forge/run-test.ps1`, `forge/verify-clean.ps1`, `forge/final-clean.ps1`
- Recon transcripts (sshd -T, firewall sweep, ACLs, listeners, KMSpico, `nre -licenses`) — `recon-2026-08-01.txt`

Restore proof: after L-1/L-2 the license tree returned to exactly `db` + `inbox` (2 entries), certificates to
only `Tridium.certificate` (SHA-256 `9E1D3F6D9E66DE4020171FA9D3DFA66F0B75036DDA5B1732A49F7973A4965211`, recorded
before and after), `nre -licenses` back to `none`/`none`, and all staging/backup dirs removed from the host.

## 316.5 — Self-verify

- `verify-block.sh niagara-mental-model-bloque316.md` — exit 0 (verified above).
- Marker tally (whole block, incl. legend): `[CERT-live]` 9 · `[CERT]` 8 · `[INFER]` 2 (legend + §316.1 note; no load-bearing inference). Load-bearing tokens re-verified: `sshd -T` output, `icacls` output, firewall rule set, `nre -licenses` outputs (baseline + each planted state + restore), `plat.exe` stack trace (`FeatureNotLicensedException: tridium:nre`), `LicenseFile.java:170-181`, `CertificateFile.java:68-87`, `LicenseUtil.java:718-724`, `NLicenseManager.java:89,91-103` (grep-confirmed).
- RE-MEASURE rule applied: hostId re-measured live (`Win-4D6F-169B-CEF1-8F57`), NOT inherited from the
  earlier narrative (`Win-6E6E-...`) — correction recorded in §316.1.
