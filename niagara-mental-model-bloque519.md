# B519 — signing-pki: licensing vs module-integrity are two EVENT-TRIGGERED gates in one host process; live posture (`moduleVerificationMode=low`) and the interposition surface

**Focus:** `signing-pki` · **Mode:** dynamic §12 (live-install, read-only) + synthesis · **Companion to** [B518] (license verifier fail-closed). **Language:** English.

**Scope.** Answer three operator questions with live evidence on this host: (1) are license-check and module-check the **same service or different**? (2) **when** do the "watchers" fire — always, or on events? (3) what is the **live module-integrity posture** and its interposition ("mirror") surface? Corroborates/extends [B392]/[B398]/[B482]/[B489] on the operator's actual supervisor (OptimizerSupervisor N4.14.0.162; 11 station configs; a live station on :443). SECRETS DISCIPLINE: no secret values; credentials never persisted.

**Markers:** `[CERT-live]` observed on this running host; `[CERT]` code/prior block; `[INFER]` reasoned. Evidence: `sources/probes/B519-module-verify-live-2026-08-25/RUN-module-verify.md`.

---

## 1. Same host process, DIFFERENT verification systems (the orthogonality, made precise)

License-entitlement and module-integrity are **not the same check** — they share the host but nothing else:

| Axis | License / entitlement | Module integrity |
|---|---|---|
| Signature algo | **DSA-1024**/SHA-1 (+ v2 ECDSA-P256) | **RSA-2048**/SHA-256 |
| Trust anchor | hidden master **DSA** root embedded in `baja.jar` [B395] | `truststore.jks` + embedded **TPK** pin; roots to **Honeywell Product PKI** [B392] |
| Verifier code | `LicenseManager`/`LicenseUtil.verify` (baja) | `JarSignatureRegistry`→`CertificateChainValidator` (baja) + native `nverify.exe` [B489] |
| Bound to | **HostId** (machine) + date window | nothing machine-specific (code identity only) |
| Fail mode | features withheld / `System.exit(-3)` over-cap | mode-gated / required-verify → `System.exit(-6)` |

- **What they SHARE:** the same **host processes** (`station.exe` runs both; `niagarad` runs platform-feature license + can drive dist/module verify) and the same **native crypto library** `dsfspi.dll` (Mocana) — which carries BOTH the RSA verify (modules) and the DSA verify (licenses) [B484]. `[CERT]`
- **Answer to "same service?":** at the OS level, yes — one Windows service `Niagara` (Running) hosts everything `[CERT-live]`. At the mechanism level, **no** — two independent gates, two keys, two anchors, two failure modes. Defeating one does **not** defeat the other; they are orthogonal. `[CERT]`/`[CERT-live]`

## 2. The gates are EVENT-TRIGGERED, not always-on watchers

The operator's "are the watchers always watching / when are they called" — measured:
- **License gate fires at STATION BOOT (load time).** `LicenseManager` reads `security/licenses`, verifies, materializes a feature map in memory; after that it is a map lookup. Node-locked has **NO post-boot re-watcher** ([B481]/[B487]). Confirmed live: my [B518] license swap-and-restore was **invisible to the running `station.exe`** precisely because nothing re-reads after boot. `[CERT-live]`
- **Module gate fires at module ADD-time and CLASS-LOAD.** `JarSignatureRegistry`/`CertificateChainValidator` run on those events, not on a timer. `[CERT]` [B482]
- **The only continuous watcher is the SUBSCRIPTION entitlement watchdog** — and this install is **node-locked/perpetual** (licenses `[expires: 2027-03-31]`, no subscription), so **there is no live licensing watcher at all**. `[CERT-live]`/`[CERT]`
- **Therefore "how do you know a watcher is watching?"** — you cannot observe a continuous watcher because there isn't one; you can only prove a gate **fired at its trigger** by reading its log (e.g. the `GRAVE … {invalid: Invalid signature}` of [B518], produced only when the boot-time verify ran). Between triggers the gate is dormant — which is the structural reason the "mirror" works. `[INFER]` grounded in the live results.

## 3. Live module-integrity posture on THIS host (a security finding)

`[CERT-live]` (all from `defaults/system.properties`, `bin/policy/signing.properties`, `security/`):
- **`niagara.moduleVerificationMode=low`** — module verification is set to its relaxed tier on this supervisor. Corroborates [B398] on the live box.
- **`niagara.commandLinePropertyBlacklist` is COMMENTED OUT** — so `moduleVerificationMode` and `program.requireSigning` are **not protected from command-line override**; an operator/attacker can (re)set them at launch. `[CERT-live]`
- **Build pin present:** `signing.properties` pins `CN=Niagara4Modules Code Signing` ⇐ `Honeywell CodeSign RSA CA` (the Honeywell Product PKI chain of [B392]); `truststore.jks` present (958 B). `[CERT-live]`
- **`nverify.exe` on a shipped module** ran the verify path (`INFO Verifying archive …`, no failure) — the native module verifier is functional and reachable via interop. `[CERT-live]`

## 4. The "mirror" / interposition surface (feasibility, not a PoC)

Making a gate "return as if OK" is feasible **in proportion to enforcement strictness**, and this host is relaxed:
- At **`highSecurity`**, a required-verify failure is fatal (`System.exit(-6)`, [B482]) — a self-DoS, hard to mirror.
- At **`low`** (live here), verification does not hard-fail the same way → an unsigned/mismatched module is far more likely to load, and the in-process verifier (`JarSignatureRegistry` / a rogue JCE provider, order configurable [B441]) is the shim point. `[CERT-live]`/`[INFER]`
- **NOT attempted:** no interposition PoC was run. Proving a live in-process shim needs Windows-side JVM instrumentation (Frida-Windows/JVMTI); the installed Frida is Linux-only and cannot inject the Windows PE. → gap **SP-G10** (`requires-execution`, needs Windows-side instrumentation + explicit operator go). `[CERT-live]` (the tooling limit)

## 5. Note on the deferred boot test (SP-G3a)

The full-boot "required-feature-missing → exit vs degrade" test ([B518] SP-G3a) was **NOT run**: read-first showed this is the operator's **working supervisor host with 11 station configs (several customer-named) and a live station on :443** — not a clean sacrificial box. Booting a second `station.exe` risks port collision / collateral. SP-G3a is **re-typed `blocked (requires-artifact: a truly isolated station/VM)`**, not open-on-this-host.

## 6. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | License & module verify are distinct systems (DSA vs RSA, diff anchors/fail modes) | `[CERT]` | table §1; [B482]/[B484]/[B392]/[B395] |
| 2 | Both run in the same host processes + share dsfspi.dll | `[CERT]`/`[CERT-live]` | service `Niagara` Running; [B484] dsfspi has RSA+DSA |
| 3 | License gate fires at boot, no post-boot re-watch (node-locked) | `[CERT-live]` | [B518] swap invisible to live station; perpetual licenses |
| 4 | Module gate fires at add/class-load, event-triggered | `[CERT]` | [B482] |
| 5 | `moduleVerificationMode=low` live on this host | `[CERT-live]` | defaults/system.properties |
| 6 | moduleVerificationMode not CLI-blacklisted (override-able) | `[CERT-live]` | commented commandLinePropertyBlacklist |
| 7 | Build pin = Honeywell Product PKI; truststore.jks present | `[CERT-live]` | signing.properties; security/truststore.jks |
| 8 | nverify runs the module-verify path | `[CERT-live]` | `INFO Verifying archive`, no SEVERE |
| 9 | Interposition PoC feasibility ∝ mode; not attempted | `[CERT-live]`/`[INFER]` | mode=low; Frida Linux can't inject Win PE → SP-G10 |

**Tally:** 6 `[CERT-live]`, 2 `[CERT]`, 1 mixed/`[INFER]` (explicit). No unmarked claims. Nothing re-derived from [B392]/[B398]/[B482]/[B489] — cited.

## 7. Connections & open gaps

- **Companion to** [B518]; **corroborates live** [B398] (moduleVerificationMode=low), [B392]/[B489] (Honeywell Product PKI + nverify), [B482] (verifier chain), [B481]/[B487] (no node-locked watcher).
- **New gap SP-G10** (`requires-execution`): live in-process interposition PoC (shim/rogue-provider) vs `moduleVerificationMode` — needs Windows-side JVM instrumentation.
- **SP-G3a** re-typed `blocked (requires-artifact: isolated station/VM)` — this shared host is unsafe for a blind station boot.
