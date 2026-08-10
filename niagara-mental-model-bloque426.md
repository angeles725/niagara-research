# Block 426 — How the `program` module compiles a Program: a spawned `bin/javac` subprocess, with tools.jar one level removed

> Research of the **compilation path of the `program` module** (`com.tridium.program.ui.Compiler`): how a
> Program object's Java source becomes signed `.class` bytes. Answers the specific question "how does the
> program module invoke `javac` from `tools.jar`" — and CORRECTS the common assumption. Scope: the compile
> command construction, the classpath, the execution channel, the read-back, and the code-signing hand-off.
> Does NOT cover Program runtime execution (`BProgram` lifecycle on the station) or the code-signing crypto
> internals (that is [Block 392]/[Block 425] territory).
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `program-wb.jar`
> sha256 `321ad9e2b8b96c17cc8f75f11b3abb9f1cd3642979a0a3f4dc3e582af4a4180d`.
>
> Sources: preserved decompilation `sources/decompiled/program-wb/com/tridium/program/ui/Compiler.java`
> (+ `RecompileTool.java`); cross-checked `program-rt.jar` (station) has ZERO compiler references. Method:
> Vineflower decompilation + grep. Markers: `[CERT]` decompiled source (`file:line`) · `[INFER]` deduction.
>
> Framework layer (Program/robotic-code subsystem). Connects [Block 392] (code signing / `program.requireSigning`),
> [Block 398] (security-audit saw `program.requireSigning` off live), [Block 425] (the DSF/Mocana signing crypto
> `SigningUtil` ultimately uses). Native `lib/tools.jar` = Azul Zulu JDK8 `1.8.0_282` (identity established when
> answering the tools.jar question this block formalizes).

---

## 426.1 — Compilation happens in the WORKBENCH, not the station `[CERT]`

The compiler is `com.tridium.program.ui.Compiler`, in **`program-wb`** (the Workbench-profile module). The
station-side `program-rt.jar` has **zero** references to `javac`/`tools.jar`/`JavaCompiler` (grep: 0 hits).
`[CERT]` Both recompile entry points — `RecompileTool` and `BProgramRecompileTool` — live in `program-wb`
and drive the same class: `new Compiler(null).compile(className, code, src)`
(`sources/decompiled/program-wb/com/tridium/program/ui/RecompileTool.java:191`). `[CERT]` So a Program's Java
is compiled at ENGINEERING time (Workbench), and the resulting signed `.class` bytes are stored in the
`BCode` component (in the `.bog`) and shipped to the station, which only EXECUTES them. `[INFER]` this
CORRECTS the intuitive "the station compiles Programs at runtime" — it does not; it has no compiler.

## 426.2 — tools.jar is invoked INDIRECTLY: the module spawns the `bin/javac` executable `[CERT]`

`getCompileJavaCommand` builds a command line that runs the JDK's **`javac` executable**, located from the
running JVM's own home: `[CERT]`

```java
String jdkHome = System.getProperty("java.home");                 // :411
String javacMacro = "\"" + jdkHome + File.separator + "bin" + File.separator
   + "javac\" -encoding UTF-8 %javac.profile% -Xlint:deprecation "
   + "-classpath \"%javac.classpath%\" -d \"%javac.out%\" %javac.src%";   // :412–417
```

(`sources/decompiled/program-wb/com/tridium/program/ui/Compiler.java:402`–`:442`.) The program module does
**NOT** load `com.sun.tools.javac.Main` reflectively, nor use the in-process `javax.tools.JavaCompiler` API,
nor put `tools.jar` on any classpath. `[CERT]` It launches `<java.home>/bin/javac` as an external command.
**`lib/tools.jar`'s role is therefore ONE LEVEL REMOVED**: the `javac` launcher executable itself loads
`com.sun.tools.javac` from `lib/tools.jar` internally (standard JDK 8 behavior) — the program module never
touches tools.jar directly. `[CERT]`/`[INFER]` (the module-side is `[CERT]`; the javac-loads-tools.jar step
is standard JDK 8 mechanics, `[INFER]` from the launcher design, not from this code.)

The "**Problem loading 'javac' command.**" message (`Compiler.java:115`) is printed when that command
construction/exec throws — i.e. when the JDK `javac` cannot be run, not when a class fails to load. `[CERT]`

## 426.3 — The execution channel: NShell, not ProcessBuilder `[CERT]`

The command runs through Niagara's own shell, `com.tridium.nsh.NShell` (or the Workbench `BConsole` when an
owner widget is present): `[CERT]`

```java
NShell shell = new NShell(System.out);
shell.exec(cmd);
shell.execWaitUntilDone();     // Compiler.java:174–176
```

`compile()` dispatches three ways (`Compiler.java:156`–`:180`): with a UI owner → `BConsole.exec` (async,
callback `consoleExecDone`); headless with `reportErrors` → `NShell` capturing stdout, scanning for
`"error"`; headless default → `NShell` to `System.out`. `[CERT]` `[INFER]` NShell is where the `javac`
command string is actually turned into a process, so the `bin/javac` launch is mediated by Niagara's shell
layer, giving it the console plumbing and the `execWaitUntilDone` synchronization.

## 426.4 — What gets compiled, and the classpath it builds `[CERT]`

`compile()` (`Compiler.java:63`–`:118`) stages a temp compilation under the user home: `[CERT]`

| Step | Detail | Citation |
|---|---|---|
| Temp source | writes the Program source to `<niagaraUserHome>/temp/<className>.java` | `Compiler.java:65`,`:79` |
| Classpath | every `bin/ext/*.jar`, then `modules/baja.jar`, then each dependency module's `<mod>.jar` (deps parsed from the code's imports resolved via `Sys.getRegistry()`) | `Compiler.java:86`–`:103` |
| Profile | `-profile compact3` when all deps are `rt`/`wb`; empty (full SE) when any dep is `ux`+ | `Compiler.java:399`,`:422` |
| Output | `-d <temp>`, so `.class` (+ inner `$` classes) land beside the source | `Compiler.java:420` |
| Read-back | on exit 0, `<className>.class` and each `<className>$*.class` are read into a `BCode` as `BBlob`s | `Compiler.java:200`–`:233` |
| Cleanup | temp `.java`/`.class`/inner files are deleted | `Compiler.java:359`–`:382` |

`[CERT]` The `-profile compact3` flag ties the Program to the Java 8 **compact3** profile for station-only
code — a JDK-8-specific compiler feature, corroborating that this pipeline is built around the bundled
JDK 8 (`lib/tools.jar` = Azul Zulu `1.8.0_282`). `[INFER]`

## 426.5 — After compile: code signing gates the bytes `[CERT]`

On success, before storing, `compileSuccess` calls `signCode` when `signingEnabled` (`Compiler.java:237`).
`signCode` (`Compiler.java:257`,`:298`) signs the `.class` and each inner class via
`SigningUtil.generateSignature(bytes, alias, tsaUrl, keyPassword, keyStore)`, using a signing cert alias
from `BCodeSigningOptions` and an optional TSA URL. `[CERT]` If no alias is set AND the system property
**`program.requireSigning`** is true, it forces cert selection; if `program.requireSigning` is false and no
alias, it stores an unsigned `BBlob.DEFAULT` signature and logs `program.willNotSign` (`Compiler.java:260`,
`:301`–`:304`). `[CERT]` This is the same `program.requireSigning` [Block 398] found **off** on the live
supervisor — meaning compiled Programs there are stored unsigned. `[INFER]` (connects the compile pipeline
to the corpus signing thread.)

## 426.6 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | Compilation is in `program-wb` (`Compiler`); `program-rt` has zero compiler refs | `[CERT]` | `Compiler.java` + rt grep 0 hits |
| 2 | Both recompile tools call `new Compiler(null).compile(...)` | `[CERT]` | `RecompileTool.java:191` |
| 3 | Command = `"<java.home>/bin/javac" -encoding UTF-8 [-profile compact3] -Xlint:deprecation -classpath … -d … <src>` | `[CERT]` | `Compiler.java:411`–`:417` |
| 4 | Module does NOT load tools.jar / `JavaCompiler` in-process; tools.jar is used only inside the spawned `javac` | `[CERT]`/`[INFER]` | `Compiler.java:402`–`:442` |
| 5 | Executed via `NShell.exec` / `BConsole.exec`, not `ProcessBuilder` | `[CERT]` | `Compiler.java:158`,`:163`,`:175` |
| 6 | Classpath = `bin/ext/*.jar` + `baja.jar` + dependency module jars | `[CERT]` | `Compiler.java:86`–`:103` |
| 7 | Output `.class` read back into `BCode`, then optionally signed (`program.requireSigning`) | `[CERT]` | `Compiler.java:200`,`:237`,`:260` |

**Marker tally**: `[CERT]` ≈ 16 · `[INFER]` 5 ([INFER]/[CERT] ≈ 0.31). Type: **EVIDENCE block**
(decompilation) — ratio healthy. Token-checked load-bearing strings against the preserved source:
`System.getProperty("java.home")`, `bin`+`javac`, `-profile`, `NShell`, `program.requireSigning`,
`SigningUtil.generateSignature` — all present at cited lines. Premise correction recorded: the earlier
session inference "station compiles Programs at runtime via in-process tools.jar javac" is REFUTED on both
counts (Workbench-side; spawned executable).

## 426.7 — Connections

- **[Block 392]** / **[Block 425]** — `SigningUtil.generateSignature` feeds the module/DSF signing crypto;
  this block is the producer of the bytes that path signs.
- **[Block 398]** — `program.requireSigning` off on the live supervisor → Programs compiled there are stored
  unsigned (§426.5).
- **[Block 380]** — the JVM whose `java.home` is read here is the one njre launches; `bin/javac` sits beside
  that JRE in the bundled JDK.

<!-- research-block: standalone framework block (program compile path); may seed a `program` focus (BProgram lifecycle, ClassLoader, runtime exec) -->
