# Niagara N4 Platform NATIVE — Research State

> Focus: reverse‑engineer the NATIVE binaries (`.exe`/`.dll`/`.so`) of the installed
> OptimizerSupervisor‑N4.14.0.162 — the platform/runtime/daemon layer, NOT Java modules and NOT
> the live station config. READ‑ONLY. Corpus language: ENGLISH.
> Install root: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` (36 EXE / 136 DLL, PE32+ x86‑64; + `libciper.so`).
> Tools: `research-sdd/toolbelt/decompile-native.sh` + radare2/rabin2 + objdump + strings (Ghidra NOT on PATH).
> Mirrored in engram: `research/niagara/platform-native-gaps`, `research/niagara/platform-native-progress`.
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 233
gaps_closed: 7
known_gaps: 7
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


## Coverage

- **Covered blocks (this focus)**: 7 — B124, B125, B126, B127, B128, B129, B130. **Capa 25 STATIC loop CLOSED.**
- **Coverage metric**: 7 / 7 backlog items closed at static grade (N1, N2, N3, N4, N5, N6‑static, N7). The remaining open sub‑gaps (N6 live‑wire 3011/5011 framing, N4 live field‑bus driver runtime, N5 live alarm‑UI interaction) are ALL requires‑execution / DYNAMIC — none is static‑investigable. (Pre‑existing B26 partially covered N1 at doc/inference grade; B124 supersedes it with binary CERT evidence; B125 closes N2 at decompiler grade; B126 closes N3 — licensing/signature/crypto — at decompiler + real‑data grade; B127 closes N4 — native driver DLLs — at static‑RE grade; B128 closes N5 — Workbench native shell — at static‑RE grade.)
- **Last iteration**: 2026‑06‑29 (it.7, FINAL static) — closed **N7** (migration & platform tools) with B130, and **DECLARED the platform‑native STATIC loop STOPPED** (static‑investigable native gaps = 0). The 6 remaining `bin/` tool EXEs split into three classes: **thin `NreLauncher` Java launchers** (`n4mig.exe`=`Tridium.Niagara.MigrateAXtoN4` "AX to N4 Migration Tool" → Bootstrap profile `migrator:com.tridium.migrator.Migrate`; `hdbt.exe`=`Tridium.Niagara.HistoryDbTool` "History DB Tool" → `migrator:com.tridium.migrator.history.HistoryDbTool` — both structurally identical to station.exe/wb.exe of B128, so the migration/history logic is **Java**, no new native surface — already on the B124/B128 boot path); **standalone native (no JVM)** (`console.exe`=`Tridium.Niagara.Console` "Console for Niagara Environment" — pure C, sets `niagara_home`+PATH+console props then CRT `system()` = the native Niagara command prompt; sibling of `plat.exe` B129); **dual native+Java** (`test.exe`=`Tridium.Niagara.Test` "Niagara Test Runner" — links `cppunit.dll`+`common.dll`+`nre.dll` for native CppUnit + boots Java BTest via Bootstrap profile `test:javax.baja.test.TestRunner` = dev/QA harness). `nverify.exe` re‑confirmed already covered by B126 (Mocana EC_P* exports, `--unsigned *` bypass). **Outlier scoped out**: `dataExportTool.exe` is NOT a Tridium native — `Nullsoft.NSIS.exehead` "NSIS v3.04", Honeywell Inc., PE32 32‑bit GUI, Dec 2018, 78.6 MB = a Honeywell NSIS installer for a separate product. Self‑verify 16/16 tokens ✓, tally ≈33 [CERT]/4 [INFER] (ratio ≈0.12).
- **Prior iteration**: 2026‑06‑29 (it.6) — closed **N6‑static** (platform daemon, static) with B129: `plat.exe` is the `plat` multi‑command native CLI (RTTI `NativePlatformCommand` subclasses + verbatim verbs `installdaemon`/`uninstalldaemon`/`restartdaemon`/`stopdaemon`/`setsystempw`/`setdaemonuserhome`), **refining B124 §124.5** ("just an installer"→ a command launcher that (a) registers/controls the SCM service via *dynamically‑loaded* Advapi32, (b) DPAPI‑sets the system passphrase via Crypt32→`SOFTWARE\Niagara4\systempw`, (c) sets daemon user home, and (d) `LoadLibrary nre.dll → launchNre → Bootstrap → com.tridium.platform.command.BPlat` to run Java platform commands — so it *can* launch the JVM, contra B124). `plat installdaemon` `CreateServiceA`s `niagarad.exe` as service `Niagara` (display "Niagara Platform", deps FltMgr/CryptSvc/Tcpip, EventLog source) — the registration half of B125's `daemonize0`/`StartServiceCtrlDispatcherA("Niagara")` run‑time half. **Decisive negative [CERT]: ports 3011/5011 are absent from ALL 5 native binaries** — the platform TCP service is **Java** (`platDaemon-rt` 18 `B*Command` classes + `platform-rt` `BDaemonScheme ordScheme="platform"`/`BDaemonSession`/`platform.fox` MessageClient+Chunked streams, dep fox‑rt), with port model grounded in shipped `PlatformDaemon.java` (default **3011** plaintext, `secure = port != 3011` → 5011 TLS). Live‑wire framing decode = deferred requires‑execution. Self‑verify 13/13 tokens ✓, tally 37 [CERT]/8 [INFER] (ratio ≈0.22).
- **Prior iteration**: 2026‑06‑29 (it.5) — closed N5 (Workbench native shell: `wb.exe`/`wb_w.exe` + `trayIcon.dll`/`alarmDialog.dll`) with rabin2 `-I/-i/-l/-E` + strings + diff. Findings: wb*.exe are the SAME `NreLauncher`@nre.dll EXE as station.exe (identical `getInstance` import + 4 exports); `wb.exe`=console (CUI) / `wb_w.exe`=windowed (GUI) are pure subsystem/CRT‑entry twins (java.exe vs javaw.exe pattern); real JVM main is `com.tridium.nre.bootstrap.Bootstrap` for ALL launchers (refines B124), Bootstrap dispatches `workbench:com.tridium.workbench.shell.WbMain` by profile; `trayIcon.dll`=`BTrayIcon` 5 JNI natives (Shell_NotifyIcon system‑tray + hidden `TrayIconHandlerClass` msg window) and `alarmDialog.dll`=`BAlarmDialog` 4 JNI natives (USER32/GDI32 force alarm pop‑up topmost/foreground) — both desktop‑client‑only, name‑mangling JNI bind (B125), linking neither nre.dll nor common.dll. No new gaps.

## Gap‑backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **N1** | Runtime‑core boot path: nre.exe/station.exe/niagarad.exe → nre.dll/njre.dll → JVM (+daemon, watchdog) | native PE (nre/njre/nre.dll/station/niagarad) | **covered → B124** |
| high | **N2** | Native↔Java JNI bridge: how njre/nre embed & call the JVM; `NativePlatformProvider` JNI natives in detail (buildArgs/createVM control flow, JNIEnv usage in common.dll) | native PE (njre.dll, nre.dll, common.dll) | **covered → B125** |
| high | **N3** | Licensing / verify: `nverify.exe` (517 KB) signature verification CLI + `libciper.so`(+`.sig`) + `dsfspi.dll` DsfUtil | native PE/ELF + sigs | **covered → B126** |
| med | **N4** | Native driver DLLs under the Java drivers: `lon.dll`, `opc.dll`/`opcproxy`/`opccomn_ps`, `pcapBacEther.dll` (BACnet ether capture) | native PE | **covered → B127** |
| med | **N5** | Workbench native shell: `wb.exe`/`wb_w.exe` (+ trayIcon.dll, alarmDialog.dll) — how the GUI/JxBrowser shell boots vs station | native PE | **covered → B128** |
| med | N6 | Platform daemon protocol/services: `plat.exe` (CLI), the platform TCP service (3011/5011), how daemon spawns/controls station processes | native PE + runtime | **static covered → B129**; live wire (3011/5011 framing) = requires‑execution |
| low | N7 | Migration / tools: `n4mig.exe`, `hdbt.exe`, `console.exe`, `test.exe`, `nverify.exe`, `dataExportTool.exe` (75 MB) | native PE | **covered → B130** (last static gap; closes the Capa 25 static loop) |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026‑06‑28 | N1 runtime‑core boot path | B124 | 0 new (N2–N7 were pre‑seeded; N2/N3/N6 sharpened by B124 findings) |
| 2 | 2026‑06‑28 | N2 native↔Java JNI bridge | B125 | 0 new (N3 sharpened: `LicenseUtil::isFeaturePresent` agent gate seen in createVM; Ghidra 12.1 headless confirmed available for N3/N4) |
| 3 | 2026‑06‑28 | N3 licensing / signature verification / crypto | B126 | 0 new gaps; 1 CORRECTION (`libciper.so` is the Spyder/Sylk serial‑comm JNI lib for QNX‑ARM, NOT a cipher lib → ties to B106/B120/B121, not crypto). N4 sharpened: `dsfspi.dll` is the Mocana DSF JCE provider also usable by drivers. |
| 4 | 2026‑06‑28 | N4 native driver DLLs (lon/opc/pcapBacEther) | B127 | 1 new artifact placed (`bin/x86/ldvProxy.exe` = the 32‑bit LON proxy, named‑pipe server, loads Echelon `wldv32`). `dsfspi.dll` dropped from N4's target list (covered in B126). No new gaps; N5/N6/N7 unchanged. Note: deeper LON/OPC/BACnet driver behavior is now **requires‑execution** (live LON adapter / OPC server / BACnet‑Ethernet segment), not static. |
| 7 | 2026‑06‑29 | N7 migration & platform tools (n4mig/hdbt/console/test/nverify/dataExportTool) | B130 | 0 new gaps. **CLOSES the Capa 25 STATIC loop** (static‑investigable native = 0). n4mig/hdbt = thin `NreLauncher` Java launchers (`migrator:` profiles → migration/history logic is Java, already on B124/B128 boot path); console.exe = standalone native command‑prompt (env+`system()`, no JVM); test.exe = dual CppUnit+Java BTest dev harness; nverify already B126; dataExportTool = Honeywell NSIS installer (NOT Tridium, scoped out). |
| 6 | 2026‑06‑29 | N6‑static platform daemon (`plat.exe` CLI + SCM service model + Java wire location) | B129 | 0 new gaps. **Refines B124 §124.5** (`plat.exe` = `plat` multi‑command CLI, not "just an installer"; *can* launch the JVM via `nre.dll launchNre`→`Bootstrap`→`BPlat`). Established: SCM service `Niagara` registration (`CreateServiceA niagarad.exe`, deps FltMgr/CryptSvc/Tcpip) = registration half of B125's `daemonize0` run‑time half; system passphrase native write = DPAPI→`SOFTWARE\Niagara4\systempw`; **3011/5011 wire is Java** (`platDaemon-rt`/`platform.fox`, `secure = port != 3011`), absent from all native binaries. N6 live‑wire framing reclassified to requires‑execution. |
| 5 | 2026‑06‑29 | N5 Workbench native shell (wb/wb_w + trayIcon/alarmDialog) | B128 | 0 new gaps. Refines B124: real JVM main = `com.tridium.nre.bootstrap.Bootstrap` for all NreLauncher/JavaLauncher EXEs (Station/WbMain are profile‑dispatched apps Bootstrap launches). wb.exe/wb_w.exe = console/windowed twins of the SAME nre.dll `NreLauncher` EXE (desktop client = "another NreLauncher‑hosted JVM"). Two desktop‑only JNI helper DLLs documented (BTrayIcon system‑tray, BAlarmDialog foreground pop‑up). Deeper alarm‑UI interaction behaviour = requires‑execution (live GUI session). N6/N7 unchanged. |

## Blocked gaps (each tagged with what it needs)

- N6 (platform TCP wire protocol) — the *static* binary analysis is **done (B129)**: `plat.exe` + SCM service model documented, and the wire proven to be Java (`platDaemon`/`platform.fox`, 3011 plaintext / 5011 TLS via `secure = port != 3011`), absent from native binaries. Capturing the **live 3011/5011 framing/handshake** still needs a running daemon + client → requires‑execution / DYNAMIC phase.
- (none hard‑blocked on missing tools. **Ghidra 12.1 headless IS available** at `/home/linuxbrew/.linuxbrew/Cellar/ghidra/12.1` via `decompile-native.sh ghidra` — used in B125; gotcha: import `nre.dll` in isolation, its export cache crashes Ghidra's library‑lookup writer on a 0x04‑byte export name.)

## Stop control — STATIC LOOP STOPPED (primary = read‑only‑investigable exhaustion, METHODOLOGY §8)

**STOP DECLARED — 2026‑06‑29 (it.7).** The primary stopping criterion is met: **read‑only‑investigable native gaps = 0.** Every remaining open gap is requires‑execution / DYNAMIC. The Capa 25 native‑platform STATIC loop is complete: **7 blocks B124–B130** (N1 boot path, N2 JNI bridge, N3 licensing/crypto, N4 native driver shims, N5 Workbench shell, N6‑static daemon, N7 migration & tools).

- **Open gaps — read‑only investigable**: **0** ✅ (N1–N7 all closed at static grade)
- **Open gaps — requires‑execution / DYNAMIC** (deferred — each tagged with what it needs):
  - **N6 live wire** — capture/decode the live **3011 (plaintext) / 5011 (TLS)** Fox‑framed `platform.fox` chunked handshake & framing → needs a **running daemon + a platform client session** (B129 proved the wire is Java, located it, but the bytes need a live capture).
  - **N4 driver runtime** — observe live **LON** (Echelon adapter via `ldvProxy`/`\\.\pipe\LDVPipe`), **OPC DA** (live COM/DCOM server), and **BACnet/Ethernet** (live raw‑L2 segment via Npcap) data paths → needs **field hardware / adapters / a live OPC server** (B127 mapped the static shims; depth is runtime).
  - **N5 alarm‑UI interaction** — observe the desktop client's live alarm tray (`trayIcon.dll`) + foreground alarm pop‑up (`alarmDialog.dll`) behaviour → needs a **live Workbench GUI session** (B128 mapped the static JNI surface; interaction depth is runtime).
- **Open gaps — blocked** (missing tool/keys): 0. (Ghidra 12.1 headless available; no tool blockers encountered.)
- Consecutive iterations with empty backlog (secondary): n/a — stopped on primary criterion (investigable exhaustion).
- Budget cap: none.
- **Loop status**: ✅ **STATIC loop CLOSED at 7 blocks.** Re‑open only as a separate DYNAMIC phase if a live daemon / field bus / GUI session becomes available.
</content>
