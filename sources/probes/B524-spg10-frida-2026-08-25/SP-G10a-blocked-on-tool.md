# SP-G10a — license-side mirror: BLOCKED-ON-TOOL (typed wall, provisioned attempt documented)

Date: 2026-08-25 · Focus: signing-pki · Gap: SP-G10a (requires-execution) · §21.4 provisioning attempted

## What SP-G10a needs
A Frida agent that exposes the **Java bridge** (`Java.perform` / `Java.use`), to hook
`com.tridium.sys.license.LicenseUtil.verify` (and/or `java.security.Signature.verify`) and force
`true`, confirming the license mirror on the live path.

## Diagnosis (updated after standard frida reinstall, operator-authorized)
1. Reinstalled the standard wheel (`frida 17.17.0 cp37-abi3-win_amd64`, 41.8 MB) — the agent surface
   is now the FULL gumjs (`Frida, Script, Memory, Thread, ModuleMap, MemoryAccessMonitor, …`), so the
   "bare-bone binding" hypothesis is RETRACTED.
2. `Java` is still `undefined` for the whole `nre.exe -licenses` boot. Root cause (verified):
   **`nre.exe` does not load a standard `jvm.dll`.** The tree has NO `jvm.dll`;
   `nre.exe` (22,808 B) is a thin launcher → `nre.dll` (117 KB) → the JVM is embedded, and
   `jre/bin/` only ships `java.dll` (not `jvm.dll`). Frida's Java bridge auto-attach keys off the
   standard JVM (`JNI_GetCreatedJavaVMs` from `jvm.dll`); an embedded-JVM launcher never triggers it.
3. Result: the Java bridge cannot auto-attach to Niagara's `nre.exe`. The standard `java.exe`
   in `jre/bin` is NOT the process that runs the license verifier, so there is no standard-JVM
   process to hook for the license mirror.

## Provisioning ruler (what removes the wall)
The Java bridge cannot auto-attach to an embedded-JVM launcher. Two real options:
- **A — attach manually to the embedded JVM's `java.dll`** (a custom `JNI_GetCreatedJavaVMs`
  resolution in the Frida script, targeting `nre.dll`'s own JVM attach): advanced, not the standard
  `Java.perform` auto path.
- **B — run the verifier under the STANDARD `java.exe`** instead of `nre.exe` (e.g. `jre/bin/java.exe`
  with the license-verification class `com.tridium.sys.license.LicenseUtil` on the classpath) — the
  standard JVM would let `Java.perform` attach normally. This is the reversible/disposable path; the
  station itself still uses `nre.exe`, so this validates the *Java class* (which is identical), not
  the exact `nre.exe` process.
Both are follow-ups; the static Java path is already pinned ([B524] F1), and the module mirror is
already proven on the gumjs core ([B524] F2).

## Typed state
- SP-G10a stays **`blocked-on-tool`** (capability named: `frida Java bridge auto-attach to a
  standard JVM` — Niagara's `nre.exe` embeds the JVM and never loads `jvm.dll`).
- `tried:` — (a) bare-bone hypothesis → retracted by the standard reinstall (full gumjs surface
  now present); (b) `Java.perform` polling before/after resume (negative); (c) `njre.dll`/`nre.dll`
  import audit (no `jvm.dll`, no `JNI_CreateJavaVM` export path); (d) standard `jvm.dll` search
  (absent from the whole install). Final rung (§21.2): native `Interceptor` — which already closed
  the module mirror.

## What ISN'T blocked (sibling progress)
- Module-side mirror: DONE ([B524] F2 — both directions proven on the gumjs core).
- License path identity: DONE statically + live census ([B524] F1 — BC-FIPS Java-side, not dsfspi).
- Only the *runtime forcing* of the Java `LicenseUtil.verify` return remains, and it is exactly
  one `pip install` from unblocking (above).
