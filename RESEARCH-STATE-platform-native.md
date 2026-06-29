# Niagara N4 Platform NATIVE — Research State

> Focus: reverse‑engineer the NATIVE binaries (`.exe`/`.dll`/`.so`) of the installed
> OptimizerSupervisor‑N4.14.0.162 — the platform/runtime/daemon layer, NOT Java modules and NOT
> the live station config. READ‑ONLY. Corpus language: ENGLISH.
> Install root: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` (36 EXE / 136 DLL, PE32+ x86‑64; + `libciper.so`).
> Tools: `research-sdd/toolbelt/decompile-native.sh` + radare2/rabin2 + objdump + strings (Ghidra NOT on PATH).
> Mirrored in engram: `research/niagara/platform-native-gaps`, `research/niagara/platform-native-progress`.

## Coverage

- **Covered blocks (this focus)**: 5 — B124, B125, B126, B127, B128.
- **Coverage metric**: 5 / 7 backlog items closed (N1, N2, N3, N4, N5). (Pre‑existing B26 partially covered N1 at doc/inference grade; B124 supersedes it with binary CERT evidence; B125 closes N2 at decompiler grade; B126 closes N3 — licensing/signature/crypto — at decompiler + real‑data grade; B127 closes N4 — native driver DLLs — at static‑RE grade; B128 closes N5 — Workbench native shell — at static‑RE grade.)
- **Last iteration**: 2026‑06‑29 (it.5) — closed N5 (Workbench native shell: `wb.exe`/`wb_w.exe` + `trayIcon.dll`/`alarmDialog.dll`) with rabin2 `-I/-i/-l/-E` + strings + diff. Findings: wb*.exe are the SAME `NreLauncher`@nre.dll EXE as station.exe (identical `getInstance` import + 4 exports); `wb.exe`=console (CUI) / `wb_w.exe`=windowed (GUI) are pure subsystem/CRT‑entry twins (java.exe vs javaw.exe pattern); real JVM main is `com.tridium.nre.bootstrap.Bootstrap` for ALL launchers (refines B124), Bootstrap dispatches `workbench:com.tridium.workbench.shell.WbMain` by profile; `trayIcon.dll`=`BTrayIcon` 5 JNI natives (Shell_NotifyIcon system‑tray + hidden `TrayIconHandlerClass` msg window) and `alarmDialog.dll`=`BAlarmDialog` 4 JNI natives (USER32/GDI32 force alarm pop‑up topmost/foreground) — both desktop‑client‑only, name‑mangling JNI bind (B125), linking neither nre.dll nor common.dll. No new gaps.

## Gap‑backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **N1** | Runtime‑core boot path: nre.exe/station.exe/niagarad.exe → nre.dll/njre.dll → JVM (+daemon, watchdog) | native PE (nre/njre/nre.dll/station/niagarad) | **covered → B124** |
| high | **N2** | Native↔Java JNI bridge: how njre/nre embed & call the JVM; `NativePlatformProvider` JNI natives in detail (buildArgs/createVM control flow, JNIEnv usage in common.dll) | native PE (njre.dll, nre.dll, common.dll) | **covered → B125** |
| high | **N3** | Licensing / verify: `nverify.exe` (517 KB) signature verification CLI + `libciper.so`(+`.sig`) + `dsfspi.dll` DsfUtil | native PE/ELF + sigs | **covered → B126** |
| med | **N4** | Native driver DLLs under the Java drivers: `lon.dll`, `opc.dll`/`opcproxy`/`opccomn_ps`, `pcapBacEther.dll` (BACnet ether capture) | native PE | **covered → B127** |
| med | **N5** | Workbench native shell: `wb.exe`/`wb_w.exe` (+ trayIcon.dll, alarmDialog.dll) — how the GUI/JxBrowser shell boots vs station | native PE | **covered → B128** |
| med | N6 | Platform daemon protocol/services: `plat.exe` (installer), the platform TCP service (3011/5011), how daemon spawns/controls station processes | native PE + runtime | partial — investigable (static); protocol wire = requires‑execution |
| low | N7 | Migration / tools: `n4mig.exe`, `hdbt.exe`, `console.exe`, `dataExportTool.exe` (75 MB) | native PE | pending — investigable |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026‑06‑28 | N1 runtime‑core boot path | B124 | 0 new (N2–N7 were pre‑seeded; N2/N3/N6 sharpened by B124 findings) |
| 2 | 2026‑06‑28 | N2 native↔Java JNI bridge | B125 | 0 new (N3 sharpened: `LicenseUtil::isFeaturePresent` agent gate seen in createVM; Ghidra 12.1 headless confirmed available for N3/N4) |
| 3 | 2026‑06‑28 | N3 licensing / signature verification / crypto | B126 | 0 new gaps; 1 CORRECTION (`libciper.so` is the Spyder/Sylk serial‑comm JNI lib for QNX‑ARM, NOT a cipher lib → ties to B106/B120/B121, not crypto). N4 sharpened: `dsfspi.dll` is the Mocana DSF JCE provider also usable by drivers. |
| 4 | 2026‑06‑28 | N4 native driver DLLs (lon/opc/pcapBacEther) | B127 | 1 new artifact placed (`bin/x86/ldvProxy.exe` = the 32‑bit LON proxy, named‑pipe server, loads Echelon `wldv32`). `dsfspi.dll` dropped from N4's target list (covered in B126). No new gaps; N5/N6/N7 unchanged. Note: deeper LON/OPC/BACnet driver behavior is now **requires‑execution** (live LON adapter / OPC server / BACnet‑Ethernet segment), not static. |
| 5 | 2026‑06‑29 | N5 Workbench native shell (wb/wb_w + trayIcon/alarmDialog) | B128 | 0 new gaps. Refines B124: real JVM main = `com.tridium.nre.bootstrap.Bootstrap` for all NreLauncher/JavaLauncher EXEs (Station/WbMain are profile‑dispatched apps Bootstrap launches). wb.exe/wb_w.exe = console/windowed twins of the SAME nre.dll `NreLauncher` EXE (desktop client = "another NreLauncher‑hosted JVM"). Two desktop‑only JNI helper DLLs documented (BTrayIcon system‑tray, BAlarmDialog foreground pop‑up). Deeper alarm‑UI interaction behaviour = requires‑execution (live GUI session). N6/N7 unchanged. |

## Blocked gaps (each tagged with what it needs)

- N6 (platform TCP wire protocol) — the *static* binary analysis is investigable; capturing the **live 3011/5011 protocol** needs a running daemon + client → requires‑execution / DYNAMIC phase.
- (none hard‑blocked on missing tools. **Ghidra 12.1 headless IS available** at `/home/linuxbrew/.linuxbrew/Cellar/ghidra/12.1` via `decompile-native.sh ghidra` — used in B125; gotcha: import `nre.dll` in isolation, its export cache crashes Ghidra's library‑lookup writer on a 0x04‑byte export name.)

## Stop control (primary = read‑only‑investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read‑only investigable**: 2 (N6‑static, N7)
- **Open gaps — requires‑execution** (live daemon protocol capture; live LON/OPC/BACnet field bus for N4 driver runtime depth; live GUI session for N5 alarm‑UI interaction depth): 1 (N6 wire protocol) + N4‑runtime + N5‑runtime (deferred — need live hardware/servers/GUI)
- **Open gaps — blocked** (tool/hardware/keys): 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none
- **Loop‑length estimate**: ~2 more investigable iterations (one block each) → ~7 total for this focus, plus a possible DYNAMIC iteration for N6 if a live daemon is available.
</content>
