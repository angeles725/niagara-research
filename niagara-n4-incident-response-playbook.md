# Niagara N4 — Incident Response & Hardening Playbook

**Audience**: client security/operations team + Niagara integrators
**Scope**: Niagara N4.14 station compromise — an **unsigned module opened HTTPS 443**, bypassed Tridium's module-signing model, and **erased the internal audit trail**.
**Type**: defensive / remediation operational deliverable. Actionable, not academic.
**Status**: consolidated from prior internal research (see [References](#7-references)). Every technical claim below is traceable to an internal block (cited as `Bxx §y`). Items not backed by a block are marked **REQUIRES VALIDATION**.

---

## 1. Executive summary

An attacker deployed an **unsigned Java module** to the station and got it to **open TCP port 443 (HTTPS)** and then **wipe the station's internal audit history**. This was possible **without breaking Niagara's signature model** — because, with the factory configuration of this OEM build, the signature model never protected port-opening or the audit trail in the first place (B75 §75.0, §75.6).

Two facts make this an expected outcome of the default config rather than an exploit:

1. **Signature ≠ network.** Only 6 permission groups require a signature. `NETWORK_COMMUNICATION` is **not** one of them, so an unsigned module can declare a socket permission and open any port, including 443 (B75 §75.0, H2).
2. **`moduleVerificationMode=low`** is the factory default in this Honeywell build. In `low`, an unsigned module that only asks for `NETWORK_COMMUNICATION` **loads anyway** (fail-open); the same module asking for `REFLECTION` would be blocked (B75 §75.3; B17:90; B18).

The audit trail "left no trace" because `Sys.setAuditor(null)` turns the internal auditor off **without requiring any permission** (B75 H1, `Sys.java:178`).

**The good news (B112):** the attack is **detectable with native tooling already present in the station** — it was simply off or unmonitored. State-based signals (the malicious module, the weak mode, the effective permissions) are **not erasable** without undoing the attack, and the **platform daemon log lives at the OS level**, outside the reach of the in-station wipe.

### Most urgent actions (do these first)

| # | Action | Why | Detail |
|---|--------|-----|--------|
| A | **Set `niagara.moduleVerificationMode=high`** | Blocks loading of the unsigned module that opened 443 — closes the exploited link | §5 P0.1 |
| B | **Scan now with the nss Security Dashboard + PolicySpy** | Confirms whether a malicious module is loaded RIGHT NOW (fleet-wide via Fox) | §3 |
| C | **Harvest the daemon `console.log` (`~logging/console.*`) before it rotates** | This is the forensic evidence the attacker could NOT erase (deploy user/IP/timestamp) | §4 |
| D | **Blacklist the CLI bypass switches** (`skipModuleValidation` + `niagara.commissioning.ignoreVerificationMode`) | Both can re-open the door even after P0.1 | §5 P0.3 |

---

## 2. Attack vector reconstruction

Reconstructed and confirmed file:line in B75 §75.1. Summarized chain:

| Step | What happened | Evidence (B75) | Certainty |
|------|---------------|----------------|-----------|
| 1. Entry | Platform **daemon** credentials (3011 plaintext / 5011 TLS). Daemon only asks for user/password digest. | `BDaemonSession.java:263` | Mechanism CONFIRMED; the "how" (brute force / `niagara`/`niagara` defaults) INFERRED |
| 2. Deploy | Unsigned `.jar` whose `module.xml` declares `NETWORK_COMMUNICATION hosts=* ports=443`. Transferred, committed to the filesystem, station restarted. | `InstallScenario.java:956`, `BModuleInstallCommand.java:129` | CONFIRMED: deploy validates daemon credentials, **not** jar signature |
| 3. Signature bypass | Module loads clean because `NETWORK_COMMUNICATION.requiresSignature()=false` → `ModuleManager.verifyModuleSignature` raises nothing. | `ModuleManager.java:340`, `NetworkCommunicationPermissionGroup.java:22` | CONFIRMED |
| 4. Open 443 | `new SSLServerSocket(443)` under the registered `NiagaraSocketPermission`. | `NetworkCommunicationPermissionGroup.java:88-97` | Capability CONFIRMED |
| 5. Run payload | Inside the module or over the open socket. | — | INFERRED |
| 6. Erase trail | `Sys.setAuditor(null)` disables the auditor **with no permission check**; `HistoryDatabaseConnection.deleteHistory` deletes AuditHistory bypassing the Baja check; `LoggingPermission("control")` silences `java.util.logging`. | `Sys.java:178`, `HistoryDatabaseConnection.java:40`, `LoggingPermissionGroup.java:51` | Capabilities CONFIRMED |

**Why signing did not stop it.** Only 6 permission groups override `requiresSignature()` → `true`: `REFLECTION`, `ACCESS_CLASS`, `MBEAN_PERMISSION`, `HSM_SIGNING`, `PROTECTION_DOMAIN`, `THIRD_PARTY_PERMISSION` (B75 §75.0). `NETWORK_COMMUNICATION` inherits the default `false`, so the factory check `if (group.requiresSignature() && !codeSource.isSigned())` never fires for network capability (`NiagaraPermissionGroupFactory.java:196`).

**Alternate entry (station, not daemon):** an authenticated superuser can run arbitrary bytecode via a **BProgram** with no signature — `program.requireSigning=false` is the default, so an unsigned program only logs a warning (`BCode.java:202-214`). `Runtime.exec()` stays blocked unless `BProgramService.allowProgramRuntimeExec=true` (default `false`) (B75 §75.1).

### There are TWO signature gates, not one (B112 §112.2.3)

This refines B75. Closing one gate is not enough.

| Gate | Where it runs | Governed by | Bypass switch |
|------|---------------|-------------|---------------|
| **Load gate** (server-side) | In the station JVM at module load | `moduleVerificationMode` → `ModuleManager.verifyModuleSignature` (`:340`) | `skipModuleValidation` (B75 §75.4 P0.3; B17:90) |
| **Install gate** (client-side) | In the Workbench/commissioning JVM, before commit | `InstallScenario.solve` (`InstallScenario.java:302-326`) | **`niagara.commissioning.ignoreVerificationMode=true`** forces `verificationMode=low` even if the station is `medium`/`high` (`InstallScenario.java:291`) |

The daemon server (`niagarad`) has **no Java-equivalent check** — it commits whatever arrives authenticated over the file transfer (B112 §112.2.3). The load gate (P0.1) is correct and sufficient to **block loading**, but the install bypass switch must be blacklisted too (§5 P0.3).

---

## 3. Detection — is a station compromised NOW?

All three tools below are **native and already in the station**; they were off or unmonitored, not missing (B112 §112.0). Detection is **decoupled from prevention**: even if `low` lets the module load, these still flag it.

### 3.1 nss Security Dashboard (best for sweep)

Open the **SecurityService** view in Workbench (servlet logger `"securityDashboard"`, `SecurityDashboardServlet`). Two subsections matter (B112 §112.1):

- **Module Permissions** (`BSecurityDashboardModulePermissions`): cross-references *risk level of the permission groups* × *signature status*. The malicious module — unsigned + `NETWORK_COMMUNICATION` (`RiskLevel.MODERATE`, `NetworkCommunicationPermissionGroup.java:43`) — falls into `moderatePermissionGroupModulesSet` and is emitted as a **WARNING** with the module name in the summary (`:117-124`). A legitimate signed Honeywell module that opens BACnet shows as **OK** in the same subsection — so the WARNING is the discriminant.
- **Module Signatures** (`BSecurityDashboardModuleSignatures`): lists modules by signature status. The malicious module lands in the **`UNSIGNED`** bucket (yellow in `low`, red in `medium`/`high`, `:94-115`). This subsection **also flags `moduleVerificationMode` below default as a WARNING** (`:77-78`) — i.e. it directly calls out the weak mode.

**Fleet-wide (from a Supervisor):** `BSecurityService.getSystemDashboardData(cx)` aggregates posture across **every reachable station** over Fox and returns **JSON** (`BSecurityService.java:398,553`, gated by `hasAdminRead`). Use this to find the module replicated across the fleet (B112 §112.1.3).

### 3.2 PolicySpy (best for confirmation)

Open the spy page **`spy:/securityInfo/Policy Information`** in Workbench, or via the station spy URL `http://<host>/ord/spy:/securityInfo/...` (registered at `Nre.java:731`; requires the `VIEW_NIAGARA_POLICY` permission). It renders, module by module, the **live effective policy** (not `module.xml` on disk) (B112 §112.3).

For the malicious module the table shows the **smoking gun** literally:

| Column | Value for the attack module |
|--------|-----------------------------|
| `type` | `NETWORK_COMMUNICATION` |
| `params` | **`hosts=* ports=443`** ← direct proof it can open 443 (`PolicySpy:119` + `NetworkCommunicationPermissionGroup:49-60`) |
| `riskLevel` | MODERATE (gold circle) |

### 3.3 Loaded-module review

Cross-check the loaded modules against the expected signed inventory. Anything in the `UNSIGNED` bucket (§3.1) or showing `NETWORK_COMMUNICATION` in PolicySpy (§3.2) that you did not deploy and sign is a candidate. The trust anchor is the hardcoded Honeywell signing cert (`signing.properties`, `issuerDN=CN=Honeywell CodeSign RSA CA`, valid to year 9999) (B18 §18.1.5; B17:88-90).

---

## 4. Forensics / IOCs

The attacker wiped the **in-station** audit (B75 H1). The evidence that survives lives **outside** the station JVM.

### 4.1 The daemon `console.log` — primary forensic source (B112 §112.2)

The platform daemon (`niagarad`, native binary) writes its log at the **OS level**, in a directory it owns as a process — **not** the station JVM. A station-only attacker has no handle to rewrite it, and there is **no Baja API** to erase it (the contrast with `Sys.setAuditor(null)`, which only kills the in-station auditor) (B112 §112.2.1).

- **Location:** `{niagara_user_home}/logging/console.*` (`console.log`, `console.log.0`, …) — fixed by `BBackupService.java:149,165` (`~logging` + the `console.*` exclude pattern). Concrete paths (**INFERRED** by ORD convention, not literal in Java): Linux `…/niagara_user_home/logging/console.log`; Windows `C:\ProgramData\Tridium\niagara<ver>\logging\console.log`.
- **How to read it post-incident:** the daemon exposes it over HTTP via **`/systemlog`** (`GetSystemLogMessage.java:16`, optional `?log=<name>`) and **`/getdaemonoutput`** (`GetDaemonOutputMessage.java:7`, live console buffer). A dedicated `~audits` platform-audit dir also exists (`SystemFilePaths.java:32`).
- **What it captures that the Java layer does NOT:** the **deploy user, source IP, exact JAR name, and timestamp**. The client-side Java logging of the install flow does not record these (`BDaemonSession.acquireCredentials` logs **zero** auth detail; `DaemonFileUtil` logs bytes/duration but not filename or user) (B112 §112.2.2). **The literal line format of the native daemon log is REQUIRES VALIDATION** — read a real `console.log` from the compromised station to confirm the fields.

### 4.2 IOC table (what to look for, where)

| # | IOC | Where | Evidence (file:line) |
|---|-----|-------|----------------------|
| IOC-1 | Module in **WARNING** under "non-trusted modules with MODERATE permission groups" | nss Dashboard → Module Permissions | `BSecurityDashboardModulePermissions:117-124` + `NetworkCommunicationPermissionGroup:43` |
| IOC-2 | Module in the **`UNSIGNED`** bucket (yellow in `low`, red in `medium`/`high`) | nss Dashboard → Module Signatures | `BSecurityDashboardModuleSignatures:94-115` |
| IOC-3 | **WARNING** "verification mode below default" | nss Dashboard → Module Signatures | `BSecurityDashboardModuleSignatures:77-78` |
| IOC-4 | PolicySpy row `type=NETWORK_COMMUNICATION`, `params: hosts=* ports=443`, risk MODERATE | `spy:/securityInfo/Policy Information` | `PolicySpy:112-121` + `NetworkCommunicationPermissionGroup:49-60` |
| IOC-5 | Log line `"No code signers for entry … in module <X>"` — **WARNING one-shot** then drops to FINEST | station log (`console.log` / JUL) | `ModuleClassLoader:73,395-407` |
| IOC-6 | Log line `program.notSigned` | station log | `BCode.java:214` |
| IOC-7 | Daemon log: authenticated connection + install op (user / IP / timestamp of the deploy) | `{niagara_user_home}/logging/console.*` (via `/systemlog`, `/getdaemonoutput`) | `BBackupService:149,165`; `GetSystemLogMessage:16`; `GetDaemonOutputMessage:7` |
| IOC-8 | Suspicious sysprop active: `niagara.commissioning.ignoreVerificationMode=true` or `skipModuleValidation` | command-line args / `system.properties` | `InstallScenario:291`; B75 §75.4 P0.3 |
| IOC-9 | **Abrupt gap** in AuditHistory after a timestamp — the absence IS the signal of `Sys.setAuditor(null)` | station AuditHistory | `Sys.java:178` |

### 4.3 Signal → where → who can erase it (B112 §112.5)

| Compromise signal | Where to detect (file:line) | Erasable by a station-only attacker? |
|-------------------|-----------------------------|--------------------------------------|
| Unsigned module declares `NETWORK_COMMUNICATION ports=443` | nss Dashboard (IOC-1/2); PolicySpy (IOC-4) | **NO** — computed live from policy/modules on disk. Only disappears if the module is removed (which is already remediation). |
| `moduleVerificationMode` weakened (`low`) | nss Dashboard (IOC-3) `BSecurityDashboardModuleSignatures:77` | **NO** — reflects live config. |
| Unsigned module load event | `"No code signers"` warning (IOC-5) `ModuleClassLoader:397` | **PARTIAL** — in the station log; erasable if the attacker silences JUL (`LoggingPermission("control")`) or rotates the log. One-shot ⇒ fragile. |
| The **deploy** of the JAR (user, IP, timestamp) | daemon `console.log` (IOC-7) `BBackupService:165` + `/systemlog` | **NO** — OS-level, written by native `niagarad`; no Baja API to erase it. **Primary forensic evidence.** |
| Internal audit wipe | time gap in AuditHistory (IOC-9) `Sys.java:178` | The wipe **is** the action; the gap is the signal. Pre-image survives only if syslog offload (§5 P2) was on. |
| Station restart to load the module | daemon log `"Stopping stations on …"` | **NO** — server-side daemon `BModuleInstallCommand:128`. |

> **Operational reading:** for *presence*, use the Dashboard + PolicySpy (state, not erasable). For *who/when/from where*, use the daemon `console.log`.

---

## 5. Remediation / hardening

The P0/P1/P2 plan of B75 §75.4, turned into an actionable checklist. Each item: what to do, where, what risk it mitigates.

### P0 — Cut the primary vector

- [ ] **P0.1 — Mandatory signature for ALL modules.** Set `niagara.moduleVerificationMode=high` in `defaults/system.properties` (today `low`). **Closes:** the exploited link — blocks the unsigned module that opens 443 (and rejects self-signed, `ModuleManager.java:358-363`).
  - **Caveat:** at `high`, all custom modules must be signed with a cert that chains to the Honeywell trust anchor (`signing.properties`, `truststore.jks` holds only Honeywell/Tridium CAs — B17:152-153). **Validate the signing pipeline first** (B18; B26 standalone signing playbook) or the station will not bring up legitimate modules.
- [ ] **P0.2 — Mandatory signature for program objects.** Set `program.requireSigning=true` (today `false`). **Closes:** the BProgram alternate vector (`BCode.java:214` today only warns).
- [ ] **P0.3 — Blacklist the CLI/sysprop bypass switches.** Add **both** to `commandLineBlacklist` (`Nre.java:839`) and confirm neither sysprop is present:
  - `skipModuleValidation` (B75 P0.3) — re-opens the **load** gate (the `Webs.license` feature exists and cannot be removed from the license, B75 §75.5).
  - `niagara.commissioning.ignoreVerificationMode` (B112 §112.2.3, `InstallScenario:291`) — re-opens the **install** gate.
  - **Closes:** both signature gates' bypass levers (§2).

### P1 — Reduce the entry surface (network/OS)

- [ ] **P1.1 — Daemon TLS-only.** Set `BPlatformSSLSettings.sslOnly=true` (Platform Admin UI) → disables plaintext 3011, leaves only 5011 TLS. Requires the crypto ssl license feature (active, B75 §75.4). **Mitigates:** credential capture on the plaintext daemon port (the entry of step 1, §2).
- [ ] **P1.2 — External firewall.** Restrict 3011/5011 to admin IPs + control inbound 443 at the network/OS layer. **Gap:** N4 has **no IP filtering in the daemon** — must be done at network/OS (B75 §75.4).
- [ ] **P1.3 — Brute-force mitigation at OS level.** The daemon has **no account lockout** (`BUserService` lockout only covers station Fox/HTTP sessions). Use fail2ban/IPS + strong credentials (change the `niagara`/`niagara` defaults) (B75 §75.4).

### P2 — Detection / evidence (does not prevent, answers "left no trace")

- [ ] **P2.1 — Syslog offload to an external SIEM.** Set `BSyslogSettings.enabled=true` + `serverHost` (today `false`). The `publish` is **synchronous inside `audit()`, before** the local delete (`BAuditHistoryService.java:98-103`, `SyslogAuditHandler.java:18-55`) → **audit becomes tamper-evident**: the pre-image is off-station before any wipe (B75 §75.4 P2).
- [ ] **P2.2 — Harvest the daemon `console.log` to an external store.** Ship `~logging/console.*` off-host (alongside P2.1). It is the evidence that survives the internal wipe (§4.1; B112 §112.6). **REQUIRES VALIDATION** of the exact native line format on a real station.
- [ ] **P2.3 — Continuously monitor the nss Security Dashboard** (ideally via Fox from the Supervisor) and **alert** on any WARNING/ALERT in *Module Permissions* or *Module Signatures*. Free, native, continuous detection (B112 §112.6).
- [ ] **P2.4 — Alert on loader warnings.** Watch for `"No code signers"` (`ModuleClassLoader`, IOC-5) and `program.notSigned` (`BCode.java:214`, IOC-6). Note IOC-5 is **one-shot** per classloader — fragile if the log rotates.

---

## 6. Residual risk / gaps

Items with **no native mitigation** (accept, or mitigate outside N4) — from B75 §75.5 and B112:

| Gap | Evidence | Possible mitigation |
|-----|----------|---------------------|
| `Sys.setAuditor(null)` has no guard and needs no permission | `Sys.java:178` | Only P2.1 (syslog captures before the wipe) |
| `HistoryDatabaseConnection.deleteHistory()` skips the Baja permission check | `HistoryDatabaseConnection.java:40` | Syslog offload (P2.1) |
| `ProgramProtectionDomain` inherits a broad `CodeSource(null)` policy | `BCode.java:507` | Manually edit the `.policy` file to narrow program `SocketPermission` (advanced) |
| Daemon has no lockout / no IP filtering / no granularity to disable only deploy | `BDaemonSession.java` | Network/OS (firewall, IPS) |
| License carries `smDeveloperMode` + `skipModuleValidation` features (cannot be stripped) | `Webs.license` (B75 §75.5) | Block activation via blacklist (P0.3) |
| `VIEW_NIAGARA_POLICY` gives an authenticated attacker the full module→capability map (PolicySpy) | `PolicySpy.java:26-28` | Restrict the permission to admins (B112 §112.3) |

**Open validation items (not yet proven on a live station):**

1. **`moduleVerificationMode=high` blocking the unsigned module has not been empirically validated** against a live station — confirm it blocks a test unsigned module **before** applying in production (B75 close note; B112 §112.7).
2. **The literal line format of the native daemon `console.log` for an install op** is outside the decompiled Java corpus — confirm the exact captured fields (user/IP/JAR/timestamp) by reading a real `console.log` from the compromised station (B112 §112.7).

---

## 7. References

Internal research blocks (file:line traceability lives in the blocks):

| Block | File | Contribution to this playbook |
|-------|------|-------------------------------|
| **B75** | `niagara-mental-model-bloque75-security-incident.md` | **Attack vector reconstruction (prevention).** Chain `NETWORK_COMMUNICATION.requiresSignature()=false` → unsigned module opens 443 via `SSLServerSocket` + `NiagaraSocketPermission` → `Sys.setAuditor(null)` wipes audit → deploy via `BDaemonSession`. P0/P1/P2 hardening plan. §2, §5, §6. |
| **B112** | `niagara-mental-model-bloque112.md` | **Detection & forensics.** nss SecurityDashboard, daemon `console.log`, PolicySpy. IOC matrix. The **two signature gates** refinement (load + install). §3, §4, §5 P0.3/P2. |
| **B18** | `niagara-mental-model-bloque18.md` | Module signing model: `niagara-signing-plugin`, `signing.properties` Honeywell cert (year 9999), raw RSA-2048 `.sig` sidecar, `nverify.exe`. §3.3, §5 P0.1 caveat. |
| **B17** | `niagara-mental-model-bloque17.md` | Install filesystem: `signing.properties` hardcoded cert path, `truststore.jks` (Honeywell/Tridium CAs only), `moduleVerificationMode=low` default. §3.3, §5 P0.1 caveat. |
| **B27** | `niagara-mental-model-bloque27.md` | Network surface: port catalog (443 = `BWebService.httpsPort`, 3011/5011 daemon), `BServerPort` model. §2, §5 P1. |

---

*Consolidated 2026-06-21 from internal blocks B17/B18/B27/B75/B112. No new technical claims — every assertion is traceable to a cited block; unproven items are marked REQUIRES VALIDATION.*
