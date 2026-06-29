# Niagara N4 Platform NATIVE — Research State

> Focus: reverse‑engineer the NATIVE binaries (`.exe`/`.dll`/`.so`) of the installed
> OptimizerSupervisor‑N4.14.0.162 — the platform/runtime/daemon layer, NOT Java modules and NOT
> the live station config. READ‑ONLY. Corpus language: ENGLISH.
> Install root: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/` (36 EXE / 136 DLL, PE32+ x86‑64; + `libciper.so`).
> Tools: `research-sdd/toolbelt/decompile-native.sh` + radare2/rabin2 + objdump + strings (Ghidra NOT on PATH).
> Mirrored in engram: `research/niagara/platform-native-gaps`, `research/niagara/platform-native-progress`.

## Coverage

- **Covered blocks (this focus)**: 2 — B124, B125.
- **Coverage metric**: 2 / 7 backlog items closed (N1, N2). (Pre‑existing B26 partially covered N1 at doc/inference grade; B124 supersedes it with binary CERT evidence; B125 closes N2 at decompiler grade.)
- **Last iteration**: 2026‑06‑28 (it.2) — closed N2 (native↔Java JNI bridge) with Ghidra 12.1 decompilation.

## Gap‑backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **N1** | Runtime‑core boot path: nre.exe/station.exe/niagarad.exe → nre.dll/njre.dll → JVM (+daemon, watchdog) | native PE (nre/njre/nre.dll/station/niagarad) | **covered → B124** |
| high | **N2** | Native↔Java JNI bridge: how njre/nre embed & call the JVM; `NativePlatformProvider` JNI natives in detail (buildArgs/createVM control flow, JNIEnv usage in common.dll) | native PE (njre.dll, nre.dll, common.dll) | **covered → B125** |
| high | N3 | Licensing / verify: `nverify.exe` (517 KB) signature verification CLI + `libciper.so`(+`.sig`) crypto/signing + `dsfspi.dll` DsfUtil | native PE/ELF + sigs | pending — investigable |
| med | N4 | Native driver DLLs under the Java drivers: `lon.dll`, `opc.dll`/`opcproxy`/`opccomn_ps`, `dsfspi.dll`, `pcapBacEther.dll` (BACnet ether capture) | native PE | pending — investigable |
| med | N5 | Workbench native shell: `wb.exe`/`wb_w.exe` (+ trayIcon.dll, alarmDialog.dll) — how the GUI/JxBrowser shell boots vs station | native PE | pending — investigable |
| med | N6 | Platform daemon protocol/services: `plat.exe` (installer), the platform TCP service (3011/5011), how daemon spawns/controls station processes | native PE + runtime | partial — investigable (static); protocol wire = requires‑execution |
| low | N7 | Migration / tools: `n4mig.exe`, `hdbt.exe`, `console.exe`, `dataExportTool.exe` (75 MB) | native PE | pending — investigable |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026‑06‑28 | N1 runtime‑core boot path | B124 | 0 new (N2–N7 were pre‑seeded; N2/N3/N6 sharpened by B124 findings) |
| 2 | 2026‑06‑28 | N2 native↔Java JNI bridge | B125 | 0 new (N3 sharpened: `LicenseUtil::isFeaturePresent` agent gate seen in createVM; Ghidra 12.1 headless confirmed available for N3/N4) |

## Blocked gaps (each tagged with what it needs)

- N6 (platform TCP wire protocol) — the *static* binary analysis is investigable; capturing the **live 3011/5011 protocol** needs a running daemon + client → requires‑execution / DYNAMIC phase.
- (none hard‑blocked on missing tools. **Ghidra 12.1 headless IS available** at `/home/linuxbrew/.linuxbrew/Cellar/ghidra/12.1` via `decompile-native.sh ghidra` — used in B125; gotcha: import `nre.dll` in isolation, its export cache crashes Ghidra's library‑lookup writer on a 0x04‑byte export name.)

## Stop control (primary = read‑only‑investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read‑only investigable**: 5 (N3, N4, N5, N6‑static, N7)
- **Open gaps — requires‑execution** (live daemon protocol capture): 1 (N6 wire protocol)
- **Open gaps — blocked** (tool/hardware/keys): 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none
- **Loop‑length estimate**: ~5 more investigable iterations (one block each) → ~7 total for this focus, plus a possible DYNAMIC iteration for N6 if a live daemon is available.
</content>
