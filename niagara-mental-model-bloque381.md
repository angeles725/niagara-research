# Block 381 — `plat.exe` installdaemon + setsystempw decompiled (Ghidra grade): the daemon runs as LocalSystem/auto-start, and the System Passphrase is an argv-passed, complexity-checked, DPAPI-sealed REG_BINARY under HKLM

> **Focus `platform-native` — Ghidra sub-pass NG3.** [B129] documented `plat.exe`'s six commands, its
> dynamically-loaded SCM/Crypt32 APIs, and `setsystempw → CryptProtectData → SOFTWARE\Niagara4\systempw`
> ALL FROM STRINGS/RTTI — and explicitly judged (B129 §129.7 note) that "decompilation would only add the
> exact argv-dispatch order, not load-bearing." That judgment was too conservative: decompiling the two
> commands yields concrete, load-bearing SECURITY facts strings could not — the **service privilege
> account, its start type, the passphrase complexity policy, the registry hive/value type, and that the
> passphrase travels on argv**. Those are §14-refinements to B129, not re-derivations. READ-ONLY.
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/plat.exe`
> (PE32+ x86-64, image base `0x140000000`, sha256 in evidence). Ghidra 12.1.2 `analyzeHeadless` +
> `tools/ghidra-scripts/DecompileByString.java`; `rabin2 -z` for strings.
> Raw evidence: `audits/B381-plat-decomp-bystring.txt` (25 decompiled functions), `audits/B381-plat-ghidra.txt`.
> Markers: `[CERT]` observed in the decompiled body / binary (`audits/…:line` or offset) · `[INFER]` deduction
> (Win32 constant → documented semantic).
>
> Native platform layer (Capa 25). Connects [Block 129] (refines its §129.3 — the SCM/DPAPI/registry
> operations, from strings to decompiled control flow), [Block 125] (`daemonize0` run-time half; `get/
> setSystemPassword0` Java read side), [Block 114] (the System Passphrase seed this write produces),
> [Block 126] (same `LicenseUtil`/native trust layer).

---

## 381.1 — The dynamic API resolver, decompiled — confirms B129's "all behind LoadLibrary" as `[CERT]` `[CERT]`

`FUN_140001320` (`audits/B381-plat-decomp-bystring.txt:107-189`) is the resolver B129 inferred from strings.
It `LoadLibraryA("Advapi32.dll")` (else `"SEVERE: Failed to load Advapi32.dll"`) and `GetProcAddress`es a
function-pointer struct: `[1]=CreateServiceA`, `[2]=CloseServiceHandle`, `[3]=OpenServiceA`,
`[4]=OpenSCManagerA`, `[5]=DeleteService`, `[6]=QueryServiceStatus`, `[7]=StartServiceA`, `[8]=ControlService`,
`[0]=ChangeServiceConfig2A`, `[10]=RegSetValueExA`, `[0xb]=RegCreateKeyA`, `[0xc]=RegCreateKeyExA`,
`[0xd]=RegQueryValueExA`, `[0xe]=RegDeleteKeyA`, `[0xf]=RegisterEventSourceA`, `[0x10]=ReportEventA`; then
`LoadLibraryA("Crypt32.dll")` → `[0x11]=CryptProtectData`. Every later SCM/registry/DPAPI call goes through
one of these pointers via the CFG dispatch thunk `PTR__guard_dispatch_icall_1400072f8` — which is exactly why
[B124]'s static import table showed none of them. B129's inference is now `[CERT]` from the resolver body. `[CERT]`

---

## 381.2 — `installdaemon`: the daemon is registered as a LocalSystem, auto-start service `[CERT]`

The `CreateServiceA` call (`audits/B381-plat-decomp-bystring.txt:927-963`) reveals the service's security
posture, which B129 could not read from strings. First it `OpenSCManagerA(NULL,NULL,0xf003f)`
(`SC_MANAGER_ALL_ACCESS`) and `OpenServiceA(scm,"Niagara",0xf01ff)` to test existence (proceeds on
`GetLastError()==0x424` = `ERROR_SERVICE_DOES_NOT_EXIST`). Then the `CreateServiceA` parameters, built as
the stack args at `:943-953`: `[CERT]`

| Param | Value (decompiled) | Meaning |
|---|---|---|
| lpServiceName / lpDisplayName | `"Niagara"` / `"Niagara"` | service id (matches `daemonize0`'s `SERVICE_TABLE_ENTRYA`, [B125 §125.5]) |
| dwDesiredAccess | `0xf01ff` | `SERVICE_ALL_ACCESS` |
| **dwServiceType** | `0x10` | `SERVICE_WIN32_OWN_PROCESS` |
| **dwStartType** | `2` | **`SERVICE_AUTO_START`** — starts at boot |
| dwErrorControl | `1` | `SERVICE_ERROR_NORMAL` |
| lpBinaryPathName | `niagarad.exe` (bin-dir path, `:833/846`) | the daemon EXE of [B124] |
| lpDependencies | `"FltMgr\0CryptSvc\0Tcpip"` (multi-sz, `:945` + strings) | filter-manager, crypto, TCP/IP |
| **lpServiceStartName** | `NULL` (`:947-948`) | **runs as `LocalSystem`** (Win32: NULL start-name ⇒ LocalSystem) |
| lpPassword | `NULL` | consistent with LocalSystem |

So the platform daemon is a **boot-auto-started, LocalSystem (highest-privilege) own-process Windows
service** — the concrete privilege facts B129's string-grade pass could not state. `[CERT]` on the constants;
`[INFER]` on the Win32 semantic (NULL start-name ⇒ LocalSystem; `2` ⇒ auto-start — well-known documented
constants). After creation, if a verbose flag is set it calls `ChangeServiceConfig2A(svc, 1
/*SERVICE_CONFIG_DESCRIPTION*/, "Platform management service for Niagara tools")` (`:958-960`) — the service
description, another new string. It also retries on `GetLastError()==0x430` (`ERROR_SERVICE_MARKED_FOR_DELETE`)
with `Sleep(1000)` (`:964-965`), and registers an EventLog source under
`HKLM\SYSTEM\CurrentControlSet\Services\EventLog\Application\Niagara` with `EventMessageFile` +
`TypesSupported=7` (`:969-996`). `[CERT]`

---

## 381.3 — `setsystempw`: passphrase on argv, a native complexity policy, DPAPI, REG_BINARY under HKLM `[CERT]`

`FUN_140002830` (`audits/B381-plat-decomp-bystring.txt:339-484`) is the System Passphrase setter. New facts
beyond B129 §129.3: `[CERT]`

- **The passphrase is `argv[2]`** (`pbVar2 = *(byte**)(param_3+0x10)`, `:374`) — `plat setsystempw newpass`
  takes the secret **on the command line**, so it is exposed to the process table, shell history, and any
  audit of `plat` invocations. (SECRETS DISCIPLINE: structure only — no value handled.) `[CERT]`
- **A native complexity policy is enforced before storing** (`:410-437`): the setter counts character
  classes with `isalpha`/`isupper`/`isdigit` and requires **length > 9 (i.e. ≥10) AND ≥1 uppercase AND ≥1
  lowercase AND ≥1 digit** (`if ((9 < len) && digits && lowercase && uppercase)`). A passphrase failing any
  of these is silently NOT written. This password policy lives in the native binary, not the Java layer —
  new, and operationally load-bearing. `[CERT]`
- **`/check` mode**: if `argv[2]` is `"/check"` (`:399`) it only opens `HKLM\SOFTWARE\Niagara4`
  (`RegOpenKeyExA(0x80000002, "SOFTWARE\\Niagara4", …)`, `:384-385`) and `RegQueryValueExA(…, "systempw", …)`,
  returning "set" iff the value exists (`iVar3==2` = `ERROR_FILE_NOT_FOUND` ⇒ not set, `:390-393`). `[CERT]`
- **Storage** (`:443-460`): opens `HKLM\SOFTWARE\Niagara4` with `0xf013f` (`KEY_ALL_ACCESS`), calls
  `CryptProtectData` on the passphrase blob **with the optional-entropy `DATA_BLOB` argument NULL** (`:453` —
  no app-supplied entropy; the blob is bound to the DPAPI master key only), then
  `RegSetValueExA(key, "systempw", 0, 3 /*REG_BINARY*/, blob, len)` (`:457`) and `LocalFree`s the blob. So
  the on-disk secret is a **DPAPI-sealed `REG_BINARY` value `systempw` under `HKLM\SOFTWARE\Niagara4`** —
  hive, value type, and DPAPI-no-entropy all now `[CERT]` (B129 had the key path from strings but not the
  hive/type/entropy). `[CERT]`

This is the native write path for the seed [B114] identified as protecting keyring-sealed BOG secrets, and
the write side of [B125 §125.4]'s `get/setSystemPassword0` JNI natives. `[CERT]`

---

## 381.4 — Defensive-security summary `[CERT]`

1. **Daemon = LocalSystem + auto-start** `[CERT]` (§381.2): the platform service runs at the highest OS
   privilege and starts at boot; anything that compromises `niagarad.exe` or its load path inherits
   LocalSystem. Dependencies `FltMgr/CryptSvc/Tcpip` gate its start ordering.
2. **System Passphrase passed on argv** `[CERT]` (§381.3): `plat setsystempw <newpass>` exposes the secret
   to the process command line — a bootstrap/CLI exposure surface (mitigated only by who can run `plat`).
3. **Native complexity policy** `[CERT]` (§381.3): ≥10 chars, upper+lower+digit — enforced in the binary,
   so it holds even when the Java layer is bypassed; a weak passphrase is silently rejected (no write).
4. **DPAPI without app entropy** `[CERT]` (§381.3): the sealed blob is bound to the DPAPI master key with NO
   secondary app entropy, so its confidentiality rests entirely on machine/DPAPI protection of
   `HKLM\SOFTWARE\Niagara4\systempw` (REG_BINARY). Ties to [B114]'s at-rest model.
5. **All privileged APIs are LoadLibrary/GetProcAddress-resolved** `[CERT]` (§381.1) — no static import to
   hook; a monitor keying on the import table would miss every SCM/DPAPI/registry call.

No secret VALUES read or written; analysis on an isolated copy of a distributable binary; nothing mutated.

---

## 381.5 — Self-verify

**Token re-checks** (load-bearing `[CERT]`):
1. Resolver `FUN_140001320`: `LoadLibraryA("Advapi32.dll")`/`("Crypt32.dll")` + `GetProcAddress` of CreateServiceA/RegSetValueExA/CryptProtectData into a pointer struct — ✓ (`decomp:148-185`).
2. `OpenSCManagerA(0,0,0xf003f)` + `OpenServiceA(scm,"Niagara",0xf01ff)` + create-on-`0x424` — ✓ (`decomp:927-931`).
3. CreateServiceA params: dwServiceType `0x10`, dwStartType `2` (auto), dwErrorControl `1`, lpServiceStartName `NULL`, deps `FltMgr\0CryptSvc\0Tcpip` — ✓ (`decomp:945-953`; `Tcpip`/`Platform management service` strings @ `0x140007e18`/`0x140007e20`).
4. `ChangeServiceConfig2A(svc,1,"Platform management service for Niagara tools")` — ✓ (`decomp:958-960`).
5. `setsystempw` passphrase = `argv[2]` (`param_3+0x10`) — ✓ (`decomp:374`).
6. Complexity policy `9 < len && digit && lowercase && uppercase` via isalpha/isupper/isdigit — ✓ (`decomp:410-437`).
7. `/check` → `RegOpenKeyExA(0x80000002,"SOFTWARE\\Niagara4")` + `RegQueryValueExA("systempw")`, not-set on `==2` — ✓ (`decomp:384-393`).
8. Store: `KEY_ALL_ACCESS 0xf013f` open → `CryptProtectData` (entropy blob NULL) → `RegSetValueExA("systempw",0,3=REG_BINARY,...)` — ✓ (`decomp:443-457`).
9. HKLM hive constant `0x80000002` = HKEY_LOCAL_MACHINE — ✓ (`decomp:385/444`).

**9/9 load-bearing tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 20 · `[INFER]` 2 (the two Win32-constant→semantic reads: NULL start-name ⇒
LocalSystem, `2` ⇒ auto-start). Ratio ≈ 0.10 — low; EVIDENCE block over decompiled bodies. This does NOT
re-derive B129 — the command set/verbs/dynamic-load FACT are remitted; the privilege account, start type,
complexity policy, hive/value-type, DPAPI-entropy, and argv-passing are new.

---

## 381.x — Connections

- **[Block 129]** — *refines §129.3 (§14).* B129 read `plat.exe` from strings/RTTI and explicitly deferred
  decompilation as "not load-bearing"; B381 shows it WAS — the LocalSystem account, auto-start, native
  password policy, REG_BINARY/HKLM, DPAPI-no-entropy, and argv-passed passphrase are only visible in the
  decompiled bodies. The command set and dynamic-load fact are REMITTED to B129.
- **[Block 125]** — `daemonize0`→`StartServiceCtrlDispatcherA("Niagara")` is the run-time half of the service
  §381.2 registers; `get/setSystemPassword0` is the Java read side of §381.3's DPAPI write.
- **[Block 114]** — the System Passphrase written here is the seed protecting keyring-sealed BOG secrets.
- **[Block 124]** — `niagarad.exe` (the service binary) and the boot path.
- **Forward — reopened focus**: NG4 (`libciper.so` QNX-ARM Sylk bodies), NG1-G1 (nverify `skip-*` gate
  sites), NG2b (nre.dll native bodies). On focus exhaustion: a Ghidra-sub-pass synthesis block + §18 retro.
