# Block 130 — Migration & platform tools: which `bin/*.exe` are thin Java launchers vs standalone native, and the one outlier

> Research of the **remaining `bin/` tool EXEs** of the installed OptimizerSupervisor‑N4.14.0.162, statically (READ‑ONLY): for each of `n4mig.exe`, `hdbt.exe`, `console.exe`, `test.exe`, `nverify.exe`, `dataExportTool.exe` — WHAT it is, whether it is a thin `NreLauncher`/`JavaLauncher` EXE (a Java tool that boots through the B124/B128 path) or a standalone native tool, its Java main class / runtime profile or its native job, and the decisive identity strings. This is the **cleanup gap (N7)** that closes the static native‑platform inventory; it does **not** reverse the Java migration/history logic those launchers delegate to (that is Java‑module work, out of this focus).
>
> Sources (primary, READ‑ONLY):
> - `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{n4mig.exe, hdbt.exe, console.exe, test.exe, nverify.exe, dataExportTool.exe}` (PE).
> Method: `rabin2 -I` (header/build/sign), `rabin2 -i` (imports), `rabin2 -E` (exports), `rabin2 -l` (linked DLLs), `strings` (Win32 assembly identity + `<description>` + runtime‑profile/main‑class tokens + verbatim error strings), `sha256sum`. Tool versions: radare2 6.1.6. Raw evidence preserved at:
> `…/audits/B130-triage.txt` (headers + libs + exports of all 6), `…/audits/B130-strings-java.txt` (identities + Bootstrap/profile tokens for n4mig/hdbt/console/test), `…/audits/B130-tools-detail.txt` (console spawn mechanism + NSIS/Honeywell identity of dataExportTool + nverify B126 cross‑ref).
> Markers: `[CERT]` observed in the binary (command/symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (native boot path — n4mig/hdbt are more thin `NreLauncher` EXEs like `station.exe`), [Block 128] (`com.tridium.nre.bootstrap.Bootstrap` runtime‑profile dispatch — the exact mechanism by which n4mig/hdbt/test reach their Java main), [Block 129] (`plat.exe` — the other standalone native CLI; `console.exe` is a sibling standalone native), [Block 126] (`nverify.exe` — already fully covered there; cross‑referenced, not re‑covered).

---

## 130.1 — The inventory at a glance: three classes `[CERT]`

The six remaining `bin/` tool EXEs split cleanly into three classes by their linked DLLs + exports + Win32 assembly identity (`rabin2 -l/-E/-I` + `strings`; B130-triage.txt, B130-strings-java.txt):

| EXE | Size | Class | Identity (`assemblyIdentity name` / `<description>`) | Boots via |
|---|---|---|---|---|
| `n4mig.exe` | 100 120 B | **thin Java launcher** | `Tridium.Niagara.MigrateAXtoN4` / "AX to N4 Migration Tool" | `NreLauncher`@nre.dll → Bootstrap |
| `hdbt.exe` | 35 608 B | **thin Java launcher** | `Tridium.Niagara.HistoryDbTool` / "History DB Tool" | `NreLauncher`@nre.dll → Bootstrap |
| `test.exe` | 50 456 B | **dual native+Java** | `Tridium.Niagara.Test` / "Niagara Test Runner" | CppUnit (native) **and** `NreLauncher`→Bootstrap |
| `console.exe` | 98 072 B | **standalone native** | `Tridium.Niagara.Console` / "Console for Niagara Environment" | pure C, **no JVM** |
| `nverify.exe` | 529 176 B | **standalone native** | `Niagara4.NVerify.exe` / "Niagara NVerify" | pure C — **already covered → [Block 126]** |
| `dataExportTool.exe` | 78 589 192 B | **outlier — not Tridium** | `Nullsoft.NSIS.exehead` / "Nullsoft Install System v3.04" | NSIS installer (Honeywell) |

All five Tridium EXEs are PE32+ x86‑64, `baddr 0x140000000`, Authenticode `signed true`, and were **compiled the same day Mon Jan 8 2024 13:1x** as the launchers of B124/B128/B129 (`rabin2 -I`; B130-triage.txt) — i.e. they are part of the same Niagara build. The outlier breaks every one of those invariants (§130.5). `[CERT]`

---

## 130.2 — `n4mig.exe` + `hdbt.exe` = thin `NreLauncher` Java tools (already on the B124/B128 boot path) `[CERT]`

`n4mig.exe` and `hdbt.exe` are **byte‑for‑pattern identical in structure to `station.exe`/`wb.exe` of [Block 128]**: each links **`nre.dll`** and imports exactly **two** symbols from it — `?getInstance@NreLauncher@@SAPEAV1@XZ` + the `NreLauncher` destructor (`rabin2 -i`, exactly 2 nre.dll imports each; B130-strings-java.txt / B130-tools-detail.txt) — and re‑exports the same four `NreLauncher` ctor/assign/vtable symbols (`??0NreLauncher@@…`, `??_7NreLauncher@@6B@`; `rabin2 -E`, B130-triage.txt). So both boot through the **heavyweight `NreLauncherWin32` path** (`nre.dll` → FIPS / `bin/ext` classpath / `JNI_CreateJavaVM`, per B124/B125) exactly like a Workbench or a station. `[CERT]`

What makes each a *different tool* is the embedded `com/tridium/nre/bootstrap/Bootstrap` **runtime‑profile token** that Bootstrap dispatches on (the B128 mechanism), present verbatim (`strings`; B130-strings-java.txt):

| EXE | Bootstrap main | runtime‑profile token → Java main class |
|---|---|---|
| `n4mig.exe` | `com/tridium/nre/bootstrap/Bootstrap` | `migrator:com.tridium.migrator.Migrate` |
| `hdbt.exe` | `com/tridium/nre/bootstrap/Bootstrap` | `migrator:com.tridium.migrator.history.HistoryDbTool` |

So **the actual migration logic is Java, not native**: `n4mig.exe` is a ~100 KB native shim whose only job is to start a JVM under the **`migrator`** runtime profile and hand control to `com.tridium.migrator.Migrate` (the AX→N4 migration engine); `hdbt.exe` does the same for `com.tridium.migrator.history.HistoryDbTool`. This **confirms the prior [INFER] that `hdbt` = "history database tool"** verbatim ("History DB Tool"), and pins it as a sibling of n4mig under the same `migrator` profile (i.e. the History DB Tool ships inside the migrator module, used to migrate/repair AX history databases into N4). `[CERT]` Both therefore add **no new native surface** — they are already‑covered B124/B128 boot‑path instances. `[CERT]`

---

## 130.3 — `test.exe` = the dual (native CppUnit + Java BTest) test runner `[CERT]`

`test.exe` (`Tridium.Niagara.Test` / "Niagara Test Runner") is the only one of the six that is **both** native and Java. It links **three** Niagara/test DLLs — **`common.dll`**, **`nre.dll`**, and **`cppunit.dll`** — plus `msvcp140`/`vcruntime` (`rabin2 -l`; B130-triage.txt). `[CERT]`

- **Native side (CppUnit):** it imports the full CppUnit driver surface from `cppunit.dll` — `TestRunner::addTest`/`run`, `TestResult`, `TestResultCollector::runTests`/`testFailuresTotal`, `CompilerOutputter`, `XmlOutputter` (`rabin2 -i`; B130-strings-java.txt) — and it imports C++ platform primitives directly from **`common.dll`** (`FileUtil::exists`/`verifyPath`/`fixFilePath`, `DirectoryListing`; B130-strings-java.txt). So it is a genuine **native C++ unit‑test runner for the platform libraries** (`common.dll`/`nre.dll`), emitting CppUnit XML/compiler‑format results. `[CERT]`
- **Java side:** it also embeds `com/tridium/nre/bootstrap/Bootstrap` with runtime‑profile token **`test:javax.baja.test.TestRunner`** (`strings`; B130-tools-detail.txt) — i.e. through the same B128 NreLauncher path it can boot a JVM under the **`test`** profile and run the Java BTest framework (`javax.baja.test.TestRunner`). `[CERT]`

So `test.exe` is a **developer/QA harness**, not a runtime/operational component: one binary that drives both the native CppUnit suite of the C++ platform and the Java `javax.baja.test` suite. `[INFER]` It is shipped in the install but is a build/diagnostic artifact rather than part of the station/daemon/client runtime.

---

## 130.4 — `console.exe` = standalone native "Niagara command prompt" (no JVM) `[CERT]`

`console.exe` (`Tridium.Niagara.Console` / "Console for Niagara Environment") is **pure C** (`lang c`) and links **only** `kernel32` + the CRT — **no `nre.dll`, no `njre.dll`, no JVM** (`rabin2 -l/-E` empty exports; B130-triage.txt). Unlike `plat.exe` (B129) it does not `LoadLibrary` a launcher DLL either. Its job is to **set up the Niagara shell environment and spawn a command shell into it**: `[CERT]`

- It writes the environment verbatim — `niagara_home=%s` and `path=%s;%s;` (prepends the Niagara `bin`/`jre` paths) — and `_chdir`s into the home (warning string `# WARNING: Failed to change directory to '%s'`; B130-tools-detail.txt). `[CERT]`
- It configures the console window via `SetConsoleCP`/`SetConsoleOutputCP`/`SetConsoleScreenBufferSize`/`SetConsoleTextAttribute`/`SetConsoleTitleA` + `GetStdHandle` (`rabin2 -i`; B130-tools-detail.txt). `[CERT]`
- It then calls the CRT **`system`** (imported from `api-ms-win-crt-runtime`; B130-tools-detail.txt) to launch a shell in that configured environment. `[INFER]` (the `system` import + the env/chdir setup + the `… can not launch console` SEVERE strings together imply it execs the OS shell; the literal `cmd`/COMSPEC argument is not in the string table).
- Failure paths are explicit: `SEVERE: Failed to initialize niagara_home environment variable, can not launch console` and the analogous `path` messages (`strings`; B130-tools-detail.txt). `[CERT]`

So `console.exe` is the native helper behind the Start‑menu "Niagara command prompt" — a standalone native tool (sibling of `plat.exe` in being native‑only, but it neither touches the JVM nor the SCM). It is worth a line in the inventory but carries no platform/daemon logic. `[CERT]`

---

## 130.5 — `nverify.exe` (already B126) and the outlier `dataExportTool.exe` `[CERT]`

**`nverify.exe` — cross‑reference only, already closed by [Block 126] (N3).** Its identity here re‑confirms B126: `Niagara4.NVerify.exe` / "Niagara NVerify", the Mocana provider (`ERR_MOCANA_NOT_INITIALIZED`), the `--unsigned *` wildcard‑bypass option ("Comma separated list of entry names that are allowed to be unsigned. Use * for wildcard."), and the manifest checksum‑vs‑signature check; its **exports are the Mocana EC curve tables** `EC_P224`/`EC_P256`/`EC_P384`/`EC_P521` + `PF_p224…p521` + `g_pRandomContext` (`rabin2 -E`; B130-triage.txt, B130-tools-detail.txt) — i.e. the statically‑linked Mocana ECC documented in B126. No new findings; N3 stands. `[CERT]`

**`dataExportTool.exe` — the outlier; NOT a Niagara/Tridium native.** It is the one binary in `bin/` that breaks every invariant of §130.1: it is **PE32 32‑bit i386** (not x64), `baddr 0x400000`, subsystem **Windows GUI**, **compiled Sat Dec 15 2018** (not Jan 2024), and **78.6 MB**. Its Win32 manifest self‑identifies as **`Nullsoft.NSIS.exehead` / "Nullsoft Install System v3.04"** and its version resource is **"Honeywell International Inc."** (`rabin2 -I` + `strings`; B130-triage.txt, B130-tools-detail.txt). It links the classic GUI/installer DLL set (`user32`/`gdi32`/`shell32`/`comctl32`/`ole32`/`advapi32`). `[CERT]`

So `dataExportTool.exe` is a **Honeywell‑packaged NSIS self‑extracting installer** (the 78 MB is its compressed payload), a separate Honeywell product that happens to be dropped into the Niagara `bin/`, **not part of the Niagara native platform layer**. Reversing its payload is an NSIS‑extraction exercise about a distinct Honeywell data‑export application, outside this focus's scope (the native runtime/daemon/client/tools of the Niagara platform). It is recorded here as an inventory item and explicitly scoped out. `[INFER]` on "separate product"; `[CERT]` on the NSIS/Honeywell/32‑bit/2018 facts.

---

## 130.6 — Self‑verify

**Token check (load‑bearing [CERT], grep‑confirmed in the cited evidence files):**

| # | Claim | Evidence | ✓ |
|---|---|---|---|
| 1 | n4mig identity "AX to N4 Migration Tool" | B130-strings-java.txt | ✓ |
| 2 | n4mig profile `migrator:com.tridium.migrator.Migrate` | B130-strings-java.txt | ✓ |
| 3 | hdbt identity "History DB Tool" | B130-strings-java.txt | ✓ |
| 4 | hdbt profile `migrator:com.tridium.migrator.history.HistoryDbTool` | B130-strings-java.txt | ✓ |
| 5 | n4mig/hdbt each import exactly 2 nre.dll symbols (getInstance+dtor) | B130-tools-detail.txt / B130-triage.txt | ✓ |
| 6 | n4mig/hdbt export the 4 `NreLauncher` symbols | B130-triage.txt | ✓ |
| 7 | test links common.dll + nre.dll + cppunit.dll | B130-triage.txt | ✓ |
| 8 | test CppUnit `TestRunner`/`TestResultCollector` imports | B130-strings-java.txt | ✓ |
| 9 | test profile `test:javax.baja.test.TestRunner` | B130-tools-detail.txt | ✓ |
| 10 | console pure‑C, no nre.dll, empty exports | B130-triage.txt | ✓ |
| 11 | console `niagara_home=%s` + `path=%s;%s;` + `_chdir` + `system` import | B130-tools-detail.txt | ✓ |
| 12 | console SetConsole* + GetStdHandle imports | B130-tools-detail.txt | ✓ |
| 13 | nverify identity `Niagara4.NVerify.exe` + Mocana + `--unsigned *` | B130-tools-detail.txt | ✓ |
| 14 | nverify exports EC_P224/256/384/521 + PF_p* | B130-triage.txt | ✓ |
| 15 | dataExportTool = NSIS v3.04 / Honeywell / PE32 32‑bit / Dec 2018 / 78.6 MB | B130-triage.txt / B130-tools-detail.txt | ✓ |
| 16 | all 5 Tridium EXEs signed, x64, compiled Jan 8 2024 | B130-triage.txt | ✓ |

16/16 load‑bearing [CERT] tokens confirmed in their cited sources. ✓

**Marker tally:** [CERT] ≈ 33 · [CERT‑doc] 0 · [CERT‑web] 0 · [CERT‑a] 0 · [INFER] 4 (test=build artifact; console execs the OS shell; dataExportTool=separate product; hdbt ships in migrator module). **[INFER]/[CERT] ratio ≈ 0.12.** Low ratio — but note this is a *cleanup* gap: the evidence is shallow per tool by design (identity + class + delegation target), and the deep behavior of the **Java** targets (`Migrate`, `HistoryDbTool`, `TestRunner`) is Java‑module work outside the native focus, not a native gap.

## 130.7 — Connections

- **[Block 124]** — establishes the `NreLauncher`/`JavaLauncher` thin‑EXE boot path; `n4mig.exe`/`hdbt.exe` are two more instances of it (2 nre.dll imports + 4 NreLauncher exports), confirming the topology generalizes to the tool EXEs.
- **[Block 128]** — the `com.tridium.nre.bootstrap.Bootstrap` runtime‑profile dispatch is the exact mechanism n4mig/hdbt/test use (`migrator:`/`test:` tokens), just as wb/station use `workbench:`/station classes.
- **[Block 129]** — `plat.exe` and `console.exe` are the two standalone native (non‑JVM) tools in `bin/`; `console.exe` is the lighter one (env + `system()` shell), `plat.exe` the SCM/daemon CLI.
- **[Block 126]** — `nverify.exe` was fully reversed there (N3); this block only re‑confirms its identity + Mocana ECC exports and marks it covered.
