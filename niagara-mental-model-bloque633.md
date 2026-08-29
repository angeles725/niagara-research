# Niagara N4 — module-anatomy (MA5): the daemon-side module install — a signature-gated, station-stopping, overwrite-in-place stream of the jar to `$NIAGARA_HOME/modules/<name>.jar` (no backup, no atomic rename, no rollback)

**Focus**: module-anatomy · **Gap**: MA5 (daemon install command) · **Session**: 2026-08-29 · **Block**: B633
**Sources** (all `[CERT]` decompiled Java, vineflower `decompiled/` tree):
- `organized/platDaemon/platDaemon-rt/decompiled/com/tridium/platDaemon/command/BModuleInstallCommand.java`
- `organized/platform/platform-rt/decompiled/com/tridium/install/InstallScenario.java` · `.../installable/BModuleInstallable.java` · `.../installable/LocalInstallableRegistry.java` · `.../install/part/BModulePart.java` · `.../platform/SystemFilePaths.java`

**Scope**: how a module JAR physically lands in a station's `modules/` dir during a platform/software install — the step AFTER the supervisor decides what to ship ([B569], REMIT). Signing crypto = [B489]/[B492] (REMIT); this block records the install MECHANICS + the gate.

---

## 633.1 The install pipeline (plat CLI path)

`[CERT]` `BModuleInstallCommand.java:121-154` — `plat moduleinstall -h:<hostord> [module…]` drives:
```
loadPreferredVerificationMode()                    // :121 — pull the TARGET daemon's module-verify mode
new InstallScenario(... certValidator)             // dependency resolve + SIGNATURE GATE (§633.3)
BAppSurrogate.stopAllApps(session, …)              // :147 — STOP EVERY STATION on the host
scenario.commit(listener, null)                     // :148 — stream each jar to modules/
for (stopped app) app.startAppAsync()               // :152 — restart if restartEnabled
```
Two consequences visible immediately: the verify mode is the **remote target's**, not the installer's (`loadPreferredVerificationMode`), and installation **stops all stations unconditionally** before writing (`:146` "Stopping stations…"). Module install is not a hot operation.

The [B569] software-container path (`BInstallCombinedBySpecStep`) calls `InstallScenario.commit()` directly with its own stop/restart orchestration — same commit engine, different driver.

---

## 633.2 The write target and mechanism

`[CERT]` `BModulePart.java:513-517` — the destination is computed per module part:
```java
public FilePath getDestinationPath(boolean isNiagaraHomeReadonly) {
    if (!this.getSynthetic())
        return SystemFilePaths.getModulesPath(isNiagaraHomeReadonly).merge(this.getPartName() + ".jar");
    return SystemFilePaths.getModulesPath(isNiagaraHomeReadonly).merge(this.getPartName() + ".sjar");
}
```
`[CERT]` `SystemFilePaths.java:35,58-59` — `getModulesPath()` = base `.merge("modules")`, where base is `!` (`$NIAGARA_HOME`) when writable, `~` (`$NIAGARA_USER_HOME`) when `NiagaraFiles.isNiagaraHomeReadonly()`. So the jar lands at **`$NIAGARA_HOME/modules/<partName>.jar`** (`.sjar` for synthetic/scripted; user-home when NIAGARA_HOME is read-only, e.g. a hardened JACE).

The write is a **streaming file-transfer POST over the daemon session**, not a local copy: `BModulePart.makeTransferElement()` (`:737`) wraps the module in a `ModuleFileTransferElement` bound to `getDestinationPath(...)` (`:779`); `InstallScenario.commit()` bundles all elements into one `FileTransferMessage` and `DaemonFileUtil.transfer(...)` sends it. The daemon's FileTransfer handler writes the incoming stream **directly to the target path**. There is no temp-file + atomic-rename, and the daemon side shows **no backup of the prior jar and no rollback record** — an existing `<name>.jar` is overwritten in place. (One narrow exception: `:790` copies old schema-v3 modules through `getModuleCopiesPath` because their zip is rewritten in memory; the normal path is a raw stream.)

---

## 633.3 The gate: signature check BEFORE the write — as strong as `verificationMode`

`[CERT]` `InstallScenario.java` (constructor, ~:242-283) — each `BModuleInstallable` is signature-checked against the target's mode before it can enter `toInstall`:
```java
List<ModuleSignatureStatusEnum> statuses = ((BModuleInstallable)installable).getModulePart().getSignatureStatus(certValidator);
for (status : statuses) { if (status.isAcceptable(this.verificationMode)) continue;
    signatureError = true; this.signatureFailures.put(installable, statuses); break; }
if (signatureError) continue;   // dropped from toInstall, surfaces as an UnmeetableDependency
```
Code signers come from the jar via `BModulePart.initCodeSigners(JarSignatureRegistry)` ([B632]'s `META-INF/NIAGARA4.{SF,RSA}`); `getSignatureStatus` validates them with a `CertificateChainValidator` built from the target's crypto manager (`BModuleInstallCommand.java:127`).

**The gate is only as strong as `verificationMode`** `[CERT]` `InstallScenario.java:252-254`: the mode is `max(preferred, DEFAULT)` for a version-zero base, else derived from the base version, and there is a branch that sets `verificationMode = low`. Live posture is `moduleVerificationMode=low` on the audited supervisor ([B398]/[B519], REMIT) — under `low`, signature statuses that `max` would reject become `isAcceptable`, so a weakly/improperly-signed module can pass this gate. The install-time gate is real but operator-tunable down to near-nothing — the same seam [B519]/[B523] flagged for the runtime verifier, here on the install path.

---

## 633.4 Old-jar handling and the client-side store

- **Daemon side (`modules/`)**: overwrite in place, no backup (§633.2).
- **Client sw/ registry** `[CERT]` `LocalInstallableRegistry.java:546-550` — copies a source into the local `sw/` version store ONLY if not already present (`getNavChild(...)==null`); `importDirectory` (`:195-205`) explicitly `delete()`s an existing file before re-copying. So the supervisor's LOCAL installable cache dedupes/replaces, but neither side keeps a rollback copy of the module it replaced on the target.

`BModuleInstallable` is the data model behind all this: a `BInstallable` wrapping exactly one `BModulePart` (`:89`), carrying `installableFileName`/`installableFileSize`/`version`/`dependencies`, with `localInstance=true` only when the jar is physically on this host (remote proxies = false). `LocalInstallableRegistry.getInstance()` is the source of the candidate `BModuleInstallable`s the scenario resolves.

---

## 633.5 What this means for building/distributing a module

- **Installing is disruptive and irreversible-by-default.** Every station on the host is stopped, jars are overwritten in place with no backup or atomic write, then stations restart. A transfer interrupted mid-write can leave a corrupt `<name>.jar` with no automatic recovery — take your OWN backup of `modules/` before a field upgrade. This is the operational reality behind [B12]'s clean `dist`/`moduleinstall` story.
- **Your module's acceptance at install depends on the TARGET's `moduleVerificationMode`, not yours.** A correctly Honeywell/Tridium-signed jar passes anywhere; your developer-signed jar passes only where the target's mode + trust anchor accept it ([B392]/[B519]). On a `low` target the gate barely filters — a hardening item, not a distribution convenience.
- **`partName` IS the on-disk filename.** `getDestinationPath` uses `getPartName() + ".jar"`, so the `<module name="control-rt" …>` part name is literally the jar name in `modules/`. A mismatched part-name/filename is not honored — the manifest's `name` decides where it lands (consistent with [B630]: the manifest, not the filename, is authoritative).
- **User-home fallback on read-only NIAGARA_HOME** means on a locked-down controller, user modules install under `$NIAGARA_USER_HOME/modules/`, a different search root — relevant to why a module "installed" but "isn't found" on a hardened JACE.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | plat moduleinstall → loadPreferredVerificationMode → InstallScenario → stopAllApps → commit → restart | [CERT] | BModuleInstallCommand.java:121,147,148,152 | ✅ read verbatim |
| 2 | destination = getModulesPath.merge(partName+".jar"/".sjar") | [CERT] | BModulePart.java:513-517 | ✅ read verbatim |
| 3 | modules path = base.merge("modules"); base=`!`NIAGARA_HOME writable / `~`user-home readonly | [CERT] | SystemFilePaths.java:35,55,58-59 | ✅ read verbatim |
| 4 | write = streaming FileTransferMessage to dest; overwrite in place, no temp/atomic/backup on daemon | [CERT] | BModulePart.java:737,779 + InstallScenario.commit transfer | ✅ read (transfer path) |
| 5 | signature gate before toInstall: getSignatureStatus vs isAcceptable(verificationMode); fail→dropped | [CERT] | InstallScenario.java:242-283 | ✅ read |
| 6 | gate strength = verificationMode, which has a `low` branch (live posture low, B398/B519) | [CERT] | InstallScenario.java:252-254 | ✅ read verbatim |
| 7 | client sw/ registry copies only if absent; importDirectory deletes-then-copies | [CERT] | LocalInstallableRegistry.java:546-550,195-205 | ✅ read |
| 8 | BModuleInstallable = BInstallable wrapping one BModulePart; localInstance true only if jar present | [CERT] | BModuleInstallable.java:89 | ✅ read |
| 9 | no backup/rollback → interrupted write can corrupt the target jar | [INFER] from #4 | §633.2 | ✅ derived |

**Tally**: [CERT] ×8 · [INFER] ×1 · ratio 0.13 (EVIDENCE block; evidence intact). Load-bearing citations (target path, sig gate, stopAllApps, verificationMode low branch) token-checked verbatim this iteration.

## Connections

- **[B569]** — supervisor combine-transaction; its `BInstallCombinedBySpecStep` calls the same `InstallScenario.commit`. **[B632]** — the `NIAGARA4.{SF,RSA}` code signers this gate validates. **[B630]** — the `modules/` dir this writes into is what the boot scan later enumerates.
- **[B398]/[B519]/[B523]** — `moduleVerificationMode=low` live; §633.3 shows the install-time gate degrades identically. **[B392]/[B489]/[B492]** — the trust chain (REMIT).
- Forward: MA7 (module `<permissions>` → policy) and MA8 (synthesis; the "back up modules/ before upgrade" + verify-mode items become operator recs).

## Gaps uncovered

- None new investigable on disk. The daemon-side FileTransfer HANDLER (the exact bytes that write the stream to disk) is the platform daemon protocol ([B129]/[B460]/[B628] ports focus) — REMIT, not a new row. MA5 answered read-only.
