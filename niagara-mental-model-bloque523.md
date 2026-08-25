# B523 — signing-pki: the module DEVELOPER's threat model — how you know your own module is intact, how a signature is stripped/replaced, and why enforcement (not the signature) is the real control

**Focus:** `signing-pki` · **Mode:** synthesis / defensive (developer perspective) · **Consolidates** [B518]–[B522] with the OEM-obfuscation and own-module evidence ([B207]/[B423]/[B163]–[B177] `com.angeles.chihuahua`). **Language:** English.

**Scope.** The signing subsystem re-cut from the point of view of **a party that ships a signed Niagara module** (e.g. the operator's own `com.angeles.chihuahua`): what a signature actually guarantees, how you verify your own artifact, how an adversary strips or replaces your signature, what you can and cannot control, and why the decisive control lives on the **consumer** (the station's `moduleVerificationMode`), not on your `.jar`. Defensive documentation only — no bypass procedure. SECRETS DISCIPLINE observed. Markers: `[CERT-live]` this session; `[CERT]` code/prior; `[INFER]` reasoned.

---

## 1. What your signature does and does NOT guarantee (the three orthogonal protections, developer-side)

From [B522] §1 / the licensing doc, applied to your module:
- **Integrity** — your RSA-2048 signature proves the bytes were not altered *since you signed*. Sound crypto ([B518]). `[CERT]`
- **Authorship** — the signature identifies *your* certificate (chaining to a trusted root). `[CERT]`
- **Confidentiality** — **NOT provided.** Java bytecode decompiles; a signature does nothing to hide your logic. Only separate obfuscation/encryption helps (ZKM/string-scrub/encrypted resources — the OEM pattern of [B207] `easyBinding` / [B423] `galileoKitPx`). `[CERT]`
- **The meta-point:** a signature is a **claim a verifier must choose to check.** It protects you only where the *consumer* enforces verification. You sign; the station decides whether that matters. `[CERT]`/`[INFER]`

## 2. How you know YOUR module is intact (static — file at rest)

No running station needed: `[CERT-live]` (tools exercised this session)
- `nverify.exe your-module.jar` → validates the signature and reports the signing entity.
- `unzip -l your-module.jar | grep META-INF` → your signature = `META-INF/*.SF` + `*.RSA`/`*.DSA` (plus Niagara's detached `%s.sig` sidecar, [B520]).
- `keytool -printcert -jarfile your-module.jar` → your certificate chain.
If it validates against your cert and chains to a trusted root, the artifact is intact and attributable to you. **This is the static process.**

## 3. How a signature is stripped or replaced (trivial, not an exploit)

An adversary holding your `.jar` can, on *their* copy:
- **Strip:** delete `META-INF/*.SF` + `*.RSA`/`*.DSA` (and the `.sig`) → the module becomes **unsigned**. A `zip -d` / repackage; no crypto broken. `[CERT]`/`[INFER]`
- **Replace:** repackage and **re-sign with their own certificate** → signed, but by someone else. `[CERT]`/`[INFER]`
Neither attacks the cryptography (which is sound, [B518]); they simply remove the claim or substitute a different one. **You cannot prevent this on someone else's copy** — the artifact is in their hands.

## 4. The real control is the CONSUMER's enforcement — and this install is weak

Whether a stripped/re-signed module is **rejected** depends entirely on the station at load time: `[CERT-live]`
- `moduleVerificationMode=highSecurity` + `program.requireSigning` → unsigned / wrong-cert module **rejected**, fail-closed ([B482] `exit(-6)`).
- **`moduleVerificationMode=low`** — found LIVE on this supervisor ([B519]) — may **load it anyway**.
So the documented weakness for your modules is **not** the signature (crypto is sound) — it is that **the consuming install runs `low`**, so a stripped or re-signed copy of your module can execute. Closed by [B522] H1/H2/H3. `[CERT-live]`

## 5. What you (the developer) can and cannot control

| | You CAN | You CANNOT |
|---|---|---|
| Integrity/authorship | sign correctly; publish your cert; verify your own artifacts | stop someone stripping/re-signing their copy |
| Confidentiality | obfuscate/encrypt sensitive logic & resources | rely on the signature to hide anything (bytecode decompiles) |
| Enforcement | recommend/require `highSecurity` on target stations; ship in a controlled dist | force a third-party station's `moduleVerificationMode` |
| Runtime gating | put real server-side checks in your module (e.g. chihuahua's `checkCanWrite` RBAC write-gate, [B163]/[B177]) | trust client-side gating alone (Reflow's un-gated pattern, [B150]) |

## 6. Developer-side hardening (distinct from the operator checklist of B522)

- **Sign every artifact** and keep the private key off the build host / in an HSM. `[INFER]`
- **Obfuscate** the logic you actually need to protect (the OEM playbook, [B207]/[B423]) — confidentiality is a *separate* layer from signing. `[CERT]`
- **Do not rely on client-side gating.** Enforce authorization **server-side inside the module** — your `com.angeles.chihuahua` already does the right thing (`checkCanWrite` on every mutating endpoint, [B163]/[B177]); that survives a stripped signature, an unsigned load, and a hostile client. `[CERT]`
- **Tell integrators to run `highSecurity`** — your integrity guarantee is only as strong as the weakest consuming station.

## 7. Static vs dynamic (answering the process question)

- **Static** (file at rest, no live system): verifying your own `.jar` (`nverify`/`keytool`/`unzip META-INF`); reasoning about strip/replace. `[CERT-live]`
- **Dynamic** (live system): whether the running station *enforces* verification — governed by `moduleVerificationMode` at boot ([B519]) and observable only against a running station. The strip/replace itself is static; its *consequence* is dynamic. `[CERT-live]`/`[INFER]`

## 8. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Signature = integrity+authorship, NOT confidentiality | `[CERT]` | [B522] §1; Java decompiles; [B207]/[B423] obfuscation |
| 2 | You verify your own module statically (nverify/keytool/META-INF) | `[CERT-live]` | tools run this session |
| 3 | Strip = delete META-INF/*.SF+*.RSA(+.sig); replace = re-sign — no crypto broken | `[CERT]`/`[INFER]` | JAR signing structure; [B520] sidecar |
| 4 | Rejection depends on consumer's moduleVerificationMode; this install = low | `[CERT-live]` | [B519] live |
| 5 | Developer's durable control = server-side gating in the module | `[CERT]` | chihuahua checkCanWrite [B163]/[B177] vs Reflow [B150] |
| 6 | Crypto itself is sound (tamper fails closed) | `[CERT-live]` | [B518] |

**Tally:** 3 `[CERT-live]`, 3 mixed `[CERT]`/`[INFER]`, 0 unmarked. Cited, not re-derived.

## 9. Connections & open gaps

- Developer-side counterpart to [B522] (operator hardening); uses [B518] (crypto sound), [B519] (low mode live), [B520] (sidecar/chokepoint), [B207]/[B423] (obfuscation), [B163]/[B177] (own-module RBAC).
- No new gap. Open items unchanged: SP-G6 (CRL), SP-G8 (OTA), SP-G3a (isolated-VM boot), SP-G10 (interposition runtime — refused).
