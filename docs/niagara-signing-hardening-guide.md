# Niagara N4 signature/PKI — Dynamic verification & hardening runbook (document mode §20)

**Scope.** The **dynamic** half of the `signing-pki` focus, consolidated from the live §12/§19 session
(B518–B524): every *observed-live* finding → the exact command/procedure to (a) **re-verify** it on the
running install and (b) **harden** it, using the agreed toolset: a text editor, **WDAC/AppLocker**,
**`keytool`**, **FIM (file-integrity monitoring)**, and **`nverify`**. This document **consolidates** — it
does not re-derive closed findings; it cites the blocks and preserves the exact commands run this session.
Defensive only; no bypass procedure. SECRETS DISCIPLINE: Host IDs as format templates, never values;
truststore password structure cited as the finding, not a secret to protect (it is the *default* to rotate).

**Evidence base.** Corpus blocks `[B518]–[B524]` + `[B398]`; preserved probe captures under
`sources/probes/B518-sp-g3-live-2026-08-25/`, `B519-module-verify-live-2026-08-25/`,
`B521-module-verify-granularity-2026-08-25/`, `B524-spg10-frida-2026-08-25/`; PoC source under
`codegen/spg10-frida/`. Markers per METHODOLOGY §3; everything below is `[CERT-live]` (observed this
session) or `[CERT]` (verbatim in code), cited to its block.

**Target.** `C:\Honeywell\OptimizerSupervisor-N4.14.0.162` (Honeywell OEM, build N4.14.0.162). WSL paths
appear as `/mnt/c/Honeywell/...`. All verification commands are read-only; hardening steps are
**recommendations** the operator applies in their own change window (they are config writes, not run here).

---

## 1. The one structural fact every dynamic check hangs on

Both integrity gates are **load-time / event-triggered, not continuous** ([B519] §2, [B521]):
the license verifies once at station boot; modules verify **per-JAR at add/class-load**; node-locked has
**no post-boot watcher** [B481]/[B487]. Between triggers, on-disk state is unwatched. Consequences:

- `nre.exe -licenses` is a **fresh-JVM oracle**: it re-reads `security/` and re-runs the verifier
  independently of the running `station.exe` ([B518]). This is what makes static `[CERT-live]` probes safe —
  a separate JVM is invisible to the live station's in-memory license map.
- The only two long-lived license consumers are `niagarad.exe` (platform daemon) and `station.exe`
  ([B518] §4). Instrumenting `nre.exe`/`nverify.exe` instead of attaching to those is the safe §12 rung.

---

## 2. Ground-truth re-measurement (run before ANY dynamic work — RE-MEASURE, never inherit)

```bash
cd /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162
sha256sum bin/nre.exe bin/nre.dll bin/dsfspi.dll bin/nverify.exe \
          security/licenses/*.license security/truststore.jks   # record your CURRENT values

# same-PID invariant (untouched processes must keep their PIDs across a visit)
/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -NoProfile -Command \
  "Get-Process niagarad,station -ErrorAction SilentlyContinue | Select-Object Id,ProcessName"
```

Session values ([B524], for continuity — re-measure live, do not trust these):
`nre.exe`=47b73fbd…, `nre.dll`=606ff1c6…, `dsfspi.dll`=82e8c7f0…, `nverify.exe`=b0358926…;
PIDs `niagarad`=21348, `station`=18524 (unchanged across the whole mirror run).

---

## 3. Re-verify the live posture (read-only probes, the "before" snapshot)

### 3.1 `moduleVerificationMode` + the CLI-blacklist gap — `[CERT-live]` [B519]

```bash
grep -nE "moduleVerificationMode|commandLinePropertyBlacklist" \
  defaults/system.properties
```
Live this session: `niagara.moduleVerificationMode=low` (line 442) and
`#niagara.commandLinePropertyBlacklist=…` (line 474) **commented out**. Interpretation: the relaxed tier
is active AND both `moduleVerificationMode` and `program.requireSigning` are **override-able at launch**.

### 3.2 `program.requireSigning` — `[CERT-live]` [B398]/[B522] H3

```bash
grep -n "requireSigning" defaults/system.properties
```
Live: `#program.requireSigning=false` (line 447, commented = default off). Unsigned Program objects execute.

### 3.3 `nverify` on shipped modules (window into the per-JAR verdict) — `[CERT-live]` [B521]

```bash
cd /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162
./bin/nverify.exe modules/abstractMqttDriver-rt.jar      # expected: INFO only, exit 0
./bin/nverify.exe modules/aaphp-rt.jar                    # signed module, exit 0
```
Each invocation is **one independent verdict per JAR** ([B521]) — there is no "verify all modules" pass.
`nverify.exe` is self-contained (embeds its crypto; does NOT load `nre.dll`/`dsfspi.dll`) — [B524] F4.
For a batch census, iterate over `modules/*.jar`; a `SEVERE … Verification failed` line is a finding.

### 3.4 `keytool` on the truststore (structure of the module trust anchor) — `[CERT-live]` [B392]/[B398]

```bash
cd /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162
./jre/bin/keytool.exe -list -keystore security/truststore.jks -storepass changeit
```
Live: **1 entry** — `niagaramoduledev, 15/01/2026, trustedCertEntry` (SHA-256 fingerprint
`83:7B:38:E8:…`). Findings to act on: the store password is the shipped default (`changeit`, [B392])
and the single entry is a **dev anchor**, not a production-only root ([B522] H5).

### 3.5 The license oracle (fail-closed proof, re-runnable) — `[CERT-live]` [B518]

```bash
cd /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162 && ./bin/nre.exe -licenses
```
Intact install → `{valid}` on every `.license`/`.certificate`. B518 proved (byte-tamper + restore,
byte-identical) that a 1-byte signature flip produces `{invalid: Invalid signature}` fail-closed. To
re-measure the fail-closed property safely: copy a license to scratch, flip one byte, restore — do **not**
touch `security/` on the live box (see B518's reversible recipe with `trap … EXIT` restore + sha256).

---

## 4. Hardening actions (H1–H7) — the "after" state

One editor (`notepad.exe` or your editor) on `defaults/system.properties`; back it up first:

```bash
cp defaults/system.properties /tmp/system.properties.bak.$(date +%s)
```

| Finding (live) | Action | New state / verify |
|---|---|---|
| **H1** `niagara.moduleVerificationMode=low` [B519] | Set `highSecurity` (or at least `high`) | `grep moduleVerificationMode defaults/system.properties` ⇒ `highSecurity`. Required-verify then fails closed `System.exit(-6)` [B482]; plan a restart/availability window |
| **H2** `commandLinePropertyBlacklist` commented [B519] | Uncomment line 474 | both `moduleVerificationMode` + `program.requireSigning` no longer CLI-overridable ⇒ H1 can't be defeated at launch |
| **H3** `program.requireSigning=false` [B398] | Set `program.requireSigning=true` | unsigned Program objects rejected |
| **H4** `dsfspi.dll` Authenticode-signed but **load-unenforced**; the single native chokepoint is **provably flippable in-process** [B520]/[B524] | Enforce OS code-integrity over `bin/`: a **WDAC** policy (or AppLocker) that allows only signed-by-Honeywell binaries, plus out-of-band hash watch on `dsfspi.dll` | Attempt to load a patched `dsfspi` proxy ⇒ blocked at load (WDAC). This is the only mitigation that survives the demonstrated Frida mirror (B524 §2) |
| **H5** `truststore.jks` password `changeit` + dev anchor [B392]/[B398] | Rotate the truststore password; remove/replace `niagaramoduledev` on production | `keytool -list -keystore security/truststore.jks` (new passphrase) ⇒ production anchors only |
| **H6** no post-boot re-verification; `security/` + `modules/` unwatched [B519]/[B521] | **FIM** (out-of-band) on `security/licenses`, `security/certificates`, `modules/`, `bin/dsfspi.dll`; alert on change; scheduled-restart discipline | any change fires an alert — this mitigates the blind window between triggers (does not make verification continuous — that is a Niagara design property, [B522] §4) |
| **H7** module verify is lazy per-JAR [B521] | Under H1, prefer eager verification at boot where the mode supports it; inventory-pin the module set | an un-inventoried/never-loaded jar is caught by the inventory, not by the lazy verifier |

---

## 5. What is already strong (do not "fix" what isn't broken) — `[CERT-live]` [B518]/[B522] §3

- The **crypto is real**: a 1-byte signature tamper is rejected fail-closed by the runtime; the HostId
  gate independently rejects a valid-signature/wrong-host license ([B518] §3b).
- The module-trust chain (Niagara4Modules Code Signing → Honeywell CodeSign RSA CA → Honeywell Product
  PKI RSA, [B392]) and the hidden DSA/ECDSA roots in `baja.jar` are sound — you cannot forge without the
  private keys.
- The weaknesses are **posture/config + temporal-window** issues, not broken primitives. Hardening =
  close the config gaps (H1–H3, H5) + watch the blind window (H4, H6, H7).

## 6. The dynamic mirror, distilled for operators — `[CERT-live]` [B524]

The question "can a shim make the verifier return valid" now has a live answer:

- **License half:** on this `bcfips` install the license DSA verify is **BouncyCastleFipsProvider
  Java-side** (`LicenseUtil.java:172-181` `Signature.getInstance`; provider order [B441]) — the native
  `dsfspi` DSA fired **zero** times during `-licenses` ([B524] F1). (§14-corrects [B520] §1.)
- **Module half:** `nre.dll::SignatureUtil::checkFileSignature` is the single chokepoint whose return
  value IS the enforcement signal. Forcing it to `1` → `FATAL … failed signature check` abort; forcing
  `0` → 60 jars pass, full output ([B524] F2).
- **Operator takeaway:** a local in-process attacker can flip the module gate in *both* directions.
  The durable controls are **H4 (WDAC over `bin/`)** + **H6 (FIM)** — not the verifier itself.

## 7. Toolchain map (which tool answers which question)

| Question | Tool | Block |
|---|---|---|
| Is this jar/module signed and valid? | `bin/nverify.exe <jar>` (per-JAR verdict) | [B521] |
| Does the license runtime accept a tampered license? | `bin/nre.exe -licenses` oracle | [B518] |
| What is in the module trust store? | `jre/bin/keytool.exe -list -keystore security/truststore.jks` | [B392]/[B398] |
| Is the module-mode/blacklist posture weak? | `grep` on `defaults/system.properties` | [B519] |
| Is `bin/` protected against DLL interposition? | WDAC/AppLocker policy | [B522] H4 |
| Is `security/`+`modules/` tamper detected between triggers? | FIM | [B522] H6 |
| Did a change actually land (hash-identity)? | `sha256sum` before/after | [B518]/[B524] |

## 8. The three licensing gates and where each could be interposed (operator map)

> Defensive framing — this documents WHAT is validated and WHERE, so the operator knows the real
> trust surface. No bypass procedure is described (the cryptography findings are cited; the interposition
> points are the same ones the hardening actions defend). SECRETS DISCIPLINE: HostId as `Win-XXXX…`.

| Gate | What it checks | Exact check point in code | Is it interposable? |
|---|---|---|---|
| **1. Signature** | The `.license`/`.certificate` DSA/ECDSA signature under the hidden master key | Java `LicenseUtil.verify(...)` → `Signature.verify` | ✅ **PROVEN live** — ASM rewrite to `return true` flips `{invalid}` → `{valid}` ([B528]) |
| **2. HostId binding** | The `hostId` field inside the license must equal the machine's real HostId | Java `NodeLockedLicenseManager.isLicenseHostIdValid()` → `this.hostId.equals(Nre.getHostId())` | **Mapped, not executed** — the exact one-line point (`isLicenseHostIdValid`) is a perfect analog of gate-1's rewrite; HOST-input spoofing is the other route (see below) |
| **3. Required-feature + date window** | The station feature (`station`/`stationAzul`) must be present & unexpired or the process dies | `Station.checkLicense:214-230` → `System.exit(-3)` "FATAL: Not licensed to run a station"; `Nre.licenseFailure()` → `exit(-3)` | **Mapped, not executed** — this is a *different* failure path (process exit), not a boolean the loader consumes; SP-G3a remained blocked (would need an isolated station/VM boot) |

### The HostId interposition surface (from the operator questions)

- **Live-proven (B518 §3b):** a validly-signed license for a DIFFERENT HostId is **fail-closed** —
  the runtime moves it out (`moved file`) and its features vanish. Copying `.license` files from
  another laptop does NOT work.
- **Why the HostId gate is the interesting one (B424):** `Nre.getHostId()` is a **non-cryptographic
  8-byte XOR fold** of four machine inputs — a locally-generated **hidden key** (registry `hid3`),
  **RegisteredOwner**, a generated **product id**, and the **C: volume serial**. The crypto can be
  strong while the identity inputs are spoofable: three ways to make machine B present host A:
  1. rewrite `isLicenseHostIdValid()` → `true` (Java, same technique as [B528]);
  2. hook the native `getHostId()` to return host A's value;
  3. clone the four fold inputs from A to B (registry/edit/disk work, no runtime hook).
  Routes 1–3 are **mapped, NOT executed** in this corpus (defensive-only scope).
- **"No license at all" scenario:** with no license there is nothing to forge — gates 2 and 3 still
  kill the process (`exit(-3)` / feature-not-licensed), so the signature mirror alone cannot make an
  unlicensed station run.

### Operator takeaway (the whole map in one line)

Three independent gates — signature, HostId, required-feature. The signature gate and the module gate
were flipped live ([B524]/[B528]); the HostId gate is the exact same shape at
`isLicenseHostIdValid()` and rests on spoofable machine inputs ([B424]); the required-feature gate is a
process-exit path, not a boolean. Defenses: H1–H3 (config strictness), H4 (WDAC — blocks the agent/hook),
H5 (trust store), H6 (FIM — catches a copied/cloned license), plus protecting the HostId inputs.
