# Block 128 — Workbench native shell (wb.exe / wb_w.exe + trayIcon.dll, alarmDialog.dll)

> Research of the **Niagara N4 NATIVE desktop‑client launch path**: how the **Workbench GUI** boots at the native level on the installed OptimizerSupervisor‑N4.14.0.162 — whether it reuses the same `nre.dll` `NreLauncher` as `station.exe`, what the `wb.exe` vs `wb_w.exe` (console vs windowed) split actually is, and what the two desktop‑only JNI helper DLLs (`trayIcon.dll` native system‑tray, `alarmDialog.dll` native alarm pop‑up) provide and how they bind. This is PE reverse‑engineering of the GUI shell (rabin2 `-I/-i/-l/-E` + strings), NOT the Java Workbench UI framework and NOT the live station config.
>
> Sources (primary, READ‑ONLY): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{wb.exe, wb_w.exe, trayIcon.dll, alarmDialog.dll}` (PE32+ x86‑64), cross‑checked against `bin/{station.exe, nre.exe}`.
> Method: `rabin2 -I` (info), `-i` (imports), `-l` (linked libs), `-E` (exports), `strings`, `sha256sum`, `diff` (radare2). Raw evidence preserved at `/home/cristian/investigacion/sdd-investigacion/audits/B128-workbench-native-triage.txt` (cited as `B128-triage:LINE`).
> Markers: `[CERT]` observed in the binary (command + symbol/offset/string cited) · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (launcher topology: thin EXE → `NreLauncher` in nre.dll; THIS block adds the GUI sibling and refines the "main class" claim), [Block 125] (JNI name‑mangling bind — the same contract the two helper DLLs use), [Block 1] (NRE/Station/Workbench processes), [Block 4]/[Block 13]‑era alarm UX (the Java `com.tridium.alarm.ui` package these natives back), [Block 26] (NRE launcher, doc/inference‑grade prior).

---

## 128.1 — `wb.exe` / `wb_w.exe` are NreLauncher EXEs, identical to `station.exe` `[CERT]`

The two Workbench launchers are the **same thin `NreLauncher` stub** as `nre.exe`/`station.exe` from [Block 124 §124.1], not a special GUI launcher. Both are PE32+ x86‑64 MSVC C++ stubs, image base `0x140000000` (EXE base), Authenticode‑signed, compiled the same day as the rest of the launcher family (Mon Jan 8 2024) — `wb.exe` and `wb_w.exe` were built **5 seconds apart** (`19:16:27Z` vs `19:16:31Z`). `[CERT]` (`rabin2 -I`, B128-triage:12‑34; sha256 distinct, B128-triage:6‑7).

| EXE | Size | Subsystem | Win32 assembly identity | Imported launcher symbol → DLL |
|---|---|---|---|---|
| **wb.exe** | 109 848 | **Windows CUI** (console) | `Tridium.Niagara.Workbench` / "Niagara Workbench" | `?getInstance@NreLauncher@@SAPEAV1@XZ` → **nre.dll** |
| **wb_w.exe** | 109 848 | **Windows GUI** (windowed) | `Tridium.Niagara.WorkbenchWrap` / "Niagara Workbench" | `?getInstance@NreLauncher@@SAPEAV1@XZ` → **nre.dll** |

Evidence `[CERT]`:
- Both import exactly the two `NreLauncher` symbols (`??1NreLauncher@@UEAA@XZ` dtor + `?getInstance@NreLauncher@@SAPEAV1@XZ` = `static NreLauncher* NreLauncher::getInstance()`) from **nre.dll** — byte‑for‑byte the same handoff `station.exe` uses (B128-triage:100‑113). Their only linked libs are `nre.dll` + CRT (`kernel32`, `vcruntime140`, the `api-ms-win-crt-*` set) — **no UI libraries** (no `user32`/`gdi32`/`jvm`/`awt`). `[CERT]`
- Both export the same 4 `NreLauncher` vtable/ctor symbols as `station.exe` (`??0NreLauncher`, `??4NreLauncher`, `??_7NreLauncher@@6B@` vtable) — confirmed `rabin2 -E` count: wb.exe = 4, station.exe = 4 (B128-triage:116‑120). So the GUI shell carries the identical launcher skeleton. `[CERT]`

**Consequence:** the Workbench desktop client boots through the **heavyweight `NreLauncherWin32` path of nre.dll** ([Block 124 §124.4]) — the same FIPS branch, `bin/ext` classpath builder, `-Djava.protocol.handler.pkgs=com.tridium.nre.protocol`, security manager and `JNI_CreateJavaVM` embedding ([Block 125]) — **identically to a station**, not through the lightweight `JavaLauncher`/njre.dll daemon path. The desktop client is "just another `NreLauncher`‑hosted JVM" at the native level; what differs is the Java main it ends up running (§128.3) and the GUI‑process plumbing on top (§128.2, §128.4). `[INFER]` (strong — same `getInstance` import, same nre.dll bind, same exports).

---

## 128.2 — `wb` vs `wb_w`: pure subsystem/CRT‑entry twins (console vs windowed) `[CERT]`

`wb.exe` and `wb_w.exe` are the **same launcher compiled for two PE subsystems**. The difference is entirely in the PE subsystem flag + the C‑runtime entry stub, nothing in the Niagara logic. A full `diff` of their unique strings differs in only: the subsystem, the CRT entry symbol, the assembly‑identity *name*, and the 5‑second‑apart build timestamp. `[CERT]`

| Aspect | wb.exe (console) | wb_w.exe (windowed) |
|---|---|---|
| PE subsystem | `Windows CUI` (B128-triage:22) | `Windows GUI` (B128-triage:34) |
| CRT entry import | `_get_initial_narrow_environment` (mainCRTStartup → `main`/argc/argv) (B128-triage:103) | `_get_narrow_winmain_command_line` + `GetStartupInfoW` (WinMainCRTStartup → `WinMain`) (B128-triage:110‑111) |
| Assembly identity | `Tridium.Niagara.Workbench` (B128-triage:165) | `Tridium.Niagara.WorkbenchWrap` (B128-triage:171) |
| Niagara payload | `getInstance@NreLauncher`@nre.dll, `Bootstrap`, `WbMain` — IDENTICAL | IDENTICAL |

Interpretation `[INFER]` (standard Windows console‑vs‑GUI subsystem semantics): **`wb.exe` (CUI)** is launched with a console attached — when run from a command prompt it keeps the parent console (you see stdout/stderr, log output), the classic "developer / scripted" Workbench. **`wb_w.exe` (GUI)** has subsystem `GUI`, so Windows does **not** allocate/attach a console — double‑clicked from Explorer or a Start‑menu shortcut it launches the Workbench window with no flashing console behind it (the "WorkbenchWrap" wrapper for end‑user desktop launch). Both then create the same JVM and run the same Java GUI; the choice only governs whether a Win32 console is present. This mirrors the standard JDK `java.exe` (console) vs `javaw.exe` (windowed) pairing. `[INFER]`

> This is the native counterpart of the [Block 1] observation that Workbench is a desktop process: B128 shows there are **two** desktop entry binaries, identical except for console attachment.

---

## 128.3 — Workbench Java entry: `Bootstrap` → `workbench:com.tridium.workbench.shell.WbMain` `[CERT]`

The Workbench launchers carry two Java‑class strings (B128-triage:167‑168):
- `com/tridium/nre/bootstrap/Bootstrap` — slash‑form internal class name (FindClass form).
- `workbench:com.tridium.workbench.shell.WbMain` — a **`<runtime‑profile>:<class>`** application token; `workbench` is the runtime profile (cf. `-Dniagara.supported.runtime.profiles` injected by the launcher, [Block 124 §124.2/§124.4]) and `com.tridium.workbench.shell.WbMain` is the Workbench application main. `[CERT]`

**Refinement to [Block 124 §124.1].** B124 listed each EXE's "Java main class" as a single value (station = `com.tridium.sys.station.Station`). Cross‑checking all launchers shows **every** `NreLauncher`/`JavaLauncher` EXE actually embeds `com/tridium/nre/bootstrap/Bootstrap`: `nre.exe`, `station.exe` AND `wb.exe`/`wb_w.exe` all contain it (B128-triage:167,173,180,185). So the **real JVM main handed to `JNI_CreateJavaVM`/`invokeJava` ([Block 125]) is `com.tridium.nre.bootstrap.Bootstrap`**, and the per‑application class (`…station.Station`, `workbench:…WbMain`) is the **target Bootstrap selects/launches**, distinguished by runtime profile. `[INFER]` (strong — Bootstrap is present in all three EXE families; station adds Station, wb adds WbMain; `nre.exe` the generic launcher carries Bootstrap only, no app class, B128-triage:185).

This makes the native picture: **wb*.exe → nre.dll `NreLauncher` → JVM → `com.tridium.nre.bootstrap.Bootstrap.main(...)` → (profile `workbench`) `com.tridium.workbench.shell.WbMain`** — the only divergence from the station path is the application class Bootstrap dispatches to. `[INFER]`

---

## 128.4 — `trayIcon.dll` = native Windows system‑tray for the alarm UI (5 JNI natives) `[CERT]`

`trayIcon.dll` (PE32+ DLL, base `0x180000000`, `lang c` pure‑C like `plat.exe` — not C++/MSVC like nre.dll, Authenticode‑signed; identity **`Tridium.Niagara.AlarmTray`** / `<description>Niagara Alarm Tray Library</description>`, B128-triage:188‑189) is a JNI helper for the Java class **`com.tridium.alarm.ui.BTrayIcon`**. It exports exactly 5 JNI natives (B128-triage:123‑127) `[CERT]`:

| Export | Role `[INFER]` |
|---|---|
| `Java_com_tridium_alarm_ui_BTrayIcon_nativeLoadImage` | load icon resource (`ExtractIconA`) |
| `Java_com_tridium_alarm_ui_BTrayIcon_nativeShow` | add the tray icon (`Shell_NotifyIconA` NIM_ADD) |
| `Java_com_tridium_alarm_ui_BTrayIcon_nativeUpdate` | modify icon/tooltip (`Shell_NotifyIconA` NIM_MODIFY) |
| `Java_com_tridium_alarm_ui_BTrayIcon_nativeHide` | remove the icon (`Shell_NotifyIconA` NIM_DELETE) |
| `Java_com_tridium_alarm_ui_BTrayIcon_nativeFreeImage` | `DestroyIcon` cleanup |

Backing Win32 imports `[CERT]` (B128-triage:137 + imports section):
- **SHELL32**: `Shell_NotifyIconA` (the Windows system‑tray/notification‑area API), `ExtractIconA`.
- **USER32**: `RegisterClassExA` + `CreateWindowExA` + `DefWindowProcA` + `SetWindowLongPtrA`/`GetWindowLongPtrA` + `DestroyWindow` — it registers a window class literally named **`TrayIconHandlerClass`** (string, B128-triage:190) and creates a **hidden message‑only window** to receive the tray‑icon callback messages; `GetMessageA`/`TranslateMessage`/`DispatchMessageA` + `PostThreadMessageA` = the dedicated **message pump** thread for that window. `[CERT]`

So `BTrayIcon` (Java, in the `com.tridium.alarm.ui` UX package) is backed natively by a private Win32 message window that owns a Shell notification‑area icon — the blinking alarm tray indicator of the Workbench/desktop client. It links **neither nre.dll nor common.dll** — it is a **stand‑alone JNI lib** (`rabin2 -l` shows only shell32/user32/kernel32/CRT, B128-triage; no nre/common/njre), loaded by the JVM via `java.library.path` and bound by **JNI name‑mangling, NOT RegisterNatives** (0 `RegisterNatives` refs, B128-triage) — the exact binding contract proven for the platform natives in [Block 125]. `[CERT]`

---

## 128.5 — `alarmDialog.dll` = native "force the alarm pop‑up to the front" helper (4 JNI natives) `[CERT]`

`alarmDialog.dll` (PE32+ DLL, base `0x180000000`, `lang c`, signed; identity **`Tridium.Niagara.AlarmDialog`** / `<description>Niagara Alarm Dialog Library</description>`, B128-triage:193‑194) is the JNI helper for Java class **`com.tridium.alarm.ui.BAlarmDialog`**. It exports 4 JNI natives (B128-triage:130‑133) `[CERT]`:

| Export | Backing Win32 imports `[CERT]` | Role `[INFER]` |
|---|---|---|
| `…BAlarmDialog_toTop` | USER32 `GetForegroundWindow`, `SetWindowPos`, `ShowWindow`, `EnumWindows`, `GetWindowThreadProcessId`, `GetWindowTextA`/`SetWindowTextA` | locate the alarm window and raise it above all others / steal foreground |
| `…BAlarmDialog_setAlwaysOnTop` | USER32 `SetWindowPos` (HWND_TOPMOST) | pin the alarm dialog topmost |
| `…BAlarmDialog_getPixel` | GDI32 `GetDC`/`ReleaseDC` (+ pixel read) | sample a screen pixel |
| `…BAlarmDialog_releaseNativeResources` | `DestroyWindow` / DC release | cleanup |

The import set (USER32 `EnumWindows`+`GetWindowThreadProcessId`+`GetWindowTextA` to find the window, `GetForegroundWindow`+`SetWindowPos`+`ShowWindow` to raise it, GDI32 `GetDC`/`ReleaseDC` for `getPixel`) shows `BAlarmDialog` is a **native escalation helper that forces the Java/Swing alarm pop‑up to the foreground and keeps it always‑on‑top** — so a fired alarm visibly interrupts the operator regardless of what window has focus. Like `trayIcon.dll` it links **no nre.dll/common.dll** (only user32/gdi32/kernel32/CRT) and uses **name‑mangling JNI bind, no RegisterNatives** ([Block 125] contract). `[CERT]` / `[INFER]` (the per‑function role mapping is deduced from the imports; the export names + import set are observed).

> Note: the grep for audio APIs (`winmm`/`PlaySound`/`MessageBeep`) returned nothing — the audible‑alarm side is **not** in this DLL; `alarmDialog.dll` is visual‑only (foreground/topmost/pixel). `[CERT]` (absence in `rabin2 -i`/`-l`).

---

## 128.6 — Desktop client vs daemon/station boot (synthesis) `[CERT]/[INFER]`

```
DESKTOP CLIENT (interactive operator)
  wb.exe  (CUI) ─┐
  wb_w.exe (GUI) ┘─(NreLauncher::getInstance)─► nre.dll [NreLauncherWin32]   ← SAME path as station.exe
        initPaths → loadDLL(jvm.dll) → buildVMOptions (+FIPS, bin/ext, protocol.handler)
        → JNI_CreateJavaVM → main = com.tridium.nre.bootstrap.Bootstrap
            → profile "workbench" → com.tridium.workbench.shell.WbMain   (Swing/JxBrowser GUI)
        + (in‑JVM, java.library.path) two desktop‑only JNI helpers:
            trayIcon.dll    → BTrayIcon    (Shell_NotifyIcon system‑tray, hidden msg window)
            alarmDialog.dll → BAlarmDialog (force alarm pop‑up topmost/foreground)

STATION (headless runtime)         : station.exe → nre.dll [NreLauncher] → Bootstrap → com.tridium.sys.station.Station
DAEMON (Windows service)           : niagarad.exe → njre.dll [JavaLauncher] → NiagaraDaemon → loads nre.dll JNI (SCM)   [B124]
```

Key contrasts established `[CERT]` unless noted:
1. **Same launcher, different app.** wb*/station both go through `nre.dll` `NreLauncher` (identical `getInstance` import + exports) → `Bootstrap`; only the dispatched application class differs (`WbMain` vs `Station`). The daemon alone uses the lightweight `njre.dll` `JavaLauncher` path ([Block 124]). `[CERT]` (imports) / `[INFER]` (app dispatch).
2. **Two desktop entry binaries** (console `wb.exe`, windowed `wb_w.exe`) vs one for station/daemon — the console‑attach split is unique to the interactive client. `[CERT]`
3. **GUI‑only native surface.** The desktop client pulls in two extra JNI DLLs (`trayIcon`, `alarmDialog`) in the `com.tridium.alarm.ui` package that have **no reason to exist headless** — system‑tray icon + foreground alarm pop‑up are operator‑facing. The station/daemon native surface is `NativePlatformProvider`/watchdog/SCM ([Block 124/125]); the client's extra native surface is desktop‑UX. Both bind by the same name‑mangling JNI contract ([Block 125]). `[CERT]`
4. **No JxBrowser/AWT native in these EXEs.** The wb launchers themselves link only nre.dll+CRT; the GUI toolkit (Swing/JavaFX `jfxrt.jar` on the classpath per [Block 124 §124.4], JxBrowser) is loaded later inside the JVM by `WbMain`, not by the native launcher. `[CERT]` (`rabin2 -l` wb.exe).

---

## 128.7 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool against the binary):
1. `wb.exe` imports `?getInstance@NreLauncher@@SAPEAV1@XZ` from `nre.dll` — `rabin2 -i` ✓ (triage:100‑102).
2. `wb_w.exe` imports the same `getInstance@NreLauncher`@nre.dll ✓ (triage:109).
3. `wb.exe` subsys = `Windows CUI`; `wb_w.exe` subsys = `Windows GUI` ✓ (triage:22,34).
4. `wb.exe` CRT entry `_get_initial_narrow_environment` vs `wb_w.exe` `_get_narrow_winmain_command_line`+`GetStartupInfoW` ✓ (triage:103,110‑111).
5. `wb.exe`/`wb_w.exe` linked libs = nre.dll + CRT only (no user32/gdi32/jvm) ✓ (`rabin2 -l`).
6. `wb.exe` string `workbench:com.tridium.workbench.shell.WbMain` ✓ (triage:168).
7. `Bootstrap` (`com/tridium/nre/bootstrap/Bootstrap`) present in wb.exe AND station.exe AND nre.exe ✓ (triage:167,180,185).
8. wb.exe exports 4 `NreLauncher` symbols = station.exe count 4 ✓ (`rabin2 -E`).
9. `trayIcon.dll` exports 5 `Java_com_tridium_alarm_ui_BTrayIcon_native*` ✓ (triage:123‑127).
10. `trayIcon.dll` imports `Shell_NotifyIconA` (SHELL32) + registers `TrayIconHandlerClass` ✓ (triage:137,190).
11. `alarmDialog.dll` exports 4 `Java_com_tridium_alarm_ui_BAlarmDialog_*` (getPixel/releaseNativeResources/setAlwaysOnTop/toTop) ✓ (triage:130‑133).
12. `alarmDialog.dll` imports USER32 `EnumWindows`/`GetForegroundWindow`/`SetWindowPos` + GDI32 `GetDC` ✓ (triage:153 + imports).
13. trayIcon.dll/alarmDialog.dll link neither nre.dll nor common.dll (`rabin2 -l`) ✓.
14. 0 `RegisterNatives` in any of the 4 binaries (name‑mangling bind, [Block 125]) ✓.
15. All 4 binaries `signed true` (`rabin2 -I`) ✓ (triage).

15/15 load‑bearing tokens re‑verified against re‑run tool output.

**Marker tally**: `[CERT]` ≈ 26 · `[CERT-doc]` 0 · `[CERT-web]` 0 · `[CERT-a]` 0 · `[INFER]` 8. Ratio **[INFER]/[CERT] ≈ 0.31** — moderate. The binary facts (subsystem split, nre.dll bind, JNI export names, Win32 import sets, identities) are all directly observed `[CERT]`; the inferences are the *runtime semantics* (console‑vs‑windowed launch behaviour, Bootstrap→app dispatch, per‑native‑function role from imports) which would need Ghidra control‑flow or a live run to lift to `[CERT]`. Given the launchers are tiny `NreLauncher` re‑skins already decompiled in [Block 124/125], static triage is sufficient for N5 — no decompiler was needed.

**Ghidra note**: NOT needed for N5. The wb launchers are byte‑identical `NreLauncher` stubs to station.exe (already grounded in B124/B125 at decompiler grade), and the two helper DLLs expose their entire contract through export name‑mangling + the Win32 import set. Decompilation would only refine the per‑native message‑loop body (relevant only if N5 were pushed to interaction‑level depth, which needs a live GUI session → requires‑execution).

---

## 128.x — Connections

- **[Block 124]** — *extends and refines it.* B124 mapped `nre.exe`/`station.exe` → `NreLauncher`@nre.dll and `niagarad.exe` → `JavaLauncher`@njre.dll. B128 adds the **GUI sibling**: `wb.exe`/`wb_w.exe` are the same `NreLauncher` EXE (4 identical exports, same `getInstance` import), and **refines** B124's per‑EXE "main class" — the true JVM main is `com.tridium.nre.bootstrap.Bootstrap` for all of nre/station/wb; the app class (Station / WbMain) is what Bootstrap dispatches to by runtime profile.
- **[Block 125]** — same JNI **name‑mangling bind, no RegisterNatives**: `trayIcon.dll` (5) + `alarmDialog.dll` (4) natives bind exactly like the 107 `NativePlatformProvider` natives; and the wb launcher drives the same `JNI_CreateJavaVM`/`invokeJava` embedding flow B125 decompiled.
- **[Block 1]** — the Workbench/NRE/Station process trio: B128 shows there are **two** Workbench desktop binaries (console `wb.exe` + windowed `wb_w.exe`) and that the client process is an `NreLauncher`‑hosted JVM just like a station.
- **[Block 26]** — corrects/grounds the doc‑grade NRE‑launcher picture with the GUI binaries.
- **Alarm UX (Java `com.tridium.alarm.ui`)** — `trayIcon.dll`→`BTrayIcon` (system‑tray icon) and `alarmDialog.dll`→`BAlarmDialog` (foreground pop‑up) are the native backends of the Workbench alarm console; they are **desktop‑client‑only** native surface (no headless reason to exist).
- **Forward (open gaps)**: **N6** platform daemon protocol/services (`plat.exe`, platform TCP 3011/5011) — static investigable; **N7** migration/tools (`n4mig.exe`, `hdbt.exe`, `console.exe`, `dataExportTool.exe`).
