# B520 — signing-pki SP-G10: the interposition ("mirror") surface, mapped statically — `dsfspi.dll` is the single native crypto chokepoint; dynamic PoC refused by harness

**Focus:** `signing-pki` · **Gap:** SP-G10 (surface MAPPED `[CERT]`; runtime confirmation REFUSED by harness) · **Mode:** dynamic §12 attempt + native read-only mapping · **Companion to** [B518]/[B519]. **Language:** English.

**Scope.** Where you would interpose to make the license/module verifier "return valid when it is not", who depends on the verifying process, and the module-verification granularity — mapped read-only. The live in-process PoC was **built but not executed**: the harness permission layer refused the injection (a wall on OUR side, not a target defense). SECRETS DISCIPLINE observed.

**Markers:** `[CERT]` verbatim in binary/export analysis; `[CERT-live]` observed on the running host; `[INFER]` reasoned. Evidence: `sources/probes/B520-interposition-2026-08-25/` (hook targets + refusal note) and `sources/probes/2026-08-25-native-interposition/RUN-native-interposition.md` (peer native session, read-only).

---

## 1. The single native crypto chokepoint (peer native mapping, read-only)

`rabin2 -E/-i` over `dsfspi.dll` / `nre.dll`, re-anchored by export table: `[CERT]`
- **`dsfspi.dll` is the ONLY native crypto chokepoint.** Only `nre.dll` and `njre.dll` import from it, and each imports **exactly one** symbol: `?checkFileSignature@DsfUtil` (the RSA verify for module/dist `.sig` sidecars).
- **The license DSA verify is reached ONLY via JNI**, not by a native caller: `dsfspi` exports 54 `Java_com_tridium_dsf_provider_*` natives (`DsfSecurityProvider`, `DsfDsaKeyPairGeneratorSpi`, …). Path = `baja.jar` `LicenseManager` → JCE `DsfSecurityProvider` → JNI → `dsfspi` `?verify@DsfSha1WithDsaSignature` @ `0x1800296b0`. Confirmed live: `nre.exe -licenses` **loads `dsfspi.dll`** at runtime. `[CERT]`/`[CERT-live]`
- The native text-match gate `?isFeaturePresent@LicenseUtil` (`nre.dll` @ `0x180001f90`) calls **neither** — it verifies nothing ([B518] asymmetry). `[CERT]`
- **`dsfspi.dll` is Authenticode-signed** (cert table ~10.5 KB), but **standard Windows DLL loading does not enforce Authenticode by default** → a patched/proxy `dsfspi` is loadable unless a loader enforces it. `[CERT]`/`[INFER]`

**Consequence:** `dsfspi.dll` is the highest-value single interposition target — it carries **both** the RSA module verify and the DSA license verify.

## 2. Two interposition routes (no edit to signed `baja.jar` required)

- **(A) `dsfspi.dll` proxy/patch** — the single chokepoint; dynamic load, Authenticode unenforced at load. Covers module + license verify in one place.
- **(B) rogue JCE provider ahead of `DsfSecurityProvider`** — the provider-order angle of [B441]/SP-G9; intercepts the JNI verify from the Java side.
- Both are gated by the live posture: `moduleVerificationMode=low` ([B398]/[B519]) + Authenticode-not-enforced. `[CERT]`/`[INFER]`

## 3. Module-verification granularity — one by one, on demand

Answering "is the watcher per-module or all-modules": `[CERT]`
- Verification is **per-module / per-JAR**, and **event-driven per class-load** — `DsfUtil::checkFileSignature` verifies **one file + its `%s.sig` sidecar at a time**; `JarSignatureRegistry`/`CertificateChainValidator` validate **each jar individually** at add-time/class-load ([B482]). There is **no single "verify all modules" pass** and no periodic re-scan.
- So a class that is never loaded is never verified; a module verified once is not re-verified while loaded. This is the module-side analogue of the license load-once model ([B519] §2).

## 4. False verdicts and the "see nothing" case (the operator's cluster, precisely)

- **Watcher says NO on a good module (false negative):** a valid module rejected (bad truststore/pin, clock, broken chain) → mode-gated; at `highSecurity`, required-verify fail → `System.exit(-6)` = **self-DoS**. `[CERT]` [B482]
- **Watcher says YES on a bad module (false positive = the mirror):** either the interposition of §2, or simply `moduleVerificationMode=low` letting an unsigned/mismatched jar load. `[CERT-live]`
- **"What if they see nothing":** the strongest form is not a lying verdict but **no verdict at all** — the gate is event-triggered, so between triggers (or if the trigger is shimmed to a no-op, or the class is never loaded) **nothing looks**, and the module runs unverified. Enforcement has temporal gaps by design ([B519] §2). `[INFER]` grounded in the event-triggered model.

## 5. Who hangs off the verifying process (blast radius)

`station.exe` hosts the whole station — every driver, service, the web/Fox servers, the license feature-map. A fatal license/module verdict (`exit(-3/-6)`) takes **all of it** down together; `niagarad` treats those exits as non-recoverable ([B478]). So the verifier's failure mode is **whole-station**, not per-component. `[CERT]`/`[CERT-live]`

## 6. The dynamic PoC: built, REFUSED by harness (typed wall §21)

A disposable-process PoC was designed (spawn `nre.exe -licenses` under Frida — provisioned live, `frida-python 17.17.0` on the Windows host — hook `?verify@DsfSha1WithDsaSignature` and force "valid" on a tampered license, expecting the oracle to flip `{invalid}`→`{valid}`). **Execution was DENIED by the Claude Code auto-mode permission classifier** — a wall on OUR side, not a target defense. Recorded as typed **`refused`** (METHODOLOGY §21); **not** routed to any peer (no permission laundering). Runtime confirmation of the mirror stays **open pending explicit operator permission**. Targets are frozen in the probe note for a later authorized run.

## 7. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | dsfspi.dll is the single native crypto chokepoint (RSA module + DSA license) | `[CERT]` | rabin2 -i nre/njre = 1 symbol each (checkFileSignature); 54 JNI verify natives |
| 2 | License DSA verify reached only via JNI; nre loads dsfspi live | `[CERT]`/`[CERT-live]` | export analysis; nre.exe -licenses module list includes dsfspi.dll |
| 3 | Exact hook targets located | `[CERT]` | verify@DsfSha1WithDsaSignature @0x1800296b0; checkFileSignature import @0x18000e730 |
| 4 | dsfspi Authenticode-signed but load-time-unenforced | `[CERT]`/`[INFER]` | PE cert table present; default DLL load does not verify |
| 5 | Module verify is per-JAR, per-class-load, no global re-scan | `[CERT]` | checkFileSignature per-file %s.sig; JarSignatureRegistry per-jar [B482] |
| 6 | Fatal verdict is whole-station (blast radius) | `[CERT]` | exit(-3/-6) non-recoverable [B478] |
| 7 | Dynamic mirror PoC not executed — refused by harness | `[CERT]` | auto-mode classifier denial; typed `refused` §21 |

**Tally:** 2 `[CERT-live]` (shared), 5 `[CERT]`, 2 `[INFER]` (explicit). No unmarked claims. Peer native facts cited, not re-derived.

## 8. Connections & open gaps

- **Deepens** [B519] §4 (interposition surface) with the exact chokepoint + targets; **uses** [B441]/SP-G9 (provider order) as route B.
- **SP-G10** remains open as **runtime confirmation** (the static surface is now MAPPED `[CERT]`): needs explicit operator permission to run the built Frida PoC on a disposable `nre.exe`.
- **SP-G3a** still blocked (isolated station/VM).
