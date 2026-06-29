# Block 124 — Native runtime-core boot path (nre.exe / station.exe / niagarad.exe → njre.dll/nre.dll → JVM)

> Research of the **Niagara N4 NATIVE platform/runtime layer**: how Niagara *actually boots at the native level* on the installed OptimizerSupervisor‑N4.14.0.162 — what the thin launcher EXEs do, how `njre.dll` / `nre.dll` locate and launch the embedded JRE via JNI, how the platform daemon (`niagarad`) and the station (`station.exe`) relate, the key imports/exports and verbatim strings. This is binary‑level PE reverse‑engineering (radare2/rabin2 + strings), NOT Java modules and NOT the live station config.
>
> Sources (primary, READ‑ONLY): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{nre.exe, station.exe, niagarad.exe, plat.exe, njre.dll, nre.dll}` (PE32+ x86‑64).
> Method: `rabin2 -I/-i/-l/-E`, `strings`, `sha256sum` (radare2 6.1.6). Raw evidence preserved at:
> `/home/cristian/investigacion/sdd-investigacion/audits/B124-native-triage.txt` (info/imports/linked libs),
> `…/audits/B124-strings-exports.txt` (exports),
> `…/audits/B124-strings-key.txt` (daemon/station/launch strings).
> Markers: `[CERT]` observed in the binary (command + symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (NEW arc, Capa 25). Connects [Block 26] (NRE launcher — prior, doc/inference‑grade; THIS block corrects it with binary evidence), [Block 10] (platform daemon niagarad / station lifecycle), [Block 17] (filesystem + embedded JRE), [Block 1] (NRE/Station/Workbench processes), [Block 2] (HostId/licensing), [Block 18]/[Block 114] (signing / key material at rest), [Block 123] (the live deployment those binaries run).

---

## 124.1 — The three thin launcher EXEs: identity, size, and who they hand off to `[CERT]`

Every entry binary is a tiny PE32+ x86‑64 MSVC console stub (`subsys Windows CUI`, `baddr 0x140000000`, compiled the same day Mon Jan 8 2024). Each one does essentially nothing but obtain a singleton launcher object from a sibling DLL and call it. The handoff target is visible directly in the import table.

| EXE | Size (bytes) | Win32 assembly identity / description | Imported launcher symbol → DLL | Java main class string |
|---|---|---|---|---|
| **nre.exe** | 22 808 | (none beyond CRT) | `?getInstance@NreLauncher@@SAPEAV1@XZ` → **nre.dll** | (generic) |
| **station.exe** | 24 344 | `Tridium.Niagara.Station` / "Niagara Station" | `?getInstance@NreLauncher@@SAPEAV1@XZ` → **nre.dll** | `com.tridium.sys.station.Station` |
| **niagarad.exe** | 22 808 | `Tridium.Niagara.Service` / "Niagara Service" | `?getInstance@JavaLauncher@@SAPEAV1@XZ` → **njre.dll** | `com/tridium/niagarad/NiagaraDaemon` |

Evidence:
- `rabin2 -i bin/nre.exe` → imports only `??1NreLauncher@@UEAA@XZ` + `?getInstance@NreLauncher@@SAPEAV1@XZ` from `nre.dll` (B124-native-triage.txt:41‑42). `?getInstance@NreLauncher@@SAPEAV1@XZ` demangles to `static NreLauncher* NreLauncher::getInstance()`.
- `rabin2 -i bin/station.exe` → same two symbols from `nre.dll` (B124-native-triage.txt:231‑232); `strings bin/station.exe` → `com.tridium.sys.station.Station`, identity `Tridium.Niagara.Station` (B124-strings-key.txt). `[CERT]`
- `rabin2 -i bin/niagarad.exe` → `??1JavaLauncher@@UEAA@XZ` + `?getInstance@JavaLauncher@@SAPEAV1@XZ` from **njre.dll** (B124-native-triage.txt:135‑136), PLUS `strcmp` (arg parsing); `strings bin/niagarad.exe` → `com/tridium/niagarad/NiagaraDaemon`, identity `Tridium.Niagara.Service`, and the arg literal `/console` (B124-strings-key.txt). `[CERT]`

Two corrections to [Block 26] established here:
1. **`station.exe` entry class is `com.tridium.sys.station.Station`**, not `com.tridium.niagarad.BStation` as [Block 26 §26.1] stated. `[CERT]` (string in station.exe).
2. **Launcher topology is two PARALLEL classes, not a linear chain.** [Block 26] drew `nre.exe → njre.dll → nre.dll`. Reality: station/nre EXEs bind **`NreLauncher` in nre.dll**; the daemon EXE binds **`JavaLauncher` in njre.dll**. `nre.dll` does NOT statically import `njre.dll` (its linked‑libs list is `common.dll, advapi32, crypt32, shell32, ole32, dsfspi, kernel32, …` — no njre.dll; B124-native-triage.txt:839‑854). `NreLauncher` is a SUPERSET re‑implementation of the launcher, not a wrapper around `JavaLauncher` (§124.4). `[CERT]`

Also corrected: `nre.exe` is **~22 KB**, not "~50 KB" ([Block 26 §26.1]); EXE image base is `0x140000000`, DLLs are `0x180000000` ([Block 26 §26.5] claimed `0x180000000` for all). `nre.exe` and `niagarad.exe` share the same byte size (22 808) but are **distinct binaries** (sha256 `47b73f…` vs `ed14df…`), i.e. built from one launcher template, compiled per target. `[CERT]` (`sha256sum`, `rabin2 -I`).

---

## 124.2 — `njre.dll` = the JRE wrapper: locate JVM, JNI_CreateJavaVM, run main `[CERT]`

`njre.dll` (PE32+ DLL, baddr `0x180000000`, Win32 assembly **`Tridium.Niagara.NJreLib` version 4.14.0.22**, `<description>Niagara JRE Wrapper</description>`) is the minimal, self‑contained JVM bootstrapper. It exports class **`JavaLauncher`** (abstract) and **`JavaLauncherWin32`** (Windows impl), plus `Nre`/`NreWin32` and `DsfUtil`/`SignatureUtil`/`DirectoryListing` helpers (`rabin2 -E bin/njre.dll`, B124-strings-exports.txt).

The boot sequence is not inferred — `njre.dll` ships a verbatim debug trace (gated by env var `java_debug`; `strings bin/njre.dll`: `java> debug = true`). The trace strings enumerate the exact method order `[CERT]`:

```
java> initPaths()     →  resolve jreHome, niagaraHome, niagaraUserHome, libPath, classPath, profiles
java> loadDLL()       →  "java>   Using dll = %s"  (jvmDll_0 / jvmDll_1)
java> buildArgs()     →  assemble JavaVMOption[] + program argv
java> createVM()      →  JNI_CreateJavaVM
java>   Find main class...   /  Find main method...  /  Launching main method...
java> invokeJava()    →  CallStaticVoidMethod( main, [Ljava/lang/String;)V )
java>   Detach current thread...  /  Destroy VM...
java> cleanup()
```

Backed by exported methods `?initPaths@JavaLauncherWin32@@…`, `?loadDLL@…`, `?buildArgs@…`, `?buildVMOptions@JavaLauncherWin32@@AEAAHPEAUJavaVMOption@@PEAHH@Z`, `?createVM@…`, `?invokeJava@…`, `?cleanup@…`, `?java@JavaLauncherWin32@@…` (B124-strings-exports.txt). `[CERT]`

**JVM location** (the heart of N1) `[CERT]`:
- `\jre`, `\bin\server\jvm.dll`, `\bin\client\jvm.dll` — i.e. `%NIAGARA_HOME%\jre\bin\server\jvm.dll` preferred, `client` fallback. The trace logs both candidates as `jvmDll_0` / `jvmDll_1`.
- Loaded dynamically: `njre.dll` imports `LoadLibraryA` + `GetProcAddress` + `FreeLibrary` (KERNEL32; B124-native-triage.txt:484‑486); the resolved symbol is the string `JNI_CreateJavaVM`.
- Failure strings: `Error: Cannot load: %s or %s` (both jvm.dll paths fail), `Error: Cannot find JNI functions in %s`, `Error: Can't find main class "%s"`, `Error: Can't find main method in class "%s"`, `Error: Could not detach main thread` (B124-strings-key.txt). `[CERT]`

**JVM options injected by `JavaLauncherWin32`** (verbatim format strings in `njre.dll`) `[CERT]`:
`-Xms48M`, `-Xmx48M`, `-Djava.class.path=%s`, `-Djava.library.path=%s`, `-Djava.security.manager`, `-Djava.security.properties==%s\bin\policy\java.security`, `-Dniagara.home=%s`, `-Dniagara.home.url=%s`, `-Dniagara.user.home=%s`, `-Dniagara.platform.provider=%s`, `-Dniagara.supported.runtime.profiles=%s`, `-Dniagara.required.runtime.profiles=%s`. Classpath roots assembled from `%s\bin`, `%s\bin\ext`, `%s\bin\ext\bcfips`, `%s\bin\ext\bcstd`. User‑home path pattern `%s\Niagara%d.%d\%s`.

> **Correction to [Block 26 §26.2]:** [Block 26] attributed the `-Xms48M -Xmx48M` default to njre.dll (correct) but placed the `-Djava.class.path` / `-Djava.library.path` / security / `niagara.*` flag injection under "nre.dll responsibilities". The flag *format strings* for the base set live in **njre.dll** (`JavaLauncherWin32`). `nre.dll` has its own superset (§124.4). `[CERT]`

**HostId at boot** `[CERT]`: `njre.dll` imports `GetVolumeInformationA` (KERNEL32) and emits `ERROR: Host Id cannot be found/generated.` plus debug markers `>>> hostid.debug >>>` / `nre_hostid_debug`. The HostId derivation thus runs inside the native launcher, before/around VM creation — ties to [Block 2] (HostId/licensing) at the native level.

**Signature / integrity at boot** `[CERT]`: `njre.dll` imports `?checkFileSignature@DsfUtil@@SAHPEAEHPEBDH@Z` from **dsfspi.dll** and exports `?checkFileSignature@SignatureUtil@@SAHPEBD@Z` + `?isProductionBuild@SignatureUtil@@SA_NXZ`. So the launcher verifies a digital‑signature file (DSF) and branches on production vs non‑production build during startup — the native foundation under the Java module‑signing model of [Block 18].

**Registry + secret access** `[CERT]`: `njre.dll` imports ADVAPI32 `RegOpenKeyExA/RegQueryValueExA/RegCreateKeyExA/RegSetValueExA/RegCloseKey/RegFlushKey` and CRYPT32 `CryptProtectData/CryptUnprotectData` (DPAPI), plus `SHGetKnownFolderPath` (SHELL32). Error tags `NreWin32.RHK.*` (Read HKey), `.SHK.*` (Set HKey), `.MHK.*` (Machine HKey), `.CPD/.CUD` (Crypt[Un]ProtectData) confirm registry‑based home resolution and DPAPI‑protected key material. Connects [Block 114] (key material at rest) at the native layer.

---

## 124.3 — `niagarad` daemon: same JVM launch, then SCM service + native platform provider `[CERT]`

The daemon's native entry (`niagarad.exe`) is the *simplest* of the three: it parses an optional `/console` flag and uses `njre.dll`'s `JavaLauncher` to start the JVM with main class `com/tridium/niagarad/NiagaraDaemon`. The Windows‑service behaviour is NOT in `niagarad.exe` (its imports are only `JavaLauncher` + `strcmp` + CRT; B124-native-triage.txt:135‑179). `[CERT]`

The actual service + platform machinery lives in **`nre.dll`**, exposed to the JVM as JNI natives and used by the running `NiagaraDaemon` Java object. Evidence in `nre.dll` `[CERT]`:
- **Win32 service control imports** (ADVAPI32): `StartServiceCtrlDispatcherA`, `RegisterServiceCtrlHandlerA`, `SetServiceStatus` (B124-native-triage.txt:726‑728); string `Win32ServiceThread`; `CreateProcessA`, `SetConsoleCtrlHandler`.
- **The daemon lifecycle log lines** (verbatim, B124-strings-key.txt):
  `niagarad: Niagara service startup initiated.` → `… successfully daemonized, startup can continue.` → `… startup complete, set service status to running.`
  Shutdown path: `… shutdown initiated.` → locates `NiagaraDaemon` *class* → its `instance` method → its `stop` method → `… shutdown complete, set service status to stopped.` (distinct error strings for each missing element).
  Failure: `… failed to daemonize.`, `… failed to start, set service status to stopped.`
- **JNI natives** backing the daemon (exports, `Java_com_tridium_nre_platform_NativePlatformProvider*`): `…_daemonize0`, `NativePlatformProviderTridium_daemonize0`, `…_platformDaemonShutdownRequested0`, `…_getHostId0`, `…_createWatchdog0/destroyWatchdog0/updateWatchdog0/getWatchdogTimeout0/getWatchdogPolicy0/getWatchdogCycles0`, `…_dumpThreads0`, `…_addUserAccount0/addUserToGroup0/changeUserPassword0`, `…_checkForKeyMaterialUpgrade0`, `…_enableSystemLogging0`, `…_executeNativeDiagnosticsCommand0`, `…_getCpuTime0/getCurrentCPUUtilization0`, and many more (B124-strings-exports.txt; B124-strings-key.txt). The JNI return‑type signature `()Lcom/tridium/niagarad/NiagaraDaemon;` is present in nre.dll. `[CERT]`
- **Watchdog** is native: `nre.dll` imports `EngineWatchdog::{init,open,update,check,add/remove/get Watchdog,…}` from **common.dll** (B124-native-triage.txt:630‑642) and re‑exports them as the `…Watchdog0` JNI natives — the heartbeat/restart mechanism [Block 26 §26.25] mentioned, here grounded in symbols. `[CERT]`

**Boot model of the daemon** `[INFER]` (strong, from the symbol layout): `niagarad.exe` → `njre.dll` `JavaLauncher` starts the JVM → `NiagaraDaemon` (Java) loads `nre.dll` via `java.library.path` as a JNI library → calls `NativePlatformProvider.daemonize0()` which runs `StartServiceCtrlDispatcherA` on `Win32ServiceThread`, registers the control handler, and reports `SERVICE_RUNNING` via `SetServiceStatus`. This explains why the SCM code lives in `nre.dll` (loaded later as a JNI lib) even though the service's launcher EXE only links `njre.dll`. The native↔Java JNI bridge itself is gap **N2** (next iteration).

Relation to **station.exe**: the daemon does not exec the station through these symbols; `station.exe` is itself an `NreLauncher` EXE that boots an independent JVM running `com.tridium.sys.station.Station`. The daemon manages stations as separate OS processes (consistent with [Block 10] and the live `daemon.properties` auto‑start/auto‑restart of [Block 123]); the cross‑process control is platform‑daemon protocol territory (gap **N6**, `plat.exe` / platform TCP service). `[INFER]`

---

## 124.4 — `nre.dll` = the full NRE launcher (superset of JavaLauncher) `[CERT]`

`nre.dll` (PE32+ DLL, Win32 assembly **`Tridium.Niagara.NreLib` 4.14.0.22**) exports **`NreLauncher`/`NreLauncherWin32`** — the same boot skeleton as `JavaLauncherWin32` (`initPaths/loadDLL/buildArgs/buildVMOptions/createVM/cleanup`, here driving `invokeNRE`/`nre`) plus extra responsibilities the bare JRE wrapper lacks (B124-strings-exports.txt, B124-strings-key.txt):

- **FIPS startup branch**: methods `?initFips@NreLauncherWin32@@…`, `?defaultToNonFIPS@NreLauncherWin32@@…`, the debug field `java>   fips = %s`, and string `<p n="startWorkbenchInFipsMode" v="false"/>`. The base `JavaLauncherWin32` has no FIPS logic. `[CERT]`
- **Extra JVM flags** beyond njre's set: `-Djava.protocol.handler.pkgs=com.tridium.nre.protocol` (Niagara URL handlers), `-Dprotected.station.home=%s\stations\%s` (per‑station sandbox home), plus `%s\lib\jfxrt.jar` on the classpath. `[CERT]`
- **Classpath builder over `bin/ext`**: `%s\bin\ext`, `…\bcfips`, `…\bcstd`, and the failure string `FATAL: Failed to append all items in extPath to classpath, insufficient buffer size` — so the `ext/` JAR enumeration (the ~150 MB Java stack of [Block 26 §26.4]) is assembled natively here. `[CERT]`
- **Full platform surface via common.dll**: `nre.dll` imports ~106 symbols from `common.dll` — `PerfUtil` (CPU/idle/mem), `TcpIpAdapterSettings`/`TcpIpHostSettings`/`TcpIpUtil` (IPv4/IPv6 config get/set), `FileSystemInfo` (file counts/limits), `AuthenticationUtil` (Windows account/password/group, `getPasswordHash`, `isPasswordValid`, `getDefaultAdminGroupName`), `PlatformInfo` (`archName`, `reboot`), `SystemLog`, `TimeUtil` (`setSystemTime`), and the JNI helpers `JavaExceptionHandler(JNIEnv*)` / `JNIString(JNIEnv*, jstring)` (B124-native-triage.txt:620‑725). This is why `NreLauncher` is the "real" runtime launcher and `JavaLauncher` the minimal one. `[CERT]`
- **HostId**: `?getHostId@NreWin32@@…`, JNI `…_getHostId0`, `disableHostIdGeneration`, `GetVolumeInformationA` — HostId generation is here too (shared concept with njre). Debug prefix for this launcher is `nre>` (e.g. `nre>   requiredRuntimeProfiles = %s`), distinct from njre's `java>`. `[CERT]`

So `station.exe`/`nre.exe` go through the **heavyweight** `NreLauncher` (FIPS, station sandbox, full ext classpath, platform provider, watchdog, service), while `niagarad.exe` uses the **lightweight** `JavaLauncher` and pulls the heavy native services in afterward as a JNI library. Both DLLs are Authenticode‑signed (`rabin2 -I` → `signed true`); `nre.dll` carries the **DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1** chain and a DigiCert G4 RSA4096 SHA256 timestamp (strings in nre.dll/njre.dll). `[CERT]`

---

## 124.5 — `plat.exe` (platform installer) — out of the boot path `[CERT]`

`plat.exe` (PE32+, `lang c` — pure C, not C++/MSVC like the launchers) links **neither `nre.dll` nor `njre.dll`**. It imports dynamic‑loading (`LoadLibraryA`/`GetProcAddress`/`FreeLibrary`) and a heavy filesystem set (`CreateDirectoryA`, `DeleteFileA`, `RemoveDirectoryA`, `FindFirstFileA`, `SetFileAttributesA`, `remove`, `rename`) plus string helpers (B124-native-triage.txt:327‑423). Consistent with [Block 26 §26.15] `plat.exe installdaemon` installer role: it provisions the install / registers the daemon service, it does not launch the JVM. Full analysis of `plat.exe` and the platform TCP service is gap **N6**. `[CERT]`

---

## 124.6 — Native boot path, end to end (synthesis)

```
SCM / shortcut / cmd
  │
  ├─ station.exe ──(NreLauncher::getInstance)──► nre.dll  [NreLauncherWin32]
  │     initPaths → loadDLL(jre\bin\server\jvm.dll | client) → buildVMOptions
  │     (+FIPS, -Dprotected.station.home, ext\ classpath, protocol.handler)
  │     → JNI_CreateJavaVM → main = com.tridium.sys.station.Station
  │
  ├─ nre.exe ─────(NreLauncher::getInstance)──► nre.dll  (generic main)
  │
  └─ niagarad.exe ─(JavaLauncher::getInstance)► njre.dll [JavaLauncherWin32]
        initPaths → loadDLL(jvm.dll) → buildArgs → createVM(JNI_CreateJavaVM)
        → main = com.tridium.niagarad.NiagaraDaemon
        → (Java) loads nre.dll as JNI lib → NativePlatformProvider.daemonize0
          → StartServiceCtrlDispatcherA / Win32ServiceThread / SetServiceStatus
          → watchdog + platform provider natives  [INFER on the JNI load step]

common.dll  : platform primitives (PerfUtil, TcpIp*, Auth*, EngineWatchdog, JNIString…)
dsfspi.dll  : DsfUtil::checkFileSignature  (integrity at load)
ADVAPI32/CRYPT32 : registry home resolution + DPAPI key material
```

The shared invariants across all three paths `[CERT]`: dynamic `jvm.dll` resolution under `%NIAGARA_HOME%\jre\bin\{server,client}`, `JNI_CreateJavaVM`, a Java main of signature `([Ljava/lang/String;)V`, `-Djava.security.manager` + `…\bin\policy\java.security`, classpath rooted at `bin/ext` (+`bcfips`/`bcstd`), and `-Dniagara.{home,user.home,platform.provider,supported/required.runtime.profiles}`.

---

## 124.7 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool against the binary):
1. `nre.exe` imports `?getInstance@NreLauncher@@SAPEAV1@XZ` from `nre.dll` — `rabin2 -i` ✓ (triage:41‑42).
2. `niagarad.exe` imports `?getInstance@JavaLauncher@@SAPEAV1@XZ` from `njre.dll` ✓ (triage:135‑136).
3. `station.exe` string `com.tridium.sys.station.Station` ✓ (strings‑key).
4. `niagarad.exe` string `com/tridium/niagarad/NiagaraDaemon` ✓.
5. `njre.dll` strings `\bin\server\jvm.dll` + `\bin\client\jvm.dll` + `JNI_CreateJavaVM` ✓.
6. `njre.dll` flag strings `-Xms48M` / `-Xmx48M` / `-Djava.class.path=%s` ✓.
7. `njre.dll` trace methods `initPaths()/loadDLL()/createVM()/invokeJava()/cleanup()` ✓.
8. `nre.dll` imports SCM `StartServiceCtrlDispatcherA`/`SetServiceStatus`/`RegisterServiceCtrlHandlerA` ✓ (triage:726‑728).
9. `nre.dll` daemon log line `niagarad: Niagara service startup complete, set service status to running.` ✓.
10. `nre.dll` export `Java_com_tridium_nre_platform_NativePlatformProviderTridium_daemonize0` ✓.
11. `nre.dll` FIPS methods `initFips`/`defaultToNonFIPS` + flag `-Dprotected.station.home=%s\stations\%s` ✓.
12. `njre.dll`/`nre.dll` import `DsfUtil::checkFileSignature` from `dsfspi.dll` ✓ (triage:475, 740).
13. `sha256sum` nre.exe ≠ niagarad.exe despite equal size ✓.

13/13 load‑bearing tokens re‑verified against re‑run tool output.

**Marker tally**: `[CERT]` ≈ 41 · `[CERT-doc]` 0 · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` 3. Ratio **[INFER]/[CERT] ≈ 0.07** — very low; the native binaries are a rich, near‑exhaustible primary source for this gap (symbols + verbatim debug strings make most claims directly observable). The only inferences are the *runtime* daemonization/JNI‑load sequence (N2/N6 will confirm) and the daemon‑manages‑station‑as‑process relationship.

**Ghidra note**: NOT needed for N1. The launchers retain symbols (`stripped false`) and ship a verbatim `java>`/`nre>`/`niagarad:` debug trace, so imports/exports/strings (radare2/rabin2) fully ground the boot path without pseudo‑C decompilation. Deeper control‑flow (e.g. exact order of `checkFileSignature` vs `createVM`, or `buildArgs` argv construction) would benefit from Ghidra/r2 `pdf`, relevant to N2/N3.

---

## 124.x — Connections

- **[Block 26]** — *corrects and grounds it.* B26 (Spanish, 2026‑04‑23, no markers) gave the conceptual dual‑layer NRE picture from docs/inference; B124 supplies the binary evidence and fixes: launcher topology is two parallel classes (NreLauncher/JavaLauncher), station main class is `com.tridium.sys.station.Station`, base JVM flags live in njre.dll, nre.exe is ~22 KB, EXE baddr `0x140000000`.
- **[Block 10]** — platform daemon `niagarad` and station lifecycle: B124 shows the native side (SCM dispatcher + `daemonize0` + watchdog in nre.dll; daemon launches via njre.dll).
- **[Block 17]** — embedded JRE: B124 confirms the native resolver targets `%NIAGARA_HOME%\jre\bin\{server,client}\jvm.dll`.
- **[Block 2]** — HostId/licensing: native `getHostId0` + `GetVolumeInformationA` + `disableHostIdGeneration` run inside the launcher at boot.
- **[Block 18] / [Block 114]** — signing & key material: native `DsfUtil::checkFileSignature` (dsfspi) at load + DPAPI `CryptProtectData/Unprotect` + DigiCert G4 Authenticode chain on the DLLs.
- **[Block 123]** — the live deployment: those `daemon.properties` auto‑started stations are exactly the `station.exe`/`NreLauncher` processes spawned and watchdog‑monitored by this native layer.
- **Forward (open gaps)**: **N2** native↔Java JNI bridge (how njre/nre embed and call the JVM, `NativePlatformProvider` JNI in detail); **N3** licensing/verify (`nverify.exe` + `libciper` crypto + signing); **N6** platform daemon protocol (`plat.exe`, platform TCP service).
</content>
</invoke>
