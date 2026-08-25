# B524 PROBE — SP-G10 Frida interposition (mirror) — EXECUTED, operator-authorized

Date: 2026-08-25 · Focus: signing-pki · Gap: SP-G10 · Mode: §19 build/PoC + §12 live (disposable processes only)
Operator authorization: explicit ("Ejecutar el PoC Frida (SP-G10)") — see session.

## Ground truth (re-measured live this phase — RE-MEASURE GROUND-TRUTH, never inherit)
- Install: /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162 (same HostId format Win-XXXX-XXXX-XXXX-XXXX, redacted)
- sha256: nre.exe=47b73fbd…, nre.dll=606ff1c6…, dsfspi.dll=82e8c7f0…, nverify.exe=b0358926…,
  Honeywell.license=4a799453…, HoneywellCentraLine.license=892353c7…, Webs.license=fc548614…
- Live PIDs unchanged after the whole run: niagarad.exe=21348, station.exe=18524 (same-PID invariant
  HOLDs: untouched processes were not restarted).
- Frida on Windows host: frida-python 17.17.0 (bindings) / frida-tools 14.10.4, Windows x86_64.

## The tooling wall that re-routed the PoC (typed, §21)
- This host's injected frida agent is the BARE-BONE runtime: `Module` reduced to
  getGlobalExportByName/load; `Process` reduced to getModuleByName/getModuleByAddress/
  enumerateRanges/getThreadById…; **NO `Java` global (Java bridge ABSENT)**, no Gum/Memory/Thread.
  Evidence: agentver.txt, bridge.txt (Java never becomes available during nre.exe boot,
  typeof Java === 'undefined' for the whole process).
- Consequence: Java-layer hooks are UNAVAILABLE on this host (a `blocked-on-tool` wall:
  capability = frida Java bridge / full agent). Native Interceptor + Process.getModuleByName +
  enumerateSymbols DO work (Frida 17.x idiom). Walked the fallback (§21.2 native -> r2 -> quick):
  the native interceptor rung was reached and is what produced all findings below.

## Code artifacts
- codegen/spg10-frida/run.py — native dsfspi DSA hook PoC (Frida 17 idioms)
- codegen/spg10-frida/census.py — hooks every DsfSha1WithDsaSignature/checkFileSignature/
  isFeaturePresent symbol in dsfspi.dll + nre.dll during `nre -licenses`
- codegen/spg10-frida/nre_module_mirror2.py — the MIRROR: hooks nre.dll
  SignatureUtil::checkFileSignature (1 arg = jar path), modes log|force_valid|force_invalid_all
- codegen/spg10-frida/nverify_mirror.py — nverify-based control (nverify is self-contained,
  does NOT load nre.dll/dsfspi: documented negative)
- codegen/spg10-frida/java_mirror.py — the license-side Java hook (could NOT run: no Java bridge)

## FINDINGS (the answers SP-G10 asked for)

### F1 — The license DSA verify DOES NOT flow through dsfspi's native DSA on this install. `[CERT-live]`
census.py hooked all 29 DsfSha1WithDsaSignature symbols (verify/sign/parse/init + JNI
engineVerify0/engineSign0) in dsfspi.dll across a FULL `nre -licenses` run: ZERO calls.
Meanwhile `checkFileSignature` fired ~60× (module jars) and `isFeaturePresent` fired 1× (=0).
Static re-check: com.tridium.sys.license.LicenseUtil.verify() does Signature.getInstance(algorithm)
= pure JCE. Combined with B441's shipped bin/policy/java.security
(provider.1=BCFKSWrap, provider.2=BouncyCastleFipsProvider, provider.3=Sun) -> the license
DSA verify is BouncyCastleFipsProvider JAVA-side, NOT the native DsfSha1WithDsaSignature.
This §14-CORRECTS B520 §1's "License DSA verify reached only via JNI" claim for THIS bcfips
install (B440: no fips140-2 feature => bcfips branch). The dsfspi native DSA exists in the binary
but is not on the license-verify path here.

### F2 — The module verify chokepoint is REAL and the return value IS the enforcement signal. `[CERT-live]`
nre.dll SignatureUtil::checkFileSignature(char const* filename) -> int is called per module jar
(normal run ~60 calls, 0=valid). Under Frida, forcing the return to 1 (invalid) on the FIRST
pristine jar gave: "FATAL: ...\bin\ext\annotations-13.0.jar failed signature check" and nre
ABORTED (1 call total). Forcing the return to 0 (valid) for all 60 jars: nre completed the full
-licenses output (6 {valid} licenses + brands), zero FATAL.
-> The mirror EXISTS: an in-process shim on this one native function can flip the module
integrity verdict EITHER WAY, and the process acts on the injected value immediately.

### F3 — The native mirror demo did NOT require touching any install file. `[CERT-live]`
All runs were against a fresh spawned nre.exe (stdio redirected), never attached to
niagarad.exe/station.exe. No file in the install was written; the tampered module copy lived in
%TEMP% scratch. Post-run sha256 of bin/*.dll/.exe and security/licenses/* = identical to ground
truth; the two real processes kept their PIDs (no restart).

### F4 — nverify.exe is a self-contained verifier (separate from the station path). `[CERT]`
nverify.exe embeds the crypto (exports EC_P224…/PF_p224…; imports are runtime/crt only — no
nre.dll/dsfspi import), and does its own manifest pre-check ("No signed manifests found for
archive" -> exit 1 for a stripped jar). It does NOT load nre.dll, so it is NOT a suitable
host for the checkFileSignature hook; the correct host is nre.exe (station path).

## Marker tally
[CERT-live] (F1/F2/F3 observed on the running host, disposable processes): 3
[CERT] (F4 binary/static): 1  ·  [INFER]: 0  ·  typed walls: 1 (blocked-on-tool: frida Java bridge)

## Invasiveness ladder (rung announcements)
- rung (0) passive capture: n/a (process stdout redirect for the oracle)
- rung (1) read-only probe: census/log hooks (F1, F2 log mode) + nverify controls
- rung (2) reversible write equivalent: in-process return-value patch in a DISPOSABLE spawned
  process (F2 force modes). No on-disk write. Reversed trivially by process exit.
- No rung (3)/(4). No install mutation; no live-station interaction.

## SECRETS DISCIPLINE
HostId format only (Win-XXXX-XXXX-XXXX-XXXX). No license/secret VALUES logged. License file
contents never written into scratch beyond the throwaway hash-tamper of a module JAR copy.
