# Block 125 — The native↔Java JNI bridge (njre/nre embed the JVM · NativePlatformProvider natives · common.dll marshalling)

> Research of the **Niagara N4 NATIVE↔Java JNI bridge** on the installed OptimizerSupervisor‑N4.14.0.162: HOW the native launcher embeds the JVM (the exact `buildArgs → JNI_CreateJavaVM → FindClass → GetStaticMethodID → NewObjectArray/NewStringUTF → CallStaticVoidMethod → exception‑check` control flow), HOW the ~107 `NativePlatformProvider` JNI natives in `nre.dll` are bound to Java and how a representative native actually bridges into the C++ platform code, and WHAT role `common.dll` plays as the shared C++ platform + JNI‑marshalling library. This goes deeper than [Block 124] (which mapped the boot path at imports/exports/strings grade) by adding **decompiler‑grade control flow** (Ghidra 12.1 headless + r2 disasm).
>
> Sources (primary, READ‑ONLY): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{njre.dll, nre.dll, common.dll}` (PE32+ x86‑64, baddr `0x180000000`, all Authenticode‑`signed true`, compiled Mon Jan 8 2024).
> sha256: njre `7007ff82…`, nre `606ff1c6…`, common `3e997b0d…`.
> Method: Ghidra 12.1 `analyzeHeadless` + a decompile post‑script (`DecompList.java`); radare2 6.1.6 (`pdf`, vtable‑offset mapping); `rabin2 -I/-i/-E/-l`; `strings`. Raw evidence preserved at:
> `…/audits/B125-info.txt` (identity/sign), `…/audits/B125-exports.txt`, `…/audits/B125-imports.txt`,
> `…/audits/B125-jni-natives.txt` (the 107‑name inventory), `…/audits/B125-jni-strings.txt`,
> `…/audits/B125-ghidra-njre.txt` (decompiled `buildArgs`/`createVM`/`invokeJava`),
> `…/audits/B125-ghidra-nre.txt` (decompiled `getHostId0`/`getPasswordHash0`/`daemonize0`),
> `…/audits/B125-jnienv-offsets.txt` (the verified JNIEnv vtable‑offset → JNI‑function map),
> `…/audits/B125-r2-njre-jvm.txt`, `…/audits/B125-r2-nre-getHostId0.txt`.
> Markers: `[CERT]` observed in the binary (tool command + symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (boot path — this block supplies the JNI‑bridge depth it deferred as gap N2), [Block 26] (NRE launcher conceptual), [Block 2] (HostId), [Block 114] (key material / DPAPI), [Block 10] (daemon lifecycle), [Block 18] (signing).

---

## 125.1 — The three native libraries and their division of labour `[CERT]`

| DLL | Win32 assembly identity (`strings`) | Role in the bridge |
|---|---|---|
| **njre.dll** | `Tridium.Niagara.NJreLib 4.14.0.22` — *"Niagara JRE Wrapper"* | **Embeds** the JVM: `JavaLauncherWin32` drives `JNI_CreateJavaVM` and the `main([Ljava/lang/String;)V` invocation. Has NO `NativePlatformProvider` natives. |
| **nre.dll** | `Tridium.Niagara.NreLib 4.14.0.22` | Superset launcher (`NreLauncherWin32`, same embedding code) **plus** the **107 exported `Java_com_tridium_*` JNI natives** the platform provider calls back into. |
| **common.dll** | `Tridium.Niagara.CommonLib 4.14.0.22` — *"Niagara Common Native Library"* | The shared C++ platform library + the **JNI marshalling layer** (`JNIString`, `JavaExceptionHandler`, `JavaExceptionFactory`, `JNISystemOut`). nre.dll's natives delegate the real work here. |

Evidence: `rabin2 -I` (B125-info.txt) for the three PE headers; assembly‑identity `<description>` strings in each DLL (B125-jni-strings.txt and direct `strings common.dll`). `[CERT]`

There are **two layers in the bridge**, not one `[CERT]`:
1. **Java → native (downcall)**: the JVM resolves a Java `native` method to an exported `Java_…` symbol in `nre.dll` and calls it with a `JNIEnv*`.
2. **native → Java (upcall)** and **native → C++ platform**: the `Java_…` native uses the `JNIEnv*` (and `common.dll`'s `JNIString`/`JavaExceptionHandler`) to read its arguments, call into `common.dll`'s C++ platform classes (`AuthenticationUtil`, `PerfUtil`, `TcpIp*`, `EngineWatchdog`, `Nre`…), and wrap the result back into a `jstring`/throw a Java exception.

---

## 125.2 — JVM embedding control flow, decompiled (njre `JavaLauncherWin32`) `[CERT]`

The B124 boot trace (`buildArgs()→createVM()→invokeJava()`) is now grounded in the actual decompiled bodies (Ghidra 12.1, B125-ghidra-njre.txt). The launcher keeps the VM state in three globals: `DAT_18000f3a0` = `JavaVM*`, `DAT_18000f3a8` = `JNIEnv*`, `DAT_18000f390` = `JavaVMOption[]` array (stride `0x10` = `sizeof(JavaVMOption){char* optionString; void* extraInfo}`), with `DAT_18000f3b0` = option count and `DAT_18000f3b4` = program‑arg count.

### `buildArgs(argc, argv)` (`?buildArgs@JavaLauncherWin32@@…`, 0x180002b10) `[CERT]`
Zeroes a 64‑slot `JavaVMOption[]`, splits incoming argv (tokens prefixed `-@` are rewritten to `--` and routed to the option array; everything else becomes a program arg), calls `buildVMOptions(...)`, then `_strdup`s a fixed set of options into the array **in this verbatim order** (B125-ghidra-njre.txt:421‑488):

```
-Djava.library.path=%s
-Djava.class.path=%s
-Djava.security.properties==%s\bin\policy\java.security
-Djava.security.manager
-Dniagara.home=%s
-Dniagara.home.url=%s                (built via FileUtil::pathToUrl)
-Dniagara.user.home=%s
-Dniagara.platform.provider=%s       (value = const "com.tridium.nre.platform.NativePlatformProviderTridium")
-Dniagara.supported.runtime.profiles=%s
-Dniagara.required.runtime.profiles=%s
"exit"                               (JavaVMOption with extraInfo = FUN_180004730  → the VM "exit" hook)
```

Two findings here that [Block 124] could not see at strings grade `[CERT]`:
- **`-Dniagara.platform.provider`'s value is a hard‑coded const string `com.tridium.nre.platform.NativePlatformProviderTridium`** (decompiled arg at 0x18000a088; confirmed `r2 … ps` → that exact string, B125-ghidra-njre.txt:466 + offset dump). This system property is what tells the Java runtime which class implements the platform provider — and **that class is exactly the one whose `native` methods bind to nre.dll's 107 `Java_com_tridium_nre_platform_NativePlatformProvider*` exports** (§125.4). This is the literal name of the Java↔native contract.
- The launcher installs a **JavaVMOption "exit" hook** (`extraInfo = FUN_180004730`): the VM's abort/exit path is routed back into native code, so a fatal JVM exit is handled by the launcher rather than terminating silently. `[CERT]`

### `createVM()` (`?createVM@JavaLauncherWin32@@…`, 0x1800034d0) `[CERT]`
1. Logs the option list (when `java_debug`).
2. **License gate**: scans every option for `javaagent` / `agentpath` / `agentlib`; if any is present it requires `LicenseUtil::isFeaturePresent("Tridium","developer")`, otherwise prints `FATAL: Can not use Java agent argument '%s' without a '%s' feature in '%s' license.` and aborts. So **attaching a JVMTI/Java agent to a Niagara JVM is license‑gated at the native launcher** — a defensive control invisible to Java (B125-ghidra-njre.txt:563‑580). `[CERT]`
3. Builds a `JavaVMInitArgs` on the stack: `version = 0x00010008` (verbatim `mov dword [rsp+0x30], 0x10008` — **`JNI_VERSION_1_8`**), `nOptions = DAT_18000f3b0`, `options = DAT_18000f390`, `ignoreUnrecognized = 0`.
4. Calls **`JNI_CreateJavaVM(&pvm, &penv, &initArgs)`** through the Control‑Flow‑Guard dispatch thunk `call qword [0x180009398]` (the resolved function pointer, obtained earlier by `GetProcAddress` in `loadDLL`, sits in `rax`; the CFG thunk at `0x180008af0` is a bare `jmp rax`). On failure: `Error: CreateJavaVM failed %d`. `[CERT]`

> **Why no JNI symbols appear in the import table** `[CERT]/[INFER]`: `JNI_CreateJavaVM` is resolved dynamically from `jvm.dll` via `GetProcAddress` (so it is NOT in `rabin2 -i`), and every `JNIEnv`/`JavaVM` call is an indirect call through the env/vm function table. All such indirect calls go through the MSVC **Control Flow Guard** dispatcher (`__guard_dispatch_icall`, `[0x180009398]→0x180008af0: jmp rax`). This is why disasm shows `call qword [0x180009398]` everywhere instead of named imports.

### `invokeJava(mainClass)` (`?invokeJava@JavaLauncherWin32@@…`, 0x1800041a0) `[CERT]`
Decompiled flow + the **verified JNIEnv vtable offsets** (each `mov rax,[env+0xNN]; CFG‑call`; offset/8 = JNI function‑table index; B125-jnienv-offsets.txt):

| Step (debug string) | JNIEnv offset | Index | JNI function |
|---|---|---|---|
| `java> Find main class...` | `0x30` | 6 | **FindClass**(mainClass) |
| (check) | `0x78` | 15 | ExceptionOccurred |
| `java> Find main method...` | `0x388` | 113 | **GetStaticMethodID**(cls,"main","([Ljava/lang/String;)V") |
| (check / report) | `0x78` / `0x80` | 15 / 16 | ExceptionOccurred / ExceptionDescribe |
| build args | `0x30` | 6 | FindClass("java/lang/String") |
| | `0x560` | 172 | **NewObjectArray**(argc, String.class, null) |
| per‑arg loop | `0x538` | 167 | **NewStringUTF**(arg) |
| per‑arg loop | `0x570` | 174 | **SetObjectArrayElement**(arr, i, jstr) |
| `java> Launching main method...` | `0x470` (in `FUN_180002ae0`) | 142 | **CallStaticVoidMethodV**(cls, mid, args) |
| post‑call | `0x78` / `0x80` | 15 / 16 | ExceptionOccurred / ExceptionDescribe |

The method signature string `([Ljava/lang/String;)V` is the literal arg to GetStaticMethodID (B125-ghidra-njre.txt:633). On a `null` MethodID → `Error: Can't find main method in class "%s"`; on missing class → `Error: Can't find main class "%s"`. After `CallStaticVoidMethodV` returns, an exception check decides the return code. `[CERT]`

> **Correction/upgrade vs [Block 124]:** B124 listed the `java>` trace order as a *strings* sequence and marked the JNI call list as plausible. B125 confirms the exact JNI calls **and their order** from the decompiled body and the `JNIEnv` vtable offsets (FindClass@0x30, GetStaticMethodID@0x388, NewObjectArray@0x560, NewStringUTF@0x538, SetObjectArrayElement@0x570, CallStaticVoidMethodV@0x470). The JVM target is **JNI 1.8** (`0x10008`). `[CERT]`

The teardown (`java> Detach current thread… / Destroy VM…`, error `Could not detach main thread`) lives in the launcher's outer `java()`/`cleanup()` (strings in B125-jni-strings.txt) → `DetachCurrentThread` (on `JavaVM`) + `DestroyJavaVM`. `[CERT]` (strings) / `[INFER]` (exact offsets not re‑decompiled this iteration).

---

## 125.3 — How the natives are bound: JNI name‑mangling, NOT RegisterNatives `[CERT]`

`nre.dll` **exports 107 `Java_…`‑mangled symbols** (`rabin2 -E`, B125-exports.txt → B125-jni-natives.txt):

- `RegisterNatives` does **not** appear in any import/export of njre/nre/common (`grep -i RegisterNatives` over B125-imports/exports = 0 hits). `[CERT]`
- Therefore the binding is the **standard JNI "discovery by mangled name"** mechanism: the JVM, when a `com.tridium.nre.platform.NativePlatformProvider[.Tridium]` Java `native` method first runs, looks up the exported symbol `Java_<fqcn‑with‑underscores>_<method>` in the loaded native libraries and links it. `[CERT]/[INFER]` (CERT: the exports exist and are exactly the mangled names; INFER: that the JVM resolves them by name rather than an unseen `RegisterNatives`, which is the only mechanism consistent with exporting all 107).

The mangling is verbatim, e.g. Java `com.tridium.nre.platform.NativePlatformProvider.getHostId0()` → export `Java_com_tridium_nre_platform_NativePlatformProvider_getHostId0`; the daemon‑only subclass method `…NativePlatformProviderTridium.daemonize0()` → `Java_com_tridium_nre_platform_NativePlatformProviderTridium_daemonize0`. The trailing `0` is Tridium's convention (the Java side wraps each `native xxx0()` in a public `xxx()`). `[CERT]`

**Library load model** `[INFER]` (consistent with [Block 124]): for `station.exe`/`nre.exe`, `nre.dll` IS the launcher and is already in‑process, so its `Java_…` exports are visible to the JVM directly. For `niagarad.exe`, the JVM is created by `njre.dll` and the Java `NiagaraDaemon` then loads `nre.dll` as a JNI library (via `java.library.path`); both routes converge on the same 107 exported natives.

---

## 125.4 — The NativePlatformProvider JNI native inventory (107) `[CERT]`

Breakdown by exporting class (`rabin2 -E`, B125-jni-natives.txt): **102** `Java_…NativePlatformProvider_*` + **1** `…NativePlatformProviderTridium_daemonize0` + **2** `…RegistryUtil_*` + **2** `…DpapiUtil_*` = **107**. Grouped by what each bridges (every name is `[CERT]` from the export table; the C++ target class is `[CERT]` where the decompile/imports show it, else `[INFER]` from the symbol name + common.dll's export set):

| Group | Natives | Bridges to (common.dll / Win32) |
|---|---|---|
| **Daemon / service lifecycle** | `daemonize0`(Tridium), `platformDaemonShutdownRequested0`, `allowPlatformDaemonRestart0`, `restartPlatformDaemon0`, `notifyApplicationStatus0`, `isDaemonDebugSupported0` | SCM via `StartServiceCtrlDispatcherA` + `SystemLog` (see §125.5) |
| **Watchdog** | `createWatchdog0`, `destroyWatchdog0`, `updateWatchdog0`, `getWatchdogTimeout0`, `getWatchdogPolicy0`, `getWatchdogCycles0` | `common.dll EngineWatchdog::*` |
| **HostId / host identity** | `getHostId0`, `getHostFileName0`, `getHostModel0`, `getHostModelVersion0`, `getHostParts0`, `getHostProduct0`, `getHostSerialNumber0`, `getHostVendor0`, `getComputerName0`, `getComputerDomain0`, `getProcessId0` | `Nre`/`common.dll PlatformInfo`/`AuthenticationUtil` ([Block 2]) |
| **OS info** | `getOsArchitecture0`, `getOsDescription0`, `getOsName0`, `getOsVersion0`, `isOsInstallable0`, `isEmbedded0` | `common.dll PlatformInfo` |
| **Account / auth mgmt** | `addUserAccount0`, `removeUserAccount0`, `addUserToGroup0`, `removeUserFromGroup0`, `changeUserPassword0`, `isGroupMember0`, `getAccountXml0`, `getDomainGroupsXml0`, `getDefaultAdminGroupName0`, `getDefaultUsername0`, `getDefaultPassword0`, `getPasswordHash0`, `isPasswordValid0`, `synchronizeUsers0`, `providesAccountManagement0`, `isAuthenticationReadonly0`, `getSupportedAuthenticationTypes0`, `getIdFromName0`, `getNameFromId0` | `common.dll AuthenticationUtil`/`Win32AuthUtil`/`WinNtAccount` → Win32 `LogonUserW`/`NetUserGetGroups`/`LookupAccountNameW` |
| **Key material / crypto** | `getKeyMaterial0`, `setKeyMaterial0`, `getKeyMaterialLastModified0`, `checkForKeyMaterialUpgrade0`, `supportsKeyMaterialRecovery0`, + `DpapiUtil_encrypt0/decrypt0`, + `RegistryUtil_getEncryptedRegistryString0/setEncryptedRegistryString0` | DPAPI (`CryptProtect/UnprotectData`) + registry ([Block 114]) |
| **System password** | `getSystemPassword0`, `setSystemPassword0`, `isSystemPasswordReadonly0` | registry/DPAPI |
| **Perf / memory / CPU** | `getCpuTime0`, `getCurrentCPUUtilization0`, `getOverallCPUUtilization0`, `getIdleTime0`, `getFreePhysicalMemoryBytes0`, `getTotalPhysicalMemoryBytes0`, `getNanoCount0`, `getTickCount0`, `getNRE{CodeCache,Heap,MetaSpace,RamDisk,SystemReserve}MemoryPool0` | `common.dll PerfUtil` → `GlobalMemoryStatusEx`/`GetSystemTimes`/`QueryPerformanceCounter` |
| **Filesystem** | `getAllFileSystemNames0`, `getFileSystemDisplayName0`, `getCurrentFileCount0`, `getMaxFileCount0`, `getCurrentOpenFileDescriptorCount0`, `getMaxOpenFileDescriptorCount0`, `getArchiveBackupCount0` | `common.dll FileSystemInfo` |
| **Network** | `getNetworkSettingsXML0`, `setNetworkSettingsXML0`, `usesPosixSockets0` | `common.dll TcpIpAdapterSettings`/`TcpIpHostSettings`/`TcpIpUtil`/`IpHelperWin32` |
| **Time** | `setNativeTimeZone0`, `setSystemTime0`, `isSystemTimeReadonly0` | `common.dll TimeUtil` |
| **SSH** | `getSSHPort0`, `setSSHPort0`, `isSSHSupported0` | registry config |
| **System logging** | `log0`, `enableSystemLogging0`, `readSystemLog0`, `canReadSystemLogMessages0`, `canWriteSystemLogMessages0` | `common.dll SystemLog` → `RegisterEventSourceA` (Windows Event Log) |
| **Diagnostics** | `dumpThreads0`, `executeNativeDiagnosticsCommand0`, `getNativeDiagnosticsCommands0`, `supportsNativeDiagnostics0` | `common.dll` + process APIs |
| **Platform policy / read‑only flags + misc** | `isLicenseReadonly0`, `isNiagaraHomeReadonly0`, `isSoftwareReadonly0`, `isStationPlatformReadonly0`, `requireSecurePlatform0`, `supportsNREConfiguration0`, `getAllowBrandChangeDefault0`, `getAllowStationRestartDefault0`, `reboot0` | `common.dll PlatformInfo`/`PlatformUtil` |

This is the full native surface a platform connection drives: the Workbench Platform tool's "User Accounts", "TCP/IP Configuration", "System Passphrase", "Application Director", "Lexicon"/diagnostics, etc., all bottom out in these 107 natives → `common.dll` C++ → Win32. `[INFER]` (mapping of UI features) / `[CERT]` (the native set).

---

## 125.5 — How a native actually bridges (3 decompiled natives) `[CERT]`

Three representative natives, decompiled from `nre.dll` (Ghidra, B125-ghidra-nre.txt), show the canonical bridge pattern. JNI calls are again CFG‑dispatched (`PTR__guard_dispatch_icall_18000e760`); `[env+0x538]` = `NewStringUTF` (index 167, verified).

**`getHostId0(JNIEnv* env, jobject)`** (0x180003380) — *value out* `[CERT]`:
```
Nre *p = Nre::getInstance();
(env‑guarded) p->getHostId(buf, 0x40);      // virtual call: fill 64‑byte buffer from common/Nre
return env->NewStringUTF(buf);              // [env+0x538], wrap native string → jstring
```
The native is a thin shim: get singleton → call C++ accessor → wrap result via `NewStringUTF`. (r2 confirms `Nre::getInstance` + `mov rax,[rax+0x538]` for NewStringUTF, B125-r2-nre-getHostId0.txt.)

**`getPasswordHash0(JNIEnv* env, jobject, jstring s)`** (0x180004340) — *string in + out, with marshalling* `[CERT]`:
```
if (s && env->GetStringUTFLength(s) < 0x1001) {     // length sanity cap (4096)
  JNIString js(env, s);                              // common.dll: jstring → native char* (GetStringUTFChars)
  char* h = AuthenticationUtil::getPasswordHash(js); // common.dll C++ does the real work
  if (h) { jstring r = env->NewStringUTF(h); free(h); }
  ~JNIString();                                       // release the native chars
}
```
This is the textbook bridge: **`common.dll::JNIString` marshals the `jstring` argument to `char*`, `common.dll::AuthenticationUtil` performs the platform operation, and `env->NewStringUTF` marshals the result back** — with a defensive 4096‑char input cap. (B125-ghidra-nre.txt:224‑261.) `[CERT]`

**`daemonize0(JNIEnv* env)`** (0x1800057e0) — *confirms B124's [INFER] daemon model* `[CERT]`:
```
JavaExceptionHandler eh(env);                                   // common.dll, bound to env
SystemLog::getInstance(eh); SystemLog::setLogFilterLevel(1);    // common.dll SystemLog
PlatformUtil::createThread(FUN_1800053e0, NULL, "Win32ServiceThread");  // spawn SCM thread
PlatformUtil::sleep(3000);                                      // wait up to 3s
if (serviceStartedFlag == 0) { SystemLog::log(3,"niagarad: Niagara service failed to daemonize."); return JNI_FALSE; }
else { SystemLog::log(1,"niagarad: Niagara service successfully daemonized, startup can continue.");
       SetConsoleCtrlHandler(FUN_180008b70, 1); return JNI_TRUE; }
// eh checked at end → propagates any pending Java exception
```
and `FUN_1800053e0` ("Win32ServiceThread" routine) builds a `SERVICE_TABLE_ENTRYA` named **`"Niagara"`** and calls **`StartServiceCtrlDispatcherA`** (r2: `lea rax, str.Niagara` → `call qword [ADVAPI32.dll_StartServiceCtrlDispatcherA]`). This **upgrades [Block 124 §124.3]'s `[INFER]`** ("NiagaraDaemon Java loads nre.dll → daemonize0 runs the SCM dispatcher") to **`[CERT]`**: `daemonize0` IS the JNI native that starts the Windows service, on a dedicated thread, returning a `jboolean` derived from a flag the service thread sets within 3 s, and using `common.dll`'s `SystemLog`/`JavaExceptionHandler` to report. (B125-ghidra-nre.txt:266‑318.) `[CERT]`

---

## 125.6 — common.dll: the shared C++ platform library AND the JNI marshalling layer `[CERT]`

`common.dll` ("Niagara Common Native Library 4.14.0.22") is imported by `nre.dll` (~106 symbols per [Block 124]; **35** of them are JNI/marshalling helpers, `grep` over B125-imports.txt). Its exports (`rabin2 -E`, top classes by count): `XElem`(57)/`XWriter`(18)/`XParser`(8)/`ListMap`/`KeyedList` (an XML+collections toolkit — this is how `getAccountXml0`/`getNetworkSettingsXML0`/`getDomainGroupsXml0` build their XML payloads), `TcpIpAdapterSettings`(49)/`TcpIpHostSettings`(24)/`IpHelperWin32`(14)/`TcpIpUtil` (network config), `AuthenticationUtil`(11)/`Win32AuthUtil`(8)/`WinNtAccount`(8) (accounts/auth), `PlatformInfo`(12)/`PlatformUtil`(9) (host/OS/threads/reboot), `EngineWatchdog`(12), `PerfUtil`(7), `FileSystemInfo`(7), `SystemLog`(4)/`Log`(3)/`MessageBundle`(3), `TimeUtil`(3), `RegUtil`(4). `[CERT]`

Its **JNI‑specific surface** (the marshalling layer that makes the bridge ergonomic) `[CERT]`:
- **`JNIString`** — ctor `JNIString(JNIEnv*, jstring)` + `catUTF/catUnicode/catWUnicode/copyUTF/copyUnicode/copyWUnicode(char*/wchar*, JNIEnv*, jstring)`: bidirectional `jstring ↔ native UTF/UTF‑16` conversion (the helper used in `getPasswordHash0`).
- **`JavaExceptionHandler(JNIEnv*)`** — `error(MessageBundle*)` / `clearError()`: lets native code raise/clear Java exceptions tied to the current `JNIEnv` (used in `daemonize0`).
- **`JavaExceptionFactory::throwRuntimeException(JNIEnv*, MessageBundle*)`** — throw a `java.lang.RuntimeException` from native, with a localized message bundle.
- **`JNISystemOut(JNIEnv*)`** — bridge native stdout to Java `System.out`.

Its imports show it bottoms out on Win32: `advapi32`(LogonUserW/LookupAccountName/Sid/Reg*/RegisterEventSourceA), `netapi32`(NetUserGetGroups/LocalGroups/DsRoleGetPrimaryDomainInformation), `ws2_32`, `kernel32`(GlobalMemoryStatusEx/GetSystemTimes/GetProcessTimes/QueryPerformanceCounter/Process32First/Next/OpenProcess/TerminateProcess). `[CERT]` (B125-imports.txt).

So the **layering is three‑deep** `[CERT]`: Java `NativePlatformProviderTridium` → `nre.dll` `Java_…` native (JNI glue, marshalling via `common.dll::JNIString`) → `common.dll` C++ platform class (`AuthenticationUtil`, `PerfUtil`, `TcpIp*`, `EngineWatchdog`, `PlatformInfo`…) → Win32 API. `common.dll` is BOTH the JNI marshalling library and the OS‑abstraction library; the `nre.dll` natives are intentionally thin.

---

## 125.7 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool against the binary):
1. `nre.dll` exports exactly **107** `Java_com_tridium_*` symbols (`rabin2 -E | grep -c`) — ✓ (B125-exports.txt; split 102+1+2+2 per B125-jni-natives.txt).
2. **0** occurrences of `RegisterNatives` in any import/export of the three DLLs (`grep -i`) — ✓ (binding is by mangled export name).
3. `buildArgs` injects `-Dniagara.platform.provider` whose value const at `0x18000a088` = `com.tridium.nre.platform.NativePlatformProviderTridium` (`r2 … s 0x18000a088; ps`) — ✓.
4. `createVM` sets `JavaVMInitArgs.version = 0x10008` (`mov dword [rsp+0x30], 0x10008`) = JNI_VERSION_1_8 — ✓ (B125-r2-njre-jvm.txt / ghidra).
5. `createVM` calls `JNI_CreateJavaVM` via CFG dispatch `[0x180009398]→0x180008af0: jmp rax` — ✓.
6. `createVM` license gate strings `javaagent`/`agentpath`/`agentlib` + `LicenseUtil::isFeaturePresent("Tridium","developer")` + FATAL string — ✓ (B125-ghidra-njre.txt:563‑575).
7. `invokeJava` JNIEnv offsets: FindClass `0x30`(6), GetStaticMethodID `0x388`(113), NewObjectArray `0x560`(172), NewStringUTF `0x538`(167), SetObjectArrayElement `0x570`(174), ExceptionOccurred `0x78`(15), ExceptionDescribe `0x80`(16) — ✓ (B125-jnienv-offsets.txt; offsets/8 = standard JNI indices).
8. `invokeJava` GetStaticMethodID signature literal `([Ljava/lang/String;)V` — ✓.
9. CallStaticVoidMethod family at `[env+0x470]` (index 142, CallStaticVoidMethodV) in `FUN_180002ae0` ("Launching main method") — ✓.
10. `getHostId0` = `Nre::getInstance()` → fill 0x40 buffer → `NewStringUTF` (`[env+0x538]`) — ✓ (B125-ghidra-nre.txt:214‑217 + r2).
11. `getPasswordHash0` = `GetStringUTFLength` cap `0x1001` → `JNIString(env,s)` → `AuthenticationUtil::getPasswordHash` → `NewStringUTF` → `free` → `~JNIString` — ✓ (B125-ghidra-nre.txt:240‑257).
12. `daemonize0` spawns `PlatformUtil::createThread(…,"Win32ServiceThread")`, sleeps 3000, logs the verbatim "successfully daemonized"/"failed to daemonize" lines, returns jbool — ✓; and `FUN_1800053e0` → `StartServiceCtrlDispatcherA` with name `"Niagara"` — ✓ (r2).
13. `common.dll` JNI helpers `JNIString`, `JavaExceptionHandler(JNIEnv*)`, `JavaExceptionFactory::throwRuntimeException(JNIEnv*,MessageBundle*)`, `JNISystemOut(JNIEnv*)` exported, and 35 imported by nre.dll — ✓ (B125-exports/imports.txt).
14. Identities: njre `Tridium.Niagara.NJreLib`, nre `Tridium.Niagara.NreLib`, common `Tridium.Niagara.CommonLib` (all 4.14.0.22), all `signed true` — ✓.

**14/14 load‑bearing tokens re‑verified** against re‑run tool output.

**Marker tally** (measured by `grep -oE` over this file): `[CERT]` 40 · `[CERT-doc]` 0 · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` ~6 load‑bearing (the raw `[INFER]` token count is 12, but 1 is the header legend and 5 are the enumerated inferences inside this very section). Ratio **[INFER]/[CERT] ≈ 0.15** — low. The load‑bearing inferences are: the JVM‑resolves‑by‑name (vs an unseen RegisterNatives) conclusion, the niagarad→nre.dll JNI‑load step, the UI‑feature→native mapping, the C++ target class for a few un‑decompiled natives, and the teardown offsets (DetachCurrentThread/DestroyJavaVM confirmed by string, not re‑decompiled). The binaries remain a rich, near‑exhaustible primary source for this gap.

**Ghidra‑depth note (answers B124's open question):** Ghidra 12.1 `analyzeHeadless` **was available and used** this iteration (the kit's `decompile-native.sh ghidra` path works; `GHIDRA_INSTALL_DIR=/home/linuxbrew/.linuxbrew/Cellar/ghidra/12.1`). It delivered the decompiled `buildArgs`/`createVM`/`invokeJava` (njre) and `getHostId0`/`getPasswordHash0`/`daemonize0` (nre) that strings/imports alone could not. One gotcha: Ghidra's importer **crashed writing the sibling‑DLL export cache** for `nre.dll` (a malformed export name containing a `0x04` byte → JDOM `IllegalDataException`); the workaround is to import `nre.dll` **in isolation** (copied to an empty dir, no sibling DLLs) so the library‑lookup‑table writer is never invoked. r2 (`pdg`) lacks the r2ghidra plugin here, but r2 `pdf` + manual JNIEnv vtable‑offset mapping cross‑checks every Ghidra finding.

---

## 125.x — Connections

- **[Block 124]** — *deepens and upgrades it.* B124 mapped the boot path at imports/exports/strings grade and explicitly deferred the JNI bridge as gap **N2**; B125 supplies decompiler‑grade control flow. It **upgrades B124 §124.3's `[INFER]`** (daemon JNI‑load → `daemonize0` runs SCM) to `[CERT]` by decompiling `daemonize0` → `Win32ServiceThread` → `StartServiceCtrlDispatcherA`, and pins the JVM version (JNI 1.8) and the exact `JNIEnv` call sequence B124 could only list.
- **[Block 2]** — HostId: `getHostId0` decompiled = `Nre::getInstance()->getHostId(buf,0x40)` → `NewStringUTF`.
- **[Block 114]** — key material at rest: the `getKeyMaterial0`/`setKeyMaterial0`/`Dpapi*`/`RegistryUtil_*Encrypted*` natives are the native entry points to the DPAPI/registry‑protected secrets model.
- **[Block 26]** — the conceptual NRE launcher; B125 confirms the `-Dniagara.platform.provider` property string and the JNI‑native binding model it lacked.
- **[Block 10]** — daemon/station lifecycle: `daemonize0` + `SystemLog` are the native side of the service the daemon becomes.
- **[Block 18]** — signing: the launcher's `LicenseUtil`/agent gate complements the DSF/Authenticode model.
- **Forward (open gaps)**: **N3** licensing/verify (`nverify.exe` + `libciper` crypto + `LicenseUtil::isFeaturePresent` seen here as the agent gate); **N4** native driver DLLs (`lon.dll`/`opc.dll`/`pcapBacEther.dll`); **N6** platform daemon TCP protocol (the wire side of the service `daemonize0` starts).
