# Block 129 — The platform daemon (static): `plat.exe` command launcher, the SCM service model, and why the 3011/5011 wire is Java

> Research of the **Niagara N4 NATIVE platform daemon layer** on the installed OptimizerSupervisor‑N4.14.0.162, statically: WHAT `plat.exe` actually is (it is **not** "just an installer" — it is the `plat` multi‑command native CLI, and B124 §124.5 is refined here), HOW the platform daemon is registered and controlled as a Windows service (SCM `CreateServiceA` of `niagarad.exe` under the service name `Niagara`), the **daemon process model** end‑to‑end (SCM → `niagarad.exe` → `njre.dll` JavaLauncher → `NiagaraDaemon` → `nre.dll daemonize0`), and the decisive finding that **the platform TCP service that Workbench/the installer use for commissioning (ports 3011 / 5011) is implemented in Java, not in any native binary**. The **live wire‑protocol decode of 3011/5011 on a running daemon is a deferred requires‑execution sub‑gap**.
>
> Sources (primary, READ‑ONLY):
> - `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{plat.exe, niagarad.exe, nre.dll, njre.dll, common.dll}` (PE32+ x86‑64).
> - `…/modules/platDaemon-rt.jar`, `…/modules/platform-rt.jar` (Java platform daemon command + Fox channel classes).
> - `…/niagara-help/source/platform-rt/javax/baja/platform/PlatformDaemon.java` (port/SSL factory, shipped source).
> - `…/defaults/platform.bog` (PlatformServiceContainer defaults).
> Method: `rabin2 -I/-i/-E/-l`, `strings` (RTTI + verbatim command/SCM strings), `sha256sum`, `unzip -l/-p`, direct source read (radare2 6.1.6). Raw evidence preserved at:
> `…/audits/B129-plat-triage.txt` (PE header + imports), `…/audits/B129-plat-strings.txt` (exports/libs/all strings),
> `…/audits/B129-plat-commands.txt` (command verbs + RTTI + SCM/DPAPI symbols), `…/audits/B129-java-daemon.txt` (PlatformDaemon.java + platDaemon‑rt command/manager surface + fox channels).
> Markers: `[CERT]` observed in the binary/source (command + symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (native boot path — this block **refines its §124.5** on `plat.exe` and grounds the daemon SCM model), [Block 125] (`daemonize0`/`StartServiceCtrlDispatcherA` JNI native — the in‑process half of the service `plat installdaemon` registers), [Block 123] (the live install whose 2 daemon‑managed stations these commands start/stop), [Block 128] (`com.tridium.nre.bootstrap.Bootstrap` — the same JVM main `plat` delegates to), [Block 27]/[Block 11]/[Block 17] (port catalog: 3011/5011), [Block 10] (platform/station lifecycle), [Block 114] (DPAPI key material — the system passphrase), and the [Incident Response Playbook] (3011 plaintext / 5011 TLS entry surface).

---

## 129.1 — `plat.exe` is the `plat` command launcher, NOT "just an installer" `[CERT]`

`plat.exe` (50 968 bytes, sha256 `8c9c58a0…`, PE32+ x86‑64, `subsys Windows CUI`, `baddr 0x140000000`, compiled **Mon Jan 8 13:16:11 2024** — same build day as the launchers of B124; `rabin2 -I`, B129-plat-triage.txt) carries the Win32 assembly identity **`Tridium.Niagara.PlatCommand` version 4.14.0.22**, `<description>Niagara Command Launcher</description>`, `requestedExecutionLevel level="asInvoker"` (B129-plat-strings.txt). It is `lang c` (pure C runtime, MSVC) and Authenticode‑`signed true` on the same **DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1** chain + DigiCert G4 RSA4096 SHA256 timestamp as the launcher DLLs (cert strings in B129-plat-strings.txt; matches B124/B126). `[CERT]`

Its **static import table** is exactly what B124 §124.5 saw — `KERNEL32` dynamic‑loading (`LoadLibraryA`/`GetProcAddress`/`FreeLibrary`) + a filesystem set (`CreateDirectoryA`, `DeleteFileA`, `RemoveDirectoryA`, `FindFirstFileA`/`FindNextFileA`/`FindClose`, `GetFileAttributesA`/`SetFileAttributesA`, `WriteFile`, `remove`, `rename`, `GetModuleFileNameA`) + CRT (`rabin2 -i`, B129-plat-triage.txt). It links **neither `nre.dll` nor `njre.dll`** at the import‑table level, and exports nothing (`rabin2 -E` empty, B129-plat-strings.txt). `[CERT]`

**Refinement of [Block 124 §124.5]** `[CERT]`: B124 concluded from the bare import table that `plat.exe` "provisions the install / registers the daemon service, it does not launch the JVM". The strings + RTTI prove it is **richer than an installer**: it is the **`plat` command‑line tool**, a C++ command dispatcher whose RTTI type‑descriptors are present verbatim (`strings`, B129-plat-commands.txt:689‑696):

```
.?AVNativePlatformCommand@@        (abstract base)
.?AVDaemonServiceCommand@@         (base for the service‑control verbs)
.?AVInstallDaemonCommand@@
.?AVUninstallDaemonCommand@@
.?AVRestartDaemonCommand@@
.?AVStopDaemonCommand@@
.?AVSetSystemPassphraseCommand@@
.?AVSetDaemonUserHomeCommand@@
```

So `plat.exe` is not a single installer routine but a **polymorphic command set** (`NativePlatformCommand` subclasses), dispatched by the first argv token, with `-help`/`-usage` (B129-plat-strings.txt). The SCM/registry/crypto work *is* one of its jobs, but only one.

---

## 129.2 — The six native `plat` commands (verbatim verbs + help) `[CERT]`

Each command verb and its one‑line help live as adjacent strings (B129-plat-commands.txt:457‑467); the per‑command usage/error strings (lines 472‑549) confirm the behaviour:

| Verb | Help string | What it does (from its strings) |
|---|---|---|
| `installdaemon [/nostart]` | "Install the Niagara Platform service" | `CreateServiceA` the daemon (`/nostart` skips the auto‑start); §129.3 |
| `uninstalldaemon` | "Remove the Niagara Platform service" | `ControlService`(stop)+`DeleteService` → "Niagara service successfully removed." |
| `restartdaemon` | "Restart the Niagara Platform service" | stop+start the service **and apply staged files** (§129.3) |
| `stopdaemon` | "Stop the Niagara Platform service" | `ControlService` STOP → "Niagara service successfully stopped." |
| `setsystempw [/check] newpass` | "Set the Niagara Platform System Passphrase" | DPAPI‑encrypt → registry (§129.3); `/check` returns 0 iff a passphrase is set |
| `setdaemonuserhome newuserhome` | "Set the Niagara Daemon User Home directory" | rewrites the Daemon User Home path (`niagara_home`, `%s-nduh`, `Applications\wb.exe`, `installations`) |

The **SCM API set is resolved dynamically**, not statically imported `[CERT]`: `plat.exe` `LoadLibraryA("Advapi32.dll")` (failure string `SEVERE: Failed to load Advapi32.dll, AdvApi instance unavailable`) and `GetProcAddress` for **`OpenSCManagerA`, `CreateServiceA`, `OpenServiceA`, `StartServiceA`, `ControlService`, `QueryServiceStatus`, `ChangeServiceConfig2A`, `DeleteService`, `CloseServiceHandle`** plus registry (`RegCreateKeyA/ExA`, `RegSetValueExA`, `RegQueryValueExA`, `RegDeleteKeyA`, `RegCloseKey`) and event log (`RegisterEventSourceA`, `ReportEventA`); and `LoadLibraryA("Crypt32.dll")` for **`CryptProtectData`** (B129-plat-commands.txt:435‑456). This is why B124's static import table showed none of these — they are all behind `LoadLibrary`. `[CERT]`

> So the corrected one‑liner for `plat.exe`: **the native, JVM‑independent `plat` CLI that (a) registers/controls the Windows platform‑daemon service via dynamically‑loaded SCM APIs, (b) sets the DPAPI‑protected system passphrase, (c) sets the daemon user home, and (d) for everything else, launches the Java platform command engine through `nre.dll` (§129.4).** `[CERT]`

---

## 129.3 — `installdaemon` / `restartdaemon` / `setsystempw`: the service registration in detail `[CERT]`

**Service registration (`installdaemon`)** — the strings give the exact `CreateServiceA` parameters `[CERT]` (B129-plat-commands.txt:501‑516):
- **Service binary** = `niagarad.exe` (built from `%s\%s` over the bin dir), i.e. the daemon EXE of B124 — confirming `plat installdaemon` is what wires `niagarad.exe` into the SCM. `[CERT]`
- **Display name** = `Niagara Platform`; **description** = `Platform management service for Niagara tools`. `[CERT]`
- **Dependencies** = `FltMgr`, `CryptSvc`, `Tcpip` (the daemon depends on the filter‑manager, the Windows cryptographic service, and the TCP/IP stack — consistent with a service that does code‑signing checks (B126) and listens on TCP). `[CERT]`
- **Event‑log source registration**: writes `SYSTEM\CurrentControlSet\Services\EventLog\Application\%s` with `EventMessageFile` + `TypesSupported` (warning string `Could not update registry. Errors will not be logged.`), then logs via `RegisterEventSourceA`/`ReportEventA`. `[CERT]`
- Outcome strings: `installdaemon: Niagara service successfully installed.` → `… startup initiated.` (unless `/nostart`); failure variants `… could not be installed/started`, `cannot open service manager`. `[CERT]`

The internal service name used by the control verbs is `Niagara` (the `%s service` format arg; matches the `SERVICE_TABLE_ENTRYA` name **`"Niagara"`** that `nre.dll daemonize0` passes to `StartServiceCtrlDispatcherA`, [Block 125 §125.5]). So `plat installdaemon` (SCM registration, outside the JVM) and `daemonize0` (the in‑process dispatcher, inside the JVM) are the **two halves of the same Windows service** — registration vs. run‑time dispatch. `[CERT]/[INFER]` (CERT: both use name `Niagara`/display `Niagara Platform`; INFER: that they are the same service object — the only model consistent with both).

**`restartdaemon` = stop/start + staged‑update apply** `[CERT]`: beyond stopping and starting the service, `restartdaemon` **transfers stage files** (failure string `Niagara service failed to transfer stage files.`): it enumerates `%s\stage\*`, moves `%s\jre_stage` → `%s\jre`, and replaces `%s\modules\cryptoCore.jar` / `%s\modules\daemonCrypto.jar` (strings 520‑543, referencing `nreVersion.xml`, `install-data`, `uninstall.exe`). This is the native mechanism behind a platform‑initiated JRE/crypto‑module upgrade: changes are pre‑placed in `stage/` and committed on the controlled restart while the service is down — ties to the live install layout of [Block 17]/[Block 123]. `[CERT]`

**`setsystempw` = DPAPI → registry** `[CERT]`: `setsystempw` `CryptProtectData`‑encrypts the new passphrase and stores it under registry key `SOFTWARE\Niagara4`, value `systempw`; `/check` returns 0 iff the value exists (strings 456, 475‑481). This is the **native write path** for the System Passphrase that [Block 114] documented as the seed protecting `keyring`‑sealed BOG secrets, and that the JNI native `getSystemPassword0/setSystemPassword0` ([Block 125 §125.4]) reads from the Java side — `plat setsystempw` is the offline/bootstrap setter, DPAPI‑bound to the machine. `[CERT]`

---

## 129.4 — `plat` also launches the Java platform command engine via `nre.dll` `[CERT]`

The native command set is only the JVM‑independent fast path. For all other platform commands, `plat.exe` **dynamically loads `nre.dll` and starts a JVM** — the strings carry the complete delegation contract (B129-plat-commands.txt:431‑434) `[CERT]`:

```
platform:com.tridium.platform.command.BPlat      ← the Java platform‑command entry (an ORD in the "platform" scheme)
nre.dll                                            ← LoadLibrary target
launchNre                                          ← the nre.dll entry point invoked
com/tridium/nre/bootstrap/Bootstrap                ← the JVM main (same Bootstrap as B128)
```

So `plat.exe` `LoadLibraryA("nre.dll")` → `GetProcAddress("launchNre")` → runs `com.tridium.nre.bootstrap.Bootstrap` (the **identical** JVM bootstrap [Block 128] found embedded in `nre.exe`/`station.exe`/`wb.exe`), which dispatches the Java platform command **`com.tridium.platform.command.BPlat`**. `[CERT]` (the strings) / `[INFER]` (the LoadLibrary→launchNre call order; the symbol set admits no other reading — `launchNre` is exactly the kind of exported C entry `nre.dll` would expose, complementing its `NreLauncher::getInstance` C++ export of B124).

This **refines [Block 124 §124.5] "it does not launch the JVM"**: `plat.exe` does not *statically link* a launcher, but it **can and does launch the JVM on demand** through `nre.dll launchNre` → `Bootstrap`, exactly so that the heavy/portable platform commands run as Java (`BPlat`) while the bootstrap‑critical ones (service install, passphrase, user home) stay native C. `[CERT]`

---

## 129.5 — The daemon process model, end to end (synthesis) `[CERT]/[INFER]`

```
plat installdaemon                     plat stopdaemon / restartdaemon / uninstalldaemon
   │ (Advapi32 dyn-load)                  │  (ControlService / DeleteService + stage-file apply)
   ▼                                      ▼
Windows SCM  ── service "Niagara" (display "Niagara Platform", deps FltMgr/CryptSvc/Tcpip)
   │  ImagePath = bin\niagarad.exe
   ▼
niagarad.exe ──(JavaLauncher::getInstance)──► njre.dll  [JavaLauncherWin32]      (B124)
   │   JNI_CreateJavaVM → main com.tridium.niagarad.NiagaraDaemon
   ▼
NiagaraDaemon (Java) ── loads nre.dll as JNI lib ──► NativePlatformProvider.daemonize0  (B125)
   │   daemonize0: PlatformUtil::createThread("Win32ServiceThread")
   │            → StartServiceCtrlDispatcherA(name="Niagara") → SetServiceStatus(RUNNING)
   │            + EngineWatchdog (B124/B125)
   ▼
NiagaraDaemon opens the PLATFORM TCP SERVICE  →  3011 (plaintext) / 5011 (TLS)   [JAVA — §129.6]
   │   speaks the platform/Fox wire, authenticates OS-level admin creds
   ▼
spawns + supervises station processes  ──►  station.exe  [NreLauncher, B124]      (one JVM each)
   (the 2 live stations PRUEBAS+REFLOW of B123 = these supervised processes)
```

The native layer (`plat.exe` + `niagarad.exe` + `nre.dll`/`njre.dll`/`common.dll`) provides **registration, JVM hosting, SCM dispatch, watchdog, and the `NativePlatformProvider` primitives**; the **daemon's externally‑visible behaviour — the TCP listener, the wire protocol, station spawn/control, file transfer — is Java** (`NiagaraDaemon` + `platDaemon-rt`). `[CERT]` for the native half (B124/B125 + this block's `plat` evidence); `[INFER]` for the TCP‑open and station‑spawn steps being driven by the Java `NiagaraDaemon` (grounded by §129.6's Java evidence and consistent with [Block 10]/[Block 123]).

---

## 129.6 — The platform TCP service (3011 / 5011) is **Java**, not native `[CERT]`

Decisive negative `[CERT]`: the port literals **`3011` and `5011` do not appear in ANY native binary** — `strings | grep -E '3011|5011'` over `plat.exe`, `niagarad.exe`, `nre.dll`, `njre.dll`, `common.dll` returns **0 hits** (B129 triage; the only socket‑adjacent native string is `nre.dll`'s `-Dsun.net.maxDatagramSockets=128`, a JVM flag). The native daemon hosts the listener's *process*, but the listener itself is **not** compiled into the native layer. `[CERT]`

The wire lives in the **Java platform modules** `[CERT]`:

- **Port + SSL model** — shipped source `niagara-help/source/platform-rt/javax/baja/platform/PlatformDaemon.java` documents *"port HTTP port on which the platform daemon is listening, **default is 3011**"* and, in every factory overload, derives security as **`secure = (port != 3011)`** (lines 31, 43, 51, 75, 83; B129-java-daemon.txt). So **3011 = plaintext** and the **non‑default (5011) = TLS** — the exact 3011/5011 split the [Incident Response Playbook] and [Block 27] catalog record, now grounded in the install's own source. The factory bottoms out in `com.tridium.platform.daemon.NiagaraPlatformDaemon.make(host, port, secure, credentials)`. `[CERT]`
- **The protocol scheme** — `platform-rt.jar`'s `META-INF/module.xml` registers `com.tridium.platform.daemon.BDaemonScheme name="DaemonScheme" ordScheme="platform"` (the bajadoc: *"BDaemonScheme manages the 'platform' scheme used to mount a BDaemonSession"*) and `BDaemonSession`/`BAppSurrogate`/`BStationSurrogate`/`BModuleContent`; the module `dependency name="fox-rt"` plus the `com.tridium.platform.fox.*` classes (`MessageClient`, `ChunkedInputStream`/`ChunkedOutputStream`, `TaskDispatcher`, `BLicenseChannel`, `BTimeChannel`) show the platform wire is **Fox‑framed, chunked messaging over the `platform` ORD scheme**. `[CERT]` (B129-java-daemon.txt)
- **The command surface** — `platDaemon-rt.jar` carries the **18 server‑side platform commands** `com.tridium.platDaemon.command.B*Command` `[CERT]`: station lifecycle (`BStartStationCommand`, `BStopStationCommand`, `BTellStationCommand`, `BWatchStationCommand`, `BListStationsCommand`), file transfer (`BFileGetCommand`, `BFilePutCommand`, `BFileListCommand`, `BFileDeleteCommand`, `BBigFilesCommand`), install/commissioning (`BDistInstallCommand`, `BModuleInstallCommand`, `BBackupInstallCommand`, `BJaceJarCommand`), and host control (`BRebootHostCommand`, `BSetTimeCommand`, `BIpConfigCommand`, `BDetailsCommand`) — plus `BPortPropertiesFile`/`BDaemonOptionFile` config files. The matching client‑side manager interfaces in `PlatformDaemon.java` are `StationManager`, `FileManager`, `InstallManager`, `DaemonSecurityManager`, `BackupManager`, `PlatformLicenseManager` (B129-java-daemon.txt). **These commands ARE the "platform commands" exposed for commissioning** — Workbench's Platform tool (Application Director, Distribution/Module install, File transfer, TCP/IP config, Backup, Licensing) drives exactly this set over 3011/5011. `[CERT]` (the class/interface set) / `[INFER]` (the Workbench‑feature → command mapping).

> **Deferred requires‑execution sub‑gap (N6‑wire):** the static layer proves *where* the protocol is (Java `platDaemon`/`platform.fox` over the `platform` ORD scheme, 3011/5011, `secure = port != 3011`) and *what commands* exist. Capturing the **actual on‑the‑wire framing/handshake/auth digest of a live 3011/5011 session** needs a running daemon + a Workbench/`PlatformDaemon` client → DYNAMIC phase (consistent with [Incident Response Playbook] §step 1 `BDaemonSession.java:263`: the daemon asks for a user/password digest). `[INFER]`

---

## 129.7 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool against the binary/source):
1. `plat.exe` identity `Tridium.Niagara.PlatCommand` 4.14.0.22 / "Niagara Command Launcher", `lang c`, `signed true`, 50 968 B, sha256 `8c9c58a0…` — `rabin2 -I` + `strings` ✓ (B129-plat-triage/strings).
2. `plat.exe` static imports = KERNEL32 dyn‑load + filesystem + CRT only; **no** advapi32/nre.dll/njre.dll in the import table — `rabin2 -i`/`-l` ✓.
3. RTTI command classes `NativePlatformCommand`/`DaemonServiceCommand`/`InstallDaemonCommand`/`UninstallDaemonCommand`/`RestartDaemonCommand`/`StopDaemonCommand`/`SetSystemPassphraseCommand`/`SetDaemonUserHomeCommand` — `strings` ✓ (B129-plat-commands:689‑696).
4. Six command verbs + help strings (`installdaemon`…`setdaemonuserhome`) — `strings` ✓ (457‑467).
5. SCM APIs resolved via `Advapi32.dll` dyn‑load: `OpenSCManagerA`/`CreateServiceA`/`OpenServiceA`/`StartServiceA`/`ControlService`/`DeleteService`/`ChangeServiceConfig2A`/`QueryServiceStatus`/`CloseServiceHandle` — `strings` ✓ (437‑446).
6. Service registration params: binary `niagarad.exe`, display `Niagara Platform`, desc `Platform management service for Niagara tools`, deps `FltMgr`/`CryptSvc`/`Tcpip`, EventLog `SYSTEM\CurrentControlSet\Services\EventLog\Application\%s`+`EventMessageFile`+`TypesSupported` — `strings` ✓ (501‑516).
7. `restartdaemon` stage‑apply: `%s\stage`, `%s\jre_stage`→`%s\jre`, `cryptoCore.jar`/`daemonCrypto.jar`, `nreVersion.xml` — `strings` ✓ (520‑543).
8. `setsystempw` → `CryptProtectData` (Crypt32 dyn‑load) → registry `SOFTWARE\Niagara4` value `systempw`, `/check` — `strings` ✓ (456, 475‑481).
9. `plat` Java delegation: `nre.dll` + `launchNre` + `com/tridium/nre/bootstrap/Bootstrap` + `platform:com.tridium.platform.command.BPlat` — `strings` ✓ (431‑434).
10. `3011`/`5011` absent from all 5 native binaries (`strings|grep` = 0) — ✓ (CERT negative).
11. `PlatformDaemon.java`: default port **3011**, `secure = (port != 3011)`, factory → `NiagaraPlatformDaemon.make(host,port,secure,creds)` — source read ✓ (lines 31/43/83/103).
12. `platform-rt` `module.xml`: `BDaemonScheme ordScheme="platform"` + `BDaemonSession` + `dependency fox-rt` + `platform/fox/*` (MessageClient/Chunked*/TaskDispatcher/BLicenseChannel/BTimeChannel) — `unzip -p`/`-l` ✓.
13. `platDaemon-rt` 18 `B*Command` classes (start/stop/tell/watch/list station, file get/put/list/delete/bigfiles, dist/module/backup/jacejar install, reboot/settime/ipconfig/details) — `unzip -l` ✓.

**13/13 load‑bearing tokens re‑verified** against re‑run tool output.

**Marker tally** (measured by `grep -oE` over this file; excludes the 1 legend occurrence of each): `[CERT]` 37 · `[CERT-doc]` 0 · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` 8. Ratio **[INFER]/[CERT] ≈ 0.22** — low. The native `plat.exe` is a fully observable primary source (RTTI + verbatim verbs/SCM/registry strings), and the Java port/scheme/command surface is grounded in shipped source + module.xml; the inferences are confined to (a) the LoadLibrary→`launchNre` call order, (b) `plat installdaemon` and `daemonize0` being the same service object, (c) the Java `NiagaraDaemon` driving the TCP‑open/station‑spawn, (d) the Workbench‑feature→command mapping, and (e) the deferred live‑wire framing. **The static N6 surface is near‑exhausted; the remaining depth (live 3011/5011 framing) is requires‑execution.**

**Ghidra note:** NOT needed for N6‑static. `plat.exe` retains symbols (`stripped false`) and ships RTTI type‑descriptors + verbatim command/SCM/registry strings, so imports/exports/strings (radare2/rabin2) fully ground what it is and does; the Java side is shipped as source + module.xml. Decompilation would only add the exact argv‑dispatch branch order, which is not load‑bearing for this gap.

---

## 129.x — Connections

- **[Block 124]** — *refines its §124.5.* B124 read `plat.exe`'s bare import table as "installer, no JVM". B129 shows it is the `plat` multi‑command CLI: native service/passphrase/userhome commands via dynamically‑loaded Advapi32/Crypt32, **plus** a `LoadLibrary nre.dll → launchNre → Bootstrap → BPlat` path that *does* launch the JVM for Java platform commands. Also grounds B124 §124.3's daemon SCM model: `plat installdaemon` is what registers `niagarad.exe` as the `Niagara` service whose in‑process dispatcher is `daemonize0`.
- **[Block 125]** — `daemonize0` → `StartServiceCtrlDispatcherA("Niagara")` is the run‑time half of the service `plat installdaemon` creates; `set/getSystemPassword0` is the Java read side of `plat setsystempw`'s DPAPI/registry write.
- **[Block 128]** — `com.tridium.nre.bootstrap.Bootstrap` (the JVM main `plat` delegates to via `launchNre`) is the same Bootstrap that hosts station/workbench; the platform command engine `BPlat` is "just another Bootstrap‑dispatched app", like Station/WbMain.
- **[Block 123]** — the 2 live daemon‑managed stations (PRUEBAS + REFLOW) are exactly the `station.exe` processes that the Java `NiagaraDaemon` spawns and the `BStartStationCommand`/`BWatchStationCommand` control over 3011/5011.
- **[Block 27] / [Block 11] / [Block 17]** — port catalog: B129 grounds 3011 (plaintext) / 5011 (TLS) in the install's own `PlatformDaemon.java` (`secure = port != 3011`) and proves the listener is Java, not native.
- **[Block 114]** — the System Passphrase that protects `keyring`‑sealed BOG secrets is written natively by `plat setsystempw` (DPAPI `CryptProtectData` → `SOFTWARE\Niagara4\systempw`).
- **[Block 10]** — platform/station lifecycle: the Java `platDaemon` command set (start/stop/watch station, file transfer, dist/module install) is the concrete surface behind that lifecycle.
- **[Incident Response Playbook]** — the 3011 plaintext / 5011 TLS entry surface and the daemon's user/password‑digest auth (`BDaemonSession.java:263`); B129 confirms statically that disabling plaintext = forcing `port != 3011`.
- **Forward (open gaps)**: **N6‑wire** (live 3011/5011 framing/handshake/auth digest) = requires‑execution / DYNAMIC; **N7** migration/tools (`n4mig.exe`, `hdbt.exe`, `console.exe`, `dataExportTool.exe`) = last static gap.
