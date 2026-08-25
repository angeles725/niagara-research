# B527 — signing-pki SP-G10: full step-by-step session ledger — what was tried, what fired, what did NOT fire, and who/what says each verdict is right

**Focus:** `signing-pki` · **Gap:** SP-G10 (mirror PoC) · **Mode:** §12/§19 session ledger (complements the finding block [B524]) · **Language:** English.

**Scope.** The complete operational record of the SP-G10 run: every instrument that was built/run, in order,
what each one returned, and — the question the operator asked — **how we know each verdict is right and who
says it**. [B524] holds the *findings*; this block holds the *provenance of every step*. Defensive; SECRETS
DISCIPLINE observed (HostId format only, no license/secret values).

**Evidence.** `sources/probes/B524-spg10-frida-2026-08-25/` (RUN.md + logs) and `codegen/spg10-frida/`
(all scripts). Markers: `[CERT-live]` observed on the running host; `[CERT]` code/binary; `[INFER]` reasoned.

---

## 1. The instruments, in the order they ran (and what each one proved or disproved)

| # | Instrument (codegen/spg10-frida/) | Ran? | Result | What it settled |
|---|---|---|---|---|
| 1 | `globals.py` | ✅ | `Module`=function, `Process`=object, `Interceptor`=object, **no `Java`, `Gum`, `Memory`, `Thread`** | the host agent is the bare-bone Frida runtime |
| 2 | `runtime.py` | ✅ | `Frida.version=17.17.0`; `Process` keys = getModuleByName/getModuleByAddress/enumerateRanges/getThreadById/… | the 17.x idiom that DOES work |
| 3 | `idioms.py` | ✅ | `Process.getModuleByName('dsfspi.dll')` → 193 symbols; 29 `DsfSha1WithDsaSignature*` resolved at ASLR addresses | the native hook surface, re-anchored at runtime |
| 4 | `census.py` | ✅ | **0 calls** to any `DsfSha1WithDsaSignature` during `-licenses`; ~60 `checkFileSignature`; 1 `isFeaturePresent`=0 | **the license DSA is NOT on the dsfspi native path on this install** |
| 5 | `bridge.py` | ✅ (negative) | `typeof Java === 'undefined'` for the whole `nre.exe` boot (600 ticks) | the Java bridge never appears → Java hooks impossible here |
| 6 | `java_mirror.py` (log + force) | ❌ blocked-on-tool | `ReferenceError: 'Java' is not defined` / never `hooked` | license-side mirror NOT runnable on this host |
| 7 | `nverify_mirror.py` | ⚠️ partially | `nverify.exe` does **not** load `nre.dll` (its own pre-check "No signed manifests" fires first); hook host never appeared | `nverify` is not a host for the chokepoint hook (it is self-contained) |
| 8 | `nre_module_mirror2.py` log | ✅ | ~60 `checkFileSignature`, all `ret 0` | normal "valid" return convention = 0 |
| 9 | `nre_module_mirror2.py` force_invalid_all | ✅ | `forced 1 (orig 0)` on 1st jar → `FATAL … failed signature check` → abort | **the caller CONSUMES our injected return** (invalid direction proven) |
| 10 | `nre_module_mirror2.py` force_valid | ✅ | 60 forced (`0`), 0 FATAL, full `-licenses` output (6 `{valid}` + brands) | the same chokepoint passes under forced-valid (valid direction consistent) |

## 2. "Cómo sabemos que está bien / quién lo dice" — verdict provenance

| Verdict | Says it | How (independent witness, not our own claim) |
|---|---|---|
| License verify = BC-FIPS Java-side | static code + live census | `organized/baja/baja/decompiled/com/tridium/sys/license/LicenseUtil.java:172-181` (`Signature.getInstance`) AND `census.txt` (0 native DSA hits) agree from two independent directions |
| Module chokepoint is the real enforcement signal | **the target process itself** | `nre.exe` printed `FATAL … failed signature check` and stopped — that output is emitted by the *verifier's own consumer*, not by our script (our script only logs `forced`/`ret`) |
| Force-valid truly changed nothing fatal | the target process output | `nre_mm2_valid.txt`: 0 `FATAL`, full brand/license listing — the process completed normally, which our injection cannot fabricate |
| The install was not altered | sha256 + PID invariants | pre/post `sha256` identical; `tasklist` PIDs unchanged (`niagarad` 21348, `station` 18524) — measured, not asserted |
| The right symbols were hooked | runtime export enumeration | `idioms.py` resolved the exact ASLR addresses (`verify@0x1800296b0`↔runtime `0x7ff9bdee96b0`, `engineVerify0` twins) before any write |

## 3. What did NOT work / remain open (honest bound)

- **License-side mirror (runtime forcing)**: not executed — needs an agent with the Java bridge. Static path is pinned ([B524] F1); the runtime `LicenseUtil.verify → force true` flip is **SP-G10a**.
- The mirror was demonstrated on **disposable `nre.exe`**, not inside the live `station.exe` (attaching there = rung 3+, not authorized).
- `nverify.exe` usable as a *verifier oracle* for individual jars, NOT as an injection host (self-contained build).

## 4. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 10 instruments listed; 8 ran, 1 partial, 1 blocked-on-tool | `[CERT-live]` | codegen/spg10-frida/ + sources/probes/B524…/logs |
| 2 | Fatal output is the target's, not ours | `[CERT-live]` | nre_mm2_force.txt `FATAL … failed signature check` between our `call` and `forced` lines |
| 3 | 0 native DSA hits vs 60 checkFileSignature in one census run | `[CERT-live]` | census.txt |
| 4 | Java bridge absence is a host wall, typed not silent | `[CERT-live]` | bridge.txt + run order #5/#6 |
| 5 | sha256/PID invariants held | `[CERT-live]` | RUN.md ground-truth section |

**Tally:** 5 `[CERT-live]`, 0 `[CERT]`/`[INFER]` unmarked (step 2's static anchor is cited, not re-derived). No secret values.

## 5. Connections

- Complements [B524] (findings) with the step ledger + verdict provenance; feeds SP-G10a (unfinished half).
- No new gap beyond the already-spawned SP-G10a. Open items unchanged: SP-G9a, SP-G10a, SP-G6, SP-G8 (+ blocked SP-G3a/SP-G4/SP-G9b).
