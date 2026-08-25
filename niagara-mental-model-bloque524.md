# B524 — signing-pki SP-G10 CLOSED: the interposition mirror, executed live — the license verify is BC-FIPS Java-side (NOT dsfspi), and the module-verify chokepoint CAN be flipped in-process

**Focus:** `signing-pki` · **Gap:** SP-G10 (mirror PoC — executed, closed) · **Mode:** §19 build/PoC + §12 live (disposable processes, operator-authorized) · **Companion to** [B520] (surface mapped), [B518] (fail-closed), [B519]/[B521] (two gates / per-JAR). **Language:** English.

**Scope.** The runtime answer to "can a shim make the verifier return valid": executed on the operator's host with explicit permission, using a disposable `nre.exe`/`nverify.exe` under Frida. Two concrete results: (1) the license DSA verify is **NOT** the native `dsfspi` DSA on this install — it is BouncyCastle-FIPS **Java-side**, §14-correcting [B520] §1; (2) the **module** verify is one native function whose return value IS the enforcement signal, and it can be flipped either way in-process. SECRETS DISCIPLINE observed (HostId format only). Evidence: `sources/probes/B524-spg10-frida-2026-08-25/` + `codegen/spg10-frida/`.

---

## 1. The tooling wall that re-routed the run (typed, §21)

This host's injected Frida agent is the **bare-bone runtime**: `Module` is reduced to `getGlobalExportByName`/`load`, `Process` to `getModuleByName`/`getModuleByAddress`/`enumerateRanges`/…, and — critically — **the `Java` bridge is ABSENT** (`typeof Java === 'undefined'` for the whole `nre.exe` boot; [bridge].txt). Java-layer hooks are therefore `blocked-on-tool` (capability: frida Java bridge / full agent) on this host. The native interceptor does work (Frida 17.x idiom: `Process.getModuleByName().enumerateSymbols()`), and that rung produced all findings below. `[CERT-live]`

## 2. F1 — License DSA verify does NOT flow through `dsfspi.dll` on this install (the §14 correction)

`census.py` hooked all 29 `DsfSha1WithDsaSignature` symbols (`verify`/`sign`/`parse`/`init` + JNI `engineVerify0`/`engineSign0`) in `dsfspi.dll` over a full `nre -licenses`: **zero calls**. In the same run `SignatureUtil::checkFileSignature` fired ~60× (module jars) and `isFeaturePresent` fired once (=0). `[CERT-live]`

Static re-check to corroborate (not re-derive): `com.tridium.sys.license.LicenseUtil.verify()` does `Signature.getInstance(algorithm)` → pure JCE (`organized/baja/baja/decompiled/com/tridium/sys/license/LicenseUtil.java:172-181`). Combined with [B441]'s shipped `bin/policy/java.security` (`provider.2=BouncyCastleFipsProvider` ahead of Sun, and [B440]'s `bcfips` branch with no `fips140-2` feature), the license DSA verify is **`BouncyCastleFipsProvider` Java-side**. `[CERT]`

> **§14 CORRECTION of [B520] §1.** B520 asserted "License DSA verify reached only via JNI … `dsfspi` `?verify@DsfSha1WithDsaSignature`". That is true of the `bcstd`/native-DSA branch, but **not of this `bcfips` install**: here the verifier never enters dsfspi's DSA. The correct live answer is environment-branch-dependent (SP-G9b is the sibling for the `bcstd`-licensed branch). The dsfspi DSA primitives exist in the shipped binary; they are simply not on the license path on this host.

## 3. F2 — The MODULE verify chokepoint is one function and IS the enforcement signal (the mirror, demonstrated)

Hook: `nre.dll` `SignatureUtil::checkFileSignature(char const* filename) -> int` (normal runs: ~60 module jars, `0` = valid). Under Frida, on a **fresh spawned** `nre.exe -licenses` (never the live station): `[CERT-live]`

| Forced return | Observed outcome |
|---|---|
| `1` (invalid) on the 1st pristine jar | `FATAL: …\bin\ext\annotations-13.0.jar failed signature check` → **nre aborts** (1 call total) |
| `0` (valid) on all 60 jars | nre completes the full `-licenses` output (6 `{valid}` licenses + brands), zero FATAL |

The mirror exists: an in-process shim on this one native function flips the module-integrity verdict **either way**, and the process acts on the injected value immediately. The bypass direction (force-valid on a bad jar) is the same write path as the demonstrated force-invalid; the fatal-invalid run proves the caller consumes the return, not just that we can overwrite it.

## 4. F3 — Zero install mutation; invariants hold

All runs were disposable spawned processes (stdio to a redirected log), never attached to `niagarad.exe`/`station.exe`. Tampered artifacts lived in scratch; no install file was written. Post-run `sha256` of `bin/nre.exe`, `nre.dll`, `dsfspi.dll`, `nverify.exe` and all three licenses = byte-identical to pre-run ground truth; live PIDs unchanged (`niagarad`=21348, `station`=18524) → same-PID invariant holds (untouched processes not restarted). `[CERT-live]`

## 5. F4 — `nverify.exe` is self-contained (documented negative for the hook host)

`nverify.exe` embeds the crypto (exports `EC_P224…`/`PF_p224…`; imports are CRT-only — it imports **neither** `nre.dll` nor `dsfspi.dll`) and does its own manifest pre-check (`No signed manifests found for archive` → exit 1 for a stripped jar). It is NOT a host for the `checkFileSignature` hook — the correct host is `nre.exe`. This narrows the useful instrumented surface for any future mirror work. `[CERT]`

## 6. What remains open (honest bound)

- **License mirror (Java-side) not executed** on this host: needs an agent WITH the Java bridge (or a different instrument — e.g. a JCE-level SPI substitution per [B520] route B) to hook `Signature.verify`/`LicenseUtil.verify`. Typed `blocked-on-tool` here; the static path is pinned so the Java hook is a mechanical follow-up when a full agent is available.
- The module-side mirror was demonstrated on a **disposable nre**, not inside the live `station.exe`. Attaching to the live station is out of scope (rung 3+) and was not authorized for this run.
- `moduleVerificationMode=low` ([B519]) already weakens the gate below the chokepoint — the mirror matters most under `highSecurity` (H1).

## 7. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Host agent lacks the Java bridge (typed wall) | `[CERT-live]` | bridge.txt: `typeof Java === 'undefined'` entire boot; agentver.txt global-keys |
| 2 | dsfspi DSA verify unused during `-licenses` | `[CERT-live]` | census.txt zero DsfSha1WithDsaSignature hits; ~60 checkFileSignature, 1 isFeaturePresent |
| 3 | License verify is JCE/BC-FIPS Java-side | `[CERT]` | LicenseUtil.java:172-181 `Signature.getInstance`; B441 provider order; B440 bcfips |
| 4 | checkFileSignature return = enforcement signal (mirror) | `[CERT-live]` | nre_mm2_force.txt `FATAL … failed signature check`; nre_mm2_valid.txt 0 FATAL, full output |
| 5 | Install untouched; PIDs/sha256 invariant | `[CERT-live]` | pre/post sha256 identical; tasklist PIDs unchanged |
| 6 | nverify self-contained, not a hook host | `[CERT]` | rabin2 -i nverify.exe (no nre.dll/dsfspi import) |

**Tally:** 4 `[CERT-live]`, 2 `[CERT]`, 0 `[INFER]`, 1 typed wall. No unmarked claims.

## 8. Connections & gap bookkeeping

- **Closes SP-G10** (investigable artifact: runtime mirror confirmed on the module side; license side answered with the branch correction F1 + the static Java path, the runtime Java-shell part recorded as a typed wall, not silence).
- **§14-corrects [B520] §1** (license→dsfspi JNI claim); **reconciles** with [B440] (bcfips) and [B441] (provider order) — the full picture is now: license = BC-FIPS Java, module = dsfspi native, both event-triggered [B519]/[B521].
- **Feeds [B522]** H4/H6: the single native chokepoint is now proven *flippable in-process* — WDAC/code-integrity over `bin/` + out-of-band FIM are the mitigations that survive the demonstrated shim.
- Open items unchanged: **SP-G9a** (live `getProviders`), **SP-G6** (CRL), **SP-G8** (OTA), **SP-G3a** (isolated-VM boot, blocked), **SP-G4** (blocked), **SP-G9b** (blocked).
- **SP-G10a (new, requires-execution)**: re-run the license-side mirror with a full Frida agent (Java bridge) to hook `LicenseUtil.verify`/`Signature.verify` → force `true`, confirming the license mirror on the *live* path (the Java-shell static is pinned; the runtime confirm is the remaining unproven half).
