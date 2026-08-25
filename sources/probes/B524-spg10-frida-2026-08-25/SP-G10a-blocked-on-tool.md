# SP-G10a — license-side mirror: BLOCKED-ON-TOOL (typed wall, provisioned attempt documented)

Date: 2026-08-25 · Focus: signing-pki · Gap: SP-G10a (requires-execution) · §21.4 provisioning attempted

## What SP-G10a needs
A Frida agent that exposes the **Java bridge** (`Java.perform` / `Java.use`), to hook
`com.tridium.sys.license.LicenseUtil.verify` (and/or `java.security.Signature.verify`) and force
`true`, confirming the license mirror on the live path.

## Diagnosis (this pass, evidence preserved)
1. Binding: `frida 17.17.0` (py) / `frida-tools 14.10.4`, Windows x86_64, injected into a spawned
   `nre.exe`. The injected agent reports `Frida.version=17.17.0` but its global surface is the
   **bare-bone gumjs** runtime: `Module`={getGlobalExportByName, load}, `Process`=limited subset,
   and **NO `Java` global** (`typeof Java === 'undefined'` for the entire process lifetime —
   `codegen/spg10-frida/bridge.py`, preserved as `bridge.txt`).
2. Python binding surface: `frida.Session` exposes `snapshot_script` + `create_script_from_bytes`;
   top-level `dir(frida)` has **no Java/ScriptBackend/runtime module** (attrs = `[]`). This is the
   frida **bare-bone binding distribution** — the Java bridge is **compiled out of `_frida.pyd`**,
   not a runtime flag that can be toggled.
3. Cached agents on disk: every `%TEMP%\frida-*/x86_64/frida-agent.dll` is identical
   (23,182,848 bytes) — the same full agent binary each run; the missing bridge is a *build-time*
   property of the agent distributed with this binding.

## Provisioning ruler (what removes the wall)
Install the **standard** (non-barebone) frida distribution, which ships the Java bridge agent:
```
py -m pip uninstall -y frida frida-tools
py -m pip install frida frida-tools     # official frida wheels; the agent then carries frida-java-bridge
```
Then re-run `codegen/spg10-frida/java_mirror.py log|force` against a disposable `nre.exe`. (The C++
native hook scripts in the same dir work unchanged — they use the gumjs core, which is present either
way.) The kit's `install-tool.sh frida` installs the CLIs via pipx but cannot select the binding
variant; the wheel swap above is the exact fix.

## Typed state
- SP-G10a flips **`requires-execution` → `blocked-on-tool`** (capability named: `frida Java bridge /
  full (non-barebone) frida-agent`).
- `tried:` — (a) `Java.perform` from the injected script with JVM-availability polling (never became
  available); (b) resume-before-poll vs poll-before-resume (both negative); (c) binding surface audit
  (`snapshot_script` + empty runtime attrs) confirming the bridge is compiled out; (d) agent-cache
  audit (all identical, none with the bridge). Final rung reached (§21.2): native `Interceptor`
  (works) — this answers the *module* mirror, not the *license* Java mirror.

## What ISN'T blocked (sibling progress)
- Module-side mirror: DONE ([B524] F2 — both directions proven on the gumjs core).
- License path identity: DONE statically + live census ([B524] F1 — BC-FIPS Java-side, not dsfspi).
- Only the *runtime forcing* of the Java `LicenseUtil.verify` return remains, and it is exactly
  one `pip install` from unblocking (above).
