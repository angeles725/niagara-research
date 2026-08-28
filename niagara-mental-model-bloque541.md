# Block 541 — The `program` module runtime: BProgram execution, freeform vs Robot, source+bytecode stored in the `.bog`, and the code-signing/SecurityManager sandbox

**Session**: 2026-08-28
**Focus**: `kitControl` (gap KC6 — the `program` module runtime + editor + sandbox)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY. Delegated sonnet sweep over `program-rt`/`program-wb` (55 vf classes); base classes and
the load-bearing SECURITY surface token-verified inline.
**Primary sources** `[CERT]`: `organized/program/program-rt/vineflower/com/tridium/program/` —
`BProgram.java`, `ProgramBase.java`, `BProgramCode.java`, `BCode.java`, `Robot.java`, `BProgramService.java`,
`ProgramRuntime.java`, `BProgramAction.java`; `organized/program/program-wb/.../ui/` — `BProgramEditor.java`,
`ProgramCompiler.java`, `SourceWriter.java`, `BRobotEditor.java`. DOC `[CERT-doc]`: niagara-help
`AXtoN4Migration/`, `User/program-ProgramService.txt`.

**Scope**: how a Niagara **Program** (user-written Java control logic) EXECUTES, stores itself, and is
sandboxed — the runtime the escape-hatch [Block 538] BP6 named but [Block 426] left unopened ([Block 426]
covered only COMPILATION — the spawned `javac`; REMITTANCE here). The security findings connect the control
focus to the signing/security thread ([Block 18], `security-audit`, `signing-pki`).

---

## 541.1 BProgram — the runtime component [CERT]

`BProgram extends BComponent` `[CERT] BProgram.java:32` — a plain component in the station tree. Each execution
runs the user's compiled class through a delegate `impl`:
```java
public void doExecute() { try { this.impl.onExecute(); } catch (Throwable t) { this.error("onExecute", t); } }  // :75-81
```
`impl` is a `ProgramBase` instance created by `loadImpl()` `[CERT] :83-94`:
`this.impl = getCode().newProgramInstance()` `[CERT] :85` → `impl.program = this` → `impl.onStart()`. It is
reloaded whenever the `code` slot changes. **Trigger**: `changed()` `[CERT] :64-73` — a property with the
`executeOnChange` flag fires `execute()` on change (the same flag [Block 538] R-A4 documented), so a
wire-sheet link into a program input drives its execution. `unloadImpl()` calls `impl.onStop()` and swaps in a
no-op `ProgramBase`.

## 541.2 Freeform vs Robot — two flavors [CERT]

- **Freeform Program** = `BProgram` + `ProgramBase`. `ProgramBase implements Runnable` `[CERT]
  ProgramBase.java` exposes three lifecycle hooks the user overrides: `onStart()`, `onExecute()`, `onStop()`.
  It is a LONG-LIVED, slot-wired component, PERSISTED in the `.bog`.
- **Robot** = `Robot` (abstract, single `run()`) `[CERT] Robot.java:7-26`. It is a ONE-SHOT script run against
  `BProgramService` (`robot.run()` `[CERT] BProgramService.java:111`), NOT persisted, output captured in a
  `PrintWriter log` and returned as `BRobotResult`. **Requires superUser** to run `[CERT]
  BProgramService.java:104` (`getPermissions().isSuperUser()`). Its class name is a random UUID (template
  `RobotImpl` replaced at compile time) to avoid classloader collisions.

## 541.3 Slots and wiring [CERT]

`BProgram` being a `BComponent`, the user adds dynamic input/output properties on the Slot Sheet. The editor's
`SourceWriter` enumerates every non-frozen, non-Link/Action/Topic property and generates a typed getter+setter
for each, so user code calls `getTemperature()` / `setOutput(v)` which proxy through `ProgramBase` to the
component slot by name `[CERT] ProgramBase.java` (`getInt(String)`/`setBoolean(String,…)` delegate to
`program.getInt(getProperty(prop))`). Named ACTIONS wire to methods: `BProgramAction.invoke()` reflects
`on<ActionName>()` on the impl `[CERT] BProgramAction.java` (`"on" + capitalize(name)`). So both data links
(via `executeOnChange` slots) and action links (via `on<Name>` methods) reach the user code.

## 541.4 Storage — source AND bytecode live in the `.bog` [CERT]

There is NO `BProgramExtension`; a `BProgram` holds a `code` slot of type `BProgramCode extends BCode`
`[CERT] BProgramCode.java:44`:
- `source` — HIDDEN `String` slot, the raw Java the user typed `[CERT] :47`; `className` default
  `"ProgramImpl"` `[CERT] :45`; `userDefinedImports`, `checksum` (stale-compile detection).
- Bytecode on `BCode`: `classFile` — HIDDEN `BBlob` = the compiled `.class` bytes `[CERT] BCode.java:98`;
  `signature` — `BBlob` PKCS#7 over those bytes; inner classes stored as DYNAMIC `BBlob` slots (signatures
  suffixed `#sig`); `dependencies` — semicolon module list for the classloaders.

So a Program is fully self-contained in the station database — source, bytecode, and signature all inline. No
`.class` files on disk at runtime. `newProgramInstance()` `[CERT] BProgramCode.java` loads the bytes via a
custom `ProgramClassLoader extends SecureClassLoader` `[CERT] BCode.java:23`, falling back to a no-op
`ProgramBase` if `classFile` is empty.

## 541.5 The sandbox — signing gate + SecurityManager + superUser (the load-bearing security surface) [CERT]

Three independent constraints gate a Program, all in `BCode.newInstance()`/`loadClass`:

1. **Code-signing gate** `[CERT] BCode.java:203-207`:
   ```java
   checkSigning = signature.length() > 0
       || AccessController.doPrivileged(() -> Boolean.getBoolean("program.requireSigning"));
   if (checkSigning) { SigningUtil.verifySignature(classFile.copyBytes(), signature.copyBytes(), trustStores); … }
   else { log.warning("program.notSigned"); }
   ```
   If the class is signed OR the JVM property `program.requireSigning=true` is set, the signature is verified
   against the trust stores before load; an untrusted cert throws and is added to the untrusted store. **If
   unsigned AND `program.requireSigning=false`, the program loads with only a WARNING** — this is exactly the
   posture [Block 18]/`security-audit` flagged live (`program.requireSigning` off in the audited supervisor).
   Inner classes are verified too `[CERT] :231`.
2. **Java SecurityManager** — `ProgramProtectionDomain` grants program classes the **global (untrusted)
   CodeSource permissions**, NOT framework privileges `[CERT] BCode.java:505-514` — so file access is confined
   to `file:^` (station_home) and `Runtime.exec()` is blocked. `[CERT-doc] AXtoN4Migration`: "Niagara 4
   utilizes the Java Security Manager, which puts restrictions on what Program objects may do."
3. **`ProgramRuntime.exec()` gate** `[CERT] ProgramRuntime.java`: direct `Runtime.exec()` is blocked; a program
   must call `ProgramRuntime.getRuntime().exec(this,…)`, which throws unless `allowProgramRuntimeExec=true` on
   `BProgramService` (**default false** `[CERT] BProgramService.java:37`) and AUDITS every call. Programs
   compiled into ProgramModules cannot exec at all `[CERT-doc]`.
4. **Edit is superUser-only** `[CERT] BCode.java` (`checkSuperUser` on classFile/source/signature writes) —
   non-superUsers may read code (per permission mask) but cannot modify it; `BProgramRecompileTool` re-signs
   all programs station-wide with the configured cert.

The security verdict: a Program is powerful (arbitrary Java in the station JVM) but fenced — untrusted-domain
permissions, station_home file scope, exec gated+audited, edit restricted to superUsers, and load gated on
signing when `program.requireSigning` is on. The residual risk is precisely leaving `program.requireSigning`
OFF (the default), which downgrades an unsigned malicious program from "blocked" to "loaded with a warning."

## 541.6 program-wb editor + compile [CERT]

`BProgramEditor extends BWbComponentView` `@AgentOn("program:Program")` `[CERT] BProgramEditor.java` — four
tabs: **Edit** (Java body with `JavaParser` highlighting), **Slots** (`BSlotSheet` to add I/O), **Imports**,
**Source** (read-only full generated `.java`). The boilerplate seeds `onStart/onExecute/onStop`
`[CERT] :73`. Compile: `ProgramCompiler extends Compiler` (the [Block 426] class) `[CERT] ProgramCompiler.java`
— `SourceWriter` wraps the user's method-body fragment into `public class <className> extends
ProgramBase { <generated getters/setters> <user source> }`, `javac` runs ([Block 426] path), `.class` bytes
read back into the `classFile` BBlob, checksum updated. A status bar shows compile/signing/timestamp state.
`BRobotEditor` (`@AgentOn("program:ProgramService")`) is the single-pane Robot editor.

## 541.7 Program vs Expr (closes the [Block 538] BP6 thread) [CERT-doc]

[Block 538] BP6 recorded the official recommendation to prefer `Expr` (BQL) over Program for simple logic. KC6
confirms WHY from the runtime: a Program carries a full compile+sign+classload+sandbox lifecycle and superUser
gating, whereas `Expr` "does not require compilation." Program is the heavy tool — reserved for logic that
genuinely needs arbitrary Java, not simple arithmetic/conditionals.

## 541.8 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BProgram extends BComponent; doExecute→impl.onExecute; loadImpl→newProgramInstance | [CERT] | BProgram.java:32,77,85 | token-checked ✓ |
| 2 | executeOnChange flag fires execute() (link-driven) | [CERT] | BProgram.java:64-73 | sweep-cited |
| 3 | Freeform=ProgramBase 3 hooks persisted; Robot=one-shot run() not persisted, superUser | [CERT] | ProgramBase.java; Robot.java:7-26; BProgramService.java:104,111 | token-checked ✓ |
| 4 | Dynamic slots → generated getters/setters via ProgramBase; BProgramAction reflects on<Name>() | [CERT] | ProgramBase.java; BProgramAction.java | sweep-cited |
| 5 | source in BProgramCode.source (HIDDEN String); bytecode in BCode.classFile (BBlob); all in .bog | [CERT] | BProgramCode.java:44-47; BCode.java:98 | token-checked ✓ |
| 6 | program.requireSigning gate; verifySignature; unsigned+off → warning only | [CERT] | BCode.java:203-207 | token-checked ✓ |
| 7 | ProgramProtectionDomain = global/untrusted perms (file:^ only, exec blocked) | [CERT] | BCode.java:505-514 | sweep-cited |
| 8 | ProgramRuntime.exec gated by allowProgramRuntimeExec (default false) + audited | [CERT] | ProgramRuntime.java; BProgramService.java:37 | sweep-cited |
| 9 | Code edit is superUser-only (checkSuperUser) | [CERT] | BCode.java | sweep-cited |
| 10 | ProgramClassLoader extends SecureClassLoader | [CERT] | BCode.java:23 | token-checked ✓ |
| 11 | ProgramCompiler extends Block 426 Compiler; SourceWriter wraps body | [CERT] | ProgramCompiler.java; SourceWriter.java | sweep-cited |

**Marker tally**: [CERT] ×10 · [CERT-doc] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 11 rows
token-verified inline (base class, execution entry, storage slots, the signing gate, SecureClassLoader). 55 vf
classes measured.

## Connections

- **[Block 426]** — the COMPILATION path (spawned `javac`); `ProgramCompiler extends` its `Compiler` (REMITTANCE).
- **[Block 18]** / `security-audit` — `program.requireSigning` off = unsigned programs load with a warning;
  §541.5 is the code mechanism behind that SEC finding.
- **`signing-pki`** — Programs are signed (PKCS#7 `signature` BBlob) and verified against the same trust stores
  as modules; `BProgramRecompileTool` re-signs station-wide.
- **[Block 538]** BP6 — Expr-over-Program recommendation; §541.7 gives the runtime reason.
- **Forward**: KC7 (honeywellFunctionBlocks catalog), KC8 (priority-array write path).

## Open gaps (this block)

- The `batch`/`module` sub-packages (ProgramModule — a compiled-in program library) are named, not decompiled;
  recorded, not a kitControl gap (belongs to a module-packaging thread if ever opened).
- The exact trust-store set `SigningUtil.verifySignature` consults is in the signing-pki focus (B392+), not
  re-derived here.
