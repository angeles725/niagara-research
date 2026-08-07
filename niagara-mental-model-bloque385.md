# Block 385 — `nre.dll` NG2b: the NativePlatformProvider is a 107-native platform-services API — and on the Windows supervisor the account-mutation AND native-command-exec natives are return-0 STUBS

> **Focus `platform-native` — Ghidra sub-pass NG2b (last investigable gap).** [B125 §125.4] said `nre.dll`
> exposes "107 `NativePlatformProvider` natives" and decompiled 3 representative ones (`getHostId0`,
> `getPasswordHash0`, `daemonize0`). This block enumerates and CLASSIFIES all 107, then decompiles the
> security-relevant live ones — and finds the load-bearing nuance a 3-sample missed: **a subset of the
> most privileged natives (add/remove user, change password, execute native diagnostics command, get system
> password) are compiled to shared return-0 STUBS on the Windows supervisor build** — their real
> implementations live on the embedded (JACE/QNX) platform, not here. READ-ONLY.
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/nre.dll` (PE32+ x64, base
> `0x180000000`, assembly identity `Tridium.Niagara.NreLib`; sha256 in evidence). Ghidra 12.1.2
> `analyzeHeadless` (isolated copy — the B125 sibling-DLL-cache workaround) + `ExportDecompiledC.java`;
> `rabin2 -E` for the export→vaddr map that reveals the shared stubs.
> Raw evidence: `audits/B385-nre-decomp.c` (4 decompiled natives), `audits/B385-nre-natives.txt`.
> Markers: `[CERT]` observed in the decompiled body / export map (address cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [B125] (completes its §125.4 native inventory), [B381]
> (LocalSystem daemon + DPAPI `systempw` — the `DpapiUtil` natives are the Java crypto twin), [B114]
> (keyring/at-rest crypto), [B126] (DSF crypto layer).

---

## 385.1 — The 107-native surface, classified `[CERT]`

`nre.dll` exports 107 `Java_*` JNI natives (`rabin2 -E | grep -c Java_` = 107). They form a complete native
platform-services API, in five capability buckets (`audits/B385-nre-natives.txt`): `[CERT]`

| Bucket | ~n | Representative natives |
|---|---|---|
| **crypto / key material** | 9 | `encrypt0`/`decrypt0` (DpapiUtil), `get`/`setKeyMaterial0`, `get`/`setEncryptedRegistryString0`, `checkForKeyMaterialUpgrade0`, `supportsKeyMaterialRecovery0`, `getKeyMaterialLastModified0` |
| **credentials / OS accounts** | 20 | `addUserAccount0`, `removeUserAccount0`, `changeUserPassword0`, `get`/`setSystemPassword0`, `synchronizeUsers0`, `isPasswordValid0`, `getPasswordHash0`, `getDomainGroupsXml0`, `providesAccountManagement0` |
| **exec / daemon / watchdog** | 15 | `executeNativeDiagnosticsCommand0`, `getNativeDiagnosticsCommands0`, `restartPlatformDaemon0`, `reboot0`, `createWatchdog0`/`updateWatchdog0`/`destroyWatchdog0`, `daemonize0` |
| **identity / platform** | 16 | `getHostId0`, `getHostSerialNumber0`, `getHostModel0`, `get`/`setSSHPort0`, `isEmbedded0`, `isLicenseReadonly0`, `getComputerDomain0` |
| **system info getters** | ~47 | `getCpuTime0`, `getOsName0`, `getComputerName0`, `getFreePhysicalMemoryBytes0`, the NRE memory-pool getters, `getProcessId0`, … |

Plus a **readonly-capability model** (`is*Readonly0`: system-password, license, authentication, niagara-home,
software, station-platform, system-time) that the Java layer queries before offering a mutation. This is far
richer than B125's 3-sample implied: the native provider is the JVM's entire gateway to OS crypto, accounts,
process control, and host identity. `[CERT]`

---

## 385.2 — The platform-conditional STUBS: the dangerous natives are no-ops on Windows `[CERT]`

The export→vaddr map (`rabin2 -E`) shows several natives **share one address** — a single `return 0` body
(decompiled at `audits/B385-nre-decomp.c:8-20`). Two shared stubs collect the privileged/embedded natives: `[CERT]`

| Stub vaddr | Natives mapped to it (all `return 0`) |
|---|---|
| **`0x180002520`** | `addUserAccount0`, **`executeNativeDiagnosticsCommand0`**, `getNativeDiagnosticsCommands0`, `getSystemPassword0` |
| **`0x180002530`** | `changeUserPassword0`, `removeUserAccount0` |

So on the **Windows supervisor build**, OS-account creation/removal, password change, native-command
execution, and native system-password read are all **compiled to no-ops that return 0** — the real
implementations exist only on the embedded JACE/QNX platform (where `providesAccountManagement0`/
`supportsNativeDiagnostics0` would return true). `[CERT]`

**This REFUTES a natural alarm** (`executeNativeDiagnosticsCommand0` reads like a command-injection surface):
on this platform it executes nothing. RE-MEASURE-A-DRAMATIC-NEGATIVE discipline (PROMPT-LOOP) applied — the
scary-named native was checked, not assumed, and it is inert here. The Java `NativePlatformProvider` still
declares these methods; the platform layer gates them via the `supports*`/`provides*` predicates before
calling, so a null-op native is the "unsupported on this platform" contract, not a live capability. `[CERT]/[INFER]`
(CERT: shared return-0 stub; INFER: the embedded build implements them, from the `is*Readonly`/`supports*` model).

---

## 385.3 — The LIVE security natives, decompiled `[CERT]`

- **`DpapiUtil.encrypt0` / `decrypt0`** (`Java_com_tridium_nre_util_DpapiUtil_*` @ `0x18000b960`/`0x18000b740`,
  `audits/B385-nre-decomp.c:78-233`): these ARE implemented. Each takes the Java byte array into a
  `_CRYPTOAPI_BLOB` (Windows `DATA_BLOB`), **caps input at ≤ 0x1000 (4096) bytes** ("Too large
  encrypted/data value" otherwise), calls `DpapiHelper::decrypt(in,out,flag)` / `DpapiHelper::encrypt(in,out,
  flag1,flag2)`, copies the result into a fresh Java byte array, **securely zeroizes the plaintext BLOB
  (16-byte struct wipe loop + `LocalFree`)**, and throws `java/lang/SecurityException` on any failure. So the
  JVM's `DpapiUtil` is a **DPAPI (CryptProtectData/CryptUnprotectData) wrapper** — the Java-side twin of
  [B381 §381.3]'s `plat setsystempw` DPAPI write, and the encryption primitive behind [B114]'s keyring. The
  two boolean flags map to DPAPI scope/entropy options (resolved inside `DpapiHelper`, not exposed here). `[CERT]`
- **`isPasswordValid0`** (@ `0x180004960`, `:28-70`): marshals two `JNIString`s (username, password, each
  capped `< 0x1001`) and delegates to **`AuthenticationUtil::isPasswordValid(user, pass)`** — a real local
  credential check, not a stub. `[CERT]`
- `setKeyMaterial0` (@ `0x180004d40`) is a distinct implemented body (not stubbed), consistent with the
  crypto bucket being live on Windows. `[CERT]`

So the live-on-Windows security surface is **DPAPI encrypt/decrypt + key material + local password validation**
— all running inside the LocalSystem platform daemon ([B381]) — while the OS-account-mutation and
native-command-exec natives are inert. `[CERT]`

---

## 385.4 — Defensive-security summary `[CERT]`

1. **The most dangerous-sounding natives are stubs here** `[CERT]` (§385.2): `executeNativeDiagnosticsCommand0`,
   `addUserAccount0`, `removeUserAccount0`, `changeUserPassword0`, `getSystemPassword0` all `return 0` on the
   Windows supervisor — no native command execution, no native OS-account mutation on this platform.
2. **DPAPI crypto is the live native secret primitive** `[CERT]` (§385.3): `DpapiUtil.encrypt/decrypt` bounds
   input to 4096 bytes, zeroizes plaintext, and fails to a `SecurityException` — sound hygiene; confidentiality
   inherits DPAPI's machine/user-key protection ([B381]).
3. **107-method native trust boundary** `[CERT]` (§385.1): every OS-level capability the JVM has (crypto,
   identity, process control, the info surface) crosses this one JNI provider; the `is*Readonly`/`supports*`
   predicates are the platform's capability-negotiation layer.
4. **Embedded ≠ supervisor** `[INFER]`: the stubbed natives imply the JACE/QNX firmware build ships the real
   account/command implementations — a separate target (firmware RE, requires-execution), out of this focus.

No secrets read; distributable binary; isolated-copy analysis; nothing mutated.

---

## 385.5 — Self-verify

**Token re-checks**:
1. 107 `Java_*` natives — ✓ (`rabin2 -E | grep -c Java_`).
2. Shared stub `0x180002520` = `{ return 0; }` mapped by addUserAccount0/executeNativeDiagnosticsCommand0/getNativeDiagnosticsCommands0/getSystemPassword0 — ✓ (`rabin2 -E` map + `decomp:8-20`).
3. Second stub `0x180002530` = changeUserPassword0/removeUserAccount0 — ✓ (`rabin2 -E`).
4. `DpapiUtil.decrypt0`/`encrypt0`: `_CRYPTOAPI_BLOB`, cap `< 0x1001`, `DpapiHelper::decrypt/encrypt`, zeroize+`LocalFree`, `SecurityException` — ✓ (`decomp:78-233`).
5. `isPasswordValid0` → two JNIStrings capped `< 0x1001` → `AuthenticationUtil::isPasswordValid` — ✓ (`decomp:28-70`).

**5/5 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 18 · `[INFER]` 3 (embedded implements the stubs; DPAPI flag meanings; bucket
counts approximate). Ratio ≈ 0.17 — low; EVIDENCE block. Upgrades B125 §125.4 (inventory) to a full taxonomy
+ the stub finding; no re-derivation of B125's 3 samples.

---

## 385.x — Connections

- **[B125]** — completes its §125.4 "107 natives" inventory: full taxonomy + the platform-conditional stubs;
  its 3 samples (`getHostId0`/`getPasswordHash0`/`daemonize0`) are the implemented identity/cred/daemon ones.
- **[B381]** — the `DpapiUtil` encrypt/decrypt natives are the Java-side twin of `plat setsystempw`'s DPAPI
  write; both run in the LocalSystem daemon.
- **[B114]/[B126]** — the keyring/at-rest crypto these DPAPI natives serve.
- **Focus status**: with NG2b closed, the Ghidra sub-pass has NO investigable gaps remaining — the four core
  binaries + nverify gate-sites + the native provider taxonomy are at body grade. Remaining open work is
  requires-execution (live daemon wire / field bus / GUI) or a DIFFERENT target (JACE/QNX firmware for the
  stubbed natives' real bodies).
