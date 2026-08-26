# SP-G10a — license-side mirror: BLOCKED-ON-TOOL (typed wall, provisioned attempt documented)

Date: 2026-08-25 · Focus: signing-pki · Gap: SP-G10a (requires-execution) · §21.4 provisioning attempted

## What SP-G10a needs
A Frida agent that exposes the **Java bridge** (`Java.perform` / `Java.use`), to hook
`com.tridium.sys.license.LicenseUtil.verify` (and/or `java.security.Signature.verify`) and force
`true`, confirming the license mirror on the live path.

## Diagnosis (FINAL — after standard reinstall + JVM-load probe + 45s bridge poll)
1. Standard wheel reinstalled (`frida 17.17.0`, 41.8 MB) → agent surface is now the FULL gumjs
   (`Frida, Script, Memory, Thread, ModuleMap, MemoryAccessMonitor, …`). The earlier "bare-bone
   binding" hypothesis is RETRACTED.
2. **`jvm.dll` IS loaded by `nre.exe`** — post-boot module list shows `jvm.dll` (8,851,456 B,
   base 0x58f20000) AND `java.dll` (176,128 B). The standard JVM is present; this is NOT an
   embedded-JVM-only launcher. (Earlier "no jvm.dll" was a stale `find -maxdepth 3` miss —
   it lives at `jre/bin/server/jvm.dll` and exports `JNI_CreateJavaVM`/`JNI_GetCreatedJavaVMs`/
   `JNI_GetDefaultJavaVMInitArgs`.)
3. **`Java` never becomes available even 45 s after resume** (poll 2000+ ticks) with the JVM
   fully booted. The `Java` global is absent from the agent's global surface regardless of JVM
   state. `frida-java-bridge` is NOT pip-installable (no such distribution) — it is bundled
   INSIDE a frida agent at build time, and this wheel's agent was built WITHOUT it.

## Root cause (final)
The `frida 17.17.0` wheel on this host ships a **gumjs-only agent (no Java runtime)**. The Java
bridge is a build-time component of the agent binary, not a flag or a separately installable
package. No amount of polling or JVM detection makes `Java.perform` available.

**Cross-check against the standard JVM (this pass):** spawning the install's own
`jre/bin/java.exe` (`openjdk 1.8.0_412`, `jre/bin/server/jvm.dll` present) under the SAME frida
agent gives `typeof Java === 'undefined'` on every poll tick — the bridge is absent regardless
of the process. Evidence: `codegen/spg10-frida/java_exe_bridge_probe.py` (preserved).

## Readiness items vs the environment
- Item 1 (classpath/entry for `LicenseUtil.verify` under `java.exe`): **done** — the class lives
  in `modules/baja.jar` (`com/tridium/sys/license/LicenseUtil`, deps `javax.baja.*` +
  `NLicenseManager`/`Nre.getHostId()`); `java.exe` is runnable. BUT the harness has no Java bridge,
  so running it adds no observation the static path ([B524] F1) does not already give.
- Items 2/3/5 (`Java.available`, `java_mirror.py log`, `java_mirror.py force`): **blocked-on-tool**
  — the agent has no Java bridge even against `java.exe` (probe above).
- Items 4/6 (license tamper / restore / B528): **no-op under the blocked bridge** — B518 already
  proved the fail-closed rejection of a tampered license on the real runtime, and a license-tamper
  with no Java hook would produce no new signal. Not executed; the capability gap is the blocker,
  not the lack of a reversible recipe (that recipe already exists in B518 and is cited).

## Provisioning ruler (what removes the wall)
- **A (recommended):** obtain a frida distribution whose agent bundles the Java bridge — e.g.
  a frida version/build with `frida-core` + `frida-java-bridge` compiled in (custom agent build,
  or a prebuilt `frida-agent` variant that includes the Java runtime). Then re-run
  `codegen/spg10-frida/java_mirror.py {log|force}` against a disposable `nre.exe`.
- **B (classpath, no runtime nre):** run `com.tridium.sys.license.LicenseUtil.verify` under the
  standard `jre/bin/java.exe` with the Niagara classpath (baja.jar + nre.jar + libs). The standard
  `java.exe` process loads `jvm.dll` BEFORE user code, so a Java-ready agent would attach normally.
  This validates the *class*, not the exact `nre.exe` process.

## Typed state
- SP-G10a stays **`blocked-on-tool`** (capability named: `frida agent with the built-in Java bridge
  (frida-java-bridge)`).
- `tried:` — (a) bare-bone hypothesis → retracted; (b) `Java.perform` polling before/after resume
  (10s, 45s, both negative); (c) JVM-load probe (jvm.dll + java.dll ARE loaded post-boot);
  (d) `frida-java-bridge` pip availability (no such distribution — bundled at agent build);
  (e) standard-`java.exe` spawn probe (Java still undefined). Final rung (§21.2): native
  `Interceptor` — already closed the module mirror ([B524] F2).

## What ISN'T blocked (sibling progress)
- Module-side mirror: DONE ([B524] F2 — both directions proven on the gumjs core).
- License path identity: DONE statically + live census ([B524] F1 — BC-FIPS Java-side, not dsfspi).
- Only the *runtime forcing* of the Java `LicenseUtil.verify` return remains, and it is exactly
  one `pip install` from unblocking (above).
