# B522 — signing-pki: defensive hardening synthesis — turning the live license/module-integrity findings (B518–B521, B398) into operator actions

**Focus:** `signing-pki` · **Mode:** synthesis / defensive · **Consolidates** the session's live findings ([B518]/[B519]/[B520]/[B521]) plus the live audit [B398] into a hardening checklist. **Language:** English.

**Scope.** The defensive counterpart to the session's analysis: for each *observed-live* weakness in the licensing / module-integrity posture of this OptimizerSupervisor N4.14.0.162, the concrete action that closes or mitigates it. This block documents **how to harden**, not how to bypass. SECRETS DISCIPLINE observed. Markers: `[CERT-live]` observed this session; `[CERT]` code/prior; `[INFER]` reasoned.

---

## 1. The structural fact that makes hardening necessary

Both integrity gates are **load-time / event-triggered, not continuous** ([B519] §2, [B521]): the license verifies once at station boot; modules verify per-JAR at add/class-load; node-locked has **no post-boot watcher** ([B481]/[B487]). Between triggers, on-disk state is unwatched. Therefore enforcement quality depends entirely on (a) the strictness set at the trigger and (b) protecting the trigger's inputs. Every item below tightens one of those. `[CERT-live]`/`[CERT]`

## 2. Hardening checklist (finding → action)

| # | Live finding (evidence) | Risk | Hardening action |
|---|---|---|---|
| H1 | `niagara.moduleVerificationMode=low` `[CERT-live]` [B519] | Unsigned/mismatched modules load; interposition surface widens ([B520]) | Set **`highSecurity`** (or at least `high`) in `defaults/system.properties`; required-verify then fails closed (`exit(-6)`, [B482]) — plan restart/availability around it |
| H2 | `commandLinePropertyBlacklist` **commented out** `[CERT-live]` [B519] | `moduleVerificationMode` / `program.requireSigning` can be overridden at launch, defeating H1 | **Uncomment** the blacklist so those properties cannot be set from the command line |
| H3 | `program.requireSigning` off `[CERT-live]` [B398] | Unsigned Program objects execute | Enable `program.requireSigning` |
| H4 | `dsfspi.dll` Authenticode-signed but **load-unenforced** `[CERT]` [B520] | A patched/proxy crypto DLL (the single chokepoint) loads unchecked | Enforce OS code-integrity (WDAC / app-control policy) over `bin/`; monitor `dsfspi.dll` hash out-of-band |
| H5 | `truststore.jks` password `changeit`; dev anchor present `[CERT]` [B392]/[B398] | Trust store is guessable / carries a non-prod anchor | Rotate the truststore password; remove the SEJOFA/dev anchor on production |
| H6 | No post-boot re-verification; on-disk `security/` + `modules/` unwatched `[CERT-live]` [B519]/[B521] | Post-compromise tamper is invisible until reboot/reload | Out-of-band file-integrity monitoring of `security/licenses`, `security/certificates`, `modules/`; alert on change; scheduled restart discipline where feasible |
| H7 | Module verify is per-JAR, lazy (a never-loaded class is never verified) `[CERT-live]` [B521] | Dormant malicious module not caught until loaded | Under H1, prefer eager verification at boot where the mode supports it; inventory-pin the module set |

## 3. What is already strong (do not "fix" what isn't broken)

- The **crypto is real**: a 1-byte signature tamper is rejected fail-closed by the runtime ([B518]); the HostId gate independently rejects a valid-signature/wrong-host license ([B518] §3b). The root of trust (embedded DSA/ECDSA key in `baja.jar`, Honeywell Product PKI for modules) is sound — you cannot forge without the private key. `[CERT-live]`/`[CERT]`
- The weaknesses above are **posture/configuration and temporal-window** issues, **not** broken cryptography. Hardening = closing the config gaps and watching the blind window, not replacing the primitives.

## 4. Residual risk after hardening (honest bound)

- Enforcement remains **load-time**; H6 (out-of-band monitoring) mitigates the blind window but does not make verification continuous — that is a Niagara design property, not a setting. `[INFER]`
- Exploiting any remaining gap requires **local privileged access** (write to `security/`/`bin/` or the JVM) — i.e. these are **post-compromise persistence/tamper** concerns, not remote bypasses. Perimeter/credential controls remain the first line. `[INFER]` grounded in [B520]/[B519].

## 5. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Gates are load-time/event-triggered; no node-locked post-boot watcher | `[CERT-live]`/`[CERT]` | [B519] §2, [B521]; swap invisible to live station [B518] |
| 2 | moduleVerificationMode=low, blacklist commented, program.requireSigning off (live) | `[CERT-live]` | [B519], [B398] |
| 3 | dsfspi Authenticode load-unenforced | `[CERT]`/`[INFER]` | [B520] |
| 4 | Crypto itself is sound (tamper + wrong-host both fail closed) | `[CERT-live]` | [B518] |
| 5 | Residual gaps require local privileged access | `[INFER]` | [B519]/[B520] |

**Tally:** 3 `[CERT-live]`, 2 mixed `[CERT]`/`[INFER]`, 0 unmarked. Hardening actions are recommendations derived from cited live findings; none re-derived.

## 6. Connections

- Consolidates [B518] (license fail-closed), [B519] (two gates, live posture), [B520] (interposition surface), [B521] (per-JAR granularity), [B398] (live audit), [B392] (trust anchors).
- Feeds the security-audit checklist ([B398]/[B490] SEC items) — H1–H7 map to SEC-01/03/06/20/21 family.
- No new gap. Open items unchanged: SP-G6 (CRL), SP-G8 (OTA), SP-G3a (isolated-VM boot), SP-G10 (interposition runtime — refused).
